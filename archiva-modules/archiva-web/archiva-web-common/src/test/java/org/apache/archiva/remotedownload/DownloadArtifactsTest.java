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
import org.apache.archiva.redback.rest.api.services.RoleManagementService;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.repository.Repository;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class DownloadArtifactsTest
    extends AbstractDownloadTest
{

    protected Logger log = LoggerFactory.getLogger( DownloadArtifactsTest.class );

    public Server redirectServer = null;

    public int redirectPort;

    public Server repoServer = null;

    public int repoServerPort;

    @BeforeClass
    public static void setAppServerBase()
    {
        previousAppServerBase = System.getProperty( "appserver.base" );
        System.setProperty( "appserver.base", "target/" + DownloadArtifactsTest.class.getName() );
    }


    @AfterClass
    public static void resetAppServerBase()
    {
        System.setProperty( "appserver.base", previousAppServerBase );
    }

    @Override
    protected String getSpringConfigLocation()
    {
        return "classpath*:META-INF/spring-context.xml classpath*:spring-context-test-common.xml classpath*:spring-context-artifacts-download.xml";
    }

    @Override

    @Before
    public void startServer()
        throws Exception
    {
        super.startServer();

        // repo handler

        this.repoServer = new Server( 0 );

        ServletHolder shRepo = new ServletHolder( RepoServlet.class );
        ServletContextHandler contextRepo = new ServletContextHandler();

        contextRepo.setContextPath( "/" );
        contextRepo.addServlet( shRepo, "/*" );

        repoServer.setHandler( contextRepo );
        repoServer.start();
        this.repoServerPort = repoServer.getConnectors()[0].getLocalPort();

        //redirect handler

        this.redirectServer = new Server( 0 );
        ServletHolder shRedirect = new ServletHolder( RedirectServlet.class );
        ServletContextHandler contextRedirect = new ServletContextHandler();
        contextRedirect.setAttribute( "redirectToPort", Integer.toString( this.repoServerPort ) );

        contextRedirect.setContextPath( "/" );
        contextRedirect.addServlet( shRedirect, "/*" );

        redirectServer.setHandler( contextRedirect );
        redirectServer.start();
        this.redirectPort = redirectServer.getConnectors()[0].getLocalPort();
        log.info( "redirect server port {}", redirectPort );

    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
        if ( this.redirectServer != null )
        {
            this.redirectServer.stop();
        }
    }

    @Test
    public void downloadWithRemoteRedirect()
        throws Exception
    {
        RemoteRepository remoteRepository = getRemoteRepositoriesService().getRemoteRepository( "central" );
        remoteRepository.setUrl( "http://localhost:" + redirectPort );
        getRemoteRepositoriesService().updateRemoteRepository( remoteRepository );

        RoleManagementService roleManagementService = getRoleManagementService( authorizationHeader );

        if ( !roleManagementService.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER,
                                                         "internal" ) )
        {
            roleManagementService.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, "internal" );
        }

        getUserService( authorizationHeader ).createGuestUser();
        roleManagementService.assignRole( ArchivaRoleConstants.TEMPLATE_GUEST, "guest" );

        roleManagementService.assignTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, "internal",
                                                   "guest" );

        getUserService( authorizationHeader ).removeFromCache( "guest" );

        File file = new File( "target/junit-4.9.jar" );
        if ( file.exists() )
        {
            file.delete();
        }

        HttpWagon httpWagon = new HttpWagon();
        httpWagon.connect( new Repository( "foo", "http://localhost:" + port ) );

        httpWagon.get( "/repository/internal/junit/junit/4.9/junit-4.9.jar", file );

        ZipFile zipFile = new ZipFile( file );
        List<String> entries = getZipEntriesNames( zipFile );
        ZipEntry zipEntry = zipFile.getEntry( "org/junit/runners/JUnit4.class" );
        assertNotNull( "cannot find zipEntry org/junit/runners/JUnit4.class, entries: " + entries + ", content is: "
                           + FileUtils.readFileToString( file ), zipEntry );
        zipFile.close();
        file.deleteOnExit();
    }


    public static class RedirectServlet
        extends HttpServlet
    {
        @Override
        protected void doGet( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
        {

            LoggerFactory.getLogger( getClass() ).info( "redirect servlet receive: {}", req.getRequestURI() );
            resp.setStatus( 302 );
            resp.getWriter().write( "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n" + "<html><head>\n"
                                        + "<title>302 Found</title>\n" + "</head><body>\n" + "<h1>Found</h1>\n"
                                        + "<p>The document has moved <a href=\"https://repo.maven.apache.org/maven2/junit/junit/4.9/junit-4.9.jar\">here</a>.</p>\n"
                                        + "</body></html>\n" + "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n"
                                        + "<html><head>\n" );
            resp.sendRedirect( "http://localhost:" + getServletContext().getAttribute( "redirectToPort" ) + "/maven2/"
                                   + req.getRequestURI() );
        }
    }

    public static class RepoServlet
        extends HttpServlet
    {
        @Override
        protected void doGet( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
        {
            File jar = new File( System.getProperty( "basedir" ), "src/test/junit-4.9.jar" );
            Files.copy( jar.toPath(), resp.getOutputStream() );

        }
    }


}
