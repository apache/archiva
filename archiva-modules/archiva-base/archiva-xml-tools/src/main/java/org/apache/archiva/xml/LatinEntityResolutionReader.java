package org.apache.archiva.xml;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LatinEntityResolutionReader - Read a Character Stream.
 *
 * @version $Id$
 */
public class LatinEntityResolutionReader
    extends Reader
{
    private BufferedReader originalReader;

    private char leftover[];

    private Pattern entityPattern;

    public LatinEntityResolutionReader( Reader reader )
    {
        this.originalReader = new BufferedReader( reader );
        this.entityPattern = Pattern.compile( "\\&[a-zA-Z]+\\;" );
    }

    /**
     * Read characters into a portion of an array. This method will block until some input is available, 
     * an I/O error occurs, or the end of the stream is reached.
     * 
     * @param destbuf Destination buffer
     * @param offset Offset (in destination buffer) at which to start storing characters
     * @param length Maximum number of characters to read
     * @return The number of characters read, or -1 if the end of the stream has been reached
     * @throws IOException if an I/O error occurs.
     */
    public int read( char[] destbuf, int offset, int length )
        throws IOException
    {
        int tmp_length;
        int current_requested_offset = offset;
        int current_requested_length = length;

        // Drain leftover from last read request.
        if ( leftover != null )
        {
            if ( leftover.length > length )
            {
                // Copy partial leftover.
                System.arraycopy( leftover, 0, destbuf, current_requested_offset, length );

                // Create new leftover of remaining.
                char tmp[] = new char[length];
                System.arraycopy( leftover, length, tmp, 0, length );
                leftover = new char[tmp.length];
                System.arraycopy( tmp, 0, leftover, 0, length );

                // Return len
                return length;
            }
            else
            {
                tmp_length = leftover.length;

                // Copy full leftover
                System.arraycopy( leftover, 0, destbuf, current_requested_offset, tmp_length );

                // Empty out leftover (as there is now none left)
                leftover = null;

                // Adjust offset and lengths.
                current_requested_offset += tmp_length;
                current_requested_length -= tmp_length;
            }
        }

        StringBuffer sbuf = getExpandedBuffer( current_requested_length );

        // Have we reached the end of the buffer?
        if ( sbuf == null )
        {
            // Do we have content?
            if ( current_requested_offset > offset )
            {
                // Signal that we do, by calculating length.
                return ( current_requested_offset - offset );
            }

            // No content. signal end of buffer.
            return -1;
        }

        // Copy from expanded buf whatever length we can accomodate.
        tmp_length = Math.min( sbuf.length(), current_requested_length );
        sbuf.getChars( 0, tmp_length, destbuf, current_requested_offset );

        // Create the leftover (if any)
        if ( tmp_length < sbuf.length() )
        {
            leftover = new char[sbuf.length() - tmp_length];
            sbuf.getChars( tmp_length, tmp_length + leftover.length, leftover, 0 );
        }

        // Calculate Actual Length and return.
        return ( current_requested_offset - offset ) + tmp_length;
    }

    private StringBuffer getExpandedBuffer( int minimum_length )
        throws IOException
    {
        StringBuffer buf = null;
        String line = this.originalReader.readLine();
        boolean done = ( line == null );

        while ( !done )
        {
            if ( buf == null )
            {
                buf = new StringBuffer();
            }

            buf.append( expandLine( line ) );

            // Add newline only if there is more data.
            if ( this.originalReader.ready() )
            {
                buf.append( "\n" );
            }

            if ( buf.length() > minimum_length )
            {
                done = true;
            }
            else
            {
                line = this.originalReader.readLine();
                done = ( line == null );
            }
        }

        return buf;
    }

    private String expandLine( String line )
    {
        StringBuffer ret = new StringBuffer();

        int offset = 0;
        String entity;
        Matcher mat = this.entityPattern.matcher( line );
        while ( mat.find( offset ) )
        {
            ret.append( line.substring( offset, mat.start() ) );
            entity = mat.group();
            ret.append( LatinEntities.resolveEntity( entity ) );
            offset = mat.start() + entity.length();
        }
        ret.append( line.substring( offset ) );

        return ret.toString();
    }

    public void close()
        throws IOException
    {
        this.originalReader.close();
    }
}
