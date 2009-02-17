package org.apache.maven.archiva.consumers.core.repository;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.archiva.common.utils.VersionComparator;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.ContentNotFoundException;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.events.RepositoryListener;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;

/**
 * <p>
 * This will look in a single managed repository, and purge any snapshots that are present
 * that have a corresponding released version on the same repository.
 * </p>
 * 
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
 * </p>
 *
 * @version $Id$
 */
public class CleanupReleasedSnapshotsRepositoryPurge
    extends AbstractRepositoryPurge
{
    private MetadataTools metadataTools;
    
    private ArchivaConfiguration archivaConfig;
    
    private RepositoryContentFactory repoContentFactory;

    public CleanupReleasedSnapshotsRepositoryPurge( ManagedRepositoryContent repository, MetadataTools metadataTools,
                                                    ArchivaConfiguration archivaConfig,
                                                    RepositoryContentFactory repoContentFactory,
                                                    List<RepositoryListener> listeners )
    {
        super( repository, listeners );
        this.metadataTools = metadataTools;
        this.archivaConfig = archivaConfig;
        this.repoContentFactory = repoContentFactory;
    }

    public void process( String path )
        throws RepositoryPurgeException
    {
        try
        {
            File artifactFile = new File( repository.getLocalPath(), path );

            if ( !artifactFile.exists() )
            {
                // Nothing to do here, file doesn't exist, skip it.
                return;
            }

            ArtifactReference artifactRef = repository.toArtifactReference( path );

            if ( !VersionUtil.isSnapshot( artifactRef.getVersion() ) )
            {
                // Nothing to do here, not a snapshot, skip it.
                return;
            }

            ProjectReference reference = new ProjectReference();
            reference.setGroupId( artifactRef.getGroupId() );
            reference.setArtifactId( artifactRef.getArtifactId() );
            
            // Gather up all of the versions.
            List<String> allVersions = new ArrayList<String>( repository.getVersions( reference ) );

            List<ManagedRepositoryConfiguration> repos = archivaConfig.getConfiguration().getManagedRepositories();
            for( ManagedRepositoryConfiguration repo : repos )
            {   
                if( repo.isReleases() && !repo.getId().equals( repository.getId() ) )
                {   
                    try
                    {   
                        ManagedRepositoryContent repoContent = repoContentFactory.getManagedRepositoryContent( repo.getId() );                        
                        allVersions.addAll( repoContent.getVersions( reference ) );
                    }
                    catch( RepositoryNotFoundException  e )
                    {
                        // swallow
                    }
                    catch( RepositoryException  e )
                    {
                        // swallow
                    }
                }
            }

            // Split the versions into released and snapshots.
            List<String> releasedVersions = new ArrayList<String>();
            List<String> snapshotVersions = new ArrayList<String>();

            for ( String version : allVersions )
            {   
                if ( VersionUtil.isSnapshot( version ) )
                {
                    snapshotVersions.add( version );
                }
                else
                {
                    releasedVersions.add( version );
                }
            }

            Collections.sort( allVersions, VersionComparator.getInstance() );
            Collections.sort( releasedVersions, VersionComparator.getInstance() );
            Collections.sort( snapshotVersions, VersionComparator.getInstance() );
            
            // Now clean out any version that is earlier than the highest released version.
            boolean needsMetadataUpdate = false;

            VersionedReference versionRef = new VersionedReference();
            versionRef.setGroupId( artifactRef.getGroupId() );
            versionRef.setArtifactId( artifactRef.getArtifactId() );
            
            ArchivaArtifact artifact =
                new ArchivaArtifact( artifactRef.getGroupId(), artifactRef.getArtifactId(), artifactRef.getVersion(),
                                     artifactRef.getClassifier(), artifactRef.getType(), repository.getId() );
            
            for ( String version : snapshotVersions )
            {   
                if( releasedVersions.contains( VersionUtil.getReleaseVersion( version ) ) )
                {                    
                    versionRef.setVersion( version );
                    repository.deleteVersion( versionRef );
                    
                    // TODO: looks incomplete, might not delete related artifacts?
                    for ( RepositoryListener listener : listeners )
                    {
                        listener.deleteArtifact( repository, artifact );
                    }
                    
                    needsMetadataUpdate = true;
                }
            }           
                        
            if ( needsMetadataUpdate )
            {
                updateMetadata( artifactRef );
            }
        }
        catch ( LayoutException e )
        {
            throw new RepositoryPurgeException( e.getMessage(), e );
        }
        catch ( ContentNotFoundException e )
        {
            throw new RepositoryPurgeException( e.getMessage(), e );
        }
    }

    private void updateMetadata( ArtifactReference artifact )
    {
        VersionedReference versionRef = new VersionedReference();
        versionRef.setGroupId( artifact.getGroupId() );
        versionRef.setArtifactId( artifact.getArtifactId() );
        versionRef.setVersion( artifact.getVersion() );

        ProjectReference projectRef = new ProjectReference();
        projectRef.setGroupId( artifact.getGroupId() );
        projectRef.setArtifactId( artifact.getArtifactId() );

        try
        {
            metadataTools.updateMetadata( repository, versionRef );
        }
        catch ( ContentNotFoundException e )
        {
            // Ignore. (Just means we have no snapshot versions left to reference).
        }
        catch ( RepositoryMetadataException e )
        {
            // Ignore. 
        }
        catch ( IOException e )
        {
            // Ignore. 
        }
        catch ( LayoutException e )
        {
            // Ignore.
        }

        try
        {
            metadataTools.updateMetadata( repository, projectRef );
        }
        catch ( ContentNotFoundException e )
        {
            // Ignore. (Just means we have no snapshot versions left to reference).
        }
        catch ( RepositoryMetadataException e )
        {
            // Ignore. 
        }
        catch ( IOException e )
        {
            // Ignore. 
        }
        catch ( LayoutException e )
        {
            // Ignore.
        }
    }
}
