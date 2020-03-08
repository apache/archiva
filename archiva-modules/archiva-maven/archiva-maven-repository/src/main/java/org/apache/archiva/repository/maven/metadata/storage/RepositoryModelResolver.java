package org.apache.archiva.repository.maven.metadata.storage;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.metadata.maven.MavenMetadataReader;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.SnapshotVersion;
import org.apache.archiva.proxy.maven.WagonFactory;
import org.apache.archiva.proxy.maven.WagonFactoryException;
import org.apache.archiva.proxy.maven.WagonFactoryRequest;
import org.apache.archiva.proxy.model.NetworkProxy;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RepositoryCredentials;
import org.apache.archiva.repository.maven.MavenSystemManager;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepositoryModelResolver
    implements ModelResolver
{

    private final Map<String, NetworkProxy> networkProxyMap = new HashMap<>();

    private RepositorySystemSession session;
    private VersionRangeResolver versionRangeResolver;

    private StorageAsset basedir;

    private RepositoryPathTranslator pathTranslator;

    private WagonFactory wagonFactory;

    private List<RemoteRepository> remoteRepositories;

    private ManagedRepository targetRepository;

    private static final Logger log = LoggerFactory.getLogger( RepositoryModelResolver.class );

    private static final String METADATA_FILENAME = "maven-metadata.xml";

    private MavenSystemManager mavenSystemManager;

    private MavenMetadataReader metadataReader;



    private ManagedRepository managedRepository;

    public RepositoryModelResolver(StorageAsset basedir, RepositoryPathTranslator pathTranslator)
    {
        this.basedir = basedir;

        this.pathTranslator = pathTranslator;

    }

    public RepositoryModelResolver(ManagedRepository managedRepository, RepositoryPathTranslator pathTranslator,
                                   WagonFactory wagonFactory, List<RemoteRepository> remoteRepositories,
                                   Map<String, NetworkProxy> networkProxiesMap, ManagedRepository targetRepository,
                                   MavenSystemManager mavenSystemManager, MavenMetadataReader metadataReader)
    {
        this( managedRepository.getAsset(""), pathTranslator );

        this.managedRepository = managedRepository;

        this.wagonFactory = wagonFactory;

        this.remoteRepositories = remoteRepositories;

        this.networkProxyMap.clear();
        this.networkProxyMap.putAll(networkProxiesMap);

        this.targetRepository = targetRepository;

        this.session = MavenSystemManager.newRepositorySystemSession( managedRepository.getAsset("").getFilePath().toString() );

        this.versionRangeResolver = mavenSystemManager.getLocator().getService(VersionRangeResolver.class);

        this.mavenSystemManager = mavenSystemManager;
        this.metadataReader = metadataReader;
    }


    @Override
    public ModelSource resolveModel( String groupId, String artifactId, String version )
        throws UnresolvableModelException
    {
        String filename = artifactId + "-" + version + ".pom";
        // TODO: we need to convert 1.0-20091120.112233-1 type paths to baseVersion for the below call - add a test

        StorageAsset model = pathTranslator.toFile( basedir, groupId, artifactId, version, filename );

        if ( !model.exists() )
        {
            /**
             *
             */
            // is a SNAPSHOT ? so we can try to find locally before asking remote repositories.
            if ( StringUtils.contains( version, VersionUtil.SNAPSHOT ) )
            {
                Path localSnapshotModel = findTimeStampedSnapshotPom( groupId, artifactId, version, model.getParent().getFilePath() );
                if ( localSnapshotModel != null )
                {
                    return new FileModelSource( localSnapshotModel.toFile() );
                }

            }

            for ( RemoteRepository remoteRepository : remoteRepositories )
            {
                try
                {
                    boolean success = getModelFromProxy( remoteRepository, groupId, artifactId, version, filename );
                    if ( success && model.exists() )
                    {
                        log.info( "Model '{}' successfully retrieved from remote repository '{}'",
                                  model.getPath(), remoteRepository.getId() );
                        break;
                    }
                }
                catch ( ResourceDoesNotExistException e )
                {
                    log.info(
                        "An exception was caught while attempting to retrieve model '{}' from remote repository '{}'.Reason:{}",
                        model.getPath(), remoteRepository.getId(), e.getMessage() );
                }
                catch ( Exception e )
                {
                    log.warn(
                        "An exception was caught while attempting to retrieve model '{}' from remote repository '{}'.Reason:{}",
                        model.getPath(), remoteRepository.getId(), e.getMessage() );

                    continue;
                }
            }
        }

        return new FileModelSource( model.getFilePath().toFile() );
    }

    public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        try {
            Artifact artifact = new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), "", "pom", parent.getVersion());
            VersionRangeRequest versionRangeRequest;
            versionRangeRequest = new VersionRangeRequest(artifact, null, null);
            VersionRangeResult versionRangeResult = this.versionRangeResolver.resolveVersionRange(this.session, versionRangeRequest);
            if (versionRangeResult.getHighestVersion() == null) {
                throw new UnresolvableModelException(String.format("No versions matched the requested parent version range '%s'", parent.getVersion()), parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
            } else if (versionRangeResult.getVersionConstraint() != null && versionRangeResult.getVersionConstraint().getRange() != null && versionRangeResult.getVersionConstraint().getRange().getUpperBound() == null) {
                throw new UnresolvableModelException(String.format("The requested parent version range '%s' does not specify an upper bound", parent.getVersion()), parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
            } else {
                parent.setVersion(versionRangeResult.getHighestVersion().toString());
                return this.resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
            }
        } catch ( VersionRangeResolutionException var5) {
            throw new UnresolvableModelException(var5.getMessage(), parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), var5);
        }
    }

    public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
        try {
            Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), "", "pom", dependency.getVersion());
            VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, null, null);
            VersionRangeResult versionRangeResult = this.versionRangeResolver.resolveVersionRange(this.session, versionRangeRequest);
            if (versionRangeResult.getHighestVersion() == null) {
                throw new UnresolvableModelException(String.format("No versions matched the requested dependency version range '%s'", dependency.getVersion()), dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
            } else if (versionRangeResult.getVersionConstraint() != null && versionRangeResult.getVersionConstraint().getRange() != null && versionRangeResult.getVersionConstraint().getRange().getUpperBound() == null) {
                throw new UnresolvableModelException(String.format("The requested dependency version range '%s' does not specify an upper bound", dependency.getVersion()), dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
            } else {
                dependency.setVersion(versionRangeResult.getHighestVersion().toString());
                return this.resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
            }
        } catch (VersionRangeResolutionException var5) {
            throw new UnresolvableModelException(var5.getMessage(), dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), var5);
        }
    }

    protected Path findTimeStampedSnapshotPom( String groupId, String artifactId, String version,
                                               Path parentDirectory )
    {

        // reading metadata if there
        Path mavenMetadata = parentDirectory.resolve( METADATA_FILENAME );
        if ( Files.exists(mavenMetadata) )
        {
            try
            {
                ArchivaRepositoryMetadata archivaRepositoryMetadata = metadataReader.read( mavenMetadata );
                SnapshotVersion snapshotVersion = archivaRepositoryMetadata.getSnapshotVersion();
                if ( snapshotVersion != null )
                {
                    String lastVersion = snapshotVersion.getTimestamp();
                    int buildNumber = snapshotVersion.getBuildNumber();
                    String snapshotPath =
                        StringUtils.replaceChars( groupId, '.', '/' ) + '/' + artifactId + '/' + version + '/'
                            + artifactId + '-' + StringUtils.remove( version, "-" + VersionUtil.SNAPSHOT ) + '-'
                            + lastVersion + '-' + buildNumber + ".pom";

                    log.debug( "use snapshot path {} for maven coordinate {}:{}:{}", snapshotPath, groupId, artifactId,
                               version );

                    StorageAsset model = basedir.resolve( snapshotPath );
                    //model = pathTranslator.toFile( basedir, groupId, artifactId, lastVersion, filename );
                    if ( model.exists() )
                    {
                        return model.getFilePath();
                    }
                }
            }
            catch ( RepositoryMetadataException e )
            {
                log.warn( "fail to read {}, {}", mavenMetadata.toAbsolutePath(), e.getCause() );
            }
        }

        return null;
    }

    @Override
    public void addRepository( Repository repository )
        throws InvalidRepositoryException
    {
        // we just ignore repositories outside of the current one for now
        // TODO: it'd be nice to look them up from Archiva's set, but we want to do that by URL / mapping, not just the
        //       ID since they will rarely match
    }

    @Override
    public void addRepository( Repository repository, boolean b ) throws InvalidRepositoryException
    {

    }

    @Override
    public ModelResolver newCopy()
    {
        return new RepositoryModelResolver( managedRepository,  pathTranslator, wagonFactory, remoteRepositories, 
                                            networkProxyMap, targetRepository, mavenSystemManager, metadataReader);
    }

    // FIXME: we need to do some refactoring, we cannot re-use the proxy components of archiva-proxy in maven2-repository
    // because it's causing a cyclic dependency
    private boolean getModelFromProxy( RemoteRepository remoteRepository, String groupId, String artifactId,
                                       String version, String filename )
        throws AuthorizationException, TransferFailedException, ResourceDoesNotExistException, WagonFactoryException,
        IOException, RepositoryMetadataException
    {
        boolean success = false;
        Path tmpMd5 = null;
        Path tmpSha1 = null;
        Path tmpResource = null;
        String artifactPath = pathTranslator.toPath( groupId, artifactId, version, filename );
        Path resource = targetRepository.getAsset("").getFilePath().resolve( artifactPath );

        Path workingDirectory = createWorkingDirectory( targetRepository.getLocation().toString() );
        try
        {
            Wagon wagon = null;
            try
            {
                String protocol = getProtocol( remoteRepository.getLocation().toString() );
                final NetworkProxy networkProxy = this.networkProxyMap.get( remoteRepository.getId() );

                wagon = wagonFactory.getWagon(
                    new WagonFactoryRequest( "wagon#" + protocol, remoteRepository.getExtraHeaders() ).networkProxy(
                        networkProxy )
                );

                if ( wagon == null )
                {
                    throw new RuntimeException( "Unsupported remote repository protocol: " + protocol );
                }

                boolean connected = connectToRepository( wagon, remoteRepository );
                if ( connected )
                {
                    tmpResource = workingDirectory.resolve( filename );

                    if ( VersionUtil.isSnapshot( version ) )
                    {
                        // get the metadata first!
                        Path tmpMetadataResource = workingDirectory.resolve( METADATA_FILENAME );

                        String metadataPath =
                            StringUtils.substringBeforeLast( artifactPath, "/" ) + "/" + METADATA_FILENAME;

                        wagon.get( addParameters( metadataPath, remoteRepository ), tmpMetadataResource.toFile() );

                        log.debug( "Successfully downloaded metadata." );

                        ArchivaRepositoryMetadata metadata = metadataReader.read( tmpMetadataResource );

                        // re-adjust to timestamp if present, otherwise retain the original -SNAPSHOT filename
                        SnapshotVersion snapshotVersion = metadata.getSnapshotVersion();
                        String timestampVersion = version;
                        if ( snapshotVersion != null )
                        {
                            timestampVersion = timestampVersion.substring( 0, timestampVersion.length()
                                - 8 ); // remove SNAPSHOT from end
                            timestampVersion = timestampVersion + snapshotVersion.getTimestamp() + "-"
                                + snapshotVersion.getBuildNumber();

                            filename = artifactId + "-" + timestampVersion + ".pom";

                            artifactPath = pathTranslator.toPath( groupId, artifactId, version, filename );

                            log.debug( "New artifactPath :{}", artifactPath );
                        }
                    }

                    log.info( "Retrieving {} from {}", artifactPath, remoteRepository.getName() );

                    wagon.get( addParameters( artifactPath, remoteRepository ), tmpResource.toFile() );

                    log.debug( "Downloaded successfully." );

                    tmpSha1 = transferChecksum( wagon, remoteRepository, artifactPath, tmpResource, workingDirectory,
                                                ".sha1" );
                    tmpMd5 = transferChecksum( wagon, remoteRepository, artifactPath, tmpResource, workingDirectory,
                                               ".md5" );
                }
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

            if ( resource != null )
            {
                synchronized ( resource.toAbsolutePath().toString().intern() )
                {
                    Path directory = resource.getParent();
                    moveFileIfExists( tmpMd5, directory );
                    moveFileIfExists( tmpSha1, directory );
                    moveFileIfExists( tmpResource, directory );
                    success = true;
                }
            }
        }
        finally
        {
            org.apache.archiva.common.utils.FileUtils.deleteQuietly( workingDirectory );
        }

        // do we still need to execute the consumers?

        return success;
    }

    /**
     * Using wagon, connect to the remote repository.
     *
     * @param wagon the wagon instance to establish the connection on.
     * @return true if the connection was successful. false if not connected.
     */
    private boolean connectToRepository( Wagon wagon, RemoteRepository remoteRepository )
    {
        boolean connected;

        final NetworkProxy proxyConnector = this.networkProxyMap.get( remoteRepository.getId() );
        ProxyInfo networkProxy = null;
        if ( proxyConnector != null )
        {
            networkProxy = new ProxyInfo();
            networkProxy.setType( proxyConnector.getProtocol() );
            networkProxy.setHost( proxyConnector.getHost() );
            networkProxy.setPort( proxyConnector.getPort() );
            networkProxy.setUserName( proxyConnector.getUsername() );
            networkProxy.setPassword( new String(proxyConnector.getPassword()) );

            String msg = "Using network proxy " + networkProxy.getHost() + ":" + networkProxy.getPort()
                + " to connect to remote repository " + remoteRepository.getLocation();
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

        AuthenticationInfo authInfo = null;
        RepositoryCredentials creds = remoteRepository.getLoginCredentials();
        String username = "";
        String password = "";
        if (creds instanceof UsernamePasswordCredentials) {
            UsernamePasswordCredentials c = (UsernamePasswordCredentials) creds;
            username = c.getUserName();
            password = c.getPassword();
        }

        if ( StringUtils.isNotBlank( username ) && StringUtils.isNotBlank( password ) )
        {
            log.debug( "Using username {} to connect to remote repository {}", username, remoteRepository.getLocation() );
            authInfo = new AuthenticationInfo();
            authInfo.setUserName( username );
            authInfo.setPassword( password );
        }

        int timeoutInMilliseconds = ((int)remoteRepository.getTimeout().getSeconds())*1000;
        // FIXME olamy having 2 config values
        // Set timeout
        wagon.setReadTimeout( timeoutInMilliseconds );
        wagon.setTimeout( timeoutInMilliseconds );

        try
        {
            org.apache.maven.wagon.repository.Repository wagonRepository =
                new org.apache.maven.wagon.repository.Repository( remoteRepository.getId(), remoteRepository.getLocation().toString() );
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
        catch ( ConnectionException | AuthenticationException e )
        {
            log.error( "Could not connect to {}:{} ", remoteRepository.getName(), e.getMessage() );
            connected = false;
        }

        return connected;
    }

    /**
     *
     * @param wagon The wagon instance that should be connected.
     * @param remoteRepository The repository from where the checksum file should be retrieved
     * @param remotePath The remote path of the artifact (without extension)
     * @param resource The local artifact (without extension)
     * @param workingDir The working directory where the downloaded file should be placed to
     * @param ext The extension of th checksum file
     * @return The file where the data has been downloaded to.
     * @throws AuthorizationException
     * @throws TransferFailedException
     * @throws ResourceDoesNotExistException
     */
    private Path transferChecksum( final Wagon wagon, final RemoteRepository remoteRepository,
                                   final String remotePath, final Path resource,
                                   final Path workingDir, final String ext )
        throws AuthorizationException, TransferFailedException, ResourceDoesNotExistException
    {
        Path destFile = workingDir.resolve( resource.getFileName() + ext );
        String remoteChecksumPath = remotePath + ext;

        log.info( "Retrieving {} from {}", remoteChecksumPath, remoteRepository.getName() );

        wagon.get( addParameters( remoteChecksumPath, remoteRepository ), destFile.toFile() );

        log.debug( "Downloaded successfully." );

        return destFile;
    }

    private String getProtocol( String url )
    {
        String protocol = StringUtils.substringBefore( url, ":" );

        return protocol;
    }

    private Path createWorkingDirectory( String targetRepository )
        throws IOException
    {
        return Files.createTempDirectory( "temp" );
    }

    private void moveFileIfExists( Path fileToMove, Path directory )
    {
        if ( fileToMove != null && Files.exists(fileToMove) )
        {
            Path newLocation = directory.resolve( fileToMove.getFileName() );
            try {
                Files.deleteIfExists(newLocation);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to overwrite existing target file: " + newLocation.toAbsolutePath(), e );
            }

            try {
                Files.createDirectories(newLocation.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Files.move(fileToMove, newLocation );
            } catch (IOException e) {
                try {
                    Files.copy(fileToMove, newLocation);
                } catch (IOException e1) {
                    if (Files.exists(newLocation)) {
                        log.error( "Tried to copy file {} to {} but file with this name already exists.",
                                fileToMove.getFileName(), newLocation.toAbsolutePath() );
                    } else {
                        throw new RuntimeException(
                                "Cannot copy tmp file " + fileToMove.toAbsolutePath() + " to its final location", e );
                    }
                }
            } finally {
                org.apache.archiva.common.utils.FileUtils.deleteQuietly(fileToMove);
            }
        }
    }

    protected String addParameters( String path, RemoteRepository remoteRepository )
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
}
