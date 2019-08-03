package org.apache.archiva.rest.services;
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

import org.apache.archiva.admin.model.beans.UiConfiguration;
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.rest.api.model.ChecksumSearch;
import org.apache.archiva.rest.api.model.SearchRequest;
import org.apache.archiva.rest.api.services.SearchService;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
public class SearchServiceTest
    extends AbstractArchivaRestTest
{
    private static final String TEST_REPO = "test-repo";

    @Test
    public void quickSearchOnArtifactId()
        throws Exception
    {
        SearchService searchService = getSearchService( authorizationHeader );

        // START SNIPPET: quick-search
        List<Artifact> artifacts = searchService.quickSearch( "commons-logging" );
        // return all artifacts with groupId OR artifactId OR version OR packaging OR className
        // NOTE : only artifacts with classifier empty are returned
        // END SNIPPET: quick-search

        assertNotNull( artifacts );
        assertTrue( " not 6 results for commons-logging search but " + artifacts.size() + ":" + artifacts,
                    artifacts.size() == 6 );
        log.info( "artifacts for commons-logging size {} search {}", artifacts.size(), artifacts );
    }

    /**
     * same search but with Guest user
     *
     * @throws Exception
     */
    @Test
    public void quickSearchOnArtifactIdGuest()
        throws Exception
    {
        SearchService searchService = getSearchService( null );

        // START SNIPPET: quick-search
        List<Artifact> artifacts = searchService.quickSearch( "commons-logging" );
        // return all artifacts with groupId OR artifactId OR version OR packaging OR className
        // NOTE : only artifacts with classifier empty are returned
        // END SNIPPET: quick-search

        assertNotNull( artifacts );
        assertTrue( " not 6 results for commons-logging search but " + artifacts.size() + ":" + artifacts,
                    artifacts.size() == 6 );
        log.info( "artifacts for commons-logging size {} search {}", artifacts.size(), artifacts );
    }

    @Test
    public void searchArtifactVersions()
        throws Exception
    {
        // START SNIPPET: searchservice-artifact-versions
        SearchService searchService = getSearchService( authorizationHeader );

        List<Artifact> artifacts = searchService.getArtifactVersions( "commons-logging", "commons-logging", "jar" );

        // END SNIPPET: searchservice-artifact-versions

        assertNotNull( artifacts );
        assertTrue( " not 13 results for commons-logging search but " + artifacts.size() + ":" + artifacts,
                    artifacts.size() == 13 );
        log.info( "artifacts for commons-logging size {} search {}", artifacts.size(), artifacts );

        for ( Artifact artifact : artifacts )
        {
            log.info( "url: {}", artifact.getUrl() );
            String version = artifact.getVersion();
            assertTrue( artifact.getUrl().contains( version ) );


        }
    }

    @Test
    public void searchWithSearchRequestGroupIdAndArtifactIdAndClassifier()
        throws Exception
    {
        SearchService searchService = getSearchService( authorizationHeader );

        // START SNIPPET: searchservice-with-classifier
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setGroupId( "commons-logging" );
        searchRequest.setArtifactId( "commons-logging" );
        searchRequest.setClassifier( "sources" );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );
        // END SNIPPET: searchservice-with-classifier

        assertNotNull( artifacts );
        assertTrue( " not 2 results for commons-logging search but " + artifacts.size() + ":" + artifacts,
                    artifacts.size() == 2 );
        log.info( "artifacts for commons-logging size {} search {}", artifacts.size(), artifacts );
    }

    @Test
    public void searchWithSearchRequestBundleSymbolicNameOneVersion()
        throws Exception
    {
        SearchService searchService = getSearchService( authorizationHeader );

        // START SNIPPET: searchservice-with-osgi
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBundleSymbolicName( "org.apache.karaf.features.command" );
        // END SNIPPET: searchservice-with-osgi

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertNotNull( artifacts );
        assertTrue(
            " not 1 results for Bundle Symbolic Name org.apache.karaf.features.command but " + artifacts.size() + ":"
                + artifacts, artifacts.size() == 1 );
    }

    @Test
    public void searchWithSearchRequestBundleSymbolicNameTwoVersion()
        throws Exception
    {
        UiConfiguration uiConfiguration = new UiConfiguration();
        uiConfiguration.setApplicationUrl( null );
        getArchivaAdministrationService().setUiConfiguration( uiConfiguration );

        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBundleSymbolicName( "org.apache.karaf.features.core" );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertThat( artifacts ).isNotNull().hasSize( 2 );

        for ( Artifact artifact : artifacts )
        {
            log.info( "url: {}", artifact.getUrl() );
            String version = artifact.getVersion();
            Assertions.assertThat( artifact.getUrl() ) //
                .isEqualTo( "http://localhost:" + getServerPort()
                                + "/repository/test-repo/org/apache/karaf/features/org.apache.karaf.features.core/"
                                + version + "/org.apache.karaf.features.core-" + version + ".jar" );


        }
    }

    @Test
    public void searchWithSearchRequestExportPackageOneVersion()
        throws Exception
    {
        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBundleExportPackage( "org.apache.karaf.features.command.completers" );
        searchRequest.setRepositories( Arrays.asList( TEST_REPO ) );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertNotNull( artifacts );
        assertTrue( " not 1 results for Bundle ExportPackage org.apache.karaf.features.command.completers but "
                        + artifacts.size() + ":" + artifacts, artifacts.size() == 1 );

        log.info( "artifact url {}", artifacts.get( 0 ).getUrl() );
    }

    @Test
    /**
     * ensure we don't return response for an unknown repo
     */ public void searchWithSearchUnknwownRepoId()
        throws Exception
    {
        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBundleExportPackage( "org.apache.karaf.features.command.completers" );
        searchRequest.setRepositories( Arrays.asList( "tototititata" ) );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertNotNull( artifacts );
        assertTrue( " not 0 results for Bundle ExportPackage org.apache.karaf.features.command.completers but " +
                        artifacts.size() + ":" + artifacts, artifacts.size() == 0 );
    }

    @Test
    /**
     * ensure we revert to all observable repos in case of no repo in the request
     */ public void searchWithSearchNoRepos()
        throws Exception
    {
        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBundleExportPackage( "org.apache.karaf.features.command.completers" );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertNotNull( artifacts );
        assertTrue( " not 0 results for Bundle ExportPackage org.apache.karaf.features.command.completers but "
                        + artifacts.size() + ":" + artifacts, artifacts.size() == 1 );

        log.info( "artifact url {}", artifacts.get( 0 ).getUrl() );
    }

    @Test
    public void getAllGroupIds()
        throws Exception
    {
        SearchService searchService = getSearchService( authorizationHeader );

        Collection<String> groupIds = searchService.getAllGroupIds( Arrays.asList( TEST_REPO ) ).getGroupIds();
        log.info( "groupIds  {}", groupIds );
        assertFalse( groupIds.isEmpty() );
        assertTrue( groupIds.contains( "commons-cli" ) );
        assertTrue( groupIds.contains( "org.apache.felix" ) );
    }

    @Test
    /**
     * test we don't return 2 artifacts pom + zip one
     */ public void getSearchArtifactsWithOnlyClassifier()
        throws Exception
    {
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( TEST_REPO, getProjectDirectory().resolve("src/test/repo-with-classifier-only") );

        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest =
            new SearchRequest( "org.foo", "studio-all-update-site", null, null, null, Arrays.asList( TEST_REPO ) );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );
        log.info( "artifacts: {}", artifacts );
        assertEquals( 1, artifacts.size() );
    }

    /**
     * sha1 commons-logging 1.1 ba24d5de831911b684c92cd289ed5ff826271824
     */
    @Test
    public void search_with_sha1()
        throws Exception
    {
        SearchService searchService = getSearchService( authorizationHeader );

        List<Artifact> artifacts = searchService.getArtifactByChecksum(
            new ChecksumSearch( null, "ba24d5de831911b684c92cd289ed5ff826271824" ) );

        Assertions.assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 1 );

    }


    /**
     * md5 commons-logging 1.1 6b62417e77b000a87de66ee3935edbf5
     */
    @Test
    public void search_with_md5()
        throws Exception
    {
        SearchService searchService = getSearchService( authorizationHeader );

        List<Artifact> artifacts = searchService.getArtifactByChecksum(
            new ChecksumSearch( null, "6b62417e77b000a87de66ee3935edbf5" ) );

        Assertions.assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 1 );

    }

    @Before
    public void createRepo()
        throws Exception
    {
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( TEST_REPO, getProjectDirectory( ).resolve( "src/test/repo-with-osgi" ) );

        waitForScanToComplete( TEST_REPO );
    }

    @After
    public void deleteRepo()
        throws Exception
    {
        deleteTestRepo( TEST_REPO );
    }

}


