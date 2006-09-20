package org.apache.maven.archiva.web.action.admin;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.archiva.web.util.RoleManager;
import org.codehaus.plexus.security.policy.UserSecurityPolicy;
import org.codehaus.plexus.security.ui.web.action.AbstractUserCredentialsAction;
import org.codehaus.plexus.security.ui.web.model.EditUserCredentials;
import org.codehaus.plexus.security.user.User;
import org.codehaus.plexus.security.user.UserManager;

/**
 * AddAdminUserAction 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action"
 *                   role-hint="addAdminAction"
 *                   instantiation-strategy="per-lookup"
 */
public class AddAdminUserAction
    extends AbstractUserCredentialsAction
{
    /**
     * @plexus.requirement
     */
    private RoleManager roleManager;

    /**
     * @plexus.requirement
     */
    private UserManager userManager;
    
    /**
     * @plexus.requirement
     */
    private UserSecurityPolicy userSecurityPolicy;
    
    private EditUserCredentials user;
    
    public String show()
    {
        if ( user == null )
        {
            user = new EditUserCredentials( RoleManager.ADMIN_USERNAME );
        }
        
        return INPUT;
    }

    public String submit()
    {
        if ( user == null )
        {
            user = new EditUserCredentials( RoleManager.ADMIN_USERNAME );
            addActionError( "Invalid admin credentials, try again." );
            return ERROR;
        }
        
        getLogger().info( "user = " + user );
        
        // ugly hack to get around lack of cross module plexus-cdc efforts.
        super.manager = userManager;
        super.securityPolicy = userSecurityPolicy;
        // TODO: Fix plexus-cdc to operate properly for cross-module creation efforts.
        
        internalUser = user;
        
        validateCredentialsStrict();
        
        if ( userManager.userExists( RoleManager.ADMIN_USERNAME ) )
        {
            // Means that the role name exist already.
            // We need to fail fast and return to the previous page.
            addActionError( "Admin User exists in database (someone else probably created the user before you)." );
            return ERROR;
        }
        
        if ( hasActionErrors() || hasFieldErrors() )
        {
            return ERROR;
        }

        User u = userManager.createUser( RoleManager.ADMIN_USERNAME, user.getFullName(), user.getEmail() );
        if ( u == null )
        {
            addActionError( "Unable to operate on null user." );
            return ERROR;
        }

        u.setPassword( user.getPassword() );
        u.setLocked( false );
        u.setPasswordChangeRequired( false );

        
        userManager.addUser( u );

        roleManager.addAdminUser( u.getPrincipal().toString() );

        return SUCCESS;
    }

    public EditUserCredentials getUser()
    {
        return user;
    }

    public void setUser( EditUserCredentials user )
    {
        this.user = user;
    }
}