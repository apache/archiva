package org.codehaus.plexus.redback.rbac;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.List;

/**
 * Role
 * <p/>
 * A role is assignable to a user and effectively grants that user all of the
 * permissions that are present in that role.  A role can also contain other roles
 * which add the permissions in those roles to the available permissions for authorization.
 * <p/>
 * A role can contain any number of permissions
 * A role can contain any number of other roles
 * A role can be assigned to any number of users
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface Role
{

    /**
     * Method addPermission
     *
     * @param permission
     */
    void addPermission( Permission permission );

    /**
     * Method addChildRoleName
     *
     * @param name the name of the child role.
     */
    void addChildRoleName( String name );

    /**
     * Method getChildRoleNames
     */
    List<String> getChildRoleNames();

    /**
     * Convienence method to see if Role has Child Roles.
     *
     * @return true if child roles exists and has any roles being tracked.
     */
    boolean hasChildRoles();

    /**
     * Long description of the role.
     */
    String getDescription();

    /**
     * Get the name.
     * <p/>
     * NOTE: This field is considered the Primary Key for this object.
     */
    String getName();

    /**
     * Method getPermissions
     */
    List<Permission> getPermissions();

    /**
     * true if this role is available to be assigned to a user
     */
    boolean isAssignable();

    /**
     * Method removePermission
     *
     * @param permission
     */
    void removePermission( Permission permission );

    /**
     * true if this role is available to be assigned to a user
     *
     * @param assignable
     */
    void setAssignable( boolean assignable );

    /**
     * The names of the roles that will inherit the permissions of this role
     *
     * @param names the list of names of other roles.
     */
    void setChildRoleNames( List<String> names );

    /**
     * Set the Description
     *
     * @param description
     */
    void setDescription( String description );

    /**
     * Set Name
     * <p/>
     * NOTE: This field is considered the Primary Key for this object.
     *
     * @param name
     */
    void setName( String name );

    /**
     * Set Permissions
     *
     * @param permissions
     */
    void setPermissions( List<Permission> permissions );

    /**
     * Test to see if the object is a permanent object or not.
     *
     * @return true if the object is permanent.
     */
    boolean isPermanent();

    /**
     * Set flag indicating if the object is a permanent object or not.
     *
     * @param permanent true if the object is permanent.
     */
    void setPermanent( boolean permanent );
}