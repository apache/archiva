package org.apache.maven.archiva.web.action.reports;

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

import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.stats.RepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.reports.RepositoryProblemFacet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.AbstractRepositoryBasedAction;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="generateReport" instantiation-strategy="per-lookup"
 */
public class GenerateReportAction
    extends AbstractRepositoryBasedAction
    implements SecureAction, Preparable
{
    public static final String ALL_REPOSITORIES = "All Repositories";

    public static final String BLANK = "blank";

    private static final String[] datePatterns =
        new String[]{"MM/dd/yy", "MM/dd/yyyy", "MMMMM/dd/yyyy", "MMMMM/dd/yy", "dd MMMMM yyyy", "dd/MM/yy",
            "dd/MM/yyyy", "yyyy/MM/dd", "yyyy-MM-dd", "yyyy-dd-MM", "MM-dd-yyyy", "MM-dd-yy"};

    public static final String SEND_FILE = "send-file";

    private Logger log = LoggerFactory.getLogger( GenerateReportAction.class );

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement
     */
    private RepositoryStatisticsManager repositoryStatisticsManager;

    private String groupId;

    private String repositoryId;

    private int page = 1;

    private int rowCount = 100;

    private List<String> selectedRepositories = new ArrayList<String>();

    private String startDate;

    private String endDate;

    private int numPages;

    private Collection<String> repositoryIds;

    private Map<String, List<RepositoryProblemFacet>> repositoriesMap =
        new TreeMap<String, List<RepositoryProblemFacet>>();

    private List<String> availableRepositories;

    private List<RepositoryStatistics> repositoryStatistics = new ArrayList<RepositoryStatistics>();

    private InputStream inputStream;

    private boolean lastPage;

    /**
     * @plexus.requirement
     */
    private MetadataRepository metadataRepository;

    @SuppressWarnings( "unchecked" )
    public void prepare()
    {
        repositoryIds = new ArrayList<String>();
        repositoryIds.add( ALL_REPOSITORIES ); // comes first to be first in the list
        repositoryIds.addAll( getObservableRepos() );

        availableRepositories = new ArrayList<String>();

        // remove selected repositories in the option for the statistics report
        availableRepositories.addAll( archivaConfiguration.getConfiguration().getManagedRepositoriesAsMap().keySet() );
        for ( String repo : selectedRepositories )
        {
            if ( availableRepositories.contains( repo ) )
            {
                availableRepositories.remove( repo );
            }
        }
    }

    /**
     * Generate the statistics report.
     *
     * check whether single repo report or comparison report
     * 1. if it is a single repository, get all the statistics for the repository on the specified date
     * - if no date is specified, get only the latest
     * (total page = 1 --> no pagination since only the most recent stats will be displayed)
     * - otherwise, get everything within the date range (total pages = repo stats / rows per page)
     * - required params: repository, startDate, endDate
     *
     * 2. if multiple repositories, get the latest statistics on each repository on the specified date
     * - if no date is specified, use the current date endDate
     * - required params: repositories, endDate
     * - total pages = repositories / rows per page
     *
     * @return action result
     */
    public String generateStatistics()
    {
        if ( rowCount < 10 )
        {
            // TODO: move to validation framework
            addFieldError( "rowCount", "Row count must be larger than 10." );
            return INPUT;
        }
        Date startDateInDF;
        Date endDateInDF;

        if ( selectedRepositories.size() > 1 )
        {
            numPages = 1;

            try
            {
                startDateInDF = getStartDateInDateFormat();
                endDateInDF = getEndDateInDateFormat();
            }
            catch ( ParseException e )
            {
                addActionError( "Error parsing date(s)." );
                return ERROR;
            }

            if ( startDateInDF != null && endDateInDF != null && startDateInDF.after( endDateInDF ) )
            {
                addFieldError( "startDate", "Start Date must be earlier than the End Date" );
                return INPUT;
            }

            // multiple repos
            for ( String repo : selectedRepositories )
            {
                List<RepositoryStatistics> stats = null;
                try
                {
                    stats = repositoryStatisticsManager.getStatisticsInRange( repo, startDateInDF, endDateInDF );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "Unable to retrieve stats, assuming is empty: " + e.getMessage(), e );
                }
                if ( stats == null || stats.isEmpty() )
                {
                    log.info( "No statistics available for repository '" + repo + "'." );
                    // TODO set repo's stats to 0
                    continue;
                }

                repositoryStatistics.add( stats.get( 0 ) );
            }
        }
        else if ( selectedRepositories.size() == 1 )
        {
            repositoryId = selectedRepositories.get( 0 );
            try
            {
                startDateInDF = getStartDateInDateFormat();
                endDateInDF = getEndDateInDateFormat();

                if ( startDateInDF != null && endDateInDF != null && startDateInDF.after( endDateInDF ) )
                {
                    addFieldError( "startDate", "Start Date must be earlier than the End Date" );
                    return INPUT;
                }

                List<RepositoryStatistics> stats = null;
                try
                {
                    stats = repositoryStatisticsManager.getStatisticsInRange( repositoryId, startDateInDF,
                                                                              endDateInDF );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "Unable to retrieve stats, assuming is empty: " + e.getMessage(), e );
                }
                if ( stats == null || stats.isEmpty() )
                {
                    addActionError( "No statistics available for repository. Repository might not have been scanned." );
                    return ERROR;
                }

                int rowCount = getRowCount();
                int extraPage = ( stats.size() % rowCount ) != 0 ? 1 : 0;
                int totalPages = ( stats.size() / rowCount ) + extraPage;
                numPages = totalPages;

                int currentPage = getPage();
                if ( currentPage > totalPages )
                {
                    addActionError(
                        "Error encountered while generating report :: The requested page exceeds the total number of pages." );
                    return ERROR;
                }

                int start = rowCount * ( currentPage - 1 );
                int end = ( start + rowCount ) - 1;

                if ( end > stats.size() )
                {
                    end = stats.size() - 1;
                }

                repositoryStatistics = stats.subList( start, end + 1 );
            }
            catch ( ParseException pe )
            {
                addActionError( pe.getMessage() );
                return ERROR;
            }
        }
        else
        {
            addFieldError( "availableRepositories", "Please select a repository (or repositories) from the list." );
            return INPUT;
        }

        if ( repositoryStatistics.isEmpty() )
        {
            return BLANK;
        }

        return SUCCESS;
    }

    /**
     * Export report to CSV.
     *
     * @return action result
     */
    public String downloadStatisticsReport()
    {
        Date startDateInDF;
        Date endDateInDF;

        selectedRepositories = parseSelectedRepositories();
        List<RepositoryStatistics> repositoryStatistics = new ArrayList<RepositoryStatistics>();

        StringBuffer input;
        if ( selectedRepositories.size() > 1 )
        {
            try
            {
                startDateInDF = getStartDateInDateFormat();
                endDateInDF = getEndDateInDateFormat();
            }
            catch ( ParseException e )
            {
                addActionError( "Error parsing date(s)." );
                return ERROR;
            }

            if ( startDateInDF != null && endDateInDF != null && startDateInDF.after( endDateInDF ) )
            {
                addFieldError( "startDate", "Start Date must be earlier than the End Date" );
                return INPUT;
            }

            input = new StringBuffer(
                "Repository,Total File Count,Total Size,Artifact Count,Group Count,Project Count,Plugins,Archetypes," +
                    "Jars,Wars\n" );

            // multiple repos
            for ( String repo : selectedRepositories )
            {
                List<RepositoryStatistics> stats = null;
                try
                {
                    stats = repositoryStatisticsManager.getStatisticsInRange( repo, startDateInDF, endDateInDF );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "Unable to retrieve stats, assuming is empty: " + e.getMessage(), e );
                }
                if ( stats == null || stats.isEmpty() )
                {
                    log.info( "No statistics available for repository '" + repo + "'." );
                    // TODO set repo's stats to 0
                    continue;
                }

                // only the first one
                RepositoryStatistics repositoryStats = stats.get( 0 );
                repositoryStatistics.add( repositoryStats );

                input.append( repo ).append( "," );
                input.append( repositoryStats.getTotalFileCount() ).append( "," );
                input.append( repositoryStats.getTotalArtifactFileSize() ).append( "," );
                input.append( repositoryStats.getTotalArtifactCount() ).append( "," );
                input.append( repositoryStats.getTotalGroupCount() ).append( "," );
                input.append( repositoryStats.getTotalProjectCount() ).append( "," );
                input.append( repositoryStats.getTotalCountForType( "maven-plugin" ) ).append( "," );
                input.append( repositoryStats.getTotalCountForType( "maven-archetype" ) ).append( "," );
                input.append( repositoryStats.getTotalCountForType( "jar" ) ).append( "," );
                input.append( repositoryStats.getTotalCountForType( "war" ) );
                input.append( "\n" );
            }
        }
        else if ( selectedRepositories.size() == 1 )
        {
            repositoryId = selectedRepositories.get( 0 );
            try
            {
                startDateInDF = getStartDateInDateFormat();
                endDateInDF = getEndDateInDateFormat();

                if ( startDateInDF != null && endDateInDF != null && startDateInDF.after( endDateInDF ) )
                {
                    addFieldError( "startDate", "Start Date must be earlier than the End Date" );
                    return INPUT;
                }

                List<RepositoryStatistics> stats = null;
                try
                {
                    stats = repositoryStatisticsManager.getStatisticsInRange( repositoryId, startDateInDF,
                                                                              endDateInDF );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "Unable to retrieve stats, assuming is empty: " + e.getMessage(), e );
                }
                if ( stats == null || stats.isEmpty() )
                {
                    addActionError( "No statistics available for repository. Repository might not have been scanned." );
                    return ERROR;
                }

                input = new StringBuffer(
                    "Date of Scan,Total File Count,Total Size,Artifact Count,Group Count,Project Count,Plugins," +
                        "Archetypes,Jars,Wars\n" );

                for ( RepositoryStatistics repositoryStats : stats )
                {
                    input.append( repositoryStats.getScanStartTime() ).append( "," );
                    input.append( repositoryStats.getTotalFileCount() ).append( "," );
                    input.append( repositoryStats.getTotalArtifactFileSize() ).append( "," );
                    input.append( repositoryStats.getTotalArtifactCount() ).append( "," );
                    input.append( repositoryStats.getTotalGroupCount() ).append( "," );
                    input.append( repositoryStats.getTotalProjectCount() ).append( "," );
                    input.append( repositoryStats.getTotalCountForType( "maven-plugin" ) ).append( "," );
                    input.append( repositoryStats.getTotalCountForType( "maven-archetype" ) ).append( "," );
                    input.append( repositoryStats.getTotalCountForType( "jar" ) ).append( "," );
                    input.append( repositoryStats.getTotalCountForType( "war" ) );
                    input.append( "\n" );
                }

                repositoryStatistics = stats;
            }
            catch ( ParseException pe )
            {
                addActionError( pe.getMessage() );
                return ERROR;
            }
        }
        else
        {
            addFieldError( "availableRepositories", "Please select a repository (or repositories) from the list." );
            return INPUT;
        }

        if ( repositoryStatistics.isEmpty() )
        {
            return BLANK;
        }

        // write output stream depending on single or comparison report
        StringReader reader = new StringReader( input.toString() );

        try
        {
            inputStream = new ByteArrayInputStream( IOUtils.toByteArray( reader ) );
        }
        catch ( IOException i )
        {
            addActionError( "Error occurred while generating CSV file." );
            return ERROR;
        }

        return SEND_FILE;
    }

    // hack for parsing the struts list passed as param in <s:url ../>

    private List<String> parseSelectedRepositories()
    {
        List<String> parsedSelectedRepos = new ArrayList<String>();

        for ( String repo : selectedRepositories )
        {
            String[] tokens = StringUtils.split( repo, ',' );
            if ( tokens.length > 1 )
            {
                for ( String token : tokens )
                {
                    parsedSelectedRepos.add( StringUtils.remove( StringUtils.remove( token, '[' ), ']' ).trim() );
                }
            }
            else
            {
                parsedSelectedRepos.add( StringUtils.remove( StringUtils.remove( repo, '[' ), ']' ).trim() );
            }
        }
        return parsedSelectedRepos;
    }

    private Date getStartDateInDateFormat()
        throws ParseException
    {
        Date startDateInDF;
        if ( startDate == null || "".equals( startDate ) )
        {
            startDateInDF = null;
        }
        else
        {
            startDateInDF = DateUtils.parseDate( startDate, datePatterns );
        }
        return startDateInDF;
    }

    private Date getEndDateInDateFormat()
        throws ParseException
    {
        Date endDateInDF;
        if ( endDate == null || "".equals( endDate ) )
        {
            endDateInDF = null;
        }
        else
        {
            endDateInDF = DateUtils.parseDate( endDate, datePatterns );

            // add a day, since we don't inclue time and want the date to be inclusive
            Calendar cal = Calendar.getInstance();
            cal.setTime( endDateInDF );
            cal.add( Calendar.DAY_OF_MONTH, 1 );
            endDateInDF = cal.getTime();
        }

        return endDateInDF;
    }

    public String execute()
        throws Exception
    {
        if ( repositoryId == null )
        {
            addFieldError( "repositoryId", "You must provide a repository id." );
            return INPUT;
        }

        if ( rowCount < 10 )
        {
            addFieldError( "rowCount", "Row count must be larger than 10." );
            return INPUT;
        }

        List<String> observableRepos = getObservableRepos();
        Collection<String> repoIds;
        if ( StringUtils.isEmpty( repositoryId ) || ALL_REPOSITORIES.equals( repositoryId ) )
        {
            repoIds = observableRepos;
        }
        else if ( observableRepos.contains( repositoryId ) )
        {
            repoIds = Collections.singletonList( repositoryId );
        }
        else
        {
            repoIds = Collections.emptyList();
        }

        List<RepositoryProblemFacet> problemArtifacts = new ArrayList<RepositoryProblemFacet>();
        for ( String repoId : repoIds )
        {
            // TODO: improve performance by navigating into a group subtree. Currently group is property, not part of name of item
            for ( String name : metadataRepository.getMetadataFacets( repoId, RepositoryProblemFacet.FACET_ID ) )
            {
                RepositoryProblemFacet metadataFacet = (RepositoryProblemFacet) metadataRepository.getMetadataFacet(
                    repoId, RepositoryProblemFacet.FACET_ID, name );

                if ( StringUtils.isEmpty( groupId ) || groupId.equals( metadataFacet.getNamespace() ) )
                {
                    problemArtifacts.add( metadataFacet );
                }
            }
        }

        // TODO: getting range only after reading is not efficient for a large number of artifacts
        int lowerBound = ( page - 1 ) * rowCount;
        int upperBound = ( page * rowCount ) + 1; // Add 1 to check if it's the last page or not.
        if ( upperBound <= problemArtifacts.size() )
        {
            problemArtifacts = problemArtifacts.subList( lowerBound, upperBound );
        }
        else
        {
            problemArtifacts = problemArtifacts.subList( lowerBound, problemArtifacts.size() );
        }

        for ( RepositoryProblemFacet problem : problemArtifacts )
        {
            List<RepositoryProblemFacet> problemsList;
            if ( repositoriesMap.containsKey( problem.getRepositoryId() ) )
            {
                problemsList = repositoriesMap.get( problem.getRepositoryId() );
            }
            else
            {
                problemsList = new ArrayList<RepositoryProblemFacet>();
                repositoriesMap.put( problem.getRepositoryId(), problemsList );
            }

            problemsList.add( problem );
        }

        // TODO: handling should be improved
        if ( problemArtifacts.size() <= rowCount )
        {
            lastPage = true;
        }

        if ( problemArtifacts.isEmpty() && page == 1 )
        {
            return BLANK;
        }
        else
        {
            return SUCCESS;
        }
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_ACCESS_REPORT, Resource.GLOBAL );

        return bundle;
    }

    public Collection<String> getRepositoryIds()
    {
        return repositoryIds;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public int getPage()
    {
        return page;
    }

    public void setPage( int page )
    {
        this.page = page;
    }

    public int getRowCount()
    {
        return rowCount;
    }

    public void setRowCount( int rowCount )
    {
        this.rowCount = rowCount;
    }

    public void setRepositoriesMap( Map<String, List<RepositoryProblemFacet>> repositoriesMap )
    {
        this.repositoriesMap = repositoriesMap;
    }

    public Map<String, List<RepositoryProblemFacet>> getRepositoriesMap()
    {
        return repositoriesMap;
    }

    public List<String> getSelectedRepositories()
    {
        return selectedRepositories;
    }

    public void setSelectedRepositories( List<String> selectedRepositories )
    {
        this.selectedRepositories = selectedRepositories;
    }

    public List<String> getAvailableRepositories()
    {
        return availableRepositories;
    }

    public void setAvailableRepositories( List<String> availableRepositories )
    {
        this.availableRepositories = availableRepositories;
    }

    public String getStartDate()
    {
        return startDate;
    }

    public void setStartDate( String startDate )
    {
        this.startDate = startDate;
    }

    public String getEndDate()
    {
        return endDate;
    }

    public void setEndDate( String endDate )
    {
        this.endDate = endDate;
    }

    public List<RepositoryStatistics> getRepositoryStatistics()
    {
        return repositoryStatistics;
    }

    public void setRepositoryStatistics( List<RepositoryStatistics> repositoryStatistics )
    {
        this.repositoryStatistics = repositoryStatistics;
    }

    public boolean isLastPage()
    {
        return lastPage;
    }

    public void setLastPage( boolean lastPage )
    {
        this.lastPage = lastPage;
    }

    public InputStream getInputStream()
    {
        return inputStream;
    }

    public int getNumPages()
    {
        return numPages;
    }

    public void setRepositoryStatisticsManager( RepositoryStatisticsManager repositoryStatisticsManager )
    {
        this.repositoryStatisticsManager = repositoryStatisticsManager;
    }

    public void setMetadataRepository( MetadataRepository metadataRepository )
    {
        this.metadataRepository = metadataRepository;
    }
}
