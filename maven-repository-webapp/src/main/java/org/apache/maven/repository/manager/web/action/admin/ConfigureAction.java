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
import org.apache.maven.repository.configuration.ConfigurationStore;
import org.apache.maven.repository.configuration.ConfigurationStoreException;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchException;
import org.codehaus.plexus.util.StringUtils;

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
        throws IOException, RepositoryIndexException, RepositoryIndexSearchException, ConfigurationStoreException
    {
        // TODO: if this didn't come from the form, go to configure.action instead of going through with re-saving what was just loaded

        // Normalize the path
        File file = new File( configuration.getRepositoryDirectory() );
        configuration.setRepositoryDirectory( file.getCanonicalPath() );
        if ( !file.exists() )
        {
            file.mkdirs();
            // TODO: error handling when this fails
        }

        // TODO: these defaults belong in the model. They shouldn't be stored here, as you want them to re-default
        // should the repository change even if these didn't

        // TODO: these should be on an advanced configuration form, not the standard one
        if ( StringUtils.isEmpty( configuration.getIndexPath() ) )
        {
            configuration.setIndexPath(
                new File( configuration.getRepositoryDirectory(), ".index" ).getAbsolutePath() );
        }
        if ( StringUtils.isEmpty( configuration.getMinimalIndexPath() ) )
        {
            configuration.setMinimalIndexPath(
                new File( configuration.getRepositoryDirectory(), ".index-minimal" ).getAbsolutePath() );
        }

        // Just double checking that our validation routines line up with what is expected in the configuration
        assert configuration.isValid();

        configurationStore.storeConfiguration( configuration );

        addActionMessage( "Successfully saved configuration" );

        return SUCCESS;
    }

    public String doInput()
    {
        return INPUT;
    }

    public Object getModel()
    {
        return configuration;
    }

    public void prepare()
        throws Exception
    {
        configuration = configurationStore.getConfigurationFromStore();
    }
}