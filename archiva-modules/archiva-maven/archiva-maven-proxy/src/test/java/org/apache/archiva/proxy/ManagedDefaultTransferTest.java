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

import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.policies.CachedFailuresPolicy;
import org.apache.archiva.policies.ChecksumPolicy;
import org.apache.archiva.policies.ReleasesPolicy;
import org.apache.archiva.policies.SnapshotsPolicy;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * ManagedDefaultTransferTest
 */
public class ManagedDefaultTransferTest
    extends AbstractProxyTestCase
{
    @Test
    public void testGetDefaultLayoutNotPresentConnectorOffline()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Ensure file isn't present first.
        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ONCE, SnapshotsPolicy.ONCE,
                       CachedFailuresPolicy.NO, true );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );
        assertNull( "File should not have been downloaded", downloadedFile );
    }

    @Test
    public void testGetDefaultLayoutNotPresent()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Ensure file isn't present first.
        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ONCE, SnapshotsPolicy.ONCE,
                       CachedFailuresPolicy.NO, false );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        Path sourceFile = Paths.get(REPOPATH_PROXIED1, path);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), sourceFile );
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testGetDefaultLayoutNotPresentPassthrough()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar.asc";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve(path);

        // Ensure file isn't present first.
        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ONCE, SnapshotsPolicy.ONCE,
                       CachedFailuresPolicy.NO, false );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), path );

        Path sourceFile = Paths.get(REPOPATH_PROXIED1, path);
        assertNotNull(downloadedFile);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), sourceFile );
        assertFalse( Files.exists( downloadedFile.getParent().getFilePath().resolve(downloadedFile.getName() + ".sha1" )) );
        assertFalse( Files.exists(downloadedFile.getParent().getFilePath().resolve(downloadedFile.getName() + ".md5" ) ));
        assertFalse( Files.exists( downloadedFile.getParent().getFilePath().resolve(downloadedFile.getName() + ".sha256" ) ));
        assertNoTempFiles( expectedFile );
    }

    /**
     * The attempt here should result in no file being transferred.
     * <p/>
     * The file exists locally, and the policy is ONCE.
     *
     * @throws Exception
     */
    @Test
    public void testGetDefaultLayoutAlreadyPresentPolicyOnce()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve(path);

        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( Files.exists(expectedFile) );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ONCE, SnapshotsPolicy.ONCE,
                       CachedFailuresPolicy.NO, false );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        assertFileEquals( expectedFile, downloadedFile.getFilePath(), expectedFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * The attempt here should result in no file being transferred.
     * <p/>
     * The file exists locally, and the policy is ONCE.
     *
     * @throws Exception
     */
    @Test
    public void testGetDefaultLayoutAlreadyPresentPassthrough()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar.asc";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve(path);
        Path remoteFile = Paths.get(REPOPATH_PROXIED1, path);

        assertTrue( Files.exists(expectedFile) );

        // Set the managed File to be newer than local.
        setManagedOlderThanRemote( expectedFile, remoteFile );
        long originalModificationTime = Files.getLastModifiedTime(expectedFile).toMillis();

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ONCE, SnapshotsPolicy.ONCE,
                       CachedFailuresPolicy.NO, false );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), path );

        assertNotDownloaded( downloadedFile );
        assertNotModified( expectedFile, originalModificationTime );
        assertNoTempFiles( expectedFile );
    }

    /**
     * <p>
     * Request a file, that exists locally, and remotely.
     * </p>
     * <p>
     * All policies are set to IGNORE.
     * </p>
     * <p>
     * Managed file is newer than remote file.
     * </p>
     * <p>
     * Transfer should not have occured, as managed file is newer.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testGetDefaultLayoutAlreadyPresentNewerThanRemotePolicyIgnored()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve(path);
        Path remoteFile = Paths.get(REPOPATH_PROXIED1, path);

        // Set the managed File to be newer than local.
        setManagedNewerThanRemote( expectedFile, remoteFile );

        long originalModificationTime = Files.getLastModifiedTime( expectedFile).toMillis();
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( Files.exists(expectedFile) );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.NO, false );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        assertNotDownloaded( downloadedFile );
        assertNotModified( expectedFile, originalModificationTime );
        assertNoTempFiles( expectedFile );
    }

    /**
     * <p>
     * Request a file, that exists locally, and remotely.
     * </p>
     * <p>
     * All policies are set to IGNORE.
     * </p>
     * <p>
     * Managed file is older than Remote file.
     * </p>
     * <p>
     * Transfer should have occured, as managed file is older than remote.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testGetDefaultLayoutAlreadyPresentOlderThanRemotePolicyIgnored()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve(path);
        Path remoteFile = Paths.get(REPOPATH_PROXIED1, path);

        // Set the managed file to be newer than remote file.
        setManagedOlderThanRemote( expectedFile, remoteFile );

        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( Files.exists(expectedFile) );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.NO, false );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        Path proxiedFile = Paths.get(REPOPATH_PROXIED1, path);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * The attempt here should result in file being transferred.
     * <p/>
     * The file exists locally, is over 6 years old, and the policy is DAILY.
     *
     * @throws Exception
     */
    @Test
    public void testGetDefaultLayoutRemoteUpdate()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( Files.exists(expectedFile) );
        Files.setLastModifiedTime( expectedFile, FileTime.from(getPastDate().getTime(), TimeUnit.MILLISECONDS ));

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.DAILY, SnapshotsPolicy.DAILY,
                       CachedFailuresPolicy.NO, false );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        Path proxiedFile = Paths.get(REPOPATH_PROXIED1, path);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testGetWhenInBothProxiedRepos()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-both-proxies/1.0/get-in-both-proxies-1.0.jar";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, false );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        Path proxied1File = Paths.get(REPOPATH_PROXIED1, path);
        Path proxied2File = Paths.get(REPOPATH_PROXIED2, path);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), proxied1File );
        assertNoTempFiles( expectedFile );

        // TODO: is this check even needed if it passes above? 
        String actualContents = FileUtils.readFileToString( downloadedFile.getFilePath().toFile(), Charset.defaultCharset() );
        String badContents = FileUtils.readFileToString( proxied2File.toFile(), Charset.defaultCharset() );
        assertFalse( "Downloaded file contents should not be that of proxy 2",
                     StringUtils.equals( actualContents, badContents ) );
    }

    @Test
    public void testGetInSecondProxiedRepo()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, false );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        Path proxied2File = Paths.get(REPOPATH_PROXIED2, path);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), proxied2File );
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testNotFoundInAnyProxies()
        throws Exception
    {
        String path = "org/apache/maven/test/does-not-exist/1.0/does-not-exist-1.0.jar";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, false );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        assertNull( "File returned was: " + downloadedFile + "; should have got a not found exception",
                    downloadedFile );
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testGetInSecondProxiedRepoFirstFails()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Repository (usually done within archiva.xml configuration)
        saveRemoteRepositoryConfig( "badproxied", "Bad Proxied", "" +
            "http://bad.machine.com/repo/", "default" );

        wagonMock.get( EasyMock.eq( path), EasyMock.anyObject( File.class ) );
        EasyMock.expectLastCall().andThrow( new ResourceDoesNotExistException( "transfer failed" )  );
        wagonMockControl.replay();

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "badproxied", false );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, false );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        wagonMockControl.verify();

        Path proxied2File = Paths.get(REPOPATH_PROXIED2, path);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), proxied2File );
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testGetAllRepositoriesFail()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        setupTestableManagedRepository( path );

        Path expectedFile = managedDefaultDir.resolve( path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertNotExistsInManagedDefaultRepo( expectedFile );

        // Configure Repository (usually done within archiva.xml configuration)
        saveRemoteRepositoryConfig( "badproxied1", "Bad Proxied 1", "http://bad.machine.com/repo/", "default" );
        saveRemoteRepositoryConfig( "badproxied2", "Bad Proxied 2", "http://dead.machine.com/repo/", "default" );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "badproxied1", false );
        saveConnector( ID_DEFAULT_MANAGED, "badproxied2", false );

        Path tmpFile = expectedFile.getParent().resolve(expectedFile.getFileName() + ".tmp" );

        wagonMock.get( EasyMock.eq( path ), EasyMock.anyObject( File.class ) );
        EasyMock.expectLastCall().andThrow( new ResourceDoesNotExistException( "Can't find resource." ) );

        wagonMock.get( EasyMock.eq( path ), EasyMock.anyObject( File.class ) );
        EasyMock.expectLastCall().andThrow( new ResourceDoesNotExistException( "Can't find resource." ) );

        wagonMockControl.replay();

        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        assertNotDownloaded( downloadedFile );

        wagonMockControl.verify();
        assertNoTempFiles( expectedFile );

        // TODO: do not want failures to present as a not found [MRM-492]
        // TODO: How much information on each failure should we pass back to the user vs. logging in the proxy? 
    }


}
