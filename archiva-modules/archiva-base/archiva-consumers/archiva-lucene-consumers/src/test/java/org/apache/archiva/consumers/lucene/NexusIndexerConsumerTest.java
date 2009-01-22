package org.apache.archiva.consumers.lucene;

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
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.FlatSearchRequest;
import org.sonatype.nexus.index.FlatSearchResponse;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.creator.IndexerEngine;
import org.sonatype.nexus.index.packer.IndexPacker;

public class NexusIndexerConsumerTest
    extends PlexusInSpringTestCase
{
    private KnownRepositoryContentConsumer nexusIndexerConsumer;
        
    private ManagedRepositoryConfiguration repositoryConfig;

    private NexusIndexer nexusIndexer;

    private IndexPacker indexPacker;

    private IndexerEngine indexerEngine;
    
    @Override
    protected void setUp() 
        throws Exception
    {
        super.setUp();
        
        nexusIndexer = ( NexusIndexer ) lookup( NexusIndexer.class );
        
        indexPacker = ( IndexPacker ) lookup( IndexPacker.class );
        
        indexerEngine = ( IndexerEngine ) lookup( IndexerEngine.class );
        
        nexusIndexerConsumer = new NexusIndexerConsumer( nexusIndexer, indexPacker, indexerEngine );
                
        repositoryConfig = new ManagedRepositoryConfiguration();
        repositoryConfig.setId( "test-repo" );
        repositoryConfig.setLocation( getBasedir() + "/target/test-classes/test-repo" );
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setName( "Test Repository" );
        repositoryConfig.setScanned( true );
        repositoryConfig.setSnapshots( false );
        repositoryConfig.setReleases( true );
    }
    
    @Override
    protected void tearDown()
        throws Exception
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
    
    public void testIndexerIndexArtifact()
        throws Exception
    {        
        // begin scan
        Date now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        
        // process file
        nexusIndexerConsumer.processFile( "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );
        
        // end scan
        nexusIndexerConsumer.completeScan();
        
        // search!
        BooleanQuery q = new BooleanQuery();        
        q.add( nexusIndexer.constructQuery( ArtifactInfo.GROUP_ID, "org.apache.archiva" ), Occur.SHOULD );
        q.add( nexusIndexer.constructQuery( ArtifactInfo.ARTIFACT_ID, "archiva-index-methods-jar-test" ), Occur.SHOULD );
        
        FlatSearchRequest request = new FlatSearchRequest( q );
        FlatSearchResponse response = nexusIndexer.searchFlat( request );
        
        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertTrue( new File( repositoryConfig.getLocation(), ".index" ).exists() );
        assertEquals( 1, response.getTotalHits() );
        
        Set<ArtifactInfo> results = response.getResults();
        
        ArtifactInfo artifactInfo = (ArtifactInfo) results.iterator().next();
        assertEquals( "org.apache.archiva", artifactInfo.groupId );
        assertEquals( "archiva-index-methods-jar-test", artifactInfo.artifactId );
        assertEquals( "test-repo", artifactInfo.repository );        
    }
    
    /*public void testIndexerIndexPom()
        throws Exception
    {        
        // begin scan
        Date now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        
        // process file
        //nexusIndexerConsumer.processFile(  )
        
        // end scan
        
        // search!
    }*/
}
