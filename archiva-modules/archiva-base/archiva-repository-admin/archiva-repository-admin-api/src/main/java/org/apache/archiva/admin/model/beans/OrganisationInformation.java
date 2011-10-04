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

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@XmlRootElement( name = "organisationInformation" )
public class OrganisationInformation
{
    /**
     * name of the organisation.
     */
    private String name;

    /**
     * name of the organisation.
     */
    private String url;

    /**
     * name of the organisation.
     */
    private String logoLocation;

    public OrganisationInformation()
    {
        // no op
    }

    public OrganisationInformation( String name, String url, String logoLocation )
    {
        this.name = name;
        this.url = url;
        this.logoLocation = logoLocation;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getLogoLocation()
    {
        return logoLocation;
    }

    public void setLogoLocation( String logoLocation )
    {
        this.logoLocation = logoLocation;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "OrganisationInformation" );
        sb.append( "{name='" ).append( name ).append( '\'' );
        sb.append( ", url='" ).append( url ).append( '\'' );
        sb.append( ", logoLocation='" ).append( logoLocation ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
