package org.apache.maven.archiva.proxy;

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
import net.sf.ehcache.CacheManager;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.NetworkProxyConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.policies.CachedFailuresPolicy;
import org.apache.maven.archiva.policies.ChecksumPolicy;
import org.apache.maven.archiva.policies.PropagateErrorsDownloadPolicy;
import org.apache.maven.archiva.policies.PropagateErrorsOnUpdateDownloadPolicy;
import org.apache.maven.archiva.policies.ReleasesPolicy;
import org.apache.maven.archiva.policies.SnapshotsPolicy;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Integration test for connecting over a HTTP proxy.
 *
 * @version $Id: ManagedDefaultTransferTest.java 677852 2008-07-18 08:16:24Z brett $
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class HttpProxyTransferTest
    extends TestCase
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
        super.setUp();

        proxyHandler = applicationContext.getBean( "repositoryProxyConnectors#test", RepositoryProxyConnectors.class );

        config =
            (MockConfiguration) applicationContext.getBean( "archivaConfiguration#mock", ArchivaConfiguration.class );

        // Setup source repository (using default layout)
        String repoPath = "target/test-repository/managed/" + getName();

        File destRepoDir = new File( repoPath );

        // Cleanout destination dirs.
        if ( destRepoDir.exists() )
        {
            FileUtils.deleteDirectory( destRepoDir );
        }

        // Make the destination dir.
        destRepoDir.mkdirs();

        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( MANAGED_ID );
        repo.setName( "Default Managed Repository" );
        repo.setLocation( repoPath );
        repo.setLayout( "default" );

        ManagedRepositoryContent repoContent =
            applicationContext.getBean( "managedRepositoryContent#default", ManagedRepositoryContent.class );

        repoContent.setRepository( repo );
        managedDefaultRepository = repoContent;

        config.getConfiguration().addManagedRepository( repo );

        Handler handler = new AbstractHandler()
        {
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

        server = new Server( 0 );
        server.setHandler( handler );
        server.start();

        int port = server.getConnectors()[0].getLocalPort();

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

        CacheManager.getInstance().clearAll();

    }

    @After
    public void tearDown()
        throws Exception
    {
        super.tearDown();

        server.stop();
    }

    @Test
    public void testGetOverHttpProxy()
        throws Exception
    {
        assertNull( System.getProperty( "http.proxyHost" ) );
        assertNull( System.getProperty( "http.proxyPort" ) );

        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";

        // Configure Connector (usually done within archiva.xml configuration)
        addConnector();

        File expectedFile = new File( new File( managedDefaultRepository.getRepoRoot() ), path );
        ArtifactReference artifact = managedDefaultRepository.toArtifactReference( path );

        // Attempt the proxy fetch.
        File downloadedFile = proxyHandler.fetchFromProxies( managedDefaultRepository, artifact );

        File sourceFile = new File( PROXIED_BASEDIR, path );
        assertNotNull( "Expected File should not be null.", expectedFile );
        assertNotNull( "Actual File should not be null.", downloadedFile );

        assertTrue( "Check actual file exists.", downloadedFile.exists() );
        assertEquals( "Check filename path is appropriate.", expectedFile.getCanonicalPath(),
                      downloadedFile.getCanonicalPath() );
        assertEquals( "Check file path matches.", expectedFile.getAbsolutePath(), downloadedFile.getAbsolutePath() );

        String expectedContents = FileUtils.readFileToString( sourceFile, null );
        String actualContents = FileUtils.readFileToString( downloadedFile, null );
        assertEquals( "Check file contents.", expectedContents, actualContents );

        assertNull( System.getProperty( "http.proxyHost" ) );
        assertNull( System.getProperty( "http.proxyPort" ) );
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
