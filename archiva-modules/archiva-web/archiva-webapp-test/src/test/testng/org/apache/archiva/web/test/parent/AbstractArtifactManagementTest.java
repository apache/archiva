package org.apache.archiva.web.test.parent;

public abstract class AbstractArtifactManagementTest
	extends AbstractArchivaTest
{
	
	public String getGroupId()
	{
		String groupId = getProperty( "GROUPID" ) ;
		return groupId;
	}
	
	public String getArtifactId()
	{
		String artifactId = getProperty( "ARTIFACTID" ) ;
		return artifactId;
	}
	
	public String getVersion()
	{
		String version = getProperty( "VERSION" ) ;
		return version;
	}
	
	public String getPackaging()
	{
		String packaging = getProperty( "PACKAGING" ) ;
		return packaging;
	}
	
	public String getArtifactFilePath()
	{
		return "src/test/it-resources/snapshots/org/apache/maven/archiva/web/test/foo-bar/1.0-SNAPSHOT/foo-bar-1.0-SNAPSHOT.jar";
	}
	
	public String getRepositoryId()
	{
		String repositoryId = getProperty( "REPOSITORYID" ) ;
		return repositoryId;
	}
	
	public void goToDeleteArtifactPage()
	{
        login( getProperty( "ADMIN_USERNAME" ), getProperty( "ADMIN_PASSWORD" ) );
		getSelenium().open( "/archiva/deleteArtifact.action" );
		assertDeleteArtifactPage();
	}
		
	public void deleteArtifact( String groupId, String artifactId, String version, String repositoryId )
	{
		goToDeleteArtifactPage();
		setFieldValue( "groupId" , groupId );
		setFieldValue( "artifactId" , artifactId );
		setFieldValue( "version" , version );
		selectValue( "repositoryId" ,  repositoryId );
		clickButtonWithValue( "Submit" ) ;
	}
}
