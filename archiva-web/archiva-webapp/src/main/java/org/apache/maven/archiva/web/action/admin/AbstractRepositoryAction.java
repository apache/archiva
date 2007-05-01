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

import com.opensymphony.xwork.ActionContext;
import com.opensymphony.xwork.ModelDriven;
import com.opensymphony.xwork.Preparable;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.admin.models.AdminRepositoryConfiguration;
import org.codehaus.plexus.rbac.profile.RoleProfileException;
import org.codehaus.plexus.rbac.profile.RoleProfileManager;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.security.authorization.AuthorizationException;
import org.codehaus.plexus.security.authorization.AuthorizationResult;
import org.codehaus.plexus.security.rbac.RbacManagerException;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.system.SecuritySession;
import org.codehaus.plexus.security.system.SecuritySystem;
import org.codehaus.plexus.security.ui.web.interceptor.SecureAction;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionBundle;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.io.File;
import java.io.IOException;

/**
 * AbstractRepositoryAction 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractRepositoryAction
    extends PlexusActionSupport
    implements ModelDriven, Preparable, SecureAction
{
    protected static final String SUCCESS = "success";

    /**
     * @plexus.requirement role-hint="archiva"
     */
    private RoleProfileManager roleProfileManager;

    /**
     * @plexus.requirement
     */
    private SecuritySystem securitySystem;

    private String repoid;

    private String mode;

    /**
     * @plexus.requirement
     */
    protected ArchivaConfiguration archivaConfiguration;

    /**
     * The model for this action.
     */
    protected AdminRepositoryConfiguration repository;

    public String getMode()
    {
        return this.mode;
    }

    public Object getModel()
    {
        return getRepository();
    }

    public String getRepoid()
    {
        return repoid;
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public String input()
    {
        getLogger().info( "input()" );
        return INPUT;
    }

    public abstract void prepare()
        throws Exception;

    public void setMode( String mode )
    {
        this.mode = mode;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }

    protected void addRepository( AdminRepositoryConfiguration repository )
        throws IOException, RoleProfileException
    {
        getLogger().info( ".addRepository(" + repository + ")" );

        if ( repository.isManaged() )
        {
            // Normalize the path
            File file = new File( repository.getDirectory() );
            repository.setDirectory( file.getCanonicalPath() );
            if ( !file.exists() )
            {
                file.mkdirs();
                // TODO: error handling when this fails, or is not a directory!
            }
        }

        archivaConfiguration.getConfiguration().addRepository( repository );

        // TODO: double check these are configured on start up
        roleProfileManager.getDynamicRole( "archiva-repository-manager", repository.getId() );

        roleProfileManager.getDynamicRole( "archiva-repository-observer", repository.getId() );
    }

    protected AdminRepositoryConfiguration getRepository()
    {
        if ( repository == null )
        {
            repository = new AdminRepositoryConfiguration();
        }

        return repository;
    }

    protected boolean operationAllowed( String permission, String repoid )
    {
        ActionContext context = ActionContext.getContext();
        SecuritySession securitySession = (SecuritySession) context.get( SecuritySession.ROLE );

        AuthorizationResult authzResult;
        try
        {
            authzResult = securitySystem.authorize( securitySession, permission, repoid );

            return authzResult.isAuthorized();
        }
        catch ( AuthorizationException e )
        {
            getLogger().info(
                              "Unable to authorize permission: " + permission + " against repo: " + repoid
                                  + " due to: " + e.getMessage() );
            return false;
        }
    }

    protected void removeRepository( String repoId )
    {
        getLogger().info( ".removeRepository()" );

        RepositoryConfiguration toremove = archivaConfiguration.getConfiguration().findRepositoryById( repoId );
        if ( toremove != null )
        {
            archivaConfiguration.getConfiguration().removeRepository( toremove );
        }
    }

    protected String saveConfiguration()
        throws IOException, InvalidConfigurationException, RbacManagerException, RoleProfileException,
        RegistryException
    {
        getLogger().info( ".saveConfiguration()" );

        archivaConfiguration.save( archivaConfiguration.getConfiguration() );

        addActionMessage( "Successfully saved configuration" );

        return SUCCESS;
    }
}
