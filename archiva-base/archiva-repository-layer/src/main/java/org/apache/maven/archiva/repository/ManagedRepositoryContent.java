package org.apache.maven.archiva.repository;

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

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.layout.LayoutException;

import java.io.File;
import java.util.Set;

/**
 * ManagedRepositoryContent interface for interacting with a managed repository in an abstract way, 
 * without the need for processing based on filesystem paths, or working with the database.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface ManagedRepositoryContent
{
    /**
     * Delete from the managed repository all files / directories associated with the
     * provided version reference.
     * 
     * @param reference the version reference to delete.
     * @throws ContentNotFoundException 
     */
    public void deleteVersion( VersionedReference reference )
        throws ContentNotFoundException;

    /**
     * <p>
     * Convenience method to get the repository id.
     * </p>
     * 
     * <p>
     * Equivalent to calling <code>.getRepository().getId()</code>
     * </p>
     * 
     * @return the repository id.
     */
    public String getId();

    /**
     * <p>
     * Gather up the list of related artifacts to the ArtifactReference provided.
     * This typically inclues the pom files, and those things with 
     * classifiers (such as doc, source code, test libs, etc...)
     * </p>
     * 
     * <p>
     * <strong>NOTE:</strong> Some layouts (such as maven 1 "legacy") are not compatible with this query.
     * </p> 
     * 
     * @param reference the reference to work off of.
     * @return the set of ArtifactReferences for related artifacts.
     * @throws ContentNotFoundException if the initial artifact reference does not exist within the repository.
     * @throws LayoutException 
     */
    public Set<ArtifactReference> getRelatedArtifacts( ArtifactReference reference )
        throws ContentNotFoundException, LayoutException;

    /**
     * <p>
     * Convenience method to get the repository (on disk) root directory.
     * </p>
     * 
     * <p>
     * Equivalent to calling <code>.getRepository().getLocation()</code>
     * </p>
     * 
     * @return the repository (on disk) root directory.
     */
    public String getRepoRoot();

    /**
     * Get the repository configuration associated with this
     * repository content.
     * 
     * @return the repository that is associated with this repository content.
     */
    public ManagedRepositoryConfiguration getRepository();

    /**
     * Given a specific ProjectReference, return the list of available versions for
     * that project reference.
     * 
     * @param reference the project reference to work off of.
     * @return the list of versions found for that project reference.
     * @throws ContentNotFoundException if the project reference does nto exist within the repository.
     * @throws LayoutException 
     */
    public Set<String> getVersions( ProjectReference reference )
        throws ContentNotFoundException, LayoutException;

    /**
     * <p>
     * Given a specific VersionedReference, return the list of available versions for that
     * versioned reference.
     * </p>
     * 
     * <p>
     * <strong>NOTE:</strong> This is really only useful when working with SNAPSHOTs.
     * </p>
     * 
     * @param reference the versioned reference to work off of.
     * @return the set of versions found.
     * @throws ContentNotFoundException if the versioned reference does not exist within the repository.
     * @throws LayoutException 
     */
    public Set<String> getVersions( VersionedReference reference )
        throws ContentNotFoundException, LayoutException;

    /**
     * Determines if the artifact referenced exists in the repository.
     * 
     * @param reference the artifact reference to check for.
     * @return true if the artifact referenced exists.
     */
    public boolean hasContent( ArtifactReference reference );

    /**
     * Determines if the project referenced exists in the repository.
     * 
     * @param reference the project reference to check for.
     * @return true it the project referenced exists.
     */
    public boolean hasContent( ProjectReference reference );

    /**
     * Determines if the version reference exists in the repository.
     * 
     * @param reference the version reference to check for.
     * @return true if the version referenced exists.
     */
    public boolean hasContent( VersionedReference reference );

    /**
     * Set the repository configuration to associate with this
     * repository content.
     * 
     * @param repo the repository to associate with this repository content.
     */
    public void setRepository( ManagedRepositoryConfiguration repo );

    /**
     * Given a repository relative path to a filename, return the {@link VersionedReference} object suitable for the path.
     *
     * @param path the path relative to the repository base dir for the artifact.
     * @return the {@link ArtifactReference} representing the path.  (or null if path cannot be converted to
     *         a {@link ArtifactReference})
     * @throws LayoutException if there was a problem converting the path to an artifact.
     */
    public ArtifactReference toArtifactReference( String path )
        throws LayoutException;

    /**
     * Given an ArtifactReference, return the file reference to the artifact.
     *
     * @param reference the artifact reference to use.
     * @return the relative path to the artifact.
     */
    public File toFile( ArtifactReference reference );

    /**
     * Given a project reference, return the path to the metadata for
     * the project. 
     * 
     * @param reference the reference to use.
     * @return the path to the metadata file, or null if no metadata is appropriate.
     */
    public String toMetadataPath( ProjectReference reference );

    /**
     * Given a versioned reference, return the path to the metadata for
     * the specific version of the project. 
     * 
     * @param reference the reference to use.
     * @return the path to the metadata file, or null if no metadata is appropriate.
     */
    public String toMetadataPath( VersionedReference reference );

    /**
     * Given an ArtifactReference, return the relative path to the artifact.
     *
     * @param reference the artifact reference to use.
     * @return the relative path to the artifact.
     */
    public String toPath( ArtifactReference reference );
}
