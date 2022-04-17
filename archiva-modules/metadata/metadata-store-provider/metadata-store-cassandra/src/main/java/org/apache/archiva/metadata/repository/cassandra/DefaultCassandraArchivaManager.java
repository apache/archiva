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
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.querybuilder.schema.CreateIndex;
import com.datastax.oss.driver.api.querybuilder.schema.CreateKeyspace;
import com.datastax.oss.driver.api.querybuilder.schema.CreateTableWithOptions;
import org.apache.archiva.metadata.repository.RepositorySessionFactoryBean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.datastax.oss.driver.api.querybuilder.SchemaBuilder.*;
import static org.apache.archiva.metadata.repository.cassandra.model.ColumnNames.*;

/**
 * FIXME make all configuration not hardcoded :-)
 *
 * @author Olivier Lamy
 * @since 2.0.0
 */
@Service( "archivaEntityManagerFactory#cassandra" )
public class DefaultCassandraArchivaManager
    implements CassandraArchivaManager
{

    private static final Logger logger = LoggerFactory.getLogger( DefaultCassandraArchivaManager.class );

    @Inject
    private ApplicationContext applicationContext;

    private static final String CLUSTER_NAME = "archiva";

    private static final String KEYSPACE_NAME = "ArchivaKeySpace";

    private boolean started;

    // configurable???
    private String repositoryFamilyName = "repository";

    private String namespaceFamilyName = "namespace";

    private String projectFamilyName = PROJECT.toString( );

    private String projectVersionMetadataFamilyName = "projectversionmetadata";

    private String artifactMetadataFamilyName = "artifactmetadata";

    private String metadataFacetFamilyName = "metadatafacet";

    private String mailingListFamilyName = "mailinglist";

    private String licenseFamilyName = "license";

    private String dependencyFamilyName = "dependency";

    private String checksumFamilyName = "checksum";


    private static String[] projectVersionMetadataColumns;


    static
    {
        projectVersionMetadataColumns = new String[]{
            DEFAULT_PRIMARY_KEY,
            NAMESPACE_ID.toString( ),
            REPOSITORY_NAME.toString( ),
            PROJECT_VERSION.toString( ),
            PROJECT_ID.toString( ),
            DESCRIPTION.toString( ),
            URL.toString( ),
            NAME.toString( ),
            VERSION.toString( ),
            VERSION_PROPERTIES.toString( ),
            "incomplete",
            "ciManagement.system",
            "ciManagement.url",
            "issueManagement.system",
            "issueManagement.url",
            "organization.name",
            "organization.url",
            "scm.url",
            "scm.connection",
            "scm.developerConnection"
        };
        Arrays.sort( projectVersionMetadataColumns );
    }

    @Value( "${cassandra.host}" )
    private String cassandraHost;

    @Value( "${cassandra.port}" )
    private String cassandraPort;

    @Value( "${cassandra.maxActive}" )
    private int maxActive;

    @Value( "${cassandra.driverTimeoutMs}" )
    private int driverTimeoutMs;

    @Value( "${cassandra.readConsistencyLevel}" )
    private String readConsistencyLevel;

    @Value( "${cassandra.writeConsistencyLevel}" )
    private String writeConsistencyLevel;

    @Value( "${cassandra.replicationFactor}" )
    private int replicationFactor;

    @Value( "${cassandra.keyspace.name}" )
    private String keyspaceName;

    @Value( "${cassandra.cluster.name}" )
    private String clusterName;

    @Inject
    private RepositorySessionFactoryBean repositorySessionFactoryBean;

    DriverConfigLoader configLoader;

    CqlSession cqlSession;

    @Override
    public CqlSessionBuilder getSessionBuilder( )
    {
        return CqlSession.builder( ).withConfigLoader( configLoader ).withKeyspace( keyspaceName ).withLocalDatacenter( "datacenter1" );
    }

    @Override
    public CqlSession getSession( )
    {
        if (cqlSession==null || cqlSession.isClosed()) {
            this.cqlSession = getSessionBuilder( ).build( );
        }
        return this.cqlSession;
    }

    @PostConstruct
    public void initialize( )
    {
        // skip initialisation if not cassandra
        if ( !StringUtils.equals( repositorySessionFactoryBean.getId( ), "cassandra" ) )
        {
            return;
        }

        List<String> hostNames = new ArrayList<>( );
        hostNames.add( cassandraHost + ":" + cassandraPort );
        configLoader =
            DriverConfigLoader.programmaticBuilder( )
                .withStringList( DefaultDriverOption.CONTACT_POINTS, hostNames )
                .withInt( DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE, maxActive )
                .withInt( DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE, maxActive )
                //.withInt( DefaultDriverOption.CONNECTION_MAX_REQUESTS, maxActive )
                .withString( DefaultDriverOption.REQUEST_CONSISTENCY, readConsistencyLevel )
                .withDuration( DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofMillis( driverTimeoutMs ) )
                .build( );

        {

            CreateKeyspace cKeySpace = createKeyspace( keyspaceName ).ifNotExists( ).withSimpleStrategy( replicationFactor );
            CqlSession.builder( ).withConfigLoader( configLoader ).withLocalDatacenter( "datacenter1" ).build().execute( cKeySpace.build( ) );
        }

        CqlSession session = getSession( );

        {

            // namespace table
            {
                String tableName = getNamespaceFamilyName( );
                CreateTableWithOptions table = createTable( keyspaceName, tableName ).ifNotExists( )
                    .withPartitionKey( CassandraArchivaManager.DEFAULT_PRIMARY_KEY, DataTypes.TEXT )
                    .withColumn( NAME.toString( ), DataTypes.TEXT )
                    .withColumn( REPOSITORY_NAME.toString( ), DataTypes.TEXT )
                    .withCompactStorage( );
                session.execute( table.build( ) );
                CreateIndex index = createIndex( NAME.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( NAME.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( REPOSITORY_NAME.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( REPOSITORY_NAME.toString( ) );
                session.execute( index.build( ) );
            }

            // Repository Table
            {
                String tableName = getRepositoryFamilyName( );
                CreateTableWithOptions table = createTable( keyspaceName, tableName ).ifNotExists( )
                    .withPartitionKey( CassandraArchivaManager.DEFAULT_PRIMARY_KEY, DataTypes.TEXT )
                    .withColumn( REPOSITORY_NAME.toString( ), DataTypes.TEXT )
                    .withCompactStorage( );
                session.execute( table.build( ) );
                CreateIndex index = createIndex( REPOSITORY_NAME.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( REPOSITORY_NAME.toString( ) );
                session.execute( index.build( ) );

            }

            // Project table
            {
                String tableName = getProjectFamilyName( );
                CreateTableWithOptions table = createTable( keyspaceName, tableName ).ifNotExists( )
                    .withPartitionKey( CassandraArchivaManager.DEFAULT_PRIMARY_KEY, DataTypes.TEXT )
                    .withColumn( PROJECT_ID.toString( ), DataTypes.TEXT )
                    .withColumn( REPOSITORY_NAME.toString( ), DataTypes.TEXT )
                    .withColumn( NAMESPACE_ID.toString( ), DataTypes.TEXT )
                    .withColumn( PROJECT_PROPERTIES.toString( ), DataTypes.frozenMapOf( DataTypes.TEXT, DataTypes.TEXT ) )
                    .withCompactStorage( );
                session.execute( table.build( ) );
                CreateIndex index = createIndex( PROJECT_ID.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( PROJECT_ID.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( REPOSITORY_NAME.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( REPOSITORY_NAME.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( NAMESPACE_ID.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( NAMESPACE_ID.toString( ) );
                session.execute( index.build( ) );

            }

            // Project Version Metadata Model
            {
                String tableName = getProjectVersionMetadataFamilyName( );
                CreateTableWithOptions table = createTable( keyspaceName, tableName ).ifNotExists( )
                    .withPartitionKey( CassandraArchivaManager.DEFAULT_PRIMARY_KEY, DataTypes.TEXT )
                    .withColumn( NAMESPACE_ID.toString( ), DataTypes.TEXT )
                    .withColumn( REPOSITORY_NAME.toString( ), DataTypes.TEXT )
                    .withColumn( PROJECT_VERSION.toString( ), DataTypes.TEXT )
                    .withColumn( PROJECT_ID.toString( ), DataTypes.TEXT )
                    .withColumn( DESCRIPTION.toString( ), DataTypes.TEXT )
                    .withColumn( URL.toString( ), DataTypes.TEXT )
                    .withColumn( NAME.toString(), DataTypes.TEXT )
                    .withColumn( VERSION.toString(), DataTypes.TEXT )
                    .withColumn( VERSION_PROPERTIES.toString(), DataTypes.mapOf( DataTypes.TEXT, DataTypes.TEXT ) )
                    .withColumn( "incomplete", DataTypes.BOOLEAN )
                    .withColumn( "\"ciManagement.system\"", DataTypes.TEXT )
                    .withColumn( "\"ciManagement.url\"", DataTypes.TEXT )
                    .withColumn( "\"issueManagement.system\"", DataTypes.TEXT )
                    .withColumn( "\"issueManagement.url\"", DataTypes.TEXT )
                    .withColumn( "\"organization.name\"", DataTypes.TEXT )
                    .withColumn( "\"organization.url\"", DataTypes.TEXT )
                    .withColumn( "\"scm.url\"", DataTypes.TEXT )
                    .withColumn( "\"scm.connection\"", DataTypes.TEXT )
                    .withColumn( "\"scm.developerConnection\"", DataTypes.TEXT );
                session.execute( table.build( ) );
                CreateIndex index = createIndex( NAMESPACE_ID.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( NAMESPACE_ID.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( REPOSITORY_NAME.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( REPOSITORY_NAME.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( PROJECT_VERSION.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( PROJECT_VERSION.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( PROJECT_ID.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( PROJECT_ID.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( VERSION_PROPERTIES.toString( ) + "_idx" ).ifNotExists( ).onTable( tableName ).andColumnEntries(  VERSION_PROPERTIES.toString( ) );
                session.execute( index.build( ) );
            }

            // Artifact Metadata Model
            {
                String tableName = getArtifactMetadataFamilyName( );
                CreateTableWithOptions table = createTable( keyspaceName, tableName ).ifNotExists( )
                    .withPartitionKey( CassandraArchivaManager.DEFAULT_PRIMARY_KEY, DataTypes.TEXT )
                    .withColumn( ID.toString( ), DataTypes.TEXT )
                    .withColumn( REPOSITORY_NAME.toString( ), DataTypes.TEXT )
                    .withColumn( NAMESPACE_ID.toString( ), DataTypes.TEXT )
                    .withColumn( PROJECT_ID.toString( ), DataTypes.TEXT )
                    .withColumn( PROJECT_VERSION.toString( ), DataTypes.TEXT )
                    .withColumn( VERSION.toString( ), DataTypes.TEXT )
                    .withColumn( WHEN_GATHERED.toString( ), DataTypes.BIGINT )
                    .withColumn( SHA1.toString( ), DataTypes.TEXT )
                    .withColumn( MD5.toString( ), DataTypes.TEXT )
                    .withColumn( FILE_LAST_MODIFIED.toString(), DataTypes.BIGINT)
                    .withColumn( SIZE.toString(), DataTypes.BIGINT )
                    .withCompactStorage( );
                session.execute( table.build( ) );

                CreateIndex index = createIndex( ID.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( ID.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( REPOSITORY_NAME.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( REPOSITORY_NAME.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( NAMESPACE_ID.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( NAMESPACE_ID.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( PROJECT_ID.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( PROJECT_ID.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( PROJECT_VERSION.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( PROJECT_VERSION.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( VERSION.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( VERSION.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( WHEN_GATHERED.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( WHEN_GATHERED.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( SHA1.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( SHA1.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( MD5.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( MD5.toString( ) );
                session.execute( index.build( ) );

            }
            // Metadata Facet Model
            {
                String tableName = getMetadataFacetFamilyName( );
                CreateTableWithOptions table = createTable( keyspaceName, tableName ).ifNotExists( )
                    .withPartitionKey( CassandraArchivaManager.DEFAULT_PRIMARY_KEY, DataTypes.TEXT )
                    .withColumn( FACET_ID.toString( ), DataTypes.TEXT )
                    .withColumn( REPOSITORY_NAME.toString( ), DataTypes.TEXT )
                    .withColumn( NAME.toString( ), DataTypes.TEXT )
                    .withColumn( NAMESPACE_ID.toString( ), DataTypes.TEXT )
                    .withColumn( PROJECT_ID.toString( ), DataTypes.TEXT )
                    .withColumn( PROJECT_VERSION.toString( ), DataTypes.TEXT )
                    .withColumn( KEY.toString(), DataTypes.TEXT )
                    .withColumn( VALUE.toString(), DataTypes.TEXT)
                    .withColumn( WHEN_GATHERED.toString(), DataTypes.BIGINT )
                    .withCompactStorage( );
                session.execute( table.build( ) );

                CreateIndex index = createIndex( FACET_ID.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( FACET_ID.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( REPOSITORY_NAME.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( REPOSITORY_NAME.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( NAME.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( NAME.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( NAMESPACE_ID.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( NAMESPACE_ID.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( PROJECT_ID.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( PROJECT_ID.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( PROJECT_VERSION.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( PROJECT_VERSION.toString( ) );
                session.execute( index.build( ) );
            }
            // Checksum Table
            {
                String tableName = getChecksumFamilyName( );
                CreateTableWithOptions table = createTable( keyspaceName, tableName ).ifNotExists( )
                    .withPartitionKey( DEFAULT_PRIMARY_KEY, DataTypes.TEXT )
                    .withColumn( "\"artifactMetadataModel.key\"", DataTypes.TEXT )
                    .withColumn( CHECKSUM_ALG.toString( ), DataTypes.TEXT )
                    .withColumn( CHECKSUM_VALUE.toString( ), DataTypes.TEXT )
                    .withColumn( REPOSITORY_NAME.toString( ), DataTypes.TEXT )
                    .withCompactStorage( );
                session.execute( table.build( ) );

                CreateIndex index = createIndex( CHECKSUM_ALG.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( CHECKSUM_ALG.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( CHECKSUM_VALUE.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( CHECKSUM_VALUE.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( REPOSITORY_NAME.toString( ) ).ifNotExists( ).onTable( tableName ).andColumn( REPOSITORY_NAME.toString( ) );
                session.execute( index.build( ) );
            }
            // Mailinglist Table
            {
                String tableName = getMailingListFamilyName( );
                CreateTableWithOptions table = createTable( keyspaceName, tableName ).ifNotExists( )
                    .withPartitionKey( CassandraArchivaManager.DEFAULT_PRIMARY_KEY, DataTypes.TEXT )
                    .withColumn( NAME.toString(), DataTypes.TEXT )
                    .withColumn( "\"projectVersionMetadataModel.key\"", DataTypes.TEXT )
                    .withColumn( "mainArchiveUrl", DataTypes.TEXT )
                    .withColumn( "postAddress", DataTypes.TEXT )
                    .withColumn( "subscribeAddress", DataTypes.TEXT )
                    .withColumn( "unsubscribeAddress", DataTypes.TEXT )
                    .withColumn( "otherArchive", DataTypes.frozenListOf( DataTypes.TEXT ) )
                    .withCompactStorage( );
                session.execute( table.build( ) );

                CreateIndex index = createIndex( "\"projectVersionMetadataModel_key\"" ).ifNotExists( ).onTable( tableName ).andColumn( "\"\"projectVersionMetadataModel.key\"\"" );
                session.execute( index.build( ) );
            }

            // License Table
            {
                String tableName = getLicenseFamilyName( );
                CreateTableWithOptions table = createTable( keyspaceName, tableName ).ifNotExists( )
                    .withPartitionKey( CassandraArchivaManager.DEFAULT_PRIMARY_KEY, DataTypes.TEXT )
                    .withColumn( "\"projectVersionMetadataModel.key\"", DataTypes.TEXT )
                    .withColumn( NAME.toString(), DataTypes.TEXT )
                    .withColumn( URL.toString(), DataTypes.TEXT )
                    .withCompactStorage( );
                session.execute( table.build( ) );

                CreateIndex index = createIndex( "\"projectVersionMetadataModel_key\"" ).ifNotExists( ).onTable( tableName ).andColumn( "\"\"projectVersionMetadataModel.key\"\"" );
                session.execute( index.build( ) );
            }

            // Dependency Table
            {
                String tableName = getDependencyFamilyName( );
                CreateTableWithOptions table = createTable( keyspaceName, tableName ).ifNotExists( )
                    .withPartitionKey( CassandraArchivaManager.DEFAULT_PRIMARY_KEY, DataTypes.TEXT )
                    .withColumn( REPOSITORY_NAME.toString( ), DataTypes.TEXT )
                    .withColumn( GROUP_ID.toString( ), DataTypes.TEXT )
                    .withColumn( ARTIFACT_ID.toString( ), DataTypes.TEXT )
                    .withColumn( VERSION.toString( ), DataTypes.TEXT )
                    .withColumn( "\"projectVersionMetadataModel.key\"", DataTypes.TEXT )
                    .withColumn( "classifier", DataTypes.TEXT )
                    .withColumn( "optional", DataTypes.TEXT )
                    .withColumn( "scope", DataTypes.TEXT )
                    .withColumn( "systemPath", DataTypes.TEXT )
                    .withColumn( "type", DataTypes.TEXT )
                    .withCompactStorage( );

                session.execute( table.build( ) );

                CreateIndex index = createIndex( "groupIdIdx" ).ifNotExists( ).onTable( tableName ).andColumn( GROUP_ID.toString( ) );
                session.execute( index.build( ) );
                index = createIndex( "\"projectVersionMetadataModel_key\"" ).ifNotExists( ).onTable( tableName ).andColumn( "\"\"projectVersionMetadataModel.key\"\"" );
                session.execute( index.build( ) );

            }

        }


    }

    @Override
    public void start( )
    {
    }

    @PreDestroy
    @Override
    public void shutdown( )
    {
        if (this.cqlSession!=null) {
            this.cqlSession.close( );
        }
    }


    @Override
    public boolean started( )
    {
        return started;
    }


    @Override
    public String getRepositoryFamilyName( )
    {
        return repositoryFamilyName;
    }

    @Override
    public String getNamespaceFamilyName( )
    {
        return namespaceFamilyName;
    }

    @Override
    public String getProjectFamilyName( )
    {
        return projectFamilyName;
    }

    @Override
    public String getProjectVersionMetadataFamilyName( )
    {
        return projectVersionMetadataFamilyName;
    }

    public String[] getProjectVersionMetadataColumns() {
        return projectVersionMetadataColumns;
    }

    @Override
    public String getArtifactMetadataFamilyName( )
    {
        return artifactMetadataFamilyName;
    }

    @Override
    public String getMetadataFacetFamilyName( )
    {
        return metadataFacetFamilyName;
    }

    @Override
    public String getMailingListFamilyName( )
    {
        return mailingListFamilyName;
    }

    @Override
    public String getLicenseFamilyName( )
    {
        return licenseFamilyName;
    }

    @Override
    public String getDependencyFamilyName( )
    {
        return dependencyFamilyName;
    }

    @Override
    public String getChecksumFamilyName( )
    {
        return checksumFamilyName;
    }

    @Override
    public DriverConfigLoader getConfigLoader( )
    {
        return configLoader;
    }

    @Override
    public String getKeyspaceName( )
    {
        return keyspaceName;
    }
}
