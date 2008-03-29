/* ========================================================================== *
 *         Copyright (C) 2004-2006, Pier Fumagalli <http://could.it/>         *
 *                            All rights reserved.                            *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== */
package it.could.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>An utility class providing various static methods operating on
 * {@link InputStream input} and {@link OutputStream output} streams.</p>
 *
 * @author <a href="http://could.it/">Pier Fumagalli</a>
 */
public final class StreamTools {

    /** <p>Deny construction.</p> */
    private StreamTools() { };

    /**
     * <p>Copy every byte from the specified {@link InputStream} to the specifed
     * {@link OutputStream} and then close both of them.</p>
     * 
     * <p>This method is equivalent to a call to the following method:
     * {@link #copy(InputStream,OutputStream,boolean) copy(in, out, true)}.</p>
     * 
     * @param in the {@link InputStream} to read bytes from.
     * @param out the {@link OutputStream} to write bytes to.
     * @return the number of bytes copied.
     * @throws IOException if an I/O error occurred copying the data.
     */
    public static long copy(InputStream in, OutputStream out)
    throws IOException {
        return copy(in, out, true);
    }

    /**
     * <p>Copy every byte from the specified {@link InputStream} to the specifed
     * {@link OutputStream} and then optionally close both of them.</p>
     * 
     * @param in the {@link InputStream} to read bytes from.
     * @param out the {@link OutputStream} to write bytes to.
     * @param close whether to close the streams or not.
     * @return the number of bytes copied.
     * @throws IOException if an I/O error occurred copying the data.
     */
    public static long copy(InputStream in, OutputStream out, boolean close)
    throws IOException {
        if (in == null) throw new NullPointerException("Null input");
        if (out == null) throw new NullPointerException("Null output");

        final byte buffer[] = new byte[4096];
        int length = -1;
        long total = 0;
        while ((length = in.read(buffer)) >= 0) {
            out.write(buffer, 0, length);
            total += length;
        }
        
        if (close) {
            in.close();
            out.close();
        }

        return total;
    }
    
    /**
     * Closes the output stream. The output stream can be null and any IOException's will be swallowed.
     * 
     * @param outputStream The stream to close.
     */
    public static void close( OutputStream outputStream )
    {
        if ( outputStream == null )
        {
            return;
        }

        try
        {
            outputStream.close();
        }
        catch( IOException ex )
        {
            // ignore
        }
    }
    
    /**
     * Closes the input stream. The input stream can be null and any IOException's will be swallowed.
     * 
     * @param inputStream The stream to close.
     */
    public static void close( InputStream inputStream )
    {
        if ( inputStream == null )
        {
            return;
        }

        try
        {
            inputStream.close();
        }
        catch( IOException ex )
        {
            // ignore
        }
    }
}
