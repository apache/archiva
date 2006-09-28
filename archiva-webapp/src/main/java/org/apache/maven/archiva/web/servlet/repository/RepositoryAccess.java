package org.apache.maven.archiva.web.servlet.repository;

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

import it.could.webdav.DAVTransaction;
import it.could.webdav.DAVUtilities;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfigurationStoreException;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.web.ArchivaSecurityDefaults;
import org.apache.maven.archiva.web.servlet.AbstractPlexusServlet;
import org.codehaus.plexus.security.authentication.AuthenticationException;
import org.codehaus.plexus.security.authentication.AuthenticationResult;
import org.codehaus.plexus.security.authorization.AuthorizationException;
import org.codehaus.plexus.security.system.SecuritySession;
import org.codehaus.plexus.security.system.SecuritySystem;
import org.codehaus.plexus.security.ui.web.filter.authentication.HttpAuthenticator;
import org.codehaus.plexus.security.policy.AccountLockedException;
import org.codehaus.plexus.security.policy.MustChangePasswordException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RepositoryAccess - access read/write to the repository.
 *
 * @plexus.component role="org.apache.maven.archiva.web.servlet.PlexusServlet"
 *                   role-hint="repositoryAccess"
 * 
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @todo CACHE REPOSITORY LIST
 */
public class RepositoryAccess
    extends AbstractPlexusServlet
{
    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

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
    private ArchivaSecurityDefaults archivaSecurity;

    /**
     * List of request methods that fall into the category of 'access' or 'read' of a repository.
     * All other method requests are to be considered 'write' or 'upload' requests.
     */
    private static final List ACCESS_METHODS;

    static
    {
        ACCESS_METHODS = new ArrayList();
        ACCESS_METHODS.add( "GET" );
        ACCESS_METHODS.add( "PROPFIND" );
        ACCESS_METHODS.add( "OPTIONS" );
        ACCESS_METHODS.add( "REPORT" );
    }

    public class RequestPath
    {
        String repoId;

        String path;
    }

    private Map davRepositoryMap = new HashMap();

    public String getServletInfo()
    {
        // TODO: We could produce information about # of repositories being tracked, etc...
        return "Archiva Repository Access Servlet";
    }

    public void servletRequest( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        Configuration config;
        try
        {
            config = configurationStore.getConfigurationFromStore();
        }
        catch ( ConfigurationStoreException e )
        {
            // TODO: should be a more pretty error to user. ;-)

            throw new ServletException( "Unable to obtain configuration.", e );
        }

        RequestPath reqpath = getRepositoryPath( request.getPathInfo() );

        if ( reqpath == null )
        {
            routeToErrorPage( response, "Invalid Repository URL." );
            return;
        }

        RepositoryConfiguration repoconfig = config.getRepositoryById( reqpath.repoId );

        if ( repoconfig == null )
        {
            routeToErrorPage( response, "Invalid Repository ID." );
            return;
        }
        
        // Authentication Tests.

        AuthenticationResult result;
        try
        {
            result = httpAuth.getAuthenticationResult( request, response );

            if ( !result.isAuthenticated() )
            {
                // Must Authenticate.
                httpAuth.challenge( request, response, "Repository " + repoconfig.getName(), 
                                    new AuthenticationException("User Credentials Invalid") );
                return;
            }
        }
        catch ( AuthenticationException e )
        {
            getLogger().error( "Fatal Http Authentication Error.", e );
            throw new ServletException( "Fatal Http Authentication Error.", e );
        }
        catch ( AccountLockedException e )
        {
            httpAuth.challenge( request, response, "Repository " + repoconfig.getName(),
                                new AuthenticationException("User account is locked") );
        }
        catch ( MustChangePasswordException e )
        {
            httpAuth.challenge( request, response, "Repository " + repoconfig.getName(),
                                new AuthenticationException("You must change your password before you can attempt this again.") );
        }

        // Authorization Tests.

        boolean isWriteRequest = !ACCESS_METHODS.contains( request.getMethod().toUpperCase() );

        SecuritySession securitySession = httpAuth.getSecuritySession();
        try
        {
            String permission = ArchivaSecurityDefaults.REPOSITORY_ACCESS;

            if ( isWriteRequest )
            {
                permission = ArchivaSecurityDefaults.REPOSITORY_UPLOAD;
            }

            permission += " - " + repoconfig.getId();

            boolean isAuthorized = securitySystem.isAuthorized( securitySession, permission, repoconfig.getId() );

            if ( !isAuthorized )
            {
                // Issue HTTP Challenge.
                httpAuth.challenge( request, response, "Repository " + repoconfig.getName(), 
                                    new AuthenticationException("Authorization Denied.") );
                return;
            }
        }
        catch ( AuthorizationException e )
        {
            throw new ServletException( "Fatal Authorization Subsystem Error." );
        }

        // Allow DAV To Handle Request.

        RepositoryMapping repo = getRepositoryMapping( repoconfig );

        response.setHeader( "Server", getServletContext().getServerInfo() + " Archiva : "
            + DAVUtilities.SERVLET_SIGNATURE );

        DAVTransaction transaction = new DAVTransaction( request, response );
        try
        {
            repo.getDavProcessor().process( transaction );
        }
        catch ( RuntimeException exception )
        {
            final String header = request.getMethod() + ' ' + request.getRequestURI() + ' ' + request.getProtocol();
            getLogger().error( "Error processing: " + header );
            getLogger().error( "Exception processing DAV transaction", exception );
            throw exception;
        }
    }

    public RepositoryMapping getRepositoryMapping( RepositoryConfiguration repoconfig )
        throws IOException
    {
        RepositoryMapping repo = (RepositoryMapping) davRepositoryMap.get( repoconfig.getDirectory() );
        if ( repo == null )
        {
            repo = new RepositoryMapping( repoconfig );
            davRepositoryMap.put( repoconfig.getDirectory(), repo );
        }
        return repo;
    }

    public RequestPath getRepositoryPath( String requestPathInfo )
    {
        if ( StringUtils.isEmpty( requestPathInfo ) || StringUtils.equals( "/", requestPathInfo ) )
        {
            // Got root url.  Can't do anything with this.
            return null;
        }

        RequestPath ret = new RequestPath();

        // Find the first 'path' of the pathInfo.

        // Default: "/pathid" -> "pathid"
        ret.repoId = requestPathInfo.substring( 1 );
        ret.path = "/";

        // Find first element, if slash exists. 
        int slash = requestPathInfo.indexOf( '/', 1 );
        if ( slash > 0 )
        {
            // Filtered: "/central/org/apache/maven/" -> "central"
            ret.repoId = requestPathInfo.substring( 1, slash );

            String repoPath = requestPathInfo.substring( slash );

            if ( repoPath.endsWith( "/.." ) )
            {
                repoPath += "/";
            }

            String path = FileUtils.normalize( repoPath );
            if ( path == null )
            {
                ret.path = "/";
            }
            else
            {
                ret.path = path;
            }
        }

        return ret;
    }

    public void routeToErrorPage( HttpServletResponse response, String message )
        throws IOException
    {
        response.resetBuffer();
        /* Since the primary user of this servlet will be Maven Wagon.
         * Always return 404 on error to force the wagon to stop retrying.
         */
        response.sendError( HttpServletResponse.SC_NOT_FOUND, message );
    }
}
