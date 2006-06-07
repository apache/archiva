package org.apache.maven.repository.proxy;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.repository.proxy.configuration.ProxyConfiguration;
import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.File;

/**
 * Class used to bridge the servlet to the repository proxy implementation.
 *
 * @author Edwin Punzalan
 * @todo the names get() and getRemoteFile() are confusing [!]
 */
public interface ProxyManager
{
    String ROLE = ProxyManager.class.getName();

    /**
     * Used to retrieve a cached path or retrieve one if the cache does not contain it yet.
     *
     * @param path the expected repository path
     * @return File object referencing the requested path in the cache
     * @throws ProxyException                when an exception occurred during the retrieval of the requested path
     * @throws ResourceDoesNotExistException when the requested object can't be found in any of the
     *                                       configured repositories
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
    File getRemoteFile( String path )
        throws ProxyException, ResourceDoesNotExistException;

    /**
     * Used by the factory to set the configuration of the proxy
     *
     * @param config the ProxyConfiguration to set the behavior of the proxy
     */
    void setConfiguration( ProxyConfiguration config );

    /**
     * Used to retrieve the configuration describing the behavior of the proxy
     *
     * @return the ProxyConfiguration of this proxy
     */
    ProxyConfiguration getConfiguration();
}
