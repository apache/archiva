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

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.repository.digest.DefaultDigester;
import org.apache.maven.repository.digest.Digester;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.util.List;
import java.util.Iterator;
import java.net.URL;

/**
 * @author Maria Odea Ching
 *         <p/>
 *         This class tests the GeneralRepositoryIndexSearcher.
 */
public class GeneralRepositoryIndexSearcherTest
    extends PlexusTestCase
{
    private ArtifactRepository repository;

    private ArtifactFactory artifactFactory;

    private Digester digester;

    private String indexPath;

    /**
     * Setup method
     *
     * @throws Exception
     */
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

        indexPath = "target/index";
        FileUtils.deleteDirectory( indexPath );
    }

    /**
     * Tear down method
     *
     * @throws Exception
     */
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    /**
     * Method for creating the index used for testing
     *
     * @throws Exception
     */
    private void createTestIndex()
        throws Exception
    {
        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );

        Artifact artifact = getArtifact( "org.apache.maven", "maven-artifact", "2.0.1" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.indexArtifact( artifact );
        indexer.optimize();
        indexer.close();

        artifact = getArtifact( "org.apache.maven", "maven-model", "2.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.indexArtifact( artifact );
        indexer.optimize();
        indexer.close();

        artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.indexArtifact( artifact );
        indexer.optimize();
        indexer.close();

        artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.indexArtifact( artifact );
        indexer.optimize();
        indexer.close();

        MetadataRepositoryIndex metaIndexer = factory.createMetadataRepositoryIndex( indexPath, repository );
        RepositoryMetadata repoMetadata =
            getMetadata( "org.apache.maven", null, null, "maven-metadata.xml", metaIndexer.GROUP_METADATA );
        metaIndexer.index( repoMetadata );
        metaIndexer.optimize();
        metaIndexer.close();

        repoMetadata = getMetadata( "org.apache.maven", "maven-artifact", "2.0.1", "maven-metadata.xml",
                                    metaIndexer.ARTIFACT_METADATA );
        metaIndexer.index( repoMetadata );
        metaIndexer.optimize();
        metaIndexer.close();

        repoMetadata = getMetadata( "org.apache.maven", "maven-artifact", "2.0.1", "maven-metadata.xml",
                                    metaIndexer.SNAPSHOT_METADATA );
        metaIndexer.index( repoMetadata );
        metaIndexer.optimize();
        metaIndexer.close();

        repoMetadata = getMetadata( "test", null, null, "maven-metadata.xml", metaIndexer.GROUP_METADATA );
        metaIndexer.index( repoMetadata );
        metaIndexer.optimize();
        metaIndexer.close();

        PomRepositoryIndex pomIndexer = factory.createPomRepositoryIndex( indexPath, repository );

        Model pom = getPom( "org.apache.maven", "maven-artifact", "2.0.1" );
        pomIndexer.indexPom( pom );
        pomIndexer.optimize();
        pomIndexer.close();

        pom = getPom( "org.apache.maven", "maven-model", "2.0" );
        pomIndexer.indexPom( pom );
        pomIndexer.optimize();
        pomIndexer.close();

        pom = getPom( "test", "test-artifactId", "1.0" );
        pomIndexer.indexPom( pom );
        pomIndexer.optimize();
        pomIndexer.close();

        pom = getPom( "test", "test-artifactId", "1.0" );
        pomIndexer.indexPom( pom );
        pomIndexer.optimize();
        pomIndexer.close();
    }

    /**
     * Method for testing the "query everything" searcher
     *
     * @throws Exception
     */
    public void testGeneralSearcher()
        throws Exception
    {
        createTestIndex();
        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );
        GeneralRepositoryIndexSearcher searcher = factory.createGeneralRepositoryIndexSearcher( indexer );

        List returnList = searcher.search( "org.apache.maven" );
        assertEquals( returnList.size(), 7 );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            Object obj = (Object) iter.next();
            if ( obj instanceof Artifact )
            {
                Artifact artifact = (Artifact) obj;
                assertEquals( artifact.getGroupId(), "org.apache.maven" );
            }
            else if ( obj instanceof RepositoryMetadata )
            {
                RepositoryMetadata repoMetadata = (RepositoryMetadata) obj;
                assertEquals( repoMetadata.getGroupId(), "org.apache.maven" );
            }
        }

        returnList = searcher.search( "test" );
        assertEquals( returnList.size(), 3 );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            Object obj = (Object) iter.next();
            if ( obj instanceof Artifact )
            {
                Artifact artifact = (Artifact) obj;
                assertEquals( artifact.getGroupId(), "test" );
            }
            else if ( obj instanceof RepositoryMetadata )
            {
                RepositoryMetadata repoMetadata = (RepositoryMetadata) obj;
                assertEquals( repoMetadata.getGroupId(), "test" );
            }
        }

        returnList = searcher.search( "artifact" );
        assertEquals( returnList.size(), 4 );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            Object obj = (Object) iter.next();
            if ( obj instanceof Artifact )
            {
                Artifact artifact = (Artifact) obj;
                assertEquals( artifact.getArtifactId(), "maven-artifact" );
            }
            else if ( obj instanceof RepositoryMetadata )
            {
                RepositoryMetadata repoMetadata = (RepositoryMetadata) obj;
                assertEquals( repoMetadata.getArtifactId(), "maven-artifact" );
            }
        }
    }

    /**
     * Method for creating RepositoryMetadata object
     *
     * @param groupId      the groupId to be set
     * @param artifactId   the artifactId to be set
     * @param version      the version to be set
     * @param filename     the name of the metadata file
     * @param metadataType the type of RepositoryMetadata object to be created (GROUP, ARTIFACT or SNAPSHOT)
     * @return RepositoryMetadata
     * @throws Exception
     */
    private RepositoryMetadata getMetadata( String groupId, String artifactId, String version, String filename,
                                            String metadataType )
        throws Exception
    {
        RepositoryMetadata repoMetadata = null;
        URL url;
        InputStream is = null;
        MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();

        //group metadata
        if ( metadataType.equals( MetadataRepositoryIndex.GROUP_METADATA ) )
        {
            url = new File( repository.getBasedir() + groupId.replace( '.', '/' ) + "/" + filename ).toURL();
            is = url.openStream();
            repoMetadata = new GroupRepositoryMetadata( groupId );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }
        //artifact metadata
        else if ( metadataType.equals( MetadataRepositoryIndex.ARTIFACT_METADATA ) )
        {
            url = new File(
                repository.getBasedir() + groupId.replace( '.', '/' ) + "/" + artifactId + "/" + filename ).toURL();
            is = url.openStream();
            repoMetadata = new ArtifactRepositoryMetadata( getArtifact( groupId, artifactId, version ) );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }
        //snapshot/version metadata
        else if ( metadataType.equals( MetadataRepositoryIndex.SNAPSHOT_METADATA ) )
        {
            url = new File( repository.getBasedir() + groupId.replace( '.', '/' ) + "/" + artifactId + "/" + version +
                "/" + filename ).toURL();
            is = url.openStream();
            repoMetadata = new SnapshotArtifactRepositoryMetadata( getArtifact( groupId, artifactId, version ) );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }

        return repoMetadata;
    }

    /**
     * Method for creating Artifact object
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

    /**
     * Method for creating a Model object given the groupId, artifactId and version
     *
     * @param groupId    the groupId of the model to be created
     * @param artifactId the artifactId of the model to be created
     * @param version    the version of the model to be created
     * @return Model object
     * @throws Exception
     */
    private Model getPom( String groupId, String artifactId, String version )
        throws Exception
    {
        Artifact artifact = getArtifact( groupId, artifactId, version );

        return getPom( artifact );
    }

    /**
     * Method for creating a Model object given an artifact
     *
     * @param artifact the artifact to be created a Model object for
     * @return Model object
     * @throws Exception
     */
    private Model getPom( Artifact artifact )
        throws Exception
    {
        File pomFile = getPomFile( artifact );

        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        return pomReader.read( new FileReader( pomFile ) );
    }

    /**
     * Method for creating a pom file
     *
     * @param artifact
     * @return File
     */
    private File getPomFile( Artifact artifact )
    {
        String path = new File( repository.getBasedir(), repository.pathOf( artifact ) ).getAbsolutePath();
        return new File( path.substring( 0, path.lastIndexOf( '.' ) ) + ".pom" );
    }

}
