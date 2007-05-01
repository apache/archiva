package org.apache.maven.archiva.web.repository;

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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.codehaus.plexus.security.authentication.AuthenticationException;
import org.codehaus.plexus.security.authentication.AuthenticationResult;
import org.codehaus.plexus.security.authorization.AuthorizationException;
import org.codehaus.plexus.security.authorization.AuthorizationResult;
import org.codehaus.plexus.security.policy.AccountLockedException;
import org.codehaus.plexus.security.policy.MustChangePasswordException;
import org.codehaus.plexus.security.system.SecuritySession;
import org.codehaus.plexus.security.system.SecuritySystem;
import org.codehaus.plexus.security.ui.web.filter.authentication.HttpAuthenticator;
import org.codehaus.plexus.webdav.DavServerComponent;
import org.codehaus.plexus.webdav.DavServerException;
import org.codehaus.plexus.webdav.servlet.DavServerRequest;
import org.codehaus.plexus.webdav.servlet.multiplexed.MultiplexedWebDavServlet;
import org.codehaus.plexus.webdav.util.WebdavMethodUtil;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * RepositoryServlet
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryServlet
    extends MultiplexedWebDavServlet
    implements RegistryListener
{
    /**
     * @plexus.requirement
     */
    private SecuritySystem securitySystem;

    /**
     * @plexus.requirement role-hint="basic"
     */
    private HttpAuthenticator httpAuth;

    /**
     * @plexus.requirement
     */
    private AuditLog audit;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    public void initComponents()
        throws ServletException
    {
        super.initComponents();

        securitySystem = (SecuritySystem) lookup( SecuritySystem.ROLE );
        httpAuth = (HttpAuthenticator) lookup( HttpAuthenticator.ROLE, "basic" );
        audit = (AuditLog) lookup( AuditLog.ROLE );

        dao = (ArchivaDAO) lookup( ArchivaDAO.ROLE, "jdo" );
        configuration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName() );
        configuration.addChangeListener( this );
    }

    public void initServers( ServletConfig servletConfig )
        throws DavServerException
    {
        try
        {
            List repositories = dao.getRepositoryDAO().getRepositories();
            Iterator itrepos = repositories.iterator();
            while ( itrepos.hasNext() )
            {
                ArchivaRepository repo = (ArchivaRepository) itrepos.next();
                if ( !repo.isManaged() )
                {
                    // Skip non-managed.
                    continue;
                }

                File repoDir = new File( repo.getUrl().getPath() );

                if ( !repoDir.exists() )
                {
                    if ( !repoDir.mkdirs() )
                    {
                        // Skip invalid directories.
                        log( "Unable to create missing directory for " + repo.getUrl().getPath() );
                        continue;
                    }
                }

                DavServerComponent server = createServer( repo.getId(), repoDir, servletConfig );

                server.addListener( audit );
            }
        }
        catch ( ArchivaDatabaseException e )
        {
            throw new DavServerException( "Unable to initialized dav servers: " + e.getMessage(), e );
        }
    }

    public ArchivaRepository getRepository( DavServerRequest request )
    {
        String id = request.getPrefix();
        try
        {
            return dao.getRepositoryDAO().getRepository( id );
        }
        catch ( ObjectNotFoundException e )
        {
            log( "Unable to find repository for id [" + id + "]" );
            return null;
        }
        catch ( ArchivaDatabaseException e )
        {
            log( "Unable to find repository for id [" + id + "]: " + e.getMessage(), e );
            return null;
        }
    }

    public String getRepositoryName( DavServerRequest request )
    {
        ArchivaRepository repoConfig = getRepository( request );
        if ( repoConfig == null )
        {
            return "Unknown";
        }

        return repoConfig.getModel().getName();
    }

    public boolean isAuthenticated( DavServerRequest davRequest, HttpServletResponse response )
        throws ServletException, IOException
    {
        HttpServletRequest request = davRequest.getRequest();

        // Authentication Tests.
        try
        {
            AuthenticationResult result = httpAuth.getAuthenticationResult( request, response );

            if ( ( result != null ) && !result.isAuthenticated() )
            {
                // Must Authenticate.
                httpAuth.challenge( request, response, "Repository " + getRepositoryName( davRequest ),
                                    new AuthenticationException( "User Credentials Invalid" ) );
                return false;
            }
        }
        catch ( AuthenticationException e )
        {
            log( "Fatal Http Authentication Error.", e );
            throw new ServletException( "Fatal Http Authentication Error.", e );
        }
        catch ( AccountLockedException e )
        {
            httpAuth.challenge( request, response, "Repository " + getRepositoryName( davRequest ),
                                new AuthenticationException( "User account is locked" ) );
        }
        catch ( MustChangePasswordException e )
        {
            httpAuth.challenge( request, response, "Repository " + getRepositoryName( davRequest ),
                                new AuthenticationException( "You must change your password." ) );
        }

        return true;
    }

    public boolean isAuthorized( DavServerRequest davRequest, HttpServletResponse response )
        throws ServletException, IOException
    {
        // Authorization Tests.
        HttpServletRequest request = davRequest.getRequest();

        boolean isWriteRequest = WebdavMethodUtil.isWriteMethod( request.getMethod() );

        SecuritySession securitySession = httpAuth.getSecuritySession();
        try
        {
            String permission = ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS;

            if ( isWriteRequest )
            {
                permission = ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD;
            }

            AuthorizationResult authzResult = securitySystem.authorize( securitySession, permission, davRequest
                .getPrefix() );

            if ( !authzResult.isAuthorized() )
            {
                if ( authzResult.getException() != null )
                {
                    log( "Authorization Denied [ip=" + request.getRemoteAddr() + ",isWriteRequest=" + isWriteRequest
                        + ",permission=" + permission + "] : " + authzResult.getException().getMessage() );
                }

                // Issue HTTP Challenge.
                httpAuth.challenge( request, response, "Repository " + getRepositoryName( davRequest ),
                                    new AuthenticationException( "Authorization Denied." ) );
                return false;
            }
        }
        catch ( AuthorizationException e )
        {
            throw new ServletException( "Fatal Authorization Subsystem Error." );
        }

        return true;
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        // nothing to do
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositories( propertyName ) )
        {
            getDavManager().removeAllServers();

            try
            {
                initServers( getServletConfig() );
            }
            catch ( DavServerException e )
            {
                log( "Error restarting WebDAV server after configuration change - service disabled: " + e.getMessage(),
                     e );
            }
        }
    }
}
