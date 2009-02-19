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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactContextProducer;
import org.sonatype.nexus.index.DefaultArtifactContextProducer;
import org.sonatype.nexus.index.NexusIndexer;
import org.sonatype.nexus.index.context.DefaultIndexingContext;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.creator.IndexerEngine;

public class LuceneCleanupRemoveIndexedConsumerTest
    extends PlexusInSpringTestCase
{
    private LuceneCleanupRemoveIndexedConsumer consumer;
    
    private MockControl indexerControl;
    
    private NexusIndexer indexer;
    
    private RepositoryContentFactory repoFactory;
    
    private MockControl repoFactoryControl;
    
    private ManagedRepositoryConfiguration repositoryConfig;
    
    private ArtifactContextProducer artifactContextProducer;
    
    private IndexerEngine indexerEngine;
    
    public void setUp()
        throws Exception 
    {
        super.setUp();
        
        indexerControl = MockControl.createControl( NexusIndexer.class );
        indexerControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        indexer = ( NexusIndexer ) indexerControl.getMock();
        
        repoFactoryControl = MockClassControl.createControl( RepositoryContentFactory.class );
        repoFactory = ( RepositoryContentFactory ) repoFactoryControl.getMock();
        
        indexerEngine = ( IndexerEngine ) lookup( IndexerEngine.class );
        
        consumer = new LuceneCleanupRemoveIndexedConsumer( repoFactory, indexer, indexerEngine );
        
        repositoryConfig = new ManagedRepositoryConfiguration();
        repositoryConfig.setId( "test-repo" );
        repositoryConfig.setLocation( getBasedir() + "/target/test-classes/test-repo" );
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setName( "Test Repository" );
        repositoryConfig.setScanned( true );
        repositoryConfig.setSnapshots( false );
        repositoryConfig.setReleases( true ); 
        repositoryConfig.setIndexDir( getBasedir() + "/target/test-classes/test-repo/.cleanup-index" );
        
        artifactContextProducer = new DefaultArtifactContextProducer();
    }
    
    public void tearDown() 
        throws Exception
    {
        FileUtils.deleteDirectory( new File( repositoryConfig.getIndexDir() ) );
        
        super.tearDown();
    }
    
    public void testProcessArtifactArtifactDoesNotExist()
        throws Exception
    {
        /*ArchivaArtifact artifact =
            new ArchivaArtifact( "org.apache.archiva", "archiva-lucene-consumers", "1.2", null, "jar", "test-repo" );
        ManagedRepositoryContent repoContent = new ManagedDefaultRepositoryContent();
        repoContent.setRepository( repositoryConfig );
        
        IndexingContext context =
            new DefaultIndexingContext( repositoryConfig.getId(), repositoryConfig.getId(),
                                        new File( repositoryConfig.getLocation() ),
                                        new File( repositoryConfig.getIndexDir() ), null, null,
                                        NexusIndexer.FULL_INDEX, false );

        File artifactFile =
            new File( repositoryConfig.getLocation(),
                      "org/apache/archiva/archiva-lucene-consumers/1.2/archiva-lucene-consumers-1.2.jar" );
        ArtifactContext artifactContext = artifactContextProducer.getArtifactContext( context, artifactFile );
        
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( repositoryConfig.getId() ), repoContent );
        indexerControl.expectAndReturn( indexer.addIndexingContext( repositoryConfig.getId(), repositoryConfig.getId(),
                                        new File( repositoryConfig.getLocation() ),
                                        new File( repositoryConfig.getIndexDir() ), null, null,
                                        NexusIndexer.FULL_INDEX ), context );
        indexer.deleteArtifactFromIndex( artifactContext, context );
        indexerControl.setVoidCallable();
        
        repoFactoryControl.replay();
        indexerControl.replay();
        
        consumer.processArchivaArtifact( artifact );
        
        repoFactoryControl.verify();
        indexerControl.verify();    */    
    }    
    
    public void testProcessArtifactArtifactExists()
        throws Exception
    {
        /*ArchivaArtifact artifact =
            new ArchivaArtifact( "org.apache.maven.archiva", "archiva-lucene-cleanup", "1.0", null, "jar", "test-repo" );
        ManagedRepositoryContent repoContent = new ManagedDefaultRepositoryContent();
        repoContent.setRepository( repositoryConfig );
        
        IndexingContext context =
            new DefaultIndexingContext( repositoryConfig.getId(), repositoryConfig.getId(),
                                        new File( repositoryConfig.getLocation() ),
                                        new File( repositoryConfig.getIndexDir() ), null, null,
                                        NexusIndexer.FULL_INDEX, false );
        
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( repositoryConfig.getId() ), repoContent );
        indexerControl.expectAndReturn( indexer.addIndexingContext( repositoryConfig.getId(), repositoryConfig.getId(),
                                        new File( repositoryConfig.getLocation() ),
                                        new File( repositoryConfig.getIndexDir() ), null, null,
                                        NexusIndexer.FULL_INDEX ), context );
        
        repoFactoryControl.replay();
        indexerControl.replay();
        
        consumer.processArchivaArtifact( artifact );
        
        repoFactoryControl.verify();
        indexerControl.verify();        */
    }    
}
