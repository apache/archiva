package org.apache.archiva.webdav;

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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationEvent;
import org.apache.archiva.configuration.ConfigurationListener;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryRegistry;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * RepositoryServlet
 */
public class RepositoryServlet
    extends AbstractWebdavServlet
    implements ConfigurationListener
{
    private Logger log = LoggerFactory.getLogger( RepositoryServlet.class );

    private ArchivaConfiguration configuration;

    RepositoryRegistry repositoryRegistry;

    private DavLocatorFactory locatorFactory;

    private DavResourceFactory resourceFactory;

    private DavSessionProvider sessionProvider;

    protected final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    @Override
    public void init( ServletConfig servletConfig )
        throws ServletException
    {
        super.init( servletConfig );
        initServers( servletConfig );
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

    public void initServers( ServletConfig servletConfig ) {

        long start = System.currentTimeMillis();

        WebApplicationContext wac =
            WebApplicationContextUtils.getRequiredWebApplicationContext( servletConfig.getServletContext() );

        rwLock.writeLock().lock();
        try {
            configuration = wac.getBean("archivaConfiguration#default", ArchivaConfiguration.class);
            configuration.addListener(this);

            repositoryRegistry = wac.getBean( ArchivaRepositoryRegistry.class);
            resourceFactory = wac.getBean("davResourceFactory#archiva", DavResourceFactory.class);
            locatorFactory = new ArchivaDavLocatorFactory();

            ServletAuthenticator servletAuth = wac.getBean(ServletAuthenticator.class);
            HttpAuthenticator httpAuth = wac.getBean("httpAuthenticator#basic", HttpAuthenticator.class);

            sessionProvider = new ArchivaDavSessionProvider(servletAuth, httpAuth);
        } finally {
            rwLock.writeLock().unlock();
        }
        long end = System.currentTimeMillis();

        log.debug( "initServers done in {} ms", (end - start) );
    }

    @Override
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
            initServers( getServletConfig() );
    }

    public ManagedRepository getRepository( String prefix )
        throws RepositoryAdminException
    {
        return repositoryRegistry.getManagedRepository( prefix );
    }

    ArchivaConfiguration getConfiguration()
    {
        return configuration;
    }

    @Override
    protected boolean isPreconditionValid( final WebdavRequest request, final DavResource davResource )
    {
        // check for read or write access to the resource when resource-based permission is implemented

        return true;
    }

    @Override
    public DavSessionProvider getDavSessionProvider()
    {
        return sessionProvider;
    }

    @Override
    public void setDavSessionProvider( final DavSessionProvider davSessionProvider )
    {
        this.sessionProvider = davSessionProvider;
    }

    @Override
    public DavLocatorFactory getLocatorFactory()
    {
        return locatorFactory;
    }

    @Override
    public void setLocatorFactory( final DavLocatorFactory davLocatorFactory )
    {
        locatorFactory = davLocatorFactory;
    }

    @Override
    public DavResourceFactory getResourceFactory()
    {
        return resourceFactory;
    }

    @Override
    public void setResourceFactory( final DavResourceFactory davResourceFactory )
    {
        resourceFactory = davResourceFactory;
    }

    @Override
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
        rwLock.writeLock().lock();
        try {
            configuration.removeListener(this);

            resourceFactory = null;
            configuration = null;
            locatorFactory = null;
            sessionProvider = null;

            WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());

            if (wac instanceof ConfigurableApplicationContext) {
                ((ConfigurableApplicationContext) wac).close();
            }
            super.destroy();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
