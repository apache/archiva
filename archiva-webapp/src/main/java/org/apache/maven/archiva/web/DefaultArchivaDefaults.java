package org.apache.maven.archiva.web;

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

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.security.rbac.Operation;
import org.codehaus.plexus.security.rbac.Permission;
import org.codehaus.plexus.security.rbac.RBACManager;
import org.codehaus.plexus.security.rbac.RbacObjectNotFoundException;
import org.codehaus.plexus.security.rbac.Role;
import org.codehaus.plexus.security.user.User;
import org.codehaus.plexus.security.user.UserManager;
import org.codehaus.plexus.security.user.UserNotFoundException;
import org.codehaus.plexus.security.policy.UserSecurityPolicy;

/**
 * DefaultArchivaDefaults
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.web.ArchivaDefaults"
 */
public class DefaultArchivaDefaults
    extends AbstractLogEnabled
    implements ArchivaDefaults, Initializable
{
    /**
     * @plexus.requirement
     */
    private RBACManager rbacManager;

    /**
     * @plexus.requirement
     */
    private UserManager userManager;

    /**
     * @plexus.requirement
     */
    private UserSecurityPolicy securityPolicy;

    private boolean initialized = false;

    private User guestUser;

    public void ensureDefaultsExist()
    {
        if ( initialized )
        {
            return;
        }

        ensureOperationsExist();
        ensurePermissionsExist();
        ensureRolesExist();
        ensureUsersExist();

        initialized = true;
    }

    private void ensureOperationExists( String operationName )
    {
        if ( !rbacManager.operationExists( operationName ) )
        {
            Operation operation = rbacManager.createOperation( operationName );
            rbacManager.saveOperation( operation );
        }
    }

    private void ensureOperationsExist()
    {
        ensureOperationExists( REPOSITORY_ADD_OPERATION );
        ensureOperationExists( REPOSITORY_EDIT_OPERATION );
        ensureOperationExists( REPOSITORY_DELETE_OPERATION );
        ensureOperationExists( CONFIGURATION_EDIT_OPERATION );
        ensureOperationExists( INDEX_RUN_OPERATION );
        ensureOperationExists( INDEX_REGENERATE_OPERATION );
        ensureOperationExists( REPORTS_ACCESS_OPERATION );
        ensureOperationExists( REPORTS_GENERATE_OPERATION );
        ensureOperationExists( USER_EDIT_OPERATION );
        ensureOperationExists( USERS_EDIT_ALL_OPERATION );
        ensureOperationExists( ROLES_GRANT_OPERATION );
        ensureOperationExists( ROLES_REMOVE_OPERATION );
        ensureOperationExists( REPOSITORY_ACCESS_OPERATION );
        ensureOperationExists( REPOSITORY_UPLOAD_OPERATION );
    }

    private void ensurePermissionExists( String permissionName, String operationName, String resourceIdentifier )
    {
        if ( !rbacManager.permissionExists( permissionName ) )
        {
            Permission editConfiguration = rbacManager.createPermission( permissionName, operationName,
                                                                         resourceIdentifier );
            rbacManager.savePermission( editConfiguration );
        }
    }

    private void ensurePermissionsExist()
    {
        String globalResource = rbacManager.getGlobalResource().getIdentifier();

        ensurePermissionExists( USERS_EDIT_ALL_PERMISSION, USERS_EDIT_ALL_OPERATION, globalResource );

        ensurePermissionExists( CONFIGURATION_EDIT_PERMISSION, CONFIGURATION_EDIT_OPERATION, globalResource );

        ensurePermissionExists( ROLES_GRANT_PERMISSION, ROLES_GRANT_OPERATION, globalResource );
        ensurePermissionExists( ROLES_REMOVE_PERMISSION, ROLES_REMOVE_OPERATION, globalResource );

        ensurePermissionExists( REPORTS_ACCESS_PERMISSION, REPORTS_ACCESS_OPERATION, globalResource );
        ensurePermissionExists( REPORTS_GENERATE_PERMISSION, REPORTS_GENERATE_OPERATION, globalResource );

        ensurePermissionExists( INDEX_RUN_PERMISSION, INDEX_RUN_OPERATION, globalResource );
        ensurePermissionExists( INDEX_REGENERATE_PERMISSION, INDEX_REGENERATE_OPERATION, globalResource );

        ensurePermissionExists( REPOSITORY_ADD_PERMISSION, REPOSITORY_ADD_OPERATION, globalResource );
        ensurePermissionExists( REPOSITORY_ACCESS, "access-repository", globalResource );
        ensurePermissionExists( REPOSITORY_UPLOAD, REPOSITORY_UPLOAD_OPERATION, globalResource );
    }

    private void ensureRolesExist()
    {
        try
        {
            if ( !rbacManager.roleExists( USER_ADMINISTRATOR ) )
            {
                Role userAdmin = rbacManager.createRole( USER_ADMINISTRATOR );
                userAdmin.addPermission( rbacManager.getPermission( USERS_EDIT_ALL_PERMISSION ) );
                userAdmin.addPermission( rbacManager.getPermission( ROLES_REMOVE_PERMISSION ) );
                userAdmin.addPermission( rbacManager.getPermission( ROLES_GRANT_PERMISSION ) );
                userAdmin.setAssignable( true );
                rbacManager.saveRole( userAdmin );
            }

            if ( !rbacManager.roleExists( SYSTEM_ADMINISTRATOR ) )
            {
                Role admin = rbacManager.createRole( SYSTEM_ADMINISTRATOR );
                admin.addChildRoleName( rbacManager.getRole( USER_ADMINISTRATOR ).getName() );
                admin.addPermission( rbacManager.getPermission( CONFIGURATION_EDIT_PERMISSION ) );
                admin.addPermission( rbacManager.getPermission( INDEX_RUN_PERMISSION ) );
                admin.addPermission( rbacManager.getPermission( REPOSITORY_ADD_PERMISSION ) );
                admin.addPermission( rbacManager.getPermission( REPORTS_ACCESS_PERMISSION ) );
                admin.addPermission( rbacManager.getPermission( REPORTS_GENERATE_PERMISSION ) );
                admin.addPermission( rbacManager.getPermission( INDEX_REGENERATE_PERMISSION ) );
                admin.setAssignable( true );
                rbacManager.saveRole( admin );
            }
        }
        catch ( RbacObjectNotFoundException ne )
        {
            getLogger().fatalError( "Unable to initialize Roles!", ne );
            throw new RuntimeException( "All Mandatory Defaults do not Exist!" );
        }
    }

    public void ensureUsersExist()
    {
        if( !userManager.userExists( GUEST_USERNAME ))
        {
            securityPolicy.setEnabled( false );
            this.guestUser = userManager.createUser( GUEST_USERNAME, "Guest User", "" );
            this.guestUser = userManager.addUser( this.guestUser );
            securityPolicy.setEnabled( true );
        }
        else
        {
            try
            {
                this.guestUser = userManager.findUser( GUEST_USERNAME );
            }
            catch ( UserNotFoundException e )
            {
                throw new RuntimeException( "Unable to find user '" + GUEST_USERNAME + "'", e );
            }
        }
    }

    public User getGuestUser()
    {
        return this.guestUser;
    }

    public void initialize()
        throws InitializationException
    {
        ensureDefaultsExist();
    }
}
