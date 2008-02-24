package org.apache.maven.archiva.common.utils;

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

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.PlexusTestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * ChecksumsTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ChecksumsTest
    extends PlexusTestCase
{
    private static final String GOOD = "good";

    private static final String BAD = "bad";

    public void testCheckOnFileOnly()
        throws Exception
    {
        assertCheck( false, null, null );
    }

    public void testCheckOnFileWithBadMd5AndBadSha1()
        throws Exception
    {
        assertCheck( false, BAD, BAD );
    }

    public void testCheckOnFileWithBadMd5AndGoodSha1()
        throws Exception
    {
        assertCheck( false, BAD, GOOD );
    }

    public void testCheckOnFileWithBadMd5Only()
        throws Exception
    {
        assertCheck( false, BAD, null );
    }

    public void testCheckOnFileWithBadSha1Only()
        throws Exception
    {
        assertCheck( false, null, BAD );
    }

    public void testCheckOnFileWithGoodMd5AndBadSha1()
        throws Exception
    {
        assertCheck( false, GOOD, BAD );
    }

    public void testCheckOnFileWithGoodMd5AndGoodSha1()
        throws Exception
    {
        assertCheck( true, GOOD, GOOD );
    }

    public void testCheckOnFileWithGoodMd5Only()
        throws Exception
    {
        assertCheck( true, GOOD, null );
    }

    public void testCheckOnFileWithGoodSha1Only()
        throws Exception
    {
        assertCheck( true, null, GOOD );
    }

    public void testUpdateOnFileOnly()
        throws Exception
    {
        assertUpdate( true, null, null );
    }

    public void testUpdateOnFileWithBadMd5AndBadSha1()
        throws Exception
    {
        assertUpdate( true, BAD, BAD );
    }

    public void testUpdateOnFileWithBadMd5AndGoodSha1()
        throws Exception
    {
        assertUpdate( true, BAD, GOOD );
    }

    public void testUpdateOnFileWithBadMd5Only()
        throws Exception
    {
        assertUpdate( true, BAD, null );
    }

    public void testUpdateOnFileWithBadSha1Only()
        throws Exception
    {
        assertUpdate( true, null, BAD );
    }

    public void testUpdateOnFileWithGoodMd5AndBadSha1()
        throws Exception
    {
        assertUpdate( true, GOOD, BAD );
    }

    public void testUpdateOnFileWithGoodMd5AndGoodSha1()
        throws Exception
    {
        assertUpdate( true, GOOD, GOOD );
    }

    public void testUpdateOnFileWithGoodMd5Only()
        throws Exception
    {
        assertUpdate( true, GOOD, null );
    }

    public void testUpdateOnFileWithGoodSha1Only()
        throws Exception
    {
        assertUpdate( true, null, GOOD );
    }

    private void assertCheck( boolean expectedResult, String md5State, String sha1State )
        throws Exception
    {
        Checksums checksums = lookupChecksums();
        File localFile = createTestableFiles( md5State, sha1State );

        boolean actualResult = checksums.check( localFile );
        String msg = createMessage( "check", md5State, sha1State );

        if ( actualResult == false )
        {
            assertFalse( msg + " local file should not exist:", localFile.exists() );
            File md5File = new File( localFile.getAbsolutePath() + ".sha1" );
            File sha1File = new File( localFile.getAbsolutePath() + ".md5" );
            assertFalse( msg + " local md5 file should not exist:", md5File.exists() );
            assertFalse( msg + " local sha1 file should not exist:", sha1File.exists() );
        }

        assertEquals( msg, expectedResult, actualResult );
    }

    private void assertUpdate( boolean expectedResult, String md5State, String sha1State )
        throws Exception
    {
        Checksums checksums = lookupChecksums();
        File localFile = createTestableFiles( md5State, sha1State );

        boolean actualResult = checksums.update( localFile );
        String msg = createMessage( "update", md5State, sha1State );
        assertEquals( msg, expectedResult, actualResult );

        // End result should be legitimate SHA1 and MD5 files.
        File md5File = new File( localFile.getAbsolutePath() + ".md5" );
        File sha1File = new File( localFile.getAbsolutePath() + ".sha1" );

        assertTrue( "ChecksumPolicy.apply(FIX) md5 should exist.", md5File.exists() && md5File.isFile() );
        assertTrue( "ChecksumPolicy.apply(FIX) sha1 should exist.", sha1File.exists() && sha1File.isFile() );

        String actualMd5Contents = readChecksumFile( md5File );
        String actualSha1Contents = readChecksumFile( sha1File );

        String expectedMd5Contents = "360ccd01d8a0a2d94b86f9802c2fc548  artifact.jar";
        String expectedSha1Contents = "7dd8929150664f182db60ad15f20359d875f059f  artifact.jar";

        assertEquals( msg + ": md5 contents:", expectedMd5Contents, actualMd5Contents );
        assertEquals( msg + ": sha1 contents:", expectedSha1Contents, actualSha1Contents );
    }

    /**
     * Read the first line from the checksum file, and return it (trimmed).
     */
    private String readChecksumFile( File checksumFile )
        throws Exception
    {
        FileReader freader = null;
        BufferedReader buf = null;

        try
        {
            freader = new FileReader( checksumFile );
            buf = new BufferedReader( freader );
            return buf.readLine();
        }
        finally
        {
            if ( buf != null )
            {
                buf.close();
            }

            if ( freader != null )
            {
                freader.close();
            }
        }
    }

    private String createMessage( String method, String md5State, String sha1State )
    {
        StringBuffer msg = new StringBuffer();
        msg.append( "Expected result of Checksums." ).append( method );
        msg.append( "() when working with " );
        if ( md5State == null )
        {
            msg.append( "NO" );
        }
        else
        {
            msg.append( "a " ).append( md5State.toUpperCase() );
        }

        msg.append( " MD5 and " );

        if ( sha1State == null )
        {
            msg.append( "NO" );
        }
        else
        {
            msg.append( "a " ).append( sha1State.toUpperCase() );
        }
        msg.append( " SHA1:" );

        return msg.toString();
    }

    private File createTestableFiles( String md5State, String sha1State )
        throws Exception
    {
        File sourceDir = new File( "src/test/resources/checksums/" );
        File destDir = new File( "target/checksum-tests/" + getName() + "/" );

        FileUtils.copyFileToDirectory( new File( sourceDir, "artifact.jar" ), destDir );

        if ( md5State != null )
        {
            File md5File = new File( sourceDir, "artifact.jar.md5-" + md5State );
            assertTrue( "Testable file exists: " + md5File.getName() + ":", md5File.exists() && md5File.isFile() );
            File destFile = new File( destDir, "artifact.jar.md5" );
            FileUtils.copyFile( md5File, destFile );
        }

        if ( sha1State != null )
        {
            File sha1File = new File( sourceDir, "artifact.jar.sha1-" + sha1State );
            assertTrue( "Testable file exists: " + sha1File.getName() + ":", sha1File.exists() && sha1File.isFile() );
            File destFile = new File( destDir, "artifact.jar.sha1" );
            FileUtils.copyFile( sha1File, destFile );
        }

        File localFile = new File( destDir, "artifact.jar" );
        return localFile;
    }

    private Checksums lookupChecksums()
        throws Exception
    {
        Checksums policy = (Checksums) lookup( Checksums.class );
        assertNotNull( policy );
        return policy;
    }
}
