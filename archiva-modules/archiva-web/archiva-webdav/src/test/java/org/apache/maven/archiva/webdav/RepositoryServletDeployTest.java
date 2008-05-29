package org.apache.maven.archiva.webdav;

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

import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;


/**
 * Deploy / Put Test cases for RepositoryServlet.  
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryServletDeployTest
    extends AbstractRepositoryServletTestCase
{
    public void testPutWithMissingParentCollection()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        String putUrl = "http://machine.com/repository/internal/path/to/artifact.jar";
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        assertNotNull( "artifact.jar inputstream", is );

        WebRequest request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );

        WebResponse response = sc.getResponse( request );
        assertResponseCreated( response );
        assertFileContents( "artifact.jar\n", repoRootInternal, "path/to/artifact.jar" );
    }
    
    protected void assertResponseCreated( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        assertEquals( "Should have been a 201/CREATED response code.", HttpServletResponse.SC_CREATED, response
            .getResponseCode() );
    }
}
