package org.apache.archiva.web.test;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.Assert;
import org.apache.commons.lang3.StringUtils;
import org.fluentlenium.adapter.FluentTest;
import org.fluentlenium.core.domain.FluentList;
import org.fluentlenium.core.domain.FluentWebElement;
import static org.fest.assertions.Assertions.assertThat;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.fluentlenium.core.Fluent;
import org.junit.Before;

/**
 * @author Olivier Lamy
 */
public class WebDriverBrowseTest
        extends FluentTest
{

    @Override
    public Fluent takeScreenShot( String fileName )
    {
        try
        {
            // save html to have a minimum feedback if jenkins firefox not up
            File fileNameHTML = new File( fileName + ".html" );
            FileUtils.writeStringToFile( fileNameHTML, getDriver().getPageSource() );
        }
        catch ( IOException e )
        {
            System.out.print( e.getMessage() );
            e.printStackTrace();
        }
        return super.takeScreenShot( fileName );
    }

    @Before
    public void init()
    {
        setSnapshotMode( Mode.TAKE_SNAPSHOT_ON_FAIL );
        setSnapshotPath( new File( "target", "errorshtmlsnap" ).getAbsolutePath() );
    }

    @Test
    public void simpletest()
            throws Exception
    {
        Properties p = new Properties();
        p.load( this.getClass().getClassLoader().getResourceAsStream( "test.properties" ) );

        Properties tomcatPortProperties = new Properties();
        tomcatPortProperties.load(
                new FileInputStream( new File( System.getProperty( "tomcat.propertiesPortFilePath" ) ) ) );

        int tomcatPort = Integer.parseInt( tomcatPortProperties.getProperty( "tomcat.maven.http.port" ) );

        goTo( "http://localhost:" + tomcatPort + "/archiva/index.html?request_lang=en" );

        // wait until topbar-menu-container is feeded
        await().atMost( 5, TimeUnit.SECONDS ).until( "#topbar-menu" ).isPresent();

        FluentList<FluentWebElement> elements = find( "#create-admin-link-a" );

        if ( !elements.isEmpty() && elements.get( 0 ).isDisplayed() )
        {
            WebElement webElement = elements.get( 0 ).getElement();
            Assert.assertEquals( "Create Admin User", webElement.getText() );

            webElement.click();
            await().atMost( 2, TimeUnit.SECONDS ).until( "#user-create" ).isPresent();
            assertThat( find( "#username" ).getValue().equals( "admin" ) );
            assertThat( find( "#password" ).getValue().isEmpty() );
            assertThat( find( "#confirmPassword" ).getValue().isEmpty() );
            assertThat( find( "#email" ).getValue().isEmpty() );

            fill( "#fullname" ).with( p.getProperty( "ADMIN_FULLNAME" ) );
            fill( "#email" ).with( p.getProperty( "ADMIN_EMAIL" ) );
            fill( "#password" ).with( p.getProperty( "ADMIN_PASSWORD" ) );
            fill( "#confirmPassword" ).with( p.getProperty( "ADMIN_PASSWORD" ) );
            find( "#user-create-form-register-button" ).click();

            await().atMost( 2, TimeUnit.SECONDS ).until( "#logout-link" ).isPresent();

            FluentList<FluentWebElement> elementss = find( "#menu-find-browse-a" );
            WebElement webElsement = elementss.get( 0 ).getElement();
            webElsement.click();
            await().atMost( 2, TimeUnit.SECONDS ).until( "#main_browse_result" ).isPresent();
            // give me search page :( not  browse page

            takeScreenShot( "search" );

            goTo( "http://localhost:" + tomcatPort + "/archiva/index.html#browse?request_lang=en" );
            takeScreenShot( "browse" );
            // give me a browse page
            
        }
        else
        {
            elements = find( "#login-link-a" );
            WebElement webElement = elements.get( 0 ).getElement();
            Assert.assertEquals( "LOGIN", webElement.getText() );
        }

    }

    @Override
    public WebDriver getDefaultDriver()
    {
        String seleniumBrowser = System.getProperty( "selenium.browser" );

        if ( StringUtils.contains( seleniumBrowser, "chrome" ) )
        {
            return new ChromeDriver();
        }

        if ( StringUtils.contains( seleniumBrowser, "safari" ) )
        {
            return new SafariDriver();
        }

        if ( StringUtils.contains( seleniumBrowser, "iexplore" ) )
        {
            return new InternetExplorerDriver();
        }
        
        return new FirefoxDriver();

    }
}
