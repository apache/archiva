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
import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@XmlRootElement( name = "repositoryGroup" )
public class RemoteRepository
    extends AbstractRepository
    implements Serializable
{

    private String url;

    private String userName;

    private String password;

    private int timeout = 60;

    /**
     * Activate download of remote index if remoteIndexUrl is set too.
     */
    private boolean downloadRemoteIndex = false;

    /**
     * Remote Index Url : if not starting with http will be relative to the remote repository url.
     */
    private String remoteIndexUrl = ".index";

    private String remoteDownloadNetworkProxyId;


    public RemoteRepository()
    {
        // no op
    }

    public RemoteRepository( String id, String name, String url, String layout )
    {
        super( id, name, layout );
        this.url = url;
    }

    public RemoteRepository( String id, String name, String url, String layout, String userName, String password,
                             int timeout )
    {
        super( id, name, layout );
        this.url = url;
        this.userName = userName;
        this.password = password;
        this.timeout = timeout;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
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
        sb.append( '}' );
        return sb.toString();
    }


}