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
     * @param session
     * @param repositoryId the repository the project is in
     * @param project      the project metadata to create or update
     */
    void updateProject( RepositorySession session, String repositoryId, ProjectMetadata project )
        throws MetadataRepositoryException;

    void updateArtifact( RepositorySession session, String repositoryId, String namespace, String projectId, String projectVersion,
                         ArtifactMetadata artifactMeta )
        throws MetadataRepositoryException;

    void updateProjectVersion( RepositorySession session, String repositoryId, String namespace, String projectId,
                               ProjectVersionMetadata versionMetadata )
        throws MetadataRepositoryException;

    /**
     * create the namespace in the repository. (if not exist)
     *
     *
     * @param session
     * @param repositoryId
     * @param namespace
     * @throws MetadataRepositoryException
     */
    void updateNamespace( RepositorySession session, String repositoryId, String namespace )
        throws MetadataRepositoryException;

    List<String> getMetadataFacets( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException;

    /**
     *
     * @param session
     * @param repositoryId
     * @param facetId
     * @return true if the repository datas for this facetId
     * @throws MetadataRepositoryException
     * @since 1.4-M4
     */
    boolean hasMetadataFacet( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException;

    MetadataFacet getMetadataFacet( RepositorySession session, String repositoryId, String facetId, String name )
        throws MetadataRepositoryException;

    void addMetadataFacet( RepositorySession session, String repositoryId, MetadataFacet metadataFacet )
        throws MetadataRepositoryException;

    void removeMetadataFacets( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException;

    void removeMetadataFacet( RepositorySession session, String repositoryId, String facetId, String name )
        throws MetadataRepositoryException;

    /**
     * if startTime or endTime are <code>null</code> they are not used for search
     *
     *
     * @param session
     * @param repositoryId
     * @param startTime    can be <code>null</code>
     * @param endTime      can be <code>null</code>
     * @return
     * @throws MetadataRepositoryException
     */
    List<ArtifactMetadata> getArtifactsByDateRange( RepositorySession session, String repositoryId, Date startTime, Date endTime )
        throws MetadataRepositoryException;

    Collection<ArtifactMetadata> getArtifactsByChecksum( RepositorySession session, String repositoryId, String checksum )
        throws MetadataRepositoryException;

    /**
     * Get artifacts with a project version metadata key that matches the passed value.
     *  
     *
     * @param session
     * @param key
     * @param value
     * @param repositoryId can be null, meaning search in all repositories
     * @return a list of artifacts
     * @throws MetadataRepositoryException
     */
    List<ArtifactMetadata> getArtifactsByProjectVersionMetadata( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException;

    /**
     * Get artifacts with an artifact metadata key that matches the passed value.
     *  
     *
     * @param session
     * @param key
     * @param value
     * @param repositoryId can be null, meaning search in all repositories
     * @return a list of artifacts
     * @throws MetadataRepositoryException
     */
    List<ArtifactMetadata> getArtifactsByMetadata( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException;

    /**
     * Get artifacts with a property key that matches the passed value.
     * Possible keys are 'scm.url', 'org.name', 'url', 'mailingList.0.name', 'license.0.name',...
     *  
     *
     * @param session
     * @param key
     * @param value
     * @param repositoryId can be null, meaning search in all repositories
     * @return a list of artifacts
     * @throws MetadataRepositoryException
     */
    List<ArtifactMetadata> getArtifactsByProperty( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException;

    void removeArtifact( RepositorySession session, String repositoryId, String namespace, String project, String version, String id )
        throws MetadataRepositoryException;

    /**
     * used for deleting timestamped version of SNAPSHOT artifacts
     *
     *
     * @param session
     * @param artifactMetadata the artifactMetadata with the timestamped version (2.0-20120618.214135-2)
     * @param baseVersion      the base version of the snapshot (2.0-SNAPSHOT)
     * @throws MetadataRepositoryException
     * @since 1.4-M3
     */
    void removeArtifact( RepositorySession session, ArtifactMetadata artifactMetadata, String baseVersion )
        throws MetadataRepositoryException;

    /**
     * FIXME need a unit test!!!
     * Only remove {@link MetadataFacet} for the artifact
     *
     *
     * @param session
     * @param repositoryId
     * @param namespace
     * @param project
     * @param version
     * @param metadataFacet
     * @throws MetadataRepositoryException
     * @since 1.4-M3
     */
    void removeArtifact( RepositorySession session, String repositoryId, String namespace, String project, String version,
                         MetadataFacet metadataFacet )
        throws MetadataRepositoryException;

    /**
     * Delete a repository's metadata. This includes all associated metadata facets.
     *
     * @param session
     * @param repositoryId the repository to delete
     */
    void removeRepository( RepositorySession session, String repositoryId )
        throws MetadataRepositoryException;

    /**
     *
     * @param session
     * @param repositoryId
     * @param namespace    (groupId for maven )
     * @throws MetadataRepositoryException
     * @since 1.4-M3
     */
    void removeNamespace( RepositorySession session, String repositoryId, String namespace )
        throws MetadataRepositoryException;

    List<ArtifactMetadata> getArtifacts( RepositorySession session, String repositoryId )
        throws MetadataRepositoryException;

    /**
     * basically just checking it exists not complete data returned
     *
     *
     * @param session
     * @param repoId
     * @param namespace
     * @param projectId
     * @return
     * @throws MetadataResolutionException
     */
    ProjectMetadata getProject( RepositorySession session, String repoId, String namespace, String projectId )
        throws MetadataResolutionException;

    ProjectVersionMetadata getProjectVersion( RepositorySession session, String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataResolutionException;

    Collection<String> getArtifactVersions( RepositorySession session, String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataResolutionException;

    /**
     * Retrieve project references from the metadata repository. Note that this is not built into the content model for
     * a project version as a reference may be present (due to reverse-lookup of dependencies) before the actual
     * project is, and we want to avoid adding a stub model to the content repository.
     *
     *
     * @param session
     * @param repoId         the repository ID to look within
     * @param namespace      the namespace of the project to get references to
     * @param projectId      the identifier of the project to get references to
     * @param projectVersion the version of the project to get references to
     * @return a list of project references
     */
    Collection<ProjectVersionReference> getProjectReferences( RepositorySession session, String repoId, String namespace, String projectId,
                                                              String projectVersion )
        throws MetadataResolutionException;

    Collection<String> getRootNamespaces( RepositorySession session, String repoId )
        throws MetadataResolutionException;

    /**
     *
     * @param session
     * @param repoId
     * @param namespace
     * @return {@link Collection} of child namespaces of the namespace argument
     * @throws MetadataResolutionException
     */
    Collection<String> getNamespaces( RepositorySession session, String repoId, String namespace )
        throws MetadataResolutionException;

    /**
     *
     * @param session
     * @param repoId
     * @param namespace
     * @return
     * @throws MetadataResolutionException
     */
    Collection<String> getProjects( RepositorySession session, String repoId, String namespace )
        throws MetadataResolutionException;

    /**
     *
     * @param session
     * @param repoId
     * @param namespace
     * @param projectId
     * @return
     * @throws MetadataResolutionException
     */
    Collection<String> getProjectVersions( RepositorySession session, String repoId, String namespace, String projectId )
        throws MetadataResolutionException;

    /**
     *
     * @param session
     * @param repoId
     * @param namespace
     * @param projectId
     * @param projectVersion
     * @throws MetadataRepositoryException
     * @since 1.4-M4
     */
    void removeProjectVersion( RepositorySession session, String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataRepositoryException;

    /**
     *
     * @param session
     * @param repoId
     * @param namespace
     * @param projectId
     * @param projectVersion
     * @return
     * @throws MetadataResolutionException
     */
    Collection<ArtifactMetadata> getArtifacts( RepositorySession session, String repoId, String namespace, String projectId,
                                               String projectVersion )
        throws MetadataResolutionException;

    /**
     * remove a project
     *
     *
     * @param session
     * @param repositoryId
     * @param namespace
     * @param projectId
     * @throws MetadataRepositoryException
     * @since 1.4-M4
     */
    void removeProject( RepositorySession session, String repositoryId, String namespace, String projectId )
        throws MetadataRepositoryException;


    void close()
        throws MetadataRepositoryException;


    boolean canObtainAccess( Class<?> aClass );

    <T> T obtainAccess( RepositorySession session,  Class<T> aClass )
        throws MetadataRepositoryException;

    /**
     * Full text artifacts search.
     *  
     *
     * @param session
     * @param repositoryId can be null to search in all repositories
     * @param text
     * @param exact running an exact search, the value must exactly match the text.
     * @return a list of artifacts
     * @throws MetadataRepositoryException
     */
    List<ArtifactMetadata> searchArtifacts( RepositorySession session, String repositoryId, String text, boolean exact )
        throws MetadataRepositoryException;

    /**
     * Full text artifacts search inside the specified key.
     *  
     *
     * @param session
     * @param repositoryId can be null to search in all repositories
     * @param key search only inside this key
     * @param text
     * @param exact running an exact search, the value must exactly match the text.
     * @return a list of artifacts
     * @throws MetadataRepositoryException
     */
    List<ArtifactMetadata> searchArtifacts( RepositorySession session, String repositoryId, String key, String text, boolean exact )
        throws MetadataRepositoryException;

}
