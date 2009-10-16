package org.apache.archiva.web.test.parent;

import java.io.File;

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
		String artifactId = getProperty( "GROUPID" ) ;
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
		File f = new File( "" );
		String artifactFilePath = f.getAbsolutePath();
		return artifactFilePath + "/src/test/it-resources/snapshots/org/apache/maven/archiva/web/test/foo-bar/1.0-SNAPSHOT/foo-bar-1.0-SNAPSHOT.jar" ;
	}
	
	public String getRepositoryId()
	{
		String repositoryId = getProperty( "REPOSITORYID" ) ;
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
	
	// Legacy Support
	public void goToLegacySupportPage()
	{
		clickLinkWithText( "Legacy Support" );
		assertLegacySupportPage();
	}
	
	public void assertLegacySupportPage()
	{
		assertPage( "Apache Archiva \\ Administration - Legacy Support" );
		assertTextPresent( "Administration - Legacy Artifact Path Resolution" );
		assertTextPresent( "Path Mappings" );
		assertLinkPresent( "Add" );
	}
	
	public void addLegacyArtifactPath( String path, String groupId, String artifactId, String version, String classifier, String type)
	{
		assertAddLegacyArtifactPathPage();
		setFieldValue( "legacyArtifactPath.path" , path );
		setFieldValue( "groupId" , groupId );
		setFieldValue( "artifactId" , artifactId );
		setFieldValue( "version" , version );
		setFieldValue( "classifier" , classifier );
		setFieldValue( "type" , type );
		clickButtonWithValue( "Add Legacy Artifact Path" );
	}
	
	public void assertAddLegacyArtifactPathPage()
	{
		assertPage( "Apache Archiva \\ Admin: Add Legacy Artifact Path" );
		assertTextPresent( "Admin: Add Legacy Artifact Path" );
		assertTextPresent( "Enter the legacy path to map to a particular artifact reference, then adjust the fields as necessary." );
		String element = "addLegacyArtifactPath_legacyArtifactPath_path,addLegacyArtifactPath_groupId,addLegacyArtifactPath_artifactId,addLegacyArtifactPath_version,addLegacyArtifactPath_classifier,addLegacyArtifactPath_type";
		String[] arrayElement = element.split( "," );
		for ( String arrayelement : arrayElement )
			assertElementPresent( arrayelement );
		assertButtonWithValuePresent( "Add Legacy Artifact Path" );
	}
}
