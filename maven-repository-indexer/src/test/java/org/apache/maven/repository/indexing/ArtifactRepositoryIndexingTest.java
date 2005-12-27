package org.apache.maven.repository.indexing;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

import org.codehaus.plexus.PlexusTestCase;

/**
 *
 */
public class ArtifactRepositoryIndexingTest
    extends PlexusTestCase
{
    protected ArtifactRepositoryIndexer indexer;
    protected ArtifactFactory artifactFactory;
    protected ArtifactRepository repository;
    protected String indexPath;
    private RepositorySearcher repoSearcher;
    private static final String GROUPID = "groupId";
    private static final String ARTIFACTID = "artifactId";
	private static final String VERSION = "version";
	private static final String SHA1 = "sha1";
    private static final String MD5 = "md5";
    private static final String CLASSES = "classes";
    private static final String PACKAGES = "packages";
    private static final String FILES = "files";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        File repositoryDirectory = getTestFile( "src/test/repository" );
        String repoDir = repositoryDirectory.toURL().toString();

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );
        ArtifactRepositoryFactory repoFactory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
        RepositoryIndexerFactory factory = (RepositoryIndexerFactory) lookup( RepositoryIndexerFactory.ROLE );

        String indexPath = "target/index";
        repository = repoFactory.createArtifactRepository( "test", repoDir, layout, null, null );
        indexer = (ArtifactRepositoryIndexer) factory.getArtifactRepositoryIndexer( indexPath, repository );
        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        
        repoSearcher = new ArtifactRepositorySearcher(indexPath, repository);
    }
    
    public void testIndex()
        throws Exception
    {
        Artifact artifact = getArtifact( "org.apache.maven", "maven-artifact", "2.0.1" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.addArtifactIndex( artifact );
        
        artifact = getArtifact( "org.apache.maven", "maven-model", "2.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.addArtifactIndex( artifact );
        
        indexer.optimize();
        indexer.close();

        indexer.open();
        artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        indexer.addObjectIndex( artifact );
        indexer.close();
    }
  
    public void testSearch() throws Exception{
    	
        //test the search GROUPID
        List artifacts = repoSearcher.searchArtifact("test", GROUPID);
        assertEquals( 1, artifacts.size() );
                        
        artifacts = repoSearcher.searchArtifact("test", ARTIFACTID);
        assertEquals( 1, artifacts.size() );
                
        artifacts = repoSearcher.searchArtifact("1.0", VERSION);
        assertEquals( 1, artifacts.size() );
        
        artifacts = repoSearcher.searchArtifact("App", CLASSES);
        assertEquals( 1, artifacts.size() );
        
        artifacts = repoSearcher.searchArtifact("groupId", PACKAGES);
        assertEquals( 1, artifacts.size() );
        
        artifacts = repoSearcher.searchArtifact("pom.xml", FILES);
        assertEquals( 3, artifacts.size() );

        for( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
        	Artifact artifact = (Artifact) iter.next();
        	File f = artifact.getFile();
            assertNotNull( f );
        	assertTrue( f.exists() );
        }
        
        //search org.apache.maven jars
        artifacts = repoSearcher.searchArtifact("org.apache.maven", GROUPID);
        assertEquals( 2, artifacts.size() );
        
        artifacts = repoSearcher.searchArtifact("maven-artifact", ARTIFACTID);
        assertEquals( 1, artifacts.size() );
                
        artifacts = repoSearcher.searchArtifact("2", VERSION);
        assertEquals( 2, artifacts.size() );
   
    }

    public void testIndexerExceptions()
        throws Exception
    {
        indexer.close();
        Artifact artifact = getArtifact( "test", "test-artifactId", "1.0" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );

        try
        {
            indexer.addArtifactIndex( artifact );
            fail( "Must throw exception on add index with closed index." );
        }
        catch( RepositoryIndexerException e )
        {
            //expected
        }
        
        try
        {
            indexer.optimize();
            fail( "Must throw exception on optimize index with closed index." );
        }
        catch( RepositoryIndexerException e )
        {
            //expected
        }
        
        indexer.open();
        
        try
        {
            indexer.addObjectIndex( "should fail" );
            fail( "Must throw exception on add non-Artifact object." );
        }
        catch( RepositoryIndexerException e )
        {
            //expected
        }
    }
    
    protected Artifact getArtifact( String groupId, String artifactId, String version )
    {
        return artifactFactory.createBuildArtifact( groupId, artifactId, version, "jar" );
    }
}
