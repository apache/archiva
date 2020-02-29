package org.apache.archiva.rest.services.utils;
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

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.repository.storage.fs.FilesystemAsset;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.easymock.TestSubject;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class ArtifactBuilderTest
{
    @TestSubject
    private ArtifactBuilder builder = new ArtifactBuilder();

    StorageAsset getFile(String path) throws IOException {
        Path filePath = Paths.get(path);
        FilesystemStorage filesystemStorage = new FilesystemStorage(filePath.getParent(), new DefaultFileLockManager());
        return new FilesystemAsset(filesystemStorage, filePath.getFileName().toString(), filePath);
    }

    @Test
    public void testBuildSnapshot() throws IOException {
        assertThat( builder.getExtensionFromFile( getFile( "/tmp/foo-2.3-20141119.064321-40.jar" ) ) ).isEqualTo( "jar" );
    }

    @Test
    public void testBuildPom() throws IOException {
        assertThat( builder.getExtensionFromFile( getFile( "/tmp/foo-1.0.pom" ) ) ).isEqualTo( "pom" );
    }

    @Test
    public void testBuildJar() throws IOException {
        assertThat( builder.getExtensionFromFile( getFile( "/tmp/foo-1.0-sources.jar" ) ) ).isEqualTo( "jar" );
    }

    @Test
    public void testBuildTarGz() throws IOException {
        assertThat( builder.getExtensionFromFile( getFile( "/tmp/foo-1.0.tar.gz" ) ) ).isEqualTo( "tar.gz" );
    }

    @Test
    public void testBuildPomZip() throws IOException {
        assertThat( builder.getExtensionFromFile( getFile( "/tmp/foo-1.0.pom.zip" ) ) ).isEqualTo( "pom.zip" );
    }

    @Test
    public void testBuildR00() throws IOException {
        assertThat( builder.getExtensionFromFile( getFile( "/tmp/foo-1.0.r00" ) ) ).isEqualTo( "r00" );
    }
}
