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

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;
import org.apache.archiva.web.test.tools.ArchivaSeleniumExecutionRule;
import org.junit.Assert;
import org.junit.Rule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 *
 */

public abstract class AbstractSeleniumTest
{
    final Logger logger = LoggerFactory.getLogger( AbstractSeleniumTest.class );

    @Rule
    public ArchivaSeleniumExecutionRule archivaSeleniumExecutionRule = new ArchivaSeleniumExecutionRule();

    public String browser = System.getProperty( "browser" );

    public String baseUrl =
        "http://localhost:" + System.getProperty( "tomcat.maven.http.port" ) + "/archiva/index.html?request_lang=en";

    public int maxWaitTimeInMs = Integer.getInteger( "maxWaitTimeInMs" );

    public String seleniumHost = System.getProperty( "seleniumHost", "localhost" );

    public int seleniumPort = Integer.getInteger( "seleniumPort", 4444 );

    private Selenium selenium = null;

    public Properties p;

    /**
     * this method is called by the Rule before executing a test
     *
     * @throws Exception
     */
    public void open()
        throws Exception
    {
        p = new Properties();
        p.load( this.getClass().getClassLoader().getResourceAsStream( "test.properties" ) );

        Properties tomcatPortProperties = new Properties();
        tomcatPortProperties.load(
            new FileInputStream( new File( System.getProperty( "tomcat.propertiesPortFilePath" ) ) ) );

        int tomcatPort = Integer.parseInt( tomcatPortProperties.getProperty( "tomcat.maven.http.port" ) );

        baseUrl = "http://localhost:" + tomcatPort + "/archiva/index.html?request_lang=en";


        open( baseUrl, browser, seleniumHost, seleniumPort, maxWaitTimeInMs );
        logger.info("Selected Browser: {}", browser);
        archivaSeleniumExecutionRule.selenium = selenium;
        assertAdminCreated();
    }

    /**
     * this method is called by the Rule after executing a tests
     */
    public void close()
    {
        if ( getSelenium() != null )
        {
            getSelenium().stop();
        }
    }

    /**
     * Initialize selenium
     */
    public void open( String baseUrl, String browser, String seleniumHost, int seleniumPort, int maxWaitTimeInMs )
        throws Exception
    {
        try
        {
            if ( getSelenium() == null )
            {
                selenium = new DefaultSelenium( seleniumHost, seleniumPort, browser, baseUrl );
                selenium.start();
                selenium.setTimeout( Integer.toString( maxWaitTimeInMs ) );
            }
        }
        catch ( Exception e )
        {
            // yes
            System.out.print( e.getMessage() );
            e.printStackTrace();
        }
    }

    public void assertAdminCreated()
        throws Exception
    {
        initializeArchiva( baseUrl, browser, maxWaitTimeInMs, seleniumHost, seleniumPort );
    }

    public void initializeArchiva( String baseUrl, String browser, int maxWaitTimeInMs, String seleniumHost,
                                   int seleniumPort )
        throws Exception
    {

        open( baseUrl, browser, seleniumHost, seleniumPort, maxWaitTimeInMs );

        getSelenium().open( baseUrl );

        waitPage();

        // if not admin user created create one
        if ( isElementVisible( "create-admin-link" ) )
        {
            Assert.assertFalse( getSelenium().isVisible( "login-link-a" ) );
            Assert.assertFalse( getSelenium().isVisible( "register-link-a" ) );
            // skygo need to set to true for passing is that work as expected ?
            clickLinkWithLocator( "create-admin-link-a", true );
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

    public Selenium getSelenium()
    {
        return selenium;
    }

    protected String getProperty( String key )
    {
        return p.getProperty( key );
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

    public void submitAdminData( String fullname, String email, String password )
    {
        setFieldValue( "fullname", fullname );
        setFieldValue( "email", email );
        setFieldValue( "password", password );
        setFieldValue( "confirmPassword", password );
        clickButtonWithLocator( "user-create-form-register-button" );
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


    public void assertLoginModal()
    {
        assertElementPresent( "user-login-form" );
        Assert.assertTrue( isElementVisible( "register-link" ) );
        assertElementPresent( "user-login-form-username" );
        assertElementPresent( "user-login-form-password" );
        assertButtonWithIdPresent( "modal-login-ok" );
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

    // *******************************************************
    // Auxiliar methods. This method help us and simplify test.
    // *******************************************************

    protected void assertUserLoggedIn( String username )
    {
        Assert.assertFalse( isElementVisible( "login-link" ) );
        Assert.assertTrue( isElementVisible( "logout-link" ) );
        Assert.assertFalse( isElementVisible( "register-link" ) );
        Assert.assertFalse( isElementVisible( "create-admin-link" ) );
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

    public void assertFieldValue( String fieldValue, String fieldName )
    {
        assertElementPresent( fieldName );
        Assert.assertEquals( fieldValue, getSelenium().getValue( fieldName ) );
    }

    public void assertPage( String title )
    {
        Assert.assertEquals( getTitle(), title );
    }

    public String getTitle()
    {
        // Collapse spaces
        return getSelenium().getTitle().replaceAll( "[ \n\r]+", " " );
    }

    public String getHtmlContent()
    {
        return getSelenium().getHtmlSource();
    }

    public String getText( String locator )
    {
        return getSelenium().getText( locator );
    }

    public void assertTextPresent( String text )
    {
        Assert.assertTrue( "'" + text + "' isn't present.", getSelenium().isTextPresent( text ) );
    }

    /**
     * one of text args must be in the page so use en and fr text (olamy use en locale :-) )
     *
     * @param texts
     */
    public void assertTextPresent( String... texts )
    {
        boolean present = false;
        StringBuilder sb = new StringBuilder();
        for ( String text : texts )
        {
            present = present || getSelenium().isTextPresent( text );
            sb.append( " " + text + " " );
        }
        Assert.assertTrue( "'one of the following test " + sb.toString() + "' isn't present.", present );
    }

    public void assertTextNotPresent( String text )
    {
        Assert.assertFalse( "'" + text + "' is present.", getSelenium().isTextPresent( text ) );
    }

    public void assertElementPresent( String elementLocator )
    {
        Assert.assertTrue( "'" + elementLocator + "' isn't present.", isElementPresent( elementLocator ) );
    }

    public void assertElementNotPresent( String elementLocator )
    {
        Assert.assertFalse( "'" + elementLocator + "' is present.", isElementPresent( elementLocator ) );
    }

    public void assertLinkPresent( String text )
    {
        Assert.assertTrue( "The link '" + text + "' isn't present.", isElementPresent( "link=" + text ) );
    }

    public void assertLinkNotPresent( String text )
    {
        Assert.assertFalse( "The link('" + text + "' is present.", isElementPresent( "link=" + text ) );
    }

    public void assertLinkNotVisible( String text )
    {
        Assert.assertFalse( "The link('" + text + "' is visible.", isElementVisible( "link=" + text ) );
    }

    public void assertLinkVisible( String text )
    {
        Assert.assertTrue( "The link('" + text + "' is not visible.", isElementVisible( "link=" + text ) );
    }

    public void assertImgWithAlt( String alt )
    {
        assertElementPresent( "/¯img[@alt='" + alt + "']" );
    }

    public void assertImgWithAltAtRowCol( boolean isALink, String alt, int row, int column )
    {
        String locator = "//tr[" + row + "]/td[" + column + "]/";
        locator += isALink ? "a/" : "";
        locator += "img[@alt='" + alt + "']";

        assertElementPresent( locator );
    }

    public void assertImgWithAltNotPresent( String alt )
    {
        assertElementNotPresent( "/¯img[@alt='" + alt + "']" );
    }

    public void assertCellValueFromTable( String expected, String tableElement, int row, int column )
    {
        Assert.assertEquals( expected, getCellValueFromTable( tableElement, row, column ) );
    }

    public boolean isTextPresent( String text )
    {
        return getSelenium().isTextPresent( text );
    }

    public boolean isLinkPresent( String text )
    {
        return isElementPresent( "link=" + text );
    }

    public boolean isElementPresent( String locator )
    {
        return getSelenium().isElementPresent( locator );
    }

    public boolean isElementVisible( String locator )
    {
        return getSelenium().isVisible( locator );
    }


    public void waitPage()
    {
        // TODO define a smaller maxWaitTimeJsInMs for wait javascript response for browser side validation
        //getSelenium().w .wait( Long.parseLong( maxWaitTimeInMs ) );
        //getSelenium().waitForPageToLoad( maxWaitTimeInMs );
        // http://jira.openqa.org/browse/SRC-302
        // those hack looks to break some tests :-(
        // getSelenium().waitForCondition( "selenium.isElementPresent('document.body');", maxWaitTimeInMs );
        //getSelenium().waitForCondition( "selenium.isElementPresent('footer');", maxWaitTimeInMs );
        //getSelenium().waitForCondition( "selenium.browserbot.getCurrentWindow().document.getElementById('footer')",
        //                                maxWaitTimeInMs );
        // so the only hack is to not use a too small wait time

        try
        {
            Thread.sleep( maxWaitTimeInMs );
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( "issue on Thread.sleep : " + e.getMessage(), e );
        }
    }

    public String getFieldValue( String fieldName )
    {
        return getSelenium().getValue( fieldName );
    }

    public String getCellValueFromTable( String tableElement, int row, int column )
    {
        return getSelenium().getTable( tableElement + "." + row + "." + column );
    }

    public void selectValue( String locator, String value )
    {
        getSelenium().select( locator, "label=" + value );
    }


    public void assertOptionPresent( String selectField, String[] options )
    {
        assertElementPresent( selectField );
        String[] optionsPresent = getSelenium().getSelectOptions( selectField );
        List<String> expected = Arrays.asList( options );
        List<String> present = Arrays.asList( optionsPresent );
        Assert.assertTrue( "Options expected are not included in present options", present.containsAll( expected ) );
    }

    public void assertSelectedValue( String value, String fieldName )
    {
        assertElementPresent( fieldName );
        String optionsPresent = getSelenium().getSelectedLabel( value );
        Assert.assertEquals( optionsPresent, value );
    }

    public void submit()
    {
        clickLinkWithXPath( "//input[@type='submit']" );
    }

    public void assertButtonWithValuePresent( String text )
    {
        Assert.assertTrue( "'" + text + "' button isn't present", isButtonWithValuePresent( text ) );
    }

    public void assertButtonWithIdPresent( String id )
    {
        Assert.assertTrue( "'Button with id =" + id + "' isn't present", isButtonWithIdPresent( id ) );
    }

    public void assertButtonWithValueNotPresent( String text )
    {
        Assert.assertFalse( "'" + text + "' button is present", isButtonWithValuePresent( text ) );
    }

    public boolean isButtonWithValuePresent( String text )
    {
        return isElementPresent( "//button[@value='" + text + "']" ) || isElementPresent(
            "//input[@value='" + text + "']" );
    }

    public boolean isButtonWithIdPresent( String text )
    {
        return isElementPresent( "//button[@id='" + text + "']" ) || isElementPresent( "//input[@id='" + text + "']" );
    }

    public void clickButtonWithName( String text, boolean wait )
    {
        clickLinkWithXPath( "//input[@name='" + text + "']", wait );
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
        getSelenium().click( locator );
        if ( wait )
        {
            waitPage();
        }
    }

    public void clickButtonWithLocator( String locator )
    {
        clickButtonWithLocator( locator, true );
    }

    public void clickButtonWithLocator( String locator, boolean wait )
    {
        assertElementPresent( locator );
        getSelenium().click( locator );
        if ( wait )
        {
            waitPage();
        }
    }

    public void setFieldValues( Map<String, String> fieldMap )
    {
        Map.Entry<String, String> entry;

        for ( Iterator<Entry<String, String>> entries = fieldMap.entrySet().iterator(); entries.hasNext(); )
        {
            entry = entries.next();

            getSelenium().type( entry.getKey(), entry.getValue() );
        }
    }

    public void setFieldValue( String fieldName, String value )
    {
        getSelenium().type( fieldName, value );
    }

    public void checkField( String locator )
    {
        getSelenium().check( locator );
    }

    public void uncheckField( String locator )
    {
        getSelenium().uncheck( locator );
    }

    public boolean isChecked( String locator )
    {
        return getSelenium().isChecked( locator );
    }

    public void assertIsChecked( String locator )
    {
        Assert.assertTrue( getSelenium().isChecked( locator ) );
    }

    public void assertIsNotChecked( String locator )
    {
        Assert.assertFalse( getSelenium().isChecked( locator ) );
    }

    public void assertXpathCount( String locator, int expectedCount )
    {
        int count = getSelenium().getXpathCount( locator ).intValue();
        Assert.assertEquals( count, expectedCount );
    }

    public void assertElementValue( String locator, String expectedValue )
    {
        Assert.assertEquals( getSelenium().getValue( locator ), expectedValue );
    }

    public String captureScreenShotOnFailure( Throwable failure, String methodName, String className )
    {
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy.MM.dd-HH_mm_ss" );
        String time = sdf.format( new Date() );
        File targetPath = new File( "target", "screenshots" );

        int lineNumber = 0;

        for ( StackTraceElement stackTrace : failure.getStackTrace() )
        {
            if ( stackTrace.getClassName().equals( this.getClass().getName() ) )
            {
                lineNumber = stackTrace.getLineNumber();
                break;
            }
        }

        targetPath.mkdirs();
        Selenium selenium = getSelenium();
        String fileBaseName = methodName + "_" + className + ".java_" + lineNumber + "-" + time;

        selenium.windowMaximize();
        
        try
        {
            // save html to have a minimum feedback if jenkins firefox not up
            File fileNameHTML = new File( new File( "target", "errorshtmlsnap" ) , fileBaseName + ".html" );
            FileUtils.writeStringToFile( fileNameHTML, selenium.getHtmlSource() );
        }
        catch ( IOException e )
        {
            System.out.print( e.getMessage() );
            e.printStackTrace();
        }
        
        File fileName = new File( targetPath, fileBaseName + ".png" );

        try
        {
            selenium.captureEntirePageScreenshot( fileName.getAbsolutePath( ), "background=#FFFFFF" );
        } catch (Throwable e) {
            try
            {
                selenium.captureScreenshot( fileName.getAbsolutePath( ) );
            } catch (Throwable e1) {
                logger.error("Could not capture screenshot {}:", e1.getMessage(), e1);
            }
        }
        
        return fileName.getAbsolutePath();
    }

}