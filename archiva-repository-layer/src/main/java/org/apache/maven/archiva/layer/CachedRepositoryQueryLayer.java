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
import org.codehaus.plexus.cache.Cache;
import org.codehaus.plexus.cache.CacheException;
import org.codehaus.plexus.cache.CacheHints;
import org.codehaus.plexus.cache.factory.CacheFactory;

import java.util.List;

/**
 * CachedRepositoryQueryLayer - simple wrapper around another non-cached Repository Query Layer.
 *
 * @version $Id$
 */
public class CachedRepositoryQueryLayer
    implements RepositoryQueryLayer
{
    private Cache cache;

    private RepositoryQueryLayer layer;

    public CachedRepositoryQueryLayer( RepositoryQueryLayer layer )
        throws RepositoryQueryLayerException
    {
        this.layer = layer;
        String repoId = layer.getRepository().getId();
        
        CacheHints hints = new CacheHints();
        hints.setName( repoId );
        hints.setOverflowToDisk( false );
        try
        {
            this.cache = CacheFactory.getInstance().getCache( repoId, hints );
        }
        catch ( CacheException e )
        {
            throw new RepositoryQueryLayerException( "Unable to initialize cache: " + e.getMessage(), e );
        }
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
