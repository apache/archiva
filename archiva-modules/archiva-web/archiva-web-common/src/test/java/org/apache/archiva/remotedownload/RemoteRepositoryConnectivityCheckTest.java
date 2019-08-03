package org.apache.archiva.remotedownload;

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
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.commons.io.FileUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
public class RemoteRepositoryConnectivityCheckTest
    extends AbstractDownloadTest
{

    private static Path appServerBase;

    @BeforeClass
    public static void setAppServerBase()
        throws IOException
    {
        previousAppServerBase = System.getProperty( "appserver.base" );
        appServerBase = Files.createTempDirectory( "archiva-common-web_appsrv6_" ).toAbsolutePath( );
        System.setProperty( "appserver.base", appServerBase.toString( ) );
    }

    @AfterClass
    public static void resetAppServerBase()
    {
        if (Files.exists(appServerBase)) {
            FileUtils.deleteQuietly( appServerBase.toFile() );
        }
        System.setProperty( "appserver.base", previousAppServerBase );
    }

    @Override
    protected String getSpringConfigLocation()
    {
        System.out.println( "AppserverBase: " + System.getProperty( "appserver.base" ) );
        return "classpath*:META-INF/spring-context.xml classpath*:spring-context-test-common.xml classpath*:spring-context-artifacts-download.xml";
    }

    @Test
    public void checkRemoteConnectivity()
        throws Exception
    {
        String id = Long.toString( System.currentTimeMillis() );

        Path srcRep = getProjectDirectory( ).resolve( "src/test/repositories/test-repo" );
        Path testRep = getBasedir( ).resolve( "target" ).resolve( "test-repo-" + id ).toAbsolutePath();
        FileUtils.copyDirectory( srcRep.toFile( ), testRep.toFile( ) );
        createdPaths.add( testRep );


        Server repoServer =
            buildStaticServer( testRep );

        ServerConnector serverConnector = new ServerConnector( repoServer, new HttpConnectionFactory());
        repoServer.addConnector( serverConnector );
        repoServer.start();

        RemoteRepositoriesService service = getRemoteRepositoriesService();

        WebClient.client( service ).header( "Authorization", authorizationHeader );

        try
        {

            int repoServerPort = serverConnector.getLocalPort();

            RemoteRepository repo = getRemoteRepository();

            repo.setUrl( "http://localhost:" + repoServerPort );

            service.addRemoteRepository( repo );

            assertThat( service.checkRemoteConnectivity( repo.getId() ) ).isTrue();
        }
        finally
        {
            service.deleteRemoteRepository( "id-new" );
            repoServer.stop();
        }
    }

    @Test
    public void checkRemoteConnectivityEmptyRemote()
        throws Exception
    {

        Path tmpDir = Files.createTempDirectory( "test" );
        Server repoServer = buildStaticServer( tmpDir );
        ServerConnector serverConnector = new ServerConnector( repoServer, new HttpConnectionFactory());
        repoServer.addConnector( serverConnector );
        repoServer.start();

        RemoteRepositoriesService service = getRemoteRepositoriesService();

        WebClient.client( service ).header( "Authorization", authorizationHeader );

        try
        {

            int repoServerPort = serverConnector.getLocalPort();

            RemoteRepository repo = getRemoteRepository();

            repo.setUrl( "http://localhost:" + repoServerPort );

            service.addRemoteRepository( repo );

            assertThat( service.checkRemoteConnectivity( repo.getId() ) ).isTrue();
        }
        finally
        {
            service.deleteRemoteRepository( "id-new" );
            org.apache.archiva.common.utils.FileUtils.deleteQuietly( tmpDir );
            repoServer.stop();
        }
    }

    @Test
    public void checkRemoteConnectivityFail()
        throws Exception
    {

        RemoteRepositoriesService service = getRemoteRepositoriesService();

        WebClient.client( service ).header( "Authorization", authorizationHeader );

        try
        {

            RemoteRepository repo = getRemoteRepository();

            repo.setUrl( "http://localhost:8956" );

            service.addRemoteRepository( repo );

            assertThat( service.checkRemoteConnectivity( repo.getId() ) ).isFalse();
        }
        finally
        {
            service.deleteRemoteRepository( "id-new" );

        }
    }

    protected Server buildStaticServer( Path path )
    {
        Server repoServer = new Server(  );

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed( true );
        resourceHandler.setWelcomeFiles( new String[]{ "index.html" } );
        resourceHandler.setResourceBase( path.toAbsolutePath().toString() );

        HandlerList handlers = new HandlerList();
        handlers.setHandlers( new Handler[]{ resourceHandler, new DefaultHandler() } );
        repoServer.setHandler( handlers );

        return repoServer;
    }


    RemoteRepository getRemoteRepository()
    {
        return new RemoteRepository( Locale.getDefault( ), "id-new", "new one", "http://foo.com", "default", "foo", "foopassword", 120,
                                     "cool repo" );
    }

}
