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

import org.apache.archiva.rest.api.model.ArtifactTransferRequest;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.InternalServerErrorException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Olivier Lamy
 */
public class CopyArtifactTest
    extends AbstractArchivaRestTest
{


    @Test
    public void copyToAnEmptyRepo()
        throws Exception
    {
        try
        {
            initSourceTargetRepo();

            // START SNIPPET: copy-artifact
            // configure the artifact you want to copy
            // if package ommited default will be jar
            ArtifactTransferRequest artifactTransferRequest = new ArtifactTransferRequest();
            artifactTransferRequest.setGroupId( "org.apache.karaf.features" );
            artifactTransferRequest.setArtifactId( "org.apache.karaf.features.core" );
            artifactTransferRequest.setVersion( "2.2.2" );
            artifactTransferRequest.setRepositoryId( SOURCE_REPO_ID );
            artifactTransferRequest.setTargetRepositoryId( TARGET_REPO_ID );
            // retrieve the service
            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );
            // copy the artifact
            Boolean res = repositoriesService.copyArtifact( artifactTransferRequest );
            // END SNIPPET: copy-artifact
            assertTrue( res );

            String targetRepoPath = getManagedRepositoriesService( authorizationHeader ).getManagedRepository(
                TARGET_REPO_ID ).getLocation();

            Path artifact = Paths.get( targetRepoPath,
                                      "/org/apache/karaf/features/org.apache.karaf.features.core/2.2.2/org.apache.karaf.features.core-2.2.2.jar" );
            assertTrue( Files.exists(artifact) );
            Path pom = Paths.get( targetRepoPath,
                                 "/org/apache/karaf/features/org.apache.karaf.features.core/2.2.2/org.apache.karaf.features.core-2.2.2.pom" );

            assertTrue( "not exists " + pom, Files.exists(pom) );
            // TODO find a way to force metadata generation and test it !!
        }
        finally
        {
            cleanRepos();
        }
    }

    @Test( expected = InternalServerErrorException.class )
    public void copyNonExistingArtifact()
        throws Throwable
    {
        try
        {
            initSourceTargetRepo();

            ArtifactTransferRequest artifactTransferRequest = new ArtifactTransferRequest();
            artifactTransferRequest.setGroupId( "org.apache.karaf.features" );
            artifactTransferRequest.setArtifactId( "org.apache.karaf.features.core" );
            artifactTransferRequest.setVersion( "3.0.6552" );
            artifactTransferRequest.setRepositoryId( SOURCE_REPO_ID );
            artifactTransferRequest.setTargetRepositoryId( TARGET_REPO_ID );
            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );

            repositoriesService.copyArtifact( artifactTransferRequest );
        }
        catch ( InternalServerErrorException e )
        {
            // FIXME this doesn't work anymore with cxf 3.x????
            //Assertions.assertThat( e.getResponse().getStatusInfo().getReasonPhrase() ) //
            //    .contains( "cannot find artifact" );

            // previous test with cxf 2.x
            //assertTrue( e.getMessage() + " do not contains ''",
            //            StringUtils.contains( e.getMessage(), "cannot find artifact" ) );
            throw e;
        }
        finally
        {
            cleanRepos();
        }

    }

    @Ignore
    public void copyToAnExistingRepo()
        throws Exception
    {
        initSourceTargetRepo();
        cleanRepos();
    }
}
