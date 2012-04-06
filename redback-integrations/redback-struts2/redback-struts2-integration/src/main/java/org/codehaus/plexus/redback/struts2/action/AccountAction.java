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

import org.codehaus.plexus.redback.policy.PasswordEncoder;
import org.codehaus.plexus.redback.policy.PasswordRuleViolationException;
import org.codehaus.plexus.redback.system.DefaultSecuritySession;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystemConstants;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.model.EditUserCredentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.Arrays;

/**
 * AccountAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "redback-account" )
@Scope( "prototype" )
public class AccountAction
    extends AbstractUserCredentialsAction
    implements CancellableAction
{
    private static final String ACCOUNT_SUCCESS = "security-account-success";

    // ------------------------------------------------------------------
    // Action Parameters
    // ------------------------------------------------------------------

    private EditUserCredentials user;

    private String oldPassword;

    // ------------------------------------------------------------------
    // Action Entry Points - (aka Names)
    // ------------------------------------------------------------------

    public String show()
    {
        SecuritySession session = getSecuritySession();

        if ( !session.isAuthenticated() )
        {
            addActionError( getText( "cannot.show.account.login.required" ) );
            return REQUIRES_AUTHENTICATION;
        }

        String username = session.getUser().getUsername();

        if ( username == null )
        {
            addActionError( getText( "cannot.edit.user.null.username" ) );
            return ERROR;
        }

        if ( StringUtils.isEmpty( username ) )
        {
            addActionError( getText( "cannot.edit.user.empty.username" ) );
            return ERROR;
        }

        UserManager manager = super.securitySystem.getUserManager();

        if ( !manager.userExists( username ) )
        {
            // Means that the role name doesn't exist.
            // We need to fail fast and return to the previous page.
            addActionError( getText( "user.does.not.exist", Arrays.asList( (Object) username ) ) );
            return ERROR;
        }

        internalUser = user;

        try
        {
            User u = manager.findUser( username );
            if ( u == null )
            {
                addActionError( getText( "cannot.operate.on.null.user" ) );
                return ERROR;
            }

            user = new EditUserCredentials( u );
        }
        catch ( UserNotFoundException e )
        {
            addActionError( getText( "cannot.get.user", Arrays.asList( (Object) username, e.getMessage() ) ) );
            return ERROR;
        }

        return INPUT;
    }

    public String submit()
    {
        SecuritySession session = getSecuritySession();

        if ( !session.isAuthenticated() )
        {
            addActionError( getText( "cannot.show.account.login.required" ) );
            return REQUIRES_AUTHENTICATION;
        }

        String username = session.getUser().getUsername();

        if ( username == null )
        {
            addActionError( getText( "cannot.edit.user.null.username" ) );
            return ERROR;
        }

        if ( StringUtils.isEmpty( username ) )
        {
            addActionError( getText( "cannot.edit.user.empty.username" ) );
            return ERROR;
        }

        if ( user == null )
        {
            addActionError( getText( "cannot.edit.user.null.credentials" ) );
            return ERROR;
        }

        if ( !user.getPassword().equals( user.getConfirmPassword() ) )
        {
            addFieldError( "user.confirmPassword", getText( "password.confimation.failed" ) );
            return ERROR;
        }

        UserManager manager = super.securitySystem.getUserManager();

        if ( !manager.userExists( username ) )
        {
            // Means that the role name doesn't exist.
            // We need to fail fast and return to the previous page.
            addActionError( getText( "user.does.not.exist", Arrays.asList( (Object) username ) ) );
            return ERROR;
        }

        internalUser = user;

        try
        {
            User u = manager.findUser( username );
            if ( u == null )
            {
                addActionError( getText( "cannot.operate.on.null.user" ) );
                return ERROR;
            }

            if ( StringUtils.isNotEmpty( user.getPassword() ) )
            {
                PasswordEncoder encoder = securitySystem.getPolicy().getPasswordEncoder();

                if ( !encoder.isPasswordValid( u.getEncodedPassword(), oldPassword ) )
                {
                    addFieldError( "oldPassword", getText( "password.provided.does.not.match.existing" ) );
                    return ERROR;
                }

                u.setPassword( user.getPassword() );
            }

            u.setFullName( user.getFullName() );
            u.setEmail( user.getEmail() );
            u.setPassword( user.getPassword() );

            manager.updateUser( u );

            //check if current user then update the session
            if ( getSecuritySession().getUser().getUsername().equals( u.getUsername() ) )
            {
                SecuritySession securitySession =
                    new DefaultSecuritySession( getSecuritySession().getAuthenticationResult(), u );

                this.session.put( SecuritySystemConstants.SECURITY_SESSION_KEY, securitySession );

                setSession( this.session );
            }
        }
        catch ( UserNotFoundException e )
        {
            addActionError( getText( "cannot.get.user", Arrays.asList( (Object) username, e.getMessage() ) ) );
            return ERROR;
        }
        catch ( PasswordRuleViolationException e )
        {
            processPasswordRuleViolations( e );
            return ERROR;
        }

        return ACCOUNT_SUCCESS;
    }

    public String cancel()
    {
        return CANCEL;
    }

    // ------------------------------------------------------------------
    // Parameter Accessor Methods
    // ------------------------------------------------------------------

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
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        return bundle;
    }

    public void setOldPassword( String oldPassword )
    {
        this.oldPassword = oldPassword;
    }

    public boolean isSelf()
    {
        return true;
    }
}
