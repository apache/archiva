package org.apache.maven.repository.proxy.configuration;

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
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.layout.LegacyRepositoryLayout;
import org.apache.maven.repository.proxy.repository.ProxyRepository;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProxyConfigurationTest
    extends PlexusTestCase
{
    private ProxyConfiguration config;

    private static final int DEFAULT_CACHE_PERIOD = 3600;

    private static final int DEFAULT_PORT = 80;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        config = new ProxyConfiguration();
    }

    public void testRepositoryCache()
    {
        File cacheFile = new File( "target/proxy-cache" );
        config.setRepositoryCachePath( cacheFile.getAbsolutePath() );
        assertEquals( config.getRepositoryCachePath(), cacheFile.getAbsolutePath() );
    }

    public void testRepositories()
    {
        ArtifactRepositoryLayout defLayout = new DefaultRepositoryLayout();
        ProxyRepository repo1 = new ProxyRepository( "repo1", "http://www.ibiblio.org/maven2", defLayout );
        repo1.setCacheFailures( true );
        repo1.setCachePeriod( 0 );
        repo1.setHardfail( true );
        config.addRepository( repo1 );
        assertEquals( 1, config.getRepositories().size() );

        ArtifactRepositoryLayout legacyLayout = new LegacyRepositoryLayout();
        ProxyRepository repo2 = new ProxyRepository( "repo2", "http://www.ibiblio.org/maven", legacyLayout );
        repo2.setCacheFailures( false );
        repo2.setCachePeriod( DEFAULT_CACHE_PERIOD );
        repo2.setProxied( true );
        config.setHttpProxy( "some.local.proxy", DEFAULT_PORT, "username", "password" );
        config.addRepository( repo2 );
        assertEquals( 2, config.getRepositories().size() );

        List repositories = config.getRepositories();
        ProxyRepository repo = (ProxyRepository) repositories.get( 0 );
        assertEquals( "repo1", repo.getId() );
        assertEquals( "http://www.ibiblio.org/maven2", repo.getUrl() );
        assertTrue( repo.isCacheFailures() );
        assertTrue( repo.isHardfail() );
        assertEquals( 0, repo.getCachePeriod() );
        assertEquals( repo1, repo );

        repo = (ProxyRepository) repositories.get( 1 );
        assertEquals( "repo2", repo.getId() );
        assertEquals( "http://www.ibiblio.org/maven", repo.getUrl() );
        assertFalse( repo.isCacheFailures() );
        assertTrue( repo.isHardfail() );
        assertEquals( DEFAULT_CACHE_PERIOD, repo.getCachePeriod() );
        assertEquals( repo2, repo );
        assertTrue( repo.isProxied() );

        ProxyInfo proxyInfo = config.getHttpProxy();
        assertNotNull( proxyInfo );
        assertEquals( "some.local.proxy", proxyInfo.getHost() );
        assertEquals( DEFAULT_PORT, proxyInfo.getPort() );
        assertEquals( "username", proxyInfo.getUserName() );
        assertEquals( "password", proxyInfo.getPassword() );

        try
        {
            repositories.add( new ProxyRepository( "repo", "url", defLayout ) );
            fail( "Expected UnsupportedOperationException not thrown." );
        }
        catch ( UnsupportedOperationException e )
        {
            assertTrue( true );
        }

        repositories = new ArrayList();
        repositories.add( repo1 );
        repositories.add( repo2 );
        config.setRepositories( repositories );
        assertEquals( repositories, config.getRepositories() );
    }

    protected void tearDown()
        throws Exception
    {
        config = null;

        super.tearDown();
    }
}