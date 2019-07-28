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
import org.apache.archiva.repository.RepositoryType;

import java.net.URI;
import java.util.Collection;
import java.util.List;

public interface ArchivaIndexManager {

    String DEFAULT_INDEX_PATH=".indexer";
    String DEFAULT_PACKED_INDEX_PATH=".index";

    /**
     * Compresses the index to a more dense packed format.
     * @param context
     */
    void pack(ArchivaIndexingContext context) throws IndexUpdateFailedException;

    /**
     * Rescans the whole repository, this index is associated to.
     * @param context
     *
     */
    void scan(ArchivaIndexingContext context) throws IndexUpdateFailedException;

    /**
     * Updates the index from the remote url.
     * @param context
     * @param fullUpdate
     */
    void update(ArchivaIndexingContext context, boolean fullUpdate) throws IndexUpdateFailedException;

    /**
     * Adds a list of artifacts to the index.
     * @param context
     * @param artifactReference
     */
    void addArtifactsToIndex(ArchivaIndexingContext context, Collection<URI> artifactReference) throws IndexUpdateFailedException;

    /**
     * Removes a list of artifacts from the index.
     * @param context
     * @param artifactReference
     */
    void removeArtifactsFromIndex(ArchivaIndexingContext context, Collection<URI> artifactReference) throws IndexUpdateFailedException;


    /**
     * Returns true, if this manager is able to apply the index actions for the given repository type.
     * @param type
     * @return
     */
    boolean supportsRepository(RepositoryType type);

    /**
     * Creates the indexing context for the given repository.
     * @param repository the repository for which the index context should be created
     * @return the index context
     */
    ArchivaIndexingContext createContext(Repository repository) throws IndexCreationFailedException;

    /**
     * Reinitializes the index. E.g. remove the files and create a new empty index.
     *
     * @param context
     * @return the new created index
     */
    ArchivaIndexingContext reset(ArchivaIndexingContext context) throws IndexUpdateFailedException;

    /**
     * Moves the context to a new directory. It's up to the implementation, if a new context is created
     * or the context is moved only.
     *
     * @param context The current context
     * @param repo The repository
     * @return The new context
     * @throws IndexCreationFailedException
     */
    ArchivaIndexingContext move(ArchivaIndexingContext context, Repository repo) throws IndexCreationFailedException;

    /**
     * Updates the local path where the index is stored using the repository information.
     * @return
     */
    void updateLocalIndexPath(Repository repo);


    /**
     * Merges a list of contexts into a single one.
     *
     * @param destinationRepo The destination repository
     * @param contexts The contexts of the indexes that should be merged.
     * @param packIndex True, if the merged index should be packed, otherwise false.
     * @return The merged context
     * @throws UnsupportedOperationException if the underlying implementation does not allow to merge indexing contexts
     */
    ArchivaIndexingContext mergeContexts(Repository destinationRepo, List<ArchivaIndexingContext> contexts,
                                         boolean packIndex) throws UnsupportedOperationException,
            IndexCreationFailedException;
}
