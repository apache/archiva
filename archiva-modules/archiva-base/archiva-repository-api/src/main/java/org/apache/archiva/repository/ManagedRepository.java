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


import java.util.Set;

/**
 * Represents a managed repository, that is readable and writable.
 */
public interface ManagedRepository extends Repository {

    /**
     * Returns the interface to access the contents of this repository.
     *
     * @return The repository content.
     */
    ManagedRepositoryContent getContent();

    /**
     * Returns true, if repeated deployments of the same artifact with the same version throws exceptions.
     * @return
     */
    boolean blocksRedeployments();

    /**
     * Returns the release schemes that are active by this repository. E.g. for maven repositories
     * this may either be a release repository, a snapshot repository or a combined repository.
     * @return
     */
    Set<ReleaseScheme> getActiveReleaseSchemes();


    /**
     * Returns the request info object, which you can use for gathering information from the web request path.
     * @return Instance of a request info object that corresponds to this repository
     */
    RepositoryRequestInfo getRequestInfo();

}
