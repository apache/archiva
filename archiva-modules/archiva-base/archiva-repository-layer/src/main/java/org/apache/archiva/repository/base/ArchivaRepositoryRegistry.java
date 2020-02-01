package org.apache.archiva.repository.base;

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
import org.apache.archiva.event.Event;
import org.apache.archiva.event.EventManager;
import org.apache.archiva.event.EventType;
import org.apache.archiva.indexer.*;
import org.apache.archiva.components.registry.RegistryException;
import org.apache.archiva.repository.EditableManagedRepository;
import org.apache.archiva.repository.EditableRemoteRepository;
import org.apache.archiva.repository.EditableRepository;
import org.apache.archiva.repository.EditableRepositoryGroup;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.RepositoryProvider;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.UnsupportedRepositoryTypeException;
import org.apache.archiva.repository.event.*;
import org.apache.archiva.event.EventHandler;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.archiva.repository.metadata.MetadataReader;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
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

import static org.apache.archiva.indexer.ArchivaIndexManager.DEFAULT_INDEX_PATH;

/**
 * Registry for repositories. This is the central entry point for repositories. It provides methods for
 * retrieving, adding and removing repositories.
 * <p>
 * The modification methods addXX and removeXX persist the changes immediately to the configuration. If the
 * configuration save fails the changes are rolled back.
 * <p>
 * TODO: Audit events
 *
 * @since 3.0
 */
@Service("repositoryRegistry")
public class ArchivaRepositoryRegistry implements ConfigurationListener, EventHandler<Event>,
    RepositoryRegistry
{

    private static final Logger log = LoggerFactory.getLogger(RepositoryRegistry.class);

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
    List<MetadataReader> metadataReaderList;

    @Inject
    @Named("repositoryContentFactory#default")
    RepositoryContentFactory repositoryContentFactory;

    private final EventManager eventManager;


    private Map<String, ManagedRepository> managedRepositories = new HashMap<>();
    private Map<String, ManagedRepository> uManagedRepository = Collections.unmodifiableMap(managedRepositories);

    private Map<String, RemoteRepository> remoteRepositories = new HashMap<>();
    private Map<String, RemoteRepository> uRemoteRepositories = Collections.unmodifiableMap(remoteRepositories);

    private Map<String, RepositoryGroup> repositoryGroups = new HashMap<>();
    private Map<String, RepositoryGroup> uRepositoryGroups = Collections.unmodifiableMap(repositoryGroups);

    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private volatile boolean ignoreConfigEvents = false;

    public ArchivaRepositoryRegistry() {
        this.eventManager = new EventManager(this);
    }

    @Override
    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration ) {
        this.archivaConfiguration = archivaConfiguration;
    }

    @PostConstruct
    private void initialize() {
        rwLock.writeLock().lock();
        try {
            log.debug("Initializing repository registry");
            updateManagedRepositoriesFromConfig();
            updateRemoteRepositoriesFromConfig();

            repositoryGroups.clear();
            Map<String, RepositoryGroup> repositoryGroups = getRepositorGroupsFromConfig();
            this.repositoryGroups.putAll(repositoryGroups);

            // archivaConfiguration.addChangeListener(this);
            archivaConfiguration.addListener(this);
        } finally {
            rwLock.writeLock().unlock();
        }
        pushEvent(new RepositoryRegistryEvent(RepositoryRegistryEvent.RELOADED, this));
    }

    @PreDestroy
    public void destroy() {
        for (ManagedRepository rep : managedRepositories.values()) {
            rep.close();
        }
        managedRepositories.clear();
        for (RemoteRepository repo : remoteRepositories.values()) {
            repo.close();
        }
        remoteRepositories.clear();
        pushEvent(new RepositoryRegistryEvent(RepositoryRegistryEvent.DESTROYED, this));
    }


    private Map<RepositoryType, RepositoryProvider> createProviderMap() {
        Map<RepositoryType, RepositoryProvider> map = new HashMap<>();
        if (repositoryProviders != null) {
            for (RepositoryProvider provider : repositoryProviders) {
                for (RepositoryType type : provider.provides()) {
                    map.put(type, provider);
                }
            }
        }
        return map;
    }

    private RepositoryProvider getProvider(RepositoryType type) throws RepositoryException
    {
        return repositoryProviders.stream().filter(repositoryProvider -> repositoryProvider.provides().contains(type)).findFirst().orElseThrow(() -> new RepositoryException("Repository type cannot be handled: " + type));
    }

    /*
     * Updates the repositories
     */
    private void updateManagedRepositoriesFromConfig() {
        try {

            Set<String> configRepoIds = new HashSet<>();
            List<ManagedRepositoryConfiguration> managedRepoConfigs =
                    getArchivaConfiguration().getConfiguration().getManagedRepositories();

            if (managedRepoConfigs == null) {
                return;
            }

            for (ManagedRepositoryConfiguration repoConfig : managedRepoConfigs) {
                ManagedRepository repo = putRepository(repoConfig, null);
                configRepoIds.add(repoConfig.getId());
                if (repo.supportsFeature(StagingRepositoryFeature.class)) {
                    StagingRepositoryFeature stagF = repo.getFeature(StagingRepositoryFeature.class).get();
                    if (stagF.getStagingRepository() != null) {
                        configRepoIds.add(stagF.getStagingRepository().getId());
                    }
                }
            }
            List<String> toRemove = managedRepositories.keySet().stream().filter(id -> !configRepoIds.contains(id)).collect(Collectors.toList());
            for (String id : toRemove) {
                ManagedRepository removed = managedRepositories.remove(id);
                removed.close();
            }
        } catch (Throwable e) {
            log.error("Could not initialize repositories from config: {}", e.getMessage(), e);
            return;
        }
    }

    private ManagedRepository createNewManagedRepository(RepositoryProvider provider, ManagedRepositoryConfiguration cfg) throws RepositoryException {
        log.debug("Creating repo {}", cfg.getId());
        ManagedRepository repo = provider.createManagedInstance(cfg);
        repo.registerEventHandler(RepositoryEvent.ANY,  this);
        updateRepositoryReferences(provider, repo, cfg, null);
        return repo;

    }

    private String getStagingId(String repoId) {
        return repoId + StagingRepositoryFeature.STAGING_REPO_POSTFIX;
    }

    @SuppressWarnings("unchecked")
    private void updateRepositoryReferences(RepositoryProvider provider, ManagedRepository repo, ManagedRepositoryConfiguration cfg, Configuration configuration) throws RepositoryException {
        log.debug("Updating references of repo {}", repo.getId());
        if (repo.supportsFeature(StagingRepositoryFeature.class)) {
            StagingRepositoryFeature feature = repo.getFeature(StagingRepositoryFeature.class).get();
            if (feature.isStageRepoNeeded() && feature.getStagingRepository() == null) {
                ManagedRepository stageRepo = getManagedRepository(getStagingId(repo.getId()));
                if (stageRepo == null) {
                    stageRepo = getStagingRepository(provider, cfg, configuration);
                    managedRepositories.put(stageRepo.getId(), stageRepo);
                    if (configuration != null) {
                        replaceOrAddRepositoryConfig(provider.getManagedConfiguration(stageRepo), configuration);
                    }
                    pushEvent(new LifecycleEvent(LifecycleEvent.REGISTERED, this, stageRepo));
                }
                feature.setStagingRepository(stageRepo);
            }
        }
        if (repo instanceof EditableManagedRepository ) {
            EditableManagedRepository editableRepo = (EditableManagedRepository) repo;
            if (repo.getContent() == null) {
                editableRepo.setContent(repositoryContentFactory.getManagedRepositoryContent(repo));
                editableRepo.getContent().setRepository(editableRepo);
            }
            log.debug("Index repo: " + repo.hasIndex());
            if (repo.hasIndex() && ( repo.getIndexingContext() == null || !repo.getIndexingContext().isOpen() )) {
                log.debug("Creating indexing context for {}", repo.getId());
                createIndexingContext(editableRepo);
            }
        }
        repo.registerEventHandler(RepositoryEvent.ANY, this);
    }

    @Override
    public ArchivaIndexManager getIndexManager( RepositoryType type ) {
        return indexManagerFactory.getIndexManager(type);
    }

    @Override
    public MetadataReader getMetadataReader( final RepositoryType type ) throws UnsupportedRepositoryTypeException
    {
        if (metadataReaderList!=null) {
            return metadataReaderList.stream( ).filter( mr -> mr.isValidForType( type ) ).findFirst( ).orElseThrow( ( ) -> new UnsupportedRepositoryTypeException( type ) );
        } else {
            throw new UnsupportedRepositoryTypeException( type );
        }
    }

    private void createIndexingContext( EditableRepository editableRepo) throws RepositoryException {
        if (editableRepo.supportsFeature(IndexCreationFeature.class)) {
            ArchivaIndexManager idxManager = getIndexManager(editableRepo.getType());
            try {
                editableRepo.setIndexingContext(idxManager.createContext(editableRepo));
                idxManager.updateLocalIndexPath(editableRepo);
            } catch (IndexCreationFailedException e) {
                throw new RepositoryException("Could not create index for repository " + editableRepo.getId() + ": " + e.getMessage(), e);
            }
        }
    }

    private ManagedRepository getStagingRepository(RepositoryProvider provider, ManagedRepositoryConfiguration baseRepoCfg, Configuration configuration) throws RepositoryException {
        ManagedRepository stageRepo = getManagedRepository(getStagingId(baseRepoCfg.getId()));
        if (stageRepo == null) {
            stageRepo = provider.createStagingInstance(baseRepoCfg);
            if (stageRepo.supportsFeature(StagingRepositoryFeature.class)) {
                stageRepo.getFeature(StagingRepositoryFeature.class).get().setStageRepoNeeded(false);
            }
            ManagedRepositoryConfiguration stageCfg = provider.getManagedConfiguration(stageRepo);
            updateRepositoryReferences(provider, stageRepo, stageCfg, configuration);
        }
        return stageRepo;
    }


    private void updateRemoteRepositoriesFromConfig() {
        try {
            List<RemoteRepositoryConfiguration> remoteRepoConfigs =
                    getArchivaConfiguration().getConfiguration().getRemoteRepositories();

            if (remoteRepoConfigs == null) {
                return;
            }
            Set<String> repoIds = new HashSet<>();
            for (RemoteRepositoryConfiguration repoConfig : remoteRepoConfigs) {
                putRepository(repoConfig, null);
                repoIds.add(repoConfig.getId());
            }

            List<String> toRemove = remoteRepositories.keySet().stream().filter(id -> !repoIds.contains(id)).collect(Collectors.toList());
            for (String id : toRemove) {
                RemoteRepository removed = remoteRepositories.remove(id);
                removed.close();
            }

        } catch (Throwable e) {
            log.error("Could not initialize remote repositories from config: {}", e.getMessage(), e);
            return;
        }
    }

    private RemoteRepository createNewRemoteRepository(RepositoryProvider provider, RemoteRepositoryConfiguration cfg) throws RepositoryException {
        log.debug("Creating remote repo {}", cfg.getId());
        RemoteRepository repo = provider.createRemoteInstance(cfg);
        updateRepositoryReferences(provider, repo, cfg, null);
        return repo;

    }

    private void updateRepositoryReferences(RepositoryProvider provider, RemoteRepository repo, RemoteRepositoryConfiguration cfg, Configuration configuration) throws RepositoryException {
        if (repo instanceof EditableRemoteRepository && repo.getContent() == null) {
            EditableRemoteRepository editableRepo = (EditableRemoteRepository) repo;
            editableRepo.setContent(repositoryContentFactory.getRemoteRepositoryContent(repo));
            if (repo.supportsFeature(IndexCreationFeature.class) && repo.getIndexingContext() == null) {
                createIndexingContext(editableRepo);
            }
        }
        repo.registerEventHandler(RepositoryEvent.ANY, this);
    }

    private Map<String, RepositoryGroup> getRepositorGroupsFromConfig() {
        try {
            List<RepositoryGroupConfiguration> repositoryGroupConfigurations =
                    getArchivaConfiguration().getConfiguration().getRepositoryGroups();

            if (repositoryGroupConfigurations == null) {
                return Collections.emptyMap();
            }

            Map<String, RepositoryGroup> repositoryGroupMap = new LinkedHashMap<>(repositoryGroupConfigurations.size());

            Map<RepositoryType, RepositoryProvider> providerMap = createProviderMap();
            for (RepositoryGroupConfiguration repoConfig : repositoryGroupConfigurations) {
                RepositoryType repositoryType = RepositoryType.valueOf(repoConfig.getType());
                if (providerMap.containsKey(repositoryType)) {
                    try {
                        RepositoryGroup repo = createNewRepositoryGroup(providerMap.get(repositoryType), repoConfig);
                        repositoryGroupMap.put(repo.getId(), repo);
                    } catch (Exception e) {
                        log.error("Could not create repository group {}: {}", repoConfig.getId(), e.getMessage(), e);
                    }
                }
            }
            return repositoryGroupMap;
        } catch (Throwable e) {
            log.error("Could not initialize repositories from config: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    private RepositoryGroup createNewRepositoryGroup(RepositoryProvider provider, RepositoryGroupConfiguration config) throws RepositoryException {
        RepositoryGroup repositoryGroup = provider.createRepositoryGroup(config);
        repositoryGroup.registerEventHandler(RepositoryEvent.ANY, this);
        updateRepositoryReferences(provider, repositoryGroup, config);
        return repositoryGroup;
    }

    private void updateRepositoryReferences(RepositoryProvider provider, RepositoryGroup group, RepositoryGroupConfiguration configuration) {
        if (group instanceof EditableRepositoryGroup ) {
            EditableRepositoryGroup eGroup = (EditableRepositoryGroup) group;
            eGroup.setRepositories(configuration.getRepositories().stream().map(r -> getManagedRepository(r)).collect(Collectors.toList()));
        }
    }

    private ArchivaConfiguration getArchivaConfiguration() {
        return this.archivaConfiguration;
    }

    /**
     * Returns all repositories that are registered. There is no defined order of the returned repositories.
     *
     * @return a list of managed and remote repositories
     */
    @Override
    public Collection<Repository> getRepositories( ) {
        rwLock.readLock().lock();
        try {
            return Stream.concat(managedRepositories.values().stream(), remoteRepositories.values().stream()).collect(Collectors.toList());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Returns only the managed repositories. There is no defined order of the returned repositories.
     *
     * @return a list of managed repositories
     */
    @Override
    public Collection<ManagedRepository> getManagedRepositories( ) {
        rwLock.readLock().lock();
        try {
            return uManagedRepository.values();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Returns only the remote repositories. There is no defined order of the returned repositories.
     *
     * @return a list of remote repositories
     */
    @Override
    public Collection<RemoteRepository> getRemoteRepositories( ) {
        rwLock.readLock().lock();
        try {
            return uRemoteRepositories.values();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Collection<RepositoryGroup> getRepositoryGroups( ) {
        rwLock.readLock().lock();
        try {
            return uRepositoryGroups.values();
        } finally {
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
    @Override
    public Repository getRepository( String repoId ) {
        rwLock.readLock().lock();
        try {
            log.debug("getRepository {}", repoId);
            if (managedRepositories.containsKey(repoId)) {
                log.debug("Managed repo");
                return managedRepositories.get(repoId);
            } else if (remoteRepositories.containsKey(repoId)) {
                log.debug("Remote repo");
                return remoteRepositories.get(repoId);
            } else if (repositoryGroups.containsKey(repoId)) {
                return repositoryGroups.get(repoId);
            } else {
                return null;
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Convenience method, that returns the managed repository with the given id.
     * It returns null, if no managed repository is registered with this id.
     *
     * @param repoId the repository id
     * @return the managed repository if found, otherwise null
     */
    @Override
    public ManagedRepository getManagedRepository( String repoId ) {
        rwLock.readLock().lock();
        try {
            return managedRepositories.get(repoId);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Convenience method, that returns the remote repository with the given id.
     * It returns null, if no remote repository is registered with this id.
     *
     * @param repoId the repository id
     * @return the remote repository if found, otherwise null
     */
    @Override
    public RemoteRepository getRemoteRepository( String repoId ) {
        rwLock.readLock().lock();
        try {
            return remoteRepositories.get(repoId);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public RepositoryGroup getRepositoryGroup( String groupId ) {
        rwLock.readLock().lock();
        try {
            return repositoryGroups.get(groupId);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /*
     * The <code>ignoreConfigEvents</code> works only for synchronized configuration events.
     * If the configuration throws async events, we cannot know, if the event is caused by this instance or another thread.
     */
    private void saveConfiguration(Configuration configuration) throws IndeterminateConfigurationException, RegistryException {
        ignoreConfigEvents = true;
        try {
            getArchivaConfiguration().save(configuration);
        } finally {
            ignoreConfigEvents = false;
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
    @Override
    public ManagedRepository putRepository( ManagedRepository managedRepository ) throws RepositoryException {
        rwLock.writeLock().lock();
        try {
            final String id = managedRepository.getId();
            if (remoteRepositories.containsKey(id)) {
                throw new RepositoryException("There exists a remote repository with id " + id + ". Could not update with managed repository.");
            }
            ManagedRepository originRepo = managedRepositories.put(id, managedRepository);
            try {
                if (originRepo != null && originRepo != managedRepository) {
                    originRepo.close();
                }
                RepositoryProvider provider = getProvider(managedRepository.getType());
                ManagedRepositoryConfiguration newCfg = provider.getManagedConfiguration(managedRepository);
                Configuration configuration = getArchivaConfiguration().getConfiguration();
                updateRepositoryReferences(provider, managedRepository, newCfg, configuration);
                ManagedRepositoryConfiguration oldCfg = configuration.findManagedRepositoryById(id);
                if (oldCfg != null) {
                    configuration.removeManagedRepository(oldCfg);
                }
                configuration.addManagedRepository(newCfg);
                saveConfiguration(configuration);
                if (originRepo != managedRepository) {
                    pushEvent(new LifecycleEvent(LifecycleEvent.REGISTERED, this, managedRepository));
                } else {
                    pushEvent(new LifecycleEvent(LifecycleEvent.UPDATED, this, managedRepository));
                }
                return managedRepository;
            } catch (Exception e) {
                // Rollback only partly, because repository is closed already
                if (originRepo != null) {
                    managedRepositories.put(id, originRepo);
                } else {
                    managedRepositories.remove(id);
                }
                log.error("Exception during configuration update {}", e.getMessage(), e);
                throw new RepositoryException("Could not save the configuration" + (e.getMessage() == null ? "" : ": " + e.getMessage()));
            }
        } finally {
            rwLock.writeLock().unlock();
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
    @Override
    public ManagedRepository putRepository( ManagedRepositoryConfiguration managedRepositoryConfiguration ) throws RepositoryException {
        rwLock.writeLock().lock();
        try {
            final String id = managedRepositoryConfiguration.getId();
            final RepositoryType repositoryType = RepositoryType.valueOf(managedRepositoryConfiguration.getType());
            Configuration configuration = getArchivaConfiguration().getConfiguration();
            ManagedRepository repo = managedRepositories.get(id);
            ManagedRepositoryConfiguration oldCfg = repo != null ? getProvider(repositoryType).getManagedConfiguration(repo) : null;
            repo = putRepository(managedRepositoryConfiguration, configuration);
            try {
                saveConfiguration(configuration);
            } catch (IndeterminateConfigurationException | RegistryException e) {
                if (oldCfg != null) {
                    getProvider(repositoryType).updateManagedInstance((EditableManagedRepository) repo, oldCfg);
                }
                log.error("Could not save the configuration for repository {}: {}", id, e.getMessage(), e);
                throw new RepositoryException("Could not save the configuration for repository " + id + ": " + e.getMessage());
            }
            return repo;
        } finally {
            rwLock.writeLock().unlock();
        }

    }

    /**
     * Adds a new repository or updates the repository with the same id. The given configuration object is updated, but
     * the configuration is not saved.
     *
     * @param managedRepositoryConfiguration the new or changed managed repository configuration
     * @param configuration                  the configuration object (may be <code>null</code>)
     * @return the new or updated repository
     * @throws RepositoryException if the configuration cannot be saved or updated
     */
    @Override
    public ManagedRepository putRepository( ManagedRepositoryConfiguration managedRepositoryConfiguration, Configuration configuration ) throws RepositoryException {
        rwLock.writeLock().lock();
        try {
            final String id = managedRepositoryConfiguration.getId();
            final RepositoryType repoType = RepositoryType.valueOf(managedRepositoryConfiguration.getType());
            ManagedRepository repo;
            boolean registeredNew = false;
            repo = managedRepositories.get(id);
            if (repo != null && repo.isOpen()) {
                if (repo instanceof EditableManagedRepository) {
                    getProvider(repoType).updateManagedInstance((EditableManagedRepository) repo, managedRepositoryConfiguration);
                } else {
                    throw new RepositoryException("The repository is not editable " + id);
                }
            } else {
                repo = getProvider(repoType).createManagedInstance(managedRepositoryConfiguration);
                managedRepositories.put(id, repo);
                registeredNew = true;
            }
            updateRepositoryReferences(getProvider(repoType), repo, managedRepositoryConfiguration, configuration);
            replaceOrAddRepositoryConfig(managedRepositoryConfiguration, configuration);
            if (registeredNew) {
                pushEvent(new LifecycleEvent(LifecycleEvent.REGISTERED, this, repo));
            } else {
                pushEvent(new LifecycleEvent(LifecycleEvent.UPDATED, this, repo));
            }
            return repo;
        } finally {
            rwLock.writeLock().unlock();
        }
    }


    /**
     * Adds a new repository group to the current list, or replaces the repository group definition with
     * the same id, if it exists already.
     * The change is saved to the configuration immediately.
     *
     * @param repositoryGroup the new repository group.
     * @throws RepositoryException if the new repository group could not be saved to the configuration.
     */
    @Override
    public RepositoryGroup putRepositoryGroup( RepositoryGroup repositoryGroup ) throws RepositoryException {
        rwLock.writeLock().lock();
        try {
            final String id = repositoryGroup.getId();
            RepositoryGroup originRepoGroup = repositoryGroups.put(id, repositoryGroup);
            try {
                if (originRepoGroup != null && originRepoGroup != repositoryGroup) {
                    originRepoGroup.close();
                }
                RepositoryProvider provider = getProvider(repositoryGroup.getType());
                RepositoryGroupConfiguration newCfg = provider.getRepositoryGroupConfiguration(repositoryGroup);
                Configuration configuration = getArchivaConfiguration().getConfiguration();
                updateRepositoryReferences(provider, repositoryGroup, newCfg);
                RepositoryGroupConfiguration oldCfg = configuration.findRepositoryGroupById(id);
                if (oldCfg != null) {
                    configuration.removeRepositoryGroup(oldCfg);
                }
                configuration.addRepositoryGroup(newCfg);
                saveConfiguration(configuration);
                return repositoryGroup;
            } catch (Exception e) {
                // Rollback
                if (originRepoGroup != null) {
                    repositoryGroups.put(id, originRepoGroup);
                } else {
                    repositoryGroups.remove(id);
                }
                log.error("Exception during configuration update {}", e.getMessage(), e);
                throw new RepositoryException("Could not save the configuration" + (e.getMessage() == null ? "" : ": " + e.getMessage()));
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Adds a new repository group or updates the repository with the same id, if it exists already.
     * The configuration is saved immediately.
     *
     * @param repositoryGroupConfiguration the repository configuration
     * @return the updated or created repository
     * @throws RepositoryException if an error occurs, or the configuration is not valid.
     */
    @Override
    public RepositoryGroup putRepositoryGroup( RepositoryGroupConfiguration repositoryGroupConfiguration ) throws RepositoryException {
        rwLock.writeLock().lock();
        try {
            final String id = repositoryGroupConfiguration.getId();
            final RepositoryType repositoryType = RepositoryType.valueOf(repositoryGroupConfiguration.getType());
            Configuration configuration = getArchivaConfiguration().getConfiguration();
            RepositoryGroup repo = repositoryGroups.get(id);
            RepositoryGroupConfiguration oldCfg = repo != null ? getProvider(repositoryType).getRepositoryGroupConfiguration(repo) : null;
            repo = putRepositoryGroup(repositoryGroupConfiguration, configuration);
            try {
                saveConfiguration(configuration);
            } catch (IndeterminateConfigurationException | RegistryException e) {
                if (oldCfg != null) {
                    getProvider(repositoryType).updateRepositoryGroupInstance((EditableRepositoryGroup) repo, oldCfg);
                }
                log.error("Could not save the configuration for repository group {}: {}", id, e.getMessage(), e);
                throw new RepositoryException("Could not save the configuration for repository group " + id + ": " + e.getMessage());
            }
            return repo;
        } finally {
            rwLock.writeLock().unlock();
        }

    }

    /**
     * Adds a new repository group or updates the repository group with the same id. The given configuration object is updated, but
     * the configuration is not saved.
     *
     * @param repositoryGroupConfiguration The configuration of the new or changed repository group.
     * @param configuration                The configuration object. If it is <code>null</code>, the configuration is not saved.
     * @return The new or updated repository group
     * @throws RepositoryException if the configuration cannot be saved or updated
     */
    @Override
    public RepositoryGroup putRepositoryGroup( RepositoryGroupConfiguration repositoryGroupConfiguration, Configuration configuration ) throws RepositoryException {
        rwLock.writeLock().lock();
        try {
            final String id = repositoryGroupConfiguration.getId();
            final RepositoryType repoType = RepositoryType.valueOf(repositoryGroupConfiguration.getType());
            RepositoryGroup repo;
            setRepositoryGroupDefaults(repositoryGroupConfiguration);
            if (repositoryGroups.containsKey(id)) {
                repo = repositoryGroups.get(id);
                if (repo instanceof EditableRepositoryGroup) {
                    getProvider(repoType).updateRepositoryGroupInstance((EditableRepositoryGroup) repo, repositoryGroupConfiguration);
                } else {
                    throw new RepositoryException("The repository is not editable " + id);
                }
            } else {
                repo = getProvider(repoType).createRepositoryGroup(repositoryGroupConfiguration);
                repositoryGroups.put(id, repo);
            }
            updateRepositoryReferences(getProvider(repoType), repo, repositoryGroupConfiguration);
            replaceOrAddRepositoryConfig(repositoryGroupConfiguration, configuration);
            return repo;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void setRepositoryGroupDefaults(RepositoryGroupConfiguration repositoryGroupConfiguration) {
        if (StringUtils.isEmpty(repositoryGroupConfiguration.getMergedIndexPath())) {
            repositoryGroupConfiguration.setMergedIndexPath(DEFAULT_INDEX_PATH);
        }
        if (repositoryGroupConfiguration.getMergedIndexTtl() <= 0) {
            repositoryGroupConfiguration.setMergedIndexTtl(300);
        }
        if (StringUtils.isEmpty(repositoryGroupConfiguration.getCronExpression())) {
            repositoryGroupConfiguration.setCronExpression("0 0 03 ? * MON");
        }
    }

    private void replaceOrAddRepositoryConfig(ManagedRepositoryConfiguration managedRepositoryConfiguration, Configuration configuration) {
        if (configuration != null) {
            ManagedRepositoryConfiguration oldCfg = configuration.findManagedRepositoryById(managedRepositoryConfiguration.getId());
            if (oldCfg != null) {
                configuration.removeManagedRepository(oldCfg);
            }
            configuration.addManagedRepository(managedRepositoryConfiguration);
        }
    }

    private void replaceOrAddRepositoryConfig(RemoteRepositoryConfiguration remoteRepositoryConfiguration, Configuration configuration) {
        if (configuration != null) {
            RemoteRepositoryConfiguration oldCfg = configuration.findRemoteRepositoryById(remoteRepositoryConfiguration.getId());
            if (oldCfg != null) {
                configuration.removeRemoteRepository(oldCfg);
            }
            configuration.addRemoteRepository(remoteRepositoryConfiguration);
        }
    }

    private void replaceOrAddRepositoryConfig(RepositoryGroupConfiguration repositoryGroupConfiguration, Configuration configuration) {
        RepositoryGroupConfiguration oldCfg = configuration.findRepositoryGroupById(repositoryGroupConfiguration.getId());
        if (oldCfg != null) {
            configuration.removeRepositoryGroup(oldCfg);
        }
        configuration.addRepositoryGroup(repositoryGroupConfiguration);
    }

    @Override
    public RemoteRepository putRepository( RemoteRepository remoteRepository, Configuration configuration ) throws RepositoryException {
        rwLock.writeLock().lock();
        try {
            final String id = remoteRepository.getId();
            if (managedRepositories.containsKey(id)) {
                throw new RepositoryException("There exists a managed repository with id " + id + ". Could not update with remote repository.");
            }
            RemoteRepository originRepo = remoteRepositories.put(id, remoteRepository);
            RemoteRepositoryConfiguration oldCfg = null;
            RemoteRepositoryConfiguration newCfg;
            try {
                if (originRepo != null && originRepo != remoteRepository) {
                    originRepo.close();
                }
                final RepositoryProvider provider = getProvider(remoteRepository.getType());
                newCfg = provider.getRemoteConfiguration(remoteRepository);
                updateRepositoryReferences(provider, remoteRepository, newCfg, configuration);
                oldCfg = configuration.findRemoteRepositoryById(id);
                if (oldCfg != null) {
                    configuration.removeRemoteRepository(oldCfg);
                }
                configuration.addRemoteRepository(newCfg);
                if (remoteRepository != originRepo) {
                    pushEvent(new LifecycleEvent(LifecycleEvent.REGISTERED, this, remoteRepository));
                } else {
                    pushEvent(new LifecycleEvent(LifecycleEvent.UPDATED, this, remoteRepository));
                }
                return remoteRepository;
            } catch (Exception e) {
                // Rollback
                if (originRepo != null) {
                    remoteRepositories.put(id, originRepo);
                } else {
                    remoteRepositories.remove(id);
                }
                if (oldCfg != null) {
                    RemoteRepositoryConfiguration cfg = configuration.findRemoteRepositoryById(id);
                    if (cfg != null) {
                        configuration.removeRemoteRepository(cfg);
                        configuration.addRemoteRepository(oldCfg);
                    }
                }
                log.error("Error while adding remote repository {}", e.getMessage(), e);
                throw new RepositoryException("Could not save the configuration" + (e.getMessage() == null ? "" : ": " + e.getMessage()));
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Adds a remote repository, or overwrites the repository definition with the same id, if it exists already.
     * The modification is saved to the configuration immediately.
     *
     * @param remoteRepository the remote repository to add
     * @throws RepositoryException if an error occurs during configuration save
     */
    @Override
    public RemoteRepository putRepository( RemoteRepository remoteRepository ) throws RepositoryException {
        rwLock.writeLock().lock();
        try {
            Configuration configuration = getArchivaConfiguration().getConfiguration();
            try {
                RemoteRepository repo = putRepository(remoteRepository, configuration);
                saveConfiguration(configuration);
                return repo;
            } catch (RegistryException | IndeterminateConfigurationException e) {
                log.error("Error while saving remote repository {}", e.getMessage(), e);
                throw new RepositoryException("Could not save the configuration" + (e.getMessage() == null ? "" : ": " + e.getMessage()));
            }
        } finally {
            rwLock.writeLock().unlock();
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
    @Override
    public RemoteRepository putRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration ) throws RepositoryException {
        rwLock.writeLock().lock();
        try {
            final String id = remoteRepositoryConfiguration.getId();
            final RepositoryType repositoryType = RepositoryType.valueOf(remoteRepositoryConfiguration.getType());
            Configuration configuration = getArchivaConfiguration().getConfiguration();
            RemoteRepository repo = remoteRepositories.get(id);
            RemoteRepositoryConfiguration oldCfg = repo != null ? getProvider(repositoryType).getRemoteConfiguration(repo) : null;
            repo = putRepository(remoteRepositoryConfiguration, configuration);
            try {
                saveConfiguration(configuration);
            } catch (IndeterminateConfigurationException | RegistryException e) {
                if (oldCfg != null) {
                    getProvider(repositoryType).updateRemoteInstance((EditableRemoteRepository) repo, oldCfg);
                }
                log.error("Could not save the configuration for repository {}: {}", id, e.getMessage(), e);
                throw new RepositoryException("Could not save the configuration for repository " + id + ": " + e.getMessage());
            }
            return repo;
        } finally {
            rwLock.writeLock().unlock();
        }

    }

    /**
     * Adds a new repository or updates the repository with the same id. The given configuration object is updated, but
     * the configuration is not saved.
     *
     * @param remoteRepositoryConfiguration the new or changed repository configuration
     * @param configuration                 the configuration object
     * @return the new or updated repository
     * @throws RepositoryException if the configuration cannot be saved or updated
     */
    @Override
    @SuppressWarnings("unchecked")
    public RemoteRepository putRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration, Configuration configuration ) throws RepositoryException {
        rwLock.writeLock().lock();
        try {
            final String id = remoteRepositoryConfiguration.getId();
            final RepositoryType repoType = RepositoryType.valueOf(remoteRepositoryConfiguration.getType());
            RemoteRepository repo;
            boolean registeredNew = false;
            repo = remoteRepositories.get(id);
            if (repo != null && repo.isOpen()) {
                if (repo instanceof EditableRemoteRepository) {
                    getProvider(repoType).updateRemoteInstance((EditableRemoteRepository) repo, remoteRepositoryConfiguration);
                } else {
                    throw new RepositoryException("The repository is not editable " + id);
                }
            } else {
                repo = getProvider(repoType).createRemoteInstance(remoteRepositoryConfiguration);
                remoteRepositories.put(id, repo);
                registeredNew = true;
            }
            updateRepositoryReferences(getProvider(repoType), repo, remoteRepositoryConfiguration, configuration);
            replaceOrAddRepositoryConfig(remoteRepositoryConfiguration, configuration);
            if (registeredNew) {
                pushEvent(new LifecycleEvent(LifecycleEvent.REGISTERED, this, repo));
            } else {
                pushEvent(new LifecycleEvent(LifecycleEvent.UPDATED, this, repo));
            }
            return repo;
        } finally {
            rwLock.writeLock().unlock();
        }


    }

    @Override
    public void removeRepository( String repoId ) throws RepositoryException {
        Repository repo = getRepository(repoId);
        if (repo != null) {
            removeRepository(repo);
        }
    }

    @Override
    public void removeRepository( Repository repo ) throws RepositoryException {
        if (repo == null) {
            log.warn("Trying to remove null repository");
            return;
        }
        if (repo instanceof RemoteRepository) {
            removeRepository((RemoteRepository) repo);
        } else if (repo instanceof ManagedRepository) {
            removeRepository((ManagedRepository) repo);
        } else if (repo instanceof RepositoryGroup) {
            removeRepositoryGroup((RepositoryGroup) repo);
        } else {
            throw new RepositoryException("Repository type not known: " + repo.getClass());
        }
    }

    /**
     * Removes a managed repository from the registry and configuration, if it exists.
     * The change is saved to the configuration immediately.
     *
     * @param managedRepository the managed repository to remove
     * @throws RepositoryException if a error occurs during configuration save
     */
    @Override
    public void removeRepository( ManagedRepository managedRepository ) throws RepositoryException {
        if (managedRepository == null) {
            return;
        }
        final String id = managedRepository.getId();
        ManagedRepository repo = getManagedRepository(id);
        if (repo != null) {
            rwLock.writeLock().lock();
            try {
                repo = managedRepositories.remove(id);
                if (repo != null) {
                    repo.close();
                    removeRepositoryFromGroups(repo);
                    Configuration configuration = getArchivaConfiguration().getConfiguration();
                    ManagedRepositoryConfiguration cfg = configuration.findManagedRepositoryById(id);
                    if (cfg != null) {
                        configuration.removeManagedRepository(cfg);
                    }
                    saveConfiguration(configuration);
                }
                pushEvent(new LifecycleEvent(LifecycleEvent.UNREGISTERED, this, repo));
            } catch (RegistryException | IndeterminateConfigurationException e) {
                // Rollback
                log.error("Could not save config after repository removal: {}", e.getMessage(), e);
                managedRepositories.put(repo.getId(), repo);
                throw new RepositoryException("Could not save configuration after repository removal: " + e.getMessage());
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    private void removeRepositoryFromGroups(ManagedRepository repo) {
        if (repo != null) {
            repositoryGroups.values().stream().filter(repoGroup -> repoGroup instanceof EditableRepository).
                    map(repoGroup -> (EditableRepositoryGroup) repoGroup).forEach(repoGroup -> repoGroup.removeRepository(repo));
        }
    }

    @Override
    public void removeRepository( ManagedRepository managedRepository, Configuration configuration ) throws RepositoryException {
        if (managedRepository == null) {
            return;
        }
        final String id = managedRepository.getId();
        ManagedRepository repo = getManagedRepository(id);
        if (repo != null) {
            rwLock.writeLock().lock();
            try {
                repo = managedRepositories.remove(id);
                if (repo != null) {
                    repo.close();
                    removeRepositoryFromGroups(repo);
                    ManagedRepositoryConfiguration cfg = configuration.findManagedRepositoryById(id);
                    if (cfg != null) {
                        configuration.removeManagedRepository(cfg);
                    }
                }
                pushEvent(new LifecycleEvent(LifecycleEvent.UNREGISTERED, this, repo));
            } finally {
                rwLock.writeLock().unlock();
            }
        }

    }


    /**
     * Removes a repository group from the registry and configuration, if it exists.
     * The change is saved to the configuration immediately.
     *
     * @param repositoryGroup the repository group to remove
     * @throws RepositoryException if a error occurs during configuration save
     */
    @Override
    public void removeRepositoryGroup( RepositoryGroup repositoryGroup ) throws RepositoryException {
        if (repositoryGroup == null) {
            return;
        }
        final String id = repositoryGroup.getId();
        RepositoryGroup repo = getRepositoryGroup(id);
        if (repo != null) {
            rwLock.writeLock().lock();
            try {
                repo = repositoryGroups.remove(id);
                if (repo != null) {
                    repo.close();
                    Configuration configuration = getArchivaConfiguration().getConfiguration();
                    RepositoryGroupConfiguration cfg = configuration.findRepositoryGroupById(id);
                    if (cfg != null) {
                        configuration.removeRepositoryGroup(cfg);
                    }
                    saveConfiguration(configuration);
                }

            } catch (RegistryException | IndeterminateConfigurationException e) {
                // Rollback
                log.error("Could not save config after repository removal: {}", e.getMessage(), e);
                repositoryGroups.put(repo.getId(), repo);
                throw new RepositoryException("Could not save configuration after repository removal: " + e.getMessage());
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    @Override
    public void removeRepositoryGroup( RepositoryGroup repositoryGroup, Configuration configuration ) throws RepositoryException {
        if (repositoryGroup == null) {
            return;
        }
        final String id = repositoryGroup.getId();
        RepositoryGroup repo = getRepositoryGroup(id);
        if (repo != null) {
            rwLock.writeLock().lock();
            try {
                repo = repositoryGroups.remove(id);
                if (repo != null) {
                    repo.close();
                    RepositoryGroupConfiguration cfg = configuration.findRepositoryGroupById(id);
                    if (cfg != null) {
                        configuration.removeRepositoryGroup(cfg);
                    }
                }
            } finally {
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
    @Override
    public void removeRepository( RemoteRepository remoteRepository ) throws RepositoryException {
        if (remoteRepository == null) {
            return;
        }
        final String id = remoteRepository.getId();
        RemoteRepository repo = getRemoteRepository(id);
        if (repo != null) {
            rwLock.writeLock().lock();
            try {
                repo = remoteRepositories.remove(id);
                if (repo != null) {
                    Configuration configuration = getArchivaConfiguration().getConfiguration();
                    doRemoveRepo(repo, configuration);
                    saveConfiguration(configuration);
                }
                pushEvent(new LifecycleEvent(LifecycleEvent.UNREGISTERED, this, repo));
            } catch (RegistryException | IndeterminateConfigurationException e) {
                // Rollback
                log.error("Could not save config after repository removal: {}", e.getMessage(), e);
                remoteRepositories.put(repo.getId(), repo);
                throw new RepositoryException("Could not save configuration after repository removal: " + e.getMessage());
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    @Override
    public void removeRepository( RemoteRepository remoteRepository, Configuration configuration ) throws RepositoryException {
        if (remoteRepository == null) {
            return;
        }
        final String id = remoteRepository.getId();
        RemoteRepository repo = getRemoteRepository(id);
        if (repo != null) {
            rwLock.writeLock().lock();
            try {
                repo = remoteRepositories.remove(id);
                if (repo != null) {
                    doRemoveRepo(repo, configuration);
                }
                pushEvent(new LifecycleEvent(LifecycleEvent.UNREGISTERED, this, repo));
            } finally {
                rwLock.writeLock().unlock();
            }
        }

    }

    /**
     * Reloads the registry from the configuration.
     */
    @Override
    public void reload( ) {
        initialize();
    }

    /**
     * Resets the indexing context of a given repository.
     *
     * @param repository The repository
     * @throws IndexUpdateFailedException If the index could not be resetted.
     */
    @Override
    public void resetIndexingContext( Repository repository ) throws IndexUpdateFailedException {
        if (repository.hasIndex() && repository instanceof EditableRepository) {
            EditableRepository eRepo = (EditableRepository) repository;
            ArchivaIndexingContext newCtx = getIndexManager(repository.getType()).reset(repository.getIndexingContext());
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
    @Override
    public ManagedRepository clone( ManagedRepository repo, String newId ) throws RepositoryException {
        if (managedRepositories.containsKey(newId) || remoteRepositories.containsKey(newId)) {
            throw new RepositoryException("The given id exists already " + newId);
        }
        RepositoryProvider provider = getProvider(repo.getType());
        ManagedRepositoryConfiguration cfg = provider.getManagedConfiguration(repo);
        cfg.setId(newId);
        ManagedRepository cloned = provider.createManagedInstance(cfg);
        cloned.registerEventHandler(RepositoryEvent.ANY, this);
        return cloned;
    }

    @Override
    public <T extends Repository> Repository clone( T repo, String newId ) throws RepositoryException {
        if (repo instanceof RemoteRepository) {
            return this.clone((RemoteRepository) repo, newId);
        } else if (repo instanceof ManagedRepository) {
            return this.clone((ManagedRepository) repo, newId);
        } else {
            throw new RepositoryException("This repository class is not supported " + repo.getClass().getName());
        }
    }

    /**
     * Creates a new repository instance with the same settings as this one. The cloned repository is not
     * registered or saved to the configuration.
     *
     * @param repo The origin repository
     * @return The cloned repository.
     */
    @Override
    public RemoteRepository clone( RemoteRepository repo, String newId ) throws RepositoryException {
        if (managedRepositories.containsKey(newId) || remoteRepositories.containsKey(newId)) {
            throw new RepositoryException("The given id exists already " + newId);
        }
        RepositoryProvider provider = getProvider(repo.getType());
        RemoteRepositoryConfiguration cfg = provider.getRemoteConfiguration(repo);
        cfg.setId(newId);
        RemoteRepository cloned = provider.createRemoteInstance(cfg);
        cloned.registerEventHandler(RepositoryEvent.ANY, this);
        return cloned;
    }

    @Override
    public Repository getRepositoryOfAsset( StorageAsset asset )
    {
        if (asset instanceof Repository) {
            return (Repository)asset;
        } else
        {
            return getRepositories( ).stream( ).filter( r -> r.getAsset( "" )
                .getStorage( ).equals( asset.getStorage( ) ) ).findFirst( ).orElse( null );
        }
    }


    @Override
    public void configurationEvent(ConfigurationEvent event) {
        // Note: the ignoreConfigEvents flag does not work, if the config events are asynchronous.
        if (!ignoreConfigEvents) {
            reload();
        }
    }


    @Override
    public <T extends Event> void registerEventHandler( EventType<T> type, EventHandler<? super T> eventHandler) {
        eventManager.registerEventHandler(type, eventHandler);
    }


    @Override
    public <T extends Event> void unregisterEventHandler(EventType<T> type, EventHandler<? super T> eventHandler) {
        eventManager.unregisterEventHandler(type, eventHandler);
    }


    @Override
    public void handle(Event event) {
        // To avoid event cycles:
        if (sameOriginator(event)) {
            return;
        }
        if (event instanceof RepositoryIndexEvent) {
            handleIndexCreationEvent((RepositoryIndexEvent) event);
        }
        // We propagate all events to our listeners, but with context of repository registry
        pushEvent(event);
    }

    private void handleIndexCreationEvent(RepositoryIndexEvent event) {
        RepositoryIndexEvent idxEvent = event;
        if (managedRepositories.containsKey(idxEvent.getRepository().getId()) ||
                remoteRepositories.containsKey(idxEvent.getRepository().getId())) {
            EditableRepository repo = (EditableRepository) idxEvent.getRepository();
            if (repo != null && repo.getIndexingContext() != null) {
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

    private boolean sameOriginator(Event event) {
        if (event.getSource() == this) {
            return true;
        } else if (event.hasPreviousEvent()) {
            return sameOriginator(event.getPreviousEvent());
        } else {
            return false;
        }
    }

    private void pushEvent(Event event) {
        eventManager.fireEvent(event);
    }



}
