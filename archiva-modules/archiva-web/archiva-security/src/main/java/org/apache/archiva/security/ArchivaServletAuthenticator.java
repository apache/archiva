package org.apache.archiva.security;

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

import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.authorization.AuthorizationResult;
import org.apache.archiva.redback.authorization.UnauthorizedException;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.system.DefaultSecuritySession;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 *
 */
@Service( "servletAuthenticator" )
public class ArchivaServletAuthenticator
    implements ServletAuthenticator
{
    private Logger log = LoggerFactory.getLogger( ArchivaServletAuthenticator.class );

    /**
     *
     */
    @Inject
    private SecuritySystem securitySystem;

    @Override
    public boolean isAuthenticated( HttpServletRequest request, AuthenticationResult result )
        throws AuthenticationException, AccountLockedException, MustChangePasswordException
    {
        if ( result != null && !result.isAuthenticated() )
        {
            throw new AuthenticationException( "User Credentials Invalid" );
        }

        return true;
    }

    @Override
    public boolean isAuthorized( HttpServletRequest request, SecuritySession securitySession, String repositoryId,
                                 String permission )
        throws AuthorizationException, UnauthorizedException
    {
        // TODO: also check for permission to proxy the resource when MRM-579 is implemented

        AuthorizationResult authzResult = securitySystem.authorize( securitySession, permission, repositoryId );

        if ( !authzResult.isAuthorized() )
        {
            if ( authzResult.getException() != null )
            {
                log.info( "Authorization Denied [ip={},permission={},repo={}] : {}", request.getRemoteAddr(),
                          permission, repositoryId, authzResult.getException().getMessage() );

                throw new UnauthorizedException( "Access denied for repository " + repositoryId );
            }
            throw new UnauthorizedException( "User account is locked" );
        }

        return true;
    }

    @Override
    public boolean isAuthorized( String principal, String repoId, String permission )
        throws UnauthorizedException
    {
        try
        {
            User user = securitySystem.getUserManager().findUser( principal );
            if ( user == null )
            {
                throw new UnauthorizedException(
                    "The security system had an internal error - please check your system logs" );
            }
            if ( user.isLocked() )
            {
                throw new UnauthorizedException( "User account is locked." );
            }

            AuthenticationResult authn = new AuthenticationResult( true, principal, null );
            SecuritySession securitySession = new DefaultSecuritySession( authn, user );

            return securitySystem.isAuthorized( securitySession, permission, repoId );
        }
        catch ( UserNotFoundException e )
        {
            throw new UnauthorizedException( e.getMessage(), e );
        }
        catch ( AuthorizationException e )
        {
            throw new UnauthorizedException( e.getMessage(), e );
        } catch ( UserManagerException e )
        {
            throw new UnauthorizedException( e.getMessage(), e );
        }

    }


    public SecuritySystem getSecuritySystem()
    {
        return securitySystem;
    }

    public void setSecuritySystem( SecuritySystem securitySystem )
    {
        this.securitySystem = securitySystem;
    }
}
