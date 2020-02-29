package org.apache.archiva.repository.storage.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.storage.mock.MockAsset;
import org.apache.archiva.repository.storage.mock.MockStorage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
class StorageUtilTest
{
    private static int LEVEL1 = 12;
    private static int LEVEL2 = 13;
    private static int LEVEL3 = 6;



    private MockAsset createTree() {
        return createTree( LEVEL1, LEVEL2, LEVEL3 );
    }

    private MockAsset createTree(int... levelElements) {
        MockAsset root = new MockAsset( "" );
        recurseSubTree( root, 0, levelElements );
        return root;
    }

    private void recurseSubTree(MockAsset parent, int level, int[] levelElements) {
        if (level < levelElements.length)
        {
            for ( int k = 0; k < levelElements[level]; k++ )
            {
                String name = parent.getName( ) + String.format( "%03d", k );
                MockAsset asset = new MockAsset( parent, name );
                recurseSubTree( asset, level + 1, levelElements );
            }
        }
    }

    @Test
    void testWalkFromRoot() {
        StorageAsset root = createTree( );
        ConsumeVisitStatus status = new ConsumeVisitStatus( );

        StorageUtil.walk( root, status );
        int expected = LEVEL1 * LEVEL2 * LEVEL3 + LEVEL1 * LEVEL2 + LEVEL1 + 1;
        assertEquals( expected, status.size() );
        StorageAsset first = root.list( ).get( 0 ).list( ).get( 0 ).list().get(0);
        assertEquals( first, status.getFirst( ) );
        assertEquals( root, status.getLast( ) );
    }

    @Test
    void testWalkFromChild() {
        StorageAsset root = createTree( );
        ConsumeVisitStatus status = new ConsumeVisitStatus( );
        StorageAsset testRoot = root.list( ).get( 3 );

        StorageUtil.walk( testRoot, status );
        int expected = LEVEL2 * LEVEL3 + LEVEL2 + 1;
        assertEquals( expected, status.size() );
        StorageAsset first = root.list( ).get( 3 ).list( ).get( 0 ).list().get(0);
        assertEquals( first, status.getFirst( ) );
        assertEquals( testRoot, status.getLast( ) );
    }


    @Test
    void testWalkFromRootWithCondition() {
        StorageAsset root = createTree( );
        StopVisitStatus status = new StopVisitStatus( );
        status.setStopCondition( a -> a.getName().equals("001002003") );

        StorageUtil.walk( root, status );
        assertEquals( "001002003", status.getLast( ).getName() );
        int expected = LEVEL2 * LEVEL3 + LEVEL2 + 2 * LEVEL3 + 1 + 1 + 1 + 4;
        assertEquals( expected, status.size() );
    }

    @Test
    void testStream() {
        StorageAsset root = createTree( );
        ConsumeVisitStatus status = new ConsumeVisitStatus( );

        List<StorageAsset> result;
        try ( Stream<StorageAsset> stream = StorageUtil.newAssetStream( root, false ) )
        {
            result = stream.filter( a -> a.getName( ).startsWith( "001" ) ).collect( Collectors.toList());
        }
        int expected = LEVEL2 * LEVEL3 + LEVEL2 + 1;
        assertEquals( expected, result.size( ) );
        assertEquals( "001", result.get( result.size( ) - 1 ).getName() );
        assertEquals( "001012", result.get( result.size( ) - 2 ).getName() );
    }

    @Test
    void testStreamParallel() {
        StorageAsset root = createTree( );
        ConsumeVisitStatus status = new ConsumeVisitStatus( );

        List<StorageAsset> result;
        try ( Stream<StorageAsset> stream = StorageUtil.newAssetStream( root, true ) )
        {
            result = stream.filter( a -> a.getName( ).startsWith( "001" ) ).collect( Collectors.toList());
        }
        int expected = LEVEL2 * LEVEL3 + LEVEL2 + 1;
        assertEquals( expected, result.size( ) );
    }


    @Test
    void testDelete() throws IOException
    {
        MockAsset root = createTree( );
        MockStorage storage = new MockStorage( root );

        StorageUtil.deleteRecursively( root );
        int expected = LEVEL1 * LEVEL2 * LEVEL3 + LEVEL1 * LEVEL2 + LEVEL1 + 1;
        assertEquals( expected, storage.getStatus( ).size( MockStorage.REMOVE ) );

    }

    @Test
    void testDeleteWithException() throws IOException
    {
        MockAsset root = createTree( );
        MockStorage storage = new MockStorage( root );
        root.list( ).get( 1 ).list( ).get( 2 ).setThrowException( true );

        StorageUtil.deleteRecursively( root );
        int expected = LEVEL1 * LEVEL2 * LEVEL3 + LEVEL1 * LEVEL2 + LEVEL1 + 1;
        assertEquals( expected, storage.getStatus( ).size( MockStorage.REMOVE ) );

    }

    @Test
    void testDeleteWithExceptionFailFast() throws IOException
    {
        MockAsset root = createTree( );
        MockStorage storage = new MockStorage( root );
        root.list( ).get( 1 ).list( ).get( 2 ).setThrowException( true );

        StorageUtil.deleteRecursively( root, true );
        int expected = 113;
        assertEquals( expected, storage.getStatus( ).size( MockStorage.REMOVE ) );

    }
}