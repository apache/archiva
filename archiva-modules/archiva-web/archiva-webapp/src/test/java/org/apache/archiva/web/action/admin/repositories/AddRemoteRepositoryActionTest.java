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
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.repository.remote.DefaultRemoteRepositoryAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.web.action.AbstractActionTestCase;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.easymock.MockControl;

import java.util.Collections;

/**
 * AddRemoteRepositoryActionTest
 *
 * @version $Id$
 */
public class AddRemoteRepositoryActionTest
    extends AbstractActionTestCase
{
    private AddRemoteRepositoryAction action;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    private static final String REPO_ID = "remote-repo-ident";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        action = (AddRemoteRepositoryAction) getActionProxy( "/admin/addRemoteRepository.action" ).getAction();

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();

        ( (DefaultRemoteRepositoryAdmin) action.getRemoteRepositoryAdmin() ).setArchivaConfiguration(
            archivaConfiguration );
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

    public void testAddRemoteRepositoryInitialPage()
        throws Exception
    {
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( new Configuration() );
        archivaConfigurationControl.replay();

        action.prepare();
        RemoteRepository configuration = action.getRepository();
        assertNotNull( configuration );
        assertNull( configuration.getId() );

        String status = action.input();
        assertEquals( Action.INPUT, status );
    }

    public void testAddRemoteRepository()
        throws Exception
    {
        Configuration configuration = new Configuration();
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfiguration.save( configuration );

        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( configuration );

        archivaConfigurationControl.replay();

        action.prepare();
        RemoteRepository repository = action.getRepository();
        populateRepository( repository );

        assertEquals( "url ", repository.getUrl() );

        String status = action.commit();
        assertEquals( Action.SUCCESS, status );

        assertEquals( Collections.singletonList( repository ),
                      action.getRemoteRepositoryAdmin().getRemoteRepositories() );

        assertEquals( "url", repository.getUrl() );

        archivaConfigurationControl.verify();
    }

    private void populateRepository( RemoteRepository repository )
    {
        repository.setId( REPO_ID );
        repository.setName( "repo name" );
        repository.setUrl( "url " );
        repository.setLayout( "default" );
    }

    // TODO: test errors during add, other actions
}
