package org.apache.archiva.redback.system;

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

import org.apache.archiva.redback.keys.KeyManager;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationManager;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.AuthorizationDataSource;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.authorization.AuthorizationResult;
import org.apache.archiva.redback.authorization.Authorizer;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * DefaultSecuritySystem:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 *
 */
@Service( "securitySystem" )
public class DefaultSecuritySystem
    implements SecuritySystem
{
    private Logger log = LoggerFactory.getLogger( DefaultSecuritySystem.class );

    @Inject
    private AuthenticationManager authnManager;

    @Inject
    @Named( value = "authorizer#rbac" )
    private Authorizer authorizer;

    @Inject
    @Named( value = "userManager#configurable" )
    private UserManager userManager;

    @Inject
    @Named( value = "keyManager#cached" )
    private KeyManager keyManager;

    @Inject
    private UserSecurityPolicy policy;

    // ----------------------------------------------------------------------------
    // Authentication: delegate to the authnManager
    // ----------------------------------------------------------------------------

    /**
     * delegate to the authentication system for boolean authentication checks,
     * if the result is authentic then pull the user object from the user
     * manager and add it to the session.  If the result is false return the result in
     * an authenticated session and a null user object.
     * <p/>
     * in the event of a successful authentication and a lack of corresponding user in the
     * usermanager return a null user as well
     * <p/>
     * //todo should this last case create a user in the usermanager?
     *
     * @param source
     * @return
     * @throws AuthenticationException
     * @throws UserNotFoundException
     * @throws MustChangePasswordException
     * @throws org.apache.archiva.redback.policy.AccountLockedException
     * @throws MustChangePasswordException
     */
    public SecuritySession authenticate( AuthenticationDataSource source )
        throws AuthenticationException, UserNotFoundException, AccountLockedException, MustChangePasswordException
    {
        // Perform Authentication.
        AuthenticationResult result = authnManager.authenticate( source );

        log.debug( "authnManager.authenticate() result: {}", result );

        // Process Results.
        if ( result.isAuthenticated() )
        {
            log.debug( "User '{}' authenticated.", result.getPrincipal());
            User user = userManager.findUser( result.getPrincipal() );
            if ( user != null )
            {
                log.debug( "User '{}' exists.", result.getPrincipal() );
                log.debug( "User: {}", user );
                return new DefaultSecuritySession( result, user );
            }
            else
            {
                log.debug( "User '{}' DOES NOT exist.", result.getPrincipal() );
                return new DefaultSecuritySession( result );
            }
        }
        else
        {
            log.debug( "User '{}' IS NOT authenticated.", result.getPrincipal() );
            return new DefaultSecuritySession( result );
        }
    }

    public boolean isAuthenticated( AuthenticationDataSource source )
        throws AuthenticationException, UserNotFoundException, AccountLockedException, MustChangePasswordException
    {
        return authenticate( source ).getAuthenticationResult().isAuthenticated();
    }

    public String getAuthenticatorId()
    {
        if ( authnManager == null )
        {
            return "<null>";
        }
        return authnManager.getId();
    }

    // ----------------------------------------------------------------------------
    // Authorization: delegate to the authorizer
    // ----------------------------------------------------------------------------

    public AuthorizationResult authorize( SecuritySession session, Object permission )
        throws AuthorizationException
    {
        return authorize( session, permission, null );
    }

    public AuthorizationResult authorize( SecuritySession session, Object permission, Object resource )
        throws AuthorizationException
    {
        AuthorizationDataSource source = null;

        if ( session != null )
        {
            User user = session.getUser();
            if ( user != null )
            {
                source = new AuthorizationDataSource( user.getUsername(), user, permission, resource );
            }
        }

        if ( source == null )
        {
            source = new AuthorizationDataSource( null, null, permission, resource );
        }

        return authorizer.isAuthorized( source );
    }

    public boolean isAuthorized( SecuritySession session, Object permission )
        throws AuthorizationException
    {
        return isAuthorized( session, permission, null );
    }

    public boolean isAuthorized( SecuritySession session, Object permission, Object resource )
        throws AuthorizationException
    {
        return authorize( session, permission, resource ).isAuthorized();
    }

    public String getAuthorizerId()
    {
        if ( authorizer == null )
        {
            return "<null>";
        }
        return authorizer.getId();
    }

    // ----------------------------------------------------------------------------
    // User Management: delegate to the user manager
    // ----------------------------------------------------------------------------

    public UserManager getUserManager()
    {
        return userManager;
    }

    public String getUserManagementId()
    {
        if ( userManager == null )
        {
            return "<null>";
        }
        return userManager.getId();
    }

    public KeyManager getKeyManager()
    {
        return keyManager;
    }

    public String getKeyManagementId()
    {
        if ( keyManager == null )
        {
            return "<null>";
        }
        return keyManager.getId();
    }

    public UserSecurityPolicy getPolicy()
    {
        return policy;
    }

    public String getPolicyId()
    {
        if ( policy == null )
        {
            return "<null>";
        }
        return policy.getId();
    }

    public AuthenticationManager getAuthenticationManager()
    {
        return authnManager;
    }

    public Authorizer getAuthorizer()
    {
        return authorizer;
    }

    public AuthenticationManager getAuthnManager()
    {
        return authnManager;
    }

    public void setAuthnManager( AuthenticationManager authnManager )
    {
        this.authnManager = authnManager;
    }

    public void setAuthorizer( Authorizer authorizer )
    {
        this.authorizer = authorizer;
    }

    public void setUserManager( UserManager userManager )
    {
        this.userManager = userManager;
    }

    public void setKeyManager( KeyManager keyManager )
    {
        this.keyManager = keyManager;
    }

    public void setPolicy( UserSecurityPolicy policy )
    {
        this.policy = policy;
    }
}
