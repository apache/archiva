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

/**
 *
 */
public class CachedRepositoryQueryLayerTest
    extends AbstractRepositoryQueryLayerTestCase
{
    private Cache cache;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        cache = new Cache( CachedRepositoryQueryLayer.CACHE_HIT_RATIO );

        queryLayer = new CachedRepositoryQueryLayer( new DefaultRepositoryQueryLayer( repository ), cache );
    }

    public void testUseFileCache()
    {
        testContainsArtifactTrue();
        assertEquals( 0, cache.getHitRate(), 0 );
        testContainsArtifactTrue();
        assertEquals( CachedRepositoryQueryLayer.CACHE_HIT_RATIO, cache.getHitRate(), 0 );
    }

    public void testUseMetadataCache()
        throws Exception
    {
        testArtifactVersions();
        assertEquals( 0, cache.getHitRate(), 0 );
        testArtifactVersions();
        assertEquals( CachedRepositoryQueryLayer.CACHE_HIT_RATIO, cache.getHitRate(), 0 );
    }

    public void testUseFileCacheOnSnapshot()
    {
        testContainsSnapshotArtifactTrue();
        assertEquals( 0, cache.getHitRate(), 0 );
        testContainsSnapshotArtifactTrue();
        assertEquals( CachedRepositoryQueryLayer.CACHE_HIT_RATIO, cache.getHitRate(), 0 );
    }
}
