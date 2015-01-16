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
 * @since 1.4-M1
 */
@XmlRootElement( name = "networkProxy" )
public class NetworkProxy
    implements Serializable
{
    private String id;

    /**
     * The network protocol to use with this proxy: "http", "socks-4"
     * .
     */
    private String protocol = "http";

    /**
     * The proxy host.
     */
    private String host;

    /**
     * The proxy port.
     */
    private int port = 8080;

    /**
     * The proxy user.
     */
    private String username;

    /**
     * The proxy password.
     */
    private String password;

    /**
     * @since 1.4-M3
     *
     * use NTLM proxy
     */
    private boolean useNtlm;

    public NetworkProxy()
    {
        // no op
    }

    public NetworkProxy( String id, String protocol, String host, int port, String username, String password )
    {
        this.id = id;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol( String protocol )
    {
        this.protocol = protocol;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost( String host )
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort( int port )
    {
        this.port = port;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public boolean isUseNtlm()
    {
        return useNtlm;
    }

    public void setUseNtlm( boolean useNtlm )
    {
        this.useNtlm = useNtlm;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        NetworkProxy that = (NetworkProxy) o;

        if ( id != null ? !id.equals( that.id ) : that.id != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 37 * result + ( id != null ? id.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "NetworkProxy" );
        sb.append( "{id='" ).append( id ).append( '\'' );
        sb.append( ", protocol='" ).append( protocol ).append( '\'' );
        sb.append( ", host='" ).append( host ).append( '\'' );
        sb.append( ", port=" ).append( port );
        sb.append( ", username='" ).append( username ).append( '\'' );
        //sb.append( ", password='" ).append( password ).append( '\'' );
        sb.append( ", useNtlm=" ).append( useNtlm );
        sb.append( '}' );
        return sb.toString();
    }
}
