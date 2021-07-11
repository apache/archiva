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

import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryHandler;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.base.AbstractRepositoryHandler;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.archiva.repository.validation.CheckedResult;
import org.apache.archiva.repository.validation.RepositoryChecker;
import org.apache.archiva.repository.validation.RepositoryValidator;
import org.apache.archiva.repository.validation.ValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handler implementation for managed repositories.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ManagedRepositoryHandler
    extends AbstractRepositoryHandler<ManagedRepository, ManagedRepositoryConfiguration>
implements RepositoryHandler<ManagedRepository, ManagedRepositoryConfiguration>
{
    private static final Logger log = LoggerFactory.getLogger( ManagedRepositoryHandler.class );
    private final ConfigurationHandler configurationHandler;
    private final ArchivaRepositoryRegistry repositoryRegistry;
    private final RepositoryValidator<ManagedRepository> validator;
    private Map<String, ManagedRepository> managedRepositories = new HashMap<>(  );
    private Map<String, ManagedRepository> uManagedRepositories = Collections.unmodifiableMap( managedRepositories );


    public ManagedRepositoryHandler( ArchivaRepositoryRegistry repositoryRegistry,
                                     ConfigurationHandler configurationHandler,
                                     List<RepositoryValidator<? extends Repository>> repositoryValidatorList )
    {
        this.configurationHandler = configurationHandler;
        this.repositoryRegistry = repositoryRegistry;
        this.validator = getCombinedValidatdor( ManagedRepository.class, repositoryValidatorList );
    }

    @Override
    public void initializeFromConfig( )
    {
        this.managedRepositories.clear( );
        this.managedRepositories.putAll( newInstancesFromConfig( ) );
        for ( ManagedRepository managedRepository : this.managedRepositories.values( ) )
        {
            activateRepository( managedRepository );
        }

    }

    @Override
    public void activateRepository( ManagedRepository repository )
    {

    }

    @Override
    public Map<String, ManagedRepository> newInstancesFromConfig( )
    {
        try
        {
            Set<String> configRepoIds = new HashSet<>( );
            List<ManagedRepositoryConfiguration> managedRepoConfigs =
                configurationHandler.getBaseConfiguration( ).getManagedRepositories( );

            if ( managedRepoConfigs == null )
            {
                return managedRepositories;
            }

            for ( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
            {
                ManagedRepository repo = put( repoConfig, null );
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
            return managedRepositories;
        }
        return managedRepositories;
    }

    @Override
    public ManagedRepository newInstance( RepositoryType type, String id ) throws RepositoryException
    {
        return null;
    }

    @Override
    public ManagedRepository newInstance( ManagedRepositoryConfiguration repositoryConfiguration ) throws RepositoryException
    {
        return null;
    }

    @Override
    public ManagedRepository put( ManagedRepository repository ) throws RepositoryException
    {
        return null;
    }

    @Override
    public ManagedRepository put( ManagedRepositoryConfiguration repositoryConfiguration ) throws RepositoryException
    {
        return null;
    }

    @Override
    public ManagedRepository put( ManagedRepositoryConfiguration repositoryConfiguration, Configuration configuration ) throws RepositoryException
    {
        return null;
    }

    @Override
    public <D> CheckedResult<ManagedRepository, D> putWithCheck( ManagedRepositoryConfiguration repositoryConfiguration, RepositoryChecker<ManagedRepository, D> checker ) throws RepositoryException
    {
        return null;
    }

    @Override
    public void remove( String id ) throws RepositoryException
    {

    }

    @Override
    public void remove( String id, Configuration configuration ) throws RepositoryException
    {

    }

    @Override
    public ManagedRepository get( String id )
    {
        return null;
    }

    @Override
    public ManagedRepository clone( ManagedRepository repo ) throws RepositoryException
    {
        return null;
    }

    @Override
    public void updateReferences( ManagedRepository repo, ManagedRepositoryConfiguration repositoryConfiguration ) throws RepositoryException
    {

    }

    @Override
    public Collection<ManagedRepository> getAll( )
    {
        return null;
    }

    @Override
    public RepositoryValidator<ManagedRepository> getValidator( )
    {
        return null;
    }

    @Override
    public ValidationResponse<ManagedRepository> validateRepository( ManagedRepository repository )
    {
        return null;
    }

    @Override
    public ValidationResponse<ManagedRepository> validateRepositoryForUpdate( ManagedRepository repository )
    {
        return null;
    }

    @Override
    public boolean hasRepository( String id )
    {
        return false;
    }

    @Override
    public void init( )
    {

    }

    @Override
    public void close( )
    {

    }
}
