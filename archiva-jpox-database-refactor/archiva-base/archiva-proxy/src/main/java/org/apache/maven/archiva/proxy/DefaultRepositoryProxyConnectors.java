package org.apache.maven.archiva.proxy;

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.NetworkProxyConfiguration;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryProxyConnectorConfiguration;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.proxy.policy.PostfetchPolicy;
import org.apache.maven.archiva.proxy.policy.PrefetchPolicy;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.codehaus.plexus.util.SelectorUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * DefaultRepositoryProxyConnectors 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role-hint="default"
 */
public class DefaultRepositoryProxyConnectors
    extends AbstractLogEnabled
    implements RepositoryProxyConnectors, RegistryListener, Initializable
{
    private static final String FILENAME_MAVEN_METADATA = "maven-metadata.xml";

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement role="org.apache.maven.wagon.Wagon"
     */
    private Map/*<String,Wagon>*/wagons;

    /**
     * @plexus.requirement
     */
    private BidirectionalRepositoryLayoutFactory layoutFactory;

    /**
     * @plexus.requirement role="checksum"
     */
    private PrefetchPolicy checksumPolicy;

    /**
     * @plexus.requirement role="artifact-update"
     */
    private PostfetchPolicy updatePolicy;

    private Map proxyConnectorMap = new HashMap();

    private Map networkProxyMap = new HashMap();

    private List propertyNameTriggers = new ArrayList();

    public boolean fetchFromProxies( ArchivaRepository repository, ArtifactReference artifact )
        throws ProxyException
    {
        if ( !repository.isManaged() )
        {
            throw new ProxyException( "Can only proxy managed repositories." );
        }

        File localFile;
        try
        {
            BidirectionalRepositoryLayout sourceLayout = layoutFactory.getLayout( repository.getLayoutType() );
            String sourcePath = sourceLayout.toPath( artifact );
            localFile = new File( repository.getUrl().getPath(), sourcePath );
        }
        catch ( LayoutException e )
        {
            throw new ProxyException( "Unable to proxy due to bad source repository layout definition: "
                + e.getMessage(), e );
        }

        boolean isSnapshot = VersionUtil.isSnapshot( artifact.getVersion() );

        List connectors = getProxyConnectors( repository );
        Iterator it = connectors.iterator();
        while ( it.hasNext() )
        {
            ProxyConnector connector = (ProxyConnector) it.next();
            ArchivaRepository targetRepository = connector.getTargetRepository();
            try
            {
                BidirectionalRepositoryLayout targetLayout = layoutFactory.getLayout( targetRepository.getLayoutType() );
                String targetPath = targetLayout.toPath( artifact );

                if ( transferFile( connector, targetRepository, targetPath, localFile, isSnapshot ) )
                {
                    // Transfer was successful.  return.
                    return true;
                }
            }
            catch ( LayoutException e )
            {
                getLogger().error( "Unable to proxy due to bad layout definition: " + e.getMessage(), e );
                return false;
            }
        }

        return false;
    }

    public boolean fetchFromProxies( ArchivaRepository repository, ProjectReference metadata )
        throws ProxyException
    {
        if ( !repository.isManaged() )
        {
            throw new ProxyException( "Can only proxy managed repositories." );
        }

        File localFile;
        try
        {
            BidirectionalRepositoryLayout sourceLayout = layoutFactory.getLayout( repository.getLayoutType() );
            String sourcePath = sourceLayout.toPath( metadata ) + FILENAME_MAVEN_METADATA;
            localFile = new File( repository.getUrl().getPath(), sourcePath );
        }
        catch ( LayoutException e )
        {
            throw new ProxyException( "Unable to proxy due to bad source repository layout definition: "
                + e.getMessage(), e );
        }

        List connectors = getProxyConnectors( repository );
        Iterator it = connectors.iterator();
        while ( it.hasNext() )
        {
            ProxyConnector connector = (ProxyConnector) it.next();
            ArchivaRepository targetRepository = connector.getTargetRepository();
            try
            {
                BidirectionalRepositoryLayout targetLayout = layoutFactory.getLayout( targetRepository.getLayoutType() );
                String targetPath = targetLayout.toPath( metadata ) + FILENAME_MAVEN_METADATA;

                if ( transferFile( connector, targetRepository, targetPath, localFile, false ) )
                {
                    // Transfer was successful.  return.
                    return true;
                }
            }
            catch ( LayoutException e )
            {
                getLogger().error( "Unable to proxy due to bad layout definition: " + e.getMessage(), e );
                return false;
            }
        }

        return false;
    }

    /**
     * Perform the transfer of the file.
     * 
     * @param connector
     * @param targetRepository
     * @param targetPath
     * @param localFile
     * @param isSnapshot
     * @return
     * @throws ProxyException 
     */
    private boolean transferFile( ProxyConnector connector, ArchivaRepository targetRepository, String targetPath,
                                  File localFile, boolean isSnapshot )
        throws ProxyException
    {
        if ( isSnapshot )
        {
            // Handle Snapshot Policy
            if ( !updatePolicy.applyPolicy( connector.getSnapshotsPolicy(), localFile ) )
            {
                return false;
            }
        }
        else
        {
            // Handle Release Policy
            if ( !updatePolicy.applyPolicy( connector.getReleasesPolicy(), localFile ) )
            {
                return false;
            }
        }

        // Is a whitelist defined?
        if ( CollectionUtils.isNotEmpty( connector.getWhitelist() ) )
        {
            // Path must belong to whitelist.
            if ( !matchesPattern( targetPath, connector.getWhitelist() ) )
            {
                getLogger().debug( "Path [" + targetPath + "] is not part of defined whitelist (skipping transfer)." );
                return false;
            }
        }

        // Is target path part of blacklist?
        if ( matchesPattern( targetPath, connector.getBlacklist() ) )
        {
            getLogger().debug( "Path [" + targetPath + "] is part of blacklist (skipping transfer)." );
            return false;
        }

        // Transfer the file.
        Wagon wagon = null;

        try
        {
            File temp = new File( localFile.getAbsolutePath() + ".tmp" );
            temp.deleteOnExit();

            String protocol = targetRepository.getUrl().getProtocol();
            wagon = (Wagon) wagons.get( protocol );
            if ( wagon == null )
            {
                throw new ProxyException( "Unsupported target repository protocol: " + protocol );
            }

            boolean connected = connectToRepository( connector, wagon, targetRepository );
            if ( connected )
            {
                if ( localFile.exists() )
                {
                    getLogger().debug( "Retrieving " + targetPath + " from " + targetRepository.getName() );
                    wagon.get( targetPath, temp );
                }
                else
                {
                    getLogger().debug(
                                       "Retrieving " + targetPath + " from " + targetRepository.getName()
                                           + " if updated" );
                    wagon.getIfNewer( targetPath, temp, localFile.lastModified() );
                }

                // temp won't exist if we called getIfNewer and it was older, but its still a successful return
                if ( temp.exists() )
                {
                    moveTempToTarget( temp, localFile );
                }
                else
                {
                    getLogger().debug(
                                       "Attempt to retrieving " + targetPath + " from " + targetRepository.getName()
                                           + " failed: local file does not exist." );
                    return false;
                }

                getLogger().debug( "Successfully downloaded" );
            }
        }
        catch ( WagonException e )
        {
            getLogger().warn( "Download failure:" + e.getMessage(), e );
            return false;
        }

        // Handle checksum Policy.
        return checksumPolicy.applyPolicy( connector.getChecksumPolicy(), localFile );
    }

    /**
     * Used to move the temporary file to its real destination.  This is patterned from the way WagonManager handles
     * its downloaded files.
     *
     * @param temp   The completed download file
     * @param target The final location of the downloaded file
     * @throws ProxyException when the temp file cannot replace the target file
     */
    private void moveTempToTarget( File temp, File target )
        throws ProxyException
    {
        if ( target.exists() && !target.delete() )
        {
            throw new ProxyException( "Unable to overwrite existing target file: " + target.getAbsolutePath() );
        }

        if ( !temp.renameTo( target ) )
        {
            getLogger().warn( "Unable to rename tmp file to its final name... resorting to copy command." );

            try
            {
                FileUtils.copyFile( temp, target );
            }
            catch ( IOException e )
            {
                throw new ProxyException( "Cannot copy tmp file to its final location", e );
            }
            finally
            {
                temp.delete();
            }
        }
    }

    private boolean connectToRepository( ProxyConnector connector, Wagon wagon, ArchivaRepository targetRepository )
    {
        boolean connected = false;

        ProxyInfo networkProxy = null;
        synchronized ( this.networkProxyMap )
        {
            networkProxy = (ProxyInfo) this.networkProxyMap.get( connector.getProxyId() );
        }

        try
        {
            Repository wagonRepository = new Repository( targetRepository.getId(), targetRepository.getUrl().toString() );
            if ( networkProxy != null )
            {
                wagon.connect( wagonRepository, networkProxy );
            }
            else
            {
                wagon.connect( wagonRepository );
            }
            connected = true;
        }
        catch ( ConnectionException e )
        {
            getLogger().info( "Could not connect to " + targetRepository.getName() + ": " + e.getMessage() );
        }
        catch ( AuthenticationException e )
        {
            getLogger().info( "Could not connect to " + targetRepository.getName() + ": " + e.getMessage() );
        }

        return connected;
    }

    private boolean matchesPattern( String path, List patterns )
    {
        if ( CollectionUtils.isEmpty( patterns ) )
        {
            return false;
        }

        Iterator it = patterns.iterator();
        while ( it.hasNext() )
        {
            String pattern = (String) it.next();
            if ( SelectorUtils.matchPath( pattern, path, false ) )
            {
                return true;
            }
        }

        return false;
    }

    public List getProxyConnectors( ArchivaRepository repository )
    {
        synchronized ( this.proxyConnectorMap )
        {
            List ret = (List) this.proxyConnectorMap.get( repository.getId() );
            if ( ret == null )
            {
                return Collections.EMPTY_LIST;
            }
            return ret;
        }
    }

    public boolean hasProxies( ArchivaRepository repository )
    {
        synchronized ( this.proxyConnectorMap )
        {
            return this.proxyConnectorMap.containsKey( repository.getId() );
        }
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( propertyNameTriggers.contains( propertyName ) )
        {
            initConnectorsAndNetworkProxies();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void initConnectorsAndNetworkProxies()
    {
        Iterator it;

        synchronized ( this.proxyConnectorMap )
        {
            this.proxyConnectorMap.clear();

            List proxyConfigs = archivaConfiguration.getConfiguration().getProxyConnectors();
            it = proxyConfigs.iterator();
            while ( it.hasNext() )
            {
                RepositoryProxyConnectorConfiguration proxyConfig = (RepositoryProxyConnectorConfiguration) it.next();
                String key = proxyConfig.getSourceRepoId();

                // Create connector object.
                ProxyConnector connector = new ProxyConnector();
                connector.setSourceRepository( getRepository( proxyConfig.getSourceRepoId() ) );
                connector.setTargetRepository( getRepository( proxyConfig.getTargetRepoId() ) );
                connector.setSnapshotsPolicy( proxyConfig.getSnapshotsPolicy() );
                connector.setReleasesPolicy( proxyConfig.getReleasesPolicy() );
                connector.setChecksumPolicy( proxyConfig.getChecksumPolicy() );

                // Copy any blacklist patterns.
                List blacklist = new ArrayList();
                if ( !CollectionUtils.isEmpty( proxyConfig.getBlackListPatterns() ) )
                {
                    blacklist.addAll( proxyConfig.getBlackListPatterns() );
                }
                connector.setBlacklist( blacklist );

                // Copy any whitelist patterns.
                List whitelist = new ArrayList();
                if ( !CollectionUtils.isEmpty( proxyConfig.getWhiteListPatterns() ) )
                {
                    whitelist.addAll( proxyConfig.getWhiteListPatterns() );
                }
                connector.setWhitelist( whitelist );

                // Get other connectors
                List connectors = (List) this.proxyConnectorMap.get( key );
                if ( connectors == null )
                {
                    // Create if we are the first.
                    connectors = new ArrayList();
                }

                // Add the connector.
                connectors.add( connector );

                // Set the key to the list of connectors.
                this.proxyConnectorMap.put( key, connectors );
            }
        }

        synchronized ( this.networkProxyMap )
        {
            this.networkProxyMap.clear();

            List networkProxies = archivaConfiguration.getConfiguration().getNetworkProxies();
            it = networkProxies.iterator();
            while ( it.hasNext() )
            {
                NetworkProxyConfiguration networkProxyConfig = (NetworkProxyConfiguration) it.next();
                String key = networkProxyConfig.getId();

                ProxyInfo proxy = new ProxyInfo();

                proxy.setType( networkProxyConfig.getProtocol() );
                proxy.setHost( networkProxyConfig.getHost() );
                proxy.setPort( networkProxyConfig.getPort() );
                proxy.setUserName( networkProxyConfig.getUsername() );
                proxy.setPassword( networkProxyConfig.getPassword() );

                this.networkProxyMap.put( key, proxy );
            }
        }
    }

    private ArchivaRepository getRepository( String repoId )
    {
        RepositoryConfiguration repoConfig = archivaConfiguration.getConfiguration().findRepositoryById( repoId );
        if ( repoConfig == null )
        {
            return null;
        }

        ArchivaRepository repo = new ArchivaRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getUrl() );
        return repo;
    }

    public void initialize()
        throws InitializationException
    {
        propertyNameTriggers.add( "repositories" );
        propertyNameTriggers.add( "repository" );
        propertyNameTriggers.add( "id" );
        propertyNameTriggers.add( "name" );
        propertyNameTriggers.add( "url" );
        propertyNameTriggers.add( "layout" );
        propertyNameTriggers.add( "releases" );
        propertyNameTriggers.add( "snapshots" );
        propertyNameTriggers.add( "indexed" );

        propertyNameTriggers.add( "proxyConnectors" );
        propertyNameTriggers.add( "proxyConnector" );
        propertyNameTriggers.add( "sourceRepoId" );
        propertyNameTriggers.add( "targetRepoId" );
        propertyNameTriggers.add( "proxyId" );
        propertyNameTriggers.add( "snapshotsPolicy" );
        propertyNameTriggers.add( "releasePolicy" );
        propertyNameTriggers.add( "checksumPolicy" );
        propertyNameTriggers.add( "whiteListPatterns" );
        propertyNameTriggers.add( "whiteListPattern" );
        propertyNameTriggers.add( "blackListPatterns" );
        propertyNameTriggers.add( "blackListPattern" );

        propertyNameTriggers.add( "networkProxies" );
        propertyNameTriggers.add( "networkProxy" );
        propertyNameTriggers.add( "protocol" );
        propertyNameTriggers.add( "host" );
        propertyNameTriggers.add( "port" );
        propertyNameTriggers.add( "username" );
        propertyNameTriggers.add( "password" );

        archivaConfiguration.addChangeListener( this );
        initConnectorsAndNetworkProxies();
    }
}
