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

import com.google.common.collect.Lists;
import org.apache.archiva.proxy.common.WagonFactory;
import org.apache.archiva.proxy.common.WagonFactoryException;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.NetworkProxyConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.Keys;
import org.apache.maven.archiva.model.RepositoryURL;
import org.apache.maven.archiva.policies.DownloadErrorPolicy;
import org.apache.maven.archiva.policies.DownloadPolicy;
import org.apache.maven.archiva.policies.PolicyConfigurationException;
import org.apache.maven.archiva.policies.PolicyViolationException;
import org.apache.maven.archiva.policies.PostDownloadPolicy;
import org.apache.maven.archiva.policies.PreDownloadPolicy;
import org.apache.maven.archiva.policies.ProxyDownloadException;
import org.apache.maven.archiva.policies.urlcache.UrlFailureCache;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RemoteRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.plexus.util.SelectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * DefaultRepositoryProxyConnectors
 *
 * @version $Id$
 * @todo exception handling needs work - "not modified" is not really an exceptional case, and it has more layers than
 * your average brown onion
 */
@Service( "repositoryProxyConnectors#default" )
public class DefaultRepositoryProxyConnectors
    implements RepositoryProxyConnectors, RegistryListener
{
    private Logger log = LoggerFactory.getLogger( DefaultRepositoryProxyConnectors.class );

    /**
     * plexus.requirement
     */
    @Inject
    @Named( value = "archivaConfiguration#default" )
    private ArchivaConfiguration archivaConfiguration;

    /**
     * plexus.requirement
     */
    @Inject
    @Named( value = "repositoryContentFactory#default" )
    private RepositoryContentFactory repositoryFactory;

    /**
     * plexus.requirement
     */
    @Inject
    @Named( value = "metadataTools#default" )
    private MetadataTools metadataTools;

    //@Inject
    //private ApplicationContext applicationContext;

    /**
     * plexus.requirement role="org.apache.maven.archiva.policies.PreDownloadPolicy"
     */
    @Inject
    private Map<String, PreDownloadPolicy> preDownloadPolicies;

    /**
     * plexus.requirement role="org.apache.maven.archiva.policies.PostDownloadPolicy"
     */
    @Inject
    private Map<String, PostDownloadPolicy> postDownloadPolicies;

    /**
     * plexus.requirement role="org.apache.maven.archiva.policies.DownloadErrorPolicy"
     */
    @Inject
    private Map<String, DownloadErrorPolicy> downloadErrorPolicies;

    /**
     * plexus.requirement role-hint="default"
     */
    @Inject
    private UrlFailureCache urlFailureCache;

    private Map<String, List<ProxyConnector>> proxyConnectorMap = new HashMap<String, List<ProxyConnector>>();

    private Map<String, ProxyInfo> networkProxyMap = new HashMap<String, ProxyInfo>();

    /**
     * plexus.requirement
     */
    @Inject
    private WagonFactory wagonFactory;

    /**
     * plexus.requirement role="org.apache.archiva.scheduler.ArchivaTaskScheduler" role-hint="repository"
     */
    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private ArchivaTaskScheduler scheduler;

    @PostConstruct
    public void initialize()
    {
        initConnectorsAndNetworkProxies();
        archivaConfiguration.addChangeListener( this );
        //this.postDownloadPolicies = applicationContext.getBeansOfType( PostDownloadPolicy.class );
        //his.preDownloadPolicies = applicationContext.getBeansOfType( PreDownloadPolicy.class );
        //this.downloadErrorPolicies = applicationContext.getBeansOfType( DownloadErrorPolicy.class );
    }

    @SuppressWarnings( "unchecked" )
    private void initConnectorsAndNetworkProxies()
    {
        synchronized ( this.proxyConnectorMap )
        {
            ProxyConnectorOrderComparator proxyOrderSorter = new ProxyConnectorOrderComparator();
            this.proxyConnectorMap.clear();

            List<ProxyConnectorConfiguration> proxyConfigs =
                archivaConfiguration.getConfiguration().getProxyConnectors();
            for ( ProxyConnectorConfiguration proxyConfig : proxyConfigs )
            {
                String key = proxyConfig.getSourceRepoId();

                try
                {
                    // Create connector object.
                    ProxyConnector connector = new ProxyConnector();

                    connector.setSourceRepository(
                        repositoryFactory.getManagedRepositoryContent( proxyConfig.getSourceRepoId() ) );
                    connector.setTargetRepository(
                        repositoryFactory.getRemoteRepositoryContent( proxyConfig.getTargetRepoId() ) );

                    connector.setProxyId( proxyConfig.getProxyId() );
                    connector.setPolicies( proxyConfig.getPolicies() );
                    connector.setOrder( proxyConfig.getOrder() );
                    connector.setDisabled( proxyConfig.isDisabled() );

                    // Copy any blacklist patterns.
                    List<String> blacklist = new ArrayList<String>();
                    if ( CollectionUtils.isNotEmpty( proxyConfig.getBlackListPatterns() ) )
                    {
                        blacklist.addAll( proxyConfig.getBlackListPatterns() );
                    }
                    connector.setBlacklist( blacklist );

                    // Copy any whitelist patterns.
                    List<String> whitelist = new ArrayList<String>();
                    if ( CollectionUtils.isNotEmpty( proxyConfig.getWhiteListPatterns() ) )
                    {
                        whitelist.addAll( proxyConfig.getWhiteListPatterns() );
                    }
                    connector.setWhitelist( whitelist );

                    // Get other connectors
                    List<ProxyConnector> connectors = this.proxyConnectorMap.get( key );
                    if ( connectors == null )
                    {
                        // Create if we are the first.
                        connectors = new ArrayList<ProxyConnector>();
                    }

                    // Add the connector.
                    connectors.add( connector );

                    // Ensure the list is sorted.
                    Collections.sort( connectors, proxyOrderSorter );

                    // Set the key to the list of connectors.
                    this.proxyConnectorMap.put( key, connectors );
                }
                catch ( RepositoryNotFoundException e )
                {
                    log.warn( "Unable to use proxy connector: " + e.getMessage(), e );
                }
                catch ( RepositoryException e )
                {
                    log.warn( "Unable to use proxy connector: " + e.getMessage(), e );
                }
            }

        }

        synchronized ( this.networkProxyMap )
        {
            this.networkProxyMap.clear();

            List<NetworkProxyConfiguration> networkProxies =
                archivaConfiguration.getConfiguration().getNetworkProxies();
            for ( NetworkProxyConfiguration networkProxyConfig : networkProxies )
            {
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

    public File fetchFromProxies( ManagedRepositoryContent repository, ArtifactReference artifact )
        throws ProxyDownloadException
    {
        File localFile = toLocalFile( repository, artifact );

        Properties requestProperties = new Properties();
        requestProperties.setProperty( "filetype", "artifact" );
        requestProperties.setProperty( "version", artifact.getVersion() );
        requestProperties.setProperty( "managedRepositoryId", repository.getId() );

        List<ProxyConnector> connectors = getProxyConnectors( repository );
        Map<String, Exception> previousExceptions = new LinkedHashMap<String, Exception>();
        for ( ProxyConnector connector : connectors )
        {
            if ( connector.isDisabled() )
            {
                continue;
            }

            RemoteRepositoryContent targetRepository = connector.getTargetRepository();
            requestProperties.setProperty( "remoteRepositoryId", targetRepository.getId() );

            String targetPath = targetRepository.toPath( artifact );

            try
            {
                File downloadedFile =
                    transferFile( connector, targetRepository, targetPath, repository, localFile, requestProperties,
                                  true );

                if ( fileExists( downloadedFile ) )
                {
                    log.debug( "Successfully transferred: {}", downloadedFile.getAbsolutePath() );
                    return downloadedFile;
                }
            }
            catch ( NotFoundException e )
            {
                log.debug( "Artifact {} not found on repository \"{}\".", Keys.toKey( artifact ),
                           targetRepository.getRepository().getId() );
            }
            catch ( NotModifiedException e )
            {
                log.debug( "Artifact {} not updated on repository \"{}\".", Keys.toKey( artifact ),
                           targetRepository.getRepository().getId() );
            }
            catch ( ProxyException e )
            {
                validatePolicies( this.downloadErrorPolicies, connector.getPolicies(), requestProperties, artifact,
                                  targetRepository, localFile, e, previousExceptions );
            }
        }

        if ( !previousExceptions.isEmpty() )
        {
            throw new ProxyDownloadException( "Failures occurred downloading from some remote repositories",
                                              previousExceptions );
        }

        log.debug( "Exhausted all target repositories, artifact {} not found.", Keys.toKey( artifact ) );

        return null;
    }

    public File fetchFromProxies( ManagedRepositoryContent repository, String path )
    {
        File localFile = new File( repository.getRepoRoot(), path );

        // no update policies for these paths
        if ( localFile.exists() )
        {
            return null;
        }

        Properties requestProperties = new Properties();
        requestProperties.setProperty( "filetype", "resource" );
        requestProperties.setProperty( "managedRepositoryId", repository.getId() );

        List<ProxyConnector> connectors = getProxyConnectors( repository );
        for ( ProxyConnector connector : connectors )
        {
            if ( connector.isDisabled() )
            {
                continue;
            }

            RemoteRepositoryContent targetRepository = connector.getTargetRepository();
            requestProperties.setProperty( "remoteRepositoryId", targetRepository.getId() );

            String targetPath = path;

            try
            {
                File downloadedFile =
                    transferFile( connector, targetRepository, targetPath, repository, localFile, requestProperties,
                                  false );

                if ( fileExists( downloadedFile ) )
                {
                    log.debug( "Successfully transferred: {}", downloadedFile.getAbsolutePath() );
                    return downloadedFile;
                }
            }
            catch ( NotFoundException e )
            {
                log.debug( "Resource {} not found on repository \"{}\".", path,
                           targetRepository.getRepository().getId() );
            }
            catch ( NotModifiedException e )
            {
                log.debug( "Resource {} not updated on repository \"{}\".", path,
                           targetRepository.getRepository().getId() );
            }
            catch ( ProxyException e )
            {
                log.warn(
                    "Transfer error from repository \"" + targetRepository.getRepository().getId() + "\" for resource "
                        + path + ", continuing to next repository. Error message: " + e.getMessage() );
                log.debug( "Full stack trace", e );
            }
        }

        log.debug( "Exhausted all target repositories, resource {} not found.", path );

        return null;
    }

    public File fetchMetatadaFromProxies( ManagedRepositoryContent repository, String logicalPath )
    {
        File localFile = new File( repository.getRepoRoot(), logicalPath );

        Properties requestProperties = new Properties();
        requestProperties.setProperty( "filetype", "metadata" );
        boolean metadataNeedsUpdating = false;
        long originalTimestamp = getLastModified( localFile );

        List<ProxyConnector> connectors = getProxyConnectors( repository );
        for ( ProxyConnector connector : connectors )
        {
            if ( connector.isDisabled() )
            {
                continue;
            }

            RemoteRepositoryContent targetRepository = connector.getTargetRepository();

            File localRepoFile = toLocalRepoFile( repository, targetRepository, logicalPath );
            long originalMetadataTimestamp = getLastModified( localRepoFile );

            try
            {
                transferFile( connector, targetRepository, logicalPath, repository, localRepoFile, requestProperties,
                              true );

                if ( hasBeenUpdated( localRepoFile, originalMetadataTimestamp ) )
                {
                    metadataNeedsUpdating = true;
                }
            }
            catch ( NotFoundException e )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Metadata {} not found on remote repository \"{}\".",
                               Lists.<Object>newArrayList( logicalPath, targetRepository.getRepository().getId() ), e );
                }
            }
            catch ( NotModifiedException e )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Metadata {} not updated on remote repository \"{}\".",
                               Lists.<Object>newArrayList( logicalPath, targetRepository.getRepository().getId() ), e );
                }
            }
            catch ( ProxyException e )
            {
                log.warn( "Transfer error from repository \"" + targetRepository.getRepository().getId()
                              + "\" for versioned Metadata " + logicalPath
                              + ", continuing to next repository. Error message: " + e.getMessage() );
                log.debug( "Full stack trace", e );
            }
        }

        if ( hasBeenUpdated( localFile, originalTimestamp ) )
        {
            metadataNeedsUpdating = true;
        }

        if ( metadataNeedsUpdating || !localFile.exists() )
        {
            try
            {
                metadataTools.updateMetadata( repository, logicalPath );
            }
            catch ( RepositoryMetadataException e )
            {
                log.warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage(), e );
            }
        }

        if ( fileExists( localFile ) )
        {
            return localFile;
        }

        return null;
    }

    private long getLastModified( File file )
    {
        if ( !file.exists() || !file.isFile() )
        {
            return 0;
        }

        return file.lastModified();
    }

    private boolean hasBeenUpdated( File file, long originalLastModified )
    {
        if ( !file.exists() || !file.isFile() )
        {
            return false;
        }

        long currentLastModified = getLastModified( file );
        return ( currentLastModified > originalLastModified );
    }

    private File toLocalRepoFile( ManagedRepositoryContent repository, RemoteRepositoryContent targetRepository,
                                  String targetPath )
    {
        String repoPath = metadataTools.getRepositorySpecificName( targetRepository, targetPath );
        return new File( repository.getRepoRoot(), repoPath );
    }

    /**
     * Test if the provided ManagedRepositoryContent has any proxies configured for it.
     */
    public boolean hasProxies( ManagedRepositoryContent repository )
    {
        synchronized ( this.proxyConnectorMap )
        {
            return this.proxyConnectorMap.containsKey( repository.getId() );
        }
    }

    private File toLocalFile( ManagedRepositoryContent repository, ArtifactReference artifact )
    {
        return repository.toFile( artifact );
    }

    /**
     * Simple method to test if the file exists on the local disk.
     *
     * @param file the file to test. (may be null)
     * @return true if file exists. false if the file param is null, doesn't exist, or is not of type File.
     */
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
     * @param connector         the connector configuration to use.
     * @param remoteRepository  the remote repository get the resource from.
     * @param remotePath        the path in the remote repository to the resource to get.
     * @param repository        the managed repository that will hold the file
     * @param resource          the local file to place the downloaded resource into
     * @param requestProperties the request properties to utilize for policy handling.
     * @param executeConsumers  whether to execute the consumers after proxying
     * @return the local file that was downloaded, or null if not downloaded.
     * @throws NotFoundException    if the file was not found on the remote repository.
     * @throws NotModifiedException if the localFile was present, and the resource was present on remote repository, but
     *                              the remote resource is not newer than the local File.
     * @throws ProxyException       if transfer was unsuccessful.
     */
    private File transferFile( ProxyConnector connector, RemoteRepositoryContent remoteRepository, String remotePath,
                               ManagedRepositoryContent repository, File resource, Properties requestProperties,
                               boolean executeConsumers )
        throws ProxyException, NotModifiedException
    {
        String url = remoteRepository.getURL().getUrl();
        if ( !url.endsWith( "/" ) )
        {
            url = url + "/";
        }
        url = url + remotePath;
        requestProperties.setProperty( "url", url );

        // Is a whitelist defined?
        if ( CollectionUtils.isNotEmpty( connector.getWhitelist() ) )
        {
            // Path must belong to whitelist.
            if ( !matchesPattern( remotePath, connector.getWhitelist() ) )
            {
                log.debug( "Path [{}] is not part of defined whitelist (skipping transfer from repository [{}]).",
                           remotePath, remoteRepository.getRepository().getName() );
                return null;
            }
        }

        // Is target path part of blacklist?
        if ( matchesPattern( remotePath, connector.getBlacklist() ) )
        {
            log.debug( "Path [{}] is part of blacklist (skipping transfer from repository [{}]).", remotePath,
                       remoteRepository.getRepository().getName() );
            return null;
        }

        // Handle pre-download policy
        try
        {
            validatePolicies( this.preDownloadPolicies, connector.getPolicies(), requestProperties, resource );
        }
        catch ( PolicyViolationException e )
        {
            String emsg = "Transfer not attempted on " + url + " : " + e.getMessage();
            if ( fileExists( resource ) )
            {
                log.debug( "{} : using already present local file.", emsg );
                return resource;
            }

            log.debug( emsg );
            return null;
        }

        File tmpMd5 = null;
        File tmpSha1 = null;
        File tmpResource = null;

        File workingDirectory = createWorkingDirectory( repository );
        try
        {
            Wagon wagon = null;
            try
            {
                RepositoryURL repoUrl = remoteRepository.getURL();
                String protocol = repoUrl.getProtocol();
                wagon = wagonFactory.getWagon( "wagon#" + protocol );
                if ( wagon == null )
                {
                    throw new ProxyException( "Unsupported target repository protocol: " + protocol );
                }

                boolean connected = connectToRepository( connector, wagon, remoteRepository );
                if ( connected )
                {
                    tmpResource = new File( workingDirectory, resource.getName() );
                    transferSimpleFile( wagon, remoteRepository, remotePath, repository, resource, tmpResource );

                    // TODO: these should be used to validate the download based on the policies, not always downloaded
                    // to
                    // save on connections since md5 is rarely used
                    tmpSha1 =
                        transferChecksum( wagon, remoteRepository, remotePath, repository, resource, workingDirectory,
                                          ".sha1" );
                    tmpMd5 =
                        transferChecksum( wagon, remoteRepository, remotePath, repository, resource, workingDirectory,
                                          ".md5" );
                }
            }
            catch ( NotFoundException e )
            {
                urlFailureCache.cacheFailure( url );
                throw e;
            }
            catch ( NotModifiedException e )
            {
                // Do not cache url here.
                throw e;
            }
            catch ( ProxyException e )
            {
                urlFailureCache.cacheFailure( url );
                throw e;
            }
            catch ( WagonFactoryException e )
            {
                throw new ProxyException( e.getMessage(), e );
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
                        log.warn( "Unable to disconnect wagon.", e );
                    }
                }
            }

            // Handle post-download policies.
            try
            {
                validatePolicies( this.postDownloadPolicies, connector.getPolicies(), requestProperties, tmpResource );
            }
            catch ( PolicyViolationException e )
            {
                log.warn( "Transfer invalidated from {} : {}", url, e.getMessage() );
                executeConsumers = false;
                if ( !fileExists( tmpResource ) )
                {
                    resource = null;
                }
            }

            if ( resource != null )
            {
                synchronized ( resource.getAbsolutePath().intern() )
                {
                    File directory = resource.getParentFile();
                    moveFileIfExists( tmpMd5, directory );
                    moveFileIfExists( tmpSha1, directory );
                    moveFileIfExists( tmpResource, directory );
                }
            }
        }
        finally
        {
            FileUtils.deleteQuietly( workingDirectory );
        }

        if ( executeConsumers )
        {
            // Just-in-time update of the index and database by executing the consumers for this artifact
            //consumers.executeConsumers( connector.getSourceRepository().getRepository(), resource );
            queueRepositoryTask( connector.getSourceRepository().getRepository().getId(), resource );
        }

        return resource;
    }

    private void queueRepositoryTask( String repositoryId, File localFile )
    {
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );
        task.setResourceFile( localFile );
        task.setUpdateRelatedArtifacts( true );
        task.setScanAll( true );

        try
        {
            scheduler.queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "Unable to queue repository task to execute consumers on resource file ['" + localFile.getName()
                           + "']." );
        }
    }

    /**
     * Moves the file into repository location if it exists
     *
     * @param fileToMove this could be either the main artifact, sha1 or md5 checksum file.
     * @param directory  directory to write files to
     */
    private void moveFileIfExists( File fileToMove, File directory )
        throws ProxyException
    {
        if ( fileToMove != null && fileToMove.exists() )
        {
            File newLocation = new File( directory, fileToMove.getName() );
            moveTempToTarget( fileToMove, newLocation );
        }
    }

    /**
     * <p>
     * Quietly transfer the checksum file from the remote repository to the local file.
     * </p>
     *
     * @param wagon            the wagon instance (should already be connected) to use.
     * @param remoteRepository the remote repository to transfer from.
     * @param remotePath       the remote path to the resource to get.
     * @param repository       the managed repository that will hold the file
     * @param resource         the local file that should contain the downloaded contents
     * @param tmpDirectory     the temporary directory to download to
     * @param ext              the type of checksum to transfer (example: ".md5" or ".sha1")
     * @throws ProxyException if copying the downloaded file into place did not succeed.
     */
    private File transferChecksum( Wagon wagon, RemoteRepositoryContent remoteRepository, String remotePath,
                                   ManagedRepositoryContent repository, File resource, File tmpDirectory, String ext )
        throws ProxyException
    {
        String url = remoteRepository.getURL().getUrl() + remotePath + ext;

        // Transfer checksum does not use the policy.
        if ( urlFailureCache.hasFailedBefore( url ) )
        {
            return null;
        }

        File destFile = new File( tmpDirectory, resource.getName() + ext );

        try
        {
            transferSimpleFile( wagon, remoteRepository, remotePath + ext, repository, resource, destFile );
            log.debug( "Checksum {} Downloaded: {} to move to {}", Arrays.asList( url, destFile, resource ).toArray() );
        }
        catch ( NotFoundException e )
        {
            urlFailureCache.cacheFailure( url );
            log.debug( "Transfer failed, checksum not found: {}", url );
            // Consume it, do not pass this on.
        }
        catch ( NotModifiedException e )
        {
            log.debug( "Transfer skipped, checksum not modified: {}", url );
            // Consume it, do not pass this on.
        }
        catch ( ProxyException e )
        {
            urlFailureCache.cacheFailure( url );
            log.warn( "Transfer failed on checksum: " + url + " : " + e.getMessage(), e );
            // Critical issue, pass it on.
            throw e;
        }
        return destFile;
    }

    /**
     * Perform the transfer of the remote file to the local file specified.
     *
     * @param wagon            the wagon instance to use.
     * @param remoteRepository the remote repository to use
     * @param remotePath       the remote path to attempt to get
     * @param repository       the managed repository that will hold the file
     * @param origFile         the local file to save to
     * @return The local file that was transfered.
     * @throws ProxyException if there was a problem moving the downloaded file into place.
     * @throws WagonException if there was a problem tranfering the file.
     */
    private void transferSimpleFile( Wagon wagon, RemoteRepositoryContent remoteRepository, String remotePath,
                                     ManagedRepositoryContent repository, File origFile, File destFile )
        throws ProxyException
    {
        assert ( remotePath != null );

        // Transfer the file.
        try
        {
            boolean success = false;

            if ( !origFile.exists() )
            {
                log.debug( "Retrieving {} from {}", remotePath, remoteRepository.getRepository().getName() );
                wagon.get( remotePath, destFile );
                success = true;

                // You wouldn't get here on failure, a WagonException would have been thrown.
                log.debug( "Downloaded successfully." );
            }
            else
            {
                log.debug( "Retrieving {} from {} if updated", remotePath, remoteRepository.getRepository().getName() );
                success = wagon.getIfNewer( remotePath, destFile, origFile.lastModified() );
                if ( !success )
                {
                    throw new NotModifiedException(
                        "Not downloaded, as local file is newer than remote side: " + origFile.getAbsolutePath() );
                }

                if ( destFile.exists() )
                {
                    log.debug( "Downloaded successfully." );
                }
            }
        }
        catch ( ResourceDoesNotExistException e )
        {
            throw new NotFoundException(
                "Resource [" + remoteRepository.getURL() + "/" + remotePath + "] does not exist: " + e.getMessage(),
                e );
        }
        catch ( WagonException e )
        {
            // TODO: shouldn't have to drill into the cause, but TransferFailedException is often not descriptive enough

            String msg =
                "Download failure on resource [" + remoteRepository.getURL() + "/" + remotePath + "]:" + e.getMessage();
            if ( e.getCause() != null )
            {
                msg += " (cause: " + e.getCause() + ")";
            }
            throw new ProxyException( msg, e );
        }
    }

    /**
     * Apply the policies.
     *
     * @param policies  the map of policies to execute. (Map of String policy keys, to {@link DownloadPolicy} objects)
     * @param settings  the map of settings for the policies to execute. (Map of String policy keys, to String policy
     *                  setting)
     * @param request   the request properties (utilized by the {@link DownloadPolicy#applyPolicy(String, Properties, File)}
     *                  )
     * @param localFile the local file (utilized by the {@link DownloadPolicy#applyPolicy(String, Properties, File)})
     */
    private void validatePolicies( Map<String, ? extends DownloadPolicy> policies, Map<String, String> settings,
                                   Properties request, File localFile )
        throws PolicyViolationException
    {
        for ( Entry<String, ? extends DownloadPolicy> entry : policies.entrySet() )
        {
            // olamy with spring rolehint is now downloadPolicy#hint
            // so substring after last # to get the hint as with plexus
            String key = StringUtils.substringAfterLast( entry.getKey(), "#" );
            DownloadPolicy policy = entry.getValue();
            String defaultSetting = policy.getDefaultOption();

            String setting = StringUtils.defaultString( settings.get( key ), defaultSetting );

            log.debug( "Applying [{}] policy with [{}]", key, setting );
            try
            {
                policy.applyPolicy( setting, request, localFile );
            }
            catch ( PolicyConfigurationException e )
            {
                log.error( e.getMessage(), e );
            }
        }
    }

    private void validatePolicies( Map<String, DownloadErrorPolicy> policies, Map<String, String> settings,
                                   Properties request, ArtifactReference artifact, RemoteRepositoryContent content,
                                   File localFile, ProxyException exception, Map<String, Exception> previousExceptions )
        throws ProxyDownloadException
    {
        boolean process = true;
        for ( Entry<String, ? extends DownloadErrorPolicy> entry : policies.entrySet() )
        {

            // olamy with spring rolehint is now downloadPolicy#hint
            // so substring after last # to get the hint as with plexus
            String key = StringUtils.substringAfterLast( entry.getKey(), "#" );
            DownloadErrorPolicy policy = entry.getValue();
            String defaultSetting = policy.getDefaultOption();
            String setting = StringUtils.defaultString( settings.get( key ), defaultSetting );

            log.debug( "Applying [{}] policy with [{}]", key, setting );
            try
            {
                // all policies must approve the exception, any can cancel
                process = policy.applyPolicy( setting, request, localFile, exception, previousExceptions );
                if ( !process )
                {
                    break;
                }
            }
            catch ( PolicyConfigurationException e )
            {
                log.error( e.getMessage(), e );
            }
        }

        if ( process )
        {
            // if the exception was queued, don't throw it
            if ( !previousExceptions.containsKey( content.getId() ) )
            {
                throw new ProxyDownloadException(
                    "An error occurred in downloading from the remote repository, and the policy is to fail immediately",
                    content.getId(), exception );
            }
        }
        else
        {
            // if the exception was queued, but cancelled, remove it
            previousExceptions.remove( content.getId() );
        }

        log.warn( "Transfer error from repository \"" + content.getRepository().getId() + "\" for artifact "
                      + Keys.toKey( artifact ) + ", continuing to next repository. Error message: "
                      + exception.getMessage() );
        log.debug( "Full stack trace", exception );
    }

    /**
     * Creates a working directory in the repository root for this request
     *
     * @param repository
     * @return file location of working directory
     * @throws IOException
     */
    private File createWorkingDirectory( ManagedRepositoryContent repository )
    {
        // TODO: This is ugly - lets actually clean this up when we get the new repository api
        try
        {
            File tmpDir = File.createTempFile( ".workingdirectory", null, new File( repository.getRepoRoot() ) );
            tmpDir.delete();
            tmpDir.mkdirs();
            return tmpDir;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Could not create working directory for this request", e );
        }
    }

    /**
     * Used to move the temporary file to its real destination. This is patterned from the way WagonManager handles its
     * downloaded files.
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

        target.getParentFile().mkdirs();
        if ( !temp.renameTo( target ) )
        {
            log.warn( "Unable to rename tmp file to its final name... resorting to copy command." );

            try
            {
                FileUtils.copyFile( temp, target );
            }
            catch ( IOException e )
            {
                if ( target.exists() )
                {
                    log.debug( "Tried to copy file {} to {} but file with this name already exists.", temp.getName(),
                               target.getAbsolutePath() );
                }
                else
                {
                    throw new ProxyException(
                        "Cannot copy tmp file " + temp.getAbsolutePath() + " to its final location", e );
                }
            }
            finally
            {
                FileUtils.deleteQuietly( temp );
            }
        }
    }

    /**
     * Using wagon, connect to the remote repository.
     *
     * @param connector        the connector configuration to utilize (for obtaining network proxy configuration from)
     * @param wagon            the wagon instance to establish the connection on.
     * @param remoteRepository the remote repository to connect to.
     * @return true if the connection was successful. false if not connected.
     */
    private boolean connectToRepository( ProxyConnector connector, Wagon wagon,
                                         RemoteRepositoryContent remoteRepository )
    {
        boolean connected = false;

        final ProxyInfo networkProxy;
        synchronized ( this.networkProxyMap )
        {
            networkProxy = (ProxyInfo) this.networkProxyMap.get( connector.getProxyId() );
        }

        if ( log.isDebugEnabled() )
        {
            if ( networkProxy != null )
            {
                // TODO: move to proxyInfo.toString()
                String msg = "Using network proxy " + networkProxy.getHost() + ":" + networkProxy.getPort()
                    + " to connect to remote repository " + remoteRepository.getURL();
                if ( networkProxy.getNonProxyHosts() != null )
                {
                    msg += "; excluding hosts: " + networkProxy.getNonProxyHosts();
                }
                if ( StringUtils.isNotBlank( networkProxy.getUserName() ) )
                {
                    msg += "; as user: " + networkProxy.getUserName();
                }
                log.debug( msg );
            }
        }

        AuthenticationInfo authInfo = null;
        String username = remoteRepository.getRepository().getUsername();
        String password = remoteRepository.getRepository().getPassword();

        if ( StringUtils.isNotBlank( username ) && StringUtils.isNotBlank( password ) )
        {
            log.debug( "Using username {} to connect to remote repository {}", username, remoteRepository.getURL() );
            authInfo = new AuthenticationInfo();
            authInfo.setUserName( username );
            authInfo.setPassword( password );
        }

        // Convert seconds to milliseconds
        int timeoutInMilliseconds = remoteRepository.getRepository().getTimeout() * 1000;

        // Set timeout
        wagon.setTimeout( timeoutInMilliseconds );

        try
        {
            Repository wagonRepository =
                new Repository( remoteRepository.getId(), remoteRepository.getURL().toString() );
            wagon.connect( wagonRepository, authInfo, networkProxy );
            connected = true;
        }
        catch ( ConnectionException e )
        {
            log.warn( "Could not connect to " + remoteRepository.getRepository().getName() + ": " + e.getMessage() );
            connected = false;
        }
        catch ( AuthenticationException e )
        {
            log.warn( "Could not connect to " + remoteRepository.getRepository().getName() + ": " + e.getMessage() );
            connected = false;
        }

        return connected;
    }

    /**
     * Tests whitelist and blacklist patterns against path.
     *
     * @param path     the path to test.
     * @param patterns the list of patterns to check.
     * @return true if the path matches at least 1 pattern in the provided patterns list.
     */
    private boolean matchesPattern( String path, List<String> patterns )
    {
        if ( CollectionUtils.isEmpty( patterns ) )
        {
            return false;
        }

        if ( !path.startsWith( "/" ) )
        {
            path = "/" + path;
        }

        for ( String pattern : patterns )
        {
            if ( !pattern.startsWith( "/" ) )
            {
                pattern = "/" + pattern;
            }

            if ( SelectorUtils.matchPath( pattern, path, false ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * TODO: Ensure that list is correctly ordered based on configuration. See MRM-477
     */
    public List<ProxyConnector> getProxyConnectors( ManagedRepositoryContent repository )
    {
        synchronized ( this.proxyConnectorMap )
        {
            List<ProxyConnector> ret = (List<ProxyConnector>) this.proxyConnectorMap.get( repository.getId() );
            if ( ret == null )
            {
                return Collections.emptyList();
            }

            Collections.sort( ret, ProxyConnectorOrderComparator.getInstance() );
            return ret;
        }
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isNetworkProxy( propertyName )
            || ConfigurationNames.isManagedRepositories( propertyName )
            || ConfigurationNames.isRemoteRepositories( propertyName ) || ConfigurationNames.isProxyConnector(
            propertyName ) )
        {
            initConnectorsAndNetworkProxies();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public RepositoryContentFactory getRepositoryFactory()
    {
        return repositoryFactory;
    }

    public void setRepositoryFactory( RepositoryContentFactory repositoryFactory )
    {
        this.repositoryFactory = repositoryFactory;
    }

    public MetadataTools getMetadataTools()
    {
        return metadataTools;
    }

    public void setMetadataTools( MetadataTools metadataTools )
    {
        this.metadataTools = metadataTools;
    }

    public UrlFailureCache getUrlFailureCache()
    {
        return urlFailureCache;
    }

    public void setUrlFailureCache( UrlFailureCache urlFailureCache )
    {
        this.urlFailureCache = urlFailureCache;
    }

    public WagonFactory getWagonFactory()
    {
        return wagonFactory;
    }

    public void setWagonFactory( WagonFactory wagonFactory )
    {
        this.wagonFactory = wagonFactory;
    }

    public Map<String, PreDownloadPolicy> getPreDownloadPolicies()
    {
        return preDownloadPolicies;
    }

    public void setPreDownloadPolicies( Map<String, PreDownloadPolicy> preDownloadPolicies )
    {
        this.preDownloadPolicies = preDownloadPolicies;
    }

    public Map<String, PostDownloadPolicy> getPostDownloadPolicies()
    {
        return postDownloadPolicies;
    }

    public void setPostDownloadPolicies( Map<String, PostDownloadPolicy> postDownloadPolicies )
    {
        this.postDownloadPolicies = postDownloadPolicies;
    }

    public Map<String, DownloadErrorPolicy> getDownloadErrorPolicies()
    {
        return downloadErrorPolicies;
    }

    public void setDownloadErrorPolicies( Map<String, DownloadErrorPolicy> downloadErrorPolicies )
    {
        this.downloadErrorPolicies = downloadErrorPolicies;
    }
}
