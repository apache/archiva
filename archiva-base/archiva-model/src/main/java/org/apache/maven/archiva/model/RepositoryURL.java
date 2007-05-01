package org.apache.maven.archiva.model;

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

/**
 * RepositoryURL - Mutable (and protocol forgiving) URL object.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryURL
{
    private String url;

    private String protocol;

    private String host;

    private String port;

    private String username;

    private String password;

    private String path;

    public RepositoryURL( String url )
    {
        this.url = url;

        // .\ Parse the URL \.____________________________________________

        int pos;

        pos = url.indexOf( ":/" );
        if ( pos == ( -1 ) )
        {
            throw new IllegalArgumentException( "Invalid URL, unable to parse protocol:// from " + url );
        }

        protocol = url.substring( 0, pos );

        // Determine the post protocol position.
        int postProtocolPos = protocol.length() + 1;
        while ( url.charAt( postProtocolPos ) == '/' )
        {
            postProtocolPos++;
        }
        
        // Handle special case with file protocol (which has no host, port, username, or password)
        if ( "file".equals( protocol ) )
        {
            path = "/" + url.substring( postProtocolPos );

            return;
        }

        // attempt to find the start of the 'path'
        pos = url.indexOf( "/", postProtocolPos );

        // no path specified - ex "http://localhost"
        if ( pos == ( -1 ) )
        {
            // set pos to end of string. (needed for 'middle section')
            pos = url.length();
            // default path
            path = "/";
        }
        else
        {
            // get actual path.
            path = url.substring( pos );
        }

        // get the middle section ( username : password @ hostname : port )
        String middle = url.substring( postProtocolPos, pos );

        pos = middle.indexOf( '@' );

        // we have an authentication section.
        if ( pos > 0 )
        {
            String authentication = middle.substring( 0, pos );
            middle = middle.substring( pos + 1 ); // lop off authentication for host:port search

            pos = authentication.indexOf( ':' );

            // we have a password.
            if ( pos > 0 )
            {
                username = authentication.substring( 0, pos );
                password = authentication.substring( pos + 1 );
            }
            else
            {
                username = authentication;
            }
        }

        pos = middle.indexOf( ':' );

        // we have a defined port
        if ( pos > 0 )
        {
            host = middle.substring( 0, pos );
            port = middle.substring( pos + 1 );
        }
        else
        {
            host = middle;
        }
    }

    public String toString()
    {
        return url;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public String getHost()
    {
        return host;
    }

    public String getPath()
    {
        return path;
    }

    public String getPort()
    {
        return port;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public String getUrl()
    {
        return url;
    }
}
