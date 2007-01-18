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

import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationChangeException;
import org.apache.maven.archiva.configuration.ConfigurationChangeListener;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfigurationStoreException;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
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
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryServlet
    extends MultiplexedWebDavServlet
    implements ConfigurationChangeListener
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

    private Configuration config;

    public void initComponents()
        throws ServletException
    {
        super.initComponents();

        ConfigurationStore configurationStore;

        configurationStore = (ConfigurationStore) lookup( ConfigurationStore.ROLE );
        securitySystem = (SecuritySystem) lookup( SecuritySystem.ROLE );
        httpAuth = (HttpAuthenticator) lookup( HttpAuthenticator.ROLE, "basic" );
        audit = (AuditLog) lookup( AuditLog.ROLE );

        try
        {
            config = configurationStore.getConfigurationFromStore();
            configurationStore.addChangeListener( this );
        }
        catch ( ConfigurationStoreException e )
        {
            throw new ServletException( "Unable to obtain configuration.", e );
        }

    }

    public void initServers( ServletConfig servletConfig )
        throws DavServerException
    {
        List repositories = config.getRepositories();
        Iterator itrepos = repositories.iterator();
        while ( itrepos.hasNext() )
        {
            RepositoryConfiguration repoConfig = (RepositoryConfiguration) itrepos.next();
            DavServerComponent server = createServer( repoConfig.getUrlName(), new File( repoConfig.getDirectory() ),
                                                      servletConfig );
            server.addListener( audit );
        }
    }

    public RepositoryConfiguration getRepositoryConfiguration( DavServerRequest request )
    {
        return config.getRepositoryByUrlName( request.getPrefix() );
    }

    public String getRepositoryName( DavServerRequest request )
    {
        RepositoryConfiguration repoConfig = getRepositoryConfiguration( request );
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

            AuthorizationResult authzResult = securitySystem.authorize( securitySession, permission,
                                                                        getRepositoryConfiguration( davRequest )
                                                                            .getId() );

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
    
    public void notifyOfConfigurationChange( Configuration newConfiguration )
        throws InvalidConfigurationException, ConfigurationChangeException
    {
        config = newConfiguration;

        getDavManager().removeAllServers();

        try
        {
            initServers( getServletConfig() );
        }
        catch ( DavServerException e )
        {
            throw new ConfigurationChangeException( "Unable to process configuration change.", e );
        }
    }
}
