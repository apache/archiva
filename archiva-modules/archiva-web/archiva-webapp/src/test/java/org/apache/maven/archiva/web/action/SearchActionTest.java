package org.apache.maven.archiva.web.action;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.indexer.search.SearchFields;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResultLimits;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.indexer.util.SearchUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.constraints.ArtifactsByChecksumConstraint;
import org.apache.maven.archiva.database.constraints.UniqueVersionConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.security.ArchivaXworkUser;
import org.apache.maven.archiva.security.UserRepositories;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import com.opensymphony.xwork2.Action;

/**
 * 
 */
public class SearchActionTest
    extends PlexusInSpringTestCase 
{
    private SearchAction action;
    
    private MockControl archivaConfigControl;
    
    private ArchivaConfiguration archivaConfig;
    
    private MockControl daoControl;
    
    private ArchivaDAO dao;
    
    private MockControl userReposControl;
    
    private UserRepositories userRepos;
    
    private MockControl archivaXworkUserControl;
    
    private ArchivaXworkUser archivaXworkUser;
    
    private MockControl searchControl;
    
    private RepositorySearch search;
    
    private MockControl artifactDaoControl;
    
    private ArtifactDAO artifactDao;
    
    @Override
    protected void setUp() 
        throws Exception
    {
        super.setUp();
        
        action = new SearchAction();
        
        archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );        
        archivaConfig = ( ArchivaConfiguration ) archivaConfigControl.getMock();
        
        daoControl = MockControl.createControl( ArchivaDAO.class );
        daoControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );        
        dao = ( ArchivaDAO ) daoControl.getMock();
        
        userReposControl = MockControl.createControl( UserRepositories.class );        
        userRepos = ( UserRepositories ) userReposControl.getMock();
        
        archivaXworkUserControl = MockClassControl.createControl( ArchivaXworkUser.class );
        archivaXworkUserControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        
        archivaXworkUser = ( ArchivaXworkUser ) archivaXworkUserControl.getMock();
        
        searchControl = MockControl.createControl( RepositorySearch.class );
        searchControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        search = ( RepositorySearch ) searchControl.getMock();
        
        artifactDaoControl = MockControl.createControl( ArtifactDAO.class );
        artifactDaoControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        artifactDao = ( ArtifactDAO ) artifactDaoControl.getMock();
                
        action.setArchivaConfiguration( archivaConfig );
        action.setArchivaXworkUser( archivaXworkUser );
        action.setUserRepositories( userRepos );
        action.setDao( dao );
        action.setNexusSearch( search );
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }
    
    // quick search...
    
    public void testQuickSearch()
        throws Exception
    {           
        action.setQ( "archiva" );
        action.setCurrentPage( 0 );
        action.setSearchResultsOnly( false );
        action.setCompleteQueryString( "" );
        
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( "internal" );
        selectedRepos.add( "snapshots" );
        
        SearchResultLimits limits = new SearchResultLimits( action.getCurrentPage() );
        limits.setPageSize( 30 );
        
        SearchResultHit hit = new SearchResultHit();
        hit.setGroupId( "org.apache.archiva" );
        hit.setArtifactId( "archiva-configuration" );
        hit.setUrl( "url" );
        hit.addVersion( "1.0" );
        hit.addVersion( "1.1" );        
        
        SearchResults results = new SearchResults();
        results.setLimits( limits );
        results.setTotalHits( 1 );
        results.addHit( SearchUtil.getHitId( "org.apache.archiva", "archiva-configuration" ), hit );
        
        List<String> versions = new ArrayList<String>();
        versions.add( "1.0" );
        versions.add( "1.1" );
        
        archivaXworkUserControl.expectAndReturn( getActivePrincipal(), "user", 3 );
                
        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( "user" ), selectedRepos, 2 );
        
        searchControl.expectAndReturn( search.search( "user", selectedRepos, "archiva", limits, null ), results );
                
        daoControl.expectAndReturn( dao.query( new UniqueVersionConstraint( selectedRepos, hit.getGroupId(), hit.getArtifactId() ) ), versions );
                
        archivaXworkUserControl.replay();
        userReposControl.replay();
        searchControl.replay();
        daoControl.replay();
        
        String result = action.quickSearch();
        
        assertEquals( Action.SUCCESS, result );      
        assertEquals( 1, action.getTotalPages() );
        assertEquals( 1, action.getResults().getTotalHits() );
        
        archivaXworkUserControl.verify();
        userReposControl.verify();
        searchControl.verify();
        daoControl.verify();
    }

    private String getActivePrincipal()
    {
        return archivaXworkUser.getActivePrincipal( Collections.<String,Object>emptyMap() );
    }
    
    public void testSearchWithinSearchResults()
        throws Exception
    {
        action.setQ( "archiva" );
        action.setCurrentPage( 0 );
        action.setSearchResultsOnly( true );
        action.setCompleteQueryString( "org;apache" );
        
        List<String> parsed = new ArrayList<String>();
        parsed.add( "org" );
        parsed.add( "apache" );
        
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( "internal" );
        selectedRepos.add( "snapshots" );
        
        SearchResultLimits limits = new SearchResultLimits( action.getCurrentPage() );
        limits.setPageSize( 30 );
        
        SearchResultHit hit = new SearchResultHit();
        hit.setGroupId( "org.apache.archiva" );
        hit.setArtifactId( "archiva-configuration" );
        hit.setUrl( "url" );
        hit.addVersion( "1.0" );
        hit.addVersion( "1.1" );        
        
        SearchResults results = new SearchResults();
        results.setLimits( limits );
        results.setTotalHits( 1 );
        results.addHit( SearchUtil.getHitId( "org.apache.archiva", "archiva-configuration" ), hit );
        
        List<String> versions = new ArrayList<String>();
        versions.add( "1.0" );
        versions.add( "1.1" );
        
        archivaXworkUserControl.expectAndReturn( getActivePrincipal(), "user", 3 );
                
        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( "user" ), selectedRepos, 2 );
        
        searchControl.expectAndReturn( search.search( "user", selectedRepos, "archiva", limits, parsed ), results );
                
        daoControl.expectAndReturn( dao.query( new UniqueVersionConstraint( selectedRepos, hit.getGroupId(), hit.getArtifactId() ) ), versions );
                
        archivaXworkUserControl.replay();
        userReposControl.replay();
        searchControl.replay();
        daoControl.replay();
        
        String result = action.quickSearch();
        
        assertEquals( Action.SUCCESS, result );
        assertEquals( "org;apache;archiva", action.getCompleteQueryString() );
        assertEquals( 1, action.getTotalPages() );
        assertEquals( 1, action.getResults().getTotalHits() );
        
        archivaXworkUserControl.verify();
        userReposControl.verify();
        searchControl.verify();
        daoControl.verify();
    }        
    
    public void testQuickSearchUserHasNoAccessToAnyRepository()
        throws Exception
    {
        action.setQ( "archiva" );
        action.setCurrentPage( 0 );
        
        List<String> selectedRepos = new ArrayList<String>();
        
        archivaXworkUserControl.expectAndReturn( getActivePrincipal(), "user" );
        
        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( "user" ), selectedRepos );
        
        archivaXworkUserControl.replay();
        userReposControl.replay();
        
        String result = action.quickSearch();
        
        assertEquals( GlobalResults.ACCESS_TO_NO_REPOS, result );        
        
        archivaXworkUserControl.verify();
        userReposControl.verify();        
    }
    
    public void testQuickSearchNoSearchHits()
        throws Exception
    {
        action.setQ( "archiva" );
        action.setCurrentPage( 0 );
        action.setSearchResultsOnly( false );
        action.setCompleteQueryString( "" );
        
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( "internal" );
        selectedRepos.add( "snapshots" );
        
        SearchResultLimits limits = new SearchResultLimits( action.getCurrentPage() );
        limits.setPageSize( 30 );
                
        SearchResults results = new SearchResults();
        
        archivaXworkUserControl.expectAndReturn( getActivePrincipal(), "user", 2 );
                
        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( "user" ), selectedRepos );
        
        searchControl.expectAndReturn( search.search( "user", selectedRepos, "archiva", limits, null ), results );
        
        archivaXworkUserControl.replay();
        userReposControl.replay();
        searchControl.replay();
        
        String result = action.quickSearch();
        
        assertEquals( Action.INPUT, result );        
        
        archivaXworkUserControl.verify();
        userReposControl.verify();
        searchControl.verify();
    }
        
    // advanced/filtered search...
    
    public void testAdvancedSearchOneRepository()
        throws Exception
    {
        List<String> managedRepos = new ArrayList<String>();
        managedRepos.add( "internal" );
        managedRepos.add( "snapshots" );
        
        action.setRepositoryId( "internal" );
        action.setManagedRepositoryList( managedRepos );
        action.setCurrentPage( 0 );
        action.setRowCount( 30 );
        action.setGroupId( "org" );
        
        SearchResultLimits limits = new SearchResultLimits( action.getCurrentPage() );
        limits.setPageSize( 30 );
        
        SearchResultHit hit = new SearchResultHit();
        hit.setGroupId( "org.apache.archiva" );
        hit.setArtifactId( "archiva-configuration" );
        hit.setUrl( "url" );
        hit.addVersion( "1.0" );
        hit.addVersion( "1.1" );        
        
        SearchResults results = new SearchResults();
        results.setLimits( limits );
        results.setTotalHits( 1 );
        results.addHit( SearchUtil.getHitId( "org.apache.archiva", "archiva-configuration" ), hit );
        
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( "internal" );
        selectedRepos.add( "snapshots" );
        
        SearchFields searchFields = new SearchFields( "org", null, null, null, null, selectedRepos );
        
        archivaXworkUserControl.expectAndReturn( getActivePrincipal(), "user" );
        
        searchControl.expectAndReturn( search.search( "user", searchFields, limits ), results );
        
        archivaXworkUserControl.replay();
        searchControl.replay();
        
        String result = action.filteredSearch();
        
        assertEquals( Action.SUCCESS, result );
        assertEquals( 1, action.getTotalPages() );
        assertEquals( 1, action.getResults().getTotalHits() );
        
        archivaXworkUserControl.verify();
        searchControl.verify();
    }
    
    public void testAdvancedSearchAllRepositories()
        throws Exception
    {   
        List<String> managedRepos = new ArrayList<String>();
        managedRepos.add( "internal" );
        managedRepos.add( "snapshots" );
        
        action.setRepositoryId( "all" );
        action.setManagedRepositoryList( managedRepos );
        action.setCurrentPage( 0 );
        action.setRowCount( 30 );
        action.setGroupId( "org" );
        
        SearchResultLimits limits = new SearchResultLimits( action.getCurrentPage() );
        limits.setPageSize( 30 );
        
        SearchResultHit hit = new SearchResultHit();
        hit.setGroupId( "org.apache.archiva" );
        hit.setArtifactId( "archiva-configuration" );
        hit.setUrl( "url" );
        hit.addVersion( "1.0" );
        hit.addVersion( "1.1" );        
        
        SearchResults results = new SearchResults();
        results.setLimits( limits );
        results.setTotalHits( 1 );
        results.addHit( SearchUtil.getHitId( "org.apache.archiva", "archiva-configuration" ), hit );
        
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( "internal" );
        
        SearchFields searchFields = new SearchFields( "org", null, null, null, null, selectedRepos );
        
        archivaXworkUserControl.expectAndReturn( getActivePrincipal(), "user", 2 );
        
        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( "user" ), selectedRepos );
        
        searchControl.expectAndReturn( search.search( "user", searchFields, limits ), results );
        
        archivaXworkUserControl.replay();
        searchControl.replay();
        userReposControl.replay();
        
        String result = action.filteredSearch();
        
        assertEquals( Action.SUCCESS, result );
        assertEquals( 1, action.getTotalPages() );
        assertEquals( 1, action.getResults().getTotalHits() );
        
        archivaXworkUserControl.verify();
        searchControl.verify();
        userReposControl.verify();
    }
        
    public void testAdvancedSearchNoSearchHits()
        throws Exception
    {
        List<String> managedRepos = new ArrayList<String>();
        managedRepos.add( "internal" );
        managedRepos.add( "snapshots" );
        
        action.setRepositoryId( "internal" );
        action.setManagedRepositoryList( managedRepos );
        action.setCurrentPage( 0 );
        action.setRowCount( 30 );
        action.setGroupId( "org" );
        
        SearchResultLimits limits = new SearchResultLimits( action.getCurrentPage() );
        limits.setPageSize( 30 );
                
        SearchResults results = new SearchResults();
        
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( "internal" );
        selectedRepos.add( "snapshots" );
        
        SearchFields searchFields = new SearchFields( "org", null, null, null, null, selectedRepos );
        
        archivaXworkUserControl.expectAndReturn( getActivePrincipal(), "user" );
        
        searchControl.expectAndReturn( search.search( "user", searchFields, limits ), results );
        
        archivaXworkUserControl.replay();
        searchControl.replay();
        
        String result = action.filteredSearch();
        
        assertEquals( Action.INPUT, result );
        assertFalse( action.getActionErrors().isEmpty() );
        assertEquals( "No results found",( String ) action.getActionErrors().iterator().next() );
        
        archivaXworkUserControl.verify();
        searchControl.verify();
    }
    
    public void testAdvancedSearchUserHasNoAccessToAnyRepository()
        throws Exception
    {
        List<String> managedRepos = new ArrayList<String>();
        
        action.setGroupId( "org.apache.archiva" );
        action.setManagedRepositoryList( managedRepos );
        
        String result = action.filteredSearch();
        
        assertEquals( GlobalResults.ACCESS_TO_NO_REPOS, result );
    }
    
    public void testAdvancedSearchNoSpecifiedCriteria()
        throws Exception
    {
        List<String> managedRepos = new ArrayList<String>();
        
        action.setManagedRepositoryList( managedRepos );
        
        String result = action.filteredSearch();
        
        assertEquals( Action.INPUT, result );
        assertFalse( action.getActionErrors().isEmpty() );
        assertEquals( "Advanced Search - At least one search criteria must be provided.",
                      (String) action.getActionErrors().iterator().next() );
    }
    
    // find artifact..
    
    public void testFindArtifactWithOneHit()
        throws Exception
    {
        action.setQ( "afbcdeaadbcffceabbba1" );
        
        List<ArchivaArtifact> dbResults = new ArrayList<ArchivaArtifact>();
        dbResults.add( new ArchivaArtifact( "org.apache.archiva", "archiva-configuration", "1.0",
                                           null, "jar", "test-repo" ) );
        
        daoControl.expectAndReturn( dao.getArtifactDAO(), artifactDao );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( new ArtifactsByChecksumConstraint( action.getQ() ) ), dbResults );
        
        daoControl.replay();
        artifactDaoControl.replay();
        
        String result = action.findArtifact();
        assertEquals( "artifact", result );
        assertFalse( action.getDatabaseResults().isEmpty() );
        assertEquals( 1, action.getDatabaseResults().size() );
        
        daoControl.verify();
        artifactDaoControl.verify();
    }
        
    public void testFindArtifactWithMultipleHits()
        throws Exception
    {
        action.setQ( "afbcdeaadbcffceabbba1" );
        
        List<ArchivaArtifact> dbResults = new ArrayList<ArchivaArtifact>();
        dbResults.add( new ArchivaArtifact( "org.apache.archiva", "archiva-configuration", "1.0",
                                           null, "jar", "test-repo" ) );
        dbResults.add( new ArchivaArtifact( "org.apache.archiva", "archiva-indexer", "1.0",
                                            null, "jar", "test-repo" ) );
        
        daoControl.expectAndReturn( dao.getArtifactDAO(), artifactDao );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( new ArtifactsByChecksumConstraint( action.getQ() ) ), dbResults );
        
        daoControl.replay();
        artifactDaoControl.replay();
        
        String result = action.findArtifact();
        assertEquals( "results", result );
        assertFalse( action.getDatabaseResults().isEmpty() );
        assertEquals( 2, action.getDatabaseResults().size() );
        
        daoControl.verify();
        artifactDaoControl.verify();
    }
    
    public void testFindArtifactNoChecksumSpecified()
        throws Exception
    {
        String result = action.findArtifact();
        
        assertEquals( Action.INPUT, result );
        assertFalse( action.getActionErrors().isEmpty() );
        assertEquals( "Unable to search for a blank checksum", ( String ) action.getActionErrors().iterator().next() );
    }
    
    public void testFindArtifactNoResults()
        throws Exception
    {
        action.setQ( "afbcdeaadbcffceabbba1" );
        
        List<ArchivaArtifact> dbResults = new ArrayList<ArchivaArtifact>();
        
        daoControl.expectAndReturn( dao.getArtifactDAO(), artifactDao );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( new ArtifactsByChecksumConstraint( action.getQ() ) ), dbResults );
        
        daoControl.replay();
        artifactDaoControl.replay();
        
        String result = action.findArtifact();
        assertEquals( Action.INPUT, result );
        assertFalse( action.getActionErrors().isEmpty() );
        assertEquals( "No results found", ( String )action.getActionErrors().iterator().next() );
        
        daoControl.verify();
        artifactDaoControl.verify();
    }
}
