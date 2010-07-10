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
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.reports.RepositoryProblemFacet;
import org.mockito.MockitoAnnotations;
import org.junit.Before;

public class Maven2RepositoryMergerTest
    extends PlexusInSpringTestCase
{

    private static final String SOURCE_REPOSITORY_ID = "test-repository";

    private static final String TARGET_REPOSITORY_ID = "target-repo";

    private static final String TEST_REPO_ID = "test";

    // private static final String TARGET_REPOSITORY_ID = "target-repo";

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
        ArchivaConfiguration configuration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );
        Configuration c = new Configuration();
        ManagedRepositoryConfiguration testRepo = new ManagedRepositoryConfiguration();
        testRepo.setId( TEST_REPO_ID );
        testRepo.setLocation( getTestPath( "target/test-repository" ) );
        // testRepo.setLocation( "/boot/gsoc/apps/apache-archiva-1.4-SNAPSHOT/data/repositories/internal" );

        ManagedRepositoryConfiguration targetRepo = new ManagedRepositoryConfiguration();
        targetRepo.setId( "target-rep" );
        targetRepo.setLocation( getTestPath( "src/test/resources/target-repo" ) );
        c.addManagedRepository( testRepo );
        c.addManagedRepository( targetRepo );
        configuration.save( c );

        repositoryMerger = (Maven2RepositoryMerger) lookup( RepositoryMerger.class, "maven2" );

        metadataRepository = (MetadataRepository) lookup( MetadataRepository.class );

    }

    public void testMerge()
        throws Exception
    {
        repositoryMerger.merge( TEST_REPO_ID, "target-rep" );
        // assert( true , (metadataRepository.getArtifacts( TEST_REPO_ID ).size() > 0 ) );

    }
}