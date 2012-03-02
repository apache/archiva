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
import org.apache.archiva.web.test.tools.AfterSeleniumFailure;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: AbstractSeleniumTestCase.java 761154 2009-04-02 03:31:19Z wsmoak $
 */

public abstract class AbstractSeleniumTest
{

    public static String baseUrl;

    public static String maxWaitTimeInMs;

    private static ThreadLocal<Selenium> selenium = new ThreadLocal<Selenium>();

    public static Properties p;

    private final static String PROPERTIES_SEPARATOR = "=";

    public void open()
        throws Exception
    {
        p = new Properties();
        p.load( this.getClass().getClassLoader().getResourceAsStream( "test.properties" ) );
    }

    /**
     * Initialize selenium
     */
    public void open( String baseUrl, String browser, String seleniumHost, int seleniumPort, String maxWaitTimeInMs )
        throws Exception
    {
        try
        {
            AbstractSeleniumTest.baseUrl = baseUrl;
            AbstractSeleniumTest.maxWaitTimeInMs = maxWaitTimeInMs;

            if ( getSelenium() == null )
            {
                DefaultSelenium s = new DefaultSelenium( seleniumHost, seleniumPort, browser, baseUrl );

                s.start();
                s.setTimeout( maxWaitTimeInMs );
                selenium.set( s );
            }
        }
        catch ( Exception e )
        {
            // yes
            System.out.print( e.getMessage() );
            e.printStackTrace();
        }
    }

    public static Selenium getSelenium()
    {
        return selenium == null ? null : selenium.get();
    }

    protected String getProperty( String key )
    {
        return p.getProperty( key );
    }

    protected String getEscapeProperty( String key )
    {
        InputStream input = this.getClass().getClassLoader().getResourceAsStream( "test.properties" );
        String value = null;
        List<String> lines;
        try
        {
            lines = IOUtils.readLines( input );
        }
        catch ( IOException e )
        {
            lines = new ArrayList<String>();
        }
        for ( String l : lines )
        {
            if ( l != null && l.startsWith( key ) )
            {
                int indexSeparator = l.indexOf( PROPERTIES_SEPARATOR );
                value = l.substring( indexSeparator + 1 ).trim();
                break;
            }
        }
        return value;
    }

    /**
     * Close selenium session.
     */
    public void close()
        throws Exception
    {
        if ( getSelenium() != null )
        {
            getSelenium().stop();
            selenium.set( null );
        }
    }

    // *******************************************************
    // Auxiliar methods. This method help us and simplify test.
    // *******************************************************

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
            Thread.sleep( Long.parseLong( maxWaitTimeInMs ) );
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

    @AfterSeleniumFailure
    public void captureScreenShotOnFailure( Throwable failure )
    {
        SimpleDateFormat sdf = new SimpleDateFormat( "yyyy.MM.dd-HH_mm_ss" );
        String time = sdf.format( new Date() );
        File targetPath = new File( "target", "screenshots" );
        StackTraceElement stackTrace[] = failure.getStackTrace();
        String cName = this.getClass().getName();
        int index = getStackTraceIndexOfCallingClass( cName, stackTrace );
        String methodName = stackTrace[index].getMethodName();
        int lNumber = stackTrace[index].getLineNumber();
        String lineNumber = Integer.toString( lNumber );
        String className = cName.substring( cName.lastIndexOf( '.' ) + 1 );
        targetPath.mkdirs();
        Selenium selenium = AbstractSeleniumTest.getSelenium();
        String fileBaseName = methodName + "_" + className + ".java_" + lineNumber + "-" + time;

        selenium.windowMaximize();

        File fileName = new File( targetPath, fileBaseName + ".png" );
        selenium.captureEntirePageScreenshot( fileName.getAbsolutePath(), "background=#FFFFFF" );

    }

    private int getStackTraceIndexOfCallingClass( String nameOfClass, StackTraceElement stackTrace[] )
    {
        boolean match = false;
        int i = 0;
        do
        {
            String className = stackTrace[i].getClassName();
            match = Pattern.matches( nameOfClass, className );
            i++;
        }
        while ( match == false );
        i--;
        return i;
    }

}