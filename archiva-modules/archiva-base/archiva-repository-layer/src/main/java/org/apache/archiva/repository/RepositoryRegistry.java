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
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for repositories
 */
@Service("repositoryRegistry")
public class RepositoryRegistry
{
    /**
     * We inject all repository providers
     */
    @Inject
    List<RepositoryProvider> repositoryProviders;

    @Inject
    ArchivaConfiguration archivaConfiguration;

    private Map<String, ManagedRepository> managedRepositories = new HashMap<>(  );
    private Map<String, RemoteRepository> remoteRepositories = new HashMap<>(  );

    @PostConstruct
    private void initialize() {
        managedRepositories = getManagedRepositoriesFromConfig();
        remoteRepositories = getRemoteRepositoriesFromConfig();
    }

    private Map<RepositoryType, RepositoryProvider> getProviderMap() {
        Map<RepositoryType, RepositoryProvider> map = new HashMap<>(  );
        if (repositoryProviders!=null) {
            for(RepositoryProvider provider : repositoryProviders) {
                for (RepositoryType type : provider.provides()) {
                    map.put(type, provider);
                }
            }
        }
        return map;
    }

    private Map<String,ManagedRepository> getManagedRepositoriesFromConfig() {
        List<ManagedRepositoryConfiguration> managedRepoConfigs =
            getArchivaConfiguration().getConfiguration().getManagedRepositories();

        if ( managedRepoConfigs == null )
        {
            return Collections.emptyMap();
        }

        Map<String,ManagedRepository> managedRepos = new LinkedHashMap<>( managedRepoConfigs.size() );

        Map<RepositoryType, RepositoryProvider> providerMap = getProviderMap( );
        for ( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
        {
            RepositoryType repositoryType = RepositoryType.valueOf( repoConfig.getType( ) );
            if (providerMap.containsKey( repositoryType )) {
                managedRepos.put(repoConfig.getId(), providerMap.get(repositoryType).createManagedInstance( repoConfig ));
            }
        }

        return managedRepos;
    }

    private Map<String,RemoteRepository> getRemoteRepositoriesFromConfig() {
        List<RemoteRepositoryConfiguration> remoteRepoConfigs =
            getArchivaConfiguration().getConfiguration().getRemoteRepositories();

        if ( remoteRepoConfigs == null )
        {
            return Collections.emptyMap();
        }

        Map<String,RemoteRepository> remoteRepos = new LinkedHashMap<>( remoteRepoConfigs.size() );

        Map<RepositoryType, RepositoryProvider> providerMap = getProviderMap( );
        for ( RemoteRepositoryConfiguration repoConfig : remoteRepoConfigs )
        {
            RepositoryType repositoryType = RepositoryType.valueOf( repoConfig.getType( ) );
            if (providerMap.containsKey( repositoryType )) {
                remoteRepos.put(repoConfig.getId(), providerMap.get(repositoryType).createRemoteInstance( repoConfig ));
            }
        }

        return remoteRepos;
    }

    private ArchivaConfiguration getArchivaConfiguration() {
        return this.archivaConfiguration;
    }

    public List<Repository> getRepositories() {
        ArrayList<Repository> li = new ArrayList<>(  );
        li.addAll(managedRepositories.values());
        li.addAll(remoteRepositories.values());
        return Collections.unmodifiableList( li );
    }

    public List<ManagedRepository> getManagedRepositories() {
        return Collections.unmodifiableList( new ArrayList(managedRepositories.values()) );
    }

    public List<RemoteRepository> getRemoteRepositories() {
        return Collections.unmodifiableList( new ArrayList(remoteRepositories.values()) );
    }

    public Repository getRepository(String repoId) {
        return null;
    }

    public ManagedRepository getManagedRepository(String repoId) {
        return null;
    }

    public RemoteRepository getRemoteRepository(String repoId) {
        return null;
    }

}
