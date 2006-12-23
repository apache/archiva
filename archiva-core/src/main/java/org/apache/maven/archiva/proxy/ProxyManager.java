package org.apache.maven.archiva.proxy;

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

import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.File;

/**
 * Repository proxying component. This component will take requests for a given path within a managed repository
 * and if it is not found or expired, will look in the specified proxy repositories.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ProxyManager
{
    /**
     * The Plexus role for the component.
     */
    String ROLE = ProxyManager.class.getName();

    /**
     * Used to retrieve a cached path or retrieve one if the cache does not contain it yet.
     *
     * @param path the expected repository path
     * @return File object referencing the requested path in the cache
     * @throws ProxyException when an exception occurred during the retrieval of the requested path
     * @throws org.apache.maven.wagon.ResourceDoesNotExistException
     *                        when the requested object can't be found in any of the
     *                        configured repositories
     */
    File get( String path )
        throws ProxyException, ResourceDoesNotExistException;

    /**
     * Used to force remote download of the requested path from any the configured repositories.  This method will
     * only bypass the cache for searching but the requested path will still be cached.
     *
     * @param path the expected repository path
     * @return File object referencing the requested path in the cache
     * @throws ProxyException                when an exception occurred during the retrieval of the requested path
     * @throws ResourceDoesNotExistException when the requested object can't be found in any of the
     *                                       configured repositories
     */
    File getAlways( String path )
        throws ProxyException, ResourceDoesNotExistException;
}
