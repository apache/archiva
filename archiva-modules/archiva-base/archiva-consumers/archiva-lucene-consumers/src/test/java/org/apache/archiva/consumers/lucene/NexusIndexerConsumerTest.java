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
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.indexing.ArtifactIndexingTask;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;

/**
 * NexusIndexerConsumerTest
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml","classpath*:/spring-context.xml"} )
public class NexusIndexerConsumerTest
    extends TestCase
{
    private final class ArchivaTaskSchedulerStub
        implements ArchivaTaskScheduler<ArtifactIndexingTask>
    {
        Set<File> indexed = new HashSet<File>();
        
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

    private KnownRepositoryContentConsumer nexusIndexerConsumer;

    private ManagedRepositoryConfiguration repositoryConfig;

    private ArchivaTaskSchedulerStub scheduler;

    @Inject
    private ApplicationContext applicationContext;


    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        scheduler = new ArchivaTaskSchedulerStub();

        ArchivaConfiguration configuration = applicationContext.getBean( ArchivaConfiguration.class );
        
        FileTypes filetypes = applicationContext.getBean( FileTypes.class );

        nexusIndexerConsumer = new NexusIndexerConsumer( scheduler, configuration, filetypes );
        
        // initialize to set the file types to be processed
        ( (Initializable) nexusIndexerConsumer ).initialize();

        repositoryConfig = new ManagedRepositoryConfiguration();
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
        File indexDir = new File( repositoryConfig.getLocation(), ".indexer" );
        FileUtils.deleteDirectory( indexDir );
        assertFalse( indexDir.exists() );

        indexDir = new File( repositoryConfig.getLocation(), ".index" );
        FileUtils.deleteDirectory( indexDir );
        assertFalse( indexDir.exists() );

        super.tearDown();
    }

    @Test
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

        assertTrue( scheduler.indexed.contains( artifactFile ) );
    }

    @Test
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

        assertTrue( scheduler.indexed.contains( artifactFile ) );

        // scan and index again
        now = Calendar.getInstance().getTime();
        nexusIndexerConsumer.beginScan( repositoryConfig, now );
        nexusIndexerConsumer.processFile( "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );
        nexusIndexerConsumer.completeScan();

        assertTrue( scheduler.indexed.contains( artifactFile ) );
    }

    @Test
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

        assertTrue( scheduler.indexed.contains( artifactFile ) );

        artifactFile =
            new File( repositoryConfig.getLocation(), "org/apache/archiva/archiva-index-methods-jar-test/1.0/pom.xml" );

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
        List<String> includes =  nexusIndexerConsumer.getIncludes();
        assertTrue( ".pom artifacts should be processed.", includes.contains( "**/*.pom" ) );
        assertTrue( ".xml artifacts should be processed.", includes.contains( "**/*.xml" ) );
        assertTrue( ".txt artifacts should be processed.", includes.contains( "**/*.txt" ) );
        assertTrue( ".jar artifacts should be processed.", includes.contains( "**/*.jar" ) );
        assertTrue( ".war artifacts should be processed.", includes.contains( "**/*.war" ) );
        assertTrue( ".zip artifacts should be processed.", includes.contains( "**/*.zip" ) );
    }

}
