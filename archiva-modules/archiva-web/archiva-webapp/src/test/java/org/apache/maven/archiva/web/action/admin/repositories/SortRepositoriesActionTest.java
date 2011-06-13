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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.struts2.StrutsSpringTestCase;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.easymock.MockControl;

import com.opensymphony.xwork2.Action;

/**
 * SortRepositoriesActionTest
 * 
 * @version
 */
public class SortRepositoriesActionTest 
    extends StrutsSpringTestCase
{
    private static final String REPO_GROUP_ID = "repo-group-ident";
	
    private static final String REPO1_ID = "managed-repo-ident-1";
	
    private static final String REPO2_ID = "managed-repo-ident-2";
	
    private static final String REPO3_ID = "managed-repo-ident-3";
	
    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;
    
    private SortRepositoriesAction action;

    @Override
    protected String[] getContextLocations()
    {
        return new String[]{ "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" };
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
	    
        //action = (SortRepositoriesAction) lookup( Action.class.getName(), "sortRepositoriesAction" );

        action = (SortRepositoriesAction) getActionProxy( "/admin/sortDownRepositoryFromGroup.action" ).getAction();
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
	
        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }
    
    public void testSortDownFirstRepository()
        throws Exception
    {
        Configuration configuration = createInitialConfiguration();
        
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 4 );
        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();
    	
        RepositoryGroupConfiguration repoGroup = (RepositoryGroupConfiguration) configuration.getRepositoryGroups().get( 0 );
        java.util.List<String> repositories = repoGroup.getRepositories();
    	
        assertEquals( 3, repositories.size() );
        assertEquals( REPO1_ID, repositories.get( 0 ) );
        assertEquals( REPO2_ID, repositories.get( 1 ) );
        assertEquals( REPO3_ID, repositories.get( 2 ) );
    	
        // sort down first repo
        action.setRepoGroupId( repoGroup.getId() );
        action.setTargetRepo( REPO1_ID );
    	
        String result = action.sortDown();
        assertEquals( Action.SUCCESS, result );
    	
        repoGroup = (RepositoryGroupConfiguration) configuration.getRepositoryGroups().get( 0 );
        repositories = repoGroup.getRepositories();
        assertEquals( 3, repositories.size() );
        assertEquals( REPO2_ID, repositories.get( 0 ) );
        assertEquals( REPO1_ID, repositories.get( 1 ) );
        assertEquals( REPO3_ID, repositories.get( 2 ) );
    }
    
    public void testSortDownLastRepository()
        throws Exception
    {
        Configuration configuration = createInitialConfiguration();
        
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 4 );
        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();
    	
        RepositoryGroupConfiguration repoGroup = (RepositoryGroupConfiguration) configuration.getRepositoryGroups().get( 0 );
        java.util.List<String> repositories = repoGroup.getRepositories();
        
        assertEquals( 3, repositories.size() );
        assertEquals( REPO1_ID, repositories.get( 0 ) );
        assertEquals( REPO2_ID, repositories.get( 1 ) );
        assertEquals( REPO3_ID, repositories.get( 2 ) );
    	
        // sort down last repo
        action.setRepoGroupId( repoGroup.getId() );
        action.setTargetRepo( REPO3_ID );
    	
        String result = action.sortDown();
        assertEquals( Action.SUCCESS, result );
    	
        repoGroup = (RepositoryGroupConfiguration) configuration.getRepositoryGroups().get( 0 );
        repositories = repoGroup.getRepositories();
        assertEquals( 3, repositories.size() );
        assertEquals( REPO1_ID, repositories.get( 0 ) );
        assertEquals( REPO2_ID, repositories.get( 1 ) );
        assertEquals( REPO3_ID, repositories.get( 2 ) );
    }
    
    public void testSortUpLastRepository()
        throws Exception
    {
        Configuration configuration = createInitialConfiguration();
        
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 4 );
        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();
    	
        RepositoryGroupConfiguration repoGroup = (RepositoryGroupConfiguration) configuration.getRepositoryGroups().get( 0 );
        java.util.List<String> repositories = repoGroup.getRepositories();
    	
        assertEquals( 3, repositories.size() );
        assertEquals( REPO1_ID, repositories.get( 0 ) );
        assertEquals( REPO2_ID, repositories.get( 1 ) );
        assertEquals( REPO3_ID, repositories.get( 2 ) );
    	
        // sort up last repo
        action.setRepoGroupId( repoGroup.getId() );
        action.setTargetRepo( REPO3_ID );
    	
        String result = action.sortUp();
        assertEquals( Action.SUCCESS, result );
    	
        repoGroup = (RepositoryGroupConfiguration) configuration.getRepositoryGroups().get( 0 );
        repositories = repoGroup.getRepositories();
        assertEquals( 3, repositories.size() );
        assertEquals( REPO1_ID, repositories.get( 0 ) );
        assertEquals( REPO3_ID, repositories.get( 1 ) );
        assertEquals( REPO2_ID, repositories.get( 2 ) );
    }
    
    public void testSortUpFirstRepository()
        throws Exception
    {
        Configuration configuration = createInitialConfiguration();
        
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration, 4 );
        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();
    	
        RepositoryGroupConfiguration repoGroup = (RepositoryGroupConfiguration) configuration.getRepositoryGroups().get( 0 );
        java.util.List<String> repositories = repoGroup.getRepositories();
        
        assertEquals( 3, repositories.size() );
        assertEquals( REPO1_ID, repositories.get( 0 ) );
        assertEquals( REPO2_ID, repositories.get( 1 ) );
        assertEquals( REPO3_ID, repositories.get( 2 ) );
        
        // sort up first repo
        action.setRepoGroupId( repoGroup.getId() );
        action.setTargetRepo( REPO1_ID );
    	
        String result = action.sortUp();
        assertEquals( Action.SUCCESS, result );
        
        repoGroup = (RepositoryGroupConfiguration) configuration.getRepositoryGroups().get( 0 );
        repositories = repoGroup.getRepositories();
        assertEquals( 3, repositories.size() );
        assertEquals( REPO1_ID, repositories.get( 0 ) );
        assertEquals( REPO2_ID, repositories.get( 1 ) );
        assertEquals( REPO3_ID, repositories.get( 2 ) );
    }
    
    private Configuration createInitialConfiguration()
    {
        Configuration config = new Configuration();
        
        RepositoryGroupConfiguration repoGroup = new RepositoryGroupConfiguration();
        repoGroup.setId( REPO_GROUP_ID );
        repoGroup.addRepository( REPO1_ID );
        repoGroup.addRepository( REPO2_ID );
        repoGroup.addRepository( REPO3_ID );
		
        config.addRepositoryGroup( repoGroup );
        
        return config;
    }
}
