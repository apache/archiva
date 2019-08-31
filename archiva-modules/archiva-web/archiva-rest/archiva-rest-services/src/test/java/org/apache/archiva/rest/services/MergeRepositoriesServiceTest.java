package org.apache.archiva.rest.services;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.rest.api.services.MergeRepositoriesService;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
public class MergeRepositoriesServiceTest
    extends AbstractArchivaRestTest
{

    private static final String TEST_REPOSITORY = "test-repository";
    private static final String TEST_REPOSITORY_STAGE = TEST_REPOSITORY + "-stage";

    private Path repo = getAppserverBase().resolve("data/repositories").resolve( "test-repository" );

    private Path repoStage = getAppserverBase().resolve("data/repositories").resolve( "test-repository-stage" );

    private int maxChecks = 10;
    private int checkWaitMs = 500;

    @Test
    public void getMergeConflictedArtifacts()
        throws Exception
    {
        MergeRepositoriesService service = getMergeRepositoriesService( authorizationHeader );

        waitForScanToComplete( TEST_REPOSITORY );
        waitForScanToComplete( TEST_REPOSITORY_STAGE );


        int checks = maxChecks;
        Throwable ex = null;
        while(checks-->0) {
            try {
                log.info("Test Try " + checks);
                List<Artifact> artifactMetadatas = service.getMergeConflictedArtifacts( TEST_REPOSITORY_STAGE,
                        TEST_REPOSITORY );
                log.info("conflicts: {}", artifactMetadatas);

                assertThat(artifactMetadatas).isNotNull().isNotEmpty().hasSize(8);
                return;
            } catch (Throwable e) {
                ex = e;
            }
            Thread.currentThread().sleep(checkWaitMs);
        }
        if (ex!=null && ex instanceof AssertionError) {
            throw (AssertionError)ex;
        } else {
            throw new Exception(ex);
        }

    }

    @Test
    public void merge()
        throws Exception
    {
        String mergedArtifactPath =
            "org/apache/felix/org.apache.felix.bundlerepository/1.6.4/org.apache.felix.bundlerepository-1.6.4.jar";
        String mergedArtifactPomPath =
            "org/apache/felix/org.apache.felix.bundlerepository/1.6.4/org.apache.felix.bundlerepository-1.6.4.pom";

        assertTrue( Files.exists(repoStage.resolve(mergedArtifactPath)) );
        assertTrue( Files.exists(repoStage.resolve(mergedArtifactPomPath)) );

        waitForScanToComplete( TEST_REPOSITORY );
        waitForScanToComplete( TEST_REPOSITORY_STAGE );

        MergeRepositoriesService service = getMergeRepositoriesService( authorizationHeader );

        int checks = maxChecks;
        Throwable ex = null;
        while(checks-->0) {
            try {
                log.info("Test Try " + checks);
                service.mergeRepositories(TEST_REPOSITORY_STAGE, TEST_REPOSITORY, true);

                assertTrue(Files.exists(repo.resolve(mergedArtifactPath)));
                assertTrue(Files.exists(repo.resolve(mergedArtifactPomPath)));
                return;
            } catch (Throwable e) {
                log.info("Exception {}, {}", e.getMessage(), e.getClass());
                ex = e;
            }
            Thread.currentThread().sleep(checkWaitMs);
        }
        if (ex!=null && ex instanceof AssertionError) {
            throw (AssertionError)ex;
        } else if (ex!=null) {
            throw new Exception(ex);
        }

    }

    @After
    public void deleteStageRepo()
        throws Exception
    {
        waitForScanToComplete( TEST_REPOSITORY );
        waitForScanToComplete( TEST_REPOSITORY_STAGE );

        deleteTestRepo( TEST_REPOSITORY );

        org.apache.archiva.common.utils.FileUtils.deleteDirectory( repo );
        org.apache.archiva.common.utils.FileUtils.deleteDirectory( repoStage );
    }

    @Before
    public void createStageRepo()
        throws Exception
    {
        // FileUtils.copyDirectory( Paths.get( System.getProperty( "basedir" ), "src/test/repo-with-osgi" ).toFile(), repo.toFile() );

        Path srcRepo = getProjectDirectory().resolve(  "src/test/repo-with-osgi" );
        createStagedNeededRepo( TEST_REPOSITORY, srcRepo , getProjectDirectory().resolve("src/test/repo-with-osgi-stage" ),  true );

    }
}
