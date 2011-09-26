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

import org.codehaus.plexus.redback.rbac.Operation;
import org.codehaus.plexus.redback.rbac.Permission;
import org.codehaus.plexus.redback.rbac.RBACManager;
import org.codehaus.plexus.redback.rbac.RBACManagerListener;
import org.codehaus.plexus.redback.rbac.RbacManagerException;
import org.codehaus.plexus.redback.rbac.RbacObjectInvalidException;
import org.codehaus.plexus.redback.rbac.RbacObjectNotFoundException;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.rbac.Role;
import org.codehaus.plexus.redback.rbac.UserAssignment;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Olivier Lamy
 */
@Service("rBACManager#cached")
public class TestRBACManager implements RBACManager
{
    public void addListener( RBACManagerListener listener )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeListener( RBACManagerListener listener )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Role createRole( String name )
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean roleExists( String name )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean roleExists( Role role )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Role saveRole( Role role )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void saveRoles( Collection<Role> roles )
        throws RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Role getRole( String roleName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, Role> getRoles( Collection<String> roleNames )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addChildRole( Role role, Role childRole )
        throws RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, Role> getChildRoles( Role role )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, Role> getParentRoles( Role role )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Role> getAllRoles()
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<Role> getEffectiveRoles( Role role )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeRole( Role role )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeRole( String roleName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Permission createPermission( String name )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Permission createPermission( String name, String operationName, String resourceIdentifier )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean permissionExists( String name )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean permissionExists( Permission permission )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Permission savePermission( Permission permission )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Permission getPermission( String permissionName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Permission> getAllPermissions()
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removePermission( Permission permission )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removePermission( String permissionName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Operation createOperation( String name )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean operationExists( String name )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean operationExists( Operation operation )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Operation saveOperation( Operation operation )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Operation getOperation( String operationName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Operation> getAllOperations()
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeOperation( Operation operation )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeOperation( String operationName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Resource createResource( String identifier )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean resourceExists( String identifier )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean resourceExists( Resource resource )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Resource saveResource( Resource resource )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Resource getResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Resource> getAllResources()
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeResource( Resource resource )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public UserAssignment createUserAssignment( String principal )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean userAssignmentExists( String principal )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean userAssignmentExists( UserAssignment assignment )
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UserAssignment saveUserAssignment( UserAssignment userAssignment )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public UserAssignment getUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<UserAssignment> getAllUserAssignments()
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<UserAssignment> getUserAssignmentsForRoles( Collection<String> roleNames )
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeUserAssignment( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<Role> getAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<Role> getAssignedRoles( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<Role> getEffectivelyUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<Role> getEffectivelyAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<Role> getUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<Permission> getAssignedPermissions( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, List<Permission>> getAssignedPermissionMap( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Role> getAllAssignableRoles()
        throws RbacManagerException, RbacObjectNotFoundException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Resource getGlobalResource()
        throws RbacManagerException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void eraseDatabase()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
