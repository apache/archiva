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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    protected void purge( Set<ArtifactReference> references )
    {
        if ( references != null && !references.isEmpty( ) )
        {
            MetadataRepository metadataRepository = repositorySession.getRepository( );
            Map<ArtifactInfo, ArtifactMetadata> metaRemovalList = new HashMap<>( );
            Map<String, Collection<ArtifactMetadata>> metaResolved = new HashMap<>( );
            for ( ArtifactReference reference : references )
            {
                String baseVersion = VersionUtil.getBaseVersion( reference.getVersion( ) );
                // Needed for tracking in the hashmap
                String metaBaseId = reference.getGroupId( ) + "/" + reference.getArtifactId( ) + "/" + baseVersion;

                if ( !metaResolved.containsKey( metaBaseId ) )
                {
                    try
                    {
                        metaResolved.put( metaBaseId, metadataRepository.getArtifacts( repository.getId( ), reference.getGroupId( ),
                            reference.getArtifactId( ), baseVersion ) );
                    }
                    catch ( MetadataResolutionException e )
                    {
                        log.error( "Error during metadata retrieval {}: {}", metaBaseId, e.getMessage( ) );
                    }
                }
                Path artifactFile = repository.toFile( reference );

                for ( RepositoryListener listener : listeners )
                {
                    listener.deleteArtifact( metadataRepository, repository.getId( ), reference.getGroupId( ),
                        reference.getArtifactId( ), reference.getVersion( ),
                        artifactFile.getFileName( ).toString( ) );
                }
                try
                {
                    Files.delete( artifactFile );
                    log.debug( "File deleted: {}", artifactFile.toAbsolutePath( ) );
                }
                catch ( IOException e )
                {
                    log.error( "Could not delete file {}: {}", artifactFile.toAbsolutePath( ), e.getMessage( ), e );
                    continue;
                }
                try
                {
                    repository.deleteArtifact( reference );
                }
                catch ( ContentNotFoundException e )
                {
                    log.warn( "skip error deleting artifact {}: {}", reference, e.getMessage( ) );
                }

                boolean snapshotVersion = VersionUtil.isSnapshot( reference.getVersion( ) );


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
                            if ( artifactMetadata.getVersion( ).equals( reference.getVersion( ) ) )
                            {
                                ArtifactInfo info = new ArtifactInfo( artifactMetadata.getNamespace( ), artifactMetadata.getProject( ), artifactMetadata.getProjectVersion( ), artifactMetadata.getVersion( ) );
                                if ( StringUtils.isNotBlank( reference.getClassifier( ) ) )
                                {
                                    info.setClassifier( reference.getClassifier( ) );
                                    metaRemovalList.put( info, artifactMetadata );
                                }
                                else
                                {
                                    // metadataRepository.removeArtifact( artifactMetadata, baseVersion );
                                    metaRemovalList.put( info, artifactMetadata );
                                }
                            }
                        }
                    }
                }
                else // otherwise we delete the artifact version
                {
                    ArtifactInfo info = new ArtifactInfo( reference.getGroupId( ), reference.getArtifactId( ), baseVersion, reference.getVersion( ) );
                    for ( ArtifactMetadata metadata : metaResolved.get( metaBaseId ) )
                    {
                        metaRemovalList.put( info, metadata );
                    }
                }
                triggerAuditEvent( repository.getRepository( ).getId( ), ArtifactReference.toKey( reference ),
                    AuditEvent.PURGE_ARTIFACT );
                purgeSupportFiles( artifactFile );
            }
            purgeMetadata( metadataRepository, metaRemovalList );
            repositorySession.save( );

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
        metadataRepository.save( );
        Collection<ArtifactMetadata> artifacts = null;
        // Get remaining artifacts and remove project if empty
        for ( ArtifactInfo info : projectLevelMetadata )
        {
            try
            {
                artifacts = metadataRepository.getArtifacts( repository.getId( ), info.getNamespace( ), info.getName( ),
                    info.getProjectVersion( ) );
                if ( artifacts.size( ) == 0 )
                {
                    metadataRepository.removeProjectVersion( repository.getId( ), info.getNamespace( ),
                        info.getName( ), info.getProjectVersion( ) );
                    log.debug( "Removed project version from MetadataRepository {}", info );
                }
            }
            catch ( MetadataResolutionException | MetadataRepositoryException e )
            {
                log.error( "Could not remove project version from MetadataRepository {}: {}", info, e.getMessage( ), e );
            }
        }
        metadataRepository.save( );

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
                metadataRepository.removeArtifact( repository.getId( ), groupId, artifactId,
                    version, mavenArtifactFacetToCompare );
                metadataRepository.save( );
            }
        }
        else
        {
            metadataRepository.removeArtifact( artifactMetadata, artifactInfo.getProjectVersion( ) );
        }
    }

    private void deleteSilently( Path path )
    {
        try
        {
            Files.deleteIfExists( path );
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
    private void purgeSupportFiles( Path artifactFile )
    {
        Path parentDir = artifactFile.getParent( );

        if ( !Files.exists( parentDir ) )
        {
            return;
        }

        final String artifactName = artifactFile.getFileName( ).toString( );

        try
        {
            Files.find( parentDir, 3,
                ( path, basicFileAttributes ) -> path.getFileName( ).toString( ).startsWith( artifactName )
                    && Files.isRegularFile( path ) ).forEach( this::deleteSilently );
        }
        catch ( IOException e )
        {
            log.error( "Purge of support files failed {}: {}", artifactFile, e.getMessage( ), e );
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
