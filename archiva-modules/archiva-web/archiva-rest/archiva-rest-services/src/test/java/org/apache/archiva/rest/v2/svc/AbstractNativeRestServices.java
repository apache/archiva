package org.apache.archiva.rest.v2.svc;

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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.services.BaseSetup;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Native REST tests do not use the JAX-RS client and can be used with a remote
 * REST API service. The tests
 *
 * @author Martin Schreier <martin_s@apache.org>
 */
@Tag( "rest-native" )
@Tag( "rest-v2" )
public abstract class AbstractNativeRestServices
{
    private AtomicReference<Path> projectDir = new AtomicReference<>();
    private AtomicReference<Path> appServerBase = new AtomicReference<>( );
    private AtomicReference<Path> basePath = new AtomicReference<>( );

    public static final int STOPPED = 0;
    public static final int STOPPING = 1;
    public static final int STARTING = 2;
    public static final int STARTED = 3;
    public static final int ERROR = 4;
    private final boolean startServer;
    private final String serverPort;
    private final String baseUri;

    private RequestSpecification requestSpec;
    protected Logger log = LoggerFactory.getLogger( getClass( ) );

    private static AtomicReference<Server> server = new AtomicReference<>( );
    private static AtomicReference<ServerConnector> serverConnector = new AtomicReference<>( );
    private static AtomicInteger serverStarted = new AtomicInteger( STOPPED );
    private UserManager userManager;
    private RoleManager roleManager;

    private final boolean remoteService;

    private String adminToken;
    private String adminRefreshToken;


    public AbstractNativeRestServices( )
    {
        this.startServer = BaseSetup.startServer( );
        this.serverPort = BaseSetup.getServerPort( );
        this.baseUri = BaseSetup.getBaseUri( );

        if ( startServer )
        {
            this.remoteService = false;
        } else {
            this.remoteService = true;
        }
    }

    protected abstract String getServicePath( );

    protected String getSpringConfigLocation( )
    {
        return "classpath*:META-INF/spring-context.xml,classpath:META-INF/spring-context-native-test.xml";
    }

    protected RequestSpecification getRequestSpec( )
    {
        return this.requestSpec;
    }

    protected String getContextRoot( )
    {
        return "/api";
    }


    protected String getServiceBasePath( )
    {
        return "/v2/archiva";
    }

    protected String getRedbackServiceBasePath( )
    {
        return "/v2/redback";
    }


    protected String getBasePath( )
    {
        return new StringBuilder( )
            .append( getContextRoot( ) )
            .append( getServiceBasePath( ) )
            .append( getServicePath( ) ).toString( );
    }

    /**
     * Returns the server that was started, or null if not initialized before.
     *
     * @return
     */
    public Server getServer( )
    {
        return this.server.get( );
    }

    public int getServerPort( )
    {
        ServerConnector connector = serverConnector.get( );
        if ( connector != null )
        {
            return connector.getLocalPort( );
        }
        else
        {
            return 0;
        }
    }

    /**
     * Returns true, if the server does exist and is running.
     *
     * @return true, if server does exist and is running.
     */
    public boolean isServerRunning( )
    {
        return serverStarted.get( ) == STARTED && this.server.get( ) != null && this.server.get( ).isRunning( );
    }

    private UserManager getUserManager( )
    {
        if ( this.userManager == null )
        {
            UserManager userManager = ContextLoaderListener.getCurrentWebApplicationContext( )
                .getBean( "userManager#default", UserManager.class );
            assertNotNull( userManager );
            this.userManager = userManager;
        }
        return this.userManager;
    }

    private RoleManager getRoleManager( )
    {
        if ( this.roleManager == null )
        {
            RoleManager roleManager = ContextLoaderListener.getCurrentWebApplicationContext( )
                .getBean( "roleManager", RoleManager.class );
            assertNotNull( roleManager );
            this.roleManager = roleManager;
        }
        return this.roleManager;
    }

    protected String getAdminPwd( )
    {
        return BaseSetup.getAdminPwd( );
    }

    protected String getAdminUser( )
    {
        return RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME;
    }

    private void setupAdminUser( ) throws UserManagerException, RoleManagerException
    {

        UserManager um = getUserManager( );

        User adminUser = null;
        try
        {
            adminUser = um.findUser( getAdminUser( ) );
        }
        catch ( UserNotFoundException e )
        {
            // ignore
        }
        adminUser = um.createUser( getAdminUser( ), "Administrator", "admin@local.home" );
        adminUser.setUsername( getAdminUser( ) );
        adminUser.setPassword( getAdminPwd( ) );
        adminUser.setFullName( "the admin user" );
        adminUser.setEmail( "toto@toto.fr" );
        adminUser.setPermanent( true );
        adminUser.setValidated( true );
        adminUser.setLocked( false );
        adminUser.setPasswordChangeRequired( false );
        if ( adminUser == null )
        {
            um.addUser( adminUser );
        }
        else
        {
            um.updateUser( adminUser, false );
        }
        getRoleManager( ).assignRole( "system-administrator", adminUser.getUsername( ) );
    }

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

    Path getAppserverBase() {
        if (appServerBase.get()==null) {
            String basePath = System.getProperty( "appserver.base" );
            final Path appserverPath;
            if (StringUtils.isNotEmpty( basePath )) {
                appserverPath = Paths.get( basePath ).toAbsolutePath( );
            } else {
                appserverPath = getBasedir( ).resolve( "target" ).resolve( "appserver-base-" + LocalTime.now( ).toSecondOfDay( ) );
            }
            appServerBase.compareAndSet( null, appserverPath );
        }
        return appServerBase.get();
    }

    private void removeAppsubFolder( Path appServerBase, String folder )
        throws Exception
    {
        Path directory = appServerBase.resolve( folder );
        if ( Files.exists(directory) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( directory );
        }
    }

    public void startServer( )
        throws Exception
    {
        if ( serverStarted.compareAndSet( STOPPED, STARTING ) )
        {
            try
            {
                log.info( "Starting server" );
                Path appServerBase = getAppserverBase( );

                removeAppsubFolder(appServerBase, "jcr");
                removeAppsubFolder(appServerBase, "conf");
                removeAppsubFolder(appServerBase, "data");


                Server myServer = new Server( );
                this.server.set( myServer );
                this.serverConnector.set( new ServerConnector( myServer, new HttpConnectionFactory( ) ) );
                myServer.addConnector( serverConnector.get( ) );

                ServletHolder servletHolder = new ServletHolder( new CXFServlet( ) );
                ServletContextHandler context = new ServletContextHandler( ServletContextHandler.SESSIONS );
                context.setResourceBase( SystemUtils.JAVA_IO_TMPDIR );
                context.setSessionHandler( new SessionHandler( ) );
                context.addServlet( servletHolder, getContextRoot( ) + "/*" );
                context.setInitParameter( "contextConfigLocation", getSpringConfigLocation( ) );
                context.addEventListener( new ContextLoaderListener( ) );

                getServer( ).setHandler( context );
                getServer( ).start( );

                if ( log.isDebugEnabled( ) )
                {
                    log.debug( "Jetty dump: {}", getServer( ).dump( ) );
                }

                setupAdminUser( );
                log.info( "Started server on port {}", getServerPort( ) );
                serverStarted.set( STARTED );
            }
            finally
            {
                // In case, if the last statement was not reached
                serverStarted.compareAndSet( STARTING, ERROR );
            }
        }

    }

    public void stopServer( )
        throws Exception
    {
        if ( this.serverStarted.compareAndSet( STARTED, STOPPING ) )
        {
            try
            {
                final Server myServer = getServer( );
                if ( myServer != null )
                {
                    log.info( "Stopping server" );
                    myServer.stop( );
                }
                serverStarted.set( STOPPED );
            }
            finally
            {
                serverStarted.compareAndSet( STOPPING, ERROR );
            }
        }
        else
        {
            log.error( "Server is not in STARTED state!" );
        }
    }


    protected void setupNative( ) throws Exception
    {
        if ( this.startServer )
        {
            startServer( );
        }

        if ( StringUtils.isNotEmpty( serverPort ) )
        {
            RestAssured.port = Integer.parseInt( serverPort );
        }
        else
        {
            RestAssured.port = getServerPort( );
        }
        if ( StringUtils.isNotEmpty( baseUri ) )
        {
            RestAssured.baseURI = baseUri;
        }
        else
        {
            RestAssured.baseURI = "http://localhost";
        }
        String basePath = getBasePath( );
        this.requestSpec = getRequestSpecBuilder( ).build( );
        RestAssured.basePath = basePath;
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
            ( cls, charset ) -> {
                ObjectMapper om = new ObjectMapper().findAndRegisterModules();
                om.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                om.setPropertyNamingStrategy( PropertyNamingStrategy.SNAKE_CASE );
                return om;
            }
        ));
    }

    protected RequestSpecBuilder getRequestSpecBuilder( ) {
        return getRequestSpecBuilder( null );
    }

    protected RequestSpecBuilder getRequestSpecBuilder( String basePath )
    {
        String myBasePath = basePath == null ? getBasePath( ) : basePath;
        return new RequestSpecBuilder( ).setBaseUri( baseURI )
            .setPort( port )
            .setBasePath( myBasePath )
            .addHeader( "Origin", RestAssured.baseURI + ":" + RestAssured.port );
    }

    protected RequestSpecBuilder getAuthRequestSpecBuilder( )
    {
        return new RequestSpecBuilder( ).setBaseUri( baseURI )
            .setPort( port )
            .setBasePath( new StringBuilder( )
                .append( getContextRoot( ) )
                .append( getRedbackServiceBasePath() ).append("/auth").toString() )
            .addHeader( "Origin", RestAssured.baseURI + ":" + RestAssured.port );
    }

    protected RequestSpecification getRequestSpec( String bearerToken )
    {
        return getRequestSpecBuilder( ).addHeader( "Authorization", "Bearer " + bearerToken ).build( );
    }

    protected RequestSpecification getRequestSpec( String bearerToken, String path)
    {
        return getRequestSpecBuilder( path  ).addHeader( "Authorization", "Bearer " + bearerToken ).build( );
    }

    protected void shutdownNative( ) throws Exception
    {
        if (startServer)
        {
            stopServer( );
        }
    }

    protected org.apache.archiva.redback.rest.api.model.User addRemoteUser(String userid, String password, String fullName, String mail) {

        return null;
    }

    protected void initAdminToken() {
        Map<String, Object> jsonAsMap = new HashMap<>();
        jsonAsMap.put( "grant_type", "authorization_code" );
        jsonAsMap.put("user_id", getAdminUser());
        jsonAsMap.put("password", getAdminPwd() );
        Response result = given( ).spec( getAuthRequestSpecBuilder().build() )
            .contentType( JSON )
            .body( jsonAsMap )
            .when( ).post( "/authenticate").then( ).statusCode( 200 )
            .extract( ).response( );
        this.adminToken = result.body( ).jsonPath( ).getString( "access_token" );
        this.adminRefreshToken = result.body( ).jsonPath( ).getString( "refresh_token" );
    }

    protected String getUserToken(String userId, String password) {
        Map<String, Object> jsonAsMap = new HashMap<>();
        jsonAsMap.put( "grant_type", "authorization_code" );
        jsonAsMap.put("user_id", userId);
        jsonAsMap.put("password", password );
        Response result = given( ).spec( getAuthRequestSpecBuilder().build() )
            .contentType( JSON )
            .body( jsonAsMap )
            .when( ).post( "/authenticate").then( ).statusCode( 200 )
            .extract( ).response( );
        return result.body( ).jsonPath( ).getString( "access_token" );
    }
    protected String getAdminToken()  {
        if (this.adminToken == null) {
            initAdminToken();
        }
        return this.adminToken;
    }


    protected String getAdminRefreshToken()  {
        if (this.adminRefreshToken == null) {
            initAdminToken();
        }
        return this.adminRefreshToken;
    }

    public boolean isRemoteService() {
        return this.remoteService;
    }
}
