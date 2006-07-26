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

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.digest.DefaultDigester;
import org.apache.maven.repository.digest.Digester;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public class EclipseRepositoryIndexTest
    extends PlexusTestCase
{
    private ArtifactFactory artifactFactory;

    private ArtifactRepository repository;

    private File indexPath;

    private Digester digester;

    private long artifactFileTime;

    private static final long TIME_DIFFERENCE = 10000L;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        File repositoryDirectory = getTestFile( "src/test/repository" );
        String repoDir = repositoryDirectory.toURL().toString();
        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        ArtifactRepositoryFactory repoFactory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
        repository = repoFactory.createArtifactRepository( "test", repoDir, layout, null, null );
        digester = new DefaultDigester();

        indexPath = getTestFile( "target/index" );
        FileUtils.deleteDirectory( indexPath );
    }

    /**
     * Create an index that will be used for testing.
     * Indexing process: check if the object was already indexed [ checkIfIndexed(Object) ], open the index [ open() ],
     * index the object [ index(Object) ], optimize the index [ optimize() ] and close the index [ close() ].
     *
     * @throws Exception
     */
    private EclipseRepositoryIndex createTestIndex()
        throws Exception
    {
        EclipseRepositoryIndex indexer = new EclipseRepositoryIndex( indexPath, repository, new DefaultDigester() );

        List artifacts = new ArrayList();

        Artifact artifact = getArtifact( "org.apache.maven", "maven-artifact", "2.0.1" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        artifactFileTime = artifact.getFile().lastModified();
        artifacts.add( artifact );

        long historicTime = artifactFileTime - TIME_DIFFERENCE;

        artifact = getArtifact( "org.apache.maven", "maven-model", "2.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        artifact.getFile().setLastModified( historicTime );
        artifacts.add( artifact );

        artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        artifact.getFile().setLastModified( historicTime );
        artifacts.add( artifact );

        indexer.indexArtifacts( artifacts );

        artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        artifact.getFile().setLastModified( historicTime );
        indexer.indexArtifact( artifact );

        indexer.optimize();

        return indexer;
    }

    /**
     * Method for testing the exceptions thrown by ArtifactRepositoryIndex
     *
     * @throws Exception
     */
    public void testIndexerExceptions()
        throws Exception
    {
        Artifact artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );

        try
        {
            File notIndexDir = new File( "pom.xml" );
            EclipseRepositoryIndex indexer = new EclipseRepositoryIndex( notIndexDir, repository, digester );
            indexer.indexArtifact( artifact );
            fail( "Must throw exception on non-directory index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            assertTrue( true );
        }

        try
        {
            File notIndexDir = new File( "." );
            EclipseRepositoryIndex indexer = new EclipseRepositoryIndex( notIndexDir, repository, digester );
            indexer.indexArtifact( artifact );
            fail( "Must throw an exception on a non-index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            assertTrue( true );
        }
    }

    /**
     * Test the ArtifactRepositoryIndex using a single-phrase search.
     *
     * @throws Exception
     */
    public void testSearch()
        throws Exception
    {
        EclipseRepositoryIndex index = createTestIndex();

        IndexSearcher searcher = new IndexSearcher( index.getIndexPath().getAbsolutePath() );
        try
        {
            QueryParser parser = new QueryParser( "j", index.getAnalyzer() );
            Hits hits = searcher.search( parser.parse( "maven-artifact-2.0.1.jar" ) );

            assertEquals( "Total hits", 1, hits.length() );
            Document doc = hits.doc( 0 );
            assertEquals( "Check jar name", "maven-artifact-2.0.1.jar", doc.get( "j" ) );

            parser = new QueryParser( "s", index.getAnalyzer() );
            hits = searcher.search( parser.parse( "78377" ) );
            assertEquals( "Total hits", 1, hits.length() );

            doc = hits.doc( 0 );
            assertEquals( "Check jar name", "maven-artifact-2.0.1.jar", doc.get( "j" ) );

            parser = new QueryParser( "d", index.getAnalyzer() );
            hits = searcher.search(
                parser.parse( DateTools.timeToString( artifactFileTime, DateTools.Resolution.SECOND ) ) );

            assertEquals( "Total hits", 1, hits.length() );
            doc = hits.doc( 0 );
            assertEquals( "Check jar name", "maven-artifact-2.0.1.jar", doc.get( "j" ) );

            parser = new QueryParser( "m", index.getAnalyzer() );
            hits = searcher.search( parser.parse( "AE55D9B5720E11B6CF19FE1E31A42E51" ) );
            assertEquals( "Total hits", 1, hits.length() );

            doc = hits.doc( 0 );
            assertEquals( "Check jar name", "maven-artifact-2.0.1.jar", doc.get( "j" ) );

            parser = new QueryParser( "c", index.getAnalyzer() );
            hits = searcher.search( parser.parse( "MavenXpp3Reader" ) );

            assertEquals( "Total hits", 1, hits.length() );
            doc = hits.doc( 0 );
            assertEquals( "Check jar name", "maven-model-2.0.jar", doc.get( "j" ) );

            parser = new QueryParser( "j", index.getAnalyzer() );
            hits = searcher.search( parser.parse( "maven" ) );

            assertEquals( "Total hits", 2, hits.length() );
        }
        finally
        {
            searcher.close();
        }
    }

    /**
     * Method for creating artifact object
     *
     * @param groupId    the groupId of the artifact to be created
     * @param artifactId the artifactId of the artifact to be created
     * @param version    the version of the artifact to be created
     * @return Artifact object
     * @throws Exception
     */
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

        super.tearDown();
    }
}
