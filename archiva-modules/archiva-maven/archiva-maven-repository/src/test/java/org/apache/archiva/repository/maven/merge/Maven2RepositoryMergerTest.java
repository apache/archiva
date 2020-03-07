package org.apache.archiva.repository.maven.merge;

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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.repository.maven.merge.Maven2RepositoryMerger;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith (ArchivaSpringJUnit4ClassRunner.class)
@ContextConfiguration (locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context-merge.xml" })
public class Maven2RepositoryMergerTest
    extends TestCase
{

    private static final String TEST_REPO_ID = "test";

    @Inject
    private Maven2RepositoryMerger repositoryMerger;

    @Inject
    @Named("archivaConfiguration#default")
    ArchivaConfiguration configuration;

    private MetadataRepository metadataRepository;

    private static RepositorySessionFactory repositorySessionFactory;

    private static RepositorySession session;

    static
    {
        repositorySessionFactory = mock(RepositorySessionFactory.class);
        session = mock( RepositorySession.class );

        try
        {
            when( repositorySessionFactory.createSession( ) ).thenReturn( session );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new RuntimeException( e );
        }

    }

    public static RepositorySessionFactory getRepositorySessionFactory() {
        return repositorySessionFactory;
    }



    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks( this );
        metadataRepository = mock( MetadataRepository.class );
        repositoryMerger.setRepositorySessionFactory( repositorySessionFactory );

    }

    private List<ArtifactMetadata> getArtifacts()
    {
        List<ArtifactMetadata> metadata = new ArrayList<>();
        ArtifactMetadata artifact1 = new ArtifactMetadata();
        artifact1.setNamespace( "com.example.test" );
        artifact1.setProject( "test-artifact" );
        artifact1.setVersion( "1.0-SNAPSHOT" );
        artifact1.setProjectVersion( "1.0-SNAPSHOT" );
        artifact1.setId( "test-artifact-1.0-20100308.230825-1.jar" );

        metadata.add( artifact1 );
        return metadata;
    }

    @Test
    public void testMerge()
        throws Exception
    {
        String targetRepoPath = "target/test-repository-target";
        Path mergedArtifact = Paths.get( targetRepoPath,
                                        "com/example/test/test-artifact/1.0-SNAPSHOT/test-artifact-1.0-20100308.230825-1.jar" );

        Path mavenMetadata = Paths.get( targetRepoPath, "com/example/test/test-artifact/maven-metadata.xml" );

        Path pom = Paths.get( targetRepoPath,
                             "com/example/test/test-artifact/1.0-SNAPSHOT/test-artifact-1.0-20100308.230825-1.pom" );

        for (Path testArtifact : new Path[] { mergedArtifact, mavenMetadata, pom }) {
            Files.deleteIfExists(testArtifact);
        }

        assertFalse( "Artifact file exists already", Files.exists(mergedArtifact) );
        assertFalse( "Metadata file exists already", Files.exists(mavenMetadata) );
        assertFalse( "Pom File exists already", Files.exists(pom) );
        Configuration c = new Configuration();
        ManagedRepositoryConfiguration testRepo = new ManagedRepositoryConfiguration();
        testRepo.setId( TEST_REPO_ID );
        testRepo.setLocation( "target/test-repository" );

        RepositoryScanningConfiguration repoScanConfig = new RepositoryScanningConfiguration();
        List<String> knownContentConsumers = new ArrayList<>();
        knownContentConsumers.add( "metadata-updater12" );
        repoScanConfig.setKnownContentConsumers( knownContentConsumers );
        c.setRepositoryScanning( repoScanConfig );

        ManagedRepositoryConfiguration targetRepo = new ManagedRepositoryConfiguration();
        targetRepo.setId( "target-rep" );
        targetRepo.setLocation( targetRepoPath );
        c.addManagedRepository( testRepo );
        c.addManagedRepository( targetRepo );
        configuration.save( c );


            when(metadataRepository.getArtifacts(session, TEST_REPO_ID)).thenReturn(getArtifacts());
            repositoryMerger.merge(metadataRepository, TEST_REPO_ID, "target-rep");
            verify(metadataRepository).getArtifacts(session, TEST_REPO_ID);
        assertTrue( Files.exists(mergedArtifact) );
        assertTrue( Files.exists(mavenMetadata) );
        assertTrue( Files.exists(pom) );
    }

    @Test
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

        Configuration c = new Configuration();
        ManagedRepositoryConfiguration testRepo = new ManagedRepositoryConfiguration();
        testRepo.setId( TEST_REPO_ID );
        testRepo.setLocation( "target/test-repository" );

        String sourceRepo = "src/test/resources/test-repository-with-conflict-artifacts";
        ManagedRepositoryConfiguration testRepoWithConflicts = new ManagedRepositoryConfiguration();
        testRepoWithConflicts.setId( sourceRepoId );
        testRepoWithConflicts.setLocation( sourceRepo );

        RepositoryScanningConfiguration repoScanConfig = new RepositoryScanningConfiguration();
        List<String> knownContentConsumers = new ArrayList<>();
        knownContentConsumers.add( "metadata-updater" );
        repoScanConfig.setKnownContentConsumers( knownContentConsumers );
        c.setRepositoryScanning( repoScanConfig );

        c.addManagedRepository( testRepo );
        c.addManagedRepository( testRepoWithConflicts );
        configuration.save( c );

        Path targetRepoFile = Paths.get(
            "/target/test-repository/com/example/test/test-artifact/1.0-SNAPSHOT/test-artifact-1.0-20100308.230825-1.jar" );
        targetRepoFile.toFile().setReadOnly();

            when(metadataRepository.getArtifacts(session, sourceRepoId)).thenReturn(sourceRepoArtifactsList);
            when(metadataRepository.getArtifacts(session, TEST_REPO_ID)).thenReturn(targetRepoArtifactsList);

            assertEquals(1, repositoryMerger.getConflictingArtifacts(metadataRepository, sourceRepoId,
                    TEST_REPO_ID).size());
            verify(metadataRepository).getArtifacts(session, TEST_REPO_ID);
    }

}