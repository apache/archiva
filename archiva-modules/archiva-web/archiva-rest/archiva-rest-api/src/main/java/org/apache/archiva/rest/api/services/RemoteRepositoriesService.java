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

import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.redback.authorization.RedbackAuthorization;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Path("/remoteRepositoriesService/")
public interface RemoteRepositoriesService
{
    @Path("getRemoteRepositories")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    List<RemoteRepository> getRemoteRepositories()
        throws ArchivaRestServiceException;

    @Path("getRemoteRepository/{repositoryId}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    RemoteRepository getRemoteRepository( @PathParam("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path("deleteRemoteRepository/{repositoryId}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    Boolean deleteRemoteRepository( @PathParam("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;


    @Path("addRemoteRepository")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    Boolean addRemoteRepository( RemoteRepository remoteRepository )
        throws ArchivaRestServiceException;


    @Path("updateRemoteRepository")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    Boolean updateRemoteRepository( RemoteRepository remoteRepository )
        throws ArchivaRestServiceException;

    @Path("checkRemoteConnectivity/{repositoryId}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @RedbackAuthorization(permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION)
    Boolean checkRemoteConnectivity( @PathParam( "repositoryId" ) String repositoryId )
        throws ArchivaRestServiceException;
}
