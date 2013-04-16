package org.apache.archiva.web.test.parent;

import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

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

@RunWith( BlockJUnit4ClassRunner.class )
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

    // User Management

    public void createUser( String userName, String fullName, String email, String password, boolean valid )
    {
        createUser( userName, fullName, email, password, password, valid );
    }

    private void createUser( String userName, String fullName, String emailAd, String password, String confirmPassword,
                             boolean valid )
    {
        login( getAdminUsername(), getAdminPassword() );
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

    // User Roles


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

    public void goToHomePage()
    {
        getSelenium().open( baseUrl );
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