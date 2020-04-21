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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.audit.RepositoryListener;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 */
public class DaysOldRepositoryPurgeTest
    extends AbstractRepositoryPurgeTest
{
    private static final int OLD_TIMESTAMP = 1179382029;

    private void setLastModified( String dirPath, long lastModified ) throws IOException
    {
        Path dir = Paths.get( dirPath );
        Path[] contents = Files.list( dir ).toArray(Path[]::new );
        for ( Path content : contents )
        {
            Files.setLastModifiedTime( content, FileTime.fromMillis( lastModified ));
        }
    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
        repoPurge = null;
    }

    @Test
    public void testByLastModified()
        throws Exception
    {
        org.apache.archiva.repository.ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        ArtifactCleanupFeature atf = repoConfiguration.getFeature( ArtifactCleanupFeature.class ).get();


        sessionControl.reset();
        sessionFactoryControl.reset();
        EasyMock.expect( sessionFactory.createSession( ) ).andStubReturn( repositorySession );
        EasyMock.expect( repositorySession.getRepository()).andStubReturn( metadataRepository );
        repositorySession.save();
        EasyMock.expectLastCall().anyTimes();
        sessionFactoryControl.replay();
        sessionControl.replay();

        repoPurge = new DaysOldRepositoryPurge( getRepository(), atf.getRetentionPeriod().getDays(),
                                                atf.getRetentionCount(), repositorySession,
                                                Collections.singletonList( listener ) );

        String repoRoot = prepareTestRepos();
        String projectNs = "org.apache.maven.plugins";
        String projectPath = projectNs.replaceAll("\\.","/");
        String projectName = "maven-install-plugin";
        String projectVersion = "2.2-SNAPSHOT";
        String projectRoot = repoRoot + "/" + projectPath+"/"+projectName;
        Path repo = getTestRepoRootPath();
        Path vDir = repo.resolve(projectPath).resolve(projectName).resolve(projectVersion);
        Set<String> deletedVersions = new HashSet<>();
        deletedVersions.add("2.2-SNAPSHOT");
        deletedVersions.add("2.2-20061118.060401-2");

        setLastModified( projectRoot + "/" + projectVersion + "/", OLD_TIMESTAMP );

        // test listeners for the correct artifacts
        String[] exts = {".md5",".sha1",""};
        for (int i=0; i<exts.length; i++) {

            listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                    "maven-install-plugin", "2.2-SNAPSHOT", "maven-install-plugin-2.2-SNAPSHOT.jar"+exts[i]);
            listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                    "maven-install-plugin", "2.2-SNAPSHOT", "maven-install-plugin-2.2-SNAPSHOT.pom"+exts[i]);
            listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                    "maven-install-plugin", "2.2-SNAPSHOT",
                    "maven-install-plugin-2.2-20061118.060401-2.jar"+exts[i]);
            listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                    "maven-install-plugin", "2.2-SNAPSHOT",
                    "maven-install-plugin-2.2-20061118.060401-2.pom"+exts[i]);
        }
        listenerControl.replay();

        // Provide the metadata list
        List<ArtifactMetadata> ml = getArtifactMetadataFromDir(TEST_REPO_ID , projectName, repo.getParent(), vDir );
        when(metadataRepository.getArtifacts( repositorySession, TEST_REPO_ID,
            projectNs, projectName, projectVersion )).thenReturn(ml);

        repoPurge.process( PATH_TO_BY_DAYS_OLD_ARTIFACT );

        listenerControl.verify();

        // Verify the metadataRepository invocations
        verify(metadataRepository, never()).removeProjectVersion(eq(repositorySession) , eq(TEST_REPO_ID), eq(projectNs), eq(projectName), eq(projectVersion) );
        ArgumentCaptor<ArtifactMetadata> metadataArg = ArgumentCaptor.forClass(ArtifactMetadata.class);
        verify(metadataRepository, times(2)).removeTimestampedArtifact(eq(repositorySession) , metadataArg.capture(), eq(projectVersion) );
        List<ArtifactMetadata> metaL = metadataArg.getAllValues();
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
    }

    @Test
    public void testOrderOfDeletion()
        throws Exception
    {
        org.apache.archiva.repository.ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        ArtifactCleanupFeature atf = repoConfiguration.getFeature( ArtifactCleanupFeature.class ).get();
        List<RepositoryListener> listeners = Collections.singletonList( listener );

        sessionControl.reset();
        sessionFactoryControl.reset();
        EasyMock.expect( sessionFactory.createSession( ) ).andStubReturn( repositorySession );
        EasyMock.expect( repositorySession.getRepository()).andStubReturn( metadataRepository );
        repositorySession.save();
        EasyMock.expectLastCall().anyTimes();
        sessionFactoryControl.replay();
        sessionControl.replay();
        repoPurge = new DaysOldRepositoryPurge( getRepository(), atf.getRetentionPeriod().getDays(),
                                                atf.getRetentionCount(), repositorySession, listeners );

        String repoRoot = prepareTestRepos();
        String projectNs = "org.apache.maven.plugins";
        String projectPath = projectNs.replaceAll("\\.","/");
        String projectName = "maven-assembly-plugin";
        String projectVersion = "1.1.2-SNAPSHOT";
        String projectRoot = repoRoot + "/" + projectPath+"/"+projectName;
        Path repo = getTestRepoRootPath();
        Path vDir = repo.resolve(projectPath).resolve(projectName).resolve(projectVersion);
        Set<String> deletedVersions = new HashSet<>();
        deletedVersions.add("1.1.2-20070427.065136-1");

        setLastModified( projectRoot + "/" + projectVersion + "/", OLD_TIMESTAMP );

        // test listeners for the correct artifacts
        String[] exts = {".md5",".sha1",""};
        for (int i=0; i<exts.length; i++) {
            listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                    "maven-assembly-plugin", "1.1.2-SNAPSHOT",
                    "maven-assembly-plugin-1.1.2-20070427.065136-1.jar"+exts[i]);
            listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                    "maven-assembly-plugin", "1.1.2-SNAPSHOT",
                    "maven-assembly-plugin-1.1.2-20070427.065136-1.pom"+exts[i]);
        }
        listenerControl.replay();

        // Provide the metadata list
        List<ArtifactMetadata> ml = getArtifactMetadataFromDir(TEST_REPO_ID , projectName, repo.getParent(), vDir );
        when(metadataRepository.getArtifacts(repositorySession , TEST_REPO_ID,
            projectNs, projectName, projectVersion )).thenReturn(ml);


        repoPurge.process( PATH_TO_TEST_ORDER_OF_DELETION );

        listenerControl.verify();

        // Verify the metadataRepository invocations
        verify(metadataRepository, never()).removeProjectVersion(eq(repositorySession) , eq(TEST_REPO_ID), eq(projectNs), eq(projectName), eq(projectVersion) );
        ArgumentCaptor<ArtifactMetadata> metadataArg = ArgumentCaptor.forClass(ArtifactMetadata.class);
        verify(metadataRepository, times(deletedVersions.size())).removeTimestampedArtifact(eq(repositorySession) , metadataArg.capture(), eq(projectVersion) );
        List<ArtifactMetadata> metaL = metadataArg.getAllValues();
        for (ArtifactMetadata meta : metaL) {
            assertTrue(meta.getId().startsWith(projectName));
            assertTrue(deletedVersions.contains(meta.getVersion()));
        }


        assertDeleted( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070427.065136-1.jar" );
        assertDeleted( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070427.065136-1.jar.sha1" );
        assertDeleted( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070427.065136-1.jar.md5" );
        assertDeleted( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070427.065136-1.pom" );
        assertDeleted( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070427.065136-1.pom.sha1" );
        assertDeleted( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070427.065136-1.pom.md5" );

        // the following should not have been deleted
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070506.163513-2.jar" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070506.163513-2.jar.sha1" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070506.163513-2.jar.md5" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070506.163513-2.pom" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070506.163513-2.pom.sha1" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070506.163513-2.pom.md5" );

        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.jar" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.jar.sha1" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.jar.md5" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.pom" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.pom.sha1" );
        assertExists( projectRoot + "/1.1.2-SNAPSHOT/maven-assembly-plugin-1.1.2-20070615.105019-3.pom.md5" );
    }

    @Test
    public void testMetadataDrivenSnapshots()
        throws Exception
    {
        org.apache.archiva.repository.ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        ArtifactCleanupFeature atf = repoConfiguration.getFeature( ArtifactCleanupFeature.class ).get();
        List<RepositoryListener> listeners = Collections.singletonList( listener );

        sessionControl.reset();
        sessionFactoryControl.reset();
        EasyMock.expect( sessionFactory.createSession( ) ).andStubReturn( repositorySession );
        EasyMock.expect( repositorySession.getRepository()).andStubReturn( metadataRepository );
        repositorySession.save();
        EasyMock.expectLastCall().anyTimes();
        sessionFactoryControl.replay();
        sessionControl.replay();
        repoPurge = new DaysOldRepositoryPurge( getRepository(), atf.getRetentionPeriod().getDays(),
            atf.getRetentionCount(), repositorySession, listeners );

        String repoRoot = prepareTestRepos();
        String projectNs = "org.codehaus.plexus";
        String projectPath = projectNs.replaceAll("\\.","/");
        String projectName = "plexus-utils";
        String projectVersion = "1.4.3-SNAPSHOT";
        String projectRoot = repoRoot + "/" + projectPath+"/"+projectName;
        Path repo = getTestRepoRootPath();
        Path vDir = repo.resolve(projectPath).resolve(projectName).resolve(projectVersion);
        Set<String> deletedVersions = new HashSet<>();
        deletedVersions.add("1.4.3-20070113.163208-4");


        String versionRoot = projectRoot + "/"+ projectVersion;

        Calendar currentDate = Calendar.getInstance( TimeZone.getTimeZone("UTC") );
        setLastModified( versionRoot, currentDate.getTimeInMillis() );

        String timestamp = new SimpleDateFormat( "yyyyMMdd.HHmmss" ).format( currentDate.getTime() );

        for ( int i = 5; i <= 7; i++ )
        {
            Path jarFile = Paths.get( versionRoot, "/plexus-utils-1.4.3-" + timestamp + "-" + i + ".jar" );
            Files.createFile( jarFile );
            Path pomFile = Paths.get( versionRoot, "/plexus-utils-1.4.3-" + timestamp + "-" + i + ".pom" );
            Files.createFile(pomFile);

            // set timestamp to older than 100 days for the first build, but ensure the filename timestamp is honoured instead
            if ( i == 5 )
            {
                Files.setLastModifiedTime( jarFile, FileTime.fromMillis( OLD_TIMESTAMP ));
                Files.setLastModifiedTime( pomFile, FileTime.fromMillis( OLD_TIMESTAMP ));
            }
        }

        // test listeners for the correct artifacts
        String[] exts = {".sha1",""};
        for (int i=0; i<exts.length; i++) {

            listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.codehaus.plexus", "plexus-utils",
                    "1.4.3-SNAPSHOT", "plexus-utils-1.4.3-20070113.163208-4.jar"+exts[i]);
            listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.codehaus.plexus", "plexus-utils",
                    "1.4.3-SNAPSHOT", "plexus-utils-1.4.3-20070113.163208-4.pom"+exts[i]);
        }
        listenerControl.replay();

        // Provide the metadata list
        List<ArtifactMetadata> ml = getArtifactMetadataFromDir(TEST_REPO_ID , projectName, repo.getParent(), vDir );
        when(metadataRepository.getArtifacts(repositorySession , TEST_REPO_ID,
            projectNs, projectName, projectVersion )).thenReturn(ml);


        repoPurge.process( PATH_TO_BY_DAYS_OLD_METADATA_DRIVEN_ARTIFACT );

        listenerControl.verify();

        // Verify the metadataRepository invocations
        verify(metadataRepository, never()).removeProjectVersion(eq(repositorySession) , eq(TEST_REPO_ID), eq(projectNs), eq(projectName), eq(projectVersion) );
        ArgumentCaptor<ArtifactMetadata> metadataArg = ArgumentCaptor.forClass(ArtifactMetadata.class);
        verify(metadataRepository, times(deletedVersions.size())).removeTimestampedArtifact( eq(repositorySession), metadataArg.capture(), eq(projectVersion) );
        List<ArtifactMetadata> metaL = metadataArg.getAllValues();
        for (ArtifactMetadata meta : metaL) {
            assertTrue(meta.getId().startsWith(projectName));
            assertTrue(deletedVersions.contains(meta.getVersion()));
        }


        // this should be deleted since the filename version (timestamp) is older than
        // 100 days even if the last modified date was <100 days ago
        assertDeleted( versionRoot + "/plexus-utils-1.4.3-20070113.163208-4.jar" );
        assertDeleted( versionRoot + "/plexus-utils-1.4.3-20070113.163208-4.jar.sha1" );
        assertDeleted( versionRoot + "/plexus-utils-1.4.3-20070113.163208-4.pom" );
        assertDeleted( versionRoot + "/plexus-utils-1.4.3-20070113.163208-4.pom.sha1" );

        // this should not be deleted because last modified date is <100 days ago
        assertExists( versionRoot + "/plexus-utils-1.4.3-SNAPSHOT.jar" );
        assertExists( versionRoot + "/plexus-utils-1.4.3-SNAPSHOT.pom" );

        for ( int i = 5; i <= 7; i++ )
        {
            assertExists( versionRoot + "/plexus-utils-1.4.3-" + timestamp + "-" + i + ".jar" );
            assertExists( versionRoot + "/plexus-utils-1.4.3-" + timestamp + "-" + i + ".pom" );
        }
    }


}
