package org.apache.maven.repository.proxy;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.configuration.ConfigurationStore;
import org.apache.maven.repository.configuration.ConfigurationStoreException;
import org.apache.maven.repository.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.repository.configuration.ProxiedRepositoryConfiguration;
import org.apache.maven.repository.configuration.Proxy;
import org.apache.maven.repository.configuration.RepositoryConfiguration;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of the proxy manager that bridges the repository configuration classes to the proxy API. This
 * class is not thread safe (due to the request handler being a non-thread safe requirement).
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo we should be able to configure "views" that sit in front of this (ie, prefix = /legacy, appears as layout maven-1.x, path gets translated before being passed on)
 * @plexus.component instantiation-strategy="per-lookup"
 */
public class DefaultProxyManager
    implements ProxyManager
{
    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    /**
     * @plexus.requirement role="org.apache.maven.repository.proxy.ProxyRequestHandler"
     * @todo seems to be a bug in qdox that the role above is required
     */
    private ProxyRequestHandler requestHandler;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory repositoryFactory;

    /**
     * The proxy groups for each managed repository.
     */
    private static Map/*<String,ProxiedRepositoryGroup>*/ proxyGroups;

    /**
     * The default proxy group/managed repository.
     */
    private static ProxiedRepositoryGroup defaultProxyGroup;

    public File get( String path )
        throws ProxyException, ResourceDoesNotExistException
    {
        assert path.startsWith( "/" );

        Map groups = getProxyGroups();

        ProxiedRepositoryGroup proxyGroup = parseRepositoryId( path, groups );

        String repositoryPath = path;
        if ( proxyGroup == null )
        {
            if ( defaultProxyGroup != null )
            {
                proxyGroup = defaultProxyGroup;
            }
            else
            {
                throw new ResourceDoesNotExistException( "No repositories exist under the path: " + path );
            }
        }
        else
        {
            repositoryPath = repositoryPath.substring( proxyGroup.getManagedRepository().getId().length() + 2 );
        }

        return requestHandler.get( repositoryPath, proxyGroup.getProxiedRepositories(),
                                   proxyGroup.getManagedRepository(), proxyGroup.getWagonProxy() );
    }

    public File getAlways( String path )
        throws ProxyException, ResourceDoesNotExistException
    {
        assert path.startsWith( "/" );

        Map groups = getProxyGroups();

        ProxiedRepositoryGroup proxyGroup = parseRepositoryId( path, groups );

        String repositoryPath = path;
        if ( proxyGroup == null )
        {
            if ( defaultProxyGroup != null )
            {
                proxyGroup = defaultProxyGroup;
            }
            else
            {
                throw new ResourceDoesNotExistException( "No repositories exist under the path: " + path );
            }
        }
        else
        {
            repositoryPath = repositoryPath.substring( proxyGroup.getManagedRepository().getId().length() + 2 );
        }

        return requestHandler.getAlways( repositoryPath, proxyGroup.getProxiedRepositories(),
                                         proxyGroup.getManagedRepository(), proxyGroup.getWagonProxy() );
    }

    private Configuration getConfiguration()
        throws ProxyException
    {
        Configuration configuration;
        try
        {
            configuration = configurationStore.getConfigurationFromStore();
        }
        catch ( ConfigurationStoreException e )
        {
            throw new ProxyException( "Error reading configuration, unable to proxy any requests: " + e.getMessage(),
                                      e );
        }
        return configuration;
    }

    private Map getProxyGroups()
        throws ProxyException
    {
        if ( proxyGroups == null )
        {
            Map groups = new HashMap();

            Configuration configuration = getConfiguration();

            ProxyInfo wagonProxy = createWagonProxy( configuration.getProxy() );

            for ( Iterator i = configuration.getRepositories().iterator(); i.hasNext(); )
            {
                RepositoryConfiguration repository = (RepositoryConfiguration) i.next();
                ArtifactRepository managedRepository = repositoryFactory.createRepository( repository );
                List proxiedRepositories = getProxiedRepositoriesForManagedRepository(
                    configuration.getProxiedRepositories(), repository.getId() );

                groups.put( repository.getId(),
                            new ProxiedRepositoryGroup( proxiedRepositories, managedRepository, wagonProxy ) );
            }

            // TODO: ability to configure default proxy separately

            if ( groups.size() == 1 )
            {
                defaultProxyGroup = (ProxiedRepositoryGroup) groups.values().iterator().next();
            }

            proxyGroups = groups;
        }
        return proxyGroups;
    }

    private List getProxiedRepositoriesForManagedRepository( List proxiedRepositories, String id )
    {
        List repositories = new ArrayList();
        for ( Iterator i = proxiedRepositories.iterator(); i.hasNext(); )
        {
            ProxiedRepositoryConfiguration config = (ProxiedRepositoryConfiguration) i.next();

            if ( config.getManagedRepository().equals( id ) )
            {
                repositories.add( repositoryFactory.createProxiedRepository( config ) );
            }
        }
        return repositories;
    }

    private static ProxiedRepositoryGroup parseRepositoryId( String path, Map groups )
        throws ProxyException, ResourceDoesNotExistException
    {
        ProxiedRepositoryGroup group = null;

        for ( Iterator i = groups.entrySet().iterator(); i.hasNext() && group == null; )
        {
            Map.Entry entry = (Map.Entry) i.next();

            if ( path.startsWith( "/" + entry.getKey() + "/" ) )
            {
                group = (ProxiedRepositoryGroup) entry.getValue();
            }
        }

        return group;
    }

    private static ProxyInfo createWagonProxy( Proxy proxy )
    {
        ProxyInfo proxyInfo = null;
        if ( proxy != null && !StringUtils.isEmpty( proxy.getHost() ) )
        {
            proxyInfo = new ProxyInfo();
            proxyInfo.setHost( proxy.getHost() );
            proxyInfo.setPort( proxy.getPort() );
            proxyInfo.setUserName( proxy.getUsername() );
            proxyInfo.setPassword( proxy.getPassword() );
            proxyInfo.setNonProxyHosts( proxy.getNonProxyHosts() );
            proxyInfo.setType( proxy.getProtocol() );
        }
        return proxyInfo;
    }
}
