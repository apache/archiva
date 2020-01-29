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

import org.apache.archiva.common.utils.BaseFile;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.FileType;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.consumers.functors.ConsumerWantsFilePredicate;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.xmlunit.assertj.XmlAssert;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 */
@ContextConfiguration (
    locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context-purge-consumer-test.xml" } )
public class RepositoryPurgeConsumerTest
    extends AbstractRepositoryPurgeTest
{
    private static final Logger log = LoggerFactory.getLogger( RepositoryPurgeConsumerTest.class );

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

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

    @SuppressWarnings( "deprecation" )
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

        Path repoLocation = Paths.get( "target/test-" + getName() + "/test-repo" );

        Path localFile = repoLocation.resolve( path );

        ConsumerWantsFilePredicate predicate = new ConsumerWantsFilePredicate();
        BaseFile baseFile = new BaseFile( repoLocation.toFile(), localFile.toFile() );
        predicate.setBasefile( baseFile );

        assertFalse( predicate.evaluate( repoPurgeConsumer ) );
    }

    private void setLastModified( String path ) throws IOException
    {
        Path dir = Paths.get( path );
        Path[] contents = new Path[0];
        try
        {
            contents = Files.list( dir ).toArray(Path[]::new);
        }
        catch ( IOException e )
        {
            log.error("Could not list files {}: {}", dir, e.getMessage(), e);
            contents = new Path[0];
        }
        for ( int i = 0; i < contents.length; i++ )
        {
            Files.setLastModifiedTime( contents[i], FileTime.fromMillis( 1179382029 ) );
        }
    }

    @Test
    public void testConsumerByRetentionCount()
        throws Exception
    {
        RepositoryPurgeConsumer repoPurgeConsumer =
            applicationContext.getBean( "knownRepositoryContentConsumer#repo-purge-consumer-by-retention-count",
                                        RepositoryPurgeConsumer.class );
        repoPurgeConsumer.setRepositorySessionFactory( sessionFactory );
        org.apache.archiva.repository.ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        ArtifactCleanupFeature atf = repoConfiguration.getFeature( ArtifactCleanupFeature.class ).get();
        atf.setRetentionPeriod( Period.ofDays( 0 ) ); // force days older off to allow retention count purge to execute.
        atf.setRetentionCount( TEST_RETENTION_COUNT );
        addRepoToConfiguration( "retention-count", repoConfiguration );

        sessionControl.reset();
        sessionFactoryControl.reset();
        EasyMock.expect( sessionFactory.createSession( ) ).andStubReturn( repositorySession );
        EasyMock.expect( repositorySession.getRepository()).andStubReturn( metadataRepository );
        repositorySession.save();
        EasyMock.expectLastCall().anyTimes();
        sessionFactoryControl.replay();
        sessionControl.replay();

        repoPurgeConsumer.beginScan( repoConfiguration, null );

        String repoRoot = prepareTestRepos();
        String projectNs = "org.jruby.plugins";
        String projectPath = projectNs.replaceAll("\\.","/");
        String projectName = "jruby-rake-plugin";
        String projectVersion = "1.0RC1-SNAPSHOT";
        String projectRoot = repoRoot + "/" + projectPath+"/"+projectName;
        String versionRoot = projectRoot + "/" + projectVersion;

        Path repo = getTestRepoRootPath();
        Path vDir = repo.resolve(projectPath).resolve(projectName).resolve(projectVersion);

        // Provide the metadata list
        List<ArtifactMetadata> ml = getArtifactMetadataFromDir( TEST_REPO_ID, projectName, repo, vDir );



        when(metadataRepository.getArtifacts( repositorySession, TEST_REPO_ID,
            projectNs, projectName, projectVersion )).thenReturn(ml);
        Set<String> deletedVersions = new HashSet<>();
        deletedVersions.add("1.0RC1-20070504.153317-1");
        deletedVersions.add("1.0RC1-20070504.160758-2");

        repoPurgeConsumer.processFile( PATH_TO_BY_RETENTION_COUNT_ARTIFACT );

        // Verify the metadataRepository invocations
        verify(metadataRepository, never()).removeProjectVersion( eq(repositorySession), eq(TEST_REPO_ID), eq(projectNs), eq(projectName), eq(projectVersion) );
        ArgumentCaptor<ArtifactMetadata> metadataArg = ArgumentCaptor.forClass(ArtifactMetadata.class);
        verify(metadataRepository, times(2)).removeTimestampedArtifact( eq(repositorySession), metadataArg.capture(), eq(projectVersion) );
        List<ArtifactMetadata> metaL = metadataArg.getAllValues();
        for (ArtifactMetadata meta : metaL) {
            assertTrue(meta.getId().startsWith(projectName));
            assertTrue(deletedVersions.contains(meta.getVersion()));
        }



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

    private void addRepoToConfiguration( String configHint, org.apache.archiva.repository.ManagedRepository repoConfiguration )
        throws Exception
    {
        RepositoryRegistry repositoryRegistry = applicationContext.getBean( ArchivaRepositoryRegistry.class);
        repositoryRegistry.putRepository( repoConfiguration );
    }

    private void removeRepoFromConfiguration( String configHint, org.apache.archiva.repository.ManagedRepository repoConfiguration )
        throws Exception
    {
        RepositoryRegistry repositoryRegistry = applicationContext.getBean( ArchivaRepositoryRegistry.class);
        repositoryRegistry.removeRepository( repoConfiguration );
    }

    @Test
    public void testConsumerByDaysOld()
        throws Exception
    {
        RepositoryPurgeConsumer repoPurgeConsumer =
            applicationContext.getBean( "knownRepositoryContentConsumer#repo-purge-consumer-by-days-old",
                RepositoryPurgeConsumer.class );

        repoPurgeConsumer.setRepositorySessionFactory( sessionFactory );

        org.apache.archiva.repository.ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        ArtifactCleanupFeature atf = repoConfiguration.getFeature( ArtifactCleanupFeature.class ).get();
        atf.setRetentionPeriod( Period.ofDays( TEST_DAYS_OLDER ) );
        addRepoToConfiguration( "days-old", repoConfiguration );

        sessionControl.reset();
        sessionFactoryControl.reset();
        EasyMock.expect( sessionFactory.createSession( ) ).andStubReturn( repositorySession );
        EasyMock.expect( repositorySession.getRepository()).andStubReturn( metadataRepository );
        repositorySession.save();
        EasyMock.expectLastCall().anyTimes();
        sessionFactoryControl.replay();
        sessionControl.replay();
        repoPurgeConsumer.beginScan( repoConfiguration, null );

        String repoRoot = prepareTestRepos();
        String projectNs = "org.apache.maven.plugins";
        String projectPath = projectNs.replaceAll("\\.","/");
        String projectName = "maven-install-plugin";
        String projectVersion = "2.2-SNAPSHOT";
        String projectRoot = repoRoot + "/" + projectPath+"/"+projectName;

        setLastModified( projectRoot + "/"+projectVersion);


        Path repo = getTestRepoRootPath();
        Path vDir = repo.resolve(projectPath).resolve(projectName).resolve(projectVersion);

        // Provide the metadata list
        List<ArtifactMetadata> ml = getArtifactMetadataFromDir( TEST_REPO_ID, projectName, repo, vDir );
        when(metadataRepository.getArtifacts( repositorySession, TEST_REPO_ID,
            projectNs, projectName, projectVersion )).thenReturn(ml);
        Set<String> deletedVersions = new HashSet<>();
        deletedVersions.add("2.2-SNAPSHOT");
        deletedVersions.add("2.2-20061118.060401-2");

        repoPurgeConsumer.processFile( PATH_TO_BY_DAYS_OLD_ARTIFACT );

        // Verify the metadataRepository invocations
        verify(metadataRepository, never()).removeProjectVersion( eq(repositorySession), eq(TEST_REPO_ID), eq(projectNs), eq(projectName), eq(projectVersion) );
        ArgumentCaptor<ArtifactMetadata> metadataArg = ArgumentCaptor.forClass(ArtifactMetadata.class);
        verify(metadataRepository, times(2)).removeTimestampedArtifact( eq(repositorySession), metadataArg.capture(), eq(projectVersion) );
        List<ArtifactMetadata> metaL = metadataArg.getAllValues();
        assertTrue( metaL.size( ) > 0 );
        for (ArtifactMetadata meta : metaL) {
            assertTrue(meta.getId().startsWith(projectName));
            assertTrue(deletedVersions.contains(meta.getVersion()));
        }

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

        org.apache.archiva.repository.ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        ArtifactCleanupFeature acf = repoConfiguration.getFeature( ArtifactCleanupFeature.class ).get();
        acf.setDeleteReleasedSnapshots( false ); // Set to NOT delete released snapshots.
        addRepoToConfiguration( "retention-count", repoConfiguration );

        repoPurgeConsumer.beginScan( repoConfiguration, null );

        String repoRoot = prepareTestRepos();
        String projectNs = "org.apache.maven.plugins";
        String projectPath = projectNs.replaceAll("\\.","/");
        String projectName = "maven-plugin-plugin";
        String projectVersion = "2.3-SNAPSHOT";
        String projectRoot = repoRoot + "/" + projectPath+"/"+projectName;

        Path repo = getTestRepoRootPath();
        Path vDir = repo.resolve(projectPath).resolve(projectName).resolve(projectVersion);

        // Provide the metadata list
        List<ArtifactMetadata> ml = getArtifactMetadataFromDir( TEST_REPO_ID, projectName, repo, vDir );
        when(metadataRepository.getArtifacts( repositorySession, TEST_REPO_ID,
            projectNs, projectName, projectVersion )).thenReturn(ml);

        repoPurgeConsumer.processFile(
            CleanupReleasedSnapshotsRepositoryPurgeTest.PATH_TO_RELEASED_SNAPSHOT_IN_SAME_REPO );

        verify(metadataRepository, never()).removeProjectVersion( eq(repositorySession), eq(TEST_REPO_ID), eq(projectNs), eq(projectName), eq(projectVersion) );
        ArgumentCaptor<ArtifactMetadata> metadataArg = ArgumentCaptor.forClass(ArtifactMetadata.class);
        verify(metadataRepository, never()).removeTimestampedArtifact( eq(repositorySession), any(), any() );
        verify(metadataRepository, never()).removeFacetFromArtifact( eq(repositorySession), any(), any(), any(), any(), any(MetadataFacet.class) );

        // check if the snapshot wasn't removed

        assertExists( projectRoot + "/2.3-SNAPSHOT" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.md5" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.sha1" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.md5" );
        assertExists( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.sha1" );

        // check if metadata file wasn't updated
        Path artifactMetadataFile = Paths.get( projectRoot + "/maven-metadata.xml" );

        String metadataXml = org.apache.archiva.common.utils.FileUtils.readFileToString( artifactMetadataFile, Charset.defaultCharset() );

        XmlAssert.assertThat( metadataXml ).valueByXPath( "//metadata/versioning/latest" ).isEqualTo( "2.3-SNAPSHOT" );
        XmlAssert.assertThat( metadataXml ).valueByXPath( "//metadata/versioning/versions/version" ).isEqualTo( "2.3-SNAPSHOT" );
        XmlAssert.assertThat(metadataXml).valueByXPath("//metadata/versioning/lastUpdated").isEqualTo ( "20070315032817" );

        removeRepoFromConfiguration( "retention-count", repoConfiguration );
    }

    @Test
    public void testReleasedSnapshotsWereCleaned()
        throws Exception
    {
        RepositoryPurgeConsumer repoPurgeConsumer =
            applicationContext.getBean( "knownRepositoryContentConsumer#repo-purge-consumer-by-days-old",
                                        RepositoryPurgeConsumer.class );
        repoPurgeConsumer.setRepositorySessionFactory( sessionFactory );
        org.apache.archiva.repository.ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        ArtifactCleanupFeature acf = repoConfiguration.getFeature( ArtifactCleanupFeature.class ).get();
        acf.setDeleteReleasedSnapshots( true );
        addRepoToConfiguration( "days-old", repoConfiguration );

        sessionControl.reset();
        sessionFactoryControl.reset();
        EasyMock.expect( sessionFactory.createSession( ) ).andStubReturn( repositorySession );
        EasyMock.expect( repositorySession.getRepository()).andStubReturn( metadataRepository );
        repositorySession.save();
        EasyMock.expectLastCall().anyTimes();
        sessionFactoryControl.replay();
        sessionControl.replay();
        repoPurgeConsumer.beginScan( repoConfiguration, null );

        String repoRoot = prepareTestRepos();
        String projectNs = "org.apache.maven.plugins";
        String projectPath = projectNs.replaceAll("\\.","/");
        String projectName = "maven-plugin-plugin";
        String projectVersion = "2.3-SNAPSHOT";
        String projectRoot = repoRoot + "/" + projectPath+"/"+projectName;
        Path repo = getTestRepoRootPath();
        Path vDir = repo.resolve(projectPath).resolve(projectName).resolve(projectVersion);

        // Provide the metadata list
        List<ArtifactMetadata> ml = getArtifactMetadataFromDir(TEST_REPO_ID , projectName, repo.getParent(), vDir );
        when(metadataRepository.getArtifacts( repositorySession, TEST_REPO_ID,
            projectNs, projectName, projectVersion )).thenReturn(ml);

        repoPurgeConsumer.processFile(
            CleanupReleasedSnapshotsRepositoryPurgeTest.PATH_TO_RELEASED_SNAPSHOT_IN_SAME_REPO );

        verify(metadataRepository, times(1)).removeProjectVersion( eq(repositorySession), eq(TEST_REPO_ID), eq(projectNs), eq(projectName), eq(projectVersion) );
        ArgumentCaptor<ArtifactMetadata> metadataArg = ArgumentCaptor.forClass(ArtifactMetadata.class);
        verify(metadataRepository, never()).removeTimestampedArtifact( eq(repositorySession), any(), any() );

        // check if the snapshot was removed
        assertDeleted( projectRoot + "/2.3-SNAPSHOT" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.md5" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.sha1" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.md5" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.sha1" );

        // check if metadata file was updated
        Path artifactMetadataFile = Paths.get( projectRoot + "/maven-metadata.xml" );

        String metadataXml = org.apache.archiva.common.utils.FileUtils.readFileToString( artifactMetadataFile, Charset.defaultCharset() );

        String expectedVersions =
            "<expected><versions><version>2.2</version>" + "<version>2.3</version></versions></expected>";

        XmlAssert.assertThat( metadataXml ).valueByXPath( "//metadata/versioning/latest" ).isEqualTo( "2.3" );
        // XMLAssert.assertXpathEvaluatesTo( "2.3", "//metadata/versioning/latest", metadataXml );
        XmlAssert.assertThat( metadataXml ).nodesByXPath( "//metadata/versioning/versions/version" ).hasSize( 2 );
        XmlAssert.assertThat( metadataXml ).valueByXPath( "//metadata/versioning/versions/version[1]" ).isEqualTo( "2.2" );
        XmlAssert.assertThat( metadataXml ).valueByXPath( "//metadata/versioning/versions/version[2]" ).isEqualTo( "2.3" );
        // XMLAssert.assertXpathsEqual( "//expected/versions/version", expectedVersions,
        //                             "//metadata/versioning/versions/version", metadataXml );

        XmlAssert.assertThat( metadataXml ).valueByXPath( "//metadata/versioning/lastUpdated" ).isEqualTo( "20070315032817" );
        //XMLAssert.assertXpathEvaluatesTo( "20070315032817", "//metadata/versioning/lastUpdated", metadataXml );

        removeRepoFromConfiguration( "days-old", repoConfiguration );
    }
}
