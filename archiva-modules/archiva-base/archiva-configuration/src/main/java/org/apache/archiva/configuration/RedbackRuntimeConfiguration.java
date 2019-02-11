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
 * 
 *         The redback runtime configuration.
 *       
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class RedbackRuntimeConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * flag to know if redback configuration has been
     * checked/migrated.
     */
    private boolean migratedFromRedbackConfiguration = false;

    /**
     * Field userManagerImpls.
     */
    private java.util.List<String> userManagerImpls;

    /**
     * Field rbacManagerImpls.
     */
    private java.util.List<String> rbacManagerImpls;

    /**
     * the ldap configuration.
     */
    private LdapConfiguration ldapConfiguration;

    /**
     * Field ldapGroupMappings.
     */
    private java.util.List<LdapGroupMapping> ldapGroupMappings;

    /**
     * Field configurationProperties.
     */
    private java.util.Map configurationProperties;

    /**
     * flag to know if redback will use a cache to prevent
     * searching users already found.
     */
    private boolean useUsersCache = true;

    /**
     * the users cache configuration.
     */
    private CacheConfiguration usersCacheConfiguration;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addConfigurationProperty.
     * 
     * @param key
     * @param value
     */
    public void addConfigurationProperty( Object key, String value )
    {
        getConfigurationProperties().put( key, value );
    } //-- void addConfigurationProperty( Object, String )

    /**
     * Method addLdapGroupMapping.
     * 
     * @param ldapGroupMapping
     */
    public void addLdapGroupMapping( LdapGroupMapping ldapGroupMapping )
    {
        getLdapGroupMappings().add( ldapGroupMapping );
    } //-- void addLdapGroupMapping( LdapGroupMapping )

    /**
     * Method addRbacManagerImpl.
     * 
     * @param string
     */
    public void addRbacManagerImpl( String string )
    {
        getRbacManagerImpls().add( string );
    } //-- void addRbacManagerImpl( String )

    /**
     * Method addUserManagerImpl.
     * 
     * @param string
     */
    public void addUserManagerImpl( String string )
    {
        getUserManagerImpls().add( string );
    } //-- void addUserManagerImpl( String )

    /**
     * Method getConfigurationProperties.
     * 
     * @return Map
     */
    public java.util.Map getConfigurationProperties()
    {
        if ( this.configurationProperties == null )
        {
            this.configurationProperties = new java.util.HashMap();
        }

        return this.configurationProperties;
    } //-- java.util.Map getConfigurationProperties()

    /**
     * Get the ldap configuration.
     * 
     * @return LdapConfiguration
     */
    public LdapConfiguration getLdapConfiguration()
    {
        return this.ldapConfiguration;
    } //-- LdapConfiguration getLdapConfiguration()

    /**
     * Method getLdapGroupMappings.
     * 
     * @return List
     */
    public java.util.List<LdapGroupMapping> getLdapGroupMappings()
    {
        if ( this.ldapGroupMappings == null )
        {
            this.ldapGroupMappings = new java.util.ArrayList<LdapGroupMapping>();
        }

        return this.ldapGroupMappings;
    } //-- java.util.List<LdapGroupMapping> getLdapGroupMappings()

    /**
     * Method getRbacManagerImpls.
     * 
     * @return List
     */
    public java.util.List<String> getRbacManagerImpls()
    {
        if ( this.rbacManagerImpls == null )
        {
            this.rbacManagerImpls = new java.util.ArrayList<String>();
        }

        return this.rbacManagerImpls;
    } //-- java.util.List<String> getRbacManagerImpls()

    /**
     * Method getUserManagerImpls.
     * 
     * @return List
     */
    public java.util.List<String> getUserManagerImpls()
    {
        if ( this.userManagerImpls == null )
        {
            this.userManagerImpls = new java.util.ArrayList<String>();
        }

        return this.userManagerImpls;
    } //-- java.util.List<String> getUserManagerImpls()

    /**
     * Get the users cache configuration.
     * 
     * @return CacheConfiguration
     */
    public CacheConfiguration getUsersCacheConfiguration()
    {
        return this.usersCacheConfiguration;
    } //-- CacheConfiguration getUsersCacheConfiguration()

    /**
     * Get flag to know if redback configuration has been
     * checked/migrated.
     * 
     * @return boolean
     */
    public boolean isMigratedFromRedbackConfiguration()
    {
        return this.migratedFromRedbackConfiguration;
    } //-- boolean isMigratedFromRedbackConfiguration()

    /**
     * Get flag to know if redback will use a cache to prevent
     * searching users already found.
     * 
     * @return boolean
     */
    public boolean isUseUsersCache()
    {
        return this.useUsersCache;
    } //-- boolean isUseUsersCache()

    /**
     * Method removeLdapGroupMapping.
     * 
     * @param ldapGroupMapping
     */
    public void removeLdapGroupMapping( LdapGroupMapping ldapGroupMapping )
    {
        getLdapGroupMappings().remove( ldapGroupMapping );
    } //-- void removeLdapGroupMapping( LdapGroupMapping )

    /**
     * Method removeRbacManagerImpl.
     * 
     * @param string
     */
    public void removeRbacManagerImpl( String string )
    {
        getRbacManagerImpls().remove( string );
    } //-- void removeRbacManagerImpl( String )

    /**
     * Method removeUserManagerImpl.
     * 
     * @param string
     */
    public void removeUserManagerImpl( String string )
    {
        getUserManagerImpls().remove( string );
    } //-- void removeUserManagerImpl( String )

    /**
     * Set extra properties for redback configuration.
     * String/String.
     * 
     * @param configurationProperties
     */
    public void setConfigurationProperties( java.util.Map configurationProperties )
    {
        this.configurationProperties = configurationProperties;
    } //-- void setConfigurationProperties( java.util.Map )

    /**
     * Set the ldap configuration.
     * 
     * @param ldapConfiguration
     */
    public void setLdapConfiguration( LdapConfiguration ldapConfiguration )
    {
        this.ldapConfiguration = ldapConfiguration;
    } //-- void setLdapConfiguration( LdapConfiguration )

    /**
     * Set ldapGroupMappings.
     * 
     * @param ldapGroupMappings
     */
    public void setLdapGroupMappings( java.util.List<LdapGroupMapping> ldapGroupMappings )
    {
        this.ldapGroupMappings = ldapGroupMappings;
    } //-- void setLdapGroupMappings( java.util.List )

    /**
     * Set flag to know if redback configuration has been
     * checked/migrated.
     * 
     * @param migratedFromRedbackConfiguration
     */
    public void setMigratedFromRedbackConfiguration( boolean migratedFromRedbackConfiguration )
    {
        this.migratedFromRedbackConfiguration = migratedFromRedbackConfiguration;
    } //-- void setMigratedFromRedbackConfiguration( boolean )

    /**
     * Set the RBAC Manager impls to use.
     * 
     * @param rbacManagerImpls
     */
    public void setRbacManagerImpls( java.util.List<String> rbacManagerImpls )
    {
        this.rbacManagerImpls = rbacManagerImpls;
    } //-- void setRbacManagerImpls( java.util.List )

    /**
     * Set flag to know if redback will use a cache to prevent
     * searching users already found.
     * 
     * @param useUsersCache
     */
    public void setUseUsersCache( boolean useUsersCache )
    {
        this.useUsersCache = useUsersCache;
    } //-- void setUseUsersCache( boolean )

    /**
     * Set the user manager impls to use.
     * 
     * @param userManagerImpls
     */
    public void setUserManagerImpls( java.util.List<String> userManagerImpls )
    {
        this.userManagerImpls = userManagerImpls;
    } //-- void setUserManagerImpls( java.util.List )

    /**
     * Set the users cache configuration.
     * 
     * @param usersCacheConfiguration
     */
    public void setUsersCacheConfiguration( CacheConfiguration usersCacheConfiguration )
    {
        this.usersCacheConfiguration = usersCacheConfiguration;
    } //-- void setUsersCacheConfiguration( CacheConfiguration )

}
