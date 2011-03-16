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
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.memory.TestRepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
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

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * EditManagedRepositoryActionTest
 *
 * @version $Id$
 */
public class EditManagedRepositoryActionTest
    extends PlexusInSpringTestCase
{
    private EditManagedRepositoryAction action;

    private RoleManager roleManager;

    private MockControl roleManagerControl;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    private static final String REPO_ID = "repo-ident";

    private File location;

    private MetadataRepository metadataRepository;

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
        location = getTestFile( "target/test/location" );

        metadataRepository = mock( MetadataRepository.class );
        RepositorySession repositorySession = mock( RepositorySession.class );
        when( repositorySession.getRepository() ).thenReturn( metadataRepository );
        TestRepositorySessionFactory factory = (TestRepositorySessionFactory) lookup( RepositorySessionFactory.class );
        factory.setRepositorySession( repositorySession );
        action.setRepositorySessionFactory( factory );
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
        assertRepositoryEquals( repository, createRepository() );

        String status = action.input();
        assertEquals( Action.INPUT, status );
        repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
    }

    public void testEditRepository()
        throws Exception
    {
        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setReturnValue( false );

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID + "-stage" );
        roleManagerControl.setReturnValue( false );

        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setVoidCallable();
        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setReturnValue( false );

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID + "-stage" );
        roleManagerControl.setReturnValue( false );

        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setVoidCallable();

        roleManagerControl.replay();

        Configuration configuration = createConfigurationForEditing( createRepository() );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        Configuration stageRepoConfiguration = new Configuration();
        stageRepoConfiguration.addManagedRepository( createStagingRepository() );
        archivaConfigurationControl.setReturnValue( stageRepoConfiguration );
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.setReturnValue( configuration );

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
        action.setRepositoryStatisticsManager( repositoryStatisticsManager );
        // no deletion
        repositoryStatisticsManagerControl.replay();

        new File( "target/test/" + REPO_ID + "-stage" ).mkdirs();

        String status = action.commit();
        assertEquals( Action.SUCCESS, status );

        ManagedRepositoryConfiguration newRepository = createRepository();
        newRepository.setName( "new repo name" );
        assertRepositoryEquals( repository, newRepository );
        assertEquals( Collections.singletonList( repository ), configuration.getManagedRepositories() );

        roleManagerControl.verify();
        archivaConfigurationControl.verify();
        repositoryStatisticsManagerControl.verify();
    }

    public void testEditRepositoryLocationChanged()
        throws Exception
    {
        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setReturnValue( false );

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID + "-stage" );
        roleManagerControl.setReturnValue( false );

        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setVoidCallable();
        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setReturnValue( false );

        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID + "-stage" );
        roleManagerControl.setReturnValue( false );

        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setVoidCallable();

        roleManagerControl.replay();

        Configuration configuration = createConfigurationForEditing( createRepository() );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        Configuration stageRepoConfiguration = new Configuration();
        stageRepoConfiguration.addManagedRepository( createStagingRepository() );
        archivaConfigurationControl.setReturnValue( stageRepoConfiguration );

        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        MockControl repositoryStatisticsManagerControl = MockControl.createControl( RepositoryStatisticsManager.class );
        RepositoryStatisticsManager repositoryStatisticsManager =
            (RepositoryStatisticsManager) repositoryStatisticsManagerControl.getMock();
        action.setRepositoryStatisticsManager( repositoryStatisticsManager );
        repositoryStatisticsManager.deleteStatistics( metadataRepository, REPO_ID );
        repositoryStatisticsManagerControl.replay();

        new File( "target/test/location/" + REPO_ID + "-stage" ).mkdirs();

        action.setRepoid( REPO_ID );
        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );

        ManagedRepositoryConfiguration repository = new ManagedRepositoryConfiguration();
        populateRepository( repository );
        File testFile = getTestFile( "target/test/location/new" );
        FileUtils.deleteDirectory( testFile );
        repository.setLocation( testFile.getCanonicalPath() );
        action.setRepository( repository );
        String status = action.commit();
        assertEquals( Action.SUCCESS, status );
        assertEquals( Collections.singletonList( repository ), configuration.getManagedRepositories() );

        roleManagerControl.verify();
        archivaConfigurationControl.verify();
        repositoryStatisticsManagerControl.verify();
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
        assertEquals( expectedRepository.isScanned(), actualRepository.isScanned() );
        assertEquals( expectedRepository.isReleases(), actualRepository.isReleases() );
        assertEquals( expectedRepository.isSnapshots(), actualRepository.isSnapshots() );
    }

    private Configuration createConfigurationForEditing( ManagedRepositoryConfiguration repositoryConfiguration )
        throws Exception
    {
        Configuration configuration = new Configuration();
        configuration.addManagedRepository( repositoryConfiguration );
//        configuration.addManagedRepository( createStagingRepository() );
        return configuration;
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

    private void populateRepository( ManagedRepositoryConfiguration repository )
        throws IOException
    {
        repository.setId( REPO_ID );
        repository.setName( "repo name" );
        repository.setLocation( location.getCanonicalPath() );
        repository.setLayout( "default" );
        repository.setRefreshCronExpression( "* 0/5 * * * ?" );
        repository.setDaysOlder( 31 );
        repository.setRetentionCount( 20 );
        repository.setReleases( true );
        repository.setSnapshots( true );
        repository.setScanned( false );
        repository.setDeleteReleasedSnapshots( true );
    }

    private void populateStagingRepository( ManagedRepositoryConfiguration repository )
        throws IOException
    {
        repository.setId( REPO_ID + "-stage" );
        repository.setName( "repo name" );
        repository.setLocation( location.getCanonicalPath() );
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
