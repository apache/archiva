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

import org.apache.archiva.web.test.parent.AbstractArtifactManagementTest;
import org.testng.annotations.Test;

@Test( groups = { "userroles" }, dependsOnMethods = { "testWithCorrectUsernamePassword" } )
public class ArtifactManagementTest
	extends AbstractArtifactManagementTest
{

	
	public void testAddArtifactNullValues()
	{
		goToAddArtifactPage();
		clickButtonWithValue( "Submit" );
		assertTextPresent( "Please add a file to upload." );
		assertTextPresent( "Invalid version." );
		assertTextPresent( "You must enter a groupId." );
		assertTextPresent( "You must enter an artifactId." );
		assertTextPresent( "You must enter a version" );
		assertTextPresent( "You must enter a packaging" );
	}
	
	@Test(dependsOnMethods = { "testAddArtifactNullValues" } )
	public void testAddArtifactNoGroupId()
	{
		addArtifact( " " , getArtifactId(), getVersion(), getPackaging() , getArtifactFilePath(), getRepositoryId() );
		assertTextPresent( "You must enter a groupId." );
	}
	
	@Test(dependsOnMethods = { "testAddArtifactNoGroupId" } )
	public void testAddArtifactNoArtifactId()
	{
		
		addArtifact( getGroupId() , " ", getVersion(), getPackaging() , getArtifactFilePath(), getRepositoryId() );
		assertTextPresent( "You must enter an artifactId." );
	}
	
	@Test(dependsOnMethods = { "testAddArtifactNoGroupId" } )
	public void testAddArtifactNoVersion()
	{
		addArtifact( getGroupId() , getArtifactId(), " ", getPackaging() , getArtifactFilePath(), getRepositoryId() );
		assertTextPresent( "You must enter a version." );
	}
	
	@Test(dependsOnMethods = { "testAddArtifactNoGroupId" } )
	public void testAddArtifactInvalidVersion()
	{
		addArtifact( getGroupId() , getArtifactId(), "asdf", getPackaging() , getArtifactFilePath(), getRepositoryId() );
		assertTextPresent( "Invalid version." );
	}
	
	@Test(dependsOnMethods = { "testAddArtifactNoGroupId" } )
	public void testAddArtifactNoPackaging()
	{
		addArtifact( getGroupId() , getArtifactId(), getVersion(), " " , getArtifactFilePath(), getRepositoryId() );
		assertTextPresent( "You must enter a packaging." );
	}
	
	@Test(dependsOnMethods = { "testAddArtifactNoGroupId" } )
	public void testAddArtifactNoFilePath()
	{
		addArtifact( getGroupId() , getArtifactId(), getVersion(), getPackaging() , " ", getRepositoryId() );
		assertTextPresent( "Please add a file to upload." );
	}
	
	public void testAddArtifactValidValues()
	{
		addArtifact( getGroupId() , getArtifactId(), getVersion(), getPackaging() , getArtifactFilePath(), getRepositoryId() );
		assertTextPresent( "Artifact 'test:test:1.0' was successfully deployed to repository 'internal'" );
	}
	
	public void testAddArtifactValidValues1()
	{
		String groupId = p.getProperty( "GROUPID1" );
		String artifactId = p.getProperty( "ARTIFACTID1" );
		String version = p.getProperty( "VERSION1" );
		String packaging = p.getProperty( "PACKAGING1" );
		String repositoryId = p.getProperty( "REPOSITORYID1" );
		addArtifact( groupId , artifactId, version, packaging , getArtifactFilePath(), repositoryId );
		assertTextPresent( "Artifact 'delete:delete:1.0' was successfully deployed to repository 'internal'" );
	}
	
	@Test(dependsOnMethods = { "testAddArtifactValidValues1" } )
	public void testDeleteArtifact()
	{
		deleteArtifact( "delete", "delete", "1.0", "internal");
		assertTextPresent( "Artifact 'delete:delete:1.0' was successfully deleted from repository 'internal'" );
	}
	
	public void testDeleteArtifactNoGroupId()
	{
		deleteArtifact( " ", "delete", "1.0", "internal");
		assertTextPresent( "You must enter a groupId." );
	}
	
	public void testDeleteArtifactNoArtifactId()
	{
		deleteArtifact( "delete", " ", "1.0", "internal");
		assertTextPresent( "You must enter an artifactId." );
	}
	
	public void testDeleteArtifactNoVersion()
	{
		deleteArtifact( "delete", "delete", " ", "internal");
		assertTextPresent( "Invalid version." );
		assertTextPresent( "You must enter a version." );
	}
	
	public void testDeleteArtifactInvalidVersion()
	{
		deleteArtifact( "delete", "delete", "asdf", "internal");
		assertTextPresent( "Invalid version." );
	}
}
