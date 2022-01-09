package org.apache.archiva.rest.api.v2.model;/*
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@XmlRootElement(name="ldapConfiguration")
@Schema(name="LdapConfiguration", description = "LDAP configuration attributes")
public class LdapConfiguration implements Serializable, RestModel
{
    private static final long serialVersionUID = -4736767846016398583L;

    private String hostName = "";
    private int port = 389;
    private String baseDn = "";
    private String groupsBaseDn = "";
    private String bindDn = "";
    private String bindPassword = "";
    private String authenticationMethod = "none";
    private String contextFactory;
    private boolean sslEnabled = false;
    private boolean bindAuthenticatorEnabled = true;
    private boolean useRoleNameAsGroup = false;
    private Map<String, String> properties = new TreeMap<>();
    private boolean writable = false;
    private List<String> availableContextFactories;

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
        newCfg.setSslEnabled( ldapConfiguration.isSsl() );
        newCfg.setContextFactory( ldapConfiguration.getContextFactory() );
        if (ldapConfiguration.getPort()<=0) {
            newCfg.setPort( newCfg.isSslEnabled() ? 636 : 389 );
        } else
        {
            newCfg.setPort( ldapConfiguration.getPort( ) );
        }
        newCfg.setProperties( ldapConfiguration.getExtraProperties( ) );
        newCfg.setWritable( ldapConfiguration.isWritable() );
        return newCfg;
    }

    @Schema(name="host_name", description = "The hostname to use to connect to the LDAP server")
    public String getHostName( )
    {
        return hostName;
    }

    public void setHostName( String hostName )
    {
        this.hostName = hostName==null?"":hostName;
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

    @Schema(name="context_factory",description = "The class name of the LDAP context factory")
    public String getContextFactory( )
    {
        return contextFactory;
    }

    public void setContextFactory( String contextFactory )
    {
        this.contextFactory = contextFactory;
    }


    @Schema(name="ssl_enabled", description = "True, if SSL/TLS should be used for connecting the LDAP server")
    public boolean isSslEnabled( )
    {
        return sslEnabled;
    }

    public void setSslEnabled( boolean sslEnabled )
    {
        this.sslEnabled = sslEnabled;
    }

    @Schema(name="base_dn", description = "The BASE DN used for the LDAP server")
    public String getBaseDn( )
    {
        return baseDn;
    }

    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn == null ? "" : baseDn;
    }

    @Schema(name="bind_dn", description = "The distinguished name of the bind user which is used to bind to the LDAP server")
    public String getBindDn( )
    {
        return bindDn;
    }

    public void setBindDn( String bindDn )
    {
        this.bindDn = bindDn == null ? "" : bindDn;
    }

    @Schema(name="bind_password", description = "The password used to bind to the ldap server")
    public String getBindPassword( )
    {
        return bindPassword;
    }

    public void setBindPassword( String bindPassword )
    {
        this.bindPassword = bindPassword==null?"":bindPassword;
    }

    @Schema(name="groups_base_dn", description = "The distinguished name of the base to use for searching group.")
    public String getGroupsBaseDn( )
    {
        return groupsBaseDn;
    }

    public void setGroupsBaseDn( String groupsBaseDn )
    {
        this.groupsBaseDn = groupsBaseDn==null?"":groupsBaseDn;
    }

    @Schema(name="authentication_method", description = "The authentication method used to bind to the LDAP server (PLAINTEXT, SASL, ...)")
    public String getAuthenticationMethod( )
    {
        return authenticationMethod;
    }

    public void setAuthenticationMethod( String authenticationMethod )
    {
        this.authenticationMethod = authenticationMethod==null?"":authenticationMethod;
    }

    @Schema(name="bind_authenticator_enabled", description = "True, if the LDAP bind authentication is used for logging in to Archiva")
    public boolean isBindAuthenticatorEnabled( )
    {
        return bindAuthenticatorEnabled;
    }

    public void setBindAuthenticatorEnabled( boolean bindAuthenticatorEnabled )
    {
        this.bindAuthenticatorEnabled = bindAuthenticatorEnabled;
    }

    @Schema(name="user_role_name_as_group", description = "True, if the archiva role name is also the LDAP group name")
    public boolean isUseRoleNameAsGroup( )
    {
        return useRoleNameAsGroup;
    }

    public void setUseRoleNameAsGroup( boolean useRoleNameAsGroup )
    {
        this.useRoleNameAsGroup = useRoleNameAsGroup;
    }

    @Schema(description = "LDAP ConnectionFactory environment properties")
    public Map<String, String> getProperties( )
    {
        return properties;
    }

    public void setProperties( Map<String, String> properties )
    {
        this.properties = new TreeMap<>( properties  );
    }

    public void addProperty(String key, String value) {
        this.properties.put( key, value );
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

    @Schema(name="available_context_factories", description = "The LDAP context factories that are known and available")
    public List<String> getAvailableContextFactories( )
    {
        return availableContextFactories;
    }

    public void setAvailableContextFactories( List<String> availableContextFactories )
    {
        this.availableContextFactories = new ArrayList<>( availableContextFactories );
    }

    public void addAvailableContextFactory(String contextFactory) {
        if (!this.availableContextFactories.contains( contextFactory ) ) {
            this.availableContextFactories.add( contextFactory );
        }
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
        sb.append( "host_name='" ).append( hostName ).append( '\'' );
        sb.append( ", port=" ).append( port );
        sb.append( ", ssl_enabled=" ).append( sslEnabled );
        sb.append( ", base_dn='" ).append( baseDn ).append( '\'' );
        sb.append( ", groups_base_dn='" ).append( groupsBaseDn ).append( '\'' );
        sb.append( ", bind_dn='" ).append( bindDn ).append( '\'' );
        sb.append( ", bind_password='" ).append( bindPassword ).append( '\'' );
        sb.append( ", authentication_method='" ).append( authenticationMethod ).append( '\'' );
        sb.append( ", bind_authenticator_enabled=" ).append( bindAuthenticatorEnabled );
        sb.append( ", use_role_name_as_group=" ).append( useRoleNameAsGroup );
        sb.append( ", properties=" ).append( properties );
        sb.append( ", writable=" ).append( writable );
        sb.append( '}' );
        return sb.toString( );
    }
}
