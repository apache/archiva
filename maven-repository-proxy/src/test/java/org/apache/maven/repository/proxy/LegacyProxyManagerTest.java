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

import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.LegacyRepositoryLayout;
import org.apache.maven.repository.proxy.configuration.ProxyConfiguration;
import org.apache.maven.repository.proxy.repository.ProxyRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class LegacyProxyManagerTest
    extends PlexusTestCase
{
    private ProxyManager proxy;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ProxyManagerFactory factory = (ProxyManagerFactory) container.lookup( ProxyManagerFactory.ROLE );
        proxy = factory.getProxyManager( "default", getTestConfiguration() );
    }

    public void testExceptions()
    {
        proxy.setConfiguration( null );

        try
        {
            proxy.get( "/invalid" );
            fail( "Expected empty configuration error." );
        }
        catch ( ProxyException e )
        {
            assertEquals( "Expected Exception not thrown.", "No proxy configuration defined.", e.getMessage() );
        }
        catch ( ResourceDoesNotExistException e )
        {
            fail( "Expected Exception not thrown." );
        }
    }

    public void testArtifactDownload()
        throws Exception
    {
        //test download
        File file = proxy.get( "/commons-logging/jars/commons-logging-1.0.jar" );
        assertTrue( "File must be downloaded.", file.exists() );
        assertTrue( "Downloaded file should be present in the cache.",
                    file.getAbsolutePath().startsWith( proxy.getConfiguration().getRepositoryCachePath() ) );

        //test cache
        file = proxy.get( "/commons-logging/jars/commons-logging-1.0.jar" );

        try
        {
            file = proxy.get( "/commons-logging/jars/commons-logging-2.0.jar" );
            fail( "Expected ResourceDoesNotExistException exception not thrown" );
        }
        catch ( ResourceDoesNotExistException e )
        {
            assertTrue( true );
        }
    }

    public void testArtifactChecksum()
        throws Exception
    {
        //force the downlod from the remote repository, use getRemoteFile()
        File file = proxy.getRemoteFile( "/commons-logging/jars/commons-logging-1.0.jar.md5" );
        assertTrue( "File must be downloaded.", file.exists() );
        assertTrue( "Downloaded file should be present in the cache.",
                    file.getAbsolutePath().startsWith( proxy.getConfiguration().getRepositoryCachePath() ) );
    }

    public void testNonArtifactWithNoChecksum()
        throws Exception
    {
        File file = proxy.get( "/not-standard/repository/file.txt" );
        assertTrue( "File must be downloaded.", file.exists() );
        assertTrue( "Downloaded file should be present in the cache.",
                    file.getAbsolutePath().startsWith( proxy.getConfiguration().getRepositoryCachePath() ) );
    }

    public void testNonArtifactWithMD5Checksum()
        throws Exception
    {
        File file = proxy.get( "/checksumed-md5/repository/file.txt" );
        assertTrue( "File must be downloaded.", file.exists() );
        assertTrue( "Downloaded file should be present in the cache.",
                    file.getAbsolutePath().startsWith( proxy.getConfiguration().getRepositoryCachePath() ) );
    }

    public void testNonArtifactWithSHA1Checksum()
        throws Exception
    {
        File file = proxy.get( "/checksumed-sha1/repository/file.txt" );
        assertTrue( "File must be downloaded.", file.exists() );
        assertTrue( "Downloaded file should be present in the cache.",
                    file.getAbsolutePath().startsWith( proxy.getConfiguration().getRepositoryCachePath() ) );
    }

    protected void tearDown()
        throws Exception
    {
        container.release( proxy );

        super.tearDown();
    }

    private ProxyConfiguration getTestConfiguration()
        throws ComponentLookupException
    {
        ProxyConfiguration config = (ProxyConfiguration) container.lookup( ProxyConfiguration.ROLE );

        config.setRepositoryCachePath( "target/proxy-cache" );

        ArtifactRepositoryLayout layout = new LegacyRepositoryLayout();

        File repo1File = getTestFile( "src/test/m1-remote-repo" );

        ProxyRepository repo1 = new ProxyRepository( "m1-test-repo", "file://" + repo1File.getAbsolutePath(), layout );

        config.addRepository( repo1 );

        return config;
    }
}
