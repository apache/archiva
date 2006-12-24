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

import org.apache.maven.shared.web.test.AbstractSeleniumTestCase;

/**
 * @author Edwin Punzalan
 */
public abstract class AbstractArchivaTestCase
    extends AbstractSeleniumTestCase
{
    private String baseUrl = "http://localhost:9595/archiva";

    protected String getApplicationName()
    {
        return "Archiva";
    }

    protected String getInceptionYear()
    {
        return "2005";
    }

    protected void postAdminUserCreation()
    {
        if ( getTitle().equals( getTitlePrefix() + "Configuration" ) )
        {
            //Add Managed Repository
            setFieldValue( "id", "web-ui" );
            setFieldValue( "urlName", "web-ui" );
            setFieldValue( "name", "Web UI Test Managed Repository" );
            setFieldValue( "directory", getBasedir() + "target/web-ui-dir" );
            clickButtonWithValue( "AddRepository" );

            //Set Index location
            assertPage( "Configuration" );
            setFieldValue( "indexPath", getBasedir() + "target/web-ui-index" );
            clickButtonWithValue( "Save Configuration" );
            assertPage( "Administration" );
        }
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
            "/img[@src='" + getWebContext() + "/images/archiva.png']" ) );

        assertTrue( "bannerRight is missing",  getSelenium().isElementPresent( "xpath=//div[@id='banner']/span[@id='bannerRight']" ) );
    }

    protected String getTitlePrefix()
    {
        return "Maven Archiva :: ";
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    protected String getWebContext()
    {
        return "/archiva";
    }
}
