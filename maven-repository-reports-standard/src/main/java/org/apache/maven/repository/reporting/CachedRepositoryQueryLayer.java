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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;



/**
 * 
 */
public class CachedRepositoryQueryLayer
    extends AbstractRepositoryQueryLayer
{
    private Cache cache;

    
    public CachedRepositoryQueryLayer( ArtifactRepository repository )
    {
        this.repository = repository;
        
        cache = new Cache( 0.5 );
    }
    
    public double getCacheHitRate()
    {
        return cache.getHitRate();
    }
    
    public boolean containsArtifact( Artifact artifact )
    {
        boolean artifactFound = true;
        
        // @todo should check for snapshot artifacts
        String artifactPath = repository.getBasedir() + "/" + repository.pathOf( artifact );

        if ( cache.get( artifactPath ) == null )
        {
            artifactFound = super.containsArtifact( artifact );
            if ( artifactFound )
            {
                cache.put( artifactPath, artifactPath );
            }
        }

        return artifactFound;
    }

    public boolean containsArtifact( Artifact artifact, Snapshot snapshot )
    {
        boolean artifactFound = true;

        String path = getSnapshotArtifactRepositoryPath( artifact, snapshot );

        if ( cache.get( path ) == null )
        {
            artifactFound = super.containsArtifact( artifact, snapshot );
            if ( artifactFound )
            {
                cache.put( path, path );
            }
        }

        return artifactFound;
    }

    /**
     * Override method to utilize the cache
     */
    protected Metadata getMetadata( Artifact artifact )
        throws RepositoryQueryLayerException
    {
        Metadata metadata = (Metadata) cache.get( artifact.getId() );
        
        if ( metadata == null )
        {
            metadata = super.getMetadata( artifact );
            cache.put( artifact.getId(), metadata );
        }
        
        return metadata;
    }
}
