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

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@XmlRootElement( name = "resource" )
public class Resource
    implements Serializable
{
    private String identifier;

    private boolean pattern;

    private boolean permanent;

    public Resource()
    {
        // no op
    }

    public Resource( org.codehaus.plexus.redback.rbac.Resource resource )
    {
        this.identifier = resource.getIdentifier();
        this.pattern = resource.isPattern();
        this.permanent = resource.isPermanent();
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier( String identifier )
    {
        this.identifier = identifier;
    }

    public boolean isPattern()
    {
        return pattern;
    }

    public void setPattern( boolean pattern )
    {
        this.pattern = pattern;
    }

    public boolean isPermanent()
    {
        return permanent;
    }

    public void setPermanent( boolean permanent )
    {
        this.permanent = permanent;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "Resource" );
        sb.append( "{identifier='" ).append( identifier ).append( '\'' );
        sb.append( ", pattern=" ).append( pattern );
        sb.append( ", permanent=" ).append( permanent );
        sb.append( '}' );
        return sb.toString();
    }
}
