package org.apache.maven.repository.scheduler;

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

import org.apache.maven.repository.configuration.Configuration;
import org.apache.maven.repository.configuration.ConfigurationChangeException;
import org.apache.maven.repository.configuration.ConfigurationChangeListener;
import org.apache.maven.repository.configuration.ConfigurationStore;
import org.apache.maven.repository.configuration.ConfigurationStoreException;
import org.apache.maven.repository.configuration.InvalidConfigurationException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Startable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StartingException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StoppingException;
import org.codehaus.plexus.scheduler.AbstractJob;
import org.codehaus.plexus.scheduler.Scheduler;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import java.text.ParseException;

/**
 * Default implementation of a scheduling component for the application.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo should we use plexus-taskqueue instead of or in addition to this?
 * @plexus.component role="org.apache.maven.repository.scheduler.RepositoryTaskScheduler"
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
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    private static final String DISCOVERER_GROUP = "DISCOVERER";

    private static final String INDEXER_JOB = "indexerTask";

    /**
     * @plexus.requirement role-hint="indexer"
     */
    private RepositoryTask indexerTask;

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
            JobDetail jobDetail = new JobDetail( INDEXER_JOB, DISCOVERER_GROUP, RepositoryTaskJob.class );
            JobDataMap dataMap = new JobDataMap();
            dataMap.put( AbstractJob.LOGGER, getLogger() );
            dataMap.put( RepositoryTaskJob.TASK_KEY, indexerTask );
            jobDetail.setJobDataMap( dataMap );

            getLogger().info( "Scheduling indexer: " + configuration.getIndexerCronExpression() );
            CronTrigger trigger =
                new CronTrigger( INDEXER_JOB + "Trigger", DISCOVERER_GROUP, configuration.getIndexerCronExpression() );
            scheduler.scheduleJob( jobDetail, trigger );

            // TODO: run as a job so it doesn't block startup/configuration saving
            try
            {
                indexerTask.executeNowIfNeeded();
            }
            catch ( TaskExecutionException e )
            {
                getLogger().error( "Error executing task first time, continuing anyway: " + e.getMessage(), e );
            }
        }
        else
        {
            getLogger().info( "Not scheduling indexer - index path is not configured" );
        }

        // TODO: wire in the converter
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
        throws TaskExecutionException
    {
        indexerTask.execute();
    }
}
