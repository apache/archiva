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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 * Utility class for traversing the asset tree recursively and stream based access to the assets.
 *
 * @since 3.0
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class StorageUtil
{
    /**
     * Walk the tree starting at the given asset. The consumer is called for each asset found.
     * It runs a depth-first search where children are consumed before their parents.
     *
     * @param start the starting asset
     * @param consumer the consumer that is applied to each asset
     */
    public static void walk( StorageAsset start, Consumer<StorageAsset> consumer ) {
        try(Stream<StorageAsset> assetStream = newAssetStream( start, false )) {
            assetStream.forEach( consumer::accept );
        }
    }

    /**
     * Walk the tree starting at the given asset. The consumer function is called for each asset found
     * as long as it returns <code>true</code> as result. If the function returns <code>false</code> the
     * processing stops.
     * It runs a depth-first search where children are consumed before their parents.
     *
     * @param start the starting asset
     * @param consumer the consumer function that is applied to each asset and that has to return <code>true</code>,
     *                 if the walk should continue.
     */
    public static void walk( StorageAsset start, Function<StorageAsset, Boolean> consumer ) {
        try(Stream<StorageAsset> assetStream = newAssetStream( start, false )) {
            assetStream.anyMatch( a -> !consumer.apply( a ) );
        }
    }


    /**
     * Returns a stream of assets starting at the given start node. The returned stream returns a closable
     * stream and should always be used in a try-with-resources statement.
     *
     * @param start the starting asset
     * @param parallel <code>true</code>, if a parallel stream should be created, otherwise <code>false</code>
     * @return the newly created stream
     */
    public static Stream<StorageAsset> newAssetStream( StorageAsset start, boolean parallel )
    {
        return StreamSupport.stream( new AssetSpliterator( start ), parallel );
    }


    /**
     * Returns a non-parallel stream.
     * Calls {@link #newAssetStream(StorageAsset, boolean)} with <code>parallel=false</code>.
     *
     * @param start the starting asset
     * @return the returned stream object
     */
    public static Stream<StorageAsset> newAssetStream( StorageAsset start) {
        return newAssetStream( start, false );
    }


}
