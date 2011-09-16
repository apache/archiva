package org.apache.archiva.model;

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

import junit.framework.TestCase;

/**
 * RepositoryURLTest 
 *
 * @version $Id$
 */
public class RepositoryURLTest
    extends TestCase
{
    private static final String NO_HOST = null;

    private static final String NO_PORT = null;

    private static final String NO_USER = null;

    private static final String NO_PASS = null;

    private void assertURL( String url, String expectedProtocol, String expectedHost, String expectedPort,
                            String expectedPath, String expectedUsername, String expectedPassword )
    {
        RepositoryURL rurl = new RepositoryURL( url );
        assertEquals( "Protocol", expectedProtocol, rurl.getProtocol() );
        assertEquals( "Host", expectedHost, rurl.getHost() );
        assertEquals( "Port", expectedPort, rurl.getPort() );
        assertEquals( "Path", expectedPath, rurl.getPath() );
        assertEquals( "Username", expectedUsername, rurl.getUsername() );
        assertEquals( "Password", expectedPassword, rurl.getPassword() );
    }

    public void testFileUrlNormal()
    {
        assertURL( "file:///home/joakim/code/test/this/", "file", NO_HOST, NO_PORT, "/home/joakim/code/test/this/",
                   NO_USER, NO_PASS );
    }

    public void testFileUrlShort()
    {
        assertURL( "file:/home/joakim/code/test/this/", "file", NO_HOST, NO_PORT, "/home/joakim/code/test/this/",
                   NO_USER, NO_PASS );
    }

    public void testHttpUrlPathless()
    {
        assertURL( "http://machine", "http", "machine", NO_PORT, "/", NO_USER, NO_PASS );
    }

    public void testHttpUrlWithPort()
    {
        assertURL( "http://machine:8080/", "http", "machine", "8080", "/", NO_USER, NO_PASS );
    }

    public void testHttpUrlWithUsernamePassword()
    {
        assertURL( "http://user:pass@machine/secured/", "http", "machine", NO_PORT, "/secured/", "user", "pass" );
    }

    public void testHttpUrlWithUsernameNoPassword()
    {
        assertURL( "http://user@machine/secured/", "http", "machine", NO_PORT, "/secured/", "user", NO_PASS );
    }

    public void testHttpUrlWithUsernamePasswordAndPort()
    {
        assertURL( "http://user:pass@machine:9090/secured/", "http", "machine", "9090", "/secured/", "user", "pass" );
    }

    public void testBogusWithPath()
    {
        // This should not fail.  The intent of RepositoryURL is to have it support oddball protocols that
        // are used by maven-scm and maven-wagon (unlike java.net.URL)
        assertURL( "bogus://a.machine.name.com/path/to/resource/file.txt", "bogus", "a.machine.name.com", NO_PORT,
                   "/path/to/resource/file.txt", NO_USER, NO_PASS );
    }
}
