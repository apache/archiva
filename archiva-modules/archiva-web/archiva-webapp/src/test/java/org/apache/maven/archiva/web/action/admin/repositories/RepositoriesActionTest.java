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
import org.apache.archiva.admin.repository.group.DefaultRepositoryGroupAdmin;
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.remote.DefaultRemoteRepositoryAdmin;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.memory.TestRepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatistics;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.struts2.StrutsSpringTestCase;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.easymock.MockControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the repositories action returns the correct data.
 */
public class RepositoriesActionTest
    extends StrutsSpringTestCase
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    private RepositoriesAction action;

    ArchivaConfiguration originalArchivaConfiguration;

    protected void setUp()
        throws Exception
    {

        super.setUp();

        action = (RepositoriesAction) getActionProxy( "/admin/index.action" ).getAction();
        originalArchivaConfiguration =
            ( (DefaultRepositoryGroupAdmin) action.getRepositoryGroupAdmin() ).getArchivaConfiguration();
        // some other test are modifying archivaConfiguration with a mocked instance : this test need the real one
        // so use the real one from spring, backup the mock and restore it at the end (tearDown)
        ArchivaConfiguration real = applicationContext.getBean( ArchivaConfiguration.class );
        ( (DefaultRepositoryGroupAdmin) action.getRepositoryGroupAdmin() ).setArchivaConfiguration( real );
        ( (DefaultManagedRepositoryAdmin) action.getManagedRepositoryAdmin() ).setArchivaConfiguration( real );
        ( (DefaultRemoteRepositoryAdmin) action.getRemoteRepositoryAdmin() ).setArchivaConfiguration( real );
    }


    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        ( (DefaultRepositoryGroupAdmin) action.getRepositoryGroupAdmin() ).setArchivaConfiguration(
            originalArchivaConfiguration );
        ( (DefaultManagedRepositoryAdmin) action.getManagedRepositoryAdmin() ).setArchivaConfiguration(
            originalArchivaConfiguration );
        ( (DefaultRemoteRepositoryAdmin) action.getRemoteRepositoryAdmin() ).setArchivaConfiguration(
            originalArchivaConfiguration );
    }

    @Override
    protected String[] getContextLocations()
    {
        return new String[]{ "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" };
    }

    public void testGetRepositories()
        throws Exception
    {
        try
        {
            MockControl control = MockControl.createControl( MetadataRepository.class );
            MetadataRepository metadataRepository = (MetadataRepository) control.getMock();
            control.expectAndReturn( metadataRepository.getMetadataFacets( "internal", RepositoryStatistics.FACET_ID ),
                                     Arrays.asList( "20091125.123456.678" ) );
            control.expectAndReturn(
                metadataRepository.getMetadataFacet( "internal", RepositoryStatistics.FACET_ID, "20091125.123456.678" ),
                new RepositoryStatistics() );
            control.expectAndReturn( metadataRepository.getMetadataFacets( "snapshots", RepositoryStatistics.FACET_ID ),
                                     Arrays.asList( "20091112.012345.012" ) );
            control.expectAndReturn( metadataRepository.getMetadataFacet( "snapshots", RepositoryStatistics.FACET_ID,
                                                                          "20091112.012345.012" ),
                                     new RepositoryStatistics() );
            control.replay();

            RepositorySession session = mock( RepositorySession.class );
            when( session.getRepository() ).thenReturn( metadataRepository );
            TestRepositorySessionFactory factory =
                applicationContext.getBean( "repositorySessionFactory#test", TestRepositorySessionFactory.class );
            factory.setRepositorySession( session );

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

            control.verify();
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw e;
        }
    }

    public void testSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = action.getSecureActionBundle();
        assertTrue( bundle.requiresAuthentication() );
        assertEquals( 1, bundle.getAuthorizationTuples().size() );
    }
}
