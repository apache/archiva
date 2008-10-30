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

import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.maven.archiva.common.ArchivaException;
import org.apache.maven.archiva.scheduled.ArchivaTaskScheduler;
import org.codehaus.plexus.spring.PlexusToSpringUtils;
import org.codehaus.plexus.taskqueue.execution.TaskQueueExecutor;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * ArchivaStartup - the startup of all archiva features in a deterministic order. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArchivaStartup
    implements ServletContextListener
{
    public void contextInitialized(ServletContextEvent contextEvent) {
        WebApplicationContext wac =  WebApplicationContextUtils.getRequiredWebApplicationContext(contextEvent.getServletContext());
        
        SecuritySynchronization securitySync = (SecuritySynchronization) wac.getBean(PlexusToSpringUtils.buildSpringId(SecuritySynchronization.class));
        ResolverFactoryInit resolverFactory = (ResolverFactoryInit) wac.getBean(PlexusToSpringUtils.buildSpringId(ResolverFactoryInit.class));
        ArchivaTaskScheduler taskScheduler = (ArchivaTaskScheduler) wac.getBean(PlexusToSpringUtils.buildSpringId(ArchivaTaskScheduler.class));
        TaskQueueExecutor databaseUpdateQueue = (TaskQueueExecutor) wac.getBean(PlexusToSpringUtils.buildSpringId(TaskQueueExecutor.class, "database-update"));
        TaskQueueExecutor repositoryScanningQueue = (TaskQueueExecutor) wac.getBean(PlexusToSpringUtils.buildSpringId(TaskQueueExecutor.class, "repository-scanning"));
        OsgiContainerManager containerManager = getOsgiContainerManager(wac);

        try
        {
            if (!containerManager.isRunning())
            {
                containerManager.start();
            }
            containerManager.start();
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

    public void contextDestroyed(ServletContextEvent contextEvent)
    {
        WebApplicationContext wac =  WebApplicationContextUtils.getRequiredWebApplicationContext(contextEvent.getServletContext());
        OsgiContainerManager containerManager = getOsgiContainerManager(wac);
        if (containerManager.isRunning())
        {
            containerManager.stop();
        }
    }

    private OsgiContainerManager getOsgiContainerManager(ApplicationContext context)
    {
        return (OsgiContainerManager)context.getBean("osgiContainerManager");
    }
}
