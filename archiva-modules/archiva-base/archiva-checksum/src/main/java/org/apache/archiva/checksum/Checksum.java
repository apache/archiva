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

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Checksum - simple checksum hashing routines.
 */
public class Checksum
{
    private byte[] result = new byte[0];

    private final MessageDigest md;

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
            throw new IllegalStateException(
                "Unable to initialize MessageDigest algorithm " + checksumAlgorithm.getAlgorithm() + " : "
                    + e.getMessage(), e );
        }
    }

    public String getChecksum()
    {
        if (this.result.length==0) {
            finish();
        }
        return Hex.encode( this.result );
    }

    public byte[] getChecksumBytes() {
        if (this.result.length==0) {
            finish();
        }
        return this.result;
    }

    public ChecksumAlgorithm getAlgorithm()
    {
        return this.checksumAlgorithm;
    }

    public void reset()
    {
        md.reset();
        this.result = new byte[0];
    }

    public Checksum update( byte[] buffer, int offset, int size )
    {
        if (this.result.length!=0) {
            reset();
        }
        md.update( buffer, 0, size );
        return this;
    }

    public Checksum update( ByteBuffer buffer)
    {
        if (this.result.length!=0) {
            reset();
        }
        md.update( buffer );
        return this;
    }

    public Checksum finish() {
        this.result = md.digest();
        return this;
    }

    public boolean compare(byte[] cmp) {
        if (this.result == null || this.result.length==0) {
            finish();
        }
        return MessageDigest.isEqual( this.result, cmp );
    }

    public boolean compare(String hexString) {
        if (this.result == null || this.result.length==0) {
            finish();
        }
        return MessageDigest.isEqual(this.result, Hex.decode( hexString ));
    }
}
