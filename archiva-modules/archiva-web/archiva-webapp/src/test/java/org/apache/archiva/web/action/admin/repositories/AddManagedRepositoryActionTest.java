package org.apache.archiva.web.action.admin.repositories;

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
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.repository.DefaultRepositoryCommonValidator;
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.redback.integration.interceptor.SecureActionBundle;
import org.apache.archiva.redback.integration.interceptor.SecureActionException;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.web.validator.utils.ValidatorUtil;
import org.apache.commons.io.FileUtils;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AddManagedRepositoryActionTest
 *
 * @version $Id$
 */
public class AddManagedRepositoryActionTest
    extends AbstractManagedRepositoryActionTest
{
    private AddManagedRepositoryAction action;

    private RoleManager roleManager;

    private MockControl roleManagerControl;

    private MockControl archivaConfigurationControl;

    private org.apache.archiva.redback.components.registry.Registry registry;

    private MockControl registryControl;

    private ArchivaConfiguration archivaConfiguration;

    private MockControl repositoryTaskSchedulerControl;

    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;


    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = new AddManagedRepositoryAction();

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();

        roleManagerControl = MockControl.createControl( RoleManager.class );
        roleManager = (RoleManager) roleManagerControl.getMock();

        registryControl = MockControl.createControl( org.apache.archiva.redback.components.registry.Registry.class );
        registry = (org.apache.archiva.redback.components.registry.Registry) registryControl.getMock();
        //action.setRegistry( registry );

        repositoryTaskSchedulerControl = MockClassControl.createControl( RepositoryArchivaTaskScheduler.class );
        repositoryTaskScheduler = (RepositoryArchivaTaskScheduler) repositoryTaskSchedulerControl.getMock();

        location = new File( System.getProperty( "basedir" ), "target/test/location" );
        ( (DefaultManagedRepositoryAdmin) getManagedRepositoryAdmin() ).setArchivaConfiguration( archivaConfiguration );
        ( (DefaultManagedRepositoryAdmin) getManagedRepositoryAdmin() ).setRoleManager( roleManager );
        ( (DefaultManagedRepositoryAdmin) getManagedRepositoryAdmin() ).setRegistry( registry );
        ( (DefaultManagedRepositoryAdmin) getManagedRepositoryAdmin() ).setRepositoryTaskScheduler(
            repositoryTaskScheduler );

        DefaultRepositoryCommonValidator defaultRepositoryCommonValidator = new DefaultRepositoryCommonValidator();
        defaultRepositoryCommonValidator.setArchivaConfiguration( archivaConfiguration );
        defaultRepositoryCommonValidator.setRegistry( registry );

        ( (DefaultManagedRepositoryAdmin) getManagedRepositoryAdmin() ).setRepositoryCommonValidator(
            defaultRepositoryCommonValidator );

        action.setRepositoryCommonValidator( defaultRepositoryCommonValidator );

        action.setManagedRepositoryAdmin( getManagedRepositoryAdmin() );

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
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration() );
        archivaConfigurationControl.replay();

        action.prepare();
        ManagedRepository configuration = action.getRepository();
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

        registry.getString( "appserver.base", "${appserver.base}" );
        registryControl.setReturnValue( "target/test", 1, 3 );
        registry.getString( "appserver.home", "${appserver.home}" );
        registryControl.setReturnValue( "target/test", 1, 3 );

        registryControl.replay();

        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( REPO_ID );
        repositoryTaskScheduler.isProcessingRepositoryTask( REPO_ID );
        repositoryTaskSchedulerControl.setReturnValue( false );
        repositoryTaskScheduler.queueTask( task );
        repositoryTaskSchedulerControl.setVoidCallable();
        repositoryTaskSchedulerControl.replay();

        Configuration configuration = new Configuration();
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        action.prepare();
        ManagedRepository repository = action.getRepository();
        populateRepository( repository );

        assertFalse( location.exists() );
        String status = action.commit();
        assertEquals( Action.SUCCESS, status );
        assertTrue( location.exists() );

        assertEquals( Collections.singletonList( repository ), getManagedRepositoryAdmin().getManagedRepositories() );
        assertEquals( location.getCanonicalPath(), new File( repository.getLocation() ).getCanonicalPath() );

        roleManagerControl.verify();
        archivaConfigurationControl.verify();
        registryControl.verify();
    }


    public void testAddRepositoryExistingLocation()
        throws Exception
    {
        if ( !location.exists() )
        {
            location.mkdirs();
        }

        registry.getString( "appserver.base", "${appserver.base}" );
        registryControl.setReturnValue( "target/test" );
        registry.getString( "appserver.home", "${appserver.home}" );
        registryControl.setReturnValue( "target/test" );

        registryControl.replay();

        action.prepare();
        ManagedRepository repository = action.getRepository();
        populateRepository( repository );

        assertTrue( location.exists() );
        String status = action.commit();
        assertEquals( AddManagedRepositoryAction.CONFIRM, status );
        assertEquals( location.getCanonicalPath(), new File( repository.getLocation() ).getCanonicalPath() );
        registryControl.verify();
    }

    public void testStruts2ValidationFrameworkWithNullInputs()
        throws Exception
    {
        // prep
        // 0 is the default value for primitive int; null for objects
        ManagedRepository managedRepositoryConfiguration = createManagedRepository( null, null, null, null );
        action.setRepository( managedRepositoryConfiguration );

        // test
        actionValidatorManager.validate( action, EMPTY_STRING );

        // verify
        assertTrue( action.hasFieldErrors() );

        Map<String, List<String>> fieldErrors = action.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a repository identifier." );
        expectedFieldErrors.put( "repository.id", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a directory." );
        expectedFieldErrors.put( "repository.location", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a repository name." );
        expectedFieldErrors.put( "repository.name", expectedErrorMessages );

        ValidatorUtil.assertFieldErrors( expectedFieldErrors, fieldErrors );
    }

    public void testStruts2ValidationFrameworkWithBlankInputs()
        throws Exception
    {
        // prep
        // 0 is the default value for primitive int
        ManagedRepository managedRepositoryConfiguration =
            createManagedRepository( EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING );
        action.setRepository( managedRepositoryConfiguration );

        // test
        actionValidatorManager.validate( action, EMPTY_STRING );

        // verify
        assertTrue( action.hasFieldErrors() );

        Map<String, List<String>> fieldErrors = action.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a repository identifier." );
        expectedFieldErrors.put( "repository.id", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a directory." );
        expectedFieldErrors.put( "repository.location", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "You must enter a repository name." );
        expectedFieldErrors.put( "repository.name", expectedErrorMessages );

        ValidatorUtil.assertFieldErrors( expectedFieldErrors, fieldErrors );
    }

    public void testStruts2ValidationFrameworkWithInvalidInputs()
        throws Exception
    {
        // prep
        ManagedRepository managedRepositoryConfiguration =
            createManagedRepository( REPOSITORY_ID_INVALID_INPUT, REPOSITORY_NAME_INVALID_INPUT,
                                     REPOSITORY_LOCATION_INVALID_INPUT, REPOSITORY_INDEX_DIR_INVALID_INPUT,
                                     REPOSITORY_DAYS_OLDER_INVALID_INPUT, REPOSITORY_RETENTION_COUNT_INVALID_INPUT );
        action.setRepository( managedRepositoryConfiguration );

        // test
        actionValidatorManager.validate( action, EMPTY_STRING );

        // verify
        assertTrue( action.hasFieldErrors() );

        Map<String, List<String>> fieldErrors = action.getFieldErrors();

        // make an expected field error object
        Map<String, List<String>> expectedFieldErrors = new HashMap<String, List<String>>();

        // populate
        List<String> expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add(
            "Identifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        expectedFieldErrors.put( "repository.id", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add(
            "Directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
        expectedFieldErrors.put( "repository.location", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add(
            "Repository Name must only contain alphanumeric characters, white-spaces(' '), forward-slashes(/), open-parenthesis('('), close-parenthesis(')'),  underscores(_), dots(.), and dashes(-)." );
        expectedFieldErrors.put( "repository.name", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add(
            "Index directory must only contain alphanumeric characters, equals(=), question-marks(?), exclamation-points(!), ampersands(&), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-)." );
        expectedFieldErrors.put( "repository.indexDirectory", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "Repository Purge By Retention Count needs to be between 1 and 100." );
        expectedFieldErrors.put( "repository.retentionCount", expectedErrorMessages );

        expectedErrorMessages = new ArrayList<String>();
        expectedErrorMessages.add( "Repository Purge By Days Older Than needs to be larger than 0." );
        expectedFieldErrors.put( "repository.daysOlder", expectedErrorMessages );

        ValidatorUtil.assertFieldErrors( expectedFieldErrors, fieldErrors );
    }

    public void testStruts2ValidationFrameworkWithValidInputs()
        throws Exception
    {
        // prep
        ManagedRepository managedRepositoryConfiguration =
            createManagedRepository( REPOSITORY_ID_VALID_INPUT, REPOSITORY_NAME_VALID_INPUT,
                                     REPOSITORY_LOCATION_VALID_INPUT, REPOSITORY_INDEX_DIR_VALID_INPUT,
                                     REPOSITORY_DAYS_OLDER_VALID_INPUT, REPOSITORY_RETENTION_COUNT_VALID_INPUT );
        action.setRepository( managedRepositoryConfiguration );

        // test
        actionValidatorManager.validate( action, EMPTY_STRING );

        // verify
        assertFalse( action.hasFieldErrors() );
    }

    // TODO: test errors during add, other actions
}
