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

import org.codehaus.plexus.redback.keys.AuthenticationKey;
import org.codehaus.plexus.redback.keys.KeyManagerException;
import org.codehaus.plexus.redback.policy.UserSecurityPolicy;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.mail.Mailer;
import org.codehaus.redback.integration.model.CreateUserCredentials;
import org.codehaus.redback.integration.security.role.RedbackRoleConstants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import java.util.Arrays;

/**
 * RegisterAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "redback-register" )
@Scope( "prototype" )
public class RegisterAction
    extends AbstractUserCredentialsAction
    implements CancellableAction
{
    protected static final String REGISTER_SUCCESS = "security-register-success";

    private static final String VALIDATION_NOTE = "validation-note";

    private static final String RESEND_VALIDATION_EMAIL = "security-resend-validation-email";

    // ------------------------------------------------------------------
    //  Component Requirements
    // ------------------------------------------------------------------

    /**
     *
     */
    @Inject
    private Mailer mailer;

    /**
     *
     */
    @Inject
    private RoleManager roleManager;

    private CreateUserCredentials user;

    private boolean emailValidationRequired;

    private String username;

    // ------------------------------------------------------------------
    // Action Entry Points - (aka Names)
    // ------------------------------------------------------------------

    public String show()
    {
        if ( user == null )
        {
            user = new CreateUserCredentials();
        }

        emailValidationRequired = securitySystem.getPolicy().getUserValidationSettings().isEmailValidationRequired();

        return INPUT;
    }

    public String register()
    {
        if ( user == null )
        {
            user = new CreateUserCredentials();
            addActionError( getText( "invalid.user.credentials" ) );
            return ERROR;
        }

        UserSecurityPolicy securityPolicy = securitySystem.getPolicy();

        emailValidationRequired = securityPolicy.getUserValidationSettings().isEmailValidationRequired();

        internalUser = user;

        if ( securityPolicy.getUserValidationSettings().isEmailValidationRequired() )
        {
            validateCredentialsLoose();
        }
        else
        {
            validateCredentialsStrict();
        }

        // NOTE: Do not perform Password Rules Validation Here.
        UserManager manager = super.securitySystem.getUserManager();

        if ( manager.userExists( user.getUsername() ) )
        {
            // Means that the role name doesn't exist.
            // We need to fail fast and return to the previous page.
            addActionError( getText( "user.already.exists", Arrays.asList( (Object) user.getUsername() ) ) );
        }

        if ( hasActionErrors() || hasFieldErrors() )
        {
            return ERROR;
        }

        User u = manager.createUser( user.getUsername(), user.getFullName(), user.getEmail() );
        u.setPassword( user.getPassword() );
        u.setValidated( false );
        u.setLocked( false );

        try
        {
            roleManager.assignRole( RedbackRoleConstants.REGISTERED_USER_ROLE_ID, u.getPrincipal().toString() );
        }
        catch ( RoleManagerException rpe )
        {
            addActionError( getText( "assign.role.failure" ) );
            log.error( "RoleProfile Error: " + rpe.getMessage(), rpe );
            return ERROR;
        }

        if ( securityPolicy.getUserValidationSettings().isEmailValidationRequired() )
        {
            u.setLocked( true );

            try
            {
                AuthenticationKey authkey =
                    securitySystem.getKeyManager().createKey( u.getPrincipal().toString(), "New User Email Validation",
                                                              securityPolicy.getUserValidationSettings().getEmailValidationTimeout() );

                mailer.sendAccountValidationEmail( Arrays.asList( u.getEmail() ), authkey, getBaseUrl() );

                securityPolicy.setEnabled( false );
                manager.addUser( u );

                return VALIDATION_NOTE;
            }
            catch ( KeyManagerException e )
            {
                addActionError( getText( "cannot.register.user" ) );
                log.error( "Unable to register a new user.", e );
                return ERROR;
            }
            finally
            {
                securityPolicy.setEnabled( true );
            }
        }
        else
        {
            manager.addUser( u );
        }

        AuditEvent event = new AuditEvent( getText( "log.account.create" ) );
        event.setAffectedUser( username );
        event.log();

        return REGISTER_SUCCESS;
    }

    public String resendRegistrationEmail()
    {
        UserSecurityPolicy securityPolicy = securitySystem.getPolicy();

        try
        {
            User user = super.securitySystem.getUserManager().findUser( username );

            AuthenticationKey authkey =
                securitySystem.getKeyManager().createKey( user.getPrincipal().toString(), "New User Email Validation",
                                                          securityPolicy.getUserValidationSettings().getEmailValidationTimeout() );

            mailer.sendAccountValidationEmail( Arrays.asList( user.getEmail() ), authkey, getBaseUrl() );

            return RESEND_VALIDATION_EMAIL;
        }
        catch ( KeyManagerException e )
        {
            addActionError( getText( "cannot.register.user" ) );
            log.error( "Unable to register a new user.", e );
            return ERROR;
        }
        catch ( UserNotFoundException e )
        {
            addActionError( getText( "cannot.find.user" ) );
            log.error( "Unable to find user.", e );
            return ERROR;
        }
    }

    public String cancel()
    {
        return CANCEL;
    }

    // ------------------------------------------------------------------
    // Parameter Accessor Methods
    // ------------------------------------------------------------------

    public CreateUserCredentials getUser()
    {
        return user;
    }

    public void setUser( CreateUserCredentials user )
    {
        this.user = user;
    }

    public boolean isEmailValidationRequired()
    {
        return emailValidationRequired;
    }

    public void setEmailValidationRequired( boolean emailValidationRequired )
    {
        this.emailValidationRequired = emailValidationRequired;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        return SecureActionBundle.OPEN;
    }
}
