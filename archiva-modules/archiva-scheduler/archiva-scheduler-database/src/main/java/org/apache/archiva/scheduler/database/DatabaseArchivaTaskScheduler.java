package org.apache.archiva.scheduler.database;

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
import java.util.List;

import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.maven.archiva.common.ArchivaException;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationEvent;
import org.apache.maven.archiva.configuration.ConfigurationListener;
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
 * @plexus.component role="org.apache.archiva.scheduler.ArchivaTaskScheduler" role-hint="database"
 */
public class DatabaseArchivaTaskScheduler
    implements ArchivaTaskScheduler<DatabaseTask>, Startable, ConfigurationListener
{
    private Logger log = LoggerFactory.getLogger( DatabaseArchivaTaskScheduler.class );

    /**
     * @plexus.requirement
     */
    private Scheduler scheduler;

    /**
     * @plexus.requirement role-hint="database-update"
     */
    private TaskQueue databaseUpdateQueue;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private static final String DATABASE_SCAN_GROUP = "dbg";

    private static final String DATABASE_JOB = "dbj";

    private static final String DATABASE_JOB_TRIGGER = "dbt";

    static final String TASK_QUEUE = "TASK_QUEUE";

    public static final String CRON_HOURLY = "0 0 * * * ?";

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

    public void queueTask( DatabaseTask task )
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
                log.error( "Error restarting the database scanning job after property change." );
            }
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