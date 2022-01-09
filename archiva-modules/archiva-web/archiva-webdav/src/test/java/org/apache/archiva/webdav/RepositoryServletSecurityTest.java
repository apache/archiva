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


import junit.framework.TestCase;
import org.apache.archiva.configuration.provider.ArchivaConfiguration;
import org.apache.archiva.configuration.model.Configuration;
import org.apache.archiva.configuration.model.ManagedRepositoryConfiguration;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.UnauthorizedException;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.redback.system.DefaultSecuritySession;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.memory.SimpleUser;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.webdav.mock.metadata.audit.TestAuditListener;
import org.apache.archiva.repository.base.group.RepositoryGroupHandler;
import org.apache.archiva.security.ServletAuthenticator;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.WebApplicationContext;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


/**
 * RepositoryServletSecurityTest Test the flow of the authentication and authorization checks. This does not necessarily
 * perform redback security checking.
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context-servlet-security-test.xml" } )
public class RepositoryServletSecurityTest
    extends TestCase
{
    protected static final String REPOID_INTERNAL = "internal";

    @Inject
    protected ArchivaConfiguration archivaConfiguration;

    @Inject
    protected RepositoryRegistry repositoryRegistry;

    @SuppressWarnings( "unused" )
    @Inject
    RepositoryGroupHandler repositoryGroupHandler;

    private DavSessionProvider davSessionProvider;

    private ServletAuthenticator servletAuth;

    private HttpAuthenticator httpAuth;

    private RepositoryServlet servlet;

    @Inject
    ApplicationContext applicationContext;
   

    @Rule
    public ArchivaTemporaryFolderRule repoRootInternal = new ArchivaTemporaryFolderRule();

    private AtomicReference<Path> projectBase = new AtomicReference<>( );

    public Path getProjectBase() {
        if (this.projectBase.get()==null) {
            String pathVal = System.getProperty("mvn.project.base.dir");
            Path baseDir;
            if ( StringUtils.isEmpty(pathVal)) {
                baseDir= Paths.get("").toAbsolutePath();
            } else {
                baseDir = Paths.get(pathVal).toAbsolutePath();
            }
            this.projectBase.compareAndSet(null, baseDir);
        }
        return this.projectBase.get();
    }

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        
        super.setUp();

        String appserverBase =
            System.getProperty( "appserver.base", getProjectBase().resolve( "target/appserver-base" ).toAbsolutePath().toString() );

        Path testConf = getProjectBase().resolve( "src/test/resources/repository-archiva.xml" );
        Path testConfDest = Paths.get(appserverBase, "conf/archiva.xml" );
        FileUtils.copyFile( testConf.toFile(), testConfDest.toFile() );
        
        
 
        Configuration config = archivaConfiguration.getConfiguration();
        // clear managed repository
        List<ManagedRepositoryConfiguration> f1 = new ArrayList<>(config.getManagedRepositories());
        for (ManagedRepositoryConfiguration f: f1 ) {
            config.removeManagedRepository(f);
        }
        assertEquals(0,config.getManagedRepositories().size());
        // add internal repo
        config.addManagedRepository(
                createManagedRepository( REPOID_INTERNAL, "Internal Test Repo", repoRootInternal.getRoot() ) );
        
        saveConfiguration( archivaConfiguration );


        servletAuth = mock( ServletAuthenticator.class );

        httpAuth = mock( HttpAuthenticator.class );

        davSessionProvider = new ArchivaDavSessionProvider( servletAuth, httpAuth );

        final MockServletContext mockServletContext = new MockServletContext();

        WebApplicationContext webApplicationContext =
            new AbstractRepositoryServletTestCase.TestWebapplicationContext( applicationContext, mockServletContext );

        mockServletContext.setAttribute( WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
                                         webApplicationContext );

        MockServletConfig mockServletConfig = new MockServletConfig()
        {
            @Override
            public ServletContext getServletContext()
            {
                return mockServletContext;
            }
        };

        servlet = new RepositoryServlet();

        servlet.init( mockServletConfig );
    }

    protected ManagedRepositoryConfiguration createManagedRepository( String id, String name, Path location )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.toAbsolutePath().toString() );
        return repo;
    }

    /*protected void saveConfiguration()
        throws Exception
    {
        saveConfiguration( archivaConfiguration );
    }*/

    protected void saveConfiguration( ArchivaConfiguration archivaConfiguration )
        throws Exception
    {
        repositoryRegistry.reload();
        archivaConfiguration.save( archivaConfiguration.getConfiguration() );        
    }

    /*protected void setupCleanRepo( File repoRootDir )
        throws IOException
    {
    }*/

    @Override
    @After
    public void tearDown()
        throws Exception
    {

       /* if ( repoRootInternal.exists() )
        {
            FileUtils.deleteDirectory( repoRootInternal );
        }*/

        super.tearDown();
        String appBaseProp = System.getProperty( "appserver.base" );
        if (StringUtils.isNotEmpty( appBaseProp )) {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( Paths.get(appBaseProp) );
        }
    }



    // test deploy with invalid user, and guest has no write access to repo
    // 401 must be returned
    @Test
    public void testPutWithInvalidUserAndGuestHasNoWriteAccess()
        throws Exception
    {
        
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        servlet.setDavSessionProvider( davSessionProvider );

        AuthenticationResult result = new AuthenticationResult();

        when( httpAuth.getAuthenticationResult( any(  ),
                                                           any(  ) ) ).thenReturn(
            result );

        when(servletAuth.isAuthenticated( any(  ),
                                     any(  ) )).thenThrow( new AuthenticationException( "Authentication error" ) );

        when(servletAuth.isAuthorized( "guest", "internal", ArchivaRoleConstants.OPERATION_ADD_ARTIFACT ))
            .thenThrow( new UnauthorizedException( "'guest' has no write access to repository" ) );

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "PUT" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/path/to/artifact.jar" );
        mockHttpServletRequest.setContent( IOUtils.toByteArray( is ) );
        mockHttpServletRequest.setContentType( "application/octet-stream" );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );

        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, mockHttpServletResponse.getStatus() );
    }

    // test deploy with invalid user, but guest has write access to repo
    @Test
    public void testPutWithInvalidUserAndGuestHasWriteAccess()
        throws Exception
    {
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        when( httpAuth.getAuthenticationResult( any(  ),
                                                           any(  ) ) ).thenReturn(
            result );


        when( servletAuth.isAuthorized( "guest", "internal",
                                                   ArchivaRoleConstants.OPERATION_ADD_ARTIFACT ) ).thenReturn(
            true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        when( httpAuth.getAuthenticationResult( any( ),
                                                           any( ) ) ).thenReturn(
            result );

        when( httpAuth.getSecuritySession( any( ) ) ).thenReturn( session );

        when( servletAuth.isAuthenticated( any( ),
            any( ) ) ).thenThrow(
            new AuthenticationException( "Authentication error" ) );

        when( httpAuth.getSessionUser( any( ) ) ).thenReturn( null );

        // check if guest has write access
        when( servletAuth.isAuthorized( "guest", "internal",
                                                   ArchivaRoleConstants.OPERATION_ADD_ARTIFACT ) ).thenReturn(
            true );

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

        assertEquals( HttpServletResponse.SC_CREATED, mockHttpServletResponse.getStatus() );
    }

    // test deploy with a valid user with no write access
    @Test
    public void testPutWithValidUserWithNoWriteAccess()
        throws Exception
    {
        
        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );
        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        when( httpAuth.getAuthenticationResult( any( ),
                                                           any( ) ) ).thenReturn(
            result );

        when( servletAuth.isAuthenticated( any( ),
                                                      any( ) ) ).thenReturn( true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        when( httpAuth.getAuthenticationResult( any( ),
                                                           any( ) ) ).thenReturn(
            result );

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

        when( httpAuth.getSecuritySession( mockHttpServletRequest.getSession( true ) ) ).thenReturn(
            session );

        when( httpAuth.getSessionUser( mockHttpServletRequest.getSession() ) ).thenReturn( new SimpleUser() );

        when( servletAuth.isAuthenticated( any( ), eq( result ) ) ).thenReturn(
            true );

        when(
            servletAuth.isAuthorized( any( ), eq( session ), eq( "internal" ),
                                      eq( ArchivaRoleConstants.OPERATION_ADD_ARTIFACT ) ) ).thenThrow(
            new UnauthorizedException( "User not authorized" ) );
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "PUT" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/path/to/artifact.jar" );
        mockHttpServletRequest.setContent( IOUtils.toByteArray( is ) );
        mockHttpServletRequest.setContentType( "application/octet-stream" );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );
        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, mockHttpServletResponse.getStatus() );
    }

    // test deploy with a valid user with write access
    @Test
    public void testPutWithValidUserWithWriteAccess()
        throws Exception
    {
        assertTrue( Files.exists(repoRootInternal.getRoot()) );

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

        when( httpAuth.getAuthenticationResult( any( ),
                                                           any( ) ) ).thenReturn(
            result );

        when( servletAuth.isAuthenticated( any( ),
                                                      any( ) ) ).thenReturn( true );

        User user = new SimpleUser();
        user.setUsername( "admin" );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        when( httpAuth.getAuthenticationResult( any( ),
                                                           any( ) ) ).thenReturn(
            result );

        when( httpAuth.getSecuritySession( mockHttpServletRequest.getSession() ) ).thenReturn( session );

        when( httpAuth.getSessionUser( mockHttpServletRequest.getSession() ) ).thenReturn( user );

        when( servletAuth.isAuthenticated( any( ), eq( result ) ) ).thenReturn(
            true );

        when(
            servletAuth.isAuthorized( any( ), eq( session ), eq( "internal" ),
                                      eq( ArchivaRoleConstants.OPERATION_ADD_ARTIFACT ) ) ).thenReturn( true );

        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "PUT" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/path/to/artifact.jar" );
        mockHttpServletRequest.setContent( IOUtils.toByteArray( is ) );
        mockHttpServletRequest.setContentType( "application/octet-stream" );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );
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

        Path artifactFile = repoRootInternal.getRoot().resolve( commonsLangJar );
        Files.createDirectories(artifactFile.getParent());

        org.apache.archiva.common.utils.FileUtils.writeStringToFile( artifactFile, Charset.defaultCharset() , expectedArtifactContents);

        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        when( httpAuth.getAuthenticationResult( any( ),
                                                           any( ) ) ).thenReturn(
            result );


        when( servletAuth.isAuthorized( "guest", "internal",
                                                   ArchivaRoleConstants.OPERATION_READ_REPOSITORY ) ).thenReturn(
            true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        when( httpAuth.getSecuritySession( any( ) ) ).thenReturn( session );

        when( httpAuth.getSessionUser( any( ) ) ).thenReturn( null );

        when( servletAuth.isAuthenticated( any( ), eq( result ) ) ).thenReturn(
            true );
        when( servletAuth.isAuthenticated( any( ),
            not(eq(result)) ) ).thenThrow(
            new AuthenticationException( "Authentication error" ) );

        when(
            servletAuth.isAuthorized( any( ), eq( session ), eq( "internal" ),
                                      eq( ArchivaRoleConstants.OPERATION_READ_REPOSITORY ) ) ).thenReturn( true );
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "GET" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/" + commonsLangJar );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );
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

        Path artifactFile = repoRootInternal.getRoot().resolve( commonsLangJar );
        Files.createDirectories(artifactFile.getParent());

        org.apache.archiva.common.utils.FileUtils.writeStringToFile( artifactFile, Charset.defaultCharset() , expectedArtifactContents);

        servlet.setDavSessionProvider( davSessionProvider );

        AuthenticationResult result = new AuthenticationResult();

        when( httpAuth.getAuthenticationResult( any( ),
                                                           any( ) ) ).thenReturn(
            result );

        when( servletAuth.isAuthenticated( any( ),
                                                      any( ) ) ).thenThrow(
            new AuthenticationException( "Authentication error" ) );

        when( servletAuth.isAuthorized( "guest", "internal",
                                                   ArchivaRoleConstants.OPERATION_READ_REPOSITORY ) ).thenReturn(
            false );
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "GET" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/" + commonsLangJar );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );
        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, mockHttpServletResponse.getStatus() );
    }

    // test get with valid user with read access to repo
    @Test
    public void testGetWithAValidUserWithReadAccess()
        throws Exception
    {
        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        Path artifactFile = repoRootInternal.getRoot().resolve( commonsLangJar );
        Files.createDirectories(artifactFile.getParent());

        org.apache.archiva.common.utils.FileUtils.writeStringToFile( artifactFile, Charset.defaultCharset() , expectedArtifactContents);

        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        when( httpAuth.getAuthenticationResult( any( ),
                                                           any( ) ) ).thenReturn(
            result );

        when( servletAuth.isAuthenticated( any( ),
                                                      any( ) ) ).thenReturn( true );
        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        when( httpAuth.getAuthenticationResult( any( ),
                                                           any( ) ) ).thenReturn(
            result );

        when( httpAuth.getSecuritySession( any( ) ) ).thenReturn( session );

        when( httpAuth.getSessionUser( any( ) ) ).thenReturn( new SimpleUser() );

        when( servletAuth.isAuthenticated( any( ), eq( result ) ) ).thenReturn(
            true );

        when(
            servletAuth.isAuthorized( any( ), eq( session ), eq( "internal" ),
                                      eq( ArchivaRoleConstants.OPERATION_READ_REPOSITORY ) ) ).thenReturn( true );

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "GET" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/" + commonsLangJar );

        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );
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

        Path artifactFile = repoRootInternal.getRoot().resolve( commonsLangJar );
        Files.createDirectories(artifactFile.getParent());

        org.apache.archiva.common.utils.FileUtils.writeStringToFile( artifactFile, Charset.defaultCharset() , expectedArtifactContents);

        servlet.setDavSessionProvider( davSessionProvider );

        ArchivaDavResourceFactory archivaDavResourceFactory = (ArchivaDavResourceFactory) servlet.getResourceFactory();
        archivaDavResourceFactory.setHttpAuth( httpAuth );
        archivaDavResourceFactory.setServletAuth( servletAuth );

        servlet.setResourceFactory( archivaDavResourceFactory );

        AuthenticationResult result = new AuthenticationResult();

        when( httpAuth.getAuthenticationResult( any( ),
                                                           any( ) ) ).thenReturn(
            result );

        when( servletAuth.isAuthenticated( any( ),
                                                      any( ) ) ).thenReturn( true );

        // ArchivaDavResourceFactory#isAuthorized()
        SecuritySession session = new DefaultSecuritySession();

        when( httpAuth.getAuthenticationResult( any( ),
                                                           any( ) ) ).thenReturn(
            result );

        when( httpAuth.getSecuritySession( any( ) ) ).thenReturn( session );

        when( httpAuth.getSessionUser( any( ) ) ).thenReturn( new SimpleUser() );

        when( servletAuth.isAuthenticated( any( ), eq( result ) ) ).thenReturn(
            true );

        when(
            servletAuth.isAuthorized( any( ), eq( session ), eq( "internal" ),
                                      eq( ArchivaRoleConstants.OPERATION_READ_REPOSITORY ) ) ).thenThrow(
            new UnauthorizedException( "User not authorized to read repository." ) );
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader( "User-Agent", "foo" );
        mockHttpServletRequest.setMethod( "GET" );
        mockHttpServletRequest.setRequestURI( "/repository/internal/" + commonsLangJar );


        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

        servlet.service( mockHttpServletRequest, mockHttpServletResponse );
        assertEquals( HttpServletResponse.SC_UNAUTHORIZED, mockHttpServletResponse.getStatus() );
    }

}
