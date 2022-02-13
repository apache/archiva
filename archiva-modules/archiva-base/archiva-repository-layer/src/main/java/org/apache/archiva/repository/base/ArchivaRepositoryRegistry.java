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
import org.apache.archiva.configuration.model.AbstractRepositoryConfiguration;
import org.apache.archiva.configuration.provider.ArchivaConfiguration;
import org.apache.archiva.configuration.model.Configuration;
import org.apache.archiva.configuration.provider.ConfigurationEvent;
import org.apache.archiva.configuration.provider.ConfigurationListener;
import org.apache.archiva.configuration.provider.IndeterminateConfigurationException;
import org.apache.archiva.configuration.model.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.model.RemoteRepositoryConfiguration;
import org.apache.archiva.configuration.model.RepositoryGroupConfiguration;
import org.apache.archiva.event.Event;
import org.apache.archiva.event.EventHandler;
import org.apache.archiva.event.BasicEventManager;
import org.apache.archiva.event.EventSource;
import org.apache.archiva.event.EventType;
import org.apache.archiva.event.central.CentralEventManager;
import org.apache.archiva.indexer.ArchivaIndexManager;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.indexer.IndexCreationFailedException;
import org.apache.archiva.indexer.IndexManagerFactory;
import org.apache.archiva.indexer.IndexUpdateFailedException;
import org.apache.archiva.repository.EditableRepository;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.RepositoryHandler;
import org.apache.archiva.repository.RepositoryProvider;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.UnsupportedRepositoryTypeException;
import org.apache.archiva.repository.event.RepositoryIndexEvent;
import org.apache.archiva.repository.event.RepositoryRegistryEvent;
import org.apache.archiva.repository.metadata.MetadataReader;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.validation.CheckedResult;
import org.apache.archiva.repository.validation.RepositoryValidator;
import org.apache.archiva.repository.validation.ValidationError;
import org.apache.archiva.repository.validation.ValidationResponse;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

    @SuppressWarnings( "SpringJavaInjectionPointsAutowiringInspection" )
    @Inject
    IndexManagerFactory indexManagerFactory;

    @Inject
    List<MetadataReader> metadataReaderList;

    @Inject
    List<RepositoryValidator<? extends Repository>> repositoryValidatorList;

    @Inject
    @Named("eventManager#archiva")
    CentralEventManager centralEventManager;

    private boolean ignoreIndexing = false;

    private final BasicEventManager eventManager;


    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock( );

    private RepositoryHandler<RepositoryGroup, RepositoryGroupConfiguration> groupHandler;
    private RepositoryHandler<ManagedRepository, ManagedRepositoryConfiguration> managedRepositoryHandler;
    private RepositoryHandler<RemoteRepository, RemoteRepositoryConfiguration> remoteRepositoryHandler;

    private final Set<RepositoryValidator<? extends Repository>> validators;
    private final ConfigurationHandler configurationHandler;


    private final AtomicBoolean groups_initalized = new AtomicBoolean( false );
    private final AtomicBoolean managed_initialized = new AtomicBoolean( false );
    private final AtomicBoolean remote_initialized = new AtomicBoolean( false );


    public ArchivaRepositoryRegistry( ConfigurationHandler configurationHandler, List<RepositoryValidator<? extends Repository>> validatorList )
    {
        this.eventManager = new BasicEventManager( this );
        this.configurationHandler = configurationHandler;
        this.validators = initValidatorList( validatorList );
    }


    private Set<RepositoryValidator<? extends Repository>> initValidatorList( List<RepositoryValidator<? extends Repository>> validators )
    {
        TreeSet<RepositoryValidator<? extends Repository>> val = new TreeSet<>( );
        for ( RepositoryValidator<? extends Repository> validator : validators )
        {
            val.add( validator );
            validator.setRepositoryRegistry( this );
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
            initializeManagedRepositories();
            initializeRemoteRepositories();
            initializeRepositoryGroups( );

            for ( RepositoryProvider provider : repositoryProviders )
            {
                provider.addRepositoryEventHandler( this );
            }
            this.configurationHandler.addListener( this );
            registerEventHandler( EventType.ROOT, centralEventManager );
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

    private void initializeManagedRepositories( )
    {
        if ( this.managedRepositoryHandler != null )
        {
            this.managedRepositoryHandler.initializeFromConfig( );
            this.managed_initialized.set( true );
            pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.MANAGED_REPOS_INITIALIZED, this ) );
        }
    }

    private void initializeRemoteRepositories() {
        if (this.remoteRepositoryHandler != null ){
            this.remoteRepositoryHandler.initializeFromConfig( );
            this.remote_initialized.set( true );
            pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.REMOTE_REPOS_INITIALIZED, this ) );

        }
    }

    public void registerGroupHandler( RepositoryHandler<RepositoryGroup, RepositoryGroupConfiguration> groupHandler )
    {
        this.groupHandler = groupHandler;
        doRegister( groupHandler );
        initializeRepositoryGroups( );
        if ( managed_initialized.get( ) && remote_initialized.get( ) && groups_initalized.get( ) )
        {
            pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.INITIALIZED, this ) );
        }
    }

    public void registerManagedRepositoryHandler( RepositoryHandler<ManagedRepository, ManagedRepositoryConfiguration> managedRepositoryHandler )
    {
        this.managedRepositoryHandler = managedRepositoryHandler;
        doRegister( managedRepositoryHandler );
        initializeManagedRepositories();
        if ( managed_initialized.get( ) && remote_initialized.get( ) && groups_initalized.get( ) )
        {
            pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.INITIALIZED, this ) );
        }
    }

    public void registerRemoteRepositoryHandler( RepositoryHandler<RemoteRepository, RemoteRepositoryConfiguration> remoteRepositoryHandler )
    {
        this.remoteRepositoryHandler = remoteRepositoryHandler;
        doRegister( remoteRepositoryHandler );
        initializeRemoteRepositories();
        if ( managed_initialized.get( ) && remote_initialized.get( ) && groups_initalized.get( ) )
        {
            pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.INITIALIZED, this ) );
        }
    }

    @PreDestroy
    public void destroy( )
    {
        managedRepositoryHandler.close( );
        remoteRepositoryHandler.close();
        groupHandler.close( );
        pushEvent( new RepositoryRegistryEvent( RepositoryRegistryEvent.DESTROYED, this ) );
    }


    public Map<RepositoryType, RepositoryProvider> getRepositoryProviderMap( )
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

    public RepositoryProvider getProvider( RepositoryType type ) throws RepositoryException
    {
        return repositoryProviders.stream( ).filter( repositoryProvider -> repositoryProvider.provides( ).contains( type ) ).findFirst( ).orElseThrow( ( ) -> new RepositoryException( "Repository type cannot be handled: " + type ) );
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
            return Stream.concat( managedRepositoryHandler.getAll().stream( ), remoteRepositoryHandler.getAll().stream( ) ).collect( Collectors.toList( ) );
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
            return managed_initialized.get() ? managedRepositoryHandler.getAll( ) : Collections.emptyList();
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
            return remote_initialized.get() ? remoteRepositoryHandler.getAll( ) : Collections.emptyList();
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
            if ( managedRepositoryHandler.hasRepository( repoId ) )
            {
                log.debug( "Managed repo" );
                return managedRepositoryHandler.get( repoId );
            }
            else if ( remoteRepositoryHandler.hasRepository( repoId ) )
            {
                log.debug( "Remote repo" );
                return remoteRepositoryHandler.get( repoId );
            }
            else if ( groupHandler.hasRepository( repoId ) )
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
            return managed_initialized.get() ? managedRepositoryHandler.get( repoId ) : null;
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
            return remote_initialized.get() ? remoteRepositoryHandler.get( repoId ) : null;
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
        return ( managedRepositoryHandler != null && managedRepositoryHandler.hasRepository( repoId ) )
            || ( remoteRepositoryHandler != null && remoteRepositoryHandler.hasRepository( repoId ) )
            || ( this.groupHandler != null && groupHandler.hasRepository( repoId ) );
    }

    @Override
    public boolean hasManagedRepository( String repoId )
    {
        return managedRepositoryHandler!=null && managedRepositoryHandler.hasRepository( repoId );
    }

    @Override
    public boolean hasRemoteRepository( String repoId )
    {
        return remoteRepositoryHandler!=null && remoteRepositoryHandler.hasRepository( repoId );
    }

    @Override
    public boolean hasRepositoryGroup( String groupId )
    {
        return this.groupHandler != null && groupHandler.hasRepository( groupId );
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
            return managed_initialized.get() ? managedRepositoryHandler.put( managedRepository ) : null;
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
            return managedRepositoryHandler.put( managedRepositoryConfiguration );
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
            return managedRepositoryHandler.put( managedRepositoryConfiguration, configuration );
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
    }

    @Override
    public CheckedResult<ManagedRepository, Map<String, List<ValidationError>>> putRepositoryAndValidate( ManagedRepositoryConfiguration configuration ) throws RepositoryException
    {
        rwLock.writeLock().lock();
        try {
            return managedRepositoryHandler.putWithCheck( configuration );
        } finally
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
            return groupHandler.putWithCheck( repositoryGroupConfiguration );
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
            return remoteRepositoryHandler.put( remoteRepository );
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
            return remoteRepositoryHandler.put( remoteRepositoryConfiguration );
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
    }

    @Override
    public CheckedResult<RemoteRepository, Map<String, List<ValidationError>>> putRepositoryAndValidate( RemoteRepositoryConfiguration remoteRepositoryConfiguration ) throws RepositoryException
    {
        rwLock.writeLock().lock();
        try {
            return remoteRepositoryHandler.putWithCheck( remoteRepositoryConfiguration );
        } finally
        {
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
    public RemoteRepository putRepository( RemoteRepositoryConfiguration remoteRepositoryConfiguration, Configuration configuration ) throws RepositoryException
    {
        rwLock.writeLock( ).lock( );
        try
        {
            return remoteRepositoryHandler.put( remoteRepositoryConfiguration, configuration );
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
        rwLock.writeLock( ).lock( );
        try
        {
            if (managed_initialized.get() ) managedRepositoryHandler.remove( managedRepository.getId( ) );
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
    }


    @Override
    public void removeRepository( ManagedRepository managedRepository, Configuration configuration ) throws RepositoryException
    {
        if ( managedRepository == null )
        {
            return;
        }
            rwLock.writeLock( ).lock( );
            try
            {
                if (managed_initialized.get()) managedRepositoryHandler.remove( managedRepository.getId( ), configuration );
            }
            finally
            {
                rwLock.writeLock( ).unlock( );
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
        if ( groupHandler.hasRepository( id ) )
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
        if ( groupHandler.hasRepository( id ) )
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
        if ( remoteRepositoryHandler.hasRepository( id ) )
        {
            rwLock.writeLock( ).lock( );
            try
            {
                remoteRepositoryHandler.remove( id );
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
        if ( remoteRepositoryHandler.hasRepository( id ) )
        {
            rwLock.writeLock( ).lock( );
            try
            {
                remoteRepositoryHandler.remove( id, configuration );
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
        if (isRegisteredId( newId )) {
            throw new RepositoryException( "The new id exists already: " + newId );
        }
        return managedRepositoryHandler.clone( repo, newId );
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
        if (isRegisteredId( newId )) {
            throw new RepositoryException( "The new id exists already: " + newId );
        }
        return remoteRepositoryHandler.clone( repo, newId );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T extends Repository> T clone( T repo, String newId ) throws RepositoryException
    {
        if (isRegisteredId( newId )) {
            throw new RepositoryException( "The new id exists already: " + newId );
        }
        if ( repo instanceof RemoteRepository )
        {
            return (T) remoteRepositoryHandler.clone( (RemoteRepository) repo, newId );
        }
        else if ( repo instanceof ManagedRepository )
        {
            return (T) managedRepositoryHandler.clone( (ManagedRepository) repo, newId );
        }
        else if (repo instanceof RepositoryGroup) {
            return (T) groupHandler.clone( (RepositoryGroup) repo, newId );
        }
        else
        {
            throw new RepositoryException( "This repository class is not supported " + repo.getClass( ).getName( ) );
        }
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
        @SuppressWarnings( "unchecked" ) Map<String, List<ValidationError>> errorMap = this.validators.stream( )
            .filter( ( validator ) -> validator.getType( ).equals( RepositoryType.ALL ) || repository.getType( ).equals( validator.getType( ) ) )
            .filter( val -> val.isFlavour( repository.getClass( ) ) )
            .flatMap( validator -> ( (RepositoryValidator<R>) validator ).apply( repository ).getResult( ).entrySet( ).stream( ) )
            .collect( Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                ListUtils::union
            ) );
        return new ValidationResponse<>( repository, errorMap );
    }

    @Override
    public <R extends Repository> ValidationResponse<R> validateRepositoryForUpdate( R repository )
    {
        @SuppressWarnings( "unchecked" ) Map<String, List<ValidationError>> errorMap = this.validators.stream( )
            .filter( ( validator ) -> validator.getType( ).equals( RepositoryType.ALL ) || repository.getType( ).equals( validator.getType( ) ) )
            .filter( val -> val.isFlavour( repository.getClass( ) ) )
            .flatMap( validator -> ( (RepositoryValidator<R>) validator ).applyForUpdate( repository ).getResult( ).entrySet( ).stream( ) )
            .collect( Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                ListUtils::union
            ) );
        return new ValidationResponse<>( repository, errorMap );
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
        if (!ignoreIndexing && !( event.getRepository() instanceof  ManagedRepository ))
        {
            EditableRepository repo = (EditableRepository) event.getRepository( );
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

    private <R extends Repository, C extends AbstractRepositoryConfiguration> void doRegister( RepositoryHandler<R, C> repositoryHandler )
    {
        repositoryHandler.setRepositoryProviders( this.repositoryProviders );
        repositoryHandler.setRepositoryValidator( this.repositoryValidatorList );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public void registerHandler( RepositoryHandler<?, ?> handler )
    {
        if ( handler.getFlavour( ).isAssignableFrom( RepositoryGroup.class ) )
        {
            registerGroupHandler( (RepositoryHandler<RepositoryGroup, RepositoryGroupConfiguration>) handler );
        }
        else if ( handler.getFlavour( ).isAssignableFrom( ManagedRepository.class ) )
        {
            registerManagedRepositoryHandler( (RepositoryHandler<ManagedRepository, ManagedRepositoryConfiguration>) handler );
        }
        else if ( handler.getFlavour().isAssignableFrom( RemoteRepository.class )) {
            registerRemoteRepositoryHandler( (RepositoryHandler<RemoteRepository, RemoteRepositoryConfiguration>) handler );
        }
    }

    @Override
    public boolean isRegisteredId( String id )
    {
        return hasRepository( id );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <R extends Repository, C extends AbstractRepositoryConfiguration> RepositoryHandler<R, C> getHandler( Class<R> repositoryClazz, Class<C> configurationClazz )
    {
        if ( repositoryClazz.isAssignableFrom( RepositoryGroup.class ) )
        {
            return (RepositoryHandler<R, C>) this.groupHandler;
        }
        else if ( repositoryClazz.isAssignableFrom( ManagedRepository.class ) )
        {
            return (RepositoryHandler<R, C>) this.managedRepositoryHandler;
        }
        else if ( repositoryClazz.isAssignableFrom( RemoteRepository.class )) {
            return (RepositoryHandler<R, C>) this.remoteRepositoryHandler;
        }
        else
        {
            return null;
        }
    }

    public boolean isIgnoreIndexing( )
    {
        return ignoreIndexing;
    }

    public void setIgnoreIndexing( boolean ignoreIndexing )
    {
        this.ignoreIndexing = ignoreIndexing;
    }


}
