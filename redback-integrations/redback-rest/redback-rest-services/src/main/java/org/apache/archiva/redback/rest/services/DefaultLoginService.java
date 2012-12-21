package org.apache.archiva.redback.rest.services;

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

import org.apache.archiva.redback.authentication.AuthenticationConstants;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationFailureCause;
import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.keys.KeyManager;
import org.apache.archiva.redback.keys.jdo.JdoAuthenticationKey;
import org.apache.archiva.redback.keys.memory.MemoryAuthenticationKey;
import org.apache.archiva.redback.keys.memory.MemoryKeyManager;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.LoginRequest;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.services.LoginService;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author Olivier Lamy
 * @since 1.3
 */
@Service( "loginService#rest" )
public class DefaultLoginService
    implements LoginService
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private SecuritySystem securitySystem;

    private HttpAuthenticator httpAuthenticator;

    @Context
    private HttpServletRequest httpServletRequest;

    @Inject
    public DefaultLoginService( SecuritySystem securitySystem,
                                @Named( "httpAuthenticator#basic" ) HttpAuthenticator httpAuthenticator )
    {
        this.securitySystem = securitySystem;
        this.httpAuthenticator = httpAuthenticator;
    }


    public String addAuthenticationKey( String providedKey, String principal, String purpose, int expirationMinutes )
        throws RedbackServiceException
    {
        KeyManager keyManager = securitySystem.getKeyManager();
        AuthenticationKey key;

        if ( keyManager instanceof MemoryKeyManager )
        {
            key = new MemoryAuthenticationKey();
        }
        else
        {
            key = new JdoAuthenticationKey();
        }

        key.setKey( providedKey );
        key.setForPrincipal( principal );
        key.setPurpose( purpose );

        Calendar now = getNowGMT();
        key.setDateCreated( now.getTime() );

        if ( expirationMinutes >= 0 )
        {
            Calendar expiration = getNowGMT();
            expiration.add( Calendar.MINUTE, expirationMinutes );
            key.setDateExpires( expiration.getTime() );
        }

        keyManager.addKey( key );

        return key.getKey();
    }

    public Boolean ping()
        throws RedbackServiceException
    {
        return Boolean.TRUE;
    }

    public Boolean pingWithAutz()
        throws RedbackServiceException
    {
        return Boolean.TRUE;
    }

    public User logIn( LoginRequest loginRequest )
        throws RedbackServiceException
    {
        String userName = loginRequest.getUsername(), password = loginRequest.getPassword();
        PasswordBasedAuthenticationDataSource authDataSource =
            new PasswordBasedAuthenticationDataSource( userName, password );
        try
        {
            SecuritySession securitySession = securitySystem.authenticate( authDataSource );
            if ( securitySession.getAuthenticationResult().isAuthenticated() )
            {
                org.apache.archiva.redback.users.User user = securitySession.getUser();
                if ( !user.isValidated() )
                {
                    log.info( "user {} not validated", user.getUsername() );
                    return null;
                }
                User restUser = buildRestUser( user );
                restUser.setReadOnly( securitySystem.userManagerReadOnly() );
                // here create an http session
                httpAuthenticator.authenticate( authDataSource, httpServletRequest.getSession( true ) );
                return restUser;
            }
            if ( securitySession.getAuthenticationResult() != null
                && securitySession.getAuthenticationResult().getAuthenticationFailureCauses() != null )
            {
                List<ErrorMessage> errorMessages = new ArrayList<ErrorMessage>();
                for ( AuthenticationFailureCause authenticationFailureCause : securitySession.getAuthenticationResult().getAuthenticationFailureCauses() )
                {
                    if ( authenticationFailureCause.getCause() == AuthenticationConstants.AUTHN_NO_SUCH_USER )
                    {
                        errorMessages.add( new ErrorMessage( "incorrect.username.password" ) );
                    }
                    else
                    {
                        errorMessages.add( new ErrorMessage().message( authenticationFailureCause.getMessage() ) );
                    }
                }

                throw new RedbackServiceException( errorMessages );
            }
            return null;
        }
        catch ( AuthenticationException e )
        {
            throw new RedbackServiceException( e.getMessage(), Response.Status.FORBIDDEN.getStatusCode() );
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        catch ( AccountLockedException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        catch ( MustChangePasswordException e )
        {
            return buildRestUser( e.getUser() );
        }
        catch ( UserManagerException e )
        {
            log.info( "UserManagerException: {}", e.getMessage() );
            List<ErrorMessage> errorMessages =
                Arrays.asList( new ErrorMessage().message( "UserManagerException: " + e.getMessage() ) );
            throw new RedbackServiceException( errorMessages );
        }

    }

    public User isLogged()
        throws RedbackServiceException
    {
        SecuritySession securitySession = httpAuthenticator.getSecuritySession( httpServletRequest.getSession( true ) );
        Boolean isLogged = securitySession != null;
        log.debug( "isLogged {}", isLogged );
        return isLogged && securitySession.getUser() != null ? buildRestUser( securitySession.getUser() ) : null;
    }

    public Boolean logout()
        throws RedbackServiceException
    {
        HttpSession httpSession = httpServletRequest.getSession();
        if ( httpSession != null )
        {
            httpSession.invalidate();
        }
        return Boolean.TRUE;
    }

    private Calendar getNowGMT()
    {
        return Calendar.getInstance( TimeZone.getTimeZone( "GMT" ) );
    }

    private User buildRestUser( org.apache.archiva.redback.users.User user )
    {
        User restUser = new User();
        restUser.setEmail( user.getEmail() );
        restUser.setUsername( user.getUsername() );
        restUser.setPasswordChangeRequired( user.isPasswordChangeRequired() );
        restUser.setLocked( user.isLocked() );
        restUser.setValidated( user.isValidated() );
        restUser.setFullName( user.getFullName() );
        return restUser;
    }
}
