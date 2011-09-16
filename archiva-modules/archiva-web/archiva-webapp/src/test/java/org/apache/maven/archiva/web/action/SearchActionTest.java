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

import com.opensymphony.xwork2.Action;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.indexer.search.SearchFields;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResultLimits;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.indexer.util.SearchUtil;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.memory.TestRepositorySessionFactory;
import org.apache.archiva.security.UserRepositories;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.easymock.MockControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class SearchActionTest
    extends AbstractActionTestCase
{
    private SearchAction action;

    private MockControl userReposControl;

    private UserRepositories userRepos;

    private MockControl searchControl;

    private MockControl repoAdminControl;

    private ManagedRepositoryAdmin managedRepositoryAdmin;

    private RepositorySearch search;

    private static final String TEST_CHECKSUM = "afbcdeaadbcffceabbba1";

    private static final String TEST_REPO = "test-repo";

    private static final String GUEST = "guest";

    private RepositorySession session;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = new SearchAction();

        session = mock( RepositorySession.class );
        //TestRepositorySessionFactory factory = (TestRepositorySessionFactory) lookup( RepositorySessionFactory.class );
        TestRepositorySessionFactory factory = new TestRepositorySessionFactory();
        factory.setRepositorySession( session );
        action.setRepositorySessionFactory( factory );

        MockControl archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );
        ArchivaConfiguration archivaConfig = (ArchivaConfiguration) archivaConfigControl.getMock();

        userReposControl = MockControl.createControl( UserRepositories.class );
        userRepos = (UserRepositories) userReposControl.getMock();

        searchControl = MockControl.createControl( RepositorySearch.class );
        searchControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        search = (RepositorySearch) searchControl.getMock();

        repoAdminControl = MockControl.createControl( ManagedRepositoryAdmin.class );
        managedRepositoryAdmin = (ManagedRepositoryAdmin) repoAdminControl.getMock();

        //( (DefaultManagedRepositoryAdmin) action.getManagedRepositoryAdmin() ).setArchivaConfiguration( archivaConfig );

        action.setManagedRepositoryAdmin( managedRepositoryAdmin );
        action.setUserRepositories( userRepos );
        action.setNexusSearch( search );
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
        results.addHit( SearchUtil.getHitId( "org.apache.archiva", "archiva-configuration", null, "jar" ), hit );

        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( "user" ), selectedRepos );

        searchControl.expectAndReturn( search.search( "user", selectedRepos, "archiva", limits, null ), results );

        userReposControl.replay();
        searchControl.replay();

        action.setPrincipal( "user" );
        String result = action.quickSearch();

        assertEquals( Action.SUCCESS, result );
        assertEquals( 1, action.getTotalPages() );
        assertEquals( 1, action.getResults().getTotalHits() );

        userReposControl.verify();
        searchControl.verify();
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
        results.addHit( SearchUtil.getHitId( "org.apache.archiva", "archiva-configuration", null, "jar" ), hit );

        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( "user" ), selectedRepos );

        searchControl.expectAndReturn( search.search( "user", selectedRepos, "archiva", limits, parsed ), results );

        userReposControl.replay();
        searchControl.replay();

        action.setPrincipal( "user" );
        String result = action.quickSearch();

        assertEquals( Action.SUCCESS, result );
        assertEquals( "org;apache;archiva", action.getCompleteQueryString() );
        assertEquals( 1, action.getTotalPages() );
        assertEquals( 1, action.getResults().getTotalHits() );

        userReposControl.verify();
        searchControl.verify();
    }

    public void testQuickSearchUserHasNoAccessToAnyRepository()
        throws Exception
    {
        action.setQ( "archiva" );
        action.setCurrentPage( 0 );

        List<String> selectedRepos = new ArrayList<String>();

        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( "user" ), selectedRepos );

        userReposControl.replay();

        action.setPrincipal( "user" );
        String result = action.quickSearch();

        assertEquals( GlobalResults.ACCESS_TO_NO_REPOS, result );

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

        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( "user" ), selectedRepos );

        searchControl.expectAndReturn( search.search( "user", selectedRepos, "archiva", limits, null ), results );

        userReposControl.replay();
        searchControl.replay();

        action.setPrincipal( "user" );
        String result = action.quickSearch();

        assertEquals( Action.INPUT, result );

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
        results.addHit( SearchUtil.getHitId( "org.apache.archiva", "archiva-configuration", null, "jar" ), hit );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( "internal" );
        selectedRepos.add( "snapshots" );

        SearchFields searchFields = new SearchFields( "org", null, null, null, null, selectedRepos );

        searchControl.expectAndReturn( search.search( "user", searchFields, limits ), results );

        searchControl.replay();

        String result = action.filteredSearch();

        assertEquals( Action.SUCCESS, result );
        assertEquals( 1, action.getTotalPages() );
        assertEquals( 1, action.getResults().getTotalHits() );

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
        results.addHit( SearchUtil.getHitId( "org.apache.archiva", "archiva-configuration", null, "jar" ), hit );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( "internal" );

        SearchFields searchFields = new SearchFields( "org", null, null, null, null, selectedRepos );

        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( "user" ), selectedRepos );

        searchControl.expectAndReturn( search.search( "user", searchFields, limits ), results );

        searchControl.replay();
        userReposControl.replay();

        action.setPrincipal( "user" );
        String result = action.filteredSearch();

        assertEquals( Action.SUCCESS, result );
        assertEquals( 1, action.getTotalPages() );
        assertEquals( 1, action.getResults().getTotalHits() );

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

        searchControl.expectAndReturn( search.search( "user", searchFields, limits ), results );

        searchControl.replay();

        String result = action.filteredSearch();

        assertEquals( Action.INPUT, result );
        assertFalse( action.getActionErrors().isEmpty() );
        assertEquals( "No results found", (String) action.getActionErrors().iterator().next() );

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
        action.setQ( TEST_CHECKSUM );

        MockControl control = MockControl.createControl( MetadataRepository.class );
        MetadataRepository metadataRepository = (MetadataRepository) control.getMock();
        when( session.getRepository() ).thenReturn( metadataRepository );

        ArtifactMetadata artifact = createArtifact( "archiva-configuration", "1.0" );
        control.expectAndReturn( metadataRepository.getArtifactsByChecksum( TEST_REPO, TEST_CHECKSUM ),
                                 Collections.singletonList( artifact ) );

        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( GUEST ),
                                          Collections.singletonList( TEST_REPO ) );

        control.replay();
        userReposControl.replay();

        String result = action.findArtifact();
        assertEquals( "artifact", result );
        assertEquals( 1, action.getDatabaseResults().size() );
        assertEquals( artifact, action.getDatabaseResults().get( 0 ) );

        control.verify();
        userReposControl.verify();
    }

    public void testFindArtifactWithMultipleHits()
        throws Exception
    {
        action.setQ( TEST_CHECKSUM );

        MockControl control = MockControl.createControl( MetadataRepository.class );
        MetadataRepository metadataRepository = (MetadataRepository) control.getMock();
        when( session.getRepository() ).thenReturn( metadataRepository );

        List<ArtifactMetadata> artifacts = Arrays.asList( createArtifact( "archiva-configuration", "1.0" ),
                                                          createArtifact( "archiva-indexer", "1.0" ) );
        control.expectAndReturn( metadataRepository.getArtifactsByChecksum( TEST_REPO, TEST_CHECKSUM ), artifacts );

        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( GUEST ),
                                          Collections.singletonList( TEST_REPO ) );

        control.replay();
        userReposControl.replay();

        String result = action.findArtifact();
        assertEquals( "results", result );
        assertFalse( action.getDatabaseResults().isEmpty() );
        assertEquals( 2, action.getDatabaseResults().size() );

        control.verify();
        userReposControl.verify();
    }

    public void testFindArtifactNoChecksumSpecified()
        throws Exception
    {
        String result = action.findArtifact();

        assertEquals( Action.INPUT, result );
        assertFalse( action.getActionErrors().isEmpty() );
        assertEquals( "Unable to search for a blank checksum", (String) action.getActionErrors().iterator().next() );
    }

    public void testFindArtifactNoResults()
        throws Exception
    {
        action.setQ( TEST_CHECKSUM );

        MockControl control = MockControl.createControl( MetadataRepository.class );
        MetadataRepository metadataRepository = (MetadataRepository) control.getMock();
        when( session.getRepository() ).thenReturn( metadataRepository );

        control.expectAndReturn( metadataRepository.getArtifactsByChecksum( TEST_REPO, TEST_CHECKSUM ),
                                 Collections.<ArtifactMetadata>emptyList() );

        userReposControl.expectAndReturn( userRepos.getObservableRepositoryIds( GUEST ),
                                          Collections.singletonList( TEST_REPO ) );

        control.replay();
        userReposControl.replay();

        String result = action.findArtifact();
        assertEquals( Action.INPUT, result );
        assertFalse( action.getActionErrors().isEmpty() );
        assertEquals( "No results found", (String) action.getActionErrors().iterator().next() );

        control.verify();
        userReposControl.verify();
    }

    private ArtifactMetadata createArtifact( String project, String version )
    {
        ArtifactMetadata metadata = new ArtifactMetadata();
        metadata.setNamespace( "org.apache.archiva" );
        metadata.setProject( project );
        metadata.setProjectVersion( version );
        metadata.setVersion( version );
        metadata.setRepositoryId( TEST_REPO );
        metadata.setId( project + "-" + version + ".jar" );
        return metadata;
    }
}
