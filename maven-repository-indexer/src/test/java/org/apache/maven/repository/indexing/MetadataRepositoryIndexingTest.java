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
import org.apache.maven.artifact.repository.metadata.*;
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

    private static final String FLD_LASTUPDATE = "lastUpdate";

    private static final String FLD_PLUGINPREFIX = "pluginPrefix";

    private static final String GROUP_TYPE = "GROUP";

    private static final String ARTIFACT_TYPE = "ARTIFACT";

    private static final String SNAPSHOT_TYPE = "SNAPSHOT";

    private MetadataRepositoryIndex indexer;

    private ArtifactFactory artifactFactory;

    /**
     * Set up.
     * @throws Exception
     */
    public void setUp() throws Exception
    {
        super.setUp();
        File repositoryDirectory = getTestFile( "src/test/repository" );
        String repoDir = repositoryDirectory.toURL().toString();
        ArtifactRepositoryLayout layout = ( ArtifactRepositoryLayout ) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        ArtifactRepositoryFactory repoFactory = ( ArtifactRepositoryFactory ) lookup( ArtifactRepositoryFactory.ROLE );
        repository = repoFactory.createArtifactRepository( "test", repoDir, layout, null, null );

        indexPath = "target/index/metadata";
        FileUtils.deleteDirectory( indexPath );
    }

    /**
     * Tear down.
     * @throws Exception
     */
    public void tearDown() throws Exception
    {
        repository = null;
        super.tearDown();
    }

    /**
     * Create the test index.
     * @throws Exception
     */
    private void createTestIndex() throws Exception
    {
        RepositoryIndexingFactory factory = ( RepositoryIndexingFactory ) lookup( RepositoryIndexingFactory.ROLE );
        indexer = factory.createMetadataRepositoryIndex( indexPath, repository );

        RepositoryMetadata repoMetadata = getMetadata( "org.apache.maven", null, null, "maven-metadata.xml", GROUP_TYPE );
        indexer.index( repoMetadata );

        repoMetadata = getMetadata( "org.apache.maven", "maven-artifact", "2.0.1", "maven-metadata.xml", ARTIFACT_TYPE );
        indexer.index( repoMetadata );

        repoMetadata = getMetadata( "org.apache.maven", "maven-artifact", "2.0.1", "maven-metadata.xml", SNAPSHOT_TYPE );
        indexer.index( repoMetadata );

        indexer.optimize();
        indexer.close();
    }

    /**
     * Test the ArtifactRepositoryIndexSearcher using a single-phrase search.
     *
     * @throws Exception
     */
     public void testSearchSingle()
         throws Exception
     {
        createTestIndex();

        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        MetadataRepositoryIndex indexer = factory.createMetadataRepositoryIndex( indexPath, repository );
        RepositoryIndexSearcher repoSearcher = factory.createMetadataRepositoryIndexSearcher( indexer );

        // search last update
        org.apache.maven.repository.indexing.query.Query qry = new SinglePhraseQuery( FLD_LASTUPDATE, "20051212044643" );
        List metadataList = repoSearcher.search( qry );
        assertEquals( 1, metadataList.size() );
        for ( Iterator iter = metadataList.iterator(); iter.hasNext(); )
        {
            RepositoryMetadata repoMetadata = (RepositoryMetadata) iter.next();

            Metadata metadata = repoMetadata.getMetadata();
            Versioning versioning = metadata.getVersioning();
            assertEquals( "20051212044643", versioning.getLastUpdated() );
        }

        // search plugin prefix
        qry = new SinglePhraseQuery( FLD_PLUGINPREFIX, "org.apache.maven" );
        metadataList = repoSearcher.search( qry );
        assertEquals( 1, metadataList.size() );
        for ( Iterator iter = metadataList.iterator(); iter.hasNext(); )
        {
            RepositoryMetadata repoMetadata = (RepositoryMetadata) iter.next();
            Metadata metadata = repoMetadata.getMetadata();
            List plugins = metadata.getPlugins();
            for( Iterator it = plugins.iterator(); it.hasNext(); )
            {
                Plugin plugin = (Plugin) it.next();
                assertEquals( "org.apache.maven", plugin.getPrefix() );
            }
        }

        // search last update using INCLUSIVE Range Query
        Query qry1 = new SinglePhraseQuery( FLD_LASTUPDATE, "20051212000000" );
        Query qry2 = new SinglePhraseQuery( FLD_LASTUPDATE, "20051212235959");
        RangeQuery rQry = new RangeQuery( true );
        rQry.addQuery( qry1 );
        rQry.addQuery( qry2 );

        metadataList = repoSearcher.search( rQry );
        for ( Iterator iter = metadataList.iterator(); iter.hasNext(); )
        {
            RepositoryMetadata repoMetadata = (RepositoryMetadata) iter.next();
            Metadata metadata = repoMetadata.getMetadata();
            Versioning versioning = metadata.getVersioning();
            assertEquals( "20051212044643", versioning.getLastUpdated() );
        }

        // search last update using EXCLUSIVE Range Query
        qry1 = new SinglePhraseQuery( FLD_LASTUPDATE, "20051212000000" );
        qry2 = new SinglePhraseQuery( FLD_LASTUPDATE, "20051212044643");
        rQry = new RangeQuery( false );
        rQry.addQuery( qry1 );
        rQry.addQuery( qry2 );

        metadataList = repoSearcher.search( rQry );
        assertEquals( metadataList.size(), 0 );

        indexer.close();
     }

    /**
     * Create RepositoryMetadata object.
     *
     * @param groupId the groupId to be set
     * @param artifactId the artifactId to be set
     * @param version the version to be set
     * @param filename the name of the metadata file
     * @param metadataType the type of RepositoryMetadata object to be created (GROUP, ARTIFACT or SNAPSHOT)
     * @return RepositoryMetadata
     * @throws Exception
     */
    private RepositoryMetadata getMetadata( String groupId, String artifactId, String version, String filename, String metadataType)
        throws Exception
    {
        RepositoryMetadata repoMetadata = null;
        URL url;
        InputStream is = null;
        MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();

        //group metadata
        if( metadataType.equals( GROUP_TYPE ) )
        {
            url = new File( repository.getBasedir() + groupId.replace('.', '/') + "/" + filename ).toURL();
            is = url.openStream();
            repoMetadata = new GroupRepositoryMetadata(groupId);
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }
        //artifact metadata
        else if( metadataType.equals( ARTIFACT_TYPE ) )
        {
            url = new File( repository.getBasedir() + groupId.replace('.', '/') + "/" + artifactId + "/" + filename ).toURL();
            is = url.openStream();
            repoMetadata = new ArtifactRepositoryMetadata( getArtifact( groupId, artifactId, version ) );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }
        //snapshot/version metadata
        else if( metadataType.equals( SNAPSHOT_TYPE ) )
        {
            url = new File( repository.getBasedir() + groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + filename ).toURL();
            is = url.openStream();
            repoMetadata = new SnapshotArtifactRepositoryMetadata( getArtifact( groupId, artifactId, version ) );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }

        return repoMetadata;
    }


    /**
     * Create artifact object.
     * @param groupId the groupId of the artifact
     * @param artifactId the artifactId of the artifact
     * @param version the version of the artifact
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
