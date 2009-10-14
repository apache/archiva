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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.scheduled.ArchivaTaskScheduler;
import org.apache.maven.archiva.scheduled.tasks.ArtifactIndexingTask;
import org.apache.maven.archiva.scheduled.tasks.TaskCreator;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.codehaus.plexus.taskqueue.TaskQueue;
import org.codehaus.plexus.taskqueue.TaskQueueException;

/**
 * NexusIndexerConsumerTest
 */
public class NexusIndexerConsumerTest
    extends PlexusInSpringTestCase
{
    private KnownRepositoryContentConsumer nexusIndexerConsumer;

    private ManagedRepositoryConfiguration repositoryConfig;

    private TaskQueue indexingQueue;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaTaskScheduler scheduler = (ArchivaTaskScheduler) lookup( ArchivaTaskScheduler.class );

        indexingQueue = (TaskQueue) lookup( TaskQueue.ROLE, "indexing" );

        nexusIndexerConsumer = new NexusIndexerConsumer( scheduler );

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
        File artifactFile =
            new File( repositoryConfig.getLocation(),
                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        // begin scan
        Date now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        nexusIndexerConsumer.processFile( "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );
        nexusIndexerConsumer.completeScan();

        assertTaskQueued( artifactFile );
    }

    @SuppressWarnings( "unchecked" )
    private void assertTaskQueued( File artifactFile )
        throws TaskQueueException
    {
        List<ArtifactIndexingTask> queue = indexingQueue.getQueueSnapshot();
        assertEquals( 2, queue.size() );
        ArtifactIndexingTask task =
            TaskCreator.createIndexingTask( repositoryConfig.getId(), artifactFile, ArtifactIndexingTask.Action.ADD );
        assertEquals( task, queue.get( 0 ) );

        task = TaskCreator.createIndexingTask( repositoryConfig.getId(), null, ArtifactIndexingTask.Action.FINISH );
        assertEquals( task, queue.get( 1 ) );
    }

    public void testIndexerArtifactAlreadyIndexed()
        throws Exception
    {
        File artifactFile =
            new File( repositoryConfig.getLocation(),
                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        // begin scan
        Date now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        nexusIndexerConsumer.processFile( "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );
        nexusIndexerConsumer.completeScan();

        assertTaskQueued( artifactFile );
        indexingQueue.take();
        indexingQueue.take();

        // scan and index again
        now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        nexusIndexerConsumer.processFile( "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );
        nexusIndexerConsumer.completeScan();

        assertTaskQueued( artifactFile );
    }

    public void testIndexerIndexArtifactThenPom()
        throws Exception
    {
        File artifactFile =
            new File( repositoryConfig.getLocation(),
                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        // begin scan
        Date now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        nexusIndexerConsumer.processFile( "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );
        nexusIndexerConsumer.completeScan();

        assertTaskQueued( artifactFile );
        indexingQueue.take();
        indexingQueue.take();

        artifactFile =
            new File( repositoryConfig.getLocation(), "org/apache/archiva/archiva-index-methods-jar-test/1.0/pom.xml" );

        // scan and index again
        now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        nexusIndexerConsumer.processFile( "org/apache/archiva/archiva-index-methods-jar-test/1.0/pom.xml" );
        nexusIndexerConsumer.completeScan();

        assertTaskQueued( artifactFile );
    }

    @Override
    protected String getPlexusConfigLocation()
    {
        return "/org/apache/archiva/consumers/lucene/LuceneConsumersTest.xml";
    }
}
