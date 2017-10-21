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
import org.apache.archiva.redback.components.registry.RegistryException;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
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
 */
@Service( "repositoryRegistry" )
public class RepositoryRegistry implements ConfigurationListener {

    private static final Logger log = LoggerFactory.getLogger( RepositoryRegistry.class );

    /**
     * We inject all repository providers
     */
    @Inject
    List<RepositoryProvider> repositoryProviders;

    @Inject
    ArchivaConfiguration archivaConfiguration;

    @Inject
    RepositoryContentFactory repositoryContentFactory;

    private Map<String, ManagedRepository> managedRepositories = new HashMap<>( );
    private Map<String, ManagedRepository> uManagedRepository = Collections.unmodifiableMap( managedRepositories );

    private Map<String, RemoteRepository> remoteRepositories = new HashMap<>( );
    private Map<String, RemoteRepository> uRemoteRepositories = Collections.unmodifiableMap( remoteRepositories );

    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock( );

    @PostConstruct
    private void initialize( )
    {
        rwLock.writeLock( ).lock( );
        try
        {
            managedRepositories.clear( );
            managedRepositories.putAll( getManagedRepositoriesFromConfig( ) );
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
            return Collections.EMPTY_MAP;
        }
    }

    private ManagedRepository createNewManagedRepository( RepositoryProvider provider, ManagedRepositoryConfiguration cfg ) throws RepositoryException
    {
        ManagedRepository repo = provider.createManagedInstance( cfg );
        if ( repo.supportsFeature( StagingRepositoryFeature.class ) )
        {
            StagingRepositoryFeature feature = repo.getFeature( StagingRepositoryFeature.class ).get( );
            if ( feature.isStageRepoNeeded( ) )
            {
                ManagedRepository stageRepo = getStagingRepository( provider, cfg );
                feature.setStagingRepository( stageRepo );
            }
        }
        if ( repo instanceof EditableManagedRepository )
        {
            ( (EditableManagedRepository) repo ).setContent( repositoryContentFactory.getManagedRepositoryContent( repo.getId( ) ) );
        }
        return repo;

    }

    private ManagedRepository getStagingRepository(RepositoryProvider provider, ManagedRepositoryConfiguration baseRepoCfg ) throws RepositoryException
    {
        ManagedRepository stageRepo = getManagedRepository( baseRepoCfg.getId( ) + StagingRepositoryFeature.STAGING_REPO_POSTFIX );
        if ( stageRepo == null )
        {
            stageRepo = provider.createStagingInstance( baseRepoCfg );
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
                return Collections.EMPTY_MAP;
            }

            Map<String, RemoteRepository> remoteRepos = new LinkedHashMap<>( remoteRepoConfigs.size( ) );

            Map<RepositoryType, RepositoryProvider> providerMap = createProviderMap( );
            for ( RemoteRepositoryConfiguration repoConfig : remoteRepoConfigs )
            {
                RepositoryType repositoryType = RepositoryType.valueOf( repoConfig.getType( ) );
                if ( providerMap.containsKey( repositoryType ) )
                {
                    try
                    {
                        remoteRepos.put( repoConfig.getId( ), providerMap.get( repositoryType ).createRemoteInstance( repoConfig ) );
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
            return Collections.EMPTY_MAP;
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
            if ( managedRepositories.containsKey( repoId ) )
            {
                return managedRepositories.get( repoId );
            }
            else
            {
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
     * Adds a new repository to the current list, or overwrites the repository definition with
     * the same id, if it exists already.
     * The change is saved to the configuration immediately.
     *
     * @param managedRepository the new repository.
     * @throws RepositoryException if the new repository could not be saved to the configuration.
     */
    public void addRepository( ManagedRepository managedRepository ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = managedRepository.getId();
            ManagedRepository originRepo = managedRepositories.put( id, managedRepository );
            ManagedRepositoryConfiguration originCfg = null;
            List<ManagedRepositoryConfiguration> cfgList = null;
            int index = 0;
            try
            {
                ManagedRepositoryConfiguration newCfg = getProvider( managedRepository.getType( ) ).getManagedConfiguration( managedRepository );
                Configuration configuration = getArchivaConfiguration( ).getConfiguration( );
                ManagedRepositoryConfiguration oldCfg = configuration.findManagedRepositoryById( id );
                if (oldCfg!=null) {
                    configuration.removeManagedRepository( oldCfg );
                }
                configuration.addManagedRepository( newCfg );
                getArchivaConfiguration( ).save( configuration );
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
                throw new RepositoryException( "Could not save the configuration: " + e.getMessage( ) );
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
    public void addRepository( RemoteRepository remoteRepository ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = remoteRepository.getId();
            RemoteRepository originRepo = remoteRepositories.put( id, remoteRepository );
            RemoteRepositoryConfiguration originCfg = null;
            List<RemoteRepositoryConfiguration> cfgList = null;
            int index = 0;
            try
            {
                RemoteRepositoryConfiguration newCfg = getProvider( remoteRepository.getType( ) ).getRemoteConfiguration( remoteRepository );
                Configuration configuration = getArchivaConfiguration( ).getConfiguration( );
                RemoteRepositoryConfiguration oldCfg = configuration.findRemoteRepositoryById( id );
                if (oldCfg!=null) {
                    configuration.removeRemoteRepository( oldCfg );
                }
                configuration.addRemoteRepository( newCfg );
                getArchivaConfiguration( ).save( configuration );
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
                throw new RepositoryException( "Could not save the configuration: " + e.getMessage( ) );
            }
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
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
                if (repo!=null) {
                    managedRepositories.put(repo.getId(), repo);
                }
                throw new RepositoryException( "Could not save configuration after repository removal: "+e.getMessage() );
            }
            finally
            {
                rwLock.writeLock().unlock();
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
                    RemoteRepositoryConfiguration cfg = configuration.findRemoteRepositoryById( id );
                    if (cfg!=null) {
                        configuration.removeRemoteRepository( cfg );
                    }
                    getArchivaConfiguration().save( configuration );
                }

            }
            catch ( RegistryException | IndeterminateConfigurationException e )
            {
                // Rollback
                log.error("Could not save config after repository removal: {}", e.getMessage(), e);
                if (repo!=null) {
                    remoteRepositories.put(repo.getId(), repo);
                }
                throw new RepositoryException( "Could not save configuration after repository removal: "+e.getMessage() );
            }
            finally
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
        return cloned;
    }

    public <T extends Repository> Repository clone(T repo, String newId) throws RepositoryException {
        if (repo instanceof RemoteRepository) {
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
    public RemoteRepository clone(RemoteRepository repo, String newId) throws RepositoryException
    {
        if (managedRepositories.containsKey(newId) || remoteRepositories.containsKey(newId)) {
            throw new RepositoryException("The given id exists already "+newId);
        }
        RepositoryProvider provider = getProvider(repo.getType());
        RemoteRepositoryConfiguration cfg = provider.getRemoteConfiguration(repo);
        cfg.setId(newId);
        RemoteRepository cloned = provider.createRemoteInstance(cfg);
        return cloned;
    }

    public EditableManagedRepository createNewManaged(RepositoryType type, String id, String name) throws RepositoryException {
        return getProvider(type).createManagedInstance(id, name);
    }

    public EditableRemoteRepository createNewRemote(RepositoryType type, String id, String name) throws RepositoryException {
        return getProvider(type).createRemoteInstance(id, name);
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {

    }
}
