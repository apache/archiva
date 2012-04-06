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

import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.RbacObjectNotFoundException;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.rbac.Role;
import org.codehaus.plexus.redback.rbac.UserAssignment;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.model.ModelApplication;
import org.codehaus.plexus.redback.struts2.action.AbstractUserCredentialsAction;
import org.codehaus.plexus.redback.struts2.action.AuditEvent;
import org.codehaus.plexus.redback.struts2.model.ApplicationRoleDetails;
import org.codehaus.plexus.redback.struts2.model.ApplicationRoleDetails.RoleTableCell;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.UserManager;
import org.codehaus.plexus.redback.users.UserNotFoundException;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.redback.integration.model.AdminEditUserCredentials;
import org.codehaus.redback.integration.role.RoleConstants;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * AssignmentsAction
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Controller("redback-assignments")
@Scope("prototype")
public class AssignmentsAction
    extends AbstractUserCredentialsAction
{
    // ------------------------------------------------------------------
    //  Component Requirements
    // ------------------------------------------------------------------

    /**
     *  role-hint="default"
     */
    @Inject
    private RoleManager rmanager;

    // ------------------------------------------------------------------
    // Action Parameters
    // ------------------------------------------------------------------

    private String principal;

    private AdminEditUserCredentials user;

    /**
     * A List of {@link Role} objects.
     */
    private List<Role> assignedRoles;

    /**
     * A List of {@link Role} objects.
     */
    private List<Role> availableRoles;

    private List<Role> effectivelyAssignedRoles;

    /**
     * List of names (received from client) of dynamic roles to set/unset
     */
    private List<String> addDSelectedRoles;

    /**
     * List of names (received from client) of nondynamic roles to set/unset
     */
    private List<String> addNDSelectedRoles;

    private List<Role> nondynamicroles;

    private List<Role> dynamicroles;

    private List<String> NDRoles;

    private List<String> DRoles;

    private List<ApplicationRoleDetails> applicationRoleDetails = new ArrayList<ApplicationRoleDetails>();

    // ------------------------------------------------------------------
    // Action Entry Points - (aka Names)
    // ------------------------------------------------------------------

    public List<ApplicationRoleDetails> getApplicationRoleDetails()
    {
        return applicationRoleDetails;
    }

    /**
     * Display the edit user panel. <p/> This should consist of the Role details for the specified user. <p/> A table of
     * currently assigned roles. This table should have a column to remove the role from the user. This table should
     * also have a column of checkboxes that can be selected and then removed from the user. <p/> A table of roles that
     * can be assigned. This table should have a set of checkboxes that can be selected and then added to the user. <p/>
     * Duplicate role assignment needs to be taken care of.
     * 
     * @throws RbacManagerException
     * @throws RbacObjectNotFoundException
     */
    @SuppressWarnings( "unchecked" )
    public String show()
        throws RbacManagerException
    {
        this.addNDSelectedRoles = new ArrayList<String>();
        this.addDSelectedRoles = new ArrayList<String>();

        if ( StringUtils.isEmpty( principal ) )
        {
            addActionError( getText( "rbac.edit.user.empty.principal" ) );
            return ERROR;
        }

        UserManager userManager = super.securitySystem.getUserManager();

        if ( !userManager.userExists( principal ) )
        {
            addActionError( getText( "user.does.not.exist", new String[]{principal} ) );
            return ERROR;
        }

        try
        {
            User u = userManager.findUser( principal );

            if ( u == null )
            {
                addActionError( getText( "cannot.operate.on.null.user" ) );
                return ERROR;
            }

            user = new AdminEditUserCredentials( u );
        }
        catch ( UserNotFoundException e )
        {
            addActionError( getText( "user.not.found.exception", Arrays.asList( ( Object ) principal, e.getMessage() ) ) );
            return ERROR;
        }

        // check first if role assignments for user exist
        if ( !getManager().userAssignmentExists( principal ) )
        {
            UserAssignment assignment = getManager().createUserAssignment( principal );
            getManager().saveUserAssignment( assignment );
        }

        List<Role> assignableRoles = getFilteredRolesForCurrentUserAccess();
        List<ApplicationRoleDetails> appRoleDetails = lookupAppRoleDetails( principal, assignableRoles );
        applicationRoleDetails.addAll( appRoleDetails );

        return SUCCESS;
    }

    @SuppressWarnings( "unchecked" )
    private List<ApplicationRoleDetails> lookupAppRoleDetails( String principal, List<Role> assignableRoles )
        throws RbacManagerException
    {
        List<ApplicationRoleDetails> appRoleDetails = new ArrayList<ApplicationRoleDetails>();
        for ( Iterator<ModelApplication> i = rmanager.getModel().getApplications().iterator(); i.hasNext(); )
        {
            ModelApplication application = i.next();
            ApplicationRoleDetails details =
                new ApplicationRoleDetails( application, getManager().getEffectivelyAssignedRoles( principal ),
                                            getManager().getAssignedRoles( principal ), assignableRoles );
            appRoleDetails.add( details );
        }
        return appRoleDetails;
    }

    /**
     * Applies role additions and removals and then displays the edit user panel.
     * 
     * @return
     */
    public String edituser()
    {
        try
        {
            Collection<Role> assignedRoles = getManager().getAssignedRoles( principal );
            List<Role> assignableRoles = getFilteredRolesForCurrentUserAccess();

            // Compute set of roles usable by configured apps, add/del from this set only
            List<ApplicationRoleDetails> appRoleDetails = lookupAppRoleDetails( principal, assignableRoles );
            applicationRoleDetails.addAll( appRoleDetails );

            Set<String> availableAppRoleNames = new HashSet<String>();
            for ( ApplicationRoleDetails appRoleDetail : applicationRoleDetails )
            {
                availableAppRoleNames.addAll( appRoleDetail.getAssignedRoles() );
                availableAppRoleNames.addAll( appRoleDetail.getAvailableRoles() );

                // Add dynamic roles offered on page
                for ( List<RoleTableCell> row : appRoleDetail.getTable() )
                {
                    for ( RoleTableCell col : row )
                    {
                        if ( !col.isLabel() )
                        {
                            availableAppRoleNames.add( col.getName() );
                        }
                    }
                }
            }

            Set<Role> availableRoles = new HashSet<Role>( assignedRoles );
            availableRoles.addAll( assignableRoles );

            // Filter the available roles so we only consider configured app roles
            Iterator<Role> availableRoleIterator = availableRoles.iterator();
            while ( availableRoleIterator.hasNext() )
            {
                Role availableRole = availableRoleIterator.next();
                if ( !availableAppRoleNames.contains( availableRole.getName() ) )
                {
                    availableRoleIterator.remove();
                }
            }

            List<String> selectedRoleNames = new ArrayList<String>();
            addSelectedRoles( availableRoles, selectedRoleNames, addNDSelectedRoles );
            addSelectedRoles( availableRoles, selectedRoleNames, addDSelectedRoles );

            List<String> newRoles = new ArrayList<String>( selectedRoleNames );
            String currentUser = getCurrentUser();
            for ( Role assignedRole : assignedRoles )
            {
                if ( !selectedRoleNames.contains( assignedRole.getName() ) )
                {
                    // removing a currently assigned role, check if we have permission
                    if ( !availableRoles.contains( assignedRole )
                        || !checkRoleName( assignableRoles, assignedRole.getName() ) )
                    {
                        // it may have not been on the page. Leave it assigned.
                        selectedRoleNames.add( assignedRole.getName() );
                    }
                    else
                    {
                        String role = assignedRole.getName();
                        AuditEvent event = new AuditEvent( getText( "log.revoke.role" ) );
                        event.setAffectedUser( principal );
                        event.setRole( role );
                        event.setCurrentUser( currentUser );
                        event.log();
                    }
                }
                else
                {
                    newRoles.remove( assignedRole.getName() );
                }
            }
            for ( String r : newRoles )
            {
                AuditEvent event = new AuditEvent( getText( "log.assign.role" ) );
                event.setAffectedUser( principal );
                event.setRole( r );
                event.setCurrentUser( currentUser );
                event.log();
            }

            UserAssignment assignment;

            if ( getManager().userAssignmentExists( principal ) )
            {
                assignment = getManager().getUserAssignment( principal );
            }
            else
            {
                assignment = getManager().createUserAssignment( principal );
            }

            assignment.setRoleNames( selectedRoleNames );

            assignment = getManager().saveUserAssignment( assignment );
        }
        catch ( RbacManagerException ne )
        {
            addActionError( getText( "error.removing.selected.roles", Arrays.asList( ( Object ) ne.getMessage() ) ) );
            return ERROR;
        }
        return SUCCESS;
    }

    private void addSelectedRoles( Collection<Role> assignableRoles, List<String> roles, List<String> selectedRoles )
    {
        if ( selectedRoles != null )
        {
            for ( String r : selectedRoles )
            {
                if ( checkRoleName( assignableRoles, r ) )
                {
                    roles.add( r );
                }
            }
        }
    }

    private boolean checkRoleName( Collection<Role> assignableRoles, String r )
    {
        for ( Role role : assignableRoles )
        {
            if ( role.getName().equals( r ) )
            {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Parameter Accessor Methods
    // ------------------------------------------------------------------

    public List<Role> getAssignedRoles()
    {
        return assignedRoles;
    }

    public void setAssignedRoles( List<Role> assignedRoles )
    {
        this.assignedRoles = assignedRoles;
    }

    public List<Role> getAvailableRoles()
    {
        return availableRoles;
    }

    public void setAvailableRoles( List<Role> availableRoles )
    {
        this.availableRoles = availableRoles;
    }

    public List<Role> getEffectivelyAssignedRoles()
    {
        return effectivelyAssignedRoles;
    }

    public void setEffectivelyAssignedRoles( List<Role> effectivelyAssignedRoles )
    {
        this.effectivelyAssignedRoles = effectivelyAssignedRoles;
    }

    public String getPrincipal()
    {
        return principal;
    }

    public void setPrincipal( String principal )
    {
        this.principal = principal;
    }

    public void setUsername( String username )
    {
        this.principal = username;
    }

    public AdminEditUserCredentials getUser()
    {
        return user;
    }

    public SecureActionBundle initSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();
        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION, Resource.GLOBAL );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION, Resource.GLOBAL );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_ROLE_GRANT_OPERATION, Resource.GLOBAL );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_ROLE_DROP_OPERATION, Resource.GLOBAL );
        bundle.addRequiredAuthorization( RoleConstants.USER_MANAGEMENT_USER_ROLE_OPERATION, Resource.GLOBAL );

        return bundle;
    }

    public List<Role> getNondynamicroles()
    {
        return nondynamicroles;
    }

    public void setNondynamicroles( List<Role> nondynamicroles )
    {
        this.nondynamicroles = nondynamicroles;
    }

    public List<Role> getDynamicroles()
    {
        return dynamicroles;
    }

    public void setDynamicroles( List<Role> dynamicroles )
    {
        this.dynamicroles = dynamicroles;
    }

    public List<String> getNDRoles()
    {
        return NDRoles;
    }

    public void setNDRoles( List<String> roles )
    {
        NDRoles = roles;
    }

    public List<String> getDRoles()
    {
        return DRoles;
    }

    public void setDRoles( List<String> roles )
    {
        DRoles = roles;
    }

    public List<String> getAddDSelectedRoles()
    {
        return addDSelectedRoles;
    }

    public void setAddDSelectedRoles( List<String> addDSelectedRoles )
    {
        this.addDSelectedRoles = addDSelectedRoles;
    }

    public List<String> getAddNDSelectedRoles()
    {
        return addNDSelectedRoles;
    }

    public void setAddNDSelectedRoles( List<String> addNDSelectedRoles )
    {
        this.addNDSelectedRoles = addNDSelectedRoles;
    }
}
