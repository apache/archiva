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

import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.archiva.policies.CachedFailuresPolicy;
import org.apache.archiva.policies.ChecksumPolicy;
import org.apache.archiva.policies.ReleasesPolicy;
import org.apache.archiva.policies.SnapshotsPolicy;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * ManagedLegacyTransferTest 
 *
 * @version $Id$
 */
public class ManagedLegacyTransferTest
    extends AbstractProxyTestCase
{
    /**
     * Incoming request on a Managed Legacy repository, for content that does not
     * exist in the managed legacy repository, but does exist on a remote default layout repository.
     */
    @Test
    public void testManagedLegacyNotPresentRemoteDefaultPresent()
        throws Exception
    {
        String path = "org.apache.maven.test/jars/get-default-layout-1.0.jar";
        File expectedFile = new File( managedLegacyDir, path );
        ArtifactReference artifact = managedLegacyRepository.toArtifactReference( path );

        assertNotExistsInManagedLegacyRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_PROXIED1, false );

        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );

        File proxied2File = new File( REPOPATH_PROXIED1,
                                      "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar" );
        assertFileEquals( expectedFile, downloadedFile, proxied2File );
        assertNoTempFiles( expectedFile );
    }

    /**
     * Incoming request on a Managed Legacy repository, for content that already
     * exist in the managed legacy repository, and also exist on a remote default layout repository.
     */
    @Test
    public void testManagedLegacyPresentRemoteDefaultPresent()
        throws Exception
    {
        String path = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        String remotePath = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        
        File expectedFile = new File( managedLegacyDir, path );
        File remoteFile = new File( REPOPATH_PROXIED1, remotePath );

        setManagedOlderThanRemote( expectedFile, remoteFile );
        
        ArtifactReference artifact = managedLegacyRepository.toArtifactReference( path );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ALWAYS,
                       SnapshotsPolicy.ALWAYS, CachedFailuresPolicy.NO, false );

        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );

        assertFileEquals( expectedFile, downloadedFile, remoteFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * Incoming request on a Managed Legacy repository, for content that does not
     * exist in the managed legacy repository, and does not exist on a remote legacy layout repository.
     */
    @Test
    public void testManagedLegacyNotPresentRemoteLegacyPresent()
        throws Exception
    {
        String path = "org.apache.maven.test/plugins/get-legacy-plugin-1.0.jar";
        File expectedFile = new File( managedLegacyDir, path );
        ArtifactReference artifact = managedLegacyRepository.toArtifactReference( path );

        assertNotExistsInManagedLegacyRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_LEGACY_PROXIED, false );

        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED_LEGACY, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * Incoming request on a Managed Legacy repository, for content that does exist in the 
     * managed legacy repository, and also exists on a remote legacy layout repository. 
     */
    @Test
    public void testManagedLegacyPresentRemoteLegacyPresent()
        throws Exception
    {
        String path = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        File expectedFile = new File( managedLegacyDir, path );
        File remoteFile = new File( REPOPATH_PROXIED_LEGACY, path );

        setManagedOlderThanRemote( expectedFile, remoteFile );
        
        ArtifactReference artifact = managedLegacyRepository.toArtifactReference( path );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_LEGACY_PROXIED, false );

        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );

        assertFileEquals( expectedFile, downloadedFile, remoteFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * Incoming request on a Managed Legacy repository, for content that does exist in the 
     * managed legacy repository, and does not exist on a remote legacy layout repository. 
     */
    @Test
    public void testManagedLegacyPresentRemoteLegacyNotPresent()
        throws Exception
    {
        String path = "org.apache.maven.test/jars/managed-only-lib-2.1.jar";
        File expectedFile = new File( managedLegacyDir, path );
        ArtifactReference artifact = managedLegacyRepository.toArtifactReference( path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_LEGACY_PROXIED, false );

        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );

        assertNotDownloaded( downloadedFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * Incoming request on a Managed Legacy repository, for content that does exist in the 
     * managed legacy repository, and does not exists on a remote default layout repository. 
     */
    @Test
    public void testManagedLegacyPresentRemoteDefaultNotPresent()
        throws Exception
    {
        String path = "org.apache.maven.test/jars/managed-only-lib-2.1.jar";
        File expectedFile = new File( managedLegacyDir, path );
        ArtifactReference artifact = managedLegacyRepository.toArtifactReference( path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_PROXIED1, false );

        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );

        assertNotDownloaded( downloadedFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * Incoming request on a Managed Legacy repository, for content that does not exist in the 
     * managed legacy repository, and does not exists on a remote legacy layout repository. 
     */
    @Test
    public void testManagedLegacyNotPresentRemoteLegacyNotPresent()
        throws Exception
    {
        String path = "org.apache.archiva.test/jars/mystery-lib-1.0.jar";
        File expectedFile = new File( managedLegacyDir, path );
        ArtifactReference artifact = managedLegacyRepository.toArtifactReference( path );

        assertNotExistsInManagedLegacyRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_LEGACY_PROXIED, false );

        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );

        assertNotDownloaded( downloadedFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * Incoming request on a Managed Legacy repository, for content that does not exist in the 
     * managed legacy repository, and does not exists on a remote default layout repository. 
     */
    @Test
    public void testManagedLegacyNotPresentRemoteDefaultNotPresent()
        throws Exception
    {
        String path = "org.apache.archiva.test/jars/mystery-lib-2.1.jar";
        File expectedFile = new File( managedLegacyDir, path );
        ArtifactReference artifact = managedLegacyRepository.toArtifactReference( path );

        assertNotExistsInManagedLegacyRepo( expectedFile );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_PROXIED1, false );

        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );

        assertNotDownloaded( downloadedFile );
        assertNoTempFiles( expectedFile );
    }
}
