package org.apache.archiva.web.servlet;

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

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.policies.CachedFailuresPolicy;
import org.apache.maven.archiva.policies.ChecksumPolicy;
import org.apache.maven.archiva.policies.ReleasesPolicy;
import org.apache.maven.archiva.policies.SnapshotsPolicy;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHandler;

import java.io.File;

/**
 * AbstractRepositoryServletProxiedTestCase 
 *
 * @version $Id$
 */
public abstract class AbstractRepositoryServletProxiedTestCase
    extends AbstractRepositoryServletTestCase
{
    class RemoteRepoInfo
    {
        public String id;

        public String url;

        public String context;

        public Server server;

        public File root;

        public RemoteRepositoryConfiguration config;
    }

    protected static final long ONE_SECOND = ( 1000 /* milliseconds */ );

    protected static final long ONE_MINUTE = ( ONE_SECOND * 60 );

    protected static final long ONE_HOUR = ( ONE_MINUTE * 60 );

    protected static final long ONE_DAY = ( ONE_HOUR * 24 );

    protected static final long OVER_ONE_HOUR = ( ONE_HOUR + ONE_MINUTE );

    protected static final long OVER_ONE_DAY = ( ONE_DAY + ONE_HOUR );

    protected static final long OLDER = ( -1 );

    protected static final long NEWER = 0;

    protected static final int EXPECT_MANAGED_CONTENTS = 1;

    protected static final int EXPECT_REMOTE_CONTENTS = 2;

    protected static final int EXPECT_NOT_FOUND = 3;

    protected static final boolean HAS_MANAGED_COPY = true;

    protected static final boolean NO_MANAGED_COPY = false;

    protected RemoteRepoInfo remoteCentral;

    protected RemoteRepoInfo remoteSnapshots;
    
    protected RemoteRepoInfo remotePrivateSnapshots;
    
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    protected RemoteRepoInfo createServer( String id )
        throws Exception
    {
        RemoteRepoInfo repo = new RemoteRepoInfo();
        repo.id = id;
        repo.context = "/" + id;
        repo.root = getTestFile( "target/remote-repos/" + id + "/" );

        // Remove exising root contents.
        if ( repo.root.exists() )
        {
            FileUtils.deleteDirectory( repo.root );
        }

        // Establish root directory.
        if ( !repo.root.exists() )
        {
            repo.root.mkdirs();
        }

        repo.server = new Server();
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        repo.server.setHandler( contexts );

        SocketConnector connector = new SocketConnector();
        connector.setPort( 0 ); // 0 means, choose and empty port. (we'll find out which, later)

        repo.server.setConnectors( new Connector[] { connector } );

        ContextHandler context = new ContextHandler();
        context.setContextPath( repo.context );
        context.setResourceBase( repo.root.getAbsolutePath() );
        context.setAttribute( "dirAllowed", true );
        context.setAttribute( "maxCacheSize", 0 );
        ServletHandler servlet = new ServletHandler();
        servlet.addServletWithMapping( DefaultServlet.class.getName(), "/" );
        context.setHandler( servlet );
        contexts.addHandler( context );

        repo.server.start();

        int port = connector.getLocalPort();
        repo.url = "http://localhost:" + port + repo.context;
        System.out.println( "Remote HTTP Server started on " + repo.url );

        repo.config = createRemoteRepository( repo.id, "Testable [" + repo.id + "] Remote Repo", repo.url );

        return repo;
    }

    protected void assertServerSetupCorrectly( RemoteRepoInfo remoteRepo )
        throws Exception
    {
        WebConversation wc = new WebConversation();
        WebResponse response = wc.getResponse( remoteRepo.url );
        assertResponseOK( response );
    }

    private void setupConnector( String repoId, RemoteRepoInfo remoteRepo, String releasesPolicy, String snapshotsPolicy )
    {
        ProxyConnectorConfiguration connector = new ProxyConnectorConfiguration();
        connector.setSourceRepoId( repoId );
        connector.setTargetRepoId( remoteRepo.id );
        connector.addPolicy( ProxyConnectorConfiguration.POLICY_RELEASES, releasesPolicy );
        connector.addPolicy( ProxyConnectorConfiguration.POLICY_SNAPSHOTS, snapshotsPolicy );
        connector.addPolicy( ProxyConnectorConfiguration.POLICY_CHECKSUM, ChecksumPolicy.IGNORE );
        connector.addPolicy( ProxyConnectorConfiguration.POLICY_CACHE_FAILURES, CachedFailuresPolicy.NO );

        archivaConfiguration.getConfiguration().addProxyConnector( connector );
    }

    protected void shutdownServer( RemoteRepoInfo remoteRepo )
    {
        if ( remoteRepo != null )
        {
            if ( remoteRepo.server != null )
            {
                if ( remoteRepo.server.isRunning() )
                {
                    try
                    {
                        remoteRepo.server.stop();
                        // int graceful = remoteRepo.server.getGracefulShutdown();
                        // System.out.println( "server set to graceful shutdown: " + graceful );
                        // remoteRepo = null;
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace( System.err );
                    }
                }
            }
        }
    }

    protected File populateRepo( RemoteRepoInfo remoteRepo, String path, String contents )
        throws Exception
    {
        File destFile = new File( remoteRepo.root, path );
        destFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( destFile, contents, null );
        return destFile;
    }

    protected void setupCentralRemoteRepo()
        throws Exception
    {
        remoteCentral = createServer( "central" );

        assertServerSetupCorrectly( remoteCentral );
        archivaConfiguration.getConfiguration().addRemoteRepository( remoteCentral.config );
        setupCleanRepo( remoteCentral.root );
    }

    protected void setupConnector( String repoId, RemoteRepoInfo remoteRepo )
    {
        setupConnector( repoId, remoteRepo, ReleasesPolicy.ALWAYS, SnapshotsPolicy.ALWAYS );
    }

    protected void setupReleaseConnector( String managedRepoId, RemoteRepoInfo remoteRepo, String releasePolicy )
    {
        setupConnector( managedRepoId, remoteRepo, releasePolicy, SnapshotsPolicy.ALWAYS );
    }

    protected void setupSnapshotConnector( String managedRepoId, RemoteRepoInfo remoteRepo, String snapshotsPolicy )
    {
        setupConnector( managedRepoId, remoteRepo, ReleasesPolicy.ALWAYS, snapshotsPolicy );
    }

    protected void setupSnapshotsRemoteRepo()
        throws Exception
    {
        remoteSnapshots = createServer( "snapshots" );

        assertServerSetupCorrectly( remoteSnapshots );
        archivaConfiguration.getConfiguration().addRemoteRepository( remoteSnapshots.config );
        setupCleanRepo( remoteSnapshots.root );
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        shutdownServer( remoteCentral );
        shutdownServer( remoteSnapshots );
        super.tearDown();
    }
}
