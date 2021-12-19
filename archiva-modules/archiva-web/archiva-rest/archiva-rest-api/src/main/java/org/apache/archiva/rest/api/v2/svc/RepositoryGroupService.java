package org.apache.archiva.rest.api.v2.svc;
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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.rest.api.v2.model.RepositoryGroup;
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
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.archiva.rest.api.v2.svc.RestConfiguration.DEFAULT_PAGE_LIMIT;

/**
 * Endpoint for repository groups that combine multiple repositories into a single virtual repository.
 *
 * @author Olivier Lamy
 * @author Martin Stockhammer
 * @since 3.0
 */
@Path( "/repository_groups" )
@Schema( name = "RepositoryGroups", description = "Managing of repository groups or virtual repositories" )
public interface RepositoryGroupService
{
    @Path( "" )
    @GET
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Operation( summary = "Returns all repository group entries.",
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
    PagedResult<RepositoryGroup> getRepositoriesGroups( @QueryParam( "q" ) @DefaultValue( "" ) String searchTerm,
                                                        @QueryParam( "offset" ) @DefaultValue( "0" ) Integer offset,
                                                        @QueryParam( "limit" ) @DefaultValue( value = DEFAULT_PAGE_LIMIT ) Integer limit,
                                                        @QueryParam( "orderBy" ) @DefaultValue( "id" ) List<String> orderBy,
                                                        @QueryParam( "order" ) @DefaultValue( "asc" ) String order )
        throws ArchivaRestServiceException;

    @Path( "{id}" )
    @GET
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Operation( summary = "Returns a single repository group configuration.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the configuration is returned",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = RepositoryGroup.class ) )
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The repository group with the given id does not exist",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    RepositoryGroup getRepositoryGroup( @PathParam( "id" ) String repositoryGroupId )
        throws ArchivaRestServiceException;

    @Path( "" )
    @POST
    @Consumes( {APPLICATION_JSON} )
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Operation( summary = "Creates a new group entry.",
        requestBody =
        @RequestBody( required = true, description = "The configuration of the repository group.",
            content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = RepositoryGroup.class ) )
        )
        ,
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "201",
                description = "If the repository group was created",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = RepositoryGroup.class ) )
            ),
            @ApiResponse( responseCode = "303", description = "The repository group exists already",
                headers = {
                    @Header( name = "Location", description = "The URL of existing group", schema = @Schema( type = "string" ) )
                }
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "422", description = "The body data is not valid",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    RepositoryGroup addRepositoryGroup( RepositoryGroup repositoryGroup )
        throws ArchivaRestServiceException;

    @Path( "{id}" )
    @PUT
    @Consumes( {APPLICATION_JSON} )
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Operation( summary = "Returns all repository group entries.",
        requestBody =
        @RequestBody( required = true, description = "The configuration of the repository group.",
            content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = RepositoryGroup.class ) )
        )
        ,
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the group is returned",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = RepositoryGroup.class ) )
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The group with the given id does not exist",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "422", description = "The body data is not valid",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) )
        }
    )
    RepositoryGroup updateRepositoryGroup( @PathParam( "id" ) String groupId, RepositoryGroup repositoryGroup )
        throws ArchivaRestServiceException;

    @Path( "{id}" )
    @DELETE
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Operation( summary = "Deletes the repository group entry with the given id.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the group was deleted"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to delete the group",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The group with the given id does not exist",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
        }
    )
    Response deleteRepositoryGroup( @PathParam( "id" ) String repositoryGroupId )
        throws ArchivaRestServiceException;

    @Path( "{id}/repositories/{repositoryId}" )
    @PUT
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Operation( summary = "Adds the repository with the given id to the repository group.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the repository was added or if it was already part of the group"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to delete the group",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "The group with the given id does not exist",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
        }
    )
    RepositoryGroup addRepositoryToGroup( @PathParam( "id" ) String repositoryGroupId,
                                          @PathParam( "repositoryId" ) String repositoryId )
        throws ArchivaRestServiceException;

    @Path( "{id}/repositories/{repositoryId}" )
    @DELETE
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Operation( summary = "Removes the repository with the given id from the repository group.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the repository was removed."
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to delete the group",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
            @ApiResponse( responseCode = "404", description = "Either the group with the given id does not exist, or the repository was not part of the group.",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = ArchivaRestError.class ) ) ),
        }
    )
    RepositoryGroup deleteRepositoryFromGroup( @PathParam( "id" ) String repositoryGroupId,
                                               @PathParam( "repositoryId" ) String repositoryId )
        throws ArchivaRestServiceException;


}
