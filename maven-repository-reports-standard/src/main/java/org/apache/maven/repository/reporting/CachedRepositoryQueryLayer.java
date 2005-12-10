package org.apache.maven.repository.reporting;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;


/**
 *
 */
public class CachedRepositoryQueryLayer
    implements RepositoryQueryLayer
{
    // plexus components
    private ArtifactRepository repository;

    
    //cache for metadata
    private Map cacheMetadata;

    //cache for repository files, all types
    //@todo should also cache missing files???
    private Map cacheFile;
    
    public CachedRepositoryQueryLayer( ArtifactRepository repository )
    {
        this.repository = repository;
        
        cacheMetadata = new HashMap();
        
        cacheFile = new HashMap();
    }

    public boolean containsArtifact( Artifact artifact )
    {
        // @todo should check for snapshot artifacts
        File artifactFile = new File( repository.pathOf( artifact ) );
        
        return fileExists( artifactFile );
    }

    public boolean containsArtifact( Artifact artifact, Snapshot snapshot )
    {
        return false;
    }

    private List getArtifactVersions( Artifact artifact )
    {
        Metadata metadata = getMetadata( artifact );
        
        return metadata.getVersioning().getVersions();
    }

    /**
     * Method to utilize the cache
     */
    private boolean fileExists( File file )
    {
        boolean existing = true;
        
        String path = file.getAbsolutePath();
        if ( !cacheFile.containsKey( path ) )
        {
            if ( file.exists() )
            {
                cacheFile.put( path, file );
            }
            else
            {
                existing = false;
            }
        }
        
        return existing;
    }

    private boolean fileExists( String repositoryPath )
    {
        return fileExists(  new File( repository.getBasedir(), repositoryPath ) );
    }

    /**
     * Method to utilize the cache
     */
    private Metadata getMetadata( Artifact artifact )
    {
        Metadata metadata = null;
        
        if ( cacheMetadata.containsKey( artifact.getId() ) )
        {
            metadata = (Metadata) cacheMetadata.get( artifact.getId() );
        }
        else
        {
            ArtifactRepositoryMetadata repositoryMetadata = new ArtifactRepositoryMetadata( artifact );
            String path = repository.pathOfRemoteRepositoryMetadata( repositoryMetadata );
            if ( fileExists( new File( path ) ) )
            {
                MetadataXpp3Reader reader = new MetadataXpp3Reader();
                try
                {
                    metadata = reader.read( new FileReader( path ) );
                    cacheMetadata.put( path, metadata );
                }
                catch ( Exception e )
                {
                    //@todo should throw something
                }
            }
        }
        
        return metadata;
    }
}
