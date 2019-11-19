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
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.filter.ExcludesFilter;
import org.apache.archiva.metadata.repository.storage.ReadMetadataRequest;
import org.apache.archiva.metadata.repository.storage.RepositoryStorage;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataInvalidException;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataNotFoundException;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageRuntimeException;
import org.apache.archiva.components.cache.Cache;
import org.apache.archiva.metadata.audit.RepositoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Default implementation of the metadata resolver API. At present it will handle updating the content repository
 * from new or changed information in the model and artifacts from the repository storage.
 * </p>
 * <p>
 * This is a singleton component to allow an alternate implementation to be provided. It is intended to be the same
 * system-wide for the whole content repository instead of on a per-managed-repository basis. Therefore, the session is
 * passed in as an argument to obtain any necessary resources, rather than the class being instantiated within the
 * session in the context of a single managed repository's resolution needs.
 * </p>
 * <p>
 * Note that the caller is responsible for the session, such as closing and saving (which is implied by the resolver
 * being obtained from within the session). The {@link RepositorySession#markDirty()} method is used as a hint to ensure
 * that the session knows we've made changes at close. We cannot ensure the changes will be persisted if the caller
 * chooses to revert first. This is preferable to storing the metadata immediately - a separate session would require
 * having a bi-directional link with the session factory, and saving the existing session might save other changes
 * unknowingly by the caller.
 * </p>
 */
@Service("metadataResolver#default")
public class DefaultMetadataResolver
    implements MetadataResolver
{

    private Logger log = LoggerFactory.getLogger( DefaultMetadataResolver.class );

    /**
     * <p>
     * FIXME: this needs to be configurable based on storage type - and could also be instantiated per repo. Change to a
     * factory, and perhaps retrieve from the session. We should avoid creating one per request, however.
     * </p>
     * <p>
     * TODO: Also need to accommodate availability of proxy module
     * ... could be a different type since we need methods to modify the storage metadata, which would also allow more
     * appropriate methods to pass in the already determined repository configuration, for example, instead of the ID
     * </p>
     */
    @Inject
    @Named(value = "repositoryStorage#maven2")
    private RepositoryStorage repositoryStorage;

    @Inject
    @Autowired(required = false)
    private List<RepositoryListener> listeners = new ArrayList<>();

    /**
     * Cache used for namespaces
     */
    @Inject
    @Named( value = "cache#namespaces" )
    private Cache<String, Collection<String>> namespacesCache;

    @Override
    public ProjectVersionMetadata resolveProjectVersion( RepositorySession session, String repoId, String namespace,
                                                         String projectId, String projectVersion )
        throws MetadataResolutionException
    {
        MetadataRepository metadataRepository = session.getRepository();

        ProjectVersionMetadata metadata =
            metadataRepository.getProjectVersion( session, repoId, namespace, projectId, projectVersion );
        // TODO: do we want to detect changes as well by comparing timestamps? isProjectVersionNewerThan(updated)
        //       in such cases we might also remove/update stale metadata, including adjusting plugin-based facets
        //       This would also be better than checking for completeness - we can then refresh only when fixed (though
        //       sometimes this has an additional dependency - such as a parent - requesting the user to force an update
        //       may then work here and be more efficient than always trying again)
        if ( metadata == null || metadata.isIncomplete() )
        {
            try
            {
                ReadMetadataRequest readMetadataRequest =
                    new ReadMetadataRequest().repositoryId( repoId ).namespace( namespace ).projectId(
                        projectId ).projectVersion( projectVersion ).browsingRequest( true );
                metadata = repositoryStorage.readProjectVersionMetadata( readMetadataRequest );

                log.debug( "Resolved project version metadata from storage: {}", metadata );

                // FIXME: make this a more generic post-processing that plugins can take advantage of
                //       eg. maven projects should be able to process parent here
                if ( !metadata.getDependencies().isEmpty() )
                {
                    ProjectVersionReference ref = new ProjectVersionReference();
                    ref.setNamespace( namespace );
                    ref.setProjectId( projectId );
                    ref.setProjectVersion( projectVersion );
                    ref.setReferenceType( ProjectVersionReference.ReferenceType.DEPENDENCY );
                }
                try
                {
                    for ( RepositoryListener listener : listeners )
                    {
                        listener.addArtifact( session, repoId, namespace, projectId, metadata );
                    }
                    metadataRepository.updateProjectVersion( session, repoId, namespace, projectId, metadata );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "Unable to persist resolved information: {}", e.getMessage(), e );
                }

                session.markDirty();
            }
            catch ( RepositoryStorageMetadataInvalidException e )
            {
                for ( RepositoryListener listener : listeners )
                {
                    listener.addArtifactProblem( session, repoId, namespace, projectId, projectVersion, e );
                }
                throw new MetadataResolutionException( e.getMessage(), e );
            }
            catch ( RepositoryStorageMetadataNotFoundException e )
            {
                for ( RepositoryListener listener : listeners )
                {
                    listener.addArtifactProblem( session, repoId, namespace, projectId, projectVersion, e );
                }
                // no need to rethrow - return null
            }
            catch ( RepositoryStorageRuntimeException e )
            {
                for ( RepositoryListener listener : listeners )
                {
                    listener.addArtifactProblem( session, repoId, namespace, projectId, projectVersion, e );
                }
                throw new MetadataResolutionException( e.getMessage(), e );
            }

        }
        return metadata;
    }

    @Override
    public Collection<ProjectVersionReference> resolveProjectReferences( RepositorySession session, String repoId,
                                                                         String namespace, String projectId,
                                                                         String projectVersion )
        throws MetadataResolutionException
    {
        // TODO: is this assumption correct? could a storage mech. actually know all references in a non-Maven scenario?
        // not passed to the storage mechanism as resolving references would require iterating all artifacts
        MetadataRepository metadataRepository = session.getRepository();
        return metadataRepository.getProjectReferences( session, repoId, namespace, projectId, projectVersion );
    }

    @Override
    public Collection<String> resolveRootNamespaces( RepositorySession session, String repoId )
        throws MetadataResolutionException
    {
        try
        {

            Collection<String> namespaces = namespacesCache.get( repoId );
            if ( namespaces != null )
            {
                return namespaces;
            }

            MetadataRepository metadataRepository = session.getRepository();
            namespaces = metadataRepository.getRootNamespaces( session, repoId );
            Collection<String> storageNamespaces =
                repositoryStorage.listRootNamespaces( repoId, new ExcludesFilter<String>( namespaces ) );
            if ( storageNamespaces != null && !storageNamespaces.isEmpty() )
            {

                log.debug( "Resolved root namespaces from storage: {}", storageNamespaces );

                for ( String n : storageNamespaces )
                {
                    try
                    {
                        metadataRepository.updateNamespace( session, repoId, n );
                        // just invalidate cache entry
                        String cacheKey = repoId + "-" + n;
                        namespacesCache.remove( cacheKey );
                    }
                    catch ( MetadataRepositoryException e )
                    {
                        log.warn( "Unable to persist resolved information: {}", e.getMessage(), e );
                    }
                }
                session.markDirty();

                namespaces = new ArrayList<>( namespaces );
                namespaces.addAll( storageNamespaces );
            }

            namespacesCache.put( repoId, namespaces );

            return namespaces;
        }
        catch ( RepositoryStorageRuntimeException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<String> resolveNamespaces( RepositorySession session, String repoId, String namespace )
        throws MetadataResolutionException
    {
        try
        {
            MetadataRepository metadataRepository = session.getRepository();
            String cacheKey = repoId + "-" + namespace;
            Collection<String> namespaces = namespacesCache.get( cacheKey );
            if ( namespaces == null )
            {
                namespaces = metadataRepository.getChildNamespaces( session, repoId, namespace );
                namespacesCache.put( cacheKey, namespaces );
            }
            Collection<String> exclusions = new ArrayList<>( namespaces );
            exclusions.addAll( metadataRepository.getProjects( session, repoId, namespace ) );
            Collection<String> storageNamespaces =
                repositoryStorage.listNamespaces( repoId, namespace, new ExcludesFilter<String>( exclusions ) );
            if ( storageNamespaces != null && !storageNamespaces.isEmpty() )
            {

                log.debug( "Resolved namespaces from storage: {}", storageNamespaces );

                for ( String n : storageNamespaces )
                {
                    try
                    {
                        metadataRepository.updateNamespace( session, repoId, namespace + "." + n );
                        // just invalidate cache entry
                        cacheKey = repoId + "-" + namespace + "." + n;
                        namespacesCache.remove( cacheKey );
                    }
                    catch ( MetadataRepositoryException e )
                    {
                        log.warn( "Unable to persist resolved information: {}", e.getMessage(), e );
                    }
                }
                session.markDirty();

                namespaces = new ArrayList<>( namespaces );
                namespaces.addAll( storageNamespaces );
            }
            return namespaces;
        }
        catch ( RepositoryStorageRuntimeException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<String> resolveProjects( RepositorySession session, String repoId, String namespace )
        throws MetadataResolutionException
    {
        try
        {
            MetadataRepository metadataRepository = session.getRepository();
            Collection<String> projects = metadataRepository.getProjects( session, repoId, namespace );
            Collection<String> exclusions = new ArrayList<>( projects );

            String cacheKey = repoId + "-" + namespace;
            Collection<String> namespaces = namespacesCache.get( cacheKey );
            if ( namespaces == null )
            {
                namespaces = metadataRepository.getChildNamespaces( session, repoId, namespace );
                namespacesCache.put( cacheKey, namespaces );
            }

            exclusions.addAll( namespaces );

            Collection<String> storageProjects =
                repositoryStorage.listProjects( repoId, namespace, new ExcludesFilter<>( exclusions ) );
            if ( storageProjects != null && !storageProjects.isEmpty() )
            {

                log.debug( "Resolved projects from storage: {}", storageProjects );
                for ( String projectId : storageProjects )
                {
                    ProjectMetadata projectMetadata =
                        repositoryStorage.readProjectMetadata( repoId, namespace, projectId );
                    if ( projectMetadata != null )
                    {
                        try
                        {
                            metadataRepository.updateProject( session, repoId, projectMetadata );
                        }
                        catch ( MetadataRepositoryException e )
                        {
                            log.warn( "Unable to persist resolved information: {}", e.getMessage(), e );
                        }
                    }
                }
                session.markDirty();

                projects = new ArrayList<>( projects );
                projects.addAll( storageProjects );
            }
            return projects;
        }
        catch ( RepositoryStorageRuntimeException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<String> resolveProjectVersions( RepositorySession session, String repoId, String namespace,
                                                      String projectId )
        throws MetadataResolutionException
    {
        try
        {
            MetadataRepository metadataRepository = session.getRepository();

            Collection<String> projectVersions = metadataRepository.getProjectVersions( session, repoId, namespace, projectId );
            Collection<String> storageProjectVersions =
                repositoryStorage.listProjectVersions( repoId, namespace, projectId,
                                                       new ExcludesFilter<String>( projectVersions ) );
            if ( storageProjectVersions != null && !storageProjectVersions.isEmpty() )
            {
                log.debug( "Resolved project versions from storage: {}", storageProjectVersions );

                for ( String projectVersion : storageProjectVersions )
                {
                    try
                    {
                        ReadMetadataRequest readMetadataRequest =
                            new ReadMetadataRequest().repositoryId( repoId ).namespace( namespace ).projectId(
                                projectId ).projectVersion( projectVersion );
                        ProjectVersionMetadata versionMetadata =
                            repositoryStorage.readProjectVersionMetadata( readMetadataRequest );
                        for ( RepositoryListener listener : listeners )
                        {
                            listener.addArtifact( session, repoId, namespace, projectId, versionMetadata );
                        }

                        metadataRepository.updateProjectVersion( session, repoId, namespace, projectId, versionMetadata );
                    }
                    catch ( MetadataRepositoryException e )
                    {
                        log.warn( "Unable to persist resolved information: {}", e.getMessage(), e );
                    }
                    catch ( RepositoryStorageMetadataInvalidException e )
                    {
                        log.warn(
                            "Not update project in metadata repository due to an error resolving it from storage: {}",
                            e.getMessage() );

                        for ( RepositoryListener listener : listeners )
                        {
                            listener.addArtifactProblem( session, repoId, namespace, projectId, projectVersion, e );
                        }
                    }
                    catch ( RepositoryStorageMetadataNotFoundException e )
                    {
                        for ( RepositoryListener listener : listeners )
                        {
                            listener.addArtifactProblem( session, repoId, namespace, projectId, projectVersion, e );
                        }
                    }
                }
                session.markDirty();

                projectVersions = new ArrayList<>( projectVersions );
                projectVersions.addAll( storageProjectVersions );
            }
            return projectVersions;
        }
        catch ( RepositoryStorageRuntimeException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<ArtifactMetadata> resolveArtifacts( RepositorySession session, String repoId, String namespace,
                                                          String projectId, String projectVersion )
        throws MetadataResolutionException
    {
        try
        {
            MetadataRepository metadataRepository = session.getRepository();
            Collection<ArtifactMetadata> artifacts =
                metadataRepository.getArtifacts( session, repoId, namespace, projectId, projectVersion );
            ExcludesFilter<String> filter = new ExcludesFilter<String>( createArtifactIdList( artifacts ) );

            ReadMetadataRequest readMetadataRequest =
                new ReadMetadataRequest().repositoryId( repoId ).namespace( namespace ).projectId(
                    projectId ).projectVersion( projectVersion ).filter( filter );

            Collection<ArtifactMetadata> storageArtifacts =
                repositoryStorage.readArtifactsMetadata( readMetadataRequest );
            if ( storageArtifacts != null && !storageArtifacts.isEmpty() )
            {

                log.debug( "Resolved artifacts from storage: {}", storageArtifacts );

                for ( ArtifactMetadata artifact : storageArtifacts )
                {
                    try
                    {
                        metadataRepository.updateArtifact( session, repoId, namespace, projectId, projectVersion, artifact );
                    }
                    catch ( MetadataRepositoryException e )
                    {
                        log.warn( "Unable to persist resolved information: {}", e.getMessage(), e );
                    }
                }
                session.markDirty();

                artifacts = new ArrayList<>( artifacts );
                artifacts.addAll( storageArtifacts );
            }
            return artifacts;
        }
        catch ( RepositoryStorageRuntimeException e )
        {
            for ( RepositoryListener listener : listeners )
            {
                listener.addArtifactProblem( session, repoId, namespace, projectId, projectVersion, e );
            }
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    private Collection<String> createArtifactIdList( Collection<ArtifactMetadata> artifacts )
    {
        Collection<String> artifactIds = new ArrayList<>();
        for ( ArtifactMetadata artifact : artifacts )
        {
            artifactIds.add( artifact.getId() );
        }
        return artifactIds;
    }
}
