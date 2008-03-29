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

package org.apache.maven.archiva.webdav.servlet.multiplexed;

import junit.framework.TestCase;
import org.apache.maven.archiva.webdav.TestableHttpServletRequest;
import org.apache.maven.archiva.webdav.util.WrappedRepositoryRequest;

import java.net.MalformedURLException;

/**
 * MultiplexedDavServerRequestTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: MultiplexedDavServerRequestTest.java 6940 2007-10-16 01:02:02Z joakime $
 */
public class MultiplexedDavServerRequestTest
    extends TestCase
{
    private void assertMultiURL( String expectedPrefix, String expectedLogicalResource, String url )
        throws MalformedURLException
    {
        TestableHttpServletRequest testrequest = new TestableHttpServletRequest();
        testrequest.setMethod( "GET" );
        testrequest.setServletPath( "/repository" );
        testrequest.setUrl( url );

        WrappedRepositoryRequest wraprequest = new WrappedRepositoryRequest( testrequest );
        MultiplexedDavServerRequest multirequest = new MultiplexedDavServerRequest( wraprequest );

        assertEquals( expectedPrefix, multirequest.getPrefix() );
        assertEquals( expectedLogicalResource, multirequest.getLogicalResource() );
    }

    public void testNormalUsage()
        throws MalformedURLException
    {
        assertMultiURL( "corporate", "/", "http://localhost:9091/repository/corporate" );
        assertMultiURL( "corporate", "/dom4j/dom4j/1.4", "http://localhost:9091/repository/corporate/dom4j/dom4j/1.4" );
    }

    public void testHacker()
        throws MalformedURLException
    {
        assertMultiURL( "corporate", "/etc/passwd", "http://localhost:9091/repository/corporate//etc/passwd" );
        // Since the double ".." puts the path outside of the /corporate/, it will return "/" as a hack fallback.
        assertMultiURL( "corporate", "/", "http://localhost:9091/repository/corporate/dom4j/../../etc/passwd" );
        assertMultiURL( "corporate", "/", "http://localhost:9091/repository/corporate/../.." );
    }
}
