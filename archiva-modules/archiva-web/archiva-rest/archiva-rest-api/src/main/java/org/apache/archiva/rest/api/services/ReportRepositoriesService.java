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

import org.apache.archiva.metadata.model.facets.RepositoryProblemFacet;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatistics;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.security.common.ArchivaRoleConstants;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Date;
import java.util.List;

/**
 * ReportRepositoriesService
 *
 * @author Adrien Lecharpentier &lt;adrien.lecharpentier@zenika.com&gt;
 * @since 1.4-M3
 */
@Path( "/reportServices/" )
public interface ReportRepositoriesService
{

    @Path( "getStatisticsReport" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    List<RepositoryStatistics> getStatisticsReport( @QueryParam( "repository" ) List<String> repositoriesId,
                                                           @QueryParam( "rowCount" ) int rowCount,
                                                           @QueryParam( "startDate" ) Date startDate,
                                                           @QueryParam( "endDate" ) Date endDate )
        throws ArchivaRestServiceException;

    @Path( "getHealthReports/{repository}/{rowCount}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION )
    List<RepositoryProblemFacet> getHealthReport( @PathParam( "repository" ) String repository,
                                                         @QueryParam( "groupId" ) String groupId,
                                                         @PathParam( "rowCount" ) int rowCount )
        throws ArchivaRestServiceException;

}
