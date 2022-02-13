package org.apache.archiva.repository.base.managed;
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
import org.apache.archiva.configuration.model.ManagedRepositoryConfiguration;
import org.apache.archiva.indexer.ArchivaIndexManager;
import org.apache.archiva.indexer.IndexCreationFailedException;
import org.apache.archiva.indexer.IndexManagerFactory;
import org.apache.archiva.repository.EditableManagedRepository;
import org.apache.archiva.repository.EditableRepository;
import org.apache.archiva.repository.ManagedRepository;
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
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Handler implementation for managed repositories.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service( "managedRepositoryHandler#default" )
public class ManagedRepositoryHandler
    extends AbstractRepositoryHandler<ManagedRepository, ManagedRepositoryConfiguration>
    implements RepositoryHandler<ManagedRepository, ManagedRepositoryConfiguration>
{
    private static final Logger log = LoggerFactory.getLogger( ManagedRepositoryHandler.class );
    private final RepositoryHandlerManager repositoryHandlerManager;
    private final RepositoryContentFactory repositoryContentFactory;


    IndexManagerFactory indexManagerFactory;


    public ManagedRepositoryHandler( RepositoryHandlerManager repositoryHandlerManager,
                                     ConfigurationHandler configurationHandler, IndexManagerFactory indexManagerFactory,
                                     @Named( "repositoryContentFactory#default" )
                                         RepositoryContentFactory repositoryContentFactory
    )
    {
        super( ManagedRepository.class, ManagedRepositoryConfiguration.class, configurationHandler );
        this.repositoryHandlerManager = repositoryHandlerManager;
        this.indexManagerFactory = indexManagerFactory;
        this.repositoryContentFactory = repositoryContentFactory;
    }

    @Override
    @PostConstruct
    public void init( )
    {
        log.debug( "Initializing repository handler " + ManagedRepositoryHandler.class );
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
        Map<String, ManagedRepository> currentInstances = new HashMap<>( getRepositories( ) );
        getRepositories().clear();
        getRepositories( ).putAll( newOrUpdateInstancesFromConfig( currentInstances ) );
        for ( ManagedRepository managedRepository : getRepositories( ).values( ) )
        {
            activateRepository( managedRepository );
        }
        for (ManagedRepository managedRepository : currentInstances.values()) {
            deactivateRepository( managedRepository );
        }

    }

    @Override
    public Map<String, ManagedRepository> newInstancesFromConfig( )
    {
        try
        {
            List<ManagedRepositoryConfiguration> managedRepoConfigs =
                new ArrayList<>(
                    getConfigurationHandler( ).getBaseConfiguration( ).getManagedRepositories( ) );

            if ( managedRepoConfigs == null )
            {
                return Collections.emptyMap( );
            }

            Map<String, ManagedRepository> result = new HashMap<>( );
            for ( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
            {
                ManagedRepository repo = newInstance( repoConfig );
                result.put( repo.getId( ), repo );
                if ( repo.supportsFeature( StagingRepositoryFeature.class ) )
                {
                    StagingRepositoryFeature stagF = repo.getFeature( StagingRepositoryFeature.class );
                    if ( stagF.getStagingRepository( ) != null )
                    {
                        ManagedRepository stagingRepo = getStagingRepository( repo );
                        if ( stagingRepo != null )
                        {
                            result.put( stagingRepo.getId( ), stagingRepo );
                        }
                    }
                }
            }
            return result;
        }
        catch ( Throwable e )
        {
            log.error( "Could not initialize repositories from config: {}", e.getMessage( ), e );
            return new HashMap<>( );
        }
    }

    public Map<String, ManagedRepository> newOrUpdateInstancesFromConfig( Map<String, ManagedRepository> currentInstances)
    {
        try
        {
            List<ManagedRepositoryConfiguration> managedRepoConfigs =
                new ArrayList<>(
                    getConfigurationHandler( ).getBaseConfiguration( ).getManagedRepositories( ) );

            if ( managedRepoConfigs == null )
            {
                return Collections.emptyMap( );
            }

            Map<String, ManagedRepository> result = new HashMap<>( );
            for ( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
            {
                String id = repoConfig.getId( );
                if (result.containsKey( id )) {
                    log.error( "There are repositories with the same id in the configuration: {}", id );
                    continue;
                }
                ManagedRepository repo;
                if ( currentInstances.containsKey( id ) )
                {
                    repo = currentInstances.remove( id );
                    getProvider( repo.getType() ).updateManagedInstance( (EditableManagedRepository) repo, repoConfig );
                    updateReferences( repo, repoConfig );
                }
                else
                {
                    repo = newInstance( repoConfig );
                }
                result.put( id, repo );
                if ( repo.supportsFeature( StagingRepositoryFeature.class ) )
                {
                    StagingRepositoryFeature stagF = repo.getFeature( StagingRepositoryFeature.class );
                    if ( stagF.getStagingRepository( ) != null )
                    {
                        String stagingId = getStagingId( id );
                        ManagedRepository stagingRepo;
                        if ( currentInstances.containsKey( stagingId ) )
                        {
                            stagingRepo = currentInstances.remove( stagingId );
                            managedRepoConfigs.stream( ).filter( cfg -> stagingId.equals( cfg.getId( ) ) ).findFirst( ).ifPresent(
                                stagingRepoConfig ->
                                {
                                    try
                                    {
                                        getProvider( stagingRepo.getType() ).updateManagedInstance( (EditableManagedRepository) stagingRepo, stagingRepoConfig );
                                        updateReferences( stagingRepo, stagingRepoConfig );
                                    }
                                    catch ( RepositoryException e )
                                    {
                                        log.error( "Could not update staging repo {}: {}", stagingId, e.getMessage( ) );
                                    }
                                }
                            );
                        }
                        else
                        {
                            stagingRepo = getStagingRepository( repo );
                        }
                        if ( stagingRepo != null )
                        {
                            result.put( stagingRepo.getId( ), stagingRepo );
                        }
                    }
                }
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
    public ManagedRepository newInstance( RepositoryType type, String id ) throws RepositoryException
    {
        log.debug( "Creating repo {}", id );
        RepositoryProvider provider = getProvider( type );
        EditableManagedRepository repo;
        try
        {
            repo = provider.createManagedInstance( id, id );
        }
        catch ( IOException e )
        {
            throw new RepositoryException( "Could not create repository '" + id + "': " + e.getMessage( ) );
        }
        repo.registerEventHandler( RepositoryEvent.ANY, repositoryHandlerManager );
        updateReferences( repo, null );
        repo.setLastState( RepositoryState.REFERENCES_SET );
        return repo;
    }

    private String getStagingId( String repoId )
    {
        return repoId + StagingRepositoryFeature.STAGING_REPO_POSTFIX;
    }


    private ManagedRepository getStagingRepository( ManagedRepository baseRepo ) throws RepositoryException
    {
        ManagedRepository stageRepo = get( getStagingId( baseRepo.getId( ) ) );
        final RepositoryType type = baseRepo.getType( );
        if ( stageRepo == null )
        {
            RepositoryProvider provider = getProvider( type );
            ManagedRepositoryConfiguration cfg = provider.getManagedConfiguration( baseRepo );
            stageRepo = provider.createStagingInstance( cfg );
            if ( stageRepo.supportsFeature( StagingRepositoryFeature.class ) )
            {
                stageRepo.getFeature( StagingRepositoryFeature.class ).setStageRepoNeeded( false );
            }
            updateReferences( stageRepo, cfg );
        }
        return stageRepo;
    }

    public ArchivaIndexManager getIndexManager( RepositoryType type )
    {
        return indexManagerFactory.getIndexManager( type );
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


    @Override
    public ManagedRepository newInstance( ManagedRepositoryConfiguration repositoryConfiguration ) throws RepositoryException
    {
        RepositoryType type = RepositoryType.valueOf( repositoryConfiguration.getType( ) );
        RepositoryProvider provider = getProvider( type );
        if ( provider == null )
        {
            throw new RepositoryException( "Provider not found for repository type: " + repositoryConfiguration.getType( ) );
        }
        final ManagedRepository repo = provider.createManagedInstance( repositoryConfiguration );
        repo.registerEventHandler( RepositoryEvent.ANY, repositoryHandlerManager );
        updateReferences( repo, null );
        if ( repo instanceof EditableRepository )
        {
            ( (EditableRepository) repo ).setLastState( RepositoryState.REFERENCES_SET );
        }
        return repo;
    }

    @Override
    protected ManagedRepositoryConfiguration findRepositoryConfiguration( final Configuration configuration, final String id )
    {
        return configuration.findManagedRepositoryById( id );
    }

    @Override
    protected void removeRepositoryConfiguration( final Configuration configuration, final ManagedRepositoryConfiguration repoConfiguration )
    {
        configuration.removeManagedRepository( repoConfiguration );
    }

    @Override
    protected void addRepositoryConfiguration( Configuration configuration, ManagedRepositoryConfiguration repoConfiguration )
    {
        configuration.addManagedRepository( repoConfiguration );
    }

    @Override
    public ManagedRepository put( ManagedRepository repository ) throws RepositoryException
    {
        final String id = repository.getId( );
        ManagedRepository originRepo = getRepositories( ).remove( id );
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
            ManagedRepositoryConfiguration newCfg = provider.getManagedConfiguration( repository );
            getConfigurationHandler( ).getLock( ).writeLock( ).lock( );
            try
            {
                Configuration configuration = getConfigurationHandler( ).getBaseConfiguration( );
                updateReferences( repository, newCfg );
                ManagedRepositoryConfiguration oldCfg = configuration.findManagedRepositoryById( id );
                if ( oldCfg != null )
                {
                    configuration.removeManagedRepository( oldCfg );
                }
                configuration.addManagedRepository( newCfg );
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
    public ManagedRepository put( ManagedRepositoryConfiguration repositoryConfiguration ) throws RepositoryException
    {
        final String id = repositoryConfiguration.getId( );
        final RepositoryType repositoryType = RepositoryType.valueOf( repositoryConfiguration.getType( ) );
        final RepositoryProvider provider = getProvider( repositoryType );
        ReentrantReadWriteLock.WriteLock configLock = this.getConfigurationHandler( ).getLock( ).writeLock( );
        configLock.lock( );
        ManagedRepository repo = null;
        ManagedRepository oldRepository = null;
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
                provider.updateManagedInstance( (EditableManagedRepository) repo, repositoryConfiguration );
                updated = true;
                pushEvent( LifecycleEvent.UPDATED, repo );
            }
            registerNewRepository( repositoryConfiguration, repo, configuration, updated );
        }
        catch ( IndeterminateConfigurationException | RegistryException e )
        {
            if ( oldRepository != null )
            {
                ManagedRepositoryConfiguration oldCfg = provider.getManagedConfiguration( oldRepository );
                provider.updateManagedInstance( (EditableManagedRepository) repo, oldCfg );
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

    @Override
    public ManagedRepository put( ManagedRepositoryConfiguration repositoryConfiguration, Configuration configuration ) throws RepositoryException
    {
        final String id = repositoryConfiguration.getId( );
        final RepositoryType repoType = RepositoryType.valueOf( repositoryConfiguration.getType( ) );
        ManagedRepository repo;
        setRepositoryDefaults( repositoryConfiguration );
        if ( getRepositories( ).containsKey( id ) )
        {
            repo = clone( getRepositories( ).get( id ), id );
            if ( repo instanceof EditableManagedRepository )
            {
                getProvider( repoType ).updateManagedInstance( (EditableManagedRepository) repo, repositoryConfiguration );
            }
            else
            {
                throw new RepositoryException( "The repository is not editable " + id );
            }
        }
        else
        {
            repo = getProvider( repoType ).createManagedInstance( repositoryConfiguration );
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

    @SuppressWarnings( "unused" )
    private void setRepositoryDefaults( ManagedRepositoryConfiguration repositoryConfiguration )
    {
        // We do nothing here
    }

    @Override
    public ManagedRepository clone( ManagedRepository repo, String id ) throws RepositoryException
    {
        RepositoryProvider provider = getProvider( repo.getType( ) );
        ManagedRepositoryConfiguration cfg = provider.getManagedConfiguration( repo );
        cfg.setId( id );
        ManagedRepository cloned = provider.createManagedInstance( cfg );
        cloned.registerEventHandler( RepositoryEvent.ANY, repositoryHandlerManager );
        setLastState( cloned, RepositoryState.CREATED );
        return cloned;
    }


    @Override
    public void updateReferences( ManagedRepository repo, ManagedRepositoryConfiguration repositoryConfiguration ) throws RepositoryException
    {
        log.debug( "Updating references of repo {}", repo.getId( ) );
        if ( repo.supportsFeature( StagingRepositoryFeature.class ) )
        {
            Configuration configuration = getConfigurationHandler( ).getBaseConfiguration( );
            RepositoryProvider provider = getProvider( repo.getType( ) );
            StagingRepositoryFeature feature = repo.getFeature( StagingRepositoryFeature.class );
            if ( feature.isStageRepoNeeded( ) && feature.getStagingRepository( ) == null )
            {
                ManagedRepository stageRepo = get( getStagingId( repo.getId( ) ) );
                if ( stageRepo == null )
                {
                    stageRepo = getStagingRepository( repo );
                    getRepositories( ).put( stageRepo.getId( ), stageRepo );
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
        repo.registerEventHandler( RepositoryEvent.ANY, repositoryHandlerManager );
    }

}
