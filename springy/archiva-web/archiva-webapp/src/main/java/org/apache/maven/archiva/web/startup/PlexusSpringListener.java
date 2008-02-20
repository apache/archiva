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

import org.apache.maven.archiva.common.spring.PlexusFactory;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.xwork.PlexusLifecycleListener;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class PlexusSpringListener
    implements ServletContextListener
{
    public void contextInitialized( ServletContextEvent event )
    {
        BeanFactory factory = WebApplicationContextUtils.getRequiredWebApplicationContext( event.getServletContext() );

        PlexusContainer container =
            (PlexusContainer) event.getServletContext().getAttribute( PlexusLifecycleListener.KEY );

        container.getContext().put( BeanFactory.class, factory );
        PlexusFactory plexusFactory = (PlexusFactory) factory.getBean( "plexusCacheFactory" );
        plexusFactory.setContainer( container );
    }

    public void contextDestroyed( ServletContextEvent event )
    {
        // This space left intentionally blank
    }
}
