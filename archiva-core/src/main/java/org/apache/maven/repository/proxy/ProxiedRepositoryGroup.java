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
import org.apache.maven.wagon.proxy.ProxyInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A set of information to store for a group of proxies.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ProxiedRepositoryGroup
{

    /**
     * The locally managed repository that caches proxied artifacts.
     */
    private ArtifactRepository managedRepository;

    /**
     * The remote repositories that are being proxied.
     */
    private List/*<ArtifactRepository>*/ proxiedRepositories;

    /**
     * A wagon proxy to communicate to the proxy repository over a proxy (eg, http proxy)... TerminologyOverflowException
     */
    private final ProxyInfo wagonProxy;

    /**
     * Constructor.
     *
     * @param proxiedRepositories the proxied repository
     * @param managedRepository   the locally managed repository
     * @param wagonProxy          the network proxy to use
     */
    public ProxiedRepositoryGroup( List/*<ArtifactRepository>*/ proxiedRepositories,
                                   ArtifactRepository managedRepository, ProxyInfo wagonProxy )
    {
        this.proxiedRepositories = proxiedRepositories;

        this.managedRepository = managedRepository;

        this.wagonProxy = wagonProxy;
    }

    /**
     * Constructor.
     *
     * @param proxiedRepositories the proxied repository
     * @param managedRepository   the locally managed repository
     */
    public ProxiedRepositoryGroup( List/*<ArtifactRepository>*/ proxiedRepositories,
                                   ArtifactRepository managedRepository )
    {
        this( proxiedRepositories, managedRepository, null );
    }

    public ArtifactRepository getManagedRepository()
    {
        return managedRepository;
    }

    public List getProxiedRepositories()
    {
        return proxiedRepositories;
    }

    public ProxyInfo getWagonProxy()
    {
        return wagonProxy;
    }
}
