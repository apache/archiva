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
import org.testng.annotations.Test;
import org.testng.Assert;

@Test( groups = { "virtualrepository" }, dependsOnMethods = { "testWithCorrectUsernamePassword" } )
public class VirtualRepositoryTest
    extends AbstractRepositoryTest
{
    public void testAddRepositoryGroupNullValue()
    {
        addRepositoryGroup( " " );
        assertTextPresent( "Identifier field is required." );
    }

    @Test( dependsOnMethods = { "testWithCorrectUsernamePassword" } )
    public void testAddRepositoryGroupValidValue()
    {
        addRepositoryGroup( "testing" );
        Assert.assertEquals( getSelenium().getText( "//div[@id='contentArea']/div[2]/div/div[1]/p[1]" ), "testing" );
    }

    @Test( dependsOnMethods = { "testAddRepositoryGroupValidValue" } )
    public void testAddRepositoryToRepositoryGroup()
    {
        addRepositoryToRepositoryGroup( "testing", "internal" );
        Assert.assertEquals( getSelenium().getText( "//div[@id='contentArea']/div[2]/div/div[3]/div[2]/p[1]" ),
                             "internal" );
    }

    @Test( dependsOnMethods = { "testAddRepositoryToRepositoryGroup" } )
    public void testDeleteRepositoryOfRepositoryGroup()
    {
        deleteRepositoryInRepositoryGroups();
        assertTextPresent( "Repository Groups" );
        assertTextNotPresent( "No Repository Groups Defined." );
        Assert.assertFalse( getSelenium().isElementPresent( "//div[@id='contentArea']/div[2]/div/div[3]/div[2]/p[1]" ) );
    }

    @Test( dependsOnMethods = { "testDeleteRepositoryOfRepositoryGroup" } )
    public void testDeleteRepositoryGroup()
    {
        assertRepositoryGroupsPage();
        deleteRepositoryGroup( "testing" );
        assertTextPresent( "No Repository Groups Defined." );
    }

}