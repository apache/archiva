package org.apache.maven.archiva.applet;

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

import javax.swing.*;
import java.applet.Applet;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;

/**
 * Applet that takes a file on the local filesystem and checksums it for sending to the server.
 *
 */
public class ChecksumApplet
    extends Applet
{
    private static final int CHECKSUM_BUFFER_SIZE = 8192;

    private static final int BYTE_MASK = 0xFF;

    private JProgressBar progressBar;

    public void init()
    {
        setLayout( new BorderLayout() );
        progressBar = new JProgressBar();
        progressBar.setStringPainted( true );
        add( progressBar, BorderLayout.CENTER );
        JLabel label = new JLabel( "Checksum progress: " );
        add( label, BorderLayout.WEST );
    }

    public String generateMd5( final String file )
        throws IOException, NoSuchAlgorithmException
    {
        Object o = AccessController.doPrivileged( new PrivilegedAction()
        {
            public Object run()
            {
                try
                {
                    return checksumFile( file );
                }
                catch ( NoSuchAlgorithmException e )
                {
                    return "Error checksumming file: " + e.getMessage();
                }
                catch ( FileNotFoundException e )
                {
                    return "Couldn't find the file. " + e.getMessage();
                }
                catch ( IOException e )
                {
                    return "Error reading file: " + e.getMessage();
                }
            }
        } );
        return (String) o;
    }

    protected String checksumFile( String file )
        throws NoSuchAlgorithmException, IOException
    {
        MessageDigest digest = MessageDigest.getInstance( "MD5" );

        long total = new File( file ).length();
        InputStream fis = new FileInputStream( file );
        try
        {
            long totalRead = 0;
            byte[] buffer = new byte[CHECKSUM_BUFFER_SIZE];
            int numRead;
            do
            {
                numRead = fis.read( buffer );
                if ( numRead > 0 )
                {
                    digest.update( buffer, 0, numRead );
                    totalRead += numRead;
                    progressBar.setValue( (int) ( totalRead * progressBar.getMaximum() / total ) );
                }
            }
            while ( numRead != -1 );
        }
        finally
        {
            fis.close();
        }

        return byteArrayToHexStr( digest.digest() );
    }

    protected static String byteArrayToHexStr( byte[] data )
    {
        String output = "";

        for ( int cnt = 0; cnt < data.length; cnt++ )
        {
            //Deposit a byte into the 8 lsb of an int.
            int tempInt = data[cnt] & BYTE_MASK;

            //Get hex representation of the int as a string.
            String tempStr = Integer.toHexString( tempInt );

            //Append a leading 0 if necessary so that each hex string will contain 2 characters.
            if ( tempStr.length() == 1 )
            {
                tempStr = "0" + tempStr;
            }

            //Concatenate the two characters to the output string.
            output = output + tempStr;
        }

        return output.toUpperCase();
    }
}
