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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.RepositoryDAO;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Startable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StartingException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StoppingException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.codehaus.plexus.scheduler.Scheduler;
import org.codehaus.plexus.taskqueue.TaskQueue;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

/**
 * Default implementation of a scheduling component for archiva..
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:jmcconnell@apache.org">Jesse McConnell</a>
 * @plexus.component role="org.apache.maven.archiva.scheduled.ArchivaTaskScheduler" role-hint="default"
 */
public class DefaultArchivaTaskScheduler
    extends AbstractLogEnabled
    implements ArchivaTaskScheduler, Startable, RegistryListener
{
    /**
     * @plexus.requirement
     */
    private Scheduler scheduler;

   
    /**
     * @plexus.requirement role-hint="archiva-task-queue"
     */
    private TaskQueue archivaTaskQueue;
      
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;
  
    
    public static final String DATABASE_DISCOVERER_GROUP = "database-group";
    
    public static final String DATABASE_JOB = "database-job";
    public static final String DATABASE_JOB_TRIGGER = "database-job-trigger";
   
    public static final String REPOSITORY_DISCOVERER_GROUP = "repository-group";
    
    public static final String REPOSITORY_JOB = "repository-job";
    public static final String REPOSITORY_JOB_TRIGGER = "repository-job-trigger";
    
    public void start()
        throws StartingException
    {
        try
        {
        	List repositories = archivaConfiguration.getConfiguration().getRepositories();
        	
        	for ( Iterator i = repositories.iterator(); i.hasNext(); )
        	{
        		RepositoryConfiguration repoConfig = (RepositoryConfiguration)i.next();
        		
        		scheduleRepositoryJobs( repoConfig );        		
        	}
        	
        	scheduleDatabaseJobs( );
        }
        catch ( SchedulerException e )
        {
            throw new StartingException( "Unable to start scheduler: " + e.getMessage(), e );
        }
    }

    private void scheduleRepositoryJobs( RepositoryConfiguration repoConfig )
    	throws SchedulerException
    {
        if ( repoConfig.getRefreshCronExpression() == null )
        {
            getLogger().warn( "Skipping job, no cron expression for " + repoConfig.getId() );
            return;
        }
        
        // get the cron string for these database scanning jobs
        String cronString = repoConfig.getRefreshCronExpression();        
        
        // setup the unprocessed artifact job
        JobDetail repositoryJob =
            new JobDetail( REPOSITORY_JOB + ":" + repoConfig.getId() , REPOSITORY_DISCOVERER_GROUP, RepositoryTaskJob.class );

        JobDataMap dataMap = new JobDataMap();
        dataMap.put( RepositoryTaskJob.TASK_QUEUE, archivaTaskQueue );
        dataMap.put( RepositoryTaskJob.TASK_REPOSITORY, repoConfig.getId() );
        repositoryJob.setJobDataMap( dataMap );
       
        try 
        {
            CronTrigger trigger =
                new CronTrigger( REPOSITORY_JOB_TRIGGER + ":" + repoConfig.getId() , REPOSITORY_DISCOVERER_GROUP, cronString );
        
            scheduler.scheduleJob( repositoryJob, trigger );
        }
        catch ( ParseException e )
        {
            getLogger().error( "ParseException in repository scanning cron expression, disabling repository scanning for '" + repoConfig.getId() + "': " + e.getMessage() );
        }
             
    }
    
    private void scheduleDatabaseJobs( )
        throws SchedulerException
    {        
        String cronString = archivaConfiguration.getConfiguration().getDatabaseScanning().getCronExpression();
        
        // setup the unprocessed artifact job
        JobDetail databaseJob =
            new JobDetail( DATABASE_JOB, DATABASE_DISCOVERER_GROUP, DatabaseTaskJob.class );

        JobDataMap dataMap = new JobDataMap();
        dataMap.put( DatabaseTaskJob.TASK_QUEUE, archivaTaskQueue );
        databaseJob.setJobDataMap( dataMap );
       
        try 
        {
            CronTrigger trigger =
                new CronTrigger( DATABASE_JOB_TRIGGER, DATABASE_DISCOVERER_GROUP, cronString );
        
            scheduler.scheduleJob( databaseJob, trigger );
        }
        catch ( ParseException e )
        {
            getLogger().error( "ParseException in database scanning cron expression, disabling database scanning: " + e.getMessage() );
        }
        
    }

    public void stop()
        throws StoppingException
    {
        try
        {
            scheduler.unscheduleJob( DATABASE_JOB, DATABASE_DISCOVERER_GROUP );         
        }
        catch ( SchedulerException e )
        {
            throw new StoppingException( "Unable to unschedule tasks", e );
        }
    }


    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        // nothing to do
    }

    /**
     * 
     */
    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        // cronExpression comes from the database scanning section
        if ( "cronExpression".equals( propertyName ) )
        {
            getLogger().debug( "Restarting the database scheduled task after property change: " + propertyName );
            
            try
            {
                scheduler.unscheduleJob( DATABASE_JOB, DATABASE_DISCOVERER_GROUP );
            
                scheduleDatabaseJobs();
            }
            catch ( SchedulerException e )
            {
                getLogger().error( "Error restarting the database scanning job after property change." );
            }
        }
        
        // refreshCronExpression comes from the repositories section
        // 
        // currently we have to reschedule all repo jobs because we don't know where the changed one came from
        if ( "refreshCronExpression".equals( propertyName ) )
        {
            List repositories = archivaConfiguration.getConfiguration().getRepositories();
            
            for ( Iterator i = repositories.iterator(); i.hasNext(); )
            {
                RepositoryConfiguration repoConfig = (RepositoryConfiguration)i.next();
                
                if ( repoConfig.getRefreshCronExpression() != null )
                {
                    try
                    {
                        // unschedule handles jobs that might not exist
                        scheduler.unscheduleJob( REPOSITORY_JOB + ":" + repoConfig.getId() , REPOSITORY_DISCOVERER_GROUP );
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

    public void runAllRepositoryTasks() throws TaskExecutionException
    {
        try
        {
            List repositories = archivaConfiguration.getConfiguration().getRepositories();
            
            for ( Iterator i = repositories.iterator(); i.hasNext(); )
            {
                RepositoryConfiguration repoConfig = (RepositoryConfiguration)i.next();
                
                scheduleRepositoryJobs( repoConfig );               
            }
            
        }
        catch ( SchedulerException e )
        {
            throw new TaskExecutionException( "Unable to schedule repository jobs: " + e.getMessage(), e );
        }
    }

    public void runDatabaseTasks() throws TaskExecutionException
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

    public void runRepositoryTasks( String repositoryId ) throws TaskExecutionException
    {
        try
        {
            RepositoryConfiguration repoConfig = archivaConfiguration.getConfiguration().findRepositoryById( repositoryId );
            
            scheduleRepositoryJobs( repoConfig );                         
        }
        catch ( SchedulerException e )
        {
            throw new TaskExecutionException( "Unable to schedule repository jobs: " + e.getMessage(), e );
        } 
    }

    
    
}
