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

import com.google.common.collect.ImmutableMap;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolType;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.connectionpool.impl.Slf4jConnectionPoolMonitorImpl;
import com.netflix.astyanax.ddl.KeyspaceDefinition;
import com.netflix.astyanax.entitystore.DefaultEntityManager;
import com.netflix.astyanax.entitystore.EntityManager;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;
import org.apache.archiva.metadata.repository.cassandra.model.ArtifactMetadataModel;
import org.apache.archiva.metadata.repository.cassandra.model.MetadataFacetModel;
import org.apache.archiva.metadata.repository.cassandra.model.Namespace;
import org.apache.archiva.metadata.repository.cassandra.model.Project;
import org.apache.archiva.metadata.repository.cassandra.model.ProjectVersionMetadataModel;
import org.apache.archiva.metadata.repository.cassandra.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import java.util.Properties;

/**
 * FIXME make all configuration not hardcoded :-)
 *
 * @author Olivier Lamy
 */
@Service( "archivaEntityManagerFactory#cassandra" )
public class DefaultCassandraEntityManagerFactory
    implements CassandraEntityManagerFactory
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ApplicationContext applicationContext;

    private static final String CLUSTER_NAME = "archiva";

    private static final String KEYSPACE_NAME = "ArchivaKeySpace";

    private AstyanaxContext<Keyspace> keyspaceContext;

    private Keyspace keyspace;

    private EntityManager<Repository, String> repositoryEntityManager;

    private EntityManager<Namespace, String> namespaceEntityManager;

    private EntityManager<Project, String> projectEntityManager;

    private EntityManager<ArtifactMetadataModel, String> artifactMetadataModelEntityManager;

    private EntityManager<MetadataFacetModel, String> metadataFacetModelEntityManager;

    private EntityManager<ProjectVersionMetadataModel, String> projectVersionMetadataModelEntityManager;


    @PostConstruct
    public void initialize()
        throws ConnectionException
    {
        String cassandraHost = System.getProperty( "cassandraHost", "localhost" );
        String cassandraPort = System.getProperty( "cassandraPort" );
        keyspaceContext = new AstyanaxContext.Builder().forCluster( CLUSTER_NAME ).forKeyspace(
            KEYSPACE_NAME ).withAstyanaxConfiguration(
            new AstyanaxConfigurationImpl().setDiscoveryType( NodeDiscoveryType.RING_DESCRIBE ).setConnectionPoolType(
                ConnectionPoolType.TOKEN_AWARE ) ).withConnectionPoolConfiguration(
            new ConnectionPoolConfigurationImpl( CLUSTER_NAME + "_" + KEYSPACE_NAME ).setSocketTimeout(
                30000 ).setMaxTimeoutWhenExhausted( 2000 ).setMaxConnsPerHost( 20 ).setInitConnsPerHost( 10 ).setSeeds(
                cassandraHost + ":" + cassandraPort ) ).withConnectionPoolMonitor(
            new Slf4jConnectionPoolMonitorImpl() ).buildKeyspace( ThriftFamilyFactory.getInstance() );

        keyspaceContext.start();

        keyspace = keyspaceContext.getClient();

        ImmutableMap<String, Object> options = ImmutableMap.<String, Object>builder().put( "strategy_options",
                                                                                           ImmutableMap.<String, Object>builder().put(
                                                                                               "replication_factor",
                                                                                               "1" ).build() ).put(
            "strategy_class", "SimpleStrategy" ).build();

        // test if the namespace already exists if exception or null create it
        boolean keyspaceExists = false;
        try
        {
            KeyspaceDefinition keyspaceDefinition = keyspace.describeKeyspace();
            if ( keyspaceDefinition != null )
            {
                keyspaceExists = true;
            }

        }
        catch ( ConnectionException e )
        {
        }

        if ( !keyspaceExists )
        {
            keyspace.createKeyspace( options );
        }

        try
        {
            Properties properties = keyspace.getKeyspaceProperties();
            logger.info( "keyspace properties: {}", properties );
        }
        catch ( ConnectionException e )
        {
            // FIXME better logging !
            logger.warn( e.getMessage(), e );
        }

        try
        {
            repositoryEntityManager =
                new DefaultEntityManager.Builder<Repository, String>().withEntityType( Repository.class ).withKeyspace(
                    keyspace ).build();
            boolean exists = columnFamilyExists( "repository" );
            // TODO very basic test we must test model change too
            if ( !exists )
            {
                repositoryEntityManager.createStorage( null );
            }

            namespaceEntityManager =
                new DefaultEntityManager.Builder<Namespace, String>().withEntityType( Namespace.class ).withKeyspace(
                    keyspace ).build();

            exists = columnFamilyExists( "namespace" );
            if ( !exists )
            {
                namespaceEntityManager.createStorage( null );
            }

            projectEntityManager =
                new DefaultEntityManager.Builder<Project, String>().withEntityType( Project.class ).withKeyspace(
                    keyspace ).build();

            exists = columnFamilyExists( "project" );
            if ( !exists )
            {
                projectEntityManager.createStorage( null );
            }

            artifactMetadataModelEntityManager =
                new DefaultEntityManager.Builder<ArtifactMetadataModel, String>().withEntityType(
                    ArtifactMetadataModel.class ).withKeyspace( keyspace ).build();

            exists = columnFamilyExists( "artifactmetadatamodel" );
            if ( !exists )
            {
                artifactMetadataModelEntityManager.createStorage( null );
            }

            metadataFacetModelEntityManager =
                new DefaultEntityManager.Builder<MetadataFacetModel, String>().withEntityType(
                    MetadataFacetModel.class ).withKeyspace( keyspace ).build();

            exists = columnFamilyExists( "metadatafacetmodel" );
            if ( !exists )
            {
                metadataFacetModelEntityManager.createStorage( null );
            }

            projectVersionMetadataModelEntityManager =
                new DefaultEntityManager.Builder<ProjectVersionMetadataModel, String>().withEntityType(
                    ProjectVersionMetadataModel.class ).withKeyspace( keyspace ).build();

            exists = columnFamilyExists( "projectversionmetadatamodel" );
            if ( !exists )
            {
                projectVersionMetadataModelEntityManager.createStorage( null );
            }

        }
        catch ( PersistenceException e )
        {
            // FIXME report exception
            logger.error( e.getMessage(), e );
        }
        catch ( ConnectionException e )
        {
            // FIXME report exception
            logger.error( e.getMessage(), e );
        }
    }

    private boolean columnFamilyExists( String columnFamilyName )
        throws ConnectionException
    {
        try
        {
            Properties properties = keyspace.getColumnFamilyProperties( columnFamilyName );
            logger.debug( "getColumnFamilyProperties for {}: {}", columnFamilyName, properties );
            return true;
        }
        catch ( NotFoundException e )
        {
            return false;
        }
    }


    @Override
    public Keyspace getKeyspace()
    {
        return keyspace;
    }

    public EntityManager<Repository, String> getRepositoryEntityManager()
    {
        return repositoryEntityManager;
    }

    public void setRepositoryEntityManager( EntityManager<Repository, String> repositoryEntityManager )
    {
        this.repositoryEntityManager = repositoryEntityManager;
    }

    public EntityManager<Namespace, String> getNamespaceEntityManager()
    {
        return namespaceEntityManager;
    }

    public void setNamespaceEntityManager( EntityManager<Namespace, String> namespaceEntityManager )
    {
        this.namespaceEntityManager = namespaceEntityManager;
    }

    public EntityManager<Project, String> getProjectEntityManager()
    {
        return projectEntityManager;
    }

    public void setProjectEntityManager( EntityManager<Project, String> projectEntityManager )
    {
        this.projectEntityManager = projectEntityManager;
    }

    public EntityManager<ArtifactMetadataModel, String> getArtifactMetadataModelEntityManager()
    {
        return artifactMetadataModelEntityManager;
    }

    public void setArtifactMetadataModelEntityManager(
        EntityManager<ArtifactMetadataModel, String> artifactMetadataModelEntityManager )
    {
        this.artifactMetadataModelEntityManager = artifactMetadataModelEntityManager;
    }

    public EntityManager<MetadataFacetModel, String> getMetadataFacetModelEntityManager()
    {
        return metadataFacetModelEntityManager;
    }

    public void setMetadataFacetModelEntityManager(
        EntityManager<MetadataFacetModel, String> metadataFacetModelEntityManager )
    {
        this.metadataFacetModelEntityManager = metadataFacetModelEntityManager;
    }

    public EntityManager<ProjectVersionMetadataModel, String> getProjectVersionMetadataModelEntityManager()
    {
        return projectVersionMetadataModelEntityManager;
    }

    public void setProjectVersionMetadataModelEntityManager(
        EntityManager<ProjectVersionMetadataModel, String> projectVersionMetadataModelEntityManager )
    {
        this.projectVersionMetadataModelEntityManager = projectVersionMetadataModelEntityManager;
    }
}
