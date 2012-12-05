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
import org.apache.archiva.admin.model.beans.ArchivaLdapConfiguration;
import org.apache.archiva.admin.model.beans.ArchivaRuntimeConfiguration;
import org.apache.archiva.admin.model.runtime.ArchivaRuntimeConfigurationAdmin;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.configuration.RedbackRuntimeConfiguration;
import org.apache.archiva.redback.components.registry.RegistryException;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service("archivaRuntimeConfigurationAdmin#default")
public class DefaultArchivaRuntimeConfigurationAdmin
    extends AbstractRepositoryAdmin
    implements ArchivaRuntimeConfigurationAdmin
{

    @Inject
    @Named(value = "userConfiguration#redback")
    UserConfiguration userConfiguration;

    @PostConstruct
    public void initialize()
        throws RepositoryAdminException
    {

        ArchivaRuntimeConfiguration archivaRuntimeConfiguration = getArchivaRuntimeConfiguration();
        // migrate or not data from redback
        if ( !archivaRuntimeConfiguration.isMigratedFromRedbackConfiguration() )
        {
            // so migrate if available
            String userManagerImpl = userConfiguration.getString( UserConfigurationKeys.USER_MANAGER_IMPL );
            if ( StringUtils.isNotEmpty( userManagerImpl ) )
            {
                archivaRuntimeConfiguration.setUserManagerImpl( userManagerImpl );
            }

            // now ldap

            ArchivaLdapConfiguration archivaLdapConfiguration =
                archivaRuntimeConfiguration.getArchivaLdapConfiguration();
            if ( archivaLdapConfiguration == null )
            {
                archivaLdapConfiguration = new ArchivaLdapConfiguration();
                archivaRuntimeConfiguration.setArchivaLdapConfiguration( archivaLdapConfiguration );
            }

            archivaLdapConfiguration.setHostName(
                userConfiguration.getString( UserConfigurationKeys.LDAP_HOSTNAME, null ) );
            archivaLdapConfiguration.setPort( userConfiguration.getInt( UserConfigurationKeys.LDAP_PORT, -1 ) );
            archivaLdapConfiguration.setSsl( userConfiguration.getBoolean( UserConfigurationKeys.LDAP_SSL, false ) );
            archivaLdapConfiguration.setBaseDn( userConfiguration.getConcatenatedList( "ldap.config.base.dn", null ) );
            archivaLdapConfiguration.setContextFactory(
                userConfiguration.getString( UserConfigurationKeys.LDAP_CONTEX_FACTORY, null ) );
            archivaLdapConfiguration.setBindDn( userConfiguration.getConcatenatedList( "ldap.config.bind.dn", null ) );
            archivaLdapConfiguration.setPassword(
                userConfiguration.getString( UserConfigurationKeys.LDAP_PASSWORD, null ) );
            archivaLdapConfiguration.setAuthenticationMethod(
                userConfiguration.getString( UserConfigurationKeys.LDAP_AUTHENTICATION_METHOD, null ) );

            archivaRuntimeConfiguration.setMigratedFromRedbackConfiguration( true );

            updateArchivaRuntimeConfiguration( archivaRuntimeConfiguration );
        }

    }

    public ArchivaRuntimeConfiguration getArchivaRuntimeConfiguration()
        throws RepositoryAdminException
    {
        return build( getArchivaConfiguration().getConfiguration().getRedbackRuntimeConfiguration() );
    }

    public void updateArchivaRuntimeConfiguration( ArchivaRuntimeConfiguration archivaRuntimeConfiguration )
        throws RepositoryAdminException
    {
        RedbackRuntimeConfiguration runtimeConfiguration = build( archivaRuntimeConfiguration );
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        configuration.setRedbackRuntimeConfiguration( runtimeConfiguration );
        try
        {
            getArchivaConfiguration().save( configuration );
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

    private ArchivaRuntimeConfiguration build( RedbackRuntimeConfiguration runtimeConfiguration )
    {
        return new BeanReplicator().replicateBean( runtimeConfiguration, ArchivaRuntimeConfiguration.class );
    }

    private RedbackRuntimeConfiguration build( ArchivaRuntimeConfiguration archivaRuntimeConfiguration )
    {
        return new BeanReplicator().replicateBean( archivaRuntimeConfiguration, RedbackRuntimeConfiguration.class );
    }
}
