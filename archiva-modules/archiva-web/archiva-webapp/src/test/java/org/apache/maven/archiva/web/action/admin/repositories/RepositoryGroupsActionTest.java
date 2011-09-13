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

import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import com.opensymphony.xwork2.Action;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.group.RepositoryGroup;
import org.apache.archiva.admin.repository.group.DefaultRepositoryGroupAdmin;
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.maven.archiva.web.action.AbstractActionTestCase;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.easymock.MockControl;

import java.util.Collections;
import java.util.List;

/**
 * RepositoryGroupsActionTest
 */
public class RepositoryGroupsActionTest
    extends AbstractActionTestCase
{
    private static final String REPO_GROUP_ID = "repo-group-ident";

    private static final String REPO1_ID = "managed-repo-ident-1";

    private static final String REPO2_ID = "managed-repo-ident-2";

    private RepositoryGroupsAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = (RepositoryGroupsAction) getActionProxy( "/admin/repositoryGroups.action" ).getAction();

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );
        ( (DefaultRepositoryGroupAdmin) action.getRepositoryGroupAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
        ( (DefaultManagedRepositoryAdmin) ( (DefaultRepositoryGroupAdmin) action.getRepositoryGroupAdmin() ).getManagedRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
    }

    public void testSecureActionBundle()
        throws SecureActionException, RepositoryAdminException
    {
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration(), 3 );
        archivaConfigurationControl.replay();

        action.prepare();
        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }

    public void testAddRepositoryGroup()
        throws Exception
    {
        Configuration configuration = new Configuration();
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 6 );

        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.prepare();
        RepositoryGroup repositoryGroup = action.getRepositoryGroup();
        repositoryGroup.setId( REPO_GROUP_ID );

        String status = action.addRepositoryGroup();
        assertEquals( Action.SUCCESS, status );

        assertEquals( Collections.singletonList( repositoryGroup ),
                      action.getRepositoryGroupAdmin().getRepositoriesGroups() );

        archivaConfigurationControl.verify();
    }

    public void testAddEmptyRepositoryGroup()
        throws Exception
    {
        Configuration configuration = new Configuration();
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 5 );

        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        action.prepare();

        String status = action.addRepositoryGroup();
        assertEquals( Action.ERROR, status );

        assertEquals( 0, configuration.getRepositoryGroups().size() );
    }

    public void testAddDuplicateRepositoryGroup()
        throws Exception
    {
        Configuration configuration = new Configuration();
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 8 );

        archivaConfiguration.save( configuration );

        archivaConfigurationControl.replay();

        action.prepare();
        RepositoryGroup repositoryGroup = action.getRepositoryGroup();
        repositoryGroup.setId( REPO_GROUP_ID );

        String status = action.addRepositoryGroup();
        assertEquals( Action.SUCCESS, status );

        assertEquals( Collections.singletonList( repositoryGroup ),
                      action.getRepositoryGroupAdmin().getRepositoriesGroups() );

        repositoryGroup.setId( REPO_GROUP_ID );
        status = action.addRepositoryGroup();

        assertEquals( Action.ERROR, status );
        assertEquals( Collections.singletonList( repositoryGroup ),
                      action.getRepositoryGroupAdmin().getRepositoriesGroups() );
    }

    public void testGetRepositoryGroups()
        throws Exception
    {
        ServletRunner sr = new ServletRunner();
        ServletUnitClient sc = sr.newClient();

        Configuration configuration = createInitialConfiguration();

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 6 );
        archivaConfigurationControl.replay();

        action.setServletRequest( sc.newInvocation( "http://localhost/admin/repositoryGroups.action" ).getRequest() );
        action.prepare();
        String result = action.execute();
        assertEquals( Action.SUCCESS, result );

        assertEquals( "http://localhost:0/repository", action.getBaseUrl() );

        assertNotNull( action.getRepositoryGroups() );
        assertEquals( 1, action.getRepositoryGroups().size() );
        assertEquals( 2, action.getManagedRepositories().size() );

        RepositoryGroup repoGroup = action.getRepositoryGroups().get( REPO_GROUP_ID );

        assertEquals( 1, repoGroup.getRepositories().size() );
        assertEquals( REPO1_ID, repoGroup.getRepositories().get( 0 ) );
        assertNotNull( action.getGroupToRepositoryMap() );
        assertEquals( 1, action.getGroupToRepositoryMap().size() );

        List<String> repos = action.getGroupToRepositoryMap().get( repoGroup.getId() );
        assertEquals( 1, repos.size() );
        assertEquals( REPO2_ID, repos.get( 0 ) );
    }

    public void testAddRepositoryToGroup()
        throws Exception
    {
        Configuration configuration = createInitialConfiguration();

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 17 );
        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.prepare();
        String result = action.execute();
        assertEquals( Action.SUCCESS, result );

        assertNotNull( action.getRepositoryGroups() );
        assertEquals( 1, action.getRepositoryGroups().size() );
        assertEquals( 2, action.getManagedRepositories().size() );

        RepositoryGroup repoGroup = action.getRepositoryGroups().get( REPO_GROUP_ID );
        assertEquals( 1, repoGroup.getRepositories().size() );
        assertEquals( REPO1_ID, repoGroup.getRepositories().get( 0 ) );

        assertNotNull( action.getGroupToRepositoryMap() );
        assertEquals( 1, action.getGroupToRepositoryMap().size() );

        List<String> repos = action.getGroupToRepositoryMap().get( repoGroup.getId() );
        assertEquals( 1, repos.size() );
        assertEquals( REPO2_ID, repos.get( 0 ) );

        action.setRepoGroupId( REPO_GROUP_ID );
        action.setRepoId( REPO2_ID );

        result = action.addRepositoryToGroup();
        assertEquals( Action.SUCCESS, result );

        action.prepare();
        result = action.execute();
        assertEquals( Action.SUCCESS, result );

        assertEquals( 1, action.getRepositoryGroups().size() );
        repoGroup = action.getRepositoryGroups().get( REPO_GROUP_ID );
        assertEquals( 2, repoGroup.getRepositories().size() );
        assertEquals( REPO1_ID, repoGroup.getRepositories().get( 0 ) );
        assertEquals( REPO2_ID, repoGroup.getRepositories().get( 1 ) );

        assertEquals( 0, action.getGroupToRepositoryMap().size() );
        assertNull( action.getGroupToRepositoryMap().get( repoGroup.getId() ) );
    }

    public void testRemoveRepositoryFromGroup()
        throws Exception
    {
        Configuration configuration = createInitialConfiguration();

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 13 );
        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.prepare();
        String result = action.execute();
        assertEquals( Action.SUCCESS, result );

        assertNotNull( action.getRepositoryGroups() );
        assertEquals( 1, action.getRepositoryGroups().size() );
        assertEquals( 2, action.getManagedRepositories().size() );

        RepositoryGroup repoGroup = action.getRepositoryGroups().get( REPO_GROUP_ID );
        assertEquals( 1, repoGroup.getRepositories().size() );
        assertEquals( REPO1_ID, repoGroup.getRepositories().get( 0 ) );

        assertNotNull( action.getGroupToRepositoryMap() );
        assertEquals( 1, action.getGroupToRepositoryMap().size() );

        List<String> repos = action.getGroupToRepositoryMap().get( repoGroup.getId() );
        assertEquals( 1, repos.size() );
        assertEquals( REPO2_ID, repos.get( 0 ) );

        action.setRepoGroupId( REPO_GROUP_ID );
        action.setRepoId( REPO1_ID );

        result = action.removeRepositoryFromGroup();
        assertEquals( Action.SUCCESS, result );

        action.prepare();
        result = action.execute();
        assertEquals( Action.SUCCESS, result );

        repoGroup = action.getRepositoryGroups().get( REPO_GROUP_ID );
        assertEquals( 0, repoGroup.getRepositories().size() );

        assertNotNull( action.getGroupToRepositoryMap() );
        assertEquals( 1, action.getGroupToRepositoryMap().size() );

        repos = action.getGroupToRepositoryMap().get( repoGroup.getId() );
        assertEquals( 2, repos.size() );
        assertEquals( REPO1_ID, repos.get( 0 ) );
        assertEquals( REPO2_ID, repos.get( 1 ) );
    }

    public void testAddDuplicateRepositoryToGroup()
        throws Exception
    {
        Configuration configuration = createInitialConfiguration();

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 6 );
        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.prepare();
        String result = action.execute();
        assertEquals( Action.SUCCESS, result );

        assertNotNull( action.getRepositoryGroups() );
        assertEquals( 1, action.getRepositoryGroups().size() );
        assertEquals( 2, action.getManagedRepositories().size() );

        RepositoryGroup repoGroup = action.getRepositoryGroups().get( REPO_GROUP_ID );
        assertEquals( 1, repoGroup.getRepositories().size() );
        assertEquals( REPO1_ID, repoGroup.getRepositories().get( 0 ) );

        assertNotNull( action.getGroupToRepositoryMap() );
        assertEquals( 1, action.getGroupToRepositoryMap().size() );

        List<String> repos = action.getGroupToRepositoryMap().get( repoGroup.getId() );
        assertEquals( 1, repos.size() );
        assertEquals( REPO2_ID, repos.get( 0 ) );

        action.setRepoGroupId( REPO_GROUP_ID );
        action.setRepoId( REPO1_ID );

        result = action.addRepositoryToGroup();
        assertEquals( Action.ERROR, result );
    }

    public void testRemoveRepositoryNotInGroup()
        throws Exception
    {
        Configuration configuration = createInitialConfiguration();

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 6 );
        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.prepare();
        String result = action.execute();
        assertEquals( Action.SUCCESS, result );

        assertNotNull( action.getRepositoryGroups() );
        assertEquals( 1, action.getRepositoryGroups().size() );
        assertEquals( 2, action.getManagedRepositories().size() );

        RepositoryGroup repoGroup = action.getRepositoryGroups().get( REPO_GROUP_ID );
        assertEquals( 1, repoGroup.getRepositories().size() );
        assertEquals( REPO1_ID, repoGroup.getRepositories().get( 0 ) );

        assertNotNull( action.getGroupToRepositoryMap() );
        assertEquals( 1, action.getGroupToRepositoryMap().size() );

        List<String> repos = action.getGroupToRepositoryMap().get( repoGroup.getId() );
        assertEquals( 1, repos.size() );
        assertEquals( REPO2_ID, repos.get( 0 ) );

        action.setRepoGroupId( REPO_GROUP_ID );
        action.setRepoId( REPO2_ID );

        result = action.removeRepositoryFromGroup();
        assertEquals( Action.ERROR, result );
    }

    private Configuration createInitialConfiguration()
    {
        Configuration config = new Configuration();

        ManagedRepositoryConfiguration managedRepo1 = new ManagedRepositoryConfiguration();
        managedRepo1.setId( REPO1_ID );

        config.addManagedRepository( managedRepo1 );

        ManagedRepositoryConfiguration managedRepo2 = new ManagedRepositoryConfiguration();
        managedRepo2.setId( REPO2_ID );

        config.addManagedRepository( managedRepo2 );

        RepositoryGroupConfiguration repoGroup = new RepositoryGroupConfiguration();
        repoGroup.setId( REPO_GROUP_ID );
        repoGroup.addRepository( REPO1_ID );

        config.addRepositoryGroup( repoGroup );

        return config;
    }
}
