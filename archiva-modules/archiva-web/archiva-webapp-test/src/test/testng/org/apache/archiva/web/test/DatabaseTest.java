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

@Test( groups = { "database" }, dependsOnMethods = { "testWithCorrectUsernamePassword" }, sequential = true )
public class DatabaseTest
	extends AbstractRepositoryTest
{
	public void testUpdateCron_NullValue()
	{
		goToDatabasePage();
		setFieldValue( "database_cron" , "");
		clickButtonWithValue( "Update Cron" );
		assertTextPresent( "Invalid cron expression value(s)" );
		assertTextPresent( "You must enter a cron expression." );
	}
	
	@Test (dependsOnMethods = { "testUpdateCron_NullValue" } )
	public void testUpdateCron_InvalidValue()
	{
		setFieldValue( "database_cron" , "asdf" );
		clickButtonWithValue( "Update Cron" );
		assertTextPresent( "Invalid cron expression value(s)" );
	}
	
	@Test (dependsOnMethods = { "testUpdateCron_InvalidValue" } )
	public void testUpdateCron_ValidValue()
	{
		setFieldValue( "database_cron" , "0 0 * * * ?" );
		clickButtonWithValue( "Update Cron" );
		assertPage( "Apache Archiva \\ Administration - Database" );
	}
	
	@Test (dependsOnMethods = { "testUpdateCron_ValidValue" } )
	public void testUpdateConsumersUnprocessedArtifactsScanning_UnsetAll()
	{
		getSelenium().uncheck( "enabledUnprocessedConsumers" );
		clickSubmitWithLocator( "//input[@id='database_0' and @value='Update Consumers']" );
		
		assertPage( "Apache Archiva \\ Administration - Database" );
	}
	
	@Test (dependsOnMethods = { "testUpdateConsumersUnprocessedArtifactsScanning_UnsetAll" } )
	public void testUpdateConsumersUnprocessedArtifactsScanning()
	{
		checkField( "enabledUnprocessedConsumers" );
		clickSubmitWithLocator( "//input[@id='database_0' and @value='Update Consumers']" );
		assertPage( "Apache Archiva \\ Administration - Database" );
	}
	
	@Test (dependsOnMethods = { "testUpdateConsumersUnprocessedArtifactsScanning" } )
	public void testUpdateConsumersArtifactCleanupScanning_UnsetAll()
	{
		getSelenium().uncheck( "enabledCleanupConsumers" );
		getSelenium().uncheck( "//input[@name='enabledCleanupConsumers' and @value='not-present-remove-db-project']" );
		getSelenium().uncheck( "//input[@name='enabledCleanupConsumers' and @value='not-present-remove-indexed']" );
		clickSubmitWithLocator( "//form[@id='database']/table/tbody/tr[5]/td/input" );
		assertPage( "Apache Archiva \\ Administration - Database" );
	}
	
	@Test (dependsOnMethods = { "testUpdateConsumersArtifactCleanupScanning_UnsetAll" } )
	public void testUpdateConsumersArtifactCleanupScanning()
	{
		checkField( "enabledCleanupConsumers" );
		clickSubmitWithLocator( "//form[@id='database']/table/tbody/tr[5]/td/input" );
		assertPage( "Apache Archiva \\ Administration - Database" );
	}
	
	@Test (dependsOnMethods = { "testUpdateConsumersArtifactCleanupScanning" } )
	public void testUpdateDatabaseNow()
	{
		clickButtonWithValue( "Update Database Now" );
		assertPage( "Apache Archiva \\ Administration - Database" );
	}
}
