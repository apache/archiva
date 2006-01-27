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

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Test the digester.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class DigesterTest
    extends TestCase
{
    private Digester digester = new DefaultDigester();

    private static final String MD5 = "adbc688ce77fa2aece4bb72cad9f98ba";

    private static final String SHA1 = "2a7b459938e12a2dc35d1bf6cff35e9c2b592fa9";

    private static final String WRONG_SHA1 = "4d8703779816556cdb8be7f6bb5c954f4b5730e2";

    public void testBareDigestFormat()
        throws NoSuchAlgorithmException, IOException
    {
        File file = new File( getClass().getResource( "/test-file.txt" ).getPath() );
        assertTrue( "test bare format MD5", digester.verifyChecksum( file, MD5, Digester.MD5 ) );
        assertTrue( "test bare format SHA1", digester.verifyChecksum( file, SHA1, Digester.SHA1 ) );

        assertFalse( "test wrong sha1", digester.verifyChecksum( file, WRONG_SHA1, Digester.SHA1 ) );
    }

    public void testOpensslDigestFormat()
        throws NoSuchAlgorithmException, IOException
    {
        File file = new File( getClass().getResource( "/test-file.txt" ).getPath() );
        assertTrue( "test openssl format MD5",
                    digester.verifyChecksum( file, "MD5(test-file.txt)= " + MD5, Digester.MD5 ) );
        assertTrue( "test openssl format SHA1",
                    digester.verifyChecksum( file, "SHA1(test-file.txt)= " + SHA1, Digester.SHA1 ) );

        assertTrue( "test freebsd format MD5",
                    digester.verifyChecksum( file, "MD5 (test-file.txt) = " + MD5, Digester.MD5 ) );
        assertTrue( "test freebsd format SHA1",
                    digester.verifyChecksum( file, "SHA1 (test-file.txt) = " + SHA1, Digester.SHA1 ) );

        assertFalse( "test wrong filename", digester.verifyChecksum( file, "SHA1 (FOO) = " + SHA1, Digester.SHA1 ) );
        assertFalse( "test wrong sha1",
                     digester.verifyChecksum( file, "SHA1 (test-file.txt) = " + WRONG_SHA1, Digester.SHA1 ) );
    }

    public void testGnuDigestFormat()
        throws NoSuchAlgorithmException, IOException
    {
        File file = new File( getClass().getResource( "/test-file.txt" ).getPath() );
        assertTrue( "test GNU format MD5", digester.verifyChecksum( file, MD5 + " *test-file.txt", Digester.MD5 ) );
        assertTrue( "test GNU format SHA1", digester.verifyChecksum( file, SHA1 + " *test-file.txt", Digester.SHA1 ) );

        assertTrue( "test GNU text format MD5", digester.verifyChecksum( file, MD5 + " test-file.txt", Digester.MD5 ) );
        assertTrue( "test GNU text format SHA1",
                    digester.verifyChecksum( file, SHA1 + " test-file.txt", Digester.SHA1 ) );

        assertFalse( "test wrong filename", digester.verifyChecksum( file, SHA1 + " FOO", Digester.SHA1 ) );
        assertFalse( "test wrong sha1", digester.verifyChecksum( file, WRONG_SHA1 + " test-file.txt", Digester.SHA1 ) );
    }

    public void testUntrimmedContent()
        throws NoSuchAlgorithmException, IOException
    {
        File file = new File( getClass().getResource( "/test-file.txt" ).getPath() );
        assertTrue( "test untrimmed GNU format SHA1",
                    digester.verifyChecksum( file, SHA1 + " *test-file.txt \n", Digester.SHA1 ) );
    }
}
