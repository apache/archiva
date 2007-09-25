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

import org.apache.maven.archiva.configuration.AbstractRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.xwork.interceptor.SecureAction;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionException;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.io.IOException;

/**
 * Base class for repository configuration actions.
 */
public abstract class AbstractConfigureRepositoryAction<T extends AbstractRepositoryConfiguration>
    extends PlexusActionSupport
    implements SecureAction
{
    /**
     * The model for this action.
     */
    protected T repository;

    /**
     * @plexus.requirement
     */
    protected ArchivaConfiguration archivaConfiguration;

    protected String repoid;

    public String getRepoid()
    {
        return repoid;
    }

    public T getRepository()
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

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    protected String saveConfiguration( Configuration configuration )
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
            return ERROR;
        }

        return SUCCESS;
    }

    public String add()
    {
        Configuration configuration = archivaConfiguration.getConfiguration();

        String repoId = repository.getId();
        if ( configuration.getManagedRepositoriesAsMap().containsKey( repoId ) ||
            configuration.getRemoteRepositoriesAsMap().containsKey( repoId ) )
        {
            addFieldError( "repository.id",
                           "Unable to add new repository with id [" + repoId + "], that id already exists." );
            return INPUT;
        }

        boolean containsError = validateFields( configuration );
        if ( containsError )
        {
            return INPUT;
        }

        return saveRepositoryConfiguration( configuration );
    }

    public String edit()
    {
        Configuration configuration = archivaConfiguration.getConfiguration();

        boolean containsError = validateFields( configuration );
        if ( containsError )
        {
            return INPUT;
        }

        removeRepository( repository.getId(), configuration );

        return saveRepositoryConfiguration( configuration );
    }

    protected String saveRepositoryConfiguration( Configuration configuration )
    {
        String result;
        try
        {
            addRepository( repository, configuration );
            result = saveConfiguration( configuration );
        }
        catch ( IOException e )
        {
            addActionError( "I/O Exception: " + e.getMessage() );
            result = ERROR;
        }
        catch ( InvalidConfigurationException e )
        {
            addActionError( "Invalid Configuration Exception: " + e.getMessage() );
            result = ERROR;
        }
        catch ( RegistryException e )
        {
            addActionError( "Configuration Registry Exception: " + e.getMessage() );
            result = ERROR;
        }
        catch ( RoleManagerException e )
        {
            addActionError( "Security role creation Exception: " + e.getMessage() );
            result = ERROR;
        }

        return result;
    }

    protected abstract boolean validateFields( Configuration config );

    protected abstract void addRepository( T repository, Configuration configuration )
        throws IOException, RoleManagerException;

    protected abstract void removeRepository( String repoId, Configuration configuration );
}
