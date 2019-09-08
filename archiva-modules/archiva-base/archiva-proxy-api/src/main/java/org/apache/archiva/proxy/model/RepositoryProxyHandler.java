package org.apache.archiva.proxy.model;

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
import org.apache.archiva.policies.Policy;
import org.apache.archiva.policies.ProxyDownloadException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.storage.StorageAsset;

import java.util.List;
import java.util.Map;

/**
 * A repository proxy handler is used to fetch remote artifacts from different remote repositories.
 * A proxy handler is connected to one managed repository and a list of remote repositories.
 *
 * Repository proxies should not be confused with network proxies. Network are proxies for specific network protocols,
 * like HTTP. A repository proxy delegates the repository requests to remote repositories and caches artifacts.
 *
 * If a artifact is requested for the managed repository and the artifact is not cached locally, the handler goes through
 * the list of remotes and tries to download the artifact. If a download was successful the artifact is cached locally.
 *
 * The connection between managed and remote repositories is defined by list of {@link ProxyConnector} each defines a one-to-one relationship.
 *
 * A proxy connector defines specifics about the download behaviour:
 * <ul>
 * <li>Policies {@link org.apache.archiva.policies.Policy} define the behaviour for different cases (errors, not available, caching lifetime).</li>
 * <li>Black- and Whitelists are used to ban or allow certain paths on the remote repositories.
 * </ul>
 *
 * The policies and black- and whitelist are set on the {@link ProxyConnector}
 *
 * There may be network proxies needed to connect the remote repositories.
 *
 */
public interface RepositoryProxyHandler
{

    List<RepositoryType> supports( );

    /**
     * Performs the artifact fetch operation against the target repositories
     * of the provided source repository.
     * <p>
     * If the artifact is found, it is downloaded and placed into the source repository
     * filesystem.
     *
     * @param repository the source repository to use. (must be a managed repository)
     * @param artifact   the artifact to fetch.
     * @return the file that was obtained, or null if no content was obtained
     * @throws ProxyDownloadException if there was a problem fetching the content from the target repositories.
     */
    StorageAsset fetchFromProxies( ManagedRepository repository, ArtifactReference artifact )
        throws ProxyDownloadException;

    /**
     * Performs the metadata fetch operation against the target repositories
     * of the provided source repository.
     * <p>
     * If the metadata is found, it is downloaded and placed into the source repository
     * filesystem.
     *
     * @param repository  the source repository to use. (must be a managed repository)
     * @param logicalPath the metadata to fetch.
     * @return the file that was obtained, or null if no content was obtained
     */
    ProxyFetchResult fetchMetadataFromProxies( ManagedRepository repository, String logicalPath );

    /**
     * Performs the fetch operation against the target repositories
     * of the provided source repository by a specific path.
     *
     * @param managedRepository the source repository to use. (must be a managed repository)
     * @param path              the path of the resource to fetch
     * @return the file that was obtained, or null if no content was obtained
     */
    StorageAsset fetchFromProxies( ManagedRepository managedRepository, String path );

    /**
     * Get the List of {@link ProxyConnector} objects of the source repository.
     *
     * @param repository the source repository to look for.
     * @return the List of {@link ProxyConnector} objects.
     */
    List<ProxyConnector> getProxyConnectors( ManagedRepository repository );

    /**
     * Tests to see if the provided repository is a source repository for
     * any {@link ProxyConnector} objects.
     *
     * @param repository the source repository to look for.
     * @return true if there are proxy connectors that use the provided
     * repository as a source repository.
     */
    boolean hasProxies( ManagedRepository repository );

    /**
     * Sets network proxies (normally HTTP proxies) to access the remote repositories.
     *
     * @param networkProxies A map of (repository id, network proxy) where the repository id must be the id of an
     *                existing remote repository.
     */
    void setNetworkProxies( Map<String, NetworkProxy> networkProxies );

    /**
     * Adds a network proxy that is used to access the remote repository.
     *
     * @param id The repository id
     * @param networkProxy The network proxy to use
     */
    void addNetworkproxy( String id, NetworkProxy networkProxy);

    /**
     * Returns a map of the defined network proxies, or a empty map, if no proxy is defined.
     *
     * @return A map (repository id, network proxy). If none is defined, a empty map is returned.
     */
    Map<String, NetworkProxy> getNetworkProxies( );

    /**
     * Returns the network proxy that is defined for the given repository id.
     * @param id The remote repository id
     * @return A network proxy or <code>null</code> if no one is defined for this id.
     */
    NetworkProxy getNetworkProxy( String id );

    /**
     * Returns the proxy handler implementation. This can be used, if the underlying implementation for a specific
     * repository type is needed.
     *
     * @param clazz The class to convert to
     * @param <T>   The type
     * @return The handler
     */
    <T extends RepositoryProxyHandler> T getHandler( Class<T> clazz ) throws IllegalArgumentException;

    /**
     * Sets the policies that this handler should validate.
     * @param policyList
     */
    void setPolicies(List<Policy> policyList);

    /**
     * Adds a policy
     * @param policy
     */
    void addPolicy( Policy policy );

    /**
     * Removes a policy
     * @param policy
     */
    void removePolicy( Policy policy );

    void addProxyConnector(ProxyConnector connector);

    void setProxyConnectors( List<ProxyConnector> proxyConnectors );
}
