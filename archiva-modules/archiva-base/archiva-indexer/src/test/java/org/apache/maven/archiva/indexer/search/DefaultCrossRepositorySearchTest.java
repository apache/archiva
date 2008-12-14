package org.apache.maven.archiva.indexer.search;

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

import org.apache.commons.io.FileUtils;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.indexer.MockConfiguration;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.bytecode.BytecodeRecord;
import org.apache.maven.archiva.indexer.filecontent.FileContentRecord;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesRecord;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * DefaultCrossRepositorySearchTest
 *
 * @version $Id$
 */
public class DefaultCrossRepositorySearchTest    
    extends PlexusInSpringTestCase
{
    private static final String TEST_DEFAULT_REPOSITORY_NAME = "Test Default Repository";

    private static final String TEST_DEFAULT_REPO_ID = "testDefaultRepo";

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        RepositoryContentIndexFactory indexFactory =
            (RepositoryContentIndexFactory) lookup( RepositoryContentIndexFactory.class
               .getName(), "lucene" );

        File repoDir = new File( getBasedir(), "src/test/managed-repository" );

        assertTrue( "Default Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        ManagedRepositoryConfiguration repository = createRepository( TEST_DEFAULT_REPO_ID, TEST_DEFAULT_REPOSITORY_NAME, repoDir );

        File indexLocation = new File( "target/index-crossrepo-" + getName() + "/" );

        MockConfiguration config = (MockConfiguration) lookup( ArchivaConfiguration.class.getName(), "mock" );

        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( TEST_DEFAULT_REPO_ID );
        repoConfig.setName( TEST_DEFAULT_REPOSITORY_NAME );
        repoConfig.setLocation( repoDir.getAbsolutePath() );
        repoConfig.setIndexDir( indexLocation.getAbsolutePath() );
        repoConfig.setScanned( true );

        if ( indexLocation.exists() )
        {
            FileUtils.deleteDirectory( indexLocation );
        }

        config.getConfiguration().addManagedRepository( repoConfig );

        // Create the (empty) indexes.
        RepositoryContentIndex indexHashcode = indexFactory.createHashcodeIndex( repository );
        RepositoryContentIndex indexBytecode = indexFactory.createBytecodeIndex( repository );
        RepositoryContentIndex indexContents = indexFactory.createFileContentIndex( repository );

        // Now populate them.
        Map<String, HashcodesRecord> hashcodesMap = new HashcodesIndexPopulator().populate( new File( getBasedir() ) );
        indexHashcode.indexRecords( hashcodesMap.values() );
        assertEquals( "Hashcode Key Count", hashcodesMap.size(), indexHashcode.getAllRecordKeys().size() );
        assertRecordCount( indexHashcode, hashcodesMap.size() );

        Map<String, BytecodeRecord> bytecodeMap = new BytecodeIndexPopulator().populate( new File( getBasedir() ) );
        indexBytecode.indexRecords( bytecodeMap.values() );
        assertEquals( "Bytecode Key Count", bytecodeMap.size(), indexBytecode.getAllRecordKeys().size() );
        assertRecordCount( indexBytecode, bytecodeMap.size() );

        Map<String, FileContentRecord> contentMap = new FileContentIndexPopulator().populate( new File( getBasedir() ) );
        indexContents.indexRecords( contentMap.values() );
        assertEquals( "File Content Key Count", contentMap.size(), indexContents.getAllRecordKeys().size() );
        assertRecordCount( indexContents, contentMap.size() );
    }

    private void assertRecordCount( RepositoryContentIndex index, int expectedCount )
        throws Exception
    {
        Query query = new MatchAllDocsQuery();
        Searcher searcher = (Searcher) index.getSearchable();
        Hits hits = searcher.search( query );
        assertEquals( "Expected Record Count for " + index.getId(), expectedCount, hits.length() );
    }

    private CrossRepositorySearch lookupCrossRepositorySearch()
        throws Exception
    {
        CrossRepositorySearch search =
            (CrossRepositorySearch) lookup( CrossRepositorySearch.class.getName(), "default" );
        assertNotNull( "CrossRepositorySearch:default should not be null.", search );
        return search;
    }

    public void testSearchTerm_Org()
        throws Exception
    {
        CrossRepositorySearch search = lookupCrossRepositorySearch();

        SearchResults results = search.searchForTerm( "guest", Arrays.asList(TEST_DEFAULT_REPO_ID), "org", new SearchResultLimits(1) );

        assertNotNull(results);
        assertEquals(7, results.getHits().size());
    }

    public void testSearchArtifactIdHasMoreWieghtThanGroupId() throws Exception
    {
        CrossRepositorySearch search = lookupCrossRepositorySearch();

        String expectedRepos[] = new String[] {
            TEST_DEFAULT_REPO_ID
        };

        List<SearchResultHit> expectedHits = new ArrayList<SearchResultHit>();
        SearchResultHit hit = new SearchResultHit();
        hit.setGroupId("ant");
        hit.setArtifactId("ant");
        hit.setVersion("1.5.1");
        expectedHits.add(hit);

        hit = new SearchResultHit();
        hit.setGroupId("ant");
        hit.setArtifactId("ant");
        hit.setVersion("1.5");
        expectedHits.add(hit);

        hit = new SearchResultHit();
        hit.setGroupId("ant");
        hit.setArtifactId("ant-optional");
        hit.setVersion("1.5.1");
        expectedHits.add(hit);

        hit = new SearchResultHit();
        hit.setGroupId("ant");
        hit.setArtifactId("ant-junit");
        hit.setVersion("1.6.5");
        expectedHits.add(hit);

        assertSearchResults( expectedRepos, expectedHits, search, "ant", null, false );
    }

    public void testSearchInvalidTerm()
        throws Exception
    {
        CrossRepositorySearch search = lookupCrossRepositorySearch();

        String expectedRepos[] = new String[] {
            TEST_DEFAULT_REPO_ID
        };

        assertSearchResults( expectedRepos, new ArrayList<SearchResultHit>(), search, "monosodium", null, false );
    }

    public void testSearchForClassesAndPackages()
        throws Exception
    {
        CrossRepositorySearch search = lookupCrossRepositorySearch();

        String expectedRepos[] = new String[] {
            TEST_DEFAULT_REPO_ID
        };

//        String expectedResults[] = new String[] {
//            "archiva-common-1.0.jar"
//        };

        ArrayList<SearchResultHit> expectedHits = new ArrayList<SearchResultHit>();

        // class with packagename search
        assertSearchResults( expectedRepos, expectedHits, search,
                             "org.apache.maven.archiva.common.utils.BaseFile", null, true );
        // class name search
        assertSearchResults( expectedRepos, expectedHits, search,
                             "BaseFile", null, true );

//        String expectedMethodSearchResults[] = new String[] {
//            "continuum-webapp-1.0.3-SNAPSHOT.war"
//        };

        ArrayList<SearchResultHit> expectedMethodSearchResults = new ArrayList<SearchResultHit>();

        // method search
        assertSearchResults( expectedRepos, expectedMethodSearchResults, search,
                             "org.apache.maven.continuum.web.action.BuildDefinitionAction.isBuildFresh", null, true );
    }
    
    public void testExecuteFilteredSearch()
        throws Exception
    {
        CrossRepositorySearch search = lookupCrossRepositorySearch();

        String expectedRepos[] = new String[] { TEST_DEFAULT_REPO_ID };

        String expectedResults[] = new String[] { "org1", "org2", "org3", "org4", "org5", "org6", "org7", "org8" };

        String secondExpectedResults[] = new String[] { "continuum-webapp" };

        String thirdExpectedResults[] = new String[] { "archiva-common" };

        // search for groupId
        assertFilteredSearchResults( expectedRepos, expectedResults, search, "org", null, null, null, 30 );

        // search for groupId and artifactId
        assertFilteredSearchResults( expectedRepos, secondExpectedResults, search, "org.apache.maven",
                                     "continuum-webapp", null, null, 30 );

        // search for groupId , artifactId and version
        assertFilteredSearchResults( expectedRepos, thirdExpectedResults, search, "org.apache.maven.archiva",
                                     "archiva-common", "1.0", null, 30 );
    }
    
    // MRM-981 - artifactIds with numeric characters aren't found in advanced search
    public void testFilteredSearchArtifactIdHasNumericChar()
        throws Exception
    {
        CrossRepositorySearch search = lookupCrossRepositorySearch();

        String expectedRepos[] = new String[] { TEST_DEFAULT_REPO_ID };

        String expectedResults[] = new String[] { "a-common5" };
        
        assertFilteredSearchResults( expectedRepos, expectedResults, search, null, "a-common5", null, null, 30 );
        
        assertFilteredSearchResults( expectedRepos, expectedResults, search, "a", "a-common5", null, null, 30 );
        
        assertFilteredSearchResults( expectedRepos, expectedResults, search, "a", "a-common5", "1.0", null, 30 );
        
        assertFilteredSearchResults( expectedRepos, expectedResults, search, "a", "a-common5", "1.0", "ACommonTestFile", 30 );
                
        assertFilteredSearchResults( expectedRepos, expectedResults, search, "a", "a-common5", "1.0", "a.common5.package.", 30 );
        
        String noHitsExpectedResults[] = new String[] {};
        
        assertFilteredSearchResults( expectedRepos, noHitsExpectedResults, search, "org.apache.maven.archiva",
                                     "a-common5", null, null, 30 );
    }
    
    private void assertFilteredSearchResults ( String expectedRepos[], String expectedResults[], CrossRepositorySearch search, 
                                               String groupId, String artifactId, String version, String className , int rowCount )
    {
        SearchResultLimits limits = new SearchResultLimits( 0 );
        limits.setPageSize(  rowCount );
        
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.addAll( Arrays.asList( expectedRepos ) );
        
        SearchResults results = null;
        
        results = search.executeFilteredSearch( "guest" , selectedRepos, groupId, artifactId, version, className, limits );
        
        assertNotNull( "Search Results should not be null.", results );
        assertEquals( "Repository Hits", expectedRepos.length, results.getRepositories().size() );
        assertEquals( expectedRepos.length, 1);
        assertEquals( TEST_DEFAULT_REPO_ID , selectedRepos.get( 0 ) );
        assertEquals( "Search Result Hits", expectedResults.length, results.getHits().size() );
    }
    
    private void assertSearchResults( String expectedRepos[], List<SearchResultHit> expectedResults, CrossRepositorySearch search,
                                      String term, List<String> previousSearchTerms, boolean bytecode )
        throws Exception
    {
        SearchResultLimits limits = new SearchResultLimits( 0 );
        limits.setPageSize( 20 );
        
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.addAll( Arrays.asList( expectedRepos ) );
       
        SearchResults results = null;

        if( previousSearchTerms == null )
        {
            if( bytecode )
            {
                results = search.searchForBytecode( "guest", selectedRepos, term, limits );
            }
            else
            {
                results = search.searchForTerm( "guest", selectedRepos, term, limits );
            }
        }
        else
        {
            results = search.searchForTerm( "guest", selectedRepos, term, limits, previousSearchTerms );
        }

        
        assertNotNull( "Search Results should not be null.", results );
        assertEquals( "Repository Hits", expectedRepos.length, results.getRepositories().size() );
        
        // TODO: test the repository ids returned.

        assertEquals( "Search Result Hits", expectedResults.size(), results.getHits().size() );

        for (int i = 0; i < expectedResults.size(); i++)
        {
            final SearchResultHit expectedResult = expectedResults.get(i);
            final SearchResultHit hit = results.getHits().get(i);
            assertEquals("artifactid", expectedResult.getArtifactId(), hit.getArtifactId());
            assertEquals("groupid", expectedResult.getGroupId(), hit.getGroupId());
            assertEquals("version", expectedResult.getVersion(), hit.getVersion());
        }
    }
    
    protected ManagedRepositoryConfiguration createRepository( String id, String name, File location )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        return repo;
    }
}
