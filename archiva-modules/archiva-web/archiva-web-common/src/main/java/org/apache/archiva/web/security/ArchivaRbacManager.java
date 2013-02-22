package org.apache.archiva.web.security;
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.runtime.RedbackRuntimeConfigurationAdmin;
import org.apache.archiva.redback.components.cache.Cache;
import org.apache.archiva.redback.rbac.AbstractRBACManager;
import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.RbacObjectInvalidException;
import org.apache.archiva.redback.rbac.RbacObjectNotFoundException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.users.UserManager;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service( "rbacManager#archiva" )
public class ArchivaRbacManager
    extends AbstractRBACManager
    implements RBACManager
{

    private Map<String, RBACManager> rbacManagersPerId;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private RedbackRuntimeConfigurationAdmin redbackRuntimeConfigurationAdmin;

    @Inject
    @Named( value = "cache#operations" )
    private Cache<String, Operation> operationsCache;

    @Inject
    @Named( value = "cache#permissions" )
    private Cache<String, Permission> permissionsCache;

    @Inject
    @Named( value = "cache#resources" )
    private Cache<String, Resource> resourcesCache;

    @Inject
    @Named( value = "cache#roles" )
    private Cache<String, Role> rolesCache;

    @Inject
    @Named( value = "cache#userAssignments" )
    private Cache<String, UserAssignment> userAssignmentsCache;

    @Inject
    @Named( value = "cache#userPermissions" )
    private Cache<String, Map<String, List<Permission>>> userPermissionsCache;

    @Inject
    @Named( value = "cache#effectiveRoleSet" )
    private Cache<String, Set<Role>> effectiveRoleSetCache;

    @Override
    public void initialize()
    {
        try
        {
            List<String> rbacManagerIds =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration().getRbacManagerImpls();

            log.info( "use rbacManagerIds: '{}'", rbacManagerIds );

            this.rbacManagersPerId = new LinkedHashMap<String, RBACManager>( rbacManagerIds.size() );

            for ( String id : rbacManagerIds )
            {
                RBACManager rbacManager = applicationContext.getBean( "rbacManager#" + id, RBACManager.class );

                rbacManagersPerId.put( id, rbacManager );
            }
        }
        catch ( RepositoryAdminException e )
        {
            // revert to a default one ?
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    protected RBACManager getRbacManagerForWrite()
    {
        for ( RBACManager rbacManager : this.rbacManagersPerId.values() )
        {
            if ( !rbacManager.isReadOnly() )
            {
                return rbacManager;
            }
        }
        return this.rbacManagersPerId.values().iterator().next();
    }

    public Role createRole( String name )
    {
        return getRbacManagerForWrite().createRole( name );
    }

    public Role saveRole( Role role )
        throws RbacObjectInvalidException, RbacManagerException
    {
        Exception lastException = null;
        boolean allFailed = true;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                if ( !rbacManager.isReadOnly() )
                {
                    role = rbacManager.saveRole( role );
                    allFailed = false;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }
        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return role;
    }

    public void saveRoles( Collection<Role> roles )
        throws RbacObjectInvalidException, RbacManagerException
    {
        Exception lastException = null;
        boolean allFailed = true;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                if ( !rbacManager.isReadOnly() )
                {
                    rbacManager.saveRoles( roles );
                    allFailed = false;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }
        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
    }

    public Role getRole( String roleName )
        throws RbacObjectNotFoundException, RbacManagerException
    {

        Role el = rolesCache.get( roleName );
        if ( el != null )
        {
            return el;
        }

        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                Role role = rbacManager.getRole( roleName );
                if ( role != null )
                {
                    rolesCache.put( role.getName(), role );
                    return role;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }
        log.debug( "cannot find role for name: ‘{}", roleName );
        if ( lastException != null )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return null;
    }

    public List<Role> getAllRoles()
        throws RbacManagerException
    {
        Map<String, Role> allRoles = new HashMap<String, Role>();
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                List<Role> roles = rbacManager.getAllRoles();
                for ( Role role : roles )
                {
                    allRoles.put( role.getName(), role );
                }
                allFailed = false;
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }

        return new ArrayList<Role>( allRoles.values() );
    }

    public void removeRole( Role role )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                rbacManager.removeRole( role );
                rolesCache.remove( role.getName() );
                allFailed = false;
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
    }

    public Permission createPermission( String name )
        throws RbacManagerException
    {
        return getRbacManagerForWrite().createPermission( name );
    }

    public Permission createPermission( String name, String operationName, String resourceIdentifier )
        throws RbacManagerException
    {
        return getRbacManagerForWrite().createPermission( name, operationName, resourceIdentifier );
    }

    public Permission savePermission( Permission permission )
        throws RbacObjectInvalidException, RbacManagerException
    {
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                if ( rbacManager.isReadOnly() )
                {
                    permission = rbacManager.savePermission( permission );
                    allFailed = false;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }

        return permission;
    }

    public Permission getPermission( String permissionName )
        throws RbacObjectNotFoundException, RbacManagerException
    {

        Permission el = permissionsCache.get( permissionName );
        if ( el != null )
        {
            return el;
        }

        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                Permission p = rbacManager.getPermission( permissionName );
                if ( p != null )
                {
                    permissionsCache.put( permissionName, p );
                    return p;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return null;
    }

    public List<Permission> getAllPermissions()
        throws RbacManagerException
    {
        Map<String, Permission> allPermissions = new HashMap<String, Permission>();
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                List<Permission> permissions = rbacManager.getAllPermissions();
                for ( Permission p : permissions )
                {
                    allPermissions.put( p.getName(), p );
                }
                allFailed = false;
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return new ArrayList<Permission>( allPermissions.values() );
    }

    public void removePermission( Permission permission )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                rbacManager.removePermission( permission );
                permissionsCache.remove( permission.getName() );
                allFailed = false;
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
    }

    public Operation createOperation( String name )
        throws RbacManagerException
    {
        return getRbacManagerForWrite().createOperation( name );
    }

    public Operation saveOperation( Operation operation )
        throws RbacObjectInvalidException, RbacManagerException
    {
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                if ( !rbacManager.isReadOnly() )
                {
                    operation = rbacManager.saveOperation( operation );
                    allFailed = false;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return operation;
    }

    public Operation getOperation( String operationName )
        throws RbacObjectNotFoundException, RbacManagerException
    {

        Operation el = operationsCache.get( operationName );
        if ( el != null )
        {
            return el;
        }

        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                Operation o = rbacManager.getOperation( operationName );
                if ( o != null )
                {
                    operationsCache.put( operationName, o );
                    return o;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return null;
    }

    public List<Operation> getAllOperations()
        throws RbacManagerException
    {
        Map<String, Operation> allOperations = new HashMap<String, Operation>();
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                List<Operation> operations = rbacManager.getAllOperations();
                for ( Operation o : operations )
                {
                    allOperations.put( o.getName(), o );
                }
                allFailed = false;
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return new ArrayList<Operation>( allOperations.values() );
    }

    public void removeOperation( Operation operation )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                rbacManager.removeOperation( operation );
                operationsCache.remove( operation.getName() );
                allFailed = false;
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
    }

    public Resource createResource( String identifier )
        throws RbacManagerException
    {
        return getRbacManagerForWrite().createResource( identifier );
    }

    public Resource saveResource( Resource resource )
        throws RbacObjectInvalidException, RbacManagerException
    {
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                if ( !rbacManager.isReadOnly() )
                {
                    resource = rbacManager.saveResource( resource );
                    allFailed = false;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return resource;
    }

    public Resource getResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacManagerException
    {

        Resource el = resourcesCache.get( resourceIdentifier );
        if ( el != null )
        {
            return el;
        }

        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                Resource r = rbacManager.getResource( resourceIdentifier );
                if ( r != null )
                {
                    resourcesCache.put( resourceIdentifier, r );
                    return r;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return null;
    }

    public List<Resource> getAllResources()
        throws RbacManagerException
    {
        Map<String, Resource> allResources = new HashMap<String, Resource>();
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                List<Resource> resources = rbacManager.getAllResources();
                for ( Resource r : resources )
                {
                    allResources.put( r.getIdentifier(), r );
                }
                allFailed = false;
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return new ArrayList<Resource>( allResources.values() );
    }

    public void removeResource( Resource resource )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                rbacManager.removeResource( resource );
                resourcesCache.remove( resource.getIdentifier() );
                allFailed = false;
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
    }

    public UserAssignment createUserAssignment( String principal )
        throws RbacManagerException
    {
        return getRbacManagerForWrite().createUserAssignment( principal );
    }

    public UserAssignment saveUserAssignment( UserAssignment userAssignment )
        throws RbacObjectInvalidException, RbacManagerException
    {
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                if ( !rbacManager.isReadOnly() )
                {
                    userAssignment = rbacManager.saveUserAssignment( userAssignment );
                    allFailed = false;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return userAssignment;
    }

    public UserAssignment getUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        UserAssignment el = userAssignmentsCache.get( principal );
        if ( el != null )
        {
            return el;
        }

        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                UserAssignment ua = rbacManager.getUserAssignment( principal );
                if ( ua != null )
                {
                    userAssignmentsCache.put( principal, ua );
                    return ua;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return null;
    }

    @Override
    public boolean userAssignmentExists( String principal )
    {

        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                boolean exists = rbacManager.userAssignmentExists( principal );
                if ( exists )
                {
                    return true;
                }
            }
            catch ( Exception e )
            {
                // no op
            }
        }

        return false;
    }

    @Override
    public boolean userAssignmentExists( UserAssignment assignment )
    {
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                boolean exists = rbacManager.userAssignmentExists( assignment );
                if ( exists )
                {
                    return true;
                }
            }
            catch ( Exception e )
            {
                // no op
            }
        }

        return false;
    }

    public List<UserAssignment> getAllUserAssignments()
        throws RbacManagerException
    {
        Map<String, UserAssignment> allUserAssignments = new HashMap<String, UserAssignment>();
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                List<UserAssignment> userAssignments = rbacManager.getAllUserAssignments();
                for ( UserAssignment ua : userAssignments )
                {
                    allUserAssignments.put( ua.getPrincipal(), ua );
                }
                allFailed = false;
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return new ArrayList<UserAssignment>( allUserAssignments.values() );
    }

    public List<UserAssignment> getUserAssignmentsForRoles( Collection<String> roleNames )
        throws RbacManagerException
    {
        List<UserAssignment> allUserAssignments = new ArrayList<UserAssignment>();
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                List<UserAssignment> userAssignments = rbacManager.getUserAssignmentsForRoles( roleNames );

                allUserAssignments.addAll( userAssignments );

                allFailed = false;
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return allUserAssignments;
    }

    public void removeUserAssignment( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                rbacManager.removeUserAssignment( userAssignment );
                userAssignmentsCache.remove( userAssignment.getPrincipal() );
                allFailed = false;
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
    }

    @Override
    public boolean roleExists( String name )
        throws RbacManagerException
    {
        Role r = rolesCache.get( name );
        if ( r != null )
        {
            return true;
        }

        boolean allFailed = true;
        Exception lastException = null;
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            try
            {
                boolean exists = rbacManager.roleExists( name );
                if ( exists )
                {
                    return true;
                }
            }
            catch ( Exception e )
            {
                lastException = e;
            }
        }

        if ( lastException != null && allFailed )
        {
            throw new RbacManagerException( lastException.getMessage(), lastException );
        }
        return false;
    }

    @Override
    public boolean roleExists( Role role )
        throws RbacManagerException
    {
        return roleExists( role.getName() );
    }

    public void eraseDatabase()
    {
        log.warn( "eraseDatabase not implemented" );
    }

    @Override
    public boolean isFinalImplementation()
    {
        return false;
    }

    public String getDescriptionKey()
    {
        return "archiva.redback.rbacmanager.archiva";
    }

    public boolean isReadOnly()
    {
        return false;
    }
}
