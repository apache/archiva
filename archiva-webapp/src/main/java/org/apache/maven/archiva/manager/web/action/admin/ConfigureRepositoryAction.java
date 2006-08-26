package org.apache.maven.archiva.manager.web.action.admin;

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

import org.apache.maven.archiva.configuration.AbstractRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Configures the application repositories.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="configureRepositoryAction"
 */
public class ConfigureRepositoryAction
    extends AbstractConfigureRepositoryAction
{
    protected void removeRepository( AbstractRepositoryConfiguration existingRepository )
    {
        configuration.removeRepository( (RepositoryConfiguration) existingRepository );
    }

    protected AbstractRepositoryConfiguration getRepository( String id )
    {
        return configuration.getRepositoryById( id );
    }

    protected void addRepository()
        throws IOException
    {
        RepositoryConfiguration repository = (RepositoryConfiguration) getRepository();

        // Normalize the path
        File file = new File( repository.getDirectory() );
        repository.setDirectory( file.getCanonicalPath() );
        if ( !file.exists() )
        {
            file.mkdirs();
            // TODO: error handling when this fails, or is not a directory
        }

        configuration.addRepository( repository );
    }

    protected AbstractRepositoryConfiguration createRepository()
    {
        RepositoryConfiguration repository = new RepositoryConfiguration();
        repository.setIndexed( false );
        return repository;
    }
}
