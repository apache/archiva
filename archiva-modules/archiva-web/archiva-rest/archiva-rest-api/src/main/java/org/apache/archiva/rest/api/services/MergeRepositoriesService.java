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
import org.apache.archiva.security.common.ArchivaRoleConstants;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * provide REST services on the top of stage merge repository plugin
 *
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Path ("/mergeRepositoriesService/")
public interface MergeRepositoriesService
{

    /**
     * <b>permissions are checked in impl</b>
     * @since 1.4-M3
     */
    @Path ("mergeConflictedArtifacts/{sourceRepositoryId}/{targetRepositoryId}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (permissions = ArchivaRoleConstants.OPERATION_MERGE_REPOSITORY)
    List<Artifact> getMergeConflictedArtifacts( @PathParam ("sourceRepositoryId") String sourceRepositoryId,
                                                @PathParam ("targetRepositoryId") String targetRepositoryId )
        throws ArchivaRestServiceException;

    /**
     * <b>permissions are checked in impl</b>
     * @since 1.4-M3
     */
    @Path ("mergeRepositories/{sourceRepositoryId}/{targetRepositoryId}/{skipConflicts}")
    @GET
    @RedbackAuthorization (permissions = ArchivaRoleConstants.OPERATION_MERGE_REPOSITORY)
    void mergeRepositories( @PathParam ("sourceRepositoryId") String sourceRepositoryId,
                            @PathParam ("targetRepositoryId") String targetRepositoryId,
                            @PathParam ("skipConflicts") boolean skipConflicts )
        throws ArchivaRestServiceException;
}
