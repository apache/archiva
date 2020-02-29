package org.apache.archiva.indexer.merger.base;
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

import org.apache.archiva.indexer.ArchivaIndexManager;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.indexer.IndexCreationFailedException;
import org.apache.archiva.indexer.merger.IndexMerger;
import org.apache.archiva.indexer.merger.IndexMergerException;
import org.apache.archiva.indexer.merger.IndexMergerRequest;
import org.apache.archiva.indexer.merger.TemporaryGroupIndex;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @author Olivier Lamy
 * @since 1.4-M2
 */
@Service("indexMerger#default")
public class DefaultIndexMerger
    implements IndexMerger
{

    @Inject
    RepositoryRegistry repositoryRegistry;

    private Logger log = LoggerFactory.getLogger( getClass() );

    private List<TemporaryGroupIndex> temporaryGroupIndexes = new CopyOnWriteArrayList<>();

    private List<ArchivaIndexingContext>  temporaryContextes = new CopyOnWriteArrayList<>(  );

    private List<String> runningGroups = new CopyOnWriteArrayList<>();

    @Inject
    public DefaultIndexMerger( )
    {
    }

    @Override
    public ArchivaIndexingContext buildMergedIndex(IndexMergerRequest indexMergerRequest )
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
        try {
            stopWatch.reset();
            stopWatch.start();

            StorageAsset mergedIndexDirectory = indexMergerRequest.getMergedIndexDirectory();
            Repository destinationRepository = repositoryRegistry.getRepository(indexMergerRequest.getGroupId());

            ArchivaIndexManager idxManager = repositoryRegistry.getIndexManager(destinationRepository.getType());
            List<ArchivaIndexingContext> sourceContexts = indexMergerRequest.getRepositoriesIds().stream().map(id -> repositoryRegistry.getRepository(id).getIndexingContext()).collect(Collectors.toList());
            try {
                ArchivaIndexingContext result = idxManager.mergeContexts(destinationRepository, sourceContexts, indexMergerRequest.isPackIndex());
                if ( indexMergerRequest.isTemporary() )
                {
                    String tempRepoId = destinationRepository.getId()+System.currentTimeMillis();
                    temporaryGroupIndexes.add( new TemporaryGroupIndex( mergedIndexDirectory, tempRepoId, groupId,
                            indexMergerRequest.getMergedIndexTtl() ) );
                    temporaryContextes.add(result);
                }
                return result;
            } catch (IndexCreationFailedException e) {
                throw new IndexMergerException("Index merging failed " + e.getMessage(), e);
            }

        } finally {
            stopWatch.stop();
            log.info( "merged index for repos {} in {} s", indexMergerRequest.getRepositoriesIds(),
                    stopWatch.getTime() );
            runningGroups.remove(groupId);
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
            Optional<ArchivaIndexingContext> ctxOpt = temporaryContextes.stream( ).filter( ctx -> ctx.getId( ).equals( temporaryGroupIndex.getIndexId( ) ) ).findFirst( );
            if (ctxOpt.isPresent()) {
                ArchivaIndexingContext ctx = ctxOpt.get();
                ctx.close(true);
                temporaryGroupIndexes.remove( temporaryGroupIndex );
                temporaryContextes.remove( ctx );
                StorageAsset directory = temporaryGroupIndex.getDirectory();
                if ( directory != null && directory.exists() )
                {
                    org.apache.archiva.repository.storage.util.StorageUtil.deleteRecursively( directory );
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
