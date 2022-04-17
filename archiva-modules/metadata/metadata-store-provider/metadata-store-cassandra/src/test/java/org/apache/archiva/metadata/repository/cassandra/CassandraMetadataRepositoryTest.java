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

import com.datastax.oss.driver.api.core.CqlSession;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.repository.AbstractMetadataRepositoryTest;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataService;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.cassandra.model.ProjectVersionMetadataModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.truncate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Olivier Lamy
 */
@ExtendWith( SpringExtension.class )
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml"} )
public class CassandraMetadataRepositoryTest
    extends AbstractMetadataRepositoryTest
{

    private static final Logger LOGGER = LoggerFactory.getLogger( CassandraMetadataRepositoryTest.class );

    @Inject
    @Named(value = "archivaEntityManagerFactory#cassandra")
    CassandraArchivaManager cassandraArchivaManager;

    CassandraMetadataRepository cmr;

    RepositorySessionFactory sessionFactory;

    RepositorySession session;

    private static final CassandraContainer CASSANDRA =
            new CassandraContainer(DockerImageName.parse("cassandra")
                    .withTag(System.getProperty("cassandraVersion","3.11.2")));

    // because of @ExtendWith( SpringExtension.class ) @BeforeAll will not be executed before spring resolution so need to use this...
    static {
        LOGGER.info("initCassandra");
        CASSANDRA.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("org.apache.archiva.metadata.repository.cassandra.logs")));
        CASSANDRA.start();
        System.setProperty("cassandra.host", CASSANDRA.getHost());
        System.setProperty("cassandra.port", CASSANDRA.getMappedPort(9042).toString());
    }

    long cTime;
    int testNum = 0;
    final AtomicBoolean clearedTables = new AtomicBoolean( false );


    @Override
    protected RepositorySessionFactory getSessionFactory( )
    {
        return sessionFactory;
    }

    @Override
    protected MetadataRepository getRepository( )
    {
        return cmr;
    }

    @AfterAll
    public static void stopCassandra()
            throws Exception {
        CASSANDRA.close();
    }

    @BeforeEach
    @Override
    public void setUp()
        throws Exception
    {
        cTime = System.currentTimeMillis( );
        super.setUp();
        assertMaxTries =1;
        assertRetrySleepMs=10;

        Path directory = Paths.get( "target/test-repositories" );
        if ( Files.exists(directory) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( directory );
        }

        List<MetadataFacetFactory> factories = createTestMetadataFacetFactories();
        MetadataService metadataService = new MetadataService( );
        metadataService.setMetadataFacetFactories( factories );

        this.cmr = new CassandraMetadataRepository( metadataService, cassandraArchivaManager );

        sessionFactory = mock( RepositorySessionFactory.class );
        session = mock( RepositorySession.class );

        when( sessionFactory.createSession( ) ).thenReturn( session );

        if (!clearedTables.get())
        {
            clearReposAndNamespace( cassandraArchivaManager, clearedTables );
        }
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

        this.cmr.removeProjectVersion( null, TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION );

        assertThat(
            cmr.getProjectVersion( null , TEST_REPO_ID, TEST_NAMESPACE, TEST_PROJECT, TEST_PROJECT_VERSION ) ).isNull();

        assertThat( cmr.getMailingLists( key ) ).isNotNull().isEmpty();

        assertThat( cmr.getLicenses( key ) ).isNotNull().isEmpty();

        assertThat( cmr.getDependencies( key ) ).isNotNull().isEmpty();
    }


    @AfterEach
    public void shutdown()
        throws Exception
    {
        clearReposAndNamespace( cassandraArchivaManager, clearedTables );
        super.tearDown();
    }

    static void clearReposAndNamespace( final CassandraArchivaManager cassandraArchivaManager, final AtomicBoolean clearedFlag )
        throws Exception
    {
        if (cassandraArchivaManager!=null)
        {
            CqlSession session = cassandraArchivaManager.getSession( );
            {
                List<String> tables = Arrays.asList(
                    cassandraArchivaManager.getProjectFamilyName( ),
                    cassandraArchivaManager.getNamespaceFamilyName( ),
                    cassandraArchivaManager.getRepositoryFamilyName( ),
                    cassandraArchivaManager.getProjectVersionMetadataFamilyName( ),
                    cassandraArchivaManager.getArtifactMetadataFamilyName( ),
                    cassandraArchivaManager.getMetadataFacetFamilyName( ),
                    cassandraArchivaManager.getMailingListFamilyName( ),
                    cassandraArchivaManager.getLicenseFamilyName( ),
                    cassandraArchivaManager.getDependencyFamilyName( )
                );
                CompletableFuture.allOf(tables.stream()
                                .map(table -> session.executeAsync(truncate(table).build()))
                        .map( CompletionStage::toCompletableFuture ).collect( Collectors.toList( ) ).toArray( new CompletableFuture[0] ) )
                    .whenComplete( ( c, e ) -> {
                        if ( clearedFlag != null ) clearedFlag.set( true );
                        if (e!=null) {
                            System.err.println( "TRUNCATE ERROR DETECTED: " + e.getMessage( ) );
                        }
                    } ).get( )
                ;
            }
        } else {
            System.err.println( "cassandraArchivaManager is null" );
        }
    }

    static void clearReposAndNamespace( final CassandraArchivaManager cassandraArchivaManager)
        throws Exception {
        clearReposAndNamespace( cassandraArchivaManager, null );
    }

}
