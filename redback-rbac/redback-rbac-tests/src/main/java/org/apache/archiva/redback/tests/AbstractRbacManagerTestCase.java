package org.apache.archiva.redback.tests;

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

import junit.framework.TestCase;
import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RbacPermanentException;
import org.apache.archiva.redback.tests.utils.RBACDefaults;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.test.annotation.DirtiesContext;

/**
 * AbstractRbacManagerTestCase
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
public abstract class AbstractRbacManagerTestCase
    extends TestCase
{
    private RBACManager rbacManager;

    protected RbacManagerEventTracker eventTracker;

    private RBACDefaults rbacDefaults;

    private static final int EVENTCOUNT = 2;
    public void setRbacManager( RBACManager store )
    {
        this.rbacManager = store;
        if ( this.rbacManager != null )
        {
            this.eventTracker = new RbacManagerEventTracker();
            this.rbacManager.addListener( eventTracker );
        }
        rbacDefaults = new RBACDefaults( rbacManager );
    }

    public RBACManager getRbacManager()
    {
        return this.rbacManager;
    }

    public void setUp()
        throws Exception
    {
        super.setUp();
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    private Role getAdminRole()
        throws RbacManagerException
    {
        Role role = rbacManager.createRole( "ADMIN" );
        role.setAssignable( false );

        Permission perm = rbacManager.createPermission( "EDIT_ANY_USER", "EDIT", "User:*" );

        role.addPermission( perm );

        return role;
    }

    private Role getDeveloperRole()
        throws RbacManagerException
    {
        Role role = rbacManager.createRole( "DEVELOPER" );
        role.setAssignable( true );

        Permission perm = rbacManager.createPermission( "EDIT_MY_USER", "EDIT", "User:Self" );

        role.addPermission( perm );

        return role;
    }

    private Role getProjectAdminRole()
        throws RbacManagerException
    {
        Role role = rbacManager.createRole( "PROJECT_ADMIN" );
        role.setAssignable( true );

        Permission perm = rbacManager.createPermission( "EDIT_PROJECT", "EDIT", "Project:Foo" );

        role.addPermission( perm );

        return role;
    }

    private Role getSuperDeveloperRole()
    {
        Role role = rbacManager.createRole( "SUPER_DEVELOPER" );
        role.setAssignable( true );

        return role;
    }

    @Test
    public void testStoreInitialization()
        throws Exception
    {
        assertNotNull( rbacManager );

        Role role = getAdminRole();

        assertNotNull( role );

        Role added = rbacManager.saveRole( role );

        assertEquals( 1, rbacManager.getAllRoles().size() );

        assertNotNull( added );

        rbacManager.removeRole( added );

        assertEquals( 0, rbacManager.getAllRoles().size() );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        //assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 1, eventTracker.addedRoleNames.size() );
        assertEquals( 1, eventTracker.removedRoleNames.size() );
        assertEquals( 1, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testResources()
        throws Exception
    {
        assertNotNull( rbacManager );

        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );

        Resource resource = rbacManager.createResource( "foo" );
        Resource resource2 = rbacManager.createResource( "bar" );

        assertNotNull( resource );

        Resource added = rbacManager.saveResource( resource );
        assertNotNull( added );
        Resource added2 = rbacManager.saveResource( resource2 );
        assertNotNull( added2 );

        assertEquals( 2, rbacManager.getAllResources().size() );

        rbacManager.removeResource( added );

        assertEquals( 1, rbacManager.getAllResources().size() );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 0, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 0, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testAddGetPermission()
        throws RbacManagerException
    {
        assertNotNull( rbacManager );

        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );

        Role adminRole = rbacManager.saveRole( getAdminRole() );
        rbacManager.saveRole( getDeveloperRole() );

        assertEquals( 2, rbacManager.getAllRoles().size() );
        assertEquals( 2, rbacManager.getAllPermissions().size() );

        Permission createUserPerm = rbacManager.createPermission( "CREATE_USER", "CREATE", "User" );

        // perm shouldn't exist in manager (yet)
        assertEquals( 2, rbacManager.getAllPermissions().size() );

        adminRole.addPermission( createUserPerm );
        rbacManager.saveRole( adminRole );

        // perm should exist in manager now.
        assertEquals( 3, rbacManager.getAllPermissions().size() );
        Permission fetched = rbacManager.getPermission( "CREATE_USER" );
        assertNotNull( fetched );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 2, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 3, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testAddGetRole()
        throws RbacManagerException
    {
        assertNotNull( rbacManager );

        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );

        Role adminRole = rbacManager.saveRole( getAdminRole() );
        Role develRole = rbacManager.saveRole( getDeveloperRole() );

        assertEquals( 2, rbacManager.getAllRoles().size() );

        Role actualAdmin = rbacManager.getRole( adminRole.getName() );
        Role actualDevel = rbacManager.getRole( develRole.getName() );

        assertEquals( adminRole, actualAdmin );
        assertEquals( develRole, actualDevel );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 2, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 2, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testAllowRoleWithoutPermissions()
        throws RbacManagerException
    {
        assertNotNull( rbacManager );

        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );

        String rolename = "Test Role";

        Role testRole = rbacManager.createRole( rolename );
        testRole = rbacManager.saveRole( testRole );

        assertNotNull( testRole );
        assertEquals( 1, rbacManager.getAllRoles().size() );
        assertEquals( 0, rbacManager.getAllPermissions().size() );

        Role actualRole = rbacManager.getRole( rolename );

        assertEquals( testRole, actualRole );
        assertEquals( 1, rbacManager.getAllRoles().size() );
        assertEquals( 0, rbacManager.getAllPermissions().size() );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 1, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 0, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testAddGetChildRole()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;
        assertNotNull( manager );

        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );

        Role adminRole = manager.saveRole( getAdminRole() );
        Role develRole = manager.saveRole( getDeveloperRole() );

        assertEquals( 2, manager.getAllRoles().size() );

        Role actualAdmin = manager.getRole( adminRole.getName() );
        Role actualDevel = manager.getRole( develRole.getName() );

        assertEquals( adminRole, actualAdmin );
        assertEquals( develRole, actualDevel );

        // Now add a child role.
        manager.addChildRole( develRole, getProjectAdminRole() );

        manager.saveRole( develRole );

        assertEquals( 3, manager.getAllRoles().size() );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 3, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 3, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testAddGetChildRoleViaName()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;
        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );
        assertNotNull( manager );

        Role adminRole = manager.saveRole( getAdminRole() );
        Role develRole = manager.saveRole( getDeveloperRole() );

        assertEquals( 2, manager.getAllRoles().size() );

        Role actualAdmin = manager.getRole( adminRole.getName() );
        Role actualDevel = manager.getRole( develRole.getName() );

        assertEquals( adminRole, actualAdmin );
        assertEquals( develRole, actualDevel );

        // Now do a child role.
        Role projectRole = getProjectAdminRole();
        String projectRoleName = projectRole.getName();
        manager.saveRole( projectRole );

        develRole.addChildRoleName( projectRoleName );

        manager.saveRole( develRole );

        assertEquals( 3, manager.getAllRoles().size() );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 3, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 3, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testUserAssignmentAddRole()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;

        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );

        Role adminRole = manager.saveRole( getAdminRole() );

        assertEquals( 1, manager.getAllRoles().size() );

        String adminPrincipal = "admin";

        UserAssignment assignment = manager.createUserAssignment( adminPrincipal );

        assignment.addRoleName( adminRole );

        manager.saveUserAssignment( assignment );

        assertEquals( 1, manager.getAllUserAssignments().size() );
        assertEquals( 1, manager.getAllRoles().size() );

        UserAssignment ua = manager.getUserAssignment( adminPrincipal );
        assertNotNull( ua );

        Role fetched = manager.getRole( "ADMIN" );
        assertNotNull( fetched );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 1, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 1, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testUserAssignmentWithChildRoles()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;
        rbacManager.eraseDatabase();
        Role developerRole = manager.saveRole( getDeveloperRole() );

        Role adminRole = getAdminRole();

        adminRole.addChildRoleName( developerRole.getName() );

        adminRole = manager.saveRole( adminRole );

        String adminPrincipal = "admin";
        UserAssignment assignment = manager.createUserAssignment( adminPrincipal );
        assignment.addRoleName( adminRole );
        assignment = manager.saveUserAssignment( assignment );

        assertEquals( 1, assignment.getRoleNames().size() );
        assertEquals( 1, manager.getAssignedRoles( adminPrincipal ).size() );
    }

    @Test
    public void testGetAssignedPermissionsNoChildRoles()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;

        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );

        Role admin = getAdminRole();

        admin = manager.saveRole( admin );

        assertEquals( 1, manager.getAllRoles().size() );

        String adminPrincipal = "admin";

        UserAssignment ua = manager.createUserAssignment( adminPrincipal );

        ua.addRoleName( admin );

        manager.saveUserAssignment( ua );

        assertEquals( 1, manager.getAllUserAssignments().size() );

        Set<Permission> assignedPermissions = manager.getAssignedPermissions( adminPrincipal );

        assertNotNull( assignedPermissions );
        assertEquals( 1, assignedPermissions.size() );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 1, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 1, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testGlobalResource()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;
        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );
        Permission editConfiguration = manager.createPermission( "Edit Configuration" );
        editConfiguration.setOperation( manager.createOperation( "edit-configuration" ) );
        editConfiguration.setResource( manager.getGlobalResource() );
        manager.savePermission( editConfiguration );

        assertEquals( 1, manager.getAllPermissions().size() );
        assertEquals( 1, manager.getAllOperations().size() );
        assertEquals( 1, manager.getAllResources().size() );

        Permission deleteConfiguration = manager.createPermission( "Delete Configuration" );
        deleteConfiguration.setOperation( manager.createOperation( "delete-configuration" ) );
        deleteConfiguration.setResource( manager.getGlobalResource() );
        manager.savePermission( deleteConfiguration );

        assertEquals( 2, manager.getAllPermissions().size() );
        assertEquals( 2, manager.getAllOperations().size() );
        assertEquals( 1, manager.getAllResources().size() );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 0, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 2, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testGlobalResourceOneLiner()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;
        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );
        manager.savePermission(
            manager.createPermission( "Edit Configuration", "edit-configuration", Resource.GLOBAL ) );
        manager.savePermission(
            manager.createPermission( "Delete Configuration", "delete-configuration", Resource.GLOBAL ) );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 0, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 2, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testUserAssignmentAddRemoveSecondRole()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;

        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );

        Role developerRole = getDeveloperRole();
        manager.saveRole( developerRole );

        // Setup User / Assignment with 1 role.
        String username = "bob";
        UserAssignment assignment = manager.createUserAssignment( username );
        assignment.addRoleName( developerRole );
        manager.saveUserAssignment( assignment );

        assertEquals( 1, manager.getAllUserAssignments().size() );
        assertEquals( 1, manager.getAllRoles().size() );

        // Create another role add it to manager.
        Role projectAdmin = getProjectAdminRole();
        String projectAdminRoleName = projectAdmin.getName();
        manager.saveRole( projectAdmin );

        // Get User Assignment, add a second role
        UserAssignment bob = manager.getUserAssignment( username );
        bob.addRoleName( projectAdminRoleName );
        bob = manager.saveUserAssignment( bob );

        assertEquals( 1, manager.getAllUserAssignments().size() );
        assertEquals( 2, manager.getAllRoles().size() );
        assertEquals( 2, bob.getRoleNames().size() );
        assertEquals( 0, manager.getUnassignedRoles( bob.getPrincipal() ).size() );

        List<String> roles = bob.getRoleNames();
        assertEquals( 2, roles.size() );

        // Remove 1 role from bob, end up with 1 role for bob.
        roles.remove( projectAdminRoleName );
        assertEquals( 1, roles.size() );
        bob.setRoleNames( roles );
        bob = manager.saveUserAssignment( bob );
        assertEquals( "Should only have 1 role under bob now.", 1, bob.getRoleNames().size() );
        assertEquals( "Should have 2 total roles still.", 2, manager.getAllRoles().size() );
        assertEquals( "Should have 1 assignable role", 1, manager.getUnassignedRoles( bob.getPrincipal() ).size() );

        // Fetch bob again. see if role is missing.
        UserAssignment cousin = manager.getUserAssignment( username );
        assertEquals( 1, cousin.getRoleNames().size() );

        assertEquals( "Should only have 1 role under bob now.", 1, cousin.getRoleNames().size() );
        assertEquals( "Should have 2 total roles still.", 2, manager.getAllRoles().size() );

        // remove the last role
        roles.remove( developerRole.getName() );
        bob.setRoleNames( roles );
        bob = manager.saveUserAssignment( bob );
        assertEquals( "Should have 2 assignable roles.", 2, manager.getUnassignedRoles( bob.getPrincipal() ).size() );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 2, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 2, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testUserAssignmentMultipleRoles()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;

        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );

        Role devRole = getDeveloperRole();
        manager.saveRole( devRole );

        // Setup User / Assignment with 1 role.
        String username = "bob";
        UserAssignment assignment = manager.createUserAssignment( username );
        assignment.addRoleName( devRole );
        assignment = manager.saveUserAssignment( assignment );

        assertEquals( 1, manager.getAllUserAssignments().size() );
        assertEquals( 1, manager.getAllRoles().size() );

        // assign the same role again to the same user
        assignment.addRoleName( devRole.getName() );
        manager.saveUserAssignment( assignment );

        // we certainly shouldn't have 2 roles here now
        assertEquals( 1, assignment.getRoleNames().size() );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 1, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 1, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testUserAssignmentMultipleRolesWithChildRoles()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;

        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );

        Role devRole = getDeveloperRole();
        Role devPlusRole = getSuperDeveloperRole();
        devPlusRole.setChildRoleNames( Collections.singletonList( devRole.getName() ) );
        manager.saveRole( devRole );
        manager.saveRole( devPlusRole );

        // Setup User / Assignment with 1 role.
        String username = "bob";
        UserAssignment assignment = manager.createUserAssignment( username );
        assignment.addRoleName( devRole );
        assignment = manager.saveUserAssignment( assignment );

        assertEquals( 1, manager.getAllUserAssignments().size() );
        assertEquals( "should be only one role assigned", 1,
                      manager.getAssignedRoles( assignment.getPrincipal() ).size() );
        assertEquals( "should be one role left to assign", 1,
                      manager.getUnassignedRoles( assignment.getPrincipal() ).size() );
        assertEquals( 2, manager.getAllRoles().size() );

        // assign the same role again to the same user
        assignment.addRoleName( devRole.getName() );
        manager.saveUserAssignment( assignment );

        // we certainly shouldn't have 2 roles here now
        assertEquals( 1, assignment.getRoleNames().size() );

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 2, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 1, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testGetAssignedRoles()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;
        rbacManager.eraseDatabase();

        Role adminRole = manager.saveRole( getAdminRole() );
        Role projectAdminRole = manager.saveRole( getProjectAdminRole() );
        Role developerRole = manager.saveRole( getDeveloperRole() );

        // Setup 3 roles.
        assertEquals( 3, manager.getAllRoles().size() );

        // Setup User / Assignment with 3 roles.
        String username = "bob";

        UserAssignment assignment = manager.createUserAssignment( username );
        assignment.addRoleName( developerRole.getName() );
        assignment.addRoleName( projectAdminRole.getName() );
        assignment.addRoleName( adminRole.getName() );
        assignment = manager.saveUserAssignment( assignment );

        assertEquals( 3, assignment.getRoleNames().size() );
        assertEquals( 1, manager.getAllUserAssignments().size() );
        assertEquals( 3, manager.getAllRoles().size() );

        afterSetup();

        // Get the List of Assigned Roles for user bob.
        Collection<Role> assignedRoles = manager.getAssignedRoles( username );

        assertNotNull( assignedRoles );
        assertEquals( 3, assignedRoles.size() );
    }

    @Test
    public void testGetAssignedPermissions()
        throws RbacManagerException
    {
        RBACManager manager = rbacManager;
        rbacManager.eraseDatabase();
        // Setup 3 roles.
        manager.saveRole( getAdminRole() );
        manager.saveRole( getProjectAdminRole() );
        Role added = manager.saveRole( getDeveloperRole() );
        String roleName = added.getName();

        assertEquals( 3, manager.getAllRoles().size() );
        assertEquals( 3, manager.getAllPermissions().size() );

        // Setup User / Assignment with 1 role.
        String username = "bob";

        UserAssignment assignment = manager.createUserAssignment( username );
        assignment.addRoleName( roleName );
        manager.saveUserAssignment( assignment );

        assertEquals( 1, manager.getAllUserAssignments().size() );
        assertEquals( 3, manager.getAllRoles().size() );
        assertEquals( 3, manager.getAllPermissions().size() );

        // Get the List of Assigned Roles for user bob.
        Collection<Permission> assignedPermissions = manager.getAssignedPermissions( username );

        assertNotNull( assignedPermissions );
        assertEquals( 1, assignedPermissions.size() );
    }

    public Role getChildRole( RBACManager manager, Role role, String expectedChildRoleName, int childRoleCount )
        throws RbacManagerException
    {
        assertTrue( role.hasChildRoles() );
        List<String> childNames = role.getChildRoleNames();
        assertNotNull( childNames );
        assertEquals( 1, childNames.size() );
        String childName = (String) childNames.get( 0 );
        assertNotNull( childName );
        Role childRole = manager.getRole( childName );
        assertNotNull( childRole );
        assertEquals( expectedChildRoleName, childRole.getName() );

        return childRole;
    }

    @Test
    public void testGetRolesDeep()
        throws RbacManagerException
    {
        rbacManager.eraseDatabase();
        rbacDefaults.createDefaults();

        // Setup User / Assignment with 1 role.
        String username = "bob";

        UserAssignment assignment = rbacManager.createUserAssignment( username );
        assignment.addRoleName( "Developer" );
        rbacManager.saveUserAssignment( assignment );

        assertEquals( 1, rbacManager.getAllUserAssignments().size() );
        assertEquals( 4, rbacManager.getAllRoles().size() );
        assertEquals( 6, rbacManager.getAllPermissions().size() );

        // Get the List of Assigned Roles for user bob.
        Role devel = rbacManager.getRole( "Developer" );
        assertNotNull( devel );

        // First Depth.
        Role trusted = getChildRole( rbacManager, devel, "Trusted Developer", 1 );

        // Second Depth.
        Role sysAdmin = getChildRole( rbacManager, trusted, "System Administrator", 1 );

        // Third Depth.
        getChildRole( rbacManager, sysAdmin, "User Administrator", 1 );
    }

    @Test
    public void testGetAssignedPermissionsDeep()
        throws RbacManagerException
    {
        rbacDefaults.createDefaults();

        // Setup User / Assignment with 1 role.
        String username = "bob";

        UserAssignment assignment = rbacManager.createUserAssignment( username );
        assignment.addRoleName( "Developer" );
        rbacManager.saveUserAssignment( assignment );

        assertEquals( 1, rbacManager.getAllUserAssignments().size() );
        assertEquals( 4, rbacManager.getAllRoles().size() );
        assertEquals( 6, rbacManager.getAllPermissions().size() );

        afterSetup();

        // Get the List of Assigned Roles for user bob.
        Collection<Permission> assignedPermissions = rbacManager.getAssignedPermissions( username );

        assertNotNull( assignedPermissions );
        assertEquals( 6, assignedPermissions.size() );
    }

    @Test
    public void testLargeApplicationInit()
        throws RbacManagerException
    {

        rbacManager.eraseDatabase();
        rbacDefaults.createDefaults();
        assertEquals( 6, rbacManager.getAllPermissions().size() );
        assertEquals( 11, rbacManager.getAllOperations().size() );
        assertEquals( 4, rbacManager.getAllRoles().size() );
    }

    @Test
    public void testAddRemovePermanentPermission()
        throws RbacManagerException
    {
        assertNotNull( rbacManager );

        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );

        Role adminRole = rbacManager.saveRole( getAdminRole() );
        rbacManager.saveRole( getDeveloperRole() );

        assertEquals( 2, rbacManager.getAllRoles().size() );
        assertEquals( 2, rbacManager.getAllPermissions().size() );

        Permission createUserPerm = rbacManager.createPermission( "CREATE_USER", "CREATE", "User" );
        createUserPerm.setPermanent( true );

        // perm shouldn't exist in manager (yet)
        assertEquals( 2, rbacManager.getAllPermissions().size() );

        adminRole.addPermission( createUserPerm );
        rbacManager.saveRole( adminRole );

        // perm should exist in manager now.
        assertEquals( 3, rbacManager.getAllPermissions().size() );
        Permission fetched = rbacManager.getPermission( "CREATE_USER" );
        assertNotNull( fetched );

        // Attempt to remove perm now.
        try
        {
            // Use permission name technique first.
            rbacManager.removePermission( "CREATE_USER" );
        }
        catch ( RbacPermanentException e )
        {
            // expected path.
        }

        try
        {
            // Use permission object technique next.
            rbacManager.removePermission( fetched );
        }
        catch ( RbacPermanentException e )
        {
            // expected path.
        }

        // Assert some event tracker stuff
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 2, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 3, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testAddRemovePermanentRole()
        throws RbacManagerException
    {
        assertNotNull( rbacManager );
        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );

        Role adminRole = getAdminRole();
        adminRole.setPermanent( true );

        adminRole = rbacManager.saveRole( adminRole );
        Role develRole = rbacManager.saveRole( getDeveloperRole() );

        assertEquals( 2, rbacManager.getAllRoles().size() );

        Role actualAdmin = rbacManager.getRole( adminRole.getName() );
        Role actualDevel = rbacManager.getRole( develRole.getName() );

        assertEquals( adminRole, actualAdmin );
        assertEquals( develRole, actualDevel );

        // Attempt to remove perm now.
        try
        {
            // Use role name technique first.
            rbacManager.removeRole( adminRole.getName() );
        }
        catch ( RbacPermanentException e )
        {
            // expected path.
        }

        try
        {
            // Use role object technique next.
            rbacManager.removeRole( adminRole );
        }
        catch ( RbacPermanentException e )
        {
            // expected path.
        }

        /* Assert some event tracker stuff */
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 2, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 2, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    @Test
    public void testAddRemovePermanentOperation()
        throws RbacManagerException
    {

        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );
        assertNotNull( rbacManager );

        Role adminRole = rbacManager.saveRole( getAdminRole() );
        rbacManager.saveRole( getDeveloperRole() );

        assertEquals( 2, rbacManager.getAllRoles().size() );
        assertEquals( 2, rbacManager.getAllPermissions().size() );

        Permission createUserPerm = rbacManager.createPermission( "CREATE_USER", "CREATE", "User" );
        createUserPerm.getOperation().setPermanent( true );

        // perm shouldn't exist in manager (yet)
        assertEquals( 2, rbacManager.getAllPermissions().size() );
        assertEquals( 1, rbacManager.getAllOperations().size() );

        adminRole.addPermission( createUserPerm );
        rbacManager.saveRole( adminRole );

        // perm should exist in manager now.
        assertEquals( 2, rbacManager.getAllOperations().size() );
        Operation fetched = rbacManager.getOperation( "CREATE" );
        assertNotNull( fetched );

        // Attempt to remove operation now.
        try
        {
            // Use operation name technique first.
            rbacManager.removeOperation( "CREATE" );
        }
        catch ( RbacPermanentException e )
        {
            // expected path.
        }

        try
        {
            // Use operation object technique next.
            rbacManager.removeOperation( fetched );
        }
        catch ( RbacPermanentException e )
        {
            // expected path.
        }

        // Assert some event tracker stuff
        assertNotNull( eventTracker );
        assertEquals( EVENTCOUNT, eventTracker.initCount );
        assertTrue( eventTracker.lastDbFreshness.booleanValue() );

        assertEquals( 2, eventTracker.addedRoleNames.size() );
        assertEquals( 0, eventTracker.removedRoleNames.size() );
        assertEquals( 3, eventTracker.addedPermissionNames.size() );
        assertEquals( 0, eventTracker.removedPermissionNames.size() );
    }

    /**
     * Allows subclasses to hook code after a test case has finished it's setup
     */
    protected void afterSetup()
    {
        // do nothing
    }
}
