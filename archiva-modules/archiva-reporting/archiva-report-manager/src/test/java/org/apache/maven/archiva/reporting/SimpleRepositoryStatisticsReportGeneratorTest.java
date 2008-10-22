package org.apache.maven.archiva.reporting;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.constraints.ArtifactsByRepositoryConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.easymock.internal.AlwaysMatcher;

/**
 * SimpleRepositoryStatisticsReportGeneratorTest
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version $Id: SimpleRepositoryStatisticsReportGenerator.java
 * 
 * @plexus.component role="org.apache.maven.archiva.reporting.RepositoryStatisticsReportGenerator" role-hint="simple"
 */
public class SimpleRepositoryStatisticsReportGeneratorTest 
    extends PlexusInSpringTestCase
{    
    private MockControl daoControl;
    
    private ArchivaDAO dao;
    
    private MockControl artifactDaoControl;
    
    private ArtifactDAO artifactDao;
    
    private SimpleRepositoryStatisticsReportGenerator generator;
    
    private static final String REPO = "test-repo";
    
    public void setUp()
        throws Exception
    {
        super.setUp();
        
        daoControl = MockControl.createControl( ArchivaDAO.class );        
        dao = ( ArchivaDAO ) daoControl.getMock();
        
        generator = new SimpleRepositoryStatisticsReportGenerator();
        generator.setDao( dao );
        
        artifactDaoControl = MockControl.createControl( ArtifactDAO.class );
        artifactDaoControl.setDefaultMatcher( new AlwaysMatcher() );
        artifactDao = ( ArtifactDAO ) artifactDaoControl.getMock();       
    }
    
    private Date toDate( int year, int month, int date, int hour, int min, int sec )
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set( year, month, date, hour, min, sec );        
        
        return cal.getTime();        
    }
    
    private List<ArchivaArtifact> createArtifacts( String type )
    {
        List<ArchivaArtifact> artifacts = new ArrayList<ArchivaArtifact>();
        artifacts.add( createArtifact( REPO, "org.apache.archiva", "repository-statistics-" + type, "1.0", type ) );
        artifacts.add( createArtifact( REPO, "org.apache.archiva", "repository-statistics-" + type, "1.1", type ) );
        artifacts.add( createArtifact( REPO, "org.apache.archiva", "repository-statistics-" + type, "1.2", type ) );
        artifacts.add( createArtifact( REPO, "org.apache.archiva", "repository-statistics-" + type, "2.0", type ) );
        artifacts.add( createArtifact( REPO, "org.apache.archiva", "repository-statistics-" + type, "3.0", type ) );

        return artifacts;
    }    

    private ArchivaArtifact createArtifact( String repoId, String groupId, String artifactId, String version, String type )
    {
        ArchivaArtifact artifact = new ArchivaArtifact( groupId, artifactId, version, null, type );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setRepositoryId( repoId );

        return artifact;
    }
    
    private RepositoryContentStatistics createRepositoryContentStatistics( Date startDate, String repositoryId )
    {
        RepositoryContentStatistics repoContentStats = new RepositoryContentStatistics();
        repoContentStats.setRepositoryId( repositoryId );
        repoContentStats.setDuration( 10000 );
        repoContentStats.setNewFileCount( 100 );
        repoContentStats.setTotalArtifactCount( 200 );
        repoContentStats.setTotalFileCount( 250 );
        repoContentStats.setTotalGroupCount( 100 );
        repoContentStats.setTotalProjectCount( 180 );
        repoContentStats.setTotalSize( 200000 );
        repoContentStats.setWhenGathered( startDate );
        
        return repoContentStats;
    }
    
    private List<RepositoryContentStatistics> createStatisticsHistoryForSingleRepositoryTest( String repoId )
    {
        List<RepositoryContentStatistics> repoContentStatsList = new ArrayList<RepositoryContentStatistics>();
        
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 11, 1, 0, 0, 0 ), repoId ) );        
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 10, 16, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 10, 1, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 9, 16, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 9, 1, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 8, 16, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 8, 1, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 7, 16, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 7, 1, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 6, 16, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 6, 1, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 5, 16, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 5, 1, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 4, 16, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 4, 1, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 3, 16, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 3, 1, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 2, 16, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 2, 1, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 1, 16, 0, 0, 0 ), repoId ) );
        repoContentStatsList.add( createRepositoryContentStatistics( toDate( 2008, 1, 1, 0, 0, 0 ), repoId ) );
        
        return repoContentStatsList;
    }   
    
    public void testSimpleReportWithPagination()
        throws Exception
    {   
        Date startDate = toDate( 2008, 1, 1, 0, 0, 0 );
        Date endDate = toDate( 2008, 11, 30, 0, 0, 0 );
                
        DataLimits limits = new DataLimits();
        limits.setPerPageCount( 5 );
        limits.setCurrentPage( 1 );
        limits.setCountOfPages( 5 );
        limits.setTotalCount( 21 );      
                
        List<ArchivaArtifact> jarArtifacts = createArtifacts( RepositoryStatisticsReportGenerator.JAR_TYPE );        
        List<ArchivaArtifact> warArtifacts = createArtifacts( RepositoryStatisticsReportGenerator.WAR_TYPE );
        List<ArchivaArtifact> mavenPlugins = createArtifacts( RepositoryStatisticsReportGenerator.MAVEN_PLUGIN );
        
        List<RepositoryContentStatistics> repoContentStats = createStatisticsHistoryForSingleRepositoryTest( REPO );
        
        // get first page
        daoControl.expectAndReturn( dao.getArtifactDAO(), artifactDao );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( 
                new ArtifactsByRepositoryConstraint( REPO, RepositoryStatisticsReportGenerator.JAR_TYPE, endDate, "whenGathered") ), jarArtifacts, 5 );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( 
                new ArtifactsByRepositoryConstraint( REPO, RepositoryStatisticsReportGenerator.WAR_TYPE, endDate, "whenGathered") ), warArtifacts, 5 );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( 
                new ArtifactsByRepositoryConstraint( REPO, RepositoryStatisticsReportGenerator.MAVEN_PLUGIN, endDate, "whenGathered") ), mavenPlugins, 5 );
        
        daoControl.replay(); 
        artifactDaoControl.replay();
        
        List<RepositoryStatistics> data = generator.generateReport( repoContentStats, REPO, startDate, endDate, limits );
        
        daoControl.verify();
        artifactDaoControl.verify();
        
        assertEquals( 5, data.size() );
        
        RepositoryStatistics stats = (RepositoryStatistics) data.get( 0 );        
        assertEquals( REPO, stats.getRepositoryId() );
        assertEquals( 200, stats.getArtifactCount() );
        assertEquals( 5, stats.getJarCount() );
        assertEquals( 5, stats.getWarCount() );
        assertEquals( 5, stats.getPluginCount() );
        assertEquals( toDate( 2008, 11, 1, 0, 0, 0 ).getTime(), stats.getDateOfScan().getTime() );
        assertEquals( toDate( 2008, 9, 1, 0, 0, 0 ).getTime(), ( (RepositoryStatistics) data.get( 4 ) ).getDateOfScan().getTime() );
        
        // get last page
        limits.setCurrentPage( 5 );
        
        daoControl.reset();
        artifactDaoControl.reset();
        
        artifactDaoControl.setDefaultMatcher( new AlwaysMatcher() );
        
        daoControl.expectAndReturn( dao.getArtifactDAO(), artifactDao );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( 
                new ArtifactsByRepositoryConstraint( REPO, RepositoryStatisticsReportGenerator.JAR_TYPE, endDate, "whenGathered") ), jarArtifacts );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( 
                new ArtifactsByRepositoryConstraint( REPO, RepositoryStatisticsReportGenerator.WAR_TYPE, endDate, "whenGathered") ), warArtifacts );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( 
                new ArtifactsByRepositoryConstraint( REPO, RepositoryStatisticsReportGenerator.MAVEN_PLUGIN, endDate, "whenGathered") ), mavenPlugins );
        
        daoControl.replay(); 
        artifactDaoControl.replay();
        
        data = generator.generateReport( repoContentStats, REPO, startDate, endDate, limits );
        
        daoControl.verify();
        artifactDaoControl.verify();
        
        assertEquals( 1, data.size() );
        
        stats = (RepositoryStatistics) data.get( 0 );        
        assertEquals( REPO, stats.getRepositoryId() );
        assertEquals( 200, stats.getArtifactCount() );
        assertEquals( 5, stats.getJarCount() );
        assertEquals( 5, stats.getWarCount() );
        assertEquals( 5, stats.getPluginCount() );
        assertEquals( toDate( 2008, 1, 1, 0, 0, 0 ).getTime(), stats.getDateOfScan().getTime() );  
    }
    
    public void testSimpleReportWithoutPagination()
        throws Exception
    {
        Date startDate = toDate( 2008, 1, 1, 0, 0, 0 );
        Date endDate = toDate( 2008, 11, 30, 0, 0, 0 );
                        
        List<ArchivaArtifact> jarArtifacts = createArtifacts( RepositoryStatisticsReportGenerator.JAR_TYPE );        
        List<ArchivaArtifact> warArtifacts = createArtifacts( RepositoryStatisticsReportGenerator.WAR_TYPE );
        List<ArchivaArtifact> mavenPlugins = createArtifacts( RepositoryStatisticsReportGenerator.MAVEN_PLUGIN );
        
        List<RepositoryContentStatistics> repoContentStats = createStatisticsHistoryForSingleRepositoryTest( REPO );
        
        // get first page
        daoControl.expectAndReturn( dao.getArtifactDAO(), artifactDao );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( 
                new ArtifactsByRepositoryConstraint( REPO, RepositoryStatisticsReportGenerator.JAR_TYPE, endDate, "whenGathered") ), jarArtifacts, 21 );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( 
                new ArtifactsByRepositoryConstraint( REPO, RepositoryStatisticsReportGenerator.WAR_TYPE, endDate, "whenGathered") ), warArtifacts, 21 );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( 
                new ArtifactsByRepositoryConstraint( REPO, RepositoryStatisticsReportGenerator.MAVEN_PLUGIN, endDate, "whenGathered") ), mavenPlugins, 21 );
        
        daoControl.replay(); 
        artifactDaoControl.replay();
        
        List<RepositoryStatistics> data = generator.generateReport( repoContentStats, REPO, startDate, endDate, false );
        
        daoControl.verify();
        artifactDaoControl.verify();
        
        assertEquals( 21, data.size() );
        
        RepositoryStatistics stats = (RepositoryStatistics) data.get( 0 );        
        assertEquals( REPO, stats.getRepositoryId() );
        assertEquals( 200, stats.getArtifactCount() );
        assertEquals( 5, stats.getJarCount() );
        assertEquals( 5, stats.getWarCount() );
        assertEquals( 5, stats.getPluginCount() );
        assertEquals( toDate( 2008, 11, 1, 0, 0, 0 ).getTime(), stats.getDateOfScan().getTime() );
        assertEquals( toDate( 2008, 1, 1, 0, 0, 0 ).getTime(), ( (RepositoryStatistics) data.get( 20 ) ).getDateOfScan().getTime() );
    }
    
    public void testSimpleReportNoArtifactCountStatisticsAvailable()
        throws Exception
    {
        Date startDate = toDate( 2008, 1, 1, 0, 0, 0 );
        Date endDate = toDate( 2008, 11, 30, 0, 0, 0 );
                
        DataLimits limits = new DataLimits();
        limits.setPerPageCount( 5 );
        limits.setCurrentPage( 1 );
        limits.setCountOfPages( 5 );
        limits.setTotalCount( 21 );      
                
        List<ArchivaArtifact> jarArtifacts = new ArrayList<ArchivaArtifact>();        
        List<ArchivaArtifact> warArtifacts = new ArrayList<ArchivaArtifact>();
        List<ArchivaArtifact> mavenPlugins = new ArrayList<ArchivaArtifact>();
        
        List<RepositoryContentStatistics> repoContentStats = createStatisticsHistoryForSingleRepositoryTest( REPO );
                
        daoControl.expectAndReturn( dao.getArtifactDAO(), artifactDao );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( 
                new ArtifactsByRepositoryConstraint( REPO, RepositoryStatisticsReportGenerator.JAR_TYPE, endDate, "whenGathered") ), jarArtifacts, 5 );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( 
                new ArtifactsByRepositoryConstraint( REPO, RepositoryStatisticsReportGenerator.WAR_TYPE, endDate, "whenGathered") ), warArtifacts, 5 );
        
        artifactDaoControl.expectAndReturn( artifactDao.queryArtifacts( 
                new ArtifactsByRepositoryConstraint( REPO, RepositoryStatisticsReportGenerator.MAVEN_PLUGIN, endDate, "whenGathered") ), mavenPlugins, 5 );
        
        daoControl.replay(); 
        artifactDaoControl.replay();
        
        List<RepositoryStatistics> data = generator.generateReport( repoContentStats, REPO, startDate, endDate, limits );
        
        daoControl.verify();
        artifactDaoControl.verify();
        
        assertEquals( 5, data.size() );
        
        RepositoryStatistics stats = (RepositoryStatistics) data.get( 0 );        
        assertEquals( REPO, stats.getRepositoryId() );
        assertEquals( 200, stats.getArtifactCount() );
        assertEquals( 0, stats.getJarCount() );
        assertEquals( 0, stats.getWarCount() );
        assertEquals( 0, stats.getPluginCount() );
        assertEquals( toDate( 2008, 11, 1, 0, 0, 0 ).getTime(), stats.getDateOfScan().getTime() );
        assertEquals( toDate( 2008, 9, 1, 0, 0, 0 ).getTime(), ( (RepositoryStatistics) data.get( 4 ) ).getDateOfScan().getTime() );
        // no results found when ArtifactDAO was queried
    }
    
    public void testSimpleReportWithPaginationInvalidRequestedPage()
        throws Exception
    {
        Date startDate = toDate( 2008, 1, 1, 0, 0, 0 );
        Date endDate = toDate( 2008, 11, 30, 0, 0, 0 );
                
        DataLimits limits = new DataLimits();
        limits.setPerPageCount( 5 );
        limits.setCurrentPage( 10 );
        limits.setCountOfPages( 5 );
        limits.setTotalCount( 21 );      
        
        List<RepositoryContentStatistics> repoContentStats = createStatisticsHistoryForSingleRepositoryTest( REPO );
        
        try
        {
            List<RepositoryStatistics> data = generator.generateReport( repoContentStats, REPO, startDate, endDate, limits );
            fail( "An ArchivaReportException should have been thrown." );
        }
        catch ( ArchivaReportException a )
        {
            
        }
        // requested page exceeds total number of pages
    }
}
