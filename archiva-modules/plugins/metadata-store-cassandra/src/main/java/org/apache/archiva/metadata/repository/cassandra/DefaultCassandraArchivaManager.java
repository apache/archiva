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

import me.prettyprint.cassandra.model.BasicColumnDefinition;
import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ColumnIndexType;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.factory.HFactory;
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
import java.util.ArrayList;
import java.util.List;

import static org.apache.archiva.metadata.repository.cassandra.model.ColumnNames.*;

/**
 * FIXME make all configuration not hardcoded :-)
 *
 * @author Olivier Lamy
 * @since 2.0.0
 */
@Service("archivaEntityManagerFactory#cassandra")
public class DefaultCassandraArchivaManager
    implements CassandraArchivaManager
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ApplicationContext applicationContext;

    private static final String CLUSTER_NAME = "archiva";

    private static final String KEYSPACE_NAME = "ArchivaKeySpace";

    private boolean started;

    private Cluster cluster;

    private Keyspace keyspace;

    // configurable???
    private String repositoryFamilyName = "repository";

    private String namespaceFamilyName = "namespace";

    private String projectFamilyName = PROJECT.toString();

    private String projectVersionMetadataFamilyName = "projectversionmetadata";

    private String artifactMetadataFamilyName = "artifactmetadata";

    private String metadataFacetFamilyName = "metadatafacet";

    private String mailingListFamilyName = "mailinglist";

    private String licenseFamilyName = "license";

    private String dependencyFamilyName = "dependency";

    private String checksumFamilyName = "checksum";

    @Value("${cassandra.host}")
    private String cassandraHost;

    @Value("${cassandra.port}")
    private String cassandraPort;

    @Value("${cassandra.maxActive}")
    private int maxActive;

    @Value("${cassandra.readConsistencyLevel}")
    private String readConsistencyLevel;

    @Value("${cassandra.writeConsistencyLevel}")
    private String writeConsistencyLevel;

    @Value("${cassandra.replicationFactor}")
    private int replicationFactor;

    @Value("${cassandra.keyspace.name}")
    private String keyspaceName;

    @Value("${cassandra.cluster.name}")
    private String clusterName;

    @Inject
    private RepositorySessionFactoryBean repositorySessionFactoryBean;

    @PostConstruct
    public void initialize()
    {
        // skip initialisation if not cassandra
        if ( !StringUtils.equals( repositorySessionFactoryBean.getId(), "cassandra" ) )
        {
            return;
        }
        final CassandraHostConfigurator configurator =
            new CassandraHostConfigurator( cassandraHost + ":" + cassandraPort );
        configurator.setMaxActive( maxActive );
        //configurator.setCassandraThriftSocketTimeout(  );

        cluster = HFactory.getOrCreateCluster( clusterName, configurator );

        final ConfigurableConsistencyLevel consistencyLevelPolicy = new ConfigurableConsistencyLevel();
        consistencyLevelPolicy.setDefaultReadConsistencyLevel( HConsistencyLevel.valueOf( readConsistencyLevel ) );
        consistencyLevelPolicy.setDefaultWriteConsistencyLevel( HConsistencyLevel.valueOf( writeConsistencyLevel ) );
        keyspace = HFactory.createKeyspace( keyspaceName, cluster, consistencyLevelPolicy );

        List<ColumnFamilyDefinition> cfds = new ArrayList<>();

        // namespace table
        {

            final ColumnFamilyDefinition namespace =
                HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                       getNamespaceFamilyName(), //
                                                       ComparatorType.UTF8TYPE );
            cfds.add( namespace );

            // creating indexes for cql query

            BasicColumnDefinition nameColumn = new BasicColumnDefinition();
            nameColumn.setName( StringSerializer.get().toByteBuffer( NAME.toString() ) );
            nameColumn.setIndexName( NAME.toString() );
            nameColumn.setIndexType( ColumnIndexType.KEYS );
            nameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            namespace.addColumnDefinition( nameColumn );

            BasicColumnDefinition repositoryIdColumn = new BasicColumnDefinition();
            repositoryIdColumn.setName( StringSerializer.get().toByteBuffer( REPOSITORY_NAME.toString() ) );
            repositoryIdColumn.setIndexName( REPOSITORY_NAME.toString() );
            repositoryIdColumn.setIndexType( ColumnIndexType.KEYS );
            repositoryIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            namespace.addColumnDefinition( repositoryIdColumn );
        }

        // repository table
        {
            final ColumnFamilyDefinition repository =
                HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                       getRepositoryFamilyName(), //
                                                       ComparatorType.UTF8TYPE );

            cfds.add( repository );

            BasicColumnDefinition nameColumn = new BasicColumnDefinition();
            nameColumn.setName( StringSerializer.get().toByteBuffer( REPOSITORY_NAME.toString() ) );
            nameColumn.setIndexName( REPOSITORY_NAME.toString() );
            nameColumn.setIndexType( ColumnIndexType.KEYS );
            nameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            repository.addColumnDefinition( nameColumn );
        }

        // project table
        {

            final ColumnFamilyDefinition project = HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                                                          getProjectFamilyName(), //
                                                                                          ComparatorType.UTF8TYPE );
            cfds.add( project );

            // creating indexes for cql query

            BasicColumnDefinition projectIdColumn = new BasicColumnDefinition();
            projectIdColumn.setName( StringSerializer.get().toByteBuffer( PROJECT_ID.toString() ) );
            projectIdColumn.setIndexName( PROJECT_ID.toString() );
            projectIdColumn.setIndexType( ColumnIndexType.KEYS );
            projectIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            project.addColumnDefinition( projectIdColumn );

            BasicColumnDefinition repositoryIdColumn = new BasicColumnDefinition();
            repositoryIdColumn.setName( StringSerializer.get().toByteBuffer( REPOSITORY_NAME.toString() ) );
            repositoryIdColumn.setIndexName( REPOSITORY_NAME.toString() );
            repositoryIdColumn.setIndexType( ColumnIndexType.KEYS );
            repositoryIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            project.addColumnDefinition( repositoryIdColumn );

            BasicColumnDefinition namespaceIdColumn = new BasicColumnDefinition();
            namespaceIdColumn.setName( StringSerializer.get().toByteBuffer( NAMESPACE_ID.toString() ) );
            namespaceIdColumn.setIndexName( NAMESPACE_ID.toString() );
            namespaceIdColumn.setIndexType( ColumnIndexType.KEYS );
            namespaceIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            project.addColumnDefinition( namespaceIdColumn );
        }

        //projectversionmetadatamodel
        {

            final ColumnFamilyDefinition projectVersionMetadataModel =
                HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                       getProjectVersionMetadataFamilyName(), //
                                                       ComparatorType.UTF8TYPE );
            cfds.add( projectVersionMetadataModel );

            // creating indexes for cql query

            BasicColumnDefinition namespaceIdColumn = new BasicColumnDefinition();
            namespaceIdColumn.setName( StringSerializer.get().toByteBuffer( NAMESPACE_ID.toString() ) );
            namespaceIdColumn.setIndexName( NAMESPACE_ID.toString() );
            namespaceIdColumn.setIndexType( ColumnIndexType.KEYS );
            namespaceIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            projectVersionMetadataModel.addColumnDefinition( namespaceIdColumn );

            BasicColumnDefinition repositoryNameColumn = new BasicColumnDefinition();
            repositoryNameColumn.setName( StringSerializer.get().toByteBuffer( REPOSITORY_NAME.toString() ) );
            repositoryNameColumn.setIndexName( REPOSITORY_NAME.toString() );
            repositoryNameColumn.setIndexType( ColumnIndexType.KEYS );
            repositoryNameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            projectVersionMetadataModel.addColumnDefinition( repositoryNameColumn );

            BasicColumnDefinition idColumn = new BasicColumnDefinition();
            idColumn.setName( StringSerializer.get().toByteBuffer( ID.toString() ) );
            idColumn.setIndexName( ID.toString() );
            idColumn.setIndexType( ColumnIndexType.KEYS );
            idColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            projectVersionMetadataModel.addColumnDefinition( idColumn );

            BasicColumnDefinition projectIdColumn = new BasicColumnDefinition();
            projectIdColumn.setName( StringSerializer.get().toByteBuffer( PROJECT_ID.toString() ) );
            projectIdColumn.setIndexName( PROJECT_ID.toString() );
            projectIdColumn.setIndexType( ColumnIndexType.KEYS );
            projectIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            projectVersionMetadataModel.addColumnDefinition( projectIdColumn );

        }

        // artifactmetadatamodel table
        {

            final ColumnFamilyDefinition artifactMetadataModel =
                HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                       getArtifactMetadataFamilyName(), //
                                                       ComparatorType.UTF8TYPE );
            cfds.add( artifactMetadataModel );

            // creating indexes for cql query

            BasicColumnDefinition idColumn = new BasicColumnDefinition();
            idColumn.setName( StringSerializer.get().toByteBuffer( ID.toString() ) );
            idColumn.setIndexName( ID.toString() );
            idColumn.setIndexType( ColumnIndexType.KEYS );
            idColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( idColumn );

            BasicColumnDefinition repositoryNameColumn = new BasicColumnDefinition();
            repositoryNameColumn.setName( StringSerializer.get().toByteBuffer( REPOSITORY_NAME.toString() ) );
            repositoryNameColumn.setIndexName( REPOSITORY_NAME.toString() );
            repositoryNameColumn.setIndexType( ColumnIndexType.KEYS );
            repositoryNameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( repositoryNameColumn );

            BasicColumnDefinition namespaceIdColumn = new BasicColumnDefinition();
            namespaceIdColumn.setName( StringSerializer.get().toByteBuffer( NAMESPACE_ID.toString() ) );
            namespaceIdColumn.setIndexName( NAMESPACE_ID.toString() );
            namespaceIdColumn.setIndexType( ColumnIndexType.KEYS );
            namespaceIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( namespaceIdColumn );

            BasicColumnDefinition projectColumn = new BasicColumnDefinition();
            projectColumn.setName( StringSerializer.get().toByteBuffer( PROJECT.toString() ) );
            projectColumn.setIndexName( PROJECT.toString() );
            projectColumn.setIndexType( ColumnIndexType.KEYS );
            projectColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( projectColumn );

            BasicColumnDefinition projectVersionColumn = new BasicColumnDefinition();
            projectVersionColumn.setName( StringSerializer.get().toByteBuffer( PROJECT_VERSION.toString() ) );
            projectVersionColumn.setIndexName( PROJECT_VERSION.toString() );
            projectVersionColumn.setIndexType( ColumnIndexType.KEYS );
            projectVersionColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( projectVersionColumn );

            BasicColumnDefinition versionColumn = new BasicColumnDefinition();
            versionColumn.setName( StringSerializer.get().toByteBuffer( VERSION.toString() ) );
            versionColumn.setIndexName( VERSION.toString() );
            versionColumn.setIndexType( ColumnIndexType.KEYS );
            versionColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( versionColumn );

            BasicColumnDefinition whenGatheredColumn = new BasicColumnDefinition();
            whenGatheredColumn.setName( StringSerializer.get().toByteBuffer( WHEN_GATHERED.toString() ) );
            whenGatheredColumn.setIndexName( WHEN_GATHERED.toString() );
            whenGatheredColumn.setIndexType( ColumnIndexType.KEYS );
            whenGatheredColumn.setValidationClass( ComparatorType.LONGTYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( whenGatheredColumn );

            BasicColumnDefinition sha1Column = new BasicColumnDefinition();
            sha1Column.setName( StringSerializer.get().toByteBuffer( SHA1.toString() ) );
            sha1Column.setIndexName( SHA1.toString() );
            sha1Column.setIndexType( ColumnIndexType.KEYS );
            sha1Column.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( sha1Column );

            BasicColumnDefinition md5Column = new BasicColumnDefinition();
            md5Column.setName( StringSerializer.get().toByteBuffer( MD5.toString() ) );
            md5Column.setIndexName( MD5.toString() );
            md5Column.setIndexType( ColumnIndexType.KEYS );
            md5Column.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( md5Column );


        }

        // metadatafacetmodel table
        {
            final ColumnFamilyDefinition metadataFacetModel =
                HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                       getMetadataFacetFamilyName(), //
                                                       ComparatorType.UTF8TYPE );
            cfds.add( metadataFacetModel );

            // creating indexes for cql query

            BasicColumnDefinition facetIdColumn = new BasicColumnDefinition();
            facetIdColumn.setName( StringSerializer.get().toByteBuffer( FACET_ID.toString() ) );
            facetIdColumn.setIndexName( FACET_ID.toString() );
            facetIdColumn.setIndexType( ColumnIndexType.KEYS );
            facetIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            metadataFacetModel.addColumnDefinition( facetIdColumn );

            BasicColumnDefinition repositoryNameColumn = new BasicColumnDefinition();
            repositoryNameColumn.setName( StringSerializer.get().toByteBuffer( REPOSITORY_NAME.toString() ) );
            repositoryNameColumn.setIndexName( REPOSITORY_NAME.toString() );
            repositoryNameColumn.setIndexType( ColumnIndexType.KEYS );
            repositoryNameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            metadataFacetModel.addColumnDefinition( repositoryNameColumn );

            BasicColumnDefinition nameColumn = new BasicColumnDefinition();
            nameColumn.setName( StringSerializer.get().toByteBuffer( NAME.toString() ) );
            nameColumn.setIndexName( NAME.toString() );
            nameColumn.setIndexType( ColumnIndexType.KEYS );
            nameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            metadataFacetModel.addColumnDefinition( nameColumn );

            BasicColumnDefinition namespaceColumn = new BasicColumnDefinition();
            namespaceColumn.setName( StringSerializer.get().toByteBuffer( NAMESPACE_ID.toString() ) );
            namespaceColumn.setIndexName( NAMESPACE_ID.toString() );
            namespaceColumn.setIndexType( ColumnIndexType.KEYS );
            namespaceColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            metadataFacetModel.addColumnDefinition( namespaceColumn );

            BasicColumnDefinition projectIdColumn = new BasicColumnDefinition();
            projectIdColumn.setName( StringSerializer.get().toByteBuffer( PROJECT_ID.toString() ) );
            projectIdColumn.setIndexName( PROJECT_ID.toString() );
            projectIdColumn.setIndexType( ColumnIndexType.KEYS );
            projectIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            metadataFacetModel.addColumnDefinition( projectIdColumn );

            BasicColumnDefinition projectVersionColumn = new BasicColumnDefinition();
            projectVersionColumn.setName( StringSerializer.get().toByteBuffer( PROJECT_VERSION.toString() ) );
            projectVersionColumn.setIndexName( PROJECT_VERSION.toString() );
            projectVersionColumn.setIndexType( ColumnIndexType.KEYS );
            projectVersionColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            metadataFacetModel.addColumnDefinition( projectVersionColumn );

        }

        // Checksum table
        {
            final ColumnFamilyDefinition checksumCf =
                    HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                            getChecksumFamilyName(), //
                            ComparatorType.UTF8TYPE );

            BasicColumnDefinition artifactMetatadaModel_key = new BasicColumnDefinition();
            artifactMetatadaModel_key.setName( StringSerializer.get().toByteBuffer( "artifactMetadataModel.key" ) );
            artifactMetatadaModel_key.setIndexName( "artifactMetadataModel_key" );
            artifactMetatadaModel_key.setIndexType( ColumnIndexType.KEYS );
            artifactMetatadaModel_key.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            checksumCf.addColumnDefinition( artifactMetatadaModel_key );


            BasicColumnDefinition checksumAlgorithmColumn = new BasicColumnDefinition();
            checksumAlgorithmColumn.setName( StringSerializer.get().toByteBuffer( CHECKSUM_ALG.toString() ) );
            checksumAlgorithmColumn.setIndexName( CHECKSUM_ALG.toString() );
            checksumAlgorithmColumn.setIndexType( ColumnIndexType.KEYS );
            checksumAlgorithmColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            checksumCf.addColumnDefinition( checksumAlgorithmColumn );

            BasicColumnDefinition checksumValueColumn = new BasicColumnDefinition();
            checksumValueColumn.setName( StringSerializer.get().toByteBuffer( CHECKSUM_VALUE.toString() ) );
            checksumValueColumn.setIndexName( CHECKSUM_VALUE.toString() );
            checksumValueColumn.setIndexType( ColumnIndexType.KEYS );
            checksumValueColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            checksumCf.addColumnDefinition( checksumValueColumn );

            BasicColumnDefinition repositoryNameColumn = new BasicColumnDefinition();
            repositoryNameColumn.setName( StringSerializer.get().toByteBuffer( REPOSITORY_NAME.toString() ) );
            repositoryNameColumn.setIndexName( REPOSITORY_NAME.toString() );
            repositoryNameColumn.setIndexType( ColumnIndexType.KEYS );
            repositoryNameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            checksumCf.addColumnDefinition( repositoryNameColumn );


            cfds.add( checksumCf );

            // creating indexes for cql query

        }

        // mailinglist table
        {
            final ColumnFamilyDefinition mailingListCf =
                HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                       getMailingListFamilyName(), //
                                                       ComparatorType.UTF8TYPE );

            BasicColumnDefinition projectVersionMetadataModel_key = new BasicColumnDefinition();
            projectVersionMetadataModel_key.setName( StringSerializer.get().toByteBuffer( "projectVersionMetadataModel.key" ) );
            projectVersionMetadataModel_key.setIndexName( "projectVersionMetadataModel_key" );
            projectVersionMetadataModel_key.setIndexType( ColumnIndexType.KEYS );
            projectVersionMetadataModel_key.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            mailingListCf.addColumnDefinition( projectVersionMetadataModel_key );

            cfds.add( mailingListCf );

            // creating indexes for cql query

        }

        // license table
        {
            final ColumnFamilyDefinition licenseCf =
                HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                       getLicenseFamilyName(), //
                                                       ComparatorType.UTF8TYPE );

            BasicColumnDefinition projectVersionMetadataModel_key = new BasicColumnDefinition();
            projectVersionMetadataModel_key.setName( StringSerializer.get().toByteBuffer( "projectVersionMetadataModel.key" ) );
            projectVersionMetadataModel_key.setIndexName( "projectVersionMetadataModel_key" );
            projectVersionMetadataModel_key.setIndexType( ColumnIndexType.KEYS );
            projectVersionMetadataModel_key.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            licenseCf.addColumnDefinition( projectVersionMetadataModel_key );

            cfds.add( licenseCf );

            // creating indexes for cql query

        }

        // dependency table
        {
            final ColumnFamilyDefinition dependencyCf =
                HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                       getDependencyFamilyName(), //
                                                       ComparatorType.UTF8TYPE );
            cfds.add( dependencyCf );

            // creating indexes for cql query

            BasicColumnDefinition groupIdColumn = new BasicColumnDefinition();
            groupIdColumn.setName( StringSerializer.get().toByteBuffer( GROUP_ID.toString() ) );
            groupIdColumn.setIndexName( "groupIdIdx" );
            groupIdColumn.setIndexType( ColumnIndexType.KEYS );
            groupIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            dependencyCf.addColumnDefinition( groupIdColumn );

            BasicColumnDefinition projectVersionMetadataModel_key = new BasicColumnDefinition();
            projectVersionMetadataModel_key.setName( StringSerializer.get().toByteBuffer( "projectVersionMetadataModel.key" ) );
            projectVersionMetadataModel_key.setIndexName( "projectVersionMetadataModel_key" );
            projectVersionMetadataModel_key.setIndexType( ColumnIndexType.KEYS );
            projectVersionMetadataModel_key.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            dependencyCf.addColumnDefinition( projectVersionMetadataModel_key );

        }

        // TODO take care of update new table!!
        { // ensure keyspace exists, here if the keyspace doesn't exist we suppose nothing exist
            if ( cluster.describeKeyspace( keyspaceName ) == null )
            {
                logger.info( "Creating Archiva Cassandra '{}' keyspace.", keyspaceName );
                cluster.addKeyspace( HFactory.createKeyspaceDefinition( keyspaceName, //
                                                                        ThriftKsDef.DEF_STRATEGY_CLASS, //
                                                                        replicationFactor, //
                                                                        cfds )
                );
            }
        }

    }

    @Override
    public void start()
    {
    }

    @PreDestroy
    @Override
    public void shutdown()
    {
    }


    @Override
    public boolean started()
    {
        return started;
    }


    @Override
    public Keyspace getKeyspace()
    {
        return keyspace;
    }

    @Override
    public Cluster getCluster()
    {
        return cluster;
    }

    @Override
    public String getRepositoryFamilyName()
    {
        return repositoryFamilyName;
    }

    @Override
    public String getNamespaceFamilyName()
    {
        return namespaceFamilyName;
    }

    @Override
    public String getProjectFamilyName()
    {
        return projectFamilyName;
    }

    @Override
    public String getProjectVersionMetadataFamilyName()
    {
        return projectVersionMetadataFamilyName;
    }

    @Override
    public String getArtifactMetadataFamilyName()
    {
        return artifactMetadataFamilyName;
    }

    @Override
    public String getMetadataFacetFamilyName()
    {
        return metadataFacetFamilyName;
    }

    @Override
    public String getMailingListFamilyName()
    {
        return mailingListFamilyName;
    }

    @Override
    public String getLicenseFamilyName()
    {
        return licenseFamilyName;
    }

    @Override
    public String getDependencyFamilyName()
    {
        return dependencyFamilyName;
    }

    @Override
    public String getChecksumFamilyName() {
        return checksumFamilyName;
    }
}
