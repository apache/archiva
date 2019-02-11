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
 * Class NetworkProxyConfiguration.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class NetworkProxyConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The ID for this proxy.
     *           
     */
    private String id;

    /**
     * 
     *             The network protocol to use with this proxy:
     * "http", "socks-4"
     *           .
     */
    private String protocol = "http";

    /**
     * 
     *             The proxy host.
     *           
     */
    private String host;

    /**
     * 
     *             The proxy port.
     *           
     */
    private int port = 8080;

    /**
     * 
     *             The proxy user.
     *           
     */
    private String username;

    /**
     * 
     *             The proxy password.
     *           
     */
    private String password;

    /**
     * 
     *             Use ntlm authentification.
     *           
     */
    private boolean useNtlm = false;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get the proxy host.
     * 
     * @return String
     */
    public String getHost()
    {
        return this.host;
    } //-- String getHost()

    /**
     * Get the ID for this proxy.
     * 
     * @return String
     */
    public String getId()
    {
        return this.id;
    } //-- String getId()

    /**
     * Get the proxy password.
     * 
     * @return String
     */
    public String getPassword()
    {
        return this.password;
    } //-- String getPassword()

    /**
     * Get the proxy port.
     * 
     * @return int
     */
    public int getPort()
    {
        return this.port;
    } //-- int getPort()

    /**
     * Get the network protocol to use with this proxy: "http",
     * "socks-4".
     * 
     * @return String
     */
    public String getProtocol()
    {
        return this.protocol;
    } //-- String getProtocol()

    /**
     * Get the proxy user.
     * 
     * @return String
     */
    public String getUsername()
    {
        return this.username;
    } //-- String getUsername()

    /**
     * Get use ntlm authentification.
     * 
     * @return boolean
     */
    public boolean isUseNtlm()
    {
        return this.useNtlm;
    } //-- boolean isUseNtlm()

    /**
     * Set the proxy host.
     * 
     * @param host
     */
    public void setHost( String host )
    {
        this.host = host;
    } //-- void setHost( String )

    /**
     * Set the ID for this proxy.
     * 
     * @param id
     */
    public void setId( String id )
    {
        this.id = id;
    } //-- void setId( String )

    /**
     * Set the proxy password.
     * 
     * @param password
     */
    public void setPassword( String password )
    {
        this.password = password;
    } //-- void setPassword( String )

    /**
     * Set the proxy port.
     * 
     * @param port
     */
    public void setPort( int port )
    {
        this.port = port;
    } //-- void setPort( int )

    /**
     * Set the network protocol to use with this proxy: "http",
     * "socks-4".
     * 
     * @param protocol
     */
    public void setProtocol( String protocol )
    {
        this.protocol = protocol;
    } //-- void setProtocol( String )

    /**
     * Set use ntlm authentification.
     * 
     * @param useNtlm
     */
    public void setUseNtlm( boolean useNtlm )
    {
        this.useNtlm = useNtlm;
    } //-- void setUseNtlm( boolean )

    /**
     * Set the proxy user.
     * 
     * @param username
     */
    public void setUsername( String username )
    {
        this.username = username;
    } //-- void setUsername( String )

    
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

                if ( !( other instanceof NetworkProxyConfiguration ) )
                {
                    return false;
                }

                NetworkProxyConfiguration that = (NetworkProxyConfiguration) other;
                boolean result = true;
                result = result && ( getId() == null ? that.getId() == null : getId().equals( that.getId() ) );
                return result;
            }
          
}
