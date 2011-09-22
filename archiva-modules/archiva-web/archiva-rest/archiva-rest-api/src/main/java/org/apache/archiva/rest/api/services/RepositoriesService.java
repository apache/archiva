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

import org.apache.archiva.rest.api.model.ArtifactTransferRequest;
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

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Path( "/repositoriesService/" )
public interface RepositoriesService
{

    @Path( "scanRepository" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_RUN_INDEXER )
    Boolean scanRepository( @QueryParam( "repositoryId" ) String repositoryId,
                            @QueryParam( "fullScan" ) boolean fullScan )
        throws ArchivaRestServiceException;


    @Path( "alreadyScanning/{repositoryId}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_RUN_INDEXER )
    Boolean alreadyScanning( @PathParam( "repositoryId" ) String repositoryId )
        throws ArchivaRestServiceException;

    @Path( "removeScanningTaskFromQueue/{repositoryId}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_RUN_INDEXER )
    Boolean removeScanningTaskFromQueue( @PathParam( "repositoryId" ) String repositoryId )
        throws ArchivaRestServiceException;

    @Path( "scanRepositoryNow" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_RUN_INDEXER )
    Boolean scanRepositoryNow( @QueryParam( "repositoryId" ) String repositoryId,
                               @QueryParam( "fullScan" ) boolean fullScan )
        throws ArchivaRestServiceException;

    @Path( "copyArtifact" )
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noPermission = true )
    /**
     * permission are checked in impl
     * will copy an artifact from the source repository to the target repository
     */
    Boolean copyArtifact( ArtifactTransferRequest artifactTransferRequest )
        throws ArchivaRestServiceException;

}
