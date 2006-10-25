package org.apache.maven.archiva.scheduler;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.archiva.scheduler.task.IndexerTask;
import org.apache.maven.archiva.scheduler.task.RepositoryTask;
import org.codehaus.plexus.scheduler.AbstractJob;
import org.codehaus.plexus.taskqueue.TaskQueue;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This class is the discoverer job that is executed by the scheduler.
 */
public class RepositoryTaskJob
    extends AbstractJob
{
    static final String TASK_KEY = "EXECUTION";

    static final String TASK_QUEUE = "TASK_QUEUE";

    static final String TASK_QUEUE_POLICY = "TASK_QUEUE_POLICY";

    /**
     * Execute the discoverer and the indexer.
     *
     * @param context
     * @throws org.quartz.JobExecutionException
     */
    public void execute( JobExecutionContext context )
        throws JobExecutionException
    {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        setJobDataMap( dataMap );

        TaskQueue indexerQueue = (TaskQueue) dataMap.get( TASK_QUEUE );
        String queuePolicy = dataMap.get( TASK_QUEUE_POLICY ).toString();

        RepositoryTask task = new IndexerTask();
        task.setJobName( context.getJobDetail().getName() );

        try
        {
            if ( indexerQueue.getQueueSnapshot().size() == 0 )
            {
                indexerQueue.put( task );
            }
            else
            {
                if ( RepositoryTask.QUEUE_POLICY_WAIT.equals( queuePolicy ) )
                {
                    indexerQueue.put( task );
                }
                else if ( RepositoryTask.QUEUE_POLICY_SKIP.equals( queuePolicy ) )
                {
                    //do not queue anymore, policy is to skip
                }
            }
        }
        catch ( TaskQueueException e )
        {
            throw new JobExecutionException( e );
        }
    }
}
