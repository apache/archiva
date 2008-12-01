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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ChecksumTest
 *
 * @version $Id$
 */
public class ChecksumTest
    extends AbstractChecksumTestCase
{
    private static final String UNSET_SHA1 = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

    public void testConstructSha1()
    {
        Checksum checksum = new Checksum( ChecksumAlgorithm.SHA1 );
        assertEquals( "Checksum.algorithm", checksum.getAlgorithm().getAlgorithm(), ChecksumAlgorithm.SHA1
            .getAlgorithm() );
    }

    public void testConstructMd5()
    {
        Checksum checksum = new Checksum( ChecksumAlgorithm.MD5 );
        assertEquals( "Checksum.algorithm", checksum.getAlgorithm().getAlgorithm(), ChecksumAlgorithm.MD5
            .getAlgorithm() );
    }

    public void testUpdate()
    {
        Checksum checksum = new Checksum( ChecksumAlgorithm.SHA1 );
        byte buf[] = ( "You know, I'm sick of following my dreams, man. "
            + "I'm just going to ask where they're going and hook up with 'em later. - Mitch Hedberg" ).getBytes();
        checksum.update( buf, 0, buf.length );
        assertEquals( "Checksum", "e396119ae0542e85a74759602fd2f81e5d36d762", checksum.getChecksum() );
    }

    public void testUpdateMany()
        throws IOException
    {
        Checksum checksumSha1 = new Checksum( ChecksumAlgorithm.SHA1 );
        Checksum checksumMd5 = new Checksum( ChecksumAlgorithm.MD5 );
        List<Checksum> checksums = new ArrayList<Checksum>();
        checksums.add( checksumSha1 );
        checksums.add( checksumMd5 );

        byte buf[] = ( "You know, I'm sick of following my dreams, man. "
            + "I'm just going to ask where they're going and hook up with 'em later. - Mitch Hedberg" ).getBytes();

        ByteArrayInputStream stream = new ByteArrayInputStream( buf );
        Checksum.update( checksums, stream );

        assertEquals( "Checksum SHA1", "e396119ae0542e85a74759602fd2f81e5d36d762", checksumSha1.getChecksum() );
        assertEquals( "Checksum MD5", "21c2c5ca87ec018adacb2e2fb3432219", checksumMd5.getChecksum() );
    }

    public void testUpdateWholeUpdatePartial()
    {
        Checksum checksum = new Checksum( ChecksumAlgorithm.SHA1 );
        assertEquals( "Checksum unset", UNSET_SHA1, checksum.getChecksum() );

        String expected = "066c2cbbc8cdaecb8ff97dcb84502462d6f575f3";
        byte reesepieces[] = "eatagramovabits".getBytes();
        checksum.update( reesepieces, 0, reesepieces.length );
        String actual = checksum.getChecksum();

        assertEquals( "Expected", expected, actual );

        // Reset the checksum.
        checksum.reset();
        assertEquals( "Checksum unset", UNSET_SHA1, checksum.getChecksum() );

        // Now parse it again in 3 pieces.
        checksum.update( reesepieces, 0, 5 );
        checksum.update( reesepieces, 5, 5 );
        checksum.update( reesepieces, 10, reesepieces.length - 10 );

        assertEquals( "Expected", expected, actual );
    }
}
