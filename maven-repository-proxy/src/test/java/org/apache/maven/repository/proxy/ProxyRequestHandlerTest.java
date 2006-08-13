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
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Brett Porter
 * @todo! tests to do vvv
 * @todo test when failure is cached
 * @todo test when failure is cached and repo is hard fail
 * @todo test when failure should be cached but caching is disabled
 * @todo test snapshots - general
 * @todo test snapshots - newer version on repo2 is pulled down
 * @todo test snapshots - older version on repo2 is skipped
 * @todo test snapshots - update interval
 * @todo test metadata - general
 * @todo test metadata - multiple repos are merged
 * @todo test metadata - update interval
 * @todo test metadata - looking for an update and file has been removed remotely
 * @todo test when managed repo is m1 layout (proxy is m2), including metadata
 * @todo test when one proxied repo is m1 layout (managed is m2), including metadata
 * @todo test when one proxied repo is m1 layout (managed is m1), including metadata
 * @todo test get always
 * @todo test get always when resource is present locally but not in any proxied repos (should fail)
 * @todo test remote checksum only md5
 * @todo test remote checksum only sha1
 * @todo test remote checksum missing
 * @todo test remote checksum present and correct
 * @todo test remote checksum present and incorrect
 * @todo test remote checksum transfer failed
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

    /**
     * A faster recursive copy that omits .svn directories.
     *
     * @param sourceDirectory the source directory to copy
     * @param destDirectory   the target location
     * @throws java.io.IOException if there is a copying problem
     * @todo get back into plexus-utils, share with indexing module
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
