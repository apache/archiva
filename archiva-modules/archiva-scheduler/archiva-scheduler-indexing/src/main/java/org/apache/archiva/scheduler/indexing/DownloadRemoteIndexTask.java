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

import org.apache.archiva.admin.model.beans.NetworkProxy;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.proxy.common.WagonFactory;
import org.apache.archiva.proxy.common.WagonFactoryException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
public class DownloadRemoteIndexTask
    implements Runnable
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    private RemoteRepository remoteRepository;

    private NexusIndexer nexusIndexer;

    private WagonFactory wagonFactory;

    private NetworkProxy networkProxy;

    private boolean fullDownload;

    private List<String> runningRemoteDownloadIds;

    private IndexUpdater indexUpdater;

    public DownloadRemoteIndexTask( DownloadRemoteIndexTaskRequest downloadRemoteIndexTaskRequest,
                                    List<String> runningRemoteDownloadIds )
    {
        this.remoteRepository = downloadRemoteIndexTaskRequest.getRemoteRepository();
        this.nexusIndexer = downloadRemoteIndexTaskRequest.getNexusIndexer();
        this.wagonFactory = downloadRemoteIndexTaskRequest.getWagonFactory();
        this.networkProxy = downloadRemoteIndexTaskRequest.getNetworkProxy();
        this.fullDownload = downloadRemoteIndexTaskRequest.isFullDownload();
        this.runningRemoteDownloadIds = runningRemoteDownloadIds;
        this.indexUpdater = downloadRemoteIndexTaskRequest.getIndexUpdater();
    }

    public void run()
    {

        // so short lock : not sure we need it
        synchronized ( this.runningRemoteDownloadIds )
        {
            if ( this.runningRemoteDownloadIds.contains( this.remoteRepository.getId() ) )
            {
                // skip it as it's running
                log.info( "skip download index remote for repo {} it's already running",
                          this.remoteRepository.getId() );
                return;
            }
            log.info( "start download remote index for remote repository " + this.remoteRepository.getId() );
            this.runningRemoteDownloadIds.add( this.remoteRepository.getId() );
        }
        IndexingContext indexingContext =
            nexusIndexer.getIndexingContexts().get( "remote-" + remoteRepository.getId() );

        // TODO check if null ? normally not as created by DefaultDownloadRemoteIndexScheduler#startup

        // create a temp directory to download files
        final File tempIndexDirectory = new File( indexingContext.getIndexDirectoryFile().getParent(), ".tmpIndex" );
        try
        {
            if ( tempIndexDirectory.exists() )
            {
                FileUtils.deleteDirectory( tempIndexDirectory );
            }
            tempIndexDirectory.mkdirs();
            String baseIndexUrl = indexingContext.getIndexUpdateUrl();

            final Wagon wagon = wagonFactory.getWagon( new URL( this.remoteRepository.getUrl() ).getProtocol() );
            // TODO transferListener
            wagon.addTransferListener( new DownloadListener() );
            ProxyInfo proxyInfo = null;
            if ( this.networkProxy != null )
            {
                proxyInfo = new ProxyInfo();
                proxyInfo.setHost( this.networkProxy.getHost() );
                proxyInfo.setPort( this.networkProxy.getPort() );
                proxyInfo.setUserName( this.networkProxy.getUsername() );
                proxyInfo.setPassword( this.networkProxy.getPassword() );
            }
            AuthenticationInfo authenticationInfo = null;
            if ( this.remoteRepository.getUserName() != null )
            {
                authenticationInfo = new AuthenticationInfo();
                authenticationInfo.setUserName( this.remoteRepository.getUserName() );
                authenticationInfo.setPassword( this.remoteRepository.getPassword() );
            }
            wagon.connect( new Repository( this.remoteRepository.getId(), baseIndexUrl ), authenticationInfo,
                           proxyInfo );

            File indexDirectory = indexingContext.getIndexDirectoryFile();
            if ( !indexDirectory.exists() )
            {
                indexDirectory.mkdirs();
            }

            ResourceFetcher resourceFetcher = new ResourceFetcher()
            {
                public void connect( String id, String url )
                    throws IOException
                {
                    //no op
                }

                public void disconnect()
                    throws IOException
                {
                    // no op
                }

                public InputStream retrieve( String name )
                    throws IOException, FileNotFoundException
                {
                    try
                    {
                        log.debug( "resourceFetcher#retrieve, name:{}", name );
                        //TODO check those files are deleted !!
                        File file = new File( tempIndexDirectory, name );
                        if ( file.exists() )
                        {
                            file.delete();
                        }
                        //file.deleteOnExit();
                        wagon.get( name, file );
                        return new FileInputStream( file );
                    }
                    catch ( AuthorizationException e )
                    {
                        throw new IOException( e.getMessage(), e );
                    }
                    catch ( TransferFailedException e )
                    {
                        throw new IOException( e.getMessage(), e );
                    }
                    catch ( ResourceDoesNotExistException e )
                    {
                        throw new FileNotFoundException( e.getMessage() );
                    }
                }
            };

            IndexUpdateRequest request = new IndexUpdateRequest( indexingContext, resourceFetcher );

            this.indexUpdater.fetchAndUpdateIndex( request );


        }
        catch ( MalformedURLException e )
        {
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
        catch ( WagonFactoryException e )
        {
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
        catch ( ConnectionException e )
        {
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
        catch ( AuthenticationException e )
        {
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
        finally
        {
            deleteDirectoryQuiet( tempIndexDirectory );
            this.runningRemoteDownloadIds.remove( this.remoteRepository.getId() );
        }
        log.info( "end download remote index for remote repository " + this.remoteRepository.getId() );
    }

    private void deleteDirectoryQuiet( File f )
    {
        try
        {
            FileUtils.deleteDirectory( f );
        }
        catch ( IOException e )
        {
            log.warn( "skip error delete " + f + ": " + e.getMessage() );
        }
    }


    public static class DownloadListener
        implements TransferListener
    {
        private Logger log = LoggerFactory.getLogger( getClass() );

        public void transferInitiated( TransferEvent transferEvent )
        {
            log.debug( "initiate transfer of {}", transferEvent.getResource().getName() );
        }

        public void transferStarted( TransferEvent transferEvent )
        {
            log.debug( "start transfer of {}", transferEvent.getResource().getName() );
        }

        public void transferProgress( TransferEvent transferEvent, byte[] buffer, int length )
        {
            log.debug( "transfer of {} : {}/{}",
                       Arrays.asList( transferEvent.getResource().getName(), buffer.length, length ).toArray() );
        }

        public void transferCompleted( TransferEvent transferEvent )
        {
            log.info( "end of transfer file " + transferEvent.getResource().getName() );
        }

        public void transferError( TransferEvent transferEvent )
        {
            log.info( "error of transfer file {}: {}", Arrays.asList( transferEvent.getResource().getName(),
                                                                      transferEvent.getException().getMessage() ).toArray(
                new Object[2] ), transferEvent.getException() );
        }

        public void debug( String message )
        {
            log.debug( "transfer debug {}", message );
        }
    }

}
