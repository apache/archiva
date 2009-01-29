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
    
    private final static String TEST_REPO = "nexus-search-test-repo"; 
    
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        indexer = ( NexusIndexer ) lookup( NexusIndexer.class );
        
        archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );
        
        archivaConfig = ( ArchivaConfiguration ) archivaConfigControl.getMock();
                
        search = new NexusRepositorySearch( indexer, archivaConfig );
        
        indexerEngine = ( IndexerEngine ) lookup( IndexerEngine.class );
        
        artifactContextProducer = ( ArtifactContextProducer ) lookup( ArtifactContextProducer.class );
        
        config = new Configuration();
        
        ManagedRepositoryConfiguration repositoryConfig = new ManagedRepositoryConfiguration();
        repositoryConfig.setId( TEST_REPO );
        repositoryConfig.setLocation( getBasedir() + "/target/test-classes/" + TEST_REPO );
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setName( "Nexus Search Test Repository" );
        repositoryConfig.setScanned( true );
        repositoryConfig.setSnapshots( false );
        repositoryConfig.setReleases( true );
        
        config.addManagedRepository( repositoryConfig );
        
        createIndex();
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        FileUtils.deleteDirectory( new File( getBasedir(), "/target/test-classes/"+ TEST_REPO + "/.indexer" ) );
        assertFalse( new File( getBasedir(), "/target/test-classes/"+ TEST_REPO + "/.indexer" ).exists() );
        
        super.tearDown();
    }
    
    private void createIndex()
        throws IOException, UnsupportedExistingLuceneIndexException
    {
        context =
            indexer.addIndexingContext( TEST_REPO, TEST_REPO, new File( getBasedir(), "/target/test-classes/" + TEST_REPO ),
                                    new File( getBasedir(), "/target/test-classes/" + TEST_REPO + "/.indexer"), null, null, NexusIndexer.FULL_INDEX );
        context.setSearchable( true );
        
        indexerEngine.beginIndexing( context );
        
        File artifactFile =
            new File( getBasedir(),
                      "/target/test-classes/" + TEST_REPO + "/org/apache/archiva/archiva-search/1.0/archiva-search-1.0.jar" );
        ArtifactContext ac = artifactContextProducer.getArtifactContext( context, artifactFile );
        indexerEngine.index( context, ac );
        
        artifactFile =
            new File( getBasedir(),
                      "/target/test-classes/" + TEST_REPO + "/org/apache/archiva/archiva-test/1.0/archiva-test-1.0.jar" );
        ac = artifactContextProducer.getArtifactContext( context, artifactFile );
        indexerEngine.index( context, ac );
        
        artifactFile =
            new File( getBasedir(),
                      "/target/test-classes/" + TEST_REPO + "/org/apache/archiva/archiva-test/2.0/archiva-test-2.0.jar" );
        ac = artifactContextProducer.getArtifactContext( context, artifactFile );
        indexerEngine.index( context, ac );
        
        indexerEngine.endIndexing( context );     
        
        assertTrue( new File( getBasedir(), "/target/test-classes/"+ TEST_REPO + "/.indexer" ).exists() );
    }
    
    public void testQuickSearch()
        throws Exception
    {
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO );
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        
        archivaConfigControl.replay();
        
        SearchResults results = search.search( "user", selectedRepos, "archiva-search", null );
        
        archivaConfigControl.verify();
        
        assertNotNull( results );
        assertEquals( 1, results.getTotalHits() );
        
        SearchResultHit hit = results.getHits().get( 0 );
        assertEquals( "org.apache.archiva", hit.getGroupId() );
        assertEquals( "archiva-search", hit.getArtifactId() );
        assertEquals( "1.0", hit.getVersions().get( 0 ) );
        assertEquals( "nexus-search-test-repo", hit.getRepositoryId() );
        
        archivaConfigControl.reset();
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        
        archivaConfigControl.replay();
        
        results = search.search( "user", selectedRepos, "org.apache.archiva", null );
        
        archivaConfigControl.verify();
        
        assertNotNull( results );
        assertEquals( 2, results.getTotalHits() );
        
        //TODO: search for class & package names
    }
    
    public void testQuickSearchWithPagination()
        throws Exception
    {
        List<String> selectedRepos = new ArrayList<String>();
        selectedRepos.add( TEST_REPO );
        
        // page 1
        SearchResultLimits limits = new SearchResultLimits( 1 );
        limits.setPageSize( 1 );        
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        
        archivaConfigControl.replay();
        
        SearchResults results = search.search( "user", selectedRepos, "org", limits );
        
        archivaConfigControl.verify();
        
        assertNotNull( results );
        assertEquals( 1, results.getTotalHits() );
        
        archivaConfigControl.reset();
        
        // page 2
        limits = new SearchResultLimits( 2 );
        limits.setPageSize( 1 );   
        
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        
        archivaConfigControl.replay();
        
        results = search.search( "user", selectedRepos, "org", limits );
        
        archivaConfigControl.verify();
        
        assertNotNull( results );
        assertEquals( 1, results.getTotalHits() );
    }
    
    public void testArtifactFoundInMultipleRepositories()
        throws Exception
    {
        // there should be no duplicates in the search result hit
        // TODO: [BROWSE] in artifact info from browse, display all the repositories where the artifact is found
    }
    
    public void testNoMatchFound()
        throws Exception 
    {
    
    }
    
    public void testNoIndexFound()
        throws Exception
    {
    
    }
    
    public void testSearchWithinSearchResults()
        throws Exception
    {
    
    }
    
    public void testAdvancedSearch()
        throws Exception
    {
    
    }
    
    public void testPagination()
        throws Exception
    {
    
    }
    
}
