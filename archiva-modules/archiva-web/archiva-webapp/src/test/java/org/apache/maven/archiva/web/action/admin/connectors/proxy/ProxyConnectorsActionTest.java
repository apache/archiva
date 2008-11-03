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

import com.opensymphony.xwork2.Action;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.web.action.AbstractWebworkTestCase;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.plexus.registry.RegistryException;
import org.easymock.MockControl;

/**
 * ProxyConnectorsActionTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ProxyConnectorsActionTest
    extends AbstractWebworkTestCase
{
    private static final String JAVAX = "javax";

    private static final String CENTRAL = "central";

    private static final String CORPORATE = "corporate";

    private ProxyConnectorsAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = (ProxyConnectorsAction) lookup( Action.class.getName(), "proxyConnectorsAction" );

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );
    }

    public void testSecureActionBundle()
        throws Exception
    {
        expectConfigurationRequests( 3 );
        archivaConfigurationControl.replay();

        action.prepare();
        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }

    public void testExecute()
        throws Exception
    {
        expectConfigurationRequests( 3 );
        archivaConfigurationControl.replay();

        action.prepare();

        String status = action.execute();
        assertEquals( Action.SUCCESS, status );
        assertNoErrors( action );
        
        assertNotNull( action.getProxyConnectorMap() );
        assertNotNull( action.getRepoMap() );

        assertEquals( 1, action.getProxyConnectorMap().size() );
        assertEquals( 3, action.getRepoMap().size() );
    }

    private void expectConfigurationRequests( int requestConfigCount )
        throws RegistryException, IndeterminateConfigurationException
    {
        Configuration config = createInitialConfiguration();

        for ( int i = 0; i < requestConfigCount; i++ )
        {
            archivaConfiguration.getConfiguration();
            archivaConfigurationControl.setReturnValue( config );
        }

        archivaConfiguration.save( config );
    }

    private Configuration createInitialConfiguration()
    {
        Configuration config = new Configuration();

        ManagedRepositoryConfiguration managedRepo = new ManagedRepositoryConfiguration();
        managedRepo.setId( CORPORATE );
        managedRepo.setLayout( "${java.io.tmpdir}/archiva-test/managed-repo" );
        managedRepo.setReleases( true );

        config.addManagedRepository( managedRepo );

        RemoteRepositoryConfiguration remoteRepo = new RemoteRepositoryConfiguration();
        remoteRepo.setId( CENTRAL );
        remoteRepo.setUrl( "http://repo1.maven.org/maven2/" );

        config.addRemoteRepository( remoteRepo );

        remoteRepo = new RemoteRepositoryConfiguration();
        remoteRepo.setId( JAVAX );
        remoteRepo.setUrl( "http://download.java.net/maven/2/" );

        config.addRemoteRepository( remoteRepo );

        ProxyConnectorConfiguration connector = new ProxyConnectorConfiguration();
        connector.setSourceRepoId( CORPORATE );
        connector.setTargetRepoId( CENTRAL );

        config.addProxyConnector( connector );

        connector = new ProxyConnectorConfiguration();
        connector.setSourceRepoId( CORPORATE );
        connector.setTargetRepoId( JAVAX );

        config.addProxyConnector( connector );

        return config;
    }
}
