package org.apache.archiva.repository.maven.metadata.storage;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.apache.archiva.metadata.maven.MavenMetadataReader;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.Plugin;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * RepositoryMetadataReaderTest
 *
 *
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class MavenRepositoryMetadataReaderTest
    extends TestCase
{
    private Path defaultRepoDir;

    @Test
    public void testGroupMetadata()
        throws RepositoryMetadataException
    {
        Path metadataFile = defaultRepoDir.resolve( "org/apache/maven/plugins/maven-metadata.xml" );

        MavenMetadataReader metadataReader = new MavenMetadataReader( );
        ArchivaRepositoryMetadata metadata = metadataReader.read( metadataFile );

        assertNotNull( metadata );
        assertEquals( "org.apache.maven.plugins", metadata.getGroupId() );
        assertNull( metadata.getArtifactId() );
        assertNull( metadata.getReleasedVersion() );
        assertNull( metadata.getLatestVersion() );
        assertTrue( metadata.getAvailableVersions().isEmpty() );
        assertNull( metadata.getSnapshotVersion() );
        assertNull( metadata.getLastUpdated() );

        Plugin cleanPlugin = new Plugin();
        cleanPlugin.setPrefix( "clean" );
        cleanPlugin.setArtifactId( "maven-clean-plugin" );
        cleanPlugin.setName( "Maven Clean Plugin" );

        Plugin compilerPlugin = new Plugin();
        compilerPlugin.setPrefix( "compiler" );
        compilerPlugin.setArtifactId( "maven-compiler-plugin" );
        compilerPlugin.setName( "Maven Compiler Plugin" );

        Plugin surefirePlugin = new Plugin();
        surefirePlugin.setPrefix( "surefire" );
        surefirePlugin.setArtifactId( "maven-surefire-plugin" );
        surefirePlugin.setName( "Maven Surefire Plugin" );

        assertEquals( Arrays.asList( cleanPlugin, compilerPlugin, surefirePlugin ), metadata.getPlugins() );
    }

    @Test
    public void testProjectMetadata()
        throws RepositoryMetadataException
    {
        Path metadataFile = defaultRepoDir.resolve( "org/apache/maven/shared/maven-downloader/maven-metadata.xml" );

        MavenMetadataReader metadataReader = new MavenMetadataReader( );
        ArchivaRepositoryMetadata metadata = metadataReader.read( metadataFile );

        assertNotNull( metadata );
        assertEquals( "org.apache.maven.shared", metadata.getGroupId() );
        assertEquals( "maven-downloader", metadata.getArtifactId() );
        assertEquals( "1.1", metadata.getReleasedVersion() );
        assertNull( metadata.getLatestVersion() );
        assertEquals( Arrays.asList( "1.0", "1.1" ), metadata.getAvailableVersions() );
        assertNull( metadata.getSnapshotVersion() );
        assertEquals( "20061212214311", metadata.getLastUpdated() );
    }

    @Test
    public void testProjectVersionMetadata()
        throws RepositoryMetadataException
    {
        Path metadataFile = defaultRepoDir.resolve( "org/apache/apache/5-SNAPSHOT/maven-metadata.xml" );

        MavenMetadataReader metadataReader = new MavenMetadataReader( );
        ArchivaRepositoryMetadata metadata = metadataReader.read(metadataFile );

        assertNotNull( metadata );
        assertEquals( "org.apache", metadata.getGroupId() );
        assertEquals( "apache", metadata.getArtifactId() );
        assertNull( metadata.getReleasedVersion() );
        assertNull( metadata.getLatestVersion() );
        assertTrue( metadata.getAvailableVersions().isEmpty() );
        assertNotNull( metadata.getSnapshotVersion() );
        assertEquals( "20080801.151215", metadata.getSnapshotVersion().getTimestamp() );
        assertEquals( 1, metadata.getSnapshotVersion().getBuildNumber() );
        assertEquals( "20080801151215", metadata.getLastUpdated() );
    }

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        defaultRepoDir = Paths.get("target/test-repository");
    }
}