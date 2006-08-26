package org.apache.maven.archiva.proxy;

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
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

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
     * Cache of failures that have already occurred, containing paths from the repository root. The value given
     * specifies when the failure should expire.
     */
    private Map/*<String,Long>*/ failureCache = new HashMap/*<String,Long>*/();

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

    /**
     * Check if there is a previously cached failure for requesting the given path.
     *
     * @param path the path
     * @return whether there is a failure
     */
    public boolean isCachedFailure( String path )
    {
        boolean failed = false;
        if ( cacheFailures )
        {
            Long time = (Long) failureCache.get( path );
            if ( time != null )
            {
                if ( System.currentTimeMillis() < time.longValue() )
                {
                    failed = true;
                }
                else
                {
                    clearFailure( path );
                }
            }
        }
        return failed;
    }

    /**
     * Add a failure to the cache.
     *
     * @param path   the path that failed
     * @param policy the policy for when the failure should expire
     */
    public void addFailure( String path, ArtifactRepositoryPolicy policy )
    {
        failureCache.put( path, new Long( calculateExpiryTime( policy ) ) );
    }

    private long calculateExpiryTime( ArtifactRepositoryPolicy policy )
    {
        String updatePolicy = policy.getUpdatePolicy();
        long time;
        if ( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS.equals( updatePolicy ) )
        {
            time = 0;
        }
        else if ( ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY.equals( updatePolicy ) )
        {
            // Get midnight boundary
            Calendar cal = Calendar.getInstance();
            cal.set( Calendar.HOUR_OF_DAY, 0 );
            cal.set( Calendar.MINUTE, 0 );
            cal.set( Calendar.SECOND, 0 );
            cal.set( Calendar.MILLISECOND, 0 );
            cal.add( Calendar.DAY_OF_MONTH, 1 );
            time = cal.getTime().getTime();
        }
        else if ( updatePolicy.startsWith( ArtifactRepositoryPolicy.UPDATE_POLICY_INTERVAL ) )
        {
            String s = updatePolicy.substring( ArtifactRepositoryPolicy.UPDATE_POLICY_INTERVAL.length() + 1 );
            int minutes = Integer.valueOf( s ).intValue();
            Calendar cal = Calendar.getInstance();
            cal.add( Calendar.MINUTE, minutes );
            time = cal.getTime().getTime();
        }
        else
        {
            // else assume "never"
            time = Long.MAX_VALUE;
        }
        return time;
    }

    /**
     * Remove a failure.
     *
     * @param path the path that had previously failed
     */
    public void clearFailure( String path )
    {
        failureCache.remove( path );
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
