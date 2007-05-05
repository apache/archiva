package org.apache.maven.archiva.web.action.admin.repositories;

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

import com.opensymphony.xwork.ActionContext;
import com.opensymphony.xwork.Preparable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
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
 * Configures the application repositories.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="configureRepositoryAction"
 */
public class ConfigureRepositoryAction
    extends PlexusActionSupport
    implements Preparable, SecureAction
{
    /**
     * @plexus.requirement role-hint="archiva"
     */
    private RoleProfileManager roleProfileManager;

    /**
     * @plexus.requirement
     */
    private SecuritySystem securitySystem;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private String repoid;

    private String mode;

    /**
     * The model for this action.
     */
    private AdminRepositoryConfiguration repository;

    public String add()
    {
        getLogger().info( ".add()" );
        this.mode = "add";

        return INPUT;
    }

    public String confirm()
    {
        getLogger().info( ".confirm()" );

        if ( operationAllowed( ArchivaRoleConstants.OPERATION_DELETE_REPOSITORY, getRepoid() ) )
        {
            addActionError( "You do not have the appropriate permissions to delete the " + getRepoid() + " repository." );
            return ERROR;
        }

        return INPUT;
    }

    public String delete()
    {
        getLogger().info( ".delete()" );

        if ( operationAllowed( ArchivaRoleConstants.OPERATION_DELETE_REPOSITORY, getRepoid() ) )
        {
            addActionError( "You do not have the appropriate permissions to delete the " + getRepoid() + " repository." );
            return ERROR;
        }

        if ( StringUtils.equals( mode, "delete-entry" ) || StringUtils.equals( mode, "delete-contents" ) )
        {
            AdminRepositoryConfiguration existingRepository = getRepository();
            if ( existingRepository == null )
            {
                addActionError( "A repository with that id does not exist" );
                return ERROR;
            }

            // TODO: remove from index too!

            try
            {
                removeRepository( getRepoid() );
                removeRepositoryRoles( existingRepository );
                saveConfiguration();

                if ( StringUtils.equals( mode, "delete-contents" ) )
                {
                    removeContents( existingRepository );
                }
            }
            catch ( IOException e )
            {
                addActionError( "Unable to delete repository: " + e.getMessage() );
            }
            catch ( RoleProfileException e )
            {
                addActionError( "Unable to delete repository: " + e.getMessage() );
            }
            catch ( InvalidConfigurationException e )
            {
                addActionError( "Unable to delete repository: " + e.getMessage() );
            }
            catch ( RbacManagerException e )
            {
                addActionError( "Unable to delete repository: " + e.getMessage() );
            }
            catch ( RegistryException e )
            {
                addActionError( "Unable to delete repository: " + e.getMessage() );
            }
        }

        return SUCCESS;
    }

    public String edit()
    {
        getLogger().info( ".edit()" );
        this.mode = "edit";

        if ( operationAllowed( ArchivaRoleConstants.OPERATION_EDIT_REPOSITORY, getRepoid() ) )
        {
            addActionError( "You do not have the appropriate permissions to edit the " + getRepoid() + " repository." );
            return ERROR;
        }

        return INPUT;
    }

    public String getMode()
    {
        return this.mode;
    }

    public String getRepoid()
    {
        return repoid;
    }

    public AdminRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public void prepare()
        throws Exception
    {
        String id = getRepoid();
        if ( id == null )
        {
            this.repository = new AdminRepositoryConfiguration();
        }

        RepositoryConfiguration repoconfig = archivaConfiguration.getConfiguration().findRepositoryById( id );
        if ( repoconfig != null )
        {
            this.repository = new AdminRepositoryConfiguration( repoconfig );
        }
    }

    public String save()
    {
        String mode = getMode();
        String repoId = getRepository().getId();

        getLogger().info( ".save(" + mode + ":" + repoId + ")" );

        if ( StringUtils.isBlank( repository.getId() ) )
        {
            addFieldError( "id", "A repository with a blank id cannot be saved." );
            return SUCCESS;
        }

        if ( StringUtils.equalsIgnoreCase( "edit", mode ) )
        {
            removeRepository( repoId );
        }

        try
        {
            addRepository( getRepository() );
            saveConfiguration();
        }
        catch ( IOException e )
        {
            addActionError( "I/O Exception: " + e.getMessage() );
        }
        catch ( RoleProfileException e )
        {
            addActionError( "Role Profile Exception: " + e.getMessage() );
        }
        catch ( InvalidConfigurationException e )
        {
            addActionError( "Invalid Configuration Exception: " + e.getMessage() );
        }
        catch ( RbacManagerException e )
        {
            addActionError( "RBAC Manager Exception: " + e.getMessage() );
        }
        catch ( RegistryException e )
        {
            addActionError( "Configuration Registry Exception: " + e.getMessage() );
        }

        return SUCCESS;
    }

    public void setMode( String mode )
    {
        this.mode = mode;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }

    public void setRepository( AdminRepositoryConfiguration repository )
    {
        this.repository = repository;
    }

    private void addRepository( AdminRepositoryConfiguration repository )
        throws IOException, RoleProfileException
    {
        getLogger().info( ".addRepository(" + repository + ")" );

        // Fix the URL entry (could possibly be a filesystem path)
        String rawUrlEntry = repository.getUrl();
        repository.setUrl( PathUtil.toUrl( rawUrlEntry ) );
        
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

    private boolean operationAllowed( String permission, String repoid )
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

    private void removeContents( AdminRepositoryConfiguration existingRepository )
        throws IOException
    {
        if ( existingRepository.isManaged() )
        {
            getLogger().info( "Removing " + existingRepository.getDirectory() );
            FileUtils.deleteDirectory( new File( existingRepository.getDirectory() ) );
        }
    }

    private void removeRepository( String repoId )
    {
        getLogger().info( ".removeRepository()" );

        RepositoryConfiguration toremove = archivaConfiguration.getConfiguration().findRepositoryById( repoId );
        if ( toremove != null )
        {
            archivaConfiguration.getConfiguration().removeRepository( toremove );
        }
    }

    private void removeRepositoryRoles( RepositoryConfiguration existingRepository )
        throws RoleProfileException
    {
        roleProfileManager.deleteDynamicRole( "archiva-repository-manager", existingRepository.getId() );
        roleProfileManager.deleteDynamicRole( "archiva-repository-observer", existingRepository.getId() );

        getLogger().info( "removed user roles associated with repository " + existingRepository.getId() );
    }

    private String saveConfiguration()
        throws IOException, InvalidConfigurationException, RbacManagerException, RoleProfileException,
        RegistryException
    {
        getLogger().info( ".saveConfiguration()" );

        archivaConfiguration.save( archivaConfiguration.getConfiguration() );

        addActionMessage( "Successfully saved configuration" );

        return SUCCESS;
    }
}
