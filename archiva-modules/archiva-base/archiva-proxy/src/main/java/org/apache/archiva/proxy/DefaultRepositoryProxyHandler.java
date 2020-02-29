package org.apache.archiva.proxy;

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

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksumUtil;
import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.ProxyConnectorRuleConfiguration;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.Keys;
import org.apache.archiva.policies.DownloadErrorPolicy;
import org.apache.archiva.policies.DownloadPolicy;
import org.apache.archiva.policies.Policy;
import org.apache.archiva.policies.PolicyConfigurationException;
import org.apache.archiva.policies.PolicyOption;
import org.apache.archiva.policies.PolicyViolationException;
import org.apache.archiva.policies.PostDownloadPolicy;
import org.apache.archiva.policies.PreDownloadPolicy;
import org.apache.archiva.policies.ProxyDownloadException;
import org.apache.archiva.policies.urlcache.UrlFailureCache;
import org.apache.archiva.proxy.model.NetworkProxy;
import org.apache.archiva.proxy.model.ProxyConnector;
import org.apache.archiva.proxy.model.ProxyFetchResult;
import org.apache.archiva.proxy.model.RepositoryProxyHandler;
import org.apache.archiva.components.taskqueue.TaskQueueException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RemoteRepositoryContent;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.metadata.base.MetadataTools;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.storage.fs.FsStorageUtil;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class DefaultRepositoryProxyHandler implements RepositoryProxyHandler {

    protected Logger log = LoggerFactory.getLogger( DefaultRepositoryProxyHandler.class );
    @Inject
    protected UrlFailureCache urlFailureCache;

    @Inject
    @Named(value = "metadataTools#default")
    private MetadataTools metadataTools;

    private Map<String, PreDownloadPolicy> preDownloadPolicies = new HashMap<>(  );
    private Map<String, PostDownloadPolicy> postDownloadPolicies = new HashMap<>(  );
    private Map<String, DownloadErrorPolicy> downloadErrorPolicies = new HashMap<>(  );
    private ConcurrentMap<String, List<ProxyConnector>> proxyConnectorMap = new ConcurrentHashMap<>();

    @Inject
    @Named(value = "archivaTaskScheduler#repository")
    private ArchivaTaskScheduler<RepositoryTask> scheduler;

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named(value = "fileLockManager#default")
    private FileLockManager fileLockManager;

    private Map<String, NetworkProxy> networkProxyMap = new ConcurrentHashMap<>();
    private List<ChecksumAlgorithm> checksumAlgorithms;

    @PostConstruct
    public void initialize()
    {
        checksumAlgorithms = ChecksumUtil.getAlgorithms(archivaConfiguration.getConfiguration().getArchivaRuntimeConfiguration().getChecksumTypes());
    }

    private List<ProxyConnectorRuleConfiguration> findProxyConnectorRules(String sourceRepository,
                                                                          String targetRepository,
                                                                          List<ProxyConnectorRuleConfiguration> all )
    {
        List<ProxyConnectorRuleConfiguration> proxyConnectorRuleConfigurations = new ArrayList<>();

        for ( ProxyConnectorRuleConfiguration proxyConnectorRuleConfiguration : all )
        {
            for ( ProxyConnectorConfiguration proxyConnector : proxyConnectorRuleConfiguration.getProxyConnectors() )
            {
                if ( StringUtils.equals( sourceRepository, proxyConnector.getSourceRepoId() ) && StringUtils.equals(
                    targetRepository, proxyConnector.getTargetRepoId() ) )
                {
                    proxyConnectorRuleConfigurations.add( proxyConnectorRuleConfiguration );
                }
            }
        }

        return proxyConnectorRuleConfigurations;
    }



    @Override
    public StorageAsset fetchFromProxies( ManagedRepository repository, ArtifactReference artifact )
        throws ProxyDownloadException
    {
        StorageAsset localFile = toLocalFile( repository, artifact );

        Properties requestProperties = new Properties();
        requestProperties.setProperty( "filetype", "artifact" );
        requestProperties.setProperty( "version", artifact.getVersion() );
        requestProperties.setProperty( "managedRepositoryId", repository.getId() );

        List<ProxyConnector> connectors = getProxyConnectors( repository );
        Map<String, Exception> previousExceptions = new LinkedHashMap<>();
        for ( ProxyConnector connector : connectors )
        {
            if ( !connector.isEnabled() )
            {
                continue;
            }

            RemoteRepository targetRepository = connector.getTargetRepository();
            requestProperties.setProperty( "remoteRepositoryId", targetRepository.getId() );

            String targetPath = targetRepository.getContent().toPath( artifact );

            if ( SystemUtils.IS_OS_WINDOWS )
            {
                // toPath use system PATH_SEPARATOR so on windows url are \ which doesn't work very well :-)
                targetPath = PathUtil.separatorsToUnix( targetPath );
            }

            try
            {
                StorageAsset downloadedFile =
                    transferFile( connector, targetRepository, targetPath, repository, localFile, requestProperties,
                                  true );

                if ( fileExists(downloadedFile) )
                {
                    log.debug( "Successfully transferred: {}", downloadedFile.getPath() );
                    return downloadedFile;
                }
            }
            catch ( NotFoundException e )
            {
                log.debug( "Artifact {} not found on repository \"{}\".", Keys.toKey( artifact ),
                           targetRepository.getId() );
            }
            catch ( NotModifiedException e )
            {
                log.debug( "Artifact {} not updated on repository \"{}\".", Keys.toKey( artifact ),
                           targetRepository.getId() );
            }
            catch ( ProxyException e )
            {
                validatePolicies( this.downloadErrorPolicies, connector.getPolicies(), requestProperties, artifact,
                                  targetRepository.getContent(), localFile, e, previousExceptions );
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

    @Override
    public StorageAsset fetchFromProxies( ManagedRepository repository, String path )
    {
        StorageAsset localFile = repository.getAsset( path );

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
            if ( !connector.isEnabled() )
            {
                continue;
            }

            RemoteRepository targetRepository = connector.getTargetRepository();
            requestProperties.setProperty( "remoteRepositoryId", targetRepository.getId() );

            String targetPath = path;

            try
            {
                StorageAsset downloadedFile =
                    transferFile( connector, targetRepository, targetPath, repository, localFile, requestProperties,
                                  false );

                if ( fileExists( downloadedFile ) )
                {
                    log.debug( "Successfully transferred: {}", downloadedFile.getPath() );
                    return downloadedFile;
                }
            }
            catch ( NotFoundException e )
            {
                log.debug( "Resource {} not found on repository \"{}\".", path,
                           targetRepository.getId() );
            }
            catch ( NotModifiedException e )
            {
                log.debug( "Resource {} not updated on repository \"{}\".", path,
                           targetRepository.getId() );
            }
            catch ( ProxyException e )
            {
                log.warn(
                    "Transfer error from repository {} for resource {}, continuing to next repository. Error message: {}",
                    targetRepository.getId(), path, e.getMessage() );
                log.debug( MarkerFactory.getDetachedMarker( "transfer.error" ),
                           "Transfer error from repository \"{}"
                               + "\" for resource {}, continuing to next repository. Error message: {}",
                           targetRepository.getId(), path, e.getMessage(), e );
            }

        }

        log.debug( "Exhausted all target repositories, resource {} not found.", path );

        return null;
    }

    @Override
    public ProxyFetchResult fetchMetadataFromProxies( ManagedRepository repository, String logicalPath )
    {
        StorageAsset localFile = repository.getAsset( logicalPath );

        Properties requestProperties = new Properties();
        requestProperties.setProperty( "filetype", "metadata" );
        boolean metadataNeedsUpdating = false;
        long originalTimestamp = getLastModified( localFile );

        List<ProxyConnector> connectors = new ArrayList<>( getProxyConnectors( repository ) );
        for ( ProxyConnector connector : connectors )
        {
            if ( !connector.isEnabled() )
            {
                continue;
            }

            RemoteRepository targetRepository = connector.getTargetRepository();

            StorageAsset localRepoFile = toLocalRepoFile( repository, targetRepository.getContent(), logicalPath );
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

                log.debug( "Metadata {} not found on remote repository '{}'.", logicalPath,
                           targetRepository.getId(), e );

            }
            catch ( NotModifiedException e )
            {

                log.debug( "Metadata {} not updated on remote repository '{}'.", logicalPath,
                           targetRepository.getId(), e );

            }
            catch ( ProxyException e )
            {
                log.warn(
                    "Transfer error from repository {} for versioned Metadata {}, continuing to next repository. Error message: {}",
                    targetRepository.getId(), logicalPath, e.getMessage() );
                log.debug( "Full stack trace", e );
            }
        }

        if ( hasBeenUpdated( localFile, originalTimestamp ) )
        {
            metadataNeedsUpdating = true;
        }

        if ( metadataNeedsUpdating || !localFile.exists())
        {
            try
            {
                metadataTools.updateMetadata( repository.getContent(), logicalPath );
            }
            catch ( RepositoryMetadataException e )
            {
                log.warn( "Unable to update metadata {}:{}", localFile.getPath(), e.getMessage(), e );
            }

        }

        if ( fileExists( localFile ) )
        {
            return new ProxyFetchResult( localFile, metadataNeedsUpdating );
        }

        return new ProxyFetchResult( null, false );
    }

    private long getLastModified(StorageAsset file )
    {
        if ( !file.exists() || file.isContainer() )
        {
            return 0;
        }

        return file.getModificationTime().toEpochMilli();
    }

    private boolean hasBeenUpdated(StorageAsset file, long originalLastModified )
    {
        if ( !file.exists() || file.isContainer() )
        {
            return false;
        }

        long currentLastModified = getLastModified( file );
        return ( currentLastModified > originalLastModified );
    }

    private StorageAsset toLocalRepoFile( ManagedRepository repository, RemoteRepositoryContent targetRepository,
                                          String targetPath )
    {
        String repoPath = metadataTools.getRepositorySpecificName( targetRepository, targetPath );
        return repository.getAsset( repoPath );
    }

    /**
     * Test if the provided ManagedRepositoryContent has any proxies configured for it.
     * @param repository
     */
    @Override
    public boolean hasProxies( ManagedRepository repository )
    {
        synchronized ( this.proxyConnectorMap )
        {
            return this.proxyConnectorMap.containsKey( repository.getId() );
        }
    }

    private StorageAsset toLocalFile(ManagedRepository repository, ArtifactReference artifact )
    {
        return repository.getContent().toFile( artifact );
    }

    /**
     * Simple method to test if the file exists on the local disk.
     *
     * @param file the file to test. (may be null)
     * @return true if file exists. false if the file param is null, doesn't exist, or is not of type File.
     */
    private boolean fileExists( StorageAsset file )
    {
        if ( file == null )
        {
            return false;
        }

        if ( !file.exists())
        {
            return false;
        }

        return !file.isContainer();
    }

    /**
     * Perform the transfer of the file.
     *
     * @param connector         the connector configuration to use.
     * @param remoteRepository  the remote repository get the resource from.
     * @param remotePath        the path in the remote repository to the resource to get.
     * @param repository        the managed repository that will hold the file
     * @param resource          the path relative to the repository storage where the file should be downloaded to
     * @param requestProperties the request properties to utilize for policy handling.
     * @param executeConsumers  whether to execute the consumers after proxying
     * @return the local file that was downloaded, or null if not downloaded.
     * @throws NotFoundException    if the file was not found on the remote repository.
     * @throws NotModifiedException if the localFile was present, and the resource was present on remote repository, but
     *                              the remote resource is not newer than the local File.
     * @throws ProxyException       if transfer was unsuccessful.
     */
    protected StorageAsset transferFile( ProxyConnector connector, RemoteRepository remoteRepository, String remotePath,
                                         ManagedRepository repository, StorageAsset resource, Properties requestProperties,
                                         boolean executeConsumers )
        throws ProxyException, NotModifiedException
    {
        String url = null;
        try
        {
            url = remoteRepository.getLocation().toURL().toString();
        }
        catch ( MalformedURLException e )
        {
            throw new ProxyException( e.getMessage(), e );
        }
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
                           remotePath, remoteRepository.getId() );
                return null;
            }
        }

        // Is target path part of blacklist?
        if ( matchesPattern( remotePath, connector.getBlacklist() ) )
        {
            log.debug( "Path [{}] is part of blacklist (skipping transfer from repository [{}]).", remotePath,
                       remoteRepository.getId() );
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
            if ( resource.exists() )
            {
                log.debug( "{} : using already present local file.", emsg );
                return resource;
            }

            log.debug( emsg );
            return null;
        }

        Path workingDirectory = createWorkingDirectory( repository );
        FilesystemStorage tmpStorage = null;
        try
        {
            tmpStorage = new FilesystemStorage( workingDirectory, fileLockManager );
        }
        catch ( IOException e )
        {
            throw new ProxyException( "Could not create tmp storage" );
        }
        StorageAsset tmpResource = tmpStorage.getAsset( resource.getName( ) );
        StorageAsset[] tmpChecksumFiles = new StorageAsset[checksumAlgorithms.size()];
        for(int i=0; i<checksumAlgorithms.size(); i++) {
            ChecksumAlgorithm alg = checksumAlgorithms.get( i );
            tmpChecksumFiles[i] = tmpStorage.getAsset( resource.getName() + "." + alg.getDefaultExtension() );
        }

        try
        {

            transferResources( connector, remoteRepository, tmpResource,tmpChecksumFiles , url, remotePath,
                resource, workingDirectory, repository );

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
                synchronized ( resource.getPath().intern() )
                {
                    StorageAsset directory = resource.getParent();
                    for (int i=0; i<tmpChecksumFiles.length; i++) {
                        moveFileIfExists( tmpChecksumFiles[i], directory );
                    }
                    moveFileIfExists( tmpResource, directory );
                }
            }
        }
        finally
        {
            org.apache.archiva.common.utils.FileUtils.deleteQuietly( workingDirectory );
        }

        if ( executeConsumers )
        {
            // Just-in-time update of the index and database by executing the consumers for this artifact
            //consumers.executeConsumers( connector.getSourceRepository().getRepository(), resource );
            queueRepositoryTask( connector.getSourceRepository().getId(), resource );
        }

        return resource;
    }

    protected abstract void transferResources( ProxyConnector connector, RemoteRepository remoteRepository,
                                               StorageAsset tmpResource, StorageAsset[] checksumFiles, String url, String remotePath, StorageAsset resource, Path workingDirectory,
                                               ManagedRepository repository ) throws ProxyException;

    private void queueRepositoryTask(String repositoryId, StorageAsset localFile )
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
            log.error( "Unable to queue repository task to execute consumers on resource file ['{}"
                           + "'].", localFile.getName() );
        }
    }

    /**
     * Moves the file into repository location if it exists
     *
     * @param fileToMove this could be either the main artifact, sha1 or md5 checksum file.
     * @param directory  directory to write files to
     */
    private void moveFileIfExists( StorageAsset fileToMove, StorageAsset directory )
        throws ProxyException
    {
        if ( fileToMove != null && fileToMove.exists() )
        {
            StorageAsset newLocation = directory.getStorage().getAsset( directory.getPath()+ "/" + fileToMove.getName());
            moveTempToTarget( fileToMove, newLocation );
        }
    }

    /**
     * Apply the policies.
     *
     * @param policies  the map of policies to execute. (Map of String policy keys, to {@link DownloadPolicy} objects)
     * @param settings  the map of settings for the policies to execute. (Map of String policy keys, to String policy
     *                  setting)
     * @param request   the request properties (utilized by the {@link DownloadPolicy#applyPolicy(PolicyOption, Properties, StorageAsset)}
     *                  )
     * @param localFile the local file (utilized by the {@link DownloadPolicy#applyPolicy(PolicyOption, Properties, StorageAsset)})
     * @throws PolicyViolationException
     */
    private void validatePolicies( Map<String, ? extends DownloadPolicy> policies, Map<Policy, PolicyOption> settings,
                                   Properties request, StorageAsset localFile )
        throws PolicyViolationException
    {
        for ( Map.Entry<String, ? extends DownloadPolicy> entry : policies.entrySet() )
        {
            // olamy with spring rolehint is now downloadPolicy#hint
            // so substring after last # to get the hint as with plexus
            String key = entry.getValue( ).getId( );
            DownloadPolicy policy = entry.getValue();
            PolicyOption option = settings.containsKey(policy ) ? settings.get(policy) : policy.getDefaultOption();

            log.debug( "Applying [{}] policy with [{}]", key, option );
            try
            {
                policy.applyPolicy( option, request, localFile );
            }
            catch ( PolicyConfigurationException e )
            {
                log.error( e.getMessage(), e );
            }
        }
    }

    private void validatePolicies( Map<String, DownloadErrorPolicy> policies, Map<Policy, PolicyOption> settings,
                                   Properties request, ArtifactReference artifact, RemoteRepositoryContent content,
                                   StorageAsset localFile, Exception exception, Map<String, Exception> previousExceptions )
        throws ProxyDownloadException
    {
        boolean process = true;
        for ( Map.Entry<String, ? extends DownloadErrorPolicy> entry : policies.entrySet() )
        {

            // olamy with spring rolehint is now downloadPolicy#hint
            // so substring after last # to get the hint as with plexus
            String key = entry.getValue( ).getId( );
            DownloadErrorPolicy policy = entry.getValue();
            PolicyOption option = settings.containsKey( policy ) ? settings.get(policy) : policy.getDefaultOption();

            log.debug( "Applying [{}] policy with [{}]", key, option );
            try
            {
                // all policies must approve the exception, any can cancel
                process = policy.applyPolicy( option, request, localFile, exception, previousExceptions );
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

        log.warn(
            "Transfer error from repository {} for artifact {} , continuing to next repository. Error message: {}",
            content.getRepository().getId(), Keys.toKey( artifact ), exception.getMessage() );
        log.debug( "Full stack trace", exception );
    }

    /**
     * Creates a working directory
     *
     * @param repository
     * @return file location of working directory
     */
    private Path createWorkingDirectory( ManagedRepository repository )
    {
        try
        {
            return Files.createTempDirectory( "temp" );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e.getMessage(), e );
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
    private void moveTempToTarget( StorageAsset temp, StorageAsset target )
        throws ProxyException
    {

        try
        {
            org.apache.archiva.repository.storage.util.StorageUtil.moveAsset( temp, target, true , StandardCopyOption.REPLACE_EXISTING);
        }
        catch ( IOException e )
        {
            log.error( "Move failed from {} to {}, trying copy.", temp, target );
            try
            {
                FsStorageUtil.copyAsset( temp, target, true );
                if (temp.exists()) {
                    temp.getStorage( ).removeAsset( temp );
                }
            }
            catch ( IOException ex )
            {
                log.error("Copy failed from {} to {}: ({}) {}", temp, target, e.getClass(), e.getMessage());
                throw new ProxyException("Could not move temp file "+temp.getPath()+" to target "+target.getPath()+": ("+e.getClass()+") "+e.getMessage(), e);
            }
        }
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

            if ( PathUtil.matchPath( pattern, path, false ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * TODO: Ensure that list is correctly ordered based on configuration. See MRM-477
     * @param repository
     */
    @Override
    public List<ProxyConnector> getProxyConnectors( ManagedRepository repository )
    {

        if ( !this.proxyConnectorMap.containsKey( repository.getId() ) )
        {
            return Collections.emptyList();
        }
        List<ProxyConnector> ret = new ArrayList<>( this.proxyConnectorMap.get( repository.getId() ) );

        Collections.sort( ret, ProxyConnectorOrderComparator.getInstance() );
        return ret;

    }


    protected String addParameters(String path, RemoteRepository remoteRepository )
    {
        if ( remoteRepository.getExtraParameters().isEmpty() )
        {
            return path;
        }

        boolean question = false;

        StringBuilder res = new StringBuilder( path == null ? "" : path );

        for ( Map.Entry<String, String> entry : remoteRepository.getExtraParameters().entrySet() )
        {
            if ( !question )
            {
                res.append( '?' ).append( entry.getKey() ).append( '=' ).append( entry.getValue() );
            }
        }

        return res.toString();
    }

    public void setArchivaConfiguration(ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public MetadataTools getMetadataTools()
    {
        return metadataTools;
    }

    public void setMetadataTools(MetadataTools metadataTools )
    {
        this.metadataTools = metadataTools;
    }

    public UrlFailureCache getUrlFailureCache()
    {
        return urlFailureCache;
    }

    public void setUrlFailureCache(UrlFailureCache urlFailureCache )
    {
        this.urlFailureCache = urlFailureCache;
    }

    public Map<String, PreDownloadPolicy> getPreDownloadPolicies()
    {
        return preDownloadPolicies;
    }

    public void setPreDownloadPolicies(Map<String, PreDownloadPolicy> preDownloadPolicies )
    {
        this.preDownloadPolicies = preDownloadPolicies;
    }

    public Map<String, PostDownloadPolicy> getPostDownloadPolicies()
    {
        return postDownloadPolicies;
    }

    public void setPostDownloadPolicies(Map<String, PostDownloadPolicy> postDownloadPolicies )
    {
        this.postDownloadPolicies = postDownloadPolicies;
    }

    public Map<String, DownloadErrorPolicy> getDownloadErrorPolicies()
    {
        return downloadErrorPolicies;
    }

    public void setDownloadErrorPolicies(Map<String, DownloadErrorPolicy> downloadErrorPolicies )
    {
        this.downloadErrorPolicies = downloadErrorPolicies;
    }

    @Override
    public void setNetworkProxies(Map<String, NetworkProxy> networkProxies ) {
        this.networkProxyMap.clear();
        this.networkProxyMap.putAll( networkProxies );
    }

    @Override
    public NetworkProxy getNetworkProxy(String id) {
        return this.networkProxyMap.get(id);
    }

    @Override
    public Map<String, NetworkProxy> getNetworkProxies() {
        return this.networkProxyMap;
    }

    @Override
    public abstract List<RepositoryType> supports();

    @Override
    public void setPolicies( List<Policy> policyList )
    {
        preDownloadPolicies.clear();
        postDownloadPolicies.clear();
        downloadErrorPolicies.clear();
        for (Policy policy : policyList) {
            addPolicy( policy );
        }
    }

    void addPolicy(PreDownloadPolicy policy) {
        preDownloadPolicies.put( policy.getId( ), policy );
    }

    void addPolicy(PostDownloadPolicy policy) {
        postDownloadPolicies.put( policy.getId( ), policy );
    }
    void addPolicy(DownloadErrorPolicy policy) {
        downloadErrorPolicies.put( policy.getId( ), policy );
    }

    @Override
    public void addPolicy( Policy policy )
    {
        if (policy instanceof PreDownloadPolicy) {
            addPolicy( (PreDownloadPolicy)policy );
        } else if (policy instanceof PostDownloadPolicy) {
            addPolicy( (PostDownloadPolicy) policy );
        } else if (policy instanceof DownloadErrorPolicy) {
            addPolicy( (DownloadErrorPolicy) policy );
        } else {
            log.warn( "Policy not known: {}, {}", policy.getId( ), policy.getClass( ).getName( ) );
        }
    }

    @Override
    public void removePolicy( Policy policy )
    {
        final String id = policy.getId();
        if (preDownloadPolicies.containsKey( id )) {
            preDownloadPolicies.remove( id );
        } else if (postDownloadPolicies.containsKey( id )) {
            postDownloadPolicies.remove( id );
        } else if (downloadErrorPolicies.containsKey( id )) {
            downloadErrorPolicies.remove( id );
        }
    }

    @Override
    public void addProxyConnector( ProxyConnector connector )
    {
        final String sourceId = connector.getSourceRepository( ).getId( );
        List<ProxyConnector> connectors;
        if (proxyConnectorMap.containsKey( sourceId )) {
            connectors = proxyConnectorMap.get( sourceId );
        } else {
            connectors = new ArrayList<>( );
            proxyConnectorMap.put( sourceId, connectors );
        }
        connectors.add( connector );
    }

    @Override
    public void setProxyConnectors( List<ProxyConnector> proxyConnectors )
    {
        proxyConnectorMap.clear();
        for ( ProxyConnector connector : proxyConnectors )
        {
            addProxyConnector( connector );
        }
    }
}
