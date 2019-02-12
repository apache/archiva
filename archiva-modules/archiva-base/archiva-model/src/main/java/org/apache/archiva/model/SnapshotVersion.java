package org.apache.archiva.model;

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
 * The Snapshot Version.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class SnapshotVersion
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The unique timestamp for the snapshot version.
     *           
     */
    private String timestamp;

    /**
     * The incremental build number of the snapshot.
     */
    private int buildNumber = 0;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get the incremental build number of the snapshot.
     * 
     * @return int
     */
    public int getBuildNumber()
    {
        return this.buildNumber;
    } //-- int getBuildNumber()

    /**
     * Get the unique timestamp for the snapshot version.
     * 
     * @return String
     */
    public String getTimestamp()
    {
        return this.timestamp;
    } //-- String getTimestamp()

    /**
     * Set the incremental build number of the snapshot.
     * 
     * @param buildNumber
     */
    public void setBuildNumber( int buildNumber )
    {
        this.buildNumber = buildNumber;
    } //-- void setBuildNumber( int )

    /**
     * Set the unique timestamp for the snapshot version.
     * 
     * @param timestamp
     */
    public void setTimestamp( String timestamp )
    {
        this.timestamp = timestamp;
    } //-- void setTimestamp( String )

    
    private static final long serialVersionUID = -1251466956496493405L;
          
}
