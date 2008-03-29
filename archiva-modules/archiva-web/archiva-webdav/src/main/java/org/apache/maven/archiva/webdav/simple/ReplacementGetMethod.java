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

import it.could.util.StreamTools;
import it.could.webdav.DAVException;
import it.could.webdav.DAVInputStream;
import it.could.webdav.DAVMethod;
import it.could.webdav.DAVNotModified;
import it.could.webdav.DAVResource;
import it.could.webdav.DAVTransaction;
import it.could.webdav.DAVUtilities;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.webdav.util.MimeTypes;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * ReplacementGetMethod 
 *
 * @author Pier Fumagalli (Original it.could.webdav 0.4 version)
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a> (Replacement Version)
 * @version $Id: ReplacementGetMethod.java 7002 2007-10-23 22:40:37Z joakime $
 * 
 * @plexus.component 
 *              role="it.could.webdav.DAVMethod"
 *              role-hint="get-with-indexing"
 */
public class ReplacementGetMethod implements DAVMethod
{
    /** <p>The encoding charset to repsesent collections.</p> */
    public static final String ENCODING = "UTF-8";

    /** <p>The mime type that {@link ReplacementGetMethod} will use serving index.html files.</p> */
    public static final String HTML_MIME_TYPE = "text/html";

    /** <p>The mime type that {@link ReplacementGetMethod} will use serving collections.</p> */
    public static final String COLLECTION_MIME_TYPE = HTML_MIME_TYPE + "; charset=\"" + ENCODING + "\"";

    /** <p>The header for content disposition.</p> */
    public static final String CONTENT_DISPOSITION = "Content-Disposition";

    /** <p>The content-disposition for fancy-indexing.</p> */
    public static final String INLINE_INDEX_HTML = "inline; filename=\"index.html\"";

    /**
     * @plexus.requirement
     */
    private MimeTypes mimeTypes;

    private boolean useIndexHtml = false;

    /**
     * <p>Create a new {@link ReplacementGetMethod} instance.</p>
     */
    public ReplacementGetMethod()
    {
        super();
    }

    /**
     * <p>Process the <code>GET</code> method.</p>
     */
    public void process( DAVTransaction transaction, DAVResource resource ) throws IOException
    {
        // Handle boilerplate
        if ( resource.isNull() )
            throw new DAVException( 404, "Not found", resource );

        notModified( transaction, resource );

        copyHeaders( transaction, resource );

        // Process the request.
        final String originalPath = transaction.getOriginalPath();
        final String normalizedPath = transaction.getNormalizedPath();
        final String current;
        final String parent;

        if ( originalPath.equals( normalizedPath ) )
        {
            final String relativePath = resource.getRelativePath();
            if ( relativePath.equals( "" ) )
            {
                current = transaction.lookup( resource ).toASCIIString();
            }
            else
            {
                current = relativePath;
            }
            parent = "./";
        }
        else
        {
            current = "./";
            parent = "../";
        }

        if ( resource.isCollection() )
        {
            DAVResource indexHtml = null;

            if ( useIndexHtml )
            {
                for ( Iterator it = resource.getChildren(); it.hasNext(); )
                {
                    DAVResource child = (DAVResource) it.next();
                    String name = child.getDisplayName().toLowerCase();
                    if ( StringUtils.equals( "index.html", name ) || StringUtils.equals( "index.htm", name ) )
                    {
                        indexHtml = child;
                        break;
                    }
                }
            }

            if ( useIndexHtml && indexHtml != null )
            {
                transaction.setContentType( COLLECTION_MIME_TYPE );
                transaction.setHeader( CONTENT_DISPOSITION, INLINE_INDEX_HTML );
                sendResource( transaction, indexHtml );
            }
            else
            {
                transaction.setContentType( COLLECTION_MIME_TYPE );
                transaction.setHeader( CONTENT_DISPOSITION, INLINE_INDEX_HTML );
                sendFancyIndex( transaction, resource, current, parent );
            }
        }
        else
        {
            /* Processing a normal resource request */
            transaction.setContentType( mimeTypes.getMimeType( resource.getDisplayName() ) );
            transaction.setHeader( CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getDisplayName() + "\"" );
            sendResource( transaction, resource );
        }
    }

    private void copyHeaders( DAVTransaction transaction, DAVResource resource )
    {
        /* Get the headers of this method */
        String ctyp = resource.getContentType();
        String etag = resource.getEntityTag();
        String lmod = DAVUtilities.formatHttpDate( resource.getLastModified() );
        String clen = DAVUtilities.formatNumber( resource.getContentLength() );

        /* Set the normal headers that are required for a GET */
        if ( ctyp != null )
        {
            transaction.setContentType( ctyp );
        }

        if ( etag != null )
        {
            transaction.setHeader( "ETag", etag );
        }

        if ( lmod != null )
        {
            transaction.setHeader( "Last-Modified", lmod );
        }

        if ( clen != null )
        {
            transaction.setHeader( "Content-Length", clen );
        }
    }

    private void sendResource( DAVTransaction transaction, DAVResource resource ) throws IOException
    {
        OutputStream out = null;
        DAVInputStream in = null;

        try
        {
            out = transaction.write();
            in = resource.read();
            
            byte buffer[] = new byte[4096 * 16];
            int k = -1;
            while ( ( k = in.read( buffer ) ) != -1 )
            {
                out.write( buffer, 0, k );
            }
            
            out.flush();
        }
        finally
        {
            StreamTools.close( in );
            StreamTools.close( out );
        }
    }

    private void sendFancyIndex( DAVTransaction transaction, DAVResource resource, final String current,
                                 final String parent ) throws IOException
    {
        PrintWriter out = transaction.write( ENCODING );
        String path = resource.getRelativePath();
        out.println( "<html>" );
        out.println( "<head>" );
        out.println( "<title>Collection: /" + path + "</title>" );
        out.println( "</head>" );
        out.println( "<body>" );
        out.println( "<h2>Collection: /" + path + "</h2>" );
        out.println( "<ul>" );

        /* Process the parent */
        final DAVResource parentResource = resource.getParent();
        if ( parentResource != null )
        {
            out.print( "<li><a href=\"" );
            out.print( parent );
            out.print( "\">" );
            out.print( parentResource.getDisplayName() );
            out.println( "</a> <i><small>(Parent)</small></i></li>" );
            out.println( "</ul>" );
            out.println( "<ul>" );
        }

        /* Process the children (in two sorted sets, for nice ordering) */
        Set resources = new TreeSet();
        Set collections = new TreeSet();
        Iterator iterator = resource.getChildren();
        while ( iterator.hasNext() )
        {
            final DAVResource child = (DAVResource) iterator.next();
            final StringBuffer buffer = new StringBuffer();
            final String childPath = child.getDisplayName();
            buffer.append( "<li><a href=\"" );
            buffer.append( current );
            buffer.append( childPath );
            buffer.append( "\">" );
            buffer.append( childPath );
            buffer.append( "</li>" );
            if ( child.isCollection() )
            {
                collections.add( buffer.toString() );
            }
            else
            {
                resources.add( buffer.toString() );
            }
        }

        /* Spit out the collections first and the resources then */
        for ( Iterator i = collections.iterator(); i.hasNext(); )
            out.println( i.next() );
        for ( Iterator i = resources.iterator(); i.hasNext(); )
            out.println( i.next() );

        out.println( "</ul>" );
        out.println( "</body>" );
        out.println( "</html>" );
        out.flush();
    }

    private void notModified( DAVTransaction transaction, DAVResource resource )
    {
        Date ifmod = transaction.getIfModifiedSince();
        Date lsmod = resource.getLastModified();
        if ( resource.isResource() && ( ifmod != null ) && ( lsmod != null ) )
        {
            /* HTTP doesn't send milliseconds, but Java does, so, reset them */
            lsmod = new Date( ( (long) ( lsmod.getTime() / 1000 ) ) * 1000 );
            if ( !ifmod.before( lsmod ) )
                throw new DAVNotModified( resource );
        }
    }

    public boolean isUseIndexHtml()
    {
        return useIndexHtml;
    }

    public void setUseIndexHtml( boolean useIndexHtml )
    {
        this.useIndexHtml = useIndexHtml;
    }
}
