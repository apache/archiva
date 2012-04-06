package org.codehaus.redback.rest.api.model;
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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@XmlRootElement( name = "role" )
public class Role
    implements Serializable
{
    /**
     * Field name
     */
    private String name;

    /**
     * Field description
     */
    private String description;

    /**
     * Field assignable
     */
    private boolean assignable = false;

    /**
     * Field childRoleNames
     */
    private List<String> childRoleNames = new ArrayList<String>(0);

    /**
     * Field permissions
     */
    private List<Permission> permissions = new ArrayList<Permission>(0);

    /**
     * some services doesn't populate this field getAllRoles in RoleManagementService
     */
    private List<String> parentRoleNames = new ArrayList<String>(0);

    /**
     * user with a parent role
     * some services doesn't populate this field getAllRoles in RoleManagementService
     */
    private List<User> parentsRolesUsers = new ArrayList<User>(0);

    /**
     * user with this role
     * some services doesn't populate this field getAllRoles in RoleManagementService
     */
    private List<User> users = new ArrayList<User>(0);

    /**
     * users without this role or parent role
     * some services doesn't populate this field getAllRoles in RoleManagementService
     */
    private List<User> otherUsers = new ArrayList<User>(0);

    /**
     * users to remove assignement to this role
     */
    private List<User> removedUsers = new ArrayList<User>(0);

    /**
     * Field permanent
     */
    private boolean permanent = false;

    public Role()
    {
        // no op
    }

    public Role( String name )
    {
        this.name = name;
    }

    public Role( org.codehaus.plexus.redback.rbac.Role role )
    {
        this.name = role.getName();
        this.description = role.getDescription();
        this.assignable = role.isAssignable();
        this.childRoleNames = role.getChildRoleNames() == null
            ? new ArrayList<String>( 0 )
            : new ArrayList<String>( role.getChildRoleNames() );

        if ( role.getPermissions() == null )
        {
            this.permissions = new ArrayList<Permission>( 0 );
        }
        else
        {
            for ( org.codehaus.plexus.redback.rbac.Permission p : role.getPermissions() )
            {
                this.permissions.add( new Permission( p ) );
            }
        }
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public boolean isAssignable()
    {
        return assignable;
    }

    public void setAssignable( boolean assignable )
    {
        this.assignable = assignable;
    }

    public List<String> getChildRoleNames()
    {
        return childRoleNames;
    }

    public void setChildRoleNames( List<String> childRoleNames )
    {
        this.childRoleNames = childRoleNames;
    }

    public List<Permission> getPermissions()
    {
        return permissions;
    }

    public void setPermissions( List<Permission> permissions )
    {
        this.permissions = permissions;
    }

    public boolean isPermanent()
    {
        return permanent;
    }

    public void setPermanent( boolean permanent )
    {
        this.permanent = permanent;
    }

    public List<String> getParentRoleNames()
    {
        return parentRoleNames;
    }

    public void setParentRoleNames( List<String> parentRoleNames )
    {
        this.parentRoleNames = parentRoleNames;
    }

    public List<User> getParentsRolesUsers()
    {
        return parentsRolesUsers;
    }

    public void setParentsRolesUsers( List<User> parentsRolesUsers )
    {
        this.parentsRolesUsers = parentsRolesUsers;
    }

    public List<User> getUsers()
    {
        return users;
    }

    public void setUsers( List<User> users )
    {
        this.users = users;
    }

    public List<User> getOtherUsers()
    {
        return otherUsers;
    }

    public void setOtherUsers( List<User> otherUsers )
    {
        this.otherUsers = otherUsers;
    }

    public List<User> getRemovedUsers()
    {
        return removedUsers;
    }

    public void setRemovedUsers( List<User> removedUsers )
    {
        this.removedUsers = removedUsers;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "Role" );
        sb.append( "{name='" ).append( name ).append( '\'' );
        sb.append( ", description='" ).append( description ).append( '\'' );
        sb.append( ", assignable=" ).append( assignable );
        sb.append( ", childRoleNames=" ).append( childRoleNames );
        sb.append( ", permissions=" ).append( permissions );
        sb.append( ", parentRoleNames=" ).append( parentRoleNames );
        sb.append( ", parentsRolesUsers=" ).append( parentsRolesUsers );
        sb.append( ", users=" ).append( users );
        sb.append( ", otherUsers=" ).append( otherUsers );
        sb.append( ", removedUsers=" ).append( removedUsers );
        sb.append( ", permanent=" ).append( permanent );
        sb.append( '}' );
        return sb.toString();
    }


    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Role role = (Role) o;

        if ( name != null ? !name.equals( role.name ) : role.name != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return name != null ? name.hashCode() : 0;
    }
}
