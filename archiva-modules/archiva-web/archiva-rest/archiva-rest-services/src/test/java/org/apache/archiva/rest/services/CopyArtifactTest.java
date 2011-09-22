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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.rest.api.model.ArtifactTransferRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import javax.inject.Inject;
import java.io.File;

/**
 * @author Olivier Lamy
 */
public class CopyArtifactTest
    extends AbstractArchivaRestTest
{
    static final String TARGET_REPO_ID = "test-copy-target";

    static final String SOURCE_REPO_ID = "test-origin-repo";

    @Inject
    private RepositoryContentFactory repositoryFactory;


    private void initSourceTargetRepo()
        throws Exception
    {
        File targetRepo = new File( "target/test-repo-copy" );
        if ( targetRepo.exists() )
        {
            FileUtils.deleteDirectory( targetRepo );
        }
        assertFalse( targetRepo.exists() );
        targetRepo.mkdirs();

        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( TARGET_REPO_ID, true );
            assertNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) );
        }
        ManagedRepository managedRepository = getTestManagedRepository();
        managedRepository.setId( TARGET_REPO_ID );
        managedRepository.setLocation( targetRepo.getCanonicalPath() );
        managedRepository.setCronExpression( "* * * * * ?" );
        getManagedRepositoriesService( authorizationHeader ).addManagedRepository( managedRepository );
        assertNotNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) );

        File originRepo = new File( "target/test-origin-repo" );
        if ( originRepo.exists() )
        {
            FileUtils.deleteDirectory( originRepo );
        }
        assertFalse( originRepo.exists() );
        FileUtils.copyDirectory( new File( "src/test/repo-with-osgi" ), originRepo );

        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( SOURCE_REPO_ID, true );
            assertNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) );
        }

        managedRepository = getTestManagedRepository();
        managedRepository.setId( SOURCE_REPO_ID );
        managedRepository.setLocation( originRepo.getCanonicalPath() );

        getManagedRepositoriesService( authorizationHeader ).addManagedRepository( managedRepository );
        assertNotNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) );

        getArchivaAdministrationService().addKnownContentConsumer( "create-missing-checksums" );
        getArchivaAdministrationService().addKnownContentConsumer( "metadata-updater" );

    }

    public void clean()  throws Exception
    {

        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( TARGET_REPO_ID, true );
            assertNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ) );
        }
        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( SOURCE_REPO_ID, true );
            assertNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SOURCE_REPO_ID ) );
        }

    }

    @Test
    public void copyToAnEmptyRepo()
        throws Exception
    {
        initSourceTargetRepo();

        // START SNIPPET: copy-artifact
        ArtifactTransferRequest artifactTransferRequest = new ArtifactTransferRequest();
        artifactTransferRequest.setGroupId( "org.apache.karaf.features" );
        artifactTransferRequest.setArtifactId( "org.apache.karaf.features.core" );
        artifactTransferRequest.setVersion( "2.2.2" );
        artifactTransferRequest.setSourceRepositoryId( SOURCE_REPO_ID );
        artifactTransferRequest.setTargetRepositoryId( TARGET_REPO_ID );
        Boolean res = getRepositoriesService( authorizationHeader ).copyArtifact( artifactTransferRequest );
        // END SNIPPET: copy-artifact
        assertTrue( res );

        ArtifactReference artifactReference = new ArtifactReference();
        artifactReference.setArtifactId( artifactTransferRequest.getArtifactId() );
        artifactReference.setGroupId( artifactTransferRequest.getGroupId() );
        artifactReference.setVersion( artifactTransferRequest.getVersion() );
        artifactReference.setClassifier( artifactTransferRequest.getClassifier() );
        String packaging = StringUtils.trim( artifactTransferRequest.getPackaging() );
        artifactReference.setType( StringUtils.isEmpty( packaging ) ? "jar" : packaging );

        String targetRepoPath =
            getManagedRepositoriesService( authorizationHeader ).getManagedRepository( TARGET_REPO_ID ).getLocation();

        File artifact = new File( targetRepoPath,
                                  "/org/apache/karaf/features/org.apache.karaf.features.core/2.2.2/org.apache.karaf.features.core-2.2.2.jar" );
        assertTrue( artifact.exists() );
        File pom = new File( targetRepoPath,
                             "/org/apache/karaf/features/org.apache.karaf.features.core/2.2.2/org.apache.karaf.features.core-2.2.2.pom" );

        assertTrue( "not exists " + pom.getPath(), pom.exists() );
        clean();
    }

    //@Test
    public void copyToAnExistingRepo()
        throws Exception
    {
        initSourceTargetRepo();
        clean();
    }
}
