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
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.scheduler.CronExpressionValidator;

import java.io.File;
import java.io.IOException;

/**
 * Configures the application repositories.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="configureRepositoryAction"
 */
public class ConfigureRepositoryAction
    extends AbstractConfigureRepositoryAction
    implements Preparable
{
    /**
     * The model for this action.
     */
    private ManagedRepositoryConfiguration repository;

    /**
     * @plexus.requirement role-hint="default"
     */
    protected RoleManager roleManager;

    public String add()
    {
        this.mode = "add";

        this.repository.setReleases( true );
        this.repository.setScanned( true );

        return INPUT;
    }

    public String delete()
    {
        String result = SUCCESS;
        if ( StringUtils.equals( mode, "delete-entry" ) || StringUtils.equals( mode, "delete-contents" ) )
        {
            ManagedRepositoryConfiguration existingRepository = repository;
            if ( existingRepository == null )
            {
                addActionError( "A repository with that id does not exist" );
                return ERROR;
            }

            // TODO: remove from index too!

            try
            {
                Configuration configuration = archivaConfiguration.getConfiguration();
                removeRepository( repoid, configuration );
                result = saveConfiguration( configuration );

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

    public ManagedRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public void prepare()
    {
        String id = repoid;
        if ( id == null )
        {
            this.repository = new ManagedRepositoryConfiguration();
            this.repository.setReleases( false );
            this.repository.setScanned( false );
        }
        else
        {
            repository = archivaConfiguration.getConfiguration().findManagedRepositoryById( id );
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

    private void addRepository( ManagedRepositoryConfiguration repository, Configuration configuration )
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

        configuration.addManagedRepository( repository );

        // TODO: double check these are configured on start up
        // TODO: belongs in the business logic
        roleManager.createTemplatedRole( "archiva-repository-manager", repository.getId() );

        roleManager.createTemplatedRole( "archiva-repository-observer", repository.getId() );
    }

    private void removeContents( ManagedRepositoryConfiguration existingRepository )
        throws IOException
    {
        FileUtils.deleteDirectory( new File( existingRepository.getLocation() ) );
    }

    private void removeRepository( String repoId, Configuration configuration )
    {
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

    public void setRoleManager( RoleManager roleManager )
    {
        this.roleManager = roleManager;
    }
}
