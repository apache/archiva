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
import org.apache.archiva.proxy.model.RepositoryProxyHandler;
import org.apache.archiva.repository.*;
import org.apache.archiva.repository.base.BasicManagedRepository;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.HttpWagon;
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

    private RepositoryProxyHandler proxyHandler;

    private ManagedRepositoryContent managedDefaultRepository;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private RepositoryRegistry repositoryRegistry;

    @Inject
    private ArchivaConfiguration config;

    @Inject
    private ProxyRegistry proxyRegistry;

    private Server server;

    protected ManagedRepositoryContent createRepository( String id, String name, String path, String layout )
            throws Exception
    {
        ManagedRepository repo = BasicManagedRepository.newFilesystemInstance(id, name, Paths.get(path).resolve(id));
        repositoryRegistry.putRepository(repo);
        return repositoryRegistry.getManagedRepository(id).getContent();
    }

    @Before
    public void setUp()
        throws Exception
    {
        proxyHandler = applicationContext.getBean( "repositoryProxyHandler#test", RepositoryProxyHandler.class );

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
        ( (MockConfiguration) config ).triggerChange("networkProxies.networkProxy(0).host", "localhost");

        // Setup target (proxied to) repository.
        RemoteRepositoryConfiguration repoConfig = new RemoteRepositoryConfiguration();

        repoConfig.setId( PROXIED_ID );
        repoConfig.setName( "Proxied Repository 1" );
        repoConfig.setLayout( "default" );
        repoConfig.setUrl( "http://www.example.com/" );

        config.getConfiguration().addRemoteRepository( repoConfig );

        Wagon wagon = new HttpWagon( );
        WagonDelegate delegate = (WagonDelegate) applicationContext.getBean( "wagon#http", Wagon.class );
        delegate.setDelegate( wagon );

        proxyRegistry.reload();
        repositoryRegistry.reload();

        managedDefaultRepository = createRepository(MANAGED_ID, "Default Managed Repository", repoPath, "default");

    }

    @After
    public void tearDown()
        throws Exception
    {
        if (server!=null) {
            server.stop();
        }
    }

    @Test
    public void testGetOverHttpProxy()
        throws Exception
    {
        Assertions.assertThat( System.getProperty( "http.proxyHost", "" ) ).isEmpty();
        Assertions.assertThat( System.getProperty( "http.proxyPort", "" ) ).isEmpty();

        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";

        // Configure Connector (usually done within archiva.xml configuration)
        addConnector();

        managedDefaultRepository = repositoryRegistry.getManagedRepository(MANAGED_ID).getContent();

        Path expectedFile = Paths.get( managedDefaultRepository.getRepoRoot() ).resolve( path );
        Files.deleteIfExists( expectedFile );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Attempt the proxy fetch.
        StorageAsset downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository.getRepository(), artifact );

        Path sourceFile = Paths.get( PROXIED_BASEDIR, path );
        assertNotNull( "Expected File should not be null.", expectedFile );
        assertNotNull( "Actual File should not be null.", downloadedFile );

        assertTrue( "Check actual file exists.", Files.exists(downloadedFile.getFilePath()));
        assertTrue( "Check filename path is appropriate.", Files.isSameFile( expectedFile, downloadedFile.getFilePath()));
        assertTrue( "Check file path matches.", Files.isSameFile( expectedFile, downloadedFile.getFilePath()));

        String expectedContents = FileUtils.readFileToString( sourceFile.toFile(), Charset.defaultCharset() );
        String actualContents = FileUtils.readFileToString( downloadedFile.getFilePath().toFile(), Charset.defaultCharset() );
        assertEquals( "Check file contents.", expectedContents, actualContents );

        Assertions.assertThat( System.getProperty( "http.proxyHost" , "") ).isEmpty();
        Assertions.assertThat( System.getProperty( "http.proxyPort" , "") ).isEmpty();
    }

    private void addConnector()
    {
        ProxyConnectorConfiguration connectorConfig = new ProxyConnectorConfiguration();
        connectorConfig.setProxyId( PROXY_ID );
        connectorConfig.setSourceRepoId( MANAGED_ID );
        connectorConfig.setTargetRepoId( PROXIED_ID );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_CHECKSUM, ChecksumPolicy.FIX.getId() );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_RELEASES, ReleasesPolicy.ONCE.getId() );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_SNAPSHOTS, SnapshotsPolicy.ONCE.getId() );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_CACHE_FAILURES, CachedFailuresPolicy.NO.getId() );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_PROPAGATE_ERRORS,
                                   PropagateErrorsDownloadPolicy.QUEUE.getId() );
        connectorConfig.addPolicy( ProxyConnectorConfiguration.POLICY_PROPAGATE_ERRORS_ON_UPDATE,
                                   PropagateErrorsOnUpdateDownloadPolicy.NOT_PRESENT.getId() );

        int count = config.getConfiguration().getProxyConnectors().size();
        config.getConfiguration().addProxyConnector( connectorConfig );

        // Proper Triggering ...
        String prefix = "proxyConnectors.proxyConnector(" + count + ")";
        ( (MockConfiguration) config ).triggerChange( prefix + ".sourceRepoId", connectorConfig.getSourceRepoId() );
    }
}
