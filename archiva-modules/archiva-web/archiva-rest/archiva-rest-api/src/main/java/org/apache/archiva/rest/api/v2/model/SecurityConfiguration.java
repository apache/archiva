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
import org.apache.archiva.admin.model.beans.RedbackRuntimeConfiguration;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name = "securityConfiguration")
@Schema(name = "SecurityConfiguration", description = "Security configuration attributes.")
public class SecurityConfiguration implements Serializable, RestModel
{
    private static final long serialVersionUID = -4186866365979053029L;

    private List<String> activeUserManagers = new ArrayList<>(  );
    private List<String> activeRbacManagers = new ArrayList<>(  );
    private Map<String,String> properties = new TreeMap<>(  );
    private boolean userCacheEnabled=false;
    private boolean ldapActive=false;

    public SecurityConfiguration() {

    }

    public static SecurityConfiguration ofRedbackConfiguration( RedbackRuntimeConfiguration configuration ) {
        SecurityConfiguration secConfig = new SecurityConfiguration( );
        secConfig.setActiveRbacManagers( configuration.getRbacManagerImpls() );
        secConfig.setActiveUserManagers( configuration.getUserManagerImpls() );
        secConfig.setProperties( configuration.getConfigurationProperties() );
        boolean rbLdapActive = configuration.getUserManagerImpls( ).stream( ).anyMatch( um -> um.contains( "ldap" ) );
        secConfig.setLdapActive( rbLdapActive );
        secConfig.setUserCacheEnabled( configuration.isUseUsersCache() );
        return secConfig;
    }

    @Schema(name="active_user_managers", description = "List of ids of the active user managers")
    public List<String> getActiveUserManagers( )
    {
        return activeUserManagers;
    }

    public void setActiveUserManagers( List<String> activeUserManagers )
    {
        this.activeUserManagers = new ArrayList<>( activeUserManagers );
    }

    public void addSelectedUserManager(String userManager) {
        this.activeUserManagers.add( userManager );
    }

    @Schema(name="active_rbac_managers", description = "List of ids of the active rbac managers")
    public List<String> getActiveRbacManagers( )
    {
        return activeRbacManagers;
    }

    public void setActiveRbacManagers( List<String> activeRbacManagers )
    {
        this.activeRbacManagers = new ArrayList<>( activeRbacManagers );
    }

    public void addSelectedRbacManager(String rbacManager) {
        this.activeRbacManagers.add( rbacManager );
    }

    @Schema(description = "Map of all security properties")
    public Map<String, String> getProperties( )
    {
        return properties;
    }

    public void setProperties( Map<String, String> properties )
    {
        this.properties = new TreeMap<>( properties );
    }

    @Schema(name="user_cache_enabled", description = "True, if the user cache is active. It caches data from user backend.")
    public boolean isUserCacheEnabled( )
    {
        return userCacheEnabled;
    }

    public void setUserCacheEnabled( boolean userCacheEnabled )
    {
        this.userCacheEnabled = userCacheEnabled;
    }

    @Schema(name="ldap_active", description = "True, if LDAP is used as user manager")
    public boolean isLdapActive( )
    {
        return ldapActive;
    }

    public void setLdapActive( boolean ldapActive )
    {
        this.ldapActive = ldapActive;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        SecurityConfiguration that = (SecurityConfiguration) o;

        if ( userCacheEnabled != that.userCacheEnabled ) return false;
        if ( ldapActive != that.ldapActive ) return false;
        if ( !activeUserManagers.equals( that.activeUserManagers ) ) return false;
        if ( !activeRbacManagers.equals( that.activeRbacManagers ) ) return false;
        return properties.equals( that.properties );
    }

    @Override
    public int hashCode( )
    {
        int result = activeUserManagers.hashCode( );
        result = 31 * result + activeRbacManagers.hashCode( );
        result = 31 * result + properties.hashCode( );
        result = 31 * result + ( userCacheEnabled ? 1 : 0 );
        result = 31 * result + ( ldapActive ? 1 : 0 );
        return result;
    }

    @SuppressWarnings( "StringBufferReplaceableByString" )
    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "SecurityConfiguration{" );
        sb.append( "active_user_managers=" ).append( activeUserManagers );
        sb.append( ", active_rbac_managers=" ).append( activeRbacManagers );
        sb.append( ", properties=" ).append( properties );
        sb.append( ", user_cache_enabled=" ).append( userCacheEnabled );
        sb.append( ", ldap_active=" ).append( ldapActive );
        sb.append( '}' );
        return sb.toString( );
    }
}
