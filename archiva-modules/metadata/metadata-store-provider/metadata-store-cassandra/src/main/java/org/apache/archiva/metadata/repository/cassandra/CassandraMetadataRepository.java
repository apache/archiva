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
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.metadata.schema.ClusteringOrder;
import com.datastax.oss.driver.api.querybuilder.delete.Delete;
import com.datastax.oss.driver.api.querybuilder.insert.Insert;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.datastax.oss.driver.api.querybuilder.update.Update;
import com.datastax.oss.driver.api.querybuilder.update.UpdateStart;
import com.datastax.oss.driver.api.querybuilder.update.UpdateWithAssignments;
import org.apache.archiva.checksum.ChecksumAlgorithm;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Spliterator;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.*;
import static org.apache.archiva.metadata.model.ModelInfo.STORAGE_TZ;
import static org.apache.archiva.metadata.repository.cassandra.CassandraArchivaManager.DEFAULT_PRIMARY_KEY;
import static org.apache.archiva.metadata.repository.cassandra.model.ColumnNames.*;

/**
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class CassandraMetadataRepository
    extends AbstractMetadataRepository implements MetadataRepository
{

    private static final String ARTIFACT_METADATA_MODEL_KEY = "\"artifactMetadataModel.key\"";
    private Logger logger = LoggerFactory.getLogger( getClass( ) );

    private final CassandraArchivaManager cassandraArchivaManager;


    public CassandraMetadataRepository( MetadataService metadataService,
                                        CassandraArchivaManager cassandraArchivaManager )
    {
        super( metadataService );
        this.cassandraArchivaManager = cassandraArchivaManager;
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
        String cf = cassandraArchivaManager.getRepositoryFamilyName( );

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            Select query = selectFrom( cf ).column( REPOSITORY_NAME.toString( ) ).whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) ).allowFiltering();
            ResultSet qResult = session.execute( query.build( ) );
            Row row = qResult.one( );
            if ( row == null )
            {
                Repository repository = new Repository( repositoryId );
                RegularInsert insert = insertInto( cf )
                    .value( DEFAULT_PRIMARY_KEY, literal( repositoryId ) )
                    .value( REPOSITORY_NAME.toString( ), literal( repository.getName( ) ) );
                session.execute( insert.build( ) );
                return repository;
            }
            return new Repository( row.get( REPOSITORY_NAME.toString( ), String.class ) );

        }

    }


    protected Repository getRepository( String repositoryId )
        throws MetadataRepositoryException
    {

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            Select query = selectFrom( cassandraArchivaManager.getRepositoryFamilyName( ) ).column( REPOSITORY_NAME.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .allowFiltering();
            Row row = session.execute( query.build( ) ).one( );
            return row != null ? new Repository( repositoryId ) : null;
        }
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
        Repository repository = getOrCreateRepository( repositoryId );

        String key =
            new Namespace.KeyBuilder( ).withNamespace( namespaceId ).withRepositoryId( repositoryId ).build( );

        Namespace namespace = getNamespace( repositoryId, namespaceId );
        if ( namespace == null )
        {
            String cf = cassandraArchivaManager.getNamespaceFamilyName( );
            namespace = new Namespace( namespaceId, repository );

            CqlSession session = cassandraArchivaManager.getSession( );
            {
                RegularInsert insert = insertInto( cf )
                    .value( DEFAULT_PRIMARY_KEY, literal( key ) )
                    .value( NAME.toString( ), literal( namespace.getName( ) ) )
                    .value( REPOSITORY_NAME.toString( ), literal( repository.getName( ) ) );
                session.execute( insert.build( ) );
            }

        }

        return namespace;
    }

    protected Namespace getNamespace( String repositoryId, String namespaceId )
    {

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getNamespaceFamilyName( );
            String key =
                new Namespace.KeyBuilder( ).withNamespace( namespaceId ).withRepositoryId( repositoryId ).build( );
            Select query = selectFrom( table )
                .columns( REPOSITORY_NAME.toString( ), NAME.toString( ) )
                .whereColumn(  DEFAULT_PRIMARY_KEY ).isEqualTo(  literal( key ) );
            Row row = session.execute( query.build( ) ).one( );
            if ( row != null )
            {
                return new Namespace( row.get( NAME.toString( ), String.class ),
                    new Repository( row.get( REPOSITORY_NAME.toString( ), String.class ) ) );
            }
            return null;
        }
    }


    @Override
    public void removeNamespace( RepositorySession repositorySession, String repositoryId, String namespaceId )
        throws MetadataRepositoryException
    {

        String key = new Namespace.KeyBuilder( ) //
            .withNamespace( namespaceId ) //
            .withRepositoryId( repositoryId ) //
            .build( );


        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String pTable = cassandraArchivaManager.getNamespaceFamilyName( );
            Delete delete = deleteFrom( pTable ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) );
            session.execute( delete.build( ) );

            List<String> tables = Arrays.asList(
                cassandraArchivaManager.getProjectFamilyName( ),
                cassandraArchivaManager.getProjectVersionMetadataFamilyName( ),
                cassandraArchivaManager.getArtifactMetadataFamilyName( ),
                cassandraArchivaManager.getMetadataFacetFamilyName( ) );

            for ( String table : tables )
            {
                Select deleteRows = selectFrom( table )
                    .column( DEFAULT_PRIMARY_KEY )
                    .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                    .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespaceId ) )
                    .allowFiltering();
                ResultSet result = session.execute( deleteRows.build( ) );
                StreamSupport.stream( result.spliterator( ), false ).map( row -> row.getString( DEFAULT_PRIMARY_KEY ) )
                    .distinct( ).forEach( delKey ->
                        session.execute( deleteFrom( table ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( delKey ) ).build( ) ) );
            }

        }
    }


    @Override
    public void removeRepository( RepositorySession repositorySession, final String repositoryId )
        throws MetadataRepositoryException
    {

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            final String table = cassandraArchivaManager.getNamespaceFamilyName( );
            Select deleteRows = selectFrom( table )
                .column( DEFAULT_PRIMARY_KEY )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) );
            ResultSet result = session.execute( deleteRows.build( ) );
            StreamSupport.stream( result.spliterator( ), false )
                .map( row -> row.getString( DEFAULT_PRIMARY_KEY ) )
                .distinct( )
                .forEach(
                    delKey ->
                        session.execute( deleteFrom( table ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( delKey ) ).build( ) )
                );

            String deleteTable = cassandraArchivaManager.getRepositoryFamilyName( );
            Delete delete = deleteFrom( deleteTable ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( repositoryId ) );
            session.execute( delete.build( ) );

            List<String> tables = Arrays.asList(
                cassandraArchivaManager.getProjectFamilyName( ),
                cassandraArchivaManager.getProjectVersionMetadataFamilyName( ),
                cassandraArchivaManager.getArtifactMetadataFamilyName( ),
                cassandraArchivaManager.getMetadataFacetFamilyName( )
            );

            for ( String dTable : tables )
            {
                deleteRows = selectFrom( dTable )
                    .column( DEFAULT_PRIMARY_KEY )
                    .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                    .allowFiltering();
                result = session.execute( deleteRows.build( ) );
                StreamSupport.stream( result.spliterator(), false )
                    .map(row -> row.getString( DEFAULT_PRIMARY_KEY ))
                    .distinct()
                    .forEach( delKey ->
                        session.execute( deleteFrom( dTable ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal(delKey) ).build(  ) ));
            }

        }

    }

    @Override
    public List<String> getRootNamespaces( RepositorySession repositorySession, final String repoId )
        throws MetadataResolutionException
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getNamespaceFamilyName( );
            Select query = selectFrom( table ).column( NAME.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repoId ) );
            return StreamSupport.stream( session.execute( query.build( ) ).spliterator( ), false )
                .map( row ->
                    StringUtils.substringBefore( row.get( NAME.toString( ), String.class ), "." ) )
                .distinct( )
                .collect( Collectors.toList( ) );
        }
    }

    // FIXME this one need peformance improvement maybe a cache?
    @Override
    public List<String> getChildNamespaces( RepositorySession repositorySession, final String repoId, final String namespaceId )
        throws MetadataResolutionException
    {
        final String calledNs = namespaceId.endsWith( "." ) ? namespaceId : namespaceId + ".";
        final int nslen = calledNs.length( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getNamespaceFamilyName( );
            Select query = selectFrom( table ).column( NAME.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repoId ) );
            return StreamSupport.stream( session.execute( query.build( ) ).spliterator( ), false )
                .map( row -> row.get( NAME.toString( ), String.class ) )
                .filter( namespace -> namespace.length( ) > nslen && namespace.startsWith( calledNs ) )
                .map( namespace -> StringUtils.substringBefore( StringUtils.substringAfter( namespace, calledNs ), "." ) )
                .distinct( )
                .collect( Collectors.toList( ) );
        }
    }

    // only use for testing purpose
    protected List<String> getNamespaces( final String repoId )
        throws MetadataResolutionException
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getNamespaceFamilyName( );
            Select query = selectFrom( table ).column( NAME.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repoId ) );
            return StreamSupport.stream( session.execute( query.build( ) ).spliterator( ), false )
                .map( row ->
                    row.get( NAME.toString( ), String.class ) )
                .distinct( )
                .collect( Collectors.toList( ) );
        }
    }


    @Override
    public void updateProject( RepositorySession repositorySession, String repositoryId, ProjectMetadata projectMetadata )
        throws MetadataRepositoryException
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getProjectFamilyName( );
            Select query = selectFrom( table ).column( PROJECT_ID.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( projectMetadata.getNamespace( ) ) )
                .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectMetadata.getId( ) ) ).allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            if ( result.one( ) == null )
            {
                Namespace namespace = updateOrAddNamespace( repositoryId, projectMetadata.getNamespace( ) );
                String key =
                    new Project.KeyBuilder( ).withProjectId( projectMetadata.getId( ) ).withNamespace( namespace ).build( );
                RegularInsert insert = insertInto( table )
                    .value( DEFAULT_PRIMARY_KEY, literal( key ) )
                    .value( PROJECT_ID.toString( ), literal( projectMetadata.getId( ) ) )
                    .value( REPOSITORY_NAME.toString( ), literal( repositoryId ) )
                    .value( NAMESPACE_ID.toString( ), literal( projectMetadata.getNamespace( ) ) );
                session.execute( insert.build( ) );
            }
            if ( projectMetadata.hasProperties( ) )
            {
                UpdateStart update = update( table );
                UpdateWithAssignments newUpdat = null;
                final Properties props = projectMetadata.getProperties( );
                for ( String propKey : props.stringPropertyNames( ) )
                {
                    newUpdat = update.setMapValue( PROJECT_PROPERTIES.toString( ), literal( propKey ), literal( props.getProperty( propKey, "" ) ) );
                }
                Update finalUpdate = newUpdat
                    .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                    .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( projectMetadata.getNamespace( ) ) )
                    .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectMetadata.getId( ) ) );
                session.execute( finalUpdate.build( ) );
            }


        }

    }

    @Override
    public List<String> getProjects( RepositorySession repositorySession, final String repoId, final String namespace )
        throws MetadataResolutionException
    {

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getProjectFamilyName( );
            Select query = selectFrom( table ).column( PROJECT_ID.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repoId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespace ) )
                .allowFiltering();
            return StreamSupport.stream( session.execute( query.build( ) ).spliterator( ), false )
                .map( row ->
                    row.get( PROJECT_ID.toString( ), String.class ) )
                .distinct( )
                .collect( Collectors.toList( ) );
        }

    }

    @Override
    public void removeProject( RepositorySession repositorySession, final String repositoryId, final String namespaceId, final String projectId )
        throws MetadataRepositoryException
    {

        String key = new Project.KeyBuilder( ) //
            .withProjectId( projectId ) //
            .withNamespace( new Namespace( namespaceId, new Repository( repositoryId ) ) ) //
            .build( );

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getProjectFamilyName( );
            Delete delete = deleteFrom( table ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) );
            session.execute( delete.build( ) );

            table = cassandraArchivaManager.getProjectVersionMetadataFamilyName( );
            Select query = selectFrom( table ).columns( DEFAULT_PRIMARY_KEY, PROJECT_ID.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespaceId ) )
                .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectId ) )
                .allowFiltering();

            ResultSet result = session.execute( query.build( ) );
            result.forEach( row -> removeMailingList( row.get( DEFAULT_PRIMARY_KEY, String.class ) ) );


            List<String> tables = Arrays.asList(
                cassandraArchivaManager.getProjectVersionMetadataFamilyName( ),
                cassandraArchivaManager.getArtifactMetadataFamilyName( )
            );

            for ( String dTable : tables )
            {
                Select deleteRows = selectFrom( dTable ).column( DEFAULT_PRIMARY_KEY )
                    .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                    .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespaceId ) )
                    .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectId ) )
                    .allowFiltering();
                result = session.execute( deleteRows.build( ) );
                StreamSupport.stream( result.spliterator( ), false )
                    .map( row -> row.getString( DEFAULT_PRIMARY_KEY ) )
                    .forEach( delKey -> session.execute( deleteFrom( dTable ).column( PROJECT_ID.toString( ) ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( delKey ) ).build( ) ) );
            }
        }

    }

    @Override
    public List<String> getProjectVersions( RepositorySession repositorySession, final String repositoryId, final String namespaceId, final String projectId )
        throws MetadataResolutionException
    {

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getProjectVersionMetadataFamilyName( );
            Select query = selectFrom( table ).column( PROJECT_ID.toString( ) )
                .column( PROJECT_VERSION.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespaceId ) )
                .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectId ) )
                .allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            return StreamSupport.stream( result.spliterator( ), false )
                .map( row -> row.get( PROJECT_VERSION.toString( ), String.class ) )
                .distinct( )
                .collect( Collectors.toList( ) );
        }
    }

    @Override
    public ProjectMetadata getProject( RepositorySession repositorySession, final String repositoryId, final String namespaceId, final String id )
        throws MetadataResolutionException
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getProjectFamilyName( );
            Select query = selectFrom( table ).column( PROJECT_ID.toString( ) )
                .column( PROJECT_ID.toString( ) )
                .column( PROJECT_PROPERTIES.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespaceId ) )
                .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( id ) ).allowFiltering();
            Row result = session.execute( query.build( ) ).one( );
            if ( result == null )
            {
                return null;
            }
            else
            {
                ProjectMetadata projectMetadata = new ProjectMetadata( );
                projectMetadata.setId( id );
                projectMetadata.setNamespace( namespaceId );
                Map<String, String> props = result.getMap( PROJECT_PROPERTIES.toString( ), String.class, String.class );
                Properties pProps = new Properties( );
                if ( props != null )
                {
                    pProps.putAll( props );
                }
                projectMetadata.setProperties( pProps );
                return projectMetadata;
            }
        }
    }

    protected ProjectVersionMetadataModel mapProjectVersionMetadataModel( Row row )
    {
        ProjectVersionMetadataModel projectVersionMetadataModel = new ProjectVersionMetadataModel( );
        projectVersionMetadataModel.setId( row.get( VERSION.toString( ), String.class ) );
        projectVersionMetadataModel.setDescription( row.get( DESCRIPTION.toString( ), String.class ) );
        projectVersionMetadataModel.setName( row.get( NAME.toString( ), String.class ) );
        Namespace namespace = new Namespace( row.get( NAMESPACE_ID.toString( ), String.class ), //
            new Repository( row.get( REPOSITORY_NAME.toString( ), String.class ) ) );
        projectVersionMetadataModel.setNamespace( namespace );
        projectVersionMetadataModel.setIncomplete( row.getBoolean( "incomplete" ) );
        projectVersionMetadataModel.setProjectId( row.get( PROJECT_ID.toString( ), String.class ) );
        projectVersionMetadataModel.setUrl( row.get( URL.toString( ), String.class ) );
        return projectVersionMetadataModel;
    }

    protected UpdateWithAssignments addUpdate( UpdateWithAssignments update, String column, Object value )
    {
        return update.setColumn( column, literal( value ) );
    }

    @Override
    public void updateProjectVersion( RepositorySession repositorySession, String repositoryId, String namespaceId, String projectId,
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

            if ( getProject( repositorySession, repositoryId, namespaceId, projectId ) == null )
            {
                ProjectMetadata projectMetadata = new ProjectMetadata( );
                projectMetadata.setNamespace( namespaceId );
                projectMetadata.setId( projectId );
                updateProject( repositorySession, repositoryId, projectMetadata );
            }

        }
        catch ( MetadataResolutionException e )
        {
            throw new MetadataRepositoryException( e.getMessage( ), e );
        }

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getProjectVersionMetadataFamilyName( );
            Select query = selectFrom( table ).column( PROJECT_ID.toString( ) )
                .all( )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespaceId ) )
                .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectId ) )
                .whereColumn( PROJECT_VERSION.toString( ) ).isEqualTo( literal( versionMetadata.getId( ) ) ).allowFiltering();
            ProjectVersionMetadataModel projectVersionMetadataModel;
            boolean create = true;
            Row result = session.execute( query.build( ) ).one( );
            if ( result != null )
            {
                projectVersionMetadataModel = mapProjectVersionMetadataModel( result );
                create = false;
            }
            else
            {
                projectVersionMetadataModel = getModelMapper( ).map( versionMetadata, ProjectVersionMetadataModel.class );
            }
            projectVersionMetadataModel.setProjectId( projectId );
            projectVersionMetadataModel.setNamespace( new Namespace( namespaceId, new Repository( repositoryId ) ) );

            projectVersionMetadataModel.setCiManagement( versionMetadata.getCiManagement( ) );
            projectVersionMetadataModel.setIssueManagement( versionMetadata.getIssueManagement( ) );
            projectVersionMetadataModel.setOrganization( versionMetadata.getOrganization( ) );
            projectVersionMetadataModel.setScm( versionMetadata.getScm( ) );

            projectVersionMetadataModel.setMailingLists( versionMetadata.getMailingLists( ) );
            projectVersionMetadataModel.setDependencies( versionMetadata.getDependencies( ) );
            projectVersionMetadataModel.setLicenses( versionMetadata.getLicenses( ) );

            // we don't test, if repository and namespace really exist !
            String key = new ProjectVersionMetadataModel.KeyBuilder( ) //
                .withRepository( repositoryId ) //
                .withNamespace( namespaceId ) //
                .withProjectId( projectId ) //
                .withProjectVersion( versionMetadata.getVersion( ) ) //
                .withId( versionMetadata.getId( ) ) //
                .build( );

            // Update is upsert
            table = cassandraArchivaManager.getProjectVersionMetadataFamilyName( );
            UpdateWithAssignments update = update( table )
                .setColumn( PROJECT_ID.toString( ), literal( projectId ) )
                .setColumn( REPOSITORY_NAME.toString( ), literal( repositoryId ) )
                .setColumn( NAMESPACE_ID.toString( ), literal( namespaceId ) )
                .setColumn( PROJECT_VERSION.toString( ), literal( versionMetadata.getVersion( ) ) )
                .setColumn( DESCRIPTION.toString( ), literal( versionMetadata.getDescription( ) ) )
                .setColumn( NAME.toString( ), literal( versionMetadata.getName( ) ) )
                .setColumn( "incomplete", literal( versionMetadata.isIncomplete( ) ) )
                .setColumn( URL.toString( ), literal( versionMetadata.getUrl( ) ) );


            {
                CiManagement ci = versionMetadata.getCiManagement( );
                if ( ci != null )
                {
                    update = update.setColumn( "\"ciManagement.system\"", literal( ci.getSystem( ) ) )
                        .setColumn( "\"ciManagement.url\"", literal( ci.getUrl( ) ) );
                }
            }

            {
                IssueManagement issueManagement = versionMetadata.getIssueManagement( );

                if ( issueManagement != null )
                {
                    update = update.setColumn( "\"issueManagement.system\"", literal( issueManagement.getSystem( ) ) )
                        .setColumn( "\"issueManagement.url\"", literal( issueManagement.getUrl( ) ) );
                }
            }

            {
                Organization organization = versionMetadata.getOrganization( );
                if ( organization != null )
                {
                    update = update.setColumn( "\"organization.name\"", literal( organization.getName( ) ) )
                        .setColumn( "\"organization.url\"", literal( organization.getUrl( ) ) );
                }
            }

            {
                Scm scm = versionMetadata.getScm( );
                if ( scm != null )
                {
                    update = update.setColumn( "\"scm.url\"", literal( scm.getUrl( ) ) )
                        .setColumn( "\"scm.connection\"", literal( scm.getConnection( ) ) )
                        .setColumn( "\"scm.developerConnection\"", literal( scm.getDeveloperConnection( ) ) );
                }
            }
            if (versionMetadata.getProperties()!=null && versionMetadata.getProperties().size()>0) {
                for( Map.Entry<String, String> entry : versionMetadata.getProperties().entrySet()) {
                    update = update.setMapValue( VERSION_PROPERTIES.toString( ), literal( entry.getKey( ) ), literal( entry.getValue( ) ) );
                }
            }

            Update finalUpdate = update.whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) );
            session.execute( finalUpdate.build( ) );


            if ( !create )
            {
                removeMailingList( key );
                removeLicenses( key );
                removeDependencies( key );
            }
            recordMailingList( key, versionMetadata.getMailingLists( ) );
            recordLicenses( key, versionMetadata.getLicenses( ) );
            recordDependencies( key, versionMetadata.getDependencies( ), repositoryId );

            ArtifactMetadataModel artifactMetadataModel = new ArtifactMetadataModel( );
            artifactMetadataModel.setRepositoryId( repositoryId );
            artifactMetadataModel.setNamespace( namespaceId );
            artifactMetadataModel.setProject( projectId );
            artifactMetadataModel.setProjectVersion( versionMetadata.getVersion( ) );
            artifactMetadataModel.setVersion( versionMetadata.getVersion( ) );
            updateFacets( versionMetadata, artifactMetadataModel );


        }

    }


    @Override
    public ProjectVersionMetadata getProjectVersion( RepositorySession repositorySession, final String repositoryId, final String namespaceId,
                                                     final String projectId, final String projectVersion )
        throws MetadataResolutionException
    {

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getProjectVersionMetadataFamilyName( );
            Select query = selectFrom( table ).column( PROJECT_ID.toString( ) )
                .all( )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespaceId ) )
                .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectId ) )
                .whereColumn( PROJECT_VERSION.toString( ) ).isEqualTo( literal( projectVersion ) )
                .allowFiltering();
            Row result = session.execute( query.build( ) ).one( );
            if ( result == null )
            {
                return null;
            }
            String key = result.getString( DEFAULT_PRIMARY_KEY );
            ProjectVersionMetadata projectVersionMetadata = new ProjectVersionMetadata( );
            projectVersionMetadata.setId( result.getString( PROJECT_VERSION.toString( ) ) );
            projectVersionMetadata.setDescription( result.getString( DESCRIPTION.toString( ) ) );
            projectVersionMetadata.setName( result.getString( NAME.toString( ) ) );

            projectVersionMetadata.setIncomplete( result.getBoolean( "incomplete" ) ) ;

            projectVersionMetadata.setUrl( result.getString( URL.toString( ) ) );
            {
                String ciUrl = result.getString( "\"ciManagement.url\"" );
                String ciSystem = result.getString( "\"ciManagement.system\"" );

                if ( StringUtils.isNotEmpty( ciSystem ) || StringUtils.isNotEmpty( ciUrl ) )
                {
                    projectVersionMetadata.setCiManagement( new CiManagement( ciSystem, ciUrl ) );
                }
            }
            {
                String issueUrl = result.getString( "\"issueManagement.url\"" );
                String issueSystem = result.getString( "\"issueManagement.system\"" );
                if ( StringUtils.isNotEmpty( issueSystem ) || StringUtils.isNotEmpty( issueUrl ) )
                {
                    projectVersionMetadata.setIssueManagement( new IssueManagement( issueSystem, issueUrl ) );
                }
            }
            {
                String organizationUrl = result.getString( "\"organization.url\"" );
                String organizationName = result.getString( "\"organization.name\"" );
                if ( StringUtils.isNotEmpty( organizationUrl ) || StringUtils.isNotEmpty( organizationName ) )
                {
                    projectVersionMetadata.setOrganization( new Organization( organizationName, organizationUrl ) );
                }
            }
            {
                String devConn = result.getString( "\"scm.developerConnection\"" );
                String conn = result.getString( "\"scm.connection\"" );
                String url = result.getString( "\"scm.url\"" );
                if ( StringUtils.isNotEmpty( devConn ) || StringUtils.isNotEmpty( conn ) || StringUtils.isNotEmpty( url ) )
                {
                    projectVersionMetadata.setScm( new Scm( conn, devConn, url ) );
                }
            }
            projectVersionMetadata.setMailingLists( getMailingLists( key ) );
            projectVersionMetadata.setLicenses( getLicenses( key ) );
            projectVersionMetadata.setDependencies( getDependencies( key ) );


            // Facets
            table = cassandraArchivaManager.getMetadataFacetFamilyName( );
            query = selectFrom( table ).column( PROJECT_ID.toString( ) )
                .column( FACET_ID.toString( ) )
                .column( KEY.toString( ) )
                .column( VALUE.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespaceId ) )
                .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectId ) )
                .whereColumn( PROJECT_VERSION.toString( ) ).isEqualTo( literal( projectVersion ) )
                .allowFiltering();
            ResultSet rows = session.execute( query.build( ) );
            Map<String, Map<String, String>> metadataFacetsPerFacetIds = StreamSupport.stream( rows.spliterator( ), false )
                .collect(
                    Collectors.groupingBy(
                        row -> row.getString( FACET_ID.toString( ) ),
                        Collectors.toMap(
                            row -> row.getString( KEY.toString( ) ),
                            row -> row.getString( VALUE.toString( ) )
                        )
                    )
                );
            if ( !metadataFacetsPerFacetIds.isEmpty( ) )
            {
                for ( Map.Entry<String, Map<String, String>> entry : metadataFacetsPerFacetIds.entrySet( ) )
                {
                    MetadataFacetFactory<?> metadataFacetFactory = getFacetFactory( entry.getKey( ) );
                    if ( metadataFacetFactory != null )
                    {
                        MetadataFacet metadataFacet = metadataFacetFactory.createMetadataFacet( );
                        metadataFacet.fromProperties( entry.getValue( ) );
                        projectVersionMetadata.addFacet( metadataFacet );
                    }
                }
            }

            return projectVersionMetadata;
        }
    }

    protected void recordChecksums( String repositoryId, String artifactMetadataKey, Map<String, String> checksums )
    {
        if ( checksums == null || checksums.isEmpty( ) )
        {
            return;
        }

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getChecksumFamilyName( );
            for ( Map.Entry<String, String> entry : checksums.entrySet( ) )
            {
                String key = getChecksumKey( artifactMetadataKey, entry.getKey( ));
                RegularInsert insert = insertInto( table )
                    .value(DEFAULT_PRIMARY_KEY, literal(key))
                    .value( ARTIFACT_METADATA_MODEL_KEY, literal( artifactMetadataKey ) )
                    .value( CHECKSUM_ALG.toString( ), literal( entry.getKey( ) ) )
                    .value( CHECKSUM_VALUE.toString( ), literal( entry.getValue( ) ) )
                    .value( REPOSITORY_NAME.toString( ), literal( repositoryId ) );
                session.execute( insert.build( ) );

            }
        }
    }

    private String getChecksumKey(String metadataKey, String checksumAlg) {
        return metadataKey + "." + checksumAlg;
    }

    protected void removeChecksums( String artifactMetadataKey )
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getChecksumFamilyName( );
            Select deleteRows = selectFrom( table )
                .column( DEFAULT_PRIMARY_KEY )
                .whereColumn( ARTIFACT_METADATA_MODEL_KEY ).isEqualTo( literal( artifactMetadataKey ) )
                .allowFiltering();
            ResultSet result = session.execute( deleteRows.build( ) );
            StreamSupport.stream( result.spliterator(), false )
                .map(row -> row.getString( DEFAULT_PRIMARY_KEY ))
                .distinct()
                .forEach(
                    delKey -> session.execute( deleteFrom( table ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal(delKey) ).build(  ) )
                );
        }
    }

    protected Map<String, String> getChecksums( String artifactMetadataKey )
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getChecksumFamilyName( );
            Select query = selectFrom( table )
                .all( )
                .whereColumn( ARTIFACT_METADATA_MODEL_KEY ).isEqualTo( literal( artifactMetadataKey ) )
                .allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            return StreamSupport.stream( result.spliterator( ), false )
                .collect(
                    Collectors.toMap(
                        row -> row.getString( CHECKSUM_ALG.toString( ) ),
                        row -> row.getString( CHECKSUM_VALUE.toString( ) )
                    )
                );
        }
    }

    protected void recordMailingList( String projectVersionMetadataKey, List<MailingList> mailingLists )
    {
        if ( mailingLists == null || mailingLists.isEmpty( ) )
        {
            return;
        }
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getMailingListFamilyName( );
            for ( MailingList mailingList : mailingLists )
            {
                // we don't care about the key as the real used one with the projectVersionMetadata
                String keyMailingList = UUID.randomUUID( ).toString( );
                RegularInsert insert = insertInto( table )
                    .value( DEFAULT_PRIMARY_KEY, literal( keyMailingList ) );
                insert = insert.value( "\"projectVersionMetadataModel.key\"", literal( projectVersionMetadataKey ) )
                    .value( NAME.toString( ), literal( mailingList.getName( ) ) )
                    .value( "mainArchiveUrl", literal( mailingList.getMainArchiveUrl( ) ) )
                    .value( "postAddress", literal( mailingList.getPostAddress( ) ) )
                    .value( "subscribeAddress", literal( mailingList.getSubscribeAddress( ) ) )
                    .value( "unsubscribeAddress", literal( mailingList.getUnsubscribeAddress( ) ) )
                    .value( "otherArchive", literal( mailingList.getOtherArchives( ) ) );
                session.execute( insert.build( ) );
            }
        }
    }

    protected void removeMailingList( String projectVersionMetadataKey )
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getMailingListFamilyName( );
            Select deleteRows = selectFrom( table )
                .column( DEFAULT_PRIMARY_KEY )
                .whereColumn( "\"projectVersionMetadataModel.key\"" ).isEqualTo( literal( projectVersionMetadataKey ) );
            ResultSet result = session.execute( deleteRows.build( ) );
            StreamSupport.stream( result.spliterator( ), false )
                .map( row -> row.getString( DEFAULT_PRIMARY_KEY ) )
                .distinct( )
                .forEach(
                    delKey ->
                        session.execute( deleteFrom( table ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( delKey ) ).build( ) )
                );
        }
    }

    protected MailingList getMailingList( Row row )
    {
        MailingList mailingList = new MailingList( );
        mailingList.setName( row.getString( NAME.toString( ) ) );
        mailingList.setMainArchiveUrl( row.getString( "mainArchiveUrl" ) );
        mailingList.setPostAddress( row.getString( "postAddress" ) );
        mailingList.setSubscribeAddress( row.getString( "subscribeAddress" ) );
        mailingList.setUnsubscribeAddress( row.getString( "unsubscribeAddress" ) );
        mailingList.setOtherArchives( row.getList( "otherArchive", String.class ) );
        return mailingList;
    }

    protected List<MailingList> getMailingLists( String projectVersionMetadataKey )
    {
        List<MailingList> mailingLists = new ArrayList<>( );

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getMailingListFamilyName( );
            Select query = selectFrom( table )
                .all( )
                .whereColumn( "\"projectVersionMetadataModel.key\"" ).isEqualTo( literal( projectVersionMetadataKey ) );
            ResultSet result = session.execute( query.build( ) );
            return StreamSupport.stream( result.spliterator( ), false )
                .map( this::getMailingList )
                .collect( Collectors.toList( ) );
        }
    }

    protected void recordLicenses( String projectVersionMetadataKey, List<License> licenses )
    {

        if ( licenses == null || licenses.isEmpty( ) )
        {
            return;
        }
        String table = cassandraArchivaManager.getLicenseFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {

            for ( License license : licenses )
            {
                // we don't care about the key as the real used one with the projectVersionMetadata
                String keyLicense = UUID.randomUUID( ).toString( );
                RegularInsert insert = insertInto( table )
                    .value( DEFAULT_PRIMARY_KEY, literal( keyLicense ) )
                    .value( "\"projectVersionMetadataModel.key\"", literal( projectVersionMetadataKey ) )
                    .value( NAME.toString( ), literal( license.getName( ) ) )
                    .value( URL.toString( ), literal( license.getUrl( ) ) );
                session.execute( insert.build( ) );

            }
        }
    }

    protected void removeLicenses( String projectVersionMetadataKey )
    {
        String table = cassandraArchivaManager.getLicenseFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            Select deleteRows = selectFrom( table )
                .column( DEFAULT_PRIMARY_KEY )
                .whereColumn( "\"projectVersionMetadataModel.key\"" ).isEqualTo( literal( projectVersionMetadataKey ) )
                .allowFiltering();
            ResultSet result = session.execute( deleteRows.build( ) );
            StreamSupport.stream( result.spliterator( ), false )
                .map( row -> row.getString( DEFAULT_PRIMARY_KEY ) )
                .distinct( )
                .forEach(
                    delKey ->
                        session.execute( deleteFrom( table ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( delKey ) ).build( ) )
                );

        }
    }

    protected List<License> getLicenses( String projectVersionMetadataKey )
    {
        String table = cassandraArchivaManager.getLicenseFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            Select query = selectFrom( table )
                .column( NAME.toString( ) )
                .column( URL.toString( ) )
                .whereColumn( "\"projectVersionMetadataModel.key\"" ).isEqualTo( literal( projectVersionMetadataKey ) )
                .allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            return StreamSupport.stream( result.spliterator( ), false )
                .map(
                    row ->
                        new License( row.getString( NAME.toString( ) ), row.getString( URL.toString( ) ) )
                )
                .collect( Collectors.toList( ) );
        }

    }


    protected void recordDependencies( String projectVersionMetadataKey, List<Dependency> dependencies,
                                       String repositoryId )
    {

        if ( dependencies == null || dependencies.isEmpty( ) )
        {
            return;
        }
        String table = cassandraArchivaManager.getDependencyFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            for ( Dependency dependency : dependencies )
            {
                // we don't care about the key as the real used one with the projectVersionMetadata
                String keyDependency = UUID.randomUUID( ).toString( );
                RegularInsert insert = insertInto( table )
                    .value( DEFAULT_PRIMARY_KEY, literal( keyDependency ) )
                    .value( "\"projectVersionMetadataModel.key\"", literal( projectVersionMetadataKey ) )
                    .value( REPOSITORY_NAME.toString( ), literal( repositoryId ) )
                    .value( "classifier", literal( dependency.getClassifier( ) ) )
                    .value( "optional", literal( Boolean.toString( dependency.isOptional( ) ) ) )
                    .value( "scope", literal( dependency.getScope( ) ) )
                    .value( "systemPath", literal( dependency.getSystemPath( ) ) )
                    .value( "type", literal( dependency.getType( ) ) )
                    .value( ARTIFACT_ID.toString( ), literal( dependency.getArtifactId( ) ) )
                    .value( GROUP_ID.toString( ), literal( dependency.getNamespace( ) ) )
                    .value( VERSION.toString( ), literal( dependency.getVersion( ) ) );
                session.execute( insert.build( ) );
            }
        }
    }

    protected void removeDependencies( String projectVersionMetadataKey )
    {

        String table = cassandraArchivaManager.getDependencyFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            Select deleteRows = selectFrom( table )
                .column( DEFAULT_PRIMARY_KEY )
                .whereColumn( "\"projectVersionMetadataModel.key\"" ).isEqualTo( literal( projectVersionMetadataKey ) )
                .allowFiltering();
            ResultSet result = session.execute( deleteRows.build( ) );
            StreamSupport.stream( result.spliterator( ), false )
                .map( row -> row.getString( DEFAULT_PRIMARY_KEY ) )
                .distinct( )
                .forEach(
                    delKey ->
                        session.execute( deleteFrom( table ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( delKey ) ).build( ) )
                );

        }
    }

    protected Dependency newDependency( Row row )
    {
        Dependency dependency = new Dependency( );
        dependency.setClassifier( row.getString( "classifier" ) );

        dependency.setOptional( Boolean.parseBoolean( row.getString( "optional" ) ) );

        dependency.setScope( row.getString( "scope" ) );

        dependency.setSystemPath( row.getString( "systemPath" ) );

        dependency.setType( row.getString( "type" ) );

        dependency.setArtifactId( row.getString( ARTIFACT_ID.toString( ) ) );

        dependency.setNamespace( row.getString( GROUP_ID.toString( ) ) );

        dependency.setVersion( row.getString( VERSION.toString( ) ) );

        return dependency;
    }

    protected List<Dependency> getDependencies( String projectVersionMetadataKey )
    {

        String table = cassandraArchivaManager.getDependencyFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            Select query = selectFrom( table )
                .all( )
                .whereColumn( "\"projectVersionMetadataModel.key\"" ).isEqualTo( literal( projectVersionMetadataKey ) )
                .allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            return StreamSupport.stream( result.spliterator( ), false )
                .map( this::newDependency )
                .collect( Collectors.toList( ) );
        }
    }

    private Map<String, String> mapChecksums( Map<ChecksumAlgorithm, String> checksums )
    {
        return checksums.entrySet( ).stream( ).collect( Collectors.toMap(
            e -> e.getKey( ).name( ), Map.Entry::getValue
        ) );
    }

    private Map<ChecksumAlgorithm, String> mapChecksumsReverse( Map<String, String> checksums )
    {
        return checksums.entrySet( ).stream( ).collect( Collectors.toMap(
            e -> ChecksumAlgorithm.valueOf( e.getKey( ) ), Map.Entry::getValue
        ) );
    }

    @Override
    public void updateArtifact( RepositorySession repositorySession, String repositoryId, String namespaceId, String projectId, String projectVersion,
                                ArtifactMetadata artifactMeta )
        throws MetadataRepositoryException
    {

        Namespace namespace = getNamespace( repositoryId, namespaceId );
        if ( namespace == null )
        {
            namespace = updateOrAddNamespace( repositoryId, namespaceId );
        }

        ProjectMetadata projectMetadata = new ProjectMetadata( );
        projectMetadata.setId( projectId );
        projectMetadata.setNamespace( namespaceId );
        updateProject( repositorySession, repositoryId, projectMetadata );

        String key = new ArtifactMetadataModel.KeyBuilder( ).withNamespace( namespace ).withProject( projectId ).withId(
            artifactMeta.getId( ) ).withProjectVersion( projectVersion ).build( );


        String table = this.cassandraArchivaManager.getArtifactMetadataFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            Update update = update( table )
                .setColumn( ID.toString( ), literal( artifactMeta.getId( ) ) )//
                .setColumn( REPOSITORY_NAME.toString( ), literal( repositoryId ) ) //
                .setColumn( NAMESPACE_ID.toString( ), literal( namespaceId ) ) //
                .setColumn( PROJECT_ID.toString( ), literal( artifactMeta.getProject( ) ) ) //
                .setColumn( PROJECT_VERSION.toString( ), literal( projectVersion ) ) //
                .setColumn( VERSION.toString( ), literal( artifactMeta.getVersion( ) ) ) //
                .setColumn( FILE_LAST_MODIFIED.toString( ), literal( artifactMeta.getFileLastModified( ).toInstant( ).toEpochMilli( ) ) ) //
                .setColumn( SIZE.toString( ), literal( artifactMeta.getSize( ) ) ) //
                .setColumn( ( WHEN_GATHERED.toString( ) ), literal( artifactMeta.getWhenGathered( ).toInstant( ).toEpochMilli( ) ) )
                .whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) );
            session.execute( update.build( ) ).wasApplied( );
            removeChecksums( key );
            recordChecksums( repositoryId, key, mapChecksums( artifactMeta.getChecksums( ) ) );

            key = new ProjectVersionMetadataModel.KeyBuilder( ) //
                .withRepository( repositoryId ) //
                .withNamespace( namespace ) //
                .withProjectId( projectId ) //
                .withProjectVersion( projectVersion ) //
                .withId( artifactMeta.getId( ) ) //
                .build( );
            table = cassandraArchivaManager.getProjectVersionMetadataFamilyName( );

            Insert insert = insertInto( table )
                .value( DEFAULT_PRIMARY_KEY, literal( key ) )
                .value( REPOSITORY_NAME.toString( ), literal( repositoryId ) )
                .value( NAMESPACE_ID.toString( ), literal( namespaceId ) )
                .value( PROJECT_ID.toString( ), literal( projectId ) )
                .value( PROJECT_VERSION.toString( ), literal( projectVersion ) )
                .value( VERSION.toString( ), literal( artifactMeta.getVersion( ) ) )
                .ifNotExists( );
            session.execute( insert.build( ) );
        }
        ArtifactMetadataModel artifactMetadataModel = new ArtifactMetadataModel( );

        artifactMetadataModel.setRepositoryId( repositoryId );
        artifactMetadataModel.setNamespace( namespaceId );
        artifactMetadataModel.setProject( projectId );
        artifactMetadataModel.setProjectVersion( projectVersion );
        artifactMetadataModel.setVersion( artifactMeta.getVersion( ) );
        artifactMetadataModel.setFileLastModified( artifactMeta.getFileLastModified( ) == null
            ? ZonedDateTime.now( ).toInstant( ).toEpochMilli( )
            : artifactMeta.getFileLastModified( ).toInstant( ).toEpochMilli( ) );
        artifactMetadataModel.setChecksums( mapChecksums( artifactMeta.getChecksums( ) ) );

        // now facets
        updateFacets( artifactMeta, artifactMetadataModel );

    }

    @Override
    public List<String> getArtifactVersions( RepositorySession repositorySession, final String repoId, final String namespace, final String projectId,
                                             final String projectVersion )
        throws MetadataResolutionException
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getProjectVersionMetadataFamilyName( );
            Select query = selectFrom( table )
                .column( VERSION.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repoId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespace ) )
                .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectId ) )
                .whereColumn( PROJECT_VERSION.toString( ) ).isEqualTo( literal( projectVersion ) )
                .allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            return StreamSupport.stream( result.spliterator( ), false )
                .map( row -> row.getString( VERSION.toString( ) ) )
                .distinct()
                .collect( Collectors.toList( ) );
        }
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
        String table = cassandraArchivaManager.getMetadataFacetFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            for ( final String facetId : getSupportedFacets( ) )
            {
                MetadataFacet metadataFacet = facetedMetadata.getFacet( facetId );
                if (metadataFacet!=null)
                {
                    Select deleteRows = selectFrom( table )
                        .column( DEFAULT_PRIMARY_KEY )
                        .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( artifactMetadataModel.getRepositoryId( ) ) )
                        .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( artifactMetadataModel.getNamespace( ) ) )
                        .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( artifactMetadataModel.getProject( ) ) )
                        .whereColumn( PROJECT_VERSION.toString( ) ).isEqualTo( literal( artifactMetadataModel.getProjectVersion( ) ) )
                        .whereColumn( FACET_ID.toString() ).isEqualTo( literal(facetId) )
                        .allowFiltering( );
                    ResultSet resultSet = session.execute( deleteRows.build( ) );
                    StreamSupport.stream( resultSet.spliterator(), false ).map(row -> row.getString( DEFAULT_PRIMARY_KEY )).distinct().forEach( key ->
                        {
                            Delete delete = deleteFrom( table )
                                .whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) );
                            session.execute( delete.build( ) );
                        }
                    );
                    Map<String, String> properties = metadataFacet.toProperties( );

                    for ( Map.Entry<String, String> entry : properties.entrySet( ) )
                    {
                        String key = new MetadataFacetModel.KeyBuilder( ).withKey( entry.getKey( ) ).withArtifactMetadataModel(
                            artifactMetadataModel ).withFacetId( facetId ).withName( metadataFacet.getName( ) ).build( );
                        Update update = update( table )
                            .setColumn( REPOSITORY_NAME.toString( ), literal( artifactMetadataModel.getRepositoryId( ) ) )
                            .setColumn( NAMESPACE_ID.toString( ), literal( artifactMetadataModel.getNamespace( ) ) )
                            .setColumn( PROJECT_ID.toString( ), literal( artifactMetadataModel.getProject( ) ) )
                            .setColumn( PROJECT_VERSION.toString( ), literal( artifactMetadataModel.getProjectVersion( ) ) )
                            .setColumn( FACET_ID.toString( ), literal( facetId ) )
                            .setColumn( KEY.toString( ), literal( entry.getKey( ) ) )
                            .setColumn( VALUE.toString( ), literal( entry.getValue( ) ) )
                            .whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) );
                        session.execute( update.build( ) );
                    }
                }


            }
        }
    }


    @Override
    public List<String> getMetadataFacets( RepositorySession repositorySession, final String repositoryId, final String facetId )
        throws MetadataRepositoryException
    {

        String table = cassandraArchivaManager.getMetadataFacetFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            Select query = selectFrom( table )
                .column( NAME.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( FACET_ID.toString( ) ).isEqualTo( literal( facetId ) )
                .allowFiltering();

            ResultSet result = session.execute( query.build( ) );
            return StreamSupport.stream( result.spliterator( ), false )
                .map( row -> row.getString( NAME.toString( ) ) )
                .distinct( )
                .collect( Collectors.toList( ) );

        }
    }

    private <T> Spliterator<T> createResultSpliterator( ResultSet result, BiFunction<Row, T, T> converter ) throws MetadataRepositoryException
    {
        final Iterator<Row> it = result.iterator( );

        return new Spliterator<T>( )
        {
            private T lastItem = null;

            @Override
            public boolean tryAdvance( Consumer<? super T> action )
            {
                if ( it.hasNext( ) )
                {
                    while ( it.hasNext( ) )
                    {
                        Row row = it.next( );
                        T item = converter.apply( row, lastItem );
                        if ( item != null && lastItem != null && item != lastItem )
                        {
                            action.accept( lastItem );
                            lastItem = item;
                            return true;
                        }
                        lastItem = item;
                    }
                    action.accept( lastItem );
                    return true;
                }
                else
                {
                    return false;
                }
            }

            @Override
            public Spliterator<T> trySplit( )
            {
                return null;
            }

            @Override
            public long estimateSize( )
            {
                return Long.MAX_VALUE;
            }

            @Override
            public int characteristics( )
            {
                return ORDERED + NONNULL;
            }
        };
    }

    <T extends MetadataFacet> Comparator<T> getFacetComparator(boolean ascending) {
        return new Comparator<T>( )
        {
            @Override
            public int compare( T o1, T o2 )
            {
                return ascending ? o1.getName( ).compareTo( o2.getName( ) ) : o2.getName( ).compareTo( o1.getName( ) );
            }
        };
    }

    /**
     * Implementation is not very performant, because sorting is part of the stream. I do not know how to specify the sort
     * in the query.
     *
     * @param <T>
     * @param repositorySession
     * @param repositoryId
     * @param facetClazz
     * @param queryParameter
     * @return
     * @throws MetadataRepositoryException
     */
    @Override
    public <T extends MetadataFacet> Stream<T> getMetadataFacetStream( RepositorySession repositorySession, String repositoryId, Class<T> facetClazz, QueryParameter queryParameter ) throws MetadataRepositoryException
    {
        final MetadataFacetFactory<T> metadataFacetFactory = getFacetFactory( facetClazz );
        final String facetId = metadataFacetFactory.getFacetId( );
        String table = cassandraArchivaManager.getMetadataFacetFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            Select query = selectFrom( table )
                .columns( NAME.toString( ), KEY.toString( ), VALUE.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( FACET_ID.toString( ) ).isEqualTo( literal( facetId ) )
                .allowFiltering();

            ResultSet result = session.execute( query.build( ) );
            return StreamSupport.stream( createResultSpliterator( result, ( Row row, T lastItem ) -> {
                String name = row.getString( NAME.toString( ) );
                String key = row.getString( KEY.toString( ) );
                String value = row.getString( VALUE.toString( ) );
                T updateItem;
                if ( lastItem != null && lastItem.getName( ).equals( name ) )
                {
                    updateItem = lastItem;
                }
                else
                {
                    updateItem = metadataFacetFactory.createMetadataFacet( repositoryId, name );
                }
                if ( StringUtils.isNotEmpty( key ) )
                {
                    Map<String, String> map = new HashMap<>( );
                    map.put( key, value );
                    updateItem.fromProperties( map );
                }
                return updateItem;

            } ), false )
                .sorted( getFacetComparator( queryParameter.isAscending() ) )
                .skip( queryParameter.getOffset( ) ).limit( queryParameter.getLimit( ) );
        }
    }

    @Override
    public boolean hasMetadataFacet( RepositorySession session, String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        return !getMetadataFacets( session, repositoryId, facetId ).isEmpty( );
    }

    @Override
    public <T extends MetadataFacet> T getMetadataFacet( RepositorySession repositorySession, final String repositoryId, final Class<T> facetClazz, final String name )
        throws MetadataRepositoryException
    {
        final MetadataFacetFactory<T> metadataFacetFactory = getFacetFactory( facetClazz );
        if ( metadataFacetFactory == null )
        {
            return null;
        }
        final String facetId = metadataFacetFactory.getFacetId( );
        final String table = cassandraArchivaManager.getMetadataFacetFamilyName( );
        T metadataFacet = metadataFacetFactory.createMetadataFacet( repositoryId, name );
        CqlSession session = cassandraArchivaManager.getSession( );
        {

            Select query = selectFrom( table )
                .column( KEY.toString( ) )
                .column( VALUE.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( FACET_ID.toString( ) ).isEqualTo( literal( facetId ) )
                .whereColumn( NAME.toString( ) ).isEqualTo( literal( name ) )
                .allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            if ( result.getAvailableWithoutFetching( ) == 0 )
            {
                return null;
            }
            Map<String, String> props = StreamSupport.stream( result.spliterator( ), false )
                .filter(row -> !row.isNull(KEY.toString()))
                .collect( Collectors.toMap( row -> row.getString( KEY.toString( ) ), row -> row.getString( VALUE.toString( ) ) ) );
            metadataFacet.fromProperties( props );
            return metadataFacet;
        }
    }

    @Override
    public MetadataFacet getMetadataFacet( RepositorySession repositorySession, String repositoryId, String facetId, String name ) throws MetadataRepositoryException
    {
        return getMetadataFacet( repositorySession, repositoryId, getFactoryClassForId( facetId ), name );
    }

    @Override
    public void addMetadataFacet( RepositorySession repositorySession, String repositoryId, MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {

        if ( metadataFacet == null )
        {
            return;
        }
        final String table = this.cassandraArchivaManager.getMetadataFacetFamilyName( );
        if ( metadataFacet.toProperties( ).isEmpty( ) )
        {
            String key = new MetadataFacetModel.KeyBuilder( ).withRepositoryId( repositoryId ).withFacetId(
                metadataFacet.getFacetId( ) ).withName( metadataFacet.getName( ) ).build( );

            CqlSession session = cassandraArchivaManager.getSession( );
            {
                Update update = update( table )
                    .setColumn( REPOSITORY_NAME.toString( ), literal( repositoryId ) )
                    .setColumn( FACET_ID.toString( ), literal( metadataFacet.getFacetId( ) ) )
                    .setColumn( NAME.toString( ), literal( metadataFacet.getName( ) ) )
                    .whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) );
                session.execute( update.build( ) );
            }
        }
        else
        {
            CqlSession session = cassandraArchivaManager.getSession( );
            {
                for ( Map.Entry<String, String> entry : metadataFacet.toProperties( ).entrySet( ) )
                {
                    String key = new MetadataFacetModel.KeyBuilder( ).withRepositoryId( repositoryId ).withFacetId(
                        metadataFacet.getFacetId( ) ).withName( metadataFacet.getName( ) ).withKey( entry.getKey( ) ).build( );
                    Update update = update( table )
                        .setColumn( REPOSITORY_NAME.toString( ), literal( repositoryId ) )
                        .setColumn( FACET_ID.toString( ), literal( metadataFacet.getFacetId( ) ) )
                        .setColumn( NAME.toString( ), literal( metadataFacet.getName( ) ) )
                        .setColumn( KEY.toString( ), literal( entry.getKey( ) ) )
                        .setColumn( VALUE.toString( ), literal( entry.getValue( ) ) )
                        .whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) );
                    session.execute( update.build( ) );
                }
            }
        }
    }

    @Override
    public void removeMetadataFacets( RepositorySession repositorySession, final String repositoryId, final String facetId )
        throws MetadataRepositoryException
    {
        final String table = cassandraArchivaManager.getMetadataFacetFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            Select deleteRows = selectFrom( table )
                .column( DEFAULT_PRIMARY_KEY )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( FACET_ID.toString( ) ).isEqualTo( literal( facetId ) )
                .allowFiltering( );
            ResultSet result = session.execute( deleteRows.build( ) );
            StreamSupport.stream( result.spliterator(), false ).map(row -> row.getString(DEFAULT_PRIMARY_KEY))
                .distinct().forEach( delKey ->
                    session.execute( deleteFrom( table ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal(delKey) ).build(  ) )
                );
        }

    }

    @Override
    public void removeMetadataFacet( RepositorySession repositorySession, final String repositoryId, final String facetId, final String name )
        throws MetadataRepositoryException
    {
        final String table = cassandraArchivaManager.getMetadataFacetFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            Select deleteRows = selectFrom( table )
                .column( DEFAULT_PRIMARY_KEY )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( FACET_ID.toString( ) ).isEqualTo( literal( facetId ) )
                .whereColumn( NAME.toString( ) ).isEqualTo( literal( name ) )
                .allowFiltering( );
            ResultSet result = session.execute( deleteRows.build( ) );
            StreamSupport.stream( result.spliterator( ), false )
                .map( row -> row.getString( DEFAULT_PRIMARY_KEY ) )
                .distinct( )
                .forEach(
                    delKey ->
                        session.execute( deleteFrom( table ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( delKey ) ).build( ) )
                );

        }

    }

    @Override
    public List<ArtifactMetadata> getArtifactsByDateRange( RepositorySession repositorySession, final String repositoryId, final ZonedDateTime startTime,
                                                           final ZonedDateTime endTime, QueryParameter queryParameter )
        throws MetadataRepositoryException
    {
        final String table = cassandraArchivaManager.getArtifactMetadataFamilyName();
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            long start = startTime == null ? Long.MIN_VALUE : startTime.toInstant( ).toEpochMilli( );
            long end = endTime == null ? Long.MAX_VALUE : endTime.toInstant( ).toEpochMilli( );
            Select query = selectFrom( table )
                .all( )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( WHEN_GATHERED.toString( ) ).isGreaterThanOrEqualTo( literal( start ) )
                .whereColumn( WHEN_GATHERED.toString( ) ).isLessThanOrEqualTo( literal( end ) )
                .allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            return StreamSupport.stream( result.spliterator( ), false )
                .map( this::mapArtifactMetadata )
                .collect( Collectors.toList( ) );
        }
    }

    /**
     * For documentation see {@link MetadataRepository#getArtifactByDateRangeStream(RepositorySession, String, ZonedDateTime, ZonedDateTime, QueryParameter)}
     * <p>
     * This implementation orders the stream. It does not order the query in the backend.
     *
     * @param session        The repository session
     * @param repositoryId   The repository id
     * @param startTime      The start time, can be <code>null</code>
     * @param endTime        The end time, can be <code>null</code>
     * @param queryParameter Additional parameters for the query that affect ordering and number of returned results.
     * @return
     * @throws MetadataRepositoryException
     * @see MetadataRepository#getArtifactByDateRangeStream
     */
    @Override
    public Stream<ArtifactMetadata> getArtifactByDateRangeStream( RepositorySession session, String repositoryId, ZonedDateTime startTime, ZonedDateTime endTime, QueryParameter queryParameter ) throws MetadataRepositoryException
    {
        Comparator<ArtifactMetadata> comp = getArtifactMetadataComparator( queryParameter, "whenGathered" );
        return getArtifactsByDateRange( session, repositoryId, startTime, endTime, queryParameter ).stream( ).sorted( comp ).skip( queryParameter.getOffset( ) ).limit( queryParameter.getLimit( ) );
    }


    protected ArtifactMetadata mapArtifactMetadata( Row row )
    {
        ArtifactMetadata artifactMetadata = new ArtifactMetadata( );
        artifactMetadata.setNamespace( row.getString( NAMESPACE_ID.toString( ) ) );
        artifactMetadata.setSize( row.getLong( SIZE.toString( ) ) );
        artifactMetadata.setId( row.getString( ID.toString( ) ) );
        artifactMetadata.setFileLastModified( row.getLong( FILE_LAST_MODIFIED.toString( ) ) );
        artifactMetadata.setMd5( row.getString( MD5.toString( ) ) );
        artifactMetadata.setProject( row.getString( PROJECT_ID.toString( ) ) );
        artifactMetadata.setProjectVersion( row.getString( PROJECT_VERSION.toString( ) ) );
        artifactMetadata.setRepositoryId( row.getString( REPOSITORY_NAME.toString( ) ) );
        artifactMetadata.setSha1( row.getString( SHA1.toString( ) ) );
        artifactMetadata.setVersion( row.getString( VERSION.toString( ) ) );
        Long whenGathered = row.getLong( WHEN_GATHERED.toString( ) );
        if ( whenGathered != null )
        {
            artifactMetadata.setWhenGathered( ZonedDateTime.ofInstant( Instant.ofEpochMilli( whenGathered ), STORAGE_TZ ) );
        }
        artifactMetadata.setChecksums( mapChecksumsReverse( getChecksums( row.getString( DEFAULT_PRIMARY_KEY ) ) ) );
        return artifactMetadata;
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByChecksum( RepositorySession repositorySession, final String repositoryId, final String checksum )
        throws MetadataRepositoryException
    {
        String table = cassandraArchivaManager.getChecksumFamilyName( );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            Select query = selectFrom( table )
                .column( ARTIFACT_METADATA_MODEL_KEY.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( CHECKSUM_VALUE.toString( ) ).isEqualTo( literal( checksum ) )
                .allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            List<String> artifactKeys = StreamSupport.stream( result.spliterator( ), false )
                .map( row -> row.getString( ARTIFACT_METADATA_MODEL_KEY.toString( ) ) )
                .distinct( )
                .collect( Collectors.toList( ) );
            List<ArtifactMetadata> metadataList = new ArrayList<>( );
            for ( String key : artifactKeys )
            {
                table = cassandraArchivaManager.getArtifactMetadataFamilyName( );
                query = selectFrom( table )
                    .all( )
                    .whereColumn( DEFAULT_PRIMARY_KEY.toString( ) ).isEqualTo( literal( key ) );
                Row row = session.execute( query.build( ) ).one( );
                if ( row != null )
                {
                    metadataList.add( mapArtifactMetadata( row ) );
                }

            }
            return metadataList;

        }
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

    MetadataFacetModel mapMetadataFacet( Row row )
    {
        MetadataFacetModel metadataFacetModel = new MetadataFacetModel( );
        metadataFacetModel.setFacetId( row.getString( FACET_ID.toString( ) ) );
        metadataFacetModel.setName( row.getString( NAME.toString( ) ) );
        metadataFacetModel.setValue( row.getString( VALUE.toString( ) ) );
        metadataFacetModel.setKey( row.getString( KEY.toString( ) ) );
        metadataFacetModel.setProjectVersion( row.getString( PROJECT_VERSION.toString( ) ) );
        return metadataFacetModel;
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByAttribute( RepositorySession repositorySession, String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getMetadataFacetFamilyName( );
            Select query = selectFrom( table )
                .all( )
                .whereColumn( VALUE.toString( ) ).isEqualTo( literal( value ) )
                .allowFiltering();
            if ( key != null )
            {
                query = query.whereColumn( KEY.toString( ) ).isEqualTo( literal( key ) );
            }
            if ( repositoryId != null )
            {
                query = query.whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) );
            }


            final List<ArtifactMetadata> artifactMetadatas = new LinkedList<>( );
            final List<MetadataFacetModel> metadataFacetModels = new ArrayList<>( );
            table = cassandraArchivaManager.getArtifactMetadataFamilyName( );
            ResultSet result = session.execute( query.build( ) );
            Iterator<Row> iterator = result.iterator( );
            while ( iterator.hasNext( ) )
            {
                Row row = iterator.next( );
                metadataFacetModels.add( mapMetadataFacet( row ) );

                query = selectFrom( table )
                    .all( )
                    .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( row.getString( REPOSITORY_NAME.toString( ) ) ) )
                    .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( row.getString( NAMESPACE_ID.toString( ) ) ) )
                    .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( row.getString( PROJECT_ID.toString( ) ) ) )
                    .whereColumn( PROJECT_VERSION.toString( ) ).isEqualTo( literal( row.getString( PROJECT_VERSION.toString( ) ) ) )
                    .allowFiltering();

                ResultSet subResult = session.execute( query.build( ) );
                subResult.forEach( sRow ->
                    artifactMetadatas.add( mapArtifactMetadata( sRow ) ) );

            }

            return mapArtifactFacetToArtifact( metadataFacetModels, artifactMetadatas );

        }
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByProjectVersionAttribute( RepositorySession repositorySession, String key, String value, String repositoryId )
        throws MetadataRepositoryException
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String searchKey = StringUtils.wrapIfMissing( key, '"' );
            String table = cassandraArchivaManager.getProjectVersionMetadataFamilyName( );
            Select query = selectFrom( table )
                .columns( PROJECT_ID.toString( ), REPOSITORY_NAME.toString( ), NAMESPACE_ID.toString( ),
                    PROJECT_VERSION.toString( ) ).allowFiltering();
            if (Arrays.binarySearch( cassandraArchivaManager.getProjectVersionMetadataColumns(), key )>=0){
                query = query.whereColumn( searchKey ).isEqualTo( literal( value ) );
            } else {
                query = query.whereMapValue( VERSION_PROPERTIES.toString( ), literal( key ) ).isEqualTo( literal( value ) );
            }
            ResultSet result = session.execute( query.build( ) );
            List<ArtifactMetadata> artifacts = new LinkedList<>( );
            Iterator<Row> iterator = result.iterator( );
            while ( iterator.hasNext( ) )
            {
                Row row = iterator.next( );
                try
                {
                    artifacts.addAll( getArtifacts( repositorySession,
                        row.getString( REPOSITORY_NAME.toString( ) ),
                        row.getString( NAMESPACE_ID.toString( ) ),
                        row.getString( PROJECT_ID.toString( ) ), row.getString( PROJECT_VERSION.toString( ) ) ) );
                }
                catch ( MetadataResolutionException e )
                {
                    // never raised
                    throw new IllegalStateException( e );
                }
            }
            return artifacts;

        }
    }

    @Override
    public void removeArtifact( RepositorySession repositorySession, final String repositoryId, final String namespace, final String project,
                                final String version, final String id )
        throws MetadataRepositoryException
    {
        logger.debug( "removeTimestampedArtifact repositoryId: '{}', namespace: '{}', project: '{}', version: '{}', id: '{}'",
            repositoryId, namespace, project, version, id );

        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String key =
                new ArtifactMetadataModel.KeyBuilder( ).withRepositoryId( repositoryId ).withNamespace( namespace ).withId(
                    id ).withProjectVersion( version ).withProject( project ).build( );
            String table = cassandraArchivaManager.getArtifactMetadataFamilyName( );
            Delete delete = deleteFrom( table )
                .whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) );
            session.execute( delete.build( ) );

            key = new ProjectVersionMetadataModel.KeyBuilder( ) //
                .withRepository( repositoryId ) //
                .withNamespace( namespace ) //
                .withProjectId( project ) //
                .withProjectVersion( version ) //
                .withId( id ) //
                .build( );
            table = cassandraArchivaManager.getProjectVersionMetadataFamilyName( );
            delete = deleteFrom( table )
                .whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) );
            session.execute( delete.build( ) );

        }

    }

    @Override
    public void removeTimestampedArtifact( RepositorySession repositorySession, ArtifactMetadata artifactMetadata, String baseVersion )
        throws MetadataRepositoryException
    {
        logger.debug( "removeTimestampedArtifact repositoryId: '{}', namespace: '{}', project: '{}', version: '{}', id: '{}'",
            artifactMetadata.getRepositoryId( ), artifactMetadata.getNamespace( ),
            artifactMetadata.getProject( ), baseVersion, artifactMetadata.getId( ) );
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String key =
                new ArtifactMetadataModel.KeyBuilder( ).withRepositoryId( artifactMetadata.getRepositoryId( ) ).withNamespace(
                    artifactMetadata.getNamespace( ) ).withId( artifactMetadata.getId( ) ).withProjectVersion(
                    baseVersion ).withProject( artifactMetadata.getProject( ) ).build( );
            String table = cassandraArchivaManager.getArtifactMetadataFamilyName( );
            Delete delete = deleteFrom( table )
                .whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) );
            session.execute( delete.build( ) );
        }

    }

    @Override
    public void removeFacetFromArtifact( RepositorySession repositorySession, final String repositoryId, final String namespace, final String project,
                                         final String version, final MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getArtifactMetadataFamilyName( );
            Delete delete = deleteFrom( table )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespace ) )
                .whereColumn( PROJECT.toString( ) ).isEqualTo( literal( project ) )
                .whereColumn( VERSION.toString( ) ).isEqualTo( literal( version ) );
            session.execute( delete.build( ) );
        }
    }


    @Override
    public List<ArtifactMetadata> getArtifacts( RepositorySession repositorySession, final String repositoryId )
        throws MetadataRepositoryException
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getArtifactMetadataFamilyName( );
            Select query = selectFrom( table )
                .all( )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) )
                .allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            return StreamSupport.stream( result.spliterator( ), false )
                .map( this::mapArtifactMetadata )
                .collect( Collectors.toList( ) );
        }
    }


    @Override
    public List<ProjectVersionReference> getProjectReferences( RepositorySession repositorySession, String repoId, String namespace, String projectId,
                                                               String projectVersion )
        throws MetadataResolutionException
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getDependencyFamilyName( );
            Select query = selectFrom( table )
                .column( "\"projectVersionMetadataModel.key\"" )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repoId ) )
                .whereColumn( GROUP_ID.toString( ) ).isEqualTo( literal( namespace ) )
                .whereColumn( ARTIFACT_ID.toString( ) ).isEqualTo( literal( projectId ) )
                .whereColumn( VERSION.toString( ) ).isEqualTo( literal( projectVersion ) )
                .allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            List<String> dependenciesIds = StreamSupport.stream( result.spliterator( ), false )
                .map( row -> row.getString( "\"projectVersionMetadataModel.key\"" ) )
                .collect( Collectors.toList( ) );

            List<ProjectVersionReference> references = new ArrayList<>( );


            table = cassandraArchivaManager.getProjectVersionMetadataFamilyName( );
            for ( String key : dependenciesIds )
            {
                query = selectFrom( table )
                    .columns( PROJECT_ID.toString( ), NAMESPACE_ID.toString( ), PROJECT_VERSION.toString( ) )
                    .whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) );
                Row rowResult = session.execute( query.build( ) ).one( );
                if ( rowResult != null )
                {
                    references.add( new ProjectVersionReference( ProjectVersionReference.ReferenceType.DEPENDENCY,
                        rowResult.getString( PROJECT_ID.toString( ) ),
                        rowResult.getString( NAMESPACE_ID.toString( ) ),
                        rowResult.getString( PROJECT_VERSION.toString( ) )
                    ) );
                }

            }
            return references;
        }
    }

    @Override
    public void removeProjectVersion( RepositorySession repositorySession, final String repoId, final String namespace, final String projectId,
                                      final String projectVersion )
        throws MetadataRepositoryException
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getProjectVersionMetadataFamilyName( );
            Select query = selectFrom( table )
                .columns( DEFAULT_PRIMARY_KEY, VERSION.toString( ) )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repoId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespace ) )
                .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectId ) )
                .whereColumn( PROJECT_VERSION.toString( ) ).isEqualTo( literal( projectVersion ) )
                .allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            Iterator<Row> iterator = result.iterator( );
            while ( iterator.hasNext( ) )
            {
                Row row = iterator.next( );
                String key = row.getString( DEFAULT_PRIMARY_KEY );
                session.execute( deleteFrom( table ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( key ) ).build( ) );
                removeMailingList( key );
                removeLicenses( key );
                removeDependencies( key );
            }

            final String deleteTable = cassandraArchivaManager.getArtifactMetadataFamilyName( );
            Select deleteRows = selectFrom( deleteTable )
                .column( DEFAULT_PRIMARY_KEY )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repoId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespace ) )
                .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectId ) )
                .whereColumn( PROJECT_VERSION.toString( ) ).isEqualTo( literal( projectVersion ) )
                .allowFiltering();
            result = session.execute( deleteRows.build( ) );
            StreamSupport.stream( result.spliterator( ), false )
                .map( row -> row.getString( DEFAULT_PRIMARY_KEY ) )
                .distinct( )
                .forEach( delKey ->
                    session.execute( deleteFrom( deleteTable ).whereColumn( DEFAULT_PRIMARY_KEY ).isEqualTo( literal( delKey ) ).build( ) ) );

        }
    }

    @Override
    public List<ArtifactMetadata> getArtifacts( RepositorySession repositorySession, final String repoId, final String namespace,
                                                final String projectId, final String projectVersion )
        throws MetadataResolutionException
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        {
            String table = cassandraArchivaManager.getArtifactMetadataFamilyName( );
            Select query = selectFrom( table )
                .all( )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repoId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespace ) )
                .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectId ) )
                .whereColumn( PROJECT_VERSION.toString( ) ).isEqualTo( literal( projectVersion ) )
                .allowFiltering();
            ResultSet result = session.execute( query.build( ) );
            List<ArtifactMetadata> artifactMetadatas = StreamSupport.stream( result.spliterator( ), false )
                .map( this::mapArtifactMetadata )
                .collect( Collectors.toList( ) );


            table = cassandraArchivaManager.getMetadataFacetFamilyName( );
            query = selectFrom( table )
                .all( )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repoId ) )
                .whereColumn( NAMESPACE_ID.toString( ) ).isEqualTo( literal( namespace ) )
                .whereColumn( PROJECT_ID.toString( ) ).isEqualTo( literal( projectId ) )
                .whereColumn( PROJECT_VERSION.toString( ) ).isEqualTo( literal( projectVersion ) )
                .allowFiltering();
            result = session.execute( query.build( ) );
            List<MetadataFacetModel> facetMetadata = StreamSupport.stream( result.spliterator( ), false )
                .map( row -> mapMetadataFacet( row ) )
                .collect( Collectors.toList( ) );
            return mapArtifactFacetToArtifact( facetMetadata, artifactMetadatas );

        }
    }

    private List<ArtifactMetadata> mapArtifactFacetToArtifact( List<MetadataFacetModel> metadataFacetModels, List<ArtifactMetadata> artifactMetadatas )
    {
        for ( final ArtifactMetadata artifactMetadata : artifactMetadatas )
        {
            Iterator<MetadataFacetModel> iterator = metadataFacetModels.stream( ).filter( metadataFacetModel -> {
                if ( metadataFacetModel != null )
                {
                    return StringUtils.equals( artifactMetadata.getVersion( ),
                        metadataFacetModel.getProjectVersion( ) );
                }
                return false;

            } ).iterator( );
            Map<String, List<MetadataFacetModel>> metadataFacetValuesPerFacetId = new HashMap<>( );
            while ( iterator.hasNext( ) )
            {
                MetadataFacetModel metadataFacetModel = iterator.next( );
                List<MetadataFacetModel> values = metadataFacetValuesPerFacetId.get( metadataFacetModel.getName( ) );
                if ( values == null )
                {
                    values = new ArrayList<>( );
                    metadataFacetValuesPerFacetId.put( metadataFacetModel.getFacetId( ), values );
                }
                values.add( metadataFacetModel );

            }

            for ( Map.Entry<String, List<MetadataFacetModel>> entry : metadataFacetValuesPerFacetId.entrySet( ) )
            {
                MetadataFacetFactory<?> metadataFacetFactory = getFacetFactory( entry.getKey( ) );
                if ( metadataFacetFactory != null )
                {
                    List<MetadataFacetModel> facetModels = entry.getValue( );
                    if ( !facetModels.isEmpty( ) )
                    {
                        MetadataFacet metadataFacet = metadataFacetFactory.createMetadataFacet( );
                        Map<String, String> props = new HashMap<>( facetModels.size( ) );
                        for ( MetadataFacetModel metadataFacetModel : facetModels )
                        {
                            props.put( metadataFacetModel.getKey( ), metadataFacetModel.getValue( ) );
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
    public void close( )
        throws MetadataRepositoryException
    {
        logger.trace( "close" );
    }


    private static class ModelMapperHolder
    {
        private static ModelMapper MODEL_MAPPER = new ModelMapper( );
    }

    protected ModelMapper getModelMapper( )
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
        List<ArtifactMetadata> artifacts = new LinkedList<>( );
        artifacts.addAll( this.getArtifactsByAttribute( session, key, text, repositoryId ) );
        artifacts.addAll( this.getArtifactsByProjectVersionAttribute( session, key, text, repositoryId ) );
        return artifacts;
    }

    @Override
    public Stream<ArtifactMetadata> getArtifactStream( final RepositorySession repositorySession, final String repositoryId,
                                                       final QueryParameter queryParameter ) throws MetadataResolutionException
    {
        CqlSession session = cassandraArchivaManager.getSession( );
        try {
            String table = cassandraArchivaManager.getArtifactMetadataFamilyName( );
            Select query = selectFrom( table )
                .columns( ArtifactMetadataModel.COLUMNS )
                .whereColumn( REPOSITORY_NAME.toString( ) ).isEqualTo( literal( repositoryId ) );
            ResultSet result = session.execute( query.build( ) );
            return StreamSupport.stream( createResultSpliterator( result, ( Row row, ArtifactMetadata last ) ->
                mapArtifactMetadata( row ) ), false ).skip( queryParameter.getOffset( ) ).limit( queryParameter.getLimit( ) );
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
