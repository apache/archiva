package org.apache.archiva.admin.repository.admin;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.admin.RepositoryTaskAdministration;
import org.apache.archiva.admin.model.beans.ScanStatus;
import org.apache.archiva.components.taskqueue.TaskQueue;
import org.apache.archiva.components.taskqueue.TaskQueueException;
import org.apache.archiva.components.taskqueue.execution.TaskQueueExecutor;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.scheduler.indexing.ArtifactIndexingTask;
import org.apache.archiva.scheduler.indexing.IndexingArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Tag( "archiva-admin" )
@DisplayName( "Unit Tests for RepositoryTaskAdministration" )
public class RepositoryTaskAdministrationTest
{

    private RepositoryTaskAdministration taskAdministration;
    private TaskQueueExecutor<ArtifactIndexingTask> indexingTaskExecutor;
    private TaskQueueExecutor<RepositoryTask> scanningTaskExecutor;
    private RepositoryArchivaTaskScheduler repositoryArchivaTaskScheduler;
    private IndexingArchivaTaskScheduler indexingArchivaTaskScheduler;
    private RepositoryRegistry registry;

    @BeforeEach
    public void init() {
        registry = mock( RepositoryRegistry.class );
        indexingTaskExecutor = mock( TaskQueueExecutor.class );
        scanningTaskExecutor = mock( TaskQueueExecutor.class );
        repositoryArchivaTaskScheduler = mock( RepositoryArchivaTaskScheduler.class );
        indexingArchivaTaskScheduler = mock( IndexingArchivaTaskScheduler.class );
        this.taskAdministration = new DefaultRepositoryTaskAdministration( registry, indexingTaskExecutor,
            scanningTaskExecutor, repositoryArchivaTaskScheduler, indexingArchivaTaskScheduler );
    }

    @Test
    public void testScanStatus() throws RepositoryAdminException, TaskQueueException
    {
        TaskQueue queue = mock( TaskQueue.class );
        TaskQueue indexQueue = mock( TaskQueue.class );

        List<RepositoryTask> scanList = new ArrayList<>( );
        RepositoryTask scanTask1 = new RepositoryTask( );
        scanTask1.setRepositoryId( "abcde" );
        scanTask1.setScanAll( true );
        scanTask1.setResourceFile( null );
        scanList.add( scanTask1 );
        RepositoryTask scanTask2 = new RepositoryTask( );
        scanTask2.setRepositoryId( "testrepo2" );
        scanTask2.setScanAll( true );
        scanTask2.setResourceFile( null );
        scanList.add( scanTask1 );

        List<ArtifactIndexingTask> indexList = new ArrayList<>( );
        ArtifactIndexingTask indexTask1 = mock( ArtifactIndexingTask.class, RETURNS_DEEP_STUBS );
        when( indexTask1.getRepository( ).getId( ) ).thenReturn( "indexrepo1" );
        when( indexTask1.isExecuteOnEntireRepo( ) ).thenReturn( true );
        when( indexTask1.getResourceFile( ) ).thenReturn( null );
        indexList.add( indexTask1 );
        ArtifactIndexingTask indexTask2 = mock( ArtifactIndexingTask.class, RETURNS_DEEP_STUBS );
        when( indexTask2.getRepository( ).getId( ) ).thenReturn( "indexrepo2" );
        when( indexTask2.isExecuteOnEntireRepo( ) ).thenReturn( true );
        when( indexTask2.getResourceFile( ) ).thenReturn( null );
        indexList.add( indexTask2 );

        when( scanningTaskExecutor.getQueue( ) ).thenReturn( queue );
        when( indexingTaskExecutor.getQueue( ) ).thenReturn( indexQueue );
        when( queue.getQueueSnapshot( ) ).thenReturn( scanList );
        when( indexQueue.getQueueSnapshot( ) ).thenReturn( indexList );
        ScanStatus currentScanStatus = taskAdministration.getCurrentScanStatus( );
        assertNotNull( currentScanStatus );
        assertNotNull( currentScanStatus.getIndexingQueue( ) );
        assertNotNull( currentScanStatus.getScanQueue( ) );
        assertEquals( 2, currentScanStatus.getScanQueue( ).size( ) );
        assertEquals( 2, currentScanStatus.getIndexingQueue( ).size( ) );
    }

    @Test
    public void testScanStatusWithId() throws RepositoryAdminException, TaskQueueException
    {
        TaskQueue queue = mock( TaskQueue.class );
        TaskQueue indexQueue = mock( TaskQueue.class );

        List<RepositoryTask> scanList = new ArrayList<>( );
        RepositoryTask scanTask1 = new RepositoryTask( );
        scanTask1.setRepositoryId( "abcde" );
        scanTask1.setScanAll( true );
        scanTask1.setResourceFile( null );
        scanList.add( scanTask1 );
        RepositoryTask scanTask2 = new RepositoryTask( );
        scanTask2.setRepositoryId( "testrepo2" );
        scanTask2.setScanAll( true );
        scanTask2.setResourceFile( null );
        scanList.add( scanTask1 );

        List<ArtifactIndexingTask> indexList = new ArrayList<>( );
        ArtifactIndexingTask indexTask1 = mock( ArtifactIndexingTask.class, RETURNS_DEEP_STUBS );
        when( indexTask1.getRepository( ).getId( ) ).thenReturn( "indexrepo1" );
        when( indexTask1.isExecuteOnEntireRepo( ) ).thenReturn( true );
        when( indexTask1.getResourceFile( ) ).thenReturn( null );
        indexList.add( indexTask1 );
        ArtifactIndexingTask indexTask2 = mock( ArtifactIndexingTask.class, RETURNS_DEEP_STUBS );
        when( indexTask2.getRepository( ).getId( ) ).thenReturn( "indexrepo2" );
        when( indexTask2.isExecuteOnEntireRepo( ) ).thenReturn( true );
        when( indexTask2.getResourceFile( ) ).thenReturn( null );
        indexList.add( indexTask2 );

        when( scanningTaskExecutor.getQueue( ) ).thenReturn( queue );
        when( indexingTaskExecutor.getQueue( ) ).thenReturn( indexQueue );
        when( queue.getQueueSnapshot( ) ).thenReturn( scanList );
        when( indexQueue.getQueueSnapshot( ) ).thenReturn( indexList );
        when( registry.getManagedRepository( "indexrepo2" ) ).thenReturn( mock( ManagedRepository.class ) );
        ScanStatus currentScanStatus = taskAdministration.getCurrentScanStatus( "indexrepo2");
        assertNotNull( currentScanStatus );
        assertNotNull( currentScanStatus.getIndexingQueue( ) );
        assertNotNull( currentScanStatus.getScanQueue( ) );
        assertEquals( 0, currentScanStatus.getScanQueue( ).size( ) );
        assertEquals( 1, currentScanStatus.getIndexingQueue( ).size( ) );
    }

    @Test
    public void testScheduleFullScan() throws RepositoryAdminException, TaskQueueException
    {
        when( registry.getManagedRepository( "internal" ) ).thenReturn( mock( ManagedRepository.class ) );
        taskAdministration.scheduleFullScan( "internal" );
        verify( repositoryArchivaTaskScheduler, times(1) ).queueTask( any() );
        verify( indexingArchivaTaskScheduler, times(1) ).queueTask( any() );
    }

    @Test
    public void testScheduleIndexScan() throws RepositoryAdminException, TaskQueueException
    {
        when( registry.getManagedRepository( "internal" ) ).thenReturn( mock( ManagedRepository.class ) );
        taskAdministration.scheduleIndexFullScan( "internal" );
        ArgumentCaptor<ArtifactIndexingTask> captor = ArgumentCaptor.forClass( ArtifactIndexingTask.class );
        verify( repositoryArchivaTaskScheduler, times(0) ).queueTask( any() );
        verify( indexingArchivaTaskScheduler, times(1) ).queueTask( captor.capture() );
        assertTrue(captor.getValue().isExecuteOnEntireRepo());
    }

    @Test
    public void testScheduleIndexScanWithFile() throws RepositoryAdminException, TaskQueueException
    {
        ManagedRepository managedRepo = mock( ManagedRepository.class, RETURNS_DEEP_STUBS );
        when( registry.getManagedRepository( "internal" ) ).thenReturn( managedRepo );
        StorageAsset asset = mock( StorageAsset.class );
        when( asset.getFilePath( ) ).thenReturn( Paths.get( "abc/def/ghij.pom" ) );
        when( asset.exists( ) ).thenReturn( true );
        when( registry.getManagedRepository( "internal" ).getAsset( "abc/def/ghij.pom" ) ).thenReturn( asset );
        taskAdministration.scheduleIndexScan( "internal", "abc/def/ghij.pom" );
        ArgumentCaptor<ArtifactIndexingTask> captor = ArgumentCaptor.forClass( ArtifactIndexingTask.class );
        verify( repositoryArchivaTaskScheduler, times(0) ).queueTask( any() );
        verify( indexingArchivaTaskScheduler, times(1) ).queueTask( captor.capture() );
        ArtifactIndexingTask caption = captor.getValue( );
        assertFalse(caption.isExecuteOnEntireRepo());
        assertEquals( "abc/def/ghij.pom", caption.getResourceFile( ).toString() );
    }

    @Test
    public void testScheduleMetadataScan() throws RepositoryAdminException, TaskQueueException
    {
        when( registry.getManagedRepository( "internal" ) ).thenReturn( mock( ManagedRepository.class ) );
        taskAdministration.scheduleMetadataFullScan( "internal" );
        ArgumentCaptor<RepositoryTask> captor = ArgumentCaptor.forClass( RepositoryTask.class );
        verify( repositoryArchivaTaskScheduler, times(1) ).queueTask( captor.capture( ) );
        verify( indexingArchivaTaskScheduler, times(0) ).queueTask( any() );
        assertTrue(captor.getValue().isScanAll());
    }

    @Test
    public void testScheduleMetadataUpdateScan() throws RepositoryAdminException, TaskQueueException
    {
        when( registry.getManagedRepository( "internal" ) ).thenReturn( mock( ManagedRepository.class ) );
        taskAdministration.scheduleMetadataUpdateScan( "internal" );
        ArgumentCaptor<RepositoryTask> captor = ArgumentCaptor.forClass( RepositoryTask.class );
        verify( repositoryArchivaTaskScheduler, times(1) ).queueTask( captor.capture( ) );
        verify( indexingArchivaTaskScheduler, times(0) ).queueTask( any() );
        assertFalse(captor.getValue().isScanAll());
    }


    @Test
    void cancelAllTasks() throws TaskQueueException, RepositoryAdminException
    {
        TaskQueue queue = mock( TaskQueue.class );
        TaskQueue indexQueue = mock( TaskQueue.class );

        List<RepositoryTask> scanList = new ArrayList<>( );
        RepositoryTask scanTask1 = new RepositoryTask( );
        scanTask1.setRepositoryId( "abcde" );
        scanTask1.setScanAll( true );
        scanTask1.setResourceFile( null );
        scanList.add( scanTask1 );
        RepositoryTask scanTask2 = new RepositoryTask( );
        scanTask2.setRepositoryId( "testrepo2" );
        scanTask2.setScanAll( true );
        scanTask2.setResourceFile( null );
        scanList.add( scanTask1 );

        List<ArtifactIndexingTask> indexList = new ArrayList<>( );
        ArtifactIndexingTask indexTask1 = mock( ArtifactIndexingTask.class, RETURNS_DEEP_STUBS );
        when( indexTask1.getRepository( ).getId( ) ).thenReturn( "indexrepo1" );
        when( indexTask1.isExecuteOnEntireRepo( ) ).thenReturn( true );
        when( indexTask1.getResourceFile( ) ).thenReturn( null );
        indexList.add( indexTask1 );
        ArtifactIndexingTask indexTask2 = mock( ArtifactIndexingTask.class, RETURNS_DEEP_STUBS );
        when( indexTask2.getRepository( ).getId( ) ).thenReturn( "indexrepo2" );
        when( indexTask2.isExecuteOnEntireRepo( ) ).thenReturn( true );
        when( indexTask2.getResourceFile( ) ).thenReturn( null );
        indexList.add( indexTask2 );

        when( scanningTaskExecutor.getQueue( ) ).thenReturn( queue );
        when( indexingTaskExecutor.getQueue( ) ).thenReturn( indexQueue );
        when( queue.getQueueSnapshot( ) ).thenReturn( scanList );
        when( indexQueue.getQueueSnapshot( ) ).thenReturn( indexList );
        taskAdministration.cancelTasks( "indexrepo1" );
        ArgumentCaptor<List> scanCaptor = ArgumentCaptor.forClass( List.class );
        ArgumentCaptor<List> indexCaptor = ArgumentCaptor.forClass( List.class );

        verify( queue ).removeAll( scanCaptor.capture() );
        verify( indexQueue ).removeAll( indexCaptor.capture() );

        List scanCancelList = scanCaptor.getValue( );
        List indexCancelList = indexCaptor.getValue( );
        assertEquals( 0, scanCancelList.size( ) );
        assertEquals( 1, indexCancelList.size( ) );
        assertEquals( "indexrepo1", ( (ArtifactIndexingTask) indexCancelList.get( 0 ) ).getRepository( ).getId( ) );


    }
}
