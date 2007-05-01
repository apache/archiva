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
 * ManagedLegacyTransferTest 
 *
 * @author Brett Porter
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ManagedLegacyTransferTest
    extends AbstractProxyTestCase
{
    public void testLegacyManagedRepoGetNotPresent()
        throws Exception
    {
        String path = "org.apache.maven.test/jars/get-default-layout-1.0.jar";
        File expectedFile = new File( managedLegacyDir, path );
        ArtifactReference artifact = createArtifactReference( "legacy", path );
    
        expectedFile.delete();
        assertFalse( expectedFile.exists() );
    
        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );
    
        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );
    
        File proxied2File = new File( REPOPATH_PROXIED1,
                                      "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar" );
        assertFileEquals( expectedFile, downloadedFile, proxied2File );
        assertNoTempFiles( expectedFile );
    
        // TODO: timestamp preservation requires support for that in wagon
        //    assertEquals( "Check file timestamp", proxiedFile.lastModified(), file.lastModified() );
    }

    public void testLegacyManagedRepoGetAlreadyPresent()
        throws Exception
    {
        String path = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        File expectedFile = new File( managedLegacyDir, path );
        ArtifactReference artifact = createArtifactReference( "legacy", path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );

        File proxied2File = new File( REPOPATH_PROXIED1,
                                      "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar" );
        assertFileEquals( expectedFile, downloadedFile, proxied2File );
        assertNoTempFiles( expectedFile );
    }

    public void testLegacyManagedAndProxyRepoGetNotPresent()
        throws Exception
    {
        String path = "org.apache.maven.test/jars/get-default-layout-1.0.jar";
        File expectedFile = new File( managedLegacyDir, path );
        ArtifactReference artifact = createArtifactReference( "legacy", path );

        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_LEGACY_PROXIED, ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED_LEGACY, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );

        // TODO: timestamp preservation requires support for that in wagon
        //    assertEquals( "Check file timestamp", proxiedFile.lastModified(), file.lastModified() );
    }

    public void testLegacyManagedAndProxyRepoGetAlreadyPresent()
        throws Exception
    {
        String path = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        File expectedFile = new File( managedLegacyDir, path );
        ArtifactReference artifact = createArtifactReference( "legacy", path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_LEGACY_PROXIED, ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED_LEGACY, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testDefaultRequestConvertedToLegacyPathInManagedRepo()
        throws Exception
    {
        // Check that a Maven2 default request is translated to a legacy path in
        // the managed repository.

        String legacyPath = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        File expectedFile = new File( managedLegacyDir, legacyPath );
        ArtifactReference artifact = createArtifactReference( "legacy", legacyPath );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_LEGACY_MANAGED, ID_PROXIED1, ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedLegacyRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }
}
