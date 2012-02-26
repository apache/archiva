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

import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.rest.api.model.BrowseResult;
import org.apache.archiva.rest.api.model.VersionsList;
import org.codehaus.plexus.redback.authorization.RedbackAuthorization;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Path( "/browseService/" )
public interface BrowseService
{
    @Path( "rootGroups" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    BrowseResult getRootGroups()
        throws ArchivaRestServiceException;

    @Path( "browseGroupId/{groupId}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    BrowseResult browseGroupId( @PathParam( "groupId" ) String groupId )
        throws ArchivaRestServiceException;

    @Path( "versionsList/{g}/{a}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    VersionsList getVersionsList( @PathParam( "g" ) String groupId, @PathParam( "a" ) String artifactId )
        throws ArchivaRestServiceException;

    @Path( "projectVersionMetadata/{g}/{a}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    ProjectVersionMetadata getProjectVersionMetadata( @PathParam( "g" ) String groupId,
                                                      @PathParam( "a" ) String artifactId )
        throws ArchivaRestServiceException;
}
