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
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[1]/table/tbody/tr[14]/td[2]/a/img" );
		assertTextPresent( "Unable to process blank pattern." );
	}
	
	@Test (dependsOnMethods = { "testAddArtifactFileType_NullValue" } )
	public void testAddArtifactFileType()
	{
		setFieldValue( "newpattern_0" , "**/*.dll" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[1]/table/tbody/tr[14]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[1]/table.13.0"), "**/*.dll" );
	}
	
	@Test (dependsOnMethods = { "testAddArtifactFileType" } )
	public void testAddArtifactFileType_ExistingValue()
	{
		setFieldValue( "newpattern_0" , "**/*.zip" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[1]/table/tbody/tr[15]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getText("//span[@class='errorMessage']"), "Not adding pattern \"**/*.zip\" to filetype artifacts as it already exists." );
	}
	
	@Test (dependsOnMethods = { "testAddArtifactFileType_ExistingValue" } )
	public void testDeleteArtifactFileType()
	{
		Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[1]/table.13.0"), "**/*.dll" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[1]/table/tbody/tr[14]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[1]/table.13.0"), "" );
	}
	
	@Test (dependsOnMethods = { "testDeleteArtifactFileType" } )
	public void testAddAutoRemove_NullValue()
	{
		setFieldValue( "newpattern_1" , "" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[2]/table/tbody/tr[4]/td[2]/a/img" );
		assertTextPresent( "Unable to process blank pattern." );
	}
	
	@Test (dependsOnMethods = { "testAddAutoRemove_NullValue" } )
	public void testAddAutoRemove_ExistingValue()
	{
		setFieldValue( "newpattern_1" , "**/*-" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[2]/table/tbody/tr[4]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getText("//span[@class='errorMessage']"), "Not adding pattern \"**/*-\" to filetype auto-remove as it already exists." );
	}
	
	@Test (dependsOnMethods = { "testAddAutoRemove_ExistingValue" } )
	public void testAddAutoRemove()
	{
		setFieldValue( "newpattern_1" , "**/*.test" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[2]/table/tbody/tr[4]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[2]/table.3.0"), "**/*.test" );
	}
	
	@Test (dependsOnMethods = { "testAddAutoRemove" } )
	public void testDeleteAutoRemove()
	{
		Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[2]/table.3.0"), "**/*.test" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[2]/table/tbody/tr[4]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[2]/table.3.0"), "" );
	}
	
	@Test (dependsOnMethods = { "testDeleteAutoRemove" } )
	public void testAddIgnoredArtifacts_NullValue()
	{
		setFieldValue( "newpattern_2" , "" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[3]/table/tbody/tr[7]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getText("//span[@class='errorMessage']"), "Unable to process blank pattern." );
	}
	
	@Test (dependsOnMethods = { "testAddIgnoredArtifacts_NullValue" } )
	public void testAddIgnoredArtifacts_ExistingValue()
	{
		setFieldValue( "newpattern_2" , "**/*.sh" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[3]/table/tbody/tr[7]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getText("//span[@class='errorMessage']"), "Not adding pattern \"**/*.sh\" to filetype ignored as it already exists." );
	}
	
	@Test (dependsOnMethods = { "testAddIgnoredArtifacts_ExistingValue" } )
	public void testAddIgnoredArtifacts()
	{
		setFieldValue( "newpattern_2" , "**/*.log" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[3]/table/tbody/tr[7]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[3]/table.6.0"), "**/*.log" );
	}
	
	@Test (dependsOnMethods = { "testAddIgnoredArtifacts" } )
	public void testDeleteIgnoredArtifacts()
	{
		Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[3]/table.6.0"), "**/*.log" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[3]/table/tbody/tr[7]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[3]/table.6.0"), "" );
	 }
	
	//
	@Test (dependsOnMethods = { "testDeleteIgnoredArtifacts" } )
	public void testAddIndexableContent_NullValue()
	{
		setFieldValue( "newpattern_3" , "" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[4]/table/tbody/tr[10]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getText("//span[@class='errorMessage']"), "Unable to process blank pattern." );
	}
	
	@Test (dependsOnMethods = { "testAddIndexableContent_NullValue" } )
	public void testAddIndexableContent_ExistingValue()
	{
		setFieldValue( "newpattern_3" , "**/*.xml" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[4]/table/tbody/tr[10]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getText("//span[@class='errorMessage']"), "Not adding pattern \"**/*.xml\" to filetype indexable-content as it already exists." );
	}
	
	@Test (dependsOnMethods = { "testAddIndexableContent_ExistingValue" } )
	public void testAddIndexableContent()
	{
		setFieldValue( "newpattern_3" , "**/*.html" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[4]/table/tbody/tr[10]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[4]/table.9.0"), "**/*.html" );
	}
	
	@Test (dependsOnMethods = { "testAddIndexableContent" } )
	public void testDeleteIndexableContent()
	{
		Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[4]/table.9.0"), "**/*.html" );
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[4]/table/tbody/tr[10]/td[2]/a/img" );
		Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[4]/table.9.0"), "" );
	}
	
	@Test (dependsOnMethods = { "testDeleteIndexableContent" } )
	public void testUpdateConsumers()
	{
		checkField( "enabledKnownContentConsumers" );
		checkField( "//input[@name='enabledKnownContentConsumers' and @value='auto-rename']" );
		clickButtonWithValue( "Update Consumers" );
		assertPage( "Apache Archiva \\ Administration - Repository Scanning" );
	}
	
	@Test (dependsOnMethods = { "testUpdateConsumers" } )
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
	
}
