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

import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.util.QueryHelper;
import org.apache.archiva.configuration.model.RepositoryGroupConfiguration;
import org.apache.archiva.repository.EditableRepositoryGroup;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.validation.CheckedResult;
import org.apache.archiva.repository.validation.ValidationError;
import org.apache.archiva.rest.api.v2.model.MergeConfiguration;
import org.apache.archiva.rest.api.v2.model.RepositoryGroup;
import org.apache.archiva.rest.api.v2.svc.ArchivaRestServiceException;
import org.apache.archiva.rest.api.v2.svc.ErrorKeys;
import org.apache.archiva.rest.api.v2.svc.ErrorMessage;
import org.apache.archiva.rest.api.v2.svc.RepositoryGroupService;
import org.apache.archiva.rest.api.v2.svc.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * REST V2 Implementation for repository groups.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @see RepositoryGroupService
 * @since 3.0
 */
@Service( "v2.repositoryGroupService#rest" )
public class DefaultRepositoryGroupService implements RepositoryGroupService
{
    private final ConfigurationHandler configurationHandler;

    @Context
    HttpServletResponse httpServletResponse;

    @Context
    UriInfo uriInfo;

    final private RepositoryRegistry repositoryRegistry;


    private static final Logger log = LoggerFactory.getLogger( DefaultRepositoryGroupService.class );
    private static final QueryHelper<org.apache.archiva.repository.RepositoryGroup> QUERY_HELPER = new QueryHelper<>( new String[]{"id"} );

    static
    {
        QUERY_HELPER.addStringFilter( "id", org.apache.archiva.repository.RepositoryGroup::getId );
        QUERY_HELPER.addNullsafeFieldComparator( "id", org.apache.archiva.repository.RepositoryGroup::getId );
    }


    public DefaultRepositoryGroupService( RepositoryRegistry repositoryRegistry, ConfigurationHandler configurationHandler )
    {
        this.repositoryRegistry = repositoryRegistry;
        this.configurationHandler = configurationHandler;
    }

    @Override
    public PagedResult<RepositoryGroup> getRepositoriesGroups( String searchTerm, Integer offset, Integer limit, List<String> orderBy, String order ) throws ArchivaRestServiceException
    {
        try
        {
            Predicate<org.apache.archiva.repository.RepositoryGroup> filter = QUERY_HELPER.getQueryFilter( searchTerm );
            Comparator<org.apache.archiva.repository.RepositoryGroup> ordering = QUERY_HELPER.getComparator( orderBy, QUERY_HELPER.isAscending( order ) );
            int totalCount = Math.toIntExact( repositoryRegistry.getRepositoryGroups( ).stream( ).filter( filter ).count( ) );
            List<RepositoryGroup> result = repositoryRegistry.getRepositoryGroups( ).stream( ).filter( filter ).sorted( ordering ).skip( offset ).limit( limit ).map(
                RepositoryGroup::of
            ).collect( Collectors.toList( ) );
            return new PagedResult<>( totalCount, offset, limit, result );
        }
        catch ( ArithmeticException e )
        {
            log.error( "Could not convert total count: {}", e.getMessage( ) );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.INVALID_RESULT_SET_ERROR ) );
        }

    }

    @Override
    public RepositoryGroup getRepositoryGroup( String repositoryGroupId ) throws ArchivaRestServiceException
    {
        if ( StringUtils.isEmpty( repositoryGroupId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_FOUND, "" ), 404 );
        }
        org.apache.archiva.repository.RepositoryGroup group = repositoryRegistry.getRepositoryGroup( repositoryGroupId );
        return RepositoryGroup.of( group );
    }

    private RepositoryGroupConfiguration toConfig( RepositoryGroup group )
    {
        RepositoryGroupConfiguration result = new RepositoryGroupConfiguration( );
        result.setId( group.getId( ) );
        result.setName( group.getName() );
        result.setLocation( group.getLocation( ) );
        result.setRepositories( group.getRepositories( ) );
        MergeConfiguration mergeConfig = group.getMergeConfiguration( );
        if ( mergeConfig != null )
        {
            result.setMergedIndexPath( mergeConfig.getMergedIndexPath( ) );
            result.setMergedIndexTtl( mergeConfig.getMergedIndexTtlMinutes( ) );
            result.setCronExpression( mergeConfig.getIndexMergeSchedule( ) );
        }
        return result;
    }

    @Override
    public RepositoryGroup addRepositoryGroup( RepositoryGroup repositoryGroup ) throws ArchivaRestServiceException
    {
        final String groupId = repositoryGroup.getId( );
        if ( StringUtils.isEmpty( groupId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_INVALID_ID, groupId ), 422 );
        }
        if ( repositoryRegistry.hasRepositoryGroup( groupId ) )
        {
            httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePathBuilder( ).path( groupId ).build( ).toString( ) );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_ID_EXISTS, groupId ), 303 );
        }
        try
        {

            RepositoryGroupConfiguration configuration = toConfig( repositoryGroup );
            CheckedResult<org.apache.archiva.repository.RepositoryGroup, Map<String, List<ValidationError>>> validationResult = repositoryRegistry.putRepositoryGroupAndValidate( configuration );
            if ( validationResult.isValid( ) )
            {
                httpServletResponse.setStatus( 201 );
                return RepositoryGroup.of( validationResult.getRepository( ) );
            }
            else
            {
                throw ValidationException.of( validationResult.getResult( ) );
            }
        }
        catch ( RepositoryException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_ADD_FAILED ) );
        }
    }

    @Override
    public RepositoryGroup updateRepositoryGroup( final String repositoryGroupId, final RepositoryGroup repositoryGroup ) throws ArchivaRestServiceException
    {
        if ( StringUtils.isEmpty( repositoryGroupId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_FOUND, "" ), 404 );
        }
        if ( !repositoryRegistry.hasRepositoryGroup( repositoryGroupId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_FOUND ), 404 );
        }
        repositoryGroup.setId( repositoryGroupId );
        try
        {
            RepositoryGroupConfiguration configuration = toConfig( repositoryGroup );
            CheckedResult<org.apache.archiva.repository.RepositoryGroup, Map<String, List<ValidationError>>> validationResult = repositoryRegistry.putRepositoryGroupAndValidate( configuration );
            if ( validationResult.isValid( ) )
            {
                httpServletResponse.setStatus( 201 );
                return RepositoryGroup.of( validationResult.getRepository( ) );
            }
            else
            {
                throw ValidationException.of( validationResult.getResult( ) );
            }
        }
        catch ( RepositoryException e )
        {
            log.error( "Exception during repository group update: {}", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_UPDATE_FAILED, e.getMessage( ) ) );

        }
    }

    @Override
    public Response deleteRepositoryGroup( String repositoryGroupId ) throws ArchivaRestServiceException
    {
        if ( StringUtils.isEmpty( repositoryGroupId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_FOUND, "" ), 404 );
        }
        try
        {
            org.apache.archiva.repository.RepositoryGroup group = repositoryRegistry.getRepositoryGroup( repositoryGroupId );
            if ( group == null )
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_FOUND, "" ), 404 );
            }
            repositoryRegistry.removeRepositoryGroup( group );
            return Response.ok( ).build( );
        }
        catch ( RepositoryException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_DELETE_FAILED ) );
        }
    }

    @Override
    public RepositoryGroup addRepositoryToGroup( String repositoryGroupId, String repositoryId ) throws ArchivaRestServiceException
    {
        if ( StringUtils.isEmpty( repositoryGroupId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_FOUND, "" ), 404 );
        }
        if ( StringUtils.isEmpty( repositoryId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_FOUND, "" ), 404 );
        }
        try
        {
            org.apache.archiva.repository.RepositoryGroup repositoryGroup = repositoryRegistry.getRepositoryGroup( repositoryGroupId );
            if ( repositoryGroup == null )
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_FOUND, "" ), 404 );
            }
            if ( !( repositoryGroup instanceof EditableRepositoryGroup ) )
            {
                log.error( "This group instance is not editable: {}", repositoryGroupId );
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_UPDATE_FAILED, "" ), 500 );
            }
            EditableRepositoryGroup editableRepositoryGroup = (EditableRepositoryGroup) repositoryGroup;
            if ( editableRepositoryGroup.getRepositories( ).stream( ).anyMatch( repo -> repositoryId.equals( repo.getId( ) ) ) )
            {
                log.info( "Repository {} is already member of group {}", repositoryId, repositoryGroupId );
                return RepositoryGroup.of( editableRepositoryGroup );
            }
            org.apache.archiva.repository.ManagedRepository managedRepo = repositoryRegistry.getManagedRepository( repositoryId );
            if ( managedRepo == null )
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_FOUND, "" ), 404 );
            }
            editableRepositoryGroup.addRepository( managedRepo );
            org.apache.archiva.repository.RepositoryGroup newGroup = repositoryRegistry.putRepositoryGroup( editableRepositoryGroup );
            return RepositoryGroup.of( newGroup );
        }
        catch ( RepositoryException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_UPDATE_FAILED, e.getMessage( ) ), 500 );
        }
    }

    @Override
    public RepositoryGroup deleteRepositoryFromGroup( final String repositoryGroupId, final String repositoryId ) throws ArchivaRestServiceException
    {
        if ( StringUtils.isEmpty( repositoryGroupId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_FOUND, repositoryGroupId ), 404 );
        }
        if ( StringUtils.isEmpty( repositoryId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_FOUND, repositoryId ), 404 );
        }
        try
        {
            org.apache.archiva.repository.RepositoryGroup repositoryGroup = repositoryRegistry.getRepositoryGroup( repositoryGroupId );
            if ( repositoryGroup == null )
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_FOUND, "" ), 404 );
            }
            if ( repositoryGroup.getRepositories( ).stream( ).noneMatch( r -> repositoryId.equals( r.getId( ) ) ) )
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_FOUND, repositoryId ), 404 );
            }
            if ( !( repositoryGroup instanceof EditableRepositoryGroup ) )
            {
                log.error( "This group instance is not editable: {}", repositoryGroupId );
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_UPDATE_FAILED, "" ), 500 );
            }
            EditableRepositoryGroup editableRepositoryGroup = (EditableRepositoryGroup) repositoryGroup;
            editableRepositoryGroup.removeRepository( repositoryId );
            org.apache.archiva.repository.RepositoryGroup newGroup = repositoryRegistry.putRepositoryGroup( editableRepositoryGroup );
            return RepositoryGroup.of( newGroup );
        }
        catch ( RepositoryException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_UPDATE_FAILED, e.getMessage( ) ), 500 );
        }
    }


}
