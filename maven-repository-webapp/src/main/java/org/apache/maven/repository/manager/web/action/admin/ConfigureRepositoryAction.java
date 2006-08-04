package org.apache.maven.repository.manager.web.action.admin;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.opensymphony.webwork.interceptor.ParameterAware;
import com.opensymphony.xwork.ActionSupport;
import com.opensymphony.xwork.ModelDriven;
import com.opensymphony.xwork.Preparable;
import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.configuration.ConfigurationChangeException;
import org.apache.maven.repository.configuration.ConfigurationStore;
import org.apache.maven.repository.configuration.ConfigurationStoreException;
import org.apache.maven.repository.configuration.InvalidConfigurationException;
import org.apache.maven.repository.configuration.RepositoryConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Configures the application repositories.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="configureRepositoryAction"
 */
public class ConfigureRepositoryAction
    extends ActionSupport
    implements ModelDriven, Preparable, ParameterAware
{
    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    /**
     * The repository.
     */
    private RepositoryConfiguration repository;

    /**
     * The repository ID to lookup when editing a repository.
     */
    private String repoId;

    /**
     * The previously read configuration.
     */
    private Configuration configuration;

    public String add()
        throws IOException, ConfigurationStoreException, InvalidConfigurationException, ConfigurationChangeException
    {
        // TODO: if this didn't come from the form, go to configure.action instead of going through with re-saving what was just loaded

        RepositoryConfiguration existingRepository = configuration.getRepositoryById( repository.getId() );
        if ( existingRepository != null )
        {
            addFieldError( "id", "A repository with that id already exists" );
            return INPUT;
        }

        return addRepository();
    }

    public String edit()
        throws IOException, ConfigurationStoreException, InvalidConfigurationException, ConfigurationChangeException
    {
        // TODO: if this didn't come from the form, go to configure.action instead of going through with re-saving what was just loaded

        RepositoryConfiguration existingRepository = configuration.getRepositoryById( repository.getId() );
        configuration.removeRepository( existingRepository );

        return addRepository();
    }

    private String addRepository()
        throws IOException, ConfigurationStoreException, InvalidConfigurationException, ConfigurationChangeException
    {
        normalizeRepository();

        // Just double checking that our validation routines line up with what is expected in the configuration
        assert repository.isValid();

        configuration.addRepository( repository );

        configurationStore.storeConfiguration( configuration );

        // TODO: do we need to check if indexing is needed?

        addActionMessage( "Successfully saved configuration" );

        return SUCCESS;
    }

    private void normalizeRepository()
        throws IOException
    {
        // Normalize the path
        File file = new File( repository.getDirectory() );
        repository.setDirectory( file.getCanonicalPath() );
        if ( !file.exists() )
        {
            file.mkdirs();
            // TODO: error handling when this fails, or is not a directory
        }
    }

    public String input()
    {
        return INPUT;
    }

    public Object getModel()
    {
        if ( repository == null )
        {
            repository = configuration.getRepositoryById( repoId );
        }
        if ( repository == null )
        {
            repository = new RepositoryConfiguration();
            repository.setIndexed( false );
        }
        return repository;
    }

    public void prepare()
        throws ConfigurationStoreException
    {
        configuration = configurationStore.getConfigurationFromStore();
    }

    public String getRepoId()
    {
        return repoId;
    }

    public void setRepoId( String repoId )
    {
        this.repoId = repoId;
    }

    public void setParameters( Map map )
    {
        // TODO! can I replace with repository.id or something?
        if ( map.containsKey( "repoId" ) )
        {
            repoId = ( (String[]) map.get( "repoId" ) )[0];
        }
    }
}
