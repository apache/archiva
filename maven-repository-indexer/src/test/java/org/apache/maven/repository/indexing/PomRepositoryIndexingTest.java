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
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.digest.DefaultDigester;
import org.apache.maven.repository.digest.Digester;
import org.apache.maven.repository.indexing.query.CompoundQuery;
import org.apache.maven.repository.indexing.query.Query;
import org.apache.maven.repository.indexing.query.SinglePhraseQuery;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Edwin Punzalan
 */
public class PomRepositoryIndexingTest
    extends PlexusTestCase
{
    private ArtifactRepository repository;

    private ArtifactFactory artifactFactory;

    private String indexPath;

    private Digester digester;

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


    public void testIndexerExceptions()
        throws Exception
    {
        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        Model pom = getPom( "test", "test-artifactId", "1.0" );

        try
        {
            String notIndexDir = new File( "pom.xml" ).getAbsolutePath();
            PomRepositoryIndex indexer = factory.createPomRepositoryIndex( notIndexDir, repository );
            indexer.indexPom( pom );
            fail( "Must throw exception on non-directory index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            assertTrue( true );
        }

        try
        {
            String notIndexDir = new File( "" ).getAbsolutePath();
            PomRepositoryIndex indexer = factory.createPomRepositoryIndex( notIndexDir, repository );
            indexer.indexPom( pom );
            fail( "Must throw an exception on a non-index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            assertTrue( true );
        }

        PomRepositoryIndex indexer = factory.createPomRepositoryIndex( indexPath, repository );
        try
        {
            indexer.deleteIfIndexed( new Object() );
            fail( "Must throw exception when the passed object is not of type model." );
        }
        catch ( RepositoryIndexException e )
        {
            assertTrue( true );
        }
    }

    /**
     * Test the PomRepositoryIndex with DefaultRepositoryIndexSearcher using a single-phrase search.
     *
     * @throws Exception
     */
    public void testSearchSingle()
        throws Exception
    {
        createTestIndex();

        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        RepositoryIndexSearchLayer repoSearchLayer =
            (RepositoryIndexSearchLayer) lookup( RepositoryIndexSearchLayer.ROLE );

        PomRepositoryIndex indexer = factory.createPomRepositoryIndex( indexPath, repository );

        // search version
        Query qry = new SinglePhraseQuery( RepositoryIndex.FLD_VERSION, "1.0" );
        List artifactList = repoSearchLayer.searchAdvanced( qry, indexer );
        assertEquals( 1, artifactList.size() );
        for ( Iterator iter = artifactList.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "1.0", artifact.getVersion() );
        }

        // search group id
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_GROUPID, "org.apache.maven" );
        artifactList = repoSearchLayer.searchAdvanced( qry, indexer );
        assertEquals( 2, artifactList.size() );
        Iterator artifacts = artifactList.iterator();
        if ( artifacts.hasNext() )
        {
            SearchResult result = (SearchResult) artifacts.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "org.apache.maven", artifact.getGroupId() );
        }

        // search artifact id
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_ARTIFACTID, "maven-artifact" );
        artifactList = repoSearchLayer.searchAdvanced( qry, indexer );
        assertEquals( 1, artifactList.size() );
        for ( artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            SearchResult result = (SearchResult) artifacts.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
        }

        // search version
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_VERSION, "2" );
        artifactList = repoSearchLayer.searchAdvanced( qry, indexer );
        assertEquals( 2, artifactList.size() );
        for ( artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            SearchResult result = (SearchResult) artifacts.next();
            Artifact artifact = result.getArtifact();
            assertTrue( artifact.getVersion().indexOf( "2" ) != -1 );
        }

        // search packaging
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_PACKAGING, "jar" );
        artifactList = repoSearchLayer.searchAdvanced( qry, indexer );
        assertEquals( 3, artifactList.size() );
        for ( artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            SearchResult result = (SearchResult) artifacts.next();
            Map map = result.getFieldMatches();
            Set mapEntry = map.entrySet();
            assertEquals( "jar", (String) map.get( RepositoryIndex.FLD_PACKAGING ) );
        }

        //search license url
        qry =
            new SinglePhraseQuery( RepositoryIndex.FLD_LICENSE_URLS, "http://www.apache.org/licenses/LICENSE-2.0.txt" );
        artifactList = repoSearchLayer.searchAdvanced( qry, indexer );
        assertEquals( 2, artifactList.size() );
        for ( artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            SearchResult result = (SearchResult) artifacts.next();
            Map map = result.getFieldMatches();
            List matches = (List) map.get( RepositoryIndex.FLD_LICENSE_URLS );
            for ( Iterator it = matches.iterator(); it.hasNext(); )
            {
                assertEquals( "http://www.apache.org/licenses/LICENSE-2.0.txt", (String) it.next() );
            }
        }

        //search dependencies
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_DEPENDENCIES, "org.codehaus.plexus:plexus-utils:1.0.5" );
        artifactList = repoSearchLayer.searchAdvanced( qry, indexer );
        assertEquals( 2, artifactList.size() );
        for ( artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            SearchResult result = (SearchResult) artifacts.next();
            Map map = result.getFieldMatches();
            boolean depFound = false;
            List list = (List) map.get( RepositoryIndex.FLD_DEPENDENCIES );
            Iterator dependencies = list.iterator();
            while ( dependencies.hasNext() )
            {
                String dep = (String) dependencies.next();
                if ( "org.codehaus.plexus:plexus-utils:1.0.5".equals( dep ) )
                {
                    depFound = true;
                    break;
                }
            }
            assertTrue( "Searched dependency not found.", depFound );
        }

        //search build plugin
        qry =
            new SinglePhraseQuery( RepositoryIndex.FLD_PLUGINS_BUILD, "org.codehaus.modello:modello-maven-plugin:2.0" );
        artifactList = repoSearchLayer.searchAdvanced( qry, indexer );
        assertEquals( 1, artifactList.size() );
        for ( artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            SearchResult result = (SearchResult) artifacts.next();
            Map map = result.getFieldMatches();
            List list = (List) map.get( RepositoryIndex.FLD_PLUGINS_BUILD );
            Iterator plugins = list.iterator();
            boolean found = false;
            while ( plugins.hasNext() )
            {
                String plugin = (String) plugins.next();
                if ( "org.codehaus.modello:modello-maven-plugin:2.0".equals( plugin ) )
                {
                    found = true;
                    break;
                }
            }
            assertTrue( "Searched plugin not found.", found );
        }

        //search reporting plugin
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_PLUGINS_REPORT,
                                     "org.apache.maven.plugins:maven-checkstyle-plugin:2.0" );
        artifactList = repoSearchLayer.searchAdvanced( qry, indexer );
        assertEquals( 1, artifactList.size() );
        for ( artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            SearchResult result = (SearchResult) artifacts.next();
            Map map = result.getFieldMatches();
            List list = (List) map.get( RepositoryIndex.FLD_PLUGINS_REPORT );
            Iterator plugins = list.iterator();
            boolean found = false;
            while ( plugins.hasNext() )
            {
                String plugin = (String) plugins.next();
                if ( "org.apache.maven.plugins:maven-checkstyle-plugin:2.0".equals( plugin ) )
                {
                    found = true;
                    break;
                }
            }
            assertTrue( "Searched report plugin not found.", found );
        }

        // search sha1 checksum
        Artifact artifact = getArtifact( "org.apache.maven", "maven-model", "2.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        String sha1 = digester.createChecksum( artifact.getFile(), Digester.SHA1 );

        qry = new SinglePhraseQuery( RepositoryIndex.FLD_SHA1, sha1.trim() );
        artifactList = repoSearchLayer.searchAdvanced( qry, indexer );
        assertEquals( 1, artifactList.size() );
        for ( artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            SearchResult result = (SearchResult) artifacts.next();
            Artifact artifact2 = result.getArtifact();
            String sha1Tmp = digester.createChecksum( getPomFile( artifact2 ), Digester.SHA1 );
            assertEquals( sha1, sha1Tmp );
        }

        // search md5 checksum
        String md5 = digester.createChecksum( getPomFile( artifact ), Digester.MD5 );
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_MD5, md5.trim() );
        artifactList = repoSearchLayer.searchAdvanced( qry, indexer );
        assertEquals( 1, artifactList.size() );
        for ( artifacts = artifactList.iterator(); artifacts.hasNext(); )
        {
            SearchResult result = (SearchResult) artifacts.next();
            Artifact artifact2 = result.getArtifact();
            String md5Tmp = digester.createChecksum( getPomFile( artifact2 ), Digester.MD5 );
            assertEquals( md5, md5Tmp );
        }

        indexer.close();
    }

    /**
     * Test the PomRepositoryIndex with DefaultRepositoryIndexSearcher using compound search (AND, OR).
     *
     * @throws Exception
     */
    public void testSearchCompound()
        throws Exception
    {
        createTestIndex();

        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        RepositoryIndexSearchLayer repoSearchLayer =
            (RepositoryIndexSearchLayer) lookup( RepositoryIndexSearchLayer.ROLE );

        PomRepositoryIndex indexer = factory.createPomRepositoryIndex( indexPath, repository );

        // Criteria 1: required query
        // ex. artifactId=maven-artifact AND groupId=org.apache.maven
        Query qry1 = new SinglePhraseQuery( RepositoryIndex.FLD_ARTIFACTID, "maven-artifact" );
        Query qry2 = new SinglePhraseQuery( RepositoryIndex.FLD_GROUPID, "org.apache.maven" );
        CompoundQuery rQry = new CompoundQuery();
        rQry.and( qry1 );
        rQry.and( qry2 );

        List artifacts = repoSearchLayer.searchAdvanced( rQry, indexer );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
            assertEquals( "org.apache.maven", artifact.getGroupId() );
        }

        // Criteria 2: nested required query
        // ex. (artifactId=maven-artifact AND groupId=org.apache.maven) OR
        // version=2.0.3
        Query qry3 = new SinglePhraseQuery( RepositoryIndex.FLD_VERSION, "2.0.3" );
        CompoundQuery oQry = new CompoundQuery();
        oQry.and( rQry );
        oQry.or( qry3 );

        artifacts = repoSearchLayer.searchAdvanced( oQry, indexer );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
            assertEquals( "org.apache.maven", artifact.getGroupId() );
        }

        // Criteria 3: nested required query
        // ex. (artifactId=maven-artifact AND groupId=org.apache.maven) AND
        // (version=2.0.3 OR version=2.0.1)
        // AND (name=maven-artifact-2.0.1.jar OR name=maven-artifact)
        Query qry4 = new SinglePhraseQuery( RepositoryIndex.FLD_VERSION, "2.0.1" );
        oQry = new CompoundQuery();
        oQry.or( qry3 );
        oQry.or( qry4 );

        CompoundQuery oQry5 = new CompoundQuery();
        Query qry9 =
            new SinglePhraseQuery( RepositoryIndex.FLD_DEPENDENCIES, "org.codehaus.plexus:plexus-utils:1.0.5" );
        Query qry10 = new SinglePhraseQuery( RepositoryIndex.FLD_DEPENDENCIES,
                                             "org.codehaus.plexus:plexus-container-defualt:1.0-alpha-9" );
        oQry5.or( qry9 );
        oQry5.or( qry10 );

        CompoundQuery rQry2 = new CompoundQuery();
        rQry2.or( oQry );
        rQry2.and( rQry );
        rQry2.and( oQry5 );

        artifacts = repoSearchLayer.searchAdvanced( rQry2, indexer );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
            assertEquals( "org.apache.maven", artifact.getGroupId() );
            assertEquals( "2.0.1", artifact.getVersion() );
        }

        // Criteria 4: nested required query
        // ex. [(artifactId=maven-artifact AND groupId=org.apache.maven) AND
        // (version=2.0.3 OR version=2.0.1)
        // AND (name=maven-artifact-2.0.1.jar OR name=maven-artifact)]
        // OR [(artifactId=sample AND groupId=test)]
        CompoundQuery rQry3 = new CompoundQuery();
        Query qry5 = new SinglePhraseQuery( RepositoryIndex.FLD_ARTIFACTID, "sample" );
        Query qry6 = new SinglePhraseQuery( RepositoryIndex.FLD_GROUPID, "test" );
        rQry3.and( qry5 );
        rQry3.and( qry6 );
        CompoundQuery oQry2 = new CompoundQuery();
        oQry2.and( rQry2 );
        oQry2.and( rQry3 );

        artifacts = repoSearchLayer.searchAdvanced( oQry2, indexer );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
            assertEquals( "org.apache.maven", artifact.getGroupId() );
            assertEquals( "2.0.1", artifact.getVersion() );
        }

        // Criteria 4: nested required query
        // ex. [(artifactId=maven-artifact AND groupId=org.apache.maven) AND
        // (version=2.0.3 OR version=2.0.1)
        // AND (name=maven-artifact-2.0.1.jar OR name=maven-artifact)] OR
        // [(artifactId=sample AND groupId=test)] OR
        // [(artifactId=sample2 AND groupId=test)]
        CompoundQuery rQry4 = new CompoundQuery();
        Query qry7 = new SinglePhraseQuery( RepositoryIndex.FLD_ARTIFACTID, "sample2" );
        Query qry8 = new SinglePhraseQuery( RepositoryIndex.FLD_GROUPID, "test" );
        rQry4.and( qry7 );
        rQry4.and( qry8 );
        oQry2.and( rQry4 );

        artifacts = repoSearchLayer.searchAdvanced( oQry2, indexer );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
            assertEquals( "org.apache.maven", artifact.getGroupId() );
        }

        indexer.close();
    }

    /**
     * Create an index that will be used for testing.
     * Indexing process: check if the object was already indexed [ checkIfIndexed(Object) ], open the index [ open() ],
     * index the object [ index(Object) ], optimize the index [ optimize() ] and close the index [ close() ].
     *
     * @throws Exception
     */
    private void createTestIndex()
        throws Exception
    {
        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        PomRepositoryIndex indexer = factory.createPomRepositoryIndex( indexPath, repository );

        Model pom = getPom( "org.apache.maven", "maven-artifact", "2.0.1" );
        indexer.indexPom( pom );
        indexer.optimize();
        indexer.close();

        pom = getPom( "org.apache.maven", "maven-model", "2.0" );
        indexer.indexPom( pom );
        indexer.optimize();
        indexer.close();

        pom = getPom( "test", "test-artifactId", "1.0" );
        indexer.indexPom( pom );
        indexer.optimize();
        indexer.close();

        pom = getPom( "test", "test-artifactId", "1.0" );
        indexer.indexPom( pom );
        indexer.optimize();
        indexer.close();
    }

    /**
     * Test delete of pom document from index.
     *
     * @throws Exception
     */
    public void testDeletePomDocument()
        throws Exception
    {
        createTestIndex();

        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        RepositoryIndexSearcher repoSearcher = (RepositoryIndexSearcher) lookup( RepositoryIndexSearcher.ROLE );

        PomRepositoryIndex indexer = factory.createPomRepositoryIndex( indexPath, repository );

        Model pom = getPom( "org.apache.maven", "maven-artifact", "2.0.1" );
        indexer.deleteDocument( RepositoryIndex.FLD_ID, RepositoryIndex.POM + pom.getId() );

        Query qry = new SinglePhraseQuery( RepositoryIndex.FLD_ID, RepositoryIndex.POM + pom.getId() );
        List artifactList = repoSearcher.search( qry, indexer );
        assertEquals( 0, artifactList.size() );
    }

    private Model getPom( String groupId, String artifactId, String version )
        throws Exception
    {
        Artifact artifact = getArtifact( groupId, artifactId, version );

        return getPom( artifact );
    }

    private Model getPom( Artifact artifact )
        throws Exception
    {
        File pomFile = getPomFile( artifact );

        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        return pomReader.read( new FileReader( pomFile ) );
    }

    private File getPomFile( Artifact artifact )
    {
        String path = new File( repository.getBasedir(), repository.pathOf( artifact ) ).getAbsolutePath();
        return new File( path.substring( 0, path.lastIndexOf( '.' ) ) + ".pom" );
    }

    private Artifact getArtifact( String groupId, String artifactId, String version )
        throws Exception
    {
        if ( artifactFactory == null )
        {
            artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        }

        return artifactFactory.createBuildArtifact( groupId, artifactId, version, "pom" );
    }

    protected void tearDown()
        throws Exception
    {
        repository = null;

        super.tearDown();
    }
}
