package org.apache.archiva.scheduler.repository;

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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.archiva.repository.scanner.RepositoryScanStatistics;
import org.apache.archiva.repository.scanner.RepositoryScanner;
import org.apache.archiva.repository.scanner.RepositoryScannerException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Date;

/**
 * ArchivaRepositoryScanningTaskExecutor
 *
 * @version $Id$
 */
@Service( "taskExecutor#repository-scanning" )
public class ArchivaRepositoryScanningTaskExecutor
    implements TaskExecutor, Initializable
{
    private Logger log = LoggerFactory.getLogger( ArchivaRepositoryScanningTaskExecutor.class );

    /**
     *
     */
    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    /**
     * The repository scanner component.
     */
    @Inject
    private RepositoryScanner repoScanner;

    /**
     *
     */
    @Inject
    private RepositoryContentConsumers consumers;

    private Task task;

    /**
     *
     */
    @Inject
    private RepositoryStatisticsManager repositoryStatisticsManager;

    /**
     * TODO: may be different implementations
     */
    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    @PostConstruct
    public void initialize()
        throws InitializationException
    {
        log.info( "Initialized {}", this.getClass().getName() );
    }

    @SuppressWarnings( "unchecked" )
    public void executeTask( Task task )
        throws TaskExecutionException
    {
        try
        {
            // TODO: replace this whole class with the prescribed content scanning service/action
            // - scan repository for artifacts that do not have corresponding metadata or have been updated and
            // send events for each
            // - scan metadata for artifacts that have been removed and send events for each
            // - scan metadata for missing plugin data
            // - store information so that it can restart upon failure (publish event on the server recovery
            // queue, remove it on successful completion)

            this.task = task;

            RepositoryTask repoTask = (RepositoryTask) task;

            String repoId = repoTask.getRepositoryId();
            if ( StringUtils.isBlank( repoId ) )
            {
                throw new TaskExecutionException( "Unable to execute RepositoryTask with blank repository Id." );
            }

            ManagedRepository arepo = managedRepositoryAdmin.getManagedRepository( repoId );

            // execute consumers on resource file if set
            if ( repoTask.getResourceFile() != null )
            {
                log.debug( "Executing task from queue with job name: {}", repoTask );
                consumers.executeConsumers( arepo, repoTask.getResourceFile(), repoTask.isUpdateRelatedArtifacts() );
            }
            else
            {
                log.info( "Executing task from queue with job name: {}", repoTask );

                // otherwise, execute consumers on whole repository
                if ( arepo == null )
                {
                    throw new TaskExecutionException(
                        "Unable to execute RepositoryTask with invalid repository id: " + repoId );
                }

                long sinceWhen = RepositoryScanner.FRESH_SCAN;
                long previousFileCount = 0;

                RepositorySession repositorySession = repositorySessionFactory.createSession();
                MetadataRepository metadataRepository = repositorySession.getRepository();
                try
                {
                    if ( !repoTask.isScanAll() )
                    {
                        RepositoryStatistics previousStats =
                            repositoryStatisticsManager.getLastStatistics( metadataRepository, repoId );
                        if ( previousStats != null )
                        {
                            sinceWhen = previousStats.getScanStartTime().getTime();
                            previousFileCount = previousStats.getTotalFileCount();
                        }
                    }

                    RepositoryScanStatistics stats;
                    try
                    {
                        stats = repoScanner.scan( arepo, sinceWhen );
                    }
                    catch ( RepositoryScannerException e )
                    {
                        throw new TaskExecutionException( "Repository error when executing repository job.", e );
                    }

                    log.info( "Finished first scan: {}", stats.toDump( arepo ) );

                    // further statistics will be populated by the following method
                    Date endTime = new Date( stats.getWhenGathered().getTime() + stats.getDuration() );

                    log.info( "Gathering repository statistics" );

                    repositoryStatisticsManager.addStatisticsAfterScan( metadataRepository, repoId,
                                                                        stats.getWhenGathered(), endTime,
                                                                        stats.getTotalFileCount(),
                                                                        stats.getTotalFileCount() - previousFileCount );
                    repositorySession.save();
                }
                catch ( MetadataRepositoryException e )
                {
                    throw new TaskExecutionException( "Unable to store updated statistics: " + e.getMessage(), e );
                }
                finally
                {
                    repositorySession.close();
                }

//                log.info( "Scanning for removed repository content" );

//                metadataRepository.findAllProjects();
                // FIXME: do something

                log.info( "Finished repository task: {}", repoTask );

                this.task = null;
            }
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            throw new TaskExecutionException( e.getMessage(), e );
        }
    }

    public Task getCurrentTaskInExecution()
    {
        return task;
    }

    public RepositoryScanner getRepoScanner()
    {
        return repoScanner;
    }

    public void setRepoScanner( RepositoryScanner repoScanner )
    {
        this.repoScanner = repoScanner;
    }

    public RepositoryContentConsumers getConsumers()
    {
        return consumers;
    }

    public void setConsumers( RepositoryContentConsumers consumers )
    {
        this.consumers = consumers;
    }

    public RepositorySessionFactory getRepositorySessionFactory()
    {
        return repositorySessionFactory;
    }

    public void setRepositorySessionFactory( RepositorySessionFactory repositorySessionFactory )
    {
        this.repositorySessionFactory = repositorySessionFactory;
    }

    public RepositoryStatisticsManager getRepositoryStatisticsManager()
    {
        return repositoryStatisticsManager;
    }

    public void setRepositoryStatisticsManager( RepositoryStatisticsManager repositoryStatisticsManager )
    {
        this.repositoryStatisticsManager = repositoryStatisticsManager;
    }

    public ManagedRepositoryAdmin getManagedRepositoryAdmin()
    {
        return managedRepositoryAdmin;
    }

    public void setManagedRepositoryAdmin( ManagedRepositoryAdmin managedRepositoryAdmin )
    {
        this.managedRepositoryAdmin = managedRepositoryAdmin;
    }
}
