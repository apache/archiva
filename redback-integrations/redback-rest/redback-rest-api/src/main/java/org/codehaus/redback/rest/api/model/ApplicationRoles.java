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
import java.util.Collection;


/**
 * @author Olivier Lamy
 * @since 1.5
 */
@XmlRootElement( name = "applicationRole" )
public class ApplicationRoles
    implements Serializable
{
    private String name;

    private String description;

    private Collection<String> globalRoles;

    private Collection<RoleTemplate> roleTemplates;

    private Collection<String> resources;


    public ApplicationRoles()
    {
        // no op
    }

    public ApplicationRoles( String name, String description, Collection<String> globalRoles,
                             Collection<RoleTemplate> roleTemplates, Collection<String> resources )
    {
        this.name = name;
        this.description = description;
        this.globalRoles = globalRoles;
        this.roleTemplates = roleTemplates;
        this.resources = resources;
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

    public Collection<String> getGlobalRoles()
    {
        return globalRoles;
    }

    public void setGlobalRoles( Collection<String> globalRoles )
    {
        this.globalRoles = globalRoles;
    }

    public Collection<RoleTemplate> getRoleTemplates()
    {
        return roleTemplates;
    }

    public void setRoleTemplates( Collection<RoleTemplate> roleTemplates )
    {
        this.roleTemplates = roleTemplates;
    }

    public Collection<String> getResources()
    {
        return resources;
    }

    public void setResources( Collection<String> resources )
    {
        this.resources = resources;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ApplicationRoles" );
        sb.append( "{name='" ).append( name ).append( '\'' );
        sb.append( ", description='" ).append( description ).append( '\'' );
        sb.append( ", globalRoles=" ).append( globalRoles );
        sb.append( ", roleTemplates=" ).append( roleTemplates );
        sb.append( ", resources=" ).append( resources );
        sb.append( '}' );
        return sb.toString();
    }
}
