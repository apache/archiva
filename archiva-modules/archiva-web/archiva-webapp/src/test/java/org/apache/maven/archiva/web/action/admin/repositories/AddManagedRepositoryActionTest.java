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

import java.io.File;
import java.util.Collections;

import com.opensymphony.xwork2.Action;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.easymock.MockControl;

/**
 * AddManagedRepositoryActionTest 
 *
 * @version $Id$
 */
public class AddManagedRepositoryActionTest
    extends PlexusInSpringTestCase
{
    private AddManagedRepositoryAction action;

    private RoleManager roleManager;

    private MockControl roleManagerControl;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    private static final String REPO_ID = "repo-ident";

    private File location;
    
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = new AddManagedRepositoryAction();

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );

        roleManagerControl = MockControl.createControl( RoleManager.class );
        roleManager = (RoleManager) roleManagerControl.getMock();
        action.setRoleManager( roleManager );
        location = getTestFile( "target/test/location" );
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
        ManagedRepositoryConfiguration configuration = action.getRepository();
        assertNotNull( configuration );
        assertNull( configuration.getId() );
        // check all booleans are false
        assertFalse( configuration.isDeleteReleasedSnapshots() );
        assertFalse( configuration.isScanned() );
        assertFalse( configuration.isReleases() );
        assertFalse( configuration.isSnapshots() );

        String status = action.input();
        assertEquals( Action.INPUT, status );

        // check defaults
        assertFalse( configuration.isDeleteReleasedSnapshots() );
        assertTrue( configuration.isScanned() );
        assertTrue( configuration.isReleases() );
        assertFalse( configuration.isSnapshots() );
    }

    public void testAddRepository()
        throws Exception
    {
        FileUtils.deleteDirectory( location );

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setVoidCallable();
        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setVoidCallable();

        roleManagerControl.replay();

        Configuration configuration = new Configuration();
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        action.prepare();
        ManagedRepositoryConfiguration repository = action.getRepository();
        populateRepository( repository );

        assertFalse( location.exists() );
        String status = action.commit();
        assertEquals( Action.SUCCESS, status );
        assertTrue( location.exists() );

        assertEquals( Collections.singletonList( repository ), configuration.getManagedRepositories() );

        roleManagerControl.verify();
        archivaConfigurationControl.verify();
    }
    
    
    public void testAddRepositoryExistingLocation()
        throws Exception
    {
        if( !location.exists() )
        {
            location.mkdirs();
        }        
    
        action.prepare();
        ManagedRepositoryConfiguration repository = action.getRepository();
        populateRepository( repository );
    
        assertTrue( location.exists() );
        String status = action.commit();
        assertEquals( AddManagedRepositoryAction.CONFIRM, status );
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
        repository.setScanned( false );
        repository.setDeleteReleasedSnapshots( true );
    }

    // TODO: test errors during add, other actions
}
