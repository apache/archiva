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

import org.apache.archiva.common.ArchivaException;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationEvent;
import org.apache.archiva.configuration.ConfigurationListener;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatisticsManager;
import org.apache.archiva.components.scheduler.CronExpressionValidator;
import org.apache.archiva.components.scheduler.Scheduler;
import org.apache.archiva.components.taskqueue.TaskQueue;
import org.apache.archiva.components.taskqueue.TaskQueueException;
import org.apache.archiva.scheduler.repository.model.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of a scheduling component for archiva.
 */
@Service( "archivaTaskScheduler#repository" )
public class DefaultRepositoryArchivaTaskScheduler
    implements RepositoryArchivaTaskScheduler, ConfigurationListener
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private Scheduler scheduler;

    @Inject
    private CronExpressionValidator cronValidator;

    @Inject
    @Named( value = "taskQueue#repository-scanning" )
    private TaskQueue<RepositoryTask> repositoryScanningQueue;

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named( value = "repositoryStatisticsManager#default" )
    private RepositoryStatisticsManager repositoryStatisticsManager;

    /**
     * TODO: could have multiple implementations
     */
    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    private static final String REPOSITORY_SCAN_GROUP = "rg";

    private static final String REPOSITORY_JOB = "rj";

    private static final String REPOSITORY_JOB_TRIGGER = "rjt";

    static final String TASK_QUEUE = "TASK_QUEUE";

    static final String TASK_REPOSITORY = "TASK_REPOSITORY";

    public static final String CRON_HOURLY = "0 0 * * * ?";

    private Set<String> jobs = new HashSet<>();

    private List<String> queuedRepos = new ArrayList<>();

    @PostConstruct
    public void startup()
        throws ArchivaException
    {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        archivaConfiguration.addListener( this );

        List<ManagedRepositoryConfiguration> repositories =
            archivaConfiguration.getConfiguration().getManagedRepositories();

        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
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
                        throw new ArchivaException( "Unable to start scheduler: " + e.getMessage(), e );
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
                        log.warn( "Unable to determine if a repository is already scanned, skipping initial scan: {}",
                                  e.getMessage(), e );
                    }
                }
            }
        }
        finally
        {
            repositorySession.close();
        }

        stopWatch.stop();
        log.info( "Time to initalize DefaultRepositoryArchivaTaskScheduler: {} ms", stopWatch.getTime() );
    }


    @PreDestroy
    public void stop()
        throws SchedulerException
    {
        for ( String job : jobs )
        {
            scheduler.unscheduleJob( job, REPOSITORY_SCAN_GROUP );
        }
        jobs.clear();
        queuedRepos.clear();

    }

    @SuppressWarnings( "unchecked" )
    @Override
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

    @Override
    public boolean isProcessingRepositoryTask( RepositoryTask task )
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

    @Override
    public void queueTask( RepositoryTask task )
        throws TaskQueueException
    {
        synchronized ( repositoryScanningQueue )
        {
            if ( isProcessingRepositoryTask( task ) )
            {
                log.debug( "Repository task '{}' is already queued. Skipping task.", task );
            }
            else
            {
                // add check if the task is already queued if it is a file scan
                repositoryScanningQueue.put( task );
            }
        }
    }

    @Override
    public boolean unQueueTask( RepositoryTask task )
        throws TaskQueueException
    {
        synchronized ( repositoryScanningQueue )
        {
            if ( !isProcessingRepositoryTask( task ) )
            {
                log.info( "cannot unqueue Repository task '{}' not already queued.", task );
                return false;
            }
            else
            {
                return repositoryScanningQueue.remove( task );
            }
        }
    }

    @Override
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
                        log.error( "error restarting job: '{}' : '{}'", REPOSITORY_JOB, repoConfig.getId() );
                    }
                }
            }
        }
    }

    private boolean isPreviouslyScanned( ManagedRepositoryConfiguration repoConfig,
                                         MetadataRepository metadataRepository )
        throws MetadataRepositoryException
    {
        long start = System.currentTimeMillis();

        boolean res = repositoryStatisticsManager.hasStatistics( repoConfig.getId() );

        long end = System.currentTimeMillis();

        log.debug( "isPreviouslyScanned repo {} {} time: {} ms", repoConfig.getId(), res, ( end - start ) );

        return res;
    }

    // MRM-848: Pre-configured repository initially appear to be empty
    private synchronized void queueInitialRepoScan( ManagedRepositoryConfiguration repoConfig )
    {
        String repoId = repoConfig.getId();
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repoId );

        if ( !queuedRepos.contains( repoId ) )
        {
            log.info( "Repository [{}] is queued to be scanned as it hasn't been previously.", repoId );

            try
            {
                queuedRepos.add( repoConfig.getId() );
                this.queueTask( task );
            }
            catch ( TaskQueueException e )
            {
                log.error( "Error occurred while queueing repository [{}] task : {}", e.getMessage(), repoId );
            }
        }
    }

    private synchronized void scheduleRepositoryJobs( ManagedRepositoryConfiguration repoConfig )
        throws SchedulerException
    {
        if ( repoConfig.getRefreshCronExpression() == null )
        {
            log.warn( "Skipping job, no cron expression for {}", repoConfig.getId() );
            return;
        }

        if ( !repoConfig.isScanned() )
        {
            log.warn( "Skipping job, repository scannable has been disabled for {}", repoConfig.getId() );
            return;
        }

        // get the cron string for these database scanning jobs
        String cronString = repoConfig.getRefreshCronExpression();

        if ( !cronValidator.validate( cronString ) )
        {
            log.warn( "Cron expression [{}] for repository [{}] is invalid.  Defaulting to hourly.", cronString,
                      repoConfig.getId() );
            cronString = CRON_HOURLY;
        }

        JobDataMap jobDataMap = new JobDataMap( );
        jobDataMap.put( TASK_QUEUE, repositoryScanningQueue );
        jobDataMap.put( TASK_REPOSITORY, repoConfig.getId() );

        // setup the unprocessed artifact job
        JobDetail repositoryJob = JobBuilder.newJob( RepositoryTaskJob.class )
                                        .withIdentity( REPOSITORY_JOB + ":" + repoConfig.getId(), REPOSITORY_SCAN_GROUP )
                                        .setJobData( jobDataMap )
                                        .build();

        try
        {
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity( REPOSITORY_JOB_TRIGGER + ":" + repoConfig.getId(), REPOSITORY_SCAN_GROUP )
                    .withSchedule( CronScheduleBuilder.cronSchedule( cronString ) )
                    .build();

            jobs.add( REPOSITORY_JOB + ":" + repoConfig.getId() );
            scheduler.scheduleJob( repositoryJob, trigger );
        }
        catch ( RuntimeException e )
        {
            log.error(
                "ParseException in repository scanning cron expression, disabling repository scanning for '{}': {}",
                repoConfig.getId(), e.getMessage() );
        }

    }
}
