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
import org.apache.maven.archiva.database.constraints.MostRecentRepositoryScanStatistics;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.admin.models.AdminModel;
import org.apache.maven.archiva.web.action.admin.models.AdminRepositoryConfiguration;
import org.apache.maven.archiva.web.util.ContextUtils;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.ui.web.interceptor.SecureAction;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionBundle;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.util.Iterator;
import java.util.List;

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
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private AdminModel model;

    private String baseUrl;

    public Object getModel()
    {
        return getAdminModel();
    }

    public void prepare()
        throws Exception
    {
        model = null;
        getModel();
    }

    public void validate()
    {
        super.validate();
    }

    public String execute()
        throws Exception
    {
        getLogger().info( ".execute()" );
        return super.execute();
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
        this.baseUrl = ContextUtils.getBaseURL( request, "repository" );
    }

    public AdminModel getAdminModel()
    {
        if ( model == null )
        {
            model = new AdminModel( archivaConfiguration.getConfiguration() );
            model.setBaseUrl( baseUrl );
            updateLastIndexed( model.getManagedRepositories() );
        }

        return model;
    }

    private void updateLastIndexed( List managedRepositories )
    {
        Iterator it = managedRepositories.iterator();
        while ( it.hasNext() )
        {
            AdminRepositoryConfiguration config = (AdminRepositoryConfiguration) it.next();

            List results = dao.query( new MostRecentRepositoryScanStatistics( config.getId() ) );
            if ( !results.isEmpty() )
            {
                RepositoryContentStatistics stats = (RepositoryContentStatistics) results.get( 0 );
                config.setStats( stats );
            }
        }
    }

    public String getBaseUrlB()
    {
        return baseUrl;
    }

    public void setBaseUrlB( String baseUrl )
    {
        this.baseUrl = baseUrl;
    }
}
