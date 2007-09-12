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
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionException;
import org.codehaus.plexus.registry.RegistryException;
import org.easymock.MockControl;

import java.io.File;
import java.util.Collections;

/**
 * Test the repositories action returns the correct data.
 */
public class ConfigureRepositoryActionTest
    extends PlexusTestCase
{
    private ConfigureRepositoryAction action;

    private RoleManager roleManager;

    private MockControl roleManagerControl;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    private static final String REPO_ID = "repo-ident";

    private File location;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        // TODO: purely to quiet logging - shouldn't be needed
        String appserverBase = getTestFile( "target/appserver-base" ).getAbsolutePath();
        System.setProperty( "appserver.base", appserverBase );
        action = (ConfigureRepositoryAction) lookup( Action.class.getName(), "configureRepositoryAction" );

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );

        roleManagerControl = MockControl.createControl( RoleManager.class );
        roleManager = (RoleManager) roleManagerControl.getMock();
        action.setRoleManager( roleManager );
        location = getTestFile( "location" );
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

    public void testAddRepositoryInitialPage()
        throws Exception
    {
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration() );
        archivaConfigurationControl.replay();

        action.prepare();
        assertNull( action.getRepoid() );
        assertNull( action.getMode() );
        AdminRepositoryConfiguration configuration = action.getRepository();
        assertNotNull( configuration );
        assertNull( configuration.getId() );
        // check all booleans are false
        assertFalse( configuration.isDeleteReleasedSnapshots() );
        assertFalse( configuration.isIndexed() );
        assertFalse( configuration.isReleases() );
        assertFalse( configuration.isSnapshots() );

        String status = action.add();
        assertEquals( Action.INPUT, status );

        // check defaults
        assertFalse( configuration.isDeleteReleasedSnapshots() );
        assertTrue( configuration.isIndexed() );
        assertTrue( configuration.isReleases() );
        assertFalse( configuration.isSnapshots() );
    }

    public void testAddRepository()
        throws Exception
    {
        FileUtils.deleteDirectory( location );

        // TODO: should be in the business model
        roleManager.createTemplatedRole( "archiva-repository-manager", REPO_ID );
        roleManager.createTemplatedRole( "archiva-repository-observer", REPO_ID );

        roleManagerControl.replay();

        Configuration configuration = new Configuration();
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        action.prepare();
        action.setMode( "add" );
        AdminRepositoryConfiguration repository = action.getRepository();
        populateRepository( repository );

        assertFalse( location.exists() );
        String status = action.save();
        assertEquals( Action.SUCCESS, status );
        assertTrue( location.exists() );

        assertEquals( Collections.singletonList( repository ), configuration.getManagedRepositories() );

        roleManagerControl.verify();
        archivaConfigurationControl.verify();
    }

    public void testEditRepositoryInitialPage()
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
        AdminRepositoryConfiguration repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        String status = action.edit();
        assertEquals( Action.INPUT, status );
        repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
    }

    public void testEditRepository()
        throws Exception
    {
        // TODO: should be in the business model
        roleManager.createTemplatedRole( "archiva-repository-manager", REPO_ID );
        roleManager.createTemplatedRole( "archiva-repository-observer", REPO_ID );

        roleManagerControl.replay();

        Configuration configuration = createConfigurationForEditing( createRepository() );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        action.prepare();
        action.setMode( "edit" );
        AdminRepositoryConfiguration repository = action.getRepository();
        populateRepository( repository );
        repository.setName( "new repo name" );

        String status = action.save();
        assertEquals( Action.SUCCESS, status );

        ManagedRepositoryConfiguration newRepository = createRepository();
        newRepository.setName( "new repo name" );
        assertRepositoryEquals( repository, newRepository );
        assertEquals( Collections.singletonList( repository ), configuration.getManagedRepositories() );

        roleManagerControl.verify();
        archivaConfigurationControl.verify();
    }

    public void testDeleteRepositoryConfirmation()
    {
        ManagedRepositoryConfiguration originalRepository = createRepository();
        Configuration configuration = createConfigurationForEditing( originalRepository );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        assertNull( action.getMode() );
        AdminRepositoryConfiguration repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        String status = action.confirm();
        assertEquals( Action.INPUT, status );
        repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
        assertEquals( Collections.singletonList( originalRepository ), configuration.getManagedRepositories() );
    }

    public void testDeleteRepositoryKeepContent()
        throws RegistryException, IndeterminateConfigurationException
    {
        Configuration configuration = executeDeletionTest( "delete-entry", createRepository() );

        assertTrue( configuration.getManagedRepositories().isEmpty() );

        assertTrue( location.exists() );
    }

    public void testDeleteRepositoryDeleteContent()
        throws RegistryException, IndeterminateConfigurationException
    {
        Configuration configuration = executeDeletionTest( "delete-contents", createRepository() );

        assertTrue( configuration.getManagedRepositories().isEmpty() );

        assertFalse( location.exists() );
    }

    public void testDeleteRepositoryCancelled()
        throws RegistryException, IndeterminateConfigurationException
    {
        ManagedRepositoryConfiguration originalRepository = createRepository();
        Configuration configuration = executeDeletionTest( "unmodified", originalRepository );

        AdminRepositoryConfiguration repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
        assertEquals( Collections.singletonList( originalRepository ), configuration.getManagedRepositories() );

        assertTrue( location.exists() );
    }

    private Configuration executeDeletionTest( String mode, ManagedRepositoryConfiguration originalRepository )
        throws RegistryException, IndeterminateConfigurationException
    {
        location.mkdirs();

        Configuration configuration = createConfigurationForEditing( originalRepository );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );
        action.setMode( mode );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        assertEquals( mode, action.getMode() );
        AdminRepositoryConfiguration repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        assertTrue( location.exists() );
        String status = action.delete();
        assertEquals( Action.SUCCESS, status );
        return configuration;
    }

    private void assertRepositoryEquals( ManagedRepositoryConfiguration expectedRepository,
                                         ManagedRepositoryConfiguration actualRepository )
    {
        assertEquals( expectedRepository.getDaysOlder(), actualRepository.getDaysOlder() );
        assertEquals( expectedRepository.getId(), actualRepository.getId() );
        assertEquals( expectedRepository.getIndexDir(), actualRepository.getIndexDir() );
        assertEquals( expectedRepository.getLayout(), actualRepository.getLayout() );
        assertEquals( expectedRepository.getLocation(), actualRepository.getLocation() );
        assertEquals( expectedRepository.getName(), actualRepository.getName() );
        assertEquals( expectedRepository.getRefreshCronExpression(), actualRepository.getRefreshCronExpression() );
        assertEquals( expectedRepository.getRetentionCount(), actualRepository.getRetentionCount() );
        assertEquals( expectedRepository.isDeleteReleasedSnapshots(), actualRepository.isDeleteReleasedSnapshots() );
        assertEquals( expectedRepository.isIndexed(), actualRepository.isIndexed() );
        assertEquals( expectedRepository.isReleases(), actualRepository.isReleases() );
        assertEquals( expectedRepository.isSnapshots(), actualRepository.isSnapshots() );
    }

    private Configuration createConfigurationForEditing( ManagedRepositoryConfiguration repositoryConfiguration )
    {
        Configuration configuration = new Configuration();
        configuration.addManagedRepository( repositoryConfiguration );
        return configuration;
    }

    private ManagedRepositoryConfiguration createRepository()
    {
        ManagedRepositoryConfiguration r = new ManagedRepositoryConfiguration();
        r.setId( REPO_ID );
        populateRepository( r );
        return r;
    }

    private void populateRepository( ManagedRepositoryConfiguration repository )
    {
        repository.setId( REPO_ID );
        repository.setName( "repo name" );
        repository.setLocation( location.getAbsolutePath() );
        repository.setLayout( "default" );
        repository.setRefreshCronExpression( "* 0/5 * * * ?" );
        repository.setDaysOlder( 31 );
        repository.setRetentionCount( 20 );
        repository.setReleases( true );
        repository.setSnapshots( true );
        repository.setIndexed( true );
        repository.setDeleteReleasedSnapshots( true );
    }

    // TODO: test errors during add, other actions
    // TODO: what if there are proxy connectors attached to a deleted repository?
    // TODO: what about removing proxied content if a proxy is removed?
}
