package org.apache.archiva.redback.rbac;

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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RBACManager
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @todo expand on javadoc
 */
public interface RBACManager
{

    void addListener( RBACManagerListener listener );

    void removeListener( RBACManagerListener listener );


    // ------------------------------------------------------------------
    // Role Methods
    // ------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link Role}, or return an existing {@link Role}, depending
     * on the provided <code>name</code> parameter.
     * <p/>
     * Note: Be sure to use {@link #saveRole(Role)} in order to persist any changes to the Role.
     *
     * @param name the name.
     * @return the new {@link Role} object.
     */
    Role createRole( String name );

    /**
     * Tests for the existence of a Role.
     *
     * @return true if role exists in store.
     * @throws RbacManagerException
     */
    boolean roleExists( String name )
        throws RbacManagerException;

    boolean roleExists( Role role )
        throws RbacManagerException;

    Role saveRole( Role role )
        throws RbacObjectInvalidException, RbacManagerException;

    void saveRoles( Collection<Role> roles )
        throws RbacObjectInvalidException, RbacManagerException;

    /**
     * @param roleName
     * @return
     * @throws RbacObjectNotFoundException
     * @throws RbacManagerException
     */
    Role getRole( String roleName )
        throws RbacObjectNotFoundException, RbacManagerException;

    Map<String, Role> getRoles( Collection<String> roleNames )
        throws RbacObjectNotFoundException, RbacManagerException;

    void addChildRole( Role role, Role childRole )
        throws RbacObjectInvalidException, RbacManagerException;

    Map<String, Role> getChildRoles( Role role )
        throws RbacManagerException;

    Map<String, Role> getParentRoles( Role role )
        throws RbacManagerException;

    /**
     * Method getRoles
     */
    List<Role> getAllRoles()
        throws RbacManagerException;

    /**
     * Method getEffectiveRoles
     */
    Set<Role> getEffectiveRoles( Role role )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Method removeRole
     *
     * @param role
     */
    void removeRole( Role role )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    /**
     * Method removeRole
     *
     * @param roleName
     */
    void removeRole( String roleName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    // ------------------------------------------------------------------
    // Permission Methods
    // ------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link Permission}, or return an existing {@link Permission}, depending
     * on the provided <code>name</code> parameter.
     * <p/>
     * Note: Be sure to use {@link #savePermission(Permission)} in order to persist any changes to the Role.
     *
     * @param name the name.
     * @return the new Permission.
     * @throws RbacManagerException
     */
    Permission createPermission( String name )
        throws RbacManagerException;

    /**
     * Creates an implementation specific {@link Permission} with specified {@link Operation},
     * and {@link Resource} identifiers.
     * <p/>
     * Note: Be sure to use {@link #savePermission(Permission)} in order to persist any changes to the Role.
     *
     * @param name               the name.
     * @param operationName      the {@link Operation#setName(String)} value
     * @param resourceIdentifier the {@link Resource#setIdentifier(String)} value
     * @return the new Permission.
     * @throws RbacManagerException
     */
    Permission createPermission( String name, String operationName, String resourceIdentifier )
        throws RbacManagerException;

    /**
     * Tests for the existence of a permission.
     *
     * @param name the name to test for.
     * @return true if permission exists.
     * @throws RbacManagerException
     */
    boolean permissionExists( String name );

    boolean permissionExists( Permission permission );

    Permission savePermission( Permission permission )
        throws RbacObjectInvalidException, RbacManagerException;

    Permission getPermission( String permissionName )
        throws RbacObjectNotFoundException, RbacManagerException;

    List<Permission> getAllPermissions()
        throws RbacManagerException;

    void removePermission( Permission permission )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    void removePermission( String permissionName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    // ------------------------------------------------------------------
    // Operation Methods
    // ------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link Operation}, or return an existing {@link Operation}, depending
     * on the provided <code>name</code> parameter.
     * <p/>
     * Note: Be sure to use {@link #saveOperation(Operation)} in order to persist any changes to the Role.
     *
     * @param name the name.
     * @return the new Operation.
     * @throws RbacManagerException
     */
    Operation createOperation( String name )
        throws RbacManagerException;

    boolean operationExists( String name );

    boolean operationExists( Operation operation );

    /**
     * Save the new or existing operation to the store.
     *
     * @param operation the operation to save (new or existing)
     * @return the Operation that was saved.
     * @throws RbacObjectInvalidException
     * @throws RbacManagerException
     */
    Operation saveOperation( Operation operation )
        throws RbacObjectInvalidException, RbacManagerException;

    Operation getOperation( String operationName )
        throws RbacObjectNotFoundException, RbacManagerException;

    List<Operation> getAllOperations()
        throws RbacManagerException;

    void removeOperation( Operation operation )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    void removeOperation( String operationName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    // ------------------------------------------------------------------
    // Resource Methods
    // ------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link Resource}, or return an existing {@link Resource}, depending
     * on the provided <code>identifier</code> parameter.
     * <p/>
     * Note: Be sure to use {@link #saveResource(Resource)} in order to persist any changes to the Role.
     *
     * @param identifier the identifier.
     * @return the new Resource.
     * @throws RbacManagerException
     */
    Resource createResource( String identifier )
        throws RbacManagerException;

    boolean resourceExists( String identifier );

    boolean resourceExists( Resource resource );

    Resource saveResource( Resource resource )
        throws RbacObjectInvalidException, RbacManagerException;

    Resource getResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacManagerException;

    List<Resource> getAllResources()
        throws RbacManagerException;

    void removeResource( Resource resource )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    void removeResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    // ------------------------------------------------------------------
    // UserAssignment Methods
    // ------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link UserAssignment}, or return an existing {@link UserAssignment},
     * depending on the provided <code>identifier</code> parameter.
     * <p/>
     * Note: Be sure to use {@link #saveUserAssignment(UserAssignment)} in order to persist any changes to the Role.
     *
     * @param principal the principal reference to the user.
     * @return the new UserAssignment object.
     * @throws RbacManagerException
     */
    UserAssignment createUserAssignment( String principal )
        throws RbacManagerException;

    boolean userAssignmentExists( String principal );

    boolean userAssignmentExists( UserAssignment assignment );

    /**
     * Method saveUserAssignment
     *
     * @param userAssignment
     */
    UserAssignment saveUserAssignment( UserAssignment userAssignment )
        throws RbacObjectInvalidException, RbacManagerException;

    UserAssignment getUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Method getAssignments
     */
    List<UserAssignment> getAllUserAssignments()
        throws RbacManagerException;

    /**
     * Method getUserAssignmentsForRoless
     */
    List<UserAssignment> getUserAssignmentsForRoles( Collection<String> roleNames )
        throws RbacManagerException;

    /**
     * Method removeAssignment
     *
     * @param userAssignment
     */
    void removeUserAssignment( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    /**
     * Method removeAssignment
     *
     * @param principal
     */
    void removeUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    // ------------------------------------------------------------------
    // UserAssignment Utility Methods
    // ------------------------------------------------------------------

    /**
     * returns the active roles for a given principal
     * <p/>
     * NOTE: roles that are returned might have have roles themselves, if
     * you just want all permissions then use {@link #getAssignedPermissions(String principal)}
     *
     * @param principal
     * @return Collection of {@link Role} objects.
     * @throws RbacObjectNotFoundException
     * @throws RbacManagerException
     */
    Collection<Role> getAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Get the Collection of {@link Role} objects for this UserAssignment.
     *
     * @param userAssignment
     * @return Collection of {@link Role} objects for the provided UserAssignment.
     */
    Collection<Role> getAssignedRoles( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Get a list of all assignable roles that are currently not effectively assigned to the specific user,
     * meaning, not a child of any already granted role
     *
     * @param principal
     * @return
     * @throws RbacManagerException
     * @throws RbacObjectNotFoundException
     */
    Collection<Role> getEffectivelyUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException;

    /**
     * Get a list of the effectively assigned roles to the specified user, this includes child roles
     *
     * @param principal
     * @return
     * @throws RbacObjectNotFoundException
     * @throws RbacManagerException
     */
    Collection<Role> getEffectivelyAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Get a list of all assignable roles that are currently not assigned to the specific user.
     *
     * @param principal
     * @return
     * @throws RbacManagerException
     * @throws RbacObjectNotFoundException
     */
    Collection<Role> getUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException;

    /**
     * returns a set of all permissions that are in all active roles for a given
     * principal
     *
     * @param principal
     * @return
     * @throws RbacObjectNotFoundException
     * @throws RbacManagerException
     */
    Set<Permission> getAssignedPermissions( String principal )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * returns a map of assigned permissions keyed off of operation with a list value of Permissions
     *
     * @param principal
     * @return
     * @throws RbacObjectNotFoundException
     * @throws RbacManagerException
     */
    Map<String, List<Permission>> getAssignedPermissionMap( String principal )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * returns a list of all assignable roles
     *
     * @return
     * @throws RbacManagerException
     * @throws RbacObjectNotFoundException
     */
    List<Role> getAllAssignableRoles()
        throws RbacManagerException, RbacObjectNotFoundException;

    /**
     * returns the global resource object
     *
     * @return
     * @throws RbacManagerException
     */
    Resource getGlobalResource()
        throws RbacManagerException;

    void eraseDatabase();

    /**
     * consumer of user manager can use it to reload various configuration
     * with the configurable implementation is possible to change dynamically the real implementation used.
     *
     * @since 2.1
     */
    void initialize();

    /**
     * @return true if this implementation is a final one and not a wrapper (configurable, cached)
     * @since 2.1
     */
    boolean isFinalImplementation();

    /**
     * @return a key to be able to customize label in UI
     * @since 2.1
     */
    String getDescriptionKey();

    /**
     * Is the RBACManager read only?  if so then create and modify actions are to be disabled
     *
     * @return boolean true if user manager is read only
     */
    boolean isReadOnly();
}