package org.apache.archiva.redback.role;

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

import org.apache.archiva.redback.role.model.RedbackRoleModel;

import java.net.URL;

/**
 * RoleProfileManager:
 *
 * @author: Jesse McConnell <jesse@codehaus.org>
 *
 */
public interface RoleManager
{

    /**
     * load the model and create/verify operations, resources, etc exist and make static roles
     *
     * @param resourceLocation
     * @throws RoleManagerException
     */
    void loadRoleModel( URL resourceLocation )
        throws RoleManagerException;

    void loadRoleModel( RedbackRoleModel model )
        throws RoleManagerException;

    /**
     * locate a role with the corresponding name and generate it with the given resource, ${resource}
     * in the model will be replaced with this resource string, if this resource does not exist, it
     * will be created.
     *
     * @param templateId
     * @param resource
     * @throws RoleManagerException
     */
    void createTemplatedRole( String templateId, String resource )
        throws RoleManagerException;

    /**
     * removes a role corresponding to the role Id that was manufactured with the given resource
     * <p/>
     * it also removes any user assignments for that role
     *
     * @param templateId
     * @param resource
     * @throws RoleManagerException
     */
    void removeTemplatedRole( String templateId, String resource )
        throws RoleManagerException;


    /**
     * allows for a role coming from a template to be renamed effectively swapping out the bits of it that
     * were labeled with the oldResource with the newResource
     * <p/>
     * it also manages any user assignments for that role
     *
     * @param templateId
     * @param oldResource
     * @param newResource
     * @throws RoleManagerException
     */
    void updateRole( String templateId, String oldResource, String newResource )
        throws RoleManagerException;


    /**
     * Assigns the role indicated by the roleId to the given principal
     *
     * @param roleId
     * @param principal
     * @throws RoleManagerException
     */
    void assignRole( String roleId, String principal )
        throws RoleManagerException;

    /**
     * Assigns the role indicated by the roleName to the given principal
     *
     * @param roleName
     * @param principal
     * @throws RoleManagerException
     */
    void assignRoleByName( String roleName, String principal )
        throws RoleManagerException;

    /**
     * Assigns the templated role indicated by the templateId
     * <p/>
     * fails if the templated role has not been created
     *
     * @param templateId
     * @param resource
     * @param principal
     */
    void assignTemplatedRole( String templateId, String resource, String principal )
        throws RoleManagerException;

    /**
     * Unassigns the role indicated by the role id from the given principal
     *
     * @param roleId
     * @param principal
     * @throws RoleManagerException
     */
    void unassignRole( String roleId, String principal )
        throws RoleManagerException;

    /**
     * Unassigns the role indicated by the role name from the given principal
     *
     * @param roleName
     * @param principal
     * @throws RoleManagerException
     */
    void unassignRoleByName( String roleName, String principal )
        throws RoleManagerException;

    /**
     * true of a role exists with the given roleId
     *
     * @param roleId
     * @return
     * @throws RoleManagerException
     */
    boolean roleExists( String roleId )
        throws RoleManagerException;

    /**
     * true of a role exists with the given roleId
     *
     * @param templateId
     * @param resource
     * @return
     * @throws RoleManagerException
     */
    boolean templatedRoleExists( String templateId, String resource )
        throws RoleManagerException;

    /**
     * get the blessed model, the current operating instructions for all things role management
     */
    RedbackRoleModel getModel();

    /**
     * Check a role template is complete in the RBAC store.
     *
     * @param templateID the templated role
     * @param resource   the resource to verify
     * @throws RoleManagerException
     */
    void verifyTemplatedRole( String templateID, String resource )
        throws RoleManagerException;

    /**
     * intialize the role manager
     * @since 2.1
     */
    void initialize();
}