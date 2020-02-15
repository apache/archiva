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

import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.nio.file.Paths;
import org.junit.Assert;

/**
 * ChecksumAlgorithmTest
 *
 *
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class ChecksumAlgorithmTest
{
    @Test
    public void testGetHashByExtensionSha1()
    {
        Assert.assertEquals( ChecksumAlgorithm.SHA1, ChecksumAlgorithm.getByExtension( Paths.get( "something.jar.sha1" ) ) );
        Assert.assertEquals( ChecksumAlgorithm.SHA1, ChecksumAlgorithm.getByExtension( Paths.get( "OTHER.JAR.SHA1" ) ) );
    }
    
    @Test
    public void testGetHashByExtensionMd5()
    {
        Assert.assertEquals( ChecksumAlgorithm.MD5, ChecksumAlgorithm.getByExtension( Paths.get( "something.jar.md5" ) ) );
        Assert.assertEquals( ChecksumAlgorithm.MD5, ChecksumAlgorithm.getByExtension( Paths.get( "OTHER.JAR.MD5" ) ) );
    }

    @Test
    public void testGetHashByExtensionInvalid()
    {
        try
        {
            ChecksumAlgorithm.getByExtension( Paths.get( "something.jar" ) );
            Assert.fail( "Expected " + IllegalArgumentException.class.getName() );
        }
        catch ( IllegalArgumentException e )
        {
            /* expected path */
        }
    }
}
