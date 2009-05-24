package org.apache.maven.archiva.scheduled.executors;

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
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.scheduled.tasks.ArtifactIndexingTask;
import org.apache.maven.archiva.scheduled.tasks.TaskCreator;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.FlatSearchRequest;
import org.sonatype.nexus.index.FlatSearchResponse;
import org.sonatype.nexus.index.IndexerEngine;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.packer.IndexPacker;

/**
 * ArchivaIndexingTaskExecutorTest
 */
public class ArchivaIndexingTaskExecutorTest
    extends PlexusInSpringTestCase
{
    private ArchivaIndexingTaskExecutor indexingExecutor;
    
    private IndexerEngine indexerEngine;
    
    private IndexPacker indexPacker;
        
    private MockControl archivaConfigControl;
    
    private ArchivaConfiguration archivaConfiguration;
    
    private ManagedRepositoryConfiguration repositoryConfig;
    
    private Configuration configuration;
    
    private NexusIndexer indexer;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        
        indexingExecutor = new ArchivaIndexingTaskExecutor();
        indexingExecutor.initialize();    
        
        repositoryConfig = new ManagedRepositoryConfiguration();
        repositoryConfig.setId( "test-repo" );
        repositoryConfig.setLocation( getBasedir() + "/target/test-classes/test-repo" );
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setName( "Test Repository" );
        repositoryConfig.setScanned( true );
        repositoryConfig.setSnapshots( false );
        repositoryConfig.setReleases( true );
        
        configuration = new Configuration();
        configuration.addManagedRepository( repositoryConfig );
        
        archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = ( ArchivaConfiguration ) archivaConfigControl.getMock();
        
        indexer = ( NexusIndexer ) lookup( NexusIndexer.class );        
        indexerEngine = ( IndexerEngine ) lookup ( IndexerEngine.class );        
        indexPacker = ( IndexPacker ) lookup( IndexPacker.class );
        
        indexingExecutor.setIndexerEngine( indexerEngine );        
        indexingExecutor.setIndexPacker( indexPacker );        
        indexingExecutor.setArchivaConfiguration( archivaConfiguration );
    }
    
    protected void tearDown() throws Exception
    {
        // delete created index in the repository
        File indexDir = new File( repositoryConfig.getLocation(), ".indexer" );
        FileUtils.deleteDirectory( indexDir );
        assertFalse( indexDir.exists() );
        
        indexDir = new File( repositoryConfig.getLocation(), ".index" );
        FileUtils.deleteDirectory( indexDir );
        assertFalse( indexDir.exists() );
        
        super.tearDown();
    }
    
    public void testAddArtifactToIndex()
        throws Exception
    {
        File artifactFile =
            new File( repositoryConfig.getLocation(),
                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ArtifactIndexingTask task =
            TaskCreator.createIndexingTask( repositoryConfig.getId(), artifactFile, ArtifactIndexingTask.ADD );
        
        archivaConfigControl.expectAndReturn( archivaConfiguration.getConfiguration(), configuration );
        
        archivaConfigControl.replay();
        
        indexingExecutor.executeTask( task );
        
        archivaConfigControl.verify();
        
        BooleanQuery q = new BooleanQuery();        
        q.add( indexer.constructQuery( ArtifactInfo.GROUP_ID, "org.apache.archiva" ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.ARTIFACT_ID, "archiva-index-methods-jar-test" ), Occur.SHOULD );
        
        IndexingContext context = indexer.addIndexingContext( repositoryConfig.getId(), repositoryConfig.getId(), new File( repositoryConfig.getLocation() ),
                                    new File( repositoryConfig.getLocation(), ".indexer" ), null, null, NexusIndexer.FULL_INDEX );
        context.setSearchable( true );
        
        FlatSearchRequest request = new FlatSearchRequest( q );
        FlatSearchResponse response = indexer.searchFlat( request );
        
        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertTrue( new File( repositoryConfig.getLocation(), ".index" ).exists() );
        assertEquals( 1, response.getTotalHits() );
        
        Set<ArtifactInfo> results = response.getResults();
        
        ArtifactInfo artifactInfo = (ArtifactInfo) results.iterator().next();
        assertEquals( "org.apache.archiva", artifactInfo.groupId );
        assertEquals( "archiva-index-methods-jar-test", artifactInfo.artifactId );
        assertEquals( "test-repo", artifactInfo.repository );
    }
    
    public void testUpdateArtifactInIndex()
        throws Exception
    {
        File artifactFile =
            new File( repositoryConfig.getLocation(),
                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ArtifactIndexingTask task =
            TaskCreator.createIndexingTask( repositoryConfig.getId(), artifactFile, ArtifactIndexingTask.ADD );
        
        archivaConfigControl.expectAndReturn( archivaConfiguration.getConfiguration(), configuration, 2 );
        
        archivaConfigControl.replay();
        
        indexingExecutor.executeTask( task );
        indexingExecutor.executeTask( task );
        
        archivaConfigControl.verify();
                        
        BooleanQuery q = new BooleanQuery();        
        q.add( indexer.constructQuery( ArtifactInfo.GROUP_ID, "org.apache.archiva" ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.ARTIFACT_ID, "archiva-index-methods-jar-test" ), Occur.SHOULD );
        
        IndexSearcher searcher = new IndexSearcher( repositoryConfig.getLocation() + "/.indexer" );
        TopDocs topDocs = searcher.search( q, null, 10 );
        
        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertTrue( new File( repositoryConfig.getLocation(), ".index" ).exists() );
        
        // should only return 1 hit!
        assertEquals( 1, topDocs.totalHits );
    }
    
    public void testRemoveArtifactFromIndex()
        throws Exception
    {
        File artifactFile =
            new File( repositoryConfig.getLocation(),
                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ArtifactIndexingTask task =
            TaskCreator.createIndexingTask( repositoryConfig.getId(), artifactFile, ArtifactIndexingTask.ADD );
        
        archivaConfigControl.expectAndReturn( archivaConfiguration.getConfiguration(), configuration, 2 );
        
        archivaConfigControl.replay();
        
        // add artifact to index
        indexingExecutor.executeTask( task );
        
        BooleanQuery q = new BooleanQuery();        
        q.add( indexer.constructQuery( ArtifactInfo.GROUP_ID, "org.apache.archiva" ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.ARTIFACT_ID, "archiva-index-methods-jar-test" ), Occur.SHOULD );
        
        IndexSearcher searcher = new IndexSearcher( repositoryConfig.getLocation() + "/.indexer" );
        TopDocs topDocs = searcher.search( q, null, 10 );
        
        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertTrue( new File( repositoryConfig.getLocation(), ".index" ).exists() );
        
        // should return 1 hit
        assertEquals( 1, topDocs.totalHits );
        
        // remove added artifact from index
        task =
            TaskCreator.createIndexingTask( repositoryConfig.getId(), artifactFile, ArtifactIndexingTask.DELETE );
        indexingExecutor.executeTask( task );
        
        archivaConfigControl.verify();
        
        q = new BooleanQuery();        
        q.add( indexer.constructQuery( ArtifactInfo.GROUP_ID, "org.apache.archiva" ), Occur.SHOULD );
        q.add( indexer.constructQuery( ArtifactInfo.ARTIFACT_ID, "archiva-index-methods-jar-test" ), Occur.SHOULD );
        
        searcher = new IndexSearcher( repositoryConfig.getLocation() + "/.indexer" );
        topDocs = searcher.search( q, null, 10 );
        
        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertTrue( new File( repositoryConfig.getLocation(), ".index" ).exists() );
        
        // artifact should have been removed from the index!
        assertEquals( 0, topDocs.totalHits );
    }
    
}
