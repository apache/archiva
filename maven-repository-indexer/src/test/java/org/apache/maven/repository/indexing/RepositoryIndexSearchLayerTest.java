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
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
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
import java.util.Map;
import java.util.Set;

/**
 * <p/>
 * This class tests the RepositoryIndexSearchLayer.
 */
public class RepositoryIndexSearchLayerTest
    extends PlexusTestCase
{
    private ArtifactRepository repository;

    private ArtifactFactory artifactFactory;

    private File indexPath;

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

        indexPath = getTestFile( "target/index" );
        FileUtils.deleteDirectory( indexPath );
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

        artifact = getArtifact( "org.apache.maven", "maven-model", "2.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.indexArtifact( artifact );

        artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.indexArtifact( artifact );

        artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.indexArtifact( artifact );

        MetadataRepositoryIndex metaIndexer = factory.createMetadataRepositoryIndex( indexPath, repository );
        RepositoryMetadata repoMetadata = new GroupRepositoryMetadata( "org.apache.maven" );
        repoMetadata.setMetadata( readMetadata( repoMetadata ) );
        metaIndexer.indexMetadata( repoMetadata );

        repoMetadata = new ArtifactRepositoryMetadata( getArtifact( "org.apache.maven", "maven-artifact", "2.0.1" ) );
        repoMetadata.setMetadata( readMetadata( repoMetadata ) );
        metaIndexer.indexMetadata( repoMetadata );

        repoMetadata =
            new SnapshotArtifactRepositoryMetadata( getArtifact( "org.apache.maven", "maven-artifact", "2.0.1" ) );
        repoMetadata.setMetadata( readMetadata( repoMetadata ) );
        metaIndexer.indexMetadata( repoMetadata );

        repoMetadata = new GroupRepositoryMetadata( "test" );
        repoMetadata.setMetadata( readMetadata( repoMetadata ) );
        metaIndexer.indexMetadata( repoMetadata );

        metaIndexer.optimize();
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
        RepositoryIndexSearchLayer searchLayer = (RepositoryIndexSearchLayer) lookup( RepositoryIndexSearchLayer.ROLE );

        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );

        List returnList = searchLayer.searchGeneral( "org.apache.maven", indexer );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Map map = result.getFieldMatches();
            Set entries = map.entrySet();
            for ( Iterator it = entries.iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                if ( entry.getKey().equals( RepositoryIndex.FLD_PACKAGES ) )
                {
                    List packages = (List) entry.getValue();
                    for ( Iterator iterator = packages.iterator(); iterator.hasNext(); )
                    {
                        assertTrue( ( (String) iterator.next() ).indexOf( "org.apache.maven" ) != -1 );
                    }
                }
            }
        }

        //POM license urls
        returnList = searchLayer.searchGeneral( "http://www.apache.org/licenses/LICENSE-2.0.txt", indexer );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Map map = result.getFieldMatches();
            Set entries = map.entrySet();
            for ( Iterator it = entries.iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                if ( entry.getKey().equals( RepositoryIndex.FLD_LICENSE_URLS ) )
                {
                    List packages = (List) entry.getValue();
                    for ( Iterator iterator = packages.iterator(); iterator.hasNext(); )
                    {
                        assertEquals( "http://www.apache.org/licenses/LICENSE-2.0.txt", (String) iterator.next() );
                    }
                }
            }
        }

        //POM dependency
        returnList = searchLayer.searchGeneral( "org.codehaus.plexus:plexus-utils:1.0.5", indexer );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Map map = result.getFieldMatches();
            Set entries = map.entrySet();
            for ( Iterator it = entries.iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                if ( entry.getKey().equals( RepositoryIndex.FLD_DEPENDENCIES ) )
                {
                    List packages = (List) entry.getValue();
                    for ( Iterator iterator = packages.iterator(); iterator.hasNext(); )
                    {
                        assertEquals( "org.codehaus.plexus:plexus-utils:1.0.5", (String) iterator.next() );
                    }
                }
            }
        }

        // POM reporting plugin
        returnList = searchLayer.searchGeneral( "org.apache.maven.plugins:maven-checkstyle-plugin:2.0", indexer );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Map map = result.getFieldMatches();
            Set entries = map.entrySet();
            for ( Iterator it = entries.iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                if ( entry.getKey().equals( RepositoryIndex.FLD_PLUGINS_REPORT ) )
                {
                    List packages = (List) entry.getValue();
                    for ( Iterator iterator = packages.iterator(); iterator.hasNext(); )
                    {
                        assertEquals( "org.apache.maven.plugins:maven-checkstyle-plugin:2.0",
                                      (String) iterator.next() );
                    }
                }
            }
        }

        // POM build plugin
        returnList = searchLayer.searchGeneral( "org.codehaus.modello:modello-maven-plugin:2.0", indexer );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Map map = result.getFieldMatches();
            Set entries = map.entrySet();
            for ( Iterator it = entries.iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                if ( entry.getKey().equals( RepositoryIndex.FLD_PLUGINS_BUILD ) )
                {
                    List packages = (List) entry.getValue();
                    for ( Iterator iterator = packages.iterator(); iterator.hasNext(); )
                    {
                        assertEquals( "org.codehaus.modello:modello-maven-plugin:2.0", (String) iterator.next() );
                    }
                }
            }
        }

        //maven-artifact-2.0.1.jar MD5 checksum
        returnList = searchLayer.searchGeneral( "F5A934ABBBC70A33136D89A996B9D5C09F652766", indexer );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Map map = result.getFieldMatches();
            Set entries = map.entrySet();
            for ( Iterator it = entries.iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                if ( entry.getKey().equals( RepositoryIndex.FLD_MD5 ) )
                {
                    assertTrue(
                        ( (String) entry.getValue() ).indexOf( "F5A934ABBBC70A33136D89A996B9D5C09F652766" ) != -1 );
                }
            }
        }

        //maven-artifact-2.0.1.jar SHA1 checksum
        returnList = searchLayer.searchGeneral( "AE55D9B5720E11B6CF19FE1E31A42E51", indexer );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Map map = result.getFieldMatches();
            Set entries = map.entrySet();
            for ( Iterator it = entries.iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                if ( entry.getKey().equals( RepositoryIndex.FLD_SHA1 ) )
                {
                    assertTrue( ( (String) entry.getValue() ).indexOf( "AE55D9B5720E11B6CF19FE1E31A42E516" ) != -1 );
                }
            }
        }

        //packaging jar
        returnList = searchLayer.searchGeneral( "jar", indexer );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Map map = result.getFieldMatches();
            Set entries = map.entrySet();
            for ( Iterator it = entries.iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                if ( entry.getKey().equals( RepositoryIndex.FLD_PACKAGING ) )
                {
                    assertEquals( "jar", (String) entry.getValue() );
                }
            }
        }

        returnList = searchLayer.searchGeneral( "test", indexer );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            assertEquals( "test", result.getArtifact().getGroupId() );
        }

        returnList = searchLayer.searchGeneral( "test-artifactId", indexer );
        for ( Iterator iter = returnList.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            assertEquals( "test-artifactId", result.getArtifact().getArtifactId() );
        }

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
