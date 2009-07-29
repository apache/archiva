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
import org.testng.annotations.Test;

@Test( groups = { "userroles" }, dependsOnMethods = { "testWithCorrectUsernamePassword" } )
public class UserRolesTest 
	extends AbstractArchivaTest
{

	public void testBasicAddDeleteUser()
	{
		username = p.getProperty( "GUEST_USERNAME" );
		fullname = p.getProperty( "GUEST_FULLNAME" );
		
		createUser( username, fullname, getUserEmail(), getUserRolePassword(), true);
		deleteUser( username, fullname, getUserEmail() );
		clickLinkWithText( "Logout" );
		login( getAdminUsername() , getAdminPassword() );
	}
	
	@Test (dependsOnMethods = { "testBasicAddDeleteUser" } )
	public void testUserWithGuestRole()
	{
		username = p.getProperty("GUEST_USERNAME");
		fullname = p.getProperty("GUEST_FULLNAME");
		
		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );
		clickLinkWithText( "Edit Roles" );
		checkUserRoleWithValue( fullname );
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
	}
	
	@Test (dependsOnMethods = { "testUserWithGuestRole" } )
	public void testUserWithRegisteredUserRole()
	{
		username = p.getProperty("REGISTERED_USERNAME");
		fullname = p.getProperty("REGISTERED_FULLNAME");
		
		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );	
		clickLinkWithText( "Edit Roles" );
		checkUserRoleWithValue( fullname );
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
	}
	
	@Test (dependsOnMethods = { "testUserWithRegisteredUserRole" } )
	public void testUserWithSysAdminUserRole()
	{
		username = p.getProperty("SYSAD_USERNAME");
		fullname = p.getProperty("SYSAD_FULLNAME");
		
		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );
		clickLinkWithText( "Edit Roles" );
		checkUserRoleWithValue( fullname );
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
	}
	
	@Test (dependsOnMethods = { "testUserWithSysAdminUserRole" } )
	public void testUserWithUserAdminUserRole()
	{
		username = p.getProperty("USERADMIN_USERNAME");
		fullname = p.getProperty("USERADMIN_FULLNAME");
		
		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );
		clickLinkWithText( "Edit Roles" );
		checkUserRoleWithValue( fullname );
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
	}
	
	@Test (dependsOnMethods = { "testUserWithUserAdminUserRole" } )
	public void testUserWithGlobalRepoManagerRole()
	{
		username = p.getProperty("GLOBALREPOMANAGER_USERNAME");
		fullname = p.getProperty("GLOBALREPOMANAGER_FULLNAME");
		
		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );
		clickLinkWithText( "Edit Roles" );
		checkUserRoleWithValue( fullname );
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
	}

	@Test (dependsOnMethods = { "testUserWithGlobalRepoManagerRole" } )
	public void testUserWithGlobalRepoObserverRole()
	{
		username = p.getProperty("GLOBALREPOOBSERVER_USERNAME");
		fullname = p.getProperty("GLOBALREPOOBSERVER_FULLNAME");
		
		createUser(username, fullname, getUserEmail(), getUserRolePassword(), true);
		clickLinkWithText( username );
		clickLinkWithText( "Edit Roles" );
		checkUserRoleWithValue( fullname );
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
	}
	
	@Test (dependsOnMethods = { "testUserWithGlobalRepoObserverRole" } )
	public void testUserWithRepoManagerInternalRole()
	{
		username = p.getProperty("REPOMANAGER_INTERNAL_USERNAME");
		fullname = p.getProperty("REPOMANAGER_INTERNAL_FULLNAME");
		
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
	}
	
	/*@Test (dependsOnMethods = { "testUserWithRepoManagerInternalRole" } )
	public void testUserWithRepoManagerSnapshotsRole()
	{
		username = p.getProperty("REPOMANAGER_SNAPSHOTS_USERNAME");
		fullname = p.getProperty("REPOMANAGER_SNAPSHOTS_FULLNAME");
		
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
		username = p.getProperty( "REPOOBSERVER_INTERNAL_USERNAME" );
		fullname = p.getProperty( "REPOOBSERVER_INTERNAL_FULLNAME" );
		
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
	}
	
	/*@Test (dependsOnMethods = { "testUserWithRepoObserverInternalRole" } )
	public void testUserWithRepoObserverSnapshotsRole()
	{
		username = p.getProperty( "REPOOBSERVER_SNAPSHOTS_USERNAME" );
		fullname = p.getProperty( "REPOOBSERVER_SNAPSHOTS_FULLNAME" );
		
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
