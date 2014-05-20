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
import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorAdmin;
import org.apache.archiva.policies.Policy;
import org.apache.archiva.rest.api.model.PolicyInformation;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.ProxyConnectorService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@Service( "proxyConnectorService#rest" )
public class DefaultProxyConnectorService
    extends AbstractRestService
    implements ProxyConnectorService
{

    private List<Policy> allPolicies;

    @Inject
    public DefaultProxyConnectorService( ApplicationContext applicationContext )
    {
        allPolicies = new ArrayList<>( getBeansOfType( applicationContext, Policy.class ).values() );
    }

    @Override
    public List<ProxyConnector> getProxyConnectors()
        throws ArchivaRestServiceException
    {
        try
        {
            List<ProxyConnector> proxyConnectors = proxyConnectorAdmin.getProxyConnectors();
            return proxyConnectors == null ? Collections.<ProxyConnector>emptyList() : proxyConnectors;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public ProxyConnector getProxyConnector( String sourceRepoId, String targetRepoId )
        throws ArchivaRestServiceException
    {
        try
        {
            return proxyConnectorAdmin.getProxyConnector( sourceRepoId, targetRepoId );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public Boolean addProxyConnector( ProxyConnector proxyConnector )
        throws ArchivaRestServiceException
    {
        if ( proxyConnector == null )
        {
            return Boolean.FALSE;
        }
        try
        {
            return proxyConnectorAdmin.addProxyConnector( proxyConnector, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public Boolean deleteProxyConnector( ProxyConnector proxyConnector )
        throws ArchivaRestServiceException
    {
        if ( proxyConnector == null )
        {
            return Boolean.FALSE;
        }
        try
        {
            return proxyConnectorAdmin.deleteProxyConnector( proxyConnector, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public Boolean removeProxyConnector( String sourceRepoId, String targetRepoId )
        throws ArchivaRestServiceException
    {
        ProxyConnector proxyConnector = getProxyConnector( sourceRepoId, targetRepoId );
        if ( proxyConnector == null )
        {
            throw new ArchivaRestServiceException(
                "proxyConnector with sourceRepoId:" + sourceRepoId + " and targetRepoId:" + targetRepoId
                    + " not exists", null );
        }
        return deleteProxyConnector( proxyConnector );
    }

    @Override
    public Boolean updateProxyConnector( ProxyConnector proxyConnector )
        throws ArchivaRestServiceException
    {
        if ( proxyConnector == null )
        {
            return Boolean.FALSE;
        }
        try
        {
            return proxyConnectorAdmin.updateProxyConnector( proxyConnector, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public List<PolicyInformation> getAllPolicyInformations()
        throws ArchivaRestServiceException
    {
        List<PolicyInformation> policyInformations = new ArrayList<>( allPolicies.size() );

        for ( Policy policy : allPolicies )
        {
            policyInformations.add(
                new PolicyInformation( policy.getOptions(), policy.getDefaultOption(), policy.getId(),
                                       policy.getName() ) );
        }

        return policyInformations;
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


