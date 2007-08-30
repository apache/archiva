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
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.policies.CachedFailuresPolicy;
import org.apache.maven.archiva.policies.ChecksumPolicy;
import org.apache.maven.archiva.policies.ReleasesPolicy;
import org.apache.maven.archiva.policies.SnapshotsPolicy;
import org.apache.maven.wagon.TransferFailedException;

import java.io.File;

/**
 * ChecksumTransferTest 
 *
 * @author Brett Porter
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ChecksumTransferTest
    extends AbstractProxyTestCase
{
    public void testGetChecksumBothCorrect()
        throws Exception
    {
        String path = "org/apache/maven/test/get-checksum-both-right/1.0/get-checksum-both-right-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, "066d76e459f7782c312c31e8a11b3c0f1e3e43a7 *get-checksum-both-right-1.0.jar",
                         "e58f30c6a150a2e843552438d18e15cb *get-checksum-both-right-1.0.jar" );
    }

    public void testGetChecksumCorrectSha1NoMd5()
        throws Exception
    {
        String path = "org/apache/maven/test/get-checksum-sha1-only/1.0/get-checksum-sha1-only-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, "748a3a013bf5eacf2bbb40a2ac7d37889b728837 *get-checksum-sha1-only-1.0.jar", null );
    }

    public void testGetChecksumNoSha1CorrectMd5()
        throws Exception
    {
        String path = "org/apache/maven/test/get-checksum-md5-only/1.0/get-checksum-md5-only-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, null, "f3af5201bf8da801da37db8842846e1c *get-checksum-md5-only-1.0.jar" );
    }

    public void testGetWithNoChecksumsUsingIgnoredSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, null, null );
    }

    public void testGetChecksumBadSha1BadMd5IgnoredSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-checksum-both-bad/1.0/get-checksum-both-bad-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, "invalid checksum file", "invalid checksum file" );
    }

    public void testGetChecksumBadSha1BadMd5FailSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-checksum-both-bad/1.0/get-checksum-both-bad-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FAIL, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertNotDownloaded( downloadedFile );
        assertChecksums( expectedFile, null, null );
    }

    public void testGetChecksumBadSha1BadMd5FixSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-checksum-both-bad/1.0/get-checksum-both-bad-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, "4ec20a12dc91557330bd0b39d1805be5e329ae56  get-checksum-both-bad-1.0.jar",
                         "a292491a35925465e693a44809a078b5  get-checksum-both-bad-1.0.jar" );
    }

    public void testGetChecksumCorrectSha1BadMd5UsingFailSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-checksum-sha1-bad-md5/1.0/get-checksum-sha1-bad-md5-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FAIL, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertNotDownloaded( downloadedFile );
        assertChecksums( expectedFile, null, null );
    }

    public void testGetChecksumNoSha1CorrectMd5UsingFailSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-checksum-md5-only/1.0/get-checksum-md5-only-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FAIL, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        // This is a success situation. No SHA1 with a Good MD5.
        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, null, "f3af5201bf8da801da37db8842846e1c *get-checksum-md5-only-1.0.jar" );
    }

    public void testGetWithNoChecksumsUsingFailSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FAIL, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertNotDownloaded( downloadedFile );
        assertChecksums( expectedFile, null, null );
    }

    public void testGetChecksumCorrectSha1BadMd5UsingIgnoredSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-checksum-sha1-bad-md5/1.0/get-checksum-sha1-bad-md5-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, "3dd1a3a57b807d3ef3fbc6013d926c891cbb8670 *get-checksum-sha1-bad-md5-1.0.jar",
                         "invalid checksum file" );
    }

    public void testGetChecksumCorrectSha1BadMd5UsingFixSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-checksum-sha1-bad-md5/1.0/get-checksum-sha1-bad-md5-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, "3dd1a3a57b807d3ef3fbc6013d926c891cbb8670 *get-checksum-sha1-bad-md5-1.0.jar",
                         "c35f3b76268b73a4ba617f6f275c49ab  get-checksum-sha1-bad-md5-1.0.jar" );
    }

    public void testGetChecksumNoSha1CorrectMd5UsingFixSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-checksum-md5-only/1.0/get-checksum-md5-only-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, "71f7dc3f72053a3f2d9fdd6fef9db055ef957ffb  get-checksum-md5-only-1.0.jar",
                         "f3af5201bf8da801da37db8842846e1c *get-checksum-md5-only-1.0.jar" );
    }

    public void testGetWithNoChecksumsUsingFixSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, "1f12821c5e43e1a0b76b9564a6ddb0548ccb9486  get-default-layout-1.0.jar",
                         "3f7341545f21226b6f49a3c2704cb9be  get-default-layout-1.0.jar" );
    }

    public void testGetChecksumTransferFailed()
        throws Exception
    {
        String path = "org/apache/maven/test/get-checksum-sha1-only/1.0/get-checksum-sha1-only-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.getParentFile().exists() );
        assertFalse( expectedFile.exists() );

        saveRepositoryConfig( "badproxied", "Bad Proxied", "test://bad.machine.com/repo/", "default" );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "badproxied", ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        wagonMock.getIfNewer( path, new File( expectedFile.getAbsolutePath() + ".tmp" ), 0 );
        wagonMockControl.setReturnValue( true );
        wagonMock.getIfNewer( path + ".sha1", new File( expectedFile.getAbsolutePath() + ".sha1.tmp" ), 0 );
        wagonMockControl.setReturnValue( true );
        wagonMock.getIfNewer( path + ".md5", new File( expectedFile.getAbsolutePath() + ".md5.tmp" ), 0 );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );
        wagonMockControl.replay();

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        wagonMockControl.verify();

        // Do what the mock doesn't do.
        String proxyPath = new File( REPOPATH_PROXIED1, path ).getAbsolutePath();
        String localPath = new File( managedDefaultDir, path ).getAbsolutePath();
        FileUtils.copyFile( new File( proxyPath ), new File( localPath ) );
        FileUtils.copyFile( new File( proxyPath + ".sha1" ), new File( localPath + ".sha1" ) );

        // Test results.
        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, "748a3a013bf5eacf2bbb40a2ac7d37889b728837 *get-checksum-sha1-only-1.0.jar", null );
    }

    public void testGetAlwaysBadChecksumPresentLocallyAbsentRemoteUsingIgnoredSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-bad-local-checksum/1.0/get-bad-local-checksum-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.IGNORED, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        // There are no hashcodes on the proxy side to download, hence the local ones should remain invalid.
        assertChecksums( expectedFile, "invalid checksum file", "invalid checksum file" );
    }

    public void testGetAlwaysBadChecksumPresentLocallyAbsentRemoteUsingFailSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-bad-local-checksum/1.0/get-bad-local-checksum-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FAIL, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        assertNotDownloaded( downloadedFile );
        assertNoTempFiles( expectedFile );
        // There are no hashcodes on the proxy side to download.
        // The FAIL policy will delete the checksums as bad.
        assertChecksums( expectedFile, null, null );
    }

    public void testGetAlwaysBadChecksumPresentLocallyAbsentRemoteUsingFixSetting()
        throws Exception
    {
        String path = "org/apache/maven/test/get-bad-local-checksum/1.0/get-bad-local-checksum-1.0.jar";
        setupTestableManagedRepository( path );
        
        File expectedFile = new File( managedDefaultDir, path );
        ArtifactReference artifact = createArtifactReference( "default", path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied1File = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxied1File );
        assertNoTempFiles( expectedFile );
        assertChecksums( expectedFile, "96a08dc80a108cba8efd3b20aec91b32a0b2cbd4  get-bad-local-checksum-1.0.jar",
                         "46fdd6ca55bf1d7a7eb0c858f41e0ccd  get-bad-local-checksum-1.0.jar" );
    }
}
