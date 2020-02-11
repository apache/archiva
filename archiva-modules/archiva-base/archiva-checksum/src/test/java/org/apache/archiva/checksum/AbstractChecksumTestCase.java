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

import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * AbstractChecksumTestCase
 *
 *
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public abstract class AbstractChecksumTestCase
{
    @Rule public TestName name = new TestName();

    public Path getTestOutputDir()
    {
        Path dir = Paths.get( FileUtils.getBasedir(), "target/test-output/" + name.getMethodName() );
        if ( !Files.exists(dir))
        {
            try
            {
                Files.createDirectories( dir );
            }
            catch ( IOException e )
            {
                Assert.fail( "Unable to create test output directory: " + dir.toAbsolutePath() );
            }
        }
        return dir;
    }

    public Path getTestResource( String filename )
    {
        Path dir = Paths.get( FileUtils.getBasedir(), "src/test/resources" );
        Path file = dir.resolve(filename );
        if ( !Files.exists(file))
        {
            Assert.fail( "Test Resource does not exist: " + file.toAbsolutePath() );
        }
        return file;
    }

}
