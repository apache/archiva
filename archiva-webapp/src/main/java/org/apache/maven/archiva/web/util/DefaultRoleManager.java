package org.apache.maven.archiva.web.util;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.archiva.web.ArchivaSecurityDefaults;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.security.rbac.Permission;
import org.codehaus.plexus.security.rbac.RBACManager;
import org.codehaus.plexus.security.rbac.RbacObjectNotFoundException;
import org.codehaus.plexus.security.rbac.RbacStoreException;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.rbac.Role;
import org.codehaus.plexus.security.rbac.UserAssignment;
import org.codehaus.plexus.security.user.User;
import org.codehaus.plexus.security.user.UserManager;
import org.codehaus.plexus.security.user.UserManagerListener;
import org.codehaus.plexus.util.StringUtils;

/**
 * DefaultRoleManager:
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @version $Id:$
 * @plexus.component role="org.apache.maven.archiva.web.util.RoleManager"
 * role-hint="default"
 */
public class DefaultRoleManager
    extends AbstractLogEnabled
    implements RoleManager, UserManagerListener, Initializable
{
    /**
     * @plexus.requirement
     */
    private UserManager userManager;

    /**
     * @plexus.requirement
     */
    private RBACManager manager;
    
    /**
     * @plexus.requirement
     */
    private ArchivaSecurityDefaults archivaSecurity;

    private boolean initialized;
                           
    public void initialize()
        throws InitializationException
    {
        archivaSecurity.ensureDefaultsExist();
        userManager.addUserManagerListener( this );
        initialized = true;
    }

    public void addUser( String principal )
        throws RbacStoreException
    {
        // make the resource
        Resource usernameResource = manager.createResource( principal );
        manager.saveResource( usernameResource );

        Permission editUser = manager.createPermission( "Edit Myself - " + principal, "edit-user", principal );
        editUser = manager.savePermission( editUser );

        // todo this one role a user will go away when we have expressions in the resources
        String personalRoleName = "Personal Role - " + principal;
        Role userRole = manager.createRole( personalRoleName );
        userRole.addPermission( editUser );
        userRole = manager.saveRole( userRole );

        UserAssignment assignment = manager.createUserAssignment( principal );
        assignment.addRoleName( personalRoleName );
        manager.saveUserAssignment( assignment );
    }

    /**
     * helper method for just creating an admin user assignment
     *
     * @param principal
     * @throws RbacStoreException
     * @throws RbacObjectNotFoundException
     */
    public void addAdminUser( String principal )
        throws RbacStoreException
    {
        UserAssignment assignment = manager.createUserAssignment( principal );
        assignment.addRoleName( ArchivaSecurityDefaults.SYSTEM_ADMINISTRATOR );
        manager.saveUserAssignment( assignment );
    }

    public void addRepository( String repositoryName )
        throws RbacStoreException
    {
        try
        {
            // make the resource
            Resource repoResource = manager.createResource( repositoryName );
            repoResource = manager.saveResource( repoResource );

            // make the permissions
            Permission editRepo = manager.createPermission( ArchivaSecurityDefaults.REPOSITORY_EDIT + " - " + repositoryName );
            editRepo.setOperation( manager.getOperation( ArchivaSecurityDefaults.REPOSITORY_EDIT_OPERATION ) );
            editRepo.setResource( repoResource );
            editRepo = manager.savePermission( editRepo );

            Permission deleteRepo = manager.createPermission( ArchivaSecurityDefaults.REPOSITORY_DELETE + " - " + repositoryName );
            deleteRepo.setOperation( manager.getOperation( ArchivaSecurityDefaults.REPOSITORY_DELETE_OPERATION ) );
            deleteRepo.setResource( repoResource );
            deleteRepo = manager.savePermission( deleteRepo );
            
            Permission accessRepo = manager.createPermission( ArchivaSecurityDefaults.REPOSITORY_ACCESS + " - " + repositoryName );
            accessRepo.setOperation( manager.getOperation( ArchivaSecurityDefaults.REPOSITORY_ACCESS_OPERATION ) );
            accessRepo.setResource( repoResource );
            accessRepo = manager.savePermission( accessRepo );
            
            Permission uploadRepo = manager.createPermission( ArchivaSecurityDefaults.REPOSITORY_UPLOAD + " - " + repositoryName );
            uploadRepo.setOperation( manager.getOperation( ArchivaSecurityDefaults.REPOSITORY_UPLOAD_OPERATION ) );
            uploadRepo.setResource( repoResource );
            uploadRepo = manager.savePermission( uploadRepo );

            // make the roles
            Role repositoryObserver = manager.createRole( "Repository Observer - " + repositoryName );
            repositoryObserver.addPermission( manager.getPermission( ArchivaSecurityDefaults.REPORTS_ACCESS_PERMISSION ) );
            repositoryObserver.setAssignable( true );
            repositoryObserver = manager.saveRole( repositoryObserver );

            Role repositoryManager = manager.createRole( "Repository Manager - " + repositoryName );
            repositoryManager.addPermission( editRepo );
            repositoryManager.addPermission( deleteRepo );
            repositoryManager.addPermission( accessRepo );
            repositoryManager.addPermission( uploadRepo );
            repositoryManager.addPermission( manager.getPermission( ArchivaSecurityDefaults.REPORTS_GENERATE_PERMISSION ) );
            repositoryManager.addChildRoleName( repositoryObserver.getName() );
            repositoryManager.setAssignable( true );
            manager.saveRole( repositoryManager );
        }
        catch ( RbacObjectNotFoundException ne )
        {
            throw new RbacStoreException( "rbac object not found in repo role creation", ne );
        }
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    public void setInitialized( boolean initialized )
    {
        this.initialized = initialized;
    }

    public void userManagerInit( boolean freshDatabase )
    {
        // no-op
    }

    public void userManagerUserAdded( User user )
    {
        if ( !StringUtils.equals( ADMIN_USERNAME, user.getUsername() ) )
        {
            // We have a non-admin user.
            String principal = user.getPrincipal().toString();
            
            // Add the personal (dynamic) roles.
            addUser( principal );
            
            // Add the guest (static) role.
            try
            {
                Role guestRole = manager.getRole( ArchivaSecurityDefaults.GUEST_ROLE );
                guestRole = manager.saveRole( guestRole );

                UserAssignment assignment = manager.createUserAssignment( principal );
                assignment.addRoleName( guestRole.getName() );
                manager.saveUserAssignment( assignment );
            }
            catch ( RbacStoreException e )
            {
                getLogger().error( "Unable to add guest role to new user " + user.getUsername() + ".", e );
            }
            catch ( RbacObjectNotFoundException e )
            {
                getLogger().error( "Unable to add guest role to new user " + user.getUsername() + ".", e );
            }
        }
    }

    public void userManagerUserRemoved( User user )
    {
        // TODO: Should remove the personal (dynamic) roles for this user too.
    }

    public void userManagerUserUpdated( User user )
    {
        // no-op
    }
}
