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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.rest.api.model.ArchivaRepositoryStatistics;
import org.apache.archiva.security.common.ArchivaRoleConstants;

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
 * @since 1.4-M1
 */
@Path( "/managedRepositoriesService/" )
public interface ManagedRepositoriesService
{
    @Path( "getManagedRepositories" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    List<ManagedRepository> getManagedRepositories()
        throws ArchivaRestServiceException;

    @Path( "getManagedRepository/{repositoryId}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    ManagedRepository getManagedRepository( @PathParam( "repositoryId" ) String repositoryId )
        throws ArchivaRestServiceException;

    @Path( "deleteManagedRepository" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean deleteManagedRepository( @QueryParam( "repositoryId" ) String repositoryId,
                                     @QueryParam( "deleteContent" ) boolean deleteContent )
        throws ArchivaRestServiceException;


    @Path( "addManagedRepository" )
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    ManagedRepository addManagedRepository( ManagedRepository managedRepository )
        throws ArchivaRestServiceException;


    @Path( "updateManagedRepository" )
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean updateManagedRepository( ManagedRepository managedRepository )
        throws ArchivaRestServiceException;

    /**
     * @since 1.4-M3
     */
    @Path( "fileLocationExists" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean fileLocationExists( @QueryParam( "fileLocation" ) String fileLocation )
        throws ArchivaRestServiceException;

    /**
     * @since 1.4-M3
     */
    @Path( "getManagedRepositoryStatistics/{repositoryId}/{lang}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    ArchivaRepositoryStatistics getManagedRepositoryStatistics( @PathParam( "repositoryId" ) String repositoryId,
                                                                @PathParam( "lang" ) String lang )
        throws ArchivaRestServiceException;

    /**
     * return a pom snippet to use this repository with entities escaped (&lt; &gt;)
     * @since 1.4-M3
     */
    @Path( "getPomSnippet/{repositoryId}" )
    @GET
    @Produces( { MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    String getPomSnippet( @PathParam( "repositoryId" ) String repositoryId )
        throws ArchivaRestServiceException;


}
