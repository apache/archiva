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
import org.junit.Test;

import java.io.File;
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

        File targetRepo = createAndIndexRepo( testRepoId );

        SearchService searchService = getSearchService( authorizationHeader );

        // START SNIPPET: quick-search
        List<Artifact> artifacts = searchService.quickSearch( "commons-logging" );
        // return all artifacts with groupId OR artifactId
        // START SNIPPET: quick-search

        assertNotNull( artifacts );
        assertTrue( " not 6 results for commons-logging search but " + artifacts.size() + ":" + artifacts,
                    artifacts.size() == 6 );
        log.info( "artifacts for commons-logging size {} search {}", artifacts.size(), artifacts );

        deleteTestRepo( testRepoId, targetRepo );
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

        File targetRepo = createAndIndexRepo( testRepoId );

        // START SNIPPET: searchservice-artifact-versions
        SearchService searchService = getSearchService( authorizationHeader );

        List<Artifact> artifacts = searchService.getArtifactVersions( "commons-logging", "commons-logging", "jar" );

        // END SNIPPET: searchservice-artifact-versions

        assertNotNull( artifacts );
        assertTrue( " not 3 results for commons-logging search but " + artifacts.size() + ":" + artifacts,
                    artifacts.size() == 13 );
        log.info( "artifacts for commons-logging size {} search {}", artifacts.size(), artifacts );

        deleteTestRepo( testRepoId, targetRepo );
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

        File targetRepo = createAndIndexRepo( testRepoId );

        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setGroupId( "commons-logging" );
        searchRequest.setArtifactId( "commons-logging" );
        searchRequest.setClassifier( "sources" );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertNotNull( artifacts );
        assertTrue( " not 2 results for commons-logging search but " + artifacts.size() + ":" + artifacts,
                    artifacts.size() == 2 );
        log.info( "artifacts for commons-logging size {} search {}", artifacts.size(), artifacts );

        deleteTestRepo( testRepoId, targetRepo );
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

        File targetRepo = createAndIndexRepo( testRepoId );

        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBundleSymbolicName( "org.apache.karaf.features.command" );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertNotNull( artifacts );
        assertTrue(
            " not 1 results for Bundle Symbolic Name org.apache.karaf.features.command but " + artifacts.size() + ":"
                + artifacts, artifacts.size() == 1 );

        deleteTestRepo( testRepoId, targetRepo );
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

        File targetRepo = createAndIndexRepo( testRepoId );

        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBundleSymbolicName( "org.apache.karaf.features.core" );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertNotNull( artifacts );
        assertTrue(
            " not 2 results for Bundle Symbolic Name org.apache.karaf.features.core but " + artifacts.size() + ":"
                + artifacts, artifacts.size() == 2 );

        deleteTestRepo( testRepoId, targetRepo );
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

        File targetRepo = createAndIndexRepo( testRepoId );

        SearchService searchService = getSearchService( authorizationHeader );

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setBundleExportPackage( "org.apache.karaf.features.command.completers" );

        List<Artifact> artifacts = searchService.searchArtifacts( searchRequest );

        assertNotNull( artifacts );
        assertTrue( " not 1 results for Bundle ExportPackage org.apache.karaf.features.command.completers but "
                        + artifacts.size() + ":" + artifacts, artifacts.size() == 1 );

        deleteTestRepo( testRepoId, targetRepo );
    }

    private File createAndIndexRepo( String testRepoId )
        throws Exception
    {
        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( testRepoId ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( testRepoId, true );
        }
        File targetRepo = new File( "src/test/repo-with-osgi" );

        ManagedRepository managedRepository = new ManagedRepository();
        managedRepository.setId( testRepoId );
        managedRepository.setName( "test repo" );

        managedRepository.setLocation( targetRepo.getPath() );
        managedRepository.setIndexDirectory( "target/.index-" + Long.toString( new Date().getTime() ) );

        ManagedRepositoriesService service = getManagedRepositoriesService( authorizationHeader );
        service.addManagedRepository( managedRepository );

        getRepositoriesService( authorizationHeader ).scanRepositoryNow( testRepoId, true );

        return targetRepo;
    }

    private void deleteTestRepo( String id, File targetRepo )
        throws Exception
    {
        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( id ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( id, false );
        }

    }


}


