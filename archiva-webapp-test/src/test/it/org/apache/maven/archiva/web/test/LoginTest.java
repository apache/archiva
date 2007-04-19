package org.apache.maven.archiva.web.test;

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

/**
 * @author Edwin Punzalan
 */
public class LoginTest
    extends AbstractArchivaTestCase
{
    public void testBadLogin()
    {
        goToLoginPage();
        submitLoginPage( "badUsername", "badPassword", false );
        assertTextPresent( "You have entered an incorrect username and/or password" );
    }

    public void testUserLogin()
    {
        createUser( "test-user", "temp-pass" );

        goToLoginPage();
        submitLoginPage( "test-user", "temp-pass" );

        // change of password required for new users
        if ( getTitle().equals( getTitlePrefix() + "Change Password" ) )
        {
            setFieldValue( "existingPassword", "temp-pass" );
            setFieldValue( "newPassword", "p4ssw0rd" );
            setFieldValue( "newPasswordConfirm", "p4ssw0rd" );
            clickButtonWithValue( "Change Password" );
        }

        logout();

        deleteUser( "test-user" );
    }

    private void createUser( String username, String password )
    {
        goToLoginPage();
        submitLoginPage( adminUsername, adminPassword );

        clickLinkWithText( "User Management" );
        //assertPage( "[Admin] User List" );
        //assertLinkNotPresent( username );
        clickButtonWithValue( "Create New User" );

        //assertPage( "[Admin] User Create" );
        setFieldValue( "user.username", username );
        setFieldValue( "user.fullName", username + " FullName" );
        setFieldValue( "user.email", username + "@localhost.com" );
        setFieldValue( "user.password", password );
        setFieldValue( "user.confirmPassword", password );
        clickButtonWithValue( "Create User" );
        waitPage();
        //assertPage( "[Admin] User List" );
        //assertLinkPresent( username );

        logout();
    }

    private void deleteUser( String username )
    {
        goToLoginPage();
        submitLoginPage( adminUsername, adminPassword );

        clickLinkWithText( "User Management" );
        assertPage( "[Admin] User List" );
        assertLinkPresent( username );

        //this does not work bec the image is pointing to /archiva/archiva/images/pss/admin/delete.gif
        // when ran in selenium
        // clickLinkWithXPath( "//a[@href='/security/userdelete.action?username=" + username + "']" );
        //so instead we use this
        open( "/archiva/security/userdelete.action?username=" + username );

        assertPage( "[Admin] User Delete" );
        assertTextPresent( "The following user will be deleted: " + username );
        clickButtonWithValue( "Delete User" );
        assertPage( "[Admin] User List" );

        logout();
    }
}
