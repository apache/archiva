package org.apache.archiva.indexer.maven;

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

import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.indexer.ArchivaIndexManager;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.indexer.IndexCreationFailedException;
import org.apache.archiva.indexer.IndexUpdateFailedException;
import org.apache.archiva.indexer.UnsupportedBaseContextException;
import org.apache.archiva.proxy.ProxyRegistry;
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
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.storage.RepositoryStorage;
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
import org.apache.maven.index.context.ContextMemberProvider;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.StaticContextMemberProvider;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdater;
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
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * Maven implementation of index manager.
 * The index manager is a singleton, so we try to make sure, that index operations are not running
 * parallel by synchronizing on the index path.
 * A update operation waits for parallel running methods to finish before starting, but after a certain
 * time of retries a IndexUpdateFailedException is thrown.
 */
@Service( "archivaIndexManager#maven" )
public class MavenIndexManager implements ArchivaIndexManager {

    private static final Logger log = LoggerFactory.getLogger( MavenIndexManager.class );

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
    private IndexUpdater indexUpdater;

    @Inject
    private ArtifactContextProducer artifactContextProducer;

    @Inject
    private ProxyRegistry proxyRegistry;


    private ConcurrentSkipListSet<StorageAsset> activeContexts = new ConcurrentSkipListSet<>( );

    private static final int WAIT_TIME = 100;
    private static final int MAX_WAIT = 10;


    public static IndexingContext getMvnContext( ArchivaIndexingContext context ) throws UnsupportedBaseContextException
    {
        if (context!=null)
        {
            if ( !context.supports( IndexingContext.class ) )
            {
                log.error( "The provided archiva index context does not support the maven IndexingContext" );
                throw new UnsupportedBaseContextException( "The context does not support the Maven IndexingContext" );
            }
            return context.getBaseContext( IndexingContext.class );
        } else {
            return null;
        }
    }

    private StorageAsset getIndexPath( ArchivaIndexingContext ctx )
    {
        return ctx.getPath( );
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
        if (context==null) {
            throw new IndexUpdateFailedException( "Given context is null" );
        }
        IndexingContext indexingContext = null;
        try
        {
            indexingContext = getMvnContext( context );
        }
        catch ( UnsupportedBaseContextException e )
        {
            throw new IndexUpdateFailedException( "Maven index is not supported by this context", e );
        }
        final StorageAsset ctxPath = getIndexPath( context );
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
        if ( !( context.getRepository( ) instanceof RemoteRepository ) || !(context.getRepository().supportsFeature(RemoteIndexFeature.class)) )
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
                        org.apache.archiva.common.utils.FileUtils.deleteDirectory( tempIndexDirectory );
                    }
                    Files.createDirectories( tempIndexDirectory );
                    tempIndexDirectory.toFile( ).deleteOnExit( );
                    String baseIndexUrl = indexingContext.getIndexUpdateUrl( );

                    String wagonProtocol = remoteUpdateUri.toURL( ).getProtocol( );

                    NetworkProxy networkProxy = null;
                    if ( remoteRepository.supportsFeature( RemoteIndexFeature.class ) )
                    {
                        RemoteIndexFeature rif = remoteRepository.getFeature( RemoteIndexFeature.class ).get( );
                        if ( StringUtils.isNotBlank( rif.getProxyId( ) ) )
                        {
                            networkProxy = proxyRegistry.getNetworkProxy( rif.getProxyId( ) );
                            if ( networkProxy == null )
                            {
                                log.warn(
                                    "your remote repository is configured to download remote index trought a proxy we cannot find id:{}",
                                    rif.getProxyId( ) );
                            }
                        }

                        final StreamWagon wagon = (StreamWagon) wagonFactory.getWagon(
                            new WagonFactoryRequest( wagonProtocol, remoteRepository.getExtraHeaders( ) ).networkProxy(
                                networkProxy )
                        );
                        int readTimeout = (int) rif.getDownloadTimeout( ).toMillis( ) * 1000;
                        wagon.setReadTimeout( readTimeout );
                        wagon.setTimeout( (int) remoteRepository.getTimeout( ).toMillis( ) * 1000 );

                        if ( wagon instanceof AbstractHttpClientWagon )
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
                            proxyInfo.setPassword( new String(networkProxy.getPassword( )) );
                        }
                        AuthenticationInfo authenticationInfo = null;
                        if ( remoteRepository.getLoginCredentials( ) != null && ( remoteRepository.getLoginCredentials( ) instanceof PasswordCredentials ) )
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

                        indexUpdater.fetchAndUpdateIndex( request );

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
        final StorageAsset ctxUri = context.getPath();
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

        return new MavenIndexContext( repository, mvnCtx );
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
                icf.setLocalPackedIndexPath(getPackedIndexPath(repo));
            } catch (IOException e) {
                log.error("Could not set local index path for {}. New URI: {}", repo.getId(), icf.getIndexPath());
            }
        }
    }

    @Override
    public ArchivaIndexingContext mergeContexts(Repository destinationRepo, List<ArchivaIndexingContext> contexts,
                                                boolean packIndex) throws UnsupportedOperationException,
            IndexCreationFailedException, IllegalArgumentException {
        if (!destinationRepo.supportsFeature(IndexCreationFeature.class)) {
            throw new IllegalArgumentException("The given repository does not support the indexcreation feature");
        }
        Path mergedIndexDirectory = null;
        try {
            mergedIndexDirectory = Files.createTempDirectory("archivaMergedIndex");
        } catch (IOException e) {
            log.error("Could not create temporary directory for merged index: {}", e.getMessage(), e);
            throw new IndexCreationFailedException("IO error while creating temporary directory for merged index: "+e.getMessage(), e);
        }
        IndexCreationFeature indexCreationFeature = destinationRepo.getFeature(IndexCreationFeature.class).get();
        if (indexCreationFeature.getLocalIndexPath()== null) {
            throw new IllegalArgumentException("The given repository does not have a local index path");
        }
        StorageAsset destinationPath = indexCreationFeature.getLocalIndexPath();

        String tempRepoId = mergedIndexDirectory.getFileName().toString();

        try
        {
            Path indexLocation = destinationPath.getFilePath();

            List<IndexingContext> members = contexts.stream( ).filter(ctx -> ctx.supports(IndexingContext.class)).map( ctx ->
            {
                try {
                    return ctx.getBaseContext(IndexingContext.class);
                } catch (UnsupportedBaseContextException e) {
                    // does not happen here
                    return null;
                }
            }).filter( Objects::nonNull ).collect( Collectors.toList() );
            ContextMemberProvider memberProvider = new StaticContextMemberProvider(members);
            IndexingContext mergedCtx = indexer.createMergedIndexingContext( tempRepoId, tempRepoId, mergedIndexDirectory.toFile(),
                    indexLocation.toFile(), true, memberProvider);
            mergedCtx.optimize();

            if ( packIndex )
            {
                IndexPackingRequest request = new IndexPackingRequest( mergedCtx, //
                        mergedCtx.acquireIndexSearcher().getIndexReader(), //
                        indexLocation.toFile() );
                indexPacker.packIndex( request );
            }

            return new MavenIndexContext(destinationRepo, mergedCtx);
        }
        catch ( IOException e)
        {
            throw new IndexCreationFailedException( "IO Error during index merge: "+ e.getMessage(), e );
        }
    }

    private StorageAsset getIndexPath(URI indexDirUri, RepositoryStorage repoStorage, String defaultDir) throws IOException
    {
        StorageAsset rootAsset = repoStorage.getAsset("");
        RepositoryStorage storage = rootAsset.getStorage();
        Path indexDirectory;
        Path repositoryPath = rootAsset.getFilePath().toAbsolutePath();
        StorageAsset indexDir;
        if ( ! StringUtils.isEmpty(indexDirUri.toString( ) ) )
        {

            indexDirectory = PathUtil.getPathFromUri( indexDirUri );
            // not absolute so create it in repository directory
            if ( indexDirectory.isAbsolute( ) && !indexDirectory.startsWith(repositoryPath))
            {
                if (storage instanceof FilesystemStorage) {
                    FilesystemStorage fsStorage = (FilesystemStorage) storage;
                    FilesystemStorage indexStorage = new FilesystemStorage(indexDirectory.getParent(), fsStorage.getFileLockManager());
                    indexDir = indexStorage.getAsset(indexDirectory.getFileName().toString());
                } else {
                    throw new IOException("The given storage is not file based.");
                }
            } else if (indexDirectory.isAbsolute()) {
                indexDir = storage.getAsset(repositoryPath.relativize(indexDirectory).toString());
            }
            else
            {
                indexDir = storage.getAsset(indexDirectory.toString());
            }
        }
        else
        {
            indexDir = storage.getAsset( defaultDir );
        }

        if ( !indexDir.exists() )
        {
            indexDir = storage.addAsset(indexDir.getPath(), true);
        }
        return indexDir;
    }

    private StorageAsset getIndexPath( Repository repo) throws IOException {
        IndexCreationFeature icf = repo.getFeature(IndexCreationFeature.class).get();
        return getIndexPath( icf.getIndexPath(), repo, DEFAULT_INDEX_PATH);
    }

    private StorageAsset getPackedIndexPath(Repository repo) throws IOException {
        IndexCreationFeature icf = repo.getFeature(IndexCreationFeature.class).get();
        return getIndexPath(icf.getPackedIndexPath(), repo, DEFAULT_PACKED_INDEX_PATH);
    }

    private IndexingContext createRemoteContext(RemoteRepository remoteRepository ) throws IOException
    {
        String contextKey = "remote-" + remoteRepository.getId( );


        // create remote repository path
        Path repoDir = remoteRepository.getAsset( "" ).getFilePath();
        if ( !Files.exists( repoDir ) )
        {
            Files.createDirectories( repoDir );
        }

        StorageAsset indexDirectory;

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
                org.apache.archiva.common.utils.FileUtils.deleteDirectory( indexDirectory.getFilePath() );
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
        try
        {
            if (!Files.exists(indexDirectory.getFilePath())) {
                Files.createDirectories(indexDirectory.getFilePath());
            }
            return indexer.createIndexingContext( contextKey, repository.getId( ), repoDir.toFile( ), indexDirectory.getFilePath( ).toFile( ),
                repository.getLocation( ) == null ? null : repository.getLocation( ).toString( ),
                indexUrl,
                true, false,
                indexCreators );
        } catch (Exception e) {
            log.error("Could not create index for asset {}", indexDirectory);
            throw new IOException(e);
        }
    }

    private IndexingContext createManagedContext( ManagedRepository repository ) throws IOException
    {

        IndexingContext context;
        // take care first about repository location as can be relative
        Path repositoryDirectory = repository.getAsset( "" ).getFilePath();

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

        StorageAsset indexDirectory;

        if ( repository.supportsFeature( IndexCreationFeature.class ) )
        {
            indexDirectory = getIndexPath(repository);
            log.debug( "Preparing index at {}", indexDirectory );

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
                org.apache.archiva.common.utils.FileUtils.deleteDirectory( indexDirectory.getFilePath() );
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
            return baseUri.resolve( DEFAULT_INDEX_PATH ).toString( );
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
        public void connect( String id, String url ) {
            //no op
        }

        @Override
        public void disconnect( ) {
            // no op
        }

        @Override
        public InputStream retrieve( String name )
            throws IOException {
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
