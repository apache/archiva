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
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.model.PropertyEntry;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.rest.api.v2.model.BeanInformation;
import org.apache.archiva.rest.api.v2.model.CacheConfiguration;
import org.apache.archiva.rest.api.v2.model.LdapConfiguration;
import org.apache.archiva.rest.api.v2.model.SecurityConfiguration;
import org.apache.archiva.security.common.ArchivaRoleConstants;

import javax.ws.rs.Consumes;
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
    @Produces({ APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Returns the security configuration that is currently active.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the configuration could be retrieved",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = SecurityConfiguration.class))
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) )
        }
    )
    SecurityConfiguration getConfiguration()
        throws ArchivaRestServiceException;

    @Path("config")
    @PUT
    @Consumes({ APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Updates the security configuration.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the configuration was updated"
            ),
            @ApiResponse( responseCode = "422", description = "Invalid content data",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to update the configuration",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) )
        }
    )
    SecurityConfiguration updateConfiguration( SecurityConfiguration newConfiguration)
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
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) )
        }
    )
    PagedResult<PropertyEntry> getConfigurationProperties( @QueryParam("q") @DefaultValue( "" ) String searchTerm,
                                                           @QueryParam( "offset" ) @DefaultValue( "0" ) Integer offset,
                                                           @QueryParam( "limit" ) @DefaultValue( value = DEFAULT_PAGE_LIMIT ) Integer limit,
                                                           @QueryParam( "orderBy") @DefaultValue( "key" ) List<String> orderBy,
                                                           @QueryParam("order") @DefaultValue( "asc" ) String order ) throws ArchivaRestServiceException;

    @Path("config/properties/{propertyName}")
    @GET
    @Produces({ APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Returns a single configuration property value.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        parameters = {
            @Parameter(in = ParameterIn.PATH, name="propertyName", description = "The name of the property to get the value for")
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the configuration could be retrieved",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = PropertyEntry.class))
            ),
            @ApiResponse( responseCode = "404", description = "The given property name does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) )
        }
    )
    PropertyEntry getConfigurationProperty( @PathParam ( "propertyName" )  String propertyName)
        throws ArchivaRestServiceException;


    @Path("config/properties/{propertyName}")
    @PUT
    @Consumes({ APPLICATION_JSON})
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Updates a single property value of the security configuration.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        parameters = {
            @Parameter(in = ParameterIn.PATH, name="propertyName", description = "The name of the property to update")
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the property value was updated."
            ),
            @ApiResponse( responseCode = "400", description = "The body data is not valid",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) ),
            @ApiResponse( responseCode = "404", description = "The given property name does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) )
        }
    )
    Response updateConfigurationProperty( @PathParam ( "propertyName" )  String propertyName, PropertyEntry propertyValue)
        throws ArchivaRestServiceException;

    @Path("config/ldap")
    @GET
    @Produces({ APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Returns the LDAP configuration that is currently active.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the configuration could be retrieved",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = LdapConfiguration.class))
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) )
        }
    )
    LdapConfiguration getLdapConfiguration( ) throws ArchivaRestServiceException;

    @Path("config/ldap")
    @PUT
    @Consumes({ APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Updates the LDAP configuration that is currently active.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the configuration was updated"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to update the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) )
        }
    )
    LdapConfiguration updateLdapConfiguration( LdapConfiguration configuration ) throws ArchivaRestServiceException;

    @Path("config/ldap/verify")
    @POST
    @Consumes({ APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Checks the given LDAP configuration.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the check was successful"
            ),
            @ApiResponse( responseCode = "400",
                description = "If the check was not successful",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class ))
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to update the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) )
        }
    )
    Response verifyLdapConfiguration( LdapConfiguration configuration ) throws ArchivaRestServiceException;

    @Path("config/cache")
    @GET
    @Produces({ APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Returns the cache configuration that is currently active.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the configuration could be retrieved",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = CacheConfiguration.class))
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) )
        }
    )
    CacheConfiguration getCacheConfiguration( ) throws ArchivaRestServiceException;

    @Path("config/cache")
    @PUT
    @Consumes({ APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Updates the LDAP configuration that is currently active.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the configuration was updated"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to update the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) )
        }
    )
    CacheConfiguration updateCacheConfiguration( CacheConfiguration cacheConfiguration ) throws ArchivaRestServiceException;


    @Path("user_managers")
    @GET
    @Produces({ APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Returns the available user manager implementations.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be retrieved",
                content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(
                    schema = @Schema(implementation = BeanInformation.class)))
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) )
        }
    )
    List<BeanInformation> getAvailableUserManagers()
        throws ArchivaRestServiceException;

    @Path("rbac_managers")
    @GET
    @Produces({ APPLICATION_JSON })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    @Operation( summary = "Returns the available RBAC manager implementations.",
        security = {
            @SecurityRequirement(
                name = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be retrieved",
                content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(
                    schema = @Schema(implementation = BeanInformation.class)))
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ArchivaRestError.class )) )
        }
    )
    List<BeanInformation> getAvailableRbacManagers()
        throws ArchivaRestServiceException;

}
