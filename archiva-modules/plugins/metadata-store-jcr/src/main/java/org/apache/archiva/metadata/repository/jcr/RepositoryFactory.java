package org.apache.archiva.metadata.repository.jcr;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.index.IndexConstants;
import org.apache.jackrabbit.oak.plugins.index.IndexUtils;
import org.apache.jackrabbit.oak.plugins.index.lucene.ExtractedTextCache;
import org.apache.jackrabbit.oak.plugins.index.lucene.IndexCopier;
import org.apache.jackrabbit.oak.plugins.index.lucene.IndexTracker;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.hybrid.DocumentQueue;
import org.apache.jackrabbit.oak.plugins.index.lucene.hybrid.ExternalObserverBuilder;
import org.apache.jackrabbit.oak.plugins.index.lucene.hybrid.LocalIndexObserver;
import org.apache.jackrabbit.oak.plugins.index.lucene.hybrid.NRTIndexFactory;
import org.apache.jackrabbit.oak.plugins.index.lucene.reader.DefaultIndexReaderFactory;
import org.apache.jackrabbit.oak.plugins.index.lucene.util.LuceneInitializerHelper;
import org.apache.jackrabbit.oak.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders;
import org.apache.jackrabbit.oak.segment.file.FileStore;
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer;
import org.apache.jackrabbit.oak.spi.mount.MountInfoProvider;
import org.apache.jackrabbit.oak.spi.mount.Mounts;
import org.apache.jackrabbit.oak.spi.query.QueryIndexProvider;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.oak.stats.StatisticsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.Repository;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.jackrabbit.oak.plugins.index.IndexConstants.INDEX_DEFINITIONS_NAME;
import static org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexConstants.INCLUDE_PROPERTY_TYPES;

/**
 * Created by martin on 14.06.17.
 *
 * @author Martin Stockhammer
 * @since 3.0.0
 */
public class RepositoryFactory
{

    Logger log = LoggerFactory.getLogger( RepositoryFactory.class );

    public static final String SEGMENT_FILE_TYPE = "oak-segment-tar";
    public static final String IN_MEMORY_TYPE = "oak-memory";

    String storeType = SEGMENT_FILE_TYPE;

    Path repositoryPath = Paths.get( "repository" );

    public Repository createRepository( ) throws IOException, InvalidFileStoreVersionException
    {
        NodeStore nodeStore;
        if ( SEGMENT_FILE_TYPE.equals( storeType ) )
        {
            FileStore fs = FileStoreBuilder.fileStoreBuilder( repositoryPath.toFile( ) ).build( );
            nodeStore = SegmentNodeStoreBuilders.builder( fs ).build( );
        } else if (IN_MEMORY_TYPE.equals(storeType)) {
            nodeStore = null;
        } else {
            throw new IllegalArgumentException( "Store type "+storeType+" not recognized" );
        }

        Oak oak = nodeStore==null ? new Oak() : new Oak(nodeStore);
        oak.with( new RepositoryInitializer( )
        {
            @Override
            public void initialize( @Nonnull NodeBuilder root )
            {
                log.info("Creating index ");

                NodeBuilder lucene = IndexUtils.getOrCreateOakIndex( root ).child("lucene");
                lucene.setProperty( JcrConstants.JCR_PRIMARYTYPE, "oak:QueryIndexDefinition", Type.NAME);

                lucene.setProperty("compatVersion", 2);
                lucene.setProperty("type", "lucene");
                // lucene.setProperty("async", "async");
                lucene.setProperty(INCLUDE_PROPERTY_TYPES,
                    ImmutableSet.of("String"), Type.STRINGS);
                // lucene.setProperty("refresh",true);
                lucene.setProperty("async",ImmutableSet.of("async", "sync"), Type.STRINGS);
                NodeBuilder rules = lucene.child("indexRules").
                    setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED, Type.NAME);
                rules.setProperty(":childOrder",ImmutableSet.of("archiva:projectVersion","archiva:artifact",
                    "archiva:facet","archiva:namespace", "archiva:project"), Type.STRINGS);
                NodeBuilder allProps = rules.child("archiva:projectVersion")
                    .child("properties").setProperty( JcrConstants.JCR_PRIMARYTYPE,
                        "nt:unstructured", Type.NAME)
                    .setProperty( ":childOrder", ImmutableSet.of("allProps"), Type.STRINGS )
                    .setProperty("indexNodeName",true)
                    .child("allProps").setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED, Type.NAME);
                allProps.setProperty("name", ".*");
                allProps.setProperty("isRegexp", true);
                allProps.setProperty("nodeScopeIndex", true);
                allProps.setProperty("index",true);
                allProps.setProperty("analyzed",true);
                // allProps.setProperty("propertyIndex",true);
                allProps = rules.child("archiva:artifact")
                    .child("properties").setProperty( JcrConstants.JCR_PRIMARYTYPE,
                        "nt:unstructured", Type.NAME)
                    .setProperty( ":childOrder", ImmutableSet.of("allProps"), Type.STRINGS )
                    .setProperty("indexNodeName",true)
                    .child("allProps").setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED, Type.NAME);
                allProps.setProperty("name", ".*");
                allProps.setProperty("isRegexp", true);
                allProps.setProperty("nodeScopeIndex", true);
                allProps.setProperty("index",true);
                allProps.setProperty("analyzed",true);
                allProps = rules.child("archiva:facet")
                    .child("properties").setProperty( JcrConstants.JCR_PRIMARYTYPE,
                        "nt:unstructured", Type.NAME)
                    .setProperty( ":childOrder", ImmutableSet.of("allProps"), Type.STRINGS )
                    .setProperty("indexNodeName",true)
                    .child("allProps").setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED, Type.NAME);
                allProps.setProperty("name", ".*");
                allProps.setProperty("isRegexp", true);
                allProps.setProperty("nodeScopeIndex", true);
                allProps.setProperty("index",true);
                allProps.setProperty("analyzed",true);
                allProps = rules.child("archiva:namespace")
                    .child("properties").setProperty( JcrConstants.JCR_PRIMARYTYPE,
                        "nt:unstructured", Type.NAME)
                    .setProperty( ":childOrder", ImmutableSet.of("allProps"), Type.STRINGS )
                    .setProperty("indexNodeName",true)
                    .child("allProps").setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED, Type.NAME);
                allProps.setProperty("name", ".*");
                allProps.setProperty("isRegexp", true);
                allProps.setProperty("nodeScopeIndex", true);
                allProps.setProperty("index",true);
                allProps.setProperty("analyzed",true);
                allProps = rules.child("archiva:project")
                    .child("properties").setProperty( JcrConstants.JCR_PRIMARYTYPE,
                        "nt:unstructured", Type.NAME)
                    .setProperty( ":childOrder", ImmutableSet.of("allProps"), Type.STRINGS )
                    .setProperty("indexNodeName",true)
                    .child("allProps").setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED, Type.NAME);
                allProps.setProperty("name", ".*");
                allProps.setProperty("isRegexp", true);
                allProps.setProperty("nodeScopeIndex", true);
                allProps.setProperty("index",true);
                allProps.setProperty("analyzed",true);

                log.info("Index: "+lucene+" myIndex "+lucene.getChildNode( "myIndex" ));
                log.info("myIndex "+lucene.getChildNode( "myIndex" ).getProperties());
                // IndexUtils.createIndexDefinition(  )

            }
        } );

        ExecutorService executorService = createExecutor();
        StatisticsProvider statsProvider = StatisticsProvider.NOOP;
        int queueSize = Integer.getInteger("queueSize", 10000);
        File indexDir = Files.createTempDirectory( "archiva_index" ).toFile();
        log.info("Queue Index "+indexDir.toString());
        IndexCopier indexCopier = new IndexCopier( executorService, indexDir, true );
        NRTIndexFactory nrtIndexFactory = new NRTIndexFactory( indexCopier, statsProvider);
        MountInfoProvider mountInfoProvider = Mounts.defaultMountInfoProvider( );
        IndexTracker tracker = new IndexTracker(new DefaultIndexReaderFactory( mountInfoProvider, indexCopier ), nrtIndexFactory);
        DocumentQueue queue = new DocumentQueue(queueSize, tracker, executorService, statsProvider);
        LocalIndexObserver localIndexObserver = new LocalIndexObserver( queue, statsProvider);
        LuceneIndexProvider provider = new LuceneIndexProvider(tracker);

        //        ExternalObserverBuilder builder = new ExternalObserverBuilder(queue, tracker, statsProvider,
//            executorService, queueSize);
//        Observer observer = builder.build();
//        builder.getBackgroundObserver();

        LuceneIndexEditorProvider editorProvider = new LuceneIndexEditorProvider(null,
        tracker, new ExtractedTextCache(0, 0), null, mountInfoProvider);
        editorProvider.setIndexingQueue(queue);


        log.info("Oak: "+oak+" with nodeStore "+nodeStore);
        Jcr jcr = new Jcr(oak).with( editorProvider ).with( (Observer) provider )
            .with(localIndexObserver)
            // .with(observer)
            .with( ( QueryIndexProvider) provider )
            .withAsyncIndexing("async",5  );
        Repository r = jcr.createRepository();
        try
        {
            Thread.currentThread().sleep(1000);
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace( );
        }
        return r;


    }

    public String getStoreType( )
    {
        return storeType;
    }

    public void setStoreType( String storeType )
    {
        this.storeType = storeType;
    }

    public Path getRepositoryPath( )
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
                e.printStackTrace( );
            }
        }
    }

    private ExecutorService createExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 5, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();
            private final Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    log.warn("Error occurred in asynchronous processing ", e);
                }
            };
            @Override
            public Thread newThread(@Nonnull Runnable r) {
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

}
