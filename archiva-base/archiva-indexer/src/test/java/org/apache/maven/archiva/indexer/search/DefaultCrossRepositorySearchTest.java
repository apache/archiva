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

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.indexer.MockConfiguration;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Map;

/**
 * DefaultCrossRepositorySearchTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DefaultCrossRepositorySearchTest
    extends PlexusTestCase
{

    protected void setUp()
        throws Exception
    {
        super.setUp();

        RepositoryContentIndexFactory indexFactory = (RepositoryContentIndexFactory) lookup(
                                                                                             RepositoryContentIndexFactory.class
                                                                                                 .getName(), "lucene" );

        File repoDir = new File( getBasedir(), "src/test/managed-repository" );

        assertTrue( "Default Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        String repoUri = "file://" + StringUtils.replace( repoDir.getAbsolutePath(), "\\", "/" );

        ArchivaRepository repository = new ArchivaRepository( "testDefaultRepo", "Test Default Repository", repoUri );

        File indexLocation = new File( "target/index-crossrepo-" + getName() + "/" );

        MockConfiguration config = (MockConfiguration) lookup( ArchivaConfiguration.class.getName(), "mock" );

        RepositoryConfiguration repoConfig = new RepositoryConfiguration();
        repoConfig.setId( repository.getId() );
        repoConfig.setName( repository.getModel().getName() );
        repoConfig.setUrl( repository.getModel().getUrl() );
        repoConfig.setIndexDir( indexLocation.getAbsolutePath() );
        repoConfig.setIndexed( true );

        if ( indexLocation.exists() )
        {
            FileUtils.deleteDirectory( indexLocation );
        }

        config.getConfiguration().addRepository( repoConfig );

        // Create the (empty) indexes.
        RepositoryContentIndex indexHashcode = indexFactory.createHashcodeIndex( repository );
        RepositoryContentIndex indexBytecode = indexFactory.createBytecodeIndex( repository );
        RepositoryContentIndex indexContents = indexFactory.createFileContentIndex( repository );

        // Now populate them.
        Map hashcodesMap = ( new HashcodesIndexPopulator() ).populate( new File( getBasedir() ) );
        indexHashcode.indexRecords( hashcodesMap.values() );
        assertEquals( "Hashcode Key Count", hashcodesMap.size(), indexHashcode.getAllRecordKeys().size() );
        assertRecordCount( indexHashcode, hashcodesMap.size() );

        Map bytecodeMap = ( new BytecodeIndexPopulator() ).populate( new File( getBasedir() ) );
        indexBytecode.indexRecords( bytecodeMap.values() );
        assertEquals( "Bytecode Key Count", bytecodeMap.size(), indexBytecode.getAllRecordKeys().size() );
        assertRecordCount( indexBytecode, bytecodeMap.size() );

        Map contentMap = ( new FileContentIndexPopulator() ).populate( new File( getBasedir() ) );
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
        CrossRepositorySearch search = (CrossRepositorySearch) lookup( CrossRepositorySearch.class.getName(), "default" );
        assertNotNull( "CrossRepositorySearch:default should not be null.", search );
        return search;
    }

    public void testSearchTerm_Org()
        throws Exception
    {
        CrossRepositorySearch search = lookupCrossRepositorySearch();

        SearchResultLimits limits = new SearchResultLimits( 0 );
        limits.setPageSize( 20 );

        SearchResults results = search.searchForTerm( "org", limits );
        assertResults( 1, 7, results );
    }

    public void testSearchTerm_Junit()
        throws Exception
    {
        CrossRepositorySearch search = lookupCrossRepositorySearch();

        SearchResultLimits limits = new SearchResultLimits( 0 );
        limits.setPageSize( 20 );

        SearchResults results = search.searchForTerm( "junit", limits );
        assertResults( 1, 3, results );
    }

    public void testSearchInvalidTerm()
        throws Exception
    {
        CrossRepositorySearch search = lookupCrossRepositorySearch();

        SearchResultLimits limits = new SearchResultLimits( 0 );
        limits.setPageSize( 20 );

        SearchResults results = search.searchForTerm( "monosodium", limits );
        assertResults( 1, 0, results );
    }

    private void assertResults( int repoCount, int hitCount, SearchResults results )
    {
        assertNotNull( "Search Results should not be null.", results );
        assertEquals( "Repository Hits", repoCount, results.getRepositories().size() );

        assertEquals( "Search Result Hits", hitCount, results.getHits().size() );
    }
}
