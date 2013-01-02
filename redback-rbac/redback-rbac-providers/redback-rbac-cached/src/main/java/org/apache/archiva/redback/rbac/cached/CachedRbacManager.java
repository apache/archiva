package org.apache.archiva.redback.rbac.cached;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.components.cache.Cache;
import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RBACManagerListener;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.RbacObjectInvalidException;
import org.apache.archiva.redback.rbac.RbacObjectNotFoundException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CachedRbacManager is a wrapped RBACManager with caching.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
@Service( "rbacManager#cached" )
public class CachedRbacManager
    implements RBACManager, RBACManagerListener
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "rbacManager#jdo" )
    private RBACManager rbacImpl;

    @Inject
    @Named( value = "cache#operations" )
    private Cache operationsCache;

    @Inject
    @Named( value = "cache#permissions" )
    private Cache permissionsCache;

    @Inject
    @Named( value = "cache#resources" )
    private Cache resourcesCache;

    @Inject
    @Named( value = "cache#roles" )
    private Cache rolesCache;

    @Inject
    @Named( value = "cache#userAssignments" )
    private Cache userAssignmentsCache;

    @Inject
    @Named( value = "cache#userPermissions" )
    private Cache userPermissionsCache;

    @Inject
    @Named( value = "cache#effectiveRoleSet" )
    private Cache effectiveRoleSetCache;

    public void addChildRole( Role role, Role childRole )
        throws RbacObjectInvalidException, RbacManagerException
    {
        try
        {
            this.rbacImpl.addChildRole( role, childRole );
        }
        finally
        {
            invalidateCachedRole( role );
            invalidateCachedRole( childRole );
        }
    }

    public void addListener( RBACManagerListener listener )
    {
        this.rbacImpl.addListener( listener );
    }

    public Operation createOperation( String name )
        throws RbacManagerException
    {
        operationsCache.remove( name );
        return this.rbacImpl.createOperation( name );
    }

    public Permission createPermission( String name )
        throws RbacManagerException
    {
        permissionsCache.remove( name );
        return this.rbacImpl.createPermission( name );
    }

    public Permission createPermission( String name, String operationName, String resourceIdentifier )
        throws RbacManagerException
    {
        permissionsCache.remove( name );
        return this.rbacImpl.createPermission( name, operationName, resourceIdentifier );
    }

    public Resource createResource( String identifier )
        throws RbacManagerException
    {
        resourcesCache.remove( identifier );
        return this.rbacImpl.createResource( identifier );
    }

    public Role createRole( String name )
    {
        rolesCache.remove( name );
        return this.rbacImpl.createRole( name );
    }

    public UserAssignment createUserAssignment( String principal )
        throws RbacManagerException
    {
        invalidateCachedUserAssignment( principal );
        return this.rbacImpl.createUserAssignment( principal );
    }

    public void eraseDatabase()
    {
        try
        {
            this.rbacImpl.eraseDatabase();
        }
        finally
        {
            // FIXME cleanup
            //EhcacheUtils.clearAllCaches( log() );
        }
    }

    /**
     * @see org.apache.archiva.redback.rbac.RBACManager#getAllAssignableRoles()
     */
    public List<Role> getAllAssignableRoles()
        throws RbacManagerException, RbacObjectNotFoundException
    {
        log.debug( "NOT CACHED - .getAllAssignableRoles()" );
        return this.rbacImpl.getAllAssignableRoles();
    }

    public List<Operation> getAllOperations()
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getAllOperations()" );
        return this.rbacImpl.getAllOperations();
    }

    public List<Permission> getAllPermissions()
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getAllPermissions()" );
        return this.rbacImpl.getAllPermissions();
    }

    public List<Resource> getAllResources()
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getAllResources()" );
        return this.rbacImpl.getAllResources();
    }

    public List<Role> getAllRoles()
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getAllRoles()" );
        return this.rbacImpl.getAllRoles();
    }

    public List<UserAssignment> getAllUserAssignments()
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getAllUserAssignments()" );
        return this.rbacImpl.getAllUserAssignments();
    }

    /**
     * @see org.apache.archiva.redback.rbac.RBACManager#getAssignedPermissionMap(java.lang.String)
     */
    @SuppressWarnings( "unchecked" )
    public Map<String, List<Permission>> getAssignedPermissionMap( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Map<String, List<Permission>> el = (Map<String, List<Permission>>) userPermissionsCache.get( principal );

        if ( el != null )
        {
            log.debug( "using cached user permission map" );
            return el;
        }
        synchronized ( userPermissionsCache )
        {
            log.debug( "building user permission map" );
            Map<String, List<Permission>> userPermMap = this.rbacImpl.getAssignedPermissionMap( principal );
            userPermissionsCache.put( principal, userPermMap );
            return userPermMap;
        }

    }

    public Set<Permission> getAssignedPermissions( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        log.debug( "NOT CACHED - .getAssignedPermissions(String)" );
        return this.rbacImpl.getAssignedPermissions( principal );
    }

    public Collection<Role> getAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        log.debug( "NOT CACHED - .getAssignedRoles(String)" );
        return this.rbacImpl.getAssignedRoles( principal );
    }

    public Collection<Role> getAssignedRoles( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        log.debug( "NOT CACHED - .getAssignedRoles(UserAssignment)" );
        return this.rbacImpl.getAssignedRoles( userAssignment );
    }

    public Map<String, Role> getChildRoles( Role role )
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getChildRoles(Role)" );
        return this.rbacImpl.getChildRoles( role );
    }

    public Map<String, Role> getParentRoles( Role role )
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getParentRoles(Role)" );
        return this.rbacImpl.getParentRoles( role );
    }

    public Collection<Role> getEffectivelyAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        log.debug( "NOT CACHED - .getEffectivelyAssignedRoles(String)" );
        return this.rbacImpl.getEffectivelyAssignedRoles( principal );
    }

    public Collection<Role> getEffectivelyUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException
    {
        log.debug( "NOT CACHED - .getEffectivelyUnassignedRoles(String)" );
        return this.rbacImpl.getEffectivelyUnassignedRoles( principal );
    }

    @SuppressWarnings( "unchecked" )
    public Set<Role> getEffectiveRoles( Role role )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Set<Role> el = (Set<Role>) effectiveRoleSetCache.get( role.getName() );

        if ( el != null )
        {
            log.debug( "using cached effective role set" );
            return el;
        }
        else
        {
            log.debug( "building effective role set" );
            Set<Role> effectiveRoleSet = this.rbacImpl.getEffectiveRoles( role );
            effectiveRoleSetCache.put( role.getName(), effectiveRoleSet );
            return effectiveRoleSet;
        }
    }

    public Resource getGlobalResource()
        throws RbacManagerException
    {
        /* this is very light */
        log.debug( "NOT CACHED - .getGlobalResource()" );
        return this.rbacImpl.getGlobalResource();
    }

    public Operation getOperation( String operationName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Object el = operationsCache.get( operationName );
        if ( el != null )
        {
            return (Operation) el;
        }
        else
        {
            Operation operation = this.rbacImpl.getOperation( operationName );
            operationsCache.put( operationName, operation );
            return operation;
        }
    }

    public Permission getPermission( String permissionName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Object el = permissionsCache.get( permissionName );
        if ( el != null )
        {
            return (Permission) el;
        }
        else
        {
            Permission permission = this.rbacImpl.getPermission( permissionName );
            permissionsCache.put( permissionName, permission );
            return permission;
        }
    }

    public Resource getResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Object el = resourcesCache.get( resourceIdentifier );
        if ( el != null )
        {
            return (Resource) el;
        }
        else
        {
            Resource resource = this.rbacImpl.getResource( resourceIdentifier );
            resourcesCache.put( resourceIdentifier, resource );
            return resource;
        }
    }

    public Role getRole( String roleName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Object el = rolesCache.get( roleName );
        if ( el != null )
        {
            return (Role) el;
        }
        else
        {
            Role role = this.rbacImpl.getRole( roleName );
            rolesCache.put( roleName, role );
            return role;
        }
    }

    public Map<String, Role> getRoles( Collection<String> roleNames )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        log.debug( "NOT CACHED - .getRoles(Collection)" );
        return this.rbacImpl.getRoles( roleNames );
    }

    public Collection<Role> getUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException
    {
        log.debug( "NOT CACHED - .getUnassignedRoles(String)" );
        return this.rbacImpl.getUnassignedRoles( principal );
    }

    public UserAssignment getUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Object el = userAssignmentsCache.get( principal );
        if ( el != null )
        {
            return (UserAssignment) el;
        }
        else
        {
            UserAssignment userAssignment = this.rbacImpl.getUserAssignment( principal );
            userAssignmentsCache.put( principal, userAssignment );
            return userAssignment;
        }
    }

    public List<UserAssignment> getUserAssignmentsForRoles( Collection<String> roleNames )
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getUserAssignmentsForRoles(Collection)" );
        return this.rbacImpl.getUserAssignmentsForRoles( roleNames );
    }

    public boolean operationExists( Operation operation )
    {
        if ( operation == null )
        {
            return false;
        }

        if ( operationsCache.hasKey( operation.getName() ) )
        {
            return true;
        }

        return this.rbacImpl.operationExists( operation );
    }

    public boolean operationExists( String name )
    {
        if ( operationsCache.hasKey( name ) )
        {
            return true;
        }

        return this.rbacImpl.operationExists( name );
    }

    public boolean permissionExists( Permission permission )
    {
        if ( permission == null )
        {
            return false;
        }

        if ( permissionsCache.hasKey( permission.getName() ) )
        {
            return true;
        }

        return this.rbacImpl.permissionExists( permission );
    }

    public boolean permissionExists( String name )
    {
        if ( permissionsCache.hasKey( name ) )
        {
            return true;
        }

        return this.rbacImpl.permissionExists( name );
    }

    public void rbacInit( boolean freshdb )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacInit( freshdb );
        }
        // lookup all Cache and clear all ?
        this.resourcesCache.clear();
        this.operationsCache.clear();
        this.permissionsCache.clear();
        this.rolesCache.clear();
        this.userAssignmentsCache.clear();
        this.userPermissionsCache.clear();
    }

    public void rbacPermissionRemoved( Permission permission )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacPermissionRemoved( permission );
        }

        invalidateCachedPermission( permission );
    }

    public void rbacPermissionSaved( Permission permission )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacPermissionSaved( permission );
        }

        invalidateCachedPermission( permission );
    }

    public void rbacRoleRemoved( Role role )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacRoleRemoved( role );
        }

        invalidateCachedRole( role );
    }

    public void rbacRoleSaved( Role role )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacRoleSaved( role );
        }

        invalidateCachedRole( role );
    }

    public void rbacUserAssignmentRemoved( UserAssignment userAssignment )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacUserAssignmentRemoved( userAssignment );
        }

        invalidateCachedUserAssignment( userAssignment );
    }

    public void rbacUserAssignmentSaved( UserAssignment userAssignment )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacUserAssignmentSaved( userAssignment );
        }

        invalidateCachedUserAssignment( userAssignment );
    }

    public void removeListener( RBACManagerListener listener )
    {
        this.rbacImpl.removeListener( listener );
    }

    public void removeOperation( Operation operation )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedOperation( operation );
        this.rbacImpl.removeOperation( operation );
    }

    public void removeOperation( String operationName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        operationsCache.remove( operationName );
        this.rbacImpl.removeOperation( operationName );
    }

    public void removePermission( Permission permission )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedPermission( permission );
        this.rbacImpl.removePermission( permission );
    }

    public void removePermission( String permissionName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        permissionsCache.remove( permissionName );
        this.rbacImpl.removePermission( permissionName );
    }

    public void removeResource( Resource resource )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedResource( resource );
        this.rbacImpl.removeResource( resource );
    }

    public void removeResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        resourcesCache.remove( resourceIdentifier );
        this.rbacImpl.removeResource( resourceIdentifier );
    }

    public void removeRole( Role role )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedRole( role );
        this.rbacImpl.removeRole( role );
    }

    public void removeRole( String roleName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        rolesCache.remove( roleName );
        this.rbacImpl.removeRole( roleName );
    }

    public void removeUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedUserAssignment( principal );
        this.rbacImpl.removeUserAssignment( principal );
    }

    public void removeUserAssignment( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedUserAssignment( userAssignment );
        this.rbacImpl.removeUserAssignment( userAssignment );
    }

    public boolean resourceExists( Resource resource )
    {
        if ( resourcesCache.hasKey( resource.getIdentifier() ) )
        {
            return true;
        }

        return this.rbacImpl.resourceExists( resource );
    }

    public boolean resourceExists( String identifier )
    {
        if ( resourcesCache.hasKey( identifier ) )
        {
            return true;
        }

        return this.rbacImpl.resourceExists( identifier );
    }

    public boolean roleExists( Role role )
    {
        if ( rolesCache.hasKey( role.getName() ) )
        {
            return true;
        }

        return this.rbacImpl.roleExists( role );
    }

    public boolean roleExists( String name )
    {
        if ( rolesCache.hasKey( name ) )
        {
            return true;
        }

        return this.rbacImpl.roleExists( name );
    }

    public Operation saveOperation( Operation operation )
        throws RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedOperation( operation );
        return this.rbacImpl.saveOperation( operation );
    }

    public Permission savePermission( Permission permission )
        throws RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedPermission( permission );
        return this.rbacImpl.savePermission( permission );
    }

    public Resource saveResource( Resource resource )
        throws RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedResource( resource );
        return this.rbacImpl.saveResource( resource );
    }

    public synchronized Role saveRole( Role role )
        throws RbacObjectInvalidException, RbacManagerException
    {
        /*
        List assignments = this.rbacImpl.getUserAssignmentsForRoles( Collections.singletonList( role.getName() ) );

        for ( Iterator i = assignments.iterator(); i.hasNext();  )
        {
            log.debug( "invalidating user assignment with role " + role.getName() );
            invalidateCachedUserAssignment( (UserAssignment)i.next() );
        }
        */

        /*
        the above commented out section would try and invalidate just that user caches that are effected by
        changes in the users permissions map due to role changes.

        however the implementations of those do not take into account child role hierarchies so wipe all
        user caches on role saving...which is a heavy handed way to solve the problem, but not going to
        happen frequently for current applications so not a huge deal.
         */
        invalidateAllCachedUserAssignments();
        invalidateCachedRole( role );
        return this.rbacImpl.saveRole( role );
    }

    public synchronized void saveRoles( Collection<Role> roles )
        throws RbacObjectInvalidException, RbacManagerException
    {

        for ( Role role : roles )
        {
            invalidateCachedRole( role );
        }

        /*
        List assignments = this.rbacImpl.getUserAssignmentsForRoles( roles );

        for ( Iterator i = assignments.iterator(); i.hasNext();  )
        {
            log.debug( "invalidating user assignment with roles" );
            invalidateCachedUserAssignment( (UserAssignment)i.next() );
        }
        */
        invalidateAllCachedUserAssignments();
        this.rbacImpl.saveRoles( roles );
    }

    public UserAssignment saveUserAssignment( UserAssignment userAssignment )
        throws RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedUserAssignment( userAssignment );
        return this.rbacImpl.saveUserAssignment( userAssignment );
    }

    public boolean userAssignmentExists( String principal )
    {
        if ( userAssignmentsCache.hasKey( principal ) )
        {
            return true;
        }

        return this.rbacImpl.userAssignmentExists( principal );
    }

    public boolean userAssignmentExists( UserAssignment assignment )
    {
        if ( userAssignmentsCache.hasKey( assignment.getPrincipal() ) )
        {
            return true;
        }

        return this.rbacImpl.userAssignmentExists( assignment );
    }

    private void invalidateCachedRole( Role role )
    {
        if ( role != null )
        {
            rolesCache.remove( role.getName() );
            // if a role changes we need to invalidate the entire effective role set cache
            // since we have no concept of the heirarchy involved in the role sets
            effectiveRoleSetCache.clear();
        }

    }

    private void invalidateCachedOperation( Operation operation )
    {
        if ( operation != null )
        {
            operationsCache.remove( operation.getName() );
        }
    }

    private void invalidateCachedPermission( Permission permission )
    {
        if ( permission != null )
        {
            permissionsCache.remove( permission.getName() );
        }
    }

    private void invalidateCachedResource( Resource resource )
    {
        if ( resource != null )
        {
            resourcesCache.remove( resource.getIdentifier() );
        }
    }

    private void invalidateCachedUserAssignment( UserAssignment userAssignment )
    {
        if ( userAssignment != null )
        {
            userAssignmentsCache.remove( userAssignment.getPrincipal() );
            userPermissionsCache.remove( userAssignment.getPrincipal() );
        }
    }

    private void invalidateCachedUserAssignment( String principal )
    {
        userAssignmentsCache.remove( principal );
        userPermissionsCache.remove( principal );
    }

    private void invalidateAllCachedUserAssignments()
    {
        userAssignmentsCache.clear();
        userPermissionsCache.clear();
    }

    public Cache getOperationsCache()
    {
        return operationsCache;
    }

    public void setOperationsCache( Cache operationsCache )
    {
        this.operationsCache = operationsCache;
    }

    public Cache getPermissionsCache()
    {
        return permissionsCache;
    }

    public void setPermissionsCache( Cache permissionsCache )
    {
        this.permissionsCache = permissionsCache;
    }

    public Cache getResourcesCache()
    {
        return resourcesCache;
    }

    public void setResourcesCache( Cache resourcesCache )
    {
        this.resourcesCache = resourcesCache;
    }

    public Cache getRolesCache()
    {
        return rolesCache;
    }

    public void setRolesCache( Cache rolesCache )
    {
        this.rolesCache = rolesCache;
    }

    public Cache getUserAssignmentsCache()
    {
        return userAssignmentsCache;
    }

    public void setUserAssignmentsCache( Cache userAssignmentsCache )
    {
        this.userAssignmentsCache = userAssignmentsCache;
    }

    public Cache getUserPermissionsCache()
    {
        return userPermissionsCache;
    }

    public void setUserPermissionsCache( Cache userPermissionsCache )
    {
        this.userPermissionsCache = userPermissionsCache;
    }

    public Cache getEffectiveRoleSetCache()
    {
        return effectiveRoleSetCache;
    }

    public void setEffectiveRoleSetCache( Cache effectiveRoleSetCache )
    {
        this.effectiveRoleSetCache = effectiveRoleSetCache;
    }

    public RBACManager getRbacImpl()
    {
        return rbacImpl;
    }

    public void setRbacImpl( RBACManager rbacImpl )
    {
        this.rbacImpl = rbacImpl;
    }
}
