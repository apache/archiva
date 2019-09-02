package org.apache.archiva.admin.repository.proxyconnector;
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
import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorAdmin;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorOrderComparator;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.functors.ProxyConnectorSelectionPredicate;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service ( "proxyConnectorAdmin#default" )
public class DefaultProxyConnectorAdmin
    extends AbstractRepositoryAdmin
    implements ProxyConnectorAdmin
{

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Override
    public List<ProxyConnector> getProxyConnectors()
        throws RepositoryAdminException
    {
        List<ProxyConnectorConfiguration> proxyConnectorConfigurations =
            getArchivaConfiguration().getConfiguration().getProxyConnectors();
        List<ProxyConnector> proxyConnectors = new ArrayList<>( proxyConnectorConfigurations.size() );
        for ( ProxyConnectorConfiguration configuration : proxyConnectorConfigurations )
        {
            proxyConnectors.add( getProxyConnector( configuration ) );
        }
        Collections.sort( proxyConnectors, ProxyConnectorOrderComparator.getInstance() );
        return proxyConnectors;
    }

    @Override
    public ProxyConnector getProxyConnector( String sourceRepoId, String targetRepoId )
        throws RepositoryAdminException
    {
        for ( ProxyConnector proxyConnector : getProxyConnectors() )
        {
            if ( StringUtils.equals( sourceRepoId, proxyConnector.getSourceRepoId() ) && StringUtils.equals(
                targetRepoId, proxyConnector.getTargetRepoId() ) )
            {
                return proxyConnector;
            }
        }
        return null;
    }

    @Override
    public Boolean addProxyConnector( ProxyConnector proxyConnector, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        if ( getProxyConnector( proxyConnector.getSourceRepoId(), proxyConnector.getTargetRepoId() ) != null )
        {
            throw new RepositoryAdminException(
                "Unable to add proxy connector, as one already exists with source repository id ["
                    + proxyConnector.getSourceRepoId() + "] and target repository id ["
                    + proxyConnector.getTargetRepoId() + "]." );
        }

        validateProxyConnector( proxyConnector );

        proxyConnector.setBlackListPatterns( unescapePatterns( proxyConnector.getBlackListPatterns() ) );
        proxyConnector.setWhiteListPatterns( unescapePatterns( proxyConnector.getWhiteListPatterns() ) );

        Configuration configuration = getArchivaConfiguration().getConfiguration();

        ProxyConnectorConfiguration proxyConnectorConfiguration = getProxyConnectorConfiguration( proxyConnector );
        configuration.addProxyConnector( proxyConnectorConfiguration );
        saveConfiguration( configuration );
        triggerAuditEvent( proxyConnector.getSourceRepoId() + "-" + proxyConnector.getTargetRepoId(), null,
                           AuditEvent.ADD_PROXY_CONNECTOR, auditInformation );
        return Boolean.TRUE;

    }

    // FIXME take care of proxyConnectorRules !
    @Override
    public Boolean deleteProxyConnector( ProxyConnector proxyConnector, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        ProxyConnectorConfiguration proxyConnectorConfiguration =
            findProxyConnector( proxyConnector.getSourceRepoId(), proxyConnector.getTargetRepoId(), configuration );
        if ( proxyConnectorConfiguration == null )
        {
            throw new RepositoryAdminException(
                "unable to find ProxyConnector with source " + proxyConnector.getSourceRepoId() + " and target "
                    + proxyConnector.getTargetRepoId() );
        }
        configuration.removeProxyConnector( proxyConnectorConfiguration );
        saveConfiguration( configuration );
        triggerAuditEvent( proxyConnector.getSourceRepoId() + "-" + proxyConnector.getTargetRepoId(), null,
                           AuditEvent.DELETE_PROXY_CONNECTOR, auditInformation );
        return Boolean.TRUE;
    }

    // FIXME care take of proxyConnectorRules !
    @Override
    public Boolean updateProxyConnector( ProxyConnector proxyConnector, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        ProxyConnectorConfiguration proxyConnectorConfiguration =
            findProxyConnector( proxyConnector.getSourceRepoId(), proxyConnector.getTargetRepoId(), configuration );
        configuration.removeProxyConnector( proxyConnectorConfiguration );
        configuration.addProxyConnector( getProxyConnectorConfiguration( proxyConnector ) );
        saveConfiguration( configuration );
        triggerAuditEvent( proxyConnector.getSourceRepoId() + "-" + proxyConnector.getTargetRepoId(), null,
                           AuditEvent.MODIFY_PROXY_CONNECTOR, auditInformation );
        return Boolean.TRUE;
    }

    protected List<String> unescapePatterns( List<String> patterns )
    {
        if ( patterns != null )
        {
            List<String> rawPatterns = new ArrayList<>( patterns.size() );
            for ( String pattern : patterns )
            {
                rawPatterns.add( StringUtils.replace( pattern, "\\\\", "\\" ) );
            }
            return rawPatterns;
        }

        return Collections.emptyList();
    }

    @Override
    public Map<String, List<ProxyConnector>> getProxyConnectorAsMap()
        throws RepositoryAdminException
    {
        Map<String, List<ProxyConnector>> proxyConnectorMap = new HashMap<>();

        Iterator<ProxyConnector> it = getProxyConnectors().iterator();
        while ( it.hasNext() )
        {
            ProxyConnector proxyConfig = it.next();
            String key = proxyConfig.getSourceRepoId();

            List<ProxyConnector> connectors = proxyConnectorMap.get( key );
            if ( connectors == null )
            {
                connectors = new ArrayList<>( 1 );
                proxyConnectorMap.put( key, connectors );
            }

            connectors.add( proxyConfig );

            Collections.sort( connectors, ProxyConnectorOrderComparator.getInstance() );
        }

        return proxyConnectorMap;
    }

    private ProxyConnectorConfiguration findProxyConnector( String sourceId, String targetId,
                                                            Configuration configuration )
    {
        if ( StringUtils.isBlank( sourceId ) )
        {
            return null;
        }

        if ( StringUtils.isBlank( targetId ) )
        {
            return null;
        }

        ProxyConnectorSelectionPredicate selectedProxy = new ProxyConnectorSelectionPredicate( sourceId, targetId );
        return IterableUtils.find( configuration.getProxyConnectors(), selectedProxy );
    }

    protected ProxyConnectorConfiguration getProxyConnectorConfiguration( ProxyConnector proxyConnector )
    {
        return proxyConnector == null
            ? null
            : getModelMapper().map( proxyConnector, ProxyConnectorConfiguration.class );
    }

    protected ProxyConnector getProxyConnector( ProxyConnectorConfiguration proxyConnectorConfiguration )
    {
        return proxyConnectorConfiguration == null
            ? null
            : getModelMapper().map( proxyConnectorConfiguration, ProxyConnector.class );
    }

    protected void validateProxyConnector( ProxyConnector proxyConnector )
        throws RepositoryAdminException
    {
        // validate source a Managed target a Remote
        if ( repositoryRegistry.getManagedRepository( proxyConnector.getSourceRepoId() ) == null )
        {
            throw new RepositoryAdminException(
                "non valid ProxyConnector sourceRepo with id " + proxyConnector.getSourceRepoId()
                    + " is not a ManagedRepository" );
        }
        if ( repositoryRegistry.getRemoteRepository( proxyConnector.getTargetRepoId() ) == null )
        {
            throw new RepositoryAdminException(
                "non valid ProxyConnector sourceRepo with id " + proxyConnector.getTargetRepoId()
                    + " is not a RemoteRepository" );
        }

        // FIXME validate NetworkProxyConfiguration too when available
    }
}
