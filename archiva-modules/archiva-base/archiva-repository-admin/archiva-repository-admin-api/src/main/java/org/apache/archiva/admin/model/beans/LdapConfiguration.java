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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@XmlRootElement(name = "ldapConfiguration")
public class LdapConfiguration
    implements Serializable
{


    /**
     * The LDAP host.
     */
    private String hostName;

    /**
     * The LDAP port.
     */
    private int port;

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
     * The LDAP base dn for groups (if empty baseDn is used).
     */
    private String baseGroupsDn;

    /**
     * The LDAP password.
     */
    private String password;

    /**
     * The LDAP authenticationMethod.
     */
    private String authenticationMethod;

    /**
     *
     */
    private boolean bindAuthenticatorEnabled;

    /**
     * Will use role name as LDAP group.
     */
    private boolean useRoleNameAsGroup = false;

    /**
     * Field extraProperties.
     */
    private Map<String, String> extraProperties = new HashMap<>();

    /**
     * field to ease json mapping wrapper on <code>extraProperties</code> field
     */
    private List<PropertyEntry> extraPropertiesEntries;

    /**
     * LDAP writable.
     */
    private boolean writable = false;

    public LdapConfiguration()
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

    public int getPort()
    {
        return port;
    }

    public void setPort( int port )
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

    public boolean isBindAuthenticatorEnabled()
    {
        return bindAuthenticatorEnabled;
    }

    public void setBindAuthenticatorEnabled( boolean bindAuthenticatorEnabled )
    {
        this.bindAuthenticatorEnabled = bindAuthenticatorEnabled;
    }

    public List<PropertyEntry> getExtraPropertiesEntries()
    {
        extraPropertiesEntries = new ArrayList<>( getExtraProperties().size() );
        for ( Map.Entry<String, String> entry : getExtraProperties().entrySet() )
        {
            extraPropertiesEntries.add( new PropertyEntry( entry.getKey(), entry.getValue() ) );
        }
        return extraPropertiesEntries;
    }

    public void setExtraPropertiesEntries( List<PropertyEntry> extraPropertiesEntries )
    {
        this.extraPropertiesEntries = extraPropertiesEntries;
        if ( extraPropertiesEntries != null )
        {
            for ( PropertyEntry propertyEntry : extraPropertiesEntries )
            {
                this.extraProperties.put( propertyEntry.getKey(), propertyEntry.getValue() );
            }
        }
    }

    public String getBaseGroupsDn()
    {
        return baseGroupsDn;
    }

    public void setBaseGroupsDn( String baseGroupsDn )
    {
        this.baseGroupsDn = baseGroupsDn;
    }

    public boolean isWritable()
    {
        return writable;
    }

    public void setWritable( boolean writable )
    {
        this.writable = writable;
    }

    public boolean isUseRoleNameAsGroup()
    {
        return useRoleNameAsGroup;
    }

    public void setUseRoleNameAsGroup( boolean useRoleNameAsGroup )
    {
        this.useRoleNameAsGroup = useRoleNameAsGroup;
    }
}
