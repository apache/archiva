package org.apache.maven.archiva.security;

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

import javax.servlet.http.HttpServletRequest;

import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.AuthorizationResult;
import org.codehaus.plexus.redback.authorization.UnauthorizedException;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.system.DefaultSecuritySession;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version
 * @plexus.component role="org.apache.maven.archiva.security.ServletAuthenticator" role-hint="default"
 */
public class ArchivaServletAuthenticator
    implements ServletAuthenticator
{
    private Logger log = LoggerFactory.getLogger( ArchivaServletAuthenticator.class );

    /**
     * @plexus.requirement
     */
    private SecuritySystem securitySystem;

    public boolean isAuthenticated( HttpServletRequest request, AuthenticationResult result )
        throws AuthenticationException, AccountLockedException, MustChangePasswordException
    {
        if ( result != null && !result.isAuthenticated() )
        {
            throw new AuthenticationException( "User Credentials Invalid" );
        }

        return true;
    }

    public boolean isAuthorized( HttpServletRequest request, SecuritySession securitySession, String repositoryId,
                                 boolean isWriteRequest )
        throws AuthorizationException, UnauthorizedException
    {
        // TODO: also check for permission to proxy the resource when MRM-579 is implemented

        String permission = ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS;

        if ( isWriteRequest )
        {
            permission = ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD;
        }

        AuthorizationResult authzResult = securitySystem.authorize( securitySession, permission, repositoryId );

        if ( !authzResult.isAuthorized() )
        {
            if ( authzResult.getException() != null )
            {
                log.info( "Authorization Denied [ip=" + request.getRemoteAddr() + ",isWriteRequest=" + isWriteRequest +
                    ",permission=" + permission + ",repo=" + repositoryId + "] : " +
                    authzResult.getException().getMessage() );

                throw new UnauthorizedException( "Access denied for repository " + repositoryId );
            }
            throw new UnauthorizedException( "User account is locked" );
        }

        return true;
    }

    public boolean isAuthorized( String principal, String repoId, boolean isWriteRequest )
        throws UnauthorizedException
    {
        try
        {
            String permission = ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS;

            if ( isWriteRequest )
            {
                permission = ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD;
            }
            
            User user = securitySystem.getUserManager().findUser( principal );
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
            throw new UnauthorizedException( e.getMessage() );
        }
        catch ( AuthorizationException e )
        {
            throw new UnauthorizedException( e.getMessage() );
        }
    }
}
