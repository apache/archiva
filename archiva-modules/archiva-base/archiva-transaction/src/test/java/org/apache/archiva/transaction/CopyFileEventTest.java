package org.apache.archiva.transaction;

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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 */
public class CopyFileEventTest
    extends AbstractFileEventTest
{
    private Path testDir = Paths.get(org.apache.archiva.common.utils.FileUtils.getBasedir(), "target/transaction-tests/copy-file");

    private Path testDest = testDir.resolve( "test-file.txt" );

    private Path testSource = Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "target/transaction-tests/test-file.txt" );

    private Path testDestChecksum;

    private String source, oldChecksum;

    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        Files.createDirectories(testSource.getParent());

        Files.createFile(testSource);

        writeFile( testSource, "source contents" );

        testDestChecksum = Paths.get( testDest.toAbsolutePath() + ".sha1" );

        Files.createDirectories(testDestChecksum.getParent());

        Files.createFile(testDestChecksum);

        writeFile( testDestChecksum, "this is the checksum" );

        assertTrue( "Test if the source exists", Files.exists(testSource) );

        assertTrue( "Test if the destination checksum exists", Files.exists(testDestChecksum) );

        source = readFile( testSource );

        oldChecksum = readFile( testDestChecksum );
    }
    
    @Test
    public void testCopyCommitRollback()
        throws Exception
    {
        CopyFileEvent event = new CopyFileEvent( testSource, testDest, checksumAlgorithms );

        assertFalse( "Test that the destination is not yet created", Files.exists(testDest) );

        event.commit();

        assertTrue( "Test that the destination is created", Files.exists(testDest) );

        assertChecksumCommit( testDest );

        String target = readFile( testDest );

        assertTrue( "Test that the destination contents are copied correctly", source.equals( target ) );

        event.rollback();

        assertFalse( "Test that the destination file has been deleted", Files.exists(testDest) );

        assertChecksumRollback( testDest );
    }

    @Test
    public void testCopyCommitRollbackWithBackup()
        throws Exception
    {
        Files.createDirectories(testDest.getParent());

        Files.createFile(testDest);

        writeFile( testDest, "overwritten contents" );

        assertTrue( "Test that the destination exists", Files.exists(testDest) );

        CopyFileEvent event = new CopyFileEvent( testSource, testDest, checksumAlgorithms );

        String target = readFile( testDest );

        assertTrue( "Test that the destination contents have not changed", target.equals( "overwritten contents" ) );

        event.commit();

        target = readFile( testDest );

        assertTrue( "Test that the destination contents are copied correctly", source.equals( target ) );

        assertChecksumCommit( testDest );

        event.rollback();

        target = readFile( testDest );

        assertTrue( "Test the destination file contents have been restored", target.equals( "overwritten contents" ) );

        assertChecksumRollback( testDest );
    }

    @Test
    public void testCreateRollbackCommit()
        throws Exception
    {
        CopyFileEvent event = new CopyFileEvent( testSource, testDest, checksumAlgorithms );

        assertFalse( "Test that the destination is not yet created", Files.exists(testDest) );

        event.rollback();

        assertFalse( "Test that the destination file is not yet created", Files.exists(testDest) );

        event.commit();

        assertTrue( "Test that the destination is created", Files.exists(testDest) );

        assertChecksumCommit( testDest );

        String target = readFile( testDest );

        assertTrue( "Test that the destination contents are copied correctly", source.equals( target ) );
    }

    @After    
    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();

        org.apache.archiva.common.utils.FileUtils.deleteDirectory( Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "target/transaction-tests" ) );
    }

    @Override
    protected void assertChecksumCommit( Path  file )
        throws IOException
    {
        super.assertChecksumCommit( file );

        String target = readFile( testDestChecksum );

        assertFalse( "Test that the destination checksum contents are created correctly", oldChecksum.equals( target ) );
    }

    @Override
    protected void assertChecksumRollback( Path file )
        throws IOException
    {
        assertChecksumDoesNotExist( file, "md5" );
        assertChecksumExists( file, "sha1" );

        String target = readFile( testDestChecksum );

        assertEquals( "Test that the destination checksum contents are reverted correctly", oldChecksum, target );
    }
}
