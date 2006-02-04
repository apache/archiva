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

    public DefaultProxyManager( ProxyConfiguration configuration )
    {
        config = configuration;
    }

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

    public File getRemoteFile( String path )
        throws ProxyException, ResourceDoesNotExistException
    {
        try
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
        catch ( TransferFailedException e )
        {
            throw new ProxyException( e.getMessage(), e );
        }
    }

    private File getArtifactFile( Artifact artifact )
        throws TransferFailedException, ResourceDoesNotExistException
    {
        ArtifactRepository repoCache = config.getRepositoryCache();

        File artifactFile = new File( repoCache.getBasedir(), repoCache.pathOf( artifact ) );

        if ( !artifactFile.exists() )
        {
            wagon.getArtifact( artifact, config.getRepositories() );
            artifactFile = artifact.getFile();
        }

        return artifactFile;
    }

    private File getRepositoryFile( String path )
        throws ResourceDoesNotExistException, ProxyException
    {
        return getRepositoryFile( path, true );
    }

    private File getRepositoryFile( String path, boolean useChecksum )
        throws ResourceDoesNotExistException, ProxyException
    {
        ArtifactRepository cache = config.getRepositoryCache();
        File target = new File( cache.getBasedir(), path );

        for ( Iterator repositories = config.getRepositories().iterator(); repositories.hasNext(); )
        {
            ProxyRepository repository = (ProxyRepository) repositories.next();

            try
            {
                Wagon wagon = this.wagon.getWagon( repository.getProtocol() );

                //@todo configure wagon

                Map checksums = null;
                if ( useChecksum )
                {
                    checksums = prepareChecksums( wagon );
                }

                if ( connectToRepository( wagon, repository ) )
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
        }

        throw new ResourceDoesNotExistException( "Could not find " + path + " in any of the repositories." );
    }

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

    private void releaseChecksums( Wagon wagon, Map checksumMap )
    {
        for ( Iterator checksums = checksumMap.values().iterator(); checksums.hasNext(); )
        {
            ChecksumObserver listener = (ChecksumObserver) checksums.next();
            wagon.removeTransferListener( listener );
        }
    }

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
