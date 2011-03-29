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
{
    /**
     * Update metadata for a particular project in the metadata repository, or create it if it does not already exist.
     *
     * @param repositoryId the repository the project is in
     * @param project      the project metadata to create or update
     */
    void updateProject( String repositoryId, ProjectMetadata project )
        throws MetadataRepositoryException;

    void updateArtifact( String repositoryId, String namespace, String projectId, String projectVersion,
                         ArtifactMetadata artifactMeta )
        throws MetadataRepositoryException;

    void updateProjectVersion( String repositoryId, String namespace, String projectId,
                               ProjectVersionMetadata versionMetadata )
        throws MetadataRepositoryException;

    void updateNamespace( String repositoryId, String namespace )
        throws MetadataRepositoryException;

    List<String> getMetadataFacets( String repositoryId, String facetId )
        throws MetadataRepositoryException;

    MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name )
        throws MetadataRepositoryException;

    void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
        throws MetadataRepositoryException;

    void removeMetadataFacets( String repositoryId, String facetId )
        throws MetadataRepositoryException;

    void removeMetadataFacet( String repositoryId, String facetId, String name )
        throws MetadataRepositoryException;

    List<ArtifactMetadata> getArtifactsByDateRange( String repositoryId, Date startTime, Date endTime )
        throws MetadataRepositoryException;

    // TODO: remove from API, just use configuration
    Collection<String> getRepositories()
        throws MetadataRepositoryException;

    List<ArtifactMetadata> getArtifactsByChecksum( String repositoryId, String checksum )
        throws MetadataRepositoryException;

    void removeArtifact( String repositoryId, String namespace, String project, String version, String id )
        throws MetadataRepositoryException;

    /**
     * Delete a repository's metadata. This includes all associated metadata facets.
     *
     * @param repositoryId the repository to delete
     */
    void removeRepository( String repositoryId )
        throws MetadataRepositoryException;

    List<ArtifactMetadata> getArtifacts( String repositoryId )
        throws MetadataRepositoryException;

    ProjectMetadata getProject( String repoId, String namespace, String projectId )
        throws MetadataResolutionException;

    ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataResolutionException;

    Collection<String> getArtifactVersions( String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataResolutionException;

    /**
     * Retrieve project references from the metadata repository. Note that this is not built into the content model for
     * a project version as a reference may be present (due to reverse-lookup of dependencies) before the actual
     * project is, and we want to avoid adding a stub model to the content repository.
     *
     * @param repoId         the repository ID to look within
     * @param namespace      the namespace of the project to get references to
     * @param projectId      the identifier of the project to get references to
     * @param projectVersion the version of the project to get references to
     * @return a list of project references
     */
    Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                              String projectVersion )
        throws MetadataResolutionException;

    Collection<String> getRootNamespaces( String repoId )
        throws MetadataResolutionException;

    Collection<String> getNamespaces( String repoId, String namespace )
        throws MetadataResolutionException;

    Collection<String> getProjects( String repoId, String namespace )
        throws MetadataResolutionException;

    Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
        throws MetadataResolutionException;

    Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                               String projectVersion )
        throws MetadataResolutionException;

    void save()
        throws MetadataRepositoryException;

    void close();

    void revert()
        throws MetadataRepositoryException;

    boolean canObtainAccess( Class<?> aClass );

    Object obtainAccess( Class<?> aClass );
}
