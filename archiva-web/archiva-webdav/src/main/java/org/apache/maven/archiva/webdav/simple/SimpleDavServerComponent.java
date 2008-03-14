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

package org.apache.maven.archiva.webdav.simple;

import it.could.webdav.DAVListener;
import it.could.webdav.DAVProcessor;
import it.could.webdav.DAVRepository;
import it.could.webdav.DAVResource;
import it.could.webdav.DAVTransaction;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.webdav.AbstractDavServerComponent;
import org.apache.maven.archiva.webdav.DavServerException;
import org.apache.maven.archiva.webdav.servlet.DavServerRequest;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * SimpleDavServerComponent 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: SimpleDavServerComponent.java 7097 2007-11-30 12:57:29Z handyande $
 * 
 * @plexus.component role="org.apache.maven.archiva.webdav.DavServerComponent"
 *                   role-hint="simple" 
 *                   instantiation-strategy="per-lookup"
 */
public class SimpleDavServerComponent
    extends AbstractDavServerComponent
    implements DAVListener
{
    /**
     * @plexus.requirement 
     *              role="it.could.webdav.DAVMethod"
     *              role-hint="get-with-indexing"
     */
    public ReplacementGetMethod methodGet;
    
    private String prefix;

    private File rootDirectory;

    private DAVRepository davRepository;

    private DAVProcessor davProcessor;

    public String getPrefix()
    {
        return prefix;
    }

    public File getRootDirectory()
    {
        return rootDirectory;
    }

    public void setPrefix( String prefix )
    {
        this.prefix = prefix;
    }

    public void setRootDirectory( File rootDirectory )
    {
        this.rootDirectory = rootDirectory;
    }

    public void init( ServletConfig servletConfig )
        throws DavServerException
    {
        servletConfig.getServletContext().log( "Initializing " + this.getClass().getName() );
        try
        {
            davRepository = new DAVRepository( rootDirectory );
            davProcessor = new DAVProcessor( davRepository );
            davRepository.addListener( this );

            hackDavProcessor( davProcessor );
        }
        catch ( IOException e )
        {
            throw new DavServerException( "Unable to initialize DAVRepository.", e );
        }
    }

    /**
     * Replace the problematic dav methods with local hacked versions.
     * 
     * @param davProcessor
     * @throws DavServerException 
     */
    private void hackDavProcessor( DAVProcessor davProcessor )
        throws DavServerException
    {
        davProcessor.setMethod( "MOVE", new HackedMoveMethod() );
        davProcessor.setMethod( "GET", methodGet );
        
        /* Reflection based technique.
        try
        {
            Field fldInstance = davProcessor.getClass().getDeclaredField( "INSTANCES" );
            fldInstance.setAccessible( true );

            Map mapInstances = (Map) fldInstance.get( davProcessor );

            // Replace MOVE method.
            // TODO: Remove MOVE method when upgrading it.could.webdav to v0.5
            mapInstances.put( "MOVE", (DAVMethod) new HackedMoveMethod() );

            // Replace GET method.
            mapInstances.put( "GET", (DAVMethod) methodGet );
        }
        catch ( Throwable e )
        {
            throw new DavServerException( "Unable to twiddle DAVProcessor.INSTANCES field.", e );
        }
        */
    }

    public void process( DavServerRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        DAVTransaction transaction = new DAVTransaction( request.getRequest(), response );

        /* BEGIN - it.could.webdav hacks
         * TODO: Remove hacks with release of it.could.webdav 0.5 (or newer)
         */
        String depthValue = request.getRequest().getHeader( "Depth" );
        if ( StringUtils.equalsIgnoreCase( "infinity", depthValue ) )
        {
            // See - http://could.it/bugs/browse/DAV-3
            request.getRequest().setHeader( "Depth", "infinity" );
        }
        /* END - it.could.webdav hacks */

        davProcessor.process( transaction );
    }

    public void notify( DAVResource resource, int event )
    {
        switch ( event )
        {
            case DAVListener.COLLECTION_CREATED:
                triggerCollectionCreated( resource.getRelativePath() );
                break;
            case DAVListener.COLLECTION_REMOVED:
                triggerCollectionRemoved( resource.getRelativePath() );
                break;
            case DAVListener.RESOURCE_CREATED:
                triggerResourceCreated( resource.getRelativePath() );
                break;
            case DAVListener.RESOURCE_REMOVED:
                triggerResourceRemoved( resource.getRelativePath() );
                break;
            case DAVListener.RESOURCE_MODIFIED:
                triggerResourceModified( resource.getRelativePath() );
                break;
        }
    }
    
    public void setUseIndexHtml( boolean useIndexHtml )
    {
        super.setUseIndexHtml( useIndexHtml );
        this.methodGet.setUseIndexHtml( useIndexHtml );
    }
}
