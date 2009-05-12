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

import org.apache.archiva.web.test.parent.AbstractArtifactReportsTest;
import org.testng.annotations.Test;

@Test( groups = { "reports" }, dependsOnMethods = { "testWithCorrectUsernamePassword" } )
public class ReportsTest 
	extends AbstractArtifactReportsTest
{
	//TODO Tests for repository with defects
	
	@Test(dependsOnMethods = { "testWithCorrectUsernamePassword" } )
	public void testRepoStatisticsWithoutRepoCompared()
	{
		goToReportsPage();
		clickButtonWithValue( "View Statistics" );
		assertTextPresent( "Please select a repository (or repositories) from the list." );
	}
	
	@Test(dependsOnMethods = { "testWithCorrectUsernamePassword" } )
	public void testRepositoryStatisticsWithoutDate()
	{
		String repositoryName = p.getProperty( "REPOSITORY_NAME" ) ;
		compareRepositories( "label=" + repositoryName, "", "" );
		//TODO
		assertTextPresent( "Statistics Report" );
	}
	
	@Test(dependsOnMethods = { "testWithCorrectUsernamePassword" } )
	public void testRepositoryStatisticsEndEarlierThanStart()
	{
		String repositoryName = p.getProperty( "REPOSITORY_NAME" ) ;
		String startDate = p.getProperty( "END_DATE" );
		String endDate = p.getProperty( "START_DATE" );
		compareRepositories( "label=" + repositoryName, startDate, endDate );
		//assertTextPresent( "Statistics for Repository '" + repositoryName + "'" );
		assertPage( "Apache Archiva \\ Reports" );
		assertTextPresent( "Start Date must be earlier than the End Date" );
	}
		
	@Test(dependsOnMethods = { "testWithCorrectUsernamePassword" } )	
	public void testRepositoryStatistics()
	{
		String repositoryName = p.getProperty( "REPOSITORY_NAME" ) ;
		String startDate = p.getProperty( "START_DATE" );
		String endDate = p.getProperty( "END_DATE" );
		compareRepositories( "label=" + repositoryName, startDate, endDate );
		//assertTextPresent( "Statistics for Repository '" + repositoryName + "'" );
		assertPage( "Apache Archiva \\ Reports" );
		assertTextPresent( "Statistics Report" );
	}
	
	@Test(dependsOnMethods = { "testWithCorrectUsernamePassword" } )
	public void testRepositoriesStatisticComparisonReport()
	{
		goToReportsPage();
		clickButtonWithValue( "-->>" , false );
		clickButtonWithValue( "View Statistics" );
		assertTextPresent( "Statistics Report" );
	}
	
	@Test(dependsOnMethods = { "testWithCorrectUsernamePassword" } )
	public void testRepositoryHealthWithoutDefect()
	{
		goToReportsPage();
		String groupId = p.getProperty( "ARTIFACT_GROUPID" );
		getSelenium().type( "generateReport_groupId" , groupId );
		clickButtonWithValue( "Show Report" );
		assertPage( "Apache Archiva \\ Reports" );
		assertTextPresent( "The operation generated an empty report." );
	}
	
	@Test(dependsOnMethods = { "testWithCorrectUsernamePassword" } )
	public void testRepositoryHealthWithoutGroupId()
	{
		goToReportsPage();
		clickButtonWithValue( "Show Report" );
		assertPage( "Apache Archiva \\ Reports" );
		assertTextPresent( "The operation generated an empty report." );
		
		//TODO As of the creation of the tests, GroupId is not a required field in showing the reports of repository health. GroupId should be required I think.
	}
	
}