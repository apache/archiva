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
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.ProxyConnectorService;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.rest.api.services.RepositoryGroupService;
import org.apache.archiva.rest.api.services.SearchService;
import org.apache.archiva.webdav.RepositoryServlet;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.codehaus.redback.rest.api.model.User;
import org.codehaus.redback.rest.api.services.RoleManagementService;
import org.codehaus.redback.rest.api.services.UserService;
import org.codehaus.redback.rest.services.FakeCreateAdminService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;

import java.util.Collections;

/**
 * @author Olivier Lamy
 */
public abstract class AbstractDownloadTest
    extends TestCase
{

    protected Logger log = LoggerFactory.getLogger( getClass() );

    static String previousAppServerBase;

    public String authorizationHeader = getAdminAuthzHeader();

    public Server server = null;

    public int port;

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

        User user = new User();
        user.setEmail( "toto@toto.fr" );
        user.setFullName( "the root user" );
        user.setUsername( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME );
        user.setPassword( FakeCreateAdminService.ADMIN_TEST_PWD );

        getUserService( null ).createAdminUser( user );


    }


    @After
    public void tearDown()
        throws Exception
    {
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
