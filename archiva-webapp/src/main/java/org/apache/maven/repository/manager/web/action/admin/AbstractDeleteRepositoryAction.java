package org.apache.maven.repository.manager.web.action.admin;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.repository.configuration.AbstractRepositoryConfiguration;
import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.configuration.ConfigurationChangeException;
import org.apache.maven.repository.configuration.ConfigurationStore;
import org.apache.maven.repository.configuration.ConfigurationStoreException;
import org.apache.maven.repository.configuration.InvalidConfigurationException;
import org.apache.maven.repository.configuration.RepositoryConfiguration;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.io.IOException;

/**
 * Base action for repository removal actions.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractDeleteRepositoryAction
    extends PlexusActionSupport
{
    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    /**
     * The repository ID to lookup when editing a repository.
     */
    protected String repoId;

    /**
     * Which operation to select.
     */
    private String operation = "unmodified";

    public String execute()
        throws ConfigurationStoreException, IOException, InvalidConfigurationException, ConfigurationChangeException
    {
        // TODO: if this didn't come from the form, go to configure.action instead of going through with re-saving what was just loaded

        if ( "delete-entry".equals( operation ) || "delete-contents".equals( operation ) )
        {
            Configuration configuration = configurationStore.getConfigurationFromStore();

            AbstractRepositoryConfiguration existingRepository = getRepository( configuration );
            if ( existingRepository == null )
            {
                addActionError( "A repository with that id does not exist" );
                return ERROR;
            }

            // TODO: remove from index too!

            removeRepository( configuration, existingRepository );

            configurationStore.storeConfiguration( configuration );

            if ( "delete-contents".equals( operation ) )
            {
                removeContents( existingRepository );
            }
        }

        return SUCCESS;
    }

    protected abstract void removeContents( AbstractRepositoryConfiguration existingRepository )
        throws IOException;

    protected abstract AbstractRepositoryConfiguration getRepository( Configuration configuration );

    protected abstract void removeRepository( Configuration configuration,
                                              AbstractRepositoryConfiguration existingRepository );

    public String input()
    {
        return INPUT;
    }

    public String getRepoId()
    {
        return repoId;
    }

    public void setRepoId( String repoId )
    {
        this.repoId = repoId;
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation( String operation )
    {
        this.operation = operation;
    }
}
