package org.apache.archiva.web.api;
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

import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.web.model.FileMetadata;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Path( "/fileUploadService/" )
public interface FileUploadService
{

    String FILES_SESSION_KEY = FileUploadService.class.getName() + "files_session_key";

    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD )
    FileMetadata post( MultipartBody multipartBody )
        throws ArchivaRestServiceException;

    @Path( "{fileName}" )
    @DELETE
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD )
    Boolean deleteFile( @PathParam( "fileName" ) String fileName )
        throws ArchivaRestServiceException;


    @Path( "sessionFileMetadatas" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD )
    List<FileMetadata> getSessionFileMetadatas()
        throws ArchivaRestServiceException;

    @Path( "save/{repositoryId}/{groupId}/{artifactId}/{version}/{packaging}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( resource = "{repositoryId}", permissions = ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD )
    Boolean save( @PathParam( "repositoryId" ) String repositoryId, @PathParam( "groupId" ) String groupId,
                  @PathParam( "artifactId" ) String artifactId, @PathParam( "version" ) String version,
                  @PathParam( "packaging" ) String packaging, @QueryParam( "generatePom" ) boolean generatePom )
        throws ArchivaRestServiceException;


    @Path( "clearUploadedFiles" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD )
    Boolean clearUploadedFiles()
        throws ArchivaRestServiceException;

}
