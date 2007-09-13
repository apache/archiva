package org.apache.maven.archiva.web.action.admin.connectors.proxy;

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

import com.opensymphony.xwork.Action;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.easymock.MockControl;

/**
 * Test the proxy connector configuration action returns the correct data.
 */
public class ConfigureProxyConnectorActionTest
    extends PlexusTestCase
{
    private ConfigureProxyConnectorAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        // TODO: purely to quiet logging - shouldn't be needed
        String appserverBase = getTestFile( "target/appserver-base" ).getAbsolutePath();
        System.setProperty( "appserver.base", appserverBase );
        action = (ConfigureProxyConnectorAction) lookup( Action.class.getName(), "configureProxyConnectorAction" );

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );
    }

    public void testSecureActionBundle()
        throws Exception
    {
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration() );
        archivaConfigurationControl.replay();

        action.prepare();
        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }

    public void testAddProxyConnectorInitialPage()
        throws Exception
    {
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration() );
        archivaConfigurationControl.replay();

        action.prepare();
        assertNull( action.getMode() );
        ProxyConnectorConfiguration configuration = action.getConnector();
        assertNotNull( configuration );
        assertNull( configuration.getProxyId() );
        assertNull( configuration.getSourceRepoId() );
        assertNull( configuration.getTargetRepoId() );
        assertTrue( configuration.getPolicies().isEmpty() );
        assertTrue( configuration.getProperties().isEmpty() );
        assertTrue( configuration.getBlackListPatterns().isEmpty() );
        assertTrue( configuration.getWhiteListPatterns().isEmpty() );

        String status = action.add();
        assertEquals( Action.INPUT, status );
    }

    // TODO: test the population of proxyIdOptions, *RepoIdList (from prepare) and policyMap (from initialize)
    // TODO: test the other methods. Should review the structure of the action in the process as there is a lot of different combinations of parameters
}
