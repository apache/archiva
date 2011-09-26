package org.apache.archiva.web.action.admin.repositories;

import org.apache.archiva.admin.model.beans.NetworkProxy;
import org.apache.archiva.admin.model.networkproxy.NetworkProxyAdmin;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

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

/**
 * AbstractRemoteRepositoriesAction
 *
 * @version $Id$
 */
public class AbstractRemoteRepositoriesAction
    extends AbstractRepositoriesAdminAction
{
    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    private List<NetworkProxy> networkProxies;

    public RemoteRepositoryAdmin getRemoteRepositoryAdmin()
    {
        return remoteRepositoryAdmin;
    }

    public void setRemoteRepositoryAdmin( RemoteRepositoryAdmin remoteRepositoryAdmin )
    {
        this.remoteRepositoryAdmin = remoteRepositoryAdmin;
    }

    public NetworkProxyAdmin getNetworkProxyAdmin()
    {
        return networkProxyAdmin;
    }

    public void setNetworkProxyAdmin( NetworkProxyAdmin networkProxyAdmin )
    {
        this.networkProxyAdmin = networkProxyAdmin;
    }

    public List<NetworkProxy> getNetworkProxies()
    {
        return networkProxies == null ? Collections.<NetworkProxy>emptyList() : networkProxies;
    }

    public void setNetworkProxies( List<NetworkProxy> networkProxies )
    {
        this.networkProxies = networkProxies;
    }
}
