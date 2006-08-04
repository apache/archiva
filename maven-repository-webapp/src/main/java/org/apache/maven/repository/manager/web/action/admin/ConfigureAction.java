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

import com.opensymphony.xwork.ActionSupport;
import com.opensymphony.xwork.ModelDriven;
import com.opensymphony.xwork.Preparable;
import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.configuration.ConfigurationChangeException;
import org.apache.maven.repository.configuration.ConfigurationStore;
import org.apache.maven.repository.configuration.ConfigurationStoreException;
import org.apache.maven.repository.configuration.InvalidConfigurationException;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchException;

import java.io.File;
import java.io.IOException;

/**
 * Configures the application.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="configureAction"
 */
public class ConfigureAction
    extends ActionSupport
    implements ModelDriven, Preparable
{
    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    /**
     * The configuration.
     */
    private Configuration configuration;

    public String execute()
        throws IOException, RepositoryIndexException, RepositoryIndexSearchException, ConfigurationStoreException,
        InvalidConfigurationException, ConfigurationChangeException
    {
        // TODO: if this didn't come from the form, go to configure.action instead of going through with re-saving what was just loaded
        // TODO: if this is changed, do we move the index or recreate it?

        // Normalize the path
        File file = new File( configuration.getIndexPath() );
        configuration.setIndexPath( file.getCanonicalPath() );
        if ( !file.exists() )
        {
            file.mkdirs();
            // TODO: error handling when this fails, or is not a directory
        }

        // Just double checking that our validation routines line up with what is expected in the configuration
        assert configuration.isValid();

        configurationStore.storeConfiguration( configuration );

        // TODO: if the repository has changed, we need to check if indexing is needed

        addActionMessage( "Successfully saved configuration" );

        return SUCCESS;
    }

    public String input()
    {
        return INPUT;
    }

    public Object getModel()
    {
        return configuration;
    }

    public void prepare()
        throws ConfigurationStoreException
    {
        configuration = configurationStore.getConfigurationFromStore();
    }
}