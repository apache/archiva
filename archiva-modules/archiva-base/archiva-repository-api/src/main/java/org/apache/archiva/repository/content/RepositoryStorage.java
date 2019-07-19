package org.apache.archiva.repository.content;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.CopyOption;
import java.util.function.Consumer;

/**
 * Repository storage gives access to the files and directories on the storage.
 * The storage may be on a filesystem but can be any other storage system.
 *
 * This API is low level repository access. If you use this API you must
 * either have knowledge about the specific repository layout or use the structure
 * as it is, e.g. for browsing.
 *
 * It is the decision of the implementation, if this API provides access to all elements, or
 * just a selected view.
 *
 * Checking access is not part of this API.
 */
public interface RepositoryStorage {
    /**
     * Returns information about a specific storage asset.
     * @param path
     * @return
     */
    StorageAsset getAsset(String path);

    /**
     * Consumes the data and sets a lock for the file during the operation.
     *
     * @param asset The asset from which the data is consumed.
     * @param consumerFunction The consumer that reads the data
     * @param readLock If true, a read lock is acquired on the asset.
     * @throws IOException
     */
    void consumeData(StorageAsset asset, Consumer<InputStream> consumerFunction, boolean readLock) throws IOException;

    /**
     * Consumes the data and sets a lock for the file during the operation.
     *
     * @param asset The asset from which the data is consumed.
     * @param consumerFunction The consumer that reads the data
     * @param readLock If true, a read lock is acquired on the asset.
     * @throws IOException
     */
    void consumeDataFromChannel( StorageAsset asset, Consumer<ReadableByteChannel> consumerFunction, boolean readLock) throws IOException;

    /**
     * Writes data to the asset using a write lock.
     *
     * @param asset The asset to which the data is written.
     * @param consumerFunction The function that provides the data.
     * @param writeLock If true, a write lock is acquired on the destination.
     */
    void writeData( StorageAsset asset, Consumer<OutputStream> consumerFunction, boolean writeLock) throws IOException;;

    /**
     * Writes data and sets a lock during the operation.
     *
     * @param asset The asset to which the data is written.
     * @param consumerFunction The function that provides the data.
     * @param writeLock If true, a write lock is acquired on the destination.
     * @throws IOException
     */
    void writeDataToChannel( StorageAsset asset, Consumer<WritableByteChannel> consumerFunction, boolean writeLock) throws IOException;

    /**
     * Adds a new asset to the underlying storage.
     * @param path The path to the asset.
     * @param container True, if the asset should be a container, false, if it is a file.
     * @return
     */
    StorageAsset addAsset(String path, boolean container);

    /**
     * Removes the given asset from the storage.
     *
     * @param asset
     * @throws IOException
     */
    void removeAsset(StorageAsset asset) throws IOException;

    /**
     * Moves the asset to the given location and returns the asset object for the destination.
     *
     * @param origin The original asset
     * @param destination The destination path pointing to the new asset.
     * @param copyOptions The copy options.
     * @return The asset representation of the moved object.
     */
    StorageAsset moveAsset(StorageAsset origin, String destination, CopyOption... copyOptions) throws IOException;

    /**
     * Moves the asset to the new path.
     *
     * @param origin The original asset
     * @param destination The destination asset.
     * @param copyOptions The copy options (e.g. {@link java.nio.file.StandardCopyOption#REPLACE_EXISTING}
     * @throws IOException If it was not possible to copy the asset.
     */
    void moveAsset(StorageAsset origin, StorageAsset destination, CopyOption... copyOptions) throws IOException;

    /**
     * Copies the given asset to the new destination.
     *
     * @param origin The original asset
     * @param destination The path to the new asset
     * @param copyOptions The copy options, e.g. (e.g. {@link java.nio.file.StandardCopyOption#REPLACE_EXISTING}
     * @return The asset representation of the copied object
     * @throws IOException If it was not possible to copy the asset
     */
    StorageAsset copyAsset(StorageAsset origin, String destination, CopyOption... copyOptions) throws IOException;

    /**
     * Copies the given asset to the new destination.
     *
     * @param origin The original asset
     * @param destination The path to the new asset
     * @param copyOptions The copy options, e.g. (e.g. {@link java.nio.file.StandardCopyOption#REPLACE_EXISTING}
     * @throws IOException If it was not possible to copy the asset
     */
    void copyAsset( StorageAsset origin, StorageAsset destination, CopyOption... copyOptions) throws IOException;


}
