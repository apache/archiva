package org.apache.maven.repository.reporting;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

import org.codehaus.plexus.PlexusTestCase;

/**
 *
 */
public abstract class AbstractRepositoryQueryLayerTest
    extends PlexusTestCase
{
    protected ArtifactFactory artifactFactory;
    
    protected ArtifactRepository repository;
    
    protected CachedRepositoryQueryLayer queryLayer;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        File repositoryDirectory = getTestFile( "src/test/repository" );

        ArtifactFactory artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        repository =
            factory.createArtifactRepository( "test", repositoryDirectory.toURL().toString(), layout, null, null );

        queryLayer = new CachedRepositoryQueryLayer( repository );
    }

    public void testContainsArtifactTrue()
    {
        Artifact artifact = getArtifact( "groupId", "artifactId", "1.0-alpha-1" );
        
        assertTrue( "check artifact exists", queryLayer.containsArtifact( artifact ) );
    }

    public void testContainsArtifactFalse()
    {
        Artifact artifact = getArtifact( "groupId", "artifactId", "1.0-beta-1" );
        
        assertFalse( "check artifact exists", queryLayer.containsArtifact( artifact ) );
    }

    public void testContainsSnapshotArtifact()
    {
        
    }
    
    protected Artifact getArtifact( String groupId, String artifactId, String version )
    {
        return artifactFactory.createBuildArtifact( groupId, artifactId, version, "pom" );
    }
}
