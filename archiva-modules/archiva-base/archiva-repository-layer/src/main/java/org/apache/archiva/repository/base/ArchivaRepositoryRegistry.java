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

import org.apache.archiva.components.registry.RegistryException;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ConfigurationEvent;
import org.apache.archiva.configuration.ConfigurationListener;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.event.Event;
import org.apache.archiva.event.EventHandler;
import org.apache.archiva.event.EventManager;
import org.apache.archiva.event.EventType;
import org.apache.archiva.indexer.ArchivaIndexManager;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.indexer.IndexCreationFailedException;
import org.apache.archiva.indexer.IndexManagerFactory;
import org.apache.archiva.indexer.IndexUpdateFailedException;
import org.apache.archiva.repository.CheckedResult;
import org.apache.archiva.repository.EditableManagedRepository;
import org.apache.archiva.repository.EditableRemoteRepository;
import org.apache.archiva.repository.EditableRepository;
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
import org.apache.archiva.repository.base.validation.CommonGroupValidator;
import org.apache.archiva.repository.event.LifecycleEvent;
import org.apache.archiva.repository.event.RepositoryEvent;
import org.apache.archiva.repository.event.RepositoryIndexEvent;
import org.apache.archiva.repository.event.RepositoryRegistryEvent;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.archiva.repository.metadata.MetadataReader;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.validation.RepositoryChecker;
import org.apache.archiva.repository.validation.RepositoryValidator;
import org.apache.archiva.repository.validation.ValidationError;
import org.apache.archiva.repository.validation.ValidationResponse;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
@Service( "repositoryRegistry" )
public class ArchivaRepositoryRegistry implements ConfigurationListener, EventHandler<Event>,
    RepositoryRegistry
{

    private static final Logger log = LoggerFactory.getLogger( RepositoryRegistry.class );

    /**
     * We inject all repository providers
     */
    @Inject
    List<RepositoryProvider> repositoryProviders;

    @Inject
    IndexManagerFactory indexManagerFactory;

    @Inject
    List<MetadataReader> metadataReaderList;

    @Inject
    @Named( "repositoryContentFactory#default" )
    RepositoryContentFactory repositoryContentFactory;


    private final EventManager eventManager;


    private Map<String, ManagedRepository> managedRepositories = new HashMap<>( );
    private Map<String, ManagedRepository> uManagedRepository = Collections.unmodifiableMap( managedRepositories );

    private Map<String, RemoteRepository> remoteRepositories = new HashMap<>( );
    private Map<String, RemoteRepository> uRemoteRepositories = Collections.unmodifiableMap( remoteRepositories );

    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock( );

    private RepositoryGroupHandler groupHandler;
    private final Set<RepositoryValidator<? extends Repository>> validators;
    private final ConfigurationHandler configurationHandler;


    private AtomicBoolean groups_initalized = new AtomicBoolean( false );
    private AtomicBoolean managed_initialized = new AtomicBoolean( false );
    private AtomicBoolean remote_initialized = new AtomicBoolean( false );


    public ArchivaRepositoryRegistry( ConfigurationHandler configurationHandler, List<RepositoryValidator<? extends Repository>> validatorList )
    {
        this.eventManager = new EventManager( this );
        this.configurationHandler = configurationHandler;
        this.validators = initValidatorList( validatorList );
    }


    private Set<RepositoryValidator<? extends Repository>> initValidatorList( List<RepositoryValidator<? extends Repository>> validators )
    {
        TreeSet<RepositoryValidator<? extends Repository>> val = new TreeSet<>( );
        for (RepositoryValidator<? extends Repository> validator : validators) {
            val.add( validator );
        }
        return val;
    }

    @Override
    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.configurationHandler.setArchivaConfiguration( archivaConfiguration );
    }

    @PostConstruct
    private void initialize( )
    {
        rwLock.writeLock( ).lock( );
        try
        {
            log.debug( "Initializing repository registry" );
            updateManagedRepositoriesFromConfig( );
            pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.MANAGED_REPOS_INITIALIZED, this ) );
            managed_initialized.set( true );
            updateRemoteRepositoriesFromConfig( );
            pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.REMOTE_REPOS_INITIALIZED, this ) );
            remote_initialized.set( true );

            initializeRepositoryGroups( );

            for ( RepositoryProvider provider : repositoryProviders )
            {
                provider.addRepositoryEventHandler( this );
            }
            this.configurationHandler.addListener( this );
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
        pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.RELOADED, this ) );
        if ( managed_initialized.get( ) && remote_initialized.get( ) && groups_initalized.get( ) )
        {
            pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.INITIALIZED, this ) );
        }
    }

    private void initializeRepositoryGroups( )
    {
        if ( this.groupHandler != null )
        {
            this.groupHandler.initializeFromConfig( );
            this.groups_initalized.set( true );
            pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.GROUPS_INITIALIZED, this ) );
        }
    }

    public void registerGroupHandler( RepositoryGroupHandler groupHandler )
    {
        this.groupHandler = groupHandler;
        initializeRepositoryGroups( );
        if ( managed_initialized.get( ) && remote_initialized.get( ) && groups_initalized.get( ) )
        {
            pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.INITIALIZED, this ) );
        }
    }


    @PreDestroy
    public void destroy( )
    {
        for ( ManagedRepository rep : managedRepositories.values( ) )
        {
            rep.close( );
        }
        managedRepositories.clear( );
        for ( RemoteRepository repo : remoteRepositories.values( ) )
        {
            repo.close( );
        }
        remoteRepositories.clear( );
        groupHandler.close( );
        pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.DESTROYED, this ) );
    }


    protected Map<RepositoryType, RepositoryProvider> getRepositoryProviderMap( )
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

    protected RepositoryProvider getProvider( RepositoryType type ) throws RepositoryException
    {
        return repositoryProviders.stream( ).filter( repositoryProvider -> repositoryProvider.provides( ).contains( type ) ).findFirst( ).orElseThrow( ( ) -> new RepositoryException( "Repository type cannot be handled: " + type ) );
    }

    /*
     * Updates the repositories
     */
    private void updateManagedRepositoriesFromConfig( )
    {
        try
        {

            Set<String> configRepoIds = new HashSet<>( );
            List<ManagedRepositoryConfiguration> managedRepoConfigs =
                configurationHandler.getBaseConfiguration( ).getManagedRepositories( );

            if ( managedRepoConfigs == null )
            {
                return;
            }

            for ( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
            {
                ManagedRepository repo = putRepository( repoConfig, null );
                configRepoIds.add( repoConfig.getId( ) );
                if ( repo.supportsFeature( StagingRepositoryFeature.class ) )
                {
                    StagingRepositoryFeature stagF = repo.getFeature( StagingRepositoryFeature.class ).get( );
                    if ( stagF.getStagingRepository( ) != null )
                    {
                        configRepoIds.add( stagF.getStagingRepository( ).getId( ) );
                    }
                }
            }
            List<String> toRemove = managedRepositories.keySet( ).stream( ).filter( id -> !configRepoIds.contains( id ) ).collect( Collectors.toList( ) );
            for ( String id : toRemove )
            {
                ManagedRepository removed = managedRepositories.remove( id );
                removed.close( );
            }
        }
        catch ( Throwable e )
        {
            log.error( "Could not initialize repositories from config: {}", e.getMessage( ), e );
            return;
        }
    }

    private ManagedRepository createNewManagedRepository( RepositoryProvider provider, ManagedRepositoryConfiguration cfg ) throws RepositoryException
    {
        log.debug( "Creating repo {}", cfg.getId( ) );
        ManagedRepository repo = provider.createManagedInstance( cfg );
        repo.registerEventHandler( RepositoryEvent.ANY, this );
        updateRepositoryReferences( provider, repo, cfg, null );
        return repo;

    }

    private String getStagingId( String repoId )
    {
        return repoId + StagingRepositoryFeature.STAGING_REPO_POSTFIX;
    }

    @SuppressWarnings( "unchecked" )
    private void updateRepositoryReferences( RepositoryProvider provider, ManagedRepository repo, ManagedRepositoryConfiguration cfg, Configuration configuration ) throws RepositoryException
    {
        log.debug( "Updating references of repo {}", repo.getId( ) );
        if ( repo.supportsFeature( StagingRepositoryFeature.class ) )
        {
            StagingRepositoryFeature feature = repo.getFeature( StagingRepositoryFeature.class ).get( );
            if ( feature.isStageRepoNeeded( ) && feature.getStagingRepository( ) == null )
            {
                ManagedRepository stageRepo = getManagedRepository( getStagingId( repo.getId( ) ) );
                if ( stageRepo == null )
                {
                    stageRepo = getStagingRepository( provider, cfg, configuration );
                    managedRepositories.put( stageRepo.getId( ), stageRepo );
                    if ( configuration != null )
                    {
                        replaceOrAddRepositoryConfig( provider.getManagedConfiguration( stageRepo ), configuration );
                    }
                    pushEvent( new LifecycleEvent( LifecycleEvent.REGISTERED, this, stageRepo ) );
                }
                feature.setStagingRepository( stageRepo );
            }
        }
        if ( repo instanceof EditableManagedRepository )
        {
            EditableManagedRepository editableRepo = (EditableManagedRepository) repo;
            if ( repo.getContent( ) == null )
            {
                editableRepo.setContent( repositoryContentFactory.getManagedRepositoryContent( repo ) );
                editableRepo.getContent( ).setRepository( editableRepo );
            }
            log.debug( "Index repo: " + repo.hasIndex( ) );
            if ( repo.hasIndex( ) && ( repo.getIndexingContext( ) == null || !repo.getIndexingContext( ).isOpen( ) ) )
            {
                log.debug( "Creating indexing context for {}", repo.getId( ) );
                createIndexingContext( editableRepo );
            }
        }
        repo.registerEventHandler( RepositoryEvent.ANY, this );
    }

    @Override
    public ArchivaIndexManager getIndexManager( RepositoryType type )
    {
        return indexManagerFactory.getIndexManager( type );
    }

    @Override
    public MetadataReader getMetadataReader( final RepositoryType type ) throws UnsupportedRepositoryTypeException
    {
        if ( metadataReaderList != null )
        {
            return metadataReaderList.stream( ).filter( mr -> mr.isValidForType( type ) ).findFirst( ).orElseThrow( ( ) -> new UnsupportedRepositoryTypeException( type ) );
        }
        else
        {
            throw new UnsupportedRepositoryTypeException( type );
        }
    }

    private void createIndexingContext( EditableRepository editableRepo ) throws RepositoryException
    {
        if ( editableRepo.supportsFeature( IndexCreationFeature.class ) )
        {
            ArchivaIndexManager idxManager = getIndexManager( editableRepo.getType( ) );
            try
            {
                editableRepo.setIndexingContext( idxManager.createContext( editableRepo ) );
                idxManager.updateLocalIndexPath( editableRepo );
            }
            catch ( IndexCreationFailedException e )
            {
                throw new RepositoryException( "Could not create index for repository " + editableRepo.getId( ) + ": " + e.getMessage( ), e );
            }
        }
    }

    private ManagedRepository getStagingRepository( RepositoryProvider provider, ManagedRepositoryConfiguration baseRepoCfg, Configuration configuration ) throws RepositoryException
    {
        ManagedRepository stageRepo = getManagedRepository( getStagingId( baseRepoCfg.getId( ) ) );
        if ( stageRepo == null )
        {
            stageRepo = provider.createStagingInstance( baseRepoCfg );
            if ( stageRepo.supportsFeature( StagingRepositoryFeature.class ) )
            {
                stageRepo.getFeature( StagingRepositoryFeature.class ).get( ).setStageRepoNeeded( false );
            }
            ManagedRepositoryConfiguration stageCfg = provider.getManagedConfiguration( stageRepo );
            updateRepositoryReferences( provider, stageRepo, stageCfg, configuration );
        }
        return stageRepo;
    }


    private void updateRemoteRepositoriesFromConfig( )
    {
        try
        {
            List<RemoteRepositoryConfiguration> remoteRepoConfigs =
                configurationHandler.getBaseConfiguration( ).getRemoteRepositories( );

            if ( remoteRepoConfigs == null )
            {
                return;
            }
            Set<String> repoIds = new HashSet<>( );
            for ( RemoteRepositoryConfiguration repoConfig : remoteRepoConfigs )
            {
                putRepository( repoConfig, null );
                repoIds.add( repoConfig.getId( ) );
            }

            List<String> toRemove = remoteRepositories.keySet( ).stream( ).filter( id -> !repoIds.contains( id ) ).collect( Collectors.toList( ) );
            for ( String id : toRemove )
            {
                RemoteRepository removed = remoteRepositories.remove( id );
                removed.close( );
            }

        }
        catch ( Throwable e )
        {
            log.error( "Could not initialize remote repositories from config: {}", e.getMessage( ), e );
            return;
        }
    }

    private RemoteRepository createNewRemoteRepository( RepositoryProvider provider, RemoteRepositoryConfiguration cfg ) throws RepositoryException
    {
        log.debug( "Creating remote repo {}", cfg.getId( ) );
        RemoteRepository repo = provider.createRemoteInstance( cfg );
        updateRepositoryReferences( provider, repo, cfg, null );
        return repo;

    }

    private void updateRepositoryReferences( RepositoryProvider provider, RemoteRepository repo, RemoteRepositoryConfiguration cfg, Configuration configuration ) throws RepositoryException
    {
        if ( repo instanceof EditableRemoteRepository && repo.getContent( ) == null )
        {
            EditableRemoteRepository editableRepo = (EditableRemoteRepository) repo;
            editableRepo.setContent( repositoryContentFactory.getRemoteRepositoryContent( repo ) );
            if ( repo.supportsFeature( IndexCreationFeature.class ) && repo.getIndexingContext( ) == null )
            {
                createIndexingContext( editableRepo );
            }
        }
        repo.registerEventHandler( RepositoryEvent.ANY, this );
    }


    /**
     * Returns all repositories that are registered. There is no defined order of the returned repositories.
     *
     * @return a list of managed and remote repositories
     */
    @Override
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
    @Override
    public Collection<ManagedRepository> getManagedRepositories( )
    {
        rwLock.readLock( ).lock( );
        try
        {
            return uManagedRepository.values( );
        }
        finally
        {
            rwLock.readLock( ).unlock( );
        }
    }

    /**
     * Returns only the remote repositories. There is no defined order of the returned repositories.
     *
     * @return a list of remote repositories
     */
    @Override
    public Collection<RemoteRepository> getRemoteRepositories( )
    {
        rwLock.readLock( ).lock( );
        try
        {
            return uRemoteRepositories.values( );
        }
        finally
        {
            rwLock.readLock( ).unlock( );
        }
    }

    @Override
    public Collection<RepositoryGroup> getRepositoryGroups( )
    {
        rwLock.readLock( ).lock( );
        try
        {
            return groupHandler.getAll( );
        }
        finally
        {
            rwLock.readLock( ).unlock( );
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
    public Repository getRepository( String repoId )
    {
        rwLock.readLock( ).lock( );
        try
        {
            log.debug( "getRepository {}", repoId );
            if ( managedRepositories.containsKey( repoId ) )
            {
                log.debug( "Managed repo" );
                return managedRepositories.get( repoId );
            }
            else if ( remoteRepositories.containsKey( repoId ) )
            {
                log.debug( "Remote repo" );
                return remoteRepositories.get( repoId );
            }
            else if ( groupHandler.has( repoId ) )
            {
                return groupHandler.get( repoId );
            }
            else
            {
                return null;
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
    @Override
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
    @Override
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

    @Override
    public RepositoryGroup getRepositoryGroup( String groupId )
    {
        rwLock.readLock( ).lock( );
        try
        {
            return groupHandler.get( groupId );
        }
        finally
        {
            rwLock.readLock( ).unlock( );
        }
    }

    @Override
    public boolean hasRepository( String repoId )
    {
        return this.managedRepositories.containsKey( repoId ) || this.remoteRepositories.containsKey( repoId ) || groupHandler.has( repoId );
    }

    @Override
    public boolean hasManagedRepository( String repoId )
    {
        return this.managedRepositories.containsKey( repoId );
    }

    @Override
    public boolean hasRemoteRepository( String repoId )
    {
        return this.remoteRepositories.containsKey( repoId );
    }

    @Override
    public boolean hasRepositoryGroup( String groupId )
    {
        return groupHandler.has( groupId );
    }

    protected void saveConfiguration( Configuration configuration ) throws IndeterminateConfigurationException, RegistryException
    {
        configurationHandler.save( configuration, ConfigurationHandler.REGISTRY_EVENT_TAG );
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
    public ManagedRepository putRepository( ManagedRepository managedRepository ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = managedRepository.getId( );
            if ( remoteRepositories.containsKey( id ) )
            {
                throw new RepositoryException( "There exists a remote repository with id " + id + ". Could not update with managed repository." );
            }
            ManagedRepository originRepo = managedRepositories.put( id, managedRepository );
            try
            {
                if ( originRepo != null && originRepo != managedRepository )
                {
                    originRepo.close( );
                }
                RepositoryProvider provider = getProvider( managedRepository.getType( ) );
                ManagedRepositoryConfiguration newCfg = provider.getManagedConfiguration( managedRepository );
                Configuration configuration = configurationHandler.getBaseConfiguration( );
                updateRepositoryReferences( provider, managedRepository, newCfg, configuration );
                ManagedRepositoryConfiguration oldCfg = configuration.findManagedRepositoryById( id );
                if ( oldCfg != null )
                {
                    configuration.removeManagedRepository( oldCfg );
                }
                configuration.addManagedRepository( newCfg );
                saveConfiguration( configuration );
                if ( originRepo != managedRepository )
                {
                    pushEvent( new LifecycleEvent( LifecycleEvent.REGISTERED, this, managedRepository ) );
                }
                else
                {
                    pushEvent( new LifecycleEvent( LifecycleEvent.UPDATED, this, managedRepository ) );
                }
                return managedRepository;
            }
            catch ( Exception e )
            {
                // Rollback only partly, because repository is closed already
                if ( originRepo != null )
                {
                    managedRepositories.put( id, originRepo );
                }
                else
                {
                    managedRepositories.remove( id );
                }
                log.error( "Exception during configuration update {}", e.getMessage( ), e );
                throw new RepositoryException( "Could not save the configuration" + ( e.getMessage( ) == null ? "" : ": " + e.getMessage( ) ) );
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
    @Override
    public ManagedRepository putRepository( ManagedRepositoryConfiguration managedRepositoryConfiguration ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = managedRepositoryConfiguration.getId( );
            final RepositoryType repositoryType = RepositoryType.valueOf( managedRepositoryConfiguration.getType( ) );
            Configuration configuration = configurationHandler.getBaseConfiguration( );
            ManagedRepository repo = managedRepositories.get( id );
            ManagedRepositoryConfiguration oldCfg = repo != null ? getProvider( repositoryType ).getManagedConfiguration( repo ) : null;
            repo = putRepository( managedRepositoryConfiguration, configuration );
            try
            {
                saveConfiguration( configuration );
            }
            catch ( IndeterminateConfigurationException | RegistryException e )
            {
                if ( oldCfg != null )
                {
                    getProvider( repositoryType ).updateManagedInstance( (EditableManagedRepository) repo, oldCfg );
                }
                log.error( "Could not save the configuration for repository {}: {}", id, e.getMessage( ), e );
                throw new RepositoryException( "Could not save the configuration for repository " + id + ": " + e.getMessage( ) );
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
     * @param managedRepositoryConfiguration the new or changed managed repository configuration
     * @param configuration                  the configuration object (may be <code>null</code>)
     * @return the new or updated repository
     * @throws RepositoryException if the configuration cannot be saved or updated
     */
    @Override
    public ManagedRepository putRepository( ManagedRepositoryConfiguration managedRepositoryConfiguration, Configuration configuration ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = managedRepositoryConfiguration.getId( );
            final RepositoryType repoType = RepositoryType.valueOf( managedRepositoryConfiguration.getType( ) );
            ManagedRepository repo;
            boolean registeredNew = false;
            repo = managedRepositories.get( id );
            if ( repo != null && repo.isOpen( ) )
            {
                if ( repo instanceof EditableManagedRepository )
                {
                    getProvider( repoType ).updateManagedInstance( (EditableManagedRepository) repo, managedRepositoryConfiguration );
                }
                else
                {
                    throw new RepositoryException( "The repository is not editable " + id );
                }
            }
            else
            {
                repo = getProvider( repoType ).createManagedInstance( managedRepositoryConfiguration );
                managedRepositories.put( id, repo );
                registeredNew = true;
            }
            updateRepositoryReferences( getProvider( repoType ), repo, managedRepositoryConfiguration, configuration );
            replaceOrAddRepositoryConfig( managedRepositoryConfiguration, configuration );
            if ( registeredNew )
            {
                pushEvent( new LifecycleEvent( LifecycleEvent.REGISTERED, this, repo ) );
            }
            else
            {
                pushEvent( new LifecycleEvent( LifecycleEvent.UPDATED, this, repo ) );
            }
            return repo;
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
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
    public RepositoryGroup putRepositoryGroup( RepositoryGroup repositoryGroup ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            if ( this.groupHandler == null )
            {
                throw new RepositoryException( "Fatal error. RepositoryGroupHandler not registered!" );
            }
            return this.groupHandler.put( repositoryGroup );
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
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
    public RepositoryGroup putRepositoryGroup( RepositoryGroupConfiguration repositoryGroupConfiguration ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            return groupHandler.put( repositoryGroupConfiguration );
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }

    }

    @Override
    public CheckedResult<RepositoryGroup, Map<String, List<ValidationError>>> putRepositoryGroupAndValidate( RepositoryGroupConfiguration repositoryGroupConfiguration )
        throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            return groupHandler.putWithCheck( repositoryGroupConfiguration, groupHandler.getValidator() );
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
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
    public RepositoryGroup putRepositoryGroup( RepositoryGroupConfiguration repositoryGroupConfiguration, Configuration configuration ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            return groupHandler.put( repositoryGroupConfiguration, configuration );
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
    }

    private void replaceOrAddRepositoryConfig( ManagedRepositoryConfiguration managedRepositoryConfiguration, Configuration configuration )
    {
        if ( configuration != null )
        {
            ManagedRepositoryConfiguration oldCfg = configuration.findManagedRepositoryById( managedRepositoryConfiguration.getId( ) );
            if ( oldCfg != null )
            {
                configuration.removeManagedRepository( oldCfg );
            }
            configuration.addManagedRepository( managedRepositoryConfiguration );
        }
    }

    private void replaceOrAddRepositoryConfig( RemoteRepositoryConfiguration remoteRepositoryConfiguration, Configuration configuration )
    {
        if ( configuration != null )
        {
            RemoteRepositoryConfiguration oldCfg = configuration.findRemoteRepositoryById( remoteRepositoryConfiguration.getId( ) );
            if ( oldCfg != null )
            {
                configuration.removeRemoteRepository( oldCfg );
            }
            configuration.addRemoteRepository( remoteRepositoryConfiguration );
        }
    }

    private void replaceOrAddRepositoryConfig( RepositoryGroupConfiguration repositoryGroupConfiguration, Configuration configuration )
    {
        RepositoryGroupConfiguration oldCfg = configuration.findRepositoryGroupById( repositoryGroupConfiguration.getId( ) );
        if ( oldCfg != null )
        {
            configuration.removeRepositoryGroup( oldCfg );
        }
        configuration.addRepositoryGroup( repositoryGroupConfiguration );
    }

    @Override
    public RemoteRepository putRepository( RemoteRepository remoteRepository, Configuration configuration ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = remoteRepository.getId( );
            if ( managedRepositories.containsKey( id ) )
            {
                throw new RepositoryException( "There exists a managed repository with id " + id + ". Could not update with remote repository." );
            }
            RemoteRepository originRepo = remoteRepositories.put( id, remoteRepository );
            RemoteRepositoryConfiguration oldCfg = null;
            RemoteRepositoryConfiguration newCfg;
            try
            {
                if ( originRepo != null && originRepo != remoteRepository )
                {
                    originRepo.close( );
                }
                final RepositoryProvider provider = getProvider( remoteRepository.getType( ) );
                newCfg = provider.getRemoteConfiguration( remoteRepository );
                updateRepositoryReferences( provider, remoteRepository, newCfg, configuration );
                oldCfg = configuration.findRemoteRepositoryById( id );
                if ( oldCfg != null )
                {
                    configuration.removeRemoteRepository( oldCfg );
                }
                configuration.addRemoteRepository( newCfg );
                if ( remoteRepository != originRepo )
                {
                    pushEvent( new LifecycleEvent( LifecycleEvent.REGISTERED, this, remoteRepository ) );
                }
                else
                {
                    pushEvent( new LifecycleEvent( LifecycleEvent.UPDATED, this, remoteRepository ) );
                }
                return remoteRepository;
            }
            catch ( Exception e )
            {
                // Rollback
                if ( originRepo != null )
                {
                    remoteRepositories.put( id, originRepo );
                }
                else
                {
                    remoteRepositories.remove( id );
                }
                if ( oldCfg != null )
                {
                    RemoteRepositoryConfiguration cfg = configuration.findRemoteRepositoryById( id );
                    if ( cfg != null )
                    {
                        configuration.removeRemoteRepository( cfg );
                        configuration.addRemoteRepository( oldCfg );
                    }
                }
                log.error( "Error while adding remote repository {}", e.getMessage( ), e );
                throw new RepositoryException( "Could not save the configuration" + ( e.getMessage( ) == null ? "" : ": " + e.getMessage( ) ) );
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
    @Override
    public RemoteRepository putRepository( RemoteRepository remoteRepository ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            Configuration configuration = configurationHandler.getBaseConfiguration( );
            try
            {
                RemoteRepository repo = putRepository( remoteRepository, configuration );
                saveConfiguration( configuration );
                return repo;
            }
            catch ( RegistryException | IndeterminateConfigurationException e )
            {
                log.error( "Error while saving remote repository {}", e.getMessage( ), e );
                throw new RepositoryException( "Could not save the configuration" + ( e.getMessage( ) == null ? "" : ": " + e.getMessage( ) ) );
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
    @Override
    public RemoteRepository putRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = remoteRepositoryConfiguration.getId( );
            final RepositoryType repositoryType = RepositoryType.valueOf( remoteRepositoryConfiguration.getType( ) );
            Configuration configuration = configurationHandler.getBaseConfiguration( );
            RemoteRepository repo = remoteRepositories.get( id );
            RemoteRepositoryConfiguration oldCfg = repo != null ? getProvider( repositoryType ).getRemoteConfiguration( repo ) : null;
            repo = putRepository( remoteRepositoryConfiguration, configuration );
            try
            {
                saveConfiguration( configuration );
            }
            catch ( IndeterminateConfigurationException | RegistryException e )
            {
                if ( oldCfg != null )
                {
                    getProvider( repositoryType ).updateRemoteInstance( (EditableRemoteRepository) repo, oldCfg );
                }
                log.error( "Could not save the configuration for repository {}: {}", id, e.getMessage( ), e );
                throw new RepositoryException( "Could not save the configuration for repository " + id + ": " + e.getMessage( ) );
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
     * @param configuration                 the configuration object
     * @return the new or updated repository
     * @throws RepositoryException if the configuration cannot be saved or updated
     */
    @Override
    @SuppressWarnings( "unchecked" )
    public RemoteRepository putRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration, Configuration configuration ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            final String id = remoteRepositoryConfiguration.getId( );
            final RepositoryType repoType = RepositoryType.valueOf( remoteRepositoryConfiguration.getType( ) );
            RemoteRepository repo;
            boolean registeredNew = false;
            repo = remoteRepositories.get( id );
            if ( repo != null && repo.isOpen( ) )
            {
                if ( repo instanceof EditableRemoteRepository )
                {
                    getProvider( repoType ).updateRemoteInstance( (EditableRemoteRepository) repo, remoteRepositoryConfiguration );
                }
                else
                {
                    throw new RepositoryException( "The repository is not editable " + id );
                }
            }
            else
            {
                repo = getProvider( repoType ).createRemoteInstance( remoteRepositoryConfiguration );
                remoteRepositories.put( id, repo );
                registeredNew = true;
            }
            updateRepositoryReferences( getProvider( repoType ), repo, remoteRepositoryConfiguration, configuration );
            replaceOrAddRepositoryConfig( remoteRepositoryConfiguration, configuration );
            if ( registeredNew )
            {
                pushEvent( new LifecycleEvent( LifecycleEvent.REGISTERED, this, repo ) );
            }
            else
            {
                pushEvent( new LifecycleEvent( LifecycleEvent.UPDATED, this, repo ) );
            }
            return repo;
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }


    }

    @Override
    public void removeRepository( String repoId ) throws RepositoryException
    {
        Repository repo = getRepository( repoId );
        if ( repo != null )
        {
            removeRepository( repo );
        }
    }

    @Override
    public void removeRepository( Repository repo ) throws RepositoryException
    {
        if ( repo == null )
        {
            log.warn( "Trying to remove null repository" );
            return;
        }
        if ( repo instanceof RemoteRepository )
        {
            removeRepository( (RemoteRepository) repo );
        }
        else if ( repo instanceof ManagedRepository )
        {
            removeRepository( (ManagedRepository) repo );
        }
        else if ( repo instanceof RepositoryGroup )
        {
            removeRepositoryGroup( (RepositoryGroup) repo );
        }
        else
        {
            throw new RepositoryException( "Repository type not known: " + repo.getClass( ) );
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
    public void removeRepository( ManagedRepository managedRepository ) throws RepositoryException
    {
        if ( managedRepository == null )
        {
            return;
        }
        final String id = managedRepository.getId( );
        ManagedRepository repo = getManagedRepository( id );
        if ( repo != null )
        {
            rwLock.writeLock( ).lock( );
            try
            {
                repo = managedRepositories.remove( id );
                if ( repo != null )
                {
                    repo.close( );
                    this.groupHandler.removeRepositoryFromGroups( repo );
                    Configuration configuration = configurationHandler.getBaseConfiguration( );
                    ManagedRepositoryConfiguration cfg = configuration.findManagedRepositoryById( id );
                    if ( cfg != null )
                    {
                        configuration.removeManagedRepository( cfg );
                    }
                    saveConfiguration( configuration );
                }
                pushEvent( new LifecycleEvent( LifecycleEvent.UNREGISTERED, this, repo ) );
            }
            catch ( RegistryException | IndeterminateConfigurationException e )
            {
                // Rollback
                log.error( "Could not save config after repository removal: {}", e.getMessage( ), e );
                managedRepositories.put( repo.getId( ), repo );
                throw new RepositoryException( "Could not save configuration after repository removal: " + e.getMessage( ) );
            }
            finally
            {
                rwLock.writeLock( ).unlock( );
            }
        }
    }


    @Override
    public void removeRepository( ManagedRepository managedRepository, Configuration configuration ) throws RepositoryException
    {
        if ( managedRepository == null )
        {
            return;
        }
        final String id = managedRepository.getId( );
        ManagedRepository repo = getManagedRepository( id );
        if ( repo != null )
        {
            rwLock.writeLock( ).lock( );
            try
            {
                repo = managedRepositories.remove( id );
                if ( repo != null )
                {
                    repo.close( );
                    this.groupHandler.removeRepositoryFromGroups( repo );
                    ManagedRepositoryConfiguration cfg = configuration.findManagedRepositoryById( id );
                    if ( cfg != null )
                    {
                        configuration.removeManagedRepository( cfg );
                    }
                }
                pushEvent( new LifecycleEvent( LifecycleEvent.UNREGISTERED, this, repo ) );
            }
            finally
            {
                rwLock.writeLock( ).unlock( );
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
    public void removeRepositoryGroup( RepositoryGroup repositoryGroup ) throws RepositoryException
    {
        if ( repositoryGroup == null )
        {
            return;
        }
        final String id = repositoryGroup.getId( );
        if ( groupHandler.has( id ) )
        {
            rwLock.writeLock( ).lock( );
            try
            {
                groupHandler.remove( id );
            }
            finally
            {
                rwLock.writeLock( ).unlock( );
            }
        }
    }

    @Override
    public void removeRepositoryGroup( RepositoryGroup repositoryGroup, Configuration configuration ) throws RepositoryException
    {
        if ( repositoryGroup == null )
        {
            return;
        }
        final String id = repositoryGroup.getId( );
        if ( groupHandler.has( id ) )
        {
            rwLock.writeLock( ).lock( );
            try
            {
                groupHandler.remove( id, configuration );
            }
            finally
            {
                rwLock.writeLock( ).unlock( );
            }
        }
    }

    private void doRemoveRepo( RemoteRepository repo, Configuration configuration )
    {
        repo.close( );
        RemoteRepositoryConfiguration cfg = configuration.findRemoteRepositoryById( repo.getId( ) );
        if ( cfg != null )
        {
            configuration.removeRemoteRepository( cfg );
        }
        List<ProxyConnectorConfiguration> proxyConnectors = new ArrayList<>( configuration.getProxyConnectors( ) );
        for ( ProxyConnectorConfiguration proxyConnector : proxyConnectors )
        {
            if ( StringUtils.equals( proxyConnector.getTargetRepoId( ), repo.getId( ) ) )
            {
                configuration.removeProxyConnector( proxyConnector );
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
    public void removeRepository( RemoteRepository remoteRepository ) throws RepositoryException
    {
        if ( remoteRepository == null )
        {
            return;
        }
        final String id = remoteRepository.getId( );
        RemoteRepository repo = getRemoteRepository( id );
        if ( repo != null )
        {
            rwLock.writeLock( ).lock( );
            try
            {
                repo = remoteRepositories.remove( id );
                if ( repo != null )
                {
                    Configuration configuration = configurationHandler.getBaseConfiguration( );
                    doRemoveRepo( repo, configuration );
                    saveConfiguration( configuration );
                }
                pushEvent( new LifecycleEvent( LifecycleEvent.UNREGISTERED, this, repo ) );
            }
            catch ( RegistryException | IndeterminateConfigurationException e )
            {
                // Rollback
                log.error( "Could not save config after repository removal: {}", e.getMessage( ), e );
                remoteRepositories.put( repo.getId( ), repo );
                throw new RepositoryException( "Could not save configuration after repository removal: " + e.getMessage( ) );
            }
            finally
            {
                rwLock.writeLock( ).unlock( );
            }
        }
    }

    @Override
    public void removeRepository( RemoteRepository remoteRepository, Configuration configuration ) throws RepositoryException
    {
        if ( remoteRepository == null )
        {
            return;
        }
        final String id = remoteRepository.getId( );
        RemoteRepository repo = getRemoteRepository( id );
        if ( repo != null )
        {
            rwLock.writeLock( ).lock( );
            try
            {
                repo = remoteRepositories.remove( id );
                if ( repo != null )
                {
                    doRemoveRepo( repo, configuration );
                }
                pushEvent( new LifecycleEvent( LifecycleEvent.UNREGISTERED, this, repo ) );
            }
            finally
            {
                rwLock.writeLock( ).unlock( );
            }
        }

    }

    /**
     * Reloads the registry from the configuration.
     */
    @Override
    public void reload( )
    {
        initialize( );
    }

    /**
     * Resets the indexing context of a given repository.
     *
     * @param repository The repository
     * @throws IndexUpdateFailedException If the index could not be resetted.
     */
    @Override
    public void resetIndexingContext( Repository repository ) throws IndexUpdateFailedException
    {
        if ( repository.hasIndex( ) && repository instanceof EditableRepository )
        {
            EditableRepository eRepo = (EditableRepository) repository;
            ArchivaIndexingContext newCtx = getIndexManager( repository.getType( ) ).reset( repository.getIndexingContext( ) );
            eRepo.setIndexingContext( newCtx );
        }
    }


    /**
     * Creates a new repository instance with the same settings as this one. The cloned repository is not
     * registered or saved to the configuration.
     *
     * @param repo The origin repository
     * @return The cloned repository.
     */
    public ManagedRepository clone( ManagedRepository repo, String newId ) throws RepositoryException
    {
        if ( managedRepositories.containsKey( newId ) || remoteRepositories.containsKey( newId ) )
        {
            throw new RepositoryException( "The given id exists already " + newId );
        }
        RepositoryProvider provider = getProvider( repo.getType( ) );
        ManagedRepositoryConfiguration cfg = provider.getManagedConfiguration( repo );
        cfg.setId( newId );
        ManagedRepository cloned = provider.createManagedInstance( cfg );
        cloned.registerEventHandler( RepositoryEvent.ANY, this );
        return cloned;
    }

    @Override
    public <T extends Repository> T clone( T repo, String newId ) throws RepositoryException
    {
        if ( repo instanceof RemoteRepository )
        {
            return (T) this.clone( (RemoteRepository) repo, newId );
        }
        else if ( repo instanceof ManagedRepository )
        {
            return (T) this.clone( (ManagedRepository) repo, newId );
        }
        else
        {
            throw new RepositoryException( "This repository class is not supported " + repo.getClass( ).getName( ) );
        }
    }

    /**
     * Creates a new repository instance with the same settings as this one. The cloned repository is not
     * registered or saved to the configuration.
     *
     * @param repo The origin repository
     * @return The cloned repository.
     */
    public RemoteRepository clone( RemoteRepository repo, String newId ) throws RepositoryException
    {
        if ( managedRepositories.containsKey( newId ) || remoteRepositories.containsKey( newId ) )
        {
            throw new RepositoryException( "The given id exists already " + newId );
        }
        RepositoryProvider provider = getProvider( repo.getType( ) );
        RemoteRepositoryConfiguration cfg = provider.getRemoteConfiguration( repo );
        cfg.setId( newId );
        RemoteRepository cloned = provider.createRemoteInstance( cfg );
        cloned.registerEventHandler( RepositoryEvent.ANY, this );
        return cloned;
    }

    @Override
    public Repository getRepositoryOfAsset( StorageAsset asset )
    {
        if ( asset instanceof Repository )
        {
            return (Repository) asset;
        }
        else
        {
            return getRepositories( ).stream( ).filter( r -> r.getRoot( )
                .getStorage( ).equals( asset.getStorage( ) ) ).findFirst( ).orElse( null );
        }
    }

    @Override
    public <R extends Repository> ValidationResponse<R> validateRepository( R repository )
    {
        Map<String, List<ValidationError>> errorMap = this.validators.stream( )
            .filter( ( validator ) -> validator.getType( ).equals( RepositoryType.ALL ) || repository.getType( ).equals( validator.getType( ) ) )
            .filter( val -> val.isFlavour( repository.getClass() ))
            .flatMap( validator -> ((RepositoryValidator<R>)validator).apply( repository ).getResult().entrySet( ).stream( ) )
            .collect( Collectors.toMap(
                entry -> entry.getKey( ),
                entry -> entry.getValue( ),
                ( list1, list2 ) -> ListUtils.union( list1, list2 )
            ) );
        return new ValidationResponse( repository, errorMap );
    }

    @Override
    public <R extends Repository> ValidationResponse<R> validateRepositoryForUpdate( R repository )
    {
        Map<String, List<ValidationError>> errorMap = this.validators.stream( )
            .filter( ( validator ) -> validator.getType( ).equals( RepositoryType.ALL ) || repository.getType( ).equals( validator.getType( ) ) )
            .filter( val -> val.isFlavour( repository.getClass() ))
            .flatMap( validator -> ((RepositoryValidator<R>)validator).applyForUpdate( repository ).getResult().entrySet( ).stream( ) )
            .collect( Collectors.toMap(
                entry -> entry.getKey( ),
                entry -> entry.getValue( ),
                ( list1, list2 ) -> ListUtils.union( list1, list2 )
            ) );
        return new ValidationResponse( repository, errorMap );
    }

    @Override
    public void configurationEvent( ConfigurationEvent event )
    {
        // We ignore the event, if the save was triggered by ourself
        if ( !ConfigurationHandler.REGISTRY_EVENT_TAG.equals( event.getTag( ) ) )
        {
            reload( );
        }
    }


    @Override
    public <T extends Event> void registerEventHandler( EventType<T> type, EventHandler<? super T> eventHandler )
    {
        eventManager.registerEventHandler( type, eventHandler );
    }


    @Override
    public <T extends Event> void unregisterEventHandler( EventType<T> type, EventHandler<? super T> eventHandler )
    {
        eventManager.unregisterEventHandler( type, eventHandler );
    }


    @Override
    public void handle( Event event )
    {
        // To avoid event cycles:
        if ( sameOriginator( event ) )
        {
            return;
        }
        if ( event instanceof RepositoryIndexEvent )
        {
            handleIndexCreationEvent( (RepositoryIndexEvent) event );
        }
        // We propagate all events to our listeners, but with context of repository registry
        pushEvent( event );
    }

    private void handleIndexCreationEvent( RepositoryIndexEvent event )
    {
        RepositoryIndexEvent idxEvent = event;
        EditableRepository repo = (EditableRepository) idxEvent.getRepository( );
        if ( repo != null )
        {
            ArchivaIndexManager idxmgr = getIndexManager( repo.getType( ) );
            if ( repo.getIndexingContext( ) != null )
            {
                try
                {
                    ArchivaIndexingContext newCtx = idxmgr.move( repo.getIndexingContext( ), repo );
                    repo.setIndexingContext( newCtx );
                    idxmgr.updateLocalIndexPath( repo );

                }
                catch ( IndexCreationFailedException e )
                {
                    log.error( "Could not move index to new directory: '{}'", e.getMessage( ), e );
                }
            }
            else
            {
                try
                {
                    ArchivaIndexingContext context = idxmgr.createContext( repo );
                    repo.setIndexingContext( context );
                    idxmgr.updateLocalIndexPath( repo );
                }
                catch ( IndexCreationFailedException e )
                {
                    log.error( "Could not create index:  '{}'", e.getMessage( ), e );
                }
            }
        }
    }

    private boolean sameOriginator( Event event )
    {
        if ( event.getSource( ) == this )
        {
            return true;
        }
        else if ( event.hasPreviousEvent( ) )
        {
            return sameOriginator( event.getPreviousEvent( ) );
        }
        else
        {
            return false;
        }
    }

    private void pushEvent( Event event )
    {
        eventManager.fireEvent( event );
    }


}
