package org.apache.archiva.indexer.merger;
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

import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.apache.maven.index.packer.IndexPacker;
import org.apache.maven.index.packer.IndexPackingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Olivier Lamy
 * @since 1.4-M2
 */
@Service("indexMerger#default")
public class DefaultIndexMerger
    implements IndexMerger
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private MavenIndexerUtils mavenIndexerUtils;

    private NexusIndexer indexer;

    private IndexPacker indexPacker;

    private List<TemporaryGroupIndex> temporaryGroupIndexes = new CopyOnWriteArrayList<>();

    private List<String> runningGroups = new CopyOnWriteArrayList<String>();

    @Inject
    public DefaultIndexMerger( PlexusSisuBridge plexusSisuBridge, MavenIndexerUtils mavenIndexerUtils )
        throws PlexusSisuBridgeException
    {
        this.indexer = plexusSisuBridge.lookup( NexusIndexer.class );
        this.mavenIndexerUtils = mavenIndexerUtils;
        indexPacker = plexusSisuBridge.lookup( IndexPacker.class, "default" );
    }

    @Override
    public IndexingContext buildMergedIndex( IndexMergerRequest indexMergerRequest )
        throws IndexMergerException
    {
        String groupId = indexMergerRequest.getGroupId();

        if ( runningGroups.contains( groupId ) )
        {
            log.info( "skip build merge remote indexes for id: '{}' as already running", groupId );
            return null;
        }

        runningGroups.add( groupId );

        StopWatch stopWatch = new StopWatch();
        stopWatch.reset();
        stopWatch.start();

        File mergedIndexDirectory = indexMergerRequest.getMergedIndexDirectory();

        String tempRepoId = mergedIndexDirectory.getName();

        try
        {
            File indexLocation = new File( mergedIndexDirectory, indexMergerRequest.getMergedIndexPath() );
            IndexingContext indexingContext =
                indexer.addIndexingContext( tempRepoId, tempRepoId, mergedIndexDirectory, indexLocation, null, null,
                                            mavenIndexerUtils.getAllIndexCreators() );

            for ( String repoId : indexMergerRequest.getRepositoriesIds() )
            {
                IndexingContext idxToMerge = indexer.getIndexingContexts().get( repoId );
                if ( idxToMerge != null )
                {
                    indexingContext.merge( idxToMerge.getIndexDirectory() );
                }
            }

            indexingContext.optimize();

            if ( indexMergerRequest.isPackIndex() )
            {
                IndexPackingRequest request = new IndexPackingRequest( indexingContext, indexLocation );
                indexPacker.packIndex( request );
            }

            if ( indexMergerRequest.isTemporary() )
            {
                temporaryGroupIndexes.add( new TemporaryGroupIndex( mergedIndexDirectory, tempRepoId, groupId,
                                                                    indexMergerRequest.getMergedIndexTtl() ) );
            }
            stopWatch.stop();
            log.info( "merged index for repos {} in {} s", indexMergerRequest.getRepositoriesIds(),
                      stopWatch.getTime() );
            return indexingContext;
        }
        catch ( IOException e )
        {
            throw new IndexMergerException( e.getMessage(), e );
        }
        catch ( UnsupportedExistingLuceneIndexException e )
        {
            throw new IndexMergerException( e.getMessage(), e );
        }
        finally
        {
            runningGroups.remove( groupId );
        }
    }

    @Async
    @Override
    public void cleanTemporaryGroupIndex( TemporaryGroupIndex temporaryGroupIndex )
    {
        if ( temporaryGroupIndex == null )
        {
            return;
        }

        try
        {
            IndexingContext indexingContext = indexer.getIndexingContexts().get( temporaryGroupIndex.getIndexId() );
            if ( indexingContext != null )
            {
                indexer.removeIndexingContext( indexingContext, true );
            }
            File directory = temporaryGroupIndex.getDirectory();
            if ( directory != null && directory.exists() )
            {
                FileUtils.deleteDirectory( directory );
            }
            temporaryGroupIndexes.remove( temporaryGroupIndex );
        }
        catch ( IOException e )
        {
            log.warn( "fail to delete temporary group index {}", temporaryGroupIndex.getIndexId(), e );
        }
    }

    @Override
    public Collection<TemporaryGroupIndex> getTemporaryGroupIndexes()
    {
        return this.temporaryGroupIndexes;
    }
}
