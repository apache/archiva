package org.apache.archiva.rest.api.v2.svc.maven;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.rest.api.v2.model.FileInfo;
import org.apache.archiva.rest.api.v2.model.MavenManagedRepository;
import org.apache.archiva.rest.api.v2.model.MavenManagedRepositoryUpdate;
import org.apache.archiva.rest.api.v2.svc.ArchivaRestError;
import org.apache.archiva.rest.api.v2.svc.ArchivaRestServiceException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.archiva.rest.api.v2.svc.RestConfiguration.DEFAULT_PAGE_LIMIT;
import static org.apache.archiva.security.common.ArchivaRoleConstants.*;

/**
 *
 * Service interface for update, delete, add of Managed Maven Repositories
 *
 * The add, delete, update methods for a repository use "/{id}" with the classical CRUD actions.
 * Where {id} is the repository ID.
 *
 * There are subpaths for certain repository management functions:
 * <ul>
 * <li>{@code /{id}/path/{groupsection1/groupsection2/... }/{project}/{version}/{artifact-file}}
 *  is used for accessing artifacts and directories by their repository path</li>
 * <li>{@code /{id}/co/{groupid}/{artifactid}/{version} } is used to access Maven artifacts by their coordinates.
 *  Which means, {groupid} is a '.' separated string.
 * </li>
 * </ul>
 *
 * @author Martin Schreier <martin_s@apache.org>
 * @since 3.0
 */
@Schema( name = "MavenManagedRepositoryService", description = "Managing and configuration of managed maven repositories" )
@Path( "repositories/maven/managed" )
@Tag(name = "v2")
@Tag(name = "v2/Repositories")
public interface MavenManagedRepositoryService
{
    @Path( "" )
    @GET
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( permissions = { OPERATION_MANAGE_CONFIGURATION, OPERATION_LIST_REPOSITORIES } )
    @Operation( summary = "Returns all managed repositories.",
        parameters = {
            @Parameter( name = "q", description = "Search term" ),
            @Parameter( name = "offset", description = "The offset of the first element returned" ),
            @Parameter( name = "limit", description = "Maximum number of items to return in the response" ),
            @Parameter( name = "orderBy", description = "List of attribute used for sorting (key, value)" ),
            @Parameter( name = "order", description = "The sort order. Either ascending (asc) or descending (desc)" )
        },
        security = {
            @SecurityRequirement(
                name = OPERATION_MANAGE_CONFIGURATION
            ),
            @SecurityRequirement(
                name = OPERATION_LIST_REPOSITORIES
            )

        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be returned",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = PagedResult.class ) )
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    PagedResult<MavenManagedRepository> getManagedRepositories(
        @QueryParam( "q" ) @DefaultValue( "" ) String searchTerm,
        @QueryParam( "offset" ) @DefaultValue( "0" ) Integer offset,
        @QueryParam( "limit" ) @DefaultValue( value = DEFAULT_PAGE_LIMIT ) Integer limit,
        @QueryParam( "orderBy" ) @DefaultValue( "id" ) List<String> orderBy,
        @QueryParam( "order" ) @DefaultValue( "asc" ) String order )
        throws ArchivaRestServiceException;


    @Path( "{id}" )
    @GET
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization(
        permissions = { OPERATION_MANAGE_CONFIGURATION, OPERATION_READ_REPOSITORY},
        resource = "{id}"
    )
    @Operation( summary = "Returns the managed repository with the given id.",
        security = {
            @SecurityRequirement(
                name = OPERATION_MANAGE_CONFIGURATION
            ),
            @SecurityRequirement(
                name = OPERATION_READ_REPOSITORY,
                scopes = "{id}"
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the managed repository could be returned",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = MavenManagedRepository.class ) )
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The managed repository with this id does not exist",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    MavenManagedRepository getManagedRepository( @PathParam( "id" ) String repositoryId )
        throws ArchivaRestServiceException;


    @Path( "{id}" )
    @DELETE
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization(
        permissions = { OPERATION_MANAGE_CONFIGURATION, OPERATION_DELETE_REPOSITORY },
        resource = "{id}"
    )
    @Operation( summary = "Deletes the managed repository with the given id.",
        security = {
            @SecurityRequirement(
                name = OPERATION_MANAGE_CONFIGURATION
            ),
            @SecurityRequirement(
                name = OPERATION_DELETE_REPOSITORY,
                scopes = "{id}"
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the managed repository could be returned"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The managed repository with this id does not exist",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    Response deleteManagedRepository( @PathParam( "id" ) String repositoryId,
                                      @DefaultValue( "false" )
                                      @QueryParam( "deleteContent" ) Boolean deleteContent )
        throws ArchivaRestServiceException;


    @Path( "" )
    @POST
    @Consumes( {MediaType.APPLICATION_JSON} )
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization(
        permissions = { OPERATION_MANAGE_CONFIGURATION, OPERATION_ADD_REPOSITORY },
        resource = "{id}"
    )
    @Operation( summary = "Creates the managed repository",
        security = {
            @SecurityRequirement(
                name = OPERATION_MANAGE_CONFIGURATION
            ),
            @SecurityRequirement(
                name = OPERATION_ADD_REPOSITORY,
                scopes = "{id}"
            )
        },
        responses = {
            @ApiResponse( responseCode = "201",
                description = "If the managed repository could be created",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = MavenManagedRepository.class ) )
            ),
            @ApiResponse( responseCode = "303", description = "The repository exists already",
                headers = {
                    @Header( name = "Location", description = "The URL of existing repository ", schema = @Schema( type = "string" ) )
                }
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to add repositories",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "422", description = "The body data is not valid",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    MavenManagedRepository addManagedRepository( MavenManagedRepository managedRepository )
        throws ArchivaRestServiceException;


    @Path( "{id}" )
    @PUT
    @Consumes( {MediaType.APPLICATION_JSON} )
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization(
        permissions = { OPERATION_MANAGE_CONFIGURATION, OPERATION_EDIT_REPOSITORY },
        resource = "{id}"
    )
    @Operation( summary = "Updates the managed repository with the given id",
        security = {
            @SecurityRequirement(
                name = OPERATION_MANAGE_CONFIGURATION
            ),
            @SecurityRequirement(
                name = OPERATION_EDIT_REPOSITORY,
                scopes = "{id}"
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the managed repository could be updated",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = MavenManagedRepository.class ) )
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to add repositories",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "422", description = "The body data is not valid",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The managed repository with this id does not exist",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    MavenManagedRepository updateManagedRepository( @PathParam( "id" ) String repositoryId,  MavenManagedRepositoryUpdate managedRepository )
        throws ArchivaRestServiceException;


    @Path( "{id}/path/{filePath: .+}" )
    @GET
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization(
        permissions = { OPERATION_MANAGE_CONFIGURATION, OPERATION_READ_REPOSITORY},
        resource = "{id}"
    )
    @Operation( summary = "Returns the status of a given artifact file in the repository",
        security = {
            @SecurityRequirement(
                name = OPERATION_MANAGE_CONFIGURATION
            ),
            @SecurityRequirement(
                name = OPERATION_READ_REPOSITORY,
                scopes = "{id}"
            )

        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the file status is returned",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = FileInfo.class ) )
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to add repositories",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The managed repository with this id does not exist. Or the file does not exist.",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    FileInfo getFileStatus( @PathParam( "id" ) String repositoryId, @PathParam( "filePath" ) String fileLocation )
        throws ArchivaRestServiceException;


    /**
     * Permissions are checked in impl
     * will copy an artifact from the source repository to the target repository
     */
    @Path ("{srcId}/path/{path: .+}/copyto/{dstId}")
    @POST
    @Produces({APPLICATION_JSON})
    @RedbackAuthorization (noPermission = true)
    @Operation( summary = "Copies a artifact from the source repository to the destination repository with the same path",
        security = {
            @SecurityRequirement(
                name = OPERATION_READ_REPOSITORY,
                scopes = {
                    "{srcId}"
                }
            ),
            @SecurityRequirement(
                name= OPERATION_ADD_ARTIFACT,
                scopes = {
                    "{dstId}"
                }
            )

        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the artifact was copied"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The repository does not exist, or if the artifact was not found",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    Response copyArtifact( @PathParam( "srcId" ) String srcRepositoryId, @PathParam( "dstId" ) String dstRepositoryId,
                           @PathParam( "path" ) String path )
        throws ArchivaRestServiceException;


    @Path ("{id}/path/{path: .+}")
    @DELETE
    @Consumes ({ APPLICATION_JSON })
    @RedbackAuthorization (
        permissions = { OPERATION_MANAGE_CONFIGURATION, OPERATION_DELETE_ARTIFACT },
        resource = "{id}"
    )
    @Operation( summary = "Deletes a artifact from the repository.",
        security = {
            @SecurityRequirement(
                name = OPERATION_MANAGE_CONFIGURATION
            ),
            @SecurityRequirement(
                name = OPERATION_DELETE_ARTIFACT,
                scopes = "{id}"
            )

        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the artifact was deleted"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The repository or the artifact does not exist",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    Response deleteArtifact( @PathParam( "id" ) String repositoryId, @PathParam( "path" ) String path )
        throws ArchivaRestServiceException;

    @Path ( "{id}/co/{groupid}/{artifactid}/{version}" )
    @DELETE
    @Produces ({ MediaType.APPLICATION_JSON })
    @RedbackAuthorization (
        permissions = { OPERATION_MANAGE_CONFIGURATION, OPERATION_DELETE_VERSION},
        resource = "{id}"
    )
    @Operation( summary = "Removes a version and all its content from the repository",
        security = {
            @SecurityRequirement(
                name = OPERATION_MANAGE_CONFIGURATION
            ),
            @SecurityRequirement(
                name = OPERATION_DELETE_VERSION,
                scopes = "{id}"
            )

        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the deletion was successful"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to delete in repositories",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The managed repository with this id does not exist. Or the version does not exist.",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    Response removeProjectVersion( @PathParam ( "id" ) String repositoryId,
                                   @PathParam ( "groupid" ) String namespace, @PathParam ( "artifactid" ) String projectId,
                                   @PathParam ( "version" ) String version )
        throws org.apache.archiva.rest.api.services.ArchivaRestServiceException;


    @Path ( "{id}/co/{groupid}/{artifactid}" )
    @DELETE
    @Produces ({ MediaType.APPLICATION_JSON })
    @RedbackAuthorization (noPermission = true)
    @Operation( summary = "Removes a artifact and all its versions from the repository",
        security = {
            @SecurityRequirement(
                name = OPERATION_MANAGE_CONFIGURATION
            ),
            @SecurityRequirement(
                name = OPERATION_DELETE_PROJECT,
                scopes = "{id}"
            )

        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the deletion was successful"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to delete in repositories",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The managed repository with this id does not exist. Or the artifact does not exist.",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    Response deleteProject( @PathParam ("id") String repositoryId, @PathParam ( "groupid" ) String namespace, @PathParam ( "artifactid" ) String projectId )
        throws org.apache.archiva.rest.api.services.ArchivaRestServiceException;

    @Path ( "{id}/co/{groupid}" )
    @DELETE
    @Produces ({ MediaType.APPLICATION_JSON })
    @RedbackAuthorization (
        permissions = { OPERATION_MANAGE_CONFIGURATION, OPERATION_DELETE_NAMESPACE },
        resource = "{id}"
    )
    @Operation( summary = "Removes a maven group and all containing artifacts and sub groups from the repository",
        security = {
            @SecurityRequirement(
                name = OPERATION_MANAGE_CONFIGURATION
            ),
            @SecurityRequirement(
                name = OPERATION_DELETE_NAMESPACE,
                scopes = "{id}"
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the deletion was successful"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to delete namespaces in repositories",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The managed repository with this id does not exist. Or the groupid does not exist.",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    Response deleteNamespace( @PathParam ("id") String repositoryId, @PathParam ( "groupid" ) String namespace )
        throws org.apache.archiva.rest.api.services.ArchivaRestServiceException;

}
