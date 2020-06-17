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

import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.metadata.audit.RepositoryListener;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.repository.content.BaseRepositoryContentLayout;
import org.apache.archiva.repository.content.ContentNotFoundException;
import org.apache.archiva.repository.content.ContentAccessException;
import org.apache.archiva.repository.content.LayoutException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.content.Artifact;
import org.apache.archiva.repository.content.ItemNotFoundException;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.content.Version;
import org.apache.archiva.repository.content.base.ArchivaItemSelector;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.metadata.base.MetadataTools;
import org.apache.archiva.repository.storage.StorageAsset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * This will look in a single managed repository, and purge any snapshots that are present
 * that have a corresponding released version on the same repository.
 * </p>
 * <p>
 * So, if you have the following (presented in the m2/default layout form) ...
 * <pre>
 *   /com/foo/foo-tool/1.0-SNAPSHOT/foo-tool-1.0-SNAPSHOT.jar
 *   /com/foo/foo-tool/1.1-SNAPSHOT/foo-tool-1.1-SNAPSHOT.jar
 *   /com/foo/foo-tool/1.2.1-SNAPSHOT/foo-tool-1.2.1-SNAPSHOT.jar
 *   /com/foo/foo-tool/1.2.1/foo-tool-1.2.1.jar
 *   /com/foo/foo-tool/2.0-SNAPSHOT/foo-tool-2.0-SNAPSHOT.jar
 *   /com/foo/foo-tool/2.0/foo-tool-2.0.jar
 *   /com/foo/foo-tool/2.1-SNAPSHOT/foo-tool-2.1-SNAPSHOT.jar
 * </pre>
 * then the current highest ranked released (non-snapshot) version is 2.0, which means
 * the snapshots from 1.0-SNAPSHOT, 1.1-SNAPSHOT, 1.2.1-SNAPSHOT, and 2.0-SNAPSHOT can
 * be purged.  Leaving 2.1-SNAPSHOT in alone.
 */
public class CleanupReleasedSnapshotsRepositoryPurge
    extends AbstractRepositoryPurge
{
    private MetadataTools metadataTools;

    private RepositoryRegistry repositoryRegistry;

    public CleanupReleasedSnapshotsRepositoryPurge( ManagedRepositoryContent repository, MetadataTools metadataTools,
                                                    RepositoryRegistry repositoryRegistry,
                                                    RepositorySession repositorySession,
                                                    List<RepositoryListener> listeners )
    {
        super( repository, repositorySession, listeners );
        this.metadataTools = metadataTools;
        this.repositoryRegistry = repositoryRegistry;
    }

    @Override
    public void process( String path )
        throws RepositoryPurgeException
    {
        try
        {
            StorageAsset artifactFile = repository.getRepository( ).getRoot().resolve( path );
            BaseRepositoryContentLayout layout = repository.getLayout( BaseRepositoryContentLayout.class );

            if ( !artifactFile.exists() )
            {
                // Nothing to do here, file doesn't exist, skip it.
                return;
            }

            Artifact artifactRef = layout.getArtifact( path );
            // ArtifactReference artifactRef = repository.toArtifactReference( path );

            if ( !VersionUtil.isSnapshot( artifactRef.getVersion().getId( ) ) )
            {
                // Nothing to do here, not a snapshot, skip it.
                return;
            }

            ItemSelector projectSelector = ArchivaItemSelector.builder( )
                .withNamespace( artifactRef.getNamespace( ).getId() )
                .withProjectId( artifactRef.getId( ) )
                .build();


            // Gether the released versions
            List<String> releasedVersions = new ArrayList<>( );

            Collection<org.apache.archiva.repository.ManagedRepository> repos = repositoryRegistry.getManagedRepositories( );
            for ( org.apache.archiva.repository.ManagedRepository repo : repos )
            {

                if ( repo.getActiveReleaseSchemes().contains( ReleaseScheme.RELEASE ))
                {
                    BaseRepositoryContentLayout repoContent = repo.getContent().getLayout( BaseRepositoryContentLayout.class );
                    Project proj = repoContent.getProject( projectSelector );
                    for ( Version version : repoContent.getVersions( proj ) )
                    {
                        if ( !VersionUtil.isSnapshot( version.getId() ) )
                        {
                            releasedVersions.add( version.getId() );
                        }
                    }
                }
            }

            Collections.sort( releasedVersions, VersionComparator.getInstance( ) );

            // Now clean out any version that is earlier than the highest released version.
            boolean needsMetadataUpdate = false;

            ArchivaItemSelector.Builder versionSelectorBuilder = ArchivaItemSelector.builder( )
                .withNamespace( artifactRef.getNamespace().getId() )
                .withProjectId( artifactRef.getId( ) )
                .withArtifactId( artifactRef.getId( ) );

            MetadataRepository metadataRepository = repositorySession.getRepository( );

            if ( releasedVersions.contains( VersionUtil.getReleaseVersion( artifactRef.getVersion().getId( ) ) ) )
            {
                ArchivaItemSelector selector = versionSelectorBuilder.withVersion( artifactRef.getVersion().getId( ) ).build( );
                Version version = layout.getVersion( selector );
                if (version.exists())
                {
                    repository.deleteItem( version );
                }
                for ( RepositoryListener listener : listeners )
                {
                    listener.deleteArtifact( metadataRepository, repository.getId( ), artifactRef.getNamespace().getId(),
                        artifactRef.getId( ), artifactRef.getVersion().getId( ),
                        artifactFile.getName() );
                }
                metadataRepository.removeProjectVersion( repositorySession, repository.getId( ),
                    artifactRef.getNamespace().getId(), artifactRef.getId( ), artifactRef.getVersion().getId() );

                needsMetadataUpdate = true;
            }

            if ( needsMetadataUpdate )
            {
                updateMetadata( artifactRef );
            }
        }
        catch ( LayoutException e )
        {
            log.debug( "Not processing file that is not an artifact: {}", e.getMessage( ) );
        }
        catch ( MetadataRepositoryException e )
        {
            log.error( "Could not remove metadata during cleanup of released snapshots of {}", path, e );
        }
        catch ( ContentAccessException e )
        {
            e.printStackTrace( );
        }
        catch ( ItemNotFoundException e )
        {
            log.error( "Could not find item to delete {}",e.getMessage( ), e );
        }
    }


    /*
     * TODO: Uses a deprecated API, but if we use the API with location string, it does not work as expected
     * -> not sure what needs to be changed here.
     */
    @SuppressWarnings( "deprecation" )
    private void updateMetadata( Artifact artifact )
    {

        ItemSelector versionRef = ArchivaItemSelector.builder( )
            .withNamespace( artifact.getNamespace( ).getId( ) )
            .withProjectId( artifact.getId( ) )
            .withVersion( artifact.getVersion( ).getId( ) ).build( );

        ItemSelector projectRef = ArchivaItemSelector.builder()
        .withNamespace( artifact.getNamespace().getId( ) )
        .withProjectId(  artifact.getId( ) ).build();

        try
        {
            metadataTools.updateVersionMetadata( repository, versionRef );
        }
        catch ( ContentNotFoundException e )
        {
            // Ignore. (Just means we have no snapshot versions left to reference).
        }
        catch ( RepositoryMetadataException | IOException | LayoutException e )
        {
            // Ignore.
        }

        try
        {
            metadataTools.updateProjectMetadata( repository, projectRef );
        }
        catch ( ContentNotFoundException | RepositoryMetadataException | IOException | LayoutException e )
        {
            // Ignore. (Just means we have no snapshot versions left to reference).
        }

    }
}
