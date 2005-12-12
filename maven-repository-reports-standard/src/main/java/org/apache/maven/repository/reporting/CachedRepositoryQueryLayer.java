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
    extends AbstractRepositoryQueryLayer
{
    //@todo caches should expire    
    //cache for metadata
    private Map cacheMetadata;

    //cache for repository files, all types
    //@todo ???should also cache missing files
    private Map cacheFile;
    
    //@todo ???should a listener be required???
    private long cacheHits = 0;

    public CachedRepositoryQueryLayer( ArtifactRepository repository )
    {
        this.repository = repository;
        
        cacheMetadata = new HashMap();
        
        cacheFile = new HashMap();
    }
    
    public long getCacheHits()
    {
        return cacheHits;
    }

    public boolean containsArtifact( Artifact artifact )
    {
        boolean artifactFound = true;
        
        // @todo should check for snapshot artifacts
        File artifactFile = new File( repository.pathOf( artifact ) );

        if ( !checkFileCache( artifactFile ) )
        {
            artifactFound = super.containsArtifact( artifact );
            if ( artifactFound )
            {
                addToFileCache( artifactFile );
            }
        }

        return artifactFound;
    }

    public boolean containsArtifact( Artifact artifact, Snapshot snapshot )
    {
        boolean artifactFound = true;

        String path = getSnapshotArtifactRepositoryPath( artifact, snapshot );

        if ( !checkFileCache( path ) )
        {
            artifactFound = super.containsArtifact( artifact, snapshot );
            if ( artifactFound )
            {
                addToFileCache( new File( repository.getBasedir(), path ) );
            }
        }

        return artifactFound;
    }

    /**
     * Method to utilize the cache
     */
    private boolean checkFileCache( File file )
    {
        boolean existing = false;

        if ( cacheFile.containsKey( file ) )
        {
            cacheHits++;
            existing = true;
        }

        return existing;
    }

    private boolean checkFileCache( String repositoryPath )
    {
        return checkFileCache(  new File( repository.getBasedir(), repositoryPath ) );
    }
    
    private void addToFileCache( File file )
    {
        cacheFile.put( file, file );
    }

    /**
     * Override method to utilize the cache
     */
    protected Metadata getMetadata( Artifact artifact )
        throws RepositoryQueryLayerException
    {
        Metadata metadata = null;
        
        if ( cacheMetadata.containsKey( artifact.getId() ) )
        {
            cacheHits++;
            metadata = (Metadata) cacheMetadata.get( artifact.getId() );
        }
        else
        {
            metadata = super.getMetadata( artifact );
            cacheMetadata.put( artifact.getId(), metadata );
        }
        
        return metadata;
    }
}
