package org.apache.maven.repository.proxy;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.proxy.ProxyInfo;

import java.io.File;
import java.util.List;

/**
 * An individual request handler for the proxy.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface ProxyRequestHandler
{
    /**
     * The Plexus role of the component.
     */
    String ROLE = ProxyRequestHandler.class.getName();

    /**
     * Used to retrieve an artifact at a particular path, giving the cached version if it exists.
     *
     * @param path                the expected repository path
     * @param proxiedRepositories the repositories being proxied to
     * @param managedRepository   the locally managed repository to cache artifacts in
     * @return File object referencing the requested path in the cache
     * @throws ProxyException when an exception occurred during the retrieval of the requested path
     * @throws org.apache.maven.wagon.ResourceDoesNotExistException
     *                        when the requested object can't be found in any of the
     *                        configured repositories
     */
    File get( String path, List proxiedRepositories, ArtifactRepository managedRepository )
        throws ProxyException, ResourceDoesNotExistException;

    /**
     * Used to retrieve an artifact at a particular path, giving the cached version if it exists.
     *
     * @param path                the expected repository path
     * @param proxiedRepositories the repositories being proxied to
     * @param managedRepository   the locally managed repository to cache artifacts in
     * @param wagonProxy          a network proxy to use when transferring files if needed
     * @return File object referencing the requested path in the cache
     * @throws ProxyException when an exception occurred during the retrieval of the requested path
     * @throws org.apache.maven.wagon.ResourceDoesNotExistException
     *                        when the requested object can't be found in any of the
     *                        configured repositories
     */
    File get( String path, List proxiedRepositories, ArtifactRepository managedRepository, ProxyInfo wagonProxy )
        throws ProxyException, ResourceDoesNotExistException;

    /**
     * Used to force remote download of the requested path from any the configured repositories.  This method will
     * only bypass the cache for searching but the requested path will still be cached.
     *
     * @param path                the expected repository path
     * @param proxiedRepositories the repositories being proxied to
     * @param managedRepository   the locally managed repository to cache artifacts in
     * @return File object referencing the requested path in the cache
     * @throws ProxyException when an exception occurred during the retrieval of the requested path
     * @throws org.apache.maven.wagon.ResourceDoesNotExistException
     *                        when the requested object can't be found in any of the
     *                        configured repositories
     */
    File getAlways( String path, List proxiedRepositories, ArtifactRepository managedRepository )
        throws ProxyException, ResourceDoesNotExistException;

    /**
     * Used to force remote download of the requested path from any the configured repositories.  This method will
     * only bypass the cache for searching but the requested path will still be cached.
     *
     * @param path                the expected repository path
     * @param proxiedRepositories the repositories being proxied to
     * @param managedRepository   the locally managed repository to cache artifacts in
     * @param wagonProxy          a network proxy to use when transferring files if needed
     * @return File object referencing the requested path in the cache
     * @throws ProxyException when an exception occurred during the retrieval of the requested path
     * @throws org.apache.maven.wagon.ResourceDoesNotExistException
     *                        when the requested object can't be found in any of the
     *                        configured repositories
     */
    File getAlways( String path, List proxiedRepositories, ArtifactRepository managedRepository, ProxyInfo wagonProxy )
        throws ProxyException, ResourceDoesNotExistException;
}
