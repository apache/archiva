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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.apache.maven.archiva.scheduled.ArchivaTaskScheduler;
import org.apache.maven.archiva.scheduled.tasks.ArtifactIndexingTask;
import org.apache.maven.archiva.scheduled.tasks.TaskCreator;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.codehaus.plexus.taskqueue.TaskQueue;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

/**
 * LuceneCleanupRemoveIndexedConsumerTest
 */
public class LuceneCleanupRemoveIndexedConsumerTest
    extends PlexusInSpringTestCase
{
    private LuceneCleanupRemoveIndexedConsumer consumer;

    private RepositoryContentFactory repoFactory;

    private MockControl repoFactoryControl;

    private ManagedRepositoryConfiguration repositoryConfig;
    
    private TaskQueue indexingQueue;

    public void setUp()
        throws Exception
    {
        super.setUp();

        repoFactoryControl = MockClassControl.createControl( RepositoryContentFactory.class );
        repoFactory = (RepositoryContentFactory) repoFactoryControl.getMock();

        ArchivaTaskScheduler scheduler = ( ArchivaTaskScheduler ) lookup( ArchivaTaskScheduler.class );
        
        indexingQueue = (TaskQueue) lookup( TaskQueue.ROLE, "indexing" );
        
        consumer = new LuceneCleanupRemoveIndexedConsumer( repoFactory, scheduler );

        repositoryConfig = new ManagedRepositoryConfiguration();
        repositoryConfig.setId( "test-repo" );
        repositoryConfig.setLocation( getBasedir() + "/target/test-classes/test-repo" );
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setName( "Test Repository" );
        repositoryConfig.setScanned( true );
        repositoryConfig.setSnapshots( false );
        repositoryConfig.setReleases( true );
        repositoryConfig.setIndexDir( getBasedir() + "/target/test-classes/test-repo/.cleanup-index" );
    }

    public void tearDown()
        throws Exception
    {
        FileUtils.deleteDirectory( new File( repositoryConfig.getIndexDir() ) );

        super.tearDown();
    }

    @SuppressWarnings( "unchecked" )
    public void testProcessArtifactArtifactDoesNotExist()
        throws Exception
    {
        assertTrue( indexingQueue.getQueueSnapshot().isEmpty() );

        ArchivaArtifact artifact =
            new ArchivaArtifact( "org.apache.archiva", "archiva-lucene-consumers", "1.2", null, "jar", "test-repo" );
        
        ManagedRepositoryContent repoContent = new ManagedDefaultRepositoryContent();
        repoContent.setRepository( repositoryConfig );
        
        File artifactFile = new File( repoContent.getRepoRoot(), repoContent.toPath( artifact ) );
       
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( repositoryConfig.getId() ),
                                            repoContent );
       
        repoFactoryControl.replay();      

        consumer.processArchivaArtifact( artifact );

        repoFactoryControl.verify();       
        
        List<ArtifactIndexingTask> queue = indexingQueue.getQueueSnapshot();
        assertEquals( 2, queue.size() );
        ArtifactIndexingTask task =
            TaskCreator.createIndexingTask( repositoryConfig.getId(), artifactFile, ArtifactIndexingTask.Action.DELETE );
        assertEquals( task, queue.get( 0 ) );
        task =
            TaskCreator.createIndexingTask( repositoryConfig.getId(), artifactFile, ArtifactIndexingTask.Action.FINISH );
        assertEquals( task, queue.get( 1 ) );
    }

    public void testProcessArtifactArtifactExists()
        throws Exception
    {
        assertTrue( indexingQueue.getQueueSnapshot().isEmpty() );

        ArchivaArtifact artifact =
            new ArchivaArtifact( "org.apache.maven.archiva", "archiva-lucene-cleanup", "1.0", null, "jar", "test-repo" );
        ManagedRepositoryContent repoContent = new ManagedDefaultRepositoryContent();
        repoContent.setRepository( repositoryConfig );

        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( repositoryConfig.getId() ),
                                            repoContent );

        repoFactoryControl.replay();

        consumer.processArchivaArtifact( artifact );

        repoFactoryControl.verify();
        
        assertTrue( indexingQueue.getQueueSnapshot().isEmpty() );
    }
    
    @Override
    protected String getPlexusConfigLocation()
    {
        return "/org/apache/archiva/consumers/lucene/LuceneConsumersTest.xml";
    }
}
