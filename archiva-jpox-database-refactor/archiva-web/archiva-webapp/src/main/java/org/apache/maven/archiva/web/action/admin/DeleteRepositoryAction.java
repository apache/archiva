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

import org.apache.maven.archiva.configuration.AbstractRepositoryConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.codehaus.plexus.rbac.profile.RoleProfileException;
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

        try
        {
            removeRepositoryRoles( existingRepository );
        }
        catch ( RoleProfileException e )
        {
            getLogger().error( "Error removing user roles associated with repository " + existingRepository.getId() );
        }
    }

    protected void removeContents( AbstractRepositoryConfiguration existingRepository )
        throws IOException
    {
        RepositoryConfiguration repository = (RepositoryConfiguration) existingRepository;
        getLogger().info( "Removing " + repository.getDirectory() );
        FileUtils.deleteDirectory( repository.getDirectory() );
    }

    /**
     * Remove user roles associated with the repository
     *
     * @param existingRepository
     * @throws RoleProfileException
     */
    private void removeRepositoryRoles( AbstractRepositoryConfiguration existingRepository )
        throws RoleProfileException
    {
        roleProfileManager.deleteDynamicRole( "archiva-repository-manager", existingRepository.getId() );
        roleProfileManager.deleteDynamicRole( "archiva-repository-observer", existingRepository.getId() );

        getLogger().info( "removed user roles associated with repository " + existingRepository.getId() );
    }
}
