package org.apache.archiva.rest.api.services.v2.maven;
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
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.rest.api.model.v2.FileInfo;
import org.apache.archiva.rest.api.model.v2.MavenManagedRepository;
import org.apache.archiva.rest.api.model.v2.MavenManagedRepositoryUpdate;
import org.apache.archiva.rest.api.services.v2.ArchivaRestError;
import org.apache.archiva.rest.api.services.v2.ArchivaRestServiceException;
import org.apache.archiva.security.common.ArchivaRoleConstants;

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
import static org.apache.archiva.rest.api.services.v2.RestConfiguration.DEFAULT_PAGE_LIMIT;

/**
 * Service interface for managing managed maven repositories
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@Schema( name = "ManagedRepositoryService", description = "Managing and configuration of managed repositories" )
@Path( "repositories/maven/managed" )
@Tag(name = "v2")
@Tag(name = "v2/Repositories")
public interface MavenManagedRepositoryService
{
    @Path( "" )
    @GET
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
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
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
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
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Operation( summary = "Returns the managed repository with the given id.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
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
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Operation( summary = "Deletes the managed repository with the given id.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
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
                                      @QueryParam( "deleteContent" ) boolean deleteContent )
        throws ArchivaRestServiceException;


    @Path( "" )
    @POST
    @Consumes( {MediaType.APPLICATION_JSON} )
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Operation( summary = "Creates the managed repository",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
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
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Operation( summary = "Updates the managed repository with the given id",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
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
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS, resource = "{id}")
    @Operation( summary = "Returns the status of a given file in the repository",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
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
    @Operation( summary = "Copies a artifact from the source repository to the destination repository",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS,
                scopes = {
                    "{srcId}"
                }
            ),
            @SecurityRequirement(
                name= ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD,
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
    @RedbackAuthorization (noPermission = true)
    @Operation( summary = "Deletes a artifact in the repository.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_RUN_INDEXER
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

    @Path ( "{id}/co/{group}/{project}/{version}" )
    @DELETE
    @Produces ({ MediaType.APPLICATION_JSON })
    @RedbackAuthorization (noPermission = true)
    @Operation( summary = "Removes a version tree in the repository",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
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
                                   @PathParam ( "group" ) String namespace, @PathParam ( "project" ) String projectId,
                                   @PathParam ( "version" ) String version )
        throws org.apache.archiva.rest.api.services.ArchivaRestServiceException;


    @Path ( "{id}/co/{group}/{project}" )
    @DELETE
    @Produces ({ MediaType.APPLICATION_JSON })
    @RedbackAuthorization (noPermission = true)
    @Operation( summary = "Removes a project tree in the repository",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the deletion was successful"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to delete in repositories",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The managed repository with this id does not exist. Or the project does not exist.",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    Response deleteProject( @PathParam ("id") String repositoryId, @PathParam ( "group" ) String namespace, @PathParam ( "project" ) String projectId )
        throws org.apache.archiva.rest.api.services.ArchivaRestServiceException;

    @Path ( "{id}/co/{namespace}" )
    @DELETE
    @Produces ({ MediaType.APPLICATION_JSON })
    @RedbackAuthorization (noPermission = true)
    @Operation( summary = "Removes a namespace tree in the repository",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the deletion was successful"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to delete namespaces in repositories",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The managed repository with this id does not exist. Or the namespace does not exist.",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    Response deleteNamespace( @PathParam ("id") String repositoryId, @PathParam ( "namespace" ) String namespace )
        throws org.apache.archiva.rest.api.services.ArchivaRestServiceException;

}
