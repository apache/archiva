package org.apache.maven.archiva.security;

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

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.redback.rbac.Operation;
import org.codehaus.plexus.redback.rbac.Permission;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.rbac.Role;
import org.codehaus.plexus.redback.rbac.UserAssignment;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;

import java.util.List;

/**
 * DefaultUserRepositoriesTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DefaultUserRepositoriesTest
    extends PlexusTestCase
{
    private static final String PERMISSION_READ_REPOSITORY = "Archiva Read Repository";

    private static final String USER_GUEST = "guest";

    private static final String USER_ADMIN = "admin";

    private static final String USER_ALPACA = "alpaca";

    private SecuritySystem securitySystem;

    private RBACManager rbacManager;

    private RoleManager roleManager;

    public void testGetObservableRepositoryIds()
        throws Exception
    {
        UserRepositories userRepos = (UserRepositories) lookup( UserRepositories.class, "default" );
        assertNotNull( userRepos );

        // create some users.
        createUser( USER_ALPACA, "Al 'Archiva' Paca" );

        assertEquals( "Expected users", 3, securitySystem.getUserManager().getUsers().size() );

        // some unassigned repo observer roles.
        userRepos.createMissingRepositoryRoles( "central" );
        userRepos.createMissingRepositoryRoles( "coporate" );
        userRepos.createMissingRepositoryRoles( "internal" );
        userRepos.createMissingRepositoryRoles( "snapshots" );
        userRepos.createMissingRepositoryRoles( "secret" );

        // some assigned repo observer roles.
        assignRepositoryObserverRole( USER_ALPACA, "central" );
        assignRepositoryObserverRole( USER_ALPACA, "corporate" );
        assignRepositoryObserverRole( USER_GUEST, "corporate" );
        // the global repo observer role.
        assignGlobalRepositoryObserverRole( USER_ADMIN );

        assertRepoIds( new String[] { "central", "corporate" }, userRepos.getObservableRepositoryIds( USER_ALPACA ) );
        assertRepoIds( new String[] { "coporate" }, userRepos.getObservableRepositoryIds( USER_GUEST ) );
        assertRepoIds( new String[] { "central", "internal", "corporate", "snapshots", "secret" }, userRepos
            .getObservableRepositoryIds( USER_ADMIN ) );
    }

    private void assertRepoIds( String[] expectedRepoIds, List<String> observableRepositoryIds )
    {
        assertNotNull( "Observable Repository Ids cannot be null.", observableRepositoryIds );

        if ( expectedRepoIds.length != observableRepositoryIds.size() )
        {
            fail( "Size of Observable Repository Ids wrong, expected <" + expectedRepoIds.length + "> but got <"
                + observableRepositoryIds.size() + "> instead. \nExpected: [" + StringUtils.join( expectedRepoIds, "," )
                + "]\nActual: [" + StringUtils.join( observableRepositoryIds.iterator(), "," ) + "]" );
        }
    }

    private void assignGlobalRepositoryObserverRole( String principal )
        throws Exception
    {
        Role role = createRepositoryObserverRole( ArchivaRoleConstants.GLOBAL_REPOSITORY_OBSERVER_ROLE,
                                                  PERMISSION_READ_REPOSITORY, Resource.GLOBAL );
        assignRole( principal, role );
    }

    private void assignRepositoryObserverRole( String principal, String repoId )
        throws Exception
    {
        // String roleId = ArchivaRoleConstants.toRepositoryObserverRoleId( repoId );
        String roleId = ArchivaRoleConstants.toRepositoryObserverRoleName( repoId );
        roleManager.assignRole( roleId, principal );
        
//        Role role = createRepositoryObserverRole( roleName, PERMISSION_READ_REPOSITORY, repoId );
//        assertEquals( roleName, role.getName() );
//        assignRole( principal, role );
    }

    private void assignRole( String principal, Role role )
        throws Exception
    {
        UserAssignment ua;

        if ( rbacManager.userAssignmentExists( principal ) )
        {
            ua = rbacManager.getUserAssignment( principal );
        }
        else
        {
            ua = rbacManager.createUserAssignment( principal );
        }

        ua.addRoleName( role );

        rbacManager.saveUserAssignment( ua );
    }

    private void createRepositoryObserverRole( String repoId )
        throws Exception
    {
        createRepositoryObserverRole( ArchivaRoleConstants.toRepositoryObserverRoleName( repoId ),
                                      PERMISSION_READ_REPOSITORY + "-" + repoId, repoId );
    }

    private Role createRepositoryObserverRole( String roleName, String permissionName, String resourceId )
        throws Exception
    {
        if ( rbacManager.roleExists( roleName ) )
        {
            return rbacManager.getRole( roleName );
        }

        Permission perm;
        Operation operationRepoAccess;
        Resource resource;

        //        if ( rbacManager.resourceExists( resourceId ) )
        //        {
        //            resource = rbacManager.getResource( resourceId );
        //        }
        //        else
        //        {
        //            resource = rbacManager.createResource( resourceId );
        //        }
        resource = rbacManager.createResource( resourceId );

        //        if ( rbacManager.operationExists( ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS ) )
        //        {
        //            operationRepoAccess = rbacManager.getOperation( ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS );
        //        }
        //        else
        //        {
        //            operationRepoAccess = rbacManager.createOperation( ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS );
        //        }
        operationRepoAccess = rbacManager.createOperation( ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS );

        //        if ( rbacManager.permissionExists( permissionName ) )
        //        {
        //            perm = rbacManager.getPermission( permissionName );
        //        }
        //        else
        //        {
        //            perm = rbacManager.createPermission( permissionName );
        //        }
        perm = rbacManager.createPermission( permissionName );
        perm.setOperation( operationRepoAccess );
        perm.setResource( resource );

        Role role = rbacManager.createRole( roleName );
        role.addPermission( perm );

        rbacManager.saveOperation( operationRepoAccess );
        rbacManager.savePermission( perm );
        rbacManager.saveRole( role );

        return role;
    }

    private User createUser( String principal, String fullname )
    {
        UserManager userManager = securitySystem.getUserManager();

        User user = userManager.createUser( principal, fullname, principal + "@testable.archiva.apache.org" );
        securitySystem.getPolicy().setEnabled( false );
        userManager.addUser( user );
        securitySystem.getPolicy().setEnabled( true );

        return user;
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        securitySystem = (SecuritySystem) lookup( SecuritySystem.class, "testable" );
        rbacManager = (RBACManager) lookup( RBACManager.class, "memory" );
        roleManager = (RoleManager) lookup( RoleManager.class, "default" );
        
        // Setup Admin User.
        User adminUser = createUser( USER_ADMIN, "Admin User" );
        roleManager.assignRole( ArchivaRoleConstants.TEMPLATE_SYSTEM_ADMIN, adminUser.getPrincipal().toString() );

        // Setup Guest User.
        User guestUser = createUser( USER_GUEST, "Guest User" );
        roleManager.assignRole( ArchivaRoleConstants.TEMPLATE_GUEST, guestUser.getPrincipal().toString() );
    }
}
