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
import org.apache.maven.repository.indexing.query.OptionalQuery;
import org.apache.maven.repository.indexing.query.Query;
import org.apache.maven.repository.indexing.query.RequiredQuery;
import org.apache.maven.repository.indexing.query.SinglePhraseQuery;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @author Edwin Punzalan/Maria Odea Ching
 */
public class ArtifactRepositoryIndexingTest
    extends PlexusTestCase
{
    private static final String GROUPID = "groupId";

    private static final String ARTIFACTID = "artifactId";

    private static final String VERSION = "version";

    private static final String CLASSES = "classes";

    private static final String PACKAGES = "packages";

    private static final String FILES = "files";

    private ArtifactFactory artifactFactory;

    private ArtifactRepository repository;

    private String indexPath;

    private Digester digester;

    private static final String SHA1 = "sha1";

    private static final String MD5 = "md5";

    private static final String NAME = "name";

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
        ArtifactRepositoryIndex indexer;
        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );

        try
        {
            String notIndexDir = new File( "pom.xml" ).getAbsolutePath();
            indexer = factory.createArtifactRepositoryIndex( notIndexDir, repository );
            fail( "Must throw exception on non-directory index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            // expected
        }

        try
        {
            String notIndexDir = new File( "" ).getAbsolutePath();
            indexer = factory.createArtifactRepositoryIndex( notIndexDir, repository );
            fail( "Must throw an exception on a non-index directory" );
        }
        catch ( RepositoryIndexException e )
        {
            // expected
        }

        Artifact artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );

        indexer = factory.createArtifactRepositoryIndex( indexPath, repository );
        indexer.close();

        try
        {
            indexer.indexArtifact( artifact );
            fail( "Must throw exception on add index with closed index." );
        }
        catch ( RepositoryIndexException e )
        {
            // expected
        }

        try
        {
            indexer.optimize();
            fail( "Must throw exception on optimize index with closed index." );
        }
        catch ( RepositoryIndexException e )
        {
            // expected
        }

        indexer = factory.createArtifactRepositoryIndex( indexPath, repository );

        try
        {
            indexer.index( "should fail" );
            fail( "Must throw exception on add non-Artifact object." );
        }
        catch ( RepositoryIndexException e )
        {
            // expected
        }

        indexer.close();
    }

    /**
     * Create an index that will be used for testing.
     * 
     * @throws Exception
     */
    public void createTestIndex()
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
        indexer.index( artifact );

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
        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );
        RepositoryIndexSearcher repoSearcher = factory.createArtifactRepositoryIndexSearcher( indexer );

        // search version
        Query qry = new SinglePhraseQuery( VERSION, "1.0" );
        List artifacts = repoSearcher.search( qry );
        assertEquals( 1, artifacts.size() );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            assertEquals( "1.0", artifact.getVersion() );
        }

        // search classes
        qry = new SinglePhraseQuery( CLASSES, "App" );
        artifacts = repoSearcher.search( qry );
        assertEquals( 1, artifacts.size() );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            assertEquals( "test-artifactId", artifact.getArtifactId() );
        }

        // search packages
        qry = new SinglePhraseQuery( PACKAGES, "groupId" );
        artifacts = repoSearcher.search( qry );
        assertEquals( 1, artifacts.size() );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            assertEquals( "test-artifactId", artifact.getArtifactId() );
        }

        // search files
        qry = new SinglePhraseQuery( FILES, "pom.xml" );
        artifacts = repoSearcher.search( qry );
        assertEquals( 3, artifacts.size() );
        Iterator iter = artifacts.iterator();
        if ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertEquals( "test-artifactId", artifact.getArtifactId() );
        }

        // search group id
        qry = new SinglePhraseQuery( GROUPID, "org.apache.maven" );
        artifacts = repoSearcher.search( qry );
        assertEquals( 2, artifacts.size() );
        iter = artifacts.iterator();
        if ( iter.hasNext() )
        {
            Artifact artifact = (Artifact) iter.next();
            assertEquals( "org.apache.maven", artifact.getGroupId() );
        }

        // search artifact id
        qry = new SinglePhraseQuery( ARTIFACTID, "maven-artifact" );
        artifacts = repoSearcher.search( qry );
        assertEquals( 1, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
        }

        // search version
        qry = new SinglePhraseQuery( VERSION, "2" );
        artifacts = repoSearcher.search( qry );
        assertEquals( 2, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            assertTrue( artifact.getVersion().indexOf( "2" ) != -1 );
        }

        // search sha1 checksum
        Artifact artifact = getArtifact( "org.apache.maven", "maven-model", "2.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );

        String sha1 = digester.createChecksum( artifact.getFile(), Digester.SHA1 );

        qry = new SinglePhraseQuery( SHA1, sha1.trim() );
        artifacts = repoSearcher.search( qry );
        assertEquals( 1, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact2 = (Artifact) iter.next();
            String sha1Tmp = digester.createChecksum( artifact2.getFile(), Digester.SHA1 );
            assertEquals( sha1, sha1Tmp );
        }

        // search md5 checksum
        String md5 = digester.createChecksum( artifact.getFile(), Digester.MD5 );
        qry = new SinglePhraseQuery( MD5, md5.trim() );
        artifacts = repoSearcher.search( qry );
        assertEquals( 1, artifacts.size() );
        for ( iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact2 = (Artifact) iter.next();
            String md5Tmp = digester.createChecksum( artifact2.getFile(), Digester.MD5 );
            assertEquals( md5, md5Tmp );
        }

        indexer.close();
    }

    /**
     * Test the ArtifactRepositoryIndexSearcher using compound search (AND, OR).
     * 
     * @throws Exception
     */
    public void testSearchCompound()
        throws Exception
    {

        createTestIndex();

        RepositoryIndexingFactory factory = (RepositoryIndexingFactory) lookup( RepositoryIndexingFactory.ROLE );
        ArtifactRepositoryIndex indexer = factory.createArtifactRepositoryIndex( indexPath, repository );
        RepositoryIndexSearcher repoSearcher = factory.createArtifactRepositoryIndexSearcher( indexer );

        // Criteria 1: required query
        // ex. artifactId=maven-artifact AND groupId=org.apache.maven
        Query qry1 = new SinglePhraseQuery( ARTIFACTID, "maven-artifact" );
        Query qry2 = new SinglePhraseQuery( GROUPID, "org.apache.maven" );
        RequiredQuery rQry = new RequiredQuery();
        rQry.add( qry1 );
        rQry.add( qry2 );

        List artifacts = repoSearcher.search( rQry );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
            assertEquals( "org.apache.maven", artifact.getGroupId() );
        }

        // Criteria 2: nested required query
        // ex. (artifactId=maven-artifact AND groupId=org.apache.maven) OR
        // version=2.0.3
        Query qry3 = new SinglePhraseQuery( VERSION, "2.0.3" );
        OptionalQuery oQry = new OptionalQuery();
        oQry.add( rQry );
        oQry.add( qry3 );

        artifacts = repoSearcher.search( oQry );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
            assertEquals( "org.apache.maven", artifact.getGroupId() );
        }

        // Criteria 3: nested required query
        // ex. (artifactId=maven-artifact AND groupId=org.apache.maven) AND
        // (version=2.0.3 OR version=2.0.1)
        // AND (name=maven-artifact-2.0.1.jar OR name=maven-artifact)
        Query qry4 = new SinglePhraseQuery( VERSION, "2.0.1" );
        oQry = new OptionalQuery();
        oQry.add( qry3 );
        oQry.add( qry4 );

        OptionalQuery oQry5 = new OptionalQuery();
        Query qry9 = new SinglePhraseQuery( NAME, "maven-artifact-2.0.1.jar" );
        Query qry10 = new SinglePhraseQuery( NAME, "maven-artifact" );
        oQry5.add( qry9 );
        oQry5.add( qry10 );

        RequiredQuery rQry2 = new RequiredQuery();
        rQry2.add( oQry );
        rQry2.add( rQry );
        rQry2.add( oQry5 );

        artifacts = repoSearcher.search( rQry2 );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
            assertEquals( "org.apache.maven", artifact.getGroupId() );
            assertEquals( "2.0.1", artifact.getVersion() );
        }

        // Criteria 4: nested required query
        // ex. [(artifactId=maven-artifact AND groupId=org.apache.maven) AND
        // (version=2.0.3 OR version=2.0.1)
        // AND (name=maven-artifact-2.0.1.jar OR name=maven-artifact)]
        // OR [(artifactId=sample AND groupId=test)]
        RequiredQuery rQry3 = new RequiredQuery();
        Query qry5 = new SinglePhraseQuery( ARTIFACTID, "sample" );
        Query qry6 = new SinglePhraseQuery( GROUPID, "test" );
        rQry3.add( qry5 );
        rQry3.add( qry6 );
        OptionalQuery oQry2 = new OptionalQuery();
        oQry2.add( rQry2 );
        oQry2.add( rQry3 );

        artifacts = repoSearcher.search( oQry2 );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
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
        RequiredQuery rQry4 = new RequiredQuery();
        Query qry7 = new SinglePhraseQuery( ARTIFACTID, "sample2" );
        Query qry8 = new SinglePhraseQuery( GROUPID, "test" );
        rQry4.add( qry7 );
        rQry4.add( qry8 );
        oQry2.add( rQry4 );

        artifacts = repoSearcher.search( oQry2 );
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            assertEquals( "maven-artifact", artifact.getArtifactId() );
            assertEquals( "org.apache.maven", artifact.getGroupId() );
        }

        indexer.close();
    }

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
