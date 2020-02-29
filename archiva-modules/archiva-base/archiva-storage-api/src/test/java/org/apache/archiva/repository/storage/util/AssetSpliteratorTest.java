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

package org.apache.archiva.repository.storage.util;

import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.storage.mock.MockAsset;
import org.junit.jupiter.api.Test;

import java.util.Spliterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test the AssetSpliterator class
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
class AssetSpliteratorTest
{

    private static int LEVEL1 = 10;
    private static int LEVEL2 = 15;
    private static int LEVEL3 = 5;



    private StorageAsset createTree() {
        return createTree( LEVEL1, LEVEL2, LEVEL3 );
    }

    private StorageAsset createTree(int... levelElements) {
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
    void tryAdvance( )
    {
        StorageAsset root = createTree( );
        AssetSpliterator spliterator = new AssetSpliterator( root );
        final ConsumeVisitStatus status = new ConsumeVisitStatus( );
        StorageAsset expectedTarget = root.list( ).get( 0 ).list( ).get( 0 ).list( ).get( 0 );
        spliterator.tryAdvance( status );
        assertEquals( 1, status.size( ) );
        assertEquals( expectedTarget, status.getLast( ) );

        spliterator.tryAdvance( status );
        assertEquals( 2, status.size( ) );
        expectedTarget = root.list( ).get( 0 ).list( ).get( 0 ).list( ).get( 1 );
        assertEquals( expectedTarget, status.getLast( ) );

    }

    @Test
    void forEachRemaining( )
    {
        StorageAsset root = createTree( );
        AssetSpliterator spliterator = new AssetSpliterator( root );
        final ConsumeVisitStatus status = new ConsumeVisitStatus( );
        spliterator.forEachRemaining( status );
        // 10 * 15 * 5 + 10 * 15 + 10 + 1
        assertEquals( LEVEL1*LEVEL2*LEVEL3+LEVEL1*LEVEL2+LEVEL1+1
            , status.size( ) );
        assertEquals( root, status.getLast( ) );
    }

    @Test
    void forEachRemaining2( )
    {
        StorageAsset root = createTree( );
        AssetSpliterator spliterator = new AssetSpliterator( root );
        final ConsumeVisitStatus status = new ConsumeVisitStatus( );
        spliterator.tryAdvance( a -> {} );
        spliterator.tryAdvance( a -> {} );
        spliterator.tryAdvance( a -> {} );
        spliterator.tryAdvance( a -> {} );

        spliterator.forEachRemaining( status );
        int expected = LEVEL1 * LEVEL2 * LEVEL3 + LEVEL1 * LEVEL2 + LEVEL1 + 1;
        expected = expected - 4;
        assertEquals( expected
            , status.size( ) );
        assertEquals( root, status.getLast( ) );
    }

    @Test
    void forEachRemaining3( )
    {
        StorageAsset root = createTree( );
        StorageAsset testRoot = root.list( ).get( 1 );
        AssetSpliterator spliterator = new AssetSpliterator( testRoot );
        final ConsumeVisitStatus status = new ConsumeVisitStatus( );
        spliterator.forEachRemaining( status );
        int expected = LEVEL2 * LEVEL3 + LEVEL2 + 1;
        assertEquals( expected
            , status.size( ) );
        assertEquals( testRoot, status.getLast( ) );
    }


    @Test
    void trySplit( )
    {
        StorageAsset root = createTree( );
        AssetSpliterator spliterator = new AssetSpliterator( root );
        final ConsumeVisitStatus status1 = new ConsumeVisitStatus( );
        final ConsumeVisitStatus status2 = new ConsumeVisitStatus( );
        Spliterator<StorageAsset> newSpliterator = spliterator.trySplit( );
        assertNotNull( newSpliterator );
        newSpliterator.forEachRemaining( status1 );
        spliterator.forEachRemaining( status2 );

        int sum = LEVEL1 * LEVEL2 * LEVEL3 + LEVEL1 * LEVEL2 + LEVEL1 + 1;
        int expected1 = sum / 2;
        int expected2 = sum / 2 + 1 ;
        assertEquals( expected1, status1.size( ) );
        assertEquals( expected2, status2.size( ) );

    }

    @Test
    void checkCharacteristics() {
        StorageAsset root = createTree( );
        AssetSpliterator spliterator = new AssetSpliterator( root );
        assertEquals( Spliterator.NONNULL, spliterator.characteristics( ) & Spliterator.NONNULL );
        assertEquals( Spliterator.CONCURRENT, spliterator.characteristics( ) & Spliterator.CONCURRENT );
        assertEquals( Spliterator.DISTINCT, spliterator.characteristics( ) & Spliterator.DISTINCT );


    }
}