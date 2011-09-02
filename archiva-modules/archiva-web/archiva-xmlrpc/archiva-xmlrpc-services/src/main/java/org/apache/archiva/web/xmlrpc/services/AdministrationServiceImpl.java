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

import org.apache.archiva.admin.AuditInformation;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.managed.ManagedRepositoryAdmin;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
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

    private RepositoryStatisticsManager repositoryStatisticsManager;

    private RepositoryMerger repositoryMerger;

    private static final String STAGE = "-stage";

    private AuditListener auditListener;

    private RepositorySessionFactory repositorySessionFactory;

    private ManagedRepositoryAdmin managedRepositoryAdmin;

    private static final String REPOSITORY_ID_VALID_EXPRESSION = "^[a-zA-Z0-9._-]+$";

    private static final String REPOSITORY_NAME_VALID_EXPRESSION = "^([a-zA-Z0-9.)/_(-]|\\s)+$";

    private static final String REPOSITORY_LOCATION_VALID_EXPRESSION = "^[-a-zA-Z0-9._/~:?!&amp;=\\\\]+$";

    public AdministrationServiceImpl( ArchivaConfiguration archivaConfig, RepositoryContentConsumers repoConsumersUtil,
                                      RepositoryContentFactory repoFactory,
                                      RepositorySessionFactory repositorySessionFactory,
                                      RepositoryArchivaTaskScheduler repositoryTaskScheduler,
                                      Collection<RepositoryListener> listeners,
                                      RepositoryStatisticsManager repositoryStatisticsManager,
                                      RepositoryMerger repositoryMerger, AuditListener auditListener,
                                      ManagedRepositoryAdmin managedRepositoryAdmin )
    {
        this.archivaConfiguration = archivaConfig;
        this.repoConsumersUtil = repoConsumersUtil;
        this.repoFactory = repoFactory;
        this.repositoryTaskScheduler = repositoryTaskScheduler;
        this.listeners = listeners;
        this.repositorySessionFactory = repositorySessionFactory;
        this.repositoryStatisticsManager = repositoryStatisticsManager;
        this.repositoryMerger = repositoryMerger;
        this.auditListener = auditListener;
        this.managedRepositoryAdmin = managedRepositoryAdmin;
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

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            ManagedRepositoryContent repoContent = repoFactory.getManagedRepositoryContent( repoId );
            VersionedReference ref = new VersionedReference();
            ref.setGroupId( groupId );
            ref.setArtifactId( artifactId );
            ref.setVersion( version );

            // delete from file system
            repoContent.deleteVersion( ref );

            MetadataRepository metadataRepository = repositorySession.getRepository();
            Collection<ArtifactMetadata> artifacts =
                metadataRepository.getArtifacts( repoId, groupId, artifactId, version );

            for ( ArtifactMetadata artifact : artifacts )
            {
                // TODO: mismatch between artifact (snapshot) version and project (base) version here
                if ( artifact.getVersion().equals( version ) )
                {
                    metadataRepository.removeArtifact( artifact.getRepositoryId(), artifact.getNamespace(),
                                                       artifact.getProject(), artifact.getVersion(), artifact.getId() );

                    // TODO: move into the metadata repository proper - need to differentiate attachment of
                    // repository metadata to an artifact
                    for ( RepositoryListener listener : listeners )
                    {
                        listener.deleteArtifact( metadataRepository, repoId, artifact.getNamespace(),
                                                 artifact.getProject(), artifact.getVersion(), artifact.getId() );
                    }
                }
            }
            repositorySession.save();
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
        finally
        {
            repositorySession.close();
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
        throws RepositoryAdminException
    {
        List<ManagedRepository> managedRepos = new ArrayList<ManagedRepository>();

        for ( org.apache.archiva.admin.repository.managed.ManagedRepository repoConfig : managedRepositoryAdmin.getManagedRepositories() )
        {
            ManagedRepository repo =
                new ManagedRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getLocation(),
                                       repoConfig.getLayout(), repoConfig.isSnapshots(), repoConfig.isReleases() );
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
            RemoteRepository repo = new RemoteRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getUrl(),
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

        org.apache.archiva.admin.repository.managed.ManagedRepository repository =
            new org.apache.archiva.admin.repository.managed.ManagedRepository( repoId, name, location, layout,
                                                                               snapshotsIncluded, releasesIncluded,
                                                                               blockRedeployments, cronExpression );
        return managedRepositoryAdmin.addManagedRepository( repository, stageRepoNeeded, getAuditInformation() );

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

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            metadataRepository.removeRepository( repository.getId() );
            repositoryStatisticsManager.deleteStatistics( metadataRepository, repository.getId() );
            repositorySession.save();
        }
        finally
        {
            repositorySession.close();
        }
        config.removeManagedRepository( repository );

        try
        {
            saveConfiguration( config );
        }
        catch ( Exception e )
        {
            throw new Exception( "Error saving configuration for delete action" + e.getMessage() );
        }

        File dir = new File( repository.getLocation() );
        if ( !FileUtils.deleteQuietly( dir ) )
        {
            throw new IOException( "Cannot delete repository " + dir );
        }

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
                    archivaConfiguration.getConfiguration().findRepositoryGroupById( repoGroup ).removeRepository(
                        repository.getId() );
                }
            }
        }

        return Boolean.TRUE;
    }

    public Boolean deleteManagedRepositoryContent( String repoId )
        throws Exception
    {
        Configuration config = archivaConfiguration.getConfiguration();

        ManagedRepositoryConfiguration repository = config.findManagedRepositoryById( repoId );

        if ( repository == null )
        {
            throw new Exception( "Repository Id : " + repoId + " not found." );
        }

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            metadataRepository.removeRepository( repository.getId() );
            repositorySession.save();
        }
        finally
        {
            repositorySession.close();
        }

        File repoDir = new File( repository.getLocation() );
        File[] children = repoDir.listFiles();

        if ( children != null )
        {
            for ( File child : children )
            {
                FileUtils.deleteQuietly( child );
            }

            if ( repoDir.listFiles().length > 0 )
            {
                throw new IOException( "Cannot delete repository contents of " + repoDir );
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
        ManagedRepository repo = new ManagedRepository( managedRepository.getId(), managedRepository.getName(), "URL",
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

        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
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
                                repositoryMerger.getConflictingArtifacts( metadataRepository, repoId, stagingId );

                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "Artifacts in conflict.." );
                                for ( ArtifactMetadata metadata : conflicts )
                                {
                                    log.debug( metadata.getNamespace() + ":" + metadata.getProject() + ":"
                                                   + metadata.getProjectVersion() );
                                }
                            }

                            sourceArtifacts.removeAll( conflicts );

                            log.debug( "Source artifacts size :: " + sourceArtifacts.size() );
                            mergeWithOutSnapshots( sourceArtifacts, stagingId, repoId, metadataRepository );
                        }
                        else
                        {
                            log.debug( "Source artifacts size :: " + sourceArtifacts.size() );
                            mergeWithOutSnapshots( sourceArtifacts, stagingId, repoId, metadataRepository );
                        }
                    }
                    else
                    {
                        log.info( "Repository to be merged has snapshot artifacts.." );
                        if ( skipConflicts )
                        {
                            List<ArtifactMetadata> conflicts =
                                repositoryMerger.getConflictingArtifacts( metadataRepository, repoId, stagingId );

                            if ( log.isDebugEnabled() )
                            {
                                log.debug( "Artifacts in conflict.." );
                                for ( ArtifactMetadata metadata : conflicts )
                                {
                                    log.debug( metadata.getNamespace() + ":" + metadata.getProject() + ":"
                                                   + metadata.getProjectVersion() );
                                }
                            }

                            sourceArtifacts.removeAll( conflicts );

                            log.debug( "Source artifacts size :: " + sourceArtifacts.size() );

                            Filter<ArtifactMetadata> artifactsWithOutConflicts =
                                new IncludesFilter<ArtifactMetadata>( sourceArtifacts );
                            repositoryMerger.merge( metadataRepository, stagingId, repoId, artifactsWithOutConflicts );

                            log.info( "Staging repository '" + stagingId + "' merged successfully with managed repo '"
                                          + repoId + "'." );
                        }
                        else
                        {
                            repositoryMerger.merge( metadataRepository, stagingId, repoId );

                            log.info( "Staging repository '" + stagingId + "' merged successfully with managed repo '"
                                          + repoId + "'." );
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
            repositorySession.save();
        }
        finally
        {
            repositorySession.close();
        }

        return true;
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

    private void mergeWithOutSnapshots( List<ArtifactMetadata> sourceArtifacts, String sourceRepoId, String repoid,
                                        MetadataRepository metadataRepository )
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

        repositoryMerger.merge( metadataRepository, sourceRepoId, repoid, artifactListWithOutSnapShots );
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

    // FIXME find a way to get user id and adress
    private AuditInformation getAuditInformation()
    {
        return new AuditInformation( null, null );
    }
}
