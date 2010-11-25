package org.apache.archiva.web.xmlrpc.services;

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

import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.filter.Filter;
import org.apache.archiva.metadata.repository.filter.IncludesFilter;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.repository.events.RepositoryListener;
import org.apache.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.archiva.stagerepository.merge.RepositoryMerger;
import org.apache.archiva.web.xmlrpc.api.AdministrationService;
import org.apache.archiva.web.xmlrpc.api.beans.ManagedRepository;
import org.apache.archiva.web.xmlrpc.api.beans.RemoteRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.ContentNotFoundException;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.scheduler.CronExpressionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * AdministrationServiceImpl
 * 
 * @version $Id: AdministrationServiceImpl.java
 */
public class AdministrationServiceImpl
    implements AdministrationService
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    private ArchivaConfiguration archivaConfiguration;

    private RepositoryContentConsumers repoConsumersUtil;

    private RepositoryContentFactory repoFactory;

    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    private Collection<RepositoryListener> listeners;

    private MetadataRepository metadataRepository;

    private RepositoryStatisticsManager repositoryStatisticsManager;

    private RepositoryMerger repositoryMerger;

    private static final String STAGE = "-stage";

    private AuditListener auditListener;

    public AdministrationServiceImpl( ArchivaConfiguration archivaConfig, RepositoryContentConsumers repoConsumersUtil,
                                      RepositoryContentFactory repoFactory, MetadataRepository metadataRepository,
                                      RepositoryArchivaTaskScheduler repositoryTaskScheduler,
                                      Collection<RepositoryListener> listeners,
                                      RepositoryStatisticsManager repositoryStatisticsManager,
                                      RepositoryMerger repositoryMerger, AuditListener auditListener )
    {
        this.archivaConfiguration = archivaConfig;
        this.repoConsumersUtil = repoConsumersUtil;
        this.repoFactory = repoFactory;
        this.repositoryTaskScheduler = repositoryTaskScheduler;
        this.listeners = listeners;
        this.metadataRepository = metadataRepository;
        this.repositoryStatisticsManager = repositoryStatisticsManager;
        this.repositoryMerger = repositoryMerger;
        this.auditListener = auditListener;
    }

    /**
     * @see AdministrationService#configureRepositoryConsumer(String, String, boolean)
     */
    public Boolean configureRepositoryConsumer( String repoId, String consumerId, boolean enable )
        throws Exception
    {
        // TODO use repoId once consumers are configured per repository! (MRM-930)

        List<KnownRepositoryContentConsumer> knownConsumers = repoConsumersUtil.getAvailableKnownConsumers();
        List<InvalidRepositoryContentConsumer> invalidConsumers = repoConsumersUtil.getAvailableInvalidConsumers();

        boolean found = false;
        boolean isKnownContentConsumer = false;
        for ( KnownRepositoryContentConsumer consumer : knownConsumers )
        {
            if ( consumer.getId().equals( consumerId ) )
            {
                found = true;
                isKnownContentConsumer = true;
                break;
            }
        }

        if ( !found )
        {
            for ( InvalidRepositoryContentConsumer consumer : invalidConsumers )
            {
                if ( consumer.getId().equals( consumerId ) )
                {
                    found = true;
                    break;
                }
            }
        }

        if ( !found )
        {
            throw new Exception( "Invalid repository consumer." );
        }

        Configuration config = archivaConfiguration.getConfiguration();
        RepositoryScanningConfiguration repoScanningConfig = config.getRepositoryScanning();

        if ( isKnownContentConsumer )
        {
            repoScanningConfig.addKnownContentConsumer( consumerId );
        }
        else
        {
            repoScanningConfig.addInvalidContentConsumer( consumerId );
        }

        config.setRepositoryScanning( repoScanningConfig );
        saveConfiguration( config );

        return true;
    }

    /**
     * @see AdministrationService#deleteArtifact(String, String, String, String)
     */
    public Boolean deleteArtifact( String repoId, String groupId, String artifactId, String version )
        throws Exception
    {
        // TODO: remove duplication with web

        Configuration config = archivaConfiguration.getConfiguration();
        ManagedRepositoryConfiguration repoConfig = config.findManagedRepositoryById( repoId );

        if ( repoConfig == null )
        {
            throw new Exception( "Repository does not exist." );
        }

        try
        {
            ManagedRepositoryContent repoContent = repoFactory.getManagedRepositoryContent( repoId );
            VersionedReference ref = new VersionedReference();
            ref.setGroupId( groupId );
            ref.setArtifactId( artifactId );
            ref.setVersion( version );

            // delete from file system
            repoContent.deleteVersion( ref );

            Collection<ArtifactMetadata> artifacts =
                metadataRepository.getArtifacts( repoId, groupId, artifactId, version );

            for ( ArtifactMetadata artifact : artifacts )
            {
                // TODO: mismatch between artifact (snapshot) version and project (base) version here
                if ( artifact.getVersion().equals( version ) )
                {
                    metadataRepository.deleteArtifact( artifact.getRepositoryId(), artifact.getNamespace(),
                                                       artifact.getProject(), artifact.getVersion(), artifact.getId() );

                    // TODO: move into the metadata repository proper - need to differentiate attachment of
                    // repository metadata to an artifact
                    for ( RepositoryListener listener : listeners )
                    {
                        listener.deleteArtifact( repoId, artifact.getNamespace(), artifact.getProject(),
                                                 artifact.getVersion(), artifact.getId() );
                    }
                }
            }
        }
        catch ( ContentNotFoundException e )
        {
            throw new Exception( "Artifact does not exist." );
        }
        catch ( RepositoryNotFoundException e )
        {
            throw new Exception( "Repository does not exist." );
        }
        catch ( RepositoryException e )
        {
            throw new Exception( "Repository exception occurred." );
        }

        return true;
    }

    /**
     * @see AdministrationService#executeRepositoryScanner(String)
     */
    public Boolean executeRepositoryScanner( String repoId )
        throws Exception
    {
        Configuration config = archivaConfiguration.getConfiguration();
        if ( config.findManagedRepositoryById( repoId ) == null )
        {
            throw new Exception( "Repository does not exist." );
        }

        if ( repositoryTaskScheduler.isProcessingRepositoryTask( repoId ) )
        {
            return false;
        }

        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repoId );

        repositoryTaskScheduler.queueTask( task );

        return true;
    }

    /**
     * @see AdministrationService#getAllRepositoryConsumers()
     */
    public List<String> getAllRepositoryConsumers()
    {
        List<String> consumers = new ArrayList<String>();

        List<KnownRepositoryContentConsumer> knownConsumers = repoConsumersUtil.getAvailableKnownConsumers();
        List<InvalidRepositoryContentConsumer> invalidConsumers = repoConsumersUtil.getAvailableInvalidConsumers();

        for ( KnownRepositoryContentConsumer consumer : knownConsumers )
        {
            consumers.add( consumer.getId() );
        }

        for ( InvalidRepositoryContentConsumer consumer : invalidConsumers )
        {
            consumers.add( consumer.getId() );
        }

        return consumers;
    }

    /**
     * @see AdministrationService#getAllManagedRepositories()
     */
    public List<ManagedRepository> getAllManagedRepositories()
    {
        List<ManagedRepository> managedRepos = new ArrayList<ManagedRepository>();

        Configuration config = archivaConfiguration.getConfiguration();
        List<ManagedRepositoryConfiguration> managedRepoConfigs = config.getManagedRepositories();

        for ( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
        {
            // TODO fix resolution of repo url!
            ManagedRepository repo =
                new ManagedRepository( repoConfig.getId(), repoConfig.getName(), "URL", repoConfig.getLayout(),
                                       repoConfig.isSnapshots(), repoConfig.isReleases() );
            managedRepos.add( repo );
        }

        return managedRepos;
    }

    /**
     * @see AdministrationService#getAllRemoteRepositories()
     */
    public List<RemoteRepository> getAllRemoteRepositories()
    {
        List<RemoteRepository> remoteRepos = new ArrayList<RemoteRepository>();

        Configuration config = archivaConfiguration.getConfiguration();
        List<RemoteRepositoryConfiguration> remoteRepoConfigs = config.getRemoteRepositories();

        for ( RemoteRepositoryConfiguration repoConfig : remoteRepoConfigs )
        {
            RemoteRepository repo =
                new RemoteRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getUrl(),
                                      repoConfig.getLayout() );
            remoteRepos.add( repo );
        }

        return remoteRepos;
    }

    private void saveConfiguration( Configuration config )
        throws Exception
    {
        try
        {
            archivaConfiguration.save( config );
        }
        catch ( RegistryException e )
        {
            throw new Exception( "Error occurred in the registry." );
        }
        catch ( IndeterminateConfigurationException e )
        {
            throw new Exception( "Error occurred while saving the configuration." );
        }
    }

    public Boolean addManagedRepository( String repoId, String layout, String name, String location,
                                         boolean blockRedeployments, boolean releasesIncluded,
                                         boolean snapshotsIncluded, boolean stageRepoNeeded, String cronExpression )
        throws Exception
    {

        Configuration config = archivaConfiguration.getConfiguration();

        CronExpressionValidator validator = new CronExpressionValidator();

        if ( config.getManagedRepositoriesAsMap().containsKey( repoId ) )
        {
            throw new Exception( "Unable to add new repository with id [" + repoId
                + "], that id already exists as a managed repository." );
        }
        else if ( config.getRemoteRepositoriesAsMap().containsKey( repoId ) )
        {
            throw new Exception( "Unable to add new repository with id [" + repoId
                + "], that id already exists as a remote repository." );
        }
        else if ( config.getRepositoryGroupsAsMap().containsKey( repoId ) )
        {
            throw new Exception( "Unable to add new repository with id [" + repoId
                + "], that id already exists as a repository group." );
        }

        if ( !validator.validate( cronExpression ) )
        {
            throw new Exception( "Invalid cron expression." );
        }

        ManagedRepositoryConfiguration repository = new ManagedRepositoryConfiguration();

        repository.setId( repoId );
        repository.setBlockRedeployments( blockRedeployments );
        repository.setReleases( releasesIncluded );
        repository.setSnapshots( snapshotsIncluded );
        repository.setName( name );
        repository.setLocation( location );
        repository.setLayout( layout );
        repository.setRefreshCronExpression( cronExpression );

        addRepository( repository, config );

        if ( stageRepoNeeded )
        {
            ManagedRepositoryConfiguration stagingRepository = getStageRepoConfig( repository );
            addRepository( stagingRepository, config );
        }

        saveConfiguration( config );
        return Boolean.TRUE;
    }

    public Boolean deleteManagedRepository( String repoId )
        throws Exception
    {
        Configuration config = archivaConfiguration.getConfiguration();

        ManagedRepositoryConfiguration repository = config.findManagedRepositoryById( repoId );

        if ( repository == null )
        {
            throw new Exception( "A repository with that id does not exist" );
        }

        metadataRepository.deleteRepository( repository.getId() );
        repositoryStatisticsManager.deleteStatistics( repository.getId() );
        config.removeManagedRepository( repository );

        try
        {
            saveConfiguration( config );
        }
        catch ( Exception e )
        {
            throw new Exception( "Error saving configuration for delete action" + e.getMessage() );
        }

        FileUtils.deleteDirectory( new File( repository.getLocation() ) );

        List<ProxyConnectorConfiguration> proxyConnectors = config.getProxyConnectors();
        for ( ProxyConnectorConfiguration proxyConnector : proxyConnectors )
        {
            if ( StringUtils.equals( proxyConnector.getSourceRepoId(), repository.getId() ) )
            {
                archivaConfiguration.getConfiguration().removeProxyConnector( proxyConnector );
            }
        }

        Map<String, List<String>> repoToGroupMap = archivaConfiguration.getConfiguration().getRepositoryToGroupMap();
        if ( repoToGroupMap != null )
        {
            if ( repoToGroupMap.containsKey( repository.getId() ) )
            {
                List<String> repoGroups = repoToGroupMap.get( repository.getId() );
                for ( String repoGroup : repoGroups )
                {
                    archivaConfiguration.getConfiguration().findRepositoryGroupById( repoGroup ).removeRepository( repository.getId() );
                }
            }
        }

        return Boolean.TRUE;
    }

    public ManagedRepository getManagedRepository( String repoId )
        throws Exception
    {
        Configuration config = archivaConfiguration.getConfiguration();
        ManagedRepositoryConfiguration managedRepository = config.findManagedRepositoryById( repoId );
        if ( managedRepository == null )
        {
            throw new Exception( "A repository with that id does not exist" );
        }
        ManagedRepository repo =
            new ManagedRepository( managedRepository.getId(), managedRepository.getName(), "URL",
                                   managedRepository.getLayout(), managedRepository.isSnapshots(),
                                   managedRepository.isReleases() );

        return repo;
    }

    public boolean merge( String repoId, boolean skipConflicts )
        throws Exception
    {
        String stagingId = repoId + STAGE;
        ManagedRepositoryConfiguration repoConfig;
        ManagedRepositoryConfiguration stagingConfig;

        Configuration config = archivaConfiguration.getConfiguration();
        repoConfig = config.findManagedRepositoryById( repoId );

        log.debug( "Retrieved repository configuration for repo '" + repoId + "'" );

        if ( repoConfig != null )
        {
            stagingConfig = config.findManagedRepositoryById( stagingId );

            if ( stagingConfig != null )
            {
                List<ArtifactMetadata> sourceArtifacts = metadataRepository.getArtifacts( stagingId );

                if ( repoConfig.isReleases() && !repoConfig.isSnapshots() )
                {
                    log.info( "Repository to be merged contains releases only.." );
                    if ( skipConflicts )
                    {
                        List<ArtifactMetadata> conflicts =
                            repositoryMerger.getConflictingArtifacts( repoId, stagingId );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Artifacts in conflict.." );
                            for( ArtifactMetadata metadata : conflicts )
                            {
                                log.debug( metadata.getNamespace() + ":" + metadata.getProject() + ":" +
                                    metadata.getProjectVersion() );
                            }
                        }

                        sourceArtifacts.removeAll( conflicts );

                        log.debug( "Source artifacts size :: " + sourceArtifacts.size() );
                        mergeWithOutSnapshots( sourceArtifacts, stagingId, repoId );
                    }
                    else
                    {
                        log.debug( "Source artifacts size :: " + sourceArtifacts.size() );
                        mergeWithOutSnapshots( sourceArtifacts, stagingId, repoId );
                    }
                }
                else
                {
                    log.info( "Repository to be merged has snapshot artifacts.." );
                    if ( skipConflicts )
                    {
                        List<ArtifactMetadata> conflicts =
                            repositoryMerger.getConflictingArtifacts( repoId, stagingId );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Artifacts in conflict.." );
                            for( ArtifactMetadata metadata : conflicts )
                            {
                                log.debug( metadata.getNamespace() + ":" + metadata.getProject() + ":" +
                                    metadata.getProjectVersion() );
                            }
                        }

                        sourceArtifacts.removeAll( conflicts );

                        log.debug( "Source artifacts size :: " + sourceArtifacts.size() );

                        Filter<ArtifactMetadata> artifactsWithOutConflicts =
                            new IncludesFilter<ArtifactMetadata>( sourceArtifacts );
                        repositoryMerger.merge( stagingId, repoId, artifactsWithOutConflicts );

                        log.info( "Staging repository '" + stagingId + "' merged successfully with managed repo '" +
                            repoId + "'." );
                    }
                    else
                    {
                        repositoryMerger.merge( stagingId, repoId );
                        
                        log.info( "Staging repository '" + stagingId + "' merged successfully with managed repo '" +
                            repoId + "'." );
                    }
                }
            }
            else
            {
                throw new Exception( "Staging Id : " + stagingId + " not found." );
            }
        }
        else
        {
            throw new Exception( "Repository Id : " + repoId + " not found." );
        }

        if ( !repositoryTaskScheduler.isProcessingRepositoryTask( repoId ) )
        {
            RepositoryTask task = new RepositoryTask();
            task.setRepositoryId( repoId );

            repositoryTaskScheduler.queueTask( task );
        }

        AuditEvent event = createAuditEvent( repoConfig );

        // add event for audit log reports
        metadataRepository.addMetadataFacet( event.getRepositoryId(), event );

        // log event in archiva audit log
        auditListener.auditEvent( createAuditEvent( repoConfig ) );

        return true;
    }

    protected void addRepository( ManagedRepositoryConfiguration repository, Configuration configuration )
        throws IOException
    {
        // Normalize the path
        File file = new File( repository.getLocation() );
        repository.setLocation( file.getCanonicalPath() );
        if ( !file.exists() )
        {
            file.mkdirs();
        }
        if ( !file.exists() || !file.isDirectory() )
        {
            throw new IOException( "Unable to add repository - no write access, can not create the root directory: "
                + file );
        }

        configuration.addManagedRepository( repository );
    }

    // todo: setting userid of audit event
    private AuditEvent createAuditEvent( ManagedRepositoryConfiguration repoConfig )
    {

        AuditEvent event = new AuditEvent();
        event.setAction( AuditEvent.MERGE_REPO_REMOTE );
        event.setRepositoryId( repoConfig.getId() );
        event.setResource( repoConfig.getLocation() );
        event.setTimestamp( new Date() );

        return event;
    }

    private void mergeWithOutSnapshots( List<ArtifactMetadata> sourceArtifacts, String sourceRepoId, String repoid )
        throws Exception
    {
        List<ArtifactMetadata> artifactsWithOutSnapshots = new ArrayList<ArtifactMetadata>();
        for ( ArtifactMetadata metadata : sourceArtifacts )
        {

            if ( metadata.getProjectVersion().contains( "SNAPSHOT" ) )
            {
                artifactsWithOutSnapshots.add( metadata );
            }

        }
        sourceArtifacts.removeAll( artifactsWithOutSnapshots );

        Filter<ArtifactMetadata> artifactListWithOutSnapShots = new IncludesFilter<ArtifactMetadata>( sourceArtifacts );

        repositoryMerger.merge( sourceRepoId, repoid, artifactListWithOutSnapShots );
    }

    private ManagedRepositoryConfiguration getStageRepoConfig( ManagedRepositoryConfiguration repository )
    {
        ManagedRepositoryConfiguration stagingRepository = new ManagedRepositoryConfiguration();
        stagingRepository.setId( repository.getId() + "-stage" );
        stagingRepository.setLayout( repository.getLayout() );
        stagingRepository.setName( repository.getName() + "-stage" );
        stagingRepository.setBlockRedeployments( repository.isBlockRedeployments() );
        stagingRepository.setDaysOlder( repository.getDaysOlder() );
        stagingRepository.setDeleteReleasedSnapshots( repository.isDeleteReleasedSnapshots() );
        stagingRepository.setIndexDir( repository.getIndexDir() );
        String path = repository.getLocation();
        int lastIndex = path.lastIndexOf( '/' );
        stagingRepository.setLocation( path.substring( 0, lastIndex ) + "/" + stagingRepository.getId() );
        stagingRepository.setRefreshCronExpression( repository.getRefreshCronExpression() );
        stagingRepository.setReleases( repository.isReleases() );
        stagingRepository.setRetentionCount( repository.getRetentionCount() );
        stagingRepository.setScanned( repository.isScanned() );
        stagingRepository.setSnapshots( repository.isSnapshots() );
        return stagingRepository;
    }
}
