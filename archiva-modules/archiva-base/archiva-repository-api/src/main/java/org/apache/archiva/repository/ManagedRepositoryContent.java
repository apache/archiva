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
import org.apache.archiva.repository.content.StorageAsset;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * ManagedRepositoryContent interface for interacting with a managed repository in an abstract way,
 * without the need for processing based on filesystem paths, or working with the database.
 *
 * This interface
 */
public interface ManagedRepositoryContent extends RepositoryContent
{



    /**
     * Delete from the managed repository all files / directories associated with the
     * provided version reference.
     *
     * @param reference the version reference to delete.
     * @throws ContentNotFoundException
     */
    void deleteVersion( VersionedReference reference )
        throws ContentNotFoundException;

    /**
     * delete a specified artifact from the repository
     *
     * @param artifactReference
     * @throws ContentNotFoundException
     */
    void deleteArtifact( ArtifactReference artifactReference )
        throws ContentNotFoundException;

    /**
     * @param groupId
     * @throws ContentNotFoundException
     * @since 1.4-M3
     */
    void deleteGroupId( String groupId )
        throws ContentNotFoundException;

    /**
     *
     * @param namespace groupId for maven
     * @param projectId artifactId for maven
     * @throws ContentNotFoundException
     */
    void deleteProject( String namespace, String projectId )
        throws RepositoryException;

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
     * This typically inclues the pom files, and those things with
     * classifiers (such as doc, source code, test libs, etc...)
     * </p>
     * <p>
     * <strong>NOTE:</strong> Some layouts (such as maven 1 "legacy") are not compatible with this query.
     * </p>
     *
     * @param reference the reference to work off of.
     * @return the set of ArtifactReferences for related artifacts.
     * @throws ContentNotFoundException if the initial artifact reference does not exist within the repository.
     */
    Set<ArtifactReference> getRelatedArtifacts( ArtifactReference reference )
        throws ContentNotFoundException;

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
        throws ContentNotFoundException, LayoutException;

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
        throws ContentNotFoundException;

    /**
     * Determines if the artifact referenced exists in the repository.
     *
     * @param reference the artifact reference to check for.
     * @return true if the artifact referenced exists.
     */
    boolean hasContent( ArtifactReference reference );

    /**
     * Determines if the project referenced exists in the repository.
     *
     * @param reference the project reference to check for.
     * @return true it the project referenced exists.
     */
    boolean hasContent( ProjectReference reference );

    /**
     * Determines if the version reference exists in the repository.
     *
     * @param reference the version reference to check for.
     * @return true if the version referenced exists.
     */
    boolean hasContent( VersionedReference reference );

    /**
     * Set the repository configuration to associate with this
     * repository content.
     *
     * @param repo the repository to associate with this repository content.
     */
    void setRepository( org.apache.archiva.repository.ManagedRepository repo );

    /**
     * Given an {@link ArtifactReference}, return the file reference to the artifact.
     *
     * @param reference the artifact reference to use.
     * @return the relative path to the artifact.
     */
    Path toFile( ArtifactReference reference );

    /**
     * Given an {@link ArchivaArtifact}, return the file reference to the artifact.
     *
     * @param reference the archiva artifact to use.
     * @return the relative path to the artifact.
     */
    Path toFile( ArchivaArtifact reference );

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

    /**
     * Returns information about a specific storage asset.
     * @param path
     * @return
     */
    StorageAsset getAsset(String path);

    /**
     * Consumes the data and sets a lock for the file during the operation.
     *
     * @param asset
     * @param consumerFunction
     * @param readLock
     * @throws IOException
     */
    void consumeData( StorageAsset asset, Consumer<InputStream> consumerFunction, boolean readLock ) throws IOException;

    /**
     * Adds a new asset to the underlying storage.
     * @param path The path to the asset.
     * @param container True, if the asset should be a container, false, if it is a file.
     * @return
     */
    StorageAsset addAsset(String path, boolean container);

    /**
     * Removes the given asset from the storage.
     *
     * @param asset
     * @throws IOException
     */
    void removeAsset(StorageAsset asset) throws IOException;

    /**
     * Moves the asset to the given location and returns the asset object for the destination.
     *
     * @param origin
     * @param destination
     * @return
     */
    StorageAsset moveAsset(StorageAsset origin, String destination) throws IOException;


    /**
     * Copies the given asset to the new destination.
     *
     * @param origin
     * @param destination
     * @return
     * @throws IOException
     */
    StorageAsset copyAsset(StorageAsset origin, String destination) throws IOException;
}
