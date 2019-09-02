package org.apache.archiva.admin.repository.proxyconnectorrule;
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
import org.apache.archiva.admin.model.beans.ProxyConnectorRule;
import org.apache.archiva.proxy.model.ProxyConnectorRuleType;
import org.apache.archiva.admin.model.proxyconnectorrule.ProxyConnectorRuleAdmin;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.ProxyConnectorRuleConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Service("proxyConnectorRuleAdmin#default")
public class DefaultProxyConnectorRuleAdmin
    extends AbstractRepositoryAdmin
    implements ProxyConnectorRuleAdmin
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Override
    public List<ProxyConnectorRule> getProxyConnectorRules()
        throws RepositoryAdminException
    {
        List<ProxyConnectorRuleConfiguration> proxyConnectorRuleConfigurations =
            getArchivaConfiguration().getConfiguration().getProxyConnectorRuleConfigurations();
        if ( proxyConnectorRuleConfigurations.isEmpty() )
        {
            return Collections.emptyList();
        }
        List<ProxyConnectorRule> proxyConnectorRules = new ArrayList<>( proxyConnectorRuleConfigurations.size() );
        for ( ProxyConnectorRuleConfiguration proxyConnectorRuleConfiguration : proxyConnectorRuleConfigurations )
        {

            ProxyConnectorRule proxyConnectorRule = new ProxyConnectorRule();
            proxyConnectorRule.setPattern( proxyConnectorRuleConfiguration.getPattern() );
            proxyConnectorRule.setProxyConnectorRuleType(
                getProxyConnectorRuleType( proxyConnectorRuleConfiguration.getRuleType() ) );
            for ( ProxyConnectorConfiguration proxyConnectorConfiguration : proxyConnectorRuleConfiguration.getProxyConnectors() )
            {
                ProxyConnector proxyConnector = new ProxyConnector();
                proxyConnector.setSourceRepoId( proxyConnectorConfiguration.getSourceRepoId() );
                proxyConnector.setTargetRepoId( proxyConnectorConfiguration.getTargetRepoId() );
                proxyConnectorRule.getProxyConnectors().add( proxyConnector );
            }
            proxyConnectorRules.add( proxyConnectorRule );
        }

        return proxyConnectorRules;
    }


    private ProxyConnectorRuleType getProxyConnectorRuleType( String type )
    {
        if ( StringUtils.equals( ProxyConnectorRuleType.WHITE_LIST.getRuleType(), type ) )
        {
            return ProxyConnectorRuleType.WHITE_LIST;
        }
        if ( StringUtils.equals( ProxyConnectorRuleType.BLACK_LIST.getRuleType(), type ) )
        {
            return ProxyConnectorRuleType.BLACK_LIST;
        }
        return null;
    }

    @Override
    public void addProxyConnectorRule( ProxyConnectorRule proxyConnectorRule, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        ProxyConnectorRuleConfiguration proxyConnectorRuleConfiguration = new ProxyConnectorRuleConfiguration();
        proxyConnectorRuleConfiguration.setPattern( proxyConnectorRule.getPattern() );
        proxyConnectorRuleConfiguration.setRuleType( proxyConnectorRule.getProxyConnectorRuleType().getRuleType() );
        for ( ProxyConnector proxyConnector : proxyConnectorRule.getProxyConnectors() )
        {
            ProxyConnectorConfiguration proxyConnectorConfiguration = new ProxyConnectorConfiguration();
            proxyConnectorConfiguration.setSourceRepoId( proxyConnector.getSourceRepoId() );
            proxyConnectorConfiguration.setTargetRepoId( proxyConnector.getTargetRepoId() );
            proxyConnectorRuleConfiguration.getProxyConnectors().add( proxyConnectorConfiguration );
        }
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        configuration.getProxyConnectorRuleConfigurations().add( proxyConnectorRuleConfiguration );
        saveConfiguration( configuration );
    }

    @Override
    public void deleteProxyConnectorRule( ProxyConnectorRule proxyConnectorRule, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        // key is the pattern !!
        // recreate a list without the pattern

        boolean toSave = false;

        List<ProxyConnectorRuleConfiguration> proxyConnectorRuleConfigurations = new ArrayList<>();

        for ( ProxyConnectorRuleConfiguration proxyConnectorRuleConfiguration : configuration.getProxyConnectorRuleConfigurations() )
        {
            if ( StringUtils.equals( proxyConnectorRuleConfiguration.getPattern(), proxyConnectorRule.getPattern() )
                && StringUtils.equals( proxyConnectorRuleConfiguration.getRuleType(),
                                       proxyConnectorRule.getProxyConnectorRuleType().getRuleType() ) )
            {
                toSave = true;
            }
            else
            {
                proxyConnectorRuleConfigurations.add( proxyConnectorRuleConfiguration );
            }
        }

        if ( toSave )
        {
            configuration.setProxyConnectorRuleConfigurations( proxyConnectorRuleConfigurations );
            saveConfiguration( configuration );
        }

    }

    @Override
    public void updateProxyConnectorRule( ProxyConnectorRule proxyConnectorRule, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();

        for ( ProxyConnectorRuleConfiguration proxyConnectorRuleConfiguration : configuration.getProxyConnectorRuleConfigurations() )
        {
            if ( StringUtils.equals( proxyConnectorRuleConfiguration.getPattern(), proxyConnectorRule.getPattern() )
                && StringUtils.equals( proxyConnectorRuleConfiguration.getRuleType(),
                                       proxyConnectorRule.getProxyConnectorRuleType().getRuleType() ) )
            {
                List<ProxyConnectorConfiguration> proxyConnectors =
                    new ArrayList<>( proxyConnectorRule.getProxyConnectors().size() );
                for ( ProxyConnector proxyConnector : proxyConnectorRule.getProxyConnectors() )
                {
                    ProxyConnectorConfiguration proxyConnectorConfiguration = new ProxyConnectorConfiguration();
                    proxyConnectorConfiguration.setSourceRepoId( proxyConnector.getSourceRepoId() );
                    proxyConnectorConfiguration.setTargetRepoId( proxyConnector.getTargetRepoId() );
                    proxyConnectors.add( proxyConnectorConfiguration );
                }
                proxyConnectorRuleConfiguration.setProxyConnectors( proxyConnectors );
                saveConfiguration( configuration );
            }
        }

    }
}
