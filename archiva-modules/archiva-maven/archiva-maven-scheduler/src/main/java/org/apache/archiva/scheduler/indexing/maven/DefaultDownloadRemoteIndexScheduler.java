package org.apache.archiva.scheduler.indexing.maven;
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

import org.apache.archiva.proxy.ProxyRegistry;
import org.apache.archiva.proxy.model.NetworkProxy;
import org.apache.archiva.scheduler.indexing.DownloadRemoteIndexException;
import org.apache.archiva.scheduler.indexing.DownloadRemoteIndexScheduler;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationEvent;
import org.apache.archiva.configuration.ConfigurationListener;
import org.apache.archiva.indexer.UnsupportedBaseContextException;
import org.apache.archiva.proxy.maven.WagonFactory;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.features.RemoteIndexFeature;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.updater.IndexUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service( "downloadRemoteIndexScheduler#default" )
public class DefaultDownloadRemoteIndexScheduler
    implements ConfigurationListener, DownloadRemoteIndexScheduler
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "taskScheduler#indexDownloadRemote" )
    private TaskScheduler taskScheduler;

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    private WagonFactory wagonFactory;

    @Inject
    private IndexUpdater indexUpdater;

    @Inject
    private IndexPacker indexPacker;

    @Inject
    private ProxyRegistry proxyRegistry;

    // store ids about currently running remote download : updated in DownloadRemoteIndexTask
    private List<String> runningRemoteDownloadIds = new CopyOnWriteArrayList<String>();

    @PostConstruct
    public void startup()
            throws
        DownloadRemoteIndexException, UnsupportedBaseContextException {
        archivaConfiguration.addListener( this );
        // TODO add indexContexts even if null

        for ( org.apache.archiva.repository.RemoteRepository remoteRepository : repositoryRegistry.getRemoteRepositories() )
        {
            String contextKey = "remote-" + remoteRepository.getId();
            IndexingContext context = remoteRepository.getIndexingContext().getBaseContext(IndexingContext.class);
            if ( context == null )
            {
                continue;
            }
            RemoteIndexFeature rif = remoteRepository.getFeature(RemoteIndexFeature.class).get();


            // TODO record jobs from configuration
            if ( rif.isDownloadRemoteIndex() && StringUtils.isNotEmpty(
                remoteRepository.getSchedulingDefinition() ) )
            {
                boolean fullDownload = context.getIndexDirectoryFile().list().length == 0;
                scheduleDownloadRemote( remoteRepository.getId(), false, fullDownload );
            }
        }


    }

    @Override
    public void configurationEvent( ConfigurationEvent event )
    {
        // TODO remove jobs and add again
    }


    @Override
    public void scheduleDownloadRemote( String repositoryId, boolean now, boolean fullDownload )
        throws DownloadRemoteIndexException
    {
        org.apache.archiva.repository.RemoteRepository remoteRepo = repositoryRegistry.getRemoteRepository(repositoryId);

        if ( remoteRepo == null )
        {
            log.warn( "ignore scheduleDownloadRemote for repo with id {} as not exists", repositoryId );
            return;
        }
        if (!remoteRepo.supportsFeature(RemoteIndexFeature.class)) {
            log.warn("ignore scheduleDownloadRemote for repo with id {}. Does not support remote index.", repositoryId);
            return;
        }
        RemoteIndexFeature rif = remoteRepo.getFeature(RemoteIndexFeature.class).get();
        NetworkProxy networkProxy = null;
        if ( StringUtils.isNotBlank( rif.getProxyId() ) )
        {
            networkProxy = proxyRegistry.getNetworkProxy( rif.getProxyId() );
            if ( networkProxy == null )
            {
                log.warn(
                    "your remote repository is configured to download remote index trought a proxy we cannot find id:{}",
                    rif.getProxyId() );
            }
        }

        DownloadRemoteIndexTaskRequest downloadRemoteIndexTaskRequest = new DownloadRemoteIndexTaskRequest() //
            .setRemoteRepository( remoteRepo ) //
            .setNetworkProxy( networkProxy ) //
            .setFullDownload( fullDownload ) //
            .setWagonFactory( wagonFactory ) //
            .setIndexUpdater( indexUpdater ) //
            .setIndexPacker( this.indexPacker );

        if ( now )
        {
            log.info( "schedule download remote index for repository {}", remoteRepo.getId() );
            // do it now
            taskScheduler.schedule(
                new DownloadRemoteIndexTask( downloadRemoteIndexTaskRequest, this.runningRemoteDownloadIds ),
                new Date() );
        }
        else
        {
            log.info( "schedule download remote index for repository {} with cron expression {}",
                      remoteRepo.getId(), remoteRepo.getSchedulingDefinition());
            try
            {
                CronTrigger cronTrigger = new CronTrigger( remoteRepo.getSchedulingDefinition());
                taskScheduler.schedule(
                    new DownloadRemoteIndexTask( downloadRemoteIndexTaskRequest, this.runningRemoteDownloadIds ),
                    cronTrigger );
            }
            catch ( IllegalArgumentException e )
            {
                log.warn( "Unable to schedule remote index download: {}", e.getLocalizedMessage() );
            }

            if ( rif.isDownloadRemoteIndexOnStartup() )
            {
                log.info(
                    "remote repository {} configured with downloadRemoteIndexOnStartup schedule now a download",
                    remoteRepo.getId() );
                taskScheduler.schedule(
                    new DownloadRemoteIndexTask( downloadRemoteIndexTaskRequest, this.runningRemoteDownloadIds ),
                    new Date() );
            }
        }

    }

    public TaskScheduler getTaskScheduler()
    {
        return taskScheduler;
    }

    public void setTaskScheduler( TaskScheduler taskScheduler )
    {
        this.taskScheduler = taskScheduler;
    }

    @Override
    public List<String> getRunningRemoteDownloadIds()
    {
        return runningRemoteDownloadIds;
    }
}
