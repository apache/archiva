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

import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * LatinEntityResolutionReaderTest
 */
public class LatinEntityResolutionReaderTest
    extends AbstractArchivaXmlTestCase
{
    /**
     * A method to obtain the content of a reader as a String,
     * while allowing for specifing the buffer size of the operation.
     * <p/>
     * This method is only really useful for testing a Reader implementation.
     *
     * @param input   the reader to get the input from.
     * @param bufsize the buffer size to use.
     * @return the contents of the reader as a String.
     * @throws IOException if there was an I/O error.
     */
    private String toStringFromReader( Reader input, int bufsize )
        throws IOException
    {
        StringWriter output = new StringWriter();

        final char[] buffer = new char[bufsize];
        int n = 0;
        while ( -1 != ( n = input.read( buffer ) ) )
        {
            output.write( buffer, 0, n );
        }
        output.flush();

        return output.toString();
    }

    /**
     * This reads a text file from the src/test/examples directory,
     * normalizes the end of lines, and returns the contents as a big String.
     *
     * @param examplePath the name of the file in the src/test/examples directory.
     * @return the contents of the provided file
     * @throws IOException if there was an I/O error.
     */
    private String toStringFromExample( String examplePath )
        throws IOException
    {
        File exampleFile = getExampleXml( examplePath );
        FileReader fileReader = new FileReader( exampleFile );
        BufferedReader lineReader = new BufferedReader( fileReader );
        StringBuilder sb = new StringBuilder();

        boolean hasContent = false;

        String line = lineReader.readLine();
        while ( line != null )
        {
            if ( hasContent )
            {
                sb.append( "\n" );
            }
            sb.append( line );
            hasContent = true;
            line = lineReader.readLine();
        }

        return sb.toString();
    }

    public void assertProperRead( String sourcePath, String expectedPath, int bufsize )
    {
        try
        {
            File inputFile = getExampleXml( sourcePath );

            FileReader fileReader = new FileReader( inputFile );
            LatinEntityResolutionReader testReader = new LatinEntityResolutionReader( fileReader );

            String actualOutput = toStringFromReader( testReader, bufsize );
            String expectedOutput = toStringFromExample( expectedPath );

            assertEquals( expectedOutput, actualOutput );
        }
        catch ( IOException e )
        {
            fail( "IOException: " + e.getMessage() );
        }
    }

    private void assertProperRead( StringBuilder expected, String sourcePath, int bufSize )
    {
        try
        {
            File inputFile = getExampleXml( sourcePath );

            FileReader fileReader = new FileReader( inputFile );
            LatinEntityResolutionReader testReader = new LatinEntityResolutionReader( fileReader );

            String actualOutput = toStringFromReader( testReader, bufSize );

            assertEquals( "Proper Read: ", expected.toString(), actualOutput );
        }
        catch ( IOException e )
        {
            fail( "IOException: " + e.getMessage() );
        }
    }

    @Test
    public void testReaderNormalBufsize()
        throws IOException
    {
        StringBuilder expected = new StringBuilder();

        expected.append( "<basic>\n" );
        expected.append( "  <names>\n" );
        expected.append( "    <name>" ).append( TRYGVIS ).append( "</name>\n" );
        expected.append( "    <name>" ).append( INFINITE_ARCHIVA ).append( "</name>\n" );
        expected.append( "  </names>\n" );
        expected.append( "</basic>" );

        assertProperRead( expected, "no-prolog-with-entities.xml", 4096 );
    }

    @Test
    public void testReaderSmallBufsize()
        throws IOException
    {
        StringBuilder expected = new StringBuilder();

        expected.append( "<basic>\n" );
        expected.append( "  <names>\n" );
        expected.append( "    <name>" ).append( TRYGVIS ).append( "</name>\n" );
        expected.append( "    <name>" ).append( INFINITE_ARCHIVA ).append( "</name>\n" );
        expected.append( "  </names>\n" );
        expected.append( "</basic>" );

        assertProperRead( expected, "no-prolog-with-entities.xml", 1024 );
    }

    @Test
    public void testReaderRediculouslyTinyBufsize()
        throws IOException
    {
        StringBuilder expected = new StringBuilder();

        expected.append( "<basic>\n" );
        expected.append( "  <names>\n" );
        expected.append( "    <name>" ).append( TRYGVIS ).append( "</name>\n" );
        expected.append( "    <name>" ).append( INFINITE_ARCHIVA ).append( "</name>\n" );
        expected.append( "  </names>\n" );
        expected.append( "</basic>" );

        assertProperRead( expected, "no-prolog-with-entities.xml", 32 );
    }

    @Test
    public void testReaderHugeBufsize()
        throws IOException
    {
        StringBuilder expected = new StringBuilder();

        expected.append( "<basic>\n" );
        expected.append( "  <names>\n" );
        expected.append( "    <name>" ).append( TRYGVIS ).append( "</name>\n" );
        expected.append( "    <name>" ).append( INFINITE_ARCHIVA ).append( "</name>\n" );
        expected.append( "  </names>\n" );
        expected.append( "</basic>" );

        assertProperRead( expected, "no-prolog-with-entities.xml", 409600 );
    }

    @Test
    public void testReaderLeftOver()
        throws IOException
    {
        File inputFile = getExampleXml( "maven-metadata-leftover.xml" );
        //Bits from RepositoryMetadataReader.read
        InputStream in = null;
        SAXReader reader = new SAXReader();
        URL url = inputFile.toURL();
        in = url.openStream();
        InputStreamReader inReader = new InputStreamReader( in, Charset.forName( "UTF-8" ) );
        LatinEntityResolutionReader latinReader = new LatinEntityResolutionReader( inReader );
        try
        {
            reader.read( latinReader );
        }
        catch ( DocumentException e )
        {
            Assert.fail( "Should not have failed here." + e );
            IOException ioe = new IOException();
            ioe.initCause( e );
            throw ioe;
        }
    }

    @Test
    public void testNoLatinEntitiesHugeLine()
    {
        assertProperRead( "commons-codec-1.2.pom", "commons-codec-1.2.pom", 4096 );
    }
}
