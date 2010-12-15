package org.apache.maven.archiva.consumers.core.repository;

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
import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.FileType;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.scanner.functors.ConsumerWantsFilePredicate;
import org.custommonkey.xmlunit.XMLAssert;

import java.io.File;

/**
 */
public class RepositoryPurgeConsumerTest
    extends AbstractRepositoryPurgeTest
{
    public void testConsumption()
        throws Exception
    {
        assertNotConsumed( "org/apache/maven/plugins/maven-plugin-plugin/2.4.1/maven-metadata.xml" );
    }

    public void testConsumptionOfOtherMetadata()
        throws Exception
    {
        assertNotConsumed( "org/apache/maven/plugins/maven-plugin-plugin/2.4.1/maven-metadata-central.xml" );
    }

    private void assertNotConsumed( String path )
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration = (ArchivaConfiguration) lookup( ArchivaConfiguration.ROLE );
        FileType fileType =
            (FileType) archivaConfiguration.getConfiguration().getRepositoryScanning().getFileTypes().get( 0 );
        assertEquals( FileTypes.ARTIFACTS, fileType.getId() );
        fileType.addPattern( "**/*.xml" );

        // trigger reload
        FileTypes fileTypes = (FileTypes) lookup( FileTypes.class );
        fileTypes.afterConfigurationChange( null, "repositoryScanning.fileTypes", null );

        KnownRepositoryContentConsumer repoPurgeConsumer =
            (KnownRepositoryContentConsumer) lookup( KnownRepositoryContentConsumer.class, "repository-purge" );

        File repoLocation = getTestFile( "target/test-" + getName() + "/test-repo" );

        File localFile =
            new File( repoLocation, path );

        ConsumerWantsFilePredicate predicate = new ConsumerWantsFilePredicate();
        BaseFile baseFile = new BaseFile( repoLocation, localFile );
        predicate.setBasefile( baseFile );

        assertFalse( predicate.evaluate( repoPurgeConsumer ) );
    }

    private void setLastModified( String path )
    {
        File dir = new File( path );
        File[] contents = dir.listFiles();
        for ( int i = 0; i < contents.length; i++ )
        {
            contents[i].setLastModified( 1179382029 ); 
        }
    }

    public void testConsumerByRetentionCount()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer =
            (KnownRepositoryContentConsumer) lookup( KnownRepositoryContentConsumer.class,
                                                     "repo-purge-consumer-by-retention-count" );

        ManagedRepositoryConfiguration repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        repoConfiguration.setDaysOlder( 0 ); // force days older off to allow retention count purge to execute.
        repoConfiguration.setRetentionCount( TEST_RETENTION_COUNT );
        addRepoToConfiguration( "retention-count", repoConfiguration );

        repoPurgeConsumer.beginScan( repoConfiguration, null );

        String repoRoot = prepareTestRepos();

        repoPurgeConsumer.processFile( PATH_TO_BY_RETENTION_COUNT_ARTIFACT );

        String versionRoot = repoRoot + "/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT";

        // assert if removed from repo
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar.sha1" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom.sha1" );

        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar.sha1" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom.sha1" );

        // assert if not removed from repo
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar.md5" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar.sha1" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom.md5" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.pom.sha1" );

        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar.md5" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.jar.sha1" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom.md5" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070506.090132-4.pom.sha1" );
    }

    private void addRepoToConfiguration( String configHint, ManagedRepositoryConfiguration repoConfiguration )
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            (ArchivaConfiguration) lookup( ArchivaConfiguration.class, configHint );
        Configuration configuration = archivaConfiguration.getConfiguration();
        configuration.removeManagedRepository( configuration.findManagedRepositoryById( repoConfiguration.getId() ) );
        configuration.addManagedRepository( repoConfiguration );
    }

    public void testConsumerByDaysOld()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer =
            (KnownRepositoryContentConsumer) lookup( KnownRepositoryContentConsumer.class,
                                                     "repo-purge-consumer-by-days-old" );

        ManagedRepositoryConfiguration repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        repoConfiguration.setDaysOlder( TEST_DAYS_OLDER );
        addRepoToConfiguration( "days-old", repoConfiguration );

        repoPurgeConsumer.beginScan( repoConfiguration, null );

        String repoRoot = prepareTestRepos();
        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-install-plugin";

        setLastModified( projectRoot + "/2.2-SNAPSHOT" );

        repoPurgeConsumer.processFile( PATH_TO_BY_DAYS_OLD_ARTIFACT );

        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar.md5" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.jar.sha1" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom.md5" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-SNAPSHOT.pom.sha1" );

        // shouldn't be deleted because even if older than 30 days (because retention count = 2)
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070513.034619-5.jar" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070513.034619-5.jar.md5" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070513.034619-5.jar.sha1" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070513.034619-5.pom" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070513.034619-5.pom.md5" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070513.034619-5.pom.sha1" );
        
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070510.010101-4.jar" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070510.010101-4.jar.md5" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070510.010101-4.jar.sha1" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070510.010101-4.pom" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070510.010101-4.pom.md5" );
        assertExists( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20070510.010101-4.pom.sha1" );

        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.jar" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.jar.md5" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.jar.sha1" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.pom" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.pom.md5" );
        assertDeleted( projectRoot + "/2.2-SNAPSHOT/maven-install-plugin-2.2-20061118.060401-2.pom.sha1" );
    }

    /**
     * Test the snapshot clean consumer on a repository set to NOT clean/delete snapshots based on released versions.
     *
     * @throws Exception
     */
    public void testReleasedSnapshotsWereNotCleaned()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer =
            (KnownRepositoryContentConsumer) lookup( KnownRepositoryContentConsumer.class,
                                                     "repo-purge-consumer-by-retention-count" );

        ManagedRepositoryConfiguration repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        repoConfiguration.setDeleteReleasedSnapshots( false ); // Set to NOT delete released snapshots.
        addRepoToConfiguration( "retention-count", repoConfiguration );

        repoPurgeConsumer.beginScan( repoConfiguration, null );

        String repoRoot = prepareTestRepos();

        repoPurgeConsumer.processFile( CleanupReleasedSnapshotsRepositoryPurgeTest.PATH_TO_RELEASED_SNAPSHOT_IN_SAME_REPO );

        // check if the snapshot wasn't removed
        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-plugin-plugin";

        assertExists( projectRoot + "/2.3-SNAPSHOT" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.md5" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.sha1" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.md5" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.sha1" );

        // check if metadata file wasn't updated
        File artifactMetadataFile = new File( projectRoot + "/maven-metadata.xml" );

        String metadataXml = FileUtils.readFileToString( artifactMetadataFile, null );

        String expectedVersions = "<expected><versions><version>2.3-SNAPSHOT</version></versions></expected>";

        XMLAssert.assertXpathEvaluatesTo( "2.3-SNAPSHOT", "//metadata/versioning/latest", metadataXml );
        XMLAssert.assertXpathsEqual( "//expected/versions/version", expectedVersions,
                                     "//metadata/versioning/versions/version", metadataXml );
        XMLAssert.assertXpathEvaluatesTo( "20070315032817", "//metadata/versioning/lastUpdated", metadataXml );
    }

    public void testReleasedSnapshotsWereCleaned()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer =
            (KnownRepositoryContentConsumer) lookup( KnownRepositoryContentConsumer.class,
                                                     "repo-purge-consumer-by-days-old" );

        ManagedRepositoryConfiguration repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        repoConfiguration.setDeleteReleasedSnapshots( true );
        addRepoToConfiguration( "days-old", repoConfiguration );

        repoPurgeConsumer.beginScan( repoConfiguration, null );

        String repoRoot = prepareTestRepos();

        repoPurgeConsumer.processFile( CleanupReleasedSnapshotsRepositoryPurgeTest.PATH_TO_RELEASED_SNAPSHOT_IN_SAME_REPO );

        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-plugin-plugin";

        // check if the snapshot was removed
        assertDeleted( projectRoot + "/2.3-SNAPSHOT" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.md5" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.sha1" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.md5" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.sha1" );

        // check if metadata file was updated
        File artifactMetadataFile = new File( projectRoot + "/maven-metadata.xml" );

        String metadataXml = FileUtils.readFileToString( artifactMetadataFile, null );

        String expectedVersions =
            "<expected><versions><version>2.2</version>" + "<version>2.3</version></versions></expected>";

        XMLAssert.assertXpathEvaluatesTo( "2.3", "//metadata/versioning/latest", metadataXml );
        XMLAssert.assertXpathsEqual( "//expected/versions/version", expectedVersions,
                                     "//metadata/versioning/versions/version", metadataXml );
        XMLAssert.assertXpathEvaluatesTo( "20070315032817", "//metadata/versioning/lastUpdated", metadataXml );
    }
}
