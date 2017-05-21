package org.apache.archiva.webdav;

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

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RepositoryServletTest
 */
public class RepositoryServletTest
    extends AbstractRepositoryServletTestCase
{
    private static final String REQUEST_PATH = "http://machine.com/repository/internal/";

    private static final String NEW_REPOSITORY_ID = "new-id";

    private static final String NEW_REPOSITORY_NAME = "New Repository";

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        startRepository();
    }

    @Test
    public void testGetRepository()
        throws Exception
    {

        RepositoryServlet servlet = RepositoryServlet.class.cast( findServlet( "repository" ) );
        assertNotNull( servlet );

        assertRepositoryValid( servlet, REPOID_INTERNAL );
    }


    @Test
    public void testGetRepositoryAfterDelete()
        throws Exception
    {
        RepositoryServlet servlet = RepositoryServlet.class.cast( findServlet( "repository" ) );

        assertNotNull( servlet );

        ArchivaConfiguration archivaConfiguration = servlet.getConfiguration();
        Configuration c = archivaConfiguration.getConfiguration();
        c.removeManagedRepository( c.findManagedRepositoryById( REPOID_INTERNAL ) );
        saveConfiguration( archivaConfiguration );

        ManagedRepository repository = servlet.getRepository( REPOID_INTERNAL );
        assertNull( repository );
    }

    @Test
    public void testGetRepositoryAfterAdd()
        throws Exception
    {
        RepositoryServlet servlet = RepositoryServlet.class.cast( findServlet( "repository" ) );
        assertNotNull( servlet );

        ArchivaConfiguration archivaConfiguration = servlet.getConfiguration();
        Configuration c = archivaConfiguration.getConfiguration();
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( NEW_REPOSITORY_ID );
        repo.setName( NEW_REPOSITORY_NAME );
        File repoRoot = new File( "target/test-repository-root" );
        if ( !repoRoot.exists() )
        {
            repoRoot.mkdirs();
        }
        repo.setLocation( repoRoot.getAbsolutePath() );
        c.addManagedRepository( repo );
        saveConfiguration( archivaConfiguration );

        ManagedRepository repository = servlet.getRepository( NEW_REPOSITORY_ID );
        assertNotNull( repository );
        assertEquals( NEW_REPOSITORY_NAME, repository.getName() );

        // check other is still intact
        assertRepositoryValid( servlet, REPOID_INTERNAL );
    }

    @Test
    public void testGetRepositoryInvalidPathPassthroughPresent()
        throws Exception
    {
        String path = REQUEST_PATH + ".index/filecontent/segments.gen";

        populateRepo( repoRootInternal, ".index/filecontent/segments.gen", "index file" );

        WebRequest request = new GetMethodWebRequest( path );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseOK( response );
        assertEquals( "index file", response.getContentAsString() );
    }

    @Test
    public void testGetRepositoryInvalidPathPassthroughMissing()
        throws Exception
    {
        String path = REQUEST_PATH + ".index/filecontent/foo.bar";

        WebRequest request = new GetMethodWebRequest( path );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertResponseNotFound( response );
        assertThat( response.getContentAsString() ) //
            .contains( "Legacy Maven1 repository not supported anymore." );
    }
}
