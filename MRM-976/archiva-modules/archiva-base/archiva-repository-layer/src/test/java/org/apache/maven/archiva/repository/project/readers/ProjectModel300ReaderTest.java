package org.apache.maven.archiva.repository.project.readers;

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
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import java.io.File;

/**
 * ProjectModel300ReaderTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ProjectModel300ReaderTest
    extends PlexusInSpringTestCase
{
    public void testLoadSimple()
        throws ProjectModelException
    {
        File defaultRepoDir = new File( getBasedir(), "src/test/repositories/legacy-repository" );
        File pomFile = new File( defaultRepoDir, "org.apache.maven/poms/maven-model-v3-2.0.pom" );

        ProjectModelReader reader = new ProjectModel300Reader();

        ArchivaProjectModel project = reader.read( pomFile );

        assertNotNull( project );
        assertEquals( "Group Id", "org.apache.maven", project.getGroupId() );
        assertEquals( "Artifact Id", "maven-model-v3", project.getArtifactId() );
        assertEquals( "Version", "2.0", project.getVersion() );
        assertEquals( "Name", "Maven Model v3", project.getName() );
        assertEquals( "Description", "Maven Model v3", project.getDescription() );

        assertNull( "Has no parent project.", project.getParentProject() );

        assertNotNull( "Dependencies", project.getDependencies() );
        assertEquals( "Dependencies.size", 1, project.getDependencies().size() );

        Dependency dep = (Dependency) project.getDependencies().get( 0 );
        assertNotNull( dep );
        assertEquals( "dep.groupId", "org.codehaus.plexus", dep.getGroupId() );
        assertEquals( "dep.artifactId", "plexus-utils", dep.getArtifactId() );
        assertEquals( "dep.version", "1.0.4", dep.getVersion() );
    }
}
