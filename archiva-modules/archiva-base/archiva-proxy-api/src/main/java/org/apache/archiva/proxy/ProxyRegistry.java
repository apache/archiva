package org.apache.archiva.proxy;

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

import org.apache.archiva.proxy.model.NetworkProxy;
import org.apache.archiva.proxy.model.ProxyConnector;
import org.apache.archiva.proxy.model.RepositoryProxyHandler;
import org.apache.archiva.repository.RepositoryType;

import java.util.List;
import java.util.Map;

/**
 * A proxy registry is central access point for accessing a proxy. It gives access to the proxy handlers
 * that are registered for the different repository types.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface ProxyRegistry {

    /**
     * Returns the network proxy that is configured for the given id (repository id).
     *
     * @param id The proxy id
     * @return The network proxy object if defined, otherwise null.
     */
    NetworkProxy getNetworkProxy(String id);

    /**
     * Returns a map that contains a list of repository handlers for each repository type.
     * @return The map with the repository type as key and a list of handler objects as value.
     */
    Map<RepositoryType, List<RepositoryProxyHandler>> getAllHandler();

    /**
     * Returns the repository handler that are defined for the given repository type.
     *
     * @param type The repository type
     * @return Returns the list of the handler objects, or a empty list, if none defined.
     */
    List<RepositoryProxyHandler> getHandler(RepositoryType type);

    /**
     * Returns true, if there are proxy handler registered for the given type.
     *
     * @param type The repository type
     * @return True, if a handler is registered, otherwise false.
     */
    boolean hasHandler(RepositoryType type);

    /**
     * Returns the list of all proxy connectors.
     * @return
     */
    List<org.apache.archiva.proxy.model.ProxyConnector> getProxyConnectors( );

    /**
     * Returns a map of connector lists with the source repository id as key
     * @return A map with source repository ids as key and list of corresponding proxy connector objects as value.
     */
    Map<String, List<ProxyConnector>> getProxyConnectorAsMap( );

    /**
     * Reloads the proxies from the configuration.
     */
    void reload();
}
