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

            final ColumnFamilyDefinition namespaces =
                HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                       getNamespaceFamilyName(), //
                                                       ComparatorType.UTF8TYPE );
            cfds.add( namespaces );

            // creating indexes for cql query

            BasicColumnDefinition nameColumn = new BasicColumnDefinition();
            nameColumn.setName( StringSerializer.get().toByteBuffer( "name" ) );
            nameColumn.setIndexName( "name" );
            nameColumn.setIndexType( ColumnIndexType.KEYS );
            nameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            namespaces.addColumnDefinition( nameColumn );

            BasicColumnDefinition repositoryIdColumn = new BasicColumnDefinition();
            repositoryIdColumn.setName( StringSerializer.get().toByteBuffer( "repositoryId" ) );
            repositoryIdColumn.setIndexName( "repositoryId" );
            repositoryIdColumn.setIndexType( ColumnIndexType.KEYS );
            repositoryIdColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            namespaces.addColumnDefinition( repositoryIdColumn );
        }

        {
            final ColumnFamilyDefinition repository =
                HFactory.createColumnFamilyDefinition( keyspace.getKeyspaceName(), //
                                                       getRepositoryFamilyName(), //
                                                       ComparatorType.UTF8TYPE );

            cfds.add( repository );

            BasicColumnDefinition nameColumn = new BasicColumnDefinition();
            nameColumn.setName( StringSerializer.get().toByteBuffer( "name" ) );
            nameColumn.setIndexName( "name" );
            nameColumn.setIndexType( ColumnIndexType.KEYS );
            nameColumn.setValidationClass( ComparatorType.UTF8TYPE.getClassName() );
            repository.addColumnDefinition( nameColumn );
        }

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
}
