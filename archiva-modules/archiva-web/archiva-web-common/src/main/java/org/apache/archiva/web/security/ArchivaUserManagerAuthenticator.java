package org.apache.archiva.web.security;
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.runtime.RedbackRuntimeConfigurationAdmin;
import org.apache.archiva.redback.authentication.AbstractAuthenticator;
import org.apache.archiva.redback.authentication.AuthenticationConstants;
import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationFailureCause;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.Authenticator;
import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.policy.PasswordEncoder;
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service("authenticator#archiva")
public class ArchivaUserManagerAuthenticator
    extends AbstractAuthenticator
    implements Authenticator
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private UserSecurityPolicy securityPolicy;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private RedbackRuntimeConfigurationAdmin redbackRuntimeConfigurationAdmin;

    private List<UserManager> userManagers;

    private boolean valid = false;

    @PostConstruct
    @Override
    public void initialize()
        throws AuthenticationException
    {
        try
        {
            List<String> userManagerImpls =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration().getUserManagerImpls();

            userManagers = new ArrayList<>( userManagerImpls.size() );

            for ( String beanId : userManagerImpls )
            {
                userManagers.add( applicationContext.getBean( "userManager#" + beanId, UserManager.class ) );
            }
            valid=true;
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "Error during repository initialization {}", e.getMessage(), e );
            // throw new AuthenticationException( e.getMessage(), e );
        }
    }


    @Override
    public AuthenticationResult authenticate( AuthenticationDataSource ds )
        throws AuthenticationException, AccountLockedException, MustChangePasswordException
    {
        boolean authenticationSuccess = false;
        String username = null;
        Exception resultException = null;
        PasswordBasedAuthenticationDataSource source = (PasswordBasedAuthenticationDataSource) ds;
        List<AuthenticationFailureCause> authnResultErrors = new ArrayList<>();

        for ( UserManager userManager : userManagers )
        {
            try
            {
                log.debug( "Authenticate: {} with userManager: {}", source, userManager.getId() );
                User user = userManager.findUser( source.getUsername() );
                username = user.getUsername();

                if ( user.isLocked() )
                {
                    //throw new AccountLockedException( "Account " + source.getUsername() + " is locked.", user );
                    AccountLockedException e =
                        new AccountLockedException( "Account " + source.getUsername() + " is locked.", user );
                    log.warn( "{}", e.getMessage() );
                    resultException = e;
                    authnResultErrors.add(
                        new AuthenticationFailureCause( AuthenticationConstants.AUTHN_LOCKED_USER_EXCEPTION,
                                                        e.getMessage() ) );
                }

                if ( user.isPasswordChangeRequired() && source.isEnforcePasswordChange() )
                {
                    //throw new MustChangePasswordException( "Password expired.", user );
                    MustChangePasswordException e = new MustChangePasswordException( "Password expired.", user );
                    log.warn( "{}", e.getMessage() );
                    resultException = e;
                    authnResultErrors.add(
                        new AuthenticationFailureCause( AuthenticationConstants.AUTHN_MUST_CHANGE_PASSWORD_EXCEPTION,
                                                        e.getMessage() ) );
                }

                PasswordEncoder encoder = securityPolicy.getPasswordEncoder();
                log.debug( "PasswordEncoder: {}", encoder.getClass().getName() );

                boolean isPasswordValid = encoder.isPasswordValid( user.getEncodedPassword(), source.getPassword() );
                if ( isPasswordValid )
                {
                    log.debug( "User {} provided a valid password", source.getUsername() );

                    try
                    {
                        securityPolicy.extensionPasswordExpiration( user );

                        authenticationSuccess = true;

                        //REDBACK-151 do not make unnessesary updates to the user object
                        if ( user.getCountFailedLoginAttempts() > 0 )
                        {
                            user.setCountFailedLoginAttempts( 0 );
                            if ( !userManager.isReadOnly() )
                            {
                                userManager.updateUser( user );
                            }
                        }

                        return new AuthenticationResult( true, source.getUsername(), null );
                    }
                    catch ( MustChangePasswordException e )
                    {
                        user.setPasswordChangeRequired( true );
                        //throw e;
                        resultException = e;
                        authnResultErrors.add( new AuthenticationFailureCause(
                            AuthenticationConstants.AUTHN_MUST_CHANGE_PASSWORD_EXCEPTION, e.getMessage() ).user( user ) );
                    }
                }
                else
                {
                    log.warn( "Password is Invalid for user {} and userManager '{}'.", source.getUsername(),
                              userManager.getId() );
                    authnResultErrors.add( new AuthenticationFailureCause( AuthenticationConstants.AUTHN_NO_SUCH_USER,
                                                                           "Password is Invalid for user "
                                                                               + source.getUsername() + "." ).user( user ) );

                    try
                    {

                        securityPolicy.extensionExcessiveLoginAttempts( user );

                    }
                    finally
                    {
                        if ( !userManager.isReadOnly() )
                        {
                            userManager.updateUser( user );
                        }
                    }

                    //return new AuthenticationResult( false, source.getUsername(), null, authnResultExceptionsMap );
                }
            }
            catch ( UserNotFoundException e )
            {
                log.warn( "Login for user {} and userManager {} failed. user not found.", source.getUsername(),
                          userManager.getId() );
                resultException = e;
                authnResultErrors.add( new AuthenticationFailureCause( AuthenticationConstants.AUTHN_NO_SUCH_USER,
                                                                       "Login for user " + source.getUsername()
                                                                           + " failed. user not found." ) );
            }
            catch ( Exception e )
            {
                log.warn( "Login for user {} and userManager {} failed, message: {}", source.getUsername(),
                          userManager.getId(), e.getMessage() );
                e.printStackTrace();
                resultException = e;
                authnResultErrors.add( new AuthenticationFailureCause( AuthenticationConstants.AUTHN_RUNTIME_EXCEPTION,
                                                                       "Login for user " + source.getUsername()
                                                                           + " failed, message: " + e.getMessage() ) );
            }
        }
        return new AuthenticationResult( authenticationSuccess, username, resultException, authnResultErrors );
    }

    @Override
    public boolean supportsDataSource( AuthenticationDataSource source )
    {
        return ( source instanceof PasswordBasedAuthenticationDataSource );
    }

    @Override
    public String getId()
    {
        return "ArchivaUserManagerAuthenticator";
    }

    public boolean isValid() {
        return valid;
    }
}
