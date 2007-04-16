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

import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.policies.CachedFailuresPolicy;
import org.apache.maven.archiva.policies.ChecksumPolicy;
import org.apache.maven.archiva.policies.ReleasesPolicy;
import org.apache.maven.archiva.policies.SnapshotsPolicy;

import java.io.File;

/**
 * MetadataTransferTest 
 *
 * @author Brett Porter
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
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
        saveConnector( ID_DEFAULT_MANAGED, "proxied1", ChecksumPolicy.FIX, ReleasesPolicy.IGNORED,
                       SnapshotsPolicy.IGNORED, CachedFailuresPolicy.IGNORED );

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, metadata );

        assertNotDownloaded( downloadedFile );

        //        String path = "org/apache/maven/test/dummy-artifact/1.0/maven-metadata.xml";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //
        //        assertFalse( expectedFile.exists() );
        //
        //        try
        //        {
        //            File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //            fail( "Found file: " + file + "; but was expecting a failure" );
        //        }
        //        catch ( ResourceDoesNotExistException e )
        //        {
        //            // expected
        //
        //            assertFalse( expectedFile.exists() );
        //        }
    }

    public void testGetMetadataProxied()
    {
        String path = "org/apache/maven/test/get-default-metadata/1.0/maven-metadata.xml";
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-default-metadata/1.0/maven-metadata.xml";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //
        //        FileUtils.deleteDirectory( expectedFile.getParentFile() );
        //        assertFalse( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        String expectedContents = getExpectedMetadata( "get-default-metadata", "1.0" );
        //        assertEquals( "Check content matches", expectedContents, FileUtils.readFileToString( file, null ) );        
    }

    public void testGetMetadataMergeRepos()
    {
        String path = "org/apache/maven/test/get-merged-metadata/maven-metadata.xml";
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-merged-metadata/maven-metadata.xml";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //
        //        String expectedContents = getExpectedMetadata( "get-merged-metadata", getVersioning(
        //            Arrays.asList( new String[]{"0.9", "1.0", "2.0", "3.0", "5.0", "4.0"} ), file ) );
        //
        //        assertEquals( "Check content matches", expectedContents, FileUtils.readFileToString( file, null ) );
    }

    public void testGetMetadataRemovedFromProxies()
    {
        String path = "org/apache/maven/test/get-removed-metadata/1.0/maven-metadata.xml";
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-removed-metadata/1.0/maven-metadata.xml";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //        String expectedContents =
        //            FileUtils.readFileToString( new File( defaultManagedRepository.getBasedir(), path ), null );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        assertEquals( "Check content matches", expectedContents, FileUtils.readFileToString( file, null ) );        
    }

    public void testGetReleaseMetadataNotExpired()
    {
        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //        String expectedContents =
        //            FileUtils.readFileToString( new File( defaultManagedRepository.getBasedir(), path ), null );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( getPastDate().getTime() );
        //
        //        proxiedRepository1.getReleases().setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER );
        //        proxiedRepository1.getSnapshots().setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        assertEquals( "Check content matches", expectedContents, FileUtils.readFileToString( file, null ) );
        //
        //        String unexpectedContents =
        //            FileUtils.readFileToString( new File( proxiedRepository1.getBasedir(), path ), null );
        //        assertFalse( "Check content doesn't match proxy version",
        //                     unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );

    }

    public void testGetSnapshotMetadataNotExpired()
    {
        String path = "org/apache/maven/test/get-updated-metadata/1.0-SNAPSHOT/maven-metadata.xml";
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-updated-metadata/1.0-SNAPSHOT/maven-metadata.xml";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //        String expectedContents =
        //            FileUtils.readFileToString( new File( defaultManagedRepository.getBasedir(), path ), null );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( getPastDate().getTime() );
        //
        //        proxiedRepository1.getReleases().setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        //        proxiedRepository1.getSnapshots().setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER );
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        assertEquals( "Check content matches", expectedContents, FileUtils.readFileToString( file, null ) );
        //
        //        String unexpectedContents =
        //            FileUtils.readFileToString( new File( proxiedRepository1.getBasedir(), path ), null );
        //        assertFalse( "Check content doesn't match proxy version",
        //                     unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );        
    }

    public void testGetReleaseMetadataExpired()
    {
        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //        String unexpectedContents =
        //            FileUtils.readFileToString( new File( defaultManagedRepository.getBasedir(), path ), null );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( getPastDate().getTime() );
        //
        //        proxiedRepository1.getReleases().setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        //        proxiedRepository1.getSnapshots().setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER );
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //
        //        String expectedContents = getExpectedMetadata( "get-updated-metadata", getVersioning(
        //            Arrays.asList( new String[]{"1.0", "2.0"} ), file ) );
        //
        //        assertEquals( "Check content matches", expectedContents, FileUtils.readFileToString( file, null ) );
        //        assertFalse( "Check content doesn't match proxy version",
        //                     unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );

    }

    public void testGetSnapshotMetadataExpired()
    {
        String path = "org/apache/maven/test/get-updated-metadata/1.0-SNAPSHOT/maven-metadata.xml";
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-updated-metadata/1.0-SNAPSHOT/maven-metadata.xml";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //        String unexpectedContents =
        //            FileUtils.readFileToString( new File( defaultManagedRepository.getBasedir(), path ), null );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( getPastDate().getTime() );
        //
        //        proxiedRepository1.getReleases().setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER );
        //        proxiedRepository1.getSnapshots().setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //
        //        String expectedContents =
        //            getExpectedMetadata( "get-updated-metadata", "1.0-SNAPSHOT", getVersioning( "20050831.111213", 2, file ) );
        //
        //        assertEquals( "Check content matches", expectedContents, FileUtils.readFileToString( file, null ) );
        //        assertFalse( "Check content doesn't match proxy version",
        //                     unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );

    }

    public void testGetMetadataNotUpdated()
    {
        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //        String expectedContents =
        //            FileUtils.readFileToString( new File( defaultManagedRepository.getBasedir(), path ), null );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        //        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( proxiedFile.lastModified() );
        //
        //        proxiedRepository1.getReleases().setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        assertEquals( "Check content matches", expectedContents, FileUtils.readFileToString( file, null ) );
        //
        //        String unexpectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertFalse( "Check content doesn't match proxy version",
        //                     unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );        
    }

    public void testGetMetadataUpdated()
    {
        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //        String unexpectedContents =
        //            FileUtils.readFileToString( new File( defaultManagedRepository.getBasedir(), path ), null );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( getPastDate().getTime() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //
        //        String expectedContents = getExpectedMetadata( "get-updated-metadata", getVersioning(
        //            Arrays.asList( new String[]{"1.0", "2.0"} ), file ) );
        //        assertEquals( "Check content matches", expectedContents, FileUtils.readFileToString( file, null ) );
        //        assertFalse( "Check content doesn't match old version",
        //                     unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );    
    }
}
