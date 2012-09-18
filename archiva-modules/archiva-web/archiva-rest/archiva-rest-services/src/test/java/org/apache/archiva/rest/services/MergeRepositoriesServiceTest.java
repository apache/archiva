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
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.archiva.rest.api.services.MergeRepositoriesService;
import org.apache.commons.io.FileUtils;
import org.fest.assertions.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class MergeRepositoriesServiceTest
    extends AbstractArchivaRestTest
{

    @Override
    @Before
    public void startServer()
        throws Exception
    {

        FileUtils.copyDirectory( new File( System.getProperty( "basedir" ), "src/test/repo-with-osgi" ),
                                 new File( System.getProperty( "builddir" ), "test-repository" ) );
        FileUtils.copyDirectory( new File( System.getProperty( "basedir" ), "src/test/repo-with-osgi-stage" ),
                                 new File( System.getProperty( "builddir" ), "test-repository-stage" ) );
        super.startServer();

    }

    @Override
    @After
    public void stopServer()
        throws Exception
    {
        // TODO delete repositories
        super.stopServer();
        FileUtils.deleteDirectory( new File( System.getProperty( "builddir" ), "test-repository" ) );
        FileUtils.deleteDirectory( new File( System.getProperty( "builddir" ), "test-repository-stage" ) );
    }

    @Test
    public void mergeConflictedArtifacts()
        throws Exception
    {
        try
        {
            String testRepoId = "test-repository";
            createStagedNeededRepo( testRepoId,
                                    new File( System.getProperty( "builddir" ), "test-repository" ).getAbsolutePath(),
                                    true );

            // force jcr data population !
            BrowseService browseService = getBrowseService( authorizationHeader, false );
            browseService.getRootGroups( testRepoId );
            browseService.getRootGroups( testRepoId + "-stage" );

            MergeRepositoriesService service = getMergeRepositoriesService();

            List<Artifact> artifactMetadatas = service.getMergeConflictedArtifacts( testRepoId );

            log.info( "conflicts: {}", artifactMetadatas );

            Assertions.assertThat( artifactMetadatas ).isNotNull().isNotEmpty().hasSize( 8 );

            deleteTestRepo( testRepoId );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw e;
        }
    }
}
