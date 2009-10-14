package org.apache.maven.archiva.scheduled.tasks;

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

import java.io.File;

import org.codehaus.plexus.spring.PlexusInSpringTestCase;

public class TaskCreatorTest
    extends PlexusInSpringTestCase
{
    private static final String REPO_ID = "test-repo";

    public void testCreateRepositoryTask()
        throws Exception
    {
        RepositoryTask task = TaskCreator.createRepositoryTask( REPO_ID );

        assertEquals( "Incorrect repository id set.", REPO_ID, task.getRepositoryId() );
    }

    public void testCreateRepositoryTaskWithTaskNameSuffix()
        throws Exception
    {
        RepositoryTask task = TaskCreator.createRepositoryTask( REPO_ID );

        assertBasicTaskDetails( task );
    }

    public void testCreateRepositoryTaskScanAllArtifacts()
        throws Exception
    {
        RepositoryTask task = TaskCreator.createRepositoryTask( REPO_ID, true );

        assertBasicTaskDetails( task );
        assertTrue( task.isScanAll() );
    }

    public void testCreateRepositoryTaskDoNotScanAllArtifacts()
        throws Exception
    {
        RepositoryTask task = TaskCreator.createRepositoryTask( REPO_ID, false );

        assertBasicTaskDetails( task );
        assertFalse( task.isScanAll() );
    }

    public void testCreateRepositoryTaskForArtifactUpdateAllRelated()
        throws Exception
    {
        File resource = new File( getBasedir(), "target/test-classes/test.jar" );
        RepositoryTask task = TaskCreator.createRepositoryTask( REPO_ID, resource, true );

        assertBasicTaskDetails( task );
        assertEquals( "Incorrect resource file set.", resource, task.getResourceFile() );
        assertTrue( task.isUpdateRelatedArtifacts() );
    }

    public void testCreateRepositoryTaskForArtifactDoNotUpdateAllRelated()
        throws Exception
    {
        File resource = new File( getBasedir(), "target/test-classes/test.jar" );
        RepositoryTask task = TaskCreator.createRepositoryTask( REPO_ID, resource, false );

        assertBasicTaskDetails( task );
        assertEquals( "Incorrect resource file set.", resource, task.getResourceFile() );
        assertFalse( task.isUpdateRelatedArtifacts() );
    }

    public void testCreateIndexingTask()
        throws Exception
    {
        File resource = new File( getBasedir(), "target/test-classes/test.jar" );
        ArtifactIndexingTask task = TaskCreator.createIndexingTask( REPO_ID, resource, ArtifactIndexingTask.Action.ADD );

        assertEquals( "Incorrect repository id set.", REPO_ID, task.getRepositoryId() );
        assertEquals( "Incorrect action set.", ArtifactIndexingTask.Action.ADD, task.getAction() );
        assertEquals( "Incorrect resource file set.", resource, task.getResourceFile() );
    }

    private void assertBasicTaskDetails( RepositoryTask task )
    {
        assertEquals( "Incorrect repository id set.", REPO_ID, task.getRepositoryId() );
    }

}
