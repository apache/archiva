package org.apache.maven.repository.applet;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import java.applet.Applet;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.util.Properties;

/**
 * TODO: Description.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class ChecksumApplet
    extends Applet
{
    private static final int CHECKSUM_BUFFER_SIZE = 8192;

    private static final int BYTE_MASK = 0xFF;

    public void init()
    {
        super.init();
    }

    public String generateMd5( final String file )
        throws IOException, NoSuchAlgorithmException
    {
        return (String) AccessController.doPrivileged( new PrivilegedAction()
        {
            public Object run()
            {
                try
                {
                    MessageDigest digest = MessageDigest.getInstance( "MD5" );

                    InputStream fis = new FileInputStream( file );
                    try
                    {
                        byte[] buffer = new byte[CHECKSUM_BUFFER_SIZE];
                        int numRead;
                        do
                        {
                            numRead = fis.read( buffer );
                            if ( numRead > 0 )
                            {
                                digest.update( buffer, 0, numRead );
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
                catch ( NoSuchAlgorithmException e )
                {
                    throw new RuntimeException( e );
                }
                catch ( IOException e )
                {
                    throw new RuntimeException( e );
                }
            }
        } );
    }

    private static String byteArrayToHexStr( byte[] data )
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