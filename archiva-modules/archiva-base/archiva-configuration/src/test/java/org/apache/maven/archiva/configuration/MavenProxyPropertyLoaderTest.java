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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.codehaus.plexus.spring.PlexusInSpringTestCase;

/**
 * @author Edwin Punzalan
 */
public class MavenProxyPropertyLoaderTest
    extends PlexusInSpringTestCase
{
    private MavenProxyPropertyLoader loader;

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.spring.PlexusInSpringTestCase#getSpringConfigLocation()
     */
    protected String getSpringConfigLocation()
        throws Exception
    {
        return "org/apache/maven/archiva/configuration/spring-context.xml";
    }

    public void testLoadValidMavenProxyConfiguration()
        throws IOException, InvalidConfigurationException
    {
        File confFile = getTestFile( "src/test/conf/maven-proxy-complete.conf" );

        Configuration configuration = new Configuration();
        NetworkProxyConfiguration proxy = new NetworkProxyConfiguration();
        proxy.setHost( "original-host" );
        configuration.addNetworkProxy( proxy ); // overwritten

        loader.load( new FileInputStream( confFile ), configuration );

        Map<String, ManagedRepositoryConfiguration> repositoryIdMap = configuration.getManagedRepositoriesAsMap();
        assertEquals( "Count repositories", 1, repositoryIdMap.size() );
        assertRepositoryExists( "maven-proxy", "target", repositoryIdMap.get( "maven-proxy" ) );

        Map<String, RemoteRepositoryConfiguration> remoteRepositoryMap = configuration.getRemoteRepositoriesAsMap();
        assertEquals( "Count repositories", 4, remoteRepositoryMap.size() );
        assertRepositoryExists( "local-repo", "file://target", remoteRepositoryMap.get( "local-repo" ) );
        assertRepositoryExists( "www-ibiblio-org", "http://www.ibiblio.org/maven2",
                                remoteRepositoryMap.get( "www-ibiblio-org" ) );
        assertRepositoryExists( "dist-codehaus-org", "http://dist.codehaus.org",
                                remoteRepositoryMap.get( "dist-codehaus-org" ) );
        assertRepositoryExists( "private-example-com", "http://private.example.com/internal",
                                remoteRepositoryMap.get( "private-example-com" ) );
    }

    private void assertRepositoryExists( String id, String expectedLocation, ManagedRepositoryConfiguration repo )
    {
        assertNotNull( "Repository id [" + id + "] should not be null", repo );
        assertEquals( "Repository id", id, repo.getId() );
        assertEquals( "Repository url", expectedLocation, repo.getLocation() );
    }

    private void assertRepositoryExists( String id, String expectedUrl, RemoteRepositoryConfiguration repo )
    {
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

    protected void setUp()
        throws Exception
    {
        super.setUp();
        loader = new MavenProxyPropertyLoader();
    }
}
