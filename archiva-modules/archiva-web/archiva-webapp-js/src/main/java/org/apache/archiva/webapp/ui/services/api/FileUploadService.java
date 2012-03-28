package org.apache.archiva.webapp.ui.services.api;
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

import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.webapp.ui.services.model.FileMetadata;
import org.codehaus.plexus.redback.authorization.RedbackAuthorization;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Path( "/fileUploadService/" )
public interface FileUploadService
{

    //@Path( "upload" )
    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noRestriction = true )
    FileMetadata post( @QueryParam( "g" ) String groupId, @QueryParam( "a" ) String artifactId,
                       @QueryParam( "v" ) String version, @QueryParam( "p" ) String packaging,
                       @QueryParam( "c" ) String classifier, @QueryParam( "r" ) String repositoryId,
                       @QueryParam( "generatePom" ) String generatePom )
        throws ArchivaRestServiceException;

    @Path( "{fileName}" )
    @DELETE
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noRestriction = true )
    Boolean deleteFile( @PathParam( "fileName" ) String fileName )
        throws ArchivaRestServiceException;
}
