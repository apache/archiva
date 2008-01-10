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
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.NetworkProxyConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.Keys;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.RepositoryURL;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.policies.DownloadPolicy;
import org.apache.maven.archiva.policies.PolicyConfigurationException;
import org.apache.maven.archiva.policies.PolicyViolationException;
import org.apache.maven.archiva.policies.PostDownloadPolicy;
import org.apache.maven.archiva.policies.PreDownloadPolicy;
import org.apache.maven.archiva.policies.urlcache.UrlFailureCache;
import org.apache.maven.archiva.repository.ContentNotFoundException;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RemoteRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.layout.LayoutException;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * DefaultRepositoryProxyConnectors
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
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
    private Map<String, Wagon> wagons;

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
     * Fetch an artifact from a remote repository.
     *
     * @param repository the managed repository to utilize for the request.
     * @param artifact   the artifact reference to fetch.
     * @return the local file in the managed repository that was fetched, or null if the artifact was not (or
     *         could not be) fetched.
     * @throws ProxyException if there was a problem fetching the artifact.
     */
    public File fetchFromProxies( ManagedRepositoryContent repository, ArtifactReference artifact )
    {
        File localFile = toLocalFile( repository, artifact );

        Properties requestProperties = new Properties();
        requestProperties.setProperty( "filetype", "artifact" );
        requestProperties.setProperty( "version", artifact.getVersion() );

        List<ProxyConnector> connectors = getProxyConnectors( repository );
        for ( ProxyConnector connector : connectors )
        {
            RemoteRepositoryContent targetRepository = connector.getTargetRepository();
            String targetPath = targetRepository.toPath( artifact );

            try
            {
                File downloadedFile = transferFile( connector, targetRepository, targetPath, localFile,
                                                    requestProperties );

                if ( fileExists( downloadedFile ) )
                {
                    getLogger().debug( "Successfully transferred: " + downloadedFile.getAbsolutePath() );
                    return downloadedFile;
                }
            }
            catch ( NotFoundException e )
            {
                getLogger().debug( "Artifact " + Keys.toKey( artifact ) + " not found on repository \""
                                       + targetRepository.getRepository().getId() + "\"." );
            }
            catch ( NotModifiedException e )
            {
                getLogger().debug( "Artifact " + Keys.toKey( artifact ) + " not updated on repository \""
                                       + targetRepository.getRepository().getId() + "\"." );
            }
            catch ( ProxyException e )
            {
                getLogger().warn( "Transfer error from repository \"" + targetRepository.getRepository().getId() +
                    "\" for artifact " + Keys.toKey( artifact ) + ", continuing to next repository. Error message: " +
                    e.getMessage() );
                getLogger().debug( "Full stack trace", e );
            }
        }
        getLogger().debug( "Exhausted all target repositories, artifact " + Keys.toKey( artifact ) + " not found." );

        return null;
    }

    /**
     * Fetch, from the proxies, a metadata.xml file for the groupId:artifactId:version metadata contents.
     *
     * @return the (local) metadata file that was fetched/merged/updated, or null if no metadata file exists.
     */
    public File fetchFromProxies( ManagedRepositoryContent repository, VersionedReference metadata )
    {
        File localFile = toLocalFile( repository, metadata );

        Properties requestProperties = new Properties();
        requestProperties.setProperty( "filetype", "metadata" );
        boolean metadataNeedsUpdating = false;
        long originalTimestamp = getLastModified( localFile );

        List<ProxyConnector> connectors = getProxyConnectors( repository );
        for ( ProxyConnector connector : connectors )
        {
            RemoteRepositoryContent targetRepository = connector.getTargetRepository();
            String targetPath = metadataTools.toPath( metadata );

            File localRepoFile = toLocalRepoFile( repository, targetRepository, targetPath );
            long originalMetadataTimestamp = getLastModified( localRepoFile );

            try
            {
                transferFile( connector, targetRepository, targetPath, localRepoFile, requestProperties );

                if ( hasBeenUpdated( localRepoFile, originalMetadataTimestamp ) )
                {
                    metadataNeedsUpdating = true;
                }
            }
            catch ( NotFoundException e )
            {
                getLogger().debug( "Versioned Metadata " + Keys.toKey( metadata )
                                       + " not found on remote repository \""
                                       + targetRepository.getRepository().getId() + "\"." );
            }
            catch ( NotModifiedException e )
            {
                getLogger().debug( "Versioned Metadata " + Keys.toKey( metadata )
                                       + " not updated on remote repository \""
                                       + targetRepository.getRepository().getId() + "\"." );
            }
            catch ( ProxyException e )
            {
                getLogger().warn( "Transfer error from repository \"" + targetRepository.getRepository().getId() +
                    "\" for versioned Metadata " + Keys.toKey( metadata ) +
                    ", continuing to next repository. Error message: " + e.getMessage() );
                getLogger().debug( "Full stack trace", e );
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
                metadataTools.updateMetadata( repository, metadata );
            }
            catch ( LayoutException e )
            {
                getLogger().warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage() );
                // TODO: add into repository report?
            }
            catch ( RepositoryMetadataException e )
            {
                getLogger()
                    .warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage(), e );
                // TODO: add into repository report?
            }
            catch ( IOException e )
            {
                getLogger()
                    .warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage(), e );
                // TODO: add into repository report?
            }
            catch ( ContentNotFoundException e )
            {
                getLogger()
                    .warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage(), e );
                // TODO: add into repository report?
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

    /**
     * Fetch from the proxies a metadata.xml file for the groupId:artifactId metadata contents.
     *
     * @return the (local) metadata file that was fetched/merged/updated, or null if no metadata file exists.
     * @throws ProxyException if there was a problem fetching the metadata file.
     */
    public File fetchFromProxies( ManagedRepositoryContent repository, ProjectReference metadata )
    {
        File localFile = toLocalFile( repository, metadata );

        Properties requestProperties = new Properties();
        requestProperties.setProperty( "filetype", "metadata" );
        boolean metadataNeedsUpdating = false;
        long originalTimestamp = getLastModified( localFile );

        List<ProxyConnector> connectors = getProxyConnectors( repository );
        for ( ProxyConnector connector : connectors )
        {
            RemoteRepositoryContent targetRepository = connector.getTargetRepository();
            String targetPath = metadataTools.toPath( metadata );

            File localRepoFile = toLocalRepoFile( repository, targetRepository, targetPath );
            long originalMetadataTimestamp = getLastModified( localRepoFile );
            try
            {
                transferFile( connector, targetRepository, targetPath, localRepoFile, requestProperties );

                if ( hasBeenUpdated( localRepoFile, originalMetadataTimestamp ) )
                {
                    metadataNeedsUpdating = true;
                }
            }
            catch ( NotFoundException e )
            {
                getLogger().debug( "Project Metadata " + Keys.toKey( metadata ) + " not found on remote repository \""
                                       + targetRepository.getRepository().getId() + "\"." );
            }
            catch ( NotModifiedException e )
            {
                getLogger().debug( "Project Metadata " + Keys.toKey( metadata )
                                       + " not updated on remote repository \""
                                       + targetRepository.getRepository().getId() + "\"." );
            }
            catch ( ProxyException e )
            {
                getLogger().warn( "Transfer error from repository \"" + targetRepository.getRepository().getId() +
                    "\" for project metadata " + Keys.toKey( metadata ) +
                    ", continuing to next repository. Error message: " + e.getMessage() );
                getLogger().debug( "Full stack trace", e );
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
                metadataTools.updateMetadata( repository, metadata );
            }
            catch ( LayoutException e )
            {
                getLogger().warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage() );
                // TODO: add into repository report?
            }
            catch ( RepositoryMetadataException e )
            {
                getLogger()
                    .warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage(), e );
                // TODO: add into repository report?
            }
            catch ( IOException e )
            {
                getLogger()
                    .warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage(), e );
                // TODO: add into repository report?
            }
            catch ( ContentNotFoundException e )
            {
                getLogger()
                    .warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage(), e );
                // TODO: add into repository report?
            }
        }

        if ( fileExists( localFile ) )
        {
            return localFile;
        }

        return null;
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

    private File toLocalFile( ManagedRepositoryContent repository, ProjectReference metadata )
    {
        String sourcePath = metadataTools.toPath( metadata );
        return new File( repository.getRepoRoot(), sourcePath );
    }

    private File toLocalFile( ManagedRepositoryContent repository, VersionedReference metadata )
    {
        String sourcePath = metadataTools.toPath( metadata );
        return new File( repository.getRepoRoot(), sourcePath );
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
     * @param localFile         the local file to place the downloaded resource into
     * @param requestProperties the request properties to utilize for policy handling.
     * @return the local file that was downloaded, or null if not downloaded.
     * @throws NotFoundException if the file was not found on the remote repository.
     * @throws NotModifiedException if the localFile was present, and the resource was present on remote repository,
     *                              but the remote resource is not newer than the local File.
     * @throws ProxyException if transfer was unsuccessful.
     */
    private File transferFile( ProxyConnector connector, RemoteRepositoryContent remoteRepository, String remotePath,
                               File localFile, Properties requestProperties )
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
                getLogger().debug( "Path [" + remotePath +
                    "] is not part of defined whitelist (skipping transfer from repository [" +
                    remoteRepository.getRepository().getName() + "])." );
                return null;
            }
        }

        // Is target path part of blacklist?
        if ( matchesPattern( remotePath, connector.getBlacklist() ) )
        {
            getLogger().debug( "Path [" + remotePath + "] is part of blacklist (skipping transfer from repository [" +
                remoteRepository.getRepository().getName() + "])." );
            return null;
        }

        // Handle pre-download policy
        try
        {
            validatePolicies( this.preDownloadPolicies, connector.getPolicies(), requestProperties, localFile );
        }
        catch ( PolicyViolationException e )
        {
            String emsg = "Transfer not attempted on " + url + " : " + e.getMessage();
            if ( fileExists( localFile ) )
            {
                getLogger().info( emsg + ": using already present local file." );
                return localFile;
            }

            getLogger().info( emsg );
            return null;
        }
	
		if ( urlFailureCache.hasFailedBefore( url ) )
		{
			throw new NotFoundException( "Url has failed before and cache-failure is enabled on this connector" );
		}
			
        Wagon wagon = null;
        try
        {	
            RepositoryURL repoUrl = remoteRepository.getURL();
            String protocol = repoUrl.getProtocol();
            wagon = (Wagon) wagons.get( protocol );
            if ( wagon == null )
            {
                throw new ProxyException( "Unsupported target repository protocol: " + protocol );
            }

            boolean connected = connectToRepository( connector, wagon, remoteRepository );
            if ( connected )
            {
                localFile = transferSimpleFile( wagon, remoteRepository, remotePath, localFile );

                transferChecksum( wagon, remoteRepository, remotePath, localFile, ".sha1" );
                transferChecksum( wagon, remoteRepository, remotePath, localFile, ".md5" );
            }
        }
        catch ( NotFoundException e )
        {
			// public repositories may be slow to access, and many request will fail when 
			// muliple repositories are "merged" by archiva via proxies.
			// so caching "not found" is usefull here to enhance archiva response-time
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
                }
                catch ( ConnectionException e )
                {
                    getLogger().warn( "Unable to disconnect wagon.", e );
                }
            }
        }

        // Handle post-download policies.
        try
        {
            validatePolicies( this.postDownloadPolicies, connector.getPolicies(), requestProperties, localFile );
        }
        catch ( PolicyViolationException e )
        {
            getLogger().info( "Transfer invalidated from " + url + " : " + e.getMessage() );
            if ( fileExists( localFile ) )
            {
                return localFile;
            }

            return null;
        }

        // Just-in-time update of the index and database by executing the consumers for this artifact
        consumers.executeConsumers( connector.getSourceRepository().getRepository(), localFile );

        // Everything passes.
        return localFile;
    }

    /**
     * <p>
     * Quietly transfer the checksum file from the remote repository to the local file.
     * </p>
     *
     * @param wagon            the wagon instance (should already be connected) to use.
     * @param remoteRepository the remote repository to transfer from.
     * @param remotePath       the remote path to the resource to get.
     * @param localFile        the local file that should contain the downloaded contents
     * @param type             the type of checksum to transfer (example: ".md5" or ".sha1")
     * @throws ProxyException if copying the downloaded file into place did not succeed.
     */
    private void transferChecksum( Wagon wagon, RemoteRepositoryContent remoteRepository, String remotePath,
                                   File localFile, String type )
        throws ProxyException
    {
        String url = remoteRepository.getURL().getUrl() + remotePath;

        // Transfer checksum does not use the policy. 
        if ( urlFailureCache.hasFailedBefore( url + type ) )
        {
            return;
        }

        try
        {
            File hashFile = new File( localFile.getAbsolutePath() + type );
            transferSimpleFile( wagon, remoteRepository, remotePath + type, hashFile );
            getLogger().debug( "Checksum" + type + " Downloaded: " + hashFile );
        }
        catch ( NotFoundException e )
        {
            getLogger().debug( "Transfer failed, checksum not found: " + url );
            // Consume it, do not pass this on.
        }
        catch ( NotModifiedException e )
        {
            getLogger().debug( "Transfer skipped, checksum not modified: " + url );
            // Consume it, do not pass this on.
        }
        catch ( ProxyException e )
        {
            urlFailureCache.cacheFailure( url + type );
            getLogger().warn( "Transfer failed on checksum: " + url + " : " + e.getMessage(), e );
            // Critical issue, pass it on.
            throw e;
        }
    }

    /**
     * Perform the transfer of the remote file to the local file specified.
     *
     * @param wagon            the wagon instance to use.
     * @param remoteRepository the remote repository to use
     * @param remotePath       the remote path to attempt to get
     * @param localFile        the local file to save to
     * @return The local file that was transfered.
     * @throws ProxyException if there was a problem moving the downloaded file into place.
     * @throws WagonException if there was a problem tranfering the file.
     */
    private File transferSimpleFile( Wagon wagon, RemoteRepositoryContent remoteRepository, String remotePath,
                                     File localFile )
        throws ProxyException
    {
        assert ( remotePath != null );

        // Transfer the file.
        File temp = null;

        try
        {
            temp = new File( localFile.getAbsolutePath() + ".tmp" );

            boolean success = false;

            if ( !localFile.exists() )
            {
                getLogger().debug( "Retrieving " + remotePath + " from " + remoteRepository.getRepository().getName() );
                wagon.get( remotePath, temp );
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
                getLogger().debug( "Retrieving " + remotePath + " from " + remoteRepository.getRepository().getName()
                                       + " if updated" );
                success = wagon.getIfNewer( remotePath, temp, localFile.lastModified() );
                if ( !success )
                {
                    throw new NotModifiedException( "Not downloaded, as local file is newer than remote side: "
                                                    + localFile.getAbsolutePath() );
                }

                if ( temp.exists() )
                {
                    getLogger().debug( "Downloaded successfully." );
                    moveTempToTarget( temp, localFile );
                }
            }

            return localFile;
        }
        catch ( ResourceDoesNotExistException e )
        {
            throw new NotFoundException( "Resource [" + remoteRepository.getURL() + "/" + remotePath
                + "] does not exist: " + e.getMessage(), e );
        }
        catch ( WagonException e )
        {
            throw new ProxyException( "Download failure on resource [" + remoteRepository.getURL() + "/" + remotePath + "]:"
                                  + e.getMessage(), e );
        }
        finally
        {
            if ( temp != null )
            {
                temp.delete();
            }
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
            String key = (String) entry.getKey();
            DownloadPolicy policy = entry.getValue();
            String defaultSetting = policy.getDefaultOption();
            String setting = StringUtils.defaultString( (String) settings.get( key ), defaultSetting );

            getLogger().debug( "Applying [" + key + "] policy with [" + setting + "]" );
            try
            {
                policy.applyPolicy( setting, request, localFile );
            }
            catch ( PolicyConfigurationException e )
            {
                getLogger().error( e.getMessage(), e );
            }
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

    /**
     * Using wagon, connect to the remote repository.
     *
     * @param connector        the connector configuration to utilize (for obtaining network proxy configuration from)
     * @param wagon            the wagon instance to establish the connection on.
     * @param remoteRepository the remote repository to connect to.
     * @return true if the connection was successful. false if not connected.
     */
    private boolean connectToRepository( ProxyConnector connector, Wagon wagon, RemoteRepositoryContent remoteRepository )
    {
        boolean connected = false;

        ProxyInfo networkProxy = null;
        synchronized ( this.networkProxyMap )
        {
            networkProxy = (ProxyInfo) this.networkProxyMap.get( connector.getProxyId() );
        }

        try
        {
            AuthenticationInfo authInfo = null;
            String username = remoteRepository.getRepository().getUsername();
            String password = remoteRepository.getRepository().getPassword();

            if ( StringUtils.isNotBlank( username ) && StringUtils.isNotBlank( password ) )
            {
                getLogger().debug( "Using username " + username + " to connect to remote repository "
                                       + remoteRepository.getURL() );
                authInfo = new AuthenticationInfo();
                authInfo.setUserName( username );
                authInfo.setPassword( password );
            }
            else
            {
                getLogger().debug( "No authentication for remote repository needed" );
            }

            Repository wagonRepository = new Repository( remoteRepository.getId(), remoteRepository.getURL().toString() );
            if ( networkProxy != null )
            {
                wagon.connect( wagonRepository, authInfo, networkProxy );
            }
            else
            {
                wagon.connect( wagonRepository, authInfo );
            }
            connected = true;
        }
        catch ( ConnectionException e )
        {
            getLogger().warn(
                              "Could not connect to " + remoteRepository.getRepository().getName() + ": "
                                  + e.getMessage() );
            connected = false;
        }
        catch ( AuthenticationException e )
        {
            getLogger().warn(
                              "Could not connect to " + remoteRepository.getRepository().getName() + ": "
                                  + e.getMessage() );
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
                return Collections.EMPTY_LIST;
            }

            Collections.sort( ret, ProxyConnectorOrderComparator.getInstance() );
            return ret;
        }
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isNetworkProxy( propertyName )
            || ConfigurationNames.isManagedRepositories( propertyName )
            || ConfigurationNames.isRemoteRepositories( propertyName )
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
                    getLogger().warn( "Unable to use proxy connector: " + e.getMessage(), e );
                }
                catch ( RepositoryException e )
                {
                    getLogger().warn( "Unable to use proxy connector: " + e.getMessage(), e );
                }
            }

        }

        synchronized ( this.networkProxyMap )
        {
            this.networkProxyMap.clear();

            List<NetworkProxyConfiguration> networkProxies = archivaConfiguration.getConfiguration()
                .getNetworkProxies();
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
