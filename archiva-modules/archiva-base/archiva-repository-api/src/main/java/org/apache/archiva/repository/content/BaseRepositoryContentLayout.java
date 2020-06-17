package org.apache.archiva.repository.content;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.storage.StorageAsset;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Layout interface for interacting with a managed repository in an abstract way,
 * without the need for processing based on filesystem paths, or working with the database.
 *
 * 
 */
public interface BaseRepositoryContentLayout extends ManagedRepositoryContentLayout
{

    /// *****************   New generation interface **********************



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
     * It depends on the repository type, what exactly is returned for a given set of coordinates. Some repository type
     * may have different required and optional coordinates. For further information please check the documentation for the
     * type specific implementations.
     *
     * The following coordinates are optional and may further specify the artifact to return.
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
     * @throws IllegalArgumentException if the selector coordinates do not specify a artifact
     * @throws ContentAccessException if the access to the underlying storage failed
     */
    Artifact getArtifact(ItemSelector selector) throws ContentAccessException, IllegalArgumentException;

    /**
     * Returns the artifact at the given path
     * @param path the path to the artifact
     * @return the artifact instance
     * @throws LayoutException if the path does not point to a artifact
     * @throws ContentAccessException if the access to the underlying storage failed
     */
    Artifact getArtifact( String path ) throws LayoutException, ContentAccessException;

    /**
     * Returns the artifacts that match the given selector. It is up to the repository implementation
     * what artifacts are returned for a given set of coordinates.
     *
     * @param selector the selector for the artifacts
     * @return a list of artifacts.
     * @throws IllegalArgumentException if the specified coordinates cannot be found in the repository
     * @throws ContentAccessException if the access to the underlying storage failed
     */
    List<? extends Artifact> getArtifacts( ItemSelector selector) throws ContentAccessException;

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
     * @throws IllegalArgumentException if the selector is not valid for the layout
     * @throws ItemNotFoundException if the specified coordinates cannot be found in the repository
     * @throws ContentAccessException if the access to the underlying storage failed
     */
    Stream<? extends Artifact> newArtifactStream( ItemSelector selector) throws ContentAccessException, IllegalArgumentException;


    /**
     * Return the projects that are part of the given namespace.
     *
     * @param namespace the namespace
     * @return the list of projects or a empty list, if there are no projects for the given namespace.
     */
    List<? extends Project> getProjects( Namespace namespace) throws ContentAccessException;

    /**
     * Returns the list of projects that match the given selector. The selector must at least specify a
     * a namespace.
     *
     * @param selector the selector
     * @return the list of projects that match the selector. A empty list of not project matches.
     * @throws ContentAccessException if the access to the storage backend failed
     * @throws IllegalArgumentException if the selector does not contain sufficient data for selecting projects
     */
    List<? extends Project> getProjects( ItemSelector selector ) throws ContentAccessException, IllegalArgumentException;

    /**
     * Return the existing versions of the given project.
     *
     * @param project the project
     * @return a list of versions or a empty list, if not versions are available for the specified project
     * @throws ContentAccessException if the access to the underlying storage failed
     */
    List<? extends Version> getVersions( Project project) throws ContentAccessException;


    /**
     * Return the versions that match the given selector. The selector must at least specify a namespace and a projectId.
     *
     * @param selector the item selector. At least namespace and projectId must be set.
     * @return the list of version or a empty list, if no version matches the selector
     * @throws ContentAccessException if the access to the backend failed
     * @throws IllegalArgumentException if the selector does not contain enough information for selecting versions
     */
    List<? extends Version> getVersions( ItemSelector selector ) throws ContentAccessException, IllegalArgumentException;

    /**
     * Returns all found artifact versions that can be found for the given selector. The selector must specify at least
     * a project.
     *
     * @param selector the item selector that must specify at least a project
     * @return the list of artifact versions
     * @throws ContentAccessException if the access to the underlying storage failed
     * @throws IllegalArgumentException if the selector does not have project information
     */
    List<String> getArtifactVersions( ItemSelector selector ) throws ContentAccessException, IllegalArgumentException;

    /**
     * Return all the artifacts of a given content item (namespace, project, version)
     *
     * @param item the item
     * @return a list of artifacts or a empty list, if no artifacts are available for the specified item
     */
    List<? extends Artifact> getArtifacts( ContentItem item) throws ContentAccessException;

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
     * @throws ContentAccessException if the access to the underlying storage failed
     */
    Stream<? extends Artifact> newArtifactStream( ContentItem item ) throws ContentAccessException;


    /**
     * Copies the artifact to the given destination coordinates
     *
     * @param sourceFile the path to the source file
     * @param destination the coordinates of the destination
     * @throws IllegalArgumentException if the destination is not valid
     */
    void addArtifact( Path sourceFile, Artifact destination ) throws IllegalArgumentException, ContentAccessException;

    /**
     * Returns the metadata file for the given version.
     *
     * @param version the version
     * @return the metadata file
     */
    DataItem getMetadataItem( Version version );

    /**
     * Returns the metadata file for the given project
     *
     * @param project the project
     * @return the metadata file
     */
    DataItem getMetadataItem( Project project );


    /// *****************   End of new generation interface **********************


}
