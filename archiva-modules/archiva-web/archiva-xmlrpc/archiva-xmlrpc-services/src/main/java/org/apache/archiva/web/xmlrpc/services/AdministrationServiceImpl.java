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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.archiva.web.xmlrpc.api.AdministrationService;
import org.apache.archiva.web.xmlrpc.api.beans.ManagedRepository;
import org.apache.archiva.web.xmlrpc.api.beans.RemoteRepository;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.DatabaseScanningConfiguration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.constraints.ArtifactVersionsConstraint;
import org.apache.maven.archiva.database.updater.DatabaseCleanupConsumer;
import org.apache.maven.archiva.database.updater.DatabaseConsumers;
import org.apache.maven.archiva.database.updater.DatabaseUnprocessedArtifactConsumer;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.ContentNotFoundException;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.events.RepositoryListener;
import org.apache.maven.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.maven.archiva.scheduled.ArchivaTaskScheduler;
import org.apache.maven.archiva.scheduled.tasks.DatabaseTask;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTask;
import org.apache.maven.archiva.scheduled.tasks.TaskCreator;
import org.codehaus.plexus.registry.RegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        
    private DatabaseConsumers dbConsumersUtil;
            
    private RepositoryContentFactory repoFactory;
    
    private ArtifactDAO artifactDAO;
    
    private ArchivaTaskScheduler taskScheduler;
    
    private Collection<RepositoryListener> listeners;

    public AdministrationServiceImpl( ArchivaConfiguration archivaConfig, RepositoryContentConsumers repoConsumersUtil,
                                      DatabaseConsumers dbConsumersUtil, RepositoryContentFactory repoFactory,
                                      ArtifactDAO artifactDAO, ArchivaTaskScheduler taskScheduler,
                                      Collection<RepositoryListener> listeners )
    {
        this.archivaConfiguration = archivaConfig;
        this.repoConsumersUtil = repoConsumersUtil;
        this.dbConsumersUtil = dbConsumersUtil;
        this.repoFactory = repoFactory;
        this.artifactDAO = artifactDAO;
        this.taskScheduler = taskScheduler;
        this.listeners = listeners;
    }
        
    /**
     * @see AdministrationService#configureDatabaseConsumer(String, boolean)
     */
    public Boolean configureDatabaseConsumer( String consumerId, boolean enable ) throws Exception
    {
        List<DatabaseCleanupConsumer> cleanupConsumers = dbConsumersUtil.getAvailableCleanupConsumers();
        List<DatabaseUnprocessedArtifactConsumer> unprocessedConsumers =
            dbConsumersUtil.getAvailableUnprocessedConsumers();
        
        boolean found = false;
        boolean isCleanupConsumer = false;        
        for( DatabaseCleanupConsumer consumer : cleanupConsumers )
        {
            if( consumer.getId().equals( consumerId ) )
            {
                found = true;
                isCleanupConsumer = true;
                break;
            }
        }
        
        if( !found )
        {
            for( DatabaseUnprocessedArtifactConsumer consumer : unprocessedConsumers )
            {
                if( consumer.getId().equals( consumerId ) )
                {
                    found = true;
                    break;
                }
            }
        }
        
        if( !found )
        {
            throw new Exception( "Invalid database consumer." );
        }
        
        Configuration config = archivaConfiguration.getConfiguration();
        DatabaseScanningConfiguration dbScanningConfig = config.getDatabaseScanning();
        
        if( isCleanupConsumer )
        {
            dbScanningConfig.addCleanupConsumer( consumerId );            
        }
        else
        {
            dbScanningConfig.addUnprocessedConsumer( consumerId );
        }
        
        config.setDatabaseScanning( dbScanningConfig );        
        saveConfiguration( config );
        
        return new Boolean( true );
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
        for( KnownRepositoryContentConsumer consumer : knownConsumers )
        {
            if( consumer.getId().equals( consumerId ) )
            {
                found = true;
                isKnownContentConsumer = true;
                break;
            }
        }
        
        if( !found )
        {
            for( InvalidRepositoryContentConsumer consumer : invalidConsumers )
            {
                if( consumer.getId().equals( consumerId ) )
                {
                    found = true;
                    break;
                }
            }
        }
        
        if( !found )
        {
            throw new Exception( "Invalid repository consumer." );
        }
        
        Configuration config = archivaConfiguration.getConfiguration();
        RepositoryScanningConfiguration repoScanningConfig = config.getRepositoryScanning();
        
        if( isKnownContentConsumer )
        {
            repoScanningConfig.addKnownContentConsumer( consumerId );
        }
        else
        {
            repoScanningConfig.addInvalidContentConsumer( consumerId );
        }
        
        config.setRepositoryScanning( repoScanningConfig );        
        saveConfiguration( config );
        
        return new Boolean( true );
    }
    
    /**
     * @see AdministrationService#deleteArtifact(String, String, String, String)
     */
    public Boolean deleteArtifact( String repoId, String groupId, String artifactId, String version )
        throws Exception
    {
        Configuration config = archivaConfiguration.getConfiguration();
        ManagedRepositoryConfiguration repoConfig = config.findManagedRepositoryById( repoId );
        
        if( repoConfig == null )
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
            
            ArtifactVersionsConstraint constraint = new ArtifactVersionsConstraint( repoId, groupId, artifactId, false );
            List<ArchivaArtifact> artifacts = null;
            
            try
            {
                artifacts = artifactDAO.queryArtifacts( constraint );
            }
            catch ( ArchivaDatabaseException e )
            {
                throw new Exception( "Error occurred while cleaning up database." );
            }            
               
            if ( artifacts != null )
            {
                for ( ArchivaArtifact artifact : artifacts )
                {
                    if ( artifact.getVersion().equals( version ) )
                    {
                        for ( RepositoryListener listener : listeners )
                        {
                            listener.deleteArtifact( repoContent, artifact );
                        }
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
        
        return new Boolean( true );
    }

    /**
     * @see AdministrationService#executeDatabaseScanner()
     */
    public Boolean executeDatabaseScanner() throws Exception
    {
        if ( taskScheduler.isProcessingDatabaseTask() )
        {
            return false;
        }

        log.info( "Queueing database task on request from administration service" );
        DatabaseTask task = new DatabaseTask();
        
        taskScheduler.queueDatabaseTask( task );           
        
        return new Boolean( true );
    }

    /**
     * @see AdministrationService#executeRepositoryScanner(String)
     */
    public Boolean executeRepositoryScanner( String repoId ) throws Exception
    {
        Configuration config = archivaConfiguration.getConfiguration();
        if( config.findManagedRepositoryById( repoId ) == null )
        {
            throw new Exception( "Repository does not exist." );
        }
        
        if ( taskScheduler.isProcessingRepositoryTask( repoId ) )
        {
            return false;
        }

        RepositoryTask task = TaskCreator.createRepositoryTask( repoId );

        taskScheduler.queueRepositoryTask( task );          
        
        return new Boolean( true );
    }

    /**
     * @see AdministrationService#getAllDatabaseConsumers()
     */
    public List<String> getAllDatabaseConsumers()
    {
        List<String> consumers = new ArrayList<String>();
        
        List<DatabaseCleanupConsumer> cleanupConsumers = dbConsumersUtil.getAvailableCleanupConsumers();
        List<DatabaseUnprocessedArtifactConsumer> unprocessedConsumers = dbConsumersUtil.getAvailableUnprocessedConsumers();
        
        for( DatabaseCleanupConsumer consumer : cleanupConsumers )
        {
            consumers.add( consumer.getId() );
        }  
        
        for( DatabaseUnprocessedArtifactConsumer consumer : unprocessedConsumers )
        {
            consumers.add( consumer.getId() );
        } 
        
        return consumers;
    }

    /**
     * @see AdministrationService#getAllRepositoryConsumers()
     */
    public List<String> getAllRepositoryConsumers()
    {
        List<String> consumers = new ArrayList<String>();
                
        List<KnownRepositoryContentConsumer> knownConsumers = repoConsumersUtil.getAvailableKnownConsumers();
        List<InvalidRepositoryContentConsumer> invalidConsumers = repoConsumersUtil.getAvailableInvalidConsumers();
        
        for( KnownRepositoryContentConsumer consumer : knownConsumers )
        {
            consumers.add( consumer.getId() );
        }
        
        for( InvalidRepositoryContentConsumer consumer : invalidConsumers )
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
        
        for( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
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
        
        for( RemoteRepositoryConfiguration repoConfig : remoteRepoConfigs )
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
        catch(  RegistryException e )
        {
            throw new Exception( "Error occurred in the registry." );
        }
        catch ( IndeterminateConfigurationException e )
        {
            throw new Exception( "Error occurred while saving the configuration." );    
        }
    }
}
