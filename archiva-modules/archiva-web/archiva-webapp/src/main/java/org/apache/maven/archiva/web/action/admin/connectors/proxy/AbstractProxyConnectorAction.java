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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.functors.NotPredicate;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.functors.ProxyConnectorSelectionPredicate;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.PlexusActionSupport;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.struts2.interceptor.SecureAction;
import org.codehaus.plexus.redback.struts2.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.struts2.interceptor.SecureActionException;
import org.codehaus.plexus.registry.RegistryException;

import java.util.List;
import java.util.Map;

/**
 * AbstractProxyConnectorAction 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractProxyConnectorAction
    extends PlexusActionSupport
    implements SecureAction
{
    public static final String DIRECT_CONNECTION = "(direct connection)";

    /**
     * @plexus.requirement
     */
    protected ArchivaConfiguration archivaConfiguration;

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    protected void addProxyConnector( ProxyConnectorConfiguration proxyConnector )
    {
        getConfig().addProxyConnector( proxyConnector );
    }

    protected ProxyConnectorConfiguration findProxyConnector( String sourceId, String targetId )
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
        return (ProxyConnectorConfiguration) CollectionUtils.find( getConfig().getProxyConnectors(), selectedProxy );
    }

    protected Configuration getConfig()
    {
        return this.archivaConfiguration.getConfiguration();
    }

    protected Map<String, List<ProxyConnectorConfiguration>> createProxyConnectorMap()
    {
        return getConfig().getProxyConnectorAsMap();
    }

    protected void removeConnector( String sourceId, String targetId )
    {
        ProxyConnectorSelectionPredicate selectedProxy = new ProxyConnectorSelectionPredicate( sourceId, targetId );
        NotPredicate notSelectedProxy = new NotPredicate( selectedProxy );
        CollectionUtils.filter( getConfig().getProxyConnectors(), notSelectedProxy );
    }

    protected void removeProxyConnector( ProxyConnectorConfiguration connector )
    {
        getConfig().removeProxyConnector( connector );
    }

    protected String saveConfiguration()
    {
        try
        {
            archivaConfiguration.save( getConfig() );
            addActionMessage( "Successfully saved configuration" );
        }
        catch ( RegistryException e )
        {
            addActionError( "Unable to save configuration: " + e.getMessage() );
            return INPUT;
        }
        catch ( IndeterminateConfigurationException e )
        {
            addActionError( e.getMessage() );
            return INPUT;
        }

        return SUCCESS;
    }
}
