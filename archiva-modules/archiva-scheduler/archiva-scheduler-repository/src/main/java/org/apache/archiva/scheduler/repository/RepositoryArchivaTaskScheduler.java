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

import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.maven.archiva.common.ArchivaException;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationEvent;
import org.apache.maven.archiva.configuration.ConfigurationListener;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Startable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StartingException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StoppingException;
import org.codehaus.plexus.scheduler.CronExpressionValidator;
import org.codehaus.plexus.scheduler.Scheduler;
import org.codehaus.plexus.taskqueue.TaskQueue;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of a scheduling component for archiva.
 *
 * @plexus.component role="org.apache.archiva.scheduler.ArchivaTaskScheduler" role-hint="repository"
 */
public class RepositoryArchivaTaskScheduler
    implements ArchivaTaskScheduler<RepositoryTask>, Startable, ConfigurationListener
{
    private Logger log = LoggerFactory.getLogger( RepositoryArchivaTaskScheduler.class );

    /**
     * @plexus.requirement
     */
    private Scheduler scheduler;

    /**
     * @plexus.requirement role-hint="repository-scanning"
     */
    private TaskQueue repositoryScanningQueue;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement
     */
    private RepositoryStatisticsManager repositoryStatisticsManager;

    /**
     * TODO: could have multiple implementations
     *
     * @plexus.requirement
     */
    private RepositorySessionFactory repositorySessionFactory;

    private static final String REPOSITORY_SCAN_GROUP = "rg";

    private static final String REPOSITORY_JOB = "rj";

    private static final String REPOSITORY_JOB_TRIGGER = "rjt";

    static final String TASK_QUEUE = "TASK_QUEUE";

    static final String TASK_REPOSITORY = "TASK_REPOSITORY";

    public static final String CRON_HOURLY = "0 0 * * * ?";

    private Set<String> jobs = new HashSet<String>();

    private List<String> queuedRepos = new ArrayList<String>();

    public void startup()
        throws ArchivaException
    {
        archivaConfiguration.addListener( this );

        try
        {
            start();
        }
        catch ( StartingException e )
        {
            throw new ArchivaException( e.getMessage(), e );
        }
    }

    public void start()
        throws StartingException
    {
        List<ManagedRepositoryConfiguration> repositories =
            archivaConfiguration.getConfiguration().getManagedRepositories();

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            for ( ManagedRepositoryConfiguration repoConfig : repositories )
            {
                if ( repoConfig.isScanned() )
                {
                    try
                    {
                        scheduleRepositoryJobs( repoConfig );
                    }
                    catch ( SchedulerException e )
                    {
                        throw new StartingException( "Unable to start scheduler: " + e.getMessage(), e );
                    }

                    try
                    {
                        if ( !isPreviouslyScanned( repoConfig, metadataRepository ) )
                        {
                            queueInitialRepoScan( repoConfig );
                        }
                    }
                    catch ( MetadataRepositoryException e )
                    {
                        log.warn( "Unable to determine if a repository is already scanned, skipping initial scan: " +
                                      e.getMessage(), e );
                    }
                }
            }
        }
        finally
        {
            repositorySession.close();
        }
    }

    public void stop()
        throws StoppingException
    {
        try
        {
            for ( String job : jobs )
            {
                scheduler.unscheduleJob( job, REPOSITORY_SCAN_GROUP );
            }
            jobs.clear();
            queuedRepos.clear();
        }
        catch ( SchedulerException e )
        {
            throw new StoppingException( "Unable to unschedule tasks", e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public boolean isProcessingRepositoryTask( String repositoryId )
    {
        synchronized ( repositoryScanningQueue )
        {
            List<RepositoryTask> queue = null;

            try
            {
                queue = repositoryScanningQueue.getQueueSnapshot();
            }
            catch ( TaskQueueException e )
            {
                // not possible with plexus-taskqueue implementation, ignore
            }

            for ( RepositoryTask queuedTask : queue )
            {
                if ( queuedTask.getRepositoryId().equals( repositoryId ) )
                {
                    return true;
                }
            }
            return false;
        }
    }

    @SuppressWarnings( "unchecked" )
    private boolean isProcessingRepositoryTask( RepositoryTask task )
    {
        synchronized ( repositoryScanningQueue )
        {
            List<RepositoryTask> queue = null;

            try
            {
                queue = repositoryScanningQueue.getQueueSnapshot();
            }
            catch ( TaskQueueException e )
            {
                // not possible with plexus-taskqueue implementation, ignore
            }

            for ( RepositoryTask queuedTask : queue )
            {
                if ( task.equals( queuedTask ) )
                {
                    return true;
                }
            }
            return false;
        }
    }

    public void queueTask( RepositoryTask task )
        throws TaskQueueException
    {
        synchronized ( repositoryScanningQueue )
        {
            if ( isProcessingRepositoryTask( task ) )
            {
                log.debug( "Repository task '" + task + "' is already queued. Skipping task." );
            }
            else
            {
                // add check if the task is already queued if it is a file scan
                repositoryScanningQueue.put( task );
            }
        }
    }

    public void configurationEvent( ConfigurationEvent event )
    {
        if ( event.getType() == ConfigurationEvent.SAVED )
        {
            for ( String job : jobs )
            {
                try
                {
                    scheduler.unscheduleJob( job, REPOSITORY_SCAN_GROUP );
                }
                catch ( SchedulerException e )
                {
                    log.error( "Error restarting the repository scanning job after property change." );
                }
            }
            jobs.clear();

            List<ManagedRepositoryConfiguration> repositories =
                archivaConfiguration.getConfiguration().getManagedRepositories();

            for ( ManagedRepositoryConfiguration repoConfig : repositories )
            {
                if ( repoConfig.getRefreshCronExpression() != null )
                {
                    try
                    {
                        scheduleRepositoryJobs( repoConfig );
                    }
                    catch ( SchedulerException e )
                    {
                        log.error( "error restarting job: " + REPOSITORY_JOB + ":" + repoConfig.getId() );
                    }
                }
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private boolean isPreviouslyScanned( ManagedRepositoryConfiguration repoConfig,
                                         MetadataRepository metadataRepository )
        throws MetadataRepositoryException
    {
        return repositoryStatisticsManager.getLastStatistics( metadataRepository, repoConfig.getId() ) != null;
    }

    // MRM-848: Pre-configured repository initially appear to be empty
    private synchronized void queueInitialRepoScan( ManagedRepositoryConfiguration repoConfig )
    {
        String repoId = repoConfig.getId();
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repoId );

        if ( !queuedRepos.contains( repoId ) )
        {
            log.info( "Repository [" + repoId + "] is queued to be scanned as it hasn't been previously." );

            try
            {
                queuedRepos.add( repoConfig.getId() );
                this.queueTask( task );
            }
            catch ( TaskQueueException e )
            {
                log.error( "Error occurred while queueing repository [" + repoId + "] task : " + e.getMessage() );
            }
        }
    }

    private synchronized void scheduleRepositoryJobs( ManagedRepositoryConfiguration repoConfig )
        throws SchedulerException
    {
        if ( repoConfig.getRefreshCronExpression() == null )
        {
            log.warn( "Skipping job, no cron expression for " + repoConfig.getId() );
            return;
        }

        if ( !repoConfig.isScanned() )
        {
            log.warn( "Skipping job, repository scannable has been disabled for " + repoConfig.getId() );
            return;
        }

        // get the cron string for these database scanning jobs
        String cronString = repoConfig.getRefreshCronExpression();

        CronExpressionValidator cronValidator = new CronExpressionValidator();
        if ( !cronValidator.validate( cronString ) )
        {
            log.warn( "Cron expression [" + cronString + "] for repository [" + repoConfig.getId() +
                          "] is invalid.  Defaulting to hourly." );
            cronString = CRON_HOURLY;
        }

        // setup the unprocessed artifact job
        JobDetail repositoryJob = new JobDetail( REPOSITORY_JOB + ":" + repoConfig.getId(), REPOSITORY_SCAN_GROUP,
                                                 RepositoryTaskJob.class );

        JobDataMap dataMap = new JobDataMap();
        dataMap.put( TASK_QUEUE, repositoryScanningQueue );
        dataMap.put( TASK_REPOSITORY, repoConfig.getId() );
        repositoryJob.setJobDataMap( dataMap );

        try
        {
            CronTrigger trigger = new CronTrigger( REPOSITORY_JOB_TRIGGER + ":" + repoConfig.getId(),
                                                   REPOSITORY_SCAN_GROUP, cronString );

            jobs.add( REPOSITORY_JOB + ":" + repoConfig.getId() );
            scheduler.scheduleJob( repositoryJob, trigger );
        }
        catch ( ParseException e )
        {
            log.error( "ParseException in repository scanning cron expression, disabling repository scanning for '" +
                           repoConfig.getId() + "': " + e.getMessage() );
        }

    }
}