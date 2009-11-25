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

import java.util.Collection;

import org.apache.archiva.metadata.model.ProjectBuildMetadata;
import org.apache.archiva.metadata.model.ProjectMetadata;

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.MetadataResolver"
 */
public class DefaultMetadataResolver
    implements MetadataResolver
{
    /**
     * @plexus.requirement
     */
    private MetadataRepository metadataRepository;

    /**
     * TODO: this needs to be configurable based on storage type, and availability of proxy module
     * TODO: could be a different type since we need methods to modify the storage metadata
     * @plexus.requirement role-hint="maven2"
     */
    private MetadataResolver storageResolver;

    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
    {
        // TODO: intercept
        return metadataRepository.getProject( repoId, namespace, projectId );
    }

    public ProjectBuildMetadata getProjectBuild( String repoId, String namespace, String projectId, String buildId )
    {
        ProjectBuildMetadata metadata = metadataRepository.getProjectBuild( repoId, namespace, projectId, buildId );
        // TODO: do we want to detect changes as well by comparing timestamps? isProjectBuildNewerThan(updated)
        //       in such cases we might also remove/update stale metadata, including adjusting plugin-based facets
        if ( metadata == null )
        {
            metadata = storageResolver.getProjectBuild( repoId, namespace, projectId, buildId );
            metadataRepository.updateBuild( repoId, namespace, projectId, metadata );
        }
        return metadata;
    }

    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId, String buildId )
    {
        // TODO: intercept
        return metadataRepository.getArtifactVersions( repoId, namespace, projectId, buildId );
    }
}
