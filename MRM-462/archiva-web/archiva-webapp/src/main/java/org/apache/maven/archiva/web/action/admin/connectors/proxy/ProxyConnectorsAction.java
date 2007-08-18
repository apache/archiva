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

import com.opensymphony.xwork.Preparable;
import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.admin.repositories.AdminRepositoryConfiguration;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.xwork.interceptor.SecureAction;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProxyConnectorsAction
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="proxyConnectorsAction"
 */
public class ProxyConnectorsAction
    extends PlexusActionSupport
    implements SecureAction, Preparable
{
    /**
     * @plexus.requirement role-hint="adminrepoconfig"
     */
    private Transformer repoConfigToAdmin;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private Map /*<String,AdminRepositoryConfiguration>*/repoMap;

    /**
     * Map of Proxy Connectors.
     */
    private Map /*<String,AdminProxyConnector>*/proxyConnectorMap;

    public void prepare()
        throws Exception
    {
        Configuration config = archivaConfiguration.getConfiguration();

        repoMap = new HashMap();

        Closure addToRepoMap = new Closure()
        {
            public void execute( Object input )
            {
                AdminRepositoryConfiguration arepo =
                    (AdminRepositoryConfiguration) repoConfigToAdmin.transform( input );
                repoMap.put( arepo.getId(), arepo );
            }
        };

        CollectionUtils.forAllDo( config.getManagedRepositories(), addToRepoMap );
        CollectionUtils.forAllDo( config.getRemoteRepositories(), addToRepoMap );

        proxyConnectorMap = new HashMap();

        Closure addToProxyConnectorMap = new Closure()
        {
            public void execute( Object input )
            {
                if ( input instanceof ProxyConnectorConfiguration )
                {
                    ProxyConnectorConfiguration proxyConfig = (ProxyConnectorConfiguration) input;
                    String key = proxyConfig.getSourceRepoId();

                    List connectors = (List) proxyConnectorMap.get( key );
                    if ( connectors == null )
                    {
                        connectors = new ArrayList();
                        proxyConnectorMap.put( key, connectors );
                    }

                    connectors.add( proxyConfig );
                }
            }
        };

        CollectionUtils.forAllDo( config.getProxyConnectors(), addToProxyConnectorMap );
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public Map getRepoMap()
    {
        return repoMap;
    }

    public void setRepoMap( Map repoMap )
    {
        this.repoMap = repoMap;
    }

    public Map getProxyConnectorMap()
    {
        return proxyConnectorMap;
    }

    public void setProxyConnectorMap( Map proxyConnectorMap )
    {
        this.proxyConnectorMap = proxyConnectorMap;
    }
}
