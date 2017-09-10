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
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.proxy.common.WagonFactory;
import org.apache.archiva.proxy.common.WagonFactoryRequest;
import org.apache.commons.lang.time.StopWatch;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.shared.http.AbstractHttpClientWagon;
import org.apache.maven.wagon.shared.http.HttpConfiguration;
import org.apache.maven.wagon.shared.http.HttpMethodConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public class DownloadRemoteIndexTask
    implements Runnable
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    private RemoteRepository remoteRepository;

    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    private WagonFactory wagonFactory;

    private NetworkProxy networkProxy;

    private boolean fullDownload;

    private List<String> runningRemoteDownloadIds;

    private IndexUpdater indexUpdater;


    public DownloadRemoteIndexTask( DownloadRemoteIndexTaskRequest downloadRemoteIndexTaskRequest,
                                    List<String> runningRemoteDownloadIds )
    {
        this.remoteRepository = downloadRemoteIndexTaskRequest.getRemoteRepository();
        this.wagonFactory = downloadRemoteIndexTaskRequest.getWagonFactory();
        this.networkProxy = downloadRemoteIndexTaskRequest.getNetworkProxy();
        this.fullDownload = downloadRemoteIndexTaskRequest.isFullDownload();
        this.runningRemoteDownloadIds = runningRemoteDownloadIds;
        this.indexUpdater = downloadRemoteIndexTaskRequest.getIndexUpdater();
        this.remoteRepositoryAdmin = downloadRemoteIndexTaskRequest.getRemoteRepositoryAdmin();
    }

    @Override
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
            this.runningRemoteDownloadIds.add( this.remoteRepository.getId() );
        }
        Path tempIndexDirectory = null;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try
        {
            log.info( "start download remote index for remote repository {}", this.remoteRepository.getId() );
            IndexingContext indexingContext = remoteRepositoryAdmin.createIndexContext( this.remoteRepository );

            // create a temp directory to download files
            tempIndexDirectory = Paths.get(indexingContext.getIndexDirectoryFile().getParent(), ".tmpIndex" );
            Path indexCacheDirectory = Paths.get( indexingContext.getIndexDirectoryFile().getParent(), ".indexCache" );
            Files.createDirectories( indexCacheDirectory );
            if ( Files.exists(tempIndexDirectory) )
            {
                org.apache.archiva.common.utils.FileUtils.deleteDirectory( tempIndexDirectory );
            }
            Files.createDirectories( tempIndexDirectory );
            tempIndexDirectory.toFile().deleteOnExit();
            String baseIndexUrl = indexingContext.getIndexUpdateUrl();

            String wagonProtocol = new URL( this.remoteRepository.getUrl() ).getProtocol();

            final StreamWagon wagon = (StreamWagon) wagonFactory.getWagon(
                new WagonFactoryRequest( wagonProtocol, this.remoteRepository.getExtraHeaders() ).networkProxy(
                    this.networkProxy )
            );
            // FIXME olamy having 2 config values
            wagon.setReadTimeout( remoteRepository.getRemoteDownloadTimeout() * 1000 );
            wagon.setTimeout( remoteRepository.getTimeout() * 1000 );

            if ( wagon instanceof AbstractHttpClientWagon )
            {
                HttpConfiguration httpConfiguration = new HttpConfiguration();
                HttpMethodConfiguration httpMethodConfiguration = new HttpMethodConfiguration();
                httpMethodConfiguration.setUsePreemptive( true );
                httpMethodConfiguration.setReadTimeout( remoteRepository.getRemoteDownloadTimeout() * 1000 );
                httpConfiguration.setGet( httpMethodConfiguration );
                AbstractHttpClientWagon.class.cast( wagon ).setHttpConfiguration( httpConfiguration );
            }

            wagon.addTransferListener( new DownloadListener() );
            ProxyInfo proxyInfo = null;
            if ( this.networkProxy != null )
            {
                proxyInfo = new ProxyInfo();
                proxyInfo.setType( this.networkProxy.getProtocol() );
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

            Path indexDirectory = indexingContext.getIndexDirectoryFile().toPath();
            if ( !Files.exists(indexDirectory) )
            {
                Files.createDirectories( indexDirectory );
            }

            ResourceFetcher resourceFetcher =
                new WagonResourceFetcher( log, tempIndexDirectory, wagon, remoteRepository );
            IndexUpdateRequest request = new IndexUpdateRequest( indexingContext, resourceFetcher );
            request.setForceFullUpdate( this.fullDownload );
            request.setLocalIndexCacheDir( indexCacheDirectory.toFile() );

            this.indexUpdater.fetchAndUpdateIndex( request );
            stopWatch.stop();
            log.info( "time update index from remote for repository {}: {} s", this.remoteRepository.getId(),
                      ( stopWatch.getTime() / 1000 ) );

            // index packing optionnal ??
            //IndexPackingRequest indexPackingRequest =
            //    new IndexPackingRequest( indexingContext, indexingContext.getIndexDirectoryFile() );
            //indexPacker.packIndex( indexPackingRequest );
            indexingContext.updateTimestamp( true );

        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
        finally
        {
            deleteDirectoryQuiet( tempIndexDirectory );
            this.runningRemoteDownloadIds.remove( this.remoteRepository.getId() );
        }
        log.info( "end download remote index for remote repository {}", this.remoteRepository.getId() );
    }

    private void deleteDirectoryQuiet( Path f )
    {
        try
        {
            org.apache.archiva.common.utils.FileUtils.deleteDirectory( f );
        }
        catch ( IOException e )
        {
            log.warn( "skip error delete {} : {}", f, e.getMessage() );
        }
    }


    private static final class DownloadListener
        implements TransferListener
    {
        private Logger log = LoggerFactory.getLogger( getClass() );

        private String resourceName;

        private long startTime;

        private int totalLength = 0;

        @Override
        public void transferInitiated( TransferEvent transferEvent )
        {
            startTime = System.currentTimeMillis();
            resourceName = transferEvent.getResource().getName();
            log.debug( "initiate transfer of {}", resourceName );
        }

        @Override
        public void transferStarted( TransferEvent transferEvent )
        {
            this.totalLength = 0;
            resourceName = transferEvent.getResource().getName();
            log.info( "start transfer of {}", transferEvent.getResource().getName() );
        }

        @Override
        public void transferProgress( TransferEvent transferEvent, byte[] buffer, int length )
        {
            log.debug( "transfer of {} : {}/{}", transferEvent.getResource().getName(), buffer.length, length );
            this.totalLength += length;
        }

        @Override
        public void transferCompleted( TransferEvent transferEvent )
        {
            resourceName = transferEvent.getResource().getName();
            long endTime = System.currentTimeMillis();
            log.info( "end of transfer file {} {} kb: {}s", transferEvent.getResource().getName(),
                      this.totalLength / 1024, ( endTime - startTime ) / 1000 );
        }

        @Override
        public void transferError( TransferEvent transferEvent )
        {
            log.info( "error of transfer file {}: {}", transferEvent.getResource().getName(),
                      transferEvent.getException().getMessage(), transferEvent.getException() );
        }

        @Override
        public void debug( String message )
        {
            log.debug( "transfer debug {}", message );
        }
    }

    private static class WagonResourceFetcher
        implements ResourceFetcher
    {

        Logger log;

        Path tempIndexDirectory;

        Wagon wagon;

        RemoteRepository remoteRepository;

        private WagonResourceFetcher( Logger log, Path tempIndexDirectory, Wagon wagon,
                                      RemoteRepository remoteRepository )
        {
            this.log = log;
            this.tempIndexDirectory = tempIndexDirectory;
            this.wagon = wagon;
            this.remoteRepository = remoteRepository;
        }

        @Override
        public void connect( String id, String url )
            throws IOException
        {
            //no op  
        }

        @Override
        public void disconnect()
            throws IOException
        {
            // no op
        }

        @Override
        public InputStream retrieve( String name )
            throws IOException, FileNotFoundException
        {
            try
            {
                log.info( "index update retrieve file, name:{}", name );
                Path file = tempIndexDirectory.resolve( name );
                Files.deleteIfExists( file );
                file.toFile().deleteOnExit();
                wagon.get( addParameters( name, this.remoteRepository ), file.toFile() );
                return Files.newInputStream( file );
            }
            catch ( AuthorizationException | TransferFailedException e )
            {
                throw new IOException( e.getMessage(), e );
            }
            catch ( ResourceDoesNotExistException e )
            {
                FileNotFoundException fnfe = new FileNotFoundException( e.getMessage() );
                fnfe.initCause( e );
                throw fnfe;
            }
        }

        // FIXME remove crappy copy/paste
        protected String addParameters( String path, RemoteRepository remoteRepository )
        {
            if ( remoteRepository.getExtraParameters().isEmpty() )
            {
                return path;
            }

            boolean question = false;

            StringBuilder res = new StringBuilder( path == null ? "" : path );

            for ( Map.Entry<String, String> entry : remoteRepository.getExtraParameters().entrySet() )
            {
                if ( !question )
                {
                    res.append( '?' ).append( entry.getKey() ).append( '=' ).append( entry.getValue() );
                }
            }

            return res.toString();
        }

    }


}

