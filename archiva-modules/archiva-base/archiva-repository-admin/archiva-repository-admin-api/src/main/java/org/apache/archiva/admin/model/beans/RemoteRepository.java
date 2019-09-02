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

import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@XmlRootElement (name = "remoteRepository")
public class RemoteRepository
    extends AbstractRepository
    implements Serializable
{

    private String url;

    private String userName;

    private String password;

    private int timeout = 60;

    /**
     * @since 2.2.3
     * The path to use for checking availability of the remote repository
     */
    private String checkPath;

    /**
     * Activate download of remote index if remoteIndexUrl is set too.
     */
    private boolean downloadRemoteIndex = false;

    /**
     * Remote Index Url : if not starting with http will be relative to the remote repository url.
     */
    private String remoteIndexUrl = ".index";

    private String remoteDownloadNetworkProxyId;

    /**
     * default model value daily : every sunday at 8H00
     */
    private String cronExpression = "0 0 08 ? * SUN";

    private int remoteDownloadTimeout = 300;

    /**
     * @since 1.4-M2
     */
    private boolean downloadRemoteIndexOnStartup = false;

    /**
     * extraParameters.
     *
     * @since 1.4-M4
     */
    private Map<String, String> extraParameters;

    /**
     * field to ease json mapping wrapper on <code>extraParameters</code> field
     *
     * @since 1.4-M4
     */
    private List<PropertyEntry> extraParametersEntries;

    /**
     * extraHeaders.
     *
     * @since 1.4-M4
     */
    private Map<String, String> extraHeaders;

    /**
     * field to ease json mapping wrapper on <code>extraHeaders</code> field
     *
     * @since 1.4-M4
     */
    private List<PropertyEntry> extraHeadersEntries;

    public RemoteRepository() {
        super(Locale.getDefault());
    }

    public RemoteRepository(Locale locale)
    {
        super(locale);
    }

    public RemoteRepository( Locale locale, String id, String name, String url, String layout )
    {
        super( locale, id, name, layout );
        this.url = url;
    }

    public RemoteRepository( Locale locale, String id, String name, String url, String layout, String userName, String password,
                             int timeout )
    {
        super( locale, id, name, layout );
        this.url = StringUtils.stripEnd(url,"/");
        this.userName = userName;
        this.password = password;
        this.timeout = timeout;
    }

    /**
     * @since 1.4-M3
     */
    public RemoteRepository( Locale locale, String id, String name, String url, String layout, String userName, String password,
                             int timeout, String description )
    {
        this( locale, id, name, url, layout, userName, password, timeout );
        setDescription( description );
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = StringUtils.stripEnd(url,"/");
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName( String userName )
    {
        this.userName = userName;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout( int timeout )
    {
        this.timeout = timeout;
    }

    public boolean isDownloadRemoteIndex()
    {
        return downloadRemoteIndex;
    }

    public void setDownloadRemoteIndex( boolean downloadRemoteIndex )
    {
        this.downloadRemoteIndex = downloadRemoteIndex;
    }

    public String getRemoteIndexUrl()
    {
        return remoteIndexUrl;
    }

    public void setRemoteIndexUrl( String remoteIndexUrl )
    {
        this.remoteIndexUrl = remoteIndexUrl;
    }

    public String getRemoteDownloadNetworkProxyId()
    {
        return remoteDownloadNetworkProxyId;
    }

    public void setRemoteDownloadNetworkProxyId( String remoteDownloadNetworkProxyId )
    {
        this.remoteDownloadNetworkProxyId = remoteDownloadNetworkProxyId;
    }

    public String getCronExpression()
    {
        return cronExpression;
    }

    public void setCronExpression( String cronExpression )
    {
        this.cronExpression = cronExpression;
    }

    public int getRemoteDownloadTimeout()
    {
        return remoteDownloadTimeout;
    }

    public void setRemoteDownloadTimeout( int remoteDownloadTimeout )
    {
        this.remoteDownloadTimeout = remoteDownloadTimeout;
    }

    public boolean isDownloadRemoteIndexOnStartup()
    {
        return downloadRemoteIndexOnStartup;
    }

    public void setDownloadRemoteIndexOnStartup( boolean downloadRemoteIndexOnStartup )
    {
        this.downloadRemoteIndexOnStartup = downloadRemoteIndexOnStartup;
    }

    public Map<String, String> getExtraParameters()
    {
        if ( this.extraParameters == null )
        {
            this.extraParameters = new HashMap<>();
        }
        return extraParameters;
    }

    public void setExtraParameters( Map<String, String> extraParameters )
    {
        this.extraParameters = extraParameters;
    }

    public void addExtraParameter( String key, String value )
    {
        getExtraParameters().put( key, value );
    }

    public List<PropertyEntry> getExtraParametersEntries()
    {
        this.extraParametersEntries = new ArrayList<>();
        for ( Map.Entry<String, String> entry : getExtraParameters().entrySet() )
        {
            this.extraParametersEntries.add( new PropertyEntry( entry.getKey(), entry.getValue() ) );
        }
        return this.extraParametersEntries;
    }

    public void setExtraParametersEntries( List<PropertyEntry> extraParametersEntries )
    {
        if ( extraParametersEntries == null )
        {
            return;
        }

        this.extraParametersEntries = extraParametersEntries;
        for ( PropertyEntry propertyEntry : extraParametersEntries )
        {
            this.addExtraParameter( propertyEntry.getKey(), propertyEntry.getValue() );
        }
    }

    public Map<String, String> getExtraHeaders()
    {
        if ( this.extraHeaders == null )
        {
            this.extraHeaders = new HashMap<>();
        }
        return extraHeaders;
    }

    public void setExtraHeaders( Map<String, String> extraHeaders )
    {
        this.extraHeaders = extraHeaders;
    }

    public void addExtraHeader( String key, String value )
    {
        getExtraHeaders().put( key, value );
    }

    public List<PropertyEntry> getExtraHeadersEntries()
    {
        this.extraHeadersEntries = new ArrayList<>();
        for ( Map.Entry<String, String> entry : getExtraHeaders().entrySet() )
        {
            this.extraHeadersEntries.add( new PropertyEntry( entry.getKey(), entry.getValue() ) );
        }
        return this.extraHeadersEntries;
    }

    public void setExtraHeadersEntries( List<PropertyEntry> extraHeadersEntries )
    {
        if ( extraHeadersEntries == null )
        {
            return;
        }

        this.extraHeadersEntries = extraHeadersEntries;
        for ( PropertyEntry propertyEntry : extraHeadersEntries )
        {
            this.addExtraHeader( propertyEntry.getKey(), propertyEntry.getValue() );
        }
    }

    public void setCheckPath(String checkPath) {
        if (checkPath==null) {
            this.checkPath="";
        } else if (checkPath.startsWith("/")) {
            this.checkPath = StringUtils.removeStart(checkPath, "/");
            while(this.checkPath.startsWith("/")) {
                this.checkPath = StringUtils.removeStart(checkPath, "/");
            }
        } else {
            this.checkPath = checkPath;
        }
    }

    public String getCheckPath() {
        return checkPath;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( super.toString() );
        sb.append( "RemoteRepository" );
        sb.append( "{url='" ).append( url ).append( '\'' );
        sb.append( ", userName='" ).append( userName ).append( '\'' );
        sb.append( ", password='" ).append( password ).append( '\'' );
        sb.append( ", timeout=" ).append( timeout );
        sb.append( ", downloadRemoteIndex=" ).append( downloadRemoteIndex );
        sb.append( ", remoteIndexUrl='" ).append( remoteIndexUrl ).append( '\'' );
        sb.append( ", remoteDownloadNetworkProxyId='" ).append( remoteDownloadNetworkProxyId ).append( '\'' );
        sb.append( ", cronExpression='" ).append( cronExpression ).append( '\'' );
        sb.append( ", remoteDownloadTimeout=" ).append( remoteDownloadTimeout );
        sb.append( ", downloadRemoteIndexOnStartup=" ).append( downloadRemoteIndexOnStartup );
        sb.append( ", extraParameters=" ).append( extraParameters );
        sb.append( ", extraHeaders=" ).append( extraHeaders );
        sb.append( ", checkPath=").append(checkPath);
        sb.append( '}' );
        return sb.toString();
    }


}