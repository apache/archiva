package org.apache.maven.repository.indexing;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.List;

/**
 *
 */
public class ArtifactRepositoryIndexingTest
    extends PlexusTestCase
{
    private static final String GROUPID = "groupId";

    private static final String ARTIFACTID = "artifactId";

    private static final String VERSION = "version";

    private static final String CLASSES = "classes";

    private static final String PACKAGES = "packages";

    private static final String FILES = "files";

    private ArtifactFactory artifactFactory;

    private ArtifactRepository repository;

    private String indexPath;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        File repositoryDirectory = getTestFile( "src/test/repository" );
        String repoDir = repositoryDirectory.toURL().toString();
        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        ArtifactRepositoryFactory repoFactory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
        repository = repoFactory.createArtifactRepository( "test", repoDir, layout, null, null );

        indexPath = "target/index";
    }

    public void testIndexerExceptions()
        throws Exception
    {
        ArtifactRepositoryIndex indexer;
        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );

        try
        {
            String notIndexDir = new File( "pom.xml" ).getAbsolutePath();
            indexer = factory.createArtifactRepositoryIndex( notIndexDir, repository );
            fail( "Must throw exception on non-directory index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            //expected
        }

        try
        {
            String notIndexDir = new File( "" ).getAbsolutePath();
            indexer = factory.createArtifactRepositoryIndex( notIndexDir, repository );
            fail( "Must throw an exception on a non-index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            //expected
        }

        Artifact artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );

        indexer = factory.createArtifactRepositoryIndex( indexPath, repository );
        indexer.close();
        
        try
        {
            indexer.indexArtifact( artifact );
            fail( "Must throw exception on add index with closed index." );
        }
        catch ( RepositoryIndexException e )
        {
            //expected
        }

        try
        {
            indexer.optimize();
            fail( "Must throw exception on optimize index with closed index." );
        }
        catch ( RepositoryIndexException e )
        {
            //expected
        }

        indexer = factory.createArtifactRepositoryIndex( indexPath, repository );

        try
        {
            indexer.index( "should fail" );
            fail( "Must throw exception on add non-Artifact object." );
        }
        catch ( RepositoryIndexException e )
        {
            //expected
        }

        indexer.close();
    }

    public void createTestIndex()
        throws Exception
    {
        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );

        Artifact artifact = getArtifact( "org.apache.maven", "maven-artifact", "2.0.1" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.indexArtifact( artifact );

        artifact = getArtifact( "org.apache.maven", "maven-model", "2.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.indexArtifact( artifact );

        artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.index( artifact );

        indexer.optimize();
        indexer.close();
    }

    public void testSearch()
        throws Exception
    {
        createTestIndex();

        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );
        RepositoryIndexSearcher repoSearcher = factory.createArtifactRepositoryIndexSearcher( indexer );

        List artifacts = repoSearcher.search( "test", GROUPID );
        assertEquals( 1, artifacts.size() );

        artifacts = repoSearcher.search( "test", ARTIFACTID );
        assertEquals( 1, artifacts.size() );

        artifacts = repoSearcher.search( "1.0", VERSION );
        assertEquals( 1, artifacts.size() );

        artifacts = repoSearcher.search( "App", CLASSES );
        assertEquals( 1, artifacts.size() );

        artifacts = repoSearcher.search( "groupId", PACKAGES );
        assertEquals( 1, artifacts.size() );

        artifacts = repoSearcher.search( "pom.xml", FILES );
        assertEquals( 3, artifacts.size() );

        artifacts = repoSearcher.search( "org.apache.maven", GROUPID );
        assertEquals( 2, artifacts.size() );

        artifacts = repoSearcher.search( "maven-artifact", ARTIFACTID );
        assertEquals( 1, artifacts.size() );

        artifacts = repoSearcher.search( "2", VERSION );
        assertEquals( 2, artifacts.size() );

        indexer.close();
    }

    private Artifact getArtifact( String groupId, String artifactId, String version )
        throws Exception
    {
        if ( artifactFactory == null )
        {
            artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        }

        return artifactFactory.createBuildArtifact( groupId, artifactId, version, "jar" );
    }

    protected void tearDown()
        throws Exception
    {
        repository = null;
        FileUtils.deleteDirectory( indexPath );

        super.tearDown();
    }
}
