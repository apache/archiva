package org.apache.archiva.repository;

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

import org.apache.archiva.repository.storage.RepositoryStorage;

import java.util.List;

/**
 * Interface for repository groups.
 *
 * Repository groups are a combined view over a list of repositories.
 * All repositories of this group must be of the same type.
 *
 * Repository groups are read only. You cannot store artifacts into a repository group.
 *
 * This interface extends <code>{@link RepositoryStorage}</code> to provide access to the merged
 * index data files and other metadata.
 *
 */
public interface RepositoryGroup extends Repository, RepositoryStorage {

    /**
     * Returns the list of repositories. The order of the elements represents
     * the order of getting artifacts (first one wins).
     *
     *
     * @return
     */
    List<ManagedRepository> getRepositories();

    /**
     * Returns true, if the given repository is part of this group.
     *
     * @param repository The repository to check.
     * @return True, if it is part, otherwise false.
     */
    boolean contains(ManagedRepository repository);

    /**
     * Returns true, if the repository with the given id is part of this group.
     *
     * @param id The repository id to check
     * @return True, if it is part, otherwise false
     */
    boolean contains(String id);

    /**
     * Returns the time to live in seconds for the merged index.
     *
     * @return
     */
    int getMergedIndexTTL();
}
