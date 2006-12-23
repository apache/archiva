package org.apache.maven.archiva.configuration;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * @author Edwin Punzalan
 */
public class MavenProxyPropertyLoaderTest
    extends PlexusTestCase
{
    private static final int DEFAULT_CACHE_PERIOD = 3600;

    private MavenProxyPropertyLoader loader;

    public void testLoadValidMavenProxyConfiguration()
        throws IOException, InvalidConfigurationException
    {
        File confFile = getTestFile( "src/test/conf/maven-proxy-complete.conf" );

        Configuration configuration = new Configuration();
        Proxy proxy = new Proxy();
        proxy.setHost( "original-host" );
        configuration.setProxy( proxy ); // overwritten
        configuration.setIndexPath( "index-path" ); // existing value

        loader.load( new FileInputStream( confFile ), configuration );

        List list = configuration.getRepositories();
        assertEquals( "check single managed repository", 1, list.size() );
        RepositoryConfiguration managedRepository = (RepositoryConfiguration) list.iterator().next();
        assertEquals( "cache path changed", "target", managedRepository.getDirectory() );

        assertEquals( "Count repositories", 4, configuration.getProxiedRepositories().size() );

        list = configuration.getProxiedRepositories();
        ProxiedRepositoryConfiguration repo = (ProxiedRepositoryConfiguration) list.get( 0 );
        assertEquals( "Repository name not as expected", "local-repo", repo.getId() );
        assertEquals( "Repository url does not match its name", "file://target", repo.getUrl() );
        assertEquals( "Repository cache period check failed", 0, repo.getSnapshotsInterval() );
        assertFalse( "Repository failure caching check failed", repo.isCacheFailures() );

        repo = (ProxiedRepositoryConfiguration) list.get( 1 );
        assertEquals( "Repository name not as expected", "www-ibiblio-org", repo.getId() );
        assertEquals( "Repository url does not match its name", "http://www.ibiblio.org/maven2", repo.getUrl() );
        assertEquals( "Repository cache period check failed", DEFAULT_CACHE_PERIOD, repo.getSnapshotsInterval() );
        assertTrue( "Repository failure caching check failed", repo.isCacheFailures() );

        repo = (ProxiedRepositoryConfiguration) list.get( 2 );
        assertEquals( "Repository name not as expected", "dist-codehaus-org", repo.getId() );
        assertEquals( "Repository url does not match its name", "http://dist.codehaus.org", repo.getUrl() );
        assertEquals( "Repository cache period check failed", DEFAULT_CACHE_PERIOD, repo.getSnapshotsInterval() );
        assertTrue( "Repository failure caching check failed", repo.isCacheFailures() );

        repo = (ProxiedRepositoryConfiguration) list.get( 3 );
        assertEquals( "Repository name not as expected", "private-example-com", repo.getId() );
        assertEquals( "Repository url does not match its name", "http://private.example.com/internal", repo.getUrl() );
        assertEquals( "Repository cache period check failed", DEFAULT_CACHE_PERIOD, repo.getSnapshotsInterval() );
        assertFalse( "Repository failure caching check failed", repo.isCacheFailures() );
    }

    public void testInvalidConfiguration()
    {
        Configuration configuration = new Configuration();
        try
        {
            loader.load( new Properties(), configuration );
            fail( "Incomplete config should have failed" );
        }
        catch ( InvalidConfigurationException e )
        {
            assertTrue( true );
        }
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        loader = new MavenProxyPropertyLoader();
    }
}
