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
 * Class SyncConnectorConfiguration.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class SyncConnectorConfiguration
    extends AbstractRepositoryConnectorConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * When to run the sync mechanism. Default is every hour on the
     * hour.
     */
    private String cronExpression = "0 0 * * * ?";

    /**
     * The type of synchronization to use.
     */
    private String method = "rsync";


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get when to run the sync mechanism. Default is every hour on
     * the hour.
     * 
     * @return String
     */
    public String getCronExpression()
    {
        return this.cronExpression;
    } //-- String getCronExpression()

    /**
     * Get the type of synchronization to use.
     * 
     * @return String
     */
    public String getMethod()
    {
        return this.method;
    } //-- String getMethod()

    /**
     * Set when to run the sync mechanism. Default is every hour on
     * the hour.
     * 
     * @param cronExpression
     */
    public void setCronExpression( String cronExpression )
    {
        this.cronExpression = cronExpression;
    } //-- void setCronExpression( String )

    /**
     * Set the type of synchronization to use.
     * 
     * @param method
     */
    public void setMethod( String method )
    {
        this.method = method;
    } //-- void setMethod( String )

}
