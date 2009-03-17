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
import java.util.Date;
import java.util.List;

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.constraints.ArtifactsByRepositoryConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleRepositoryStatisticsReportGenerator
 * 
 * @version $Id: SimpleRepositoryStatisticsReportGenerator.java
 * 
 * @plexus.component role="org.apache.maven.archiva.reporting.RepositoryStatisticsReportGenerator" role-hint="simple"
 */
public class SimpleRepositoryStatisticsReportGenerator
    implements RepositoryStatisticsReportGenerator
{   
    private Logger log = LoggerFactory.getLogger( SimpleRepositoryStatisticsReportGenerator.class );
    
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;
    
    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.archiva.reporting.RepositoryStatisticsReportGenerator#generateReport(java.util.List
     *      repoContentStats, java.util.String repository, java.util.Date startDate, java.util.Date endDate,
     *      org.apache.maven.archiva.reporting.DataLimits limits )
     */
    public List<RepositoryStatistics> generateReport( List<RepositoryContentStatistics> repoContentStats,
                                                      String repository, Date startDate, Date endDate, DataLimits limits )
        throws ArchivaReportException
    {   
        if( limits.getCurrentPage() > limits.getCountOfPages() )
        {
            throw new ArchivaReportException( "The requested page exceeds the total number of pages." );
        }
         
        int start = ( limits.getPerPageCount() * limits.getCurrentPage() ) - limits.getPerPageCount();
        int end = ( start + limits.getPerPageCount() ) - 1;
        
        if( end > repoContentStats.size() )
        {
            end = repoContentStats.size() - 1;
        }
        
        return constructRepositoryStatistics( repoContentStats, repository, endDate, start, end );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.apache.maven.archiva.reporting.RepositoryStatisticsReportGenerator#generateReport(java.util.List
     *      repoContentStats, java.util.String repository, java.util.Date startDate, java.util.Date endDate, boolean firstStatsOnly)
     */
    public List<RepositoryStatistics> generateReport( List<RepositoryContentStatistics> repoContentStats,
                                                      String repository, Date startDate, Date endDate,
                                                      boolean firstStatsOnly )
        throws ArchivaReportException
    {
        if( firstStatsOnly )
        {
            return constructRepositoryStatistics( repoContentStats, repository, endDate, 0, 0 );
        }
        else
        {
            return constructRepositoryStatistics( repoContentStats, repository, endDate, 0, repoContentStats.size() - 1 );
        }
    }
    
    private List<RepositoryStatistics> constructRepositoryStatistics(
                                                                      List<RepositoryContentStatistics> repoContentStats,
                                                                      String repository, Date endDate,
                                                                      int start, int end )
    {
        ArtifactDAO artifactDao = dao.getArtifactDAO();    
        
        List<RepositoryStatistics> repoStatisticsList = new ArrayList<RepositoryStatistics>();
        for( int i = start; i <= end; i++ )
        {   
            RepositoryContentStatistics repoContentStat = (RepositoryContentStatistics) repoContentStats.get( i );
            RepositoryStatistics repoStatistics = new RepositoryStatistics();
            repoStatistics.setRepositoryId( repository );
            
            // get only the latest                
            repoStatistics.setArtifactCount( repoContentStat.getTotalArtifactCount() );
            repoStatistics.setGroupCount( repoContentStat.getTotalGroupCount() );
            repoStatistics.setProjectCount( repoContentStat.getTotalProjectCount() );
            repoStatistics.setTotalSize( repoContentStat.getTotalSize() );
            repoStatistics.setFileCount( repoContentStat.getTotalFileCount() );
            repoStatistics.setDateOfScan( repoContentStat.getWhenGathered() );
                
            try
            {
                //TODO use the repo content stats whenGathered date instead of endDate for single repo reports
                List<ArchivaArtifact> types = artifactDao.queryArtifacts( 
                         new ArtifactsByRepositoryConstraint( repository, JAR_TYPE, endDate, "whenGathered" ) );
                repoStatistics.setJarCount( types.size() );
                
                types = artifactDao.queryArtifacts( 
                        new ArtifactsByRepositoryConstraint( repository, WAR_TYPE, endDate, "whenGathered" ) );
                repoStatistics.setWarCount( types.size() );
                
                types = artifactDao.queryArtifacts( 
                        new ArtifactsByRepositoryConstraint( repository, MAVEN_PLUGIN, endDate, "whenGathered" ) );
                repoStatistics.setPluginCount( types.size() );
                
                // TODO: must need to be able to track archetypes. possible way of identifying an 
                //      archetype is by checking if archetype.xml exists in src/main/resources/META-INF/
                
            }
            catch( ArchivaDatabaseException e )
            {
                log.error( "Error occurred while querying artifacts from the database.", e.getMessage() );                 
            }            
                            
            repoStatisticsList.add( repoStatistics );  
        }
        
        return repoStatisticsList;
    }
        
    public void setDao( ArchivaDAO dao )
    {
        this.dao = dao;
    }
}
