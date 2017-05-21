package org.apache.archiva.mock;

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

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.NetworkProxy;
import org.apache.archiva.admin.model.networkproxy.NetworkProxyAdmin;

import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class MockNetworkProxyAdmin
    implements NetworkProxyAdmin
{
    @Override
    public List<NetworkProxy> getNetworkProxies()
        throws RepositoryAdminException
    {
        return Collections.emptyList();
    }

    @Override
    public NetworkProxy getNetworkProxy( String networkProxyId )
        throws RepositoryAdminException
    {
        return null;
    }

    @Override
    public void addNetworkProxy( NetworkProxy networkProxy, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        // no op
    }

    @Override
    public void updateNetworkProxy( NetworkProxy networkProxy, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        // no op
    }

    @Override
    public void deleteNetworkProxy( String networkProxyId, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        // no op
    }
}
