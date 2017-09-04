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

import org.apache.archiva.admin.model.beans.RepositoryGroup;
import org.apache.archiva.scheduler.MergedRemoteIndexesScheduler;
import org.apache.commons.lang.StringUtils;
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
    public void schedule( RepositoryGroup repositoryGroup, Path directory )
    {
        if ( StringUtils.isEmpty( repositoryGroup.getCronExpression() ) )
        {
            return;
        }
        CronTrigger cronTrigger = new CronTrigger( repositoryGroup.getCronExpression() );

        List<String> repositories = repositoryGroup.getRepositories();

        IndexMergerRequest indexMergerRequest =
            new IndexMergerRequest( repositories, true, repositoryGroup.getId(), repositoryGroup.getMergedIndexPath(),
                                    repositoryGroup.getMergedIndexTtl() ).mergedIndexDirectory( directory );

        MergedRemoteIndexesTaskRequest taskRequest =
            new MergedRemoteIndexesTaskRequest( indexMergerRequest, indexMerger );

        logger.info( "schedule merge remote index for group {} with cron {}", repositoryGroup.getId(),
                     repositoryGroup.getCronExpression() );

        ScheduledFuture scheduledFuture =
            taskScheduler.schedule( new MergedRemoteIndexesTask( taskRequest ), cronTrigger );
        scheduledFutureMap.put( repositoryGroup.getId(), scheduledFuture );
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
