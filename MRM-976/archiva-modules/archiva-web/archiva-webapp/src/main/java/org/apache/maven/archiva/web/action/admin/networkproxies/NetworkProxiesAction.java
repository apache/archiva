package org.apache.maven.archiva.web.action.admin.networkproxies;

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

import com.opensymphony.xwork2.Preparable;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.PlexusActionSupport;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.struts2.interceptor.SecureAction;
import org.codehaus.plexus.redback.struts2.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.struts2.interceptor.SecureActionException;

import java.util.List;

/**
 * NetworkProxiesAction 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="networkProxiesAction"
 */
public class NetworkProxiesAction
    extends PlexusActionSupport
    implements Preparable, SecureAction
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    private List networkProxies;

    public void prepare()
        throws Exception
    {
        networkProxies = configuration.getConfiguration().getNetworkProxies();
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public List getNetworkProxies()
    {
        return networkProxies;
    }

    public void setNetworkProxies( List networkProxies )
    {
        this.networkProxies = networkProxies;
    }
}
