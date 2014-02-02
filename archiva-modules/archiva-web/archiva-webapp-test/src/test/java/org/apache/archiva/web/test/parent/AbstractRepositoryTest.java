package org.apache.archiva.web.test.parent;

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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.testng.Assert;

public abstract class AbstractRepositoryTest 
	extends AbstractArchivaTest
{

    public static final String DEFAULT_NETWORK_PROXY = "(direct connection)";

    // Repository Groups
	public void goToRepositoryGroupsPage()
	{
	    if( !getTitle().equals( "Apache Archiva \\ Administration - Repository Groups" ) )
	    {
	        getSelenium().open( "/archiva/admin/repositoryGroups.action" );
	    }
		assertRepositoryGroupsPage();
	}
	
	public void assertRepositoryGroupsPage()
	{
		assertPage( "Apache Archiva \\ Administration - Repository Groups" );
		assertTextPresent( "Administration - Repository Groups" );
		assertTextPresent( "Identifier*:" );
		assertElementPresent( "repositoryGroup.id" );
		assertButtonWithValuePresent( "Add Group" );
		assertTextPresent( "Repository Groups" );
	}
	
	public void assertAddedRepositoryLink( String repositoryGroupName)
	{
		assertPage( "Apache Archiva \\ Administration - Repository Groups" );
		String repositoryGroupUrlValue = "repository/" + repositoryGroupName + "/";
		String baseUrlValue = "archiva";
		String repositoryGroupLink = baseUrl.replaceFirst( baseUrlValue, repositoryGroupUrlValue);
		assertTextPresent( repositoryGroupLink );
	}
	
	public void assertAddedRepositoryToRepositoryGroups( String repositoryName)
	{
		assertPage( "Apache Archiva \\ Administration - Repository Groups" );
		assertTextPresent( repositoryName );
		assertTextPresent( "Archiva Managed Internal Repository" );
		assertAddedRepositoryLink( repositoryName );
	}
	
	public void assertDeleteRepositoryGroupPage( String repositoryName)
	{
		assertPage( "Apache Archiva \\ Admin: Delete Repository Group" );
		assertTextPresent( "WARNING: This operation can not be undone." );
		assertTextPresent( "Are you sure you want to delete the following repository group?" );
		assertElementPresent( "//preceding::td[text()='ID:']//following::td/code[text()='" + repositoryName + "']" );
		assertButtonWithValuePresent( "Confirm" );
		assertButtonWithValuePresent( "Cancel" );
	}
	
	public void addRepositoryGroup( String repoGroupName )
	{
		goToRepositoryGroupsPage();
		setFieldValue( "repositoryGroup.id", repoGroupName );
		clickButtonWithValue( "Add Group" );
	}
	
	public void addRepositoryToRepositoryGroup( String repositoryGroupName, String repositoryName )
	{
		goToRepositoryGroupsPage();
		String s = getSelenium().getBodyText();
		if ( s.contains( "No Repository Groups Defined." ) )
		{
			setFieldValue( "repositoryGroup.id" , repositoryGroupName );
			clickButtonWithValue( "Add Group" );
			//assertAddedRepositoryLink( repositoryGroupName );
			
			selectValue( "addRepositoryToGroup_repoId" , repositoryName );
			clickButtonWithValue( "Add Repository" );
			assertAddedRepositoryToRepositoryGroups( repositoryName );
		}
		else
		{
			//assertAddedRepositoryLink( repositoryGroupName );
			selectValue( "addRepositoryToGroup_repoId" , repositoryName );
			clickButtonWithValue( "Add Repository" );
		}
	}
	
	public void deleteRepositoryInRepositoryGroups()
	{
		goToRepositoryGroupsPage();
		getSelenium().click( "xpath=//div[@id='contentArea']/div[2]/div/div[3]/div[1]/a/img" );
		waitPage();
	}
	
	public void deleteRepositoryGroup( String repositoryName )
	{
        attemptDeleteRepositoryGroup( repositoryName );
		clickButtonWithValue( "Confirm" );
	}

    protected void attemptDeleteRepositoryGroup( String repositoryName )
    {
        getSelenium().click( "xpath=//div[@id='contentArea']/div[2]/div/div[1]/div/a/img" );
        waitPage();
        assertDeleteRepositoryGroupPage( repositoryName );
    }

    ///////////////////////////////
	// proxy connectors
	///////////////////////////////
	public void goToProxyConnectorsPage()
	{
        getSelenium().open( "/archiva/admin/proxyConnectors.action" );
		assertProxyConnectorsPage();
	}
	
	public void assertProxyConnectorsPage()
	{
		assertPage( "Apache Archiva \\ Administration - Proxy Connectors" );
		assertTextPresent( "Administration - Proxy Connectors" );
		assertTextPresent( "Repository Proxy Connectors" );
		assertTextPresent( "internal" );
		assertTextPresent( "Archiva Managed Internal Repository" );
		assertTextPresent( "Proxy Connector" );
		assertTextPresent( "Central Repository" );
		assertTextPresent( "Java.net Repository for Maven 2" );
	}
	
	public void assertAddProxyConnectorPage()
	{
        assertAddProxyConnectorPageTitle();
        assertTextPresent( "Admin: Add Proxy Connector" );
		String proxy = "Network Proxy*:,Managed Repository*:,Remote Repository*:,Policies:,Return error when:,On remote error:,Releases:,Snapshots:,Checksum:,Cache failures:,Properties:,No properties have been set.,Black List:,No black list patterns have been set.,White List:,No white list patterns have been set.";
		String[] arrayProxy = proxy.split( "," );
		for ( String arrayproxy : arrayProxy )
			assertTextPresent( arrayproxy );
		/*String proxyElements = "addProxyConnector_connector_proxyId,addProxyConnector_connector_sourceRepoId,addProxyConnector_connector_targetRepoId,policy_propagate-errors-on-update,policy_propagate-errors,policy_releases,policy_snapshots,policy_checksum,policy_cache-failures,propertiesEntry,propertiesValue,blackListEntry,whiteListEntry";
		String[] arrayProxyElements = proxyElements.split( "," );
		for ( String arrayproxyelements : arrayProxyElements )
			assertTextPresent( arrayproxyelements );*/
		assertButtonWithValuePresent( "Add Property" );
		assertButtonWithValuePresent( "Add Pattern" );
		assertButtonWithValuePresent( "Add Proxy Connector" );
	}

    private void assertAddProxyConnectorPageTitle()
    {
        assertPage( "Apache Archiva \\ Admin: Add Proxy Connector" );
    }

    public void addProxyConnector( String networkProxy, String managedRepo, String remoteRepo )
	{
		goToProxyConnectorsPage();
        String xpath = getProxyConnectorXPath( managedRepo, remoteRepo );
        assertElementNotPresent( "xpath=" + xpath );

		clickLinkWithText( "Add" );
		assertAddProxyConnectorPage();
		selectValue( "connector.proxyId", networkProxy );
		selectValue( "connector.sourceRepoId", managedRepo );
		selectValue( "connector.targetRepoId", remoteRepo );

        selectValue( "policy_releases", "never" );

        addProxyProperty( "a", "b" );
        assertAddProxyConnectorPageTitle();
        addProxyProperty( "x", "y" );

        addPattern( "blackListPattern", "**/bad/**" );
        assertAddProxyConnectorPageTitle();
        addPattern( "whiteListPattern", "**/good/**" );
        assertAddProxyConnectorPageTitle();

        clickButtonWithValue( "Add Proxy Connector" );

        assertProxyConnectorsPage();
        assertElementPresent( xpath );

        LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("a", "b");
        properties.put("x", "y");
        assertProxySettings( managedRepo, remoteRepo, "never", "**/bad/**", "**/good/**", properties );
    }

    public void editProxyConnector( String managedRepo, String remoteRepo )
	{
		goToProxyConnectorsPage();
        String xpath = getProxyConnectorXPath( managedRepo, remoteRepo );

        clickLinkWithXPath( xpath + "/..//a[@class='edit']" );

        assertEditProxyConnectorPage();

        selectValue( "policy_releases", "always" );

        addProxyProperty( "c", "d" );
        assertEditProxyConnectorPage();
        editProxyProperty( "x", "z" );
        assertEditProxyConnectorPage();
        removeProxyProperty( "a" );
        assertEditProxyConnectorPage();

        removePattern( "blackListPattern", "**/bad/**" );
        assertEditProxyConnectorPage();
        removePattern( "whiteListPattern", "**/good/**" );
        addPattern( "blackListPattern", "**/bad2/**" );
        assertEditProxyConnectorPage();
        addPattern( "whiteListPattern", "**/good2/**" );

        clickButtonWithValue( "Save Proxy Connector" );

        assertProxyConnectorsPage();
        assertElementPresent( xpath );

        LinkedHashMap<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("c", "d");
        properties.put("x", "z");
        assertProxySettings( managedRepo, remoteRepo, "always", "**/bad2/**", "**/good2/**", properties );
    }

    private void assertEditProxyConnectorPage()
    {
        assertPage( "Apache Archiva \\ Admin : Edit Proxy Connector" );
    }

    private void removeProxyProperty( String name )
    {
        clickLinkWithXPath( "//a[@title='Remove [" + name + "] Property']" );
    }

    private void editProxyProperty( String name, String value )
    {
        setFieldValue( "property_" + name, value );
    }

    private void addPattern( String pattern, String value )
    {
        setFieldValue( pattern, value );
        clickLinkWithXPath( "//input[@name='" + pattern + "']/../input[@type='button']" );
    }

    private void removePattern( String pattern, String value )
    {
        setFieldValue( pattern, value );
        clickLinkWithXPath( "//a[@title='Remove [" + value + "] Pattern']" );
    }

    private void addProxyProperty( String name, String value )
    {
        setFieldValue( "propertyKey", name );
        setFieldValue( "propertyValue", value );
        clickButtonWithValue( "Add Property" );
    }

    private void assertProxySettings( String managedRepoId, String remoteRepoId, String releasesPolicy,
                                      String blackList, String whiteList, LinkedHashMap<String, String> properties )
    {
        String xpath = getProxyConnectorXPath( managedRepoId, remoteRepoId );
        clickLinkWithXPath( xpath + "/ancestor::div[contains(@class,'connector')]//a[text()='Settings']", false );

        String tableElement = "//table[@class='policies']";
        int row = 2;
        Assert.assertEquals( getCellValueFromTable( tableElement, row, 0 ), "releases" );
        Assert.assertEquals( getCellValueFromTable( tableElement, row, 1 ), releasesPolicy );

        Assert.assertEquals( getSelenium().getText( "//th[contains(text(), 'Black List')]/ancestor::tr/td/p" ),
                             "\"" + blackList + "\"" );
        Assert.assertEquals( getSelenium().getText( "//th[contains(text(), 'White List')]/ancestor::tr/td/p" ),
                             "\"" + whiteList + "\"" );

        tableElement = "//table[@class='props']";
        row = 0;
        for ( Map.Entry property : properties.entrySet() )
        {
            Assert.assertEquals( getCellValueFromTable( tableElement, row, 0 ), property.getKey() );
            Assert.assertEquals( getCellValueFromTable( tableElement, row, 1 ), property.getValue() );
            row = row + 1;
        }
    }

    public void addProxyConnector( String managedRepo, String remoteRepo )
	{
        addProxyConnector( DEFAULT_NETWORK_PROXY, managedRepo, remoteRepo );
	}
	
	public void deleteProxyConnector( String managedRepoId, String remoteRepoId, boolean failIfMissing )
	{
		goToProxyConnectorsPage();

        String xpathBase = getProxyConnectorXPath( managedRepoId, remoteRepoId );
        String xpath = xpathBase + "/..//a[@class='delete']";
        if ( failIfMissing || isElementPresent( "xpath=" + xpath ))
        {
            clickLinkWithXPath( xpath );
            assertPage( "Apache Archiva \\ Admin: Delete Proxy Connector" );
		    clickButtonWithValue( "Delete" );
		    assertPage( "Apache Archiva \\ Administration - Proxy Connectors" );

            assertElementNotPresent( xpathBase );
        }
	}

	public void disableProxyConnector( String managedRepoId, String remoteRepoId, boolean failIfMissing )
	{
        toggleProxyConnector( managedRepoId, remoteRepoId, failIfMissing, "Disable" );
    }

	public void enableProxyConnector( String managedRepoId, String remoteRepoId, boolean failIfMissing )
	{
        toggleProxyConnector( managedRepoId, remoteRepoId, failIfMissing, "Enable" );
    }

    private void toggleProxyConnector( String managedRepoId, String remoteRepoId, boolean failIfMissing,
                                       String operation )
    {
        goToProxyConnectorsPage();

        String xpathBase = getProxyConnectorXPath( managedRepoId, remoteRepoId );
        String xpath = xpathBase + "/..//a[@title='" + operation + " Proxy Connector']";
        if ( failIfMissing || isElementPresent( "xpath=" + xpath ) )
        {
            clickLinkWithXPath( xpath );
            assertPage( "Apache Archiva \\ Admin: " + operation + " Proxy Connector" );
            clickButtonWithValue( operation );
            assertPage( "Apache Archiva \\ Administration - Proxy Connectors" );
        }
    }

    private String getProxyConnectorXPath( String managedRepoId, String remoteRepoId )
    {
        String managedRepoXpath = "//div[@class='managedRepo' and ./p[text()='" + managedRepoId + "']]";
        String remoteRepoXpath = "//div[@class='remoteRepo' and ./p[text()='" + remoteRepoId + "']]";
        return "//div[@id='contentArea']" + managedRepoXpath + "/.." + remoteRepoXpath;
    }

    ///////////////////////////////
	// network proxies
	///////////////////////////////
	
	public void editNetworkProxies( String fieldName, String value)
	{
		//goToNetworkProxiesPage();
		clickLinkWithText( "Edit Network Proxy" );
		setFieldValue( fieldName, value);
		clickButtonWithValue( "Save Network Proxy" );
	}
	
	public void deleteNetworkProxy()
	{
		//goToNetworkProxiesPage();
		clickLinkWithText( "Delete Network Proxy" );
		assertPage( "Apache Archiva \\ Admin: Delete Network Proxy" );
		assertTextPresent( "WARNING: This operation can not be undone." );
		clickButtonWithValue( "Delete" );
	}
	
	// remote repositories
	public void assertAddRemoteRepository()
	{
		assertPage( "Apache Archiva \\ Admin: Add Remote Repository" );
		String remote = "Identifier*:,Name*:,URL*:,Username:,Password:,Timeout in seconds:,Type:";
		String[] arrayRemote = remote.split( "," );
		for ( String arrayremote : arrayRemote )
			assertTextPresent( arrayremote );
        String[] remoteElements =
            new String[]{ "addRemoteRepository_commit_repository_id", "addRemoteRepository_commit_repository_name",
                "addRemoteRepository_commit_repository_url", "addRemoteRepository_commit_repository_username",
                "addRemoteRepository_commit_repository_password", "addRemoteRepository_commit_repository_timeout",
                "addRemoteRepository_commit_repository_layout" };
        for ( String arrayremotelement : remoteElements )
			assertElementPresent( arrayremotelement );
	}
	
	public void assertDeleteRemoteRepositoryPage()
	{
		assertPage( "Apache Archiva \\ Admin: Delete Remote Repository" );
		assertTextPresent( "Admin: Delete Remote Repository" );
		assertTextPresent( "WARNING: This operation can not be undone." );
		assertTextPresent( "Are you sure you want to delete the following remote repository?" );
		assertButtonWithValuePresent( "Confirm" );
		assertButtonWithValuePresent( "Cancel" );
	}
	
	public void addRemoteRepository( String identifier, String name, String url, String username, String password, String timeout, String type )
	{
			if (!getSelenium().getLocation().contains( "/archiva/admin/addRemoteRepository.action" )) {
                getSelenium().open( "/archiva/admin/addRemoteRepository.action" );
            }
			assertAddRemoteRepository();
			setFieldValue( "addRemoteRepository_commit_repository_id" ,  identifier );
			setFieldValue( "addRemoteRepository_commit_repository_name" , name );
			setFieldValue( "addRemoteRepository_commit_repository_url" , url );
			setFieldValue( "addRemoteRepository_commit_repository_username" , username );
			setFieldValue( "addRemoteRepository_commit_repository_password" , password );
			setFieldValue( "addRemoteRepository_commit_repository_timeout" , timeout );
			selectValue( "addRemoteRepository_commit_repository_layout" , type );
			clickButtonWithValue( "Add Repository" );
	}
	
	public void deleteRemoteRepository( String id, boolean validate )
    {
        deleteRemoteRepository( id, validate, "Confirm" );
    }

	public void deleteRemoteRepository( String id, boolean validate, String option )
	{
		goToRepositoriesPage();
        String xpath = "//div[@id='contentArea']//a[contains(@href,'confirmDeleteRemoteRepository.action?repoid=" + id + "')]";
        if ( validate || isElementPresent( "xpath=" + xpath ) )
        {
            clickLinkWithXPath( xpath );
		assertDeleteRemoteRepositoryPage();
		clickButtonWithValue( option );
        }
	}
	
	public void editRemoteRepository( String fieldName, String value)
	{
		goToRepositoriesPage();
        clickLinkWithXPath( getRepositoryXpath( "remoterepoedit" ) + "//a[contains(text(),'Edit')]" );
		setFieldValue( fieldName, value );
		clickButtonWithValue( "Update Repository" );
	}
	
	public void editManagedRepository( String fieldName, String value )
	{	    
		goToRepositoriesPage();
        clickLinkWithXPath( getRepositoryXpath( "managedrepoedit" ) + "//a[contains(text(),'Edit')]" );
        assertPage( "Apache Archiva \\ Admin: Edit Managed Repository" );
		setFieldValue(fieldName, value);
		clickButtonWithValue( "Update Repository" );
	}

    private String getRepositoryXpath( String repositoryId )
    {
        return "//code[text()='" + repositoryId + "']/ancestor::div[contains(@class,'repository')]";
    }

    public void editManagedRepository(String name, String directory, String indexDirectory, String type, String cron, String daysOlder, String retentionCount)
        {
                goToRepositoriesPage();
		clickLinkWithXPath( getRepositoryXpath( "managedrepoedit" ) + "//a[contains(text(),'Edit')]");
		assertPage( "Apache Archiva \\ Admin: Edit Managed Repository" );
                setFieldValue( "repository.name" , name );
                setFieldValue( "repository.location" , directory );
                setFieldValue( "repository.indexDir" , indexDirectory );
                selectValue( "repository.layout", type );
                setFieldValue( "repository.refreshCronExpression" , cron );
                setFieldValue( "repository.daysOlder" , daysOlder );
                setFieldValue( "repository.retentionCount" , retentionCount );
                clickButtonWithValue( "Update Repository" );
        }
	
	public void deleteManagedRepository( String id, boolean validate, String option )
	{
        String xpath = "//div[@id='contentArea']//a[contains(@href,'confirmDeleteRepository.action?repoid=" + id + "')]";
        if ( validate || isElementPresent( "xpath=" + xpath ) )
        {
            clickLinkWithXPath( xpath );
            assertPage( "Apache Archiva \\ Admin: Delete Managed Repository" );
            clickButtonWithValue( option );
        }
    }
	
	public String getRepositoryDir()
	{
		File f = new File( "" );
		String artifactFilePath = f.getAbsolutePath();
		return artifactFilePath + "/target/";
	}
	
	/////////////////////////////////////////////
	// Repository Scanning
	/////////////////////////////////////////////
	public void goToRepositoryScanningPage()
	{
		clickLinkWithText( "Repository Scanning" );
		assertRepositoryScanningPage();
	}
	
	public void assertRepositoryScanningPage()
	{
		assertPage( "Apache Archiva \\ Administration - Repository Scanning" );
		assertTextPresent( "Administration - Repository Scanning" );
		assertTextPresent( "Repository Scanning - File Types" );
		String artifactsTypes = "**/*.pom,**/*.jar,**/*.ear,**/*.war,**/*.car,**/*.sar,**/*.mar,**/*.rar,**/*.dtd,**/*.tld,**/*.tar.gz,**/*.tar.bz2,**/*.zip";
		String [] arrayArtifactTypes = artifactsTypes.split( "," );
		for (int i = 0; i < arrayArtifactTypes.length; i++)
			Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[1]/table."+i+".0"), arrayArtifactTypes[i]);
		
		String autoremove = "**/*.bak,**/*~,**/*-";
		String [] arrayAutoremove = autoremove.split( "," );
		for (int i = 0; i < arrayAutoremove.length; i++)
			Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[2]/table."+i+".0"), arrayAutoremove[i]);
		
		String ignored = "**/.htaccess,**/KEYS,**/*.rb,**/*.sh,**/.svn/**,**/.DAV/**";
		String [] arrayIgnored = ignored.split( "," );
		for (int i = 0; i < arrayIgnored.length; i++)
			Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[3]/table."+i+".0"), arrayIgnored[i]);
		
		String indexableContent = "**/*.txt,**/*.TXT,**/*.block,**/*.config,**/*.pom,**/*.xml,**/*.xsd,**/*.dtd,**/*.tld";
		String [] arrayIndexableContent = indexableContent.split( "," );
		for (int i = 0; i < arrayIndexableContent.length; i++)
			Assert.assertEquals(getSelenium().getTable("//div[@id='contentArea']/div/div[4]/table."+i+".0"), arrayIndexableContent[i]);
	}
	
	/////////////////////////////////////////////
	// Database
	/////////////////////////////////////////////
	public void goToDatabasePage()
	{
		clickLinkWithText( "Database" );
		assertDatabasePage();
	}
	
	public void assertDatabasePage()
	{
		assertPage( "Apache Archiva \\ Administration - Database" );
		assertTextPresent( "Administration - Database" );
		assertTextPresent( "Database - Unprocessed Artifacts Scanning" );
		assertTextPresent( "Cron:" );
		assertElementPresent( "database_updateSchedule_cron" );
		assertButtonWithValuePresent( "Update Cron" );
		assertButtonWithValuePresent( "Update Database Now" );
		assertTextPresent( "Database - Unprocessed Artifacts Scanning" );
		assertTextPresent( "Database - Artifact Cleanup Scanning" );
	}
}
