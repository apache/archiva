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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.layout.LegacyRepositoryLayout;
import org.apache.maven.repository.proxy.repository.ProxyRepository;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class ProxyConfigurationTest
    extends PlexusTestCase
{
    private ProxyConfiguration config;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        config = (ProxyConfiguration) container.lookup( ProxyConfiguration.ROLE );
    }

    public void testBrowsable()
    {
        assertFalse( config.isBrowsable() );
        config.setBrowsable( true );
        assertTrue( config.isBrowsable() );
    }

    public void testRepositoryCache()
    {
        File cacheFile = new File( "target/proxy-cache" );
        config.setRepositoryCachePath( cacheFile.getAbsolutePath() );
        ArtifactRepository cache = config.getRepositoryCache();
        assertEquals( cacheFile.getAbsolutePath(), cache.getBasedir() );
        assertEquals( config.getRepositoryCachePath(), cache.getBasedir() );
    }

    public void testRepositories()
    {
        ArtifactRepositoryLayout defLayout = new DefaultRepositoryLayout();
        ProxyRepository repo1 = new ProxyRepository( "repo1", "http://www.ibiblio.org/maven2", defLayout );
        config.addRepository( repo1 );
        assertEquals( 1, config.getRepositories().size() );

        ArtifactRepositoryLayout legacyLayout = new LegacyRepositoryLayout();
        ProxyRepository repo2 = new ProxyRepository( "repo2", "http://www.ibiblio.org/maven", legacyLayout );
        config.addRepository( repo2 );
        assertEquals( 2, config.getRepositories().size() );

        List repositories = config.getRepositories();
        ProxyRepository repo = (ProxyRepository) repositories.get( 0 );
        assertEquals( repo1, repo );

        repo = (ProxyRepository) repositories.get( 1 );
        assertEquals( repo2, repo );

        try
        {
            repositories.add( new ProxyRepository( "repo", "url", defLayout ) );
            fail( "Expected UnsupportedOperationException not thrown." );
        }
        catch ( java.lang.UnsupportedOperationException e )
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