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
import org.codehaus.plexus.util.IOUtil;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * AbstractBasicWebdavProviderTestCase 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: AbstractBasicWebdavProviderTestCase.java 6000 2007-03-04 22:01:49Z joakime $
 */
public abstract class AbstractBasicWebdavProviderTestCase
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

        holder.setInitParameter( "dav.root", serverRepoDir.getAbsolutePath() );
        
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

    // --------------------------------------------------------------------
    // Actual Test Cases.
    // --------------------------------------------------------------------

    public void testPutGet()
        throws Exception
    {
        // Quote: Rocky
        String contents = "yo!\n";

        assertDavTouchFile( davRepo, CONTEXT, "data.txt", contents );

        InputStream inputStream = davRepo.getMethodData( CONTEXT + "/data.txt" );

        assertEquals( contents, IOUtil.toString( inputStream ) );
    }

    public void testCollectionTasks()
        throws Exception
    {
        // Create a few collections.
        assertDavMkDir( davRepo, CONTEXT + "/bar" );
        assertDavMkDir( davRepo, CONTEXT + "/bar/foo" );

        // Remove a collection
        davRepo.setPath( CONTEXT );
        if ( !davRepo.deleteMethod( CONTEXT + "/bar/foo" ) )
        {
            fail( "Unable to remove <" + CONTEXT + "/bar/foo> on <" + davRepo.getHttpURL().toString() + "> due to <"
                + davRepo.getStatusMessage() + ">" );
        }

        assertDavDirNotExists( davRepo, CONTEXT + "/bar/foo" );
    }

    public void testResourceCopy()
        throws Exception
    {
        // Lyrics: Cool and the Gang - Celebrate Good Times
        String contents = "we're gonna have a good time tonite. lets celebrate. it's a celebration. "
            + "cel-e-brate good times, come on!";

        // Create a few collections.
        assertDavMkDir( davRepo, CONTEXT + "/bar" );
        assertDavMkDir( davRepo, CONTEXT + "/foo" );

        // Create a resource
        assertDavTouchFile( davRepo, CONTEXT + "/bar", "data.txt", contents );

        // Test for existance of resource
        assertDavFileExists( davRepo, CONTEXT + "/bar", "data.txt" );
        assertDavFileNotExists( davRepo, CONTEXT + "/foo", "data.txt" );

        // Copy resource
        String source = CONTEXT + "/bar/data.txt";
        String dest = CONTEXT + "/foo/data.txt";
        if ( !davRepo.copyMethod( source, dest ) )
        {
            fail( "Unable to copy  <" + source + "> to <" + dest + "> on <" + davRepo.getHttpURL().toString()
                + "> due to <" + davRepo.getStatusMessage() + ">" );
        }

        // Test for existance of resource
        assertDavFileExists( davRepo, CONTEXT + "/bar", "data.txt" );
        assertDavFileExists( davRepo, CONTEXT + "/foo", "data.txt" );
    }

    public void testResourceMove()
        throws Exception
    {
        // Lyrics: Men At Work - Who Can It Be Now
        String contents = "Who can it be knocking at my door?\n" + "Make no sound, tip-toe across the floor.\n"
            + "If he hears, he'll knock all day,\n" + "I'll be trapped, and here I'll have to stay.\n"
            + "I've done no harm, I keep to myself;\n" + "There's nothing wrong with my state of mental health.\n"
            + "I like it here with my childhood friend;\n" + "Here they come, those feelings again!\n";

        // Create a few collections.
        assertDavMkDir( davRepo, CONTEXT + "/bar" );
        assertDavMkDir( davRepo, CONTEXT + "/foo" );

        // Create a resource
        assertDavTouchFile( davRepo, CONTEXT + "/bar", "data.txt", contents );

        // Test for existance of resource
        assertDavFileExists( davRepo, CONTEXT + "/bar", "data.txt" );
        assertDavFileNotExists( davRepo, CONTEXT + "/foo", "data.txt" );

        // Copy resource
        String source = CONTEXT + "/bar/data.txt";
        String dest = CONTEXT + "/foo/data.txt";
        if ( !davRepo.moveMethod( source, dest ) )
        {
            fail( "Unable to move  <" + source + "> to <" + dest + "> on <" + davRepo.getHttpURL().toString()
                + "> due to <" + davRepo.getStatusMessage() + ">" );
        }

        // Test for existance of resource
        assertDavFileNotExists( davRepo, CONTEXT + "/bar", "data.txt" );
        assertDavFileExists( davRepo, CONTEXT + "/foo", "data.txt" );
    }

    public void testResourceDelete()
        throws Exception
    {
        // Lyrics: Men At Work - Down Under
        String contents = "Lying in a den in Bombay\n" + "With a slack jaw, and not much to say\n"
            + "I said to the man, \"Are you trying to tempt me\"\n" + "Because I come from the land of plenty?\n";

        // Create a few collections.
        assertDavMkDir( davRepo, CONTEXT + "/bar" );

        // Create a resource
        assertDavTouchFile( davRepo, CONTEXT + "/bar", "data.txt", contents );

        // Move resource
        davRepo.setPath( CONTEXT );
        if ( !davRepo.deleteMethod( CONTEXT + "/bar/data.txt" ) )
        {
            fail( "Unable to remove <" + CONTEXT + "/bar/data.txt> on <" + davRepo.getHttpURL().toString()
                + "> due to <" + davRepo.getStatusMessage() + ">" );
        }

        // Test for existance via webdav interface.
        assertDavFileNotExists( davRepo, CONTEXT + "/bar", "data.txt" );
    }
}
