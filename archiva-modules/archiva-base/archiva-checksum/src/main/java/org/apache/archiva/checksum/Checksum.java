package org.apache.archiva.checksum;

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

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

/**
 * Checksum - simple checksum hashing routines. 
 *
 * @version $Id$
 */
public class Checksum
{
    private static final int BUFFER_SIZE = 32768;

    public static void update( List<Checksum> checksums, InputStream stream )
        throws IOException
    {
        byte[] buffer = new byte[BUFFER_SIZE];
        int size = stream.read( buffer, 0, BUFFER_SIZE );
        while ( size >= 0 )
        {
            for ( Checksum checksum : checksums )
            {
                checksum.update( buffer, 0, size );
            }
            size = stream.read( buffer, 0, BUFFER_SIZE );
        }
    }

    protected final MessageDigest md;

    private ChecksumAlgorithm checksumAlgorithm;

    public Checksum( ChecksumAlgorithm checksumAlgorithm )
    {
        this.checksumAlgorithm = checksumAlgorithm;
        try
        {
            md = MessageDigest.getInstance( checksumAlgorithm.getAlgorithm() );
        }
        catch ( NoSuchAlgorithmException e )
        {
            // Not really possible, but here none-the-less
            throw new IllegalStateException( "Unable to initialize MessageDigest algorithm " + checksumAlgorithm.getAlgorithm()
                + " : " + e.getMessage(), e );
        }
    }

    public String getChecksum()
    {
        return Hex.encode( md.digest() );
    }

    public ChecksumAlgorithm getAlgorithm()
    {
        return this.checksumAlgorithm;
    }

    public void reset()
    {
        md.reset();
    }

    public Checksum update( byte[] buffer, int offset, int size )
    {
        md.update( buffer, 0, size );
        return this;
    }

    public Checksum update( InputStream stream )
        throws IOException
    {
        DigestInputStream dig = new DigestInputStream( stream, md );
        IOUtils.copy( dig, new NullOutputStream() );

        return this;
    }
}
