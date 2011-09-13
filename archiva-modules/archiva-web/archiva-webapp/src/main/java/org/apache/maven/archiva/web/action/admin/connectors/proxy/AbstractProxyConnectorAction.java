package org.apache.maven.archiva.web.action.admin.connectors.proxy;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnector;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorAdmin;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.web.action.AbstractActionSupport;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * AbstractProxyConnectorAction
 *
 * @version $Id$
 */
public abstract class AbstractProxyConnectorAction
    extends AbstractActionSupport
    implements SecureAction
{
    public static final String DIRECT_CONNECTION = "(direct connection)";

    @Inject
    private ProxyConnectorAdmin proxyConnectorAdmin;

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }


    protected void addProxyConnector( ProxyConnector proxyConnector )
        throws RepositoryAdminException
    {
        getProxyConnectorAdmin().addProxyConnector( proxyConnector, getAuditInformation() );
    }

    protected ProxyConnector findProxyConnector( String sourceId, String targetId )
        throws RepositoryAdminException
    {
        if ( StringUtils.isBlank( sourceId ) )
        {
            return null;
        }

        if ( StringUtils.isBlank( targetId ) )
        {
            return null;
        }

        return getProxyConnectorAdmin().getProxyConnector( sourceId, targetId );
    }

    protected Map<String, List<ProxyConnector>> createProxyConnectorMap()
        throws RepositoryAdminException
    {
        return getProxyConnectorAdmin().getProxyConnectorAsMap();
    }

    protected void removeConnector( String sourceId, String targetId )
        throws RepositoryAdminException
    {
        ProxyConnector proxyConnector = findProxyConnector( sourceId, targetId );
        if ( proxyConnector != null )
        {
            getProxyConnectorAdmin().deleteProxyConnector( proxyConnector, getAuditInformation() );
        }
    }

    protected void removeProxyConnector( ProxyConnector connector )
        throws RepositoryAdminException
    {
        getProxyConnectorAdmin().deleteProxyConnector( connector, getAuditInformation() );
    }


    public ProxyConnectorAdmin getProxyConnectorAdmin()
    {
        return proxyConnectorAdmin;
    }

    public void setProxyConnectorAdmin( ProxyConnectorAdmin proxyConnectorAdmin )
    {
        this.proxyConnectorAdmin = proxyConnectorAdmin;
    }

    public RemoteRepositoryAdmin getRemoteRepositoryAdmin()
    {
        return remoteRepositoryAdmin;
    }

    public void setRemoteRepositoryAdmin( RemoteRepositoryAdmin remoteRepositoryAdmin )
    {
        this.remoteRepositoryAdmin = remoteRepositoryAdmin;
    }

    public ManagedRepositoryAdmin getManagedRepositoryAdmin()
    {
        return managedRepositoryAdmin;
    }

    public void setManagedRepositoryAdmin( ManagedRepositoryAdmin managedRepositoryAdmin )
    {
        this.managedRepositoryAdmin = managedRepositoryAdmin;
    }
}
