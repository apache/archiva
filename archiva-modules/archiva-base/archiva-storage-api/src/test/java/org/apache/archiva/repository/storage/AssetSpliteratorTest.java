package org.apache.archiva.repository.storage;

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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.storage.mock.MockAsset;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the AssetSpliterator class
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
class AssetSpliteratorTest
{

    private StorageAsset createTree() {
        MockAsset root = new MockAsset( "" );
        for (int i=0; i<10; i++) {
            String name1 = "a" + String.format("%03d",i);
            MockAsset parent1 = new MockAsset( root, name1 );
            for (int k=0; k<15; k++) {
                String name2 = name1 + String.format("%03d", k);
                MockAsset parent2 = new MockAsset( parent1, name2 );
                for (int u=0; u<5; u++) {
                    String name3 = name2 + String.format("%03d", u);
                    MockAsset parent3 = new MockAsset( parent2, name3 );
                }
            }
        }
        return root;
    }

    private class Status {
        LinkedList<StorageAsset> visited = new LinkedList<>( );

        Status() {

        }

        public void add(StorageAsset asset) {
            visited.addLast( asset );
        }

        public StorageAsset getLast() {
            return visited.getLast( );
        }

        public List<StorageAsset> getVisited() {
            return visited;
        }

        public int size() {
            return visited.size( );
        }
    }

    @Test
    void tryAdvance( )
    {
        StorageAsset root = createTree( );
        AssetSpliterator spliterator = new AssetSpliterator( root );
        final StorageAsset expectedTarget = root.list( ).get( 0 ).list( ).get( 0 ).list( ).get( 0 );
        final Status status = new Status( );
        spliterator.tryAdvance( a -> status.add( a ) );
        assertEquals( expectedTarget, status.getLast( ) );
    }

    @Test
    void forEachRemaining( )
    {
    }

    @Test
    void trySplit( )
    {
    }
}