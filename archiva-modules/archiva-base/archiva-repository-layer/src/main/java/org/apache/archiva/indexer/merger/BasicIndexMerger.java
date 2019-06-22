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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class BasicIndexMerger implements IndexMerger
{
    @Inject
    RepositoryRegistry repositoryRegistry;

    private Logger log = LoggerFactory.getLogger( getClass() );


    private List<TemporaryGroupIndex> temporaryGroupIndexes = new CopyOnWriteArrayList<>();

    private List<String> runningGroups = new CopyOnWriteArrayList<>();

    @Inject
    public BasicIndexMerger( )
    {
    }

    @Override
    public ArchivaIndexingContext buildMergedIndex( IndexMergerRequest indexMergerRequest )
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

        Path mergedIndexDirectory = indexMergerRequest.getMergedIndexDirectory();

        String tempRepoId = mergedIndexDirectory.getFileName().toString();

        try
        {
            Path indexLocation = mergedIndexDirectory.resolve( indexMergerRequest.getMergedIndexPath() );

            List<ArchivaIndexingContext> members = indexMergerRequest.getRepositoriesIds( ).stream( ).map( id ->
                repositoryRegistry.getRepository( id ) )
                .map( repo -> repo.getIndexingContext() ).filter( Objects::nonNull ).collect( Collectors.toList() );

            members.get( 0 ).
            if ( indexMergerRequest.isPackIndex() )
            {
                IndexPackingRequest request = new IndexPackingRequest( mergedCtx, //
                    mergedCtx.acquireIndexSearcher().getIndexReader(), //
                    indexLocation.toFile() );
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
            return new MavenIndexContext(repositoryRegistry.getRepositoryGroup(groupId), mergedCtx);
        }
        catch ( IOException e)
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

            Optional<IndexingContext> ctxOpt = temporaryContextes.stream( ).filter( ctx -> ctx.getId( ).equals( temporaryGroupIndex.getIndexId( ) ) ).findFirst( );
            if (ctxOpt.isPresent()) {
                IndexingContext ctx = ctxOpt.get();
                indexer.closeIndexingContext( ctx, true );
                temporaryGroupIndexes.remove( temporaryGroupIndex );
                temporaryContextes.remove( ctx );
                Path directory = temporaryGroupIndex.getDirectory();
                if ( directory != null && Files.exists(directory) )
                {
                    FileUtils.deleteDirectory( directory );
                }
            }
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
