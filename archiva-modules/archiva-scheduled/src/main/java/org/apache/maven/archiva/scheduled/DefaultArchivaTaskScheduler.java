package org.apache.maven.archiva.scheduled;

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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.archiva.common.ArchivaException;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationEvent;
import org.apache.maven.archiva.configuration.ConfigurationListener;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.constraints.MostRecentRepositoryScanStatistics;
import org.apache.maven.archiva.repository.scanner.RepositoryScanStatistics;
import org.apache.maven.archiva.scheduled.tasks.ArtifactIndexingTask;
import org.apache.maven.archiva.scheduled.tasks.DatabaseTask;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTask;
import org.apache.maven.archiva.scheduled.tasks.TaskCreator;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Startable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StartingException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StoppingException;
import org.codehaus.plexus.scheduler.CronExpressionValidator;
import org.codehaus.plexus.scheduler.Scheduler;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.TaskQueue;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of a scheduling component for archiva.
 *
 * @plexus.component role="org.apache.maven.archiva.scheduled.ArchivaTaskScheduler" role-hint="default"
 */
public class DefaultArchivaTaskScheduler
    implements ArchivaTaskScheduler, Startable, ConfigurationListener
{
    private Logger log = LoggerFactory.getLogger( DefaultArchivaTaskScheduler.class );
    
    /**
     * @plexus.requirement
     */
    private Scheduler scheduler;

    /**
     * @plexus.requirement role-hint="database-update"
     */
    private TaskQueue databaseUpdateQueue;

    /**
     * @plexus.requirement role-hint="repository-scanning"
     */
    private TaskQueue repositoryScanningQueue;
    
    /**
     * @plexus.requirement role-hint="indexing"
     */
    private TaskQueue indexingQueue;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;
    
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    private static final String DATABASE_SCAN_GROUP = "dbg";

    private static final String DATABASE_JOB = "dbj";

    private static final String DATABASE_JOB_TRIGGER = "dbt";

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
        try
        {
            List<ManagedRepositoryConfiguration> repositories = archivaConfiguration.getConfiguration()
                .getManagedRepositories();

            for ( ManagedRepositoryConfiguration repoConfig : repositories )
            {
                if ( repoConfig.isScanned() )
                {
                    scheduleRepositoryJobs( repoConfig );
                    
                    if( !isPreviouslyScanned( repoConfig ) )
                    {
                        queueInitialRepoScan( repoConfig );
                    }
                }
            }

            scheduleDatabaseJobs();
        }
        catch ( SchedulerException e )
        {
            throw new StartingException( "Unable to start scheduler: " + e.getMessage(), e );
        }
    }

    public void stop()
        throws StoppingException
    {
        try
        {
            scheduler.unscheduleJob( DATABASE_JOB, DATABASE_SCAN_GROUP );

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

    /**
     * @see ArchivaTaskScheduler#scheduleDatabaseTasks()
     */
    public void scheduleDatabaseTasks()
        throws TaskExecutionException
    {
        try
        {
            scheduleDatabaseJobs();
        }
        catch ( SchedulerException e )
        {
            throw new TaskExecutionException( "Unable to schedule repository jobs: " + e.getMessage(), e );

        }
    }

    /**
     * @see ArchivaTaskScheduler#isProcessingRepositoryTask(String)
     */
    @SuppressWarnings("unchecked")
    public boolean isProcessingRepositoryTask( String repositoryId )
    {
        synchronized( repositoryScanningQueue )
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
    
    /**
     * @see ArchivaTaskScheduler#isProcessingIndexingTaskWithName(String)
     */
    @SuppressWarnings("unchecked")
    private boolean isProcessingRepositoryTask( RepositoryTask task )
    {
        synchronized( repositoryScanningQueue )
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

    /**
     * @see ArchivaTaskScheduler#isProcessingDatabaseTask()
     */
    @SuppressWarnings("unchecked")
    public boolean isProcessingDatabaseTask()
    {
        List<? extends Task> queue = null;

        try
        {
            queue = databaseUpdateQueue.getQueueSnapshot();
        }
        catch ( TaskQueueException e )
        {
            // not possible with plexus-taskqueue implementation, ignore
        }

        return !queue.isEmpty();
    }

    /**
     * @see ArchivaTaskScheduler#queueRepositoryTask(RepositoryTask)
     */
    public void queueRepositoryTask( RepositoryTask task )
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

    /**
     * @see ArchivaTaskScheduler#queueDatabaseTask(DatabaseTask)
     */
    public void queueDatabaseTask( DatabaseTask task )
        throws TaskQueueException
    {
        databaseUpdateQueue.put( task );
    }
    
    /**
     * @see ArchivaTaskScheduler#queueIndexingTask(ArtifactIndexingTask)
     */
    public void queueIndexingTask( ArtifactIndexingTask task )
        throws TaskQueueException
    {
        indexingQueue.put( task );
    }

    public void configurationEvent( ConfigurationEvent event )
    {
        if ( event.getType() == ConfigurationEvent.SAVED )
        {
            try
            {
                scheduler.unscheduleJob( DATABASE_JOB, DATABASE_SCAN_GROUP );

                scheduleDatabaseJobs();
            }
            catch ( SchedulerException e )
            {
                log.error( "Error restarting the database scanning job after property change." );
            }

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

            List<ManagedRepositoryConfiguration> repositories = archivaConfiguration.getConfiguration().getManagedRepositories();

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
    
    @SuppressWarnings("unchecked")
    private boolean isPreviouslyScanned( ManagedRepositoryConfiguration repoConfig )
    {
        List<RepositoryScanStatistics> results =
            (List<RepositoryScanStatistics>) dao.query( new MostRecentRepositoryScanStatistics( repoConfig.getId() ) );

        if ( results != null && !results.isEmpty() )
        {
            return true;
        }

        return false;
    }
    
    // MRM-848: Pre-configured repository initially appear to be empty
    private synchronized void queueInitialRepoScan( ManagedRepositoryConfiguration repoConfig )
    {
        String repoId = repoConfig.getId();        
        RepositoryTask task = TaskCreator.createRepositoryTask( repoId );

        if ( !queuedRepos.contains( repoId ) )
        {
            log.info( "Repository [" + repoId + "] is queued to be scanned as it hasn't been previously." );

            try
            {
                queuedRepos.add( repoConfig.getId() );
                this.queueRepositoryTask( task );
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
        JobDetail repositoryJob =
            new JobDetail( REPOSITORY_JOB + ":" + repoConfig.getId(), REPOSITORY_SCAN_GROUP, RepositoryTaskJob.class );

        JobDataMap dataMap = new JobDataMap();
        dataMap.put( DefaultArchivaTaskScheduler.TASK_QUEUE, repositoryScanningQueue );
        dataMap.put( DefaultArchivaTaskScheduler.TASK_REPOSITORY, repoConfig.getId() );
        repositoryJob.setJobDataMap( dataMap );

        try
        {
            CronTrigger trigger =
                new CronTrigger( REPOSITORY_JOB_TRIGGER + ":" + repoConfig.getId(), REPOSITORY_SCAN_GROUP, cronString );

            jobs.add( REPOSITORY_JOB + ":" + repoConfig.getId() );
            scheduler.scheduleJob( repositoryJob, trigger );
        }
        catch ( ParseException e )
        {
            log.error(
                "ParseException in repository scanning cron expression, disabling repository scanning for '" +
                    repoConfig.getId() + "': " + e.getMessage() );
        }

    }

    private synchronized void scheduleDatabaseJobs()
        throws SchedulerException
    {
        String cronString = archivaConfiguration.getConfiguration().getDatabaseScanning().getCronExpression();

        // setup the unprocessed artifact job
        JobDetail databaseJob = new JobDetail( DATABASE_JOB, DATABASE_SCAN_GROUP, DatabaseTaskJob.class );

        JobDataMap dataMap = new JobDataMap();
        dataMap.put( TASK_QUEUE, databaseUpdateQueue );
        databaseJob.setJobDataMap( dataMap );

        CronExpressionValidator cronValidator = new CronExpressionValidator();
        if ( !cronValidator.validate( cronString ) )
        {
            log.warn(
                "Cron expression [" + cronString + "] for database update is invalid.  Defaulting to hourly." );
            cronString = CRON_HOURLY;
        }

        try
        {
            CronTrigger trigger = new CronTrigger( DATABASE_JOB_TRIGGER, DATABASE_SCAN_GROUP, cronString );

            scheduler.scheduleJob( databaseJob, trigger );
        }
        catch ( ParseException e )
        {
            log.error(
                "ParseException in database scanning cron expression, disabling database scanning: " + e.getMessage() );
        }

    }
}
