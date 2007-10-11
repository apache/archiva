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
import org.apache.maven.archiva.configuration.ConfigurationEvent;
import org.apache.maven.archiva.configuration.ConfigurationListener;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.AuthorizationResult;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.xwork.filter.authentication.HttpAuthenticator;
import org.codehaus.plexus.webdav.DavServerComponent;
import org.codehaus.plexus.webdav.DavServerException;
import org.codehaus.plexus.webdav.DavServerManager;
import org.codehaus.plexus.webdav.servlet.DavServerRequest;
import org.codehaus.plexus.webdav.servlet.multiplexed.MultiplexedWebDavServlet;
import org.codehaus.plexus.webdav.util.WebdavMethodUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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
    implements ConfigurationListener
{
    private SecuritySystem securitySystem;

    private HttpAuthenticator httpAuth;

    private AuditLog audit;

    private ArchivaConfiguration configuration;

    private Map<String, ManagedRepositoryConfiguration> repositoryMap;

    public synchronized void initComponents()
        throws ServletException
    {
        super.initComponents();

        securitySystem = (SecuritySystem) lookup( SecuritySystem.ROLE );
        httpAuth = (HttpAuthenticator) lookup( HttpAuthenticator.ROLE, "basic" );
        audit = (AuditLog) lookup( AuditLog.ROLE );

        configuration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName() );
        configuration.addListener( this );

        repositoryMap = configuration.getConfiguration().getManagedRepositoriesAsMap();
    }

    public synchronized void initServers( ServletConfig servletConfig )
        throws DavServerException
    {
        for ( ManagedRepositoryConfiguration repo : repositoryMap.values() )
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

            DavServerComponent server = createServer( repo.getId(), repoDir, servletConfig );

            server.addListener( audit );
        }
    }

    public synchronized ManagedRepositoryConfiguration getRepository( String prefix )
    {
        if ( repositoryMap.isEmpty() )
        {
            repositoryMap = configuration.getConfiguration().getManagedRepositoriesAsMap();
        }
        return repositoryMap.get( prefix );
    }

    private String getRepositoryName( DavServerRequest request )
    {
        ManagedRepositoryConfiguration repoConfig = getRepository( request.getPrefix() );
        if ( repoConfig == null )
        {
            return "Unknown";
        }

        return repoConfig.getName();
    }

    public boolean isAuthenticated( DavServerRequest davRequest, HttpServletResponse response )
        throws ServletException, IOException
    {
        HttpServletRequest request = davRequest.getRequest();

        // Authentication Tests.
        try
        {
            AuthenticationResult result = httpAuth.getAuthenticationResult( request, response );

            if ( result != null && !result.isAuthenticated() )
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

            AuthorizationResult authzResult =
                securitySystem.authorize( securitySession, permission, davRequest.getPrefix() );

            if ( !authzResult.isAuthorized() )
            {
                if ( authzResult.getException() != null )
                {
                    log( "Authorization Denied [ip=" + request.getRemoteAddr() + ",isWriteRequest=" + isWriteRequest +
                        ",permission=" + permission + ",repo=" + davRequest.getPrefix() + "] : " +
                        authzResult.getException().getMessage() );
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
    
    public void configurationEvent( ConfigurationEvent event )
    {
        if( event.getType() == ConfigurationEvent.SAVED )
        {
            initRepositories();
        }
    }

    private void initRepositories()
    {
        synchronized ( repositoryMap )
        {
            repositoryMap.clear();
            repositoryMap.putAll( configuration.getConfiguration().getManagedRepositoriesAsMap() );
        }
        
        DavServerManager davManager = getDavManager();
        
        synchronized ( davManager )
        {
            // Clear out the old servers.
            davManager.removeAllServers();
            
            // Create new servers.
            try
            {
                initServers( getServletConfig() );
            }
            catch ( DavServerException e )
            {
                log( "Unable to init servers: " + e.getMessage(), e );
            }
        }
    }
}
