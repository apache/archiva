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

package org.apache.maven.archiva.webdav.test;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpURL;
import org.apache.webdav.lib.WebdavResource;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.IOException;

/**
 * AbstractMultiWebdavProviderTestCase 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: AbstractMultiWebdavProviderTestCase.java 5997 2007-03-04 19:41:15Z joakime $
 */
public abstract class AbstractMultiWebdavProviderTestCase
    extends AbstractWebdavProviderTestCase
{
    File serverSandboxDir;

    File serverSnapshotsDir;

    /** The Jetty Server. */
    private Server server;

    private WebdavResource davSnapshots;

    private WebdavResource davSandbox;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        // Initialize server contents directory.

        serverSandboxDir = getTestDir( "sandbox" );
        serverSnapshotsDir = getTestDir( "snapshots" );

        // Setup the Jetty Server.

        System.setProperty( "DEBUG", "" );
        System.setProperty( "org.mortbay.log.class", "org.slf4j.impl.SimpleLogger" );

        server = new Server( PORT );

        WebAppContext webAppConfig = new WebAppContext( server, getTestFile( "src/test/webapp" ).getCanonicalPath(), "/" );
        ServletHandler servletHandler = webAppConfig.getServletHandler();

        ServletHolder holder = servletHandler.addServletWithMapping( TestMultiWebDavServlet.class, CONTEXT + "/*" );
        holder.setInitParameter( "root.sandbox", serverSandboxDir.getAbsolutePath() );
        holder.setInitParameter( "root.snapshots", serverSnapshotsDir.getAbsolutePath() );

        System.out.println( "root.sandbox = " + serverSandboxDir.getAbsolutePath() );
        System.out.println( "root.snapshots = " + serverSnapshotsDir.getAbsolutePath() );

        server.start();

        // Setup Client Side 

        HttpURL httpSandboxUrl = new HttpURL( "http://localhost:" + PORT + CONTEXT + "/sandbox/" );
        HttpURL httpSnapshotsUrl = new HttpURL( "http://localhost:" + PORT + CONTEXT + "/snapshots/" );

        try
        {
            davSandbox = new WebdavResource( httpSandboxUrl );
            davSnapshots = new WebdavResource( httpSnapshotsUrl );

            davSandbox.setDebug( 8 );
            davSnapshots.setDebug( 8 );

            davSandbox.setPath( CONTEXT + "/sandbox/" );
            davSnapshots.setPath( CONTEXT + "/snapshots/" );
        }
        catch ( IOException e )
        {
            tearDown();
            throw e;
        }
    }

    protected void tearDown()
        throws Exception
    {
        serverRootDir = null;

        if ( server != null )
        {
            try
            {
                server.stop();
            }
            catch ( Exception e )
            {
                /* ignore */
            }
            server = null;
        }

        if ( davSandbox != null )
        {
            try
            {
                davSandbox.close();
            }
            catch ( Exception e )
            {
                /* ignore */
            }

            davSandbox = null;
        }

        if ( davSnapshots != null )
        {
            try
            {
                davSnapshots.close();
            }
            catch ( Exception e )
            {
                /* ignore */
            }

            davSnapshots = null;
        }

        super.tearDown();
    }

    public void testResourceMoveCrossWebdav()
        throws Exception
    {
        // Create a few collections.
        assertDavMkDir( davSandbox, CONTEXT + "/sandbox/bar" );
        assertDavMkDir( davSnapshots, CONTEXT + "/snapshots/foo" );

        // Create a resource
        assertDavTouchFile( davSandbox, CONTEXT + "/sandbox/bar", "data.txt", "yo!" );

        // Move resource URL to URL (Across the WebDav Servlets)
        davSandbox.setPath( CONTEXT + "/sandbox/bar" );
        String source = CONTEXT + "/sandbox/bar/data.txt";
        String dest = "http://localhost:" + PORT + CONTEXT + "/snapshots/foo/data.txt";
        if ( !davSandbox.moveMethod( source, dest ) )
        {
            // TODO: remove when fully implemented. 
            if ( davSandbox.getStatusCode() == HttpStatus.SC_NOT_IMPLEMENTED )
            {
                // return quietly, as the server reported no support for this method.
                return;
            }

            fail( "Unable to move  <" + source + "> to <" + dest + "> on <" + davSandbox.getHttpURL().toString()
                + "> due to <" + davSandbox.getStatusMessage() + ">" );
        }

        assertDavFileNotExists( davSandbox, CONTEXT + "/sandbox/bar", "data.txt" );
        assertDavFileExists( davSnapshots, CONTEXT + "/snapshots/foo", "data.txt" );
    }

    public void testResourceDoesNotExist()
        throws Exception
    {
        // Create a few collections.
        assertDavMkDir( davSandbox, CONTEXT + "/sandbox/bar" );
        assertDavMkDir( davSnapshots, CONTEXT + "/snapshots/foo" );

        // Create a resource
        assertDavTouchFile( davSandbox, CONTEXT + "/sandbox/bar", "data.txt", "yo!" );

        // Get bad resources URLs
        String urlPrefix = "http://localhost:" + PORT + CONTEXT;
        assertGet404( urlPrefix + "/sandbox/a/resource/that/does/not/exist.html" );
        assertGet404( urlPrefix + "/" );
        assertGet404( urlPrefix + "/snapshots/foo/index.html" );
        assertGet404( urlPrefix + "/sandbox/bar.html" );
        assertGet404( urlPrefix + "/nonexistant/index.html" );
    }
}
