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
     * Returns the storage this asset belongs to.
     * @return
     */
    RepositoryStorage getStorage();

    /**
     * Returns the complete path relative to the repository to the given asset.
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
     * List the child assets.
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
     * It will throw a IOException, if the stream could not be created.
     * Implementations should create a new stream instance for each invocation and make sure that the
     * stream is proper closed after usage.
     *
     * @return The InputStream representing the content of the artifact.
     * @throws IOException
     */
    InputStream getReadStream() throws IOException;

    /**
     * Returns a NIO representation of the data.
     *
     * @return A channel to the asset data.
     * @throws IOException
     */
    ReadableByteChannel getReadChannel() throws IOException;

    /**
     *
     * Returns an output stream where you can write data to the asset. The operation is not locked or synchronized.
     * User of this method have to make sure, that the stream is proper closed after usage.
     *
     * @param replace If true, the original data will be replaced, otherwise the data will be appended.
     * @return The OutputStream where the data can be written.
     * @throws IOException
     */
    OutputStream getWriteStream( boolean replace) throws IOException;

    /**
     * Returns a NIO representation of the asset where you can write the data.
     *
     * @param replace True, if the content should be replaced by the data written to the stream.
     * @return The Channel for writing the data.
     * @throws IOException
     */
    WritableByteChannel getWriteChannel( boolean replace) throws IOException;

    /**
     * Replaces the content. The implementation may do an atomic move operation, or keep a backup. If
     * the operation fails, the implementation should try to restore the old data, if possible.
     *
     * The original file may be deleted, if the storage was successful.
     *
     * @param newData Replaces the data by the content of the given file.
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
     * Returns the real path to the asset, if it exist. Not all implementations may implement this method.
     * The method throws {@link UnsupportedOperationException}, if and only if {@link #isFileBased()} returns false.
     *
     * @return The filesystem path to the asset.
     * @throws UnsupportedOperationException If the underlying storage is not file based.
     */
    Path getFilePath() throws UnsupportedOperationException;

    /**
     * Returns true, if the asset can return a file path for the given asset. If this is true, the  {@link #getFilePath()}
     * will not throw a {@link UnsupportedOperationException}
     *
     * @return
     */
    boolean isFileBased();

    /**
     * Returns true, if there is a parent to this asset.
     * @return
     */
    boolean hasParent();

    /**
     * Returns the parent of this asset.
     * @return The asset, or <code>null</code>, if it does not exist.
     */
    StorageAsset getParent();

    /**
     * Returns the asset relative to the given path
     * @param toPath
     * @return
     */
    StorageAsset resolve(String toPath);
}
