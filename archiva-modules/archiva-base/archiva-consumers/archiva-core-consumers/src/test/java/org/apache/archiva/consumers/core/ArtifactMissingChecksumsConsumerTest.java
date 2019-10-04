package org.apache.archiva.consumers.core;

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.repository.base.BasicManagedRepository;
import org.apache.archiva.repository.EditableManagedRepository;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
    private EditableManagedRepository repoConfig;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        Path basePath = Paths.get("target/test-classes");
        repoConfig = BasicManagedRepository.newFilesystemInstance( "test-repo", "Test Repository", basePath.resolve("test-repo"));
        repoConfig.setLayout( "default" );
        repoConfig.setLocation(basePath.resolve("test-repo/" ).toUri() );

        consumer = applicationContext.getBean( "knownRepositoryContentConsumer#create-missing-checksums",
                                               KnownRepositoryContentConsumer.class );
    }

    @Test
    public void testNoExistingChecksums()
        throws Exception
    {
        String path = "no-checksums-artifact/1.0/no-checksums-artifact-1.0.jar";

        Path basePath = PathUtil.getPathFromUri( repoConfig.getLocation() );
        Path sha1Path = basePath.resolve(path + ".sha1" );
        Path md5FilePath = basePath.resolve(path + ".md5" );

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
        repoConfig.setLocation( newLocation.toAbsolutePath().toUri() );
        Path basePath = PathUtil.getPathFromUri( repoConfig.getLocation() );

        String path = "incorrect-checksums/1.0/incorrect-checksums-1.0.jar";

        Path sha1Path = basePath.resolve( path + ".sha1" );

        Path md5Path = basePath.resolve( path + ".md5" );

        ChecksummedFile checksum = new ChecksummedFile( basePath.resolve( path ) );

        Assertions.assertThat( sha1Path.toFile() ).exists();
        Assertions.assertThat( md5Path.toFile() ).exists();
        Assertions.assertThat(
            checksum.isValidChecksums( Arrays.asList(ChecksumAlgorithm.MD5, ChecksumAlgorithm.SHA1 ) ) ) //
            .isFalse();

        consumer.beginScan( repoConfig, Calendar.getInstance().getTime() );

        consumer.processFile( path );

        Assertions.assertThat( sha1Path.toFile() ).exists();
        Assertions.assertThat( md5Path.toFile() ).exists();
        Assertions.assertThat(
            checksum.isValidChecksums( Arrays.asList(ChecksumAlgorithm.MD5, ChecksumAlgorithm.SHA1 )) ) //
            .isTrue();
    }
}
