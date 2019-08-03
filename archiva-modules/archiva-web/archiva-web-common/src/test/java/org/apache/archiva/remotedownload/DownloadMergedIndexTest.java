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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.beans.RepositoryGroup;
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.services.FakeCreateAdminService;
import org.apache.archiva.rest.api.model.SearchRequest;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.ProxyConnectorService;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.archiva.rest.api.services.RepositoryGroupService;
import org.apache.archiva.rest.api.services.SearchService;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class DownloadMergedIndexTest
    extends AbstractDownloadTest
{

    private static Path appServerBase;
    private Path indexDir;

    @BeforeClass
    public static void setAppServerBase()
        throws IOException
    {
        previousAppServerBase = System.getProperty( "appserver.base" );
        appServerBase = Files.createTempDirectory( "archiva-common-web_appsrv4_" ).toAbsolutePath();
        System.setProperty( "appserver.base", appServerBase.toString( ) );
    }

    @AfterClass
    public static void resetAppServerBase()
    {
        if (Files.exists(appServerBase)) {
            FileUtils.deleteQuietly( appServerBase.toFile() );
        }
        System.setProperty( "appserver.base", previousAppServerBase );
    }

    @Override
    protected String getSpringConfigLocation()
    {
        System.out.println( "AppserverBase: " + System.getProperty( "appserver.base" ) );
        return "classpath*:META-INF/spring-context.xml classpath*:spring-context-test-common.xml classpath*:spring-context-merge-index-download.xml";
    }

    @Before
    public void init() throws IOException
    {
        indexDir = Files.createTempDirectory( "archiva-web-common-index" );
    }

    @After
    public void cleanup()
        throws Exception
    {
        super.tearDown();
        if ( Files.exists( indexDir ) )
        {
            FileUtils.deleteDirectory( indexDir.toFile() );
        }
    }


    @Test
    public void downloadMergedIndex()
        throws Exception
    {
        String id = Long.toString( System.currentTimeMillis() );
        Path srcRep = getProjectDirectory( ).resolve( "src/test/repositories/test-repo" );
        Path testRep = getBasedir( ).resolve( "target" ).resolve( "test-repo-" + id ).toAbsolutePath();
        FileUtils.copyDirectory( srcRep.toFile( ), testRep.toFile( ) );
        createdPaths.add( testRep );


        ManagedRepository managedRepository = new ManagedRepository( Locale.getDefault());
        managedRepository.setId( id );
        managedRepository.setName( "name of " + id );
        managedRepository.setLocation( testRep.toString() );
        managedRepository.setIndexDirectory( indexDir.resolve( "index-" + id ).toString() );

        ManagedRepositoriesService managedRepositoriesService = getManagedRepositoriesService();

        if ( managedRepositoriesService.getManagedRepository( id ) != null )
        {
            managedRepositoriesService.deleteManagedRepository( id, false );
        }

        getManagedRepositoriesService().addManagedRepository( managedRepository );

        RepositoriesService repositoriesService = getRepositoriesService();

        repositoriesService.scanRepositoryNow( id, true );

        // wait a bit to ensure index is finished
        int timeout = 20000;
        while ( timeout > 0 && repositoriesService.alreadyScanning( id ) )
        {
            Thread.sleep( 500 );
            timeout -= 500;
        }

        RepositoryGroupService repositoryGroupService = getRepositoryGroupService();

        String repoGroupId = "test-group";

        if ( repositoryGroupService.getRepositoryGroup( repoGroupId ) != null )
        {
            repositoryGroupService.deleteRepositoryGroup( repoGroupId );
        }

        RepositoryGroup repositoryGroup = new RepositoryGroup();
        repositoryGroup.setId( repoGroupId );
        repositoryGroup.setRepositories( Arrays.asList( id ) );

        repositoryGroupService.addRepositoryGroup( repositoryGroup );

        // create a repo with a remote on the one with index
        id = Long.toString( System.currentTimeMillis() );

        Path srcRep2 = getProjectDirectory( ).resolve( "src/test/repositories/test-repo" );
        Path testRep2 = getBasedir( ).resolve( "target" ).resolve( "test-repo-" + id ).toAbsolutePath();
        FileUtils.copyDirectory( srcRep2.toFile( ), testRep2.toFile( ) );
        createdPaths.add( testRep2 );

        managedRepository = new ManagedRepository(Locale.getDefault());
        managedRepository.setId( id );
        managedRepository.setName( "name of " + id );
        managedRepository.setLocation( testRep2.toString() );
        managedRepository.setIndexDirectory( indexDir.resolve( "index-" + id ).toString() );

        if ( managedRepositoriesService.getManagedRepository( id ) != null )
        {
            managedRepositoriesService.deleteManagedRepository( id, false );
        }

        getManagedRepositoriesService().addManagedRepository( managedRepository );

        RemoteRepository remoteRepository = new RemoteRepository(Locale.getDefault());
        remoteRepository.setId( "all-merged" );
        remoteRepository.setName( "all-merged" );
        remoteRepository.setDownloadRemoteIndex( true );
        remoteRepository.setUrl( "http://localhost:" + port + "/repository/test-group" );
        remoteRepository.setRemoteIndexUrl( "http://localhost:" + port + "/repository/test-group/.index" );
        remoteRepository.setUserName( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME );
        remoteRepository.setPassword( FakeCreateAdminService.ADMIN_TEST_PWD );

        if ( getRemoteRepositoriesService().getRemoteRepository( remoteRepository.getId() ) != null )
        {
            getRemoteRepositoriesService().deleteRemoteRepository( remoteRepository.getId() );
        }

        getRemoteRepositoriesService().addRemoteRepository( remoteRepository );

        ProxyConnectorService proxyConnectorService = getProxyConnectorService();
        ProxyConnector proxyConnector = new ProxyConnector();
        proxyConnector.setProxyId( "foo-bar1" );
        proxyConnector.setSourceRepoId( id );
        proxyConnector.setTargetRepoId( "all-merged" );
        proxyConnectorService.addProxyConnector( proxyConnector );

        repositoriesService.scheduleDownloadRemoteIndex( "all-merged", true, true );

        // wait a bit
        timeout = 20000;
        while ( timeout > 0 )
        {
            Thread.sleep( 500 );
            timeout -= 500;
        }

        SearchService searchService = getSearchService();

        SearchRequest request = new SearchRequest();
        request.setRepositories( Arrays.asList( id ) );
        request.setGroupId( "org.apache.felix" );

        List<Artifact> artifacts = searchService.searchArtifacts( request );
        assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 1 );

    }
}
