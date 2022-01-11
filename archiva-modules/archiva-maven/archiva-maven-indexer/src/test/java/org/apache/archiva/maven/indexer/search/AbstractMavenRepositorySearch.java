package org.apache.archiva.maven.indexer.search;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.configuration.provider.ArchivaConfiguration;
import org.apache.archiva.configuration.model.Configuration;
import org.apache.archiva.configuration.model.ManagedRepositoryConfiguration;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.proxy.ProxyRegistry;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.base.RepositoryHandlerDependencies;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactContextProducer;
import org.apache.maven.index.ArtifactScanningListener;
import org.apache.maven.index.DefaultScannerListener;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.IndexerEngine;
import org.apache.maven.index.QueryCreator;
import org.apache.maven.index.Scanner;
import org.apache.maven.index.ScanningRequest;
import org.apache.maven.index.ScanningResult;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index_shaded.lucene.index.IndexUpgrader;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import static org.apache.archiva.indexer.ArchivaIndexManager.DEFAULT_INDEX_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public abstract class AbstractMavenRepositorySearch
    extends TestCase
{

    protected Logger log = LoggerFactory.getLogger( getClass() );

    public static String TEST_REPO_1 = "maven-search-test-repo";

    public static String TEST_REPO_2 = "maven-search-test-repo-2";


    public static String REPO_RELEASE = "repo-release";

    MavenRepositorySearch search;

    ArchivaConfiguration archivaConfig;

    @Inject
    ArtifactContextProducer artifactContextProducer;

    @Inject
    ArchivaRepositoryRegistry repositoryRegistry;

    @SuppressWarnings( "unused" )
    @Inject
    RepositoryHandlerDependencies repositoryHandlerDependencies;

    @Inject
    ProxyRegistry proxyRegistry;

    @Inject
    private IndexerEngine indexerEngine;

    Configuration config;

    @Inject
    Indexer indexer;

    @Inject
    Scanner scanner;

    @Inject
    QueryCreator queryCreator;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        FileUtils.deleteDirectory( Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "/target/repos/" + TEST_REPO_1 + "/.indexer" ) );
        assertFalse( Files.exists(Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "/target/repos/" + TEST_REPO_1 + "/.indexer" )) );
        Files.createDirectories( Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "/target/repos/" + TEST_REPO_1 + "/.indexer" ) );

        FileUtils.deleteDirectory( Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "/target/repos/" + TEST_REPO_2 + "/.indexer" ) );
        assertFalse( Files.exists(Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "/target/repos/" + TEST_REPO_2 + "/.indexer" )) );
        Files.createDirectories( Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "/target/repos/" + TEST_REPO_2 + "/.indexer" ) );

        archivaConfig = mock( ArchivaConfiguration.class );

        repositoryRegistry.setArchivaConfiguration( archivaConfig );

        search = new MavenRepositorySearch( indexer, repositoryRegistry, proxyRegistry,
                                            queryCreator );

        assertNotNull( repositoryRegistry );

        config = new Configuration();
        config.addManagedRepository( createRepositoryConfig( TEST_REPO_1 ) );
        config.addManagedRepository( createRepositoryConfig( TEST_REPO_2 ) );
        config.addManagedRepository( createRepositoryConfig( REPO_RELEASE ) );

        when( archivaConfig.getDefaultLocale() ).thenReturn( Locale.getDefault( ) );
        when( archivaConfig.getConfiguration() ).thenReturn(config);
        archivaConfig.save(any(Configuration.class), anyString());
        repositoryRegistry.reload();

    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {
        reset( archivaConfig );
        when( archivaConfig.getDefaultLocale() ).thenReturn( Locale.getDefault( ) );
        when( archivaConfig.getConfiguration() ).thenReturn(config);
        archivaConfig.save(any(Configuration.class), anyString());
        repositoryRegistry.removeRepository(TEST_REPO_1);
        repositoryRegistry.removeRepository(TEST_REPO_2);
        repositoryRegistry.removeRepository(REPO_RELEASE);
        repositoryRegistry.destroy();
        FileUtils.deleteDirectory( Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "/target/repos/" + TEST_REPO_1 ) );
        assertFalse( Files.exists(Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "/target/repos/" + TEST_REPO_1 )) );

        FileUtils.deleteDirectory( Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "/target/repos/" + TEST_REPO_2 ) );
        assertFalse( Files.exists(Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "/target/repos/" + TEST_REPO_2 )) );

        super.tearDown();
    }

    protected ManagedRepositoryConfiguration createRepositoryConfig( String repository )
    {
        ManagedRepositoryConfiguration repositoryConfig = new ManagedRepositoryConfiguration();
        repositoryConfig.setId( repository );
        repositoryConfig.setLocation( org.apache.archiva.common.utils.FileUtils.getBasedir() + "/target/repos/" + repository );
        Path f = Paths.get( repositoryConfig.getLocation() );
        if ( !Files.exists(f) )
        {
            try
            {
                Files.createDirectories( f );
            }
            catch ( IOException e )
            {
                log.error("Could not create directories for {}", f);
            }
        }
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setName( repository );
        repositoryConfig.setScanned( true );
        repositoryConfig.setSnapshots( false );
        repositoryConfig.setReleases( true );
        repositoryConfig.setIndexDir(DEFAULT_INDEX_PATH);

        return repositoryConfig;
    }

    protected void createIndex( String repository, List<Path> filesToBeIndexed, boolean scan) throws Exception {
        createIndex(repository, filesToBeIndexed, scan, null, true);
    }

    protected void createIndex( String repository, List<Path> filesToBeIndexed, boolean scan, Path indexDir, boolean copyFiles)
        throws Exception
    {
        final Repository rRepo = repositoryRegistry.getRepository(repository);
        IndexCreationFeature icf = rRepo.getFeature( IndexCreationFeature.class );


        ArchivaIndexingContext archivaCtx = rRepo.getIndexingContext();
        IndexingContext context = archivaCtx.getBaseContext(IndexingContext.class);

        if ( archivaCtx != null )
        {
            archivaCtx.close(true);
        }

        Path repoDir = Paths.get(org.apache.archiva.common.utils.FileUtils.getBasedir()).resolve("target").resolve("repos").resolve(repository);

        Path indexerDirectory = repoDir.resolve(".indexer" );

        if ( indexDir == null && Files.exists(indexerDirectory) )
        {
            FileUtils.deleteDirectory( indexerDirectory );
            assertFalse( Files.exists(indexerDirectory) );
        }


        Path lockFile = repoDir.resolve(".indexer/write.lock" );
        if ( Files.exists(lockFile) )
        {
            Files.delete(lockFile);
        }
        assertFalse( Files.exists(lockFile) );
        if (indexDir==null) {
            Path indexDirectory =
                    Paths.get(org.apache.archiva.common.utils.FileUtils.getBasedir(), "target/index/test-" + Long.toString(System.currentTimeMillis()));
            indexDirectory.toFile().deleteOnExit();
            FileUtils.deleteDirectory(indexDirectory);
            icf.setIndexPath(indexDirectory.toUri());
            config.getManagedRepositories( ).stream( ).filter( r -> r.getId( ).equals( rRepo.getId( ) ) ).findFirst( ).ifPresent( r ->
                r.setIndexDir( indexDirectory.toAbsolutePath( ).toString( ) )
            );
            // IndexUpgrader.main(new String[]{indexDirectory.toAbsolutePath().toString()});
        } else {

            icf.setIndexPath(indexDir.toUri());
            Files.createDirectories( indexDir );
            config.getManagedRepositories( ).stream( ).filter( r -> r.getId( ).equals( rRepo.getId( ) ) ).findFirst( ).ifPresent( r ->
                r.setIndexDir( indexDir.toAbsolutePath( ).toString( ) )
            );
            IndexUpgrader.main(new String[]{indexDir.toAbsolutePath().toString()});

        }

        if (copyFiles) {
            Path repo = Paths.get(org.apache.archiva.common.utils.FileUtils.getBasedir(), "src/test/" + repository);
            assertTrue(Files.exists(repo));
            org.apache.commons.io.FileUtils.copyDirectory(repo.toFile(), repoDir.toFile());
        }


        reset( archivaConfig );
        when( archivaConfig.getConfiguration() ).thenReturn(config);
        archivaConfig.save(any(Configuration.class), anyString());
        repositoryRegistry.reload();

        Repository rRepo2 = repositoryRegistry.getRepository( repository );
        icf = rRepo2.getFeature( IndexCreationFeature.class );


        archivaCtx = rRepo2.getIndexingContext();
        context = archivaCtx.getBaseContext(IndexingContext.class);


        // minimize datas in memory
//        context.getIndexWriter().setMaxBufferedDocs( -1 );
//        context.getIndexWriter().setRAMBufferSizeMB( 1 );
        for ( Path artifactFile : filesToBeIndexed )
        {
            assertTrue( "file not exists " + artifactFile, Files.exists(artifactFile) );
            ArtifactContext ac = artifactContextProducer.getArtifactContext( context, artifactFile.toFile() );

            if ( artifactFile.toString().endsWith( ".pom" ) )
            {
                ac.getArtifactInfo().setFileExtension( "pom" );
                ac.getArtifactInfo().setPackaging( "pom" );
                ac.getArtifactInfo().setClassifier( "pom" );
            }
            indexer.addArtifactToIndex( ac, context );
            context.updateTimestamp( true );
        }

        if ( scan )
        {
            DefaultScannerListener listener = new DefaultScannerListener( context, indexerEngine, true, new ArtifactScanListener());
            ScanningRequest req = new ScanningRequest(context, listener );
            scanner.scan( req );
            context.commit();
        }
        // force flushing
        context.commit();
        //  context.getIndexWriter().commit();
        context.setSearchable( true );

    }

    static class ArtifactScanListener
        implements ArtifactScanningListener
    {
        protected Logger log = LoggerFactory.getLogger( getClass() );

        @Override
        public void scanningStarted( IndexingContext ctx )
        {
            //
        }

        @Override
        public void scanningFinished( IndexingContext ctx, ScanningResult result )
        {
            // no op
        }

        @Override
        public void artifactError( ArtifactContext ac, Exception e )
        {
            log.debug( "artifactError {}", ac.getArtifact().getPath(), e );
        }

        @Override
        public void artifactDiscovered( ArtifactContext ac )
        {
            log.debug( "artifactDiscovered {}:{}", //
                       ac.getArtifact() == null ? "" : ac.getArtifact().getPath(), //
                       ac.getArtifact() == null ? "" : ac.getArtifactInfo() );
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
