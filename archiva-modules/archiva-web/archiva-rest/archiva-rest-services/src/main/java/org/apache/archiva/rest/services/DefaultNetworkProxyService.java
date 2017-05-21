package org.apache.archiva.rest.services;
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.NetworkProxy;
import org.apache.archiva.admin.model.networkproxy.NetworkProxyAdmin;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.NetworkProxyService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "networkProxyService#rest" )
public class DefaultNetworkProxyService
    extends AbstractRestService
    implements NetworkProxyService
{
    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    @Override
    public List<NetworkProxy> getNetworkProxies()
        throws ArchivaRestServiceException
    {
        try
        {
            List<NetworkProxy> networkProxies = networkProxyAdmin.getNetworkProxies();
            return networkProxies == null ? Collections.<NetworkProxy>emptyList() : networkProxies;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public NetworkProxy getNetworkProxy( String networkProxyId )
        throws ArchivaRestServiceException
    {
        try
        {
            return networkProxyAdmin.getNetworkProxy( networkProxyId );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public void addNetworkProxy( NetworkProxy networkProxy )
        throws ArchivaRestServiceException
    {
        try
        {
            if ( networkProxy == null )
            {
                return;
            }
            getNetworkProxyAdmin().addNetworkProxy( networkProxy, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public void updateNetworkProxy( NetworkProxy networkProxy )
        throws ArchivaRestServiceException
    {
        if ( networkProxy == null )
        {
            return;
        }
        try
        {
            getNetworkProxyAdmin().updateNetworkProxy( networkProxy, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public Boolean deleteNetworkProxy( String networkProxyId )
        throws ArchivaRestServiceException
    {
        try
        {
            getNetworkProxyAdmin().deleteNetworkProxy( networkProxyId, getAuditInformation() );
            return Boolean.TRUE;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    public NetworkProxyAdmin getNetworkProxyAdmin()
    {
        return networkProxyAdmin;
    }

    public void setNetworkProxyAdmin( NetworkProxyAdmin networkProxyAdmin )
    {
        this.networkProxyAdmin = networkProxyAdmin;
    }
}
