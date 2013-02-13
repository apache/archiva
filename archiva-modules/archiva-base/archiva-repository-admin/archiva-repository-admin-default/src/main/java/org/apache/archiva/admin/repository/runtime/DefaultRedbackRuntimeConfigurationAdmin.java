package org.apache.archiva.admin.repository.runtime;
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

import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.CacheConfiguration;
import org.apache.archiva.admin.model.beans.LdapConfiguration;
import org.apache.archiva.admin.model.beans.RedbackRuntimeConfiguration;
import org.apache.archiva.admin.model.runtime.RedbackRuntimeConfigurationAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ConfigurationEvent;
import org.apache.archiva.configuration.ConfigurationListener;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.redback.components.cache.Cache;
import org.apache.archiva.redback.components.registry.RegistryException;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationException;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service( "userConfiguration#archiva" )
public class DefaultRedbackRuntimeConfigurationAdmin
    implements RedbackRuntimeConfigurationAdmin, UserConfiguration, ConfigurationListener
{

    protected Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named( value = "userConfiguration#redback" )
    UserConfiguration userConfiguration;

    @Inject
    @Named( value = "cache#users" )
    private Cache usersCache;

    private RedbackRuntimeConfiguration currentRedbackRuntimeConfiguration;

    @PostConstruct
    public void initialize()
        throws UserConfigurationException
    {
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration = getRedbackRuntimeConfiguration();
            // migrate or not data from redback
            if ( !redbackRuntimeConfiguration.isMigratedFromRedbackConfiguration() )
            {
                // not migrated so build a new fresh one
                redbackRuntimeConfiguration = new RedbackRuntimeConfiguration();
                // so migrate if available
                String userManagerImpl =
                    userConfiguration.getConcatenatedList( UserConfigurationKeys.USER_MANAGER_IMPL, "jdo" );
                if ( StringUtils.isNotEmpty( userManagerImpl ) )
                {
                    String[] impls = StringUtils.split( userManagerImpl, ',' );
                    for ( String impl : impls )
                    {
                        redbackRuntimeConfiguration.getUserManagerImpls().add( impl );
                    }
                }

                String rbacManagerImpls =
                    userConfiguration.getConcatenatedList( UserConfigurationKeys.RBAC_MANAGER_IMPL, "cached" );

                if ( StringUtils.isNotEmpty( rbacManagerImpls ) )
                {
                    String[] impls = StringUtils.split( rbacManagerImpls, ',' );
                    for ( String impl : impls )
                    {
                        redbackRuntimeConfiguration.getRbacManagerImpls().add( impl );
                    }
                }

                // now ldap

                LdapConfiguration ldapConfiguration = redbackRuntimeConfiguration.getLdapConfiguration();
                if ( ldapConfiguration == null )
                {
                    ldapConfiguration = new LdapConfiguration();
                    redbackRuntimeConfiguration.setLdapConfiguration( ldapConfiguration );
                }

                ldapConfiguration.setHostName(
                    userConfiguration.getString( UserConfigurationKeys.LDAP_HOSTNAME, null ) );
                ldapConfiguration.setPort( userConfiguration.getInt( UserConfigurationKeys.LDAP_PORT, -1 ) );
                ldapConfiguration.setSsl( userConfiguration.getBoolean( UserConfigurationKeys.LDAP_SSL, false ) );
                ldapConfiguration.setBaseDn(
                    userConfiguration.getConcatenatedList( UserConfigurationKeys.LDAP_BASEDN, null ) );

                ldapConfiguration.setBaseGroupsDn(
                    userConfiguration.getConcatenatedList( UserConfigurationKeys.LDAP_GROUPS_BASEDN,
                                                           ldapConfiguration.getBaseDn() ) );

                ldapConfiguration.setContextFactory(
                    userConfiguration.getString( UserConfigurationKeys.LDAP_CONTEX_FACTORY, null ) );
                ldapConfiguration.setBindDn(
                    userConfiguration.getConcatenatedList( UserConfigurationKeys.LDAP_BINDDN, null ) );
                ldapConfiguration.setPassword(
                    userConfiguration.getString( UserConfigurationKeys.LDAP_PASSWORD, null ) );
                ldapConfiguration.setAuthenticationMethod(
                    userConfiguration.getString( UserConfigurationKeys.LDAP_AUTHENTICATION_METHOD, null ) );

                ldapConfiguration.setWritable(
                    userConfiguration.getBoolean( UserConfigurationKeys.LDAP_WRITABLE, false ) );

                ldapConfiguration.setUseRoleNameAsGroup(
                    userConfiguration.getBoolean( UserConfigurationKeys.LDAP_GROUPS_USE_ROLENAME, false ) );

                ldapConfiguration.setBindAuthenticatorEnabled(
                    userConfiguration.getBoolean( UserConfigurationKeys.LDAP_BIND_AUTHENTICATOR_ENABLED, false ) );

                redbackRuntimeConfiguration.setMigratedFromRedbackConfiguration( true );

                updateRedbackRuntimeConfiguration( redbackRuntimeConfiguration );

            }
            // we must ensure userManagerImpls list is not empty if so put at least jdo one !
            if ( redbackRuntimeConfiguration.getUserManagerImpls().isEmpty() )
            {
                log.info(
                    "redbackRuntimeConfiguration with empty userManagerImpls so force at least jdo implementation !" );
                redbackRuntimeConfiguration.getUserManagerImpls().add( "jdo" );
                updateRedbackRuntimeConfiguration( redbackRuntimeConfiguration );
            }

            // we ensure authorizerImpls is not empty if so put
            if ( redbackRuntimeConfiguration.getRbacManagerImpls().isEmpty() )
            {
                log.info(
                    "redbackRuntimeConfiguration with empty rbacManagerImpls so force at least cached implementation !" );
                redbackRuntimeConfiguration.getRbacManagerImpls().add( "cached" );
                updateRedbackRuntimeConfiguration( redbackRuntimeConfiguration );
            }

            boolean save = false;

            // NPE free
            if ( redbackRuntimeConfiguration.getUsersCacheConfiguration() == null )
            {
                redbackRuntimeConfiguration.setUsersCacheConfiguration( new CacheConfiguration() );
            }
            // if -1 it means non initialized to take values from the spring bean
            if ( redbackRuntimeConfiguration.getUsersCacheConfiguration().getTimeToIdleSeconds() < 0 )
            {
                redbackRuntimeConfiguration.getUsersCacheConfiguration().setTimeToIdleSeconds(
                    usersCache.getTimeToIdleSeconds() );
                save = true;

            }
            usersCache.setTimeToIdleSeconds(
                redbackRuntimeConfiguration.getUsersCacheConfiguration().getTimeToIdleSeconds() );

            if ( redbackRuntimeConfiguration.getUsersCacheConfiguration().getTimeToLiveSeconds() < 0 )
            {
                redbackRuntimeConfiguration.getUsersCacheConfiguration().setTimeToLiveSeconds(
                    usersCache.getTimeToLiveSeconds() );
                save = true;

            }
            usersCache.setTimeToLiveSeconds(
                redbackRuntimeConfiguration.getUsersCacheConfiguration().getTimeToLiveSeconds() );

            if ( redbackRuntimeConfiguration.getUsersCacheConfiguration().getMaxElementsInMemory() < 0 )
            {
                redbackRuntimeConfiguration.getUsersCacheConfiguration().setMaxElementsInMemory(
                    usersCache.getMaxElementsInMemory() );
                save = true;
            }
            usersCache.setMaxElementsInMemory(
                redbackRuntimeConfiguration.getUsersCacheConfiguration().getMaxElementsInMemory() );

            if ( redbackRuntimeConfiguration.getUsersCacheConfiguration().getMaxElementsOnDisk() < 0 )
            {
                redbackRuntimeConfiguration.getUsersCacheConfiguration().setMaxElementsOnDisk(
                    usersCache.getMaxElementsOnDisk() );
                save = true;
            }
            usersCache.setMaxElementsOnDisk(
                redbackRuntimeConfiguration.getUsersCacheConfiguration().getMaxElementsOnDisk() );

            if ( save )
            {
                updateRedbackRuntimeConfiguration( redbackRuntimeConfiguration );
            }

            this.currentRedbackRuntimeConfiguration = redbackRuntimeConfiguration;

        }
        catch ( RepositoryAdminException e )
        {
            throw new UserConfigurationException( e.getMessage(), e );
        }
    }

    public RedbackRuntimeConfiguration getRedbackRuntimeConfiguration()
    {
        return build( archivaConfiguration.getConfiguration().getRedbackRuntimeConfiguration(), false );
    }

    public void updateRedbackRuntimeConfiguration( RedbackRuntimeConfiguration redbackRuntimeConfiguration )
        throws RepositoryAdminException
    {
        org.apache.archiva.configuration.RedbackRuntimeConfiguration runtimeConfiguration =
            build( redbackRuntimeConfiguration );
        Configuration configuration = archivaConfiguration.getConfiguration();
        configuration.setRedbackRuntimeConfiguration( runtimeConfiguration );
        try
        {
            archivaConfiguration.save( configuration );
            this.currentRedbackRuntimeConfiguration = redbackRuntimeConfiguration;
        }
        catch ( RegistryException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        catch ( IndeterminateConfigurationException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
    }

    private RedbackRuntimeConfiguration build(
        org.apache.archiva.configuration.RedbackRuntimeConfiguration runtimeConfiguration, boolean force )
    {
        if ( this.currentRedbackRuntimeConfiguration != null && !force )
        {
            return this.currentRedbackRuntimeConfiguration;
        }
        RedbackRuntimeConfiguration redbackRuntimeConfiguration =
            new BeanReplicator().replicateBean( runtimeConfiguration, RedbackRuntimeConfiguration.class );

        if ( runtimeConfiguration.getLdapConfiguration() != null )
        {
            redbackRuntimeConfiguration.setLdapConfiguration(
                new BeanReplicator().replicateBean( runtimeConfiguration.getLdapConfiguration(),
                                                    LdapConfiguration.class ) );
        }

        if ( runtimeConfiguration.getUsersCacheConfiguration() != null )
        {
            redbackRuntimeConfiguration.setUsersCacheConfiguration(
                new BeanReplicator().replicateBean( runtimeConfiguration.getUsersCacheConfiguration(),
                                                    CacheConfiguration.class ) );
        }

        if ( redbackRuntimeConfiguration.getLdapConfiguration() == null )
        {
            // prevent NPE
            redbackRuntimeConfiguration.setLdapConfiguration( new LdapConfiguration() );
        }

        if ( redbackRuntimeConfiguration.getUsersCacheConfiguration() == null )
        {
            redbackRuntimeConfiguration.setUsersCacheConfiguration( new CacheConfiguration() );
        }

        cleanupProperties( redbackRuntimeConfiguration );

        this.currentRedbackRuntimeConfiguration = redbackRuntimeConfiguration;

        return redbackRuntimeConfiguration;
    }

    /**
     * cleaning from map properties used directly in archiva configuration fields
     *
     * @param redbackRuntimeConfiguration
     */
    private void cleanupProperties( RedbackRuntimeConfiguration redbackRuntimeConfiguration )
    {
        Map<String, String> properties = redbackRuntimeConfiguration.getConfigurationProperties();
        properties.remove( UserConfigurationKeys.LDAP_HOSTNAME );
        properties.remove( UserConfigurationKeys.LDAP_PORT );
        properties.remove( UserConfigurationKeys.LDAP_BIND_AUTHENTICATOR_ENABLED );
        properties.remove( UserConfigurationKeys.LDAP_SSL );
        properties.remove( UserConfigurationKeys.LDAP_BASEDN );
        properties.remove( UserConfigurationKeys.LDAP_GROUPS_BASEDN );
        properties.remove( UserConfigurationKeys.LDAP_CONTEX_FACTORY );
        properties.remove( UserConfigurationKeys.LDAP_BINDDN );
        properties.remove( UserConfigurationKeys.LDAP_PASSWORD );
        properties.remove( UserConfigurationKeys.LDAP_AUTHENTICATION_METHOD );
        properties.remove( UserConfigurationKeys.LDAP_WRITABLE );
        properties.remove( UserConfigurationKeys.LDAP_GROUPS_USE_ROLENAME );
        // cleanup groups <-> role mapping
        /**for ( Map.Entry<String, String> entry : new HashMap<String, String>( properties ).entrySet() )
         {
         if ( entry.getKey().startsWith( UserConfigurationKeys.LDAP_GROUPS_ROLE_START_KEY ) )
         {
         properties.remove( entry.getKey() );
         }
         }*/
    }

    private org.apache.archiva.configuration.RedbackRuntimeConfiguration build(
        RedbackRuntimeConfiguration archivaRuntimeConfiguration )
    {
        org.apache.archiva.configuration.RedbackRuntimeConfiguration redbackRuntimeConfiguration =
            new BeanReplicator().replicateBean( archivaRuntimeConfiguration,
                                                org.apache.archiva.configuration.RedbackRuntimeConfiguration.class );

        if ( archivaRuntimeConfiguration.getLdapConfiguration() == null )
        {
            archivaRuntimeConfiguration.setLdapConfiguration( new LdapConfiguration() );
        }
        redbackRuntimeConfiguration.setLdapConfiguration(
            new BeanReplicator().replicateBean( archivaRuntimeConfiguration.getLdapConfiguration(),
                                                org.apache.archiva.configuration.LdapConfiguration.class ) );

        if ( archivaRuntimeConfiguration.getUsersCacheConfiguration() == null )
        {
            archivaRuntimeConfiguration.setUsersCacheConfiguration( new CacheConfiguration() );
        }

        redbackRuntimeConfiguration.setUsersCacheConfiguration(
            new BeanReplicator().replicateBean( archivaRuntimeConfiguration.getUsersCacheConfiguration(),
                                                org.apache.archiva.configuration.CacheConfiguration.class ) );

        return redbackRuntimeConfiguration;
    }

    // wrapper for UserConfiguration to intercept values (and store it not yet migrated


    public String getString( String key )
    {
        if ( UserConfigurationKeys.USER_MANAGER_IMPL.equals( key ) )
        {
            // possible false for others than archiva user manager
            return getRedbackRuntimeConfiguration().getUserManagerImpls().get( 0 );
        }

        RedbackRuntimeConfiguration conf = getRedbackRuntimeConfiguration();

        if ( conf.getConfigurationProperties().containsKey( key ) )
        {
            return conf.getConfigurationProperties().get( key );
        }

        String value = userConfiguration.getString( key );
        if ( value == null )
        {
            return null;
        }
        conf.getConfigurationProperties().put( key, value );

        try
        {
            updateRedbackRuntimeConfiguration( conf );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "fail to save RedbackRuntimeConfiguration: {}", e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }

        return value;
    }

    public String getString( String key, String defaultValue )
    {
        if ( UserConfigurationKeys.LDAP_HOSTNAME.equals( key ) )
        {
            return getRedbackRuntimeConfiguration().getLdapConfiguration().getHostName();
        }
        if ( UserConfigurationKeys.LDAP_CONTEX_FACTORY.equals( key ) )
        {
            return getRedbackRuntimeConfiguration().getLdapConfiguration().getContextFactory();
        }
        if ( UserConfigurationKeys.LDAP_PASSWORD.equals( key ) )
        {
            return getRedbackRuntimeConfiguration().getLdapConfiguration().getPassword();
        }
        if ( UserConfigurationKeys.LDAP_AUTHENTICATION_METHOD.equals( key ) )
        {
            return getRedbackRuntimeConfiguration().getLdapConfiguration().getAuthenticationMethod();
        }

        RedbackRuntimeConfiguration conf = getRedbackRuntimeConfiguration();

        if ( conf.getConfigurationProperties().containsKey( key ) )
        {
            return conf.getConfigurationProperties().get( key );
        }

        String value = userConfiguration.getString( key, defaultValue );

        if ( value == null )
        {
            return null;
        }

        conf.getConfigurationProperties().put( key, value );
        try
        {
            updateRedbackRuntimeConfiguration( conf );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "fail to save RedbackRuntimeConfiguration: {}", e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }

        return value;
    }

    public int getInt( String key )
    {
        RedbackRuntimeConfiguration conf = getRedbackRuntimeConfiguration();

        if ( conf.getConfigurationProperties().containsKey( key ) )
        {
            return Integer.valueOf( conf.getConfigurationProperties().get( key ) );
        }

        int value = userConfiguration.getInt( key );

        conf.getConfigurationProperties().put( key, Integer.toString( value ) );
        try
        {
            updateRedbackRuntimeConfiguration( conf );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "fail to save RedbackRuntimeConfiguration: {}", e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }

        return value;
    }

    public int getInt( String key, int defaultValue )
    {
        if ( UserConfigurationKeys.LDAP_PORT.equals( key ) )
        {
            return getRedbackRuntimeConfiguration().getLdapConfiguration().getPort();
        }

        RedbackRuntimeConfiguration conf = getRedbackRuntimeConfiguration();

        if ( conf.getConfigurationProperties().containsKey( key ) )
        {
            return Integer.valueOf( conf.getConfigurationProperties().get( key ) );
        }

        int value = userConfiguration.getInt( key, defaultValue );

        conf.getConfigurationProperties().put( key, Integer.toString( value ) );
        try
        {
            updateRedbackRuntimeConfiguration( conf );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "fail to save RedbackRuntimeConfiguration: {}", e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }

        return value;
    }

    public boolean getBoolean( String key )
    {
        RedbackRuntimeConfiguration conf = getRedbackRuntimeConfiguration();

        if ( UserConfigurationKeys.LDAP_WRITABLE.equals( key ) )
        {
            return conf.getLdapConfiguration().isWritable();
        }

        if ( UserConfigurationKeys.LDAP_GROUPS_USE_ROLENAME.equals( key ) )
        {
            return conf.getLdapConfiguration().isUseRoleNameAsGroup();
        }

        if ( UserConfigurationKeys.LDAP_BIND_AUTHENTICATOR_ENABLED.equals( key ) )
        {
            return conf.getLdapConfiguration().isBindAuthenticatorEnabled();
        }

        if ( conf.getConfigurationProperties().containsKey( key ) )
        {
            return Boolean.valueOf( conf.getConfigurationProperties().get( key ) );
        }

        boolean value = userConfiguration.getBoolean( key );

        conf.getConfigurationProperties().put( key, Boolean.toString( value ) );
        try
        {
            updateRedbackRuntimeConfiguration( conf );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "fail to save RedbackRuntimeConfiguration: {}", e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }

        return value;
    }

    public boolean getBoolean( String key, boolean defaultValue )
    {
        if ( UserConfigurationKeys.LDAP_SSL.equals( key ) )
        {
            return getRedbackRuntimeConfiguration().getLdapConfiguration().isSsl();
        }

        if ( UserConfigurationKeys.LDAP_WRITABLE.equals( key ) )
        {
            return getRedbackRuntimeConfiguration().getLdapConfiguration().isWritable();
        }

        if ( UserConfigurationKeys.LDAP_GROUPS_USE_ROLENAME.equals( key ) )
        {
            return getRedbackRuntimeConfiguration().getLdapConfiguration().isUseRoleNameAsGroup();
        }

        if ( UserConfigurationKeys.LDAP_BIND_AUTHENTICATOR_ENABLED.equals( key ) )
        {
            return getRedbackRuntimeConfiguration().getLdapConfiguration().isBindAuthenticatorEnabled();
        }

        RedbackRuntimeConfiguration conf = getRedbackRuntimeConfiguration();

        if ( conf.getConfigurationProperties().containsKey( key ) )
        {
            return Boolean.valueOf( conf.getConfigurationProperties().get( key ) );
        }

        boolean value = userConfiguration.getBoolean( key, defaultValue );

        conf.getConfigurationProperties().put( key, Boolean.toString( value ) );
        try
        {
            updateRedbackRuntimeConfiguration( conf );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "fail to save RedbackRuntimeConfiguration: {}", e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }

        return value;
    }

    public List<String> getList( String key )
    {
        List<String> value = userConfiguration.getList( key );

        RedbackRuntimeConfiguration conf = getRedbackRuntimeConfiguration();
        // TODO concat values
        conf.getConfigurationProperties().put( key, "" );
        try
        {
            updateRedbackRuntimeConfiguration( conf );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "fail to save RedbackRuntimeConfiguration: {}", e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }

        return value;
    }

    public String getConcatenatedList( String key, String defaultValue )
    {
        if ( UserConfigurationKeys.LDAP_BASEDN.equals( key ) )
        {
            return getRedbackRuntimeConfiguration().getLdapConfiguration().getBaseDn();
        }
        if ( UserConfigurationKeys.LDAP_BINDDN.equals( key ) )
        {
            return getRedbackRuntimeConfiguration().getLdapConfiguration().getBindDn();
        }
        if ( UserConfigurationKeys.LDAP_GROUPS_BASEDN.equals( key ) )
        {
            return getRedbackRuntimeConfiguration().getLdapConfiguration().getBaseGroupsDn();
        }
        return userConfiguration.getConcatenatedList( key, defaultValue );
    }

    public Collection<String> getKeys()
    {
        Collection<String> keys = userConfiguration.getKeys();

        Set<String> keysSet = new HashSet<String>( keys );

        keysSet.addAll( getRedbackRuntimeConfiguration().getConfigurationProperties().keySet() );

        return keysSet;
    }

    public void configurationEvent( ConfigurationEvent event )
    {
        build( archivaConfiguration.getConfiguration().getRedbackRuntimeConfiguration(), true );
    }
}
