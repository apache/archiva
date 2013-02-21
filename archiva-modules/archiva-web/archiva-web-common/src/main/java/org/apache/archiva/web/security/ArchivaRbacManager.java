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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        return getRbacManagerForWrite().saveRole( role );
    }

    public void saveRoles( Collection<Role> roles )
        throws RbacObjectInvalidException, RbacManagerException
    {
        getRbacManagerForWrite().saveRoles( roles );
    }

    public Role getRole( String roleName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        for ( RBACManager rbacManager : rbacManagersPerId.values() )
        {
            Role role = rbacManager.getRole( roleName );
            if ( role != null )
            {
                return role;
            }
        }
        log.debug( "cannot find role for name: ‘{}", roleName );
        return null;
    }

    public List<Role> getAllRoles()
        throws RbacManagerException
    {
        // iterate and aggregate results ?
        return getRbacManagerForWrite().getAllRoles();
    }

    public void removeRole( Role role )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        getRbacManagerForWrite().removeRole( role );
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
        return getRbacManagerForWrite().savePermission( permission );
    }

    public Permission getPermission( String permissionName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return getRbacManagerForWrite().getPermission( permissionName );
    }

    public List<Permission> getAllPermissions()
        throws RbacManagerException
    {
        return getRbacManagerForWrite().getAllPermissions();
    }

    public void removePermission( Permission permission )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        getRbacManagerForWrite().removePermission( permission );
    }

    public Operation createOperation( String name )
        throws RbacManagerException
    {
        return getRbacManagerForWrite().createOperation( name );
    }

    public Operation saveOperation( Operation operation )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return getRbacManagerForWrite().saveOperation( operation );
    }

    public Operation getOperation( String operationName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return getRbacManagerForWrite().getOperation( operationName );
    }

    public List<Operation> getAllOperations()
        throws RbacManagerException
    {
        return getRbacManagerForWrite().getAllOperations();
    }

    public void removeOperation( Operation operation )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        getRbacManagerForWrite().removeOperation( operation );
    }

    public Resource createResource( String identifier )
        throws RbacManagerException
    {
        return getRbacManagerForWrite().createResource( identifier );
    }

    public Resource saveResource( Resource resource )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return getRbacManagerForWrite().saveResource( resource );
    }

    public Resource getResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return getRbacManagerForWrite().getResource( resourceIdentifier );
    }

    public List<Resource> getAllResources()
        throws RbacManagerException
    {
        return getRbacManagerForWrite().getAllResources();
    }

    public void removeResource( Resource resource )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        getRbacManagerForWrite().removeResource( resource );
    }

    public UserAssignment createUserAssignment( String principal )
        throws RbacManagerException
    {
        return getRbacManagerForWrite().createUserAssignment( principal );
    }

    public UserAssignment saveUserAssignment( UserAssignment userAssignment )
        throws RbacObjectInvalidException, RbacManagerException
    {
        return getRbacManagerForWrite().saveUserAssignment( userAssignment );
    }

    public UserAssignment getUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return getRbacManagerForWrite().getUserAssignment( principal );
    }

    @Override
    public boolean userAssignmentExists( String principal )
    {
        return getRbacManagerForWrite().userAssignmentExists( principal );
    }

    @Override
    public boolean userAssignmentExists( UserAssignment assignment )
    {
        return getRbacManagerForWrite().userAssignmentExists( assignment );
    }

    public List<UserAssignment> getAllUserAssignments()
        throws RbacManagerException
    {
        // iterate
        return getRbacManagerForWrite().getAllUserAssignments();
    }

    public List<UserAssignment> getUserAssignmentsForRoles( Collection<String> roleNames )
        throws RbacManagerException
    {
        // iterate ?
        return getRbacManagerForWrite().getUserAssignmentsForRoles( roleNames );
    }

    public void removeUserAssignment( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        getRbacManagerForWrite().removeUserAssignment( userAssignment );
    }

    @Override
    public boolean roleExists( String name )
        throws RbacManagerException
    {
        boolean exists = false;
        for ( RBACManager manager : rbacManagersPerId.values() )
        {
            exists = manager.roleExists( name );
            if ( exists )
            {
                return true;
            }
        }
        return exists;
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
