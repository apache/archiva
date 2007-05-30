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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.NetworkProxyConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.policies.DownloadPolicy;
import org.apache.maven.archiva.policies.urlcache.UrlFailureCache;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

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
     * @plexus.requirement role="org.apache.maven.archiva.policies.PreDownloadPolicy"
     */
    private Map preDownloadPolicies;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.policies.PostDownloadPolicy"
     */
    private Map postDownloadPolicies;

    /**
     * @plexus.requirement role-hint="default"
     */
    private UrlFailureCache urlFailureCache;

    private Map proxyConnectorMap = new HashMap();

    private Map networkProxyMap = new HashMap();

    public File fetchFromProxies( ArchivaRepository repository, ArtifactReference artifact )
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

        Properties requestProperties = new Properties();
        requestProperties.setProperty( "version", artifact.getVersion() );

        List connectors = getProxyConnectors( repository );
        Iterator it = connectors.iterator();
        while ( it.hasNext() )
        {
            ProxyConnector connector = (ProxyConnector) it.next();
            getLogger().debug( "Attempting connector: " + connector );
            ArchivaRepository targetRepository = connector.getTargetRepository();
            try
            {
                BidirectionalRepositoryLayout targetLayout = layoutFactory.getLayout( targetRepository.getLayoutType() );
                String targetPath = targetLayout.toPath( artifact );

                getLogger().debug(
                                   "Using target repository: " + targetRepository.getId() + " - layout: "
                                       + targetRepository.getLayoutType() + " - targetPath: " + targetPath );

                File downloadedFile = transferFile( connector, targetRepository, targetPath, localFile,
                                                    requestProperties );

                if ( fileExists( downloadedFile ) )
                {
                    getLogger().info( "Successfully transfered: " + downloadedFile.getAbsolutePath() );
                    return downloadedFile;
                }
            }
            catch ( LayoutException e )
            {
                getLogger().error( "Unable to proxy due to bad layout definition: " + e.getMessage(), e );
                return null;
            }
        }

        return null;
    }

    public File fetchFromProxies( ArchivaRepository repository, VersionedReference metadata )
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
            String sourcePath = sourceLayout.toPath( metadata );
            localFile = new File( repository.getUrl().getPath(), sourcePath );
        }
        catch ( LayoutException e )
        {
            throw new ProxyException( "Unable to proxy due to bad source repository layout definition: "
                + e.getMessage(), e );
        }

        Properties requestProperties = new Properties();

        List connectors = getProxyConnectors( repository );
        Iterator it = connectors.iterator();
        while ( it.hasNext() )
        {
            ProxyConnector connector = (ProxyConnector) it.next();
            ArchivaRepository targetRepository = connector.getTargetRepository();
            try
            {
                BidirectionalRepositoryLayout targetLayout = layoutFactory.getLayout( targetRepository.getLayoutType() );
                String targetPath = targetLayout.toPath( metadata );

                File downloadedFile = transferFile( connector, targetRepository, targetPath, localFile,
                                                    requestProperties );

                if ( fileExists( downloadedFile ) )
                {
                    getLogger().info( "Successfully transfered: " + downloadedFile.getAbsolutePath() );
                    return downloadedFile;
                }
            }
            catch ( LayoutException e )
            {
                getLogger().error( "Unable to proxy due to bad layout definition: " + e.getMessage(), e );
                return null;
            }
        }

        return null;
    }

    public File fetchFromProxies( ArchivaRepository repository, ProjectReference metadata )
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
            String sourcePath = sourceLayout.toPath( metadata );
            localFile = new File( repository.getUrl().getPath(), sourcePath );
        }
        catch ( LayoutException e )
        {
            throw new ProxyException( "Unable to proxy due to bad source repository layout definition: "
                + e.getMessage(), e );
        }

        Properties requestProperties = new Properties();

        List connectors = getProxyConnectors( repository );
        Iterator it = connectors.iterator();
        while ( it.hasNext() )
        {
            ProxyConnector connector = (ProxyConnector) it.next();
            ArchivaRepository targetRepository = connector.getTargetRepository();
            try
            {
                BidirectionalRepositoryLayout targetLayout = layoutFactory.getLayout( targetRepository.getLayoutType() );
                String targetPath = targetLayout.toPath( metadata );

                File downloadedFile = transferFile( connector, targetRepository, targetPath, localFile,
                                                    requestProperties );

                if ( fileExists( downloadedFile ) )
                {
                    getLogger().info( "Successfully transfered: " + downloadedFile.getAbsolutePath() );
                    return downloadedFile;
                }
            }
            catch ( LayoutException e )
            {
                getLogger().error( "Unable to proxy due to bad layout definition: " + e.getMessage(), e );
                return null;
            }
        }

        return null;
    }

    private boolean fileExists( File file )
    {
        if ( file == null )
        {
            return false;
        }

        if ( !file.exists() )
        {
            return false;
        }

        if ( !file.isFile() )
        {
            return false;
        }

        return true;
    }

    /**
     * Perform the transfer of the file.
     * 
     * @param connector
     * @param targetRepository
     * @param targetPath
     * @param localFile
     * @param requestProperties
     * @return
     * @throws ProxyException 
     */
    private File transferFile( ProxyConnector connector, ArchivaRepository targetRepository, String targetPath,
                               File localFile, Properties requestProperties )
        throws ProxyException
    {
        String url = targetRepository.getUrl().toString() + targetPath;
        requestProperties.setProperty( "url", url );

        // Handle pre-download policy
        if ( !applyPolicies( connector.getPolicies(), this.preDownloadPolicies, requestProperties, localFile ) )
        {
            getLogger().info( "Failed pre-download policies - " + localFile.getAbsolutePath() );

            if ( fileExists( localFile ) )
            {
                return localFile;
            }

            return null;
        }

        // Is a whitelist defined?
        if ( !isEmpty( connector.getWhitelist() ) )
        {
            // Path must belong to whitelist.
            if ( !matchesPattern( targetPath, connector.getWhitelist() ) )
            {
                getLogger().debug( "Path [" + targetPath + "] is not part of defined whitelist (skipping transfer)." );
                return null;
            }
        }

        // Is target path part of blacklist?
        if ( matchesPattern( targetPath, connector.getBlacklist() ) )
        {
            getLogger().debug( "Path [" + targetPath + "] is part of blacklist (skipping transfer)." );
            return null;
        }

        Wagon wagon = null;
        try
        {
            String protocol = targetRepository.getUrl().getProtocol();
            wagon = (Wagon) wagons.get( protocol );
            if ( wagon == null )
            {
                throw new ProxyException( "Unsupported target repository protocol: " + protocol );
            }

            boolean connected = connectToRepository( connector, wagon, targetRepository );
            if ( connected )
            {
                localFile = transferSimpleFile( wagon, targetRepository, targetPath, localFile );

                transferChecksum( wagon, targetRepository, targetPath, localFile, ".sha1" );
                transferChecksum( wagon, targetRepository, targetPath, localFile, ".md5" );
            }
        }
        catch ( ResourceDoesNotExistException e )
        {
            // Do not cache url here.
            return null;
        }
        catch ( WagonException e )
        {
            urlFailureCache.cacheFailure( url );
            return null;
        }
        finally
        {
            if ( wagon != null )
            {
                try
                {
                    wagon.disconnect();
                }
                catch ( ConnectionException e )
                {
                    getLogger().warn( "Unable to disconnect wagon.", e );
                }
            }
        }

        // Handle post-download policies.
        if ( !applyPolicies( connector.getPolicies(), this.postDownloadPolicies, requestProperties, localFile ) )
        {
            getLogger().info( "Failed post-download policies - " + localFile.getAbsolutePath() );

            if ( fileExists( localFile ) )
            {
                return localFile;
            }

            return null;
        }

        // Everything passes.
        return localFile;
    }

    private void transferChecksum( Wagon wagon, ArchivaRepository targetRepository, String targetPath, File localFile,
                                   String type )
        throws ProxyException
    {
        String url = targetRepository.getUrl().toString() + targetPath;

        // Transfer checksum does not use the policy. 
        if ( urlFailureCache.hasFailedBefore( url + type ) )
        {
            return;
        }

        try
        {
            File hashFile = new File( localFile.getAbsolutePath() + type );
            transferSimpleFile( wagon, targetRepository, targetPath + type, hashFile );
            getLogger().debug( "Checksum" + type + " Downloaded: " + hashFile );
        }
        catch ( ResourceDoesNotExistException e )
        {
            getLogger().debug( "Checksum" + type + " Not Download: " + e.getMessage() );
        }
        catch ( WagonException e )
        {
            urlFailureCache.cacheFailure( url + type );
            getLogger().warn( "Transfer failed on checksum: " + url + " : " + e.getMessage(), e );
        }
    }

    private File transferSimpleFile( Wagon wagon, ArchivaRepository targetRepository, String targetPath, File localFile )
        throws ProxyException, WagonException
    {
        // Transfer the file.
        File temp = null;

        try
        {
            temp = new File( localFile.getAbsolutePath() + ".tmp" );

            boolean success = false;

            if ( localFile.exists() )
            {
                getLogger().debug( "Retrieving " + targetPath + " from " + targetRepository.getName() );
                wagon.get( targetPath, temp );
                success = true;

                if ( temp.exists() )
                {
                    moveTempToTarget( temp, localFile );
                }

                // You wouldn't get here on failure, a WagonException would have been thrown.
                getLogger().debug( "Downloaded successfully." );
            }
            else
            {
                getLogger().debug( "Retrieving " + targetPath + " from " + targetRepository.getName() + " if updated" );
                success = wagon.getIfNewer( targetPath, temp, localFile.lastModified() );
                if ( !success )
                {
                    getLogger().debug(
                                       "Not downloaded, as local file is newer than remote side: "
                                           + localFile.getAbsolutePath() );
                }
                else if ( temp.exists() )
                {
                    getLogger().debug( "Downloaded successfully." );
                    moveTempToTarget( temp, localFile );
                }
            }

            return localFile;
        }
        catch ( ResourceDoesNotExistException e )
        {
            getLogger().warn( "Resource does not exist: " + e.getMessage() );
            throw e;
        }
        catch ( WagonException e )
        {
            getLogger().warn( "Download failure:" + e.getMessage(), e );
            throw e;
        }
        finally
        {
            if ( temp != null )
            {
                temp.delete();
            }
        }
    }

    private boolean applyPolicies( Map policySettings, Map downloadPolicies, Properties request, File localFile )
    {
        Iterator it = downloadPolicies.entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String key = (String) entry.getKey();
            DownloadPolicy policy = (DownloadPolicy) entry.getValue();
            String defaultSetting = policy.getDefaultOption();
            String setting = StringUtils.defaultString( (String) policySettings.get( key ), defaultSetting );

            getLogger().debug( "Applying [" + key + "] policy with [" + setting + "]" );
            if ( !policy.applyPolicy( setting, request, localFile ) )
            {
                getLogger().debug( "Didn't pass the [" + key + "] policy." );
                return false;
            }
        }
        return true;
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
        if ( isEmpty( patterns ) )
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
        if ( ConfigurationNames.isNetworkProxy( propertyName ) || ConfigurationNames.isRepositories( propertyName )
            || ConfigurationNames.isProxyConnector( propertyName ) )
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
                ProxyConnectorConfiguration proxyConfig = (ProxyConnectorConfiguration) it.next();
                String key = proxyConfig.getSourceRepoId();

                // Create connector object.
                ProxyConnector connector = new ProxyConnector();
                connector.setSourceRepository( getRepository( proxyConfig.getSourceRepoId() ) );
                connector.setTargetRepository( getRepository( proxyConfig.getTargetRepoId() ) );
                connector.setProxyId( proxyConfig.getProxyId() );
                connector.setPolicies( proxyConfig.getPolicies() );

                // Copy any blacklist patterns.
                List blacklist = new ArrayList();
                if ( !isEmpty( proxyConfig.getBlackListPatterns() ) )
                {
                    blacklist.addAll( proxyConfig.getBlackListPatterns() );
                }
                connector.setBlacklist( blacklist );

                // Copy any whitelist patterns.
                List whitelist = new ArrayList();
                if ( !isEmpty( proxyConfig.getWhiteListPatterns() ) )
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

    private boolean isEmpty( Collection collection )
    {
        if ( collection == null )
        {
            return true;
        }

        return collection.isEmpty();
    }

    private ArchivaRepository getRepository( String repoId )
    {
        RepositoryConfiguration repoConfig = archivaConfiguration.getConfiguration().findRepositoryById( repoId );
        if ( repoConfig == null )
        {
            return null;
        }

        ArchivaRepository repo = new ArchivaRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getUrl() );
        repo.getModel().setLayoutName( repoConfig.getLayout() );
        return repo;
    }

    public void initialize()
        throws InitializationException
    {
        initConnectorsAndNetworkProxies();
        archivaConfiguration.addChangeListener( this );
    }
}
