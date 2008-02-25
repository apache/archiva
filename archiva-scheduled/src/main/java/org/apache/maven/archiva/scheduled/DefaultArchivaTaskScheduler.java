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

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.common.ArchivaException;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationEvent;
import org.apache.maven.archiva.configuration.ConfigurationListener;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.scheduled.tasks.ArchivaTask;
import org.apache.maven.archiva.scheduled.tasks.DatabaseTask;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTask;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTaskSelectionPredicate;
import org.codehaus.plexus.logging.AbstractLogEnabled;
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

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of a scheduling component for archiva.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:jmcconnell@apache.org">Jesse McConnell</a>
 * @plexus.component role="org.apache.maven.archiva.scheduled.ArchivaTaskScheduler" role-hint="default"
 */
public class DefaultArchivaTaskScheduler
    extends AbstractLogEnabled
    implements ArchivaTaskScheduler, Startable, ConfigurationListener
{
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
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    public static final String DATABASE_SCAN_GROUP = "database-group";

    public static final String DATABASE_JOB = "database-job";

    public static final String DATABASE_JOB_TRIGGER = "database-job-trigger";

    public static final String REPOSITORY_SCAN_GROUP = "repository-group";

    public static final String REPOSITORY_JOB = "repository-job";

    public static final String REPOSITORY_JOB_TRIGGER = "repository-job-trigger";

    public static final String CRON_HOURLY = "0 0 * * * ?";

    private Set<String> jobs = new HashSet<String>();

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
                }
            }

            scheduleDatabaseJobs();
        }
        catch ( SchedulerException e )
        {
            throw new StartingException( "Unable to start scheduler: " + e.getMessage(), e );
        }
    }

    private synchronized void scheduleRepositoryJobs( ManagedRepositoryConfiguration repoConfig )
        throws SchedulerException
    {
        if ( repoConfig.getRefreshCronExpression() == null )
        {
            getLogger().warn( "Skipping job, no cron expression for " + repoConfig.getId() );
            return;
        }

        // get the cron string for these database scanning jobs
        String cronString = repoConfig.getRefreshCronExpression();

        CronExpressionValidator cronValidator = new CronExpressionValidator();
        if ( !cronValidator.validate( cronString ) )
        {
            getLogger().warn( "Cron expression [" + cronString + "] for repository [" + repoConfig.getId() +
                "] is invalid.  Defaulting to hourly." );
            cronString = CRON_HOURLY;
        }

        // setup the unprocessed artifact job
        JobDetail repositoryJob =
            new JobDetail( REPOSITORY_JOB + ":" + repoConfig.getId(), REPOSITORY_SCAN_GROUP, RepositoryTaskJob.class );

        JobDataMap dataMap = new JobDataMap();
        dataMap.put( RepositoryTaskJob.TASK_QUEUE, repositoryScanningQueue );
        dataMap.put( RepositoryTaskJob.TASK_QUEUE_POLICY, ArchivaTask.QUEUE_POLICY_WAIT );
        dataMap.put( RepositoryTaskJob.TASK_REPOSITORY, repoConfig.getId() );
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
            getLogger().error(
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
        dataMap.put( DatabaseTaskJob.TASK_QUEUE, databaseUpdateQueue );
        databaseJob.setJobDataMap( dataMap );

        CronExpressionValidator cronValidator = new CronExpressionValidator();
        if ( !cronValidator.validate( cronString ) )
        {
            getLogger().warn(
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
            getLogger().error(
                "ParseException in database scanning cron expression, disabling database scanning: " + e.getMessage() );
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
        }
        catch ( SchedulerException e )
        {
            throw new StoppingException( "Unable to unschedule tasks", e );
        }
    }

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

    public boolean isProcessingAnyRepositoryTask()
        throws ArchivaException
    {
        List<? extends Task> queue = null;

        try
        {
            queue = repositoryScanningQueue.getQueueSnapshot();
        }
        catch ( TaskQueueException e )
        {
            throw new ArchivaException( "Unable to get repository scanning queue:" + e.getMessage(), e );
        }

        return !queue.isEmpty();
    }

    public boolean isProcessingRepositoryTask( String repositoryId )
        throws ArchivaException
    {
        List<? extends Task> queue = null;

        try
        {
            queue = repositoryScanningQueue.getQueueSnapshot();
        }
        catch ( TaskQueueException e )
        {
            throw new ArchivaException( "Unable to get repository scanning queue:" + e.getMessage(), e );
        }

        return CollectionUtils.exists( queue, new RepositoryTaskSelectionPredicate( repositoryId ) );
    }

    public boolean isProcessingDatabaseTask()
        throws ArchivaException
    {
        List<? extends Task> queue = null;

        try
        {
            queue = databaseUpdateQueue.getQueueSnapshot();
        }
        catch ( TaskQueueException e )
        {
            throw new ArchivaException( "Unable to get database update queue:" + e.getMessage(), e );
        }

        return !queue.isEmpty();
    }

    public void queueRepositoryTask( RepositoryTask task )
        throws TaskQueueException
    {
        repositoryScanningQueue.put( task );
    }

    public void queueDatabaseTask( DatabaseTask task )
        throws TaskQueueException
    {
        databaseUpdateQueue.put( task );
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
                getLogger().error( "Error restarting the database scanning job after property change." );
            }

            for ( String job : jobs )
            {
                try
                {
                    scheduler.unscheduleJob( job, REPOSITORY_SCAN_GROUP );
                }
                catch ( SchedulerException e )
                {
                    getLogger().error( "Error restarting the repository scanning job after property change." );
                }
            }
            jobs.clear();

            List<ManagedRepositoryConfiguration> repositories = archivaConfiguration.getConfiguration()
            .getManagedRepositories();

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
                        getLogger().error( "error restarting job: " + REPOSITORY_JOB + ":" + repoConfig.getId() );
                    }
                }
            }
        }
    }
}
