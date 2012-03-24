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


import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.utils.FileUtil;
import org.apache.archiva.rest.api.services.ArchivaAdministrationService;
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.archiva.rest.api.services.CommonServices;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.NetworkProxyService;
import org.apache.archiva.rest.api.services.PingService;
import org.apache.archiva.rest.api.services.ProxyConnectorService;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.rest.api.services.RepositoryGroupService;
import org.apache.archiva.rest.api.services.SearchService;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.redback.rest.services.AbstractRestServicesTest;
import org.junit.Before;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.Collections;
import java.util.Date;

/**
 * @author Olivier Lamy
 */
public abstract class AbstractArchivaRestTest
    extends AbstractRestServicesTest
{

    // START SNIPPET: authz-header
    // guest with an empty password
    public String guestAuthzHeader =
        "Basic " + org.apache.cxf.common.util.Base64Utility.encode( ( "guest" + ":" ).getBytes() );

    // with an other login/password
    //public String authzHeader =
    //    "Basic " + org.apache.cxf.common.util.Base64Utility.encode( ( "login" + ":password" ).getBytes() );

    // END SNIPPET: authz-header


    @Override
    @Before
    public void startServer()
        throws Exception
    {
        File appServerBase = new File( System.getProperty( "appserver.base" ) );

        File jcrDirectory = new File( appServerBase, "jcr" );

        if ( jcrDirectory.exists() )
        {
            FileUtils.deleteDirectory( jcrDirectory );
        }

        super.startServer();
    }

    @Override
    protected String getSpringConfigLocation()
    {
        return "classpath*:META-INF/spring-context.xml,classpath:META-INF/spring-context-test.xml";
    }

    protected String getRestServicesPath()
    {
        return "restServices";
    }

    protected RepositoriesService getRepositoriesService()
    {
        return getRepositoriesService( null );
    }


    protected RepositoriesService getRepositoriesService( String authzHeader )
    {
        RepositoriesService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       RepositoriesService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000000 );
        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );
        return service;

    }

    protected ManagedRepositoriesService getManagedRepositoriesService( String authzHeader )
    {
        ManagedRepositoriesService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       ManagedRepositoriesService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000000 );
        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );
        return service;

    }

    protected PingService getPingService()
    {
        return JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                          PingService.class,
                                          Collections.singletonList( new JacksonJaxbJsonProvider() ) );
    }

    protected RemoteRepositoriesService getRemoteRepositoriesService()
    {
        return JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                          RemoteRepositoriesService.class,
                                          Collections.singletonList( new JacksonJaxbJsonProvider() ) );


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
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000 );
        return service;
    }

    protected BrowseService getBrowseService( String authzHeader )
    {
        BrowseService service =
            JAXRSClientFactory.create( getBaseUrl() + "/" + getRestServicesPath() + "/archivaServices/",
                                       BrowseService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );
        // to add authentification
        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }

        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000000 );

        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );
        return service;

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
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 100000000 );
        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );
        return service;
    }

    protected ManagedRepository getTestManagedRepository()
    {
        String location = new File( FileUtil.getBasedir(), "target/test-repo" ).getAbsolutePath();
        return new ManagedRepository( "TEST", "test", location, "default", true, true, false, "2 * * * * ?", null,
                                      false, 2, 3, true, false );

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
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( TARGET_REPO_ID, true );
            assertNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) );
        }
        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( SOURCE_REPO_ID, true );
            assertNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) );
        }

    }

    protected void createAndIndexRepo( String testRepoId, String repoPath )
        throws Exception
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

        managedRepository.setLocation( new File( repoPath ).getPath() );
        managedRepository.setIndexDirectory(
            System.getProperty( "java.io.tmpdir" ) + "/target/.index-" + Long.toString( new Date().getTime() ) );

        ManagedRepositoriesService service = getManagedRepositoriesService( authorizationHeader );
        service.addManagedRepository( managedRepository );

        getRoleManagementService( authorizationHeader ).assignTemplatedRole(
            ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, testRepoId, "admin" );

        getRoleManagementService( authorizationHeader ).assignTemplatedRole(
            ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, testRepoId, "guest" );

        getRepositoriesService( authorizationHeader ).scanRepositoryNow( testRepoId, true );

    }

    protected void deleteTestRepo( String id )
        throws Exception
    {
        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( id ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( id, false );
        }

    }
}
