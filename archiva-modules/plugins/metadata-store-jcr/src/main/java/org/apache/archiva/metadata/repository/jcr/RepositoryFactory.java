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

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.time.StopWatch;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.index.IndexUtils;
import org.apache.jackrabbit.oak.plugins.index.lucene.*;
import org.apache.jackrabbit.oak.plugins.index.lucene.hybrid.DocumentQueue;
import org.apache.jackrabbit.oak.plugins.index.lucene.hybrid.LocalIndexObserver;
import org.apache.jackrabbit.oak.plugins.index.lucene.hybrid.NRTIndexFactory;
import org.apache.jackrabbit.oak.plugins.index.lucene.reader.DefaultIndexReaderFactory;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.archiva.metadata.repository.jcr.RepositoryFactory.StoreType.IN_MEMORY_TYPE;
import static org.apache.archiva.metadata.repository.jcr.RepositoryFactory.StoreType.SEGMENT_FILE_TYPE;
import static org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexConstants.INCLUDE_PROPERTY_TYPES;

/**
 * Created by martin on 14.06.17.
 *
 * @author Martin Stockhammer
 * @since 3.0.0
 */
public class RepositoryFactory
{

    private Logger log = LoggerFactory.getLogger( RepositoryFactory.class );

    private FileStore fileStore;

    private NodeStore nodeStore;

    private ExecutorService executorService;

    public enum StoreType
    {
        SEGMENT_FILE_TYPE,
        IN_MEMORY_TYPE;
    }

    private StoreType storeType = SEGMENT_FILE_TYPE;

    private Path repositoryPath = Paths.get( "repository" );

    public Repository createRepository()
        throws IOException, InvalidFileStoreVersionException
    {
        createExecutor();

        if ( SEGMENT_FILE_TYPE == storeType )
        {
            fileStore = FileStoreBuilder.fileStoreBuilder( repositoryPath.toFile() ).build();
            nodeStore = SegmentNodeStoreBuilders.builder( fileStore ) //
                .withStatisticsProvider( StatisticsProvider.NOOP ) //
                .build();
        }
        else if ( IN_MEMORY_TYPE == storeType )
        {
            nodeStore = null;
        }
        else
        {
            throw new IllegalArgumentException( "Store type " + storeType + " not recognized" );
        }

        Oak oak = nodeStore == null ? new Oak() : new Oak( nodeStore );
        oak.with( new RepositoryInitializer()
        {
            @Override
            public void initialize( @Nonnull NodeBuilder root )
            {
                log.info( "Creating index " );

                NodeBuilder lucene = IndexUtils.getOrCreateOakIndex( root ).child( "lucene" );
                lucene.setProperty( JcrConstants.JCR_PRIMARYTYPE, "oak:QueryIndexDefinition", Type.NAME );

                lucene.setProperty( "compatVersion", 2 );
                lucene.setProperty( "type", "lucene" );
                // lucene.setProperty("async", "async");
                lucene.setProperty( INCLUDE_PROPERTY_TYPES, ImmutableSet.of( "String" ), Type.STRINGS );
                // lucene.setProperty("refresh",true);
                lucene.setProperty( "async", ImmutableSet.of( "async", "sync" ), Type.STRINGS );
                NodeBuilder rules = lucene.child( "indexRules" ).
                    setProperty( JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED, Type.NAME );
                rules.setProperty( ":childOrder", ImmutableSet.of( "archiva:projectVersion", //
                                                                   "archiva:artifact", //
                                                                   "archiva:facet", //
                                                                   "archiva:namespace", //
                                                                   "archiva:project" ), //
                                   Type.STRINGS );
                NodeBuilder allProps = rules.child( "archiva:projectVersion" ) //
                    .child( "properties" ) //
                    .setProperty( JcrConstants.JCR_PRIMARYTYPE, "nt:unstructured", Type.NAME ) //
                    .setProperty( ":childOrder", ImmutableSet.of( "allProps" ), Type.STRINGS ) //
                    .setProperty( "indexNodeName", true ) //
                    .child( "allProps" ) //
                    .setProperty( JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED, Type.NAME );
                allProps.setProperty( "name", ".*" );
                allProps.setProperty( "isRegexp", true );
                allProps.setProperty( "nodeScopeIndex", true );
                allProps.setProperty( "index", true );
                allProps.setProperty( "analyzed", true );
                // allProps.setProperty("propertyIndex",true);
                allProps = rules.child( "archiva:artifact" ) //
                    .child( "properties" ) //
                    .setProperty( JcrConstants.JCR_PRIMARYTYPE, "nt:unstructured", Type.NAME ) //
                    .setProperty( ":childOrder", ImmutableSet.of( "allProps" ), Type.STRINGS ) //
                    .setProperty( "indexNodeName", true ).child( "allProps" ) //
                    .setProperty( JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED, Type.NAME );
                allProps.setProperty( "name", ".*" );
                allProps.setProperty( "isRegexp", true );
                allProps.setProperty( "nodeScopeIndex", true );
                allProps.setProperty( "index", true );
                allProps.setProperty( "analyzed", true );
                allProps = rules.child( "archiva:facet" ) //
                    .child( "properties" ) //
                    .setProperty( JcrConstants.JCR_PRIMARYTYPE, "nt:unstructured", Type.NAME ) //
                    .setProperty( ":childOrder", ImmutableSet.of( "allProps" ), Type.STRINGS ) //
                    .setProperty( "indexNodeName", true ) //
                    .child( "allProps" ) //
                    .setProperty( JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED, Type.NAME );
                allProps.setProperty( "name", ".*" );
                allProps.setProperty( "isRegexp", true );
                allProps.setProperty( "nodeScopeIndex", true );
                allProps.setProperty( "index", true );
                allProps.setProperty( "analyzed", true );
                allProps = rules.child( "archiva:namespace" ) //
                    .child( "properties" ) //
                    .setProperty( JcrConstants.JCR_PRIMARYTYPE, "nt:unstructured", Type.NAME ) //
                    .setProperty( ":childOrder", ImmutableSet.of( "allProps" ), Type.STRINGS ) //
                    .setProperty( "indexNodeName", true ) //
                    .child( "allProps" ) //
                    .setProperty( JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED, Type.NAME );
                allProps.setProperty( "name", ".*" );
                allProps.setProperty( "isRegexp", true );
                allProps.setProperty( "nodeScopeIndex", true );
                allProps.setProperty( "index", true );
                allProps.setProperty( "analyzed", true );
                allProps = rules.child( "archiva:project" ) //
                    .child( "properties" ) //
                    .setProperty( JcrConstants.JCR_PRIMARYTYPE, "nt:unstructured", Type.NAME ) //
                    .setProperty( ":childOrder", ImmutableSet.of( "allProps" ), Type.STRINGS ) //
                    .setProperty( "indexNodeName", true ) //
                    .child( "allProps" ) //
                    .setProperty( JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED, Type.NAME );
                allProps.setProperty( "name", ".*" );
                allProps.setProperty( "isRegexp", true );
                allProps.setProperty( "nodeScopeIndex", true );
                allProps.setProperty( "index", true );
                allProps.setProperty( "analyzed", true );

                log.info( "Index: {} myIndex {}", lucene, lucene.getChildNode( "myIndex" ) );
                log.info( "myIndex {}", lucene.getChildNode( "myIndex" ).getProperties() );
                // IndexUtils.createIndexDefinition(  )

            }
        } );

        StatisticsProvider statsProvider = StatisticsProvider.NOOP;
        int queueSize = Integer.getInteger( "queueSize", 10000 );
        Path indexDir = Files.createTempDirectory( "archiva_index" );
        log.info( "Queue Index {}", indexDir.toString() );
        IndexCopier indexCopier = new IndexCopier( executorService, indexDir.toFile(), true );
        NRTIndexFactory nrtIndexFactory = new NRTIndexFactory( indexCopier, statsProvider );
        MountInfoProvider mountInfoProvider = Mounts.defaultMountInfoProvider();
        IndexTracker tracker =
            new IndexTracker( new DefaultIndexReaderFactory( mountInfoProvider, indexCopier ), nrtIndexFactory );
        DocumentQueue queue = new DocumentQueue( queueSize, tracker, executorService, statsProvider );
        LocalIndexObserver localIndexObserver = new LocalIndexObserver( queue, statsProvider );
        LuceneIndexProvider provider = new LuceneIndexProvider( tracker );

        //        ExternalObserverBuilder builder = new ExternalObserverBuilder(queue, tracker, statsProvider,
//            executorService, queueSize);
//        Observer observer = builder.build();
//        builder.getBackgroundObserver();

        LuceneIndexEditorProvider editorProvider = //
            new LuceneIndexEditorProvider( null, tracker, //
                                           new ExtractedTextCache( 0, 0 ), //
                                           null, mountInfoProvider );
        editorProvider.setIndexingQueue( queue );

        log.info( "Oak: {} with nodeStore {}", oak, nodeStore );
        Jcr jcr = new Jcr( oak ).with( editorProvider ) //
            .with( (Observer) provider ) //
            .with( localIndexObserver )
            // .with(observer)
            .with( (QueryIndexProvider) provider ); //
            //.withAsyncIndexing( "async", 5 );
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Repository r = jcr.createRepository();
        stopWatch.stop();
        log.info( "time to create jcr repository: {} ms", stopWatch.getTime() );
//        try
//        {
//            Thread.currentThread().sleep( 1000 );
//        }
//        catch ( InterruptedException e )
//        {
//            log.error( e.getMessage(), e );
//        }
        return r;


    }

    public void close()
    {
        if ( fileStore != null )
        {
            fileStore.close();
        }
        if (executorService != null)
        {
            executorService.shutdownNow();
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

    private void createExecutor()
    {
        if (executorService ==null )
        {
            executorService = Executors.newCachedThreadPool();
        }

//
//        ThreadPoolExecutor executor =
//            new ThreadPoolExecutor( 0, 5, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
//                                    new ThreadFactory()
//                                    {
//                                        private final AtomicInteger counter = new AtomicInteger();
//
//                                        private final Thread.UncaughtExceptionHandler handler =
//                                            new Thread.UncaughtExceptionHandler()
//                                            {
//                                                @Override
//                                                public void uncaughtException( Thread t, Throwable e )
//                                                {
//                                                    log.warn( "Error occurred in asynchronous processing ", e );
//                                                }
//                                            };
//
//                                        @Override
//                                        public Thread newThread( @Nonnull Runnable r )
//                                        {
//                                            Thread thread = new Thread( r, createName() );
//                                            thread.setDaemon( true );
//                                            thread.setPriority( Thread.MIN_PRIORITY );
//                                            thread.setUncaughtExceptionHandler( handler );
//                                            return thread;
//                                        }
//
//                                        private String createName()
//                                        {
//                                            return "oak-lucene-" + counter.getAndIncrement();
//                                        }
//                                    } );
//        executor.setKeepAliveTime( 1, TimeUnit.MINUTES );
//        executor.allowCoreThreadTimeOut( true );
//        return executor;
    }

}
