package org.apache.maven.repository.proxy;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.ChecksumFailedException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.ArtifactUtils;
import org.apache.maven.repository.proxy.configuration.ProxyConfiguration;
import org.apache.maven.repository.proxy.repository.ProxyRepository;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.ChecksumObserver;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Edwin Punzalan
 * @plexus.component role="org.apache.maven.repository.proxy.ProxyManager"
 * @todo too much of wagon manager is reproduced here because checksums need to be downloaded separately - is that necessary?
 * @todo this isn't reusing the parts of wagon manager than handle snapshots
 */
public class DefaultProxyManager
    extends AbstractLogEnabled
    implements ProxyManager
{
    /**
     * @plexus.requirement
     */
    private WagonManager wagonManager;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * @plexus.requirement
     */
    private ProxyConfiguration config;

    /**
     * @plexus.requirement role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayoutMap;

    /**
     * A map
     */
    private Map failuresCache = new HashMap();

    public void setConfiguration( ProxyConfiguration config )
    {
        this.config = config;
    }

    public ProxyConfiguration getConfiguration()
    {
        return config;
    }

    /**
     * @see org.apache.maven.repository.proxy.ProxyManager#get(String)
     */
    public File get( String path )
        throws ProxyException, ResourceDoesNotExistException
    {
        checkConfiguration();

        //@todo use wagonManager for cache use file:// as URL
        String cachePath = config.getRepositoryCachePath();
        File cachedFile = new File( cachePath, path );
        if ( !cachedFile.exists() )
        {
            cachedFile = getRemoteFile( path );
        }
        return cachedFile;
    }

    /**
     * @see org.apache.maven.repository.proxy.ProxyManager#getRemoteFile(String)
     */
    public File getRemoteFile( String path )
        throws ProxyException, ResourceDoesNotExistException
    {
        checkConfiguration();

        return getRemoteFile( path, config.getRepositories() );
    }

    /**
     * Tries to download the path from the list of repositories.
     *
     * @param path         the request path to download from the proxy or repositories
     * @param repositories list of ArtifactRepositories to download the path from
     * @return File object that points to the downloaded file
     * @throws ProxyException
     * @throws ResourceDoesNotExistException
     */
    private File getRemoteFile( String path, List repositories )
        throws ProxyException, ResourceDoesNotExistException
    {
        checkConfiguration();

        File remoteFile;
        if ( path.endsWith( ".md5" ) || path.endsWith( ".sha1" ) )
        {
            remoteFile = getRepositoryFile( path, repositories, false );
        }
        else if ( path.endsWith( "maven-metadata.xml" ) )
        {
            remoteFile = getRepositoryFile( path, repositories );
        }
        else
        {
            Artifact artifact = ArtifactUtils.buildArtifact( path, artifactFactory );

            if ( artifact == null )
            {
                artifact = ArtifactUtils.buildArtifactFromLegacyPath( path, artifactFactory );
            }

            if ( artifact != null )
            {
                getArtifact( artifact, repositories );

                remoteFile = artifact.getFile();
            }
            else
            {
                //try downloading non-maven standard files
                remoteFile = getRepositoryFile( path, repositories );
            }
        }

        return remoteFile;
    }

    /**
     * Used to download an artifact object from the remote repositories.
     *
     * @param artifact     the artifact object to be downloaded from a remote repository
     * @param repositories the list of ProxyRepositories to retrieve the artifact from
     * @throws ProxyException                when an error occurred during retrieval of the requested artifact
     * @throws ResourceDoesNotExistException when the requested artifact cannot be found in any of the
     *                                       configured repositories
     */
    private void getArtifact( Artifact artifact, List repositories )
        throws ResourceDoesNotExistException, ProxyException
    {
        ArtifactRepository repoCache = getRepositoryCache();

        File artifactFile = new File( repoCache.getBasedir(), repoCache.pathOf( artifact ) );
        artifact.setFile( artifactFile );

        if ( !artifactFile.exists() )
        {
            for ( Iterator iter = repositories.iterator(); iter.hasNext(); )
            {
                ProxyRepository repository = (ProxyRepository) iter.next();
                try
                {
                    if ( checkIfFailureCached( repository.pathOf( artifact ), repository ) )
                    {
                        getLogger().debug(
                            "Skipping repository " + repository.getKey() + " for a cached path failure." );
                    }
                    else
                    {
                        wagonManager.getArtifact( artifact, repository );
                    }
                }
                catch ( TransferFailedException e )
                {
                    if ( repository.isHardfail() )
                    {
                        throw new ProxyException( e.getMessage(), e );
                    }
                }
                catch ( ResourceDoesNotExistException e )
                {
                    //handle the failure cache then throw exception as expected
                    doCacheFailure( repository.pathOf( artifact ), repository );

                    throw e;
                }
            }
        }
    }

    private void doCacheFailure( String path, ProxyRepository repository )
    {
        if ( repository.isCacheFailures() )
        {
            String key = repository.getKey();
            if ( !failuresCache.containsKey( key ) )
            {
                failuresCache.put( key, new ArrayList() );
            }

            List failureCache = (List) failuresCache.get( key );
            if ( !failureCache.contains( path ) )
            {
                failureCache.add( path );
            }
        }
    }

    private boolean checkIfFailureCached( String path, ProxyRepository repository )
    {
        boolean pathAlreadyFailed = false;

        if ( repository.isCacheFailures() )
        {
            String key = repository.getKey();

            if ( failuresCache.containsKey( key ) )
            {
                List failureCache = (List) failuresCache.get( key );

                if ( failureCache.contains( path ) )
                {
                    pathAlreadyFailed = true;
                }
            }
        }

        return pathAlreadyFailed;
    }

    private ArtifactRepositoryLayout getLayout()
        throws ProxyException
    {
        String configLayout = config.getLayout();

        if ( !repositoryLayoutMap.containsKey( configLayout ) )
        {
            throw new ProxyException( "Unable to find a proxy repository layout for " + configLayout );
        }

        return (ArtifactRepositoryLayout) repositoryLayoutMap.get( configLayout );
    }

    private ArtifactRepository getRepositoryCache()
        throws ProxyException
    {
        return repositoryFactory.createArtifactRepository( "local-cache", getRepositoryCacheURL().toString(),
                                                           getLayout(), getSnapshotsPolicy(), getReleasesPolicy() );
    }

    private ArtifactRepositoryPolicy getReleasesPolicy()
    {
        //todo get policy configuration from ProxyConfiguration
        return new ArtifactRepositoryPolicy();
    }

    private ArtifactRepositoryPolicy getSnapshotsPolicy()
    {
        //todo get policy configuration from ProxyConfiguration
        return new ArtifactRepositoryPolicy();
    }

    public URL getRepositoryCacheURL()
        throws ProxyException
    {
        URL url;

        try
        {
            url = new File( config.getRepositoryCachePath() ).toURL();
        }
        catch ( MalformedURLException e )
        {
            throw new ProxyException( "Unable to create cache URL from: " + config.getRepositoryCachePath(), e );
        }

        return url;
    }

    /**
     * Used to retrieve a remote file from the remote repositories.  This method is used only when the requested
     * path cannot be resolved into a repository object, for example, an Artifact.
     *
     * @param path         the remote path to use to search for the requested file
     * @param repositories the list of repositories to retrieve the file from
     * @return File object representing the remote file in the repository cache
     * @throws ResourceDoesNotExistException when the requested path cannot be found in any of the configured
     *                                       repositories.
     * @throws ProxyException                when an error occurred during the retrieval of the requested path
     */
    private File getRepositoryFile( String path, List repositories )
        throws ResourceDoesNotExistException, ProxyException
    {
        return getRepositoryFile( path, repositories, true );
    }

    /**
     * Used to retrieve a remote file from the remote repositories.  This method is used only when the requested
     * path cannot be resolved into a repository object, for example, an Artifact.
     *
     * @param path         the remote path to use to search for the requested file
     * @param repositories the list of repositories to retrieve the file from
     * @param useChecksum  forces the download to whether use a checksum (if present in the remote repository) or not
     * @return File object representing the remote file in the repository cache
     * @throws ResourceDoesNotExistException when the requested path cannot be found in any of the configured
     *                                       repositories.
     * @throws ProxyException                when an error occurred during the retrieval of the requested path
     */
    private File getRepositoryFile( String path, List repositories, boolean useChecksum )
        throws ResourceDoesNotExistException, ProxyException
    {
        ArtifactRepository cache = getRepositoryCache();
        File target = new File( cache.getBasedir(), path );

        for ( Iterator repos = repositories.iterator(); repos.hasNext(); )
        {
            ProxyRepository repository = (ProxyRepository) repos.next();

            if ( checkIfFailureCached( path, repository ) )
            {
                getLogger().debug( "Skipping repository " + repository.getKey() + " for a cached path failure." );
            }
            else
            {
                getFromRepository( target, path, repository, useChecksum );
            }
        }

        if ( !target.exists() )
        {
            throw new ResourceDoesNotExistException( "Could not find " + path + " in any of the repositories." );
        }

        return target;
    }

    private void getFromRepository( File target, String path, ProxyRepository repository, boolean useChecksum )
        throws ProxyException
    {
        boolean connected = false;
        Map checksums = null;
        Wagon wagon = null;

        try
        {
            wagon = wagonManager.getWagon( repository.getProtocol() );

            //@todo configure wagonManager

            if ( useChecksum )
            {
                checksums = prepareChecksumListeners( wagon );
            }

            connected = connectToRepository( wagon, repository );
            if ( connected )
            {
                File temp = new File( target.getAbsolutePath() + ".tmp" );
                temp.deleteOnExit();

                int tries = 0;
                boolean success = true;

                do
                {
                    tries++;

                    getLogger().info( "Trying " + path + " from " + repository.getId() + "..." );

                    if ( !target.exists() )
                    {
                        wagon.get( path, temp );
                    }
                    else
                    {
                        long repoTimestamp = target.lastModified() + repository.getCachePeriod() * 1000;
                        wagon.getIfNewer( path, temp, repoTimestamp );
                    }

                    if ( useChecksum )
                    {
                        success = doChecksumCheck( checksums, path, wagon );
                    }

                    if ( tries > 1 && !success )
                    {
                        throw new ProxyException( "Checksum failures occurred while downloading " + path );
                    }
                }
                while ( !success );

                disconnectWagon( wagon );

                if ( temp.exists() )
                {
                    moveTempToTarget( temp, target );
                }
            }
            //try next repository
        }
        catch ( TransferFailedException e )
        {
            String message = "Skipping repository " + repository.getUrl() + ": " + e.getMessage();
            processRepositoryFailure( repository, message, e );
        }
        catch ( ResourceDoesNotExistException e )
        {
            doCacheFailure( path, repository );
        }
        catch ( AuthorizationException e )
        {
            String message = "Skipping repository " + repository.getUrl() + ": " + e.getMessage();
            processRepositoryFailure( repository, message, e );
        }
        catch ( UnsupportedProtocolException e )
        {
            String message = "Skipping repository " + repository.getUrl() + ": no wagonManager configured " +
                "for protocol " + repository.getProtocol();
            processRepositoryFailure( repository, message, e );
        }
        finally
        {
            if ( wagon != null && checksums != null )
            {
                releaseChecksumListeners( wagon, checksums );
            }

            if ( connected )
            {
                disconnectWagon( wagon );
            }
        }
    }

    /**
     * Used to add checksum observers as transfer listeners to the wagonManager object
     *
     * @param wagon the wagonManager object to use the checksum with
     * @return map of ChecksumObservers added into the wagonManager transfer listeners
     */
    private Map prepareChecksumListeners( Wagon wagon )
    {
        Map checksums = new HashMap();
        try
        {
            ChecksumObserver checksum = new ChecksumObserver( "SHA-1" );
            wagon.addTransferListener( checksum );
            checksums.put( "sha1", checksum );

            checksum = new ChecksumObserver( "MD5" );
            wagon.addTransferListener( checksum );
            checksums.put( "md5", checksum );
        }
        catch ( NoSuchAlgorithmException e )
        {
            getLogger().info( "An error occurred while preparing checksum observers", e );
        }
        return checksums;
    }

    /**
     * Used to remove the ChecksumObservers from the wagonManager object
     *
     * @param wagon       the wagonManager object to remote the ChecksumObservers from
     * @param checksumMap the map representing the list of ChecksumObservers added to the wagonManager object
     */
    private void releaseChecksumListeners( Wagon wagon, Map checksumMap )
    {
        for ( Iterator checksums = checksumMap.values().iterator(); checksums.hasNext(); )
        {
            ChecksumObserver listener = (ChecksumObserver) checksums.next();
            wagon.removeTransferListener( listener );
        }
    }

    /**
     * Used to request the wagonManager object to connect to a repository
     *
     * @param wagon      the wagonManager object that will be used to connect to the repository
     * @param repository the repository object to connect the wagonManager to
     * @return true when the wagonManager is able to connect to the repository
     */
    private boolean connectToRepository( Wagon wagon, ProxyRepository repository )
    {
        boolean connected = false;
        try
        {
            if ( repository.isProxied() )
            {
                wagon.connect( repository, config.getHttpProxy() );
            }
            else
            {
                wagon.connect( repository );
            }
            connected = true;
        }
        catch ( ConnectionException e )
        {
            getLogger().info( "Could not connect to " + repository.getId() + ": " + e.getMessage() );
        }
        catch ( AuthenticationException e )
        {
            getLogger().info( "Could not connect to " + repository.getId() + ": " + e.getMessage() );
        }

        return connected;
    }

    /**
     * Used to verify the checksum during a wagonManager download
     *
     * @param checksumMap the map of ChecksumObservers present in the wagonManager as transferlisteners
     * @param path        path of the remote object whose checksum is to be verified
     * @param wagon       the wagonManager object used to download the requested path
     * @return true when the checksum succeeds and false when the checksum failed.
     */
    private boolean doChecksumCheck( Map checksumMap, String path, Wagon wagon )
        throws ProxyException
    {
        releaseChecksumListeners( wagon, checksumMap );
        for ( Iterator checksums = checksumMap.keySet().iterator(); checksums.hasNext(); )
        {
            String checksumExt = (String) checksums.next();
            ChecksumObserver checksum = (ChecksumObserver) checksumMap.get( checksumExt );
            String checksumPath = path + "." + checksumExt;
            File checksumFile = new File( config.getRepositoryCachePath(), checksumPath );

            try
            {
                File tempChecksumFile = new File( checksumFile.getAbsolutePath() + ".tmp" );

                wagon.get( checksumPath, tempChecksumFile );

                String remoteChecksum = FileUtils.fileRead( tempChecksumFile ).trim();
                if ( remoteChecksum.indexOf( ' ' ) > 0 )
                {
                    remoteChecksum = remoteChecksum.substring( 0, remoteChecksum.indexOf( ' ' ) );
                }

                boolean checksumCheck = false;
                if ( remoteChecksum.toUpperCase().equals( checksum.getActualChecksum().toUpperCase() ) )
                {
                    moveTempToTarget( tempChecksumFile, checksumFile );

                    checksumCheck = true;
                }
                return checksumCheck;
            }
            catch ( ChecksumFailedException e )
            {
                return false;
            }
            catch ( TransferFailedException e )
            {
                getLogger().debug( "An error occurred during the download of " + checksumPath + ": " + e.getMessage(),
                                   e );
                // do nothing try the next checksum
            }
            catch ( ResourceDoesNotExistException e )
            {
                getLogger().debug( "An error occurred during the download of " + checksumPath + ": " + e.getMessage(),
                                   e );
                // do nothing try the next checksum
            }
            catch ( AuthorizationException e )
            {
                getLogger().debug( "An error occurred during the download of " + checksumPath + ": " + e.getMessage(),
                                   e );
                // do nothing try the next checksum
            }
            catch ( IOException e )
            {
                getLogger().debug( "An error occurred while reading the temporary checksum file.", e );
                return false;
            }
        }

        getLogger().debug( "Skipping checksum validation for " + path + ": No remote checksums available." );

        return true;
    }

    /**
     * Used to ensure that this proxy instance is running with a valid configuration instance.
     *
     * @throws ProxyException
     */
    private void checkConfiguration()
        throws ProxyException
    {
        if ( config == null )
        {
            throw new ProxyException( "No proxy configuration defined." );
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
     * Used to disconnect the wagonManager from its repository
     *
     * @param wagon the connected wagonManager object
     */
    private void disconnectWagon( Wagon wagon )
    {
        try
        {
            wagon.disconnect();
        }
        catch ( ConnectionException e )
        {
            getLogger().error( "Problem disconnecting from wagonManager - ignoring: " + e.getMessage() );
        }
    }

    /**
     * Queries the configuration on how to handle a repository download failure
     *
     * @param repository the repository object where the failure occurred
     * @param message    the message/reason for the failure
     * @param t          the cause for the exception
     * @throws ProxyException if hard failure is enabled on the repository causing the failure
     */
    private void processRepositoryFailure( ProxyRepository repository, String message, Throwable t )
        throws ProxyException
    {
        if ( repository.isHardfail() )
        {
            throw new ProxyException(
                "An error occurred in hardfailing repository " + repository.getName() + "...\n    " + message, t );
        }
        else
        {
            getLogger().debug( message, t );
        }
    }
}
