package org.apache.archiva.configuration;

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

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 */
@RunWith( JUnit4.class )
public class MavenProxyPropertyLoaderTest
    extends TestCase
{
    private MavenProxyPropertyLoader loader;


    // TODO to remove
    protected String getSpringConfigLocation()
    {
        return "org/apache/maven/archiva/configuration/spring-context.xml";
    }

    @Test
    public void testLoadValidMavenProxyConfiguration()
        throws IOException, InvalidConfigurationException
    {
        File confFile = ArchivaConfigurationTest.getTestFile( "src/test/conf/maven-proxy-complete.conf" );

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

    @Test
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

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        loader = new MavenProxyPropertyLoader();
    }
}
