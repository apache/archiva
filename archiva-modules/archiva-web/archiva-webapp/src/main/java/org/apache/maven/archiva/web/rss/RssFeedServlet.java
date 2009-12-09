package org.apache.maven.archiva.web.rss;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import org.apache.archiva.metadata.repository.MetadataResolverException;
import org.apache.archiva.rss.processor.RssFeedProcessor;
import org.apache.commons.codec.Decoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.security.AccessDeniedException;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.security.ArchivaSecurityException;
import org.apache.maven.archiva.security.PrincipalNotFoundException;
import org.apache.maven.archiva.security.ServletAuthenticator;
import org.apache.maven.archiva.security.UserRepositories;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.UnauthorizedException;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.plexus.spring.PlexusToSpringUtils;
import org.codehaus.redback.integration.filter.authentication.HttpAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet for handling rss feed requests.
 * 
 * @version
 */
public class RssFeedServlet
    extends HttpServlet
{
    public static final String MIME_TYPE = "application/rss+xml; charset=UTF-8";

    private static final String COULD_NOT_GENERATE_FEED_ERROR = "Could not generate feed";

    private static final String COULD_NOT_AUTHENTICATE_USER = "Could not authenticate user";

    private static final String USER_NOT_AUTHORIZED = "User not authorized to access feed.";

    private Logger log = LoggerFactory.getLogger( RssFeedServlet.class );

    private RssFeedProcessor processor;

    private WebApplicationContext wac;

    private UserRepositories userRepositories;

    private ServletAuthenticator servletAuth;

    private HttpAuthenticator httpAuth;
    
    public void init( javax.servlet.ServletConfig servletConfig )
        throws ServletException
    {
        super.init( servletConfig );
        wac = WebApplicationContextUtils.getRequiredWebApplicationContext( servletConfig.getServletContext() );
        userRepositories =
            (UserRepositories) wac.getBean( PlexusToSpringUtils.buildSpringId( UserRepositories.class.getName() ) );
        servletAuth =
            (ServletAuthenticator) wac.getBean( PlexusToSpringUtils.buildSpringId( ServletAuthenticator.class.getName() ) );
        httpAuth =
            (HttpAuthenticator) wac.getBean( PlexusToSpringUtils.buildSpringId( HttpAuthenticator.ROLE, "basic" ) );
    }

    public void doGet( HttpServletRequest req, HttpServletResponse res )
        throws ServletException, IOException
    {
        String repoId = null;
        String groupId = null;
        String artifactId = null;
        
        String url = StringUtils.removeEnd( req.getRequestURL().toString(), "/" );          
        if( StringUtils.countMatches( StringUtils.substringAfter( url, "feeds/" ), "/" ) > 0 )
        {
            artifactId = StringUtils.substringAfterLast( url, "/" );
            groupId = StringUtils.substringBeforeLast( StringUtils.substringAfter( url, "feeds/" ), "/");
            groupId = StringUtils.replaceChars( groupId, '/', '.' );
        }
        else if( StringUtils.countMatches( StringUtils.substringAfter( url, "feeds/" ), "/" ) == 0 )
        {
            repoId = StringUtils.substringAfterLast( url, "/" );
        }
        else
        {
            res.sendError( HttpServletResponse.SC_BAD_REQUEST, "Invalid request url." );
            return;
        }        
        
        try
        {
            Map<String, String> map = new HashMap<String, String>();
            SyndFeed feed = null;
            
            if ( isAllowed( req, repoId, groupId, artifactId ) )
            {
                if ( repoId != null )
                {
                    // new artifacts in repo feed request
                    processor =
                        (RssFeedProcessor) wac.getBean( PlexusToSpringUtils.buildSpringId(
                                                                                           RssFeedProcessor.class.getName(),
                                                                                           "new-artifacts" ) );
                    map.put( RssFeedProcessor.KEY_REPO_ID, repoId );
                }
                else if ( ( groupId != null ) && ( artifactId != null ) )
                {
                    // TODO: this only works for guest - we could pass in the list of repos
                    // new versions of artifact feed request
                    processor =
                        (RssFeedProcessor) wac.getBean( PlexusToSpringUtils.buildSpringId(
                                                                                           RssFeedProcessor.class.getName(),
                                                                                           "new-versions" ) );
                    map.put( RssFeedProcessor.KEY_GROUP_ID, groupId );
                    map.put( RssFeedProcessor.KEY_ARTIFACT_ID, artifactId );
                }
            }
            else
            {
                res.sendError( HttpServletResponse.SC_UNAUTHORIZED, USER_NOT_AUTHORIZED );
                return;
            }

            feed = processor.process( map );            
            if( feed == null )
            {
                res.sendError( HttpServletResponse.SC_NO_CONTENT, "No information available." );
                return;
            }
            
            res.setContentType( MIME_TYPE );
                        
            if ( repoId != null )
            {   
                feed.setLink( req.getRequestURL().toString() );
            }
            else if ( ( groupId != null ) && ( artifactId != null ) )
            {
                feed.setLink( req.getRequestURL().toString() );                
            }

            SyndFeedOutput output = new SyndFeedOutput();
            output.output( feed, res.getWriter() );
        }
        catch ( UserNotFoundException unfe )
        {
            log.debug( COULD_NOT_AUTHENTICATE_USER, unfe );
            res.sendError( HttpServletResponse.SC_UNAUTHORIZED, COULD_NOT_AUTHENTICATE_USER );
        }
        catch ( AccountLockedException acce )
        {            
            res.sendError( HttpServletResponse.SC_UNAUTHORIZED, COULD_NOT_AUTHENTICATE_USER );
        }
        catch ( AuthenticationException authe )
        {   
            log.debug( COULD_NOT_AUTHENTICATE_USER, authe );
            res.sendError( HttpServletResponse.SC_UNAUTHORIZED, COULD_NOT_AUTHENTICATE_USER );
        }
        catch ( FeedException ex )
        {
            log.debug( COULD_NOT_GENERATE_FEED_ERROR, ex );
            res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, COULD_NOT_GENERATE_FEED_ERROR );
        }
        catch ( MustChangePasswordException e )
        {            
            res.sendError( HttpServletResponse.SC_UNAUTHORIZED, COULD_NOT_AUTHENTICATE_USER );
        }
        catch ( UnauthorizedException e )
        {
            log.debug( e.getMessage() );
            if ( repoId != null )
            {
                res.setHeader("WWW-Authenticate", "Basic realm=\"Repository Archiva Managed " + repoId + " Repository" );
            }
            else
            {
                res.setHeader("WWW-Authenticate", "Basic realm=\"Artifact " + groupId + ":" + artifactId );
            }
            
            res.sendError( HttpServletResponse.SC_UNAUTHORIZED, USER_NOT_AUTHORIZED );
        }
        catch ( MetadataResolverException e )
        {
            log.debug( COULD_NOT_GENERATE_FEED_ERROR, e );
            res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, COULD_NOT_GENERATE_FEED_ERROR );
        }
    }

    /**
     * Basic authentication.
     * 
     * @param req
     * @param repositoryId TODO
     * @param groupId TODO
     * @param artifactId TODO
     * @return
     */
    private boolean isAllowed( HttpServletRequest req, String repositoryId, String groupId, String artifactId )
        throws UserNotFoundException, AccountLockedException, AuthenticationException, MustChangePasswordException,
        UnauthorizedException
    {
        String auth = req.getHeader( "Authorization" );
        List<String> repoIds = new ArrayList<String>();

        if ( repositoryId != null )
        {
            repoIds.add( repositoryId );
        }
        else if ( artifactId != null && groupId != null )
        {
            if ( auth != null )
            {
                if ( !auth.toUpperCase().startsWith( "BASIC " ) )
                {
                    return false;
                }

                Decoder dec = new Base64();
                String usernamePassword = "";

                try
                {
                    usernamePassword = new String( (byte[]) dec.decode( auth.substring( 6 ).getBytes() ) );
                }
                catch ( DecoderException ie )
                {
                    log.warn( "Error decoding username and password.", ie.getMessage() );
                }

                if ( usernamePassword == null || usernamePassword.trim().equals( "" ) )
                {
                    repoIds = getObservableRepos( UserManager.GUEST_USERNAME );
                }
                else
                {
                    String[] userCredentials = usernamePassword.split( ":" );
                    repoIds = getObservableRepos( userCredentials[0] );
                }
            }
            else
            {
                repoIds = getObservableRepos( UserManager.GUEST_USERNAME );
            }
        }
        else
        {
            return false;
        }

        for ( String repoId : repoIds )
        {
            try
            {
                AuthenticationResult result = httpAuth.getAuthenticationResult( req, null );
                SecuritySession securitySession = httpAuth.getSecuritySession( req.getSession( true ) );

                if ( servletAuth.isAuthenticated( req, result )
                    && servletAuth.isAuthorized( req, securitySession, repoId,
                                                 ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS ) )
                {
                    return true;
                }
            }
            catch ( AuthorizationException e )
            {
                
            }
            catch ( UnauthorizedException e )
            {
             
            }
        }

        throw new UnauthorizedException( "Access denied." );
    }

    private List<String> getObservableRepos( String principal )
    {
        try
        {
            return userRepositories.getObservableRepositoryIds( principal );
        }
        catch ( PrincipalNotFoundException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( AccessDeniedException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( ArchivaSecurityException e )
        {
            log.warn( e.getMessage(), e );
        }

        return Collections.emptyList();
    }

}
