package org.apache.archiva.rest.services.v2;
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

import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.util.QueryHelper;
import org.apache.archiva.components.taskqueue.Task;
import org.apache.archiva.components.taskqueue.TaskQueueException;
import org.apache.archiva.components.taskqueue.execution.TaskQueueExecutor;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatisticsManager;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.scanner.RepositoryScanner;
import org.apache.archiva.repository.scanner.RepositoryScannerException;
import org.apache.archiva.rest.api.model.v2.Artifact;
import org.apache.archiva.rest.api.model.v2.ArtifactTransferRequest;
import org.apache.archiva.rest.api.model.v2.Repository;
import org.apache.archiva.rest.api.model.v2.RepositoryStatistics;
import org.apache.archiva.rest.api.model.v2.ScanStatus;
import org.apache.archiva.rest.api.services.v2.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.v2.ErrorMessage;
import org.apache.archiva.rest.api.services.v2.RepositoryService;
import org.apache.archiva.scheduler.indexing.ArtifactIndexingTask;
import org.apache.archiva.scheduler.indexing.IndexingArchivaTaskScheduler;
import org.apache.archiva.scheduler.indexing.maven.ArchivaIndexingTaskExecutor;
import org.apache.archiva.scheduler.repository.model.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@Service("v2.repositoryService#rest")
public class DefaultRepositoryService implements RepositoryService
{

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Inject
    RepositoryStatisticsManager repositoryStatisticsManager;

    @Inject
    @Named(value="taskQueueExecutor#indexing")
    TaskQueueExecutor<ArtifactIndexingTask> indexingTaskExecutor;

    @Inject
    @Named(value="taskQueueExecutor#repository-scanning")
    TaskQueueExecutor<RepositoryTask> scanningTaskExecutor;

    @Inject
    @Named(value = "archivaTaskScheduler#repository")
    private RepositoryArchivaTaskScheduler repositoryArchivaTaskScheduler;

    @Inject
    @Named( value = "archivaTaskScheduler#indexing" )
    private IndexingArchivaTaskScheduler indexingArchivaTaskScheduler;

    @Inject
    private RepositoryScanner repoScanner;


    private static final Logger log = LoggerFactory.getLogger( DefaultRepositoryService.class );
    private static final QueryHelper<org.apache.archiva.repository.Repository> QUERY_HELPER = new QueryHelper<>( new String[]{"id", "name"} );
    static
    {
        QUERY_HELPER.addStringFilter( "id", org.apache.archiva.repository.Repository::getId );
        QUERY_HELPER.addStringFilter( "name", org.apache.archiva.repository.Repository::getName );
        QUERY_HELPER.addNullsafeFieldComparator( "id", org.apache.archiva.repository.Repository::getId );
        QUERY_HELPER.addNullsafeFieldComparator( "name", org.apache.archiva.repository.Repository::getName );
    }

    @Override
    public PagedResult<Repository> getRepositories( String searchTerm, Integer offset, Integer limit, List<String> orderBy, String order,
                                                    String localeString) throws ArchivaRestServiceException
    {
        final Locale locale = StringUtils.isNotEmpty( localeString ) ? Locale.forLanguageTag( localeString ) : Locale.getDefault( );
        boolean isAscending = QUERY_HELPER.isAscending( order );
        Predicate<org.apache.archiva.repository.Repository> filter = QUERY_HELPER.getQueryFilter( searchTerm );
        Comparator<org.apache.archiva.repository.Repository> comparator = QUERY_HELPER.getComparator( orderBy, isAscending );
        try
        {
            int totalCount = Math.toIntExact( repositoryRegistry.getRepositories( ).stream( ).filter( filter ).count( ) );
            return new PagedResult<>( totalCount, offset, limit, repositoryRegistry.getRepositories( ).stream( )
                .filter( filter ).skip( offset ).limit( limit ).sorted( comparator ).map( repo -> Repository.of( repo, locale ) )
                .collect( Collectors.toList( ) ) );
        }
        catch ( ArithmeticException e )
        {
            log.error( "Invalid integer conversion for totalCount" );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.INVALID_RESULT_SET_ERROR ) );
        }
    }

    @Override
    public RepositoryStatistics getManagedRepositoryStatistics( String repositoryId ) throws ArchivaRestServiceException
    {
        if (repositoryRegistry.getManagedRepository( repositoryId )==null) {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_MANAGED_NOT_FOUND, repositoryId ), 404 );
        }
        try
        {
            return RepositoryStatistics.of( repositoryStatisticsManager.getLastStatistics( repositoryId ) );
        }
        catch ( MetadataRepositoryException e )
        {
            log.error( "Metadata error: {} ", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_METADATA_ERROR, e.getMessage( ) ) );
        }
    }

    @Override
    public Response scheduleRepositoryScan( String repositoryId, boolean fullScan ) throws ArchivaRestServiceException
    {
        try
        {
            org.apache.archiva.repository.ManagedRepository repository = repositoryRegistry.getManagedRepository( repositoryId );
            ArtifactIndexingTask task =
                new ArtifactIndexingTask( repository, null, ArtifactIndexingTask.Action.FINISH, repository.getIndexingContext() );
            task.setExecuteOnEntireRepo( true );
            task.setOnlyUpdate( !fullScan );
            indexingArchivaTaskScheduler.queueTask( task );
            repositoryArchivaTaskScheduler.queueTask( new RepositoryTask( repositoryId, fullScan ) );
            return Response.ok( ).build( );
        }  catch ( TaskQueueException e ) {
            log.error( "Could not queue the task: {}", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.TASK_QUEUE_FAILED, e.getMessage( ) ) );
        }
    }

    @Override
    public RepositoryStatistics scanRepositoryImmediately( String repositoryId ) throws ArchivaRestServiceException
    {
        long sinceWhen = RepositoryScanner.FRESH_SCAN;
        try
        {
            return RepositoryStatistics.of( repoScanner.scan( repositoryRegistry.getManagedRepository( repositoryId ), sinceWhen ) );
        }
        catch ( RepositoryScannerException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_SCAN_FAILED, e.getMessage() ));
        }
    }

    @Override
    public ScanStatus getScanStatus( String repositoryId ) throws ArchivaRestServiceException
    {
        ScanStatus status = new ScanStatus( );
        try
        {
            RepositoryTask scanTask = scanningTaskExecutor.getCurrentTask( );
            if ( !repositoryId.equals( scanTask.getRepositoryId( ) ) )
            {
                scanTask=null;
            }
            ArtifactIndexingTask indexTask = indexingTaskExecutor.getCurrentTask( );
            if (!repositoryId.equals(indexTask.getRepository().getId())) {
                indexTask = null;
            }
            status.updateScanInfo( scanTask, scanningTaskExecutor.getQueue( ).getQueueSnapshot( ).stream( ).filter( task -> repositoryId.equals(task.getRepositoryId()) ).collect( Collectors.toList() ) );
            status.updateIndexInfo( indexTask, indexingTaskExecutor.getQueue( ).getQueueSnapshot( ).stream().filter( task -> repositoryId.equals(task.getRepository().getId())).collect( Collectors.toList()) );
            return status;
        }
        catch ( TaskQueueException e )
        {
            log.error( "Could not get task information: {}", e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.TASK_QUEUE_FAILED, e.getMessage( ) ) );
        }
    }

    @Override
    public Response removeScanningTaskFromQueue( String repositoryId ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public Response copyArtifact( ArtifactTransferRequest artifactTransferRequest ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public Response scheduleDownloadRemoteIndex( String repositoryId, boolean now, boolean fullDownload ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public Response deleteArtifact( Artifact artifact ) throws ArchivaRestServiceException
    {
        return null;
    }

    @Override
    public List<String> getRunningRemoteDownloads( )
    {
        return null;
    }
}
