package org.apache.maven.archiva.layer;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.List;

/**
 *
 */
public abstract class AbstractRepositoryQueryLayerTestCase
    extends PlexusTestCase
{
    private ArtifactFactory artifactFactory;

    protected ArtifactRepository repository;

    protected RepositoryQueryLayer queryLayer;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        File repositoryDirectory = getTestFile( "src/test/repositories/repository" );

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        repository =
            factory.createArtifactRepository( "test", repositoryDirectory.toURL().toString(), layout, null, null );
    }

    public void testContainsArtifactTrue()
    {
        Artifact artifact = getArtifact( "groupId", "artifactId", "1.0-alpha-1" );

        assertTrue( "check artifact", queryLayer.containsArtifact( artifact ) );
    }

    public void testContainsArtifactFalse()
    {
        Artifact artifact = getArtifact( "groupId", "artifactId", "1.0-beta-1" );

        assertFalse( "check non-existent artifact", queryLayer.containsArtifact( artifact ) );
    }

    public void testContainsSnapshotArtifactTrue()
    {
        Artifact artifact = getArtifact( "groupId", "snapshot-artifact", "1.0-alpha-1-20050611.202024-1" );
        assertTrue( "check for snapshot artifact", queryLayer.containsArtifact( artifact ) );
    }

    public void testContainsSnapshotArtifactFalse()
    {
        Artifact artifact = getArtifact( "groupId", "snapshot-artifact", "1.0-alpha-1-20050611.202024-2" );
        assertFalse( "check for non-existent snapshot artifact", queryLayer.containsArtifact( artifact ) );
    }

    public void testArtifactVersions()
        throws Exception
    {
        Artifact artifact = getArtifact( "groupId", "artifactId", "ignored" );

        List versions = queryLayer.getVersions( artifact );

        assertTrue( "check version 1.0-alpha-1", versions.contains( "1.0-alpha-1" ) );
        assertFalse( "check version 1.0-alpha-2", versions.contains( "1.0-alpha-2" ) );
    }

    public void testArtifactVersionsError()
    {
        Artifact artifact = getArtifact( "groupId", "none", "ignored" );

        try
        {
            queryLayer.getVersions( artifact );
            fail( "expected error not thrown" );
        }
        catch ( RepositoryQueryLayerException e )
        {
            //expected
        }
    }

    private Artifact getArtifact( String groupId, String artifactId, String version )
    {
        Artifact projectArtifact = artifactFactory.createProjectArtifact( groupId, artifactId, version );
        projectArtifact.isSnapshot();
        return projectArtifact;
    }
}
