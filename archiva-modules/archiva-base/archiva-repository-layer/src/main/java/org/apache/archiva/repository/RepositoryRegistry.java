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

import org.apache.archiva.configuration.*;
import org.apache.archiva.indexer.*;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.redback.components.registry.RegistryException;
import org.apache.archiva.repository.features.IndexCreationEvent;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registry for repositories. This is the central entry point for repositories. It provides methods for
 * retrieving, adding and removing repositories.
 *
 * The modification methods addXX and removeXX persist the changes immediately to the configuration. If the
 * configuration save fails the changes are rolled back.
 *
 * TODO: Audit events should be sent, but we don't want dependency to the repsitory-metadata-api
 */
@Service( "repositoryRegistry" )
public class RepositoryRegistry implements ConfigurationListener, RepositoryEventHandler, RepositoryEventListener {

    private static final Logger log = LoggerFactory.getLogger( RepositoryRegistry.class );

    /**
     * We inject all repository providers
     */
    @Inject
    List<RepositoryProvider> repositoryProviders;

    @Inject
    IndexManagerFactory indexManagerFactory;

    @Inject
    ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named("repositoryContentFactory#default")
    RepositoryContentFactory repositoryContentFactory;

    private List<RepositoryEventListener> listeners = new ArrayList<>();


    private Map<String, ManagedRepository> managedRepositories = new HashMap<>( );
    private Map<String, ManagedRepository> uManagedRepository = Collections.unmodifiableMap( managedRepositories );

    private Map<String, RemoteRepository> remoteRepositories = new HashMap<>( );
    private Map<String, RemoteRepository> uRemoteRepositories = Collections.unmodifiableMap( remoteRepositories );

    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock( );

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration) {
        this.archivaConfiguration = archivaConfiguration;
    }

    @PostConstruct
    private void initialize( )
    {
        rwLock.writeLock( ).lock( );
        try
        {
            log.debug("Initializing repository registry");
            for(ManagedRepository rep : managedRepositories.values()) {
                rep.close();
            }
            managedRepositories.clear( );
            managedRepositories.putAll( getManagedRepositoriesFromConfig( ) );
            for (RemoteRepository repo : remoteRepositories.values()) {
                repo.close();
            }
            remoteRepositories.clear( );
            remoteRepositories.putAll( getRemoteRepositoriesFromConfig( ) );
            // archivaConfiguration.addChangeListener(this);
            archivaConfiguration.addListener(this);
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
    }

    @PreDestroy
    public void destroy() {
        for(ManagedRepository rep : managedRepositories.values()) {
            rep.close();
        }
        for (RemoteRepository repo : remoteRepositories.values()) {
            repo.close();
        }
    }



    private Map<RepositoryType, RepositoryProvider> createProviderMap( )
    {
        Map<RepositoryType, RepositoryProvider> map = new HashMap<>( );
        if ( repositoryProviders != null )
        {
            for ( RepositoryProvider provider : repositoryProviders )
            {
                for ( RepositoryType type : provider.provides( ) )
                {
                    map.put( type, provider );
                }
            }
        }
        return map;
    }

    private RepositoryProvider getProvider( RepositoryType type ) throws RepositoryException
    {
        return repositoryProviders.stream( ).filter( repositoryProvider -> repositoryProvider.provides( ).contains( type ) ).findFirst( ).orElseThrow( ( ) -> new RepositoryException( "Repository type cannot be handled: " + type ) );
    }

    private Map<String, ManagedRepository> getManagedRepositoriesFromConfig( )
    {
        try
        {
            List<ManagedRepositoryConfiguration> managedRepoConfigs =
                getArchivaConfiguration( ).getConfiguration( ).getManagedRepositories( );

            if ( managedRepoConfigs == null )
            {
                return Collections.EMPTY_MAP;
            }

            Map<String, ManagedRepository> managedRepos = new LinkedHashMap<>( managedRepoConfigs.size( ) );

            Map<RepositoryType, RepositoryProvider> providerMap = createProviderMap( );
            for ( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
            {
                RepositoryType repositoryType = RepositoryType.valueOf( repoConfig.getType( ) );
                if ( providerMap.containsKey( repositoryType ) )
                {
                    try
                    {
                        ManagedRepository repo = createNewManagedRepository( providerMap.get( repositoryType ), repoConfig );
                        managedRepos.put( repo.getId( ), repo );
                    }
                    catch ( Exception e )
                    {
                        log.error( "Could not create managed repository {}: {}", repoConfig.getId( ), e.getMessage( ), e );
                    }
                }
            }
            return managedRepos;
        } catch (Throwable e) {
            log.error("Could not initialize repositories from config: {}",e.getMessage(), e );
            //noinspection unchecked
            return Collections.EMPTY_MAP;
        }
    }

    private ManagedRepository createNewManagedRepository( RepositoryProvider provider, ManagedRepositoryConfiguration cfg ) throws RepositoryException
    {
        log.debug("Creating repo {}", cfg.getId());
        ManagedRepository repo = provider.createManagedInstance( cfg );
        repo.addListener(this);
        updateRepositoryReferences( provider, repo, cfg , null);
        return repo;

    }

    private void updateRepositoryReferences(RepositoryProvider provider, ManagedRepository repo, ManagedRepositoryConfiguration cfg, Configuration configuration) throws RepositoryException
    {
        log.debug("Updating references of repo {}",repo.getId());
        if ( repo.supportsFeature( StagingRepositoryFeature.class ) )
        {
            StagingRepositoryFeature feature = repo.getFeature( StagingRepositoryFeature.class ).get( );
            if ( feature.isStageRepoNeeded( ) && feature.getStagingRepository() == null)
            {
                ManagedRepository stageRepo = getStagingRepository( provider, cfg, configuration);
                managedRepositories.put(stageRepo.getId(), stageRepo);
                feature.setStagingRepository( stageRepo );
                if (configuration!=null) {
                    replaceOrAddRepositoryConfig( provider.getManagedConfiguration( stageRepo ), configuration );
                }
            }
        }
        if ( repo instanceof EditableManagedRepository)
        {
            EditableManagedRepository editableRepo = (EditableManagedRepository) repo;
            if (repo.getContent()==null) {
                editableRepo.setContent(repositoryContentFactory.getManagedRepositoryContent(repo));
            }
            log.debug("Index repo: "+repo.hasIndex());
            if (repo.hasIndex() && repo.getIndexingContext()==null) {
                log.debug("Creating indexing context for {}", repo.getId());
                createIndexingContext(editableRepo);
            }
        }

    }

    private ArchivaIndexManager getIndexManager(RepositoryType type) {
        return indexManagerFactory.getIndexManager(type);
    }

    private void createIndexingContext(EditableRepository editableRepo) throws RepositoryException {
        if (editableRepo.supportsFeature(IndexCreationFeature.class)) {
            ArchivaIndexManager idxManager = getIndexManager(editableRepo.getType());
            try {
                editableRepo.setIndexingContext(idxManager.createContext(editableRepo));
                idxManager.updateLocalIndexPath(editableRepo);
            } catch (IndexCreationFailedException e) {
                throw new RepositoryException("Could not create index for repository "+editableRepo.getId()+": "+e.getMessage(),e);
            }
        }
    }

    private ManagedRepository getStagingRepository(RepositoryProvider provider, ManagedRepositoryConfiguration baseRepoCfg, Configuration configuration) throws RepositoryException
    {
        ManagedRepository stageRepo = getManagedRepository( baseRepoCfg.getId( ) + StagingRepositoryFeature.STAGING_REPO_POSTFIX );
        if ( stageRepo == null )
        {
            stageRepo = provider.createStagingInstance( baseRepoCfg );
            if (stageRepo.supportsFeature(StagingRepositoryFeature.class)) {
                stageRepo.getFeature(StagingRepositoryFeature.class).get().setStageRepoNeeded(false);
            }
            ManagedRepositoryConfiguration stageCfg = provider.getManagedConfiguration( stageRepo );
            updateRepositoryReferences( provider, stageRepo, stageCfg, configuration);
        }
        return stageRepo;
    }




    private Map<String, RemoteRepository> getRemoteRepositoriesFromConfig( )
    {
        try
        {
            List<RemoteRepositoryConfiguration> remoteRepoConfigs =
                getArchivaConfiguration( ).getConfiguration( ).getRemoteRepositories( );

            if ( remoteRepoConfigs == null )
            {
                //noinspection unchecked
                return Collections.EMPTY_MAP;
            }

            Map<String, RemoteRepository> remoteRepos = new LinkedHashMap<>( remoteRepoConfigs.size( ) );

            Map<RepositoryType, RepositoryProvider> providerMap = createProviderMap( );
            for ( RemoteRepositoryConfiguration repoConfig : remoteRepoConfigs )
            {
                RepositoryType repositoryType = RepositoryType.valueOf( repoConfig.getType( ) );
                if ( providerMap.containsKey( repositoryType ) )
                {
                    RepositoryProvider provider = getProvider( repositoryType );
                    try
                    {

                        RemoteRepository remoteRepository = createNewRemoteRepository( provider, repoConfig );
                        remoteRepos.put( repoConfig.getId( ), remoteRepository);
                    }
                    catch ( Exception e )
                    {
                        log.error( "Could not create repository {} from config: {}", repoConfig.getId( ), e.getMessage( ), e );
                    }
                }
            }

            return remoteRepos;
        } catch (Throwable e) {
            log.error("Could not initialize remote repositories from config: {}", e.getMessage(), e);
            //noinspection unchecked
            return Collections.EMPTY_MAP;
        }
    }

    private RemoteRepository createNewRemoteRepository( RepositoryProvider provider, RemoteRepositoryConfiguration cfg ) throws RepositoryException
    {
        log.debug("Creating remote repo {}", cfg.getId());
        RemoteRepository repo = provider.createRemoteInstance( cfg );
        repo.addListener(this);
        updateRepositoryReferences( provider, repo, cfg , null);
        return repo;

    }

    private void updateRepositoryReferences( RepositoryProvider provider, RemoteRepository repo, RemoteRepositoryConfiguration cfg, Configuration configuration) throws RepositoryException
    {
        if ( repo instanceof EditableRemoteRepository && repo.getContent() == null)
        {
            EditableRemoteRepository editableRepo = (EditableRemoteRepository) repo;
            if (repo.getContent()==null) {
                editableRepo.setContent( repositoryContentFactory.getRemoteRepositoryContent( repo ) );
            }
            if (repo.supportsFeature(IndexCreationFeature.class) && repo.getIndexingContext()==null ) {
                createIndexingContext(editableRepo);
            }
        }
    }

    private ArchivaConfiguration getArchivaConfiguration( )
    {
        return this.archivaConfiguration;
    }

    /**
     * Returns all repositories that are registered. There is no defined order of the returned repositories.
     *
     * @return a list of managed and remote repositories
     */
    public Collection<Repository> getRepositories( )
    {
        rwLock.readLock( ).lock( );
        try
        {
            return Stream.concat( managedRepositories.values( ).stream( ), remoteRepositories.values( ).stream( ) ).collect( Collectors.toList( ) );
        }
        finally
        {
            rwLock.readLock( ).unlock( );
        }
    }

    /**
     * Returns only the managed repositories. There is no defined order of the returned repositories.
     *
     * @return a list of managed repositories
     */
    public Collection<ManagedRepository> getManagedRepositories( )
    {
        rwLock.readLock().lock();
        try
        {
            return uManagedRepository.values( );
        } finally
        {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Returns only the remote repositories. There is no defined order of the returned repositories.
     *
     * @return a list of remote repositories
     */
    public Collection<RemoteRepository> getRemoteRepositories( )
    {
        rwLock.readLock().lock();
        try
        {
            return uRemoteRepositories.values( );
        } finally
        {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Returns the repository with the given id. The returned repository may be a managed or remote repository.
     * It returns null, if no repository is registered with the given id.
     *
     * @param repoId the repository id
     * @return the repository if found, otherwise null
     */
    public Repository getRepository( String repoId )
    {
        rwLock.readLock( ).lock( );
        try
        {
            log.debug("getRepository {}", repoId);
            if ( managedRepositories.containsKey( repoId ) )
            {
                log.debug("Managed repo");
                return managedRepositories.get( repoId );
            }
            else
            {
                log.debug("Remote repo");
                return remoteRepositories.get( repoId );
            }
        }
        finally
        {
            rwLock.readLock( ).unlock( );
        }
    }

    /**
     * Convenience method, that returns the managed repository with the given id.
     * It returns null, if no managed repository is registered with this id.
     *
     * @param repoId the repository id
     * @return the managed repository if found, otherwise null
     */
    public ManagedRepository getManagedRepository( String repoId )
    {
        rwLock.readLock( ).lock( );
        try
        {
            return managedRepositories.get( repoId );
        }
        finally
        {
            rwLock.readLock( ).unlock( );
        }
    }

    /**
     * Convenience method, that returns the remote repository with the given id.
     * It returns null, if no remote repository is registered with this id.
     *
     * @param repoId the repository id
     * @return the remote repository if found, otherwise null
     */
    public RemoteRepository getRemoteRepository( String repoId )
    {
        rwLock.readLock( ).lock( );
        try
        {
            return remoteRepositories.get( repoId );
        }
        finally
        {
            rwLock.readLock( ).unlock( );
        }
    }

    /**
     * Adds a new repository to the current list, or replaces the repository definition with
     * the same id, if it exists already.
     * The change is saved to the configuration immediately.
     *
     * @param managedRepository the new repository.
     * @throws RepositoryException if the new repository could not be saved to the configuration.
     */
    public ManagedRepository putRepository( ManagedRepository managedRepository ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = managedRepository.getId();
            if (remoteRepositories.containsKey( id )) {
                throw new RepositoryException( "There exists a remote repository with id "+id+". Could not update with managed repository." );
            }

            ManagedRepository originRepo = managedRepositories.put( id, managedRepository );
            try
            {
                if (originRepo!=null) {
                    originRepo.close();
                }
                RepositoryProvider provider = getProvider( managedRepository.getType() );
                ManagedRepositoryConfiguration newCfg = provider.getManagedConfiguration( managedRepository );
                Configuration configuration = getArchivaConfiguration( ).getConfiguration( );
                updateRepositoryReferences( provider, managedRepository, newCfg, configuration );
                ManagedRepositoryConfiguration oldCfg = configuration.findManagedRepositoryById( id );
                if (oldCfg!=null) {
                    configuration.removeManagedRepository( oldCfg );
                }
                configuration.addManagedRepository( newCfg );
                getArchivaConfiguration( ).save( configuration );
                return managedRepository;
            }
            catch ( Exception e )
            {
                // Rollback
                if ( originRepo != null )
                {
                    managedRepositories.put( id, originRepo );
                } else {
                    managedRepositories.remove(id);
                }
                log.error("Exception during configuration update {}", e.getMessage(), e);
                throw new RepositoryException( "Could not save the configuration" + (e.getMessage( )==null?"":": "+e.getMessage()) );
            }
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
    }

    /**
     * Adds a new repository or updates the repository with the same id, if it exists already.
     * The configuration is saved immediately.
     *
     * @param managedRepositoryConfiguration the repository configuration
     * @return the updated or created repository
     * @throws RepositoryException if an error occurs, or the configuration is not valid.
     */
    public ManagedRepository putRepository( ManagedRepositoryConfiguration managedRepositoryConfiguration) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = managedRepositoryConfiguration.getId();
            final RepositoryType repositoryType = RepositoryType.valueOf( managedRepositoryConfiguration.getType() );
            Configuration configuration = getArchivaConfiguration().getConfiguration();
            ManagedRepository repo = managedRepositories.get(id);
            ManagedRepositoryConfiguration oldCfg = repo!=null ? getProvider( repositoryType ).getManagedConfiguration( repo ) : null;
            repo = putRepository( managedRepositoryConfiguration, configuration );
            try
            {
                getArchivaConfiguration().save(configuration);
            }
            catch ( IndeterminateConfigurationException | RegistryException e )
            {
                if (oldCfg!=null) {
                    getProvider( repositoryType ).updateManagedInstance( (EditableManagedRepository)repo, oldCfg );
                }
                log.error("Could not save the configuration for repository {}: {}", id, e.getMessage(),e );
                throw new RepositoryException( "Could not save the configuration for repository "+id+": "+e.getMessage() );
            }
            return repo;
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }

    }

    /**
     * Adds a new repository or updates the repository with the same id. The given configuration object is updated, but
     * the configuration is not saved.
     *
     * @param managedRepositoryConfiguration the new or changed repository configuration
     * @param configuration the configuration object
     * @return the new or updated repository
     * @throws RepositoryException if the configuration cannot be saved or updated
     */
    public ManagedRepository putRepository( ManagedRepositoryConfiguration managedRepositoryConfiguration, Configuration configuration) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = managedRepositoryConfiguration.getId();
            final RepositoryType repoType = RepositoryType.valueOf( managedRepositoryConfiguration.getType() );
            ManagedRepository repo;
            if (managedRepositories.containsKey( id )) {
                repo = managedRepositories.get(id);
                if (repo instanceof EditableManagedRepository)
                {
                    getProvider( repoType ).updateManagedInstance( (EditableManagedRepository) repo, managedRepositoryConfiguration );
                } else {
                    throw new RepositoryException( "The repository is not editable "+id );
                }
            } else
            {
                repo = getProvider( repoType ).createManagedInstance( managedRepositoryConfiguration );
                repo.addListener(this);
                managedRepositories.put(id, repo);
            }
            updateRepositoryReferences( getProvider( repoType  ), repo, managedRepositoryConfiguration, configuration );
            replaceOrAddRepositoryConfig( managedRepositoryConfiguration, configuration );
            return repo;
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
    }

    private void replaceOrAddRepositoryConfig(ManagedRepositoryConfiguration managedRepositoryConfiguration, Configuration configuration) {
        ManagedRepositoryConfiguration oldCfg = configuration.findManagedRepositoryById( managedRepositoryConfiguration.getId() );
        if ( oldCfg !=null) {
            configuration.removeManagedRepository( oldCfg );
        }
        configuration.addManagedRepository( managedRepositoryConfiguration );
    }

    private void replaceOrAddRepositoryConfig(RemoteRepositoryConfiguration remoteRepositoryConfiguration, Configuration configuration) {
        RemoteRepositoryConfiguration oldCfg = configuration.findRemoteRepositoryById( remoteRepositoryConfiguration.getId() );
        if ( oldCfg !=null) {
            configuration.removeRemoteRepository( oldCfg );
        }
        configuration.addRemoteRepository( remoteRepositoryConfiguration );
    }

    public RemoteRepository putRepository( RemoteRepository remoteRepository, Configuration configuration) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = remoteRepository.getId();
            if (managedRepositories.containsKey( id )) {
                throw new RepositoryException( "There exists a managed repository with id "+id+". Could not update with remote repository." );
            }
            RemoteRepository originRepo = remoteRepositories.put( id, remoteRepository );
            RemoteRepositoryConfiguration oldCfg=null;
            RemoteRepositoryConfiguration newCfg=null;
            try
            {
                if (originRepo!=null) {
                    originRepo.close();
                }
                final RepositoryProvider provider = getProvider( remoteRepository.getType() );
                newCfg = provider.getRemoteConfiguration( remoteRepository );
                updateRepositoryReferences( provider, remoteRepository, newCfg, configuration );
                oldCfg = configuration.findRemoteRepositoryById( id );
                if (oldCfg!=null) {
                    configuration.removeRemoteRepository( oldCfg );
                }
                configuration.addRemoteRepository( newCfg );
                return remoteRepository;
            }
            catch ( Exception e )
            {
                // Rollback
                if ( originRepo != null )
                {
                    remoteRepositories.put( id, originRepo );
                } else {
                    remoteRepositories.remove( id);
                }
                if (oldCfg!=null) {
                    RemoteRepositoryConfiguration cfg = configuration.findRemoteRepositoryById( id );
                    if (cfg!=null) {
                        configuration.removeRemoteRepository( cfg );
                        configuration.addRemoteRepository( oldCfg );
                    }
                }
                log.error("Error while adding remote repository {}", e.getMessage(), e);
                throw new RepositoryException( "Could not save the configuration" + (e.getMessage( )==null?"":": "+e.getMessage()) );
            }
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
    }

    /**
     * Adds a remote repository, or overwrites the repository definition with the same id, if it exists already.
     * The modification is saved to the configuration immediately.
     *
     * @param remoteRepository the remote repository to add
     * @throws RepositoryException if an error occurs during configuration save
     */
    public RemoteRepository putRepository( RemoteRepository remoteRepository ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            Configuration configuration = getArchivaConfiguration().getConfiguration();
            try
            {
                RemoteRepository repo = putRepository( remoteRepository, configuration );
                getArchivaConfiguration().save(configuration);
                return repo;
            }
            catch ( RegistryException | IndeterminateConfigurationException e )
            {
                log.error("Error while saving remote repository {}", e.getMessage(), e);
                throw new RepositoryException( "Could not save the configuration" + (e.getMessage( )==null?"":": "+e.getMessage()) );
            }
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
    }

    /**
     * Adds a new repository or updates the repository with the same id, if it exists already.
     * The configuration is saved immediately.
     *
     * @param remoteRepositoryConfiguration the repository configuration
     * @return the updated or created repository
     * @throws RepositoryException if an error occurs, or the configuration is not valid.
     */
    public RemoteRepository putRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = remoteRepositoryConfiguration.getId();
            final RepositoryType repositoryType = RepositoryType.valueOf( remoteRepositoryConfiguration.getType() );
            Configuration configuration = getArchivaConfiguration().getConfiguration();
            RemoteRepository repo = remoteRepositories.get(id);
            RemoteRepositoryConfiguration oldCfg = repo!=null ? getProvider( repositoryType ).getRemoteConfiguration( repo ) : null;
            repo = putRepository( remoteRepositoryConfiguration, configuration );
            try
            {
                getArchivaConfiguration().save(configuration);
            }
            catch ( IndeterminateConfigurationException | RegistryException e )
            {
                if (oldCfg!=null) {
                    getProvider( repositoryType ).updateRemoteInstance( (EditableRemoteRepository)repo, oldCfg );
                }
                log.error("Could not save the configuration for repository {}: {}", id, e.getMessage(),e );
                throw new RepositoryException( "Could not save the configuration for repository "+id+": "+e.getMessage() );
            }
            return repo;
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }

    }

    /**
     * Adds a new repository or updates the repository with the same id. The given configuration object is updated, but
     * the configuration is not saved.
     *
     * @param remoteRepositoryConfiguration the new or changed repository configuration
     * @param configuration the configuration object
     * @return the new or updated repository
     * @throws RepositoryException if the configuration cannot be saved or updated
     */
    public RemoteRepository putRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration, Configuration configuration) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = remoteRepositoryConfiguration.getId();
            final RepositoryType repoType = RepositoryType.valueOf( remoteRepositoryConfiguration.getType() );
            RemoteRepository repo;
            if (remoteRepositories.containsKey( id )) {
                repo = remoteRepositories.get(id);
                if (repo instanceof EditableRemoteRepository)
                {
                    getProvider( repoType ).updateRemoteInstance( (EditableRemoteRepository) repo, remoteRepositoryConfiguration );
                } else {
                    throw new RepositoryException( "The repository is not editable "+id );
                }
            } else
            {
                repo = getProvider( repoType ).createRemoteInstance( remoteRepositoryConfiguration );
                repo.addListener(this);
                remoteRepositories.put(id, repo);
            }
            updateRepositoryReferences( getProvider( repoType  ), repo, remoteRepositoryConfiguration, configuration );
            replaceOrAddRepositoryConfig( remoteRepositoryConfiguration, configuration );
            return repo;
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }


    }

    public void removeRepository(String repoId) throws RepositoryException {
        Repository repo = getRepository(repoId);
        if (repo!=null) {
            removeRepository(repo);
        }
    }
    public void removeRepository(Repository repo) throws RepositoryException
    {
        if (repo instanceof RemoteRepository ) {
            removeRepository( (RemoteRepository)repo );
        } else if (repo instanceof ManagedRepository) {
            removeRepository( (ManagedRepository)repo);
        } else {
            throw new RepositoryException( "Repository type not known: "+repo.getClass() );
        }
    }

    /**
     * Removes a managed repository from the registry and configuration, if it exists.
     * The change is saved to the configuration immediately.
     *
     * @param managedRepository the managed repository to remove
     * @throws RepositoryException if a error occurs during configuration save
     */
    public void removeRepository( ManagedRepository managedRepository ) throws RepositoryException
    {
        final String id = managedRepository.getId();
        ManagedRepository repo = getManagedRepository( id );
        if (repo!=null) {
            rwLock.writeLock().lock();
            try {
                repo = managedRepositories.remove( id );
                if (repo!=null) {
                    repo.close();
                    Configuration configuration = getArchivaConfiguration().getConfiguration();
                    ManagedRepositoryConfiguration cfg = configuration.findManagedRepositoryById( id );
                    if (cfg!=null) {
                        configuration.removeManagedRepository( cfg );
                    }
                    getArchivaConfiguration().save( configuration );
                }

            }
            catch ( RegistryException | IndeterminateConfigurationException e )
            {
                // Rollback
                log.error("Could not save config after repository removal: {}", e.getMessage(), e);
                managedRepositories.put(repo.getId(), repo);
                throw new RepositoryException( "Could not save configuration after repository removal: "+e.getMessage() );
            } finally
            {
                rwLock.writeLock().unlock();
            }
        }
    }

    public void removeRepository(ManagedRepository managedRepository, Configuration configuration) throws RepositoryException
    {
        final String id = managedRepository.getId();
        ManagedRepository repo = getManagedRepository( id );
        if (repo!=null) {
            rwLock.writeLock().lock();
            try {
                repo = managedRepositories.remove( id );
                if (repo!=null) {
                    repo.close();
                    ManagedRepositoryConfiguration cfg = configuration.findManagedRepositoryById( id );
                    if (cfg!=null) {
                        configuration.removeManagedRepository( cfg );
                    }
                }
            } finally
            {
                rwLock.writeLock().unlock();
            }
        }

    }


    private void doRemoveRepo(RemoteRepository repo, Configuration configuration) {
            repo.close();
            RemoteRepositoryConfiguration cfg = configuration.findRemoteRepositoryById(repo.getId());
            if (cfg != null) {
                configuration.removeRemoteRepository(cfg);
            }
            List<ProxyConnectorConfiguration> proxyConnectors = new ArrayList<>(configuration.getProxyConnectors());
            for (ProxyConnectorConfiguration proxyConnector : proxyConnectors) {
                if (StringUtils.equals(proxyConnector.getTargetRepoId(), repo.getId())) {
                    configuration.removeProxyConnector(proxyConnector);
                }
            }
    }

    /**
     * Removes the remote repository from the registry and configuration.
     * The change is saved to the configuration immediately.
     *
     * @param remoteRepository the remote repository to remove
     * @throws RepositoryException if a error occurs during configuration save
     */
    public void removeRepository( RemoteRepository remoteRepository ) throws RepositoryException
    {

        final String id = remoteRepository.getId();
        RemoteRepository repo = getRemoteRepository( id );
        if (repo!=null) {
            rwLock.writeLock().lock();
            try {
                repo = remoteRepositories.remove( id );
                if (repo!=null) {
                    Configuration configuration = getArchivaConfiguration().getConfiguration();
                    doRemoveRepo(repo, configuration);
                    getArchivaConfiguration().save( configuration );
                }
            }
            catch ( RegistryException | IndeterminateConfigurationException e )
            {
                // Rollback
                log.error("Could not save config after repository removal: {}", e.getMessage(), e);
                remoteRepositories.put(repo.getId(), repo);
                throw new RepositoryException( "Could not save configuration after repository removal: "+e.getMessage() );
            } finally
            {
                rwLock.writeLock().unlock();
            }
        }
    }

    public void removeRepository( RemoteRepository remoteRepository, Configuration configuration) throws RepositoryException
    {
        final String id = remoteRepository.getId();
        RemoteRepository repo = getRemoteRepository( id );
        if (repo!=null) {
            rwLock.writeLock().lock();
            try {
                repo = remoteRepositories.remove( id );
                if (repo!=null) {
                    doRemoveRepo(repo, configuration);
                }
            } finally
            {
                rwLock.writeLock().unlock();
            }
        }

    }

    /**
     * Reloads the registry from the configuration.
     */
    public void reload() {
        initialize();
    }

    /**
     * Resets the indexing context of a given repository.
     *
     * @param repo
     * @throws IndexUpdateFailedException
     */
    public void resetIndexingContext(Repository repo) throws IndexUpdateFailedException {
        if (repo.hasIndex() && repo instanceof EditableRepository) {
            EditableRepository eRepo = (EditableRepository) repo;
            ArchivaIndexingContext newCtx = getIndexManager(repo.getType()).reset(repo.getIndexingContext());
            eRepo.setIndexingContext(newCtx);
        }
    }


    /**
     * Creates a new repository instance with the same settings as this one. The cloned repository is not
     * registered or saved to the configuration.
     *
     * @param repo The origin repository
     * @return The cloned repository.
     */
    public ManagedRepository clone(ManagedRepository repo, String newId) throws RepositoryException
    {
        if (managedRepositories.containsKey(newId) || remoteRepositories.containsKey(newId)) {
            throw new RepositoryException("The given id exists already "+newId);
        }
        RepositoryProvider provider = getProvider(repo.getType());
        ManagedRepositoryConfiguration cfg = provider.getManagedConfiguration(repo);
        cfg.setId(newId);
        ManagedRepository cloned = provider.createManagedInstance(cfg);
        cloned.addListener(this);
        return cloned;
    }

    public <T extends Repository> Repository clone(T repo, String newId) throws RepositoryException {
        if (repo instanceof RemoteRepository ) {
            return this.clone((RemoteRepository)repo, newId);
        } else if (repo instanceof ManagedRepository) {
            return this.clone((ManagedRepository)repo, newId);
        } else {
            throw new RepositoryException("This repository class is not supported "+ repo.getClass().getName());
        }
    }

    /**
     * Creates a new repository instance with the same settings as this one. The cloned repository is not
     * registered or saved to the configuration.
     *
     * @param repo The origin repository
     * @return The cloned repository.
     */
    public RemoteRepository clone( RemoteRepository repo, String newId) throws RepositoryException
    {
        if (managedRepositories.containsKey(newId) || remoteRepositories.containsKey(newId)) {
            throw new RepositoryException("The given id exists already "+newId);
        }
        RepositoryProvider provider = getProvider(repo.getType());
        RemoteRepositoryConfiguration cfg = provider.getRemoteConfiguration(repo);
        cfg.setId(newId);
        RemoteRepository cloned = provider.createRemoteInstance(cfg);
        cloned.addListener(this);
        return cloned;
    }


    @Override
    public void configurationEvent(ConfigurationEvent event) {

    }


    @Override
    public void addListener(RepositoryEventListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    @Override
    public void removeListener(RepositoryEventListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void clearListeners() {
        this.listeners.clear();
    }

    @Override
    public <T> void raise(RepositoryEvent<T> event) {
        if (event.getType().equals(IndexCreationEvent.Index.URI_CHANGE)) {
            if (managedRepositories.containsKey(event.getRepository().getId()) ||
                    remoteRepositories.containsKey(event.getRepository().getId())) {
                EditableRepository repo = (EditableRepository) event.getRepository();
                if (repo != null && repo.getIndexingContext()!=null) {
                    try {
                        ArchivaIndexManager idxmgr = getIndexManager(repo.getType());
                        if (idxmgr != null) {
                            ArchivaIndexingContext newCtx = idxmgr.move(repo.getIndexingContext(), repo);
                            repo.setIndexingContext(newCtx);
                            idxmgr.updateLocalIndexPath(repo);
                        }

                    } catch (IndexCreationFailedException e) {
                        log.error("Could not move index to new directory {}", e.getMessage(), e);
                    }
                }
            }
        }
        for(RepositoryEventListener listener : listeners) {
            listener.raise(event);
        }
    }
}
