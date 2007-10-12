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
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.UserAssignment;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;

import java.io.File;
import java.io.IOException;

/**
 * Abstract ManagedRepositories Action.
 * 
 * Place for all generic methods used in Managed Repository Administration.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractManagedRepositoriesAction
    extends AbstractRepositoriesAdminAction
{
    /**
     * @plexus.requirement role-hint="default"
     */
    protected RoleManager roleManager;
    
    /**
     * @plexus.requirement role-hint="cached"
     */
    protected RBACManager rbacManager;

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
        String repoId = newRepository.getId();
        
        // TODO: double check these are configured on start up
        // TODO: belongs in the business logic
        
        if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) )
        {
            roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId );
        }

        if ( !roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) )
        {
            roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId );
        }

        try
        {
            UserAssignment ua = rbacManager.getUserAssignment( ArchivaRoleConstants.GUEST_ROLE );
            ua.addRoleName( ArchivaRoleConstants.REPOSITORY_OBSERVER_ROLE_PREFIX + " - " + repoId );
            rbacManager.saveUserAssignment( ua );
        }
        catch ( RbacManagerException e )
        {
            getLogger().warn( "Unable to add role [" + ArchivaRoleConstants.REPOSITORY_OBSERVER_ROLE_PREFIX + " - "
                              + repoId + "] to Guest user.", e );
        }
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
        String repoId = existingRepository.getId();
        
        if ( roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) )
        {
            roleManager.removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId );
        }
        
        if ( roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) )
        {
            roleManager.removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId );
        }

        getLogger().debug( "removed user roles associated with repository " + repoId );
    }
}
