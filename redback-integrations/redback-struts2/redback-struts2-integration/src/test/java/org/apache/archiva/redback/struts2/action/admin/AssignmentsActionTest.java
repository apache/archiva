package org.apache.archiva.redback.struts2.action.admin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.google.common.collect.Lists;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionProxy;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.rbac.RbacObjectInvalidException;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.authorization.AuthorizationResult;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.struts2.model.ApplicationRoleDetails;
import org.apache.archiva.redback.struts2.model.ApplicationRoleDetails.RoleTableCell;
import org.apache.archiva.redback.integration.interceptor.SecureActionBundle;
import org.apache.archiva.redback.integration.interceptor.SecureActionException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @todo missing tests for success/fail on standard show/edit functions (non security testing related)
 */
public class AssignmentsActionTest
    extends AbstractUserCredentialsActionTest
{
 //@Rule public TestName name = new TestName();
 // xxx help for jdk 7 investigation
    private AssignmentsAction action;

  /*  public static final List<String> favorites =
        Arrays.asList("user", "user2","user3","user-admin");
   xxx help for jdk 7 investigation 
  private void displayInfo(boolean before) throws RbacObjectNotFoundException, RbacManagerException {
    System.err.print(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
    if (before) {
      System.err.print("(b)");
    } else {
      System.err.print("(a)");
    }
    System.err.println(name.getMethodName());
    for (String user : favorites) {
      if (rbacManager.userAssignmentExists(user)) {
        for (String s : rbacManager.getUserAssignment(user).getRoleNames()) {
          System.err.println("--" + user + ">>" + s);
        }
      }
    }
    System.err.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
  }*/
   
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        ActionProxy actionProxy = getActionProxy( "/security/assignments" );
        action = (AssignmentsAction) actionProxy.getAction();

        login( action, "user", PASSWORD );      
        action.setPrincipal( "user2" );
        //displayInfo(true);//xxx help for jdk 7 investigation

    }

  /*@After xxx help for jdk 7 investigation
  @Override
  public void after() {
    super.after();
    try {
       displayInfo(false);
    } catch (RbacObjectNotFoundException ex) {
      Logger.getLogger(AssignmentsActionTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (RbacObjectInvalidException ex) {
      Logger.getLogger(AssignmentsActionTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (RbacManagerException ex) {
      Logger.getLogger(AssignmentsActionTest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }*/
    /**
     * Check security - show/edituser should fail if the permission 'user-management-user-role' is not present, but a
     * valid 'user-management-role-grant' is.
     */
    @Test
    public void testUserWithOnlyRoleGrantHasNoAccess()
        throws Exception
    {

        addAssignment( "user", "Grant Administrator - default" );

        List<SecureActionBundle.AuthorizationTuple> authorizationTuples = getTuples();
        for ( SecureActionBundle.AuthorizationTuple tuple : authorizationTuples )
        {
            AuthorizationResult authzResult = system.authorize( session, tuple.getOperation(), tuple.getResource() );

            assertFalse( authzResult.isAuthorized() );
        }
        
        rbacManager.getUserAssignment( "user" ).removeRoleName( "Grant Administrator - default" );
    }

    /**
     * Check security - check success if the permission 'user-management-user-role' is present along with global
     * 'user-management-role-grant'.
     */
    @Test
    public void testUserWithOnlyRoleGrantHasAccess()
        throws Exception
    {
        addAssignment( "user", "Project Administrator - default" );

        List<SecureActionBundle.AuthorizationTuple> authorizationTuples = getTuples();
        boolean result = false;
        for ( SecureActionBundle.AuthorizationTuple tuple : authorizationTuples )
        {
            AuthorizationResult authzResult = system.authorize( session, tuple.getOperation(), tuple.getResource() );

            result |= authzResult.isAuthorized();
        }
        assertTrue( result );
        rbacManager.getUserAssignment( "user" ).removeRoleName( "Project Administrator - default" );
    }

    private List<SecureActionBundle.AuthorizationTuple> getTuples()
        throws SecureActionException
    {
        return action.getSecureActionBundle().getAuthorizationTuples();
    }

    /**
     * Check roles can be assigned if the user has no previous assignments.
     */
    @Test
    public void testShowWhenUserHasNoAssignments()
        throws Exception
    {
        addAssignment( "user", "Project Administrator - default" );

        action.setPrincipal( "user3" );

        assertEquals( Action.SUCCESS, action.show() );

        assertEquals( 2, action.getApplicationRoleDetails().size() );
        rbacManager.getUserAssignment( "user" ).removeRoleName( "Project Administrator - default" );
    }

    /**
     * Check security - show should filter out roles that the 'user-management-role-grant' is not present for
     */
    @Test
    public void testRoleGrantFilteringOnShow()
        throws Exception
    {
        addAssignment( "user", "Project Administrator - default" );

        assertEquals( Action.SUCCESS, action.show() );

        assertEquals( 2, action.getApplicationRoleDetails().size() );
        ApplicationRoleDetails details = (ApplicationRoleDetails) action.getApplicationRoleDetails().get( 0 );
        assertEquals( "System", details.getName() );
        assertEquals( "Roles that apply system-wide, across all of the applications", details.getDescription() );
        assertEquals( "found roles " + details.getAvailableRoles(), 0, details.getAvailableRoles().size() );
        details = (ApplicationRoleDetails) action.getApplicationRoleDetails().get( 1 );
        assertEquals( "Continuum", details.getName() );
        assertEquals( "found roles " + details.getAvailableRoles(), 0, details.getAvailableRoles().size() );

        // This table rendering code clearly has to go
        List<List<RoleTableCell>> table = details.getTable();
        assertEquals( 1, table.size() );
        assertRow( table, 0, "default", "Project Administrator - default", false );
        rbacManager.getUserAssignment( "user" ).removeRoleName( "Project Administrator - default" );
    }

    @SuppressWarnings( "unchecked" )
    private void assertRow( List table, int index, String name, String label, boolean assigned )
    {
        List<RoleTableCell> row = (List<RoleTableCell>) table.get( index );
        assertEquals( name, row.get( 0 ).getName() );
        assertEquals( label, row.get( 1 ).getName() );
        assertEquals( assigned, row.get( 2 ).isAssigned() );
    }

    /**
     * Check security - show should not filter out roles if 'user-management-role-grant' is present for the global
     * resource
     */
    // TODO: currently returns all roles - we really want all templated roles
    // public void testRoleGrantFilteringOnShowGlobalGrant()
    // throws RbacObjectInvalidException, RbacManagerException
    // {
    // addAssignment( "user", "Global Grant Administrator" );
    //
    // assertEquals( Action.SUCCESS, action.show() );
    //
    // assertEquals( 2, action.getApplicationRoleDetails().size() );
    // ApplicationRoleDetails details = (ApplicationRoleDetails) action.getApplicationRoleDetails().get( 0 );
    // assertEquals( "redback-xwork-integration-core", details.getName() );
    // assertEquals( 0, details.getAvailableRoles().size() );
    //
    // details = (ApplicationRoleDetails) action.getApplicationRoleDetails().get( 1 );
    // assertEquals( "Continuum", details.getName() );
    // assertEquals( 0, details.getAvailableRoles().size() );
    //
    // List table = details.getTable();
    // assertEquals( 2, table.size() );
    // assertRow( table, 0, "default", "Project Administrator - default", false );
    // assertRow( table, 1, "other", "Project Administrator - other", false );
    // }

    /**
     * Check security - edituser should skip adding a role that 'user-management-role-grant' is not present for a
     * non-templated role
     */
    @Test
    public void testRoleGrantFilteringOnAddRolesNotPermittedTemplated()
        throws RbacObjectInvalidException, RbacManagerException
    {
        addAssignment( "user", "Project Administrator - default" );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> dSelectedRoles = new ArrayList<String>();
        dSelectedRoles.add( "Project Administrator - other" );

        action.setAddDSelectedRoles( dSelectedRoles );

        assertTrue( rbacManager.getUserAssignment( "user2" ).getRoleNames().isEmpty() );

        assertEquals( Action.SUCCESS, action.edituser() );

        assertTrue( rbacManager.getUserAssignment( "user2" ).getRoleNames().isEmpty() );
        rbacManager.getUserAssignment( "user" ).removeRoleName( "Project Administrator - default" );
    }

    /**
     * Check security - edituser should skip adding a role that 'user-management-role-grant' is not present for a
     * templated role
     */
    @Test
    public void testRoleGrantFilteringOnAddRolesNotPermittedNotTemplated()
        throws RbacObjectInvalidException, RbacManagerException
    {
        addAssignment( "user", "Project Administrator - default" );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> ndSelectedRoles = new ArrayList<String>();
        ndSelectedRoles.add( "Continuum Group Project Administrator" );

        action.setAddNDSelectedRoles( ndSelectedRoles );

        assertTrue( rbacManager.getUserAssignment( "user2" ).getRoleNames().isEmpty() );

        assertEquals( Action.SUCCESS, action.edituser() );

        assertTrue( rbacManager.getUserAssignment( "user2" ).getRoleNames().isEmpty() );
        rbacManager.getUserAssignment( "user" ).removeRoleName( "Project Administrator - default" );
    }

    /**
     * Check security - edituser should succeed if adding a role that 'user-management-role-grant' is present for
     * untemplated roles
     */
    @Test
    public void testRoleGrantFilteringOnAddRolesPermittedNotTemplated()
        throws RbacObjectInvalidException, RbacManagerException, AccountLockedException, AuthenticationException,
        UserNotFoundException
    {
        addAssignment( "user", "Global Grant Administrator" );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> ndSelectedRoles = new ArrayList<String>();
        ndSelectedRoles.add( "Continuum Group Project Administrator" );

        action.setAddNDSelectedRoles( ndSelectedRoles );

        assertTrue( rbacManager.getUserAssignment( "user2" ).getRoleNames().isEmpty() );

        assertEquals( Action.SUCCESS, action.edituser() );

        assertEquals( Lists.<String>newArrayList( "Continuum Group Project Administrator" ),
                      rbacManager.getUserAssignment( "user2" ).getRoleNames() );
        
        rbacManager.getUserAssignment( "user2" ).removeRoleName( "Continuum Group Project Administrator" );
        rbacManager.getUserAssignment( "user" ).removeRoleName( "Global Grant Administrator" );
    }

    /**
     * Check security - edituser should succeed if adding a role that 'user-management-role-grant' is present for
     * templated roles
     */
    @Ignore
    public void testRoleGrantFilteringOnAddRolesPermittedTemplated()
        throws Exception
    {

        rbacManager.removeUserAssignment( "user" );

        addAssignment( "user", "Project Administrator - default" );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> dSelectedRoles = new ArrayList<String>();
        dSelectedRoles.add( "Project Administrator - default" );

        ActionProxy actionProxy = getActionProxy( "/security/assignments" );
        AssignmentsAction newAction = (AssignmentsAction) actionProxy.getAction();

        login( newAction, "user", PASSWORD );

        newAction.setPrincipal( "user2" );

        newAction.setAddDSelectedRoles( dSelectedRoles );

        assertTrue( rbacManager.getUserAssignment( "user2" ).getRoleNames().isEmpty() );

        assertEquals( Action.SUCCESS, newAction.edituser() );

        assertEquals( Arrays.asList( "Project Administrator - default" ),
                      rbacManager.getUserAssignment( "user2" ).getRoleNames() );
    }

    /**
     * Check security - edituser should succeed if adding a role that 'user-management-role-grant' is present for
     * templated roles
     */
    @Test
    public void testRoleGrantFilteringOnAddRolesPermittedTemplatedExistingRole()
        throws Exception
    {
        addAssignment( "user", "Project Administrator - default" );

        // cleanup before next test
        rbacManager.removeUserAssignment( "user2" );

        addAssignment( "user2", "Project Administrator - other" );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> dSelectedRoles = new ArrayList<String>();
        dSelectedRoles.add( "Project Administrator - default" );

        ActionProxy actionProxy = getActionProxy( "/security/assignments" );
        AssignmentsAction newAction = (AssignmentsAction) actionProxy.getAction();

        login( newAction, "user2", PASSWORD );

        newAction.setPrincipal( "user2" );

        newAction.setAddDSelectedRoles( dSelectedRoles );

        assertEquals( Arrays.asList( "Project Administrator - other" ),
                      rbacManager.getUserAssignment( "user2" ).getRoleNames() );

        assertEquals( Action.SUCCESS, newAction.edituser() );

        //assertEquals( Arrays.asList( "Project Administrator - default", "Project Administrator - other" ),
        //              rbacManager.getUserAssignment( "user2" ).getRoleNames() );
        rbacManager.getUserAssignment( "user" ).removeRoleName( "Project Administrator - default" ); 
        
    }

    /**
     * Check security - edituser should fail if removing a role that 'user-management-role-grant' is not present for
     * untemplated roles
     */
    @Test
    public void testRoleGrantFilteringOnRemoveRolesNotPermittedNotTemplated()
        throws Exception
    {

        rbacManager.removeUserAssignment( "user2" );

        addAssignment( "user", "Project Administrator - default" );

        addAssignment( "user2", "Continuum Group Project Administrator" );

        ActionProxy actionProxy = getActionProxy( "/security/assignments" );
        AssignmentsAction newAction = (AssignmentsAction) actionProxy.getAction();

        login( newAction, "user2", PASSWORD );

        newAction.setPrincipal( "user2" );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> ndSelectedRoles = new ArrayList<String>();
        newAction.setAddNDSelectedRoles( ndSelectedRoles );

        assertEquals( Arrays.asList( "Continuum Group Project Administrator" ),
                      rbacManager.getUserAssignment( "user2" ).getRoleNames() );

        assertEquals( Action.SUCCESS, newAction.edituser() );

        assertEquals( Arrays.asList( "Continuum Group Project Administrator" ),
                      rbacManager.getUserAssignment( "user2" ).getRoleNames() );
        
        rbacManager.getUserAssignment( "user2" ).removeRoleName( "Continuum Group Project Administrator" );
        rbacManager.getUserAssignment( "user" ).removeRoleName( "Project Administrator - default" );
    }

    /**
     * Check security - edituser should fail if removing a role that 'user-management-role-grant' is not present for
     * templated roles
     */
    @Ignore
    public void testRoleGrantFilteringOnRemoveRolesNotPermittedTemplated()
        throws Exception
    {
        rbacManager.removeUserAssignment( "user2" );

        addAssignment( "user", "Project Administrator - other" );

        addAssignment( "user2", "Project Administrator - default" );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> dSelectedRoles = new ArrayList<String>();

        ActionProxy actionProxy = getActionProxy( "/security/assignments" );
        AssignmentsAction newAction = (AssignmentsAction) actionProxy.getAction();

        login( newAction, "user2", PASSWORD );

        newAction.setPrincipal( "user2" );

        newAction.setAddDSelectedRoles( dSelectedRoles );

        assertEquals( Arrays.asList( "Project Administrator - default" ),
                      rbacManager.getUserAssignment( "user2" ).getRoleNames() );

        assertEquals( Action.SUCCESS, newAction.edituser() );

        assertEquals( Arrays.asList( "Project Administrator - default" ),
                      rbacManager.getUserAssignment( "user2" ).getRoleNames() );
    }

    /**
     * Check security - edituser should succeed if removing a role that 'user-management-role-grant' is present for
     * untemplated roles
     */
    @Test
    public void testRoleGrantFilteringOnRemoveRolesPermittedNotTemplated()
        throws Exception
    {
        addAssignment( "user", "Global Grant Administrator" );

        addAssignment( "user2", "Continuum Group Project Administrator" );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> ndSelectedRoles = new ArrayList<String>();
        action.setAddNDSelectedRoles( ndSelectedRoles );

        assertEquals( Arrays.asList( "Continuum Group Project Administrator" ),
                      rbacManager.getUserAssignment( "user2" ).getRoleNames() );

        assertEquals( Action.SUCCESS, action.edituser() );

        assertTrue( rbacManager.getUserAssignment( "user2" ).getRoleNames().isEmpty() );
        
        rbacManager.getUserAssignment( "user" ).removeRoleName( "Global Grant Administrator" );
    }

    /**
     * Check security - edituser should succeed if removing a role that 'user-management-role-grant' is present for
     * templated roles and there is an existing role that is not assignable by the current user.
     */
    @Test
    public void testRoleGrantFilteringOnRemoveRolesPermittedTemplatedExistingRole()
        throws Exception
    {
        addAssignment( "user", "Project Administrator - default" );

        rbacManager.removeUserAssignment( "user2" );

        addAssignment( "user2", "Project Administrator - default" );
        addAssignment( "user2", "Project Administrator - other" );
        addAssignment( "user2", "Registered User" );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> dSelectedRoles = new ArrayList<String>();
        dSelectedRoles.add( "Project Administrator - other" );
        dSelectedRoles.add( "Registered User" );
        action.setAddDSelectedRoles( dSelectedRoles );

        assertEquals(
            Arrays.asList( "Project Administrator - default", "Project Administrator - other", "Registered User" ),
            rbacManager.getUserAssignment( "user2" ).getRoleNames() );

        assertEquals( Action.SUCCESS, action.edituser() );

        // Roles may be out of order, due to removal and subsequent re-add
        List<String> user2roles = rbacManager.getUserAssignment( "user2" ).getRoleNames();
        assertTrue( user2roles.contains( "Project Administrator - other" ) );
        assertTrue( user2roles.contains( "Registered User" ) );
        
        
        // back to initial
        rbacManager.getUserAssignment( "user2" ).removeRoleName( "Registered User" );
        rbacManager.getUserAssignment( "user2" ).removeRoleName( "Project Administrator - other" );
        rbacManager.getUserAssignment( "user" ).removeRoleName( "Project Administrator - default" );
    }

    /**
     * Check security - edituser should succeed if removing a role that 'user-management-role-grant' is present for
     * templated roles
     */
    @Test
    public void testRoleGrantFilteringOnRemoveRolesPermittedTemplated()
        throws Exception
    {
        rbacManager.removeUserAssignment( "user2" );

        addAssignment( "user", "Project Administrator - default" );

        addAssignment( "user2", "Project Administrator - default" );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> dSelectedRoles = new ArrayList<String>();
        action.setAddDSelectedRoles( dSelectedRoles );

        assertEquals( Arrays.asList( "Project Administrator - default" ),
                      rbacManager.getUserAssignment( "user2" ).getRoleNames() );

        assertEquals( Action.SUCCESS, action.edituser() );

        assertTrue( rbacManager.getUserAssignment( "user2" ).getRoleNames().isEmpty() );
        
        rbacManager.getUserAssignment( "user" ).removeRoleName( "Project Administrator - default" );
    }

    /**
     * Check security - show should succeed and display all roles, even without 'user-management-role-grant' or
     * 'user-management-user-role' for the user administrators.
     *
     * @throws org.apache.archiva.redback.policy.MustChangePasswordException
     */
    @Test
    public void testSystemAdminCanShowRoles()
        throws Exception
    {

        login( action, "admin", PASSWORD );

        assertEquals( Action.SUCCESS, action.show() );

        assertEquals( 2, action.getApplicationRoleDetails().size() );
        ApplicationRoleDetails details = (ApplicationRoleDetails) action.getApplicationRoleDetails().get( 0 );
        assertEquals( "System", details.getName() );
        assertEquals( "Roles that apply system-wide, across all of the applications", details.getDescription() );
        assertEquals( 4, details.getAvailableRoles().size() );
        assertEquals( "Guest", details.getAvailableRoles().get( 0 ) );
        assertEquals( "Registered User", details.getAvailableRoles().get( 1 ) );
        assertEquals( "System Administrator", details.getAvailableRoles().get( 2 ) );
        assertEquals( "User Administrator", details.getAvailableRoles().get( 3 ) );

        details = (ApplicationRoleDetails) action.getApplicationRoleDetails().get( 1 );
        assertEquals( "Continuum", details.getName() );

        assertEquals( 2, details.getAvailableRoles().size() );
        assertEquals( "Continuum Group Project Administrator", details.getAvailableRoles().get( 0 ) );
        assertEquals( "Global Grant Administrator", details.getAvailableRoles().get( 1 ) );

        List<List<RoleTableCell>> table = details.getTable();
        assertEquals( 2, table.size() );
        assertRow( table, 0, "default", "Project Administrator - default", false );
        assertRow( table, 1, "other", "Project Administrator - other", false );
    }

    /**
     * Check security - show should succeed and display all roles, even without 'user-management-role-grant' or
     * 'user-management-user-role' for the user administrators.
     */
    @Test
    public void testUserAdminCanShowRoles()
        throws Exception
    {

        ActionProxy actionProxy = getActionProxy( "/security/assignments" );
        AssignmentsAction newAction = (AssignmentsAction) actionProxy.getAction();

        login( newAction, "user-admin", PASSWORD );

        newAction.setPrincipal( "user-admin" );

        assertEquals( Action.SUCCESS, newAction.show() );

        assertEquals( 2, newAction.getApplicationRoleDetails().size() );
        ApplicationRoleDetails details = (ApplicationRoleDetails) newAction.getApplicationRoleDetails().get( 0 );
        assertEquals( "System", details.getName() );
        assertEquals( "Roles that apply system-wide, across all of the applications", details.getDescription() );
        // TODO assertEquals( 3, details.getAvailableRoles().size() );
        assertEquals( "Guest", details.getAvailableRoles().get( 0 ) );
        assertEquals( "not role Registered User roles : " + details.getAvailableRoles(), "Registered User",
                      details.getAvailableRoles().get( 1 ) );
        // TODO: assertEquals( "User Administrator", details.getAvailableRoles().get( 2 ) );

        details = newAction.getApplicationRoleDetails().get( 1 );
        assertEquals( "Continuum", details.getName() );

        assertEquals( 2, details.getAvailableRoles().size() );
        assertEquals( "Continuum Group Project Administrator", details.getAvailableRoles().get( 0 ) );
        assertEquals( "Global Grant Administrator", details.getAvailableRoles().get( 1 ) );

        List<List<RoleTableCell>> table = details.getTable();
        assertEquals( 2, table.size() );
        assertRow( table, 0, "default", "Project Administrator - default", false );
        assertRow( table, 1, "other", "Project Administrator - other", false );
    }

    /**
     * Check security - edituser should succeed in adding a role, even without 'user-management-role-grant' or
     * 'user-management-user-role' for the user administrators.
     */
    @Test
    public void testUserAdminCanAddRoles()
        throws Exception
    {
        login( action, "user-admin", PASSWORD );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> ndSelectedRoles = new ArrayList<String>();
        ndSelectedRoles.add( "Continuum Group Project Administrator" );

        action.setAddNDSelectedRoles( ndSelectedRoles );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> dSelectedRoles = new ArrayList<String>();
        dSelectedRoles.add( "Project Administrator - default" );

        action.setAddDSelectedRoles( dSelectedRoles );

        assertTrue( rbacManager.getUserAssignment( "user2" ).getRoleNames().isEmpty() );

        assertEquals( Action.SUCCESS, action.edituser() );

        assertEquals( Arrays.asList( "Continuum Group Project Administrator", "Project Administrator - default" ),
                      rbacManager.getUserAssignment( "user2" ).getRoleNames() );
        
        // back to inital
        rbacManager.getUserAssignment( "user2" ).removeRoleName( "Continuum Group Project Administrator" );
        rbacManager.getUserAssignment( "user2" ).removeRoleName( "Project Administrator - default" );
    }

    /**
     * Check security - edituser should succeed in removing a role, even without 'user-management-role-grant' or
     * 'user-management-user-role' for the user administrators.
     */
    @Test
    public void testUserAdminCanRemoveRoles()
        throws Exception
    {
        login( action, "user-admin", PASSWORD );

        rbacManager.removeUserAssignment( "user2" );

        addAssignment( "user2", "Continuum Group Project Administrator" );
        addAssignment( "user2", "Project Administrator - default" );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> ndSelectedRoles = new ArrayList<String>();
        action.setAddNDSelectedRoles( ndSelectedRoles );

        List<String> dSelectedRoles = new ArrayList<String>();
        action.setAddDSelectedRoles( dSelectedRoles );

        assertEquals( Arrays.asList( "Continuum Group Project Administrator", "Project Administrator - default" ),
                      rbacManager.getUserAssignment( "user2" ).getRoleNames() );

        assertEquals( Action.SUCCESS, action.edituser() );

        assertTrue( rbacManager.getUserAssignment( "user2" ).getRoleNames().isEmpty() );
    }

    /**
     * Check that a configured struts2 redback app only removes roles configured for the app. Without this, redback
     * applications sharing a user database will remove each other's roles on save.
     */
    @Test
    public void testUserAdminCannotRemoveNonAppRoles()
        throws Exception
    {
        login( action, "user-admin", PASSWORD );

        // Create a role that isn't configured for apps
        String nonAppRoleName = "Other App Role";
        Role nonAppRole = rbacManager.createRole( nonAppRoleName );
        rbacManager.saveRole( nonAppRole );

        rbacManager.removeUserAssignment( "user2" );

        addAssignment( "user2", "Continuum Group Project Administrator" );
        addAssignment( "user2", "Project Administrator - default" );
        addAssignment( "user2", nonAppRoleName );

        // set addDSelectedRoles (dynamic --> Resource Roles) and addNDSelectedRoles (non-dynamic --> Available Roles)
        List<String> ndSelectedRoles = new ArrayList<String>();
        action.setAddNDSelectedRoles( ndSelectedRoles );

        List<String> dSelectedRoles = new ArrayList<String>();
        action.setAddDSelectedRoles( dSelectedRoles );

        assertEquals(
            Arrays.asList( "Continuum Group Project Administrator", "Project Administrator - default", nonAppRoleName ),
            rbacManager.getUserAssignment( "user2" ).getRoleNames() );

        assertEquals( Action.SUCCESS, action.edituser() );

        // All roles except role from other app should be removed.
        List<String> user2roles = rbacManager.getUserAssignment( "user2" ).getRoleNames();
        assertTrue( !user2roles.contains( "Continuum Group Project Administrator" ) );
        assertTrue( !user2roles.contains( "Project Administrator - default" ) );
        assertTrue( user2roles.contains( nonAppRoleName ) );
        
        // back to initial
        rbacManager.removeRole( nonAppRole );
        rbacManager.getUserAssignment( "user2" ).removeRoleName( nonAppRoleName );
    }
}
