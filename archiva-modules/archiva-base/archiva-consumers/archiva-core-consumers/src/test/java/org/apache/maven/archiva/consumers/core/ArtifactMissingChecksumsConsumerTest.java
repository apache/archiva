package org.apache.maven.archiva.consumers.core;

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;

import java.io.File;
import java.util.Calendar;

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

public class ArtifactMissingChecksumsConsumerTest
    extends AbstractArtifactConsumerTest
{
    private ManagedRepositoryConfiguration repoConfig;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( "test-repo" );
        repoConfig.setName( "Test Repository" );
        repoConfig.setLayout( "default" );
        repoConfig.setLocation( new File( getBasedir(), "target/test-classes/test-repo/" ).getPath() );

        consumer = (ArtifactMissingChecksumsConsumer) lookup( "artifactMissingChecksumsConsumer" );
    }

    public void testNoExistingChecksums()
        throws Exception
    {
        String path = "/no-checksums-artifact/1.0/no-checksums-artifact-1.0.jar";

        File sha1File = new File( repoConfig.getLocation(), path + ".sha1" );
        File md5File = new File( repoConfig.getLocation(), path + ".md5" );

        sha1File.delete();
        md5File.delete();

        assertFalse( sha1File.exists() );
        assertFalse( md5File.exists() );

        consumer.beginScan( repoConfig, Calendar.getInstance().getTime() );

        consumer.processFile( path );

        assertTrue( sha1File.exists() );
        assertTrue( md5File.exists() );
    }

    public void testExistingIncorrectChecksums()
        throws Exception
    {
        File newLocation = getTestFile( "target/test-repo" );
        FileUtils.deleteDirectory( newLocation );
        FileUtils.copyDirectory( new File( repoConfig.getLocation() ), newLocation );
        repoConfig.setLocation( newLocation.getAbsolutePath() );

        String path = "/incorrect-checksums/1.0/incorrect-checksums-1.0.jar";

        File sha1File = new File( repoConfig.getLocation(), path + ".sha1" );
        File md5File = new File( repoConfig.getLocation(), path + ".md5" );

        ChecksummedFile checksum = new ChecksummedFile( new File( repoConfig.getLocation(), path ) );

        assertTrue( sha1File.exists() );
        assertTrue( md5File.exists() );
        assertFalse( checksum.isValidChecksums( new ChecksumAlgorithm[] { ChecksumAlgorithm.MD5, ChecksumAlgorithm.SHA1 } ) );

        consumer.beginScan( repoConfig, Calendar.getInstance().getTime() );

        consumer.processFile( path );

        assertTrue( sha1File.exists() );
        assertTrue( md5File.exists() );
        assertTrue( checksum.isValidChecksums( new ChecksumAlgorithm[] { ChecksumAlgorithm.MD5, ChecksumAlgorithm.SHA1 } ) );        
    }
}
