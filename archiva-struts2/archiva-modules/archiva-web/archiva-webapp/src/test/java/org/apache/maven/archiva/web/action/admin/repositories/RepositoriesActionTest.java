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
import org.codehaus.plexus.redback.struts2.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.struts2.interceptor.SecureActionException;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

/**
 * Test the repositories action returns the correct data.
 */
public class RepositoriesActionTest
    extends PlexusInSpringTestCase
{
    private RepositoriesAction action;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        // TODO: purely to quiet logging - shouldn't be needed
        String appserverBase = getTestFile( "target/appserver-base" ).getAbsolutePath();
        System.setProperty( "appserver.base", appserverBase );
        action = (RepositoriesAction) lookup( Action.class.getName(), "repositoriesAction" );
    }

    public void testGetRepositories()
        throws Exception
    {
        ServletRunner sr = new ServletRunner();
        ServletUnitClient sc = sr.newClient();

        action.setServletRequest( sc.newInvocation( "http://localhost/admin/repositories.action" ).getRequest() );
        action.prepare();
        String result = action.execute();
        assertEquals( Action.SUCCESS, result );

        // TODO: for some reason servletunit is not populating the port of the servlet request
        assertEquals( "http://localhost:0/repository", action.getBaseUrl() );

        assertNotNull( action.getManagedRepositories() );
        assertNotNull( action.getRemoteRepositories() );
        assertNotNull( action.getRepositoryStatistics() );

        assertEquals( 2, action.getManagedRepositories().size() );
        assertEquals( 2, action.getRemoteRepositories().size() );
        assertEquals( 2, action.getRepositoryStatistics().size() );
    }

    public void testSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }
}
