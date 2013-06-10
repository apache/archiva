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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.entitystore.DefaultEntityManager;
import com.netflix.astyanax.entitystore.EntityManager;
import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.FacetedMetadata;
import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolutionException;
import org.apache.archiva.metadata.repository.cassandra.model.ArtifactMetadataModel;
import org.apache.archiva.metadata.repository.cassandra.model.MetadataFacetModel;
import org.apache.archiva.metadata.repository.cassandra.model.Namespace;
import org.apache.archiva.metadata.repository.cassandra.model.Project;
import org.apache.archiva.metadata.repository.cassandra.model.ProjectVersionMetadataModel;
import org.apache.archiva.metadata.repository.cassandra.model.Repository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Olivier Lamy
 */
public class CassandraMetadataRepository
    implements MetadataRepository
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    private ArchivaConfiguration configuration;

    private final Map<String, MetadataFacetFactory> metadataFacetFactories;

    private Keyspace keyspace;

    private EntityManager<Repository, String> repositoryEntityManager;

    private EntityManager<Namespace, String> namespaceEntityManager;

    private EntityManager<Project, String> projectEntityManager;

    private EntityManager<ArtifactMetadataModel, String> artifactMetadataModelEntityManager;

    private EntityManager<MetadataFacetModel, String> metadataFacetModelEntityManager;

    private EntityManager<ProjectVersionMetadataModel, String> projectVersionMetadataModelEntityManager;

    public CassandraMetadataRepository( Map<String, MetadataFacetFactory> metadataFacetFactories,
                                        ArchivaConfiguration configuration, Keyspace keyspace )
    {
        this.metadataFacetFactories = metadataFacetFactories;
        this.configuration = configuration;

        this.keyspace = keyspace;

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

    public EntityManager<Repository, String> getRepositoryEntityManager()
    {
        return repositoryEntityManager;
    }

    public EntityManager<Namespace, String> getNamespaceEntityManager()
    {
        return namespaceEntityManager;
    }

    public void setRepositoryEntityManager( EntityManager<Repository, String> repositoryEntityManager )
    {
        this.repositoryEntityManager = repositoryEntityManager;
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

    @Override
    public void updateNamespace( String repositoryId, String namespaceId )
        throws MetadataRepositoryException
    {
        updateOrAddNamespace( repositoryId, namespaceId );

    }

    public Namespace updateOrAddNamespace( String repositoryId, String namespaceId )
        throws MetadataRepositoryException
    {
        try
        {
            Repository repository = this.repositoryEntityManager.get( repositoryId );

            if ( repository == null )
            {
                repository = new Repository( repositoryId );

                Namespace namespace = new Namespace( namespaceId, repository );
                this.repositoryEntityManager.put( repository );

                this.namespaceEntityManager.put( namespace );
            }
            // FIXME add a Namespace id builder
            Namespace namespace = namespaceEntityManager.get(
                new Namespace.KeyBuilder().withNamespace( namespaceId ).withRepositoryId( repositoryId ).build() );
            if ( namespace == null )
            {
                namespace = new Namespace( namespaceId, repository );
                namespaceEntityManager.put( namespace );
            }
            return namespace;
        }
        catch ( PersistenceException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }

    }


    @Override
    public void removeNamespace( String repositoryId, String namespaceId )
        throws MetadataRepositoryException
    {
        try
        {
            Namespace namespace = namespaceEntityManager.get(
                new Namespace.KeyBuilder().withNamespace( namespaceId ).withRepositoryId( repositoryId ).build() );
            if ( namespace != null )
            {
                namespaceEntityManager.remove( namespace );
            }
        }
        catch ( PersistenceException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }


    @Override
    public void removeRepository( final String repositoryId )
        throws MetadataRepositoryException
    {
        try
        {
            final List<ArtifactMetadataModel> artifactMetadataModels = new ArrayList<ArtifactMetadataModel>();

            // remove data related to the repository
            this.artifactMetadataModelEntityManager.visitAll( new Function<ArtifactMetadataModel, Boolean>()
            {
                @Override
                public Boolean apply( ArtifactMetadataModel artifactMetadataModel )
                {
                    if ( artifactMetadataModel != null )
                    {
                        if ( StringUtils.equals( artifactMetadataModel.getRepositoryId(), repositoryId ) )
                        {
                            artifactMetadataModels.add( artifactMetadataModel );
                        }
                    }
                    return Boolean.TRUE;
                }
            } );

            artifactMetadataModelEntityManager.remove( artifactMetadataModels );

            final List<Namespace> namespaces = new ArrayList<Namespace>();

            namespaceEntityManager.visitAll( new Function<Namespace, Boolean>()
            {
                @Override
                public Boolean apply( Namespace namespace )
                {
                    if ( namespace != null )
                    {
                        if ( StringUtils.equals( namespace.getRepository().getId(), repositoryId ) )
                        {
                            namespaces.add( namespace );
                        }
                    }
                    return Boolean.TRUE;
                }
            } );

            namespaceEntityManager.remove( namespaces );

            final List<Project> projects = new ArrayList<Project>();
            projectEntityManager.visitAll( new Function<Project, Boolean>()
            {
                @Override
                public Boolean apply( Project project )
                {
                    if ( project != null )
                    {
                        if ( StringUtils.equals( project.getNamespace().getRepository().getId(), repositoryId ) )
                        {
                            projects.add( project );
                        }
                    }
                    return Boolean.TRUE;
                }
            } );

            projectEntityManager.remove( projects );

            // TODO  cleanup or not
            //final List<MetadataFacetModel> metadataFacetModels = new ArrayList<MetadataFacetModel>(  );
            //metadataFacetModelEntityManager.visitAll( new Function<MetadataFacetModel, Boolean>()

            final List<ProjectVersionMetadataModel> projectVersionMetadataModels =
                new ArrayList<ProjectVersionMetadataModel>();

            projectVersionMetadataModelEntityManager.visitAll( new Function<ProjectVersionMetadataModel, Boolean>()
            {
                @Override
                public Boolean apply( ProjectVersionMetadataModel projectVersionMetadataModel )
                {
                    if ( projectVersionMetadataModel != null )
                    {
                        if ( StringUtils.equals( projectVersionMetadataModel.getNamespace().getRepository().getId(),
                                                 repositoryId ) )
                        {
                            projectVersionMetadataModels.add( projectVersionMetadataModel );
                        }
                    }
                    return Boolean.TRUE;
                }
            } );

            projectVersionMetadataModelEntityManager.remove( projectVersionMetadataModels );

            Repository repository = repositoryEntityManager.get( repositoryId );
            if ( repository != null )
            {
                repositoryEntityManager.remove( repository );
            }

        }
        catch ( PersistenceException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<String> getRepositories()
        throws MetadataRepositoryException
    {
        try
        {
            logger.debug( "getRepositories" );

            List<Repository> repositories = repositoryEntityManager.getAll();
            if ( repositories == null )
            {
                return Collections.emptyList();
            }
            List<String> repoIds = new ArrayList<String>( repositories.size() );
            for ( Repository repository : repositories )
            {
                repoIds.add( repository.getName() );
            }
            logger.debug( "getRepositories found: {}", repoIds );
            return repoIds;
        }
        catch ( PersistenceException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }

    }


    @Override
    public Collection<String> getRootNamespaces( final String repoId )
        throws MetadataResolutionException
    {
        try
        {
            final Set<String> namespaces = new HashSet<String>();

            namespaceEntityManager.visitAll( new Function<Namespace, Boolean>()
            {
                // @Nullable add dependency ?
                @Override
                public Boolean apply( Namespace namespace )
                {
                    if ( namespace != null && namespace.getRepository() != null && StringUtils.equalsIgnoreCase( repoId,
                                                                                                                 namespace.getRepository().getId() ) )
                    {
                        String name = namespace.getName();
                        if ( StringUtils.isNotEmpty( name ) )
                        {
                            namespaces.add( StringUtils.substringBefore( name, "." ) );
                        }
                    }
                    return Boolean.TRUE;
                }
            } );

            return namespaces;
        }
        catch ( PersistenceException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<String> getNamespaces( final String repoId, final String namespaceId )
        throws MetadataResolutionException
    {
        try
        {
            final Set<String> namespaces = new HashSet<String>();

            namespaceEntityManager.visitAll( new Function<Namespace, Boolean>()
            {
                // @Nullable add dependency ?
                @Override
                public Boolean apply( Namespace namespace )
                {
                    if ( namespace != null && namespace.getRepository() != null && StringUtils.equalsIgnoreCase( repoId,
                                                                                                                 namespace.getRepository().getId() ) )
                    {
                        String currentNamespace = namespace.getName();
                        // we only return childs
                        if ( StringUtils.startsWith( currentNamespace, namespaceId ) && (
                            StringUtils.length( currentNamespace ) > StringUtils.length( namespaceId ) ) )
                        {
                            // store after namespaceId '.' but before next '.'
                            // call org namespace org.apache.maven.shared -> stored apache

                            String calledNamespace =
                                StringUtils.endsWith( namespaceId, "." ) ? namespaceId : namespaceId + ".";
                            String storedNamespace = StringUtils.substringAfter( currentNamespace, calledNamespace );

                            storedNamespace = StringUtils.substringBefore( storedNamespace, "." );

                            namespaces.add( storedNamespace );
                        }
                    }
                    return Boolean.TRUE;
                }
            } );

            return namespaces;
        }
        catch ( PersistenceException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }

    }

    public List<String> getNamespaces( final String repoId )
        throws MetadataResolutionException
    {
        try
        {
            logger.debug( "getNamespaces for repository '{}'", repoId );
            //TypedQuery<Repository> typedQuery =
            //    entityManager.createQuery( "select n from Namespace n where n.repository_id=:id", Namespace.class );

            //List<Repository> namespaces = typedQuery.setParameter( "id", repoId ).getResultList();

            Repository repository = repositoryEntityManager.get( repoId );

            if ( repository == null )
            {
                return Collections.emptyList();
            }

            // FIXME find correct cql query
            //String query = "select * from namespace where repository.id = '" + repoId + "';";

            //List<Namespace> namespaces = namespaceEntityManager.find( query );

            final Set<Namespace> namespaces = new HashSet<Namespace>();

            namespaceEntityManager.visitAll( new Function<Namespace, Boolean>()
            {
                // @Nullable add dependency ?
                @Override
                public Boolean apply( Namespace namespace )
                {
                    if ( namespace != null && namespace.getRepository() != null && StringUtils.equalsIgnoreCase( repoId,
                                                                                                                 namespace.getRepository().getId() ) )
                    {
                        namespaces.add( namespace );
                    }
                    return Boolean.TRUE;
                }
            } );

            repository.setNamespaces( new ArrayList<Namespace>( namespaces ) );

            if ( repository == null || repository.getNamespaces().isEmpty() )
            {
                return Collections.emptyList();
            }
            List<String> namespaceIds = new ArrayList<String>( repository.getNamespaces().size() );

            for ( Namespace n : repository.getNamespaces() )
            {
                namespaceIds.add( n.getName() );
            }

            logger.debug( "getNamespaces for repository '{}' found {}", repoId, namespaceIds.size() );
            return namespaceIds;
        }
        catch ( PersistenceException e )
        {
            throw new MetadataResolutionException( e.getMessage(), e );
        }
    }


    @Override
    public void updateProject( String repositoryId, ProjectMetadata projectMetadata )
        throws MetadataRepositoryException
    {

        // project exists ? if yes return
        String projectKey = new Project.KeyBuilder().withProjectId( projectMetadata.getId() ).withNamespace(
            new Namespace( projectMetadata.getNamespace(), new Repository( repositoryId ) ) ).build();

        Project project = projectEntityManager.get( projectKey );
        if ( project != null )
        {
            return;
        }

        String namespaceKey = new Namespace.KeyBuilder().withRepositoryId( repositoryId ).withNamespace(
            projectMetadata.getNamespace() ).build();
        Namespace namespace = namespaceEntityManager.get( namespaceKey );
        if ( namespace == null )
        {
            namespace = updateOrAddNamespace( repositoryId, projectMetadata.getNamespace() );
        }

        project = new Project( projectKey, projectMetadata.getId(), namespace );

        try
        {
            projectEntityManager.put( project );
        }
        catch ( PersistenceException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }

    }

    @Override
    public void removeProject( final String repositoryId, final String namespaceId, final String projectId )
        throws MetadataRepositoryException
    {

        // cleanup ArtifactMetadataModel
        final List<ArtifactMetadataModel> artifactMetadataModels = new ArrayList<ArtifactMetadataModel>();

        artifactMetadataModelEntityManager.visitAll( new Function<ArtifactMetadataModel, Boolean>()
        {
            @Override
            public Boolean apply( ArtifactMetadataModel artifactMetadataModel )
            {
                if ( artifactMetadataModel != null )
                {
                    if ( StringUtils.equals( artifactMetadataModel.getRepositoryId(), repositoryId )
                        && StringUtils.equals( artifactMetadataModel.getNamespace(), namespaceId )
                        && StringUtils.equals( artifactMetadataModel.getProject(), projectId ) )
                    {
                        artifactMetadataModels.add( artifactMetadataModel );
                    }
                }
                return Boolean.TRUE;
            }
        } );

        artifactMetadataModelEntityManager.remove( artifactMetadataModels );

        Namespace namespace = new Namespace( namespaceId, new Repository( repositoryId ) );

        final List<ProjectVersionMetadataModel> projectVersionMetadataModels =
            new ArrayList<ProjectVersionMetadataModel>();

        projectVersionMetadataModelEntityManager.visitAll( new Function<ProjectVersionMetadataModel, Boolean>()
        {
            @Override
            public Boolean apply( ProjectVersionMetadataModel projectVersionMetadataModel )
            {
                if ( projectVersionMetadataModel != null )
                {
                    if ( StringUtils.equals( repositoryId,
                                             projectVersionMetadataModel.getNamespace().getRepository().getName() )
                        && StringUtils.equals( namespaceId, projectVersionMetadataModel.getNamespace().getName() )
                        && StringUtils.equals( projectId, projectVersionMetadataModel.getProjectId() ) )
                    {
                        projectVersionMetadataModels.add( projectVersionMetadataModel );
                    }
                }
                return Boolean.TRUE;
            }
        } );

        if ( !projectVersionMetadataModels.isEmpty() )
        {
            projectVersionMetadataModelEntityManager.remove( projectVersionMetadataModels );
        }

        String key = new Project.KeyBuilder().withNamespace( namespace ).withProjectId( projectId ).build();

        Project project = projectEntityManager.get( key );
        if ( project == null )
        {
            logger.debug( "removeProject notfound" );
            return;
        }
        logger.debug( "removeProject {}", project );

        projectEntityManager.remove( project );
    }

    @Override
    public Collection<String> getProjectVersions( final String repoId, final String namespace, final String projectId )
        throws MetadataResolutionException
    {
        final Set<String> versions = new HashSet<String>();
        projectVersionMetadataModelEntityManager.visitAll( new Function<ProjectVersionMetadataModel, Boolean>()
        {
            @Override
            public Boolean apply( ProjectVersionMetadataModel projectVersionMetadataModel )
            {
                if ( projectVersionMetadataModel != null )
                {
                    if ( StringUtils.equals( repoId,
                                             projectVersionMetadataModel.getNamespace().getRepository().getName() )
                        && StringUtils.startsWith( projectVersionMetadataModel.getNamespace().getName(), namespace )
                        && StringUtils.equals( projectId, projectVersionMetadataModel.getProjectId() ) )
                    {
                        versions.add( projectVersionMetadataModel.getId() );
                    }
                }
                return Boolean.TRUE;
            }
        } );
        // FIXME use cql query
        artifactMetadataModelEntityManager.visitAll( new Function<ArtifactMetadataModel, Boolean>()
        {
            @Override
            public Boolean apply( ArtifactMetadataModel artifactMetadataModel )
            {
                if ( artifactMetadataModel != null )
                {
                    if ( StringUtils.equals( repoId, artifactMetadataModel.getRepositoryId() ) && StringUtils.equals(
                        namespace, artifactMetadataModel.getNamespace() ) && StringUtils.equals( projectId,
                                                                                                 artifactMetadataModel.getProject() ) )
                    {
                        versions.add( artifactMetadataModel.getProjectVersion() );
                    }
                }
                return Boolean.TRUE;
            }
        } );

        return versions;
    }

    @Override
    public void updateArtifact( String repositoryId, String namespaceId, String projectId, String projectVersion,
                                ArtifactMetadata artifactMeta )
        throws MetadataRepositoryException
    {
        String namespaceKey =
            new Namespace.KeyBuilder().withRepositoryId( repositoryId ).withNamespace( namespaceId ).build();
        // create the namespace if not exists
        Namespace namespace = namespaceEntityManager.get( namespaceKey );
        if ( namespace == null )
        {
            namespace = updateOrAddNamespace( repositoryId, namespaceId );
        }

        // create the project if not exist
        String projectKey = new Project.KeyBuilder().withNamespace( namespace ).withProjectId( projectId ).build();

        Project project = projectEntityManager.get( projectKey );
        if ( project == null )
        {
            project = new Project( projectKey, projectId, namespace );
            try
            {
                projectEntityManager.put( project );
            }
            catch ( PersistenceException e )
            {
                throw new MetadataRepositoryException( e.getMessage(), e );
            }
        }

        String key = new ArtifactMetadataModel.KeyBuilder().withNamespace( namespace ).withProject( projectId ).withId(
            artifactMeta.getId() ).withProjectVersion( projectVersion ).build();

        ArtifactMetadataModel artifactMetadataModel = artifactMetadataModelEntityManager.get( key );
        if ( artifactMetadataModel == null )
        {
            artifactMetadataModel = new ArtifactMetadataModel( key, artifactMeta.getId(), repositoryId, namespaceId,
                                                               artifactMeta.getProject(), projectVersion,
                                                               artifactMeta.getVersion(),
                                                               artifactMeta.getFileLastModified(),
                                                               artifactMeta.getSize(), artifactMeta.getMd5(),
                                                               artifactMeta.getSha1(), artifactMeta.getWhenGathered() );

        }
        else
        {
            artifactMetadataModel.setFileLastModified( artifactMeta.getFileLastModified().getTime() );
            artifactMetadataModel.setWhenGathered( artifactMeta.getWhenGathered().getTime() );
            artifactMetadataModel.setSize( artifactMeta.getSize() );
            artifactMetadataModel.setMd5( artifactMeta.getMd5() );
            artifactMetadataModel.setSha1( artifactMeta.getSha1() );
            artifactMetadataModel.setVersion( artifactMeta.getVersion() );
        }

        try
        {
            artifactMetadataModelEntityManager.put( artifactMetadataModel );
        }
        catch ( PersistenceException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }

        key = new ProjectVersionMetadataModel.KeyBuilder().withRepository( repositoryId ).withNamespace(
            namespace ).withProjectId( projectId ).withId( projectVersion ).build();

        ProjectVersionMetadataModel projectVersionMetadataModel = projectVersionMetadataModelEntityManager.get( key );

        if ( projectVersionMetadataModel == null )
        {
            projectVersionMetadataModel = new ProjectVersionMetadataModel();
            projectVersionMetadataModel.setRowId( key );
            projectVersionMetadataModel.setProjectId( projectId );
            projectVersionMetadataModel.setId( projectVersion );
            projectVersionMetadataModel.setNamespace( namespace );

            projectVersionMetadataModelEntityManager.put( projectVersionMetadataModel );

        }

        // now facets
        updateFacets( artifactMeta, artifactMetadataModel );

    }

    @Override
    public Collection<String> getArtifactVersions( final String repoId, final String namespace, final String projectId,
                                                   final String projectVersion )
        throws MetadataResolutionException
    {
        final Set<String> versions = new HashSet<String>();
        // FIXME use cql query
        artifactMetadataModelEntityManager.visitAll( new Function<ArtifactMetadataModel, Boolean>()
        {
            @Override
            public Boolean apply( ArtifactMetadataModel artifactMetadataModel )
            {
                if ( artifactMetadataModel != null )
                {
                    if ( StringUtils.equals( repoId, artifactMetadataModel.getRepositoryId() ) && StringUtils.equals(
                        namespace, artifactMetadataModel.getNamespace() ) && StringUtils.equals( projectId,
                                                                                                 artifactMetadataModel.getProject() )
                        && StringUtils.equals( projectVersion, artifactMetadataModel.getProjectVersion() ) )
                    {
                        versions.add( artifactMetadataModel.getVersion() );
                    }
                }
                return Boolean.TRUE;
            }
        } );

        return versions;
    }

    /**
     * iterate over available facets to remove/add from the artifactMetadata
     *
     * @param facetedMetadata
     * @param artifactMetadataModel only use for the key
     */
    private void updateFacets( final FacetedMetadata facetedMetadata,
                               final ArtifactMetadataModel artifactMetadataModel )
    {

        for ( final String facetId : metadataFacetFactories.keySet() )
        {
            MetadataFacet metadataFacet = facetedMetadata.getFacet( facetId );
            if ( metadataFacet == null )
            {
                continue;
            }
            // clean first

            final List<MetadataFacetModel> metadataFacetModels = new ArrayList<MetadataFacetModel>();

            metadataFacetModelEntityManager.visitAll( new Function<MetadataFacetModel, Boolean>()
            {
                @Override
                public Boolean apply( MetadataFacetModel metadataFacetModel )
                {
                    ArtifactMetadataModel tmp = metadataFacetModel.getArtifactMetadataModel();
                    if ( StringUtils.equals( metadataFacetModel.getFacetId(), facetId ) && StringUtils.equals(
                        tmp.getRepositoryId(), artifactMetadataModel.getRepositoryId() ) && StringUtils.equals(
                        tmp.getNamespace(), artifactMetadataModel.getNamespace() ) && StringUtils.equals(
                        tmp.getProject(), artifactMetadataModel.getProject() ) )
                    {
                        metadataFacetModels.add( metadataFacetModel );
                    }
                    return Boolean.TRUE;
                }
            } );

            metadataFacetModelEntityManager.remove( metadataFacetModels );

            Map<String, String> properties = metadataFacet.toProperties();

            final List<MetadataFacetModel> metadataFacetModelsToAdd =
                new ArrayList<MetadataFacetModel>( properties.size() );

            for ( Map.Entry<String, String> entry : properties.entrySet() )
            {
                String key = new MetadataFacetModel.KeyBuilder().withKey( entry.getKey() ).withArtifactMetadataModel(
                    artifactMetadataModel ).withFacetId( facetId ).withName( metadataFacet.getName() ).build();
                MetadataFacetModel metadataFacetModel =
                    new MetadataFacetModel( key, artifactMetadataModel, facetId, entry.getKey(), entry.getValue(),
                                            metadataFacet.getName() );
                metadataFacetModelsToAdd.add( metadataFacetModel );
            }

            metadataFacetModelEntityManager.put( metadataFacetModelsToAdd );

        }
    }

    @Override
    public void updateProjectVersion( String repositoryId, String namespaceId, String projectId,
                                      ProjectVersionMetadata versionMetadata )
        throws MetadataRepositoryException
    {
        String namespaceKey =
            new Namespace.KeyBuilder().withRepositoryId( repositoryId ).withNamespace( namespaceId ).build();
        Namespace namespace = namespaceEntityManager.get( namespaceKey );
        if ( namespace == null )
        {
            namespace = updateOrAddNamespace( repositoryId, namespaceId );
        }

        String key = new Project.KeyBuilder().withNamespace( namespace ).withProjectId( projectId ).build();

        Project project = projectEntityManager.get( key );
        if ( project == null )
        {
            project = new Project( key, projectId, namespace );
            projectEntityManager.put( project );
        }

        // we don't test of repository and namespace really exist !
        key = new ProjectVersionMetadataModel.KeyBuilder().withRepository( repositoryId ).withNamespace(
            namespaceId ).withProjectId( projectId ).withId( versionMetadata.getId() ).build();

        ProjectVersionMetadataModel projectVersionMetadataModel = projectVersionMetadataModelEntityManager.get( key );

        if ( projectVersionMetadataModel == null )
        {
            projectVersionMetadataModel =
                new BeanReplicator().replicateBean( versionMetadata, ProjectVersionMetadataModel.class );
            projectVersionMetadataModel.setRowId( key );
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


        try
        {
            projectVersionMetadataModelEntityManager.put( projectVersionMetadataModel );

            ArtifactMetadataModel artifactMetadataModel = new ArtifactMetadataModel();
            artifactMetadataModel.setArtifactMetadataModelId(
                new ArtifactMetadataModel.KeyBuilder().withId( versionMetadata.getId() ).withRepositoryId(
                    repositoryId ).withNamespace( namespaceId ).withProjectVersion(
                    versionMetadata.getVersion() ).build() );
            artifactMetadataModel.setRepositoryId( repositoryId );
            artifactMetadataModel.setNamespace( namespaceId );
            artifactMetadataModel.setProject( projectId );
            artifactMetadataModel.setProjectVersion( versionMetadata.getVersion() );
            artifactMetadataModel.setVersion( versionMetadata.getVersion() );
            // facets etc...
            updateFacets( versionMetadata, artifactMetadataModel );
        }
        catch ( PersistenceException e )
        {
            throw new MetadataRepositoryException( e.getMessage(), e );
        }
    }


    private static class BooleanHolder
    {
        private boolean value = false;
    }

    @Override
    public List<String> getMetadataFacets( final String repositoryId, final String facetId )
        throws MetadataRepositoryException
    {
        // FIXME use cql query !!
        final List<String> facets = new ArrayList<String>();
        this.metadataFacetModelEntityManager.visitAll( new Function<MetadataFacetModel, Boolean>()
        {
            @Override
            public Boolean apply( MetadataFacetModel metadataFacetModel )
            {
                if ( metadataFacetModel != null )
                {
                    if ( StringUtils.equals( metadataFacetModel.getArtifactMetadataModel().getRepositoryId(),
                                             repositoryId ) && StringUtils.equals( metadataFacetModel.getFacetId(),
                                                                                   facetId ) )
                    {
                        facets.add( metadataFacetModel.getName() );
                    }
                }
                return Boolean.TRUE;
            }
        } );

        return facets;

    }

    @Override
    public boolean hasMetadataFacet( String repositoryId, String facetId )
        throws MetadataRepositoryException
    {
        return !getMetadataFacets( repositoryId, facetId ).isEmpty();
    }

    @Override
    public MetadataFacet getMetadataFacet( final String repositoryId, final String facetId, final String name )
        throws MetadataRepositoryException
    {
        // FIXME use cql query !!
        final List<MetadataFacetModel> facets = new ArrayList<MetadataFacetModel>();
        this.metadataFacetModelEntityManager.visitAll( new Function<MetadataFacetModel, Boolean>()
        {
            @Override
            public Boolean apply( MetadataFacetModel metadataFacetModel )
            {
                if ( metadataFacetModel != null )
                {
                    if ( StringUtils.equals( metadataFacetModel.getArtifactMetadataModel().getRepositoryId(),
                                             repositoryId ) && StringUtils.equals( metadataFacetModel.getFacetId(),
                                                                                   facetId ) && StringUtils.equals(
                        metadataFacetModel.getName(), name ) )
                    {
                        facets.add( metadataFacetModel );
                    }
                }
                return Boolean.TRUE;
            }
        } );

        if ( facets.isEmpty() )
        {
            return null;
        }

        MetadataFacetFactory metadataFacetFactory = metadataFacetFactories.get( facetId );
        if ( metadataFacetFactory == null )
        {
            return null;
        }
        MetadataFacet metadataFacet = metadataFacetFactory.createMetadataFacet( repositoryId, name );
        Map<String, String> map = new HashMap<String, String>( facets.size() );
        for ( MetadataFacetModel metadataFacetModel : facets )
        {
            map.put( metadataFacetModel.getKey(), metadataFacetModel.getValue() );
        }
        metadataFacet.fromProperties( map );
        return metadataFacet;
    }

    @Override
    public void addMetadataFacet( String repositoryId, MetadataFacet metadataFacet )
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
            MetadataFacetModel metadataFacetModel = metadataFacetModelEntityManager.get( key );
            if ( metadataFacetModel == null )
            {
                metadataFacetModel = new MetadataFacetModel();
            }
            // we need to store the repositoryId
            ArtifactMetadataModel artifactMetadataModel = new ArtifactMetadataModel();
            artifactMetadataModel.setRepositoryId( repositoryId );
            metadataFacetModel.setArtifactMetadataModel( artifactMetadataModel );
            metadataFacetModel.setId( key );
            metadataFacetModel.setFacetId( metadataFacet.getFacetId() );
            metadataFacetModel.setName( metadataFacet.getName() );

            try
            {
                metadataFacetModelEntityManager.put( metadataFacetModel );
            }
            catch ( PersistenceException e )
            {
                throw new MetadataRepositoryException( e.getMessage(), e );
            }
        }
        else
        {
            for ( Map.Entry<String, String> entry : metadataFacet.toProperties().entrySet() )
            {

                String key = new MetadataFacetModel.KeyBuilder().withRepositoryId( repositoryId ).withFacetId(
                    metadataFacet.getFacetId() ).withName( metadataFacet.getName() ).withKey( entry.getKey() ).build();

                MetadataFacetModel metadataFacetModel = metadataFacetModelEntityManager.get( key );
                if ( metadataFacetModel == null )
                {
                    metadataFacetModel = new MetadataFacetModel();
                    // we need to store the repositoryId
                    ArtifactMetadataModel artifactMetadataModel = new ArtifactMetadataModel();
                    artifactMetadataModel.setRepositoryId( repositoryId );
                    metadataFacetModel.setArtifactMetadataModel( artifactMetadataModel );
                    metadataFacetModel.setId( key );
                    metadataFacetModel.setKey( entry.getKey() );
                    metadataFacetModel.setFacetId( metadataFacet.getFacetId() );
                    metadataFacetModel.setName( metadataFacet.getName() );
                }
                metadataFacetModel.setValue( entry.getValue() );
                try
                {
                    metadataFacetModelEntityManager.put( metadataFacetModel );
                }
                catch ( PersistenceException e )
                {
                    throw new MetadataRepositoryException( e.getMessage(), e );
                }

            }
        }
    }

    @Override
    public void removeMetadataFacets( final String repositoryId, final String facetId )
        throws MetadataRepositoryException
    {
        logger.debug( "removeMetadataFacets repositoryId: '{}', facetId: '{}'", repositoryId, facetId );
        final List<MetadataFacetModel> toRemove = new ArrayList<MetadataFacetModel>();

        // FIXME cql query
        metadataFacetModelEntityManager.visitAll( new Function<MetadataFacetModel, Boolean>()
        {
            @Override
            public Boolean apply( MetadataFacetModel metadataFacetModel )
            {
                if ( metadataFacetModel != null )
                {
                    if ( StringUtils.equals( metadataFacetModel.getArtifactMetadataModel().getRepositoryId(),
                                             repositoryId ) && StringUtils.equals( metadataFacetModel.getFacetId(),
                                                                                   facetId ) )
                    {
                        toRemove.add( metadataFacetModel );
                    }
                }
                return Boolean.TRUE;
            }
        } );
        logger.debug( "removeMetadataFacets repositoryId: '{}', facetId: '{}', toRemove: {}", repositoryId, facetId,
                      toRemove );
        metadataFacetModelEntityManager.remove( toRemove );
    }

    @Override
    public void removeMetadataFacet( final String repositoryId, final String facetId, final String name )
        throws MetadataRepositoryException
    {
        logger.debug( "removeMetadataFacets repositoryId: '{}', facetId: '{}'", repositoryId, facetId );
        final List<MetadataFacetModel> toRemove = new ArrayList<MetadataFacetModel>();

        // FIXME cql query
        metadataFacetModelEntityManager.visitAll( new Function<MetadataFacetModel, Boolean>()
        {
            @Override
            public Boolean apply( MetadataFacetModel metadataFacetModel )
            {
                if ( metadataFacetModel != null )
                {
                    if ( StringUtils.equals( metadataFacetModel.getArtifactMetadataModel().getRepositoryId(),
                                             repositoryId ) && StringUtils.equals( metadataFacetModel.getFacetId(),
                                                                                   facetId ) && StringUtils.equals(
                        metadataFacetModel.getName(), name ) )
                    {
                        toRemove.add( metadataFacetModel );
                    }
                }
                return Boolean.TRUE;
            }
        } );
        logger.debug( "removeMetadataFacets repositoryId: '{}', facetId: '{}', toRemove: {}", repositoryId, facetId,
                      toRemove );
        metadataFacetModelEntityManager.remove( toRemove );
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByDateRange( final String repositoryId, final Date startTime,
                                                           final Date endTime )
        throws MetadataRepositoryException
    {

        final List<ArtifactMetadataModel> artifactMetadataModels = new ArrayList<ArtifactMetadataModel>();

        // FIXME cql query
        artifactMetadataModelEntityManager.visitAll( new Function<ArtifactMetadataModel, Boolean>()
        {
            @Override
            public Boolean apply( ArtifactMetadataModel artifactMetadataModel )
            {
                if ( artifactMetadataModel != null )
                {
                    if ( StringUtils.equals( artifactMetadataModel.getRepositoryId(), repositoryId )
                        && artifactMetadataModel.getNamespace() != null &&
                        artifactMetadataModel.getProject() != null && artifactMetadataModel.getId() != null )
                    {

                        Date when = artifactMetadataModel.getWhenGathered();
                        if ( ( startTime != null ? when.getTime() >= startTime.getTime() : true ) && ( endTime != null ?
                            when.getTime() <= endTime.getTime() : true ) )
                        {
                            logger.debug( "getArtifactsByDateRange visitAll found: {}", artifactMetadataModel );
                            artifactMetadataModels.add( artifactMetadataModel );
                        }
                    }
                }
                return Boolean.TRUE;
            }
        } );
        List<ArtifactMetadata> artifactMetadatas = new ArrayList<ArtifactMetadata>( artifactMetadataModels.size() );

        for ( ArtifactMetadataModel model : artifactMetadataModels )
        {
            ArtifactMetadata artifactMetadata = new BeanReplicator().replicateBean( model, ArtifactMetadata.class );
            populateFacets( artifactMetadata );
            artifactMetadatas.add( artifactMetadata );
        }

        // FIXME facets ?

        logger.debug( "getArtifactsByDateRange repositoryId: {}, startTime: {}, endTime: {}, artifactMetadatas: {}",
                      repositoryId, startTime, endTime, artifactMetadatas );

        return artifactMetadatas;
    }

    protected void populateFacets( final ArtifactMetadata artifactMetadata )
    {
        final List<MetadataFacetModel> metadataFacetModels = new ArrayList<MetadataFacetModel>();

        metadataFacetModelEntityManager.visitAll( new Function<MetadataFacetModel, Boolean>()
        {
            @Override
            public Boolean apply( MetadataFacetModel metadataFacetModel )
            {
                if ( metadataFacetModel != null )
                {
                    ArtifactMetadataModel artifactMetadataModel = metadataFacetModel.getArtifactMetadataModel();
                    if ( artifactMetadataModel != null )
                    {
                        if ( StringUtils.equals( artifactMetadata.getRepositoryId(),
                                                 artifactMetadataModel.getRepositoryId() ) && StringUtils.equals(
                            artifactMetadata.getNamespace(), artifactMetadataModel.getNamespace() )
                            && StringUtils.equals( artifactMetadata.getRepositoryId(),
                                                   artifactMetadataModel.getRepositoryId() ) && StringUtils.equals(
                            artifactMetadata.getProject(), artifactMetadataModel.getProject() ) && StringUtils.equals(
                            artifactMetadata.getId(), artifactMetadataModel.getId() ) )
                        {
                            metadataFacetModels.add( metadataFacetModel );
                        }
                    }
                }
                return Boolean.TRUE;
            }
        } );
        Map<String, Map<String, String>> facetValuesPerFacet = new HashMap<String, Map<String, String>>();

        for ( MetadataFacetModel model : metadataFacetModels )
        {
            Map<String, String> values = facetValuesPerFacet.get( model.getName() );
            if ( values == null )
            {
                values = new HashMap<String, String>();
            }
            values.put( model.getKey(), model.getValue() );
            facetValuesPerFacet.put( model.getName(), values );
        }

        for ( Map.Entry<String, Map<String, String>> entry : facetValuesPerFacet.entrySet() )
        {
            MetadataFacetFactory factory = metadataFacetFactories.get( entry.getKey() );
            if ( factory == null )
            {
                continue;
            }
            MetadataFacet metadataFacet =
                factory.createMetadataFacet( artifactMetadata.getRepositoryId(), entry.getKey() );
            metadataFacet.fromProperties( entry.getValue() );
            artifactMetadata.addFacet( metadataFacet );
        }
    }

    @Override
    public List<ArtifactMetadata> getArtifactsByChecksum( final String repositoryId, final String checksum )
        throws MetadataRepositoryException
    {
        final List<ArtifactMetadataModel> artifactMetadataModels = new ArrayList<ArtifactMetadataModel>();

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "all ArtifactMetadataModel: {}", artifactMetadataModelEntityManager.getAll() );
        }

        // FIXME cql query
        artifactMetadataModelEntityManager.visitAll( new Function<ArtifactMetadataModel, Boolean>()
        {
            @Override
            public Boolean apply( ArtifactMetadataModel artifactMetadataModel )
            {
                if ( artifactMetadataModel != null )
                {
                    if ( StringUtils.equals( artifactMetadataModel.getRepositoryId(), repositoryId )
                        && artifactMetadataModel.getNamespace() != null &&
                        artifactMetadataModel.getProject() != null && artifactMetadataModel.getId() != null )
                    {

                        if ( StringUtils.equals( checksum, artifactMetadataModel.getMd5() ) || StringUtils.equals(
                            checksum, artifactMetadataModel.getSha1() ) )
                        {
                            artifactMetadataModels.add( artifactMetadataModel );
                        }
                    }
                }
                return Boolean.TRUE;
            }
        } );
        List<ArtifactMetadata> artifactMetadatas = new ArrayList<ArtifactMetadata>( artifactMetadataModels.size() );

        for ( ArtifactMetadataModel model : artifactMetadataModels )
        {
            ArtifactMetadata artifactMetadata = new BeanReplicator().replicateBean( model, ArtifactMetadata.class );
            populateFacets( artifactMetadata );
            artifactMetadatas.add( artifactMetadata );
        }

        logger.debug( "getArtifactsByChecksum repositoryId: {}, checksum: {}, artifactMetadatas: {}", repositoryId,
                      checksum, artifactMetadatas );

        return artifactMetadatas;
    }

    @Override
    public void removeArtifact( final String repositoryId, final String namespace, final String project,
                                final String version, final String id )
        throws MetadataRepositoryException
    {
        logger.debug( "removeArtifact repositoryId: '{}', namespace: '{}', project: '{}', version: '{}', id: '{}'",
                      repositoryId, namespace, project, version, id );
        String key =
            new ArtifactMetadataModel.KeyBuilder().withRepositoryId( repositoryId ).withNamespace( namespace ).withId(
                id ).withProjectVersion( version ).withProject( project ).build();

        ArtifactMetadataModel artifactMetadataModel = new ArtifactMetadataModel();
        artifactMetadataModel.setArtifactMetadataModelId( key );

        artifactMetadataModelEntityManager.remove( artifactMetadataModel );

        key =
            new ProjectVersionMetadataModel.KeyBuilder().withId( version ).withRepository( repositoryId ).withNamespace(
                namespace ).withProjectId( project ).build();

        ProjectVersionMetadataModel projectVersionMetadataModel = new ProjectVersionMetadataModel();
        projectVersionMetadataModel.setRowId( key );

        projectVersionMetadataModelEntityManager.remove( projectVersionMetadataModel );
    }

    @Override
    public void removeArtifact( ArtifactMetadata artifactMetadata, String baseVersion )
        throws MetadataRepositoryException
    {
        logger.debug( "removeArtifact repositoryId: '{}', namespace: '{}', project: '{}', version: '{}', id: '{}'",
                      artifactMetadata.getRepositoryId(), artifactMetadata.getNamespace(),
                      artifactMetadata.getProject(), baseVersion, artifactMetadata.getId() );
        String key =
            new ArtifactMetadataModel.KeyBuilder().withRepositoryId( artifactMetadata.getRepositoryId() ).withNamespace(
                artifactMetadata.getNamespace() ).withId( artifactMetadata.getId() ).withProjectVersion(
                baseVersion ).withProject( artifactMetadata.getProject() ).build();

        ArtifactMetadataModel artifactMetadataModel = new ArtifactMetadataModel();
        artifactMetadataModel.setArtifactMetadataModelId( key );

        artifactMetadataModelEntityManager.remove( artifactMetadataModel );
    }

    @Override
    public void removeArtifact( final String repositoryId, final String namespace, final String project,
                                final String version, final MetadataFacet metadataFacet )
        throws MetadataRepositoryException
    {
        final List<ArtifactMetadataModel> artifactMetadataModels = new ArrayList<ArtifactMetadataModel>();
        artifactMetadataModelEntityManager.visitAll( new Function<ArtifactMetadataModel, Boolean>()
        {
            @Override
            public Boolean apply( ArtifactMetadataModel artifactMetadataModel )
            {
                if ( artifactMetadataModel != null )
                {
                    if ( StringUtils.equals( repositoryId, artifactMetadataModel.getRepositoryId() )
                        && StringUtils.equals( namespace, artifactMetadataModel.getNamespace() ) && StringUtils.equals(
                        project, artifactMetadataModel.getProject() ) && StringUtils.equals( project,
                                                                                             artifactMetadataModel.getVersion() ) )
                    {
                        artifactMetadataModels.add( artifactMetadataModel );
                    }
                }
                return Boolean.TRUE;
            }
        } );
        artifactMetadataModelEntityManager.remove( artifactMetadataModels );
        /*
        metadataFacetModelEntityManager.visitAll( new Function<MetadataFacetModel, Boolean>()
        {
            @Override
            public Boolean apply( MetadataFacetModel metadataFacetModel )
            {
                if ( metadataFacetModel != null )
                {
                    ArtifactMetadataModel artifactMetadataModel = metadataFacetModel.getArtifactMetadataModel();
                    if ( artifactMetadataModel != null )
                    {
                        if ( StringUtils.equals( repositoryId, artifactMetadataModel.getRepositoryId() )
                            && StringUtils.equals( namespace, artifactMetadataModel.getNamespace() )
                            && StringUtils.equals( project, artifactMetadataModel.getProject() ) && StringUtils.equals(
                            version, artifactMetadataModel.getVersion() ) )
                        {
                            if ( StringUtils.equals( metadataFacetModel.getFacetId(), metadataFacet.getFacetId() )
                                && StringUtils.equals( metadataFacetModel.getName(), metadataFacet.getName() ) )
                            {
                                metadataFacetModels.add( metadataFacetModel );
                            }
                        }
                    }
                }
                return Boolean.TRUE;
            }
        } );
        metadataFacetModelEntityManager.remove( metadataFacetModels );
        */
    }


    @Override
    public List<ArtifactMetadata> getArtifacts( final String repositoryId )
        throws MetadataRepositoryException
    {
        final List<ArtifactMetadataModel> artifactMetadataModels = new ArrayList<ArtifactMetadataModel>();
        // FIXME use cql query !
        artifactMetadataModelEntityManager.visitAll( new Function<ArtifactMetadataModel, Boolean>()
        {
            @Override
            public Boolean apply( ArtifactMetadataModel artifactMetadataModel )
            {
                if ( artifactMetadataModel != null )
                {
                    if ( StringUtils.equals( repositoryId, artifactMetadataModel.getRepositoryId() ) )
                    {
                        artifactMetadataModels.add( artifactMetadataModel );
                    }
                }

                return Boolean.TRUE;
            }
        } );

        List<ArtifactMetadata> artifactMetadatas = new ArrayList<ArtifactMetadata>( artifactMetadataModels.size() );

        for ( ArtifactMetadataModel model : artifactMetadataModels )
        {
            ArtifactMetadata artifactMetadata = new BeanReplicator().replicateBean( model, ArtifactMetadata.class );
            populateFacets( artifactMetadata );
            artifactMetadatas.add( artifactMetadata );
        }

        return artifactMetadatas;
    }

    @Override
    public ProjectMetadata getProject( final String repoId, final String namespace, final String id )
        throws MetadataResolutionException
    {
        //basically just checking it exists
        // FIXME use cql query

        final BooleanHolder booleanHolder = new BooleanHolder();

        projectEntityManager.visitAll( new Function<Project, Boolean>()
        {
            @Override
            public Boolean apply( Project project )
            {
                if ( project != null )
                {
                    if ( StringUtils.equals( repoId, project.getNamespace().getRepository().getName() )
                        && StringUtils.equals( namespace, project.getNamespace().getName() ) && StringUtils.equals( id,
                                                                                                                    project.getProjectId() ) )
                    {
                        booleanHolder.value = true;
                    }
                }
                return Boolean.TRUE;
            }
        } );

        if ( !booleanHolder.value )
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

    @Override
    public ProjectVersionMetadata getProjectVersion( final String repoId, final String namespace,
                                                     final String projectId, final String projectVersion )
        throws MetadataResolutionException
    {
        String key = new ProjectVersionMetadataModel.KeyBuilder().withRepository( repoId ).withNamespace(
            namespace ).withProjectId( projectId ).withId( projectVersion ).build();

        ProjectVersionMetadataModel projectVersionMetadataModel = projectVersionMetadataModelEntityManager.get( key );

        if ( projectVersionMetadataModel == null )
        {
            logger.debug(
                "getProjectVersion repoId: '{}', namespace: '{}', projectId: '{}', projectVersion: {} -> not found",
                repoId, namespace, projectId, projectVersion );
            return null;
        }

        ProjectVersionMetadata projectVersionMetadata =
            new BeanReplicator().replicateBean( projectVersionMetadataModel, ProjectVersionMetadata.class );

        logger.debug( "getProjectVersion repoId: '{}', namespace: '{}', projectId: '{}', projectVersion: {} -> {}",
                      repoId, namespace, projectId, projectVersion, projectVersionMetadata );

        projectVersionMetadata.setCiManagement( projectVersionMetadataModel.getCiManagement() );
        projectVersionMetadata.setIssueManagement( projectVersionMetadataModel.getIssueManagement() );
        projectVersionMetadata.setOrganization( projectVersionMetadataModel.getOrganization() );
        projectVersionMetadata.setScm( projectVersionMetadataModel.getScm() );

        // FIXME complete collections !!

        // facets
        final List<MetadataFacetModel> metadataFacetModels = new ArrayList<MetadataFacetModel>();
        // FIXME use cql query
        metadataFacetModelEntityManager.visitAll( new Function<MetadataFacetModel, Boolean>()
        {
            @Override
            public Boolean apply( MetadataFacetModel metadataFacetModel )
            {
                if ( metadataFacetModel != null )
                {
                    if ( StringUtils.equals( repoId, metadataFacetModel.getArtifactMetadataModel().getRepositoryId() )
                        && StringUtils.equals( namespace, metadataFacetModel.getArtifactMetadataModel().getNamespace() )
                        && StringUtils.equals( projectId, metadataFacetModel.getArtifactMetadataModel().getProject() )
                        && StringUtils.equals( projectVersion,
                                               metadataFacetModel.getArtifactMetadataModel().getProjectVersion() ) )
                    {
                        metadataFacetModels.add( metadataFacetModel );
                    }
                }
                return Boolean.TRUE;
            }
        } );
        Map<String, Map<String, String>> metadataFacetsPerFacetIds = new HashMap<String, Map<String, String>>();
        for ( MetadataFacetModel metadataFacetModel : metadataFacetModels )
        {

            Map<String, String> metaValues = metadataFacetsPerFacetIds.get( metadataFacetModel.getFacetId() );
            if ( metaValues == null )
            {
                metaValues = new HashMap<String, String>();
                metadataFacetsPerFacetIds.put( metadataFacetModel.getFacetId(), metaValues );
            }
            metaValues.put( metadataFacetModel.getKey(), metadataFacetModel.getValue() );

        }

        if ( !metadataFacetsPerFacetIds.isEmpty() )
        {
            for ( Map.Entry<String, Map<String, String>> entry : metadataFacetsPerFacetIds.entrySet() )
            {
                MetadataFacetFactory metadataFacetFactory = metadataFacetFactories.get( entry.getKey() );
                if ( metadataFacetFactory != null )
                {
                    MetadataFacet metadataFacet = metadataFacetFactory.createMetadataFacet( repoId, entry.getKey() );
                    metadataFacet.fromProperties( entry.getValue() );
                    projectVersionMetadata.addFacet( metadataFacet );
                }
            }
        }

        return projectVersionMetadata;
    }


    @Override
    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
        throws MetadataResolutionException
    {
        // FIXME implement this
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getProjects( final String repoId, final String namespace )
        throws MetadataResolutionException
    {
        final Set<String> projects = new HashSet<String>();

        // FIXME use cql query
        projectEntityManager.visitAll( new Function<Project, Boolean>()
        {
            @Override
            public Boolean apply( Project project )
            {
                if ( project != null )
                {
                    if ( StringUtils.equals( repoId, project.getNamespace().getRepository().getName() )
                        && StringUtils.startsWith( project.getNamespace().getName(), namespace ) )
                    {
                        projects.add( project.getProjectId() );
                    }
                }
                return Boolean.TRUE;
            }
        } );
        /*

        artifactMetadataModelEntityManager.visitAll( new Function<ArtifactMetadataModel, Boolean>()
        {
            @Override
            public Boolean apply( ArtifactMetadataModel artifactMetadataModel )
            {
                if ( artifactMetadataModel != null )
                {
                    if ( StringUtils.equals( repoId, artifactMetadataModel.getRepositoryId() ) && StringUtils.equals(
                        namespace, artifactMetadataModel.getNamespace() ) )
                    {
                        projects.add( artifactMetadataModel.getProject() );
                    }
                }
                return Boolean.TRUE;
            }
        } );
        */
        return projects;
    }


    @Override
    public void removeProjectVersion( final String repoId, final String namespace, final String projectId,
                                      final String projectVersion )
        throws MetadataRepositoryException
    {

        final List<ArtifactMetadataModel> artifactMetadataModels = new ArrayList<ArtifactMetadataModel>();

        // FIXME use cql query

        artifactMetadataModelEntityManager.visitAll( new Function<ArtifactMetadataModel, Boolean>()
        {
            @Override
            public Boolean apply( ArtifactMetadataModel artifactMetadataModel )
            {
                if ( artifactMetadataModel != null )
                {
                    if ( StringUtils.equals( repoId, artifactMetadataModel.getRepositoryId() ) && StringUtils.equals(
                        namespace, artifactMetadataModel.getNamespace() ) && StringUtils.equals( projectId,
                                                                                                 artifactMetadataModel.getProject() )
                        && StringUtils.equals( projectVersion, artifactMetadataModel.getProjectVersion() ) )
                    {
                        artifactMetadataModels.add( artifactMetadataModel );
                    }
                }
                return Boolean.TRUE;
            }
        } );

        logger.debug( "removeProjectVersions:{}", artifactMetadataModels );
        if ( artifactMetadataModels.isEmpty() )
        {
            return;
        }

        artifactMetadataModelEntityManager.remove( artifactMetadataModels );

        String key = new ProjectVersionMetadataModel.KeyBuilder().withProjectId( projectId ).withId(
            projectVersion ).withRepository( repoId ).withNamespace( namespace ).build();

        ProjectVersionMetadataModel projectVersionMetadataModel = new ProjectVersionMetadataModel();
        projectVersionMetadataModel.setRowId( key );

        projectVersionMetadataModelEntityManager.remove( projectVersionMetadataModel );
    }

    @Override
    public Collection<ArtifactMetadata> getArtifacts( final String repoId, final String namespace,
                                                      final String projectId, final String projectVersion )
        throws MetadataResolutionException
    {
        final List<ArtifactMetadataModel> artifactMetadataModels = new ArrayList<ArtifactMetadataModel>();
        // FIXME use cql query !
        artifactMetadataModelEntityManager.visitAll( new Function<ArtifactMetadataModel, Boolean>()
        {
            @Override
            public Boolean apply( ArtifactMetadataModel artifactMetadataModel )
            {
                if ( artifactMetadataModel != null )
                {
                    if ( StringUtils.equals( repoId, artifactMetadataModel.getRepositoryId() ) && StringUtils.equals(
                        namespace, artifactMetadataModel.getNamespace() ) && StringUtils.equals( projectId,
                                                                                                 artifactMetadataModel.getProject() )
                        && StringUtils.equals( projectVersion, artifactMetadataModel.getProjectVersion() ) )
                    {
                        artifactMetadataModels.add( artifactMetadataModel );
                    }
                }

                return Boolean.TRUE;
            }
        } );

        List<ArtifactMetadata> artifactMetadatas = new ArrayList<ArtifactMetadata>( artifactMetadataModels.size() );

        for ( ArtifactMetadataModel model : artifactMetadataModels )
        {
            ArtifactMetadata artifactMetadata = new BeanReplicator().replicateBean( model, ArtifactMetadata.class );
            populateFacets( artifactMetadata );
            artifactMetadatas.add( artifactMetadata );
        }

        // retrieve facets
        final List<MetadataFacetModel> metadataFacetModels = new ArrayList<MetadataFacetModel>();
        metadataFacetModelEntityManager.visitAll( new Function<MetadataFacetModel, Boolean>()
        {
            @Override
            public Boolean apply( MetadataFacetModel metadataFacetModel )
            {
                if ( metadataFacetModel != null )
                {
                    if ( StringUtils.equals( repoId, metadataFacetModel.getArtifactMetadataModel().getRepositoryId() )
                        && StringUtils.equals( namespace, metadataFacetModel.getArtifactMetadataModel().getNamespace() )
                        && StringUtils.equals( projectId, metadataFacetModel.getArtifactMetadataModel().getProject() )
                        && StringUtils.equals( projectVersion,
                                               metadataFacetModel.getArtifactMetadataModel().getProjectVersion() ) )
                    {
                        metadataFacetModels.add( metadataFacetModel );
                    }

                }
                return Boolean.TRUE;
            }
        } );

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
                                                       metadataFacetModel.getArtifactMetadataModel().getVersion() );
                        }
                        return false;
                    }
                } );
            Iterator<MetadataFacetModel> iterator = metadataFacetModelIterable.iterator();
            Map<String, List<MetadataFacetModel>> metadataFacetValuesPerFacetId =
                new HashMap<String, List<MetadataFacetModel>>();
            while ( iterator.hasNext() )
            {
                MetadataFacetModel metadataFacetModel = iterator.next();
                List<MetadataFacetModel> values = metadataFacetValuesPerFacetId.get( metadataFacetModel.getName() );
                if ( values == null )
                {
                    values = new ArrayList<MetadataFacetModel>();
                    metadataFacetValuesPerFacetId.put( metadataFacetModel.getFacetId(), values );
                }
                values.add( metadataFacetModel );

            }

            for ( Map.Entry<String, List<MetadataFacetModel>> entry : metadataFacetValuesPerFacetId.entrySet() )
            {
                MetadataFacetFactory metadataFacetFactory = metadataFacetFactories.get( entry.getKey() );
                if ( metadataFacetFactory != null )
                {
                    List<MetadataFacetModel> facetModels = entry.getValue();
                    if ( !facetModels.isEmpty() )
                    {
                        MetadataFacet metadataFacet =
                            metadataFacetFactory.createMetadataFacet( repoId, facetModels.get( 0 ).getName() );
                        Map<String, String> props = new HashMap<String, String>( facetModels.size() );
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
    public void save()
    {
        logger.trace( "save" );
    }

    @Override
    public void close()
        throws MetadataRepositoryException
    {
        logger.trace( "close" );
    }

    @Override
    public void revert()
    {
        logger.warn( "CassandraMetadataRepository cannot revert" );
    }

    @Override
    public boolean canObtainAccess( Class<?> aClass )
    {
        return false;
    }

    @Override
    public <T> T obtainAccess( Class<T> aClass )
        throws MetadataRepositoryException
    {
        throw new IllegalArgumentException(
            "Access using " + aClass + " is not supported on the cassandra metadata storage" );
    }
}
