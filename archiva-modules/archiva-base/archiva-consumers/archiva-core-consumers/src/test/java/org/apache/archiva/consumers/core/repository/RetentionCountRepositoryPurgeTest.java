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
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test RetentionsCountRepositoryPurgeTest
 */
public class RetentionCountRepositoryPurgeTest
    extends AbstractRepositoryPurgeTest
{
    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        org.apache.archiva.repository.ManagedRepository repoConfiguration = getRepoConfiguration( TEST_REPO_ID, TEST_REPO_NAME );
        List<RepositoryListener> listeners = Collections.singletonList( listener );
        ArtifactCleanupFeature acf = repoConfiguration.getFeature( ArtifactCleanupFeature.class ).get();

        sessionControl.reset();
        sessionFactoryControl.reset();
        EasyMock.expect( sessionFactory.createSession( ) ).andStubReturn( repositorySession );
        EasyMock.expect( repositorySession.getRepository()).andStubReturn( metadataRepository );
        repositorySession.save();
        EasyMock.expectLastCall().anyTimes();
        sessionFactoryControl.replay();
        sessionControl.replay();

        repoPurge = new RetentionCountRepositoryPurge( getRepository(), acf.getRetentionCount(),
                                                       repositorySession, listeners );
    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    /**
     * Test if the artifact to be processed was a jar.
     */
    @Test
    public void testIfAJarWasFound()
        throws Exception
    {
        String repoRoot = prepareTestRepos();
        String projectNs = "org.jruby.plugins";
        String projectPath = projectNs.replaceAll("\\.","/");
        String projectName = "jruby-rake-plugin";
        String projectVersion = "1.0RC1-SNAPSHOT";
        String projectRoot = repoRoot + "/" + projectPath+"/"+projectName;
        Path repo = getTestRepoRootPath();
        Path vDir = repo.resolve(projectPath).resolve(projectName).resolve(projectVersion);
        Set<String> deletedVersions = new HashSet<>();
        deletedVersions.add("1.0RC1-20070504.153317-1");
        deletedVersions.add("1.0RC1-20070504.160758-2");
        String versionRoot = projectRoot + "/" + projectVersion;

        // test listeners for the correct artifacts
        String[]  exts = { ".md5", ".sha1", ""};
        for (int i=0 ; i<exts.length; i++) {
            listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.jruby.plugins", "jruby-rake-plugin",
                    "1.0RC1-SNAPSHOT", "jruby-rake-plugin-1.0RC1-20070504.153317-1.jar"+exts[i]);
            listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.jruby.plugins", "jruby-rake-plugin",
                    "1.0RC1-SNAPSHOT", "jruby-rake-plugin-1.0RC1-20070504.153317-1.pom"+exts[i]);

            listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.jruby.plugins", "jruby-rake-plugin",
                    "1.0RC1-SNAPSHOT", "jruby-rake-plugin-1.0RC1-20070504.160758-2.jar"+exts[i]);


            listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.jruby.plugins", "jruby-rake-plugin",
                    "1.0RC1-SNAPSHOT", "jruby-rake-plugin-1.0RC1-20070504.160758-2.pom"+exts[i]);
        }
        listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.jruby.plugins", "jruby-rake-plugin",
                "1.0RC1-SNAPSHOT", "jruby-rake-plugin-1.0RC1-20070504.160758-2-javadoc.jar");

        listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.jruby.plugins", "jruby-rake-plugin",
                "1.0RC1-SNAPSHOT", "jruby-rake-plugin-1.0RC1-20070504.160758-2-javadoc.zip");
        listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.jruby.plugins", "jruby-rake-plugin",
                "1.0RC1-SNAPSHOT", "jruby-rake-plugin-1.0RC1-20070504.153317-1-javadoc.jar");

        listener.deleteArtifact(metadataRepository, getRepository().getId(), "org.jruby.plugins", "jruby-rake-plugin",
                "1.0RC1-SNAPSHOT", "jruby-rake-plugin-1.0RC1-20070504.153317-1-javadoc.zip");

        listenerControl.replay();

        // Provide the metadata list
        List<ArtifactMetadata> ml = getArtifactMetadataFromDir(TEST_REPO_ID , projectName, repo.getParent(), vDir );
        when(metadataRepository.getArtifacts( repositorySession, TEST_REPO_ID,
            projectNs, projectName, projectVersion )).thenReturn(ml);


        repoPurge.process( PATH_TO_BY_RETENTION_COUNT_ARTIFACT );

        listenerControl.verify();

        // Verify the metadataRepository invocations
        verify(metadataRepository, never()).removeProjectVersion( eq(repositorySession), eq(TEST_REPO_ID), eq(projectNs), eq(projectName), eq(projectVersion) );
        ArgumentCaptor<ArtifactMetadata> metadataArg = ArgumentCaptor.forClass(ArtifactMetadata.class);
        verify(metadataRepository, times(deletedVersions.size())).removeTimestampedArtifact( eq(repositorySession), metadataArg.capture(), eq(projectVersion) );
        List<ArtifactMetadata> metaL = metadataArg.getAllValues();
        for (ArtifactMetadata meta : metaL) {
            assertTrue(meta.getId().startsWith(projectName));
            assertTrue(deletedVersions.contains(meta.getVersion()));
        }


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

    /**
     * Test if the artifact to be processed is a pom
     */
    @Test
    public void testIfAPomWasFound()
        throws Exception
    {
        String repoRoot = prepareTestRepos();
        String projectNs = "org.codehaus.castor";
        String projectPath = projectNs.replaceAll("\\.","/");
        String projectName = "castor-anttasks";
        String projectVersion = "1.1.2-SNAPSHOT";
        String projectRoot = repoRoot + "/" + projectPath+"/"+projectName;
        Path repo = getTestRepoRootPath();
        Path vDir = repo.resolve(projectPath).resolve(projectName).resolve(projectVersion);
        Set<String> deletedVersions = new HashSet<>();
        deletedVersions.add("1.1.2-20070427.065136-1");
        String versionRoot = projectRoot + "/" + projectVersion;


        // test listeners for the correct artifacts
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.codehaus.castor", "castor-anttasks",
                "1.1.2-SNAPSHOT", "castor-anttasks-1.1.2-20070427.065136-1.jar.md5" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.codehaus.castor", "castor-anttasks",
                "1.1.2-SNAPSHOT", "castor-anttasks-1.1.2-20070427.065136-1.jar.sha1" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.codehaus.castor", "castor-anttasks",
                                 "1.1.2-SNAPSHOT", "castor-anttasks-1.1.2-20070427.065136-1.jar" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.codehaus.castor", "castor-anttasks",
                "1.1.2-SNAPSHOT", "castor-anttasks-1.1.2-20070427.065136-1.pom.md5" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.codehaus.castor", "castor-anttasks",
                "1.1.2-SNAPSHOT", "castor-anttasks-1.1.2-20070427.065136-1.pom.sha1" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.codehaus.castor", "castor-anttasks",
                                 "1.1.2-SNAPSHOT", "castor-anttasks-1.1.2-20070427.065136-1.pom" );
        listenerControl.replay();

        // Provide the metadata list
        List<ArtifactMetadata> ml = getArtifactMetadataFromDir(TEST_REPO_ID , projectName, repo.getParent(), vDir );
        when(metadataRepository.getArtifacts( repositorySession, TEST_REPO_ID,
            projectNs, projectName, projectVersion )).thenReturn(ml);

        repoPurge.process( PATH_TO_BY_RETENTION_COUNT_POM );

        listenerControl.verify();

        // Verify the metadataRepository invocations
        verify(metadataRepository, never()).removeProjectVersion( eq(repositorySession), eq(TEST_REPO_ID), eq(projectNs), eq(projectName), eq(projectVersion) );
        ArgumentCaptor<ArtifactMetadata> metadataArg = ArgumentCaptor.forClass(ArtifactMetadata.class);
        verify(metadataRepository, times(deletedVersions.size())).removeTimestampedArtifact( eq(repositorySession), metadataArg.capture(), eq(projectVersion) );
        List<ArtifactMetadata> metaL = metadataArg.getAllValues();
        for (ArtifactMetadata meta : metaL) {
            assertTrue(meta.getId().startsWith(projectName));
            assertTrue(deletedVersions.contains(meta.getVersion()));
        }


        // assert if removed from repo
        assertDeleted( versionRoot + "/castor-anttasks-1.1.2-20070427.065136-1.jar" );
        assertDeleted( versionRoot + "/castor-anttasks-1.1.2-20070427.065136-1.jar.md5" );
        assertDeleted( versionRoot + "/castor-anttasks-1.1.2-20070427.065136-1.jar.sha1" );
        assertDeleted( versionRoot + "/castor-anttasks-1.1.2-20070427.065136-1.pom" );
        assertDeleted( versionRoot + "/castor-anttasks-1.1.2-20070427.065136-1.pom.md5" );
        assertDeleted( versionRoot + "/castor-anttasks-1.1.2-20070427.065136-1.pom.sha1" );

        // assert if not removed from repo
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3.pom" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3.pom.md5" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3.pom.sha1" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3.jar" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3.jar.md5" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3.jar.sha1" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3-sources.jar" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3-sources.jar.md5" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070615.105019-3-sources.jar.sha1" );

        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2.pom" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2.pom.md5" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2.pom.sha1" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2.jar" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2.jar.md5" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2.jar.sha1" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2-sources.jar" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2-sources.jar.md5" );
        assertExists( versionRoot + "/castor-anttasks-1.1.2-20070506.163513-2-sources.jar.sha1" );
    }

    @Test
    public void testOrderOfDeletion()
        throws Exception
    {
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
        String versionRoot = projectRoot + "/" + projectVersion;


        // test listeners for the correct artifacts
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                "maven-assembly-plugin", "1.1.2-SNAPSHOT",
                "maven-assembly-plugin-1.1.2-20070427.065136-1.jar.md5" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                "maven-assembly-plugin", "1.1.2-SNAPSHOT",
                "maven-assembly-plugin-1.1.2-20070427.065136-1.jar.sha1" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                                 "maven-assembly-plugin", "1.1.2-SNAPSHOT",
                                 "maven-assembly-plugin-1.1.2-20070427.065136-1.jar" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                "maven-assembly-plugin", "1.1.2-SNAPSHOT",
                "maven-assembly-plugin-1.1.2-20070427.065136-1.pom.md5" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                "maven-assembly-plugin", "1.1.2-SNAPSHOT",
                "maven-assembly-plugin-1.1.2-20070427.065136-1.pom.sha1" );
        listener.deleteArtifact( metadataRepository, getRepository().getId(), "org.apache.maven.plugins",
                                 "maven-assembly-plugin", "1.1.2-SNAPSHOT",
                                 "maven-assembly-plugin-1.1.2-20070427.065136-1.pom" );
        listenerControl.replay();

        // Provide the metadata list
        List<ArtifactMetadata> ml = getArtifactMetadataFromDir(TEST_REPO_ID , projectName, repo.getParent(), vDir );
        when(metadataRepository.getArtifacts( repositorySession, TEST_REPO_ID,
            projectNs, projectName, projectVersion )).thenReturn(ml);

        repoPurge.process( PATH_TO_TEST_ORDER_OF_DELETION );

        listenerControl.verify();

        // Verify the metadataRepository invocations
        verify(metadataRepository, never()).removeProjectVersion( eq(repositorySession), eq(TEST_REPO_ID), eq(projectNs), eq(projectName), eq(projectVersion) );
        ArgumentCaptor<ArtifactMetadata> metadataArg = ArgumentCaptor.forClass(ArtifactMetadata.class);
        verify(metadataRepository, times(deletedVersions.size())).removeTimestampedArtifact( eq(repositorySession), metadataArg.capture(), eq(projectVersion) );
        List<ArtifactMetadata> metaL = metadataArg.getAllValues();
        for (ArtifactMetadata meta : metaL) {
            assertTrue(meta.getId().startsWith(projectName));
            assertTrue(deletedVersions.contains(meta.getVersion()));
        }


        assertDeleted( versionRoot + "/maven-assembly-plugin-1.1.2-20070427.065136-1.jar" );
        assertDeleted( versionRoot + "/maven-assembly-plugin-1.1.2-20070427.065136-1.jar.sha1" );
        assertDeleted( versionRoot + "/maven-assembly-plugin-1.1.2-20070427.065136-1.jar.md5" );
        assertDeleted( versionRoot + "/maven-assembly-plugin-1.1.2-20070427.065136-1.pom" );
        assertDeleted( versionRoot + "/maven-assembly-plugin-1.1.2-20070427.065136-1.pom.sha1" );
        assertDeleted( versionRoot + "/maven-assembly-plugin-1.1.2-20070427.065136-1.pom.md5" );

        // the following should not have been deleted
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070506.163513-2.jar" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070506.163513-2.jar.sha1" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070506.163513-2.jar.md5" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070506.163513-2.pom" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070506.163513-2.pom.sha1" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070506.163513-2.pom.md5" );

        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070615.105019-3.jar" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070615.105019-3.jar.sha1" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070615.105019-3.jar.md5" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070615.105019-3.pom" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070615.105019-3.pom.sha1" );
        assertExists( versionRoot + "/maven-assembly-plugin-1.1.2-20070615.105019-3.pom.md5" );
    }
}
