package org.apache.archiva.admin.repository.networkproxy;
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
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.NetworkProxyConfiguration;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.features.RemoteIndexFeature;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service( "networkProxyAdmin#default" )
public class DefaultNetworkProxyAdmin
    extends AbstractRepositoryAdmin
    implements NetworkProxyAdmin
{

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Override
    public List<NetworkProxy> getNetworkProxies()
        throws RepositoryAdminException
    {
        List<NetworkProxy> networkProxies =
            new ArrayList<>( getArchivaConfiguration().getConfiguration().getNetworkProxies().size() );
        for ( NetworkProxyConfiguration networkProxyConfiguration : getArchivaConfiguration().getConfiguration().getNetworkProxies() )
        {
            networkProxies.add( getNetworkProxy( networkProxyConfiguration ) );
        }
        return networkProxies;
    }

    @Override
    public NetworkProxy getNetworkProxy( String networkProxyId )
        throws RepositoryAdminException
    {
        for ( NetworkProxy networkProxy : getNetworkProxies() )
        {
            if ( StringUtils.equals( networkProxyId, networkProxy.getId() ) )
            {
                return networkProxy;
            }
        }

        return null;
    }

    @Override
    public void addNetworkProxy( NetworkProxy networkProxy, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        if ( networkProxy == null )
        {
            return;
        }
        if ( getNetworkProxy( networkProxy.getId() ) != null )
        {
            throw new RepositoryAdminException(
                "cannot add NetworkProxy with id " + networkProxy.getId() + " already exist" );
        }
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        configuration.addNetworkProxy( getNetworkProxyConfiguration( networkProxy ) );

        triggerAuditEvent( networkProxy.getId(), null, AuditEvent.ADD_NETWORK_PROXY, auditInformation );

        saveConfiguration( configuration );
    }

    @Override
    public void updateNetworkProxy( NetworkProxy networkProxy, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        if ( networkProxy == null )
        {
            return;
        }
        if ( getNetworkProxy( networkProxy.getId() ) == null )
        {
            throw new RepositoryAdminException(
                "cannot update NetworkProxy with id " + networkProxy.getId() + " as not exist" );
        }
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        NetworkProxyConfiguration networkProxyConfiguration = getNetworkProxyConfiguration( networkProxy );
        configuration.removeNetworkProxy( networkProxyConfiguration );
        configuration.addNetworkProxy( networkProxyConfiguration );

        triggerAuditEvent( networkProxy.getId(), null, AuditEvent.MODIFY_NETWORK_PROXY, auditInformation );

        saveConfiguration( configuration );
    }

    @Override
    public void deleteNetworkProxy( String networkProxyId, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

        NetworkProxy networkProxy = getNetworkProxy( networkProxyId );
        if ( networkProxy == null )
        {
            throw new RepositoryAdminException(
                "cannot delete NetworkProxy with id " + networkProxyId + " as not exist" );
        }
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        NetworkProxyConfiguration networkProxyConfiguration = getNetworkProxyConfiguration( networkProxy );
        configuration.removeNetworkProxy( networkProxyConfiguration );

        for ( RemoteRepository repo : repositoryRegistry.getRemoteRepositories()) {
            if (repo.supportsFeature( RemoteIndexFeature.class )) {
                RemoteIndexFeature rif = repo.getFeature( RemoteIndexFeature.class ).get();
                if (networkProxyId.equals(rif.getProxyId())) {
                    rif.setProxyId( null );
                    try
                    {
                        repositoryRegistry.putRepository( repo, configuration);
                    }
                    catch ( RepositoryException e )
                    {
                        log.error("Could not update repository {}", repo.getId(), e);
                    }
                }
            }
        }

        triggerAuditEvent( networkProxy.getId(), null, AuditEvent.DELETE_NETWORK_PROXY, auditInformation );

        saveConfiguration( configuration );
    }

    protected NetworkProxy getNetworkProxy( NetworkProxyConfiguration networkProxyConfiguration )
    {
        return networkProxyConfiguration == null
            ? null
            : getModelMapper().map( networkProxyConfiguration, NetworkProxy.class );
    }

    protected NetworkProxyConfiguration getNetworkProxyConfiguration( NetworkProxy networkProxy )
    {
        return networkProxy == null
            ? null
            : getModelMapper().map( networkProxy, NetworkProxyConfiguration.class );
    }
}
