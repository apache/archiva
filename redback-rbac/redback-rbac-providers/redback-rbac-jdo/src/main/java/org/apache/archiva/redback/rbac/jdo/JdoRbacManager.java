package org.apache.archiva.redback.rbac.jdo;

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

import org.apache.archiva.redback.rbac.AbstractRBACManager;
import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.rbac.RBACManagerListener;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.RbacObjectInvalidException;
import org.apache.archiva.redback.rbac.RbacObjectNotFoundException;
import org.apache.archiva.redback.rbac.RbacPermanentException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACObjectAssertions;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import java.util.Collection;
import java.util.List;

/**
 * JdoRbacManager:
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @author Jesse McConnell <jmcconnell@apache.org>
 *
 */
@Service( "rbacManager#jdo" )
public class JdoRbacManager
    extends AbstractRBACManager
    implements RBACManagerListener
{
    @Inject
    private JdoTool jdo;

    private boolean enableCache = true;

    // private static final String ROLE_DETAIL = "role-child-detail";
    private static final String ROLE_DETAIL = null;

    // ----------------------------------------------------------------------
    // Role methods
    // ----------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link Role}.
     * <p/>
     * Note: this method does not add the {@link Role} to the underlying store.
     * a call to {@link #saveRole(Role)} is required to track the role created with this
     * method call.
     *
     * @param name the name.
     * @return the new {@link Role} object with an empty (non-null) {@link Role#getChildRoleNames()} object.
     * @throws RbacManagerException
     */
    public Role createRole( String name )
    {
        Role role;

        try
        {
            role = getRole( name );
        }
        catch ( RbacManagerException e )
        {
            role = new JdoRole();
            role.setName( name );
        }

        return role;
    }

    /**
     * Method addRole
     *
     * @param role
     */
    public Role saveRole( Role role )
        throws RbacObjectInvalidException, RbacManagerException
    {
        RBACObjectAssertions.assertValid( role );

        return jdo.saveObject( role, new String[]{ ROLE_DETAIL } );
    }

    public boolean roleExists( Role role )
    {
        return jdo.objectExists( role );
    }

    public boolean roleExists( String name )
    {
        try
        {
            return jdo.objectExistsById( JdoRole.class, name );
        }
        catch ( RbacManagerException e )
        {
            return false;
        }
    }

    /**
     * @param roleName
     * @return
     * @throws RbacObjectNotFoundException
     * @throws RbacManagerException
     */
    public Role getRole( String roleName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return jdo.getObjectById( JdoRole.class, roleName, ROLE_DETAIL );
    }

    /**
     * Method getRoles
     */
    @SuppressWarnings( "unchecked" )
    public List<Role> getAllRoles()
        throws RbacManagerException
    {
        return (List<Role>) jdo.getAllObjects( JdoRole.class );
    }

    public void removeRole( Role role )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        RBACObjectAssertions.assertValid( role );

        if ( role.isPermanent() )
        {
            throw new RbacPermanentException( "Unable to delete permanent role [" + role.getName() + "]" );
        }

        jdo.removeObject( role );
    }

    public void saveRoles( Collection<Role> roles )
        throws RbacObjectInvalidException, RbacManagerException
    {
        if ( roles == null )
        {
            // Nothing to do.
            return;
        }

        // This is done in JdoRbacManager as opposed to JdoTool as we need to assertValid() on each role and
        // also wrap the entire collection into a single atomic save/makePersistent.

        PersistenceManager pm = jdo.getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try
        {
            tx.begin();

            for ( Role role : roles )
            {
                if ( ( JDOHelper.getObjectId( role ) != null ) && !JDOHelper.isDetached( role ) )
                {
                    // This is a fatal error that means we need to fix our code.
                    // Leave it as a JDOUserException, it's intentional.
                    throw new RbacManagerException( "Existing Role is not detached: " + role );
                }

                RBACObjectAssertions.assertValid( role );

                pm.makePersistent( role );
            }

            tx.commit();
        }
        finally
        {
            jdo.rollbackIfActive( tx );
        }
    }

    // ----------------------------------------------------------------------
    // Permission methods
    // ----------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link Permission}.
     * <p/>
     * Note: this method does not add the {@link Permission} to the underlying store.
     * a call to {@link #savePermission(Permission)} is required to track the permission created
     * with this method call.
     *
     * @param name the name.
     * @return the new Permission.
     * @throws RbacManagerException
     */
    public Permission createPermission( String name )
        throws RbacManagerException
    {
        Permission permission;

        try
        {
            permission = getPermission( name );
            log.debug( "Create Permission [{}] Returning Existing.", name );
        }
        catch ( RbacObjectNotFoundException e )
        {
            permission = new JdoPermission();
            permission.setName( name );
            log.debug( "Create Permission [{}] New JdoPermission.", name );
        }

        return permission;
    }

    /**
     * Creates an implementation specific {@link Permission} with specified {@link Operation},
     * and {@link Resource} identifiers.
     * <p/>
     * Note: this method does not add the Permission, Operation, or Resource to the underlying store.
     * a call to {@link #savePermission(Permission)} is required to track the permission, operation,
     * or resource created with this method call.
     *
     * @param name               the name.
     * @param operationName      the {@link Operation#setName(String)} value
     * @param resourceIdentifier the {@link Resource#setIdentifier(String)} value
     * @return the new Permission.
     * @throws RbacManagerException
     */
    public Permission createPermission( String name, String operationName, String resourceIdentifier )
        throws RbacManagerException
    {
        Permission permission = new JdoPermission();
        permission.setName( name );

        Operation operation;
        try
        {
            operation = getOperation( operationName );
        }
        catch ( RbacObjectNotFoundException e )
        {
            operation = new JdoOperation();
            operation.setName( operationName );
        }
        permission.setOperation( operation );

        Resource resource;
        try
        {
            resource = getResource( resourceIdentifier );
        }
        catch ( RbacObjectNotFoundException e )
        {
            resource = new JdoResource();
            resource.setIdentifier( resourceIdentifier );
        }
        permission.setResource( resource );

        return permission;
    }

    public Permission savePermission( Permission permission )
        throws RbacObjectInvalidException, RbacManagerException
    {
        RBACObjectAssertions.assertValid( permission );

        return jdo.saveObject( permission, null );
    }

    public boolean permissionExists( Permission permission )
    {
        return jdo.objectExists( permission );
    }

    public boolean permissionExists( String name )
    {
        try
        {
            return jdo.objectExistsById( JdoPermission.class, name );
        }
        catch ( RbacManagerException e )
        {
            return false;
        }
    }

    public Permission getPermission( String permissionName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return jdo.getObjectById( JdoPermission.class, permissionName, null );
    }

    @SuppressWarnings( "unchecked" )
    public List<Permission> getAllPermissions()
        throws RbacManagerException
    {
        return (List<Permission>) jdo.getAllObjects( JdoPermission.class );
    }

    public void removePermission( Permission permission )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        RBACObjectAssertions.assertValid( permission );

        if ( permission.isPermanent() )
        {
            throw new RbacPermanentException( "Unable to delete permanent permission [" + permission.getName() + "]" );
        }

        jdo.removeObject( permission );
    }

    // ----------------------------------------------------------------------
    // Operation methods
    // ----------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link Operation}.
     * <p/>
     * Note: this method does not add the {@link Operation} to the underlying store.
     * a call to {@link #saveOperation(Operation)} is required to track the operation created
     * with this method call.
     *
     * @param name the name.
     * @return the new Operation.
     * @throws RbacManagerException
     */
    public Operation createOperation( String name )
        throws RbacManagerException
    {
        Operation operation;

        try
        {
            operation = getOperation( name );
        }
        catch ( RbacObjectNotFoundException e )
        {
            operation = new JdoOperation();
            operation.setName( name );
        }

        return operation;
    }

    public Operation saveOperation( Operation operation )
        throws RbacObjectInvalidException, RbacManagerException
    {
        RBACObjectAssertions.assertValid( operation );
        return jdo.saveObject( operation, null );
    }

    public boolean operationExists( Operation operation )
    {
        return jdo.objectExists( operation );
    }

    public boolean operationExists( String name )
    {
        try
        {
            return jdo.objectExistsById( JdoOperation.class, name );
        }
        catch ( RbacManagerException e )
        {
            return false;
        }
    }

    public Operation getOperation( String operationName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return jdo.getObjectById( JdoOperation.class, operationName, null );
    }

    @SuppressWarnings( "unchecked" )
    public List<Operation> getAllOperations()
        throws RbacManagerException
    {
        return (List<Operation>) jdo.getAllObjects( JdoOperation.class );
    }

    public void removeOperation( Operation operation )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        RBACObjectAssertions.assertValid( operation );

        if ( operation.isPermanent() )
        {
            throw new RbacPermanentException( "Unable to delete permanent operation [" + operation.getName() + "]" );
        }

        jdo.removeObject( operation );
    }

    // ----------------------------------------------------------------------
    // Resource methods
    // ----------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link Resource}.
     * <p/>
     * Note: this method does not add the {@link Resource} to the underlying store.
     * a call to {@link #saveResource(Resource)} is required to track the resource created
     * with this method call.
     *
     * @param identifier the identifier.
     * @return the new Resource.
     * @throws RbacManagerException
     */
    public Resource createResource( String identifier )
        throws RbacManagerException
    {
        Resource resource;

        try
        {
            resource = getResource( identifier );
            log.debug( "Create Resource [ {} ] Returning Existing.", identifier );
        }
        catch ( RbacObjectNotFoundException e )
        {
            resource = new JdoResource();
            resource.setIdentifier( identifier );
            log.debug( "Create Resource [ {} ] New JdoResource.", identifier );
        }

        return resource;
    }

    public Resource saveResource( Resource resource )
        throws RbacObjectInvalidException, RbacManagerException
    {
        RBACObjectAssertions.assertValid( resource );
        return jdo.saveObject( resource, null );
    }

    public boolean resourceExists( Resource resource )
    {
        return jdo.objectExists( resource );
    }

    public boolean resourceExists( String identifier )
    {
        try
        {
            return jdo.objectExistsById( JdoResource.class, identifier );
        }
        catch ( RbacManagerException e )
        {
            return false;
        }
    }

    public Resource getResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return jdo.getObjectById( JdoResource.class, resourceIdentifier, null );
    }

    @SuppressWarnings( "unchecked" )
    public List<Resource> getAllResources()
        throws RbacManagerException
    {
        return (List<Resource>) jdo.getAllObjects( JdoResource.class );
    }

    public void removeResource( Resource resource )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        RBACObjectAssertions.assertValid( resource );

        if ( resource.isPermanent() )
        {
            throw new RbacPermanentException(
                "Unable to delete permanent resource [" + resource.getIdentifier() + "]" );
        }

        jdo.removeObject( resource );
    }

    // ----------------------------------------------------------------------
    // User Assignment methods
    // ----------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link UserAssignment}.
     * <p/>
     * Note: this method does not add the {@link UserAssignment} to the underlying store.
     * a call to {@link #saveUserAssignment(UserAssignment)} is required to track the user
     * assignment created with this method call.
     *
     * @param principal the principal reference to the user.
     * @return the new UserAssignment with an empty (non-null) {@link UserAssignment#getRoleNames()} object.
     * @throws RbacManagerException
     */
    public UserAssignment createUserAssignment( String principal )
    {
        UserAssignment ua;

        try
        {
            ua = getUserAssignment( principal );
        }
        catch ( RbacManagerException e )
        {
            ua = new JdoUserAssignment();
            ua.setPrincipal( principal );
        }

        return ua;
    }

    /**
     * Method addUserAssignment
     *
     * @param userAssignment
     */
    public UserAssignment saveUserAssignment( UserAssignment userAssignment )
        throws RbacObjectInvalidException, RbacManagerException
    {
        RBACObjectAssertions.assertValid( "Save User Assignment", userAssignment );

        fireRbacUserAssignmentSaved( userAssignment );

        return jdo.saveObject( userAssignment, new String[]{ ROLE_DETAIL } );
    }

    public boolean userAssignmentExists( String principal )
    {
        try
        {
            return jdo.objectExistsById( JdoUserAssignment.class, principal );
        }
        catch ( RbacManagerException e )
        {
            return false;
        }
    }

    public boolean userAssignmentExists( UserAssignment assignment )
    {
        return jdo.objectExists( assignment );
    }

    public UserAssignment getUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return jdo.getObjectById( JdoUserAssignment.class, principal, ROLE_DETAIL );
    }

    /**
     * Method getAssignments
     */
    @SuppressWarnings( "unchecked" )
    public List<UserAssignment> getAllUserAssignments()
        throws RbacManagerException
    {
        return (List<UserAssignment>) jdo.getAllObjects( JdoUserAssignment.class );
    }

    /**
     * Method getUserAssignmentsForRoles
     */
    @SuppressWarnings( "unchecked" )
    public List<UserAssignment> getUserAssignmentsForRoles( Collection<String> roleNames )
        throws RbacManagerException
    {
        return (List<UserAssignment>) jdo.getUserAssignmentsForRoles( JdoUserAssignment.class, null, roleNames );
    }

    /**
     * Method removeAssignment
     *
     * @param userAssignment
     */
    public void removeUserAssignment( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        RBACObjectAssertions.assertValid( userAssignment );

        if ( userAssignment.isPermanent() )
        {
            throw new RbacPermanentException(
                "Unable to delete permanent user assignment [" + userAssignment.getPrincipal() + "]" );
        }

        fireRbacUserAssignmentRemoved( userAssignment );

        jdo.removeObject( userAssignment );
    }

    public void eraseDatabase()
    {
        // Must delete in order so that FK constraints don't get violated
        jdo.removeAll( JdoRole.class );
        jdo.removeAll( JdoPermission.class );
        jdo.removeAll( JdoOperation.class );
        jdo.removeAll( JdoResource.class );
        jdo.removeAll( JdoUserAssignment.class );
        jdo.removeAll( RbacJdoModelModelloMetadata.class );
    }

    @PostConstruct
    public void initialize()
    {
        super.initialize();

        jdo.setListener( this );
        if ( enableCache )
        {
            jdo.enableCache( JdoRole.class );
            jdo.enableCache( JdoOperation.class );
            jdo.enableCache( JdoResource.class );
            jdo.enableCache( JdoUserAssignment.class );
            jdo.enableCache( JdoPermission.class );
        }
    }

    public void rbacInit( boolean freshdb )
    {
        fireRbacInit( freshdb );
    }

    public void rbacPermissionRemoved( Permission permission )
    {
        fireRbacPermissionRemoved( permission );
    }

    public void rbacPermissionSaved( Permission permission )
    {
        fireRbacPermissionSaved( permission );
    }

    public void rbacRoleRemoved( Role role )
    {
        fireRbacRoleRemoved( role );
    }

    public void rbacRoleSaved( Role role )
    {
        fireRbacRoleSaved( role );
    }


    public void rbacUserAssignmentSaved( UserAssignment userAssignment )
    {
        fireRbacUserAssignmentSaved( userAssignment );
    }

    public void rbacUserAssignmentRemoved( UserAssignment userAssignment )
    {
        fireRbacUserAssignmentRemoved( userAssignment );
    }

    public JdoTool getJdo()
    {
        return jdo;
    }

    public void setJdo( JdoTool jdo )
    {
        this.jdo = jdo;
    }

    public boolean isEnableCache()
    {
        return enableCache;
    }

    public void setEnableCache( boolean enableCache )
    {
        this.enableCache = enableCache;
    }

    @Override
    public boolean isFinalImplementation()
    {
        return true;
    }

    public String getDescriptionKey()
    {
        return "archiva.redback.rbacmanager.jdo";
    }
}
