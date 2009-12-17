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
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

public class RepositoryStatisticsManagerTest
    extends TestCase
{
    private DefaultRepositoryStatisticsManager repositoryStatisticsManager;

    private static final String TEST_REPO_ID = "test-repo";

    private MockControl metadataRepositoryControl;

    private MetadataRepository metadataRepository;

    private static final String FIRST_TEST_SCAN = "2009/12/01/123456.789";

    private static final String SECOND_TEST_SCAN = "2009/12/02/012345.678";

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

        ManagedRepositoryConfiguration repository = new ManagedRepositoryConfiguration();
        repository.setId( TEST_REPO_ID );
        repository.setLocation( "" );
        ManagedDefaultRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( repository );
        MockControl control = MockClassControl.createControl( RepositoryContentFactory.class );
        RepositoryContentFactory contentFactory = (RepositoryContentFactory) control.getMock();
        contentFactory.getManagedRepositoryContent( TEST_REPO_ID );
        control.setDefaultReturnValue( content );
        control.replay();
        repositoryStatisticsManager.setRepositoryContentFactory( contentFactory );
    }

    public void testGetLatestStats()
        throws ParseException
    {
        Date endTime = new Date( RepositoryStatistics.SCAN_TIMESTAMP.parse( SECOND_TEST_SCAN ).getTime() + 60000 );

        RepositoryStatistics stats = new RepositoryStatistics();
        stats.setScanStartTime( RepositoryStatistics.SCAN_TIMESTAMP.parse( SECOND_TEST_SCAN ) );
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
        assertEquals( SECOND_TEST_SCAN, RepositoryStatistics.SCAN_TIMESTAMP.format( stats.getScanStartTime() ) );
        assertEquals( SECOND_TEST_SCAN, stats.getName() );
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

        RepositoryStatistics stats = createTestStats( startTime, current );

        walkRepository( 1 );

        metadataRepository.addMetadataFacet( TEST_REPO_ID, stats );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ),
            Arrays.asList( stats.getName() ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, stats.getName() ),
            stats );

        metadataRepositoryControl.replay();

        repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, startTime, current, 56345, 45 );

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

        metadataRepositoryControl.verify();
    }

    public void testDeleteStats()
    {
        walkRepository( 2 );

        Date current = new Date();

        Date startTime1 = new Date( current.getTime() - 12345 );
        RepositoryStatistics stats1 = createTestStats( startTime1, new Date( current.getTime() - 6000 ) );
        metadataRepository.addMetadataFacet( TEST_REPO_ID, stats1 );

        Date startTime2 = new Date( current.getTime() - 3000 );
        RepositoryStatistics stats2 = createTestStats( startTime2, current );
        metadataRepository.addMetadataFacet( TEST_REPO_ID, stats2 );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ),
            Arrays.asList( stats1.getName(), stats2.getName() ) );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacet( TEST_REPO_ID, RepositoryStatistics.FACET_ID, stats2.getName() ),
            stats2 );

        metadataRepository.removeMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID );

        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getMetadataFacets( TEST_REPO_ID, RepositoryStatistics.FACET_ID ),
            Collections.emptyList() );

        metadataRepositoryControl.replay();

        repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, startTime1, stats1.getScanEndTime(), 56345,
                                                            45 );
        repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, startTime2, stats2.getScanEndTime(), 56345,
                                                            45 );

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
        walkRepository( 3 );

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
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats.getScanStartTime(),
                                                                stats.getScanEndTime(), 56345, 45 );
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
        walkRepository( 3 );

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
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats.getScanStartTime(),
                                                                stats.getScanEndTime(), 56345, 45 );
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
        walkRepository( 3 );

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
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats.getScanStartTime(),
                                                                stats.getScanEndTime(), 56345, 45 );
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
        walkRepository( 3 );

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
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats.getScanStartTime(),
                                                                stats.getScanEndTime(), 56345, 45 );
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
        walkRepository( 3 );

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
            repositoryStatisticsManager.addStatisticsAfterScan( TEST_REPO_ID, stats.getScanStartTime(),
                                                                stats.getScanEndTime(), 56345, 45 );
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
        metadataRepository.addMetadataFacet( TEST_REPO_ID, stats );
        statsCreated.put( stats.getName(), stats );
    }

    private ArtifactMetadata createArtifact( String namespace, String projectId, String projectVersion, String type )
    {
        ArtifactMetadata metadata = new ArtifactMetadata();
        metadata.setRepositoryId( TEST_REPO_ID );
        metadata.setId( projectId + "-" + projectVersion + "." + type );
        metadata.setProject( projectId );
        metadata.setSize( 12345L );
        metadata.setVersion( projectVersion );
        metadata.setNamespace( namespace );
        return metadata;
    }

    private RepositoryStatistics createTestStats( Date startTime, Date endTime )
    {
        RepositoryStatistics stats = new RepositoryStatistics();
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
    {
        for ( int i = 0; i < count; i++ )
        {
            metadataRepositoryControl.expectAndReturn( metadataRepository.getRootNamespaces( TEST_REPO_ID ),
                                                       Arrays.asList( "com", "org" ) );
            metadataRepositoryControl.expectAndReturn( metadataRepository.getProjects( TEST_REPO_ID, "com" ),
                                                       Arrays.asList() );
            metadataRepositoryControl.expectAndReturn( metadataRepository.getNamespaces( TEST_REPO_ID, "com" ),
                                                       Arrays.asList( "example" ) );
            metadataRepositoryControl.expectAndReturn( metadataRepository.getNamespaces( TEST_REPO_ID, "com.example" ),
                                                       Arrays.asList() );
            metadataRepositoryControl.expectAndReturn( metadataRepository.getProjects( TEST_REPO_ID, "com.example" ),
                                                       Arrays.asList( "example-project" ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getProjectVersions( TEST_REPO_ID, "com.example", "example-project" ),
                Arrays.asList( "1.0", "1.1" ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getArtifacts( TEST_REPO_ID, "com.example", "example-project", "1.0" ),
                Arrays.asList( createArtifact( "com.example", "example-project", "1.0", "jar" ),
                               createArtifact( "com.example", "example-project", "1.0", "pom" ) ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getArtifacts( TEST_REPO_ID, "com.example", "example-project", "1.1" ),
                Arrays.asList( createArtifact( "com.example", "example-project", "1.1", "jar" ),
                               createArtifact( "com.example", "example-project", "1.1", "pom" ) ) );
            metadataRepositoryControl.expectAndReturn( metadataRepository.getNamespaces( TEST_REPO_ID, "org" ),
                                                       Arrays.asList( "apache", "codehaus" ) );
            metadataRepositoryControl.expectAndReturn( metadataRepository.getNamespaces( TEST_REPO_ID, "org.apache" ),
                                                       Arrays.asList( "archiva", "maven" ) );
            metadataRepositoryControl.expectAndReturn( metadataRepository.getProjects( TEST_REPO_ID, "org.apache" ),
                                                       Arrays.asList() );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getNamespaces( TEST_REPO_ID, "org.apache.archiva" ), Arrays.asList() );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getProjects( TEST_REPO_ID, "org.apache.archiva" ),
                Arrays.asList( "metadata-repository-api", "metadata-model" ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getProjectVersions( TEST_REPO_ID, "org.apache.archiva", "metadata-repository-api" ),
                Arrays.asList( "1.3-SNAPSHOT", "1.3" ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getArtifacts( TEST_REPO_ID, "org.apache.archiva", "metadata-repository-api",
                                                 "1.3-SNAPSHOT" ),
                Arrays.asList( createArtifact( "org.apache.archiva", "metadata-repository-api", "1.3-SNAPSHOT", "jar" ),
                               createArtifact( "org.apache.archiva", "metadata-repository-api", "1.3-SNAPSHOT",
                                               "pom" ) ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getArtifacts( TEST_REPO_ID, "org.apache.archiva", "metadata-repository-api", "1.3" ),
                Arrays.asList( createArtifact( "org.apache.archiva", "metadata-repository-api", "1.3", "jar" ),
                               createArtifact( "org.apache.archiva", "metadata-repository-api", "1.3", "pom" ) ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getProjectVersions( TEST_REPO_ID, "org.apache.archiva", "metadata-model" ),
                Arrays.asList( "1.3-SNAPSHOT", "1.3" ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getArtifacts( TEST_REPO_ID, "org.apache.archiva", "metadata-model", "1.3-SNAPSHOT" ),
                Arrays.asList( createArtifact( "org.apache.archiva", "metadata-model", "1.3-SNAPSHOT", "jar" ),
                               createArtifact( "org.apache.archiva", "metadata-model", "1.3-SNAPSHOT",
                                               "pom" ) ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getArtifacts( TEST_REPO_ID, "org.apache.archiva", "metadata-model", "1.3" ),
                Arrays.asList( createArtifact( "org.apache.archiva", "metadata-model", "1.3", "jar" ),
                               createArtifact( "org.apache.archiva", "metadata-model", "1.3", "pom" ) ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getNamespaces( TEST_REPO_ID, "org.apache.maven" ), Arrays.asList() );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getProjects( TEST_REPO_ID, "org.apache.maven" ), Arrays.asList( "maven-model" ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getProjectVersions( TEST_REPO_ID, "org.apache.maven", "maven-model" ),
                Arrays.asList( "2.2.1" ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getArtifacts( TEST_REPO_ID, "org.apache.maven", "maven-model", "2.2.1" ),
                Arrays.asList( createArtifact( "org.apache.archiva", "maven-model", "2.2.1", "jar" ),
                               createArtifact( "org.apache.archiva", "maven-model", "2.2.1", "pom" ) ) );
            metadataRepositoryControl.expectAndReturn( metadataRepository.getNamespaces( TEST_REPO_ID, "org.codehaus" ),
                                                       Arrays.asList( "plexus" ) );
            metadataRepositoryControl.expectAndReturn( metadataRepository.getProjects( TEST_REPO_ID, "org" ),
                                                       Arrays.asList() );
            metadataRepositoryControl.expectAndReturn( metadataRepository.getProjects( TEST_REPO_ID, "org.codehaus" ),
                                                       Arrays.asList() );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getNamespaces( TEST_REPO_ID, "org.codehaus.plexus" ), Arrays.asList() );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getProjects( TEST_REPO_ID, "org.codehaus.plexus" ),
                Arrays.asList( "plexus-spring" ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getProjectVersions( TEST_REPO_ID, "org.codehaus.plexus", "plexus-spring" ),
                Arrays.asList( "1.0", "1.1", "1.2" ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getArtifacts( TEST_REPO_ID, "org.codehaus.plexus", "plexus-spring", "1.0" ),
                Arrays.asList( createArtifact( "org.codehaus.plexus", "plexus-spring", "1.0", "jar" ),
                               createArtifact( "org.codehaus.plexus", "plexus-spring", "1.0", "pom" ) ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getArtifacts( TEST_REPO_ID, "org.codehaus.plexus", "plexus-spring", "1.1" ),
                Arrays.asList( createArtifact( "org.codehaus.plexus", "plexus-spring", "1.1", "jar" ),
                               createArtifact( "org.codehaus.plexus", "plexus-spring", "1.1", "pom" ) ) );
            metadataRepositoryControl.expectAndReturn(
                metadataRepository.getArtifacts( TEST_REPO_ID, "org.codehaus.plexus", "plexus-spring", "1.2" ),
                Arrays.asList( createArtifact( "org.codehaus.plexus", "plexus-spring", "1.2", "jar" ),
                               createArtifact( "org.codehaus.plexus", "plexus-spring", "1.2", "pom" ) ) );
        }
    }
}
