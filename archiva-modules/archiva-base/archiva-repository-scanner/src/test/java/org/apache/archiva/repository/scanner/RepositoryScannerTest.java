package org.apache.archiva.repository.scanner;

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
import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.repository.base.BasicManagedRepository;
import org.apache.archiva.repository.base.BasicRemoteRepository;
import org.apache.archiva.repository.EditableManagedRepository;
import org.apache.archiva.repository.EditableRemoteRepository;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.scanner.mock.ManagedRepositoryContentMock;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * RepositoryScannerTest
 */
@RunWith(ArchivaSpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:/META-INF/spring-context.xml" })
public class RepositoryScannerTest
    extends TestCase
{

    @Inject
    ApplicationContext applicationContext;

    Path repoBaseDir;

    private Path getRepoBaseDir() {
        if (repoBaseDir==null) {
            try
            {
                repoBaseDir =Paths.get(Thread.currentThread( ).getContextClassLoader( ).getResource( "repositories" ).toURI());
            }
            catch ( URISyntaxException e )
            {
                throw new RuntimeException( "Could not retrieve repository base directory" );
            }
        }
        return repoBaseDir;
    }

    protected EditableManagedRepository createRepository( String id, String name, Path location ) throws IOException {
        FileLockManager lockManager = new DefaultFileLockManager();
        FilesystemStorage storage = new FilesystemStorage(location.toAbsolutePath(), lockManager);
        BasicManagedRepository repo = new BasicManagedRepository(id, name, storage);
        repo.setLocation( location.toAbsolutePath().toUri());
        repo.setContent(new ManagedRepositoryContentMock(repo));
        return repo;
    }

    protected EditableRemoteRepository createRemoteRepository( String id, String name, String url ) throws URISyntaxException, IOException {
        BasicRemoteRepository repo = BasicRemoteRepository.newFilesystemInstance(id, name, Paths.get("remotes"));
        repo.setLocation( new URI( url ) );
        return repo;
    }

    private static final String[] ARTIFACT_PATTERNS =
        new String[]{ "**/*.jar", "**/*.pom", "**/*.rar", "**/*.zip", "**/*.war", "**/*.tar.gz" };

    private ManagedRepository createDefaultRepository() throws IOException, URISyntaxException
    {
        Path repoDir = getRepoBaseDir().resolve("default-repository" );

        assertTrue( "Default Test Repository should exist.", Files.exists(repoDir) && Files.isDirectory(repoDir) );

        return createRepository( "testDefaultRepo", "Test Default Repository", repoDir );
    }

    private ManagedRepository createSimpleRepository()
        throws IOException, ParseException
    {
        Path srcDir = getRepoBaseDir().resolve("simple-repository" );

        Path repoDir = Paths.get( System.getProperty( "basedir" ),  "target/test-repos/simple-repository" );

        org.apache.archiva.common.utils.FileUtils.deleteDirectory( repoDir );

        FileUtils.copyDirectory( srcDir.toFile(), repoDir.toFile() );

        Path repoFile = repoDir.resolve(
                                  "groupId/snapshot-artifact/1.0-alpha-1-SNAPSHOT/snapshot-artifact-1.0-alpha-1-20050611.202024-1.pom" );
        Files.setLastModifiedTime(repoFile, FileTime.fromMillis(getTimestampAsMillis( "20050611.202024" ) ));

        assertTrue( "Simple Test Repository should exist.", Files.exists(repoDir) && Files.isDirectory(repoDir) );

        return createRepository( "testSimpleRepo", "Test Simple Repository", repoDir );
    }

    private static long getTimestampAsMillis( String timestamp )
        throws ParseException
    {
        SimpleDateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss", Locale.US );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return fmt.parse( timestamp ).getTime();
    }

    private ManagedRepository createLegacyRepository() throws IOException {
        Path repoDir = getRepoBaseDir().resolve("legacy-repository" );

        assertTrue( "Legacy Test Repository should exist.", Files.exists(repoDir) && Files.isDirectory(repoDir) );

        EditableManagedRepository repo = createRepository( "testLegacyRepo", "Test Legacy Repository", repoDir );
        repo.setLayout( "legacy" );

        return repo;
    }

    private void assertMinimumHits( String msg, int minimumHitCount, long actualCount )
    {
        if ( actualCount < minimumHitCount )
        {
            fail( "Minimum hit count on " + msg + " not satisfied.  Expected more than <" + minimumHitCount
                      + ">, but actually got <" + actualCount + ">." );
        }
    }

    private RepositoryScanner lookupRepositoryScanner()
        throws Exception
    {
        return applicationContext.getBean( RepositoryScanner.class );
    }

    private List<String> getIgnoreList()
    {
        List<String> ignores = new ArrayList<>();
        ignores.addAll( Arrays.asList( RepositoryScanner.IGNORABLE_CONTENT ) );
        return ignores;
    }

    @Test
    public void testTimestampRepositoryScanner()
        throws Exception
    {
        ManagedRepository repository = createSimpleRepository();

        List<KnownRepositoryContentConsumer> knownConsumers = new ArrayList<>();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setIncludes( ARTIFACT_PATTERNS );
        knownConsumers.add( consumer );

        List<InvalidRepositoryContentConsumer> invalidConsumers = new ArrayList<>();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();

        RepositoryScanStatistics stats = scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(),
                                                       getTimestampAsMillis( "20061101.000000" ) );

        assertNotNull( "Stats should not be null.", stats );
        assertEquals( "Stats.totalFileCount", 4, stats.getTotalFileCount() );
        assertEquals( "Stats.newFileCount", 3, stats.getNewFileCount() );
        assertEquals( "Processed Count", 2, consumer.getProcessCount() );
        assertEquals( "Processed Count (of invalid items)", 1, badconsumer.getProcessCount() );
    }

    @Test
    public void testTimestampRepositoryScannerFreshScan()
        throws Exception
    {
        ManagedRepository repository = createSimpleRepository();

        List<KnownRepositoryContentConsumer> knownConsumers = new ArrayList<>();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setIncludes( ARTIFACT_PATTERNS );
        knownConsumers.add( consumer );

        List<InvalidRepositoryContentConsumer> invalidConsumers = new ArrayList<>();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryScanStatistics stats =
            scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(), RepositoryScanner.FRESH_SCAN );

        assertNotNull( "Stats should not be null.", stats );
        assertEquals( "Stats.totalFileCount", 4, stats.getTotalFileCount() );
        assertEquals( "Stats.newFileCount", 4, stats.getNewFileCount() );
        assertEquals( "Processed Count", 3, consumer.getProcessCount() );
        assertEquals( "Processed Count (of invalid items)", 1, badconsumer.getProcessCount() );
    }

    @Test
    public void testTimestampRepositoryScannerProcessUnmodified()
        throws Exception
    {
        ManagedRepository repository = createSimpleRepository();

        List<KnownRepositoryContentConsumer> knownConsumers = new ArrayList<>();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setProcessUnmodified( true );
        consumer.setIncludes( ARTIFACT_PATTERNS );
        knownConsumers.add( consumer );

        List<InvalidRepositoryContentConsumer> invalidConsumers = new ArrayList<>();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryScanStatistics stats = scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(),
                                                       getTimestampAsMillis( "20061101.000000" ) );

        assertNotNull( "Stats should not be null.", stats );
        assertEquals( "Stats.totalFileCount", 4, stats.getTotalFileCount() );
        assertEquals( "Stats.newFileCount", 3, stats.getNewFileCount() );
        assertEquals( "Processed Count", 3, consumer.getProcessCount() );
        assertEquals( "Processed Count (of invalid items)", 1, badconsumer.getProcessCount() );
    }

    @Test
    public void testDefaultRepositoryScanner()
        throws Exception
    {
        ManagedRepository repository = createDefaultRepository();

        List<KnownRepositoryContentConsumer> knownConsumers = new ArrayList<>();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setIncludes(
            new String[]{ "**/*.jar", "**/*.war", "**/*.pom", "**/maven-metadata.xml", "**/*-site.xml", "**/*.zip",
                "**/*.tar.gz", "**/*.sha1", "**/*.md5" }
        );
        knownConsumers.add( consumer );

        List<InvalidRepositoryContentConsumer> invalidConsumers = new ArrayList<>();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryScanStatistics stats =
            scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(), RepositoryScanner.FRESH_SCAN );

        assertNotNull( "Stats should not be null.", stats );
        assertMinimumHits( "Stats.totalFileCount", 17, stats.getTotalFileCount() );
        assertMinimumHits( "Processed Count", 17, consumer.getProcessCount() );
        assertEquals( "Processed Count (of invalid items):" + badconsumer.getPaths(), 6, badconsumer.getProcessCount() );
        List<String> paths = new ArrayList<>( badconsumer.getPaths() );
        paths.sort( Comparator.naturalOrder() );
        List<String> expected = Arrays.asList(
            "CVS/Root",
            "invalid/invalid/1/invalid-1",
            "javax/sql/jdbc/2.0/maven-metadata-repository.xml",
            "javax/sql/jdbc/maven-metadata-repository.xml",
            "javax/sql/maven-metadata-repository.xml",
            "org/apache/maven/maven-parent/4/maven-parent-4-site_en.xml");
        assertThat( paths, is( expected ) );
    }

    @Test
    public void testDefaultRepositoryArtifactScanner()
        throws Exception
    {
        List<String> actualArtifactPaths = new ArrayList<>();

        actualArtifactPaths.add( "invalid/invalid/1.0-20050611.123456-1/invalid-1.0-20050611.123456-1.jar" );
        actualArtifactPaths.add( "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar" );
        actualArtifactPaths.add( "invalid/invalid/1.0/invalid-1.0b.jar" );
        actualArtifactPaths.add( "invalid/invalid/1.0/invalid-2.0.jar" );
        actualArtifactPaths.add( "invalid/invalid-1.0.jar" );
        actualArtifactPaths.add( "org/apache/maven/test/1.0-SNAPSHOT/wrong-artifactId-1.0-20050611.112233-1.jar" );
        actualArtifactPaths.add( "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-20050611.112233-1-javadoc.jar" );
        actualArtifactPaths.add( "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-20050611.112233-1.jar" );
        actualArtifactPaths.add( "org/apache/maven/A/1.0/A-1.0.war" );
        actualArtifactPaths.add( "org/apache/maven/A/1.0/A-1.0.pom" );
        actualArtifactPaths.add( "org/apache/maven/B/2.0/B-2.0.pom" );
        actualArtifactPaths.add( "org/apache/maven/B/1.0/B-1.0.pom" );
        actualArtifactPaths.add( "org/apache/maven/some-ejb/1.0/some-ejb-1.0-client.jar" );
        actualArtifactPaths.add( "org/apache/maven/C/1.0/C-1.0.war" );
        actualArtifactPaths.add( "org/apache/maven/C/1.0/C-1.0.pom" );
        actualArtifactPaths.add( "org/apache/maven/update/test-not-updated/1.0/test-not-updated-1.0.pom" );
        actualArtifactPaths.add( "org/apache/maven/update/test-not-updated/1.0/test-not-updated-1.0.jar" );
        actualArtifactPaths.add( "org/apache/maven/update/test-updated/1.0/test-updated-1.0.pom" );
        actualArtifactPaths.add( "org/apache/maven/update/test-updated/1.0/test-updated-1.0.jar" );
        actualArtifactPaths.add( "org/apache/maven/discovery/1.0/discovery-1.0.pom" );
        actualArtifactPaths.add( "org/apache/maven/testing/1.0/testing-1.0-test-sources.jar" );
        actualArtifactPaths.add( "org/apache/maven/testing/1.0/testing-1.0.jar" );
        actualArtifactPaths.add( "org/apache/maven/testing/1.0/testing-1.0-sources.jar" );
        actualArtifactPaths.add( "org/apache/maven/testing/1.0/testing-1.0.zip" );
        actualArtifactPaths.add( "org/apache/maven/testing/1.0/testing-1.0.tar.gz" );
        actualArtifactPaths.add( "org/apache/maven/samplejar/2.0/samplejar-2.0.pom" );
        actualArtifactPaths.add( "org/apache/maven/samplejar/2.0/samplejar-2.0.jar" );
        actualArtifactPaths.add( "org/apache/maven/samplejar/1.0/samplejar-1.0.pom" );
        actualArtifactPaths.add( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar" );
        actualArtifactPaths.add( "org/apache/testgroup/discovery/1.0/discovery-1.0.pom" );
        actualArtifactPaths.add( "javax/sql/jdbc/2.0/jdbc-2.0.jar" );

        ManagedRepository repository = createDefaultRepository();

        List<KnownRepositoryContentConsumer> knownConsumers = new ArrayList<>();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setIncludes( ARTIFACT_PATTERNS );
        knownConsumers.add( consumer );

        List<InvalidRepositoryContentConsumer> invalidConsumers = new ArrayList<>();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryScanStatistics stats =
            scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(), RepositoryScanner.FRESH_SCAN );

        assertNotNull( "Stats should not be null.", stats );
        assertMinimumHits( "Stats.totalFileCount", actualArtifactPaths.size(), stats.getTotalFileCount() );
        assertMinimumHits( "Processed Count", actualArtifactPaths.size(), consumer.getProcessCount() );
    }

    @Test
    public void testDefaultRepositoryMetadataScanner()
        throws Exception
    {
        List<String> actualMetadataPaths = new ArrayList<>();

        actualMetadataPaths.add( "org/apache/maven/some-ejb/1.0/maven-metadata.xml" );
        actualMetadataPaths.add( "org/apache/maven/update/test-not-updated/maven-metadata.xml" );
        actualMetadataPaths.add( "org/apache/maven/update/test-updated/maven-metadata.xml" );
        actualMetadataPaths.add( "org/apache/maven/maven-metadata.xml" );
        actualMetadataPaths.add( "org/apache/testgroup/discovery/1.0/maven-metadata.xml" );
        actualMetadataPaths.add( "org/apache/testgroup/discovery/maven-metadata.xml" );
        actualMetadataPaths.add( "javax/sql/jdbc/2.0/maven-metadata-repository.xml" );
        actualMetadataPaths.add( "javax/sql/jdbc/maven-metadata-repository.xml" );
        actualMetadataPaths.add( "javax/sql/maven-metadata-repository.xml" );
        actualMetadataPaths.add( "javax/maven-metadata.xml" );

        ManagedRepository repository = createDefaultRepository();

        List<KnownRepositoryContentConsumer> knownConsumers = new ArrayList<>();
        KnownScanConsumer knownConsumer = new KnownScanConsumer();
        knownConsumer.setIncludes( new String[]{ "**/maven-metadata*.xml" } );
        knownConsumers.add( knownConsumer );

        List<InvalidRepositoryContentConsumer> invalidConsumers = new ArrayList<>();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryScanStatistics stats =
            scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(), RepositoryScanner.FRESH_SCAN );

        assertNotNull( "Stats should not be null.", stats );
        assertMinimumHits( "Stats.totalFileCount", actualMetadataPaths.size(), stats.getTotalFileCount() );
        assertMinimumHits( "Processed Count", actualMetadataPaths.size(), knownConsumer.getProcessCount() );
    }

    @Test
    public void testDefaultRepositoryProjectScanner()
        throws Exception
    {
        List<String> actualProjectPaths = new ArrayList<>();

        actualProjectPaths.add( "org/apache/maven/A/1.0/A-1.0.pom" );
        actualProjectPaths.add( "org/apache/maven/B/2.0/B-2.0.pom" );
        actualProjectPaths.add( "org/apache/maven/B/1.0/B-1.0.pom" );
        actualProjectPaths.add( "org/apache/maven/C/1.0/C-1.0.pom" );
        actualProjectPaths.add( "org/apache/maven/update/test-not-updated/1.0/test-not-updated-1.0.pom" );
        actualProjectPaths.add( "org/apache/maven/update/test-updated/1.0/test-updated-1.0.pom" );
        actualProjectPaths.add( "org/apache/maven/discovery/1.0/discovery-1.0.pom" );
        actualProjectPaths.add( "org/apache/maven/samplejar/2.0/samplejar-2.0.pom" );
        actualProjectPaths.add( "org/apache/maven/samplejar/1.0/samplejar-1.0.pom" );
        actualProjectPaths.add( "org/apache/testgroup/discovery/1.0/discovery-1.0.pom" );

        ManagedRepository repository = createDefaultRepository();

        List<KnownRepositoryContentConsumer> knownConsumers = new ArrayList<>();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setIncludes( new String[]{ "**/*.pom" } );
        knownConsumers.add( consumer );

        List<InvalidRepositoryContentConsumer> invalidConsumers = new ArrayList<>();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryScanStatistics stats =
            scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(), RepositoryScanner.FRESH_SCAN );

        assertNotNull( "Stats should not be null.", stats );
        assertMinimumHits( "Stats.totalFileCount", actualProjectPaths.size(), stats.getTotalFileCount() );
        assertMinimumHits( "Processed Count", actualProjectPaths.size(), consumer.getProcessCount() );
    }

    @Test
    public void testLegacyRepositoryArtifactScanner()
        throws Exception
    {
        List<String> actualArtifactPaths = new ArrayList<>();

        actualArtifactPaths.add( "invalid/jars/1.0/invalid-1.0.jar" );
        actualArtifactPaths.add( "invalid/jars/invalid-1.0.rar" );
        actualArtifactPaths.add( "invalid/jars/invalid.jar" );
        actualArtifactPaths.add( "invalid/invalid-1.0.jar" );
        actualArtifactPaths.add( "javax.sql/jars/jdbc-2.0.jar" );
        actualArtifactPaths.add( "org.apache.maven/jars/some-ejb-1.0-client.jar" );
        actualArtifactPaths.add( "org.apache.maven/jars/testing-1.0.jar" );
        actualArtifactPaths.add( "org.apache.maven/jars/testing-1.0-sources.jar" );
        actualArtifactPaths.add( "org.apache.maven/jars/testing-UNKNOWN.jar" );
        actualArtifactPaths.add( "org.apache.maven/jars/testing-1.0.zip" );
        actualArtifactPaths.add( "org.apache.maven/jars/testing-1.0-20050611.112233-1.jar" );
        actualArtifactPaths.add( "org.apache.maven/jars/testing-1.0.tar.gz" );
        actualArtifactPaths.add( "org.apache.maven.update/jars/test-not-updated-1.0.jar" );
        actualArtifactPaths.add( "org.apache.maven.update/jars/test-updated-1.0.jar" );

        ManagedRepository repository = createLegacyRepository();

        List<KnownRepositoryContentConsumer> knownConsumers = new ArrayList<>();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setIncludes( ARTIFACT_PATTERNS );
        knownConsumers.add( consumer );

        List<InvalidRepositoryContentConsumer> invalidConsumers = new ArrayList<>();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryScanStatistics stats =
            scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(), RepositoryScanner.FRESH_SCAN );

        assertNotNull( "Stats should not be null.", stats );
        assertMinimumHits( "Stats.totalFileCount", actualArtifactPaths.size(), stats.getTotalFileCount() );
        assertMinimumHits( "Processed Count", actualArtifactPaths.size(), consumer.getProcessCount() );
    }
}
