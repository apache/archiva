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
import org.apache.maven.repository.digest.DefaultDigester;
import org.apache.maven.repository.digest.Digester;
import org.apache.maven.repository.digest.DigesterException;
import org.apache.maven.repository.indexing.query.CompoundQuery;
import org.apache.maven.repository.indexing.query.QueryTerm;
import org.apache.maven.repository.indexing.query.SingleTermQuery;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Edwin Punzalan
 */
public class ArtifactRepositoryIndexingTest
    extends PlexusTestCase
{
    private ArtifactFactory artifactFactory;

    private ArtifactRepository repository;

    private File indexPath;

    private Digester digester;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        File repositoryDirectory = getTestFile( "src/test/repository" );
        String repoDir = repositoryDirectory.toURI().toURL().toString();
        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        ArtifactRepositoryFactory repoFactory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
        repository = repoFactory.createArtifactRepository( "test", repoDir, layout, null, null );
        digester = new DefaultDigester();
        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        indexPath = getTestFile( "target/index" );
        FileUtils.deleteDirectory( indexPath );
    }

    public void testInheritedFields()
        throws Exception
    {
        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        Artifact artifact = createArtifact( "test.inherited", "test-inherited", "1.0.15", "pom" );

        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );
        indexer.indexArtifact( artifact );

        RepositoryIndexSearchLayer repoSearchLayer =
            (RepositoryIndexSearchLayer) lookup( RepositoryIndexSearchLayer.ROLE );

        // search version
        QueryTerm queryTerm = new QueryTerm( RepositoryIndex.FLD_VERSION, "1.0.15" );
        List artifactList = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 1, artifactList.size() );
        for ( Iterator iter = artifactList.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact a = result.getArtifact();
            assertEquals( "1.0.15", a.getVersion() );
        }

        // search group id
        queryTerm = new QueryTerm( RepositoryIndex.FLD_GROUPID, "test.inherited" );
        artifactList = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 1, artifactList.size() );
        Iterator artifacts = artifactList.iterator();
        if ( artifacts.hasNext() )
        {
            SearchResult result = (SearchResult) artifacts.next();
            Artifact a = result.getArtifact();
            assertEquals( "test.inherited", a.getGroupId() );
        }

    }

    /**
     * Method for testing the exceptions thrown by ArtifactRepositoryIndex
     *
     * @throws Exception
     */
    public void testIndexerExceptions()
        throws Exception
    {
        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );

        try
        {
            File notIndexDir = new File( "." );
            factory.createArtifactRepositoryIndex( notIndexDir, repository );
            fail( "Must throw an exception on a non-index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            assertTrue( true );
        }

        try
        {
            File notIndexDir = new File( "pom.xml" );
            factory.createArtifactRepositoryIndex( notIndexDir, repository );
            fail( "Must throw exception on non-directory index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            assertTrue( true );
        }
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
        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );

        List artifacts = new ArrayList();

        Artifact artifact = createArtifact( "org.apache.maven", "maven-artifact", "2.0.1" );
        artifacts.add( artifact );

        artifact = createArtifact( "org.apache.maven", "maven-model", "2.0" );
        artifacts.add( artifact );

        artifact = createArtifact( "test", "test-artifactId", "1.0" );
        artifacts.add( artifact );

        artifact = createArtifact( "org.apache.maven", "maven-artifact", "2.0.1", "pom" );
        artifacts.add( artifact );

        artifact = createArtifact( "org.apache.maven", "maven-model", "2.0", "pom" );
        artifacts.add( artifact );

        artifact = createArtifact( "test", "test-artifactId", "1.0", "pom" );
        artifacts.add( artifact );

        indexer.indexArtifacts( artifacts );
    }

    /**
     * Test the ArtifactRepositoryIndex using a single-phrase search.
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

        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );

        // search version
        QueryTerm queryTerm = new QueryTerm( RepositoryIndex.FLD_VERSION, "1.0" );
        List artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 2, artifacts.size() );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "1.0", artifact.getVersion() );
        }

        // search group id
        queryTerm = new QueryTerm( RepositoryIndex.FLD_GROUPID, "org.apache.maven" );
        artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 4, artifacts.size() );
        Iterator iter = artifacts.iterator();
        if ( iter.hasNext() )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "org.apache.maven", artifact.getGroupId() );
        }

        // search artifact id
        queryTerm = new QueryTerm( RepositoryIndex.FLD_ARTIFACTID, "maven-artifact" );
        artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 2, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
        }

        // search version
        queryTerm = new QueryTerm( RepositoryIndex.FLD_VERSION, "2" );
        artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 4, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertTrue( artifact.getVersion().indexOf( "2" ) != -1 );
        }

        // search classes
        queryTerm = new QueryTerm( RepositoryIndex.FLD_CLASSES, "App" );
        artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 1, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "test-artifactId", artifact.getArtifactId() );
        }

        // search packages
        queryTerm = new QueryTerm( RepositoryIndex.FLD_PACKAGES, "groupId" );
        artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 1, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "test-artifactId", artifact.getArtifactId() );
        }

        // search files
        queryTerm = new QueryTerm( RepositoryIndex.FLD_FILES, "pom.xml" );
        artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 3, artifacts.size() );
        iter = artifacts.iterator();
        if ( iter.hasNext() )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "test-artifactId", artifact.getArtifactId() );
        }

        // search packaging
        queryTerm = new QueryTerm( RepositoryIndex.FLD_PACKAGING, "jar" );
        artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 3, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Map map = result.getFieldMatches();
            assertEquals( "jar", (String) map.get( RepositoryIndex.FLD_PACKAGING ) );
        }

        //search license url
        queryTerm = new QueryTerm( RepositoryIndex.FLD_LICENSE_URLS, "http://www.apache.org/licenses/LICENSE-2.0.txt" );
        artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 2, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Map map = result.getFieldMatches();
            List matches = (List) map.get( RepositoryIndex.FLD_LICENSE_URLS );
            for ( Iterator it = matches.iterator(); it.hasNext(); )
            {
                assertEquals( "http://www.apache.org/licenses/LICENSE-2.0.txt", (String) it.next() );
            }
        }

        //search dependencies
        queryTerm = new QueryTerm( RepositoryIndex.FLD_DEPENDENCIES, "org.codehaus.plexus:plexus-utils:1.0.5" );
        artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 2, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
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
        queryTerm = new QueryTerm( RepositoryIndex.FLD_PLUGINS_BUILD, "org.codehaus.modello:modello-maven-plugin:2.0" );
        artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 1, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
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
        queryTerm =
            new QueryTerm( RepositoryIndex.FLD_PLUGINS_REPORT, "org.apache.maven.plugins:maven-checkstyle-plugin:2.0" );
        artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 1, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
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

        Artifact artifact = createArtifact( "org.apache.maven", "maven-model", "2.0" );

        confirmChecksum( artifact, Digester.SHA1, RepositoryIndex.FLD_SHA1, repoSearchLayer, indexer );
        confirmChecksum( artifact, Digester.MD5, RepositoryIndex.FLD_MD5, repoSearchLayer, indexer );

        artifact = createArtifact( "org.apache.maven", "maven-model", "2.0", "pom" );

        confirmChecksum( artifact, Digester.SHA1, RepositoryIndex.FLD_SHA1, repoSearchLayer, indexer );
        confirmChecksum( artifact, Digester.MD5, RepositoryIndex.FLD_MD5, repoSearchLayer, indexer );
    }

    private void confirmChecksum( Artifact artifact, String algorithm, String field,
                                  RepositoryIndexSearchLayer repoSearchLayer, ArtifactRepositoryIndex indexer )
        throws DigesterException, RepositoryIndexSearchException
    {
        String sha1 = digester.createChecksum( artifact.getFile(), algorithm );

        QueryTerm queryTerm = new QueryTerm( field, sha1.trim() );
        List artifacts = repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 1, artifacts.size() );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact2 = result.getArtifact();
            String sha1Tmp = digester.createChecksum( artifact2.getFile(), algorithm );
            assertEquals( sha1, sha1Tmp );
        }
    }

    /**
     * Test the ArtifactRepositoryIndex using compound search (AND, OR).
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

        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );

        // Criteria 1: required query
        // ex. artifactId=maven-artifact AND groupId=org.apache.maven
        QueryTerm qry1 = new QueryTerm( RepositoryIndex.FLD_ARTIFACTID, "maven-artifact" );
        QueryTerm qry2 = new QueryTerm( RepositoryIndex.FLD_GROUPID, "org.apache.maven" );
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
        QueryTerm qry3 = new QueryTerm( RepositoryIndex.FLD_VERSION, "2.0.3" );
        CompoundQuery oQry = new CompoundQuery();
        oQry.or( rQry );
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
        QueryTerm qry4 = new QueryTerm( RepositoryIndex.FLD_VERSION, "2.0.1" );
        oQry = new CompoundQuery();
        oQry.or( qry3 );
        oQry.or( qry4 );

        CompoundQuery oQry5 = new CompoundQuery();
        QueryTerm qry9 = new QueryTerm( RepositoryIndex.FLD_NAME, "maven-artifact-2.0.1.jar" );
        QueryTerm qry10 = new QueryTerm( RepositoryIndex.FLD_NAME, "maven-artifact" );
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

        CompoundQuery oQry6 = new CompoundQuery();
        QueryTerm qry11 = new QueryTerm( RepositoryIndex.FLD_DEPENDENCIES, "org.codehaus.plexus:plexus-utils:1.0.5" );
        QueryTerm qry12 = new QueryTerm( RepositoryIndex.FLD_DEPENDENCIES,
                                         "org.codehaus.plexus:plexus-container-defualt:1.0-alpha-9" );
        oQry6.or( qry11 );
        oQry6.or( qry12 );

        CompoundQuery rQry3 = new CompoundQuery();
        rQry3.or( oQry );
        rQry3.and( rQry );
        rQry3.and( oQry6 );

        artifacts = repoSearchLayer.searchAdvanced( rQry3, indexer );
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
        CompoundQuery rQry4 = new CompoundQuery();
        QueryTerm qry5 = new QueryTerm( RepositoryIndex.FLD_ARTIFACTID, "sample" );
        QueryTerm qry6 = new QueryTerm( RepositoryIndex.FLD_GROUPID, "test" );
        rQry4.and( qry5 );
        rQry4.and( qry6 );
        CompoundQuery oQry2 = new CompoundQuery();
        oQry2.and( rQry4 );
        oQry2.and( rQry4 );

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
        CompoundQuery rQry5 = new CompoundQuery();
        QueryTerm qry7 = new QueryTerm( RepositoryIndex.FLD_ARTIFACTID, "sample2" );
        QueryTerm qry8 = new QueryTerm( RepositoryIndex.FLD_GROUPID, "test" );
        rQry5.and( qry7 );
        rQry5.and( qry8 );
        oQry2.and( rQry5 );

        artifacts = repoSearchLayer.searchAdvanced( oQry2, indexer );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
            assertEquals( "org.apache.maven", artifact.getGroupId() );
        }
    }

    /**
     * Test the exceptions thrown by DefaultRepositoryIndexSearcher
     *
     * @throws Exception
     */
    public void testSearchExceptions()
        throws Exception
    {
        createTestIndex();

        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        RepositoryIndexSearchLayer repoSearchLayer =
            (RepositoryIndexSearchLayer) lookup( RepositoryIndexSearchLayer.ROLE );

        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );

        try
        {
            QueryTerm queryTerm = new QueryTerm( RepositoryIndex.FLD_VERSION, "~~~~~" );
            repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
            fail( "Must throw an exception on unparseable query." );
        }
        catch ( RepositoryIndexSearchException re )
        {
            assertTrue( true );
        }

        indexer = factory.createArtifactRepositoryIndex( getTestFile( "target/index/sample" ), repository );

        try
        {
            QueryTerm queryTerm = new QueryTerm( RepositoryIndex.FLD_VERSION, "1.0" );
            repoSearchLayer.searchAdvanced( new SingleTermQuery( queryTerm ), indexer );
            fail( "Must throw an exception on invalid index location." );
        }
        catch ( RepositoryIndexSearchException re )
        {
            assertTrue( true );
        }

    }

    /**
     * Test delete of document from the artifact index.
     *
     * @throws Exception
     */
    public void testDeleteArtifactDocument()
        throws Exception
    {
        createTestIndex();

        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        RepositoryIndexSearcher repoSearcher = (RepositoryIndexSearcher) lookup( RepositoryIndexSearcher.ROLE );

        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );

        Artifact artifact = createArtifact( "org.apache.maven", "maven-artifact", "2.0.1" );
        Artifact pomArtifact =
            createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), "pom" );

        QueryTerm queryTerm =
            new QueryTerm( RepositoryIndex.FLD_ID, RepositoryIndex.ARTIFACT + ":" + artifact.getId() );
        List results = repoSearcher.search( new SingleTermQuery( queryTerm ), indexer );
        assertFalse( results.isEmpty() );

        queryTerm = new QueryTerm( RepositoryIndex.FLD_ID, RepositoryIndex.POM + ":" + pomArtifact.getId() );
        results = repoSearcher.search( new SingleTermQuery( queryTerm ), indexer );
        assertFalse( results.isEmpty() );

        indexer.deleteArtifact( artifact );
        indexer.deleteArtifact( pomArtifact );

        queryTerm = new QueryTerm( RepositoryIndex.FLD_ID, RepositoryIndex.ARTIFACT + ":" + artifact.getId() );
        results = repoSearcher.search( new SingleTermQuery( queryTerm ), indexer );
        assertTrue( results.isEmpty() );

        queryTerm = new QueryTerm( RepositoryIndex.FLD_ID, RepositoryIndex.POM + ":" + pomArtifact.getId() );
        results = repoSearcher.search( new SingleTermQuery( queryTerm ), indexer );
        assertTrue( results.isEmpty() );
    }

    /**
     * Test delete of document from the artifact index.
     *
     * @throws Exception
     */
    public void testCorruptJar()
        throws Exception
    {
        createTestIndex();

        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        RepositoryIndexSearcher repoSearcher = (RepositoryIndexSearcher) lookup( RepositoryIndexSearcher.ROLE );

        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );

        Artifact artifact = createArtifact( "org.apache.maven", "maven-corrupt-jar", "2.0" );
        indexer.indexArtifact( artifact );

        QueryTerm queryTerm = new QueryTerm( RepositoryIndex.FLD_ID, RepositoryIndex.ARTIFACT + artifact.getId() );
        List artifacts = repoSearcher.search( new SingleTermQuery( queryTerm ), indexer );
        assertEquals( 0, artifacts.size() );
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
    private Artifact createArtifact( String groupId, String artifactId, String version )
        throws Exception
    {
        return createArtifact( groupId, artifactId, version, "jar" );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version, String type )
    {
        Artifact artifact = artifactFactory.createBuildArtifact( groupId, artifactId, version, type );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        return artifact;
    }

    protected void tearDown()
        throws Exception
    {
        repository = null;

        super.tearDown();
    }
}
