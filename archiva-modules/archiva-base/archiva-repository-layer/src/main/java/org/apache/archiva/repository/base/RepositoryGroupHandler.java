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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.components.registry.RegistryException;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.indexer.merger.MergedRemoteIndexesScheduler;
import org.apache.archiva.repository.EditableRepository;
import org.apache.archiva.repository.EditableRepositoryGroup;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.RepositoryProvider;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.event.RepositoryEvent;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.archiva.indexer.ArchivaIndexManager.DEFAULT_INDEX_PATH;

/**
 * This class manages repository groups for the RepositoryRegistry.
 * It is tightly coupled with the {@link ArchivaRepositoryRegistry}.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service("repositoryGroupHandler#default")
public class RepositoryGroupHandler
{
    private static final Logger log = LoggerFactory.getLogger(RepositoryGroupHandler.class);

    private final ArchivaRepositoryRegistry repositoryRegistry;
    private final ConfigurationHandler configurationHandler;
    private final MergedRemoteIndexesScheduler mergedRemoteIndexesScheduler;

    private Map<String, RepositoryGroup> repositoryGroups = new HashMap<>();

    private Path groupsDirectory;

    /**
     * Creates a new instance. All dependencies are injected on the constructor.
     * @param repositoryRegistry the registry. To avoid circular dependencies via DI, this class registers itself on the registry.
     * @param configurationHandler the configuration handler is used to retrieve and save configuration.
     * @param mergedRemoteIndexesScheduler the index scheduler is used for merging the indexes from all group members
     */
    public RepositoryGroupHandler( ArchivaRepositoryRegistry repositoryRegistry,
                                   ConfigurationHandler configurationHandler,
                                   @Named("mergedRemoteIndexesScheduler#default") MergedRemoteIndexesScheduler mergedRemoteIndexesScheduler) {
        this.configurationHandler = configurationHandler;
        this.mergedRemoteIndexesScheduler = mergedRemoteIndexesScheduler;
        this.repositoryRegistry = repositoryRegistry;
    }

    @PostConstruct
    private void init() {
        log.debug( "Initializing repository group handler " + repositoryRegistry.toString( ) );
        // We are registering this class on the registry. This is necessary to avoid circular dependencies via injection.
        this.repositoryRegistry.registerGroupHandler( this );
        initializeStorage();
    }

    public void initializeFromConfig() {
        this.repositoryGroups.clear();
        this.repositoryGroups.putAll( getRepositoryGroupsFromConfig( ) );
        for (RepositoryGroup group : this.repositoryGroups.values()) {
            initializeGroup( group );
        }
    }

    private void initializeStorage() {
        Path baseDir = this.configurationHandler.getArchivaConfiguration( ).getRepositoryGroupBaseDir( );
        if (!Files.exists( baseDir) ) {
            try
            {
                Files.createDirectories( baseDir );
            }
            catch ( IOException e )
            {
                log.error( "Could not create group base directory: {}", e.getMessage( ), e );
            }
        }
        this.groupsDirectory = baseDir;
    }

    private void initializeGroup(RepositoryGroup repositoryGroup) {
        StorageAsset indexDirectoy = getMergedIndexDirectory( repositoryGroup );
        if (!indexDirectoy.exists()) {
            try
            {
                indexDirectoy.create( );
            }
            catch ( IOException e )
            {
                log.error( "Could not create index directory {} for group {}: {}", indexDirectoy, repositoryGroup.getId( ), e.getMessage( ) );
            }
        }
        Path groupPath = groupsDirectory.resolve(repositoryGroup.getId() );
        if ( !Files.exists(groupPath) )
        {
            try {
                Files.createDirectories(groupPath);
            } catch (IOException e) {
                log.error("Could not create repository group directory {}", groupPath);
            }
        }
        mergedRemoteIndexesScheduler.schedule( repositoryGroup,
            indexDirectoy);
    }

    public StorageAsset getMergedIndexDirectory( RepositoryGroup group )
    {
        if (group!=null) {
            return group.getFeature( IndexCreationFeature.class).get().getLocalIndexPath();
        } else {
            return null;
        }
    }

    public Map<String, RepositoryGroup> getRepositoryGroupsFromConfig() {
        try {
            List<RepositoryGroupConfiguration> repositoryGroupConfigurations =
                this.configurationHandler.getBaseConfiguration().getRepositoryGroups();

            if (repositoryGroupConfigurations == null) {
                return Collections.emptyMap();
            }

            Map<String, RepositoryGroup> repositoryGroupMap = new LinkedHashMap<>(repositoryGroupConfigurations.size());

            Map<RepositoryType, RepositoryProvider> providerMap = repositoryRegistry.getRepositoryProviderMap();
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

    public RepositoryGroup createNewRepositoryGroup(RepositoryProvider provider, RepositoryGroupConfiguration config) throws RepositoryException
    {
        RepositoryGroup repositoryGroup = provider.createRepositoryGroup(config);
        repositoryGroup.registerEventHandler( RepositoryEvent.ANY, repositoryRegistry);
        updateRepositoryReferences(provider, repositoryGroup, config);
        return repositoryGroup;
    }

    public void updateRepositoryReferences( RepositoryProvider provider, RepositoryGroup group, RepositoryGroupConfiguration configuration) {
        if (group instanceof EditableRepositoryGroup ) {
            EditableRepositoryGroup eGroup = (EditableRepositoryGroup) group;
            eGroup.setRepositories(configuration.getRepositories().stream()
                .map(r -> repositoryRegistry.getManagedRepository(r)).collect( Collectors.toList()));
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
    public RepositoryGroup putRepositoryGroup( RepositoryGroup repositoryGroup ) throws RepositoryException {
            final String id = repositoryGroup.getId();
            RepositoryGroup originRepoGroup = repositoryGroups.put(id, repositoryGroup);
            try {
                if (originRepoGroup != null && originRepoGroup != repositoryGroup) {
                    this.mergedRemoteIndexesScheduler.unschedule( originRepoGroup );
                    originRepoGroup.close();
                }
                RepositoryProvider provider = repositoryRegistry.getProvider( repositoryGroup.getType());
                RepositoryGroupConfiguration newCfg = provider.getRepositoryGroupConfiguration(repositoryGroup);
                Configuration configuration = this.configurationHandler.getBaseConfiguration();
                updateRepositoryReferences(provider, repositoryGroup, newCfg);
                RepositoryGroupConfiguration oldCfg = configuration.findRepositoryGroupById(id);
                if (oldCfg != null) {
                    configuration.removeRepositoryGroup(oldCfg);
                }
                configuration.addRepositoryGroup(newCfg);
                repositoryRegistry.saveConfiguration(configuration);
                initializeGroup( repositoryGroup );
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
    }

    /**
     * Adds a new repository group or updates the repository with the same id, if it exists already.
     * The configuration is saved immediately.
     *
     * @param repositoryGroupConfiguration the repository configuration
     * @return the updated or created repository
     * @throws RepositoryException if an error occurs, or the configuration is not valid.
     */
    public RepositoryGroup putRepositoryGroup( RepositoryGroupConfiguration repositoryGroupConfiguration ) throws RepositoryException {
            final String id = repositoryGroupConfiguration.getId();
            final RepositoryType repositoryType = RepositoryType.valueOf(repositoryGroupConfiguration.getType());
            Configuration configuration = this.configurationHandler.getBaseConfiguration();
            RepositoryGroup repositoryGroup = repositoryGroups.get(id);
            RepositoryGroupConfiguration oldCfg = repositoryGroup != null ? repositoryRegistry.getProvider(repositoryType).getRepositoryGroupConfiguration(repositoryGroup) : null;
            repositoryGroup = putRepositoryGroup(repositoryGroupConfiguration, configuration);
            try {
                repositoryRegistry.saveConfiguration(configuration);
            } catch ( IndeterminateConfigurationException | RegistryException e) {
                if (oldCfg != null) {
                    repositoryRegistry.getProvider(repositoryType).updateRepositoryGroupInstance((EditableRepositoryGroup) repositoryGroup, oldCfg);
                }
                log.error("Could not save the configuration for repository group {}: {}", id, e.getMessage(), e);
                throw new RepositoryException("Could not save the configuration for repository group " + id + ": " + e.getMessage());
            }
            return repositoryGroup;
    }

    public RepositoryGroup putRepositoryGroup( RepositoryGroupConfiguration repositoryGroupConfiguration, Configuration configuration ) throws RepositoryException {
            final String id = repositoryGroupConfiguration.getId();
            final RepositoryType repoType = RepositoryType.valueOf(repositoryGroupConfiguration.getType());
            RepositoryGroup repo;
            setRepositoryGroupDefaults(repositoryGroupConfiguration);
            if (repositoryGroups.containsKey(id)) {
                repo = repositoryGroups.get(id);
                this.mergedRemoteIndexesScheduler.unschedule( repo );
                if (repo instanceof EditableRepositoryGroup) {
                    repositoryRegistry.getProvider(repoType).updateRepositoryGroupInstance((EditableRepositoryGroup) repo, repositoryGroupConfiguration);
                } else {
                    throw new RepositoryException("The repository is not editable " + id);
                }
            } else {
                repo = repositoryRegistry.getProvider(repoType).createRepositoryGroup(repositoryGroupConfiguration);
                repositoryGroups.put(id, repo);
            }
            updateRepositoryReferences(repositoryRegistry.getProvider(repoType), repo, repositoryGroupConfiguration);
            replaceOrAddRepositoryConfig(repositoryGroupConfiguration, configuration);
            initializeGroup( repo );
            return repo;
    }

    private void setRepositoryGroupDefaults(RepositoryGroupConfiguration repositoryGroupConfiguration) {
        if ( StringUtils.isEmpty(repositoryGroupConfiguration.getMergedIndexPath())) {
            repositoryGroupConfiguration.setMergedIndexPath(DEFAULT_INDEX_PATH);
        }
        if (repositoryGroupConfiguration.getMergedIndexTtl() <= 0) {
            repositoryGroupConfiguration.setMergedIndexTtl(300);
        }
        if (StringUtils.isEmpty(repositoryGroupConfiguration.getCronExpression())) {
            repositoryGroupConfiguration.setCronExpression("0 0 03 ? * MON");
        }
    }

    private void replaceOrAddRepositoryConfig(RepositoryGroupConfiguration repositoryGroupConfiguration, Configuration configuration) {
        RepositoryGroupConfiguration oldCfg = configuration.findRepositoryGroupById(repositoryGroupConfiguration.getId());
        if (oldCfg != null) {
            configuration.removeRepositoryGroup(oldCfg);
        }
        configuration.addRepositoryGroup(repositoryGroupConfiguration);
    }

    public void removeRepositoryFromGroups( ManagedRepository repo) {
        if (repo != null) {
            repositoryGroups.values().stream().filter(repoGroup -> repoGroup instanceof EditableRepository ).
                map(repoGroup -> (EditableRepositoryGroup) repoGroup).forEach(repoGroup -> repoGroup.removeRepository(repo));
        }
    }

    /**
     * Removes a repository group from the registry and configuration, if it exists.
     * The change is saved to the configuration immediately.
     *
     * @param id the id of the repository group to remove
     * @throws RepositoryException if a error occurs during configuration save
     */
    public void removeRepositoryGroup( final String id ) throws RepositoryException {
        RepositoryGroup repo = getRepositoryGroup(id);
        if (repo != null) {
            try {
                repo = repositoryGroups.remove(id);
                if (repo != null) {
                    this.mergedRemoteIndexesScheduler.unschedule( repo );
                    repo.close();
                    Configuration configuration = this.configurationHandler.getBaseConfiguration();
                    RepositoryGroupConfiguration cfg = configuration.findRepositoryGroupById(id);
                    if (cfg != null) {
                        configuration.removeRepositoryGroup(cfg);
                    }
                    this.configurationHandler.save(configuration, ConfigurationHandler.REGISTRY_EVENT_TAG );
                }

            } catch (RegistryException | IndeterminateConfigurationException e) {
                // Rollback
                log.error("Could not save config after repository removal: {}", e.getMessage(), e);
                repositoryGroups.put(repo.getId(), repo);
                throw new RepositoryException("Could not save configuration after repository removal: " + e.getMessage());
            }
        }
    }

    public void removeRepositoryGroup( String id, Configuration configuration ) throws RepositoryException {
        RepositoryGroup repo = repositoryGroups.get(id);
        if (repo != null) {
                repo = repositoryGroups.remove(id);
                if (repo != null) {
                    this.mergedRemoteIndexesScheduler.unschedule( repo );
                    repo.close();
                    RepositoryGroupConfiguration cfg = configuration.findRepositoryGroupById(id);
                    if (cfg != null) {
                        configuration.removeRepositoryGroup(cfg);
                    }
                }
        }

    }

    public RepositoryGroup getRepositoryGroup( String groupId ) {
        return repositoryGroups.get(groupId);
    }

    public Collection<RepositoryGroup> getRepositoryGroups() {
        return repositoryGroups.values( );
    }

    public boolean hasRepositoryGroup(String id) {
        return repositoryGroups.containsKey( id );
    }

    @PreDestroy
    private void destroy() {
        this.close( );
    }

    public void close() {
        for (RepositoryGroup group : repositoryGroups.values()) {
            try
            {
                mergedRemoteIndexesScheduler.unschedule( group );
                group.close( );
            } catch (Throwable e) {
                log.error( "Could not close repository group {}: {}", group.getId( ), e.getMessage( ) );
            }
        }
        this.repositoryGroups.clear();
    }

}
