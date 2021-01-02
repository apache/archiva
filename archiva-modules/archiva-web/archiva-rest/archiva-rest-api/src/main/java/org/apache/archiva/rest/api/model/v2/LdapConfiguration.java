package org.apache.archiva.rest.api.model.v2;/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="ldapConfiguration")
public class LdapConfiguration implements Serializable
{
    private static final long serialVersionUID = -4736767846016398583L;

    private String hostName = "";
    private int port = 389;
    private boolean sslEnabled = false;
    private String baseDn = "";
    private String groupsBaseDn = "";
    private String bindDn = "";
    private String bindPassword = "";
    private String authenticationMethod = "";
    private boolean bindAuthenticatorEnabled = true;
    private boolean useRoleNameAsGroup = false;
    private final Map<String, String> properties = new TreeMap<>();
    private boolean writable = false;

    public LdapConfiguration( )
    {
    }

    public static LdapConfiguration of( org.apache.archiva.admin.model.beans.LdapConfiguration ldapConfiguration ) {
        LdapConfiguration newCfg = new LdapConfiguration( );
        newCfg.setAuthenticationMethod( ldapConfiguration.getAuthenticationMethod( ) );
        newCfg.setBaseDn( ldapConfiguration.getBaseDn( ) );
        newCfg.setGroupsBaseDn( ldapConfiguration.getBaseGroupsDn() );
        newCfg.setBindDn( ldapConfiguration.getBindDn() );
        newCfg.setBindPassword( ldapConfiguration.getPassword() );
        newCfg.setBindAuthenticatorEnabled( ldapConfiguration.isBindAuthenticatorEnabled() );
        newCfg.setHostName( ldapConfiguration.getHostName( ) );
        newCfg.setPort( ldapConfiguration.getPort( ) );
        newCfg.setProperties( ldapConfiguration.getExtraProperties( ) );
        newCfg.setSslEnabled( ldapConfiguration.isSsl() );
        newCfg.setWritable( ldapConfiguration.isWritable() );
        return newCfg;
    }

    @Schema(description = "The hostname to use to connect to the LDAP server")
    public String getHostName( )
    {
        return hostName;
    }

    public void setHostName( String hostName )
    {
        this.hostName = hostName;
    }

    @Schema(description = "The port to use to connect to the LDAP server")
    public int getPort( )
    {
        return port;
    }

    public void setPort( int port )
    {
        this.port = port;
    }

    @Schema(description = "If SSL should be used for connecting the LDAP server")
    public boolean isSslEnabled( )
    {
        return sslEnabled;
    }

    public void setSslEnabled( boolean sslEnabled )
    {
        this.sslEnabled = sslEnabled;
    }

    @Schema(description = "The BASE DN used for the LDAP server")
    public String getBaseDn( )
    {
        return baseDn;
    }

    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn;
    }

    @Schema(description = "The distinguished name of the bind user which is used to bind to the LDAP server")
    public String getBindDn( )
    {
        return bindDn;
    }

    public void setBindDn( String bindDn )
    {
        this.bindDn = bindDn;
    }

    @Schema(description = "The password used to bind to the ldap server")
    public String getBindPassword( )
    {
        return bindPassword;
    }

    public void setBindPassword( String bindPassword )
    {
        this.bindPassword = bindPassword;
    }

    @Schema(description = "The distinguished name of the base to use for searching group.")
    public String getGroupsBaseDn( )
    {
        return groupsBaseDn;
    }

    public void setGroupsBaseDn( String groupsBaseDn )
    {
        this.groupsBaseDn = groupsBaseDn;
    }

    @Schema(description = "The authentication method used to bind to the LDAP server (PLAINTEXT, SASL, ...)")
    public String getAuthenticationMethod( )
    {
        return authenticationMethod;
    }

    public void setAuthenticationMethod( String authenticationMethod )
    {
        this.authenticationMethod = authenticationMethod;
    }

    @Schema(description = "True, if the LDAP bind authentication is used for logging in to Archiva")
    public boolean isBindAuthenticatorEnabled( )
    {
        return bindAuthenticatorEnabled;
    }

    public void setBindAuthenticatorEnabled( boolean bindAuthenticatorEnabled )
    {
        this.bindAuthenticatorEnabled = bindAuthenticatorEnabled;
    }

    @Schema(description = "True, if the archiva role name is also the LDAP group name")
    public boolean isUseRoleNameAsGroup( )
    {
        return useRoleNameAsGroup;
    }

    public void setUseRoleNameAsGroup( boolean useRoleNameAsGroup )
    {
        this.useRoleNameAsGroup = useRoleNameAsGroup;
    }

    @Schema(description = "Map of additional properties")
    public Map<String, String> getProperties( )
    {
        return properties;
    }

    public void setProperties( Map<String, String> properties )
    {
        this.properties.clear();
        this.properties.putAll( properties );
    }

    @Schema(description = "True, if attributes in the the LDAP server can be edited by Archiva")
    public boolean isWritable( )
    {
        return writable;
    }

    public void setWritable( boolean writable )
    {
        this.writable = writable;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        LdapConfiguration that = (LdapConfiguration) o;

        if ( port != that.port ) return false;
        if ( sslEnabled != that.sslEnabled ) return false;
        if ( bindAuthenticatorEnabled != that.bindAuthenticatorEnabled ) return false;
        if ( useRoleNameAsGroup != that.useRoleNameAsGroup ) return false;
        if ( writable != that.writable ) return false;
        if ( !Objects.equals( hostName, that.hostName ) ) return false;
        if ( !Objects.equals( baseDn, that.baseDn ) ) return false;
        if ( !Objects.equals( bindDn, that.bindDn ) ) return false;
        if ( !Objects.equals( groupsBaseDn, that.groupsBaseDn ) )
            return false;
        if ( !Objects.equals( bindPassword, that.bindPassword ) ) return false;
        if ( !Objects.equals( authenticationMethod, that.authenticationMethod ) )
            return false;
        return properties.equals( that.properties );
    }

    @Override
    public int hashCode( )
    {
        int result = hostName != null ? hostName.hashCode( ) : 0;
        result = 31 * result + port;
        result = 31 * result + ( sslEnabled ? 1 : 0 );
        result = 31 * result + ( baseDn != null ? baseDn.hashCode( ) : 0 );
        result = 31 * result + ( bindDn != null ? bindDn.hashCode( ) : 0 );
        result = 31 * result + ( groupsBaseDn != null ? groupsBaseDn.hashCode( ) : 0 );
        result = 31 * result + ( bindPassword != null ? bindPassword.hashCode( ) : 0 );
        result = 31 * result + ( authenticationMethod != null ? authenticationMethod.hashCode( ) : 0 );
        result = 31 * result + ( bindAuthenticatorEnabled ? 1 : 0 );
        result = 31 * result + ( useRoleNameAsGroup ? 1 : 0 );
        result = 31 * result + properties.hashCode( );
        result = 31 * result + ( writable ? 1 : 0 );
        return result;
    }

    @SuppressWarnings( "StringBufferReplaceableByString" )
    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "LdapConfiguration{" );
        sb.append( "hostName='" ).append( hostName ).append( '\'' );
        sb.append( ", port=" ).append( port );
        sb.append( ", sslEnabled=" ).append( sslEnabled );
        sb.append( ", baseDn='" ).append( baseDn ).append( '\'' );
        sb.append( ", groupsBaseDn='" ).append( groupsBaseDn ).append( '\'' );
        sb.append( ", bindDn='" ).append( bindDn ).append( '\'' );
        sb.append( ", bindPassword='" ).append( bindPassword ).append( '\'' );
        sb.append( ", authenticationMethod='" ).append( authenticationMethod ).append( '\'' );
        sb.append( ", bindAuthenticatorEnabled=" ).append( bindAuthenticatorEnabled );
        sb.append( ", useRoleNameAsGroup=" ).append( useRoleNameAsGroup );
        sb.append( ", properties=" ).append( properties );
        sb.append( ", writable=" ).append( writable );
        sb.append( '}' );
        return sb.toString( );
    }
}
