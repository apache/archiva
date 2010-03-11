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
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.AuditEventArgumentsMatcher;
import org.codehaus.plexus.redback.role.RoleManager;
import org.codehaus.plexus.redback.role.RoleManagerException;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.easymock.MockControl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * DeleteManagedRepositoryActionTest 
 *
 * @version $Id$
 */
public class DeleteManagedRepositoryActionTest
    extends PlexusInSpringTestCase
{
    private DeleteManagedRepositoryAction action;

    private RoleManager roleManager;

    private MockControl roleManagerControl;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;
    
    private static final String REPO_ID = "repo-ident";

    private File location;

    private MockControl repositoryStatisticsManagerControl;

    private RepositoryStatisticsManager repositoryStatisticsManager;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = new DeleteManagedRepositoryAction();
        
        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );
        
        roleManagerControl = MockControl.createControl( RoleManager.class );
        roleManager = (RoleManager) roleManagerControl.getMock();
        action.setRoleManager( roleManager );
        location = getTestFile( "target/test/location" );

        repositoryStatisticsManagerControl = MockControl.createControl( RepositoryStatisticsManager.class );
        repositoryStatisticsManager = (RepositoryStatisticsManager) repositoryStatisticsManagerControl.getMock();
        action.setRepositoryStatisticsManager( repositoryStatisticsManager );

        MockControl metadataRepositoryControl = MockControl.createControl( MetadataRepository.class );
        MetadataRepository metadataRepository = (MetadataRepository) metadataRepositoryControl.getMock();
        metadataRepository.deleteRepository( REPO_ID );
        action.setMetadataRepository( metadataRepository );

        metadataRepositoryControl.replay();
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

    public void testDeleteRepositoryConfirmation()
        throws Exception
    {
        ManagedRepositoryConfiguration originalRepository = createRepository();
        Configuration configuration = createConfigurationForEditing( originalRepository );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        ManagedRepositoryConfiguration repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );
        
        String status = action.execute();
        assertEquals( Action.SUCCESS, status );
                
        repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
        assertEquals( Collections.singletonList( originalRepository ), configuration.getManagedRepositories() );
    }

    public void testDeleteRepositoryKeepContent()
        throws Exception
    {
        // even when we keep the content, we don't keep the metadata at this point
        repositoryStatisticsManager.deleteStatistics( REPO_ID );
        repositoryStatisticsManagerControl.replay();

        prepareRoleManagerMock();
        
        Configuration configuration = prepDeletionTest( createRepository(), 4 );

        MockControl control = mockAuditListeners();

        MockControl metadataRepositoryControl = mockMetadataRepository();

        String status = action.deleteEntry();

        assertEquals( Action.SUCCESS, status );

        assertTrue( configuration.getManagedRepositories().isEmpty() );

        assertTrue( location.exists() );

        repositoryStatisticsManagerControl.verify();
        control.verify();
        metadataRepositoryControl.verify();
    }

    private MockControl mockMetadataRepository()
    {
        MockControl metadataRepositoryControl = MockControl.createControl( MetadataRepository.class );
        MetadataRepository metadataRepository = (MetadataRepository) metadataRepositoryControl.getMock();
        metadataRepository.deleteRepository( REPO_ID );
        metadataRepositoryControl.replay();
        action.setMetadataRepository( metadataRepository );
        return metadataRepositoryControl;
    }

    private MockControl mockAuditListeners()
    {
        MockControl control = MockControl.createControl( AuditListener.class );
        AuditListener listener = (AuditListener) control.getMock();
        listener.auditEvent( new AuditEvent( REPO_ID, "guest", null, AuditEvent.DELETE_MANAGED_REPO ) );
        control.setMatcher( new AuditEventArgumentsMatcher() );
        control.replay();
        action.setAuditListeners( Arrays.asList( listener ) );
        return control;
    }

    public void testDeleteRepositoryDeleteContent()
        throws Exception
    {
        repositoryStatisticsManager.deleteStatistics( REPO_ID );
        repositoryStatisticsManagerControl.replay();

        prepareRoleManagerMock();
        
        Configuration configuration = prepDeletionTest( createRepository(), 4 );              
        
        MockControl control = mockAuditListeners();

        MockControl metadataRepositoryControl = mockMetadataRepository();

        String status = action.deleteContents();
        
        assertEquals( Action.SUCCESS, status );

        assertTrue( configuration.getManagedRepositories().isEmpty() );

        assertFalse( location.exists() );

        repositoryStatisticsManagerControl.verify();
        control.verify();
        metadataRepositoryControl.verify();
    }
    
    public void testDeleteRepositoryAndAssociatedProxyConnectors()
        throws Exception
    {
        repositoryStatisticsManager.deleteStatistics( REPO_ID );
        repositoryStatisticsManagerControl.replay();

        Configuration configuration = prepDeletionTest( createRepository(), 5 );
        configuration.addRemoteRepository( createRemoteRepository( "codehaus", "http://repository.codehaus.org" ) );
        configuration.addRemoteRepository( createRemoteRepository( "java.net", "http://dev.java.net/maven2" ) );
        configuration.addProxyConnector( createProxyConnector( REPO_ID, "codehaus" ) );

        prepareRoleManagerMock();

        assertEquals( 1, configuration.getProxyConnectors().size() );
        
        MockControl control = mockAuditListeners();
        MockControl metadataRepositoryControl = mockMetadataRepository();
        String status = action.deleteContents();
        
        assertEquals( Action.SUCCESS, status );

        assertTrue( configuration.getManagedRepositories().isEmpty() );
        assertEquals( 0, configuration.getProxyConnectors().size() );

        assertFalse( location.exists() );

        repositoryStatisticsManagerControl.verify();
        control.verify();
        metadataRepositoryControl.verify();
    }
    
    public void testDeleteRepositoryCancelled()
        throws Exception
    {
        repositoryStatisticsManagerControl.replay();

        ManagedRepositoryConfiguration originalRepository = createRepository();
        Configuration configuration = prepDeletionTest( originalRepository, 3 );
                
        String status = action.execute();
        assertEquals( Action.SUCCESS, status );

        ManagedRepositoryConfiguration repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
        assertEquals( Collections.singletonList( originalRepository ), configuration.getManagedRepositories() );

        assertTrue( location.exists() );

        repositoryStatisticsManagerControl.verify();
    }
    
    public void testDeleteRepositoryAndReposUnderRepoGroup()
        throws Exception
    {
        repositoryStatisticsManager.deleteStatistics( REPO_ID );
        repositoryStatisticsManagerControl.replay();

        Configuration configuration = prepDeletionTest( createRepository(), 5 );
        List<String> repoIds = new ArrayList<String>();
        repoIds.add( REPO_ID );
        configuration.addRepositoryGroup( createRepoGroup( repoIds, "repo.group" ) );

        prepareRoleManagerMock();

        assertEquals( 1, configuration.getRepositoryGroups().size() );
        
        MockControl control = mockAuditListeners();
        MockControl metadataRepositoryControl = mockMetadataRepository();
        String status = action.deleteContents();
        assertEquals( Action.SUCCESS, status );

        assertTrue( configuration.getManagedRepositories().isEmpty() );
        assertEquals( 0, configuration.getRepositoryGroups().get( 0 ).getRepositories().size() );

        assertFalse( location.exists() );

        repositoryStatisticsManagerControl.verify();
        control.verify();
        metadataRepositoryControl.verify();
    }

    private Configuration prepDeletionTest( ManagedRepositoryConfiguration originalRepository, int expectCountGetConfig )
        throws RegistryException, IndeterminateConfigurationException
    {
        location.mkdirs();

        Configuration configuration = createConfigurationForEditing( originalRepository );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, expectCountGetConfig );

        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );

        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        ManagedRepositoryConfiguration repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        assertTrue( location.exists() );
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
    {
        ManagedRepositoryConfiguration r = new ManagedRepositoryConfiguration();
        r.setId( REPO_ID );
        r.setName( "repo name" );
        r.setLocation( location.getAbsolutePath() );
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

    private RemoteRepositoryConfiguration createRemoteRepository(String id, String url)
    {
        RemoteRepositoryConfiguration r = new RemoteRepositoryConfiguration();
        r.setId( id );
        r.setUrl( url );
        r.setLayout( "default" );
        
        return r;
    }
    
    private ProxyConnectorConfiguration createProxyConnector( String managedRepoId, String remoteRepoId )
    {
        ProxyConnectorConfiguration connector = new ProxyConnectorConfiguration();
        connector.setSourceRepoId( managedRepoId );
        connector.setTargetRepoId( remoteRepoId );

        return connector;
    }

    private RepositoryGroupConfiguration createRepoGroup( List<String> repoIds, String repoGroupId )
    {
        RepositoryGroupConfiguration repoGroup = new RepositoryGroupConfiguration();
        repoGroup.setId( repoGroupId );
        repoGroup.setRepositories( repoIds );
        
        return repoGroup;
    }
    
    private void prepareRoleManagerMock()
        throws RoleManagerException
    {
        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManagerControl.setReturnValue( true );
        roleManager.removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, REPO_ID );
        roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.setReturnValue( true );
        roleManager.removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, REPO_ID );
        roleManagerControl.replay();
    }
}
