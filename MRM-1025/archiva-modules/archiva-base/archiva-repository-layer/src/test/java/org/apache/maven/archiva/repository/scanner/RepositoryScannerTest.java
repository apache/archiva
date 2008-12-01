package org.apache.maven.archiva.repository.scanner;

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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.repository.AbstractRepositoryLayerTestCase;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * RepositoryScannerTest
 *
 * @version $Id$
 */
public class RepositoryScannerTest
    extends AbstractRepositoryLayerTestCase
{
    private static final String[] ARTIFACT_PATTERNS =
        new String[]{"**/*.jar", "**/*.pom", "**/*.rar", "**/*.zip", "**/*.war", "**/*.tar.gz"};

    private ManagedRepositoryConfiguration createDefaultRepository()
    {
        File repoDir = new File( getBasedir(), "src/test/repositories/default-repository" );

        assertTrue( "Default Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        return createRepository( "testDefaultRepo", "Test Default Repository", repoDir );
    }

    private ManagedRepositoryConfiguration createSimpleRepository()
        throws IOException, ParseException
    {
        File srcDir = new File( getBasedir(), "src/test/repositories/simple-repository" );

        File repoDir = getTestFile( "target/test-repos/simple-repository" );

        FileUtils.deleteDirectory( repoDir );

        FileUtils.copyDirectory( srcDir, repoDir );

        File repoFile = new File( repoDir,
                                  "groupId/snapshot-artifact/1.0-alpha-1-SNAPSHOT/snapshot-artifact-1.0-alpha-1-20050611.202024-1.pom" );
        repoFile.setLastModified( getTimestampAsMillis( "20050611.202024" ) );

        assertTrue( "Simple Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        return createRepository( "testSimpleRepo", "Test Simple Repository", repoDir );
    }

    private static long getTimestampAsMillis( String timestamp )
        throws ParseException
    {
        SimpleDateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss", Locale.US );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return fmt.parse( timestamp ).getTime();
    }

    private ManagedRepositoryConfiguration createLegacyRepository()
    {
        File repoDir = new File( getBasedir(), "src/test/repositories/legacy-repository" );

        assertTrue( "Legacy Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        ManagedRepositoryConfiguration repo = createRepository( "testLegacyRepo", "Test Legacy Repository", repoDir );
        repo.setLayout( "legacy" );

        return repo;
    }

    private void assertMinimumHits( String msg, int minimumHitCount, long actualCount )
    {
        if ( actualCount < minimumHitCount )
        {
            fail( "Minimum hit count on " + msg + " not satisfied.  Expected more than <" + minimumHitCount +
                ">, but actually got <" + actualCount + ">." );
        }
    }

    private RepositoryScanner lookupRepositoryScanner()
        throws Exception
    {
        return (RepositoryScanner) lookup( RepositoryScanner.class );
    }

    private List getIgnoreList()
    {
        List ignores = new ArrayList();
        ignores.addAll( Arrays.asList( RepositoryScanner.IGNORABLE_CONTENT ) );
        return ignores;
    }

    public void testTimestampRepositoryScanner()
        throws Exception
    {
        ManagedRepositoryConfiguration repository = createSimpleRepository();

        List knownConsumers = new ArrayList();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setIncludes( ARTIFACT_PATTERNS );
        knownConsumers.add( consumer );

        List invalidConsumers = new ArrayList();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();

        RepositoryContentStatistics stats = scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(),
                                                          getTimestampAsMillis( "20061101.000000" ) );

        assertNotNull( "Stats should not be null.", stats );
        assertEquals( "Stats.totalFileCount", 4, stats.getTotalFileCount() );
        assertEquals( "Stats.newFileCount", 3, stats.getNewFileCount() );
        assertEquals( "Processed Count", 2, consumer.getProcessCount() );
        assertEquals( "Processed Count (of invalid items)", 1, badconsumer.getProcessCount() );
    }

    public void testTimestampRepositoryScannerFreshScan()
        throws Exception
    {
        ManagedRepositoryConfiguration repository = createSimpleRepository();

        List knownConsumers = new ArrayList();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setIncludes( ARTIFACT_PATTERNS );
        knownConsumers.add( consumer );

        List invalidConsumers = new ArrayList();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryContentStatistics stats =
            scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(), RepositoryScanner.FRESH_SCAN );

        assertNotNull( "Stats should not be null.", stats );
        assertEquals( "Stats.totalFileCount", 4, stats.getTotalFileCount() );
        assertEquals( "Stats.newFileCount", 4, stats.getNewFileCount() );
        assertEquals( "Processed Count", 3, consumer.getProcessCount() );
        assertEquals( "Processed Count (of invalid items)", 1, badconsumer.getProcessCount() );
    }

    public void testTimestampRepositoryScannerProcessUnmodified()
        throws Exception
    {
        ManagedRepositoryConfiguration repository = createSimpleRepository();

        List knownConsumers = new ArrayList();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setProcessUnmodified( true );
        consumer.setIncludes( ARTIFACT_PATTERNS );
        knownConsumers.add( consumer );

        List invalidConsumers = new ArrayList();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryContentStatistics stats = scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(),
                                                          getTimestampAsMillis( "20061101.000000" ) );

        assertNotNull( "Stats should not be null.", stats );
        assertEquals( "Stats.totalFileCount", 4, stats.getTotalFileCount() );
        assertEquals( "Stats.newFileCount", 3, stats.getNewFileCount() );
        assertEquals( "Processed Count", 3, consumer.getProcessCount() );
        assertEquals( "Processed Count (of invalid items)", 1, badconsumer.getProcessCount() );
    }

    public void testDefaultRepositoryScanner()
        throws Exception
    {
        ManagedRepositoryConfiguration repository = createDefaultRepository();

        List knownConsumers = new ArrayList();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setIncludes( new String[]{"**/*.jar", "**/*.war", "**/*.pom", "**/maven-metadata.xml", "**/*-site.xml",
            "**/*.zip", "**/*.tar.gz", "**/*.sha1", "**/*.md5"} );
        knownConsumers.add( consumer );

        List invalidConsumers = new ArrayList();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryContentStatistics stats =
            scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(), RepositoryScanner.FRESH_SCAN );

        assertNotNull( "Stats should not be null.", stats );
        assertMinimumHits( "Stats.totalFileCount", 17, stats.getTotalFileCount() );
        assertMinimumHits( "Processed Count", 17, consumer.getProcessCount() );
        assertEquals( "Processed Count (of invalid items)", 6, badconsumer.getProcessCount() );
    }

    public void testDefaultRepositoryArtifactScanner()
        throws Exception
    {
        List actualArtifactPaths = new ArrayList();

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

        ManagedRepositoryConfiguration repository = createDefaultRepository();

        List knownConsumers = new ArrayList();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setIncludes( ARTIFACT_PATTERNS );
        knownConsumers.add( consumer );

        List invalidConsumers = new ArrayList();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryContentStatistics stats =
            scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(), RepositoryScanner.FRESH_SCAN );

        assertNotNull( "Stats should not be null.", stats );
        assertMinimumHits( "Stats.totalFileCount", actualArtifactPaths.size(), stats.getTotalFileCount() );
        assertMinimumHits( "Processed Count", actualArtifactPaths.size(), consumer.getProcessCount() );
    }

    public void testDefaultRepositoryMetadataScanner()
        throws Exception
    {
        List actualMetadataPaths = new ArrayList();

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

        ManagedRepositoryConfiguration repository = createDefaultRepository();

        List knownConsumers = new ArrayList();
        KnownScanConsumer knownConsumer = new KnownScanConsumer();
        knownConsumer.setIncludes( new String[]{"**/maven-metadata*.xml"} );
        knownConsumers.add( knownConsumer );

        List invalidConsumers = new ArrayList();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryContentStatistics stats =
            scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(), RepositoryScanner.FRESH_SCAN );

        assertNotNull( "Stats should not be null.", stats );
        assertMinimumHits( "Stats.totalFileCount", actualMetadataPaths.size(), stats.getTotalFileCount() );
        assertMinimumHits( "Processed Count", actualMetadataPaths.size(), knownConsumer.getProcessCount() );
    }

    public void testDefaultRepositoryProjectScanner()
        throws Exception
    {
        List actualProjectPaths = new ArrayList();

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

        ManagedRepositoryConfiguration repository = createDefaultRepository();

        List knownConsumers = new ArrayList();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setIncludes( new String[]{"**/*.pom"} );
        knownConsumers.add( consumer );

        List invalidConsumers = new ArrayList();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryContentStatistics stats =
            scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(), RepositoryScanner.FRESH_SCAN );

        assertNotNull( "Stats should not be null.", stats );
        assertMinimumHits( "Stats.totalFileCount", actualProjectPaths.size(), stats.getTotalFileCount() );
        assertMinimumHits( "Processed Count", actualProjectPaths.size(), consumer.getProcessCount() );
    }

    public void testLegacyRepositoryArtifactScanner()
        throws Exception
    {
        List actualArtifactPaths = new ArrayList();

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

        ManagedRepositoryConfiguration repository = createLegacyRepository();

        List knownConsumers = new ArrayList();
        KnownScanConsumer consumer = new KnownScanConsumer();
        consumer.setIncludes( ARTIFACT_PATTERNS );
        knownConsumers.add( consumer );

        List invalidConsumers = new ArrayList();
        InvalidScanConsumer badconsumer = new InvalidScanConsumer();
        invalidConsumers.add( badconsumer );

        RepositoryScanner scanner = lookupRepositoryScanner();
        RepositoryContentStatistics stats =
            scanner.scan( repository, knownConsumers, invalidConsumers, getIgnoreList(), RepositoryScanner.FRESH_SCAN );

        assertNotNull( "Stats should not be null.", stats );
        assertMinimumHits( "Stats.totalFileCount", actualArtifactPaths.size(), stats.getTotalFileCount() );
        assertMinimumHits( "Processed Count", actualArtifactPaths.size(), consumer.getProcessCount() );
    }
}
