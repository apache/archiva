package org.apache.archiva.web.test.parent;

import org.apache.archiva.web.test.XPathExpressionUtil;

import java.io.File;
import java.io.IOException;

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
    protected String username;

    protected String fullname;

    public String getUserEmail()
    {
        String email = getProperty( "USERROLE_EMAIL" );
        return email;
    }

    public String getUserRolePassword()
    {
        String password = getProperty( "USERROLE_PASSWORD" );
        return password;
    }

    public String getUserRoleNewPassword()
    {
        String password_new = getProperty( "NEW_USERROLE_PASSWORD" );
        return password_new;
    }

    public String getBasedir()
    {
        String basedir = System.getProperty( "basedir" );

        if ( basedir == null )
        {
            basedir = new File( "" ).getAbsolutePath();
        }

        return basedir;
    }

    public String getAdminUsername()
    {
        String adminUsername = getProperty( "ADMIN_USERNAME" );
        return adminUsername;
    }

    public String getAdminPassword()
    {
        String adminPassword = getProperty( "ADMIN_PASSWORD" );
        return adminPassword;
    }

    public void assertCreateAdmin()
    {
        assertElementPresent( "adminCreateForm" );
        //assertPage( "Apache Archiva \\ Create Admin User" );
        //assertTextPresent( "Username" );
        assertFieldValue( "admin", "user.username" );
        //assertTextPresent( "Full Name*" );
        assertElementPresent( "user.fullName" );
        //assertTextPresent( "Email Address*" );
        assertElementPresent( "user.email" );
        //assertTextPresent( "Password*" );
        assertElementPresent( "user.password" );
        //assertTextPresent( "Confirm Password*" );
        assertElementPresent( "user.confirmPassword" );
        //assertButtonWithValuePresent( "Create Admin" );
    }

    public void submitAdminData( String fullname, String email, String password )
    {
        setFieldValue( "user.fullName", fullname );
        setFieldValue( "user.email", email );
        setFieldValue( "user.password", password );
        setFieldValue( "user.confirmPassword", password );
        submit();
    }

    // Go to Login Page
    public void goToLoginPage()
    {
        getSelenium().open( baseUrl );
        // are we already logged in ?
        if ( isElementPresent( "logoutLink" ) )
        {
            // so logout
            clickLinkWithLocator( "logoutLink" );
            clickLinkWithLocator( "loginLink" );
        }
        else if ( isElementPresent( "loginLink" ) )
        {
            clickLinkWithLocator( "loginLink" );
        }
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
            assertUserLoggedIn( username );
        }
        else
        {
            assertLoginPage();
        }
    }

    public void assertLoginPage()
    {
        //assertPage( "Apache Archiva \\ Login Page" );
        //assertTextPresent( "Login" );
        assertElementPresent( "loginForm" );
        //assertTextPresent( "Register" );
        assertElementPresent( "registerLink" );
        //assertTextPresent( "Username" );
        assertElementPresent( "username" );
        //assertTextPresent( "Password" );
        assertElementPresent( "password" );
        //assertTextPresent( "Remember Me" );
        assertElementPresent( "rememberMe" );
        //assertButtonWithValuePresent( "Login" );
        assertButtonWithIdPresent( "loginSubmit" );
        //assertButtonWithValuePresent( "Cancel" );
        assertButtonWithIdPresent( "loginCancel" );
        //assertTextPresent( "Need an Account? Register!" );
        //assertTextPresent( "Forgot your Password? Request a password reset." );
        assertElementPresent( "registerLinkLoginPage" );
        assertElementPresent( "forgottenPasswordLink" );
    }

    // User Management
    public void goToUserManagementPage()
    {
        getSelenium().open( "/archiva/security/userlist.action" );
        assertUserManagementPage();
    }

    public void assertUserManagementPage()
    {
        assertPage( "Apache Archiva \\ [Admin] User List" );
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

    /*
     * //User Role public void goToUserRolesPage() { clickLinkWithText( "User Roles" ); assertUserRolesPage(); }
     */

    public void assertUserRolesPage()
    {
        assertPage( "Apache Archiva \\ [Admin] User Edit" );
        assertTextPresent( "[Admin] User Roles" );
        assertTextPresent( "Username" );
        assertTextPresent( "Full Name" );
        String userRoles =
            "Guest,Registered User,System Administrator,User Administrator,Global Repository Observer,Global Repository Manager,Repository Observer,Repository Manager,internal";
        String[] arrayRole = userRoles.split( "," );
        for ( String userroles : arrayRole )
        {
            assertTextPresent( userroles );
        }
    }

    public void assertDeleteUserPage( String username )
    {
        assertPage( "Apache Archiva \\ [Admin] User Delete" ); // TODO
        assertTextPresent( "[Admin] User Delete" );
        assertTextPresent( "The following user will be deleted:" );
        assertTextPresent( "Username: " + username );
        assertButtonWithValuePresent( "Delete User" );
    }

    public void createUser( String userName, String fullName, String email, String password, boolean valid )
    {
        createUser( userName, fullName, email, password, password, valid );
    }

    private void createUser( String userName, String fullName, String emailAd, String password, String confirmPassword,
                             boolean valid )
    {
        // login( getAdminUsername() , getAdminPassword() );
        getSelenium().open( "/archiva/security/userlist.action" );
        clickButtonWithValue( "Create New User" );
        assertCreateUserPage();
        setFieldValue( "user.username", userName );
        setFieldValue( "user.fullName", fullName );
        setFieldValue( "user.email", emailAd );
        setFieldValue( "user.password", password );
        setFieldValue( "user.confirmPassword", confirmPassword );
        submit();

        assertUserRolesPage();
        clickButtonWithValue( "Submit" );

        if ( valid )
        {
            String[] columnValues = { userName, fullName, emailAd };
            assertElementPresent( XPathExpressionUtil.getTableRow( columnValues ) );
        }
        else
        {
            assertCreateUserPage();
        }
    }

    public void deleteUser( String userName, String fullName, String emailAdd )
    {
        deleteUser( userName, fullName, emailAdd, false, false );
    }

    public void deleteUser( String userName, String fullName, String emailAd, boolean validated, boolean locked )
    {
        String[] columnValues = { userName, fullName, emailAd };
        // clickLinkWithText( "userlist" );
        clickLinkWithXPath( "//table[@id='ec_table']/tbody[2]/tr[3]/td[7]/a/img" );
        assertDeleteUserPage( userName );
        submit();
        assertElementNotPresent( XPathExpressionUtil.getTableRow( columnValues ) );
    }

    public void login( String username, String password )
    {
        login( username, password, true, "Login Page" );
    }

    public void login( String username, String password, boolean valid, String assertReturnPage )
    {
        if ( isElementPresent( "loginLink" ) )
        {
            goToLoginPage();

            submitLoginPage( username, password, false, valid, assertReturnPage );
        }
        if ( valid )
        {
            assertUserLoggedIn( username );
        }
    }

    public void submitLoginPage( String username, String password )
    {
        submitLoginPage( username, password, false, true, "Login Page" );
    }

    public void submitLoginPage( String username, String password, boolean validUsernamePassword )
    {
        submitLoginPage( username, password, false, validUsernamePassword, "Login Page" );
    }

    public void submitLoginPage( String username, String password, boolean rememberMe, boolean validUsernamePassword,
                                 String assertReturnPage )
    {
        assertLoginPage();
        setFieldValue( "username", username );
        setFieldValue( "password", password );
        if ( rememberMe )
        {
            checkField( "rememberMe" );
        }
        //clickButtonWithValue( "Login" );
        clickButtonWithLocator( "loginSubmit" );
        if ( validUsernamePassword )
        {
            assertUserLoggedIn( username );
        }
        else
        {
            if ( "Login Page".equals( assertReturnPage ) )
            {
                assertLoginPage();
            }
            else
            {
                assertPage( assertReturnPage );
            }
        }
    }

    protected void assertUserLoggedIn( String username )
    {
        //assertTextPresent( "Current User:" );
        assertTextPresent( username );
        //assertLinkPresent( "Edit Details" );
        assertElementPresent( "editUserLink" );
        assertElementPresent( "logoutLink" );
        //assertLinkPresent( "Logout" );
        //assertTextNotPresent( "Login" );
        assertElementNotPresent( "loginLink" );
    }

    // User Roles
    public void assertUserRoleCheckBoxPresent( String value )
    {
        getSelenium().isElementPresent(
            "xpath=//input[@id='addRolesToUser_addNDSelectedRoles' and @name='addNDSelectedRoles' and @value='" + value
                + "']" );
    }

    public void assertResourceRolesCheckBoxPresent( String value )
    {
        getSelenium().isElementPresent( "xpath=//input[@name='addDSelectedRoles' and @value='" + value + "']" );
    }

    public void checkUserRoleWithValue( String value )
    {
        assertUserRoleCheckBoxPresent( value );
        getSelenium().click(
            "xpath=//input[@id='addRolesToUser_addNDSelectedRoles' and @name='addNDSelectedRoles' and @value='" + value
                + "']" );
    }

    public void checkResourceRoleWithValue( String value )
    {
        assertResourceRolesCheckBoxPresent( value );
        getSelenium().click( "xpath=//input[@name='addDSelectedRoles' and @value='" + value + "']" );
    }

    public void changePassword( String oldPassword, String newPassword )
    {
        assertPage( "Apache Archiva \\ Change Password" );
        setFieldValue( "existingPassword", oldPassword );
        setFieldValue( "newPassword", newPassword );
        setFieldValue( "newPasswordConfirm", newPassword );
        clickButtonWithValue( "Change Password" );
    }

    public void assertCreateUserPage()
    {
        assertPage( "Apache Archiva \\ [Admin] User Create" );
        assertTextPresent( "[Admin] User Create" );
        assertTextPresent( "Username*:" );
        assertElementPresent( "user.username" );
        assertTextPresent( "Full Name*:" );
        assertElementPresent( "user.fullName" );
        assertTextPresent( "Email Address*:" );
        assertElementPresent( "user.email" );
        assertTextPresent( "Password*:" );
        assertElementPresent( "user.password" );
        assertTextPresent( "Confirm Password*:" );
        assertElementPresent( "user.confirmPassword" );
        assertButtonWithValuePresent( "Create User" );
    }

    public void assertLeftNavMenuWithRole( String role )
    {
        if ( role.equals( "Guest" ) || role.equals( "Registered User" ) || role.equals( "Global Repository Observer" )
            || role.equals( "Repository Observer - internal" ) || role.equals( "Repository Observer - snapshots" ) )
        {
            assertTextPresent( "Search" );
            assertLinkPresent( "Find Artifact" );
            assertLinkPresent( "Browse" );
            assertLinkNotPresent( "Repositories" );
        }
        else if ( role.equals( "User Administrator" ) )
        {
            assertTextPresent( "Search" );
            assertLinkPresent( "Find Artifact" );
            assertLinkPresent( "Browse" );
            assertLinkPresent( "User Management" );
            assertLinkPresent( "User Roles" );
            assertLinkNotPresent( "Repositories" );
        }
        else if ( role.equals( "Global Repository Manager" ) || role.equals( "Repository Manager - internal" )
            || role.equals( "Repository Manager - snapshots" ) )
        {
            assertTextPresent( "Search" );
            assertLinkPresent( "Find Artifact" );
            assertLinkPresent( "Browse" );
            assertLinkPresent( "Upload Artifact" );
            assertLinkPresent( "Delete Artifact" );
            assertLinkNotPresent( "Repositories" );
        }
        else
        {
            assertTextPresent( "Search" );
            String navMenu =
                "Find Artifact,Browse,Reports,User Management,User Roles,Appearance,Upload Artifact,Delete Artifact,Repository Groups,Repositories,Proxy Connectors,Legacy Support,Network Proxies,Repository Scanning";
            String[] arrayMenu = navMenu.split( "," );
            for ( String navmenu : arrayMenu )
            {
                assertLinkPresent( navmenu );
            }
        }
    }

    // Find Artifact
    public void goToFindArtifactPage()
    {
        getSelenium().open( "/archiva/findArtifact.action" );
        assertFindArtifactPage();
    }

    public void assertFindArtifactPage()
    {
        //assertPage( "Apache Archiva \\ Find Artifact" );
        assertElementPresent( "searchBox" );
        //assertTextPresent( "Find Artifact" );
        //assertTextPresent( "Search for:" );
        //assertTextPresent( "Checksum:" );
        assertElementPresent( "quickSearchBox" );
        assertElementPresent( "checksumSearch" );
        //assertButtonWithValuePresent( "Search" );
        assertElementPresent( "checksumSearch_0" );
    }

    // Appearance
    public void goToAppearancePage()
    {
        getSelenium().open( "/archiva/admin/configureAppearance.action" );
        assertAppearancePage();
    }

    public void assertAppearancePage()
    {
        assertPage( "Apache Archiva \\ Configure Appearance" );
        String appearance =
            "Appearance,Organization Details,The logo in the top right of the screen is controlled by the following settings.,Organization Information,Name,URL,Logo URL";
        String[] arrayAppearance = appearance.split( "," );
        for ( String appear : arrayAppearance )
        {
            assertTextPresent( appear );
        }
        assertLinkPresent( "Edit" );
        assertLinkPresent( "Change your appearance" );
    }

    public void addEditAppearance( String name, String url, String logoUrl )
    {
        setFieldValue( "organisationName", name );
        setFieldValue( "organisationUrl", url );
        setFieldValue( "organisationLogo", logoUrl );
        clickButtonWithValue( "Save" );
    }

    public void goToHomePage()
    {
        getSelenium().open( "" );
    }

    // Upload Artifact
    public void goToAddArtifactPage()
    {
        // must be logged as admin
        getSelenium().open( "/archiva/upload.action" );
        assertAddArtifactPage();
    }

    public void assertAddArtifactPage()
    {
        assertPage( "Apache Archiva \\ Upload Artifact" );
        assertTextPresent( "Upload Artifact" );

        String artifact =
            "Upload Artifact,Group Id*:,Artifact Id*:,Version*:,Packaging*:,Classifier:,Generate Maven 2 POM,Artifact File*:,POM File:,Repository Id:";
        String[] arrayArtifact = artifact.split( "," );
        for ( String arrayartifact : arrayArtifact )
        {
            assertTextPresent( arrayartifact );
        }

        String artifactElements =
            "upload_groupId,upload_artifactId,upload_version,upload_packaging,upload_classifier,upload_generatePom,upload_artifact,upload_pom,upload_repositoryId,uploadSubmit";
        String[] arrayArtifactElements = artifactElements.split( "," );
        for ( String artifactelements : arrayArtifactElements )
        {
            assertElementPresent( artifactelements );
        }
    }

    public void addArtifact( String groupId, String artifactId, String version, String packaging,
                             String artifactFilePath, String repositoryId, boolean wait )
    {
        addArtifact( groupId, artifactId, version, packaging, true, artifactFilePath, repositoryId, wait );
    }

    public void addArtifact( String groupId, String artifactId, String version, String packaging, boolean generatePom,
                             String artifactFilePath, String repositoryId, boolean wait )
    {
        login( getProperty( "ADMIN_USERNAME" ), getProperty( "ADMIN_PASSWORD" ) );
        goToAddArtifactPage();
        setFieldValue( "groupId", groupId );
        setFieldValue( "artifactId", artifactId );
        setFieldValue( "version", version );
        setFieldValue( "packaging", packaging );

        if ( generatePom )
        {
            checkField( "generatePom" );
        }

        String path;
        if ( artifactFilePath != null && artifactFilePath.trim().length() > 0 )
        {
            File f = new File( artifactFilePath );
            try
            {
                path = f.getCanonicalPath();
            }
            catch ( IOException e )
            {
                path = f.getAbsolutePath();
            }
        }
        else
        {
            path = artifactFilePath;
        }

        setFieldValue( "artifact", path );
        selectValue( "upload_repositoryId", repositoryId );

        //clickButtonWithValue( "Submit" );
        clickButtonWithLocator( "uploadSubmit", wait );
    }

    public void goToRepositoriesPage()
    {
        if ( !getTitle().equals( "Apache Archiva \\ Administration - Repositories" ) )
        {
            getSelenium().open( "/archiva/admin/repositories.action" );
        }
        assertRepositoriesPage();
    }

    public void assertRepositoriesPage()
    {
        assertPage( "Apache Archiva \\ Administration - Repositories" );
        assertTextPresent( "Administration - Repositories" );
        assertTextPresent( "Managed Repositories" );
        assertTextPresent( "Remote Repositories" );
    }

    public void addManagedRepository( String identifier, String name, String directory, String indexDirectory,
                                      String type, String cron, String daysOlder, String retentionCount )
    {
        // goToRepositoriesPage();
        // clickLinkWithText( "Add" );
        setFieldValue( "repository.id", identifier );
        setFieldValue( "repository.name", name );
        setFieldValue( "repository.location", directory );
        setFieldValue( "repository.indexDirectory", indexDirectory );
        selectValue( "repository.layout", type );
        setFieldValue( "repository.cronExpression", cron );
        setFieldValue( "repository.daysOlder", daysOlder );
        setFieldValue( "repository.retentionCount", retentionCount );
        // TODO
        clickButtonWithValue( "Add Repository" );
    }

    // artifact management
    public void assertDeleteArtifactPage()
    {
        assertPage( "Apache Archiva \\ Delete Artifact" );
        assertTextPresent( "Delete Artifact" );
        assertTextPresent( "Group Id*:" );
        assertTextPresent( "Artifact Id*:" );
        assertTextPresent( "Version*:" );
        assertTextPresent( "Repository Id:" );
        assertElementPresent( "groupId" );
        assertElementPresent( "artifactId" );
        assertElementPresent( "version" );
        assertElementPresent( "repositoryId" );
        assertButtonWithValuePresent( "Submit" );
    }

    // network proxies
    public void goToNetworkProxiesPage()
    {
        clickLinkWithText( "Network Proxies" );
        assertNetworkProxiesPage();
    }

    public void assertNetworkProxiesPage()
    {
        assertPage( "Apache Archiva \\ Administration - Network Proxies" );
        assertTextPresent( "Administration - Network Proxies" );
        assertTextPresent( "Network Proxies" );
        assertLinkPresent( "Add Network Proxy" );
    }

    public void addNetworkProxy( String identifier, String protocol, String hostname, String port, String username,
                                 String password )
    {
        //goToNetworkProxiesPage();
        clickLinkWithText( "Add Network Proxy" );
        assertAddNetworkProxy();
        setFieldValue( "proxy.id", identifier );
        setFieldValue( "proxy.protocol", protocol );
        setFieldValue( "proxy.host", hostname );
        setFieldValue( "proxy.port", port );
        setFieldValue( "proxy.username", username );
        setFieldValue( "proxy.password", password );
        clickButtonWithValue( "Save Network Proxy" );
    }

    public void assertAddNetworkProxy()
    {
        assertPage( "Apache Archiva \\ Admin: Add Network Proxy" );
        assertTextPresent( "Admin: Add Network Proxy" );
        assertTextPresent( "Add network proxy:" );
        assertTextPresent( "Identifier*:" );
        assertTextPresent( "Protocol*:" );
        assertTextPresent( "Hostname*:" );
        assertTextPresent( "Port*:" );
        assertTextPresent( "Username:" );
        assertTextPresent( "Password:" );
        assertButtonWithValuePresent( "Save Network Proxy" );
    }

    // Legacy Support
    public void goToLegacySupportPage()
    {
        getSelenium().open( "/archiva/admin/legacyArtifactPath.action" );
        assertLegacySupportPage();
    }

    public void assertLegacySupportPage()
    {
        assertPage( "Apache Archiva \\ Administration - Legacy Support" );
        assertTextPresent( "Administration - Legacy Artifact Path Resolution" );
        assertTextPresent( "Path Mappings" );
        assertLinkPresent( "Add" );
    }

    public void addLegacyArtifactPath( String path, String groupId, String artifactId, String version,
                                       String classifier, String type )
    {
        assertAddLegacyArtifactPathPage();
        setFieldValue( "legacyArtifactPath.path", path );
        setFieldValue( "groupId", groupId );
        setFieldValue( "artifactId", artifactId );
        setFieldValue( "version", version );
        setFieldValue( "classifier", classifier );
        setFieldValue( "type", type );
        clickButtonWithValue( "Add Legacy Artifact Path" );
    }

    public void assertAddLegacyArtifactPathPage()
    {
        assertPage( "Apache Archiva \\ Admin: Add Legacy Artifact Path" );
        assertTextPresent( "Admin: Add Legacy Artifact Path" );
        assertTextPresent(
            "Enter the legacy path to map to a particular artifact reference, then adjust the fields as necessary." );
        String element =
            "addLegacyArtifactPath_legacyArtifactPath_path,addLegacyArtifactPath_groupId,addLegacyArtifactPath_artifactId,addLegacyArtifactPath_version,addLegacyArtifactPath_classifier,addLegacyArtifactPath_type";
        String[] arrayElement = element.split( "," );
        for ( String arrayelement : arrayElement )
        {
            assertElementPresent( arrayelement );
        }
        assertButtonWithValuePresent( "Add Legacy Artifact Path" );
    }

    // add managed repository and its staging repository
    public void addStagingRepository( String identifier, String name, String directory, String indexDirectory,
                                      String type, String cron, String daysOlder, String retentionCount )
    {
        setFieldValue( "repository.id", identifier );
        setFieldValue( "repository.name", name );
        setFieldValue( "repository.location", directory );
        setFieldValue( "repository.indexDirectory", indexDirectory );
        selectValue( "repository.layout", type );
        setFieldValue( "repository.cronExpression", cron );
        setFieldValue( "repository.daysOlder", daysOlder );
        setFieldValue( "repository.retentionCount", retentionCount );
        checkField( "stageNeeded" );

        clickButtonWithValue( "Add Repository" );
    }

    protected void logout()
    {
        clickLinkWithText( "Logout" );
        assertTextNotPresent( "Current User:" );
        assertLinkNotPresent( "Edit Details" );
        assertLinkNotPresent( "Logout" );
        assertLinkPresent( "Login" );
    }
}
