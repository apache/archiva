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

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import junit.framework.TestCase;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.services.RoleManagementService;
import org.apache.archiva.redback.rest.api.services.UserService;
import org.apache.archiva.redback.rest.services.FakeCreateAdminService;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.ProxyConnectorService;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.rest.api.services.RepositoryGroupService;
import org.apache.archiva.rest.api.services.SearchService;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.archiva.webdav.RepositoryServlet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public abstract class AbstractDownloadTest
    extends TestCase
{

    AtomicReference<Path> projectDir = new AtomicReference<>( );
    AtomicReference<Path> basePath = new AtomicReference<>( );

    protected List<Path> createdPaths = new ArrayList<>( );

    protected final Logger log = LoggerFactory.getLogger( getClass() );

    protected static String previousAppServerBase;

    public String authorizationHeader = getAdminAuthzHeader();

    public Server server = null;

    ServerConnector serverConnector;

    public int port;

    protected Path getProjectDirectory() {
        if ( projectDir.get()==null) {
            String propVal = System.getProperty("mvn.project.base.dir");
            Path newVal;
            if (StringUtils.isEmpty(propVal)) {
                newVal = Paths.get("").toAbsolutePath();
            } else {
                newVal = Paths.get(propVal).toAbsolutePath();
            }
            projectDir.compareAndSet(null, newVal);
        }
        return projectDir.get();
    }

    public Path getBasedir()
    {
        if (basePath.get()==null) {
            String baseDir = System.getProperty( "basedir" );
            final Path baseDirPath;
            if (StringUtils.isNotEmpty( baseDir ))  {
                baseDirPath = Paths.get( baseDir );
            } else {
                baseDirPath = getProjectDirectory( );
            }
            basePath.compareAndSet( null, baseDirPath );
        }
        return basePath.get( );
    }



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


    protected abstract String getSpringConfigLocation();


    protected String getRestServicesPath()
    {
        return "restServices";
    }


    @Before
    public void startServer()
        throws Exception
    {

        System.setProperty( "redback.admin.creation.file", "target/auto-admin-creation.properties" );

        server = new Server();
        serverConnector = new ServerConnector( server, new HttpConnectionFactory() );
        server.addConnector( serverConnector );

        ServletHolder servletHolder = new ServletHolder( new CXFServlet() );
        ServletContextHandler context = new ServletContextHandler( ServletContextHandler.SESSIONS );
        context.setResourceBase( SystemUtils.JAVA_IO_TMPDIR );
        context.setSessionHandler( new SessionHandler() );
        context.addServlet( servletHolder, "/" + getRestServicesPath() + "/*" );
        context.setInitParameter( "contextConfigLocation", getSpringConfigLocation() );
        context.addEventListener( new ContextLoaderListener() );

        ServletHolder servletHolderRepo = new ServletHolder( new RepositoryServlet() );
        context.addServlet( servletHolderRepo, "/repository/*" );

        server.setHandler( context );
        server.start();
        port = serverConnector.getLocalPort();
        log.info( "start server on port {}", this.port );

        User user = new User();
        user.setEmail( "toto@toto.fr" );
        user.setFullName( "the root user" );
        user.setUsername( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME );
        user.setPassword( FakeCreateAdminService.ADMIN_TEST_PWD );

        getUserService( null ).createAdminUser( user );


    }


    @After
    @Override
    public void tearDown()
        throws Exception
    {

        for(Path dir : createdPaths) {
            if ( Files.exists( dir)) {
                FileUtils.deleteQuietly( dir.toFile( ) );
            }
        }
        createdPaths.clear();

        System.clearProperty( "redback.admin.creation.file" );
        super.tearDown();
        if ( this.server != null )
        {
            this.server.stop();
        }
    }


    protected ProxyConnectorService getProxyConnectorService()
    {
        ProxyConnectorService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       ProxyConnectorService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.client( service ).header( "Referer", "http://localhost:" + port );

        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000L );
        return service;
    }

    protected RemoteRepositoriesService getRemoteRepositoriesService()
    {
        RemoteRepositoriesService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       RemoteRepositoriesService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.client( service ).header( "Referer", "http://localhost:" + port );

        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000L );
        return service;
    }

    protected ManagedRepositoriesService getManagedRepositoriesService()
    {
        ManagedRepositoriesService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       ManagedRepositoriesService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.client( service ).header( "Referer", "http://localhost:" + port );

        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000L );
        return service;
    }


    protected RepositoryGroupService getRepositoryGroupService()
    {
        RepositoryGroupService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       RepositoryGroupService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.client( service ).header( "Referer", "http://localhost:" + port );

        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000L );
        return service;
    }

    protected RepositoriesService getRepositoriesService()
    {
        RepositoriesService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       RepositoriesService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.client( service ).header( "Referer", "http://localhost:" + port );

        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000L );
        return service;
    }

    protected SearchService getSearchService()
    {
        SearchService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       SearchService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.client( service ).header( "Referer", "http://localhost:" + port );

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
                                       RoleManagementService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client( service ).header( "Referer", "http://localhost:" + port );

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
                                       UserService.class, Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client( service ).header( "Referer", "http://localhost:" + port );

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


    protected List<String> getZipEntriesNames( ZipFile zipFile )
    {
        try
        {
            List<String> entriesNames = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while ( entries.hasMoreElements() )
            {
                entriesNames.add( entries.nextElement().getName() );
            }
            return entriesNames;
        }
        catch ( Throwable e )
        {
            log.info( "fail to get zipEntries {}", e.getMessage(), e );
        }
        return Collections.emptyList();
    }
}
