package org.apache.archiva;
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
import org.apache.archiva.rest.api.services.ProxyConnectorService;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.webdav.RepositoryServlet;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.redback.integration.security.role.RedbackRoleConstants;
import org.codehaus.redback.rest.api.services.RoleManagementService;
import org.codehaus.redback.rest.api.services.UserService;
import org.codehaus.redback.rest.services.FakeCreateAdminService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Olivier Lamy
 */
@RunWith( JUnit4.class )
public class DownloadArtifactsTest
    extends TestCase
{

    protected static Logger log = LoggerFactory.getLogger( DownloadArtifactsTest.class );

    public String authorizationHeader = getAdminAuthzHeader();

    public Server server = null;

    public Server redirectServer = null;

    public int port;

    public int redirectPort;

    public static String encode( String uid, String password )
    {
        return "Basic " + Base64Utility.encode( ( uid + ":" + password ).getBytes() );
    }

    public static String getAdminAuthzHeader()
    {
        String adminPwdSysProps = System.getProperty( "rest.admin.pwd" );
        if ( StringUtils.isBlank( adminPwdSysProps ) )
        {
            return encode( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME, FakeCreateAdminService.ADMIN_TEST_PWD );
        }
        return encode( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME, adminPwdSysProps );
    }

    protected String getSpringConfigLocation()
    {
        return "classpath*:META-INF/spring-context.xml classpath*:spring-context-artifacts-download.xml";
    }


    protected String getRestServicesPath()
    {
        return "restServices";
    }

    static String previousAppServerBase;

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

    @Before
    public void startServer()
        throws Exception
    {

        System.setProperty( "redback.admin.creation.file", "target/auto-admin-creation.properties" );
        this.server = new Server( 0 );

        ServletContextHandler context = new ServletContextHandler();

        context.setContextPath( "/" );

        context.setInitParameter( "contextConfigLocation", getSpringConfigLocation() );

        ContextLoaderListener contextLoaderListener = new ContextLoaderListener();

        context.addEventListener( contextLoaderListener );

        ServletHolder sh = new ServletHolder( CXFServlet.class );

        SessionHandler sessionHandler = new SessionHandler();

        context.setSessionHandler( sessionHandler );

        context.addServlet( sh, "/" + getRestServicesPath() + "/*" );

        ServletHolder repoSh = new ServletHolder( RepositoryServlet.class );
        context.addServlet( repoSh, "/repository/*" );

        server.setHandler( context );
        this.server.start();
        Connector connector = this.server.getConnectors()[0];
        this.port = connector.getLocalPort();
        log.info( "start server on port " + this.port );

        //redirect handler

        this.redirectServer = new Server( 0 );
        ServletHolder shRedirect = new ServletHolder( RedirectServlet.class );
        ServletContextHandler contextRedirect = new ServletContextHandler();

        contextRedirect.setContextPath( "/" );
        contextRedirect.addServlet( shRedirect, "/*" );

        redirectServer.setHandler( contextRedirect );
        redirectServer.start();
        this.redirectPort = redirectServer.getConnectors()[0].getLocalPort();
        log.info( "redirect server port {}", redirectPort );

        FakeCreateAdminService fakeCreateAdminService = getFakeCreateAdminService();

        Boolean res = fakeCreateAdminService.createAdminIfNeeded();
        assertTrue( res.booleanValue() );


    }

    @After
    public void tearDown()
        throws Exception
    {
        System.clearProperty( "redback.admin.creation.file" );
        super.tearDown();
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

        URL url = new URL( "http://localhost:" + port + "/repository/internal/junit/junit/4.9/junit-4.9.jar" );
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        //urlConnection.setRequestProperty( "Authorization", authorizationHeader );
        InputStream is = urlConnection.getInputStream();
        File file = new File( "target/junit-4.9.jar" );
        if ( file.exists() )
        {
            file.delete();
        }

        FileWriter fw = new FileWriter( file );
        IOUtil.copy( is, fw );
        // assert jar contains org/junit/runners/JUnit4.class
        ZipFile zipFile = new ZipFile( file );
        ZipEntry zipEntry = zipFile.getEntry( "org/junit/runners/JUnit4.class" );
        assertNotNull( zipEntry );
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

            log.info( "redirect servlet receive: {}", req.getRequestURI() );
            resp.setStatus( 302 );
            resp.getWriter().write( "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n" + "<html><head>\n"
                                        + "<title>302 Found</title>\n" + "</head><body>\n" + "<h1>Found</h1>\n"
                                        + "<p>The document has moved <a href=\"http://repo1.maven.apache.org/maven2/junit/junit/4.9/junit-4.9.jar\">here</a>.</p>\n"
                                        + "</body></html>\n" + "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n"
                                        + "<html><head>\n" );
            resp.sendRedirect( "http://repo1.maven.apache.org/maven2/" + req.getRequestURI() );
        }
    }

    protected ProxyConnectorService getProxyConnectorService()
    {
        ProxyConnectorService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       ProxyConnectorService.class );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000L );
        return service;
    }

    protected RemoteRepositoriesService getRemoteRepositoriesService()
    {
        RemoteRepositoriesService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       RemoteRepositoriesService.class );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000L );
        return service;
    }

    protected String getBaseUrl()
    {
        String baseUrlSysProps = System.getProperty( "archiva.baseRestUrl" );
        return StringUtils.isBlank( baseUrlSysProps ) ? "http://localhost:" + port : baseUrlSysProps;
    }


    protected RoleManagementService getRoleManagementService( String authzHeader )
    {
        RoleManagementService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/redbackServices/",
                                       RoleManagementService.class );

        // for debuging purpose
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 3000000L );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        return service;
    }

    protected UserService getUserService( String authzHeader )
    {
        UserService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/redbackServices/",
                                       UserService.class );

        // for debuging purpose
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 3000000L );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        return service;
    }

    protected FakeCreateAdminService getFakeCreateAdminService()
    {
        return JAXRSClientFactory.create(
            "http://localhost:" + port + "/" + getRestServicesPath() + "/fakeCreateAdminService/",
            FakeCreateAdminService.class );
    }


}
