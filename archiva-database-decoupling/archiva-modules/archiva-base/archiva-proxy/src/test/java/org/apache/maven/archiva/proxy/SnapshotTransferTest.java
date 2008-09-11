package org.apache.maven.archiva.proxy;

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

import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.policies.CachedFailuresPolicy;
import org.apache.maven.archiva.policies.ChecksumPolicy;
import org.apache.maven.archiva.policies.ReleasesPolicy;
import org.apache.maven.archiva.policies.SnapshotsPolicy;

import java.io.File;

/**
 * SnapshotTransferTest 
 *
 * @author Brett Porter
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class SnapshotTransferTest
    extends AbstractProxyTestCase
{
    public void testSnapshotNonExistant()
        throws Exception
    {
        String path = "org/apache/maven/test/does-not-exist/1.0-SNAPSHOT/does-not-exist-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );
        assertNotDownloaded( downloadedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testTimestampDrivenSnapshotNotPresentAlready()
        throws Exception
    {
        String path = "org/apache/maven/test/get-timestamped-snapshot/1.0-SNAPSHOT/get-timestamped-snapshot-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testNewerTimestampDrivenSnapshotOnFirstRepo()
        throws Exception
    {
        String path = "org/apache/maven/test/get-present-timestamped-snapshot/1.0-SNAPSHOT/get-present-timestamped-snapshot-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( expectedFile.exists() );
        expectedFile.setLastModified( getPastDate().getTime() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testOlderTimestampDrivenSnapshotOnFirstRepo()
        throws Exception
    {
        String path = "org/apache/maven/test/get-present-timestamped-snapshot/1.0-SNAPSHOT/get-present-timestamped-snapshot-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        File remoteFile = new File( REPOPATH_PROXIED1, path );
        
        setManagedNewerThanRemote( expectedFile, remoteFile );
        
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false );

        // Attempt to download.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

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
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

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
        File proxiedFile = new File( REPOPATH_PROXIED2, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    } 

    public void testOlderTimestampDrivenSnapshotOnSecondRepoThanFirstNotPresentAlready()
        throws Exception
    {
        String path = "org/apache/maven/test/get-timestamped-snapshot-in-both/1.0-SNAPSHOT/get-timestamped-snapshot-in-both-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

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

    public void testTimestampDrivenSnapshotNotExpired()
        throws Exception
    {
        String path = "org/apache/maven/test/get-present-timestamped-snapshot/1.0-SNAPSHOT/get-present-timestamped-snapshot-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( expectedFile.exists() );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        proxiedFile.setLastModified( getFutureDate().getTime() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testTimestampDrivenSnapshotNotUpdated()
        throws Exception
    {
        String path = "org/apache/maven/test/get-present-timestamped-snapshot/1.0-SNAPSHOT/get-present-timestamped-snapshot-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        File remoteFile = new File( REPOPATH_PROXIED1, path );

        setManagedNewerThanRemote( expectedFile, remoteFile );
        long expectedTimestamp = expectedFile.lastModified(); 
        
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertNotDownloaded( downloadedFile );
        assertNotModified( expectedFile, expectedTimestamp );
        assertNoTempFiles( expectedFile );
    }

    public void testTimestampDrivenSnapshotNotPresentAlreadyExpiredCacheFailure()
        throws Exception
    {
        String path = "org/apache/maven/test/get-timestamped-snapshot/1.0-SNAPSHOT/get-timestamped-snapshot-1.0-SNAPSHOT.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.IGNORE, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.YES , false);
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, ChecksumPolicy.IGNORE, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.YES , false);

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testMetadataDrivenSnapshotNotPresentAlready()
        throws Exception
    {
        String path = "org/apache/maven/test/get-metadata-snapshot/1.0-SNAPSHOT/get-metadata-snapshot-1.0-20050831.101112-1.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testGetMetadataDrivenSnapshotRemoteUpdate()
        throws Exception
    {
        // Metadata driven snapshots (using a full timestamp) are treated like a release. It is the timing of the
        // updates to the metadata files that triggers which will be downloaded

        String path = "org/apache/maven/test/get-present-metadata-snapshot/1.0-SNAPSHOT/get-present-metadata-snapshot-1.0-20050831.101112-1.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        assertTrue( expectedFile.exists() );

        expectedFile.setLastModified( getPastDate().getTime() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, false);

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }
}
