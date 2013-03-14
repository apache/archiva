package org.apache.archiva.web.model;
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
import java.io.Serializable;
import java.util.Calendar;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@XmlRootElement(name = "applicationRuntimeInfo")
public class ApplicationRuntimeInfo
    implements Serializable
{
    private boolean devMode = false;

    private boolean javascriptLog = false;

    private String version;

    private String buildNumber;

    private long timestamp;

    private String copyrightRange;

    private boolean logMissingI18n;

    private String baseUrl;

    private String timestampStr;

    private CookieInformation cookieInformation;

    public ApplicationRuntimeInfo()
    {
        this.devMode = Boolean.getBoolean( "archiva.devMode" );

        this.javascriptLog = Boolean.getBoolean( "archiva.javascriptLog" );

        this.logMissingI18n = Boolean.getBoolean( "archiva.logMissingI18n" );

        this.copyrightRange = "2005 - " + Calendar.getInstance().get( Calendar.YEAR );
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

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getBuildNumber()
    {
        return buildNumber;
    }

    public void setBuildNumber( String buildNumber )
    {
        this.buildNumber = buildNumber;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp( long timestamp )
    {
        this.timestamp = timestamp;
    }

    public String getCopyrightRange()
    {
        return copyrightRange;
    }

    public void setCopyrightRange( String copyrightRange )
    {
        this.copyrightRange = copyrightRange;
    }

    public boolean isLogMissingI18n()
    {
        return logMissingI18n;
    }

    public void setLogMissingI18n( boolean logMissingI18n )
    {
        this.logMissingI18n = logMissingI18n;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl( String baseUrl )
    {
        this.baseUrl = baseUrl;
    }

    public String getTimestampStr()
    {
        return timestampStr;
    }

    public void setTimestampStr( String timestampStr )
    {
        this.timestampStr = timestampStr;
    }

    public CookieInformation getCookieInformation()
    {
        return cookieInformation;
    }

    public void setCookieInformation( CookieInformation cookieInformation )
    {
        this.cookieInformation = cookieInformation;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ApplicationRuntimeInfo" );
        sb.append( "{devMode=" ).append( devMode );
        sb.append( ", javascriptLog=" ).append( javascriptLog );
        sb.append( ", version='" ).append( version ).append( '\'' );
        sb.append( ", buildNumber='" ).append( buildNumber ).append( '\'' );
        sb.append( ", timestamp=" ).append( timestamp );
        sb.append( ", copyrightRange='" ).append( copyrightRange ).append( '\'' );
        sb.append( ", logMissingI18n=" ).append( logMissingI18n );
        sb.append( ", baseUrl='" ).append( baseUrl ).append( '\'' );
        sb.append( ", timestampStr='" ).append( timestampStr ).append( '\'' );
        sb.append( ", cookieInformation=" ).append( cookieInformation );
        sb.append( '}' );
        return sb.toString();
    }
}
