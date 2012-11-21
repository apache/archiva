package org.apache.archiva.redback.rest.services;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.services.LoginService;
import org.apache.archiva.redback.rest.api.services.RoleManagementService;
import org.apache.archiva.redback.rest.api.services.UserService;
import org.apache.catalina.Context;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;

import javax.ws.rs.core.MediaType;
import java.util.Collections;

/**
 * @author Olivier Lamy
 */
@RunWith( JUnit4.class )
public abstract class AbstractRestServicesTest
    extends TestCase
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    private Tomcat tomcat;

    public int port;

    public String authorizationHeader = getAdminAuthzHeader();


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
        return "classpath*:spring-context.xml,classpath*:META-INF/spring-context.xml";
    }


    protected String getRestServicesPath()
    {
        return "restServices";
    }

    @Before
    public void startServer()
        throws Exception
    {

        tomcat = new Tomcat();
        tomcat.setBaseDir( System.getProperty( "java.io.tmpdir" ) );
        tomcat.setPort( 0 );

        Context context = tomcat.addContext( "", System.getProperty( "java.io.tmpdir" ) );

        ApplicationParameter applicationParameter = new ApplicationParameter();
        applicationParameter.setName( "contextConfigLocation" );
        applicationParameter.setValue( getSpringConfigLocation() );
        context.addApplicationParameter( applicationParameter );

        context.addApplicationListener( ContextLoaderListener.class.getName() );

        Tomcat.addServlet( context, "cxf", new CXFServlet() );
        context.addServletMapping( "/" + getRestServicesPath() + "/*", "cxf" );

        tomcat.start();

        this.port = tomcat.getConnector().getLocalPort();

        log.info( "start server on port " + this.port );

        UserService userService = getUserService();

        User adminUser = new User();
        adminUser.setUsername( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME );
        adminUser.setPassword( FakeCreateAdminServiceImpl.ADMIN_TEST_PWD );
        adminUser.setFullName( "the admin user" );
        adminUser.setEmail( "toto@toto.fr" );
        Boolean res = userService.createAdminUser( adminUser );

        FakeCreateAdminService fakeCreateAdminService = getFakeCreateAdminService();
        //assertTrue( res.booleanValue() );

    }

    protected FakeCreateAdminService getFakeCreateAdminService()
    {
        return JAXRSClientFactory.create(
            "http://localhost:" + port + "/" + getRestServicesPath() + "/fakeCreateAdminService/",
            FakeCreateAdminService.class, Collections.singletonList( new JacksonJaxbJsonProvider() ) );
    }

    @After
    public void stopServer()
        throws Exception
    {
        if ( this.tomcat != null )
        {
            this.tomcat.stop();
        }
    }

    protected UserService getUserService()
    {
        return getUserService( null );
    }

    protected UserService getUserService( String authzHeader )
    {
        UserService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/redbackServices/",
                                       UserService.class, Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        // for debuging purpose
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000 );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );

        return service;
    }

    protected RoleManagementService getRoleManagementService( String authzHeader )
    {
        RoleManagementService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/redbackServices/",
                                       RoleManagementService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        // for debuging purpose
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000 );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }

        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );

        return service;
    }

    protected LoginService getLoginService( String authzHeader )
    {
        LoginService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/redbackServices/",
                                       LoginService.class, Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        // for debuging purpose
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000 );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }

        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );

        return service;
    }

}
