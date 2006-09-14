package org.apache.maven.archiva.web.servlet;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.xwork.PlexusLifecycleListener;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * PlexusComponentServlet - This is merely a servlet facade against a loaded
 * plexus component called foo
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class PlexusComponentServlet
    implements Servlet
{
    private PlexusContainer plexus;

    private PlexusServlet servletProxy;

    private boolean isInitialized = false;

    public void destroy()
    {
        if ( isInitialized )
        {
            servletProxy.servletDestroy();
        }
    }

    public ServletConfig getServletConfig()
    {
        if ( isInitialized )
        {
            return servletProxy.getServletConfig();
        }

        return null;
    }

    public String getServletInfo()
    {
        if ( isInitialized )
        {
            return servletProxy.getServletInfo();
        }

        return null;
    }

    public void init( ServletConfig config )
        throws ServletException
    {
        isInitialized = false;

        plexus = (PlexusContainer) config.getServletContext().getAttribute( PlexusLifecycleListener.KEY );

        String componentKey = config.getInitParameter( "key" );

        try
        {
            Object obj = plexus.lookup( PlexusServlet.ROLE, componentKey );
            if ( !( obj instanceof PlexusServlet ) )
            {
                throw new ServletException( "Class " + obj.getClass().getName() + " does not implement "
                    + PlexusServlet.class.getName() );
            }

            servletProxy = (PlexusServlet) obj;
            servletProxy.setServletConfig( config );

            isInitialized = true;
        }
        catch ( ComponentLookupException e )
        {
            throw new ServletException( "Unable to initialize PlexusComponentServlet.", e );
        }
    }

    public void service( ServletRequest req, ServletResponse res )
        throws ServletException, IOException
    {
        if ( !isInitialized )
        {
            throw new ServletException( "PlexusComponentServlet is not initialized correctly!" );
        }

        if ( !( req instanceof HttpServletRequest ) )
        {
            throw new ServletException( "PlexusComponentServlet can only handle HttpServletRequests." );
        }

        if ( !( res instanceof HttpServletResponse ) )
        {
            throw new ServletException( "PlexusComponentServlet can only handle HttpServletResponse." );
        }

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        servletProxy.servletRequest( request, response );
    }
}
