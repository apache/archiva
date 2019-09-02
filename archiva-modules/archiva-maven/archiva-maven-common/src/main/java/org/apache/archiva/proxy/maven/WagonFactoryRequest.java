package org.apache.archiva.proxy.maven;
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

import org.apache.archiva.proxy.model.NetworkProxy;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
public class WagonFactoryRequest
{

    public static final String USER_AGENT_SYSTEM_PROPERTY = "archiva.userAgent";

    private static String DEFAULT_USER_AGENT = "Java-Archiva";

    /**
     * the protocol to find the Wagon for, which must be prefixed with <code>wagon#</code>, for example
     * <code>wagon#http</code>. <b>to have a wagon supporting ntlm add -ntlm</b>
     */
    private String protocol;

    private Map<String, String> headers = new HashMap<>();

    private String userAgent = DEFAULT_USER_AGENT;

    static {
        if ( StringUtils.isNotBlank( System.getProperty( USER_AGENT_SYSTEM_PROPERTY))) {
            DEFAULT_USER_AGENT=System.getProperty(USER_AGENT_SYSTEM_PROPERTY);
        }
    }

    private NetworkProxy networkProxy;

    public WagonFactoryRequest()
    {
        // no op
    }

    public WagonFactoryRequest( String protocol, Map<String, String> headers )
    {
        this.protocol = protocol;
        this.headers = headers;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol( String protocol )
    {
        this.protocol = protocol;
    }

    public WagonFactoryRequest protocol( String protocol )
    {
        this.protocol = protocol;
        return this;
    }

    public Map<String, String> getHeaders()
    {
        if ( this.headers == null )
        {
            this.headers = new HashMap<>();
        }
        return headers;
    }

    public void setHeaders( Map<String, String> headers )
    {
        this.headers = headers;
    }

    public WagonFactoryRequest headers( Map<String, String> headers )
    {
        this.headers = headers;
        return this;
    }

    public String getUserAgent()
    {
        return userAgent;
    }

    public void setUserAgent( String userAgent )
    {
        this.userAgent = userAgent;
    }

    public WagonFactoryRequest userAgent( String userAgent )
    {
        this.userAgent = userAgent;
        return this;
    }

    public NetworkProxy getNetworkProxy()
    {
        return networkProxy;
    }

    public void setNetworkProxy( NetworkProxy networkProxy )
    {
        this.networkProxy = networkProxy;
    }

    public WagonFactoryRequest networkProxy( NetworkProxy networkProxy )
    {
        this.networkProxy = networkProxy;
        return this;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof WagonFactoryRequest ) )
        {
            return false;
        }

        WagonFactoryRequest that = (WagonFactoryRequest) o;

        if ( protocol != null ? !protocol.equals( that.protocol ) : that.protocol != null )
        {
            return false;
        }
        if ( userAgent != null ? !userAgent.equals( that.userAgent ) : that.userAgent != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = protocol != null ? protocol.hashCode() : 0;
        result = 31 * result + ( userAgent != null ? userAgent.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return "WagonFactoryRequest{" +
            "protocol='" + protocol + '\'' +
            ", headers=" + headers +
            ", userAgent='" + userAgent + '\'' +
            ", networkProxy=" + networkProxy +
            '}';
    }
}
