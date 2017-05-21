package org.apache.archiva.scheduler.indexing;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.expr.StringSearchExpression;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ArchivaIndexingTaskExecutorTest
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class ArchivaIndexingTaskExecutorTest
    extends TestCase
{
    @Inject
    private ArchivaIndexingTaskExecutor indexingExecutor;

    private ManagedRepository repositoryConfig;

    private NexusIndexer indexer;

    @Inject
    PlexusSisuBridge plexusSisuBridge;

    @Inject
    MavenIndexerUtils mavenIndexerUtils;

    @Inject
    ManagedRepositoryAdmin managedRepositoryAdmin;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        repositoryConfig = new ManagedRepository();
        repositoryConfig.setId( "test-repo" );
        repositoryConfig.setLocation(
            new File( System.getProperty( "basedir" ), "target/test-classes/test-repo" ).getAbsolutePath() );
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setName( "Test Repository" );
        repositoryConfig.setScanned( true );
        repositoryConfig.setSnapshots( false );
        repositoryConfig.setReleases( true );

        indexer = plexusSisuBridge.lookup( NexusIndexer.class );

        managedRepositoryAdmin.createIndexContext( repositoryConfig );
    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {

        for ( IndexingContext indexingContext : indexer.getIndexingContexts().values() )
        {
            indexer.removeIndexingContext( indexingContext, true );
        }
        /*
        removeIndexingContext with true cleanup files.
        // delete created index in the repository
        File indexDir = new File( repositoryConfig.getLocation(), ".indexer" );
        FileUtils.deleteDirectory( indexDir );
        assertFalse( indexDir.exists() );

        indexDir = new File( repositoryConfig.getLocation(), ".index" );
        FileUtils.deleteDirectory( indexDir );
        assertFalse( indexDir.exists() );
        */
        super.tearDown();
    }

    protected IndexingContext getIndexingContext()
    {
        return indexer.getIndexingContexts().get( repositoryConfig.getId() );
    }

    @Test
    public void testAddArtifactToIndex()
        throws Exception
    {
        File artifactFile = new File( repositoryConfig.getLocation(),
                                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.ADD,
                                      getIndexingContext() );

        indexingExecutor.executeTask( task );

        BooleanQuery q = new BooleanQuery();
        q.add( indexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( "org.apache.archiva" ) ),
               Occur.SHOULD );
        q.add(
            indexer.constructQuery( MAVEN.ARTIFACT_ID, new StringSearchExpression( "archiva-index-methods-jar-test" ) ),
            Occur.SHOULD );

        if ( !indexer.getIndexingContexts().containsKey( repositoryConfig.getId() ) )
        {
            IndexingContext context = indexer.addIndexingContext( repositoryConfig.getId(), repositoryConfig.getId(),
                                                                  new File( repositoryConfig.getLocation() ),
                                                                  new File( repositoryConfig.getLocation(),
                                                                            ".indexer" ), null, null,
                                                                  mavenIndexerUtils.getAllIndexCreators()
            );
            context.setSearchable( true );
        }

        FlatSearchRequest request = new FlatSearchRequest( q );
        FlatSearchResponse response = indexer.searchFlat( request );

        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertFalse( new File( repositoryConfig.getLocation(), ".index" ).exists() );
        assertEquals( 1, response.getTotalHits() );

        Set<ArtifactInfo> results = response.getResults();

        ArtifactInfo artifactInfo = results.iterator().next();
        assertEquals( "org.apache.archiva", artifactInfo.groupId );
        assertEquals( "archiva-index-methods-jar-test", artifactInfo.artifactId );
        assertEquals( "test-repo", artifactInfo.repository );

    }

    @Test
    public void testUpdateArtifactInIndex()
        throws Exception
    {
        File artifactFile = new File( repositoryConfig.getLocation(),
                                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.ADD,
                                      getIndexingContext() );

        indexingExecutor.executeTask( task );
        indexingExecutor.executeTask( task );

        BooleanQuery q = new BooleanQuery();
        q.add( indexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( "org.apache.archiva" ) ),
               Occur.SHOULD );
        q.add(
            indexer.constructQuery( MAVEN.ARTIFACT_ID, new StringSearchExpression( "archiva-index-methods-jar-test" ) ),
            Occur.SHOULD );

        IndexingContext ctx = indexer.getIndexingContexts().get( repositoryConfig.getId() );

        IndexSearcher searcher = ctx.acquireIndexSearcher();
        TopDocs topDocs = searcher.search( q, null, 10 );

        //searcher.close();
        ctx.releaseIndexSearcher( searcher );

        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertFalse( new File( repositoryConfig.getLocation(), ".index" ).exists() );

        // should only return 1 hit!
        assertEquals( 1, topDocs.totalHits );
    }

    @Test
    public void testRemoveArtifactFromIndex()
        throws Exception
    {
        File artifactFile = new File( repositoryConfig.getLocation(),
                                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.ADD,
                                      getIndexingContext() );

        // add artifact to index
        indexingExecutor.executeTask( task );

        BooleanQuery q = new BooleanQuery();
        q.add( indexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.apache.archiva" ) ),
               Occur.SHOULD );
        //q.add(
        //    indexer.constructQuery( MAVEN.ARTIFACT_ID, new SourcedSearchExpression( "archiva-index-methods-jar-test" ) ),
        //    Occur.SHOULD );

        FlatSearchRequest flatSearchRequest =
            new FlatSearchRequest( q, indexer.getIndexingContexts().get( repositoryConfig.getId() ) );

        FlatSearchResponse response = indexer.searchFlat( flatSearchRequest );

        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertFalse( new File( repositoryConfig.getLocation(), ".index" ).exists() );

        // should return 1 hit
        assertEquals( 1, response.getTotalHitsCount() );

        // remove added artifact from index
        task = new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.DELETE,
                                         getIndexingContext() );
        indexingExecutor.executeTask( task );

        task = new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.FINISH,
                                         getIndexingContext() );
        indexingExecutor.executeTask( task );

        q = new BooleanQuery();
        q.add( indexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.apache.archiva" ) ),
               Occur.SHOULD );
        q.add( indexer.constructQuery( MAVEN.ARTIFACT_ID,
                                       new SourcedSearchExpression( "archiva-index-methods-jar-test" ) ), Occur.SHOULD
        );

        assertTrue( new File( repositoryConfig.getLocation(), ".indexer" ).exists() );
        assertFalse( new File( repositoryConfig.getLocation(), ".index" ).exists() );

        flatSearchRequest = new FlatSearchRequest( q, getIndexingContext() );

        response = indexer.searchFlat( flatSearchRequest );
        // artifact should have been removed from the index!
        assertEquals( 0, response.getTotalHitsCount() );//.totalHits );

        // TODO: test it was removed from the packaged index also
    }

    @Test
    public void testPackagedIndex()
        throws Exception
    {

        File indexerDirectory = new File( repositoryConfig.getLocation(), ".indexer" );

        indexerDirectory.listFiles( new FilenameFilter()
        {
            @Override
            public boolean accept( File file, String s )
            {
                if ( s.startsWith( "nexus-maven-repository-index" ) )
                {
                    new File( file, s ).delete();
                }
                return false;
            }
        } );

        File artifactFile = new File( repositoryConfig.getLocation(),
                                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.ADD,
                                      getIndexingContext() );
        task.setExecuteOnEntireRepo( false );

        indexingExecutor.executeTask( task );

        task = new ArtifactIndexingTask( repositoryConfig, null, ArtifactIndexingTask.Action.FINISH,
                                         getIndexingContext() );

        task.setExecuteOnEntireRepo( false );

        indexingExecutor.executeTask( task );

        assertTrue( indexerDirectory.exists() );

        // test packed index file creation
        assertTrue( new File( indexerDirectory, "nexus-maven-repository-index.zip" ).exists() );
        assertTrue( new File( indexerDirectory, "nexus-maven-repository-index.properties" ).exists() );
        assertTrue( new File( indexerDirectory, "nexus-maven-repository-index.gz" ).exists() );

        // unpack .zip index
        File destDir = new File( repositoryConfig.getLocation(), ".indexer/tmp" );
        unzipIndex( indexerDirectory.getPath(), destDir.getPath() );

        BooleanQuery q = new BooleanQuery();
        q.add( indexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( "org.apache.archiva" ) ),
               Occur.SHOULD );
        q.add(
            indexer.constructQuery( MAVEN.ARTIFACT_ID, new StringSearchExpression( "archiva-index-methods-jar-test" ) ),
            Occur.SHOULD );

        FlatSearchRequest request = new FlatSearchRequest( q, getIndexingContext() );
        FlatSearchResponse response = indexer.searchFlat( request );

        Set<ArtifactInfo> results = response.getResults();

        ArtifactInfo artifactInfo = results.iterator().next();
        assertEquals( "org.apache.archiva", artifactInfo.groupId );
        assertEquals( "archiva-index-methods-jar-test", artifactInfo.artifactId );
        assertEquals( "test-repo", artifactInfo.repository );

        assertEquals( 1, response.getTotalHits() );
    }

    private void unzipIndex( String indexDir, String destDir )
        throws IOException
    {
        final int buff = 2048;

        Files.createDirectories( Paths.get( destDir ) );

        try (InputStream fin = Files.newInputStream( Paths.get( indexDir, "nexus-maven-repository-index.zip" ) ))
        {
            ZipInputStream in = new ZipInputStream( new BufferedInputStream( fin ) );
            ZipEntry entry;

            while ( ( entry = in.getNextEntry() ) != null )
            {
                int count;
                byte data[] = new byte[buff];
                try (OutputStream fout = Files.newOutputStream( Paths.get( destDir, entry.getName() ) ))
                {
                    try (BufferedOutputStream out = new BufferedOutputStream( fout, buff ))
                    {

                        while ( ( count = in.read( data, 0, buff ) ) != -1 )
                        {
                            out.write( data, 0, count );
                        }
                    }
                }
            }

        }
    }
}
