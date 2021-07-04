package org.apache.archiva.repository.base.group;
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
import org.apache.archiva.repository.RepositoryState;
import org.apache.archiva.repository.base.AbstractRepositoryHandler;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.validation.CheckedResult;
import org.apache.archiva.repository.EditableRepository;
import org.apache.archiva.repository.EditableRepositoryGroup;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.RepositoryHandler;
import org.apache.archiva.repository.RepositoryProvider;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.event.RepositoryEvent;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.validation.RepositoryChecker;
import org.apache.archiva.repository.validation.RepositoryValidator;
import org.apache.archiva.repository.validation.ValidationError;
import org.apache.archiva.repository.validation.ValidationResponse;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static org.apache.archiva.indexer.ArchivaIndexManager.DEFAULT_INDEX_PATH;

/**
 * This class manages repository groups for the RepositoryRegistry.
 * It is tightly coupled with the {@link ArchivaRepositoryRegistry}.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service( "repositoryGroupHandler#default" )
public class RepositoryGroupHandler
    extends AbstractRepositoryHandler<RepositoryGroup, RepositoryGroupConfiguration>
    implements RepositoryHandler<RepositoryGroup, RepositoryGroupConfiguration>
{
    private static final Logger log = LoggerFactory.getLogger( RepositoryGroupHandler.class );

    private final ArchivaRepositoryRegistry repositoryRegistry;
    private final ConfigurationHandler configurationHandler;
    private final MergedRemoteIndexesScheduler mergedRemoteIndexesScheduler;

    private final Map<String, RepositoryGroup> repositoryGroups = new HashMap<>( );
    private final RepositoryValidator<RepositoryGroup> validator;

    private Path groupsDirectory;


    /**
     * Creates a new instance. All dependencies are injected on the constructor.
     *
     * @param repositoryRegistry           the registry. To avoid circular dependencies via DI, this class registers itself on the registry.
     * @param configurationHandler         the configuration handler is used to retrieve and save configuration.
     * @param mergedRemoteIndexesScheduler the index scheduler is used for merging the indexes from all group members
     * @param repositoryValidatorList the list of validators that are registered
     */
    public RepositoryGroupHandler( ArchivaRepositoryRegistry repositoryRegistry,
                                   ConfigurationHandler configurationHandler,
                                   @Named( "mergedRemoteIndexesScheduler#default" ) MergedRemoteIndexesScheduler mergedRemoteIndexesScheduler,
                                   List<RepositoryValidator<? extends Repository>> repositoryValidatorList
                                   )
    {
        this.configurationHandler = configurationHandler;
        this.mergedRemoteIndexesScheduler = mergedRemoteIndexesScheduler;
        this.repositoryRegistry = repositoryRegistry;
        this.validator = getCombinedValidatdor( RepositoryGroup.class, repositoryValidatorList );
    }

    @Override
    @PostConstruct
    public void init( )
    {
        log.debug( "Initializing repository group handler " + repositoryRegistry.toString( ) );
        initializeStorage( );
        // We are registering this class on the registry. This is necessary to avoid circular dependencies via injection.
        this.repositoryRegistry.registerGroupHandler( this );
    }

    @Override
    public void initializeFromConfig( )
    {
        this.repositoryGroups.clear( );
        this.repositoryGroups.putAll( newInstancesFromConfig( ) );
        for ( RepositoryGroup group : this.repositoryGroups.values( ) )
        {
            initialize( group );
        }
    }

    private void initializeStorage( )
    {
        Path baseDir = this.configurationHandler.getArchivaConfiguration( ).getRepositoryGroupBaseDir( );
        if ( !Files.exists( baseDir ) )
        {
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

    @Override
    public void initialize( RepositoryGroup repositoryGroup )
    {
        StorageAsset indexDirectory = getMergedIndexDirectory( repositoryGroup );
        if ( !indexDirectory.exists( ) )
        {
            try
            {
                indexDirectory.create( );
            }
            catch ( IOException e )
            {
                log.error( "Could not create index directory {} for group {}: {}", indexDirectory, repositoryGroup.getId( ), e.getMessage( ) );
            }
        }
        Path groupPath = groupsDirectory.resolve( repositoryGroup.getId( ) );
        if ( !Files.exists( groupPath ) )
        {
            try
            {
                Files.createDirectories( groupPath );
            }
            catch ( IOException e )
            {
                log.error( "Could not create repository group directory {}", groupPath );
            }
        }
        mergedRemoteIndexesScheduler.schedule( repositoryGroup,
            indexDirectory );
        setLastState( repositoryGroup, RepositoryState.INITIALIZED );
    }

    public StorageAsset getMergedIndexDirectory( RepositoryGroup group )
    {
        if ( group != null )
        {
            return group.getFeature( IndexCreationFeature.class ).get( ).getLocalIndexPath( );
        }
        else
        {
            return null;
        }
    }


    @Override
    public Map<String, RepositoryGroup> newInstancesFromConfig( )
    {
        try
        {
            List<RepositoryGroupConfiguration> repositoryGroupConfigurations =
                this.configurationHandler.getBaseConfiguration( ).getRepositoryGroups( );

            if ( repositoryGroupConfigurations == null )
            {
                return Collections.emptyMap( );
            }

            Map<String, RepositoryGroup> repositoryGroupMap = new LinkedHashMap<>( repositoryGroupConfigurations.size( ) );

            Map<RepositoryType, RepositoryProvider> providerMap = repositoryRegistry.getRepositoryProviderMap( );
            for ( RepositoryGroupConfiguration repoConfig : repositoryGroupConfigurations )
            {
                RepositoryType repositoryType = RepositoryType.valueOf( repoConfig.getType( ) );
                if ( providerMap.containsKey( repositoryType ) )
                {
                    try
                    {
                        RepositoryGroup repo = createNewRepositoryGroup( providerMap.get( repositoryType ), repoConfig );
                        repositoryGroupMap.put( repo.getId( ), repo );
                    }
                    catch ( Exception e )
                    {
                        log.error( "Could not create repository group {}: {}", repoConfig.getId( ), e.getMessage( ), e );
                    }
                }
            }
            return repositoryGroupMap;
        }
        catch ( Throwable e )
        {
            log.error( "Could not initialize repositories from config: {}", e.getMessage( ), e );
            return Collections.emptyMap( );
        }
    }

    @Override
    public RepositoryGroup newInstance( final RepositoryType type, String id ) throws RepositoryException
    {
        RepositoryProvider provider = repositoryRegistry.getProvider( type );
        RepositoryGroupConfiguration config = new RepositoryGroupConfiguration( );
        config.setId( id );
        return createNewRepositoryGroup( provider, config );
    }

    @Override
    public RepositoryGroup newInstance( final RepositoryGroupConfiguration repositoryConfiguration ) throws RepositoryException
    {
        RepositoryType type = RepositoryType.valueOf( repositoryConfiguration.getType( ) );
        RepositoryProvider provider = repositoryRegistry.getProvider( type );
        return createNewRepositoryGroup( provider, repositoryConfiguration );
    }

    private RepositoryGroup createNewRepositoryGroup( RepositoryProvider provider, RepositoryGroupConfiguration config ) throws RepositoryException
    {
        RepositoryGroup repositoryGroup = provider.createRepositoryGroup( config );
        updateReferences( repositoryGroup, config );
        if (repositoryGroup instanceof EditableRepository)
        {
            ( (EditableRepository) repositoryGroup ).setLastState( RepositoryState.REFERENCES_SET );
        }
        return repositoryGroup;
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
    public RepositoryGroup put( final RepositoryGroup repositoryGroup ) throws RepositoryException
    {
        final String id = repositoryGroup.getId( );
        RepositoryGroup originRepoGroup = repositoryGroups.remove( id );
        try
        {
            if ( originRepoGroup != null && originRepoGroup != repositoryGroup )
            {
                this.mergedRemoteIndexesScheduler.unschedule( originRepoGroup );
                originRepoGroup.close( );
            }
            RepositoryProvider provider = repositoryRegistry.getProvider( repositoryGroup.getType( ) );
            RepositoryGroupConfiguration newCfg = provider.getRepositoryGroupConfiguration( repositoryGroup );
            ReentrantReadWriteLock.WriteLock configLock = this.configurationHandler.getLock( ).writeLock( );
            configLock.lock( );
            try
            {
                Configuration configuration = this.configurationHandler.getBaseConfiguration( );
                updateReferences( repositoryGroup, newCfg );
                RepositoryGroupConfiguration oldCfg = configuration.findRepositoryGroupById( id );
                if ( oldCfg != null )
                {
                    configuration.removeRepositoryGroup( oldCfg );
                }
                configuration.addRepositoryGroup( newCfg );
                configurationHandler.save( configuration, ConfigurationHandler.REGISTRY_EVENT_TAG );
                setLastState( repositoryGroup, RepositoryState.SAVED );
                initialize( repositoryGroup );
            }
            finally
            {
                configLock.unlock( );
            }
            repositoryGroups.put( id, repositoryGroup );
            setLastState( repositoryGroup, RepositoryState.REGISTERED );
            return repositoryGroup;
        }
        catch ( Exception e )
        {
            // Rollback
            if ( originRepoGroup != null )
            {
                repositoryGroups.put( id, originRepoGroup );
            }
            else
            {
                repositoryGroups.remove( id );
            }
            log.error( "Exception during configuration update {}", e.getMessage( ), e );
            throw new RepositoryException( "Could not save the configuration" + ( e.getMessage( ) == null ? "" : ": " + e.getMessage( ) ), e);
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
    public RepositoryGroup put( RepositoryGroupConfiguration repositoryGroupConfiguration ) throws RepositoryException
    {
        final String id = repositoryGroupConfiguration.getId( );
        final RepositoryType repositoryType = RepositoryType.valueOf( repositoryGroupConfiguration.getType( ) );
        final RepositoryProvider provider = repositoryRegistry.getProvider( repositoryType );
        RepositoryGroup currentRepository;
        ReentrantReadWriteLock.WriteLock configLock = this.configurationHandler.getLock( ).writeLock( );
        configLock.lock( );
        try
        {
            Configuration configuration = this.configurationHandler.getBaseConfiguration( );
            currentRepository = repositoryRegistry.getRepositoryGroup( id );
            RepositoryGroup oldRepository = currentRepository == null ? null : clone( currentRepository );
            try
            {

                if (currentRepository==null) {
                    currentRepository = put( repositoryGroupConfiguration, configuration );
                } else
                {
                    setRepositoryGroupDefaults( repositoryGroupConfiguration );
                    provider.updateRepositoryGroupInstance( (EditableRepositoryGroup) currentRepository, repositoryGroupConfiguration );
                }
                configurationHandler.save( configuration, ConfigurationHandler.REGISTRY_EVENT_TAG );
                updateReferences( currentRepository, repositoryGroupConfiguration );
                setLastState( currentRepository, RepositoryState.REFERENCES_SET );
                initialize( currentRepository );
                this.repositoryGroups.put( id, currentRepository );
                setLastState( currentRepository, RepositoryState.REGISTERED );
            }
            catch ( IndeterminateConfigurationException | RegistryException | RepositoryException e )
            {
                // Trying a rollback
                if ( oldRepository != null  )
                {
                    RepositoryGroupConfiguration oldCfg = provider.getRepositoryGroupConfiguration( oldRepository );
                    provider.updateRepositoryGroupInstance( (EditableRepositoryGroup) currentRepository, oldCfg);
                    replaceOrAddRepositoryConfig( oldCfg, configuration );
                    try
                    {
                        configurationHandler.save( configuration, ConfigurationHandler.REGISTRY_EVENT_TAG );
                    }
                    catch ( IndeterminateConfigurationException | RegistryException indeterminateConfigurationException )
                    {
                        log.error( "Fatal error, config save during rollback failed: {}", e.getMessage( ), e );
                    }
                    updateReferences( oldRepository, oldCfg  );
                    setLastState( oldRepository, RepositoryState.REFERENCES_SET );
                    initialize( oldRepository );
                    repositoryGroups.put( id, oldRepository );
                    setLastState( oldRepository, RepositoryState.REGISTERED );
                } else {
                    repositoryGroups.remove( id );
                }
                log.error( "Could not save the configuration for repository group {}: {}", id, e.getMessage( ), e );
                if (e instanceof RepositoryException) {
                    throw (RepositoryException) e;
                } else
                {
                    throw new RepositoryException( "Could not save the configuration for repository group " + id + ": " + e.getMessage( ) );
                }
            }
        }
        finally
        {
            configLock.unlock( );
        }
        return currentRepository;
    }

    @Override
    public RepositoryGroup put( RepositoryGroupConfiguration repositoryGroupConfiguration, Configuration configuration ) throws RepositoryException
    {
        final String id = repositoryGroupConfiguration.getId( );
        final RepositoryType repoType = RepositoryType.valueOf( repositoryGroupConfiguration.getType( ) );
        RepositoryGroup repo;
        setRepositoryGroupDefaults( repositoryGroupConfiguration );
        if ( repositoryGroups.containsKey( id ) )
        {
            repo = clone( repositoryGroups.get( id ) );
            if ( repo instanceof EditableRepositoryGroup )
            {
                repositoryRegistry.getProvider( repoType ).updateRepositoryGroupInstance( (EditableRepositoryGroup) repo, repositoryGroupConfiguration );
            }
            else
            {
                throw new RepositoryException( "The repository is not editable " + id );
            }
        }
        else
        {
            repo = repositoryRegistry.getProvider( repoType ).createRepositoryGroup( repositoryGroupConfiguration );
            setLastState( repo, RepositoryState.CREATED );
        }
        replaceOrAddRepositoryConfig( repositoryGroupConfiguration, configuration );
        updateReferences( repo, repositoryGroupConfiguration );
        setLastState( repo, RepositoryState.REFERENCES_SET );
        return repo;
    }

    @Override
    public <D> CheckedResult<RepositoryGroup, D> putWithCheck( RepositoryGroupConfiguration repositoryConfiguration, RepositoryChecker<RepositoryGroup, D> checker ) throws RepositoryException
    {
        final String id = repositoryConfiguration.getId( );
        RepositoryGroup currentGroup = repositoryGroups.get( id );
        Configuration configuration = configurationHandler.getBaseConfiguration( );
        RepositoryGroup repositoryGroup = put( repositoryConfiguration, configuration );
        CheckedResult<RepositoryGroup, D> result;
        if ( currentGroup == null )
        {
            result = checker.apply( repositoryGroup );
        }
        else
        {
            result = checker.applyForUpdate( repositoryGroup );
        }
        if ( result.isValid( ) )
        {
            put( result.getRepository() );
        }
        return result;
    }


    private void setRepositoryGroupDefaults( RepositoryGroupConfiguration repositoryGroupConfiguration )
    {
        if ( StringUtils.isEmpty( repositoryGroupConfiguration.getMergedIndexPath( ) ) )
        {
            repositoryGroupConfiguration.setMergedIndexPath( DEFAULT_INDEX_PATH );
        }
        if ( repositoryGroupConfiguration.getMergedIndexTtl( ) <= 0 )
        {
            repositoryGroupConfiguration.setMergedIndexTtl( 300 );
        }
        if ( StringUtils.isEmpty( repositoryGroupConfiguration.getCronExpression( ) ) )
        {
            repositoryGroupConfiguration.setCronExpression( "0 0 03 ? * MON" );
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

    public void removeRepositoryFromGroups( ManagedRepository repo )
    {
        if ( repo != null )
        {
            repositoryGroups.values( ).stream( ).filter( repoGroup -> repoGroup instanceof EditableRepository ).
                map( repoGroup -> (EditableRepositoryGroup) repoGroup ).forEach( repoGroup -> repoGroup.removeRepository( repo ) );
        }
    }

    /**
     * Removes a repository group from the registry and configuration, if it exists.
     * The change is saved to the configuration immediately.
     *
     * @param id the id of the repository group to remove
     * @throws RepositoryException if a error occurs during configuration save
     */
    @Override
    public void remove( final String id ) throws RepositoryException
    {
        RepositoryGroup repo = get( id );
        if ( repo != null )
        {
            try
            {
                repo = repositoryGroups.remove( id );
                if ( repo != null )
                {
                    this.mergedRemoteIndexesScheduler.unschedule( repo );
                    repo.close( );
                    Configuration configuration = this.configurationHandler.getBaseConfiguration( );
                    RepositoryGroupConfiguration cfg = configuration.findRepositoryGroupById( id );
                    if ( cfg != null )
                    {
                        configuration.removeRepositoryGroup( cfg );
                    }
                    this.configurationHandler.save( configuration, ConfigurationHandler.REGISTRY_EVENT_TAG );
                    setLastState( repo, RepositoryState.UNREGISTERED );
                }

            }
            catch ( RegistryException | IndeterminateConfigurationException e )
            {
                // Rollback
                log.error( "Could not save config after repository removal: {}", e.getMessage( ), e );
                repositoryGroups.put( repo.getId( ), repo );
                throw new RepositoryException( "Could not save configuration after repository removal: " + e.getMessage( ) );
            }
        }
    }

    @Override
    public void remove( String id, Configuration configuration ) throws RepositoryException
    {
        RepositoryGroup repo = repositoryGroups.get( id );
        if ( repo != null )
        {
            repo = repositoryGroups.remove( id );
            if ( repo != null )
            {
                this.mergedRemoteIndexesScheduler.unschedule( repo );
                repo.close( );
                RepositoryGroupConfiguration cfg = configuration.findRepositoryGroupById( id );
                if ( cfg != null )
                {
                    configuration.removeRepositoryGroup( cfg );
                }
                setLastState( repo, RepositoryState.UNREGISTERED );
            }
        }

    }

    @Override
    public RepositoryGroup get( String groupId )
    {
        return repositoryGroups.get( groupId );
    }

    @Override
    public RepositoryGroup clone( RepositoryGroup repo ) throws RepositoryException
    {
        RepositoryProvider provider = repositoryRegistry.getProvider( repo.getType( ) );
        RepositoryGroupConfiguration cfg = provider.getRepositoryGroupConfiguration( repo );
        RepositoryGroup cloned = provider.createRepositoryGroup( cfg );
        cloned.registerEventHandler( RepositoryEvent.ANY, repositoryRegistry );
        setLastState( cloned, RepositoryState.CREATED );
        return cloned;
    }

    @Override
    public void updateReferences( RepositoryGroup repo, RepositoryGroupConfiguration repositoryConfiguration ) throws RepositoryException
    {
        if ( repo instanceof EditableRepositoryGroup && repositoryConfiguration!=null)
        {
            EditableRepositoryGroup eGroup = (EditableRepositoryGroup) repo;
            eGroup.setRepositories( repositoryConfiguration.getRepositories( ).stream( )
                .map( repositoryRegistry::getManagedRepository ).collect( Collectors.toList( ) ) );
        }

    }

    @Override
    public Collection<RepositoryGroup> getAll( )
    {
        return repositoryGroups.values( );
    }

    @Override
    public RepositoryValidator<RepositoryGroup> getValidator( )
    {
        return this.validator;
    }

    @Override
    public CheckedResult<RepositoryGroup, Map<String, List<ValidationError>>> validateRepository( RepositoryGroup repository )
    {
        return this.validator.apply( repository );

    }

    @Override
    public CheckedResult<RepositoryGroup,Map<String, List<ValidationError>>> validateRepositoryForUpdate( RepositoryGroup repository )
    {
        return this.validator.applyForUpdate( repository );
    }
    @Override
    public boolean has( String id )
    {
        return repositoryGroups.containsKey( id );
    }

    @PreDestroy
    private void destroy( )
    {
        this.close( );
    }

    @Override
    public void close( )
    {
        for ( RepositoryGroup group : repositoryGroups.values( ) )
        {
            try
            {
                mergedRemoteIndexesScheduler.unschedule( group );
                group.close( );
            }
            catch ( Throwable e )
            {
                log.error( "Could not close repository group {}: {}", group.getId( ), e.getMessage( ) );
            }
        }
        this.repositoryGroups.clear( );
    }

}
