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
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditManager;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.archiva.security.AccessDeniedException;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.archiva.security.PrincipalNotFoundException;
import org.apache.archiva.security.UserRepositories;
import org.apache.maven.archiva.web.action.AbstractActionSupport;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Controller( "viewAuditLogReport" )
@Scope( "prototype" )
public class ViewAuditLogReportAction
    extends AbstractActionSupport
    implements SecureAction, ServletRequestAware, Preparable
{
    protected HttpServletRequest request;

    @Inject
    private UserRepositories userRepositories;

    @Inject
    private AuditManager auditManager;

    private String repository;

    private List<String> repositories;

    private String groupId;

    private String artifactId;

    private String startDate;

    private String endDate;

    private int rowCount = 30;

    private int page = 1;

    protected boolean isLastPage = true;

    private List<AuditEvent> auditLogs;

    private static final String ALL_REPOSITORIES = "all";

    private String initial = "true";

    private String headerName;

    private static final String HEADER_LATEST_EVENTS = "Latest Events";

    private static final String HEADER_RESULTS = "Results";

    private String[] datePatterns =
    new String[]{ "MM/dd/yy", "MM/dd/yyyy", "MMMMM/dd/yyyy", "MMMMM/dd/yy", "dd MMMMM yyyy", "dd/MM/yy",
    "dd/MM/yyyy", "yyyy/MM/dd", "yyyy-MM-dd", "yyyy-dd-MM", "MM-dd-yyyy", "MM-dd-yy" };



    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        // TODO: should require this, but for now we trust in the list of repositories
//        bundle.setRequiresAuthentication( true );
//        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_VIEW_AUDIT_LOG );

        return bundle;
    }

    public void setServletRequest( HttpServletRequest request )
    {
        this.request = request;
    }

    @SuppressWarnings( "unchecked" )
    public void prepare()
        throws Exception
    {
        repositories = new ArrayList<String>();
        repositories.add( ALL_REPOSITORIES );
        List<String> repos = getManagableRepositories();
        repositories.addAll( repos );

        auditLogs = null;
        groupId = "";
        artifactId = "";
        repository = "";

        if ( Boolean.parseBoolean( initial ) )
        {
            headerName = HEADER_LATEST_EVENTS;
        }
        else
        {
            headerName = HEADER_RESULTS;
        }

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            auditLogs = auditManager.getMostRecentAuditEvents( repositorySession.getRepository(), repos );
        }
        finally
        {
            repositorySession.close();
        }
    }

    public String execute()
        throws Exception
    {
        Date startDateInDF = null;
        Date endDateInDF = null;
        if ( !StringUtils.isEmpty( startDate ) )
        {
            startDateInDF = DateUtils.parseDate( startDate, datePatterns );
        }

        if ( !StringUtils.isEmpty( endDate ) )
        {
            endDateInDF = DateUtils.parseDate( endDate, datePatterns );
            Calendar cal = Calendar.getInstance();
            cal.setTime( endDateInDF );
            cal.set( Calendar.HOUR, 23 );
            cal.set( Calendar.MINUTE, 59 );
            cal.set( Calendar.SECOND, 59 );

            endDateInDF = cal.getTime();
        }

        Collection<String> repos = getManagableRepositories();
        if ( !repository.equals( ALL_REPOSITORIES ) )
        {
            if ( repos.contains( repository ) )
            {
                repos = Collections.singletonList( repository );
            }
            else
            {
                repos = Collections.emptyList();
            }
        }

        if ( StringUtils.isEmpty( groupId ) && !StringUtils.isEmpty( artifactId ) )
        {
            // Until we store the full artifact metadata in the audit event, we can't query by these individually
            addActionError( "If you specify an artifact ID, you must specify a group ID" );
            auditLogs = null;
            return INPUT;
        }

        String resource = null;
        if ( !StringUtils.isEmpty( groupId ) )
        {
            String groupIdAsPath = groupId.replace( '.', '/' );
            if ( StringUtils.isEmpty( artifactId ) )
            {
                resource = groupIdAsPath;
            }
            else
            {
                resource = groupIdAsPath + "/" + artifactId;
            }
        }

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            auditLogs =
                auditManager.getAuditEventsInRange( repositorySession.getRepository(), repos, resource, startDateInDF,
                                                    endDateInDF );
        }
        finally
        {
            repositorySession.close();
        }

        headerName = HEADER_RESULTS;

        if ( auditLogs.isEmpty() )
        {
            addActionError( "No audit logs found." );
            initial = "true";
            return SUCCESS;
        }
        else
        {
            initial = "false";
            return paginate();
        }
    }

    private String paginate()
    {
        int rowCount = getRowCount();
        int extraPage = ( auditLogs.size() % rowCount ) != 0 ? 1 : 0;
        int totalPages = ( auditLogs.size() / rowCount ) + extraPage;

        int currentPage = getPage();
        if ( currentPage > totalPages )
        {
            addActionError(
                "Error encountered while generating report :: The requested page exceeds the total number of pages." );
            return ERROR;
        }

        if ( currentPage == totalPages )
        {
            isLastPage = true;
        }
        else
        {
            isLastPage = false;
        }

        int start = rowCount * ( currentPage - 1 );
        int end = ( start + rowCount ) - 1;

        if ( end >= auditLogs.size() )
        {
            end = auditLogs.size() - 1;
        }

        auditLogs = auditLogs.subList( start, end + 1 );

        return SUCCESS;
    }

    private List<String> getManagableRepositories()
    {
        try
        {
            return userRepositories.getManagableRepositoryIds( getPrincipal() );
        }
        catch ( PrincipalNotFoundException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( AccessDeniedException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( ArchivaSecurityException e )
        {
            log.warn( e.getMessage(), e );
        }
        return Collections.emptyList();
    }

    public String getRepository()
    {
        return repository;
    }

    public void setRepository( String repository )
    {
        this.repository = repository;
    }

    public List<String> getRepositories()
    {
        return repositories;
    }

    public void setRepositories( List<String> repositories )
    {
        this.repositories = repositories;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public List<AuditEvent> getAuditLogs()
    {
        return auditLogs;
    }

    public int getRowCount()
    {
        return rowCount;
    }

    public void setRowCount( int rowCount )
    {
        this.rowCount = rowCount;
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

    public int getPage()
    {
        return page;
    }

    public void setPage( int page )
    {
        this.page = page;
    }

    public boolean getIsLastPage()
    {
        return isLastPage;
    }

    public void setIsLastPage( boolean isLastPage )
    {
        this.isLastPage = isLastPage;
    }

    public String getInitial()
    {
        return initial;
    }

    public void setInitial( String initial )
    {
        this.initial = initial;
    }

    public String getHeaderName()
    {
        return headerName;
    }

    public void setHeaderName( String headerName )
    {
        this.headerName = headerName;
    }
}
