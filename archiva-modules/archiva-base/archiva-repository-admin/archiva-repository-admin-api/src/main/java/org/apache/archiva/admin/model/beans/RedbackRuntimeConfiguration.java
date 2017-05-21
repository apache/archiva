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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@XmlRootElement(name = "redbackRuntimeConfiguration")
public class RedbackRuntimeConfiguration
    implements Serializable
{

    /**
     * Field userManagerImpls.
     */
    private List<String> userManagerImpls = new ArrayList<>();

    /**
     * Field rbacManagerImpls.
     */
    private java.util.List<String> rbacManagerImpls = new ArrayList<>();

    private LdapConfiguration ldapConfiguration;

    /**
     * flag to know if redback configuration has been checked/migrated.
     */
    private boolean migratedFromRedbackConfiguration = false;

    private Map<String, String> configurationProperties;

    /**
     * field to ease json mapping wrapper on <code>configurationProperties</code> field
     */
    private List<PropertyEntry> configurationPropertiesEntries;

    /**
     * flag to know if redback will use a cache to prevent
     * searching users already found.
     */
    private boolean useUsersCache = true;

    private CacheConfiguration usersCacheConfiguration;

    /**
     * Field ldapGroupMappings.
     */
    private List<LdapGroupMapping> ldapGroupMappings;

    public RedbackRuntimeConfiguration()
    {
        // no op
    }

    public List<String> getUserManagerImpls()
    {
        return userManagerImpls;
    }

    public void setUserManagerImpls( List<String> userManagerImpls )
    {
        this.userManagerImpls = userManagerImpls;
    }

    public LdapConfiguration getLdapConfiguration()
    {
        return ldapConfiguration;
    }

    public void setLdapConfiguration( LdapConfiguration ldapConfiguration )
    {
        this.ldapConfiguration = ldapConfiguration;
    }

    public boolean isMigratedFromRedbackConfiguration()
    {
        return migratedFromRedbackConfiguration;
    }

    public void setMigratedFromRedbackConfiguration( boolean migratedFromRedbackConfiguration )
    {
        this.migratedFromRedbackConfiguration = migratedFromRedbackConfiguration;
    }

    public Map<String, String> getConfigurationProperties()
    {
        if ( this.configurationProperties == null )
        {
            this.configurationProperties = new HashMap<>();
        }
        return configurationProperties;
    }

    public void setConfigurationProperties( Map<String, String> configurationProperties )
    {
        this.configurationProperties = configurationProperties;
    }

    public List<PropertyEntry> getConfigurationPropertiesEntries()
    {
        configurationPropertiesEntries = new ArrayList<PropertyEntry>( getConfigurationProperties().size() );
        for ( Map.Entry<String, String> entry : getConfigurationProperties().entrySet() )
        {
            configurationPropertiesEntries.add( new PropertyEntry( entry.getKey(), entry.getValue() ) );
        }
        Collections.sort( configurationPropertiesEntries );
        return configurationPropertiesEntries;
    }

    public void setConfigurationPropertiesEntries( List<PropertyEntry> configurationPropertiesEntries )
    {
        this.configurationPropertiesEntries = configurationPropertiesEntries;
        if ( configurationPropertiesEntries != null )
        {
            this.configurationProperties = new HashMap<>( configurationPropertiesEntries.size() );
            for ( PropertyEntry propertyEntry : configurationPropertiesEntries )
            {
                this.configurationProperties.put( propertyEntry.getKey(), propertyEntry.getValue() );
            }
        }
    }

    public boolean isUseUsersCache()
    {
        return useUsersCache;
    }

    public void setUseUsersCache( boolean useUsersCache )
    {
        this.useUsersCache = useUsersCache;
    }

    public CacheConfiguration getUsersCacheConfiguration()
    {
        return usersCacheConfiguration;
    }

    public void setUsersCacheConfiguration( CacheConfiguration usersCacheConfiguration )
    {
        this.usersCacheConfiguration = usersCacheConfiguration;
    }

    public List<String> getRbacManagerImpls()
    {
        return rbacManagerImpls;
    }

    public void setRbacManagerImpls( List<String> rbacManagerImpls )
    {
        this.rbacManagerImpls = rbacManagerImpls;
    }

    public List<LdapGroupMapping> getLdapGroupMappings()
    {
        return ldapGroupMappings;
    }

    public void setLdapGroupMappings( List<LdapGroupMapping> ldapGroupMappings )
    {
        this.ldapGroupMappings = ldapGroupMappings;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "RedbackRuntimeConfiguration" );
        sb.append( "{userManagerImpls=" ).append( userManagerImpls );
        sb.append( ", rbacManagerImpls=" ).append( rbacManagerImpls );
        sb.append( ", ldapConfiguration=" ).append( ldapConfiguration );
        sb.append( ", migratedFromRedbackConfiguration=" ).append( migratedFromRedbackConfiguration );
        sb.append( ", configurationProperties=" ).append( configurationProperties );
        sb.append( ", configurationPropertiesEntries=" ).append( configurationPropertiesEntries );
        sb.append( ", useUsersCache=" ).append( useUsersCache );
        sb.append( ", usersCacheConfiguration=" ).append( usersCacheConfiguration );
        sb.append( '}' );
        return sb.toString();
    }
}
