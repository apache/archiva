package org.apache.archiva.webapp.ui.services.model;
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
 * @since 1.4-M3
 */
@XmlRootElement( name = "applicationRuntimeInfo" )
public class ApplicationRuntimeInfo
{
    private boolean devMode = false;

    private boolean javascriptLog = false;

    public ApplicationRuntimeInfo()
    {
        this.devMode = Boolean.getBoolean( "archiva.devMode" );

        this.javascriptLog = Boolean.getBoolean( "archiva.javascriptLog" );
    }

    public boolean isDevMode()
    {
        return devMode;
    }

    public void setDevMode( boolean devMode )
    {
        this.devMode = devMode;
    }

    public boolean isJavascriptLog()
    {
        return javascriptLog;
    }

    public void setJavascriptLog( boolean javascriptLog )
    {
        this.javascriptLog = javascriptLog;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ApplicationRuntimeInfo" );
        sb.append( "{devMode=" ).append( devMode );
        sb.append( ", javascriptLog=" ).append( javascriptLog );
        sb.append( '}' );
        return sb.toString();
    }
}
