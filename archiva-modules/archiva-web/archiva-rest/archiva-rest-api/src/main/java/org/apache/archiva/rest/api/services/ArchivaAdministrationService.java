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

import org.apache.archiva.admin.model.beans.FileType;
import org.apache.archiva.admin.model.beans.LegacyArtifactPath;
import org.apache.archiva.admin.model.beans.OrganisationInformation;
import org.apache.archiva.admin.model.beans.UiConfiguration;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.codehaus.plexus.redback.authorization.RedbackAuthorization;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Path( "/archivaAdministrationService/" )
public interface ArchivaAdministrationService
{
    @Path( "getLegacyArtifactPaths" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    List<LegacyArtifactPath> getLegacyArtifactPaths()
        throws ArchivaRestServiceException;

    @Path( "addLegacyArtifactPath" )
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    void addLegacyArtifactPath( LegacyArtifactPath legacyArtifactPath )
        throws ArchivaRestServiceException;

    @Path( "deleteLegacyArtifactPath" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean deleteLegacyArtifactPath( @QueryParam( "path" ) String path )
        throws ArchivaRestServiceException;

    @Path( "addFileTypePattern" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean addFileTypePattern( @QueryParam( "fileTypeId" ) String fileTypeId, @QueryParam( "pattern" ) String pattern )
        throws ArchivaRestServiceException;

    @Path( "removeFileTypePattern" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean removeFileTypePattern( @QueryParam( "fileTypeId" ) String fileTypeId,
                                   @QueryParam( "pattern" ) String pattern )
        throws ArchivaRestServiceException;

    @Path( "getFileType" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    FileType getFileType( @QueryParam( "fileTypeId" ) String fileTypeId )
        throws ArchivaRestServiceException;

    @Path( "addFileType" )
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    void addFileType( FileType fileType )
        throws ArchivaRestServiceException;

    @Path( "removeFileType" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean removeFileType( @QueryParam( "fileTypeId" ) String fileTypeId )
        throws ArchivaRestServiceException;

    @Path( "addKnownContentConsumer" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean addKnownContentConsumer( @QueryParam( "knownContentConsumer" ) String knownContentConsumer )
        throws ArchivaRestServiceException;

    @Path( "setKnownContentConsumers" )
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    void setKnownContentConsumers( List<String> knownContentConsumers )
        throws ArchivaRestServiceException;


    @Path( "removeKnownContentConsumer" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean removeKnownContentConsumer( @QueryParam( "knownContentConsumer" ) String knownContentConsumer )
        throws ArchivaRestServiceException;

    @Path( "addInvalidContentConsumer" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean addInvalidContentConsumer( @QueryParam( "invalidContentConsumer" ) String invalidContentConsumer )
        throws ArchivaRestServiceException;

    @Path( "setInvalidContentConsumers" )
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    void setInvalidContentConsumers( List<String> invalidContentConsumers )
        throws ArchivaRestServiceException;

    @Path( "removeInvalidContentConsumer" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    Boolean removeInvalidContentConsumer( @QueryParam( "invalidContentConsumer" ) String invalidContentConsumer )
        throws ArchivaRestServiceException;

    @Path( "getFileTypes" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    List<FileType> getFileTypes()
        throws ArchivaRestServiceException;

    @Path( "getKnownContentConsumers" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    List<String> getKnownContentConsumers()
        throws ArchivaRestServiceException;

    @Path( "getInvalidContentConsumers" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    List<String> getInvalidContentConsumers()
        throws ArchivaRestServiceException;

    @Path( "getOrganisationInformation" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    OrganisationInformation getOrganisationInformation()
        throws ArchivaRestServiceException;

    @Path( "setOrganisationInformation" )
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    void setOrganisationInformation( OrganisationInformation organisationInformation )
        throws ArchivaRestServiceException;

    @Path( "getUiConfiguration" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    UiConfiguration getUiConfiguration()
        throws ArchivaRestServiceException;

    @Path( "setUiConfiguration" )
    @POST
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permission = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    void setUiConfiguration( UiConfiguration uiConfiguration )
        throws ArchivaRestServiceException;
}

