package org.apache.archiva.consumers.core;

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private ManagedRepository repoConfig;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        repoConfig = new ManagedRepository();
        repoConfig.setId( "test-repo" );
        repoConfig.setName( "Test Repository" );
        repoConfig.setLayout( "default" );
        repoConfig.setLocation( Paths.get( "target/test-classes/test-repo/" ).toString() );

        consumer = applicationContext.getBean( "knownRepositoryContentConsumer#create-missing-checksums",
                                               KnownRepositoryContentConsumer.class );
    }

    @Test
    public void testNoExistingChecksums()
        throws Exception
    {
        String path = "no-checksums-artifact/1.0/no-checksums-artifact-1.0.jar";

        Path sha1Path = Paths.get( repoConfig.getLocation(), path + ".sha1" );
        Path md5FilePath = Paths.get( repoConfig.getLocation(), path + ".md5" );

        Files.deleteIfExists( sha1Path );
        Files.deleteIfExists( md5FilePath );

        Assertions.assertThat( sha1Path.toFile() ).doesNotExist();
        Assertions.assertThat( md5FilePath.toFile() ).doesNotExist();

        consumer.beginScan( repoConfig, Calendar.getInstance().getTime() );

        consumer.processFile( path );

        Assertions.assertThat( sha1Path.toFile() ).exists();
        long sha1LastModified = sha1Path.toFile().lastModified();
        Assertions.assertThat( md5FilePath.toFile() ).exists();
        long md5LastModified = md5FilePath.toFile().lastModified();
        Thread.sleep( 1000 );
        consumer.processFile( path );

        Assertions.assertThat( sha1Path.toFile() ).exists();
        Assertions.assertThat( md5FilePath.toFile() ).exists();

        Assertions.assertThat( sha1Path.toFile().lastModified() ).isEqualTo( sha1LastModified );

        Assertions.assertThat( md5FilePath.toFile().lastModified() ).isEqualTo( md5LastModified );
    }

    @Test
    public void testExistingIncorrectChecksums()
        throws Exception
    {
        Path newLocation = Paths.get( "target/test-repo" );
        org.apache.archiva.common.utils.FileUtils.deleteDirectory( newLocation );
        FileUtils.copyDirectory( Paths.get(repoConfig.getLocation() ).toFile(), newLocation.toFile() );
        repoConfig.setLocation( newLocation.toAbsolutePath().toString() );

        String path = "incorrect-checksums/1.0/incorrect-checksums-1.0.jar";

        Path sha1Path = Paths.get( repoConfig.getLocation(), path + ".sha1" );

        Path md5Path = Paths.get( repoConfig.getLocation(), path + ".md5" );

        ChecksummedFile checksum = new ChecksummedFile( Paths.get(repoConfig.getLocation(), path ) );

        Assertions.assertThat( sha1Path.toFile() ).exists();
        Assertions.assertThat( md5Path.toFile() ).exists();
        Assertions.assertThat(
            checksum.isValidChecksums( new ChecksumAlgorithm[]{ ChecksumAlgorithm.MD5, ChecksumAlgorithm.SHA1 } ) ) //
            .isFalse();

        consumer.beginScan( repoConfig, Calendar.getInstance().getTime() );

        consumer.processFile( path );

        Assertions.assertThat( sha1Path.toFile() ).exists();
        Assertions.assertThat( md5Path.toFile() ).exists();
        Assertions.assertThat(
            checksum.isValidChecksums( new ChecksumAlgorithm[]{ ChecksumAlgorithm.MD5, ChecksumAlgorithm.SHA1 } ) ) //
            .isTrue();
    }
}
