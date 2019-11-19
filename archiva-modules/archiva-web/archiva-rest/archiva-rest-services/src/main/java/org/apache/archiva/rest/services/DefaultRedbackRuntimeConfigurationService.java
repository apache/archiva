package org.apache.archiva.rest.services;
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.LdapConfiguration;
import org.apache.archiva.admin.model.beans.RedbackRuntimeConfiguration;
import org.apache.archiva.admin.model.runtime.RedbackRuntimeConfigurationAdmin;
import org.apache.archiva.redback.authentication.Authenticator;
import org.apache.archiva.redback.common.ldap.connection.LdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionConfiguration;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.common.ldap.connection.LdapException;
import org.apache.archiva.redback.common.ldap.user.LdapUserMapper;
import org.apache.archiva.components.cache.Cache;
import org.apache.archiva.redback.policy.CookieSettings;
import org.apache.archiva.redback.policy.PasswordRule;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.rest.api.model.RBACManagerImplementationInformation;
import org.apache.archiva.rest.api.model.RedbackImplementationsInformations;
import org.apache.archiva.rest.api.model.UserManagerImplementationInformation;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.RedbackRuntimeConfigurationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InvalidNameException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service("redbackRuntimeConfigurationService#rest")
public class DefaultRedbackRuntimeConfigurationService
    extends AbstractRestService
    implements RedbackRuntimeConfigurationService
{

    @Inject
    private RedbackRuntimeConfigurationAdmin redbackRuntimeConfigurationAdmin;

    @Inject
    @Named(value = "userManager#default")
    private UserManager userManager;

    @Inject
    @Named(value = "rbacManager#default")
    private RBACManager rbacManager;

    @Inject
    private RoleManager roleManager;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    @Named(value = "ldapConnectionFactory#configurable")
    private LdapConnectionFactory ldapConnectionFactory;

    @Inject
    @Named(value = "cache#users")
    private Cache usersCache;

    @Inject
    private LdapUserMapper ldapUserMapper;


    @Override
    public RedbackRuntimeConfiguration getRedbackRuntimeConfiguration()
        throws ArchivaRestServiceException
    {
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration();

            log.debug( "getRedbackRuntimeConfiguration -> {}", redbackRuntimeConfiguration );

            return redbackRuntimeConfiguration;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public Boolean updateRedbackRuntimeConfiguration( RedbackRuntimeConfiguration redbackRuntimeConfiguration )
        throws ArchivaRestServiceException
    {
        try
        {
            // has user manager impl changed ?
            boolean userManagerChanged = redbackRuntimeConfiguration.getUserManagerImpls().size()
                != redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration().getUserManagerImpls().size();

            userManagerChanged =
                userManagerChanged || ( redbackRuntimeConfiguration.getUserManagerImpls().toString().hashCode()
                    != redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration().getUserManagerImpls().toString().hashCode() );

            boolean rbacManagerChanged = redbackRuntimeConfiguration.getRbacManagerImpls().size()
                != redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration().getRbacManagerImpls().size();

            rbacManagerChanged =
                rbacManagerChanged || ( redbackRuntimeConfiguration.getRbacManagerImpls().toString().hashCode()
                    != redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration().getRbacManagerImpls().toString().hashCode() );

            boolean ldapConfigured = false;
            for (String um : redbackRuntimeConfiguration.getUserManagerImpls()) {
                if (um.contains("ldap")) {
                    ldapConfigured=true;
                }
            }
            if (!ldapConfigured) {
                for (String rbm : redbackRuntimeConfiguration.getRbacManagerImpls()) {
                    if (rbm.contains("ldap")) {
                        ldapConfigured = true;
                    }
                }
            }

            redbackRuntimeConfigurationAdmin.updateRedbackRuntimeConfiguration( redbackRuntimeConfiguration );

            if ( userManagerChanged )
            {
                log.info( "user managerImpls changed to {} so reload it",
                          redbackRuntimeConfiguration.getUserManagerImpls() );
                userManager.initialize();
            }

            if ( rbacManagerChanged )
            {
                log.info( "rbac manager changed to {} so reload it",
                          redbackRuntimeConfiguration.getRbacManagerImpls() );
                rbacManager.initialize();
                roleManager.initialize();
            }

            if (ldapConfigured) {
                try {
                    ldapConnectionFactory.initialize();
                } catch (Exception e) {
                    ArchivaRestServiceException newEx = new ArchivaRestServiceException(e.getMessage(), e);
                    newEx.setErrorKey("error.ldap.connectionFactory.init.failed");
                    throw newEx;
                }
            }
            Collection<PasswordRule> passwordRules = applicationContext.getBeansOfType( PasswordRule.class ).values();

            for ( PasswordRule passwordRule : passwordRules )
            {
                passwordRule.initialize();
            }

            Collection<CookieSettings> cookieSettingsList =
                applicationContext.getBeansOfType( CookieSettings.class ).values();

            for ( CookieSettings cookieSettings : cookieSettingsList )
            {
                cookieSettings.initialize();
            }

            Collection<Authenticator> authenticators =
                applicationContext.getBeansOfType( Authenticator.class ).values();

            for ( Authenticator authenticator : authenticators )
            {
                try {
                    log.debug("Initializing authenticatior "+authenticator.getId());
                    authenticator.initialize();
                } catch (Exception e) {
                    log.error("Initialization of authenticator failed "+authenticator.getId(),e);
                }
            }

            // users cache
            usersCache.setTimeToIdleSeconds(
                redbackRuntimeConfiguration.getUsersCacheConfiguration().getTimeToIdleSeconds() );
            usersCache.setTimeToLiveSeconds(
                redbackRuntimeConfiguration.getUsersCacheConfiguration().getTimeToLiveSeconds() );
            usersCache.setMaxElementsInMemory(
                redbackRuntimeConfiguration.getUsersCacheConfiguration().getMaxElementsInMemory() );
            usersCache.setMaxElementsOnDisk(
                redbackRuntimeConfiguration.getUsersCacheConfiguration().getMaxElementsOnDisk() );

            if (ldapConfigured) {
                try {
                    ldapUserMapper.initialize();
                } catch (Exception e) {
                    ArchivaRestServiceException newEx = new ArchivaRestServiceException(e.getMessage(), e);
                    newEx.setErrorKey("error.ldap.userMapper.init.failed");
                    throw newEx;
                }
            }




            return Boolean.TRUE;
        }
        catch (ArchivaRestServiceException e) {
            log.error(e.getMessage(), e);
            throw e;
        } catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException(e.getMessage(), e);
        }
    }

    @Override
    public List<UserManagerImplementationInformation> getUserManagerImplementationInformations()
        throws ArchivaRestServiceException
    {

        Map<String, UserManager> beans = applicationContext.getBeansOfType( UserManager.class );

        if ( beans.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<UserManagerImplementationInformation> informations = new ArrayList<>( beans.size() );

        for ( Map.Entry<String, UserManager> entry : beans.entrySet() )
        {
            UserManager userManager = applicationContext.getBean( entry.getKey(), UserManager.class );
            if ( userManager.isFinalImplementation() )
            {
                UserManagerImplementationInformation information = new UserManagerImplementationInformation();
                information.setBeanId( StringUtils.substringAfter( entry.getKey(), "#" ) );
                information.setDescriptionKey( userManager.getDescriptionKey() );
                information.setReadOnly( userManager.isReadOnly() );
                informations.add( information );
            }
        }

        return informations;
    }

    @Override
    public List<RBACManagerImplementationInformation> getRbacManagerImplementationInformations()
        throws ArchivaRestServiceException
    {
        Map<String, RBACManager> beans = applicationContext.getBeansOfType( RBACManager.class );

        if ( beans.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<RBACManagerImplementationInformation> informations = new ArrayList<>( beans.size() );

        for ( Map.Entry<String, RBACManager> entry : beans.entrySet() )
        {
            RBACManager rbacManager = applicationContext.getBean( entry.getKey(), RBACManager.class );
            if ( rbacManager.isFinalImplementation() )
            {
                RBACManagerImplementationInformation information = new RBACManagerImplementationInformation();
                information.setBeanId( StringUtils.substringAfter( entry.getKey(), "#" ) );
                information.setDescriptionKey( rbacManager.getDescriptionKey() );
                information.setReadOnly( rbacManager.isReadOnly() );
                informations.add( information );
            }
        }

        return informations;
    }

    @Override
    public RedbackImplementationsInformations getRedbackImplementationsInformations()
        throws ArchivaRestServiceException
    {
        return new RedbackImplementationsInformations( getUserManagerImplementationInformations(),
                                                       getRbacManagerImplementationInformations() );
    }

    @Override
    public Boolean checkLdapConnection()
        throws ArchivaRestServiceException
    {
        LdapConnection ldapConnection = null;
        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();
        }
        catch ( LdapException e )
        {
            log.warn( "fail to get ldapConnection: {}", e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {

            if ( ldapConnection != null )
            {
                ldapConnection.close();
            }
        }

        return Boolean.TRUE;
    }

    @Override
    public Boolean checkLdapConnection( LdapConfiguration ldapConfiguration )
        throws ArchivaRestServiceException
    {
        LdapConnection ldapConnection = null;
        try
        {
            LdapConnectionConfiguration ldapConnectionConfiguration =
                new LdapConnectionConfiguration( ldapConfiguration.getHostName(), ldapConfiguration.getPort(),
                                                 ldapConfiguration.getBaseDn(), ldapConfiguration.getContextFactory(),
                                                 ldapConfiguration.getBindDn(), ldapConfiguration.getPassword(),
                                                 ldapConfiguration.getAuthenticationMethod(),
                                                 toProperties( ldapConfiguration.getExtraProperties() ) );
            ldapConnectionConfiguration.setSsl( ldapConfiguration.isSsl() );

            ldapConnection = ldapConnectionFactory.getConnection( ldapConnectionConfiguration );

            ldapConnection.close();

            // verify groups dn value too

            ldapConnectionConfiguration =
                new LdapConnectionConfiguration( ldapConfiguration.getHostName(), ldapConfiguration.getPort(),
                                                 ldapConfiguration.getBaseGroupsDn(),
                                                 ldapConfiguration.getContextFactory(), ldapConfiguration.getBindDn(),
                                                 ldapConfiguration.getPassword(),
                                                 ldapConfiguration.getAuthenticationMethod(),
                                                 toProperties( ldapConfiguration.getExtraProperties() ) );

            ldapConnectionConfiguration.setSsl( ldapConfiguration.isSsl() );

            ldapConnection = ldapConnectionFactory.getConnection( ldapConnectionConfiguration );
        }
        catch ( InvalidNameException e )
        {
            log.warn( "fail to get ldapConnection: {}", e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        catch ( LdapException e )
        {
            log.warn( "fail to get ldapConnection: {}", e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {

            if ( ldapConnection != null )
            {
                ldapConnection.close();
            }
        }

        return Boolean.TRUE;
    }

    private Properties toProperties( Map<String, String> map )
    {
        Properties properties = new Properties();
        if ( map == null || map.isEmpty() )
        {
            return properties;
        }
        for ( Map.Entry<String, String> entry : map.entrySet() )
        {
            properties.put( entry.getKey(), entry.getValue() );
        }
        return properties;
    }

}


