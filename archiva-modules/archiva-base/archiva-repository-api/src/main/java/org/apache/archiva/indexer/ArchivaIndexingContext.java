package org.apache.archiva.indexer;

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

import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.storage.StorageAsset;

import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * This represents a indexing context that is used to manage the index of a certain repository.
 *
 */
public interface ArchivaIndexingContext {

    /**
     * The identifier of the context
     * @return
     */
    String getId();

    /**
     * Returns the repository this index context is associated to.
     * @return
     */
    Repository getRepository();

    /**
     * The path where the index is stored.
     * @return
     */
    StorageAsset getPath();

    /**
     * Returns true, if the index has no entries or is not initialized.
     * @return
     */
    boolean isEmpty() throws IOException;

    /**
     * Writes the last changes to the index.
     * @throws IOException
     */
    void commit() throws IOException;

    /**
     * Throws away the last changes.
     * @throws IOException
     */
    void rollback() throws IOException;

    /**
     * Optimizes the index
     * @throws IOException
     */
    void optimize() throws IOException;

    /**
     * Closes any resources, this context has open.
     * @param deleteFiles True, if the index files should be deleted.
     * @throws IOException
     */
    void close(boolean deleteFiles) throws IOException;

    /**
     * Closes the context without deleting the files.
     * Is identical to <code>close(false)</code>
     * @throws IOException
     */
    void close() throws IOException;

    /**
     * Returns the status of this context. This method will return <code>false</code>, after the {@link #close()} method
     * has been called.
     *
     * @return <code>true</code>, if the <code>close()</code> method has not been called, otherwise <code>false</code>
     */
    boolean isOpen();

    /**
     * Removes all entries from the index. After this method finished,
     * isEmpty() should return true.
     * @throws IOException
     */
    void purge() throws IOException;

    /**
     * Returns true, if this index implementation has support for the given repository specific
     * implementation class.
     * @param clazz
     * @return
     */
    boolean supports(Class<?> clazz);

    /**
     * Returns the repository specific implementation of the index. E.g. the maven index class.
     * @param clazz the specific class
     * @return the instance of the given class representing this index
     * @throws UnsupportedOperationException if the implementation is not supported
     */
    <T> T getBaseContext(Class<T> clazz) throws UnsupportedBaseContextException;



    /**
     * Returns the list of groups that are assigned to this index
     * @return
     */
    Set<String> getGroups() throws IOException;

    /**
     * Updates the timestamp of the index.
     * @param save
     * @throws IOException
     */
    void updateTimestamp(boolean save) throws IOException;

    /**
     * Updates the timestamp with the given time.
     * @param save
     * @param time
     * @throws IOException
     */
    void updateTimestamp(boolean save, ZonedDateTime time) throws IOException;
}
