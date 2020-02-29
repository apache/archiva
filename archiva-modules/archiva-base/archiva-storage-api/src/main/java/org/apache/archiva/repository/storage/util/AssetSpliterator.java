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

import java.io.Closeable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * Base Spliterator implementation for Storage Assets. The spliterator visits the tree by depth-first.
 * For the non-concurrent usage it is guaranteed that children are visited before their
 * parents. If the spliterator is used in a parallel stream, there is no guarantee for
 * the order of returned assets.
 *
 * The estimated size is not accurate, because the tree paths are scanned on demand (lazy loaded)
 *
 * The spliterator returns the status of the assets at the time of retrieval. If modifications occur
 * during traversal the returned assets may not represent the latest state.
 * There is no check for modifications during traversal and no <code>{@link java.util.ConcurrentModificationException}</code> are thrown.
 *
 *
 *
 * @since 3.0
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class AssetSpliterator implements Spliterator<StorageAsset>, Closeable
{
    public static final int DEFAULT_SPLIT_THRESHOLD = 2;

    // the linked list is used as stack
    private LinkedList<StorageAsset> workList = new LinkedList<>( );
    private LinkedHashSet<StorageAsset> visitedContainers = new LinkedHashSet<>( );
    private long visited = 0;
    private final int splitThreshold;
    private static final int CHARACTERISTICS =  Spliterator.DISTINCT|Spliterator.NONNULL|Spliterator.CONCURRENT;


    public AssetSpliterator( int splitThreshold, StorageAsset... assets) {
        this.splitThreshold = splitThreshold;
        init( assets );
    }

    private void init( StorageAsset[] assets )
    {
        if (assets.length==0 || assets[0] == null) {
            throw new IllegalArgumentException( "There must be at least one non-null asset" );
        }
        Collections.addAll( this.workList, assets );
        retrieveNextPath( this.workList.get( 0 ) );
    }

    public AssetSpliterator( StorageAsset... assets) {
        this.splitThreshold = DEFAULT_SPLIT_THRESHOLD;
        init( assets );
    }

    protected AssetSpliterator() {
        this.splitThreshold = DEFAULT_SPLIT_THRESHOLD;
    }

    protected AssetSpliterator( int splitThreshold) {
        this.splitThreshold = splitThreshold;
    }


    protected AssetSpliterator( int splitThreshold, Set<StorageAsset> visitedContainers) {
        this.visitedContainers.addAll( visitedContainers );
        this.splitThreshold = splitThreshold;
    }

    protected AssetSpliterator( List<StorageAsset> baseList, Set<StorageAsset> visitedContainers) {
        this.workList.addAll(baseList);
        retrieveNextPath( this.workList.get( 0 ) );
        this.visitedContainers.addAll( visitedContainers );
        this.splitThreshold = DEFAULT_SPLIT_THRESHOLD;
    }

    private void add( StorageAsset asset) {
        workList.addLast( asset );
    }


    @Override
    public void close( )
    {
        this.workList.clear();
        this.workList=null;
        this.visitedContainers.clear();
        this.visitedContainers=null;
    }

    @Override
    public boolean tryAdvance( Consumer<? super StorageAsset> action )
    {
        try
        {
            StorageAsset asset = workList.getLast( );
            consumeAsset( action, asset );
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private void consumeAsset( Consumer<? super StorageAsset> action, StorageAsset asset )
    {
        // Traverse the path to the deepest descent (depth-first)
        while(retrieveNextPath( asset )) {
            asset = workList.getLast( );
        }
        action.accept( workList.removeLast() );
        visited++;
    }

    private boolean retrieveNextPath(StorageAsset parent) {
        if (parent.isContainer() && !visitedContainers.contains( parent )) {
            // Containers after files in stack guarantee the depth-first behaviour
            workList.addAll( getChildFiles( parent ) );
            workList.addAll( getChildContainers( parent ) );
            visitedContainers.add( parent );
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void forEachRemaining( Consumer<? super StorageAsset> action )
    {
        try
        {
            //noinspection InfiniteLoopStatement
            while ( true )
            {
                consumeAsset( action, workList.getLast( ) );
            }
        } catch (NoSuchElementException e) {
            // Should happen at the end.
        }
    }

    // Assets are returned in reverse order
    List<? extends StorageAsset> getChildContainers( StorageAsset parent) {
        final List<? extends StorageAsset> children = parent.list( );
        final int len = children.size( );
        return IntStream.range( 0, children.size( ) ).mapToObj( i ->
            children.get(len - i - 1)).filter( StorageAsset::isContainer ).collect( Collectors.toList( ) );
    }

    // Assets are returned in reverse order
    List<? extends StorageAsset> getChildFiles(StorageAsset parent) {
        final List<? extends StorageAsset> children = parent.list( );
        final int len = children.size( );
        return IntStream.range( 0, children.size( ) ).mapToObj( i ->
            children.get(len - i - 1)).filter( StorageAsset::isLeaf ).collect( Collectors.toList( ) );
    }


    /**
     * Splits by moving every second asset to the new spliterator. This allows to start both at similar
     * tree depths. But it is not guaranteed that they start on the same depth.
     * The split happens only, if the number of elements in the worklist is greater than 2.
     *
     * @return the new spliterator if the work list size is greater than 2
     */
    @Override
    public Spliterator<StorageAsset> trySplit( )
    {
        if (workList.size()>splitThreshold) {
            // We use the elements alternately for the current and the new spliterator
            // For the parallel scenario we cannot guarantee that children are visited
            // before their parents
            final LinkedList<StorageAsset> newWorkList = new LinkedList<>( );
            final AssetSpliterator newSpliterator = new AssetSpliterator( this.splitThreshold, visitedContainers );
            try {
                //noinspection InfiniteLoopStatement
                while (true)
                {
                    newWorkList.add( workList.removeFirst( ) );
                    newSpliterator.add( workList.removeFirst( ) );
                }
            } catch (NoSuchElementException e) {
                //
            }
            // Swap the worklist
            this.workList = newWorkList;
            return newSpliterator;
        } else {
            return null;
        }
    }

    @Override
    public long estimateSize( )
    {
        return workList.size()+visited;
    }

    @Override
    public int characteristics( )
    {
        return CHARACTERISTICS;
    }


}
