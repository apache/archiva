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

import org.apache.archiva.admin.model.AbstractRepository;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@XmlRootElement( name = "remoteRepository" )
public class RemoteRepository
    extends AbstractRepository
    implements Serializable
{

    private String url;

    private String userName;

    private String password;

    private int timeout = 60;

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
                             int timeOut )
    {
        super( id, name, layout );
        this.url = url;
        this.userName = userName;
        this.password = password;
        this.timeout = timeOut;
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

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "RemoteRepository" );
        sb.append( "{url='" ).append( url ).append( '\'' );
        sb.append( ", userName='" ).append( userName ).append( '\'' );
        sb.append( ", password='" ).append( password ).append( '\'' );
        sb.append( ", timeout=" ).append( timeout );
        sb.append( '}' );
        sb.append( super.toString() );
        return sb.toString();
    }


}