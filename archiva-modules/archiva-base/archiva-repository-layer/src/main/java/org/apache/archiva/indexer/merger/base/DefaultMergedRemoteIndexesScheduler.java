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

import org.apache.archiva.indexer.merger.IndexMerger;
import org.apache.archiva.indexer.merger.IndexMergerRequest;
import org.apache.archiva.indexer.merger.MergedRemoteIndexesScheduler;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * @author Olivier Lamy
 * @since 2.0.0
 */
@Service( "mergedRemoteIndexesScheduler#default" )
public class DefaultMergedRemoteIndexesScheduler
    implements MergedRemoteIndexesScheduler
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "taskScheduler#mergeRemoteIndexes" )
    private TaskScheduler taskScheduler;

    @Inject
    private IndexMerger indexMerger;

    private Map<String, ScheduledFuture> scheduledFutureMap = new ConcurrentHashMap<>();

    @Override
    public void schedule(RepositoryGroup repositoryGroup, StorageAsset directory )
    {
        if ( StringUtils.isEmpty( repositoryGroup.getSchedulingDefinition() ) )
        {
            return;
        }
        CronTrigger cronTrigger = new CronTrigger( repositoryGroup.getSchedulingDefinition() );

        List<ManagedRepository> repositories = repositoryGroup.getRepositories();

        if (repositoryGroup.supportsFeature( IndexCreationFeature.class ))
        {

            IndexCreationFeature indexCreationFeature = repositoryGroup.getFeature( IndexCreationFeature.class ).get();
            Path indexPath = indexCreationFeature.getLocalIndexPath().getFilePath();
            if (indexPath!=null)
            {
                IndexMergerRequest indexMergerRequest =
                    new IndexMergerRequest( repositories.stream( ).map( r -> r.getId( ) ).collect( Collectors.toList( ) ), true, repositoryGroup.getId( ),
                        indexPath.toString( ),
                        repositoryGroup.getMergedIndexTTL( ) ).mergedIndexDirectory( directory );

                MergedRemoteIndexesTaskRequest taskRequest =
                    new MergedRemoteIndexesTaskRequest( indexMergerRequest, indexMerger );

                logger.info( "schedule merge remote index for group {} with cron {}", repositoryGroup.getId( ),
                    repositoryGroup.getSchedulingDefinition( ) );

                ScheduledFuture scheduledFuture =
                    taskScheduler.schedule( new MergedRemoteIndexesTask( taskRequest ), cronTrigger );
                scheduledFutureMap.put( repositoryGroup.getId( ), scheduledFuture );
            } else {
                logger.error("Requested index merger for repository group {} with non local index path {}", repositoryGroup.getId(), indexCreationFeature.getLocalIndexPath());
            }
        } else {
            logger.error("Scheduling merged index for repository group {}, but it does not support IndexCreationFeature.", repositoryGroup.getId());
        }
    }

    @Override
    public void unschedule( RepositoryGroup repositoryGroup )
    {
        ScheduledFuture scheduledFuture = scheduledFutureMap.remove( repositoryGroup.getId() );
        if ( scheduledFuture != null )
        {
            scheduledFuture.cancel( true );
        }
    }
}
