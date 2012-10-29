package org.apache.archiva.consumers.core.repository;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.maven2.MavenArtifactFacet;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.events.RepositoryListener;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Base class for all repository purge tasks.
 */
public abstract class AbstractRepositoryPurge
    implements RepositoryPurge
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    protected final ManagedRepositoryContent repository;

    protected final RepositorySession repositorySession;

    protected final List<RepositoryListener> listeners;

    private Logger logger = LoggerFactory.getLogger( "org.apache.archiva.AuditLog" );

    private static final char DELIM = ' ';

    public AbstractRepositoryPurge( ManagedRepositoryContent repository, RepositorySession repositorySession,
                                    List<RepositoryListener> listeners )
    {
        this.repository = repository;
        this.repositorySession = repositorySession;
        this.listeners = listeners;
    }

    /**
     * Purge the repo. Update db and index of removed artifacts.
     *
     * @param references
     */
    protected void purge( Set<ArtifactReference> references )
    {
        if ( references != null && !references.isEmpty() )
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            for ( ArtifactReference reference : references )
            {
                File artifactFile = repository.toFile( reference );

                // FIXME: looks incomplete, might not delete related metadata?
                for ( RepositoryListener listener : listeners )
                {
                    listener.deleteArtifact( metadataRepository, repository.getId(), reference.getGroupId(),
                                             reference.getArtifactId(), reference.getVersion(),
                                             artifactFile.getName() );
                }

                // TODO: this needs to be logged
                artifactFile.delete();
                try
                {
                    repository.deleteArtifact( reference );
                }
                catch ( ContentNotFoundException e )
                {
                    log.warn( "skip error deleting artifact {}: {}", reference, e.getMessage() );
                }

                try
                {
                    metadataRepository.removeProjectVersion( repository.getId(), reference.getGroupId(),
                                                             reference.getArtifactId(), reference.getVersion() );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "skip error removeProjectVersion artifact {}: {}", reference, e.getMessage() );
                }

                boolean snapshotVersion = VersionUtil.isSnapshot( reference.getVersion() );

                try
                {
                    if ( snapshotVersion )
                    {
                        String baseVersion = VersionUtil.getBaseVersion( reference.getVersion() );
                        Collection<ArtifactMetadata> artifacts =
                            metadataRepository.getArtifacts( repository.getId(), reference.getGroupId(),
                                                             reference.getArtifactId(), baseVersion );
                        if ( artifacts != null )
                        {
                            // cleanup snapshots metadata
                            for ( ArtifactMetadata artifactMetadata : artifacts )
                            {

                                // TODO: mismatch between artifact (snapshot) version and project (base) version here
                                if ( artifactMetadata.getVersion().equals( reference.getVersion() ) )
                                {
                                    if ( StringUtils.isNotBlank( reference.getClassifier() ) )
                                    {

                                        // cleanup facet which contains classifier information
                                        MavenArtifactFacet mavenArtifactFacet =
                                            (MavenArtifactFacet) artifactMetadata.getFacet(
                                                MavenArtifactFacet.FACET_ID );

                                        if ( StringUtils.equals( reference.getClassifier(),
                                                                 mavenArtifactFacet.getClassifier() ) )
                                        {
                                            artifactMetadata.removeFacet( MavenArtifactFacet.FACET_ID );
                                            String groupId = reference.getGroupId(), artifactId =
                                                reference.getArtifactId(),
                                                version = reference.getVersion();
                                            MavenArtifactFacet mavenArtifactFacetToCompare = new MavenArtifactFacet();
                                            mavenArtifactFacetToCompare.setClassifier( reference.getClassifier() );
                                            metadataRepository.removeArtifact( repository.getId(), groupId, artifactId,
                                                                               version, mavenArtifactFacetToCompare );
                                            metadataRepository.save();
                                        }

                                    }
                                    else
                                    {
                                        metadataRepository.removeArtifact( artifactMetadata, VersionUtil.getBaseVersion(
                                            reference.getVersion() ) );
                                    }

                                }
                            }
                        }
                    }
                }
                catch ( MetadataResolutionException e )
                {
                    log.warn( "skip error deleting metadata {}: {}", reference, e.getMessage() );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "skip error deleting metadata {}: {}", reference, e.getMessage() );
                }

                repositorySession.save();

                triggerAuditEvent( repository.getRepository().getId(), ArtifactReference.toKey( reference ),
                                   AuditEvent.PURGE_ARTIFACT );
                purgeSupportFiles( artifactFile );
            }
        }
    }

    /**
     * <p>
     * This find support files for the artifactFile and deletes them.
     * </p>
     * <p>
     * Support Files are things like ".sha1", ".md5", ".asc", etc.
     * </p>
     *
     * @param artifactFile the file to base off of.
     */
    private void purgeSupportFiles( File artifactFile )
    {
        File parentDir = artifactFile.getParentFile();

        if ( !parentDir.exists() )
        {
            return;
        }

        FilenameFilter filter = new ArtifactFilenameFilter( artifactFile.getName() );

        File[] files = parentDir.listFiles( filter );

        for ( File file : files )
        {
            if ( file.exists() && file.isFile() )
            {
                String fileName = file.getName();
                file.delete();
                // TODO: log that it was deleted
                triggerAuditEvent( repository.getRepository().getId(), fileName, AuditEvent.PURGE_FILE );
            }
        }
    }

    private void triggerAuditEvent( String repoId, String resource, String action )
    {
        String msg =
            repoId + DELIM + "<system-purge>" + DELIM + "<system>" + DELIM + '\"' + resource + '\"' + DELIM + '\"' +
                action + '\"';

        logger.info( msg );
    }
}
