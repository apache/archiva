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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork.ModelDriven;
import com.opensymphony.xwork.Preparable;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.admin.models.AdminRepositoryConfiguration;
import org.codehaus.plexus.rbac.profile.RoleProfileException;
import org.codehaus.plexus.rbac.profile.RoleProfileManager;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.security.rbac.RbacManagerException;
import org.codehaus.plexus.security.rbac.Resource;
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
    implements ModelDriven, Preparable, SecureAction
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement role-hint="archiva"
     */
    private RoleProfileManager roleProfileManager;

    /**
     * The repository.
     */
    private AdminRepositoryConfiguration repository;

    /**
     * The repository ID to lookup when editing a repository.
     */
    private String repoId;

    /**
     * The previously read configuration.
     */
    private Configuration configuration;

    public String add()
        throws IOException, InvalidConfigurationException, RbacManagerException, RoleProfileException,
        RegistryException
    {
        // TODO: if this didn't come from the form, go to configure.action instead of going through with re-saving what was just loaded
        getLogger().info( ".add()" );

        AdminRepositoryConfiguration existingRepository = getRepository( repository.getId() );
        if ( existingRepository != null )
        {
            addFieldError( "id", "A repository with that id already exists" );
            return INPUT;
        }

        return saveConfiguration();
    }

    public String edit()
        throws IOException, InvalidConfigurationException, RbacManagerException, RoleProfileException,
        RegistryException
    {
        // TODO: if this didn't come from the form, go to configure.action instead of going through with re-saving what was just loaded
        getLogger().info( ".edit()" );

        if ( StringUtils.isBlank( repository.getId() ) )
        {
            addFieldError( "id", "A repository with a blank id cannot be editted." );
            return INPUT;
        }

        removeRepository( getRepository() );

        addRepository();

        return saveConfiguration();
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }

    public Object getModel()
    {
        getLogger().info( ".getModel()" );
        if( repository == null )
        {
            repository = createRepository();
        }
        
        return repository;
    }

    public String getRepoId()
    {
        return repoId;
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );

        if ( getRepoId() != null )
        {
            // TODO: this is not right. It needs to change based on method. But is this really the right way to restrict this area?
            // TODO: not right. We only care about this permission on managed repositories. Otherwise, it's configuration
            bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_EDIT_REPOSITORY, getRepoId() );
        }
        else
        {
            bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );
        }

        return bundle;
    }

    public String input()
    {
        getLogger().info( ".input()" );
        return INPUT;
    }
    
    public String doDefault()
        throws Exception
    {
        getLogger().info( ".doDefault()" );
        return super.doDefault();
    }
    
    public String doInput()
        throws Exception
    {
        getLogger().info( ".doInput()" );
        return super.doInput();
    }
    
    public void validate()
    {
        getLogger().info( ".validate()" );
        // super.validate();
    }
    
    public String execute()
        throws Exception
    {
        getLogger().info( ".execute()" );
        return super.execute();
    }

    public void prepare()
    {
        getLogger().info( ".prepare()" );
        configuration = archivaConfiguration.getConfiguration();

        if ( repository == null )
        {
            repository = getRepository( repoId );
        }
        if ( repository == null )
        {
            repository = createRepository();
        }
    }

    public void setRepoId( String repoId )
    {
        this.repoId = repoId;
    }
    
    private void addRepository()
        throws IOException, RoleProfileException
    {
        getLogger().info( ".addRepository()" );
        AdminRepositoryConfiguration repository = (AdminRepositoryConfiguration) getRepository();

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

        configuration.addRepository( repository );

        // TODO: double check these are configured on start up
        roleProfileManager.getDynamicRole( "archiva-repository-manager", repository.getId() );

        roleProfileManager.getDynamicRole( "archiva-repository-observer", repository.getId() );
    }

    private AdminRepositoryConfiguration createRepository()
    {
        getLogger().info( ".createRepository()" );
        AdminRepositoryConfiguration repository = new AdminRepositoryConfiguration();
        repository.setIndexed( false );
        return repository;
    }

    private AdminRepositoryConfiguration getRepository()
    {
        return repository;
    }

    private AdminRepositoryConfiguration getRepository( String id )
    {
        getLogger().info( ".getRepository(" + id + ")" );

        RepositoryConfiguration repoconfig = configuration.findRepositoryById( id );
        if ( repoconfig == null )
        {
            return createRepository();
        }
        return new AdminRepositoryConfiguration( repoconfig );
    }

    private boolean removeRepository( RepositoryConfiguration existingRepository )
    {
        getLogger().info( ".removeRepository()" );

        RepositoryConfiguration toremove = configuration.findRepositoryById( existingRepository.getId() );
        if ( toremove != null )
        {
            configuration.removeRepository( toremove );
            return true;
        }
        
        return false;
    }

    private String saveConfiguration()
        throws IOException, InvalidConfigurationException, RbacManagerException, RoleProfileException,
        RegistryException
    {
        getLogger().info( ".saveConfiguration()" );
        addRepository();

        archivaConfiguration.save( configuration );

        // TODO: do we need to check if indexing is needed?

        addActionMessage( "Successfully saved configuration" );

        return SUCCESS;
    }
}
