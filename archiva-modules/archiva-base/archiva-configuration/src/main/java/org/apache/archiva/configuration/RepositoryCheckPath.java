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
 * Class RepositoryCheckPath.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class RepositoryCheckPath
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The URL for which this path should be used
     *           .
     */
    private String url;

    /**
     * 
     *             The path to use for checking the repository
     * connection.
     *           
     */
    private String path;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get the path to use for checking the repository connection.
     * 
     * @return String
     */
    public String getPath()
    {
        return this.path;
    } //-- String getPath()

    /**
     * Get the URL for which this path should be used.
     * 
     * @return String
     */
    public String getUrl()
    {
        return this.url;
    } //-- String getUrl()

    /**
     * Set the path to use for checking the repository connection.
     * 
     * @param path
     */
    public void setPath( String path )
    {
        this.path = path;
    } //-- void setPath( String )

    /**
     * Set the URL for which this path should be used.
     * 
     * @param url
     */
    public void setUrl( String url )
    {
        this.url = url;
    } //-- void setUrl( String )

}
