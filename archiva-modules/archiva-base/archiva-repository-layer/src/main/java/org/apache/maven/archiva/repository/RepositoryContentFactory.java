package org.apache.maven.archiva.repository;

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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.collections.map.UnmodifiableMap;

/**
 * RepositoryContentRequest 
 *
 * @version $Id$
 * 
 * @plexus.component 
 *      role="org.apache.maven.archiva.repository.RepositoryContentFactory"
 */
public class RepositoryContentFactory
    implements Contextualizable, RegistryListener, Initializable
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private final Map<String, ManagedRepositoryContent> managedContentMap;

    private final Map<String, RemoteRepositoryContent> remoteContentMap;

    private PlexusContainer container;

    public RepositoryContentFactory()
    {
        managedContentMap = new ConcurrentHashMap<String, ManagedRepositoryContent>();
        remoteContentMap = new ConcurrentHashMap<String, RemoteRepositoryContent>();
    }

    /**
     * Gets an unmodifiable map representing the Managed repositories
     * @return managedContentMap
     */
    public Map<String, ManagedRepositoryContent> getManagedContentMap()
    {
        if (managedContentMap.isEmpty())
        {
            for (final ManagedRepositoryConfiguration configuration : archivaConfiguration.getConfiguration().getManagedRepositories())
            {
                try
                {
                    managedContentMap.put(configuration.getId(), createManagedRepositoryContent(configuration));
                }
                catch (RepositoryException e)
                {
                    //Do nothing
                }
            }
        }

        return UnmodifiableMap.decorate(managedContentMap);
    }

    /**
     * Gets an unmodifiable map representing the Remote repositories
     * @return remoteContentMap
     */
    public Map<String, RemoteRepositoryContent> getRemoteContentMap()
    {
        if (remoteContentMap.isEmpty())
        {
            for (final RemoteRepositoryConfiguration configuration : archivaConfiguration.getConfiguration().getRemoteRepositories())
            {
                try
                {
                    remoteContentMap.put(configuration.getId(), createRemoteRepositoryContent(configuration));
                }
                catch ( RepositoryException e )
                {
                    // Do nothing
                }
            }
        }

        return UnmodifiableMap.decorate(remoteContentMap);
    }

    /**
     * Get the ManagedRepositoryContent object for the repository Id specified.
     * 
     * @param repoId the repository id to fetch.
     * @return the ManagedRepositoryContent object associated with the repository id.
     * @throws RepositoryNotFoundException if the repository id does not exist within the configuration.
     * @throws RepositoryException the repository content object cannot be loaded due to configuration issue.
     */
    public ManagedRepositoryContent getManagedRepositoryContent( String repoId )
        throws RepositoryNotFoundException, RepositoryException
    {
        ManagedRepositoryContent repo = managedContentMap.get( repoId );

        if ( repo != null )
        {
            return repo;
        }

        ManagedRepositoryConfiguration configuration = archivaConfiguration.getConfiguration().findManagedRepositoryById( repoId );
        if ( configuration == null )
        {
            throw new RepositoryNotFoundException( "Unable to find managed repository configuration for id:" + repoId );
        }

        return createManagedRepositoryContent(configuration);
    }

    public RemoteRepositoryContent getRemoteRepositoryContent( String repoId )
        throws RepositoryNotFoundException, RepositoryException
    {
        RemoteRepositoryContent repo = remoteContentMap.get( repoId );

        if ( repo != null )
        {
            return repo;
        }

        RemoteRepositoryConfiguration configuration = archivaConfiguration.getConfiguration().findRemoteRepositoryById( repoId );
        if ( configuration == null )
        {
            throw new RepositoryNotFoundException( "Unable to find remote repository configuration for id:" + repoId );
        }

        return createRemoteRepositoryContent(configuration);
    }

    private RemoteRepositoryContent createRemoteRepositoryContent(RemoteRepositoryConfiguration configuration)
        throws RepositoryException
    {
        RemoteRepositoryContent repositoryContent = null;
        try
        {
            repositoryContent = (RemoteRepositoryContent) container.lookup( RemoteRepositoryContent.class, configuration.getLayout() );
            repositoryContent.setRepository( configuration );
        }
        catch ( ComponentLookupException e )
        {
            throw new RepositoryException( "Specified layout [" + configuration.getLayout()
                + "] on remote repository id [" + configuration.getId() + "] is not valid.", e );
        }
        return repositoryContent;
    }

    private ManagedRepositoryContent createManagedRepositoryContent(ManagedRepositoryConfiguration configuration)
        throws RepositoryException
    {
        ManagedRepositoryContent repositoryContent = null;
        try
        {
            repositoryContent = (ManagedRepositoryContent) container.lookup( ManagedRepositoryContent.class, configuration.getLayout() );
            repositoryContent.setRepository( configuration );
        }
        catch ( ComponentLookupException e )
        {
            throw new RepositoryException( "Specified layout [" + configuration.getLayout()
                + "] on managed repository id [" + configuration.getId() + "] is not valid.", e );
        }
        return repositoryContent;
    }


    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( "plexus" );
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isManagedRepositories( propertyName )
            || ConfigurationNames.isRemoteRepositories( propertyName ) )
        {
            initMaps();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    public void initialize()
        throws InitializationException
    {
        archivaConfiguration.addChangeListener( this );
    }

    private void initMaps()
    {
        synchronized ( managedContentMap )
        {
            // First, return any references to the container.
            for ( ManagedRepositoryContent repo : managedContentMap.values() )
            {
                try
                {
                    container.release( repo );
                }
                catch ( ComponentLifecycleException e )
                {
                    /* ignore */
                }
            }

            // Next clear the map.
            managedContentMap.clear();
        }

        synchronized ( remoteContentMap )
        {
            // First, return any references to the container.
            for ( RemoteRepositoryContent repo : remoteContentMap.values() )
            {
                try
                {
                    container.release( repo );
                }
                catch ( ComponentLifecycleException e )
                {
                    /* ignore */
                }
            }

            // Next clear the map.
            remoteContentMap.clear();
        }
    }
}
