package org.apache.archiva.admin.repository.networkproxy;
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

import org.apache.archiva.admin.model.beans.NetworkProxy;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.networkproxy.NetworkProxyAdmin;
import org.apache.archiva.admin.repository.AbstractRepositoryAdminTest;
import org.apache.archiva.audit.AuditEvent;
import org.junit.Test;

import javax.inject.Inject;

/**
 * @author Olivier Lamy
 */
public class NetworkProxyAdminTest
    extends AbstractRepositoryAdminTest
{

    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    @Test
    public void getAllEmpty()
        throws Exception
    {
        assertNotNull( networkProxyAdmin.getNetworkProxies() );
    }

    @Test
    public void addAndDelete()
        throws Exception
    {
        mockAuditListener.clearEvents();
        int initialSize = networkProxyAdmin.getNetworkProxies().size();
        NetworkProxy networkProxy = getNetworkProxyTest( "foo" );

        networkProxyAdmin.addNetworkProxy( networkProxy, getFakeAuditInformation() );

        assertEquals( initialSize + 1, networkProxyAdmin.getNetworkProxies().size() );

        networkProxy = networkProxyAdmin.getNetworkProxy( "foo" );

        assertNotNull( networkProxy );
        assertEquals( getNetworkProxyTest( "foo" ).getId(), networkProxy.getId() );
        assertEquals( getNetworkProxyTest( "foo" ).getHost(), networkProxy.getHost() );
        assertEquals( getNetworkProxyTest( "foo" ).getPassword(), networkProxy.getPassword() );
        assertEquals( getNetworkProxyTest( "foo" ).getPort(), networkProxy.getPort() );
        assertEquals( getNetworkProxyTest( "foo" ).getUsername(), networkProxy.getUsername() );
        assertEquals( getNetworkProxyTest( "foo" ).getProtocol(), networkProxy.getProtocol() );

        networkProxyAdmin.deleteNetworkProxy( "foo", getFakeAuditInformation() );

        assertNull( networkProxyAdmin.getNetworkProxy( "foo" ) );

        assertEquals( 2, mockAuditListener.getAuditEvents().size() );

        assertEquals( AuditEvent.ADD_NETWORK_PROXY, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
        assertEquals( AuditEvent.DELETE_NETWORK_PROXY, mockAuditListener.getAuditEvents().get( 1 ).getAction() );

        mockAuditListener.clearEvents();
    }

    @Test
    public void addAndUpdateAndDelete()
        throws Exception
    {
        mockAuditListener.clearEvents();
        int initialSize = networkProxyAdmin.getNetworkProxies().size();
        NetworkProxy networkProxy = getNetworkProxyTest( "foo" );

        networkProxyAdmin.addNetworkProxy( networkProxy, getFakeAuditInformation() );

        assertEquals( initialSize + 1, networkProxyAdmin.getNetworkProxies().size() );

        networkProxy = networkProxyAdmin.getNetworkProxy( "foo" );

        assertNotNull( networkProxy );
        assertEquals( getNetworkProxyTest( "foo" ).getId(), networkProxy.getId() );
        assertEquals( getNetworkProxyTest( "foo" ).getHost(), networkProxy.getHost() );
        assertEquals( getNetworkProxyTest( "foo" ).getPassword(), networkProxy.getPassword() );
        assertEquals( getNetworkProxyTest( "foo" ).getPort(), networkProxy.getPort() );
        assertEquals( getNetworkProxyTest( "foo" ).getUsername(), networkProxy.getUsername() );
        assertEquals( getNetworkProxyTest( "foo" ).getProtocol(), networkProxy.getProtocol() );

        networkProxy.setHost( "https://toto.com" );
        networkProxy.setPassword( "newpasswd" );
        networkProxy.setPort( 9191 );
        networkProxy.setProtocol( "http" );
        networkProxy.setUsername( "newusername" );

        networkProxyAdmin.updateNetworkProxy( networkProxy, getFakeAuditInformation() );

        NetworkProxy updatedNetworkProxy = networkProxyAdmin.getNetworkProxy( "foo" );

        assertNotNull( updatedNetworkProxy );
        assertEquals( networkProxy.getId(), updatedNetworkProxy.getId() );
        assertEquals( networkProxy.getHost(), updatedNetworkProxy.getHost() );
        assertEquals( networkProxy.getPassword(), updatedNetworkProxy.getPassword() );
        assertEquals( networkProxy.getPort(), updatedNetworkProxy.getPort() );
        assertEquals( networkProxy.getUsername(), updatedNetworkProxy.getUsername() );
        assertEquals( networkProxy.getProtocol(), updatedNetworkProxy.getProtocol() );

        networkProxyAdmin.deleteNetworkProxy( "foo", getFakeAuditInformation() );

        assertEquals( 3, mockAuditListener.getAuditEvents().size() );

        assertEquals( AuditEvent.ADD_NETWORK_PROXY, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
        assertEquals( AuditEvent.MODIFY_NETWORK_PROXY, mockAuditListener.getAuditEvents().get( 1 ).getAction() );
        assertEquals( AuditEvent.DELETE_NETWORK_PROXY, mockAuditListener.getAuditEvents().get( 2 ).getAction() );

        mockAuditListener.clearEvents();
    }

    /**
     * ensure we cleanup remote repos linked to a network proxy
     */
    @Test
    public void addAndDeleteWithRemoteRepoLinked()
        throws Exception
    {
        mockAuditListener.clearEvents();
        int initialSize = networkProxyAdmin.getNetworkProxies().size();
        NetworkProxy networkProxy = getNetworkProxyTest( "foo" );

        networkProxyAdmin.addNetworkProxy( networkProxy, getFakeAuditInformation() );

        assertEquals( initialSize + 1, networkProxyAdmin.getNetworkProxies().size() );

        networkProxy = networkProxyAdmin.getNetworkProxy( "foo" );

        assertNotNull( networkProxy );

        RemoteRepository remoteRepository = getRemoteRepository();
        remoteRepository.setRemoteDownloadNetworkProxyId( networkProxy.getId() );

        remoteRepositoryAdmin.addRemoteRepository( remoteRepository, getFakeAuditInformation() );

        networkProxyAdmin.deleteNetworkProxy( "foo", getFakeAuditInformation() );

        remoteRepository = remoteRepositoryAdmin.getRemoteRepository( getRemoteRepository().getId() );

        assertNull( remoteRepository.getRemoteDownloadNetworkProxyId() );

        remoteRepositoryAdmin.deleteRemoteRepository( getRemoteRepository().getId(), getFakeAuditInformation() );
    }


    protected NetworkProxy getNetworkProxyTest( String id )
    {
        NetworkProxy networkProxy = new NetworkProxy();
        networkProxy.setId( "foo" );
        networkProxy.setHost( "http://foo.com" );
        networkProxy.setPassword( "passwd" );
        networkProxy.setPort( 9090 );
        networkProxy.setUsername( "root" );
        networkProxy.setProtocol( "https" );
        return networkProxy;
    }

}
