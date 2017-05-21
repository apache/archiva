package org.apache.archiva.common.filelock;

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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author Olivier Lamy
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:/META-INF/spring-context.xml" })
public class DefaultFileLockManagerTimeoutTest
{

    final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named(value = "fileLockManager#default")
    FileLockManager fileLockManager;

    @Before
    public void initialize()
    {
        fileLockManager.setSkipLocking( false );

        fileLockManager.setTimeout( 5000 );

        fileLockManager.clearLockFiles();
    }

    @Test(expected = FileLockTimeoutException.class)
    public void testTimeout()
        throws Throwable
    {

        File file = new File( System.getProperty( "buildDirectory" ), "foo.txt" );

        Files.deleteIfExists( file.toPath() );

        File largeJar = new File( System.getProperty( "basedir" ), "src/test/cassandra-all-2.0.3.jar" );

        Lock lock = fileLockManager.writeFileLock( file );

        Files.copy( largeJar.toPath(), lock.getFile().toPath(), StandardCopyOption.REPLACE_EXISTING );

        lock = fileLockManager.writeFileLock( file );

    }

}
