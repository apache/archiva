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
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.5
 */
@XmlRootElement( name = "roleTemplate" )
public class RoleTemplate
    implements Serializable
{
    /**
     * Field id
     */
    private String id;

    private String namePrefix;

    private String delimiter = " - ";

    private String description;

    private String resource;

    private List<String> roles;

    public RoleTemplate()
    {
        // no op
    }

    public RoleTemplate( String id, String namePrefix, String delimiter, String description, String resource,
                         List<String> roles )
    {
        this.id = id;
        this.namePrefix = namePrefix;
        this.delimiter = delimiter;
        this.description = description;
        this.resource = resource;
        this.roles = roles;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getNamePrefix()
    {
        return namePrefix;
    }

    public void setNamePrefix( String namePrefix )
    {
        this.namePrefix = namePrefix;
    }

    public String getDelimiter()
    {
        return delimiter;
    }

    public void setDelimiter( String delimiter )
    {
        this.delimiter = delimiter;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getResource()
    {
        return resource;
    }

    public void setResource( String resource )
    {
        this.resource = resource;
    }

    public List<String> getRoles()
    {
        return roles;
    }

    public void setRoles( List<String> roles )
    {
        this.roles = roles;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "RoleTemplate" );
        sb.append( "{id='" ).append( id ).append( '\'' );
        sb.append( ", namePrefix='" ).append( namePrefix ).append( '\'' );
        sb.append( ", delimiter='" ).append( delimiter ).append( '\'' );
        sb.append( ", description='" ).append( description ).append( '\'' );
        sb.append( ", resource='" ).append( resource ).append( '\'' );
        sb.append( ", roles=" ).append( roles );
        sb.append( '}' );
        return sb.toString();
    }
}
