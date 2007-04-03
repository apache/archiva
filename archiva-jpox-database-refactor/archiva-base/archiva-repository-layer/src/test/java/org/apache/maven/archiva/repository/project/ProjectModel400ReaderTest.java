package org.apache.maven.archiva.repository.project;

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

import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * ProjectModel400ReaderTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ProjectModel400ReaderTest extends PlexusTestCase
{
    public void testLoadSimple() throws ProjectModelException
    {
        File defaultRepoDir = new File( getBasedir(), "src/test/repositories/default-repository" );
        File pomFile = new File( defaultRepoDir, "org/apache/maven/shared/maven-downloader/1.0/maven-downloader-1.0.pom" );

        ProjectModelReader reader = new ProjectModel400Reader();

        ArchivaProjectModel project = reader.read( pomFile );

        assertNotNull( project );
        assertEquals( "Group Id", "org.apache.maven.shared", project.getContentKey().getGroupId() );
        assertEquals( "Artifact Id", "maven-downloader", project.getContentKey().getArtifactId() );
        assertEquals( "Released Version", "1.0", project.getContentKey().getVersion() );
        assertEquals( "Name", "Maven Downloader", project.getName() );
        assertEquals( "Description", "Provide a super simple interface for downloading a single artifact.", project.getDescription() );
//        assertTrue( "Available version 1.0", project.getAvailableVersions().contains( "1.0" ) );
//        assertTrue( "Available version 1.1", project.getAvailableVersions().contains( "1.1" ) );
    }
}
