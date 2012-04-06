package org.codehaus.plexus.redback.rbac;

import java.util.List;

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

/**
 * UserAssignment - This the mapping object that takes the principal for a user and associates it with a
 * set of Roles.
 * 
 * This is the many to many mapping object needed by persistence stores.
 *
 * @author Jesse McConnell <jmcconnell@apache.org>
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @todo expand on javadoc
 */
public interface UserAssignment
{

    
    /**
     * The principal for the User that the set of roles is associated with.
     * 
     * NOTE: This field is considered the Primary Key for this object.
     * 
     * @return the principal for the User.
     */
    String getPrincipal();

    /**
     * Get the roles for this user.
     * 
     * @return List of &lt;{@link String}&gt; objects representing the Role Names.
     */
    List<String> getRoleNames();
    
    /**
     * Add a rolename to this assignment.
     * 
     * @param role the role.
     */
    void addRoleName( Role role );
    
    /**
     * Add a rolename to this assignment.
     * 
     * @param roleName the role name.
     */
    void addRoleName( String roleName );
    
    /**
     * Remove a rolename from this assignment.
     * 
     * @param role the role who's name is to be removed.
     */
    void removeRoleName( Role role );
    
    /**
     * Remove a role name from this assignment.
     * 
     * @param roleName the role name to be removed.
     */
    void removeRoleName( String roleName );

    /**
     * Set the user principal object for this association.
     * 
     * NOTE: This field is considered the Primary Key for this object.
     * 
     * @param principal
     */
    void setPrincipal( String principal );

    /**
     * Set the roles names for this user.
     * 
     * @param roles the List of &lt;{@link String}&gt; objects representing the Role Names.
     */
    void setRoleNames( List<String> roles );
    
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