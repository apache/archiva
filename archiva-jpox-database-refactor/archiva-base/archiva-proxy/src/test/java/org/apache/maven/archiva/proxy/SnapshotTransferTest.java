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

import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.IOException;
import java.text.ParseException;

/**
 * SnapshotTransferTest 
 *
 * @author Brett Porter
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class SnapshotTransferTest
    extends AbstractProxyTestCase
{
    public void testSnapshotNonExistant()
    {
        String path = "org/apache/maven/test/does-not-exist/1.0-SNAPSHOT/does-not-exist-1.0-SNAPSHOT.jar";
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/does-not-exist/1.0-SNAPSHOT/does-not-exist-1.0-SNAPSHOT.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //
        //        assertFalse( expectedFile.exists() );
        //
        //        try
        //        {
        //            File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //            fail( "File returned was: " + file + "; should have got a not found exception" );
        //        }
        //        catch ( ResourceDoesNotExistException e )
        //        {
        //            // expected, but check file was not created
        //            assertFalse( expectedFile.exists() );
        //        }        
    }

    public void testTimestampDrivenSnapshotNotPresentAlready()
    {
        String path = "org/apache/maven/test/does-not-exist/1.0-SNAPSHOT/does-not-exist-1.0-SNAPSHOT.jar";
        fail( "Implemented " + getName() );

        //        String path =
        //            "org/apache/maven/test/get-timestamped-snapshot/1.0-SNAPSHOT/get-timestamped-snapshot-1.0-SNAPSHOT.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //
        //        expectedFile.delete();
        //        assertFalse( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
    }

    public void testNewerTimestampDrivenSnapshotOnFirstRepo()
        throws ResourceDoesNotExistException, ProxyException, IOException, ParseException
    {
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-present-timestamped-snapshot/1.0-SNAPSHOT/get-present-timestamped-snapshot-1.0-SNAPSHOT.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        expectedFile.setLastModified( getPastDate().getTime() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
    }

    public void testOlderTimestampDrivenSnapshotOnFirstRepo()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-present-timestamped-snapshot/1.0-SNAPSHOT/get-present-timestamped-snapshot-1.0-SNAPSHOT.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( expectedFile, null );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        expectedFile.setLastModified( getFutureDate().getTime() );
        //
        //        proxiedRepository1.getSnapshots().setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
        //
        //        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        //        String unexpectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );
    }

    public void testNewerTimestampDrivenSnapshotOnSecondRepoThanFirstNotPresentAlready()
        throws Exception
    {
        // TODO: wagon may not support timestamps (yet)
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-timestamped-snapshot-in-both/1.0-SNAPSHOT/get-timestamped-snapshot-in-both-1.0-SNAPSHOT.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //
        //        assertFalse( expectedFile.exists() );
        //
        //        File repoLocation = getTestFile( "target/test-repository/proxied1" );
        //        FileUtils.deleteDirectory( repoLocation );
        //        copyDirectoryStructure( getTestFile( "src/test/repositories/proxied1" ), repoLocation );
        //        proxiedRepository1 = createRepository( "proxied1", repoLocation );
        //
        //        new File( proxiedRepository1.getBasedir(), path ).setLastModified( getPastDate().getTime() );
        //
        //        proxiedRepositories.clear();
        //        proxiedRepositories.add( createProxiedRepository( proxiedRepository1 ) );
        //        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //
        //        File proxiedFile = new File( proxiedRepository2.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
        //
        //        proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        //        String unexpectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );
    }

    public void testOlderTimestampDrivenSnapshotOnSecondRepoThanFirstNotPresentAlready()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-timestamped-snapshot-in-both/1.0-SNAPSHOT/get-timestamped-snapshot-in-both-1.0-SNAPSHOT.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //
        //        expectedFile.delete();
        //        assertFalse( expectedFile.exists() );
        //
        //        File repoLocation = getTestFile( "target/test-repository/proxied2" );
        //        FileUtils.deleteDirectory( repoLocation );
        //        copyDirectoryStructure( getTestFile( "src/test/repositories/proxied2" ), repoLocation );
        //        proxiedRepository2 = createRepository( "proxied2", repoLocation );
        //
        //        new File( proxiedRepository2.getBasedir(), path ).setLastModified( getPastDate().getTime() );
        //
        //        proxiedRepositories.clear();
        //        proxiedRepositories.add( createProxiedRepository( proxiedRepository1 ) );
        //        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //
        //        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
        //
        //        proxiedFile = new File( proxiedRepository2.getBasedir(), path );
        //        String unexpectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );
    }

    public void testTimestampDrivenSnapshotNotExpired()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-present-timestamped-snapshot/1.0-SNAPSHOT/get-present-timestamped-snapshot-1.0-SNAPSHOT.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        //        proxiedFile.setLastModified( getFutureDate().getTime() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        String expectedContents = FileUtils.readFileToString( expectedFile, null );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
        //
        //        String unexpectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );
    }

    public void testTimestampDrivenSnapshotNotUpdated()
        throws Exception
    {
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-present-timestamped-snapshot/1.0-SNAPSHOT/get-present-timestamped-snapshot-1.0-SNAPSHOT.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( expectedFile, null );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        //        expectedFile.setLastModified( proxiedFile.lastModified() );
        //
        //        proxiedRepository1.getSnapshots().setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
        //
        //        String unexpectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );
    }

    public void testTimestampDrivenSnapshotNotPresentAlreadyExpiredCacheFailure()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-timestamped-snapshot/1.0-SNAPSHOT/get-timestamped-snapshot-1.0-SNAPSHOT.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //
        //        expectedFile.delete();
        //        assertFalse( expectedFile.exists() );
        //
        //        proxiedRepositories.clear();
        //        ProxiedArtifactRepository proxiedArtifactRepository = createProxiedRepository( proxiedRepository1 );
        //        proxiedArtifactRepository.addFailure( path, ALWAYS_UPDATE_POLICY );
        //        proxiedRepositories.add( proxiedArtifactRepository );
        //        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //
        //        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
        //
        //        assertFalse( "Check failure", proxiedArtifactRepository.isCachedFailure( path ) );
    }

    public void testMetadataDrivenSnapshotNotPresentAlready()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        fail( "Implemented " + getName() );

        //        String path = "org/apache/maven/test/get-metadata-snapshot/1.0-SNAPSHOT/get-metadata-snapshot-1.0-20050831.101112-1.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //
        //        expectedFile.delete();
        //        assertFalse( expectedFile.exists() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
    }

    public void testGetMetadataDrivenSnapshotRemoteUpdate()
        throws ResourceDoesNotExistException, ProxyException, IOException, ParseException
    {
        fail( "Implemented " + getName() );

        // Metadata driven snapshots (using a full timestamp) are treated like a release. It is the timing of the
        // updates to the metadata files that triggers which will be downloaded

        //        String path = "org/apache/maven/test/get-present-metadata-snapshot/1.0-SNAPSHOT/get-present-metadata-snapshot-1.0-20050831.101112-1.jar";
        //        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        //        String expectedContents = FileUtils.readFileToString( expectedFile, null );
        //
        //        assertTrue( expectedFile.exists() );
        //
        //        expectedFile.setLastModified( getPastDate().getTime() );
        //
        //        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        //
        //        assertEquals( "Check file matches", expectedFile, file );
        //        assertTrue( "Check file created", file.exists() );
        //        assertEquals( "Check file contents", expectedContents, FileUtils.readFileToString( file, null ) );
        //        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        //        String unexpectedContents = FileUtils.readFileToString( proxiedFile, null );
        //        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.readFileToString( file, null ) ) );
    }
}
