package org.apache.maven.repository.reporting;

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

import java.util.HashMap;
import java.util.Map;

/**
 * Class to implement caching
 */
public class Cache
{
    private Map cache;

    private DblLinkedList mostRecent;

    private double cacheHitRatio = 0;

    private long cacheMaxSize = 0;

    private long cacheHits = 0;

    private long cacheMiss = 0;

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
    public Cache( long cacheMaxSize )
    {
        this( (double) 1, cacheMaxSize );
    }

    /**
     * Caches all data and expires only the oldest data when either the specified cache hit rate is reached
     * or the maximum cache size is reached.
     */
    public Cache( double cacheHitRatio, long cacheMaxSize )
    {
        this.cacheHitRatio = cacheHitRatio;
        this.cacheMaxSize = cacheMaxSize;

        cache = new HashMap();
    }

    /**
     * Check if the specified key is already mapped to an object.
     *
     * @param key the key used to map the cached object
     * @returns true if the cache contains an object associated with the given key
     */
    public boolean containsKey( Object key )
    {
        boolean contains = cache.containsKey( key );

        if ( contains )
        {
            cacheHits++;
        }
        else
        {
            cacheMiss++;
        }

        return contains;
    }

    /**
     * Check for a cached object and return it if it exists. Returns null when the keyed object is not found
     *
     * @param key the key used to map the cached object
     * @returns the object mapped to the given key, or null if no cache object is mapped to the given key
     */
    public Object get( Object key )
    {
        Object retValue = null;

        if ( cache.containsKey( key ) )
        {
            DblLinkedList cacheEntry = (DblLinkedList) cache.get( key );

            makeMostRecent( cacheEntry );

            retValue = cacheEntry.cacheValue;

            cacheHits++;
        }
        else
        {
            cacheMiss++;
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
        DblLinkedList entry;
        if ( !cache.containsKey( key ) )
        {
            entry = new DblLinkedList( key, value );
            cache.put( key, entry );
            manageCache();
        }
        else
        {
            entry = (DblLinkedList) cache.get( key );
        }

        makeMostRecent( entry );
    }

    /**
     * Compute for the efficiency of this cache.
     *
     * @returns the ratio of cache hits to the cache misses to queries for cache objects
     */
    public double getHitRate()
    {
        return ( cacheHits == 0 && cacheMiss == 0 ) ? 0 : ( (double) cacheHits ) / (double) ( cacheHits + cacheMiss );
    }

    /**
     * Get the total number of cache objects currently cached.
     */
    public long size()
    {
        return cache.size();
    }

    /**
     * Empty the cache and reset the cache hit rate
     */
    public void flush()
    {
        while ( cache.size() > 0 )
        {
            trimCache();
        }
        cacheHits = 0;
        cacheMiss = 0;
        cache = new HashMap();
    }

    private void makeMostRecent( DblLinkedList list )
    {
        if ( mostRecent != list )
        {
            removeFromLinks( list );

            if ( mostRecent != null )
            {
                list.next = mostRecent;
                mostRecent.prev = list;
            }
            mostRecent = list;
        }
    }

    private void removeFromLinks( DblLinkedList list )
    {
        if ( list.prev != null )
        {
            list.prev.next = list.next;
        }
        if ( list.next != null )
        {
            list.next.prev = list.prev;
        }

        list.prev = null;
        list.next = null;
    }

    private void manageCache()
    {
        if ( cacheMaxSize == 0 )
        {
            //desired HitRatio is reached, we can trim the cache to conserve memory
            if ( cacheHitRatio <= getHitRate() )
            {
                trimCache();
            }
        }
        else if ( cache.size() > cacheMaxSize )
        {
            // maximum cache size is reached
            while ( cache.size() > cacheMaxSize )
            {
                trimCache();
            }
        }
        else
        {
            //even though the max has not been reached, the desired HitRatio is already reached,
            //    so we can trim the cache to conserve memory
            if ( cacheHitRatio <= getHitRate() )
            {
                trimCache();
            }
        }
    }

    private void trimCache()
    {
        DblLinkedList leastRecent = getLeastRecent();
        cache.remove( leastRecent.cacheKey );
        if ( cache.size() > 0 )
        {
            removeFromLinks( leastRecent );
        }
        else
        {
            mostRecent = null;
        }
    }

    private DblLinkedList getLeastRecent()
    {
        DblLinkedList trail = mostRecent;

        while ( trail.next != null )
        {
            trail = trail.next;
        }

        return trail;
    }

    private class DblLinkedList
    {
        Object cacheKey;

        Object cacheValue;

        DblLinkedList prev;

        DblLinkedList next;

        public DblLinkedList( Object key, Object value )
        {
            this.cacheKey = key;
            this.cacheValue = value;
        }
    }
}
