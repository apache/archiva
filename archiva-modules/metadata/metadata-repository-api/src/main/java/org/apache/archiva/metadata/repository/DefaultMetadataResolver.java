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
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.filter.ExcludesFilter;
import org.apache.archiva.metadata.repository.storage.StorageMetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

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
    private StorageMetadataResolver storageResolver;

    private static final Logger log = LoggerFactory.getLogger( DefaultMetadataResolver.class );

    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
        throws MetadataResolutionException
    {
        // TODO: intercept
        return metadataRepository.getProject( repoId, namespace, projectId );
    }

    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
        throws MetadataResolutionException
    {
        ProjectVersionMetadata metadata = metadataRepository.getProjectVersion( repoId, namespace, projectId,
                                                                                projectVersion );
        // TODO: do we want to detect changes as well by comparing timestamps? isProjectVersionNewerThan(updated)
        //       in such cases we might also remove/update stale metadata, including adjusting plugin-based facets
        //       This would also be better than checking for completeness - we can then refresh only when fixed (though
        //       sometimes this has an additional dependency - such as a parent - requesting the user to force an update
        //       may then work here and be more efficient than always trying again)
        if ( metadata == null || metadata.isIncomplete() )
        {
            metadata = storageResolver.getProjectVersion( repoId, namespace, projectId, projectVersion );
            if ( metadata != null )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Resolved project version metadata from storage: " + metadata );
                }
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
                        try
                        {
                            metadataRepository.updateProjectReference( repoId, dependency.getGroupId(),
                                                                       dependency.getArtifactId(),
                                                                       dependency.getVersion(), ref );
                        }
                        catch ( MetadataRepositoryException e )
                        {
                            log.warn( "Unable to persist resolved information: " + e.getMessage(), e );
                        }
                    }
                }
                try
                {
                    metadataRepository.updateProjectVersion( repoId, namespace, projectId, metadata );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "Unable to persist resolved information: " + e.getMessage(), e );
                }
            }
        }
        return metadata;
    }

    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                   String projectVersion )
        throws MetadataResolutionException
    {
        // TODO: intercept
        return metadataRepository.getArtifactVersions( repoId, namespace, projectId, projectVersion );
    }

    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
        throws MetadataResolutionException
    {
        // TODO: is this assumption correct? could a storage mech. actually know all references in a non-Maven scenario?
        // not passed to the storage mechanism as resolving references would require iterating all artifacts
        return metadataRepository.getProjectReferences( repoId, namespace, projectId, projectVersion );
    }

    public Collection<String> getRootNamespaces( String repoId )
        throws MetadataResolutionException
    {
        Collection<String> namespaces = metadataRepository.getRootNamespaces( repoId );
        Collection<String> storageNamespaces = storageResolver.getRootNamespaces( repoId, new ExcludesFilter<String>(
            namespaces ) );
        if ( storageNamespaces != null && !storageNamespaces.isEmpty() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Resolved root namespaces from storage: " + storageNamespaces );
            }
            for ( String n : storageNamespaces )
            {
                try
                {
                    metadataRepository.updateNamespace( repoId, n );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "Unable to persist resolved information: " + e.getMessage(), e );
                }
            }
            namespaces = new ArrayList<String>( namespaces );
            namespaces.addAll( storageNamespaces );
        }
        return namespaces;
    }

    public Collection<String> getNamespaces( String repoId, String namespace )
        throws MetadataResolutionException
    {
        Collection<String> namespaces = metadataRepository.getNamespaces( repoId, namespace );
        Collection<String> exclusions = new ArrayList<String>( namespaces );
        exclusions.addAll( metadataRepository.getProjects( repoId, namespace ) );
        Collection<String> storageNamespaces = storageResolver.getNamespaces( repoId, namespace,
                                                                              new ExcludesFilter<String>(
                                                                                  exclusions ) );
        if ( storageNamespaces != null && !storageNamespaces.isEmpty() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Resolved namespaces from storage: " + storageNamespaces );
            }
            for ( String n : storageNamespaces )
            {
                try
                {
                    metadataRepository.updateNamespace( repoId, namespace + "." + n );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "Unable to persist resolved information: " + e.getMessage(), e );
                }
            }
            namespaces = new ArrayList<String>( namespaces );
            namespaces.addAll( storageNamespaces );
        }
        return namespaces;
    }

    public Collection<String> getProjects( String repoId, String namespace )
        throws MetadataResolutionException
    {
        Collection<String> projects = metadataRepository.getProjects( repoId, namespace );
        Collection<String> exclusions = new ArrayList<String>( projects );
        exclusions.addAll( metadataRepository.getNamespaces( repoId, namespace ) );
        Collection<String> storageProjects = storageResolver.getProjects( repoId, namespace, new ExcludesFilter<String>(
            exclusions ) );
        if ( storageProjects != null && !storageProjects.isEmpty() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Resolved projects from storage: " + storageProjects );
            }
            for ( String projectId : storageProjects )
            {
                ProjectMetadata projectMetadata = storageResolver.getProject( repoId, namespace, projectId );
                if ( projectMetadata != null )
                {
                    try
                    {
                        metadataRepository.updateProject( repoId, projectMetadata );
                    }
                    catch ( MetadataRepositoryException e )
                    {
                        log.warn( "Unable to persist resolved information: " + e.getMessage(), e );
                    }
                }
            }
            projects = new ArrayList<String>( projects );
            projects.addAll( storageProjects );
        }
        return projects;
    }

    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
        throws MetadataResolutionException
    {
        Collection<String> projectVersions = metadataRepository.getProjectVersions( repoId, namespace, projectId );
        Collection<String> storageProjectVersions = storageResolver.getProjectVersions( repoId, namespace, projectId,
                                                                                        new ExcludesFilter<String>(
                                                                                            projectVersions ) );
        if ( storageProjectVersions != null && !storageProjectVersions.isEmpty() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Resolved project versions from storage: " + storageProjectVersions );
            }
            for ( String projectVersion : storageProjectVersions )
            {
                try
                {
                    ProjectVersionMetadata versionMetadata = storageResolver.getProjectVersion( repoId, namespace,
                                                                                                projectId,
                                                                                                projectVersion );
                    if ( versionMetadata != null )
                    {
                        metadataRepository.updateProjectVersion( repoId, namespace, projectId, versionMetadata );
                    }
                }
                catch ( MetadataResolutionException e )
                {
                    log.warn( "Not update project in metadata repository due to an error resolving it from storage: " +
                                  e.getMessage() );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "Unable to persist resolved information: " + e.getMessage(), e );
                }
            }
            projectVersions = new ArrayList<String>( projectVersions );
            projectVersions.addAll( storageProjectVersions );
        }
        return projectVersions;
    }

    public Collection<ArtifactMetadata> getArtifacts( String repoId, String namespace, String projectId,
                                                      String projectVersion )
        throws MetadataResolutionException
    {
        Collection<ArtifactMetadata> artifacts = metadataRepository.getArtifacts( repoId, namespace, projectId,
                                                                                  projectVersion );
        Collection<ArtifactMetadata> storageArtifacts = storageResolver.getArtifacts( repoId, namespace, projectId,
                                                                                      projectVersion,
                                                                                      new ExcludesFilter<String>(
                                                                                          createArtifactIdList(
                                                                                              artifacts ) ) );
        if ( storageArtifacts != null && !storageArtifacts.isEmpty() )
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Resolved artifacts from storage: " + storageArtifacts );
            }
            for ( ArtifactMetadata artifact : storageArtifacts )
            {
                try
                {
                    metadataRepository.updateArtifact( repoId, namespace, projectId, projectVersion, artifact );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "Unable to persist resolved information: " + e.getMessage(), e );
                }
            }
            artifacts = new ArrayList<ArtifactMetadata>( artifacts );
            artifacts.addAll( storageArtifacts );
        }
        return artifacts;
    }

    private Collection<String> createArtifactIdList( Collection<ArtifactMetadata> artifacts )
    {
        Collection<String> artifactIds = new ArrayList<String>();
        for ( ArtifactMetadata artifact : artifacts )
        {
            artifactIds.add( artifact.getId() );
        }
        return artifactIds;
    }
}
