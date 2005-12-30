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
import java.util.Iterator;
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

    private static final String SHA1 = "sha1";

    private static final String MD5 = "md5";

    private static final String CLASSES = "classes";

    private static final String PACKAGES = "packages";

    private static final String FILES = "files";

    protected ArtifactRepositoryIndex indexer;

    protected ArtifactFactory artifactFactory;

    protected ArtifactRepository repository;

    protected String indexPath;

    private RepositoryIndexSearcher repoSearcher;

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

        FileUtils.deleteDirectory( indexPath );
    }

    public void testIndexerExceptions()
        throws Exception
    {
        try
        {
            String notIndexDir = new File( "pom.xml" ).getAbsolutePath();
            indexer = (ArtifactRepositoryIndex) lookup( RepositoryIndex.ROLE, "artifact" );
            indexer.open( notIndexDir );
            fail( "Must throw exception on non-directory index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            //expected
        }

        try
        {
            String notIndexDir = new File( "" ).getAbsolutePath();
            indexer = (ArtifactRepositoryIndex) lookup( RepositoryIndex.ROLE, "artifact" );
            indexer.open( notIndexDir );
            fail( "Must throw an exception on a non-index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            //expected
        }

        //indexer = (ArtifactRepositoryIndex) factory.getArtifactRepositoryIndexer( indexPath, repository );
        //indexer.close();
        indexer = (ArtifactRepositoryIndex) lookup( RepositoryIndex.ROLE, "artifact" );
        Artifact artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );

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

        indexer.open( indexPath );

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

    public void testIndex()
        throws Exception
    {
        //indexer = (ArtifactRepositoryIndex) factory.getArtifactRepositoryIndexer( indexPath, repository );
        indexer = (ArtifactRepositoryIndex) lookup( RepositoryIndex.ROLE, "artifact" );
        indexer.open( indexPath );

        Artifact artifact = getArtifact( "org.apache.maven", "maven-artifact", "2.0.1" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.indexArtifact( artifact );

        artifact = getArtifact( "org.apache.maven", "maven-model", "2.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.indexArtifact( artifact );

        indexer.optimize();
        indexer.close();

        indexer.open( indexPath );
        artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.index( artifact );
        indexer.close();
    }

    public void testSearch()
        throws Exception
    {
        indexer = (ArtifactRepositoryIndex) lookup( RepositoryIndex.ROLE, "artifact" );
        indexer.open( getTestPath( "src/test/index" ) );

        //repoSearcher = new ArtifactRepositoryIndexSearcher( indexer, indexPath, repository );
        repoSearcher = (RepositoryIndexSearcher) lookup( RepositoryIndexSearcher.ROLE, "artifact" );

        List artifacts = repoSearcher.search( indexer, "test", GROUPID );
        assertEquals( 1, artifacts.size() );

        artifacts = repoSearcher.search( indexer, "test", ARTIFACTID );
        assertEquals( 1, artifacts.size() );

        artifacts = repoSearcher.search( indexer, "1.0", VERSION );
        assertEquals( 1, artifacts.size() );

        artifacts = repoSearcher.search( indexer, "App", CLASSES );
        assertEquals( 1, artifacts.size() );

        artifacts = repoSearcher.search( indexer, "groupId", PACKAGES );
        assertEquals( 1, artifacts.size() );

        artifacts = repoSearcher.search( indexer, "pom.xml", FILES );
        assertEquals( 3, artifacts.size() );

        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            File f = artifact.getFile();
            //assertNotNull( f );
            //assertTrue( f.exists() );
        }

        artifacts = repoSearcher.search( indexer, "org.apache.maven", GROUPID );
        assertEquals( 2, artifacts.size() );

        artifacts = repoSearcher.search( indexer, "maven-artifact", ARTIFACTID );
        assertEquals( 1, artifacts.size() );

        artifacts = repoSearcher.search( indexer, "2", VERSION );
        assertEquals( 2, artifacts.size() );
    }

    protected Artifact getArtifact( String groupId, String artifactId, String version )
        throws Exception
    {
        if ( artifactFactory == null )
        {
            artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        }

        return artifactFactory.createBuildArtifact( groupId, artifactId, version, "jar" );
    }
}
