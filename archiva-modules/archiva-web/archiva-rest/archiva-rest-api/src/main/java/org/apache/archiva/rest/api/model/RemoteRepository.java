package org.apache.archiva.rest.api.model;

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


@XmlRootElement( name = "remoteRepository" )
public class RemoteRepository
    implements Serializable
{
    private String id;

    private String name;

    private String layout;

    private String url;

    private String userName;

    private String password;

    private int timeOut = 60;

    public RemoteRepository()
    {
        // no op
    }

    public RemoteRepository( String id, String name, String url, String layout )
    {
        this.id = id;
        this.name = name;
        this.url = url;
        this.layout = layout;
    }

    public RemoteRepository( String id, String name, String url, String layout, String userName, String password,
                             int timeOut )
    {
        this( id, name, url, layout );
        this.userName = userName;
        this.password = password;
        this.timeOut = timeOut;
    }

    public String getId()
    {
        return this.id;
    }

    public String getLayout()
    {
        return this.layout;
    }

    public String getName()
    {
        return this.name;
    }

    public String getUrl()
    {
        return this.url;
    }


    public void setId( String id )
    {
        this.id = id;
    }

    public void setLayout( String layout )
    {
        this.layout = layout;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }


    public int hashCode()
    {
        int result = 17;
        result = 37 * result + ( id != null ? id.hashCode() : 0 );
        return result;
    }

    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof RemoteRepository ) )
        {
            return false;
        }

        RemoteRepository that = (RemoteRepository) other;
        boolean result = true;
        result = result && ( getId() == null ? that.getId() == null : getId().equals( that.getId() ) );
        return result;
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

    public int getTimeOut()
    {
        return timeOut;
    }

    public void setTimeOut( int timeOut )
    {
        this.timeOut = timeOut;
    }


    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "RemoteRepository" );
        sb.append( "{id='" ).append( id ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", url='" ).append( url ).append( '\'' );
        sb.append( ", layout='" ).append( layout ).append( '\'' );
        sb.append( ", userName='" ).append( userName ).append( '\'' );
        sb.append( ", password='" ).append( password ).append( '\'' );
        sb.append( ", timeOut=" ).append( timeOut );
        sb.append( '}' );
        return sb.toString();
    }
}