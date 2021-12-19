package org.apache.archiva.rest.v2.svc;
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
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.util.QueryHelper;
import org.apache.archiva.components.rest.util.RestUtil;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatisticsManager;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.scanner.RepositoryScanner;
import org.apache.archiva.repository.scanner.RepositoryScannerException;
import org.apache.archiva.rest.api.v2.model.Repository;
import org.apache.archiva.rest.api.v2.model.RepositoryStatistics;
import org.apache.archiva.rest.api.v2.model.ScanStatus;
import org.apache.archiva.rest.api.v2.svc.ArchivaRestServiceException;
import org.apache.archiva.rest.api.v2.svc.ErrorKeys;
import org.apache.archiva.rest.api.v2.svc.ErrorMessage;
import org.apache.archiva.rest.api.v2.svc.RepositoryService;
import org.apache.archiva.scheduler.indexing.DownloadRemoteIndexException;
import org.apache.archiva.scheduler.indexing.DownloadRemoteIndexScheduler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Named;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@Service( "v2.repositoryService#rest" )
public class DefaultRepositoryService implements RepositoryService
{

    final
    RepositoryRegistry repositoryRegistry;

    final
    RepositoryStatisticsManager repositoryStatisticsManager;

    private final RepositoryTaskAdministration repositoryTaskAdministration;

    private final RepositoryScanner repoScanner;

    private final DownloadRemoteIndexScheduler downloadRemoteIndexScheduler;

    private static final Logger log = LoggerFactory.getLogger( DefaultRepositoryService.class );
    private static final QueryHelper<org.apache.archiva.repository.Repository> QUERY_HELPER = new QueryHelper<>( new String[]{"id", "name"} );

    static
    {
        QUERY_HELPER.addStringFilter( "id", org.apache.archiva.repository.Repository::getId );
        QUERY_HELPER.addStringFilter( "name", org.apache.archiva.repository.Repository::getName );
        QUERY_HELPER.addStringFilter( "description", org.apache.archiva.repository.Repository::getDescription );
        QUERY_HELPER.addStringFilter( "type", repo -> repo.getType( ).name( ) );
        QUERY_HELPER.addBooleanFilter( "scanned", org.apache.archiva.repository.Repository::isScanned );
        QUERY_HELPER.addNullsafeFieldComparator( "id", org.apache.archiva.repository.Repository::getId );
        QUERY_HELPER.addNullsafeFieldComparator( "name", org.apache.archiva.repository.Repository::getName );
        QUERY_HELPER.addNullsafeFieldComparator( "type", repo -> repo.getType( ).name( ) );
        QUERY_HELPER.addNullsafeFieldComparator( "boolean", org.apache.archiva.repository.Repository::isScanned );
    }

    public DefaultRepositoryService( RepositoryRegistry repositoryRegistry, RepositoryStatisticsManager repositoryStatisticsManager,
                                     @Named( value = "repositoryTaskAdministration#default") RepositoryTaskAdministration repositoryTaskAdministration,
                                     RepositoryScanner repoScanner, DownloadRemoteIndexScheduler downloadRemoteIndexScheduler )
    {
        this.repositoryRegistry = repositoryRegistry;
        this.repositoryStatisticsManager = repositoryStatisticsManager;
        this.repoScanner = repoScanner;
        this.repositoryTaskAdministration = repositoryTaskAdministration;
        this.downloadRemoteIndexScheduler = downloadRemoteIndexScheduler;
    }

    private void handleAdminException( RepositoryAdminException e ) throws ArchivaRestServiceException
    {
        log.error( "Repository admin error: {}", e.getMessage( ), e );
        if ( e.keyExists( ) )
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.PREFIX + e.getKey( ), e.getParameters( ) ) );
        }
        else
        {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_ADMIN_ERROR, e.getMessage( ) ) );
        }
    }


    @Override
    public PagedResult<Repository> getRepositories( String searchTerm, Integer offset, Integer limit, List<String> orderBy, String order,
                                                    String localeString ) throws ArchivaRestServiceException
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
        if ( repositoryRegistry.getManagedRepository( repositoryId ) == null )
        {
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
            repositoryTaskAdministration.scheduleFullScan( repositoryId );
            return Response.ok( ).build( );
        }
        catch ( RepositoryAdminException e )
        {
            handleAdminException( e );
            return Response.serverError( ).build( );
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
            log.error( e.getMessage( ), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_SCAN_FAILED, e.getMessage( ) ) );
        }
    }

    @Override
    public ScanStatus getScanStatus( String repositoryId ) throws ArchivaRestServiceException
    {
        try
        {
            return ScanStatus.of( repositoryTaskAdministration.getCurrentScanStatus( ) );
        }
        catch ( RepositoryAdminException e )
        {
            handleAdminException( e );
            return new ScanStatus();
        }
    }

    @Override
    public Response removeScanningTaskFromQueue( String repositoryId ) throws ArchivaRestServiceException
    {
        try
        {
            repositoryTaskAdministration.cancelTasks( repositoryId );
            return Response.ok( ).build( );
        }
        catch ( RepositoryAdminException e )
        {
            handleAdminException( e );
            return Response.serverError( ).build( );
        }
    }


    @Override
    public Response scheduleDownloadRemoteIndex( String repositoryId, boolean immediately, boolean full,
                                                 UriInfo uriInfo ) throws ArchivaRestServiceException
    {
        boolean immediateSet = RestUtil.isFlagSet( uriInfo, "immediate" );
        boolean fullSet = RestUtil.isFlagSet( uriInfo, "full" );
        RemoteRepository repo = repositoryRegistry.getRemoteRepository( repositoryId );
        if (repo==null) {
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_REMOTE_NOT_FOUND, repositoryId ), 404 );
        }
        try
        {
            downloadRemoteIndexScheduler.scheduleDownloadRemote( repositoryId, immediateSet, fullSet );
            return Response.ok( ).build( );
        }
        catch ( DownloadRemoteIndexException e )
        {
            log.error( "Could not schedule index download for repository {}: {}", repositoryId, e.getMessage(), e );
            throw new ArchivaRestServiceException( ErrorMessage.of( ErrorKeys.REPOSITORY_REMOTE_INDEX_DOWNLOAD_FAILED, e.getMessage( ) ) );
        }
    }



    @Override
    public List<String> getRunningRemoteDownloads( )
    {
        return downloadRemoteIndexScheduler.getRunningRemoteDownloadIds( );
    }
}
