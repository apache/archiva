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

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;
import junit.framework.TestCase;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public abstract class AbstractSeleniumTestCase
    extends TestCase
{
    public static final String CHECKBOX_CHECK = "on";

    public static final String CHECKBOX_UNCHECK = "off";

    private Selenium sel;

    protected String adminUsername;

    protected String adminPassword;

    protected String adminFullName = getApplicationName() + " Admin";

    protected String adminEmail = "admin@localhost.localdomain";

    protected String maxWaitTimeInMs;

    protected String baseUrl;

    public void setUp()
        throws Exception
    {
        super.setUp();

        Properties p = new Properties();
        p.load ( this.getClass().getClassLoader().getResourceAsStream( "it.properties" ) );

        baseUrl = p.getProperty( "BASE_URL" );
        maxWaitTimeInMs = p.getProperty( "MAX_WAIT_TIME_IN_MS" );
        adminUsername = p.getProperty( "ADMIN_USERNAME" );
        adminPassword = p.getProperty( "ADMIN_PASSWORD" );
        String seleniumHost = p.getProperty( "SELENIUM_HOST" );
        int seleniumPort = Integer.parseInt( (p.getProperty( "SELENIUM_PORT" ) ) );

        String browser = System.getProperty( "browser" );
        if ( StringUtils.isEmpty( browser ) )
        {
            browser = p.getProperty( "SELENIUM_BROWSER" );
        }

        sel = new DefaultSelenium( seleniumHost, seleniumPort, browser, baseUrl );
        sel.start();
        initialize();
    }

    public void tearDown()
        throws Exception
    {
        sel.stop();
    }

    public Selenium getSelenium()
    {
        return sel;
    }

    public abstract String getBaseUrl();

    /**
     * We create an admin user if it doesn't exist
     */
    protected void initialize()
    {
        open( getWebContext() );

        if ( getTitle().endsWith( "Create Admin User" ) )
        {
            assertCreateAdminUserPage();
            submitCreateAdminUserPage( adminFullName, adminEmail, adminPassword, adminPassword );
            assertLoginPage();
            submitLoginPage( adminUsername, adminPassword );
            postAdminUserCreation();
            logout();
        }
    }

    /**
     * where webapp initial configurations can be done
     */
    protected void postAdminUserCreation()
    {
    	if ( getTitle().endsWith( "Continuum - Configuration" ) )
    	{
    		setFieldValue("baseUrl", baseUrl);
    		clickButtonWithValue( "Save" );
    	}
    }

    protected abstract String getApplicationName();

    /**
     * some webapps have
     *
     * @return the page prefix set by the webapp
     */
    protected String getTitlePrefix()
    {
        return "";
    }

    protected abstract String getInceptionYear();

    protected String getWebContext()
    {
        return "/";
    }

    public void open( String url )
    {
        sel.open( url );
    }

    public String getTitle()
    {
        return sel.getTitle();
    }

    public String getHtmlContent()
    {
        return getSelenium().getHtmlSource();
    }

    public void assertTextPresent( String text )
    {
        assertTrue( "'" + text + "' isn't present.", sel.isTextPresent( text ) );
    }

    public void assertTextNotPresent( String text )
    {
        assertFalse( "'" + text + "' is present.", sel.isTextPresent( text ) );
    }

    public void assertElementPresent( String elementLocator )
    {
        assertTrue( "'" + elementLocator + "' isn't present.", isElementPresent( elementLocator ) );
    }

    public void assertElementNotPresent( String elementLocator )
    {
        assertFalse( "'" + elementLocator + "' is present.", isElementPresent( elementLocator ) );
    }

    public void assertLinkPresent( String text )
    {
        assertTrue( "The link '" + text + "' isn't present.", isElementPresent( "link=" + text ) );
    }

    public void assertLinkNotPresent( String text )
    {
        assertFalse( "The link '" + text + "' is present.", isElementPresent( "link=" + text ) );
    }

    public void assertImgWithAlt( String alt )
    {
        assertElementPresent( "//img[@alt='" + alt + "']" );
    }

    public void assertImgWithAltAtRowCol( boolean isALink, String alt, int row, int column )
    {
        String locator = "//tr[" + row + "]/td[" + column + "]/";
        locator += isALink ? "a/" : "";
        locator += "img[@alt='" + alt + "']";

        assertElementPresent( locator );
    }

    public void assertCellValueFromTable( String expected, String tableElement, int row, int column )
    {
        assertEquals( expected, getCellValueFromTable( tableElement, row, column ) );
    }

    public boolean isTextPresent( String text )
    {
        return sel.isTextPresent( text );
    }

    public boolean isLinkPresent( String text )
    {
        return isElementPresent( "link=" + text );
    }

    public boolean isElementPresent( String locator )
    {
        return sel.isElementPresent( locator );
    }

    public void waitPage()
    {
        waitPage( 180000 );
    }

    public void waitPage( int nbMillisecond )
    {
        sel.waitForPageToLoad( String.valueOf( nbMillisecond ) );
    }

    public void assertPage( String title )
    {
        assertEquals( getTitlePrefix() + title, getTitle() );
        assertHeader();
        assertFooter();
    }

    public abstract void assertHeader();

    
    public void assertFooter()
    {
        int currentYear = Calendar.getInstance().get( Calendar.YEAR );
        assertTrue( getSelenium().getText( "xpath=//div[@id='footer']/div[1]" ).endsWith(
            "Copyright © " + getInceptionYear() + "-" + currentYear + " The Apache Software Foundation" ) );
    }

    public String getFieldValue( String fieldName )
    {
        return sel.getValue( fieldName );
    }

    public String getCellValueFromTable( String tableElement, int row, int column )
    {
        return getSelenium().getTable( tableElement + "." + row + "." + column );
    }

    public void selectValue( String locator, String value )
    {
        getSelenium().select( locator, "label=" + value );
    }

    public void submit()
    {
        clickLinkWithXPath( "//input[@type='submit']" );
    }

    public void assertButtonWithValuePresent( String text )
    {
        assertTrue( "'" + text + "' button isn't present", isButtonWithValuePresent( text ) );
    }

    public void assertButtonWithValueNotPresent( String text )
    {
        assertFalse( "'" + text + "' button is present", isButtonWithValuePresent( text ) );
    }

    public boolean isButtonWithValuePresent( String text )
    {
        return isElementPresent( "//button[@value='" + text + "']" ) || isElementPresent( "//input[@value='" + text + "']" );
    }

    public void clickButtonWithValue( String text )
    {
        clickButtonWithValue( text, true );
    }

    public void clickButtonWithValue( String text, boolean wait )
    {
        assertButtonWithValuePresent( text );

        if ( isElementPresent( "//button[@value='" + text + "']" ) )
        {
            clickLinkWithXPath( "//button[@value='" + text + "']", wait );
        }
        else
        {
            clickLinkWithXPath( "//input[@value='" + text + "']", wait );
        }
    }

    public void clickSubmitWithLocator( String locator )
    {
        clickLinkWithLocator( locator );
    }

    public void clickSubmitWithLocator( String locator, boolean wait )
    {
        clickLinkWithLocator( locator, wait );
    }

    public void clickImgWithAlt( String alt )
    {
        clickLinkWithLocator( "//img[@alt='" + alt + "']" );
    }

    public void clickLinkWithText( String text )
    {
        clickLinkWithText( text, true );
    }

    public void clickLinkWithText( String text, boolean wait )
    {
        clickLinkWithLocator( "link=" + text, wait );
    }

    public void clickLinkWithXPath( String xpath )
    {
        clickLinkWithXPath( xpath, true );
    }

    public void clickLinkWithXPath( String xpath, boolean wait )
    {
        clickLinkWithLocator( "xpath=" + xpath, wait );
    }

    public void clickLinkWithLocator( String locator )
    {
        clickLinkWithLocator( locator, true );
    }

    public void clickLinkWithLocator( String locator, boolean wait )
    {
        assertElementPresent( locator );
        sel.click( locator );
        if ( wait )
        {
            waitPage();
        }
    }

    public void setFieldValues( Map fieldMap )
    {
        Map.Entry entry;

        for ( Iterator entries = fieldMap.entrySet().iterator(); entries.hasNext(); )
        {
            entry = (Map.Entry) entries.next();

            sel.type( (String) entry.getKey(), (String) entry.getValue() );
        }
    }

    public void setFieldValue( String fieldName, String value )
    {
        sel.type( fieldName, value );
    }

    public void checkField( String locator )
    {
        sel.check( locator );
    }

    public void uncheckField( String locator )
    {
        sel.uncheck( locator );
    }

    public boolean isChecked( String locator )
    {
        return sel.isChecked( locator );
    }

    //////////////////////////////////////
    // Login
    //////////////////////////////////////
    public void goToLoginPage()
    {
        clickLinkWithText( "Login" );

        assertLoginPage();
    }

    public void login( String username, String password )
    {
        login( username, password, true, "Login Page" );
    }

    public void login( String username, String password, boolean valid, String assertReturnPage )
    {
        if ( isLinkPresent( "Login" ) )
        {
            goToLoginPage();

            submitLoginPage( username, password, false, valid, assertReturnPage );
        }
    }

    public void assertLoginPage()
    {
        assertPage( "Login Page" );
        assertTextPresent( "Login" );
        assertTextPresent( "Username" );
        assertTextPresent( "Password" );
        assertTextPresent( "Remember Me" );
        assertFalse( isChecked( "rememberMe" ) );
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
        clickButtonWithValue( "Login" );

        if ( validUsernamePassword )
        {
            //assertTextPresent( "Current User:" );
            assertTextPresent( username );
            assertLinkPresent( "Edit Details" );
            assertLinkPresent( "Logout" );
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

    public boolean isAuthenticated()
    {
        return !( isLinkPresent( "Login" ) && isLinkPresent( "Register" ) );
    }

    //////////////////////////////////////
    // Logout
    //////////////////////////////////////
    public void logout()
    {
        assertTrue( "User wasn't authenticated.", isAuthenticated() );
        clickLinkWithText( "Logout" );
        assertFalse( "The user is always authenticated after a logout.", isAuthenticated() );
    }

    //////////////////////////////////////
    // My Account
    //////////////////////////////////////
    public void goToMyAccount()
    {
        clickLinkWithText( "Edit Details" );
    }

    public void assertMyAccountDetails( String username, String newFullName, String newEmailAddress )
        throws Exception
    {
        assertPage( "Account Details" );

        //isTextPresent( "Username" );
        assertTextPresent( "Username:" );
        assertElementPresent( "registerForm_user_username" );
        assertCellValueFromTable( username, "//form/table", 0, 1 );

        assertTextPresent( "Full Name*:" );
        assertElementPresent( "user.fullName" );
        assertEquals( newFullName, getFieldValue( "user.fullName" ) );

        assertTextPresent( "Email Address*:" );
        assertElementPresent( "user.email" );
        assertEquals( newEmailAddress, getFieldValue( "user.email" ) );
        
        assertTextPresent("Current Password*:");
        assertElementPresent("oldPassword");

        assertTextPresent( "New Password*:" );
        assertElementPresent( "user.password" );

        assertTextPresent( "Confirm Password*:" );
        assertElementPresent( "user.confirmPassword" );

        assertTextPresent( "Last Password Change" );
        assertElementPresent( "registerForm_user_timestampLastPasswordChange" );

    }

    public void editMyUserInfo( String newFullName, String newEmailAddress, String oldPassword, String newPassword,
                                String confirmNewPassword )
    {
        goToMyAccount();

        setFieldValue( "user.fullName", newFullName );
        setFieldValue( "user.email", newEmailAddress );
        setFieldValue( "oldPassword", oldPassword );
        setFieldValue( "user.password", newPassword );
        setFieldValue( "user.confirmPassword", confirmNewPassword );
        clickButtonWithValue( "Submit" );
    }

    //////////////////////////////////////
    // Users
    //////////////////////////////////////
    public void assertUsersListPage()
    {
        assertPage( "[Admin] User List" );
    }

    public void assertCreateUserPage()
    {
        assertPage( "[Admin] User Create" );
        assertTextPresent( "Username" );
        assertTextPresent( "Full Name" );
        assertTextPresent( "Email Address" );
        assertTextPresent( "Password" );
        assertTextPresent( "Confirm Password" );
    }

    public void assertUserRolesPage()
    {
        assertPage( "[Admin] User Edit" );
        assertTextPresent( "[Admin] User Roles" );
        assertTextPresent( "Assigned Roles" );
        assertTextPresent( "Available Roles" );
    }

    public void assertDeleteUserPage( String username )
    {
        assertPage( "[Admin] User Delete" );
        assertTextPresent( "[Admin] User Delete" );
        assertTextPresent( "The following user will be deleted: " + username );
        assertButtonWithValuePresent( "Delete User" );
    }

    //////////////////////////////////////
    // Create Admin User
    //////////////////////////////////////
    public void assertCreateAdminUserPage()
    {
        assertPage( "Create Admin User" );
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

    public void submitCreateAdminUserPage( String fullName, String email, String password, String confirmPassword )
    {
        setFieldValue( "user.fullName", fullName );
        setFieldValue( "user.email", email );
        setFieldValue( "user.password", password );
        setFieldValue( "user.confirmPassword", confirmPassword );
        submit();
        waitPage();
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
    
}
