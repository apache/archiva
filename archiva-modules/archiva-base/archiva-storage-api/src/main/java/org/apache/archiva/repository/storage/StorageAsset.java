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
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * A instance of this interface represents information about a specific asset in a repository.
 * The asset may be an real artifact, a directory, or a virtual asset.
 *
 * Each asset has a unique path relative to the repository.
 *
 * The implementation may read the data directly from the filesystem or underlying storage implementation.
 *
 * @since 3.0
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface StorageAsset
{

    /**
     * Returns the storage this asset belongs to. Each asset belongs to exactly one storage instance.
     *
     * @return the storage instance
     */
    RepositoryStorage getStorage();

    /**
     * Returns the complete path relative to the base path to the given asset.
     *
     * @return A path starting with '/' that uniquely identifies the asset in the repository.
     */
    String getPath();

    /**
     * Returns the name of the asset. It may be just the filename.
     * @return the asset name
     */
    String getName();

    /**
     * Returns the time of the last modification.
     *
     * @return the time instant of the last modification
     */
    Instant getModificationTime();

    /**
     * Returns true, if this asset is a container type and contains further child assets.
     * @return <code>true</code>, if this is a container type, otherwise <code>false</code>
     */
    boolean isContainer();

    /**
     * Returns true, if this asset is a leaf node and cannot contain further childs
     * @return <code>true</code>, if this is a leaf type, otherwise <code>false</code>
     */
    boolean isLeaf();

    /**
     * List the child assets. Implementations should return a ordered list of children.
     *
     * @return The list of children. If there are no children and if the asset is not a container, a empty list will be returned.
     */
    List<? extends StorageAsset> list();

    /**
     * The size in bytes of the asset. If the asset does not have a size, -1 should be returned.
     *
     * @return The size if the asset has a size, otherwise -1
     */
    long getSize();

    /**
     * Returns the input stream of the artifact content.
     * This method will throw a IOException, if the stream could not be created.
     * Assets of type {@link AssetType#CONTAINER} will throw an IOException because they have no data attached.
     * Implementations should create a new stream instance for each invocation and make sure that the
     * stream is proper closed after usage.
     *
     * @return The InputStream representing the content of the artifact.
     * @throws IOException if the stream could not be created, either because of a problem accessing the storage, or because
     * the asset is not capable to provide a data stream
     */
    InputStream getReadStream() throws IOException;

    /**
     * Returns a NIO representation of the data.
     * This method will throw a IOException, if the stream could not be created.
     * Assets of type {@link AssetType#CONTAINER} will throw an IOException because they have no data attached.
     * Implementations should create a new channel instance for each invocation and make sure that the
     * channel is proper closed after usage.

     * @return A channel to the asset data.
     * @throws IOException if the channel could not be created, either because of a problem accessing the storage, or because
     * the asset is not capable to provide a data stream
     */
    ReadableByteChannel getReadChannel() throws IOException;

    /**
     *
     * Returns an output stream where you can write data to the asset. The operation is not locked or synchronized.
     * This method will throw a IOException, if the stream could not be created.
     * Assets of type {@link AssetType#CONTAINER} will throw an IOException because they have no data attached.
     * User of this method have to make sure, that the stream is proper closed after usage.
     *
     * @param replace If true, the original data will be replaced, otherwise the data will be appended.
     * @return The OutputStream where the data can be written.
     * @throws IOException if the stream could not be created, either because of a problem accessing the storage, or because
     * the asset is not capable to provide a data stream
     */
    OutputStream getWriteStream( boolean replace) throws IOException;

    /**
     * Returns a NIO representation of the asset where you can write the data.
     * This method will throw a IOException, if the stream could not be created.
     * Assets of type {@link AssetType#CONTAINER} will throw an IOException because they have no data attached.
     * Implementations should create a new channel instance for each invocation and make sure that the
     * channel is proper closed after usage.
     *
     * @param replace True, if the content should be replaced by the data written to the stream.
     * @return The Channel for writing the data.
     * @throws IOException if the channel could not be created, either because of a problem accessing the storage, or because
     * the asset is not capable to provide a data stream
     */
    WritableByteChannel getWriteChannel( boolean replace) throws IOException;

    /**
     * Replaces the content. The implementation may do an atomic move operation, or keep a backup. If
     * the operation fails, the implementation should try to restore the old data, if possible.
     *
     * The original file may be deleted, if the storage was successful.
     *
     * @param newData Replaces the data by the content of the given file.
     * @throws IOException if the access to the storage failed
     */
    boolean replaceDataFromFile( Path newData) throws IOException;

    /**
     * Returns true, if the asset exists.
     *
     * @return True, if the asset exists, otherwise false.
     */
    boolean exists();

    /**
     * Creates the asset in the underlying storage, if it does not exist.
     */
    void create() throws IOException;

    /**
     * Creates the asset as the given type
     * @param type the type to create, if the asset does not exist
     * @throws IOException if the asset could not be created
     */
    void create(AssetType type) throws IOException;

    /**
     * Returns the real path to the asset, if it exist. Not all implementations may implement this method.
     * The method throws {@link UnsupportedOperationException}, if and only if {@link #isFileBased()} returns false.
     *
     * @return The filesystem path to the asset.
     * @throws UnsupportedOperationException If the underlying storage is not file based.
     */
    Path getFilePath() throws UnsupportedOperationException;

    /**
     * Returns true, if the asset can return a file path for the given asset. If this is true, the  {@link #getFilePath()}
     * must not throw a {@link UnsupportedOperationException}
     *
     * @return
     */
    boolean isFileBased();

    /**
     * Returns true, if there is a parent to this asset.
     * @return True, if this asset is a descendant of a parent. False, if this is the root asset of the storage.
     */
    boolean hasParent();

    /**
     * Returns the parent of this asset. If this is the root asset of the underlying storage,
     * <code>null</code> will be returned.
     *
     * @return The asset, or <code>null</code>, if it does not exist.
     */
    StorageAsset getParent();

    /**
     * Returns the asset instance relative to the given path. The returned asset may not persisted yet.
     *
     * @param toPath the path relative to the current asset
     * @return the asset representing the given path
     */
    StorageAsset resolve(String toPath);

    /**
     * Returns the relative path from <code>this</code> asset to the given asset.
     * If the given asset is from a different storage implementation, than this asset, the
     * result is undefined.
     *
     * @param asset the asset that should be a descendant of <code>this</code>
     * @return the relative path
     */
    String relativize(StorageAsset asset );
}
