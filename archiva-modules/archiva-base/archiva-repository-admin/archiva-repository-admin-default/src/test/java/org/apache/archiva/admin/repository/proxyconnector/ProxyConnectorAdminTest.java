package org.apache.archiva.admin.repository.proxyconnector;
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

import org.apache.archiva.admin.model.proxyconnector.ProxyConnector;
import org.apache.archiva.admin.model.remote.RemoteRepository;
import org.apache.archiva.admin.repository.AbstractRepositoryAdminTest;
import org.apache.archiva.audit.AuditEvent;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Olivier Lamy
 */
public class ProxyConnectorAdminTest
    extends AbstractRepositoryAdminTest
{

    @Test
    public void addAndDelete()
        throws Exception
    {
        mockAuditListener.clearEvents();
        assertEquals( "not proxyConnectors 2 " + proxyConnectorAdmin.getProxyConnectors(), 2,
                      proxyConnectorAdmin.getProxyConnectors().size() );
        assertFalse( proxyConnectorAdmin.getProxyConnectors().isEmpty() );
        ProxyConnector proxyConnector = new ProxyConnector();
        proxyConnector.setSourceRepoId( "snapshots" );
        proxyConnector.setTargetRepoId( "central" );
        proxyConnectorAdmin.addProxyConnector( proxyConnector, getFakeAuditInformation() );

        assertFalse( proxyConnectorAdmin.getProxyConnectors().isEmpty() );
        assertEquals( 3, proxyConnectorAdmin.getProxyConnectors().size() );

        assertNotNull( proxyConnectorAdmin.getProxyConnector( "snapshots", "central" ) );

        proxyConnectorAdmin.deleteProxyConnector( proxyConnector, getFakeAuditInformation() );

        assertEquals( 2, proxyConnectorAdmin.getProxyConnectors().size() );
        assertFalse( proxyConnectorAdmin.getProxyConnectors().isEmpty() );

        assertEquals( 2, mockAuditListener.getAuditEvents().size() );

        assertEquals( AuditEvent.ADD_PROXY_CONNECTOR, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
        assertEquals( "root", mockAuditListener.getAuditEvents().get( 0 ).getUserId() );
        assertEquals( "archiva-localhost", mockAuditListener.getAuditEvents().get( 0 ).getRemoteIP() );

        assertEquals( AuditEvent.DELETE_PROXY_CONNECTOR, mockAuditListener.getAuditEvents().get( 1 ).getAction() );
        assertEquals( "root", mockAuditListener.getAuditEvents().get( 1 ).getUserId() );

        assertNull( proxyConnectorAdmin.getProxyConnector( "snapshots", "central" ) );

        mockAuditListener.clearEvents();
    }

    @Test
    public void addAndUpdateAndDelete()
        throws Exception
    {
        mockAuditListener.clearEvents();
        RemoteRepository remoteRepository = getRemoteRepository( "test-new-one" );

        remoteRepositoryAdmin.addRemoteRepository( remoteRepository, getFakeAuditInformation() );

        assertEquals( "not proxyConnectors 2 " + proxyConnectorAdmin.getProxyConnectors(), 2,
                      proxyConnectorAdmin.getProxyConnectors().size() );
        assertFalse( proxyConnectorAdmin.getProxyConnectors().isEmpty() );
        ProxyConnector proxyConnector = new ProxyConnector();
        proxyConnector.setSourceRepoId( "snapshots" );
        proxyConnector.setTargetRepoId( "central" );
        proxyConnector.setWhiteListPatterns( Arrays.asList( "foo", "bar" ) );
        proxyConnectorAdmin.addProxyConnector( proxyConnector, getFakeAuditInformation() );

        assertFalse( proxyConnectorAdmin.getProxyConnectors().isEmpty() );
        assertEquals( 3, proxyConnectorAdmin.getProxyConnectors().size() );

        assertNotNull( proxyConnectorAdmin.getProxyConnector( "snapshots", "central" ) );
        assertEquals( Arrays.asList( "foo", "bar" ),
                      proxyConnectorAdmin.getProxyConnector( "snapshots", "central" ).getWhiteListPatterns() );

        proxyConnectorAdmin.deleteProxyConnector( proxyConnector, getFakeAuditInformation() );

        proxyConnector.setTargetRepoId( remoteRepository.getId() );
        proxyConnectorAdmin.addProxyConnector( proxyConnector, getFakeAuditInformation() );

        assertNull( proxyConnectorAdmin.getProxyConnector( "snapshots", "central" ) );
        assertNotNull( remoteRepository.getId(),
                       proxyConnectorAdmin.getProxyConnector( "snapshots", remoteRepository.getId() ) );

        proxyConnectorAdmin.deleteProxyConnector( proxyConnector, getFakeAuditInformation() );

        assertNull( proxyConnectorAdmin.getProxyConnector( "snapshots", "central" ) );

        assertEquals( 2, proxyConnectorAdmin.getProxyConnectors().size() );
        assertFalse( proxyConnectorAdmin.getProxyConnectors().isEmpty() );

        assertEquals( 5, mockAuditListener.getAuditEvents().size() );

        assertEquals( AuditEvent.ADD_REMOTE_REPO, mockAuditListener.getAuditEvents().get( 0 ).getAction() );

        assertEquals( AuditEvent.ADD_PROXY_CONNECTOR, mockAuditListener.getAuditEvents().get( 1 ).getAction() );
        assertEquals( "root", mockAuditListener.getAuditEvents().get( 2 ).getUserId() );
        assertEquals( "archiva-localhost", mockAuditListener.getAuditEvents().get( 2 ).getRemoteIP() );

        assertEquals( AuditEvent.DELETE_PROXY_CONNECTOR, mockAuditListener.getAuditEvents().get( 2 ).getAction() );

        assertEquals( AuditEvent.ADD_PROXY_CONNECTOR, mockAuditListener.getAuditEvents().get( 3 ).getAction() );

        assertEquals( AuditEvent.DELETE_PROXY_CONNECTOR, mockAuditListener.getAuditEvents().get( 4 ).getAction() );
        assertEquals( "root", mockAuditListener.getAuditEvents().get( 4 ).getUserId() );

        remoteRepositoryAdmin.deleteRemoteRepository( remoteRepository.getId(), getFakeAuditInformation() );
        mockAuditListener.clearEvents();
    }

    @Test
    public void findProxyConnector()
        throws Exception
    {
        ProxyConnector proxyConnector = proxyConnectorAdmin.getProxyConnector( "internal", "central" );
        assertNotNull( proxyConnector );
    }

    @Test
    public void updateProxyConnector()
        throws Exception
    {
        mockAuditListener.clearEvents();
        ProxyConnector proxyConnector = proxyConnectorAdmin.getProxyConnector( "internal", "central" );
        assertNotNull( proxyConnector );
        proxyConnector.setDisabled( false );
        proxyConnectorAdmin.updateProxyConnector( proxyConnector, getFakeAuditInformation() );
        proxyConnector = proxyConnectorAdmin.getProxyConnector( "internal", "central" );
        assertFalse( proxyConnector.isDisabled() );

        proxyConnector.setDisabled( true );
        proxyConnectorAdmin.updateProxyConnector( proxyConnector, getFakeAuditInformation() );
        proxyConnector = proxyConnectorAdmin.getProxyConnector( "internal", "central" );
        assertTrue( proxyConnector.isDisabled() );

        proxyConnector.setOrder( 4 );
        proxyConnectorAdmin.updateProxyConnector( proxyConnector, getFakeAuditInformation() );
        proxyConnector = proxyConnectorAdmin.getProxyConnector( "internal", "central" );
        assertEquals( 4, proxyConnector.getOrder() );
        mockAuditListener.clearEvents();

    }

}
