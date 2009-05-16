package org.apache.archiva.web.test;

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
	}

	@Test (dependsOnMethods = { "testUserWithUserAdminUserRole" } )
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
	}
	
	@Test (dependsOnMethods = { "testUserWithGlobalRepoManagerRole" } )
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
	}
	
	@Test (dependsOnMethods = { "testUserWithGlobalRepoManagerRole" } )
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
	}
	
	@Test (dependsOnMethods = { "testUserWithGlobalRepoObserverRole" } )
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
	}
	
	@Test (dependsOnMethods = { "testUserWithGlobalRepoObserverRole" } )
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
	}
}
