package org.apache.archiva.admin.repository.proxyconnectorrule;
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

import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.apache.archiva.admin.model.beans.ProxyConnectorRule;
import org.apache.archiva.proxy.model.ProxyConnectorRuleType;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.repository.AbstractRepositoryAdminTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;

/**
 * @author Olivier Lamy
 */
public class ProxyConnectorRuleAdminTest
    extends AbstractRepositoryAdminTest
{
    @Test
    public void addProxyConnectorRule()
        throws Exception
    {
        ProxyConnector proxyConnector = new ProxyConnector();
        proxyConnector.setSourceRepoId( "snapshots" );
        proxyConnector.setTargetRepoId( "central" );

        ProxyConnectorRule rule = null;
        try
        {
            int size = proxyConnectorRuleAdmin.getProxyConnectorRules().size();
            assertEquals( 0, size );

            proxyConnectorAdmin.addProxyConnector( proxyConnector, getFakeAuditInformation() );

            rule = new ProxyConnectorRule( "org/apache/maven", ProxyConnectorRuleType.BLACK_LIST,
                                           Arrays.asList( proxyConnector ) );

            proxyConnectorRuleAdmin.addProxyConnectorRule( rule, getFakeAuditInformation() );
            assertEquals( size + 1, proxyConnectorRuleAdmin.getProxyConnectorRules().size() );

            rule = proxyConnectorRuleAdmin.getProxyConnectorRules().get( 0 );

            assertEquals( "org/apache/maven", rule.getPattern() );
            assertEquals( 1, rule.getProxyConnectors().size() );
            assertEquals( "snapshots", rule.getProxyConnectors().get( 0 ).getSourceRepoId() );
            assertEquals( "central", rule.getProxyConnectors().get( 0 ).getTargetRepoId() );
            assertEquals( ProxyConnectorRuleType.BLACK_LIST, rule.getProxyConnectorRuleType() );
        }
        finally
        {
            proxyConnectorRuleAdmin.deleteProxyConnectorRule( rule, getFakeAuditInformation() );
            proxyConnectorAdmin.deleteProxyConnector( proxyConnector, getFakeAuditInformation() );
        }
    }

    @Test
    public void addProxyConnectorRuleWithTwoProxyConnectors()
        throws Exception
    {
        RemoteRepository remoteRepository = new RemoteRepository(Locale.getDefault());
        remoteRepository.setId( "archiva" );
        remoteRepository.setName( "archiva rocks" );
        remoteRepository.setUrl( "http://wine.org" );

        remoteRepositoryAdmin.addRemoteRepository( remoteRepository, getFakeAuditInformation() );

        int size = proxyConnectorRuleAdmin.getProxyConnectorRules().size();
        assertEquals( 0, size );
        ProxyConnector proxyConnector1 = new ProxyConnector();
        proxyConnector1.setSourceRepoId( "snapshots" );
        proxyConnector1.setTargetRepoId( "central" );
        proxyConnectorAdmin.addProxyConnector( proxyConnector1, getFakeAuditInformation() );

        ProxyConnector proxyConnector2 = new ProxyConnector();
        proxyConnector2.setSourceRepoId( "snapshots" );
        proxyConnector2.setTargetRepoId( "archiva" );
        proxyConnectorAdmin.addProxyConnector( proxyConnector2, getFakeAuditInformation() );

        ProxyConnectorRule rule = new ProxyConnectorRule( "org/apache/maven", ProxyConnectorRuleType.BLACK_LIST,
                                                          Arrays.asList( proxyConnector1, proxyConnector2 ) );
        try
        {
            proxyConnectorRuleAdmin.addProxyConnectorRule( rule, getFakeAuditInformation() );
            assertEquals( size + 1, proxyConnectorRuleAdmin.getProxyConnectorRules().size() );

            rule = proxyConnectorRuleAdmin.getProxyConnectorRules().get( 0 );

            assertEquals( "org/apache/maven", rule.getPattern() );
            assertEquals( 2, rule.getProxyConnectors().size() );
            assertEquals( ProxyConnectorRuleType.BLACK_LIST, rule.getProxyConnectorRuleType() );
        }
        finally
        {
            proxyConnectorRuleAdmin.deleteProxyConnectorRule( rule, getFakeAuditInformation() );
            proxyConnectorAdmin.deleteProxyConnector( proxyConnector1, getFakeAuditInformation() );
            proxyConnectorAdmin.deleteProxyConnector( proxyConnector2, getFakeAuditInformation() );
            remoteRepositoryAdmin.deleteRemoteRepository( remoteRepository.getId(), getFakeAuditInformation() );
        }
    }


    @Test
    public void updateProxyConnectorRuleWithTwoProxyConnectors()
        throws Exception
    {
        RemoteRepository remoteRepository = new RemoteRepository( Locale.getDefault( ));
        remoteRepository.setId( "archiva" );
        remoteRepository.setName( "archiva rocks" );
        remoteRepository.setUrl( "http://wine.org" );

        remoteRepositoryAdmin.addRemoteRepository( remoteRepository, getFakeAuditInformation() );

        int size = proxyConnectorRuleAdmin.getProxyConnectorRules().size();
        assertEquals( 0, size );
        ProxyConnector proxyConnector1 = new ProxyConnector();
        proxyConnector1.setSourceRepoId( "snapshots" );
        proxyConnector1.setTargetRepoId( "central" );
        proxyConnectorAdmin.addProxyConnector( proxyConnector1, getFakeAuditInformation() );

        ProxyConnector proxyConnector2 = new ProxyConnector();
        proxyConnector2.setSourceRepoId( "snapshots" );
        proxyConnector2.setTargetRepoId( "archiva" );
        proxyConnectorAdmin.addProxyConnector( proxyConnector2, getFakeAuditInformation() );

        ProxyConnectorRule rule = new ProxyConnectorRule( "org/apache/maven", ProxyConnectorRuleType.BLACK_LIST,
                                                          Arrays.asList( proxyConnector1, proxyConnector2 ) );
        try
        {
            proxyConnectorRuleAdmin.addProxyConnectorRule( rule, getFakeAuditInformation() );
            assertEquals( size + 1, proxyConnectorRuleAdmin.getProxyConnectorRules().size() );

            rule = proxyConnectorRuleAdmin.getProxyConnectorRules().get( 0 );

            assertEquals( "org/apache/maven", rule.getPattern() );
            assertEquals( 2, rule.getProxyConnectors().size() );
            //assertEquals( "snapshots", rule.getProxyConnectors().get( 0 ).getSourceRepoId() );
            //assertEquals( "central", rule.getProxyConnectors().get( 0 ).getTargetRepoId() );
            assertEquals( ProxyConnectorRuleType.BLACK_LIST, rule.getProxyConnectorRuleType() );

            rule.setProxyConnectors( Arrays.asList( proxyConnector1 ) );

            proxyConnectorRuleAdmin.updateProxyConnectorRule( rule, getFakeAuditInformation() );

            assertEquals( size + 1, proxyConnectorRuleAdmin.getProxyConnectorRules().size() );

            rule = proxyConnectorRuleAdmin.getProxyConnectorRules().get( 0 );

            assertEquals( "org/apache/maven", rule.getPattern() );
            assertEquals( 1, rule.getProxyConnectors().size() );
            assertEquals( "snapshots", rule.getProxyConnectors().get( 0 ).getSourceRepoId() );
            assertEquals( "central", rule.getProxyConnectors().get( 0 ).getTargetRepoId() );

        }
        finally
        {
            proxyConnectorRuleAdmin.deleteProxyConnectorRule( rule, getFakeAuditInformation() );
            proxyConnectorAdmin.deleteProxyConnector( proxyConnector1, getFakeAuditInformation() );
            proxyConnectorAdmin.deleteProxyConnector( proxyConnector2, getFakeAuditInformation() );
            remoteRepositoryAdmin.deleteRemoteRepository( remoteRepository.getId(), getFakeAuditInformation() );
        }
    }

}
