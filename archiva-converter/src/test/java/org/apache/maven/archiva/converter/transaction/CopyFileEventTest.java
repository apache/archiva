package org.apache.maven.archiva.converter.transaction;

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
public class CopyFileEventTest
    extends PlexusTestCase
{
    private File testDir = new File( PlexusTestCase.getBasedir(), "target/transaction-tests/copy-file" );

    private File testDest = new File( testDir, "test-file.txt" );

    private File testSource = new File( PlexusTestCase.getBasedir(), "target/transaction-tests/test-file.txt" );

    public void setUp()
        throws Exception
    {
        super.setUp();

        testSource.getParentFile().mkdirs();

        testSource.createNewFile();

        FileUtils.fileWrite( testSource.getAbsolutePath(), "source contents" );
    }

    public void testCopyCommitRollback()
        throws Exception
    {
        assertTrue( "Test if the source exists", testSource.exists() );

        String source = FileUtils.fileRead( testSource.getAbsolutePath() );

        CopyFileEvent event = new CopyFileEvent( testSource, testDest );

        assertFalse( "Test that the destination is not yet created", testDest.exists() );

        event.commit();

        assertTrue( "Test that the destination is created", testDest.exists() );

        String target = FileUtils.fileRead( testDest.getAbsolutePath() );

        assertTrue( "Test that the destination contents are copied correctly", source.equals( target ) );

        event.rollback();

        assertFalse( "Test that the destination file has been deleted", testDest.exists() );
    }

    public void testCopyCommitRollbackWithBackup()
        throws Exception
    {
        assertTrue( "Test if the source exists", testSource.exists() );

        String source = FileUtils.fileRead( testSource.getAbsolutePath() );

        testDest.getParentFile().mkdirs();

        testDest.createNewFile();

        FileUtils.fileWrite( testDest.getAbsolutePath(), "overwritten contents" );

        assertTrue( "Test that the destination exists", testDest.exists() );

        CopyFileEvent event = new CopyFileEvent( testSource, testDest );

        String target = FileUtils.fileRead( testDest.getAbsolutePath() );

        assertTrue( "Test that the destination contents have not changed", target.equals( "overwritten contents" ) );

        event.commit();

        target = FileUtils.fileRead( testDest.getAbsolutePath() );

        assertTrue( "Test that the destination contents are copied correctly", source.equals( target ) );

        event.rollback();

        target = FileUtils.fileRead( testDest.getAbsolutePath() );

        assertTrue( "Test the destination file contents have been restored", target.equals( "overwritten contents" ) );
    }

    public void testCreateRollbackCommit()
        throws Exception
    {
        assertTrue( "Test if the source exists", testSource.exists() );

        String source = FileUtils.fileRead( testSource.getAbsolutePath() );

        CopyFileEvent event = new CopyFileEvent( testSource, testDest );

        assertFalse( "Test that the destination is not yet created", testDest.exists() );

        event.rollback();

        assertFalse( "Test that the destination file is not yet created", testDest.exists() );

        event.commit();

        assertTrue( "Test that the destination is created", testDest.exists() );

        String target = FileUtils.fileRead( testDest.getAbsolutePath() );

        assertTrue( "Test that the destination contents are copied correctly", source.equals( target ) );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        FileUtils.deleteDirectory(
            new File( PlexusTestCase.getBasedir(), "target/transaction-tests" ).getAbsolutePath() );
    }
}
