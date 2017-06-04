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
import org.apache.archiva.web.test.tools.WebdriverInitializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.fluentlenium.adapter.junit.FluentTest;
import org.fluentlenium.core.domain.FluentList;
import org.fluentlenium.core.domain.FluentWebElement;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
public class WebDriverBrowseTest
        extends FluentTest
{

    @Override
    public void takeScreenShot( String fileName )
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
        super.takeScreenShot( fileNameHTML.getAbsolutePath() );

    }

    @Before
    public void init()
    {

        setScreenshotMode( TriggerMode.AUTOMATIC_ON_FAIL);
        setDriverLifecycle( DriverLifecycle.CLASS );

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
        await().atMost( 5, TimeUnit.SECONDS ).until( $("#topbar-menu" )).present();

        FluentList<FluentWebElement> elements = find( "#create-admin-link-a" );

        if ( !elements.isEmpty() && elements.get( 0 ).displayed() )
        {
            WebElement webElement = elements.get( 0 ).getElement();
            Assert.assertEquals( "Create Admin User", webElement.getText() );

            webElement.click();
            await().atMost( 2, TimeUnit.SECONDS ).until($( "#user-create" )).present();
            assertThat( find( "#username" ).value().equals( "admin" ) );
            assertThat( find( "#password" ).value().isEmpty() );
            assertThat( find( "#confirmPassword" ).value().isEmpty() );
            assertThat( find( "#email" ).value().isEmpty() );

            $("#fullname").fill().with( p.getProperty( "ADMIN_FULLNAME" ) );
            $("#email").fill().with( p.getProperty( "ADMIN_EMAIL" ) );
            $("#password").fill().with( p.getProperty( "ADMIN_PASSWORD" ) );
            $("#confirmPassword").fill().with( p.getProperty( "ADMIN_PASSWORD" ) );
            find( "#user-create-form-register-button" ).click();

            await().atMost( 2, TimeUnit.SECONDS ).until($("#logout-link" )).present();

            FluentList<FluentWebElement> elementss = find( "#menu-find-browse-a" );
            WebElement webElsement = elementss.get( 0 ).getElement();
            webElsement.click();
            await().atMost( 2, TimeUnit.SECONDS ).until($("#main_browse_result" )).present();
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
    public WebDriver newWebDriver() {
        return WebdriverInitializer.newWebDriver();
    }
}
