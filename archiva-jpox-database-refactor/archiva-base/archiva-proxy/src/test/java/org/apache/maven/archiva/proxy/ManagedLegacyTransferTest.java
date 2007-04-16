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

        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File proxied2File = new File( REPOPATH_PROXIED2,
                                      "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar" );
        assertFileEquals( expectedFile, downloadedFile, proxied2File );
        assertNoTempFiles( expectedFile );

        // TODO: timestamp preservation requires support for that in wagon
        //    assertEquals( "Check file timestamp", proxiedFile.lastModified(), file.lastModified() );
    }

    public void testLegacyManagedRepoGetAlreadyPresent()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        //        File expectedFile = new File( legacyManagedRepository.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( expectedFile, null );
        //        long originalModificationTime = expectedFile.lastModified();
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, legacyManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
        //        File proxiedFile = new File( proxiedRepository1.getBasedir(),
        //                                     "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar" );
        //        String unexpectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );
        //        assertFalse( "Check file timestamp is not that of proxy", proxiedFile.lastModified() == file.lastModified() );
        //        assertEquals( "Check file timestamp is that of original managed file", originalModificationTime, file
        //            .lastModified() );
    }

    public void testLegacyProxyRepoGetNotPresent()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //
        //        expectedFile.delete();
        //        assertFalse( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, legacyProxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        File proxiedFile = new File( legacyProxiedRepository.getBasedir(),
        //                                     "org.apache.maven.test/jars/get-default-layout-1.0.jar" );
        //        String expectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
        // TODO: timestamp preservation requires support for that in wagon
        //    assertEquals( "Check file timestamp", proxiedFile.lastModified(), file.lastModified() );
    }

    public void testLegacyProxyRepoGetAlreadyPresent()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( expectedFile, null );
        //        long originalModificationTime = expectedFile.lastModified();
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, legacyProxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
        //        File proxiedFile = new File( legacyProxiedRepository.getBasedir(),
        //                                     "org.apache.maven.test/jars/get-default-layout-present-1.0.jar" );
        //        String unexpectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );
        //        assertFalse( "Check file timestamp is not that of proxy", proxiedFile.lastModified() == file.lastModified() );
        //        assertEquals( "Check file timestamp is that of original managed file", originalModificationTime, file
        //            .lastModified() );
    }

    public void testLegacyManagedAndProxyRepoGetNotPresent()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org.apache.maven.test/jars/get-default-layout-1.0.jar";
        //        File expectedFile = new File( legacyManagedRepository.getBasedir(), path );
        //
        //        assertFalse( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, legacyProxiedRepositories, legacyManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        File proxiedFile = new File( legacyProxiedRepository.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
        // TODO: timestamp preservation requires support for that in wagon
        //    assertEquals( "Check file timestamp", proxiedFile.lastModified(), file.lastModified() );
    }

    public void testLegacyManagedAndProxyRepoGetAlreadyPresent()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        //        File expectedFile = new File( legacyManagedRepository.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( expectedFile, null );
        //        long originalModificationTime = expectedFile.lastModified();
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, legacyProxiedRepositories, legacyManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
        //        File proxiedFile = new File( legacyProxiedRepository.getBasedir(), path );
        //        String unexpectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );
        //        assertFalse( "Check file timestamp is not that of proxy", proxiedFile.lastModified() == file.lastModified() );
        //        assertEquals( "Check file timestamp is that of original managed file", originalModificationTime, file
        //            .lastModified() );
    }

    public void testLegacyRequestConvertedToDefaultPathInManagedRepo()
        throws Exception
    {
        fail( "Implemented " + getName() );

        // Check that a Maven1 legacy request is translated to a maven2 path in
        // the managed repository.

        //        String legacyPath = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        //        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( legacyPath, legacyProxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
    }

    public void testDefaultRequestConvertedToLegacyPathInManagedRepo()
        throws Exception
    {
        fail( "Implemented " + getName() );

        // Check that a Maven2 default request is translated to a legacy path in
        // the managed repository.

        //        String legacyPath = "org.apache.maven.test/jars/get-default-layout-present-1.0.jar";
        //        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        //        File expectedFile = new File( legacyManagedRepository.getBasedir(), legacyPath );
        //        assertTrue( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, legacyManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
    }
}
