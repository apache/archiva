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
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

public class RepositoryServletTest
    extends PlexusTestCase
{
    private ServletUnitClient sc;

    private String appserverBase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        appserverBase = getTestFile( "target/appserver-base" ).getAbsolutePath();
        System.setProperty( "appserver.base", appserverBase );
        System.setProperty( "derby.system.home", appserverBase );

        ServletRunner sr = new ServletRunner();
        sr.registerServlet( "/repository/*", UnauthenticatedRepositoryServlet.class.getName() );
        sc = sr.newClient();
        sc.getSession( true ).getServletContext().setAttribute( PlexusConstants.PLEXUS_KEY, getContainer() );
    }

    public void testPutWithMissingParentCollection()
        throws IOException, SAXException
    {
        File repository = new File( appserverBase, "data/repositories/internal" );
        FileUtils.deleteDirectory( repository );

        WebRequest request = new PutMethodWebRequest( "http://localhost/repository/internal/path/to/artifact.jar",
                                                      getClass().getResourceAsStream( "/artifact.jar" ),
                                                      "application/octet-stream" );
        WebResponse response = sc.getResponse( request );
        assertNotNull( "No response received", response );
        assertEquals( "file contents", "artifact.jar\n",
                      FileUtils.fileRead( new File( repository, "path/to/artifact.jar" ) ) );
    }
}
