package org.apache.archiva.redback.integration.filter.authentication;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.system.SecuritySystemConstants;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * HttpAuthenticator
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public abstract class HttpAuthenticator
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    protected SecuritySystem securitySystem;

    /**
     * The Public Face of the Authenticator.
     *
     * @throws MustChangePasswordException
     * @throws AccountLockedException
     */
    public AuthenticationResult authenticate( AuthenticationDataSource ds, HttpSession httpSession )
        throws AuthenticationException, AccountLockedException, MustChangePasswordException
    {
        try
        {
            SecuritySession securitySession = securitySystem.authenticate( ds );

            setSecuritySession( securitySession, httpSession );

            return securitySession.getAuthenticationResult().user( securitySession.getUser() );
        }
        catch ( AuthenticationException e )
        {
            String msg = "Unable to authenticate user: " + ds;
            log.info( msg, e );
            throw new HttpAuthenticationException( msg, e );
        }
        catch ( UserNotFoundException e )
        {
            log.info( "Login attempt against unknown user: {}", ds );
            throw new HttpAuthenticationException( "User name or password invalid." );
        }
    }

    /**
     * Entry point for a Filter.
     *
     * @param request
     * @param response
     * @throws AuthenticationException
     */
    public void authenticate( HttpServletRequest request, HttpServletResponse response )
        throws AuthenticationException
    {
        try
        {
            AuthenticationResult result = getAuthenticationResult( request, response );

            if ( ( result == null ) || ( !result.isAuthenticated() ) )
            {
                throw new HttpAuthenticationException( "You are not authenticated." );
            }
        }
        catch ( AccountLockedException e )
        {
            throw new HttpAuthenticationException( "Your account is locked.", e );
        }
        catch ( MustChangePasswordException e )
        {
            throw new HttpAuthenticationException( "You must change your password.", e );
        }

    }

    /**
     * Issue a Challenge Response back to the HTTP Client.
     *
     * @param request
     * @param response
     * @param realmName
     * @param exception
     * @throws IOException
     */
    public abstract void challenge( HttpServletRequest request, HttpServletResponse response, String realmName,
                                    AuthenticationException exception )
        throws IOException;

    /**
     * Parse the incoming request and return an AuthenticationResult.
     *
     * @param request
     * @param response
     * @return null if no http auth credentials, or the actual authentication result based on the credentials.
     * @throws AuthenticationException
     * @throws MustChangePasswordException
     * @throws AccountLockedException
     */
    public abstract AuthenticationResult getAuthenticationResult( HttpServletRequest request,
                                                                  HttpServletResponse response )
        throws AuthenticationException, AccountLockedException, MustChangePasswordException;


    public User getSessionUser( HttpSession httpSession )
    {
        return (User) httpSession.getAttribute( SecuritySession.USERKEY );
    }

    public boolean isAlreadyAuthenticated( HttpSession httpSession )
    {
        User user = getSessionUser( httpSession );

        return ( ( user != null ) && !user.isLocked() && !user.isPasswordChangeRequired() );
    }

    public SecuritySession getSecuritySession( HttpSession httpSession )
    {
        SecuritySession securitySession = (SecuritySession) httpSession.getAttribute( SecuritySession.SESSION_KEY );
        if ( securitySession != null )
        {
            return securitySession;
        }
        return (SecuritySession) httpSession.getAttribute( SecuritySystemConstants.SECURITY_SESSION_KEY );

    }


    public void setSecuritySession( SecuritySession session, HttpSession httpSession )
    {
        httpSession.setAttribute( SecuritySession.SESSION_KEY, session );
        httpSession.setAttribute( SecuritySession.USERKEY, session.getUser() );
    }

    public void setSessionUser( User user, HttpSession httpSession )
    {
        httpSession.setAttribute( SecuritySession.SESSION_KEY, null );
        httpSession.setAttribute( SecuritySession.USERKEY, user );
    }

    public String storeDefaultUser( String principal, HttpSession httpSession )
    {
        httpSession.setAttribute( SecuritySession.SESSION_KEY, null );
        httpSession.setAttribute( SecuritySession.USERKEY, null );

        if ( StringUtils.isEmpty( principal ) )
        {
            return null;
        }

        try
        {
            User user = securitySystem.getUserManager().findUser( principal );
            httpSession.setAttribute( SecuritySession.USERKEY, user );

            return user.getUsername();

        }
        catch ( UserNotFoundException e )
        {
            log.warn( "Default User '" + principal + "' not found.", e );
            return null;
        }
    }
}
