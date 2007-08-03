package org.apache.maven.archiva.repository.metadata;

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

import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * RepositoryMetadataReaderTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryMetadataReaderTest extends PlexusTestCase
{
    public void testLoadSimple() throws RepositoryMetadataException
    {
        File defaultRepoDir = new File( getBasedir(), "src/test/repositories/default-repository" );
        File metadataFile = new File( defaultRepoDir, "org/apache/maven/shared/maven-downloader/maven-metadata.xml" );

        RepositoryMetadataReader reader = new RepositoryMetadataReader();

        ArchivaRepositoryMetadata metadata = reader.read( metadataFile );

        assertNotNull( metadata );
        assertEquals( "Group Id", "org.apache.maven.shared", metadata.getGroupId() );
        assertEquals( "Artifact Id", "maven-downloader", metadata.getArtifactId() );
        assertEquals( "Released Version", "1.1", metadata.getReleasedVersion() );
        assertEquals( "List of Available Versions", 2, metadata.getAvailableVersions().size() );
        assertTrue( "Available version 1.0", metadata.getAvailableVersions().contains( "1.0" ) );
        assertTrue( "Available version 1.1", metadata.getAvailableVersions().contains( "1.1" ) );
    }
    
    public void testLoadComplex() throws RepositoryMetadataException
    {
        File defaultRepoDir = new File( getBasedir(), "src/test/repositories/default-repository" );
        File metadataFile = new File( defaultRepoDir, "org/apache/maven/samplejar/maven-metadata.xml" );

        RepositoryMetadataReader reader = new RepositoryMetadataReader();

        ArchivaRepositoryMetadata metadata = reader.read( metadataFile );

        assertNotNull( metadata );
        assertEquals( "Group Id", "org.apache.maven", metadata.getGroupId() );
        assertEquals( "Artifact Id", "samplejar", metadata.getArtifactId() );
        assertEquals( "Released Version", "2.0", metadata.getReleasedVersion() );
        assertEquals( "Latest Version", "6.0-SNAPSHOT", metadata.getLatestVersion() );
        assertEquals( "List of Available Versions", 18, metadata.getAvailableVersions().size() );
        assertTrue( "Available version 6.0-20060311.183228-10", metadata.getAvailableVersions().contains( "6.0-20060311.183228-10" ) );
        assertTrue( "Available version 6.0-SNAPSHOT", metadata.getAvailableVersions().contains( "6.0-SNAPSHOT" ) );
    }
}
