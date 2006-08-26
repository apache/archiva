package org.apache.maven.repository.manager.web.action.admin;

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

import com.opensymphony.xwork.ActionSupport;
import com.opensymphony.xwork.ModelDriven;
import com.opensymphony.xwork.Preparable;
import org.apache.maven.repository.configuration.AbstractRepositoryConfiguration;
import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.configuration.ConfigurationChangeException;
import org.apache.maven.repository.configuration.ConfigurationStore;
import org.apache.maven.repository.configuration.ConfigurationStoreException;
import org.apache.maven.repository.configuration.InvalidConfigurationException;

import java.io.IOException;

/**
 * Base action for repository configuration actions.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractConfigureRepositoryAction
    extends ActionSupport
    implements ModelDriven, Preparable
{
    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

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
        throws IOException, ConfigurationStoreException, InvalidConfigurationException, ConfigurationChangeException
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
        throws IOException, ConfigurationStoreException, InvalidConfigurationException, ConfigurationChangeException
    {
        // TODO: if this didn't come from the form, go to configure.action instead of going through with re-saving what was just loaded

        AbstractRepositoryConfiguration existingRepository = getRepository( repository.getId() );
        removeRepository( existingRepository );

        return saveConfiguration();
    }

    protected abstract void removeRepository( AbstractRepositoryConfiguration existingRepository );

    protected abstract AbstractRepositoryConfiguration getRepository( String id );

    private String saveConfiguration()
        throws IOException, ConfigurationStoreException, InvalidConfigurationException, ConfigurationChangeException
    {
        addRepository();

        configurationStore.storeConfiguration( configuration );

        // TODO: do we need to check if indexing is needed?

        addActionMessage( "Successfully saved configuration" );

        return SUCCESS;
    }

    protected abstract void addRepository()
        throws IOException;

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
}
