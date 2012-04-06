package org.codehaus.plexus.redback.struts2.action;

/*
 * Copyright 2005-2006 The Codehaus.
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

import org.apache.struts2.ServletActionContext;
import org.codehaus.plexus.redback.authentication.AuthenticationConstants;
import org.codehaus.plexus.redback.authentication.AuthenticationDataSource;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.codehaus.plexus.redback.authentication.TokenBasedAuthenticationDataSource;
import org.codehaus.plexus.redback.configuration.UserConfiguration;
import org.codehaus.plexus.redback.keys.AuthenticationKey;
import org.codehaus.plexus.redback.keys.KeyManagerException;
import org.codehaus.plexus.redback.keys.KeyNotFoundException;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.util.AutoLoginCookies;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Date;

/**
 * LoginAction
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "redback-login" )
@Scope( "prototype" )
public class LoginAction
    extends AbstractSecurityAction
    implements CancellableAction
{
    private static final String LOGIN_SUCCESS = "security-login-success";

    private static final String PASSWORD_CHANGE = "security-must-change-password";

    private static final String ACCOUNT_LOCKED = "security-login-locked";

    // ------------------------------------------------------------------
    //  Component Requirements
    // ------------------------------------------------------------------

    /**
     *
     */
    @Inject
    protected SecuritySystem securitySystem;

    private String username;

    private String password;

    private String validateMe;

    private String resetPassword;

    private boolean rememberMe;

    /**
     *
     */
    @Inject
    private AutoLoginCookies autologinCookies;

    /**
     *
     */
    @Inject
    private UserConfiguration config;

    // ------------------------------------------------------------------
    // Action Entry Points - (aka Names)
    // ------------------------------------------------------------------

    public String show()
    {
        return INPUT;
    }

    /**
     * 1) check if this is a validation authentication action
     * 2) check if this is a reset password authentication action
     * 3) sets up a password based authentication and passes on to webLogin()
     *
     * @return
     */
    public String login()
    {
        if ( StringUtils.isNotEmpty( validateMe ) )
        {
            // Process a login / validate request.
            return validated();
        }

        if ( StringUtils.isNotEmpty( resetPassword ) )
        {
            // Process a login / reset password request.
            return resetPassword();
        }

        if ( StringUtils.isEmpty( username ) )
        {
            addFieldError( "username", getText( "username.required" ) );
            return ERROR;
        }

        PasswordBasedAuthenticationDataSource authdatasource = new PasswordBasedAuthenticationDataSource();
        authdatasource.setPrincipal( username );
        authdatasource.setPassword( password );

        return webLogin( authdatasource, rememberMe );
    }

    /**
     * 1) sets up a token based authentication
     * 2) forces a password change requirement to the user
     * 3) passes on to webLogin()
     *
     * @return
     */
    public String resetPassword()
    {
        if ( StringUtils.isEmpty( resetPassword ) )
        {
            addActionError( getText( "reset.password.missing" ) );
            return ERROR;
        }

        try
        {
            AuthenticationKey authkey = securitySystem.getKeyManager().findKey( resetPassword );

            User user = securitySystem.getUserManager().findUser( authkey.getForPrincipal() );

            user.setPasswordChangeRequired( true );
            user.setEncodedPassword( "" );

            TokenBasedAuthenticationDataSource authsource = new TokenBasedAuthenticationDataSource();
            authsource.setPrincipal( user.getPrincipal().toString() );
            authsource.setToken( authkey.getKey() );
            authsource.setEnforcePasswordChange( false );

            securitySystem.getUserManager().updateUser( user );

            AuditEvent event = new AuditEvent( getText( "log.password.change" ) );
            event.setAffectedUser( username );
            event.log();

            return webLogin( authsource, false );
        }
        catch ( KeyNotFoundException e )
        {
            log.info( "Invalid key requested: {}", resetPassword );
            addActionError( getText( "cannot.find.key" ) );
            return ERROR;
        }
        catch ( KeyManagerException e )
        {
            addActionError( getText( "cannot.find.key.at.the.moment" ) );
            log.warn( "Key Manager error: ", e );
            return ERROR;
        }
        catch ( UserNotFoundException e )
        {
            addActionError( getText( "cannot.find.user" ) );
            return ERROR;
        }
    }

    /**
     * 1) sets up a token based authentication
     * 2) forces a password change requirement to the user
     * 3) passes on to webLogin()
     *
     * @return
     */
    public String validated()
    {
        if ( StringUtils.isEmpty( validateMe ) )
        {
            addActionError( getText( "validation.failure.key.missing" ) );
            return ERROR;
        }

        try
        {
            AuthenticationKey authkey = securitySystem.getKeyManager().findKey( validateMe );

            User user = securitySystem.getUserManager().findUser( authkey.getForPrincipal() );

            user.setValidated( true );
            user.setLocked( false );
            user.setPasswordChangeRequired( true );
            user.setEncodedPassword( "" );

            TokenBasedAuthenticationDataSource authsource = new TokenBasedAuthenticationDataSource();
            authsource.setPrincipal( user.getPrincipal().toString() );
            authsource.setToken( authkey.getKey() );
            authsource.setEnforcePasswordChange( false );

            securitySystem.getUserManager().updateUser( user );
            String currentUser = getCurrentUser();

            AuditEvent event = new AuditEvent( getText( "log.account.validation" ) );
            event.setAffectedUser( username );
            event.setCurrentUser( currentUser );
            event.log();

            return webLogin( authsource, false );
        }
        catch ( KeyNotFoundException e )
        {
            log.info( "Invalid key requested: {}", validateMe );
            addActionError( getText( "cannot.find.key" ) );
            return ERROR;
        }
        catch ( KeyManagerException e )
        {
            addActionError( getText( "cannot.find.key.at.the.momment" ) );
            return ERROR;
        }
        catch ( UserNotFoundException e )
        {
            addActionError( getText( "cannot.find.user" ) );
            return ERROR;
        }
    }

    public String cancel()
    {
        return CANCEL;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public String getValidateMe()
    {
        return validateMe;
    }

    public void setValidateMe( String validateMe )
    {
        this.validateMe = validateMe;
    }

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        return SecureActionBundle.OPEN;
    }

    public String getResetPassword()
    {
        return resetPassword;
    }

    public void setResetPassword( String resetPassword )
    {
        this.resetPassword = resetPassword;
    }

    public boolean isRememberMe()
    {
        return rememberMe;
    }

    public void setRememberMe( boolean rememberMe )
    {
        this.rememberMe = rememberMe;
    }


    /**
     * 1) attempts to authentication based on the passed in data source
     * 2) if successful sets cookies and returns LOGIN_SUCCESS
     * 3) if failure then check what kinda failure and return error
     *
     * @param authdatasource
     * @param rememberMe
     * @return
     */
    private String webLogin( AuthenticationDataSource authdatasource, boolean rememberMe )
    {
        // An attempt should log out your authentication tokens first!
        setAuthTokens( null );

        clearErrorsAndMessages();

        // TODO: share this section with AutoLoginInterceptor
        try
        {
            SecuritySession securitySession = securitySystem.authenticate( authdatasource );

            if ( securitySession.isAuthenticated() )
            {
                // Success!  Create tokens.
                setAuthTokens( securitySession );

                if ( securitySystem.getPolicy().getUserValidationSettings().isEmailValidationRequired() )
                {
                    if ( !securitySession.getUser().getUsername().equals(
                        config.getString( "redback.default.admin" ) ) )
                    {
                        if ( !securitySession.getUser().isValidated() )
                        {
                            setAuthTokens( null );
                            // NOTE: this text is the same as incorrect.username.password to avoid exposing actual account existence
                            addActionError( getText( "account.validation.required" ) );
                            return ERROR;
                        }
                    }
                }

                setCookies( authdatasource, rememberMe );

                AuditEvent event = new AuditEvent( getText( "log.login.success" ) );
                event.setAffectedUser( username );
                event.log();

                User user = securitySession.getUser();
                user.setLastLoginDate( new Date() );
                securitySystem.getUserManager().updateUser( user );

                if ( StringUtils.isNotEmpty( validateMe ) )
                {
                    try
                    {
                        //REDBACK-146: delete key after validating so user won't be able to use it the second time around
                        securitySystem.getKeyManager().deleteKey( validateMe );
                    }
                    catch ( KeyManagerException e )
                    {
                        addActionError( getText( "cannot.find.key.at.the.momment" ) );
                        return ERROR;
                    }
                }

                return LOGIN_SUCCESS;
            }
            else
            {
                log.debug( "Login Action failed against principal : {}",
                           securitySession.getAuthenticationResult().getPrincipal(),
                           securitySession.getAuthenticationResult().getException() );

                AuthenticationResult result = securitySession.getAuthenticationResult();
                if ( result.getExceptionsMap() != null && !result.getExceptionsMap().isEmpty() )
                {
                    if ( result.getExceptionsMap().get( AuthenticationConstants.AUTHN_NO_SUCH_USER ) != null )
                    {
                        addActionError( getText( "incorrect.username.password" ) );
                    }
                    else
                    {
                        addActionError( getText( "authentication.failed" ) );
                    }
                }
                else
                {
                    addActionError( getText( "authentication.failed" ) );
                }

                AuditEvent event = new AuditEvent( getText( "log.login.fail" ) );
                event.setAffectedUser( username );
                event.log();

                return ERROR;
            }
        }
        catch ( AuthenticationException ae )
        {
            addActionError( getText( "authentication.exception", Arrays.asList( (Object) ae.getMessage() ) ) );
            return ERROR;
        }
        catch ( UserNotFoundException ue )
        {
            addActionError(
                getText( "user.not.found.exception", Arrays.asList( (Object) username, ue.getMessage() ) ) );

            AuditEvent event = new AuditEvent( getText( "log.login.fail" ) );
            event.setAffectedUser( username );
            event.log();
            return ERROR;
        }
        catch ( AccountLockedException e )
        {
            addActionError( getText( "account.locked" ) );

            AuditEvent event = new AuditEvent( getText( "log.login.fail.locked" ) );
            event.setAffectedUser( username );
            event.log();
            return ACCOUNT_LOCKED;
        }
        catch ( MustChangePasswordException e )
        {
            // TODO: preferably we would not set the cookies for this "partial" login state
            setCookies( authdatasource, rememberMe );

            AuditEvent event = new AuditEvent( getText( "log.login.fail.locked" ) );
            event.setAffectedUser( username );
            event.log();
            return PASSWORD_CHANGE;
        }
    }

    private void setCookies( AuthenticationDataSource authdatasource, boolean rememberMe )
    {
        if ( rememberMe )
        {
            autologinCookies.setRememberMeCookie( authdatasource.getPrincipal(), ServletActionContext.getResponse(),
                                                  ServletActionContext.getRequest() );
        }
        autologinCookies.setSignonCookie( authdatasource.getPrincipal(), ServletActionContext.getResponse(),
                                          ServletActionContext.getRequest() );
    }
}
