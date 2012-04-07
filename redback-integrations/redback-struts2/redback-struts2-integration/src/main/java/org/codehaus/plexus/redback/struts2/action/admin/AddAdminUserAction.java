package org.codehaus.plexus.redback.struts2.action.admin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.users.UserManager;
import org.apache.struts2.ServletActionContext;
import org.apache.archiva.redback.authentication.AuthenticationConstants;
import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.struts2.action.AuditEvent;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.model.EditUserCredentials;
import org.codehaus.redback.integration.util.AutoLoginCookies;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Date;

/**
 * AddAdminUserAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "redback-admin-account" )
@Scope( "prototype" )
public class AddAdminUserAction
    extends AbstractAdminUserCredentialsAction
{
    private static final String LOGIN_ERROR = "login-error";

    private static final String LOGIN_SUCCESS = "security-login-success";

    private static final String PASSWORD_CHANGE = "security-must-change-password";

    private static final String ACCOUNT_LOCKED = "security-login-locked";

    @Inject
    private RoleManager roleManager;


    @Inject
    private UserConfiguration config;

    private EditUserCredentials user;

    @Inject
    private AutoLoginCookies autologinCookies;

    public String show()
    {
        if ( user == null )
        {
            user = new EditUserCredentials( config.getString( "redback.default.admin" ) );
        }

        return INPUT;
    }

    /**
     * TODO this must done in a service !!
     * @return
     */
    public String submit()
    {
        if ( user == null )
        {
            user = new EditUserCredentials( config.getString( "redback.default.admin" ) );
            addActionError( getText( "invalid.admin.credentials" ) );
            return ERROR;
        }

        log.info( "user = {}", user );

        internalUser = user;

        validateCredentialsStrict();

        UserManager userManager = super.securitySystem.getUserManager();

        if ( userManager.userExists( config.getString( "redback.default.admin" ) ) )
        {
            // Means that the role name exist already.
            // We need to fail fast and return to the previous page.
            addActionError( getText( "admin.user.already.exists" ) );
            return ERROR;
        }

        if ( hasActionErrors() || hasFieldErrors() )
        {
            return ERROR;
        }

        User u =
            userManager.createUser( config.getString( "redback.default.admin" ), user.getFullName(), user.getEmail() );
        if ( u == null )
        {
            addActionError( getText( "cannot.operate.on.null.user" ) );
            return ERROR;
        }

        u.setPassword( user.getPassword() );
        u.setLocked( false );
        u.setPasswordChangeRequired( false );
        u.setPermanent( true );

        userManager.addUser( u );

        AuditEvent event = new AuditEvent( getText( "log.account.create" ) );
        event.setAffectedUser( u.getUsername() );
        event.log();

        try
        {
            roleManager.assignRole( "system-administrator", u.getPrincipal().toString() );
            event = new AuditEvent( getText( "log.assign.role" ) );
            event.setAffectedUser( u.getUsername() );
            event.setRole( "system-administrator" );
            event.log();
        }
        catch ( RoleManagerException rpe )
        {
            addActionError( getText( "cannot.assign.admin.role" ) );
            return ERROR;
        }

        PasswordBasedAuthenticationDataSource authdatasource = new PasswordBasedAuthenticationDataSource();
        authdatasource.setPrincipal( user.getUsername() );
        authdatasource.setPassword( user.getPassword() );

        return webLogin( authdatasource );
    }

    public EditUserCredentials getUser()
    {
        return user;
    }

    public void setUser( EditUserCredentials user )
    {
        this.user = user;
    }

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        return SecureActionBundle.OPEN;
    }

    /**
     * 1) attempts to authentication based on the passed in data source
     * 2) if successful sets cookies and returns LOGIN_SUCCESS
     * 3) if failure then check what kinda failure and return error
     *
     * @param authdatasource
     * @return
     */
    private String webLogin( AuthenticationDataSource authdatasource )
    {
        // An attempt should log out your authentication tokens first!
        setAuthTokens( null );

        clearErrorsAndMessages();

        String principal = authdatasource.getPrincipal();

        try
        {
            SecuritySession securitySession = securitySystem.authenticate( authdatasource );

            if ( securitySession.getAuthenticationResult().isAuthenticated() )
            {
                // Success!  Create tokens.
                setAuthTokens( securitySession );

                setCookies( authdatasource );

                AuditEvent event = new AuditEvent( getText( "log.login.success" ) );
                event.setAffectedUser( principal );
                event.log();

                User u = securitySession.getUser();
                u.setLastLoginDate( new Date() );
                securitySystem.getUserManager().updateUser( u );

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
                event.setAffectedUser( principal );
                event.log();

                return LOGIN_ERROR;
            }
        }
        catch ( AuthenticationException ae )
        {
            addActionError( getText( "authentication.exception", Arrays.asList( (Object) ae.getMessage() ) ) );
            return LOGIN_ERROR;
        }
        catch ( UserNotFoundException ue )
        {
            addActionError(
                getText( "user.not.found.exception", Arrays.asList( (Object) principal, ue.getMessage() ) ) );

            AuditEvent event = new AuditEvent( getText( "log.login.fail" ) );
            event.setAffectedUser( principal );
            event.log();
            return LOGIN_ERROR;
        }
        catch ( AccountLockedException e )
        {
            addActionError( getText( "account.locked" ) );

            AuditEvent event = new AuditEvent( getText( "log.login.fail.locked" ) );
            event.setAffectedUser( principal );
            event.log();
            return ACCOUNT_LOCKED;
        }
        catch ( MustChangePasswordException e )
        {
            // TODO: preferably we would not set the cookies for this "partial" login state
            setCookies( authdatasource );

            AuditEvent event = new AuditEvent( getText( "log.login.fail.locked" ) );
            event.setAffectedUser( principal );
            event.log();
            return PASSWORD_CHANGE;
        }
    }

    private void setCookies( AuthenticationDataSource authdatasource )
    {
        autologinCookies.setSignonCookie( authdatasource.getPrincipal(), ServletActionContext.getResponse(),
                                          ServletActionContext.getRequest() );
    }
}
