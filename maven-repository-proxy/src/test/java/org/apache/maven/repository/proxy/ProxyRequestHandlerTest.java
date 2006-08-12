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
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Brett Porter
 * @todo! tests to do vvv
 * @todo test when >1 repo has the artifact
 * @todo test when >1 repo has the artifact but one fails
 * @todo test hard failure on repo1
 * @todo test when failure is cached
 * @todo test when failure should be cached but caching is disabled
 * @todo test snapshots - general
 * @todo test snapshots - newer version on repo2 is pulled down
 * @todo test snapshots - older version on repo2 is skipped
 * @todo test snapshots - update interval
 * @todo test metadata - general
 * @todo test metadata - multiple repos are merged
 * @todo test metadata - update interval
 * @todo test when managed repo is m1 layout (proxy is m2), including metadata
 * @todo test when one proxied repo is m1 layout (managed is m2), including metadata
 * @todo test when one proxied repo is m1 layout (managed is m1), including metadata
 * @todo test get always
 */
public class ProxyRequestHandlerTest
    extends PlexusTestCase
{
    private ProxyRequestHandler requestHandler;

    private List proxiedRepositories;

    private ArtifactRepository defaultManagedRepository;

    private ArtifactRepository proxiedRepository1;

    private ArtifactRepository proxiedRepository2;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        requestHandler = (ProxyRequestHandler) lookup( ProxyRequestHandler.ROLE );

        File repoLocation = getTestFile( "target/test-repository/managed" );
        FileUtils.deleteDirectory( repoLocation );
        copyDirectoryStructure( getTestFile( "src/test/repositories/managed" ), repoLocation );

        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
        ArtifactRepositoryLayout defaultLayout =
            (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        defaultManagedRepository = factory.createArtifactRepository( "managed-repository",
                                                                     repoLocation.toURI().toURL().toExternalForm(),
                                                                     defaultLayout, null, null );

        File location = getTestFile( "src/test/repositories/proxied1" );
        proxiedRepository1 = factory.createArtifactRepository( "proxied1", location.toURI().toURL().toExternalForm(),
                                                               defaultLayout, null, null );

        location = getTestFile( "src/test/repositories/proxied2" );
        proxiedRepository2 = factory.createArtifactRepository( "proxied2", location.toURI().toURL().toExternalForm(),
                                                               defaultLayout, null, null );

        proxiedRepositories = new ArrayList( 2 );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository1 ) );
        proxiedRepositories.add( createProxiedRepository( proxiedRepository2 ) );
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

    /**
     * A faster recursive copy that omits .svn directories.
     *
     * @param sourceDirectory the source directory to copy
     * @param destDirectory   the target location
     * @throws java.io.IOException if there is a copying problemt
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
        return proxiedArtifactRepository;
    }
}
