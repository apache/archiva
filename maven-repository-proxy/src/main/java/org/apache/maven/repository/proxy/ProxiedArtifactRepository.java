package org.apache.maven.repository.proxy;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.HashSet;
import java.util.Set;

/**
 * A proxied artifact repository - contains the artifact repository and additional information about
 * the proxied repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ProxiedArtifactRepository
{
    /**
     * Whether to cache failures or not.
     */
    private boolean cacheFailures;

    /**
     * Whether failures on this repository cause the whole group to fail.
     */
    private boolean hardFail;

    /**
     * Whether to use the network proxy for any requests.
     */
    private boolean useNetworkProxy;

    /**
     * The artifact repository on the other end of the proxy.
     */
    private final ArtifactRepository repository;

    /**
     * Cache of failures that have already occurred, containing paths from the repository root.
     */
    private Set/*<String>*/ failureCache = new HashSet/*<String>*/();

    /**
     * A user friendly name for the repository.
     */
    private String name;

    public ProxiedArtifactRepository( ArtifactRepository repository )
    {
        this.repository = repository;
    }

    public boolean isHardFail()
    {
        return hardFail;
    }

    public boolean isUseNetworkProxy()
    {
        return useNetworkProxy;
    }

    public boolean isCacheFailures()
    {
        return cacheFailures;
    }

    public ArtifactRepository getRepository()
    {
        return repository;
    }

    public boolean isCachedFailure( String path )
    {
        return cacheFailures && failureCache.contains( path );
    }

    public void addFailure( String path )
    {
        if ( cacheFailures )
        {
            failureCache.add( path );
        }
    }

    public void clearFailure( String path )
    {
        if ( cacheFailures )
        {
            failureCache.remove( path );
        }
    }

    public String getName()
    {
        return name;
    }

    public void setCacheFailures( boolean cacheFailures )
    {
        this.cacheFailures = cacheFailures;
    }

    public void setHardFail( boolean hardFail )
    {
        this.hardFail = hardFail;
    }

    public void setUseNetworkProxy( boolean useNetworkProxy )
    {
        this.useNetworkProxy = useNetworkProxy;
    }

    public void setName( String name )
    {
        this.name = name;
    }
}
