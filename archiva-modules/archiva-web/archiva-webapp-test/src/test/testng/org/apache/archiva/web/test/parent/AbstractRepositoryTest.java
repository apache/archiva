package org.apache.archiva.web.test.parent;

import java.io.File;

public abstract class AbstractRepositoryTest 
	extends AbstractArchivaTest
{
	// Repository Groups
	public void goToRepositoryGroupsPage()
	{
		clickLinkWithText( "Repository Groups" );
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
		assertTextPresent( "ID: " + repositoryName );
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
		getSelenium().click( "xpath=//div[@id='contentArea']/div[2]/div/div[1]/div/a/img" );
		waitPage();
		assertDeleteRepositoryGroupPage( repositoryName );
		clickButtonWithValue( "Confirm" );
	}
	
	///////////////////////////////
	// proxy connectors
	///////////////////////////////
	public void goToProxyConnectorsPage()
	{
		clickLinkWithText( "Proxy Connectors" );
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
		assertPage( "Apache Archiva \\ Admin: Add Proxy Connector" );
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
	
	// this only fills in the values of required fields in adding Proxy Connectors
	public void addProxyConnector( String networkProxy, String managedRepo, String remoteRepo )
	{
		goToProxyConnectorsPage();
		clickLinkWithText( "Add" );
		assertAddProxyConnectorPage();
		selectValue( "connector.proxyId" , networkProxy );
		selectValue( "connector.sourceRepoId" , managedRepo );
		selectValue( "connector.targetRepoId" , remoteRepo );
	}
	
	public void deleteProxyConnector()
	{
		goToProxyConnectorsPage();
		clickLinkWithXPath( "//div[@id='contentArea']/div[2]/div[1]/div[2]/div[1]/a[3]/img" );
		assertPage( "Apache Archiva \\ Admin: Delete Proxy Connectors" );
		clickButtonWithValue( "Delete" );
		assertPage( "Apache Archiva \\ Administration - Proxy Connectors" );
	}
	
	///////////////////////////////
	// network proxies
	///////////////////////////////
	public void goToNetworkProxiesPage()
	{
		clickLinkWithText( "Network Proxies" );
		assertNetworkProxiesPage();
	}
	
	public void assertNetworkProxiesPage()
	{
		assertPage( "Apache Archiva \\ Administration - Network Proxies" );
		assertTextPresent( "Administration - Network Proxies" );
		assertTextPresent( "Network Proxies" );
		assertLinkPresent( "Add Network Proxy" );
	}
	
	public void assertAddNetworkProxy()
	{
		assertPage( "Apache Archiva \\ Admin: Add Network Proxy" );
		assertTextPresent( "Admin: Add Network Proxy" );
		assertTextPresent( "Add network proxy:" );
		assertTextPresent( "Identifier*:" );
		assertTextPresent( "Protocol*:" );
		assertTextPresent( "Hostname*:" );
		assertTextPresent( "Port*:" );
		assertTextPresent( "Username:" );
		assertTextPresent( "Password:" );
		assertButtonWithValuePresent( "Save Network Proxy" );
	}
	
	public void addNetworkProxy( String identifier, String protocol, String hostname, String port, String username, String password )
	{
		//goToNetworkProxiesPage();
		clickLinkWithText( "Add Network Proxy" );
		assertAddNetworkProxy();
		setFieldValue( "proxy.id" , identifier );
		setFieldValue( "proxy.protocol" , protocol );
		setFieldValue( "proxy.host" , hostname );
		setFieldValue( "proxy.port" , port );
		setFieldValue( "proxy.username" , username );
		setFieldValue( "proxy.password" , password );
		clickButtonWithValue( "Save Network Proxy" );
	}
	
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
		String remoteElements = "addRemoteRepository_repository_id,addRemoteRepository_repository_name,addRemoteRepository_repository_url,addRemoteRepository_repository_username,addRemoteRepository_repository_password,addRemoteRepository_repository_timeout,addRemoteRepository_repository_layout";
		String[] arrayRemoteElements = remoteElements.split( "," );
		for ( String arrayremotelement : arrayRemoteElements )
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
			//goToRepositoriesPage();
			assertAddRemoteRepository();
			setFieldValue( "addRemoteRepository_repository_id" ,  identifier );
			setFieldValue( "addRemoteRepository_repository_name" , name );
			setFieldValue( "addRemoteRepository_repository_url" , url );
			setFieldValue( "addRemoteRepository_repository_username" , username );
			setFieldValue( "addRemoteRepository_repository_password" , password );
			setFieldValue( "addRemoteRepository_repository_timeout" , timeout );
			selectValue( "addRemoteRepository_repository_layout" , type );
			clickButtonWithValue( "Add Repository" );
	}
	
	public void deleteRemoteRepository()
	{
		goToRepositoriesPage();
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[8]/div[1]/a[2]" );
		assertDeleteRemoteRepositoryPage();
		clickButtonWithValue( "Confirm" );
	}
	
	public void editRemoteRepository( String fieldName, String value)
	{
		goToRepositoriesPage();
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[8]/div[1]/a[1]" );
		setFieldValue( fieldName, value );
		clickButtonWithValue( "Update Repository" );
	}
	
	public void editManagedRepository( String fieldName, String value )
	{
		//goToRepositoriesPage();
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[5]/div[1]/a[1]/img" );
		assertPage( "Apache Archiva \\ Admin: Edit Managed Repository" );
		setFieldValue(fieldName, value);
		//TODO
		clickButtonWithValue( "Update Repository" );
	}
	
	public void deleteManagedRepository()
	{
		clickLinkWithXPath( "//div[@id='contentArea']/div/div[5]/div[1]/a[2]" );
		assertPage( "Apache Archiva \\ Admin: Delete Managed Repository" );
		clickButtonWithValue( "Delete Configuration Only" );
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
		for (String artifacttypes : arrayArtifactTypes )
			assertTextPresent( artifacttypes );
		
		String autoremove = "**/*.bak,**/*~,**/*-";
		String [] arrayAutoremove = autoremove.split( "," );
		for ( String arrayautoremove : arrayAutoremove )
			assertTextPresent( arrayautoremove );
		
		String ignored = "**/.htaccess,**/KEYS,**/*.rb,**/*.sh,**/.svn/**,**/.DAV/**";
		String [] arrayIgnored = ignored.split( "," );
		for ( String arrayignored : arrayIgnored )
			assertTextPresent( arrayignored );
		
		String indexableContent = "**/*.txt,**/*.TXT,**/*.block,**/*.config,**/*.pom,**/*.xml,**/*.xsd,**/*.dtd,**/*.tld";
		String [] arrayIndexableContent = indexableContent.split( "," );
		for ( String indexablecontent : arrayIndexableContent )
			assertTextPresent( indexablecontent );
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
		assertElementPresent( "database_cron" );
		assertButtonWithValuePresent( "Update Cron" );
		assertButtonWithValuePresent( "Update Database Now" );
		assertTextPresent( "Database - Unprocessed Artifacts Scanning" );
		assertTextPresent( "Database - Artifact Cleanup Scanning" );
	}
}
