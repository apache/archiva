package org.apache.archiva.metadata.repository.storage.maven2;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;
import java.util.Map;

import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.proxy.common.WagonFactory;
import org.apache.archiva.proxy.common.WagonFactoryException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.xml.XMLException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryModelResolver
    implements ModelResolver
{
    private File basedir;

    private RepositoryPathTranslator pathTranslator;

    private WagonFactory wagonFactory;

    private List<RemoteRepositoryConfiguration> remoteRepositories;

    private ManagedRepositoryConfiguration targetRepository;

    private static final Logger log = LoggerFactory.getLogger( RepositoryModelResolver.class );

    private static final String METADATA_FILENAME = "maven-metadata.xml";

    // key/value: remote repo ID/network proxy
    Map<String, ProxyInfo> networkProxyMap;

    public RepositoryModelResolver( File basedir, RepositoryPathTranslator pathTranslator )
    {
        this.basedir = basedir;

        this.pathTranslator = pathTranslator;
    }

    public RepositoryModelResolver( File basedir, RepositoryPathTranslator pathTranslator,
                                    WagonFactory wagonFactory, List<RemoteRepositoryConfiguration> remoteRepositories,
                                    Map<String, ProxyInfo> networkProxiesMap, ManagedRepositoryConfiguration targetRepository )
    {
        this( basedir, pathTranslator );

        this.wagonFactory = wagonFactory;

        this.remoteRepositories = remoteRepositories;

        this.networkProxyMap = networkProxiesMap;

        this.targetRepository = targetRepository;
    }

    public ModelSource resolveModel( String groupId, String artifactId, String version )
        throws UnresolvableModelException
    {
        String filename = artifactId + "-" + version + ".pom";
        // TODO: we need to convert 1.0-20091120.112233-1 type paths to baseVersion for the below call - add a test

        File model = pathTranslator.toFile( basedir, groupId, artifactId, version, filename );

        if( !model.exists() )
        {
            for( RemoteRepositoryConfiguration remoteRepository : remoteRepositories )
            {
                try
                {
                    boolean success = getModelFromProxy( remoteRepository, groupId, artifactId, version, filename );
                    if( success && model.exists() )
                    {
                        log.info( "Model '" + model.getAbsolutePath() + "' successfully retrieved from remote repository '"
                            + remoteRepository.getId() + "'" );
                        break;
                    }
                }
                catch( Exception e )
                {
                    log.warn( "An exception was caught while attempting to retrieve model '" + model.getAbsolutePath()
                        + "' from remote repository '" + remoteRepository.getId() + "'.", e );
                    continue;
                }
            }
        }

        return new FileModelSource( model );
    }

    public void addRepository( Repository repository )
        throws InvalidRepositoryException
    {
        // we just ignore repositories outside of the current one for now
        // TODO: it'd be nice to look them up from Archiva's set, but we want to do that by URL / mapping, not just the
        //       ID since they will rarely match
    }

    public ModelResolver newCopy()
    {
        return new RepositoryModelResolver( basedir, pathTranslator );
    }

    // FIXME: we need to do some refactoring, we cannot re-use the proxy components of archiva-proxy in maven2-repository
    // because it's causing a cyclic dependency
    private boolean getModelFromProxy( RemoteRepositoryConfiguration remoteRepository, String groupId,
                                    String artifactId, String version, String filename )
        throws AuthorizationException, TransferFailedException, ResourceDoesNotExistException, WagonFactoryException,
        XMLException
    {
        boolean success = false;
        File tmpMd5 = null;
        File tmpSha1 = null;
        File tmpResource = null;
        String artifactPath = pathTranslator.toPath( groupId, artifactId, version, filename );
        File resource = new File( targetRepository.getLocation(), artifactPath );

        File workingDirectory = createWorkingDirectory( targetRepository.getLocation() );
        try
        {
            Wagon wagon = null;
            try
            {
                String protocol = getProtocol( remoteRepository.getUrl() );

                wagon = wagonFactory.getWagon( "wagon#" + protocol );
                if ( wagon == null )
                {
                    throw new RuntimeException( "Unsupported remote repository protocol: " + protocol );
                }

                boolean connected = connectToRepository( wagon, remoteRepository );
                if ( connected )
                {
                    tmpResource = new File( workingDirectory, filename );

                    if ( VersionUtil.isSnapshot( version ) )
                    {
                        // get the metadata first!
                        File tmpMetadataResource = new File( workingDirectory, METADATA_FILENAME );

                        String metadataPath = StringUtils.substringBeforeLast( artifactPath, "/" ) + "/" + METADATA_FILENAME;

                        wagon.get( metadataPath, tmpMetadataResource );

                        log.debug( "Successfully downloaded metadata." );

                        MavenRepositoryMetadata metadata = MavenRepositoryMetadataReader.read( tmpMetadataResource );

                        // re-adjust to timestamp if present, otherwise retain the original -SNAPSHOT filename
                        MavenRepositoryMetadata.Snapshot snapshotVersion = metadata.getSnapshotVersion();
                        String timestampVersion = version;
                        if ( snapshotVersion != null )
                        {
                            timestampVersion =
                                timestampVersion.substring( 0, timestampVersion.length() - 8 ); // remove SNAPSHOT from end
                            timestampVersion =
                                timestampVersion + snapshotVersion.getTimestamp() + "-" + snapshotVersion.getBuildNumber();

                            filename = artifactId + "-" + timestampVersion + ".pom";

                            artifactPath = pathTranslator.toPath( groupId, artifactId, version, filename );

                            log.debug( "New artifactPath : " + artifactPath );
                        }
                    }

                    log.info( "Retrieving " + artifactPath + " from " + remoteRepository.getName() );

                    wagon.get( artifactPath, tmpResource );

                    log.debug( "Downloaded successfully." );

                    tmpSha1 =
                        transferChecksum( wagon, remoteRepository, artifactPath, tmpResource, workingDirectory, ".sha1" );
                    tmpMd5 =
                        transferChecksum( wagon, remoteRepository, artifactPath, tmpResource, workingDirectory, ".md5" );
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
                synchronized ( resource.getAbsolutePath().intern() )
                {
                    File directory = resource.getParentFile();
                    moveFileIfExists( tmpMd5, directory );
                    moveFileIfExists( tmpSha1, directory );
                    moveFileIfExists( tmpResource, directory );
                    success = true;
                }
            }
        }
        finally
        {
            FileUtils.deleteQuietly( workingDirectory );
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
    private boolean connectToRepository( Wagon wagon, RemoteRepositoryConfiguration remoteRepository )
    {
        boolean connected;

        final ProxyInfo networkProxy;
        networkProxy = this.networkProxyMap.get( remoteRepository.getId() );

        if ( networkProxy != null )
        {
            String msg =
                "Using network proxy " + networkProxy.getHost() + ":" + networkProxy.getPort()
                    + " to connect to remote repository " + remoteRepository.getUrl();
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
        String username = remoteRepository.getUsername();
        String password = remoteRepository.getPassword();

        if ( StringUtils.isNotBlank( username ) && StringUtils.isNotBlank( password ) )
        {
            log.debug( "Using username " + username + " to connect to remote repository " + remoteRepository.getUrl() );
            authInfo = new AuthenticationInfo();
            authInfo.setUserName( username );
            authInfo.setPassword( password );
        }

        // Convert seconds to milliseconds
        int timeoutInMilliseconds = remoteRepository.getTimeout() * 1000;

        // Set timeout
        wagon.setTimeout( timeoutInMilliseconds );

        try
        {
            org.apache.maven.wagon.repository.Repository wagonRepository =
                new org.apache.maven.wagon.repository.Repository( remoteRepository.getId(), remoteRepository.getUrl() );
            wagon.connect( wagonRepository, authInfo, networkProxy );
            connected = true;
        }
        catch ( ConnectionException e )
        {
            log.error( "Could not connect to " + remoteRepository.getName() + ": " + e.getMessage() );
            connected = false;
        }
        catch ( AuthenticationException e )
        {
            log.error( "Could not connect to " + remoteRepository.getName() + ": " + e.getMessage() );
            connected = false;
        }

        return connected;
    }

    private File transferChecksum( Wagon wagon, RemoteRepositoryConfiguration remoteRepository, String remotePath,
                                   File resource, File tmpDirectory, String ext )
        throws AuthorizationException, TransferFailedException, ResourceDoesNotExistException
    {
        File destFile = new File( tmpDirectory, resource.getName() + ext );

        log.info( "Retrieving " + remotePath + " from " + remoteRepository.getName() );

        wagon.get( remotePath, destFile );

        log.debug( "Downloaded successfully." );

        return destFile;
    }

    private String getProtocol( String url )
    {
        String protocol = StringUtils.substringBefore( url, ":" );

        return protocol;
    }

    private File createWorkingDirectory( String targetRepository )
    {
        try
        {
            File tmpDir = File.createTempFile( ".workingdirectory", null, new File( targetRepository ) );
            tmpDir.delete();
            tmpDir.mkdirs();
            return tmpDir;
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Could not create working directory for this request", e );
        }
    }

    private void moveFileIfExists( File fileToMove, File directory )
    {
        if ( fileToMove != null && fileToMove.exists() )
        {
            File newLocation = new File( directory, fileToMove.getName() );
            if ( newLocation.exists() && !newLocation.delete() )
            {
                throw new RuntimeException( "Unable to overwrite existing target file: " + newLocation.getAbsolutePath() );
            }

            newLocation.getParentFile().mkdirs();
            if ( !fileToMove.renameTo( newLocation ) )
            {
                log.warn( "Unable to rename tmp file to its final name... resorting to copy command." );

                try
                {
                    FileUtils.copyFile( fileToMove, newLocation );
                }
                catch ( IOException e )
                {
                    if ( newLocation.exists() )
                    {
                        log.error( "Tried to copy file " + fileToMove.getName() + " to " + newLocation.getAbsolutePath()
                            + " but file with this name already exists." );
                    }
                    else
                    {
                        throw new RuntimeException( "Cannot copy tmp file " + fileToMove.getAbsolutePath()
                            + " to its final location", e );
                    }
                }
                finally
                {
                    FileUtils.deleteQuietly( fileToMove );
                }
            }
        }
    }
}
