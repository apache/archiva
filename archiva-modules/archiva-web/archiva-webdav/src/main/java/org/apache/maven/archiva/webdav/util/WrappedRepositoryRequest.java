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

package org.apache.maven.archiva.webdav.util;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * RepositoryRequest - wrapped servlet request to adjust the incoming request before the components get it.
 * It eliminates the prefix from the pathInfo portion of the URL requested.
 * And also allows for Header adjustment.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: WrappedRepositoryRequest.java 7001 2007-10-23 22:40:14Z joakime $
 */
public class WrappedRepositoryRequest
    extends HttpServletRequestWrapper
{
    private String pathInfo;

    private Map headers;

    /**
     * The Date Formats most commonly seen in Request Headers.
     */
    private SimpleDateFormat dateFormats[];

    public WrappedRepositoryRequest( HttpServletRequest request )
    {
        super( request );

        dateFormats = new SimpleDateFormat[] {
            new SimpleDateFormat( "EEE, dd MMM yyyy HH:mm:ss zzz" ),
            new SimpleDateFormat( "EEE, dd-MMM-yy HH:mm:ss" ),
            new SimpleDateFormat( "EEE MMM dd HH:mm:ss yyyy" ) };
        
        headers = new HashMap();

        Enumeration enHeaders = request.getHeaderNames();
        while ( enHeaders.hasMoreElements() )
        {
            String name = (String) enHeaders.nextElement();
            String value = request.getHeader( name );
            headers.put( name, value );
        }
    }

    public void setHeader( String name, String value )
    {
        headers.put( name, value );
    }

    public long getDateHeader( String name )
    {
        String value = (String) headers.get( name );
        if ( StringUtils.isEmpty( value ) )
        {
            // no value? return -1
            return -1;
        }

        // Try most common formats first.
        for ( int i = 0; i < dateFormats.length; i++ )
        {
            try
            {
                Date date = (Date) dateFormats[i].parseObject( value );
                return date.getTime();
            }
            catch ( java.lang.Exception e )
            {
                /* ignore exception */
            }
        }

        // Now check for the odd "GMT" formats (hey, it happens)
        if ( value.endsWith( " GMT" ) )
        {
            value = value.substring( 0, value.length() - 4 );

            for ( int i = 0; i < dateFormats.length; i++ )
            {
                try
                {
                    Date date = (Date) dateFormats[i].parseObject( value );
                    return date.getTime();
                }
                catch ( java.lang.Exception e )
                {
                    /* ignore exception */
                }
            }
        }

        // unrecognized format? return -1
        return -1;
    }

    public String getHeader( String name )
    {
        return (String) headers.get( name );
    }

    public Enumeration getHeaderNames()
    {
        return new Enumeration()
        {
            private Iterator iter = headers.keySet().iterator();

            public boolean hasMoreElements()
            {
                return iter.hasNext();
            }

            public Object nextElement()
            {
                return iter.next();
            }
        };
    }

    public int getIntHeader( String name )
    {
        String value = getHeader( name );
        try
        {
            return Integer.parseInt( value );
        }
        catch ( NumberFormatException e )
        {
            return -1;
        }
    }

    public void setPathInfo( String alternatePathInfo )
    {
        this.pathInfo = alternatePathInfo;
    }

    public String getPathInfo()
    {
        if ( this.pathInfo != null )
        {
            return this.pathInfo;
        }

        return super.getPathInfo();
    }

    public String getServletPath()
    {
        if ( this.pathInfo != null )
        {
            return super.getServletPath() + "/" + this.pathInfo;
        }

        return super.getServletPath();
    }
}
