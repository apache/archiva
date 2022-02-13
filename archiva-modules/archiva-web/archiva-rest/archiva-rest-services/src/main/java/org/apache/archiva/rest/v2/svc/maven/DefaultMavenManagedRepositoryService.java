package org.apache.archiva.rest.v2.svc.maven;
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

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.common.MultiModelMapper;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.util.QueryHelper;
import org.apache.archiva.configuration.model.ManagedRepositoryConfiguration;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.system.DefaultSecuritySession;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.LayoutException;
import org.apache.archiva.repository.storage.fs.FsStorageUtil;
import org.apache.archiva.rest.api.v2.model.FileInfo;
import org.apache.archiva.rest.api.v2.model.MavenManagedRepository;
import org.apache.archiva.rest.api.v2.model.MavenManagedRepositoryUpdate;
import org.apache.archiva.rest.api.v2.model.map.ServiceMapperFactory;
import org.apache.archiva.rest.api.v2.svc.ArchivaRestServiceException;
import org.apache.archiva.rest.api.v2.svc.ErrorKeys;
import org.apache.archiva.rest.api.v2.svc.ErrorMessage;
import org.apache.archiva.rest.api.v2.svc.maven.MavenManagedRepositoryService;
import org.apache.archiva.rest.v2.svc.AbstractService;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.archiva.security.common.ArchivaRoleConstants.OPERATION_ADD_ARTIFACT;
import static org.apache.archiva.security.common.ArchivaRoleConstants.OPERATION_READ_REPOSITORY;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service("v2.managedMavenRepositoryService#rest")
public class DefaultMavenManagedRepositoryService extends AbstractService implements MavenManagedRepositoryService
{
    @Context
    HttpServletResponse httpServletResponse;

    @Context
    UriInfo uriInfo;



    private static final Logger log = LoggerFactory.getLogger( DefaultMavenManagedRepositoryService.class );
    private static final QueryHelper<ManagedRepository> QUERY_HELPER = new QueryHelper<>( new String[]{"id", "name"} );
    static
    {
        QUERY_HELPER.addStringFilter( "id", ManagedRepository::getId );
        QUERY_HELPER.addStringFilter( "name", ManagedRepository::getName );
        QUERY_HELPER.addStringFilter( "location", (r)  -> r.getLocation().toString() );
        QUERY_HELPER.addBooleanFilter( "snapshot", (r) -> r.getActiveReleaseSchemes( ).contains( ReleaseScheme.SNAPSHOT ) );
        QUERY_HELPER.addBooleanFilter( "release", (r) -> r.getActiveReleaseSchemes().contains( ReleaseScheme.RELEASE ));
        QUERY_HELPER.addNullsafeFieldComparator( "id", ManagedRepository::getId );
        QUERY_HELPER.addNullsafeFieldComparator( "name", ManagedRepository::getName );
    }

    private final ManagedRepositoryAdmin managedRepositoryAdmin;
    private final RepositoryRegistry repositoryRegistry;
    private final SecuritySystem securitySystem;
    private final ServiceMapperFactory serviceMapperFactory;
    private final MultiModelMapper<MavenManagedRepository, ManagedRepositoryConfiguration, ManagedRepository> mapper;


    public DefaultMavenManagedRepositoryService( SecuritySystem securitySystem,
                                                 RepositoryRegistry repositoryRegistry,
                                                 ManagedRepositoryAdmin managedRepositoryAdmin,
                                                 ServiceMapperFactory serviceMapperFactory ) throws IllegalArgumentException
    {
        this.securitySystem = securitySystem;
        this.repositoryRegistry = repositoryRegistry;
        this.managedRepositoryAdmin = managedRepositoryAdmin;
        this.serviceMapperFactory = serviceMapperFactory;
        this.mapper = serviceMapperFactory.getMapper( MavenManagedRepository.class, ManagedRepositoryConfiguration.class, ManagedRepository.class );
    }

    @Override
    public PagedResult<MavenManagedRepository> getManagedRepositories( final String searchTerm, final Integer offset,
                                                                       final Integer limit, final List<String> orderBy,
                                                                       final String order ) throws ArchivaRestServiceException
    {
        try
        {
            Collection<ManagedRepository> repos = repositoryRegistry.getManagedRepositories( );
            final Predicate<ManagedRepository> queryFilter = QUERY_HELPER.getQueryFilter( searchTerm ).and( r -> r.getType() == RepositoryType.MAVEN );
            final Comparator<ManagedRepository> comparator = QUERY_HELPER.getComparator( orderBy, order );
            int totalCount = Math.toIntExact( repos.stream( ).filter( queryFilter ).count( ) );
            return PagedResult.of( totalCount, offset, limit, repos.stream( ).filter( queryFilter ).sorted( comparator )
                .map( mapper::reverseMap ).skip( offset ).limit( limit ).collect( Collectors.toList( ) ) );
        }
        catch (ArithmeticException e) {
            log.error( "Invalid number of repositories detected." );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.INVALID_RESULT_SET_ERROR ) );
        }
    }

    @Override
    public MavenManagedRepository getManagedRepository( String repositoryId ) throws ArchivaRestServiceException
    {
        ManagedRepository repo = repositoryRegistry.getManagedRepository( repositoryId );
        if (repo==null) {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_FOUND, repositoryId ), 404 );
        }
        if (repo.getType()!=RepositoryType.MAVEN) {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_WRONG_TYPE, repositoryId, repo.getType().name() ), 404 );
        }
        return mapper.reverseMap( repo );
    }

    @Override
    public Response deleteManagedRepository( String repositoryId, Boolean deleteContent ) throws ArchivaRestServiceException
    {
        MavenManagedRepository repo = getManagedRepository( repositoryId );
        if (repo != null)
        {
            try
            {
                managedRepositoryAdmin.deleteManagedRepository( repositoryId, getAuditInformation( ), deleteContent );
                return Response.ok( ).build( );
            }
            catch ( RepositoryAdminException e )
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_DELETE_FAILED, e.getMessage( ) ) );
            }
        } else {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_FOUND, repositoryId ), 404 );
        }
    }

    private org.apache.archiva.admin.model.beans.ManagedRepository convert(MavenManagedRepository repository) {
        org.apache.archiva.admin.model.beans.ManagedRepository repoBean = new org.apache.archiva.admin.model.beans.ManagedRepository( );
        repoBean.setId( repository.getId( ) );
        repoBean.setName( repository.getName() );
        repoBean.setDescription( repository.getDescription() );
        repoBean.setBlockRedeployments( repository.isBlocksRedeployments() );
        repoBean.setCronExpression( repository.getSchedulingDefinition() );
        repoBean.setLocation( repository.getLocation() );
        repoBean.setReleases( repository.getReleaseSchemes().contains( ReleaseScheme.RELEASE.name() ) );
        repoBean.setSnapshots( repository.getReleaseSchemes().contains( ReleaseScheme.SNAPSHOT.name() ) );
        repoBean.setScanned( repository.isScanned() );
        repoBean.setDeleteReleasedSnapshots( repository.isDeleteSnapshotsOfRelease() );
        repoBean.setSkipPackedIndexCreation( repository.isSkipPackedIndexCreation() );
        repoBean.setRetentionCount( repository.getRetentionCount() );
        if (repository.getRetentionPeriod()!=null)
        {
            repoBean.setRetentionPeriod( repository.getRetentionPeriod( ).getDays( ) );
        }
        repoBean.setIndexDirectory( repository.getIndexPath() );
        repoBean.setPackedIndexDirectory( repository.getPackedIndexPath() );
        repoBean.setLayout( repository.getLayout() );
        repoBean.setType( RepositoryType.MAVEN.name( ) );
        return repoBean;
    }

    @Override
    public MavenManagedRepository addManagedRepository( MavenManagedRepository managedRepository ) throws ArchivaRestServiceException
    {
        final String repoId = managedRepository.getId( );
        if ( StringUtils.isEmpty( repoId ) ) {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_INVALID_ID, repoId ), 422 );
        }
        Repository repo = repositoryRegistry.getRepository( repoId );
        if (repo!=null) {
            httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePathBuilder( ).path( repoId ).build( ).toString( ) );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_ID_EXISTS, repoId ), 303 );
        }
        try
        {
            repositoryRegistry.putRepository( mapper.map( managedRepository ) );
            httpServletResponse.setStatus( 201 );
            return mapper.reverseMap( repositoryRegistry.getManagedRepository( repoId ) );
        }
        catch ( RepositoryException e )
        {
            log.error( "Could not create repository: {}", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_ADD_FAILED, repoId ) );
        }
    }

    @Override
    public MavenManagedRepository updateManagedRepository( final String repositoryId, final MavenManagedRepositoryUpdate managedRepository ) throws ArchivaRestServiceException
    {
        org.apache.archiva.admin.model.beans.ManagedRepository repo = convert( managedRepository );
        try
        {
            managedRepositoryAdmin.updateManagedRepository( repo, managedRepository.hasStagingRepository( ), getAuditInformation( ), managedRepository.isResetStats( ) );
            ManagedRepository newRepo = repositoryRegistry.getManagedRepository( managedRepository.getId( ) );
            if (newRepo==null) {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_UPDATE_FAILED, repositoryId ) );
            }
            return mapper.reverseMap( newRepo );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_ADMIN_ERROR, e.getMessage( ) ) );
        }
    }

    @Override
    public FileInfo getFileStatus( String repositoryId, String fileLocation ) throws ArchivaRestServiceException
    {
        ManagedRepository repo = repositoryRegistry.getManagedRepository( repositoryId );
        if (repo==null) {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_FOUND, repositoryId ), 404 );
        }
        try
        {
            ContentItem contentItem = repo.getContent( ).toItem( fileLocation );
            if (contentItem.getAsset( ).exists( ))  {
                return FileInfo.of( contentItem.getAsset( ) );
            } else {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.ARTIFACT_NOT_FOUND, repositoryId, fileLocation ), 404 );
            }
        }
        catch ( LayoutException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_LAYOUT_ERROR, e.getMessage( ) ) );
        }
    }

    @Override
    public Response copyArtifact( String srcRepositoryId, String dstRepositoryId,
                                  String path ) throws ArchivaRestServiceException
    {
        final AuditInformation auditInformation = getAuditInformation( );
        final String userName = auditInformation.getUser( ).getUsername( );
        if ( StringUtils.isEmpty( userName ) )
        {
            httpServletResponse.setHeader( "WWW-Authenticate", "Bearer realm=\"archiva\"" );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.NOT_AUTHENTICATED ), 401 );
        }
        ManagedRepository srcRepo = repositoryRegistry.getManagedRepository( srcRepositoryId );
        if (srcRepo==null) {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_FOUND, srcRepositoryId ), 404 );
        }
        ManagedRepository dstRepo = repositoryRegistry.getManagedRepository( dstRepositoryId );
        if (dstRepo==null) {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_FOUND, dstRepositoryId ), 404 );
        }
        checkAuthority( auditInformation.getUser().getUsername(), srcRepositoryId, dstRepositoryId );
        try
        {
            ContentItem srcItem = srcRepo.getContent( ).toItem( path );
            ContentItem dstItem = dstRepo.getContent( ).toItem( path );
            if (!srcItem.getAsset().exists()){
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.ARTIFACT_NOT_FOUND, srcRepositoryId, path ), 404 );
            }
            if (dstItem.getAsset().exists()) {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.ARTIFACT_EXISTS_AT_DEST, srcRepositoryId, path ), 400 );
            }
            FsStorageUtil.copyAsset( srcItem.getAsset( ), dstItem.getAsset( ), true );
        }
        catch ( LayoutException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_LAYOUT_ERROR, e.getMessage() ) );
        }
        catch ( IOException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.ARTIFACT_COPY_ERROR, e.getMessage() ) );
        }
        return Response.ok( ).build();
    }

    private void checkAuthority(final String userName, final String srcRepositoryId, final String dstRepositoryId ) throws ArchivaRestServiceException {
        User user;
        try
        {
            user = securitySystem.getUserManager().findUser( userName );
        }
        catch ( UserNotFoundException e )
        {
            httpServletResponse.setHeader( "WWW-Authenticate", "Bearer realm=\"archiva\"" );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.USER_NOT_FOUND, userName ), 401 );
        }
        catch ( UserManagerException e )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.USER_MANAGER_ERROR, e.getMessage( ) ) );
        }

        // check karma on source : read
        AuthenticationResult authn = new AuthenticationResult( true, userName, null );
        SecuritySession securitySession = new DefaultSecuritySession( authn, user );
        try
        {
            boolean authz =
                securitySystem.isAuthorized( securitySession, OPERATION_READ_REPOSITORY,
                    srcRepositoryId );
            if ( !authz )
            {
                throw new ArchivaRestServiceException(ErrorMessage.of( ErrorKeys.PERMISSION_REPOSITORY_DENIED, srcRepositoryId, OPERATION_READ_REPOSITORY ), 403);
            }
        }
        catch ( AuthorizationException e )
        {
            log.error( "Error reading permission: {}", e.getMessage(), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.AUTHORIZATION_ERROR, e.getMessage() ), 403);
        }

        // check karma on target: write
        try
        {
            boolean authz =
                securitySystem.isAuthorized( securitySession, ArchivaRoleConstants.OPERATION_ADD_ARTIFACT,
                    dstRepositoryId );
            if ( !authz )
            {
                throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.PERMISSION_REPOSITORY_DENIED, dstRepositoryId, OPERATION_ADD_ARTIFACT ) );
            }
        }
        catch ( AuthorizationException e )
        {
            log.error( "Error reading permission: {}", e.getMessage(), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.AUTHORIZATION_ERROR, e.getMessage() ), 403);
        }


    }

    @Override
    public Response deleteArtifact( String repositoryId, String path ) throws ArchivaRestServiceException
    {

        return null;
    }


    @Override
    public Response removeProjectVersion( String repositoryId, String namespace, String projectId, String version ) throws org.apache.archiva.rest.api.services.ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public Response deleteProject( String repositoryId, String namespace, String projectId ) throws org.apache.archiva.rest.api.services.ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public Response deleteNamespace( String repositoryId, String namespace ) throws org.apache.archiva.rest.api.services.ArchivaRestServiceException
    {
        return null;
    }

}
