package org.apache.archiva.web.test.parent;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

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

    @Override
    @AfterTest
    public void close()
        throws Exception
    {
        super.close();
    }

    @Override
    @BeforeSuite
    public void open()
        throws Exception
    {
        super.open();
    }

    public void assertAdminCreated()
        throws Exception
    {
        initializeArchiva( System.getProperty( "baseUrl" ), System.getProperty( "browser" ),
                           Integer.getInteger( "maxWaitTimeInMs" ), System.getProperty( "seleniumHost" ),
                           Integer.getInteger( "seleniumPort" ) );
    }

    @BeforeTest
    @Parameters( { "baseUrl", "browser", "maxWaitTimeInMs", "seleniumHost", "seleniumPort" } )
    public void initializeArchiva( String baseUrl, String browser, int maxWaitTimeInMs,
                                   @Optional( "localhost" ) String seleniumHost, @Optional( "4444" ) int seleniumPort )
        throws Exception
    {

        super.open( baseUrl, browser, seleniumHost, seleniumPort, Integer.toString( maxWaitTimeInMs ) );

        getSelenium().open( baseUrl );

        waitPage();

        // if not admin user created create one
        if ( isElementVisible( "create-admin-link" ) )
        {
            Assert.assertFalse( getSelenium().isVisible( "login-link-a" ) );
            Assert.assertFalse( getSelenium().isVisible( "register-link-a" ) );
            clickLinkWithLocator( "create-admin-link-a", false );
            assertCreateAdmin();
            String fullname = getProperty( "ADMIN_FULLNAME" );
            String username = getAdminUsername();
            String mail = getProperty( "ADMIN_EMAIL" );
            String password = getProperty( "ADMIN_PASSWORD" );
            submitAdminData( fullname, mail, password );
            assertUserLoggedIn( username );
            clickLinkWithLocator( "logout-link-a" );
        }
        else
        {
            Assert.assertTrue( getSelenium().isVisible( "login-link-a" ) );
            Assert.assertTrue( getSelenium().isVisible( "register-link-a" ) );
            login( getAdminUsername(), getAdminPassword() );
        }

    }

    protected static String getErrorMessageText()
    {
        return getSelenium().getText( "//ul[@class='errorMessage']/li/span" );
    }

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
        assertElementPresent( "user-create" );
        assertFieldValue( "admin", "username" );
        assertElementPresent( "fullname" );
        assertElementPresent( "password" );
        assertElementPresent( "confirmPassword" );
        assertElementPresent( "email" );
    }

    public void submitAdminData( String fullname, String email, String password )
    {
        setFieldValue( "fullname", fullname );
        setFieldValue( "email", email );
        setFieldValue( "password", password );
        setFieldValue( "confirmPassword", password );
        clickButtonWithLocator( "user-create-form-register-button" );
        //submit();
    }

    // Go to Login Page
    public void goToLoginPage()
    {
        getSelenium().open( baseUrl );
        waitPage();
        // are we already logged in ?
        if ( isElementVisible( "logout-link" ) ) //isElementPresent( "logoutLink" ) )
        {
            // so logout
            clickLinkWithLocator( "logout-link-a", false );
            clickLinkWithLocator( "login-link-a" );
        }
        else if ( isElementVisible( "login-link-a" ) )
        {
            clickLinkWithLocator( "login-link-a" );
        }
        assertLoginModal();
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
            assertLoginModal();
        }
    }

    public void assertLoginModal()
    {
        assertElementPresent( "user-login-form" );
        Assert.assertTrue( isElementVisible( "register-link" ) );
        assertElementPresent( "user-login-form-username" );
        assertElementPresent( "user-login-form-password" );
        assertButtonWithIdPresent( "modal-login-ok" );
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
        //assertPage( "Apache Archiva \\ [Admin] User Edit" );
        //[Admin] Rôles de l'utilisateur

        assertTextPresent( "[Admin] User Roles", "[Admin] R\u00F4les de l'utilisateur" );
        assertTextPresent( "Username", "Nom d'utilisateur" );
        assertTextPresent( "Full Name", "Nom complet" );
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
        assertTextPresent( "[Admin] User Delete", "[Admin] Suppression de l'utilisateur",
                           "L'utilisateur suivant va \u00EAtre supprim\u00E9:" );
        assertTextPresent( "The following user will be deleted:" );
        assertTextPresent( "Username: " + username, "Nom d'utilisateur:" + username );
        assertButtonWithIdPresent( "userDeleteSubmit" );
    }

    public void createUser( String userName, String fullName, String email, String password, boolean valid )
    {
        createUser( userName, fullName, email, password, password, valid );
    }

    private void createUser( String userName, String fullName, String emailAd, String password, String confirmPassword,
                             boolean valid )
    {

        clickLinkWithLocator( "menu-users-list-a", true );
        clickLinkWithLocator( "users-view-tabs-li-user-edit-a", true );

        assertCreateUserPage();
        setFieldValue( "username", userName );
        setFieldValue( "fullname", fullName );
        setFieldValue( "email", emailAd );
        setFieldValue( "password", password );
        setFieldValue( "confirmPassword", confirmPassword );

        clickLinkWithLocator( "user-create-form-register-button", true );

        assertTextPresent( "User " + userName + " created." );
        assertElementPresent( "users-grid-user-id-" + userName );

        if ( valid )
        {
            //String[] columnValues = { userName, fullName, emailAd };
            //assertElementPresent( XPathExpressionUtil.getTableRow( columnValues ) );

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
        clickLinkWithLocator( "menu-users-list-a", true );
        assertTextPresent( userName );
        assertTextPresent( fullName );

        clickLinkWithLocator( "users-grid-delete-" + userName );

        clickLinkWithLocator( "dialog-confirm-modal-ok" );
        assertTextPresent( "User " + userName + " deleted." );

        clickLinkWithLocator( "alert-message-success-close-a" );

        assertElementNotPresent( "users-grid-user-id-" + userName );
        assertTextNotPresent( fullName );


    }

    public void login( String username, String password )
    {
        login( username, password, true, "Login Page" );
    }

    public void login( String username, String password, boolean valid, String assertReturnPage )
    {
        if ( isElementVisible( "login-link-a" ) )//isElementPresent( "loginLink" ) )
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
        clickLinkWithLocator( "login-link-a", false );
        setFieldValue( "user-login-form-username", username );
        setFieldValue( "user-login-form-password", password );
        /*
        if ( rememberMe )
        {
            checkField( "rememberMe" );
        }*/

        clickButtonWithLocator( "modal-login-ok" );
        if ( validUsernamePassword )
        {
            assertUserLoggedIn( username );
        }
        /*
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
        }*/
    }

    protected void assertUserLoggedIn( String username )
    {
        Assert.assertFalse( isElementVisible( "login-link" ) );
        Assert.assertTrue( isElementVisible( "logout-link" ) );
        Assert.assertFalse( isElementVisible( "register-link" ) );
        Assert.assertFalse( isElementVisible( "create-admin-link" ) );
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
        assertTextPresent( "Username" );
        assertElementPresent( "username" );
        assertTextPresent( "Full Name" );
        assertElementPresent( "fullname" );
        assertTextPresent( "Email Address" );
        assertElementPresent( "email" );
        assertTextPresent( "Password" );
        assertElementPresent( "password" );
        assertTextPresent( "Confirm Password" );
        assertElementPresent( "confirmPassword" );
        assertButtonWithIdPresent( "user-create-form-register-button" );

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

    public void addEditAppearance( String name, String url, String logoUrl, boolean wait )
    {
        setFieldValue( "organisationName", name );
        setFieldValue( "organisationUrl", url );
        setFieldValue( "organisationLogo", logoUrl );
        clickButtonWithValue( "Save", wait );
    }

    public void goToHomePage()
    {
        getSelenium().open( baseUrl );
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
                                      String type, String cron, String daysOlder, String retentionCount, boolean wait )
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
        clickButtonWithValue( "Add Repository", wait );
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
                                       String classifier, String type, boolean wait )
    {
        assertAddLegacyArtifactPathPage();
        setFieldValue( "legacyArtifactPath.path", path );
        setFieldValue( "groupId", groupId );
        setFieldValue( "artifactId", artifactId );
        setFieldValue( "version", version );
        setFieldValue( "classifier", classifier );
        setFieldValue( "type", type );
        clickButtonWithValue( "Add Legacy Artifact Path", wait );
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
        assertLinkNotVisible( "Edit Details" );
        assertLinkNotVisible( "Logout" );
        assertLinkVisible( "Login" );
    }

    protected String getAdminUserName()
    {
        return getProperty( "ADMIN_FULLNAME" );
    }
}