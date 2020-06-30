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
import org.apache.archiva.admin.model.beans.ProxyConnectorRule;
import org.apache.archiva.admin.model.proxyconnectorrule.ProxyConnectorRuleAdmin;
import org.apache.archiva.rest.api.model.ActionStatus;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.ProxyConnectorRuleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service ("proxyConnectorRuleService#rest")
public class DefaultProxyConnectorRuleService
    extends AbstractRestService
    implements ProxyConnectorRuleService
{

    @Inject
    private ProxyConnectorRuleAdmin proxyConnectorRuleAdmin;

    @Override
    public List<ProxyConnectorRule> getProxyConnectorRules()
        throws ArchivaRestServiceException
    {
        try
        {
            return proxyConnectorRuleAdmin.getProxyConnectorRules();
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    private void validateProxyConnectorRule( ProxyConnectorRule proxyConnectorRule )
        throws ArchivaRestServiceException
    {
        if ( StringUtils.isEmpty( proxyConnectorRule.getPattern() ) )
        {
            ArchivaRestServiceException e = new ArchivaRestServiceException( "pattern cannot be empty", null );
            e.setErrorKey( "proxy-connector-rule.pattern.empty" );
            throw e;
        }

        if ( proxyConnectorRule.getProxyConnectors() == null || proxyConnectorRule.getProxyConnectors().isEmpty() )
        {
            ArchivaRestServiceException e =
                new ArchivaRestServiceException( "proxyConnector rule must have proxyConnectors.", null );
            e.setErrorKey( "proxy-connector-rule.pattern.connectors.empty" );
            throw e;
        }

        for ( ProxyConnectorRule proxyConnectorRule1 : getProxyConnectorRules() )
        {
            if ( StringUtils.equals( proxyConnectorRule.getPattern(), proxyConnectorRule1.getPattern() )
                && proxyConnectorRule.getProxyConnectorRuleType() == proxyConnectorRule1.getProxyConnectorRuleType() )
            {
                ArchivaRestServiceException e =
                    new ArchivaRestServiceException( "same ProxyConnector rule already exists.", null );
                e.setErrorKey( "proxy-connector-rule.pattern.already.exists" );
                throw e;
            }
        }
    }

    @Override
    public ActionStatus addProxyConnectorRule( ProxyConnectorRule proxyConnectorRule )
        throws ArchivaRestServiceException
    {

        validateProxyConnectorRule( proxyConnectorRule );

        try
        {
            proxyConnectorRuleAdmin.addProxyConnectorRule( proxyConnectorRule, getAuditInformation() );
            return ActionStatus.SUCCESS;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public ActionStatus deleteProxyConnectorRule( ProxyConnectorRule proxyConnectorRule )
        throws ArchivaRestServiceException
    {
        try
        {
            proxyConnectorRuleAdmin.deleteProxyConnectorRule( proxyConnectorRule, getAuditInformation() );
            return ActionStatus.SUCCESS;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public ActionStatus updateProxyConnectorRule( ProxyConnectorRule proxyConnectorRule )
        throws ArchivaRestServiceException
    {
        try
        {
            proxyConnectorRuleAdmin.updateProxyConnectorRule( proxyConnectorRule, getAuditInformation() );
            return ActionStatus.SUCCESS;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }
}
