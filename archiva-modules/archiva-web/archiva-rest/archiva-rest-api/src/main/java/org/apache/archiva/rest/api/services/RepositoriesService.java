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

import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.repository.scanner.RepositoryScanStatistics;
import org.apache.archiva.rest.api.model.ArtifactTransferRequest;
import org.apache.archiva.rest.api.model.StringList;
import org.apache.archiva.security.common.ArchivaRoleConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Path ("/repositoriesService/")
public interface RepositoriesService
{

    /**
     * index repository
     */
    @Path ("scanRepository")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (permissions = ArchivaRoleConstants.OPERATION_RUN_INDEXER)
    Boolean scanRepository( @QueryParam ("repositoryId") String repositoryId,
                            @QueryParam ("fullScan") boolean fullScan )
        throws ArchivaRestServiceException;


    /**
     * scan directories
     * @since 1.4-M3
     */
    @Path ("scanRepositoryDirectoriesNow/{repositoryId}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (permissions = ArchivaRoleConstants.OPERATION_RUN_INDEXER)
    RepositoryScanStatistics scanRepositoryDirectoriesNow( @PathParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;


    @Path ("alreadyScanning/{repositoryId}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (permissions = ArchivaRoleConstants.OPERATION_RUN_INDEXER)
    Boolean alreadyScanning( @PathParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("removeScanningTaskFromQueue/{repositoryId}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (permissions = ArchivaRoleConstants.OPERATION_RUN_INDEXER)
    Boolean removeScanningTaskFromQueue( @PathParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("scanRepositoryNow")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (permissions = ArchivaRoleConstants.OPERATION_RUN_INDEXER)
    Boolean scanRepositoryNow( @QueryParam ("repositoryId") String repositoryId,
                               @QueryParam ("fullScan") boolean fullScan )
        throws ArchivaRestServiceException;

    /**
     * permissions are checked in impl
     * will copy an artifact from the source repository to the target repository
     */
    @Path ("copyArtifact")
    @POST
    @Consumes ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (noPermission = true)
    Boolean copyArtifact( ArtifactTransferRequest artifactTransferRequest )
        throws ArchivaRestServiceException;

    @Path ("scheduleDownloadRemoteIndex")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (permissions = ArchivaRoleConstants.OPERATION_RUN_INDEXER)
    Boolean scheduleDownloadRemoteIndex( @QueryParam ("repositoryId") String repositoryId,
                                         @QueryParam ("now") boolean now,
                                         @QueryParam ("fullDownload") boolean fullDownload )
        throws ArchivaRestServiceException;


    /**
     * <b>permissions are checked in impl</b>
     * @since 1.4-M2
     */
    @Path ("deleteArtifact")
    @POST
    @Consumes ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (noPermission = true)
    Boolean deleteArtifact( Artifact artifact )
        throws ArchivaRestServiceException;

    /**
     * <b>permissions are checked in impl</b>
     * @since 1.4-M4
     */
    @Path ("projectVersion/{repositoryId}/{namespace}/{projectId}/{version}")
    @DELETE
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (noPermission = true)
    Boolean removeProjectVersion( @PathParam ( "repositoryId" ) String repositoryId,
                                  @PathParam ( "namespace" ) String namespace, @PathParam ( "projectId" ) String projectId,
                                  @PathParam ( "version" ) String version )
        throws ArchivaRestServiceException;

    @Path ("isAuthorizedToDeleteArtifacts/{repositoryId}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    Boolean isAuthorizedToDeleteArtifacts( @PathParam ("repositoryId") String repoId )
        throws ArchivaRestServiceException;

    /**
     * <b>permissions are checked in impl</b>
     * @since 1.4-M3
     */
    @Path ("deleteGroupId")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (noPermission = true)
    Boolean deleteGroupId( @QueryParam ("groupId") String groupId, @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    /**
     * <b>permissions are checked in impl</b>
     * @since 1.4-M4
     */
    @Path ("project/{repositoryId}/{groupId}/{projectId}")
    @DELETE
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (noPermission = true)
    Boolean deleteProject( @PathParam ("groupId") String groupId, @PathParam ("projectId") String projectId,
                           @PathParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    /**
     * @since 2.0
     */
    @Path ("runningRemoteDownloadIds")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization (noPermission = true)
    StringList getRunningRemoteDownloadIds();

}
