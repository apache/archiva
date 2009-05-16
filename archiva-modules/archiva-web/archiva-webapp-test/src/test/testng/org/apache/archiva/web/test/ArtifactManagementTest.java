package org.apache.archiva.web.test;

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
}
