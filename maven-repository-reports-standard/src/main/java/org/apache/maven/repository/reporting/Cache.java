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

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Cache
{
    private Map cache;

    private DblLinkedList mostRecent;
    private double cacheHitRatio = 0;
    private long cacheMaxSize = 0;
    
    private long cacheHits = 0;
    private long cacheMiss = 0;

    public Cache( double cacheHitRatio )
    {
        this( cacheHitRatio, 0 );
    }

    public Cache( double cacheHitRatio, long cacheMaxSize )
    {
        this.cacheHitRatio = cacheHitRatio;
        this.cacheMaxSize = cacheMaxSize;
        
        cache = new HashMap();
    }

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

    public double getHitRate()
    {
        return ( cacheHits == 0 && cacheMiss == 0 ) ? 0 : ( (double) cacheHits ) / (double) ( cacheHits + cacheMiss );
    }
    
    public long size()
    {
        return cache.size();
    }
    
    public void flush()
    {
        while ( cache.size() > 0 )
            trimCache();
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
            list.prev.next = list.next;
        if ( list.next != null )
            list.next.prev = list.prev;
        
        list.prev = null;
        list.next = null;
    }

    private void manageCache()
    {
        if ( cacheMaxSize == 0 )
        {
            //if desired HitRatio is reached, we can trim the cache to conserve memory
            if ( cacheHitRatio <= getHitRate() )
            {
                trimCache();
            }
        }
        else if ( cache.size() > cacheMaxSize )
        {
            //trim cache regardless of cacheHitRatio
            while( cache.size() > cacheMaxSize )
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

        while( trail.next != null )
            trail = trail.next;

        return trail;
    }

    private class DblLinkedList {
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
