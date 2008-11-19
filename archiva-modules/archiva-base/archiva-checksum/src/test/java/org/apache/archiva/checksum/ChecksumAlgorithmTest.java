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

import java.io.File;

import junit.framework.TestCase;

/**
 * ChecksumAlgorithmTest
 *
 * @version $Id$
 */
public class ChecksumAlgorithmTest
    extends TestCase
{
    public void testGetHashByExtensionSha1()
    {
        assertEquals( ChecksumAlgorithm.SHA1, ChecksumAlgorithm.getByExtension( new File( "something.jar.sha1" ) ) );
        assertEquals( ChecksumAlgorithm.SHA1, ChecksumAlgorithm.getByExtension( new File( "OTHER.JAR.SHA1" ) ) );
    }

    public void testGetHashByExtensionMd5()
    {
        assertEquals( ChecksumAlgorithm.MD5, ChecksumAlgorithm.getByExtension( new File( "something.jar.md5" ) ) );
        assertEquals( ChecksumAlgorithm.MD5, ChecksumAlgorithm.getByExtension( new File( "OTHER.JAR.MD5" ) ) );
    }

    public void testGetHashByExtensionInvalid()
    {
        try
        {
            ChecksumAlgorithm.getByExtension( new File( "something.jar" ) );
            fail( "Expected " + IllegalArgumentException.class.getName() );
        }
        catch ( IllegalArgumentException e )
        {
            /* expected path */
        }
    }
}
