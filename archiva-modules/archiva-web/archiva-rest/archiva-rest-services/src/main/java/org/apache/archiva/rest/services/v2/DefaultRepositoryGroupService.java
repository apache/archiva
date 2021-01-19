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

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.EntityExistsException;
import org.apache.archiva.admin.model.EntityNotFoundException;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.group.RepositoryGroupAdmin;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.util.QueryHelper;
import org.apache.archiva.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.apache.archiva.redback.rest.services.RedbackRequestInformation;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.rest.api.model.v2.RepositoryGroup;
import org.apache.archiva.rest.api.services.v2.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.v2.ErrorMessage;
import org.apache.archiva.rest.api.services.v2.RepositoryGroupService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * REST V2 Implementation for repository groups.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @see RepositoryGroupService
 * @since 3.0
 */
@Service("v2.repositoryGroupService#rest")
public class DefaultRepositoryGroupService implements RepositoryGroupService
{
    @Context
    HttpServletResponse httpServletResponse;

    @Context
    UriInfo uriInfo;

    private static final Logger log = LoggerFactory.getLogger( DefaultRepositoryGroupService.class );

    private static final QueryHelper<org.apache.archiva.admin.model.beans.RepositoryGroup> QUERY_HELPER = new QueryHelper<>( new String[]{"id"} );

    @Inject
    private RepositoryGroupAdmin repositoryGroupAdmin;


    static
    {
        QUERY_HELPER.addStringFilter( "id", org.apache.archiva.admin.model.beans.RepositoryGroup::getId );
        QUERY_HELPER.addNullsafeFieldComparator( "id", org.apache.archiva.admin.model.beans.RepositoryGroup::getId );
    }


    protected AuditInformation getAuditInformation( )
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get( );
        User user = redbackRequestInformation == null ? null : redbackRequestInformation.getUser( );
        String remoteAddr = redbackRequestInformation == null ? null : redbackRequestInformation.getRemoteAddr( );
        return new AuditInformation( user, remoteAddr );
    }

    @Override
    public PagedResult<RepositoryGroup> getRepositoriesGroups( String searchTerm, Integer offset, Integer limit, List<String> orderBy, String order ) throws ArchivaRestServiceException
    {
        try
        {
            Predicate<org.apache.archiva.admin.model.beans.RepositoryGroup> filter = QUERY_HELPER.getQueryFilter( searchTerm );
            Comparator<org.apache.archiva.admin.model.beans.RepositoryGroup> ordering = QUERY_HELPER.getComparator( orderBy, QUERY_HELPER.isAscending( order ) );
            int totalCount = Math.toIntExact( repositoryGroupAdmin.getRepositoriesGroups( ).stream( ).filter( filter ).count( ) );
            List<RepositoryGroup> result = repositoryGroupAdmin.getRepositoriesGroups( ).stream( ).filter( filter ).sorted( ordering ).skip( offset ).limit( limit ).map(
                RepositoryGroup::of
            ).collect( Collectors.toList( ) );
            return new PagedResult<>( totalCount, offset, limit, result );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( "Repository admin error: {}", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_ADMIN_ERROR, e.getMessage( ) ) );
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
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_EXIST, "" ), 404 );
        }
        try
        {
            org.apache.archiva.admin.model.beans.RepositoryGroup group = repositoryGroupAdmin.getRepositoryGroup( repositoryGroupId );
            return RepositoryGroup.of( group );
        }
        catch ( EntityNotFoundException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_EXIST, repositoryGroupId ), 404 );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_ADMIN_ERROR, e.getMessage( ) ) );
        }
    }

    private org.apache.archiva.admin.model.beans.RepositoryGroup toModel( RepositoryGroup group )
    {
        org.apache.archiva.admin.model.beans.RepositoryGroup result = new org.apache.archiva.admin.model.beans.RepositoryGroup( );
        result.setId( group.getId( ) );
        result.setLocation( group.getLocation( ) );
        result.setRepositories( new ArrayList<>( group.getRepositories( ) ) );
        result.setMergedIndexPath( group.getMergeConfiguration( ).getMergedIndexPath( ) );
        result.setMergedIndexTtl( group.getMergeConfiguration( ).getMergedIndexTtlMinutes( ) );
        result.setCronExpression( group.getMergeConfiguration( ).getIndexMergeSchedule( ) );
        return result;
    }

    @Override
    public RepositoryGroup addRepositoryGroup( RepositoryGroup repositoryGroup ) throws ArchivaRestServiceException
    {
        try
        {
            Boolean result = repositoryGroupAdmin.addRepositoryGroup( toModel( repositoryGroup ), getAuditInformation( ) );
            if ( result )
            {
                org.apache.archiva.admin.model.beans.RepositoryGroup newGroup = repositoryGroupAdmin.getRepositoryGroup( repositoryGroup.getId( ) );
                if ( newGroup != null )
                {
                    return RepositoryGroup.of( newGroup );
                }
                else
                {
                    throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_ADD_FAILED ) );
                }
            }
            else
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_ADD_FAILED ) );
            }
        }
        catch ( EntityExistsException e )
        {
            httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePathBuilder( ).path( repositoryGroup.getId( ) ).build( ).toString( ) );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_EXIST, repositoryGroup.getId( ) ), 303 );
        }
        catch ( RepositoryAdminException e )
        {
            return handleAdminException( e );
        }
    }

    private RepositoryGroup handleAdminException( RepositoryAdminException e ) throws ArchivaRestServiceException
    {
        log.error( "Repository admin error: {}", e.getMessage( ), e );
        if ( e.keyExists( ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.PREFIX + e.getKey( ), e.getParameters( ) ) );
        }
        else
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_ADMIN_ERROR, e.getMessage( ) ) );
        }
    }

    @Override
    public RepositoryGroup updateRepositoryGroup( String repositoryGroupId, RepositoryGroup repositoryGroup ) throws ArchivaRestServiceException
    {
        if ( StringUtils.isEmpty( repositoryGroupId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_EXIST, "" ), 404 );
        }
        org.apache.archiva.admin.model.beans.RepositoryGroup updateGroup = toModel( repositoryGroup );
        try
        {
            org.apache.archiva.admin.model.beans.RepositoryGroup originGroup = repositoryGroupAdmin.getRepositoryGroup( repositoryGroupId );
            if ( StringUtils.isEmpty( updateGroup.getId( ) ) )
            {
                updateGroup.setId( repositoryGroupId );
            }
            if ( StringUtils.isEmpty( updateGroup.getLocation( ) ) )
            {
                updateGroup.setLocation( originGroup.getLocation( ) );
            }
            if ( StringUtils.isEmpty( updateGroup.getMergedIndexPath( ) ) )
            {
                updateGroup.setMergedIndexPath( originGroup.getMergedIndexPath( ) );
            }
            if ( updateGroup.getCronExpression( ) == null )
            {
                updateGroup.setCronExpression( originGroup.getCronExpression( ) );
            }
            if ( updateGroup.getRepositories( ) == null || updateGroup.getRepositories( ).size( ) == 0 )
            {
                updateGroup.setRepositories( originGroup.getRepositories( ) );
            }
            if ( updateGroup.getMergedIndexTtl( ) <= 0 )
            {
                updateGroup.setMergedIndexTtl( originGroup.getMergedIndexTtl( ) );
            }
            repositoryGroupAdmin.updateRepositoryGroup( updateGroup, getAuditInformation( ) );
            return RepositoryGroup.of( repositoryGroupAdmin.getRepositoryGroup( repositoryGroupId ) );
        }
        catch ( EntityNotFoundException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_EXIST, repositoryGroupId ), 404 );
        }
        catch ( RepositoryAdminException e )
        {
            return handleAdminException( e );
        }
    }

    @Override
    public Response deleteRepositoryGroup( String repositoryGroupId ) throws ArchivaRestServiceException
    {
        if ( StringUtils.isEmpty( repositoryGroupId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_EXIST, "" ), 404 );
        }
        try
        {
            Boolean deleted = repositoryGroupAdmin.deleteRepositoryGroup( repositoryGroupId, getAuditInformation( ) );
            if ( !deleted )
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_DELETE_FAILED ) );
            }
            return Response.ok( ).build( );
        }
        catch ( EntityNotFoundException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_EXIST, repositoryGroupId ), 404 );
        }
        catch ( RepositoryAdminException e )
        {
            handleAdminException( e );
            // cannot happen:
            return null;
        }
    }

    @Override
    public RepositoryGroup addRepositoryToGroup( String repositoryGroupId, String repositoryId ) throws ArchivaRestServiceException
    {
        if ( StringUtils.isEmpty( repositoryGroupId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_EXIST, "" ), 404 );
        }
        if ( StringUtils.isEmpty( repositoryId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_EXIST, "" ), 404 );
        }
        try
        {
            repositoryGroupAdmin.addRepositoryToGroup( repositoryGroupId, repositoryId, getAuditInformation( ) );
            return RepositoryGroup.of( repositoryGroupAdmin.getRepositoryGroup( repositoryGroupId ) );
        }
        catch ( EntityNotFoundException e )
        {
            return handleNotFoundException( repositoryGroupId, repositoryId, e );
        }
        catch ( EntityExistsException e )
        {
            // This is thrown, if the repositoryId is already assigned to the group. We ignore this for the PUT action (nothing to do).
            try
            {
                return RepositoryGroup.of( repositoryGroupAdmin.getRepositoryGroup( repositoryGroupId ) );
            }
            catch ( RepositoryAdminException repositoryAdminException )
            {
                return handleAdminException( e );
            }
        }
        catch ( RepositoryAdminException e )
        {
            return handleAdminException( e );
        }
    }

    @Override
    public RepositoryGroup deleteRepositoryFromGroup( String repositoryGroupId, String repositoryId ) throws org.apache.archiva.rest.api.services.v2.ArchivaRestServiceException
    {
        if ( StringUtils.isEmpty( repositoryGroupId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_EXIST, "" ), 404 );
        }
        if ( StringUtils.isEmpty( repositoryId ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_EXIST, "" ), 404 );
        }
        try
        {
            repositoryGroupAdmin.deleteRepositoryFromGroup( repositoryGroupId, repositoryId, getAuditInformation( ) );
            return RepositoryGroup.of( repositoryGroupAdmin.getRepositoryGroup( repositoryGroupId ) );
        }
        catch ( EntityNotFoundException e )
        {
            return handleNotFoundException( repositoryGroupId, repositoryId, e );
        }
        catch ( RepositoryAdminException e )
        {
            return handleAdminException( e );
        }
    }

    protected RepositoryGroup handleNotFoundException( String repositoryGroupId, String repositoryId, EntityNotFoundException e ) throws ArchivaRestServiceException
    {
        if ( e.getParameters( ).length > 0 )
        {
            if ( repositoryGroupId.equals( e.getParameters( )[0] ) )
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_EXIST, repositoryGroupId ), 404 );
            }
            else if ( repositoryId.equals( e.getParameters( )[0] ) )
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_EXIST, repositoryGroupId ), 404 );
            }
        }
        log.warn( "Entity not found but neither group nor repo set in exception" );
        throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_GROUP_NOT_EXIST, repositoryGroupId ), 404 );
    }


}
