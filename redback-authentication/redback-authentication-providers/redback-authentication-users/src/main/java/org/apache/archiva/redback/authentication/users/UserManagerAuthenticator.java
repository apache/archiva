package org.apache.archiva.redback.authentication.users;

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
import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.Authenticator;
import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.policy.PasswordEncoder;
import org.apache.archiva.redback.policy.PolicyViolationException;
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link Authenticator} implementation that uses a wrapped {@link UserManager} to authenticate.
 *
 * @author <a href='mailto:rahul.thakur.xdev@gmail.com'>Rahul Thakur</a>
 */
@Service("authenticator#user-manager")
public class UserManagerAuthenticator
    implements Authenticator
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named(value = "userManager#configurable")
    private UserManager userManager;

    @Inject
    protected UserSecurityPolicy securityPolicy;

    public String getId()
    {
        return "UserManagerAuthenticator";
    }

    /**
     * @throws org.apache.archiva.redback.policy.AccountLockedException
     *
     * @throws MustChangePasswordException
     * @throws MustChangePasswordException
     * @throws PolicyViolationException
     * @see org.apache.archiva.redback.authentication.Authenticator#authenticate(org.apache.archiva.redback.authentication.AuthenticationDataSource)
     */
    public AuthenticationResult authenticate( AuthenticationDataSource ds )
        throws AuthenticationException, AccountLockedException, MustChangePasswordException
    {
        boolean authenticationSuccess = false;
        String username = null;
        Exception resultException = null;
        PasswordBasedAuthenticationDataSource source = (PasswordBasedAuthenticationDataSource) ds;
        Map<String, String> authnResultExceptionsMap = new HashMap<String, String>();

        try
        {
            log.debug( "Authenticate: {}", source );
            User user = userManager.findUser( source.getPrincipal() );
            username = user.getUsername();

            if ( user.isLocked() )
            {
                throw new AccountLockedException( "Account " + source.getPrincipal() + " is locked.", user );
            }

            if ( user.isPasswordChangeRequired() && source.isEnforcePasswordChange() )
            {
                throw new MustChangePasswordException( "Password expired.", user );
            }

            PasswordEncoder encoder = securityPolicy.getPasswordEncoder();
            log.debug( "PasswordEncoder: {}", encoder.getClass().getName() );

            boolean isPasswordValid = encoder.isPasswordValid( user.getEncodedPassword(), source.getPassword() );
            if ( isPasswordValid )
            {
                log.debug( "User {} provided a valid password", source.getPrincipal() );

                try
                {
                    securityPolicy.extensionPasswordExpiration( user );
                }
                catch ( MustChangePasswordException e )
                {
                    user.setPasswordChangeRequired( true );
                    throw e;
                }

                authenticationSuccess = true;

                //REDBACK-151 do not make unnessesary updates to the user object
                if ( user.getCountFailedLoginAttempts() > 0 )
                {
                    user.setCountFailedLoginAttempts( 0 );
                    userManager.updateUser( user );
                }

                return new AuthenticationResult( true, source.getPrincipal(), null );
            }
            else
            {
                log.warn( "Password is Invalid for user {}.", source.getPrincipal() );
                authnResultExceptionsMap.put( AuthenticationConstants.AUTHN_NO_SUCH_USER,
                                              "Password is Invalid for user " + source.getPrincipal() + "." );

                try
                {
                    securityPolicy.extensionExcessiveLoginAttempts( user );
                }
                finally
                {
                    userManager.updateUser( user );
                }

                return new AuthenticationResult( false, source.getPrincipal(), null, authnResultExceptionsMap );
            }
        }
        catch ( UserNotFoundException e )
        {
            log.warn( "Login for user {} failed. user not found.", source.getPrincipal() );
            resultException = e;
            authnResultExceptionsMap.put( AuthenticationConstants.AUTHN_NO_SUCH_USER,
                                          "Login for user " + source.getPrincipal() + " failed. user not found." );
        }
        catch ( UserManagerException e )
        {
            log.warn( "Login for user {} failed, message: {}", source.getPrincipal(), e.getMessage() );
            resultException = e;
            authnResultExceptionsMap.put( AuthenticationConstants.AUTHN_RUNTIME_EXCEPTION,
                                          "Login for user " + source.getPrincipal() + " failed, message: "
                                              + e.getMessage() );
        }

        return new AuthenticationResult( authenticationSuccess, username, resultException, authnResultExceptionsMap );
    }

    /**
     * Returns the wrapped {@link UserManager} used by this {@link org.apache.archiva.redback.authentication.Authenticator}
     * implementation for authentication.
     *
     * @return the userManager
     */
    public UserManager getUserManager()
    {
        return userManager;
    }

    /**
     * Sets a {@link UserManager} to be used by this {@link Authenticator}
     * implementation for authentication.
     *
     * @param userManager the userManager to set
     */
    public void setUserManager( UserManager userManager )
    {
        this.userManager = userManager;
    }

    public boolean supportsDataSource( AuthenticationDataSource source )
    {
        return ( source instanceof PasswordBasedAuthenticationDataSource );
    }

    public UserSecurityPolicy getSecurityPolicy()
    {
        return securityPolicy;
    }

    public void setSecurityPolicy( UserSecurityPolicy securityPolicy )
    {
        this.securityPolicy = securityPolicy;
    }
}
