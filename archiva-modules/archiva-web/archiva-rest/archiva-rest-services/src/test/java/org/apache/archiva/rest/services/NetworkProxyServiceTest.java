package org.apache.archiva.rest.services;
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

import org.apache.archiva.rest.api.model.NetworkProxy;
import org.junit.Test;

/**
 * @author Olivier Lamy
 */
public class NetworkProxyServiceTest
    extends AbstractArchivaRestTest
{
    @Test
    public void addAndDelete()
        throws Exception
    {
        assertNotNull( getNetworkProxyService().getNetworkProxies() );
        assertTrue( getNetworkProxyService().getNetworkProxies().isEmpty() );

        getNetworkProxyService().addNetworkProxy( getNetworkProxy( "foo" ) );

        assertNotNull( getNetworkProxyService().getNetworkProxies() );
        assertFalse( getNetworkProxyService().getNetworkProxies().isEmpty() );
        assertEquals( 1, getNetworkProxyService().getNetworkProxies().size() );

        getNetworkProxyService().deleteNetworkProxy( "foo" );

        assertNotNull( getNetworkProxyService().getNetworkProxies() );
        assertTrue( getNetworkProxyService().getNetworkProxies().isEmpty() );

    }

    @Test
    public void addAndUpdateAndDelete()
        throws Exception
    {
        assertNotNull( getNetworkProxyService().getNetworkProxies() );
        assertTrue( getNetworkProxyService().getNetworkProxies().isEmpty() );

        getNetworkProxyService().addNetworkProxy( getNetworkProxy( "foo" ) );

        assertNotNull( getNetworkProxyService().getNetworkProxies() );
        assertFalse( getNetworkProxyService().getNetworkProxies().isEmpty() );
        assertEquals( 1, getNetworkProxyService().getNetworkProxies().size() );

        NetworkProxy networkProxy = getNetworkProxy( "foo" );
        networkProxy.setHost( "http://toto.com" );
        networkProxy.setPassword( "newpasswd" );
        networkProxy.setUsername( "newusername" );
        networkProxy.setPort( 9191 );

        getNetworkProxyService().updateNetworkProxy( networkProxy );

        assertEquals( networkProxy.getHost(), getNetworkProxyService().getNetworkProxy( "foo" ).getHost() );
        assertEquals( networkProxy.getPassword(), getNetworkProxyService().getNetworkProxy( "foo" ).getPassword() );
        assertEquals( networkProxy.getUsername(), getNetworkProxyService().getNetworkProxy( "foo" ).getUsername() );
        assertEquals( networkProxy.getPort(), getNetworkProxyService().getNetworkProxy( "foo" ).getPort() );

        getNetworkProxyService().deleteNetworkProxy( "foo" );

        assertNotNull( getNetworkProxyService().getNetworkProxies() );
        assertTrue( getNetworkProxyService().getNetworkProxies().isEmpty() );

    }

    NetworkProxy getNetworkProxy( String id )
    {

        NetworkProxy networkProxy = new NetworkProxy();
        networkProxy.setId( id );
        networkProxy.setHost( "http://foo.com" );
        networkProxy.setPassword( "passwd" );
        networkProxy.setUsername( "username" );
        networkProxy.setPort( 9090 );
        return networkProxy;
    }
}
