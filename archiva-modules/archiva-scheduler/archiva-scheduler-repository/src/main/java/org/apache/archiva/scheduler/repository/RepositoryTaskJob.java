package org.apache.archiva.scheduler.repository;

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

import org.apache.archiva.components.scheduler.AbstractJob;
import org.apache.archiva.components.taskqueue.TaskQueue;
import org.apache.archiva.components.taskqueue.TaskQueueException;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This class is the repository job that is executed by the scheduler.
 */
public class RepositoryTaskJob
    extends AbstractJob
{
    /**
     * Execute the discoverer and the indexer.
     * 
     * @param context
     * @throws org.quartz.JobExecutionException
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public void execute( JobExecutionContext context )
        throws JobExecutionException
    {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        setJobDataMap( dataMap );

        TaskQueue taskQueue = (TaskQueue) dataMap.get( DefaultRepositoryArchivaTaskScheduler.TASK_QUEUE );

        String repositoryId = (String) dataMap.get( DefaultRepositoryArchivaTaskScheduler.TASK_REPOSITORY );
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );

        try
        {
            taskQueue.put( task );
        }
        catch ( TaskQueueException e )
        {
            throw new JobExecutionException( e );
        }
    }
}
