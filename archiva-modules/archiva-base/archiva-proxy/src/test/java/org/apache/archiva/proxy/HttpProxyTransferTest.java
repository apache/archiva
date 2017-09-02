package org.apache.archiva.proxy;

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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.proxy.model.RepositoryProxyConnectors;
import org.apache.commons.io.FileUtils;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.NetworkProxyConfiguration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.policies.CachedFailuresPolicy;
import org.apache.archiva.policies.ChecksumPolicy;
import org.apache.archiva.policies.PropagateErrorsDownloadPolicy;
import org.apache.archiva.policies.PropagateErrorsOnUpdateDownloadPolicy;
import org.apache.archiva.policies.ReleasesPolicy;
import org.apache.archiva.policies.SnapshotsPolicy;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * Integration test for connecting over a HTTP proxy.
 *
 *
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class HttpProxyTransferTest
{
    private static final String PROXY_ID = "proxy";

    private static final String MANAGED_ID = "default-managed-repository";

    private static final String PROXIED_ID = "proxied1";

    private static final String PROXIED_BASEDIR = "src/test/repositories/proxied1";

    private RepositoryProxyConnectors proxyHandler;

    private ArchivaConfiguration config;

    private ManagedRepositoryContent managedDefaultRepository;

    @Inject
    private ApplicationContext applicationContext;

    private Server server;

    @Before
    public void setUp()
        throws Exception
    {
        proxyHandler = applicationContext.getBean( "repositoryProxyConnectors#test", RepositoryProxyConnectors.class );

        config = applicationContext.getBean( "archivaConfiguration#mock", ArchivaConfiguration.class );

        // clear from previous tests - TODO the spring context should be initialised per test instead, or the config
        // made a complete mock
        config.getConfiguration().getProxyConnectors().clear();

        // Setup source repository (using default layout)
        String repoPath = "target/test-repository/managed/" + getClass().getSimpleName();

        Path destRepoDir = Paths.get( repoPath );

        // Cleanout destination dirs.
        if ( Files.exists(destRepoDir))
        {
            FileUtils.deleteDirectory( destRepoDir.toFile() );
        }

        // Make the destination dir.
        Files.createDirectories(destRepoDir);

        ManagedRepository repo = new ManagedRepository();
        repo.setId( MANAGED_ID );
        repo.setName( "Default Managed Repository" );
        repo.setLocation( repoPath );
        repo.setLayout( "default" );

        ManagedRepositoryContent repoContent =
            applicationContext.getBean( "managedRepositoryContent#default", ManagedRepositoryContent.class );

        repoContent.setRepository( repo );
        managedDefaultRepository = repoContent;

        ( (DefaultManagedRepositoryAdmin) applicationContext.getBean(
            ManagedRepositoryAdmin.class ) ).setArchivaConfiguration( config );

        ManagedRepositoryAdmin managedRepositoryAdmin = applicationContext.getBean( ManagedRepositoryAdmin.class );
        if ( managedRepositoryAdmin.getManagedRepository( repo.getId() ) == null )
        {
            managedRepositoryAdmin.addManagedRepository( repo, false, null );
        }

        //config.getConfiguration().addManagedRepository( repo );

        Handler handler = new AbstractHandler()
        {
            @Override
            public void handle( String s, Request request, HttpServletRequest httpServletRequest,
                                HttpServletResponse response )
                throws IOException, ServletException
            {
                response.setContentType( "text/plain" );
                response.setStatus( HttpServletResponse.SC_OK );
                response.getWriter().print( "get-default-layout-1.0.jar\n\n" );
                assertNotNull( request.getHeader( "Proxy-Connection" ) );

                ( (Request) request ).setHandled( true );
            }

            public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
                throws IOException, ServletException
            {
                response.setContentType( "text/plain" );
                response.setStatus( HttpServletResponse.SC_OK );
                response.getWriter().print( "get-default-layout-1.0.jar\n\n" );
                assertNotNull( request.getHeader( "Proxy-Connection" ) );

                ( (Request) request ).setHandled( true );
            }
        };

        server = new Server(  );
        ServerConnector serverConnector = new ServerConnector( server, new HttpConnectionFactory());
        server.addConnector( serverConnector );
        server.setHandler( handler );
        server.start();

        int port = serverConnector.getLocalPort();

        NetworkProxyConfiguration proxyConfig = new NetworkProxyConfiguration();
        proxyConfig.setHost( "localhost" );
        proxyConfig.setPort( port );
        proxyConfig.setProtocol( "http" );
        proxyConfig.setId( PROXY_ID );
        config.getConfiguration().addNetworkProxy( proxyConfig );

        // Setup target (proxied to) repository.
        RemoteRepositoryConfiguration repoConfig = new RemoteRepositoryConfiguration();

        repoConfig.setId( PROXIED_ID );
        repoConfig.setName( "Proxied Repository 1" );
        repoConfig.setLayout( "default" );
        repoConfig.setUrl( "http://www.example.com/" );

        config.getConfiguration().addRemoteRepository( repoConfig );

    }

    @After
    public void tearDown()
        throws Exception
    {
        server.stop();
    }

    @Test
    public void testGetOverHttpProxy()
        throws Exception
    {
        Assertions.assertThat( System.getProperty( "http.proxyHost" ) ).isEmpty();
        Assertions.assertThat( System.getProperty( "http.proxyPort" ) ).isEmpty();

        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";

        // Configure Connector (usually done within archiva.xml configuration)
        addConnector();

        Path expectedFile = Paths.get( managedDefaultRepository.getRepoRoot() ).resolve( path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Attempt the proxy fetch.
        Path downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        Path sourceFile = Paths.get( PROXIED_BASEDIR, path );
        assertNotNull( "Expected File should not be null.", expectedFile );
        assertNotNull( "Actual File should not be null.", downloadedFile );

        assertTrue( "Check actual file exists.", Files.exists(downloadedFile));
        assertTrue( "Check filename path is appropriate.", Files.isSameFile( expectedFile, downloadedFile));
        assertTrue( "Check file path matches.", Files.isSameFile( expectedFile, downloadedFile));

        String expectedContents = FileUtils.readFileToString( sourceFile.toFile(), Charset.defaultCharset() );
        String actualContents = FileUtils.readFileToString( downloadedFile.toFile(), Charset.defaultCharset() );
        assertEquals( "Check file contents.", expectedContents, actualContents );

        Assertions.assertThat( System.getProperty( "http.proxyHost" ) ).isEmpty();
        Assertions.assertThat( System.getProperty( "http.proxyPort" ) ).isEmpty();
    }

    private void addConnector()
    {
        ProxyConnectorConfiguration connectorConfig = new ProxyConnectorConfiguration();
        connectorConfig.setProxyId( PROXY_ID );
        connectorConfig.setSourceRepoId( MANAGED_ID );
        connectorConfig.setTargetRepoId( PROXIED_ID );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_CHECKSUM, ChecksumPolicy.FIX );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_RELEASES, ReleasesPolicy.ONCE );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_SNAPSHOTS, SnapshotsPolicy.ONCE );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_CACHE_FAILURES, CachedFailuresPolicy.NO );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_PROPAGATE_ERRORS,
                                   PropagateErrorsDownloadPolicy.QUEUE );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_PROPAGATE_ERRORS_ON_UPDATE,
                                   PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT );

        int count = config.getConfiguration().getProxyConnectors().size();
        config.getConfiguration().addProxyConnector( connectorConfig );

        // Proper Triggering ...
        String prefix = "proxyConnectors.proxyConnector(" + count + ")";
        ( (MockConfiguration) config ).triggerChange( prefix + ".sourceRepoId", connectorConfig.getSourceRepoId() );
    }
}
