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
import org.apache.archiva.admin.model.beans.IndexingTask;
import org.apache.archiva.admin.model.beans.MetadataScanTask;
import org.apache.archiva.admin.model.beans.RepositoryTaskInfo;
import org.apache.archiva.admin.model.beans.ScanStatus;
import org.apache.archiva.components.taskqueue.TaskQueueException;
import org.apache.archiva.components.taskqueue.execution.TaskQueueExecutor;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatisticsManager;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.scheduler.indexing.ArtifactIndexingTask;
import org.apache.archiva.scheduler.indexing.IndexingArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service( "repositoryTaskAdministration#default" )
public class DefaultRepositoryTaskAdministration implements RepositoryTaskAdministration
{
    private static final Logger log = LoggerFactory.getLogger( DefaultRepositoryTaskAdministration.class );


    final RepositoryRegistry repositoryRegistry;

    final
    TaskQueueExecutor<ArtifactIndexingTask> indexingTaskExecutor;

    final
    TaskQueueExecutor<RepositoryTask> scanningTaskExecutor;

    private final RepositoryArchivaTaskScheduler repositoryArchivaTaskScheduler;

    private final IndexingArchivaTaskScheduler indexingArchivaTaskScheduler;

    public DefaultRepositoryTaskAdministration( RepositoryRegistry repositoryRegistry,
                                     @Named( value = "taskQueueExecutor#indexing" ) TaskQueueExecutor<ArtifactIndexingTask> indexingTaskExecutor,
                                     @Named( value = "taskQueueExecutor#repository-scanning" ) TaskQueueExecutor<RepositoryTask> scanningTaskExecutor,
                                     @Named( value = "archivaTaskScheduler#repository" ) RepositoryArchivaTaskScheduler repositoryArchivaTaskScheduler,
                                     @Named( value = "archivaTaskScheduler#indexing" ) IndexingArchivaTaskScheduler indexingArchivaTaskScheduler)
    {
        this.repositoryRegistry = repositoryRegistry;
        this.indexingTaskExecutor = indexingTaskExecutor;
        this.scanningTaskExecutor = scanningTaskExecutor;
        this.repositoryArchivaTaskScheduler = repositoryArchivaTaskScheduler;
        this.indexingArchivaTaskScheduler = indexingArchivaTaskScheduler;
    }

    @Override
    public void scheduleFullScan( String repositoryId ) throws RepositoryAdminException
    {
        if ( StringUtils.isEmpty( repositoryId ) ) {
            throw RepositoryAdminException.ofKey( "repository.id.invalid", "" );
        }
        try
        {
            org.apache.archiva.repository.ManagedRepository repository = repositoryRegistry.getManagedRepository( repositoryId );
            if (repository==null) {
                throw RepositoryAdminException.ofKey( "repository.not_found", repositoryId );
            }
            ArtifactIndexingTask task =
                new ArtifactIndexingTask( repository, null, ArtifactIndexingTask.Action.FINISH, repository.getIndexingContext( ) );
            task.setExecuteOnEntireRepo( true );
            task.setOnlyUpdate( false );
            indexingArchivaTaskScheduler.queueTask( task );
            repositoryArchivaTaskScheduler.queueTask( new RepositoryTask( repositoryId, true ) );
        }
        catch ( TaskQueueException e )
        {
            log.error( "Could not queue the task: {}", e.getMessage( ), e );
            throw RepositoryAdminException.ofKey( "repository.scan.task_queue_error", e, e.getMessage( ) );
        }
    }

    @Override
    public void scheduleIndexFullScan( String repositoryId ) throws RepositoryAdminException
    {
        if ( StringUtils.isEmpty( repositoryId ) ) {
            throw RepositoryAdminException.ofKey( "repository.id.invalid", "" );
        }
        try
        {
            org.apache.archiva.repository.ManagedRepository repository = repositoryRegistry.getManagedRepository( repositoryId );
            if (repository==null) {
                throw RepositoryAdminException.ofKey( "repository.not_found", repositoryId );
            }
            ArtifactIndexingTask task =
                new ArtifactIndexingTask( repository, null, ArtifactIndexingTask.Action.FINISH, repository.getIndexingContext( ) );
            task.setExecuteOnEntireRepo( true );
            task.setOnlyUpdate( false );
            indexingArchivaTaskScheduler.queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "Could not queue the task: {}", e.getMessage( ), e );
            throw RepositoryAdminException.ofKey( "repository.scan.task_queue_error", e, e.getMessage( ) );
        }
    }

    @Override
    public void scheduleIndexScan( String repositoryId, String relativePath ) throws RepositoryAdminException
    {
        if ( StringUtils.isEmpty( repositoryId ) ) {
            throw RepositoryAdminException.ofKey( "repository.id.invalid", "" );
        }
        try
        {
            org.apache.archiva.repository.ManagedRepository repository = repositoryRegistry.getManagedRepository( repositoryId );
            if (repository==null) {
                throw RepositoryAdminException.ofKey( "repository.not_found", repositoryId );
            }
            StorageAsset asset = repository.getAsset( relativePath );
            if (!asset.exists()) {
                throw RepositoryAdminException.ofKey( "repository.file.not_found", repositoryId, relativePath );
            }
            ArtifactIndexingTask task =
                new ArtifactIndexingTask( repository, asset.getFilePath( ), ArtifactIndexingTask.Action.FINISH, repository.getIndexingContext( ) );
            task.setExecuteOnEntireRepo( false );
            task.setOnlyUpdate( true );
            indexingArchivaTaskScheduler.queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "Could not queue the task: {}", e.getMessage( ), e );
            throw RepositoryAdminException.ofKey( "repository.scan.task_queue_error", e, e.getMessage( ) );
        }

    }

    @Override
    public void scheduleMetadataFullScan( String repositoryId ) throws RepositoryAdminException
    {
        if ( StringUtils.isEmpty( repositoryId ) ) {
            throw RepositoryAdminException.ofKey( "repository.id.invalid", "" );
        }
        try
        {
            org.apache.archiva.repository.ManagedRepository repository = repositoryRegistry.getManagedRepository( repositoryId );
            if (repository==null) {
                throw RepositoryAdminException.ofKey( "repository.not_found", repositoryId );
            }
            repositoryArchivaTaskScheduler.queueTask( new RepositoryTask( repositoryId, true ) );
        }
        catch ( TaskQueueException e )
        {
            log.error( "Could not queue the task: {}", e.getMessage( ), e );
            throw RepositoryAdminException.ofKey( "repository.scan.task_queue_error", e, e.getMessage( ) );
        }

    }

    @Override
    public void scheduleMetadataUpdateScan( String repositoryId ) throws RepositoryAdminException
    {
        if ( StringUtils.isEmpty( repositoryId ) ) {
            throw RepositoryAdminException.ofKey( "repository.id.invalid", "" );
        }
        try
        {
            org.apache.archiva.repository.ManagedRepository repository = repositoryRegistry.getManagedRepository( repositoryId );
            if (repository==null) {
                throw RepositoryAdminException.ofKey( "repository.not_found", repositoryId );
            }
            repositoryArchivaTaskScheduler.queueTask( new RepositoryTask( repositoryId, false ) );
        }
        catch ( TaskQueueException e )
        {
            log.error( "Could not queue the task: {}", e.getMessage( ), e );
            throw RepositoryAdminException.ofKey( "repository.scan.task_queue_error", e, e.getMessage( ) );
        }
        
    }

    public static MetadataScanTask getMetadataScanTaskInfo(RepositoryTask repositoryTask) {
        MetadataScanTask scanTask = new MetadataScanTask( );
        scanTask.setFullScan( repositoryTask.isScanAll());
        scanTask.setUpdateRelatedArtifacts( repositoryTask.isUpdateRelatedArtifacts() );
        StorageAsset file = repositoryTask.getResourceFile( );
        scanTask.setResource( repositoryTask.getResourceFile( )==null?"":repositoryTask.getResourceFile().toString( ) );
        scanTask.setMaxExecutionTimeMs( repositoryTask.getMaxExecutionTime() );
        scanTask.setRepositoryId( repositoryTask.getRepositoryId( ) );
        return scanTask;
    }

    public static IndexingTask getIndexingTaskInfo(ArtifactIndexingTask repositoryTask) {
        IndexingTask indexingTask = new IndexingTask( );
        indexingTask.setFullScan( repositoryTask.isExecuteOnEntireRepo());
        indexingTask.setUpdateOnly( repositoryTask.isOnlyUpdate() );
        indexingTask.setResource( repositoryTask.getResourceFile( )==null?"":repositoryTask.getResourceFile().toString( ) );
        indexingTask.setMaxExecutionTimeMs( repositoryTask.getMaxExecutionTime() );
        indexingTask.setRepositoryId( repositoryTask.getRepository().getId() );
        return indexingTask;

    }

    public void updateScanInfo( ScanStatus scanStatus, RepositoryTask runningRepositoryTask, List<RepositoryTask> taskQueue) {
        List<MetadataScanTask> newScanQueue = new ArrayList<>( );
        if (runningRepositoryTask!=null) {
            MetadataScanTask taskInfo = getMetadataScanTaskInfo( runningRepositoryTask );
            taskInfo.setRunning( true );
            newScanQueue.add( 0,  taskInfo);
        }
        newScanQueue.addAll( taskQueue.stream( ).map( task -> getMetadataScanTaskInfo( task ) ).collect( Collectors.toList( ) ) );
        scanStatus.setScanQueue( newScanQueue );
    }

    public void updateIndexInfo( ScanStatus scanStatus, ArtifactIndexingTask runningIndexingTask, List<ArtifactIndexingTask> taskQueue) {
        List<IndexingTask> newIndexQueue = new ArrayList<>(  );
        if (runningIndexingTask!=null) {
            IndexingTask taskInfo = getIndexingTaskInfo( runningIndexingTask );
            taskInfo.setRunning( true );
            newIndexQueue.add(taskInfo );
        }
        newIndexQueue.addAll( taskQueue.stream( ).map( task -> getIndexingTaskInfo( task ) ).collect( Collectors.toList( ) ) );
        scanStatus.setIndexingQueue( newIndexQueue );
    }


    @Override
    public ScanStatus getCurrentScanStatus( String repositoryId ) throws RepositoryAdminException
    {
        if ( StringUtils.isEmpty( repositoryId ) ) {
            throw RepositoryAdminException.ofKey( "repository.id.invalid", "" );
        }
        org.apache.archiva.repository.ManagedRepository repository = repositoryRegistry.getManagedRepository( repositoryId );
        if (repository==null) {
            throw RepositoryAdminException.ofKey( "repository.not_found", repositoryId );
        }
        ScanStatus status = new ScanStatus( );
        try
        {
            RepositoryTask scanTask = scanningTaskExecutor.getCurrentTask( );
            if ( scanTask!=null && !repositoryId.equals( scanTask.getRepositoryId( ) ) )
            {
                scanTask = null;
            }
            ArtifactIndexingTask indexTask = indexingTaskExecutor.getCurrentTask( );
            if ( indexTask!=null && !repositoryId.equals( indexTask.getRepository( ).getId( ) ) )
            {
                indexTask = null;
            }
            updateScanInfo( status, scanTask, scanningTaskExecutor.getQueue( ).getQueueSnapshot( ).stream( ).filter( task -> repositoryId.equals( task.getRepositoryId( ) ) ).collect( Collectors.toList( ) ) );
            updateIndexInfo( status, indexTask, indexingTaskExecutor.getQueue( ).getQueueSnapshot( ).stream( ).filter( task -> repositoryId.equals( task.getRepository( ).getId( ) ) ).collect( Collectors.toList( ) ) );
            return status;
        }
        catch ( TaskQueueException e )
        {
            log.error( "Could not get task information: {}", e.getMessage( ), e );
            throw RepositoryAdminException.ofKey( "repository.scan.task_retrieval_failed", e.getMessage( ) );
        }

    }

    @Override
    public ScanStatus getCurrentScanStatus( ) throws RepositoryAdminException
    {
        ScanStatus status = new ScanStatus( );
        try
        {
            RepositoryTask scanTask = scanningTaskExecutor.getCurrentTask( );
            ArtifactIndexingTask indexTask = indexingTaskExecutor.getCurrentTask( );
            updateScanInfo( status, scanTask, scanningTaskExecutor.getQueue( ).getQueueSnapshot() );
            updateIndexInfo( status, indexTask, indexingTaskExecutor.getQueue( ).getQueueSnapshot( ) );
            return status;
        }
        catch ( TaskQueueException e )
        {
            log.error( "Could not get task information: {}", e.getMessage( ), e );
            throw RepositoryAdminException.ofKey( "repository.scan.task_retrieval_failed", e.getMessage( ) );
        }

    }

    @Override
    public List<RepositoryTaskInfo> cancelTasks( String repositoryId ) throws RepositoryAdminException
    {
        ArrayList<RepositoryTaskInfo> resultList = new ArrayList<>( );
        resultList.addAll( cancelScanTasks( repositoryId ) );
        resultList.addAll( cancelIndexTasks( repositoryId ) );
        return resultList;
    }

    @Override
    public List<RepositoryTaskInfo> cancelScanTasks( String repositoryId ) throws RepositoryAdminException
    {
        try
        {
            ArrayList<RepositoryTaskInfo> resultList = new ArrayList<>( );
            List<RepositoryTask> removeTasks = scanningTaskExecutor.getQueue( ).getQueueSnapshot( ).stream( ).filter( task -> repositoryId.equals( task.getRepositoryId() ) ).collect( Collectors.toList( ) );
            scanningTaskExecutor.getQueue( ).removeAll( removeTasks );
            RepositoryTask currentTask = scanningTaskExecutor.getCurrentTask( );
            if ( currentTask != null && repositoryId.equals( currentTask.getRepositoryId()) )
            {
                scanningTaskExecutor.cancelTask( currentTask );
                resultList.add( getMetadataScanTaskInfo( currentTask ) );
            }
            resultList.addAll( removeTasks.stream( ).map( task -> getMetadataScanTaskInfo( task ) ).collect( Collectors.toList( ) ) );
            return resultList;
        }
        catch ( TaskQueueException e )
        {
            throw RepositoryAdminException.ofKey( "repository.task.dequeue_failed", repositoryId );
        }
    }

    @Override
    public List<RepositoryTaskInfo> cancelIndexTasks( String repositoryId ) throws RepositoryAdminException
    {
        try
        {
            ArrayList<RepositoryTaskInfo> resultList = new ArrayList<>( );
            List<ArtifactIndexingTask> removeTasks = indexingTaskExecutor.getQueue( ).getQueueSnapshot( ).stream( ).filter( task -> repositoryId.equals( task.getRepository( ).getId( ) ) ).collect( Collectors.toList( ) );
            indexingTaskExecutor.getQueue( ).removeAll( removeTasks );
            ArtifactIndexingTask currentTask = indexingTaskExecutor.getCurrentTask( );
            if ( currentTask != null && repositoryId.equals( currentTask.getRepository( ).getId( ) ) )
            {
                indexingTaskExecutor.cancelTask( currentTask );
                resultList.add( getIndexingTaskInfo( currentTask ) );
            }
            resultList.addAll( removeTasks.stream( ).map( task -> getIndexingTaskInfo( task ) ).collect( Collectors.toList( ) ) );
            return resultList;
        }
        catch ( TaskQueueException e )
        {
            throw RepositoryAdminException.ofKey( "repository.task.dequeue_failed", repositoryId );
        }
    }


}
