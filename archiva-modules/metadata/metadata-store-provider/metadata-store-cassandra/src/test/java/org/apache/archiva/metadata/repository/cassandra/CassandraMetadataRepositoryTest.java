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
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.truncate;
import static com.datastax.oss.driver.api.querybuilder.SchemaBuilder.dropTable;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
@ExtendWith( SpringExtension.class )
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
public class CassandraMetadataRepositoryTest
    extends AbstractMetadataRepositoryTest
{
    @Inject
    @Named(value = "archivaEntityManagerFactory#cassandra")
    CassandraArchivaManager cassandraArchivaManager;

    CassandraMetadataRepository cmr;

    IMocksControl sessionFactoryControl;
    RepositorySessionFactory sessionFactory;

    IMocksControl sessionControl;
    RepositorySession session;

    long cTime;
    int testNum = 0;
    AtomicBoolean clearedTables = new AtomicBoolean( false );


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

    @BeforeEach
    public void setUp( TestInfo testInfo )
        throws Exception
    {
        cTime = System.currentTimeMillis( );
        System.err.println( "Setting up "+(testNum++) + " - " + testInfo.getDisplayName() );
        super.setUp();
        System.err.println( "Setting up 2 " + testInfo.getDisplayName( ) + " - " + (System.currentTimeMillis( ) - cTime) );
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

        sessionFactoryControl = EasyMock.createControl( );
        sessionFactory = sessionFactoryControl.createMock( RepositorySessionFactory.class );
        sessionControl = EasyMock.createControl( );
        session = sessionControl.createMock( RepositorySession.class );

        EasyMock.expect( sessionFactory.createSession( ) ).andStubReturn( session );

        sessionFactoryControl.replay();

        if (!clearedTables.get())
        {
            clearReposAndNamespace( cassandraArchivaManager );
            clearedTables.set( true );
        }
        System.err.println( "Finished setting up "+testInfo.getDisplayName() + " - " + (System.currentTimeMillis( ) - cTime) );
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
    public void shutdown(TestInfo testInfo)
        throws Exception
    {
        System.err.println( "Shutting down " + testInfo.getDisplayName( ) + " - " + ( System.currentTimeMillis( ) - cTime ) );
        clearReposAndNamespace( cassandraArchivaManager );
        clearedTables.set( true );
        super.tearDown();
        System.err.println( "Shutting down finished" + testInfo.getDisplayName( ) + " - " + ( System.currentTimeMillis( ) - cTime ) );
    }

    static void clearReposAndNamespace( CassandraArchivaManager cassandraArchivaManager )
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
                for ( String table : tables )
                {
                    session.execute( truncate( table ).build( ) );
                }

            }
        } else {
            System.err.println( "cassandraArchivaManager is null" );
        }
    }

}
