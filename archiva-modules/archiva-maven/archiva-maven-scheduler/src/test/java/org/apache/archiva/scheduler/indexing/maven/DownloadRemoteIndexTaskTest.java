package org.apache.archiva.scheduler.indexing.maven;
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

import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.StringSearchExpression;
import org.apache.maven.index_shaded.lucene.search.BooleanClause;
import org.apache.maven.index_shaded.lucene.search.BooleanQuery;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
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

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class DownloadRemoteIndexTaskTest
{

    private Server server;
    private ServerConnector serverConnector;

    private int port;

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    RemoteRepositoryAdmin remoteRepositoryAdmin;

    @Inject
    DefaultDownloadRemoteIndexScheduler downloadRemoteIndexScheduler;

    @Inject
    Indexer indexer;

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Before
    public void initialize()
        throws Exception
    {
        Path cfgFile = Paths.get("target/appserver-base/conf/archiva.xml");
        if (Files.exists(cfgFile)) {
            Files.delete(cfgFile);
        }
        try {
            remoteRepositoryAdmin.deleteRemoteRepository("test-repo-re", null);
        } catch (Exception e) {
            // Ignore
        }
        server = new Server( );
        serverConnector = new ServerConnector( server, new HttpConnectionFactory());
        server.addConnector( serverConnector );
        createContext( server, Paths.get( "src/test/" ) );
        this.server.start();
        this.port = serverConnector.getLocalPort();
        log.info( "start server on port {}", this.port );
    }

    protected void createContext( Server server, Path repositoryDirectory )
        throws IOException
    {
        ServletContextHandler context = new ServletContextHandler();
        context.setResourceBase( repositoryDirectory.toAbsolutePath().toString() );
        context.setContextPath( "/" );
        ServletHolder sh = new ServletHolder( DefaultServlet.class );
        context.addServlet( sh, "/" );
        server.setHandler( context );

    }

    @After
    public void tearDown()
        throws Exception
    {
        if (server!=null) {
            server.stop();
        }
        Path cfgFile = Paths.get("target/appserver-base/conf/archiva.xml");
        if (Files.exists(cfgFile)) {
            Files.delete(cfgFile);
        }
    }

    @Test
    public void downloadAndMergeRemoteIndexInEmptyIndex()
        throws Exception
    {
        RemoteRepository remoteRepository = getRemoteRepository();

        remoteRepositoryAdmin.addRemoteRepository( remoteRepository, null );

        downloadRemoteIndexScheduler.startup();

        downloadRemoteIndexScheduler.scheduleDownloadRemote( "test-repo-re", true, true );

        ( (ThreadPoolTaskScheduler) downloadRemoteIndexScheduler.getTaskScheduler() ).getScheduledExecutor().awaitTermination(
            10, TimeUnit.SECONDS );

        remoteRepositoryAdmin.deleteRemoteRepository( "test-repo-re", null );

        // search
        BooleanQuery.Builder iQuery = new BooleanQuery.Builder();
        iQuery.add( indexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( "commons-logging" ) ),
                    BooleanClause.Occur.SHOULD );

        remoteRepositoryAdmin.addRemoteRepository(remoteRepository,  null);
        FlatSearchRequest rq = new FlatSearchRequest( iQuery.build() );
        rq.setContexts(
            Arrays.asList( repositoryRegistry.getRemoteRepository(remoteRepository.getId()).getIndexingContext().getBaseContext(IndexingContext.class) ) );

        FlatSearchResponse response = indexer.searchFlat(rq);

        log.info( "returned hit count:{}", response.getReturnedHitsCount() );
        Assertions.assertThat( response.getReturnedHitsCount() ).isEqualTo( 8 );
    }


    protected RemoteRepository getRemoteRepository() throws IOException
    {
        RemoteRepository remoteRepository = new RemoteRepository( Locale.getDefault());
        Path indexDirectory =
            Paths.get( FileUtils.getBasedir(), "target/index/test-" + Long.toString( System.currentTimeMillis() ) );
        Files.createDirectories( indexDirectory );
        indexDirectory.toFile().deleteOnExit();

        remoteRepository.setName( "foo" );
        remoteRepository.setIndexDirectory( indexDirectory.toAbsolutePath().toString() );
        remoteRepository.setDownloadRemoteIndex( true );
        remoteRepository.setId( "test-repo-re" );
        remoteRepository.setUrl( "http://localhost:" + port );
        remoteRepository.setRemoteIndexUrl( "http://localhost:" + port + "/index-updates/" );

        return remoteRepository;
    }

}
