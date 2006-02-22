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
import org.apache.maven.repository.indexing.query.CompoundQuery;
import org.apache.maven.repository.indexing.query.Query;
import org.apache.maven.repository.indexing.query.SinglePhraseQuery;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public class ArtifactRepositoryIndexingTest
    extends PlexusTestCase
{
    private ArtifactFactory artifactFactory;

    private ArtifactRepository repository;

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

    /**
     * Method for testing the exceptions thrown by ArtifactRepositoryIndex
     *
     * @throws Exception
     */
    public void testIndexerExceptions()
        throws Exception
    {
        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        Artifact artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );

        try
        {
            String notIndexDir = new File( "pom.xml" ).getAbsolutePath();
            ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( notIndexDir, repository );
            indexer.indexArtifact( artifact );
            fail( "Must throw exception on non-directory index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            assertTrue( true );
        }

        try
        {
            String notIndexDir = new File( "" ).getAbsolutePath();
            ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( notIndexDir, repository );
            indexer.indexArtifact( artifact );
            fail( "Must throw an exception on a non-index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            assertTrue( true );
        }

        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );
        try
        {
            indexer.isIndexed( new Object() );
            fail( "Must throw exception on object not of type artifact." );
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
        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );
        RepositoryIndexSearchLayer repoSearchLayer = factory.createRepositoryIndexSearchLayer( indexer );

        // search version
        Query qry = new SinglePhraseQuery( RepositoryIndex.FLD_VERSION, "1.0" );
        List artifacts = repoSearchLayer.searchAdvanced( qry );
        assertEquals( 1, artifacts.size() );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "1.0", artifact.getVersion() );
        }

        // search classes
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_CLASSES, "App" );
        artifacts = repoSearchLayer.searchAdvanced( qry );
        assertEquals( 1, artifacts.size() );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "test-artifactId", artifact.getArtifactId() );
        }

        // search packages
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_PACKAGES, "groupId" );
        artifacts = repoSearchLayer.searchAdvanced( qry );
        assertEquals( 1, artifacts.size() );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "test-artifactId", artifact.getArtifactId() );
        }

        // search files
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_FILES, "pom.xml" );
        artifacts = repoSearchLayer.searchAdvanced( qry );
        assertEquals( 3, artifacts.size() );
        Iterator iter = artifacts.iterator();
        if ( iter.hasNext() )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "test-artifactId", artifact.getArtifactId() );
        }

        // search group id
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_GROUPID, "org.apache.maven" );
        artifacts = repoSearchLayer.searchAdvanced( qry );
        assertEquals( 2, artifacts.size() );
        iter = artifacts.iterator();
        if ( iter.hasNext() )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "org.apache.maven", artifact.getGroupId() );
        }

        // search artifact id
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_ARTIFACTID, "maven-artifact" );
        artifacts = repoSearchLayer.searchAdvanced( qry );
        assertEquals( 1, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
        }

        // search version
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_VERSION, "2" );
        artifacts = repoSearchLayer.searchAdvanced( qry );
        assertEquals( 2, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact = result.getArtifact();
            assertTrue( artifact.getVersion().indexOf( "2" ) != -1 );
        }

        // search sha1 checksum
        Artifact artifact = getArtifact( "org.apache.maven", "maven-model", "2.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );

        String sha1 = digester.createChecksum( artifact.getFile(), Digester.SHA1 );

        qry = new SinglePhraseQuery( RepositoryIndex.FLD_SHA1, sha1.trim() );
        artifacts = repoSearchLayer.searchAdvanced( qry );
        assertEquals( 1, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact2 = result.getArtifact();
            String sha1Tmp = digester.createChecksum( artifact2.getFile(), Digester.SHA1 );
            assertEquals( sha1, sha1Tmp );
        }

        // search md5 checksum
        String md5 = digester.createChecksum( artifact.getFile(), Digester.MD5 );
        qry = new SinglePhraseQuery( RepositoryIndex.FLD_MD5, md5.trim() );
        artifacts = repoSearchLayer.searchAdvanced( qry );
        assertEquals( 1, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            SearchResult result = (SearchResult) iter.next();
            Artifact artifact2 = result.getArtifact();
            String md5Tmp = digester.createChecksum( artifact2.getFile(), Digester.MD5 );
            assertEquals( md5, md5Tmp );
        }

        indexer.close();
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
        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );
        //RepositoryIndexSearcher repoSearchLayer = factory.createDefaultRepositoryIndexSearcher( indexer );
        RepositoryIndexSearchLayer repoSearchLayer = factory.createRepositoryIndexSearchLayer( indexer );

        // Criteria 1: required query
        // ex. artifactId=maven-artifact AND groupId=org.apache.maven
        Query qry1 = new SinglePhraseQuery( RepositoryIndex.FLD_ARTIFACTID, "maven-artifact" );
        Query qry2 = new SinglePhraseQuery( RepositoryIndex.FLD_GROUPID, "org.apache.maven" );
        CompoundQuery rQry = new CompoundQuery();
        rQry.and( qry1 );
        rQry.and( qry2 );

        List artifacts = repoSearchLayer.searchAdvanced( rQry );
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
        oQry.or( rQry );
        oQry.or( qry3 );

        artifacts = repoSearchLayer.searchAdvanced( oQry );
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
        Query qry9 = new SinglePhraseQuery( RepositoryIndex.FLD_NAME, "maven-artifact-2.0.1.jar" );
        Query qry10 = new SinglePhraseQuery( RepositoryIndex.FLD_NAME, "maven-artifact" );
        oQry5.or( qry9 );
        oQry5.or( qry10 );

        CompoundQuery rQry2 = new CompoundQuery();
        rQry2.or( oQry );
        rQry2.and( rQry );
        rQry2.or( oQry5 );

        artifacts = repoSearchLayer.searchAdvanced( rQry2 );
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

        artifacts = repoSearchLayer.searchAdvanced( oQry2 );
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

        artifacts = repoSearchLayer.searchAdvanced( oQry2 );
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
     * Test the exceptions thrown by DefaultRepositoryIndexSearcher
     *
     * @throws Exception
     */
    public void testSearchExceptions()
        throws Exception
    {
        createTestIndex();

        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );
        // RepositoryIndexSearcher repoSearchLayer = factory.createDefaultRepositoryIndexSearcher( indexer );
        RepositoryIndexSearchLayer repoSearchLayer = factory.createRepositoryIndexSearchLayer( indexer );

        try
        {
            Query qry = new SinglePhraseQuery( RepositoryIndex.FLD_VERSION, "~~~~~" );
            List artifacts = repoSearchLayer.searchAdvanced( qry );
            fail( "Must throw an exception on unparseable query." );
        }
        catch ( RepositoryIndexSearchException re )
        {
            assertTrue( true );
        }

        indexer = factory.createArtifactRepositoryIndex( "target/index/sample", repository );
        repoSearchLayer = factory.createRepositoryIndexSearchLayer( indexer );

        try
        {
            Query qry = new SinglePhraseQuery( RepositoryIndex.FLD_VERSION, "1.0" );
            List artifacts = repoSearchLayer.searchAdvanced( qry );
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
        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );

        Artifact artifact = getArtifact( "org.apache.maven", "maven-artifact", "2.0.1" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.deleteDocument( RepositoryIndex.FLD_ID, RepositoryIndex.ARTIFACT + artifact.getId() );

        RepositoryIndexSearcher repoSearcher = factory.createDefaultRepositoryIndexSearcher( indexer );
        Query qry = new SinglePhraseQuery( RepositoryIndex.FLD_ID, RepositoryIndex.ARTIFACT + artifact.getId() );
        List artifacts = repoSearcher.search( qry );
        assertEquals( artifacts.size(), 0 );
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
