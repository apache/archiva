package org.apache.archiva.redback.rest.services;
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

import org.apache.archiva.redback.common.ldap.MappingException;
import org.apache.archiva.redback.common.ldap.connection.DefaultLdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.common.ldap.connection.LdapException;
import org.apache.archiva.redback.common.ldap.role.LdapRoleMapper;
import org.apache.archiva.redback.common.ldap.role.LdapRoleMapperConfiguration;
import org.apache.archiva.redback.rest.api.model.LdapGroupMapping;
import org.apache.archiva.redback.rest.api.model.LdapGroupMappingUpdateRequest;
import org.apache.archiva.redback.rest.api.model.StringList;
import org.apache.archiva.redback.rest.api.services.LdapGroupMappingService;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 2.1
 */
@Service("ldapGroupMappingService#rest")
public class DefaultLdapGroupMappingService
    implements LdapGroupMappingService
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named(value = "ldapRoleMapper#default")
    private LdapRoleMapper ldapRoleMapper;

    @Inject
    @Named(value = "ldapRoleMapperConfiguration#default")
    private LdapRoleMapperConfiguration ldapRoleMapperConfiguration;

    @Inject
    @Named(value = "ldapConnectionFactory#configurable")
    private LdapConnectionFactory ldapConnectionFactory;

    public StringList getLdapGroups()
        throws RedbackServiceException
    {
        DefaultLdapConnection ldapConnection = null;

        DirContext context = null;

        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();
            context = ldapConnection.getDirContext();
            return new StringList( ldapRoleMapper.getAllGroups( context ) );
        }
        catch ( LdapException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        catch ( MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        finally
        {
            closeContext( context );
            closeLdapConnection( ldapConnection );
        }
    }

    public List<LdapGroupMapping> getLdapGroupMappings()
        throws RedbackServiceException
    {
        try
        {
            Map<String, Collection<String>> map = ldapRoleMapperConfiguration.getLdapGroupMappings();
            List<LdapGroupMapping> ldapGroupMappings = new ArrayList<LdapGroupMapping>( map.size() );
            for ( Map.Entry<String, Collection<String>> entry : map.entrySet() )
            {
                LdapGroupMapping ldapGroupMapping = new LdapGroupMapping( entry.getKey(), entry.getValue() );
                ldapGroupMappings.add( ldapGroupMapping );
            }

            return ldapGroupMappings;
        }
        catch ( MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
    }

    public Boolean addLdapGroupMapping( LdapGroupMapping ldapGroupMapping )
        throws RedbackServiceException
    {
        try
        {
            ldapRoleMapperConfiguration.addLdapMapping( ldapGroupMapping.getGroup(),
                                                        new ArrayList( ldapGroupMapping.getRoleNames() ) );
        }
        catch ( MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public Boolean removeLdapGroupMapping( String group )
        throws RedbackServiceException
    {
        try
        {
            ldapRoleMapperConfiguration.removeLdapMapping( group );
        }
        catch ( MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    public Boolean updateLdapGroupMapping( LdapGroupMappingUpdateRequest ldapGroupMappingUpdateRequest )
        throws RedbackServiceException
    {
        try
        {
            for ( LdapGroupMapping ldapGroupMapping : ldapGroupMappingUpdateRequest.getLdapGroupMapping() )
            {
                ldapRoleMapperConfiguration.updateLdapMapping( ldapGroupMapping.getGroup(),
                                                               new ArrayList( ldapGroupMapping.getRoleNames() ) );
            }
        }
        catch ( MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        return Boolean.TRUE;
    }

    //------------------
    // utils
    //------------------

    protected void closeLdapConnection( DefaultLdapConnection ldapConnection )
    {
        if ( ldapConnection != null )
        {
            ldapConnection.close();
        }
    }

    protected void closeContext( DirContext context )
    {
        if ( context != null )
        {
            try
            {
                context.close();
            }
            catch ( NamingException e )
            {
                log.warn( "skip issue closing context: {}", e.getMessage() );
            }
        }
    }
}
