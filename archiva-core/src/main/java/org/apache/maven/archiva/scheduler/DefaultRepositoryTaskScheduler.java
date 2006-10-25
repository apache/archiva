package org.apache.maven.archiva.scheduler;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationChangeException;
import org.apache.maven.archiva.configuration.ConfigurationChangeListener;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfigurationStoreException;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.scheduler.executors.IndexerTaskExecutor;
import org.apache.maven.archiva.scheduler.task.IndexerTask;
import org.apache.maven.archiva.scheduler.task.RepositoryTask;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Startable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StartingException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StoppingException;
import org.codehaus.plexus.scheduler.Scheduler;
import org.codehaus.plexus.taskqueue.TaskQueue;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import java.io.File;
import java.text.ParseException;

/**
 * Default implementation of a scheduling component for the application.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.archiva.scheduler.RepositoryTaskScheduler"
 */
public class DefaultRepositoryTaskScheduler
    extends AbstractLogEnabled
    implements RepositoryTaskScheduler, Startable, ConfigurationChangeListener
{
    /**
     * @plexus.requirement
     */
    private Scheduler scheduler;

    /**
     * @plexus.requirement role-hint="indexer"
     */
    private TaskQueue indexerQueue;

    /**
     * @plexus.requirement role="org.codehaus.plexus.taskqueue.execution.TaskExecutor" role-hint="indexer"
     */
    private IndexerTaskExecutor indexerTaskExecutor;

    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    /**
     * @plexus.requirement
     */
    private RepositoryArtifactIndexFactory indexFactory;

    private static final String DISCOVERER_GROUP = "DISCOVERER";

    private static final String INDEXER_JOB = "indexerTask";

    public void start()
        throws StartingException
    {
        Configuration configuration;
        try
        {
            configuration = configurationStore.getConfigurationFromStore();
            configurationStore.addChangeListener( this );
        }
        catch ( ConfigurationStoreException e )
        {
            throw new StartingException( "Unable to read configuration from the store", e );
        }

        try
        {
            scheduleJobs( configuration );
        }
        catch ( ParseException e )
        {
            throw new StartingException( "Invalid configuration: " + configuration.getIndexerCronExpression(), e );
        }
        catch ( SchedulerException e )
        {
            throw new StartingException( "Unable to start scheduler: " + e.getMessage(), e );
        }
    }

    private void scheduleJobs( Configuration configuration )
        throws ParseException, SchedulerException
    {
        if ( configuration.getIndexPath() != null )
        {
            JobDetail jobDetail = createJobDetail( INDEXER_JOB );

            getLogger().info( "Scheduling indexer: " + configuration.getIndexerCronExpression() );
            CronTrigger trigger =
                new CronTrigger( INDEXER_JOB + "Trigger", DISCOVERER_GROUP, configuration.getIndexerCronExpression() );
            scheduler.scheduleJob( jobDetail, trigger );

            try
            {
                queueNowIfNeeded();
            }
            catch ( org.codehaus.plexus.taskqueue.execution.TaskExecutionException e )
            {
                getLogger().error( "Error executing task first time, continuing anyway: " + e.getMessage(), e );
            }
        }
        else
        {
            getLogger().info( "Not scheduling indexer - index path is not configured" );
        }
    }

    private JobDetail createJobDetail( String jobName )
    {
        JobDetail jobDetail = new JobDetail( jobName, DISCOVERER_GROUP, RepositoryTaskJob.class );

        JobDataMap dataMap = new JobDataMap();
        dataMap.put( RepositoryTaskJob.TASK_QUEUE, indexerQueue );
        dataMap.put( RepositoryTaskJob.TASK_QUEUE_POLICY, RepositoryTask.QUEUE_POLICY_SKIP );
        jobDetail.setJobDataMap( dataMap );

        return jobDetail;
    }

    public void stop()
        throws StoppingException
    {
        try
        {
            scheduler.unscheduleJob( INDEXER_JOB, DISCOVERER_GROUP );
        }
        catch ( SchedulerException e )
        {
            throw new StoppingException( "Unable to unschedule tasks", e );
        }
    }

    public void notifyOfConfigurationChange( Configuration configuration )
        throws InvalidConfigurationException, ConfigurationChangeException
    {
        try
        {
            stop();

            scheduleJobs( configuration );
        }
        catch ( StoppingException e )
        {
            throw new ConfigurationChangeException( "Unable to unschedule previous tasks", e );
        }
        catch ( ParseException e )
        {
            throw new InvalidConfigurationException( "indexerCronExpression", "Invalid cron expression", e );
        }
        catch ( SchedulerException e )
        {
            throw new ConfigurationChangeException( "Unable to schedule new tasks", e );
        }
    }

    public void runIndexer()
        throws org.apache.maven.archiva.scheduler.TaskExecutionException
    {
        IndexerTask task = new IndexerTask();
        task.setJobName( "INDEX_INIT" );
        try
        {
            indexerQueue.put( task );
        }
        catch ( TaskQueueException e )
        {
            throw new org.apache.maven.archiva.scheduler.TaskExecutionException( e.getMessage(), e );
        }
    }

    public void queueNowIfNeeded()
        throws org.codehaus.plexus.taskqueue.execution.TaskExecutionException
    {
        Configuration configuration;
        try
        {
            configuration = configurationStore.getConfigurationFromStore();
        }
        catch ( ConfigurationStoreException e )
        {
            throw new TaskExecutionException( e.getMessage(), e );
        }

        File indexPath = new File( configuration.getIndexPath() );

        try
        {
            RepositoryArtifactIndex artifactIndex = indexFactory.createStandardIndex( indexPath );
            if ( !artifactIndex.exists() )
            {
                runIndexer();
            }
        }
        catch ( RepositoryIndexException e )
        {
            throw new TaskExecutionException( e.getMessage(), e );
        }
        catch ( org.apache.maven.archiva.scheduler.TaskExecutionException e )
        {
            throw new TaskExecutionException( e.getMessage(), e );
        }
    }

}
