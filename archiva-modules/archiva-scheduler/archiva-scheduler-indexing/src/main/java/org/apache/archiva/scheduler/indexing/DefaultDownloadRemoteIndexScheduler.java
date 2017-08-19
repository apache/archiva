package org.apache.archiva.scheduler.indexing;
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.NetworkProxy;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.networkproxy.NetworkProxyAdmin;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorAdmin;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.common.ArchivaException;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.ConfigurationEvent;
import org.apache.archiva.configuration.ConfigurationListener;
import org.apache.archiva.proxy.common.WagonFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.updater.IndexUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
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
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    private WagonFactory wagonFactory;

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    @Inject
    private ProxyConnectorAdmin proxyConnectorAdmin;

    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    @Inject
    private NexusIndexer nexusIndexer;

    @Inject
    private IndexUpdater indexUpdater;

    @Inject
    private IndexPacker indexPacker;

    // store ids about currently running remote download : updated in DownloadRemoteIndexTask
    private List<String> runningRemoteDownloadIds = new CopyOnWriteArrayList<String>();

    @PostConstruct
    public void startup()
        throws ArchivaException, RepositoryAdminException, PlexusSisuBridgeException, IOException,
        UnsupportedExistingLuceneIndexException, DownloadRemoteIndexException
    {
        archivaConfiguration.addListener( this );
        // TODO add indexContexts even if null

        for ( RemoteRepository remoteRepository : remoteRepositoryAdmin.getRemoteRepositories() )
        {
            String contextKey = "remote-" + remoteRepository.getId();
            IndexingContext context = nexusIndexer.getIndexingContexts().get( contextKey );
            if ( context == null )
            {
                continue;
            }

            // TODO record jobs from configuration
            if ( remoteRepository.isDownloadRemoteIndex() && StringUtils.isNotEmpty(
                remoteRepository.getCronExpression() ) )
            {
                boolean fullDownload = context.getIndexDirectoryFile().list().length == 0;
                scheduleDownloadRemote( remoteRepository.getId(), false, fullDownload );
            }
        }


    }

    @PreDestroy
    public void shutdown()
        throws RepositoryAdminException, IOException
    {
        for ( RemoteRepository remoteRepository : remoteRepositoryAdmin.getRemoteRepositories() )
        {
            String contextKey = "remote-" + remoteRepository.getId();
            IndexingContext context = nexusIndexer.getIndexingContexts().get( contextKey );
            if ( context == null )
            {
                continue;
            }
            nexusIndexer.removeIndexingContext( context, false );
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
        try
        {
            RemoteRepository remoteRepository = remoteRepositoryAdmin.getRemoteRepository( repositoryId );
            if ( remoteRepository == null )
            {
                log.warn( "ignore scheduleDownloadRemote for repo with id {} as not exists", repositoryId );
                return;
            }
            NetworkProxy networkProxy = null;
            if ( StringUtils.isNotBlank( remoteRepository.getRemoteDownloadNetworkProxyId() ) )
            {
                networkProxy = networkProxyAdmin.getNetworkProxy( remoteRepository.getRemoteDownloadNetworkProxyId() );
                if ( networkProxy == null )
                {
                    log.warn(
                        "your remote repository is configured to download remote index trought a proxy we cannot find id:{}",
                        remoteRepository.getRemoteDownloadNetworkProxyId() );
                }
            }

            DownloadRemoteIndexTaskRequest downloadRemoteIndexTaskRequest = new DownloadRemoteIndexTaskRequest() //
                .setRemoteRepository( remoteRepository ) //
                .setNetworkProxy( networkProxy ) //
                .setFullDownload( fullDownload ) //
                .setWagonFactory( wagonFactory ) //
                .setRemoteRepositoryAdmin( remoteRepositoryAdmin ) //
                .setIndexUpdater( indexUpdater ) //
                .setIndexPacker( this.indexPacker );

            if ( now )
            {
                log.info( "schedule download remote index for repository {}", remoteRepository.getId() );
                // do it now
                taskScheduler.schedule(
                    new DownloadRemoteIndexTask( downloadRemoteIndexTaskRequest, this.runningRemoteDownloadIds ),
                    new Date() );
            }
            else
            {
                log.info( "schedule download remote index for repository {} with cron expression {}",
                          remoteRepository.getId(), remoteRepository.getCronExpression() );
                try
                {
                    CronTrigger cronTrigger = new CronTrigger( remoteRepository.getCronExpression() );
                    taskScheduler.schedule(
                        new DownloadRemoteIndexTask( downloadRemoteIndexTaskRequest, this.runningRemoteDownloadIds ),
                        cronTrigger );
                }
                catch ( IllegalArgumentException e )
                {
                    log.warn( "Unable to schedule remote index download: {}", e.getLocalizedMessage() );
                }

                if ( remoteRepository.isDownloadRemoteIndexOnStartup() )
                {
                    log.info(
                        "remote repository {} configured with downloadRemoteIndexOnStartup schedule now a download",
                        remoteRepository.getId() );
                    taskScheduler.schedule(
                        new DownloadRemoteIndexTask( downloadRemoteIndexTaskRequest, this.runningRemoteDownloadIds ),
                        new Date() );
                }
            }

        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            throw new DownloadRemoteIndexException( e.getMessage(), e );
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
