package org.apache.archiva.indexer.search;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.indexer.search.SearchResultHit;
import org.apache.maven.archiva.indexer.search.SearchResultLimits;
import org.apache.maven.archiva.indexer.search.SearchResults;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactContextProducer;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.context.UnsupportedExistingLuceneIndexException;
import org.sonatype.nexus.index.creator.IndexerEngine;

public class NexusRepositorySearchTest
    extends PlexusInSpringTestCase
{
    private RepositorySearch search;

    private ArchivaConfiguration archivaConfig;

    private NexusIndexer indexer;

    private IndexingContext context;

    private IndexerEngine indexerEngine;

    private ArtifactContextProducer artifactContextProducer;

    private MockControl archivaConfigControl;

    private Configuration config;

    private final static String TEST_REPO_1 = "nexus-search-test-repo";

    private final static String TEST_REPO_2 = "nexus-search-test-repo-2";

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        indexer = (NexusIndexer) lookup( NexusIndexer.class );

        archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );

        archivaConfig = (ArchivaConfiguration) archivaConfigControl.getMock();

        search = new NexusRepositorySearch( indexer, archivaConfig );

        indexerEngine = (IndexerEngine) lookup( IndexerEngine.class );

        artifactContextProducer = (ArtifactContextProducer) lookup( ArtifactContextProducer.class );

        config = new Configuration();
        config.addManagedRepository( createRepositoryConfig( TEST_REPO_1 ) );

        List<File> files = new ArrayList<File>();
        files.add( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_1 +
            "/org/apache/archiva/archiva-search/1.0/archiva-search-1.0.jar" ) );
        files.add( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_1 +
            "/org/apache/archiva/archiva-test/1.0/archiva-test-1.0.jar" ) );
        files.add( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_1 +
            "/org/apache/archiva/archiva-test/2.0/archiva-test-2.0.jar" ) );

        createIndex( TEST_REPO_1, files );
    }

    private ManagedRepositoryConfiguration createRepositoryConfig( String repository )
    {
        ManagedRepositoryConfiguration repositoryConfig = new ManagedRepositoryConfiguration();
        repositoryConfig.setId( repository );
        repositoryConfig.setLocation( getBasedir() + "/target/test-classes/" + repository );
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setName( repository );
        repositoryConfig.setScanned( true );
        repositoryConfig.setSnapshots( false );
        repositoryConfig.setReleases( true );

        return repositoryConfig;
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        FileUtils.deleteDirectory( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_1 + "/.indexer" ) );
        assertFalse( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_1 + "/.indexer" ).exists() );

        super.tearDown();
    }

    private void createIndex( String repository, List<File> filesToBeIndexed )
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        context =
            indexer.addIndexingContext( repository, repository, new File( getBasedir(), "/target/test-classes/" +
                repository ), new File( getBasedir(), "/target/test-classes/" + repository + "/.indexer" ), null, null,
                                        NexusIndexer.FULL_INDEX );
        context.setSearchable( true );

        indexerEngine.beginIndexing( context );

        for ( File artifactFile : filesToBeIndexed )
        {
            ArtifactContext ac = artifactContextProducer.getArtifactContext( context, artifactFile );
            indexerEngine.index( context, ac );
        }

        indexerEngine.endIndexing( context );
        indexer.removeIndexingContext( context, false );
        
        assertTrue( new File( getBasedir(), "/target/test-classes/" + repository + "/.indexer" ).exists() );
    }

    public void testQuickSearch()
        throws Exception
    {   
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        // search artifactId
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "archiva-search", null, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 1, results.getTotalHits() );

        SearchResultHit hit = results.getHits().get( 0 );
        assertEquals( "org.apache.archiva", hit.getGroupId() );
        assertEquals( "archiva-search", hit.getArtifactId() );
        assertEquals( "1.0", hit.getVersions().get( 0 ) );
        
        archivaConfigControl.reset();

        // search groupId
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        results = search.search( "user", selectedRepos, "org.apache.archiva", null, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 2, results.getTotalHits() );

        //TODO: search for class & package names
    }

    public void testQuickSearchWithPagination()
        throws Exception
    {   
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );

        // page 1
        SearchResultLimits limits = new SearchResultLimits( 0 );
        limits.setPageSize( 1 );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "org", limits, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 1, results.getHits().size() );
        assertEquals( 2, results.getTotalHits() );
        assertEquals( limits, results.getLimits() );

        archivaConfigControl.reset();

        // page 2
        limits = new SearchResultLimits( 1 );
        limits.setPageSize( 1 );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        results = search.search( "user", selectedRepos, "org", limits, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 1, results.getHits().size() );
        assertEquals( 2, results.getTotalHits() );
        assertEquals( limits, results.getLimits() );
    }

    public void testArtifactFoundInMultipleRepositories()
        throws Exception
    {
        List<File> files = new ArrayList<File>();
        files.add( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_2 +
            "/org/apache/archiva/archiva-search/1.0/archiva-search-1.0.jar" ) );
        files.add( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_2 +
            "/org/apache/archiva/archiva-search/1.1/archiva-search-1.1.jar" ) );
        createIndex( TEST_REPO_2, files );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );
        selectedRepos.add( TEST_REPO_2 );

        config.addManagedRepository( createRepositoryConfig( TEST_REPO_2 ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 2 );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "archiva-search", null, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 1, results.getTotalHits() );

        SearchResultHit hit = results.getHits().get( 0 );
        assertEquals( "org.apache.archiva", hit.getGroupId() );
        assertEquals( "archiva-search", hit.getArtifactId() );
        assertEquals( 2, hit.getVersions().size() );
        assertTrue( hit.getVersions().contains( "1.0" ) );
        assertTrue( hit.getVersions().contains( "1.1" ) );

        archivaConfigControl.reset();

        FileUtils.deleteDirectory( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_2 + "/.indexer" ) );
        assertFalse( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_2 + "/.indexer" ).exists() );

        // TODO: [BROWSE] in artifact info from browse, display all the repositories where the artifact is found
    }

    public void testNoMatchFound()
        throws Exception
    {
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "dfghdfkweriuasndsaie", null, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 0, results.getTotalHits() );
    }

    public void testNoIndexFound()
        throws Exception
    {
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( "non-existing-repo" );
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "org.apache.archiva", null, null );
        assertNotNull( results );
        assertEquals( 0, results.getTotalHits() );
        
        archivaConfigControl.verify();            
    }

    public void testSearchWithinSearchResults()
        throws Exception
    {
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );
        
        List<String> previousSearchTerms = new ArrayList<String>();
        previousSearchTerms.add( "archiva-test" );
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", selectedRepos, "1.0", null, previousSearchTerms );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 1, results.getTotalHits() );
        
        SearchResultHit hit = results.getHits().get( 0 );
        assertEquals( "org.apache.archiva", hit.getGroupId() );
        assertEquals( "archiva-test", hit.getArtifactId() );
        assertEquals( 1, hit.getVersions().size() );
        assertEquals( "1.0", hit.getVersions().get( 0 ) );
    }

    public void testAdvancedSearch()
        throws Exception
    {
        List<File> files = new ArrayList<File>();
        files.add( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_2 +
            "/org/apache/archiva/archiva-search/1.0/archiva-search-1.0.jar" ) );
        files.add( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_2 +
            "/org/apache/archiva/archiva-search/1.1/archiva-search-1.1.jar" ) );
        createIndex( TEST_REPO_2, files );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );
        selectedRepos.add( TEST_REPO_2 );

        config.addManagedRepository( createRepositoryConfig( TEST_REPO_2 ) );
        
        SearchFields searchFields = new SearchFields();
        searchFields.setGroupId( "org.apache.archiva" );
        searchFields.setVersion( "1.0" );
        searchFields.setRepositories( selectedRepos );        
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 2 );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", searchFields, null );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 2, results.getTotalHits() );
        
        FileUtils.deleteDirectory( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_2 + "/.indexer" ) );
        assertFalse( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_2 + "/.indexer" ).exists() );
    }
    
    public void testAdvancedSearchWithPagination()
        throws Exception
    {
        List<File> files = new ArrayList<File>();
        files.add( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_2 +
            "/org/apache/archiva/archiva-search/1.0/archiva-search-1.0.jar" ) );
        files.add( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_2 +
            "/org/apache/archiva/archiva-search/1.1/archiva-search-1.1.jar" ) );
        createIndex( TEST_REPO_2, files );

        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO_1 );
        selectedRepos.add( TEST_REPO_2 );

        config.addManagedRepository( createRepositoryConfig( TEST_REPO_2 ) );
        
        SearchFields searchFields = new SearchFields();
        searchFields.setGroupId( "org.apache.archiva" );
        searchFields.setVersion( "1.0" );
        searchFields.setRepositories( selectedRepos );        
        
        // page 1
        
        SearchResultLimits limits = new SearchResultLimits( 0 );
        limits.setPageSize( 1 );
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 2 );

        archivaConfigControl.replay();

        SearchResults results = search.search( "user", searchFields, limits );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 2, results.getTotalHits() );
        assertEquals( 1, results.getHits().size() );
        
        // page 2
        archivaConfigControl.reset();
        
        limits = new SearchResultLimits( 1 );
        limits.setPageSize( 1 );
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 2 );

        archivaConfigControl.replay();

        results = search.search( "user", searchFields, limits );

        archivaConfigControl.verify();

        assertNotNull( results );
        assertEquals( 2, results.getTotalHits() );
        assertEquals( 1, results.getHits().size() );
        
        FileUtils.deleteDirectory( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_2 + "/.indexer" ) );
        assertFalse( new File( getBasedir(), "/target/test-classes/" + TEST_REPO_2 + "/.indexer" ).exists() );
    }

}
