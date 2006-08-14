package org.apache.maven.repository.proxy;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Test the proxy handler.
 *
 * @author Brett Porter
 * @todo! tests to do vvv
 * @todo test snapshots - general
 * @todo test snapshots - newer version on repo1 (than local), timestamp driven
 * @todo test snapshots - older version on repo1 skipped (than local), timestamp driven
 * @todo test snapshots - newer version on repo2 is pulled down (no local), timestamp driven
 * @todo test snapshots - older version on repo2 is skipped  (no local), timestamp driven
 * @todo test snapshots - update interval (not updated if within period), timestamp driven
 * @todo test snapshots - newer version on repo1 (than local), metadata driven
 * @todo test snapshots - older version on repo1 skipped (than local), metadata driven
 * @todo test snapshots - newer version on repo2 is pulled down (no local), metadata driven
 * @todo test snapshots - older version on repo2 is skipped  (no local), metadata driven
 * @todo test snapshots - update interval (not updated if within period), metadata driven
 * @todo test snapshots - when failure is cached but cache period is over (and check failure is cleared)
 * @todo test when managed repo is m1 layout (proxy is m2), including metadata
 * @todo test when one proxied repo is m1 layout (managed is m2), including metadata
 * @todo test when one proxied repo is m1 layout (managed is m1), including metadata
 */
public class ProxyRequestHandlerTest
    extends PlexusTestCase
{
    private ProxyRequestHandler requestHandler;

    private List proxiedRepositories;

    private ArtifactRepository defaultManagedRepository;

    private ArtifactRepository proxiedRepository1;

    private ArtifactRepository proxiedRepository2;

    private ArtifactRepositoryLayout defaultLayout;

    private ArtifactRepositoryFactory factory;

    private MockControl wagonMockControl;

    private Wagon wagonMock;

    private static final ArtifactRepositoryPolicy DEFAULT_POLICY =
        new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER, null );

    private static final ArtifactRepositoryPolicy ALWAYS_UPDATE_POLICY =
        new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, null );

    protected void setUp()
        throws Exception
    {
        super.setUp();

        requestHandler = (ProxyRequestHandler) lookup( ProxyRequestHandler.ROLE );

        File repoLocation = getTestFile( "target/test-repository/managed" );
        FileUtils.deleteDirectory( repoLocation );
        copyDirectoryStructure( getTestFile( "src/test/repositories/managed" ), repoLocation );

        defaultLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        defaultManagedRepository = createRepository( "managed-repository", repoLocation );

        File location = getTestFile( "src/test/repositories/proxied1" );
        proxiedRepository1 = createRepository( "proxied1", location );

        location = getTestFile( "src/test/repositories/proxied2" );
        proxiedRepository2 = createRepository( "proxied2", location );

        proxiedRepositories = new ArrayList( 2 );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository1 ) );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );

        wagonMockControl = MockControl.createNiceControl( Wagon.class );
        wagonMock = (Wagon) wagonMockControl.getMock();
        WagonDelegate delegate = (WagonDelegate) lookup( Wagon.ROLE, "test" );
        delegate.setDelegate( wagonMock );
    }

    public void testGetDefaultLayoutNotPresent()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( proxiedFile );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );
        // TODO: timestamp preservation requires support for that in wagon
//        assertEquals( "Check file timestamp", proxiedFile.lastModified(), file.lastModified() );
    }

    public void testGetDefaultLayoutAlreadyPresent()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( expectedFile );
        long originalModificationTime = expectedFile.lastModified();

        assertTrue( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );
        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        String unexpectedContents = FileUtils.fileRead( proxiedFile );
        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.fileRead( file ) ) );
        assertFalse( "Check file timestamp is not that of proxy", proxiedFile.lastModified() == file.lastModified() );
        assertEquals( "Check file timestamp is that of original managed file", originalModificationTime,
                      file.lastModified() );
    }

    public void testGetWhenInBothProxiedRepos()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-in-both-proxies/1.0/get-in-both-proxies-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );

        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( proxiedFile );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );

        proxiedFile = new File( proxiedRepository2.getBasedir(), path );
        String unexpectedContents = FileUtils.fileRead( proxiedFile );
        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.fileRead( file ) ) );
    }

    public void testGetInSecondProxiedRepo()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        File proxiedFile = new File( proxiedRepository2.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( proxiedFile );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );
    }

    public void testNotFoundInAnyProxies()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/does-not-exist/1.0/does-not-exist-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        try
        {
            File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
            fail( "File returned was: " + file + "; should have got a not found exception" );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // expected, but check file was not created
            assertFalse( expectedFile.exists() );
        }
    }

    public void testGetInSecondProxiedRepoFirstFails()
        throws ResourceDoesNotExistException, ProxyException, IOException, TransferFailedException,
        AuthorizationException
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path ).getAbsoluteFile();

        assertFalse( expectedFile.exists() );

        proxiedRepository1 = createRepository( "proxied1", "test://..." );
        proxiedRepositories.clear();
        ProxiedArtifactRepository proxiedArtifactRepository = createProxiedRepository( proxiedRepository1 );
        proxiedRepositories.add( proxiedArtifactRepository );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );

        wagonMock.get( path, new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" ) );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );

        wagonMockControl.replay();

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        wagonMockControl.verify();

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        File proxiedFile = new File( proxiedRepository2.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( proxiedFile );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );

        assertTrue( "Check failure", proxiedArtifactRepository.isCachedFailure( path ) );
    }

    public void testGetButAllRepositoriesFail()
        throws ResourceDoesNotExistException, ProxyException, IOException, TransferFailedException,
        AuthorizationException
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path ).getAbsoluteFile();

        assertFalse( expectedFile.exists() );

        proxiedRepository1 = createRepository( "proxied1", "test://..." );
        proxiedRepository2 = createRepository( "proxied2", "test://..." );
        proxiedRepositories.clear();
        ProxiedArtifactRepository proxiedArtifactRepository1 = createProxiedRepository( proxiedRepository1 );
        proxiedRepositories.add( proxiedArtifactRepository1 );
        ProxiedArtifactRepository proxiedArtifactRepository2 = createProxiedRepository( proxiedRepository2 );
        proxiedRepositories.add( proxiedArtifactRepository2 );

        wagonMock.get( path, new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" ) );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );

        wagonMock.get( path, new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" ) );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );

        wagonMockControl.replay();

        try
        {
            File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
            fail( "Found file: " + file + "; but was expecting a failure" );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // as expected
            wagonMockControl.verify();
            assertTrue( "Check failure", proxiedArtifactRepository1.isCachedFailure( path ) );
            assertTrue( "Check failure", proxiedArtifactRepository2.isCachedFailure( path ) );

            // TODO: do we really want failures to present as a not found?
            // TODO: How much information on each failure should we pass back to the user vs. logging in the proxy? 
        }
    }

    public void testGetInSecondProxiedRepoFirstHardFails()
        throws ResourceDoesNotExistException, ProxyException, IOException, TransferFailedException,
        AuthorizationException
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path ).getAbsoluteFile();

        assertFalse( expectedFile.exists() );

        proxiedRepository1 = createRepository( "proxied1", "test://..." );
        proxiedRepositories.clear();
        ProxiedArtifactRepository proxiedArtifactRepository = createHardFailProxiedRepository( proxiedRepository1 );
        proxiedRepositories.add( proxiedArtifactRepository );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );

        wagonMock.get( path, new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" ) );
        TransferFailedException failedException = new TransferFailedException( "transfer failed" );
        wagonMockControl.setThrowable( failedException );

        wagonMockControl.replay();

        try
        {
            File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
            fail( "Found file: " + file + "; but was expecting a failure" );
        }
        catch ( ProxyException e )
        {
            // expect a failure
            wagonMockControl.verify();

            assertEquals( "Check cause", failedException, e.getCause() );
            assertTrue( "Check failure", proxiedArtifactRepository.isCachedFailure( path ) );
        }
    }

    public void testGetInSecondProxiedRepoFirstFailsFromCache()
        throws ResourceDoesNotExistException, ProxyException, IOException, TransferFailedException,
        AuthorizationException
    {
        // fail from the cache, even though it is in the first repo now

        String path = "org/apache/maven/test/get-in-both-proxies/1.0/get-in-both-proxies-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        proxiedRepositories.clear();
        ProxiedArtifactRepository proxiedArtifactRepository = createProxiedRepository( proxiedRepository1 );
        proxiedArtifactRepository.addFailure( path, DEFAULT_POLICY );
        proxiedRepositories.add( proxiedArtifactRepository );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );
        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );

        File proxiedFile = new File( proxiedRepository2.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( proxiedFile );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );

        proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        String unexpectedContents = FileUtils.fileRead( proxiedFile );
        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.fileRead( file ) ) );
    }

    public void testGetInSecondProxiedRepoFirstHardFailsFromCache()
        throws ResourceDoesNotExistException, ProxyException, IOException, TransferFailedException,
        AuthorizationException
    {
        // fail from the cache, even though it is in the first repo now

        String path = "org/apache/maven/test/get-in-both-proxies/1.0/get-in-both-proxies-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        proxiedRepositories.clear();
        ProxiedArtifactRepository proxiedArtifactRepository = createHardFailProxiedRepository( proxiedRepository1 );
        proxiedArtifactRepository.addFailure( path, DEFAULT_POLICY );
        proxiedRepositories.add( proxiedArtifactRepository );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );
        try
        {
            File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
            fail( "Found file: " + file + "; but was expecting a failure" );
        }
        catch ( ProxyException e )
        {
            // expect a failure
            assertTrue( "Check failure", proxiedArtifactRepository.isCachedFailure( path ) );
        }
    }

    public void testGetInSecondProxiedRepoFirstFailsDisabledCacheFailure()
        throws ResourceDoesNotExistException, ProxyException, IOException, TransferFailedException,
        AuthorizationException
    {
        String path = "org/apache/maven/test/get-in-second-proxy/1.0/get-in-second-proxy-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path ).getAbsoluteFile();

        assertFalse( expectedFile.exists() );

        proxiedRepository1 = createRepository( "proxied1", "test://..." );
        proxiedRepositories.clear();
        ProxiedArtifactRepository proxiedArtifactRepository = createProxiedRepository( proxiedRepository1 );
        proxiedArtifactRepository.addFailure( path, DEFAULT_POLICY );
        proxiedArtifactRepository.setCacheFailures( false );
        proxiedRepositories.add( proxiedArtifactRepository );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );

        wagonMock.get( path, new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" ) );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );

        wagonMockControl.replay();

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        wagonMockControl.verify();

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        File proxiedFile = new File( proxiedRepository2.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( proxiedFile );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );

        assertFalse( "Check failure", proxiedArtifactRepository.isCachedFailure( path ) );
    }

    public void testGetWhenInBothProxiedReposFirstHasExpiredCacheFailure()
        throws ResourceDoesNotExistException, ProxyException, IOException, ParseException
    {
        String path = "org/apache/maven/test/get-in-both-proxies/1.0/get-in-both-proxies-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        proxiedRepositories.clear();
        ProxiedArtifactRepository proxiedArtifactRepository = createProxiedRepository( proxiedRepository1 );
        proxiedArtifactRepository.addFailure( path, ALWAYS_UPDATE_POLICY );
        proxiedRepositories.add( proxiedArtifactRepository );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );
        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );

        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( proxiedFile );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );

        proxiedFile = new File( proxiedRepository2.getBasedir(), path );
        String unexpectedContents = FileUtils.fileRead( proxiedFile );
        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.fileRead( file ) ) );

        assertFalse( "Check failure", proxiedArtifactRepository.isCachedFailure( path ) );
    }

    public void testGetAlwaysAlreadyPresent()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        String unexpectedContents = FileUtils.fileRead( expectedFile );

        assertTrue( expectedFile.exists() );

        File file = requestHandler.getAlways( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( proxiedFile );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );
        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.fileRead( file ) ) );
    }

    public void testGetAlwaysAlreadyPresentRemovedFromProxies()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-removed-from-proxies/1.0/get-removed-from-proxies-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( expectedFile );

        assertTrue( expectedFile.exists() );

        File file = requestHandler.getAlways( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );

        // TODO: is this the correct behaviour, or should it be considered removed too?
    }

    public void testGetAlwaysWithCachedFailure()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-default-layout-present/1.0/get-default-layout-present-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        String unexpectedContents = FileUtils.fileRead( expectedFile );

        assertTrue( expectedFile.exists() );

        proxiedRepositories.clear();
        ProxiedArtifactRepository proxiedArtifactRepository = createProxiedRepository( proxiedRepository1 );
        proxiedArtifactRepository.addFailure( path, DEFAULT_POLICY );
        proxiedRepositories.add( proxiedArtifactRepository );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );
        File file = requestHandler.getAlways( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( proxiedFile );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );
        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.fileRead( file ) ) );
    }

    public void testGetRemovesTemporaryFileOnSuccess()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        File tempFile = new File( file.getParentFile(), file.getName() + ".tmp" );
        assertFalse( "Check temporary file removed", tempFile.exists() );
    }

    public void testGetRemovesTemporaryFileOnError()
        throws ResourceDoesNotExistException, ProxyException, IOException, TransferFailedException,
        AuthorizationException
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        proxiedRepository1 = createRepository( "proxied1", "test://..." );
        proxiedRepositories.clear();
        ProxiedArtifactRepository proxiedArtifactRepository1 = createProxiedRepository( proxiedRepository1 );
        proxiedRepositories.add( proxiedArtifactRepository1 );

        wagonMock.get( path, new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" ) );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );

        wagonMockControl.replay();

        try
        {
            File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
            fail( "Found file: " + file + "; but was expecting a failure" );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // as expected
            wagonMockControl.verify();

            File tempFile = new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" );
            assertFalse( "Check temporary file removed", tempFile.exists() );
        }
    }

    public void testGetRemovesTemporaryChecksumFileOnSuccess()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-checksum-sha1-only/1.0/get-checksum-sha1-only-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        File tempFile = new File( file.getParentFile(), file.getName() + ".sha1.tmp" );
        assertFalse( "Check temporary file removed", tempFile.exists() );
    }

    public void testGetRemovesTemporaryChecksumFileOnError()
        throws ResourceDoesNotExistException, ProxyException, IOException, TransferFailedException,
        AuthorizationException
    {
        String path = "org/apache/maven/test/get-checksum-sha1-only/1.0/get-checksum-sha1-only-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        proxiedRepository1 = createRepository( "proxied1", "test://..." );
        proxiedRepositories.clear();
        ProxiedArtifactRepository proxiedArtifactRepository1 = createProxiedRepository( proxiedRepository1 );
        proxiedRepositories.add( proxiedArtifactRepository1 );

        wagonMock.get( path, new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" ) );

        mockFailedChecksums( path, expectedFile );

        wagonMockControl.replay();

        try
        {
            File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
            fail( "Found file: " + file + "; but was expecting a failure" );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // as expected
            wagonMockControl.verify();

            File tempFile = new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" );
            assertFalse( "Check temporary file removed", tempFile.exists() );

            tempFile = new File( expectedFile.getParentFile(), expectedFile.getName() + ".sha1.tmp" );
            assertFalse( "Check temporary file removed", tempFile.exists() );
        }
    }

    public void testGetChecksumBothCorrect()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-checksum-both-right/1.0/get-checksum-both-right-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );

        File checksumFile = getChecksumFile( file, "sha1" );
        assertTrue( "Check file created", checksumFile.exists() );
        assertEquals( "Check checksum", "066d76e459f7782c312c31e8a11b3c0f1e3e43a7 *get-checksum-both-right-1.0.jar",
                      FileUtils.fileRead( checksumFile ).trim() );

        assertFalse( "Check file not created", getChecksumFile( file, "md5" ).exists() );
    }

    public void testGetCorrectSha1NoMd5()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-checksum-sha1-only/1.0/get-checksum-sha1-only-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );

        File checksumFile = getChecksumFile( file, "sha1" );
        assertTrue( "Check file created", checksumFile.exists() );
        assertEquals( "Check checksum", "748a3a013bf5eacf2bbb40a2ac7d37889b728837 *get-checksum-sha1-only-1.0.jar",
                      FileUtils.fileRead( checksumFile ).trim() );

        assertFalse( "Check file not created", getChecksumFile( file, "md5" ).exists() );
    }

    public void testGetCorrectSha1BadMd5()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-checksum-sha1-bad-md5/1.0/get-checksum-sha1-bad-md5-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );

        File checksumFile = getChecksumFile( file, "sha1" );
        assertTrue( "Check file created", checksumFile.exists() );
        assertEquals( "Check checksum", "3dd1a3a57b807d3ef3fbc6013d926c891cbb8670 *get-checksum-sha1-bad-md5-1.0.jar",
                      FileUtils.fileRead( checksumFile ).trim() );

        assertFalse( "Check file not created", getChecksumFile( file, "md5" ).exists() );
    }

    public void testGetCorrectMd5NoSha1()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-checksum-md5-only/1.0/get-checksum-md5-only-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );

        File checksumFile = getChecksumFile( file, "md5" );
        assertTrue( "Check file created", checksumFile.exists() );
        assertEquals( "Check checksum", "f3af5201bf8da801da37db8842846e1c *get-checksum-md5-only-1.0.jar",
                      FileUtils.fileRead( checksumFile ).trim() );

        assertFalse( "Check file not created", getChecksumFile( file, "sha1" ).exists() );
    }

    public void testGetCorrectMd5BadSha1()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-checksum-md5-bad-sha1/1.0/get-checksum-md5-bad-sha1-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );

        File checksumFile = getChecksumFile( file, "md5" );
        assertTrue( "Check file created", checksumFile.exists() );
        assertEquals( "Check checksum", "8a02aa67549d27b2a03cd4547439c6d3 *get-checksum-md5-bad-sha1-1.0.jar",
                      FileUtils.fileRead( checksumFile ).trim() );

        assertFalse( "Check file not created", getChecksumFile( file, "sha1" ).exists() );
    }

    public void testGetWithNoChecksums()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-default-layout/1.0/get-default-layout-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );

        assertFalse( "Check file not created", getChecksumFile( file, "md5" ).exists() );
        assertFalse( "Check file not created", getChecksumFile( file, "sha1" ).exists() );
    }

    public void testGetBadMd5BadSha1()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-checksum-both-bad/1.0/get-checksum-both-bad-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        try
        {
            File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
            fail( "Found file: " + file + "; but was expecting a failure" );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // expect a failure
            assertFalse( "Check file not created", expectedFile.exists() );

            assertFalse( "Check file not created", getChecksumFile( expectedFile, "md5" ).exists() );
            assertFalse( "Check file not created", getChecksumFile( expectedFile, "sha1" ).exists() );
        }
    }

    public void testGetChecksumTransferFailed()
        throws ResourceDoesNotExistException, ProxyException, IOException, TransferFailedException,
        AuthorizationException
    {
        String path = "org/apache/maven/test/get-checksum-sha1-only/1.0/get-checksum-sha1-only-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        proxiedRepository1 = createRepository( "proxied1", "test://..." );
        proxiedRepositories.clear();
        ProxiedArtifactRepository proxiedArtifactRepository1 = createProxiedRepository( proxiedRepository1 );
        proxiedRepositories.add( proxiedArtifactRepository1 );

        wagonMock.get( path, new File( expectedFile.getParentFile(), expectedFile.getName() + ".tmp" ) );

        mockFailedChecksums( path, expectedFile );

        wagonMockControl.replay();

        try
        {
            File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
            fail( "Found file: " + file + "; but was expecting a failure" );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // as expected
            wagonMockControl.verify();

            assertFalse( "Check file not created", expectedFile.exists() );

            assertFalse( "Check file not created", getChecksumFile( expectedFile, "md5" ).exists() );
            assertFalse( "Check file not created", getChecksumFile( expectedFile, "sha1" ).exists() );
        }
    }

    public void testGetAlwaysBadChecksumPresentLocallyAbsentRemote()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-bad-local-checksum/1.0/get-bad-local-checksum-1.0.jar";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        String unexpectedContents = FileUtils.fileRead( expectedFile );

        assertTrue( expectedFile.exists() );

        File file = requestHandler.getAlways( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( proxiedFile );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );
        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.fileRead( file ) ) );

        assertFalse( "Check checksum removed", new File( file.getParentFile(), file.getName() + ".sha1" ).exists() );
        assertFalse( "Check checksum removed", new File( file.getParentFile(), file.getName() + ".md5" ).exists() );
    }

    public void testGetChecksumPresentInManagedRepo()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path =
            "org/apache/maven/test/get-checksum-from-managed-repo/1.0/get-checksum-from-managed-repo-1.0.jar.sha1";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( expectedFile );

        assertTrue( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        String unexpectedContents = FileUtils.fileRead( proxiedFile );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );
        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.fileRead( file ) ) );
    }

    public void testGetAlwaysChecksumPresentInManagedRepo()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path =
            "org/apache/maven/test/get-checksum-from-managed-repo/1.0/get-checksum-from-managed-repo-1.0.jar.sha1";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( expectedFile );

        assertTrue( expectedFile.exists() );

        File file = requestHandler.getAlways( path, proxiedRepositories, defaultManagedRepository );

        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        String unexpectedContents = FileUtils.fileRead( proxiedFile );
        assertEquals( "Check file contents", expectedContents, FileUtils.fileRead( file ) );
        assertFalse( "Check file contents", unexpectedContents.equals( FileUtils.fileRead( file ) ) );
    }

    public void testGetChecksumNotPresentInManagedRepo()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-checksum-sha1-only/1.0/get-checksum-sha1-only-1.0.jar.sha1";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        try
        {
            File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
            fail( "Found file: " + file + "; but was expecting a failure" );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // expected

            assertFalse( expectedFile.exists() );
        }
    }

    public void testGetAlwaysChecksumNotPresentInManagedRepo()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-checksum-sha1-only/1.0/get-checksum-sha1-only-1.0.jar.sha1";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        try
        {
            File file = requestHandler.getAlways( path, proxiedRepositories, defaultManagedRepository );
            fail( "Found file: " + file + "; but was expecting a failure" );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // expected

            assertFalse( expectedFile.exists() );
        }
    }

    public void testGetMetadataNotPresent()
        throws ProxyException
    {
        String path = "org/apache/maven/test/dummy-artifact/1.0/maven-metadata.xml";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        try
        {
            File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
            fail( "Found file: " + file + "; but was expecting a failure" );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // expected

            assertFalse( expectedFile.exists() );
        }
    }

    public void testGetMetadataProxied()
        throws ProxyException, ResourceDoesNotExistException, IOException
    {
        String path = "org/apache/maven/test/get-default-metadata/1.0/maven-metadata.xml";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertFalse( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        String expectedContents = FileUtils.fileRead( new File( proxiedRepository1.getBasedir(), path ) );
        assertEquals( "Check content matches", expectedContents, FileUtils.fileRead( file ) );
    }

    public void testGetMetadataMergeRepos()
        throws IOException, ResourceDoesNotExistException, ProxyException
    {
        String path = "org/apache/maven/test/get-merged-metadata/maven-metadata.xml";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );

        assertTrue( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );

        StringWriter expectedContents = new StringWriter();
        Metadata m = new Metadata();
        m.setGroupId( "org.apache.maven.test" );
        m.setArtifactId( "get-merged-metadata" );
        m.setVersioning( new Versioning() );
        m.getVersioning().addVersion( "0.9" );
        m.getVersioning().addVersion( "1.0" );
        m.getVersioning().addVersion( "2.0" );
        m.getVersioning().addVersion( "3.0" );
        m.getVersioning().addVersion( "5.0" );
        m.getVersioning().addVersion( "4.0" );
        m.setModelEncoding( null );
        new MetadataXpp3Writer().write( expectedContents, m );

        assertEquals( "Check content matches", expectedContents.toString(), FileUtils.fileRead( file ) );
    }

    public void testGetMetadataRemovedFromProxies()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-removed-metadata/1.0/maven-metadata.xml";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( new File( defaultManagedRepository.getBasedir(), path ) );

        assertTrue( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        assertEquals( "Check content matches", expectedContents, FileUtils.fileRead( file ) );
    }

    public void testGetMetadataNotExpired()
        throws IOException, ResourceDoesNotExistException, ProxyException
    {
        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( new File( defaultManagedRepository.getBasedir(), path ) );

        assertTrue( expectedFile.exists() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        assertEquals( "Check content matches", expectedContents, FileUtils.fileRead( file ) );

        String unexpectedContents = FileUtils.fileRead( new File( proxiedRepository1.getBasedir(), path ) );
        assertFalse( "Check content doesn't match proxy version",
                     unexpectedContents.equals( FileUtils.fileRead( file ) ) );
    }

    public void testGetMetadataNotUpdated()
        throws ResourceDoesNotExistException, ProxyException, IOException
    {
        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        String expectedContents = FileUtils.fileRead( new File( defaultManagedRepository.getBasedir(), path ) );

        assertTrue( expectedFile.exists() );

        File proxiedFile = new File( proxiedRepository1.getBasedir(), path );
        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( proxiedFile.lastModified() );

        proxiedRepository1.getReleases().setUpdatePolicy( ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS );
        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );
        assertEquals( "Check content matches", expectedContents, FileUtils.fileRead( file ) );

        String unexpectedContents = FileUtils.fileRead( proxiedFile );
        assertFalse( "Check content doesn't match proxy version",
                     unexpectedContents.equals( FileUtils.fileRead( file ) ) );
    }

    public void testGetMetadataUpdated()
        throws IOException, ResourceDoesNotExistException, ProxyException, ParseException
    {
        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        String unexpectedContents = FileUtils.fileRead( new File( defaultManagedRepository.getBasedir(), path ) );

        assertTrue( expectedFile.exists() );

        new File( expectedFile.getParentFile(), ".metadata-proxied1" ).setLastModified( getHistoricalDate().getTime() );

        File file = requestHandler.get( path, proxiedRepositories, defaultManagedRepository );
        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );

        StringWriter expectedContents = new StringWriter();
        Metadata m = new Metadata();
        m.setGroupId( "org.apache.maven.test" );
        m.setArtifactId( "get-updated-metadata" );
        m.setVersioning( new Versioning() );
        m.getVersioning().addVersion( "1.0" );
        m.getVersioning().addVersion( "2.0" );
        m.setModelEncoding( null );
        new MetadataXpp3Writer().write( expectedContents, m );
        assertEquals( "Check content matches", expectedContents.toString(), FileUtils.fileRead( file ) );
        assertFalse( "Check content doesn't match old version",
                     unexpectedContents.equals( FileUtils.fileRead( file ) ) );
    }

    public void testGetAlwaysMetadata()
        throws IOException, ResourceDoesNotExistException, ProxyException
    {
        String path = "org/apache/maven/test/get-updated-metadata/maven-metadata.xml";
        File expectedFile = new File( defaultManagedRepository.getBasedir(), path );
        String unexpectedContents = FileUtils.fileRead( new File( defaultManagedRepository.getBasedir(), path ) );

        assertTrue( expectedFile.exists() );

        File file = requestHandler.getAlways( path, proxiedRepositories, defaultManagedRepository );
        assertEquals( "Check file matches", expectedFile, file );
        assertTrue( "Check file created", file.exists() );

        StringWriter expectedContents = new StringWriter();
        Metadata m = new Metadata();
        m.setGroupId( "org.apache.maven.test" );
        m.setArtifactId( "get-updated-metadata" );
        m.setVersioning( new Versioning() );
        m.getVersioning().addVersion( "1.0" );
        m.getVersioning().addVersion( "2.0" );
        m.setModelEncoding( null );
        new MetadataXpp3Writer().write( expectedContents, m );
        assertEquals( "Check content matches", expectedContents.toString(), FileUtils.fileRead( file ) );
        assertFalse( "Check content doesn't match old version",
                     unexpectedContents.equals( FileUtils.fileRead( file ) ) );
    }

    private static Date getHistoricalDate()
        throws ParseException
    {
        return new SimpleDateFormat( "yyyy-MM-dd", Locale.US ).parse( "2000-01-01" );
    }

    private void mockFailedChecksums( String path, File expectedFile )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        // must do it twice as it will re-attempt it
        wagonMock.get( path + ".sha1", new File( expectedFile.getParentFile(), expectedFile.getName() + ".sha1.tmp" ) );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );

        wagonMock.get( path + ".md5", new File( expectedFile.getParentFile(), expectedFile.getName() + ".md5.tmp" ) );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );

        wagonMock.get( path + ".sha1", new File( expectedFile.getParentFile(), expectedFile.getName() + ".sha1.tmp" ) );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );

        wagonMock.get( path + ".md5", new File( expectedFile.getParentFile(), expectedFile.getName() + ".md5.tmp" ) );
        wagonMockControl.setThrowable( new TransferFailedException( "transfer failed" ) );
    }

    private File getChecksumFile( File file, String algorithm )
    {
        return new File( file.getParentFile(), file.getName() + "." + algorithm );
    }

    /**
     * A faster recursive copy that omits .svn directories.
     *
     * @param sourceDirectory the source directory to copy
     * @param destDirectory   the target location
     * @throws java.io.IOException if there is a copying problem
     * @todo get back into plexus-utils, share with converter module
     */
    private static void copyDirectoryStructure( File sourceDirectory, File destDirectory )
        throws IOException
    {
        if ( !sourceDirectory.exists() )
        {
            throw new IOException( "Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ")." );
        }

        File[] files = sourceDirectory.listFiles();

        String sourcePath = sourceDirectory.getAbsolutePath();

        for ( int i = 0; i < files.length; i++ )
        {
            File file = files[i];

            String dest = file.getAbsolutePath();

            dest = dest.substring( sourcePath.length() + 1 );

            File destination = new File( destDirectory, dest );

            if ( file.isFile() )
            {
                destination = destination.getParentFile();

                FileUtils.copyFileToDirectory( file, destination );
            }
            else if ( file.isDirectory() )
            {
                if ( !".svn".equals( file.getName() ) )
                {
                    if ( !destination.exists() && !destination.mkdirs() )
                    {
                        throw new IOException(
                            "Could not create destination directory '" + destination.getAbsolutePath() + "'." );
                    }

                    copyDirectoryStructure( file, destination );
                }
            }
            else
            {
                throw new IOException( "Unknown file type: " + file.getAbsolutePath() );
            }
        }
    }

    private static ProxiedArtifactRepository createProxiedRepository( ArtifactRepository repository )
    {
        ProxiedArtifactRepository proxiedArtifactRepository = new ProxiedArtifactRepository( repository );
        proxiedArtifactRepository.setName( repository.getId() );
        proxiedArtifactRepository.setCacheFailures( true );
        return proxiedArtifactRepository;
    }

    private static ProxiedArtifactRepository createHardFailProxiedRepository( ArtifactRepository repository )
    {
        ProxiedArtifactRepository proxiedArtifactRepository = createProxiedRepository( repository );
        proxiedArtifactRepository.setHardFail( true );
        return proxiedArtifactRepository;
    }

    private ArtifactRepository createRepository( String id, File repoLocation )
        throws MalformedURLException
    {
        return createRepository( id, repoLocation.toURI().toURL().toExternalForm() );
    }

    private ArtifactRepository createRepository( String id, String url )
    {
        return createRepository( id, url, defaultLayout );
    }

    private ArtifactRepository createRepository( String id, String url, ArtifactRepositoryLayout repositoryLayout )
    {
        return factory.createArtifactRepository( id, url, repositoryLayout, null, null );
    }
}
