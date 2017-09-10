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

import junit.framework.TestCase;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.redback.components.taskqueue.TaskQueueException;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.indexing.ArtifactIndexingTask;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexCreator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NexusIndexerConsumerTest
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class NexusIndexerConsumerTest
    extends TestCase
{
    private final class ArchivaTaskSchedulerStub
        implements ArchivaTaskScheduler<ArtifactIndexingTask>
    {
        Set<Path> indexed = new HashSet<>();

        @Override
        public void queueTask( ArtifactIndexingTask task )
            throws TaskQueueException
        {
            switch ( task.getAction() )
            {
                case ADD:
                    indexed.add( task.getResourceFile() );
                    break;
                case DELETE:
                    indexed.remove( task.getResourceFile() );
                    break;
                case FINISH:
                    try
                    {
                        task.getContext().close( false );
                    }
                    catch ( IOException e )
                    {
                        throw new TaskQueueException( e.getMessage() );
                    }
                    break;
            }
        }
    }

    private NexusIndexerConsumer nexusIndexerConsumer;

    private ManagedRepository repositoryConfig;

    private ArchivaTaskSchedulerStub scheduler;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private NexusIndexer nexusIndexer;

    @Inject
    private List<IndexCreator> indexCreators;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;


    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        scheduler = new ArchivaTaskSchedulerStub();

        ArchivaConfiguration configuration = applicationContext.getBean( ArchivaConfiguration.class );

        FileTypes filetypes = applicationContext.getBean( FileTypes.class );

        nexusIndexerConsumer =
            new NexusIndexerConsumer( scheduler, configuration, filetypes, indexCreators,
                                      managedRepositoryAdmin, nexusIndexer );

        // initialize to set the file types to be processed
        nexusIndexerConsumer.initialize();

        repositoryConfig = new ManagedRepository();
        repositoryConfig.setId( "test-repo" );
        repositoryConfig.setLocation( "target/test-classes/test-repo" );
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setName( "Test Repository" );
        repositoryConfig.setScanned( true );
        repositoryConfig.setSnapshots( false );
        repositoryConfig.setReleases( true );
    }

    @Override
    @After
    public void tearDown()
        throws Exception
    {
        // delete created index in the repository
        Path indexDir = Paths.get( repositoryConfig.getLocation(), ".indexer" );
        org.apache.archiva.common.utils.FileUtils.deleteDirectory( indexDir );
        assertFalse( Files.exists(indexDir) );

        indexDir = Paths.get( repositoryConfig.getLocation(), ".index" );
        org.apache.archiva.common.utils.FileUtils.deleteDirectory( indexDir );
        assertFalse( Files.exists(indexDir) );

        super.tearDown();
    }

    @Test
    public void testIndexerIndexArtifact()
        throws Exception
    {
        Path artifactFile = Paths.get( repositoryConfig.getLocation(),
                                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        // begin scan
        Date now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        nexusIndexerConsumer.processFile(
            "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );
        nexusIndexerConsumer.completeScan();

        assertTrue( scheduler.indexed.contains( artifactFile ) );
    }

    @Test
    public void testIndexerArtifactAlreadyIndexed()
        throws Exception
    {
        Path artifactFile = Paths.get( repositoryConfig.getLocation(),
                                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        // begin scan
        Date now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        nexusIndexerConsumer.processFile(
            "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );
        nexusIndexerConsumer.completeScan();

        assertTrue( scheduler.indexed.contains( artifactFile ) );

        // scan and index again
        now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        nexusIndexerConsumer.processFile(
            "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );
        nexusIndexerConsumer.completeScan();

        assertTrue( scheduler.indexed.contains( artifactFile ) );
    }

    @Test
    public void testIndexerIndexArtifactThenPom()
        throws Exception
    {
        Path artifactFile = Paths.get( repositoryConfig.getLocation(),
                                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        // begin scan
        Date now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        nexusIndexerConsumer.processFile(
            "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );
        nexusIndexerConsumer.completeScan();

        assertTrue( scheduler.indexed.contains( artifactFile ) );

        artifactFile =
            Paths.get( repositoryConfig.getLocation(), "org/apache/archiva/archiva-index-methods-jar-test/1.0/pom.xml" );

        // scan and index again
        now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        nexusIndexerConsumer.processFile( "org/apache/archiva/archiva-index-methods-jar-test/1.0/pom.xml" );
        nexusIndexerConsumer.completeScan();

        assertTrue( scheduler.indexed.contains( artifactFile ) );
    }

    // MRM-1275 - Include other file types for the index consumer instead of just the indexable-content
    @Test
    public void testIncludedFileTypes()
        throws Exception
    {
        List<String> includes = nexusIndexerConsumer.getIncludes();
        assertTrue( ".pom artifacts should be processed.", includes.contains( "**/*.pom" ) );
        assertTrue( ".xml artifacts should be processed.", includes.contains( "**/*.xml" ) );
        assertTrue( ".txt artifacts should be processed.", includes.contains( "**/*.txt" ) );
        assertTrue( ".jar artifacts should be processed.", includes.contains( "**/*.jar" ) );
        assertTrue( ".war artifacts should be processed.", includes.contains( "**/*.war" ) );
        assertTrue( ".zip artifacts should be processed.", includes.contains( "**/*.zip" ) );
    }

}
