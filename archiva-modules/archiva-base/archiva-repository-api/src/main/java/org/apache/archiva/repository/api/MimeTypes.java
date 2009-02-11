package org.apache.archiva.repository.api;

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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * MimeType sniffing for repository Artifacts
 */
public class MimeTypes
{
    private static final Logger log = Logger.getLogger(MimeTypes.class);

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private String resource = "/mime.types";
    
    private Map mimeMap = new HashMap();

    public MimeTypes()
    {
        load(resource);
    }

    /**
     * Get the Mime Type for the provided filename.
     * 
     * @param filename the filename to obtain the mime type for.
     * @return a mime type String, or null if filename is null, has no extension, or no mime type is associated with it.
     */
    public String getMimeType( String filename )
    {
        String value = null;
        if ( !StringUtils.isEmpty( filename ) )
        {
            int index = filename.lastIndexOf( '.' );

            if ( index >= 0 )
            {
                value = (String) mimeMap.get( filename.substring( index + 1 ).toLowerCase() );
            }
        }


        if (value == null)
        {
            value = DEFAULT_MIME_TYPE;
        }

        return value;

    }

    private void load( String resourceName )
    {
        ClassLoader cloader = this.getClass().getClassLoader();

        /* Load up the mime types table */
        URL mimeURL = cloader.getResource( resourceName );

        if ( mimeURL == null )
        {
            throw new IllegalStateException( "Unable to find resource " + resourceName );
        }

        InputStream mimeStream = null;

        try
        {
            mimeStream = mimeURL.openStream();
            load( mimeStream );
        }
        catch ( IOException e )
        {
            log.error( "Unable to load mime map " + resourceName + " : " + e.getMessage(), e );
        }
        finally
        {
            IOUtils.closeQuietly( mimeStream );
        }
    }

    private void load( InputStream mimeStream )
    {
        mimeMap.clear();

        InputStreamReader reader = null;
        BufferedReader buf = null;

        try
        {
            reader = new InputStreamReader( mimeStream );
            buf = new BufferedReader( reader );
            String line = null;

            while ( ( line = buf.readLine() ) != null )
            {
                line = line.trim();

                if ( line.length() == 0 )
                {
                    // empty line. skip it
                    continue;
                }

                if ( line.startsWith( "#" ) )
                {
                    // Comment. skip it
                    continue;
                }

                StringTokenizer tokenizer = new StringTokenizer( line );
                if ( tokenizer.countTokens() > 1 )
                {
                    String type = tokenizer.nextToken();
                    while ( tokenizer.hasMoreTokens() )
                    {
                        String extension = tokenizer.nextToken().toLowerCase();
                        this.mimeMap.put( extension, type );
                    }
                }
            }
        }
        catch ( IOException e )
        {
            log.error( "Unable to read mime types from input stream : " + e.getMessage(), e );
        }
        finally
        {
            IOUtils.closeQuietly( buf );
            IOUtils.closeQuietly( reader );
        }
    }
}
