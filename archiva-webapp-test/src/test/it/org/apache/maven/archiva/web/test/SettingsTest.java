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


/**
 * Test archiva 'Settings'
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class SettingsTest
    extends AbstractArchivaTestCase
{

    public void testRunIndexer()
    {
        clickSettings();

        clickLinkWithText( "Run Now" );
        waitPage();

        assertPage( "Administration" );

        logout();
    }

    public void testEditIndexDirectory()
    {
        clickEditConfiguration();

        setFieldValue( "indexPath", getBasedir() + "target/index" );
        clickButtonWithValue( "Save Configuration" );
        waitPage();
        assertPage( "Administration" );
        assertTextPresent( getBasedir() + "target/index" );

        logout();
    }

    public void testValidIndexSchedule()
    {
        clickEditConfiguration();

        setFieldValue( "second", "*" );
        setFieldValue( "minute", "*" );
        clickButtonWithValue( "Save Configuration" );
        waitPage();
        assertPage( "Administration" );

        logout();
    }

    public void testInvalidIndexSchedule()
    {
        clickEditConfiguration();
        setFieldValue( "second", "asdf" );
        clickButtonWithValue( "Save Configuration" );
        waitPage();
        assertPage( "Configuration" );
        assertTextPresent( "Invalid Cron Expression" );

        logout();
    }

    public void testEditProxyHost()
    {
        clickEditConfiguration();

        setFieldValue( "proxy.host", "asdf" );
        clickButtonWithValue( "Save Configuration" );
        waitPage();
        assertPage( "Administration" );

        logout();
    }

    public void testValidProxyPort()
    {
        clickEditConfiguration();

        setFieldValue( "proxy.port", "32143" );
        clickButtonWithValue( "Save Configuration" );
        waitPage();
        assertPage( "Administration" );

        logout();
    }

    public void testInvalidProxyPort()
    {
        clickEditConfiguration();
        setFieldValue( "proxy.port", "asdf" );
        clickButtonWithValue( "Save Configuration" );
        waitPage();
        assertPage( "Configuration" );
        assertTextPresent( "Port" );
        assertTextPresent( "Invalid field value for field \"proxy.port\"" );

        setFieldValue( "proxy.port", "-1" );
        clickButtonWithValue( "Save Configuration" );
        waitPage();
        assertPage( "Administration" );

        logout();
    }

    public void testEditProxyCredentials()
    {
        clickEditConfiguration();

        setFieldValue( "proxy.username", "asdf" );
        clickButtonWithValue( "Save Configuration" );
        waitPage();
        assertPage( "Administration" );

        logout();
    }

    /**
     * Click Edit Configuration link
     */
    private void clickEditConfiguration()
    {
        clickSettings();

        clickLinkWithText( "Edit Configuration" );
        assertPage( "Configuration" );
    }

    /**
     * Click Settings from the navigation menu
     */
    private void clickSettings()
    {
        goToLoginPage();
        submitLoginPage( adminUsername, adminPassword );

        clickLinkWithText( "Settings" );
        assertPage( "Administration" );
    }

}
