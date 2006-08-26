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

import org.apache.maven.repository.configuration.AbstractRepositoryConfiguration;
import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.configuration.RepositoryConfiguration;
import org.codehaus.plexus.util.FileUtils;

import java.io.IOException;

/**
 * Configures the application repositories.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="deleteRepositoryAction"
 */
public class DeleteRepositoryAction
    extends AbstractDeleteRepositoryAction
{
    protected AbstractRepositoryConfiguration getRepository( Configuration configuration )
    {
        return configuration.getRepositoryById( repoId );
    }

    protected void removeRepository( Configuration configuration, AbstractRepositoryConfiguration existingRepository )
    {
        configuration.removeRepository( (RepositoryConfiguration) existingRepository );
    }

    protected void removeContents( AbstractRepositoryConfiguration existingRepository )
        throws IOException
    {
        RepositoryConfiguration repository = (RepositoryConfiguration) existingRepository;
        getLogger().info( "Removing " + repository.getDirectory() );
        FileUtils.deleteDirectory( repository.getDirectory() );
    }
}
