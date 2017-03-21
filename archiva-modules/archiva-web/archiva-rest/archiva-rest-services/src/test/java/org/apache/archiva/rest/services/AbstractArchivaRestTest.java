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
import org.apache.archiva.common.utils.FileUtil;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.services.AbstractRestServicesTest;
import org.apache.archiva.rest.api.services.ArchivaAdministrationService;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.RedbackRuntimeConfigurationService;
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.archiva.rest.api.services.CommonServices;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.MergeRepositoriesService;
import org.apache.archiva.rest.api.services.NetworkProxyService;
import org.apache.archiva.rest.api.services.PingService;
import org.apache.archiva.rest.api.services.ProxyConnectorRuleService;
import org.apache.archiva.rest.api.services.ProxyConnectorService;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.rest.api.services.RepositoryGroupService;
import org.apache.archiva.rest.api.services.SearchService;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import org.apache.archiva.rest.api.services.PluginsService;

/**
 * @author Olivier Lamy
 */
@RunWith(ArchivaBlockJUnit4ClassRunner.class)
public abstract class AbstractArchivaRestTest
    extends AbstractRestServicesTest
{

    // START SNIPPET: authz-header
    // guest with an empty password
    public static String guestAuthzHeader =
        "Basic " + org.apache.cxf.common.util.Base64Utility.encode( ( "guest" + ":" ).getBytes() );

    // with an other login/password
    //public String authzHeader =
    //    "Basic " + org.apache.cxf.common.util.Base64Utility.encode( ( "login" + ":password" ).getBytes() );

    // END SNIPPET: authz-header

    @BeforeClass
    public static void chekRepo()
    {
        Assume.assumeTrue( !System.getProperty( "appserver.base" ).contains( " " ) );
        LoggerFactory.getLogger( AbstractArchivaRestTest.class.getName() ).
            error( "Rest services unit test must be run in a folder with no space" );
        // skygo: was not possible to fix path in this particular module
        // Skip test if not in proper folder , otherwise test are not fair coz repository
        // cannot have space in their name.
    }

    @Override
    @Before
    public void startServer()
        throws Exception
    {
        File appServerBase = new File( System.getProperty( "appserver.base" ) );

        removeAppsubFolder( appServerBase, "jcr" );
        removeAppsubFolder( appServerBase, "conf" );
        removeAppsubFolder( appServerBase, "data" );

        super.startServer();
    }

    private void removeAppsubFolder( File appServerBase, String folder )
        throws Exception
    {
        File directory = new File( appServerBase, folder );
        if ( directory.exists() )
        {
            FileUtils.deleteDirectory( directory );
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
        WebClient.client(service).header("Referer","http://localhost:"+port);
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
        WebClient.client(service).header("Referer","http://localhost:"+port);
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
        WebClient.client(service).header("Referer","http://localhost:"+port);
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
        WebClient.client(service).header("Referer","http://localhost:"+port);

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
        WebClient.client(service).header("Referer","http://localhost:"+port);
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
        WebClient.client(service).header("Referer","http://localhost:"+port);

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
        WebClient.client(service).header("Referer","http://localhost:"+port);
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
        WebClient.client(service).header("Referer","http://localhost:"+port);
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000000 );
        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );
        return service;
    }

    protected ManagedRepository getTestManagedRepository()
    {
        String location = new File( FileUtil.getBasedir(), "target/test-repo" ).getAbsolutePath();
        return new ManagedRepository( "TEST", "test", location, "default", true, true, false, "2 * * * * ?", null,
                                      false, 2, 3, true, false, "my nice repo", false );

    }

    protected String getBaseUrl()
    {
        String baseUrlSysProps = System.getProperty( "archiva.baseRestUrl" );
        return StringUtils.isBlank( baseUrlSysProps ) ? "http://localhost:" + port : baseUrlSysProps;
    }

    //-----------------------------------------------------
    // utilities to create repos for testing
    //-----------------------------------------------------

    static final String TARGET_REPO_ID = "test-copy-target";

    static final String SOURCE_REPO_ID = "test-origin-repo";

    protected void initSourceTargetRepo()
        throws Exception
    {
        File targetRepo = new File( "target/test-repo-copy" );
        if ( targetRepo.exists() )
        {
            FileUtils.deleteDirectory( targetRepo );
        }
        assertFalse( targetRepo.exists() );
        targetRepo.mkdirs();

        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( TARGET_REPO_ID, true );
            assertNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) );
        }
        ManagedRepository managedRepository = getTestManagedRepository();
        managedRepository.setId( TARGET_REPO_ID );
        managedRepository.setLocation( targetRepo.getCanonicalPath() );
        managedRepository.setCronExpression( "* * * * * ?" );
        getManagedRepositoriesService( authorizationHeader ).addManagedRepository( managedRepository );
        assertNotNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) );

        File originRepo = new File( "target/test-origin-repo" );
        if ( originRepo.exists() )
        {
            FileUtils.deleteDirectory( originRepo );
        }
        assertFalse( originRepo.exists() );
        FileUtils.copyDirectory( new File( "src/test/repo-with-osgi" ), originRepo );

        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( SOURCE_REPO_ID, true );
            assertNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) );
        }

        managedRepository = getTestManagedRepository();
        managedRepository.setId( SOURCE_REPO_ID );
        managedRepository.setLocation( originRepo.getCanonicalPath() );

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

    protected void createAndIndexRepo( String testRepoId, String repoPath, boolean stageNeeded )
        throws ArchivaRestServiceException, IOException, RedbackServiceException
    {
        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( testRepoId ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( testRepoId, false );
        }

        ManagedRepository managedRepository = new ManagedRepository();
        managedRepository.setId( testRepoId );
        managedRepository.setName( "test repo" );

        File badContent = new File( repoPath, "target" );
        if ( badContent.exists() )
        {
            FileUtils.deleteDirectory( badContent );
        }

        File file = new File( repoPath );
        if ( !file.isAbsolute() )
        {
            repoPath = getBasedir() + "/" + repoPath;
        }

        managedRepository.setLocation( new File( repoPath ).getPath() );
        managedRepository.setIndexDirectory(
            System.getProperty( "java.io.tmpdir" ) + "/target/.index-" + Long.toString( new Date().getTime() ) );

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

    protected void createAndIndexRepo( String testRepoId, String repoPath )
        throws Exception
    {
        createAndIndexRepo( testRepoId, repoPath, false );
        scanRepo( testRepoId );
    }

    protected void createStagedNeededRepo( String testRepoId, String repoPath, boolean scan )
        throws Exception
    {
        createAndIndexRepo( testRepoId, repoPath, true );
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

    public String getBasedir()
    {
        return System.getProperty( "basedir" );
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
