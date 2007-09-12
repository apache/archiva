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
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.registry.RegistryException;

import java.io.IOException;

/**
 * Configures the application repositories.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="configureRemoteRepositoryAction"
 */
public class ConfigureRemoteRepositoryAction
    extends AbstractConfigureRepositoryAction
    implements Preparable
{
    /**
     * The model for this action.
     */
    private RemoteRepositoryConfiguration repository;

    public String add()
    {
        this.mode = "add";

        return INPUT;
    }

    public String delete()
    {
        RemoteRepositoryConfiguration existingRepository = repository;
        if ( existingRepository == null )
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
        }
        catch ( IOException e )
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

        return result;
    }

    public RemoteRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public void prepare()
    {
        String id = repoid;
        if ( id == null )
        {
            this.repository = new RemoteRepositoryConfiguration();
        }
        else
        {
            this.repository = archivaConfiguration.getConfiguration().findRemoteRepositoryById( id );
        }
    }

    public String save()
    {
        // TODO! share
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
        // TODO! share
        boolean containsError = false;
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

        if ( StringUtils.isBlank( repository.getUrl() ) )
        {
            addFieldError( "repository.url", "You must enter a URL." );
            containsError = true;
        }
        if ( StringUtils.isBlank( repository.getName() ) )
        {
            addFieldError( "repository.name", "You must enter a repository name." );
            containsError = true;
        }

        return containsError;
    }

    private void addRepository( RemoteRepositoryConfiguration repository, Configuration configuration )
        throws IOException, RoleManagerException
    {
        configuration.addRemoteRepository( repository );
    }

    private void removeRepository( String repoId, Configuration configuration )
    {
        RemoteRepositoryConfiguration toremove = configuration.findRemoteRepositoryById( repoId );
        if ( toremove != null )
        {
            configuration.removeRemoteRepository( toremove );
        }
    }
}
