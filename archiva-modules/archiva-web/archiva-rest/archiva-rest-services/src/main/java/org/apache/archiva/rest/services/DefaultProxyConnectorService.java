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
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.proxyconnector.ProxyConnectorAdmin;
import org.apache.archiva.rest.api.model.ProxyConnector;
import org.apache.archiva.rest.api.services.ProxyConnectorService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        throws RepositoryAdminException
    {
        List<ProxyConnector> proxyConnectors = new ArrayList<ProxyConnector>();
        for ( org.apache.archiva.admin.repository.proxyconnector.ProxyConnector proxyConnector : proxyConnectorAdmin.getProxyConnectors() )
        {
            proxyConnectors.add( new BeanReplicator().replicateBean( proxyConnector, ProxyConnector.class ) );
        }
        return proxyConnectors;
    }

    public ProxyConnector getProxyConnector( String sourceRepoId, String targetRepoId )
        throws RepositoryAdminException
    {
        org.apache.archiva.admin.repository.proxyconnector.ProxyConnector proxyConnector =
            proxyConnectorAdmin.getProxyConnector( sourceRepoId, targetRepoId );
        return proxyConnector == null
            ? null
            : new BeanReplicator().replicateBean( proxyConnector, ProxyConnector.class );
    }

    public Boolean addProxyConnector( ProxyConnector proxyConnector )
        throws RepositoryAdminException
    {
        if ( proxyConnector == null )
        {
            return Boolean.FALSE;
        }
        return proxyConnectorAdmin.addProxyConnector( new BeanReplicator().replicateBean( proxyConnector,
                                                                                          org.apache.archiva.admin.repository.proxyconnector.ProxyConnector.class ),
                                                      getAuditInformation() );
    }

    public Boolean deleteProxyConnector( ProxyConnector proxyConnector )
        throws RepositoryAdminException
    {
        if ( proxyConnector == null )
        {
            return Boolean.FALSE;
        }
        return proxyConnectorAdmin.deleteProxyConnector( new BeanReplicator().replicateBean( proxyConnector,
                                                                                             org.apache.archiva.admin.repository.proxyconnector.ProxyConnector.class ),
                                                         getAuditInformation() );
    }

    public Boolean updateProxyConnector( ProxyConnector proxyConnector )
        throws RepositoryAdminException
    {
        if ( proxyConnector == null )
        {
            return Boolean.FALSE;
        }
        return proxyConnectorAdmin.updateProxyConnector( new BeanReplicator().replicateBean( proxyConnector,
                                                                                             org.apache.archiva.admin.repository.proxyconnector.ProxyConnector.class ),
                                                         getAuditInformation() );
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


