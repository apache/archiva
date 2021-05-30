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

import org.apache.archiva.repository.storage.AssetType;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.storage.mock.MockStorage;
import org.apache.archiva.repository.storage.util.StorageUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract test class for the storage utility to test for different storage implementations.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public abstract class AbstractStorageUtilTest
{
    private static final int LEVEL1 = 12;
    private static final int LEVEL2 = 13;
    private static final int LEVEL3 = 6;

    /**
     * A subclass must override this method. This method returns a new asset instance with the given parent.
     *
     * @param parent the parent asset for the newly created asset
     * @param name   the name of the new asset
     * @return the asset
     */
    protected abstract StorageAsset createAsset( StorageAsset parent, String name, AssetType type );

    protected StorageAsset createAsset(StorageAsset parent, String name) {
        return createAsset( parent, name, AssetType.FILE );
    }

    /**
     * A subclass must override this method. This method returns a new root asset instance without parent.
     * @return the newly created asset instance
     */
    protected abstract StorageAsset createRootAsset( );

    /**
     * Activates a exception on a certain asset in the storage
     * @param root the root asset
     */
    protected abstract void activateException( StorageAsset root );



    /**
     * A subclass should override this method. This method creates a new storage instance with the given root element.
     *
     * @param root the root asset
     * @return the storage instance
     */
    protected abstract RepositoryStorage createStorage( StorageAsset root );


    protected StorageAsset createTree( )
    {
        return createTree( LEVEL1, LEVEL2, LEVEL3 );
    }

    protected StorageAsset createTree( int... levelElements )
    {
        StorageAsset root = createRootAsset( );
        recurseSubTree( root, 0, levelElements );
        return root;
    }

    private void recurseSubTree( StorageAsset parent, int level, int[] levelElements )
    {
        if ( level < levelElements.length )
        {
            AssetType type = ( level == levelElements.length - 1 ) ? AssetType.FILE : AssetType.CONTAINER;
            for ( int k = 0; k < levelElements[level]; k++ )
            {
                String name = parent.getName( ) + String.format( "%03d", k );
                StorageAsset asset = createAsset( parent, name, type );
                recurseSubTree( asset, level + 1, levelElements );
            }
        }
    }

    @Test
    void testWalkFromRoot( )
    {
        StorageAsset root = createTree( );
        ConsumeVisitStatus status = new ConsumeVisitStatus( );

        StorageUtil.walk( root, status );
        int expected = LEVEL1 * LEVEL2 * LEVEL3 + LEVEL1 * LEVEL2 + LEVEL1 + 1;
        Assertions.assertEquals( expected, status.size( ) );
        StorageAsset first = root.list( ).get( 0 ).list( ).get( 0 ).list( ).get( 0 );
        Assertions.assertEquals( first, status.getFirst( ) );
        Assertions.assertEquals( root, status.getLast( ) );
    }

    @Test
    void testWalkFromChild( )
    {
        StorageAsset root = createTree( );
        ConsumeVisitStatus status = new ConsumeVisitStatus( );
        StorageAsset testRoot = root.list( ).get( 3 );

        StorageUtil.walk( testRoot, status );
        int expected = LEVEL2 * LEVEL3 + LEVEL2 + 1;
        Assertions.assertEquals( expected, status.size( ) );
        StorageAsset first = root.list( ).get( 3 ).list( ).get( 0 ).list( ).get( 0 );
        Assertions.assertEquals( first, status.getFirst( ) );
        Assertions.assertEquals( testRoot, status.getLast( ) );
    }

    @Test
    void testWalkFromRootWithCondition( )
    {
        StorageAsset root = createTree( );
        StopVisitStatus status = new StopVisitStatus( );
        status.setStopCondition( a -> a.getName( ).equals( "001002003" ) );

        StorageUtil.walk( root, status );
        Assertions.assertEquals( "001002003", status.getLast( ).getName( ) );
        int expected = LEVEL2 * LEVEL3 + LEVEL2 + 2 * LEVEL3 + 1 + 1 + 1 + 4;
        Assertions.assertEquals( expected, status.size( ) );
    }

    @Test
    void testStream( )
    {
        StorageAsset root = createTree( );
        List<StorageAsset> result;
        try ( Stream<StorageAsset> stream = StorageUtil.newAssetStream( root, false ) )
        {
            result = stream.filter( a -> a.getName( ).startsWith( "001" ) ).collect( Collectors.toList( ) );
        }
        int expected = LEVEL2 * LEVEL3 + LEVEL2 + 1;
        Assertions.assertEquals( expected, result.size( ) );
        Assertions.assertEquals( "001", result.get( result.size( ) - 1 ).getName( ) );
        Assertions.assertEquals( "001012", result.get( result.size( ) - 2 ).getName( ) );
    }

    @Test
    void testStreamParallel( )
    {
        StorageAsset root = createTree( );
        List<StorageAsset> result;
        try ( Stream<StorageAsset> stream = StorageUtil.newAssetStream( root, true ) )
        {
            result = stream.filter( a -> a.getName( ).startsWith( "001" ) ).collect( Collectors.toList( ) );
        }
        int expected = LEVEL2 * LEVEL3 + LEVEL2 + 1;
        Assertions.assertEquals( expected, result.size( ) );
    }

    @Test
    void testDelete( )
    {
        StorageAsset root = createTree( );
        RepositoryStorage storage = createStorage( root );

        StorageUtil.deleteRecursively( root );
        int expected = LEVEL1 * LEVEL2 * LEVEL3 + LEVEL1 * LEVEL2 + LEVEL1 + 1;
        testDeletionStatus( expected, storage );

    }

    protected abstract void testDeletionStatus( int expected, RepositoryStorage storage );

    @Test
    void testDeleteWithException( )
    {
        StorageAsset root = createTree( );
        RepositoryStorage storage = createStorage( root );
        activateException( root );

        StorageUtil.deleteRecursively( root );
        int expected = LEVEL1 * LEVEL2 * LEVEL3 + LEVEL1 * LEVEL2 + LEVEL1 + 1;
        testDeletionStatus( expected, storage );
    }

    @Test
    void testDeleteWithExceptionFailFast( )
    {
        StorageAsset root = createTree( );
        RepositoryStorage storage = createStorage( root );
        activateException( root );

        StorageUtil.deleteRecursively( root, true );
        int expected = 113;
        testDeletionStatus( expected, storage );
    }

    @Test
    void testCopyRecursive( ) throws IOException
    {
        StorageAsset root = createTree( );
        createStorage( root );
        StorageAsset destinationRoot = createRootAsset( );
        RepositoryStorage destinationStorage = createStorage( destinationRoot );
        StorageAsset destination = destinationStorage.getAsset( "" );
        boolean result = StorageUtil.copyRecursively( root, destination, false );
        Assertions.assertTrue( result );
        Assertions.assertTrue( destination.exists( ) );
        Assertions.assertTrue( destination.resolve( "000/000000/000000000" ).exists( ) );
        Assertions.assertTrue( destination.resolve( "011/011000/011000000" ).exists( ) );
        Assertions.assertTrue( destination.resolve( "010/010000/010000000" ).exists( ) );
        Assertions.assertTrue( destination.resolve( "000/000000/000000000" ).exists( ) );

    }
}
