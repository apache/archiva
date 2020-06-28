package org.apache.archiva.metadata.repository.file;

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

import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.repository.AbstractMetadataRepositoryTest;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataService;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.junit.Before;
import org.junit.Ignore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileMetadataRepositoryTest
    extends AbstractMetadataRepositoryTest
{

    private FileMetadataRepository repository;
    private RepositorySessionFactory sessionFactory = new FileRepositorySessionFactory();

    @Override
    protected MetadataRepository getRepository( )
    {
        return this.repository;
    }

    @Override
    protected RepositorySessionFactory getSessionFactory( )
    {
        return this.sessionFactory;
    }

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();
        assertMaxTries = 1;
        assertRetrySleepMs = 10;

        Path directory = Paths.get( "target/test-repositories" );
        if (Files.exists(directory))
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( directory );
        }
        ArchivaConfiguration config = createTestConfiguration( directory );
        List<MetadataFacetFactory> factories = createTestMetadataFacetFactories();
        MetadataService metadataService = new MetadataService( );
        metadataService.setMetadataFacetFactories( factories );

        this.repository = new FileMetadataRepository( metadataService, config );
    }

    @Override
    @Ignore
    public void testGetArtifactsByProjectVersionMetadata()
        throws Exception
    {
        // TODO not implemented
    }

    @Override
    @Ignore
    public void testGetArtifactsByProjectVersionMetadataNoRepository()
        throws Exception
    {
        // TODO not implemented
    }

    @Override
    @Ignore
    public void testGetArtifactsByProjectVersionMetadataAllRepositories()
        throws Exception
    {
        // TODO not implemented
    }

    @Override
    @Ignore
    public void testGetArtifactsByMetadataAllRepositories()
        throws Exception
    {
        // TODO not implemented
    }

    @Override
    @Ignore
    public void testGetArtifactsByPropertySingleResult()
        throws Exception
    {
        // TODO not implemented
    }

    @Override
    @Ignore
    public void testSearchArtifactsByKey()
        throws Exception
    {
        // TODO not implemented
    }

    @Override
    @Ignore
    public void testSearchArtifactsByKeyExact()
        throws Exception
    {
        // TODO not implemented
    }

    @Override
    @Ignore
    public void testSearchArtifactsFullText()
        throws Exception
    {
        // TODO not implemented
    }

    @Override
    @Ignore
    public void testSearchArtifactsFullTextExact()
        throws Exception
    {
        // TODO not implemented
    }

    @Override
    @Ignore
    public void testSearchArtifactsByFacetKeyAllRepos()
        throws Exception
    {
        // TODO not implemented
    }

    @Override
    @Ignore
    public void testSearchArtifactsByFacetKey()
        throws Exception
    {
        // TODO not implemented
    }

    @Override
    @Ignore
    public void testSearchArtifactsFullTextByFacet()
        throws Exception
    {
        // TODO not implemented
    }

    protected static ArchivaConfiguration createTestConfiguration( Path directory )
    {
        ArchivaConfiguration config = mock( ArchivaConfiguration.class );
        Configuration configData = new Configuration();
        configData.addManagedRepository( createManagedRepository( TEST_REPO_ID, directory ) );
        configData.addManagedRepository( createManagedRepository( "other-repo", directory ) );
        when( config.getConfiguration() ).thenReturn( configData );
        return config;
    }

    private static ManagedRepositoryConfiguration createManagedRepository( String repoId, Path directory )
    {
        ManagedRepositoryConfiguration managedRepository = new ManagedRepositoryConfiguration();
        managedRepository.setId( repoId );
        managedRepository.setLocation( directory.resolve( repoId ).toAbsolutePath().toString() );
        return managedRepository;
    }
}
