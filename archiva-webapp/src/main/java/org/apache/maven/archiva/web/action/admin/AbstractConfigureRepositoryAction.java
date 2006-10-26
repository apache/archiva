package org.apache.maven.archiva.web.action.admin;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.opensymphony.xwork.ModelDriven;
import com.opensymphony.xwork.Preparable;
import org.apache.maven.archiva.configuration.AbstractRepositoryConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationChangeException;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfigurationStoreException;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.rbac.profile.RoleProfileException;
import org.codehaus.plexus.rbac.profile.RoleProfileManager;
import org.codehaus.plexus.security.rbac.RbacManagerException;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.ui.web.interceptor.SecureAction;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionBundle;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.io.IOException;

/**
 * Base action for repository configuration actions.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractConfigureRepositoryAction
    extends PlexusActionSupport
    implements ModelDriven, Preparable, SecureAction
{
    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    /**
     * @plexus.requirement role-hint="archiva"
     */
    protected RoleProfileManager roleProfileManager;

    /**
     * The repository.
     */
    private AbstractRepositoryConfiguration repository;

    /**
     * The repository ID to lookup when editing a repository.
     */
    private String repoId;

    /**
     * The previously read configuration.
     */
    protected Configuration configuration;

    public String add()
        throws IOException, ConfigurationStoreException, InvalidConfigurationException, ConfigurationChangeException,
        RbacManagerException, RoleProfileException
    {
        // TODO: if this didn't come from the form, go to configure.action instead of going through with re-saving what was just loaded

        AbstractRepositoryConfiguration existingRepository = getRepository( repository.getId() );
        if ( existingRepository != null )
        {
            addFieldError( "id", "A repository with that id already exists" );
            return INPUT;
        }

        return saveConfiguration();
    }

    public String edit()
        throws IOException, ConfigurationStoreException, InvalidConfigurationException, ConfigurationChangeException,
        RbacManagerException, RoleProfileException
    {
        // TODO: if this didn't come from the form, go to configure.action instead of going through with re-saving what was just loaded

        AbstractRepositoryConfiguration existingRepository = getRepository( repository.getId() );
        removeRepository( existingRepository );

        return saveConfiguration();
    }

    protected abstract void removeRepository( AbstractRepositoryConfiguration existingRepository );

    protected abstract AbstractRepositoryConfiguration getRepository( String id );

    private String saveConfiguration()
        throws IOException, ConfigurationStoreException, InvalidConfigurationException, ConfigurationChangeException,
        RbacManagerException, RoleProfileException
    {
        addRepository();

        configurationStore.storeConfiguration( configuration );

        // TODO: do we need to check if indexing is needed?

        addActionMessage( "Successfully saved configuration" );

        return SUCCESS;
    }

    protected abstract void addRepository()
        throws IOException, RoleProfileException;

    public String input()
    {
        return INPUT;
    }

    public Object getModel()
    {
        return repository;
    }

    protected abstract AbstractRepositoryConfiguration createRepository();

    public void prepare()
        throws ConfigurationStoreException
    {
        configuration = configurationStore.getConfigurationFromStore();

        if ( repository == null )
        {
            repository = getRepository( repoId );
        }
        if ( repository == null )
        {
            repository = createRepository();
        }
    }

    public String getRepoId()
    {
        return repoId;
    }

    public void setRepoId( String repoId )
    {
        this.repoId = repoId;
    }

    protected AbstractRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public Configuration getConfiguration()
    {
        return configuration;
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
}
