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
    private String baseUrl = "http://localhost:9595/archiva";

    public static final String CREATE_ADMIN_USER_PAGE_TITLE = "Maven Archiva :: Create Admin User";

    protected String getApplicationName()
    {
        return "Archiva";
    }

    protected String getInceptionYear()
    {
        return "2005";
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
}
