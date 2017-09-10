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

    private Path repo = Paths.get( System.getProperty( "builddir" ), "test-repository" );

    private Path repoStage = Paths.get( System.getProperty( "builddir" ), "test-repository-stage" );

    @Test
    public void getMergeConflictedArtifacts()
        throws Exception
    {
        MergeRepositoriesService service = getMergeRepositoriesService( authorizationHeader );

        List<Artifact> artifactMetadatas = service.getMergeConflictedArtifacts( TEST_REPOSITORY + "-stage",
                                                                                TEST_REPOSITORY );

        log.info( "conflicts: {}", artifactMetadatas );

        assertThat( artifactMetadatas ).isNotNull().isNotEmpty().hasSize( 8 );
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

        MergeRepositoriesService service = getMergeRepositoriesService( authorizationHeader );

        service.mergeRepositories( TEST_REPOSITORY + "-stage", TEST_REPOSITORY, true );

        assertTrue( Files.exists(repo.resolve(mergedArtifactPath)) );
        assertTrue( Files.exists(repo.resolve(mergedArtifactPomPath)) );
    }

    @After
    public void deleteStageRepo()
        throws Exception
    {
        waitForScanToComplete( TEST_REPOSITORY );

        deleteTestRepo( TEST_REPOSITORY );

        org.apache.archiva.common.utils.FileUtils.deleteDirectory( repo );
        org.apache.archiva.common.utils.FileUtils.deleteDirectory( repoStage );
    }

    @Before
    public void createStageRepo()
        throws Exception
    {
        FileUtils.copyDirectory( Paths.get( System.getProperty( "basedir" ), "src/test/repo-with-osgi" ).toFile(), repo.toFile() );
        FileUtils.copyDirectory( Paths.get( System.getProperty( "basedir" ), "src/test/repo-with-osgi-stage" ).toFile(),
                                 repoStage.toFile() );

        createStagedNeededRepo( TEST_REPOSITORY, repo.toAbsolutePath().toString(), true );
    }
}
