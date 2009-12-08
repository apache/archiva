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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private Map<String, RepositoryStatistics> statsCreated = new LinkedHashMap<String, RepositoryStatistics>();

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

        RepositoryStatistics stats1 = createTestStats( startTime, current );

        String startTimeAsString = DefaultRepositoryStatisticsManager.SCAN_TIMESTAMP.format( startTime );
        metadataRepository.addMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, startTimeAsString, stats1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ),
            Arrays.asList( startTimeAsString ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, startTimeAsString ),
            stats1 );
        RepositoryStatistics stats = stats1;

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

    public void testDeleteStats()
    {
        Date current = new Date();

        Date startTime1 = new Date( current.getTime() - 12345 );
        RepositoryStatistics stats1 = createTestStats( startTime1, new Date( current.getTime() - 6000 ) );
        String startTimeAsString1 = DefaultRepositoryStatisticsManager.SCAN_TIMESTAMP.format( startTime1 );
        metadataRepository.addMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, startTimeAsString1, stats1 );

        Date startTime2 = new Date( current.getTime() - 3000 );
        RepositoryStatistics stats2 = createTestStats( startTime2, current );
        String startTimeAsString2 = DefaultRepositoryStatisticsManager.SCAN_TIMESTAMP.format( startTime2 );
        metadataRepository.addMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, startTimeAsString2, stats2 );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ),
            Arrays.asList( startTimeAsString1, startTimeAsString2 ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, startTimeAsString2 ),
            stats2 );

        metadataRepository.removeMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ),
            Collections.emptyList() );

        metadataRepositoryControl.replay();

        repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats1 );
        repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats2 );

        assertNotNull( repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID ) );

        repositoryStatisticsManager.deleteStatistics( TEST_REPO_ID );

        assertNull( repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID ) );

        metadataRepositoryControl.verify();
    }

    public void testDeleteStatsWhenEmpty()
    {
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ),
            Collections.emptyList(), 2 );
        metadataRepository.removeMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID );

        metadataRepositoryControl.replay();

        assertNull( repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID ) );

        repositoryStatisticsManager.deleteStatistics( TEST_REPO_ID );

        assertNull( repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID ) );

        metadataRepositoryControl.verify();
    }

    public void testGetStatsRangeInside()
    {
        Date current = new Date();

        addStats( new Date( current.getTime() - 12345 ), new Date( current.getTime() - 6000 ) );
        addStats( new Date( current.getTime() - 3000 ), new Date( current.getTime() - 2000 ) );
        addStats( new Date( current.getTime() - 1000 ), current );

        ArrayList<String> keys = new ArrayList<String>( statsCreated.keySet() );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ), keys );

        // only match the middle one
        String key = keys.get( 1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, key ),
            statsCreated.get( key ) );

        metadataRepositoryControl.replay();

        for ( RepositoryStatistics stats : statsCreated.values() )
        {
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats );           
        }

        List<RepositoryStatistics> list =
            repositoryStatisticsManager.getStatisticsInRange( TEST_REPO_ID, new Date( current.getTime() - 4000 ),
                                                              new Date( current.getTime() - 2000 ) );

        assertEquals( 1, list.size() );
        assertEquals( new Date( current.getTime() - 3000 ), list.get( 0 ).getScanStartTime() );

        metadataRepositoryControl.verify();
    }

    public void testGetStatsRangeUpperOutside()
    {
        Date current = new Date();

        addStats( new Date( current.getTime() - 12345 ), new Date( current.getTime() - 6000 ) );
        addStats( new Date( current.getTime() - 3000 ), new Date( current.getTime() - 2000 ) );
        addStats( new Date( current.getTime() - 1000 ), current );

        ArrayList<String> keys = new ArrayList<String>( statsCreated.keySet() );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ), keys );

        String key = keys.get( 1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, key ),
            statsCreated.get( key ) );
        key = keys.get( 2 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, key ),
            statsCreated.get( key ) );

        metadataRepositoryControl.replay();

        for ( RepositoryStatistics stats : statsCreated.values() )
        {
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats );
        }

        List<RepositoryStatistics> list =
            repositoryStatisticsManager.getStatisticsInRange( TEST_REPO_ID, new Date( current.getTime() - 4000 ),
                                                              current );

        assertEquals( 2, list.size() );
        assertEquals( new Date( current.getTime() - 3000 ), list.get( 1 ).getScanStartTime() );
        assertEquals( new Date( current.getTime() - 1000 ), list.get( 0 ).getScanStartTime() );

        metadataRepositoryControl.verify();
    }

    public void testGetStatsRangeLowerOutside()
    {
        Date current = new Date();

        addStats( new Date( current.getTime() - 12345 ), new Date( current.getTime() - 6000 ) );
        addStats( new Date( current.getTime() - 3000 ), new Date( current.getTime() - 2000 ) );
        addStats( new Date( current.getTime() - 1000 ), current );

        ArrayList<String> keys = new ArrayList<String>( statsCreated.keySet() );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ), keys );

        String key = keys.get( 0 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, key ),
            statsCreated.get( key ) );
        key = keys.get( 1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, key ),
            statsCreated.get( key ) );

        metadataRepositoryControl.replay();

        for ( RepositoryStatistics stats : statsCreated.values() )
        {
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats );
        }

        List<RepositoryStatistics> list =
            repositoryStatisticsManager.getStatisticsInRange( TEST_REPO_ID, new Date( current.getTime() - 20000 ),
                                                              new Date( current.getTime() - 2000 ) );

        assertEquals( 2, list.size() );
        assertEquals( new Date( current.getTime() - 12345 ), list.get( 1 ).getScanStartTime() );
        assertEquals( new Date( current.getTime() - 3000 ), list.get( 0 ).getScanStartTime() );

        metadataRepositoryControl.verify();
    }

    public void testGetStatsRangeLowerAndUpperOutside()
    {
        Date current = new Date();

        addStats( new Date( current.getTime() - 12345 ), new Date( current.getTime() - 6000 ) );
        addStats( new Date( current.getTime() - 3000 ), new Date( current.getTime() - 2000 ) );
        addStats( new Date( current.getTime() - 1000 ), current );

        ArrayList<String> keys = new ArrayList<String>( statsCreated.keySet() );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ), keys );

        String key = keys.get( 0 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, key ),
            statsCreated.get( key ) );
        key = keys.get( 1 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, key ),
            statsCreated.get( key ) );
        key = keys.get( 2 );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, key ),
            statsCreated.get( key ) );

        metadataRepositoryControl.replay();

        for ( RepositoryStatistics stats : statsCreated.values() )
        {
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats );
        }

        List<RepositoryStatistics> list =
            repositoryStatisticsManager.getStatisticsInRange( TEST_REPO_ID, new Date( current.getTime() - 20000 ),
                                                              current );

        assertEquals( 3, list.size() );
        assertEquals( new Date( current.getTime() - 12345 ), list.get( 2 ).getScanStartTime() );
        assertEquals( new Date( current.getTime() - 3000 ), list.get( 1 ).getScanStartTime() );
        assertEquals( new Date( current.getTime() - 1000 ), list.get( 0 ).getScanStartTime() );

        metadataRepositoryControl.verify();
    }

    public void testGetStatsRangeNotInside()
    {
        Date current = new Date();

        addStats( new Date( current.getTime() - 12345 ), new Date( current.getTime() - 6000 ) );
        addStats( new Date( current.getTime() - 3000 ), new Date( current.getTime() - 2000 ) );
        addStats( new Date( current.getTime() - 1000 ), current );

        ArrayList<String> keys = new ArrayList<String>( statsCreated.keySet() );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ), keys );

        metadataRepositoryControl.replay();

        for ( RepositoryStatistics stats : statsCreated.values() )
        {
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats );
        }

        List<RepositoryStatistics> list =
            repositoryStatisticsManager.getStatisticsInRange( TEST_REPO_ID, new Date( current.getTime() - 20000 ),
                                                              new Date( current.getTime() - 16000 ) );

        assertEquals( 0, list.size() );

        metadataRepositoryControl.verify();
    }

    private void addStats( Date startTime, Date endTime )
    {
        RepositoryStatistics stats = createTestStats( startTime, endTime );
        String startTimeAsString = DefaultRepositoryStatisticsManager.SCAN_TIMESTAMP.format( startTime );
        metadataRepository.addMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, startTimeAsString, stats );
        statsCreated.put( startTimeAsString, stats );
    }

    private RepositoryStatistics createTestStats( Date startTime, Date endTime )
    {
        RepositoryStatistics stats = new RepositoryStatistics();
        stats.setScanStartTime( startTime );
        stats.setScanEndTime( endTime );
        stats.setTotalArtifactFileSize( 1400032000L );
        stats.setNewFileCount( 45 );
        stats.setTotalArtifactCount( 10412 );
        stats.setTotalProjectCount( 2036 );
        stats.setTotalGroupCount( 531 );
        stats.setTotalFileCount( 56345 );
        return stats;
    }
}
