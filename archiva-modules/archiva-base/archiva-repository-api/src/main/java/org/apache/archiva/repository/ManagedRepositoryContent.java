package org.apache.archiva.repository;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.model.ArchivaArtifact;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.ProjectReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.repository.content.Artifact;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.ItemNotFoundException;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.Namespace;
import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.content.Version;
import org.apache.archiva.repository.storage.StorageAsset;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * ManagedRepositoryContent interface for interacting with a managed repository in an abstract way,
 * without the need for processing based on filesystem paths, or working with the database.
 *
 * This interface
 */
public interface ManagedRepositoryContent extends RepositoryContent
{


    /**
     * Returns the version reference for the given coordinates.
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version number
     * @return a version reference
     */
    VersionedReference toVersion( String groupId, String artifactId, String version );


    /**
     * Returns the version reference that represents the generic version, which means that
     * snapshot versions are converted to <VERSION>-SNAPSHOT
     * @param artifactReference the artifact reference
     * @return the generic version
     */
    VersionedReference toGenericVersion( ArtifactReference artifactReference );

    /**
     * Return the version reference that matches exactly the version string of the artifact
     *
     * @param artifactReference The artifact reference
     * @return the version reference
     */
    VersionedReference toVersion( ArtifactReference artifactReference);

    /**
     * Returns a artifact reference for the given coordinates.
     * @param groupId the group id
     * @param artifactId the artifact id
     * @param version the version
     * @param type the type
     * @param classifier the classifier
     * @return a artifact reference object
     */
    ArtifactReference toArtifact( String groupId, String artifactId, String version, String type, String classifier);

    /**
     * Removes the specified content item and all content stored under the given item.
     *
     * @param item the item.
     * @throws ItemNotFoundException if the item cannot be found
     * @throws ContentAccessException if the deletion was not possible or only partly successful, because the access
     *  to the artifacts failed
     */
    void deleteItem( ContentItem item) throws ItemNotFoundException, ContentAccessException;

    /**
     * Returns the namespace for the given selected coordinates. The selector must specify a namespace. All other
     * coordinates are ignored.
     * The following coordinates must be set at the given selector:
     * <ul>
     *     <li>namespace</li>
     * </ul>
     * If not, a {@link IllegalArgumentException} will be thrown.
     *
     * @param namespaceSelector the selectory with the namespace coordinates
     * @return the namespace
     * @throws ItemNotFoundException if the item does not exist
     * @throws ContentAccessException if the item cannot be accessed
     * @throws IllegalArgumentException if the selector has no namespace specified
     */
    Namespace getNamespace( ItemSelector namespaceSelector ) throws ContentAccessException, IllegalArgumentException;

    /**
     * Returns the project for the given coordinates.
     * The following coordinates must be set at the given selector:
     * <ul>
     *     <li>namespace</li>
     *     <li>projectId</li>
     * </ul>
     * If not, a {@link IllegalArgumentException} will be thrown.
     * Additional coordinates will be ignored.
     *
     * @param projectSelector
     * @return the project instance
     * @throws ItemNotFoundException if the project does not exist
     * @throws ContentAccessException if the item cannot be accessed
     * @throws IllegalArgumentException if the selector does not specify the required coordinates
     */
    Project getProject( ItemSelector projectSelector ) throws ContentAccessException, IllegalArgumentException;

    /**
     * Returns the version for the given coordinates.
     * The following coordinates must be set at the given selector:
     * <ul>
     *     <li>namespace</li>
     *     <li>projectId</li>
     *     <li>version</li>
     * </ul>
     * If not, a {@link IllegalArgumentException} will be thrown.
     *
     * Additional coordinates will be ignored.
     *
     * @param versionCoordinates
     * @return the version object
     * @throws ItemNotFoundException
     * @throws ContentAccessException
     * @throws IllegalArgumentException
     */
    Version getVersion(ItemSelector versionCoordinates) throws ContentAccessException, IllegalArgumentException;

    /**
     * Returns the artifact object for the given coordinates.
     *
     * Normally the following coordinates should be set at the given selector:
     * <ul>
     *     <li>namespace</li>
     *     <li>artifactVersion and or version</li>
     *     <li>artifactId or projectId</li>
     * </ul>
     * If the coordinates do not provide enough information for selecting a artifact, a {@link IllegalArgumentException} will be thrown
     * It depends on the repository type, what exactly is deleted for a given set of coordinates. Some repository type
     * may have different required and optional coordinates. For further information please check the documentation for the
     * type specific implementations.
     *
     * The following coordinates are optional and may further specify the artifact to delete.
     * <ul>
     *     <li>classifier</li>
     *     <li>type</li>
     *     <li>extension</li>
     * </ul>
     *
     * The method always returns a artifact object, if the coordinates are valid. It does not guarantee that the artifact
     * exists. To check if there is really a physical representation of the artifact, use the <code>{@link Artifact#exists()}</code>
     * method of the artifact.
     * For upload and data retrieval use the methods of the {@link StorageAsset} reference returned in the artifact.
     *
     *
     * @param selector the selector with the artifact coordinates
     * @return a artifact object
     * @throws ItemNotFoundException if the selector coordinates do not specify a artifact
     * @throws ContentAccessException if the access to the underlying storage failed
     */
    Artifact getArtifact(ItemSelector selector) throws ContentAccessException;


    /**
     * Returns the artifacts that match the given selector. It is up to the repository implementation
     * what artifacts are returned for a given set of coordinates.
     *
     * @param selector the selector for the artifacts
     * @return a list of artifacts.
     * @throws ItemNotFoundException if the specified coordinates cannot be found in the repository
     * @throws ContentAccessException if the access to the underlying storage failed
     */
    List<? extends Artifact> getAllArtifacts( ItemSelector selector) throws ContentAccessException;

    /**
     * Returns the artifacts that match the given selector. It is up to the repository implementation
     * what artifacts are returned for a given set of coordinates.
     *
     * The returned stream is autoclosable and should always closed after using it.
     *
     * There is no guarantee about the order of the returned artifacts
     *
     * @param selector the selector for the artifacts
     * @return a stream with artifact elements.
     * @throws ItemNotFoundException if the specified coordinates cannot be found in the repository
     * @throws ContentAccessException if the access to the underlying storage failed
     */
    Stream<? extends Artifact> getAllArtifactStream( ItemSelector selector) throws ContentAccessException;


    /**
     * Return the projects that are part of the given namespace.
     *
     * @param namespace the namespace
     * @return the list of projects or a empty list, if there are no projects for the given namespace.
     */
    List<? extends Project> getProjects( Namespace namespace) throws ContentAccessException;

    /**
     * Return the existing versions of the given project.
     *
     * @param project the project
     * @return a list of versions or a empty list, if not versions are available for the specified project
     */
    List<? extends Version> getVersions( Project project) throws ContentAccessException;

    /**
     * Return all the artifacts of a given content item (namespace, project, version)
     *
     * @param item the item
     * @return a list of artifacts or a empty list, if no artifacts are available for the specified item
     */
    List<? extends Artifact> getArtifacts( ContentItem item) throws ContentAccessException;

    /**
     * Return all the artifacts of a given namespace and all sub namespaces that are defined under the
     * given namespace.
     *
     * @param namespace the namespace, which is the parent namespace
     * @return a list of artifacts or a empty list, if no artifacts are available for the specified namespace
     */
    List<? extends Artifact> getArtifactsStartingWith( Namespace namespace ) throws ContentAccessException;


    /**
     * Return a stream of artifacts that are part of the given content item. The returned stream is
     * auto closable. There is no guarantee about the order of returned artifacts.
     *
     * As the stream may access IO resources, you should always use call this method inside try-with-resources or
     * make sure, that the stream is closed after using it.
     *
     * @param item the item from where the artifacts should be returned
     * @return a stream of artifacts. The stream is auto closable. You should always make sure, that the stream
     * is closed after use.
     */
    Stream<? extends Artifact> getArtifactStream( ContentItem item ) throws ContentAccessException;


    /**
     * Return a stream of all artifacts that are available for the given namespace and its sub namespaces. The artifacts
     * are retrieved recursively. There is no guarantee about the order of returned artifacts.
     *
     * As the stream may access IO resources, you should always use call this method inside try-with-resources or
     * make sure, that the stream is closed after using it.
     *
     * @param namespace the namespace from where the artifacts should be returned
     * @return a stream of artifacts. The stream is auto closable. You should always make sure, that the stream
     * is closed after use.
     */
    Stream<? extends Artifact> getArtifactStreamStartingWith( Namespace namespace ) throws ContentAccessException;


    /**
     * Returns true, if the selector coordinates point to a existing item in the repository.
     *
     * @param selector the item selector
     * @return <code>true</code>, if there exists such a item, otherwise <code>false</code>
     */
    boolean hasContent( ItemSelector selector );

    /**
     * Copies the artifact to the given destination coordinates
     *
     * @param sourceFile the path to the source file
     * @param destination the coordinates of the destination
     * @throws IllegalArgumentException if the destination is not valid
     */
    void copyArtifact( Path sourceFile, ItemSelector destination ) throws IllegalArgumentException;

    /**
     * Delete from the managed repository all files / directories associated with the
     * provided version reference.
     *
     * @param reference the version reference to delete.
     * @throws ContentNotFoundException
     */
    void deleteVersion( VersionedReference reference )
        throws ContentNotFoundException, ContentAccessException;



    /**
     * delete a specified artifact from the repository
     *
     * @param artifactReference
     * @throws ContentNotFoundException
     */
    void deleteArtifact( ArtifactReference artifactReference )
        throws ContentNotFoundException, ContentAccessException;



    /**
     * @param groupId
     * @throws ContentNotFoundException
     * @since 1.4-M3
     */
    void deleteGroupId( String groupId )
        throws ContentNotFoundException, ContentAccessException;




    /**
     *
     * @param namespace groupId for maven
     * @param projectId artifactId for maven
     * @throws ContentNotFoundException
     */
    void deleteProject( String namespace, String projectId )
        throws ContentNotFoundException, ContentAccessException;


    /**
     * Deletes a project
     * @param reference
     */
    void deleteProject(ProjectReference reference) throws ContentNotFoundException, ContentAccessException;




    /**
     * <p>
     * Convenience method to get the repository id.
     * </p>
     * <p>
     * Equivalent to calling <code>.getRepository().getId()</code>
     * </p>
     *
     * @return the repository id.
     */
    String getId();

    /**
     * <p>
     * Gather up the list of related artifacts to the ArtifactReference provided.
     * If type and / or classifier of the reference is set, this returns only a list of artifacts that is directly
     * related to the given artifact, like checksums.
     * If type and classifier is <code>null</code> it will return the same artifacts as 
     * {@link #getRelatedArtifacts(VersionedReference)}
     * </p>
     * <p>
     * <strong>NOTE:</strong> Some layouts (such as maven 1 "legacy") are not compatible with this query.
     * </p>
     *
     * @param reference the reference to work off of.
     * @return the list of ArtifactReferences for related artifacts, if
     * @throws ContentNotFoundException if the initial artifact reference does not exist within the repository.
     * @see #getRelatedArtifacts(VersionedReference)
     */
    List<ArtifactReference> getRelatedArtifacts( ArtifactReference reference )
        throws ContentNotFoundException, LayoutException, ContentAccessException;

    /**
     * <p>
     * Gather up the list of related artifacts to the ArtifactReference provided.
     * This typically includes the pom files, and those things with
     * classifiers (such as doc, source code, test libs, etc...). Even if the classifier
     * is set in the artifact reference, it may return artifacts with different classifiers.
     * </p>
     * <p>
     * <strong>NOTE:</strong> Some layouts (such as maven 1 "legacy") are not compatible with this query.
     * </p>
     *
     * @param reference the reference to work off of.
     * @return the list of ArtifactReferences for related artifacts, if
     * @throws ContentNotFoundException if the initial artifact reference does not exist within the repository.
     */
    List<ArtifactReference> getRelatedArtifacts( VersionedReference reference )
        throws ContentNotFoundException, LayoutException, ContentAccessException;







    /**
     * Returns all the assets that belong to a given artifact type. The list returned contain
     * all the files that correspond to the given artifact reference.
     * This method is the same as {@link #getRelatedArtifacts(ArtifactReference)} but may also return
     * e.g. hash files.
     *
     * @param reference
     * @return
     */
    List<StorageAsset> getRelatedAssets(ArtifactReference reference) throws ContentNotFoundException, LayoutException, ContentAccessException;

    /**
     * Returns all artifacts that belong to a given version
     * @param reference the version reference
     * @return the list of artifacts or a empty list
     */
    List<ArtifactReference> getArtifacts(VersionedReference reference) throws ContentNotFoundException, LayoutException, ContentAccessException;




    /**
     * <p>
     * Convenience method to get the repository (on disk) root directory.
     * </p>
     * <p>
     * Equivalent to calling <code>.getRepository().getLocation()</code>
     * </p>
     *
     * @return the repository (on disk) root directory.
     */
    String getRepoRoot();

    /**
     * Get the repository configuration associated with this
     * repository content.
     *
     * @return the repository that is associated with this repository content.
     */
    ManagedRepository getRepository();

    /**
     * Given a specific {@link ProjectReference}, return the list of available versions for
     * that project reference.
     *
     * @param reference the project reference to work off of.
     * @return the list of versions found for that project reference.
     * @throws ContentNotFoundException if the project reference does nto exist within the repository.
     * @throws LayoutException
     */
    Set<String> getVersions( ProjectReference reference )
        throws ContentNotFoundException, LayoutException, ContentAccessException;



    /**
     * <p>
     * Given a specific {@link VersionedReference}, return the list of available versions for that
     * versioned reference.
     * </p>
     * <p>
     * <strong>NOTE:</strong> This is really only useful when working with SNAPSHOTs.
     * </p>
     *
     * @param reference the versioned reference to work off of.
     * @return the set of versions found.
     * @throws ContentNotFoundException if the versioned reference does not exist within the repository.
     */
    Set<String> getVersions( VersionedReference reference )
        throws ContentNotFoundException, ContentAccessException, LayoutException;

    /**
     * Determines if the artifact referenced exists in the repository.
     *
     * @param reference the artifact reference to check for.
     * @return true if the artifact referenced exists.
     */
    boolean hasContent( ArtifactReference reference ) throws ContentAccessException;

    /**
     * Determines if the project referenced exists in the repository.
     *
     * @param reference the project reference to check for.
     * @return true it the project referenced exists.
     */
    boolean hasContent( ProjectReference reference ) throws ContentAccessException;

    /**
     * Determines if the version reference exists in the repository.
     *
     * @param reference the version reference to check for.
     * @return true if the version referenced exists.
     */
    boolean hasContent( VersionedReference reference ) throws ContentAccessException;

    /**
     * Set the repository configuration to associate with this
     * repository content.
     *
     * @param repo the repository to associate with this repository content.
     */
    void setRepository( ManagedRepository repo );

    /**
     * Given an {@link ArtifactReference}, return the file reference to the artifact.
     *
     * @param reference the artifact reference to use.
     * @return the relative path to the artifact.
     */
    StorageAsset toFile( VersionedReference reference );

    /**
     * Given an {@link ArtifactReference}, return the file reference to the artifact.
     *
     * @param reference the artifact reference to use.
     * @return the relative path to the artifact.
     */
    StorageAsset toFile( ArtifactReference reference );

    /**
     * Given an {@link ArchivaArtifact}, return the file reference to the artifact.
     *
     * @param reference the archiva artifact to use.
     * @return the relative path to the artifact.
     */
    StorageAsset toFile( ArchivaArtifact reference );

    /**
     * Given a {@link ProjectReference}, return the path to the metadata for
     * the project.
     *
     * @param reference the reference to use.
     * @return the path to the metadata file, or null if no metadata is appropriate.
     */
    String toMetadataPath( ProjectReference reference );

    /**
     * Given a {@link VersionedReference}, return the path to the metadata for
     * the specific version of the project.
     *
     * @param reference the reference to use.
     * @return the path to the metadata file, or null if no metadata is appropriate.
     */
    String toMetadataPath( VersionedReference reference );

    /**
     * Given an {@link ArchivaArtifact}, return the relative path to the artifact.
     *
     * @param reference the archiva artifact to use.
     * @return the relative path to the artifact.
     */
    String toPath( ArchivaArtifact reference );


}
