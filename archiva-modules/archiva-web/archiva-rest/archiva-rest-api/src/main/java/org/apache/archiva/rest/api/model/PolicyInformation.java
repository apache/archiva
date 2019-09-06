package org.apache.archiva.rest.api.model;
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

import org.apache.archiva.policies.PolicyOption;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@XmlRootElement( name = "policyInformation" )
public class PolicyInformation
    implements Serializable
{
    private List<PolicyOption> options;

    private PolicyOption defaultOption;

    private String id;

    private String name;

    public PolicyInformation()
    {
        // no op
    }

    public PolicyInformation(List<PolicyOption> options, PolicyOption defaultOption, String id, String name )
    {
        this.options = options;
        this.defaultOption = defaultOption;
        this.id = id;
        this.name = name;
    }

    public List<PolicyOption> getOptions()
    {
        return options;
    }

    public void setOptions( List<PolicyOption> options )
    {
        this.options = options;
    }

    public PolicyOption getDefaultOption()
    {
        return defaultOption;
    }

    public void setDefaultOption( PolicyOption defaultOption )
    {
        this.defaultOption = defaultOption;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "PolicyInformation" );
        sb.append( "{options=" ).append( options );
        sb.append( ", defaultOption='" ).append( defaultOption ).append( '\'' );
        sb.append( ", id='" ).append( id ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
