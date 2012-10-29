package org.apache.archiva.scheduler.repository;

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

import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.stats.RepositoryStatistics;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * ArchivaRepositoryScanningTaskExecutorPhase2Test
 *
 *
 */

@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class ArchivaRepositoryScanningTaskExecutorPhase2Test
    extends ArchivaRepositoryScanningTaskExecutorAbstractTest
{

    @Test
    public void testExecutorScanOnlyNewArtifacts()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );
        repoTask.setScanAll( false );

        createAndSaveTestStats();

        taskExecutor.executeTask( repoTask );

        // check no artifacts processed
        Collection<ArtifactReference> unprocessedResultList = testConsumer.getConsumed();

        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected. No new artifacts should have been found.", 0,
                      unprocessedResultList.size() );

        // check correctness of new stats
        RepositoryStatistics newStats =
            repositoryStatisticsManager.getLastStatistics( metadataRepository, TEST_REPO_ID );
        assertEquals( 0, newStats.getNewFileCount() );
        assertEquals( 31, newStats.getTotalFileCount() );
        // FIXME: can't test these as they weren't stored in the database, move to tests for RepositoryStatisticsManager implementation
//        assertEquals( 8, newStats.getTotalArtifactCount() );
//        assertEquals( 3, newStats.getTotalGroupCount() );
//        assertEquals( 5, newStats.getTotalProjectCount() );
//        assertEquals( 14159, newStats.getTotalArtifactFileSize() );

        File newArtifactGroup = new File( repoDir, "org/apache/archiva" );
        assertFalse( "newArtifactGroup should not exist.", newArtifactGroup.exists() );

        FileUtils.copyDirectoryStructure( new File( "target/test-classes/test-repo/org/apache/archiva" ),
                                          newArtifactGroup );

        // update last modified date
        new File( newArtifactGroup, "archiva-index-methods-jar-test/1.0/pom.xml" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() + 1000 );
        new File( newArtifactGroup,
                  "archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() + 1000 );

        assertTrue( newArtifactGroup.exists() );

        taskExecutor.executeTask( repoTask );

        unprocessedResultList = testConsumer.getConsumed();
        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected. One new artifact should have been found.", 1,
                      unprocessedResultList.size() );

        // check correctness of new stats
        RepositoryStatistics updatedStats =
            repositoryStatisticsManager.getLastStatistics( metadataRepository, TEST_REPO_ID );
        assertEquals( 2, updatedStats.getNewFileCount() );
        assertEquals( 33, updatedStats.getTotalFileCount() );
        // FIXME: can't test these as they weren't stored in the database, move to tests for RepositoryStatisticsManager implementation
//        assertEquals( 8, newStats.getTotalArtifactCount() );
//        assertEquals( 3, newStats.getTotalGroupCount() );
//        assertEquals( 5, newStats.getTotalProjectCount() );
//        assertEquals( 19301, updatedStats.getTotalArtifactFileSize() );
    }

    @Test
    public void testExecutorScanOnlyNewArtifactsChangeTimes()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );
        repoTask.setScanAll( false );

        createAndSaveTestStats();

        File newArtifactGroup = new File( repoDir, "org/apache/archiva" );
        assertFalse( "newArtifactGroup should not exist.", newArtifactGroup.exists() );

        FileUtils.copyDirectoryStructure( new File( "target/test-classes/test-repo/org/apache/archiva" ),
                                          newArtifactGroup );

        // update last modified date, placing shortly after last scan
        new File( newArtifactGroup, "archiva-index-methods-jar-test/1.0/pom.xml" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() + 1000 );
        new File( newArtifactGroup,
                  "archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() + 1000 );

        assertTrue( newArtifactGroup.exists() );

        // scan using the really long previous duration
        taskExecutor.executeTask( repoTask );

        // check no artifacts processed
        Collection<ArtifactReference> unprocessedResultList = testConsumer.getConsumed();
        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected. One new artifact should have been found.", 1,
                      unprocessedResultList.size() );

        // check correctness of new stats
        RepositoryStatistics newStats =
            repositoryStatisticsManager.getLastStatistics( metadataRepository, TEST_REPO_ID );
        assertEquals( 2, newStats.getNewFileCount() );
        assertEquals( 33, newStats.getTotalFileCount() );
        // FIXME: can't test these as they weren't stored in the database, move to tests for RepositoryStatisticsManager implementation
//        assertEquals( 8, newStats.getTotalArtifactCount() );
//        assertEquals( 3, newStats.getTotalGroupCount() );
//        assertEquals( 5, newStats.getTotalProjectCount() );
//        assertEquals( 19301, newStats.getTotalArtifactFileSize() );
    }

    @Test
    public void testExecutorScanOnlyNewArtifactsMidScan()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );
        repoTask.setScanAll( false );

        createAndSaveTestStats();

        File newArtifactGroup = new File( repoDir, "org/apache/archiva" );
        assertFalse( "newArtifactGroup should not exist.", newArtifactGroup.exists() );

        FileUtils.copyDirectoryStructure( new File( "target/test-classes/test-repo/org/apache/archiva" ),
                                          newArtifactGroup );

        // update last modified date, placing in middle of last scan
        new File( newArtifactGroup, "archiva-index-methods-jar-test/1.0/pom.xml" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() - 50000 );
        new File( newArtifactGroup,
                  "archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" ).setLastModified(
            Calendar.getInstance().getTimeInMillis() - 50000 );

        assertTrue( newArtifactGroup.exists() );

        // scan using the really long previous duration
        taskExecutor.executeTask( repoTask );

        // check no artifacts processed
        Collection<ArtifactReference> unprocessedResultList = testConsumer.getConsumed();
        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected. One new artifact should have been found.", 1,
                      unprocessedResultList.size() );

        // check correctness of new stats
        RepositoryStatistics newStats =
            repositoryStatisticsManager.getLastStatistics( metadataRepository, TEST_REPO_ID );
        assertEquals( 2, newStats.getNewFileCount() );
        assertEquals( 33, newStats.getTotalFileCount() );
        // FIXME: can't test these as they weren't stored in the database, move to tests for RepositoryStatisticsManager implementation
//        assertEquals( 8, newStats.getTotalArtifactCount() );
//        assertEquals( 3, newStats.getTotalGroupCount() );
//        assertEquals( 5, newStats.getTotalProjectCount() );
//        assertEquals( 19301, newStats.getTotalArtifactFileSize() );
    }

    @Test
    public void testExecutorForceScanAll()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );
        repoTask.setScanAll( true );

        Date date = Calendar.getInstance().getTime();
        repositoryStatisticsManager.addStatisticsAfterScan( metadataRepository, TEST_REPO_ID,
                                                            new Date( date.getTime() - 1234567 ), date, 8, 8 );

        taskExecutor.executeTask( repoTask );

        Collection<ArtifactReference> unprocessedResultList = testConsumer.getConsumed();

        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected.", 8, unprocessedResultList.size() );
    }

    private void createAndSaveTestStats()
        throws MetadataRepositoryException
    {
        Date date = Calendar.getInstance().getTime();
        RepositoryStatistics stats = new RepositoryStatistics();
        stats.setScanStartTime( new Date( date.getTime() - 1234567 ) );
        stats.setScanEndTime( date );
        stats.setNewFileCount( 31 );
        stats.setTotalArtifactCount( 8 );
        stats.setTotalFileCount( 31 );
        stats.setTotalGroupCount( 3 );
        stats.setTotalProjectCount( 5 );
        stats.setTotalArtifactFileSize( 38545 );

        repositoryStatisticsManager.addStatisticsAfterScan( metadataRepository, TEST_REPO_ID,
                                                            new Date( date.getTime() - 1234567 ), date, 31, 31 );
    }
}
