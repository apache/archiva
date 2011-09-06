package org.apache.archiva.rest.api.services;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.rest.api.model.RepositoryGroup;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.codehaus.plexus.redback.authorization.RedbackAuthorization;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Path( "/repositoryGroupService/" )
public interface RepositoryGroupService
{
    @Path( "getRepositoriesGroups" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    List<RepositoryGroup> getRepositoriesGroups()
        throws RepositoryAdminException;

    @Path( "getRepositoryGroup/{repositoryGroupId}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    RepositoryGroup getRepositoryGroup( @PathParam( "repositoryGroupId" ) String repositoryGroupId )
        throws RepositoryAdminException;

    @Path( "addRepositoryGroup" )
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean addRepositoryGroup( RepositoryGroup repositoryGroup )
        throws RepositoryAdminException;

    @Path( "updateRepositoryGroup" )
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean updateRepositoryGroup( RepositoryGroup repositoryGroup )
        throws RepositoryAdminException;

    @Path( "deleteRepositoryGroup/{repositoryGroupId}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean deleteRepositoryGroup( @PathParam( "repositoryGroupId" ) String repositoryGroupId )
        throws RepositoryAdminException;

    @Path( "addRepositoryToGroup" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean addRepositoryToGroup( @QueryParam( "repositoryGroupId" ) String repositoryGroupId,
                                  @QueryParam( "repositoryId" ) String repositoryId )
        throws RepositoryAdminException;

    @Path( "addRepositoryToGroup" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean deleteRepositoryFromGroup( @QueryParam( "repositoryGroupId" ) String repositoryGroupId,
                                       @QueryParam( "repositoryId" ) String repositoryId )
        throws RepositoryAdminException;


}
