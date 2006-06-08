package org.apache.maven.repository.converter.transaction;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class CreateFileEventTest
    extends PlexusTestCase
{
    private File testDir = new File( PlexusTestCase.getBasedir(), "target/transaction-tests/create-file" );

    public void testCreateCommitRollback()
        throws Exception
    {
        File testFile = new File( testDir, "test-file.txt" );

        CreateFileEvent event = new CreateFileEvent( "file contents", testFile );

        assertFalse( "Test file is not yet created", testFile.exists() );

        event.commit();

        assertTrue( "Test file is not yet created", testFile.exists() );

        event.rollback();

        assertFalse( "Test file is has been deleted after rollback", testFile.exists() );
        assertFalse( "Test file parent directories has been rolledback too", testDir.exists() );
        assertTrue( "target directory still exists", new File( PlexusTestCase.getBasedir(), "target" ).exists() );
    }

    public void testCreateCommitRollbackWithBackup()
        throws Exception
    {
        File testFile = new File( testDir, "test-file.txt" );

        testFile.createNewFile();

        FileUtils.fileWrite( testFile.getAbsolutePath(), "original contents" );

        CreateFileEvent event = new CreateFileEvent( "modified contents", testFile );

        String contents = FileUtils.fileRead( testFile.getAbsolutePath() );

        assertEquals( "Test contents have not changed", "original contents", contents );

        event.commit();

        assertTrue( "Test file is not yet created", testFile.exists() );

        event.rollback();

        assertFalse( "Test file is has been deleted after rollback", testFile.exists() );
        assertFalse( "Test file parent directories has been rolledback too", testDir.exists() );
        assertTrue( "target directory still exists", new File( PlexusTestCase.getBasedir(), "target" ).exists() );
    }

    public void testCreateRollbackCommit()
        throws Exception
    {
        File testFile = new File( testDir, "test-file.txt" );

        CreateFileEvent event = new CreateFileEvent( "file contents", testFile );

        assertFalse( "Test file is not yet created", testFile.exists() );

        event.rollback();

        assertFalse( "Test file is not yet created", testFile.exists() );

        event.commit();

        assertTrue( "Test file is not yet created", testFile.exists() );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        FileUtils.deleteDirectory( new File( testDir, "target/transaction-tests" ).getAbsolutePath() );
    }
}
