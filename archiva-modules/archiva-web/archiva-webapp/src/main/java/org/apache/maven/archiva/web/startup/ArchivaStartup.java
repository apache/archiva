package org.apache.maven.archiva.web.startup;

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

import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.maven.archiva.common.ArchivaException;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.ThreadedTaskQueueExecutor;
import org.codehaus.redback.components.scheduler.DefaultScheduler;
import org.quartz.SchedulerException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;

/**
 * ArchivaStartup - the startup of all archiva features in a deterministic order.
 *
 * @version $Id$
 */
public class ArchivaStartup
    implements ServletContextListener
{
    private ThreadedTaskQueueExecutor tqeDbScanning;

    private ThreadedTaskQueueExecutor tqeRepoScanning;

    private ThreadedTaskQueueExecutor tqeIndexing;

    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    private PlexusSisuBridge plexusSisuBridge;

    private NexusIndexer nexusIndexer;

    public void contextInitialized( ServletContextEvent contextEvent )
    {
        WebApplicationContext wac =
            WebApplicationContextUtils.getRequiredWebApplicationContext( contextEvent.getServletContext() );

        SecuritySynchronization securitySync = wac.getBean( SecuritySynchronization.class );

        repositoryTaskScheduler =
            wac.getBean( "archivaTaskScheduler#repository", RepositoryArchivaTaskScheduler.class );

        tqeRepoScanning = wac.getBean( "taskQueueExecutor#repository-scanning", ThreadedTaskQueueExecutor.class );

        tqeIndexing = wac.getBean( "taskQueueExecutor#indexing", ThreadedTaskQueueExecutor.class );

        plexusSisuBridge = wac.getBean( PlexusSisuBridge.class );

        try
        {
            nexusIndexer = plexusSisuBridge.lookup( NexusIndexer.class );
        }
        catch ( PlexusSisuBridgeException e )
        {
            throw new RuntimeException( "Unable to get NexusIndexer: " + e.getMessage(), e );
        }
        try
        {
            securitySync.startup();
            repositoryTaskScheduler.startup();
            Banner.display();
        }
        catch ( ArchivaException e )
        {
            throw new RuntimeException( "Unable to properly startup archiva: " + e.getMessage(), e );
        }
    }

    public void contextDestroyed( ServletContextEvent contextEvent )
    {
        WebApplicationContext applicationContext =
            WebApplicationContextUtils.getRequiredWebApplicationContext( contextEvent.getServletContext() );

        // TODO check this stop

        /*
        if ( applicationContext != null && applicationContext instanceof ClassPathXmlApplicationContext )
        {
            ( (ClassPathXmlApplicationContext) applicationContext ).close();
        } */

        if ( applicationContext != null ) //&& applicationContext instanceof PlexusWebApplicationContext )
        {
            // stop task queue executors
            stopTaskQueueExecutor( tqeDbScanning );
            stopTaskQueueExecutor( tqeRepoScanning );
            stopTaskQueueExecutor( tqeIndexing );

            // stop the DefaultArchivaTaskScheduler and its scheduler
            if ( repositoryTaskScheduler != null )
            {
                try
                {
                    repositoryTaskScheduler.stop();
                }
                catch ( SchedulerException e )
                {
                    e.printStackTrace();
                }

                try
                {
                    // shutdown the scheduler, otherwise Quartz scheduler and Threads still exists
                    Field schedulerField = repositoryTaskScheduler.getClass().getDeclaredField( "scheduler" );
                    schedulerField.setAccessible( true );

                    DefaultScheduler scheduler = (DefaultScheduler) schedulerField.get( repositoryTaskScheduler );
                    scheduler.stop();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }

            // close the application context
            //applicationContext.close();
            // TODO fix close call
            //applicationContext.
        }

        // closing correctly indexer to close correctly lock and file
        for( IndexingContext indexingContext : nexusIndexer.getIndexingContexts().values() )
        {
            try
            {
                indexingContext.close( false );
            } catch ( Exception e )
            {
                contextEvent.getServletContext().log( "skip error closing indexingContext " + e.getMessage() );
            }
        }

    }

    private void stopTaskQueueExecutor( ThreadedTaskQueueExecutor taskQueueExecutor )
    {
        if ( taskQueueExecutor != null )
        {
            Task currentTask = taskQueueExecutor.getCurrentTask();
            if ( currentTask != null )
            {
                taskQueueExecutor.cancelTask( currentTask );
            }

            try
            {
                taskQueueExecutor.stop();
                ExecutorService service = getExecutorServiceForTTQE( taskQueueExecutor );
                if ( service != null )
                {
                    service.shutdown();
                }
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
    }

    private ExecutorService getExecutorServiceForTTQE( ThreadedTaskQueueExecutor ttqe )
    {
        ExecutorService service = null;
        try
        {
            Field executorServiceField = ttqe.getClass().getDeclaredField( "executorService" );
            executorServiceField.setAccessible( true );
            service = (ExecutorService) executorServiceField.get( ttqe );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        return service;
    }
}
