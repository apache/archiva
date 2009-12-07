package org.apache.archiva.metadata.repository.stats;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import junit.framework.TestCase;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.easymock.MockControl;

public class RepositoryStatisticsManagerTest
    extends TestCase
{
    private DefaultRepositoryStatisticsManager repositoryStatisticsManager;

    private static final String TEST_REPO_ID = "test-repo";

    private MockControl metadataRepositoryControl;

    private MetadataRepository metadataRepository;

    private static final String FIRST_TEST_SCAN = "20091201.123456.789";

    private static final String SECOND_TEST_SCAN = "20091202.012345.678";

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        repositoryStatisticsManager = new DefaultRepositoryStatisticsManager();

        metadataRepositoryControl = MockControl.createControl( MetadataRepository.class );
        metadataRepository = (MetadataRepository) metadataRepositoryControl.getMock();
        repositoryStatisticsManager.setMetadataRepository( metadataRepository );
    }

    public void testGetLatestStats()
        throws ParseException
    {
        Date endTime =
            new Date( DefaultRepositoryStatisticsManager.SCAN_TIMESTAMP.parse( SECOND_TEST_SCAN ).getTime() + 60000 );

        RepositoryStatistics stats = new RepositoryStatistics();
        stats.setScanStartTime( DefaultRepositoryStatisticsManager.SCAN_TIMESTAMP.parse( SECOND_TEST_SCAN ) );
        stats.setScanEndTime( endTime );
        stats.setTotalArtifactFileSize( 1314527915L );
        stats.setNewFileCount( 123 );
        stats.setTotalArtifactCount( 10386 );
        stats.setTotalProjectCount( 2031 );
        stats.setTotalGroupCount( 529 );
        stats.setTotalFileCount( 56229 );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ),
            Arrays.asList( FIRST_TEST_SCAN, SECOND_TEST_SCAN ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, SECOND_TEST_SCAN ),
            stats );
        metadataRepositoryControl.replay();

        stats = repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID );
        assertNotNull( stats );
        assertEquals( 1314527915L, stats.getTotalArtifactFileSize() );
        assertEquals( 123, stats.getNewFileCount() );
        assertEquals( 10386, stats.getTotalArtifactCount() );
        assertEquals( 2031, stats.getTotalProjectCount() );
        assertEquals( 529, stats.getTotalGroupCount() );
        assertEquals( 56229, stats.getTotalFileCount() );
        assertEquals( SECOND_TEST_SCAN,
                      DefaultRepositoryStatisticsManager.SCAN_TIMESTAMP.format( stats.getScanStartTime() ) );
        assertEquals( endTime, stats.getScanEndTime() );

        metadataRepositoryControl.verify();
    }

    public void testGetLatestStatsWhenEmpty()
    {
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ),
            Collections.emptyList() );
        metadataRepositoryControl.replay();

        RepositoryStatistics stats = repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID );
        assertNull( stats );

        metadataRepositoryControl.verify();
    }

    public void testAddNewStats()
    {
        Date current = new Date();
        Date startTime = new Date( current.getTime() - 12345 );

        RepositoryStatistics stats = new RepositoryStatistics();
        stats.setScanStartTime( startTime );
        stats.setScanEndTime( current );
        stats.setTotalArtifactFileSize( 1400032000L );
        stats.setNewFileCount( 45 );
        stats.setTotalArtifactCount( 10412 );
        stats.setTotalProjectCount( 2036 );
        stats.setTotalGroupCount( 531 );
        stats.setTotalFileCount( 56345 );

        String startTimeAsString = DefaultRepositoryStatisticsManager.SCAN_TIMESTAMP.format( startTime );
        metadataRepository.addMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, startTimeAsString, stats );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ),
            Arrays.asList( startTimeAsString ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, startTimeAsString ),
            stats );

        metadataRepositoryControl.replay();

        repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats );

        stats = repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID );
        assertNotNull( stats );
        assertEquals( 1400032000L, stats.getTotalArtifactFileSize() );
        assertEquals( 45, stats.getNewFileCount() );
        assertEquals( 10412, stats.getTotalArtifactCount() );
        assertEquals( 2036, stats.getTotalProjectCount() );
        assertEquals( 531, stats.getTotalGroupCount() );
        assertEquals( 56345, stats.getTotalFileCount() );
        assertEquals( current.getTime() - 12345, stats.getScanStartTime().getTime() );
        assertEquals( current, stats.getScanEndTime() );

        metadataRepositoryControl.verify();
    }
}
