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

package org.apache.maven.archiva.webdav.servlet.multiplexed;

import org.apache.maven.archiva.webdav.DavServerComponent;
import org.apache.maven.archiva.webdav.DavServerException;
import org.apache.maven.archiva.webdav.DavServerManager;
import org.apache.maven.archiva.webdav.servlet.AbstractWebDavServlet;
import org.apache.maven.archiva.webdav.servlet.DavServerRequest;
import org.apache.maven.archiva.webdav.util.WrappedRepositoryRequest;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Iterator;

/**
 * <p>
 * MultiplexedWebDavServlet - and abstracted multiplexed webdav servlet.
 * </p>
 * 
 * <p>
 * Implementations of this servlet should override the {@link #initServers} method and create all of the
 * appropriate DavServerComponents needed using the {@link DavServerManager} obtained via the {@link #getDavManager()}
 * method.
 * </p>
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: MultiplexedWebDavServlet.java 6000 2007-03-04 22:01:49Z joakime $
 */
public abstract class MultiplexedWebDavServlet
    extends AbstractWebDavServlet
{
    private boolean useIndexHtml = false;

    public void init( ServletConfig config )
        throws ServletException
    {
        super.init( config );

        this.useIndexHtml = getUseIndexHtml( config );

        try
        {
            initServers( config );
        }
        catch ( DavServerException e )
        {
            throw new ServletException( e );
        }
    }

    /**
     * Create any DavServerComponents here.
     * Use the {@link #createServer(String, File, ServletConfig)} method to create your servers.
     * 
     * @param config the config to use.
     * @throws DavServerException if there was a problem initializing the server components.
     */
    public abstract void initServers( ServletConfig config )
        throws DavServerException;

    public DavServerComponent createServer( String prefix, File rootDirectory, ServletConfig config )
        throws DavServerException
    {
        DavServerComponent serverComponent = davManager.createServer( prefix, rootDirectory );
        serverComponent.setUseIndexHtml( useIndexHtml );
        serverComponent.init( config );
        return serverComponent;
    }

    protected void service( HttpServletRequest httpRequest, HttpServletResponse httpResponse )
        throws ServletException, IOException
    {
        DavServerRequest davRequest = new MultiplexedDavServerRequest( new WrappedRepositoryRequest( httpRequest ) );

        DavServerComponent davServer = davManager.getServer( davRequest.getPrefix() );

        if ( davServer == null )
        {
            String errorMessage = "[" + davRequest.getPrefix() + "] Not Found (Likely Unconfigured).";
            httpResponse.sendError( HttpURLConnection.HTTP_NOT_FOUND, errorMessage );
            return;
        }

        requestDebug( httpRequest );

        if ( !isAuthenticated( davRequest, httpResponse ) )
        {
            return;
        }

        if ( !isAuthorized( davRequest, httpResponse ) )
        {
            return;
        }

        try
        {
            davServer.process( davRequest, httpResponse );
        }
        catch ( DavServerException e )
        {
            throw new ServletException( "Unable to process request.", e );
        }
    }
    
    public void setUseIndexHtml( boolean useIndexHtml )
    {
        for ( Iterator it = davManager.getServers().iterator(); it.hasNext(); )
        {
            DavServerComponent davServer = (DavServerComponent) it.next();
            davServer.setUseIndexHtml( useIndexHtml );
        }
    }
}
