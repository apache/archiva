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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.template.ColumnFamilyResult;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ColumnFamilyUpdater;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.metadata.QueryParameter;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.CiManagement;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.FacetedMetadata;
import org.apache.archiva.metadata.model.IssueManagement;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.model.Organization;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.model.Scm;
import org.apache.archiva.metadata.repository.AbstractMetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.MetadataService;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.cassandra.model.ArtifactMetadataModel;
import org.apache.archiva.metadata.repository.cassandra.model.MetadataFacetModel;
import org.apache.archiva.metadata.repository.cassandra.model.Namespace;
import org.apache.archiva.metadata.repository.cassandra.model.Project;
import org.apache.archiva.metadata.repository.cassandra.model.ProjectVersionMetadataModel;
import org.apache.archiva.metadata.repository.cassandra.model.Repository;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.apache.archiva.metadata.model.ModelInfo.STORAGE_TZ;
import static org.apache.archiva.metadata.repository.cassandra.CassandraUtils.*;
import static org.apache.archiva.metadata.repository.cassandra.model.ColumnNames.*;

/**
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class CassandraMetadataRepository
    extends AbstractMetadataRepository implements MetadataRepository
{

    private static final String ARTIFACT_METADATA_MODEL_KEY = "artifactMetadataModel.key";
    private Logger logger = LoggerFactory.getLogger( getClass() );

    private ArchivaConfiguration configuration;

    private final CassandraArchivaManager cassandraArchivaManager;

    private final ColumnFamilyTemplate<String, String> projectVersionMetadataTemplate;

    private final ColumnFamilyTemplate<String, String> projectTemplate;

    private final ColumnFamilyTemplate<String, String> artifactMetadataTemplate;

    private final ColumnFamilyTemplate<String, String> metadataFacetTemplate;

    private final ColumnFamilyTemplate<String, String> mailingListTemplate;

    private final ColumnFamilyTemplate<String, String> licenseTemplate;

    private final ColumnFamilyTemplate<String, String> dependencyTemplate;

    private final ColumnFamilyTemplate<String, String> checksumTemplate;

    private final Keyspace keyspace;

    private final StringSerializer ss = StringSerializer.get();

    public CassandraMetadataRepository( MetadataService metadataService,
                                        ArchivaConfiguration configuration,
                                        CassandraArchivaManager cassandraArchivaManager )
    {
        super( metadataService );
        this.configuration = configuration;
        this.cassandraArchivaManager = cassandraArchivaManager;
        this.keyspace = cassandraArchivaManager.getKeyspace();

        this.projectVersionMetadataTemplate =
            new ThriftColumnFamilyTemplate<>( cassandraArchivaManager.getKeyspace(), //
                                              cassandraArchivaManager.getProjectVersionMetadataFamilyName(), //
                                              StringSerializer.get(), //
                                              StringSerializer.get() );

        this.projectTemplate = new ThriftColumnFamilyTemplate<>( cassandraArchivaManager.getKeyspace(), //
                                                                 cassandraArchivaManager.getProjectFamilyName(), //
                                                                 //
                                                                 StringSerializer.get(), //
                                                                 StringSerializer.get() );

        this.artifactMetadataTemplate = new ThriftColumnFamilyTemplate<>( cassandraArchivaManager.getKeyspace(), //
                                                                          cassandraArchivaManager.getArtifactMetadataFamilyName(),
                                                                          StringSerializer.get(), //
                                                                          StringSerializer.get() );

        this.metadataFacetTemplate = new ThriftColumnFamilyTemplate<>( cassandraArchivaManager.getKeyspace(), //
                                                                       cassandraArchivaManager.getMetadataFacetFamilyName(),
                                                                       //
                                                                       StringSerializer.get(), //
                                                                       StringSerializer.get() );

        this.mailingListTemplate = new ThriftColumnFamilyTemplate<>( cassandraArchivaManager.getKeyspace(), //
                                                                     cassandraArchivaManager.getMailingListFamilyName(),
                                                                     //
                                                                     StringSerializer.get(), //
                                                                     StringSerializer.get() );

        this.licenseTemplate = new ThriftColumnFamilyTemplate<>( cassandraArchivaManager.getKeyspace(), //
                                                                 cassandraArchivaManager.getLicenseFamilyName(),
                                                                 //
                                                                 StringSerializer.get(), //
                                                                 StringSerializer.get() );

        this.dependencyTemplate = new ThriftColumnFamilyTemplate<>( cassandraArchivaManager.getKeyspace(), //
                                                                    cassandraArchivaManager.getDependencyFamilyName(),
                                                                    //
                                                                    StringSerializer.get(), //
                                                                    StringSerializer.get() );

        this.checksumTemplate = new ThriftColumnFamilyTemplate<>( cassandraArchivaManager.getKeyspace(), //
                cassandraArchivaManager.getChecksumFamilyName(),
                //
                StringSerializer.get(), //
                StringSerializer.get() );
    }


    /**
     * if the repository doesn't exist it will be created
     *
     * @param repositoryId
     * @return
     */
    public Repository getOrCreateRepository( String repositoryId )
        throws MetadataRepositoryException
    {
        String cf = cassandraArchivaManager.getRepositoryFamilyName();

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, StringSerializer.get(), StringSerializer.get(),
                                     StringSerializer.get() ) //
            .setColumnFamily( cf ) //
            .setColumnNames( REPOSITORY_NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .execute();

        if ( result.get().getCount() < 1 )
        {
            // we need to create the repository
            Repository repository = new Repository( repositoryId );

            try
            {
                MutationResult mutationResult = HFactory.createMutator( keyspace, StringSerializer.get() ) //
                    .addInsertion( repositoryId, cf,
                                   CassandraUtils.column( REPOSITORY_NAME.toString(), repository.getName() ) ) //
                    .execute();
                logger.debug( "time to insert repository: {}", mutationResult.getExecutionTimeMicro() );
                return repository;
            }
            catch ( HInvalidRequestException e )
            {
                logger.error( e.getMessage(), e );
                throw new MetadataRepositoryException( e.getMessage(), e );
            }

        }

        return new Repository(
            result.get().getList().get( 0 ).getColumnSlice().getColumnByName( REPOSITORY_NAME.toString() ).getValue() );
    }


    protected Repository getRepository( String repositoryId )
        throws MetadataRepositoryException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, StringSerializer.get(), StringSerializer.get(),
                                     StringSerializer.get() ) //
            .setColumnFamily( cassandraArchivaManager.getRepositoryFamilyName() ) //
            .setColumnNames( REPOSITORY_NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .execute();
        return ( result.get().getCount() > 0 ) ? new Repository( repositoryId ) : null;
    }

    @Override
    public void updateNamespace( RepositorySession session, String repositoryId, String namespaceId )
        throws MetadataRepositoryException
    {
        updateOrAddNamespace( repositoryId, namespaceId );
    }

    private Namespace updateOrAddNamespace( String repositoryId, String namespaceId )
        throws MetadataRepositoryException
    {
        try
        {
            Repository repository = getOrCreateRepository( repositoryId );

            String key =
                new Namespace.KeyBuilder().withNamespace( namespaceId ).withRepositoryId( repositoryId ).build();

            Namespace namespace = getNamespace( repositoryId, namespaceId );
            if ( namespace == null )
            {
                String cf = cassandraArchivaManager.getNamespaceFamilyName();
                namespace = new Namespace( namespaceId, repository );
                HFactory.createMutator( keyspace, StringSerializer.get() )
                    //  values
                    .addInsertion( key, cf, CassandraUtils.column( NAME.toString(), namespace.getName() ) ) //
                    .addInsertion( key, cf, CassandraUtils.column( REPOSITORY_NAME.toString(), repository.getName() ) ) //
                    .execute();
            }

            return namespace;
        }
        catch ( HInvalidRequestException e )
        {
            logger.error( e.getMessage(), e );
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    protected Namespace getNamespace( String repositoryId, String namespaceId )
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getNamespaceFamilyName() ) //
            .setColumnNames( REPOSITORY_NAME.toString(), NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .addEqualsExpression( NAME.toString(), namespaceId ) //
            .execute();
        if ( result.get().getCount() > 0 )
        {
            ColumnSlice<String, String> columnSlice = result.get().getList().get( 0 ).getColumnSlice();
            return new Namespace( getStringValue( columnSlice, NAME.toString() ), //
                                  new Repository( getStringValue( columnSlice, REPOSITORY_NAME.toString() ) ) );

        }
        return null;
    }


    @Override
    public void removeNamespace( RepositorySession session, String repositoryId, String namespaceId )
        throws MetadataRepositoryException
    {

        try
        {
            String key = new Namespace.KeyBuilder() //
                .withNamespace( namespaceId ) //
                .withRepositoryId( repositoryId ) //
                .build();

            HFactory.createMutator( cassandraArchivaManager.getKeyspace(), new StringSerializer() ) //
                .addDeletion( key, cassandraArchivaManager.getNamespaceFamilyName() ) //
                .execute();

            QueryResult<OrderedRows<String, String, String>> result = HFactory //
                .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
                .setColumnFamily( cassandraArchivaManager.getProjectFamilyName() ) //
                .setColumnNames( REPOSITORY_NAME.toString() ) //
                .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
                .addEqualsExpression( NAMESPACE_ID.toString(), namespaceId ) //
                .execute();

            for ( Row<String, String, String> row : result.get() )
            {
                this.projectTemplate.deleteRow( row.getKey() );
            }

            result = HFactory //
                .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
                .setColumnFamily( cassandraArchivaManager.getProjectVersionMetadataFamilyName() ) //
                .setColumnNames( REPOSITORY_NAME.toString() ) //
                .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
                .addEqualsExpression( NAMESPACE_ID.toString(), namespaceId ) //
                .execute();

            for ( Row<String, String, String> row : result.get() )
            {
                this.projectVersionMetadataTemplate.deleteRow( row.getKey() );
                removeMailingList( row.getKey() );
            }

            result = HFactory //
                .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
                .setColumnFamily( cassandraArchivaManager.getArtifactMetadataFamilyName() ) //
                .setColumnNames( REPOSITORY_NAME.toString() ) //
                .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
                .addEqualsExpression( NAMESPACE_ID.toString(), namespaceId ) //
                .execute();

            for ( Row<String, String, String> row : result.get() )
            {
                this.artifactMetadataTemplate.deleteRow( row.getKey() );
            }

            result = HFactory //
                .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
                .setColumnFamily( cassandraArchivaManager.getMetadataFacetFamilyName() ) //
                .setColumnNames( REPOSITORY_NAME.toString() ) //
                .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
                .addEqualsExpression( NAMESPACE_ID.toString(), namespaceId ) //
                .execute();

            for ( Row<String, String, String> row : result.get() )
            {
                this.metadataFacetTemplate.deleteRow( row.getKey() );
            }

        }
        catch ( HInvalidRequestException e )
        {
            logger.error( e.getMessage(), e );
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }


    @Override
    public void removeRepository( RepositorySession session, final String repositoryId )
        throws MetadataRepositoryException
    {

        // TODO use cql queries to delete all
        List<String> namespacesKey = new ArrayList<>();

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getNamespaceFamilyName() ) //
            .setColumnNames( REPOSITORY_NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .execute();

        for ( Row<String, String, String> row : result.get().getList() )
        {
            namespacesKey.add( row.getKey() );
        }

        HFactory.createMutator( cassandraArchivaManager.getKeyspace(), ss ) //
            .addDeletion( namespacesKey, cassandraArchivaManager.getNamespaceFamilyName() ) //
            .execute();

        //delete repositoryId
        HFactory.createMutator( cassandraArchivaManager.getKeyspace(), ss ) //
            .addDeletion( repositoryId, cassandraArchivaManager.getRepositoryFamilyName() ) //
            .execute();

        result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectFamilyName() ) //
            .setColumnNames( REPOSITORY_NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .execute();

        for ( Row<String, String, String> row : result.get() )
        {
            this.projectTemplate.deleteRow( row.getKey() );
        }

        result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectVersionMetadataFamilyName() ) //
            .setColumnNames( REPOSITORY_NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .execute();

        for ( Row<String, String, String> row : result.get() )
        {
            this.projectVersionMetadataTemplate.deleteRow( row.getKey() );
            removeMailingList( row.getKey() );
        }

        result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getArtifactMetadataFamilyName() ) //
            .setColumnNames( REPOSITORY_NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .execute();

        for ( Row<String, String, String> row : result.get() )
        {
            this.artifactMetadataTemplate.deleteRow( row.getKey() );
        }

        result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getMetadataFacetFamilyName() ) //
            .setColumnNames( REPOSITORY_NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .execute();

        for ( Row<String, String, String> row : result.get() )
        {
            this.metadataFacetTemplate.deleteRow( row.getKey() );
        }


    }

    // FIXME this one need peformance improvement maybe a cache?
    @Override
    public List<String> getRootNamespaces( RepositorySession session, final String repoId )
        throws MetadataResolutionException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getNamespaceFamilyName() ) //
            .setColumnNames( NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .execute();

        Set<String> namespaces = new HashSet<String>( result.get().getCount() );

        for ( Row<String, String, String> row : result.get() )
        {
            namespaces.add( StringUtils.substringBefore( getStringValue( row.getColumnSlice(), NAME.toString() ), "." ) );
        }

        return new ArrayList<>( namespaces );
    }

    // FIXME this one need peformance improvement maybe a cache?
    @Override
    public List<String> getChildNamespaces( RepositorySession session, final String repoId, final String namespaceId )
        throws MetadataResolutionException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getNamespaceFamilyName() ) //
            .setColumnNames( NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .execute();

        List<String> namespaces = new ArrayList<>( result.get().getCount() );

        for ( Row<String, String, String> row : result.get() )
        {
            String currentNamespace = getStringValue( row.getColumnSlice(), NAME.toString() );
            if ( StringUtils.startsWith( currentNamespace, namespaceId ) //
                && ( StringUtils.length( currentNamespace ) > StringUtils.length( namespaceId ) ) )
            {
                // store after namespaceId '.' but before next '.'
                // call org namespace org.apache.maven.shared -> stored apache

                String calledNamespace = StringUtils.endsWith( namespaceId, "." ) ? namespaceId : namespaceId + ".";
                String storedNamespace = StringUtils.substringAfter( currentNamespace, calledNamespace );

                storedNamespace = StringUtils.substringBefore( storedNamespace, "." );

                namespaces.add( storedNamespace );
            }
        }

        return namespaces;

    }

    // only use for testing purpose
    protected List<String> getNamespaces( final String repoId )
        throws MetadataResolutionException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getNamespaceFamilyName() ) //
            .setColumnNames( NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .execute();

        List<String> namespaces = new ArrayList<>( result.get().getCount() );

        for ( Row<String, String, String> row : result.get() )
        {
            namespaces.add( getStringValue( row.getColumnSlice(), NAME.toString() ) );
        }

        return namespaces;
    }


    @Override
    public void updateProject( RepositorySession session, String repositoryId, ProjectMetadata projectMetadata )
        throws MetadataRepositoryException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectFamilyName() ) //
            .setColumnNames( PROJECT_ID.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), projectMetadata.getNamespace() ) //
            .addEqualsExpression( PROJECT_ID.toString(), projectMetadata.getId() ) //
            .execute();

        // project exists ? if yes return nothing to update here
        if ( result.get().getCount() > 0 )
        {
            return;
        }
        else
        {
            Namespace namespace = updateOrAddNamespace( repositoryId, projectMetadata.getNamespace() );

            String key =
                new Project.KeyBuilder().withProjectId( projectMetadata.getId() ).withNamespace( namespace ).build();

            String cf = cassandraArchivaManager.getProjectFamilyName();
            projectTemplate.createMutator()
                //  values
                .addInsertion( key, cf, CassandraUtils.column( PROJECT_ID.toString(), projectMetadata.getId() ) ) //
                .addInsertion( key, cf, CassandraUtils.column( REPOSITORY_NAME.toString(), repositoryId ) ) //
                .addInsertion( key, cf, CassandraUtils.column( NAMESPACE_ID.toString(), projectMetadata.getNamespace() ) )//
                .execute();
        }
    }

    @Override
    public List<String> getProjects( RepositorySession session, final String repoId, final String namespace )
        throws MetadataResolutionException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectFamilyName() ) //
            .setColumnNames( PROJECT_ID.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespace ) //
            .execute();

        final Set<String> projects = new HashSet<String>( result.get().getCount() );

        for ( Row<String, String, String> row : result.get() )
        {
            projects.add( getStringValue( row.getColumnSlice(), PROJECT_ID.toString() ) );
        }

        return new ArrayList<>( projects );
    }

    @Override
    public void removeProject( RepositorySession session, final String repositoryId, final String namespaceId, final String projectId )
        throws MetadataRepositoryException
    {

        String key = new Project.KeyBuilder() //
            .withProjectId( projectId ) //
            .withNamespace( new Namespace( namespaceId, new Repository( repositoryId ) ) ) //
            .build();

        this.projectTemplate.deleteRow( key );

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectVersionMetadataFamilyName() ) //
            .setColumnNames( ID.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespaceId ) //
            .addEqualsExpression( PROJECT_ID.toString(), projectId ) //
            .execute();

        for ( Row<String, String, String> row : result.get() )
        {
            this.projectVersionMetadataTemplate.deleteRow( row.getKey() );
            removeMailingList( row.getKey() );
        }

        result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getArtifactMetadataFamilyName() ) //
            .setColumnNames( PROJECT_ID.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespaceId ) //
            .addEqualsExpression( PROJECT_ID.toString(), projectId ) //
            .execute();

        for ( Row<String, String, String> row : result.get() )
        {
            this.artifactMetadataTemplate.deleteRow( row.getKey() );
        }
    }

    @Override
    public List<String> getProjectVersions( RepositorySession session, final String repoId, final String namespace, final String projectId )
        throws MetadataResolutionException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectVersionMetadataFamilyName() ) //
            .setColumnNames( PROJECT_VERSION.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespace ) //
            .addEqualsExpression( PROJECT_ID.toString(), projectId ) //
            .execute();

        int count = result.get().getCount();

        if ( count < 1 )
        {
            return Collections.emptyList();
        }

        Set<String> versions = new HashSet<String>( count );

        for ( Row<String, String, String> orderedRows : result.get() )
        {
            versions.add( getStringValue( orderedRows.getColumnSlice(), PROJECT_VERSION.toString() ) );
        }

        return new ArrayList<>( versions );

    }

    @Override
    public ProjectMetadata getProject( RepositorySession session, final String repoId, final String namespace, final String id )
        throws MetadataResolutionException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectFamilyName() ) //
            .setColumnNames( PROJECT_ID.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespace ) //
            .addEqualsExpression( PROJECT_ID.toString(), id ) //
            .execute();

        int count = result.get().getCount();

        if ( count < 1 )
        {
            return null;
        }

        ProjectMetadata projectMetadata = new ProjectMetadata();
        projectMetadata.setId( id );
        projectMetadata.setNamespace( namespace );

        logger.debug( "getProject repoId: {}, namespace: {}, projectId: {} -> {}", repoId, namespace, id,
                      projectMetadata );

        return projectMetadata;
    }

    protected ProjectVersionMetadataModel mapProjectVersionMetadataModel( ColumnSlice<String, String> columnSlice )
    {
        ProjectVersionMetadataModel projectVersionMetadataModel = new ProjectVersionMetadataModel();
        projectVersionMetadataModel.setId( getStringValue( columnSlice, ID.toString() ) );
        projectVersionMetadataModel.setDescription( getStringValue( columnSlice, DESCRIPTION.toString() ) );
        projectVersionMetadataModel.setName( getStringValue( columnSlice, NAME.toString() ) );
        Namespace namespace = new Namespace( getStringValue( columnSlice, NAMESPACE_ID.toString() ), //
                                             new Repository( getStringValue( columnSlice, REPOSITORY_NAME.toString() ) ) );
        projectVersionMetadataModel.setNamespace( namespace );
        projectVersionMetadataModel.setIncomplete(
            Boolean.parseBoolean( getStringValue( columnSlice, "incomplete" ) ) );
        projectVersionMetadataModel.setProjectId( getStringValue( columnSlice, PROJECT_ID.toString() ) );
        projectVersionMetadataModel.setUrl( getStringValue( columnSlice, URL.toString() ) );
        return projectVersionMetadataModel;
    }


    @Override
    public void updateProjectVersion( RepositorySession session, String repositoryId, String namespaceId, String projectId,
                                      ProjectVersionMetadata versionMetadata )
        throws MetadataRepositoryException
    {
        try
        {
            Namespace namespace = getNamespace( repositoryId, namespaceId );

            if ( namespace == null )
            {
                updateOrAddNamespace( repositoryId, namespaceId );
            }

            if ( getProject( session, repositoryId, namespaceId, projectId ) == null )
            {
                ProjectMetadata projectMetadata = new ProjectMetadata();
                projectMetadata.setNamespace( namespaceId );
                projectMetadata.setId( projectId );
                updateProject( session, repositoryId, projectMetadata );
            }

        }
        catch ( MetadataResolutionException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectVersionMetadataFamilyName() ) //
            .setColumnNames( PROJECT_VERSION.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespaceId ) //
            .addEqualsExpression( PROJECT_ID.toString(), projectId ) //
            .addEqualsExpression( PROJECT_VERSION.toString(), versionMetadata.getId() ) //
            .execute();

        ProjectVersionMetadataModel projectVersionMetadataModel = null;
        boolean creation = true;
        if ( result.get().getCount() > 0 )
        {
            projectVersionMetadataModel =
                mapProjectVersionMetadataModel( result.get().getList().get( 0 ).getColumnSlice() );
            creation = false;
        }
        else
        {
            projectVersionMetadataModel = getModelMapper().map( versionMetadata, ProjectVersionMetadataModel.class );
        }

        projectVersionMetadataModel.setProjectId( projectId );
        projectVersionMetadataModel.setNamespace( new Namespace( namespaceId, new Repository( repositoryId ) ) );

        projectVersionMetadataModel.setCiManagement( versionMetadata.getCiManagement() );
        projectVersionMetadataModel.setIssueManagement( versionMetadata.getIssueManagement() );
        projectVersionMetadataModel.setOrganization( versionMetadata.getOrganization() );
        projectVersionMetadataModel.setScm( versionMetadata.getScm() );

        projectVersionMetadataModel.setMailingLists( versionMetadata.getMailingLists() );
        projectVersionMetadataModel.setDependencies( versionMetadata.getDependencies() );
        projectVersionMetadataModel.setLicenses( versionMetadata.getLicenses() );

        // we don't test of repository and namespace really exist !
        String key = new ProjectVersionMetadataModel.KeyBuilder() //
            .withRepository( repositoryId ) //
            .withNamespace( namespaceId ) //
            .withProjectId( projectId ) //
            .withProjectVersion( versionMetadata.getVersion() ) //
            .withId( versionMetadata.getId() ) //
            .build();

        // FIXME nested objects to store!!!
        if ( creation )
        {
            String cf = cassandraArchivaManager.getProjectVersionMetadataFamilyName();
            Mutator<String> mutator = projectVersionMetadataTemplate.createMutator()
                //  values
                .addInsertion( key, cf, column( PROJECT_ID.toString(), projectId ) ) //
                .addInsertion( key, cf, column( REPOSITORY_NAME.toString(), repositoryId ) ) //
                .addInsertion( key, cf, column( NAMESPACE_ID.toString(), namespaceId ) )//
                .addInsertion( key, cf, column( PROJECT_VERSION.toString(), versionMetadata.getVersion() ) ); //

            addInsertion( mutator, key, cf, DESCRIPTION.toString(), versionMetadata.getDescription() );

            addInsertion( mutator, key, cf, NAME.toString(), versionMetadata.getName() );

            addInsertion( mutator, key, cf, "incomplete", Boolean.toString( versionMetadata.isIncomplete() ) );

            addInsertion( mutator, key, cf, URL.toString(), versionMetadata.getUrl() );
            {
                CiManagement ci = versionMetadata.getCiManagement();
                if ( ci != null )
                {
                    addInsertion( mutator, key, cf, "ciManagement.system", ci.getSystem() );
                    addInsertion( mutator, key, cf, "ciManagement.url", ci.getUrl() );
                }
            }

            {
                IssueManagement issueManagement = versionMetadata.getIssueManagement();

                if ( issueManagement != null )
                {
                    addInsertion( mutator, key, cf, "issueManagement.system", issueManagement.getSystem() );
                    addInsertion( mutator, key, cf, "issueManagement.url", issueManagement.getUrl() );
                }
            }

            {
                Organization organization = versionMetadata.getOrganization();
                if ( organization != null )
                {
                    addInsertion( mutator, key, cf, "organization.name", organization.getName() );
                    addInsertion( mutator, key, cf, "organization.url", organization.getUrl() );
                }
            }

            {
                Scm scm = versionMetadata.getScm();
                if ( scm != null )
                {
                    addInsertion( mutator, key, cf, "scm.url", scm.getUrl() );
                    addInsertion( mutator, key, cf, "scm.connection", scm.getConnection() );
                    addInsertion( mutator, key, cf, "scm.developerConnection", scm.getDeveloperConnection() );
                }
            }

            recordMailingList( key, versionMetadata.getMailingLists() );

            recordLicenses( key, versionMetadata.getLicenses() );

            recordDependencies( key, versionMetadata.getDependencies(), repositoryId );

            MutationResult mutationResult = mutator.execute();
        }
        else
        {
            ColumnFamilyUpdater<String, String> updater = projectVersionMetadataTemplate.createUpdater( key );
            addUpdateStringValue( updater, PROJECT_ID.toString(), projectId );
            addUpdateStringValue( updater, REPOSITORY_NAME.toString(), repositoryId );
            addUpdateStringValue( updater, NAMESPACE_ID.toString(), namespaceId );
            addUpdateStringValue( updater, PROJECT_VERSION.toString(), versionMetadata.getVersion() );
            addUpdateStringValue( updater, DESCRIPTION.toString(), versionMetadata.getDescription() );

            addUpdateStringValue( updater, NAME.toString(), versionMetadata.getName() );

            updater.setString( "incomplete", Boolean.toString( versionMetadata.isIncomplete() ) );
            addUpdateStringValue( updater, URL.toString(), versionMetadata.getUrl() );

            {
                CiManagement ci = versionMetadata.getCiManagement();
                if ( ci != null )
                {
                    addUpdateStringValue( updater, "ciManagement.system", ci.getSystem() );
                    addUpdateStringValue( updater, "ciManagement.url", ci.getUrl() );
                }
            }
            {
                IssueManagement issueManagement = versionMetadata.getIssueManagement();
                if ( issueManagement != null )
                {
                    addUpdateStringValue( updater, "issueManagement.system", issueManagement.getSystem() );
                    addUpdateStringValue( updater, "issueManagement.url", issueManagement.getUrl() );
                }
            }
            {
                Organization organization = versionMetadata.getOrganization();
                if ( organization != null )
                {
                    addUpdateStringValue( updater, "organization.name", organization.getName() );
                    addUpdateStringValue( updater, "organization.url", organization.getUrl() );
                }
            }
            {
                Scm scm = versionMetadata.getScm();
                if ( scm != null )
                {
                    addUpdateStringValue( updater, "scm.url", scm.getUrl() );
                    addUpdateStringValue( updater, "scm.connection", scm.getConnection() );
                    addUpdateStringValue( updater, "scm.developerConnection", scm.getDeveloperConnection() );
                }
            }

            // update is a delete record
            removeMailingList( key );
            recordMailingList( key, versionMetadata.getMailingLists() );

            removeLicenses( key );
            recordLicenses( key, versionMetadata.getLicenses() );

            removeDependencies( key );
            recordDependencies( key, versionMetadata.getDependencies(), repositoryId );

            projectVersionMetadataTemplate.update( updater );

        }

        ArtifactMetadataModel artifactMetadataModel = new ArtifactMetadataModel();
        artifactMetadataModel.setRepositoryId( repositoryId );
        artifactMetadataModel.setNamespace( namespaceId );
        artifactMetadataModel.setProject( projectId );
        artifactMetadataModel.setProjectVersion( versionMetadata.getVersion() );
        artifactMetadataModel.setVersion( versionMetadata.getVersion() );
        updateFacets( versionMetadata, artifactMetadataModel );

    }


    @Override
    public ProjectVersionMetadata getProjectVersion( RepositorySession session, final String repoId, final String namespace,
                                                     final String projectId, final String projectVersion )
        throws MetadataResolutionException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectVersionMetadataFamilyName() ) //
            .setColumnNames( PROJECT_VERSION.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespace ) //
            .addEqualsExpression( PROJECT_ID.toString(), projectId ) //
            .addEqualsExpression( PROJECT_VERSION.toString(), projectVersion ) //
            .execute();

        if ( result.get().getCount() < 1 )
        {
            return null;
        }

        String key = result.get().iterator().next().getKey();

        ColumnFamilyResult<String, String> columnFamilyResult = this.projectVersionMetadataTemplate.queryColumns( key );

        if ( !columnFamilyResult.hasResults() )
        {
            return null;
        }

        ProjectVersionMetadata projectVersionMetadata = new ProjectVersionMetadata();
        projectVersionMetadata.setId( columnFamilyResult.getString( PROJECT_VERSION.toString() ) );
        projectVersionMetadata.setDescription( columnFamilyResult.getString( DESCRIPTION.toString() ) );
        projectVersionMetadata.setName( columnFamilyResult.getString( NAME.toString() ) );

        projectVersionMetadata.setIncomplete( Boolean.parseBoolean( columnFamilyResult.getString( "incomplete" ) ) );

        projectVersionMetadata.setUrl( columnFamilyResult.getString( URL.toString() ) );
        {
            String ciUrl = columnFamilyResult.getString( "ciManagement.url" );
            String ciSystem = columnFamilyResult.getString( "ciManagement.system" );

            if ( StringUtils.isNotEmpty( ciSystem ) || StringUtils.isNotEmpty( ciUrl ) )
            {
                projectVersionMetadata.setCiManagement( new CiManagement( ciSystem, ciUrl ) );
            }
        }
        {
            String issueUrl = columnFamilyResult.getString( "issueManagement.url" );
            String issueSystem = columnFamilyResult.getString( "issueManagement.system" );
            if ( StringUtils.isNotEmpty( issueSystem ) || StringUtils.isNotEmpty( issueUrl ) )
            {
                projectVersionMetadata.setIssueManagement( new IssueManagement( issueSystem, issueUrl ) );
            }
        }
        {
            String organizationUrl = columnFamilyResult.getString( "organization.url" );
            String organizationName = columnFamilyResult.getString( "organization.name" );
            if ( StringUtils.isNotEmpty( organizationUrl ) || StringUtils.isNotEmpty( organizationName ) )
            {
                projectVersionMetadata.setOrganization( new Organization( organizationName, organizationUrl ) );
            }
        }
        {
            String devConn = columnFamilyResult.getString( "scm.developerConnection" );
            String conn = columnFamilyResult.getString( "scm.connection" );
            String url = columnFamilyResult.getString( "scm.url" );
            if ( StringUtils.isNotEmpty( devConn ) || StringUtils.isNotEmpty( conn ) || StringUtils.isNotEmpty( url ) )
            {
                projectVersionMetadata.setScm( new Scm( conn, devConn, url ) );
            }
        }
        projectVersionMetadata.setMailingLists( getMailingLists( key ) );
        projectVersionMetadata.setLicenses( getLicenses( key ) );
        projectVersionMetadata.setDependencies( getDependencies( key ) );
        // facets

        result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getMetadataFacetFamilyName() ) //
            .setColumnNames( FACET_ID.toString(), KEY.toString(), VALUE.toString(), NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespace ) //
            .addEqualsExpression( PROJECT_ID.toString(), projectId ) //
            .addEqualsExpression( PROJECT_VERSION.toString(), projectVersion ) //
            .execute();

        Map<String, Map<String, String>> metadataFacetsPerFacetIds = new HashMap<>();

        for ( Row<String, String, String> row : result.get() )
        {
            ColumnSlice<String, String> columnSlice = row.getColumnSlice();
            String facetId = getStringValue( columnSlice, FACET_ID.toString() );
            Map<String, String> metaValues = metadataFacetsPerFacetIds.get( facetId );
            if ( metaValues == null )
            {
                metaValues = new HashMap<>();
                metadataFacetsPerFacetIds.put( facetId, metaValues );
            }
            metaValues.put( getStringValue( columnSlice, KEY.toString() ), getStringValue( columnSlice, VALUE.toString() ) );
        }

        if ( !metadataFacetsPerFacetIds.isEmpty() )
        {
            for ( Map.Entry<String, Map<String, String>> entry : metadataFacetsPerFacetIds.entrySet() )
            {
                MetadataFacetFactory metadataFacetFactory = getFacetFactory( entry.getKey() );
                if ( metadataFacetFactory != null )
                {
                    MetadataFacet metadataFacet = metadataFacetFactory.createMetadataFacet();
                    metadataFacet.fromProperties( entry.getValue() );
                    projectVersionMetadata.addFacet( metadataFacet );
                }
            }
        }

        return projectVersionMetadata;
    }

    protected void recordChecksums( String repositoryId, String artifactMetadataKey, Map<String, String> checksums)
    {
        if ( checksums == null || checksums.isEmpty() )
        {
            return;
        }
        Mutator<String> checksumMutator = this.checksumTemplate.createMutator();
        for ( Map.Entry<String, String> entry : checksums.entrySet())
        {
            // we don't care about the key as the real used one with the projectVersionMetadata
            String keyChecksums = UUID.randomUUID().toString();
            String cfChecksums = cassandraArchivaManager.getChecksumFamilyName();

            addInsertion( checksumMutator, keyChecksums, cfChecksums, ARTIFACT_METADATA_MODEL_KEY,
                    artifactMetadataKey );
            addInsertion( checksumMutator, keyChecksums, cfChecksums, CHECKSUM_ALG.toString(), entry.getKey());
            addInsertion( checksumMutator, keyChecksums, cfChecksums, CHECKSUM_VALUE.toString(),
                    entry.getValue() );
            addInsertion(checksumMutator, keyChecksums, cfChecksums, REPOSITORY_NAME.toString(), repositoryId);

        }
        checksumMutator.execute();
    }

    protected void removeChecksums( String artifactMetadataKey )
    {

        QueryResult<OrderedRows<String, String, String>> result =
                HFactory.createRangeSlicesQuery( cassandraArchivaManager.getKeyspace(), ss, ss, ss ) //
                        .setColumnFamily( cassandraArchivaManager.getChecksumFamilyName() ) //
                        .setColumnNames( CHECKSUM_ALG.toString() ) //
                        .setRowCount( Integer.MAX_VALUE ) //
                        .addEqualsExpression(ARTIFACT_METADATA_MODEL_KEY, artifactMetadataKey ) //
                        .execute();

        if ( result.get().getCount() < 1 )
        {
            return;
        }

        for ( Row<String, String, String> row : result.get() )
        {
            this.checksumTemplate.deleteRow( row.getKey() );
        }

    }

    protected Map<String, String> getChecksums( String artifactMetadataKey )
    {
        Map<String, String> checksums = new HashMap<>();

        QueryResult<OrderedRows<String, String, String>> result =
                HFactory.createRangeSlicesQuery( cassandraArchivaManager.getKeyspace(), ss, ss, ss ) //
                        .setColumnFamily( cassandraArchivaManager.getChecksumFamilyName() ) //
                        .setColumnNames( ARTIFACT_METADATA_MODEL_KEY, REPOSITORY_NAME.toString(),
                                CHECKSUM_ALG.toString(), CHECKSUM_VALUE.toString() ) //
                        .setRowCount( Integer.MAX_VALUE ) //
                        .addEqualsExpression(ARTIFACT_METADATA_MODEL_KEY, artifactMetadataKey) //
                        .execute();
        for ( Row<String, String, String> row : result.get() )
        {
            ColumnFamilyResult<String, String> columnFamilyResult =
                    this.checksumTemplate.queryColumns( row.getKey() );

            checksums.put(columnFamilyResult.getString(CHECKSUM_ALG.toString()),
                    columnFamilyResult.getString(CHECKSUM_VALUE.toString()));
        }

        return checksums;
    }

    protected void recordMailingList( String projectVersionMetadataKey, List<MailingList> mailingLists )
    {
        if ( mailingLists == null || mailingLists.isEmpty() )
        {
            return;
        }
        Mutator<String> mailingMutator = this.mailingListTemplate.createMutator();
        for ( MailingList mailingList : mailingLists )
        {
            // we don't care about the key as the real used one with the projectVersionMetadata
            String keyMailingList = UUID.randomUUID().toString();
            String cfMailingList = cassandraArchivaManager.getMailingListFamilyName();

            addInsertion( mailingMutator, keyMailingList, cfMailingList, "projectVersionMetadataModel.key",
                          projectVersionMetadataKey );
            addInsertion( mailingMutator, keyMailingList, cfMailingList, NAME.toString(), mailingList.getName() );
            addInsertion( mailingMutator, keyMailingList, cfMailingList, "mainArchiveUrl",
                          mailingList.getMainArchiveUrl() );
            addInsertion( mailingMutator, keyMailingList, cfMailingList, "postAddress", mailingList.getPostAddress() );
            addInsertion( mailingMutator, keyMailingList, cfMailingList, "subscribeAddress",
                          mailingList.getSubscribeAddress() );
            addInsertion( mailingMutator, keyMailingList, cfMailingList, "unsubscribeAddress",
                          mailingList.getUnsubscribeAddress() );
            int idx = 0;
            for ( String otherArchive : mailingList.getOtherArchives() )
            {
                addInsertion( mailingMutator, keyMailingList, cfMailingList, "otherArchive." + idx, otherArchive );
                idx++;
            }

        }
        mailingMutator.execute();
    }

    protected void removeMailingList( String projectVersionMetadataKey )
    {

        QueryResult<OrderedRows<String, String, String>> result =
            HFactory.createRangeSlicesQuery( cassandraArchivaManager.getKeyspace(), ss, ss, ss ) //
                .setColumnFamily( cassandraArchivaManager.getMailingListFamilyName() ) //
                .setColumnNames( NAME.toString() ) //
                .setRowCount( Integer.MAX_VALUE ) //
                .addEqualsExpression( "projectVersionMetadataModel.key", projectVersionMetadataKey ) //
                .execute();

        if ( result.get().getCount() < 1 )
        {
            return;
        }

        for ( Row<String, String, String> row : result.get() )
        {
            this.mailingListTemplate.deleteRow( row.getKey() );
        }

    }

    protected List<MailingList> getMailingLists( String projectVersionMetadataKey )
    {
        List<MailingList> mailingLists = new ArrayList<>();

        QueryResult<OrderedRows<String, String, String>> result =
            HFactory.createRangeSlicesQuery( cassandraArchivaManager.getKeyspace(), ss, ss, ss ) //
                .setColumnFamily( cassandraArchivaManager.getMailingListFamilyName() ) //
                .setColumnNames( NAME.toString() ) //
                .setRowCount( Integer.MAX_VALUE ) //
                .addEqualsExpression( "projectVersionMetadataModel.key", projectVersionMetadataKey ) //
                .execute();
        for ( Row<String, String, String> row : result.get() )
        {
            ColumnFamilyResult<String, String> columnFamilyResult =
                this.mailingListTemplate.queryColumns( row.getKey() );

            MailingList mailingList = new MailingList();
            mailingList.setName( columnFamilyResult.getString( NAME.toString() ) );
            mailingList.setMainArchiveUrl( columnFamilyResult.getString( "mainArchiveUrl" ) );
            mailingList.setPostAddress( columnFamilyResult.getString( "postAddress" ) );
            mailingList.setSubscribeAddress( columnFamilyResult.getString( "subscribeAddress" ) );
            mailingList.setUnsubscribeAddress( columnFamilyResult.getString( "unsubscribeAddress" ) );

            List<String> otherArchives = new ArrayList<>();

            for ( String columnName : columnFamilyResult.getColumnNames() )
            {
                if ( StringUtils.startsWith( columnName, "otherArchive." ) )
                {
                    otherArchives.add( columnFamilyResult.getString( columnName ) );
                }
            }

            mailingList.setOtherArchives( otherArchives );
            mailingLists.add( mailingList );
        }

        return mailingLists;
    }

    protected void recordLicenses( String projectVersionMetadataKey, List<License> licenses )
    {

        if ( licenses == null || licenses.isEmpty() )
        {
            return;
        }
        Mutator<String> licenseMutator = this.licenseTemplate.createMutator();

        for ( License license : licenses )
        {
            // we don't care about the key as the real used one with the projectVersionMetadata
            String keyLicense = UUID.randomUUID().toString();
            String cfLicense = cassandraArchivaManager.getLicenseFamilyName();

            addInsertion( licenseMutator, keyLicense, cfLicense, "projectVersionMetadataModel.key",
                          projectVersionMetadataKey );

            addInsertion( licenseMutator, keyLicense, cfLicense, NAME.toString(), license.getName() );

            addInsertion( licenseMutator, keyLicense, cfLicense, URL.toString(), license.getUrl() );

        }
        licenseMutator.execute();
    }

    protected void removeLicenses( String projectVersionMetadataKey )
    {

        QueryResult<OrderedRows<String, String, String>> result =
            HFactory.createRangeSlicesQuery( cassandraArchivaManager.getKeyspace(), ss, ss, ss ) //
                .setColumnFamily( cassandraArchivaManager.getLicenseFamilyName() ) //
                .setColumnNames( NAME.toString() ) //
                .setRowCount( Integer.MAX_VALUE ) //
                .addEqualsExpression( "projectVersionMetadataModel.key", projectVersionMetadataKey ) //
                .execute();
        for ( Row<String, String, String> row : result.get() )
        {
            this.licenseTemplate.deleteRow( row.getKey() );
        }
    }

    protected List<License> getLicenses( String projectVersionMetadataKey )
    {
        List<License> licenses = new ArrayList<>();

        QueryResult<OrderedRows<String, String, String>> result =
            HFactory.createRangeSlicesQuery( cassandraArchivaManager.getKeyspace(), ss, ss, ss ) //
                .setColumnFamily( cassandraArchivaManager.getLicenseFamilyName() ) //
                .setColumnNames( "projectVersionMetadataModel.key" ) //
                .setRowCount( Integer.MAX_VALUE ) //
                .addEqualsExpression( "projectVersionMetadataModel.key", projectVersionMetadataKey ) //
                .execute();

        for ( Row<String, String, String> row : result.get() )
        {
            ColumnFamilyResult<String, String> columnFamilyResult = this.licenseTemplate.queryColumns( row.getKey() );

            licenses.add(
                new License( columnFamilyResult.getString( NAME.toString() ), columnFamilyResult.getString( URL.toString() ) ) );
        }

        return licenses;
    }


    protected void recordDependencies( String projectVersionMetadataKey, List<Dependency> dependencies,
                                       String repositoryId )
    {

        if ( dependencies == null || dependencies.isEmpty() )
        {
            return;
        }
        Mutator<String> dependencyMutator = this.dependencyTemplate.createMutator();

        for ( Dependency dependency : dependencies )
        {
            // we don't care about the key as the real used one with the projectVersionMetadata
            String keyDependency = UUID.randomUUID().toString();
            String cfDependency = cassandraArchivaManager.getDependencyFamilyName();

            addInsertion( dependencyMutator, keyDependency, cfDependency, "projectVersionMetadataModel.key",
                          projectVersionMetadataKey );

            addInsertion( dependencyMutator, keyDependency, cfDependency, REPOSITORY_NAME.toString(), repositoryId );

            addInsertion( dependencyMutator, keyDependency, cfDependency, "classifier", dependency.getClassifier() );

            addInsertion( dependencyMutator, keyDependency, cfDependency, "optional",
                          Boolean.toString( dependency.isOptional() ) );

            addInsertion( dependencyMutator, keyDependency, cfDependency, "scope", dependency.getScope() );

            addInsertion( dependencyMutator, keyDependency, cfDependency, "systemPath", dependency.getSystemPath() );

            addInsertion( dependencyMutator, keyDependency, cfDependency, "type", dependency.getType() );

            addInsertion( dependencyMutator, keyDependency, cfDependency, ARTIFACT_ID.toString(), dependency.getArtifactId() );

            addInsertion( dependencyMutator, keyDependency, cfDependency, GROUP_ID.toString(), dependency.getNamespace() );

            addInsertion( dependencyMutator, keyDependency, cfDependency, VERSION.toString(), dependency.getVersion() );

        }
        dependencyMutator.execute();
    }

    protected void removeDependencies( String projectVersionMetadataKey )
    {

        QueryResult<OrderedRows<String, String, String>> result =
            HFactory.createRangeSlicesQuery( cassandraArchivaManager.getKeyspace(), ss, ss, ss ) //
                .setColumnFamily( cassandraArchivaManager.getDependencyFamilyName() ) //
                .setColumnNames( GROUP_ID.toString() ) //
                .setRowCount( Integer.MAX_VALUE ) //
                .addEqualsExpression( "projectVersionMetadataModel.key", projectVersionMetadataKey ) //
                .execute();
        for ( Row<String, String, String> row : result.get() )
        {
            this.dependencyTemplate.deleteRow( row.getKey() );
        }
    }

    protected List<Dependency> getDependencies( String projectVersionMetadataKey )
    {
        List<Dependency> dependencies = new ArrayList<>();

        QueryResult<OrderedRows<String, String, String>> result =
            HFactory.createRangeSlicesQuery( cassandraArchivaManager.getKeyspace(), ss, ss, ss ) //
                .setColumnFamily( cassandraArchivaManager.getDependencyFamilyName() ) //
                .setColumnNames( "projectVersionMetadataModel.key" ) //
                .setRowCount( Integer.MAX_VALUE ) //
                .addEqualsExpression( "projectVersionMetadataModel.key", projectVersionMetadataKey ) //
                .execute();

        for ( Row<String, String, String> row : result.get() )
        {
            ColumnFamilyResult<String, String> columnFamilyResult =
                this.dependencyTemplate.queryColumns( row.getKey() );

            Dependency dependency = new Dependency();
            dependency.setClassifier( columnFamilyResult.getString( "classifier" ) );

            dependency.setOptional( Boolean.parseBoolean( columnFamilyResult.getString( "optional" ) ) );

            dependency.setScope( columnFamilyResult.getString( "scope" ) );

            dependency.setSystemPath( columnFamilyResult.getString( "systemPath" ) );

            dependency.setType( columnFamilyResult.getString( "type" ) );

            dependency.setArtifactId( columnFamilyResult.getString( ARTIFACT_ID.toString() ) );

            dependency.setNamespace( columnFamilyResult.getString( GROUP_ID.toString() ) );

            dependency.setVersion( columnFamilyResult.getString( VERSION.toString() ) );

            dependencies.add( dependency );
        }

        return dependencies;
    }

    private Map<String, String> mapChecksums(Map<ChecksumAlgorithm,String> checksums) {
        return checksums.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().name(), e -> e.getValue()
        ));
    }

    private Map<ChecksumAlgorithm, String> mapChecksumsReverse(Map<String,String> checksums) {
        return checksums.entrySet().stream().collect(Collectors.toMap(
                e -> ChecksumAlgorithm.valueOf(e.getKey()), e -> e.getValue()
        ));
    }

    @Override
    public void updateArtifact( RepositorySession session, String repositoryId, String namespaceId, String projectId, String projectVersion,
                                ArtifactMetadata artifactMeta )
        throws MetadataRepositoryException
    {

        Namespace namespace = getNamespace( repositoryId, namespaceId );
        if ( namespace == null )
        {
            namespace = updateOrAddNamespace( repositoryId, namespaceId );
        }

        ProjectMetadata projectMetadata = new ProjectMetadata();
        projectMetadata.setId( projectId );
        projectMetadata.setNamespace( namespaceId );
        updateProject( session, repositoryId, projectMetadata );

        String key = new ArtifactMetadataModel.KeyBuilder().withNamespace( namespace ).withProject( projectId ).withId(
            artifactMeta.getId() ).withProjectVersion( projectVersion ).build();

        // exists?

        boolean exists = this.artifactMetadataTemplate.isColumnsExist( key );

        if ( exists )
        {
            // updater
            ColumnFamilyUpdater<String, String> updater = this.artifactMetadataTemplate.createUpdater( key );
            updater.setLong( FILE_LAST_MODIFIED.toString(), artifactMeta.getFileLastModified().toInstant().toEpochMilli());
            updater.setLong( WHEN_GATHERED.toString(), artifactMeta.getWhenGathered().toInstant().toEpochMilli() );
            updater.setLong( SIZE.toString(), artifactMeta.getSize() );
            addUpdateStringValue( updater, VERSION.toString(), artifactMeta.getVersion() );
            removeChecksums(key);
            recordChecksums(repositoryId, key, mapChecksums(artifactMeta.getChecksums()));
            this.artifactMetadataTemplate.update( updater );
        }
        else
        {
            String cf = this.cassandraArchivaManager.getArtifactMetadataFamilyName();
            // create
            this.artifactMetadataTemplate.createMutator() //
                .addInsertion( key, cf, column( ID.toString(), artifactMeta.getId() ) )//
                .addInsertion( key, cf, column( REPOSITORY_NAME.toString(), repositoryId ) ) //
                .addInsertion( key, cf, column( NAMESPACE_ID.toString(), namespaceId ) ) //
                .addInsertion( key, cf, column( PROJECT.toString(), artifactMeta.getProject() ) ) //
                .addInsertion( key, cf, column( PROJECT_VERSION.toString(), projectVersion ) ) //
                .addInsertion( key, cf, column( VERSION.toString(), artifactMeta.getVersion() ) ) //
                .addInsertion( key, cf, column( FILE_LAST_MODIFIED.toString(), artifactMeta.getFileLastModified().toInstant().toEpochMilli() ) ) //
                .addInsertion( key, cf, column( SIZE.toString(), artifactMeta.getSize() ) ) //
                .addInsertion( key, cf, column( WHEN_GATHERED.toString(), artifactMeta.getWhenGathered().toInstant().toEpochMilli() ) )//
                .execute();
            recordChecksums(repositoryId, key, mapChecksums(artifactMeta.getChecksums()));
        }

        key = new ProjectVersionMetadataModel.KeyBuilder() //
            .withRepository( repositoryId ) //
            .withNamespace( namespace ) //
            .withProjectId( projectId ) //
            .withProjectVersion( projectVersion ) //
            .withId( artifactMeta.getId() ) //
            .build();

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectVersionMetadataFamilyName() ) //
            .setColumnNames( VERSION.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespaceId ) //
            .addEqualsExpression( PROJECT_ID.toString(), projectId ) //
            .addEqualsExpression( PROJECT_VERSION.toString(), projectVersion ) //
            .addEqualsExpression( VERSION.toString(), artifactMeta.getVersion() ) //
            .execute();

        exists = result.get().getCount() > 0;

        if ( !exists )
        {
            String cf = this.cassandraArchivaManager.getProjectVersionMetadataFamilyName();

            projectVersionMetadataTemplate.createMutator() //
                .addInsertion( key, cf, column( NAMESPACE_ID.toString(), namespace.getName() ) ) //
                .addInsertion( key, cf, column( REPOSITORY_NAME.toString(), repositoryId ) ) //
                .addInsertion( key, cf, column( PROJECT_VERSION.toString(), projectVersion ) ) //
                .addInsertion( key, cf, column( PROJECT_ID.toString(), projectId ) ) //
                .addInsertion( key, cf, column( VERSION.toString(), artifactMeta.getVersion() ) ) //
                .execute();

        }

        ArtifactMetadataModel artifactMetadataModel = new ArtifactMetadataModel();

        artifactMetadataModel.setRepositoryId( repositoryId );
        artifactMetadataModel.setNamespace( namespaceId );
        artifactMetadataModel.setProject( projectId );
        artifactMetadataModel.setProjectVersion( projectVersion );
        artifactMetadataModel.setVersion( artifactMeta.getVersion() );
        artifactMetadataModel.setFileLastModified( artifactMeta.getFileLastModified() == null
                                                       ? ZonedDateTime.now().toInstant().toEpochMilli()
                                                       : artifactMeta.getFileLastModified().toInstant().toEpochMilli() );
        artifactMetadataModel.setChecksums(mapChecksums(artifactMeta.getChecksums()));

        // now facets
        updateFacets( artifactMeta, artifactMetadataModel );

    }

    @Override
    public List<String> getArtifactVersions( RepositorySession session, final String repoId, final String namespace, final String projectId,
                                             final String projectVersion )
        throws MetadataResolutionException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectVersionMetadataFamilyName() ) //
            .setColumnNames( VERSION.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespace ) //
            .addEqualsExpression( PROJECT_ID.toString(), projectId ) //
            .addEqualsExpression( PROJECT_VERSION.toString(), projectVersion ) //
            .execute();

        final Set<String> versions = new HashSet<>();

        for ( Row<String, String, String> row : result.get() )
        {
            versions.add( getStringValue( row.getColumnSlice(), VERSION.toString() ) );
        }

        return new ArrayList<>( versions );

    }

    /*
     * iterate over available facets to remove/add from the artifactMetadata
     *
     * @param facetedMetadata
     * @param artifactMetadataModel only use for the key
     */
    private void updateFacets( final FacetedMetadata facetedMetadata,
                               final ArtifactMetadataModel artifactMetadataModel )
    {

        String cf = cassandraArchivaManager.getMetadataFacetFamilyName();

        for ( final String facetId : getSupportedFacets() )
        {
            MetadataFacet metadataFacet = facetedMetadata.getFacet( facetId );
            if ( metadataFacet == null )
            {
                continue;
            }
            // clean first

            QueryResult<OrderedRows<String, String, String>> result =
                HFactory.createRangeSlicesQuery( keyspace, ss, ss, ss ) //
                    .setColumnFamily( cf ) //
                    .setColumnNames( REPOSITORY_NAME.toString() ) //
                    .addEqualsExpression( REPOSITORY_NAME.toString(), artifactMetadataModel.getRepositoryId() ) //
                    .addEqualsExpression( NAMESPACE_ID.toString(), artifactMetadataModel.getNamespace() ) //
                    .addEqualsExpression( PROJECT_ID.toString(), artifactMetadataModel.getProject() ) //
                    .addEqualsExpression( PROJECT_VERSION.toString(), artifactMetadataModel.getProjectVersion() ) //
                    .addEqualsExpression( FACET_ID.toString(), facetId ) //
                    .execute();

            for ( Row<String, String, String> row : result.get().getList() )
            {
                this.metadataFacetTemplate.deleteRow( row.getKey() );
            }

            Map<String, String> properties = metadataFacet.toProperties();

            for ( Map.Entry<String, String> entry : properties.entrySet() )
            {
                String key = new MetadataFacetModel.KeyBuilder().withKey( entry.getKey() ).withArtifactMetadataModel(
                    artifactMetadataModel ).withFacetId( facetId ).withName( metadataFacet.getName() ).build();
                Mutator<String> mutator = metadataFacetTemplate.createMutator() //
                    .addInsertion( key, cf, column( REPOSITORY_NAME.toString(), artifactMetadataModel.getRepositoryId() ) ) //
                    .addInsertion( key, cf, column( NAMESPACE_ID.toString(), artifactMetadataModel.getNamespace() ) ) //
                    .addInsertion( key, cf, column( PROJECT_ID.toString(), artifactMetadataModel.getProject() ) ) //
                    .addInsertion( key, cf, column( PROJECT_VERSION.toString(), artifactMetadataModel.getProjectVersion() ) ) //
                    .addInsertion( key, cf, column( FACET_ID.toString(), facetId ) ) //
                    .addInsertion( key, cf, column( KEY.toString(), entry.getKey() ) ) //
                    .addInsertion( key, cf, column( VALUE.toString(), entry.getValue() ) );

                if ( metadataFacet.getName() != null )
                {
                    mutator.addInsertion( key, cf, column( NAME.toString(), metadataFacet.getName() ) );
                }

                mutator.execute();
            }
        }
    }


    @Override
    public List<String> getMetadataFacets( RepositorySession session, final String repositoryId, final String facetId )
        throws MetadataRepositoryException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getMetadataFacetFamilyName() ) //
            .setColumnNames( NAME.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .addEqualsExpression( FACET_ID.toString(), facetId ) //
            .execute();

        final List<String> facets = new ArrayList<>();

        for ( Row<String, String, String> row : result.get() )
        {
            facets.add( getStringValue( row.getColumnSlice(), NAME.toString() ) );
        }
        return facets;
    }

    private <T> Spliterator<T> createResultSpliterator( QueryResult<OrderedRows<String, String, String>> result, BiFunction<Row<String, String, String>, T, T> converter) throws MetadataRepositoryException
    {
        final int size = result.get().getCount();
        final Iterator<Row<String, String, String>> it = result.get( ).iterator( );

        return new Spliterator<T>( )
        {
            private T lastItem = null;

            @Override
            public boolean tryAdvance( Consumer<? super T> action )
            {
                if (size>=1)
                {
                    if(it.hasNext())
                    {
                        while ( it.hasNext( ) )
                        {
                            Row<String, String, String> row = it.next( );
                            T item = converter.apply( row, lastItem );
                            if ( item != null && lastItem !=null && item != lastItem )
                            {
                                action.accept( lastItem );
                                lastItem = item;
                                return true;
                            }
                            lastItem = item;
                        }
                        action.accept( lastItem );
                        return true;
                    } else {
                        return false;
                    }
                }
                return false;
            }

            @Override
            public Spliterator<T> trySplit( )
            {
                return null;
            }

            @Override
            public long estimateSize( )
            {
                return size;
            }

            @Override
            public int characteristics( )
            {
                return ORDERED+NONNULL+SIZED;
            }
        };
    }


    /**
     * Implementation is not very performant, because sorting is part of the stream. I do not know how to specify the sort
     * in the query.
     * 
     * @param <T>
     * @param session
     * @param repositoryId
     * @param facetClazz
     * @param queryParameter
     * @return
     * @throws MetadataRepositoryException
     */
    @Override
    public <T extends MetadataFacet> Stream<T> getMetadataFacetStream(RepositorySession session, String repositoryId, Class<T> facetClazz, QueryParameter queryParameter) throws MetadataRepositoryException
    {
        final MetadataFacetFactory<T> metadataFacetFactory = getFacetFactory( facetClazz );
        final String facetId = metadataFacetFactory.getFacetId( );

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getMetadataFacetFamilyName( ) ) //
            .setColumnNames( NAME.toString( ), KEY.toString( ), VALUE.toString( ) ) //
            .addEqualsExpression( REPOSITORY_NAME.toString( ), repositoryId ) //
            .addEqualsExpression( FACET_ID.toString( ), facetId ) //
            .setRange( null, null, false, Integer.MAX_VALUE )
            .setRowCount( Integer.MAX_VALUE )
            .execute( );



        return StreamSupport.stream( createResultSpliterator( result, ( Row<String, String, String> row, T lastItem)-> {
            ColumnSlice<String, String> columnSlice = row.getColumnSlice();
            String name = getStringValue( columnSlice, NAME.toString( ) );
            T updateItem;
            if (lastItem!=null && lastItem.getName().equals(name))
            {
                updateItem = lastItem;
            } else
            {
                updateItem = metadataFacetFactory.createMetadataFacet( repositoryId, name );
            }
            String key = getStringValue( columnSlice, KEY.toString() );
            if (StringUtils.isNotEmpty( key ))
            {
                Map<String, String> map = new HashMap<>( );
                map.put( key , getStringValue( columnSlice, VALUE.toString( ) ) );
                updateItem.fromProperties( map );
            }
            return updateItem;

        }), false ).sorted( (f1, f2) -> f1.getName()!=null ? f1.getName().compareTo( f2.getName() ) : 1 ).skip( queryParameter.getOffset()).limit( queryParameter.getLimit());
    }

    @Override
    public boolean hasMetadataFacet( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        return !getMetadataFacets( session, repositoryId, facetId ).isEmpty();
    }

    @Override
    public <T extends MetadataFacet> T getMetadataFacet( RepositorySession session, final String repositoryId, final Class<T> facetClazz, final String name )
        throws MetadataRepositoryException
    {
        final MetadataFacetFactory<T> metadataFacetFactory = getFacetFactory( facetClazz );
        if (metadataFacetFactory==null) {
            return null;
        }
        final String facetId = metadataFacetFactory.getFacetId( );
        if ( metadataFacetFactory == null )
        {
            return null;
        }

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getMetadataFacetFamilyName() ) //
            .setColumnNames( KEY.toString(), VALUE.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .addEqualsExpression( FACET_ID.toString(), facetId ) //
            .addEqualsExpression( NAME.toString(), name ) //
            .execute();

        T metadataFacet = metadataFacetFactory.createMetadataFacet( repositoryId, name );
        int size = result.get().getCount();
        if ( size < 1 )
        {
            return null;
        }
        Map<String, String> map = new HashMap<>( size );
        for ( Row<String, String, String> row : result.get() )
        {
            ColumnSlice<String, String> columnSlice = row.getColumnSlice();
            map.put( getStringValue( columnSlice, KEY.toString() ), getStringValue( columnSlice, VALUE.toString() ) );
        }
        metadataFacet.fromProperties( map );
        return metadataFacet;
    }

    @Override
    public MetadataFacet getMetadataFacet( RepositorySession session, String repositoryId, String facetId, String name ) throws MetadataRepositoryException
    {
        return getMetadataFacet( session, repositoryId, getFactoryClassForId( facetId ), name );
    }

    @Override
    public void addMetadataFacet( RepositorySession session, String repositoryId, MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {

        if ( metadataFacet == null )
        {
            return;
        }

        if ( metadataFacet.toProperties().isEmpty() )
        {
            String key = new MetadataFacetModel.KeyBuilder().withRepositoryId( repositoryId ).withFacetId(
                metadataFacet.getFacetId() ).withName( metadataFacet.getName() ).build();

            boolean exists = this.metadataFacetTemplate.isColumnsExist( key );

            if ( exists )
            {
                ColumnFamilyUpdater<String, String> updater = this.metadataFacetTemplate.createUpdater( key );
                addUpdateStringValue( updater, FACET_ID.toString(), metadataFacet.getFacetId() );
                addUpdateStringValue( updater, NAME.toString(), metadataFacet.getName() );
                this.metadataFacetTemplate.update( updater );
            }
            else
            {
                String cf = this.cassandraArchivaManager.getMetadataFacetFamilyName();
                this.metadataFacetTemplate.createMutator() //
                    .addInsertion( key, cf, column( REPOSITORY_NAME.toString(), repositoryId ) ) //
                    .addInsertion( key, cf, column( FACET_ID.toString(), metadataFacet.getFacetId() ) ) //
                    .addInsertion( key, cf, column( NAME.toString(), metadataFacet.getName() ) ) //
                    .execute();
            }

        }
        else
        {
            for ( Map.Entry<String, String> entry : metadataFacet.toProperties().entrySet() )
            {
                String key = new MetadataFacetModel.KeyBuilder().withRepositoryId( repositoryId ).withFacetId(
                    metadataFacet.getFacetId() ).withName( metadataFacet.getName() ).withKey( entry.getKey() ).build();

                boolean exists = this.metadataFacetTemplate.isColumnsExist( key );
                if ( !exists )
                {
                    String cf = this.cassandraArchivaManager.getMetadataFacetFamilyName();
                    this.metadataFacetTemplate.createMutator() //
                        .addInsertion( key, cf, column( REPOSITORY_NAME.toString(), repositoryId ) ) //
                        .addInsertion( key, cf, column( FACET_ID.toString(), metadataFacet.getFacetId() ) ) //
                        .addInsertion( key, cf, column( NAME.toString(), metadataFacet.getName() ) ) //
                        .addInsertion( key, cf, column( KEY.toString(), entry.getKey() ) ) //
                        .addInsertion( key, cf, column( VALUE.toString(), entry.getValue() ) ) //
                        .execute();
                }
                else
                {
                    ColumnFamilyUpdater<String, String> updater = this.metadataFacetTemplate.createUpdater( key );
                    addUpdateStringValue( updater, VALUE.toString(), entry.getValue() );
                    this.metadataFacetTemplate.update( updater );
                }
            }
        }
    }

    @Override
    public void removeMetadataFacets( RepositorySession session, final String repositoryId, final String facetId )
        throws MetadataRepositoryException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getMetadataFacetFamilyName() ) //
            .setColumnNames( KEY.toString(), VALUE.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .addEqualsExpression( FACET_ID.toString(), facetId ) //
            .execute();

        for ( Row<String, String, String> row : result.get() )
        {
            this.metadataFacetTemplate.deleteRow( row.getKey() );
        }

    }

    @Override
    public void removeMetadataFacet( RepositorySession session, final String repositoryId, final String facetId, final String name )
        throws MetadataRepositoryException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getMetadataFacetFamilyName() ) //
            .setColumnNames( KEY.toString(), VALUE.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .addEqualsExpression( FACET_ID.toString(), facetId ) //
            .addEqualsExpression( NAME.toString(), name ) //
            .execute();

        for ( Row<String, String, String> row : result.get() )
        {
            this.metadataFacetTemplate.deleteRow( row.getKey() );
        }
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByDateRange( RepositorySession session, final String repositoryId, final ZonedDateTime startTime,
                                                           final ZonedDateTime endTime, QueryParameter queryParameter )
        throws MetadataRepositoryException
    {

        LongSerializer ls = LongSerializer.get();
        RangeSlicesQuery<String, String, Long> query = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ls ) //
            .setColumnFamily( cassandraArchivaManager.getArtifactMetadataFamilyName() ) //
            .setColumnNames( ArtifactMetadataModel.COLUMNS ); //


        if ( startTime != null )
        {
            query = query.addGteExpression( WHEN_GATHERED.toString(), startTime.toInstant().toEpochMilli() );
        }
        if ( endTime != null )
        {
            query = query.addLteExpression( WHEN_GATHERED.toString(), endTime.toInstant().toEpochMilli() );
        }
        QueryResult<OrderedRows<String, String, Long>> result = query.execute();

        List<ArtifactMetadata> artifactMetadatas = new ArrayList<>( result.get().getCount() );
        Iterator<Row<String, String, Long>> keyIter = result.get().iterator();
        if (keyIter.hasNext()) {
            String key = keyIter.next().getKey();
            for (Row<String, String, Long> row : result.get()) {
                ColumnSlice<String, Long> columnSlice = row.getColumnSlice();
                String repositoryName = getAsStringValue(columnSlice, REPOSITORY_NAME.toString());
                if (StringUtils.equals(repositoryName, repositoryId)) {

                    artifactMetadatas.add(mapArtifactMetadataLongColumnSlice(key, columnSlice));
                }
            }
        }

        return artifactMetadatas;
    }

    /**
     * For documentation see {@link MetadataRepository#getArtifactByDateRangeStream(RepositorySession, String, ZonedDateTime, ZonedDateTime, QueryParameter)}
     *
     * This implementation orders the stream. It does not order the query in the backend.
     *
     * @param session The repository session
     * @param repositoryId The repository id
     * @param startTime The start time, can be <code>null</code>
     * @param endTime The end time, can be <code>null</code>
     * @param queryParameter Additional parameters for the query that affect ordering and number of returned results.
     * @return
     * @throws MetadataRepositoryException
     * @see MetadataRepository#getArtifactByDateRangeStream
     */
    @Override
    public Stream<ArtifactMetadata> getArtifactByDateRangeStream( RepositorySession session, String repositoryId, ZonedDateTime startTime, ZonedDateTime endTime, QueryParameter queryParameter) throws MetadataRepositoryException
    {
        Comparator<ArtifactMetadata> comp = getArtifactMetadataComparator(queryParameter, "whenGathered");
        return getArtifactsByDateRange(session, repositoryId, startTime, endTime, queryParameter).stream().sorted(comp).skip(queryParameter.getOffset()).limit(queryParameter.getLimit());
    }


    protected ArtifactMetadata mapArtifactMetadataLongColumnSlice( String key, ColumnSlice<String, Long> columnSlice )
    {
        ArtifactMetadata artifactMetadata = new ArtifactMetadata();
        artifactMetadata.setNamespace( getAsStringValue( columnSlice, NAMESPACE_ID.toString() ) );
        artifactMetadata.setSize( getLongValue( columnSlice, SIZE.toString() ) );
        artifactMetadata.setId( getAsStringValue( columnSlice, ID.toString() ) );
        artifactMetadata.setFileLastModified( getLongValue( columnSlice, FILE_LAST_MODIFIED.toString() ) );
        artifactMetadata.setMd5( getAsStringValue( columnSlice, MD5.toString() ) );
        artifactMetadata.setProject( getAsStringValue( columnSlice, PROJECT.toString() ) );
        artifactMetadata.setProjectVersion( getAsStringValue( columnSlice, PROJECT_VERSION.toString() ) );
        artifactMetadata.setRepositoryId( getAsStringValue( columnSlice, REPOSITORY_NAME.toString() ) );
        artifactMetadata.setSha1( getAsStringValue( columnSlice, SHA1.toString() ) );
        artifactMetadata.setVersion( getAsStringValue( columnSlice, VERSION.toString() ) );
        Long whenGathered = getLongValue( columnSlice, WHEN_GATHERED.toString() );
        if ( whenGathered != null )
        {
            artifactMetadata.setWhenGathered(ZonedDateTime.ofInstant(Instant.ofEpochMilli(whenGathered), STORAGE_TZ));
        }
        artifactMetadata.setChecksums(mapChecksumsReverse(getChecksums(key)));
        return artifactMetadata;
    }

    protected ArtifactMetadata mapArtifactMetadataStringColumnSlice( String key, ColumnSlice<String, String> columnSlice )
    {
        ArtifactMetadata artifactMetadata = new ArtifactMetadata();
        artifactMetadata.setNamespace( getStringValue( columnSlice, NAMESPACE_ID.toString() ) );
        artifactMetadata.setSize( getAsLongValue( columnSlice, SIZE.toString() ) );
        artifactMetadata.setId( getStringValue( columnSlice, ID.toString() ) );
        artifactMetadata.setFileLastModified( getAsLongValue( columnSlice, FILE_LAST_MODIFIED.toString() ) );
        artifactMetadata.setMd5( getStringValue( columnSlice, MD5.toString() ) );
        artifactMetadata.setProject( getStringValue( columnSlice, PROJECT.toString() ) );
        artifactMetadata.setProjectVersion( getStringValue( columnSlice, PROJECT_VERSION.toString() ) );
        artifactMetadata.setRepositoryId( getStringValue( columnSlice, REPOSITORY_NAME.toString() ) );
        artifactMetadata.setSha1( getStringValue( columnSlice, SHA1.toString() ) );
        artifactMetadata.setVersion( getStringValue( columnSlice, VERSION.toString() ) );
        Long whenGathered = getAsLongValue( columnSlice, WHEN_GATHERED.toString() );
        if ( whenGathered != null )
        {
            artifactMetadata.setWhenGathered(ZonedDateTime.ofInstant(Instant.ofEpochMilli(whenGathered), STORAGE_TZ));
        }
        artifactMetadata.setChecksums(mapChecksumsReverse(getChecksums(key)));
        return artifactMetadata;
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByChecksum(RepositorySession session, final String repositoryId, final String checksum )
        throws MetadataRepositoryException
    {

        // cql cannot run or in queries so running twice the query
        Map<String, ArtifactMetadata> artifactMetadataMap = new HashMap<>();

        RangeSlicesQuery<String, String, String> query = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getChecksumFamilyName()) //
            .setColumnNames(ARTIFACT_METADATA_MODEL_KEY); //

        query = query.addEqualsExpression( CHECKSUM_VALUE.toString(), checksum )
                .addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId );

        QueryResult<OrderedRows<String, String, String>> result = query.execute();

        List<String> artifactKeys = new ArrayList<>();
        for ( Row<String, String, String> row : result.get() )
        {
            ColumnSlice<String, String> columnSlice = row.getColumnSlice();

            artifactKeys.add(columnSlice.getColumnByName(ARTIFACT_METADATA_MODEL_KEY).getValue());

        }

        for (String key : artifactKeys) {
            query = HFactory //
                    .createRangeSlicesQuery(keyspace, ss, ss, ss) //
                    .setColumnFamily(cassandraArchivaManager.getArtifactMetadataFamilyName()) //
                    .setColumnNames(NAMESPACE_ID.toString(), SIZE.toString(), ID.toString(), FILE_LAST_MODIFIED.toString(), MD5.toString(), PROJECT.toString(), PROJECT_VERSION.toString(),
                            REPOSITORY_NAME.toString(), VERSION.toString(), WHEN_GATHERED.toString(), SHA1.toString())
                    .setKeys(key, key);
            result = query.execute();

            for (Row<String, String, String> row : result.get()) {
                ColumnSlice<String, String> columnSlice = row.getColumnSlice();

                artifactMetadataMap.put(row.getKey(), mapArtifactMetadataStringColumnSlice(key, columnSlice));
            }
        }

        return new ArrayList(artifactMetadataMap.values());
    }

    /**
     * Project version and artifact level metadata are stored in the same place, no distinctions in Cassandra
     * implementation, just calls {@link MetadataRepository#getArtifactsByAttribute(RepositorySession, String, String, String)}
     */
    @Override
    public List<ArtifactMetadata> getArtifactsByProjectVersionFacet( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        return this.getArtifactsByAttribute( session, key, value, repositoryId );
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByAttribute( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        RangeSlicesQuery<String, String, String> query =
            HFactory.createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getMetadataFacetFamilyName() ) //
            .setColumnNames( MetadataFacetModel.COLUMNS ) //
            .addEqualsExpression( VALUE.toString(), value );

        if ( key != null )
        {
            query.addEqualsExpression( KEY.toString(), key ); //
        }
        if ( repositoryId != null )
        {
            query.addEqualsExpression( "repositoryName", repositoryId );
        }

        QueryResult<OrderedRows<String, String, String>> metadataFacetResult = query.execute();
        if ( metadataFacetResult.get() == null || metadataFacetResult.get().getCount() < 1 )
        {
            return Collections.emptyList();
        }

        List<ArtifactMetadata> artifactMetadatas = new LinkedList<ArtifactMetadata>();

        // TODO doing multiple queries, there should be a way to get all the artifactMetadatas for any number of
        // projects
        for ( Row<String, String, String> row : metadataFacetResult.get() )
        {
            QueryResult<OrderedRows<String, String, String>> artifactMetadataResult =
                HFactory.createRangeSlicesQuery( keyspace, ss, ss, ss ) //
                .setColumnFamily( cassandraArchivaManager.getArtifactMetadataFamilyName() ) //
                .setColumnNames( ArtifactMetadataModel.COLUMNS ) //
                .setRowCount( Integer.MAX_VALUE ) //
                .addEqualsExpression( REPOSITORY_NAME.toString(),
                                      getStringValue( row.getColumnSlice(), REPOSITORY_NAME ) ) //
                .addEqualsExpression( NAMESPACE_ID.toString(), getStringValue( row.getColumnSlice(), NAMESPACE_ID ) ) //
                .addEqualsExpression( PROJECT.toString(), getStringValue( row.getColumnSlice(), PROJECT_ID ) ) //
                .addEqualsExpression( PROJECT_VERSION.toString(),
                                      getStringValue( row.getColumnSlice(), PROJECT_VERSION ) ) //
                .execute();

            if ( artifactMetadataResult.get() == null || artifactMetadataResult.get().getCount() < 1 )
            {
                return Collections.emptyList();
            }

            for ( Row<String, String, String> artifactMetadataRow : artifactMetadataResult.get() )
            {
                String artifactKey = artifactMetadataRow.getKey();
                artifactMetadatas.add( mapArtifactMetadataStringColumnSlice( artifactKey, artifactMetadataRow.getColumnSlice() ) );
            }
        }

        return mapArtifactFacetToArtifact( metadataFacetResult, artifactMetadatas );
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByProjectVersionAttribute( RepositorySession session, String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        QueryResult<OrderedRows<String, String, String>> result =
            HFactory.createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectVersionMetadataFamilyName() ) //
            .setColumnNames( PROJECT_ID.toString(), REPOSITORY_NAME.toString(), NAMESPACE_ID.toString(),
                             PROJECT_VERSION.toString() ) //
            .addEqualsExpression( key, value ) //
            .execute();

        int count = result.get().getCount();

        if ( count < 1 )
        {
            return Collections.emptyList();
        }

        List<ArtifactMetadata> artifacts = new LinkedList<ArtifactMetadata>();

        for ( Row<String, String, String> row : result.get() )
        {
            // TODO doing multiple queries, there should be a way to get all the artifactMetadatas for any number of
            // projects
            try
            {
                artifacts.addAll( getArtifacts( session,
                    getStringValue( row.getColumnSlice(), REPOSITORY_NAME ),
                    getStringValue( row.getColumnSlice(), NAMESPACE_ID ),
                    getStringValue( row.getColumnSlice(), PROJECT_ID ), getStringValue( row.getColumnSlice(), PROJECT_VERSION ) ) );
            }
            catch ( MetadataResolutionException e )
            {
                // never raised
                throw new IllegalStateException( e );
            }
        }
        return artifacts;
    }

    @Override
    public void removeArtifact( RepositorySession session, final String repositoryId, final String namespace, final String project,
                                final String version, final String id )
        throws MetadataRepositoryException
    {
        logger.debug( "removeTimestampedArtifact repositoryId: '{}', namespace: '{}', project: '{}', version: '{}', id: '{}'",
                      repositoryId, namespace, project, version, id );
        String key =
            new ArtifactMetadataModel.KeyBuilder().withRepositoryId( repositoryId ).withNamespace( namespace ).withId(
                id ).withProjectVersion( version ).withProject( project ).build();

        this.artifactMetadataTemplate.deleteRow( key );

        key = new ProjectVersionMetadataModel.KeyBuilder() //
            .withRepository( repositoryId ) //
            .withNamespace( namespace ) //
            .withProjectId( project ) //
            .withProjectVersion( version ) //
            .withId( id ) //
            .build();

        this.projectVersionMetadataTemplate.deleteRow( key );
    }

    @Override
    public void removeTimestampedArtifact( RepositorySession session, ArtifactMetadata artifactMetadata, String baseVersion )
        throws MetadataRepositoryException
    {
        logger.debug( "removeTimestampedArtifact repositoryId: '{}', namespace: '{}', project: '{}', version: '{}', id: '{}'",
                      artifactMetadata.getRepositoryId(), artifactMetadata.getNamespace(),
                      artifactMetadata.getProject(), baseVersion, artifactMetadata.getId() );
        String key =
            new ArtifactMetadataModel.KeyBuilder().withRepositoryId( artifactMetadata.getRepositoryId() ).withNamespace(
                artifactMetadata.getNamespace() ).withId( artifactMetadata.getId() ).withProjectVersion(
                baseVersion ).withProject( artifactMetadata.getProject() ).build();

        this.artifactMetadataTemplate.deleteRow( key );

    }

    @Override
    public void removeFacetFromArtifact( RepositorySession session, final String repositoryId, final String namespace, final String project,
                                         final String version, final MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {

        RangeSlicesQuery<String, String, String> query = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getArtifactMetadataFamilyName() ) //
            .setColumnNames( NAMESPACE_ID.toString() ); //

        query = query.addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespace ) //
            .addEqualsExpression( PROJECT.toString(), project ) //
            .addEqualsExpression( VERSION.toString(), version );

        QueryResult<OrderedRows<String, String, String>> result = query.execute();

        for ( Row<String, String, String> row : result.get() )
        {
            this.artifactMetadataTemplate.deleteRow( row.getKey() );
        }
    }


    @Override
    public List<ArtifactMetadata> getArtifacts( RepositorySession session, final String repositoryId )
        throws MetadataRepositoryException
    {

        RangeSlicesQuery<String, String, String> query = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getArtifactMetadataFamilyName() ) //
            .setColumnNames( ArtifactMetadataModel.COLUMNS ); //

        query = query.addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId );

        QueryResult<OrderedRows<String, String, String>> result = query.execute();



        List<ArtifactMetadata> artifactMetadatas = new ArrayList<>( result.get().getCount() );

        for ( Row<String, String, String> row : result.get() )
        {
            String key = row.getKey();
            ColumnSlice<String, String> columnSlice = row.getColumnSlice();
            artifactMetadatas.add( mapArtifactMetadataStringColumnSlice( key, columnSlice ) );

        }

        return artifactMetadatas;
    }


    @Override
    public List<ProjectVersionReference> getProjectReferences( RepositorySession session, String repoId, String namespace, String projectId,
                                                               String projectVersion )
        throws MetadataResolutionException
    {
        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getDependencyFamilyName() ) //
            .setColumnNames( "projectVersionMetadataModel.key" ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .addEqualsExpression( GROUP_ID.toString(), namespace ) //
            .addEqualsExpression( ARTIFACT_ID.toString(), projectId ) //
            .addEqualsExpression( VERSION.toString(), projectVersion ) //
            .execute();

        List<String> dependenciesIds = new ArrayList<>( result.get().getCount() );

        for ( Row<String, String, String> row : result.get().getList() )
        {
            dependenciesIds.add( getStringValue( row.getColumnSlice(), "projectVersionMetadataModel.key" ) );
        }

        List<ProjectVersionReference> references = new ArrayList<>( result.get().getCount() );

        for ( String key : dependenciesIds )
        {
            ColumnFamilyResult<String, String> columnFamilyResult =
                this.projectVersionMetadataTemplate.queryColumns( key );
            references.add( new ProjectVersionReference( ProjectVersionReference.ReferenceType.DEPENDENCY, //
                                                         columnFamilyResult.getString( PROJECT_ID.toString() ), //
                                                         columnFamilyResult.getString( NAMESPACE_ID.toString() ), //
                                                         columnFamilyResult.getString( PROJECT_VERSION.toString() ) ) );
        }

        return references;
    }

    @Override
    public void removeProjectVersion( RepositorySession session, final String repoId, final String namespace, final String projectId,
                                      final String projectVersion )
        throws MetadataRepositoryException
    {

        QueryResult<OrderedRows<String, String, String>> result = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getProjectVersionMetadataFamilyName() ) //
            .setColumnNames( VERSION.toString() ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespace ) //
            .addEqualsExpression( PROJECT_ID.toString(), projectId ) //
            .addEqualsExpression( PROJECT_VERSION.toString(), projectVersion ) //
            .execute();

        for ( Row<String, String, String> row : result.get().getList() )
        {
            this.projectVersionMetadataTemplate.deleteRow( row.getKey() );
            removeMailingList( row.getKey() );
            removeLicenses( row.getKey() );
            removeDependencies( row.getKey() );
        }

        RangeSlicesQuery<String, String, String> query = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getArtifactMetadataFamilyName() ) //
            .setColumnNames( NAMESPACE_ID.toString() ); //

        query = query.addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespace ) //
            .addEqualsExpression( PROJECT.toString(), projectId ) //
            .addEqualsExpression( PROJECT_VERSION.toString(), projectVersion );

        result = query.execute();

        for ( Row<String, String, String> row : result.get() )
        {
            this.artifactMetadataTemplate.deleteRow( row.getKey() );

        }
    }

    @Override
    public List<ArtifactMetadata> getArtifacts( RepositorySession session, final String repoId, final String namespace,
                                                final String projectId, final String projectVersion )
        throws MetadataResolutionException
    {

        QueryResult<OrderedRows<String, String, String>> result =
            HFactory.createRangeSlicesQuery( keyspace, ss, ss, ss ) //
                .setColumnFamily( cassandraArchivaManager.getArtifactMetadataFamilyName() ) //
                .setColumnNames( ArtifactMetadataModel.COLUMNS )//
                .setRowCount( Integer.MAX_VALUE ) //
                .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
                .addEqualsExpression( NAMESPACE_ID.toString(), namespace ) //
                .addEqualsExpression( PROJECT.toString(), projectId ) //
                .addEqualsExpression( PROJECT_VERSION.toString(), projectVersion ) //
                .execute();

        if ( result.get() == null || result.get().getCount() < 1 )
        {
            return Collections.emptyList();
        }

        List<ArtifactMetadata> artifactMetadatas = new ArrayList<>( result.get().getCount() );

        for ( Row<String, String, String> row : result.get() )
        {
            String key = row.getKey();
            artifactMetadatas.add( mapArtifactMetadataStringColumnSlice( key, row.getColumnSlice() ) );
        }

        result = HFactory.createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getMetadataFacetFamilyName() ) //
            .setColumnNames( MetadataFacetModel.COLUMNS ) //
            .setRowCount( Integer.MAX_VALUE ) //
            .addEqualsExpression( REPOSITORY_NAME.toString(), repoId ) //
            .addEqualsExpression( NAMESPACE_ID.toString(), namespace ) //
            .addEqualsExpression( PROJECT_ID.toString(), projectId ) //
            .addEqualsExpression( PROJECT_VERSION.toString(), projectVersion ) //
            .execute();

        return mapArtifactFacetToArtifact(result, artifactMetadatas);
    }

    /**
     * Attach metadata to each of the  ArtifactMetadata objects
     */
    private List<ArtifactMetadata> mapArtifactFacetToArtifact( QueryResult<OrderedRows<String, String, String>> result, List<ArtifactMetadata> artifactMetadatas) {
        if ( result.get() == null || result.get().getCount() < 1 )
        {
            return artifactMetadatas;
        }

        final List<MetadataFacetModel> metadataFacetModels = new ArrayList<>( result.get().getCount() );

        for ( Row<String, String, String> row : result.get() )
        {
            ColumnSlice<String, String> columnSlice = row.getColumnSlice();
            MetadataFacetModel metadataFacetModel = new MetadataFacetModel();
            metadataFacetModel.setFacetId( getStringValue( columnSlice, FACET_ID.toString() ) );
            metadataFacetModel.setName( getStringValue( columnSlice, NAME.toString() ) );
            metadataFacetModel.setValue( getStringValue( columnSlice, VALUE.toString() ) );
            metadataFacetModel.setKey( getStringValue( columnSlice, KEY.toString() ) );
            metadataFacetModel.setProjectVersion( getStringValue( columnSlice, PROJECT_VERSION.toString() ) );
            metadataFacetModels.add( metadataFacetModel );
        }

        // rebuild MetadataFacet for artifacts

        for ( final ArtifactMetadata artifactMetadata : artifactMetadatas )
        {
            Iterable<MetadataFacetModel> metadataFacetModelIterable =
                Iterables.filter( metadataFacetModels, new Predicate<MetadataFacetModel>()
                {
                    @Override
                    public boolean apply( MetadataFacetModel metadataFacetModel )
                    {
                        if ( metadataFacetModel != null )
                        {
                            return StringUtils.equals( artifactMetadata.getVersion(),
                                                       metadataFacetModel.getProjectVersion() );
                        }
                        return false;
                    }
                } );
            Iterator<MetadataFacetModel> iterator = metadataFacetModelIterable.iterator();
            Map<String, List<MetadataFacetModel>> metadataFacetValuesPerFacetId = new HashMap<>();
            while ( iterator.hasNext() )
            {
                MetadataFacetModel metadataFacetModel = iterator.next();
                List<MetadataFacetModel> values = metadataFacetValuesPerFacetId.get( metadataFacetModel.getName() );
                if ( values == null )
                {
                    values = new ArrayList<>();
                    metadataFacetValuesPerFacetId.put( metadataFacetModel.getFacetId(), values );
                }
                values.add( metadataFacetModel );

            }

            for ( Map.Entry<String, List<MetadataFacetModel>> entry : metadataFacetValuesPerFacetId.entrySet() )
            {
                MetadataFacetFactory metadataFacetFactory = getFacetFactory( entry.getKey() );
                if ( metadataFacetFactory != null )
                {
                    List<MetadataFacetModel> facetModels = entry.getValue();
                    if ( !facetModels.isEmpty() )
                    {
                        MetadataFacet metadataFacet = metadataFacetFactory.createMetadataFacet();
                        Map<String, String> props = new HashMap<>( facetModels.size() );
                        for ( MetadataFacetModel metadataFacetModel : facetModels )
                        {
                            props.put( metadataFacetModel.getKey(), metadataFacetModel.getValue() );
                        }
                        metadataFacet.fromProperties( props );
                        artifactMetadata.addFacet( metadataFacet );
                    }
                }
            }
        }

        return artifactMetadatas;
    }

    @Override
    public void close()
        throws MetadataRepositoryException
    {
        logger.trace( "close" );
    }


    private static class ModelMapperHolder
    {
        private static ModelMapper MODEL_MAPPER = new ModelMapper();
    }

    protected ModelMapper getModelMapper()
    {
        return ModelMapperHolder.MODEL_MAPPER;
    }

    /**
     * This implementation just calls getArtifactsByAttribute( null, text, repositoryId ). We can't search artifacts by
     * any property.
     */
    @Override
    public List<ArtifactMetadata> searchArtifacts( final RepositorySession session, final String repositoryId,
                                                   final String text, final boolean exact )
        throws MetadataRepositoryException
    {
        return this.getArtifactsByAttribute( session, null, text, repositoryId );
    }

    /**
     * The exact parameter is ignored as we can't do non exact searches in Cassandra
     */
    @Override
    public List<ArtifactMetadata> searchArtifacts( final RepositorySession session, final String repositoryId,
                                                   final String key, final String text, final boolean exact )
        throws MetadataRepositoryException
    {
        // TODO optimize
        List<ArtifactMetadata> artifacts = new LinkedList<ArtifactMetadata>();
        artifacts.addAll( this.getArtifactsByAttribute( session, key, text, repositoryId ) );
        artifacts.addAll( this.getArtifactsByProjectVersionAttribute( session, key, text, repositoryId ) );
        return artifacts;
    }

    @Override
    public Stream<ArtifactMetadata> getArtifactStream( final RepositorySession session, final String repositoryId,
                                                       final QueryParameter queryParameter ) throws MetadataResolutionException
    {
        RangeSlicesQuery<String, String, String> query = HFactory //
            .createRangeSlicesQuery( keyspace, ss, ss, ss ) //
            .setColumnFamily( cassandraArchivaManager.getArtifactMetadataFamilyName( ) ) //
            .setColumnNames( ArtifactMetadataModel.COLUMNS ); //

        query = query.addEqualsExpression( REPOSITORY_NAME.toString(), repositoryId );

        QueryResult<OrderedRows<String, String, String>> result = query.execute();

        try
        {
            return StreamSupport.stream( createResultSpliterator( result, ( Row<String, String, String> row, ArtifactMetadata last ) ->
                mapArtifactMetadataStringColumnSlice( row.getKey( ), row.getColumnSlice( ) ) ), false )
                .skip( queryParameter.getOffset( ) ).limit( queryParameter.getLimit( ) );
        }
        catch ( MetadataRepositoryException e )
        {
            throw new MetadataResolutionException( e.getMessage( ), e );
        }
    }

    @Override
    public Stream<ArtifactMetadata> getArtifactStream( final RepositorySession session, final String repoId,
                                                       final String namespace, final String projectId, final String projectVersion,
                                                       final QueryParameter queryParameter ) throws MetadataResolutionException
    {
        // Currently we have to align the facets with the artifacts, which means querying artifacts, querying facets and combining them.
        // I so no stream friendly way to do this, so we just use the collection based method and return the stream.
        // TODO: Maybe we can query the facets for each artifact separately, but not sure, if this affects performance significantly
        //       We need some data to verify this.
        return getArtifacts( session, repoId, namespace, projectId, projectVersion ).stream( ).skip( queryParameter.getOffset( ) ).limit( queryParameter.getLimit( ) );
    }
}
