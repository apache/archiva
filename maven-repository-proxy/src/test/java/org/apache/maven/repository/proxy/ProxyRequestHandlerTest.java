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

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class ProxyRequestHandlerTest
    extends PlexusTestCase
{
    private ProxyRequestHandler requestHandler;

    public void testdummy(){}

/* TODO!
    protected void setUp()
        throws Exception
    {
        super.setUp();

        requestHandler = (ProxyRequestHandler) container.lookup( ProxyRequestHandler.ROLE );
    }

    public void testArtifactDownload()
        throws Exception
    {
        //test download
        String s = "/commons-logging/commons-logging/1.0/commons-logging-1.0.jar";
        File file = get( s );
        assertTrue( "File must be downloaded.", file.exists() );
        assertTrue( "Downloaded file should be present in the cache.",
                    file.getAbsolutePath().startsWith( managedRepository.getBasedir() ) );

        //test cache
        get( "/commons-logging/commons-logging/1.0/commons-logging-1.0.jar" );

        try
        {
            get( "/commons-logging/commons-logging/2.0/commons-logging-2.0.jar" );
            fail( "Expected ResourceDoesNotExistException exception not thrown" );
        }
        catch ( ResourceDoesNotExistException e )
        {
            assertTrue( true );
        }
    }

    private File get( String s )
        throws ProxyException, ResourceDoesNotExistException
    {
        return requestHandler.get( s, proxiedRepositories, managedRepository );
    }

    public void testArtifactChecksum()
        throws Exception
    {
        //force the downlod from the remote repository, use getAlways()
        File file = requestHandler.getAlways( "/commons-logging/commons-logging/1.0/commons-logging-1.0.jar.md5" );
        assertTrue( "File must be downloaded.", file.exists() );
        assertTrue( "Downloaded file should be present in the cache.",
                    file.getAbsolutePath().startsWith( managedRepository.getBasedir() ) );
    }

    public void testNonArtifactWithNoChecksum()
        throws Exception
    {
        File file = get( "/not-standard/repository/file.txt" );
        assertTrue( "File must be downloaded.", file.exists() );
        assertTrue( "Downloaded file should be present in the cache.",
                    file.getAbsolutePath().startsWith( managedRepository.getBasedir() ) );
    }

    public void testNonArtifactWithMD5Checksum()
        throws Exception
    {
        File file = get( "/checksumed-md5/repository/file.txt" );
        assertTrue( "File must be downloaded.", file.exists() );
        assertTrue( "Downloaded file should be present in the cache.",
                    file.getAbsolutePath().startsWith( managedRepository.getBasedir() ) );
    }

    public void testNonArtifactWithSHA1Checksum()
        throws Exception
    {
        File file = get( "/checksumed-sha1/repository/file.txt" );
        assertTrue( "File must be downloaded.", file.exists() );
        assertTrue( "Downloaded file should be present in the cache.",
                    file.getAbsolutePath().startsWith( managedRepository.getBasedir() ) );
    }

    private ProxyConfiguration getProxyConfiguration()
        throws ComponentLookupException
    {
        ProxyConfiguration config = new ProxyConfiguration();

        config.setRepositoryCachePath( "target/requestHandler-cache" );

        ArtifactRepositoryLayout defLayout = new DefaultRepositoryLayout();

        File repo1File = getTestFile( "src/test/remote-repo1" );

        ProxyRepository repo1 = new ProxyRepository( "test-repo", "file://" + repo1File.getAbsolutePath(), defLayout );

        config.addRepository( repo1 );

        return config;
    }
*/
}
