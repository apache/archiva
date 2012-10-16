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
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.maven2.model.TreeEntry;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.rest.api.model.ArtifactContent;
import org.apache.archiva.rest.api.model.ArtifactContentEntry;
import org.apache.archiva.rest.api.model.BrowseResult;
import org.apache.archiva.rest.api.model.Entry;
import org.apache.archiva.rest.api.model.MetadataAddRequest;
import org.apache.archiva.rest.api.model.VersionsList;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Path ("/browseService/")
public interface BrowseService
{
    @Path ("rootGroups")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    BrowseResult getRootGroups( @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("browseGroupId/{groupId}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    /**
     * @param groupId groupId to browse
     * @param repositoryId optionnal (repository to browse if <code>null</code> all available user repositories are used)
     */
    BrowseResult browseGroupId( @PathParam ("groupId") String groupId,
                                @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("versionsList/{g}/{a}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    VersionsList getVersionsList( @PathParam ("g") String groupId, @PathParam ("a") String artifactId,
                                  @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("projectVersionMetadata/{g}/{a}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    ProjectVersionMetadata getProjectVersionMetadata( @PathParam ("g") String groupId,
                                                      @PathParam ("a") String artifactId,
                                                      @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("projectVersionMetadata/{g}/{a}/{v}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    ProjectVersionMetadata getProjectMetadata( @PathParam ("g") String groupId, @PathParam ("a") String artifactId,
                                               @PathParam ("v") String version,
                                               @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("userRepositories")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    /**
     * @return List of managed repositories current user can read
     */
    List<ManagedRepository> getUserRepositories()
        throws ArchivaRestServiceException;

    @Path ("treeEntries/{g}/{a}/{v}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    /**
     * return the dependency Tree for an artifacts
     * <b>the List result has only one entry</b>
     */
    List<TreeEntry> getTreeEntries( @PathParam ("g") String groupId, @PathParam ("a") String artifactId,
                                    @PathParam ("v") String version, @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("dependees/{g}/{a}/{v}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    /**
     * List of artifacts using the artifact passed in parameter.
     */
    List<Artifact> getDependees( @PathParam ("g") String groupId, @PathParam ("a") String artifactId,
                                 @PathParam ("v") String version, @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("metadatas/{g}/{a}/{v}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    List<Entry> getMetadatas( @PathParam ("g") String groupId, @PathParam ("a") String artifactId,
                              @PathParam ("v") String version, @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("metadata/{g}/{a}/{v}/{key}/{value}")
    @PUT
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = false, noRestriction = false, permissions = "archiva-add-metadata")
    Boolean addMetadata( @PathParam ("g") String groupId, @PathParam ("a") String artifactId,
                         @PathParam ("v") String version, @PathParam ("key") String key,
                         @PathParam ("value") String value, @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("metadata/{g}/{a}/{v}/{key}")
    @DELETE
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = false, noRestriction = false, permissions = "archiva-add-metadata")
    Boolean deleteMetadata( @PathParam ("g") String groupId, @PathParam ("a") String artifactId,
                            @PathParam ("v") String version, @PathParam ("key") String key,
                            @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path( "importMetadata" )
    @POST
    @RedbackAuthorization( noPermission = false, noRestriction = false, permissions = "archiva-add-metadata")
    Boolean importMetadata( MetadataAddRequest metadataAddRequest, @QueryParam ("repository") String repository )
        throws ArchivaRestServiceException;

    @Path ("artifactContentEntries/{g}/{a}/{v}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    List<ArtifactContentEntry> getArtifactContentEntries( @PathParam ("g") String groupId,
                                                          @PathParam ("a") String artifactId,
                                                          @PathParam ("v") String version,
                                                          @QueryParam ("c") String classifier,
                                                          @QueryParam ("t") String type, @QueryParam ("p") String path,
                                                          @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("artifactDownloadInfos/{g}/{a}/{v}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    List<Artifact> getArtifactDownloadInfos( @PathParam ("g") String groupId, @PathParam ("a") String artifactId,
                                             @PathParam ("v") String version,
                                             @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("artifactContentText/{g}/{a}/{v}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    /**
     * if path is empty content of the file is returned (for pom view)
     */
    ArtifactContent getArtifactContentText( @PathParam ("g") String groupId, @PathParam ("a") String artifactId,
                                            @PathParam ("v") String version, @QueryParam ("c") String classifier,
                                            @QueryParam ("t") String type, @QueryParam ("p") String path,
                                            @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("artifactAvailable/{g}/{a}/{v}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    /**
     * verify if an artifact is available locally if not download from proxies will be try
     * @since 1.4-M3
     */
    Boolean artifactAvailable( @PathParam ("g") String groupId, @PathParam ("a") String artifactId,
                               @PathParam ("v") String version, @QueryParam ("repositoryId") String repositoryId )
        throws ArchivaRestServiceException;

    @Path ("artifacts/{r}")
    @GET
    @Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization (noPermission = true, noRestriction = true)
    /**
     *
     * return List of all artifacts from this repository
     * @param repositoryId
     * @return
     * @throws ArchivaRestServiceException
     * @since 1.4-M3
     */
    List<Artifact> getArtifacts( @PathParam ( "r" ) String repositoryId )
        throws ArchivaRestServiceException;
}
