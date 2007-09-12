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

import com.opensymphony.xwork.Preparable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.xwork.interceptor.SecureAction;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionException;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.scheduler.CronExpressionValidator;
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
     * @plexus.requirement role-hint="default"
     */
    private RoleManager roleManager;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private String repoid;

    // TODO! consider removing? was just meant to be for delete...
    private String mode;

    /**
     * The model for this action.
     */
    private AdminRepositoryConfiguration repository;

    public String add()
    {
        this.mode = "add";

        this.repository.setReleases( true );
        this.repository.setIndexed( true );

        return INPUT;
    }

    public String confirm()
    {
        return INPUT;
    }

    public String delete()
    {
        String result = SUCCESS;
        if ( StringUtils.equals( mode, "delete-entry" ) || StringUtils.equals( mode, "delete-contents" ) )
        {
            AdminRepositoryConfiguration existingRepository = repository;
            if ( existingRepository == null )
            {
                addActionError( "A repository with that id does not exist" );
                return ERROR;
            }

            // TODO: remove from index too!

            try
            {
                removeRepository( repoid, archivaConfiguration.getConfiguration() );
                result = saveConfiguration( archivaConfiguration.getConfiguration() );

                if ( result.equals( SUCCESS ) )
                {
                    removeRepositoryRoles( existingRepository );
                    if ( StringUtils.equals( mode, "delete-contents" ) )
                    {
                        removeContents( existingRepository );
                    }
                }
            }
            catch ( IOException e )
            {
                addActionError( "Unable to delete repository: " + e.getMessage() );
                result = INPUT;
            }
            catch ( RoleManagerException e )
            {
                addActionError( "Unable to delete repository: " + e.getMessage() );
                result = INPUT;
            }
            catch ( InvalidConfigurationException e )
            {
                addActionError( "Unable to delete repository: " + e.getMessage() );
                result = INPUT;
            }
            catch ( RegistryException e )
            {
                addActionError( "Unable to delete repository: " + e.getMessage() );
                result = INPUT;
            }
        }

        return result;
    }

    public String edit()
    {
        this.mode = "edit";

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
    {
        String id = repoid;
        if ( id == null )
        {
            this.repository = new AdminRepositoryConfiguration();
            this.repository.setReleases( false );
            this.repository.setIndexed( false );
        }

        // TODO! others?
        ManagedRepositoryConfiguration repoconfig =
            archivaConfiguration.getConfiguration().findManagedRepositoryById( id );
        if ( repoconfig != null )
        {
            this.repository = new AdminRepositoryConfiguration( repoconfig );
        }
    }

    public String save()
    {
        String repoId = repository.getId();

        Configuration configuration = archivaConfiguration.getConfiguration();
        boolean containsError = validateFields( configuration );

        if ( containsError && StringUtils.equalsIgnoreCase( "add", mode ) )
        {
            return INPUT;
        }
        else if ( containsError && StringUtils.equalsIgnoreCase( "edit", this.mode ) )
        {
            return ERROR;
        }

        if ( StringUtils.equalsIgnoreCase( "edit", this.mode ) )
        {
            removeRepository( repoId, configuration );
        }

        String result;
        try
        {
            addRepository( repository, configuration );
            result = saveConfiguration( configuration );
        }
        catch ( IOException e )
        {
            addActionError( "I/O Exception: " + e.getMessage() );
            result = INPUT;
        }
        catch ( RoleManagerException e )
        {
            addActionError( "Role Manager Exception: " + e.getMessage() );
            result = INPUT;
        }
        catch ( InvalidConfigurationException e )
        {
            addActionError( "Invalid Configuration Exception: " + e.getMessage() );
            result = INPUT;
        }
        catch ( RegistryException e )
        {
            addActionError( "Configuration Registry Exception: " + e.getMessage() );
            result = INPUT;
        }

        return result;
    }

    private boolean validateFields( Configuration config )
    {
        boolean containsError = false;
        CronExpressionValidator validator = new CronExpressionValidator();
        String repoId = repository.getId();

        if ( StringUtils.isBlank( repoId ) )
        {
            addFieldError( "repository.id", "You must enter a repository identifier." );
            containsError = true;
        }
        //if edit mode, do not validate existence of repoId
        else if ( ( config.getManagedRepositoriesAsMap().containsKey( repoId ) ||
            config.getRemoteRepositoriesAsMap().containsKey( repoId ) ) &&
            !StringUtils.equalsIgnoreCase( mode, "edit" ) )
        {
            addFieldError( "repository.id",
                           "Unable to add new repository with id [" + repoId + "], that id already exists." );
            containsError = true;
        }

        // TODO! split
        if ( StringUtils.isBlank( repository.getLocation() ) )
        {
            addFieldError( "repository.location", "You must enter a directory." );
            containsError = true;
        }
        if ( StringUtils.isBlank( repository.getName() ) )
        {
            addFieldError( "repository.name", "You must enter a repository name." );
            containsError = true;
        }
        if ( !validator.validate( repository.getRefreshCronExpression() ) )
        {
            addFieldError( "repository.refreshCronExpression", "Invalid cron expression." );
            containsError = true;
        }

        return containsError;
    }

    public void setMode( String mode )
    {
        this.mode = mode;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }

    private void addRepository( AdminRepositoryConfiguration repository, Configuration configuration )
        throws IOException, RoleManagerException
    {
        // Normalize the path
        File file = new File( repository.getLocation() );
        repository.setLocation( file.getCanonicalPath() );
        if ( !file.exists() )
        {
            file.mkdirs();
            // TODO: error handling when this fails, or is not a directory!
        }

        // TODO! others
        configuration.addManagedRepository( repository );

        // TODO: double check these are configured on start up
        // TODO: belongs in the business logic
        roleManager.createTemplatedRole( "archiva-repository-manager", repository.getId() );

        roleManager.createTemplatedRole( "archiva-repository-observer", repository.getId() );
    }

    private void removeContents( AdminRepositoryConfiguration existingRepository )
        throws IOException
    {
        FileUtils.deleteDirectory( new File( existingRepository.getLocation() ) );
    }

    private void removeRepository( String repoId, Configuration configuration )
    {
        // TODO! what about others?
        ManagedRepositoryConfiguration toremove = configuration.findManagedRepositoryById( repoId );
        if ( toremove != null )
        {
            configuration.removeManagedRepository( toremove );
        }
    }

    private void removeRepositoryRoles( ManagedRepositoryConfiguration existingRepository )
        throws RoleManagerException
    {
        roleManager.removeTemplatedRole( "archiva-repository-manager", existingRepository.getId() );
        roleManager.removeTemplatedRole( "archiva-repository-observer", existingRepository.getId() );

        getLogger().debug( "removed user roles associated with repository " + existingRepository.getId() );
    }

    private String saveConfiguration( Configuration configuration )
        throws IOException, InvalidConfigurationException, RegistryException
    {
        try
        {
            archivaConfiguration.save( configuration );
            addActionMessage( "Successfully saved configuration" );
        }
        catch ( IndeterminateConfigurationException e )
        {
            addActionError( e.getMessage() );
            return INPUT;
        }

        return SUCCESS;
    }

    public void setRoleManager( RoleManager roleManager )
    {
        this.roleManager = roleManager;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

}
