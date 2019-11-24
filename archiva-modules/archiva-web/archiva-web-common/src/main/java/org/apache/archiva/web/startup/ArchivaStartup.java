package org.apache.archiva.web.startup;

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

import org.apache.archiva.common.ArchivaException;
import org.apache.archiva.components.scheduler.DefaultScheduler;
import org.apache.archiva.components.taskqueue.Task;
import org.apache.archiva.components.taskqueue.execution.ThreadedTaskQueueExecutor;
import org.apache.archiva.scheduler.repository.DefaultRepositoryArchivaTaskScheduler;
import org.quartz.SchedulerException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

/**
 * ArchivaStartup - the startup of all archiva features in a deterministic order.
 */
public class ArchivaStartup
    implements ServletContextListener
{
    private ThreadedTaskQueueExecutor tqeDbScanning;

    private ThreadedTaskQueueExecutor tqeRepoScanning;

    private ThreadedTaskQueueExecutor tqeIndexing;

    private DefaultRepositoryArchivaTaskScheduler repositoryTaskScheduler;

    @Override
    public void contextInitialized( ServletContextEvent contextEvent )
    {
        WebApplicationContext wac =
            WebApplicationContextUtils.getRequiredWebApplicationContext( contextEvent.getServletContext() );

        SecuritySynchronization securitySync = wac.getBean( SecuritySynchronization.class );

        repositoryTaskScheduler =
            wac.getBean( "archivaTaskScheduler#repository", DefaultRepositoryArchivaTaskScheduler.class );

        Properties archivaRuntimeProperties = wac.getBean( "archivaRuntimeProperties", Properties.class );

        tqeRepoScanning = wac.getBean( "taskQueueExecutor#repository-scanning", ThreadedTaskQueueExecutor.class );

        tqeIndexing = wac.getBean( "taskQueueExecutor#indexing", ThreadedTaskQueueExecutor.class );


        try
        {
            securitySync.startup();
            repositoryTaskScheduler.startup();
            Banner.display( (String) archivaRuntimeProperties.get( "archiva.version" ) );
        }
        catch ( ArchivaException e )
        {
            throw new RuntimeException( "Unable to properly startup archiva: " + e.getMessage(), e );
        }
    }

    @Override
    public void contextDestroyed( ServletContextEvent contextEvent )
    {
        WebApplicationContext applicationContext =
            WebApplicationContextUtils.getRequiredWebApplicationContext( contextEvent.getServletContext() );

        // we log using servlet mechanism as due to some possible problem with slf4j when container shutdown
        // so servletContext.log
        ServletContext servletContext = contextEvent.getServletContext();

        // TODO check this stop

        /*
        if ( applicationContext != null && applicationContext instanceof ClassPathXmlApplicationContext )
        {
            ( (ClassPathXmlApplicationContext) applicationContext ).close();
        } */

        if ( applicationContext != null ) //&& applicationContext instanceof PlexusWebApplicationContext )
        {
            // stop task queue executors
            stopTaskQueueExecutor( tqeDbScanning, servletContext );
            stopTaskQueueExecutor( tqeRepoScanning, servletContext );
            stopTaskQueueExecutor( tqeIndexing, servletContext );

            // stop the DefaultArchivaTaskScheduler and its scheduler
            if ( repositoryTaskScheduler != null )
            {
                try
                {
                    repositoryTaskScheduler.stop();
                }
                catch ( SchedulerException e )
                {
                    servletContext.log( e.getMessage(), e );
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
                    servletContext.log( e.getMessage(), e );
                }
            }

            // close the application context
            //applicationContext.close();
            // TODO fix close call
            //applicationContext.
        }


    }

    private void stopTaskQueueExecutor( ThreadedTaskQueueExecutor taskQueueExecutor, ServletContext servletContext )
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
                ExecutorService service = getExecutorServiceForTTQE( taskQueueExecutor, servletContext );
                if ( service != null )
                {
                    service.shutdown();
                }
            }
            catch ( Exception e )
            {
                servletContext.log( e.getMessage(), e );
            }
        }
    }

    private ExecutorService getExecutorServiceForTTQE( ThreadedTaskQueueExecutor ttqe, ServletContext servletContext )
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
            servletContext.log( e.getMessage(), e );
        }
        return service;
    }
}
