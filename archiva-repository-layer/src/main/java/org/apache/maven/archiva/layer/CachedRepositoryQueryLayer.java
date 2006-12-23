package org.apache.maven.archiva.layer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.List;


/**
 *
 */
public class CachedRepositoryQueryLayer
    implements RepositoryQueryLayer
{
    private Cache cache;

    public static final double CACHE_HIT_RATIO = 0.5;

    private RepositoryQueryLayer layer;

    public CachedRepositoryQueryLayer( RepositoryQueryLayer layer )
    {
        this.layer = layer;

        cache = new Cache( CACHE_HIT_RATIO );
    }

    public CachedRepositoryQueryLayer( RepositoryQueryLayer layer, Cache cache )
    {
        this.cache = cache;
        this.layer = layer;
    }

    public double getCacheHitRate()
    {
        return cache.getHitRate();
    }

    public boolean containsArtifact( Artifact artifact )
    {
        boolean artifactFound = true;

        String artifactPath = layer.getRepository().pathOf( artifact );

        if ( cache.get( artifactPath ) == null )
        {
            artifactFound = layer.containsArtifact( artifact );
            if ( artifactFound )
            {
                cache.put( artifactPath, artifactPath );
            }
        }

        return artifactFound;
    }

    public List getVersions( Artifact artifact )
        throws RepositoryQueryLayerException
    {
        List list = (List) cache.get( artifact.getId() );

        if ( list == null )
        {
            list = layer.getVersions( artifact );
            cache.put( artifact.getId(), list );
        }

        return list;
    }

    public ArtifactRepository getRepository()
    {
        return layer.getRepository();
    }
}
