package org.apache.archiva.rest.services.v2;
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
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.util.QueryHelper;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.rest.api.model.v2.Artifact;
import org.apache.archiva.rest.api.model.v2.FileInfo;
import org.apache.archiva.rest.api.model.v2.MavenManagedRepository;
import org.apache.archiva.rest.api.services.v2.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.v2.ErrorKeys;
import org.apache.archiva.rest.api.services.v2.ErrorMessage;
import org.apache.archiva.rest.api.services.v2.MavenManagedRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service("v2.managedMavenRepositoryService#rest")
public class DefaultMavenManagedRepositoryService implements MavenManagedRepositoryService
{
    private static final Logger log = LoggerFactory.getLogger( DefaultMavenManagedRepositoryService.class );
    private static final QueryHelper<org.apache.archiva.admin.model.beans.ManagedRepository> QUERY_HELPER = new QueryHelper<>( new String[]{"id", "name"} );
    static
    {
        QUERY_HELPER.addStringFilter( "id", org.apache.archiva.admin.model.beans.ManagedRepository::getId );
        QUERY_HELPER.addStringFilter( "name", org.apache.archiva.admin.model.beans.ManagedRepository::getName );
        QUERY_HELPER.addStringFilter( "location", org.apache.archiva.admin.model.beans.ManagedRepository::getName );
        QUERY_HELPER.addBooleanFilter( "snapshot", org.apache.archiva.admin.model.beans.ManagedRepository::isSnapshots );
        QUERY_HELPER.addBooleanFilter( "release", org.apache.archiva.admin.model.beans.ManagedRepository::isReleases );
        QUERY_HELPER.addNullsafeFieldComparator( "id", org.apache.archiva.admin.model.beans.ManagedRepository::getId );
        QUERY_HELPER.addNullsafeFieldComparator( "name", org.apache.archiva.admin.model.beans.ManagedRepository::getName );
    }

    private ManagedRepositoryAdmin managedRepositoryAdmin;
    private RepositoryRegistry repositoryRegistry;

    public DefaultMavenManagedRepositoryService( RepositoryRegistry repositoryRegistry, ManagedRepositoryAdmin managedRepositoryAdmin )
    {
        this.managedRepositoryAdmin = managedRepositoryAdmin;
    }

    @Override
    public PagedResult<MavenManagedRepository> getManagedRepositories( String searchTerm, Integer offset, Integer limit, List<String> orderBy, String order ) throws ArchivaRestServiceException
    {
        try
        {
            List<org.apache.archiva.admin.model.beans.ManagedRepository> result = managedRepositoryAdmin.getManagedRepositories( );
            int totalCount = Math.toIntExact( result.stream( ).count( ) );
        }
        catch (ArithmeticException e) {
            log.error( "Invalid number of repositories detected." );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.INVALID_RESULT_SET_ERROR ) );
        }
        catch ( RepositoryAdminException e )
        {
            e.printStackTrace( );
        }
        return null;

    }

    @Override
    public MavenManagedRepository getManagedRepository( String repositoryId ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public Response deleteManagedRepository( String repositoryId, boolean deleteContent ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public MavenManagedRepository addManagedRepository( MavenManagedRepository managedRepository ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public MavenManagedRepository updateManagedRepository( MavenManagedRepository managedRepository ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public FileInfo getFileStatus( String repositoryId, String fileLocation ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public Response copyArtifact( String srcRepositoryId, String dstRepositoryId,
                                  String path ) throws ArchivaRestServiceException
    {
        ManagedRepository srcRepo = repositoryRegistry.getManagedRepository( srcRepositoryId );
        if (srcRepo==null) {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_FOUND, srcRepositoryId ), 404 );
        }
        ManagedRepository dstRepo = repositoryRegistry.getManagedRepository( dstRepositoryId );
        if (dstRepo==null) {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_NOT_FOUND, dstRepositoryId ), 404 );
        }
        if (dstRepo.getAsset( path ).exists()) {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.ARTIFACT_EXISTS_AT_DEST, path ) );
        }


        return null;
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
