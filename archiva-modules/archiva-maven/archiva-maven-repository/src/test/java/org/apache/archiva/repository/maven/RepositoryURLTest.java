package org.apache.archiva.repository.maven;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import junit.framework.TestCase;
import org.apache.archiva.model.RepositoryURL;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;

/**
 * RepositoryURLTest 
 *
 *
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class RepositoryURLTest
    extends TestCase
{
    private void assertURL( String actualURL, String protocol, String username, String password, String hostname,
                            String port, String path )
    {
        RepositoryURL url = new RepositoryURL( actualURL );

        assertEquals( protocol, url.getProtocol() );
        assertEquals( username, url.getUsername() );
        assertEquals( password, url.getPassword() );
        assertEquals( hostname, url.getHost() );
        assertEquals( port, url.getPort() );
        assertEquals( path, url.getPath() );
    }
    
    @Test
    public void testProtocolHttp()
        throws MalformedURLException
    {
        assertURL( "http://localhost/path/to/resource.txt", "http", null, null, "localhost", null,
                   "/path/to/resource.txt" );
    }

    @Test
    public void testProtocolWagonWebdav()
        throws MalformedURLException
    {
        assertURL( "dav:http://localhost/path/to/resource.txt", "dav:http", null, null, "localhost", null,
                   "/path/to/resource.txt" );
    }

    @Test
    public void testProtocolHttpWithPort()
        throws MalformedURLException
    {
        assertURL( "http://localhost:9090/path/to/resource.txt", "http", null, null, "localhost", "9090",
                   "/path/to/resource.txt" );
    }

    @Test
    public void testProtocolHttpWithUsername()
        throws MalformedURLException
    {
        assertURL( "http://user@localhost/path/to/resource.txt", "http", "user", null, "localhost", null,
                   "/path/to/resource.txt" );
    }

    @Test
    public void testProtocolHttpWithUsernamePassword()
        throws MalformedURLException
    {
        assertURL( "http://user:pass@localhost/path/to/resource.txt", "http", "user", "pass", "localhost", null,
                   "/path/to/resource.txt" );
    }

    @Test
    public void testProtocolHttpWithUsernamePasswordPort()
        throws MalformedURLException
    {
        assertURL( "http://user:pass@localhost:9090/path/to/resource.txt", "http", "user", "pass", "localhost", "9090",
                   "/path/to/resource.txt" );
    }
}
