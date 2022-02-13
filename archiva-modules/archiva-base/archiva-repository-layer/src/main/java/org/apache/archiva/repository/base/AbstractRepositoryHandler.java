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
import org.apache.archiva.configuration.model.AbstractRepositoryConfiguration;
import org.apache.archiva.configuration.model.Configuration;
import org.apache.archiva.configuration.provider.IndeterminateConfigurationException;
import org.apache.archiva.event.Event;
import org.apache.archiva.event.BasicEventManager;
import org.apache.archiva.event.EventType;
import org.apache.archiva.repository.EditableRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryHandler;
import org.apache.archiva.repository.RepositoryProvider;
import org.apache.archiva.repository.RepositoryState;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.event.LifecycleEvent;
import org.apache.archiva.repository.validation.CheckedResult;
import org.apache.archiva.repository.validation.CombinedValidator;
import org.apache.archiva.repository.validation.RepositoryChecker;
import org.apache.archiva.repository.validation.RepositoryValidator;
import org.apache.archiva.repository.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base abstract class for repository handlers.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public abstract class AbstractRepositoryHandler<R extends Repository, C extends AbstractRepositoryConfiguration> implements RepositoryHandler<R, C>
{

    private static final Logger log = LoggerFactory.getLogger( AbstractRepositoryHandler.class );

    protected final Map<RepositoryType, RepositoryProvider> providerMap = new HashMap<>( );
    private CombinedValidator<R> combinedValidator;
    private final Class<R> repositoryClazz;
    private final Class<C> configurationClazz;
    private final BasicEventManager eventManager;
    private final Map<String, R> repositoryMap  = new HashMap<>(  );
    private final ConfigurationHandler configurationHandler;

    public AbstractRepositoryHandler(Class<R> repositoryClazz, Class<C> configurationClazz, ConfigurationHandler configurationHandler) {
        this.repositoryClazz = repositoryClazz;
        this.configurationClazz = configurationClazz;
        this.eventManager = new BasicEventManager( this );
        this.configurationHandler = configurationHandler;
    }

    protected List<RepositoryValidator<R>> initValidators( Class<R> clazz, List<RepositoryValidator<? extends Repository>> repositoryGroupValidatorList )
    {
        if ( repositoryGroupValidatorList != null && repositoryGroupValidatorList.size( ) > 0 )
        {
            return repositoryGroupValidatorList.stream( ).filter(
                v -> v.isFlavour( clazz )
            ).map( v -> v.narrowTo( clazz ) ).collect( Collectors.toList( ) );
        }
        else
        {
            return Collections.emptyList( );
        }
    }

    protected CombinedValidator<R> getCombinedValidator( Class<R> clazz, List<RepositoryValidator<? extends Repository>> repositoryGroupValidatorList )
    {
        return new CombinedValidator<>( clazz, initValidators( clazz, repositoryGroupValidatorList ) );
    }

    protected void setLastState( R repo, RepositoryState state )
    {
        RepositoryState currentState = repo.getLastState( );
        if ( repo instanceof EditableRepository )
        {
            if ( state.getOrderNumber( ) > repo.getLastState( ).getOrderNumber( ) )
            {
                ( (EditableRepository) repo ).setLastState( state );
            }
        }
        else
        {
            log.error( "Found a not editable repository instance: {}, {}", repo.getId( ), repo.getClass( ).getName( ) );
        }
        if (state == RepositoryState.REGISTERED && state != currentState ) {
            pushEvent( LifecycleEvent.REGISTERED,  repo );
        } else if (state == RepositoryState.UNREGISTERED && state != currentState) {
            pushEvent( LifecycleEvent.UNREGISTERED, repo );
        }
    }

    @Override
    public void setRepositoryProviders( List<RepositoryProvider> providers )
    {
        if ( providers != null )
        {
            for ( RepositoryProvider provider : providers )
            {
                for ( RepositoryType type : provider.provides( ) )
                {
                    providerMap.put( type, provider );
                }
            }
        }
    }

    protected RepositoryProvider getProvider(RepositoryType type) {
        return providerMap.get( type );
    }

    @Override
    public void setRepositoryValidator( List<RepositoryValidator<? extends Repository>> repositoryValidatorList )
    {
        this.combinedValidator = getCombinedValidator( repositoryClazz, repositoryValidatorList );
    }

    protected CombinedValidator<R> getCombinedValidator() {
        return this.combinedValidator;
    }

    @Override
    public Class<R> getFlavour( )
    {
        return this.repositoryClazz;
    }

    @Override
    public Class<C> getConfigurationFlavour( )
    {
        return this.configurationClazz;
    }

    @Override
    public void processOtherVariantRemoval( Repository repository )
    {
        // Default: do nothing
    }

    protected void pushEvent( Event event )
    {
        eventManager.fireEvent( event );
    }

    protected void pushEvent(EventType<? extends LifecycleEvent> event, R repo) {
        pushEvent( new LifecycleEvent( event, this, repo ) );
    }

    protected Map<String, R> getRepositories() {
        return repositoryMap;
    }

    protected ConfigurationHandler getConfigurationHandler() {
        return configurationHandler;
    }

    @Override
    public void activateRepository( R repository )
    {
        //
    }

    @Override
    public void deactivateRepository( R repository )
    {
        repository.close();
    }

    @Override
    public abstract R newInstance( C repositoryConfiguration ) throws RepositoryException;

    @Override
    public <D> CheckedResult<R, D> putWithCheck( C repositoryConfiguration, RepositoryChecker<R, D> checker ) throws RepositoryException
    {
        final String id = repositoryConfiguration.getId( );
        R currentRepository = getRepositories().get( id );
        R managedRepository = newInstance( repositoryConfiguration );
        CheckedResult<R, D> result;
        if ( currentRepository == null )
        {
            result = checker.apply( managedRepository );
        }
        else
        {
            result = checker.applyForUpdate( managedRepository );
        }
        if ( result.isValid( ) )
        {
            put( result.getRepository() );
        }
        return result;

    }

    @Override
    public CheckedResult<R, Map<String, List<ValidationError>>> putWithCheck( C repositoryConfiguration ) throws RepositoryException
    {
        return putWithCheck( repositoryConfiguration, getValidator( ) );
    }

    protected abstract C findRepositoryConfiguration( Configuration configuration, String id);

    protected abstract void removeRepositoryConfiguration(Configuration configuration, C repoConfiguration );

    protected abstract void addRepositoryConfiguration( Configuration configuration, C repoConfiguration );

    /**
     * Removes a repository group from the registry and configuration, if it exists.
     * The change is saved to the configuration immediately.
     *
     * @param id the id of the repository group to remove
     * @throws RepositoryException if an error occurs during configuration save
     */
    @Override
    public void remove( String id ) throws RepositoryException
    {
        R repo = get( id );
        if ( repo != null )
        {
            try
            {
                repo = getRepositories().remove( id );
                if ( repo != null )
                {
                    deactivateRepository( repo );
                    Configuration configuration = this.configurationHandler.getBaseConfiguration( );
                    C cfg = findRepositoryConfiguration( configuration, id );
                    if ( cfg != null )
                    {
                        removeRepositoryConfiguration( configuration, cfg );
                    }
                    this.configurationHandler.save( configuration, ConfigurationHandler.REGISTRY_EVENT_TAG );
                    setLastState( repo, RepositoryState.UNREGISTERED );
                }

            }
            catch ( RegistryException | IndeterminateConfigurationException e )
            {
                // Rollback
                log.error( "Could not save config after repository removal: {}", e.getMessage( ), e );
                getRepositories().put( repo.getId( ), repo );
                throw new RepositoryException( "Could not save configuration after repository removal: " + e.getMessage( ) );
            }
        }
    }

    @Override
    public void remove( String id, Configuration configuration ) throws RepositoryException
    {
        R repo = getRepositories().get( id );
        if ( repo != null )
        {
            repo = getRepositories().remove( id );
            if ( repo != null )
            {
                deactivateRepository( repo );
            }
            setLastState( repo, RepositoryState.UNREGISTERED );
        }
        C cfg = findRepositoryConfiguration(configuration, id );
        if ( cfg != null )
        {
            removeRepositoryConfiguration(configuration, cfg );
        }
    }

    @Override
    public R get( String groupId )
    {
        return getRepositories().get( groupId );
    }

    @Override
    public Collection<R> getAll( )
    {
        return Collections.unmodifiableCollection( getRepositories( ).values( ) );
    }

    @Override
    public RepositoryValidator<R> getValidator( )
    {
        return getCombinedValidator();
    }

    @Override
    public CheckedResult<R, Map<String, List<ValidationError>>> validateRepository( R repository )
    {
        return getCombinedValidator(  ).apply( repository );

    }

    @Override
    public CheckedResult<R,Map<String, List<ValidationError>>> validateRepositoryForUpdate( R repository )
    {
        return getCombinedValidator().applyForUpdate( repository );
    }
    @Override
    public boolean hasRepository( String id )
    {
        return getRepositories().containsKey( id );
    }


    @Override
    public void close( )
    {
        for ( R repository : getRepositories().values( ) )
        {
            try
            {
                deactivateRepository( repository );
            }
            catch ( Throwable e )
            {
                log.error( "Could not close repository {}: {}", repository.getId( ), e.getMessage( ) );
            }
        }
        getRepositories().clear( );
    }

    protected void registerNewRepository( C repositoryGroupConfiguration, R currentRepository, Configuration configuration, boolean updated ) throws IndeterminateConfigurationException, RegistryException, RepositoryException
    {
        final String id = repositoryGroupConfiguration.getId( );
        getConfigurationHandler().save( configuration, ConfigurationHandler.REGISTRY_EVENT_TAG );
        updateReferences( currentRepository, repositoryGroupConfiguration );
        if (!updated )
        {
            setLastState( currentRepository, RepositoryState.REFERENCES_SET );
        }
        activateRepository( currentRepository );
        getRepositories().put( id, currentRepository );
        setLastState( currentRepository, RepositoryState.REGISTERED );
    }



    protected void rollback( Configuration configuration, R oldRepository, Exception e, C oldCfg ) throws RepositoryException
    {
        final String id = oldRepository.getId( );
        replaceOrAddRepositoryConfig( oldCfg, configuration );
        try
        {
            getConfigurationHandler().save( configuration, ConfigurationHandler.REGISTRY_EVENT_TAG );
        }
        catch ( IndeterminateConfigurationException | RegistryException indeterminateConfigurationException )
        {
            log.error( "Fatal error, config save during rollback failed: {}", e.getMessage( ), e );
        }
        updateReferences( oldRepository, oldCfg );
        setLastState( oldRepository, RepositoryState.REFERENCES_SET );
        activateRepository( oldRepository );
        getRepositories().put( id, oldRepository );
        setLastState( oldRepository, RepositoryState.REGISTERED );
    }

    protected void replaceOrAddRepositoryConfig( C repositoryGroupConfiguration, Configuration configuration )
    {
        C oldCfg = findRepositoryConfiguration( configuration, repositoryGroupConfiguration.getId( ) );
        if ( oldCfg != null )
        {
            removeRepositoryConfiguration(configuration, oldCfg );
        }
        addRepositoryConfiguration( configuration, repositoryGroupConfiguration );
    }



}
