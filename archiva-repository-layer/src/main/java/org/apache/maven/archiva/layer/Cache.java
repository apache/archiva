package org.apache.maven.archiva.layer;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class to implement caching.
 */
public class Cache
{
    private final Map cache;

    private final double cacheHitRatio;

    private final int cacheMaxSize;

    private long cacheHits;

    private long cacheMiss;

    /**
     * Caches all data and expires only the oldest data when the specified cache hit rate is reached.
     */
    public Cache( double cacheHitRatio )
    {
        this( cacheHitRatio, 0 );
    }

    /**
     * Caches all data and expires only the oldest data when the maximum cache size is reached
     */
    public Cache( int cacheMaxSize )
    {
        this( (double) 1, cacheMaxSize );
    }

    /**
     * Caches all data and expires only the oldest data when either the specified cache hit rate is reached
     * or the maximum cache size is reached.
     */
    public Cache( double cacheHitRatio, int cacheMaxSize )
    {
        this.cacheHitRatio = cacheHitRatio;
        this.cacheMaxSize = cacheMaxSize;

        if ( cacheMaxSize > 0 )
        {
            cache = new LinkedHashMap( cacheMaxSize );
        }
        else
        {
            cache = new LinkedHashMap();
        }
    }

    /**
     * Check if the specified key is already mapped to an object.
     *
     * @param key the key used to map the cached object
     * @return true if the cache contains an object associated with the given key
     */
    public boolean containsKey( Object key )
    {
        boolean contains;
        synchronized ( cache )
        {
            contains = cache.containsKey( key );

            if ( contains )
            {
                cacheHits++;
            }
            else
            {
                cacheMiss++;
            }
        }

        return contains;
    }

    /**
     * Check for a cached object and return it if it exists. Returns null when the keyed object is not found
     *
     * @param key the key used to map the cached object
     * @return the object mapped to the given key, or null if no cache object is mapped to the given key
     */
    public Object get( Object key )
    {
        Object retValue = null;

        synchronized ( cache )
        {
            if ( cache.containsKey( key ) )
            {
                // remove and put: this promotes it to the top since we use a linked hash map
                retValue = cache.remove( key );

                cache.put( key, retValue );

                cacheHits++;
            }
            else
            {
                cacheMiss++;
            }
        }

        return retValue;
    }

    /**
     * Cache the given value and map it using the given key
     *
     * @param key   the object to map the valued object
     * @param value the object to cache
     */
    public void put( Object key, Object value )
    {
        // remove and put: this promotes it to the top since we use a linked hash map
        synchronized ( cache )
        {
            if ( cache.containsKey( key ) )
            {
                cache.remove( key );
            }

            cache.put( key, value );
        }

        manageCache();
    }

    /**
     * Compute for the efficiency of this cache.
     *
     * @return the ratio of cache hits to the cache misses to queries for cache objects
     */
    public double getHitRate()
    {
        synchronized ( cache )
        {
            return cacheHits == 0 && cacheMiss == 0 ? 0 : (double) cacheHits / (double) ( cacheHits + cacheMiss );
        }
    }

    /**
     * Get the total number of cache objects currently cached.
     */
    public int size()
    {
        return cache.size();
    }

    /**
     * Empty the cache and reset the cache hit rate
     */
    public void clear()
    {
        synchronized ( cache )
        {
            cacheHits = 0;
            cacheMiss = 0;
            cache.clear();
        }
    }

    private void manageCache()
    {
        synchronized ( cache )
        {
            Iterator iterator = cache.entrySet().iterator();
            if ( cacheMaxSize == 0 )
            {
                //desired HitRatio is reached, we can trim the cache to conserve memory
                if ( cacheHitRatio <= getHitRate() )
                {
                    iterator.next();
                    iterator.remove();
                }
            }
            else if ( cache.size() > cacheMaxSize )
            {
                // maximum cache size is reached
                while ( cache.size() > cacheMaxSize )
                {
                    iterator.next();
                    iterator.remove();
                }
            }
            else
            {
                //even though the max has not been reached, the desired HitRatio is already reached,
                //    so we can trim the cache to conserve memory
                if ( cacheHitRatio <= getHitRate() )
                {
                    iterator.next();
                    iterator.remove();
                }
            }
        }
    }

}
