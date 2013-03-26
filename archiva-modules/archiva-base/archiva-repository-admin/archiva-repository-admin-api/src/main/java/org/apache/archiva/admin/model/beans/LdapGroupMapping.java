package org.apache.archiva.admin.model.beans;
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

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
public class LdapGroupMapping
{
    private String group;

    private Collection<String> roleNames;

    public LdapGroupMapping()
    {
        // no op
    }

    public LdapGroupMapping( String group )
    {
        this.group = group;
    }

    public LdapGroupMapping( String group, Collection<String> roleNames )
    {
        this.group = group;
        this.roleNames = roleNames;
    }

    public LdapGroupMapping( String group, String[] roleNames )
    {
        this.group = group;
        if ( roleNames != null )
        {
            this.roleNames = Arrays.asList( roleNames );
        }
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup( String group )
    {
        this.group = group;
    }

    public Collection<String> getRoleNames()
    {
        return roleNames;
    }

    public void setRoleNames( Collection<String> roleNames )
    {
        this.roleNames = roleNames;
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

        LdapGroupMapping that = (LdapGroupMapping) o;

        if ( group != null ? !group.equals( that.group ) : that.group != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return group != null ? group.hashCode() : 0;
    }

    @Override
    public String toString()
    {
        return "LdapGroupMapping{" +
            "group='" + group + '\'' +
            ", roleNames=" + roleNames +
            '}';
    }
}
