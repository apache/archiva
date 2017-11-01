package org.apache.archiva.admin.repository.managed;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.IndeterminateConfigurationException;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatisticsManager;
import org.apache.archiva.redback.components.cache.Cache;
import org.apache.archiva.redback.components.registry.RegistryException;
import org.apache.archiva.redback.components.taskqueue.TaskQueueException;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.archiva.scheduler.repository.model.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index_shaded.lucene.index.IndexFormatTooOldException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FIXME review the staging mechanism to have a per user session one
 *
 * @author Olivier Lamy
 */
@Service("managedRepositoryAdmin#default")
public class DefaultManagedRepositoryAdmin
    extends AbstractRepositoryAdmin
    implements ManagedRepositoryAdmin
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    public static final String STAGE_REPO_ID_END = "-stage";


    @Inject
    private RepositoryRegistry repositoryRegistry;

    @Inject
    @Named(value = "archivaTaskScheduler#repository")
    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    /**
     * FIXME: this could be multiple implementations and needs to be configured.
     */
    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    @Inject
    private RepositoryStatisticsManager repositoryStatisticsManager;

    @Inject
    protected RoleManager roleManager;

    @Inject
    @Named(value = "cache#namespaces")
    private Cache<String, Collection<String>> namespacesCache;

    // fields
    @Inject
    private List<? extends IndexCreator> indexCreators;

    @Inject
    private NexusIndexer indexer;

    @PostConstruct
    public void initialize()
        throws RepositoryAdminException, RoleManagerException
    {
        // initialize index context on start and check roles here
        for ( ManagedRepository managedRepository : getManagedRepositories() )
        {
            log.debug("Initializating {}", managedRepository.getId());
            createIndexContext( managedRepository );
            addRepositoryRoles( managedRepository.getId() );

        }
    }

    @PreDestroy
    public void shutdown()
        throws RepositoryAdminException
    {
        try
        {
            // close index on shutdown
            for ( ManagedRepository managedRepository : getManagedRepositories() )
            {
                IndexingContext context = indexer.getIndexingContexts().get( managedRepository.getId() );
                if ( context != null )
                {
                    indexer.removeIndexingContext( context, false );
                }
            }
        }
        catch ( IOException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
    }

    /*
     * Conversion between the repository from the registry and the serialized DTO for the admin API
     */
    private ManagedRepository convertRepo( org.apache.archiva.repository.ManagedRepository repo ) {
        if (repo==null) {
            return null;
        }
        ManagedRepository adminRepo = new ManagedRepository( getArchivaConfiguration().getDefaultLocale() );
        setBaseRepoAttributes( adminRepo, repo );
        adminRepo.setLocation( convertUriToString( repo.getLocation()) );
        adminRepo.setReleases(repo.getActiveReleaseSchemes().contains( ReleaseScheme.RELEASE ));
        adminRepo.setSnapshots( repo.getActiveReleaseSchemes().contains(ReleaseScheme.SNAPSHOT) );
        adminRepo.setBlockRedeployments( repo.blocksRedeployments() );
        adminRepo.setCronExpression( repo.getSchedulingDefinition() );
        if (repo.supportsFeature( IndexCreationFeature.class )) {
            IndexCreationFeature icf = repo.getFeature( IndexCreationFeature.class ).get();
            adminRepo.setIndexDirectory(convertUriToString( icf.getIndexPath() ));
            adminRepo.setSkipPackedIndexCreation( icf.isSkipPackedIndexCreation() );
        }
        adminRepo.setScanned( repo.isScanned() );
        if (repo.supportsFeature( ArtifactCleanupFeature.class) ) {
            ArtifactCleanupFeature acf = repo.getFeature( ArtifactCleanupFeature.class ).get();
            adminRepo.setRetentionPeriod( acf.getRetentionPeriod().getDays() );
            adminRepo.setRetentionCount( acf.getRetentionCount() );
            adminRepo.setDeleteReleasedSnapshots( acf.isDeleteReleasedSnapshots() );

        }
        if (repo.supportsFeature( StagingRepositoryFeature.class )) {
            StagingRepositoryFeature stf = repo.getFeature( StagingRepositoryFeature.class ).get();
            adminRepo.setStageRepoNeeded( stf.isStageRepoNeeded() );
            if (stf.getStagingRepository()!=null) {
                adminRepo.setStagingRepository( convertRepo( stf.getStagingRepository() ) );
            }
        }
        return adminRepo;
    }

    private ManagedRepositoryConfiguration getRepositoryConfiguration(ManagedRepository repo) {
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        setBaseRepoAttributes( repoConfig, repo );
        repoConfig.setBlockRedeployments( repo.isBlockRedeployments( ) );
        repoConfig.setReleases( repo.isReleases() );
        repoConfig.setSnapshots( repo.isSnapshots() );
        repoConfig.setScanned( repo.isScanned() );
        repoConfig.setLocation( getRepositoryCommonValidator().removeExpressions( repo.getLocation() ) );
        repoConfig.setRefreshCronExpression( repo.getCronExpression() );
        repoConfig.setRetentionPeriod( repo.getRetentionPeriod() );
        repoConfig.setRetentionCount( repo.getRetentionCount());
        repoConfig.setDeleteReleasedSnapshots( repo.isDeleteReleasedSnapshots() );
        repoConfig.setSkipPackedIndexCreation( repo.isSkipPackedIndexCreation());
        repoConfig.setStageRepoNeeded( repo.isStageRepoNeeded() );
        return repoConfig;
    }

    @Override
    public List<ManagedRepository> getManagedRepositories()
        throws RepositoryAdminException
    {

        return repositoryRegistry.getManagedRepositories().stream().map( rep -> this.convertRepo( rep ) ).collect( Collectors.toList());
    }

    @Override
    public Map<String, ManagedRepository> getManagedRepositoriesAsMap()
        throws RepositoryAdminException
    {
        return repositoryRegistry.getManagedRepositories().stream().collect( Collectors.toMap( e -> e.getId(), e -> convertRepo( e ) ) );
    }

    @Override
    public ManagedRepository getManagedRepository( String repositoryId )
        throws RepositoryAdminException
    {
        return convertRepo( repositoryRegistry.getManagedRepository( repositoryId ) );
    }

    @Override
    public Boolean addManagedRepository( ManagedRepository managedRepository, boolean needStageRepo,
                                         AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        log.debug("addManagedRepository {}, {}, {}", managedRepository.getId(), needStageRepo, auditInformation);

        getRepositoryCommonValidator().basicValidation( managedRepository, false );
        getRepositoryCommonValidator().validateManagedRepository( managedRepository );
        triggerAuditEvent( managedRepository.getId(), null, AuditEvent.ADD_MANAGED_REPO, auditInformation );
        ManagedRepositoryConfiguration repoConfig = getRepositoryConfiguration( managedRepository );
        if (needStageRepo) {
            repoConfig.setStageRepoNeeded( true );
        }
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        try
        {
            org.apache.archiva.repository.ManagedRepository newRepo = repositoryRegistry.putRepository( repoConfig, configuration );
            log.debug("Added new repository {}", newRepo.getId());
            org.apache.archiva.repository.ManagedRepository stagingRepo = null;
            addRepositoryRoles( newRepo.getId() );
            if ( newRepo.supportsFeature( StagingRepositoryFeature.class )) {
                StagingRepositoryFeature stf = newRepo.getFeature( StagingRepositoryFeature.class ).get();
                stagingRepo = stf.getStagingRepository();
                if (stf.isStageRepoNeeded() && stagingRepo != null) {
                    addRepositoryRoles( stagingRepo.getId() );
                    triggerAuditEvent( stagingRepo.getId(), null, AuditEvent.ADD_MANAGED_REPO, auditInformation );
                }
            }
            saveConfiguration( configuration );
            //MRM-1342 Repository statistics report doesn't appear to be working correctly
            //scan repository when adding of repository is successful
            try
            {
                if ( newRepo.isScanned())
                {
                    scanRepository( newRepo.getId(), true );
                }

                if ( stagingRepo!=null && stagingRepo.isScanned() )
                {
                    scanRepository( stagingRepo.getId(), true );
                }
            }
            catch ( Exception e )
            {
                log.warn("Unable to scan repository [{}]: {}", newRepo.getId(), e.getMessage(), e);
            }
        }
        catch ( RepositoryException e )
        {
            log.error("Could not add managed repository {}"+managedRepository);
            throw new RepositoryAdminException( "Could not add repository "+e.getMessage() );
        }
        catch ( RoleManagerException e )
        {
            log.error("Could not add repository roles for repository [{}]: {}", managedRepository.getId(), e.getMessage(), e);
            throw new RepositoryAdminException( "Could not add roles to repository "+e.getMessage() );
        }
        createIndexContext( managedRepository );
        return Boolean.TRUE;

    }



    @Override
    public Boolean deleteManagedRepository( String repositoryId, AuditInformation auditInformation,
                                            boolean deleteContent )
        throws RepositoryAdminException
    {
        Configuration config = getArchivaConfiguration().getConfiguration();
        ManagedRepositoryConfiguration repoConfig=config.findManagedRepositoryById( repositoryId );

        log.debug("Repo location "+repoConfig.getLocation());

        org.apache.archiva.repository.ManagedRepository repo = repositoryRegistry.getManagedRepository( repositoryId );
        org.apache.archiva.repository.ManagedRepository stagingRepository = null;
        if (repo!=null) {
            try
            {
                if (repo.supportsFeature( StagingRepositoryFeature.class )) {
                    stagingRepository = repo.getFeature( StagingRepositoryFeature.class ).get().getStagingRepository();
                }
                repositoryRegistry.removeRepository( repo, config );
            }
            catch ( RepositoryException e )
            {
                log.error("Removal of repository {} failed: {}", repositoryId, e.getMessage(), e);
                throw new RepositoryAdminException( "Removal of repository "+repositoryId+" failed." );
            }
        } else {
            throw new RepositoryAdminException( "A repository with that id does not exist" );
        }

        triggerAuditEvent( repositoryId, null, AuditEvent.DELETE_MANAGED_REPO, auditInformation );
        if (repoConfig!=null)
        {
            deleteManagedRepository( repoConfig, deleteContent, config, false );
        }


        // stage repo exists ?
        if ( stagingRepository != null )
        {
            // do not trigger event when deleting the staged one
            ManagedRepositoryConfiguration stagingRepositoryConfig = config.findManagedRepositoryById( stagingRepository.getId( ) );
            try
            {
                repositoryRegistry.removeRepository( stagingRepository );
                if (stagingRepositoryConfig!=null)
                {
                    deleteManagedRepository( stagingRepositoryConfig, deleteContent, config, true );
                }
            }
            catch ( RepositoryException e )
            {
                log.error("Removal of staging repository {} failed: {}", stagingRepository.getId(), e.getMessage(), e);
            }
        }

        try
        {
            saveConfiguration( config );
        }
        catch ( Exception e )
        {
            throw new RepositoryAdminException( "Error saving configuration for delete action" + e.getMessage(), e );
        }

        return Boolean.TRUE;
    }

    private Boolean deleteManagedRepository( ManagedRepositoryConfiguration repository, boolean deleteContent,
                                             Configuration config, boolean stagedOne )
        throws RepositoryAdminException
    {

        try
        {
            IndexingContext context = indexer.getIndexingContexts().get( repository.getId() );
            if ( context != null )
            {
                // delete content only if directory exists
                indexer.removeIndexingContext( context,
                                                    deleteContent && context.getIndexDirectoryFile().exists() );
            }
        }
        catch ( IOException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        if ( !stagedOne )
        {
            RepositorySession repositorySession = getRepositorySessionFactory().createSession();
            try
            {
                MetadataRepository metadataRepository = repositorySession.getRepository();
                metadataRepository.removeRepository( repository.getId() );
                //invalidate cache
                namespacesCache.remove( repository.getId() );
                log.debug( "call repositoryStatisticsManager.deleteStatistics" );
                getRepositoryStatisticsManager().deleteStatistics( metadataRepository, repository.getId() );
                repositorySession.save();
            }
            catch ( MetadataRepositoryException e )
            {
                //throw new RepositoryAdminException( e.getMessage(), e );
                log.warn( "skip error during removing repository from MetadataRepository:{}", e.getMessage(), e );
            }
            finally
            {
                repositorySession.close();
            }
        }

        if ( deleteContent )
        {
            // TODO could be async ? as directory can be huge
            Path dir = Paths.get( repository.getLocation() );
            org.apache.archiva.common.utils.FileUtils.deleteQuietly( dir );
        }

        // olamy: copy list for reading as a unit test in webapp fail with ConcurrentModificationException
        List<ProxyConnectorConfiguration> proxyConnectors = new ArrayList<>( config.getProxyConnectors() );
        for ( ProxyConnectorConfiguration proxyConnector : proxyConnectors )
        {
            if ( StringUtils.equals( proxyConnector.getSourceRepoId(), repository.getId() ) )
            {
                config.removeProxyConnector( proxyConnector );
            }
        }

        Map<String, List<String>> repoToGroupMap = config.getRepositoryToGroupMap();
        if ( repoToGroupMap != null )
        {
            if ( repoToGroupMap.containsKey( repository.getId() ) )
            {
                List<String> repoGroups = repoToGroupMap.get( repository.getId() );
                for ( String repoGroup : repoGroups )
                {
                    // copy to prevent UnsupportedOperationException
                    RepositoryGroupConfiguration repositoryGroupConfiguration =
                        config.findRepositoryGroupById( repoGroup );
                    List<String> repos = new ArrayList<>( repositoryGroupConfiguration.getRepositories() );
                    config.removeRepositoryGroup( repositoryGroupConfiguration );
                    repos.remove( repository.getId() );
                    repositoryGroupConfiguration.setRepositories( repos );
                    config.addRepositoryGroup( repositoryGroupConfiguration );
                }
            }
        }

        try
        {
            removeRepositoryRoles( repository );
        }
        catch ( RoleManagerException e )
        {
            throw new RepositoryAdminException(
                "fail to remove repository roles for repository " + repository.getId() + " : " + e.getMessage(), e );
        }

        saveConfiguration( config );

        return Boolean.TRUE;
    }


    @Override
    public Boolean updateManagedRepository( ManagedRepository managedRepository, boolean needStageRepo,
                                            AuditInformation auditInformation, boolean resetStats )
        throws RepositoryAdminException
    {

        log.debug( "updateManagedConfiguration repo {} needStage {} resetStats {} ", managedRepository, needStageRepo,
                   resetStats );

        // Ensure that the fields are valid.

        getRepositoryCommonValidator().basicValidation( managedRepository, true );

        getRepositoryCommonValidator().validateManagedRepository( managedRepository );

        Configuration configuration = getArchivaConfiguration().getConfiguration();

        ManagedRepositoryConfiguration updatedRepoConfig = getRepositoryConfiguration( managedRepository );
        updatedRepoConfig.setStageRepoNeeded( needStageRepo );

        org.apache.archiva.repository.ManagedRepository oldRepo = repositoryRegistry.getManagedRepository( managedRepository.getId( ) );
        boolean stagingExists = false;
        if (oldRepo.supportsFeature( StagingRepositoryFeature.class ) ){
            stagingExists = oldRepo.getFeature( StagingRepositoryFeature.class ).get().getStagingRepository() != null;
        }
        boolean updateIndexContext = !StringUtils.equals( updatedRepoConfig.getIndexDir(), managedRepository.getIndexDirectory() );

        // TODO remove content from old if path has changed !!!!!
        try
        {
            org.apache.archiva.repository.ManagedRepository newRepo = repositoryRegistry.putRepository( updatedRepoConfig, configuration );
            if (newRepo.supportsFeature( StagingRepositoryFeature.class )) {
                org.apache.archiva.repository.ManagedRepository stagingRepo = newRepo.getFeature( StagingRepositoryFeature.class ).get( ).getStagingRepository( );
                if (stagingRepo!=null && !stagingExists)
                {
                    triggerAuditEvent( stagingRepo.getId(), null, AuditEvent.ADD_MANAGED_REPO, auditInformation );
                    addRepositoryRoles( stagingRepo.getId( ) );
                }
            }


        }
        catch ( RepositoryException e )
        {
            log.error("Could not update repository {}: {}", managedRepository.getId(), e.getMessage(), e);
            throw new RepositoryAdminException( "Could not update repository "+managedRepository.getId());
        }
        catch ( RoleManagerException e ) {
            log.error("Error during role update of stage repo {}", managedRepository.getId(), e);
            throw new RepositoryAdminException( "Could not update repository "+managedRepository.getId());
        }
        triggerAuditEvent( managedRepository.getId(), null, AuditEvent.MODIFY_MANAGED_REPO,
            auditInformation );
        try
        {
            getArchivaConfiguration().save(configuration);
        }
        catch ( RegistryException | IndeterminateConfigurationException e )
        {
            log.error("Could not save repository configuration: {}", e.getMessage(), e);
            throw new RepositoryAdminException( "Could not save repository configuration: "+e.getMessage() );
        }

        // Save the repository configuration.
        RepositorySession repositorySession = getRepositorySessionFactory().createSession();

        try
        {

            if ( resetStats )
            {
                log.debug( "call repositoryStatisticsManager.deleteStatistics" );
                getRepositoryStatisticsManager().deleteStatistics( repositorySession.getRepository(),
                                                                   managedRepository.getId() );
                repositorySession.save();
            }

        }
        catch ( MetadataRepositoryException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.close();
        }

        if ( updateIndexContext )
        {
            try
            {
                IndexingContext indexingContext = indexer.getIndexingContexts().get( managedRepository.getId() );
                if ( indexingContext != null )
                {
                    indexer.removeIndexingContext( indexingContext, true );
                }

                // delete directory too as only content is deleted
                Path indexDirectory = indexingContext.getIndexDirectoryFile().toPath();
                org.apache.archiva.common.utils.FileUtils.deleteDirectory( indexDirectory );

                createIndexContext( managedRepository );
            }
            catch ( IOException e )
            {
                throw new RepositoryAdminException( e.getMessage(), e );
            }
        }

        return true;
    }

    //--------------------------
    // utils methods
    //--------------------------


    protected void addRepository( ManagedRepositoryConfiguration repository, Configuration configuration )
        throws RepositoryAdminException, IOException
    {
        try
        {
            getRepositoryRegistry().putRepository( repository, configuration );
        }
        catch ( RepositoryException e )
        {
            throw new RepositoryAdminException( "Could not add the repository to the registry. Cause: "+e.getMessage() );
        }

    }

    public IndexingContext createIndexContext( org.apache.archiva.repository.ManagedRepository repository) throws RepositoryAdminException
    {
        return createIndexContext( convertRepo( repository ) );
    }

    @Override
    public IndexingContext createIndexContext( ManagedRepository repository )
        throws RepositoryAdminException
    {

        IndexingContext context = indexer.getIndexingContexts().get( repository.getId() );

        if ( context != null )
        {
            log.debug( "skip creating repository indexingContent with id {} as already exists", repository.getId() );
            return context;
        }

        // take care first about repository location as can be relative
        Path repositoryDirectory = Paths.get( repository.getLocation() );

        if ( !repositoryDirectory.isAbsolute() )
        {
            repositoryDirectory =
                Paths.get( getRegistry().getString( "appserver.base" ), "repositories",
                          repository.getLocation() );
        }

        if ( !Files.exists(repositoryDirectory) )
        {
            try {
                Files.createDirectories(repositoryDirectory);
            } catch (IOException e) {
                log.error("Could not create directory {}", repositoryDirectory);
            }
        }

        try
        {

            String indexDir = repository.getIndexDirectory();
            //File managedRepository = new File( repository.getLocation() );

            Path indexDirectory = null;
            if ( StringUtils.isNotBlank( indexDir ) )
            {
                indexDirectory = Paths.get( repository.getIndexDirectory() );
                // not absolute so create it in repository directory
                if ( !indexDirectory.isAbsolute() )
                {
                    indexDirectory = repositoryDirectory.resolve(repository.getIndexDirectory() );
                }
                repository.setIndexDirectory( indexDirectory.toAbsolutePath().toString() );
            }
            else
            {
                indexDirectory = repositoryDirectory.resolve(".indexer" );
                if ( !repositoryDirectory.isAbsolute() )
                {
                    indexDirectory = repositoryDirectory.resolve( ".indexer" );
                }
                repository.setIndexDirectory( indexDirectory.toAbsolutePath().toString() );
            }

            if ( !Files.exists(indexDirectory) )
            {
                Files.createDirectories(indexDirectory);
            }

            context = indexer.getIndexingContexts().get( repository.getId() );

            if ( context == null )
            {
                try
                {
                    context = indexer.addIndexingContext( repository.getId(), repository.getId(), repositoryDirectory.toFile(),
                                                          indexDirectory.toFile(),
                                                          repositoryDirectory.toUri().toURL().toExternalForm(),
                                                          indexDirectory.toUri().toURL().toString(), indexCreators );

                    context.setSearchable( repository.isScanned() );
                }
                catch ( IndexFormatTooOldException e )
                {
                    // existing index with an old lucene format so we need to delete it!!!
                    // delete it first then recreate it.
                    log.warn( "the index of repository {} is too old we have to delete and recreate it", //
                              repository.getId() );
                    org.apache.archiva.common.utils.FileUtils.deleteDirectory( indexDirectory );
                    context = indexer.addIndexingContext( repository.getId(), repository.getId(), repositoryDirectory.toFile(),
                                                          indexDirectory.toFile(),
                                                          repositoryDirectory.toUri().toURL().toExternalForm(),
                                                          indexDirectory.toUri().toURL().toString(), indexCreators );

                    context.setSearchable( repository.isScanned() );
                }
            }
            return context;
        }
        catch ( IOException| UnsupportedExistingLuceneIndexException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
    }

    public Boolean scanRepository( String repositoryId, boolean fullScan )
    {
        if ( getRepositoryTaskScheduler().isProcessingRepositoryTask( repositoryId ) )
        {
            log.info( "scanning of repository with id {} already scheduled", repositoryId );
        }
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );
        task.setScanAll( fullScan );
        try
        {
            getRepositoryTaskScheduler().queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "failed to schedule scanning of repo with id {}", repositoryId, e );
            return false;
        }
        return true;
    }


    private void addRepositoryRoles( String repoId )
        throws RoleManagerException
    {
        // TODO: double check these are configured on start up
        // TODO: belongs in the business logic

        if ( !getRoleManager().templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) )
        {
            getRoleManager().createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId );
        }

        if ( !getRoleManager().templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) )
        {
            getRoleManager().createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId );
        }
    }

    protected void removeRepositoryRoles( ManagedRepositoryConfiguration existingRepository )
        throws RoleManagerException
    {
        String repoId = existingRepository.getId();

        if ( getRoleManager().templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) )
        {
            getRoleManager().removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId );
        }

        if ( getRoleManager().templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) )
        {
            getRoleManager().removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId );
        }

        log.debug( "removed user roles associated with repository {}", repoId );
    }

    //--------------------------
    // setters/getters
    //--------------------------


    public RoleManager getRoleManager()
    {
        return roleManager;
    }

    public void setRoleManager( RoleManager roleManager )
    {
        this.roleManager = roleManager;
    }

    public RepositoryStatisticsManager getRepositoryStatisticsManager()
    {
        return repositoryStatisticsManager;
    }

    public void setRepositoryStatisticsManager( RepositoryStatisticsManager repositoryStatisticsManager )
    {
        this.repositoryStatisticsManager = repositoryStatisticsManager;
    }

    public RepositorySessionFactory getRepositorySessionFactory()
    {
        return repositorySessionFactory;
    }

    public void setRepositorySessionFactory( RepositorySessionFactory repositorySessionFactory )
    {
        this.repositorySessionFactory = repositorySessionFactory;
    }


    public RepositoryArchivaTaskScheduler getRepositoryTaskScheduler()
    {
        return repositoryTaskScheduler;
    }

    public void setRepositoryTaskScheduler( RepositoryArchivaTaskScheduler repositoryTaskScheduler )
    {
        this.repositoryTaskScheduler = repositoryTaskScheduler;
    }

    public NexusIndexer getIndexer()
    {
        return indexer;
    }

    public void setIndexer( NexusIndexer indexer )
    {
        this.indexer = indexer;
    }

    public List<? extends IndexCreator> getIndexCreators()
    {
        return indexCreators;
    }

    public void setIndexCreators( List<? extends IndexCreator> indexCreators )
    {
        this.indexCreators = indexCreators;
    }

    public RepositoryRegistry getRepositoryRegistry( )
    {
        return repositoryRegistry;
    }

    public void setRepositoryRegistry( RepositoryRegistry repositoryRegistry )
    {
        this.repositoryRegistry = repositoryRegistry;
    }
}
