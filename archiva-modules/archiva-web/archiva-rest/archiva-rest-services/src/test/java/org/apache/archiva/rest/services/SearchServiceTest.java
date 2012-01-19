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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.rest.api.model.SearchRequest;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.SearchService;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class SearchServiceTest
    extends AbstractArchivaRestTest
{


    @Test
    public void quickSearchOnArtifactId()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, "src/test/repo-with-osgi" );

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

        deleteTestRepo( testRepoId );
    }

    @Test
    public void searchArtifactVersions()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, "src/test/repo-with-osgi" );

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
            log.info( "url:" + artifact.getUrl() );
            String version = artifact.getVersion();
            assertTrue( artifact.getUrl().contains( version ) );


        }

        deleteTestRepo( testRepoId );
    }

    @Test
    public void searchWithSearchRequestGroupIdAndArtifactIdAndClassifier()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, "src/test/repo-with-osgi" );

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

        deleteTestRepo( testRepoId );
    }

    @Test
    public void searchWithSearchRequestBundleSymbolicNameOneVersion()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, "src/test/repo-with-osgi" );

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

        deleteTestRepo( testRepoId );
    }

    @Test
    public void searchWithSearchRequestBundleSymbolicNameTwoVersion()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, "src/test/repo-with-osgi" );

        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBundleSymbolicName( "org.apache.karaf.features.core" );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertNotNull( artifacts );
        assertTrue(
            " not 2 results for Bundle Symbolic Name org.apache.karaf.features.core but " + artifacts.size() + ":"
                + artifacts, artifacts.size() == 2 );

        for ( Artifact artifact : artifacts )
        {
            log.info( "url:" + artifact.getUrl() );
            String version = artifact.getVersion();
            assertEquals( "http://localhost:" + port
                              + "/repository/test-repo/org/apache/karaf/features/org.apache.karaf.features.core/"
                              + version + "/org.apache.karaf.features.core-" + version + ".bundle", artifact.getUrl() );


        }

        deleteTestRepo( testRepoId );
    }

    @Test
    public void searchWithSearchRequestExportPackageOneVersion()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, "src/test/repo-with-osgi" );

        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBundleExportPackage( "org.apache.karaf.features.command.completers" );
        searchRequest.setRepositories( Arrays.asList( testRepoId ) );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertNotNull( artifacts );
        assertTrue( " not 1 results for Bundle ExportPackage org.apache.karaf.features.command.completers but "
                        + artifacts.size() + ":" + artifacts, artifacts.size() == 1 );

        log.info( "artifact url " + artifacts.get( 0 ).getUrl() );
        deleteTestRepo( testRepoId );
    }

    @Test
    /**
     * ensure we don't return response for an unknown repo
     */
    public void searchWithSearchUnknwownRepoId()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, "src/test/repo-with-osgi" );

        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBundleExportPackage( "org.apache.karaf.features.command.completers" );
        searchRequest.setRepositories( Arrays.asList( "tototititata" ) );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertNotNull( artifacts );
        assertTrue( " not 0 results for Bundle ExportPackage org.apache.karaf.features.command.completers but "
                        + artifacts.size() + ":" + artifacts, artifacts.size() == 0 );

        deleteTestRepo( testRepoId );
    }

    @Test
    /**
     * ensure we revert to all observable repos in case of no repo in the request
     */
    public void searchWithSearchNoRepos()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, "src/test/repo-with-osgi" );

        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBundleExportPackage( "org.apache.karaf.features.command.completers" );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertNotNull( artifacts );
        assertTrue( " not 0 results for Bundle ExportPackage org.apache.karaf.features.command.completers but "
                        + artifacts.size() + ":" + artifacts, artifacts.size() == 1 );

        log.info( "artifact url " + artifacts.get( 0 ).getUrl() );
        deleteTestRepo( testRepoId );
    }

    @Test
    public void getAllGroupIds()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, "src/test/repo-with-osgi" );

        SearchService searchService = getSearchService( authorizationHeader );

        Collection<String> groupIds = searchService.getAllGroupIds( Arrays.asList( testRepoId ) ).getGroupIds();
        log.info( "groupIds  " + groupIds );
        assertFalse( groupIds.isEmpty() );
        assertTrue( groupIds.contains( "commons-cli" ) );
        assertTrue( groupIds.contains( "org.apache.felix" ) );
        deleteTestRepo( testRepoId );
    }

    @Test
    /**
     * test we don't return 2 artifacts pom + zip one
     */
    public void getSearchArtifactsWithOnlyClassifier()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, "src/test/repo-with-classifier-only" );

        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest =
            new SearchRequest( "org.foo", "studio-all-update-site", null, null, null, Arrays.asList( "test-repo" ) );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );
        log.info( "artifacts:" + artifacts );
        assertEquals( 1, artifacts.size() );
        deleteTestRepo( testRepoId );
    }

    private void createAndIndexRepo( String testRepoId, String repoPath )
        throws Exception
    {
        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( testRepoId ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( testRepoId, false );
        }

        ManagedRepository managedRepository = new ManagedRepository();
        managedRepository.setId( testRepoId );
        managedRepository.setName( "test repo" );

        managedRepository.setLocation( new File( repoPath ).getPath() );
        managedRepository.setIndexDirectory( "target/.index-" + Long.toString( new Date().getTime() ) );

        ManagedRepositoriesService service = getManagedRepositoriesService( authorizationHeader );
        service.addManagedRepository( managedRepository );

        getRoleManagementService( authorizationHeader ).assignTemplatedRole(
            ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, testRepoId, "admin" );

        getRepositoriesService( authorizationHeader ).scanRepositoryNow( testRepoId, true );

    }

    private void deleteTestRepo( String id )
        throws Exception
    {
        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( id ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( id, false );
        }

    }


}


