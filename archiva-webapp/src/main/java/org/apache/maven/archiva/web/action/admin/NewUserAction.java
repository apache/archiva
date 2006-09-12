package org.apache.maven.archiva.web.action.admin;

/*
* Copyright 2005 The Apache Software Foundation.
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
import org.codehaus.plexus.security.system.SecuritySystem;
import org.codehaus.plexus.security.user.User;
import org.codehaus.plexus.security.user.UserManager;
import org.codehaus.plexus.security.user.policy.PasswordRuleViolationException;
import org.codehaus.plexus.security.user.policy.PasswordRuleViolations;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.util.Iterator;
import java.util.List;

/**
 * LoginAction:
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id:$
 * @plexus.component role="com.opensymphony.xwork.Action"
 * role-hint="newUser"
 */
public class NewUserAction
    extends PlexusActionSupport
{

    /**
     * @plexus.requirement
     */
    private SecuritySystem securitySystem;

    /**
     * @plexus.requirement
     */
    private RoleManager roleManager;

    private String username;

    private String password;

    private String passwordConfirm;

    private String email;

    private String fullName;

    public String createUser()
    {
        if ( username == null )
        {
            return INPUT;
        }

        // TODO: use commons-validator for these fields.

        if ( StringUtils.isEmpty( username ) )
        {
            addActionError( "User Name is required." );
        }

        if ( StringUtils.isEmpty( fullName ) )
        {
            addActionError( "Full Name is required." );
        }

        if ( StringUtils.isEmpty( email ) )
        {
            addActionError( "Email Address is required." );
        }

        // TODO: Validate Email Address (use commons-validator)

        if ( StringUtils.equals( password, passwordConfirm ) )
        {
            addActionError( "Passwords do not match." );
        }

        UserManager um = securitySystem.getUserManager();

        if ( um.userExists( username ) )
        {
            addActionError( "User already exists!" );
        }
        else
        {
            User user = um.createUser( username, fullName, email );

            user.setPassword( password );

            try
            {
                um.addUser( user );
            }
            catch ( PasswordRuleViolationException e )
            {
                PasswordRuleViolations violations = e.getViolations();
                List violationList = violations.getLocalizedViolations();
                Iterator it = violationList.iterator();
                while ( it.hasNext() )
                {
                    addActionError( (String) it.next() );
                }
            }
            roleManager.addUser( user.getPrincipal().toString() );

            addActionMessage( "user " + username + " was successfully registered!");
        }
        
        if ( hasActionErrors() )
        {
            return INPUT;
        }

        return SUCCESS;
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

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName( String fullName )
    {
        this.fullName = fullName;
    }

    public String getPasswordConfirm()
    {
        return passwordConfirm;
    }

    public void setPasswordConfirm( String passwordConfirm )
    {
        this.passwordConfirm = passwordConfirm;
    }
}
