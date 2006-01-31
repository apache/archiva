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
import org.apache.maven.artifact.manager.ChecksumFailedException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.proxy.configuration.ProxyConfiguration;
import org.apache.maven.repository.proxy.files.Checksum;
import org.apache.maven.repository.proxy.files.DefaultRepositoryFileManager;
import org.apache.maven.repository.proxy.repository.ProxyRepository;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.ChecksumObserver;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

/**
 * @author Edwin Punzalan
 */
public class DefaultProxyManager
    //implements ProxyManager
{
    /* @plexus.requirement */
    private WagonManager wagon;

    private ProxyConfiguration config;

    public DefaultProxyManager( ProxyConfiguration configuration )
    {
        config = configuration;
    }

    public File get( String path )
        throws ProxyException
    {
        String cachePath = config.getRepositoryCachePath();
        File cachedFile = new File( cachePath, path );
        if ( !cachedFile.exists() )
        {
            getRemoteFile( path );
        }
        return cachedFile;
    }

    public File getRemoteFile( String path )
        throws ProxyException
    {
        try
        {
            if ( path.indexOf( "/jars/" ) >= 0 )
            {
                //@todo maven 1 repo request
                throw new ProxyException( "Maven 1 repository requests not yet supported." );
            }
            else if ( path.indexOf( "/poms/" ) >= 0 )
            {
                //@todo maven 1 repo request
                throw new ProxyException( "Maven 1 repository requests not yet supported." );
            }
            else
            {
                //maven 2 repo request
                Object obj = new DefaultRepositoryFileManager().getRequestedObjectFromPath( path );

                if ( obj == null )
                {
                    //right now, only metadata is known to fit here
                    return getRepositoryFile( path );
                }
                else if ( obj instanceof Checksum )
                {
                    return getRepositoryFile( path, false );
                }
                else if ( obj instanceof Artifact )
                {
                    Artifact artifact = (Artifact) obj;
                    return getArtifactFile( artifact );
                }
                else
                {
                    throw new ProxyException( "Could not hande repository object: " + obj.getClass() );
                }
            }
        }
        catch ( TransferFailedException e )
        {
            throw new ProxyException( e.getMessage(), e );
        }
        catch ( ResourceDoesNotExistException e )
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
        throws ProxyException
    {
        return getRepositoryFile( path, true );
    }

    private File getRepositoryFile( String path, boolean useChecksum )
        throws ProxyException
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

                ChecksumObserver listener = null;
                try
                {
                    listener = repository.getChecksumObserver();

                    if ( listener != null )
                    {
                        wagon.addTransferListener( listener );
                    }
                }
                catch ( NoSuchAlgorithmException e )
                {
                    System.out.println(
                        "Skipping checksum validation for unsupported algorithm: " + repository.getChecksum() );
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
                            success = doChecksumCheck( listener, repository, path, wagon );
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
                System.out.println( "Skipping repository " + repository.getUrl() + ": " + e.getMessage() );
            }
            catch ( ResourceDoesNotExistException e )
            {
                //do nothing, file not found in this repository
            }
            catch ( AuthorizationException e )
            {
                System.out.println( "Skipping repository " + repository.getUrl() + ": " + e.getMessage() );
            }
            catch ( UnsupportedProtocolException e )
            {
                System.out.println( "Skipping repository " + repository.getUrl() +
                    ": no wagon configured for protocol " + repository.getProtocol() );
            }
        }

        throw new ProxyException( "Could not find " + path + " in any of the repositories." );
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
            System.out.println( "Could not connect to " + repository.getId() + ": " + e.getMessage() );
        }
        catch ( AuthenticationException e )
        {
            System.out.println( "Could not connect to " + repository.getId() + ": " + e.getMessage() );
        }

        return connected;
    }

    private boolean doChecksumCheck( ChecksumObserver listener, ProxyRepository repository, String path, Wagon wagon )
    //throws ChecksumFailedException
    {
        boolean success = false;

        try
        {
            String checksumExt = repository.getChecksum().getFileExtension();
            String remotePath = path + "." + checksumExt;
            File checksumFile = new File( config.getRepositoryCache().getBasedir(), remotePath );

            verifyChecksum( listener.getActualChecksum(), checksumFile, remotePath, checksumExt, wagon );

            wagon.removeTransferListener( listener );

            success = true;
        }
        catch ( ChecksumFailedException e )
        {
            System.out.println( "*** CHECKSUM FAILED - " + e.getMessage() + " - RETRYING" );
        }

        return success;
    }

    private void verifyChecksum( String actualChecksum, File destination, String remotePath,
                                 String checksumFileExtension, Wagon wagon )
        throws ChecksumFailedException
    {
        try
        {
            File tempDestination = new File( destination.getAbsolutePath() + ".tmp" );
            tempDestination.deleteOnExit();

            File tempChecksumFile = new File( tempDestination + checksumFileExtension + ".tmp" );
            tempChecksumFile.deleteOnExit();

            wagon.get( remotePath + checksumFileExtension, tempChecksumFile );

            String expectedChecksum = FileUtils.fileRead( tempChecksumFile );

            // remove whitespaces at the end
            expectedChecksum = expectedChecksum.trim();

            // check for 'MD5 (name) = CHECKSUM'
            if ( expectedChecksum.startsWith( "MD5" ) )
            {
                int lastSpacePos = expectedChecksum.lastIndexOf( ' ' );
                expectedChecksum = expectedChecksum.substring( lastSpacePos + 1 );
            }
            else
            {
                // remove everything after the first space (if available)
                int spacePos = expectedChecksum.indexOf( ' ' );

                if ( spacePos != -1 )
                {
                    expectedChecksum = expectedChecksum.substring( 0, spacePos );
                }
            }

            if ( expectedChecksum.equals( actualChecksum ) )
            {
                File checksumFile = new File( destination + checksumFileExtension );
                if ( checksumFile.exists() )
                {
                    checksumFile.delete();
                }
                FileUtils.copyFile( tempChecksumFile, checksumFile );
            }
            else
            {
                throw new ChecksumFailedException( "Checksum failed on download: local = '" + actualChecksum +
                    "'; remote = '" + expectedChecksum + "'" );
            }
        }
        catch ( TransferFailedException e )
        {
            System.out.println( "Skipping checksum validation for " + remotePath + ": " + e.getMessage() );
        }
        catch ( ResourceDoesNotExistException e )
        {
            System.out.println( "Skipping checksum validation for " + remotePath + ": " + e.getMessage() );
        }
        catch ( AuthorizationException e )
        {
            System.out.println( "Skipping checksum validation for " + remotePath + ": " + e.getMessage() );
        }
        catch ( IOException e )
        {
            throw new ChecksumFailedException( "Invalid checksum file", e );
        }
    }

    private void disconnectWagon( Wagon wagon )
    {
        try
        {
            wagon.disconnect();
        }
        catch ( ConnectionException e )
        {
            System.err.println( "Problem disconnecting from wagon - ignoring: " + e.getMessage() );
        }
    }
}
