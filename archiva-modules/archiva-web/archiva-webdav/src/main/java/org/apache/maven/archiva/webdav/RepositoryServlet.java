package org.apache.maven.archiva.webdav;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.managed.ManagedRepository;
import org.apache.archiva.admin.repository.managed.ManagedRepositoryAdmin;
import org.apache.archiva.security.ServletAuthenticator;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavRequestImpl;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.WebdavResponseImpl;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationEvent;
import org.apache.maven.archiva.configuration.ConfigurationListener;
import org.codehaus.redback.integration.filter.authentication.HttpAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * RepositoryServlet
 */
public class RepositoryServlet
    extends AbstractWebdavServlet
    implements ConfigurationListener
{
    private Logger log = LoggerFactory.getLogger( RepositoryServlet.class );

    private ArchivaConfiguration configuration;

    private ManagedRepositoryAdmin managedRepositoryAdmin;

    private Map<String, ManagedRepository> repositoryMap;

    private DavLocatorFactory locatorFactory;

    private DavResourceFactory resourceFactory;

    private DavSessionProvider sessionProvider;

    private final Object reloadLock = new Object();

    public void init( javax.servlet.ServletConfig servletConfig )
        throws ServletException
    {
        super.init( servletConfig );
        try
        {
            initServers( servletConfig );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            throw new ServletException( e.getMessage(), e );
        }
    }

    /**
     * Service the given request. This method has been overridden and copy/pasted to allow better exception handling and
     * to support different realms
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws java.io.IOException
     */
    @Override
    protected void service( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        WebdavRequest webdavRequest = new WebdavRequestImpl( request, getLocatorFactory() );
        // DeltaV requires 'Cache-Control' header for all methods except 'VERSION-CONTROL' and 'REPORT'.
        int methodCode = DavMethods.getMethodCode( request.getMethod() );
        boolean noCache = DavMethods.isDeltaVMethod( webdavRequest ) && !( DavMethods.DAV_VERSION_CONTROL == methodCode
            || DavMethods.DAV_REPORT == methodCode );
        WebdavResponse webdavResponse = new WebdavResponseImpl( response, noCache );
        DavResource resource = null;

        try
        {
            // make sure there is a authenticated user
            if ( !getDavSessionProvider().attachSession( webdavRequest ) )
            {
                return;
            }

            // check matching if=header for lock-token relevant operations
            resource =
                getResourceFactory().createResource( webdavRequest.getRequestLocator(), webdavRequest, webdavResponse );

            if ( !isPreconditionValid( webdavRequest, resource ) )
            {
                webdavResponse.sendError( DavServletResponse.SC_PRECONDITION_FAILED );
                return;
            }
            if ( !execute( webdavRequest, webdavResponse, methodCode, resource ) )
            {
                super.service( request, response );
            }

        }
        catch ( UnauthorizedDavException e )
        {
            webdavResponse.setHeader( "WWW-Authenticate", getAuthenticateHeaderValue( e.getRepositoryName() ) );
            webdavResponse.sendError( e.getErrorCode(), e.getStatusPhrase() );
        }
        catch ( BrowserRedirectException e )
        {
            response.sendRedirect( e.getLocation() );
        }
        catch ( DavException e )
        {
            if ( e.getErrorCode() == HttpServletResponse.SC_UNAUTHORIZED )
            {
                final String msg = "Should throw " + UnauthorizedDavException.class.getName();
                log.error( msg );
                webdavResponse.sendError( e.getErrorCode(), msg );
            }
            else if ( e.getCause() != null )
            {
                webdavResponse.sendError( e.getErrorCode(), e.getCause().getMessage() );
            }
            else
            {
                webdavResponse.sendError( e.getErrorCode(), e.getMessage() );
            }
        }
        finally
        {
            getDavSessionProvider().releaseSession( webdavRequest );
        }
    }

    public synchronized void initServers( ServletConfig servletConfig )
        throws RepositoryAdminException
    {
        WebApplicationContext wac =
            WebApplicationContextUtils.getRequiredWebApplicationContext( servletConfig.getServletContext() );

        configuration = wac.getBean( "archivaConfiguration#default", ArchivaConfiguration.class );
        configuration.addListener( this );

        managedRepositoryAdmin = wac.getBean( ManagedRepositoryAdmin.class );

        repositoryMap = managedRepositoryAdmin.getManagedRepositoriesAsMap();

        for ( ManagedRepository repo : repositoryMap.values() )
        {
            File repoDir = new File( repo.getLocation() );

            if ( !repoDir.exists() )
            {
                if ( !repoDir.mkdirs() )
                {
                    // Skip invalid directories.
                    log( "Unable to create missing directory for " + repo.getLocation() );
                    continue;
                }
            }
        }

        resourceFactory = wac.getBean( "davResourceFactory#archiva", DavResourceFactory.class );
        locatorFactory = new ArchivaDavLocatorFactory();

        ServletAuthenticator servletAuth = wac.getBean( ServletAuthenticator.class );
        HttpAuthenticator httpAuth = wac.getBean( "httpAuthenticator#basic", HttpAuthenticator.class );

        sessionProvider = new ArchivaDavSessionProvider( servletAuth, httpAuth );

        log.info( "initServers done" );
    }

    public void configurationEvent( ConfigurationEvent event )
    {
        if ( event.getType() == ConfigurationEvent.SAVED )
        {
            try
            {
                initRepositories();
            }
            catch ( RepositoryAdminException e )
            {
                log.error( e.getMessage(), e );
                throw new RuntimeException( e.getMessage(), e );
            }
        }
    }

    private void initRepositories()
        throws RepositoryAdminException
    {
        synchronized ( repositoryMap )
        {
            repositoryMap.clear();
            repositoryMap.putAll( managedRepositoryAdmin.getManagedRepositoriesAsMap() );
        }

        synchronized ( reloadLock )
        {
            initServers( getServletConfig() );
        }
    }

    public synchronized ManagedRepository getRepository( String prefix )
        throws RepositoryAdminException
    {
        if ( repositoryMap.isEmpty() )
        {
            repositoryMap.putAll( managedRepositoryAdmin.getManagedRepositoriesAsMap() );
        }
        return repositoryMap.get( prefix );
    }

    ArchivaConfiguration getConfiguration()
    {
        return configuration;
    }

    protected boolean isPreconditionValid( final WebdavRequest request, final DavResource davResource )
    {
        // check for read or write access to the resource when resource-based permission is implemented

        return true;
    }

    public DavSessionProvider getDavSessionProvider()
    {
        return sessionProvider;
    }

    public void setDavSessionProvider( final DavSessionProvider davSessionProvider )
    {
        this.sessionProvider = davSessionProvider;
    }

    public DavLocatorFactory getLocatorFactory()
    {
        return locatorFactory;
    }

    public void setLocatorFactory( final DavLocatorFactory davLocatorFactory )
    {
        locatorFactory = davLocatorFactory;
    }

    public DavResourceFactory getResourceFactory()
    {
        return resourceFactory;
    }

    public void setResourceFactory( final DavResourceFactory davResourceFactory )
    {
        resourceFactory = davResourceFactory;
    }

    public String getAuthenticateHeaderValue()
    {
        throw new UnsupportedOperationException();
    }

    public String getAuthenticateHeaderValue( String repository )
    {
        return "Basic realm=\"Repository Archiva Managed " + repository + " Repository\"";
    }

    @Override
    public void destroy()
    {
        configuration.removeListener( this );

        resourceFactory = null;
        configuration = null;
        locatorFactory = null;
        sessionProvider = null;
        repositoryMap.clear();
        repositoryMap = null;

        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext( getServletContext() );

        if ( wac instanceof ConfigurableApplicationContext )
        {
            ( (ConfigurableApplicationContext) wac ).close();
        }
        super.destroy();
    }
}
