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
 *         The network configuration for external http request to
 * repositories.
 *       
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class NetworkConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * maximum total external http connections.
     */
    private int maxTotal = 30;

    /**
     * maximum total external http connections per host.
     */
    private int maxTotalPerHost = 30;

    /**
     * use or not http connection pooling default true.
     */
    private boolean usePooling = true;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get maximum total external http connections.
     * 
     * @return int
     */
    public int getMaxTotal()
    {
        return this.maxTotal;
    } //-- int getMaxTotal()

    /**
     * Get maximum total external http connections per host.
     * 
     * @return int
     */
    public int getMaxTotalPerHost()
    {
        return this.maxTotalPerHost;
    } //-- int getMaxTotalPerHost()

    /**
     * Get use or not http connection pooling default true.
     * 
     * @return boolean
     */
    public boolean isUsePooling()
    {
        return this.usePooling;
    } //-- boolean isUsePooling()

    /**
     * Set maximum total external http connections.
     * 
     * @param maxTotal
     */
    public void setMaxTotal( int maxTotal )
    {
        this.maxTotal = maxTotal;
    } //-- void setMaxTotal( int )

    /**
     * Set maximum total external http connections per host.
     * 
     * @param maxTotalPerHost
     */
    public void setMaxTotalPerHost( int maxTotalPerHost )
    {
        this.maxTotalPerHost = maxTotalPerHost;
    } //-- void setMaxTotalPerHost( int )

    /**
     * Set use or not http connection pooling default true.
     * 
     * @param usePooling
     */
    public void setUsePooling( boolean usePooling )
    {
        this.usePooling = usePooling;
    } //-- void setUsePooling( boolean )

}
