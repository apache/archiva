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

import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.metadata.maven.model.MavenArtifactFacet;
import org.apache.archiva.metadata.repository.*;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.ContentNotFoundException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.metadata.audit.RepositoryListener;
import org.apache.archiva.repository.content.Artifact;
import org.apache.archiva.repository.content.ItemNotFoundException;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.storage.util.StorageUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for all repository purge tasks.
 */
public abstract class AbstractRepositoryPurge
    implements RepositoryPurge
{
    protected Logger log = LoggerFactory.getLogger( getClass( ) );

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

    /*
     * We have to track namespace, project, project version, artifact version and classifier
     * There is no metadata class that contains all these properties.
     */
    class ArtifactInfo
    {
        final String namespace;
        final String name;
        final String projectVersion;
        String version;
        String classifier;

        ArtifactInfo( String namespace, String name, String projectVersion, String version )
        {
            this.namespace = namespace;
            this.name = name;
            this.projectVersion = projectVersion;
            this.version = version;
        }

        ArtifactInfo( String namespace, String name, String projectVersion )
        {
            this.namespace = namespace;
            this.name = name;
            this.projectVersion = projectVersion;
        }

        /*
         * Creates a info object without version and classifier
         */
        ArtifactInfo projectVersionLevel( )
        {
            return new ArtifactInfo( this.namespace, this.name, this.projectVersion );
        }

        public void setClassifier( String classifier )
        {
            this.classifier = classifier;
        }

        public String getNamespace( )
        {
            return namespace;
        }

        public String getName( )
        {
            return name;
        }

        public String getProjectVersion( )
        {
            return projectVersion;
        }

        public String getVersion( )
        {
            return version;
        }

        public String getClassifier( )
        {
            return classifier;
        }

        public boolean hasClassifier( )
        {
            return classifier != null && !"".equals( classifier );
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o ) return true;
            if ( o == null || getClass( ) != o.getClass( ) ) return false;

            ArtifactInfo that = (ArtifactInfo) o;

            if ( !namespace.equals( that.namespace ) ) return false;
            if ( !name.equals( that.name ) ) return false;
            if ( !projectVersion.equals( that.projectVersion ) ) return false;
            if ( !( version != null ? version.equals( that.version ) : that.version == null ) ) return false;
            return classifier != null ? classifier.equals( that.classifier ) : that.classifier == null;
        }

        @Override
        public int hashCode( )
        {
            int result = namespace.hashCode( );
            result = 31 * result + name.hashCode( );
            result = 31 * result + projectVersion.hashCode( );
            result = 31 * result + ( version != null ? version.hashCode( ) : 0 );
            result = 31 * result + ( classifier != null ? classifier.hashCode( ) : 0 );
            return result;
        }

        @Override
        public String toString( )
        {
            final StringBuilder sb = new StringBuilder( "ArtifactInfo{" );
            sb.append( "namespace='" ).append( namespace ).append( '\'' );
            sb.append( ", name='" ).append( name ).append( '\'' );
            sb.append( ", projectVersion='" ).append( projectVersion ).append( '\'' );
            sb.append( ", version='" ).append( version ).append( '\'' );
            sb.append( ", classifier='" ).append( classifier ).append( '\'' );
            sb.append( '}' );
            return sb.toString( );
        }
    }

    /**
     * Purge the repo. Update db and index of removed artifacts.
     *
     * @param references
     */
    protected void purge( Set<Artifact> references )
    {
        if ( references != null && !references.isEmpty( ) )
        {
            MetadataRepository metadataRepository = repositorySession.getRepository( );
            Map<ArtifactInfo, ArtifactMetadata> metaRemovalList = new HashMap<>( );
            Map<String, Collection<ArtifactMetadata>> metaResolved = new HashMap<>( );
            for ( Artifact reference : references )
            {
                String baseVersion = reference.getVersion( ).getVersion( );
                String namespace = reference.getVersion( ).getProject( ).getNamespace( ).getNamespace( );
                // Needed for tracking in the hashmap
                String metaBaseId = reference.toKey();

                if ( !metaResolved.containsKey( metaBaseId ) )
                {
                    try
                    {
                        metaResolved.put( metaBaseId, metadataRepository.getArtifacts(repositorySession, repository.getId( ),
                            namespace, reference.getId( ), baseVersion ) );
                    }
                    catch ( MetadataResolutionException e )
                    {
                        log.error( "Error during metadata retrieval {}: {}", metaBaseId, e.getMessage( ) );
                    }
                }
                StorageAsset artifactFile = reference.getAsset();

                for ( RepositoryListener listener : listeners )
                {
                    listener.deleteArtifact( metadataRepository, repository.getId( ), namespace,
                        reference.getId( ), reference.getVersion( ).getVersion(),
                            artifactFile.getName( ));
                }
                if (reference.exists())
                {
                    try
                    {
                        repository.deleteItem( reference );
                    }
                    catch ( org.apache.archiva.repository.ContentAccessException e )
                    {
                        log.error( "Error while trying to delete artifact {}: {}", reference.toString( ), e.getMessage( ), e );
                    }
                    catch ( ItemNotFoundException e )
                    {
                        log.error( "Asset deleted from background other thread: {}", e.getMessage( ) );
                    }
                }

                boolean snapshotVersion = VersionUtil.isSnapshot( baseVersion );


                // If this is a snapshot we have to search for artifacts with the same version. And remove all of them.
                if ( snapshotVersion )
                {
                    Collection<ArtifactMetadata> artifacts =
                        metaResolved.get( metaBaseId );
                    if ( artifacts != null )
                    {
                        // cleanup snapshots metadata
                        for ( ArtifactMetadata artifactMetadata : artifacts )
                        {
                            // Artifact metadata and reference version should match.
                            if ( artifactMetadata.getVersion( ).equals( reference.getArtifactVersion( ) ) )
                            {
                                ArtifactInfo info = new ArtifactInfo( artifactMetadata.getNamespace( ), artifactMetadata.getProject( ), artifactMetadata.getProjectVersion( ), artifactMetadata.getVersion( ) );
                                if ( StringUtils.isNotBlank( reference.getClassifier( ) ) )
                                {
                                    info.setClassifier( reference.getClassifier( ) );
                                    metaRemovalList.put( info, artifactMetadata );
                                }
                                else
                                {
                                    // metadataRepository.removeTimestampedArtifact( artifactMetadata, baseVersion );
                                    metaRemovalList.put( info, artifactMetadata );
                                }
                            }
                        }
                    }
                }
                else // otherwise we delete the artifact version
                {
                    ArtifactInfo info = new ArtifactInfo( namespace, reference.getId( ), baseVersion, reference.getArtifactVersion() );
                    for ( ArtifactMetadata metadata : metaResolved.get( metaBaseId ) )
                    {
                        metaRemovalList.put( info, metadata );
                    }
                }
                triggerAuditEvent( repository.getRepository( ).getId( ), reference.toKey(),
                    AuditEvent.PURGE_ARTIFACT );
                // purgeSupportFiles( artifactFile );
            }
            purgeMetadata( metadataRepository, metaRemovalList );
            try
            {
                repositorySession.save( );
            }
            catch ( org.apache.archiva.metadata.repository.MetadataSessionException e )
            {
                e.printStackTrace( );
            }

        }
    }

    /*
     * Purges the metadata. First removes the artifacts. After that empty versions will be removed.
     */
    private void purgeMetadata( MetadataRepository metadataRepository, Map<ArtifactInfo, ArtifactMetadata> dataList )
    {
        Set<ArtifactInfo> projectLevelMetadata = new HashSet<>( );
        for ( Map.Entry<ArtifactInfo, ArtifactMetadata> infoEntry : dataList.entrySet( ) )
        {
            ArtifactInfo info = infoEntry.getKey( );
            try
            {
                removeArtifact( metadataRepository, info, infoEntry.getValue( ) );
                log.debug( "Removed artifact from MetadataRepository {}", info );
            }
            catch ( MetadataRepositoryException e )
            {
                log.error( "Could not remove artifact from MetadataRepository {}: {}", info, e.getMessage( ), e );
            }
            projectLevelMetadata.add( info.projectVersionLevel( ) );
        }
        try {
            repositorySession.save( );
        } catch (MetadataSessionException e) {
            log.error("Could not save sesion {}", e.getMessage());
        }
        Collection<ArtifactMetadata> artifacts = null;
        // Get remaining artifacts and remove project if empty
        for ( ArtifactInfo info : projectLevelMetadata )
        {
            try
            {
                artifacts = metadataRepository.getArtifacts(repositorySession , repository.getId( ), info.getNamespace( ),
                    info.getName( ), info.getProjectVersion( ) );
                if ( artifacts.size( ) == 0 )
                {
                    metadataRepository.removeProjectVersion(repositorySession , repository.getId( ),
                        info.getNamespace( ), info.getName( ), info.getProjectVersion( ) );
                    log.debug( "Removed project version from MetadataRepository {}", info );
                }
            }
            catch ( MetadataResolutionException | MetadataRepositoryException e )
            {
                log.error( "Could not remove project version from MetadataRepository {}: {}", info, e.getMessage( ), e );
            }
        }
        try {
            repositorySession.save( );
        } catch (MetadataSessionException e) {
            log.error("Could not save sesion {}", e.getMessage());

        }

    }

    /*
     * Removes the artifact from the metadataRepository. If a classifier is set, the facet will be removed.
     */
    private void removeArtifact( MetadataRepository metadataRepository, ArtifactInfo artifactInfo, ArtifactMetadata artifactMetadata ) throws MetadataRepositoryException
    {
        if ( artifactInfo.hasClassifier( ) )
        {
            // cleanup facet which contains classifier information
            MavenArtifactFacet mavenArtifactFacet =
                (MavenArtifactFacet) artifactMetadata.getFacet(
                    MavenArtifactFacet.FACET_ID );

            if ( StringUtils.equals( artifactInfo.classifier,
                mavenArtifactFacet.getClassifier( ) ) )
            {
                artifactMetadata.removeFacet( MavenArtifactFacet.FACET_ID );
                String groupId = artifactInfo.getNamespace( ), artifactId =
                    artifactInfo.getName( ),
                    version = artifactInfo.getProjectVersion( );
                MavenArtifactFacet mavenArtifactFacetToCompare = new MavenArtifactFacet( );
                mavenArtifactFacetToCompare.setClassifier( artifactInfo.getClassifier( ) );
                metadataRepository.removeFacetFromArtifact(repositorySession , repository.getId( ), groupId,
                    artifactId, version, mavenArtifactFacetToCompare );
                try {
                    repositorySession.save( );
                } catch (MetadataSessionException e) {
                    log.error("Could not save session {}", e.getMessage());
                }
            }
        }
        else
        {
            metadataRepository.removeTimestampedArtifact(repositorySession , artifactMetadata, artifactInfo.getProjectVersion( ) );
        }
    }

    private void deleteSilently( StorageAsset path )
    {
        try
        {
            path.getStorage().removeAsset(path);
            triggerAuditEvent( repository.getRepository( ).getId( ), path.toString( ), AuditEvent.PURGE_FILE );
        }
        catch ( IOException e )
        {
            log.error( "Error occured during file deletion {}: {} ", path, e.getMessage( ), e );
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
    private void purgeSupportFiles( StorageAsset artifactFile )
    {
        StorageAsset parentDir = artifactFile.getParent( );

        if ( !parentDir.exists() )
        {
            return;
        }

        final String artifactName = artifactFile.getName( );

        StorageUtil.walk(parentDir, a -> {
            if (!a.isContainer() && a.getName().startsWith(artifactName)) deleteSilently(a);
        });

    }

    private void triggerAuditEvent( String repoId, String resource, String action )
    {
        String msg =
            repoId + DELIM + "<system-purge>" + DELIM + "<system>" + DELIM + '\"' + resource + '\"' + DELIM + '\"' +
                action + '\"';

        logger.info( msg );
    }
}
