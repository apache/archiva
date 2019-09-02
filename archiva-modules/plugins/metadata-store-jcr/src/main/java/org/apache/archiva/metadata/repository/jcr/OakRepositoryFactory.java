package org.apache.archiva.metadata.repository.jcr;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.index.AsyncIndexInfoService;
import org.apache.jackrabbit.oak.plugins.index.AsyncIndexInfoServiceImpl;
import org.apache.jackrabbit.oak.plugins.index.IndexInfoProvider;
import org.apache.jackrabbit.oak.plugins.index.IndexPathService;
import org.apache.jackrabbit.oak.plugins.index.IndexPathServiceImpl;
import org.apache.jackrabbit.oak.plugins.index.IndexUtils;
import org.apache.jackrabbit.oak.plugins.index.aggregate.SimpleNodeAggregator;
import org.apache.jackrabbit.oak.plugins.index.lucene.IndexAugmentorFactory;
import org.apache.jackrabbit.oak.plugins.index.lucene.IndexCopier;
import org.apache.jackrabbit.oak.plugins.index.lucene.IndexTracker;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexInfoProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.directory.ActiveDeletedBlobCollectorFactory;
import org.apache.jackrabbit.oak.plugins.index.lucene.directory.BufferedOakDirectory;
import org.apache.jackrabbit.oak.plugins.index.lucene.directory.LuceneIndexImporter;
import org.apache.jackrabbit.oak.plugins.index.lucene.hybrid.DocumentQueue;
import org.apache.jackrabbit.oak.plugins.index.lucene.hybrid.ExternalObserverBuilder;
import org.apache.jackrabbit.oak.plugins.index.lucene.hybrid.LocalIndexObserver;
import org.apache.jackrabbit.oak.plugins.index.lucene.hybrid.NRTIndexFactory;
import org.apache.jackrabbit.oak.plugins.index.lucene.property.PropertyIndexCleaner;
import org.apache.jackrabbit.oak.plugins.index.lucene.reader.DefaultIndexReaderFactory;
import org.apache.jackrabbit.oak.plugins.index.lucene.score.ScorerProviderFactory;
import org.apache.jackrabbit.oak.plugins.index.lucene.score.impl.ScorerProviderFactoryImpl;
import org.apache.jackrabbit.oak.plugins.index.lucene.util.IndexDefinitionBuilder;
import org.apache.jackrabbit.oak.plugins.index.search.ExtractedTextCache;
import org.apache.jackrabbit.oak.plugins.index.search.FulltextIndexConstants;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.plugins.name.Namespaces;
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders;
import org.apache.jackrabbit.oak.segment.file.FileStore;
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.apache.jackrabbit.oak.spi.blob.FileBlobStore;
import org.apache.jackrabbit.oak.spi.blob.GarbageCollectableBlobStore;
import org.apache.jackrabbit.oak.spi.commit.BackgroundObserver;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.mount.MountInfoProvider;
import org.apache.jackrabbit.oak.spi.mount.Mounts;
import org.apache.jackrabbit.oak.spi.namespace.NamespaceConstants;
import org.apache.jackrabbit.oak.spi.query.QueryIndex;
import org.apache.jackrabbit.oak.spi.query.QueryIndexProvider;
import org.apache.jackrabbit.oak.spi.state.Clusterable;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.oak.stats.StatisticsProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.archiva.metadata.repository.jcr.JcrConstants.*;
import static org.apache.archiva.metadata.repository.jcr.OakRepositoryFactory.StoreType.IN_MEMORY_TYPE;
import static org.apache.archiva.metadata.repository.jcr.OakRepositoryFactory.StoreType.SEGMENT_FILE_TYPE;
import static org.apache.commons.io.FileUtils.ONE_MB;
import static org.apache.jackrabbit.JcrConstants.*;
import static org.apache.jackrabbit.oak.api.Type.NAME;

/**
 * Created by martin on 14.06.17.
 *
 * @author Martin Stockhammer
 * @since 3.0.0
 */
public class OakRepositoryFactory
{

    private Logger log = LoggerFactory.getLogger( OakRepositoryFactory.class );

    private FileStore fileStore;

    private NodeStore nodeStore;

    private IndexTracker tracker;

    private DocumentQueue documentQueue;

    private NRTIndexFactory nrtIndexFactory;

    private IndexCopier indexCopier;

    private ExecutorService executorService;
    private ExtractedTextCache extractedTextCache;

    private boolean hybridIndex = true;
    private boolean prefetchEnabled = true;
    private boolean enableAsyncIndexOpen = true;
    int queueSize = 10000;
    int cleanerInterval = 10*60;
    boolean enableCopyOnWrite = true;
    boolean enableCopyOnRead = true;
    int cacheSizeInMB = 20;
    int cacheExpiryInSecs = 300;
    int threadPoolSize = 5;

    private StatisticsProvider statisticsProvider;

    private MountInfoProvider mountInfoProvider =  Mounts.defaultMountInfoProvider();

    private AsyncIndexInfoService asyncIndexInfoService = null;

    private LuceneIndexProvider indexProvider;

    private ScorerProviderFactory scorerFactory = new ScorerProviderFactoryImpl( );
    private IndexAugmentorFactory augmentorFactory = new IndexAugmentorFactory( );

    private ActiveDeletedBlobCollectorFactory.ActiveDeletedBlobCollector activeDeletedBlobCollector = ActiveDeletedBlobCollectorFactory.NOOP;

    private QueryIndex.NodeAggregator nodeAggregator = new SimpleNodeAggregator( );

    private BackgroundObserver backgroundObserver;

    private BackgroundObserver externalIndexObserver;

    private GarbageCollectableBlobStore blobStore;

    private PropertyIndexCleaner cleaner;

    private IndexPathService indexPathService;

    private LuceneIndexEditorProvider editorProvider;

    private Path indexDir;

    public enum StoreType
    {
        SEGMENT_FILE_TYPE,
        IN_MEMORY_TYPE;
    }

    private StoreType storeType = SEGMENT_FILE_TYPE;

    private Path repositoryPath = Paths.get( "repository" );

    public OakRepositoryFactory() {
        final OakRepositoryFactory repositoryFactory = this;
        Runtime.getRuntime().addShutdownHook( new Thread( ( ) -> {
            if (repositoryFactory!=null)
            {
                repositoryFactory.close( );
            }
        } ) );
    }

    private void initializeExtractedTextCache( StatisticsProvider statisticsProvider) {
        boolean alwaysUsePreExtractedCache = false;

        extractedTextCache = new ExtractedTextCache(
            cacheSizeInMB * ONE_MB,
            cacheExpiryInSecs,
            alwaysUsePreExtractedCache,
            indexDir.toFile(), statisticsProvider);
    }

    private IndexTracker createTracker() throws IOException {
        IndexTracker tracker;
        if (enableCopyOnRead){
            initializeIndexCopier();
            log.info("Enabling CopyOnRead support. Index files would be copied under {}", indexDir.toAbsolutePath());
            if (hybridIndex) {
                nrtIndexFactory = new NRTIndexFactory(indexCopier, statisticsProvider);
            }
            tracker = new IndexTracker(new DefaultIndexReaderFactory(mountInfoProvider, indexCopier), nrtIndexFactory);
        } else {
            tracker = new IndexTracker(new DefaultIndexReaderFactory(mountInfoProvider, null));
        }

        tracker.setAsyncIndexInfoService(asyncIndexInfoService);
        tracker.refresh();
        return tracker;
    }

    private void initializeIndexCopier() throws IOException {
        if(indexCopier != null){
            return;
        }

        if (prefetchEnabled){
            log.info("Prefetching of index files enabled. Index would be opened after copying all new files locally");
        }

        indexCopier = new IndexCopier(getExecutorService(), indexDir.toFile(), prefetchEnabled);

    }

    ExecutorService getExecutorService(){
        if (executorService == null){
            executorService = createExecutor();
        }
        return executorService;
    }

    private ExecutorService createExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();
            private final Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    log.warn("Error occurred in asynchronous processing ", e);
                }
            };
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r, createName());
                thread.setDaemon(true);
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.setUncaughtExceptionHandler(handler);
                return thread;
            }

            private String createName() {
                return "oak-lucene-" + counter.getAndIncrement();
            }
        });
        executor.setKeepAliveTime(1, TimeUnit.MINUTES);
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private void initialize(){
        if(indexProvider == null){
            return;
        }

        if(nodeAggregator != null){
            log.debug("Using NodeAggregator {}", nodeAggregator.getClass());
        }

        indexProvider.setAggregator(nodeAggregator);
    }

    private void registerObserver() {
        Observer observer = indexProvider;
        if (enableAsyncIndexOpen) {
            backgroundObserver = new BackgroundObserver(indexProvider, getExecutorService(), 5);
            log.info("Registering the LuceneIndexProvider as a BackgroundObserver");
        }
    }

    private void registerLocalIndexObserver(IndexTracker tracker) {
        if (!hybridIndex){
            log.info("Hybrid indexing feature disabled");
            return;
        }
        documentQueue = new DocumentQueue( queueSize, tracker, getExecutorService(), statisticsProvider);
        LocalIndexObserver localIndexObserver = new LocalIndexObserver(documentQueue, statisticsProvider);

        int observerQueueSize = 1000;
        int builderMaxSize = 5000;
        // regs.add(bundleContext.registerService(JournalPropertyService.class.getName(),
        //    new LuceneJournalPropertyService(builderMaxSize), null));
        ExternalObserverBuilder builder = new ExternalObserverBuilder(documentQueue, tracker, statisticsProvider,
            getExecutorService(), observerQueueSize);
        log.info("Configured JournalPropertyBuilder with max size {} and backed by BackgroundObserver " +
            "with queue size {}", builderMaxSize, observerQueueSize);

        Observer observer = builder.build();
        externalIndexObserver = builder.getBackgroundObserver();
        log.info("Hybrid indexing enabled for configured indexes with queue size of {}", queueSize );
    }

    private IndexInfoProvider registerIndexInfoProvider() {
        return new LuceneIndexInfoProvider(nodeStore, asyncIndexInfoService, getIndexCheckDir().toFile());
    }

    private Path getIndexCheckDir() {
        return checkNotNull(indexDir).resolve("indexCheckDir");
    }

    private LuceneIndexImporter registerIndexImporterProvider() {
        return new LuceneIndexImporter(blobStore);
    }

    private void registerPropertyIndexCleaner( ) {

        if (cleanerInterval <= 0) {
            log.info("Property index cleaner would not be registered");
            return;
        }

        cleaner = new PropertyIndexCleaner(nodeStore, indexPathService, asyncIndexInfoService, statisticsProvider);

        //Proxy check for DocumentNodeStore
        if (nodeStore instanceof Clusterable ) {
            cleaner.setRecursiveDelete(true);
            log.info("PropertyIndexCleaner configured to perform recursive delete");
        }
        log.info("Property index cleaner configured to run every [{}] seconds", cleanerInterval);
    }

    private void registerIndexEditor( IndexTracker tracker) throws IOException {
        boolean enableCopyOnWrite = true;
        if (enableCopyOnWrite){
            initializeIndexCopier();
            editorProvider = new LuceneIndexEditorProvider(indexCopier, tracker, extractedTextCache,
                augmentorFactory,  mountInfoProvider, activeDeletedBlobCollector, null, statisticsProvider);
            log.info("Enabling CopyOnWrite support. Index files would be copied under {}", indexDir.toAbsolutePath());
        } else {
            editorProvider = new LuceneIndexEditorProvider(null, tracker, extractedTextCache, augmentorFactory,
                mountInfoProvider, activeDeletedBlobCollector, null, statisticsProvider);
        }
        editorProvider.setBlobStore(blobStore);

        if (hybridIndex){
            editorProvider.setIndexingQueue(checkNotNull(documentQueue));
        }


    }

    public Repository createRepository()
        throws IOException, InvalidFileStoreVersionException
    {

        indexDir = repositoryPath.resolve( ".index-lucene" );
        if (!Files.exists( indexDir )) {
            Files.createDirectories( indexDir );
        }
        blobStore = new FileBlobStore( indexDir.resolve( "blobs" ).toAbsolutePath().toString() );

        statisticsProvider = StatisticsProvider.NOOP;

        if ( SEGMENT_FILE_TYPE == storeType )
        {
            fileStore = FileStoreBuilder.fileStoreBuilder( repositoryPath.toFile() )
                .withStatisticsProvider( statisticsProvider )
                .build();
            nodeStore = SegmentNodeStoreBuilders.builder( fileStore ) //
                .withStatisticsProvider( statisticsProvider ) //
                .build();
        }
        else if ( IN_MEMORY_TYPE == storeType )
        {
            nodeStore = new MemoryNodeStore( );
        }
        else
        {
            throw new IllegalArgumentException( "Store type " + storeType + " not recognized" );
        }

        asyncIndexInfoService = new AsyncIndexInfoServiceImpl( nodeStore );

        indexPathService = new IndexPathServiceImpl( nodeStore, mountInfoProvider );

        BufferedOakDirectory.setEnableWritingSingleBlobIndexFile( true );

        initializeExtractedTextCache( statisticsProvider );

        tracker = createTracker();

        indexProvider = new LuceneIndexProvider(tracker, scorerFactory, augmentorFactory);

        initialize();
        registerObserver();
        registerLocalIndexObserver(tracker);
        registerIndexInfoProvider();
        registerIndexImporterProvider();
        registerPropertyIndexCleaner();

        registerIndexEditor(tracker);



        RepositoryInitializer repoInitializer = new RepositoryInitializer( )
        {
            private IndexDefinitionBuilder.PropertyRule initRegexAll( IndexDefinitionBuilder.IndexRule rule ) {
                return rule
                    .indexNodeName( )
                    .property(JCR_LASTMODIFIED ).propertyIndex().type( "Date" ).ordered()
                    .property(JCR_PRIMARYTYPE).propertyIndex()
                    .property(JCR_MIXINTYPES).propertyIndex()
                    .property(JCR_PATH).propertyIndex().ordered()
                    .property( FulltextIndexConstants.REGEX_ALL_PROPS, true )
                    .propertyIndex().analyzed( ).nodeScopeIndex();
            }

            private IndexDefinitionBuilder.PropertyRule initBaseRule( IndexDefinitionBuilder.IndexRule rule ) {
                return rule
                    .indexNodeName( )
                    .property(JCR_CREATED).propertyIndex().type("Date").ordered()
                    .property(JCR_LASTMODIFIED ).propertyIndex().type( "Date" ).ordered()
                    .property(JCR_PRIMARYTYPE).propertyIndex()
                    .property(JCR_MIXINTYPES).propertyIndex()
                    .property(JCR_PATH).propertyIndex().ordered()
                    .property( "id" ).propertyIndex().analyzed( );
            }

            @Override
            public void initialize(  NodeBuilder root )
            {
                NodeBuilder namespaces;
                if ( !root.hasChildNode( NamespaceConstants.REP_NAMESPACES ) )
                {
                    namespaces = Namespaces.createStandardMappings( root );
                    Namespaces.buildIndexNode( namespaces ); // index node for faster lookup
                }
                else
                {
                    namespaces = root.getChildNode( NamespaceConstants.REP_NAMESPACES );
                }
                Namespaces.addCustomMapping( namespaces, "http://archiva.apache.org/jcr/", "archiva" );

                log.info( "Creating index " );

                NodeBuilder oakIdx = IndexUtils.getOrCreateOakIndex( root );
                if (!oakIdx.hasChildNode( "repo-lucene" ))
                {
                    NodeBuilder lucene = oakIdx.child( "repo-lucene" );
                    lucene.setProperty( JCR_PRIMARYTYPE, "oak:QueryIndexDefinition", NAME );

                    lucene.setProperty( "compatVersion", 2 );
                    lucene.setProperty( "type", "lucene" );
                    // lucene.setProperty("async", "async");
                    // lucene.setProperty( INCLUDE_PROPERTY_TYPES, ImmutableSet.of(  ), Type.STRINGS );
                    // lucene.setProperty("refresh",true);
                    NodeBuilder rules = lucene.child( "indexRules" ).
                        setProperty( JCR_PRIMARYTYPE, NT_UNSTRUCTURED, NAME );
                    rules.setProperty( ":childOrder", ImmutableSet.of(
                        REPOSITORY_NODE_TYPE,
                            NAMESPACE_MIXIN_TYPE, //
                            PROJECT_MIXIN_TYPE,
                        PROJECT_VERSION_NODE_TYPE, //
                        ARTIFACT_NODE_TYPE, //
                        FACET_NODE_TYPE //
                    ), Type.STRINGS );
                    IndexDefinitionBuilder idxBuilder = new IndexDefinitionBuilder( lucene );
                    idxBuilder.async( "async", "nrt", "sync" ).includedPaths( "/repositories" ).evaluatePathRestrictions();

                    initBaseRule(idxBuilder.indexRule( REPOSITORY_NODE_TYPE ));
                    initBaseRule(idxBuilder.indexRule(NAMESPACE_MIXIN_TYPE))
                        .property( "namespace" ).propertyIndex().analyzed();
                    initBaseRule(idxBuilder.indexRule(PROJECT_MIXIN_TYPE))
                        .property( "name" ).propertyIndex().analyzed().notNullCheckEnabled().nullCheckEnabled();
                    initBaseRule( idxBuilder.indexRule( PROJECT_VERSION_NODE_TYPE ) )
                        .property("name").propertyIndex().analyzed().notNullCheckEnabled().nullCheckEnabled()
                        .property("description").propertyIndex().analyzed().notNullCheckEnabled().nullCheckEnabled()
                        .property("url").propertyIndex().analyzed( ).notNullCheckEnabled().nullCheckEnabled()
                        .property("incomplete").type("Boolean").propertyIndex()
                        .property("mailinglist/name").propertyIndex().analyzed()
                        .property("license/license.name").propertyIndex().analyzed();
                    initBaseRule(idxBuilder.indexRule( ARTIFACT_NODE_TYPE ))
                        .property( "whenGathered" ).type("Date").propertyIndex().analyzed().ordered()
                        .property("size").type("Long").propertyIndex().analyzed().ordered()
                        .property("version").propertyIndex().analyzed().ordered()
                        .property("checksums/*/value").propertyIndex();

                    initBaseRule( idxBuilder.indexRule( CHECKSUM_NODE_TYPE ) )
                        .property("type").propertyIndex()
                        .property("value").propertyIndex();

                    initRegexAll( idxBuilder.indexRule( FACET_NODE_TYPE ) )
                        .property("archiva:facetId").propertyIndex().analyzed().ordered()
                        .property("archiva:name").propertyIndex().analyzed().ordered().nullCheckEnabled().notNullCheckEnabled();

                    idxBuilder.indexRule( MIXIN_META_SCM )
                        .property( "scm.connection" ).propertyIndex()
                        .property( "scm.developerConnection" ).propertyIndex()
                        .property( "scm.url").type("URI").propertyIndex().analyzed();
                    idxBuilder.indexRule( MIXIN_META_CI )
                        .property( "ci.system" ).propertyIndex( )
                        .property( "ci.ur" ).propertyIndex( ).analyzed( );
                    idxBuilder.indexRule( MIXIN_META_ISSUE )
                        .property( "issue.system").propertyIndex()
                        .property("issue.url").propertyIndex().analyzed();
                    idxBuilder.indexRule( MIXIN_META_ORGANIZATION )
                        .property( "org.name" ).propertyIndex( ).analyzed( )
                        .property( "org.url" ).propertyIndex( ).analyzed( );
                    idxBuilder.indexRule( LICENSE_NODE_TYPE )
                        .property( "license.name" ).propertyIndex( ).analyzed( )
                        .property( "license.url" ).propertyIndex( ).analyzed( );
                    idxBuilder.indexRule( MAILINGLIST_NODE_TYPE )
                        .property( "name" ).propertyIndex().analyzed();
                    initBaseRule(idxBuilder.indexRule( DEPENDENCY_NODE_TYPE ))
                        .property( "groupId" ).propertyIndex().analyzed().ordered()
                        .property( "artifactId").propertyIndex().analyzed().ordered()
                        .property("version").propertyIndex().analyzed().ordered()
                        .property("type").propertyIndex().analyzed().ordered()
                        .property( "classifier" ).propertyIndex().ordered()
                        .property("scope").propertyIndex()
                        .property("systemPath").propertyIndex().analyzed()
                        .property("optional").type("Boolean").propertyIndex();

                    idxBuilder.aggregateRule( PROJECT_VERSION_NODE_TYPE ).include( "dependencies")
                        .path("dependencies/*" ).relativeNode();

                    idxBuilder.build( );

                    IndexUtils.createIndexDefinition( oakIdx, "baseIndexes", true, false, ImmutableList.of( "jcr:uuid", "rep:principalName" ), null );

                    log.info( "Index: {} repo-lucene: {}", lucene, lucene.getChildNode( "repo-lucene" ) );
                    log.info( "repo-lucene Properties: {}", lucene.getChildNode( "repo-lucene" ).getProperties( ) );
                } else {
                    log.info( "No Index update" );
                }
                // IndexUtils.createIndexDefinition(  )

            }
        };

        //        ExternalObserverBuilder builder = new ExternalObserverBuilder(queue, tracker, statsProvider,
//            executorService, queueSize);
//        Observer observer = builder.build();
//        builder.getBackgroundObserver();



        log.info( "Starting Jcr repo with nodeStore {}", nodeStore );
        Jcr jcr = new Jcr( nodeStore ).with( editorProvider ) //
            .with( backgroundObserver ) //
            .with( externalIndexObserver )
            // .with(observer)
            .with( (QueryIndexProvider) indexProvider )
            .with (repoInitializer)
            .withAsyncIndexing( "async", 5 );
            //
            //.withAsyncIndexing( "async", 5 );
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Repository r = jcr.createRepository();
        stopWatch.stop();
        log.info( "time to create jcr repository: {} ms", stopWatch.getTime() );

        return r;


    }

    private void closeSilently( Closeable service) {
        if (service!=null) {
            try
            {
                service.close();
            }
            catch ( Throwable e )
            {
                //
            }
        }
    }

    public void close()
    {
        log.info( "Closing JCR RepositoryFactory" );
        closeSilently( fileStore );
        closeSilently( backgroundObserver );
        closeSilently( externalIndexObserver );
        closeSilently( indexProvider );
        indexProvider = null;
        closeSilently( documentQueue );
        closeSilently( nrtIndexFactory );
        closeSilently( indexCopier );

        if (executorService != null){
            executorService.shutdown();
            try
            {
                executorService.awaitTermination(1, TimeUnit.MINUTES);
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace( );
            }
        }

        if (extractedTextCache != null) {
            extractedTextCache.close();
        }

    }

    public StoreType getStoreType()
    {
        return storeType;
    }

    public void setStoreType( StoreType storeType )
    {
        this.storeType = storeType;
    }

    public Path getRepositoryPath()
    {
        return repositoryPath;
    }

    public void setRepositoryPath( Path repositoryPath )
    {
        this.repositoryPath = repositoryPath;
    }

    public void setRepositoryPath( String repositoryPath )
    {
        this.repositoryPath = Paths.get( repositoryPath );
        if ( !Files.exists( this.repositoryPath ) )
        {
            try
            {
                Files.createDirectories( this.repositoryPath );
            }
            catch ( IOException e )
            {
                log.error( e.getMessage(), e );
                throw new IllegalArgumentException( "cannot create directory:" + repositoryPath, e );
            }
        }
    }


}
