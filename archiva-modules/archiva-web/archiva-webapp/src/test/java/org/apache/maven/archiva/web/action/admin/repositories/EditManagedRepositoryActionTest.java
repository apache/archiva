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
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.RepositoryContentStatisticsDAO;
import org.apache.maven.archiva.database.constraints.RepositoryContentStatisticsByRepositoryConstraint;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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

    private MockControl archivaDaoControl;

    private ArchivaDAO archivaDao;

    private MockControl repoContentStatsDaoControl;

    private RepositoryContentStatisticsDAO repoContentStatsDao;

    private static final String REPO_ID = "repo-ident";

    private File location;

    @Override
    protected String getPlexusConfigLocation()
    {
        return AbstractManagedRepositoriesAction.class.getName().replace( '.', '/' ) + "Test.xml";
    }
    
    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = (EditManagedRepositoryAction) lookup( Action.class.getName(), "editManagedRepositoryAction" );

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );

        roleManagerControl = MockControl.createControl( RoleManager.class );
        roleManager = (RoleManager) roleManagerControl.getMock();
        action.setRoleManager( roleManager );
        location = getTestFile( "target/test/location" );

        archivaDaoControl = MockControl.createControl( ArchivaDAO.class );
        archivaDao = (ArchivaDAO) archivaDaoControl.getMock();
        action.setArchivaDAO( archivaDao );

        repoContentStatsDaoControl = MockControl.createControl( RepositoryContentStatisticsDAO.class );
        repoContentStatsDao = (RepositoryContentStatisticsDAO) repoContentStatsDaoControl.getMock();
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
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setVoidCallable();
        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setReturnValue( false );
        roleManager.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setVoidCallable();

        roleManagerControl.replay();

        Configuration configuration = createConfigurationForEditing( createRepository() );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
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

        String status = action.commit();
        assertEquals( Action.SUCCESS, status );

        ManagedRepositoryConfiguration newRepository = createRepository();
        newRepository.setName( "new repo name" );
        assertRepositoryEquals( repository, newRepository );
        assertEquals( Collections.singletonList( repository ), configuration.getManagedRepositories() );

        roleManagerControl.verify();
        archivaConfigurationControl.verify();
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

        roleManagerControl.replay();

        Configuration configuration = createConfigurationForEditing( createRepository() );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        archivaDaoControl.expectAndReturn( archivaDao.getRepositoryContentStatisticsDAO(), repoContentStatsDao );

        archivaDaoControl.replay();

        repoContentStatsDao.queryRepositoryContentStatistics(
                new RepositoryContentStatisticsByRepositoryConstraint( REPO_ID ) );
        repoContentStatsDaoControl.setMatcher( MockControl.ALWAYS_MATCHER );

        List<RepositoryContentStatistics> repoStats = createRepositoryContentStatisticsList();
        repoContentStatsDaoControl.setReturnValue( repoStats );

        repoContentStatsDao.deleteRepositoryContentStatistics( repoStats.get( 0 ) );
        repoContentStatsDaoControl.setVoidCallable();
        repoContentStatsDao.deleteRepositoryContentStatistics( repoStats.get( 1 ) );
        repoContentStatsDaoControl.setVoidCallable();

        repoContentStatsDaoControl.replay();
        
        action.setRepoid( REPO_ID );
        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        
        ManagedRepositoryConfiguration repository = new ManagedRepositoryConfiguration();
        populateRepository( repository );
        repository.setLocation( new File( "target/test/location/new" ).getCanonicalPath() );
        action.setRepository( repository );
        String status = action.commit();
        assertEquals( Action.SUCCESS, status );
        assertEquals( Collections.singletonList( repository ), configuration.getManagedRepositories() );

        roleManagerControl.verify();
        archivaConfigurationControl.verify();
        archivaDaoControl.verify();
        repoContentStatsDaoControl.verify();
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
    {
        Configuration configuration = new Configuration();
        configuration.addManagedRepository( repositoryConfiguration );
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

    private List<RepositoryContentStatistics> createRepositoryContentStatisticsList()
    {
        List<RepositoryContentStatistics> repoStatsList = new ArrayList<RepositoryContentStatistics>();

        repoStatsList.add( createRepositoryContentStatistics() );
        repoStatsList.add( createRepositoryContentStatistics() );

        return repoStatsList;
    }

    private RepositoryContentStatistics createRepositoryContentStatistics()
    {
        RepositoryContentStatistics repoStats = new RepositoryContentStatistics();
        repoStats.setRepositoryId( REPO_ID );
        repoStats.setDuration( 1000 );
        repoStats.setTotalArtifactCount( 100 );
        repoStats.setTotalSize( 10 );
        repoStats.setTotalFileCount( 10 );
        repoStats.setTotalProjectCount( 2 );
        repoStats.setTotalGroupCount( 1 );
        repoStats.setNewFileCount( 3 );
        repoStats.setWhenGathered( new Date( System.currentTimeMillis() ) );

        return repoStats;
    }
}
