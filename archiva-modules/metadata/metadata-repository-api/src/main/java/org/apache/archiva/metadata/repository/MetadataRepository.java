package org.apache.archiva.metadata.repository;

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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface MetadataRepository
    extends MetadataResolver
{
    /**
     * Update metadata for a particular project in the metadata repository, or create it if it does not already exist.
     *
     * @param repositoryId  the repository the project is in
     * @param project the project metadata to create or update
     */
    void updateProject( String repositoryId, ProjectMetadata project );

    void updateArtifact( String repositoryId, String namespace, String projectId, String projectVersion,
                         ArtifactMetadata artifactMeta );

    void updateProjectVersion( String repositoryId, String namespace, String projectId,
                               ProjectVersionMetadata versionMetadata );

    void updateProjectReference( String repositoryId, String namespace, String projectId, String projectVersion,
                                 ProjectVersionReference reference );

    void updateNamespace( String repositoryId, String namespace );

    List<String> getMetadataFacets( String repositoryId, String facetId );

    MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name );

    void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet );

    void removeMetadataFacets( String repositoryId, String facetId );

    void removeMetadataFacet( String repositoryId, String facetId, String name );

    List<ArtifactMetadata> getArtifactsByDateRange( String repositoryId, Date startTime, Date endTime );

    // TODO: remove from API, just use configuration
    Collection<String> getRepositories();

    List<ArtifactMetadata> getArtifactsByChecksum( String repositoryId, String checksum );

    void deleteArtifact( String repositoryId, String namespace, String project, String version, String id );

    /**
     * Delete a repository's metadata. This includes all associated metadata facets.
     * @param repositoryId the repository to delete
     */
    void deleteRepository( String repositoryId );

    List<ArtifactMetadata> getArtifacts(String repositoryId);
}
