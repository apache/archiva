package org.apache.maven.archiva.web.test;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import com.thoughtworks.selenium.Selenium;

/**
 * @author Edwin Punzalan
 */
public class LoginTest
    extends AbstractArchivaTestCase
{
    public void testBadLogin()
    {
        getSelenium().open( "/archiva/login.action" );
        submitLoginPage( "badUsername", "badPassword", false );
        assertLoginPage();
        assertTextPresent( "Authentication failed" );
    }

    public void testUserLogin()
    {
        createUser( "user", "user01" );
        getSelenium().open( "/archiva/login.action" );
        assertLoginPage();
        submitLoginPage( "user", "user01" );
    }

    public void login()
    {
    }

    private void createUser( String username, String password )
    {
        Selenium sel = getSelenium();

        sel.open( "/archiva/security/login.action" );
        submitLoginPage( adminUsername, adminPassword );

        sel.open( "/archiva/security/userlist.action" );
        assertPage( "Maven Archiva :: [Admin] User List" );
        assertTextNotPresent( username );
        sel.open( "/archiva/security/usercreate!show.action" );
        assertPage( "Maven Archiva :: [Admin] User Create" );
        sel.type( "user.username", username );
        sel.type( "user.fullName", username + " FullName" );
        sel.type( "user.email", username + "@localhost.com" );
        sel.type( "user.password", password );
        sel.type( "user.confirmPassword", password );
        sel.click( "//input[@type='submit' and @value='Create User']" );
        waitPage();
        assertPage( "Maven Archiva :: [Admin] User List" );
        assertTextPresent( username );
    }
}
