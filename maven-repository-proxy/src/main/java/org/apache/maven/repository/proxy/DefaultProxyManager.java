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
import org.apache.maven.repository.ArtifactUtils;
import org.apache.maven.repository.digest.DefaultDigester;
import org.apache.maven.repository.digest.Digester;
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

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Edwin Punzalan
 * @plexus.component role="org.apache.maven.repository.proxy.ProxyManager"
 */
public class DefaultProxyManager
    extends AbstractLogEnabled
    implements ProxyManager
{
    /**
     * @plexus.requirement
     */
    private WagonManager wagon;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    private ProxyConfiguration config;

    /**
     * Constructor.
     *
     * @param configuration the configuration object to base the behavior of this instance
     */
    public DefaultProxyManager( ProxyConfiguration configuration )
    {
        config = configuration;
    }

    /**
     * @see org.apache.maven.repository.proxy.ProxyManager#get(String)
     */
    public File get( String path )
        throws ProxyException, ResourceDoesNotExistException
    {
        //@todo use wagon for cache use file:// as URL
        String cachePath = config.getRepositoryCachePath();
        File cachedFile = new File( cachePath, path );
        if ( !cachedFile.exists() )
        {
            getRemoteFile( path );
        }
        return cachedFile;
    }

    /**
     * @see org.apache.maven.repository.proxy.ProxyManager#getRemoteFile(String)
     */
    public File getRemoteFile( String path )
        throws ProxyException, ResourceDoesNotExistException
    {
        Artifact artifact = ArtifactUtils.buildArtifact( path, artifactFactory );

        File remoteFile;
        if ( artifact != null )
        {
            remoteFile = getArtifactFile( artifact );
        }
        else if ( path.endsWith( ".md5" ) || path.endsWith( ".sha1" ) )
        {
            remoteFile = getRepositoryFile( path, false );
        }
        else
        {
            // as of now, only metadata fits here
            remoteFile = getRepositoryFile( path );
        }

        return remoteFile;
    }

    /**
     * Used to download an artifact object from the remote repositories.
     *
     * @param artifact the artifact object to be downloaded from a remote repository
     * @return File object representing the remote artifact in the repository cache
     * @throws ProxyException when an error occurred during retrieval of the requested artifact
     * @throws ResourceDoesNotExistException when the requested artifact cannot be found in any of the
     *      configured repositories
     */
    private File getArtifactFile( Artifact artifact )
        throws ResourceDoesNotExistException, ProxyException
    {
        ArtifactRepository repoCache = config.getRepositoryCache();

        File artifactFile = new File( repoCache.getBasedir(), repoCache.pathOf( artifact ) );

        if ( !artifactFile.exists() )
        {
            try
            {
                wagon.getArtifact( artifact, config.getRepositories() );
            }
            catch ( TransferFailedException e )
            {
                throw new ProxyException( e.getMessage(), e );
            }
            artifactFile = artifact.getFile();
        }

        return artifactFile;
    }

    /**
     * Used to retrieve a remote file from the remote repositories.  This method is used only when the requested
     *      path cannot be resolved into a repository object, for example, an Artifact.
     *
     * @param path the remote path to use to search for the requested file
     * @return File object representing the remote file in the repository cache
     * @throws ResourceDoesNotExistException when the requested path cannot be found in any of the configured
     *      repositories.
     * @throws ProxyException when an error occurred during the retrieval of the requested path
     */
    private File getRepositoryFile( String path )
        throws ResourceDoesNotExistException, ProxyException
    {
        return getRepositoryFile( path, true );
    }

    /**
     * Used to retrieve a remote file from the remote repositories.  This method is used only when the requested
     *      path cannot be resolved into a repository object, for example, an Artifact.
     *
     * @param path the remote path to use to search for the requested file
     * @param useChecksum forces the download to whether use a checksum (if present in the remote repository) or not
     * @return File object representing the remote file in the repository cache
     * @throws ResourceDoesNotExistException when the requested path cannot be found in any of the configured
     *      repositories.
     * @throws ProxyException when an error occurred during the retrieval of the requested path
     */
    private File getRepositoryFile( String path, boolean useChecksum )
        throws ResourceDoesNotExistException, ProxyException
    {
        Map checksums = null;
        Wagon wagon = null;
        boolean connected = false;

        ArtifactRepository cache = config.getRepositoryCache();
        File target = new File( cache.getBasedir(), path );

        for ( Iterator repositories = config.getRepositories().iterator(); repositories.hasNext(); )
        {
            ProxyRepository repository = (ProxyRepository) repositories.next();

            try
            {
                wagon = this.wagon.getWagon( repository.getProtocol() );

                //@todo configure wagon

                if ( useChecksum )
                {
                    checksums = prepareChecksums( wagon );
                }

                connected = connectToRepository( wagon, repository );
                if ( connected )
                {
                    File temp = new File( target.getAbsolutePath() + ".tmp" );
                    temp.deleteOnExit();

                    int tries = 0;
                    boolean success = false;

                    while ( !success )
                    {
                        tries++;

                        wagon.get( path, temp );

                        if ( useChecksum )
                        {
                            releaseChecksums( wagon, checksums );
                            success = doChecksumCheck( checksums, path, wagon );
                        }
                        else
                        {
                            success = true;
                        }

                        if ( tries > 1 && !success )
                        {
                            throw new ProxyException( "Checksum failures occurred while downloading " + path );
                        }
                    }
                    disconnectWagon( wagon );

                    return target;
                }
                //try next repository
            }
            catch ( TransferFailedException e )
            {
                getLogger().info( "Skipping repository " + repository.getUrl() + ": " + e.getMessage() );
            }
            catch ( ResourceDoesNotExistException e )
            {
                //do nothing, file not found in this repository
            }
            catch ( AuthorizationException e )
            {
                getLogger().info( "Skipping repository " + repository.getUrl() + ": " + e.getMessage() );
            }
            catch ( UnsupportedProtocolException e )
            {
                getLogger().info( "Skipping repository " + repository.getUrl() + ": no wagon configured for protocol " +
                    repository.getProtocol() );
            }
            finally
            {
                if ( wagon != null && checksums != null )
                {
                    releaseChecksums( wagon, checksums );
                }

                if ( connected )
                {
                    disconnectWagon( wagon );
                }
            }
        }

        throw new ResourceDoesNotExistException( "Could not find " + path + " in any of the repositories." );
    }

    /**
     * Used to add checksum observers as transfer listeners to the wagon object
     *
     * @param wagon the wagon object to use the checksum with
     * @return map of ChecksumObservers added into the wagon transfer listeners
     */
    private Map prepareChecksums( Wagon wagon )
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
     * Used to remove the ChecksumObservers from the wagon object
     *
     * @param wagon the wagon object to remote the ChecksumObservers from
     * @param checksumMap the map representing the list of ChecksumObservers added to the wagon object
     */
    private void releaseChecksums( Wagon wagon, Map checksumMap )
    {
        for ( Iterator checksums = checksumMap.values().iterator(); checksums.hasNext(); )
        {
            ChecksumObserver listener = (ChecksumObserver) checksums.next();
            wagon.removeTransferListener( listener );
        }
    }

    /**
     * Used to request the wagon object to connect to a repository
     *
     * @param wagon the wagon object that will be used to connect to the repository
     * @param repository the repository object to connect the wagon to
     * @return true when the wagon is able to connect to the repository
     */
    private boolean connectToRepository( Wagon wagon, ProxyRepository repository )
    {
        boolean connected = false;
        try
        {
            wagon.connect( repository );
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
     * Used to verify the checksum during a wagon download
     *
     * @param checksumMap the map of ChecksumObservers present in the wagon as transferlisteners
     * @param path path of the remote object whose checksum is to be verified
     * @param wagon the wagon object used to download the requested path
     * @return true when the checksum succeeds and false when the checksum failed.
     */
    private boolean doChecksumCheck( Map checksumMap, String path, Wagon wagon )
    {
        for ( Iterator checksums = checksumMap.keySet().iterator(); checksums.hasNext(); )
        {
            String checksumExt = (String) checksums.next();
            ChecksumObserver checksum = (ChecksumObserver) checksumMap.get( checksumExt );
            String remotePath = path + "." + checksumExt;
            File checksumFile = new File( config.getRepositoryCache().getBasedir(), remotePath );

            try
            {
                File tempChecksumFile = new File( checksumFile.getAbsolutePath() + "." + checksumExt );

                wagon.get( remotePath + "." + checksumExt, tempChecksumFile );

                String algorithm;
                if ( "md5".equals( checksumExt ) )
                {
                    algorithm = "MD5";
                }
                else
                {
                    algorithm = "SHA-1";
                }

                Digester digester = new DefaultDigester();
                try
                {
                    return digester.verifyChecksum( tempChecksumFile, checksum.getActualChecksum(), algorithm );
                }
                catch ( NoSuchAlgorithmException e )
                {
                    getLogger().info( "Failed to initialize checksum: " + algorithm + "\n  " + e.getMessage() );
                    return false;
                }
                catch ( IOException e )
                {
                    getLogger().info( "Failed to verify checksum: " + algorithm + "\n  " + e.getMessage() );
                    return false;
                }

            }
            catch ( ChecksumFailedException e )
            {
                return false;
            }
            catch ( TransferFailedException e )
            {
                getLogger().warn( "An error occurred during the download of " + remotePath + ": " + e.getMessage() );
                // do nothing try the next checksum
            }
            catch ( ResourceDoesNotExistException e )
            {
                getLogger().warn( "An error occurred during the download of " + remotePath + ": " + e.getMessage() );
                // do nothing try the next checksum
            }
            catch ( AuthorizationException e )
            {
                getLogger().warn( "An error occurred during the download of " + remotePath + ": " + e.getMessage() );
                // do nothing try the next checksum
            }
        }

        getLogger().info( "Skipping checksum validation for " + path + ": No remote checksums available." );

        return true;
    }

    /**
     * Used to disconnect the wagon from its repository
     *
     * @param wagon the connected wagon object
     */
    private void disconnectWagon( Wagon wagon )
    {
        try
        {
            wagon.disconnect();
        }
        catch ( ConnectionException e )
        {
            getLogger().error( "Problem disconnecting from wagon - ignoring: " + e.getMessage() );
        }
    }
}
