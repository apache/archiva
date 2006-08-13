package org.apache.maven.repository.proxy;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.repository.discovery.ArtifactDiscoverer;
import org.apache.maven.repository.discovery.DiscovererException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.observers.ChecksumObserver;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the proxy handler.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component
 * @todo this currently duplicates a lot of the wagon manager, and doesn't do things like snapshot resolution, etc.
 * The checksum handling is inconsistent with that of the wagon manager.
 * Should we have a more artifact based one? This will merge metadata so should behave correctly, and it is able to
 * correct some limitations of the wagon manager (eg, it can retrieve newer SNAPSHOT files without metadata)
 */
public class DefaultProxyRequestHandler
    extends AbstractLogEnabled
    implements ProxyRequestHandler
{
    /**
     * @plexus.requirement role-hint="default"
     * @todo use a map, and have priorities in them
     */
    private ArtifactDiscoverer defaultArtifactDiscoverer;

    /**
     * @plexus.requirement role-hint="legacy"
     */
    private ArtifactDiscoverer legacyArtifactDiscoverer;

    /**
     * @plexus.requirement role="org.apache.maven.wagon.Wagon"
     */
    private Map/*<String,Wagon>*/ wagons;

    public File get( String path, List proxiedRepositories, ArtifactRepository managedRepository )
        throws ProxyException, ResourceDoesNotExistException
    {
        return get( path, proxiedRepositories, managedRepository, null );
    }

    public File get( String path, List proxiedRepositories, ArtifactRepository managedRepository, ProxyInfo wagonProxy )
        throws ProxyException, ResourceDoesNotExistException
    {
        // TODO! this will prove wrong for metadata and snapshots, let tests bring it out
        //@todo use wagonManager for cache use file:// as URL
        File cachedFile = new File( managedRepository.getBasedir(), path );
        if ( !cachedFile.exists() )
        {
            cachedFile = getAlways( path, proxiedRepositories, managedRepository, wagonProxy );
        }
        return cachedFile;
    }


    public File getAlways( String path, List proxiedRepositories, ArtifactRepository managedRepository )
        throws ProxyException, ResourceDoesNotExistException
    {
        return getAlways( path, proxiedRepositories, managedRepository, null );
    }

    public File getAlways( String path, List proxiedRepositories, ArtifactRepository managedRepository,
                           ProxyInfo wagonProxy )
        throws ResourceDoesNotExistException, ProxyException
    {
        File target = new File( managedRepository.getBasedir(), path );

        for ( Iterator i = proxiedRepositories.iterator(); i.hasNext(); )
        {
            ProxiedArtifactRepository repository = (ProxiedArtifactRepository) i.next();

            if ( repository.isCachedFailure( path ) )
            {
                processCachedRepositoryFailure( repository, "Cached failure found for: " + path );
            }
            else
            {
                get( path, target, repository, managedRepository, wagonProxy );
            }
        }

        if ( !target.exists() )
        {
            throw new ResourceDoesNotExistException( "Could not find " + path + " in any of the repositories." );
        }

        return target;
    }

    private void get( String path, File target, ProxiedArtifactRepository repository,
                      ArtifactRepository managedRepository, ProxyInfo wagonProxy )
        throws ProxyException
    {
        ArtifactRepositoryPolicy policy = null;

        if ( path.endsWith( ".md5" ) || path.endsWith( ".sha1" ) )
        {
            // always read from the managed repository, no need to make remote request
        }
        else if ( path.endsWith( "maven-metadata.xml" ) )
        {
            // TODO: this is not "always" as this method expects!
            // TODO: merge the metadata!
            policy = repository.getRepository().getReleases();
            if ( !target.exists() || isOutOfDate( policy, target ) )
            {
                getFileFromRepository( path, repository, managedRepository.getBasedir(), wagonProxy, target, policy );
            }
        }
        else
        {
            Artifact artifact = null;
            try
            {
                artifact = defaultArtifactDiscoverer.buildArtifact( path );
            }
            catch ( DiscovererException e )
            {
                getLogger().debug( "Failed to build artifact using default layout with message: " + e.getMessage() );
            }

            if ( artifact == null )
            {
                try
                {
                    artifact = legacyArtifactDiscoverer.buildArtifact( path );
                }
                catch ( DiscovererException e )
                {
                    getLogger().debug( "Failed to build artifact using legacy layout with message: " + e.getMessage() );
                }
            }

            if ( artifact != null )
            {
                ArtifactRepository artifactRepository = repository.getRepository();
                policy = artifact.isSnapshot() ? artifactRepository.getSnapshots() : artifactRepository.getReleases();

                if ( !policy.isEnabled() )
                {
                    getLogger().debug( "Skipping disabled repository " + repository.getName() );
                }
                else
                {
                    // Don't use releases policy, we don't want to perform updates on them (only metadata, as used earlier)
                    // TODO: this is not "always" as this method expects!
                    if ( !target.exists() || isOutOfDate( policy, target ) )
                    {
                        getFileFromRepository( artifactRepository.pathOf( artifact ), repository,
                                               managedRepository.getBasedir(), wagonProxy, target, policy );
                    }
                }
            }
            else
            {
                // Some other unknown file in the repository, proxy as is
                // TODO: this is not "always" as this method expects!
                if ( !target.exists() )
                {
                    policy = repository.getRepository().getReleases();
                    getFileFromRepository( path, repository, managedRepository.getBasedir(), wagonProxy, target,
                                           policy );
                }
            }
        }

        if ( target.exists() )
        {
            // in case it previously failed and we've since found it
            repository.clearFailure( path );
        }
    }

    private void getFileFromRepository( String path, ProxiedArtifactRepository repository, String repositoryCachePath,
                                        ProxyInfo httpProxy, File target, ArtifactRepositoryPolicy policy )
        throws ProxyException
    {
        boolean connected = false;
        Map checksums = null;
        Wagon wagon = null;

        try
        {
            String protocol = repository.getRepository().getProtocol();
            wagon = (Wagon) wagons.get( protocol );
            if ( wagon == null )
            {
                throw new ProxyException( "Unsupported remote protocol: " + protocol );
            }

            //@todo configure wagon (ssh settings, etc)

            checksums = prepareChecksumListeners( wagon );

            connected = connectToRepository( wagon, repository, httpProxy );
            if ( connected )
            {
                File temp = new File( target.getAbsolutePath() + ".tmp" );
                temp.deleteOnExit();

                int tries = 0;
                boolean success;

                do
                {
                    tries++;

                    getLogger().debug( "Trying " + path + " from " + repository.getName() + "..." );

                    if ( !target.exists() )
                    {
                        wagon.get( path, temp );
                    }
                    else
                    {
                        wagon.getIfNewer( path, temp, target.lastModified() );
                    }

                    success = checkChecksum( checksums, path, wagon, repositoryCachePath );

                    if ( tries > 1 && !success )
                    {
                        processRepositoryFailure( repository, "Checksum failures occurred while downloading " + path,
                                                  path, policy );
                        return;
                    }

                    // temp won't exist if we called getIfNewer and it was older, but its still a successful return
                    if ( temp.exists() )
                    {
                        moveTempToTarget( temp, target );
                    }
                }
                while ( !success );
            }
            //try next repository
        }
        catch ( TransferFailedException e )
        {
            processRepositoryFailure( repository, e, path, policy );
        }
        catch ( AuthorizationException e )
        {
            processRepositoryFailure( repository, e, path, policy );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // hard failure setting doesn't affect "not found".
            getLogger().debug( "Artifact not found in repository: " + repository.getName() + ": " + e.getMessage() );
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

    private static boolean isOutOfDate( ArtifactRepositoryPolicy policy, File target )
    {
        return policy != null && policy.checkOutOfDate( new Date( target.lastModified() ) );
    }

    /**
     * Used to add checksum observers as transfer listeners to the wagonManager object
     *
     * @param wagon the wagonManager object to use the checksum with
     * @return map of ChecksumObservers added into the wagonManager transfer listeners
     */
    private Map prepareChecksumListeners( Wagon wagon )
    {
        Map checksums = new LinkedHashMap();
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

    private void releaseChecksumListeners( Wagon wagon, Map checksumMap )
    {
        for ( Iterator checksums = checksumMap.values().iterator(); checksums.hasNext(); )
        {
            ChecksumObserver listener = (ChecksumObserver) checksums.next();
            wagon.removeTransferListener( listener );
        }
    }

    private boolean connectToRepository( Wagon wagon, ProxiedArtifactRepository repository, ProxyInfo httpProxy )
    {
        boolean connected = false;
        try
        {
            ArtifactRepository artifactRepository = repository.getRepository();
            Repository wagonRepository = new Repository( artifactRepository.getId(), artifactRepository.getUrl() );
            if ( repository.isUseNetworkProxy() && httpProxy != null )
            {
                wagon.connect( wagonRepository, httpProxy );
            }
            else
            {
                wagon.connect( wagonRepository );
            }
            connected = true;
        }
        catch ( ConnectionException e )
        {
            getLogger().info( "Could not connect to " + repository.getName() + ": " + e.getMessage() );
        }
        catch ( AuthenticationException e )
        {
            getLogger().info( "Could not connect to " + repository.getName() + ": " + e.getMessage() );
        }

        return connected;
    }

    private boolean checkChecksum( Map checksumMap, String path, Wagon wagon, String repositoryCachePath )
        throws ProxyException
    {
        releaseChecksumListeners( wagon, checksumMap );
        for ( Iterator checksums = checksumMap.keySet().iterator(); checksums.hasNext(); )
        {
            String checksumExt = (String) checksums.next();
            ChecksumObserver checksum = (ChecksumObserver) checksumMap.get( checksumExt );
            String checksumPath = path + "." + checksumExt;
            File checksumFile = new File( repositoryCachePath, checksumPath );

            try
            {
                File tempChecksumFile = new File( checksumFile.getAbsolutePath() + ".tmp" );

                wagon.get( checksumPath, tempChecksumFile );

                String remoteChecksum = FileUtils.fileRead( tempChecksumFile ).trim();
                if ( remoteChecksum.indexOf( ' ' ) > 0 )
                {
                    remoteChecksum = remoteChecksum.substring( 0, remoteChecksum.indexOf( ' ' ) );
                }

                String actualChecksum = checksum.getActualChecksum().toUpperCase();
                remoteChecksum = remoteChecksum.toUpperCase();

                boolean checksumCheck;
                if ( remoteChecksum.equals( actualChecksum ) )
                {
                    moveTempToTarget( tempChecksumFile, checksumFile );

                    checksumCheck = true;
                }
                else
                {
                    getLogger().warn(
                        "The checksum '" + actualChecksum + "' did not match the remote value: " + remoteChecksum );
                    checksumCheck = false;
                }
                return checksumCheck;
            }
            catch ( TransferFailedException e )
            {
                getLogger().warn( "An error occurred during the download of " + checksumPath + ": " + e.getMessage(),
                                  e );
                // do nothing try the next checksum
            }
            catch ( ResourceDoesNotExistException e )
            {
                getLogger().debug( "The checksum did not exist: " + checksumPath, e );
                // do nothing try the next checksum
            }
            catch ( AuthorizationException e )
            {
                getLogger().warn( "An error occurred during the download of " + checksumPath + ": " + e.getMessage(),
                                  e );
                // do nothing try the next checksum
            }
            catch ( IOException e )
            {
                getLogger().warn( "An error occurred while reading the temporary checksum file.", e );
                return false;
            }
        }

        getLogger().debug( "No remote checksums available." );

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

    private void processRepositoryFailure( ProxiedArtifactRepository repository, Throwable t, String path,
                                           ArtifactRepositoryPolicy policy )
        throws ProxyException
    {
        repository.addFailure( path, policy );

        String message = t.getMessage();
        if ( repository.isHardFail() )
        {
            repository.addFailure( path, policy );
            throw new ProxyException(
                "An error occurred in hardfailing repository " + repository.getName() + "...\n    " + message, t );
        }

        getLogger().warn( "Skipping repository " + repository.getName() + ": " + message );
        getLogger().debug( "Cause", t );
    }

    private void processRepositoryFailure( ProxiedArtifactRepository repository, String message, String path,
                                           ArtifactRepositoryPolicy policy )
        throws ProxyException
    {
        repository.addFailure( path, policy );

        processCachedRepositoryFailure( repository, message );
    }

    private void processCachedRepositoryFailure( ProxiedArtifactRepository repository, String message )
        throws ProxyException
    {
        if ( repository.isHardFail() )
        {
            throw new ProxyException(
                "An error occurred in hardfailing repository " + repository.getName() + "...\n    " + message );
        }

        getLogger().warn( "Skipping repository " + repository.getName() + ": " + message );
    }
}
