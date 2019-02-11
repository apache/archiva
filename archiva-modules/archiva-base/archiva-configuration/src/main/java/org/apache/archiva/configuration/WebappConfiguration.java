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
 *         The webapp configuration settings.
 *       
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class WebappConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * options for altering the ui presentation.
     */
    private UserInterfaceOptions ui;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get options for altering the ui presentation.
     * 
     * @return UserInterfaceOptions
     */
    public UserInterfaceOptions getUi()
    {
        return this.ui;
    } //-- UserInterfaceOptions getUi()

    /**
     * Set options for altering the ui presentation.
     * 
     * @param ui
     */
    public void setUi( UserInterfaceOptions ui )
    {
        this.ui = ui;
    } //-- void setUi( UserInterfaceOptions )

}
