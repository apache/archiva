/*
 *  Copyright 2008 jdumay.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package org.apache.maven.archiva.web.action.admin.connectors.proxy;

import com.opensymphony.xwork2.Action;
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.proxyconnector.DefaultProxyConnectorAdmin;
import org.apache.archiva.admin.repository.remote.DefaultRemoteRepositoryAdmin;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.web.action.AbstractWebworkTestCase;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.easymock.MockControl;

public class EnableProxyConnectorActionTest
    extends AbstractWebworkTestCase
{
    private static final String TEST_TARGET_ID = "central";

    private static final String TEST_SOURCE_ID = "corporate";

    private EnableProxyConnectorAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        action = (EnableProxyConnectorAction) getActionProxy( "/admin/enableProxyConnector.action" ).getAction();
        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        ( (DefaultManagedRepositoryAdmin) action.getManagedRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
        ( (DefaultRemoteRepositoryAdmin) action.getRemoteRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
        ( (DefaultProxyConnectorAdmin) action.getProxyConnectorAdmin() ).setArchivaConfiguration( archivaConfiguration );
    }

    private void expectConfigurationRequests( int requestConfigCount )
        throws RegistryException, IndeterminateConfigurationException
    {
        Configuration config = createInitialConfiguration();

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( config, requestConfigCount );
        archivaConfiguration.save( config );
    }

    public void testConfirmDeleteBadSourceOrTarget()
        throws Exception
    {
        expectConfigurationRequests( 4 );
        archivaConfigurationControl.replay();

        // Attempt to show the confirm enable screen, but provide
        // a bad source id or target id to actually enable

        preRequest( action );
        action.setSource( "bad-source" ); // id doesn't exist.
        action.setTarget( "bad-target" ); // id doesn't exist.
        String status = action.confirmEnable();
        // Should have resulted in an error.
        assertEquals( Action.ERROR, status );
        assertHasErrors( action );

        preRequest( action );
        action.setSource( "bad" ); // Bad doesn't exist.
        action.setTarget( TEST_TARGET_ID );
        status = action.confirmEnable();
        // Should have resulted in an error.
        assertEquals( Action.ERROR, status );
        assertHasErrors( action );

        preRequest( action );
        action.setSource( TEST_SOURCE_ID );
        action.setTarget( "bad" ); // Bad doesn't exist.
        status = action.confirmEnable();
        // Should have resulted in an error.
        assertEquals( Action.ERROR, status );
        assertHasErrors( action );
    }

    public void testConfirmEnableNoSourceOrTarget()
        throws Exception
    {
        expectConfigurationRequests( 1 );
        archivaConfigurationControl.replay();

        // Attempt to show the confirm enable screen, but don't provide
        // the source id or target id to actually delete

        preRequest( action );
        action.setSource( null ); // No source Id.
        action.setTarget( null ); // No target Id.
        String status = action.confirmEnable();
        // Should have resulted in an error.
        assertEquals( Action.ERROR, status );
        assertHasErrors( action );

        preRequest( action );
        action.setSource( TEST_SOURCE_ID );
        action.setTarget( null ); // No target Id.
        status = action.confirmEnable();
        // Should have resulted in an error.
        assertEquals( Action.ERROR, status );
        assertHasErrors( action );

        preRequest( action );
        action.setSource( null ); // No source Id.
        action.setTarget( TEST_TARGET_ID );
        status = action.confirmEnable();
        // Should have resulted in an error.
        assertEquals( Action.ERROR, status );
        assertHasErrors( action );
    }

    public void testEnable()
        throws Exception
    {
        expectConfigurationRequests( 5 );
        archivaConfigurationControl.replay();

        // Show the confirm the enable of proxy connector screen.
        preRequest( action );
        action.setSource( TEST_SOURCE_ID );
        action.setTarget( TEST_TARGET_ID );
        String status = action.confirmEnable();
        assertEquals( Action.INPUT, status );
        assertNoErrors( action );

        // Perform the delete.
        preRequest( action );
        status = action.enable();
        assertEquals( Action.SUCCESS, status );
        assertNoErrors( action );
        assertHasMessages( action );

        // Test the configuration.
        assertEquals( 1, archivaConfiguration.getConfiguration().getProxyConnectors().size() );
        ProxyConnectorConfiguration config =
            (ProxyConnectorConfiguration) archivaConfiguration.getConfiguration().getProxyConnectors().get( 0 );
        assertFalse( config.isDisabled() );
    }

    public void testSecureActionBundle()
        throws Exception
    {
        expectConfigurationRequests( 1 );
        archivaConfigurationControl.replay();

        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }

    public void testConfirmEnable()
        throws Exception
    {
        expectConfigurationRequests( 1 );
        archivaConfigurationControl.replay();

        // Show the confirm the enable of proxy connector screen.
        preRequest( action );
        action.setSource( TEST_SOURCE_ID );
        action.setTarget( TEST_TARGET_ID );
        String status = action.confirmEnable();
        assertEquals( Action.INPUT, status );
        assertNoErrors( action );
    }

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
        connector.setDisabled( true );

        config.addProxyConnector( connector );

        return config;
    }
}
