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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 *
 * Maps request paths to native repository paths. Normally HTTP requests and the path in the repository
 * storage should be identically.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface RequestPathMapper
{
    /**
     * Maps a request path to a repository path. The request path should be relative
     * to the repository. The resulting path should always start with a '/'.
     * The returned object contains additional information, if this request
     *
     * @param requestPath
     * @return
     */
    RelocatablePath relocatableRequestToRepository(String requestPath);


    String requestToRepository(String requestPath);


    /**
     * Maps a repository path to a request path. The repository path is relative to the
     * repository. The resulting path should always start with a '/'.
     *
     * @param repositoryPath
     * @return
     */
    String repositoryToRequest(String repositoryPath);

}
