package org.apache.archiva.rest.api.services.v2;/*
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.model.PropertyEntry;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.rest.api.model.v2.BeanInformation;
import org.apache.archiva.rest.api.model.v2.CacheConfiguration;
import org.apache.archiva.rest.api.model.v2.LdapConfiguration;
import org.apache.archiva.rest.api.model.v2.SecurityConfiguration;
import org.apache.archiva.security.common.ArchivaRoleConstants;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.archiva.rest.api.services.v2.Configuration.DEFAULT_PAGE_LIMIT;

/**
 *
 * Service for configuration of redback and security related settings.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@Path( "/security" )
@Tag(name = "v2")
@Tag(name = "v2/Security")
@SecurityRequirement(name = "BearerAuth")
public interface SecurityConfigurationService
{
    @Path("config")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Returns the security configuration that is currently active.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the configuration could be retrieved"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestServiceException.class )) )
        }
    )
    SecurityConfiguration getConfiguration()
        throws ArchivaRestServiceException;

    @Path( "config/properties" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    @Operation( summary = "Returns all configuration properties. The result is paged.",
        parameters = {
            @Parameter(name = "q", description = "Search term"),
            @Parameter(name = "offset", description = "The offset of the first element returned"),
            @Parameter(name = "limit", description = "Maximum number of items to return in the response"),
            @Parameter(name = "orderBy", description = "List of attribute used for sorting (key, value)"),
            @Parameter(name = "order", description = "The sort order. Either ascending (asc) or descending (desc)")
        },
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be returned",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class))
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestServiceException.class )) )
        }
    )
    PagedResult<PropertyEntry> getConfigurationProperties( @QueryParam("q") @DefaultValue( "" ) String searchTerm,
                                                           @QueryParam( "offset" ) @DefaultValue( "0" ) Integer offset,
                                                           @QueryParam( "limit" ) @DefaultValue( value = DEFAULT_PAGE_LIMIT ) Integer limit,
                                                           @QueryParam( "orderBy") @DefaultValue( "key" ) List<String> orderBy,
                                                           @QueryParam("order") @DefaultValue( "asc" ) String order ) throws ArchivaRestServiceException;

    @Path("config/ldap")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Returns the LDAP configuration that is currently active.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the configuration could be retrieved"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestServiceException.class )) )
        }
    )
    LdapConfiguration getLdapConfiguration( ) throws ArchivaRestServiceException;


    @Path("config/cache")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Returns the cache configuration that is currently active.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the configuration could be retrieved"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestServiceException.class )) )
        }
    )
    CacheConfiguration getCacheConfiguration( ) throws ArchivaRestServiceException;

    @Path("user_managers")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Returns the available user manager implementations.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be retrieved"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestServiceException.class )) )
        }
    )
    List<BeanInformation> getAvailableUserManagers()
        throws ArchivaRestServiceException;

    @Path("rbac_managers")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Returns the available RBAC manager implementations.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be retrieved"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestServiceException.class )) )
        }
    )
    List<BeanInformation> getAvailableRbacManagers()
        throws ArchivaRestServiceException;

}
