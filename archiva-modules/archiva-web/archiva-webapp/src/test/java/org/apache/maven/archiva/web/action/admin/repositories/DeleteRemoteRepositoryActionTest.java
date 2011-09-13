package org.apache.maven.archiva.web.action.admin.repositories;

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
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.remote.RemoteRepository;
import org.apache.archiva.admin.repository.remote.DefaultRemoteRepositoryAdmin;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.web.action.AbstractActionTestCase;
import org.codehaus.plexus.registry.RegistryException;
import org.easymock.MockControl;

import java.util.Collections;

/**
 * DeleteRemoteRepositoryActionTest
 *
 * @version $Id$
 */
public class DeleteRemoteRepositoryActionTest
    extends AbstractActionTestCase
{
    private static final String REPO_ID = "remote-repo-ident";

    private DeleteRemoteRepositoryAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        //action = (DeleteRemoteRepositoryAction) lookup( Action.class.getName(), "deleteRemoteRepositoryAction" );
        action = (DeleteRemoteRepositoryAction) getActionProxy( "/admin/deleteRemoteRepository.action" ).getAction();

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );

        ( (DefaultRemoteRepositoryAdmin) action.getRemoteRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
    }

    public void testDeleteRemoteRepositoryConfirmation()
        throws Exception
    {
        RemoteRepository originalRepository = createRepository();
        Configuration configuration = createConfigurationForEditing( originalRepository );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        RemoteRepository repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        String status = action.confirmDelete();
        assertEquals( Action.INPUT, status );
        repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
        assertEquals( Collections.singletonList( originalRepository ),
                      action.getRemoteRepositoryAdmin().getRemoteRepositories() );
    }

    public void testDeleteRemoteRepository()
        throws RegistryException, IndeterminateConfigurationException, RepositoryAdminException
    {
        Configuration configuration = createConfigurationForEditing( createRepository() );
        configuration.addManagedRepository( createManagedRepository( "internal", "target/repo/internal" ) );
        configuration.addManagedRepository( createManagedRepository( "snapshots", "target/repo/snapshots" ) );
        configuration.addProxyConnector( createProxyConnector( "internal", REPO_ID ) );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 4 );

        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        RemoteRepository repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        assertEquals( 1, configuration.getProxyConnectors().size() );

        String status = action.delete();
        assertEquals( Action.SUCCESS, status );

        assertTrue( configuration.getRemoteRepositories().isEmpty() );
        assertEquals( 0, configuration.getProxyConnectors().size() );
    }

    public void testDeleteRemoteRepositoryCancelled()
        throws Exception
    {
        RemoteRepository originalRepository = createRepository();
        Configuration configuration = createConfigurationForEditing( originalRepository );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 2 );

        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        RemoteRepository repositoryConfiguration = action.getRepository();
        assertNotNull( repositoryConfiguration );
        assertRepositoryEquals( repositoryConfiguration, createRepository() );

        String status = action.execute();
        assertEquals( Action.SUCCESS, status );

        RemoteRepository repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
        assertEquals( Collections.singletonList( originalRepository ),
                      action.getRemoteRepositoryAdmin().getRemoteRepositories() );
    }

    private Configuration createConfigurationForEditing( RemoteRepository repositoryConfiguration )
    {
        Configuration configuration = new Configuration();
        RemoteRepositoryConfiguration conf = new RemoteRepositoryConfiguration();
        conf.setId( repositoryConfiguration.getId() );
        conf.setLayout( repositoryConfiguration.getLayout() );
        conf.setUrl( repositoryConfiguration.getUrl() );
        conf.setName( repositoryConfiguration.getName() );
        configuration.addRemoteRepository( conf );
        return configuration;
    }

    private RemoteRepository createRepository()
    {
        RemoteRepository r = new RemoteRepository();
        r.setId( REPO_ID );
        populateRepository( r );
        return r;
    }

    private void assertRepositoryEquals( RemoteRepository expectedRepository, RemoteRepository actualRepository )
    {
        assertEquals( expectedRepository.getId(), actualRepository.getId() );
        assertEquals( expectedRepository.getLayout(), actualRepository.getLayout() );
        assertEquals( expectedRepository.getUrl(), actualRepository.getUrl() );
        assertEquals( expectedRepository.getName(), actualRepository.getName() );
    }

    private ManagedRepositoryConfiguration createManagedRepository( String string, String testPath )
    {
        ManagedRepositoryConfiguration r = new ManagedRepositoryConfiguration();
        r.setId( REPO_ID );
        r.setName( "repo name" );
        r.setLocation( testPath );
        r.setLayout( "default" );
        r.setRefreshCronExpression( "* 0/5 * * * ?" );
        r.setDaysOlder( 0 );
        r.setRetentionCount( 0 );
        r.setReleases( true );
        r.setSnapshots( true );
        r.setScanned( false );
        r.setDeleteReleasedSnapshots( false );
        return r;
    }

    private ProxyConnectorConfiguration createProxyConnector( String managedRepoId, String remoteRepoId )
    {
        ProxyConnectorConfiguration connector = new ProxyConnectorConfiguration();
        connector.setSourceRepoId( managedRepoId );
        connector.setTargetRepoId( remoteRepoId );

        return connector;
    }

    private void populateRepository( RemoteRepository repository )
    {
        repository.setId( REPO_ID );
        repository.setName( "repo name" );
        repository.setUrl( "url" );
        repository.setLayout( "default" );
    }

    // TODO: what about removing proxied content if a proxy is removed?
}
