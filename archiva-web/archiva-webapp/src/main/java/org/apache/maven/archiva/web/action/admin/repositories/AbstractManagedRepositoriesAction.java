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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;

import java.io.File;
import java.io.IOException;

/**
 * Abstract ManagedRepositories Action.
 * 
 * Place for all generic methods used in Managed Repository Administration.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractManagedRepositoriesAction
    extends AbstractRepositoriesAdminAction
{
    /**
     * @plexus.requirement role-hint="default"
     */
    protected RoleManager roleManager;

    public RoleManager getRoleManager()
    {
        return roleManager;
    }

    public void setRoleManager( RoleManager roleManager )
    {
        this.roleManager = roleManager;
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
            throw new IOException( "unable to add repository - can not create the root directory: " + file );
        }

        configuration.addManagedRepository( repository );

    }

    protected void addRepositoryRoles( ManagedRepositoryConfiguration newRepository ) throws RoleManagerException
    {
        // TODO: double check these are configured on start up
        // TODO: belongs in the business logic
        roleManager.createTemplatedRole( "archiva-repository-manager", newRepository.getId() );
        roleManager.createTemplatedRole( "archiva-repository-observer", newRepository.getId() );
    }

    protected void removeContents( ManagedRepositoryConfiguration existingRepository )
        throws IOException
    {
        FileUtils.deleteDirectory( new File( existingRepository.getLocation() ) );
    }

    protected void removeRepository( String repoId, Configuration configuration )
    {
        ManagedRepositoryConfiguration toremove = configuration.findManagedRepositoryById( repoId );
        if ( toremove != null )
        {
            configuration.removeManagedRepository( toremove );
        }
    }

    protected void removeRepositoryRoles( ManagedRepositoryConfiguration existingRepository )
        throws RoleManagerException
    {
        roleManager.removeTemplatedRole( "archiva-repository-manager", existingRepository.getId() );
        roleManager.removeTemplatedRole( "archiva-repository-observer", existingRepository.getId() );

        getLogger().debug( "removed user roles associated with repository " + existingRepository.getId() );
    }
}
