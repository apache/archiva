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
 * Configures the managed repositories.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="configureRepositoryAction"
 */
public class ConfigureRepositoryAction
    extends AbstractConfigureRepositoryAction<ManagedRepositoryConfiguration>
    implements Preparable
{
    /**
     * @plexus.requirement role-hint="default"
     */
    protected RoleManager roleManager;

    private String deleteMode = "delete-entry";

    public String getDeleteMode()
    {
        return deleteMode;
    }

    public void setDeleteMode( String deleteMode )
    {
        this.deleteMode = deleteMode;
    }

    public String addInput()
    {
        // set defaults
        this.repository.setReleases( true );
        this.repository.setScanned( true );

        return INPUT;
    }

    public String editInput()
    {
        return INPUT;
    }

    public String delete()
    {
        if ( repository == null )
        {
            addActionError( "A repository with that id does not exist" );
            return ERROR;
        }

        String result;
        try
        {
            Configuration configuration = archivaConfiguration.getConfiguration();
            removeRepository( repoid, configuration );
            result = saveConfiguration( configuration );

            if ( result.equals( SUCCESS ) )
            {
                removeRepositoryRoles( repository );
                if ( StringUtils.equals( deleteMode, "delete-contents" ) )
                {
                    removeContents( repository );
                }
            }
        }
        catch ( IOException e )
        {
            addActionError( "Unable to delete repository: " + e.getMessage() );
            result = ERROR;
        }
        catch ( RoleManagerException e )
        {
            addActionError( "Unable to delete repository: " + e.getMessage() );
            result = ERROR;
        }
        catch ( InvalidConfigurationException e )
        {
            addActionError( "Unable to delete repository: " + e.getMessage() );
            result = ERROR;
        }
        catch ( RegistryException e )
        {
            addActionError( "Unable to delete repository: " + e.getMessage() );
            result = ERROR;
        }

        return result;
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

    protected boolean validateFields( Configuration config )
    {
        boolean containsError = false;
        CronExpressionValidator validator = new CronExpressionValidator();
        String repoId = repository.getId();

        if ( StringUtils.isBlank( repoId ) )
        {
            addFieldError( "repository.id", "You must enter a repository identifier." );
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

    protected void addRepository( ManagedRepositoryConfiguration repository, Configuration configuration )
        throws IOException, RoleManagerException
    {
        // Normalize the path
        File file = new File( repository.getLocation() );
        repository.setLocation( file.getCanonicalPath() );
        if ( !file.exists() )
        {
            file.mkdirs();
        }
        if ( !file.exists() || !file.isDirectory() )
        {
            throw new IOException( "unable to add repository - can not create the root directory: " + file );
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

    protected void removeRepository( String repoId, Configuration configuration )
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
