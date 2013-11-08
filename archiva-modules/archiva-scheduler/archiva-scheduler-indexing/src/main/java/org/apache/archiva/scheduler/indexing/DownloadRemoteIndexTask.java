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
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.proxy.common.WagonFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.codecs.LengthDelimitedDecoder;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.ZeroCopyConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
        File tempIndexDirectory = null;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try
        {
            log.info( "start download remote index for remote repository {}", this.remoteRepository.getId() );
            IndexingContext indexingContext = remoteRepositoryAdmin.createIndexContext( this.remoteRepository );

            // create a temp directory to download files
            tempIndexDirectory = new File( indexingContext.getIndexDirectoryFile().getParent(), ".tmpIndex" );
            File indexCacheDirectory = new File( indexingContext.getIndexDirectoryFile().getParent(), ".indexCache" );
            indexCacheDirectory.mkdirs();
            if ( tempIndexDirectory.exists() )
            {
                FileUtils.deleteDirectory( tempIndexDirectory );
            }
            tempIndexDirectory.mkdirs();
            tempIndexDirectory.deleteOnExit();
            String baseIndexUrl = indexingContext.getIndexUpdateUrl();

            URL indexUrl = new URL( baseIndexUrl );

            /*
            String wagonProtocol = new URL( this.remoteRepository.getUrl() ).getProtocol();

            final StreamWagon wagon = (StreamWagon) wagonFactory.getWagon(
                new WagonFactoryRequest( wagonProtocol, this.remoteRepository.getExtraHeaders() ).networkProxy(
                    this.networkProxy ) );
            int timeoutInMilliseconds = remoteRepository.getTimeout() * 1000;
            // FIXME olamy having 2 config values
            wagon.setReadTimeout( timeoutInMilliseconds );
            wagon.setTimeout( timeoutInMilliseconds );

            if ( wagon instanceof AbstractHttpClientWagon )
            {
                HttpConfiguration httpConfiguration = new HttpConfiguration();
                HttpMethodConfiguration httpMethodConfiguration = new HttpMethodConfiguration();
                httpMethodConfiguration.setUsePreemptive( true );
                httpMethodConfiguration.setReadTimeout( timeoutInMilliseconds );
                httpConfiguration.setGet( httpMethodConfiguration );
                ( (AbstractHttpClientWagon) wagon ).setHttpConfiguration( httpConfiguration );
            }

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
            */
            //---------------------------------------------

            HttpAsyncClientBuilder builder = HttpAsyncClientBuilder.create();

            BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();

            if ( this.networkProxy != null )
            {
                HttpHost httpHost = new HttpHost( this.networkProxy.getHost(), this.networkProxy.getPort() );
                builder = builder.setProxy( httpHost );

                if ( this.networkProxy.getUsername() != null )
                {
                    basicCredentialsProvider.setCredentials(
                        new AuthScope( this.networkProxy.getHost(), this.networkProxy.getPort(), null, null ),
                        new UsernamePasswordCredentials( this.networkProxy.getUsername(),
                                                         this.networkProxy.getPassword() ) );
                }

            }

            if ( this.remoteRepository.getUserName() != null )
            {
                basicCredentialsProvider.setCredentials(
                    new AuthScope( indexUrl.getHost(), indexUrl.getPort(), null, null ),
                    new UsernamePasswordCredentials( this.remoteRepository.getUserName(),
                                                     this.remoteRepository.getPassword() ) );

            }

            builder = builder.setDefaultCredentialsProvider( basicCredentialsProvider );

            File indexDirectory = indexingContext.getIndexDirectoryFile();
            if ( !indexDirectory.exists() )
            {
                indexDirectory.mkdirs();
            }

            CloseableHttpAsyncClient closeableHttpAsyncClient = builder.build();
            closeableHttpAsyncClient.start();
            ResourceFetcher resourceFetcher =
                new ZeroCopyResourceFetcher( log, tempIndexDirectory, remoteRepository, closeableHttpAsyncClient,
                                             baseIndexUrl );

            IndexUpdateRequest request = new IndexUpdateRequest( indexingContext, resourceFetcher );
            request.setForceFullUpdate( this.fullDownload );
            request.setLocalIndexCacheDir( indexCacheDirectory );

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
        catch ( MalformedURLException e )
        {
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage(), e );
            throw new RuntimeException( e.getMessage(), e );
        }
        catch ( RepositoryAdminException e )
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
            log.warn( "skip error delete {} : {}", f, e.getMessage() );
        }
    }

    private static class ZeroCopyConsumerListener
        extends ZeroCopyConsumer
    {
        private Logger log = LoggerFactory.getLogger( getClass() );

        private String resourceName;

        private long startTime;

        private long totalLength = 0;

        //private long currentLength = 0;

        private ZeroCopyConsumerListener( File file, String resourceName )
            throws FileNotFoundException
        {
            super( file );
            this.resourceName = resourceName;
        }

        @Override
        protected File process( final HttpResponse response, final File file, final ContentType contentType )
            throws Exception
        {
            if ( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK )
            {
                throw new ClientProtocolException( "Upload failed: " + response.getStatusLine() );
            }
            long endTime = System.currentTimeMillis();
            log.info( "end of transfer file {} {} kb: {}s", resourceName, this.totalLength / 1024,
                      ( endTime - startTime ) / 1000 );
            return file;
        }

        @Override
        protected void onContentReceived( ContentDecoder decoder, IOControl ioControl )
            throws IOException
        {
            if ( decoder instanceof LengthDelimitedDecoder )
            {
                LengthDelimitedDecoder ldl = LengthDelimitedDecoder.class.cast( decoder );
                long len = getLen( ldl );
                if ( len > -1 )
                {
                    log.debug( "transfer of {} : {}/{}", resourceName, len / 1024, this.totalLength / 1024 );
                }
            }

            super.onContentReceived( decoder, ioControl );
        }

        @Override
        protected void onResponseReceived( HttpResponse response )
        {
            this.startTime = System.currentTimeMillis();
            super.onResponseReceived( response );
            this.totalLength = response.getEntity().getContentLength();
            log.info( "start transfer of {}, contentLength: {}", resourceName, this.totalLength / 1024 );
        }

        @Override
        protected void onEntityEnclosed( HttpEntity entity, ContentType contentType )
            throws IOException
        {
            super.onEntityEnclosed( entity, contentType );
        }

        private long getLen( LengthDelimitedDecoder ldl )
        {
            try
            {
                Field lenField = LengthDelimitedDecoder.class.getDeclaredField( "len" );
                lenField.setAccessible( true );
                long len = (Long) lenField.get( ldl );
                return len;
            }
            catch ( NoSuchFieldException e )
            {
                log.debug( e.getMessage(), e );
                return -1;
            }
            catch ( IllegalAccessException e )
            {
                log.debug( e.getMessage(), e );
                return -1;
            }
        }
    }

    private static class ZeroCopyResourceFetcher
        implements ResourceFetcher
    {

        Logger log;

        File tempIndexDirectory;

        final RemoteRepository remoteRepository;

        CloseableHttpAsyncClient httpclient;

        String baseIndexUrl;

        private ZeroCopyResourceFetcher( Logger log, File tempIndexDirectory, RemoteRepository remoteRepository,
                                         CloseableHttpAsyncClient httpclient, String baseIndexUrl )
        {
            this.log = log;
            this.tempIndexDirectory = tempIndexDirectory;
            this.remoteRepository = remoteRepository;
            this.httpclient = httpclient;
            this.baseIndexUrl = baseIndexUrl;
        }

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

        public InputStream retrieve( final String name )
            throws IOException
        {

            log.info( "index update retrieve file, name:{}", name );
            File file = new File( tempIndexDirectory, name );
            if ( file.exists() )
            {
                file.delete();
            }
            file.deleteOnExit();

            ZeroCopyConsumer<File> consumer = new ZeroCopyConsumerListener( file, name );

            URL targetUrl = new URL( this.baseIndexUrl );
            final HttpHost targetHost = new HttpHost( targetUrl.getHost(), targetUrl.getPort() );

            Future<File> httpResponseFuture = httpclient.execute( new HttpAsyncRequestProducer()
            {
                @Override
                public HttpHost getTarget()
                {
                    return targetHost;
                }

                @Override
                public HttpRequest generateRequest()
                    throws IOException, HttpException
                {
                    StringBuilder url = new StringBuilder( baseIndexUrl );
                    if ( !StringUtils.endsWith( baseIndexUrl, "/" ) )
                    {
                        url.append( '/' );
                    }
                    HttpGet httpGet = new HttpGet( url.append( addParameters( name, remoteRepository ) ).toString() );
                    return httpGet;
                }

                @Override
                public void produceContent( ContentEncoder encoder, IOControl ioctrl )
                    throws IOException
                {
                    // no op
                }

                @Override
                public void requestCompleted( HttpContext context )
                {
                    log.debug( "requestCompleted" );
                }

                @Override
                public void failed( Exception ex )
                {
                    log.error( "http request failed", ex );
                }

                @Override
                public boolean isRepeatable()
                {
                    log.debug( "isRepeatable" );
                    return true;
                }

                @Override
                public void resetRequest()
                    throws IOException
                {
                    log.debug( "resetRequest" );
                }

                @Override
                public void close()
                    throws IOException
                {
                    log.debug( "close" );
                }

            }, consumer, null );
            try
            {
                int timeOut = this.remoteRepository.getRemoteDownloadTimeout();
                file = timeOut > 0 ? httpResponseFuture.get( timeOut, TimeUnit.SECONDS ) : httpResponseFuture.get();
            }
            catch ( InterruptedException e )
            {
                throw new IOException( e.getMessage(), e );
            }
            catch ( ExecutionException e )
            {
                throw new IOException( e.getMessage(), e );
            }
            catch ( TimeoutException e )
            {
                throw new IOException( e.getMessage(), e );
            }
            return new FileInputStream( file );
        }

    }

    // FIXME remove crappy copy/paste
    protected static String addParameters( String path, RemoteRepository remoteRepository )
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


