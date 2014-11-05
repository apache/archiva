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

    /**
     * create the namespace in the repository. (if not exist)
     *
     * @param repositoryId
     * @param namespace
     * @throws MetadataRepositoryException
     */
    void updateNamespace( String repositoryId, String namespace )
        throws MetadataRepositoryException;

    List<String> getMetadataFacets( String repositoryId, String facetId )
        throws MetadataRepositoryException;

    /**
     * @param repositoryId
     * @param facetId
     * @return true if the repository datas for this facetId
     * @throws MetadataRepositoryException
     * @since 1.4-M4
     */
    boolean hasMetadataFacet( String repositoryId, String facetId )
        throws MetadataRepositoryException;

    MetadataFacet getMetadataFacet( String repositoryId, String facetId, String name )
        throws MetadataRepositoryException;

    void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
        throws MetadataRepositoryException;

    void removeMetadataFacets( String repositoryId, String facetId )
        throws MetadataRepositoryException;

    void removeMetadataFacet( String repositoryId, String facetId, String name )
        throws MetadataRepositoryException;

    /**
     * if startTime or endTime are <code>null</code> they are not used for search
     *
     * @param repositoryId
     * @param startTime    can be <code>null</code>
     * @param endTime      can be <code>null</code>
     * @return
     * @throws MetadataRepositoryException
     */
    List<ArtifactMetadata> getArtifactsByDateRange( String repositoryId, Date startTime, Date endTime )
        throws MetadataRepositoryException;

    // TODO: remove from API, just use configuration
    Collection<String> getRepositories()
        throws MetadataRepositoryException;

    Collection<ArtifactMetadata> getArtifactsByChecksum( String repositoryId, String checksum )
        throws MetadataRepositoryException;

    /**
     * Get artifacts with a project version metadata key that matches the passed value.
     *  
     * @param key
     * @param value
     * @param repositoryId can be null, meaning search in all repositories
     * @return a list of artifacts
     * @throws MetadataRepositoryException
     */
    List<ArtifactMetadata> getArtifactsByProjectVersionMetadata( String key, String value, String repositoryId )
        throws MetadataRepositoryException;

    /**
     * Get artifacts with an artifact metadata key that matches the passed value.
     *  
     * @param key
     * @param value
     * @param repositoryId can be null, meaning search in all repositories
     * @return a list of artifacts
     * @throws MetadataRepositoryException
     */
    List<ArtifactMetadata> getArtifactsByMetadata( String key, String value, String repositoryId )
        throws MetadataRepositoryException;

    /**
     * Get artifacts with a property key that matches the passed value.
     * Possible keys are 'scm.url', 'org.name', 'url', 'mailingList.0.name', 'license.0.name',...
     *  
     * @param key
     * @param value
     * @param repositoryId can be null, meaning search in all repositories
     * @return a list of artifacts
     * @throws MetadataRepositoryException
     */
    List<ArtifactMetadata> getArtifactsByProperty( String key, String value, String repositoryId )
        throws MetadataRepositoryException;

    void removeArtifact( String repositoryId, String namespace, String project, String version, String id )
        throws MetadataRepositoryException;

    /**
     * used for deleting timestamped version of SNAPSHOT artifacts
     *
     * @param artifactMetadata the artifactMetadata with the timestamped version (2.0-20120618.214135-2)
     * @param baseVersion      the base version of the snapshot (2.0-SNAPSHOT)
     * @throws MetadataRepositoryException
     * @since 1.4-M3
     */
    void removeArtifact( ArtifactMetadata artifactMetadata, String baseVersion )
        throws MetadataRepositoryException;

    /**
     * FIXME need a unit test!!!
     * Only remove {@link MetadataFacet} for the artifact
     *
     * @param repositoryId
     * @param namespace
     * @param project
     * @param version
     * @param metadataFacet
     * @throws MetadataRepositoryException
     * @since 1.4-M3
     */
    void removeArtifact( String repositoryId, String namespace, String project, String version,
                         MetadataFacet metadataFacet )
        throws MetadataRepositoryException;

    /**
     * Delete a repository's metadata. This includes all associated metadata facets.
     *
     * @param repositoryId the repository to delete
     */
    void removeRepository( String repositoryId )
        throws MetadataRepositoryException;

    /**
     * @param repositoryId
     * @param namespace    (groupId for maven )
     * @throws MetadataRepositoryException
     * @since 1.4-M3
     */
    void removeNamespace( String repositoryId, String namespace )
        throws MetadataRepositoryException;

    List<ArtifactMetadata> getArtifacts( String repositoryId )
        throws MetadataRepositoryException;

    /**
     * basically just checking it exists not complete data returned
     *
     * @param repoId
     * @param namespace
     * @param projectId
     * @return
     * @throws MetadataResolutionException
     */
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

    /**
     * @param repoId
     * @param namespace
     * @return {@link Collection} of child namespaces of the namespace argument
     * @throws MetadataResolutionException
     */
    Collection<String> getNamespaces( String repoId, String namespace )
        throws MetadataResolutionException;

    /**
     * @param repoId
     * @param namespace
     * @return
     * @throws MetadataResolutionException
     */
    Collection<String> getProjects( String repoId, String namespace )
        throws MetadataResolutionException;

    /**
     * @param repoId
     * @param namespace
     * @param projectId
     * @return
     * @throws MetadataResolutionException
     */
    Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
        throws MetadataResolutionException;

    /**
     * @param repoId
     * @param namespace
     * @param projectId
     * @param projectVersion
     * @throws MetadataRepositoryException
     * @since 1.4-M4
     */
    void removeProjectVersion( String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataRepositoryException;

    /**
     * @param repoId
     * @param namespace
     * @param projectId
     * @param projectVersion
     * @return
     * @throws MetadataResolutionException
     */
    Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                               String projectVersion )
        throws MetadataResolutionException;

    /**
     * remove a project
     *
     * @param repositoryId
     * @param namespace
     * @param projectId
     * @throws MetadataRepositoryException
     * @since 1.4-M4
     */
    void removeProject( String repositoryId, String namespace, String projectId )
        throws MetadataRepositoryException;


    /**
     * <b>implementations can throw RuntimeException</b>
     */
    void save();


    void close()
        throws MetadataRepositoryException;

    /**
     * <b>implementations can throw RuntimeException</b>
     */
    void revert();

    boolean canObtainAccess( Class<?> aClass );

    <T> T obtainAccess( Class<T> aClass )
        throws MetadataRepositoryException;

    /**
     * Full text artifacts search.
     *  
     * @param text
     * @param repositoryId can be null to search in all repositories
     * @param exact running an exact search, the value must exactly match the text.  
     * @return a list of artifacts
     * @throws MetadataRepositoryException
     */
    List<ArtifactMetadata> searchArtifacts( String text, String repositoryId, boolean exact )
        throws MetadataRepositoryException;

    /**
     * Full text artifacts search inside the specified key.
     *  
     * @param key search only inside this key
     * @param text
     * @param repositoryId can be null to search in all repositories
     * @param exact running an exact search, the value must exactly match the text.  
     * @return a list of artifacts
     * @throws MetadataRepositoryException
     */
    List<ArtifactMetadata> searchArtifacts( String key, String text, String repositoryId, boolean exact )
        throws MetadataRepositoryException;

}
