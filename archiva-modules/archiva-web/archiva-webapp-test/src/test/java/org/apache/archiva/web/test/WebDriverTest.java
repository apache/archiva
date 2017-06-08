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

import org.fluentlenium.adapter.junit.After;
import org.junit.Assert;
import org.apache.archiva.web.test.tools.WebdriverUtility;
import org.apache.commons.io.FileUtils;
import org.fluentlenium.adapter.junit.FluentTest;
import org.fluentlenium.configuration.ConfigurationProperties;
import org.fluentlenium.configuration.FluentConfiguration;
import org.fluentlenium.core.domain.FluentList;
import org.fluentlenium.core.domain.FluentWebElement;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Olivier Lamy
 */
@FluentConfiguration(driverLifecycle = ConfigurationProperties.DriverLifecycle.CLASS)
public class WebDriverTest
        extends FluentTest
{

    final Logger log = LoggerFactory.getLogger( WebDriver.class );

    @Override
    public void takeScreenShot( String fileName )
    {
        log.info("Taking screenshot "+fileName);
        WebdriverUtility.takeScreenShot( fileName, getDriver());
    }

    @Override
    protected void failed( String testName )
    {
        takeScreenShot( testName + ".png" );
    }

    @Override
    public boolean canTakeScreenShot()
    {
        return true;
    }

    @Before
    public void init() {
        setScreenshotMode(TriggerMode.AUTOMATIC_ON_FAIL);
        setScreenshotPath( Paths.get("target", "errorshtmlsnap").toAbsolutePath().toString());
        setDriverLifecycle( DriverLifecycle.CLASS );
    }

    @Test
    public void simpletest()
            throws Exception {

        String url = WebdriverUtility.getBaseUrl()+ "/index.html?request_lang=en";
        goTo(url);

        // wait until topbar-menu-container is feeded
        //await().atMost(20, TimeUnit.SECONDS).until($("#topbar-menu")).present();
        await().untilPredicate((fl) ->$("#topbar-menu").present());
        FluentList<FluentWebElement> elements = find("#create-admin-link-a");

        if (!elements.isEmpty() && elements.get(0).displayed()) {
            WebElement webElement = elements.get(0).getElement();
            Assert.assertEquals("Create Admin User", webElement.getText());
        } else {
            elements = find( By.id("login-link-a"));
            for(FluentWebElement element : elements) {
                log.info("Found login link: "+element.getElement().getTagName()+ " "+ element.getElement().getText());
            }
            WebElement webElement = elements.get(0).getElement();
            log.info("innerText: "+webElement.getAttribute("innerText"));
            log.info("value: "+webElement.getAttribute("value"));
            log.info("innerHTML: "+webElement.getAttribute( "innerHTML" ));
            log.info("JS: "+(((JavascriptExecutor)getDriver()).executeScript("return $(arguments[0]).text();", webElement)));
            if (getDriver() instanceof HtmlUnitDriver ) {
                Assert.assertEquals( "LOGIN", webElement.getText().toUpperCase() );
            } else
            {
                Assert.assertEquals( "LOGIN", webElement.getText() );
            }

        }

    }

    @Override
    public WebDriver newWebDriver() {
        return WebdriverUtility.newWebDriver();
    }
}
