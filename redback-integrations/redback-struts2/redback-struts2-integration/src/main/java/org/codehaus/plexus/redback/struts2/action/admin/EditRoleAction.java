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

import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.plexus.redback.rbac.Permission;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.rbac.Role;
import org.codehaus.plexus.redback.rbac.UserAssignment;
import org.codehaus.plexus.redback.struts2.action.AbstractUserCredentialsAction;
import org.codehaus.plexus.redback.struts2.action.AuditEvent;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.role.RoleConstants;
import org.codehaus.redback.integration.security.role.RedbackRoleConstants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * EditRoleAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller( "redback-role-edit" )
@Scope( "prototype" )
public class EditRoleAction
    extends AbstractUserCredentialsAction
{
    // ------------------------------------------------------------------
    // Action Parameters
    // ------------------------------------------------------------------

    private String name;

    private String description;

    private String newDescription;

    private List<String> childRoleNames = new ArrayList<String>();

    private List<String> parentRoleNames = new ArrayList<String>();

    private List<Permission> permissions = new ArrayList<Permission>();

    private List<User> users = new ArrayList<User>();

    private List<User> parentUsers = new ArrayList<User>();

    private List<User> allUsers = new ArrayList<User>();

    private List<String> usersList = new ArrayList<String>();

    private List<String> availableUsers = new ArrayList<String>();

    private List<String> currentUsers = new ArrayList<String>();

    // ------------------------------------------------------------------
    // Action Entry Points - (aka Names)
    // ------------------------------------------------------------------

    public String input()
    {
        if ( name == null )
        {
            addActionError( getText( "cannot.edit.null.role" ) );
            return ERROR;
        }

        if ( StringUtils.isEmpty( name ) )
        {
            addActionError( getText( "cannot.edit.empty.role" ) );
            return ERROR;
        }

        name = StringEscapeUtils.escapeXml( name );

        if ( !getManager().roleExists( name ) )
        {
            // Means that the role name doesn't exist.
            // We should exit early and not attempt to look up the role information.
            return INPUT;
        }

        try
        {
            if ( !isAuthorized() )
            {
                log.warn( getCurrentUser() + " isn't authorized to access to the role '" + name + "'" );
                addActionError( getText( "alert.message" ) );
                return ERROR;
            }

            Role role = getManager().getRole( name );
            if ( role == null )
            {
                addActionError( getText( "cannot.operate.null.role" ) );
                return ERROR;
            }

            description = role.getDescription();
            childRoleNames = role.getChildRoleNames();
            Map<String, Role> parentRoles = getManager().getParentRoles( role );
            for ( String roleName : parentRoles.keySet() )
            {
                parentRoleNames.add( roleName );
            }
            permissions = role.getPermissions();

            //Get users of the current role
            List<String> roles = new ArrayList<String>();
            roles.add( name );
            List<UserAssignment> userAssignments = getManager().getUserAssignmentsForRoles( roles );
            users = new ArrayList<User>();
            if ( userAssignments != null )
            {
                for ( UserAssignment userAssignment : userAssignments )
                {
                    try
                    {
                        User user = getUserManager().findUser( userAssignment.getPrincipal() );
                        users.add( user );
                    }
                    catch ( UserNotFoundException e )
                    {
                        log.warn( "User '" + userAssignment.getPrincipal() + "' doesn't exist.", e );
                    }
                }
            }

            //Get users of the parent roles
            parentUsers = new ArrayList<User>();
            if ( !parentRoles.isEmpty() )
            {
                List<UserAssignment> userParentAssignments =
                    getManager().getUserAssignmentsForRoles( parentRoles.keySet() );
                if ( userParentAssignments != null )
                {
                    for ( UserAssignment userAssignment : userParentAssignments )
                    {
                        try
                        {
                            User user = getUserManager().findUser( userAssignment.getPrincipal() );
                            parentUsers.add( user );
                        }
                        catch ( UserNotFoundException e )
                        {
                            log.warn( "User '" + userAssignment.getPrincipal() + "' doesn't exist.", e );
                        }
                    }
                }
            }
        }
        catch ( RbacManagerException e )
        {
            List<Object> list = new ArrayList<Object>();
            list.add( name );
            list.add( e.getMessage() );
            addActionError( getText( "cannot.get.role", list ) );
            return ERROR;
        }

        return INPUT;
    }

    private boolean isAuthorized()
        throws RbacManagerException
    {
        List<Role> assignableRoles = getFilteredRolesForCurrentUserAccess();
        boolean updatableRole = false;
        for ( Role r : assignableRoles )
        {
            if ( r.getName().equalsIgnoreCase( name ) )
            {
                updatableRole = true;
            }
        }

        return updatableRole;
    }

    public String edit()
    {
        String result = input();
        if ( ERROR.equals( result ) )
        {
            return result;
        }

        newDescription = description;

        //TODO: Remove all users defined in parent roles too
        allUsers = getUserManager().getUsers();

        for ( User user : users )
        {
            if ( allUsers.contains( user ) )
            {
                allUsers.remove( user );
            }
        }

        for ( User user : parentUsers )
        {
            if ( allUsers.contains( user ) )
            {
                allUsers.remove( user );
            }
        }

        return result;
    }

    public String save()
    {
        String result = input();
        if ( ERROR.equals( result ) )
        {
            return result;
        }

        if ( name == null )
        {
            addActionError( getText( "cannot.edit.null.role" ) );
            return ERROR;
        }

        if ( StringUtils.isEmpty( name ) )
        {
            addActionError( getText( "cannot.edit.empty.role" ) );
            return ERROR;
        }

        try
        {
            Role role;
            if ( getManager().roleExists( name ) )
            {
                role = getManager().getRole( name );
            }
            else
            {
                role = getManager().createRole( name );
            }

            //TODO: allow to modify childRoleNames and permissions
            role.setDescription( newDescription );
            //role.setChildRoleNames( childRoleNames );
            //role.setPermissions( permissions );

            getManager().saveRole( role );

            List<Object> list = new ArrayList<Object>();
            list.add( name );
            String currentUser = getCurrentUser();
            AuditEvent event = new AuditEvent( getText( "log.role.edit" ) );
            event.setRole( name );
            event.setCurrentUser( currentUser );
            event.log();
            addActionMessage( getText( "save.role.success", list ) );
        }
        catch ( RbacManagerException e )
        {
            List<Object> list = new ArrayList<Object>();
            list.add( name );
            list.add( e.getMessage() );
            addActionError( getText( "cannot.get.role", list ) );
            return ERROR;
        }

        return SUCCESS;
    }

    public String addUsers()
    {
        if ( availableUsers == null || availableUsers.isEmpty() )
        {
            return INPUT;
        }

        for ( String principal : availableUsers )
        {
            if ( !getUserManager().userExists( principal ) )
            {
                // Means that the role name doesn't exist.
                // We need to fail fast and return to the previous page.
                List<Object> list = new ArrayList<Object>();
                list.add( principal );
                addActionError( getText( "user.does.not.exist", list ) );
                return ERROR;
            }

            try
            {
                UserAssignment assignment;

                if ( getManager().userAssignmentExists( principal ) )
                {
                    assignment = getManager().getUserAssignment( principal );
                }
                else
                {
                    assignment = getManager().createUserAssignment( principal );
                }

                assignment.addRoleName( name );
                assignment = getManager().saveUserAssignment( assignment );
                log.info( "{} role assigned to {}", name, principal );
            }
            catch ( RbacManagerException e )
            {
                List<Object> list = new ArrayList<Object>();
                list.add( principal );
                list.add( e.getMessage() );
                addActionError( getText( "cannot.assign.role", list ) );
                return ERROR;
            }
        }

        edit();
        return SUCCESS;
    }

    public String removeUsers()
    {
        if ( currentUsers == null || currentUsers.isEmpty() )
        {
            return INPUT;
        }

        for ( String principal : currentUsers )
        {
            if ( !getUserManager().userExists( principal ) )
            {
                // Means that the role name doesn't exist.
                // We need to fail fast and return to the previous page.
                List<Object> list = new ArrayList<Object>();
                list.add( principal );
                addActionError( getText( "user.does.not.exist", list ) );
                return ERROR;
            }

            try
            {
                UserAssignment assignment;

                if ( getManager().userAssignmentExists( principal ) )
                {
                    assignment = getManager().getUserAssignment( principal );
                }
                else
                {
                    assignment = getManager().createUserAssignment( principal );
                }

                assignment.removeRoleName( name );
                assignment = getManager().saveUserAssignment( assignment );
                log.info( "{} role unassigned to {}", name, principal );
            }
            catch ( RbacManagerException e )
            {
                List<Object> list = new ArrayList<Object>();
                list.add( principal );
                list.add( e.getMessage() );
                addActionError( getText( "cannot.assign.role", list ) );
                return ERROR;
            }
        }

        edit();
        return SUCCESS;
    }

    private UserManager getUserManager()
    {
        return securitySystem.getUserManager();
    }

    // ------------------------------------------------------------------
    // Parameter Accessor Methods
    // ------------------------------------------------------------------

    public String getName()
    {
        return name;
    }

    public void setName( String roleName )
    {
        this.name = roleName;
    }

    public List<String> getChildRoleNames()
    {
        return childRoleNames;
    }

    public void setChildRoleNames( List<String> childRoleNames )
    {
        this.childRoleNames = childRoleNames;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getNewDescription()
    {
        return newDescription;
    }

    public void setNewDescription( String newDescription )
    {
        this.newDescription = newDescription;
    }

    public List<Permission> getPermissions()
    {
        return permissions;
    }

    public void setPermissions( List<Permission> permissions )
    {
        this.permissions = permissions;
    }

    public List<User> getUsers()
    {
        return users;
    }

    public void setUsers( List<User> users )
    {
        this.users = users;
    }

    public List<User> getAllUsers()
    {
        return allUsers;
    }

    public void setAllUsers( List<User> allUsers )
    {
        this.allUsers = allUsers;
    }

    public List<String> getUsersList()
    {
        return usersList;
    }

    public void setUsersList( List<String> usersList )
    {
        this.usersList = usersList;
    }

    public List<String> getAvailableUsers()
    {
        return availableUsers;
    }

    public void setAvailableUsers( List<String> availableUsers )
    {
        this.availableUsers = availableUsers;
    }

    public List<String> getCurrentUsers()
    {
        return currentUsers;
    }

    public void setCurrentUsers( List<String> currentUsers )
    {
        this.currentUsers = currentUsers;
    }

    public List<String> getParentRoleNames()
    {
        return parentRoleNames;
    }

    public void setParentRoleNames( List<String> parentRoleNames )
    {
        this.parentRoleNames = parentRoleNames;
    }

    public List<User> getParentUsers()
    {
        return parentUsers;
    }

    public void setParentUsers( List<User> parentUsers )
    {
        this.parentUsers = parentUsers;
    }

    // ------------------------------------------------------------------
    // Internal Support Methods
    // ------------------------------------------------------------------

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION, Resource.GLOBAL );
        bundle.addRequiredAuthorization( RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION, Resource.GLOBAL );
        bundle.addRequiredAuthorization( RedbackRoleConstants.USER_MANAGEMENT_ROLE_GRANT_OPERATION, Resource.GLOBAL );
        bundle.addRequiredAuthorization( RedbackRoleConstants.USER_MANAGEMENT_ROLE_DROP_OPERATION, Resource.GLOBAL );
        bundle.addRequiredAuthorization( RedbackRoleConstants.USER_MANAGEMENT_USER_ROLE_OPERATION, Resource.GLOBAL );
        return bundle;
    }
}
