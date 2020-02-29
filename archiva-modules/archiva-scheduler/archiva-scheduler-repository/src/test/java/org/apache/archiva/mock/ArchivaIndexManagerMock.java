package org.apache.archiva.mock;

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

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.indexer.ArchivaIndexManager;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.indexer.IndexCreationFailedException;
import org.apache.archiva.indexer.IndexUpdateFailedException;
import org.apache.archiva.indexer.UnsupportedBaseContextException;
import org.apache.archiva.proxy.maven.WagonFactory;
import org.apache.archiva.proxy.maven.WagonFactoryException;
import org.apache.archiva.proxy.maven.WagonFactoryRequest;
import org.apache.archiva.proxy.model.NetworkProxy;
import org.apache.archiva.repository.EditableRepository;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.base.PasswordCredentials;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.UnsupportedRepositoryTypeException;
import org.apache.archiva.repository.storage.fs.FilesystemAsset;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.RemoteIndexFeature;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.index.ArtifactContext;
import org.apache.maven.index.ArtifactContextProducer;
import org.apache.maven.index.DefaultScannerListener;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.IndexerEngine;
import org.apache.maven.index.Scanner;
import org.apache.maven.index.ScanningRequest;
import org.apache.maven.index.ScanningResult;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index_shaded.lucene.index.IndexFormatTooOldException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.StreamWagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.shared.http.AbstractHttpClientWagon;
import org.apache.maven.wagon.shared.http.HttpConfiguration;
import org.apache.maven.wagon.shared.http.HttpMethodConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Service("archivaIndexManager#maven")
public class ArchivaIndexManagerMock implements ArchivaIndexManager {

    private static final Logger log = LoggerFactory.getLogger( ArchivaIndexManagerMock.class );

    @Inject
    private Indexer indexer;

    @Inject
    private IndexerEngine indexerEngine;

    @Inject
    private List<? extends IndexCreator> indexCreators;

    @Inject
    private IndexPacker indexPacker;

    @Inject
    private Scanner scanner;

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    private WagonFactory wagonFactory;


    @Inject
    private ArtifactContextProducer artifactContextProducer;

    private ConcurrentSkipListSet<Path> activeContexts = new ConcurrentSkipListSet<>( );

    private static final int WAIT_TIME = 100;
    private static final int MAX_WAIT = 10;


    public static IndexingContext getMvnContext(ArchivaIndexingContext context ) throws UnsupportedBaseContextException
    {
        if ( !context.supports( IndexingContext.class ) )
        {
            log.error( "The provided archiva index context does not support the maven IndexingContext" );
            throw new UnsupportedBaseContextException( "The context does not support the Maven IndexingContext" );
        }
        return context.getBaseContext( IndexingContext.class );
    }

    private Path getIndexPath( ArchivaIndexingContext ctx )
    {
        return ctx.getPath().getFilePath();
    }

    @FunctionalInterface
    interface IndexUpdateConsumer
    {

        void accept( IndexingContext indexingContext ) throws IndexUpdateFailedException;
    }

    /*
     * This method is used to do some actions around the update execution code. And to make sure, that no other
     * method is running on the same index.
     */
    private void executeUpdateFunction( ArchivaIndexingContext context, IndexUpdateConsumer function ) throws IndexUpdateFailedException
    {
        IndexingContext indexingContext = null;
        try
        {
            indexingContext = getMvnContext( context );
        }
        catch ( UnsupportedBaseContextException e )
        {
            throw new IndexUpdateFailedException( "Maven index is not supported by this context", e );
        }
        final Path ctxPath = getIndexPath( context );
        int loop = MAX_WAIT;
        boolean active = false;
        while ( loop-- > 0 && !active )
        {
            active = activeContexts.add( ctxPath );
            try
            {
                Thread.currentThread( ).sleep( WAIT_TIME );
            }
            catch ( InterruptedException e )
            {
                // Ignore this
            }
        }
        if ( active )
        {
            try
            {
                function.accept( indexingContext );
            }
            finally
            {
                activeContexts.remove( ctxPath );
            }
        }
        else
        {
            throw new IndexUpdateFailedException( "Timeout while waiting for index release on context " + context.getId( ) );
        }
    }

    @Override
    public void pack( final ArchivaIndexingContext context ) throws IndexUpdateFailedException
    {
        executeUpdateFunction( context, indexingContext -> {
                    try
                    {
                        IndexPackingRequest request = new IndexPackingRequest( indexingContext,
                                indexingContext.acquireIndexSearcher( ).getIndexReader( ),
                                indexingContext.getIndexDirectoryFile( ) );
                        indexPacker.packIndex( request );
                        indexingContext.updateTimestamp( true );
                    }
                    catch ( IOException e )
                    {
                        log.error( "IOException while packing index of context " + context.getId( ) + ( StringUtils.isNotEmpty( e.getMessage( ) ) ? ": " + e.getMessage( ) : "" ) );
                        throw new IndexUpdateFailedException( "IOException during update of " + context.getId( ), e );
                    }
                }
        );

    }

    @Override
    public void scan(final ArchivaIndexingContext context) throws IndexUpdateFailedException
    {
        executeUpdateFunction( context, indexingContext -> {
            DefaultScannerListener listener = new DefaultScannerListener( indexingContext, indexerEngine, true, null );
            ScanningRequest request = new ScanningRequest( indexingContext, listener );
            ScanningResult result = scanner.scan( request );
            if ( result.hasExceptions( ) )
            {
                log.error( "Exceptions occured during index scan of " + context.getId( ) );
                result.getExceptions( ).stream( ).map( e -> e.getMessage( ) ).distinct( ).limit( 5 ).forEach(
                        s -> log.error( "Message: " + s )
                );
            }

        } );
    }

    @Override
    public void update(final ArchivaIndexingContext context, final boolean fullUpdate) throws IndexUpdateFailedException
    {
        log.info( "start download remote index for remote repository {}", context.getRepository( ).getId( ) );
        URI remoteUpdateUri;
        if ( !( context.getRepository( ) instanceof RemoteRepository) || !(context.getRepository().supportsFeature(RemoteIndexFeature.class)) )
        {
            throw new IndexUpdateFailedException( "The context is not associated to a remote repository with remote index " + context.getId( ) );
        } else {
            RemoteIndexFeature rif = context.getRepository().getFeature(RemoteIndexFeature.class).get();
            remoteUpdateUri = context.getRepository().getLocation().resolve(rif.getIndexUri());
        }
        final RemoteRepository remoteRepository = (RemoteRepository) context.getRepository( );

        executeUpdateFunction( context,
                indexingContext -> {
                    try
                    {
                        // create a temp directory to download files
                        Path tempIndexDirectory = Paths.get( indexingContext.getIndexDirectoryFile( ).getParent( ), ".tmpIndex" );
                        Path indexCacheDirectory = Paths.get( indexingContext.getIndexDirectoryFile( ).getParent( ), ".indexCache" );
                        Files.createDirectories( indexCacheDirectory );
                        if ( Files.exists( tempIndexDirectory ) )
                        {
                            FileUtils.deleteDirectory( tempIndexDirectory );
                        }
                        Files.createDirectories( tempIndexDirectory );
                        tempIndexDirectory.toFile( ).deleteOnExit( );
                        String baseIndexUrl = indexingContext.getIndexUpdateUrl( );

                        String wagonProtocol = remoteUpdateUri.toURL( ).getProtocol( );

                        NetworkProxy networkProxy = null;
                        if ( remoteRepository.supportsFeature( RemoteIndexFeature.class ) )
                        {
                            RemoteIndexFeature rif = remoteRepository.getFeature( RemoteIndexFeature.class ).get( );

                            final StreamWagon wagon = (StreamWagon) wagonFactory.getWagon(
                                    new WagonFactoryRequest( wagonProtocol, remoteRepository.getExtraHeaders( ) ).networkProxy(
                                            networkProxy )
                            );
                            int readTimeout = (int) rif.getDownloadTimeout( ).toMillis( ) * 1000;
                            wagon.setReadTimeout( readTimeout );
                            wagon.setTimeout( (int) remoteRepository.getTimeout( ).toMillis( ) * 1000 );

                            if ( wagon instanceof AbstractHttpClientWagon)
                            {
                                HttpConfiguration httpConfiguration = new HttpConfiguration( );
                                HttpMethodConfiguration httpMethodConfiguration = new HttpMethodConfiguration( );
                                httpMethodConfiguration.setUsePreemptive( true );
                                httpMethodConfiguration.setReadTimeout( readTimeout );
                                httpConfiguration.setGet( httpMethodConfiguration );
                                AbstractHttpClientWagon.class.cast( wagon ).setHttpConfiguration( httpConfiguration );
                            }

                            wagon.addTransferListener( new DownloadListener( ) );
                            ProxyInfo proxyInfo = null;
                            if ( networkProxy != null )
                            {
                                proxyInfo = new ProxyInfo( );
                                proxyInfo.setType( networkProxy.getProtocol( ) );
                                proxyInfo.setHost( networkProxy.getHost( ) );
                                proxyInfo.setPort( networkProxy.getPort( ) );
                                proxyInfo.setUserName( networkProxy.getUsername( ) );
                                proxyInfo.setPassword(new String(networkProxy.getPassword()));
                            }
                            AuthenticationInfo authenticationInfo = null;
                            if ( remoteRepository.getLoginCredentials( ) != null && ( remoteRepository.getLoginCredentials( ) instanceof PasswordCredentials) )
                            {
                                PasswordCredentials creds = (PasswordCredentials) remoteRepository.getLoginCredentials( );
                                authenticationInfo = new AuthenticationInfo( );
                                authenticationInfo.setUserName( creds.getUsername( ) );
                                authenticationInfo.setPassword( new String( creds.getPassword( ) ) );
                            }
                            wagon.connect( new org.apache.maven.wagon.repository.Repository( remoteRepository.getId( ), baseIndexUrl ), authenticationInfo,
                                    proxyInfo );

                            Path indexDirectory = indexingContext.getIndexDirectoryFile( ).toPath( );
                            if ( !Files.exists( indexDirectory ) )
                            {
                                Files.createDirectories( indexDirectory );
                            }

                            ResourceFetcher resourceFetcher =
                                    new WagonResourceFetcher( log, tempIndexDirectory, wagon, remoteRepository );
                            IndexUpdateRequest request = new IndexUpdateRequest( indexingContext, resourceFetcher );
                            request.setForceFullUpdate( fullUpdate );
                            request.setLocalIndexCacheDir( indexCacheDirectory.toFile( ) );

                            // indexUpdater.fetchAndUpdateIndex( request );

                            indexingContext.updateTimestamp( true );
                        }

                    }
                    catch ( AuthenticationException e )
                    {
                        log.error( "Could not login to the remote proxy for updating index of {}", remoteRepository.getId( ), e );
                        throw new IndexUpdateFailedException( "Login in to proxy failed while updating remote repository " + remoteRepository.getId( ), e );
                    }
                    catch ( ConnectionException e )
                    {
                        log.error( "Connection error during index update for remote repository {}", remoteRepository.getId( ), e );
                        throw new IndexUpdateFailedException( "Connection error during index update for remote repository " + remoteRepository.getId( ), e );
                    }
                    catch ( MalformedURLException e )
                    {
                        log.error( "URL for remote index update of remote repository {} is not correct {}", remoteRepository.getId( ), remoteUpdateUri, e );
                        throw new IndexUpdateFailedException( "URL for remote index update of repository is not correct " + remoteUpdateUri, e );
                    }
                    catch ( IOException e )
                    {
                        log.error( "IOException during index update of remote repository {}: {}", remoteRepository.getId( ), e.getMessage( ), e );
                        throw new IndexUpdateFailedException( "IOException during index update of remote repository " + remoteRepository.getId( )
                                + ( StringUtils.isNotEmpty( e.getMessage( ) ) ? ": " + e.getMessage( ) : "" ), e );
                    }
                    catch ( WagonFactoryException e )
                    {
                        log.error( "Wagon for remote index download of {} could not be created: {}", remoteRepository.getId( ), e.getMessage( ), e );
                        throw new IndexUpdateFailedException( "Error while updating the remote index of " + remoteRepository.getId( ), e );
                    }
                } );

    }

    @Override
    public void addArtifactsToIndex( final ArchivaIndexingContext context, final Collection<URI> artifactReference ) throws IndexUpdateFailedException
    {
        StorageAsset ctxUri = context.getPath();
        executeUpdateFunction(context, indexingContext -> {
            Collection<ArtifactContext> artifacts = artifactReference.stream().map(r -> artifactContextProducer.getArtifactContext(indexingContext, Paths.get(ctxUri.getFilePath().toUri().resolve(r)).toFile())).collect(Collectors.toList());
            try {
                indexer.addArtifactsToIndex(artifacts, indexingContext);
            } catch (IOException e) {
                log.error("IOException while adding artifact {}", e.getMessage(), e);
                throw new IndexUpdateFailedException("Error occured while adding artifact to index of "+context.getId()
                        + (StringUtils.isNotEmpty(e.getMessage()) ? ": "+e.getMessage() : ""));
            }
        });
    }

    @Override
    public void removeArtifactsFromIndex( ArchivaIndexingContext context, Collection<URI> artifactReference ) throws IndexUpdateFailedException
    {
        final StorageAsset ctxUri = context.getPath();
        executeUpdateFunction(context, indexingContext -> {
            Collection<ArtifactContext> artifacts = artifactReference.stream().map(r -> artifactContextProducer.getArtifactContext(indexingContext, Paths.get(ctxUri.getFilePath().toUri().resolve(r)).toFile())).collect(Collectors.toList());
            try {
                indexer.deleteArtifactsFromIndex(artifacts, indexingContext);
            } catch (IOException e) {
                log.error("IOException while removing artifact {}", e.getMessage(), e);
                throw new IndexUpdateFailedException("Error occured while removing artifact from index of "+context.getId()
                        + (StringUtils.isNotEmpty(e.getMessage()) ? ": "+e.getMessage() : ""));
            }
        });

    }

    @Override
    public boolean supportsRepository( RepositoryType type )
    {
        return type == RepositoryType.MAVEN;
    }

    @Override
    public ArchivaIndexingContext createContext( Repository repository ) throws IndexCreationFailedException
    {
        log.debug("Creating context for repo {}, type: {}", repository.getId(), repository.getType());
        if ( repository.getType( ) != RepositoryType.MAVEN )
        {
            throw new UnsupportedRepositoryTypeException( repository.getType( ) );
        }
        IndexingContext mvnCtx = null;
        try
        {
            if ( repository instanceof RemoteRepository )
            {
                mvnCtx = createRemoteContext( (RemoteRepository) repository );
            }
            else if ( repository instanceof ManagedRepository )
            {
                mvnCtx = createManagedContext( (ManagedRepository) repository );
            }
        }
        catch ( IOException e )
        {
            log.error( "IOException during context creation " + e.getMessage( ), e );
            throw new IndexCreationFailedException( "Could not create index context for repository " + repository.getId( )
                    + ( StringUtils.isNotEmpty( e.getMessage( ) ) ? ": " + e.getMessage( ) : "" ), e );
        }
        MavenIndexContextMock context = new MavenIndexContextMock( repository, mvnCtx );

        return context;
    }

    @Override
    public ArchivaIndexingContext reset(ArchivaIndexingContext context) throws IndexUpdateFailedException {
        ArchivaIndexingContext ctx;
        executeUpdateFunction(context, indexingContext -> {
            try {
                indexingContext.close(true);
            } catch (IOException e) {
                log.warn("Index close failed");
            }
            org.apache.archiva.repository.storage.util.StorageUtil.deleteRecursively(context.getPath());
        });
        try {
            Repository repo = context.getRepository();
            ctx = createContext(context.getRepository());
            if (repo instanceof EditableRepository) {
                ((EditableRepository)repo).setIndexingContext(ctx);
            }
        } catch (IndexCreationFailedException e) {
            throw new IndexUpdateFailedException("Could not create index");
        }
        return ctx;
    }

    @Override
    public ArchivaIndexingContext move(ArchivaIndexingContext context, Repository repo) throws IndexCreationFailedException {
        if (context==null) {
            return null;
        }
        if (context.supports(IndexingContext.class)) {
            try {
                StorageAsset newPath = getIndexPath(repo);
                IndexingContext ctx = context.getBaseContext(IndexingContext.class);
                Path oldPath = ctx.getIndexDirectoryFile().toPath();
                if (oldPath.equals(newPath)) {
                    // Nothing to do, if path does not change
                    return context;
                }
                if (!Files.exists(oldPath)) {
                    return createContext(repo);
                } else if (context.isEmpty()) {
                    context.close();
                    return createContext(repo);
                } else {
                    context.close(false);
                    Files.move(oldPath, newPath.getFilePath());
                    return createContext(repo);
                }
            } catch (IOException e) {
                log.error("IOException while moving index directory {}", e.getMessage(), e);
                throw new IndexCreationFailedException("Could not recreated the index.", e);
            } catch (UnsupportedBaseContextException e) {
                throw new IndexCreationFailedException("The given context, is not a maven context.");
            }
        } else {
            throw new IndexCreationFailedException("Bad context type. This is not a maven context.");
        }
    }

    @Override
    public void updateLocalIndexPath(Repository repo) {
        if (repo.supportsFeature(IndexCreationFeature.class)) {
            IndexCreationFeature icf = repo.getFeature(IndexCreationFeature.class).get();
            try {
                icf.setLocalIndexPath(getIndexPath(repo));
            } catch (IOException e) {
                log.error("Could not set local index path for {}. New URI: {}", repo.getId(), icf.getIndexPath());
            }
        }
    }

    @Override
    public ArchivaIndexingContext mergeContexts(Repository destinationRepo, List<ArchivaIndexingContext> contexts, boolean packIndex) throws UnsupportedOperationException, IndexCreationFailedException {
        return null;
    }

    private StorageAsset getIndexPath( Repository repo) throws IOException {
        IndexCreationFeature icf = repo.getFeature(IndexCreationFeature.class).get();
        Path repoDir = repo.getAsset("").getFilePath();
        URI indexDir = icf.getIndexPath();
        String indexPath = indexDir.getPath();
        Path indexDirectory = null;
        FilesystemStorage filesystemStorage = (FilesystemStorage) repo.getAsset("").getStorage();
        if ( ! StringUtils.isEmpty(indexDir.toString( ) ) )
        {

            indexDirectory = PathUtil.getPathFromUri( indexDir );
            // not absolute so create it in repository directory
            if ( indexDirectory.isAbsolute( ) )
            {
                indexPath = indexDirectory.getFileName().toString();
                filesystemStorage = new FilesystemStorage(indexDirectory, new DefaultFileLockManager());
            }
            else
            {
                indexDirectory = repoDir.resolve( indexDirectory );
            }
        }
        else
        {
            indexDirectory = repoDir.resolve( ".index" );
            indexPath = ".index";
        }

        if ( !Files.exists( indexDirectory ) )
        {
            Files.createDirectories( indexDirectory );
        }
        return new FilesystemAsset( filesystemStorage, indexPath, indexDirectory);
    }

    private IndexingContext createRemoteContext(RemoteRepository remoteRepository ) throws IOException
    {
        Path appServerBase = archivaConfiguration.getAppServerBaseDir( );

        String contextKey = "remote-" + remoteRepository.getId( );


        // create remote repository path
        Path repoDir = remoteRepository.getAsset("").getFilePath();
        if ( !Files.exists( repoDir ) )
        {
            Files.createDirectories( repoDir );
        }

        StorageAsset indexDirectory = null;

        // is there configured indexDirectory ?
        if ( remoteRepository.supportsFeature( RemoteIndexFeature.class ) )
        {
            RemoteIndexFeature rif = remoteRepository.getFeature( RemoteIndexFeature.class ).get( );
            indexDirectory = getIndexPath(remoteRepository);
            String remoteIndexUrl = calculateIndexRemoteUrl( remoteRepository.getLocation( ), rif );
            try
            {

                return getIndexingContext( remoteRepository, contextKey, repoDir, indexDirectory, remoteIndexUrl );
            }
            catch ( IndexFormatTooOldException e )
            {
                // existing index with an old lucene format so we need to delete it!!!
                // delete it first then recreate it.
                log.warn( "the index of repository {} is too old we have to delete and recreate it", //
                        remoteRepository.getId( ) );
                FileUtils.deleteDirectory( indexDirectory.getFilePath() );
                return getIndexingContext( remoteRepository, contextKey, repoDir, indexDirectory, remoteIndexUrl );

            }
        }
        else
        {
            throw new IOException( "No remote index defined" );
        }
    }

    private IndexingContext getIndexingContext( Repository repository, String contextKey, Path repoDir, StorageAsset indexDirectory, String indexUrl ) throws IOException
    {
        return indexer.createIndexingContext( contextKey, repository.getId( ), repoDir.toFile( ), indexDirectory.getFilePath().toFile( ),
                repository.getLocation( ) == null ? null : repository.getLocation( ).toString( ),
                indexUrl,
                true, false,
                indexCreators );
    }

    private IndexingContext createManagedContext( ManagedRepository repository ) throws IOException
    {

        IndexingContext context;
        // take care first about repository location as can be relative
        Path repositoryDirectory = repository.getAsset("").getFilePath();

        if ( !Files.exists( repositoryDirectory ) )
        {
            try
            {
                Files.createDirectories( repositoryDirectory );
            }
            catch ( IOException e )
            {
                log.error( "Could not create directory {}", repositoryDirectory );
            }
        }

        StorageAsset indexDirectory = null;

        if ( repository.supportsFeature( IndexCreationFeature.class ) )
        {
            indexDirectory = getIndexPath(repository);

            String indexUrl = repositoryDirectory.toUri( ).toURL( ).toExternalForm( );
            try
            {
                context = getIndexingContext( repository, repository.getId( ), repositoryDirectory, indexDirectory, indexUrl );
                context.setSearchable( repository.isScanned( ) );
            }
            catch ( IndexFormatTooOldException e )
            {
                // existing index with an old lucene format so we need to delete it!!!
                // delete it first then recreate it.
                log.warn( "the index of repository {} is too old we have to delete and recreate it", //
                        repository.getId( ) );
                FileUtils.deleteDirectory( indexDirectory.getFilePath() );
                context = getIndexingContext( repository, repository.getId( ), repositoryDirectory, indexDirectory, indexUrl );
                context.setSearchable( repository.isScanned( ) );
            }
            return context;
        }
        else
        {
            throw new IOException( "No repository index defined" );
        }
    }

    private String calculateIndexRemoteUrl( URI baseUri, RemoteIndexFeature rif )
    {
        if ( rif.getIndexUri( ) == null )
        {
            return baseUri.resolve( ".index" ).toString( );
        }
        else
        {
            return baseUri.resolve( rif.getIndexUri( ) ).toString( );
        }
    }

    private static final class DownloadListener
            implements TransferListener
    {
        private Logger log = LoggerFactory.getLogger( getClass( ) );

        private String resourceName;

        private long startTime;

        private int totalLength = 0;

        @Override
        public void transferInitiated( TransferEvent transferEvent )
        {
            startTime = System.currentTimeMillis( );
            resourceName = transferEvent.getResource( ).getName( );
            log.debug( "initiate transfer of {}", resourceName );
        }

        @Override
        public void transferStarted( TransferEvent transferEvent )
        {
            this.totalLength = 0;
            resourceName = transferEvent.getResource( ).getName( );
            log.info( "start transfer of {}", transferEvent.getResource( ).getName( ) );
        }

        @Override
        public void transferProgress( TransferEvent transferEvent, byte[] buffer, int length )
        {
            log.debug( "transfer of {} : {}/{}", transferEvent.getResource( ).getName( ), buffer.length, length );
            this.totalLength += length;
        }

        @Override
        public void transferCompleted( TransferEvent transferEvent )
        {
            resourceName = transferEvent.getResource( ).getName( );
            long endTime = System.currentTimeMillis( );
            log.info( "end of transfer file {} {} kb: {}s", transferEvent.getResource( ).getName( ),
                    this.totalLength / 1024, ( endTime - startTime ) / 1000 );
        }

        @Override
        public void transferError( TransferEvent transferEvent )
        {
            log.info( "error of transfer file {}: {}", transferEvent.getResource( ).getName( ),
                    transferEvent.getException( ).getMessage( ), transferEvent.getException( ) );
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
        public void disconnect( )
                throws IOException
        {
            // no op
        }

        @Override
        public InputStream retrieve(String name )
                throws IOException, FileNotFoundException
        {
            try
            {
                log.info( "index update retrieve file, name:{}", name );
                Path file = tempIndexDirectory.resolve( name );
                Files.deleteIfExists( file );
                file.toFile( ).deleteOnExit( );
                wagon.get( addParameters( name, remoteRepository ), file.toFile( ) );
                return Files.newInputStream( file );
            }
            catch ( AuthorizationException | TransferFailedException e )
            {
                throw new IOException( e.getMessage( ), e );
            }
            catch ( ResourceDoesNotExistException e )
            {
                FileNotFoundException fnfe = new FileNotFoundException( e.getMessage( ) );
                fnfe.initCause( e );
                throw fnfe;
            }
        }

        // FIXME remove crappy copy/paste
        protected String addParameters( String path, RemoteRepository remoteRepository )
        {
            if ( remoteRepository.getExtraParameters( ).isEmpty( ) )
            {
                return path;
            }

            boolean question = false;

            StringBuilder res = new StringBuilder( path == null ? "" : path );

            for ( Map.Entry<String, String> entry : remoteRepository.getExtraParameters( ).entrySet( ) )
            {
                if ( !question )
                {
                    res.append( '?' ).append( entry.getKey( ) ).append( '=' ).append( entry.getValue( ) );
                }
            }

            return res.toString( );
        }

    }
}
