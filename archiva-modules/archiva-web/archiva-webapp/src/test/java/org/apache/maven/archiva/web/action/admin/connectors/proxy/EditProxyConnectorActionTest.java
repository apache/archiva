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
import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.proxyconnector.DefaultProxyConnectorAdmin;
import org.apache.archiva.admin.repository.remote.DefaultRemoteRepositoryAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.policies.CachedFailuresPolicy;
import org.apache.archiva.policies.ChecksumPolicy;
import org.apache.archiva.policies.PropagateErrorsDownloadPolicy;
import org.apache.archiva.policies.PropagateErrorsOnUpdateDownloadPolicy;
import org.apache.archiva.policies.ReleasesPolicy;
import org.apache.archiva.policies.SnapshotsPolicy;
import org.apache.maven.archiva.web.action.AbstractWebworkTestCase;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.easymock.MockControl;

import java.util.List;
import java.util.Map;

/**
 * EditProxyConnectorActionTest
 *
 * @version $Id$
 */
public class EditProxyConnectorActionTest
    extends AbstractWebworkTestCase
{
    private static final String TEST_TARGET_ID = "central";

    private static final String TEST_SOURCE_ID = "corporate";

    private EditProxyConnectorAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = (EditProxyConnectorAction) getActionProxy( "/admin/editProxyConnector.action" ).getAction();

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();



        ( (DefaultManagedRepositoryAdmin) action.getManagedRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
        ( (DefaultRemoteRepositoryAdmin) action.getRemoteRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
        ( (DefaultProxyConnectorAdmin) action.getProxyConnectorAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
    }

    private void expectConfigurationRequests( int requestConfigCount )
        throws RegistryException, IndeterminateConfigurationException
    {
        expectConfigurationRequests( requestConfigCount, 1 );
    }

    private void expectConfigurationRequests( int requestConfigCount, int saveRequestCount )
        throws RegistryException, IndeterminateConfigurationException
    {
        Configuration config = createInitialConfiguration();

        archivaConfigurationControl.expectAndReturn( archivaConfiguration.getConfiguration(), config,
                                                     requestConfigCount , 20);
        //archivaConfiguration.getConfiguration();
        //archivaConfigurationControl.setReturnValue( config, requestConfigCount );

        for ( int i = 0; i <= saveRequestCount; i++ )
        {
            archivaConfiguration.save( config );
        }

        ( (DefaultManagedRepositoryAdmin) action.getManagedRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
        ( (DefaultRemoteRepositoryAdmin) action.getRemoteRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
        ( (DefaultProxyConnectorAdmin) action.getProxyConnectorAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
    }

    public void testAddBlackListPattern()
        throws Exception
    {
        expectConfigurationRequests( 7 );
        archivaConfigurationControl.replay();

        // Prepare Test.
        action.setSource( TEST_SOURCE_ID );
        action.setTarget( TEST_TARGET_ID );
        action.prepare();
        ProxyConnector connector = action.getConnector();
        assertInitialProxyConnector( connector );

        // Perform Test w/no values.
        preRequest( action );
        String status = action.addBlackListPattern();
        assertEquals( Action.INPUT, status );

        // Should have returned an error, with no blacklist pattern added.
        assertHasErrors( action );
        assertEquals( 0, connector.getBlackListPatterns().size() );

        // Try again, but now with a pattern to add.
        action.setBlackListPattern( "**/*-javadoc.jar" );
        preRequest( action );
        status = action.addBlackListPattern();
        assertEquals( Action.INPUT, status );

        // Should have no error, and 1 blacklist pattern added.
        assertNoErrors( action );
        assertEquals( 1, connector.getBlackListPatterns().size() );
    }

    public void testAddProperty()
        throws Exception
    {
        expectConfigurationRequests( 7 );
        archivaConfigurationControl.replay();

        // Prepare Test.
        action.setSource( TEST_SOURCE_ID );
        action.setTarget( TEST_TARGET_ID );
        action.prepare();
        ProxyConnector connector = action.getConnector();
        assertInitialProxyConnector( connector );

        // Perform Test w/no values.
        preRequest( action );
        String status = action.addProperty();
        assertEquals( Action.INPUT, status );

        // Should have returned an error, with no property pattern added.
        assertHasErrors( action );
        assertEquals( 0, connector.getProperties().size() );

        // Try again, but now with a property key/value to add.
        action.setPropertyKey( "eat-a" );
        action.setPropertyValue( "gramov-a-bits" );
        preRequest( action );
        status = action.addProperty();
        assertEquals( Action.INPUT, status );

        // Should have no error, and 1 property added.
        assertNoErrors( action );
        assertEquals( 1, connector.getProperties().size() );
        assertEquals( "gramov-a-bits", connector.getProperties().get( "eat-a" ) );
    }

    public void testAddWhiteListPattern()
        throws Exception
    {
        expectConfigurationRequests( 7 );
        archivaConfigurationControl.replay();

        // Prepare Test.
        action.setSource( TEST_SOURCE_ID );
        action.setTarget( TEST_TARGET_ID );
        action.prepare();
        ProxyConnector connector = action.getConnector();
        assertInitialProxyConnector( connector );

        // Perform Test w/no values.
        preRequest( action );
        String status = action.addWhiteListPattern();
        assertEquals( Action.INPUT, status );

        // Should have returned an error, with no whitelist pattern added.
        assertHasErrors( action );
        assertEquals( 0, connector.getWhiteListPatterns().size() );

        // Try again, but now with a pattern to add.
        action.setWhiteListPattern( "**/*.jar" );
        preRequest( action );
        status = action.addWhiteListPattern();
        assertEquals( Action.INPUT, status );

        // Should have no error, and 1 whitelist pattern added.
        assertNoErrors( action );
        assertEquals( 1, connector.getWhiteListPatterns().size() );
    }

    @SuppressWarnings( "unchecked" )
    public void testEditProxyConnectorCommit()
        throws Exception
    {
        expectConfigurationRequests( 9, 2 );
        archivaConfigurationControl.replay();

        // Prepare Test.
        action.setSource( TEST_SOURCE_ID );
        action.setTarget( TEST_TARGET_ID );
        action.prepare();
        ProxyConnector connector = action.getConnector();
        assertInitialProxyConnector( connector );
        // forms will use an array
        //connector.getProperties().put( "eat-a", new String[]{ "gramov-a-bits" } );
        // FIXME check the array mode
        connector.getProperties().put( "eat-a", "gramov-a-bits" );

        // Create the input screen.
        assertRequestStatus( action, Action.SUCCESS, "commit" );
        assertNoErrors( action );

        // Test configuration.
        List<ProxyConnectorConfiguration> proxyConfigs = archivaConfiguration.getConfiguration().getProxyConnectors();
        assertNotNull( proxyConfigs );
        assertEquals( 1, proxyConfigs.size() );

        ProxyConnectorConfiguration actualConnector = proxyConfigs.get( 0 );

        assertNotNull( actualConnector );
        // The use of "(direct connection)" should result in a proxyId which is <null>.
        assertNull( actualConnector.getProxyId() );
        assertEquals( "corporate", actualConnector.getSourceRepoId() );
        assertEquals( "central", actualConnector.getTargetRepoId() );

    }

    public void testEditProxyConnectorInitialPage()
        throws Exception
    {
        expectConfigurationRequests( 3 );
        archivaConfigurationControl.replay();

        action.setSource( TEST_SOURCE_ID );
        action.setTarget( TEST_TARGET_ID );
        action.prepare();
        ProxyConnector connector = action.getConnector();
        assertInitialProxyConnector( connector );

        String status = action.input();
        assertEquals( Action.INPUT, status );
    }

    public void testRemoveBlackListPattern()
        throws Exception
    {
        expectConfigurationRequests( 7 );
        archivaConfigurationControl.replay();

        // Prepare Test.
        action.setSource( TEST_SOURCE_ID );
        action.setTarget( TEST_TARGET_ID );
        action.prepare();
        ProxyConnector connector = action.getConnector();
        assertInitialProxyConnector( connector );

        // Add some arbitrary blacklist patterns.
        connector.addBlackListPattern( "**/*-javadoc.jar" );
        connector.addBlackListPattern( "**/*.war" );

        // Perform Test w/no pattern value.
        preRequest( action );
        String status = action.removeBlackListPattern();
        assertEquals( Action.INPUT, status );

        // Should have returned an error, with no blacklist pattern removed.
        assertHasErrors( action );
        assertEquals( 2, connector.getBlackListPatterns().size() );

        // Perform test w/invalid (non-existant) pattern value to remove.
        preRequest( action );
        action.setPattern( "**/*oops*" );
        status = action.removeBlackListPattern();
        assertEquals( Action.INPUT, status );

        // Should have returned an error, with no blacklist pattern removed.
        assertHasErrors( action );
        assertEquals( 2, connector.getBlackListPatterns().size() );

        // Try again, but now with a valid pattern to remove.
        action.setPattern( "**/*-javadoc.jar" );
        preRequest( action );
        status = action.removeBlackListPattern();
        assertEquals( Action.INPUT, status );

        // Should have no error, and 1 blacklist pattern left.
        assertNoErrors( action );
        assertEquals( 1, connector.getBlackListPatterns().size() );
        assertEquals( "Should have left 1 blacklist pattern", "**/*.war", connector.getBlackListPatterns().get( 0 ) );
    }

    public void testRemoveProperty()
        throws Exception
    {
        expectConfigurationRequests( 7 );
        archivaConfigurationControl.replay();

        // Prepare Test.
        action.setSource( TEST_SOURCE_ID );
        action.setTarget( TEST_TARGET_ID );
        action.prepare();
        ProxyConnector connector = action.getConnector();
        assertInitialProxyConnector( connector );

        // Add some arbitrary properties.
        connector.addProperty( "username", "general-tso" );
        connector.addProperty( "password", "chicken" );

        // Perform Test w/no property key.
        preRequest( action );
        String status = action.removeProperty();
        assertEquals( Action.INPUT, status );

        // Should have returned an error, with no properties removed.
        assertHasErrors( action );
        assertEquals( 2, connector.getProperties().size() );

        // Perform test w/invalid (non-existant) property key to remove.
        preRequest( action );
        action.setPropertyKey( "slurm" );
        status = action.removeProperty();
        assertEquals( Action.INPUT, status );

        // Should have returned an error, with no properties removed.
        assertHasErrors( action );
        assertEquals( 2, connector.getProperties().size() );

        // Try again, but now with a valid property to remove.
        preRequest( action );
        action.setPropertyKey( "password" );
        status = action.removeProperty();
        assertEquals( Action.INPUT, status );

        // Should have no error, and 1 property left.
        assertNoErrors( action );
        assertEquals( 1, connector.getProperties().size() );
        assertEquals( "Should have left 1 property", "general-tso", connector.getProperties().get( "username" ) );
    }

    public void testRemoveWhiteListPattern()
        throws Exception
    {
        expectConfigurationRequests( 7 );
        archivaConfigurationControl.replay();

        // Prepare Test.
        action.setSource( TEST_SOURCE_ID );
        action.setTarget( TEST_TARGET_ID );
        action.prepare();
        ProxyConnector connector = action.getConnector();
        assertInitialProxyConnector( connector );

        // Add some arbitrary whitelist patterns.
        connector.addWhiteListPattern( "javax/**/*" );
        connector.addWhiteListPattern( "com/sun/**/*" );

        // Perform Test w/no pattern value.
        preRequest( action );
        String status = action.removeWhiteListPattern();
        assertEquals( Action.INPUT, status );

        // Should have returned an error, with no whitelist pattern removed.
        assertHasErrors( action );
        assertEquals( 2, connector.getWhiteListPatterns().size() );

        // Perform test w/invalid (non-existant) pattern value to remove.
        preRequest( action );
        action.setPattern( "**/*oops*" );
        status = action.removeWhiteListPattern();
        assertEquals( Action.INPUT, status );

        // Should have returned an error, with no whitelist pattern removed.
        assertHasErrors( action );
        assertEquals( 2, connector.getWhiteListPatterns().size() );

        // Try again, but now with a valid pattern to remove.
        action.setPattern( "com/sun/**/*" );
        preRequest( action );
        status = action.removeWhiteListPattern();
        assertEquals( Action.INPUT, status );

        // Should have no error, and 1 whitelist pattern left.
        assertNoErrors( action );
        assertEquals( 1, connector.getWhiteListPatterns().size() );
        assertEquals( "Should have left 1 whitelist pattern", "javax/**/*", connector.getWhiteListPatterns().get( 0 ) );
    }

    public void testSecureActionBundle()
        throws Exception
    {
        /* Configuration will be requested at least 3 times. */
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration(), 3 );
        archivaConfigurationControl.replay();

        action.prepare();
        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }

    private void assertInitialProxyConnector( ProxyConnector connector )
    {
        assertNotNull( connector );
        assertNotNull( connector.getBlackListPatterns() );
        assertNotNull( connector.getWhiteListPatterns() );
        assertNotNull( connector.getProperties() );

        assertEquals( TEST_SOURCE_ID, connector.getSourceRepoId() );
        assertEquals( TEST_TARGET_ID, connector.getTargetRepoId() );
    }

    @SuppressWarnings( "unchecked" )
    private Configuration createInitialConfiguration()
    {
        Configuration config = new Configuration();

        ManagedRepositoryConfiguration managedRepo = new ManagedRepositoryConfiguration();
        managedRepo.setId( TEST_SOURCE_ID );
        managedRepo.setLayout( "${java.io.tmpdir}/archiva-test/managed-repo" );
        managedRepo.setReleases( true );

        config.addManagedRepository( managedRepo );

        RemoteRepositoryConfiguration remoteRepo = new RemoteRepositoryConfiguration();
        remoteRepo.setId( TEST_TARGET_ID );
        remoteRepo.setUrl( "http://repo1.maven.org/maven2/" );

        config.addRemoteRepository( remoteRepo );

        ProxyConnectorConfiguration connector = new ProxyConnectorConfiguration();
        connector.setSourceRepoId( TEST_SOURCE_ID );
        connector.setTargetRepoId( TEST_TARGET_ID );

        // TODO: Set these options programatically via list of available policies.
        Map<String, String> policies = connector.getPolicies();
        policies.put( "releases", new ReleasesPolicy().getDefaultOption() );
        policies.put( "snapshots", new SnapshotsPolicy().getDefaultOption() );
        policies.put( "checksum", new ChecksumPolicy().getDefaultOption() );
        policies.put( "cache-failures", new CachedFailuresPolicy().getDefaultOption() );
        policies.put( "propagate-errors", new PropagateErrorsDownloadPolicy().getDefaultOption() );
        policies.put( "propagate-errors-on-update", new PropagateErrorsOnUpdateDownloadPolicy().getDefaultOption() );

        config.addProxyConnector( connector );

        return config;
    }
}
