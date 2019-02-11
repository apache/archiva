package org.apache.archiva.configuration;

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

/**
 * configuration of a LDAP group to Archiva roles.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class LdapGroupMapping
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * LDAP Group.
     */
    private String group;

    /**
     * Field roleNames.
     */
    private java.util.List<String> roleNames;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addRoleName.
     * 
     * @param string
     */
    public void addRoleName( String string )
    {
        getRoleNames().add( string );
    } //-- void addRoleName( String )

    /**
     * Get lDAP Group.
     * 
     * @return String
     */
    public String getGroup()
    {
        return this.group;
    } //-- String getGroup()

    /**
     * Method getRoleNames.
     * 
     * @return List
     */
    public java.util.List<String> getRoleNames()
    {
        if ( this.roleNames == null )
        {
            this.roleNames = new java.util.ArrayList<String>();
        }

        return this.roleNames;
    } //-- java.util.List<String> getRoleNames()

    /**
     * Method removeRoleName.
     * 
     * @param string
     */
    public void removeRoleName( String string )
    {
        getRoleNames().remove( string );
    } //-- void removeRoleName( String )

    /**
     * Set lDAP Group.
     * 
     * @param group
     */
    public void setGroup( String group )
    {
        this.group = group;
    } //-- void setGroup( String )

    /**
     * Set archiva roles.
     * 
     * @param roleNames
     */
    public void setRoleNames( java.util.List<String> roleNames )
    {
        this.roleNames = roleNames;
    } //-- void setRoleNames( java.util.List )

}
