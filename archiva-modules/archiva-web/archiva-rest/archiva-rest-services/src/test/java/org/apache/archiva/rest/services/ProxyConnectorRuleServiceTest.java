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

import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.apache.archiva.admin.model.beans.ProxyConnectorRule;
import org.apache.archiva.proxy.model.ProxyConnectorRuleType;
import org.apache.archiva.rest.api.services.ProxyConnectorRuleService;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author Olivier Lamy
 */
public class ProxyConnectorRuleServiceTest
    extends AbstractArchivaRestTest
{

    @Test
    public void addProxyConnectorRule()
        throws Exception
    {
        ProxyConnector proxyConnector = new ProxyConnector();
        proxyConnector.setSourceRepoId( "snapshots" );
        proxyConnector.setTargetRepoId( "central" );

        ProxyConnectorRuleService service = getProxyConnectorRuleService( authorizationHeader );

        ProxyConnectorRule rule = null;
        try
        {

            int size = service.getProxyConnectorRules().size();
            assertEquals( 0, size );

            getProxyConnectorService().addProxyConnector( proxyConnector );

            rule = new ProxyConnectorRule( "org/apache/maven", ProxyConnectorRuleType.BLACK_LIST,
                                           Arrays.asList( proxyConnector ) );

            service.addProxyConnectorRule( rule );
            assertEquals( size + 1, service.getProxyConnectorRules().size() );

            rule = service.getProxyConnectorRules().get( 0 );

            assertEquals( "org/apache/maven", rule.getPattern() );
            assertEquals( 1, rule.getProxyConnectors().size() );
            assertEquals( "snapshots", rule.getProxyConnectors().get( 0 ).getSourceRepoId() );
            assertEquals( "central", rule.getProxyConnectors().get( 0 ).getTargetRepoId() );
            assertEquals( ProxyConnectorRuleType.BLACK_LIST, rule.getProxyConnectorRuleType() );
        }
        finally
        {
            service.deleteProxyConnectorRule( rule );
            getProxyConnectorService().deleteProxyConnector( proxyConnector );
        }
    }
}

