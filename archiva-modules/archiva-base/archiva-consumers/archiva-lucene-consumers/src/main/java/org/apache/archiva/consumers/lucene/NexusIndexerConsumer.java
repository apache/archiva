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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.indexing.ArtifactIndexingTask;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Consumer for indexing the repository to provide search and IDE integration features.
 */
public class NexusIndexerConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer, RegistryListener
{
    private Logger log = LoggerFactory.getLogger( NexusIndexerConsumer.class );

    private ArchivaConfiguration configuration;

    private FileTypes filetypes;

    private File managedRepository;

    private ArchivaTaskScheduler<ArtifactIndexingTask> scheduler;

    private IndexingContext context;

    private NexusIndexer nexusIndexer;

    private List<String> includes = new ArrayList<String>();

    private ManagedRepository repository;

    private List<? extends IndexCreator> allIndexCreators;

    public NexusIndexerConsumer( ArchivaTaskScheduler<ArtifactIndexingTask> scheduler,
                                 ArchivaConfiguration configuration, FileTypes filetypes,
                                 PlexusSisuBridge plexusSisuBridge, MavenIndexerUtils mavenIndexerUtils )
        throws PlexusSisuBridgeException
    {
        this.configuration = configuration;
        this.filetypes = filetypes;
        this.scheduler = scheduler;
        this.nexusIndexer = plexusSisuBridge.lookup( NexusIndexer.class );
        this.allIndexCreators = mavenIndexerUtils.getAllIndexCreators();
    }

    public String getDescription()
    {
        return "Indexes the repository to provide search and IDE integration features";
    }

    public String getId()
    {
        return "index-content";
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void beginScan( ManagedRepository repository, Date whenGathered )
        throws ConsumerException
    {
        this.repository = repository;
        managedRepository = new File( repository.getLocation() );

        try
        {
            log.info( "Creating indexing context for repo : {}", repository.getId() );
            context = ArtifactIndexingTask.createContext( repository, nexusIndexer, allIndexCreators );
        }
        catch ( IOException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }
        catch ( UnsupportedExistingLuceneIndexException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }
    }

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

    public void processFile( String path )
        throws ConsumerException
    {
        File artifactFile = new File( managedRepository, path );

        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repository, artifactFile, ArtifactIndexingTask.Action.ADD, context );
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
                new ArtifactIndexingTask( repository, artifactFile, ArtifactIndexingTask.Action.ADD, context, false );
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

    public void completeScan()
    {
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
        context = null;
    }

    public void completeScan( boolean executeOnEntireRepo )
    {
        if ( executeOnEntireRepo )
        {
            completeScan();
        }

        // else, do nothing as the context will be closed when indexing task is executed if not a repo scan request!
    }

    public List<String> getExcludes()
    {
        return Collections.emptyList();
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositoryScanning( propertyName ) )
        {
            initIncludes();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void initIncludes()
    {
        includes.clear();

        includes.addAll( filetypes.getFileTypePatterns( FileTypes.INDEXABLE_CONTENT ) );

        includes.addAll( filetypes.getFileTypePatterns( FileTypes.ARTIFACTS ) );
    }

    @PostConstruct
    public void initialize()
    {
        configuration.addChangeListener( this );

        initIncludes();
    }

    public List<String> getIncludes()
    {
        return includes;
    }
}
