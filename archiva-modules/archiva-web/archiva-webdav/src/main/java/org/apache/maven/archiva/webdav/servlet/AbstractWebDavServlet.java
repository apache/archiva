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

package org.apache.maven.archiva.webdav.servlet;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.webdav.DavServerManager;
import org.codehaus.plexus.spring.PlexusToSpringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * AbstractWebDavServlet 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: AbstractWebDavServlet.java 7009 2007-10-25 23:34:43Z joakime $
 */
public abstract class AbstractWebDavServlet
    extends HttpServlet
{
    public static final String INIT_USE_INDEX_HTML = "dav.use.index.html";

    private boolean debug = false;

    protected DavServerManager davManager;

    public String getServletInfo()
    {
        return "Plexus WebDAV Servlet";
    }

    public void init( ServletConfig config )
        throws ServletException
    {
        super.init( config );

        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext( config.getServletContext() );
        davManager = (DavServerManager) wac.getBean( PlexusToSpringUtils.buildSpringId( DavServerManager.ROLE ) );
        if ( davManager == null )
        {
            throw new ServletException( "Unable to lookup davManager" );
        }
    }

    /**
     * Perform any authentication steps here.
     * 
     * If authentication fails, it is the responsibility of the implementor to issue
     * the appropriate status codes and/or challenge back on the response object, then
     * return false on the overridden version of this method.
     * 
     * To effectively not have authentication, just implement this method and always
     * return true.
     * 
     * @param davRequest the incoming dav request.
     * @param httpResponse the outgoing http response.
     * @return true if user is authenticated, false if not.
     * @throws ServletException if there was a problem performing authencation.
     * @throws IOException if there was a problem obtaining credentials or issuing challenge.
     */
    public boolean isAuthenticated( DavServerRequest davRequest, HttpServletResponse httpResponse )
        throws ServletException, IOException
    {
        // Always return true. Effectively no Authentication done.
        return true;
    }

    /**
     * Perform any authorization steps here.
     * 
     * If authorization fails, it is the responsibility of the implementor to issue
     * the appropriate status codes and/or challenge back on the response object, then
     * return false on the overridden version of this method.
     * 
     * to effectively not have authorization, just implement this method and always
     * return true.
     * 
     * @param davRequest
     * @param httpResponse
     * @return
     * @throws ServletException
     * @throws IOException
     */
    public boolean isAuthorized( DavServerRequest davRequest, HttpServletResponse httpResponse )
        throws ServletException, IOException
    {
        // Always return true. Effectively no Authorization done.
        return true;
    }

    public boolean isDebug()
    {
        return debug;
    }

    public void setDebug( boolean debug )
    {
        this.debug = debug;
    }

    protected void requestDebug( HttpServletRequest request )
    {
        if ( debug )
        {
            System.out.println( "-->>> request ----------------------------------------------------------" );
            System.out.println( "--> " + request.getScheme() + "://" + request.getServerName() + ":"
                + request.getServerPort() + request.getServletPath() );
            System.out.println( request.getMethod() + " " + request.getRequestURI()
                + ( request.getQueryString() != null ? "?" + request.getQueryString() : "" ) + " " + "HTTP/1.1" );

            Enumeration enHeaders = request.getHeaderNames();
            while ( enHeaders.hasMoreElements() )
            {
                String headerName = (String) enHeaders.nextElement();
                String headerValue = request.getHeader( headerName );
                System.out.println( headerName + ": " + headerValue );
            }

            System.out.println();

            System.out.println( "------------------------------------------------------------------------" );
        }
    }

    public abstract void setUseIndexHtml( boolean useIndexHtml );

    public boolean getUseIndexHtml( ServletConfig config )
        throws ServletException
    {
        String useIndexHtml = config.getInitParameter( INIT_USE_INDEX_HTML );

        if ( StringUtils.isEmpty( useIndexHtml ) )
        {
            return false;
        }

        return BooleanUtils.toBoolean( useIndexHtml );
    }
}
