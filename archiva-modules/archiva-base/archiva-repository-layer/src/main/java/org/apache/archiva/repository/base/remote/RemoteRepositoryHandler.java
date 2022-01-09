package org.apache.archiva.repository.base.remote;
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
import org.apache.archiva.configuration.model.Configuration;
import org.apache.archiva.configuration.provider.IndeterminateConfigurationException;
import org.apache.archiva.configuration.model.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.model.RemoteRepositoryConfiguration;
import org.apache.archiva.indexer.ArchivaIndexManager;
import org.apache.archiva.indexer.IndexCreationFailedException;
import org.apache.archiva.indexer.IndexManagerFactory;
import org.apache.archiva.repository.EditableRemoteRepository;
import org.apache.archiva.repository.EditableRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryHandler;
import org.apache.archiva.repository.RepositoryHandlerManager;
import org.apache.archiva.repository.RepositoryProvider;
import org.apache.archiva.repository.RepositoryState;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.base.AbstractRepositoryHandler;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.event.LifecycleEvent;
import org.apache.archiva.repository.event.RepositoryEvent;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service( "remoteRepositoryHandler#default" )
public class RemoteRepositoryHandler extends AbstractRepositoryHandler<RemoteRepository, RemoteRepositoryConfiguration> implements RepositoryHandler<RemoteRepository, RemoteRepositoryConfiguration>
{
    private static final Logger log = LoggerFactory.getLogger( RemoteRepositoryHandler.class );
    private final RepositoryHandlerManager repositoryHandlerManager;
    private final RepositoryContentFactory repositoryContentFactory;
    private final IndexManagerFactory indexManagerFactory;


    public RemoteRepositoryHandler( RepositoryHandlerManager repositoryHandlerManager, ConfigurationHandler configurationHandler,
                                    IndexManagerFactory indexManagerFactory,
                                    @Named( "repositoryContentFactory#default" ) RepositoryContentFactory repositoryContentFactory )
    {
        super( RemoteRepository.class, RemoteRepositoryConfiguration.class, configurationHandler );
        this.repositoryHandlerManager = repositoryHandlerManager;
        this.repositoryContentFactory = repositoryContentFactory;
        this.indexManagerFactory = indexManagerFactory;
    }

    @Override
    @PostConstruct
    public void init( )
    {
        log.debug( "Initializing repository handler " + RemoteRepositoryHandler.class );
        initializeStorage( );
        // We are registering this class on the registry. This is necessary to avoid circular dependencies via injection.
        this.repositoryHandlerManager.registerHandler( this );
    }

    private void initializeStorage( )
    {

    }

    @Override
    public void initializeFromConfig( )
    {
        Map<String, RemoteRepository> currentInstances = new HashMap<>( getRepositories( ) );
        getRepositories().clear();
        Map<String, RemoteRepository> newAndUpdated = newOrUpdateInstancesFromConfig( currentInstances );
        getRepositories( ).putAll( newAndUpdated );
        currentInstances.entrySet( ).stream( ).filter( entry -> !newAndUpdated.containsKey( entry.getKey( ) ) ).forEach(
            r ->
            deactivateRepository( r.getValue() )
        );
        for ( RemoteRepository remoteRepository : getRepositories( ).values( ) )
        {
            activateRepository( remoteRepository );
        }


    }

    public Map<String, RemoteRepository> newOrUpdateInstancesFromConfig( Map<String, RemoteRepository> currentInstances)
    {
        try
        {
            List<RemoteRepositoryConfiguration> remoteRepoConfigs =
                new ArrayList<>(
                    getConfigurationHandler( ).getBaseConfiguration( ).getRemoteRepositories( ) );

            if ( remoteRepoConfigs == null )
            {
                return Collections.emptyMap( );
            }

            Map<String, RemoteRepository> result = new HashMap<>( );
            for ( RemoteRepositoryConfiguration repoConfig : remoteRepoConfigs )
            {
                String id = repoConfig.getId( );
                if (result.containsKey( id )) {
                    log.error( "There are repositories with the same id in the configuration: {}", id );
                    continue;
                }
                RemoteRepository repo;
                if ( currentInstances.containsKey( id ) )
                {
                    repo = currentInstances.remove( id );
                    getProvider( repo.getType( ) ).updateRemoteInstance( (EditableRemoteRepository) repo, repoConfig );
                    updateReferences( repo, repoConfig );
                }
                else
                {
                    repo = newInstance( repoConfig );
                }
                result.put( id, repo );
            }
            return result;
        }
        catch ( Throwable e )
        {
            log.error( "Could not initialize repositories from config: {}", e.getMessage( ), e );
            return new HashMap<>( );
        }
    }


    @Override
    public Map<String, RemoteRepository> newInstancesFromConfig( )
    {
        try
        {
            List<RemoteRepositoryConfiguration> remoteRepoConfigs =
                new ArrayList<>(
                    getConfigurationHandler( ).getBaseConfiguration( ).getRemoteRepositories( ) );

            if ( remoteRepoConfigs == null )
            {
                return Collections.emptyMap( );
            }

            Map<String, RemoteRepository> result = new HashMap<>( );
            for ( RemoteRepositoryConfiguration repoConfig : remoteRepoConfigs )
            {
                RemoteRepository repo = newInstance( repoConfig );
                result.put( repo.getId( ), repo );
            }
            return result;
        }
        catch ( Throwable e )
        {
            log.error( "Could not initialize repositories from config: {}", e.getMessage( ), e );
            return new HashMap<>( );
        }
    }

    @Override
    public RemoteRepository newInstance( RepositoryType type, String id ) throws RepositoryException
    {
        log.debug( "Creating repo {}", id );
        RepositoryProvider provider = getProvider( type );
        EditableRemoteRepository repo;
        repo = provider.createRemoteInstance( id, id );
        repo.registerEventHandler( RepositoryEvent.ANY, repositoryHandlerManager );
        updateReferences( repo, null );
        repo.setLastState( RepositoryState.REFERENCES_SET );
        return repo;

    }


    @Override
    public RemoteRepository newInstance( RemoteRepositoryConfiguration repositoryConfiguration ) throws RepositoryException
    {
        RepositoryType type = RepositoryType.valueOf( repositoryConfiguration.getType( ) );
        RepositoryProvider provider = getProvider( type );
        if ( provider == null )
        {
            throw new RepositoryException( "Provider not found for repository type: " + repositoryConfiguration.getType( ) );
        }
        final RemoteRepository repo = provider.createRemoteInstance( repositoryConfiguration );
        repo.registerEventHandler( RepositoryEvent.ANY, repositoryHandlerManager );
        updateReferences( repo, null );
        if ( repo instanceof EditableRepository )
        {
            ( (EditableRepository) repo ).setLastState( RepositoryState.REFERENCES_SET );
        }
        return repo;
    }

    @Override
    protected RemoteRepositoryConfiguration findRepositoryConfiguration( Configuration configuration, String id )
    {
        return configuration.findRemoteRepositoryById( id );
    }

    @Override
    protected void removeRepositoryConfiguration( Configuration configuration, RemoteRepositoryConfiguration repoConfiguration )
    {
        configuration.removeRemoteRepository( repoConfiguration );
        List<ProxyConnectorConfiguration> proxyConnectors = new ArrayList<>( configuration.getProxyConnectors( ) );
        for ( ProxyConnectorConfiguration proxyConnector : proxyConnectors )
        {
            if ( StringUtils.equals( proxyConnector.getTargetRepoId( ), repoConfiguration.getId( ) ) )
            {
                configuration.removeProxyConnector( proxyConnector );
            }
        }
    }

    @Override
    protected void addRepositoryConfiguration( Configuration configuration, RemoteRepositoryConfiguration repoConfiguration )
    {
        configuration.addRemoteRepository( repoConfiguration );

    }



    @Override
    public RemoteRepository put( RemoteRepository repository ) throws RepositoryException
    {
        final String id = repository.getId( );
        RemoteRepository originRepo = getRepositories( ).remove( id );
        if ( originRepo == null && repositoryHandlerManager.isRegisteredId( id ) )
        {
            throw new RepositoryException( "There exists a repository with id " + id + ". Could not update with managed repository." );
        }
        try
        {
            if ( originRepo != null && repository != originRepo )
            {
                deactivateRepository( originRepo );
                pushEvent( LifecycleEvent.UNREGISTERED, originRepo );
            }
            RepositoryProvider provider = getProvider( repository.getType( ) );
            RemoteRepositoryConfiguration newCfg = provider.getRemoteConfiguration( repository );
            getConfigurationHandler( ).getLock( ).writeLock( ).lock( );
            try
            {
                Configuration configuration = getConfigurationHandler( ).getBaseConfiguration( );
                updateReferences( repository, newCfg );
                RemoteRepositoryConfiguration oldCfg = configuration.findRemoteRepositoryById( id );
                if ( oldCfg != null )
                {
                    configuration.removeRemoteRepository( oldCfg );
                }
                configuration.addRemoteRepository( newCfg );
                getConfigurationHandler( ).save( configuration, ConfigurationHandler.REGISTRY_EVENT_TAG );
                setLastState( repository, RepositoryState.SAVED );
                activateRepository( repository );
            }
            finally
            {
                getConfigurationHandler( ).getLock( ).writeLock( ).unlock( );
            }
            getRepositories( ).put( id, repository );
            setLastState( repository, RepositoryState.REGISTERED );
            return repository;
        }
        catch ( Exception e )
        {
            // Rollback only partly, because repository is closed already
            if ( originRepo != null )
            {
                getRepositories( ).put( id, originRepo );
            }
            else
            {
                getRepositories( ).remove( id );
            }
            log.error( "Exception during configuration update {}", e.getMessage( ), e );
            throw new RepositoryException( "Could not save the configuration" + ( e.getMessage( ) == null ? "" : ": " + e.getMessage( ) ) );
        }

    }

    @Override
    public RemoteRepository put( RemoteRepositoryConfiguration repositoryConfiguration ) throws RepositoryException
    {
        final String id = repositoryConfiguration.getId( );
        final RepositoryType repositoryType = RepositoryType.valueOf( repositoryConfiguration.getType( ) );
        final RepositoryProvider provider = getProvider( repositoryType );
        ReentrantReadWriteLock.WriteLock configLock = this.getConfigurationHandler( ).getLock( ).writeLock( );
        configLock.lock( );
        RemoteRepository repo = null;
        RemoteRepository oldRepository = null;
        Configuration configuration = null;
        try
        {
            boolean updated = false;
            configuration = getConfigurationHandler( ).getBaseConfiguration( );
            repo = getRepositories( ).get( id );
            oldRepository = repo == null ? null : clone( repo, id );
            if ( repo == null )
            {
                repo = put( repositoryConfiguration, configuration );
            }
            else
            {
                setRepositoryDefaults( repositoryConfiguration );
                provider.updateRemoteInstance( (EditableRemoteRepository) repo, repositoryConfiguration );
                updated = true;
                pushEvent( LifecycleEvent.UPDATED, repo );
            }
            registerNewRepository( repositoryConfiguration, repo, configuration, updated );
        }
        catch ( IndeterminateConfigurationException | RegistryException e )
        {
            if ( oldRepository != null )
            {
                RemoteRepositoryConfiguration oldCfg = provider.getRemoteConfiguration( oldRepository );
                provider.updateRemoteInstance( (EditableRemoteRepository) repo, oldCfg );
                rollback( configuration, oldRepository, e, oldCfg );
            }
            else
            {
                getRepositories( ).remove( id );
            }
            log.error( "Could not save the configuration for repository {}: {}", id, e.getMessage( ), e );
            throw new RepositoryException( "Could not save the configuration for repository " + id + ": " + e.getMessage( ) );
        }
        finally
        {
            configLock.unlock( );
        }
        return repo;

    }

    @SuppressWarnings( "unused" )
    private void setRepositoryDefaults( RemoteRepositoryConfiguration repositoryConfiguration )
    {
        // We do nothing here
    }


    @Override
    public RemoteRepository put( RemoteRepositoryConfiguration repositoryConfiguration, Configuration configuration ) throws RepositoryException
    {
        final String id = repositoryConfiguration.getId( );
        final RepositoryType repoType = RepositoryType.valueOf( repositoryConfiguration.getType( ) );
        RemoteRepository repo;
        setRepositoryDefaults( repositoryConfiguration );
        if ( getRepositories( ).containsKey( id ) )
        {
            repo = clone( getRepositories( ).get( id ), id );
            if ( repo instanceof EditableRemoteRepository )
            {
                getProvider( repoType ).updateRemoteInstance( (EditableRemoteRepository) repo, repositoryConfiguration );
            }
            else
            {
                throw new RepositoryException( "The repository is not editable " + id );
            }
        }
        else
        {
            repo = getProvider( repoType ).createRemoteInstance( repositoryConfiguration );
            setLastState( repo, RepositoryState.CREATED );
        }
        if ( configuration != null )
        {
            replaceOrAddRepositoryConfig( repositoryConfiguration, configuration );
        }
        updateReferences( repo, repositoryConfiguration );
        setLastState( repo, RepositoryState.REFERENCES_SET );
        return repo;

    }

    @Override
    public RemoteRepository clone( RemoteRepository repo, String newId ) throws RepositoryException
    {
        RepositoryProvider provider = getProvider( repo.getType( ) );
        RemoteRepositoryConfiguration cfg = provider.getRemoteConfiguration( repo );
        cfg.setId( newId );
        RemoteRepository cloned = provider.createRemoteInstance( cfg );
        cloned.registerEventHandler( RepositoryEvent.ANY, repositoryHandlerManager );
        setLastState( cloned, RepositoryState.CREATED );
        return cloned;

    }

    @Override
    public void updateReferences( RemoteRepository repo, RemoteRepositoryConfiguration repositoryConfiguration ) throws RepositoryException
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
        repo.registerEventHandler( RepositoryEvent.ANY, repositoryHandlerManager );

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

    public ArchivaIndexManager getIndexManager( RepositoryType type )
    {
        return indexManagerFactory.getIndexManager( type );
    }

}
