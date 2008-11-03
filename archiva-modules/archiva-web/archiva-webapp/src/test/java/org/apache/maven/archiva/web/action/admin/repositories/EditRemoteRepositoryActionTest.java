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
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;

import java.util.Collections;

/**
 * EditRemoteRepositoryActionTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class EditRemoteRepositoryActionTest
    extends PlexusInSpringTestCase
{
    private static final String REPO_ID = "remote-repo-ident";

    private EditRemoteRepositoryAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    public void testEditRemoteRepository()
        throws Exception
    {
        Configuration configuration = createConfigurationForEditing( createRepository() );
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfigurationControl.setReturnValue( configuration );
        archivaConfiguration.save( configuration );
        archivaConfigurationControl.replay();

        action.setRepoid( REPO_ID );
        action.prepare();
        assertEquals( REPO_ID, action.getRepoid() );
        RemoteRepositoryConfiguration repository = action.getRepository();
        populateRepository( repository );
        repository.setName( "new repo name" );

        String status = action.commit();
        assertEquals( Action.SUCCESS, status );

        RemoteRepositoryConfiguration newRepository = createRepository();
        newRepository.setName( "new repo name" );
        assertRepositoryEquals( repository, newRepository );
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
        RemoteRepositoryConfiguration repository = action.getRepository();
        assertNotNull( repository );
        assertRepositoryEquals( repository, createRepository() );

        String status = action.input();
        assertEquals( Action.INPUT, status );
        repository = action.getRepository();
        assertRepositoryEquals( repository, createRepository() );
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
    
    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = (EditRemoteRepositoryAction) lookup( Action.class.getName(), "editRemoteRepositoryAction" );

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();
        action.setArchivaConfiguration( archivaConfiguration );
    }
}
