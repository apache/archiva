package org.apache.archiva.repository.api;

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

import java.io.InputStream;
import java.io.OutputStream;

/**
 * RepositoryContext abstracts away the HTTP request/response
 * for use retreiving or storing data in a repository
 */
public interface RepositoryContext extends ResourceContext
{
    /**
     * Gets the ID for a Repostory
     * @return repositoryId
     */
    String getRepositoryId();

    /**
     * Gets the logical path of the request
     * @return logicalPath
     */
    String getLogicalPath();

    /**
     * Gets the principal for the current context
     * @return
     */
    String getPrincipal();

    /**
     * Gets the type of Request the RepositoryContext Represents
     * @return
     */
    RequestType getRequestType();

    /**
     * Gets the InputStream for the request body
     * @return inputStream
     */
    InputStream getInputStream();

    /**
     * Gets the OutputStream for the request response
     * @return outputStream
     */
    OutputStream getOutputStream();

    /**
     * Gets the ContentLength of the Response
     * @return contentLength
     */
    int getContentLength();

    /**
     * Gets the ContentType of the Response
     * @return
     */
    String getContentType();
}
