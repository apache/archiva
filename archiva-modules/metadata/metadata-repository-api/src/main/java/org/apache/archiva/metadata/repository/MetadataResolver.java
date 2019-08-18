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
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface MetadataResolver
{
    default List<RepositoryType> supportsRepositoryTypes() {
        return Arrays.asList( RepositoryType.MAVEN );
    }

    ProjectVersionMetadata resolveProjectVersion( RepositorySession session, String repoId, String namespace,
                                                  String projectId, String projectVersion )
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
    Collection<ProjectVersionReference> resolveProjectReferences( RepositorySession session, String repoId,
                                                                  String namespace, String projectId,
                                                                  String projectVersion )
        throws MetadataResolutionException;

    Collection<String> resolveRootNamespaces( RepositorySession session, String repoId )
        throws MetadataResolutionException;

    Collection<String> resolveNamespaces( RepositorySession session, String repoId, String namespace )
        throws MetadataResolutionException;

    Collection<String> resolveProjects( RepositorySession session, String repoId, String namespace )
        throws MetadataResolutionException;

    Collection<String> resolveProjectVersions( RepositorySession session, String repoId, String namespace,
                                               String projectId )
        throws MetadataResolutionException;

    Collection<ArtifactMetadata> resolveArtifacts( RepositorySession session, String repoId, String namespace,
                                                   String projectId, String projectVersion )
        throws MetadataResolutionException;
}
