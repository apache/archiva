package org.apache.archiva.rest.services;
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
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.services.AbstractRestServicesTest;
import org.apache.archiva.rest.api.services.ArchivaAdministrationService;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.archiva.rest.api.services.CommonServices;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.MergeRepositoriesService;
import org.apache.archiva.rest.api.services.NetworkProxyService;
import org.apache.archiva.rest.api.services.PingService;
import org.apache.archiva.rest.api.services.PluginsService;
import org.apache.archiva.rest.api.services.ProxyConnectorRuleService;
import org.apache.archiva.rest.api.services.ProxyConnectorService;
import org.apache.archiva.rest.api.services.RedbackRuntimeConfigurationService;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.rest.api.services.RepositoryGroupService;
import org.apache.archiva.rest.api.services.SearchService;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author Olivier Lamy
 */
@RunWith(ArchivaBlockJUnit4ClassRunner.class)
public abstract class AbstractArchivaRestTest
    extends AbstractRestServicesTest
{
    private AtomicReference<Path> projectDir = new AtomicReference<>();
    private AtomicReference<Path> appServerBase = new AtomicReference<>( );
    private AtomicReference<Path> basePath = new AtomicReference<>( );

    private boolean reuseServer = true;


    protected void setReuseServer(boolean value) {
        this.reuseServer = value;
    }

    protected boolean isReuseServer() {
        return this.reuseServer;
    }

    /*
     * Used by tryAssert to allow to throw exceptions in the lambda expression.
     */
    @FunctionalInterface
    protected interface AssertFunction
    {
        void accept( ) throws Exception;
    }

    protected void tryAssert( AssertFunction func ) throws Exception
    {
        tryAssert( func, 10, 500 );
    }

    /*
     * Runs the assert method until the assert is successful or the number of retries
     * is reached. This is needed because the JCR Oak index update is asynchronous, so updates
     * may not be visible immediately after the modification.
     */
    private void tryAssert( AssertFunction func, int retries, int sleepMillis ) throws Exception
    {
        Throwable t = null;
        int retry = retries;
        while ( retry-- > 0 )
        {
            try
            {
                func.accept( );
                return;
            }
            catch ( Exception | AssertionError e )
            {
                t = e;
                Thread.currentThread( ).sleep( sleepMillis );
                log.warn( "Retrying assert {}: {}", retry, e.getMessage( ) );
            }
        }
        log.warn( "Retries: {}, Exception: {}", retry, t.getMessage( ) );
        if ( retry <= 0 && t != null )
        {
            if ( t instanceof RuntimeException )
            {
                throw (RuntimeException) t;
            }
            else if ( t instanceof Exception )
            {
                throw (Exception) t;
            }
            else if ( t instanceof Error )
            {
                throw (Error) t;
            }
        }
    }

    // START SNIPPET: authz-header
    // guest with an empty password
    public static String guestAuthzHeader =
        "Basic " + org.apache.cxf.common.util.Base64Utility.encode( ( "guest" + ":" ).getBytes() );

    // with an other login/password
    //public String authzHeader =
    //    "Basic " + org.apache.cxf.common.util.Base64Utility.encode( ( "login" + ":password" ).getBytes() );

    // END SNIPPET: authz-header

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

    @BeforeClass
    public static void checkRepo()
    {
        Assume.assumeFalse("Test is ignored, because path to appserver contains whitespace characters!", System.getProperty( "appserver.base" ).contains( " " ) );
        // skygo: was not possible to fix path in this particular module
        // Skip test if not in proper folder , otherwise test are not fair coz repository
        // cannot have space in their name.
    }

    @Override
    @Before
    public void startServer()
        throws Exception
    {
        if ( (!isReuseServer()) || (isReuseServer() && !isServerRunning())) {
            log.info("Starting new server reuse={}, running={}, instance={}, server={}", isReuseServer(), isServerRunning(), this.hashCode(), getServer()==null ? "" : getServer().hashCode());
            Path appServerBase = getAppserverBase( );

            removeAppsubFolder(appServerBase, "jcr");
            removeAppsubFolder(appServerBase, "conf");
            removeAppsubFolder(appServerBase, "data");
            super.startServer();
        } else {
            log.info("Reusing running server instance reuse={}, running={}", isReuseServer(), isServerRunning());
        }
    }

    @Override
    @After
    public void stopServer()
            throws Exception
    {
        if ( !isReuseServer() )
        {
            log.info("Stopping server reuse={}, running={}, instance={}, server={}", isReuseServer(), isServerRunning(), this.hashCode(), getServer()==null ? "" : getServer().hashCode());
            super.stopServer();
        } else {
            log.info("Server not stopping reuse={}, running={}", isReuseServer(), isServerRunning());
        }
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

    @Override
    protected String getSpringConfigLocation()
    {
        return "classpath*:META-INF/spring-context.xml,classpath:META-INF/spring-context-test.xml";
    }

    @Override
    protected String getRestServicesPath()
    {
        return "restServices";
    }

    protected RepositoriesService getRepositoriesService()
    {
        return getRepositoriesService( null );
    }

    protected <T> T getService( Class<T> clazz, String authzHeader )
    {
        T service = JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/", clazz,
                                               Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.client(service).header("Referer","http://localhost:"+getServerPort());
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000000 );
        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );
        return service;
    }

    protected ProxyConnectorRuleService getProxyConnectorRuleService( String authzHeader )
    {
        return getService( ProxyConnectorRuleService.class, authzHeader );
    }

    protected MergeRepositoriesService getMergeRepositoriesService( String authzHeader )
    {
        return getService( MergeRepositoriesService.class, authzHeader );
    }

    protected RepositoriesService getRepositoriesService( String authzHeader )
    {
        return getService( RepositoriesService.class, authzHeader );

    }

    protected ManagedRepositoriesService getManagedRepositoriesService( String authzHeader )
    {
        return getService( ManagedRepositoriesService.class, authzHeader );
    }

    protected PingService getPingService()
    {
        return getService( PingService.class, null );
    }
    
    protected PluginsService getPluginsService()
    {
        PluginsService service = getService( PluginsService.class, null );
        WebClient.client( service ).accept( MediaType.TEXT_PLAIN );
        WebClient.client( service ).type( MediaType.TEXT_PLAIN );
        return service;
    }

    protected RemoteRepositoriesService getRemoteRepositoriesService()
    {
        return getService( RemoteRepositoriesService.class, null );


    }

    protected RepositoryGroupService getRepositoryGroupService()
    {
        return JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                          RepositoryGroupService.class,
                                          Collections.singletonList( new JacksonJaxbJsonProvider() ) );
    }

    protected ProxyConnectorService getProxyConnectorService()
    {
        ProxyConnectorService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       ProxyConnectorService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.client(service).header("Referer","http://localhost:"+getServerPort());
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000 );
        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );
        return service;
    }

    protected NetworkProxyService getNetworkProxyService()
    {
        NetworkProxyService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       NetworkProxyService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.client(service).header("Referer","http://localhost:"+getServerPort());
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000 );
        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );
        return service;
    }

    protected ArchivaAdministrationService getArchivaAdministrationService()
    {
        ArchivaAdministrationService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       ArchivaAdministrationService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.client(service).header("Referer","http://localhost:"+getServerPort());

        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000 );
        return service;
    }

    protected RedbackRuntimeConfigurationService getRedbackRuntimeConfigurationService()
    {
        RedbackRuntimeConfigurationService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       RedbackRuntimeConfigurationService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.client(service).header("Referer","http://localhost:"+getServerPort());
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000 );
        return service;
    }

    protected BrowseService getBrowseService( String authzHeader, boolean useXml )
    {
        // START SNIPPET: cxf-browseservice-creation
        BrowseService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       BrowseService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );
        // to add authentification
        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        // Set the Referer header to your archiva server url
        WebClient.client(service).header("Referer","http://localhost:"+getServerPort());

        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000000 );
        if ( useXml )
        {
            WebClient.client( service ).accept( MediaType.APPLICATION_XML_TYPE );
            WebClient.client( service ).type( MediaType.APPLICATION_XML_TYPE );
        }
        else
        {
            WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
            WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );
        }
        return service;
        // END SNIPPET: cxf-browseservice-creation

    }

    protected SearchService getSearchService( String authzHeader )
    {
        // START SNIPPET: cxf-searchservice-creation        
        SearchService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       SearchService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );
        // to add authentification
        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        // Set the Referer header to your archiva server url
        WebClient.client(service).header("Referer","http://localhost:"+getServerPort());
        // to configure read timeout
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000000 );
        // if you want to use json as exchange format xml is supported too
        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );
        return service;
        // END SNIPPET: cxf-searchservice-creation

    }

    protected CommonServices getCommonServices( String authzHeader )
    {
        CommonServices service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       CommonServices.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.client(service).header("Referer","http://localhost:"+getServerPort());
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000000 );
        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );
        return service;
    }

    protected ManagedRepository getTestManagedRepository()
    {
        String location = getAppserverBase().resolve( "data/repositories/test-repo" ).toAbsolutePath().toString();
        return new ManagedRepository( Locale.getDefault(),  "TEST", "test", location, "default", true, true, false, "2 * * * * ?", null,
                                      false, 2, 3, true, false, "my nice repo", false );

    }

    protected String getBaseUrl()
    {
        String baseUrlSysProps = System.getProperty( "archiva.baseRestUrl" );
        return StringUtils.isBlank( baseUrlSysProps ) ? "http://localhost:" + getServerPort() : baseUrlSysProps;
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

    //-----------------------------------------------------
    // utilities to create repos for testing
    //-----------------------------------------------------

    static final String TARGET_REPO_ID = "test-copy-target";

    static final String SOURCE_REPO_ID = "test-origin-repo";

    protected void initSourceTargetRepo()
        throws Exception
    {
        Path targetRepo = getAppserverBase().resolve("data/repositories/test-repo-copy" );
        if ( Files.exists(targetRepo) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( targetRepo );
        }
        assertFalse( Files.exists(targetRepo) );
        Files.createDirectories( targetRepo );

        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( TARGET_REPO_ID, true );
            assertNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) );
        }
        ManagedRepository managedRepository = getTestManagedRepository();
        managedRepository.setId( TARGET_REPO_ID );
        managedRepository.setLocation( targetRepo.toAbsolutePath().toString() );
        managedRepository.setCronExpression( "* * * * * ?" );
        getManagedRepositoriesService( authorizationHeader ).addManagedRepository( managedRepository );
        assertNotNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) );

        Path originRepo = getAppserverBase().resolve( "data/repositories/test-origin-repo" );
        if ( Files.exists(originRepo) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( originRepo );
        }
        assertFalse( Files.exists(originRepo) );
        FileUtils.copyDirectory( getProjectDirectory().resolve("src/test/repo-with-osgi" ).toAbsolutePath().toFile(), originRepo.toAbsolutePath().toFile() );

        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( SOURCE_REPO_ID, true );
            assertNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) );
        }

        managedRepository = getTestManagedRepository();
        managedRepository.setId( SOURCE_REPO_ID );
        managedRepository.setLocation( originRepo.toAbsolutePath().toString() );

        getManagedRepositoriesService( authorizationHeader ).addManagedRepository( managedRepository );
        assertNotNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) );

        getArchivaAdministrationService().enabledKnownContentConsumer( "create-missing-checksums" );
        getArchivaAdministrationService().enabledKnownContentConsumer( "metadata-updater" );

    }

    protected void cleanRepos()
        throws Exception
    {

        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) != null )
        {
            try
            {
                getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( TARGET_REPO_ID, true );
                assertNull(
                    getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) );
            }
            catch ( Exception e )
            {
                log.warn( "skip issue while cleaning test repository: this can cause test failure", e );
            }
        }
        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) != null )
        {
            try
            {
                getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( SOURCE_REPO_ID, true );
                assertNull(
                    getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) );
            }
            catch ( Exception e )
            {
                log.warn( "skip issue while cleaning test repository: this can cause test failure", e );
            }
        }

    }

    protected void createAndIndexRepo( String testRepoId, Path srcRepoPath, Path stagedSrcRepoPath, boolean stageNeeded )
        throws ArchivaRestServiceException, IOException, RedbackServiceException
    {
        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( testRepoId ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( testRepoId, false );
        }

        ManagedRepository managedRepository = new ManagedRepository(Locale.getDefault());
        managedRepository.setId( testRepoId );
        managedRepository.setName( "test repo" );

        Path badContent = srcRepoPath.resolve( "target" );
        if ( Files.exists(badContent) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( badContent );
        }

        Path repoPath = getAppserverBase().resolve( "data" ).resolve( "repositories" ).resolve( testRepoId);
        Path stagedRepoPath = getAppserverBase().resolve( "data" ).resolve( "repositories" ).resolve( testRepoId + "-stage");

        FileUtils.deleteQuietly(repoPath.toFile());
        FileUtils.copyDirectory(srcRepoPath.toFile(), repoPath.toFile());

        if (stagedSrcRepoPath!=null) {
            FileUtils.deleteQuietly(stagedRepoPath.toFile());
            FileUtils.copyDirectory(stagedSrcRepoPath.toFile(), stagedRepoPath.toFile());

        }

        managedRepository.setLocation( repoPath.toAbsolutePath().toString() );
        String suffix = Long.toString( new Date().getTime() );
        Path baseDir = Files.createTempDirectory( "archiva-test-index" ).toAbsolutePath();
        managedRepository.setIndexDirectory(
            baseDir.resolve( ".indexer-" + suffix ).toString());
        managedRepository.setPackedIndexDirectory(baseDir.resolve(".index-" + suffix).toString());

        managedRepository.setStageRepoNeeded( stageNeeded );
        managedRepository.setSnapshots( true );

        //managedRepository.setScanned( scanned );

        ManagedRepositoriesService service = getManagedRepositoriesService( authorizationHeader );
        service.addManagedRepository( managedRepository );

        getRoleManagementService( authorizationHeader ).assignTemplatedRole(
            ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, testRepoId, "admin" );

        getRoleManagementService( authorizationHeader ).assignTemplatedRole(
            ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, testRepoId, "guest" );
    }

    protected void scanRepo( String testRepoId )
        throws ArchivaRestServiceException
    {
        getRepositoriesService( authorizationHeader ).scanRepositoryNow( testRepoId, true );
    }

    protected void createAndIndexRepo( String testRepoId, Path srcRepoPath )
        throws Exception
    {
        createAndIndexRepo( testRepoId, srcRepoPath, null, false );
        scanRepo( testRepoId );
    }

    protected void createStagedNeededRepo( String testRepoId, Path srcRepoPath, Path stagedSrcRepoPath, boolean scan )
        throws Exception
    {
        createAndIndexRepo( testRepoId, srcRepoPath, stagedSrcRepoPath, true );
        if ( scan )
        {
            scanRepo( testRepoId );
        }

        RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );
        repositoriesService.scanRepositoryDirectoriesNow( testRepoId );
        if ( scan )
        {
            repositoriesService.scanRepositoryNow( testRepoId + "-stage", true );
            repositoriesService.scanRepositoryDirectoriesNow( testRepoId + "-stage" );
        }
    }


    protected void deleteTestRepo( String id )
        throws Exception
    {
        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( id ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( id, false );
        }
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

    protected void waitForScanToComplete( String repoId )
        throws ArchivaRestServiceException, InterruptedException
    {
        while ( getRepositoriesService( authorizationHeader ).alreadyScanning( repoId ) ) {
            // Would be better to cancel, if we had that capacity
            Thread.sleep( 100 );
        }
    }
}
