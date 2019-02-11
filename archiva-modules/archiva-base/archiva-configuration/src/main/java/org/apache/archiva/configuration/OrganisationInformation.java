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
 * 
 *         The organisation information settings.
 *       
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class OrganisationInformation
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

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


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get name of the organisation.
     * 
     * @return String
     */
    public String getLogoLocation()
    {
        return this.logoLocation;
    } //-- String getLogoLocation()

    /**
     * Get name of the organisation.
     * 
     * @return String
     */
    public String getName()
    {
        return this.name;
    } //-- String getName()

    /**
     * Get name of the organisation.
     * 
     * @return String
     */
    public String getUrl()
    {
        return this.url;
    } //-- String getUrl()

    /**
     * Set name of the organisation.
     * 
     * @param logoLocation
     */
    public void setLogoLocation( String logoLocation )
    {
        this.logoLocation = logoLocation;
    } //-- void setLogoLocation( String )

    /**
     * Set name of the organisation.
     * 
     * @param name
     */
    public void setName( String name )
    {
        this.name = name;
    } //-- void setName( String )

    /**
     * Set name of the organisation.
     * 
     * @param url
     */
    public void setUrl( String url )
    {
        this.url = url;
    } //-- void setUrl( String )

}
