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
import java.util.HashMap;
import java.util.List;

import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.indexer.util.SearchUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.constraints.UniqueVersionConstraint;
import org.apache.maven.archiva.indexer.search.SearchResultHit;
import org.apache.maven.archiva.indexer.search.SearchResultLimits;
import org.apache.maven.archiva.indexer.search.SearchResults;
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
        
        archivaXworkUserControl.expectAndReturn( archivaXworkUser.getActivePrincipal( new HashMap() ), "user", 3 );
                
        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( "user" ), selectedRepos, 2 );
        
        searchControl.expectAndReturn( search.search( "user", selectedRepos, "archiva", limits, null ), results );
                
        daoControl.expectAndReturn( dao.query( new UniqueVersionConstraint( selectedRepos, hit.getGroupId(), hit.getArtifactId() ) ), versions );
                
        archivaXworkUserControl.replay();
        userReposControl.replay();
        searchControl.replay();
        daoControl.replay();
        
        String result = action.quickSearch();
        
        assertEquals( Action.SUCCESS, result );        
        
        archivaXworkUserControl.verify();
        userReposControl.verify();
        searchControl.verify();
        daoControl.verify();
    }
    
    public void testSearchWithinSearchResults()
        throws Exception
    {
        // test filter of completeQueryString?
        // test no need to filter completeQueryString?
    }
        
    public void testAdvancedSearch()
        throws Exception
    {
    
    }
    
    public void testSearchUserHasNoAccessToAnyRepository()
        throws Exception
    {
    
    }
    
    public void testNoSearchHits()
        throws Exception
    {
    
    }
    
    // test pagination or just totalPages?
    public void testPagination()
        throws Exception
    {
    
    }
    
    public void testFindArtifactWithOneHit()
        throws Exception
    {
    
    }
    
    public void testFindArtifactWithMultipleHits()
        throws Exception
    {
    
    }
    
    public void testFindArtifactNoChecksumSpecified()
        throws Exception
    {
    
    }
    
}
