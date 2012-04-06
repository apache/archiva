package org.codehaus.plexus.redback.struts2.action.admin;

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

import java.util.Arrays;

import org.codehaus.plexus.redback.policy.UserSecurityPolicy;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.struts2.action.AbstractUserCredentialsAction;
import org.codehaus.plexus.redback.struts2.action.AuditEvent;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.model.CreateUserCredentials;
import org.codehaus.redback.integration.role.RoleConstants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * UserCreateAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller("redback-admin-user-create")
@Scope("prototype")
public class UserCreateAction
    extends AbstractUserCredentialsAction
{
    // ------------------------------------------------------------------
    // Action Parameters
    // ------------------------------------------------------------------

    private CreateUserCredentials user;

    // ------------------------------------------------------------------
    // Action Entry Points - (aka Names)
    // ------------------------------------------------------------------

    public String show()
    {
        if ( user == null )
        {
            user = new CreateUserCredentials();
        }

        return INPUT;
    }

    public String submit()
    {
        if ( user == null )
        {
            user = new CreateUserCredentials();
            addActionError( getText( "invalid.user.credentials" ) );
            return ERROR;
        }

        internalUser = user;

        validateCredentialsLoose();

        // NOTE: Do not perform Password Rules Validation Here.

        UserManager manager = super.securitySystem.getUserManager();

        if ( manager.userExists( user.getUsername() ) )
        {
            // Means that the role name doesn't exist.
            // We need to fail fast and return to the previous page.
            addActionError( getText( "user.already.exists", Arrays.asList( ( Object ) user.getUsername() ) ) );
        }

        if ( hasActionErrors() || hasFieldErrors() )
        {
            return ERROR;
        }

        User u = manager.createUser( user.getUsername(), user.getFullName(), user.getEmail() );
        u.setPassword( user.getPassword() );

        // force the user to change their password when they log in next
        u.setPasswordChangeRequired( true );

        // Disable Password Rules for this creation.
        UserSecurityPolicy securityPolicy = securitySystem.getPolicy();
        try
        {
        	// REDBACK-156
            securityPolicy.setEnabled( false );
            u.setValidated( true );
            manager.addUser( u );
            String currentUser = getCurrentUser();
            AuditEvent event = new AuditEvent( getText( "log.account.create" ) );
            event.setAffectedUser( u.getUsername() );
            event.setCurrentUser( currentUser );
            event.log();
        }
        finally
        {
            securityPolicy.setEnabled( true );
        }

        return SUCCESS;
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

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_USER_CREATE_OPERATION, Resource.GLOBAL );
        return bundle;
    }

}
