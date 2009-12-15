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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;

public interface MetadataRepository
    extends MetadataResolver
{
    /**
     * Update metadata for a particular project in the metadata repository, or create it if it does not already exist.
     *
     * @param repoId  the repository the project is in
     * @param project the project metadata to create or update
     */
    void updateProject( String repoId, ProjectMetadata project );

    void updateArtifact( String repoId, String namespace, String projectId, String projectVersion,
                         ArtifactMetadata artifactMeta );

    void updateProjectVersion( String repoId, String namespace, String projectId,
                               ProjectVersionMetadata versionMetadata );

    void updateProjectReference( String repoId, String namespace, String projectId, String projectVersion,
                                 ProjectVersionReference reference );

    void updateNamespace( String repoId, String namespace );

    List<String> getMetadataFacets( String repodId, String facetId );

    MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name );

    void addMetadataFacet( String repositoryId, String facetId, MetadataFacet metadataFacet );

    void removeMetadataFacets( String repositoryId, String facetId );

    void removeMetadataFacet( String repoId, String facetId, String name );

    List<ArtifactMetadata> getArtifactsByDateRange( String repoId, Date startTime, Date endTime );

    Collection<String> getRepositories();

    List<ArtifactMetadata> getArtifactsByChecksum( String repoId, String checksum );
}
