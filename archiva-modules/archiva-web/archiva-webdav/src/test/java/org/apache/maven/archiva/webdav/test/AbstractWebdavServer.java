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

import org.apache.maven.archiva.webdav.DavServerManager;
import org.apache.maven.archiva.webdav.servlet.basic.BasicWebDavServlet;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.spring.PlexusContainerAdapter;
import org.codehaus.plexus.util.FileUtils;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * AbstractWebdavServer - Baseline server for starting up a BasicWebDavServlet to allow experimentation with.  
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: AbstractWebdavServer.java 5407 2007-01-12 19:41:09Z joakime $
 */
public abstract class AbstractWebdavServer
{
    public static final int PORT = 14541;

    protected PlexusContainer container;

    protected String basedir;

    /** the jetty server */
    protected Server server;

    private DavServerManager manager;

    public String getBasedir()
    {
        if ( basedir != null )
        {
            return basedir;
        }

        basedir = System.getProperty( "basedir" );
        if ( basedir == null )
        {
            basedir = new File( "" ).getAbsolutePath();
        }

        return basedir;
    }

    public File getTestFile( String path )
    {
        return new File( getBasedir(), path );
    }

    protected abstract String getProviderHint();

    public void startServer()
        throws Exception
    {
        container = createContainerInstance();
        
        // ----------------------------------------------------------------------------
        // Create the DavServerManager
        // ----------------------------------------------------------------------------

        manager = (DavServerManager) container.lookup( DavServerManager.ROLE, getProviderHint() );

        // ----------------------------------------------------------------------------
        // Create the jetty server
        // ----------------------------------------------------------------------------

        System.setProperty( "DEBUG", "" );
        System.setProperty( "org.mortbay.log.class", "org.slf4j.impl.SimpleLogger" );

        server = new Server( PORT );
        Context root = new Context( server, "/", Context.SESSIONS );
        ServletHandler servletHandler = root.getServletHandler();
        root.setContextPath( "/" );
        root.setAttribute( PlexusConstants.PLEXUS_KEY, container );
        
        // ----------------------------------------------------------------------------
        // Configure the webdav servlet
        // ----------------------------------------------------------------------------

        ServletHolder holder = servletHandler.addServletWithMapping( BasicWebDavServlet.class, "/projects/*" );

        // Initialize server contents directory.
        File serverContentsDir = new File( "target/test-server/" );

        FileUtils.deleteDirectory( serverContentsDir );
        if ( serverContentsDir.exists() )
        {
            throw new IllegalStateException( "Unable to execute test, server contents test directory ["
                + serverContentsDir.getAbsolutePath() + "] exists, and cannot be deleted by the test case." );
        }

        if ( !serverContentsDir.mkdirs() )
        {
            throw new IllegalStateException( "Unable to execute test, server contents test directory ["
                + serverContentsDir.getAbsolutePath() + "] cannot be created." );
        }

        holder.setInitParameter( "dav.root", serverContentsDir.getAbsolutePath() );

        // ----------------------------------------------------------------------------
        // Start the jetty server
        // ----------------------------------------------------------------------------

        server.start();
    }

    protected PlexusContainer createContainerInstance()
        throws PlexusContainerException
    {
        return new PlexusContainerAdapter();
    }

    public void stopServer()
    {
        if ( server != null )
        {
            try
            {
                server.stop();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }

        if ( container != null )
        {
            container.dispose();
        }
    }
}
