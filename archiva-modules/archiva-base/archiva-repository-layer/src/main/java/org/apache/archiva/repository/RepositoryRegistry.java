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

import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registry for repositories
 */
@Service( "repositoryRegistry" )
public class RepositoryRegistry
{

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
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
    }

    private Map<RepositoryType, RepositoryProvider> getProviderMap( )
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

    private Map<String, ManagedRepository> getManagedRepositoriesFromConfig( )
    {
        List<ManagedRepositoryConfiguration> managedRepoConfigs =
            getArchivaConfiguration( ).getConfiguration( ).getManagedRepositories( );

        if ( managedRepoConfigs == null )
        {
            return Collections.emptyMap( );
        }

        Map<String, ManagedRepository> managedRepos = new LinkedHashMap<>( managedRepoConfigs.size( ) );

        Map<RepositoryType, RepositoryProvider> providerMap = getProviderMap( );
        for ( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
        {
            RepositoryType repositoryType = RepositoryType.valueOf( repoConfig.getType( ) );
            if ( providerMap.containsKey( repositoryType ) )
            {
                try
                {
                    ManagedRepository repo = createNewManagedRepository( providerMap.get( repositoryType ), repoConfig );
                    managedRepos.put(repo.getId(), repo);
                }
                catch ( Exception e )
                {
                    log.error( "Could not create managed repository {}: {}", repoConfig.getId( ), e.getMessage( ), e );
                }
            }
        }
        return managedRepos;
    }

    private ManagedRepository createNewManagedRepository( RepositoryProvider provider, ManagedRepositoryConfiguration cfg ) throws RepositoryException
    {
        ManagedRepository repo = provider.createManagedInstance( cfg );
        if ( repo.supportsFeature( StagingRepositoryFeature.class ) )
        {
            StagingRepositoryFeature feature = repo.getFeature( StagingRepositoryFeature.class ).get( );
            if ( feature.isStageRepoNeeded( ) )
            {
                ManagedRepository stageRepo = getStageRepository( provider, cfg );
                feature.setStagingRepository( stageRepo );
            }
        }
        if ( repo instanceof EditableManagedRepository )
        {
            ( (EditableManagedRepository) repo ).setContent( repositoryContentFactory.getManagedRepositoryContent( repo.getId( ) ) );
        }
        return repo;

    }

    private ManagedRepository getStageRepository( RepositoryProvider provider, ManagedRepositoryConfiguration baseRepoCfg ) throws RepositoryException
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
        List<RemoteRepositoryConfiguration> remoteRepoConfigs =
            getArchivaConfiguration( ).getConfiguration( ).getRemoteRepositories( );

        if ( remoteRepoConfigs == null )
        {
            return Collections.emptyMap( );
        }

        Map<String, RemoteRepository> remoteRepos = new LinkedHashMap<>( remoteRepoConfigs.size( ) );

        Map<RepositoryType, RepositoryProvider> providerMap = getProviderMap( );
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
    }

    private ArchivaConfiguration getArchivaConfiguration( )
    {
        return this.archivaConfiguration;
    }

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

    public Collection<ManagedRepository> getManagedRepositories( )
    {
        return uManagedRepository.values( );
    }

    public Collection<RemoteRepository> getRemoteRepositories( )
    {
        return uRemoteRepositories.values( );
    }

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

    public ManagedRepository getManagedRepository( String repoId )
    {
        rwLock.readLock().lock();
        try
        {
            return managedRepositories.get( repoId );
        } finally
        {
            rwLock.readLock().unlock();
        }
    }

    public RemoteRepository getRemoteRepository( String repoId )
    {
        rwLock.readLock().lock();
        try
        {
            return remoteRepositories.get( repoId );
        } finally
        {
            rwLock.readLock().unlock();
        }
    }

    public void addRepository( ManagedRepository managedRepository )
    {
        rwLock.writeLock( ).lock( );
        try
        {
            managedRepositories.put( managedRepository.getId( ), managedRepository );
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
    }

    public void addRepository( RemoteRepository remoteRepository )
    {
        rwLock.writeLock( ).lock( );
        try
        {
            remoteRepositories.put( remoteRepository.getId( ), remoteRepository );
        }
        finally
        {
            rwLock.writeLock( ).unlock( );
        }
    }

}
