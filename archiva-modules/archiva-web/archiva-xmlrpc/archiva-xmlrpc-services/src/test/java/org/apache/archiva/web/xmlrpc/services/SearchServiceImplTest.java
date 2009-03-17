package org.apache.archiva.web.xmlrpc.services;

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
 

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.web.xmlrpc.api.SearchService;
import org.apache.archiva.web.xmlrpc.api.beans.Artifact;
import org.apache.archiva.web.xmlrpc.api.beans.Dependency;
import org.apache.archiva.web.xmlrpc.security.XmlRpcUserRepositories;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.browsing.BrowsingResults;
import org.apache.maven.archiva.database.browsing.RepositoryBrowsing;
import org.apache.maven.archiva.database.constraints.ArtifactsByChecksumConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

/**
 * SearchServiceImplTest
 * 
 * @version $Id: SearchServiceImplTest.java
 */
public class SearchServiceImplTest
    extends PlexusInSpringTestCase
{
    private SearchService searchService;
    
    private MockControl userReposControl;
    
    private XmlRpcUserRepositories userRepos;
    
    private MockControl searchControl;
    
    private RepositorySearch search;
        
    private MockControl archivaDAOControl;
    
    private ArchivaDAO archivaDAO;
    
    private MockControl artifactDAOControl;
    
    private ArtifactDAO artifactDAO;
    
    private MockControl repoBrowsingControl;
    
    private RepositoryBrowsing repoBrowsing;
        
    @Override
    public void setUp()
        throws Exception
    {
        userReposControl = MockClassControl.createControl( XmlRpcUserRepositories.class );
        userRepos = ( XmlRpcUserRepositories ) userReposControl.getMock();
                
        archivaDAOControl = MockControl.createControl( ArchivaDAO.class );
        archivaDAO = ( ArchivaDAO ) archivaDAOControl.getMock();
        
        repoBrowsingControl = MockControl.createControl( RepositoryBrowsing.class );
        repoBrowsing = ( RepositoryBrowsing ) repoBrowsingControl.getMock();
        
        searchControl = MockControl.createControl( RepositorySearch.class );
        search = ( RepositorySearch ) searchControl.getMock();
        
        searchService = new SearchServiceImpl( userRepos, archivaDAO, repoBrowsing, search );
        
        artifactDAOControl = MockControl.createControl( ArtifactDAO.class );
        artifactDAO = ( ArtifactDAO ) artifactDAOControl.getMock();
    }
    
    /*
     * quick/general text search which returns a list of artifacts
     * query for an artifact based on a checksum
     * query for all available versions of an artifact, sorted in version significance order
     * query for all available versions of an artifact since a given date
     * query for an artifact's direct dependencies
     * query for an artifact's dependency tree (as with mvn dependency:tree - no duplicates should be included)
     * query for all artifacts that depend on a given artifact
     */
 
 /* quick search */
    
//    public void testQuickSearchArtifactBytecodeSearch()
//        throws Exception
//    {
//        // 1. check whether bytecode search or ordinary search
//        // 2. get observable repos
//        // 3. convert results to a list of Artifact objects
//
//        List<String> observableRepoIds = new ArrayList<String>();
//        observableRepoIds.add( "repo1.mirror" );
//        observableRepoIds.add( "public.releases" );
//
//        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );
//
//        Date whenGathered = new Date();
//        SearchResults results = new SearchResults();
//        ArchivaArtifact artifact = new ArchivaArtifact( "org.apache.archiva", "archiva-test", "1.0", "", "jar", "public.releases" );
//        artifact.getModel().setWhenGathered( whenGathered );
//
//        SearchResultHit resultHit = new SearchResultHit();
//        resultHit.setArtifact(artifact);
//        resultHit.setRepositoryId("repo1.mirror");
//
//        results.addHit(SearchUtil.getHitId(artifact.getGroupId(), artifact.getArtifactId()), resultHit);
//
//        SearchResultLimits limits = new SearchResultLimits( SearchResultLimits.ALL_PAGES );
//
//        searchControl.expectAndDefaultReturn( search.search( "", observableRepoIds, "MyClassName", limits, null ), results );
//
//        archivaDAOControl.expectAndReturn( archivaDAO.getArtifactDAO(), artifactDAO );
//        artifactDAOControl.expectAndReturn( artifactDAO.getArtifact( "org.apache.archiva", "archiva-test", "1.0", "", "pom", "public.releases" ), artifact );
//
//        userReposControl.replay();
//        searchControl.replay();
//        archivaDAOControl.replay();
//        artifactDAOControl.replay();
//
//        List<Artifact> artifacts = searchService.quickSearch( "bytecode:MyClassName" );
//
//        userReposControl.verify();
//        searchControl.verify();
//        archivaDAOControl.verify();
//        artifactDAOControl.verify();
//
//        assertNotNull( artifacts );
//        assertEquals( 1, artifacts.size() );
//    }
//
//    public void testQuickSearchArtifactRegularSearch()
//        throws Exception
//    {
//        List<String> observableRepoIds = new ArrayList<String>();
//        observableRepoIds.add( "repo1.mirror" );
//        observableRepoIds.add( "public.releases" );
//
//        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );
//
//        Date whenGathered = new Date();
//        SearchResults results = new SearchResults();
//        ArchivaArtifact artifact = new ArchivaArtifact( "org.apache.archiva", "archiva-test", "1.0", "", "jar", "public.releases" );
//        artifact.getModel().setWhenGathered( whenGathered );
//
//
//        SearchResultHit resultHit = new SearchResultHit();
//        resultHit.setArtifact(artifact);
//        resultHit.setRepositoryId("repo1.mirror");
//        results.addHit(SearchUtil.getHitId(artifact.getGroupId(), artifact.getArtifactId()), resultHit);
//
//        SearchResultLimits limits = new SearchResultLimits( SearchResultLimits.ALL_PAGES );
//
//        searchControl.expectAndDefaultReturn( search.search( "", observableRepoIds, "archiva", limits, null ), results );
//
//        archivaDAOControl.expectAndReturn( archivaDAO.getArtifactDAO(), artifactDAO );
//        artifactDAOControl.expectAndReturn( artifactDAO.getArtifact( "org.apache.archiva", "archiva-test", "1.0", "", "pom", "public.releases" ), artifact );
//
//        userReposControl.replay();
//        searchControl.replay();
//        archivaDAOControl.replay();
//        artifactDAOControl.replay();
//
//        List<Artifact> artifacts = searchService.quickSearch( "archiva" );
//
//        userReposControl.verify();
//        searchControl.verify();
//        archivaDAOControl.verify();
//        artifactDAOControl.verify();
//
//        assertNotNull( artifacts );
//        assertEquals( 1, artifacts.size() );
//    }
    
/* query artifact by checksum */
    
    public void testGetArtifactByChecksum()
        throws Exception
    {
        Date whenGathered = new Date();
        
        ArtifactsByChecksumConstraint constraint = new ArtifactsByChecksumConstraint( "a1b2c3aksjhdasfkdasasd" );
        List<ArchivaArtifact> artifacts = new ArrayList<ArchivaArtifact>();
        ArchivaArtifact artifact = new ArchivaArtifact( "org.apache.archiva", "archiva-test", "1.0", "", "jar", "test-repo"  );
        artifact.getModel().setWhenGathered( whenGathered );
        artifacts.add( artifact );
        
        archivaDAOControl.expectAndReturn( archivaDAO.getArtifactDAO(), artifactDAO );
        artifactDAO.queryArtifacts( constraint );
        artifactDAOControl.setMatcher( MockControl.ALWAYS_MATCHER );
        artifactDAOControl.setReturnValue( artifacts );
        
        archivaDAOControl.replay();
        artifactDAOControl.replay();
        
        List<Artifact> results = searchService.getArtifactByChecksum( "a1b2c3aksjhdasfkdasasd" );
        
        archivaDAOControl.verify();
        artifactDAOControl.verify();
        
        assertNotNull( results );
        assertEquals( 1, results.size() );
    }
    
/* query artifact versions */
    
    public void testGetArtifactVersionsArtifactExists()
        throws Exception
    {
        Date whenGathered = new Date();
        
        List<String> observableRepoIds = new ArrayList<String>();
        observableRepoIds.add( "repo1.mirror" );
        observableRepoIds.add( "public.releases" );
        
        List<String> versions = new ArrayList<String>();
        versions.add( "1.0" );
        versions.add( "1.1-beta-1" );
        versions.add( "1.1-beta-2" );
        versions.add( "1.1" );
        versions.add( "1.2" );
        versions.add( "1.2.1-SNAPSHOT" );
        
        BrowsingResults results = new BrowsingResults( "org.apache.archiva", "archiva-test" );
        results.setSelectedRepositoryIds( observableRepoIds );
        results.setVersions( versions );
        
        List<ArchivaArtifact> archivaArtifacts = new ArrayList<ArchivaArtifact>();
        ArchivaArtifact archivaArtifact = new ArchivaArtifact( "org.apache.archiva", "archiva-test", versions.get( 0 ), "", "pom", "repo1.mirror" );
        archivaArtifact.getModel().setWhenGathered( whenGathered );
        archivaArtifacts.add( archivaArtifact );
        
        archivaArtifact = new ArchivaArtifact( "org.apache.archiva", "archiva-test", versions.get( 1 ), "", "pom", "public.releases" );
        archivaArtifact.getModel().setWhenGathered( whenGathered );
        archivaArtifacts.add( archivaArtifact );
        
        archivaArtifact = new ArchivaArtifact( "org.apache.archiva", "archiva-test", versions.get( 2 ), "", "pom", "repo1.mirror" );
        archivaArtifact.getModel().setWhenGathered( whenGathered );
        archivaArtifacts.add( archivaArtifact );
        
        archivaArtifact = new ArchivaArtifact( "org.apache.archiva", "archiva-test", versions.get( 3 ), "", "pom", "public.releases" );
        archivaArtifact.getModel().setWhenGathered( whenGathered );
        archivaArtifacts.add( archivaArtifact );
        
        archivaArtifact = new ArchivaArtifact( "org.apache.archiva", "archiva-test", versions.get( 4 ), "", "pom", "repo1.mirror" );
        archivaArtifact.getModel().setWhenGathered( whenGathered );
        archivaArtifacts.add( archivaArtifact );
        
        archivaArtifact = new ArchivaArtifact( "org.apache.archiva", "archiva-test", versions.get( 5 ), "", "pom", "public.releases" );
        archivaArtifact.getModel().setWhenGathered( whenGathered );
        archivaArtifacts.add( archivaArtifact );
        
        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );
        repoBrowsingControl.expectAndReturn( repoBrowsing.selectArtifactId( "", observableRepoIds, "org.apache.archiva", "archiva-test" ), results );
        archivaDAOControl.expectAndReturn( archivaDAO.getArtifactDAO(), artifactDAO );
        
        artifactDAOControl.expectAndDefaultReturn( artifactDAO.getArtifact( "org.apache.archiva", "archiva-test", versions.get( 0 ), "", "pom", "repo1.mirror" ),  archivaArtifacts.get( 0 ) );
        artifactDAOControl.expectAndDefaultReturn( artifactDAO.getArtifact( "org.apache.archiva", "archiva-test", versions.get( 1 ), "", "pom", "public.releases" ),  archivaArtifacts.get( 1 ) );
        artifactDAOControl.expectAndDefaultReturn( artifactDAO.getArtifact( "org.apache.archiva", "archiva-test", versions.get( 2 ), "", "pom", "repo1.mirror" ),  archivaArtifacts.get( 2 ) );
        artifactDAOControl.expectAndDefaultReturn( artifactDAO.getArtifact( "org.apache.archiva", "archiva-test", versions.get( 3 ), "", "pom", "public.releases" ),  archivaArtifacts.get( 3 ) );
        artifactDAOControl.expectAndDefaultReturn( artifactDAO.getArtifact( "org.apache.archiva", "archiva-test", versions.get( 4 ), "", "pom", "repo1.mirror" ),  archivaArtifacts.get( 4 ) );
        artifactDAOControl.expectAndDefaultReturn( artifactDAO.getArtifact( "org.apache.archiva", "archiva-test", versions.get( 5 ), "", "pom", "public.releases" ),  archivaArtifacts.get( 5 ) );
        
        userReposControl.replay();
        repoBrowsingControl.replay();
        archivaDAOControl.replay();
        artifactDAOControl.replay();
        
        List<Artifact> artifacts = searchService.getArtifactVersions( "org.apache.archiva", "archiva-test" );
        
        userReposControl.verify();
        repoBrowsingControl.verify();
        archivaDAOControl.verify();
        artifactDAOControl.verify();
        
        assertNotNull( artifacts );
        assertEquals( 6, artifacts.size() );
    }
    
/* query artifact versions since a given date */
    
    public void testGetArtifactVersionsByDateArtifactExists()
        throws Exception
    {
    
    }
    
    public void testGetArtifactVersionsByDateArtifactDoesNotExist()
        throws Exception
    {
    
    }
    
/* query artifact dependencies */
    
    public void testGetDependenciesArtifactExists()
        throws Exception
    {   
        List<String> observableRepoIds = new ArrayList<String>();
        observableRepoIds.add( "repo1.mirror" );
        observableRepoIds.add( "public.releases" );
        
        ArchivaProjectModel model = new ArchivaProjectModel();
        model.setGroupId( "org.apache.archiva" );
        model.setArtifactId( "archiva-test" );
        model.setVersion( "1.0" );
        
        org.apache.maven.archiva.model.Dependency dependency = new org.apache.maven.archiva.model.Dependency();
        dependency.setGroupId( "commons-logging" );
        dependency.setArtifactId( "commons-logging" );
        dependency.setVersion( "2.0" );
        
        model.addDependency( dependency );
        
        dependency = new org.apache.maven.archiva.model.Dependency();
        dependency.setGroupId( "junit" );
        dependency.setArtifactId( "junit" );
        dependency.setVersion( "2.4" );
        dependency.setScope( "test" );
        
        model.addDependency( dependency );
        
        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );        
        repoBrowsingControl.expectAndReturn( 
                 repoBrowsing.selectVersion( "", observableRepoIds, "org.apache.archiva", "archiva-test", "1.0" ), model );
        
        repoBrowsingControl.replay(); 
        userReposControl.replay();
        
        List<Dependency> dependencies = searchService.getDependencies( "org.apache.archiva", "archiva-test", "1.0" );
        
        repoBrowsingControl.verify();
        userReposControl.verify();
        
        assertNotNull( dependencies );
        assertEquals( 2, dependencies.size() );
    }
    
    public void testGetDependenciesArtifactDoesNotExist()
        throws Exception
    {
        List<String> observableRepoIds = new ArrayList<String>();
        observableRepoIds.add( "repo1.mirror" );
        observableRepoIds.add( "public.releases" );
        
        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );
        repoBrowsingControl.expectAndThrow( 
               repoBrowsing.selectVersion( "", observableRepoIds, "org.apache.archiva", "archiva-test", "1.0" ), new ObjectNotFoundException( "Artifact does not exist." ) );
        
        userReposControl.replay();
        repoBrowsingControl.replay();
        
        try
        {
            searchService.getDependencies( "org.apache.archiva", "archiva-test", "1.0" );
            fail( "An exception should have been thrown." );
        }
        catch ( Exception e )
        {
            assertEquals( "Artifact does not exist.", e.getMessage() );
        }
        
        userReposControl.verify();
        repoBrowsingControl.verify();
    }
    
/* get dependency tree */
    
    public void testGetDependencyTreeArtifactExists()
        throws Exception
    {
        
    }
    
    public void testGetDependencyTreeArtifactDoesNotExist()
        throws Exception
    {
    
    }
    
/* get dependees */
    
    public void testGetDependees()
        throws Exception
    {
        Date date = new Date();
        List<String> observableRepoIds = new ArrayList<String>();
        observableRepoIds.add( "repo1.mirror" );
        observableRepoIds.add( "public.releases" );
        
        List<ArchivaProjectModel> dependeeModels = new ArrayList<ArchivaProjectModel>();
        ArchivaProjectModel dependeeModel = new ArchivaProjectModel();
        dependeeModel.setGroupId( "org.apache.archiva" );
        dependeeModel.setArtifactId( "archiva-dependee-one" );
        dependeeModel.setVersion( "1.0" );
        dependeeModel.setWhenIndexed( date );
        dependeeModels.add( dependeeModel );
        
        dependeeModel = new ArchivaProjectModel();
        dependeeModel.setGroupId( "org.apache.archiva" );
        dependeeModel.setArtifactId( "archiva-dependee-two" );
        dependeeModel.setVersion( "1.0" );
        dependeeModel.setWhenIndexed( date );
        dependeeModels.add( dependeeModel );
        
        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );
        repoBrowsingControl.expectAndReturn( repoBrowsing.getUsedBy( "", observableRepoIds, "org.apache.archiva", "archiva-test", "1.0" ), dependeeModels );
        
        repoBrowsingControl.replay(); 
        userReposControl.replay();

        List<Artifact> dependees = searchService.getDependees( "org.apache.archiva", "archiva-test", "1.0" );
        
        repoBrowsingControl.verify();
        userReposControl.verify();
        
        assertNotNull( dependees );
        assertEquals( 2, dependees.size() );
    }
    
    /*public void testGetDependeesArtifactDoesNotExist()
        throws Exception
    {
        Date date = new Date();
        List<String> observableRepoIds = new ArrayList<String>();
        observableRepoIds.add( "repo1.mirror" );
        observableRepoIds.add( "public.releases" );
        
        List dependeeModels = new ArrayList();
        ArchivaProjectModel dependeeModel = new ArchivaProjectModel();
        dependeeModel.setGroupId( "org.apache.archiva" );
        dependeeModel.setArtifactId( "archiva-dependee-one" );
        dependeeModel.setVersion( "1.0" );
        dependeeModel.setWhenIndexed( date );
        dependeeModels.add( dependeeModel );
        
        dependeeModel = new ArchivaProjectModel();
        dependeeModel.setGroupId( "org.apache.archiva" );
        dependeeModel.setArtifactId( "archiva-dependee-two" );
        dependeeModel.setVersion( "1.0" );
        dependeeModel.setWhenIndexed( date );
        dependeeModels.add( dependeeModel );
        
        userReposControl.expectAndReturn( userRepos.getObservableRepositories(), observableRepoIds );
        repoBrowsingControl.expectAndReturn( repoBrowsing.getUsedBy( "", observableRepoIds, "org.apache.archiva", "archiva-test", "1.0" ), dependeeModels );
        
        repoBrowsingControl.replay(); 
        userReposControl.replay();

        try
        {
            List<Artifact> dependees = searchService.getDependees( "org.apache.archiva", "archiva-test", "1.0" );
            fail( "An exception should have been thrown." );
        }
        catch ( Exception e )
        {
            assertEquals( "Artifact does not exist." )
        }
        
        repoBrowsingControl.verify();
        userReposControl.verify();
        
        assertNotNull( dependees );
        assertEquals( 2, dependees.size() );
    }*/
}