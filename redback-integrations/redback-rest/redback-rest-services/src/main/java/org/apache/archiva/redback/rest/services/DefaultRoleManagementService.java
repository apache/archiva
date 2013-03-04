package org.apache.archiva.redback.rest.services;
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

import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.role.model.ModelApplication;
import org.apache.archiva.redback.role.model.ModelRole;
import org.apache.archiva.redback.role.model.ModelTemplate;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.redback.integration.model.AdminEditUserCredentials;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.integration.util.RoleSorter;
import org.apache.archiva.redback.rest.api.model.Application;
import org.apache.archiva.redback.rest.api.model.ApplicationRoles;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.Role;
import org.apache.archiva.redback.rest.api.model.RoleTemplate;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.api.services.RoleManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Olivier Lamy
 * @since 1.3
 */
@Service("roleManagementService#rest")
public class DefaultRoleManagementService
    implements RoleManagementService
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private RoleManager roleManager;

    private RBACManager rbacManager;

    private UserManager userManager;

    @Inject
    public DefaultRoleManagementService( RoleManager roleManager,
                                         @Named(value = "rbacManager#default") RBACManager rbacManager,
                                         @Named(value = "userManager#default") UserManager userManager )
    {
        this.roleManager = roleManager;
        this.rbacManager = rbacManager;
        this.userManager = userManager;

        log.debug( "use rbacManager impl: {}", rbacManager.getClass().getName() );
        log.debug( "use userManager impl: {}", userManager.getClass().getName() );
    }

    public Boolean createTemplatedRole( String templateId, String resource )
        throws RedbackServiceException
    {
        try
        {
            roleManager.createTemplatedRole( templateId, resource );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public Boolean removeTemplatedRole( String templateId, String resource )
        throws RedbackServiceException
    {

        try
        {
            roleManager.removeTemplatedRole( templateId, resource );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public Boolean updateRole( String templateId, String oldResource, String newResource )
        throws RedbackServiceException
    {
        try
        {
            roleManager.updateRole( templateId, oldResource, newResource );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public Boolean assignRole( String roleId, String principal )
        throws RedbackServiceException
    {
        try
        {
            roleManager.assignRole( roleId, principal );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public Boolean assignRoleByName( String roleName, String principal )
        throws RedbackServiceException
    {
        try
        {
            roleManager.assignRoleByName( roleName, principal );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public Boolean assignTemplatedRole( String templateId, String resource, String principal )
        throws RedbackServiceException
    {
        try
        {
            roleManager.assignTemplatedRole( templateId, resource, principal );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public Boolean unassignRole( String roleId, String principal )
        throws RedbackServiceException
    {
        try
        {
            roleManager.unassignRole( roleId, principal );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public Boolean unassignRoleByName( String roleName, String principal )
        throws RedbackServiceException
    {
        try
        {
            roleManager.unassignRoleByName( roleName, principal );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public Boolean roleExists( String roleId )
        throws RedbackServiceException
    {
        try
        {
            return roleManager.roleExists( roleId );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
    }

    public Boolean templatedRoleExists( String templateId, String resource )
        throws RedbackServiceException
    {
        try
        {
            return roleManager.templatedRoleExists( templateId, resource );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }

    }

    public Boolean verifyTemplatedRole( String templateId, String resource )
        throws RedbackServiceException
    {
        try
        {
            roleManager.verifyTemplatedRole( templateId, resource );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public List<Role> getEffectivelyAssignedRoles( String username )
        throws RedbackServiceException
    {
        if ( StringUtils.isEmpty( username ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "user.cannot.be.null" ) );
        }
        try
        {
            List<org.apache.archiva.redback.rbac.Role> roles =
                filterAssignableRoles( rbacManager.getEffectivelyAssignedRoles( username ) );

            List<Role> effectivelyAssignedRoles = new ArrayList<Role>( roles.size() );

            for ( org.apache.archiva.redback.rbac.Role r : roles )
            {
                effectivelyAssignedRoles.add( new Role( r ) );
            }
            return effectivelyAssignedRoles;
        }
        catch ( RbacManagerException rme )
        {
            // ignore, this can happen when the user has no roles assigned  
        }
        return new ArrayList<Role>( 0 );
    }


    public List<Application> getApplications( String username )
        throws RedbackServiceException
    {

        List<ModelApplication> modelApplications = roleManager.getModel().getApplications();

        List<Application> applications = new ArrayList<Application>( modelApplications.size() );

        for ( ModelApplication modelApplication : modelApplications )
        {
            Application application = new Application();
            application.setDescription( modelApplication.getDescription() );
            application.setId( modelApplication.getId() );
            application.setLongDescription( modelApplication.getLongDescription() );
            application.setVersion( modelApplication.getVersion() );
            applications.add( application );
        }

        return applications;
    }

    public List<Role> getAllRoles()
        throws RedbackServiceException
    {
        try
        {
            List<org.apache.archiva.redback.rbac.Role> roles = rbacManager.getAllRoles();

            if ( roles == null )
            {
                return Collections.emptyList();
            }

            roles = filterRolesForCurrentUserAccess( roles );

            List<Role> res = new ArrayList<Role>( roles.size() );

            for ( org.apache.archiva.redback.rbac.Role r : roles )
            {
                res.add( new Role( r ) );
            }
            return res;

        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
    }

    public List<Role> getDetailedAllRoles()
        throws RedbackServiceException
    {
        try
        {
            List<org.apache.archiva.redback.rbac.Role> roles = rbacManager.getAllRoles();

            if ( roles == null )
            {
                return Collections.emptyList();
            }

            roles = filterRolesForCurrentUserAccess( roles );

            List<Role> res = new ArrayList<Role>( roles.size() );

            for ( org.apache.archiva.redback.rbac.Role r : roles )
            {
                res.add( getRole( r.getName() ) );
            }
            return res;

        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
    }

    private List<org.apache.archiva.redback.rbac.Role> filterAssignableRoles(
        Collection<org.apache.archiva.redback.rbac.Role> roles )
    {
        List<org.apache.archiva.redback.rbac.Role> assignableRoles =
            new ArrayList<org.apache.archiva.redback.rbac.Role>( roles.size() );
        for ( org.apache.archiva.redback.rbac.Role r : roles )
        {
            if ( r.isAssignable() )
            {
                assignableRoles.add( r );
            }
        }
        return assignableRoles;
    }

    public Role getRole( String roleName )
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.rbac.Role rbacRole = rbacManager.getRole( roleName );
            Role role = new Role( rbacRole );

            Map<String, org.apache.archiva.redback.rbac.Role> parentRoles = rbacManager.getParentRoles( rbacRole );
            for ( String parentRoleName : parentRoles.keySet() )
            {
                role.getParentRoleNames().add( parentRoleName );
            }

            List<UserAssignment> userAssignments = rbacManager.getUserAssignmentsForRoles( Arrays.asList( roleName ) );

            if ( userAssignments != null )
            {
                for ( UserAssignment userAssignment : userAssignments )
                {
                    try
                    {
                        User user = userManager.findUser( userAssignment.getPrincipal() );
                        role.getUsers().add( new org.apache.archiva.redback.rest.api.model.User( user ) );
                    }
                    catch ( UserNotFoundException e )
                    {
                        log.warn( "User '" + userAssignment.getPrincipal() + "' doesn't exist.", e );
                    }
                }
            }

            if ( !role.getParentRoleNames().isEmpty() )
            {
                List<UserAssignment> userParentAssignments =
                    rbacManager.getUserAssignmentsForRoles( parentRoles.keySet() );
                if ( userParentAssignments != null )
                {
                    for ( UserAssignment userAssignment : userParentAssignments )
                    {
                        try
                        {
                            User user = userManager.findUser( userAssignment.getPrincipal() );
                            role.getParentsRolesUsers().add(
                                new org.apache.archiva.redback.rest.api.model.User( user ) );
                        }
                        catch ( UserNotFoundException e )
                        {
                            log.warn( "User '" + userAssignment.getPrincipal() + "' doesn't exist.", e );
                        }
                    }
                }
            }

            List<org.apache.archiva.redback.rest.api.model.User> otherUsers =
                new ArrayList<org.apache.archiva.redback.rest.api.model.User>();
            for ( User u : userManager.getUsers() )
            {
                org.apache.archiva.redback.rest.api.model.User user =
                    new org.apache.archiva.redback.rest.api.model.User( u );
                if ( role.getParentsRolesUsers().contains( user ) )
                {
                    continue;
                }
                if ( role.getUsers().contains( user ) )
                {
                    continue;
                }
                otherUsers.add( user );
            }

            role.setOtherUsers( otherUsers );

            return role;
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }
    }

    public Boolean updateRoleDescription( String roleName, String description )
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.rbac.Role rbacRole = rbacManager.getRole( roleName );
            rbacRole.setDescription( description );
            rbacManager.saveRole( rbacRole );
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }
        return Boolean.TRUE;
    }

    public Boolean updateRoleUsers( Role role )
        throws RedbackServiceException
    {

        for ( org.apache.archiva.redback.rest.api.model.User user : role.getUsers() )
        {
            String username = user.getUsername();

            try
            {

                if ( !userManager.userExists( username ) )
                {
                    log.error( "user {} not exits", username );
                    throw new RedbackServiceException(
                        new ErrorMessage( "user.not.exists", new String[]{ username } ) );
                }

                UserAssignment assignment;

                if ( rbacManager.userAssignmentExists( username ) )
                {
                    assignment = rbacManager.getUserAssignment( username );
                }
                else
                {
                    assignment = rbacManager.createUserAssignment( username );
                }

                assignment.addRoleName( role.getName() );
                assignment = rbacManager.saveUserAssignment( assignment );
                log.info( "{} role assigned to {}", role.getName(), username );
            }
            catch ( RbacManagerException e )
            {
                log.error( "error during assign role " + role.getName() + " to user " + username, e );
                throw new RedbackServiceException(
                    new ErrorMessage( "error.assign.role.user", new String[]{ role.getName(), username } ) );
            }
            catch ( UserManagerException e )
            {
                throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
            }
        }

        for ( org.apache.archiva.redback.rest.api.model.User user : role.getRemovedUsers() )
        {
            String username = user.getUsername();

            try
            {

                if ( !userManager.userExists( username ) )
                {
                    log.error( "user {} not exits", username );
                    throw new RedbackServiceException(
                        new ErrorMessage( "user.not.exists", new String[]{ username } ) );
                }

                UserAssignment assignment;

                if ( rbacManager.userAssignmentExists( username ) )
                {
                    assignment = rbacManager.getUserAssignment( username );
                }
                else
                {
                    assignment = rbacManager.createUserAssignment( username );
                }

                assignment.removeRoleName( role.getName() );
                assignment = rbacManager.saveUserAssignment( assignment );
                log.info( "{} role unassigned to {}", role.getName(), username );
            }
            catch ( RbacManagerException e )
            {
                log.error( "error during unassign role " + role.getName() + " to user " + username, e );
                throw new RedbackServiceException(
                    new ErrorMessage( "error.unassign.role.user", new String[]{ role.getName(), username } ) );
            }
            catch ( UserManagerException e )
            {
                throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
            }
        }

        return Boolean.TRUE;
    }

    public List<ApplicationRoles> getApplicationRoles( String username )
        throws RedbackServiceException
    {
        AdminEditUserCredentials user = null;
        if ( StringUtils.isEmpty( username ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "rbac.edit.user.empty.principal" ) );
        }

        try
        {
            if ( !userManager.userExists( username ) )
            {
                throw new RedbackServiceException(
                    new ErrorMessage( "user.does.not.exist", new String[]{ username } ) );
            }

            User u = userManager.findUser( username );

            if ( u == null )
            {
                throw new RedbackServiceException( new ErrorMessage( "cannot.operate.on.null.user" ) );
            }

            user = new AdminEditUserCredentials( u );
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException(
                new ErrorMessage( "user.does.not.exist", new String[]{ username, e.getMessage() } ) );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }
        try
        {
            // check first if role assignments for user exist
            if ( !rbacManager.userAssignmentExists( username ) )
            {
                UserAssignment assignment = rbacManager.createUserAssignment( username );
                rbacManager.saveUserAssignment( assignment );
            }

            List<org.apache.archiva.redback.rbac.Role> allRoles =
                filterRolesForCurrentUserAccess( rbacManager.getAllRoles() );

            List<ModelApplication> modelApplications = roleManager.getModel().getApplications();

            List<ApplicationRoles> applicationRolesList = new ArrayList<ApplicationRoles>( modelApplications.size() );

            for ( ModelApplication modelApplication : modelApplications )
            {
                ApplicationRoles applicationRoles = new ApplicationRoles();

                applicationRoles.setDescription( modelApplication.getDescription() );
                applicationRoles.setName( modelApplication.getId() );

                Collection<org.apache.archiva.redback.rbac.Role> appRoles =
                    filterApplicationRoles( modelApplication, allRoles, modelApplication.getTemplates() );

                applicationRoles.setGlobalRoles( toRoleNames( appRoles ) );

                Set<String> resources = discoverResources( modelApplication.getTemplates(), appRoles );

                applicationRoles.setResources( resources );

                applicationRoles.setRoleTemplates( toRoleTemplates( modelApplication.getTemplates() ) );

                // cleanup app roles remove roles coming from templates

                List<String> appRoleNames = new ArrayList<String>( appRoles.size() );

                for ( String appRoleName : applicationRoles.getGlobalRoles() )
                {
                    if ( !roleFromTemplate( appRoleName, modelApplication.getTemplates() ) )
                    {
                        appRoleNames.add( appRoleName );
                    }
                }

                applicationRoles.setGlobalRoles( appRoleNames );

                applicationRolesList.add( applicationRoles );
            }

            return applicationRolesList;

        }
        catch ( RbacManagerException e )
        {
            RedbackServiceException redbackServiceException =
                new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
            redbackServiceException.setHttpErrorCode( Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
            throw redbackServiceException;
        }
    }

    public Boolean updateUserRoles( org.apache.archiva.redback.rest.api.model.User user )
        throws RedbackServiceException
    {

        String username = user.getUsername();

        if ( StringUtils.isEmpty( username ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "rbac.edit.user.empty.principal" ) );
        }

        try
        {

            if ( !userManager.userExists( username ) )
            {
                throw new RedbackServiceException(
                    new ErrorMessage( "user.does.not.exist", new String[]{ username } ) );
            }

            User u = userManager.findUser( username );

            if ( u == null )
            {
                throw new RedbackServiceException( new ErrorMessage( "cannot.operate.on.null.user" ) );
            }

        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException(
                new ErrorMessage( "user.does.not.exist", new String[]{ username, e.getMessage() } ) );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }

        try
        {

            UserAssignment assignment;

            if ( rbacManager.userAssignmentExists( username ) )
            {
                assignment = rbacManager.getUserAssignment( username );
            }
            else
            {
                assignment = rbacManager.createUserAssignment( username );
            }

            assignment.setRoleNames( user.getAssignedRoles() );

            assignment = rbacManager.saveUserAssignment( assignment );

        }
        catch ( RbacManagerException e )
        {
            RedbackServiceException redbackServiceException =
                new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
            redbackServiceException.setHttpErrorCode( Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() );
            throw redbackServiceException;
        }

        return Boolean.TRUE;

    }

    //----------------------------------------------------------------
    // Internal methods
    //----------------------------------------------------------------

    private org.apache.archiva.redback.rbac.Role isInList( String roleName,
                                                           Collection<org.apache.archiva.redback.rbac.Role> roles )
    {
        for ( org.apache.archiva.redback.rbac.Role role : roles )
        {
            if ( roleName.equals( role.getName() ) )
            {
                return role;
            }
        }
        return null;
    }

    private Collection<org.apache.archiva.redback.rbac.Role> filterApplicationRoles( ModelApplication application,
                                                                                     List<org.apache.archiva.redback.rbac.Role> allRoles,
                                                                                     List<ModelTemplate> applicationTemplates )
    {
        Set<org.apache.archiva.redback.rbac.Role> applicationRoles =
            new HashSet<org.apache.archiva.redback.rbac.Role>();
        List<ModelRole> roles = application.getRoles();

        for ( ModelRole modelRole : roles )
        {
            org.apache.archiva.redback.rbac.Role r = isInList( modelRole.getName(), allRoles );
            if ( r != null )
            {
                applicationRoles.add( r );
            }
        }

        List<String> roleNames = toRoleNames( allRoles );

        for ( ModelTemplate modelTemplate : applicationTemplates )
        {
            for ( org.apache.archiva.redback.rbac.Role r : allRoles )
            {
                if ( StringUtils.startsWith( r.getName(),
                                             modelTemplate.getNamePrefix() + modelTemplate.getDelimiter() ) )
                {
                    applicationRoles.add( r );
                }
            }
        }

        return applicationRoles;
    }

    private boolean roleFromTemplate( String roleName, List<ModelTemplate> applicationTemplates )
    {

        for ( ModelTemplate modelTemplate : applicationTemplates )
        {
            if ( StringUtils.startsWith( roleName, modelTemplate.getNamePrefix() + modelTemplate.getDelimiter() ) )
            {
                return true;
            }

        }
        return false;
    }

    private List<String> toRoleNames( Collection<org.apache.archiva.redback.rbac.Role> roles )
    {
        List<String> names = new ArrayList<String>( roles.size() );

        for ( org.apache.archiva.redback.rbac.Role r : roles )
        {
            names.add( r.getName() );
        }

        return names;
    }

    private List<RoleTemplate> toRoleTemplates( List<ModelTemplate> modelTemplates )
    {
        if ( modelTemplates == null || modelTemplates.isEmpty() )
        {
            return new ArrayList<RoleTemplate>( 0 );
        }

        List<RoleTemplate> roleTemplates = new ArrayList<RoleTemplate>( modelTemplates.size() );

        for ( ModelTemplate modelTemplate : modelTemplates )
        {
            RoleTemplate roleTemplate = new RoleTemplate();

            roleTemplate.setDelimiter( modelTemplate.getDelimiter() );
            roleTemplate.setDescription( modelTemplate.getDescription() );
            roleTemplate.setId( modelTemplate.getId() );
            roleTemplate.setNamePrefix( modelTemplate.getNamePrefix() );

            roleTemplates.add( roleTemplate );
        }

        return roleTemplates;
    }

    private Set<String> discoverResources( List<ModelTemplate> applicationTemplates,
                                           Collection<org.apache.archiva.redback.rbac.Role> roles )
    {
        Set<String> resources = new HashSet<String>();
        for ( ModelTemplate modelTemplate : applicationTemplates )
        {
            for ( org.apache.archiva.redback.rbac.Role role : roles )
            {
                String roleName = role.getName();
                if ( roleName.startsWith( modelTemplate.getNamePrefix() ) )
                {
                    String delimiter = modelTemplate.getDelimiter();
                    resources.add( roleName.substring( roleName.indexOf( delimiter ) + delimiter.length() ) );
                }
            }
        }
        return resources;
    }

    /**
     * this is a hack. this is a hack around the requirements of putting RBAC constraints into the model. this adds one
     * very major restriction to this security system, that a role name must contain the identifiers of the resource
     * that is being constrained for adding and granting of roles, this is unacceptable in the long term and we need to
     * get the model refactored to include this RBAC concept
     *
     * @param roleList
     * @return
     * @throws org.apache.archiva.redback.rbac.RbacManagerException
     *
     */
    protected List<org.apache.archiva.redback.rbac.Role> filterRolesForCurrentUserAccess(
        List<org.apache.archiva.redback.rbac.Role> roleList )
        throws RedbackServiceException
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();
        // olamy: should not happened normally as annotations check this first
        if ( redbackRequestInformation == null || redbackRequestInformation.getUser() == null )
        {
            throw new RedbackServiceException( new ErrorMessage( "login.mandatory" ) );
        }
        String currentUser = redbackRequestInformation.getUser().getUsername();

        List<org.apache.archiva.redback.rbac.Role> filteredRoleList =
            new ArrayList<org.apache.archiva.redback.rbac.Role>();
        try
        {
            Map<String, List<Permission>> assignedPermissionMap = rbacManager.getAssignedPermissionMap( currentUser );
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
            for ( org.apache.archiva.redback.rbac.Role role : roleList )
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
        }
        catch ( RbacManagerException rme )
        {
            // ignore, this can happen when the user has no roles assigned  
        }
        Collections.sort( filteredRoleList, new RoleSorter() );
        return filteredRoleList;
    }


}
