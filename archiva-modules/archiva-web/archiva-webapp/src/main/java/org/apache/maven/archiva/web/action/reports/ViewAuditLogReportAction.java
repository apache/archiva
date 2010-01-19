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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.audit.AuditManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.apache.maven.archiva.security.AccessDeniedException;
import org.apache.maven.archiva.security.ArchivaSecurityException;
import org.apache.maven.archiva.security.PrincipalNotFoundException;
import org.apache.maven.archiva.security.UserRepositories;
import org.apache.maven.archiva.web.action.PlexusActionSupport;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;

/**
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="viewAuditLogReport"
 * instantiation-strategy="per-lookup"
 */
public class ViewAuditLogReportAction
    extends PlexusActionSupport
    implements SecureAction, ServletRequestAware, Preparable
{
    protected HttpServletRequest request;

    /**
     * @plexus.requirement
     */
    private UserRepositories userRepositories;

    private String repository;

    private List<String> repositories;

    private String groupId;

    private String artifactId;

    private String startDate;

    private String endDate;

    private int rowCount = 30;

    private int page = 1;

    private String prev;

    private String next;

    protected boolean isLastPage = true;

    private List<AuditEvent> auditLogs;

    private static final String ALL_REPOSITORIES = "all";

    protected int[] range = new int[2];

    private String initial = "true";

    private String headerName;

    private static final String HEADER_LATEST_EVENTS = "Latest Events";

    private static final String HEADER_RESULTS = "Results";

    private String[] datePatterns =
        new String[]{"MM/dd/yy", "MM/dd/yyyy", "MMMMM/dd/yyyy", "MMMMM/dd/yy", "dd MMMMM yyyy", "dd/MM/yy",
            "dd/MM/yyyy", "yyyy/MM/dd", "yyyy-MM-dd", "yyyy-dd-MM", "MM-dd-yyyy", "MM-dd-yy"};

    /**
     * @plexus.requirement
     */
    private AuditManager auditManager;

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

    @SuppressWarnings("unchecked")
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

        auditLogs = auditManager.getMostRecentAuditEvents( repos );
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

        range[0] = ( page - 1 ) * rowCount;
        range[1] = ( page * rowCount ) + 1;

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
            return INPUT;
        }

        String resource = null;
        if ( !StringUtils.isEmpty( groupId ) )
        {
            if ( StringUtils.isEmpty( artifactId ) )
            {
                resource = groupId;
            }
            else
            {
                resource = groupId + "/" + artifactId;
            }
        }

        auditLogs = auditManager.getAuditEventsInRange( repos, resource, startDateInDF, endDateInDF );

        if ( auditLogs.isEmpty() )
        {
            addActionError( "No audit logs found." );
            initial = "true";
        }
        else
        {
            initial = "false";
        }

        headerName = HEADER_RESULTS;
        paginate();

        return SUCCESS;
    }

    private void paginate()
    {
        if ( auditLogs.size() <= rowCount )
        {
            isLastPage = true;
        }
        else
        {
            isLastPage = false;
            auditLogs.remove( rowCount );
        }

        prev = request.getRequestURL() + "?page=" + ( page - 1 ) + "&rowCount=" + rowCount + "&groupId=" + groupId +
            "&artifactId=" + artifactId + "&repository=" + repository + "&startDate=" + startDate + "&endDate=" +
            endDate;

        next = request.getRequestURL() + "?page=" + ( page + 1 ) + "&rowCount=" + rowCount + "&groupId=" + groupId +
            "&artifactId=" + artifactId + "&repository=" + repository + "&startDate=" + startDate + "&endDate=" +
            endDate;

        prev = StringUtils.replace( prev, " ", "%20" );
        next = StringUtils.replace( next, " ", "%20" );
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

    public String getPrev()
    {
        return prev;
    }

    public void setPrev( String prev )
    {
        this.prev = prev;
    }

    public String getNext()
    {
        return next;
    }

    public void setNext( String next )
    {
        this.next = next;
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
