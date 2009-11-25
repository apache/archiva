package org.apache.archiva.metadata.repository.storage.maven2;

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
import java.util.Arrays;

import org.apache.maven.archiva.xml.XMLException;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

/**
 * RepositoryMetadataReaderTest
 *
 * @version $Id$
 */
public class MavenRepositoryMetadataReaderTest
    extends PlexusInSpringTestCase
{
    public void testProjectMetadata()
        throws XMLException
    {
        File defaultRepoDir = new File( getBasedir(), "src/test/repositories/test" );
        File metadataFile = new File( defaultRepoDir, "org/apache/maven/shared/maven-downloader/maven-metadata.xml" );

        MavenRepositoryMetadata metadata = MavenRepositoryMetadataReader.read( metadataFile );

        assertNotNull( metadata );
        assertEquals( "org.apache.maven.shared", metadata.getGroupId() );
        assertEquals( "maven-downloader", metadata.getArtifactId() );
        assertEquals( "1.1", metadata.getReleasedVersion() );
        assertNull( metadata.getLatestVersion() );
        assertEquals( Arrays.asList( "1.0", "1.1" ), metadata.getAvailableVersions() );
        assertNull( metadata.getSnapshotVersion() );
        assertEquals( "20061212214311", metadata.getLastUpdated() );
    }

    public void testProjectVersionMetadata()
        throws XMLException
    {
        File defaultRepoDir = new File( getBasedir(), "src/test/repositories/test" );
        File metadataFile = new File( defaultRepoDir, "org/apache/apache/5-SNAPSHOT/maven-metadata.xml" );

        MavenRepositoryMetadata metadata = MavenRepositoryMetadataReader.read( metadataFile );

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
}