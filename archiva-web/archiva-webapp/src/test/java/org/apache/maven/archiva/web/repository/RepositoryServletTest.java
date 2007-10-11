package org.apache.maven.archiva.web.repository;

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

import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationEvent;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.util.FileUtils;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

public class RepositoryServletTest
    extends PlexusTestCase
{
    private ServletUnitClient sc;

    private static final String REQUEST_PATH = "http://localhost/repository/internal/path/to/artifact.jar";

    private File repositoryLocation;

    private ArchivaConfiguration configuration;

    private static final String REPOSITORY_ID = "internal";

    private static final String NEW_REPOSITORY_ID = "new-id";

    private static final String NEW_REPOSITORY_NAME = "New Repository";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        // TODO: purely to quiet logging - shouldn't be needed
        String appserverBase = getTestFile( "target/appserver-base" ).getAbsolutePath();
        System.setProperty( "appserver.base", appserverBase );

        configuration = (ArchivaConfiguration) lookup( ArchivaConfiguration.ROLE );

        repositoryLocation = new File( appserverBase, "data/repositories/internal" );

        ServletRunner sr = new ServletRunner();
        sr.registerServlet( "/repository/*", UnauthenticatedRepositoryServlet.class.getName() );
        sc = sr.newClient();
        sc.getSession( true ).getServletContext().setAttribute( PlexusConstants.PLEXUS_KEY, getContainer() );
    }

    public void testPutWithMissingParentCollection()
        throws IOException, SAXException
    {
        FileUtils.deleteDirectory( repositoryLocation );

        WebRequest request = new PutMethodWebRequest( REQUEST_PATH, getClass().getResourceAsStream( "/artifact.jar" ),
                                                      "application/octet-stream" );
        WebResponse response = sc.getResponse( request );
        assertNotNull( "No response received", response );
        assertEquals( "file contents", "artifact.jar\n",
                      FileUtils.fileRead( new File( repositoryLocation, "path/to/artifact.jar" ) ) );
    }

    public void testGetRepository()
        throws IOException, ServletException
    {
        RepositoryServlet servlet = (RepositoryServlet) sc.newInvocation( REQUEST_PATH ).getServlet();
        assertNotNull( servlet );

        ManagedRepositoryConfiguration repository = servlet.getRepository( REPOSITORY_ID );
        assertNotNull( repository );
        assertEquals( "Archiva Managed Internal Repository", repository.getName() );
    }

    public void testGetRepositoryAfterDelete()
        throws IOException, ServletException, RegistryException, IndeterminateConfigurationException
    {
        RepositoryServlet servlet = (RepositoryServlet) sc.newInvocation( REQUEST_PATH ).getServlet();
        assertNotNull( servlet );

        Configuration c = configuration.getConfiguration();
        c.removeManagedRepository( c.findManagedRepositoryById( REPOSITORY_ID ) );
        // TODO it would be better to use a mock configuration and "save" to more accurately reflect the calls made
        servlet.configurationEvent( new ConfigurationEvent( ConfigurationEvent.SAVED) );

        ManagedRepositoryConfiguration repository = servlet.getRepository( REPOSITORY_ID );
        assertNull( repository );
    }

    public void testGetRepositoryAfterAdd()
        throws IOException, ServletException, RegistryException, IndeterminateConfigurationException
    {
        RepositoryServlet servlet = (RepositoryServlet) sc.newInvocation( REQUEST_PATH ).getServlet();
        assertNotNull( servlet );

        Configuration c = configuration.getConfiguration();
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( NEW_REPOSITORY_ID );
        repo.setName( NEW_REPOSITORY_NAME );
        File repoRoot = new File( getBasedir(), "target/test-repository-root" );
        if ( !repoRoot.exists() )
        {
            repoRoot.mkdirs();
        }
        repo.setLocation( repoRoot.getAbsolutePath() );
        c.addManagedRepository( repo );
        // TODO it would be better to use a mock configuration and "save" to more accurately reflect the calls made
        servlet.configurationEvent( new ConfigurationEvent( ConfigurationEvent.SAVED) );

        ManagedRepositoryConfiguration repository = servlet.getRepository( NEW_REPOSITORY_ID );
        assertNotNull( repository );
        assertEquals( NEW_REPOSITORY_NAME, repository.getName() );

        // check other is still intact
        repository = servlet.getRepository( REPOSITORY_ID );
        assertNotNull( repository );
        assertEquals( "Archiva Managed Internal Repository", repository.getName() );
    }
}
