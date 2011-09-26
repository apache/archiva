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
 * @since 1.4
 */
@XmlRootElement( name = "uiConfiguration" )
public class UiConfiguration
{
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

    public UiConfiguration()
    {
        // noop
    }

    public boolean isShowFindArtifacts()
    {
        return showFindArtifacts;
    }

    public void setShowFindArtifacts( boolean showFindArtifacts )
    {
        this.showFindArtifacts = showFindArtifacts;
    }

    public boolean isAppletFindEnabled()
    {
        return appletFindEnabled;
    }

    public void setAppletFindEnabled( boolean appletFindEnabled )
    {
        this.appletFindEnabled = appletFindEnabled;
    }

    public boolean isDisableEasterEggs()
    {
        return disableEasterEggs;
    }

    public void setDisableEasterEggs( boolean disableEasterEggs )
    {
        this.disableEasterEggs = disableEasterEggs;
    }
}
