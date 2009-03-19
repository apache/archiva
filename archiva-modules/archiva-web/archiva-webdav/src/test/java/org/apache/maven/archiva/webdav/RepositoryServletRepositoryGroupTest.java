package org.apache.maven.archiva.webdav;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;


/**
 * RepositoryServletRepositoryGroupTest
 * 
 * Test Case 1.  Accessing a valid repository group root url (e.g. http://machine.com/repository/repository-group/) returns a Bad Request (HTTP 400)
 * Test Case 2.  Accessing an invalid repository group root url is forwarded to managed repository checking (this is not covered here)
 * Test Case 3.  Accessing an artifact in a valid repository group will iterate over the managed repositories in the repository group
 *     Test Case 3.a.  If an invalid managed repository is encountered (managed repository doesn't exist),
 *                     a Not Found (HTTP 404) is returned and the iteration is broken
 *     Test Case 3.b.  If an artifact is not found in a valid managed repository (after proxying, etc.),
 *                     a Not Found (HTTP 404) is set but not returned yet, the iteration continues to the next managed repository.
 *                     The Not Found (HTTP 404) is returned after exhausting all valid managed repositories
 *     Test Case 3.c.  If an artifact is found in a valid managed repository,
 *                     the artifact is returned, the iteration is broken and any Not Found (HTTP 404) is disregarded
 * Test Case 4.  Accessing a valid repository group with any http write method returns a Bad Request (HTTP 400)
 *                     
 *
 */
public class RepositoryServletRepositoryGroupTest
    extends AbstractRepositoryServletTestCase
{
    protected File repoRootFirst;
    
    protected File repoRootLast;
    
    protected File repoRootInvalid;
    
    protected static final String MANAGED_REPO_FIRST = "first";
    
    protected static final String MANAGED_REPO_LAST = "last";
    
    protected static final String MANAGED_REPO_INVALID = "invalid";
    
    protected static final String REPO_GROUP_WITH_VALID_REPOS = "group-with-valid-repos";

    protected static final String REPO_GROUP_WITH_INVALID_REPOS = "group-with-invalid-repos";
    
    
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        String appserverBase = System.getProperty( "appserver.base" );
        
        Configuration configuration = archivaConfiguration.getConfiguration();
        
        repoRootFirst = new File( appserverBase, "data/repositories/" + MANAGED_REPO_FIRST );
        repoRootLast = new File( appserverBase, "data/repositories/" + MANAGED_REPO_LAST );
        
        configuration.addManagedRepository( createManagedRepository( MANAGED_REPO_FIRST, "First Test Repo", repoRootFirst ) );
        configuration.addManagedRepository( createManagedRepository( MANAGED_REPO_LAST, "Last Test Repo", repoRootLast ) );
        
        List<String> managedRepoIds = new ArrayList<String>();
        managedRepoIds.add( MANAGED_REPO_FIRST );
        managedRepoIds.add( MANAGED_REPO_LAST );
        
        configuration.addRepositoryGroup( createRepositoryGroup( REPO_GROUP_WITH_VALID_REPOS, managedRepoIds ) );
        
        // Create the repository group with an invalid managed repository
        repoRootInvalid = new File( appserverBase, "data/repositories/" + MANAGED_REPO_INVALID );
        ManagedRepositoryConfiguration managedRepositoryConfiguration = createManagedRepository( MANAGED_REPO_INVALID, "Invalid Test Repo", repoRootInvalid );
        
        configuration.addManagedRepository( createManagedRepository( MANAGED_REPO_FIRST, "First Test Repo", repoRootFirst ) );
        configuration.addManagedRepository( managedRepositoryConfiguration );
        configuration.addManagedRepository( createManagedRepository( MANAGED_REPO_LAST, "Last Test Repo", repoRootLast ) );
        
        List<String> invalidManagedRepoIds = new ArrayList<String>();
        invalidManagedRepoIds.add( MANAGED_REPO_FIRST );
        invalidManagedRepoIds.add( MANAGED_REPO_INVALID );
        invalidManagedRepoIds.add( MANAGED_REPO_LAST );
        
        configuration.addRepositoryGroup( createRepositoryGroup( REPO_GROUP_WITH_INVALID_REPOS, invalidManagedRepoIds ) );
        
        configuration.removeManagedRepository( managedRepositoryConfiguration );
        FileUtils.deleteDirectory( repoRootInvalid );
        
        saveConfiguration( archivaConfiguration );
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        setupCleanRepo( repoRootFirst );
        setupCleanRepo( repoRootLast );
        
        super.tearDown();
    }
        
    /*
     * Test Case 3.c
     */
    public void testGetFromFirstManagedRepositoryReturnOk()
        throws Exception
    {
        String resourceName = "dummy/dummy-first-resource/1.0/dummy-first-resource-1.0.txt";
        
        File dummyInternalResourceFile = new File( repoRootFirst, resourceName );
        dummyInternalResourceFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( dummyInternalResourceFile, "first", null );
        
        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/" + resourceName );
        WebResponse response = sc.getResponse( request );
        
        assertResponseOK( response );
        assertEquals( "Expected file contents", "first", response.getText() );
    }
    
    /*
     * Test Case 3.c
     */
    public void testGetFromLastManagedRepositoryReturnOk()
        throws Exception
    {        
        String resourceName = "dummy/dummy-last-resource/1.0/dummy-last-resource-1.0.txt";
        
        File dummyReleasesResourceFile = new File( repoRootLast, resourceName );
        dummyReleasesResourceFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( dummyReleasesResourceFile, "last", null );
    
        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/" + resourceName );
        WebResponse response = sc.getResponse( request );
        
        assertResponseOK( response );
        assertEquals( "Expected file contents", "last", response.getText() );
    }
    
    /*
     * Test Case 3.b
     */
    public void testGetFromValidRepositoryGroupReturnNotFound()
        throws Exception
    {
        String resourceName = "dummy/dummy-no-resource/1.0/dummy-no-resource-1.0.txt";
        
        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/" + resourceName );
        WebResponse response = sc.getResponse( request );
        
        assertResponseNotFound( response );
    } 
    
    /*
     * Test Case 3.a
     */
    public void testGetInvalidManagedRepositoryInGroupReturnNotFound()
        throws Exception
    {
        String resourceName = "dummy/dummy-no-resource/1.0/dummy-no-resource-1.0.txt";
        
        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/" + REPO_GROUP_WITH_INVALID_REPOS + "/" + resourceName );
        WebResponse response = sc.getResponse( request );
        
        assertResponseInternalServerError( response );
    }
    
    /*
     * Test Case 4
     */
    public void testPutValidRepositoryGroupReturnBadRequest()
        throws Exception
    {
        String resourceName = "dummy/dummy-put-resource/1.0/dummy-put-resource-1.0.txt";
        String putUrl = "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/" + resourceName;
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        
        WebRequest request = new PutMethodWebRequest( putUrl, is, "text/plain" );
        WebResponse response = sc.getResponse( request );
         
        assertResponseMethodNotAllowed( response );
    }
        
    // MRM-872
    public void testGetMergedMetadata()
        throws Exception
    {   
        // first metadata file        
        String resourceName = "dummy/dummy-merged-metadata-resource/maven-metadata.xml";  
        
        File dummyInternalResourceFile = new File( repoRootFirst, resourceName );
        dummyInternalResourceFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( dummyInternalResourceFile, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        		"<metadata>\n<groupId>dummy</groupId>\n<artifactId>dummy-merged-metadata-resource</artifactId>\n" +
        		"<versioning>\n<latest>1.0</latest>\n<release>1.0</release>\n<versions>\n<version>1.0</version>\n" +
        		"<version>2.5</version>\n</versions>\n<lastUpdated>20080708095554</lastUpdated>\n</versioning>\n</metadata>", null );
        
        //second metadata file
        resourceName = "dummy/dummy-merged-metadata-resource/maven-metadata.xml";        
        dummyInternalResourceFile = new File( repoRootLast, resourceName );
        dummyInternalResourceFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( dummyInternalResourceFile, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<metadata><groupId>dummy</groupId><artifactId>dummy-merged-metadata-resource</artifactId>" +
                "<versioning><latest>2.0</latest><release>2.0</release><versions><version>1.0</version>" +
                "<version>1.5</version><version>2.0</version></versions><lastUpdated>20080709095554</lastUpdated>" +
                "</versioning></metadata>", null );
        
        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/dummy/" +
                "dummy-merged-metadata-resource/maven-metadata.xml" );
        WebResponse response = sc.getResource( request );              
        
        File returnedMetadata = new File( getBasedir(), "/target/test-classes/retrievedMetadataFile.xml");
        FileUtils.writeStringToFile( returnedMetadata, response.getText() );
        ArchivaRepositoryMetadata metadata = RepositoryMetadataReader.read( returnedMetadata );        
        
        assertResponseOK( response );
        assertEquals( "Versions list size", 4, metadata.getAvailableVersions().size() );
        assertTrue( "Versions list contains version 1.0", metadata.getAvailableVersions().contains( "1.0" ) );
        assertTrue( "Versions list contains version 1.5", metadata.getAvailableVersions().contains( "1.5" ) );
        assertTrue( "Versions list contains version 2.0", metadata.getAvailableVersions().contains( "2.0" ) );
        assertTrue( "Versions list contains version 2.5", metadata.getAvailableVersions().contains( "2.5" ) );
        
        //check if the checksum files were generated
        File checksumFileSha1 = new File( repoRootFirst, resourceName + ".sha1" );
        checksumFileSha1.getParentFile().mkdirs();
        FileUtils.writeStringToFile( checksumFileSha1, "3290853214d3687134", null );
        
        File checksumFileMd5 = new File( repoRootFirst, resourceName + ".md5" );
        checksumFileMd5.getParentFile().mkdirs();
        FileUtils.writeStringToFile( checksumFileMd5, "98745897234eda12836423", null );
        
        // request the sha1 checksum of the metadata
        request =
            new GetMethodWebRequest( "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/dummy/" +
                "dummy-merged-metadata-resource/maven-metadata.xml.sha1" );
        response = sc.getResource( request );
        
        assertResponseOK( response );
        assertEquals( "add113b0d7f8c6adb92a5015a7a3701081edf998  maven-metadata-group-with-valid-repos.xml", response.getText() );
        
        // request the md5 checksum of the metadata
        request =
            new GetMethodWebRequest( "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/dummy/" +
                "dummy-merged-metadata-resource/maven-metadata.xml.md5" );
        response = sc.getResource( request );
                
        assertResponseOK( response );
        assertEquals( "5b85ea4aa5f52bb76760041a52f98de8  maven-metadata-group-with-valid-repos.xml", response.getText().trim() );
    }
    
    // MRM-901
    public void testBrowseWithTwoArtifactsWithSameGroupIdInRepos()
        throws Exception
    {
        String resourceName = "dummy/dummy-artifact/1.0/dummy-artifact-1.0.txt";
        
        File dummyInternalResourceFile = new File( repoRootFirst, resourceName );
        dummyInternalResourceFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( dummyInternalResourceFile, "first", null );
        
        resourceName = "dummy/dummy-artifact/2.0/dummy-artifact-2.0.txt";
        
        File dummyReleasesResourceFile = new File( repoRootLast, resourceName );
        dummyReleasesResourceFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( dummyReleasesResourceFile, "last", null );
        
        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/dummy/dummy-artifact/" );
        WebResponse response = sc.getResource( request );
        
        assertResponseOK( response );
        assertTrue( StringUtils.contains( response.getText(), "Collection" ) );
        assertTrue( StringUtils.contains( response.getText(), "dummy/dummy-artifact" ) );
        assertTrue( StringUtils.contains( response.getText(), "1.0" ) );
        assertTrue( StringUtils.contains( response.getText(), "2.0" ) );
    }
        
    protected void assertResponseMethodNotAllowed( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        assertEquals( "Should have been an 405/Method Not Allowed response code.", HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getResponseCode() );
    }

    protected RepositoryGroupConfiguration createRepositoryGroup( String id, List<String> repositories )
    {
        RepositoryGroupConfiguration repoGroupConfiguration = new RepositoryGroupConfiguration();
        repoGroupConfiguration.setId( id );
        repoGroupConfiguration.setRepositories( repositories );
        return repoGroupConfiguration;
    }
}
