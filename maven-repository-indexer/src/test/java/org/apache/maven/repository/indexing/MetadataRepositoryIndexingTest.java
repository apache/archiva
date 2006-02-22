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
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.repository.indexing.query.Query;
import org.apache.maven.repository.indexing.query.RangeQuery;
import org.apache.maven.repository.indexing.query.SinglePhraseQuery;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

/**
 * This class tests the MetadataRepositoryIndex.
 */
public class MetadataRepositoryIndexingTest
    extends PlexusTestCase
{
    private ArtifactRepository repository;

    private String indexPath;

    private MetadataRepositoryIndex indexer;

    private ArtifactFactory artifactFactory;

    /**
     * Set up.
     *
     * @throws Exception
     */
    public void setUp()
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

    /**
     * Tear down.
     *
     * @throws Exception
     */
    public void tearDown()
        throws Exception
    {
        repository = null;
        super.tearDown();
    }

    /**
     * Create the test index.
     * Indexing process: check if the object was already indexed [ checkIfIndexed(Object) ], open the index [ open() ],
     * index the object [ index(Object) ], optimize the index [ optimize() ] and close the index [ close() ].
     *
     * @throws Exception
     */
    private void createTestIndex()
        throws Exception
    {
        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        indexer = factory.createMetadataRepositoryIndex( indexPath, repository );

        RepositoryMetadata repoMetadata =
            getMetadata( "org.apache.maven", null, null, "maven-metadata.xml", MetadataRepositoryIndex.GROUP_METADATA );
        indexer.index( repoMetadata );
        indexer.optimize();
        indexer.close();

        repoMetadata = getMetadata( "org.apache.maven", "maven-artifact", "2.0.1", "maven-metadata.xml",
                                    MetadataRepositoryIndex.ARTIFACT_METADATA );
        indexer.index( repoMetadata );
        indexer.optimize();
        indexer.close();

        repoMetadata = getMetadata( "org.apache.maven", "maven-artifact", "2.0.1", "maven-metadata.xml",
                                    MetadataRepositoryIndex.SNAPSHOT_METADATA );
        indexer.index( repoMetadata );
        indexer.optimize();
        indexer.close();

        repoMetadata = getMetadata( "test", null, null, "maven-metadata.xml", MetadataRepositoryIndex.GROUP_METADATA );
        indexer.index( repoMetadata );
        indexer.optimize();
        indexer.close();
    }

    /**
     * Test the ArtifactRepositoryIndex using a single-phrase search.
     *
     * @throws Exception
     */
    public void testSearch()
        throws Exception
    {
        createTestIndex();

        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        MetadataRepositoryIndex indexer = factory.createMetadataRepositoryIndex( indexPath, repository );
        //RepositoryIndexSearcher repoSearchLayer = factory.createDefaultRepositoryIndexSearcher( indexer );
        RepositoryIndexSearchLayer repoSearchLayer = factory.createRepositoryIndexSearchLayer( indexer );

        // search last update
        org.apache.maven.repository.indexing.query.Query qry =
            new SinglePhraseQuery( RepositoryIndex.FLD_LASTUPDATE, "20051212044643" );
        List metadataList = repoSearchLayer.searchAdvanced( qry );
        //assertEquals( 1, metadataList.size() );
        for ( Iterator iter = metadataList.iterator(); iter.hasNext(); )
        {
            RepositoryIndexSearchHit hit = (RepositoryIndexSearchHit) iter.next();
            if ( hit.isMetadata() )
            {
                RepositoryMetadata repoMetadata = (RepositoryMetadata) hit.getObject();
                Metadata metadata = repoMetadata.getMetadata();
                Versioning versioning = metadata.getVersioning();
                assertEquals( "20051212044643", versioning.getLastUpdated() );
            }
        }

        // search plugin prefix
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_PLUGINPREFIX, "org.apache.maven" );
        metadataList = repoSearchLayer.searchAdvanced( qry );
        //assertEquals( 1, metadataList.size() );
        for ( Iterator iter = metadataList.iterator(); iter.hasNext(); )
        {
            RepositoryIndexSearchHit hit = (RepositoryIndexSearchHit) iter.next();
            if ( hit.isMetadata() )
            {
                RepositoryMetadata repoMetadata = (RepositoryMetadata) hit.getObject();
                Metadata metadata = repoMetadata.getMetadata();
                List plugins = metadata.getPlugins();
                for ( Iterator it = plugins.iterator(); it.hasNext(); )
                {
                    Plugin plugin = (Plugin) it.next();
                    assertEquals( "org.apache.maven", plugin.getPrefix() );
                }
            }
        }

        // search last update using INCLUSIVE Range Query
        Query qry1 = new SinglePhraseQuery( RepositoryIndex.FLD_LASTUPDATE, "20051212000000" );
        Query qry2 = new SinglePhraseQuery( RepositoryIndex.FLD_LASTUPDATE, "20051212235959" );
        RangeQuery rQry = new RangeQuery( true );
        rQry.addQuery( qry1 );
        rQry.addQuery( qry2 );

        metadataList = repoSearchLayer.searchAdvanced( rQry );
        for ( Iterator iter = metadataList.iterator(); iter.hasNext(); )
        {
            RepositoryIndexSearchHit hit = (RepositoryIndexSearchHit) iter.next();
            if ( hit.isMetadata() )
            {
                RepositoryMetadata repoMetadata = (RepositoryMetadata) hit.getObject();
                Metadata metadata = repoMetadata.getMetadata();
                Versioning versioning = metadata.getVersioning();
                assertEquals( "20051212044643", versioning.getLastUpdated() );
            }
        }

        // search last update using EXCLUSIVE Range Query
        qry1 = new SinglePhraseQuery( RepositoryIndex.FLD_LASTUPDATE, "20051212000000" );
        qry2 = new SinglePhraseQuery( RepositoryIndex.FLD_LASTUPDATE, "20051212044643" );
        rQry = new RangeQuery( false );
        rQry.addQuery( qry1 );
        rQry.addQuery( qry2 );

        metadataList = repoSearchLayer.searchAdvanced( rQry );
        assertEquals( metadataList.size(), 0 );

        indexer.close();
    }

    /**
     * Test the exceptions thrown by MetadataRepositoryIndex.
     *
     * @throws Exception
     */
    public void testExceptions()
        throws Exception
    {
        //test when the object passed in the index(..) method is not a RepositoryMetadata instance
        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        indexer = factory.createMetadataRepositoryIndex( indexPath, repository );
        try
        {
            Artifact artifact = getArtifact( "org.apache.maven", "maven-artifact", "2.0.1" );
            indexer.index( artifact );
            fail( "Must throw exception when the passed object is not a RepositoryMetadata object." );
            indexer.optimize();
            indexer.close();
        }
        catch ( RepositoryIndexException e )
        {
            assertTrue( true );
        }

        try
        {
            indexer.isIndexed( new Object() );
            fail( "Must throw exception when the passed object is not of type metadata." );
        }
        catch ( RepositoryIndexException e )
        {
            assertTrue( true );
        }
    }

    /**
     * Test delete of document from metadata index.
     *
     * @throws Exception
     */
    public void testDeleteMetadataDocument()
        throws Exception
    {
        createTestIndex();

        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        indexer = factory.createMetadataRepositoryIndex( indexPath, repository );

        RepositoryMetadata repoMetadata =
            getMetadata( "org.apache.maven", null, null, "maven-metadata.xml", MetadataRepositoryIndex.GROUP_METADATA );
        indexer.deleteDocument( RepositoryIndex.FLD_ID, (String) repoMetadata.getKey() );

        RepositoryIndexSearcher repoSearcher = factory.createDefaultRepositoryIndexSearcher( indexer );
        org.apache.maven.repository.indexing.query.Query qry =
            new SinglePhraseQuery( RepositoryIndex.FLD_ID, (String) repoMetadata.getKey() );
        List metadataList = repoSearcher.search( qry );
        assertEquals( metadataList.size(), 0 );
    }

    /**
     * Create RepositoryMetadata object.
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
     * Create artifact object.
     *
     * @param groupId    the groupId of the artifact
     * @param artifactId the artifactId of the artifact
     * @param version    the version of the artifact
     * @return Artifact
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
}
