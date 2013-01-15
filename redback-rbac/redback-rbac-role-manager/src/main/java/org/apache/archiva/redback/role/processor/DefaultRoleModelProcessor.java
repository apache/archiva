package org.apache.archiva.redback.role.processor;

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

import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.role.model.ModelApplication;
import org.apache.archiva.redback.role.model.ModelOperation;
import org.apache.archiva.redback.role.model.ModelPermission;
import org.apache.archiva.redback.role.model.ModelResource;
import org.apache.archiva.redback.role.model.ModelRole;
import org.apache.archiva.redback.role.model.RedbackRoleModel;
import org.apache.archiva.redback.role.util.RoleModelUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DefaultRoleModelProcessor: inserts the components of the model that can be populated into the rbac manager
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 */
@Service("roleModelProcessor")
public class DefaultRoleModelProcessor
    implements RoleModelProcessor
{
    private Logger log = LoggerFactory.getLogger( DefaultRoleModelProcessor.class );

    @Inject
    @Named(value = "rbacManager#default")
    private RBACManager rbacManager;

    private Map<String, Resource> resourceMap = new HashMap<String, Resource>();

    private Map<String, Operation> operationMap = new HashMap<String, Operation>();

    public void process( RedbackRoleModel model )
        throws RoleManagerException
    {
        // must process resources and operations first, they are required for the
        // permissions in the roles to add in correctly
        processResources( model );
        processOperations( model );

        processRoles( model );
    }

    @SuppressWarnings("unchecked")
    private void processResources( RedbackRoleModel model )
        throws RoleManagerException
    {
        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelResource profileResource : application.getResources() )
            {
                try
                {
                    if ( !rbacManager.resourceExists( profileResource.getName() ) )
                    {

                        Resource resource = rbacManager.createResource( profileResource.getName() );
                        resource.setPermanent( profileResource.isPermanent() );
                        resource = rbacManager.saveResource( resource );

                        // store for use in permission creation
                        resourceMap.put( profileResource.getId(), resource );

                    }
                    else
                    {
                        resourceMap.put( profileResource.getId(),
                                         rbacManager.getResource( profileResource.getName() ) );
                    }
                }
                catch ( RbacManagerException e )
                {
                    throw new RoleManagerException( "error creating resource '" + profileResource.getName() + "'", e );
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processOperations( RedbackRoleModel model )
        throws RoleManagerException
    {
        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelOperation profileOperation : application.getOperations() )
            {
                try
                {
                    if ( !rbacManager.operationExists( profileOperation.getName() ) )
                    {

                        Operation operation = rbacManager.createOperation( profileOperation.getName() );
                        operation.setPermanent( profileOperation.isPermanent() );
                        operation.setDescription( profileOperation.getDescription() );
                        operation = rbacManager.saveOperation( operation );

                        // store for use in permission creation
                        operationMap.put( profileOperation.getId(), operation );

                    }
                    else
                    {
                        operationMap.put( profileOperation.getId(),
                                          rbacManager.getOperation( profileOperation.getName() ) );
                    }
                }
                catch ( RbacManagerException e )
                {
                    throw new RoleManagerException( "error creating operation '" + profileOperation.getName() + "'",
                                                    e );
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processRoles( RedbackRoleModel model )
        throws RoleManagerException
    {
        List<String> sortedGraph;
        try
        {
            sortedGraph = RoleModelUtils.reverseTopologicalSortedRoleList( model );
        }
        catch ( CycleDetectedException e )
        {
            throw new RoleManagerException( "cycle detected: this should have been caught in validation", e );
        }

        for ( String roleId : sortedGraph )
        {
            ModelRole roleProfile = RoleModelUtils.getModelRole( model, roleId );

            List<Permission> permissions = processPermissions( roleProfile.getPermissions() );

            boolean roleExists = false;

            try
            {
                roleExists = rbacManager.roleExists( roleProfile.getName() );
            }
            catch ( RbacManagerException e )
            {
                throw new RoleManagerException( e.getMessage(), e );
            }

            if ( !roleExists )
            {
                try
                {
                    Role role = rbacManager.createRole( roleProfile.getName() );
                    role.setDescription( roleProfile.getDescription() );
                    role.setPermanent( roleProfile.isPermanent() );
                    role.setAssignable( roleProfile.isAssignable() );

                    // add any permissions associated with this role
                    for ( Permission permission : permissions )
                    {
                        role.addPermission( permission );
                    }

                    // add child roles to this role
                    if ( roleProfile.getChildRoles() != null )
                    {
                        for ( String childRoleId : (List<String>) roleProfile.getChildRoles() )
                        {
                            ModelRole childRoleProfile = RoleModelUtils.getModelRole( model, childRoleId );
                            role.addChildRoleName( childRoleProfile.getName() );
                        }
                    }

                    rbacManager.saveRole( role );

                    // add link from parent roles to this new role
                    if ( roleProfile.getParentRoles() != null )
                    {
                        for ( String parentRoleId : (List<String>) roleProfile.getParentRoles() )
                        {
                            ModelRole parentModelRole = RoleModelUtils.getModelRole( model, parentRoleId );
                            Role parentRole = rbacManager.getRole( parentModelRole.getName() );
                            parentRole.addChildRoleName( role.getName() );
                            rbacManager.saveRole( parentRole );
                        }
                    }

                }
                catch ( RbacManagerException e )
                {
                    throw new RoleManagerException( "error creating role '" + roleProfile.getName() + "'", e );
                }
            }
            else
            {
                try
                {
                    Role role = rbacManager.getRole( roleProfile.getName() );

                    boolean changed = false;
                    for ( Permission permission : permissions )
                    {
                        if ( !role.getPermissions().contains( permission ) )
                        {
                            log.info( "Adding new permission '" + permission.getName() + "' to role '" + role.getName()
                                          + "'" );
                            role.addPermission( permission );
                            changed = true;
                        }
                    }

                    // Copy list to avoid concurrent modification [REDBACK-220]
                    List<Permission> oldPermissions = new ArrayList<Permission>( role.getPermissions() );
                    for ( Permission permission : oldPermissions )
                    {
                        if ( !permissions.contains( permission ) )
                        {
                            log.info(
                                "Removing old permission '" + permission.getName() + "' from role '" + role.getName()
                                    + "'" );
                            role.removePermission( permission );
                            changed = true;
                        }
                    }
                    if ( changed )
                    {
                        rbacManager.saveRole( role );
                    }
                }
                catch ( RbacManagerException e )
                {
                    throw new RoleManagerException( "error updating role '" + roleProfile.getName() + "'", e );
                }
            }
        }
    }

    private List<Permission> processPermissions( List<ModelPermission> permissions )
        throws RoleManagerException
    {
        List<Permission> rbacPermissions = new ArrayList<Permission>( permissions.size() );

        for ( ModelPermission profilePermission : permissions )
        {
            try
            {
                if ( !rbacManager.permissionExists( profilePermission.getName() ) )
                {

                    Permission permission = rbacManager.createPermission( profilePermission.getName() );

                    // get the operation out of the map we stored it in when we created it _by_ the id in the model
                    Operation operation = (Operation) operationMap.get( profilePermission.getOperation() );
                    // same with resource
                    Resource resource = (Resource) resourceMap.get( profilePermission.getResource() );

                    permission.setOperation( operation );
                    permission.setResource( resource );
                    permission.setPermanent( profilePermission.isPermanent() );
                    permission.setDescription( profilePermission.getDescription() );

                    permission = rbacManager.savePermission( permission );

                    rbacPermissions.add( permission );

                }
                else
                {
                    rbacPermissions.add( rbacManager.getPermission( profilePermission.getName() ) );
                }
            }
            catch ( RbacManagerException e )
            {
                throw new RoleManagerException( "error creating permission '" + profilePermission.getName() + "'", e );
            }
        }
        return rbacPermissions;
    }

    public RBACManager getRbacManager()
    {
        return rbacManager;
    }

    public void setRbacManager( RBACManager rbacManager )
    {
        this.rbacManager = rbacManager;
    }
}


