package org.apache.maven.archiva.reporting;

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

import junit.framework.TestCase;

/**
 *
 */
public class CacheTest
    extends TestCase
{
    private Cache cache;

    private static final double CACHE_HIT_RATIO = 0.5;

    private static final double CACHE_HIT_RATIO_THRESHOLD = 0.75;

    public void testCacheManagementBasedOnHitsRatio()
    {
        cache = new Cache( CACHE_HIT_RATIO );
        newCacheObjectTests();

        String key = "key";
        String value = "value";
        for ( int ctr = 1; ctr < 10; ctr++ )
        {
            cache.put( key + ctr, value + ctr );
        }

        while ( cache.getHitRate() < CACHE_HIT_RATIO_THRESHOLD )
        {
            cache.get( "key2" );
        }
        cache.put( "key10", "value10" );
        assertNull( "first key must be expired", cache.get( "key1" ) );
    }

    public void testCacheManagementBasedOnCacheSize()
    {
        cache = new Cache( 9 );
        newCacheObjectTests();

        String key = "key";
        String value = "value";
        for ( int ctr = 1; ctr < 10; ctr++ )
        {
            cache.put( key + ctr, value + ctr );
        }

        cache.put( "key10", "value10" );
        assertNull( "first key must be expired", cache.get( "key1" ) );
        assertEquals( "check cache size to be max size", 9, cache.size() );
    }

    public void testCacheManagementBasedOnCacheSizeAndHitRate()
    {
        cache = new Cache( CACHE_HIT_RATIO, 9 );
        newCacheObjectTests();

        String key = "key";
        String value = "value";
        for ( int ctr = 1; ctr < 5; ctr++ )
        {
            cache.put( key + ctr, value + ctr );
        }

        while ( cache.getHitRate() < CACHE_HIT_RATIO )
        {
            cache.get( "key3" );
        }

        cache.put( "key10", "value10" );
        assertNull( "first key must be expired", cache.get( "key1" ) );

        while ( cache.getHitRate() >= CACHE_HIT_RATIO )
        {
            cache.get( "key11" );
        }

        for ( int ctr = 5; ctr < 10; ctr++ )
        {
            cache.put( key + ctr, value + ctr );
        }

        cache.put( "key11", "value11" );
        assertNull( "second key must be expired", cache.get( "key2" ) );
        assertEquals( "check cache size to be max size", 9, cache.size() );
    }

    public void testCacheOnRedundantData()
    {
        cache = new Cache( CACHE_HIT_RATIO, 9 );
        newCacheObjectTests();

        String key = "key";
        String value = "value";
        for ( int ctr = 1; ctr < 10; ctr++ )
        {
            cache.put( key + ctr, value + ctr );
        }

        cache.put( "key1", "value1" );
        cache.put( "key10", "value10" );
        assertNull( "second key must be gone", cache.get( "key2" ) );
        assertEquals( "check cache size to be max size", 9, cache.size() );
    }

    private void newCacheObjectTests()
    {
        assertEquals( (double) 0, cache.getHitRate(), 0 );
        assertEquals( "check cache size", 0, cache.size() );

        String value = "value";
        String key = "key";

        cache.put( key, value );
        assertEquals( "check cache hit", value, cache.get( key ) );
        assertEquals( (double) 1, cache.getHitRate(), 0 );
        assertEquals( "check cache size", 1, cache.size() );
        assertNull( "check cache miss", cache.get( "none" ) );
        assertEquals( CACHE_HIT_RATIO, cache.getHitRate(), 0 );
        cache.clear();
        assertNull( "check flushed object", cache.get( "key" ) );
        assertEquals( (double) 0, cache.getHitRate(), 0 );
        assertEquals( "check flushed cache size", 0, cache.size() );
        cache.clear();
    }
}
