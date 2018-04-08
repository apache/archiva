package org.apache.archiva.consumers.lucene.test;
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
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Olivier Lamy
 */
@Service("rbacManager#cached")
public class TestRBACManager implements RBACManager
{

    @Override
    public void initialize()
    {

    }

    @Override
    public boolean isFinalImplementation()
    {
        return false;
    }

    @Override
    public String getDescriptionKey()
    {
        return "archiva.redback.rbacmanager.test";
    }

    @Override
    public void addListener( RBACManagerListener listener )
    {

    }

    @Override
    public void removeListener( RBACManagerListener listener )
    {

    }

    @Override
    public Role createRole( String name )
    {
        return null;
    }

    @Override
    public boolean roleExists( String name )
    {
        return false;
    }

    @Override
    public boolean roleExists( Role role )
    {
        return false;
    }

    @Override
    public Role saveRole( Role role )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return null;
    }

    @Override
    public void saveRoles( Collection<Role> roles )
        throws RbacObjectInvalidException, RbacManagerException
    {

    }

    @Override
    public Role getRole( String roleName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;
    }

    @Override
    public Map<String, Role> getRoles( Collection<String> roleNames )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addChildRole( Role role, Role childRole )
        throws RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, Role> getChildRoles( Role role )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, Role> getParentRoles( Role role )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<Role> getAllRoles()
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Role> getEffectiveRoles( Role role )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeRole( Role role )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeRole( String roleName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Permission createPermission( String name )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Permission createPermission( String name, String operationName, String resourceIdentifier )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean permissionExists( String name )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean permissionExists( Permission permission )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Permission savePermission( Permission permission )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Permission getPermission( String permissionName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<Permission> getAllPermissions()
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removePermission( Permission permission )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removePermission( String permissionName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Operation createOperation( String name )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean operationExists( String name )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean operationExists( Operation operation )
    {
        return false;
    }

    @Override
    public Operation saveOperation( Operation operation )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return null;
    }

    @Override
    public Operation getOperation( String operationName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;
    }

    @Override
    public List<Operation> getAllOperations()
        throws RbacManagerException
    {
        return null;
    }

    @Override
    public void removeOperation( Operation operation )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
    }

    @Override
    public void removeOperation( String operationName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
    }

    @Override
    public Resource createResource( String identifier )
        throws RbacManagerException
    {
        return null;
    }

    @Override
    public boolean resourceExists( String identifier )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean resourceExists( Resource resource )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Resource saveResource( Resource resource )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Resource getResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<Resource> getAllResources()
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeResource( Resource resource )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserAssignment createUserAssignment( String principal )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean userAssignmentExists( String principal )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean userAssignmentExists( UserAssignment assignment )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserAssignment saveUserAssignment( UserAssignment userAssignment )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UserAssignment getUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserAssignment> getAllUserAssignments()
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<UserAssignment> getUserAssignmentsForRoles( Collection<String> roleNames )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserAssignment( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Role> getAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Role> getAssignedRoles( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Role> getEffectivelyUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Role> getEffectivelyAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Role> getUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Permission> getAssignedPermissions( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, List<? extends Permission>> getAssignedPermissionMap( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<Role> getAllAssignableRoles()
        throws RbacManagerException, RbacObjectNotFoundException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Resource getGlobalResource()
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void eraseDatabase()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isReadOnly()
    {
        return false;
    }
}
