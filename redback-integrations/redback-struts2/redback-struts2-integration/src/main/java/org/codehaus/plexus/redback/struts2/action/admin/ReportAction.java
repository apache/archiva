package org.codehaus.plexus.redback.struts2.action.admin;

/*
 * Copyright 2005-2006 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.struts2.action.AbstractSecurityAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.reports.Report;
import org.codehaus.redback.integration.reports.ReportException;
import org.codehaus.redback.integration.reports.ReportManager;
import org.codehaus.redback.integration.role.RoleConstants;

import com.opensymphony.module.sitemesh.filter.PageResponseWrapper;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * ReportAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller("redback-report")
@Scope("prototype")
public class ReportAction
    extends AbstractSecurityAction
{
    /**
     *
     */
    @Inject
    private ReportManager reportManager;

    private String reportId;

    private String reportType;

    public String generate()
    {
        Report report;
        try
        {
            report = reportManager.findReport( reportId, reportType );
        }
        catch ( ReportException e )
        {
            addActionError( getText( "cannot.get.report", Arrays.asList( ( Object ) e.getMessage() ) ) );
            return ERROR;
        }

        HttpServletResponse response = ServletActionContext.getResponse();

        // HACK: Unwrap sitemesh response. (effectively disables sitemesh)
        if ( response instanceof PageResponseWrapper )
        {
            response = (HttpServletResponse) ( (PageResponseWrapper) response ).getResponse();
        }

        try
        {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            report.writeReport( os );

            response.reset();
            response.setContentType( report.getMimeType() );
            response.addHeader( "Content-Disposition",
                                "attachment; filename=" + report.getId() + "." + report.getType() );
            byte bytes[] = os.toByteArray();
            response.setContentLength( bytes.length );
            response.getOutputStream().write( bytes, 0, bytes.length );
            response.getOutputStream().flush();
            response.getOutputStream().close();

            // Don't return a result.
            return null;
        }
        catch ( ReportException e )
        {
            String emsg = getText( "cannot.generate.report" );
            addActionError( emsg );
            log.error( emsg, e );
            return ERROR;
        }
        catch ( IOException e )
        {
            String emsg = getText( "cannot.generate.report" );
            addActionError( emsg );
            log.error( emsg, e );
            return ERROR;
        }
    }

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION, Resource.GLOBAL );
        return bundle;
    }

    public String getReportId()
    {
        return reportId;
    }

    public void setReportId( String reportId )
    {
        this.reportId = reportId;
    }

    public String getReportType()
    {
        return reportType;
    }

    public void setReportType( String reportType )
    {
        this.reportType = reportType;
    }
}
