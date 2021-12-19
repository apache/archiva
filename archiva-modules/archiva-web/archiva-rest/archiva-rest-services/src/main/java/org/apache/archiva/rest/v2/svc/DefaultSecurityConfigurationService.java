package org.apache.archiva.rest.v2.svc;/*
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.RedbackRuntimeConfiguration;
import org.apache.archiva.admin.model.runtime.RedbackRuntimeConfigurationAdmin;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.model.PropertyEntry;
import org.apache.archiva.components.rest.util.QueryHelper;
import org.apache.archiva.redback.authentication.Authenticator;
import org.apache.archiva.redback.common.ldap.connection.LdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionConfiguration;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.common.ldap.connection.LdapException;
import org.apache.archiva.redback.common.ldap.user.LdapUserMapper;
import org.apache.archiva.redback.policy.CookieSettings;
import org.apache.archiva.redback.policy.PasswordRule;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.rest.api.v2.model.BeanInformation;
import org.apache.archiva.rest.api.v2.model.CacheConfiguration;
import org.apache.archiva.rest.api.v2.model.LdapConfiguration;
import org.apache.archiva.rest.api.v2.model.SecurityConfiguration;
import org.apache.archiva.rest.api.v2.svc.ArchivaRestServiceException;
import org.apache.archiva.rest.api.v2.svc.ErrorKeys;
import org.apache.archiva.rest.api.v2.svc.ErrorMessage;
import org.apache.archiva.rest.api.v2.svc.SecurityConfigurationService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.CommunicationException;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.ServiceUnavailableException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.archiva.rest.api.v2.svc.ErrorKeys.INVALID_RESULT_SET_ERROR;
import static org.apache.archiva.rest.api.v2.svc.ErrorKeys.REPOSITORY_ADMIN_ERROR;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service( "v2.defaultSecurityConfigurationService" )
public class DefaultSecurityConfigurationService implements SecurityConfigurationService
{
    private static final Logger log = LoggerFactory.getLogger( DefaultSecurityConfigurationService.class );

    private static final String[] KNOWN_LDAP_CONTEXT_PROVIDERS = {"com.sun.jndi.ldap.LdapCtxFactory","com.ibm.jndi.LDAPCtxFactory"};
    private final List<String> availableContextProviders = new ArrayList<>( );

    private static final QueryHelper<PropertyEntry> PROP_QUERY_HELPER = new QueryHelper<>( new String[]{"key"} );

    static
    {
        PROP_QUERY_HELPER.addStringFilter( "key", PropertyEntry::getKey );
        PROP_QUERY_HELPER.addStringFilter( "value", PropertyEntry::getValue );
        PROP_QUERY_HELPER.addNullsafeFieldComparator( "key", PropertyEntry::getKey );
        PROP_QUERY_HELPER.addNullsafeFieldComparator( "value", PropertyEntry::getValue );

    }

    private ResourceBundle bundle;


    @Inject
    private RedbackRuntimeConfigurationAdmin redbackRuntimeConfigurationAdmin;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    @Named( value = "userManager#default" )
    private UserManager userManager;

    @Inject
    @Named( value = "rbacManager#default" )
    private RBACManager rbacManager;

    @Inject
    private RoleManager roleManager;

    @Inject
    @Named( value = "ldapConnectionFactory#configurable" )
    private LdapConnectionFactory ldapConnectionFactory;

    @Inject
    private LdapUserMapper ldapUserMapper;


    @PostConstruct
    void init( )
    {
        bundle = ResourceBundle.getBundle( "org.apache.archiva.rest.RestBundle" );
        for (String ldapClass : KNOWN_LDAP_CONTEXT_PROVIDERS) {
            if (isContextFactoryAvailable( ldapClass )) {
                availableContextProviders.add( ldapClass );
            }
        }
    }

    @Override
    public SecurityConfiguration getConfiguration( ) throws ArchivaRestServiceException
    {
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration( );

            log.debug( "getRedbackRuntimeConfiguration -> {}", redbackRuntimeConfiguration );

            return SecurityConfiguration.ofRedbackConfiguration( redbackRuntimeConfiguration );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR ) );
        }
    }

    private void updateConfig( SecurityConfiguration newConfig, RedbackRuntimeConfiguration rbConfig )
    {
        rbConfig.setUserManagerImpls( newConfig.getActiveUserManagers( ) );
        rbConfig.setRbacManagerImpls( newConfig.getActiveRbacManagers( ) );
        rbConfig.setUseUsersCache( newConfig.isUserCacheEnabled( ) );
        Map<String, String> props = rbConfig.getConfigurationProperties( );
        for ( Map.Entry<String, String> newProp : newConfig.getProperties( ).entrySet( ) )
        {
            props.put( newProp.getKey( ), newProp.getValue( ) );
        }
    }

    private void updateConfig( LdapConfiguration newConfig, RedbackRuntimeConfiguration rbConfig )
    {
        org.apache.archiva.admin.model.beans.LdapConfiguration ldapConfig = rbConfig.getLdapConfiguration( );
        ldapConfig.setBaseDn( newConfig.getBaseDn( ) );
        ldapConfig.setAuthenticationMethod( newConfig.getAuthenticationMethod( ) );
        ldapConfig.setBindAuthenticatorEnabled( newConfig.isBindAuthenticatorEnabled( ) );
        ldapConfig.setBindDn( newConfig.getBindDn( ) );
        ldapConfig.setSsl( newConfig.isSslEnabled( ) );
        ldapConfig.setBaseGroupsDn( newConfig.getGroupsBaseDn( ) );
        ldapConfig.setHostName( newConfig.getHostName( ) );
        ldapConfig.setPort( newConfig.getPort( ) );
        ldapConfig.setPassword( newConfig.getBindPassword( ) );
        ldapConfig.setUseRoleNameAsGroup( newConfig.isUseRoleNameAsGroup( ) );
        ldapConfig.setWritable( newConfig.isWritable( ) );
        ldapConfig.setContextFactory( newConfig.getContextFactory( ) );

        Map<String, String> props = ldapConfig.getExtraProperties( );
        for ( Map.Entry<String, String> newProp : newConfig.getProperties( ).entrySet( ) )
        {
            props.put( newProp.getKey( ), newProp.getValue( ) );
        }
    }

    private void updateConfig( CacheConfiguration newConfig, RedbackRuntimeConfiguration rbConfig )
    {
        org.apache.archiva.admin.model.beans.CacheConfiguration cacheConfig = rbConfig.getUsersCacheConfiguration( );
        cacheConfig.setMaxElementsInMemory( newConfig.getMaxEntriesInMemory( ) );
        cacheConfig.setMaxElementsOnDisk( newConfig.getMaxEntriesOnDisk( ) );
        cacheConfig.setTimeToLiveSeconds( newConfig.getTimeToLiveSeconds( ) );
        cacheConfig.setTimeToIdleSeconds( newConfig.getTimeToIdleSeconds( ) );
    }

    @Override
    public SecurityConfiguration updateConfiguration( SecurityConfiguration newConfiguration ) throws ArchivaRestServiceException
    {
        if ( newConfiguration == null )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.MISSING_DATA ), 400 );
        }
        try
        {
            RedbackRuntimeConfiguration conf = redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration( );
            boolean userManagerChanged = !CollectionUtils.isEqualCollection( newConfiguration.getActiveUserManagers( ), conf.getUserManagerImpls( ) );
            boolean rbacManagerChanged = !CollectionUtils.isEqualCollection( newConfiguration.getActiveRbacManagers( ), conf.getRbacManagerImpls( ) );

            boolean ldapConfigured = newConfiguration.getActiveUserManagers( ).stream( ).anyMatch( um -> um.contains( "ldap" ) );
            if ( !ldapConfigured )
            {
                ldapConfigured= newConfiguration.getActiveRbacManagers( ).stream( ).anyMatch( um -> um.contains( "ldap" ) );
            }

            updateConfig( newConfiguration, conf );
            redbackRuntimeConfigurationAdmin.updateRedbackRuntimeConfiguration( conf );

            if ( userManagerChanged )
            {
                log.info( "user managerImpls changed to {} so reload it",
                    newConfiguration.getActiveUserManagers( ) );
                userManager.initialize( );
            }

            if ( rbacManagerChanged )
            {
                log.info( "rbac manager changed to {} so reload it",
                    newConfiguration.getActiveRbacManagers( ) );
                rbacManager.initialize( );
                roleManager.initialize( );
            }

            if ( ldapConfigured )
            {
                try
                {
                    ldapConnectionFactory.initialize( );
                }
                catch ( Exception e )
                {
                    log.error( "Could not initialize LDAP connection factory: {}", e.getMessage( ) );
                    throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.LDAP_CF_INIT_FAILED, e.getMessage( ) ) );
                }
            }
            Collection<PasswordRule> passwordRules = applicationContext.getBeansOfType( PasswordRule.class ).values( );

            for ( PasswordRule passwordRule : passwordRules )
            {
                passwordRule.initialize( );
            }

            Collection<CookieSettings> cookieSettingsList =
                applicationContext.getBeansOfType( CookieSettings.class ).values( );

            for ( CookieSettings cookieSettings : cookieSettingsList )
            {
                cookieSettings.initialize( );
            }

            Collection<Authenticator> authenticators =
                applicationContext.getBeansOfType( Authenticator.class ).values( );

            for ( Authenticator authenticator : authenticators )
            {
                try
                {
                    log.debug( "Initializing authenticatior " + authenticator.getId( ) );
                    authenticator.initialize( );
                }
                catch ( Exception e )
                {
                    log.error( "Initialization of authenticator failed " + authenticator.getId( ), e );
                }
            }

            if ( ldapConfigured )
            {
                try
                {
                    ldapUserMapper.initialize( );
                }
                catch ( Exception e )
                {
                    throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.LDAP_USER_MAPPER_INIT_FAILED, e.getMessage( ) ) );
                }
            }
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR, e.getMessage( ) ) );
        }
        try
        {
            return SecurityConfiguration.ofRedbackConfiguration( redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration( ) );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "Error while retrieve updated configuration: {}", e.getMessage( ) );
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR, e.getMessage( ) ) );
        }
    }

    @Override
    public PagedResult<PropertyEntry> getConfigurationProperties( String searchTerm, Integer offset, Integer limit, List<String> orderBy, String order ) throws ArchivaRestServiceException
    {
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration( );

            log.debug( "getRedbackRuntimeConfiguration -> {}", redbackRuntimeConfiguration );

            boolean ascending = PROP_QUERY_HELPER.isAscending( order );
            Predicate<PropertyEntry> filter = PROP_QUERY_HELPER.getQueryFilter( searchTerm );
            Comparator<PropertyEntry> comparator = PROP_QUERY_HELPER.getComparator( orderBy, ascending );
            Map<String, String> props = redbackRuntimeConfiguration.getConfigurationProperties( );
            int totalCount = Math.toIntExact( props.entrySet( ).stream( ).map(
                entry -> new PropertyEntry( entry.getKey( ), entry.getValue( ) )
            ).filter( filter ).count( ) );
            List<PropertyEntry> result = props.entrySet( ).stream( ).map(
                entry -> new PropertyEntry( entry.getKey( ), entry.getValue( ) )
            ).filter( filter )
                .sorted( comparator )
                .skip( offset ).limit( limit )
                .collect( Collectors.toList( ) );
            return new PagedResult<>( totalCount, offset, limit, result );
        } catch (ArithmeticException e) {
            log.error( "The total count of the result properties is higher than max integer value!" );
            throw new ArchivaRestServiceException( ErrorMessage.of( INVALID_RESULT_SET_ERROR ) );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR ) );
        }
    }

    @Override
    public PropertyEntry getConfigurationProperty( String propertyName ) throws ArchivaRestServiceException
    {
        try
        {
            RedbackRuntimeConfiguration conf = redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration( );
            if ( conf.getConfigurationProperties( ).containsKey( propertyName ) )
            {
                String value = conf.getConfigurationProperties( ).get( propertyName );
                return new PropertyEntry( propertyName, value );
            }
            else
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.PROPERTY_NOT_FOUND ), 404 );
            }

        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR, e.getMessage( ) ) );
        }

    }

    @Override
    public Response updateConfigurationProperty( String propertyName, PropertyEntry propertyValue ) throws ArchivaRestServiceException
    {
        if ( propertyValue == null )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.MISSING_DATA ), 400 );
        }
        try
        {
            RedbackRuntimeConfiguration conf = redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration( );
            if ( conf.getConfigurationProperties( ).containsKey( propertyName ) )
            {
                conf.getConfigurationProperties( ).put( propertyName, propertyValue.getValue( ) );
                redbackRuntimeConfigurationAdmin.updateRedbackRuntimeConfiguration( conf );
                return Response.ok( ).build( );
            }
            else
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.PROPERTY_NOT_FOUND ), 404 );
            }

        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR, e.getMessage( ) ) );
        }
    }

    @Override
    public LdapConfiguration getLdapConfiguration( ) throws ArchivaRestServiceException
    {
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration( );

            log.debug( "getRedbackRuntimeConfiguration -> {}", redbackRuntimeConfiguration );

            LdapConfiguration ldapConfig = LdapConfiguration.of( redbackRuntimeConfiguration.getLdapConfiguration( ) );
            ldapConfig.setAvailableContextFactories( availableContextProviders );
            return ldapConfig;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR ) );
        }

    }

    @Override
    public LdapConfiguration updateLdapConfiguration( LdapConfiguration configuration ) throws ArchivaRestServiceException
    {
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration( );

            log.debug( "getRedbackRuntimeConfiguration -> {}", redbackRuntimeConfiguration );

            updateConfig( configuration, redbackRuntimeConfiguration );

            redbackRuntimeConfigurationAdmin.updateRedbackRuntimeConfiguration( redbackRuntimeConfiguration );
            ldapConnectionFactory.initialize( );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR ) );
        }

        try
        {
            return LdapConfiguration.of( redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration( ).getLdapConfiguration() );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "Error while retrieve updated configuration: {}", e.getMessage( ) );
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR, e.getMessage( ) ) );
        }

    }

    static Properties toProperties( Map<String, String> values )
    {
        Properties result = new Properties( );
        for ( Map.Entry<String, String> entry : values.entrySet( ) )
        {
            result.setProperty( entry.getKey( ), entry.getValue( ) );
        }
        return result;
    }

    private static boolean isContextFactoryAvailable( final String factoryClass)
    {
        try
        {
            return Thread.currentThread().getContextClassLoader().loadClass( factoryClass )
                != null;
        }
        catch ( ClassNotFoundException e )
        {
            return false;
        }
    }


    @Override
    public Response verifyLdapConfiguration( LdapConfiguration ldapConfiguration ) throws ArchivaRestServiceException
    {
        LdapConnection ldapConnection = null;
        try
        {
            LdapConnectionConfiguration ldapConnectionConfiguration =
                new LdapConnectionConfiguration( ldapConfiguration.getHostName( ), ldapConfiguration.getPort( ),
                    ldapConfiguration.getBaseDn( ), ldapConfiguration.getContextFactory( ),
                    ldapConfiguration.getBindDn( ), ldapConfiguration.getBindPassword( ),
                    ldapConfiguration.getAuthenticationMethod( ),
                    toProperties( ldapConfiguration.getProperties( ) ) );
            ldapConnectionConfiguration.setSsl( ldapConfiguration.isSslEnabled( ) );

            ldapConnection = ldapConnectionFactory.getConnection( ldapConnectionConfiguration );
        }
        catch ( InvalidNameException e )
        {
            log.warn( "LDAP connection check failed with invalid name : {}", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.LDAP_INVALID_NAME, e.getMessage( ) ), 400 );
        }
        catch ( LdapException e )
        {
            handleLdapException( e );
        }
        finally
        {
            if ( ldapConnection != null )
            {
                try
                {
                    ldapConnection.close( );
                }
                catch ( NamingException e )
                {
                    log.error( "Could not close connection: {}", e.getMessage( ) );
                }
            }
            ldapConnection = null;
        }

        try
        {
            // verify groups dn value too

            LdapConnectionConfiguration ldapConnectionConfiguration = new LdapConnectionConfiguration( ldapConfiguration.getHostName( ), ldapConfiguration.getPort( ),
                ldapConfiguration.getGroupsBaseDn( ),
                ldapConfiguration.getContextFactory( ), ldapConfiguration.getBindDn( ),
                ldapConfiguration.getBindPassword( ),
                ldapConfiguration.getAuthenticationMethod( ),
                toProperties( ldapConfiguration.getProperties( ) ) );

            ldapConnectionConfiguration.setSsl( ldapConfiguration.isSslEnabled( ) );

            ldapConnection = ldapConnectionFactory.getConnection( ldapConnectionConfiguration );
        }
        catch ( InvalidNameException e )
        {
            log.warn( "LDAP connection check failed with invalid name : {}", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.LDAP_INVALID_NAME, e.getMessage( ) ), 400 );
        }
        catch ( LdapException e )
        {
            handleLdapException( e );
        }
        finally
        {
            if ( ldapConnection != null )
            {
                try
                {
                    ldapConnection.close( );
                }
                catch ( NamingException e )
                {
                    log.error( "Could not close connection: {}", e.getMessage( ), e );
                }
            }
        }

        return Response.ok( ).build( );
    }

    private void handleLdapException( LdapException e ) throws ArchivaRestServiceException
    {
        Throwable rootCause = e.getRootCause( );
        if ( rootCause instanceof CommunicationException )
        {
            log.warn( "LDAP connection check failed with CommunicationException: {}", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.LDAP_COMMUNICATION_ERROR, e.getMessage( ) ), 400 );
        } else if (rootCause instanceof ServiceUnavailableException ) {
            log.warn( "LDAP connection check failed with ServiceUnavailableException: {}", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.LDAP_SERVICE_UNAVAILABLE, e.getMessage( ) ), 400 );
        } else if (rootCause instanceof AuthenticationException ) {
            log.warn( "LDAP connection check failed with AuthenticationException: {}", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.LDAP_SERVICE_AUTHENTICATION_FAILED, e.getMessage( ) ), 400 );
        } else if (rootCause instanceof AuthenticationNotSupportedException ) {
            log.warn( "LDAP connection check failed with AuthenticationNotSupportedException: {}", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.LDAP_SERVICE_AUTHENTICATION_NOT_SUPPORTED, e.getMessage( ) ), 400 );
        } else if (rootCause instanceof NoPermissionException ) {
            log.warn( "LDAP connection check failed with NoPermissionException: {}", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.LDAP_SERVICE_NO_PERMISSION, e.getMessage( ) ), 400 );
        }
        log.warn( "LDAP connection check failed: {} - {}", e.getClass().getName(), e.getMessage( ), e );
        throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.LDAP_GENERIC_ERROR, e.getMessage( ) ), 400 );
    }

    @Override
    public CacheConfiguration getCacheConfiguration( ) throws ArchivaRestServiceException
    {
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration( );

            log.debug( "getRedbackRuntimeConfiguration -> {}", redbackRuntimeConfiguration );

            return CacheConfiguration.of( redbackRuntimeConfiguration.getUsersCacheConfiguration( ) );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR ) );
        }

    }

    @Override
    public CacheConfiguration updateCacheConfiguration( CacheConfiguration cacheConfiguration ) throws ArchivaRestServiceException
    {
        if ( cacheConfiguration == null )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.MISSING_DATA ), 400 );
        }
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration( );

            log.debug( "getRedbackRuntimeConfiguration -> {}", redbackRuntimeConfiguration );
            updateConfig( cacheConfiguration, redbackRuntimeConfiguration );
            redbackRuntimeConfigurationAdmin.updateRedbackRuntimeConfiguration( redbackRuntimeConfiguration );
            return getCacheConfiguration( );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR ) );
        }
    }

    @Override
    public List<BeanInformation> getAvailableUserManagers( )
    {
        Map<String, UserManager> beans = applicationContext.getBeansOfType( UserManager.class );

        if ( beans.isEmpty( ) )
        {
            return Collections.emptyList( );
        }

        return beans.entrySet( ).stream( )
            .filter( entry -> entry.getValue( ).isFinalImplementation( ) )
            .map( ( Map.Entry<String, UserManager> entry ) -> {
                UserManager um = entry.getValue( );
                String id = StringUtils.substringAfter( entry.getKey( ), "#" );
                String displayName = bundle.getString( "user_manager." + id + ".display_name" );
                String description = bundle.getString( "user_manager." + id + ".description" );
                return new BeanInformation( StringUtils.substringAfter( entry.getKey( ), "#" ), displayName, um.getDescriptionKey( ), description, um.isReadOnly( ) );
            } ).collect( Collectors.toList( ) );
    }

    @Override
    public List<BeanInformation> getAvailableRbacManagers( )
    {
        Map<String, RBACManager> beans = applicationContext.getBeansOfType( RBACManager.class );

        if ( beans.isEmpty( ) )
        {
            return Collections.emptyList( );
        }

        return beans.entrySet( ).stream( )
            .filter( entry -> entry.getValue( ).isFinalImplementation( ) )
            .map( ( Map.Entry<String, RBACManager> entry ) -> {
                RBACManager rm = entry.getValue( );
                String id = StringUtils.substringAfter( entry.getKey( ), "#" );
                String displayName = bundle.getString( "rbac_manager." + id + ".display_name" );
                String description = bundle.getString( "rbac_manager." + id + ".description" );
                return new BeanInformation( StringUtils.substringAfter( entry.getKey( ), "#" ), displayName, rm.getDescriptionKey( ), description, rm.isReadOnly( ) );
            } ).collect( Collectors.toList( ) );
    }
}
