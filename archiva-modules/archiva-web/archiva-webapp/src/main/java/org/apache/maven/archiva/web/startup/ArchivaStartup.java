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

import java.lang.reflect.Field;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.maven.archiva.common.ArchivaException;
import org.apache.maven.archiva.scheduled.ArchivaTaskScheduler;
import org.apache.maven.archiva.scheduled.DefaultArchivaTaskScheduler;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StoppingException;
import org.codehaus.plexus.scheduler.DefaultScheduler;
import org.codehaus.plexus.spring.PlexusToSpringUtils;
import org.codehaus.plexus.spring.PlexusWebApplicationContext;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.TaskQueueExecutor;
import org.codehaus.plexus.taskqueue.execution.ThreadedTaskQueueExecutor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;

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

    private ArchivaTaskScheduler taskScheduler;
    
    public void contextInitialized( ServletContextEvent contextEvent )
    {
        WebApplicationContext wac =
            WebApplicationContextUtils.getRequiredWebApplicationContext( contextEvent.getServletContext() );

        SecuritySynchronization securitySync =
            (SecuritySynchronization) wac.getBean( PlexusToSpringUtils.buildSpringId( SecuritySynchronization.class ) );
        ResolverFactoryInit resolverFactory =
            (ResolverFactoryInit) wac.getBean( PlexusToSpringUtils.buildSpringId( ResolverFactoryInit.class ) );

        taskScheduler =
            (ArchivaTaskScheduler) wac.getBean( PlexusToSpringUtils.buildSpringId( ArchivaTaskScheduler.class ) );

        tqeDbScanning =
            (ThreadedTaskQueueExecutor) wac.getBean( PlexusToSpringUtils.buildSpringId( TaskQueueExecutor.class,
                                                                                        "database-update" ) );
        tqeRepoScanning =
            (ThreadedTaskQueueExecutor) wac.getBean( PlexusToSpringUtils.buildSpringId( TaskQueueExecutor.class,
                                                                                        "repository-scanning" ) );
        tqeIndexing =
            (ThreadedTaskQueueExecutor) wac.getBean( PlexusToSpringUtils.buildSpringId( TaskQueueExecutor.class,
                                                                                        "indexing" ) );

        try
        {
            securitySync.startup();
            resolverFactory.startup();
            taskScheduler.startup();
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
        if ( applicationContext != null && applicationContext instanceof ClassPathXmlApplicationContext )
        {
            ( (ClassPathXmlApplicationContext) applicationContext ).close();
        }

        if ( applicationContext != null && applicationContext instanceof PlexusWebApplicationContext )
        {
            // stop task queue executors
            stopTaskQueueExecutor( tqeDbScanning );
            stopTaskQueueExecutor( tqeRepoScanning );
            stopTaskQueueExecutor( tqeIndexing );

            // stop the DefaultArchivaTaskScheduler and its scheduler
            if ( taskScheduler != null && taskScheduler instanceof DefaultArchivaTaskScheduler )
            {
                try
                {
                    ( (DefaultArchivaTaskScheduler) taskScheduler ).stop();
                }
                catch ( StoppingException e )
                {
                    e.printStackTrace();
                }
            }

            try
            {
                // shutdown the scheduler, otherwise Quartz scheduler and Threads still exists
                Field schedulerField = taskScheduler.getClass().getDeclaredField( "scheduler" );
                schedulerField.setAccessible( true );

                DefaultScheduler scheduler = (DefaultScheduler) schedulerField.get( taskScheduler );
                scheduler.stop();
            }
            catch ( Exception e )
            {   
                e.printStackTrace();
            }

            // close the application context
            ( (PlexusWebApplicationContext) applicationContext ).close();
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
