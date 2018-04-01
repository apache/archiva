package org.apache.archiva.transaction;

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

import junit.framework.TestCase;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public abstract class AbstractFileEventTest
    extends TestCase
{
    protected List<ChecksumAlgorithm> checksumAlgorithms;

    @SuppressWarnings( "unchecked" )
    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        checksumAlgorithms = Arrays.asList( ChecksumAlgorithm.SHA256, ChecksumAlgorithm.SHA1, ChecksumAlgorithm.MD5 );
    }

    protected void assertChecksumExists(Path file, String algorithm )
    {

        assertChecksum( file, algorithm, true );
    }

    protected void assertChecksumDoesNotExist( Path file, String algorithm )
    {
        assertChecksum( file, algorithm, false );
    }

    private void assertChecksum( Path file, String algorithm, boolean exist )
    {
        String msg = exist ? "exists" : "does not exist";
        Path checksumFile = Paths.get( file.toAbsolutePath() + "." + algorithm );
        assertEquals( "Test file " + algorithm + " checksum " + msg, exist, Files.exists(checksumFile) );
    }

    protected void assertChecksumCommit( Path file )
        throws IOException
    {
        assertChecksumExists( file, "md5" );
        assertChecksumExists( file, "sha1" );
    }

    protected void assertChecksumRollback( Path file )
        throws IOException
    {
        assertChecksumDoesNotExist( file, "md5" );
        assertChecksumDoesNotExist( file, "sha1" );
    }

    protected String readFile( Path file )
        throws IOException
    {
        return FileUtils.readFileToString( file.toFile(), Charset.forName( "UTF-8" ) );
    }

    protected void writeFile( Path file, String content )
        throws IOException
    {
        org.apache.archiva.common.utils.FileUtils.writeStringToFile( file, Charset.defaultCharset(), content );
    }
}
