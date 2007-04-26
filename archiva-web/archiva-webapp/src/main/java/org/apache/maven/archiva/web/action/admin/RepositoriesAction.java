package org.apache.maven.archiva.web.action.admin;

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

import com.opensymphony.webwork.interceptor.ServletRequestAware;
import com.opensymphony.xwork.ModelDriven;
import com.opensymphony.xwork.Preparable;
import com.opensymphony.xwork.Validateable;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.ui.web.interceptor.SecureAction;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionBundle;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import javax.servlet.http.HttpServletRequest;

/**
 * Shows the Repositories Tab for the administrator. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="repositoriesAction"
 */
public class RepositoriesAction
    extends PlexusActionSupport
    implements ModelDriven, Preparable, Validateable, SecureAction, ServletRequestAware
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    private HttpServletRequest request;

    private AdminModel model;

    public Object getModel()
    {
        return model;
    }

    public void prepare()
        throws Exception
    {
        model = new AdminModel( archivaConfiguration.getConfiguration() );
    }

    public void validate()
    {
        super.validate();
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public void setServletRequest( HttpServletRequest request )
    {
        this.request = request;
        StringBuffer baseUrl = new StringBuffer();

        baseUrl.append( request.getScheme() );
        baseUrl.append( request.getServerName() );
        int portnum = request.getServerPort();

        // Only add port if non-standard.
        if ( ( "https".equalsIgnoreCase( request.getScheme() ) && ( portnum != 443 ) )
            || ( "http".equalsIgnoreCase( request.getScheme() ) && ( portnum != 80 ) ) )
        {
            baseUrl.append( ":" ).append( String.valueOf( portnum ) );
        }
        baseUrl.append( request.getContextPath() );
        baseUrl.append( "/repository" );

        model.setBaseUrl( baseUrl.toString() );
    }
}
