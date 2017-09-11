package org.apache.archiva.metadata.repository.cassandra;

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

import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.repository.AbstractMetadataRepositoryTest;
import org.apache.archiva.metadata.repository.cassandra.model.ProjectVersionMetadataModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
public class CassandraMetadataRepositoryTest
    extends AbstractMetadataRepositoryTest
{
    @Inject
    @Named(value = "archivaEntityManagerFactory#cassandra")
    CassandraArchivaManager cassandraArchivaManager;

    CassandraMetadataRepository cmr;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        Path directory = Paths.get( "target/test-repositories" );
        if ( Files.exists(directory) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( directory );
        }

        Map<String, MetadataFacetFactory> factories = createTestMetadataFacetFactories();

        this.cmr = new CassandraMetadataRepository( factories, null, cassandraArchivaManager );
        this.repository = this.cmr;

        clearReposAndNamespace( cassandraArchivaManager );
    }

    /**
     * ensure all dependant tables are cleaned up (mailinglist, license, dependencies)
     *
     * @throws Exception
     */
    @Test
    public void clean_dependant_tables()
        throws Exception
    {

        super.testUpdateProjectVersionMetadataWithAllElements();

        String key = new ProjectVersionMetadataModel.KeyBuilder().withRepository( TEST_REPO_ID ) //
            .withNamespace( TEST_NAMESPACE ) //
            .withProjectId( TEST_PROJECT ) //
            .withProjectVersion( TEST_PROJECT_VERSION ) //
            .withId( TEST_PROJECT_VERSION ) //
            .build();

        this.cmr.removeProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        assertThat(
            repository.getProjectVersion( TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION ) ).isNull();

        assertThat( cmr.getMailingLists( key ) ).isNotNull().isEmpty();

        assertThat( cmr.getLicenses( key ) ).isNotNull().isEmpty();

        assertThat( cmr.getDependencies( key ) ).isNotNull().isEmpty();
    }


    @After
    public void shutdown()
        throws Exception
    {
        clearReposAndNamespace( cassandraArchivaManager );
        super.tearDown();
    }

    static void clearReposAndNamespace( CassandraArchivaManager cassandraArchivaManager )
        throws Exception
    {
        cassandraArchivaManager.getCluster().truncate( cassandraArchivaManager.getKeyspace().getKeyspaceName(),
                                                       cassandraArchivaManager.getProjectFamilyName() );

        cassandraArchivaManager.getCluster().truncate( cassandraArchivaManager.getKeyspace().getKeyspaceName(),
                                                       cassandraArchivaManager.getNamespaceFamilyName() );

        cassandraArchivaManager.getCluster().truncate( cassandraArchivaManager.getKeyspace().getKeyspaceName(),
                                                       cassandraArchivaManager.getRepositoryFamilyName() );

        cassandraArchivaManager.getCluster().truncate( cassandraArchivaManager.getKeyspace().getKeyspaceName(),
                                                       cassandraArchivaManager.getProjectVersionMetadataFamilyName() );

        cassandraArchivaManager.getCluster().truncate( cassandraArchivaManager.getKeyspace().getKeyspaceName(),
                                                       cassandraArchivaManager.getArtifactMetadataFamilyName() );

        cassandraArchivaManager.getCluster().truncate( cassandraArchivaManager.getKeyspace().getKeyspaceName(),
                                                       cassandraArchivaManager.getMetadataFacetFamilyName() );

        cassandraArchivaManager.getCluster().truncate( cassandraArchivaManager.getKeyspace().getKeyspaceName(),
                                                       cassandraArchivaManager.getMailingListFamilyName() );

        cassandraArchivaManager.getCluster().truncate( cassandraArchivaManager.getKeyspace().getKeyspaceName(),
                                                       cassandraArchivaManager.getLicenseFamilyName() );

        cassandraArchivaManager.getCluster().truncate( cassandraArchivaManager.getKeyspace().getKeyspaceName(),
                                                       cassandraArchivaManager.getDependencyFamilyName() );

    }

}
