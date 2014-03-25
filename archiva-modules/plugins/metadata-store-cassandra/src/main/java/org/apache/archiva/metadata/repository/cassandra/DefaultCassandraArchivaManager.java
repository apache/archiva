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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

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

    private String projectFamilyName = "project";

    private String projectVersionMetadataFamilyName = "projectversionmetadata";

    private String artifactMetadataFamilyName = "artifactmetadata";

    private String metadataFacetFamilyName = "metadatafacet";

    private String mailingListFamilyName = "mailinglist";


    @PostConstruct
    public void initialize()
    {
        // FIXME must come from configuration not sys props
        String cassandraHost = System.getProperty( "cassandraHost", "localhost" );
        String cassandraPort = System.getProperty( "cassandraPort" );
        int maxActive = Integer.getInteger( "cassandra.maxActive", 20 );
        String readConsistencyLevel =
            System.getProperty( "cassandra.readConsistencyLevel", HConsistencyLevel.QUORUM.name() );
        String writeConsistencyLevel =
            System.getProperty( "cassandra.readConsistencyLevel", HConsistencyLevel.QUORUM.name() );

        int replicationFactor = Integer.getInteger( "cassandra.replicationFactor", 1 );

        String keyspaceName = System.getProperty( "cassandra.keyspace.name", KEYSPACE_NAME );
        String clusterName = System.getProperty( "cassandra.cluster.name", CLUSTER_NAME );

        final CassandraHostConfigurator configurator =
            new CassandraHostConfigurator( cassandraHost + ":" + cassandraPort );
        configurator.setMaxActive( maxActive );

        cluster = HFactory.getOrCreateCluster( clusterName, configurator );

        final ConfigurableConsistencyLevel consistencyLevelPolicy = new ConfigurableConsistencyLevel();
        consistencyLevelPolicy.setDefaultReadConsistencyLevel( HConsistencyLevel.valueOf( readConsistencyLevel ) );
        consistencyLevelPolicy.setDefaultWriteConsistencyLevel( HConsistencyLevel.valueOf( writeConsistencyLevel ) );
        keyspace = HFactory.createKeyspace( keyspaceName, cluster, consistencyLevelPolicy );

        List<ColumnFamilyDefinition> cfds = new ArrayList<ColumnFamilyDefinition>();

        // namespace table
        {

            final ColumnFamilyDefinition namespace =
                HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                       getNamespaceFamilyName(), //
                                                       ComparatorType.UTF8TYPE );
            cfds.add( namespace );

            // creating indexes for cql query

            BasicColumnDefinition nameColumn = new BasicColumnDefinition();
            nameColumn.setName( StringSerializer.get().toByteBuffer( "name" ) );
            nameColumn.setIndexName( "name" );
            nameColumn.setIndexType( ColumnIndexType.KEYS );
            nameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            namespace.addColumnDefinition( nameColumn );

            BasicColumnDefinition repositoryIdColumn = new BasicColumnDefinition();
            repositoryIdColumn.setName( StringSerializer.get().toByteBuffer( "repositoryName" ) );
            repositoryIdColumn.setIndexName( "repositoryName" );
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
            nameColumn.setName( StringSerializer.get().toByteBuffer( "repositoryName" ) );
            nameColumn.setIndexName( "repositoryName" );
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
            projectIdColumn.setName( StringSerializer.get().toByteBuffer( "projectId" ) );
            projectIdColumn.setIndexName( "projectId" );
            projectIdColumn.setIndexType( ColumnIndexType.KEYS );
            projectIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            project.addColumnDefinition( projectIdColumn );

            BasicColumnDefinition repositoryIdColumn = new BasicColumnDefinition();
            repositoryIdColumn.setName( StringSerializer.get().toByteBuffer( "repositoryName" ) );
            repositoryIdColumn.setIndexName( "repositoryName" );
            repositoryIdColumn.setIndexType( ColumnIndexType.KEYS );
            repositoryIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            project.addColumnDefinition( repositoryIdColumn );

            BasicColumnDefinition namespaceIdColumn = new BasicColumnDefinition();
            namespaceIdColumn.setName( StringSerializer.get().toByteBuffer( "namespaceId" ) );
            namespaceIdColumn.setIndexName( "namespaceId" );
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
            namespaceIdColumn.setName( StringSerializer.get().toByteBuffer( "namespaceId" ) );
            namespaceIdColumn.setIndexName( "namespaceId" );
            namespaceIdColumn.setIndexType( ColumnIndexType.KEYS );
            namespaceIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            projectVersionMetadataModel.addColumnDefinition( namespaceIdColumn );

            BasicColumnDefinition repositoryNameColumn = new BasicColumnDefinition();
            repositoryNameColumn.setName( StringSerializer.get().toByteBuffer( "repositoryName" ) );
            repositoryNameColumn.setIndexName( "repositoryName" );
            repositoryNameColumn.setIndexType( ColumnIndexType.KEYS );
            repositoryNameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            projectVersionMetadataModel.addColumnDefinition( repositoryNameColumn );

            BasicColumnDefinition idColumn = new BasicColumnDefinition();
            idColumn.setName( StringSerializer.get().toByteBuffer( "id" ) );
            idColumn.setIndexName( "id" );
            idColumn.setIndexType( ColumnIndexType.KEYS );
            idColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            projectVersionMetadataModel.addColumnDefinition( idColumn );

            BasicColumnDefinition projectIdColumn = new BasicColumnDefinition();
            projectIdColumn.setName( StringSerializer.get().toByteBuffer( "projectId" ) );
            projectIdColumn.setIndexName( "projectId" );
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
            idColumn.setName( StringSerializer.get().toByteBuffer( "id" ) );
            idColumn.setIndexName( "id" );
            idColumn.setIndexType( ColumnIndexType.KEYS );
            idColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( idColumn );

            BasicColumnDefinition repositoryNameColumn = new BasicColumnDefinition();
            repositoryNameColumn.setName( StringSerializer.get().toByteBuffer( "repositoryName" ) );
            repositoryNameColumn.setIndexName( "repositoryName" );
            repositoryNameColumn.setIndexType( ColumnIndexType.KEYS );
            repositoryNameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( repositoryNameColumn );

            BasicColumnDefinition namespaceIdColumn = new BasicColumnDefinition();
            namespaceIdColumn.setName( StringSerializer.get().toByteBuffer( "namespaceId" ) );
            namespaceIdColumn.setIndexName( "namespaceId" );
            namespaceIdColumn.setIndexType( ColumnIndexType.KEYS );
            namespaceIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( namespaceIdColumn );

            BasicColumnDefinition projectColumn = new BasicColumnDefinition();
            projectColumn.setName( StringSerializer.get().toByteBuffer( "project" ) );
            projectColumn.setIndexName( "project" );
            projectColumn.setIndexType( ColumnIndexType.KEYS );
            projectColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( projectColumn );

            BasicColumnDefinition projectVersionColumn = new BasicColumnDefinition();
            projectVersionColumn.setName( StringSerializer.get().toByteBuffer( "projectVersion" ) );
            projectVersionColumn.setIndexName( "projectVersion" );
            projectVersionColumn.setIndexType( ColumnIndexType.KEYS );
            projectVersionColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( projectVersionColumn );

            BasicColumnDefinition versionColumn = new BasicColumnDefinition();
            versionColumn.setName( StringSerializer.get().toByteBuffer( "version" ) );
            versionColumn.setIndexName( "version" );
            versionColumn.setIndexType( ColumnIndexType.KEYS );
            versionColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( versionColumn );

            BasicColumnDefinition whenGatheredColumn = new BasicColumnDefinition();
            whenGatheredColumn.setName( StringSerializer.get().toByteBuffer( "whenGathered" ) );
            whenGatheredColumn.setIndexName( "whenGathered" );
            whenGatheredColumn.setIndexType( ColumnIndexType.KEYS );
            whenGatheredColumn.setValidationClass( ComparatorType.LONGTYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( whenGatheredColumn );

            BasicColumnDefinition sha1Column = new BasicColumnDefinition();
            sha1Column.setName( StringSerializer.get().toByteBuffer( "sha1" ) );
            sha1Column.setIndexName( "sha1" );
            sha1Column.setIndexType( ColumnIndexType.KEYS );
            sha1Column.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            artifactMetadataModel.addColumnDefinition( sha1Column );

            BasicColumnDefinition md5Column = new BasicColumnDefinition();
            md5Column.setName( StringSerializer.get().toByteBuffer( "md5" ) );
            md5Column.setIndexName( "md5" );
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
            facetIdColumn.setName( StringSerializer.get().toByteBuffer( "facetId" ) );
            facetIdColumn.setIndexName( "facetId" );
            facetIdColumn.setIndexType( ColumnIndexType.KEYS );
            facetIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            metadataFacetModel.addColumnDefinition( facetIdColumn );

            BasicColumnDefinition repositoryNameColumn = new BasicColumnDefinition();
            repositoryNameColumn.setName( StringSerializer.get().toByteBuffer( "repositoryName" ) );
            repositoryNameColumn.setIndexName( "repositoryName" );
            repositoryNameColumn.setIndexType( ColumnIndexType.KEYS );
            repositoryNameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            metadataFacetModel.addColumnDefinition( repositoryNameColumn );

            BasicColumnDefinition nameColumn = new BasicColumnDefinition();
            nameColumn.setName( StringSerializer.get().toByteBuffer( "name" ) );
            nameColumn.setIndexName( "name" );
            nameColumn.setIndexType( ColumnIndexType.KEYS );
            nameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            metadataFacetModel.addColumnDefinition( nameColumn );

            BasicColumnDefinition namespaceColumn = new BasicColumnDefinition();
            namespaceColumn.setName( StringSerializer.get().toByteBuffer( "namespaceId" ) );
            namespaceColumn.setIndexName( "namespaceId" );
            namespaceColumn.setIndexType( ColumnIndexType.KEYS );
            namespaceColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            metadataFacetModel.addColumnDefinition( namespaceColumn );

            BasicColumnDefinition projectIdColumn = new BasicColumnDefinition();
            projectIdColumn.setName( StringSerializer.get().toByteBuffer( "projectId" ) );
            projectIdColumn.setIndexName( "projectId" );
            projectIdColumn.setIndexType( ColumnIndexType.KEYS );
            projectIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            metadataFacetModel.addColumnDefinition( projectIdColumn );

            BasicColumnDefinition projectVersionColumn = new BasicColumnDefinition();
            projectVersionColumn.setName( StringSerializer.get().toByteBuffer( "projectVersion" ) );
            projectVersionColumn.setIndexName( "projectVersion" );
            projectVersionColumn.setIndexType( ColumnIndexType.KEYS );
            projectVersionColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            metadataFacetModel.addColumnDefinition( projectVersionColumn );

        }

        // mailinglist table
        {
            final ColumnFamilyDefinition mailingListCf =
                HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                       getMailingListFamilyName(), //
                                                       ComparatorType.UTF8TYPE );
            cfds.add( mailingListCf );

            // creating indexes for cql query

            BasicColumnDefinition projectVersionMetadataIdColumn = new BasicColumnDefinition();
            projectVersionMetadataIdColumn.setName( StringSerializer.get().toByteBuffer( "projectVersionMetadataId" ) );
            projectVersionMetadataIdColumn.setIndexName( "projectVersionMetadataId" );
            projectVersionMetadataIdColumn.setIndexType( ColumnIndexType.KEYS );
            projectVersionMetadataIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            mailingListCf.addColumnDefinition( projectVersionMetadataIdColumn );


        }

        // TODO take care of update new table!!
        { // ensure keyspace exists, here if the keyspace doesn't exist we suppose nothing exist
            if ( cluster.describeKeyspace( keyspaceName ) == null )
            {
                logger.info( "Creating Archiva Cassandra '" + keyspaceName + "' keyspace." );
                cluster.addKeyspace( HFactory.createKeyspaceDefinition( keyspaceName, //
                                                                        ThriftKsDef.DEF_STRATEGY_CLASS, //
                                                                        replicationFactor, //
                                                                        cfds )
                );
            }
        }

    }

    public void start()
    {
    }

    @PreDestroy
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

    public Cluster getCluster()
    {
        return cluster;
    }

    public String getRepositoryFamilyName()
    {
        return repositoryFamilyName;
    }

    public String getNamespaceFamilyName()
    {
        return namespaceFamilyName;
    }

    public String getProjectFamilyName()
    {
        return projectFamilyName;
    }

    public String getProjectVersionMetadataFamilyName()
    {
        return projectVersionMetadataFamilyName;
    }

    public String getArtifactMetadataFamilyName()
    {
        return artifactMetadataFamilyName;
    }

    public String getMetadataFacetFamilyName()
    {
        return metadataFacetFamilyName;
    }

    public String getMailingListFamilyName()
    {
        return mailingListFamilyName;
    }
}
