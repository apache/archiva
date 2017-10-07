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
 * The editable part of a remote repository
 */
public interface EditableRemoteRepository extends EditableRepository, RemoteRepository
{

    /**
     * Sets the login credentials for login to the remote repository.
     * @param credentials
     */
    void setCredentials(RepositoryCredentials credentials);

    /**
     * Sets the path relative to the root url of the repository that should be used to check
     * the availability of the repository.
     *
     * @param path The path string.
     */
    void setCheckPath(String path);

    /**
     * Sets additional parameters to be used to access the remote repository.
     * @param params A map of parameters, may not be null.
     */
    void setExtraParameters(Map<String,String> params);

    /**
     * Adds an additional parameter.
     * @param key The key of the parameter
     * @param value The value of the parameter
     */
    void addExtraParameter(String key, String value);

    /**
     * Sets the extra headers, that are added to the requests to the remote repository.
     */
    void setExtraHeaders(Map<String,String> headers);

    /**
     * Adds an extra header.
     * @param header The header name
     * @param value The header value
     */
    void addExtraHeader(String header, String value);

    /**
     * Sets the timeout for requests to the remote repository.
     *
     * @param duration The amount of time, after that the request is aborted.
     */
    void setTimeout(Duration duration);

    /**
     * Sets the content.
     * @param content
     */
    void setContent(RemoteRepositoryContent content);
}
