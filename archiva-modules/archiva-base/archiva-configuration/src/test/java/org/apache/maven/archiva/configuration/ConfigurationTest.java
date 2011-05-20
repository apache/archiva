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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

/**
 * Test the generated Configuration class from Modello. This is primarily to test the hand coded methods.
 */
@RunWith( JUnit4.class )
public class ConfigurationTest
    extends TestCase
{
    private Configuration configuration = new Configuration();

    @Test
    public void testNetworkProxyRetrieval()
    {
        NetworkProxyConfiguration proxy1 = createNetworkProxy( "id1", "host1", 8080 );
        configuration.addNetworkProxy( proxy1 );
        NetworkProxyConfiguration proxy2 = createNetworkProxy( "id2", "host2", 9090 );
        configuration.addNetworkProxy( proxy2 );

        Map<String, NetworkProxyConfiguration> map = configuration.getNetworkProxiesAsMap();
        assertNotNull( map );
        assertEquals( 2, map.size() );
        assertEquals( new HashSet<String>( Arrays.asList( "id1", "id2" ) ), map.keySet() );
        assertEquals( new HashSet<NetworkProxyConfiguration>( Arrays.asList( proxy1, proxy2 ) ),
                      new HashSet<NetworkProxyConfiguration>( map.values() ) );
    }

    private NetworkProxyConfiguration createNetworkProxy( String id, String host, int port )
    {
        NetworkProxyConfiguration proxy = new NetworkProxyConfiguration();
        proxy.setId( id );
        proxy.setHost( host );
        proxy.setPort( port );
        proxy.setProtocol( "http" );
        return proxy;
    }

    @Test
    public void testRemoteRepositoryRetrieval()
    {
        RemoteRepositoryConfiguration repo1 = createRemoteRepository( "id1", "name 1", "url 1" );
        configuration.addRemoteRepository( repo1 );
        RemoteRepositoryConfiguration repo2 = createRemoteRepository( "id2", "name 2", "url 2" );
        configuration.addRemoteRepository( repo2 );

        Map<String, RemoteRepositoryConfiguration> map = configuration.getRemoteRepositoriesAsMap();
        assertNotNull( map );
        assertEquals( 2, map.size() );
        assertEquals( new HashSet<String>( Arrays.asList( "id1", "id2" ) ), map.keySet() );
        assertEquals( new HashSet<RemoteRepositoryConfiguration>( Arrays.asList( repo1, repo2 ) ),
                      new HashSet<RemoteRepositoryConfiguration>( map.values() ) );

        assertEquals( repo1, configuration.findRemoteRepositoryById( "id1" ) );
        assertEquals( repo2, configuration.findRemoteRepositoryById( "id2" ) );
        assertNull( configuration.findRemoteRepositoryById( "id3" ) );
    }

    private RemoteRepositoryConfiguration createRemoteRepository( String id, String name, String url )
    {
        RemoteRepositoryConfiguration repo = new RemoteRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLayout( "default" );
        repo.setUrl( url );
        return repo;
    }

    @Test
    public void testManagedRepositoryRetrieval()
    {
        ManagedRepositoryConfiguration repo1 = createManagedRepository( "id1", "name 1", "path 1", false );
        configuration.addManagedRepository( repo1 );
        ManagedRepositoryConfiguration repo2 = createManagedRepository( "id2", "name 2", "path 2", true );
        configuration.addManagedRepository( repo2 );

        Map<String, ManagedRepositoryConfiguration> map = configuration.getManagedRepositoriesAsMap();
        assertNotNull( map );
        assertEquals( 2, map.size() );
        assertEquals( new HashSet<String>( Arrays.asList( "id1", "id2" ) ), map.keySet() );
        assertEquals( new HashSet<ManagedRepositoryConfiguration>( Arrays.asList( repo1, repo2 ) ),
                      new HashSet<ManagedRepositoryConfiguration>( map.values() ) );

        assertEquals( repo1, configuration.findManagedRepositoryById( "id1" ) );
        assertEquals( repo2, configuration.findManagedRepositoryById( "id2" ) );
        assertNull( configuration.findManagedRepositoryById( "id3" ) );
    }

    private ManagedRepositoryConfiguration createManagedRepository( String id, String name, String location,
                                                                    boolean scanned )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location );
        repo.setScanned( scanned );
        return repo;
    }

    @Test
    public void testNetworkProxyRetrievalWhenEmpty()
    {
        Map<String, NetworkProxyConfiguration> map = configuration.getNetworkProxiesAsMap();
        assertNotNull( map );
        assertTrue( map.isEmpty() );
    }

    @Test
    public void testRemoteRepositoryRetrievalWhenEmpty()
    {
        Map<String, RemoteRepositoryConfiguration> map = configuration.getRemoteRepositoriesAsMap();
        assertNotNull( map );
        assertTrue( map.isEmpty() );

        assertNull( configuration.findRemoteRepositoryById( "id" ) );
    }

    @Test
    public void testManagedRepositoryRetrievalWhenEmpty()
    {
        Map<String, ManagedRepositoryConfiguration> map = configuration.getManagedRepositoriesAsMap();
        assertNotNull( map );
        assertTrue( map.isEmpty() );

        assertNull( configuration.findManagedRepositoryById( "id" ) );
    }
}
