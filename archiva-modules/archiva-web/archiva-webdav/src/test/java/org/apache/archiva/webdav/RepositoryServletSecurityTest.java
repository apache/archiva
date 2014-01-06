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
import org.apache.catalina.Container;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.ContextLoaderListener;

import javax.inject.Inject;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;

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

    StandardContext context;

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

        tomcat = new Tomcat();
        tomcat.setBaseDir( System.getProperty( "java.io.tmpdir" ) );
        tomcat.setPort( 0 );

        context = StandardContext.class.cast( tomcat.addContext( "", System.getProperty( "java.io.tmpdir" ) ) );

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

        // FIXME use mock to avoid starting Tomcat
        //RepositoryServlet repositoryServlet = new RepositoryServlet();
        //MockServletConfig mockServletConfig = new MockServletConfig();

        //MockServletContext mockServletContext = new MockServletContext(  );
        //mockServletContext

        //repositoryServlet.init( mockServletConfig );

        servlet = RepositoryServlet.class.cast( findServlet( "repository" ) );
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

        if ( this.tomcat != null )
        {
            this.tomcat.stop();
        }

        super.tearDown();
    }

    protected Servlet findServlet( String name )
        throws Exception
    {
        Container[] childs = context.findChildren();
        for ( Container container : childs )
        {
            if ( StringUtils.equals( container.getName(), name ) )
            {
                Tomcat.ExistingStandardWrapper esw = Tomcat.ExistingStandardWrapper.class.cast( container );
                Servlet servlet = esw.loadServlet();

                return servlet;
            }
        }
        return null;
    }

    // test deploy with invalid user, and guest has no write access to repo
    // 401 must be returned
    @Test
    public void testPutWithInvalidUserAndGuestHasNoWriteAccess()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        servlet.setDavSessionProvider( davSessionProvider );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        servletAuth.isAuthenticated( EasyMock.anyObject( HttpServletRequest.class ),
                                     EasyMock.anyObject( AuthenticationResult.class ) );
        EasyMock.expectLastCall().andThrow( new AuthenticationException( "Authentication error" ) );

        servletAuth.isAuthorized( "guest", "internal", ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD );

        EasyMock.expectLastCall().andThrow( new UnauthorizedException( "'guest' has no write access to repository" ) );

        httpAuthControl.replay();
        servletAuthControl.replay();
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "PUT" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/path/to/artifact.jar" );
        mockHttpServletRequest.setContent( IOUtils.toByteArray( is ) );
        mockHttpServletRequest.setContentType( "application/octet-stream" );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, mockHttpServletResponse.getStatus() );
    }

    // test deploy with invalid user, but guest has write access to repo
    @Test
    public void testPutWithInvalidUserAndGuestHasWriteAccess()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ),
                                                      anyObject( AuthenticationResult.class ) ) ).andThrow(
            new AuthenticationException( "Authentication error" ) );

        EasyMock.expect( servletAuth.isAuthorized( "guest", "internal",
                                                   ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD ) ).andReturn(
            true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        EasyMock.expect( httpAuth.getSecuritySession( anyObject( HttpSession.class ) ) ).andReturn( session );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), eq( result ) ) ).andThrow(
            new AuthenticationException( "Authentication error" ) );

        EasyMock.expect( httpAuth.getSessionUser( anyObject( HttpSession.class ) ) ).andReturn( null );

        // check if guest has write access
        EasyMock.expect( servletAuth.isAuthorized( "guest", "internal",
                                                   ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD ) ).andReturn(
            true );

        httpAuthControl.replay();
        servletAuthControl.replay();

        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "PUT" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/path/to/artifact.jar" );
        mockHttpServletRequest.setContent( IOUtils.toByteArray( is ) );
        mockHttpServletRequest.setContentType( "application/octet-stream" );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_CREATED, mockHttpServletResponse.getStatus() );
    }

    // test deploy with a valid user with no write access
    @Test
    public void testPutWithValidUserWithNoWriteAccess()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );
        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ),
                                                      anyObject( AuthenticationResult.class ) ) ).andReturn( true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

        EasyMock.expect( httpAuth.getSecuritySession( mockHttpServletRequest.getSession( true ) ) ).andReturn(
            session );

        EasyMock.expect( httpAuth.getSessionUser( mockHttpServletRequest.getSession() ) ).andReturn( new SimpleUser() );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), eq( result ) ) ).andReturn(
            true );

        EasyMock.expect(
            servletAuth.isAuthorized( anyObject( HttpServletRequest.class ), eq( session ), eq( "internal" ),
                                      eq( ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD ) ) ).andThrow(
            new UnauthorizedException( "User not authorized" ) );
        httpAuthControl.replay();
        servletAuthControl.replay();

        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "PUT" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/path/to/artifact.jar" );
        mockHttpServletRequest.setContent( IOUtils.toByteArray( is ) );
        mockHttpServletRequest.setContentType( "application/octet-stream" );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, mockHttpServletResponse.getStatus() );
    }

    // test deploy with a valid user with write access
    @Test
    public void testPutWithValidUserWithWriteAccess()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );
        assertTrue( repoRootInternal.exists() );

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        String putUrl = "http://machine.com/repository/internal/path/to/artifact.jar";
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        TestAuditListener listener = new TestAuditListener();
        archivaDavResourceFactory.addAuditListener( listener );
        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ),
                                                      anyObject( AuthenticationResult.class ) ) ).andReturn( true );

        User user = new SimpleUser();
        user.setUsername( "admin" );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        EasyMock.expect( httpAuth.getSecuritySession( mockHttpServletRequest.getSession() ) ).andReturn( session );

        EasyMock.expect( httpAuth.getSessionUser( mockHttpServletRequest.getSession() ) ).andReturn( user );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), eq( result ) ) ).andReturn(
            true );

        EasyMock.expect(
            servletAuth.isAuthorized( anyObject( HttpServletRequest.class ), eq( session ), eq( "internal" ),
                                      eq( ArchivaRoleConstants.OPERATION_REPOSITORY_UPLOAD ) ) ).andReturn( true );

        httpAuthControl.replay();
        servletAuthControl.replay();

        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "PUT" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/path/to/artifact.jar" );
        mockHttpServletRequest.setContent( IOUtils.toByteArray( is ) );
        mockHttpServletRequest.setContentType( "application/octet-stream" );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_CREATED, mockHttpServletResponse.getStatus() );

        assertEquals( "admin", listener.getEvents().get( 0 ).getUserId() );
    }

    // test get with invalid user, and guest has read access to repo
    @Test
    public void testGetWithInvalidUserAndGuestHasReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ),
                                                      anyObject( AuthenticationResult.class ) ) ).andThrow(
            new AuthenticationException( "Authentication error" ) );

        EasyMock.expect( servletAuth.isAuthorized( "guest", "internal",
                                                   ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS ) ).andReturn(
            true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        EasyMock.expect( httpAuth.getSecuritySession( anyObject( HttpSession.class ) ) ).andReturn( session );

        EasyMock.expect( httpAuth.getSessionUser( anyObject( HttpSession.class ) ) ).andReturn( null );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), eq( result ) ) ).andReturn(
            true );

        EasyMock.expect(
            servletAuth.isAuthorized( anyObject( HttpServletRequest.class ), eq( session ), eq( "internal" ),
                                      eq( ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS ) ) ).andReturn( true );
        httpAuthControl.replay();
        servletAuthControl.replay();

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "GET" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/" + commonsLangJar );


        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus() );

        assertEquals( "Expected file contents", expectedArtifactContents, mockHttpServletResponse.getContentAsString() );
    }

    // test get with invalid user, and guest has no read access to repo
    @Test
    public void testGetWithInvalidUserAndGuestHasNoReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        servlet.setDavSessionProvider( davSessionProvider );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ),
                                                      anyObject( AuthenticationResult.class ) ) ).andThrow(
            new AuthenticationException( "Authentication error" ) );

        EasyMock.expect( servletAuth.isAuthorized( "guest", "internal",
                                                   ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS ) ).andReturn(
            false );
        httpAuthControl.replay();
        servletAuthControl.replay();

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "GET" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/" + commonsLangJar );


        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, mockHttpServletResponse.getStatus() );
    }

    // test get with valid user with read access to repo
    @Test
    public void testGetWithAValidUserWithReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ),
                                                      anyObject( AuthenticationResult.class ) ) ).andReturn( true );
        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        EasyMock.expect( httpAuth.getSecuritySession( anyObject( HttpSession.class ) ) ).andReturn( session );

        EasyMock.expect( httpAuth.getSessionUser( anyObject( HttpSession.class ) ) ).andReturn( new SimpleUser() );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), eq( result ) ) ).andReturn(
            true );

        EasyMock.expect(
            servletAuth.isAuthorized( anyObject( HttpServletRequest.class ), eq( session ), eq( "internal" ),
                                      eq( ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS ) ) ).andReturn( true );

        httpAuthControl.replay();
        servletAuthControl.replay();

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "GET" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/" + commonsLangJar );


        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_OK, mockHttpServletResponse.getStatus() );
        assertEquals( "Expected file contents", expectedArtifactContents, mockHttpServletResponse.getContentAsString() );
    }

    // test get with valid user with no read access to repo
    @Test
    public void testGetWithAValidUserWithNoReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repoRootInternal, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, Charset.defaultCharset() );

        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ),
                                                      anyObject( AuthenticationResult.class ) ) ).andReturn( true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        EasyMock.expect( httpAuth.getAuthenticationResult( anyObject( HttpServletRequest.class ),
                                                           anyObject( HttpServletResponse.class ) ) ).andReturn(
            result );

        EasyMock.expect( httpAuth.getSecuritySession( anyObject( HttpSession.class ) ) ).andReturn( session );

        EasyMock.expect( httpAuth.getSessionUser( anyObject( HttpSession.class ) ) ).andReturn( new SimpleUser() );

        EasyMock.expect( servletAuth.isAuthenticated( anyObject( HttpServletRequest.class ), eq( result ) ) ).andReturn(
            true );

        EasyMock.expect(
            servletAuth.isAuthorized( anyObject( HttpServletRequest.class ), eq( session ), eq( "internal" ),
                                      eq( ArchivaRoleConstants.OPERATION_REPOSITORY_ACCESS ) ) ).andThrow(
            new UnauthorizedException( "User not authorized to read repository." ) );
        httpAuthControl.replay();
        servletAuthControl.replay();

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "GET" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/" + commonsLangJar );


        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );

        httpAuthControl.verify();
        servletAuthControl.verify();

        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, mockHttpServletResponse.getStatus() );
    }
}
