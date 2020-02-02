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

import org.apache.archiva.metadata.QueryParameter;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * A Metadata repository provides information about artifact metadata. It does not provide the artifact data itself.
 * It may be possible to use the same backend for metadata and storage, but this depends on the backends and they are
 * provided by different APIs.
 * <p>
 * The motivation for this API is to provide fast access to the repository metadata and fulltext search. Also dependencies
 * are stored in this repository.
 * <p>
 * The methods here do not update the artifacts itself. They are only updating the data in the metadata repository.
 * That means, if you want to update some artifact, you should make sure to update the artifact itself and the metadata
 * repository (either directly or by repository scanning).
 * <p>
 * Currently we are providing JCR, File based and Cassandra as backend for the metadata.
 * <p>
 * The metadata repository uses sessions for accessing the data. Please make sure to always close the sessions after using it.
 * Best idiom for using the sessions:
 * <code>
 * try(RepositorySession session = sessionFactory.createSession() {
 * // do your stuff
 * }
 * </code>
 * <p>
 * It is implementation dependent, if the sessions are really used by the backend. E.g. the file based implementation ignores
 * the sessions completely.
 * <p>
 * Sessions should be closed immediately after usage. If it is expensive to open a session for a given backend. The backend
 * should provide a session pool if possible. There are methods for refreshing a session if needed.
 * <p>
 * You should avoid stacking sessions, which means, you should not create a new session in the same thread, when a session is opened already.
 * <p>
 * Some backend implementations (JCR) update the metadata in the background, that means update of the metadata is not reflected
 * immediately.
 * <p>
 * The base metadata coordinates are:
 * <ul>
 *     <li>Repository ID: The identifier of the repository, where the artifact resides</li>
 *     <li>Namespace: This is a hierarchical coordinate for locating the projects. E.g. this corresponds to the groupId in maven. </li>
 *     <li>Project ID: The project itself</li>
 *     <li>Version: Each project may have different versions.</li>
 *     <li>Artifact: Artifacts correspond to files / blob data. Each artifact has additional metadata, like name, version, modification time, ...</li>
 * </ul>
 * <p>
 * As the repository connects to some backend either locally or remote, the access to the repository may fail. The methods capsule the
 * backend errors into <code>{@link MetadataRepositoryException}</code>.
 * <p>
 * Facets are the way to provide additional metadata that is not part of the base API. It depends on the repository type (e.g. Maven, NPM,
 * not the metadata backend) what facets are stored in addition to the standard metadata.
 * Facets have a specific facet ID that represents the schema for the data stored. For creating specific objects for a given
 * facet id the <code>{@link org.apache.archiva.metadata.model.MetadataFacetFactory}</code> is used.
 * For each facet id there may exist multiple facet instances on each level. Facet instances are identified by their name, which may be
 * a hierarchical path.
 * The data in each facet instance is stored in properties (key-value pairs). The properties are converted into / from the specific
 * facet object.
 * <p>
 * Facets can be stored on repository, project, version and artifact level.
 * <p>
 * For retrieving artifacts there are methods that return lists and streaming based methods. Some implementations (e.g. JCR) use
 * lazy loading for the retrieved objects. So the streaming methods may be faster and use less memory than the list based methods.
 * But for some backends there is no difference.
 */
public interface MetadataRepository
{


    /**
     * Update metadata for a particular project in the metadata repository, or create it, if it does not already exist.
     *
     * @param session      The session used for updating.
     * @param repositoryId the repository the project is in
     * @param project      the project metadata to create or update
     * @throws MetadataRepositoryException if the update fails
     */
    void updateProject( RepositorySession session, String repositoryId, ProjectMetadata project )
        throws MetadataRepositoryException;

    /**
     * Update the metadata of a given artifact. If the artifact, namespace, version, project does not exist in the repository it will be created.
     *
     * @param session        The repository session
     * @param repositoryId   The repository id
     * @param namespace      The namespace ('.' separated)
     * @param projectId      The project id
     * @param projectVersion The project version
     * @param artifactMeta   Information about the artifact itself.
     * @throws MetadataRepositoryException if something goes wrong during update.
     */
    void updateArtifact( RepositorySession session, String repositoryId,
                         String namespace, String projectId, String projectVersion,
                         ArtifactMetadata artifactMeta )
        throws MetadataRepositoryException;

    /**
     * Updates the metadata for a specific version of a given project. If the namespace, project, version does not exist,
     * it will be created.
     *
     * @param session         The repository session
     * @param repositoryId    The repository id
     * @param namespace       The namespace ('.' separated)
     * @param projectId       The project id
     * @param versionMetadata The metadata for the version
     * @throws MetadataRepositoryException if something goes wrong during update
     */
    void updateProjectVersion( RepositorySession session, String repositoryId,
                               String namespace, String projectId,
                               ProjectVersionMetadata versionMetadata )
        throws MetadataRepositoryException;

    /**
     * Create the namespace in the repository, if it does not exist.
     * Namespaces do not have specific metadata attached.
     *
     * @param session      The repository session
     * @param repositoryId The repository id
     * @param namespace    The namespace ('.' separated)
     * @throws MetadataRepositoryException if something goes wrong during update
     */
    void updateNamespace( RepositorySession session, String repositoryId, String namespace )
        throws MetadataRepositoryException;

    /**
     * Return the facet names stored for the given facet id on the repository level.
     *
     * @param session      The repository session
     * @param repositoryId The repository id
     * @param facetId      The facet id
     * @return The list of facet names, or an empty list, if there are no facets stored on this repository for the given facet id.
     * @throws MetadataRepositoryException if something goes wrong
     */
    List<String> getMetadataFacets( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException;


    /**
     * The same as {@link #getMetadataFacetStream(RepositorySession, String, Class, QueryParameter)}
     * but uses default query parameters.
     * <p>
     * There is no limitation of the number of result objects returned, but implementations may have a hard upper bound for
     * the number of results.
     *
     * @param session      The repository session.
     * @param repositoryId The repository id.
     * @param facetClazz   The facet class
     * @param <T>          The facet type
     * @return A stream of facet objects, or a empty stream if no facet was found.
     * @throws MetadataRepositoryException if the facet retrieval fails.
     * @since 3.0
     */
    <T extends MetadataFacet> Stream<T> getMetadataFacetStream( RepositorySession session,
                                                                String repositoryId, Class<T> facetClazz )
        throws MetadataRepositoryException;

    /**
     * Returns a stream of MetadataFacet elements that match the given facet class.
     * Implementations should order the resulting stream by facet name.
     *
     * @param session      The repository session
     * @param repositoryId The repository id
     * @param facetClazz   The class of the facet
     * @param <T>          The facet type
     * @return A stream of facet objects, or a empty stream if no facet was found.
     * @throws MetadataRepositoryException if the facet retrieval fails
     * @since 3.0
     */
    <T extends MetadataFacet> Stream<T> getMetadataFacetStream( RepositorySession session,
                                                                String repositoryId, Class<T> facetClazz,
                                                                QueryParameter queryParameter )
        throws MetadataRepositoryException;

    /**
     * Returns true, if there is facet data stored for the given facet id on the repository on repository level. The facet data itself
     * may be empty. It's just checking if there is an object stored for the given facet id.
     *
     * @param session      The repository session
     * @param repositoryId The repository id
     * @param facetId      The facet id
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
     * @param session      The repository session
     * @param repositoryId The repository id
     * @param facetId      The facet id
     * @param name         The attribute name
     * @return The facet values
     * @throws MetadataRepositoryException if something goes wrong.
     */
    MetadataFacet getMetadataFacet( RepositorySession session, String repositoryId, String facetId,
                                    String name )
        throws MetadataRepositoryException;

    /**
     * Returns the facet instance for the given class, which is stored on repository level for the given name.
     * If the given name does not point to a instance that can be represented by this class, <code>null</code> will be returned.
     * If the facet is not found the method returns <code>null</code>.
     *
     * @param session      The repository session
     * @param repositoryId The id of the repository
     * @param clazz        The facet object class
     * @param name         The name of the facet (name or path)
     * @param <T>          The type of the facet object
     * @return The facet instance, if it exists.
     * @throws MetadataRepositoryException if the data cannot be retrieved from the backend
     * @since 3.0
     */
    <T extends MetadataFacet> T getMetadataFacet( RepositorySession session, String repositoryId,
                                                  Class<T> clazz, String name )
        throws MetadataRepositoryException;

    /**
     * Adds a facet to the repository level.
     *
     * @param session       The repository session
     * @param repositoryId  The id of the repository
     * @param metadataFacet The facet to add
     * @throws MetadataRepositoryException if the facet cannot be stored.
     */
    void addMetadataFacet( RepositorySession session, String repositoryId,
                           MetadataFacet metadataFacet )
        throws MetadataRepositoryException;

    /**
     * Removes all facets with the given facetId from the repository level.
     *
     * @param session      The repository session
     * @param repositoryId The id of the repository
     * @param facetId      The facet id
     * @throws MetadataRepositoryException if the removal fails
     */
    void removeMetadataFacets( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException;

    /**
     * Removes the given facet from the repository level, if it exists.
     *
     * @param session      The repository session
     * @param repositoryId The id of the repository
     * @param facetId      The facet id
     * @param name         The facet name or path
     */
    void removeMetadataFacet( RepositorySession session, String repositoryId, String facetId, String name )
        throws MetadataRepositoryException;


    /**
     * Is the same as {@link #getArtifactsByDateRange(RepositorySession, String, ZonedDateTime, ZonedDateTime, QueryParameter)}, but
     * uses default query parameters.
     */
    List<ArtifactMetadata> getArtifactsByDateRange( RepositorySession session, String repositoryId,
                                                    ZonedDateTime startTime, ZonedDateTime endTime )
        throws MetadataRepositoryException;

    /**
     * Searches for artifacts where the 'whenGathered' attribute value is between the given start and end time.
     * If start or end time or both are <code>null</code>, the time range for the search is unbounded for this parameter.
     *
     * @param session        The repository session
     * @param repositoryId   The repository id
     * @param startTime      The start time/date as zoned date, can be <code>null</code>
     * @param endTime        The end time/date as zoned date, can be <code>null</code>
     * @param queryParameter Additional parameters for the query that affect ordering and returned results
     * @return The list of metadata objects for the found instances.
     * @throws MetadataRepositoryException if the query fails.
     * @since 3.0
     */
    List<ArtifactMetadata> getArtifactsByDateRange( RepositorySession session, String repositoryId,
                                                    ZonedDateTime startTime, ZonedDateTime endTime,
                                                    QueryParameter queryParameter )
        throws MetadataRepositoryException;


    /**
     * Returns all the artifacts who's 'whenGathered' attribute value is inside the given time range (inclusive) as stream of objects.
     * <p>
     * Implementations should return a stream of sorted objects. The objects should be sorted by the 'whenGathered' date in ascending order.
     *
     * @param session      The repository session
     * @param repositoryId The repository id
     * @param startTime    The start time, can be <code>null</code>
     * @param endTime      The end time, can be <code>null</code>
     * @return A stream of artifact metadata objects, or a empty stream if no artifact was found.
     * @throws MetadataRepositoryException if the artifact retrieval fails.
     * @since 3.0
     */
    Stream<ArtifactMetadata> getArtifactByDateRangeStream( RepositorySession session, String repositoryId,
                                                           ZonedDateTime startTime, ZonedDateTime endTime )
        throws MetadataRepositoryException;

    /**
     * Returns all the artifacts who's 'whenGathered' attribute value is inside the given time range (inclusive) as stream of objects.
     * <p>
     * If no sort attributes are given by the queryParameter, the result is sorted by the 'whenGathered' date.
     *
     * @param session        The repository session
     * @param repositoryId   The repository id
     * @param startTime      The start time, can be <code>null</code>
     * @param endTime        The end time, can be <code>null</code>
     * @param queryParameter Additional parameters for the query that affect ordering and number of returned results.
     * @return A stream of artifact metadata objects.
     * @throws MetadataRepositoryException if the artifact retrieval fails.
     * @since 3.0
     */
    Stream<ArtifactMetadata> getArtifactByDateRangeStream( RepositorySession session, String repositoryId,
                                                           ZonedDateTime startTime, ZonedDateTime endTime,
                                                           QueryParameter queryParameter )
        throws MetadataRepositoryException;


    /**
     * Returns the artifacts that match the given checksum. All checksum types are searched.
     *
     * @param session      The repository session
     * @param repositoryId The repository id
     * @param checksum     The checksum as string of numbers
     * @return The list of artifacts that match the given checksum.
     * @throws MetadataRepositoryException if the artifact retrieval fails
     */
    List<ArtifactMetadata> getArtifactsByChecksum( RepositorySession session, String repositoryId, String checksum )
        throws MetadataRepositoryException;

    /**
     * Get artifacts with a project version metadata key that matches the passed value.
     *
     * @param session      The repository session
     * @param key          The attribute key to search
     * @param value        The attribute value used for search
     * @param repositoryId can be <code>null</code>, meaning search in all repositories
     * @return a list of artifacts. A empty list, if no artifact was found.
     * @throws MetadataRepositoryException if the artifact retrieval fails.
     */
    List<ArtifactMetadata> getArtifactsByProjectVersionFacet( RepositorySession session, String key, String value,
                                                              String repositoryId )
        throws MetadataRepositoryException;

    /**
     * Get artifacts with an artifact metadata key that matches the passed value.
     * <code>key</code> ist the string representation of one of the metadata attributes. Only artifacts are returned where
     * the attribute value matches exactly the given search value.
     *
     * @param session      The repository session.
     * @param key          The string representation of the artifact metadata attribute.
     * @param value        The search value.
     * @param repositoryId can be <code>null</code>, meaning search in all repositories
     * @return a list of artifact objects for each artifact that matches the search string
     * @throws MetadataRepositoryException if the artifact retrieval fails.
     */
    List<ArtifactMetadata> getArtifactsByAttribute( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException;

    /**
     * Get artifacts with a attribute on project version level that matches the passed value.
     * Possible keys are 'scm.url', 'org.name', 'url', 'mailingList.0.name', 'license.0.name',...
     *
     * @param session      the repository session.
     * @param key          The name of the attribute (may be nested like scm.url, mailinglist.0.name)
     * @param value        The value to search for
     * @param repositoryId can be <code>null</code>, which means to search in all repositories
     * @return a list of artifacts or a empty list, if no artifact was found
     * @throws MetadataRepositoryException if the artifact retrieval fails
     */
    List<ArtifactMetadata> getArtifactsByProjectVersionAttribute( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException;

    /**
     * Removes the data for the artifact with the given coordinates from the metadata repository. This will not remove the artifact itself
     * from the storage. It will only remove the metadata.
     *
     * @param session      The repository session
     * @param repositoryId The repository id
     * @param namespace    The namespace of the project
     * @param project      The project name
     * @param version      The project version
     * @param id           The artifact id
     * @throws MetadataRepositoryException if the artifact retrieval fails, or if the artifact cannot be found.
     */
    void removeArtifact( RepositorySession session, String repositoryId, String namespace, String project, String version, String id )
        throws MetadataRepositoryException;

    /**
     * Remove timestamped version of artifact. This removes a snapshot artifact by giving the artifact metadata
     * and the base version of the project.
     *
     * @param session          The repository session
     * @param artifactMetadata the artifactMetadata with the timestamped version (2.0-20120618.214135-2)
     * @param baseVersion      the base version of the snapshot (2.0-SNAPSHOT)
     * @throws MetadataRepositoryException if the removal fails.
     * @since 1.4-M3
     */
    void removeTimestampedArtifact( RepositorySession session, ArtifactMetadata artifactMetadata, String baseVersion )
        throws MetadataRepositoryException;

    /**
     * FIXME need a unit test!!!
     * Removes the {@link MetadataFacet} of the given artifact.
     *
     * @param session       The repository session
     * @param repositoryId  The repository id.
     * @param namespace     The namespace
     * @param project       The project name
     * @param version       The project version
     * @param metadataFacet The facet data
     * @throws MetadataRepositoryException if the removal failed
     * @since 1.4-M3
     */
    void removeFacetFromArtifact( RepositorySession session, String repositoryId, String namespace, String project, String version,
                                  MetadataFacet metadataFacet )
        throws MetadataRepositoryException;

    /**
     * Deletes all metadata of the given repository. This includes artifact metadata and all associated metadata facets.
     *
     * @param session      The repository session
     * @param repositoryId the repository to delete
     * @throws MetadataRepositoryException if the removal failed
     */
    void removeRepository( RepositorySession session, String repositoryId )
        throws MetadataRepositoryException;

    /**
     * Removes the given namespace and its contents from the metadata repository.
     *
     * @param session      The repository session
     * @param repositoryId The repository id
     * @param namespace    The namespace '.' separated  ( it's the groupId for maven )
     * @throws MetadataRepositoryException if the removal failed
     * @since 1.4-M3
     */
    void removeNamespace( RepositorySession session, String repositoryId, String namespace )
        throws MetadataRepositoryException;

    /**
     * Returns the metadata for all artifacts of the given repository.
     *
     * @param session      The repository session
     * @param repositoryId The repository id
     * @return a list of artifact metadata objects. A empty list if no artifacts where found.
     * @throws MetadataRepositoryException if the retrieval failed.
     */
    List<ArtifactMetadata> getArtifacts( RepositorySession session, String repositoryId )
        throws MetadataRepositoryException;

    /**
     * Returns a stream of artifacts that are stored in the given repository. The number and order of elements in the stream
     * is defined by the <code>queryParameter</code>.
     * The efficiency of ordering of elements is dependent on the implementation.
     * There may be some implementations that have to put a hard limit on the elements returned.
     * If there are no <code>sortFields</code> defined in the query parameter, the order of elements in the stream is undefined and depends
     * on the implementation.
     *
     * @param session      The repository session.
     * @param repositoryId The repository id.
     * @return A stream of artifact metadata objects for each artifact found in the repository.
     * @since 3.0
     */
    Stream<ArtifactMetadata> getArtifactStream( RepositorySession session, String repositoryId, QueryParameter queryParameter )
        throws MetadataResolutionException;

    /**
     * Returns a stream of all the artifacts in the given repository using default query parameter.
     * The order of the artifacts returned in the stream depends on the implementation.
     * The number of elements in the stream is unlimited, but there may be some implementations that have to put a hard
     * limit on the elements returned.
     * For further information see {@link #getArtifactStream(RepositorySession, String, QueryParameter)}
     *
     * @param session      The repository session
     * @param repositoryId The repository id
     * @return A (unlimited) stream of artifact metadata elements that are found in this repository
     * @see #getArtifactStream(RepositorySession, String, QueryParameter)
     * @since 3.0
     */
    Stream<ArtifactMetadata> getArtifactStream( RepositorySession session, String repositoryId )
        throws MetadataResolutionException;

    /**
     * Returns a stream of artifacts found for the given artifact coordinates and using the <code>queryParameter</code>
     *
     * @param session        The repository session. May not be <code>null</code>.
     * @param repoId         The repository id. May not be <code>null</code>.
     * @param namespace      The namespace. May not be <code>null</code>.
     * @param projectId      The project id. May not be <code>null</code>.
     * @param projectVersion The project version. May not be <code>null</code>.
     * @return A stream of artifact metadata object. Order and number of elements returned, depends on the <code>queryParameter</code>.
     * @throws MetadataResolutionException if there are no elements for the given artifact coordinates.
     * @since 3.0
     */
    Stream<ArtifactMetadata> getArtifactStream( RepositorySession session, String repoId,
                                                String namespace, String projectId,
                                                String projectVersion, QueryParameter queryParameter )
        throws MetadataResolutionException;

    /**
     * Returns a stream of artifacts found for the given artifact coordinates. The order of elements returned, depends on the
     * implementation.
     *
     * @param session        The repository session. May not be <code>null</code>.
     * @param repoId         The repository id. May not be <code>null</code>.
     * @param namespace      The namespace. May not be <code>null</code>.
     * @param projectId      The project id. May not be <code>null</code>.
     * @param projectVersion The project version. May not be <code>null</code>.
     * @return A stream of artifact metadata object. Order and number of elements returned, depends on the <code>queryParameter</code>.
     * @throws MetadataResolutionException if there are no elements for the given artifact coordinates.
     * @since 3.0
     */
    Stream<ArtifactMetadata> getArtifactStream( RepositorySession session, String repoId,
                                                String namespace, String projectId,
                                                String projectVersion )
        throws MetadataResolutionException;

    /**
     * Returns the metadata for the given project. If there are no custom properties stored on the project, it will
     * just return a <code>ProjectMetadata</code> object with the data provided by parameters.
     *
     * @param session   The session id
     * @param repoId    The repository id
     * @param namespace The namespace '.'-separated.
     * @param projectId The project name
     * @return The project metadata or <code>null</code> if not found.
     * @throws MetadataResolutionException if the metadata retrieval failed
     */
    ProjectMetadata getProject( RepositorySession session, String repoId, String namespace, String projectId )
        throws MetadataResolutionException;

    /**
     * Returns the metadata for the project version.
     *
     * @param session        The repository session.
     * @param repoId         The repository id.
     * @param namespace      The namespace '.'-separated
     * @param projectId      The project name
     * @param projectVersion The project version
     * @return The version metadata object, or <code>null</code>, if not found.
     * @throws MetadataResolutionException if the retrieval of the metadata failed.
     */
    ProjectVersionMetadata getProjectVersion( RepositorySession session, String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataResolutionException;

    /**
     * Returns all artifact version strings for a given project version. This is for snapshot versions and returns the timestamped
     * versions, if available.
     *
     * @param session        The repository session.
     * @param repoId         The repository id.
     * @param namespace      The namespace '.'-separated
     * @param projectId      The project name.
     * @param projectVersion The project version.
     * @return A list of version strings, or a empty list if no versions are found, or this is not a snapshot version.
     * @throws MetadataResolutionException if the retrieval of the metadata failed.
     */
    List<String> getArtifactVersions( RepositorySession session, String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataResolutionException;

    /**
     * Retrieve project references from the metadata repository. Note that this is not built into the content model for
     * a project version as a reference may be present (due to reverse-lookup of dependencies) before the actual
     * project is, and we want to avoid adding a stub model to the content repository.
     *
     * @param session        The repository session.
     * @param repoId         The repository ID to look within
     * @param namespace      The namespace of the project to get references to
     * @param projectId      The identifier of the project to get references to
     * @param projectVersion The version of the project to get references to
     * @return a list of project references
     * @throws MetadataResolutionException if the version could not be found.
     */
    List<ProjectVersionReference> getProjectReferences( RepositorySession session, String repoId, String namespace, String projectId,
                                                        String projectVersion )
        throws MetadataResolutionException;

    /**
     * Returns the names of the root namespaces stored for this repository.
     *
     * @param session The repository session.
     * @param repoId  The repository id.
     * @return A list of namespace names, or empty list, if no namespace is stored for this repository.
     * @throws MetadataResolutionException If the retrieval failed.
     */
    List<String> getRootNamespaces( RepositorySession session, String repoId )
        throws MetadataResolutionException;

    /**
     * Returns the list of namespace names that are children of the given namespace. It does not descend recursively.
     *
     * @param session   The repository session.
     * @param repoId    The repository id.
     * @param namespace The parent namespace '.'-separated.
     * @return {@link List} of child namespace names, or a empty list, if there are no children for the given parent namespace.
     * @throws MetadataResolutionException if the retrieval failed.
     */
    List<String> getChildNamespaces( RepositorySession session, String repoId, String namespace )
        throws MetadataResolutionException;

    /**
     * Return the project names that of all projects stored under the given namespace.
     *
     * @param session   The repository session.
     * @param repoId    The repository id.
     * @param namespace The namespace '.'-separated.
     * @return The list of project names or empty list if no project exists at the given namespace.
     * @throws MetadataResolutionException if the retrieval failed.
     */
    List<String> getProjects( RepositorySession session, String repoId, String namespace )
        throws MetadataResolutionException;

    /**
     * Returns the names of all versions stored under the given project.
     *
     * @param session   The repository session.
     * @param repoId    The repository id.
     * @param namespace The namespace '.'-separated.
     * @param projectId The project name.
     * @return The list of versions or a empty list, if not version was found.
     * @throws MetadataResolutionException if the retrieval failed.
     */
    List<String> getProjectVersions( RepositorySession session, String repoId, String namespace, String projectId )
        throws MetadataResolutionException;

    /**
     * Removes a project version and all its artifact and facet metadata under it.
     *
     * @param session        The repository session.
     * @param repoId         The repository id.
     * @param namespace      The namespace '.'-separated.
     * @param projectId      The project name
     * @param projectVersion The project version.
     * @throws MetadataRepositoryException if the removal failed.
     * @since 1.4-M4
     */
    void removeProjectVersion( RepositorySession session, String repoId, String namespace, String projectId, String projectVersion )
        throws MetadataRepositoryException;

    /**
     * Returns the metadata of all artifacts stored for the given project version.
     *
     * @param session        The repository session.
     * @param repoId         The repository id.
     * @param namespace      The namespace '.'-separated.
     * @param projectId      The project name.
     * @param projectVersion The project version.
     * @return The list of artifact metadata objects, or a empty list, if no artifact exists for this version.
     * @throws MetadataResolutionException if the retrieval failed.
     */
    List<ArtifactMetadata> getArtifacts( RepositorySession session, String repoId, String namespace, String projectId,
                                         String projectVersion )
        throws MetadataResolutionException;

    /**
     * Removes the project metadata and metadata for all stored versions, artifacts and facets of this project.
     *
     * @param session      The repository session.
     * @param repositoryId The repository id.
     * @param namespace    The namespace '.'-separated.
     * @param projectId    The project name.
     * @throws MetadataRepositoryException if the removal failed.
     * @since 1.4-M4
     */
    void removeProject( RepositorySession session, String repositoryId, String namespace, String projectId )
        throws MetadataRepositoryException;

    /**
     * Closes the repository.
     * Repositories are normally opened during startup and closed on shutdown. The closing of a repository stops all
     * invalidates all connections to it.
     * Sessions that are open are invalidated too. The repository will throw exceptions if it is used after closing.
     *
     * @throws MetadataRepositoryException if the something went wrong or if the repository was closed already.
     */
    void close( )
        throws MetadataRepositoryException;


    /**
     * Full text artifacts search. Searches for the given string in all metadata and returns artifacts where the
     * text was found.
     *
     * @param session      The repository session.
     * @param repositoryId can be <code>null</code> to search in all repositories
     * @param text         The search text
     * @param exact        if true, the value must exactly match the text.
     * @return a list of artifacts or empty list if no results where found.
     * @throws MetadataRepositoryException if the retrieval failed.
     */
    List<ArtifactMetadata> searchArtifacts( RepositorySession session, String repositoryId, String text, boolean exact )
        throws MetadataRepositoryException;

    /**
     * Full text artifacts search inside the specified key. Searches for the given text in all attributes with the given
     * name.
     *
     * @param session      The repository session.
     * @param repositoryId can be <code>null</code> to search in all repositories
     * @param key          search only inside this attribute.
     * @param text         The search string.
     * @param exact        if true, the value must exactly match the text.
     * @return a list of artifacts or empty list if no results were found.
     * @throws MetadataRepositoryException if the retrieval failed.
     */
    List<ArtifactMetadata> searchArtifacts( RepositorySession session, String repositoryId, String key, String text, boolean exact )
        throws MetadataRepositoryException;

}
