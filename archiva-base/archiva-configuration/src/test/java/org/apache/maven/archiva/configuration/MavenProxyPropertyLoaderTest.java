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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Edwin Punzalan
 */
public class MavenProxyPropertyLoaderTest extends PlexusTestCase
{
    private MavenProxyPropertyLoader loader;

    public void testLoadValidMavenProxyConfiguration() throws IOException, InvalidConfigurationException
    {
        File confFile = getTestFile( "src/test/conf/maven-proxy-complete.conf" );

        Configuration configuration = new Configuration();
        NetworkProxyConfiguration proxy = new NetworkProxyConfiguration();
        proxy.setHost( "original-host" );
        configuration.addNetworkProxy( proxy ); // overwritten

        loader.load( new FileInputStream( confFile ), configuration );

        List repos = configuration.getRepositories();
        assertEquals( "Count repositories", 5, repos.size() );

        Map repositoryIdMap = new HashMap();

        for ( Iterator itRepo = repos.iterator(); itRepo.hasNext(); )
        {
            RepositoryConfiguration repo = (RepositoryConfiguration) itRepo.next();
            repositoryIdMap.put( repo.getId(), repo );
        }

        assertRepositoryExists( repositoryIdMap, "local-repo", "file://target" );

        assertRepositoryExists( repositoryIdMap, "www-ibiblio-org", "http://www.ibiblio.org/maven2" );
        assertRepositoryExists( repositoryIdMap, "dist-codehaus-org", "http://dist.codehaus.org" );
        assertRepositoryExists( repositoryIdMap, "private-example-com", "http://private.example.com/internal" );
    }

    private void assertRepositoryExists( Map repoMap, String id, String expectedUrl )
    {
        RepositoryConfiguration repo = (RepositoryConfiguration) repoMap.get( id );
        assertNotNull( "Repository id [" + id + "] should not be null", repo );
        assertEquals( "Repository id", id, repo.getId() );
        assertEquals( "Repository url", expectedUrl, repo.getUrl() );
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

    protected void setUp() throws Exception
    {
        super.setUp();
        loader = new MavenProxyPropertyLoader();
    }
}
