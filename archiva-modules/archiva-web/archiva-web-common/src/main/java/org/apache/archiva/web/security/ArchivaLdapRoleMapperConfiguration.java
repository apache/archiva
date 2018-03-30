package org.apache.archiva.web.security;
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
import org.apache.archiva.admin.model.beans.LdapGroupMapping;
import org.apache.archiva.admin.model.beans.RedbackRuntimeConfiguration;
import org.apache.archiva.admin.model.runtime.RedbackRuntimeConfigurationAdmin;
import org.apache.archiva.redback.common.ldap.MappingException;
import org.apache.archiva.redback.common.ldap.role.LdapRoleMapperConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 2.1
 */
@Service( "ldapRoleMapperConfiguration#archiva" )
public class ArchivaLdapRoleMapperConfiguration
    implements LdapRoleMapperConfiguration
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "redbackRuntimeConfigurationAdmin#default" )
    private RedbackRuntimeConfigurationAdmin redbackRuntimeConfigurationAdmin;

    @Override
    public void addLdapMapping( String ldapGroup, List<String> roles )
        throws MappingException
    {
        logger.debug( "addLdapMapping ldapGroup: {}, roles: {}", ldapGroup, roles );
        // TODO check if already exist first
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration();
            boolean added =
                redbackRuntimeConfiguration.getLdapGroupMappings().add( new LdapGroupMapping( ldapGroup, roles ) );
            logger.debug( "addLdapMapping ldapGroup: {}, roles: {}, added: {}", ldapGroup, roles, added );
            redbackRuntimeConfigurationAdmin.updateRedbackRuntimeConfiguration( redbackRuntimeConfiguration );
        }
        catch ( RepositoryAdminException e )
        {
            throw new MappingException( e.getMessage(), e );
        }

    }

    @Override
    public void updateLdapMapping( String ldapGroup, List<String> roles )
        throws MappingException
    {

        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration();
            LdapGroupMapping ldapGroupMapping = new LdapGroupMapping( ldapGroup );
            int idx = redbackRuntimeConfiguration.getLdapGroupMappings().indexOf( ldapGroupMapping );
            if ( idx > -1 )
            {
                logger.debug( "updateLdapMapping ldapGroup: {}, roles: {}", ldapGroup, roles );
                ldapGroupMapping = redbackRuntimeConfiguration.getLdapGroupMappings().get( idx );
                ldapGroupMapping.setRoleNames( roles );
            }
            redbackRuntimeConfigurationAdmin.updateRedbackRuntimeConfiguration( redbackRuntimeConfiguration );

        }
        catch ( RepositoryAdminException e )
        {
            throw new MappingException( e.getMessage(), e );
        }
    }

    @Override
    public void removeLdapMapping( String group )
        throws MappingException
    {
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration();
            boolean removed =
                redbackRuntimeConfiguration.getLdapGroupMappings().remove( new LdapGroupMapping( group ) );
            redbackRuntimeConfigurationAdmin.updateRedbackRuntimeConfiguration( redbackRuntimeConfiguration );
            logger.debug( "removeLdapMapping ldapGroup: {}, removed: {}", group, removed );
        }
        catch ( RepositoryAdminException e )
        {
            throw new MappingException( e.getMessage(), e );
        }

    }

    @Override
    public Map<String, Collection<String>> getLdapGroupMappings()
        throws MappingException
    {
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration();

            List<LdapGroupMapping> ldapGroupMappings = redbackRuntimeConfiguration.getLdapGroupMappings();

            if ( ldapGroupMappings == null )
            {
                return Collections.emptyMap();
            }

            Map<String, Collection<String>> res = new HashMap<>( ldapGroupMappings.size() );

            for ( LdapGroupMapping ldapGroupMapping : ldapGroupMappings )
            {
                res.put( ldapGroupMapping.getGroup(), ldapGroupMapping.getRoleNames() );
            }

            return res;
        }
        catch ( RepositoryAdminException e )
        {
            throw new MappingException( e.getMessage(), e );
        }
    }

    @Override
    public void setLdapGroupMappings( Map<String, List<String>> mappings )
        throws MappingException
    {
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration();

            List<LdapGroupMapping> ldapGroupMappings = new ArrayList<>( mappings.size() );

            for ( Map.Entry<String, List<String>> entry : mappings.entrySet() )
            {
                ldapGroupMappings.add( new LdapGroupMapping( entry.getKey(), entry.getValue() ) );
            }

            redbackRuntimeConfiguration.setLdapGroupMappings( ldapGroupMappings );

            redbackRuntimeConfigurationAdmin.updateRedbackRuntimeConfiguration( redbackRuntimeConfiguration );
        }
        catch ( RepositoryAdminException e )
        {
            throw new MappingException( e.getMessage(), e );
        }

    }
}
