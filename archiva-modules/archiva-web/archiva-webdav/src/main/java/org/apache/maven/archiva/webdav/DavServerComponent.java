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

package org.apache.maven.archiva.webdav;

import org.apache.maven.archiva.webdav.servlet.DavServerRequest;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * DavServerComponent 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: DavServerComponent.java 6000 2007-03-04 22:01:49Z joakime $
 */
public interface DavServerComponent
{
    /** The Plexus ROLE name */
    public static final String ROLE = DavServerComponent.class.getName();
    
    /**
     * Get the Prefix for this server component.
     * @return the prefix associated with this component.
     */
    public String getPrefix();

    /**
     * Set the prefix for this server component.
     * @param prefix the prefix to use.
     */
    public void setPrefix( String prefix );
    
    /**
     * <p>
     * Flag to indicate how the dav server component should treat a GET request against
     * a DAV Collection.
     * </p>
     * 
     * <p>
     * If true, the collection being requested will be searched for an index.html (or index.htm) 
     * file to serve back, before it defaults to displaying the collection (directory) contents.
     * </p>
     * 
     * <p>
     * If false, the collection will always be presented in as a list of contents.
     * </p>
     *   
     * @return true to use the index.html instead of directory contents.
     */
    public boolean isUseIndexHtml();

    /**
     * <p>
     * Flag to indicate how the dav server component should treat a GET request against
     * a DAV Collection.
     * </p>
     * 
     * <p>
     * If true, the collection being requested will be searched for an index.html (or index.htm) 
     * file to serve back, before it defaults to displaying the collection (directory) contents.
     * </p>
     * 
     * <p>
     * If false, the collection will always be presented in as a list of contents.
     * </p>
     *   
     * @param useIndexHtml true to use the index.html instead of directory contents.
     */
    public void setUseIndexHtml( boolean useIndexHtml );
    
    /**
     * Get the root directory for this server.
     * 
     * @return the root directory for this server.
     */
    public File getRootDirectory();

    /**
     * Set the root directory for this server's content.
     * 
     * @param rootDirectory the root directory for this server's content.
     */
    public void setRootDirectory( File rootDirectory );

    /**
     * Add a Server Listener for this server component.
     * 
     * @param listener the listener to add for this component.
     */
    public void addListener( DavServerListener listener );
    
    /**
     * Remove a server listener for this server component.
     * 
     * @param listener the listener to remove.
     */
    public void removeListener( DavServerListener listener );

    /**
     * Perform any initialization needed.
     * 
     * @param servletConfig the servlet config that might be needed.
     * @throws DavServerException if there was a problem initializing the server component.
     */
    public void init( ServletConfig servletConfig ) throws DavServerException;

    /**
     * Performs a simple filesystem check for the specified resource.
     * 
     * @param resource the resource to check for.
     * @return true if the resource exists.
     */
    public boolean hasResource( String resource );

    /**
     * Process incoming request.
     * 
     * @param request the incoming request to process.
     * @param response the outgoing response to provide.
     */
    public void process( DavServerRequest request, HttpServletResponse response )
        throws DavServerException, ServletException, IOException;
}
