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

import org.apache.commons.httpclient.HttpURL;
import org.apache.maven.archiva.webdav.servlet.basic.BasicWebDavServlet;
import org.apache.webdav.lib.WebdavResource;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.IOException;

public abstract class AbstractWebdavIndexHtmlTestCase
    extends AbstractWebdavProviderTestCase
{
    private File serverRepoDir;

    private WebdavResource davRepo;

    /** The Jetty Server. */
    private Server server;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        // Initialize server contents directory.

        serverRepoDir = getTestDir( "sandbox" );

        // Setup the Jetty Server.

        System.setProperty( "DEBUG", "" );
        System.setProperty( "org.mortbay.log.class", "org.slf4j.impl.SimpleLogger" );

        server = new Server( PORT );
        WebAppContext webAppConfig = new WebAppContext( server, getTestFile( "src/test/webapp" ).getCanonicalPath(), "/" );

        ServletHandler servletHandler = webAppConfig.getServletHandler();

        ServletHolder holder = servletHandler.addServletWithMapping( BasicWebDavServlet.class, CONTEXT + "/*" );

        holder.setInitParameter( BasicWebDavServlet.INIT_ROOT_DIRECTORY, serverRepoDir.getAbsolutePath() );
        holder.setInitParameter( BasicWebDavServlet.INIT_USE_INDEX_HTML, "true" );

        server.start();

        // Setup Client Side

        HttpURL httpSandboxUrl = new HttpURL( "http://localhost:" + PORT + CONTEXT + "/" );

        try
        {
            davRepo = new WebdavResource( httpSandboxUrl );

            davRepo.setDebug( 8 );

            davRepo.setPath( CONTEXT );
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
        serverRepoDir = null;

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

        if ( davRepo != null )
        {
            try
            {
                davRepo.close();
            }
            catch ( Exception e )
            {
                /* ignore */
            }

            davRepo = null;
        }

        super.tearDown();
    }

    public void testCollectionIndexHtml()
        throws Exception
    {
        // Lyrics: Colin Hay - Overkill
        String contents = "I cant get to sleep\n" + "I think about the implications\n" + "Of diving in too deep\n"
            + "And possibly the complications\n" + "Especially at night\n" + "I worry over situations\n"
            + "I know will be alright\n" + "Perahaps its just my imagination\n" + "Day after day it reappears\n"
            + "Night after night my heartbeat, shows the fear\n" + "Ghosts appear and fade away";

        // Create a few collections.
        assertDavMkDir( davRepo, CONTEXT + "/bar" );
        assertDavMkDir( davRepo, CONTEXT + "/foo" );

        // Create a resource
        assertDavTouchFile( davRepo, CONTEXT + "/bar", "index.html", contents );

        // Test for existance of resource
        assertDavFileExists( davRepo, CONTEXT + "/bar", "index.html" );
        assertDavFileNotExists( davRepo, CONTEXT + "/foo", "index.html" );

        // Copy resource
        String actual = davRepo.getMethodDataAsString( CONTEXT + "/bar/" );

        assertEquals( contents, actual );
    }
}
