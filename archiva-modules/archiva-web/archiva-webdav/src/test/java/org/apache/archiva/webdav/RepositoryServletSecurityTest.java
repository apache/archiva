package org.apache.archiva.webdav;

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


import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.UnauthorizedException;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.redback.system.DefaultSecuritySession;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.memory.SimpleUser;
import org.apache.archiva.repository.audit.TestAuditListener;
import org.apache.archiva.security.ServletAuthenticator;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.archiva.webdav.util.MavenIndexerCleaner;
import org.apache.catalina.Context;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.*;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.ContextLoaderListener;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * RepositoryServletSecurityTest Test the flow of the authentication and authorization checks. This does not necessarily
 * perform redback security checking.
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class RepositoryServletSecurityTest
    extends TestCase
{
    protected static final String REPOID_INTERNAL = "internal";


    protected File repoRootInternal;

    protected ArchivaConfiguration archivaConfiguration;

    private DavSessionProvider davSessionProvider;

    private IMocksControl servletAuthControl;

    private ServletAuthenticator servletAuth;

    private IMocksControl httpAuthControl;

    private HttpAuthenticator httpAuth;

    private RepositoryServlet servlet;

    protected Tomcat tomcat;

    protected static int port;

    @Inject
    ApplicationContext applicationContext;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        String appserverBase =
            System.getProperty( "appserver.base", new File( "target/appserver-base" ).getAbsolutePath() );

        File testConf = new File( "src/test/resources/repository-archiva.xml" );
        File testConfDest = new File( appserverBase, "conf/archiva.xml" );
        FileUtils.copyFile( testConf, testConfDest );

        repoRootInternal = new File( appserverBase, "data/repositories/internal" );

        archivaConfiguration = applicationContext.getBean( ArchivaConfiguration.class );
        Configuration config = archivaConfiguration.getConfiguration();

        if ( !config.getManagedRepositoriesAsMap().containsKey( REPOID_INTERNAL ) )
        {
            config.addManagedRepository(
                createManagedRepository( REPOID_INTERNAL, "Internal Test Repo", repoRootInternal ) );
        }
        saveConfiguration( archivaConfiguration );

        CacheManager.getInstance().clearAll();

        /*
        sr = new ServletRunner( new File( "src/test/resources/WEB-INF/repository-servlet-security-test/web.xml" ) );
        sr.registerServlet( "/repository/*", RepositoryServlet.class.getName() );
        sc = sr.newClient();
        */


        tomcat = new Tomcat();
        tomcat.setBaseDir( System.getProperty( "java.io.tmpdir" ) );
        tomcat.setPort( 0 );

        Context context = tomcat.addContext( "", System.getProperty( "java.io.tmpdir" ) );

        ApplicationParameter applicationParameter = new ApplicationParameter();
        applicationParameter.setName( "contextConfigLocation" );
        applicationParameter.setValue( getSpringConfigLocation() );
        context.addApplicationParameter( applicationParameter );

        context.addApplicationListener( ContextLoaderListener.class.getName() );

        context.addApplicationListener( MavenIndexerCleaner.class.getName() );

        Tomcat.addServlet( context, "repository", new UnauthenticatedRepositoryServlet() );
        context.addServletMapping( "/repository/*", "repository" );

        tomcat.start();

        this.port = tomcat.getConnector().getLocalPort();


        servletAuthControl = EasyMock.createControl();

        servletAuth = servletAuthControl.createMock( ServletAuthenticator.class );

        httpAuthControl = EasyMock.createControl();

        httpAuth = httpAuthControl.createMock( HttpAuthenticator.class );

        davSessionProvider = new ArchivaDavSessionProvider( servletAuth, httpAuth );
    }

    protected String getSpringConfigLocation()
    {
        return "classpath*:/META-INF/spring-context.xml,classpath*:/spring-context-servlet-security-test.xml";
    }

    protected ManagedRepositoryConfiguration createManagedRepository( String id, String name, File location )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        return repo;
    }

    protected void saveConfiguration()
        throws Exception
    {
        saveConfiguration( archivaConfiguration );
    }

    protected void saveConfiguration( ArchivaConfiguration archivaConfiguration )
        throws Exception
    {
        archivaConfiguration.save( archivaConfiguration.getConfiguration() );
    }

    protected void setupCleanRepo( File repoRootDir )
        throws IOException
    {
        FileUtils.deleteDirectory( repoRootDir );
        if ( !repoRootDir.exists() )
        {
            repoRootDir.mkdirs();
        }
    }

    @Override
    @After
    public void tearDown()
        throws Exception
    {


        if ( repoRootInternal.exists() )
        {
            FileUtils.deleteDirectory( repoRootInternal );
        }

        servlet = null;

        if (this.tomcat != null)
        {
            this.tomcat.stop();
        }

        super.tearDown();
    }

    // test deploy with invalid user, and guest has no write access to repo
    // 401 must be returned
    @Ignore("rewrite")
    public void testPutWithInvalidUserAndGuestHasNoWriteAccess()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        String putUrl = "http://machine.com/repository/internal/path/to/artifact.jar";
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        WebRequest request = new AbstractRepositoryServletTestCase.PutMethodWebRequest( putUrl, is, "application/octet-stream" );
        //InvocationContext ic = sc.newInvocation( request );
        //servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn( result );

        servletAuth.isAuthenticated( EasyMock.anyObject( HttpServletRequest.class ),
                                     EasyMock.anyObject( AuthenticationResult.class ) );
        EasyMock.expectLastCall().andThrow( new AuthenticationException( "Authentication error" ) );

        servletAuth.isAuthorized( "guest", "internal", ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD );

        EasyMock.expectLastCall().andThrow( new UnauthorizedException( "'guest' has no write access to repository" ) );

        httpAuthControl.replay();
        servletAuthControl.replay();

        //servlet.service( ic.getRequest(), ic.getResponse() );

        httpAuthControl.verify();
        servletAuthControl.verify();

        //assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getResponseCode());
    }

    // test deploy with invalid user, but guest has write access to repo
    @Ignore("rewrite")
    public void testPutWithInvalidUserAndGuestHasWriteAccess()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        String putUrl = "http://machine.com/repository/internal/path/to/artifact.jar";
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        WebRequest request = new AbstractRepositoryServletTestCase.PutMethodWebRequest( putUrl, is, "application/octet-stream" );

        //InvocationContext ic = sc.newInvocation( request );
        //servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn( result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ),
                                                      anyObject( AuthenticationResult.class ) ) ).andThrow(
            new AuthenticationException( "Authentication error" ) );

        EasyMock.expect(servletAuth.isAuthorized( "guest", "internal", ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD )).andReturn( true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn( result );

        EasyMock.expect( httpAuth.getSecuritySession( anyObject( HttpSession.class ) ) ).andReturn( session );

        EasyMock.expect(servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), eq(result) )).andThrow( new AuthenticationException( "Authentication error" ) );

        EasyMock.expect( httpAuth.getSessionUser( anyObject( HttpSession.class ) ) ).andReturn( null );

        // check if guest has write access
        EasyMock.expect( servletAuth.isAuthorized( "guest", "internal",
                                                   ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD ) ).andReturn(
            true );

        httpAuthControl.replay();
        servletAuthControl.replay();

        //servlet.service( ic.getRequest(), ic.getResponse() );

        httpAuthControl.verify();
        servletAuthControl.verify();

        // assertEquals( HttpServletResponse.SC_CREATED, response.getResponseCode() );
    }

    // test deploy with a valid user with no write access
    @Ignore("rewrite")
    public void testPutWithValidUserWithNoWriteAccess()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        String putUrl = "http://machine.com/repository/internal/path/to/artifact.jar";
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        WebRequest request = new AbstractRepositoryServletTestCase.PutMethodWebRequest( putUrl, is, "application/octet-stream" );

        //InvocationContext ic = sc.newInvocation( request );
        //servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );
        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn( result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ),
                                                      anyObject( AuthenticationResult.class ) ) ).andReturn( true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn( result );

        //EasyMock.expect( httpAuth.getSecuritySession( ic.getRequest().getSession( true ) ) ).andReturn( session );

        //EasyMock.expect( httpAuth.getSessionUser( ic.getRequest().getSession() ) ).andReturn( new SimpleUser() );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ),
                                                      eq( result ) ) ).andReturn( true );

        EasyMock.expect( servletAuth.isAuthorized( anyObject( HttpServletRequest.class ), eq(session), eq("internal"),
                                                   eq(ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD) ) ).andThrow(
            new UnauthorizedException( "User not authorized" ) );
        httpAuthControl.replay();
        servletAuthControl.replay();

        //servlet.service( ic.getRequest(), ic.getResponse() );

        httpAuthControl.verify();
        servletAuthControl.verify();

        // assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getResponseCode());
    }

    // test deploy with a valid user with write access
    @Ignore("rewrite")
    public void testPutWithValidUserWithWriteAccess()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );
        assertTrue( repoRootInternal.exists() );

        String putUrl = "http://machine.com/repository/internal/path/to/artifact.jar";
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        WebRequest request = new AbstractRepositoryServletTestCase.PutMethodWebRequest( putUrl, is, "application/octet-stream" );

        //InvocationContext ic = sc.newInvocation( request );
        //servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        TestAuditListener listener = new TestAuditListener();
        archivaDavResourceFactory.addAuditListener( listener );
        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class) )).andReturn( result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ),
                                                      anyObject( AuthenticationResult.class ) ) ).andReturn( true );

        User user = new SimpleUser();
        user.setUsername( "admin" );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        EasyMock.expect( httpAuth.getAuthenticationResult(anyObject( HttpServletRequest.class ),
                                                          anyObject( HttpServletResponse.class) ) ).andReturn( result );

        //EasyMock.expect( httpAuth.getSecuritySession( ic.getRequest().getSession( true ) ) ).andReturn( session );

        //EasyMock.expect( httpAuth.getSessionUser( ic.getRequest().getSession() ) ).andReturn( user );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), eq(result) ) ).andReturn(
            true );

        EasyMock.expect( servletAuth.isAuthorized( anyObject( HttpServletRequest.class ), eq(session), eq("internal"),
                                                   eq(ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD) ) ).andReturn(
            true );

        httpAuthControl.replay();
        servletAuthControl.replay();

        //servlet.service( ic.getRequest(), ic.getResponse() );

        httpAuthControl.verify();
        servletAuthControl.verify();

        // assertEquals(HttpServletResponse.SC_CREATED, response.getResponseCode());

        assertEquals( "admin", listener.getEvents().get( 0 ).getUserId() );
    }

    // test get with invalid user, and guest has read access to repo
    @Ignore("rewrite")
    public void testGetWithInvalidUserAndGuestHasReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        WebRequest request = new AbstractRepositoryServletTestCase.GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        //InvocationContext ic = sc.newInvocation( request );
        //servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ), anyObject( HttpServletResponse.class ) ) )
            .andReturn( result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), anyObject( AuthenticationResult.class ) ) ).andThrow(
            new AuthenticationException( "Authentication error" ) );

        EasyMock.expect( servletAuth.isAuthorized( "guest", "internal",
                                                   ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS ) ).andReturn(
            true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ), anyObject( HttpServletResponse.class ) ) ).andReturn( result );

        EasyMock.expect( httpAuth.getSecuritySession( anyObject( HttpSession.class ) ) ).andReturn( session );

        EasyMock.expect( httpAuth.getSessionUser( anyObject( HttpSession.class ) ) ).andReturn( null );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), eq(result) ) ).andReturn(
            true );

        EasyMock.expect( servletAuth.isAuthorized( anyObject( HttpServletRequest.class ), eq(session), eq("internal"),
                                                   eq(ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS) ) ).andReturn( true );
        httpAuthControl.replay();
        servletAuthControl.replay();

        WebResponse response = null;// sc.getResponse( request );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_OK, response.getStatusCode() );
        assertEquals( "Expected file contents", expectedArtifactContents, response.getContentAsString() );
    }

    // test get with invalid user, and guest has no read access to repo
    @Ignore("rewrite")
    public void testGetWithInvalidUserAndGuestHasNoReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        WebRequest request = new AbstractRepositoryServletTestCase.GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        //InvocationContext ic = sc.newInvocation( request );
        //servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ), anyObject( HttpServletResponse.class ) ) ).andReturn( result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), anyObject( AuthenticationResult.class ) ) ).andThrow(
            new AuthenticationException( "Authentication error" ) );

        EasyMock.expect( servletAuth.isAuthorized( "guest", "internal",
                                                   ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS ) ).andReturn(
            false );
        httpAuthControl.replay();
        servletAuthControl.replay();

        WebResponse response = null;//sc.getResponse( request );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, response.getStatusCode() );
    }

    // test get with valid user with read access to repo
    @Ignore("rewrite")
    public void testGetWithAValidUserWithReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        WebRequest request = new AbstractRepositoryServletTestCase.GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        //InvocationContext ic = sc.newInvocation( request );
        //servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ), anyObject( HttpServletResponse.class ) ) ).andReturn( result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), anyObject( AuthenticationResult.class ) ) ).andReturn( true );
        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ), anyObject( HttpServletResponse.class ) ) ).andReturn( result );

        EasyMock.expect( httpAuth.getSecuritySession( anyObject( HttpSession.class ) ) ).andReturn( session );

        EasyMock.expect( httpAuth.getSessionUser( anyObject( HttpSession.class ) ) ).andReturn( new SimpleUser() );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), eq(result) ) ).andReturn(
            true );

        EasyMock.expect( servletAuth.isAuthorized( anyObject( HttpServletRequest.class ), eq(session), eq("internal"),
                                                   eq(ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS) ) ).andReturn(
            true );

        httpAuthControl.replay();
        servletAuthControl.replay();

        WebResponse response = null;// sc.getResponse( request );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_OK, response.getStatusCode() );
        assertEquals( "Expected file contents", expectedArtifactContents, response.getContentAsString() );
    }

    // test get with valid user with no read access to repo
    @Ignore("rewrite")
    public void testGetWithAValidUserWithNoReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        WebRequest request = new AbstractRepositoryServletTestCase.GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        //InvocationContext ic = sc.newInvocation( request );
        //servlet = (RepositoryServlet) ic.getServlet();
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ), anyObject( HttpServletResponse.class ) ) ).andReturn( result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), anyObject( AuthenticationResult.class ) ) ).andReturn( true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ), anyObject( HttpServletResponse.class ) ) ).andReturn( result );

        EasyMock.expect( httpAuth.getSecuritySession( anyObject( HttpSession.class) ) ).andReturn( session );

        EasyMock.expect( httpAuth.getSessionUser( anyObject( HttpSession.class) ) ).andReturn( new SimpleUser() );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), eq(result) ) ).andReturn(
            true );

        EasyMock.expect( servletAuth.isAuthorized( anyObject( HttpServletRequest.class ), eq(session), eq("internal"),
                                                   eq(ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS) ) ).andThrow(
            new UnauthorizedException( "User not authorized to read repository." ) );
        httpAuthControl.replay();
        servletAuthControl.replay();

        WebResponse response = null;//sc.getResponse( request );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, response.getStatusCode() );
    }
}
