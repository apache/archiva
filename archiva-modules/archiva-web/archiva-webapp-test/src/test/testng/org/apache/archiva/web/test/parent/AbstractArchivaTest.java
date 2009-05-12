package org.apache.archiva.web.test.parent;

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

public abstract class AbstractArchivaTest 
	extends AbstractSeleniumTest
{

	public void assertCreateAdmin()
	{
			assertPage( "Apache Archiva \\ Create Admin User" );
			assertTextPresent( "Username" );
        	assertFieldValue( "admin", "user.username" );
       		assertTextPresent( "Full Name*" );
       		assertElementPresent( "user.fullName" );
      		assertTextPresent( "Email Address*" );
        	assertElementPresent( "user.email");
        	assertTextPresent( "Password*" );
        	assertElementPresent( "user.password" );
        	assertTextPresent( "Confirm Password*"	 );
        	assertElementPresent( "user.confirmPassword" );
        	assertButtonWithValuePresent( "Create Admin" );
	}
	
	public void submitAdminData( String fullname, String email, String password )
    	{
        	setFieldValue( "user.fullName", fullname );
        	setFieldValue( "user.email", email );
        	setFieldValue( "user.password", password );
       	 	setFieldValue( "user.confirmPassword", password );
        	submit();
    	}
	
	//Go to Login Page
	public void goToLoginPage()
	{
		getSelenium().open( baseUrl );
		clickLinkWithText( "Login");
		assertLoginPage();
	}
	
	public void submitUserData( String username, String password, boolean rememberme, boolean success )
    	{

        	setFieldValue( "username", username );
        	setFieldValue( "password", password );
        	if ( rememberme )
        	{
            		checkField( "rememberMe" );
        	}
        	
		submit();
        	if ( success )
        	{
        	    	assertAuthenticatedPage( username );
        	}
        	else
        	{
        	    assertLoginPage();
        	}
    	}
	
	public void assertLoginPage()
	{
			assertPage( "Apache Archiva \\ Login Page" );
        	assertTextPresent( "Login" );
        	assertTextPresent( "Register" );
        	assertTextPresent( "Username" );
        	assertElementPresent( "username" );
        	assertTextPresent( "Password" );
        	assertElementPresent( "password" );
        	assertTextPresent( "Remember Me" );
        	assertElementPresent( "rememberMe" );
        	assertButtonWithValuePresent( "Login" );
        	assertButtonWithValuePresent( "Cancel" );
        	assertTextPresent( "Need an Account? Register!" );
        	assertTextPresent( "Forgot your Password? Request a password reset." );
	}
	
	public void assertAuthenticatedPage( String username )
	{
		assertTextPresent( "Current User" );
		assertTextPresent( "Edit Details" );
		assertTextPresent( "Logout" );
		assertTextNotPresent( "Login" );
		assertTextPresent( username );
	}
	
	
/*	//Find Artifact
	public void goToFindArtifactPage()
	{
		clickLinkWithText( "Find Artifact" );
		assertFindArtifactPage();
	}
	
	public void assertFindArtifactPage()
	{
		//assertPage( "Apache Archiva \\ Find Artifact" );
		assertTextPresent( "Find Artifact" );
		assertTextPresent( "Search For" );
		assertElementPresent( "f" );
		assertTextPresent( "Checksum" );
		assertElementPresent( "q" );
		assertButtonWithValuePresent( "Search" );
		assertTextPresent( "This allows you to search the repository using the checksum of an artifact that you are trying to identify. You can either specify the checksum to look for directly, or scan a local artifact file. " );
		assertTextPresent( "Tï scan a local file, select the file you would like to locate in the remote repository. Ôhe entire file will not  be uploaded$to the server. See the progress bar below for progress of locally creating a checksum that is uploaded to the server ifter you hit ");
	}
	

	//User Management
	public void goToUserManagementPage()
	{
		clickLinkWithText( "User Management" );
		assertUserManagementPage();
	}
	
	public void assertUserManagementPage()
	{
		//assertPage( "Apache Archiva \\ [Admin] User List" );
		assertTextPresent( "[Admin] List of Users in Role: Any" );
		assertTextPresent( "Navigation" );
		assertImgWithAlt( "First" );
		assertImgWithAlt( "Prev" );
		assertImgWithAlt( "Next" );
		assertImgWithAlt( "Last" );
		assertTextPresent( "Display Rows" );
		assertTextPresent( "Username" );
		assertTextPresent( "Full Name" );
		assertTextPresent( "Email" );
		assertTextPresent( "Permanent" );
		assertTextPresent( "Validated" );
		assertTextPresent( "Locked" );
		assertTextPresent( "Tasks" );
		assertTextPresent( "Tools" );
		assertTextPresent( "Tasks" );
		assertTextPresent( "The following tools are available for administrators to manipulate the user list." );
		assertButtonWithValuePresent( "Create New User" );
		assertButtonWithValuePresent( "Show Users In Role" );
		assertElementPresent( "roleName" );
		assertTextPresent( "Reports" );
		assertTextPresent( "Name" );
		assertTextPresent( "Types" );
		assertTextPresent( "User List" );
		assertTextPresent( "Roles Matrix" );
	}
	
	//User Role
	public void goToUserRolesPage()
	{
		clickLinkWithText( "User Roles" );
		assertUserRolesPage();
	}
	
	public void assertUserRolesPage()
	{
		//assertPage( "Apache Archiva \\ [Admin] Role List" );
		assertTextPresent( "[Admin] Role List" );
		assertTextPresent( "Role Name" );
		assertTextPresent( "Role Description" );
		String userRoles = "Guest,Registered User,System Administrator,User Administrator,Global Repository Observer,Archiva Guest,Archiva System Administrator,Global Repository Manager,Archiva User Administrator,Repository Observer - internal,Repository Manager - internal,Repository Observer - snapshots,Repository Manager - snapshots";
		String[] arrayRole = userRoles.split( "," );
		for ( String userroles : arrayRole )
			assertLinkPresent( userroles );
	}
	
	//Appearance
	public void goToAppearancePage()
	{
		clickLinkWithText( "Appearance" );
		assertAppearancePage();
	}
	
	public void assertAppearancePage()
	{
		//assertPage( "Apache Archiva \\ Configure Appearance" );
		String appearance = "Appearance,Organization Details,The logo in the top right of the screen is controlled by the following settings.,Organizations Information,Name,URL,Logo URL";
		String[] arrayAppearance = appearance.split( "," );
		for ( String appear : arrayAppearance )
			assertTextPresent( appear );
		assertLinkPresent( "Edit" );
		assertLinkPresent( "Change your appearance" );
	}
	
	//Upload Artifact
	public void goToUploadArtifactPage()
	{
		clickLinkWithText( "Upload Artifact" );
		assertUploadArtifactPage();
	}
	
	public void assertUploadArtifactPage()
	{
		//assertPage( "Apache Archiva \\ Upload Artifact" );
		String uploadArtifact = "Upload Artifact,Group Id*,Artifact Id*,Version*,Packaging*,Classifier,Generate Maven 2 POM,Artifact File*,POM File,Repository Id";
		String[] arrayUploadArtifact = uploadArtifact.split( "," );
		for ( String uploadartifact : arrayUploadArtifact )
			assertTextPresent( uploadartifact );
		String uploadElements = "groupId,artifactId,version,packaging,classifier,generatePom,artifact,pom,repositoryId";
		String[] arrayUploadElements = uploadElements.split( "," );
		for ( String uploadelements : arrayUploadElements )
			assertElementPresent( uploadelements );
		assertButtonWithValuePresent( "Submit" );
	} */
}
