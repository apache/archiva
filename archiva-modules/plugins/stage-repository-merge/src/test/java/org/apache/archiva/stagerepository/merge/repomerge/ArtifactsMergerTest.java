package org.apache.archiva.stagerepository.merge.repomerge;

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

import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.stub;

import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.archiva.stagerepository.merge.repodetails.SourceAritfacts;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataResolver;
//import com.sun.xml.internal.ws.api.wsdl.parser.MetaDataResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.io.File;

public class ArtifactsMergerTest
    extends PlexusInSpringTestCase
{

    private static final String SOURCE_REPOSITORY_ID = "test-repository";

    private static final String TARGET_REPOSITORY_ID = "target-repo";

    // private static final String TARGET_REPOSITORY_ID = "target-repo";

    private Configuration config;

    @MockitoAnnotations.Mock
    private MetadataResolver metadataResolver;

    private RepositoryContentFactory repositoryFactory;

    private ArchivaConfiguration configuration;

    private SourceAritfacts sourceArtifacts;

    private ArtifactsMerger merger;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks( this );
        metadataResolver = mock( MetadataResolver.class );
        repositoryFactory = mock( RepositoryContentFactory.class );
        configuration = mock( ArchivaConfiguration.class );
        sourceArtifacts = mock( SourceAritfacts.class );
        sourceArtifacts.setRepoId( SOURCE_REPOSITORY_ID );
        sourceArtifacts.setMetadataResolver( metadataResolver );
        setRepositoryConfigurations();
        merger = new ArtifactsMerger( TARGET_REPOSITORY_ID, SOURCE_REPOSITORY_ID );
        merger.setConfiguration( configuration );
        merger.setRepositoryFactory( repositoryFactory );
        merger.setMetadataResolver( metadataResolver );
        setSourceArtifacts();
    }

    @Test
    public void setSourceArtifacts()
    {
        when( sourceArtifacts.getSourceArtifactList() ).thenReturn( getArtifacts() );
        merger.setSourceArtifacts( sourceArtifacts );
        verify( sourceArtifacts ).getSourceArtifactList();
    }

    @Test
    public void testDomerge()
        throws Exception
    {
        ManagedRepositoryContent sourceRepoContent = new ManagedDefaultRepositoryContent();
        sourceRepoContent.setRepository( config.findManagedRepositoryById( SOURCE_REPOSITORY_ID ) );

        ManagedRepositoryContent targetRepoContent = new ManagedDefaultRepositoryContent();
        sourceRepoContent.setRepository( config.findManagedRepositoryById( TARGET_REPOSITORY_ID ) );

        when( configuration.getConfiguration() ).thenReturn( config );
        when( repositoryFactory.getManagedRepositoryContent( SOURCE_REPOSITORY_ID ) ).thenReturn( sourceRepoContent );
        when( repositoryFactory.getManagedRepositoryContent( TARGET_REPOSITORY_ID ) ).thenReturn( targetRepoContent );
        when( sourceArtifacts.getSourceArtifactList() ).thenReturn( getArtifacts() );
        when( metadataResolver.getArtifacts( TARGET_REPOSITORY_ID, "archiva", "archiva", "1.2.1" ) ).thenReturn( getMetaDataList() );
        merger.doMerge();

        // verify(configuration);
        // verify(repositoryFactory);
        // verify(repositoryFactory);
        // verify(sourceArtifacts);
        verify( configuration ).getConfiguration();
        verify( repositoryFactory ).getManagedRepositoryContent( SOURCE_REPOSITORY_ID );
        verify( repositoryFactory ).getManagedRepositoryContent( TARGET_REPOSITORY_ID );
        verify( sourceArtifacts ).getSourceArtifactList();
        verify( metadataResolver ).getArtifacts( TARGET_REPOSITORY_ID, "org.apache.archiva", "archiva", "1.2.1" );
    }

    public Collection<ArchivaArtifact> getArtifacts()
    {
        ArchivaArtifact a1 =
            new ArchivaArtifact( "org.apache.archiva", "archiva", "1.2.1", "", "jar", SOURCE_REPOSITORY_ID );
        ArchivaArtifact a2 =
            new ArchivaArtifact( "org.apache.archiva", "archiva", "1.5", "", "jar", SOURCE_REPOSITORY_ID );
        ArrayList<ArchivaArtifact> list = new ArrayList<ArchivaArtifact>();
        list.add( a1 );
        // list.add(a2) ;
        return list;
    }

    public Collection<ArtifactMetadata> getMetaDataList()
    {
        ArtifactMetadata m1 = new ArtifactMetadata();
        m1.setNamespace( "org.apache.archiva" );
        m1.setProject( "archiva" );
        m1.setVersion( "1.2.1" );
        ArrayList<ArtifactMetadata> list = new ArrayList<ArtifactMetadata>();
        list.add( m1 );
        return list;
    }

    public void setRepositoryConfigurations()
    {
        File sourceRepoFile = new File( getBasedir(), "src/test/resources/test-repository" );
        File targetRepoFile = new File( getBasedir(), "src/test/resources/target-repo" );
        // sourceRepoFile.mkdirs();
        // targetRepoFile.mkdirs();

        assertTrue( sourceRepoFile.exists() );
        this.config = new Configuration();
        RepositoryScanningConfiguration repoScanConfig = new RepositoryScanningConfiguration();
        List<String> knownContentConsumers = new ArrayList<String>();
        knownContentConsumers.add( "metadata-updater12" );
        repoScanConfig.setKnownContentConsumers( knownContentConsumers );
        config.setRepositoryScanning( repoScanConfig );
        // config.setManagedRepositories();
        ManagedRepositoryConfiguration sourceRepoConfig = new ManagedRepositoryConfiguration();
        sourceRepoConfig.setId( SOURCE_REPOSITORY_ID );
        sourceRepoConfig.setLayout( "default" );
        sourceRepoConfig.setLocation( sourceRepoFile.getPath() );
        sourceRepoConfig.setName( SOURCE_REPOSITORY_ID );
        sourceRepoConfig.setBlockRedeployments( true );

        ManagedRepositoryConfiguration targetRepoConfig = new ManagedRepositoryConfiguration();
        targetRepoConfig.setId( TARGET_REPOSITORY_ID );
        targetRepoConfig.setLayout( "default" );
        targetRepoConfig.setLocation( targetRepoFile.getPath() );
        targetRepoConfig.setName( TARGET_REPOSITORY_ID );
        targetRepoConfig.setBlockRedeployments( true );

        this.config.addManagedRepository( sourceRepoConfig );
        this.config.addManagedRepository( targetRepoConfig );
    }
}
