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
import java.util.ArrayList;
import java.util.List;

import org.apache.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.archiva.repository.scanner.RepositoryScanStatistics;
import org.apache.archiva.repository.scanner.RepositoryScanner;
import org.apache.archiva.repository.scanner.RepositoryScannerException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.ArtifactsByRepositoryConstraint;
import org.apache.maven.archiva.database.constraints.MostRecentRepositoryScanStatistics;
import org.apache.maven.archiva.database.constraints.UniqueArtifactIdConstraint;
import org.apache.maven.archiva.database.constraints.UniqueGroupIdConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.repository.events.RepositoryListener;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ArchivaRepositoryScanningTaskExecutor
 *
 * @version $Id$
 *
 * @plexus.component
 *   role="org.codehaus.plexus.taskqueue.execution.TaskExecutor"
 *   role-hint="repository-scanning"
 */
public class ArchivaRepositoryScanningTaskExecutor
    implements TaskExecutor, Initializable
{
    private Logger log = LoggerFactory.getLogger( ArchivaRepositoryScanningTaskExecutor.class );

    /**
     * TODO: just for stats, remove this and use the main stats module
     *
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * The repository scanner component.
     *
     * @plexus.requirement
     */
    private RepositoryScanner repoScanner;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.repository.events.RepositoryListener"
     */
    private List<RepositoryListener> repositoryListeners;

    /**
     * @plexus.requirement
     */
    private RepositoryContentConsumers consumers;

    private Task task;

    public void initialize()
        throws InitializationException
    {
        log.info( "Initialized " + this.getClass().getName() );
    }

    @SuppressWarnings("unchecked")
    public void executeTask( Task task )
        throws TaskExecutionException
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

        if ( StringUtils.isBlank( repoTask.getRepositoryId() ) )
        {
            throw new TaskExecutionException("Unable to execute RepositoryTask with blank repository Id.");
        }

        ManagedRepositoryConfiguration arepo = archivaConfiguration.getConfiguration().findManagedRepositoryById( repoTask.getRepositoryId() );

        // execute consumers on resource file if set
        if( repoTask.getResourceFile() != null )
        {
            log.debug( "Executing task from queue with job name: " + repoTask );
            consumers.executeConsumers( arepo, repoTask.getResourceFile(), repoTask.isUpdateRelatedArtifacts() );
        }
        else
        {
            log.info( "Executing task from queue with job name: " + repoTask );

            // otherwise, execute consumers on whole repository
            try
            {
                if ( arepo == null )
                {
                    throw new TaskExecutionException( "Unable to execute RepositoryTask with invalid repository id: " + repoTask.getRepositoryId() );
                }

                long sinceWhen = RepositoryScanner.FRESH_SCAN;

                List<RepositoryContentStatistics> results = (List<RepositoryContentStatistics>) dao.query( new MostRecentRepositoryScanStatistics( arepo.getId() ) );

                if ( CollectionUtils.isNotEmpty( results ) )
                {
                    RepositoryContentStatistics lastStats = results.get( 0 );
                    if( !repoTask.isScanAll() )
                    {
                        sinceWhen = lastStats.getWhenGathered().getTime() + lastStats.getDuration();
                    }
                }

                RepositoryScanStatistics stats = repoScanner.scan( arepo, sinceWhen );

                log.info( "Finished first scan: " + stats.toDump( arepo ) );

                RepositoryContentStatistics dbstats = constructRepositoryStatistics( arepo, sinceWhen, results, stats );

                dao.getRepositoryContentStatisticsDAO().saveRepositoryContentStatistics( dbstats );

//                log.info( "Scanning for removed repository content" );

                // FIXME: remove hardcoding
//                MetadataRepository metadataRepository =
//                    new FileMetadataRepository( new File( arepo.getLocation(), ".metadata" ) );

//                metadataRepository.findAllProjects();
                // FIXME: do something

                log.info( "Finished repository task: " + repoTask );

                this.task = null;
            }
            catch ( RepositoryScannerException e )
            {
                throw new TaskExecutionException( "Repository error when executing repository job.", e );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private RepositoryContentStatistics constructRepositoryStatistics( ManagedRepositoryConfiguration arepo,
                                                                       long sinceWhen,
                                                                       List<RepositoryContentStatistics> results,
                                                                       RepositoryScanStatistics stats )
    {
        // I hate jpox and modello <-- and so do I
        RepositoryContentStatistics dbstats = new RepositoryContentStatistics();
        dbstats.setDuration( stats.getDuration() );
        dbstats.setNewFileCount( stats.getNewFileCount() );
        dbstats.setRepositoryId( stats.getRepositoryId() );
        dbstats.setTotalFileCount( stats.getTotalFileCount() );
        dbstats.setWhenGathered( stats.getWhenGathered() );

        // total artifact count
        try
        {
            List<ArchivaArtifact> artifacts = dao.getArtifactDAO().queryArtifacts(
                      new ArtifactsByRepositoryConstraint( arepo.getId(), stats.getWhenGathered(), "groupId", true ) );
            dbstats.setTotalArtifactCount( artifacts.size() );
        }
        catch ( ObjectNotFoundException oe )
        {
            log.error( "Object not found in the database : " + oe.getMessage() );
        }
        catch ( ArchivaDatabaseException ae )
        {
            log.error( "Error occurred while querying artifacts for artifact count : " + ae.getMessage() );
        }

        // total repo size
        long size = FileUtils.sizeOfDirectory( new File( arepo.getLocation() ) );
        dbstats.setTotalSize( size );

          // total unique groups
        List<String> repos = new ArrayList<String>();
        repos.add( arepo.getId() );

        List<String> groupIds = (List<String>) dao.query( new UniqueGroupIdConstraint( repos ) );
        dbstats.setTotalGroupCount( groupIds.size() );

        List<Object[]> artifactIds = (List<Object[]>) dao.query( new UniqueArtifactIdConstraint( arepo.getId(), true ) );
        dbstats.setTotalProjectCount( artifactIds.size() );

        return dbstats;
    }

    public Task getCurrentTaskInExecution()
    {
        return task;
    }
}
