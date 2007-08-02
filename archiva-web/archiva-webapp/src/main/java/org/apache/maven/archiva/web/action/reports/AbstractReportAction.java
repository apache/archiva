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
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.model.RepositoryProblemReport;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.xwork.interceptor.SecureAction;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import com.opensymphony.webwork.interceptor.ServletRequestAware;

/**
 * Abstract reporting.
 */
public abstract class AbstractReportAction extends PlexusActionSupport implements SecureAction, ServletRequestAware
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    protected ArchivaDAO dao;

    protected Constraint constraint;

    protected HttpServletRequest request;

    protected List reports = new ArrayList();

    protected String prev;

    protected String next;

    protected int[] range = new int[2];

    protected int page = 1;

    protected int rowCount = 100;

    protected boolean isLastPage = false;

    public static final String BLANK = "blank";

    public String execute() throws Exception
    {
        range[0] = ( page - 1 ) * rowCount;
        range[1] = ( page * rowCount ) + 1; // Add 1 to check if it's the last page or not.

        configureConstraint();
        List problemArtifacts = dao.getRepositoryProblemDAO().queryRepositoryProblems( constraint );

        String contextPath =
            request.getRequestURL().substring( 0, request.getRequestURL().indexOf( request.getRequestURI() ) );
        RepositoryProblem problemArtifact;
        RepositoryProblemReport problemArtifactReport;
        for ( int i = 0; i < problemArtifacts.size(); i++ )
        {
            problemArtifact = (RepositoryProblem) problemArtifacts.get( i );
            problemArtifactReport = new RepositoryProblemReport( problemArtifact );

            problemArtifactReport.setGroupURL( contextPath + "/browse/" + problemArtifact.getGroupId() );
            problemArtifactReport.setArtifactURL( contextPath + "/browse/" + problemArtifact.getGroupId() + "/"
                            + problemArtifact.getArtifactId() );
            problemArtifactReport.setVersionURL( contextPath + "/browse/" + problemArtifact.getGroupId() + "/"
                            + problemArtifact.getArtifactId() + "/" + problemArtifact.getVersion() );

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

        prev = request.getRequestURL() + "?page=" + ( page - 1 ) + "&rowCount=" + rowCount;
        next = request.getRequestURL() + "?page=" + ( page + 1 ) + "&rowCount=" + rowCount;

        if ( reports.size() == 0 && page == 1 )
        {
            return BLANK;
        }
        else
        {
            return SUCCESS;
        }
    }

    /**
     * To be implemented by sub-reports to configure specific constraints.
     */
    abstract protected void configureConstraint();

    public void setServletRequest( HttpServletRequest request )
    {
        this.request = request;
    }

    public List getReports()
    {
        return reports;
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

    public SecureActionBundle getSecureActionBundle() throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_ACCESS_REPORT, Resource.GLOBAL );

        return bundle;
    }
}
