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

import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.indexer.ArchivaIndexManager;
import org.apache.archiva.indexer.IndexCreationFailedException;
import org.apache.archiva.repository.EditableRemoteRepository;
import org.apache.archiva.repository.EditableRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryProvider;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.UnsupportedURIException;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.RemoteIndexFeature;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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
    DefaultDownloadRemoteIndexScheduler downloadRemoteIndexScheduler;

    @Inject
    Indexer indexer;

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Inject
    RepositoryProvider repositoryProvider;

    @Before
    public void initialize()
        throws Exception
    {
        Path cfgFile = Paths.get("target/appserver-base/conf/archiva.xml");
        if (Files.exists(cfgFile)) {
            Files.delete(cfgFile);
        }
        try {
            repositoryRegistry.removeRepository( "test-repo-re" );
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
        Path repoDirectory = Paths.get( FileUtils.getBasedir( ), "target/repo-" + Long.toString( System.currentTimeMillis( ) ) );

        RemoteRepository remoteRepository = getRemoteRepository(repoDirectory);

        repositoryRegistry.putRepository( remoteRepository);
        repositoryRegistry.reload();

        downloadRemoteIndexScheduler.startup();

        downloadRemoteIndexScheduler.scheduleDownloadRemote( "test-repo-re", true, true );

        ( (ThreadPoolTaskScheduler) downloadRemoteIndexScheduler.getTaskScheduler() ).getScheduledExecutor().awaitTermination(
            10, TimeUnit.SECONDS );

        repositoryRegistry.removeRepository( "test-repo-re" );

        // search
        BooleanQuery.Builder iQuery = new BooleanQuery.Builder();
        iQuery.add( indexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( "commons-logging" ) ),
                    BooleanClause.Occur.SHOULD );

        remoteRepository = getRemoteRepository( repoDirectory );
        FlatSearchRequest rq = new FlatSearchRequest( iQuery.build() );
        rq.setContexts(
            Arrays.asList( remoteRepository.getIndexingContext().getBaseContext(IndexingContext.class) ) );

        FlatSearchResponse response = indexer.searchFlat(rq);

        log.info( "returned hit count:{}", response.getReturnedHitsCount() );
        Assertions.assertThat( response.getReturnedHitsCount() ).isEqualTo( 8 );
    }


    protected RemoteRepository getRemoteRepository(Path repoDirectory) throws IOException, URISyntaxException, UnsupportedURIException, RepositoryException
    {

        EditableRemoteRepository remoteRepository = repositoryProvider.createRemoteInstance( "test-repo-re", "foo" );
        Path indexDirectory = repoDirectory.resolve( "index" );
        Files.createDirectories( indexDirectory );
        remoteRepository.setLocation( new URI( "http://localhost:" + port ) );
        repoDirectory.toFile().deleteOnExit();
        createIndexingContext( remoteRepository );

        RemoteIndexFeature rif = remoteRepository.getFeature( RemoteIndexFeature.class ).get();
        rif.setDownloadRemoteIndex( true );
        rif.setIndexUri( new URI("http://localhost:" + port + "/index-updates/" ) );
        IndexCreationFeature icf = remoteRepository.getFeature( IndexCreationFeature.class ).get( );
        icf.setLocalIndexPath( remoteRepository.getAsset(  "index" ) );
        return remoteRepository;
    }

    private void createIndexingContext( EditableRepository editableRepo) throws RepositoryException
    {
        if (editableRepo.supportsFeature(IndexCreationFeature.class)) {
            ArchivaIndexManager idxManager = getIndexManager(editableRepo.getType());
            try {
                editableRepo.setIndexingContext(idxManager.createContext(editableRepo));
                idxManager.updateLocalIndexPath(editableRepo);
            } catch ( IndexCreationFailedException e) {
                throw new RepositoryException("Could not create index for repository " + editableRepo.getId() + ": " + e.getMessage(), e);
            }
        }
    }

    public ArchivaIndexManager getIndexManager( RepositoryType type ) {
        return repositoryRegistry.getIndexManager( type );
    }
}
