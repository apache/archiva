package org.apache.archiva.webdav.util;
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

import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author Olivier Lamy
 */
@Service
public class MavenIndexerCleaner
    implements ServletContextListener
{
    Logger log = LoggerFactory.getLogger( getClass() );



    @Inject
    private ApplicationContext applicationContext;

    @Inject
    NexusIndexer nexusIndexer;

    @PostConstruct
    public void startup()
    {
        cleanupIndex();
    }

    @PreDestroy
    public void shutdown()
    {
        cleanupIndex();
    }


    @Override
    public void contextInitialized( ServletContextEvent servletContextEvent )
    {
        try
        {
            cleanupIndex();

        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    @Override
    public void contextDestroyed( ServletContextEvent servletContextEvent )
    {
        try
        {
            cleanupIndex();

        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    public void cleanupIndex()
    {
        log.info( "cleanup IndexingContext" );
        try
        {
            for ( IndexingContext context : nexusIndexer.getIndexingContexts().values() )
            {
                nexusIndexer.removeIndexingContext( context, true );
            }
        }
        catch ( Exception e )
        {
            log.warn( "fail to cleanupIndex: {}", e.getMessage(), e );
        }

    }


}
