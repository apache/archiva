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
import org.apache.maven.shared.web.test.AbstractSeleniumTestCase;

import java.util.Calendar;

/**
 * @author Edwin Punzalan
 */
public abstract class AbstractArchivaTestCase
    extends AbstractSeleniumTestCase
{
    protected String adminUsername = "admin";

    protected String adminPassword = "admin1";

    protected String adminFullName = "Archiva Admin";

    protected String adminEmail = "admin@localhost.localdomain.com";

    private String baseUrl = "http://localhost:9595/archiva";

    public static final String CREATE_ADMIN_USER_PAGE_TITLE = "Maven Archiva :: Create Admin User";

    protected void initialize()
    {
        getSelenium().setTimeout( "120000" );
        getSelenium().open( "/archiva" );

        if ( CREATE_ADMIN_USER_PAGE_TITLE.equals( getSelenium().getTitle() ) )
        {
            assertCreateAdminUserPage();
            submitCreateAdminUserPage( adminFullName, adminEmail, adminPassword, adminPassword );
            assertLoginPage();
            submitLoginPage( adminUsername, adminPassword );
            logout();
        }

        login();
    }

    protected String getApplicationName()
    {
        return "Archiva";
    }

    protected String getInceptionYear()
    {
        return "2005";
    }

    public abstract void login();

    public void assertFooter()
    {
        int currentYear = Calendar.getInstance().get( Calendar.YEAR );
        assertTrue( getSelenium().getText( "xpath=//div[@id='footer']/div" ).endsWith(
            " " + getInceptionYear() + "-" + currentYear + " Apache Software Foundation" ) );
    }

    public void assertHeader()
    {
        assertTrue( "banner is missing" , getSelenium().isElementPresent( "xpath=//div[@id='banner']" ) );
        assertTrue( "bannerLeft is missing" , getSelenium().isElementPresent( "xpath=//div[@id='banner']" +
            "/span[@id='bannerLeft']" ) );
        assertTrue( "bannerLeft link is missing" , getSelenium().isElementPresent( "xpath=//div[@id='banner']" +
            "/span[@id='bannerLeft']/a[@href='http://maven.apache.org/archiva/']" ) );
        assertTrue( "bannerLeft img is missing" , getSelenium().isElementPresent( "xpath=//div[@id='banner']" +
            "/span[@id='bannerLeft']/a[@href='http://maven.apache.org/archiva/']" +
            "/img[@src='http://maven.apache.org/images/maven.jpg']" ) );

        assertTrue( "bannerRight is missing",  getSelenium().isElementPresent( "xpath=//div[@id='banner']/span[@id='bannerRight']" ) );
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    //////////////////////////////////////
    // Create Admin User
    //////////////////////////////////////
    public void assertCreateAdminUserPage()
    {
        assertPage( CREATE_ADMIN_USER_PAGE_TITLE );
        assertTextPresent( "Create Admin User" );
        assertTextPresent( "Username" );
        assertElementPresent( "user.username" );
        assertTextPresent( "Full Name" );
        assertElementPresent( "user.fullName" );
        assertTextPresent( "Email Address" );
        assertElementPresent( "user.email" );
        assertTextPresent( "Password" );
        assertElementPresent( "user.password" );
        assertTextPresent( "Confirm Password" );
        assertElementPresent( "user.confirmPassword" );
    }

    //////////////////////////////////////
    // Login
    //////////////////////////////////////
    public void assertLoginPage()
    {
        assertPage( "Maven Archiva :: Login Page" );
        assertTextPresent( "Login" );
        assertTextPresent( "Username" );
        assertTextPresent( "Password" );
        assertTextPresent( "Remember Me" );
        assertFalse( isChecked( "rememberMe" ) );
    }

    public void submitCreateAdminUserPage( String fullName, String email, String password, String confirmPassword )
    {
        Selenium sel = getSelenium();
        sel.type( "user.fullName", fullName );
        sel.type( "user.email", email );
        sel.type( "user.password", password );
        sel.type( "user.confirmPassword", confirmPassword );
        sel.click( "//input[@type='submit']" );
        waitPage();
    }
}
