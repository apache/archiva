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
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
        File fileNameHTML = new File( "target", "errorshtmlsnap" );
        try
        {
            // save html to have a minimum feedback if jenkins firefox not up
            fileNameHTML = new File( fileNameHTML, fileName );
            FileUtils.writeStringToFile( new File ( new File( "target", "errorshtmlsnap" ) , fileName + ".html"), getDriver().getPageSource() );

        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        return super.takeScreenShot( fileNameHTML.getAbsolutePath() );

    }

    @Before
    public void init()
    {
        setSnapshotMode( Mode.TAKE_SNAPSHOT_ON_FAIL );
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

            await().atMost( 10, TimeUnit.SECONDS ).until( "#logout-link" ).isPresent();
            await().atMost( 10, TimeUnit.SECONDS ).until( "#footer-content" ).isPresent();

            FluentList<FluentWebElement> elementss = find( "#menu-find-search-a" );
            WebElement webElsement = elementss.get( 0 ).getElement();
            webElsement.click();
            elementss = find( "#menu-find-browse-a" );
            webElsement = elementss.get( 0 ).getElement();
            webElsement.click();
            await().atMost( 120, TimeUnit.SECONDS ).until( "#main_browse_result" ).isPresent();
            // give me search page :( not  browse page

            takeScreenShot( "search.png" );

            goTo( "http://localhost:" + tomcatPort + "/archiva/index.html#browse?request_lang=en" );
            takeScreenShot( "browse.png" );
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
    public WebDriver getDefaultDriver() {
        String seleniumBrowser = System.getProperty("selenium.browser");
        String seleniumHost = System.getProperty("seleniumHost", "localhost");
        int seleniumPort = Integer.getInteger("seleniumPort", 4444);
        try {

            if (StringUtils.contains(seleniumBrowser, "chrome")) {
                return new RemoteWebDriver(new URL("http://" + seleniumHost + ":" + seleniumPort + "/wd/hub"),
                        DesiredCapabilities.chrome()
                );
            }

            if (StringUtils.contains(seleniumBrowser, "safari")) {
                return new RemoteWebDriver(new URL("http://" + seleniumHost + ":" + seleniumPort + "/wd/hub"),
                        DesiredCapabilities.safari()
                );
            }

            if (StringUtils.contains(seleniumBrowser, "iexplore")) {
                return new RemoteWebDriver(new URL("http://" + seleniumHost + ":" + seleniumPort + "/wd/hub"),
                        DesiredCapabilities.internetExplorer()
                );
            }

            return new RemoteWebDriver(new URL("http://" + seleniumHost + ":" + seleniumPort + "/wd/hub"),
                    DesiredCapabilities.firefox()
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException("Initializion of remote driver failed");
        }

    }
}
