package org.apache.archiva.indexer.search;

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
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.archiva.common.utils.FileUtil;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactContextProducer;
import org.apache.maven.index.ArtifactScanningListener;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.ScanningResult;
import org.apache.maven.index.context.IndexingContext;
import org.easymock.MockControl;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public abstract class AbstractNexusRepositorySearch
    extends TestCase
{

    protected Logger log = LoggerFactory.getLogger( getClass() );

    public static String TEST_REPO_1 = "nexus-search-test-repo";

    public static String TEST_REPO_2 = "nexus-search-test-repo-2";

    NexusRepositorySearch search;

    ArchivaConfiguration archivaConfig;

    ArtifactContextProducer artifactContextProducer;

    MockControl archivaConfigControl;

    Configuration config;

    @Inject
    PlexusSisuBridge plexusSisuBridge;

    @Inject
    MavenIndexerUtils mavenIndexerUtils;

    NexusIndexer nexusIndexer;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        FileUtils.deleteDirectory( new File( FileUtil.getBasedir(), "/target/repos/" + TEST_REPO_1 + "/.indexer" ) );
        assertFalse( new File( FileUtil.getBasedir(), "/target/repos/" + TEST_REPO_1 + "/.indexer" ).exists() );

        FileUtils.deleteDirectory( new File( FileUtil.getBasedir(), "/target/repos/" + TEST_REPO_2 + "/.indexer" ) );
        assertFalse( new File( FileUtil.getBasedir(), "/target/repos/" + TEST_REPO_2 + "/.indexer" ).exists() );

        archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );

        archivaConfig = (ArchivaConfiguration) archivaConfigControl.getMock();

        DefaultManagedRepositoryAdmin defaultManagedRepositoryAdmin = new DefaultManagedRepositoryAdmin();
        defaultManagedRepositoryAdmin.setArchivaConfiguration( archivaConfig );

        search = new NexusRepositorySearch( plexusSisuBridge, defaultManagedRepositoryAdmin, mavenIndexerUtils );

        nexusIndexer = plexusSisuBridge.lookup( NexusIndexer.class );

        artifactContextProducer = plexusSisuBridge.lookup( ArtifactContextProducer.class );

        config = new Configuration();
        config.addManagedRepository( createRepositoryConfig( TEST_REPO_1 ) );
        config.addManagedRepository( createRepositoryConfig( TEST_REPO_2 ) );
    }

    @After
    public void tearDown()
        throws Exception
    {

        for ( IndexingContext indexingContext : nexusIndexer.getIndexingContexts().values() )
        {
            nexusIndexer.removeIndexingContext( indexingContext, true );
        }

        FileUtils.deleteDirectory( new File( FileUtil.getBasedir(), "/target/repos/" + TEST_REPO_1 ) );
        assertFalse( new File( FileUtil.getBasedir(), "/target/repos/" + TEST_REPO_1 ).exists() );

        FileUtils.deleteDirectory( new File( FileUtil.getBasedir(), "/target/repos/" + TEST_REPO_2 ) );
        assertFalse( new File( FileUtil.getBasedir(), "/target/repos/" + TEST_REPO_2 ).exists() );

        super.tearDown();
    }

    protected ManagedRepositoryConfiguration createRepositoryConfig( String repository )
    {
        ManagedRepositoryConfiguration repositoryConfig = new ManagedRepositoryConfiguration();
        repositoryConfig.setId( repository );
        repositoryConfig.setLocation( FileUtil.getBasedir() + "/target/repos/" + repository );
        File f = new File( repositoryConfig.getLocation() );
        if ( !f.exists() )
        {
            f.mkdirs();
        }
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setName( repository );
        repositoryConfig.setScanned( true );
        repositoryConfig.setSnapshots( false );
        repositoryConfig.setReleases( true );

        return repositoryConfig;
    }

    protected void createIndex( String repository, List<File> filesToBeIndexed, boolean scan )
        throws Exception
    {

        IndexingContext context = nexusIndexer.getIndexingContexts().get( repository );

        if ( context != null )
        {
            nexusIndexer.removeIndexingContext( context, true );
        }

        File indexerDirectory = new File( FileUtil.getBasedir(), "/target/repos/" + repository + "/.indexer" );

        if ( indexerDirectory.exists() )
        {
            FileUtils.deleteDirectory( indexerDirectory );
        }

        assertFalse( indexerDirectory.exists() );

        File lockFile = new File( FileUtil.getBasedir(), "/target/repos/" + repository + "/.indexer/write.lock" );
        if ( lockFile.exists() )
        {
            lockFile.delete();
        }

        assertFalse( lockFile.exists() );

        File repo = new File( FileUtil.getBasedir(), "src/test/" + repository );
        assertTrue( repo.exists() );
        File indexDirectory =
            new File( FileUtil.getBasedir(), "target/index/test-" + Long.toString( System.currentTimeMillis() ) );
        indexDirectory.deleteOnExit();
        FileUtils.deleteDirectory( indexDirectory );

        context = nexusIndexer.addIndexingContext( repository, repository, repo, indexDirectory,
                                                   repo.toURI().toURL().toExternalForm(),
                                                   indexDirectory.toURI().toURL().toString(),
                                                   search.getAllIndexCreators() );

        // minimize datas in memory
        context.getIndexWriter().setMaxBufferedDocs( -1 );
        context.getIndexWriter().setRAMBufferSizeMB( 1 );
        for ( File artifactFile : filesToBeIndexed )
        {
            assertTrue( "file not exists " + artifactFile.getPath(), artifactFile.exists() );
            ArtifactContext ac = artifactContextProducer.getArtifactContext( context, artifactFile );
            nexusIndexer.addArtifactToIndex( ac, context );
            context.updateTimestamp( true );
        }

        if ( scan )
        {
            nexusIndexer.scan( context, new ArtifactScanListener(), false );
        }
        // force flushing
        context.getIndexWriter().commit();
        context.getIndexWriter().close( true );
        // wait for io flush ....
        //Thread.sleep( 2000 );
        context.setSearchable( true );

    }

    static class ArtifactScanListener
        implements ArtifactScanningListener
    {
        protected Logger log = LoggerFactory.getLogger( getClass() );

        public void scanningStarted( IndexingContext ctx )
        {

        }

        public void scanningFinished( IndexingContext ctx, ScanningResult result )
        {

        }

        public void artifactError( ArtifactContext ac, Exception e )
        {
            log.debug( "artifactError " + ac.getArtifact().getPath(), e );
        }

        public void artifactDiscovered( ArtifactContext ac )
        {
            log.debug( "artifactDiscovered " + ac.getArtifact().getPath() + ":" + ac.getArtifactInfo() );
        }
    }

    public String niceDisplay( SearchResults searchResults )
        throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for ( SearchResultHit hit : searchResults.getHits() )
        {
            sb.append( hit.toString() ).append( SystemUtils.LINE_SEPARATOR );
        }
        return sb.toString();
    }
}
