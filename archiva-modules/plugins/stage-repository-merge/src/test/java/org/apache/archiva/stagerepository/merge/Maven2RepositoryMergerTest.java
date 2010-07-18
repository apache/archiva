package org.apache.archiva.stagerepository.merge;

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

import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.junit.Before;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

public class Maven2RepositoryMergerTest
    extends PlexusInSpringTestCase
{

    private static final String TEST_REPO_ID = "test";

    private static final String TARGET_REPOSITORY_ID = "target-repo";

    private Configuration config;

    @MockitoAnnotations.Mock
    private MetadataRepository metadataResolver;

    private RepositoryContentFactory repositoryFactory;

    private ArchivaConfiguration configuration;

    private Maven2RepositoryMerger repositoryMerger;

    private MetadataRepository metadataRepository;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks( this );
        metadataRepository = mock( MetadataRepository.class );
        repositoryMerger = (Maven2RepositoryMerger) lookup( RepositoryMerger.class, "maven2" );
        repositoryMerger.setMetadataRepository( metadataRepository );
    }

    private List<ArtifactMetadata> getArtifacts()
    {
        List<ArtifactMetadata> metadata = new ArrayList<ArtifactMetadata>();
        ArtifactMetadata artifact1 = new ArtifactMetadata();
        artifact1.setNamespace( "com.example.test" );
        artifact1.setProject( "test-artifact" );
        artifact1.setVersion( "1.0-SNAPSHOT" );
        artifact1.setProjectVersion( "1.0-SNAPSHOT" );
        artifact1.setId( "test-artifact-1.0-20100308.230825-1.jar" );

        metadata.add( artifact1 );
        return metadata;
    }

    public void testMerge()
        throws Exception
    {
        ArchivaConfiguration configuration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );
        Configuration c = new Configuration();
        ManagedRepositoryConfiguration testRepo = new ManagedRepositoryConfiguration();
        testRepo.setId( TEST_REPO_ID );
        testRepo.setLocation( getTestPath( "target/test-repository" ) );

        RepositoryScanningConfiguration repoScanConfig = new RepositoryScanningConfiguration();
        List<String> knownContentConsumers = new ArrayList<String>();
        knownContentConsumers.add( "metadata-updater12" );
        repoScanConfig.setKnownContentConsumers( knownContentConsumers );
        c.setRepositoryScanning( repoScanConfig );

        ManagedRepositoryConfiguration targetRepo = new ManagedRepositoryConfiguration();
        targetRepo.setId( "target-rep" );
        targetRepo.setLocation( getTestPath( "target" ) );
        c.addManagedRepository( testRepo );
        c.addManagedRepository( targetRepo );
        configuration.save( c );

        when( metadataRepository.getArtifacts( TEST_REPO_ID ) ).thenReturn( getArtifacts() );
        repositoryMerger.merge( TEST_REPO_ID, "target-rep" );
        verify( metadataRepository ).getArtifacts( TEST_REPO_ID );
    }

    public void testMergeWithOutConflictArtifacts()
        throws Exception
    {
        String sourceRepoId = "source-repo";
        ArtifactMetadata artifact1 = new ArtifactMetadata();
        artifact1.setNamespace( "org.testng" );
        artifact1.setProject( "testng" );
        artifact1.setVersion( "5.8" );
        artifact1.setProjectVersion( "5.8" );
        artifact1.setId( "testng-5.8-jdk15.jar" );
        artifact1.setRepositoryId( sourceRepoId );

        List<ArtifactMetadata> sourceRepoArtifactsList = getArtifacts();
        sourceRepoArtifactsList.add( artifact1 );
        List<ArtifactMetadata> targetRepoArtifactsList = getArtifacts();

        ArchivaConfiguration configuration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );
        Configuration c = new Configuration();
        ManagedRepositoryConfiguration testRepo = new ManagedRepositoryConfiguration();
        testRepo.setId( TEST_REPO_ID );
        testRepo.setLocation( getTestPath( "target/test-repository" ) );

        String sourceRepo = "src/test/resources/test-repository-with-conflict-artifacts";
        ManagedRepositoryConfiguration testRepoWithConflicts = new ManagedRepositoryConfiguration();
        testRepoWithConflicts.setId( sourceRepoId );
        testRepoWithConflicts.setLocation( getTestPath( sourceRepo ) );

        RepositoryScanningConfiguration repoScanConfig = new RepositoryScanningConfiguration();
        List<String> knownContentConsumers = new ArrayList<String>();
        knownContentConsumers.add( "metadata-updater" );
        repoScanConfig.setKnownContentConsumers( knownContentConsumers );
        c.setRepositoryScanning( repoScanConfig );

        c.addManagedRepository( testRepo );
        c.addManagedRepository( testRepoWithConflicts );
        configuration.save( c );

        File targetRepoFile = new File( getTestPath(
            "/target/test-repository/com/example/test/test-artifact/1.0-SNAPSHOT/test-artifact-1.0-20100308.230825-1.jar" ) );
        targetRepoFile.setReadOnly();

        when( metadataRepository.getArtifacts( sourceRepoId ) ).thenReturn( sourceRepoArtifactsList );
        when( metadataRepository.getArtifacts( TEST_REPO_ID ) ).thenReturn( targetRepoArtifactsList );

        assertEquals( 1, repositoryMerger.mergeWithOutConflictArtifacts( sourceRepoId, TEST_REPO_ID ).size() );
        verify( metadataRepository ).getArtifacts( TEST_REPO_ID );
    }

}