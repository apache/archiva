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
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.SnapshotVersion;
import org.apache.maven.archiva.policies.CachedFailuresPolicy;
import org.apache.maven.archiva.policies.ChecksumPolicy;
import org.apache.maven.archiva.policies.ReleasesPolicy;
import org.apache.maven.archiva.policies.SnapshotsPolicy;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataWriter;

import java.io.File;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * MetadataTransferTest 
 *
 * @author Brett Porter
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MetadataTransferTest
    extends AbstractProxyTestCase
{
    public void testGetMetadataNotPresent()
        throws Exception
    {
        String path = "org/apache/maven/test/dummy-artifact/1.0/maven-metadata.xml";
        File expectedFile = new File( managedDefaultDir, path );
        ProjectReference metadata = createMetadataReference( "default", path );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, metadata );

        assertNotDownloaded( downloadedFile );
        assertNoTempFiles( expectedFile );
    }

    private String getExpectedMetadata( String artifactId, String version )
        throws RepositoryMetadataException
    {
        return getExpectedMetadata( artifactId, version, (SnapshotVersion) null, null );
    }

    private static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone( "UTC" );

    private static String getLastUpdatedTimestamp( File file )
    {
        DateFormat fmt = new SimpleDateFormat( "yyyyMMddHHmmss", Locale.US );
        fmt.setTimeZone( UTC_TIMEZONE );
        return fmt.format( new Date( file.lastModified() ) );
    }

    private String getExpectedMetadata( String artifactId, String[] availableVersions, File file )
        throws RepositoryMetadataException
    {
        return getExpectedMetadata( artifactId, null, availableVersions, file );
    }

    private SnapshotVersion getSnapshotVersion( String timestamp, int buildNumber )
    {
        SnapshotVersion snapshot = new SnapshotVersion();

        snapshot.setTimestamp( timestamp );
        snapshot.setBuildNumber( buildNumber );

        return snapshot;
    }

    private String getExpectedMetadata( String artifactId, String version, SnapshotVersion snapshot, File file )
        throws RepositoryMetadataException
    {
        StringWriter expectedContents = new StringWriter();

        ArchivaRepositoryMetadata m = new ArchivaRepositoryMetadata();
        m.setGroupId( "org.apache.maven.test" );
        m.setArtifactId( artifactId );
        m.setVersion( version );
        m.setSnapshotVersion( snapshot );
        if ( file != null )
        {
            m.setLastUpdated( getLastUpdatedTimestamp( file ) );
        }
        m.setModelEncoding( null );

        RepositoryMetadataWriter.write( m, expectedContents );
        return expectedContents.toString();
    }

    private String getExpectedMetadata( String artifactId, String version, String[] availableVersions, File file )
        throws RepositoryMetadataException
    {
        StringWriter expectedContents = new StringWriter();

        ArchivaRepositoryMetadata m = new ArchivaRepositoryMetadata();
        m.setGroupId( "org.apache.maven.test" );
        m.setArtifactId( artifactId );
        m.setVersion( version );
        if ( file != null )
        {
            m.setLastUpdated( getLastUpdatedTimestamp( file ) );
        }
        if ( availableVersions != null )
        {
            m.getAvailableVersions().addAll( Arrays.asList( availableVersions ) );
        }
        m.setModelEncoding( null );

        RepositoryMetadataWriter.write( m, expectedContents );
        return expectedContents.toString();
    }

    private void assertMetadataEquals( File expectedFile, File downloadedFile, String expectedMetadata )
        throws Exception
    {
        assertNotNull( "Expected File should not be null.", expectedFile );
        assertNotNull( "Downloaded File should not be null.", downloadedFile );

        assertTrue( "Check downloaded file exists.", downloadedFile.exists() );
        assertEquals( "Check file path matches.", expectedFile.getAbsolutePath(), downloadedFile.getAbsolutePath() );

        StringWriter actualContents = new StringWriter();
        RepositoryMetadataReader metadataReader = new RepositoryMetadataReader();
        ArchivaRepositoryMetadata metadata = metadataReader.read( downloadedFile );
        RepositoryMetadataWriter.write( metadata, actualContents );
        assertEquals( "Check file contents.", expectedMetadata, actualContents );
    }

    public void testGetMetadataProxied()
        throws Exception
    {
        String path = "org/apache/maven/test/get-default-metadata/1.0/maven-metadata.xml";
        File expectedFile = new File( managedDefaultDir, path );
        ProjectReference metadata = createMetadataReference( "default", path );

        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        assertFalse( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, metadata );

        String expectedMetadata = getExpectedMetadata( "get-default-metadata", "1.0" );
        assertMetadataEquals( expectedFile, downloadedFile, expectedMetadata );
        assertNoTempFiles( expectedFile );
    }

    public void testGetMetadataMergeRepos()
        throws Exception
    {
        String path = "org/apache/maven/test/get-merged-metadata/maven-metadata.xml";
        File expectedFile = new File( managedDefaultDir, path );
        ProjectReference metadata = createMetadataReference( "default", path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, ID_PROXIED1, ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, metadata );

        String expectedMetadata = getExpectedMetadata( "get-merged-metadata", new String[] {
            "0.9",
            "1.0",
            "2.0",
            "3.0",
            "5.0",
            "4.0" }, downloadedFile );
        assertMetadataEquals( expectedFile, downloadedFile, expectedMetadata );
        assertNoTempFiles( expectedFile );
    }

    public void testGetMetadataRemovedFromProxies()
        throws Exception
    {
        String path = "org/apache/maven/test/get-removed-metadata/1.0/maven-metadata.xml";
        File expectedFile = new File( managedDefaultDir, path );
        ProjectReference metadata = createMetadataReference( "default", path );

        assertTrue( expectedFile.exists() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, metadata );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testGetReleaseMetadataNotExpired()
        throws Exception
    {
        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        File expectedFile = new File( managedDefaultDir, path );
        ProjectReference metadata = createMetadataReference( "default", path );

        assertTrue( expectedFile.exists() );

        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( getPastDate().getTime() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, metadata );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        assertFileEquals( expectedFile, downloadedFile, proxiedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testGetSnapshotMetadataNotExpired()
        throws Exception
    {
        String path = "org/apache/maven/test/get-updated-metadata/1.0-SNAPSHOT/maven-metadata.xml";
        File expectedFile = new File( managedDefaultDir, path );
        ProjectReference metadata = createMetadataReference( "default", path );

        assertTrue( expectedFile.exists() );

        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( getPastDate().getTime() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, metadata );

        // Content should NOT match that from proxied 1. 
        assertFileEquals( expectedFile, downloadedFile, expectedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testGetReleaseMetadataExpired()
        throws Exception
    {
        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        File expectedFile = new File( managedDefaultDir, path );
        ProjectReference metadata = createMetadataReference( "default", path );

        assertTrue( expectedFile.exists() );

        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( getPastDate().getTime() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, metadata );

        String expectedMetadata = getExpectedMetadata( "get-updated-metadata", new String[] { "1.0", "2.0", },
                                                       downloadedFile );
        assertMetadataEquals( expectedFile, downloadedFile, expectedMetadata );
        assertNoTempFiles( expectedFile );
    }

    public void testGetSnapshotMetadataExpired()
        throws Exception
    {
        String path = "org/apache/maven/test/get-updated-metadata/1.0-SNAPSHOT/maven-metadata.xml";
        File expectedFile = new File( managedDefaultDir, path );
        ProjectReference metadata = createMetadataReference( "default", path );

        assertTrue( expectedFile.exists() );

        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( getPastDate().getTime() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, metadata );

        String expectedMetadata = getExpectedMetadata( "get-updated-metadata", "1.0-SNAPSHOT",
                                                       getSnapshotVersion( "20050831.111213", 2 ), downloadedFile );
        assertMetadataEquals( expectedFile, downloadedFile, expectedMetadata );
        assertNoTempFiles( expectedFile );
    }

    public void testGetMetadataNotUpdated()
        throws Exception
    {
        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        File expectedFile = new File( managedDefaultDir, path );
        ProjectReference metadata = createMetadataReference( "default", path );

        assertTrue( expectedFile.exists() );

        File proxiedFile = new File( REPOPATH_PROXIED1, path );
        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( proxiedFile.lastModified() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, metadata );

        assertFileEquals( expectedFile, downloadedFile, expectedFile );
        assertNoTempFiles( expectedFile );
    }

    public void testGetMetadataUpdated()
        throws Exception
    {
        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        File expectedFile = new File( managedDefaultDir, path );
        ProjectReference metadata = createMetadataReference( "default", path );

        assertTrue( expectedFile.exists() );

        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( getPastDate().getTime() );

        // Configure Connector (usually done within archiva.xml configuration)
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, metadata );

        String expectedMetadata = getExpectedMetadata( "get-updated-metadata", new String[] { "1.0", "2.0" },
                                                       downloadedFile );

        assertMetadataEquals( expectedFile, downloadedFile, expectedMetadata );
        assertNoTempFiles( expectedFile );
    }
}
