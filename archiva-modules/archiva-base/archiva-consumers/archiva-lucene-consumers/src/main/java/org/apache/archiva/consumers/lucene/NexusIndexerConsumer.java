package org.apache.archiva.consumers.lucene;

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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationNames;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.redback.components.registry.Registry;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.indexing.ArtifactIndexingTask;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.archiva.redback.components.registry.RegistryListener;
import org.apache.archiva.redback.components.taskqueue.TaskQueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Consumer for indexing the repository to provide search and IDE integration features.
 */
@Service( "knownRepositoryContentConsumer#index-content" )
@Scope( "prototype" )
public class NexusIndexerConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    private ArchivaConfiguration configuration;

    private FileTypes filetypes;

    private File managedRepository;

    private ArchivaTaskScheduler<ArtifactIndexingTask> scheduler;

    private IndexingContext indexingContext;

    private NexusIndexer nexusIndexer;

    private List<String> includes = new ArrayList<>( 0 );

    private ManagedRepository repository;

    private List<? extends IndexCreator> allIndexCreators;

    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    public NexusIndexerConsumer(
        @Named( value = "archivaTaskScheduler#indexing" ) ArchivaTaskScheduler<ArtifactIndexingTask> scheduler,
        @Named( value = "archivaConfiguration" ) ArchivaConfiguration configuration, FileTypes filetypes,
        PlexusSisuBridge plexusSisuBridge, MavenIndexerUtils mavenIndexerUtils,
        ManagedRepositoryAdmin managedRepositoryAdmin )
        throws PlexusSisuBridgeException
    {
        this.configuration = configuration;
        this.filetypes = filetypes;
        this.scheduler = scheduler;
        this.nexusIndexer = plexusSisuBridge.lookup( NexusIndexer.class );
        this.allIndexCreators = mavenIndexerUtils.getAllIndexCreators();
        this.managedRepositoryAdmin = managedRepositoryAdmin;
    }

    @Override
    public String getDescription()
    {
        return "Indexes the repository to provide search and IDE integration features";
    }

    @Override
    public String getId()
    {
        return "index-content";
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        this.repository = repository;
        managedRepository = new File( repository.getLocation() );

        try
        {
            log.info( "Creating indexing context for repo : {}", repository.getId() );
            indexingContext = managedRepositoryAdmin.createIndexContext( repository );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }
    }

    @Override
    public void beginScan( ManagedRepository repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        if ( executeOnEntireRepo )
        {
            beginScan( repository, whenGathered );
        }
        else
        {
            this.repository = repository;
            managedRepository = new File( repository.getLocation() );
        }
    }

    @Override
    public void processFile( String path )
        throws ConsumerException
    {
        File artifactFile = new File( managedRepository, path );

        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repository, artifactFile, ArtifactIndexingTask.Action.ADD, getIndexingContext() );
        try
        {
            log.debug( "Queueing indexing task '{}' to add or update the artifact in the index.", task );
            scheduler.queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }
    }

    @Override
    public void processFile( String path, boolean executeOnEntireRepo )
        throws Exception
    {
        if ( executeOnEntireRepo )
        {
            processFile( path );
        }
        else
        {
            File artifactFile = new File( managedRepository, path );

            // specify in indexing task that this is not a repo scan request!
            ArtifactIndexingTask task =
                new ArtifactIndexingTask( repository, artifactFile, ArtifactIndexingTask.Action.ADD,
                                          getIndexingContext(), false );
            // only update index we don't need to scan the full repo here
            task.setOnlyUpdate( true );
            try
            {
                log.debug( "Queueing indexing task '{}' to add or update the artifact in the index.", task );
                scheduler.queueTask( task );
            }
            catch ( TaskQueueException e )
            {
                throw new ConsumerException( e.getMessage(), e );
            }
        }
    }

    @Override
    public void completeScan()
    {
        IndexingContext context = this.indexingContext;
        if ( context == null )
        {
            try
            {
                context = getIndexingContext();
            }
            catch ( ConsumerException e )
            {
                log.warn( "failed to get an IndexingContext:{}", e.getMessage() );
                return;
            }
        }
        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repository, null, ArtifactIndexingTask.Action.FINISH, context );
        try
        {
            log.debug( "Queueing indexing task '{}' to finish indexing.", task );
            scheduler.queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "Error queueing task: " + task + ": " + e.getMessage(), e );
        }
    }

    @Override
    public void completeScan( boolean executeOnEntireRepo )
    {
        if ( executeOnEntireRepo )
        {
            completeScan();
        }

        // else, do nothing as the context will be closed when indexing task is executed if not a repo scan request!
    }

    @Override
    public List<String> getExcludes()
    {
        return Collections.emptyList();
    }

    @Override
    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositoryScanning( propertyName ) )
        {
            initIncludes();
        }
    }

    @Override
    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void initIncludes()
    {
        List<String> indexable = filetypes.getFileTypePatterns( FileTypes.INDEXABLE_CONTENT );
        List<String> artifacts = filetypes.getFileTypePatterns( FileTypes.ARTIFACTS );

        includes = new ArrayList<>( indexable.size() + artifacts.size() );

        includes.addAll( indexable );

        includes.addAll( artifacts );
    }

    @PostConstruct
    public void initialize()
    {
        configuration.addChangeListener( this );

        initIncludes();
    }

    @Override
    public List<String> getIncludes()
    {
        return includes;
    }


    private IndexingContext getIndexingContext()
        throws ConsumerException
    {

        if ( this.indexingContext == null )
        {
            try
            {
                indexingContext = managedRepositoryAdmin.createIndexContext( repository );
            }
            catch ( RepositoryAdminException e )
            {
                throw new ConsumerException( e.getMessage(), e );
            }
        }
        return indexingContext;
    }
}
