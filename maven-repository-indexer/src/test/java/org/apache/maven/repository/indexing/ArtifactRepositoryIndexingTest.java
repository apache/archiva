package org.apache.maven.repository.indexing;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
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
 * @author Edwin Punzalan
 */
public class ArtifactRepositoryIndexingTest
    extends PlexusTestCase
{
    protected ArtifactRepositoryIndexer indexer;
    protected ArtifactFactory artifactFactory;
    protected ArtifactRepository repository;
    protected String indexPath;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        File repositoryDirectory = getTestFile( "src/test/repository" );
        String repoDir = repositoryDirectory.toURL().toString();

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        ArtifactRepositoryFactory repoFactory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
        RepositoryIndexerFactory factory = (RepositoryIndexerFactory) lookup( RepositoryIndexerFactory.ROLE );

        String indexPath = "target/index";
        repository = repoFactory.createArtifactRepository( "test", repoDir, layout, null, null );
        indexer = (ArtifactRepositoryIndexer) factory.getArtifactRepositoryIndexer( indexPath, repository );
        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
    }

    public void testIndex()
        throws Exception
    {
        Artifact artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.addArtifactIndex( artifact );
        //indexer.optimize();
        indexer.close();
    }

    protected Artifact getArtifact( String groupId, String artifactId, String version )
    {
        return artifactFactory.createBuildArtifact( groupId, artifactId, version, "jar" );
    }
}
