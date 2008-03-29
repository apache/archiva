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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.apache.maven.archiva.common.ArchivaException;
import org.apache.maven.archiva.scheduled.ArchivaTaskScheduler;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.spring.PlexusToSpringUtils;
import org.codehaus.plexus.taskqueue.execution.TaskQueueExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public void contextDestroyed(ServletContextEvent arg0) {
    }

    public void contextInitialized(ServletContextEvent arg0) {
        WebApplicationContext wac =  WebApplicationContextUtils.getRequiredWebApplicationContext(arg0.getServletContext());
        
        SecuritySynchronization securitySync = (SecuritySynchronization) wac.getBean(PlexusToSpringUtils.buildSpringId(SecuritySynchronization.class));
        ResolverFactoryInit resolverFactory = (ResolverFactoryInit) wac.getBean(PlexusToSpringUtils.buildSpringId(ResolverFactoryInit.class));
        ArchivaTaskScheduler taskScheduler = (ArchivaTaskScheduler) wac.getBean(PlexusToSpringUtils.buildSpringId(ArchivaTaskScheduler.class));
        TaskQueueExecutor databaseUpdateQueue = (TaskQueueExecutor) wac.getBean(PlexusToSpringUtils.buildSpringId(TaskQueueExecutor.class, "database-update"));
        TaskQueueExecutor repositoryScanningQueue = (TaskQueueExecutor) wac.getBean(PlexusToSpringUtils.buildSpringId(TaskQueueExecutor.class, "repository-scanning"));
        Banner banner = (Banner) wac.getBean(PlexusToSpringUtils.buildSpringId(Banner.class));

        try
        {
            securitySync.startup();
            resolverFactory.startup();
            taskScheduler.startup();
            banner.display();
        }
        catch ( ArchivaException e )
        {
            throw new RuntimeException( "Unable to properly startup archiva: " + e.getMessage(), e );
        }
    }

}
