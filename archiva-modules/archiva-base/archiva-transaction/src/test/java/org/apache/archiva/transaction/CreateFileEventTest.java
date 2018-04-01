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
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 */
public class CreateFileEventTest
    extends AbstractFileEventTest
{
    private Path testDir = Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "target/transaction-tests/create-file" );

    @Test
    public void testCreateCommitRollback()
        throws Exception
    {
        Path testFile = testDir.resolve("test-file.txt" );

        CreateFileEvent event = new CreateFileEvent( "file contents", testFile, checksumAlgorithms );

        assertFalse( "Test file is not yet created", Files.exists(testFile) );

        event.commit();

        assertTrue( "Test file has been created", Files.exists(testFile) );

        assertChecksumCommit( testFile );

        event.rollback();

        assertFalse( "Test file is has been deleted after rollback", Files.exists(testFile) );

        assertChecksumRollback( testFile );

        assertFalse( "Test file parent directories has been rolledback too", Files.exists(testDir) );
        assertTrue( "target directory still exists", Files.exists(Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "target" )) );
    }

    @Test
    public void testCreateCommitRollbackWithBackup()
        throws Exception
    {
        Path testFile = testDir.resolve( "test-file.txt" );

        Files.createDirectories(testFile.getParent());

        Files.createFile(testFile);

        writeFile( testFile, "original contents" );

        CreateFileEvent event = new CreateFileEvent( "modified contents", testFile, checksumAlgorithms );

        String contents = readFile( testFile );

        assertEquals( "Test contents have not changed", "original contents", contents );

        event.commit();

        contents = readFile( testFile );

        assertEquals( "Test contents have not changed", "modified contents", contents );

        assertChecksumCommit( testFile );

        event.rollback();

        contents = readFile( testFile );

        assertEquals( "Test contents have not changed", "original contents", contents );

        assertChecksumRollback( testFile );
    }

    @Test
    public void testCreateRollbackCommit()
        throws Exception
    {
        Path testFile = testDir.resolve( "test-file.txt" );

        CreateFileEvent event = new CreateFileEvent( "file contents", testFile, checksumAlgorithms );

        assertFalse( "Test file is not yet created", Files.exists(testFile) );

        event.rollback();

        assertFalse( "Test file is not yet created", Files.exists(testFile) );

        event.commit();

        assertTrue( "Test file is not yet created", Files.exists(testFile) );

        assertChecksumCommit( testFile );
    }

    @Override
    @After
    public void tearDown()
        throws Exception
    {
        super.tearDown();

        org.apache.archiva.common.utils.FileUtils.deleteDirectory( Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "target/transaction-tests" ) );
    }
}
