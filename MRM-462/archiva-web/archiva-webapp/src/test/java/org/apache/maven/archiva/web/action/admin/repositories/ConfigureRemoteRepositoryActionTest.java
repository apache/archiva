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

import com.opensymphony.xwork.Action;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionException;
import org.codehaus.plexus.registry.RegistryException;
import org.easymock.MockControl;

import java.util.Collections;

/**
 * Test the repositories action returns the correct data.
 */
public class ConfigureRemoteRepositoryActionTest
    extends PlexusTestCase
{
    private ConfigureRemoteRepositoryAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    private static final String REPO_ID = "remote-repo-ident";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        // TODO: purely to quiet logging - shouldn't be needed
        String appserverBase = getTestFile( "target/appserver-base" ).getAbsolutePath();
        System.setProperty( "appserver.base", appserverBase );
        action = (ConfigureRemoteRepositoryAction) lookup( Action.class.getName(), "configureRemoteRepositoryAction" );

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );
    }

    public void testSecureActionBundle()
        throws SecureActionException
    {
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration() );
        archivaConfigurationControl.replay();

        action.prepare();
        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }

    public void testAddRemoteRepositoryInitialPage()
        throws Exception
    {
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration() );
        archivaConfigurationControl.replay();

        action.prepare();
        assertNull( action.getRepoid() );
        assertNull( action.getMode() );
        RemoteRepositoryConfiguration configuration = action.getRepository();
        assertNotNull( configuration );
        assertNull( configuration.getId() );

        String status = action.add();
        assertEquals( Action.INPUT, status );
    }

    public void testAddRemoteRepository()
        throws Exception
    {
        Configuration configuration = new Configuration();
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        action.prepare();
        action.setMode( "add" );
        RemoteRepositoryConfiguration repository = action.getRepository();
        populateRepository( repository );

        String status = action.save();
        assertEquals( Action.SUCCESS, status );

        assertEquals( Collections.singletonList( repository ), configuration.getRemoteRepositories() );

        archivaConfigurationControl.verify();
    }

    public void testEditRemoteRepositoryInitialPage()
        throws Exception
    {
        Configuration configuration = createConfigurationForEditing( createRepository() );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        assertNull( action.getMode() );
        RemoteRepositoryConfiguration repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        String status = action.edit();
        assertEquals( Action.INPUT, status );
        repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
    }

    public void testEditRemoteRepository()
        throws Exception
    {
        Configuration configuration = createConfigurationForEditing( createRepository() );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        action.prepare();
        action.setMode( "edit" );
        RemoteRepositoryConfiguration repository = action.getRepository();
        populateRepository( repository );
        repository.setName( "new repo name" );

        String status = action.save();
        assertEquals( Action.SUCCESS, status );

        RemoteRepositoryConfiguration newRepository = createRepository();
        newRepository.setName( "new repo name" );
        assertRepositoryEquals( repository, newRepository );
        assertEquals( Collections.singletonList( repository ), configuration.getRemoteRepositories() );

        archivaConfigurationControl.verify();
    }

    public void testDeleteRemoteRepositoryConfirmation()
    {
        RemoteRepositoryConfiguration originalRepository = createRepository();
        Configuration configuration = createConfigurationForEditing( originalRepository );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        assertNull( action.getMode() );
        RemoteRepositoryConfiguration repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        String status = action.confirm();
        assertEquals( Action.INPUT, status );
        repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
        assertEquals( Collections.singletonList( originalRepository ), configuration.getRemoteRepositories() );
    }

    public void testDeleteRemoteRepositoryKeepContent()
        throws RegistryException, IndeterminateConfigurationException
    {
        Configuration configuration = executeDeletionTest( "delete-entry", createRepository() );

        assertTrue( configuration.getRemoteRepositories().isEmpty() );
    }

    public void testDeleteRemoteRepositoryCancelled()
        throws Exception
    {
        RemoteRepositoryConfiguration originalRepository = createRepository();
        Configuration configuration = createConfigurationForEditing( originalRepository );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );
        action.setMode( "unmodified" ); // TODO! remove

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        assertEquals( "unmodified", action.getMode() );
        RemoteRepositoryConfiguration repositoryConfiguration = action.getRepository();
        assertNotNull( repositoryConfiguration );
        assertRepositoryEquals( repositoryConfiguration, createRepository() );

        String status = action.execute();
        assertEquals( Action.SUCCESS, status );

        RemoteRepositoryConfiguration repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
        assertEquals( Collections.singletonList( originalRepository ), configuration.getRemoteRepositories() );
    }

    private Configuration executeDeletionTest( String mode, RemoteRepositoryConfiguration originalRepository )
        throws RegistryException, IndeterminateConfigurationException
    {
        Configuration configuration = createConfigurationForEditing( originalRepository );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );
        action.setMode( mode ); // TODO! remove

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        assertEquals( mode, action.getMode() );
        RemoteRepositoryConfiguration repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        String status = action.delete();
        assertEquals( Action.SUCCESS, status );
        return configuration;
    }

    private void assertRepositoryEquals( RemoteRepositoryConfiguration expectedRepository,
                                         RemoteRepositoryConfiguration actualRepository )
    {
        assertEquals( expectedRepository.getId(), actualRepository.getId() );
        assertEquals( expectedRepository.getLayout(), actualRepository.getLayout() );
        assertEquals( expectedRepository.getUrl(), actualRepository.getUrl() );
        assertEquals( expectedRepository.getName(), actualRepository.getName() );
    }

    private Configuration createConfigurationForEditing( RemoteRepositoryConfiguration repositoryConfiguration )
    {
        Configuration configuration = new Configuration();
        configuration.addRemoteRepository( repositoryConfiguration );
        return configuration;
    }

    private RemoteRepositoryConfiguration createRepository()
    {
        RemoteRepositoryConfiguration r = new RemoteRepositoryConfiguration();
        r.setId( REPO_ID );
        populateRepository( r );
        return r;
    }

    private void populateRepository( RemoteRepositoryConfiguration repository )
    {
        repository.setId( REPO_ID );
        repository.setName( "repo name" );
        repository.setUrl( "url" );
        repository.setLayout( "default" );
    }

    // TODO: test errors during add, other actions
    // TODO: what if there are proxy connectors attached to a deleted repository?
    // TODO: what about removing proxied content if a proxy is removed?
}
