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

import org.apache.archiva.maven.metadata.model.MavenArtifactFacet;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.model.DefaultRepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class RepositoryStatisticsManagerTest
{
    private DefaultRepositoryStatisticsManager repositoryStatisticsManager;

    private static final String TEST_REPO_ID = "test-repo";


    private MetadataRepository metadataRepository;

    private static final String FIRST_TEST_SCAN = "2009/12/01/123456.789";

    private static final String SECOND_TEST_SCAN = "2009/12/02/012345.678";

    private Map<String, RepositoryStatistics> statsCreated = new LinkedHashMap<String, RepositoryStatistics>();

    private static final SimpleDateFormat TIMESTAMP_FORMAT = createTimestampFormat();

    private RepositorySessionFactory repositorySessionFactory;
    private RepositorySession session;

    private static SimpleDateFormat createTimestampFormat()
    {
        SimpleDateFormat fmt = new SimpleDateFormat( DefaultRepositoryStatistics.SCAN_TIMESTAMP_FORMAT );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return fmt;
    }

    @BeforeEach
    public void setUp()
        throws Exception
    {
        repositoryStatisticsManager = new DefaultRepositoryStatisticsManager();

        metadataRepository = mock( MetadataRepository.class );

        repositorySessionFactory = mock(RepositorySessionFactory.class);

        repositoryStatisticsManager.setRepositorySessionFactory( repositorySessionFactory );

        session = mock( RepositorySession.class );

    }

    @Test
    public void testGetLatestStats()
        throws Exception
    {
        Date startTime = TIMESTAMP_FORMAT.parse( SECOND_TEST_SCAN );
        Date endTime = new Date( startTime.getTime() + 60000 );

        DefaultRepositoryStatistics defStats = new DefaultRepositoryStatistics();
        defStats.setScanStartTime( startTime );
        defStats.setScanEndTime( endTime );
        RepositoryStatistics stats = defStats;
        stats.setTotalArtifactFileSize( 1314527915L );
        stats.setNewFileCount( 123 );
        stats.setTotalArtifactCount( 10386 );
        stats.setTotalProjectCount( 2031 );
        stats.setTotalGroupCount( 529 );
        stats.setTotalFileCount( 56229 );

        when( repositorySessionFactory.createSession( ) ).thenReturn( session );
        when( session.getRepository() ).thenReturn( metadataRepository );
        session.close();


        when(metadataRepository.getMetadataFacets(session, TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID)).thenReturn(
                    Arrays.asList(FIRST_TEST_SCAN, SECOND_TEST_SCAN));

        when(metadataRepository.getMetadataFacet(session, TEST_REPO_ID,
                    DefaultRepositoryStatistics.FACET_ID, SECOND_TEST_SCAN)).thenReturn(stats);

        stats = repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID );
        assertNotNull( stats );
        assertEquals( 1314527915L, stats.getTotalArtifactFileSize() );
        assertEquals( 123, stats.getNewFileCount() );
        assertEquals( 10386, stats.getTotalArtifactCount() );
        assertEquals( 2031, stats.getTotalProjectCount() );
        assertEquals( 529, stats.getTotalGroupCount() );
        assertEquals( 56229, stats.getTotalFileCount() );
        assertEquals( SECOND_TEST_SCAN, TIMESTAMP_FORMAT.format( stats.getScanStartTime() ) );
        assertEquals( SECOND_TEST_SCAN, stats.getName() );
        assertEquals( endTime, stats.getScanEndTime() );

    }

    @Test
    public void testGetLatestStatsWhenEmpty()
        throws Exception
    {

        when( repositorySessionFactory.createSession( ) ).thenReturn( session );
        when( session.getRepository() ).thenReturn( metadataRepository );
        session.close();

        when( metadataRepository.getMetadataFacets(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID ) ).thenReturn(
            Collections.<String>emptyList() );

        RepositoryStatistics stats = repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID );
        assertNull( stats );

    }

    @Test
    public void testAddNewStats()
        throws Exception
    {
        Date current = new Date();
        Date startTime = new Date( current.getTime() - 12345 );

        RepositoryStatistics stats = createTestStats( startTime, current );

        walkRepository( 1 );

        when( repositorySessionFactory.createSession( ) ).thenReturn( session );
        when( session.getRepository() ).thenReturn( metadataRepository );
        session.close();

        metadataRepository.addMetadataFacet(session , TEST_REPO_ID, stats );

        when( metadataRepository.getMetadataFacets(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID ) ).thenReturn(
            Arrays.asList( stats.getName() ) );

        when( metadataRepository.getMetadataFacet(session , TEST_REPO_ID,
            DefaultRepositoryStatistics.FACET_ID, stats.getName() ) ).thenReturn( stats );

        repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, startTime, current, 56345,
                                                            45 );

        stats = repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID );
        assertNotNull( stats );
        assertEquals( 246900, stats.getTotalArtifactFileSize() );
        assertEquals( 45, stats.getNewFileCount() );
        assertEquals( 20, stats.getTotalArtifactCount() );
        assertEquals( 5, stats.getTotalProjectCount() );
        assertEquals( 4, stats.getTotalGroupCount() );
        assertEquals( 56345, stats.getTotalFileCount() );
        assertEquals( current.getTime() - 12345, stats.getScanStartTime().getTime() );
        assertEquals( current, stats.getScanEndTime() );

    }

    @Test
    public void testDeleteStats()
        throws Exception
    {
        walkRepository( 2 );

        Date current = new Date();

        Date startTime1 = new Date( current.getTime() - 12345 );
        DefaultRepositoryStatistics stats1 = createTestStats( startTime1, new Date( current.getTime() - 6000 ) );
        when( repositorySessionFactory.createSession( ) ).thenReturn( session );
        when( session.getRepository() ).thenReturn( metadataRepository );
        session.close();

        metadataRepository.addMetadataFacet(session , TEST_REPO_ID, stats1 );

        Date startTime2 = new Date( current.getTime() - 3000 );
        DefaultRepositoryStatistics stats2 = createTestStats( startTime2, current );
        metadataRepository.addMetadataFacet(session , TEST_REPO_ID, stats2 );


        when( metadataRepository.getMetadataFacets(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID ) ).thenReturn(
            Arrays.asList( stats1.getName(), stats2.getName() ) ).thenReturn( Collections.emptyList() );

        when( metadataRepository.getMetadataFacet(session , TEST_REPO_ID,
            DefaultRepositoryStatistics.FACET_ID, stats2.getName() ) ).thenReturn( stats2 );
        metadataRepository.removeMetadataFacets(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID );

        repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, startTime1,
            stats1.getScanEndTime(), 56345, 45 );

        repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, startTime2,
                                                            stats2.getScanEndTime(), 56345, 45 );

        assertNotNull( repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID ) );

        repositoryStatisticsManager.deleteStatistics( TEST_REPO_ID );

        assertNull( repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID ) );

    }

    @Test
    public void testDeleteStatsWhenEmpty()
        throws Exception
    {
        when( repositorySessionFactory.createSession( ) ).thenReturn( session );
        when( session.getRepository() ).thenReturn( metadataRepository );
        session.close();

        when( metadataRepository.getMetadataFacets( session, TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID ) ).thenReturn(
            Collections.<String>emptyList( ) );
        metadataRepository.removeMetadataFacets(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID );

        assertNull( repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID ) );

        repositoryStatisticsManager.deleteStatistics( TEST_REPO_ID );

        assertNull( repositoryStatisticsManager.getLastStatistics( TEST_REPO_ID ) );
        verify( metadataRepository, times( 2 ) ).getMetadataFacets( session, TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID );

    }

    @Test
    public void testGetStatsRangeInside()
        throws Exception
    {
        walkRepository( 3 );

        Date current = new Date();

        when( repositorySessionFactory.createSession( ) ).thenReturn( session );
        when( session.getRepository() ).thenReturn( metadataRepository );
        session.close();
        addStats( new Date( current.getTime() - 12345 ), new Date( current.getTime() - 6000 ) );
        addStats( new Date( current.getTime() - 3000 ), new Date( current.getTime() - 2000 ) );
        addStats( new Date( current.getTime() - 1000 ), current );

        ArrayList<String> keys = new ArrayList<>( statsCreated.keySet() );

        when( metadataRepository.getMetadataFacets(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID ) ).thenReturn( keys );

        // only match the middle one
        String key = keys.get( 1 );

        when( metadataRepository.getMetadataFacet(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID, key ) ).thenReturn(
            statsCreated.get( key ) );


        for ( RepositoryStatistics stats : statsCreated.values() )
        {
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID,
                                                                stats.getScanStartTime(), stats.getScanEndTime(), 56345,
                                                                45 );
        }

        List<RepositoryStatistics> list =
            repositoryStatisticsManager.getStatisticsInRange( TEST_REPO_ID,
                                                              new Date( current.getTime() - 4000 ),
                                                              new Date( current.getTime() - 2000 ) );

        assertEquals( 1, list.size() );
        assertEquals( new Date( current.getTime() - 3000 ), list.get( 0 ).getScanStartTime() );

    }

    @Test
    public void testGetStatsRangeUpperOutside()
        throws Exception
    {
        walkRepository( 3 );

        Date current = new Date();

        when( repositorySessionFactory.createSession( ) ).thenReturn( session );
        when( session.getRepository() ).thenReturn( metadataRepository );
        session.close();

        addStats( new Date( current.getTime() - 12345 ), new Date( current.getTime() - 6000 ) );
        addStats( new Date( current.getTime() - 3000 ), new Date( current.getTime() - 2000 ) );
        addStats( new Date( current.getTime() - 1000 ), current );

        List<String> keys = new ArrayList<>( statsCreated.keySet() );

        when( metadataRepository.getMetadataFacets(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID ) ).thenReturn( keys );

        String key = keys.get( 1 );

        when( metadataRepository.getMetadataFacet(session, TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID, key ) ).thenReturn(
            statsCreated.get( key ) );

        key = keys.get( 2 );


        when( metadataRepository.getMetadataFacet(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID, key ) ).thenReturn(
            statsCreated.get( key ) );


        for ( RepositoryStatistics stats : statsCreated.values() )
        {
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID,
                                                                stats.getScanStartTime(), stats.getScanEndTime(), 56345,
                                                                45 );
        }

        List<RepositoryStatistics> list =
            repositoryStatisticsManager.getStatisticsInRange( TEST_REPO_ID,
                                                              new Date( current.getTime() - 4000 ), current );

        assertEquals( 2, list.size() );
        assertEquals( new Date( current.getTime() - 3000 ), list.get( 1 ).getScanStartTime() );
        assertEquals( new Date( current.getTime() - 1000 ), list.get( 0 ).getScanStartTime() );

    }

    @Test
    public void testGetStatsRangeLowerOutside()
        throws Exception
    {
        walkRepository( 3 );

        Date current = new Date();

        when( repositorySessionFactory.createSession( ) ).thenReturn( session );
        when( session.getRepository() ).thenReturn( metadataRepository );
        session.close();

        addStats( new Date( current.getTime() - 12345 ), new Date( current.getTime() - 6000 ) );
        addStats( new Date( current.getTime() - 3000 ), new Date( current.getTime() - 2000 ) );
        addStats( new Date( current.getTime() - 1000 ), current );

        List<String> keys = new ArrayList<>( statsCreated.keySet() );

        when( metadataRepository.getMetadataFacets(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID ) ).thenReturn( keys );

        String key = keys.get( 0 );

        when( metadataRepository.getMetadataFacet(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID, key ) ).thenReturn(
            statsCreated.get( key ) );
        key = keys.get( 1 );

        when( metadataRepository.getMetadataFacet(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID, key ) ).thenReturn(
            statsCreated.get( key ) );

        for ( RepositoryStatistics stats : statsCreated.values() )
        {
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID,
                                                                stats.getScanStartTime(), stats.getScanEndTime(), 56345,
                                                                45 );
        }

        List<RepositoryStatistics> list =
            repositoryStatisticsManager.getStatisticsInRange( TEST_REPO_ID,
                                                              new Date( current.getTime() - 20000 ),
                                                              new Date( current.getTime() - 2000 ) );

        assertEquals( 2, list.size() );
        assertEquals( new Date( current.getTime() - 12345 ), list.get( 1 ).getScanStartTime() );
        assertEquals( new Date( current.getTime() - 3000 ), list.get( 0 ).getScanStartTime() );

    }

    @Test
    public void testGetStatsRangeLowerAndUpperOutside()
        throws Exception
    {
        walkRepository( 3 );

        Date current = new Date();

        when( repositorySessionFactory.createSession( ) ).thenReturn( session );
        when( session.getRepository() ).thenReturn( metadataRepository );
        session.close();

        addStats( new Date( current.getTime() - 12345 ), new Date( current.getTime() - 6000 ) );
        addStats( new Date( current.getTime() - 3000 ), new Date( current.getTime() - 2000 ) );
        addStats( new Date( current.getTime() - 1000 ), current );

        ArrayList<String> keys = new ArrayList<>( statsCreated.keySet() );

        when( metadataRepository.getMetadataFacets(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID ) ).thenReturn( keys );

        String key = keys.get( 0 );

        when( metadataRepository.getMetadataFacet(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID, key ) ).thenReturn(
            statsCreated.get( key ) );
        key = keys.get( 1 );

        when( metadataRepository.getMetadataFacet(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID, key ) ).thenReturn(
            statsCreated.get( key ) );
        key = keys.get( 2 );

        when( metadataRepository.getMetadataFacet(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID, key ) ).thenReturn(
            statsCreated.get( key ) );

        for ( RepositoryStatistics stats : statsCreated.values() )
        {
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID,
                                                                stats.getScanStartTime(), stats.getScanEndTime(), 56345,
                                                                45 );
        }

        List<RepositoryStatistics> list =
            repositoryStatisticsManager.getStatisticsInRange( TEST_REPO_ID,
                                                              new Date( current.getTime() - 20000 ), current );

        assertEquals( 3, list.size() );
        assertEquals( new Date( current.getTime() - 12345 ), list.get( 2 ).getScanStartTime() );
        assertEquals( new Date( current.getTime() - 3000 ), list.get( 1 ).getScanStartTime() );
        assertEquals( new Date( current.getTime() - 1000 ), list.get( 0 ).getScanStartTime() );

    }

    @Test
    public void testGetStatsRangeNotInside()
        throws Exception
    {
        walkRepository( 3 );

        Date current = new Date();
        when( repositorySessionFactory.createSession( ) ).thenReturn( session );
        when( session.getRepository() ).thenReturn( metadataRepository );
        session.close();

        addStats( new Date( current.getTime() - 12345 ), new Date( current.getTime() - 6000 ) );
        addStats( new Date( current.getTime() - 3000 ), new Date( current.getTime() - 2000 ) );
        addStats( new Date( current.getTime() - 1000 ), current );

        ArrayList<String> keys = new ArrayList<>( statsCreated.keySet() );

        when( metadataRepository.getMetadataFacets(session , TEST_REPO_ID, DefaultRepositoryStatistics.FACET_ID ) ).thenReturn( keys );

        for ( RepositoryStatistics stats : statsCreated.values() )
        {
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID,
                                                                stats.getScanStartTime(), stats.getScanEndTime(), 56345,
                                                                45 );
        }

        List<RepositoryStatistics> list =
            repositoryStatisticsManager.getStatisticsInRange( TEST_REPO_ID,
                                                              new Date( current.getTime() - 20000 ),
                                                              new Date( current.getTime() - 16000 ) );

        assertEquals( 0, list.size() );

    }

    private void addStats( Date startTime, Date endTime )
        throws Exception
    {
        RepositorySession session = repositorySessionFactory.createSession();

        DefaultRepositoryStatistics stats = createTestStats( startTime, endTime );
        metadataRepository.addMetadataFacet(session , TEST_REPO_ID, stats );
        statsCreated.put( stats.getName(), stats );
    }

    private ArtifactMetadata createArtifact( String namespace, String projectId, String projectVersion, String type )
    {
        ArtifactMetadata metadata = new ArtifactMetadata();
        metadata.setRepositoryId( TEST_REPO_ID );
        metadata.setId( projectId + "-" + projectVersion + "." + type );
        metadata.setProject( projectId );
        metadata.setSize( 12345L );
        metadata.setProjectVersion( projectVersion );
        metadata.setVersion( projectVersion );
        metadata.setNamespace( namespace );

        MavenArtifactFacet facet = new MavenArtifactFacet();
        facet.setType( type );
        metadata.addFacet( facet );

        return metadata;
    }

    private DefaultRepositoryStatistics createTestStats( Date startTime, Date endTime )
    {
        DefaultRepositoryStatistics stats = new DefaultRepositoryStatistics();
        stats.setRepositoryId( TEST_REPO_ID );
        stats.setScanStartTime( startTime );
        stats.setScanEndTime( endTime );
        stats.setTotalArtifactFileSize( 20 * 12345L );
        stats.setNewFileCount( 45 );
        stats.setTotalArtifactCount( 20 );
        stats.setTotalProjectCount( 5 );
        stats.setTotalGroupCount( 4 );
        stats.setTotalFileCount( 56345 );
        stats.setTotalCountForType( "jar", 10 );
        stats.setTotalCountForType( "pom", 10 );
        return stats;
    }

    private void walkRepository( int count )
        throws Exception
    {
        when( repositorySessionFactory.createSession( ) ).thenReturn( session );

        for ( int i = 0; i < count; i++ )
        {


            when( metadataRepository.getRootNamespaces(session , TEST_REPO_ID ) ).thenReturn( Arrays.asList( "com", "org" ) );

            when( metadataRepository.getProjects(session , TEST_REPO_ID, "com" ) ).thenReturn( Arrays.<String>asList() );

            when( metadataRepository.getChildNamespaces(session , TEST_REPO_ID, "com" ) ).thenReturn( Arrays.asList( "example" ) );

            when( metadataRepository.getChildNamespaces(session , TEST_REPO_ID, "com.example" ) ).thenReturn(
                Arrays.<String>asList() );

            when( metadataRepository.getProjects(session , TEST_REPO_ID, "com.example" ) ).thenReturn(
                Arrays.asList( "example-project" ) );

            when( metadataRepository.getProjectVersions(session , TEST_REPO_ID, "com.example", "example-project" ) ).thenReturn(
                Arrays.asList( "1.0", "1.1" ) );

            when(
                metadataRepository.getArtifacts(session , TEST_REPO_ID, "com.example", "example-project", "1.0" ) ).thenReturn(
                Arrays.asList( createArtifact( "com.example", "example-project", "1.0", "jar" ),
                               createArtifact( "com.example", "example-project", "1.0", "pom" ) ) );

            when(
                metadataRepository.getArtifacts(session , TEST_REPO_ID, "com.example", "example-project", "1.1" ) ).thenReturn(
                Arrays.asList( createArtifact( "com.example", "example-project", "1.1", "jar" ),
                               createArtifact( "com.example", "example-project", "1.1", "pom" ) ) );


            when( metadataRepository.getChildNamespaces(session , TEST_REPO_ID, "org" ) ).thenReturn( Arrays.asList( "apache", "codehaus" ) );

            when( metadataRepository.getChildNamespaces(session , TEST_REPO_ID, "org.apache" ) ).thenReturn( Arrays.asList( "archiva", "maven" )  );


            when( metadataRepository.getProjects(session , TEST_REPO_ID, "org.apache" ) ).thenReturn( Arrays.<String>asList() );

            when( metadataRepository.getChildNamespaces(session , TEST_REPO_ID, "org.apache.archiva" ) ).thenReturn( Arrays.<String>asList() );

            when( metadataRepository.getProjects(session , TEST_REPO_ID, "org.apache.archiva" ) ).thenReturn( Arrays.asList( "metadata-repository-api", "metadata-model" ) );

            when( metadataRepository.getProjectVersions(session , TEST_REPO_ID, "org.apache.archiva", "metadata-repository-api" ) )
                .thenReturn( Arrays.asList( "1.3-SNAPSHOT", "1.3" )  );


            when( metadataRepository.getArtifacts(session , TEST_REPO_ID, "org.apache.archiva", "metadata-repository-api", "1.3-SNAPSHOT" ) )
                .thenReturn( Arrays.asList( createArtifact( "org.apache.archiva", "metadata-repository-api", "1.3-SNAPSHOT", "jar" ),
                                           createArtifact( "org.apache.archiva", "metadata-repository-api", "1.3-SNAPSHOT",
                                                           "pom" ) )  );

            when( metadataRepository.getArtifacts(session , TEST_REPO_ID, "org.apache.archiva", "metadata-repository-api", "1.3" ) )
                .thenReturn( Arrays.asList( createArtifact( "org.apache.archiva", "metadata-repository-api", "1.3", "jar" ),
                                           createArtifact( "org.apache.archiva", "metadata-repository-api", "1.3", "pom" ) ) );

            when( metadataRepository.getProjectVersions(session , TEST_REPO_ID, "org.apache.archiva", "metadata-model" ) )
                .thenReturn( Arrays.asList( "1.3-SNAPSHOT", "1.3" )  );

            when( metadataRepository.getArtifacts(session , TEST_REPO_ID, "org.apache.archiva", "metadata-model", "1.3-SNAPSHOT" ) )
                .thenReturn( Arrays.asList( createArtifact( "org.apache.archiva", "metadata-model", "1.3-SNAPSHOT", "jar" ),
                                           createArtifact( "org.apache.archiva", "metadata-model", "1.3-SNAPSHOT", "pom" ) ) );

            when( metadataRepository.getArtifacts(session , TEST_REPO_ID, "org.apache.archiva", "metadata-model", "1.3" ) )
                .thenReturn( Arrays.asList( createArtifact( "org.apache.archiva", "metadata-model", "1.3", "jar" ),
                                           createArtifact( "org.apache.archiva", "metadata-model", "1.3", "pom" ) ) );

            when( metadataRepository.getChildNamespaces(session , TEST_REPO_ID, "org.apache.maven" ) ).thenReturn( Arrays.<String>asList() );

            when( metadataRepository.getProjects(session , TEST_REPO_ID, "org.apache.maven" ) )
                .thenReturn( Arrays.asList( "maven-model" )  );

            when( metadataRepository.getProjectVersions(session , TEST_REPO_ID, "org.apache.maven", "maven-model" ) )
                .thenReturn( Arrays.asList( "2.2.1" ) );

            when( metadataRepository.getArtifacts(session , TEST_REPO_ID, "org.apache.maven", "maven-model", "2.2.1" ) )
                .thenReturn( Arrays.asList( createArtifact( "org.apache.archiva", "maven-model", "2.2.1", "jar" ),
                                           createArtifact( "org.apache.archiva", "maven-model", "2.2.1", "pom" ) ) );

            when( metadataRepository.getChildNamespaces(session , TEST_REPO_ID, "org.codehaus" ) ).thenReturn( Arrays.asList( "plexus" ) );

            when( metadataRepository.getProjects(session , TEST_REPO_ID, "org" ) ).thenReturn( Arrays.<String>asList(  ) );

            when( metadataRepository.getProjects(session , TEST_REPO_ID, "org.codehaus" ) )
                .thenReturn( Arrays.<String>asList(  ) );

            when( metadataRepository.getChildNamespaces(session , TEST_REPO_ID, "org.codehaus.plexus" ) )
                .thenReturn( Arrays.<String>asList(  ) );

            when( metadataRepository.getProjects(session , TEST_REPO_ID, "org.codehaus.plexus" ) )
                .thenReturn( Arrays.asList( "plexus-spring" )  );

            when( metadataRepository.getProjectVersions(session, TEST_REPO_ID, "org.codehaus.plexus", "plexus-spring" ) )
                .thenReturn( Arrays.asList( "1.0", "1.1", "1.2" ) );


            when( metadataRepository.getArtifacts(session , TEST_REPO_ID, "org.codehaus.plexus", "plexus-spring", "1.0" ) )
                .thenReturn( Arrays.asList( createArtifact( "org.codehaus.plexus", "plexus-spring", "1.0", "jar" ),
                                           createArtifact( "org.codehaus.plexus", "plexus-spring", "1.0", "pom" ) ) );

            when( metadataRepository.getArtifacts(session, TEST_REPO_ID, "org.codehaus.plexus", "plexus-spring", "1.1" ) )
                .thenReturn( Arrays.asList( createArtifact( "org.codehaus.plexus", "plexus-spring", "1.1", "jar" ),
                                           createArtifact( "org.codehaus.plexus", "plexus-spring", "1.1", "pom" ) )  );

            when( metadataRepository.getArtifacts(session , TEST_REPO_ID, "org.codehaus.plexus", "plexus-spring", "1.2" ) )
                .thenReturn( Arrays.asList( createArtifact( "org.codehaus.plexus", "plexus-spring", "1.2", "jar" ),
                                           createArtifact( "org.codehaus.plexus", "plexus-spring", "1.2", "pom" ) )  );
        }
    }
}
