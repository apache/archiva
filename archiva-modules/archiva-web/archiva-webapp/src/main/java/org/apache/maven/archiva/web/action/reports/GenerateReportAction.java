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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.RepositoryContentStatisticsDAO;
import org.apache.maven.archiva.database.constraints.RangeConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryContentStatisticsByRepositoryConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryProblemByGroupIdConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryProblemByRepositoryIdConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryProblemConstraint;
import org.apache.maven.archiva.database.constraints.UniqueFieldConstraint;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.model.RepositoryProblemReport;
import org.apache.maven.archiva.reporting.ArchivaReportException;
import org.apache.maven.archiva.reporting.DataLimits;
import org.apache.maven.archiva.reporting.RepositoryStatistics;
import org.apache.maven.archiva.reporting.RepositoryStatisticsReportGenerator;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.rbac.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.maven.archiva.web.action.PlexusActionSupport;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;

/**
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="generateReport"
 */
public class GenerateReportAction
    extends PlexusActionSupport
    implements SecureAction, ServletRequestAware, Preparable
{
    private Logger log = LoggerFactory.getLogger( GenerateReportAction.class );
    
    /**
     * @plexus.requirement role-hint="jdo"
     */
    protected ArchivaDAO dao;
    
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    protected Constraint constraint;

    protected HttpServletRequest request;

    protected List<RepositoryProblemReport> reports = new ArrayList<RepositoryProblemReport>();

    protected String groupId;

    protected String repositoryId;

    protected String prev;

    protected String next;

    protected int[] range = new int[2];

    protected int page = 1;

    protected int rowCount = 100;

    protected boolean isLastPage;

    public static final String BLANK = "blank";

    public static final String BASIC = "basic";

    private static Boolean jasperPresent;

    private Collection<String> repositoryIds;

    public static final String ALL_REPOSITORIES = "All Repositories";
    
    protected Map<String, List<RepositoryProblemReport>> repositoriesMap = 
    		new TreeMap<String, List<RepositoryProblemReport>>();
    
    // for statistics report
    /**
     * @plexus.requirement role-hint="simple"
     */
    private RepositoryStatisticsReportGenerator generator;
    
    private List<String> selectedRepositories = new ArrayList<String>();
    
    private List<String> availableRepositories;  
    
    private String startDate;
    
    private String endDate;
    
    private int reposSize;
    
    private String selectedRepo;
    
    private List<RepositoryStatistics> repositoryStatistics = new ArrayList<RepositoryStatistics>();
    
    private DataLimits limits = new DataLimits();
    
    private String[] datePatterns = new String[] { "MM/dd/yy", "MM/dd/yyyy", "MMMMM/dd/yyyy", "MMMMM/dd/yy", 
        "dd MMMMM yyyy", "dd/MM/yy", "dd/MM/yyyy", "yyyy/MM/dd", "yyyy-MM-dd", "yyyy-dd-MM", "MM-dd-yyyy",
        "MM-dd-yy" };
    
    public static final String SEND_FILE = "send-file";
    
    private InputStream inputStream;
    
    public void prepare()
    {
        repositoryIds = new ArrayList<String>();
        repositoryIds.add( ALL_REPOSITORIES ); // comes first to be first in the list
        repositoryIds.addAll(
            dao.query( new UniqueFieldConstraint( RepositoryProblem.class.getName(), "repositoryId" ) ) );
        
        availableRepositories = new ArrayList<String>();
     
        // remove selected repositories in the option for the statistics report
        availableRepositories.addAll( archivaConfiguration.getConfiguration().getManagedRepositoriesAsMap().keySet() );        
        for( String repo : selectedRepositories )
        {
            if( availableRepositories.contains( repo ) )
            {
                availableRepositories.remove( repo );
            }
        }
    }

    public Collection<String> getRepositoryIds()
    {
        return repositoryIds;
    }

    /**
     * Generate the statistics report.
     * 
     * check whether single repo report or comparison report
     * 1. if it is a single repository, get all the statistics for the repository on the specified date
     *    - if no date is specified, get only the latest 
     *          (total page = 1 --> no pagination since only the most recent stats will be displayed)
     *    - otherwise, get everything within the date range (total pages = repo stats / rows per page)
     *       - required params: repository, startDate, endDate
     *       
     * 2. if multiple repositories, get the latest statistics on each repository on the specified date
     *    - if no date is specified, use the current date endDate
     *       - required params: repositories, endDate
     *    - total pages = repositories / rows per page
     * 
     * @return
     */
    public String generateStatistics()
    {   
        if( rowCount < 10 )
        {
            addFieldError( "rowCount", "Row count must be larger than 10." );
            return INPUT;
        }
        reposSize = selectedRepositories.size();                
        
        try
        {
            RepositoryContentStatisticsDAO repoContentStatsDao = dao.getRepositoryContentStatisticsDAO();
            Date startDateInDF = null;
            Date endDateInDF = null;
            
            if( selectedRepositories.size() > 1 )
            {
                limits.setTotalCount( selectedRepositories.size() );            
                limits.setCurrentPage( 1 );
                limits.setPerPageCount( 1 );
                limits.setCountOfPages( 1 );
                
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
                
                // multiple repos
                generateReportForMultipleRepos(repoContentStatsDao, startDateInDF, endDateInDF, true);                
            }
            else if ( selectedRepositories.size() == 1 )
            {   
                limits.setCurrentPage( getPage() );
                limits.setPerPageCount( getRowCount() );
                
                selectedRepo = selectedRepositories.get( 0 );
                try
                {	 
                	startDateInDF = getStartDateInDateFormat();                	
                	endDateInDF = getEndDateInDateFormat();
                	 
                    List<RepositoryContentStatistics> contentStats = repoContentStatsDao.queryRepositoryContentStatistics( 
                           new RepositoryContentStatisticsByRepositoryConstraint( selectedRepo, startDateInDF, endDateInDF ) );
                    
                    if( contentStats == null || contentStats.isEmpty() )
                    {   
                        addActionError( "No statistics available for repository. Repository might not have been scanned." );
                        return ERROR;
                    }   
                    
                    limits.setTotalCount( contentStats.size() );                    
                    int extraPage = ( limits.getTotalCount() % limits.getPerPageCount() ) != 0 ? 1 : 0;
                    int totalPages = ( limits.getTotalCount() / limits.getPerPageCount() ) + extraPage;                    
                    limits.setCountOfPages( totalPages );
                    
                    repositoryStatistics = generator.generateReport( contentStats, selectedRepo, startDateInDF, endDateInDF, limits );
                }
                catch ( ObjectNotFoundException oe )
                {
                    addActionError( oe.getMessage() );
                    return ERROR;
                }
                catch ( ArchivaDatabaseException de )
                {
                    addActionError( de.getMessage() );
                    return ERROR;
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
            
            if( repositoryStatistics.isEmpty() )
            {
                return BLANK;
            }            
        }
        catch ( ArchivaReportException e )
        {
            addActionError( "Error encountered while generating report :: " + e.getMessage() );
            return ERROR;
        }
        
        return SUCCESS;
    }
	
    /**
     * Export report to CSV.
     * 
     * @return
     */
    public String downloadStatisticsReport()
    {   
        try
        {
        	Date startDateInDF = null;
            Date endDateInDF = null;
            
            selectedRepositories = parseSelectedRepositories();
            repositoryStatistics = new ArrayList<RepositoryStatistics>();
            
            RepositoryContentStatisticsDAO repoContentStatsDao = dao.getRepositoryContentStatisticsDAO();            
            if( selectedRepositories.size() > 1 )
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
                
             // multiple repos
                generateReportForMultipleRepos( repoContentStatsDao, startDateInDF, endDateInDF, false );
            }
            else if ( selectedRepositories.size() == 1 )
            {   
                selectedRepo = selectedRepositories.get( 0 );
                try
                {                 
                	startDateInDF = getStartDateInDateFormat();
                	endDateInDF = getEndDateInDateFormat();
                	
                    List<RepositoryContentStatistics> contentStats = repoContentStatsDao.queryRepositoryContentStatistics( 
                           new RepositoryContentStatisticsByRepositoryConstraint( selectedRepo, startDateInDF, endDateInDF ) );
                                        
                    if( contentStats == null || contentStats.isEmpty() )
                    {   
                        addActionError( "No statistics available for repository. Repository might not have been scanned." );
                        return ERROR;
                    }   
                    
                    repositoryStatistics = generator.generateReport( contentStats, selectedRepo, startDateInDF, endDateInDF, false );                    
                }
                catch ( ObjectNotFoundException oe )
                {
                    addActionError( oe.getMessage() );
                    return ERROR;
                }
                catch ( ArchivaDatabaseException de )
                {
                    addActionError( de.getMessage() );
                    return ERROR;
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
            
            if( repositoryStatistics.isEmpty() )
            {
                return BLANK;
            }            
        }
        catch ( ArchivaReportException e )
        {
            addActionError( "Error encountered while generating report :: " + e.getMessage() );
            return ERROR;
        }    
        
        // write output stream depending on single or comparison report              
        StringBuffer input = getInput();        
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
        List<String> pasedSelectedRepos = new ArrayList<String>();
     
        for( String repo : selectedRepositories )
        {   
            String[] tokens = StringUtils.split( repo, ',' );
            if( tokens.length > 1 )
            {
                for( int i = 0; i < tokens.length; i++ )
                {   
                    pasedSelectedRepos.add( StringUtils.remove( StringUtils.remove( tokens[i], '[' ), ']' ).trim() );
                }
            }
            else
            {
                pasedSelectedRepos.add( StringUtils.remove( StringUtils.remove( repo, '[' ), ']' ).trim() );
            }
        }
        return pasedSelectedRepos;
    }

    private void generateReportForMultipleRepos( RepositoryContentStatisticsDAO repoContentStatsDao,
                                                 Date startDateInDF, Date endDateInDF, boolean useLimits )
        throws ArchivaReportException
    {   
        for ( String repo : selectedRepositories )
        {   
            try
            {                
                List contentStats = repoContentStatsDao.queryRepositoryContentStatistics( 
                         new RepositoryContentStatisticsByRepositoryConstraint( repo, startDateInDF, endDateInDF ) );

                if ( contentStats == null || contentStats.isEmpty() )
                {
                    log.info( "No statistics available for repository '" + repo + "'." );
                    // TODO set repo's stats to 0
                    continue;
                }
                
                if( useLimits )
                {
                    repositoryStatistics.addAll( generator.generateReport( contentStats, repo, startDateInDF, endDateInDF,
                                                                       limits ) );
                }
                else
                {
                    repositoryStatistics.addAll( generator.generateReport( contentStats, repo, startDateInDF, endDateInDF, true ) );
                }
            }
            catch ( ObjectNotFoundException oe )
            {
                log.error( "No statistics available for repository '" + repo + "'." );
                // TODO set repo's stats to 0
            }
            catch ( ArchivaDatabaseException ae )
            {
                log.error( "Error encountered while querying statistics of repository '" + repo + "'." );
                // TODO set repo's stats to 0
            }
        }
    }

    private Date getStartDateInDateFormat()
        throws ParseException
    {
        Date startDateInDF;
        if ( startDate == null || "".equals( startDate ) )
        {
            startDateInDF = getDefaultStartDate();
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
            endDateInDF = getDefaultEndDate();
        }
        else
        {
            endDateInDF = DateUtils.parseDate( endDate, datePatterns );
        }
        
        return endDateInDF;
    }
    
    private StringBuffer getInput()
    {
        StringBuffer input = null;
        
        if( selectedRepositories.size() == 1 )
        {        	
        	input = new StringBuffer( "Date of Scan,Total File Count,Total Size,Artifact Count,Group Count,Project Count," +
        			"Plugins,Archetypes,Jars,Wars,Deployments,Downloads\n" );
        	
        	for( RepositoryStatistics stats : repositoryStatistics )
        	{
        		input.append( stats.getDateOfScan() ).append( "," );
        		input.append( stats.getFileCount() ).append( "," );
        		input.append( stats.getTotalSize() ).append( "," );
        		input.append( stats.getArtifactCount() ).append( "," );
        		input.append( stats.getGroupCount() ).append( "," );
        		input.append( stats.getProjectCount() ).append( "," );
        		input.append( stats.getPluginCount() ).append( "," );
        		input.append( stats.getArchetypeCount() ).append( "," );
        		input.append( stats.getJarCount() ).append( "," );
        		input.append( stats.getWarCount() ).append( "," );
        		input.append( stats.getDeploymentCount() ).append( "," );
        		input.append( stats.getDownloadCount() ).append( "\n" );
        	}        	
        }            
        else if( selectedRepositories.size() > 1 )
        {
        	input = new StringBuffer( "Repository,Total File Count,Total Size,Artifact Count,Group Count,Project Count," +
					"Plugins,Archetypes,Jars,Wars,Deployments,Downloads\n" );
			
			for( RepositoryStatistics stats : repositoryStatistics )
			{
				input.append( stats.getRepositoryId() ).append( "," );
				input.append( stats.getFileCount() ).append( "," );
				input.append( stats.getTotalSize() ).append( "," );
				input.append( stats.getArtifactCount() ).append( "," );
				input.append( stats.getGroupCount() ).append( "," );
				input.append( stats.getProjectCount() ).append( "," );
				input.append( stats.getPluginCount() ).append( "," );
				input.append( stats.getArchetypeCount() ).append( "," );
				input.append( stats.getJarCount() ).append( "," );
				input.append( stats.getWarCount() ).append( "," );
				input.append( stats.getDeploymentCount() ).append( "," );
				input.append( stats.getDownloadCount() ).append( "\n" );
			}
        }
        
        return input;
    }
    
    private Date getDefaultStartDate()
    {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set( 1900, 1, 1, 0, 0, 0 );
        
        return cal.getTime();
    }
    
    private Date getDefaultEndDate()
    {
        return Calendar.getInstance().getTime();
    }
    
    public String execute()
        throws Exception
    {   
        if( repositoryId == null )
        {
            addFieldError( "repositoryId", "You must provide a repository id.");            
            return INPUT;
        }
        
        if( rowCount < 10 )
        {
            addFieldError( "rowCount", "Row count must be larger than 10." );
            return INPUT;
        }
        
        List<RepositoryProblem> problemArtifacts =
            dao.getRepositoryProblemDAO().queryRepositoryProblems( configureConstraint() );

        String contextPath =
            request.getRequestURL().substring( 0, request.getRequestURL().indexOf( request.getRequestURI() ) );
        RepositoryProblem problemArtifact;
        RepositoryProblemReport problemArtifactReport;
        for ( int i = 0; i < problemArtifacts.size(); i++ )
        {
            problemArtifact = (RepositoryProblem) problemArtifacts.get( i );
            problemArtifactReport = new RepositoryProblemReport( problemArtifact );

            problemArtifactReport.setGroupURL( contextPath + "/browse/" + problemArtifact.getGroupId() );
            problemArtifactReport.setArtifactURL(
                contextPath + "/browse/" + problemArtifact.getGroupId() + "/" + problemArtifact.getArtifactId() );

            addToList( problemArtifactReport );
            
            // retained the reports list because this is the datasource for the jasper report            
            reports.add( problemArtifactReport );
        }
        
        if ( reports.size() <= rowCount )
        {
            isLastPage = true;
        }
        else
        {
            reports.remove( rowCount );
        }

        prev = request.getRequestURL() + "?page=" + ( page - 1 ) + "&rowCount=" + rowCount + "&groupId=" + groupId +
            "&repositoryId=" + repositoryId;
        next = request.getRequestURL() + "?page=" + ( page + 1 ) + "&rowCount=" + rowCount + "&groupId=" + groupId +
            "&repositoryId=" + repositoryId;

        if ( reports.size() == 0 && page == 1 )
        {
            return BLANK;
        }
        else if ( isJasperPresent() )
        {
            return "jasper";
        }
        else
        {
            return SUCCESS;
        }
    }
    
    private static boolean isJasperPresent()
    {
        if ( jasperPresent == null )
        {
            try
            {
                Class.forName( "net.sf.jasperreports.engine.JRExporterParameter" );
                jasperPresent = Boolean.TRUE;
            }
            catch ( NoClassDefFoundError e )
            {
                jasperPresent = Boolean.FALSE;
            }
            catch ( ClassNotFoundException e )
            {
                jasperPresent = Boolean.FALSE;
            }
        }
        return jasperPresent.booleanValue();
    }

    private Constraint configureConstraint()
    {
        Constraint constraint;

        range[0] = ( page - 1 ) * rowCount;
        range[1] = ( page * rowCount ) + 1; // Add 1 to check if it's the last page or not.

        if ( groupId != null && ( !groupId.equals( "" ) ) )
        {
            if ( repositoryId != null && ( !repositoryId.equals( "" ) && !repositoryId.equals( ALL_REPOSITORIES ) ) )
            {
                constraint = new RepositoryProblemConstraint( range, groupId, repositoryId );
            }
            else
            {
                constraint = new RepositoryProblemByGroupIdConstraint( range, groupId );
            }
        }
        else if ( repositoryId != null && ( !repositoryId.equals( "" ) && !repositoryId.equals( ALL_REPOSITORIES ) ) )
        {
            constraint = new RepositoryProblemByRepositoryIdConstraint( range, repositoryId );
        }
        else
        {
            constraint = new RangeConstraint( range, "repositoryId" );
        }

        return constraint;
    }
    
    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
    
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_ACCESS_REPORT, Resource.GLOBAL );
    
        return bundle;
    }
    
    private void addToList( RepositoryProblemReport repoProblemReport )
    {
        List<RepositoryProblemReport> problemsList = null;
        
        if ( repositoriesMap.containsKey( repoProblemReport.getRepositoryId() ) )
        {
            problemsList = ( List<RepositoryProblemReport> ) repositoriesMap.get( repoProblemReport.getRepositoryId() );
        }
        else
        {
            problemsList = new ArrayList<RepositoryProblemReport>();
            repositoriesMap.put( repoProblemReport.getRepositoryId(), problemsList );
        }
        
        problemsList.add( repoProblemReport );
    }

    public void setServletRequest( HttpServletRequest request )
    {
        this.request = request;
    }

    public List<RepositoryProblemReport> getReports()
    {
        return reports;
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

    public String getPrev()
    {
        return prev;
    }

    public String getNext()
    {
        return next;
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

    public boolean getIsLastPage()
    {
        return isLastPage;
    }

    public void setRepositoriesMap( Map<String, List<RepositoryProblemReport>> repositoriesMap )
    {
    	this.repositoriesMap = repositoriesMap;
    }
    
    public Map<String, List<RepositoryProblemReport>> getRepositoriesMap()
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
    
    public int getReposSize()
    {
        return reposSize;
    }

    public void setReposSize( int reposSize )
    {
        this.reposSize = reposSize;
    }

    public String getSelectedRepo()
    {
        return selectedRepo;
    }

    public void setSelectedRepo( String selectedRepo )
    {
        this.selectedRepo = selectedRepo;
    }

    public DataLimits getLimits()
    {
        return limits;
    }

    public void setLimits( DataLimits limits )
    {
        this.limits = limits;
    }
    
    public InputStream getInputStream()
    {
    	return inputStream;
    }
}
