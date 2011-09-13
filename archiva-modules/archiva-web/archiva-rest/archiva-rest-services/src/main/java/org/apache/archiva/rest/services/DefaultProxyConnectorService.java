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

import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorAdmin;
import org.apache.archiva.rest.api.model.ProxyConnector;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.ProxyConnectorService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "proxyConnectorService#rest" )
public class DefaultProxyConnectorService
    extends AbstractRestService
    implements ProxyConnectorService
{
    @Inject
    private ProxyConnectorAdmin proxyConnectorAdmin;

    public List<ProxyConnector> getProxyConnectors()
        throws ArchivaRestServiceException
    {
        try
        {
            List<ProxyConnector> proxyConnectors = new ArrayList<ProxyConnector>();
            for ( org.apache.archiva.admin.model.proxyconnector.ProxyConnector proxyConnector : proxyConnectorAdmin.getProxyConnectors() )
            {
                proxyConnectors.add( new BeanReplicator().replicateBean( proxyConnector, ProxyConnector.class ) );
            }
            return proxyConnectors;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public ProxyConnector getProxyConnector( String sourceRepoId, String targetRepoId )
        throws ArchivaRestServiceException
    {
        try
        {
            org.apache.archiva.admin.model.proxyconnector.ProxyConnector proxyConnector =
                proxyConnectorAdmin.getProxyConnector( sourceRepoId, targetRepoId );
            return proxyConnector == null
                ? null
                : new BeanReplicator().replicateBean( proxyConnector, ProxyConnector.class );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public Boolean addProxyConnector( ProxyConnector proxyConnector )
        throws ArchivaRestServiceException
    {
        if ( proxyConnector == null )
        {
            return Boolean.FALSE;
        }
        try
        {
            return proxyConnectorAdmin.addProxyConnector( new BeanReplicator().replicateBean( proxyConnector,
                                                                                              org.apache.archiva.admin.model.proxyconnector.ProxyConnector.class ),
                                                          getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public Boolean deleteProxyConnector( ProxyConnector proxyConnector )
        throws ArchivaRestServiceException
    {
        if ( proxyConnector == null )
        {
            return Boolean.FALSE;
        }
        try
        {
            return proxyConnectorAdmin.deleteProxyConnector( new BeanReplicator().replicateBean( proxyConnector,
                                                                                                 org.apache.archiva.admin.model.proxyconnector.ProxyConnector.class ),
                                                             getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public Boolean updateProxyConnector( ProxyConnector proxyConnector )
        throws ArchivaRestServiceException
    {
        if ( proxyConnector == null )
        {
            return Boolean.FALSE;
        }
        try
        {
            return proxyConnectorAdmin.updateProxyConnector( new BeanReplicator().replicateBean( proxyConnector,
                                                                                                 org.apache.archiva.admin.model.proxyconnector.ProxyConnector.class ),
                                                             getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public ProxyConnectorAdmin getProxyConnectorAdmin()
    {
        return proxyConnectorAdmin;
    }

    public void setProxyConnectorAdmin( ProxyConnectorAdmin proxyConnectorAdmin )
    {
        this.proxyConnectorAdmin = proxyConnectorAdmin;
    }
}


