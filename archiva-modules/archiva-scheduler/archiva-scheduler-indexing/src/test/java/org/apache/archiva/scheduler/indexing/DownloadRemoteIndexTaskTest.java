package org.apache.archiva.scheduler.indexing;
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

import junit.framework.TestCase;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.utils.FileUtil;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.expr.StringSearchExpression;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author Olivier Lamy
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class DownloadRemoteIndexTaskTest
    extends TestCase
{

    private Server server;

    private int port;

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    RemoteRepositoryAdmin remoteRepositoryAdmin;

    @Inject
    DefaultDownloadRemoteIndexScheduler downloadRemoteIndexScheduler;

    @Inject
    PlexusSisuBridge plexusSisuBridge;

    NexusIndexer nexusIndexer;

    @Before
    public void initialize()
        throws Exception
    {
        super.setUp();
        server = new Server( 0 );
        createContext( server, new File( "src/test/" ) );

        this.server.start();
        Connector connector = this.server.getConnectors()[0];
        this.port = connector.getLocalPort();
        log.info( "start server on port " + this.port );
        nexusIndexer = plexusSisuBridge.lookup( NexusIndexer.class );
    }

    protected void createContext( Server server, File repositoryDirectory )
        throws IOException
    {
        ServletContextHandler context = new ServletContextHandler();
        context.setResourceBase( repositoryDirectory.getAbsolutePath() );
        context.setContextPath( "/" );
        ServletHolder sh = new ServletHolder( DefaultServlet.class );
        context.addServlet( sh, "/" );
        server.setHandler( context );

    }

    @After
    public void tearDown()
        throws Exception
    {
        server.stop();
        super.tearDown();
    }

    @Test
    public void downloadAndMergeRemoteIndexInEmptyIndex()
        throws Exception
    {
        RemoteRepository remoteRepository = getRemoteRepository();

        remoteRepositoryAdmin.addRemoteRepository( remoteRepository, null );

        downloadRemoteIndexScheduler.startup();

        downloadRemoteIndexScheduler.scheduleDownloadRemote( "test-repo", true, true );

        ( (ThreadPoolTaskScheduler) downloadRemoteIndexScheduler.getTaskScheduler() ).getScheduledExecutor().awaitTermination(
            10, TimeUnit.SECONDS );

        remoteRepositoryAdmin.deleteRemoteRepository( "test-repo", null );

        // search
        BooleanQuery iQuery = new BooleanQuery();
        iQuery.add( nexusIndexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( "commons-logging" ) ),
                    BooleanClause.Occur.SHOULD );

        FlatSearchRequest rq = new FlatSearchRequest( iQuery );
        rq.setContexts(
            Arrays.asList( nexusIndexer.getIndexingContexts().get( "remote-" + getRemoteRepository().getId() ) ) );

        FlatSearchResponse response = nexusIndexer.searchFlat( rq );

        log.info( "returned hit count:" + response.getReturnedHitsCount() );
        assertEquals( 8, response.getReturnedHitsCount() );
    }


    protected RemoteRepository getRemoteRepository()
    {
        RemoteRepository remoteRepository = new RemoteRepository();
        File indexDirectory =
            new File( FileUtil.getBasedir(), "target/index/test-" + Long.toString( System.currentTimeMillis() ) );
        indexDirectory.mkdirs();
        indexDirectory.deleteOnExit();

        remoteRepository.setName( "foo" );
        remoteRepository.setIndexDirectory( indexDirectory.getAbsolutePath() );
        remoteRepository.setDownloadRemoteIndex( true );
        remoteRepository.setId( "test-repo" );
        remoteRepository.setUrl( "http://localhost:" + port );
        remoteRepository.setRemoteIndexUrl( "http://localhost:" + port + "/index-updates/" );
        return remoteRepository;
    }

}
