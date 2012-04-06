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

import org.codehaus.plexus.redback.policy.PasswordRuleViolationException;
import org.codehaus.plexus.redback.rbac.Permission;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.rbac.Role;
import org.codehaus.plexus.redback.system.SecuritySystem;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.redback.integration.model.UserCredentials;
import org.codehaus.redback.integration.role.RoleConstants;
import org.codehaus.redback.integration.security.role.RedbackRoleConstants;
import org.codehaus.redback.integration.util.RoleSorter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AbstractUserCredentialsAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractUserCredentialsAction
    extends AbstractSecurityAction
{
    // ------------------------------------------------------------------
    //  Component Requirements
    // ------------------------------------------------------------------

    /**
     *
     */
    @Inject
    @Named( value = "rBACManager#cached" )
    private RBACManager manager;

    /**
     *
     */
    @Inject
    protected SecuritySystem securitySystem;

    // ------------------------------------------------------------------
    // Action Parameters
    // ------------------------------------------------------------------

    protected UserCredentials internalUser;

    protected final String VALID_USERNAME_CHARS = "[a-zA-Z_0-9\\-.@]*";

    public RBACManager getManager()
    {
        return manager;
    }

    public void setManager( RBACManager manager )
    {
        this.manager = manager;
    }

    public SecuritySystem getSecuritySystem()
    {
        return securitySystem;
    }

    public void setSecuritySystem( SecuritySystem securitySystem )
    {
        this.securitySystem = securitySystem;
    }

    // ------------------------------------------------------------------
    // Action Entry Points - (aka Names)
    // ------------------------------------------------------------------

    public void validateCredentialsLoose()
    {
        if ( StringUtils.isEmpty( internalUser.getUsername() ) )
        {
            addFieldError( "user.username", getText( "username.required" ) );
        }
        else
        {
            if ( !internalUser.getUsername().matches( VALID_USERNAME_CHARS ) )
            {
                addFieldError( "user.username", getText( "username.invalid.characters" ) );
            }
        }

        if ( StringUtils.isEmpty( internalUser.getFullName() ) )
        {
            addFieldError( "user.fullName", getText( "fullName.required" ) );
        }

        if ( StringUtils.isEmpty( internalUser.getEmail() ) )
        {
            addFieldError( "user.email", getText( "email.required" ) );
        }

        if ( !StringUtils.equals( internalUser.getPassword(), internalUser.getConfirmPassword() ) )
        {
            addFieldError( "user.confirmPassword", getText( "passwords.does.not.match" ) );
        }

        try
        {
            if ( !StringUtils.isEmpty( internalUser.getEmail() ) )
            {
                new InternetAddress( internalUser.getEmail(), true );
            }
        }
        catch ( AddressException e )
        {
            addFieldError( "user.email", getText( "email.invalid" ) );
        }
    }

    public void validateCredentialsStrict()
    {
        validateCredentialsLoose();

        User tmpuser = internalUser.createUser( securitySystem.getUserManager() );

        try
        {
            securitySystem.getPolicy().validatePassword( tmpuser );
        }
        catch ( PasswordRuleViolationException e )
        {
            processPasswordRuleViolations( e );
        }

        if ( ( StringUtils.isEmpty( internalUser.getPassword() ) ) )
        {
            addFieldError( "user.password", getText( "password.required" ) );
        }
    }

    /**
     * this is a hack. this is a hack around the requirements of putting RBAC constraints into the model. this adds one
     * very major restriction to this security system, that a role name must contain the identifiers of the resource
     * that is being constrained for adding and granting of roles, this is unacceptable in the long term and we need to
     * get the model refactored to include this RBAC concept
     *
     * @param roleList
     * @return
     * @throws org.codehaus.plexus.redback.rbac.RbacManagerException
     *
     */
    protected List<Role> filterRolesForCurrentUserAccess( List<Role> roleList )
        throws RbacManagerException
    {
        String currentUser = getCurrentUser();

        List<Role> filteredRoleList = new ArrayList<Role>();

        Map<String, List<Permission>> assignedPermissionMap = manager.getAssignedPermissionMap( currentUser );
        List<String> resourceGrants = new ArrayList<String>();

        if ( assignedPermissionMap.containsKey( RedbackRoleConstants.USER_MANAGEMENT_ROLE_GRANT_OPERATION ) )
        {
            List<Permission> roleGrantPermissions =
                assignedPermissionMap.get( RedbackRoleConstants.USER_MANAGEMENT_ROLE_GRANT_OPERATION );

            for ( Permission permission : roleGrantPermissions )
            {
                if ( permission.getResource().getIdentifier().equals( Resource.GLOBAL ) )
                {
                    // the current user has the rights to assign any given role
                    return roleList;
                }
                else
                {
                    resourceGrants.add( permission.getResource().getIdentifier() );
                }
            }
        }
        else
        {
            return Collections.emptyList();
        }

        String delimiter = " - ";

        // we should have a list of resourceGrants now, this will provide us with the information necessary to restrict
        // the role list
        for ( Role role : roleList )
        {
            int delimiterIndex = role.getName().indexOf( delimiter );
            for ( String resourceIdentifier : resourceGrants )
            {

                if ( ( role.getName().indexOf( resourceIdentifier ) != -1 ) && ( delimiterIndex != -1 ) )
                {
                    String resourceName = role.getName().substring( delimiterIndex + delimiter.length() );
                    if ( resourceName.equals( resourceIdentifier ) )
                    {
                        filteredRoleList.add( role );
                    }
                }
            }
        }

        Collections.sort( filteredRoleList, new RoleSorter() );
        return filteredRoleList;
    }

    protected List<Role> getFilteredRolesForCurrentUserAccess()
        throws RbacManagerException
    {
        List<Role> roles = manager.getAllRoles();

        if ( roles == null )
        {
            return Collections.emptyList();
        }

        return filterRolesForCurrentUserAccess( roles );
    }
}
