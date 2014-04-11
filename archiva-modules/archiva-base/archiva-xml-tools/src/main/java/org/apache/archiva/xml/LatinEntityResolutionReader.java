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
 *
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
    @Override
    public int read( char[] destbuf, int offset, int length )
        throws IOException
    {
        int tmpLength;
        int currentRequestedOffset = offset;
        int currentRequestedLength = length;

        // Drain leftover from last read request.
        if ( leftover != null )
        {
            if ( leftover.length > length )
            {
                // Copy partial leftover.
                System.arraycopy( leftover, 0, destbuf, currentRequestedOffset, length );
                int copyLeftOverLength = leftover.length - length;

                // Create new leftover of remaining.
                char tmp[] = new char[copyLeftOverLength];
                System.arraycopy( leftover, length, tmp, 0, copyLeftOverLength );
                leftover = new char[tmp.length];
                System.arraycopy( tmp, 0, leftover, 0, copyLeftOverLength );

                // Return len
                return length;
            }
            else
            {
                tmpLength = leftover.length;

                // Copy full leftover
                System.arraycopy( leftover, 0, destbuf, currentRequestedOffset, tmpLength );

                // Empty out leftover (as there is now none left)
                leftover = null;

                // Adjust offset and lengths.
                currentRequestedOffset += tmpLength;
                currentRequestedLength -= tmpLength;
            }
        }

        StringBuilder sbuf = getExpandedBuffer( currentRequestedLength );

        // Have we reached the end of the buffer?
        if ( sbuf == null )
        {
            // Do we have content?
            if ( currentRequestedOffset > offset )
            {
                // Signal that we do, by calculating length.
                return ( currentRequestedOffset - offset );
            }

            // No content. signal end of buffer.
            return -1;
        }

        // Copy from expanded buf whatever length we can accomodate.
        tmpLength = Math.min( sbuf.length(), currentRequestedLength );
        sbuf.getChars( 0, tmpLength, destbuf, currentRequestedOffset );

        // Create the leftover (if any)
        if ( tmpLength < sbuf.length() )
        {
            leftover = new char[sbuf.length() - tmpLength];
            sbuf.getChars( tmpLength, tmpLength + leftover.length, leftover, 0 );
        }

        // Calculate Actual Length and return.
        return ( currentRequestedOffset - offset ) + tmpLength;
    }

    private StringBuilder getExpandedBuffer( int minimumLength )
        throws IOException
    {
        StringBuilder buf = null;
        String line = this.originalReader.readLine();
        boolean done = ( line == null );

        while ( !done )
        {
            if ( buf == null )
            {
                buf = new StringBuilder();
            }

            buf.append( expandLine( line ) );

            // Add newline only if there is more data.
            if ( this.originalReader.ready() )
            {
                buf.append( "\n" );
            }

            if ( buf.length() > minimumLength )
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
        StringBuilder ret = new StringBuilder();

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

    @Override
    public void close()
        throws IOException
    {
        this.originalReader.close();
    }
}
