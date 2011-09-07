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

import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.AuditInformation;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.remote.RemoteRepositoryAdmin;
import org.apache.archiva.audit.AuditEvent;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.functors.ProxyConnectorSelectionPredicate;
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
 * @since 1.4
 */
@Service( "proxyConnectorAdmin#default" )
public class DefaultProxyConnectorAdmin
    extends AbstractRepositoryAdmin
    implements ProxyConnectorAdmin
{

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    public List<ProxyConnector> getProxyConnectors()
        throws RepositoryAdminException
    {
        List<ProxyConnectorConfiguration> proxyConnectorConfigurations =
            getArchivaConfiguration().getConfiguration().getProxyConnectors();
        List<ProxyConnector> proxyConnectors = new ArrayList<ProxyConnector>( proxyConnectorConfigurations.size() );
        for ( ProxyConnectorConfiguration configuration : proxyConnectorConfigurations )
        {
            ProxyConnector proxyConnector = new ProxyConnector();
            proxyConnectors.add( proxyConnector );
            proxyConnector.setOrder( configuration.getOrder() );
            proxyConnector.setBlackListPatterns( new ArrayList<String>( configuration.getBlackListPatterns() ) );
            proxyConnector.setWhiteListPatterns( new ArrayList<String>( configuration.getWhiteListPatterns() ) );
            proxyConnector.setDisabled( configuration.isDisabled() );
            proxyConnector.setPolicies( new HashMap<String, String>( configuration.getPolicies() ) );
            proxyConnector.setProperties( new HashMap<String, String>( configuration.getProperties() ) );
            proxyConnector.setProxyId( configuration.getProxyId() );
            proxyConnector.setSourceRepoId( configuration.getSourceRepoId() );
            proxyConnector.setTargetRepoId( configuration.getTargetRepoId() );
        }

        return proxyConnectors;
    }

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

    protected List<String> unescapePatterns( List<String> patterns )
    {
        List<String> rawPatterns = new ArrayList<String>();
        if ( patterns != null )
        {
            for ( String pattern : patterns )
            {
                rawPatterns.add( StringUtils.replace( pattern, "\\\\", "\\" ) );
            }
        }

        return rawPatterns;
    }

    public Map<String, List<ProxyConnector>> getProxyConnectorAsMap()
        throws RepositoryAdminException
    {
        java.util.Map<String, List<ProxyConnector>> proxyConnectorMap =
            new HashMap<String, java.util.List<ProxyConnector>>();

        Iterator<ProxyConnector> it = getProxyConnectors().iterator();
        while ( it.hasNext() )
        {
            ProxyConnector proxyConfig = it.next();
            String key = proxyConfig.getSourceRepoId();

            java.util.List<ProxyConnector> connectors = proxyConnectorMap.get( key );
            if ( connectors == null )
            {
                connectors = new ArrayList<ProxyConnector>();
                proxyConnectorMap.put( key, connectors );
            }

            connectors.add( proxyConfig );

            Collections.sort( connectors, ProxyConnectorOrderComparator.getInstance() );
        }

        return proxyConnectorMap;
    }

    public ProxyConnector findProxyConnector( String sourceId, String targetId )
        throws RepositoryAdminException
    {
        return getProxyConnector(
            findProxyConnector( sourceId, targetId, getArchivaConfiguration().getConfiguration() ) );
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
        return (ProxyConnectorConfiguration) CollectionUtils.find( configuration.getProxyConnectors(), selectedProxy );
    }

    protected ProxyConnectorConfiguration getProxyConnectorConfiguration( ProxyConnector proxyConnector )
    {
        /*
        ProxyConnectorConfiguration proxyConnectorConfiguration = new ProxyConnectorConfiguration();
        proxyConnectorConfiguration.setOrder( proxyConnector.getOrder() );
        proxyConnectorConfiguration.setBlackListPatterns(
            new ArrayList<String>( proxyConnector.getBlackListPatterns() ) );
        proxyConnectorConfiguration.setWhiteListPatterns(
            new ArrayList<String>( proxyConnector.getWhiteListPatterns() ) );
        proxyConnectorConfiguration.setDisabled( proxyConnector.isDisabled() );
        proxyConnectorConfiguration.setPolicies( new HashMap( proxyConnector.getPolicies() ) );
        proxyConnectorConfiguration.setProperties( new HashMap( proxyConnector.getProperties() ) );
        proxyConnectorConfiguration.setProxyId( proxyConnector.getProxyId() );
        proxyConnectorConfiguration.setSourceRepoId( proxyConnector.getSourceRepoId() );
        proxyConnectorConfiguration.setTargetRepoId( proxyConnector.getTargetRepoId() );
        return proxyConnectorConfiguration;*/
        return new BeanReplicator().replicateBean( proxyConnector, ProxyConnectorConfiguration.class );
    }

    protected ProxyConnector getProxyConnector( ProxyConnectorConfiguration proxyConnectorConfiguration )
    {
        return new BeanReplicator().replicateBean( proxyConnectorConfiguration, ProxyConnector.class );
    }

    protected void validateProxyConnector( ProxyConnector proxyConnector )
        throws RepositoryAdminException
    {
        // validate source a Managed target a Remote
        if ( managedRepositoryAdmin.getManagedRepository( proxyConnector.getSourceRepoId() ) == null )
        {
            throw new RepositoryAdminException(
                "non valid ProxyConnector sourceRepo with id " + proxyConnector.getSourceRepoId()
                    + " is not a ManagedRepository" );
        }
        if ( remoteRepositoryAdmin.getRemoteRepository( proxyConnector.getTargetRepoId() ) == null )
        {
            throw new RepositoryAdminException(
                "non valid ProxyConnector sourceRepo with id " + proxyConnector.getTargetRepoId()
                    + " is not a RemoteRepository" );
        }

        // FIXME validate NetworkProxyConfiguration too
    }
}
