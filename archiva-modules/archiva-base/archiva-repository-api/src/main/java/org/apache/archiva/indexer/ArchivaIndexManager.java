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

import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.RepositoryType;

import java.net.URI;

public interface ArchivaIndexManager {

    /**
     * Compresses the index to a more dense packed format.
     * @param context
     */
    void pack(ArchivaIndexingContext context);

    /**
     * Rescans the whole repository, this index is associated to.
     * @param context
     * @param update
     */
    void scan(ArchivaIndexingContext context, boolean update);

    /**
     * Updates the index from the remote url.
     * @param context
     * @param remoteUpdateUri
     * @param fullUpdate
     */
    void update(ArchivaIndexingContext context, URI remoteUpdateUri, boolean fullUpdate);

    /**
     * Adds a artifact to the index.
     * @param context
     * @param artifactReference
     */
    void addArtifactToIndex(ArchivaIndexingContext context, ArtifactReference artifactReference);

    /**
     * Removes a artifact from the index.
     * @param context
     * @param artifactReference
     */
    void removeArtifactFromIndex(ArchivaIndexingContext context, ArtifactReference artifactReference);


    /**
     * Returns true, if this manager is able to apply the index actions for the given repository type.
     * @param type
     * @return
     */
    boolean supportsRepository(RepositoryType type);
}
