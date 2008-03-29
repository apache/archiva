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

import com.opensymphony.webwork.interceptor.ServletRequestAware;
import com.opensymphony.xwork.Preparable;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.constraints.RangeConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryProblemByGroupIdConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryProblemByRepositoryIdConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryProblemConstraint;
import org.apache.maven.archiva.database.constraints.UniqueFieldConstraint;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.model.RepositoryProblemReport;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.xwork.interceptor.SecureAction;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="generateReport"
 */
public class GenerateReportAction
    extends PlexusActionSupport
    implements SecureAction, ServletRequestAware, Preparable
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    protected ArchivaDAO dao;

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

    public void prepare()
    {
        repositoryIds = new ArrayList<String>();
        repositoryIds.add( ALL_REPOSITORIES ); // comes first to be first in the list
        repositoryIds.addAll(
            dao.query( new UniqueFieldConstraint( RepositoryProblem.class.getName(), "repositoryId" ) ) );
    }

    public Collection<String> getRepositoryIds()
    {
        return repositoryIds;
    }

    public String execute()
        throws Exception
    {
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
}
