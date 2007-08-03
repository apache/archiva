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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.policies.CachedFailuresPolicy;
import org.apache.maven.archiva.policies.ChecksumPolicy;
import org.apache.maven.archiva.policies.ReleasesPolicy;
import org.apache.maven.archiva.policies.SnapshotsPolicy;
import org.apache.maven.wagon.TransferFailedException;

import java.io.File;

/**
 * ManagedDefaultTransferTest 
 *
 * @author Brett Porter
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ManagedDefaultTransferTest
    extends AbstractProxyTestCase
{
    public void testGetDefaultLayoutNotPresent()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        // Ensure file isn't present first.
        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ONCE, SnapshotsPolicy.ONCE,
                       CachedFailuresPolicy.IGNORED );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File sourceFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, sourceFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * The attempt here should result in no file being transferred.
     * 
     * The file exists locally, and the policy is ONCE.
     * 
     * @throws Exception
     */
    public void testGetDefaultLayoutAlreadyPresentPolicyOnce()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );

        ArtifactReference artifact = createArtifactReference( "default", path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.ONCE, SnapshotsPolicy.ONCE,
                       CachedFailuresPolicy.IGNORED );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertFileEquals( expectedFile, downloadedFile, expectedFile );
        assertNoTempFiles( expectedFile );
    }

    /**
     * The attempt here should result in file being transferred.
     * 
     * The file exists locally, and the policy is IGNORE.
     * 
     * @throws Exception
     */
    public void testGetDefaultLayoutAlreadyPresentPolicyIgnored()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );

        long originalModificationTime = expectedFile.lastModified();
        ArtifactReference artifact = createArtifactReference( "default", path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );

        long proxiedLastModified = proxiedFile.lastModified();
        long downloadedLastModified = downloadedFile.lastModified();
        assertFalse( "Check file timestamp is not that of proxy:", proxiedLastModified == downloadedLastModified );

        if ( originalModificationTime != downloadedLastModified )
        {
            /* On some systems the timestamp functions are not accurate enough.
             * This delta is the amount of milliseconds of 'fudge factor' we allow for
             * the unit test to still be considered 'passed'.
             */
            int delta = 1100;

            long hirange = originalModificationTime + ( delta / 2 );
            long lorange = originalModificationTime - ( delta / 2 );

            if ( ( downloadedLastModified < lorange ) || ( downloadedLastModified > hirange ) )
            {
                fail( "Check file timestamp is that of original managed file: expected within range lo:<" + lorange
                    + "> hi:<" + hirange + "> but was:<" + downloadedLastModified + ">" );
            }
        }
        assertNoTempFiles( expectedFile );
    }

    /**
     * The attempt here should result in file being transferred.
     * 
     * The file exists locally, is over 6 years old, and the policy is DAILY.
     * 
     * @throws Exception
     */
    public void testGetDefaultLayoutRemoteUpdate()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        assertTrue( expectedFile.exists() );
        expectedFile.setLastModified( getPastDate().getTime() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.DAILY,
                       SnapshotsPolicy.DAILY, CachedFailuresPolicy.IGNORED );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testGetWhenInBothProxiedRepos()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-both-proxies/1.0/get-in-both-proxies-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        File proxied2File = new File( REPOPATH_PROXIED2, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );

        // TODO: is this check even needed if it passes above? 
        String actualContents = FileUtils.readFileToString( downloadedFile, null );
        String badContents = FileUtils.readFileToString( proxied2File, null );
        assertFalse( "Downloaded file contents should not be that of proxy 2", StringUtils.equals( actualContents,
                                                                                                   badContents ) );
    }

    public void testGetInSecondProxiedRepo()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied2File = new File( REPOPATH_PROXIED2, path );
        assertFileEquals( expectedFile, downloadedFile, proxied2File );
        assertNoTempFiles( expectedFile );
    }

    public void testNotFoundInAnyProxies()
        throws Exception
    {
        String path = "org/apache/maven/test/does-not-exist/1.0/does-not-exist-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );
        saveConnector( ID_DEFAULT_MANAGED, ID_LEGACY_PROXIED, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertNull( "File returned was: " + downloadedFile + "; should have got a not found exception", downloadedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testGetInSecondProxiedRepoFirstFails()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        // Configure Repository (usually done within archiva.xml configuration)
        saveRepositoryConfig( "badproxied", "Bad Proxied", "test://bad.machine.com/repo/", "default" );

        wagonMock.getIfNewer( path, new File( expectedFile.getAbsolutePath() + ".tmp" ), 0 );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );
        wagonMockControl.replay();

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "badproxied", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED2, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        wagonMockControl.verify();

        File proxied2File = new File( REPOPATH_PROXIED2, path );
        assertFileEquals( expectedFile, downloadedFile, proxied2File );
        assertNoTempFiles( expectedFile );
    }

    public void testGetAllRepositoriesFail()
        throws Exception
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        // Configure Repository (usually done within archiva.xml configuration)
        saveRepositoryConfig( "badproxied1", "Bad Proxied 1", "test://bad.machine.com/repo/", "default" );
        saveRepositoryConfig( "badproxied2", "Bad Proxied 2", "test://dead.machine.com/repo/", "default" );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "badproxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );
        saveConnector( ID_DEFAULT_MANAGED, "badproxied2", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        wagonMock.getIfNewer( path, new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" ), 0 );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );

        wagonMock.getIfNewer( path, new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" ), 0 );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );

        wagonMockControl.replay();

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertNotDownloaded( downloadedFile );

        wagonMockControl.verify();
        assertNoTempFiles( expectedFile );

        // TODO: do not want failures to present as a not found!
        // TODO: How much information on each failure should we pass back to the user vs. logging in the proxy? 
    }

    public void testLegacyProxyRepoGetAlreadyPresent()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_LEGACY_PROXIED, ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED_LEGACY,
                                     "org.apache.maven.test/jars/get-default-layout-present-1.0.jar" );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testLegacyRequestConvertedToDefaultPathInManagedRepo()
        throws Exception
    {
        // Check that a Maven1 legacy request is translated to a maven2 path in
        // the managed repository.

        String legacyPath = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_LEGACY_PROXIED, ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED_LEGACY, legacyPath );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testLegacyProxyRepoGetNotPresent()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        expectedFile.delete();
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_LEGACY_PROXIED, ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxiedFile = new File( REPOPATH_PROXIED_LEGACY, "org.apache.maven.test/jars/get-default-layout-1.0.jar" );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );

        // TODO: timestamp preservation requires support for that in wagon
        //    assertEquals( "Check file timestamp", proxiedFile.lastModified(), file.lastModified() );
    }
}
