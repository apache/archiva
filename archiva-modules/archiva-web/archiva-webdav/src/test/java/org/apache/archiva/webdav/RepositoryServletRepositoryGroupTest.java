package org.apache.archiva.webdav;

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


import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.metadata.maven.MavenMetadataReader;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * RepositoryServletRepositoryGroupTest
 * <p/>
 * Test Case 1.  Accessing a valid repository group root url (e.g. http://machine.com/repository/repository-group/) returns a Bad Request (HTTP 400)
 * Test Case 2.  Accessing an invalid repository group root url is forwarded to managed repository checking (this is not covered here)
 * Test Case 3.  Accessing an artifact in a valid repository group will iterate over the managed repositories in the repository group
 * Test Case 3.a.  If an invalid managed repository is encountered (managed repository doesn't exist),
 * a Not Found (HTTP 404) is returned and the iteration is broken
 * Test Case 3.b.  If an artifact is not found in a valid managed repository (after proxying, etc.),
 * a Not Found (HTTP 404) is set but not returned yet, the iteration continues to the next managed repository.
 * The Not Found (HTTP 404) is returned after exhausting all valid managed repositories
 * Test Case 3.c.  If an artifact is found in a valid managed repository,
 * the artifact is returned, the iteration is broken and any Not Found (HTTP 404) is disregarded
 * Test Case 4.  Accessing a valid repository group with any http write method returns a Bad Request (HTTP 400)
 */
public class RepositoryServletRepositoryGroupTest
    extends AbstractRepositoryServletTestCase
{
    protected Path repoRootFirst;

    protected Path repoRootLast;

    protected Path repoRootInvalid;

    protected static final String MANAGED_REPO_FIRST = "first";

    protected static final String MANAGED_REPO_LAST = "last";

    protected static final String MANAGED_REPO_INVALID = "invalid";

    protected static final String REPO_GROUP_WITH_VALID_REPOS = "group-with-valid-repos";

    protected static final String REPO_GROUP_WITH_INVALID_REPOS = "group-with-invalid-repos";


    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        String appserverBase = getAppserverBase().toAbsolutePath().toString();
        log.debug( "Appserver Base {}", appserverBase );

        Configuration configuration = archivaConfiguration.getConfiguration();

        repoRootFirst = Paths.get( appserverBase, "data/repositories/" + MANAGED_REPO_FIRST );
        repoRootLast = Paths.get( appserverBase, "data/repositories/" + MANAGED_REPO_LAST );

        configuration.addManagedRepository(
            createManagedRepository( MANAGED_REPO_FIRST, "First Test Repo", repoRootFirst, true ) );
        configuration.addManagedRepository(
            createManagedRepository( MANAGED_REPO_LAST, "Last Test Repo", repoRootLast, true ) );

        List<String> managedRepoIds = new ArrayList<>();
        managedRepoIds.add( MANAGED_REPO_FIRST );
        managedRepoIds.add( MANAGED_REPO_LAST );

        configuration.addRepositoryGroup( createRepositoryGroup( REPO_GROUP_WITH_VALID_REPOS, managedRepoIds ) );

        // Create the repository group with an invalid managed repository
        repoRootInvalid = Paths.get( appserverBase, "data/repositories/" + MANAGED_REPO_INVALID );
        ManagedRepositoryConfiguration managedRepositoryConfiguration =
            createManagedRepository( MANAGED_REPO_INVALID, "Invalid Test Repo", repoRootInvalid, true );

        configuration.addManagedRepository( managedRepositoryConfiguration );

        List<String> invalidManagedRepoIds = new ArrayList<>();
        invalidManagedRepoIds.add( MANAGED_REPO_FIRST );
        invalidManagedRepoIds.add( MANAGED_REPO_INVALID );
        invalidManagedRepoIds.add( MANAGED_REPO_LAST );

        configuration.addRepositoryGroup(
            createRepositoryGroup( REPO_GROUP_WITH_INVALID_REPOS, invalidManagedRepoIds ) );

        configuration.removeManagedRepository( managedRepositoryConfiguration );
        org.apache.archiva.common.utils.FileUtils.deleteDirectory( repoRootInvalid );

        repositoryRegistry.reload();
        saveConfiguration( archivaConfiguration );

        startRepository();
    }


    @Override
    @After
    public void tearDown()
        throws Exception
    {
        setupCleanRepo( repoRootFirst );
        setupCleanRepo( repoRootLast );

        repositoryRegistry.destroy();

        super.tearDown();

        String appserverBase = System.getProperty( "appserver.base" );
        if ( StringUtils.isNotEmpty( appserverBase ) )
        {
            FileUtils.deleteDirectory( Paths.get( appserverBase ) );
        }
    }

    /*
    * Test Case 3.c
    */
    @Test
    public void testGetFromFirstManagedRepositoryReturnOk()
        throws Exception
    {
        String resourceName = "dummy/dummy-first-resource/1.0/dummy-first-resource-1.0.txt";

        Path dummyInternalResourceFile = repoRootFirst.resolve( resourceName );
        Files.createDirectories( dummyInternalResourceFile.getParent() );
        org.apache.archiva.common.utils.FileUtils.writeStringToFile( dummyInternalResourceFile, Charset.defaultCharset(), "first" );

        WebRequest request = new GetMethodWebRequest(
            "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/" + resourceName );
        WebResponse response = getServletUnitClient().getResponse( request );

        assertResponseOK( response );
        assertThat( response.getContentAsString() ).isEqualTo( "first" );
    }

    /*
    * Test Case 3.c
    */
    @Test
    public void testGetFromLastManagedRepositoryReturnOk()
        throws Exception
    {
        String resourceName = "dummy/dummy-last-resource/1.0/dummy-last-resource-1.0.txt";

        Path dummyReleasesResourceFile = repoRootLast.resolve( resourceName );
        Files.createDirectories(dummyReleasesResourceFile.getParent());
        org.apache.archiva.common.utils.FileUtils.writeStringToFile(dummyReleasesResourceFile, Charset.defaultCharset(), "last");

        WebRequest request = new GetMethodWebRequest(
            "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/" + resourceName );
        WebResponse response = getServletUnitClient().getResponse( request );

        assertResponseOK( response );

        assertThat( response.getContentAsString() ).isEqualTo( "last" );
    }

    /*
    * Test Case 3.b
    */
    @Test
    public void testGetFromValidRepositoryGroupReturnNotFound()
        throws Exception
    {
        String resourceName = "dummy/dummy-no-resource/1.0/dummy-no-resource-1.0.txt";

        WebRequest request = new GetMethodWebRequest(
            "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/" + resourceName );
        WebResponse response = getServletUnitClient().getResponse( request );

        assertResponseNotFound( response );
    }

    /*
    * Test Case 4
    */
    @Test
    public void testPutValidRepositoryGroupReturnBadRequest()
        throws Exception
    {
        String resourceName = "dummy/dummy-put-resource/1.0/dummy-put-resource-1.0.txt";
        String putUrl = "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/" + resourceName;
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );

        WebRequest request = new PutMethodWebRequest( putUrl, is, "text/plain" );
        WebResponse response = getServletUnitClient().getResponse( request );

        assertResponseMethodNotAllowed( response );
    }

    // MRM-872
    @Test
    public void testGetMergedMetadata()
        throws Exception
    {
        // first metadata file        
        String resourceName = "dummy/dummy-merged-metadata-resource/maven-metadata.xml";

        Path dummyInternalResourceFile =  repoRootFirst.resolve( resourceName );
        Files.createDirectories(dummyInternalResourceFile.getParent());
        org.apache.archiva.common.utils.FileUtils.writeStringToFile( dummyInternalResourceFile,
            Charset.defaultCharset(),  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<metadata>\n<groupId>dummy</groupId>\n<artifactId>dummy-merged-metadata-resource</artifactId>\n"
            + "<versioning>\n<latest>1.0</latest>\n<release>1.0</release>\n<versions>\n<version>1.0</version>\n"
            + "<version>2.5</version>\n</versions>\n<lastUpdated>20080708095554</lastUpdated>\n</versioning>\n</metadata>" );

        //second metadata file
        resourceName = "dummy/dummy-merged-metadata-resource/maven-metadata.xml";
        dummyInternalResourceFile = repoRootLast.resolve( resourceName );
        Files.createDirectories(dummyInternalResourceFile.getParent());
        org.apache.archiva.common.utils.FileUtils.writeStringToFile( dummyInternalResourceFile, Charset.defaultCharset(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<metadata><groupId>dummy</groupId><artifactId>dummy-merged-metadata-resource</artifactId>"
            + "<versioning><latest>2.0</latest><release>2.0</release><versions><version>1.0</version>"
            + "<version>1.5</version><version>2.0</version></versions><lastUpdated>20080709095554</lastUpdated>"
            + "</versioning></metadata>" );

        WebRequest request = new GetMethodWebRequest(
            "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/dummy/"
                + "dummy-merged-metadata-resource/maven-metadata.xml" );
        WebResponse response = getServletUnitClient().getResource( request );

        Path returnedMetadata = getProjectBase().resolve( "target/test-classes/retrievedMetadataFile.xml" );
        org.apache.archiva.common.utils.FileUtils.writeStringToFile( returnedMetadata, Charset.defaultCharset(), response.getContentAsString() );
        MavenMetadataReader metadataReader = new MavenMetadataReader( );
        ArchivaRepositoryMetadata metadata = metadataReader.read( returnedMetadata );

        assertResponseOK( response );

        assertThat( metadata.getAvailableVersions() ).isNotNull()
            .hasSize( 4 ).contains( "1.0", "1.5", "2.0", "2.5" );


        //check if the checksum files were generated
        Path checksumFileSha1 = repoRootFirst.resolve( resourceName + ".sha1" );
        Files.createDirectories(checksumFileSha1.getParent());
        org.apache.archiva.common.utils.FileUtils.writeStringToFile(checksumFileSha1, Charset.defaultCharset(), "3290853214d3687134");

        Path checksumFileMd5 = repoRootFirst.resolve( resourceName + ".md5" );
        Files.createDirectories(checksumFileMd5.getParent());
        org.apache.archiva.common.utils.FileUtils.writeStringToFile(checksumFileMd5, Charset.defaultCharset(), "98745897234eda12836423");

        // request the sha1 checksum of the metadata
        request = new GetMethodWebRequest( "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/dummy/"
                                               + "dummy-merged-metadata-resource/maven-metadata.xml.sha1" );
        response = getServletUnitClient().getResource( request );

        assertResponseOK( response );

        assertThat( response.getContentAsString() )
            .startsWith( "f8a7a858a46887368adf0b30874de1f807d91453" );

        // request the md5 checksum of the metadata
        request = new GetMethodWebRequest( "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/dummy/"
                                               + "dummy-merged-metadata-resource/maven-metadata.xml.md5" );
        response = getServletUnitClient().getResource( request );

        assertResponseOK( response );

        assertThat( response.getContentAsString() )
            .startsWith( "cec864b66849153dd45fddb7cdde12b2" );
    }

    // MRM-901
    @Test
    public void testBrowseWithTwoArtifactsWithSameGroupIdInRepos()
        throws Exception
    {
        String resourceName = "dummy/dummy-artifact/1.0/dummy-artifact-1.0.txt";

        Path dummyInternalResourceFile = repoRootFirst.resolve( resourceName );
        Files.createDirectories(dummyInternalResourceFile.getParent());
        org.apache.archiva.common.utils.FileUtils.writeStringToFile(dummyInternalResourceFile, Charset.defaultCharset(), "first");

        resourceName = "dummy/dummy-artifact/2.0/dummy-artifact-2.0.txt";

        Path dummyReleasesResourceFile = repoRootLast.resolve( resourceName );
        Files.createDirectories(dummyReleasesResourceFile.getParent());
        org.apache.archiva.common.utils.FileUtils.writeStringToFile(dummyReleasesResourceFile, Charset.defaultCharset(), "last");

        WebRequest request = new GetMethodWebRequest(
            "http://machine.com/repository/" + REPO_GROUP_WITH_VALID_REPOS + "/dummy/dummy-artifact/" );
        WebResponse response = getServletUnitClient().getResource( request );

        assertResponseOK( response );

        assertThat( response.getContentAsString() ).contains( "Collection" )
            .contains( "dummy/dummy-artifact" )
            .contains( "1.0" )
            .contains( "2.0" );

    }

    protected void assertResponseMethodNotAllowed( WebResponse response )
    {

        assertThat( response ).isNotNull();

        assertThat( response.getStatusCode() ).isEqualTo( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
    }

    protected RepositoryGroupConfiguration createRepositoryGroup( String id, List<String> repositories )
    {
        RepositoryGroupConfiguration repoGroupConfiguration = new RepositoryGroupConfiguration();
        repoGroupConfiguration.setId( id );
        repoGroupConfiguration.setRepositories( repositories );
        return repoGroupConfiguration;
    }
}
