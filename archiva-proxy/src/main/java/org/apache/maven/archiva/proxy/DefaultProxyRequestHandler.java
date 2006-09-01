package org.apache.maven.archiva.proxy;

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

import org.apache.maven.archiva.digest.DigestUtils;
import org.apache.maven.archiva.digest.DigesterException;
import org.apache.maven.archiva.discoverer.ArtifactDiscoverer;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
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
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the proxy handler. This class is not thread safe (the class itself is, but the wagons it uses
 * are not) - it is declared <code>per-lookup</code> for that reason.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component instantiation-strategy="per-lookup"
 * @todo use wagonManager for cache use file:// as URL
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
     * @plexus.requirement
     */
    private ArtifactFactory factory;

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
        return get( managedRepository, path, proxiedRepositories, wagonProxy, false );
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
        return get( managedRepository, path, proxiedRepositories, wagonProxy, true );
    }

    private File get( ArtifactRepository managedRepository, String path, List proxiedRepositories, ProxyInfo wagonProxy,
                      boolean force )
        throws ProxyException, ResourceDoesNotExistException
    {
        File target = new File( managedRepository.getBasedir(), path );

        if ( path.endsWith( "maven-metadata.xml" ) )
        {
            // Request for managed repository metadatas
            getMetadata( path, target, proxiedRepositories, managedRepository, wagonProxy, force );
        }
        else
        {
            boolean checksum = false;
            String checksumExtension = null;
            String artifactPath = path;
            if ( path.endsWith( ".md5" ) || path.endsWith( ".sha1" ) )
            {
                int index = path.lastIndexOf( '.' );
                checksumExtension = path.substring( index + 1 );
                checksum = true;
                artifactPath = path.substring( 0, index );
            }

            // Request for artifact: parse the requested path to build an Artifact.
            Artifact artifact = null;
            try
            {
                artifact = defaultArtifactDiscoverer.buildArtifact( artifactPath );
            }
            catch ( DiscovererException e )
            {
                getLogger().debug( "Failed to build artifact using default layout with message: " + e.getMessage() );
            }

            if ( artifact == null )
            {
                try
                {
                    artifact = legacyArtifactDiscoverer.buildArtifact( artifactPath );
                }
                catch ( DiscovererException e )
                {
                    getLogger().debug( "Failed to build artifact using legacy layout with message: " + e.getMessage() );
                }
            }

            if ( artifact != null )
            {
                applyRelocation( managedRepository, artifact, proxiedRepositories, wagonProxy, force );

                if ( !checksum )
                {
                    // Build the target file name
                    target = new File( managedRepository.getBasedir(), managedRepository.pathOf( artifact ) );

                    // Get the requested artifact from proxiedRepositories
                    getArtifactFromRepository( managedRepository, target, artifact, proxiedRepositories, wagonProxy,
                                               force );
                }
                else
                {
                    // Just adjust the filename for relocation, don't actualy get it
                    target = new File( managedRepository.getBasedir(),
                                       managedRepository.pathOf( artifact ) + "." + checksumExtension );
                }
            }
            else if ( !checksum )
            {
                // Some other unknown file in the repository, proxy as is, unless it was a checksum
                if ( force || !target.exists() )
                {
                    getFileFromRepository( managedRepository, target, path, proxiedRepositories, wagonProxy, force );
                }
            }
        }

        if ( !target.exists() )
        {
            throw new ResourceDoesNotExistException( "Could not find " + path + " in any of the repositories." );
        }

        return target;
    }

    private void getFileFromRepository( ArtifactRepository managedRepository, File target, String path,
                                        List proxiedRepositories, ProxyInfo wagonProxy, boolean force )
        throws ProxyException, ResourceDoesNotExistException
    {
        for ( Iterator i = proxiedRepositories.iterator(); i.hasNext(); )
        {
            ProxiedArtifactRepository repository = (ProxiedArtifactRepository) i.next();

            if ( !force && repository.isCachedFailure( path ) )
            {
                processCachedRepositoryFailure( repository, "Cached failure found for: " + path );
            }
            else
            {
                ArtifactRepositoryPolicy policy = repository.getRepository().getReleases();
                getFileFromRepository( path, repository, managedRepository.getBasedir(), wagonProxy, target, policy,
                                       force );
            }
        }
    }

    private void getArtifactFromRepository( ArtifactRepository managedRepository, File target, Artifact artifact,
                                            List proxiedRepositories, ProxyInfo wagonProxy, boolean force )
        throws ProxyException, ResourceDoesNotExistException
    {
        for ( Iterator i = proxiedRepositories.iterator(); i.hasNext(); )
        {
            ProxiedArtifactRepository repository = (ProxiedArtifactRepository) i.next();
            String path = repository.getRepository().getLayout().pathOf( artifact );

            if ( !force && repository.isCachedFailure( path ) )
            {
                processCachedRepositoryFailure( repository, "Cached failure found for: " + path );
            }
            else
            {
                get( artifact, target, repository, managedRepository, wagonProxy, force );
            }
        }
    }

    private void applyRelocation( ArtifactRepository managedRepository, Artifact artifact, List proxiedRepositories,
                                  ProxyInfo wagonProxy, boolean force )
    {
        Artifact pomArtifact =
            factory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );

        File pomFile = new File( managedRepository.getBasedir(), managedRepository.pathOf( pomArtifact ) );
        try
        {
            getArtifactFromRepository( managedRepository, pomFile, pomArtifact, proxiedRepositories, wagonProxy,
                                       force );
        }
        catch ( ProxyException e )
        {
            getLogger().warn( "Error getting POM for artifact - not relocating: " + e.getMessage() );
            getLogger().debug( "Cause", e );
        }
        catch ( ResourceDoesNotExistException e )
        {
            getLogger().debug( "Remote POM not found for artifact - not relocating" );
        }

        if ( pomFile.exists() )
        {
            Model model = null;
            try
            {
                // Parse the pom and look at relocation metadata
                Reader reader = new FileReader( pomFile );
                model = new MavenXpp3Reader().read( reader );
            }
            catch ( IOException e )
            {
                getLogger().warn( "Error reading POM for artifact - not relocating: " + e.getMessage() );
                getLogger().debug( "Cause", e );
            }
            catch ( XmlPullParserException e )
            {
                getLogger().warn( "Error parsing POM for artifact - not relocating: " + e.getMessage() );
                getLogger().debug( "Cause", e );
            }

            if ( model != null )
            {
                DistributionManagement dist;
                dist = model.getDistributionManagement();

                if ( dist != null )
                {
                    Relocation relocation = dist.getRelocation();
                    if ( relocation != null )
                    {
                        String requestedId =
                            artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();

                        // artifact is relocated : update the artifact
                        if ( relocation.getGroupId() != null )
                        {
                            artifact.setGroupId( relocation.getGroupId() );
                        }
                        if ( relocation.getArtifactId() != null )
                        {
                            artifact.setArtifactId( relocation.getArtifactId() );
                        }
                        if ( relocation.getVersion() != null )
                        {
                            artifact.setVersion( relocation.getVersion() );
                        }

                        String relocatedId =
                            artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();

                        getLogger().debug( "Artifact " + requestedId + " has been relocated to " + relocatedId +
                            ( relocation.getMessage() != null ? ": " + relocation.getMessage() : "" ) );

                        applyRelocation( managedRepository, artifact, proxiedRepositories, wagonProxy, force );
                    }
                }
            }
        }
    }

    private void getMetadata( String path, File target, List proxiedRepositories, ArtifactRepository managedRepository,
                              ProxyInfo wagonProxy, boolean force )
        throws ProxyException
    {
        for ( Iterator i = proxiedRepositories.iterator(); i.hasNext(); )
        {
            ProxiedArtifactRepository repository = (ProxiedArtifactRepository) i.next();
            File metadataFile = new File( target.getParentFile(), ".metadata-" + repository.getRepository().getId() );

            ArtifactRepositoryPolicy policy = repository.getRepository().getReleases();

            // if it is snapshot metadata, use a different policy
            if ( path.endsWith( "-SNAPSHOT/maven-metadata.xml" ) )
            {
                policy = repository.getRepository().getSnapshots();
            }

            if ( force || !metadataFile.exists() || isOutOfDate( policy, metadataFile ) )
            {
                getFileFromRepository( path, repository, managedRepository.getBasedir(), wagonProxy, metadataFile,
                                       policy, force );

                mergeMetadataFiles( target, metadataFile );
            }
        }
    }

    private void get( Artifact artifact, File target, ProxiedArtifactRepository repository,
                      ArtifactRepository managedRepository, ProxyInfo wagonProxy, boolean force )
        throws ProxyException
    {
        ArtifactRepository artifactRepository = repository.getRepository();

        // we use the release policy for tracking failures, but only check for updates on snapshots
        // also, we don't look for updates on timestamp snapshot files, only non-unique-version ones
        ArtifactRepositoryPolicy policy =
            artifact.isSnapshot() ? artifactRepository.getSnapshots() : artifactRepository.getReleases();

        boolean needsUpdate = false;
        if ( artifact.getVersion().endsWith( "-SNAPSHOT" ) && isOutOfDate( policy, target ) )
        {
            needsUpdate = true;
        }

        if ( needsUpdate || force || !target.exists() )
        {
            getFileFromRepository( artifactRepository.pathOf( artifact ), repository, managedRepository.getBasedir(),
                                   wagonProxy, target, policy, force );
        }
    }

    private void mergeMetadataFiles( File target, File metadataFile )
        throws ProxyException
    {
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        if ( metadataFile.exists() )
        {
            Metadata metadata = null;
            if ( target.exists() )
            {
                FileReader fileReader = null;
                try
                {
                    fileReader = new FileReader( target );
                    metadata = reader.read( fileReader );
                }
                catch ( XmlPullParserException e )
                {
                    throw new ProxyException( "Unable to parse existing metadata: " + e.getMessage(), e );
                }
                catch ( IOException e )
                {
                    throw new ProxyException( "Unable to read existing metadata: " + e.getMessage(), e );
                }
                finally
                {
                    IOUtil.close( fileReader );
                }
            }

            FileReader fileReader = null;
            boolean changed = false;
            try
            {
                fileReader = new FileReader( metadataFile );
                Metadata newMetadata = reader.read( fileReader );

                if ( metadata != null )
                {
                    changed = metadata.merge( newMetadata );
                }
                else
                {
                    metadata = newMetadata;
                    changed = true;
                }
            }
            catch ( IOException e )
            {
                // ignore the merged file
                getLogger().warn( "Unable to read new metadata: " + e.getMessage() );
            }
            catch ( XmlPullParserException e )
            {
                // ignore the merged file
                getLogger().warn( "Unable to parse new metadata: " + e.getMessage() );
            }
            finally
            {
                IOUtil.close( fileReader );
            }

            if ( changed )
            {
                FileWriter fileWriter = null;
                try
                {
                    fileWriter = new FileWriter( target );
                    new MetadataXpp3Writer().write( fileWriter, metadata );
                }
                catch ( IOException e )
                {
                    getLogger().warn( "Unable to store new metadata: " + e.getMessage() );
                }
                finally
                {
                    IOUtil.close( fileWriter );
                }
            }
        }
    }

    private void getFileFromRepository( String path, ProxiedArtifactRepository repository, String repositoryCachePath,
                                        ProxyInfo httpProxy, File target, ArtifactRepositoryPolicy policy,
                                        boolean force )
        throws ProxyException
    {
        if ( !policy.isEnabled() )
        {
            getLogger().debug( "Skipping disabled repository " + repository.getName() );
            return;
        }

        Map checksums = null;
        Wagon wagon = null;

        File temp = new File( target.getAbsolutePath() + ".tmp" );
        temp.deleteOnExit();

        boolean connected = false;
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
                int tries = 0;
                boolean success;

                do
                {
                    tries++;

                    getLogger().debug( "Trying " + path + " from " + repository.getName() + "..." );

                    boolean downloaded = true;
                    if ( force || !target.exists() )
                    {
                        wagon.get( path, temp );
                    }
                    else
                    {
                        downloaded = wagon.getIfNewer( path, temp, target.lastModified() );
                    }

                    if ( downloaded )
                    {
                        success = checkChecksum( checksums, path, wagon, repositoryCachePath );

                        if ( tries > 1 && !success )
                        {
                            processRepositoryFailure( repository,
                                                      "Checksum failures occurred while downloading " + path, path,
                                                      policy );
                            return;
                        }
                    }
                    else
                    {
                        // getIfNewer determined we were up to date
                        success = true;
                    }
                }
                while ( !success );

                // temp won't exist if we called getIfNewer and it was older, but its still a successful return
                if ( temp.exists() )
                {
                    moveTempToTarget( temp, target );
                }
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
            temp.delete();

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
            getLogger().error( "An error occurred while preparing checksum observers: " + e.getMessage() );
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

        boolean correctChecksum = false;

        boolean allNotFound = true;

        for ( Iterator i = checksumMap.keySet().iterator(); i.hasNext() && !correctChecksum; )
        {
            String checksumExt = (String) i.next();
            ChecksumObserver checksum = (ChecksumObserver) checksumMap.get( checksumExt );
            String checksumPath = path + "." + checksumExt;
            File checksumFile = new File( repositoryCachePath, checksumPath );

            File tempChecksumFile = new File( checksumFile.getAbsolutePath() + ".tmp" );
            tempChecksumFile.deleteOnExit();

            try
            {
                wagon.get( checksumPath, tempChecksumFile );

                allNotFound = false;

                String remoteChecksum = DigestUtils.cleanChecksum( FileUtils.fileRead( tempChecksumFile ),
                                                                   checksumExt.toUpperCase(),
                                                                   path.substring( path.lastIndexOf( '/' ) ) );

                String actualChecksum = checksum.getActualChecksum();

                remoteChecksum = remoteChecksum.toUpperCase();

                if ( actualChecksum != null && remoteChecksum.equals( actualChecksum.toUpperCase() ) )
                {
                    moveTempToTarget( tempChecksumFile, checksumFile );

                    correctChecksum = true;
                }
                else
                {
                    getLogger().warn(
                        "The checksum '" + actualChecksum + "' did not match the remote value: " + remoteChecksum );
                }
            }
            catch ( TransferFailedException e )
            {
                getLogger().warn( "An error occurred during the download of " + checksumPath + ": " + e.getMessage() );
                // do nothing try the next checksum

                allNotFound = false;
            }
            catch ( ResourceDoesNotExistException e )
            {
                getLogger().debug( "The checksum did not exist: " + checksumPath + "; " + e.getMessage() );
                // do nothing try the next checksum
                // remove it if it is present locally in case there is an old incorrect one
                if ( checksumFile.exists() )
                {
                    checksumFile.delete();
                }
            }
            catch ( AuthorizationException e )
            {
                getLogger().warn( "An error occurred during the download of " + checksumPath + ": " + e.getMessage() );
                // do nothing try the next checksum

                allNotFound = false;
            }
            catch ( IOException e )
            {
                getLogger().warn( "An error occurred while reading the temporary checksum file: " + e.getMessage() );
                // do nothing try the next checksum

                allNotFound = false;
            }
            catch ( DigesterException e )
            {
                getLogger().warn( "The checksum was invalid: " + checksumPath + ": " + e.getMessage() );
                // do nothing try the next checksum

                allNotFound = false;
            }
            finally
            {
                tempChecksumFile.delete();
            }
        }
        return correctChecksum || allNotFound;
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
