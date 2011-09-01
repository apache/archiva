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

import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;

/**
 * Abstract ManagedRepositories Action.
 * <p/>
 * Place for all generic methods used in Managed Repository Administration.
 *
 * @version $Id$
 */
public abstract class AbstractManagedRepositoriesAction
    extends AbstractRepositoriesAdminAction
{


    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    public static final String CONFIRM = "confirm";

    
    public void setRepositoryTaskScheduler( RepositoryArchivaTaskScheduler repositoryTaskScheduler )
    {
        this.repositoryTaskScheduler = repositoryTaskScheduler;
    }

    protected void addRepository( ManagedRepositoryConfiguration repository, Configuration configuration )
        throws IOException
    {
        // Normalize the path
        File file = new File( repository.getLocation() );
        repository.setLocation( file.getCanonicalPath() );
        if ( !file.exists() )
        {
            file.mkdirs();
        }
        if ( !file.exists() || !file.isDirectory() )
        {
            throw new IOException(
                "Unable to add repository - no write access, can not create the root directory: " + file );
        }

        configuration.addManagedRepository( repository );

    }
}
