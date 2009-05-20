package org.apache.archiva.web.test.parent;

import java.io.File;

public abstract class AbstractArtifactManagementTest 
	extends AbstractArchivaTest
{
	
	public String getGroupId()
	{
		String groupId = p.getProperty( "GROUPID" ) ;
		return groupId;
	}
	
	public String getArtifactId()
	{
		String artifactId = p.getProperty( "GROUPID" ) ;
		return artifactId;
	}
	
	public String getVersion()
	{
		String version = p.getProperty( "VERSION" ) ;
		return version;
	}
	
	public String getPackaging()
	{
		String packaging = p.getProperty( "PACKAGING" ) ;
		return packaging;
	}
	
	public String getArtifactFilePath()
	{
		File f = new File( "" );
		String artifactFilePath = f.getAbsolutePath();
		return artifactFilePath + "/src/test/it-resources/snapshots/org/apache/maven/archiva/web/test/foo-bar/1.0-SNAPSHOT/foo-bar-1.0-SNAPSHOT.jar" ;
	}
	
	public String getRepositoryId()
	{
		String repositoryId = p.getProperty( "REPOSITORYID" ) ;
		return repositoryId;
	}
	
	public void goToAddArtifactPage()
	{
		clickLinkWithText( "Upload Artifact" );
		assertAddArtifactPage();
	}
	
	public void goToDeleteArtifactPage()
	{
		clickLinkWithText( "Delete Artifact" );
		assertDeleteArtifactPage();
	}
		
	public void addArtifact( String groupId, String artifactId, String version, String packaging, String artifactFilePath, String repositoryId )
	{
		addArtifact(groupId, artifactId, version, packaging, true,  artifactFilePath, repositoryId);
	}
	
	public void addArtifact( String groupId, String artifactId, String version, String packaging, boolean generatePom, String artifactFilePath, String repositoryId)
	{
		goToAddArtifactPage();
		setFieldValue( "groupId" , groupId );
		setFieldValue( "artifactId" , artifactId );
		setFieldValue( "version" , version );
		setFieldValue( "packaging" , packaging );
		
		if ( generatePom )
		{
			checkField( "generatePom" );
		}
		
		setFieldValue( "artifact" , artifactFilePath );
		setFieldValue( "repositoryId" , repositoryId );
		
		clickButtonWithValue( "Submit" );
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
	
	public void assertAddArtifactPage()
	{
		assertPage( "Apache Archiva \\ Upload Artifact" );
		assertTextPresent( "Upload Artifact" );
		
		String artifact = "Upload Artifact,Group Id*:,Artifact Id*:,Version*:,Packaging*:,Classifier:,Generate Maven 2 POM,Artifact File*:,POM File:,Repository Id:";
		String[] arrayArtifact = artifact.split( "," );
		for ( String arrayartifact : arrayArtifact )
			assertTextPresent( arrayartifact );
		
		String artifactElements = "upload_groupId,upload_artifactId,upload_version,upload_packaging,upload_classifier,upload_generatePom,upload_artifact,upload_pom,upload_repositoryId,upload_0";
		String[] arrayArtifactElements = artifactElements.split( "," );
		for ( String artifactelements : arrayArtifactElements )
			assertElementPresent( artifactelements );
	}
	
	public void assertDeleteArtifactPage()
	{
		assertPage( "Apache Archiva \\ Delete Artifact" );
		assertTextPresent( "Delete Artifact" );
		assertTextPresent( "Group Id*:" );
		assertTextPresent( "Artifact Id*:" );
		assertTextPresent( "Version*:" );
		assertTextPresent( "Repository Id:" );
		assertElementPresent( "groupId" );
		assertElementPresent( "artifactId" );
		assertElementPresent( "version" );
		assertElementPresent( "repositoryId" );
		assertButtonWithValuePresent( "Submit" );
	}
}
