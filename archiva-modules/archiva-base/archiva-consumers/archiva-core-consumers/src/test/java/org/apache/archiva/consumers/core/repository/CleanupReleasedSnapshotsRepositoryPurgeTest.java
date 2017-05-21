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

import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.events.RepositoryListener;
import org.apache.archiva.repository.metadata.MetadataTools;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;


/**
 */
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml",
    "classpath:/spring-context-cleanup-released-snapshots.xml" } )
public class CleanupReleasedSnapshotsRepositoryPurgeTest
    extends AbstractRepositoryPurgeTest
{
    private static final String INDEX_PATH = ".index\\nexus-maven-repository-index.zip";

    private ArchivaConfiguration archivaConfiguration;

    public static final String PATH_TO_RELEASED_SNAPSHOT_IN_DIFF_REPO =
        "org/apache/archiva/released-artifact-in-diff-repo/1.0-SNAPSHOT/released-artifact-in-diff-repo-1.0-SNAPSHOT.jar";

    public static final String PATH_TO_HIGHER_SNAPSHOT_EXISTS_IN_SAME_REPO =
        "org/apache/maven/plugins/maven-source-plugin/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.jar";

    public static final String PATH_TO_RELEASED_SNAPSHOT_IN_SAME_REPO =
        "org/apache/maven/plugins/maven-plugin-plugin/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar";

    @Inject
    MetadataTools metadataTools;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        RepositoryContentFactory factory =
            applicationContext.getBean( "repositoryContentFactory#cleanup-released-snapshots",
                                        RepositoryContentFactory.class );

        archivaConfiguration =
            applicationContext.getBean( "archivaConfiguration#cleanup-released-snapshots", ArchivaConfiguration.class );

        listenerControl = EasyMock.createControl( );

        listener = listenerControl.createMock( RepositoryListener.class );
        List<RepositoryListener> listeners = Collections.singletonList( listener );
        repoPurge = new CleanupReleasedSnapshotsRepositoryPurge( getRepository(), metadataTools,
                                                                 applicationContext.getBean(
                                                                     ManagedRepositoryAdmin.class ), factory,
                                                                 repositorySession, listeners );

        ( (DefaultManagedRepositoryAdmin) applicationContext.getBean(
            ManagedRepositoryAdmin.class ) ).setArchivaConfiguration( archivaConfiguration );
        removeMavenIndexes();
    }

    //@Test
    public void testReleasedSnapshotsExistsInSameRepo()
        throws Exception
    {
        applicationContext.getBean( ManagedRepositoryAdmin.class ).deleteManagedRepository( TEST_REPO_ID, null, true );
        applicationContext.getBean( ManagedRepositoryAdmin.class ).addManagedRepository(
            getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME ), false, null );

        String repoRoot = prepareTestRepos();

        // test listeners for the correct artifacts
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                                 "maven-plugin-plugin", "2.3-SNAPSHOT", "maven-plugin-plugin-2.3-SNAPSHOT.jar" );
        listenerControl.replay();

        repoPurge.process( PATH_TO_RELEASED_SNAPSHOT_IN_SAME_REPO );

        listenerControl.verify();

        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-plugin-plugin";

        // check if the snapshot was removed
        assertDeleted( projectRoot + "/2.3-SNAPSHOT" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.md5" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.jar.sha1" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.md5" );
        assertDeleted( projectRoot + "/2.3-SNAPSHOT/maven-plugin-plugin-2.3-SNAPSHOT.pom.sha1" );

        // check if the released version was not removed
        assertExists( projectRoot + "/2.3" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3-sources.jar" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3-sources.jar.md5" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3-sources.jar.sha1" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3.jar" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3.jar.md5" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3.jar.sha1" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3.pom" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3.pom.md5" );
        assertExists( projectRoot + "/2.3/maven-plugin-plugin-2.3.pom.sha1" );

        // check if metadata file was updated
        File artifactMetadataFile = new File( projectRoot + "/maven-metadata.xml" );

        String metadataXml = FileUtils.readFileToString( artifactMetadataFile, Charset.defaultCharset() );

        String expectedVersions =
            "<expected><versions><version>2.2</version>" + "<version>2.3</version></versions></expected>";

        XMLAssert.assertXpathEvaluatesTo( "2.3", "//metadata/versioning/release", metadataXml );
        XMLAssert.assertXpathEvaluatesTo( "2.3", "//metadata/versioning/latest", metadataXml );
        XMLAssert.assertXpathsEqual( "//expected/versions/version", expectedVersions,
                                     "//metadata/versioning/versions/version", metadataXml );
        XMLAssert.assertXpathEvaluatesTo( "20070315032817", "//metadata/versioning/lastUpdated", metadataXml );
    }

    //@Test
    public void testNonArtifactFile()
        throws Exception
    {

        applicationContext.getBean( ManagedRepositoryAdmin.class ).deleteManagedRepository( TEST_REPO_ID, null, false );
        applicationContext.getBean( ManagedRepositoryAdmin.class ).addManagedRepository(
            getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME ), false, null );

        String repoRoot = prepareTestRepos();

        // test listeners for the correct artifacts
        listenerControl.replay();

        File file = new File( repoRoot, INDEX_PATH );
        if ( !file.exists() )
        {
            // help windauze to create directory with .
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        assertTrue( file.exists() );

        repoPurge.process( INDEX_PATH );

        listenerControl.verify();

        assertTrue( file.exists() );
    }

    //@Test
    public void testReleasedSnapshotsExistsInDifferentRepo()
        throws Exception
    {

        applicationContext.getBean( ManagedRepositoryAdmin.class ).deleteManagedRepository( TEST_REPO_ID, null, false );
        applicationContext.getBean( ManagedRepositoryAdmin.class ).addManagedRepository(
            getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME ), false, null );

        applicationContext.getBean( ManagedRepositoryAdmin.class ).addManagedRepository(
            getRepoConfiguration( RELEASES_TEST_REPO_ID, RELEASES_TEST_REPO_NAME ), false, null );

        String repoRoot = prepareTestRepos();

        // test listeners for the correct artifacts
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.apache.archiva",
                                 "released-artifact-in-diff-repo", "1.0-SNAPSHOT",
                                 "released-artifact-in-diff-repo-1.0-SNAPSHOT.jar" );
        listenerControl.replay();

        repoPurge.process( PATH_TO_RELEASED_SNAPSHOT_IN_DIFF_REPO );

        listenerControl.verify();

        String projectRoot = repoRoot + "/org/apache/archiva/released-artifact-in-diff-repo";

        // check if the snapshot was removed
        assertDeleted( projectRoot + "/1.0-SNAPSHOT" );
        assertDeleted( projectRoot + "/1.0-SNAPSHOT/released-artifact-in-diff-repo-1.0-SNAPSHOT.jar" );
        assertDeleted( projectRoot + "/1.0-SNAPSHOT/released-artifact-in-diff-repo-1.0-SNAPSHOT.jar.md5" );
        assertDeleted( projectRoot + "/1.0-SNAPSHOT/released-artifact-in-diff-repo-1.0-SNAPSHOT.jar.sha1" );
        assertDeleted( projectRoot + "/1.0-SNAPSHOT/released-artifact-in-diff-repo-1.0-SNAPSHOT.pom" );
        assertDeleted( projectRoot + "/1.0-SNAPSHOT/released-artifact-in-diff-repo-1.0-SNAPSHOT.pom.md5" );
        assertDeleted( projectRoot + "/1.0-SNAPSHOT/released-artifact-in-diff-repo-1.0-SNAPSHOT.pom.sha1" );

        String releasesProjectRoot =
            AbstractRepositoryPurgeTest.fixPath( new File( "target/test-" + getName() + "/releases-test-repo-one" ).getAbsolutePath()
                + "/org/apache/archiva/released-artifact-in-diff-repo" );

        // check if the released version was not removed
        assertExists( releasesProjectRoot + "/1.0" );
        assertExists( releasesProjectRoot + "/1.0/released-artifact-in-diff-repo-1.0.jar" );
        assertExists( releasesProjectRoot + "/1.0/released-artifact-in-diff-repo-1.0.jar.md5" );
        assertExists( releasesProjectRoot + "/1.0/released-artifact-in-diff-repo-1.0.jar.sha1" );
        assertExists( releasesProjectRoot + "/1.0/released-artifact-in-diff-repo-1.0.pom" );
        assertExists( releasesProjectRoot + "/1.0/released-artifact-in-diff-repo-1.0.pom.md5" );
        assertExists( releasesProjectRoot + "/1.0/released-artifact-in-diff-repo-1.0.pom.sha1" );
        
        // remove RELEASES_TEST_REPO_ID so this test will be more independant
        applicationContext.getBean( ManagedRepositoryAdmin.class ).deleteManagedRepository( RELEASES_TEST_REPO_ID, null, false );
    }

    @Test
    public void testHigherSnapshotExistsInSameRepo()
        throws Exception
    {

        applicationContext.getBean( ManagedRepositoryAdmin.class ).deleteManagedRepository( TEST_REPO_ID, null, false );
        applicationContext.getBean( ManagedRepositoryAdmin.class ).addManagedRepository(
            getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME ), false, null );

        String repoRoot = prepareTestRepos();

        // test listeners for the correct artifacts - no deletions
        listenerControl.replay();

        repoPurge.process( CleanupReleasedSnapshotsRepositoryPurgeTest.PATH_TO_HIGHER_SNAPSHOT_EXISTS_IN_SAME_REPO );

        listenerControl.verify();

        String projectRoot = repoRoot + "/org/apache/maven/plugins/maven-source-plugin";

        // check if the snapshot was not removed
        assertExists( projectRoot + "/2.0.3-SNAPSHOT" );
        assertExists( projectRoot + "/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.jar" );
        assertExists( projectRoot + "/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.jar.md5" );
        assertExists( projectRoot + "/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.jar.sha1" );
        assertExists( projectRoot + "/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.pom" );
        assertExists( projectRoot + "/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.pom.md5" );
        assertExists( projectRoot + "/2.0.3-SNAPSHOT/maven-source-plugin-2.0.3-SNAPSHOT.pom.sha1" );

        // check if the released version was not removed
        assertExists( projectRoot + "/2.0.4-SNAPSHOT" );
        assertExists( projectRoot + "/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.jar" );
        assertExists( projectRoot + "/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.jar.md5" );
        assertExists( projectRoot + "/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.jar.sha1" );
        assertExists( projectRoot + "/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.pom" );
        assertExists( projectRoot + "/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.pom.md5" );
        assertExists( projectRoot + "/2.0.4-SNAPSHOT/maven-source-plugin-2.0.4-SNAPSHOT.pom.sha1" );

        // check if metadata file was not updated (because nothing was removed)
        File artifactMetadataFile = new File( projectRoot + "/maven-metadata.xml" );

        String metadataXml = FileUtils.readFileToString( artifactMetadataFile, Charset.defaultCharset() );

        String expectedVersions = "<expected><versions><version>2.0.3-SNAPSHOT</version>"
            + "<version>2.0.4-SNAPSHOT</version></versions></expected>";

        XMLAssert.assertXpathEvaluatesTo( "2.0.4-SNAPSHOT", "//metadata/versioning/latest", metadataXml );
        XMLAssert.assertXpathsEqual( "//expected/versions/version", expectedVersions,
                                     "//metadata/versioning/versions/version", metadataXml );
        XMLAssert.assertXpathEvaluatesTo( "20070427033345", "//metadata/versioning/lastUpdated", metadataXml );
    }
}
