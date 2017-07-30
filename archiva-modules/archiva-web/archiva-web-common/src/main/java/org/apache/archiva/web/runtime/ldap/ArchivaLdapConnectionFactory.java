package org.apache.archiva.web.runtime.ldap;
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
import org.apache.archiva.admin.model.runtime.RedbackRuntimeConfigurationAdmin;
import org.apache.archiva.redback.common.ldap.connection.ConfigurableLdapConnectionFactory;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.naming.InvalidNameException;
import java.util.Map;
import java.util.Properties;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
@Service( "ldapConnectionFactory#archiva" )
public class ArchivaLdapConnectionFactory
    extends ConfigurableLdapConnectionFactory
{

    private final Logger log = LoggerFactory.getLogger(ArchivaLdapConnectionFactory.class);

    private boolean valid = false;

    @Inject
    private RedbackRuntimeConfigurationAdmin redbackRuntimeConfigurationAdmin;

    private LdapConnectionConfiguration ldapConnectionConfiguration;

    @PostConstruct
    @Override
    public void initialize()
    {
        try
        {
            LdapConfiguration ldapConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration().getLdapConfiguration();
            ldapConnectionConfiguration = new LdapConnectionConfiguration();
            ldapConnectionConfiguration.setHostname( ldapConfiguration.getHostName() );
            ldapConnectionConfiguration.setPort( ldapConfiguration.getPort() );
            ldapConnectionConfiguration.setSsl( ldapConfiguration.isSsl() );
            ldapConnectionConfiguration.setBaseDn( ldapConfiguration.getBaseDn() );
            ldapConnectionConfiguration.setContextFactory( ldapConfiguration.getContextFactory() );
            ldapConnectionConfiguration.setBindDn( ldapConfiguration.getBindDn() );
            ldapConnectionConfiguration.setPassword( ldapConfiguration.getPassword() );
            ldapConnectionConfiguration.setAuthenticationMethod( ldapConfiguration.getAuthenticationMethod() );
            ldapConnectionConfiguration.setExtraProperties( toProperties( ldapConfiguration.getExtraProperties() ) );
            valid=true;
        }
        catch ( InvalidNameException e )
        {
            log.error( "Error during initialization of LdapConnectionFactory {}", e.getMessage(), e );
            // throw new RuntimeException( "Error while initializing connection factory.", e );
        }
        catch ( RepositoryAdminException e )
        {
            throw new RuntimeException( "Error while initializing ldapConnectionConfiguration: " + e.getMessage(), e );
        }
    }

    private Properties toProperties( Map<String, String> map )
    {
        Properties properties = new Properties();
        if ( map == null )
        {
            return properties;
        }
        for ( Map.Entry<String, String> entry : map.entrySet() )
        {
            properties.put( entry.getKey(), entry.getValue() );
        }
        return properties;
    }

    @Override
    public LdapConnectionConfiguration getLdapConnectionConfiguration()
    {
        return this.ldapConnectionConfiguration;
    }

    @Override
    public void setLdapConnectionConfiguration( LdapConnectionConfiguration ldapConnectionConfiguration )
    {
        this.ldapConnectionConfiguration = ldapConnectionConfiguration;
    }

    @Override
    public boolean isValid() {
        return valid;
    }
}
