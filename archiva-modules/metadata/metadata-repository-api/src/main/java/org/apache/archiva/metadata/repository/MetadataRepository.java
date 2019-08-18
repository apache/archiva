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
import org.apache.maven.index_shaded.lucene.util.packed.DirectMonotonicReader;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * A Metadata repository provides information about artifact metadata. It does not provide the artifact data itself.
 * It may be possible to use the same backend for metadata and storage, but this depends on the backends and they are
 * provided by different APIs.
 *
 * The motivation for this API is to provide fast access to the repository metadata and fulltext search. Also dependencies
 * are stored in this repository.
 *
 * The methods here do not update the artifacts itself. They are only updating the data in the metadata repository.
 * That means, if you want to update some artifact, you should make sure to update the artifact itself and the metadata
 * repository (either directly or by repository scanning).
 *
 * Currently we are providing JCR, File based and Cassandra as backend for the metadata.
 *
 * The metadata repository uses sessions for accessing the data. Please make sure to always close the sessions after using it.
 * Best idiom for using the sessions:
 * <code>
 * try(RepositorySession session = sessionFactory.createSession() {
 *     // do your stuff
 * }
 * </code>
 *
 * It is implementation dependent, if the sessions are really used by the backend. E.g. the file based implementation ignores
 * the sessions completely.
 *
 * Sessions should be closed immediately after usage. If it is expensive to open a session for a given backend. The backend
 * should provide a session pool if possible. There are methods for refreshing a session if needed.
 *
 * You should avoid stacking sessions, that means, do not create a new session in the same thread, when a session is opened already.
 *
 * Some backend implementations (JCR) update the metadata in the background, that means update of the metadata is not reflected
 * immediately.
 *
 * The base metadata coordinates are:
 * <ul>
 *     <li>Repository ID: The identifier of the repository, where the artifact resides</li>
 *     <li>Namespace: This is a hierarchical coordinate for locating the projects. E.g. this corresponds to the groupId in maven. </li>
 *     <li>Project ID: The project itself</li>
 *     <li>Version: Each project may have different versions.</li>
 *     <li>Artifact: Artifacts correspond to files / blob data. Each artifact has additional metadata, like name, version, modification time, ...</li>
 * </ul>
 *
 * As the repository connects to some backend either locally or remote, the access to the repository may fail. The methods capsule the
 * backend errors into <code>{@link MetadataRepositoryException}</code>.
 *
 * Facets are the way to provide additional metadata that is not part of the base API. It depends on the repository type (e.g. Maven, NPM,
 * not the metadata backend) what facets are stored in addition to the standard metadata.
 * Facets have a specific facet ID that represents the schema for the data stored. For creating specific objects for a given
 * facet id the <code>{@link org.apache.archiva.metadata.model.MetadataFacetFactory}</code> is used.
 * For each facet id there may exist multiple facet instances on each level. Facet instances are identified by their name, which may be
 * a hierarchical path.
 * The data in each facet instance is stored in properties (key-value pairs). The properties are converted into / from the specific
 * facet object.
 *
 * Facets can be stored on repository, project, version and artifact level.
 *
 */
public interface MetadataRepository
{
    /**
     * Update metadata for a particular project in the metadata repository, or create it, if it does not already exist.
     *
     * @param session The session used for updating.
     * @param repositoryId the repository the project is in
     * @param project      the project metadata to create or update
     * @throws MetadataRepositoryException if the update fails
     */
    void updateProject( RepositorySession session, String repositoryId, ProjectMetadata project )
        throws MetadataRepositoryException;

    /**
     * Update the metadata of a given artifact. If the artifact, namespace, version, project does not exist in the repository it will be created.
     *
     * @param session The repository session
     * @param repositoryId The repository id
     * @param namespace The namespace ('.' separated)
     * @param projectId The project id
     * @param projectVersion The project version
     * @param artifactMeta Information about the artifact itself.
     * @throws MetadataRepositoryException if something goes wrong during update.
     */
    void updateArtifact( RepositorySession session, String repositoryId, String namespace, String projectId, String projectVersion,
                         ArtifactMetadata artifactMeta )
        throws MetadataRepositoryException;

    /**
     * Updates the metadata for a specific version of a given project. If the namespace, project, version does not exist,
     * it will be created.
     *
     * @param session The repository session
     * @param repositoryId The repository id
     * @param namespace The namespace ('.' separated)
     * @param projectId The project id
     * @param versionMetadata The metadata for the version
     * @throws MetadataRepositoryException if something goes wrong during update
     */
    void updateProjectVersion( RepositorySession session, String repositoryId, String namespace, String projectId,
                               ProjectVersionMetadata versionMetadata )
        throws MetadataRepositoryException;

    /**
     * Create the namespace in the repository, if it does not exist.
     * Namespaces do not have specific metadata attached.
     *
     * @param session The repository session
     * @param repositoryId The repository id
     * @param namespace The namespace ('.' separated)
     * @throws MetadataRepositoryException if something goes wrong during update
     */
    void updateNamespace( RepositorySession session, String repositoryId, String namespace )
        throws MetadataRepositoryException;

    /**
     * Return the facet names stored for the given facet id on the repository level.
     *
     * @param session The repository session
     * @param repositoryId The repository id
     * @param facetId The facet id
     * @return The list of facet names, or an empty list, if there are no facets stored on this repository for the given facet id.
     * @throws MetadataRepositoryException if something goes wrong
     */
    List<String> getMetadataFacets( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException;

    <T extends MetadataFacet> Stream<T> getMetadataFacetStream( RepositorySession session, String repositoryId, Class<T> facetClazz)
        throws MetadataRepositoryException;

    <T extends MetadataFacet> Stream<T> getMetadataFacetStream( RepositorySession session, String repositoryId, Class<T> facetClazz, long offset, long maxEntries)
        throws MetadataRepositoryException;

    /**
     * Returns true, if there is facet data stored for the given id on the repository. The facet data itself
     * may be empty. It's just checking if there is data stored for the given facet id.
     *
     * @param session The repository session
     * @param repositoryId The repository id
     * @param facetId The facet id
     * @return true if there is data stored this facetId on repository level.
     * @throws MetadataRepositoryException if something goes wrong
     * @since 1.4-M4
     */
    boolean hasMetadataFacet( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException;

    /**
     * Returns the facet data stored on the repository level. The facet instance is identified by the facet id and the
     * facet name. The returned object is a instance created by using <code>{@link org.apache.archiva.metadata.model.MetadataFacetFactory}</code>.
     *
     * @param session The repository session
     * @param repositoryId The repository id
     * @param facetId The facet id
     * @param name The attribute name
     * @return The facet values
     * @throws MetadataRepositoryException if something goes wrong.
     */
    MetadataFacet getMetadataFacet( RepositorySession session, String repositoryId, String facetId, String name )
        throws MetadataRepositoryException;

    /**
     * Returns the facet instance using the proper class.
     *
     * @param session The repository session
     * @param repositoryId The repository
     * @param clazz The facet object class
     * @param name The name of the facet
     * @param <T> The facet object
     * @return The facet instance if it exists.
     * @throws MetadataRepositoryException
     */
    <T extends MetadataFacet> T getMetadataFacet(RepositorySession session, String repositoryId, Class<T> clazz, String name)
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

    Stream<ArtifactMetadata> getArtifactsByDateRangeStream( RepositorySession session, String repositoryId, ZonedDateTime startTime, ZonedDateTime endTime )
        throws MetadataRepositoryException;

    Stream<ArtifactMetadata> getArtifactsByDateRangeStream( RepositorySession session, String repositoryId,
                                                            ZonedDateTime startTime, ZonedDateTime endTime, long offset, long maxEntries )
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
