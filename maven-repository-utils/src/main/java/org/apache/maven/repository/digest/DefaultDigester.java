package org.apache.maven.repository.digest;

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
 */

import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Create a digest for a file.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.repository.digest.Digester"
 */
public class DefaultDigester
    implements Digester
{
    private static final int CHECKSUM_BUFFER_SIZE = 16384;

    private static final int BYTE_MASK = 0xFF;

    public String createChecksum( File file, String algorithm )
        throws IOException, NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance( algorithm );

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
            IOUtil.close( fis );
        }

        return byteArrayToHexStr( digest.digest() );
    }

    public boolean verifyChecksum( File file, String checksum, String algorithm )
        throws NoSuchAlgorithmException, IOException
    {
        boolean result = true;

        String trimmedChecksum = checksum.replace( '\n', ' ' ).trim();
        // Free-BSD / openssl
        Matcher m =
            Pattern.compile( algorithm.replaceAll( "-", "" ) + "\\s*\\((.*?)\\)\\s*=\\s*([a-zA-Z0-9]+)" ).matcher(
                trimmedChecksum );
        if ( m.matches() )
        {
            String filename = m.group( 1 );
            if ( !filename.equals( file.getName() ) )
            {
                // TODO: provide better warning
                result = false;
            }
            trimmedChecksum = m.group( 2 );
        }
        else
        {
            // GNU tools
            m = Pattern.compile( "([a-zA-Z0-9]+)\\s\\*?(.+)" ).matcher( trimmedChecksum );
            if ( m.matches() )
            {
                String filename = m.group( 2 );
                if ( !filename.equals( file.getName() ) )
                {
                    // TODO: provide better warning
                    result = false;
                }
                trimmedChecksum = m.group( 1 );
            }
        }

        if ( result )
        {
            //Create checksum for jar file
            String sum = createChecksum( file, algorithm );
            result = trimmedChecksum.toUpperCase().equals( sum.toUpperCase() );
        }
        return result;
    }

    /**
     * Convert an incoming array of bytes into a string that represents each of
     * the bytes as two hex characters.
     *
     * @param data
     */
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
