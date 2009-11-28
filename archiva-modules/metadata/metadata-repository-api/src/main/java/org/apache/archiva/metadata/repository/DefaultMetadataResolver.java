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

import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;

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
     * FIXME: this needs to be configurable based on storage type, and availability of proxy module
     * ... could be a different type since we need methods to modify the storage metadata, which would also allow more
     * appropriate methods to pass in the already determined repository configuration, for example, instead of the ID
     *
     * @plexus.requirement role-hint="maven2"
     */
    private MetadataResolver storageResolver;

    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
    {
        // TODO: intercept
        return metadataRepository.getProject( repoId, namespace, projectId );
    }

    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
        throws MetadataResolverException
    {
        ProjectVersionMetadata metadata =
            metadataRepository.getProjectVersion( repoId, namespace, projectId, projectVersion );
        // TODO: do we want to detect changes as well by comparing timestamps? isProjectVersionNewerThan(updated)
        //       in such cases we might also remove/update stale metadata, including adjusting plugin-based facets
        if ( metadata == null )
        {
            metadata = storageResolver.getProjectVersion( repoId, namespace, projectId, projectVersion );
            if ( metadata != null )
            {
                // FIXME: make this a more generic post-processing that plugins can take advantage of
                //       eg. maven projects should be able to process parent here
                if ( !metadata.getDependencies().isEmpty() )
                {
                    ProjectVersionReference ref = new ProjectVersionReference();
                    ref.setNamespace( namespace );
                    ref.setProjectId( projectId );
                    ref.setProjectVersion( projectVersion );
                    ref.setReferenceType( ProjectVersionReference.ReferenceType.DEPENDENCY );
                    for ( Dependency dependency : metadata.getDependencies() )
                    {
                        metadataRepository.updateProjectReference( repoId, dependency.getGroupId(),
                                                                   dependency.getArtifactId(), dependency.getVersion(),
                                                                   ref );
                    }
                }
                metadataRepository.updateProjectVersion( repoId, namespace, projectId, metadata );
            }
        }
        return metadata;
    }

    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                   String projectVersion )
    {
        // TODO: intercept
        return metadataRepository.getArtifactVersions( repoId, namespace, projectId, projectVersion );
    }

    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
    {
        // TODO: is this assumption correct? could a storage mech. actually know all references in a non-Maven scenario?
        // not passed to the storage mechanism as resolving references would require iterating all artifacts
        return metadataRepository.getProjectReferences( repoId, namespace, projectId, projectVersion );
    }

    public Collection<String> getRootNamespaces( String repoId )
    {
        Collection<String> rootNamespaces = metadataRepository.getRootNamespaces( repoId );

        // TODO: may want caching on this
        Collection<String> storageRootNamespaces = storageResolver.getRootNamespaces( repoId );
        if ( storageRootNamespaces != null && !storageRootNamespaces.equals( rootNamespaces ) )
        {
            // TODO: update the metadata repository
            rootNamespaces = storageRootNamespaces;
        }

        return rootNamespaces;
    }

    public Collection<String> getNamespaces( String repoId, String namespace )
    {
        Collection<String> namespaces = metadataRepository.getNamespaces( repoId, namespace );
        // TODO: may want caching on this
        Collection<String> storageNamespaces = storageResolver.getNamespaces( repoId, namespace );
        if ( storageNamespaces != null && !storageNamespaces.equals( namespaces ) )
        {
            // TODO: update the metadata repository
            namespaces = storageNamespaces;
        }

        return namespaces;
    }

    public Collection<String> getProjects( String repoId, String namespace )
    {
        Collection<String> projects = metadataRepository.getProjects( repoId, namespace );
        // TODO: may want caching on this
        Collection<String> storageProjects = storageResolver.getProjects( repoId, namespace );
        if ( storageProjects != null && !storageProjects.equals( projects ) )
        {
            // TODO: update the metadata repository
            projects = storageProjects;
        }

        return projects;
    }

    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
    {
        Collection<String> projectVersions = metadataRepository.getProjectVersions( repoId, namespace, projectId );
        // TODO: may want caching on this
        Collection<String> storageProjectVersions = storageResolver.getProjectVersions( repoId, namespace, projectId );
        if ( storageProjectVersions != null && !storageProjectVersions.equals( projectVersions ) )
        {
            // TODO: update the metadata repository
            projectVersions = storageProjectVersions;
        }
        return projectVersions;
    }
}
