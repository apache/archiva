package org.apache.archiva.web.test;

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

import org.apache.archiva.web.test.parent.AbstractArchivaTest;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@Test( groups = { "userroles" })
public class UserRolesTest
	extends AbstractArchivaTest
{
    @BeforeTest
    public void setUp()
    {
        loginAsAdmin();
    }

    @AfterSuite
    public void tearDown()
    {
        goToUserManagementPage();
        String[] usernameProperties =
            new String[]{ "GUEST_USERNAME", "REGISTERED_USERNAME", "SYSAD_USERNAME", "USERADMIN_USERNAME",
                "GLOBALREPOMANAGER_USERNAME", "GLOBALREPOOBSERVER_USERNAME", "REPOMANAGER_INTERNAL_USERNAME",
                "REPOOBSERVER_INTERNAL_USERNAME" };
        for ( String p : usernameProperties )
        {
            String username = getProperty( p );

            if ( isLinkPresent( username ) )
            {
                deleteUser( username );
            }
        }
    }

    public void testBasicAddDeleteUser()
	{
		username = getProperty( "GUEST_USERNAME" );
		fullname = getProperty( "GUEST_FULLNAME" );

		createUser( username, fullname, getUserEmail(), getUserRolePassword(), true);
		deleteUser( username );
	}

	@Test (dependsOnMethods = { "testBasicAddDeleteUser" } )
	public void testUserWithGuestRole()
	{
		username = getProperty("GUEST_USERNAME");
		fullname = getProperty("GUEST_FULLNAME");

		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );
		clickLinkWithText( "Edit Roles" );
		checkUserRoleWithValue( fullname );
		clickButtonWithValue( "Submit" );

        logout();
        login(username, getUserRolePassword());
		changePassword( getUserRolePassword(), getUserRoleNewPassword());

		// this section will be removed if issue from redback after changing password will be fixed.
		getSelenium().goBack();
        logout();
        //assertTextPresent("You are already logged in.");

		login(username, getUserRoleNewPassword());
		assertLeftNavMenuWithRole( fullname );
        logout();
        login( getAdminUsername() , getAdminPassword() );
	}

	@Test (dependsOnMethods = { "testUserWithGuestRole" } )
	public void testUserWithRegisteredUserRole()
	{
		username = getProperty("REGISTERED_USERNAME");
		fullname = getProperty("REGISTERED_FULLNAME");

		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );
		clickLinkWithText( "Edit Roles" );
		checkUserRoleWithValue( fullname );
		clickButtonWithValue( "Submit" );

        logout();
        login(username, getUserRolePassword());
		changePassword( getUserRolePassword(), getUserRoleNewPassword());

		// this section will be removed if issue from redback after changing password will be fixed.
		getSelenium().goBack();
        logout();
        //assertTextPresent("You are already logged in.");

		login(username, getUserRoleNewPassword());
		assertLeftNavMenuWithRole( fullname );
        logout();
        login( getAdminUsername() , getAdminPassword() );
	}

	@Test (dependsOnMethods = { "testUserWithRegisteredUserRole" } )
	public void testUserWithSysAdminUserRole()
	{
		username = getProperty("SYSAD_USERNAME");
		fullname = getProperty("SYSAD_FULLNAME");

		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );
		clickLinkWithText( "Edit Roles" );
		checkUserRoleWithValue( fullname );
		clickButtonWithValue( "Submit" );

        logout();
        login(username, getUserRolePassword());
		changePassword( getUserRolePassword(), getUserRoleNewPassword());

		// this section will be removed if issue from redback after changing password will be fixed.
		getSelenium().goBack();
        logout();
        //assertTextPresent("You are already logged in.");

		login(username, getUserRoleNewPassword());
		assertLeftNavMenuWithRole( fullname );
        logout();
        login( getAdminUsername() , getAdminPassword() );
	}

	@Test (dependsOnMethods = { "testUserWithSysAdminUserRole" } )
	public void testUserWithUserAdminUserRole()
	{
		username = getProperty("USERADMIN_USERNAME");
		fullname = getProperty("USERADMIN_FULLNAME");

		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );
		clickLinkWithText( "Edit Roles" );
		checkUserRoleWithValue( fullname );
		clickButtonWithValue( "Submit" );

        logout();
        login(username, getUserRolePassword());
		changePassword( getUserRolePassword(), getUserRoleNewPassword());

		// this section will be removed if issue from redback after changing password will be fixed.
		getSelenium().goBack();
        logout();
        //assertTextPresent("You are already logged in.");

		login(username, getUserRoleNewPassword());
		assertLeftNavMenuWithRole( fullname );
        logout();
        login( getAdminUsername() , getAdminPassword() );
	}

	@Test (dependsOnMethods = { "testUserWithUserAdminUserRole" } )
	public void testUserWithGlobalRepoManagerRole()
	{
		username = getProperty("GLOBALREPOMANAGER_USERNAME");
		fullname = getProperty("GLOBALREPOMANAGER_FULLNAME");

		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );
		clickLinkWithText( "Edit Roles" );
		checkUserRoleWithValue( fullname );
		clickButtonWithValue( "Submit" );

        logout();
        login(username, getUserRolePassword());
		changePassword( getUserRolePassword(), getUserRoleNewPassword());

		// this section will be removed if issue from redback after changing password will be fixed.
		getSelenium().goBack();
        logout();
        //assertTextPresent("You are already logged in.");

		login(username, getUserRoleNewPassword());
		assertLeftNavMenuWithRole( fullname );
        logout();
        login( getAdminUsername() , getAdminPassword() );
	}

	@Test (dependsOnMethods = { "testUserWithGlobalRepoManagerRole" } )
	public void testUserWithGlobalRepoObserverRole()
	{
		username = getProperty("GLOBALREPOOBSERVER_USERNAME");
		fullname = getProperty("GLOBALREPOOBSERVER_FULLNAME");

		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );
		clickLinkWithText( "Edit Roles" );
		checkUserRoleWithValue( fullname );
		clickButtonWithValue( "Submit" );

        logout();
        login(username, getUserRolePassword());
		changePassword( getUserRolePassword(), getUserRoleNewPassword());

		// this section will be removed if issue from redback after changing password will be fixed.
		getSelenium().goBack();
        logout();
        //assertTextPresent("You are already logged in.");

		login(username, getUserRoleNewPassword());
		assertLeftNavMenuWithRole( fullname );
        logout();
        login( getAdminUsername() , getAdminPassword() );
	}

	@Test (dependsOnMethods = { "testUserWithGlobalRepoObserverRole" } )
	public void testUserWithRepoManagerInternalRole()
	{
		username = getProperty("REPOMANAGER_INTERNAL_USERNAME");
		fullname = getProperty("REPOMANAGER_INTERNAL_FULLNAME");

        createUserWithRole( username, fullname, getUserEmail(), getUserRolePassword() );

        logout();
        login(username, getUserRolePassword());
		changePassword( getUserRolePassword(), getUserRoleNewPassword());

		// this section will be removed if issue from redback after changing password will be fixed.
		getSelenium().goBack();
        logout();
        //assertTextPresent("You are already logged in.");

		login(username, getUserRoleNewPassword());
		assertLeftNavMenuWithRole( fullname );
        logout();
        login( getAdminUsername() , getAdminPassword() );
	}

    /*@Test (dependsOnMethods = { "testUserWithRepoManagerInternalRole" } )
	public void testUserWithRepoManagerSnapshotsRole()
	{
		username = getProperty("REPOMANAGER_SNAPSHOTS_USERNAME");
		fullname = getProperty("REPOMANAGER_SNAPSHOTS_FULLNAME");
		
		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );
		clickLinkWithText( "Edit Roles" );
		checkResourceRoleWithValue( fullname );
		clickButtonWithValue( "Submit" );
		
		clickLinkWithText("Logout");
		login(username, getUserRolePassword());
		changePassword( getUserRolePassword(), getUserRoleNewPassword());
		
		// this section will be removed if issue from redback after changing password will be fixed.
		getSelenium().goBack();
		clickLinkWithText("Logout");
		//assertTextPresent("You are already logged in.");
		
		login(username, getUserRoleNewPassword());
		assertLeftNavMenuWithRole( fullname );
		clickLinkWithText("Logout");
		login( getAdminUsername() , getAdminPassword() );
	}*/

	@Test (dependsOnMethods = { "testUserWithRepoManagerInternalRole" } )
	public void testUserWithRepoObserverInternalRole()
	{
		username = getProperty( "REPOOBSERVER_INTERNAL_USERNAME" );
		fullname = getProperty( "REPOOBSERVER_INTERNAL_FULLNAME" );

        createUserWithRole( username, fullname, getUserEmail(), getUserRolePassword() );

        logout();
        login(username, getUserRolePassword());
		changePassword( getUserRolePassword(), getUserRoleNewPassword());

		// this section will be removed if issue from redback after changing password will be fixed.
		getSelenium().goBack();
        logout();
        //assertTextPresent("You are already logged in.");

		login(username, getUserRoleNewPassword());
		assertLeftNavMenuWithRole( fullname );
        logout();
        login( getAdminUsername() , getAdminPassword() );
	}

    /*@Test (dependsOnMethods = { "testUserWithRepoObserverInternalRole" } )
     public void testUserWithRepoObserverSnapshotsRole()
     {
         username = getProperty( "REPOOBSERVER_SNAPSHOTS_USERNAME" );
         fullname = getProperty( "REPOOBSERVER_SNAPSHOTS_FULLNAME" );

         createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
         clickLinkWithText( username );
         clickLinkWithText( "Edit Roles" );
         checkResourceRoleWithValue( fullname );
         clickButtonWithValue( "Submit" );

         clickLinkWithText("Logout");
         login(username, getUserRolePassword());
         changePassword( getUserRolePassword(), getUserRoleNewPassword());

         // this section will be removed if issue from redback after changing password will be fixed.
         getSelenium().goBack();
         clickLinkWithText("Logout");
         //assertTextPresent("You are already logged in.");

         login(username, getUserRoleNewPassword());
         assertLeftNavMenuWithRole( fullname );
         clickLinkWithText("Logout");
         login( getAdminUsername() , getAdminPassword() );
     }*/
}
