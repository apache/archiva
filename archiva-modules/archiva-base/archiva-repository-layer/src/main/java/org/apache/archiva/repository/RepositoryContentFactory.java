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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationNames;
import org.apache.archiva.redback.components.registry.Registry;
import org.apache.archiva.redback.components.registry.RegistryListener;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RepositoryContentRequest
 *
 *
 */
@Service( "repositoryContentFactory#default" )
public class RepositoryContentFactory
    implements RegistryListener
{
    /**
     *
     */
    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    @Inject
    private ApplicationContext applicationContext;

    private final Map<String, ManagedRepositoryContent> managedContentMap;

    private final Map<String, RemoteRepositoryContent> remoteContentMap;


    public RepositoryContentFactory()
    {
        managedContentMap = new ConcurrentHashMap<String, ManagedRepositoryContent>();
        remoteContentMap = new ConcurrentHashMap<String, RemoteRepositoryContent>();
    }

    /**
     * Get the ManagedRepositoryContent object for the repository Id specified.
     *
     * @param repoId the repository id to fetch.
     * @return the ManagedRepositoryContent object associated with the repository id.
     * @throws RepositoryNotFoundException if the repository id does not exist within the configuration.
     * @throws RepositoryException         the repository content object cannot be loaded due to configuration issue.
     */
    public ManagedRepositoryContent getManagedRepositoryContent( String repoId )
        throws RepositoryNotFoundException, RepositoryException
    {
        try
        {
            ManagedRepositoryContent repo = managedContentMap.get( repoId );

            if ( repo != null )
            {
                return repo;
            }

            ManagedRepository repoConfig = managedRepositoryAdmin.getManagedRepository( repoId );
            if ( repoConfig == null )
            {
                throw new RepositoryNotFoundException(
                    "Unable to find managed repository configuration for id:" + repoId );
            }

            repo = applicationContext.getBean( "managedRepositoryContent#" + repoConfig.getLayout(),
                                               ManagedRepositoryContent.class );
            repo.setRepository( repoConfig );
            managedContentMap.put( repoId, repo );

            return repo;
        }
        catch ( RepositoryAdminException e )
        {
            throw new RepositoryException( e.getMessage(), e );
        }
    }

    public RemoteRepositoryContent getRemoteRepositoryContent( String repoId )
        throws RepositoryNotFoundException, RepositoryException
    {
        try
        {
            RemoteRepositoryContent repo = remoteContentMap.get( repoId );

            if ( repo != null )
            {
                return repo;
            }

            RemoteRepository repoConfig = remoteRepositoryAdmin.getRemoteRepository( repoId );
            if ( repoConfig == null )
            {
                throw new RepositoryNotFoundException(
                    "Unable to find remote repository configuration for id:" + repoId );
            }

            repo = applicationContext.getBean( "remoteRepositoryContent#" + repoConfig.getLayout(),
                                               RemoteRepositoryContent.class );
            repo.setRepository( repoConfig );
            remoteContentMap.put( repoId, repo );

            return repo;
        }
        catch ( RepositoryAdminException e )
        {
            throw new RepositoryException( e.getMessage(), e );
        }
    }


    @Override
    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isManagedRepositories( propertyName ) || ConfigurationNames.isRemoteRepositories(
            propertyName ) )
        {
            initMaps();
        }
    }

    @Override
    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    @PostConstruct
    public void initialize()
    {
        archivaConfiguration.addChangeListener( this );
    }

    private void initMaps()
    {
        // olamy we use concurent so no need of synchronize
        //synchronized ( managedContentMap )
        //{
        managedContentMap.clear();
        //}

        //synchronized ( remoteContentMap )
        //{
        remoteContentMap.clear();
        //}
    }

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }
}
