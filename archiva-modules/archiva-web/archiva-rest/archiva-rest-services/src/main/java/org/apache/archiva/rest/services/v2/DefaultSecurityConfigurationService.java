package org.apache.archiva.rest.services.v2;/*
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
import org.apache.archiva.components.rest.util.PagingHelper;
import org.apache.archiva.components.rest.util.QueryHelper;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.rest.api.model.UserManagerImplementationInformation;
import org.apache.archiva.rest.api.model.v2.BeanInformation;
import org.apache.archiva.rest.api.model.v2.CacheConfiguration;
import org.apache.archiva.rest.api.model.v2.LdapConfiguration;
import org.apache.archiva.rest.api.model.v2.SecurityConfiguration;
import org.apache.archiva.rest.api.services.v2.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.v2.ErrorMessage;
import org.apache.archiva.rest.api.services.v2.SecurityConfigurationService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.management.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.archiva.rest.services.v2.ErrorKeys.REPOSITORY_ADMIN_ERROR;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service( "v2.defaultSecurityConfigurationService" )
public class DefaultSecurityConfigurationService implements SecurityConfigurationService
{
    private static final Logger log = LoggerFactory.getLogger( DefaultSecurityConfigurationService.class );

    private static final QueryHelper<PropertyEntry> PROP_QUERY_HELPER = new QueryHelper( new String[]{"key"} );
    private static final PagingHelper PROP_PAGING_HELPER = new PagingHelper( );
    static {
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

    @PostConstruct
    void init() {
        bundle = ResourceBundle.getBundle( "org.apache.archiva.rest.RestBundle" );
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
            int totalCount = props.size( );
            List<PropertyEntry> result = props.entrySet( ).stream( ).map(
                entry -> new PropertyEntry( entry.getKey( ), entry.getValue( ) )
            ).filter( filter )
                .sorted( comparator )
                .skip( offset ).limit( limit )
                .collect( Collectors.toList( ) );
            return new PagedResult<>( totalCount, offset, limit, result );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR ) );
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

            return LdapConfiguration.of( redbackRuntimeConfiguration.getLdapConfiguration() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR ) );
        }

    }

    @Override
    public CacheConfiguration getCacheConfiguration( ) throws ArchivaRestServiceException
    {
        try
        {
            RedbackRuntimeConfiguration redbackRuntimeConfiguration =
                redbackRuntimeConfigurationAdmin.getRedbackRuntimeConfiguration( );

            log.debug( "getRedbackRuntimeConfiguration -> {}", redbackRuntimeConfiguration );

            return CacheConfiguration.of( redbackRuntimeConfiguration.getUsersCacheConfiguration() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( REPOSITORY_ADMIN_ERROR ) );
        }

    }

    @Override
    public List<BeanInformation> getAvailableUserManagers( ) throws ArchivaRestServiceException
    {
        Map<String, UserManager> beans = applicationContext.getBeansOfType( UserManager.class );

        if ( beans.isEmpty() )
        {
            return Collections.emptyList();
        }

        return beans.entrySet( ).stream( )
            .filter( entry -> entry.getValue().isFinalImplementation() )
            .map( (Map.Entry<String, UserManager> entry) -> {
                UserManager um = entry.getValue( );
                String id = StringUtils.substringAfter( entry.getKey( ), "#" );
                String displayName = bundle.getString( "user_manager." + id + ".display_name" );
                String description = bundle.getString( "user_manager." + id + ".description" );
                return new BeanInformation( StringUtils.substringAfter( entry.getKey( ), "#" ), displayName, um.getDescriptionKey( ), description, um.isReadOnly( ) );
            } ).collect( Collectors.toList());
    }

    @Override
    public List<BeanInformation> getAvailableRbacManagers( ) throws ArchivaRestServiceException
    {
        Map<String, RBACManager> beans = applicationContext.getBeansOfType( RBACManager.class );

        if ( beans.isEmpty() )
        {
            return Collections.emptyList();
        }

        return beans.entrySet( ).stream( )
            .filter( entry -> entry.getValue().isFinalImplementation() )
            .map( (Map.Entry<String, RBACManager> entry) -> {
                RBACManager rm = entry.getValue( );
                String id = StringUtils.substringAfter( entry.getKey( ), "#" );
                String displayName = bundle.getString( "rbac_manager." + id + ".display_name" );
                String description = bundle.getString( "rbac_manager." + id + ".description" );
                return new BeanInformation( StringUtils.substringAfter( entry.getKey( ), "#" ), displayName, rm.getDescriptionKey( ), description, rm.isReadOnly( ) );
            } ).collect( Collectors.toList());
    }
}
