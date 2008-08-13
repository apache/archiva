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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

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
import org.apache.maven.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.codehaus.plexus.util.SelectorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultRepositoryProxyConnectors
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @todo exception handling needs work - "not modified" is not really an exceptional case, and it has more layers than your average brown onion
 * @plexus.component role-hint="default"
 */
public class DefaultRepositoryProxyConnectors
    implements RepositoryProxyConnectors, RegistryListener, Initializable
{
    private Logger log = LoggerFactory.getLogger( DefaultRepositoryProxyConnectors.class );

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement
     */
    private RepositoryContentFactory repositoryFactory;

    /**
     * @plexus.requirement
     */
    private MetadataTools metadataTools;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.policies.PreDownloadPolicy"
     */
    private Map<String, PreDownloadPolicy> preDownloadPolicies;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.policies.PostDownloadPolicy"
     */
    private Map<String, PostDownloadPolicy> postDownloadPolicies;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.policies.DownloadErrorPolicy"
     */
    private Map<String, DownloadErrorPolicy> downloadErrorPolicies;

    /**
     * @plexus.requirement role-hint="default"
     */
    private UrlFailureCache urlFailureCache;

    private Map<String, List<ProxyConnector>> proxyConnectorMap = new HashMap<String, List<ProxyConnector>>();

    private Map<String, ProxyInfo> networkProxyMap = new HashMap<String, ProxyInfo>();

    /**
     * @plexus.requirement
     */
    private RepositoryContentConsumers consumers;

    /**
     * @plexus.requirement
     */
    private WagonFactory wagonFactory;
    
    public File fetchFromProxies( ManagedRepositoryContent repository, ArtifactReference artifact )
        throws ProxyDownloadException
    {
        File workingDirectory = createWorkingDirectory(repository);
        try
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
                RemoteRepositoryContent targetRepository = connector.getTargetRepository();
                requestProperties.setProperty( "remoteRepositoryId", targetRepository.getId() );

                String targetPath = targetRepository.toPath( artifact );

                try
                {
                    File downloadedFile =
                        transferFile( connector, targetRepository, targetPath, repository, workingDirectory, localFile, requestProperties,
                                      true );

                    if ( fileExists( downloadedFile ) )
                    {
                        log.debug( "Successfully transferred: " + downloadedFile.getAbsolutePath() );
                        return downloadedFile;
                    }
                }
                catch ( NotFoundException e )
                {
                    log.debug( "Artifact " + Keys.toKey( artifact ) + " not found on repository \""
                                           + targetRepository.getRepository().getId() + "\"." );
                }
                catch ( NotModifiedException e )
                {
                    log.debug( "Artifact " + Keys.toKey( artifact ) + " not updated on repository \""
                                           + targetRepository.getRepository().getId() + "\"." );
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

            log.debug( "Exhausted all target repositories, artifact " + Keys.toKey( artifact ) + " not found." );
        }
        finally
        {
            FileUtils.deleteQuietly(workingDirectory);
        }

        return null;
    }

    public File fetchFromProxies( ManagedRepositoryContent repository, String path )
    {
        File workingDir = createWorkingDirectory(repository);
        try
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
                RemoteRepositoryContent targetRepository = connector.getTargetRepository();
                requestProperties.setProperty( "remoteRepositoryId", targetRepository.getId() );

                String targetPath = path;

                try
                {
                    File downloadedFile =
                        transferFile( connector, targetRepository, targetPath, repository, workingDir, localFile, requestProperties, false );

                    if ( fileExists( downloadedFile ) )
                    {
                        log.debug( "Successfully transferred: " + downloadedFile.getAbsolutePath() );
                        return downloadedFile;
                    }
                }
                catch ( NotFoundException e )
                {
                    log.debug( "Resource " + path + " not found on repository \""
                                           + targetRepository.getRepository().getId() + "\"." );
                }
                catch ( NotModifiedException e )
                {
                    log.debug( "Resource " + path + " not updated on repository \""
                                           + targetRepository.getRepository().getId() + "\"." );
                }
                catch ( ProxyException e )
                {
                    log.warn( "Transfer error from repository \"" + targetRepository.getRepository().getId()
                        + "\" for resource " + path + ", continuing to next repository. Error message: " + e.getMessage() );
                    log.debug( "Full stack trace", e );
                }
            }

            log.debug( "Exhausted all target repositories, resource " + path + " not found." );
        }
        finally
        {
            FileUtils.deleteQuietly(workingDir);
        }

        return null;
    }
    
    public File fetchMetatadaFromProxies(ManagedRepositoryContent repository, String logicalPath)
    {
        File workingDir = createWorkingDirectory(repository);
        try
        {
            File localFile = new File(repository.getRepoRoot(), logicalPath);

            Properties requestProperties = new Properties();
            requestProperties.setProperty( "filetype", "metadata" );
            boolean metadataNeedsUpdating = false;
            long originalTimestamp = getLastModified( localFile );

            List<ProxyConnector> connectors = getProxyConnectors( repository );
            for ( ProxyConnector connector : connectors )
            {
                RemoteRepositoryContent targetRepository = connector.getTargetRepository();

                File localRepoFile = toLocalRepoFile( repository, targetRepository, logicalPath );
                long originalMetadataTimestamp = getLastModified( localRepoFile );

                try
                {
                    transferFile( connector, targetRepository, logicalPath, repository, workingDir, localRepoFile, requestProperties, true );

                    if ( hasBeenUpdated( localRepoFile, originalMetadataTimestamp ) )
                    {
                        metadataNeedsUpdating = true;
                    }
                }
                catch ( NotFoundException e )
                {
                    log.debug( "Metadata " + logicalPath
                                           + " not found on remote repository \""
                                           + targetRepository.getRepository().getId() + "\".", e );
                }
                catch ( NotModifiedException e )
                {
                    log.debug( "Metadata " + logicalPath
                                           + " not updated on remote repository \""
                                           + targetRepository.getRepository().getId() + "\".", e );
                }
                catch ( ProxyException e )
                {
                    log.warn( "Transfer error from repository \"" + targetRepository.getRepository().getId() +
                        "\" for versioned Metadata " + logicalPath +
                        ", continuing to next repository. Error message: " + e.getMessage() );
                    log.debug( "Full stack trace", e );
                }
            }

            if ( hasBeenUpdated( localFile, originalTimestamp ) )
            {
                metadataNeedsUpdating = true;
            }

            if ( metadataNeedsUpdating )
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
        }
        finally
        {
            FileUtils.deleteQuietly(workingDir);
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
     * @throws NotModifiedException if the localFile was present, and the resource was present on remote repository,
     *                              but the remote resource is not newer than the local File.
     * @throws ProxyException       if transfer was unsuccessful.
     */
    private File transferFile( ProxyConnector connector, RemoteRepositoryContent remoteRepository, String remotePath,
                               ManagedRepositoryContent repository, File workingDirectory, File resource, Properties requestProperties,
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
                log.debug( "Path [" + remotePath +
                    "] is not part of defined whitelist (skipping transfer from repository [" +
                    remoteRepository.getRepository().getName() + "])." );
                return null;
            }
        }

        // Is target path part of blacklist?
        if ( matchesPattern( remotePath, connector.getBlacklist() ) )
        {
            log.debug( "Path [" + remotePath + "] is part of blacklist (skipping transfer from repository [" +
                remoteRepository.getRepository().getName() + "])." );
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
                log.info( emsg + ": using already present local file." );
                return resource;
            }

            log.info( emsg );
            return null;
        }

        // MRM-631 - the lightweight wagon does not reset these - remove if we switch to httpclient based wagon
        String previousHttpProxyHost = System.getProperty( "http.proxyHost" );
        String previousHttpProxyPort = System.getProperty( "http.proxyPort" );
        String previousProxyExclusions = System.getProperty( "http.nonProxyHosts" );

        File tmpMd5 = null;
        File tmpSha1 = null;
        File tmpResource = null;
        
        Wagon wagon = null;
        try
        {
            RepositoryURL repoUrl = remoteRepository.getURL();
            String protocol = repoUrl.getProtocol();
            wagon = (Wagon) wagonFactory.getWagon( "wagon#" + protocol );
            if ( wagon == null )
            {
                throw new ProxyException( "Unsupported target repository protocol: " + protocol );
            }

            boolean connected = connectToRepository( connector, wagon, remoteRepository );
            if ( connected )
            {
                tmpResource = transferSimpleFile( wagon, remoteRepository, remotePath, repository, workingDirectory, resource );

                // TODO: these should be used to validate the download based on the policies, not always downloaded to
                //   save on connections since md5 is rarely used
                tmpSha1 = transferChecksum( wagon, remoteRepository, remotePath, repository, workingDirectory, resource, ".sha1" );
                tmpMd5 = transferChecksum( wagon, remoteRepository, remotePath, repository, workingDirectory, resource, ".md5" );
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
        finally
        {
            if ( wagon != null )
            {
                try
                {
                    wagon.disconnect();

                    // MRM-631 - the lightweight wagon does not reset these - remove if we switch to httpclient based wagon
                    if ( previousHttpProxyHost != null )
                    {
                        System.setProperty( "http.proxyHost", previousHttpProxyHost );
                    }
                    else
                    {
                        System.getProperties().remove( "http.proxyHost" );
                    }
                    if ( previousHttpProxyPort != null )
                    {
                        System.setProperty( "http.proxyPort", previousHttpProxyPort );
                    }
                    else
                    {
                        System.getProperties().remove( "http.proxyPort" );
                    }
                    if ( previousProxyExclusions != null )
                    {
                        System.setProperty( "http.nonProxyHosts", previousProxyExclusions );
                    }
                    else
                    {
                        System.getProperties().remove( "http.nonProxyHosts" );
                    }
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
            log.info( "Transfer invalidated from " + url + " : " + e.getMessage() );
            executeConsumers = false;
            if ( !fileExists( tmpResource ) )
            {
                resource = null;
            }
        }

        if (resource != null)
        {
            synchronized (resource.getAbsolutePath().intern())
            {
                File directory = resource.getParentFile();
                moveFileIfExists(tmpMd5, directory);
                moveFileIfExists(tmpSha1, directory);
                moveFileIfExists(tmpResource, directory);
            }
        }

        if ( executeConsumers )
        {
            // Just-in-time update of the index and database by executing the consumers for this artifact
            consumers.executeConsumers( connector.getSourceRepository().getRepository(), resource );
        }
        
        return resource;
    }
    
    
    
    /**
     * Moves the file into repository location if it exists
     * 
     * @param fileToMove this could be either the main artifact, sha1 or md5 checksum file.
     * @param directory directory to write files to
     */
    private void moveFileIfExists(File fileToMove, File directory) throws ProxyException
    {
        if (fileToMove != null && fileToMove.exists())
        {
            File newLocation = new File(directory, fileToMove.getName());
            moveTempToTarget(fileToMove, newLocation);
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
     * @param localFile        the local file that should contain the downloaded contents
     * @param type             the type of checksum to transfer (example: ".md5" or ".sha1")
     * @throws ProxyException if copying the downloaded file into place did not succeed.
     */
    private File transferChecksum( Wagon wagon, RemoteRepositoryContent remoteRepository, String remotePath,
                                   ManagedRepositoryContent repository, File workingDirectory, File localFile, String type )
        throws ProxyException
    {
        File hashFile = new File( localFile.getAbsolutePath() + type );
        File tmpChecksum = new File(workingDirectory, hashFile.getName());
        String url = remoteRepository.getURL().getUrl() + remotePath;

        // Transfer checksum does not use the policy.
        if ( urlFailureCache.hasFailedBefore( url + type ) )
        {
            return null;
        }

        try
        {
            transferSimpleFile( wagon, remoteRepository, remotePath + type, repository, workingDirectory, hashFile );
            log.debug( "Checksum" + type + " Downloaded: " + hashFile );
        }
        catch ( NotFoundException e )
        {
            urlFailureCache.cacheFailure( url + type );
            log.debug( "Transfer failed, checksum not found: " + url );
            // Consume it, do not pass this on.
        }
        catch ( NotModifiedException e )
        {
            log.debug( "Transfer skipped, checksum not modified: " + url );
            // Consume it, do not pass this on.
        }
        catch ( ProxyException e )
        {
            urlFailureCache.cacheFailure( url + type );
            log.warn( "Transfer failed on checksum: " + url + " : " + e.getMessage(), e );
            // Critical issue, pass it on.
            throw e;
        }
        return tmpChecksum;
    }

    /**
     * Perform the transfer of the remote file to the local file specified.
     *
     * @param wagon            the wagon instance to use.
     * @param remoteRepository the remote repository to use
     * @param remotePath       the remote path to attempt to get
     * @param repository       the managed repository that will hold the file
     * @param localFile        the local file to save to
     * @return The local file that was transfered.
     * @throws ProxyException if there was a problem moving the downloaded file into place.
     * @throws WagonException if there was a problem tranfering the file.
     */
    private File transferSimpleFile( Wagon wagon, RemoteRepositoryContent remoteRepository, String remotePath,
                                     ManagedRepositoryContent repository, File workingDirectory, File localFile )
        throws ProxyException
    {
        assert ( remotePath != null );

        // Transfer the file.
        File temp = null;

        try
        {
            temp = new File(workingDirectory, localFile.getName());

            boolean success = false;

            if ( !localFile.exists() )
            {
                log.debug( "Retrieving " + remotePath + " from " + remoteRepository.getRepository().getName() );
                wagon.get( remotePath, temp );
                success = true;

                // You wouldn't get here on failure, a WagonException would have been thrown.
                log.debug( "Downloaded successfully." );
            }
            else
            {
                log.debug( "Retrieving " + remotePath + " from " + remoteRepository.getRepository().getName()
                                       + " if updated" );
                success = wagon.getIfNewer( remotePath, temp, localFile.lastModified() );
                if ( !success )
                {
                    throw new NotModifiedException(
                        "Not downloaded, as local file is newer than remote side: " + localFile.getAbsolutePath() );
                }

                if ( temp.exists() )
                {
                    log.debug( "Downloaded successfully." );
                }
            }

            return temp;
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
     * @param settings  the map of settings for the policies to execute. (Map of String policy keys, to String policy setting)
     * @param request   the request properties (utilized by the {@link DownloadPolicy#applyPolicy(String,Properties,File)})
     * @param localFile the local file (utilized by the {@link DownloadPolicy#applyPolicy(String,Properties,File)})
     */
    private void validatePolicies( Map<String, ? extends DownloadPolicy> policies, Map<String, String> settings,
                                   Properties request, File localFile )
        throws PolicyViolationException
    {
        for ( Entry<String, ? extends DownloadPolicy> entry : policies.entrySet() )
        {
            String key = entry.getKey();
            DownloadPolicy policy = entry.getValue();
            String defaultSetting = policy.getDefaultOption();
            String setting = StringUtils.defaultString( settings.get( key ), defaultSetting );

            log.debug( "Applying [" + key + "] policy with [" + setting + "]" );
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
            String key = entry.getKey();
            DownloadErrorPolicy policy = entry.getValue();
            String defaultSetting = policy.getDefaultOption();
            String setting = StringUtils.defaultString( settings.get( key ), defaultSetting );

            log.debug( "Applying [" + key + "] policy with [" + setting + "]" );
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

        log.warn( "Transfer error from repository \"" + content.getRepository().getId() + "\" for artifact " +
            Keys.toKey( artifact ) + ", continuing to next repository. Error message: " + exception.getMessage() );
        log.debug( "Full stack trace", exception );
    }
    
    /**
     * Creates a working directory in the repository root for this request
     * @param repository
     * @return file location of working directory
     */
    private File createWorkingDirectory(ManagedRepositoryContent repository)
    {
        //TODO: This is ugly - lets actually clean this up when we get the new repository api
        try
        {
            File tmpDir = File.createTempFile(".workingdirectory", null, new File(repository.getRepoRoot()));
            tmpDir.delete();
            tmpDir.mkdirs();
            return tmpDir;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not create working directory for this request", e);
        }
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
                if (target.exists())
                {
                    log.debug("Tried to copy file " + temp.getName() + " to " + target.getAbsolutePath() + " but file with this name already exists.");
                }
                else
                {
                    throw new ProxyException( "Cannot copy tmp file " + temp.getAbsolutePath() + " to its final location", e );
                }
            }
            finally
            {
                FileUtils.deleteQuietly(temp);
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
                String msg =
                    "Using network proxy " + networkProxy.getHost() + ":" + networkProxy.getPort()
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
            log.debug( "Using username " + username + " to connect to remote repository "
                + remoteRepository.getURL() );
            authInfo = new AuthenticationInfo();
            authInfo.setUserName( username );
            authInfo.setPassword( password );
        }

        //Convert seconds to milliseconds
        int timeoutInMilliseconds = remoteRepository.getRepository().getTimeout() * 1000;

        //Set timeout
        wagon.setTimeout(timeoutInMilliseconds);

        try
        {
            Repository wagonRepository = new Repository( remoteRepository.getId(), remoteRepository.getURL().toString() );
            wagon.connect( wagonRepository, authInfo, networkProxy );
            connected = true;
        }
        catch ( ConnectionException e )
        {
            log.warn(
                "Could not connect to " + remoteRepository.getRepository().getName() + ": " + e.getMessage() );
            connected = false;
        }
        catch ( AuthenticationException e )
        {
            log.warn(
                "Could not connect to " + remoteRepository.getRepository().getName() + ": " + e.getMessage() );
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

        for ( String pattern : patterns )
        {
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
        if ( ConfigurationNames.isNetworkProxy( propertyName ) ||
            ConfigurationNames.isManagedRepositories( propertyName ) ||
            ConfigurationNames.isRemoteRepositories( propertyName ) ||
            ConfigurationNames.isProxyConnector( propertyName ) )
        {
            initConnectorsAndNetworkProxies();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    @SuppressWarnings("unchecked")
    private void initConnectorsAndNetworkProxies()
    {
        synchronized ( this.proxyConnectorMap )
        {
            ProxyConnectorOrderComparator proxyOrderSorter = new ProxyConnectorOrderComparator();
            this.proxyConnectorMap.clear();

            List<ProxyConnectorConfiguration> proxyConfigs = archivaConfiguration.getConfiguration()
                .getProxyConnectors();
            for ( ProxyConnectorConfiguration proxyConfig : proxyConfigs )
            {
                String key = proxyConfig.getSourceRepoId();

                try
                {
                    // Create connector object.
                    ProxyConnector connector = new ProxyConnector();

                    connector.setSourceRepository( repositoryFactory.getManagedRepositoryContent( proxyConfig
                        .getSourceRepoId() ) );
                    connector.setTargetRepository( repositoryFactory.getRemoteRepositoryContent( proxyConfig
                        .getTargetRepoId() ) );

                    connector.setProxyId( proxyConfig.getProxyId() );
                    connector.setPolicies( proxyConfig.getPolicies() );
                    connector.setOrder( proxyConfig.getOrder() );

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

            List<NetworkProxyConfiguration> networkProxies = archivaConfiguration.getConfiguration().getNetworkProxies();
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

    public void initialize()
        throws InitializationException
    {
        initConnectorsAndNetworkProxies();
        archivaConfiguration.addChangeListener( this );
    }
}
