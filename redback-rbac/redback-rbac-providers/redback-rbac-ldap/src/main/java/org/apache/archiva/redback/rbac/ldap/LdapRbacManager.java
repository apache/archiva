package org.apache.archiva.redback.rbac.ldap;
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

import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.rbac.AbstractRBACManager;
import org.apache.archiva.redback.rbac.AbstractRole;
import org.apache.archiva.redback.rbac.AbstractUserAssignment;
import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.RbacObjectInvalidException;
import org.apache.archiva.redback.rbac.RbacObjectNotFoundException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 2.1
 */
@Service( "rbacManager#ldap" )
public class LdapRbacManager
    extends AbstractRBACManager
    implements RBACManager
{

    @Inject
    private LdapConnectionFactory ldapConnectionFactory;

    public Role createRole( String name )
    {
        return new MockRole();
    }

    public Role saveRole( Role role )
        throws RbacManagerException
    {
        return role;
    }

    public void saveRoles( Collection<Role> roles )
        throws RbacManagerException
    {
        // no op
    }

    public Role getRole( String roleName )
        throws RbacManagerException
    {
        // TODO
        return null;
    }

    public List<Role> getAllRoles()
        throws RbacManagerException
    {
        // TODO
        return Collections.emptyList();
    }

    public void removeRole( Role role )
        throws RbacManagerException
    {
        // no op
    }

    public Permission createPermission( String name )
        throws RbacManagerException
    {
        return new MockPermission();
    }

    public Permission createPermission( String name, String operationName, String resourceIdentifier )
        throws RbacManagerException
    {
        return new MockPermission();
    }

    public Permission savePermission( Permission permission )
        throws RbacManagerException
    {
        return permission;
    }

    public Permission getPermission( String permissionName )
        throws RbacManagerException
    {
        return new MockPermission();
    }

    public List<Permission> getAllPermissions()
        throws RbacManagerException
    {
        // TODO
        return Collections.emptyList();
    }

    public void removePermission( Permission permission )
        throws RbacManagerException
    {
        // no op
    }

    public Operation createOperation( String name )
        throws RbacManagerException
    {
        return new MockOperation();
    }

    public Operation saveOperation( Operation operation )
        throws RbacManagerException
    {
        return operation;
    }

    public Operation getOperation( String operationName )
        throws RbacManagerException
    {
        return new MockOperation();
    }

    public List<Operation> getAllOperations()
        throws RbacManagerException
    {
        // TODO
        return Collections.emptyList();
    }

    public void removeOperation( Operation operation )
        throws RbacManagerException
    {
        // no op
    }

    public Resource createResource( String identifier )
        throws RbacManagerException
    {
        return new MockResource();
    }

    public Resource saveResource( Resource resource )
        throws RbacManagerException
    {
        return resource;
    }

    public Resource getResource( String resourceIdentifier )
        throws RbacManagerException
    {
        // TODO
        return new MockResource();
    }

    public List<Resource> getAllResources()
        throws RbacManagerException
    {
        // TODO
        return Collections.emptyList();
    }

    public void removeResource( Resource resource )
        throws RbacManagerException
    {
        // no op
    }

    public UserAssignment createUserAssignment( String principal )
        throws RbacManagerException
    {
        return new MockUserAssignment();
    }

    public UserAssignment saveUserAssignment( UserAssignment userAssignment )
        throws RbacManagerException
    {
        return userAssignment;
    }

    public UserAssignment getUserAssignment( String principal )
        throws RbacManagerException
    {
        // TODO
        return new MockUserAssignment();
    }

    public List<UserAssignment> getAllUserAssignments()
        throws RbacManagerException
    {
        // TODO
        return Collections.emptyList();
    }

    public List<UserAssignment> getUserAssignmentsForRoles( Collection<String> roleNames )
        throws RbacManagerException
    {
        // TODO
        return Collections.emptyList();
    }

    public void removeUserAssignment( UserAssignment userAssignment )
        throws RbacManagerException
    {
        // no op
    }

    public void eraseDatabase()
    {
        // no op
    }

    //-------------------------------
    // Mock classes
    //-------------------------------

    private static class MockRole
        extends AbstractRole
        implements Role
    {
        public void addPermission( Permission permission )
        {
            // no op
        }

        public void addChildRoleName( String name )
        {
            // no op
        }

        public List<String> getChildRoleNames()
        {
            return Collections.emptyList();
        }

        public String getDescription()
        {
            return null;
        }

        public String getName()
        {
            return null;
        }

        public List<Permission> getPermissions()
        {
            return Collections.emptyList();
        }

        public boolean isAssignable()
        {
            return false;
        }

        public void removePermission( Permission permission )
        {
            // no op
        }

        public void setAssignable( boolean assignable )
        {
            // no op
        }

        public void setChildRoleNames( List<String> names )
        {
            // no op
        }

        public void setDescription( String description )
        {
            // no op
        }

        public void setName( String name )
        {
            // no op
        }

        public void setPermissions( List<Permission> permissions )
        {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean isPermanent()
        {
            return false;
        }

        public void setPermanent( boolean permanent )
        {
            // no op
        }
    }

    private static class MockPermission
        implements Permission
    {
        public String getDescription()
        {
            return null;
        }

        public String getName()
        {
            return null;
        }

        public Operation getOperation()
        {
            return null;
        }

        public Resource getResource()
        {
            return null;
        }

        public void setDescription( String description )
        {
            // no op
        }

        public void setName( String name )
        {
            // no op
        }

        public void setOperation( Operation operation )
        {
            // no op
        }

        public void setResource( Resource resource )
        {
            // no op
        }

        public boolean isPermanent()
        {
            return false;
        }

        public void setPermanent( boolean permanent )
        {
            // no op
        }
    }

    private static class MockOperation
        implements Operation
    {
        public String getDescription()
        {
            return null;
        }

        public String getName()
        {
            return null;
        }

        public void setDescription( String description )
        {
            // no op
        }

        public void setName( String name )
        {
            // no op
        }

        public boolean isPermanent()
        {
            return false;
        }

        public void setPermanent( boolean permanent )
        {
            // no op
        }
    }

    private static class MockResource
        implements Resource
    {
        public String getIdentifier()
        {
            return null;
        }

        public boolean isPattern()
        {
            return false;
        }

        public void setIdentifier( String identifier )
        {
            // no op
        }

        public void setPattern( boolean pattern )
        {
            // no op
        }

        public boolean isPermanent()
        {
            return false;
        }

        public void setPermanent( boolean permanent )
        {
            // no op
        }
    }

    private static class MockUserAssignment
        extends AbstractUserAssignment
        implements UserAssignment
    {
        public String getPrincipal()
        {
            return null;
        }

        public List<String> getRoleNames()
        {
            return Collections.emptyList();
        }

        public void setPrincipal( String principal )
        {
            // no op
        }

        public void setRoleNames( List<String> roles )
        {
            // no op
        }

        public boolean isPermanent()
        {
            return false;
        }

        public void setPermanent( boolean permanent )
        {
            // no op
        }
    }
}
