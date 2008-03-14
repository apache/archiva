/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.archiva.webdav.util;

import org.apache.maven.archiva.webdav.TestableHttpServletRequest;
import org.codehaus.plexus.PlexusTestCase;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;

/**
 * WrappedRepositoryRequestTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: WrappedRepositoryRequestTest.java 6940 2007-10-16 01:02:02Z joakime $
 */
public class WrappedRepositoryRequestTest
    extends PlexusTestCase
{
    private HttpServletRequest createHttpServletGetRequest( String url )
        throws MalformedURLException
    {
        TestableHttpServletRequest testrequest = new TestableHttpServletRequest();
        testrequest.setMethod( "GET" );
        testrequest.setServletPath( "/repository" );
        testrequest.setUrl( url );

        return testrequest;
    }

    public void testShort()
        throws Exception
    {
        HttpServletRequest request = createHttpServletGetRequest( "http://machine.com/repository/org" );
        WrappedRepositoryRequest wrapreq = new WrappedRepositoryRequest( request );
        assertNotNull( wrapreq );

        assertEquals( "/repository", wrapreq.getServletPath() );
        assertEquals( "/org", wrapreq.getPathInfo() );
        assertEquals( "/org", wrapreq.getRequestURI() );
    }

    public void testLonger()
        throws Exception
    {
        HttpServletRequest request = createHttpServletGetRequest( "http://machine.com/repository/"
            + "org/codehaus/plexus/webdav/plexus-webdav-simple/1.0-alpha-3/plexus-webdav-simple-1.0-alpha-3.jar" );

        WrappedRepositoryRequest wrapreq = new WrappedRepositoryRequest( request );
        assertNotNull( wrapreq );

        assertEquals( "/repository", wrapreq.getServletPath() );

        String expected = "/org/codehaus/plexus/webdav/plexus-webdav-simple/1.0-alpha-3/plexus-webdav-simple-1.0-alpha-3.jar";
        assertEquals( expected, wrapreq.getPathInfo() );
        assertEquals( expected, wrapreq.getRequestURI() );
    }
}
