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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.web.test.parent.AbstractRepositoryTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test( groups = { "reposcan" }, dependsOnMethods = { "testWithCorrectUsernamePassword" }, sequential = true )
public class RepositoryScanningTest
    extends AbstractRepositoryTest
{
    public void testAddArtifactFileType_NullValue()
    {
        goToRepositoryScanningPage();
        clickAddIcon( "newpattern_0" );
        assertTextPresent( "Unable to process blank pattern." );
    }

    @Test( dependsOnMethods = { "testAddArtifactFileType_NullValue" } )
    public void testAddArtifactFileType()
    {
        setFieldValue( "newpattern_0", "**/*.dll" );
        clickAddIcon( "newpattern_0" );
        Assert.assertEquals( getSelenium().getTable( "//div[@id='contentArea']/div/div[1]/table.13.0" ), "**/*.dll" );
    }

    @Test( dependsOnMethods = { "testAddArtifactFileType" }, enabled = false )
    public void testAddArtifactFileType_ExistingValue()
    {
        // FIXME: broken
        setFieldValue( "newpattern_0", "**/*.zip" );
        clickAddIcon( "newpattern_0" );
        Assert.assertEquals( getErrorMessageText(),
                             "Not adding pattern \"**/*.zip\" to filetype artifacts as it already exists." );
    }

    @Test( dependsOnMethods = { "testAddArtifactFileType" } )
    public void testDeleteArtifactFileType()
    {
        String path = "//div[@id='contentArea']/div/div/table/tbody/tr[14]/td/code";
        assertElementPresent( path );
        Assert.assertEquals( getSelenium().getText( path ), "**/*.dll" );
        clickDeleteIcon( "**/*.dll" );
        assertElementNotPresent( path );
    }

    @Test( dependsOnMethods = { "testDeleteArtifactFileType" } )
    public void testAddAutoRemove_NullValue()
    {
        setFieldValue( "newpattern_1", "" );
        clickAddIcon( "newpattern_1" );
        assertTextPresent( "Unable to process blank pattern." );
    }

    @Test( dependsOnMethods = { "testAddAutoRemove_NullValue" }, enabled = false )
    public void testAddAutoRemove_ExistingValue()
    {
        setFieldValue( "newpattern_1", "**/*-" );
        clickAddIcon( "newpattern_1" );
        // FIXME: broken
        Assert.assertEquals( getErrorMessageText(),
                             "Not adding pattern \"**/*-\" to filetype auto-remove as it already exists." );
    }

    @Test( dependsOnMethods = { "testAddAutoRemove_NullValue" } )
    public void testAddAutoRemove()
    {
        setFieldValue( "newpattern_1", "**/*.test" );
        clickAddIcon( "newpattern_1" );
        Assert.assertEquals( getSelenium().getTable( "//div[@id='contentArea']/div/div[2]/table.3.0" ), "**/*.test" );
    }

    @Test( dependsOnMethods = { "testAddAutoRemove" } )
    public void testDeleteAutoRemove()
    {
        String path = "//div[@id='contentArea']/div/div[2]/table/tbody/tr[4]/td/code";
        assertElementPresent( path );
        Assert.assertEquals( getSelenium().getText( path ), "**/*.test" );
        clickDeleteIcon( "**/*.test" );
        assertElementNotPresent( path );
    }

    @Test( dependsOnMethods = { "testDeleteAutoRemove" } )
    public void testAddIgnoredArtifacts_NullValue()
    {
        setFieldValue( "newpattern_2", "" );
        clickAddIcon( "newpattern_2" );
        Assert.assertEquals( getErrorMessageText(),
                             "Unable to process blank pattern." );
    }

    @Test(enabled = false)
    public void testAddIgnoredArtifacts_ExistingValue()
    {
        setFieldValue( "newpattern_2", "**/*.sh" );
        clickAddIcon( "newpattern_2" );
        // FIXME: broken
        Assert.assertEquals( getErrorMessageText(),
                             "Not adding pattern \"**/*.sh\" to filetype ignored as it already exists." );
    }

    @Test
    public void testAddIgnoredArtifacts()
    {
        goToRepositoryScanningPage();
        setFieldValue( "newpattern_2", "**/*.log" );
        clickAddIcon( "newpattern_2" );
        Assert.assertEquals( getSelenium().getTable( "//div[@id='contentArea']/div/div[3]/table.6.0" ), "**/*.log" );
    }

    @Test( dependsOnMethods = { "testAddIgnoredArtifacts" } )
    public void testDeleteIgnoredArtifacts()
    {
        String pattern = "**/*.log";
        String path = "//div[@id='contentArea']/div/div[3]/table/tbody/tr[7]/td/code";
        assertElementPresent( path );
        Assert.assertEquals( getSelenium().getText( path ), pattern );
        clickDeleteIcon( pattern );
        assertElementNotPresent( path );
    }

    //
    @Test( dependsOnMethods = { "testDeleteIgnoredArtifacts" } )
    public void testAddIndexableContent_NullValue()
    {
        setFieldValue( "newpattern_3", "" );
        clickAddIcon( "newpattern_3" );
        Assert.assertEquals( getErrorMessageText(),
                             "Unable to process blank pattern." );
    }

    @Test( enabled = false )
    public void testAddIndexableContent_ExistingValue()
    {
        setFieldValue( "newpattern_3", "**/*.xml" );
        clickAddIcon( "newpattern_3" );
        // FIXME: broken
        Assert.assertEquals( getErrorMessageText(),
                             "Not adding pattern \"**/*.xml\" to filetype indexable-content as it already exists." );
    }

    @Test
    public void testAddIndexableContent()
    {
        goToRepositoryScanningPage();
        setFieldValue( "newpattern_3", "**/*.html" );
        clickAddIcon( "newpattern_3" );
        Assert.assertEquals( getSelenium().getTable( "//div[@id='contentArea']/div/div[4]/table.9.0" ), "**/*.html" );
    }

    @Test( dependsOnMethods = { "testAddIndexableContent" } )
    public void testDeleteIndexableContent()
    {
        String pattern = "**/*.html";
        String path = "//div[@id='contentArea']/div/div[4]/table/tbody/tr[10]/td/code";
        assertElementPresent( path );
        Assert.assertEquals( getSelenium().getText( path ), pattern );
        clickDeleteIcon( pattern );
        assertElementNotPresent( path );
    }

    @Test( dependsOnMethods = { "testDeleteIndexableContent" } )
    public void testUpdateConsumers()
    {
        checkField( "enabledKnownContentConsumers" );
        checkField( "//input[@name='enabledKnownContentConsumers' and @value='auto-rename']" );
        clickButtonWithValue( "Update Consumers" );
        assertPage( "Apache Archiva \\ Administration - Repository Scanning" );
    }

    @Test( dependsOnMethods = { "testUpdateConsumers" } )
    public void testUpdateConsumers_UnsetAll()
    {
        getSelenium().uncheck( "enabledKnownContentConsumers" );
        getSelenium().uncheck( "//input[@name='enabledKnownContentConsumers' and @value='auto-rename']" );
        getSelenium().uncheck( "//input[@name='enabledKnownContentConsumers' and @value='create-missing-checksums']" );
        getSelenium().uncheck( "//input[@name='enabledKnownContentConsumers' and @value='index-content']" );
        getSelenium().uncheck( "//input[@name='enabledKnownContentConsumers' and @value='metadata-updater']" );
        getSelenium().uncheck( "//input[@name='enabledKnownContentConsumers' and @value='repository-purge']" );
        getSelenium().uncheck( "//input[@name='enabledKnownContentConsumers' and @value='validate-checksums']" );
        clickButtonWithValue( "Update Consumers" );

        assertPage( "Apache Archiva \\ Administration - Repository Scanning" );
    }
    
    private void clickAddIcon( String fieldId )
    {
        String xPath = "//preceding::td/input[@id='" + fieldId + "']//following::td/a/img";
        clickLinkWithLocator( xPath );
    }

    private void clickDeleteIcon( String pattern )
    {
        String xPath = "//preceding::td/code[contains(text(),'" + pattern + "')]//following::td/a/img";
        clickLinkWithLocator( xPath );
    }

}
