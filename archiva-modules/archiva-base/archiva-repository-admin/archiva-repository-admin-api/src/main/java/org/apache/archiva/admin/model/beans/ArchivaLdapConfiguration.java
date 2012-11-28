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
import java.util.HashMap;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@XmlRootElement ( name = "archivaLdapConfiguration" )
public class ArchivaLdapConfiguration
{


    /**
     * The LDAP host.
     */
    private String hostName;

    /**
     * The LDAP port.
     */
    private String port;

    /**
     * ssl LDAP connection.
     */
    private boolean ssl = false;

    /**
     * The LDAP base dn.
     */
    private String baseDn;

    /**
     * contextFactory to use.
     */
    private String contextFactory;

    /**
     * The LDAP bind dn.
     */
    private String bindDn;

    /**
     * The LDAP password.
     */
    private String password;

    /**
     * The LDAP authenticationMethod.
     */
    private String authenticationMethod;

    /**
     * Field extraProperties.
     */
    private Map<String, String> extraProperties = new HashMap<String, String>();

    public ArchivaLdapConfiguration()
    {
        // no op
    }

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName( String hostName )
    {
        this.hostName = hostName;
    }

    public String getPort()
    {
        return port;
    }

    public void setPort( String port )
    {
        this.port = port;
    }

    public boolean isSsl()
    {
        return ssl;
    }

    public void setSsl( boolean ssl )
    {
        this.ssl = ssl;
    }

    public String getBaseDn()
    {
        return baseDn;
    }

    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn;
    }

    public String getContextFactory()
    {
        return contextFactory;
    }

    public void setContextFactory( String contextFactory )
    {
        this.contextFactory = contextFactory;
    }

    public String getBindDn()
    {
        return bindDn;
    }

    public void setBindDn( String bindDn )
    {
        this.bindDn = bindDn;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public String getAuthenticationMethod()
    {
        return authenticationMethod;
    }

    public void setAuthenticationMethod( String authenticationMethod )
    {
        this.authenticationMethod = authenticationMethod;
    }

    public Map<String, String> getExtraProperties()
    {
        return extraProperties;
    }

    public void setExtraProperties( Map<String, String> extraProperties )
    {
        this.extraProperties = extraProperties;
    }
}
