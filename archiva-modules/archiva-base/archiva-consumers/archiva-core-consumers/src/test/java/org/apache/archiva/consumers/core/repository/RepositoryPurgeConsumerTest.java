package org.apache.archiva.consumers.core.repository;

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

import org.apache.archiva.admin.model.RepositoryCommonValidator;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.DefaultRepositoryCommonValidator;
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.common.utils.BaseFile;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.FileType;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.consumers.functors.ConsumerWantsFilePredicate;
import org.apache.archiva.mock.MockRepositorySessionFactory;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 */
@ContextConfiguration (
    locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context-purge-consumer-test.xml" } )
public class RepositoryPurgeConsumerTest
    extends AbstractRepositoryPurgeTest
{
    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        MockRepositorySessionFactory factory = applicationContext.getBean( MockRepositorySessionFactory.class );
        factory.setRepository( metadataRepository );
    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    @Test
    public void testConsumption()
        throws Exception
    {
        assertNotConsumed( "org/apache/maven/plugins/maven-plugin-plugin/2.4.1/maven-metadata.xml" );
        cleanupFileTypes();
    }

    @Test
    public void testConsumptionOfOtherMetadata()
        throws Exception
    {
        assertNotConsumed( "org/apache/maven/plugins/maven-plugin-plugin/2.4.1/maven-metadata-central.xml" );
        cleanupFileTypes();
    }

    private void cleanupFileTypes()
    {
        ArchivaConfiguration archivaConfiguration =
            applicationContext.getBean( "archivaConfiguration#default", ArchivaConfiguration.class );

        FileType fileType = archivaConfiguration.getConfiguration().getRepositoryScanning().getFileTypes().get( 0 );
        fileType.removePattern( "**/*.xml" );
    }

    private void assertNotConsumed( String path )
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            applicationContext.getBean( "archivaConfiguration#default", ArchivaConfiguration.class );

        FileType fileType = archivaConfiguration.getConfiguration().getRepositoryScanning().getFileTypes().get( 0 );
        assertEquals( FileTypes.ARTIFACTS, fileType.getId() );
        fileType.addPattern( "**/*.xml" );

        // trigger reload
        //FileTypes fileTypes = applicationContext.getBean( FileTypes.class );
        for ( FileTypes fileTypes : applicationContext.getBeansOfType( FileTypes.class ).values() )
        {
            fileTypes.afterConfigurationChange( null, "repositoryScanning.fileTypes", null );
        }
        KnownRepositoryContentConsumer repoPurgeConsumer =
            applicationContext.getBean( "knownRepositoryContentConsumer#repository-purge",
                                        KnownRepositoryContentConsumer.class );

        File repoLocation = new File( "target/test-" + getName() + "/test-repo" );

        File localFile = new File( repoLocation, path );

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

    @Test
    public void testConsumerByRetentionCount()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer =
            applicationContext.getBean( "knownRepositoryContentConsumer#repo-purge-consumer-by-retention-count",
                                        KnownRepositoryContentConsumer.class );

        ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        repoConfiguration.setDaysOlder( 0 ); // force days older off to allow retention count purge to execute.
        repoConfiguration.setRetentionCount( TEST_RETENTION_COUNT );
        addRepoToConfiguration( "retention-count", repoConfiguration );

        repoPurgeConsumer.beginScan( repoConfiguration, null );

        String repoRoot = prepareTestRepos();

        repoPurgeConsumer.processFile( PATH_TO_BY_RETENTION_COUNT_ARTIFACT );

        String versionRoot = repoRoot + "/org/jruby/plugins/jruby-rake-plugin/1.0RC1-SNAPSHOT";

        // assert if removed from repo
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1-javadoc.jar" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1-javadoc.zip" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.jar.sha1" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.153317-1.pom.sha1" );

        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2-javadoc.jar" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2-javadoc.zip" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.jar.sha1" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom.md5" );
        assertDeleted( versionRoot + "/jruby-rake-plugin-1.0RC1-20070504.160758-2.pom.sha1" );

        // assert if not removed from repo
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3.jar" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3-javadoc.jar" );
        assertExists( versionRoot + "/jruby-rake-plugin-1.0RC1-20070505.090015-3-javadoc.zip" );
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

        removeRepoFromConfiguration( "retention-count", repoConfiguration );
    }

    private void addRepoToConfiguration( String configHint, ManagedRepository repoConfiguration )
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            applicationContext.getBean( "archivaConfiguration#" + configHint, ArchivaConfiguration.class );
        ( (DefaultManagedRepositoryAdmin) applicationContext.getBean(
            ManagedRepositoryAdmin.class ) ).setArchivaConfiguration( archivaConfiguration );
        // skygo: Default Validator was not looking at same config
        ( (DefaultRepositoryCommonValidator) applicationContext.getBean(
            RepositoryCommonValidator.class ) ).setArchivaConfiguration( archivaConfiguration );
        ManagedRepositoryAdmin managedRepositoryAdmin = applicationContext.getBean( ManagedRepositoryAdmin.class );
        if ( managedRepositoryAdmin.getManagedRepository( repoConfiguration.getId() ) != null )
        {
            managedRepositoryAdmin.deleteManagedRepository( repoConfiguration.getId(), null, false );
        }
        managedRepositoryAdmin.addManagedRepository( repoConfiguration, false, null );
    }

    private void removeRepoFromConfiguration( String configHint, ManagedRepository repoConfiguration )
        throws Exception
    {
        ArchivaConfiguration archivaConfiguration =
            applicationContext.getBean( "archivaConfiguration#" + configHint, ArchivaConfiguration.class );

        ( (DefaultManagedRepositoryAdmin) applicationContext.getBean(
            ManagedRepositoryAdmin.class ) ).setArchivaConfiguration( archivaConfiguration );
        // skygo: Default Validator was not looking at same config
        ( (DefaultRepositoryCommonValidator) applicationContext.getBean(
            RepositoryCommonValidator.class ) ).setArchivaConfiguration( archivaConfiguration );
        ManagedRepositoryAdmin managedRepositoryAdmin = applicationContext.getBean( ManagedRepositoryAdmin.class );
        if ( managedRepositoryAdmin.getManagedRepository( repoConfiguration.getId() ) != null )
        {
            managedRepositoryAdmin.deleteManagedRepository( repoConfiguration.getId(), null, true );
        }
    }

    @Test
    public void testConsumerByDaysOld()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer =
            applicationContext.getBean( "knownRepositoryContentConsumer#repo-purge-consumer-by-days-old",
                                        KnownRepositoryContentConsumer.class );

        ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
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

        removeRepoFromConfiguration( "days-old", repoConfiguration );
    }

    /**
     * Test the snapshot clean consumer on a repository set to NOT clean/delete snapshots based on released versions.
     *
     * @throws Exception
     */
    @Test
    public void testReleasedSnapshotsWereNotCleaned()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer =
            applicationContext.getBean( "knownRepositoryContentConsumer#repo-purge-consumer-by-retention-count",
                                        KnownRepositoryContentConsumer.class );

        ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        repoConfiguration.setDeleteReleasedSnapshots( false ); // Set to NOT delete released snapshots.
        addRepoToConfiguration( "retention-count", repoConfiguration );

        repoPurgeConsumer.beginScan( repoConfiguration, null );

        String repoRoot = prepareTestRepos();

        repoPurgeConsumer.processFile(
            CleanupReleasedSnapshotsRepositoryPurgeTest.PATH_TO_RELEASED_SNAPSHOT_IN_SAME_REPO );

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

        String metadataXml = FileUtils.readFileToString( artifactMetadataFile, Charset.defaultCharset() );

        String expectedVersions = "<expected><versions><version>2.3-SNAPSHOT</version></versions></expected>";

        XMLAssert.assertXpathEvaluatesTo( "2.3-SNAPSHOT", "//metadata/versioning/latest", metadataXml );
        XMLAssert.assertXpathsEqual( "//expected/versions/version", expectedVersions,
                                     "//metadata/versioning/versions/version", metadataXml );
        XMLAssert.assertXpathEvaluatesTo( "20070315032817", "//metadata/versioning/lastUpdated", metadataXml );

        removeRepoFromConfiguration( "retention-count", repoConfiguration );
    }

    @Test
    public void testReleasedSnapshotsWereCleaned()
        throws Exception
    {
        KnownRepositoryContentConsumer repoPurgeConsumer =
            applicationContext.getBean( "knownRepositoryContentConsumer#repo-purge-consumer-by-days-old",
                                        KnownRepositoryContentConsumer.class );

        ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        repoConfiguration.setDeleteReleasedSnapshots( true );
        addRepoToConfiguration( "days-old", repoConfiguration );

        repoPurgeConsumer.beginScan( repoConfiguration, null );

        String repoRoot = prepareTestRepos();

        repoPurgeConsumer.processFile(
            CleanupReleasedSnapshotsRepositoryPurgeTest.PATH_TO_RELEASED_SNAPSHOT_IN_SAME_REPO );

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

        String metadataXml = FileUtils.readFileToString( artifactMetadataFile, Charset.defaultCharset() );

        String expectedVersions =
            "<expected><versions><version>2.2</version>" + "<version>2.3</version></versions></expected>";

        XMLAssert.assertXpathEvaluatesTo( "2.3", "//metadata/versioning/latest", metadataXml );
        XMLAssert.assertXpathsEqual( "//expected/versions/version", expectedVersions,
                                     "//metadata/versioning/versions/version", metadataXml );
        XMLAssert.assertXpathEvaluatesTo( "20070315032817", "//metadata/versioning/lastUpdated", metadataXml );

        removeRepoFromConfiguration( "days-old", repoConfiguration );
    }
}
