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
 *         The user interface configuration settings.
 *       
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class UserInterfaceOptions
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * true if find artifacts should be enabled.
     */
    private boolean showFindArtifacts = true;

    /**
     * true if applet behavior for find artifacts should be enabled.
     */
    private boolean appletFindEnabled = true;

    /**
     * Field disableEasterEggs.
     */
    private boolean disableEasterEggs = false;

    /**
     * Field applicationUrl.
     */
    private String applicationUrl;

    /**
     * Field disableRegistration.
     */
    private boolean disableRegistration = false;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get the applicationUrl field.
     * 
     * @return String
     */
    public String getApplicationUrl()
    {
        return this.applicationUrl;
    } //-- String getApplicationUrl()

    /**
     * Get true if applet behavior for find artifacts should be
     * enabled.
     * 
     * @return boolean
     */
    public boolean isAppletFindEnabled()
    {
        return this.appletFindEnabled;
    } //-- boolean isAppletFindEnabled()

    /**
     * Get the disableEasterEggs field.
     * 
     * @return boolean
     */
    public boolean isDisableEasterEggs()
    {
        return this.disableEasterEggs;
    } //-- boolean isDisableEasterEggs()

    /**
     * Get the disableRegistration field.
     * 
     * @return boolean
     */
    public boolean isDisableRegistration()
    {
        return this.disableRegistration;
    } //-- boolean isDisableRegistration()

    /**
     * Get true if find artifacts should be enabled.
     * 
     * @return boolean
     */
    public boolean isShowFindArtifacts()
    {
        return this.showFindArtifacts;
    } //-- boolean isShowFindArtifacts()

    /**
     * Set true if applet behavior for find artifacts should be
     * enabled.
     * 
     * @param appletFindEnabled
     */
    public void setAppletFindEnabled( boolean appletFindEnabled )
    {
        this.appletFindEnabled = appletFindEnabled;
    } //-- void setAppletFindEnabled( boolean )

    /**
     * Set the applicationUrl field.
     * 
     * @param applicationUrl
     */
    public void setApplicationUrl( String applicationUrl )
    {
        this.applicationUrl = applicationUrl;
    } //-- void setApplicationUrl( String )

    /**
     * Set the disableEasterEggs field.
     * 
     * @param disableEasterEggs
     */
    public void setDisableEasterEggs( boolean disableEasterEggs )
    {
        this.disableEasterEggs = disableEasterEggs;
    } //-- void setDisableEasterEggs( boolean )

    /**
     * Set the disableRegistration field.
     * 
     * @param disableRegistration
     */
    public void setDisableRegistration( boolean disableRegistration )
    {
        this.disableRegistration = disableRegistration;
    } //-- void setDisableRegistration( boolean )

    /**
     * Set true if find artifacts should be enabled.
     * 
     * @param showFindArtifacts
     */
    public void setShowFindArtifacts( boolean showFindArtifacts )
    {
        this.showFindArtifacts = showFindArtifacts;
    } //-- void setShowFindArtifacts( boolean )

}
