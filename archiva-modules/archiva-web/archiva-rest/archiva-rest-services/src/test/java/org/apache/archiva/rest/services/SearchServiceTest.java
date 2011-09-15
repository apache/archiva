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

import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.rest.api.model.ManagedRepository;
import org.apache.archiva.rest.api.model.SearchRequest;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.SearchService;
import org.apache.commons.io.FileUtils;
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

        List<Artifact> artifacts = searchService.quickSearch( "commons-logging" );

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

        SearchService searchService = getSearchService( authorizationHeader );

        List<Artifact> artifacts = searchService.getArtifactVersions( "commons-logging", "commons-logging", "jar" );

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

    private File createAndIndexRepo( String testRepoId )
        throws Exception
    {
        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( testRepoId ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( testRepoId, true );
        }
        File targetRepo = new File( System.getProperty( "targetDir", "./target" ), "test-repo" );
        cleanupFiles( targetRepo );

        File sourceRepo = new File( "src/test/repo-with-osgi" );

        FileUtils.copyDirectory( sourceRepo, targetRepo );

        ManagedRepository managedRepository = new ManagedRepository();
        managedRepository.setId( testRepoId );
        managedRepository.setName( "test repo" );

        managedRepository.setLocation( targetRepo.getPath() );
        managedRepository.setIndexDirectory( targetRepo.getPath() + "/index-" + Long.toString( new Date().getTime() ) );

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
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( id, true );
        }
        cleanupFiles( targetRepo );

    }

    private void cleanupFiles( File targetRepo )
        throws Exception
    {

        File indexerDir = new File( targetRepo, ".indexer" );

        if ( targetRepo.exists() )
        {
            FileUtils.deleteDirectory( targetRepo );
        }

        if ( indexerDir.exists() )
        {
            FileUtils.deleteDirectory( indexerDir );
        }

        File lockFile = new File( indexerDir, "write.lock" );
        if ( lockFile.exists() )
        {
            FileUtils.forceDelete( lockFile );
        }

        assertFalse( targetRepo.exists() );
        assertFalse( indexerDir.exists() );
        assertFalse( lockFile.exists() );
    }

}


