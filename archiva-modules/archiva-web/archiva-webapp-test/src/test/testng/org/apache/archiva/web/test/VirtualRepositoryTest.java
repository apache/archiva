package org.apache.archiva.web.test;

import org.apache.archiva.web.test.parent.AbstractRepositoryTest;
import org.testng.annotations.Test;

@Test( groups = { "virtualrepository" }, dependsOnMethods = { "testWithCorrectUsernamePassword" } )
public class VirtualRepositoryTest 
	extends AbstractRepositoryTest
{
	public void testAddRepositoryNullValue()
	{
		addRepositoryGroup( " " );
		assertTextPresent( "Identifier field is required." );
	}
	
	@Test(dependsOnMethods = { "testWithCorrectUsernamePassword" } )
	public void testAddRepositoryValidValue()
	{
		addRepositoryGroup( "testing" );
		//assertAddedRepositoryLink( "testing" );
		assertTextPresent( "testing" );
	}
	
	//@Test(dependsOnMethods = { "testAddRepositoryValidValue" } )
	public void testAddRepositoryToRepositoryGroup()
	{
		addRepositoryToRepositoryGroup( "testing", "internal" );
		assertTextPresent( "internal" );
		//clickLinkWithXPath( "/html/body/div[4]/div/div/div[2]/div/div/p[2]/a" );
		//getSelenium().goBack();
	}
	
	@Test(dependsOnMethods = { "testAddRepositoryToRepositoryGroup" } )
	public void testDeleteRepositoryOfRepositoryGroup()
	{
		deleteRepositoryInRepositoryGroups();
		assertTextPresent( "Repository Groups" );
		assertTextNotPresent( "No Repository Groups Defined." );
	}
	
	@Test(dependsOnMethods = { "testDeleteRepositoryOfRepositoryGroup" } )
	public void testDeleteRepositoryGroup()
	{
		deleteRepositoryGroup( "testing" );
		assertTextPresent( "No Repository Groups Defined." );
	}
	
	/*@Test(dependsOnMethods = { "testAddRepositoryToRepositoryGroup" } )
	public void testCheckRepositoryGroup()
	{
		clickLinkWithXPath( "/html/body/div[4]/div/div/div[2]/div/div/p[2]/a" );
		getSelenium().goBack();
	}*/
}
