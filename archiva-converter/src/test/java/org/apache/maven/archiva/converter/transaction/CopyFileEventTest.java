package org.apache.maven.archiva.converter.transaction;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author Edwin Punzalan
 */
public class CopyFileEventTest
    extends AbstractFileEventTest
{
    private File testDir = new File( PlexusTestCase.getBasedir(), "target/transaction-tests/copy-file" );

    private File testDest = new File( testDir, "test-file.txt" );

    private File testSource = new File( PlexusTestCase.getBasedir(), "target/transaction-tests/test-file.txt" );

    private File testDestChecksum;

    private String source, oldChecksum;

    public void setUp()
        throws Exception
    {
        super.setUp();

        testSource.getParentFile().mkdirs();

        testSource.createNewFile();

        FileUtils.writeStringToFile( testSource, "source contents", null );

        testDestChecksum = new File( testDest.getPath() + ".sha1" );

        testDestChecksum.getParentFile().mkdirs();

        testDestChecksum.createNewFile();

        FileUtils.writeStringToFile( testDestChecksum, "this is the checksum", null );

        assertTrue( "Test if the source exists", testSource.exists() );

        assertTrue( "Test if the destination checksum exists", testDestChecksum.exists() );

        source = FileUtils.readFileToString( testSource, null );

        oldChecksum = FileUtils.readFileToString( testDestChecksum, null );
    }

    public void testCopyCommitRollback()
        throws Exception
    {
        CopyFileEvent event = new CopyFileEvent( testSource, testDest, digesters );

        assertFalse( "Test that the destination is not yet created", testDest.exists() );

        event.commit();

        assertTrue( "Test that the destination is created", testDest.exists() );

        assertChecksumCommit( testDest );

        String target = FileUtils.readFileToString( testDest, null );

        assertTrue( "Test that the destination contents are copied correctly", source.equals( target ) );

        event.rollback();

        assertFalse( "Test that the destination file has been deleted", testDest.exists() );

        assertChecksumRollback( testDest );
    }

    public void testCopyCommitRollbackWithBackup()
        throws Exception
    {
        testDest.getParentFile().mkdirs();

        testDest.createNewFile();

        FileUtils.writeStringToFile( testDest, "overwritten contents", null );

        assertTrue( "Test that the destination exists", testDest.exists() );

        CopyFileEvent event = new CopyFileEvent( testSource, testDest, digesters );

        String target = FileUtils.readFileToString( testDest, null );

        assertTrue( "Test that the destination contents have not changed", target.equals( "overwritten contents" ) );

        event.commit();

        target = FileUtils.readFileToString( testDest, null );

        assertTrue( "Test that the destination contents are copied correctly", source.equals( target ) );

        assertChecksumCommit( testDest );

        event.rollback();

        target = FileUtils.readFileToString( testDest, null );

        assertTrue( "Test the destination file contents have been restored", target.equals( "overwritten contents" ) );

        assertChecksumRollback( testDest );
    }

    public void testCreateRollbackCommit()
        throws Exception
    {
        CopyFileEvent event = new CopyFileEvent( testSource, testDest, digesters );

        assertFalse( "Test that the destination is not yet created", testDest.exists() );

        event.rollback();

        assertFalse( "Test that the destination file is not yet created", testDest.exists() );

        event.commit();

        assertTrue( "Test that the destination is created", testDest.exists() );

        assertChecksumCommit( testDest );

        String target = FileUtils.readFileToString( testDest, null );

        assertTrue( "Test that the destination contents are copied correctly", source.equals( target ) );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        FileUtils.deleteDirectory( new File( PlexusTestCase.getBasedir(), "target/transaction-tests" ) );
    }

    protected void assertChecksumCommit( File file )
        throws IOException
    {
        super.assertChecksumCommit( file );

        String target = FileUtils.readFileToString( testDestChecksum, null );

        assertFalse( "Test that the destination checksum contents are created correctly", oldChecksum.equals( target ) );
    }

    protected void assertChecksumRollback( File file )
        throws IOException
    {
        assertChecksumDoesNotExist( file, "md5" );
        assertChecksumExists( file, "sha1" );

        String target = FileUtils.readFileToString( testDestChecksum, null );

        assertEquals( "Test that the destination checksum contents are reverted correctly", oldChecksum, target );
    }
}
