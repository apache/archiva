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
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * DefaultCrossRepositorySearchTest
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DefaultCrossRepositorySearchTest
    extends PlexusTestCase
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

        String expectedRepos[] = new String[] {
            TEST_DEFAULT_REPO_ID
        };
        
        String expectedResults[] = new String[] { 
            "org","org2","org3","org4","org5","org6","org7"
        };
        
        assertSearchResults( expectedRepos, expectedResults, search, "org" );
    }

    public void testSearchTerm_Junit()
        throws Exception
    {
        CrossRepositorySearch search = lookupCrossRepositorySearch();
        
        String expectedRepos[] = new String[] {
            TEST_DEFAULT_REPO_ID
        };
        
        String expectedResults[] = new String[] { 
            "junit","junit2","junit3"
        };
        
        assertSearchResults( expectedRepos, expectedResults, search, "junit" );
    }

    public void testSearchInvalidTerm()
        throws Exception
    {
        CrossRepositorySearch search = lookupCrossRepositorySearch();

        String expectedRepos[] = new String[] {
            TEST_DEFAULT_REPO_ID
        };
        
        String expectedResults[] = new String[] { 
            // Nothing.
        };
        
        assertSearchResults( expectedRepos, expectedResults, search, "monosodium" );
    }
    
    private void assertSearchResults( String expectedRepos[], String expectedResults[], CrossRepositorySearch search, String term )
        throws Exception
    {
        SearchResultLimits limits = new SearchResultLimits( 0 );
        limits.setPageSize( 20 );
        
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.addAll( Arrays.asList( expectedRepos ) );
        
        SearchResults results = search.searchForTerm( "guest", selectedRepos, term, limits );
        
        assertNotNull( "Search Results should not be null.", results );
        assertEquals( "Repository Hits", expectedRepos.length, results.getRepositories().size() );
        // TODO: test the repository ids returned.

        assertEquals( "Search Result Hits", expectedResults.length, results.getHits().size() );
        // TODO: test the order of hits.
        // TODO: test the value of the hits.
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
