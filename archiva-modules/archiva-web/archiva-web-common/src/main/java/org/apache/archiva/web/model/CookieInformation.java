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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
public class CookieInformation
    implements Serializable
{
    private String path;

    private String domain;

    private String secure;

    private String timeout;

    private boolean rememberMeEnabled;

    public CookieInformation()
    {
        // no op
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public String getDomain()
    {
        return domain;
    }

    public void setDomain( String domain )
    {
        this.domain = domain;
    }

    public String getSecure()
    {
        return secure;
    }

    public void setSecure( String secure )
    {
        this.secure = secure;
    }

    public String getTimeout()
    {
        return timeout;
    }

    public void setTimeout( String timeout )
    {
        this.timeout = timeout;
    }

    public boolean isRememberMeEnabled()
    {
        return rememberMeEnabled;
    }

    public void setRememberMeEnabled( boolean rememberMeEnabled )
    {
        this.rememberMeEnabled = rememberMeEnabled;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "CookieInformation" );
        sb.append( "{path='" ).append( path ).append( '\'' );
        sb.append( ", domain='" ).append( domain ).append( '\'' );
        sb.append( ", secure='" ).append( secure ).append( '\'' );
        sb.append( ", timeout='" ).append( timeout ).append( '\'' );
        sb.append( ", rememberMeEnabled=" ).append( rememberMeEnabled );
        sb.append( '}' );
        return sb.toString();
    }
}
