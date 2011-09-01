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
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.memory.TestRepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.validator.utils.ValidatorUtil;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * EditManagedRepositoryActionTest
 *
 * @version $Id$
 */
public class EditManagedRepositoryActionTest
    extends AbstractManagedRepositoryActionTest
{
    private EditManagedRepositoryAction action;

    private RoleManager roleManager;

    private MockControl roleManagerControl;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    private Registry registry;

    private MockControl registryControl;

    private MetadataRepository metadataRepository;

    private MockControl repositoryTaskSchedulerControl;

    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = new EditManagedRepositoryAction();

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );

        roleManagerControl = MockControl.createControl( RoleManager.class );
        roleManager = (RoleManager) roleManagerControl.getMock();
        action.setRoleManager( roleManager );

        registryControl = MockControl.createControl( Registry.class );
        registry = (Registry) registryControl.getMock();
        action.setRegistry( registry );

        repositoryTaskSchedulerControl = MockClassControl.createControl( RepositoryArchivaTaskScheduler.class );
        repositoryTaskScheduler = (RepositoryArchivaTaskScheduler) repositoryTaskSchedulerControl.getMock();
        action.setRepositoryTaskScheduler( repositoryTaskScheduler );

        location = new File( "target/test/location" );

        metadataRepository = mock( MetadataRepository.class );
        RepositorySession repositorySession = mock( RepositorySession.class );
        when( repositorySession.getRepository() ).thenReturn( metadataRepository );
        TestRepositorySessionFactory factory = applicationContext.getBean( TestRepositorySessionFactory.class );
        factory.setRepositorySession( repositorySession );
        action.setRepositorySessionFactory( factory );

        ( (DefaultManagedRepositoryAdmin) getManagedRepositoryAdmin() ).setArchivaConfiguration( archivaConfiguration );
        ( (DefaultManagedRepositoryAdmin) getManagedRepositoryAdmin() ).setRoleManager( roleManager );
        ( (DefaultManagedRepositoryAdmin) getManagedRepositoryAdmin() ).setRepositoryTaskScheduler(
            repositoryTaskScheduler );
        ( (DefaultManagedRepositoryAdmin) getManagedRepositoryAdmin() ).setRepositorySessionFactory( factory );

        ( (DefaultManagedRepositoryAdmin) getManagedRepositoryAdmin() ).setAuditListeners(
            new ArrayList<AuditListener>( 0 ) );

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

    public void testEditRepositoryInitialPage()
        throws Exception
    {
        Configuration configuration = createConfigurationForEditing( createRepository() );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        Configuration stageRepoConfiguration = new Configuration();
        stageRepoConfiguration.addManagedRepository( createStagingRepository() );
        archivaConfigurationControl.setReturnValue( stageRepoConfiguration );

        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        ManagedRepositoryConfiguration repository = action.getRepository();
        assertNotNull( repository );

        ManagedRepositoryConfiguration newRepository = createRepository();
        assertRepositoryEquals( repository, newRepository );
        assertEquals( repository.getLocation(), newRepository.getLocation() );

        String status = action.input();
        assertEquals( Action.INPUT, status );
        repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
    }

    public void testEditRepository()
        throws Exception
    {
        String stageRepoId = REPO_ID + "-stage";

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setVoidCallable();

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setVoidCallable();

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, stageRepoId );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, stageRepoId );
        roleManagerControl.setVoidCallable();

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, stageRepoId );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, stageRepoId );
        roleManagerControl.setVoidCallable();

        roleManagerControl.replay();

        registry.getString( "appserver.base", "${appserver.base}" );
        registryControl.setReturnValue( "target/test" );
        registry.getString( "appserver.home", "${appserver.home}" );
        registryControl.setReturnValue( "target/test" );

        registryControl.replay();

        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( REPO_ID );
        repositoryTaskScheduler.isProcessingRepositoryTask( REPO_ID );
        repositoryTaskSchedulerControl.setReturnValue( false );
        repositoryTaskScheduler.queueTask( task );
        repositoryTaskSchedulerControl.setVoidCallable();

        RepositoryTask stageTask = new RepositoryTask();
        stageTask.setRepositoryId( stageRepoId );
        repositoryTaskScheduler.isProcessingRepositoryTask( stageRepoId );
        repositoryTaskSchedulerControl.setReturnValue( false );
        repositoryTaskScheduler.queueTask( stageTask );
        repositoryTaskSchedulerControl.setVoidCallable();

        repositoryTaskSchedulerControl.replay();

        Configuration configuration = createConfigurationForEditing( createRepository() );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfigurationControl.setReturnValue( configuration );

        Configuration stageRepoConfiguration = new Configuration();
        stageRepoConfiguration.addManagedRepository( createStagingRepository() );
        archivaConfigurationControl.setReturnValue( stageRepoConfiguration );
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );
        archivaConfiguration.save( configuration );

        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );
        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        ManagedRepositoryConfiguration repository = action.getRepository();
        populateRepository( repository );
        repository.setName( "new repo name" );

        MockControl repositoryStatisticsManagerControl = MockControl.createControl( RepositoryStatisticsManager.class );
        RepositoryStatisticsManager repositoryStatisticsManager =
            (RepositoryStatisticsManager) repositoryStatisticsManagerControl.getMock();
        ( (DefaultManagedRepositoryAdmin) getManagedRepositoryAdmin() ).setRepositoryStatisticsManager(
            repositoryStatisticsManager );
        // no deletion
        repositoryStatisticsManagerControl.replay();

        new File( "target/test/" + REPO_ID + "-stage" ).mkdirs();

        action.setRepository( repository );
        action.setStageNeeded( true );
        String status = action.commit();
        assertEquals( Action.SUCCESS, status );

        ManagedRepositoryConfiguration newRepository = createRepository();
        newRepository.setName( "new repo name" );
        assertRepositoryEquals( repository, newRepository );
        //assertEquals( Collections.singletonList( repository ), configuration.getManagedRepositories() );
        //assertEquals( location.getCanonicalPath(), new File( repository.getLocation() ).getCanonicalPath() );

        roleManagerControl.verify();
        //archivaConfigurationControl.verify();
        repositoryStatisticsManagerControl.verify();
        registryControl.verify();
    }

    public void testEditRepositoryLocationChanged()
        throws Exception
    {
        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setVoidCallable();

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setVoidCallable();

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID + "-stage" );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID + "-stage" );
        roleManagerControl.setVoidCallable();

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID + "-stage" );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID + "-stage" );
        roleManagerControl.setVoidCallable();

        roleManagerControl.replay();

        registry.getString( "appserver.base", "${appserver.base}" );
        registryControl.setReturnValue( "target/test" );
        registry.getString( "appserver.home", "${appserver.home}" );
        registryControl.setReturnValue( "target/test" );

        registryControl.replay();

        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( REPO_ID );
        repositoryTaskScheduler.isProcessingRepositoryTask( REPO_ID );
        repositoryTaskSchedulerControl.setReturnValue( false );
        repositoryTaskScheduler.queueTask( task );
        repositoryTaskSchedulerControl.setVoidCallable();

        repositoryTaskSchedulerControl.replay();

        Configuration configuration = createConfigurationForEditing( createRepository() );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfigurationControl.setReturnValue( buildEasyConfiguration() );

        Configuration stageRepoConfiguration = buildEasyConfiguration();
        stageRepoConfiguration.addManagedRepository( createStagingRepository() );
        archivaConfigurationControl.setReturnValue( stageRepoConfiguration );

        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );
        configuration.addManagedRepository( stageRepoConfiguration.getManagedRepositories().get( 0 ) );
        archivaConfiguration.save( configuration );
        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        MockControl repositoryStatisticsManagerControl = MockControl.createControl( RepositoryStatisticsManager.class );
        RepositoryStatisticsManager repositoryStatisticsManager =
            (RepositoryStatisticsManager) repositoryStatisticsManagerControl.getMock();
        ( (DefaultManagedRepositoryAdmin) getManagedRepositoryAdmin() ).setRepositoryStatisticsManager(
            repositoryStatisticsManager );
        repositoryStatisticsManager.deleteStatistics( metadataRepository, REPO_ID );
        repositoryStatisticsManagerControl.replay();

        new File( "target/test/location/" + REPO_ID + "-stage" ).mkdirs();

        action.setStageNeeded( true );
        action.setRepoid( REPO_ID );
        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );

        ManagedRepositoryConfiguration repository = new ManagedRepositoryConfiguration();
        populateRepository( repository );
        File testFile = new File( "target/test/location/new" );
        FileUtils.deleteDirectory( testFile );
        repository.setLocation( "${appserver.base}/location/new" );
        action.setRepository( repository );
        String status = action.commit();
        assertEquals( Action.SUCCESS, status );
        //assertEquals( Collections.singletonList( repository ), configuration.getManagedRepositories() );
        //assertEquals( testFile.getCanonicalPath(), new File( repository.getLocation() ).getCanonicalPath() );

        roleManagerControl.verify();
        //archivaConfigurationControl.verify();
        repositoryStatisticsManagerControl.verify();
        registryControl.verify();
    }

    public void testStruts2ValidationFrameworkWithNullInputs()
        throws Exception
    {
        // prep
        // 0 is the default value for primitive int; null for objects
        ManagedRepositoryConfiguration managedRepositoryConfiguration =
            createManagedRepositoryConfiguration( null, null, null, null );
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
        ManagedRepositoryConfiguration managedRepositoryConfiguration =
            createManagedRepositoryConfiguration( EMPTY_STRING, EMPTY_STRING, EMPTY_STRING, EMPTY_STRING );
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
        ManagedRepositoryConfiguration managedRepositoryConfiguration =
            createManagedRepositoryConfiguration( REPOSITORY_ID_INVALID_INPUT, REPOSITORY_NAME_INVALID_INPUT,
                                                  REPOSITORY_LOCATION_INVALID_INPUT, REPOSITORY_INDEX_DIR_INVALID_INPUT,
                                                  REPOSITORY_DAYS_OLDER_INVALID_INPUT,
                                                  REPOSITORY_RETENTION_COUNT_INVALID_INPUT );
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
        expectedFieldErrors.put( "repository.indexDir", expectedErrorMessages );

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
        ManagedRepositoryConfiguration managedRepositoryConfiguration =
            createManagedRepositoryConfiguration( REPOSITORY_ID_VALID_INPUT, REPOSITORY_NAME_VALID_INPUT,
                                                  REPOSITORY_LOCATION_VALID_INPUT, REPOSITORY_INDEX_DIR_VALID_INPUT,
                                                  REPOSITORY_DAYS_OLDER_VALID_INPUT,
                                                  REPOSITORY_RETENTION_COUNT_VALID_INPUT );
        action.setRepository( managedRepositoryConfiguration );

        // test
        actionValidatorManager.validate( action, EMPTY_STRING );

        // verify
        assertFalse( action.hasFieldErrors() );
    }

    private void assertRepositoryEquals( ManagedRepositoryConfiguration expectedRepository,
                                         ManagedRepositoryConfiguration actualRepository )
    {
        assertEquals( expectedRepository.getDaysOlder(), actualRepository.getDaysOlder() );
        assertEquals( expectedRepository.getId(), actualRepository.getId() );
        assertEquals( expectedRepository.getIndexDir(), actualRepository.getIndexDir() );
        assertEquals( expectedRepository.getLayout(), actualRepository.getLayout() );
        assertEquals( expectedRepository.getName(), actualRepository.getName() );
        assertEquals( expectedRepository.getRefreshCronExpression(), actualRepository.getRefreshCronExpression() );
        assertEquals( expectedRepository.getRetentionCount(), actualRepository.getRetentionCount() );
        assertEquals( expectedRepository.isDeleteReleasedSnapshots(), actualRepository.isDeleteReleasedSnapshots() );
        assertEquals( expectedRepository.isScanned(), actualRepository.isScanned() );
        assertEquals( expectedRepository.isReleases(), actualRepository.isReleases() );
        assertEquals( expectedRepository.isSnapshots(), actualRepository.isSnapshots() );
    }

    private Configuration createConfigurationForEditing( ManagedRepositoryConfiguration repositoryConfiguration )
        throws Exception
    {
        Configuration configuration = buildEasyConfiguration();
        configuration.addManagedRepository( repositoryConfiguration );
        return configuration;
    }

    // easy configuration for hashCode/equals
    private Configuration buildEasyConfiguration()
    {
        return new Configuration()
        {
            @Override
            public int hashCode()
            {
                return getManagedRepositories().size();
            }

            @Override
            public boolean equals( Object o )
            {
                return true;
            }
        };
    }

    private ManagedRepositoryConfiguration createRepository()
        throws IOException
    {
        ManagedRepositoryConfiguration r = new ManagedRepositoryConfiguration();
        r.setId( REPO_ID );
        populateRepository( r );
        return r;
    }

    private ManagedRepositoryConfiguration createStagingRepository()
        throws IOException
    {
        ManagedRepositoryConfiguration r = new ManagedRepositoryConfiguration();
        r.setId( REPO_ID + "-stage" );
        populateStagingRepository( r );
        return r;
    }

    private void populateStagingRepository( ManagedRepositoryConfiguration repository )
        throws IOException
    {
        repository.setId( REPO_ID + "-stage" );
        repository.setName( "repo name" );
        repository.setLocation( "${appserver.base}/location" );
        repository.setLayout( "default" );
        repository.setRefreshCronExpression( "* 0/5 * * * ?" );
        repository.setDaysOlder( 31 );
        repository.setRetentionCount( 20 );
        repository.setReleases( true );
        repository.setSnapshots( true );
        repository.setScanned( false );
        repository.setDeleteReleasedSnapshots( true );
    }
}
