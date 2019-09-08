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
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SnapshotTransferTest 
 *
 *
 */
public class SnapshotTransferTest
    extends AbstractProxyTestCase
{
    @Test
    public void testSnapshotNonExistant()
        throws Exception
    {
        String path = "org/apache/maven/test/does-not-exist/1.0-SNAPSHOT/does-not-exist-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        Files.deleteIfExists(expectedFile);
        assertFalse( Files.exists(expectedFile) );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );
        assertNotDownloaded( downloadedFile );
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testTimestampDrivenSnapshotNotPresentAlready()
        throws Exception
    {
        String path = "org/apache/maven/test/get-timestamped-snapshot/1.0-SNAPSHOT/get-timestamped-snapshot-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        Files.deleteIfExists(expectedFile);
        assertFalse( Files.exists(expectedFile) );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        Path proxiedFile = Paths.get(REPOPATH_PROXIED1, path);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testNewerTimestampDrivenSnapshotOnFirstRepo()
        throws Exception
    {
        String path = "org/apache/maven/test/get-present-timestamped-snapshot/1.0-SNAPSHOT/get-present-timestamped-snapshot-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( Files.exists(expectedFile) );
        Files.setLastModifiedTime( expectedFile, FileTime.from( getPastDate().getTime(), TimeUnit.MILLISECONDS ));

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        Path proxiedFile = Paths.get(REPOPATH_PROXIED1, path);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testOlderTimestampDrivenSnapshotOnFirstRepo()
        throws Exception
    {
        String path = "org/apache/maven/test/get-present-timestamped-snapshot/1.0-SNAPSHOT/get-present-timestamped-snapshot-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        Path expectedFile = managedDefaultDir.resolve(path);
        Path remoteFile = Paths.get(REPOPATH_PROXIED1, path);
        
        setManagedNewerThanRemote( expectedFile, remoteFile );
        
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false );

        // Attempt to download.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        // Should not have downloaded as managed is newer than remote.
        assertNotDownloaded( downloadedFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * TODO: Has problems with wagon implementation not preserving timestamp.
     */
    /*
    public void testNewerTimestampDrivenSnapshotOnSecondRepoThanFirstNotPresentAlready()
        throws Exception
    {
        String path = "org/apache/maven/test/get-timestamped-snapshot-in-both/1.0-SNAPSHOT/get-timestamped-snapshot-in-both-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = createArtifactReference( "default", path );

        Files.delete(expectedFile);
        assertFalse( Files.exists(expectedFile) );

        // Create customized proxy / target repository
        File targetProxyDir = saveTargetedRepositoryConfig( ID_PROXIED1_TARGET, REPOPATH_PROXIED1,
                                                            REPOPATH_PROXIED1_TARGET, "default" );

        new File( targetProxyDir, path ).setLastModified( getPastDate().getTime() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1_TARGET, ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        // Should have downloaded the content from proxy2, as proxy1 has an old (by file.lastModified check) version.
        Path proxiedFile = Paths.get(REPOPATH_PROXIED2, path);
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    } 

    public void testOlderTimestampDrivenSnapshotOnSecondRepoThanFirstNotPresentAlready()
        throws Exception
    {
        String path = "org/apache/maven/test/get-timestamped-snapshot-in-both/1.0-SNAPSHOT/get-timestamped-snapshot-in-both-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = createArtifactReference( "default", path );

        Files.delete(expectedFile);
        assertFalse( Files.exists(expectedFile) );

        // Create customized proxy / target repository
        File targetProxyDir = saveTargetedRepositoryConfig( ID_PROXIED2_TARGET, REPOPATH_PROXIED2,
                                                            REPOPATH_PROXIED2_TARGET, "default" );

        new File( targetProxyDir, path ).setLastModified( getPastDate().getTime() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2_TARGET, ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED1_TARGET, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    } */

    @Test
    public void testTimestampDrivenSnapshotNotExpired()
        throws Exception
    {
        String path = "org/apache/maven/test/get-present-timestamped-snapshot/1.0-SNAPSHOT/get-present-timestamped-snapshot-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( Files.exists(expectedFile) );

        Path proxiedFile = Paths.get(REPOPATH_PROXIED1, path);
        Files.setLastModifiedTime( proxiedFile, FileTime.from( getFutureDate().getTime(), TimeUnit.MILLISECONDS ));

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        assertFileEquals( expectedFile, downloadedFile.getFilePath(), proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testTimestampDrivenSnapshotNotUpdated()
        throws Exception
    {
        String path = "org/apache/maven/test/get-present-timestamped-snapshot/1.0-SNAPSHOT/get-present-timestamped-snapshot-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        Path expectedFile = managedDefaultDir.resolve(path);
        Path remoteFile = Paths.get(REPOPATH_PROXIED1, path);

        setManagedNewerThanRemote( expectedFile, remoteFile, 12000000 );
        long expectedTimestamp = Files.getLastModifiedTime( expectedFile ).toMillis();
        
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        assertNotDownloaded( downloadedFile );
        assertNotModified( expectedFile, expectedTimestamp );
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testTimestampDrivenSnapshotNotPresentAlreadyExpiredCacheFailure()
        throws Exception
    {
        String path = "org/apache/maven/test/get-timestamped-snapshot/1.0-SNAPSHOT/get-timestamped-snapshot-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        Files.deleteIfExists(expectedFile);
        assertFalse( Files.exists(expectedFile) );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.IGNORE, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.YES , false);
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, ChecksumPolicy.IGNORE, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.YES , false);

        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        Path proxiedFile = Paths.get(REPOPATH_PROXIED1, path);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testMetadataDrivenSnapshotNotPresentAlready()
        throws Exception
    {
        String path = "org/apache/maven/test/get-metadata-snapshot/1.0-SNAPSHOT/get-metadata-snapshot-1.0-20050831.101112-1.jar";
        setupTestableManagedRepository( path );
        
        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        Files.deleteIfExists(expectedFile);
        assertFalse( Files.exists(expectedFile) );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        Path proxiedFile = Paths.get(REPOPATH_PROXIED1, path);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    @Test
    public void testGetMetadataDrivenSnapshotRemoteUpdate()
        throws Exception
    {
        // Metadata driven snapshots (using a full timestamp) are treated like a release. It is the timing of the
        // updates to the metadata files that triggers which will be downloaded

        String path = "org/apache/maven/test/get-present-metadata-snapshot/1.0-SNAPSHOT/get-present-metadata-snapshot-1.0-20050831.101112-1.jar";
        setupTestableManagedRepository( path );
        
        Path expectedFile = managedDefaultDir.resolve(path);
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( Files.exists(expectedFile) );

        Files.setLastModifiedTime( expectedFile, FileTime.from( getPastDate().getTime(), TimeUnit.MILLISECONDS ));

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        Path proxiedFile = Paths.get(REPOPATH_PROXIED1, path);
        assertFileEquals( expectedFile, downloadedFile.getFilePath(), proxiedFile );
        assertNoTempFiles( expectedFile );
    }
}
