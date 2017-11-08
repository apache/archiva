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


import java.time.Duration;
import java.util.Map;

/**
 * This represents a repository that is not fully managed by archiva. Its some kind of proxy that
 * forwards requests to the remote repository and is able to cache artifacts locally.
 */
public interface RemoteRepository extends Repository {

    /**
     * Returns the interface to access the content of the repository.
     * @return
     */
    RemoteRepositoryContent getContent();

    /**
     * Returns the credentials used to login to the remote repository.
     * @return the credentials, null if not set.
     */
    RepositoryCredentials getLoginCredentials();

    /**
     * Returns the path relative to the root url of the repository that should be used
     * to check the availability of the repository.
     * @return The check path, null if not set.
     */
    String getCheckPath();


    /**
     * Returns additional parameters, that are used for accessing the remote repository.
     * @return A map of key, value pairs.
     */
    Map<String,String> getExtraParameters();


    /**
     * Returns extra headers that are added to the request to the remote repository.
     * @return
     */
    Map<String,String> getExtraHeaders();

    /**
     * Returns the time duration, after that the request is aborted and a error is returned, if the remote repository
     * does not respond.
     * @return The timeout.
     */
    Duration getTimeout();


}
