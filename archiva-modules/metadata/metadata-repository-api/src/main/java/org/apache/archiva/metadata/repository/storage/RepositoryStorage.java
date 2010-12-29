package org.apache.archiva.metadata.repository.storage;

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
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.filter.Filter;

import java.util.Collection;

// FIXME: we should drop the repoId parameters and attach this to an instance of a repository storage
public interface RepositoryStorage
{
    ProjectMetadata readProjectMetadata( String repoId, String namespace, String projectId );

    ProjectVersionMetadata readProjectVersionMetadata( String repoId, String namespace, String projectId,
                                                       String projectVersion )
        throws RepositoryStorageMetadataInvalidException, RepositoryStorageMetadataNotFoundException;

    Collection<String> listRootNamespaces( String repoId, Filter<String> filter );

    Collection<String> listNamespaces( String repoId, String namespace, Filter<String> filter );

    Collection<String> listProjects( String repoId, String namespace, Filter<String> filter );

    Collection<String> listProjectVersions( String repoId, String namespace, String projectId, Filter<String> filter );

    Collection<ArtifactMetadata> readArtifactsMetadata( String repoId, String namespace, String projectId,
                                                        String projectVersion, Filter<String> filter );

    // FIXME: reconsider this API, do we want to expose storage format in the form of a path?
    ArtifactMetadata readArtifactMetadataFromPath( String repoId, String path );
}
