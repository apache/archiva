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
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
        MetadataRepositoryIndex indexer = factory.createMetadataRepositoryIndex( indexPath, repository );

        RepositoryMetadata repoMetadata = new GroupRepositoryMetadata( "org.apache.maven" );
        repoMetadata.setMetadata( readMetadata( repoMetadata ) );
        indexer.index( repoMetadata );
        indexer.optimize();
        indexer.close();

        repoMetadata = new ArtifactRepositoryMetadata( getArtifact( "org.apache.maven", "maven-artifact", "2.0.1" ) );
        repoMetadata.setMetadata( readMetadata( repoMetadata ) );
        indexer.index( repoMetadata );
        indexer.optimize();
        indexer.close();

        repoMetadata =
            new SnapshotArtifactRepositoryMetadata( getArtifact( "org.apache.maven", "maven-artifact", "2.0.1" ) );
        repoMetadata.setMetadata( readMetadata( repoMetadata ) );
        indexer.index( repoMetadata );
        indexer.optimize();
        indexer.close();

        repoMetadata = new GroupRepositoryMetadata( "test" );
        repoMetadata.setMetadata( readMetadata( repoMetadata ) );
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
        Query qry = new SinglePhraseQuery( RepositoryIndex.FLD_LASTUPDATE, "20051212044643" );
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
        assertEquals( 0, metadataList.size() );

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
        MetadataRepositoryIndex indexer = factory.createMetadataRepositoryIndex( indexPath, repository );
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
            indexer.deleteIfIndexed( new Object() );
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
        MetadataRepositoryIndex indexer = factory.createMetadataRepositoryIndex( indexPath, repository );

        RepositoryMetadata repoMetadata = new GroupRepositoryMetadata( "org.apache.maven" );
        repoMetadata.setMetadata( readMetadata( repoMetadata ) );
        indexer.deleteDocument( RepositoryIndex.FLD_ID, (String) repoMetadata.getKey() );

        RepositoryIndexSearcher repoSearcher = factory.createDefaultRepositoryIndexSearcher( indexer );
        Query qry = new SinglePhraseQuery( RepositoryIndex.FLD_ID, (String) repoMetadata.getKey() );
        List metadataList = repoSearcher.search( qry );
        assertEquals( 0, metadataList.size() );
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

    /**
     * Create RepositoryMetadata object.
     *
     * @return RepositoryMetadata
     */
    private Metadata readMetadata( RepositoryMetadata repoMetadata )
        throws RepositoryIndexSearchException
    {
        File file = new File( repository.getBasedir(), repository.pathOfRemoteRepositoryMetadata( repoMetadata ) );

        MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();

        FileReader reader = null;
        try
        {
            reader = new FileReader( file );
            return metadataReader.read( reader );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryIndexSearchException( "Unable to find metadata file: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryIndexSearchException( "Unable to read metadata file: " + e.getMessage(), e );
        }
        catch ( XmlPullParserException xe )
        {
            throw new RepositoryIndexSearchException( "Unable to parse metadata file: " + xe.getMessage(), xe );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }
}
