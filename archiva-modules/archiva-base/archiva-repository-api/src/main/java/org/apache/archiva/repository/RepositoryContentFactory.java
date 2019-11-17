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
import org.apache.archiva.configuration.ConfigurationNames;
import org.apache.archiva.components.registry.Registry;
import org.apache.archiva.components.registry.RegistryListener;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RepositoryContentRequest
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
    private ApplicationContext applicationContext;

    @Inject
    private List<RepositoryContentProvider> repositoryContentProviders;

    private final Map<String, ManagedRepositoryContent> managedContentMap;

    private final Map<String, RemoteRepositoryContent> remoteContentMap;


    public RepositoryContentFactory( )
    {
        managedContentMap = new ConcurrentHashMap<String, ManagedRepositoryContent>( );
        remoteContentMap = new ConcurrentHashMap<String, RemoteRepositoryContent>( );
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
        throws RepositoryException
    {
        ManagedRepositoryContent repo = managedContentMap.get( repoId );

        if ( repo != null )
        {
            return repo;
        }
        else
        {
            throw new RepositoryNotFoundException(
                "Unable to find managed repository configuration for id " + repoId );
        }

    }

    private RepositoryContentProvider getProvider(final String layout, final RepositoryType repoType) throws RepositoryException
    {
        return repositoryContentProviders.stream().filter(p->p.supports( repoType ) && p.supportsLayout( layout )).
            findFirst().orElseThrow( ( ) -> new RepositoryException( "Could not find content provider for repository type "+repoType+" and layout "+layout ) );
    }

    public ManagedRepositoryContent getManagedRepositoryContent( org.apache.archiva.repository.ManagedRepository mRepo )
        throws RepositoryException
    {
        final String id = mRepo.getId();
        ManagedRepositoryContent content = managedContentMap.get( id );

        if ( content != null && content.getRepository()==mRepo)
        {
            return content;
        }

        RepositoryContentProvider contentProvider = getProvider( mRepo.getLayout( ), mRepo.getType( ) );
        content = contentProvider.createManagedContent( mRepo );
        if (content==null) {
            throw new RepositoryException( "Could not create repository content instance for "+mRepo.getId() );
        }
        ManagedRepositoryContent previousContent = managedContentMap.put( id, content );
        if (previousContent!=null) {
            previousContent.setRepository( null );
        }

        return content;
    }

    public RemoteRepositoryContent getRemoteRepositoryContent( String repoId )
        throws RepositoryException
    {
        RemoteRepositoryContent repo = remoteContentMap.get( repoId );

        if ( repo != null )
        {
            return repo;
        }
        else
        {
            throw new RepositoryNotFoundException(
                "Unable to find remote repository configuration for id:" + repoId );
        }

    }

    public RemoteRepositoryContent getRemoteRepositoryContent( RemoteRepository mRepo )
        throws RepositoryException
    {
        final String id = mRepo.getId();
        RemoteRepositoryContent content = remoteContentMap.get( id );

        if ( content != null && content.getRepository()==mRepo)
        {
            return content;
        }

        RepositoryContentProvider contentProvider = getProvider( mRepo.getLayout( ), mRepo.getType( ) );
        content = contentProvider.createRemoteContent( mRepo );
        if (content==null) {
            throw new RepositoryException( "Could not create repository content instance for "+mRepo.getId() );
        }
        RemoteRepositoryContent previousContent = remoteContentMap.put( id, content );
        if (previousContent!=null) {
            previousContent.setRepository( null );
        }
        return content;
    }


    @Override
    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isManagedRepositories( propertyName ) || ConfigurationNames.isRemoteRepositories(
            propertyName ) )
        {
            initMaps( );
        }
    }

    @Override
    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    @PostConstruct
    public void initialize( )
    {
        archivaConfiguration.addChangeListener( this );
    }

    private void initMaps( )
    {
        // olamy we use concurent so no need of synchronize
        //synchronized ( managedContentMap )
        //{
        managedContentMap.clear( );
        //}

        //synchronized ( remoteContentMap )
        //{
        remoteContentMap.clear( );
        //}
    }

    public ArchivaConfiguration getArchivaConfiguration( )
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public void setRepositoryContentProviders(List<RepositoryContentProvider> providers) {
        this.repositoryContentProviders = providers;
    }

    public List<RepositoryContentProvider> getRepositoryContentProviders() {
        return repositoryContentProviders;
    }
}
